package com.example.termowidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.name;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;

public class TermoBroadCastReceiver extends BroadcastReceiver {

    final static String LOG_TAG = "TermoBroadCastReceiver";
    final static Integer DIVISOR_ML_SEC = 1000;
    private static Integer lastTimeAddToDB = 0; // (seconds)
    private static final Integer MIN_PERIOD_ADD_TO_DB = 60; //(seconds) minimum period between adding temperature to DB
    private static Boolean flagAddToDB;

    private QuickSharedPreferences quickSharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(context);

        //  get battery temperature from intent extra
        int batteryTemper = (int)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;
        //  set temperature to widget
        setTemperature(context,batteryTemper);
        addTemperatureToDB(context,batteryTemper);
    }

    private void setTemperature(Context context, int batteryTemper){
        //  get RemoteViews by package name
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        //  set temperature string
        widgetView.setTextViewText(R.id.tvTemperature,Integer.toString(batteryTemper)+context.getString(R.string.degree));

        //  set color for widget text
        widgetView.setTextColor(R.id.tvTemperature,context.getResources().getColor(TermoColor.getColor(batteryTemper)));

        Log.d(LOG_TAG, "batteryTemper "+batteryTemper);

        //  widget blinking
        Blinker blinker = new Blinker(context);
        blinker.schedule();

        //  update widget
        updateWidget(context,widgetView);

        //  set temperature to status bar
        setIconToStatusBar(context,batteryTemper);
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

        // find matched color for temperature
        static int getColor(int batteryTemper) {
            for (TermoColor termoColor : TermoColor.values()){
                if (batteryTemper > termoColor.temperature()) {
                    return termoColor.color();
                }
            }
            return R.color.colorUnknown;
        }
    }

    private void updateWidget(Context context, RemoteViews widgetView){
        //  get name of the application component from context
        ComponentName componentName = new ComponentName(context,TermoWidget.class);
        //  get widget menager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //  update widget
        appWidgetManager.updateAppWidget(componentName, widgetView);
        Log.d(LOG_TAG, "updateWidget "+componentName);
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
            //  get blinking status
            //  if blinking is on schedule timer
            if(quickSharedPreferences.isBlinking()){
                timer.schedule(this, DELAY_FIRST_TIME, BLINK_DELAY_TIME);
            }
        }
    }

    private void setIconToStatusBar(Context context, int batteryTemper){

        Integer NOTIFICATION_ID = 1;
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if(quickSharedPreferences.isBlinking()){
            mNotifyMgr.cancel(NOTIFICATION_ID);
        }

        if (quickSharedPreferences.isStatusBar()){

            NotificationCompat.Builder status_bar_Builder = new NotificationCompat.Builder(context)
                    .setContentTitle(batteryTemper + " °");

            // find matched icon for temperature
            if (batteryTemper < TermoIcon.M31.temperature()){
                //  set icon for status bar
                status_bar_Builder.setSmallIcon(TermoIcon.M31.icon());
            }
            else if(batteryTemper > TermoIcon.P51.temperature()){
                //  set icon for status bar
                status_bar_Builder.setSmallIcon(TermoIcon.P51.icon());
            }
            else{
                status_bar_Builder.setSmallIcon(TermoIcon.getIcon(batteryTemper));
            }

            Intent status_bar_Intent = new Intent(context, ConfigActivity.class);
            PendingIntent status_bar_PendingIntent = PendingIntent.getActivity(context, 0, status_bar_Intent, PendingIntent.FLAG_UPDATE_CURRENT);
            status_bar_Builder.setContentIntent(status_bar_PendingIntent);
            Notification notification = status_bar_Builder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

            mNotifyMgr.notify(NOTIFICATION_ID, notification);
        }
    }

    enum TermoIcon{
        //  each temperature match icon
        M31 (-31, R.drawable.m31, R.drawable.m31), M30 (-30, R.drawable.m30, R.drawable.m30),
        M29 (-29, R.drawable.m29, R.drawable.m29), M28 (-28, R.drawable.m28, R.drawable.m28), M27 (-27, R.drawable.m27, R.drawable.m27), M26 (-26, R.drawable.m26, R.drawable.m26), M25 (-25, R.drawable.m25, R.drawable.m25),
        M24 (-24, R.drawable.m24, R.drawable.m24), M23 (-23, R.drawable.m23, R.drawable.m23), M22 (-22, R.drawable.m22, R.drawable.m22), M21 (-21, R.drawable.m21, R.drawable.m21), M20 (-20, R.drawable.m20, R.drawable.m20),
        M19 (-19, R.drawable.m19, R.drawable.m19), M18 (-18, R.drawable.m18, R.drawable.m18), M17 (-17, R.drawable.m17, R.drawable.m17), M16 (-16, R.drawable.m16, R.drawable.m16), M15 (-15, R.drawable.m15, R.drawable.m15),
        M14 (-14, R.drawable.m14, R.drawable.m14), M13 (-13, R.drawable.m13, R.drawable.m13), M12 (-12, R.drawable.m12, R.drawable.m12), M11 (-11, R.drawable.m11, R.drawable.m11), M10 (-10, R.drawable.m10, R.drawable.m10),
        M09 (-9, R.drawable.m09, R.drawable.m09), M08 (-8, R.drawable.m08,R.drawable.m08), M07 (-7, R.drawable.m07, R.drawable.m07), M06 (-6, R.drawable.m06, R.drawable.m06), M05 (-5, R.drawable.m05, R.drawable.m05),
        M04 (-4, R.drawable.m04, R.drawable.m04), M03 (-3, R.drawable.m03, R.drawable.m03), M02 (-2, R.drawable.m02, R.drawable.m02), M01 (-1, R.drawable.m01, R.drawable.m01),
        P00 (0, R.drawable.p00, R.drawable.p00_l),
        P01 (1, R.drawable.p01, R.drawable.p01), P02 (2, R.drawable.p02, R.drawable.p02), P03 (3, R.drawable.p03, R.drawable.p03), P04 (4, R.drawable.p04, R.drawable.p04), P05 (5, R.drawable.p05, R.drawable.p05),
        P06 (6, R.drawable.p06, R.drawable.p06), P07 (7, R.drawable.p07, R.drawable.p07), P08 (8, R.drawable.p08, R.drawable.p08), P09 (9, R.drawable.p09, R.drawable.p09), P10 (10, R.drawable.p10, R.drawable.p10),
        P11 (11, R.drawable.p11, R.drawable.p11), P12 (12, R.drawable.p12, R.drawable.p12), P13 (13, R.drawable.p13, R.drawable.p13), P14 (14, R.drawable.p14, R.drawable.p14), P15 (15, R.drawable.p15, R.drawable.p15),
        P16 (16, R.drawable.p16, R.drawable.p16), P17 (17, R.drawable.p17, R.drawable.p17), P18 (18, R.drawable.p18, R.drawable.p18), P19 (19, R.drawable.p19, R.drawable.p19), P20 (20, R.drawable.p20, R.drawable.p20),
        P21 (21, R.drawable.p21, R.drawable.p21), P22 (22, R.drawable.p22, R.drawable.p22), P23 (23, R.drawable.p23, R.drawable.p23), P24 (24, R.drawable.p24, R.drawable.p24), P25 (25, R.drawable.p25, R.drawable.p25),
        P26 (26, R.drawable.p26, R.drawable.p26), P27 (27, R.drawable.p27, R.drawable.p27), P28 (28, R.drawable.p28, R.drawable.p28), P29 (29, R.drawable.p29, R.drawable.p29), P30 (30, R.drawable.p30, R.drawable.p30),
        P31 (31, R.drawable.p31, R.drawable.p31), P32 (32, R.drawable.p32, R.drawable.p32), P33 (33, R.drawable.p33, R.drawable.p33), P34 (34, R.drawable.p34, R.drawable.p34), P35 (35, R.drawable.p35, R.drawable.p35),
        P36 (36, R.drawable.p36, R.drawable.p36), P37 (37, R.drawable.p37, R.drawable.p37), P38 (38, R.drawable.p38, R.drawable.p38), P39 (39, R.drawable.p39, R.drawable.p39), P40 (40, R.drawable.p40, R.drawable.p40),
        P41 (41, R.drawable.p41, R.drawable.p41), P42 (42, R.drawable.p42, R.drawable.p42), P43 (43, R.drawable.p43, R.drawable.p43), P44 (44, R.drawable.p44, R.drawable.p44), P45 (45, R.drawable.p45, R.drawable.p45),
        P46 (46, R.drawable.p46, R.drawable.p46), P47 (47, R.drawable.p47, R.drawable.p47), P48 (48, R.drawable.p48, R.drawable.p48), P49 (49, R.drawable.p49, R.drawable.p49), P50 (50, R.drawable.p50, R.drawable.p50),
        P51 (51, R.drawable.p51, R.drawable.p51);

        private final int temperature;
        private final  int icon;
        private final  int iconL;

        TermoIcon(int temperature, int icon, int iconL){
            this.temperature=temperature;
            this.icon=icon;
            this.iconL=iconL;
        }

        int temperature(){return temperature;}

        int icon(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                return iconL;
            }
            return icon;
        }

        int iconL(){ return iconL;}

        // find matched icon for temperature
        static int getIcon(int batteryTemper) {
            for (TermoIcon termoIcon : TermoIcon.values()){
                if (batteryTemper == termoIcon.temperature()) {
                    return termoIcon.icon();
                }
            }
            return R.mipmap.ic_launcher;
        }
    }

    private void addTemperatureToDB(Context context, int batteryTemper) {
        //  if graphic is off no not add data to DB
        if(quickSharedPreferences.isGraphic() == false){
            flagAddToDB = false;
            return;
        }
        if (flagAddToDB){

            Date date = new Date();
            Integer curTimeAddToDB =(int) (date.getTime()/DIVISOR_ML_SEC);

            //  create data object to insert in DB
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBHelper.DATE_TERMO_ROW_NAME, curTimeAddToDB);
            contentValues.put(DBHelper.TEMPERATURE_TERMO_ROW_NAME, batteryTemper);

            //	insert to DB in separate thread
            AddToDBThread addToDBThread = new AddToDBThread(context, contentValues);
            addToDBThread.start();

            flagAddToDB = false;
        }
    }

    public static Boolean isTimeAddToDB() {
        //  if graphic is off no not add data to DB
        Date date = new Date();
        Integer curTimeAddToDB =(int) (date.getTime()/DIVISOR_ML_SEC);
        Integer secondsFromLastAddToDB = curTimeAddToDB - lastTimeAddToDB;

        if (secondsFromLastAddToDB >= MIN_PERIOD_ADD_TO_DB ) {
            lastTimeAddToDB = curTimeAddToDB;
            flagAddToDB = true;
        }
        return flagAddToDB;
    }

    //	insert to DB in separate thread
    class AddToDBThread extends Thread {
        private Context m_context;
        private ContentValues m_contentValues;
        AddToDBThread(Context context, ContentValues contentValues){
            m_context = context;
            m_contentValues = contentValues;
        }
        public void run(){
            //  connect to DB
            DBHelper dbHelper = new DBHelper(m_context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // insert row to DB and receive it ID
            long rowID = db.insert(DBHelper.TERMO_TABLE_NAME, null, m_contentValues);
            Log.d(LOG_TAG, "Add To DB " + m_contentValues.toString() );

            //  close connection to DB
            dbHelper.close();
        }
    }
}
