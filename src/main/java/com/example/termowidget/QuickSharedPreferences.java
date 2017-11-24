package com.example.termowidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;
import static com.example.termowidget.TermoBroadCastReceiver.DIVISOR_ML_SEC;
import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;
import static com.example.termowidget.TermoWidget.startCircleWidgetUpdater;

public class QuickSharedPreferences {

    public static final String PREFERENCES_FILE_NAME  = "config";
    public static final String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";
    public static final String BLINKING_PREFERENCES_KEY = "is_blinking";
    public static final String GRAPHIC_PREFERENCES_KEY = "is_graphic";

    public static final String CALIBRATE_PREFERENCES_KEY = "calibration_temperature";
    public static final String UPDATE_TIME_PREFERENCES_KEY = "update_time";

    private static SharedPreferences sharedPreferences;
    private Context m_context;

    private static Integer calibrationTemperature;
    private static Integer updateTime;

    private static Boolean is_status_bar;
    private static Boolean is_graphic;
    private static Boolean is_blinking;

    public QuickSharedPreferences(Context context){
        m_context = context;
        //  set sharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
    }

    private Boolean loadPreferences (SharedPreferences sharedPreferences, String key, Boolean defaultValue) throws IOException{

        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getBoolean(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exist");
        }
    }

    private Integer loadPreferences (SharedPreferences sharedPreferences, String key, Integer defaultValue) throws IOException{

        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getInt(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exist");
        }
    }

    private void savePreferences(SharedPreferences sharedPreferences, String key, Boolean value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putBoolean(key, value);
        preferencesEditor.apply();
    }

    private void savePreferences(SharedPreferences sharedPreferences, String key, Integer value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putInt(key, value);
        preferencesEditor.apply();
    }

    public void saveBoolean (String key, Boolean value){
        savePreferences(sharedPreferences, key, value);
        switch (key){
            case STATUS_BAR_PREFERENCES_KEY:
                is_status_bar = value;
                break;
            case BLINKING_PREFERENCES_KEY:
                is_blinking = value;
                break;
            case GRAPHIC_PREFERENCES_KEY:
                is_graphic = value;
                break;
        }
    }

    public void saveInteger (String key, Integer value){
        switch (key){
            case CALIBRATE_PREFERENCES_KEY:
                calibrationTemperature = value;
                break;
            case UPDATE_TIME_PREFERENCES_KEY:
                value *= DIVISOR_ML_SEC;
                updateTime = value;
                startCircleWidgetUpdater(updateTime);
                break;
        }
        savePreferences(sharedPreferences, key, value);
    }

    public Integer getCalibrationTemperature() {
        if (calibrationTemperature == null){
            try {
                calibrationTemperature = loadPreferences(sharedPreferences, CALIBRATE_PREFERENCES_KEY, 0);
            } catch (IOException e) {
                if (isDebug) Log.w(LOG_TAG, e.toString());
                calibrationTemperature = 0;
            }
        }
        return calibrationTemperature;
    }

    public Integer getUpdateTime() {
        if (updateTime == null){
            try {
                updateTime = loadPreferences(sharedPreferences, UPDATE_TIME_PREFERENCES_KEY,  m_context.getResources().getInteger(R.integer.UPDATE_TIME));
            } catch (IOException e) {
                if (isDebug) Log.w(LOG_TAG, e.toString());
                updateTime =  m_context.getResources().getInteger(R.integer.UPDATE_TIME);
            }
        }
        return updateTime;
    }

    public Boolean isStatusBar() {
        if(is_status_bar == null){
            try {
                is_status_bar = loadPreferences(sharedPreferences, STATUS_BAR_PREFERENCES_KEY, false);
            } catch (IOException e) {
                if (isDebug) Log.w(LOG_TAG , e.toString());
                is_status_bar = false;
            }
        }
        return is_status_bar;
    }

    public Boolean isBlinking() {
        if(is_blinking == null) {
            try {
                is_blinking = loadPreferences(sharedPreferences, BLINKING_PREFERENCES_KEY, true);
            } catch (IOException e) {
                if (isDebug) Log.w(LOG_TAG, e.toString());
                is_blinking = true;
            }
        }
        return is_blinking;
    }

    public Boolean isGraphic() {
        if(is_graphic == null){
            try {
                is_graphic = loadPreferences(sharedPreferences, GRAPHIC_PREFERENCES_KEY, true);
            } catch (IOException e) {
                if (isDebug) Log.w(LOG_TAG , e.toString());
                is_graphic = true;
            }
        }
        return is_graphic;
    }
}
