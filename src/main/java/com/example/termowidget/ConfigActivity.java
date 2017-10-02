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


public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";
    final static public String  PREFERENCES_FILE_NAME  = "config";
    public final static String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";

    private CheckBox statusBarCheckBox;
    public static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        GriphicBitmap griphicBitmap = new GriphicBitmap();
        ImageView iv = (ImageView) findViewById(R.id.termo_graphic);
        iv.setImageBitmap(griphicBitmap.create(this, 10));

        sharedPreferences = getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);

        Boolean statusBar = false;
        //  if STATUS_BAR_PREFERENCES_KEY exist in config
        //  load from config statusBar value
        try {
            statusBar = loadPreferences(sharedPreferences, STATUS_BAR_PREFERENCES_KEY, false);
        }
        catch (IOException e){
            Log.d(LOG_TAG, e.toString());
        }

        //  find statusBar CheckBox
        statusBarCheckBox = (CheckBox) findViewById(R.id.status_bar_info);
        //  set CheckBox value
        statusBarCheckBox.setChecked(statusBar);

    }

    public void onStatusBarChBoxClick(View view){
        Boolean statusBar = statusBarCheckBox.isChecked();
        savePreferences(sharedPreferences,STATUS_BAR_PREFERENCES_KEY,statusBar);
        //  run widget update
        Context context = getApplicationContext();
        context.startService(new Intent(context,WidgetUpdaterService.class));
    }

    public void onGraphicClick(View view){
        ReadFromDBThread readFromDBThread = new ReadFromDBThread(this);
        readFromDBThread.start();
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

    private Bitmap createGriphicBitmap(Integer interval){
        final Integer MAX_DATA_WIDTH = 290;
        final Integer MAX_DATA_HEIGHT = 210;

        // check data
        if ( interval <= 0) return null;

        // open bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.graphic);

        // create canvas
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setTextSize((float) (canvas.getHeight()*0.07));
        paint.setColor(Color.BLACK);
        canvas.drawText("w "+canvas.getWidth()+" h "+canvas.getHeight(), (float)(canvas.getWidth() * 0.07), (float)(canvas.getHeight() * 0.99), paint);

        // get current time and calculate time of graphic begin
        // get amount of  data from interval in DB
        // calculate number of data per pixel or number pixel per one data count
        // get data and draw the rectangles
        return bitmap;
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

