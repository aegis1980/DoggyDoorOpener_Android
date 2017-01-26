package com.fitc.dooropener.control;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Jon on 23/01/2017.
 */
public class DeviceCamera {
    public static void takePhoto(Context context) {
        Intent i = new Intent(context, DeviceCameraService.class);
        i.setAction(DeviceCameraService.ACTION_DEVICECAMERA_TAKE_PHOTO);
        context.startService(i);

    }
}
