package com.fitc.dooropener.lib;

import android.accounts.Account;
import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.fitc.dooropener.lib.credentials.VolleyAuthIntentService;
import com.fitc.dooropener.lib.gcm.GcmDataPayload;
import com.fitc.dooropener.lib.gcm.GcmRegisterService;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GoogleApiAvailability;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();
    private static final int ACCOUNTPICKER_REQUEST_CODE = 67;

    private BroadcastReceiver mAuthReceiver;
    private ServerResponseReceiver mServerResponseReceiver;
    private GcmRegisterReceiver mGcmRegisterReceiver;
    private GcmStatusBroadcastReceiver mGcmStatusReceiver;

    protected CoordinatorLayout mCoordinatorLayout;
    private String mAccountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String storedAccountName = CommonApplication.getStoredAccountName();
        boolean accountFound = false;
        if (storedAccountName == null) {
            accountFound = false;
        } else { // there
            mAccountName = storedAccountName;
            accountFound = startAccountAuth();
            accountFound = true;
        }

        if (!accountFound){

            // This starts the built in Android account picket
            // calls onActivityResult when done.
            Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                    false, null, null, null, null);
            startActivityForResult(intent, ACCOUNTPICKER_REQUEST_CODE);
        }

    }



    @Override
    public void onResume() {
        super.onResume();
        if (mAuthReceiver == null) mAuthReceiver = new AuthReceiver();
        IntentFilter intentFilter0 = new IntentFilter(CommonApplication.ACTION_AUTH_RESPONSE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mAuthReceiver, intentFilter0);

        if (mServerResponseReceiver == null) mServerResponseReceiver = new ServerResponseReceiver();
        IntentFilter intentFilter = new IntentFilter(CommonApplication.ACTION_SERVER_RESPONSE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mServerResponseReceiver, intentFilter);

        if (mGcmRegisterReceiver == null) mGcmRegisterReceiver = new GcmRegisterReceiver();
        IntentFilter intentFilter2 = new IntentFilter(GcmRegisterService.ACTION_GCM_REGISTER);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGcmRegisterReceiver, intentFilter2);

        if (mGcmStatusReceiver == null) mGcmStatusReceiver = new GcmStatusBroadcastReceiver();
        IntentFilter intentFilter3 = new IntentFilter(CommonApplication.ACTION_GCM_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGcmStatusReceiver, intentFilter3);


    }

    @Override
    public void onPause() {
        super.onPause();

        // AuthReceiver deals with auth on server side.
        if (mAuthReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mAuthReceiver);
        if (mServerResponseReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mServerResponseReceiver);
        if (mGcmRegisterReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mGcmRegisterReceiver);
        if (mGcmStatusReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mGcmStatusReceiver);
    }


    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        if (requestCode == ACCOUNTPICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.i(TAG, "Setting account name: " + mAccountName);
            mAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            CommonApplication.setStoredAccountName(mAccountName);
            if (CommonApplication.USE_AUTHORISATION){
                startAccountAuth();
            } else {
                Intent i = new Intent(BaseActivity.this, GcmRegisterService.class);
                startService(i);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission for GET_ACCOUNTS granted
                    startAccountAuth();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "WRITE_CONTACTS Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //**********************************************************************************************
    // Helper Methods.

    /**
     * Looks for account on Android system and if it finds it passes on
     * to {@link VolleyAuthIntentService} for server-side auth.
     * @return accountFounf whether account found in Android system
     */
    private boolean startAccountAuth() {
        Log.d(TAG,"startAccountAuth: " + mAccountName );

        // Marshmellow: have to gain GET_ACCOUNTS permission at runtime.
        // if permission on yet granted then accounts stuff deferred to onRequestPermissionsResult

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           boolean havePermission = haveGetAccountPermission();
           if (!havePermission){
                Log.d(TAG,"No GET_ACCOUNT permission");
               return false;
          } else {
               Log.d(TAG,"Have GET_ACCOUNT permission");
           }
        }
        boolean accountFound = false;
        AccountManager accountManager = AccountManager.get(this);
       // Account[] accounts = accountManager.getAccountsByType("com.google");
        Account[] accounts = accountManager.getAccounts();
        Log.v(TAG,"Found " + accounts.length + " accounts.");
        for (Account account : accounts) {
            Log.v(TAG,"Account Name:" + account.name);
            if (account.name.equals(mAccountName)) {
                /*
                 * Creates a new Intent to start the AuthIntentService
                 * passes account as an extra.
                 */
                Intent intent = new Intent(this, VolleyAuthIntentService.class);
                intent.putExtra(VolleyAuthIntentService.ACCOUNT_EXTRA, account);

                // Starts the IntentService
                startService(intent);
                accountFound = true;
                CommonApplication.setStoredAccountName(mAccountName);
                break;
            }
        }
        return accountFound;
    }

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @TargetApi(23)
    private boolean haveGetAccountPermission() {
        Log.i(TAG, "haveGetAccountPermission()");
        int hasWriteContactsPermission = ActivityCompat.checkSelfPermission(this,Manifest.permission.GET_ACCOUNTS);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.GET_ACCOUNTS},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return false;
        } else {
            return true;
        }
    }


    //**********************************************************************************************
    // abstract methods

    public abstract void onServerResponse(Context context, Intent intent);

    public abstract void onGcmStatus(GcmDataPayload status);

    //**********************************************************************************************

    /**
     * From {@link VolleyAuthIntentService}
     */
    private class AuthReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean authSuccess = intent.getBooleanExtra(CommonApplication.EXTRA_AUTH_RESPONSE_SUCCESS, false);

            if (authSuccess) {
                // Start service to sort out Google Cloud messgaing registration and stuff.
                // uses GcmRegisterReceiver to make get what happens back with this activity.
                Intent i = new Intent(BaseActivity.this, GcmRegisterService.class);
                startService(i);
            }
        }
    }



    private class ServerResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()!=null && intent.getAction().equals(CommonApplication.ACTION_SERVER_RESPONSE_AUTHERROR)){
                int sc = intent.getIntExtra(CommonApplication.EXTRA_SERVER_RESPONSE_MESSAGE,-1);

                Snackbar.make(mCoordinatorLayout, "There was an authentication error - type " + sc, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Action", null).show();
            }

            // abstract method for Activity specific implementations.
            onServerResponse(context, intent);
        }
    }

    /**
     * Gets intent local broadcast by {@link com.fitc.dooropener.lib.gcm.MyGcmListenerService} when a gcm message comes in.
     */
    private class GcmStatusBroadcastReceiver extends BroadcastReceiver {

        private static final String TAG = "StatusBroadcastReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            String statusJson = intent.getStringExtra(CommonApplication.EXTRA_GCM_STATUS_JSON);
            GcmDataPayload status = GcmDataPayload.makeFromJson(statusJson);
            onGcmStatus(status);
        }
    }

    private class GcmRegisterReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("GcmRegisterReceiver", "onReceive");
            if (intent.getAction().equals(GcmRegisterService.ACTION_GCM_REGISTER)) {
                switch (intent.getIntExtra(GcmRegisterService.EXTRA_GCM_REGISTER, GcmRegisterService.GOOGLE_PLAY_USER_RESOLVABLE_ERROR)) {
                    case GcmRegisterService.GCM_REGISTER_SUCCESSFUL:
                        Toast.makeText(BaseActivity.this, "GCM registration successful.", Toast.LENGTH_LONG).show();
                        break;
                    case GcmRegisterService.GCM_TOKEN_ERROR:
                        Toast.makeText(BaseActivity.this, "GCM registration token error.", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                        int resultCode = apiAvailability.isGooglePlayServicesAvailable(BaseActivity.this);
                        apiAvailability.getErrorDialog(BaseActivity.this, resultCode, GcmRegisterService.PLAY_SERVICES_RESOLUTION_REQUEST)
                                .show();

                }
            }
        }

    }


}
