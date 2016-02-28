package com.fitc.wifitrawl;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;


public class WifiDeviceListActivity extends ListActivity implements SwipeRefreshLayout.OnRefreshListener, WifiTrawlerCallback {

    public static final int REQUEST_DEVICES = 3465;
    private ArrayList<WifiDevice> mDevices;
    private WifiDevicesTrawler mNetworkDeviceTrawler;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WifiDeviceArrayAdapter mDeviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifidevices);

        // Set result in case the user backs out
        // Just a blank intent. Re read current devices in main activity,
        setResult(Activity.RESULT_OK);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);


        mDevices = new ArrayList<>();
        mDeviceAdapter =  new WifiDeviceArrayAdapter(this, R.layout.network_device, mDevices);;
        mNetworkDeviceTrawler = new WifiDevicesTrawler(this,mDevices);


        setListAdapter(mDeviceAdapter);

        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                updateDevices();
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

 //


    }

    @Override
    public void onRefresh(){
      //  mSwipeRefreshLayout.setRefreshing(true);
        updateDevices();
    }

    private void updateDevices() {
        mNetworkDeviceTrawler.update(this);


    }

    @Override
    public void onFinish(List<WifiDevice> devcies) {
        mSwipeRefreshLayout.setRefreshing(false);
        mDeviceAdapter.notifyDataSetChanged();
    }
}
