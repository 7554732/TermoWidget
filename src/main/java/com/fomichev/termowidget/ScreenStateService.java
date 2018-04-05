package com.fomichev.termowidget;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class ScreenStateService extends Service {

    private ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerScreenStateReceiver();

        if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "ScreenStateService Started");

        return super.onStartCommand(intent, flags, startId);
    }

    private void registerScreenStateReceiver() {
        unregisterScreenStateReceiver();
        //  register receiver to catch ACTION_SCREEN_ON
        registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG , "screenStateReceiver  registered");
    }

    private void unregisterScreenStateReceiver(){
        try {
            unregisterReceiver(screenStateReceiver);
            if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "screenStateReceiver unregistered");
        }
        catch (Exception e){
            if (TermoWidget.isDebug) Log.w(TermoWidget.LOG_TAG , "screenStateReceiver is not registered");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterScreenStateReceiver();
        if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "ScreenStateService Destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (TermoWidget.isDebug) Log.d(TermoWidget.LOG_TAG , "ScreenStateService onBind");
        return null;
    }
}


