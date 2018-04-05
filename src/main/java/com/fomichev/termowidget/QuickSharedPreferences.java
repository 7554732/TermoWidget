package com.fomichev.termowidget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;
import static com.fomichev.termowidget.TermoBroadCastReceiver.DIVISOR_ML_SEC;
import static com.fomichev.termowidget.WidgetUpdaterService.MIN_UPDATE_TIME;

public class QuickSharedPreferences {

    public static final String PREFERENCES_FILE_NAME  = "config";
    public static final String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";
    public static final String BLINKING_PREFERENCES_KEY = "is_blinking";
    public static final String GRAPHIC_PREFERENCES_KEY = "is_graphic";

    public static final String CALIBRATE_PREFERENCES_KEY = "calibration_temperature";
    public static final String UPDATE_TIME_PREFERENCES_KEY = "update_time";

    private static SharedPreferences sharedPreferences;
    private Context mContext;

    private static Integer calibrationTemperature;
    private static Integer updateTime;

    private static Boolean is_status_bar;
    private static Boolean is_graphic;
    private static Boolean is_blinking;

    public QuickSharedPreferences(Context context){
        mContext = context;
        //  set sharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);
    }

    private boolean loadPreferences (SharedPreferences sharedPreferences, String key, boolean defaultValue) throws IOException{
        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getBoolean(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exist");
        }
    }

    private int loadPreferences (SharedPreferences sharedPreferences, String key, int defaultValue) throws IOException{
        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getInt(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exist");
        }
    }

    private void savePreferences(SharedPreferences sharedPreferences, String key, boolean value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putBoolean(key, value);
        preferencesEditor.apply();
    }

    private void savePreferences(SharedPreferences sharedPreferences, String key, int value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putInt(key, value);
        preferencesEditor.apply();
    }

    public void saveBoolean (String key, boolean value){
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

    public void saveInteger (String key, int value){
        switch (key){
            case CALIBRATE_PREFERENCES_KEY:
                calibrationTemperature = value;
                break;
            case UPDATE_TIME_PREFERENCES_KEY:
                value *= DIVISOR_ML_SEC;
                updateTime = value;
                mContext.startService(new Intent(mContext,WidgetUpdaterService.class));
                break;
        }
        savePreferences(sharedPreferences, key, value);
    }

    public int getCalibrationTemperature() {
        if (calibrationTemperature == null){
            try {
                calibrationTemperature = loadPreferences(sharedPreferences, CALIBRATE_PREFERENCES_KEY, 0);
            } catch (IOException e) {
                if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG, e.toString());
                calibrationTemperature = 0;
            }
        }
        return calibrationTemperature;
    }

    public int getUpdateTime() {
        if (updateTime == null){
            try {
                updateTime = loadPreferences(sharedPreferences, UPDATE_TIME_PREFERENCES_KEY,  MIN_UPDATE_TIME);
            } catch (IOException e) {
                if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG, e.toString());
                updateTime = MIN_UPDATE_TIME;
            }
        }
        return updateTime;
    }

    public boolean isStatusBar() {
        if(is_status_bar == null){
            try {
                is_status_bar = loadPreferences(sharedPreferences, STATUS_BAR_PREFERENCES_KEY, false);
            } catch (IOException e) {
                if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG , e.toString());
                is_status_bar = false;
            }
        }
        return is_status_bar;
    }

    public boolean isBlinking() {
        if(is_blinking == null) {
            try {
                is_blinking = loadPreferences(sharedPreferences, BLINKING_PREFERENCES_KEY, true);
            } catch (IOException e) {
                if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG, e.toString());
                is_blinking = true;
            }
        }
        return is_blinking;
    }

    public boolean isGraphic() {
        if(is_graphic == null){
            try {
                is_graphic = loadPreferences(sharedPreferences, GRAPHIC_PREFERENCES_KEY, true);
            } catch (IOException e) {
                if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG , e.toString());
                is_graphic = true;
            }
        }
        return is_graphic;
    }
}
