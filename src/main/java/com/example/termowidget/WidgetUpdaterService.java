package com.example.termowidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.RemoteViews;


public class WidgetUpdaterService extends IntentService{

    final static String LOG_TAG = "WidgetUpdaterService";
    static private TermoBroadCastReceiver termoBroadCastReceiver = new TermoBroadCastReceiver() ;

    private static Boolean isPendingIntentSet =false;

    public WidgetUpdaterService(){
        super("WidgetUpdaterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //  register receiver to catch ACTION_BATTERY_CHANGED
        this.registerReceiver(termoBroadCastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //  set PendingIntent to start ConfigActivity at first time WidgetUpdaterService runned
        if(isPendingIntentSet==false){
            setOnClickPendingIntent(this);
            isPendingIntentSet=true;
        }

        Log.d(LOG_TAG, "WidgetUpdaterService Started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
            try {
                this.unregisterReceiver(termoBroadCastReceiver);
                Log.d(LOG_TAG, "termoBroadCastReceiver unregistered");
            }
            catch (Exception e){
                Log.d(LOG_TAG, "termoBroadCastReceiver is not registered yet");
            }
        Log.d(LOG_TAG, "WidgetUpdaterService Destroy");
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

        Log.d(LOG_TAG, "PendingIntent set");
    }
}
