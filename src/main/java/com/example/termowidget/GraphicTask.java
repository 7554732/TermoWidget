package com.example.termowidget;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;

public class GraphicTask extends AsyncTask<Object, Void, Bitmap> {

    private static final int BITMAP_ORIGIN_WIDTH = 320;
    private static final int BITMAP_ORIGIN_HEIGHT = 240;
    private static final int GRAPHIC_ORIGIN_WIDTH = 290;
    private static final int GRAPHIC_ORIGIN_HEIGHT = 210;
    private static final int GRAPHIC_ORIGIN_X = 22;
    private static final int GRAPHIC_ORIGIN_Y = 220;
    private static final int ORIGIN_TEXT_SIZE = 16;
    private static final int DATE_ORIGIN_X = 20;
    private static final int DATE_ORIGIN_Y = 237;
    private static final int PATH_DATE_X_OFFSET = 2;
    private static final int PATH_DATE_Y_OFFSET = 2;
    private static final int GRADUS_ON_GRAPHIC = 70;
    private static final int MIN_GRADUS_ON_GRAPHIC = -30;

    private int mInterval = 0;
    private ConfigActivity mActivity;

    public GraphicTask (ConfigActivity configActivity, int interval) {
        link(configActivity);
        // check data
        if (interval > 0) mInterval = interval;
    }

    protected Bitmap doInBackground(Object[] params) {

        // open bitmap
        Bitmap bitmap;
        Bitmap immutableBitmap = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.graphic);
        // bitmap for canvas mast be mutable since API 11
        bitmap = immutableBitmap.copy(Bitmap.Config.ARGB_8888, true);
        //  finish GraphicTask if copy of origin bitmap is steel immutable
        if (!bitmap.isMutable())
            return bitmap;

        // create canvas
        Canvas canvas = new Canvas(bitmap);
        int canvasHeight = canvas.getHeight();
        int canvasWidth = canvas.getWidth();

        // create CanvasObject to convert object coordinates
        CanvasObject dateText = new CanvasObject(canvas, BITMAP_ORIGIN_WIDTH, BITMAP_ORIGIN_HEIGHT);

        //  create Paint for text
        Paint textPaint = new Paint();
        textPaint.setTextSize(dateText.getSize(ORIGIN_TEXT_SIZE));
        textPaint.setColor(Color.BLACK);

        // get current time and calculate time of graphic begin
        Date curDate = new Date();
        int curTime =(int) (curDate.getTime()/TermoBroadCastReceiver.DIVISOR_ML_SEC);
        int beginTime = curTime - mInterval;

        //  convert number of seconds to time string
        String beginString = timeToString(beginTime,"yyyy-MM-dd HH:mm:ss");
        //  draw graphic begin time string
        canvas.drawText("From " + beginString, dateText.getX(DATE_ORIGIN_X), dateText.getY(DATE_ORIGIN_Y), textPaint);

        // get amount of  data from interval in DB

        //  connect to DB
        DBHelper dbHelper = new DBHelper(mActivity);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // query all data from TERMO TABLE
        String selection = DBHelper.DATE_TERMO_ROW_NAME + ">" + beginTime;
        Cursor cursor = db.query(DBHelper.TERMO_TABLE_NAME, null, selection, null, null, null, null);

        int numberOfData = cursor.getCount();
        if (isDebug) Log.d(LOG_TAG , "Amount of  data from interval in DB: " + numberOfData );

        // create CanvasObject to convert object coordinates
        CanvasObject graphic = new CanvasObject(canvas, BITMAP_ORIGIN_WIDTH, BITMAP_ORIGIN_HEIGHT);

        // get data and draw the rectangles
        if(numberOfData > 0){
            drawRects(canvas, graphic, cursor, numberOfData);
        }

        cursor.close();
        //  close connection to DB
        dbHelper.close();

