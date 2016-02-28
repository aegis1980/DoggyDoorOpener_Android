package com.fitc.dooropener.lib.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.fitc.dooropener.lib.CommonApplication;

/**
 * Created by Jon on 20/01/2016.
 */
public class WifiStateChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        boolean wifi = wifiManager.isWifiEnabled();

        if (!wifi){
            wifiManager.setWifiEnabled(true);
        } else { // have wifi  now
            ((CommonApplication) context).executeQueuedErrorRequests();
        }

    }
}
