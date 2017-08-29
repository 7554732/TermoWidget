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
import android.widget.RemoteViews;
import android.widget.Toast;

import static java.lang.Thread.sleep;

//  Widget for Android displays the temperature of the battery
//
//  Plan to add:
//      config,
//      status bar,
//      temperature graphic

public class TermoWidget extends AppWidgetProvider {

    final static String LOG_TAG = "TermoWidget";
    final static int UPDATE_TIME = 10000;
    final static int DELAY_TIME = 1000;
    private static Timer timer = new Timer();
    public static CircleWidgetUpdater circleWidgetUpdater;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        if(circleWidgetUpdater!=null) circleWidgetUpdater.cancel();

        //  run permanently widget update
        circleWidgetUpdater = new CircleWidgetUpdater();
        circleWidgetUpdater.setContext(context);
        timer.schedule(circleWidgetUpdater, DELAY_TIME,UPDATE_TIME);

        //  start MainService to manage TermoWidget
        context.startService(new Intent(context, MainService.class));


        Log.d(LOG_TAG, "TermoWidget Updated");

    }


    static public class MainService extends Service {

        @Override
        public void onCreate() {
            super.onCreate();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

            //  register reciver to catch ACTION_SCREEN_ON
            try {
                this.unregisterReceiver(screenStateReceiver);
            }
            catch (Exception e){
                this.registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
                Log.d(LOG_TAG, "screenStateReceiver  registered");
            }

            Log.d(LOG_TAG, "MainService Started");

            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(LOG_TAG, "MainService Destroy");
        }

        @Override
        public IBinder onBind(Intent intent) {
            Log.d(LOG_TAG, "MainService onBind");
            return null;
        }
    }

    static private BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //  run widget update
            context.startService(new Intent(context,WidgetUpdaterService.class));
        }
    };

    public static class CircleWidgetUpdater extends TimerTask {
        private Context m_context;
        //   Restart WidgetUpdaterService to get new temperature
        public void run(){
            m_context.startService(new Intent(m_context,WidgetUpdaterService.class));
        }
        public void setContext(Context context){
            m_context=context;
        }
    }

    static public class WidgetUpdaterService extends IntentService{
        public WidgetUpdaterService(){
            super("TermoWidget$WidgetUpdaterService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

            //  register reciver to catch ACTION_BATTERY_CHANGED
            this.registerReceiver(termoBroadCastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            Log.d(LOG_TAG, "WidgetUpdaterService Started");
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
            Log.d(LOG_TAG, "WidgetUpdaterService Destroy");
            super.onDestroy();
        }
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

    static private BroadcastReceiver termoBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //  get battery temperature from intent extra
            int batteryTemper = (int)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;
            //  set temperature to widget
            setTemperature(context,batteryTemper);
        }
    };

    private static void setTemperature(Context context, int batteryTemper){
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

        //  get widget id from context
        ComponentName widgetID = new ComponentName(context,TermoWidget.class);
        //  get widget menager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //  update widget
        appWidgetManager.updateAppWidget(widgetID, widgetView);
        Log.d(LOG_TAG, "updateAppWidget "+widgetID);
    }
}