        return bitmap;
    }


    public static String timeToString(int time, String formatStr) {
        Date date = new Date();
        date.setTime((long)time * TermoBroadCastReceiver.DIVISOR_ML_SEC);
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatStr);
        String resString = dateFormat.format(date);
        return resString;
    }

    private void drawRects(Canvas canvas,CanvasObject graphic, Cursor cursor, int numberOfData){
        float graphicX = graphic.getX(GRAPHIC_ORIGIN_X);
        float graphicY = graphic.getY(GRAPHIC_ORIGIN_Y);
        float graphicWidth = graphic.getWidth(GRAPHIC_ORIGIN_WIDTH);
        float graphicHeight = graphic.getHeight(GRAPHIC_ORIGIN_HEIGHT);
        float heightPerGradus = graphicHeight / GRADUS_ON_GRAPHIC;

        int numberOfRect;

        if (numberOfData > graphicWidth){
            // number of rectangles can not be more then number of pixels in the graphic area
            numberOfRect = (int)(graphicWidth /1);
        }
        else{
            numberOfRect = numberOfData;
        }
        // calculate number of data per rectangle
        int dataPerRect = numberOfData / numberOfRect;
        if( numberOfData % numberOfRect > 0) dataPerRect++;
        //  calculate  Width of rectangle
        float rectWidth = graphicWidth / numberOfRect;

        if (isDebug) Log.d(LOG_TAG , "numberOfRect: " + numberOfRect
                + " dataPerRect: " + dataPerRect
                + " rectWidth: " + rectWidth);

        // create CanvasObject to convert object coordinates
        CanvasObject timeText = new CanvasObject(canvas, BITMAP_ORIGIN_WIDTH, BITMAP_ORIGIN_HEIGHT);

        //  create Paint for text
        Paint textPaint = new Paint();
        textPaint.setTextSize(timeText.getSize(ORIGIN_TEXT_SIZE));
        textPaint.setColor(Color.BLACK);

        //  create Paint for rectangles
        Paint rectPaint = new Paint();

        int dateColIndex = cursor.getColumnIndex(DBHelper.DATE_TERMO_ROW_NAME);
        int temperatureColIndex = cursor.getColumnIndex(DBHelper.TEMPERATURE_TERMO_ROW_NAME);

        if(cursor.moveToFirst()){
            //  get data and draw
            for(int rectCounter = 0; rectCounter < numberOfRect; rectCounter++){
                int temperatureSum=0;
                int dateSum=0;
                int dataCounter;
                //  get data for current rectangle
                for (dataCounter = 0; dataCounter < dataPerRect; dataCounter++){
                    dateSum += cursor.getInt(dateColIndex);
                    temperatureSum += cursor.getInt(temperatureColIndex);
                    if(cursor.moveToNext() == false) {
                        dataCounter++;
                        break;
                    }
                }
                //  averaging data if dataPerRect > 1
                int rectTime = dateSum / dataCounter;
                int rectTemperature = temperatureSum / dataCounter;

                //  create rectangle
                RectF rectf = new RectF(graphicX + rectCounter * rectWidth, graphicY - (rectTemperature - MIN_GRADUS_ON_GRAPHIC)* heightPerGradus,
                                        graphicX + (rectCounter + 1) * rectWidth, graphicY);
                //  set color for rectangle
                rectPaint.setColor(mActivity.getResources().getColor(TermoBroadCastReceiver.TermoColor.getColor(rectTemperature)));
                //  draw rectangle
                canvas.drawRect(rectf, rectPaint);
                //  create path for time string
                Path timePath =  createTimePath(graphicX, graphicY, graphicHeight, rectCounter, rectWidth);

                //  convert number of seconds to time string
                String timeString = timeToString(rectTime,"HH:mm:ss");
                timeString += " T=" + rectTemperature;
                //  draw time string
                if(textPaint.getTextSize() < rectWidth){
                    canvas.drawTextOnPath(timeString, timePath, 0, 0, textPaint);
                }

                if (isDebug) Log.d(LOG_TAG , "RectF: " + rectf.toString()+" dataCounter " + dataCounter );
            }
        }

    }

    private Path createTimePath(float graphicX, float graphicY, float graphicHeight, int rectCounter, float rectWidth) {
        Path path = new Path();
        path.reset();
        path.moveTo(graphicX + (rectCounter + 1) * rectWidth - PATH_DATE_X_OFFSET, graphicY - PATH_DATE_Y_OFFSET);
        path.lineTo(graphicX + (rectCounter + 1) * rectWidth, graphicY - graphicHeight);
        return path;
    }


    protected void onPostExecute(Bitmap result) {
        mActivity.setGraphicBitmap(result);
    }

    public void link(ConfigActivity configActivity){
        mActivity = configActivity;
    }

    public void unLink() {
        mActivity = null;
    }

    private class CanvasObject {
        private float size;
        private float x;
        private float y;
        private int m_bitmap_origin_width ;
        private int m_bitmap_origin_height;
        private int canvasHeight;
        private int canvasWidth;

        public CanvasObject (Canvas canvas, int bitmap_origin_width, int bitmap_origin_height){
            canvasHeight = canvas.getHeight();
            canvasWidth = canvas.getWidth();
            m_bitmap_origin_width = bitmap_origin_width;
            m_bitmap_origin_height = bitmap_origin_height;
        }

        private Float getValue(int origin_value, int bitmap_origin_size, int canvasSize){
            if (origin_value > 0 & m_bitmap_origin_height > 0 & canvasHeight > 0 ){
                float ratio = (float) origin_value / (float) bitmap_origin_size;
                float result = canvasSize * ratio;
                return result;
            }
            else{
                return null;
            }
        }

        public float getSize(int origin_size){
            return getValue(origin_size, m_bitmap_origin_height, canvasHeight);
        }

        public float getX(int origin_x){
            return getValue(origin_x, m_bitmap_origin_width, canvasWidth);
        }

        public float getY(int origin_y){
            return getValue(origin_y, m_bitmap_origin_height, canvasHeight);
        }

        public float getWidth(int origin_width){
            return getValue(origin_width, m_bitmap_origin_width, canvasWidth);
        }

        public float getHeight(int origin_height){
            return getValue(origin_height, m_bitmap_origin_height, canvasHeight);
        }
    }
}
