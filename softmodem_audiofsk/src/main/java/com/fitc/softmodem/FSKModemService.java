package com.fitc.softmodem;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.xi.audiofsk.FSKModem;
import org.xi.audiofsk.FSKModemListener;

/**
 * Created by Jon on 19/10/2015.
 */
public class FSKModemService extends Service  {
    private static final String TAG = "FSKModemService";
    private static final boolean DEBUG = true;

    public static final String ACTION_FSKMODEM_DATA_INCOMING = "com.fitc.softmodem.ACTION_FSKMODEM_DATA_INCOMING";
    public static final String EXTRA_FSKMODEM_DATA_INCOMING = "com.fitc.softmodem.EXTRA_FSKMODEM_DATA_INCOMING";

    public static final String ACTION_FSKMODEM_DATA_TO_SEND = "com.fitc.softmodem.ACTION_FSKMODEM_DATA_TO_SEND";
    public static final String EXTRA_FSKMODEM_DATA_TO_SEND = "com.fitc.softmodem.EXTRA_FSKMODEM_DATA_TO_SEND";


    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    FSKModem mModem;



    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler implements FSKModemListener{
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            String command = (String) msg.obj;

            mModem.writeBytes(command.getBytes());
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //stopSelf(msg.arg1);

        }

        @Override
        public void dataReceivedFromFSKModem(byte[] data) {
            Log.d(TAG,"dataReceivedFromFSKModem.");


            StringBuilder sb = new StringBuilder();
            for(int i=0; data != null && i < data.length; i++) {
                int v = data[i] & 0xff;
                if (v == 0xff) {
                    continue;
                }
                if (v > 31 && v < 127) {
                    sb.append((char)v);
                } else {
                    if (v < 16) {
                        sb.append(" 0");
                        sb.append(Integer.toHexString(v));
                        sb.append(' ');
                    } else {
                        sb.append(' ');
                        sb.append(Integer.toHexString(v));
                        sb.append(' ');
                    }
                }
            }

            String command = sb.toString();

            if (command!=null && !command.equals("")) {
                // Broadcast incoming data to whoever is listening.
                command = command.trim();
                Intent intent = new Intent(ACTION_FSKMODEM_DATA_INCOMING);
                intent.putExtra(EXTRA_FSKMODEM_DATA_INCOMING, command);
                LocalBroadcastManager.getInstance(FSKModemService.this).sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Initialise soft-modem.
        if (mModem==null) mModem = new FSKModem();
        mModem.start();
        mModem.addDataReceiver(mServiceHandler);
        if (DEBUG) FSKModem.debugPrint(mModem);

        // Set audio volume to maximum.
        AudioManager am =
                (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "service starting");


        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        if (intent != null) {
            // start modem listening is not already.
            if (mModem != null) {
                mModem.start();
         //       mModem.addDataReceiver(mServiceHandler);
                Log.i(TAG,"FSK modem started");
            }

            String action = intent.getAction();
            if (action != null) {
              if (action.equals(FSKModemService.ACTION_FSKMODEM_DATA_TO_SEND)) {
                    Message msg = mServiceHandler.obtainMessage();
                    msg.arg1 = startId;
                    msg.obj = intent.getStringExtra(FSKModemService.EXTRA_FSKMODEM_DATA_TO_SEND);

                    mServiceHandler.sendMessage(msg);
                }
            }
        }
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        mModem.stop();
        mModem = null;
    }

    //**********************************************************************************************


}