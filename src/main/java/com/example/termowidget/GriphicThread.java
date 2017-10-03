package com.example.termowidget;


import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GriphicThread extends Thread {

    final static String LOG_TAG = "GriphicBitmap";

    private static final Integer BITMAP_ORIGIN_WIDTH = 320;
    private static final Integer BITMAP_ORIGIN_HEIGHT = 240;
    private static final Integer DATE_ORIGIN_TEXT_SIZE = 16;
    private static final Integer DATE_ORIGIN_X = 20;
    private static final Integer DATE_ORIGIN_Y = 237;

    private Context m_context;
    private Integer m_interval = 0;
    private ConfigActivity m_configActivity;

    public GriphicThread (Context context, Integer interval) {
        m_context = context;
        // check data
        if (interval > 0) m_interval = interval;
    }

    public void link(ConfigActivity configActivity){
        m_configActivity = configActivity;
    }

    public void run(){

        // open bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(m_context.getResources(), R.drawable.graphic);

        // create canvas
        Canvas canvas = new Canvas(bitmap);
        Integer canvasHeight = canvas.getHeight();
        Integer canvasWidth = canvas.getWidth();

        CanvasObject dateText = new CanvasObject(canvas, BITMAP_ORIGIN_WIDTH, BITMAP_ORIGIN_HEIGHT);

        Paint paint = new Paint();
        paint.setTextSize(dateText.getSize(DATE_ORIGIN_TEXT_SIZE));
        paint.setColor(Color.BLACK);

        // get current time and calculate time of graphic begin
        Date curDate = new Date();
        Integer curTime =(int) (curDate.getTime()/TermoBroadCastReceiver.DIVISOR_ML_SEC);
        Integer beginTime = curTime - m_interval;

        Date beginDate = new Date();
        beginDate.setTime(beginTime * TermoBroadCastReceiver.DIVISOR_ML_SEC);
        SimpleDateFormat beginDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String beginDateString = beginDateFormat.format(beginDate);
        canvas.drawText("From " + beginDateString, dateText.getX(DATE_ORIGIN_X), dateText.getY(DATE_ORIGIN_Y), paint);

        // get amount of  data from interval in DB

        //  connect to DB
        DBHelper dbHelper = new DBHelper(m_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // query all data from TERMO TABLE
        String selection = DBHelper.DATE_TERMO_ROW_NAME + ">" + beginTime;
        Cursor cursor = db.query(DBHelper.TERMO_TABLE_NAME, null, selection, null, null, null, null);

        Log.d(LOG_TAG, "Amount of  data from interval in DB: " + cursor.getCount());

        cursor.close();
        //  close connection to DB
        dbHelper.close();

        // calculate number of data per pixel or number pixel per one data count
        // get data and draw the rectangles
        try {
            m_configActivity.setGraphicBitmap(bitmap);
        }
        catch (Exception e){
            Log.d(LOG_TAG, e.toString());
        }
    }

    private class CanvasObject {
        private Float size;
        private Float x;
        private Float y;
        private Integer m_bitmap_origin_width ;
        private Integer m_bitmap_origin_height;
        private Integer canvasHeight;
        private Integer canvasWidth;

        public CanvasObject (Canvas canvas, Integer bitmap_origin_width, Integer bitmap_origin_height){
            canvasHeight = canvas.getHeight();
            canvasWidth = canvas.getWidth();
            m_bitmap_origin_width = bitmap_origin_width;
            m_bitmap_origin_height = bitmap_origin_height;
        }

        private Float getValue(Integer origin_value, Integer bitmap_origin_size, Integer canvasSize){
            if (origin_value > 0 & m_bitmap_origin_height > 0 & canvasHeight > 0 ){
                Float ratio = (float) origin_value / (float) bitmap_origin_size;
                Float result = canvasSize * ratio;
                return result;
            }
            else{
                return null;
            }
        }

        public Float getSize(Integer origin_size){
            return getValue(origin_size, m_bitmap_origin_height, canvasHeight);
        }

        public Float getX(Integer origin_x){
            return getValue(origin_x, m_bitmap_origin_width, canvasWidth);
        }

        public Float getY(Integer origin_y){
            return getValue(origin_y, m_bitmap_origin_height, canvasHeight);
        }
    }
}
