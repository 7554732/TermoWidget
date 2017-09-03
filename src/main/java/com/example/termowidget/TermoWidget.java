package com.example.termowidget;

import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
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
        circleWidgetUpdater = new CircleWidgetUpdater();
        circleWidgetUpdater.setContext(context);
        circleWidgetUpdater.timer.schedule(circleWidgetUpdater, circleWidgetUpdater.DELAY_FIRST_TIME, circleWidgetUpdater.UPDATE_TIME);


        //  start MainService to manage TermoWidget
        context.startService(new Intent(context, MainService.class));

        Log.d(LOG_TAG, "TermoWidget Updated");

    }

    public class CircleWidgetUpdater extends TimerTask {

        final int UPDATE_TIME = 10000;
        final int DELAY_FIRST_TIME = 10;

        public   Timer timer = new Timer();

        private Context m_context;
        //   Restart WidgetUpdaterService to get new temperature
        public void run(){
            m_context.startService(new Intent(m_context,WidgetUpdaterService.class));
        }
        public void setContext(Context context){
            m_context=context;
        }
    }



}