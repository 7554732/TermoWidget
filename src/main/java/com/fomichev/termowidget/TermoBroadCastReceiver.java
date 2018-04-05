package com.fomichev.termowidget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.termowidget.R;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.NOTIFICATION_SERVICE;

public class TermoBroadCastReceiver extends BroadcastReceiver {

    public static final int DIVISOR_ML_SEC = 1000;  //  multiplier to convert between seconds and milliseconds
    private static final int MIN_PERIOD_ADD_TO_DB = 300; // (seconds) minimum period between adding temperature to DB

    private static final int BLINK_DELAY_FIRST_TIME = 500;    //  (milliseconds) before first blink
    private static final int BLINK_DELAY_TIME = 500;    //  (milliseconds) between blinks

    private static int lastTimeAddToDB = 0; // (seconds)
    private static boolean flagReady = true;
    private static boolean flagAddToDB = false;

    private QuickSharedPreferences quickSharedPreferences;

    public static boolean isReady(){
        return flagReady;
    }

    public static void setReady(boolean isReady){
        flagReady = isReady;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(context);
        //  get calibration temperature that means difference between environment and battery temperature
        int calibrationTemperature = quickSharedPreferences.getCalibrationTemperature();

        //  get battery temperature from intent extra
        int batteryTemperature = (int)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;
        //  calculate environment temperature
        int temperature = batteryTemperature + calibrationTemperature;

        //  set temperature to widget
        setTemperature(context,temperature);

        addTemperatureToDB(context,temperature);
        setReady(true);
    }

    private void setTemperature(Context context, int temperature){
        //  get RemoteViews by package name
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        //  set temperature string
        widgetView.setTextViewText(R.id.tvTemperature,Integer.toString(temperature)+context.getString(R.string.degree));
        //  set color for widget text
        widgetView.setTextColor(R.id.tvTemperature,context.getResources().getColor(TermoColor.getColor(temperature)));

        if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "temperature "+temperature);

        //  widget blinking
        scheduleBlinker(context);

        //  update widget
        updateWidget(context,widgetView);

