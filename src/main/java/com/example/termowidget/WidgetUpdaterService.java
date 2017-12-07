package com.example.termowidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Date;

import static com.example.termowidget.TermoBroadCastReceiver.DIVISOR_ML_SEC;
import static com.example.termowidget.TermoBroadCastReceiver.isTimeAddToDB;
import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;
import static com.example.termowidget.TermoWidget.quickSharedPreferences;


public class WidgetUpdaterService extends IntentService{

    private static final Integer WAIT_FOR_RECEIVER_READY = 300;

    private Date date = new Date();
    private static Integer registerReceiverTime;
    private static TermoBroadCastReceiver termoBroadCastReceiver = new TermoBroadCastReceiver() ;

    private static PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;

    public WidgetUpdaterService(){
        super("WidgetUpdaterService");
        registerReceiverTime = (int) (date.getTime()/DIVISOR_ML_SEC);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Integer curTime =(int) (date.getTime()/DIVISOR_ML_SEC);
        Integer secondsFromLastRegisterReceiver = curTime - registerReceiverTime;

        if( TermoBroadCastReceiver.isReady()
            || (secondsFromLastRegisterReceiver >= WAIT_FOR_RECEIVER_READY )){

            //  screen on to receive properly temperature
            setScreenOn(this, isTimeAddToDB());

            //  register receiver to catch ACTION_BATTERY_CHANGED
            this.registerReceiver(termoBroadCastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            registerReceiverTime = curTime;
            TermoBroadCastReceiver.setReady(false);
            if (isDebug) Log.d(LOG_TAG , "termoBroadCastReceiver registered");

        }

        if (isDebug) Log.d(LOG_TAG , "WidgetUpdaterService Started");
        return super.onStartCommand(intent, flags, startId);
    }

    // Sets screenOn:
    public static void setScreenOn(Context context, Boolean screenOn) {

        if(screenOn == true) {
            if(quickSharedPreferences.isGraphic()){
                acquireWakelock(context);
            }
        } else {
            releaseWakelock();
        }
    }

    private static void acquireWakelock(Context context)
    {
        if(powerManager == null) powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if(wakeLock == null) wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "setScreenOn");

        if ((wakeLock != null) && (!wakeLock.isHeld()))
        {
            wakeLock.acquire();
            if (isDebug) Log.d(LOG_TAG , "acquire FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP");
        }
    }

    private static void releaseWakelock()
    {
        if ((wakeLock != null) && (wakeLock.isHeld()))
        {
            wakeLock.release();
            if (isDebug) Log.d(LOG_TAG , "release FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP");
        }
    }

    @Override
    public void onDestroy() {
            try {
                this.unregisterReceiver(termoBroadCastReceiver);
                if (isDebug) Log.d(LOG_TAG , "termoBroadCastReceiver unregistered");
            }
            catch (Exception e){
                if (isDebug) Log.w(LOG_TAG , "termoBroadCastReceiver is not registered yet");
            }
        if (isDebug) Log.d(LOG_TAG , "WidgetUpdaterService Destroy");
        super.onDestroy();
    }

}
