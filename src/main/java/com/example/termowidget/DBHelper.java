package com.example.termowidget;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.example.termowidget.TermoWidget.LOG_TAG;
import static com.example.termowidget.TermoWidget.isDebug;


public class DBHelper extends SQLiteOpenHelper {

    final static String DB_NAME = "temperature";
    final static String TERMO_TABLE_NAME = "temperature";
    final static String ID_TERMO_ROW_NAME = "id";
    final static String DATE_TERMO_ROW_NAME = "date";
    final static String TEMPERATURE_TERMO_ROW_NAME = "temperature";
    final static Integer DB_VER = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create table
        db.execSQL("create table "+ TERMO_TABLE_NAME +" ("
                + ID_TERMO_ROW_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + DATE_TERMO_ROW_NAME + " INTEGER,"
                + TEMPERATURE_TERMO_ROW_NAME + " INTEGER" + ");");
        if (isDebug) Log.d(LOG_TAG , "database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
