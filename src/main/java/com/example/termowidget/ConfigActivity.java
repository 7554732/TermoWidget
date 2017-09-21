package com.example.termowidget;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.IOException;


public class ConfigActivity extends Activity {

    final static String LOG_TAG = "ConfigActivity";
    final static private String  PREFERENCES_FILE_NAME  = "config";
    final static private String  STATUS_BAR_PREFERENCES_KEY  = "status_bar_info";

    private CheckBox statusBarCheckBox;
    private  SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(convertToImg("ok"));
//        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

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

        Integer NOTIFICATION_ID = 1;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (statusBar){
            Drawable drawable = new BitmapDrawable(getResources(), convertToImg("ok"));
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setSmallIcon(R.drawable.d_0)
//                    .setLargeIcon( convertToImg("ok"))
                    .setContentTitle("Title");
            Intent resultIntent = new Intent(this, ConfigActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

            mNotifyMgr.notify(NOTIFICATION_ID, notification);
        }
        else {
            mNotifyMgr.cancel(NOTIFICATION_ID);
        }
    }

    private Bitmap convertToImg(String text)
    {
        Bitmap btmText = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_4444);
        Canvas cnvText = new Canvas(btmText);

//        Typeface tf = Typeface.createFromAsset(context.getAssets(),"fonts/Benegraphic.ttf");

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
//        paint.setTypeface(tf);
        paint.setColor(Color.WHITE);
        paint.setTextSize(12);

        cnvText.drawText(text, 20, 20, paint);
        return btmText;
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

