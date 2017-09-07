package com.example.termowidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import java.io.IOException;


public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";
    final static private String  PREFERENCES_FILE_NAME  = "config";
    final static private String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";

    private CheckBox statusBarCheckBox;
    private  SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

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

