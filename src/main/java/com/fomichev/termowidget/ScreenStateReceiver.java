package com.fomichev.termowidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.fomichev.termowidget.WidgetUpdaterService.wakeLock;

public class ScreenStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //  run widget update if user initiate screen on
        if ((wakeLock != null) && (!wakeLock.isHeld())){
            context.startService(new Intent(context,WidgetUpdaterService.class));
        }
    }
}