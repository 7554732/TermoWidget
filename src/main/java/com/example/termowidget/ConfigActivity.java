package com.example.termowidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.IOException;

//
//  Plan to add:
//  in ConfigActivity
//      ClearDBThread instead ReadFromDBThread
//      change 0 color

public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";

    private static Integer graphicPeriod = 3600;

    private CheckBox statusBarCheckBox;
    private CheckBox blinkingCheckBox;
    private CheckBox graphicCheckBox;
    private ImageView graphicViev;
    private GraphicTask graphicTask;

    private QuickSharedPreferences quickSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(this);

        //  find statusBar CheckBox
        statusBarCheckBox = (CheckBox) findViewById(R.id.status_bar_info);
        //  set CheckBox value
        statusBarCheckBox.setChecked(quickSharedPreferences.isStatusBar());

        //  find blinking CheckBox
        blinkingCheckBox = (CheckBox) findViewById(R.id.is_blinking);
        //  set CheckBox value
        blinkingCheckBox.setChecked(quickSharedPreferences.isBlinking());

        //  find graphic CheckBox
        graphicCheckBox = (CheckBox) findViewById(R.id.is_graphic);
        //  set CheckBox value
        graphicCheckBox.setChecked(quickSharedPreferences.isGraphic());

        graphicViev = (ImageView) findViewById(R.id.termo_graphic);
        createGraphic(quickSharedPreferences.isGraphic());
        registerForContextMenu(graphicViev);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v.getId() == R.id.termo_graphic){
            getMenuInflater().inflate(R.menu.graphic_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_data:
                break;
            case R.id.delete_data:
                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void createGraphic(Boolean is_graphic) {
        if(is_graphic){
            graphicViev.setVisibility (View.VISIBLE);
            graphicTask = (GraphicTask) getLastNonConfigurationInstance();
            if (graphicTask == null) {
                graphicTask = new GraphicTask(this, graphicPeriod);
                graphicTask.execute();
            }
            else{
                // send current Activity in the GraphicTask
                graphicTask.link(this);
            }
        }
        else{
            graphicViev.setVisibility (View.INVISIBLE);
        }
    }

    public Object onRetainNonConfigurationInstance() {
        // remove from GraphicTask link to prior Activity
        graphicTask.unLink();
        return graphicTask;
    }

    public void onStatusBarChBoxClick(View view){
        Boolean is_status_bar = statusBarCheckBox.isChecked();
        quickSharedPreferences.saveBoolean(quickSharedPreferences.STATUS_BAR_PREFERENCES_KEY,is_status_bar);
        //  run widget update
        Context context = getApplicationContext();
        context.startService(new Intent(context,WidgetUpdaterService.class));
    }

    public void onBlinkingChBoxClick(View view){
        Boolean is_blinking = blinkingCheckBox.isChecked();
        quickSharedPreferences.saveBoolean(quickSharedPreferences.BLINKING_PREFERENCES_KEY,is_blinking);
    }

    public void onGraphicChBoxClick(View view){
        Boolean is_graphic = graphicCheckBox.isChecked();
        quickSharedPreferences.saveBoolean(quickSharedPreferences.GRAPHIC_PREFERENCES_KEY,is_graphic);
        createGraphic(is_graphic);

        TermoWidget.stopAlarmManager(TermoWidget.pIntentWidgetUpdaterService);
        TermoWidget.setAlarmManager(this);
    }

    public void onGraphicClick(View view){
        ReadFromDBThread readFromDBThread = new ReadFromDBThread(this);
        readFromDBThread.start();
    }

    public void setGraphicBitmap(Bitmap bitmap){
        graphicViev.setImageBitmap(bitmap);
    }

    class ReadFromDBThread extends Thread {
        private Context m_context;
        ReadFromDBThread(Context context){
            m_context = context;
        }
        public void run(){
            //  connect to DB
            DBHelper dbHelper = new DBHelper(m_context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // query all data from TERMO TABLE
            Cursor cursor = db.query(DBHelper.TERMO_TABLE_NAME, null, null, null, null, null, null);
            Integer idColIndex = cursor.getColumnIndex(DBHelper.ID_TERMO_ROW_NAME);
            Integer dateColIndex = cursor.getColumnIndex(DBHelper.DATE_TERMO_ROW_NAME);
            Integer temperatureColIndex = cursor.getColumnIndex(DBHelper.TEMPERATURE_TERMO_ROW_NAME);

            // set cursor position on the first line
            // if no one line return false
            if (cursor.moveToFirst()) {
                do{
                    Log.d(LOG_TAG,    DBHelper.ID_TERMO_ROW_NAME + " : " + cursor.getInt(idColIndex) + " "
                                    + DBHelper.DATE_TERMO_ROW_NAME + " : " + cursor.getInt(dateColIndex) + " "
                                    + DBHelper.TEMPERATURE_TERMO_ROW_NAME + " : " + cursor.getInt(temperatureColIndex) );
                }
                while (cursor.moveToNext());
            }

            cursor.close();

            //  close connection to DB
            dbHelper.close();
        }
    }


}

