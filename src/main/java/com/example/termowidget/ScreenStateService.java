package com.example.termowidget;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class ScreenStateService extends Service {

    final static private String LOG_TAG = "ScreenStateService";
    static private ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();

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

        Log.d(LOG_TAG, "ScreenStateService Started");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(screenStateReceiver);
            Log.d(LOG_TAG, "screenStateReceiver unregistered");
        }
        catch (Exception e){
                Log.d(LOG_TAG, "screenStateReceiver is not registered");
        }
        Log.d(LOG_TAG, "ScreenStateService Destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "ScreenStateService onBind");
        return null;
    }
}


