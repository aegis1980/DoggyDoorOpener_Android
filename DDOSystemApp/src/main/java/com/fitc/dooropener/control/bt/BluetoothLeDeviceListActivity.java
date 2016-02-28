/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fitc.dooropener.control.bt;
 
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;


import com.fitc.dooropener.control.R;

import java.util.ArrayList;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class BluetoothLeDeviceListActivity extends Activity {

    public static final int REQUEST_CONNECT_DEVICE = 1;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
 
    /**
     * Tag for Log
     */
    private static final String TAG = "BluetoothLeDeviceListActivity";


    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name" ;
 
    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;
 
    /**
     * Newly discovered devices
     */
    private ArrayAdapter<BluetoothDevice> mNewDevicesArrayAdapter;
    private ArrayList<BluetoothDevice> mBtLeScanDevices = new ArrayList<>();

    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLeScanner mBtLeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
 
        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);


        // Defines a Handler object that's attached to the UI thread
        mHandler = new Handler(Looper.getMainLooper());

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mScanning) {
                    scanLeDevice(true);
                }
                v.setVisibility(View.GONE);
            }
        });
 
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mNewDevicesArrayAdapter = new DeviceDataArrayAdapter(this, R.layout.device_name, mBtLeScanDevices);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
 
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtLeScanner = mBtAdapter.getBluetoothLeScanner();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
 
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null ) {
           scanLeDevice(false);
        }
    }
 
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBtLeScanner.stopScan(mLeScanCallback);
                    setProgressBarIndeterminateVisibility(false);
                    setTitle(R.string.select_device);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBtLeScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtLeScanner.stopScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BluetoothDevice device = result.getDevice();

                            // If it's already in list, skip it, because it's been listed already
                            // BluetoothDevice.equals() goes by MAC address.
                            if (!mBtLeScanDevices.contains(device)) {
                                mNewDevicesArrayAdapter.add(device);
                                mNewDevicesArrayAdapter.notifyDataSetChanged();
                            }

                        }
                    });
                }
            };
 
    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long arg3) {
            // Cancel scan because it's costly and we're about to connect
            scanLeDevice(false);
 

            BluetoothDevice device = mBtLeScanDevices.get(position);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
            intent.putExtra(EXTRA_DEVICE_NAME, device.getName());
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
 

}