        //  set temperature to status bar
        setIconToStatusBar(context,temperature);
    }

    private void scheduleBlinker(Context context){
        Blinker blinker = new Blinker(context);
        Timer timer = new Timer();
        //  get blinking status
        //  schedule timer if blinking is on
        if(quickSharedPreferences.isBlinking()){
            timer.schedule(blinker, BLINK_DELAY_FIRST_TIME, BLINK_DELAY_TIME);
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

        // find matched color for temperature
        static int getColor(int temperature) {
            for (TermoColor termoColor : TermoColor.values()){
                if (temperature > termoColor.temperature()) {
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
        if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "updateWidget "+componentName);
    }

    private class Blinker extends TimerTask {

        private Context context;

        private int counterExecution = 0;
        private final int SECOND_EXECUTION = 2;

        public void run(){
            //  get RemoteViews by package name
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

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
            updateWidget(context,widgetView);
        }

        public Blinker(Context arg_context){
            context=arg_context;
        }
    }

    private void setIconToStatusBar(Context context, int temperature){

        int NOTIFICATION_ID = 1;
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if(quickSharedPreferences.isBlinking()){
            mNotifyMgr.cancel(NOTIFICATION_ID);
        }

        if (quickSharedPreferences.isStatusBar()){

            NotificationCompat.Builder status_bar_Builder = new NotificationCompat.Builder(context)
                    .setContentTitle(temperature + " °");

            // find matched icon for temperature
            if (temperature < TermoIcon.M31.temperature()){
                //  set icon for status bar
                status_bar_Builder.setSmallIcon(TermoIcon.M31.icon());
            }
            else if(temperature > TermoIcon.P51.temperature()){
                //  set icon for status bar
                status_bar_Builder.setSmallIcon(TermoIcon.P51.icon());
            }
            else{
                status_bar_Builder.setSmallIcon(TermoIcon.getIcon(temperature));
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
        M31 (-31, R.drawable.m31, R.drawable.m31_l), M30 (-30, R.drawable.m30, R.drawable.m30_l),
        M29 (-29, R.drawable.m29, R.drawable.m29_l), M28 (-28, R.drawable.m28, R.drawable.m28_l), M27 (-27, R.drawable.m27, R.drawable.m27_l), M26 (-26, R.drawable.m26, R.drawable.m26_l), M25 (-25, R.drawable.m25, R.drawable.m25_l),
        M24 (-24, R.drawable.m24, R.drawable.m24_l), M23 (-23, R.drawable.m23, R.drawable.m23_l), M22 (-22, R.drawable.m22, R.drawable.m22_l), M21 (-21, R.drawable.m21, R.drawable.m21_l), M20 (-20, R.drawable.m20, R.drawable.m20_l),
        M19 (-19, R.drawable.m19, R.drawable.m19_l), M18 (-18, R.drawable.m18, R.drawable.m18_l), M17 (-17, R.drawable.m17, R.drawable.m17_l), M16 (-16, R.drawable.m16, R.drawable.m16_l), M15 (-15, R.drawable.m15, R.drawable.m15_l),
        M14 (-14, R.drawable.m14, R.drawable.m14_l), M13 (-13, R.drawable.m13, R.drawable.m13_l), M12 (-12, R.drawable.m12, R.drawable.m12_l), M11 (-11, R.drawable.m11, R.drawable.m11_l), M10 (-10, R.drawable.m10, R.drawable.m10_l),
        M09 (-9, R.drawable.m09, R.drawable.m09_l), M08 (-8, R.drawable.m08,R.drawable.m08_l), M07 (-7, R.drawable.m07, R.drawable.m07_l), M06 (-6, R.drawable.m06, R.drawable.m06_l), M05 (-5, R.drawable.m05, R.drawable.m05_l),
        M04 (-4, R.drawable.m04, R.drawable.m04_l), M03 (-3, R.drawable.m03, R.drawable.m03_l), M02 (-2, R.drawable.m02, R.drawable.m02_l), M01 (-1, R.drawable.m01, R.drawable.m01_l),
        P00 (0, R.drawable.p00, R.drawable.p00_l),
        P01 (1, R.drawable.p01, R.drawable.p01_l), P02 (2, R.drawable.p02, R.drawable.p02_l), P03 (3, R.drawable.p03, R.drawable.p03_l), P04 (4, R.drawable.p04, R.drawable.p04_l), P05 (5, R.drawable.p05, R.drawable.p05_l),
        P06 (6, R.drawable.p06, R.drawable.p06_l), P07 (7, R.drawable.p07, R.drawable.p07_l), P08 (8, R.drawable.p08, R.drawable.p08_l), P09 (9, R.drawable.p09, R.drawable.p09_l), P10 (10, R.drawable.p10, R.drawable.p10_l),
        P11 (11, R.drawable.p11, R.drawable.p11_l), P12 (12, R.drawable.p12, R.drawable.p12_l), P13 (13, R.drawable.p13, R.drawable.p13_l), P14 (14, R.drawable.p14, R.drawable.p14_l), P15 (15, R.drawable.p15, R.drawable.p15_l),
        P16 (16, R.drawable.p16, R.drawable.p16_l), P17 (17, R.drawable.p17, R.drawable.p17_l), P18 (18, R.drawable.p18, R.drawable.p18_l), P19 (19, R.drawable.p19, R.drawable.p19_l), P20 (20, R.drawable.p20, R.drawable.p20_l),
        P21 (21, R.drawable.p21, R.drawable.p21_l), P22 (22, R.drawable.p22, R.drawable.p22_l), P23 (23, R.drawable.p23, R.drawable.p23_l), P24 (24, R.drawable.p24, R.drawable.p24_l), P25 (25, R.drawable.p25, R.drawable.p25_l),
        P26 (26, R.drawable.p26, R.drawable.p26_l), P27 (27, R.drawable.p27, R.drawable.p27_l), P28 (28, R.drawable.p28, R.drawable.p28_l), P29 (29, R.drawable.p29, R.drawable.p29_l), P30 (30, R.drawable.p30, R.drawable.p30_l),
        P31 (31, R.drawable.p31, R.drawable.p31_l), P32 (32, R.drawable.p32, R.drawable.p32_l), P33 (33, R.drawable.p33, R.drawable.p33_l), P34 (34, R.drawable.p34, R.drawable.p34_l), P35 (35, R.drawable.p35, R.drawable.p35_l),
        P36 (36, R.drawable.p36, R.drawable.p36_l), P37 (37, R.drawable.p37, R.drawable.p37_l), P38 (38, R.drawable.p38, R.drawable.p38_l), P39 (39, R.drawable.p39, R.drawable.p39_l), P40 (40, R.drawable.p40, R.drawable.p40_l),
        P41 (41, R.drawable.p41, R.drawable.p41_l), P42 (42, R.drawable.p42, R.drawable.p42_l), P43 (43, R.drawable.p43, R.drawable.p43_l), P44 (44, R.drawable.p44, R.drawable.p44_l), P45 (45, R.drawable.p45, R.drawable.p45_l),
        P46 (46, R.drawable.p46, R.drawable.p46_l), P47 (47, R.drawable.p47, R.drawable.p47_l), P48 (48, R.drawable.p48, R.drawable.p48_l), P49 (49, R.drawable.p49, R.drawable.p49_l), P50 (50, R.drawable.p50, R.drawable.p50_l),
        P51 (51, R.drawable.p51, R.drawable.p51_l);

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
        static int getIcon(int temperature) {
            for (TermoIcon termoIcon : TermoIcon.values()){
                if (temperature == termoIcon.temperature()) {
                    return termoIcon.icon();
                }
            }
            return R.mipmap.ic_launcher;
        }
    }

    private void addTemperatureToDB(Context context, int temperature) {
        //  if graphic is off no not add data to DB
        if(quickSharedPreferences.isGraphic() == false){
            flagAddToDB = false;
            return;
        }
        if (flagAddToDB){

            Date date = new Date();
            int curTimeAddToDB =(int) (date.getTime()/DIVISOR_ML_SEC);

            //  create data object to insert in DB
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBHelper.DATE_TERMO_ROW_NAME, curTimeAddToDB);
            contentValues.put(DBHelper.TEMPERATURE_TERMO_ROW_NAME, temperature);

            //	insert to DB in separate thread
            AddToDBThread addToDBThread = new AddToDBThread(context, contentValues);
            addToDBThread.start();

            flagAddToDB = false;
        }
    }

    //  check time for adding data to DB
    public static boolean isTimeAddToDB() {
        Date date = new Date();
        int curTimeAddToDB =(int) (date.getTime()/DIVISOR_ML_SEC);
        int secondsFromLastAddToDB = curTimeAddToDB - lastTimeAddToDB;

        if (secondsFromLastAddToDB >= MIN_PERIOD_ADD_TO_DB ) {
            lastTimeAddToDB = curTimeAddToDB;
            flagAddToDB = true;
        }
        return flagAddToDB;
    }

    //	insert to DB in separate thread
    class AddToDBThread extends Thread {
        private Context context;
        private ContentValues contentValues;
        AddToDBThread(Context arg_context, ContentValues arg_contentValues){
            context = arg_context;
            contentValues = arg_contentValues;
        }
        public void run(){
            //  connect to DB
            DBHelper dbHelper = new DBHelper(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // insert row to DB and receive it ID
            long rowID = db.insert(DBHelper.TERMO_TABLE_NAME, null, contentValues);
            if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "Add To DB " + contentValues.toString() );

            //  close connection to DB
            dbHelper.close();
        }
    }
}
