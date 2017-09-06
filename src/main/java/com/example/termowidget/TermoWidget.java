package com.example.termowidget;

import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

//  Widget for Android displays the temperature of the battery
//
//  Plan to add:
//      config,
//      status bar,
//      temperature graphic

public class TermoWidget extends AppWidgetProvider {

    final static String LOG_TAG = "TermoWidget";
    public  CircleWidgetUpdater circleWidgetUpdater;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        if(circleWidgetUpdater!=null) circleWidgetUpdater.cancel();

        //  run permanently widget update
        circleWidgetUpdater = new CircleWidgetUpdater(context);
        circleWidgetUpdater.schedule();

        //  start ScreenStateService  to catch ACTION_SCREEN_ON
        context.startService(new Intent(context, ScreenStateService.class));

        //  set PendingIntent to start ConfigActivity
        setOnClickPendingIntent(context, appWidgetManager, appWidgetIds);

        Log.d(LOG_TAG, "TermoWidget Updated");

    }

    private class CircleWidgetUpdater extends TimerTask {

        private Context m_context;

        final private int DELAY_FIRST_TIME;
        final private int UPDATE_TIME;

        private Timer timer = new Timer();

        //   Restart WidgetUpdaterService to get new temperature
        public void run(){
            m_context.startService(new Intent(m_context,WidgetUpdaterService.class));
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

    private void  setOnClickPendingIntent(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){

        //  create PendingIntent from Intent to start ConfigActivity
        Intent configIntent = new Intent(context, ConfigActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, configIntent, 0);

        //  get RemoteViews by package name
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

        widgetView.setOnClickPendingIntent(R.id.tvTemperature, pIntent);
        //  update widget
        appWidgetManager.updateAppWidget(appWidgetIds, widgetView);
    }
}