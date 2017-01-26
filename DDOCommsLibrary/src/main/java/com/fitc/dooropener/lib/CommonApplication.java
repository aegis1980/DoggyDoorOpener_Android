package com.fitc.dooropener.lib;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alexbbb.uploadservice.UploadService;
import com.android.volley.Request;
import com.fitc.dooropener.lib.server.VolleySingleton;
import com.fitc.dooropener.lib.server.WifiStateChangeReceiver;

import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

/**
 * @author appsrox.com
 *
 */
public class CommonApplication extends Application {

    private static final String TAG = "CommonApplication" ;

    public static final String PROFILE_ID = "profile_id";

    public static final String EXTRA_STATUS = "status";
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 0;

    public static final String ACTION_AUTH_RESPONSE = "com.fitc.dooropener.lib.credentials.ACTION_AUTH_RESPONSE" ;
    public static final String EXTRA_AUTH_RESPONSE_SUCCESS = "com.fitc.dooropener.lib.credentials.EXTRA_AUTH_RESPONSE_SUCCESS" ;
    public static final String ACTION_SERVER_RESPONSE = "com.fitc.dooropener.lib.server.ACTION_SERVER_RESPONSE" ;
    public static final String ACTION_SERVER_RESPONSE_AUTHERROR = "com.fitc.dooropener.lib.server.ACTION_SERVER_RESPONSE_AUTHERROR" ;
    public static final String EXTRA_SERVER_RESPONSE_MESSAGE = "com.fitc.dooropener.lib.server.SERVER_RESPONSE_MESSAGE" ;
    public static final String ACTION_SERVER_REQUEST  = "com.fitc.dooropener.lib.server.ACTION_SERVER_REQUEST" ;
    public static final String EXTRA_SERVER_REQUEST_TYPE_INT = "com.fitc.dooropener.lib.server.EXTRA_SERVER_REQUEST_TYPE_INT" ;
    public static final String BUNDLE_SERVER_REQUEST_PARAMS = "com.fitc.dooropener.lib.server.BUNDLE_SERVER_REQUEST_PARAMS";

    public static final String ACTION_GCM_STATUS = "com.fitc.dooropener.lib.gcm.ACTION_GCM_STATUS" ;
    public static final String EXTRA_GCM_STATUS_JSON = "com.fitc.dooropener.lib.gcm.EXTRA_GCM_STATUS_JSON";

    public static final int REGISTER = 0;
    public static final int UNREGISTER = 1;
    public static final int COMMAND =2;
    public static final int MESSAGE_ME =99;
    public static final int DOOR_STATUS = 3;
    public static final int BLUETOOTH_STATUS = 4 ;


    public static final int IMAGE_UPLOAD = 10;

    /**
     * Length of time on ms that door takes to open and close.
     */
    public static final int DOOR_ANIM_LENGTH = 2000;
    public static final boolean DEBUG = true;

    /**
     * For whether we are using authoriation and authenication with GAE server.
     * Implemented in {@link BaseActivity} and {@link com.fitc.dooropener.lib.credentials.VolleyAuthIntentService}
     */
    public static final boolean USE_AUTHORISATION =false;



    public static String[] email_arr;

    private static SharedPreferences prefs;
    private static String sServerUrl;
    private static String sCommandUrl, sStatusUrl;
    private static String sRegisterUrl, sUnregisterUrl;
    private static String sMessageMeUrl;
    private static String sEmail;
    private static String sGcmRegId;
    private static String sImagePostUploadUrl, sImagePostDownloadUrl;
    private static boolean sGoogleAccountAuthenticated;
    private static HttpCookie sGoogleAccountCookie;

    // Context-requirinf fields
    private WifiStateChangeReceiver mWifiStateReceiver;
    private Queue<Request<?>> mErroredRequestQueue;


