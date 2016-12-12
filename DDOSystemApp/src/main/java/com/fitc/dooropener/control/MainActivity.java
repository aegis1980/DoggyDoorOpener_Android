package com.fitc.dooropener.control;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fitc.dooropener.control.bt.BluetoothLeConnectionService;
import com.fitc.dooropener.control.bt.BluetoothLeDeviceListActivity;
import com.fitc.dooropener.lib.BaseActivity;
import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.gcm.GcmDataPayload;
import com.fitc.dooropener.lib.server.MapBundler;

import com.fitc.dooropener.lib.server.ServerService;
import com.fitc.dooropener.lib.ui.DoorControllerLayout;
//import com.fitc.softmodem.FSKModemService;
import com.fitc.usbconnectionlibrary.UsbConnectionService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity implements View.OnClickListener, Response.ErrorListener, Response.Listener<String> {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 69;

    private DoorControllerLayout mDoorControllerLayout;
    private Button mDoorOpenButton, mDoorCloseButton;

    private TextView mDataTextView;

    private TextView mBTTagCxnStatusTV, mBTTagRangeStatusTV, mDoorCxnStatusTV;
    private ImageView mBTTagStatusIV;


    private BroadcastReceiver mCameraReceiver;
    private CameraManagerService mCameraManager;



    private boolean mBluetoothConnected;
    BluetoothAdapter mBluetoothAdapter;
    boolean mBound = false;
    private String mLastControlTaskCommand = null;


    protected boolean mUsbConnected = false;

    private Snackbar mSnackBar;
    private boolean mBluetoothEnabled = false;


    //*************************************************************************************************************
    // START-OF- Activity Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

           Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBound) mCameraManager.takePhoto();

          //      getWindowManager().addView(camPreview, new WindowManager.LayoutParams());
                //            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //                   .setAction("Action", null).show();
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

        mDoorCxnStatusTV = (TextView) findViewById(R.id.textView_doorcxn) ;

        mBTTagCxnStatusTV = (TextView) findViewById(R.id.textView_btconnectionstatus);
        mBTTagRangeStatusTV = (TextView) findViewById(R.id.textView_btrangestatus);
        mBTTagStatusIV = (ImageView) findViewById(R.id.imageView_btstatus);

        CardView btCardView = (CardView) findViewById(R.id.cardview_bluetooth);

        btCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSavedBluetoothDeviceConnection();
            }
        });


        Intent camIntent = new Intent(this, CameraManagerService.class);
        startService(camIntent);


        //Log.e(TAG, CommonApplication.getEmail());
        // Start service to do the USB connectionstuff.
        //TODO : needs to go in manifest to start on boot...or on USB plug in.
        Intent i = new Intent(this,UsbConnectionService.class);
        startService(i);

        // current door status from the arduino
        sendDoorTasktoArduino(CommonApplication.ControlTask.STATUS);




        // try to malke connection to a svaed device.
        // if fails then starts new-connection path
        attemptSavedBluetoothDeviceConnection();


    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent cameraServiceIntent = new Intent(this, CameraManagerService.class);
        bindService(cameraServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

        if (mCameraReceiver == null) mCameraReceiver = new CameraBroadcastReceiver();
        IntentFilter intentFilter2 = new IntentFilter(CameraManagerService.ACTION_PHOTO_READY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mCameraReceiver, intentFilter2);


        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction(BluetoothLeConnectionService.ACTION_GATT_CONNECTED);
        intentFilter3.addAction(BluetoothLeConnectionService.ACTION_GATT_DISCONNECTED);
        intentFilter3.addAction(BluetoothLeConnectionService.ACTION_DATA_AVAILABLE);
        intentFilter3.addAction(BluetoothLeConnectionService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter3.addAction(BluetoothLeConnectionService.ACTION_SCAN_FOR_SPECIFIC_DEVICE_FAILED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBluetoothUpdateReceiver, intentFilter3);

        IntentFilter filter = new IntentFilter(UsbConnectionService.ACTION_USB_CONNECTION_STATUS_CHANGE);
        filter.addAction(UsbConnectionService.ACTION_USB_DATA_INCOMING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mUsbServiceReceiver, filter);
    }

    @Override
    public void onResume(){
        super.onResume();



        UsbConnectionService.requestConnectionStatus(this);

    }

    @Override
    public void onPause(){
        super.onPause();

        if (mCameraManager!=null)  mCameraManager.releaseCameraAndPreview();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothLeDeviceListActivity.REQUEST_CONNECT_DEVICE:
                // When BluetoothLeDeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(BluetoothLeDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    String deviceName = data.getStringExtra(BluetoothLeDeviceListActivity.EXTRA_DEVICE_NAME);
                    if (address!=null){
                        // save device to saved preferences.
                        CommonApplication.Bluetooth.setBluetoothDeviceMacAddress(address);
                        CommonApplication.Bluetooth.setBluetoothDeviceName(deviceName);
                        setUiBluetoothState(false);

                        /// Start service that manages the BT LE connection
                        Intent i = new Intent(this,BluetoothLeConnectionService.class);
                        startService(i);

                    } else {
                        Log.e(TAG, "No BT device address in received intent data.");
                    }

                }
                break;
  /*          case WifiDeviceListActivity.REQUEST_DEVICES:
                Set<String> mWifiDevices = WifiDevice.loadSavedDevices(this);
                // When WifiDeviceListActivity returns
                break;*/
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled.
                    if (CommonApplication.Bluetooth.getBluetoothDeviceMacAddress()==null || CommonApplication.Bluetooth.getBluetoothDeviceName()==null ){
                        // Start up the Device list activity to pick a device.
                        registerBluetoothDevice();
                    }
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    mBluetoothEnabled = false;
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    setUiBluetoothNotSetup();
                    break;
                }
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        if (mUsbServiceReceiver!=null) LocalBroadcastManager.getInstance(this).unregisterReceiver(mUsbServiceReceiver);
        if (mCameraReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(mCameraReceiver);
        if (mBluetoothUpdateReceiver != null) LocalBroadcastManager.getInstance(this).unregisterReceiver(mBluetoothUpdateReceiver);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        UsbConnectionService.requestCloseAccessory(this);

    }

    // END-OF- Activity Lifecycle
    //*************************************************************************************************************
    // START-OF- UI methods


    @Override
    public void onClick(View v) {
        final int id = v.getId();

        String task;
        if (id==mDoorOpenButton.getId()) {
            sendDoorTasktoArduino(CommonApplication.ControlTask.OPEN);
            sendStatusToServer(CommonApplication.DOOR_STATUS,CommonApplication.DoorAction.OPEN_BY_UI);
            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
            mSnackBar = Snackbar.make(mCoordinatorLayout, "Sending command to open door", Snackbar.LENGTH_LONG);
            mSnackBar.show();
        } else if (id==mDoorCloseButton.getId()) {
            sendDoorTasktoArduino(CommonApplication.ControlTask.CLOSE);
            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSE_BY_UI);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_wifidevices) {
            setWifiDevices();
            return true;
        }

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
        Log.i(TAG, "onGcmStatus");
        if(payload.getStatusType()==GcmDataPayload.Status.COMMAND_TO_CONTROL){
            String command = payload.getStatusData();
            switch(command){
                case CommonApplication.ControlTask.OPEN:
                case CommonApplication.ControlTask.CLOSE:
                case CommonApplication.ControlTask.STATUS:
                  mDataTextView.append("GCM INCOMING: Server Command:" + payload.toString() + "\n");
                    sendDoorTasktoArduino(command);
                    mLastControlTaskCommand = command;
                    break;
                case CommonApplication.ControlTask.PHOTO:
                    if (mBound){
                        mCameraManager.takePhoto();
                    } else {
                        Toast.makeText(this, "Not bound to camera service.",Toast.LENGTH_LONG).show();
                    }
                    break;
                case CommonApplication.ControlTask.BT_STATUS:

            }
        }
    }

    // END-OF- Server and GCM callbacks
    //*************************************************************************************************************
    // START-OF- Bluetooth helper methods


    private void setUiBluetoothNotSetup() {
        mBTTagStatusIV.setImageResource(R.drawable.bt_not_setup);
        mBTTagCxnStatusTV.setText(R.string.bt_not_setup);
        mBTTagRangeStatusTV.setText("");
    }

    private void setUiBluetoothState(boolean connected) {
        Resources res = getResources();
        final String nameStr = String.format(res.getString(R.string.bt_connected_message), CommonApplication.Bluetooth.getBluetoothDeviceName());
        mBTTagCxnStatusTV.setText(nameStr);
        if (connected) {
            mBTTagStatusIV.setImageResource(R.drawable.bt_inrange);
            mBTTagRangeStatusTV.setText(R.string.bt_in_range);
        } else {
            mBTTagStatusIV.setImageResource(R.drawable.bt_outofrange);
            mBTTagRangeStatusTV.setText(R.string.bt_out_of_range);
        }
    }

    private void attemptSavedBluetoothDeviceConnection() {
        /**
         * If a BT device has been saved then set up BT connections, otherwise don't
         * User needs to click on icon in the action bar to set up.
         */
        if (CommonApplication.Bluetooth.getBluetoothDeviceMacAddress()!=null) {
            Log.i(TAG, "Registered Mac Address: " + CommonApplication.Bluetooth.getBluetoothDeviceMacAddress());
            boolean isOn = turnOnBluetoothConnection();

            // if bt is enabled, then connect to the saved device.
            if (isOn){
                setUiBluetoothState(false);
                Intent i = new Intent(this,BluetoothLeConnectionService.class);
                startService(i);
            }
        } else {
            setUiBluetoothNotSetup();
            Log.i(TAG,"No registered MacAddress");
            // this start the DevicelistActivity. If BT was not on then it is deferred to
            // onActivityResult.
            registerBluetoothDevice();
        }
    }

    private void registerBluetoothDevice() {
        Intent intent = new Intent(this, BluetoothLeDeviceListActivity.class);
        startActivityForResult(intent, BluetoothLeDeviceListActivity.REQUEST_CONNECT_DEVICE);
    }

    private boolean turnOnBluetoothConnection() {

        // If BT is not on, request that it be enabled.
        // will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            return false;
            // Otherwise, setup the chat session
        } else {
            return true;
        }
    }

    private void setConnectionStatus(boolean connected){
        if (connected){
            mDoorCxnStatusTV.setText("Door connected");
        }else{
            mDoorCxnStatusTV.setText("Door not connected");
        }

    }

    // END OF  Bluetooth helper methods
    //*************************************************************************************************************
    // START OF wifi devices

    /**
     * Open o set wifi devices
     *
     */
    private  void setWifiDevices(){
        //Intent intent = new Intent(this, ActivityMain.class);
        //startActivityForResult(intent, 1);
    }

    // END OF  Wifi devcies helper methods
    //*************************************************************************************************************
    // START OF ____

    /**
     * Starts Door Task intent service.
     * @param  statusType
     * @param status
     * @return
     */
    private boolean sendStatusToServer(int statusType, String status) {
        Map<String,String> params = new HashMap<>();
        params.put(CommonApplication.EndPointParams.REQUEST_ID,ServerService.getRequestId());
        params.put(CommonApplication.EndPointParams.FROM_EMAIL, CommonApplication.getEmail());
        params.put(CommonApplication.EndPointParams.GCM_TOKEN,CommonApplication.getGcmRegId());
        params.put(CommonApplication.EndPointParams.STATUS, status);

        final Intent serviceIntent = new Intent(this, ServerService.class);
        serviceIntent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
        serviceIntent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, statusType);
        serviceIntent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));
        // Starts the IntentService
        startService(serviceIntent);
        return true;
    }

    private boolean sendDoorTasktoArduino(String doorTask){

        char data;
        switch(doorTask){
            case CommonApplication.ControlTask.OPEN:
                Door.open(this,Door.FULL_SPEED);
                break;
            case CommonApplication.ControlTask.CLOSE:
                Door.close(this,Door.FULL_SPEED);
                break;
            case CommonApplication.ControlTask.REPEAT_LAST:
                Door.repeatLast(this);
                break;
            default:
                Door.getStatus(this);
        }

        mDataTextView.append("ARDUINO OUTGOING: " + doorTask + "\n");

        return true;
    }

    private void incomingUsbData(int len, byte[] dataPayload){
                final char protocol =  (char) dataPayload[0];

                if (protocol == Door.SYNC){
                    char command = (char) dataPayload[1];
                    mDataTextView.append("ARDUINO INCOMING: " + command + "\n");
                    switch(command){
                        case Door.IN_OPENING:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.OPENING);
                            mDoorControllerLayout.startOpenDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "Door opening", Snackbar.LENGTH_LONG);
                            mSnackBar.show();
                            break;
                        case Door.IN_CLOSING:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSING);
                            mDoorControllerLayout.startCloseDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "Door closing", Snackbar.LENGTH_LONG);
                            mSnackBar.show();
                            break;
                        case Door.IN_OPENED:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.OPENED);
                            mDoorControllerLayout.startOpenDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "Door open", Snackbar.LENGTH_LONG);
                            mSnackBar.show();

                            // whenever door becomes opened, we send a photo.
                            if (mBound) mCameraManager.takePhoto();

                            break;
                        case Door.IN_CLOSED:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSED);
                            mDoorControllerLayout.startCloseDoorAnim();

                            if (mSnackBar!=null && mSnackBar.isShown()) mSnackBar.dismiss();
                            mSnackBar = Snackbar.make(mCoordinatorLayout, "Door closed", Snackbar.LENGTH_LONG);
                            mSnackBar.show();

                            // whenever door becomes closed, we send a photo.
                            if (mBound) mCameraManager.takePhoto();

                            break;
                        case Door.IN_STALLED_OPENING:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.STALLED_OPENING);
                            break;
                        case Door.IN_STALLED_CLOSING:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.STALLED_CLOSING);
                            break;
                        case Door.IN_OPEN_ASSIST:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.OPEN_ASSIST);
                            break;
                        case Door.IN_CLOSE_ASSIST:
                            sendStatusToServer(CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSE_ASSIST);
                            break;
                        case Door.IN_REPEAT_LAST:
                            if (mLastControlTaskCommand!=null) sendDoorTasktoArduino(mLastControlTaskCommand);
                            break;
                        default:
                            // this means that there was a communication error.
                            sendDoorTasktoArduino(CommonApplication.ControlTask.REPEAT_LAST);
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

    /**
     * Called when CameraService gets some data coming in.
     */
    private class CameraBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent!=null){
                byte[] data = intent.getByteArrayExtra(CameraManagerService.EXTRA_IMAGE_DATA);
                if (data!=null)saveAndSendPhoto(data);


            }


        }
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

                // updateTimer(dataPayload);
            }
        }
    };


    /** Handles various events fired by the {@code BluetoothLeConnectionService}
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    // ACTION_SCAN_FOR_SPECIFIC_DEVICE_FAILED the scan for the saved BT device failed.
    **/
    private final BroadcastReceiver mBluetoothUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeConnectionService.ACTION_GATT_CONNECTED.equals(action)) {
                mBluetoothConnected = true;
                setUiBluetoothState(true);

                // Start wifi scan.
/*                WifiDevicesTrawler trawler = new WifiDevicesTrawler(MainActivity.this);
                trawler.update(new WifiTrawlerCallback() {
                    public void onFinish(List<WifiDevice> devices) {
                        Set<String> macs = WifiDevice.loadSavedDevices(MainActivity.this);
                        String m;
                        int i = 0;
                        for (WifiDevice d : devices) {
                            m = WifiDevice.cleanMacAddress(d.macAddress);
                            if (macs.contains(m)) i++;
                        }

                        if (i == macs.size()) {
                            // all registered wifi devices were on the network - people are in.
                            // do nothing.
                        } else if (i == 0) {
                            //No registered devices were on the network. Nobody in.
                            // open the door.
                            sendDoorTasktoArduino(CommonApplication.ControlTask.OPEN);
                            sendStatusToServer(CommonApplication.BLUETOOTH_STATUS, CommonApplication.Bluetooth.BLUETOOTH_CONNECTED);
                        } else {
                            // somebody is in. think nothing need happen in this scenario.
                        }

                    }
                });*/
            } else if (BluetoothLeConnectionService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBluetoothConnected = false;
                setUiBluetoothState(false);

                // Start wifi scan.
/*                WifiDevicesTrawler trawler = new WifiDevicesTrawler(MainActivity.this);
                trawler.update(new WifiTrawlerCallback(){
                    public void onFinish(List<WifiDevice> devices){
                        Set<String> macs = WifiDevice.loadSavedDevices(MainActivity.this);
                        String m;
                        int i=0;
                        for (WifiDevice d:devices){
                            m = WifiDevice.cleanMacAddress(d.macAddress);
                            if (macs.contains(m)) i++;
                        }

                        if (i==macs.size()){
                            // all registered wifi devices were on the network - people are in.
                            // do nothing.
                        } else if (i==0){
                            //No registered devices were on the network. Nobody in.
                            // close the door.
                            sendDoorTasktoArduino(CommonApplication.ControlTask.CLOSE);
                            sendStatusToServer(CommonApplication.BLUETOOTH_STATUS, CommonApplication.Bluetooth.BLUETOOTH_DISCONNECTED);
                        } else {
                            // somebody is in. think nothing need happen in this scenario.
                        }

                    }
                });*/


            } else if (BluetoothLeConnectionService.ACTION_SCAN_FOR_SPECIFIC_DEVICE_FAILED.equals(action)){
                Toast.makeText(MainActivity.this,"Scan for Bluetooth Device Failed",Toast.LENGTH_LONG);
            } else if (BluetoothLeConnectionService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // TODO:
            } else if (BluetoothLeConnectionService.ACTION_DATA_AVAILABLE.equals(action)) {
                // TODO:
            }
        }
    };

    private void saveAndSendPhoto(byte[] data) {
        new SavePhotoTask().execute(data);
    }

    class SavePhotoTask extends AsyncTask<byte[], String, String[]> {
        @Override
        protected String[] doInBackground(byte[]... jpeg) {
            String[] fileDetails = new String[2];
            fileDetails[0] = "photo.jpg";
            File photo=new File(Environment.getExternalStorageDirectory(), fileDetails[0] );
            fileDetails[1] =photo.getAbsolutePath();
            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return fileDetails;
        }

        protected void onPostExecute(String[] result) {

            Map<String,String> params = new HashMap<>();
            params.put(CommonApplication.EndPointParams.IMAGE_FILENAME,result[0]);
            params.put(CommonApplication.EndPointParams.IMAGE_FILEPATH,result[1]);

            final Intent serviceIntent = new Intent(MainActivity.this, ServerService.class);
            serviceIntent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
            serviceIntent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, CommonApplication.IMAGE_UPLOAD);
            serviceIntent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));
            // Starts the IntentService
            startService(serviceIntent);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CameraManagerService.LocalBinder binder = (CameraManagerService.LocalBinder) service;
            mCameraManager = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
