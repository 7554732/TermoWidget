package com.example.termowidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

//  Widget for Android displays the temperature of the battery


public class TermoWidget extends AppWidgetProvider {

    public static final boolean isDebug = true;
    public static final String LOG_TAG = "TermoWidget";

    private static CircleWidgetUpdater circleWidgetUpdater;
	
    private static QuickSharedPreferences quickSharedPreferences;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);


        //  stop permanently widget update
        stopCircleWidgetUpdater();

        //  run permanently widget update
        circleWidgetUpdater = new CircleWidgetUpdater(context);
        circleWidgetUpdater.schedule();

        //  start ScreenStateService  to catch ACTION_SCREEN_ON
        context.startService(new Intent(context, ScreenStateService.class));

        if (isDebug) Log.d(LOG_TAG , "TermoWidget Updated");

    }


    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        //  stop ScreenStateService
        context.stopService(new Intent(context, ScreenStateService.class));
        //  stop permanently widget update
        stopCircleWidgetUpdater();
        if (isDebug) Log.d(LOG_TAG, "TermoWidget Disabled");
    }

    private void stopCircleWidgetUpdater(){
        try{
            circleWidgetUpdater.cancel();
            if (isDebug) Log.d(LOG_TAG, "circleWidgetUpdater canceled");
        }
        catch(Exception e){
            if (isDebug) Log.d(LOG_TAG, "circleWidgetUpdater does not exist");
        }
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