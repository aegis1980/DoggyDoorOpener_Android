package com.fitc.wifitrawl;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jon on 23/02/2016.
 */
public class WifiDevice {
    public String deviceName;
    public String macAddress;
    public String ipAddress;
    public boolean isCurrentlyConnected;
    public boolean isSelected;

    @Override
    public boolean equals(Object o){
        WifiDevice thatDevice = null;
        if (o instanceof WifiDevice){
            return false;
        } else {
            thatDevice = (WifiDevice) o;
        }

        if (!this.macAddress.equals(thatDevice.macAddress)) return false;

        return true;

    }

    public static String cleanMacAddress(String address){
        return address.trim().replaceAll(":","").toUpperCase();
    }

    public static Set<String> saveToSharedPreferences(Context context, WifiDevice device){

        final String mac = cleanMacAddress(device.macAddress);

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.saved_devices_file_key), Context.MODE_PRIVATE);

        // get current saved devices (saved at cleaned up mac address)
        Set<String> savedDevices = sharedPref.getStringSet(context.getString(R.string.saved_devices), new HashSet<String>());

        // add new device and save.
        if (!savedDevices.contains(mac)){
            savedDevices.add(mac);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putStringSet(context.getString(R.string.saved_devices), savedDevices);
            editor.commit();
        }

        return savedDevices;

    }


    public static Set<String> removeFromSharedPreferences(Context context, WifiDevice device) {

        final String mac = cleanMacAddress(device.macAddress);

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.saved_devices_file_key), Context.MODE_PRIVATE);

        // get current saved devices (saved at cleaned up mac address)
        Set<String> savedDevices = sharedPref.getStringSet(context.getString(R.string.saved_devices), new HashSet<String>());

        // add new device and save.
            savedDevices.remove(mac);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(context.getString(R.string.saved_devices));
            editor.commit();
            editor.putStringSet(context.getString(R.string.saved_devices), savedDevices);
            editor.commit();


        return savedDevices;
    }

    public static Set<String> loadSavedDevices(Context context) {

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.saved_devices_file_key), Context.MODE_PRIVATE);

        return sharedPref.getStringSet(context.getString(R.string.saved_devices), new HashSet<String>());
    }

}
