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
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GraphicTask extends AsyncTask<Object, Void, Bitmap> {

    final static String LOG_TAG = "GraphicTask";

    private static final Integer BITMAP_ORIGIN_WIDTH = 320;
    private static final Integer BITMAP_ORIGIN_HEIGHT = 240;
    private static final Integer GRAPHIC_ORIGIN_WIDTH = 290;
    private static final Integer GRAPHIC_ORIGIN_HEIGHT = 210;
    private static final Integer GRAPHIC_ORIGIN_X = 20;
    private static final Integer GRAPHIC_ORIGIN_Y = 220;
    private static final Integer DATE_ORIGIN_TEXT_SIZE = 16;
    private static final Integer DATE_ORIGIN_X = 20;
    private static final Integer DATE_ORIGIN_Y = 237;
    private static final Integer GRADUS_ON_GRAPHIC = 70;
    private static final Integer MIN_GRADUS_ON_GRAPHIC = -30;

    private Integer m_interval = 0;
    private ConfigActivity m_activity;

    public GraphicTask (ConfigActivity configActivity, Integer interval) {
        link(configActivity);
        // check data
        if (interval > 0) m_interval = interval;
    }

    protected Bitmap doInBackground(Object[] params) {

        // open bitmap
        final Bitmap bitmap = BitmapFactory.decodeResource(m_activity.getResources(), R.drawable.graphic);

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
        beginDate.setTime((long)beginTime * TermoBroadCastReceiver.DIVISOR_ML_SEC);
        SimpleDateFormat beginDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String beginDateString = beginDateFormat.format(beginDate);
        canvas.drawText("From " + beginDateString, dateText.getX(DATE_ORIGIN_X), dateText.getY(DATE_ORIGIN_Y), paint);

        // get amount of  data from interval in DB

        //  connect to DB
        DBHelper dbHelper = new DBHelper(m_activity);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // query all data from TERMO TABLE
        String selection = DBHelper.DATE_TERMO_ROW_NAME + ">" + beginTime;
        Cursor cursor = db.query(DBHelper.TERMO_TABLE_NAME, null, selection, null, null, null, null);

        Integer numberOfData = cursor.getCount();
        Log.d(LOG_TAG, "Amount of  data from interval in DB: " + numberOfData );

        CanvasObject graphic = new CanvasObject(canvas, BITMAP_ORIGIN_WIDTH, BITMAP_ORIGIN_HEIGHT);

        // get data and draw the rectangles
        drawRects(canvas, graphic, cursor, numberOfData);

        cursor.close();
        //  close connection to DB
        dbHelper.close();

        return bitmap;
    }

    private void drawRects(Canvas canvas,CanvasObject graphic, Cursor cursor, Integer numberOfData){
        Float graphicX = graphic.getX(GRAPHIC_ORIGIN_X);
        Float graphicY = graphic.getY(GRAPHIC_ORIGIN_Y);
        Float graphicWidth = graphic.getWidth(GRAPHIC_ORIGIN_WIDTH);
        Float graphicHeight = graphic.getHeight(GRAPHIC_ORIGIN_HEIGHT);
        Float heightPerGradus = graphicHeight / GRADUS_ON_GRAPHIC;

        Integer numberOfRect;
        Integer dataPerRect;
        Float rectWidth;

        if (numberOfData > graphicWidth){
            // number of rectangles can not be more then number of pixels in the graphic area
            numberOfRect = (int)(graphicWidth /1);
        }
        else{
            numberOfRect = numberOfData;
        }
        // calculate number of data per rectangle
        dataPerRect = numberOfData / numberOfRect;
        if( numberOfData % numberOfRect > 0) dataPerRect++;
        //  calculate  Width of rectangle
        rectWidth = graphicWidth / numberOfRect;

        Log.d(LOG_TAG, "numberOfRect: " + numberOfRect
                + " dataPerRect: " + dataPerRect
                + " rectWidth: " + rectWidth);

        Paint paint = new Paint();

        Integer temperatureColIndex = cursor.getColumnIndex(DBHelper.TEMPERATURE_TERMO_ROW_NAME);

        if(cursor.moveToFirst()){
            for(Integer rectCounter = 0; rectCounter < numberOfRect; rectCounter++){
                Integer temperatureSum=0;
                Integer dataCounter;
                for (dataCounter = 0; dataCounter < dataPerRect; dataCounter++){
                    temperatureSum += cursor.getInt(temperatureColIndex);
                    if(cursor.moveToNext() == false) {
                        break;
                    }
                }
                Integer temperature = temperatureSum / (dataCounter + 1);

                RectF rectf = new RectF(graphicX + rectCounter * rectWidth, graphicY - (temperature - MIN_GRADUS_ON_GRAPHIC)* heightPerGradus,
                                        graphicX + (rectCounter + 1) * rectWidth, graphicY);
                paint.setColor(m_activity.getResources().getColor(TermoBroadCastReceiver.TermoColor.getColor(temperature)));
                canvas.drawRect(rectf, paint);
                Log.d(LOG_TAG, "RectF: " + rectf.toString());
            }
        }

    }

    protected void onPostExecute(Bitmap result) {
        m_activity.setGraphicBitmap(result);
    }

    public void link(ConfigActivity configActivity){
        m_activity = configActivity;
    }

    public void unLink() {
        m_activity = null;
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

        public Float getWidth(Integer origin_width){
            return getValue(origin_width, m_bitmap_origin_width, canvasWidth);
        }

        public Float getHeight(Integer origin_height){
            return getValue(origin_height, m_bitmap_origin_height, canvasHeight);
        }
    }
}
