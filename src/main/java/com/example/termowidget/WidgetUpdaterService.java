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

import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;


public class WidgetUpdaterService extends IntentService{

    static private TermoBroadCastReceiver termoBroadCastReceiver = new TermoBroadCastReceiver() ;

    private static Boolean isOnClickPendingIntentSet =false;

    private static PowerManager powerManager;
    private static PowerManager.WakeLock wakeLock;

    private static QuickSharedPreferences quickSharedPreferences;

    public WidgetUpdaterService(){
        super("WidgetUpdaterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //  screen on to receive properly temperature
        setScreenOn(this, TermoBroadCastReceiver.isTimeAddToDB());

        //  register receiver to catch ACTION_BATTERY_CHANGED
        this.registerReceiver(termoBroadCastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (isDebug) Log.d(LOG_TAG , "termoBroadCastReceiver registered");

        //  set PendingIntent to start ConfigActivity if WidgetUpdaterService has been runned first time
        if(isOnClickPendingIntentSet==false){
            isOnClickPendingIntentSet=true;
        }
        setOnClickPendingIntent(this);

        if (isDebug) Log.d(LOG_TAG , "WidgetUpdaterService Started");
        return super.onStartCommand(intent, flags, startId);
    }

    // Sets screenOn:
    public static void setScreenOn(Context context, Boolean screenOn) {

        if(screenOn == true) {
            if(quickSharedPreferences == null) quickSharedPreferences = new QuickSharedPreferences(context);

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

    private void  setOnClickPendingIntent(Context context){

        //  create PendingIntent from Intent to start ConfigActivity
        Intent configIntent = new Intent(context, ConfigActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //  get RemoteViews by package name
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

        widgetView.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        //  get widget menager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //  update widget
        ComponentName componentName = new ComponentName(context,TermoWidget.class);
        appWidgetManager.updateAppWidget(componentName, widgetView);

        if (isDebug) Log.d(LOG_TAG , "PendingIntent set");
    }
}
