package com.example.termowidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.IOException;


public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";
    final static public String  PREFERENCES_FILE_NAME  = "config";
    public final static String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";

    private CheckBox statusBarCheckBox;
    public static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        sharedPreferences = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);

        Boolean statusBar = false;
        //  if STATUS_BAR_PREFERENCES_KEY exist in config
        //  load from config statusBar value
        try {
            statusBar = loadPreferences(sharedPreferences, STATUS_BAR_PREFERENCES_KEY, false);
        }
        catch (IOException e){
            Log.d(LOG_TAG, e.toString());
        }

        //  find statusBar CheckBox
        statusBarCheckBox = (CheckBox) findViewById(R.id.status_bar_info);
        //  set CheckBox value
        statusBarCheckBox.setChecked(statusBar);

    }

    public void onStatusBarChBoxClick(View view){
        Boolean statusBar = statusBarCheckBox.isChecked();
        savePreferences(sharedPreferences,STATUS_BAR_PREFERENCES_KEY,statusBar);
        //  run widget update
        Context context = getApplicationContext();
        context.startService(new Intent(context,WidgetUpdaterService.class));
    }


    static public Boolean loadPreferences (SharedPreferences sharedPreferences, String key, Boolean defaultValue) throws IOException{

        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getBoolean(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exsist");
        }
    }

    static public void savePreferences(SharedPreferences sharedPreferences, String key, Boolean value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putBoolean(key, value);
        preferencesEditor.apply();
    }


}

