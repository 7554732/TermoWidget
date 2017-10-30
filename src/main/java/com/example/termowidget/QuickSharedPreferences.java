package com.example.termowidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;
import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;

public class QuickSharedPreferences {

    final static public String  PREFERENCES_FILE_NAME  = "config";
    public static final String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";
    public static final String BLINKING_PREFERENCES_KEY = "is_blinking";
    public static final String GRAPHIC_PREFERENCES_KEY = "is_graphic";

    public static final String CALIBRATE_PREFERENCES_KEY = "calibration_temperature";

    public static SharedPreferences sharedPreferences;

    public QuickSharedPreferences(Context context){
        //  set sharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
    }

    static public Boolean loadPreferences (SharedPreferences sharedPreferences, String key, Boolean defaultValue) throws IOException{

        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getBoolean(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exist");
        }
    }

    static public Integer loadPreferences (SharedPreferences sharedPreferences, String key, Integer defaultValue) throws IOException{

        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getInt(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exist");
        }
    }

    static public void savePreferences(SharedPreferences sharedPreferences, String key, Boolean value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putBoolean(key, value);
        preferencesEditor.apply();
    }

    static public void savePreferences(SharedPreferences sharedPreferences, String key, Integer value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putInt(key, value);
        preferencesEditor.apply();
    }

    public void saveBoolean (String key, Boolean value){
        savePreferences(sharedPreferences, key, value);
    }

    public void saveInteger (String key, Integer value){
        savePreferences(sharedPreferences, key, value);
    }

    public Integer getCalibrationTemperature() {
        Integer calibrationTemperature = 0;
        try {
            calibrationTemperature = loadPreferences(sharedPreferences, CALIBRATE_PREFERENCES_KEY, 0);
        } catch (IOException e) {
            if (isDebug) Log.w(LOG_TAG , e.toString());
        }
        return calibrationTemperature;
    }

    public Boolean isStatusBar() {
        Boolean is_status_bar = false;
        try {
            is_status_bar = loadPreferences(sharedPreferences, STATUS_BAR_PREFERENCES_KEY, false);
        } catch (IOException e) {
            if (isDebug) Log.w(LOG_TAG , e.toString());
        }
        return is_status_bar;
    }

    public Boolean isBlinking() {
        Boolean is_blinking = true;
        try {
            is_blinking = loadPreferences(sharedPreferences, BLINKING_PREFERENCES_KEY, true);
        } catch (IOException e) {
            if (isDebug) Log.w(LOG_TAG , e.toString());
        }
        return is_blinking;
    }

    public Boolean isGraphic() {
        Boolean is_graphic = true;
        try {
            is_graphic = loadPreferences(sharedPreferences, GRAPHIC_PREFERENCES_KEY, true);
        } catch (IOException e) {
            if (isDebug) Log.w(LOG_TAG , e.toString());
        }
        return is_graphic;
    }
}
