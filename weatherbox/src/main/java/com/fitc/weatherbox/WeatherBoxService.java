package com.fitc.weatherbox;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Jon on 20/01/2017.
 */

public class WeatherBoxService extends Service{

    public static final String ACTION_SYNC_WITH_CLOUD = "ACTION_SYNC_WITH_CLOUD";
    public static final String ACTION_INCOMING_DATA = "ACTION_INCOMING_DATA";

    public static final String EXTRA_INCOMING_DATA = "ACTION_DATA_PAYLOAD";
    private static final int MESSAGEID_CLOUD_SYNC = 1;
    private static final int MESSAGEID_INCOMING_DATA = 2;

    public static void processIncomingData(Context context, byte[] incoming) {

        Intent i = new Intent(context,WeatherBoxService.class);
        i.setAction(ACTION_INCOMING_DATA);
        i.putExtra(EXTRA_INCOMING_DATA,incoming);
        context.startService(i);

    }


    public static void initiateCloudSync(Context context, boolean user) {

        Intent i = new Intent(context,WeatherBoxService.class);
        i.setAction(ACTION_SYNC_WITH_CLOUD);
        context.startService(i);

    }




    //***********************************************************************************

    private static final String TAG = WeatherBoxService.class.getSimpleName();
    private static final long CLOUD_SYNC_FREQUENCY = 5 * 60 * 1000;
    private static boolean LOG = true;

    private LastData mData = new LastData();

    private TimeAverage mAirTempTimeAvg = new TimeAverage();
    private TimeAverage mAirHumidityTimeAvg = new TimeAverage();
    private TimeAverage mAirDewPointTimeAvg = new TimeAverage();
    private TimeAverage mWaterPresentTimeAvg = new TimeAverage();
    private TimeAverage mWaterTempTimeAvg = new TimeAverage();
    private TimeAverage mLux1TimeAvg = new TimeAverage();
    private TimeAverage mLux2TimeAvg = new TimeAverage();
    private TimeAverage mPowerTimeAvg = new TimeAverage();



    public boolean parseData(byte[] incoming){
        long t = System.currentTimeMillis();
        // water present
        mData.waterPresent = (char) incoming[0]>0;
        mWaterPresentTimeAvg.addDataPoint(t, mData.waterPresent?1f:0f);
        // parse rest of numbers
        for (int i = 0; i < 6; i++) {
            char[] arr = new char[4];
            arr[0] = (char)incoming[1 + (i * 4)];
            arr[1] = (char)incoming[1 + (i * 4) + 1];
            arr[2] = (char)incoming[1 + (i * 4) + 2];
            arr[3] = (char)incoming[1 + (i * 4) + 3];
            long data = bytesToLong(arr);
            switch (i+1) {
                case 1:
                    mData.waterTemp = ((float)data) / 100f;
                    mWaterTempTimeAvg.addDataPoint(t,mData.waterTemp);
                    break;
                case 2:
                    mData.airTemp = ((float)data) / 100f;
                    mAirTempTimeAvg.addDataPoint(t,mData.airTemp);
                    break;
                case 3:
                    mData.airHumidity = ((float)data) / 100f;
                    mAirHumidityTimeAvg.addDataPoint(t,mData.airHumidity);
                    break;
                case 4:
                    mData.airDewPoint = ((float)data) / 100f;
                    mAirDewPointTimeAvg.addDataPoint(t,mData.airDewPoint);
                    break;
                case 5:
                    mData.lux1 = data;
                    mLux1TimeAvg.addDataPoint(t, (float)mData.lux1);
                    break;
                case 6:
                    mData.lux2 = data;
                    mLux2TimeAvg.addDataPoint(t, (float)mData.lux2);
                    break;
            }



        }

        mData.power = (int) (0.0079 * ((mData.lux1 + mData.lux2) /2.));
        mPowerTimeAvg.addDataPoint(t, (float)mData.power);

        if (LOG){
            Log.d(TAG,"Weather Box data:"+
                    " W:" + mData.waterPresent +
                    " Tw:" + (int) mData.waterTemp +
                    " Ta:" + (int) mData.airTemp +
                    " Ha:" + (int) mData.airHumidity +
                    " Da:" + (int) mData.airDewPoint +
                    " L1: " + roundToTen(mData.lux1,2) +
                    " L2:" + roundToTen(mData.lux2, 2) +
                    " P: " + roundToTen(mData.power, 1));
        }
        mData.lastDataUpdateTime = t;
        return true;
    }

    private void cloudSync(){
        mData.cloudSyncingOn = true;

        float airTemp = mAirTempTimeAvg.calcAndClear();
        float airHumidity = mAirHumidityTimeAvg.calcAndClear();
        float dewPoint = mAirDewPointTimeAvg.calcAndClear();

        float waterPresent = mWaterPresentTimeAvg.calcAndClear();
        // boolean waterPresent = (a<0.5) ? false : true;

        float waterTemp = mWaterTempTimeAvg.calcAndClear();
        long lux1 = roundToTen(mLux1TimeAvg.calcAndClear(), 2);
        long lux2 = roundToTen(mLux2TimeAvg.calcAndClear(), 2);

        int power = (int) roundToTen(mPowerTimeAvg.calcAndClear(), 1);

        WeatherBoxSyncService.uploadToGoogleForms(this, airTemp, airHumidity, dewPoint, waterPresent, waterTemp, lux1, lux2, power);

        mData.lastCloudSyncTime = System.currentTimeMillis();
    }

