package com.fitc.dooropener.control;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.gcm.GcmDataPayload;

public class IncomingGcmBroadcastReceiver extends BroadcastReceiver {
    public IncomingGcmBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String statusJson = intent.getStringExtra(CommonApplication.EXTRA_GCM_STATUS_JSON);
        GcmDataPayload payload = GcmDataPayload.makeFromJson(statusJson);
        if(payload.getStatusType()== GcmDataPayload.Status.COMMAND_TO_CONTROL){
            String command = payload.getStatusData();
            switch(command){
                case CommonApplication.ControlTask.DOORARDUINO_OPEN:
                case CommonApplication.ControlTask.DOORARDUINO_CLOSE:
                case CommonApplication.ControlTask.DOORARDUINO_STATUS:
                    DoorArduino.sendTask(context,command);
                    break;
                case CommonApplication.ControlTask.DEVICECAMERA_TAKEPHOTO:
                    DeviceCamera.takePhoto(context);
                    break;
                case CommonApplication.ControlTask.BT_STATUS:

            }
        }
    }
}
