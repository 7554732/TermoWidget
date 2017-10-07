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
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.IOException;

//
//  Plan to add:
//  in ConfigActivity
//      change 0 color
//      remove ReadFromDBThread

public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";
    final static public String  PREFERENCES_FILE_NAME  = "config";
    public static final String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";
    public static final String BLINKING_PREFERENCES_KEY = "is_blinking";
    public static final String GRAPHIC_PREFERENCES_KEY = "is_graphic";
    private static Integer graphicPeriod = 3600;

    private CheckBox statusBarCheckBox;
    private CheckBox blinkingCheckBox;
    private CheckBox graphicCheckBox;
    public static SharedPreferences sharedPreferences;
    private ImageView graphicViev;
    private GraphicTask graphicTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        sharedPreferences = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);

        Boolean statusBar = false;
        Boolean is_blinking = true;
        Boolean is_graphic = true;
        //  if STATUS_BAR_PREFERENCES_KEY exist in config
        //  load from config statusBar, is_blinking and is_graphic values
        try {
            statusBar = loadPreferences(sharedPreferences, STATUS_BAR_PREFERENCES_KEY, false);
            is_blinking = loadPreferences(sharedPreferences, BLINKING_PREFERENCES_KEY, true);
            is_graphic = loadPreferences(sharedPreferences, GRAPHIC_PREFERENCES_KEY, true);
        }
        catch (IOException e){
            Log.d(LOG_TAG, e.toString());
        }

        //  find statusBar CheckBox
        statusBarCheckBox = (CheckBox) findViewById(R.id.status_bar_info);
        //  set CheckBox value
        statusBarCheckBox.setChecked(statusBar);

        //  find blinking CheckBox
        blinkingCheckBox = (CheckBox) findViewById(R.id.is_blinking);
        //  set CheckBox value
        blinkingCheckBox.setChecked(is_blinking);

        //  find graphic CheckBox
        graphicCheckBox = (CheckBox) findViewById(R.id.is_graphic);
        //  set CheckBox value
        graphicCheckBox.setChecked(is_graphic);

        graphicViev = (ImageView) findViewById(R.id.termo_graphic);
        createGraphic(is_graphic);
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
        Boolean statusBar = statusBarCheckBox.isChecked();
        savePreferences(sharedPreferences,STATUS_BAR_PREFERENCES_KEY,statusBar);
        //  run widget update
        Context context = getApplicationContext();
        context.startService(new Intent(context,WidgetUpdaterService.class));
    }

    public void onBlinkingChBoxClick(View view){
        Boolean is_blinking = blinkingCheckBox.isChecked();
        savePreferences(sharedPreferences,BLINKING_PREFERENCES_KEY,is_blinking);
    }

    public void onGraphicChBoxClick(View view){
        Boolean is_graphic = graphicCheckBox.isChecked();
        savePreferences(sharedPreferences,GRAPHIC_PREFERENCES_KEY,is_graphic);
        createGraphic(is_graphic);
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

    static public Boolean loadPreferences (SharedPreferences sharedPreferences, String key, Boolean defaultValue) throws IOException{

        //  check preferences key for exist and get it value
        if (sharedPreferences.contains(key)) {
            return  sharedPreferences.getBoolean(key, defaultValue);
        }
        else{
            throw new IOException("Preferences Key does not exsist");
        }
    }

    static public void savePreferences(SharedPreferences sharedPreferences, String key, Boolean value){
        SharedPreferences.Editor preferencesEditor = sharedPreferences.edit();
        preferencesEditor.putBoolean(key, value);
        preferencesEditor.apply();
    }


}