    @Override
    public void onCreate() {
        super.onCreate();

        sServerUrl = getString(R.string.server_URL);
        sCommandUrl = sServerUrl + getString(R.string.server_command);
        sRegisterUrl = sServerUrl + getString(R.string.server_register);
        sUnregisterUrl = sServerUrl + getString(R.string.server_unregister);
        sMessageMeUrl = sServerUrl + getString(R.string.server_message);
        sStatusUrl = sServerUrl + getString(R.string.server_status);
        sImagePostUploadUrl = sServerUrl + getString(R.string.server_image_upload);
        sImagePostDownloadUrl = sServerUrl + getString(R.string.server_image_download);
    //    sEmail = getString(R.string.email);


        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // setup the broadcast action namespace string which will be used to notify
        // upload status
        UploadService.NAMESPACE = "com.fitc.dooropener";

    }


    public static String getEmail() {
        return sEmail;
    }

    public static void setGcmRegId(String gcmRegId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("stored_gcm_regid", gcmRegId);
        editor.commit();
        sGcmRegId = gcmRegId;
    }

    public static String getGcmRegId() {
        sGcmRegId =  prefs.getString("stored_gcm_regid",null);
        return sGcmRegId;
    }

    public static boolean isNotify() {
        return prefs.getBoolean("notifications_new_message", true);
    }

    public static String getRingtone() {
        return prefs.getString("notifications_new_message_ringtone", android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString());
    }


    public static String getServerRootUrl() {
        return sServerUrl;
    }

    public static String getStoredAccountName(){
        sEmail = prefs.getString("stored_account_name",null);
        return sEmail;
    }

    public static void setStoredAccountName(String accountName){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("stored_account_name", accountName);
        editor.commit();
        sEmail = accountName;

    }



    public static String getServerCommandUrl() {
        return sCommandUrl;
    }

    public static String getServerRegisterUrl() {
        return sRegisterUrl;
    }

    public static String getServerUnregisterUrl() {
        return sUnregisterUrl;
    }

    public static String getServerMessageMeUrl() {
        return sMessageMeUrl;
    }

    public static String getServerStatusUrl() { return sStatusUrl; };

    public static String getServerUploadImageUrl() {return sImagePostUploadUrl; }

    public static String getServerImageDownloadUrl() {return sImagePostDownloadUrl; }

    public static String getSenderId(Context context) {
        return context.getString(R.string.gcm_defaultSenderId);
    }

    public static String getServerImageDownloadUrl(String imagename) {
        return getServerImageDownloadUrl() + "?" + EndPointParams.IMAGEID + "=" + removeExtension(imagename);
    }

