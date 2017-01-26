package com.fitc.googleformuploadertestapp;

import android.app.IntentService;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.fitc.weatherbox.WeatherBoxSyncService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WeatherBoxSyncService.setFormId("1TgseUiZFY0VidMmwOuZ5UFFty43OgGnPur-Cnns2Sn0");
        final EditText editText = (EditText) findViewById(R.id.edit_text);
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = editText.getText().toString();

                try{
                    float f = Float.parseFloat(text.trim());
                    WeatherBoxSyncService.uploadToGoogleForms(MainActivity.this,f,0,true,0,0,0);
                } catch (NumberFormatException e){ //validation
                    Log.e(TAG, "", e);
                }



            }
        });

    }
}
