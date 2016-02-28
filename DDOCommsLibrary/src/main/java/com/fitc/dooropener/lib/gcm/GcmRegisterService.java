package com.fitc.dooropener.lib.gcm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Created by Jon on 12/10/2015.
 */
public class GcmRegisterService extends Service {

    public static final String ACTION_GCM_REGISTER = "com.fitc.dooropener.lib.gcm.ACTION_GCM_REGISTER";
    public static final String EXTRA_GCM_REGISTER = "com.fitc.dooropener.lib.gcm.EXTRA_GCM_REGISTER";
    public static final int GCM_REGISTER_SUCCESSFUL = 1;
    public static final int GOOGLE_PLAY_USER_RESOLVABLE_ERROR = 10;
    public static final int GCM_TOKEN_ERROR = 11;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;


    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = GcmRegisterService.class.getSimpleName();

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(final Message msg) {

            if (checkPlayServices()) {
                // Start IntentService to register this application with GCM.
                Intent intent = new Intent(GcmRegisterService.this, RegistrationIntentService.class);
                startService(intent);
            }

        }


    }

    @Override
    public void onCreate() {
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

        mRegistrationBroadcastReceiver = new RegisterReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting GcmRegisiterService", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "service starting GcmRegisiterService");
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Intent intent = new Intent(GcmRegisterService.ACTION_GCM_REGISTER);
                intent.putExtra(EXTRA_GCM_REGISTER,GOOGLE_PLAY_USER_RESOLVABLE_ERROR);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            } else {
                Log.i(TAG, "This device is not supported.");
                stopSelf();
            }
            return false;
        }
        return true;
    }

    /**
     *
     * Receives intent from {@link RegistrationIntentService}
     */
    private class RegisterReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"RegisterReceiver onReceive");
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            boolean sentToken = sharedPreferences
                    .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
            int code = GCM_TOKEN_ERROR;
            if (sentToken) {
                code = GCM_REGISTER_SUCCESSFUL;
            }

            // to main activity.
            Intent i = new Intent(GcmRegisterService.ACTION_GCM_REGISTER);
            i.putExtra(EXTRA_GCM_REGISTER, code);
            LocalBroadcastManager.getInstance(GcmRegisterService.this).sendBroadcast(intent);

            Intent stopIntent = new Intent(context,GcmRegisterService.class);
            startService(stopIntent);
        }
    }


    }
