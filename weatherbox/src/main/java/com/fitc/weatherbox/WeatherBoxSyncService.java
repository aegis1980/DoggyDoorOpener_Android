package com.fitc.weatherbox;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fitc.googleformuploader.GoogleFormUploader;

/**
 * Created by Jon on 18/01/2017.
 *
 * Syncs Weatherbox with online serice.
 *
 */



public class WeatherBoxSyncService extends IntentService {

    /**
     * From https://docs.google.com/forms/d/e/1FAIpQLSdAb5TQ_n6mERl7_9cItCC1PsUwohMa5J1ZvePfx9nzXmFSgQ/viewform
     */
    private static final String AIRTEMP = "1613021516";
    private static final String AIRHUMIDITY = "2004822308";
    private static final String WATERPRESENT = "2129880371";
    private static final String WATERTEMP = "1906475845";
    private static final String LUX1 = "261397132";
    private static final String LUX2 = "1238921711";
    private static final String POWER = "1441751458";
    private static final String DEWPOINT = "339232289";

    private static String sFormId;

    public static void  setFormId(String id){
        sFormId = id;
    }


    public static void uploadToGoogleForms(
            Context c,
            float airTemp,
            float airHumidity,
            float dewPoint,
            float waterPresent,
            float waterTemp,
            long lux1,
            long lux2,
            int power) {
        Intent i = new Intent(c, WeatherBoxSyncService.class);
        i.putExtra(AIRTEMP, airTemp);
        i.putExtra(AIRHUMIDITY, airHumidity);
        i.putExtra(DEWPOINT, dewPoint);
        i.putExtra(WATERPRESENT,waterPresent);
        i.putExtra(WATERTEMP, waterTemp);
        i.putExtra(LUX1, lux1);
        i.putExtra(LUX2, lux2);
        i.putExtra(POWER, power);
        // Starts the IntentService
        c.startService(i);
    }


    public WeatherBoxSyncService() {
        super(WeatherBoxSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GoogleFormUploader gfu = new GoogleFormUploader(sFormId);

        gfu.addEntry(AIRTEMP,""+intent.getFloatExtra(AIRTEMP, 0));
        gfu.addEntry(AIRHUMIDITY,""+intent.getFloatExtra(AIRHUMIDITY, 0));
        gfu.addEntry(DEWPOINT,""+intent.getFloatExtra(DEWPOINT, 0));
        gfu.addEntry(WATERPRESENT,""+intent.getFloatExtra(WATERPRESENT, 0));
        gfu.addEntry(WATERTEMP,""+intent.getFloatExtra(WATERTEMP, 0));
        gfu.addEntry(LUX1,""+intent.getLongExtra(LUX1, 0));
        gfu.addEntry(LUX2,""+intent.getLongExtra(LUX2, 0));
        gfu.addEntry(POWER,""+intent.getIntExtra(POWER, 0));


        gfu.upload(false);
    }



}
