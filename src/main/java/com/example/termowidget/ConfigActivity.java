package com.example.termowidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;

import static com.example.termowidget.GraphicTask.timeToString;

//
//  Plan to add:
//  in ConfigActivity
//      graphicCheckBox after DeleteFromDBThread error
//      change 0 color

public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";
    private static final int DIALOG_DELETE_FROM_DB = 1;

    private static Integer graphicPeriod = 3600;

    private CheckBox statusBarCheckBox;
    private CheckBox blinkingCheckBox;
    private CheckBox graphicCheckBox;
    private ImageView graphicViev;
    private GraphicTask graphicTask;

    private QuickSharedPreferences quickSharedPreferences;
    private Handler handler;

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
        registerForContextMenu(graphicViev);
        createGraphic(quickSharedPreferences.isGraphic());

        handler = new ConfigActivityHandler(this);
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
                ExportFromDBThread exportFromDBThread = new ExportFromDBThread(this);
                exportFromDBThread.start();
                break;
            case R.id.delete_data:
                showDialog(DIALOG_DELETE_FROM_DB);
                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_DELETE_FROM_DB) {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);

            adb.setTitle(R.string.del_data_title);
            adb.setMessage(R.string.del_data_mes);
            adb.setIcon(android.R.drawable.ic_dialog_info);
            adb.setPositiveButton(R.string.yes, myClickListener);
            adb.setNegativeButton(R.string.no, myClickListener);

            return adb.create();
        }
        return super.onCreateDialog(id);
    }

    DialogInterface.OnClickListener myClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    DeleteFromDBThread deleteFromDBThread = new DeleteFromDBThread(getApplicationContext());
                    deleteFromDBThread.start();
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void createGraphic(Boolean is_graphic) {
        graphicTask = (GraphicTask) getLastNonConfigurationInstance();
        if (graphicTask != null) {
            // send current Activity in the GraphicTask
            graphicTask.link(this);
        }
        else if(is_graphic){
            graphicTask = new GraphicTask(this, graphicPeriod);
            graphicTask.execute();
        }
        else{

        }
        if(is_graphic){
            graphicViev.setVisibility (View.VISIBLE);
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

        //  restart AlarmManager of WidgetUpdaterService with new type
        TermoWidget.stopAlarmManager(TermoWidget.pIntentWidgetUpdaterService);
        TermoWidget.setAlarmManager(this);
    }

    public void onGraphicClick(View view){
    }

    public void setGraphicBitmap(Bitmap bitmap){
        graphicViev.setImageBitmap(bitmap);
    }

    class ExportFromDBThread extends Thread {
        private static final String DIR_SD = "TermoExport";
        private Context m_context;
        private File sdFile;
        ExportFromDBThread(Context context){
            m_context = context;

            // get path to SD
            File sdPath = Environment.getExternalStorageDirectory();
            // add dir to the path
            sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
            //  check dir for exist
            if(sdPath.exists() == false){
                //  make dir
                if(sdPath.mkdirs()){
                    Log.d(LOG_TAG, "make dir " + sdPath.getAbsolutePath());
                }
                else{
                    Log.d(LOG_TAG, "Error make dir " + sdPath.getAbsolutePath());
                }
            }
            else {
                Log.d(LOG_TAG,sdPath.getAbsolutePath() + " already exist");
            }

            // get current time
            Date curDate = new Date();
            Integer curTime =(int) (curDate.getTime()/TermoBroadCastReceiver.DIVISOR_ML_SEC);

            //  convert number of seconds to time string
            String curTimeString = timeToString(curTime,"yyyy-MM-dd-HH-mm-ss");
            // create File Object, that contains path to file
            String sdFileName = curTimeString + ".txt";
            sdFile = new File(sdPath, sdFileName);
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
            Integer counterExportedStrings=0;
            if (cursor.moveToFirst()) {
                do{
                    Integer id = cursor.getInt(idColIndex);
                    Integer time = cursor.getInt(dateColIndex);
                    String timeString = timeToString(time,"yyyy-MM-dd HH:mm:ss");
                    Integer temperature = cursor.getInt(temperatureColIndex);
                    String exportString = DBHelper.ID_TERMO_ROW_NAME + " : " + id + " "
                                        + DBHelper.DATE_TERMO_ROW_NAME + " : " + timeString + " "
                                        + DBHelper.TEMPERATURE_TERMO_ROW_NAME + " : " + temperature;
                    if(writeFileSD(exportString)) counterExportedStrings++;
                    Log.d(LOG_TAG, exportString);
                }
                while (cursor.moveToNext());
            }

            cursor.close();

            //  close connection to DB
            dbHelper.close();

            //  send message to createToast
            String msgString =  counterExportedStrings + " " + m_context.getString(R.string.file_export) + " " +  sdFile.getAbsolutePath();
            Message message = handler.obtainMessage();
            message.obj = msgString;
            handler.sendMessage(message);

            Log.d(LOG_TAG, msgString);
        }

        Boolean writeFileSD(String outputStr) {
            // check access to SD
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.d(LOG_TAG, "SD-card access denied: " + Environment.getExternalStorageState());
                return false;
            }
            Boolean is_append = true;
            try {
                // create stream for write
                BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, is_append));
                // write outputStr
                bw.write(outputStr);
                // write \n
                bw.newLine();
                //  flush the stream
                bw.flush();
                // close stream
                bw.close();
                Log.d(LOG_TAG, "File have been written to SD:  " + sdFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    private void createToast(String toastString){
        Toast toast = Toast.makeText(getApplicationContext(), toastString, Toast.LENGTH_LONG);
        toast.show();
    }

    static class ConfigActivityHandler extends Handler {
        WeakReference<ConfigActivity> wrActivity;
        public ConfigActivityHandler(ConfigActivity configActivity) {
            wrActivity = new WeakReference<ConfigActivity>(configActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ConfigActivity activity = wrActivity.get();
            if (activity != null){
                String message = (String) msg.obj;
                activity.createToast(message);
            }
        }
    }

    private class DeleteFromDBThread extends Thread {

        private Context m_context;

        DeleteFromDBThread(Context context) {
            m_context = context;
        }
        public void run(){
            //  connect to DB
            DBHelper dbHelper = new DBHelper(m_context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // delete all data from TERMO TABLE
            Integer numberOfDeletedRows  = db.delete(DBHelper.TERMO_TABLE_NAME, null, null);

            //  close connection to DB
            dbHelper.close();

            //  send message to createToast
            String msgString = numberOfDeletedRows + " " +  m_context.getString(R.string.data_deleted);
            Message message = handler.obtainMessage();
            message.obj = msgString;
            handler.sendMessage(message);

            Log.d(LOG_TAG, msgString);
        }
    }
}

