package com.example.termowidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.termowidget.TermoBroadCastReceiver.DIVISOR_ML_SEC;
import static com.example.termowidget.TermoBroadCastReceiver.isTimeAddToDB;
import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;


public class WidgetUpdaterService extends Service {

    private static final int DELAY_FIRST_TIME = 500;    //  (milliseconds)  before first circle widget update
    public static final int MIN_UPDATE_TIME = 5000;    //  (milliseconds) between circle widget updates
    private static final int WAIT_FOR_RECEIVER_READY = 300;     //  (seconds)   wait to register receiver if it is not ready

    private Date date = new Date();

    private static int registerReceiverTime;

    private TermoBroadCastReceiver termoBroadCastReceiver = new TermoBroadCastReceiver() ;
    private QuickSharedPreferences quickSharedPreferences;
    private CircleWidgetUpdater circleWidgetUpdater;

    private static PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(this);

        registerReceiverTime = (int) (date.getTime()/DIVISOR_ML_SEC);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //  schedule circle widget update
        int update_time = quickSharedPreferences.getUpdateTime();

        if(circleWidgetUpdater != null){
            circleWidgetUpdater.cancel();
        }

        circleWidgetUpdater = new CircleWidgetUpdater();
        Timer timer = new Timer();
        timer.schedule(circleWidgetUpdater, DELAY_FIRST_TIME, update_time);

        if (isDebug) Log.d(LOG_TAG , "WidgetUpdaterService Started");
        return super.onStartCommand(intent, flags, startId);
    }

    private class CircleWidgetUpdater extends TimerTask {
        //   Restart TermoBroadCastReceiver to get new temperature
        public void run() {
            registerTermoBroadCastReceiver();
            if (isDebug) Log.d(LOG_TAG , "CircleWidgetUpdater Started");
        }
    }

    private void registerTermoBroadCastReceiver(){
        int curTime = (int) (date.getTime() / DIVISOR_ML_SEC);
        int secondsFromLastRegisterReceiver = curTime - registerReceiverTime;

        if( TermoBroadCastReceiver.isReady()
                || (secondsFromLastRegisterReceiver > WAIT_FOR_RECEIVER_READY )){
            //  screen on to receive properly temperature
            setScreenOn(this, isTimeAddToDB());

            //  register receiver to catch ACTION_BATTERY_CHANGED
            registerReceiver(termoBroadCastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            registerReceiverTime = curTime;
            TermoBroadCastReceiver.setReady(false);

            if (isDebug) Log.d(LOG_TAG , "TermoBroadCastReceiver registered");
        }
    }

    private void unregisterTermoBroadCastReceiver(){
        try {
            unregisterReceiver(termoBroadCastReceiver);
            if (isDebug) Log.d(LOG_TAG , "termoBroadCastReceiver unregistered");
        }
        catch (Exception e){
            if (isDebug) Log.w(LOG_TAG , "termoBroadCastReceiver is not registered yet");
        }
    }

    // Sets screenOn:
    private void setScreenOn(Context context, Boolean screenOn) {
        if(screenOn == true) {
            if(quickSharedPreferences.isGraphic()){
                acquireWakelock(context);
            }
        } else {
            releaseWakelock();
        }
    }

    private void acquireWakelock(Context context){
        if(powerManager == null){
            powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        }
        if(wakeLock == null){
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "setScreenOn");
        }

        if ((wakeLock != null) && (!wakeLock.isHeld()))
        {
            wakeLock.acquire();
            if (isDebug) Log.d(LOG_TAG , "acquire FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP");
        }
    }

    private void releaseWakelock(){
        if ((wakeLock != null) && (wakeLock.isHeld()))
        {
            wakeLock.release();
            if (isDebug) Log.d(LOG_TAG , "release FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP");
        }
    }

    @Override
    public void onDestroy() {
        circleWidgetUpdater.cancel();
        unregisterTermoBroadCastReceiver();

        if (isDebug) Log.d(LOG_TAG , "WidgetUpdaterService Destroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (isDebug) Log.d(LOG_TAG , "ScreenStateService onBind");
        return null;
    }

}
