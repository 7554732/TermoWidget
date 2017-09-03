package com.example.termowidget;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


public class WidgetUpdaterService extends IntentService{

    final static String LOG_TAG = "WidgetUpdaterService";
    static private TermoBroadCastReceiver termoBroadCastReceiver = new TermoBroadCastReceiver() ;

    public WidgetUpdaterService(){
        super("WidgetUpdaterService");
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
