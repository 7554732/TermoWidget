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
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import static android.content.Context.ALARM_SERVICE;

//  Widget for Android displays the temperature of the battery


public class TermoWidget extends AppWidgetProvider {

    final static String LOG_TAG = "TermoWidget";


    final static private int DELAY_FIRST_TIME = 500;
    final static private int UPDATE_TIME = 5000;

    private static AlarmManager mAlarmManager;
    public static PendingIntent pIntentWidgetUpdaterService;

    private static QuickSharedPreferences quickSharedPreferences;
    private static PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(context);

        stopAlarmManager(pIntentWidgetUpdaterService);

        //  run permanently widget update
        pIntentWidgetUpdaterService = setAlarmManager(context);

        //  start ScreenStateService  to catch ACTION_SCREEN_ON
        context.startService(new Intent(context, ScreenStateService.class));

        Log.d(LOG_TAG, "TermoWidget Updated");

    }

    public static void stopAlarmManager(PendingIntent pIntent) {
        if(pIntent != null) {
            mAlarmManager.cancel(pIntent);
            Log.d(LOG_TAG, "AlarmManager canceled");
        }
    }

    public static PendingIntent setAlarmManager(Context context) {
        Intent intent = new Intent(context,WidgetUpdaterService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        Integer amType;
        String amTypeString;

        powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getPackageName().toString());

        if(quickSharedPreferences.isGraphic()){
            amType = AlarmManager.RTC_WAKEUP;
            amTypeString = "RTC_WAKEUP";
            wakeLock.acquire();
        }
        else {
            amType = AlarmManager.RTC;
            amTypeString = "RTC";
            if(wakeLock.isHeld()){
                wakeLock.release();
            }
        }

        mAlarmManager.setRepeating(amType, System.currentTimeMillis() + DELAY_FIRST_TIME, UPDATE_TIME, pIntent);
        Log.d(LOG_TAG, "AlarmManager runned. amType: " + amTypeString);
        return pIntent;
    }



    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        //  stop ScreenStateService
        context.stopService(new Intent(context, ScreenStateService.class));

        stopAlarmManager(pIntentWidgetUpdaterService);

        Log.d(LOG_TAG, "TermoWidget Disabled");
    }
}