package com.fitc.dooropener.control.bt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.fitc.dooropener.lib.CommonApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * My lazy implementation that checks every X seconds whether registered BT device in still in range or not.
 * Nothing more.
 */
public class BluetoothLeConnectionService extends Service {
    public static final String TAG = BluetoothLeConnectionService.class.getSimpleName();

    public final static String ACTION_GATT_CONNECTED =
            "com.fitc.dooropener.control.bt.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.fitc.dooropener.control.bt.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.fitc.dooropener.control.bt.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.fitc.dooropener.control.bt.ACTION_DATA_AVAILABLE";
    public final static String ACTION_SCAN_FOR_SPECIFIC_DEVICE_FAILED =
            "com.fitc.dooropener.control.bt.ACTION_SCAN_FOR_SPECIFIC_DEVICE_FAILED";
    public final static String EXTRA_DATA =
            "com.fitc.dooropener.control.bt.EXTRA_DATA";

    /**
     *
     *
     * time interval between consecutive scans
     */
    private static final long SCAN_INTERVAL = 30 * 1000;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    /**
     * Member fields
     */

    /**
     * Newly discovered devices
     */
    private ArrayList<BluetoothDevice> mBtLeScanDevices = new ArrayList<>();

    private boolean mScanning;

    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mBtLeScanner;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;




    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // schedule scan to stop in 10secs whatever
            mServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBtLeScanner.stopScan(mLeScanCallback);
                    broadcastUpdate(ACTION_SCAN_FOR_SPECIFIC_DEVICE_FAILED);
                }
            }, SCAN_INTERVAL);

            final String address = CommonApplication.Bluetooth.getBluetoothDeviceMacAddress();

            if (address != null) {

                mScanning = true;

                ScanFilter.Builder fBuilder = new ScanFilter.Builder();
                fBuilder.setDeviceAddress(address);
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(fBuilder.build());

                ScanSettings.Builder sBuilder = new ScanSettings.Builder();
                //   sBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
                //    sBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
                sBuilder.build();

                mBtLeScanner.startScan(filters, sBuilder.build(), mLeScanCallback);
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtLeScanner = mBtAdapter.getBluetoothLeScanner();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    //***************************************************************************************************
    // Device scan callback.
    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {

                    BluetoothDevice device = result.getDevice();
                    if (device.getAddress().equals(CommonApplication.Bluetooth.getBluetoothDeviceMacAddress())){
                        mBluetoothGatt = device.connectGatt(BluetoothLeConnectionService.this, true, mGattCallback);
                    }
                }
            };


    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        Log.i(TAG, "Connected to GATT server.");
                        broadcastUpdate(intentAction);

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }
            };

}
