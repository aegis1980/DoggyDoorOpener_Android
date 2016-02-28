package com.fitc.dooropener.control.bt;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fitc.dooropener.control.R;

import java.util.ArrayList;

public class DeviceDataArrayAdapter extends ArrayAdapter<BluetoothDevice> {
  private final Context context;
  private final ArrayList<BluetoothDevice> data;
  private final int layoutResourceId;

  public DeviceDataArrayAdapter(Context context, int layoutResourceId, ArrayList<BluetoothDevice> data) {
    super(context, -1, data);
    this.context = context;
    this.data = data;
      this.layoutResourceId = layoutResourceId;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

      View row = convertView;
      ViewHolder holder = null;

      if(row == null)
      {
          LayoutInflater inflater = ((Activity)context).getLayoutInflater();
          row = inflater.inflate(layoutResourceId, parent, false);

          holder = new ViewHolder();
          holder.textViewName = (TextView)row.findViewById(R.id.textview_devicename);
          holder.textViewMac = (TextView)row.findViewById(R.id.textview_macaddress);


          row.setTag(holder);
      }
      else
      {
          holder = (ViewHolder)row.getTag();
      }

      BluetoothDevice d = data.get(position);

      holder.textViewName.setText(d.getName());
      holder.textViewMac.setText(d.getAddress());


      return row;
  }

    static class ViewHolder
    {
        TextView textViewName;
        TextView textViewMac;

    }
} 
