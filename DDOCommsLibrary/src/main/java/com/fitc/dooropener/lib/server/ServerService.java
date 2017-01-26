package com.fitc.dooropener.lib.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.alexbbb.uploadservice.BinaryUploadRequest;
import com.alexbbb.uploadservice.MultipartUploadRequest;
import com.alexbbb.uploadservice.UploadNotificationConfig;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.fitc.dooropener.lib.CommonApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jon on 12/10/2015.
 */
public class ServerService extends Service  {
    private static final String TAG = ServerService.class.getSimpleName();

    private static final int INTENT_ERROR = 99991;

    /**
     * Starts UsbProtocol Task intent service.
     * @param  statusType
     * @param status
     * @return
     */
    public static boolean sendStatusToServer(Context c, int statusType, String status) {
        Map<String,String> params = new HashMap<>();
        params.put(CommonApplication.EndPointParams.REQUEST_ID,ServerService.getRequestId());
        params.put(CommonApplication.EndPointParams.FROM_EMAIL, CommonApplication.getEmail());
        params.put(CommonApplication.EndPointParams.GCM_TOKEN,CommonApplication.getGcmRegId());
        params.put(CommonApplication.EndPointParams.STATUS, status);

        final Intent serviceIntent = new Intent(c, ServerService.class);
        serviceIntent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
        serviceIntent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, statusType);
        serviceIntent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));
        // Starts the IntentService
        c.startService(serviceIntent);
        return true;
    }

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    public static String getRequestId() {
        return ""+System.currentTimeMillis();
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(final Message msg) {
            Log.i(TAG,"ServiceHandler#hd handle the responsandlemessage");
            // Formulate the request ane.
            if (msg.what==CommonApplication.IMAGE_UPLOAD){
                String imageFileName = ((Map<String,String>) msg.obj).get(CommonApplication.EndPointParams.IMAGE_FILENAME);
                String imageFilePath = ((Map<String,String>) msg.obj).get(CommonApplication.EndPointParams.IMAGE_FILEPATH);
                uploadMultipart(ServerService.this, imageFileName, imageFilePath);
            } else {
                MyServerRequest.make(ServerService.this,msg.what, (Map<String,String>) msg.obj,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // Broadcast response.
                                Intent msgIntent = new Intent(CommonApplication.ACTION_SERVER_RESPONSE);
                                msgIntent.putExtra(CommonApplication.EXTRA_SERVER_RESPONSE_MESSAGE, response);
                                LocalBroadcastManager.getInstance(ServerService.this).sendBroadcast(msgIntent);
                                // Stop the service using the startId, so that we don't stop
                                // the service in the middle of handling another job
                                stopSelf(msg.arg1);
                                // Do something with the response
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                NetworkResponse nr =  error.networkResponse;
                                if (nr!=null){
                                    Log.e(TAG,"Error: "+ error.networkResponse.statusCode);// Log error
                                    int sc = error.networkResponse.statusCode;

                                    // authentication or authorisation error. Bad dog!!!!
                                    if (sc == 401 || sc == 403){
                                        // Broadcast response.
                                        Intent msgIntent = new Intent(CommonApplication.ACTION_SERVER_RESPONSE_AUTHERROR);
                                        msgIntent.putExtra(CommonApplication.EXTRA_SERVER_RESPONSE_MESSAGE, sc);
                                        LocalBroadcastManager.getInstance(ServerService.this).sendBroadcast(msgIntent);
                                        // Stop the service using the startId, so that we don't stop
                                        // the service in the middle of handling another job
                                        stopSelf(msg.arg1);
                                        // Do something with the response
                                    }
                                }
                            }
                        });
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (CommonApplication.DEBUG) Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job

        if (intent!=null) {
            Message msg = mServiceHandler.obtainMessage();
            msg.what = intent.getIntExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, INTENT_ERROR);
            msg.obj = MapBundler.fromBundle(intent.getBundleExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS));
            msg.arg1 = startId;
            mServiceHandler.sendMessage(msg);
        } else {
            Log.e(TAG, "Intent to start service null");
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


        Toast.makeText(this, "ServerService done", Toast.LENGTH_SHORT).show();
    }


    public void uploadMultipart(final Context context, final String fileName, final String filePath) {
        final MultipartUploadRequest request =
                new MultipartUploadRequest(context,
                        "custom-upload-id",
                      CommonApplication.getServerUploadImageUrl());

    /*
     * parameter-name: is the name of the parameter that will contain file's data.
     * Pass "uploaded_file" if you're using the test PHP script
     *
     * custom-file-name.extension: is the file name seen by the server.
     * E.g. value of $_FILES["uploaded_file"]["name"] of the test PHP script
     */
        request.addFileToUpload(filePath,
                "parameter-name",
               fileName ,
                "multipart/form-data");

        //You can add your own custom headers
        //request.addHeader("your-custom-header", "your-custom-value");

        //and parameters
        //request.addParameter("parameter-name", "parameter-value");

        //If you want to add a parameter with multiple values, you can do the following:
        //request.addParameter("array-parameter-name", "value1");
        //request.addParameter("array-parameter-name", "value2");
        //request.addParameter("array-parameter-name", "valueN");

        //or
        //String[] values = new String[] {"value1", "value2", "valueN"};
        //request.addArrayParameter("array-parameter-name", values);

        //or
        //List<String> valuesList = new ArrayList<String>();
        //valuesList.add("value1");
        //valuesList.add("value2");
        //valuesList.add("valueN");
        //request.addArrayParameter("array-parameter-name", valuesList);

        //configure the notification
        // as the default one.
        UploadNotificationConfig config = new UploadNotificationConfig(this);

        request.setNotificationConfig(config);

        // set a custom user agent string for the upload request
        // if you comment the following line, the system default user-agent will be used
        //request.setCustomUserAgent("UploadServiceDemo/1.0");

        // set the intent to perform when the user taps on the upload notification.
        // currently tested only with intents that launches an activity
        // if you comment this line, no action will be performed when the user taps
        // on the notification
       // request.setNotificationClickIntent(new Intent(context, YourActivity.class));

        // set the maximum number of automatic upload retries on error
        request.setMaxRetries(2);

        try {
            //Start upload service and display the notification
            request.startUpload();

        } catch (Exception exc) {
            //You will end up here only if you pass an incomplete upload request
            Log.e("AndroidUploadService", exc.getLocalizedMessage(), exc);
        }
    }
}
