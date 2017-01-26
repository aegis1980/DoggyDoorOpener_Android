package com.fitc.dooropener.control;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fitc.dooropener.lib.BaseActivity;
import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.gcm.GcmDataPayload;
import com.fitc.dooropener.lib.server.ServerService;
import com.fitc.dooropener.lib.ui.DoorControllerLayout;
import com.fitc.usbconnectionlibrary.UsbConnectionService;
import com.fitc.weatherbox.WeatherBoxService;
import com.fitc.weatherbox.WeatherBoxSyncService;

public class MainActivity extends BaseActivity implements View.OnClickListener, Response.ErrorListener, Response.Listener<String> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private DoorControllerLayout mDoorControllerLayout;
    private Button mDoorOpenButton, mDoorCloseButton;

    private TextView mDataTextView;


    private TextView mUsbCxnStatusTV;

    boolean mBound = false;


    protected boolean mUsbConnected = false;

    private Snackbar mSnackBar;

    //*************************************************************************************************************
    // START-OF- Activity Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        turnOffBluetooth();
        turnOffGPS();

        setContentView(R.layout.activity_main);

        WeatherBoxSyncService.setFormId("1TgseUiZFY0VidMmwOuZ5UFFty43OgGnPur-Cnns2Sn0");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceCamera.takePhoto(MainActivity.this);
            }
        });

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.parent);
        mDoorControllerLayout = (DoorControllerLayout) findViewById(R.id.door_opener_layout);

        mDoorOpenButton = (Button) findViewById(R.id.openButton);
        mDoorOpenButton.setOnClickListener(this);

        mDoorCloseButton = (Button) findViewById(R.id.closeButton);
        mDoorCloseButton.setOnClickListener(this);

        mDataTextView = (TextView) findViewById(R.id.textView_dataincoming);
        mDataTextView.setMovementMethod(new ScrollingMovementMethod());

        mUsbCxnStatusTV = (TextView) findViewById(R.id.textView_usbcxn) ;


        Button syncButton = (Button) findViewById(R.id.button_wbsync);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeatherBoxService.initiateCloudSync(MainActivity.this, true);
            }
        });

        Intent camIntent = new Intent(this, DeviceCameraService.class);
        startService(camIntent);

        //TODO : needs to go in manifest to start on boot...or on USB plug in.
        Intent i = new Intent(this,UsbConnectionService.class);
        startService(i);

        // current door status from the arduino
        DoorArduino.sendTask(this,CommonApplication.ControlTask.DOORARDUINO_STATUS);

    }


    private void turnOffGPS(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }


    private void turnOffBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // start camera service.
        Intent cameraServiceIntent = new Intent(this, DeviceCameraService.class);
        startService(cameraServiceIntent);

        IntentFilter usbCxnFilter = new IntentFilter(UsbConnectionService.ACTION_USB_CONNECTION_STATUS_CHANGE);
        usbCxnFilter.addAction(UsbConnectionService.ACTION_USB_DATA_INCOMING);
        registerReceiver(mUsbServiceReceiver, usbCxnFilter);

    }

    @Override
    public void onResume(){
        super.onResume();
        UsbConnectionService.requestConnectionStatus(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mUsbServiceReceiver!=null) unregisterReceiver(mUsbServiceReceiver);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // END-OF- Activity Lifecycle
    //*************************************************************************************************************
    // START-OF- UI methods


    @Override
    public void onClick(View v) {
        final int id = v.getId();

        String task;
        if (id==mDoorOpenButton.getId()) {
            DoorArduino.sendTask(this,CommonApplication.ControlTask.DOORARDUINO_OPEN);
            ServerService.sendStatusToServer(this,CommonApplication.DOOR_STATUS,CommonApplication.DoorAction.OPEN_BY_UI);
            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
            mSnackBar = Snackbar.make(mCoordinatorLayout, "Sending command to open door", Snackbar.LENGTH_LONG);
            mSnackBar.show();
        } else if (id==mDoorCloseButton.getId()) {
            DoorArduino.sendTask(this,CommonApplication.ControlTask.DOORARDUINO_CLOSE);
            ServerService.sendStatusToServer(this,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSE_BY_UI);
            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
            mSnackBar = Snackbar.make(mCoordinatorLayout, "Sending command to close door", Snackbar.LENGTH_LONG);
            mSnackBar.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    // END-OF- UI methods
    //*************************************************************************************************************
    // START-OF- Server and GCM callbacks

    @Override
    public void onServerResponse(Context context, Intent intent) {
        if (intent.getAction().equals(CommonApplication.ACTION_SERVER_RESPONSE)) {
           // TODO
        }
    }

    @Override
    public void onGcmStatus(GcmDataPayload payload) {
        if(payload.getStatusType()==GcmDataPayload.Status.COMMAND_TO_CONTROL){
            String command = payload.getStatusData();
            switch(command){
                case CommonApplication.ControlTask.DOORARDUINO_OPEN:
                case CommonApplication.ControlTask.DOORARDUINO_CLOSE:
                case CommonApplication.ControlTask.DOORARDUINO_STATUS:
                  mDataTextView.append("GCM INCOMING: Server Command:" + payload.toString() + "\n");
                    break;
                case CommonApplication.ControlTask.DEVICECAMERA_TAKEPHOTO:
                    break;
                case CommonApplication.ControlTask.BT_STATUS:

            }
        }
    }

    // END-OF- Server and GCM callbacks
    //*************************************************************************************************************
    // START OF USB

    private void setConnectionStatus(boolean connected){
        if (connected){
            mUsbCxnStatusTV.setText("UsbProtocol connected");
        }else{
            mUsbCxnStatusTV.setText("UsbProtocol not connected");
        }

    }


    private void incomingUsbData(int len, byte[] dataPayload){
                final char protocol =  (char) dataPayload[0];

                if (protocol == DoorArduino.SYNC){
                    char command = (char) dataPayload[1];
                    mDataTextView.append("ARDUINO INCOMING: " + command + "\n");
                    switch(command){
                        case DoorArduino.IN_OPENING:

                            mDoorControllerLayout.startOpenDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "UsbProtocol opening", Snackbar.LENGTH_LONG);
                            mSnackBar.show();
                            break;
                        case DoorArduino.IN_CLOSING:

                            mDoorControllerLayout.startCloseDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "UsbProtocol closing", Snackbar.LENGTH_LONG);
                            mSnackBar.show();
                            break;
                        case DoorArduino.IN_OPENED:

                            mDoorControllerLayout.startOpenDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "UsbProtocol open", Snackbar.LENGTH_LONG);
                            mSnackBar.show();
                            break;
                        case DoorArduino.IN_CLOSED:

                            mDoorControllerLayout.startCloseDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "UsbProtocol closed", Snackbar.LENGTH_LONG);
                            mSnackBar.show();
                            break;
                        case DoorArduino.IN_STALLED_OPENING:

                            break;
                        case DoorArduino.IN_STALLED_CLOSING:

                            break;
                        case DoorArduino.IN_OPEN_ASSIST:

                            break;
                        case DoorArduino.IN_CLOSE_ASSIST:

                            break;
                        case DoorArduino.IN_REPEAT_LAST:

                            break;
                        default:
                            // this means that there was a communication error.

                            break;
                    }
                }

    }

    //********************************************************************************************
    // callback from Camera.PictureCallback interface


    //********************************************************************************************


    @Override
    public void onErrorResponse(VolleyError error) {

    }

    @Override
    public void onResponse(String response) {

    }


    //*********************************************************************************************

    private final BroadcastReceiver mUsbServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbConnectionService.ACTION_USB_CONNECTION_STATUS_CHANGE.equals(action)) {
                mUsbConnected = intent.getBooleanExtra(UsbConnectionService.EXTRA_USB_CONNECTION_STATUS, false);
                setConnectionStatus(mUsbConnected);
            } else if (UsbConnectionService.ACTION_USB_DATA_INCOMING.equals(action)) {
                int len = intent.getIntExtra(UsbConnectionService.EXTRA_USB_INCOMING_DATA_PAYLOAD_LENGTH, 0);
                byte[] dataPayload = intent.getByteArrayExtra(UsbConnectionService.EXTRA_USB_INCOMING_DATA_PAYLOAD);
                if (len>0){
                    incomingUsbData(len,dataPayload);
                }
            }
        }
    };


}
