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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.R;
import com.google.android.gms.gcm.GcmListenerService;

import java.util.List;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String json = data.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + json);

        // Deserialse the message JSON to Status obj.
        GcmDataPayload status = GcmDataPayload.makeFromJson(json);

        if (status!=null){
            Intent intent = new Intent(CommonApplication.ACTION_GCM_STATUS);
            intent.putExtra(CommonApplication.EXTRA_GCM_STATUS_JSON, json);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);



            // [START_EXCLUDE]
            /**
             * Production applications would usually process the message here.
             * Eg: - Syncing with server.
             *     - Store message in local database.
             *     - Update UI.
             */

            /**
             * In some cases it may be useful to show a notification indicating to the user
             * that a message was received.
             */
            if(status.issueNotification()) sendNotification(status);
            // [END_EXCLUDE]
        }

    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param status GCM message received.
     */
    private void sendNotification(GcmDataPayload status) {

        /**
         * This code should find launcher activity of app using this librbary and set it as the what gets launched
         */
        Intent intent=null;
        final PackageManager packageManager=getPackageManager();
        Log.i(TAG, "PACKAGE NAME " + getPackageName());

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appList = packageManager.queryIntentActivities(mainIntent, 0);

        for(final ResolveInfo resolveInfo:appList)
        {
            if(getPackageName().equals(resolveInfo.activityInfo.packageName))  //if this activity is not in our activity (in other words, it's another default home screen)
            {
                intent=packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
                break;
            }
        }
        PendingIntent pendingIntent = null;
        if (intent!=null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                        PendingIntent.FLAG_ONE_SHOT);
        }



        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(getResources().getString(R.string.notif_content_title))
                .setContentText(status.getStatusData())
                .setAutoCancel(true)
                .setSound(defaultSoundUri);



        if (pendingIntent!=null){
            notificationBuilder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}