    //************************************************************************
    // Helpers


    private static long bytesToLong(char[] arr)
    {
        long value = 0;
        for (int i = 0; i < arr.length; i++)
        {
            value = (value << 8) + (arr[i] & 0xff);
        }

        return value;
    }


    private static int roundToTen(float x, int n){
        int a = (int) Math.pow(10,n);

        int b = (int) (x/a);

        return b*a;
    }


    //********************************************************************************************
    // Service stuff from https://developer.android.com/guide/components/services.html

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;



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

        loadLastInstanceData(true);

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent!=null && intent.getAction()!=null){
            String action = intent.getAction();
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            switch (action){
                case ACTION_SYNC_WITH_CLOUD:
                    msg.arg2 = MESSAGEID_CLOUD_SYNC;
                    mServiceHandler.sendMessage(msg);
                    break;
                case ACTION_INCOMING_DATA:
                    msg.arg2 = MESSAGEID_INCOMING_DATA;
                    byte[] data = intent.getByteArrayExtra(EXTRA_INCOMING_DATA);
                    msg.obj = data;
                    mServiceHandler.sendMessage(msg);
                    //if (!mData.cloudSyncingOn) initiateCloudSync(this,false);
                    break;
                default:
                    msg.recycle();
            }

        }


        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;

    }

    @Override
    public void onDestroy() {
        saveInstanceData();
    }

    //*******************************************************************************************

    private void saveInstanceData(){
        SharedPreferences sharedPref = getSharedPreferences( getString(R.string.preference_file_key),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        try {
            editor.putString(getString(R.string.saved_weatherboxdata_key),serialiseTimeAverageData());
            editor.putString(getString(R.string.saved_weatherboxdata_key2),serialiseInstanceData());
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.commit();
    }

    private void loadLastInstanceData(boolean andClearAll) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        try {
            if (sharedPref.contains(getString(R.string.saved_weatherboxdata_key))) {
                String dataSerial = sharedPref.getString(getString(R.string.saved_weatherboxdata_key), null);
                if (dataSerial != null) {
                    deserialiseTimeAverageData(dataSerial);
                }
            }

            if (sharedPref.contains(getString(R.string.saved_weatherboxdata_key2))) {
                String dataSerial = sharedPref.getString(getString(R.string.saved_weatherboxdata_key2), null);
                if (dataSerial != null) {
                    deserialiseInstanceData(dataSerial);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (andClearAll) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.commit();
        }

    }


    private String serialiseTimeAverageData() throws IOException{
        ArrayList<TimeAverage> l = new ArrayList<>();


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(l);
        out.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void deserialiseTimeAverageData(String s) throws IOException, ClassNotFoundException{
        byte[] b =Base64.decode(s, Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(b));
        ArrayList<TimeAverage> list  = (ArrayList<TimeAverage>) ois.readObject();
        mAirTempTimeAvg = list.get(0);
        mAirHumidityTimeAvg= list.get(1);
        mAirDewPointTimeAvg= list.get(2);
        mWaterPresentTimeAvg= list.get(3);
        mWaterTempTimeAvg= list.get(4);
        mLux1TimeAvg= list.get(5);
        mLux2TimeAvg= list.get(6);
        mPowerTimeAvg= list.get(7);
        ois.close();
    }

    private String serialiseInstanceData() throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(mData);
        out.close();
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void deserialiseInstanceData(String s) throws IOException, ClassNotFoundException{
        byte[] b =Base64.decode(s, Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(b));
        mData = (LastData) ois.readObject();
    }


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg2){
                case MESSAGEID_INCOMING_DATA:
                    byte[] data = (byte[]) msg.obj;
                    parseData(data);
                    break;
                case MESSAGEID_CLOUD_SYNC:
                    cloudSync();
                    Message m = obtainMessage();
                    m.arg1 = msg.arg1;
                    m.arg2 = MESSAGEID_CLOUD_SYNC;
                    sendMessageDelayed(m, CLOUD_SYNC_FREQUENCY);
                    break;
            }

        }
    }


    //**********************************************************************


    class TimeAverage implements Serializable {

        int size=0;
        double nom=0;
        double dom=0;
        long lastTime;
        double lastVal;

        /**
         *
         */
        TimeAverage(){
            Log.d(TAG,"Reinit.");
            calcAndClear();
        }

        public void addDataPoint(Long time, Float val){
            if (size>0){
                double dt = time - lastTime;
                dom = dom + dt;
                nom = nom + (dt * 0.5*(val+lastVal));
            }

            lastTime = time;
            lastVal = val;
            size++;
        }


        public float calcAndClear(){
            float result = (float) (nom/dom);

            size=0;
            nom= 0;
            dom=0;

            return result;
        }
    }

    class LastData implements Serializable{
        private float airTemp;
        private float airHumidity;
        private float airDewPoint;
        private boolean waterPresent;
        private float waterTemp;
        private long lux1;
        private long lux2;
        private int power;
        private long lastDataUpdateTime;
        private long lastCloudSyncTime;

        private boolean cloudSyncingOn;
    }
}
