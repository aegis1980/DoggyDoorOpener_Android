package com.fitc.dooropener.control;

import android.content.Context;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.usbconnectionlibrary.UsbConnectionService;

/**
 * Created by Jon on 26/11/2016.
 */

public class DoorArduino implements ArduinoUsbProtocol.UsbProtocol {

    /**
     * Greatest PWM value you can sent to Arduino pin
     */
    private static final int MAXIMUM_VALUE = 255;
    private static final int MINIMUM_VALUE = 0;
    public static final int FULL_SPEED = MAXIMUM_VALUE;

    private static String sLastControlTaskCommand = null;

    private DoorArduino(){

    }

    public static boolean sendTask(Context c, String doorTask){

        char data;
        switch(doorTask){
            case CommonApplication.ControlTask.DOORARDUINO_OPEN:
                DoorArduino.open(c, DoorArduino.FULL_SPEED);
                break;
            case CommonApplication.ControlTask.DOORARDUINO_CLOSE:
                DoorArduino.close(c, DoorArduino.FULL_SPEED);
                break;
            case CommonApplication.ControlTask.REPEAT_LAST:
                DoorArduino.repeatLast(c);
                break;
            default:
                DoorArduino.getStatus(c);
        }

     //   mDataTextView.append("ARDUINO OUTGOING: " + doorTask + "\n");

        return true;
    }

    private static void open(Context c){
        open(c, DoorArduino.FULL_SPEED);
    }


    private static void close(Context c){
        close(c, DoorArduino.FULL_SPEED);
    }

    private static void open(Context c,int speed) {
        speed = modulate(speed);
        byte[] msg = new byte[3];
        msg[0] = SYNC;
        msg[1] = OUT_OPEN;
        msg[2] = modulate(speed);
        UsbConnectionService.sendData(c, msg);
    }

    private static void close(Context c,int speed) {
        speed = modulate(speed);
        byte[] msg = new byte[3];
        msg[0] = SYNC;
        msg[1] = OUT_CLOSE;
        msg[2] = modulate(speed);
        UsbConnectionService.sendData(c, msg);
    }

    private static void getStatus(Context c) {
        byte[] msg = new byte[3];
        msg[0] = SYNC;
        msg[1] = OUT_STATUS;
        UsbConnectionService.sendData(c, msg);
    }

    private static  void repeatLast(Context c){
        byte[] msg = new byte[3];
        msg[0] = SYNC;
        msg[1] = OUT_REPEAT_LAST;
        UsbConnectionService.sendData(c, msg);

    }

    /**
     * Contrains input to 0-255 (pos)
     * then casts input int to byte between -128 and 127
     * @param i
     * @return
     */
    private static byte modulate(int i) {
        i = Math.abs(i);
        if (i>MAXIMUM_VALUE) i = MAXIMUM_VALUE;
        if (i<MINIMUM_VALUE) i = MINIMUM_VALUE;

        // convert to byte
        byte b = (byte) (i - 128);

        return b;
    }


}
