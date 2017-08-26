package com.example.termowidget;

import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;


public class TermoWidget extends AppWidgetProvider {

    final static String LOG_TAG = "TermoWidget";
    final static int UPDATE_TIME = 10000;
    final static int DELAY_TIME = 1000;
    private static Timer timer = new Timer();
    public static CircleWidgetUpdater circleWidgetUpdater;

    public static class CircleWidgetUpdater extends TimerTask {
        private Context m_context;
        public void run(){
            m_context.stopService(new Intent(m_context,MainService.class));
            m_context.startService(new Intent(m_context,MainService.class));
        }
        public void setContext(Context context){
            m_context=context;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        if(circleWidgetUpdater!=null) circleWidgetUpdater.cancel();

        circleWidgetUpdater = new CircleWidgetUpdater();
        circleWidgetUpdater.setContext(context);
        timer.schedule(circleWidgetUpdater, DELAY_TIME,UPDATE_TIME);


        Log.d(LOG_TAG, "TermoWidget Updated");

    }

    static public class MainService extends IntentService{
        public MainService(){
            super("TermoWidget$MainService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {


            this.registerReceiver(termoBroadCastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            Log.d(LOG_TAG, "MainService Started");
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public void onDestroy() {
            try {
                this.unregisterReceiver(termoBroadCastReceiver);
            }
            catch (Exception e){
                Log.d(LOG_TAG, "termoBroadCastReceiver is not registered yet");
            }
            Log.d(LOG_TAG, "MainService Destroy");
            super.onDestroy();
        }
    }

    enum TermoColor{
        HEAT(25, R.color.colorHeat), HOT(20, R.color.colorHot), NORMAL(15, R.color.colorNormal), COOL(10, R.color.colorCool),
        COLD(5, R.color.colorCold), CHILLY(0, R.color.colorChilly), FREEZE(-5, R.color.colorFreeze), FROST(-10, R.color.colorFrost),
        HFROST(-15, R.color.colorHFrost)
        ;
        private final int temperature;
        private final  int color;

        TermoColor(int temperature, int color){
            this.temperature=temperature;
            this.color=color;

        }

        int temperature(){return temperature;}

        int color(){return color;}
    }

    static private BroadcastReceiver termoBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        int batteryTemper = (int)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;

        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        widgetView.setTextViewText(R.id.tvTemperature,Integer.toString(batteryTemper)+context.getString(R.string.degree));


        for (TermoColor termoColor : TermoColor.values())
            if (batteryTemper>termoColor.temperature()){
                widgetView.setTextColor(R.id.tvTemperature,context.getResources().getColor(termoColor.color()));
                break;
            }

        Log.d(LOG_TAG, "batteryTemper "+batteryTemper);

        ComponentName widgetID = new ComponentName(context,TermoWidget.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(widgetID, widgetView);
        Log.d(LOG_TAG, "updateAppWidget "+widgetID);
        }
    };

}