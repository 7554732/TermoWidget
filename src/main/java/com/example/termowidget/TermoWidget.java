package com.example.termowidget;

import java.util.Timer;
import java.util.TimerTask;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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


        //  start MainService to manage TermoWidget
        context.startService(new Intent(context, MainService.class));

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
}