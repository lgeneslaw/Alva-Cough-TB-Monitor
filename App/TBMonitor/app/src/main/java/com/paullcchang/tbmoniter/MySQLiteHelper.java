package com.paullcchang.tbmoniter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by paul on 4/14/2015.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_COUGH_COUNTS = "COUGH_COUNTS_";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "_dateTime";
    public static final String COLUMN_COUGH_COUNT = "_coughCount";
    public static final String COLUMN_IS_POST_TREATMENT = "_isPostTreatment";

    private static final String DATABASE_NAME = "TBMonitor.db";
    private static final int DATABASE_VERSION = 1;

    private static final String CREATE = "CREATE TABLE ";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String INT_PRIMARY_TYPE = " INTEGER PRIMARY KEY AUTOINCREMENT";
    private static final String COMMA_SEP = ",";

    // Database creation sql statement
    private static final String DATABASE_CREATE_FIRSTHALF =
            CREATE + TABLE_COUGH_COUNTS;

    private static final String DATABASE_CREATE_SECONDHALF = " (" +
            COLUMN_ID + INT_PRIMARY_TYPE + COMMA_SEP +
            COLUMN_DATE + TEXT_TYPE + COMMA_SEP +
            COLUMN_COUGH_COUNT + INTEGER_TYPE + COMMA_SEP +
            COLUMN_IS_POST_TREATMENT + INTEGER_TYPE +
            ");";

    private String TABLE_COUGH_COUNTS_USER;

    private String email;

    public MySQLiteHelper(Context context, String email) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.email = email;
        this.TABLE_COUGH_COUNTS_USER = TABLE_COUGH_COUNTS;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String DB_CREATE = DATABASE_CREATE_FIRSTHALF + DATABASE_CREATE_SECONDHALF;
        Log.d("http", "onCreate: " + DB_CREATE);
        database.execSQL(DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUGH_COUNTS_USER);
        onCreate(db);
    }

}