package com.fitc.dooropener.control;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.server.MapBundler;
import com.fitc.dooropener.lib.server.ServerService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jon on 23/10/2015.
 */
public class DeviceCameraService extends Service
{
    private static final String TAG = "DeviceCameraService";
    public static final String ACTION_PHOTO_READY = "com.fitc.dooropener.control.ACTION_PHOTO_READY";
    public static final String EXTRA_IMAGE_DATA = "com.fitc.dooropener.control.EXTRA_PHOTO_READY";
    public static final String ACTION_DEVICECAMERA_TAKE_PHOTO =  "com.fitc.dooropener.control.ACTION_PHOTO_READY" ;
    private static final int VIEW_TAG = 10;
    private static final int MESSAGEID_PHOTODATAREADY = 1;
    private static final int MESSAGEID_TAKEPHOTO = 2 ;

    private int mCameraId;
    Camera mCamera;
    private SurfaceView mPreview = null;

    //********************************************************************************************
    // Service stuff from https://developer.android.com/guide/components/services.html

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private boolean mSetup;

    @Override
    public void onCreate() {
        super.onCreate();
        setupService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mSetup) setupService();

        Message m = mServiceHandler.obtainMessage();
        m.arg1 = startId;
        m.arg2 = MESSAGEID_TAKEPHOTO;
        mServiceHandler.sendMessage(m);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        setupService();
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSetup = false;
        releaseCameraAndPreview();
        deleteSurfaceView();
    }

    private void setupService() {
        if (!mSetup) {
            mSetup = true;
            setupCamera();

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
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg2){
                case MESSAGEID_PHOTODATAREADY:
                    byte[] data = (byte[]) msg.obj;
                    saveAndUploadPhotoData(data);
                    break;
                case MESSAGEID_TAKEPHOTO:
                    takePhoto();
                    break;
            }

        }
    }

    private void saveAndUploadPhotoData(byte[] data) {
        String[] fileDetails = new String[2];
        fileDetails[0] = "photo.jpg";
        File photo=new File(Environment.getExternalStorageDirectory(), fileDetails[0] );
        fileDetails[1] =photo.getAbsolutePath();
        if (photo.exists()) {
            photo.delete();
        }

        try {
            FileOutputStream fos=new FileOutputStream(photo.getPath());

            fos.write(data);
            fos.close();
        }
        catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }


        Map<String,String> params = new HashMap<>();
        params.put(CommonApplication.EndPointParams.IMAGE_FILENAME,fileDetails[0]);
        params.put(CommonApplication.EndPointParams.IMAGE_FILEPATH,fileDetails[1]);

        final Intent serviceIntent = new Intent(this, ServerService.class);
        serviceIntent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
        serviceIntent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, CommonApplication.IMAGE_UPLOAD);
        serviceIntent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));
        // Starts the IntentService
        startService(serviceIntent);
    }

    //***********************************************************************************************


    private void takePhoto(){
        if (mCamera!=null) mCamera.takePicture(null,null,mDeferredCall);
    }

    @SuppressWarnings("deprecation")
    private void setupCamera() {
        mCameraId = getFrontCameraId();
        mPreview = new SurfaceView(this);
        SurfaceHolder holder = mPreview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            //The mPreview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                safeCameraOpen(mCameraId);

                try {

                    mCamera.setPreviewDisplay(holder);


                } catch (Exception e) {
                    if (mCamera != null)
                        releaseCameraAndPreview();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCameraAndPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Camera.Parameters params = mCamera.getParameters();

                //Camera.Size size = getOptimalPreviewSize(width,height);
                params.setPictureFormat(ImageFormat.JPEG);
                //params.setPreviewSize(size.width,size.height);
                mCamera.setParameters(params);
                setCameraDisplayOrientation();
                mCamera.startPreview();
            }
        });


        WindowManager wm = (WindowManager)this
                .getSystemService(Context.WINDOW_SERVICE);

     //   wm =((Activity)mContext).getWindowManager();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);
        //Don't set the mPreview visibility to GONE or INVISIBLE
        wm.addView(mPreview, params);

    }

    private static void showMessage(String message) {
        Log.i("Camera", message);
    }

    private Camera.Size getOptimalPreviewSize(int width, int height)
    {
        Camera.Size optimalSize = null;

        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPictureSizes();

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        // Try to find a size match which suits the whole screen minus the menu on the left.
        for (Camera.Size size : sizes)
        {
            Log.d(TAG,"Supported size: w:" + size.width + ", h:" + size.height);
            if (size.height != width) continue;
            double ratio = (double) size.width / size.height;
            if (ratio <= targetRatio + ASPECT_TOLERANCE && ratio >= targetRatio - ASPECT_TOLERANCE)
            {
                optimalSize = size;
            }
        }

        // If we cannot find the one that matches the aspect ratio, ignore the requirement.
        if (optimalSize == null)
        {
            // TODO : Backup in case we don't get a size.
        }

        return optimalSize;
    }

    private static int getFrontCameraId(){
        int camId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo ci = new Camera.CameraInfo();

        for(int i = 0;i < numberOfCameras;i++){
            Camera.getCameraInfo(i,ci);
            if(ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                camId = i;
            }
        }

        return camId;
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();

            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(TAG, "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    private void setCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        int rotation = ((WindowManager)this
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        mCamera.setDisplayOrientation(result);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setRotation(270);
        mCamera.setParameters(parameters);
    }

    Camera.PictureCallback mDeferredCall = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "Photo taken successfully");

            Message m = mServiceHandler.obtainMessage();
            m.arg2 = MESSAGEID_PHOTODATAREADY;
            m.obj = data;
            mServiceHandler.sendMessage(m);
            Intent intent = new Intent(ACTION_PHOTO_READY);
            intent.putExtra(EXTRA_IMAGE_DATA, data);
            LocalBroadcastManager.getInstance(DeviceCameraService.this).sendBroadcast(intent);
          //  releaseCameraAndPreview();
          //  deleteSurfaceView();
            if (mCamera!=null) mCamera.startPreview();
        }
    };

    private void deleteSurfaceView() {
        WindowManager wm = (WindowManager)this
                .getSystemService(Context.WINDOW_SERVICE);

        //Don't set the mPreview visibility to GONE or INVISIBLE
        try {
            if (mPreview != null) wm.removeView(mPreview);
            mPreview =null;
        } catch (Exception e) {
            Log.e(TAG,"Remove view exception",e);
        }
    }


}
