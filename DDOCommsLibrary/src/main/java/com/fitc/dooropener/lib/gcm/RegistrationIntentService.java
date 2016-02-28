/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fitc.dooropener.lib.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.R;
import com.fitc.dooropener.lib.server.MapBundler;
import com.fitc.dooropener.lib.server.ServerService;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG,"Starting RegistrationIntentService");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationToServer(token);

            // Subscribe to topic channels
          //  subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
       // Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
      //  LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {

        CommonApplication.setGcmRegId(token);

        String type;
        if (getPackageName().contains(CommonApplication.ClientType.REMOTE)){
            type = CommonApplication.ClientType.REMOTE;
        } else {
            type = CommonApplication.ClientType.CONTROLLER;
        }


        Map<String,String> params = new HashMap<>();
        params.put(CommonApplication.EndPointParams.REQUEST_ID, ServerService.getRequestId());
        params.put(CommonApplication.EndPointParams.FROM_EMAIL, CommonApplication.getEmail());
        params.put(CommonApplication.EndPointParams.CLIENT_TYPE, type);
        params.put(CommonApplication.EndPointParams.GCM_TOKEN, token);

        Intent intent = new Intent(this, ServerService.class);
        intent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
        intent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, CommonApplication.REGISTER);
        intent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));

        startService(intent);
    }


    /**
     * Persist unregister to third-party servers.
     *
     */
    private void sendUnregisterToServer(String email) {
        Map<String,String> params = new HashMap<>();
        params.put(CommonApplication.EndPointParams.REQUEST_ID,ServerService.getRequestId());
        params.put(CommonApplication.EndPointParams.FROM_EMAIL, CommonApplication.getEmail());
        params.put(CommonApplication.EndPointParams.GCM_TOKEN, CommonApplication.getGcmRegId());

        Intent intent = new Intent(getApplicationContext(), ServerService.class);
        intent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
        intent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, CommonApplication.UNREGISTER);
        intent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));

        startService(intent);
    }


}