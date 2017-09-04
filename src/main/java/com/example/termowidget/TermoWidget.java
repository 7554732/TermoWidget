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
        circleWidgetUpdater = new CircleWidgetUpdater();
        circleWidgetUpdater.schedule(context);


        //  start MainService to manage TermoWidget
        context.startService(new Intent(context, MainService.class));

        Log.d(LOG_TAG, "TermoWidget Updated");

    }

    private class CircleWidgetUpdater extends TimerTask {

        private Context m_context;

        private int DELAY_FIRST_TIME;
        private int UPDATE_TIME;

        private Timer timer = new Timer();

        //   Restart WidgetUpdaterService to get new temperature
        public void run(){
            m_context.startService(new Intent(m_context,WidgetUpdaterService.class));
        }

        //  initialize local variable
        private void setContext(Context context){
            m_context=context;
        }

        //  get variables from resources
        private void getResources(){
            DELAY_FIRST_TIME = m_context.getResources().getInteger(R.integer.DELAY_FIRST_TIME);
            UPDATE_TIME = m_context.getResources().getInteger(R.integer.UPDATE_TIME);
        }

        //  schedule itself using local variables
        public void schedule(Context context){
            setContext(context);
            getResources();
            timer.schedule(this, DELAY_FIRST_TIME, UPDATE_TIME);
        }
    }



}