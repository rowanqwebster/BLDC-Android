package com.example.bldc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "monitorInfo";
    private static final String TABLE_INFO = "info";
    private static final String KEY_ID = "id";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INFO);
        String CREATE_INFO_TABLE = "CREATE TABLE " + TABLE_INFO + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + Constants.SPEED + " REAL DEFAULT 0,"
                + Constants.POWER + " REAL DEFAULT 0,"
                + Constants.CURRENT + " REAL DEFAULT 0,"
                + Constants.CONTROL_TEMP + " REAL DEFAULT 0,"
                + Constants.BATTERY_VOLT + " REAL DEFAULT 0,"
                + Constants.BATTERY_REM + " REAL DEFAULT 0,"
                + Constants.PWM_FREQ + " REAL DEFAULT 0,"
                + Constants.MAX_SPEED + " REAL DEFAULT 0,"
                + Constants.MAX_POWER_DRAW + " REAL DEFAULT 0,"
                + Constants.MAX_CURRENT_DRAW + " REAL DEFAULT 0,"
                + Constants.BATTERY_CHEMISTRY + " INTEGER DEFAULT 0,"
                + Constants.DRIVING_MODE + " INTEGER DEFAULT 0,"
                + Constants.THROTTLE_CAL + " INTEGER DEFAULT 0"
                + ")";
        Log.i("test","test");
        db.execSQL(CREATE_INFO_TABLE);
        db.execSQL("INSERT INTO " + TABLE_INFO + " DEFAULT VALUES");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INFO);

        // Create tables again
        onCreate(db);
    }

    public void resetInfo()
    {
        SQLiteDatabase db = getWritableDatabase();
        onCreate(db);
    }

    public void setInfo(String key, double value)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(key, value);
        db.update(TABLE_INFO, values, KEY_ID + " = ?", new String[] {"1"});
    }

    public double getInfo(String key)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + key + " FROM " + TABLE_INFO, null);
        if (c.moveToFirst())
        {
            return c.getDouble(0);
        }
        return -1;
    }
}