    public static String getTimestampFromImageFileName(String imagename){
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();

        String s = "";

        long timestamp = Long.parseLong(removeExtension(imagename).replaceAll("[^\\d.]", ""));
        Log.d(TAG,""+ timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM h:mm a");
        sdf.setTimeZone(tz);
        return sdf.format(timestamp);

    }

    public static String removeExtension(String uri) {
        if (uri == null) {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0) {
            return uri.substring(0,dot);
        } else {
            // No extension.
            return "";
        }
    }

    public static void setGoogleAccountAuthenticated(boolean success) {
        sGoogleAccountAuthenticated = success;
    }

    /**
     * Retains current auth cookie from Google App Engine. Set in {@link com.fitc.dooropener.lib.credentials.VolleyAuthIntentService}
     * @param googleAccountCookie
     */
    public static void setGoogleAccountCookie(HttpCookie googleAccountCookie) {
        sGoogleAccountCookie = googleAccountCookie;
    }
    public static boolean getGoogleAccountAuthenticated() {
        return sGoogleAccountAuthenticated;
    }

    public static HttpCookie getGoogleAccountCookie() {
        return sGoogleAccountCookie;
    }

    //*************************************************************************************************************8
    // INSTANCE methods with context


    public void registerWifiStateChangeReceiver() {
        if (mWifiStateReceiver == null){
            mWifiStateReceiver = new WifiStateChangeReceiver();
        } else {
            // un register if we have one already so we dont end up with two.
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mWifiStateReceiver);
        }
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mWifiStateReceiver,intentFilter);
    }

    public void queueErroredRequest(Request<?> request) {
        if (mErroredRequestQueue==null) mErroredRequestQueue = new LinkedList<Request<?>>();

        mErroredRequestQueue.add(request);

    }

    public void executeQueuedErrorRequests() {

        while (mErroredRequestQueue.peek()!=null) {
            Request r = mErroredRequestQueue.remove();
            VolleySingleton.getInstance(this).addToRequestQueue(r);
        }
    }

    public interface  EndPointParams{
        public static final String REQUEST_ID = "request_id";

        public static final String FROM_EMAIL = "email";
        public static final String GCM_TOKEN = "token";

        /**
         *  for [remote]-to->[server] requests
         */
        public static final String COMMAND = "command";

        /**
         *  for [control]-to->[server] requests
         */
        public static final String STATUS = "status";
        public static final String CLIENT_TYPE = "client_type";


        /**
         * for iage upload
         */
        public static final String IMAGE_FILENAME = "filename";
        public static final String IMAGE_FILEPATH = "filepath";

        public static final String IMAGEID = "imageid";
    }

    public interface ControlTask {
        public static String DOORARDUINO_OPEN ="open";
        public static String DOORARDUINO_CLOSE ="close";
        public static String DOORARDUINO_STATUS ="status";

        public static String DEVICECAMERA_TAKEPHOTO ="photo";
        public static String REPEAT_LAST = "repeat" ;
        public static String BT_STATUS = "btstatus";
    }

    public interface DoorAction{
        public static String OPENING="opening";
        public static String CLOSING="closing";
        public static String STALLED_OPENING="stalled_opening";
        public static String STALLED_CLOSING="stalled_closing";
        public static String OPENED="opened";
        public static String CLOSED="closed";

        public static String OPEN_BY_UI="open_by_ui";
        public static String CLOSE_BY_UI="close_by_ui";
        public static String OPEN_ASSIST = "open_assist";
        public static String CLOSE_ASSIST = "close_assist";
    }



    public interface ClientType{
        public static String REMOTE="remote";
        public static String CONTROLLER="control";
    }


    public static class Bluetooth {


        public static final long NO_SAVED_TIME = -1 ;

        /**
         * Params sent to the server.
         */
        public static final String BLUETOOTH_CONNECTED = "btconnected";
        public static final String BLUETOOTH_DISCONNECTED = "btdisconnected";
        /**
         * Stores the MAC address of the BT dog collar.
         */
        private static String sBluetoothDeviceMacAddress;

        private static String sBluetoothDeviceName;

        public static String getBluetoothDeviceMacAddress() {
            return prefs.getString("bt_device_mac_address",null);
        }

        public static String getBluetoothDeviceName() {
            return  prefs.getString("bt_device_name",null);
        }

        public static void setBluetoothDeviceMacAddress(String bluetoothDeviceMacAddress) {
            Log.d(TAG, "Setting BT device Mac: " + bluetoothDeviceMacAddress);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("bt_device_mac_address", bluetoothDeviceMacAddress);
            editor.commit();
            }

        public static void setBluetoothDeviceName(String bluetoothDeviceName) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("bt_device_name", bluetoothDeviceName);
            editor.commit();

        }

        public static void setLastConnectionTime(long time) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("bt_device_last_cxn_time", time);
            editor.commit();
        }

        public static long getLastConnectionTime() {
            return prefs.getLong("bt_device_last_cxn_time", System.currentTimeMillis());
        }

        public static void setLastDisconnectionTime(long time) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("bt_device_last_discxn_time",time);
            editor.commit();
        }

        public static long getLastDisconnectionTime() {
            return prefs.getLong("bt_device_last_discxn_time", System.currentTimeMillis());
        }


    }
}
