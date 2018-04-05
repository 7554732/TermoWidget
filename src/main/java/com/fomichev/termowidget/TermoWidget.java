package com.fomichev.termowidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.termowidget.R;

//  Widget for Android displays the temperature of the battery


public class TermoWidget extends AppWidgetProvider {

    public static final boolean isDebug = true;
    public static final String LOG_TAG = "TermoWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        //  set PendingIntent to start ConfigActivity
        setOnClickPendingIntent(context);

        //  start  WidgetUpdaterService
        context.startService(new Intent(context,WidgetUpdaterService.class));

        //  start ScreenStateService  to catch ACTION_SCREEN_ON
        context.startService(new Intent(context, ScreenStateService.class));

        if (isDebug) Log.d(LOG_TAG , "TermoWidget Updated");

    }


    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        //  stop ScreenStateService
        context.stopService(new Intent(context, ScreenStateService.class));

        //  stop WidgetUpdaterService
        context.stopService(new Intent(context,WidgetUpdaterService.class));

        if (isDebug) Log.d(LOG_TAG, "TermoWidget Disabled");
    }

    private void  setOnClickPendingIntent(Context context){
        //  create PendingIntent from Intent to start ConfigActivity
        Intent configIntent = new Intent(context, ConfigActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //  get RemoteViews by package name
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

        widgetView.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        //  get widget menager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //  update widget
        ComponentName componentName = new ComponentName(context,TermoWidget.class);
        appWidgetManager.updateAppWidget(componentName, widgetView);

        if (isDebug) Log.d(LOG_TAG , "PendingIntent set");
    }

}