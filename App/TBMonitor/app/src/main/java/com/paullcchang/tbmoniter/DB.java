package com.paullcchang.tbmoniter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by paul on 4/14/2015.
 */
public class DB {
    private DateFormat dateFormatter = new SimpleDateFormat(DBInterface.DATE_FORMAT);

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
            MySQLiteHelper.COLUMN_DATE,
            MySQLiteHelper.COLUMN_COUGH_COUNT,
            MySQLiteHelper.COLUMN_IS_POST_TREATMENT };
    private String email;
    private String TABLE_COUGH_COUNTS_USER;

    public DB(Context context, String email) {
        this.email = email;
        //String newUsername = email.replace("@","").replace(".","");
        dbHelper = new MySQLiteHelper(context, email);
        TABLE_COUGH_COUNTS_USER = MySQLiteHelper.TABLE_COUGH_COUNTS;
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addCoughDataPoint(CoughDataPoint c) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_DATE, dateFormatter.format(c.getDate().getTime()).toString());
        values.put(MySQLiteHelper.COLUMN_COUGH_COUNT, (Integer) c.getCoughCount());
        values.put(MySQLiteHelper.COLUMN_IS_POST_TREATMENT, c.getIsPostTreatment());
        database.insert(TABLE_COUGH_COUNTS_USER, null,
                values);
    }

    public void deleteCoughDataPointByDate(CoughDataPoint coughDataPoint) {
        Calendar date = coughDataPoint.getDate();
        database.delete(TABLE_COUGH_COUNTS_USER, MySQLiteHelper.COLUMN_DATE
                + " = " + dateFormatter.format(date.getTime()).toString(), null);
    }

    public void deleteCoughDataPoint(CoughDataPoint coughDataPoint) {
        Calendar date = coughDataPoint.getDate();
        database.delete(TABLE_COUGH_COUNTS_USER, MySQLiteHelper.COLUMN_ID
                + " = " + coughDataPoint.getId(), null);
    }

    public List<CoughDataPoint> getCoughDataListByTreatmentType(int isPostTreatment) {
        List<CoughDataPoint> coughDataList = new ArrayList<CoughDataPoint>();

        Cursor cursor = database.query(TABLE_COUGH_COUNTS_USER,
                allColumns,
                MySQLiteHelper.COLUMN_IS_POST_TREATMENT + " = " + isPostTreatment,
                null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CoughDataPoint coughDataPoint = cursorToComment(cursor);
            coughDataList.add(coughDataPoint);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return coughDataList;
    }

    private CoughDataPoint cursorToComment(Cursor cursor) {
        Calendar date = Calendar.getInstance();
        try {
            date.setTime(dateFormatter.parse(cursor.getString(1)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        CoughDataPoint c = new CoughDataPoint(date, cursor.getInt(2), cursor.getInt(3));
        c.setId(cursor.getLong(0));
        return c;
    }
}
