package com.fitc.weatherbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fitc.usbconnectionlibrary.UsbConnectionService;

import java.util.Arrays;

/**
 * Created by Jon on 21/01/2017.
 */

public class WeatherBoxBroadcastReceiver extends BroadcastReceiver {


    private static final String TAG = WeatherBoxBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"On Receive...");
        int len = intent.getIntExtra(UsbConnectionService.EXTRA_USB_INCOMING_DATA_PAYLOAD_LENGTH, 0);
        byte[] dataPayload = intent.getByteArrayExtra(UsbConnectionService.EXTRA_USB_INCOMING_DATA_PAYLOAD);

        final char protocol = (char) dataPayload[0];

        if (len>0 && protocol==UsbProtocol.SYNC){
            Log.d(TAG,"...incoming data.");
            byte[] incoming = Arrays.copyOfRange(dataPayload, 1, dataPayload.length);
            WeatherBoxService.processIncomingData(context,incoming);
        }
    }
}
