package com.fitc.dooropener.lib.server;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fitc.dooropener.lib.BaseActivity;
import com.fitc.dooropener.lib.CommonApplication;

/**
 * If volley reponse is a no connectin error this turns wifi on and off to try and reset connection.
 * It queues the original request and retries them
 *
 * Created by Jon on 20/01/2016.
 */
public abstract class SpecialResponseErrorListener implements Response.ErrorListener {

    private static final String TAG = SpecialResponseErrorListener.class.getSimpleName();
    Context mContext;


    public SpecialResponseErrorListener(Service s) {
        mContext = s;
    }

    public SpecialResponseErrorListener(BaseActivity a) {
        mContext = a;
    }

    public void queueErroredRequest(Request r){
        if (mContext instanceof Service){
            ((CommonApplication) ((Service) mContext).getApplication()).queueErroredRequest(r);
        } else if (mContext instanceof BaseActivity){
            ((CommonApplication) ((BaseActivity) mContext).getApplication()).queueErroredRequest(r);
        }
    }


    @Override
    public void onErrorResponse(VolleyError error) {

        //other catches
        if (error instanceof NoConnectionError) {


            CommonApplication app = null;
            if (mContext instanceof Service) {
                Service s = ((Service) mContext);
                app = (CommonApplication) s.getApplication();
            } else if (mContext instanceof Activity) {
                Activity a = ((Activity) mContext);
                app = (CommonApplication) a.getApplication();
            }

            if (app != null) {
                app.registerWifiStateChangeReceiver();
                // interent connection lost.
                // Close door as the default - the dog is either in or out.
                //turn on wifi
                WifiManager wifiManager;
                wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(false);
                // when this goes through, it'l be picked up by WifiStateChangeReceiver
            } else {
                Log.e(TAG, "There was a context problem with creating WifiStateStateReceiver in CommonApplicaiton");
            }

        }
    }
}

