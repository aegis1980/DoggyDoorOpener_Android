package com.fitc.wifitrawl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class WifiDeviceArrayAdapter extends ArrayAdapter<WifiDevice> {
    private static final String TAG = WifiDeviceArrayAdapter.class.getSimpleName();
    private final Context context;
    private final ArrayList<WifiDevice> data;
    private final int layoutResourceId;
    private final String mMyMacddress;
    private Set<String> mSavedDevices;

    public WifiDeviceArrayAdapter(Context context, int layoutResourceId, ArrayList<WifiDevice> data) {
        super(context, -1, data);
        this.context = context;
        this.data = data;
        this.layoutResourceId = layoutResourceId;

        // get mac address
        mMyMacddress = WifiUtils.getMACAddress("wlan0").trim().replaceAll(":","").toUpperCase();

        mSavedDevices = WifiDevice.loadSavedDevices(context);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        ViewHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.textViewName = (TextView)row.findViewById(R.id.textview_devicename);
            holder.textViewMac = (TextView)row.findViewById(R.id.textview_macaddress);
            holder.textIpAddress= (TextView)row.findViewById(R.id.textview_ipaddress);
            holder.checkSelect = (CheckBox)row.findViewById(R.id.checkbox_selected);

            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        WifiDevice d = data.get(position);

        holder.textViewName.setText(d.deviceName);
        holder.textViewMac.setText(d.macAddress);
        holder.textIpAddress.setText(d.ipAddress);

        // Check if the device is this device - cannot select it if it is.
        boolean isMe = false;
        String deviceMac = WifiDevice.cleanMacAddress(d.macAddress);

        if (deviceMac.equals(mMyMacddress)){
            row.setBackgroundColor(Color.LTGRAY);
            isMe = true;
        } else {
            row.setBackgroundColor(Color.TRANSPARENT);
        }

        //Check if in saved devcies or not
        if (mSavedDevices.contains(deviceMac)){
            d.isSelected=true;
        }


        if (!isMe) {
            holder.checkSelect.setVisibility(View.VISIBLE);
            holder.checkSelect.setChecked(d.isSelected);
            holder.checkSelect.setTag(d);
            holder.checkSelect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    WifiDevice device = (WifiDevice) cb.getTag();
                    device.isSelected = cb.isChecked();
                    if(device.isSelected) {
                        mSavedDevices = WifiDevice.saveToSharedPreferences(context, device);
                    } else {
                        mSavedDevices = WifiDevice.removeFromSharedPreferences(context,device);
                    }
                }
            });
        } else {
            holder.checkSelect.setVisibility(View.INVISIBLE);
        }
        return row;
    }

    static class ViewHolder {
        CheckBox checkSelect;
        TextView textViewName;
        TextView textViewMac;
        TextView textIpAddress;
    }
} 
