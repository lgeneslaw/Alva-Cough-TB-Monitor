package com.paullcchang.tbmoniter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by paul on 4/14/2015.
 */
public class CoughDataPoint implements Comparable{

    private DateFormat dateFormatter = new SimpleDateFormat(DBInterface.DATE_FORMAT);

    private long _id;

    private boolean isPostTreatment;
    private Number coughCount;
    private Calendar date;

    public CoughDataPoint(){
        this._id = 0;
        this.date = null;
        this.coughCount = 0;
        this.isPostTreatment = true;
    }

    public CoughDataPoint(Calendar date, Number coughCount, int isPostTreatment){
        this._id = 0;
        this.date = date;
        this.coughCount = coughCount;
        setIsPostTreatment(isPostTreatment);
    }

    public CoughDataPoint(JSONObject coughJSON){
        this._id = 0;
        try {
            String dateInString = null;
            dateInString = coughJSON.getString(DBInterface.LoadCoughDataTask.TAG_DATE);

            this.date = Calendar.getInstance();
            DateFormat dateFormatter = new SimpleDateFormat(DBInterface.DATE_FORMAT);
            this.date.setTime(dateFormatter.parse(dateInString));
            this.coughCount = coughJSON.getInt(DBInterface.LoadCoughDataTask.TAG_COUGH_COUNT);
            setIsPostTreatment(coughJSON.getInt(DBInterface.LoadCoughDataTask.TAG_POST_TREATMENT));
        }
        catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public long getId(){
        return _id;
    }

    public void setId(long _id){
        this._id = _id;
    }

    public boolean getIsPostTreatment(){
        return isPostTreatment;
    }

    public Number getCoughCount(){
        return coughCount;
    }

    public Calendar getDate(){
        return (Calendar) date.clone();
    }

    public void setIsPostTreatment(int isPostTreatment){
        if(isPostTreatment == 0) {
            this.isPostTreatment = false;
        }else{
            this.isPostTreatment = true;
        }
    }

    public void setCoughCount(int coughCount){
        this.coughCount = coughCount;
    }

    public void setDate(Calendar date){
        this.date = date;
    }

    @Override
    public int compareTo(Object otherDataPoint) {
        if(otherDataPoint == null){
            return -1;
        }
        Calendar otherDate = ((CoughDataPoint) otherDataPoint).getDate();
        if(otherDate == null){
            return -1;
        }
        return date.compareTo(otherDate);
    }

    //returns whether point is between [start, end) Start inclusive and NOT end inclusive.
    public boolean isBetween(Calendar start, Calendar end) {
        if(start == null || end == null){
            return false;
        }
        return date.compareTo(start) >= 0 && date.before(end);
    }

    public String toString(){
        return dateFormatter.format(date.getTime()) + " | " + coughCount;
    }
}


