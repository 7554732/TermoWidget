package com.example.termowidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Timer;
import java.util.TimerTask;

public class TermoBroadCastReceiver extends BroadcastReceiver {

    final static String LOG_TAG = "TermoBroadCastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        //  get battery temperature from intent extra
        int batteryTemper = (int)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;
        //  set temperature to widget
        setTemperature(context,batteryTemper);
    }

    private void setTemperature(Context context, int batteryTemper){
        //  get RemoteViews by package name
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        //  set temperature string
        widgetView.setTextViewText(R.id.tvTemperature,Integer.toString(batteryTemper)+context.getString(R.string.degree));

        // find matched color for temperature
        for (TermoColor termoColor : TermoColor.values())
            if (batteryTemper>termoColor.temperature()){
                //  set color for widget text
                widgetView.setTextColor(R.id.tvTemperature,context.getResources().getColor(termoColor.color()));
                break;
            }

        Log.d(LOG_TAG, "batteryTemper "+batteryTemper);

        //  widget visibility restore
        Blinker blinker = new Blinker(context);
        blinker.schedule();

        //  update widget
        updateWidget(context,widgetView);
    }

    enum TermoColor{
        //  each temperature match color
        HEAT(25, R.color.colorHeat), HOT(20, R.color.colorHot), NORMAL(15, R.color.colorNormal), COOL(10, R.color.colorCool),
        COLD(5, R.color.colorCold), CHILLY(0, R.color.colorChilly), FREEZE(-5, R.color.colorFreeze), FROST(-10, R.color.colorFrost),
        HFROST(-15, R.color.colorHFrost);

        private final int temperature;
        private final  int color;

        TermoColor(int temperature, int color){
            this.temperature=temperature;
            this.color=color;

        }

        int temperature(){return temperature;}

        int color(){return color;}
    }

    private void updateWidget(Context context, RemoteViews widgetView){
        //  get widget id from context
        ComponentName widgetID = new ComponentName(context,TermoWidget.class);
        //  get widget menager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //  update widget
        appWidgetManager.updateAppWidget(widgetID, widgetView);
        Log.d(LOG_TAG, "updateWidget "+widgetID);
    }

    private class Blinker extends TimerTask {

        private Context m_context;

        private int counterExecution = 0;
        final private  int SECOND_EXECUTION = 2;

        final private int DELAY_FIRST_TIME;
        final private int BLINK_DELAY_TIME;

        private Timer timer = new Timer();

        public void run(){
            //  get RemoteViews by package name
            RemoteViews widgetView = new RemoteViews(m_context.getPackageName(), R.layout.widget);

            if (++counterExecution >= SECOND_EXECUTION){
                //  set visible
                widgetView.setViewVisibility(R.id.tvTemperature, View.VISIBLE);
                this.cancel();
            }
            else{
                //  set invisible
                widgetView.setViewVisibility(R.id.tvTemperature, View.INVISIBLE);

            }

            //  update widget
            updateWidget(m_context,widgetView);
        }

        public Blinker(Context context){
            m_context=context;
            DELAY_FIRST_TIME = m_context.getResources().getInteger(R.integer.DELAY_FIRST_TIME);
            BLINK_DELAY_TIME = m_context.getResources().getInteger(R.integer.BLINK_DELAY_TIME);
        }

        //  schedule itself using local constants
        public void schedule(){
            timer.schedule(this, DELAY_FIRST_TIME, BLINK_DELAY_TIME);
        }
    }

}
