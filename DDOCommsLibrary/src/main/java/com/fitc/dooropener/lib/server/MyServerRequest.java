package com.fitc.dooropener.lib.server;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.fitc.dooropener.lib.CommonApplication;

import java.util.Map;

/**
 * Created by Jon on 14/10/2015.
 */
public class MyServerRequest extends StringRequest {


    private static final String TAG = MyServerRequest.class.getSimpleName();

    public static MyServerRequest make(Context context,int request_type, Map<String, String> params, Response.Listener<String> listener, Response.ErrorListener errorListener){
        RequestQueue queue = VolleySingleton.getInstance(context).getRequestQueue();



        String url;
        switch (request_type){
            case CommonApplication.REGISTER:
                url = CommonApplication.getServerRegisterUrl();
                break;
            case CommonApplication.UNREGISTER:
                url = CommonApplication.getServerUnregisterUrl();
                break;
            case CommonApplication.COMMAND:
                url = CommonApplication.getServerCommandUrl();
                break;
            case CommonApplication.DOOR_STATUS:
                url= CommonApplication.getServerStatusUrl();
                break;
            default:
                url = CommonApplication.getServerMessageMeUrl();
                break;
        }
        Log.i(TAG, "Queuing request to " + url);

        final MyServerRequest request = new MyServerRequest(url,params,listener,errorListener);

        //Add the request to the RequestQueue.
        queue.add(request);

        return request;
    }

    // ... other methods go here

    private Map<String, String> mParams;

    public MyServerRequest(String url, Map<String, String> params, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Request.Method.POST, url, listener, errorListener);
        mParams = params;

    }

    @Override
    public Map<String, String> getParams() {
        return mParams;
    }

}
