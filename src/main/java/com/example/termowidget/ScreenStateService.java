package com.example.termowidget;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;

public class ScreenStateService extends Service {

    private static ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            this.unregisterReceiver(screenStateReceiver);
            if (isDebug) Log.d(LOG_TAG , "screenStateReceiver unregistered");
        }
        catch (Exception e){
            if (isDebug) Log.w(LOG_TAG , "screenStateReceiver is not registered");
        }

        //  register receiver to catch ACTION_SCREEN_ON
        this.registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        if (isDebug) Log.w(LOG_TAG , "screenStateReceiver  registered");

        if (isDebug) Log.d(LOG_TAG , "ScreenStateService Started");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.unregisterReceiver(screenStateReceiver);
            if (isDebug) Log.d(LOG_TAG , "screenStateReceiver unregistered");
        }
        catch (Exception e){
                if (isDebug) Log.w(LOG_TAG , "screenStateReceiver is not registered");
        }
        if (isDebug) Log.d(LOG_TAG , "ScreenStateService Destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (isDebug) Log.d(LOG_TAG , "ScreenStateService onBind");
        return null;
    }
}


