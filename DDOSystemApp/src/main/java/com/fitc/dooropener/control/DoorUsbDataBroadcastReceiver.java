package com.fitc.dooropener.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.server.ServerService;
import com.fitc.usbconnectionlibrary.UsbConnectionService;

/**
 * Receives any usb data FROM the arduino for the door.
 */
public class DoorUsbDataBroadcastReceiver extends BroadcastReceiver {
    public DoorUsbDataBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (UsbConnectionService.ACTION_USB_DATA_INCOMING.equals(action)) {
            int len = intent.getIntExtra(UsbConnectionService.EXTRA_USB_INCOMING_DATA_PAYLOAD_LENGTH, 0);
            byte[] dataPayload = intent.getByteArrayExtra(UsbConnectionService.EXTRA_USB_INCOMING_DATA_PAYLOAD);
            final char protocol =  (char) dataPayload[0];
            if (len>0 && protocol == DoorArduino.SYNC){
                onDataInUsbFromDoorArduino(context, len,dataPayload);
            }
        }
    }

    public static void onDataInUsbFromDoorArduino(Context context, int len, byte[] dataPayload) {

        char command = (char) dataPayload[1];
        switch(command){
            case DoorArduino.IN_OPENING:
                ServerService.sendStatusToServer(context, CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.OPENING);
                break;
            case DoorArduino.IN_CLOSING:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSING);
                break;
            case DoorArduino.IN_OPENED:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.OPENED);
                DeviceCamera.takePhoto(context);
                break;
            case DoorArduino.IN_CLOSED:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSED);
                DeviceCamera.takePhoto(context);
                break;
            case DoorArduino.IN_STALLED_OPENING:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.STALLED_OPENING);
                DeviceCamera.takePhoto(context);
                break;
            case DoorArduino.IN_STALLED_CLOSING:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.STALLED_CLOSING);
                DeviceCamera.takePhoto(context);
                break;
            case DoorArduino.IN_OPEN_ASSIST:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.OPEN_ASSIST);
                DeviceCamera.takePhoto(context);
                break;
            case DoorArduino.IN_CLOSE_ASSIST:
                ServerService.sendStatusToServer(context,CommonApplication.DOOR_STATUS, CommonApplication.DoorAction.CLOSE_ASSIST);
                DeviceCamera.takePhoto(context);
                break;
            case DoorArduino.IN_REPEAT_LAST:
             //   if (sLastControlTaskCommand!=null) sendTask(sLastControlTaskCommand);
                break;
            default:
                // this means that there was a communication error.
                DoorArduino.sendTask(context,CommonApplication.ControlTask.REPEAT_LAST);
                break;
        }

    }
}
