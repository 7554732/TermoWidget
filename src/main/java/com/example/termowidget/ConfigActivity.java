package com.example.termowidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;

import static android.R.attr.data;
import static com.example.termowidget.GraphicTask.timeToString;
import static com.example.termowidget.TermoBroadCastReceiver.DIVISOR_ML_SEC;
import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;


public class ConfigActivity extends FragmentActivity implements DelDataDialogFragment.DelDataDialogListener{

    private static final int MAX_CALIBRATE_VALUE = 10;

    private static Integer graphicPeriod = 3600;

    private CheckBox statusBarCheckBox;
    private CheckBox blinkingCheckBox;
    private CheckBox graphicCheckBox;
    private ImageView graphicView;
    private GraphicTask graphicTask;

    private static QuickSharedPreferences quickSharedPreferences;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        //  initialize SharedPreferences
        quickSharedPreferences = new QuickSharedPreferences(this);


        // set data for spinner
        final Integer[] calibration_data = new Integer[2*MAX_CALIBRATE_VALUE + 1];
        for(int counter = 0; counter < 2*MAX_CALIBRATE_VALUE + 1; counter++){
            calibration_data[counter] = counter - MAX_CALIBRATE_VALUE;
        }

        Integer calibrationTemperature = quickSharedPreferences.getCalibrationTemperature();

        IntSpinnerWraper calibrationSpinner = new IntSpinnerWraper( this,calibration_data, calibrationTemperature,
                quickSharedPreferences.CALIBRATE_PREFERENCES_KEY,
                R.id.calibrate_spinner, R.string.calibrate_tv);


        // set data for spinner
        final Integer[] update_time_data = {5, 10, 30, 60};

        Integer update_time = DIVISOR_ML_SEC * quickSharedPreferences.getUpdateTime();

        IntSpinnerWraper updateSpinner = new IntSpinnerWraper( this,update_time_data, update_time,
                quickSharedPreferences.UPDATE_TIME_PREFERENCES_KEY,
                R.id.update_time_spinner, R.string.update_time_tv);

        //  find statusBar CheckBox
        statusBarCheckBox = (CheckBox) findViewById(R.id.status_bar_info);
        //  set CheckBox value
        statusBarCheckBox.setChecked(quickSharedPreferences.isStatusBar());
        //  set OnClickListener
        statusBarCheckBox.setOnClickListener(chBoxListener);

        //  find blinking CheckBox
        blinkingCheckBox = (CheckBox) findViewById(R.id.is_blinking);
        //  set CheckBox value
        blinkingCheckBox.setChecked(quickSharedPreferences.isBlinking());
        //  set OnClickListener
        blinkingCheckBox.setOnClickListener(chBoxListener);

        //  find graphic CheckBox
        graphicCheckBox = (CheckBox) findViewById(R.id.is_graphic);
        //  set CheckBox value
        graphicCheckBox.setChecked(quickSharedPreferences.isGraphic());
        //  set OnClickListener
        graphicCheckBox.setOnClickListener(chBoxListener);

        graphicView = (ImageView) findViewById(R.id.termo_graphic);
        registerForContextMenu(graphicView);
        createGraphic();

