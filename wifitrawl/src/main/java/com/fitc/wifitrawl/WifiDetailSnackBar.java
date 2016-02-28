package com.fitc.wifitrawl;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Created by Jon on 25/02/2016.
 */
public class WifiDetailSnackBar

{

   public static void  make(Context c, View v){


       // get mac address
       WifiManager manager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
       WifiInfo info = manager.getConnectionInfo();
       String macddress = WifiUtils.getMACAddress("wlan0");
       String ipAddress = WifiUtils.getIPAddress(true);


        Snackbar.make(v,
                "MAC: " + macddress +"\n" +
                "IP:" + ipAddress
                , Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
}


}
