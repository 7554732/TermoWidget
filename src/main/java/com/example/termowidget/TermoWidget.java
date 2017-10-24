package com.example.termowidget;

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import static android.content.Context.ALARM_SERVICE;

//  Widget for Android displays the temperature of the battery


public class TermoWidget extends AppWidgetProvider {

    public static final boolean isDebug = true;
    public static final String LOG_TAG = "TermoWidget";


    final static private int DELAY_FIRST_TIME = 500;
    final static private int UPDATE_TIME = 5000;

    private static AlarmManager mAlarmManager;
    public static PendingIntent pIntentWidgetUpdaterService;

    private static QuickSharedPreferences quickSharedPreferences;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        stopAlarmManager(pIntentWidgetUpdaterService);

        //  run permanently widget update
        pIntentWidgetUpdaterService = setAlarmManager(context);

        //  start ScreenStateService  to catch ACTION_SCREEN_ON
        context.startService(new Intent(context, ScreenStateService.class));

        if (isDebug) Log.d(LOG_TAG , "TermoWidget Updated");

    }

    public static void stopAlarmManager(PendingIntent pIntent) {
        if(pIntent != null) {
            mAlarmManager.cancel(pIntent);
            if (isDebug) Log.d(LOG_TAG , "AlarmManager canceled");
        }
    }

    public static PendingIntent setAlarmManager(Context context) {

        mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(context);

        Integer amType;
        String amTypeString;
        if(quickSharedPreferences.isGraphic()){
            amType = AlarmManager.RTC_WAKEUP;
            amTypeString = "RTC_WAKEUP";
        }
        else {
            amType = AlarmManager.RTC;
            amTypeString = "RTC";
        }

        Intent intent = new Intent(context,WidgetUpdaterService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mAlarmManager.setRepeating(amType, System.currentTimeMillis() + DELAY_FIRST_TIME, UPDATE_TIME, pIntent);

        if (isDebug) Log.d(LOG_TAG , "AlarmManager runned. amType: " + amTypeString);
        return pIntent;
    }



    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        //  stop ScreenStateService
        context.stopService(new Intent(context, ScreenStateService.class));

        stopAlarmManager(pIntentWidgetUpdaterService);

        if (isDebug) Log.d(LOG_TAG , "TermoWidget Disabled");
    }
}