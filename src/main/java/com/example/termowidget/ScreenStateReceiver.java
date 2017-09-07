package com.example.termowidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //  run widget update
        context.startService(new Intent(context,WidgetUpdaterService.class));
    }
}