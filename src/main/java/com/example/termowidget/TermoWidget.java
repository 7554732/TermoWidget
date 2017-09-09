package com.example.termowidget;

import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

//  Widget for Android displays the temperature of the battery
//
//  Plan to add:
//      status bar,
//      temperature graphic

public class TermoWidget extends AppWidgetProvider {

    final static String LOG_TAG = "TermoWidget";
    static private   CircleWidgetUpdater circleWidgetUpdater;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);


        try{
            circleWidgetUpdater.cancel();
            Log.d(LOG_TAG, "circleWidgetUpdater canceled");
        }
        catch(Exception e){
            Log.d(LOG_TAG, "circleWidgetUpdater does not exist");
        }

        //  run permanently widget update
        circleWidgetUpdater = new CircleWidgetUpdater(context);
        circleWidgetUpdater.schedule();

        //  start ScreenStateService  to catch ACTION_SCREEN_ON
        context.startService(new Intent(context, ScreenStateService.class));

        Log.d(LOG_TAG, "TermoWidget Updated");

    }


    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        //  stop ScreenStateService
        context.stopService(new Intent(context, ScreenStateService.class));
        //  stop permanently widget update
        try{
            circleWidgetUpdater.cancel();
            Log.d(LOG_TAG, "circleWidgetUpdater canceled");
        }
        catch(Exception e){
            Log.d(LOG_TAG, "circleWidgetUpdater does not exist");
        }

        Log.d(LOG_TAG, "TermoWidget Disabled");
    }

    private class CircleWidgetUpdater extends TimerTask {

        private Context m_context;

        final private int DELAY_FIRST_TIME;
        final private int UPDATE_TIME;

        private Timer timer = new Timer();

        private Boolean isPendingIntentSet =false;

        //   Restart WidgetUpdaterService to get new temperature
        public void run(){
            m_context.startService(new Intent(m_context,WidgetUpdaterService.class));
            //  set PendingIntent to start ConfigActivity at first time CircleWidgetUpdater runned
            if(isPendingIntentSet==false){
                setOnClickPendingIntent(m_context);
                isPendingIntentSet=true;
            }
        }

        public CircleWidgetUpdater(Context context){
            m_context=context;
            UPDATE_TIME = m_context.getResources().getInteger(R.integer.UPDATE_TIME);
            DELAY_FIRST_TIME = m_context.getResources().getInteger(R.integer.DELAY_FIRST_TIME);
        }

        //  schedule itself using local constants
        public void schedule(){
            timer.schedule(this, DELAY_FIRST_TIME, UPDATE_TIME);
        }
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