        handler = new ConfigActivityHandler(this);


    }

    class IntSpinnerWraper{

        private ArrayAdapter<Integer> adapter;
        public Spinner spinner;
        private Integer position;
        final Integer[] m_data_array;
        final String m_preferences_key;

        public IntSpinnerWraper(Context context, final Integer[] data_array, int current_value, final String preferences_key, int viewId, int stringRes) {
            m_data_array = data_array;
            m_preferences_key = preferences_key;

            initAdapter(context);
            initSpinner(current_value, viewId, stringRes);

        }

        private void initAdapter(Context context){
            adapter = new ArrayAdapter<Integer>(context, android.R.layout.simple_spinner_item, m_data_array);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        private void initSpinner(int current_value, int viewId, int stringRes) {

            //  find Spinner
            spinner = (Spinner) findViewById(viewId);
            spinner.setAdapter(adapter);

            // title
            spinner.setPrompt(getString(stringRes));

            // select spinner position
            position = adapter.getPosition(current_value);
            spinner.setSelection(position);

            setOnItemSelectedListener();
        }

        private void setOnItemSelectedListener() {
            // set Listener
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Integer value = m_data_array[position];
                    quickSharedPreferences.saveInteger(m_preferences_key, value);
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        }
    }

    private View.OnClickListener chBoxListener = new View.OnClickListener()
    {

        public void onClick(View view)
        {
            switch (view.getId()){
                case R.id.status_bar_info:
                    onStatusBarChBoxClick(view);
                    break;
                case R.id.is_blinking:
                    onBlinkingChBoxClick(view);
                    break;
                case R.id.is_graphic:
                    onGraphicChBoxClick(view);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        //  create context menu for graphic
        if(view.getId() == R.id.termo_graphic){
            getMenuInflater().inflate(R.menu.graphic_context_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.export_data:
                //  export from DB
                ExportFromDBThread exportFromDBThread = new ExportFromDBThread(this);
                exportFromDBThread.start();
                break;
            case R.id.delete_data:
                // clear DB
                DialogFragment dialog = new DelDataDialogFragment();
                dialog.show(getSupportFragmentManager(), "DelDataDialogFragment");

                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDelDataDialogPositiveClick(DialogFragment dialog, int which) {
        deleteFromDB();
    }

    @Override
    public void onDelDataDialogNegativeClick(DialogFragment dialog, int which) {

    }

    private void deleteFromDB() {
        DeleteFromDBThread deleteFromDBThread = new DeleteFromDBThread(getApplicationContext());
        deleteFromDBThread.start();
    }

    @Override
    protected void onDestroy() {
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void createGraphic() {
        Boolean is_graphic = quickSharedPreferences.isGraphic();
        if (isDebug) Log.d(LOG_TAG, "is_graphic " + is_graphic);
        graphicTask = (GraphicTask) getLastNonConfigurationInstance();

        if (graphicTask != null) {
            // send current Activity in the returned GraphicTask
            graphicTask.link(this);
        }
        else if(is_graphic){
            //  if there is no returned GraphicTask and graphic is on
            //  run new GraphicTask
            graphicTask = new GraphicTask(this, graphicPeriod);
            graphicTask.execute();
        }
        else{
        }

        if(is_graphic){
            graphicView.setVisibility (View.VISIBLE);
        }
        else{
            graphicView.setVisibility (View.INVISIBLE);
        }
    }

    //  return graphicTask for current activity if it has not been completed in previous activity
    public Object onRetainCustomNonConfigurationInstance() {
        if(graphicTask.getStatus() == AsyncTask.Status.RUNNING){
            return graphicTask;
        }
        else{
            return null;
        }
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

        if (isDebug) Log.d(LOG_TAG, "is_graphic " + is_graphic);
        createGraphic();
    }

    public void onGraphicClick(View view){
    }

    public void setGraphicBitmap(Bitmap bitmap){
        graphicView.setImageBitmap(bitmap);
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
                    if (isDebug) Log.d(LOG_TAG , "make dir " + sdPath.getAbsolutePath());
                }
                else{
                    if (isDebug) Log.w(LOG_TAG , "Error make dir " + sdPath.getAbsolutePath());
                }
            }
            else {
                if (isDebug) Log.d(LOG_TAG ,sdPath.getAbsolutePath() + " already exist");
            }

            // get current time
            Date curDate = new Date();
            Integer curTime =(int) (curDate.getTime()/ DIVISOR_ML_SEC);

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
                    if (isDebug) Log.d(LOG_TAG , exportString);
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

            if (isDebug) Log.d(LOG_TAG , msgString);
        }

        Boolean writeFileSD(String outputStr) {
            // check access to SD
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (isDebug) Log.w(LOG_TAG , "SD-card access denied: " + Environment.getExternalStorageState());
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
                if (isDebug) Log.d(LOG_TAG , "File have been written to SD:  " + sdFile.getAbsolutePath());
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

    //  recieve message and send it to toast
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

            createGraphic();

            if (isDebug) Log.d(LOG_TAG , msgString);
        }
    }
}

