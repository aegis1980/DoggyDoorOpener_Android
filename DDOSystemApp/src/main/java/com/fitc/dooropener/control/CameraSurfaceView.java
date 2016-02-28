package com.fitc.dooropener.control;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.List;

public class CameraSurfaceView extends SurfaceView {
    private final int mCameraId;
    Camera camera;
        SurfaceHolder previewHolder;

        public CameraSurfaceView(Context context)
        {
            super(context);
            previewHolder = this.getHolder();
            mCameraId = getFrontCameraId();

            //previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            previewHolder.addCallback(surfaceHolderListener);

        }

        SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {

            public void surfaceCreated(SurfaceHolder holder) {
                safeCameraOpen(mCameraId);
            try {

                camera.setPreviewDisplay(previewHolder);

            }
                catch (Throwable e){ }
            }
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {
                Camera.Parameters params = camera.getParameters();

                //Camera.Size size = getOptimalPreviewSize(width,height);
                params.setPictureFormat(ImageFormat.JPEG);
                //params.setPreviewSize(size.width,size.height);
                camera.setParameters(params);
                setCameraDisplayOrientation();
                camera.startPreview();


            }
            public void surfaceDestroyed(SurfaceHolder arg0)
            {
                camera.stopPreview();
                camera.release();
            }

        };

        public void onResume() {
            camera.startPreview();

        }

        public void onPause() {
            // TODO Auto-generated method stub
            camera.stopPreview();
        }

    public void setCameraDisplayOrientation() {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);
        int rotation = ((Activity)getContext()).getWindowManager().getDefaultDisplay()
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
        camera.setDisplayOrientation(result);
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


    public boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            releaseCameraAndPreview();

            camera = Camera.open(id);
            qOpened = (camera != null);
        } catch (Exception e) {
            Log.e(getContext().getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    public void releaseCameraAndPreview() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize(int width, int height)
    {
        Camera.Size optimalSize = null;

        List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) height / width;

        // Try to find a size match which suits the whole screen minus the menu on the left.
        for (Camera.Size size : sizes)
        {
           // Log.d(TAG,"Supported size: w:" + size.width + ", h:" + size.height);
            if (size.width != width) continue;
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



}