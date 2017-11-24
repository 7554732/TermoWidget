package com.example.termowidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.termowidget.TermoBroadCastReceiver.DIVISOR_ML_SEC;

//  Widget for Android displays the temperature of the battery


public class TermoWidget extends AppWidgetProvider {

    public static final boolean isDebug = true;
    public static final String LOG_TAG = "TermoWidget";

    private static CircleWidgetUpdater circleWidgetUpdater;
    private static QuickSharedPreferences quickSharedPreferences;

    private static Context m_context;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        m_context = context;
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(context);

        Integer update_time = quickSharedPreferences.getUpdateTime();

        //  start  permanently widget update
        startCircleWidgetUpdater(update_time);

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

    private static void stopCircleWidgetUpdater(){
        try{
            circleWidgetUpdater.cancel();
            if (isDebug) Log.d(LOG_TAG, "circleWidgetUpdater canceled");
        }
        catch(Exception e){
            if (isDebug) Log.d(LOG_TAG, "circleWidgetUpdater does not exist");
        }
    }

    public static void startCircleWidgetUpdater(Integer update_time){
        //  stop permanently widget update
        stopCircleWidgetUpdater();

        //  run permanently widget update
        circleWidgetUpdater = new CircleWidgetUpdater(update_time);
        circleWidgetUpdater.schedule();
    }

    private static class CircleWidgetUpdater extends TimerTask {

        final private int DELAY_FIRST_TIME;
        final private int UPDATE_TIME;

        private Timer timer = new Timer();

        //   Restart WidgetUpdaterService to get new temperature
        public void run(){
            m_context.startService(new Intent(m_context,WidgetUpdaterService.class));
        }

        public CircleWidgetUpdater(Integer update_time){
            if(update_time >  m_context.getResources().getInteger(R.integer.UPDATE_TIME)){
                UPDATE_TIME = update_time;
            }
            else{
                UPDATE_TIME = m_context.getResources().getInteger(R.integer.UPDATE_TIME);
            }
            DELAY_FIRST_TIME = m_context.getResources().getInteger(R.integer.DELAY_FIRST_TIME);
        }

        //  schedule itself using local constants
        public void schedule(){
            timer.schedule(this, DELAY_FIRST_TIME, UPDATE_TIME);
        }
    }

}