package com.paullcchang.tbmoniter;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.lang.Number;

/**
 * Created by paul on 3/4/2015.
 */
public class DBInterface {
    public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private DateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);

    private ArrayList<CoughDataPoint> coughDataList;
    private ArrayList<CoughDataPoint> coughDataList_preTreatment;
    private LoadCoughDataTask dataLoader;

    private DB db;

    private SessionManager session;

    private int averageCoughsPerHour;

    private String email;

    //FOR GETTING/SETTING LOCAL DATABASE
    public DBInterface(Context context){
        this.session = new SessionManager(context);
        this.email = session.getUserEmail();
        this.db = new DB(context, email);
        this.db.open();
        this.averageCoughsPerHour = 0;
    }

    //FOR LOGGING IN AND REGISTERING
    public DBInterface(){
    }

    public void loadCoughData(MainActivity mainActivity, Resources resources, TextView messageTextView) {
        //Sets off thread to make an http request from the server
        dataLoader = (LoadCoughDataTask) new LoadCoughDataTask(mainActivity, resources, messageTextView).execute();
    }

    //stores list in local backend
    private void setCoughDataList(ArrayList<CoughDataPoint> coughDataList){
        new loadSQLiteBackend().execute();
        this.coughDataList = coughDataList;
    }

    class loadSQLiteBackend extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.d("http", "pushing list to backend");
            for(CoughDataPoint c : coughDataList){
                db.addCoughDataPoint(c);
            }
            return null;
        }
    }

    //pulls list from local backend
    public void getCoughDataList(){
        Log.d("datas", "getting list from backend");
        this.coughDataList = (ArrayList) db.getCoughDataListByTreatmentType(1);
        this.coughDataList_preTreatment = (ArrayList) db.getCoughDataListByTreatmentType(0);
        if(coughDataList_preTreatment.isEmpty()) return;
        double total = 0;
        double count = 0;
        for(CoughDataPoint c : coughDataList_preTreatment){
            total += (int)c.getCoughCount();
            count++;
        }
        double average = total/count;
        averageCoughsPerHour = (int) average;
    }


    //Calculates the baseline scaled to each tab
    public Number getBaseLine(GraphFragment.TAB tabPosition) {
        Log.d("scale", "baseLine: " + scaleCoughByTab(tabPosition, averageCoughsPerHour));
        return scaleCoughByTab(tabPosition, averageCoughsPerHour);
    }

    //Calculates the baseline scaled to each tab
    public Number getMaxOrMin(GraphFragment.TAB tabPosition, boolean isMax) {
        int bufferRatio = 4;
        if(coughDataList.isEmpty()) return 0;
        Number max = averageCoughsPerHour, min = averageCoughsPerHour;
        for(CoughDataPoint c : coughDataList){
            Number coughs = c.getCoughCount();
            if(coughs.intValue() > max.intValue()){
                max = coughs;
            }
            if(coughs.intValue() < min.intValue()){
                min = coughs;
            }
        }
        int buffer = (max.intValue() - min.intValue()) / bufferRatio;
        if (buffer <= 1){
            buffer = 2;
        }
        if(isMax){
            Log.d("scale", "max: " + scaleCoughByTab(tabPosition, max.intValue() + buffer));
            return scaleCoughByTab(tabPosition, max.intValue() + buffer);
        } else {
            Log.d("scale", "min: " + scaleCoughByTab(tabPosition, min.intValue() + buffer));
            return scaleCoughByTab(tabPosition, min.intValue() - buffer);
        }
    }

    private Number scaleCoughByTab(GraphFragment.TAB tabPosition, Number coughs){
        switch (tabPosition){
            case DAY:
                return coughs.intValue();
            case WEEK:
                return coughs.intValue() * 24;
            case MONTH:
                return coughs.intValue() * 24 * 7;
            case YEAR: //TODO SCALE BY MONTH
                return coughs.intValue() * 24 * 7 * 30;
        }
        return null;
    }

    //Returns the array of coughs from the given start time to (but NOT including) the end time
    //The tab position determines how many points and what periods to use for each point.
    public Number[] getCoughs(Calendar startTime, GraphFragment.TAB tabPosition) {
        if(coughDataList == null){
            Log.d("datas", "CoughList Object not initialized, getting from backend");
            getCoughDataList();
        }
        //Calculate number of points based on tabPosition
        int numPoints = 0;
        switch(tabPosition){
            case DAY:
                Log.d("datas", "getting day's data from DB from " + dateFormatter.format(startTime.getTime()).toString());
                numPoints = 24;
                break;
            case WEEK:
                Log.d("datas", "getting weeks's data from DB from " + dateFormatter.format(startTime.getTime()).toString());
                numPoints = 7;
                break;
            case MONTH:
                Log.d("datas", "getting month's data from DB from " + dateFormatter.format(startTime.getTime()).toString());
                numPoints = 5;
                break;
            case YEAR:
                Log.d("datas", "getting year's data from DB from " + dateFormatter.format(startTime.getTime()).toString());
                numPoints = 12;
                break;
        }
        Calendar firstDay = startTime;
        Number[] coughData = new Number[numPoints];
        for(int i = 0; i < numPoints; i++){
            Calendar endTime = (Calendar) firstDay.clone();
            //Increment endTime by one period
            switch(tabPosition){
                case DAY:
                    endTime.add(Calendar.HOUR, 1);
                    break;
                case WEEK:
                    endTime.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case MONTH:
                    endTime.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case YEAR:
                    endTime.add(Calendar.MONTH, 1);
                    break;
            }
            //Get coughs for current period
            coughData[i] = getCoughs(firstDay, endTime);
            //Increment to the next period
            firstDay = (Calendar) endTime.clone();
        }
        return coughData;
    }

    //Returns the total number of coughs from the given start time to (but NOT including) the end time
    public Number getCoughs(Calendar startTime, Calendar endTime) {
        //Return -1 because there were no data points in the future
        if(!startTime.before(Calendar.getInstance())){
            Log.d("datas", "startTime was past today, no datapoint");
            return -1;
        }
        if(!endTime.before(Calendar.getInstance())){
            Log.d("datas", "endTime switched from: " + dateFormatter.format(endTime.getTime()).toString() + " to now");
            endTime = Calendar.getInstance();
        }
        Log.d("datas", "getting data from DB from " + dateFormatter.format(startTime.getTime()).toString() + " to " + dateFormatter.format(endTime.getTime()).toString());
        Number totalCoughs = 0;
        int count = 0;
        if (coughDataList == null) return null;
        for(CoughDataPoint coughDataPoint : coughDataList){
            if(coughDataPoint.isBetween(startTime, endTime)){
                totalCoughs = (int)totalCoughs + (int)coughDataPoint.getCoughCount();
                count++;
            }
        }
        //Return -1 because there were no data points for that entry.
        if(count == 0){
            return -1;
        }
        Log.d("datas", "total coughs for this period: " + totalCoughs + " | count: " + count);
        return totalCoughs;
    }

    public int getLoadVersion() {
        return 1;
    }

    /**
     * Background Async Task to Load all Cough Data by making HTTP Requests
     * */
    class LoadCoughDataTask extends AsyncTask<String, String, String> {

        //UI elements that are affected
        private MainActivity mainActivity;
        private TextView messageTextView;
        private Resources resources;

        private boolean error;

        public LoadCoughDataTask(MainActivity mainActivity, Resources resources, TextView messageTextView){
            this.mainActivity = mainActivity;
            this.messageTextView = messageTextView;
            this.resources = resources;
            error = false;
            session = new SessionManager(mainActivity);
        }

        // Creating JSON Parser object
        private JSONParser jParser = new JSONParser();

        private JSONArray coughJSON;

        // url to get all products list
        private static final String URL = "http://skysip.org/p539/php/";
        private static final String url_get_patient_cough_data = URL + "db_get_patient_cough_data.php";

        //skysip.org ip: 67.20.113.159

        // JSON Node names
        public static final String TAG_SUCCESS = "success";
        public static final String TAG_COUGH_COUNTS = "cough counts";
        public static final String TAG_DATE = "date time";
        public static final String TAG_COUGH_COUNT = "cough count";
        public static final String TAG_POST_TREATMENT = "post treatment";

        private static final String PARAM_EMAIL = "email";

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * get all the cough data from the server
         * */
        protected String doInBackground(String... args) {
            if(session.isLoadVersionUpToDate(getLoadVersion())) {
                mainActivity.signalDataLoaded();
                return null;
            }

            String email = session.getUserEmail();
            if (email == null || !email.equals("lgeneslaw@yahoo.com")){
                email = "lgeneslaw@gmail.com";
            }
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(PARAM_EMAIL, email));
            //params.add(new BasicNameValuePair("D1", "2015-03-05 2008:00:00"));
            //params.add(new BasicNameValuePair("D2", "2015-03-06 2001:00:00"));
            // getting JSON string from URL
            //TODO add params
            JSONObject json = jParser.makeHttpRequest(url_get_patient_cough_data, JSONParser.GET, params);

            // Check your log cat for JSON reponse
            if(json != null) {
                Log.d("http", "return data: " + json.toString());
            }

            try {
                int success = 0;
                // Checking for SUCCESS TAG
                if(json != null) {
                    success = json.getInt(TAG_SUCCESS);
                }
                Log.d("http", "success tag: " + success);

                if (success == 1) {
                    // Getting Array of Products
                    coughJSON = json.getJSONArray(TAG_COUGH_COUNTS);
                    Log.d("http", "parsing data out of array of length: " + coughJSON.length());

                    // creating new HashMap
                    ArrayList<CoughDataPoint> coughDataList = new ArrayList<>();

                    // looping through All Products
                    for (int i = 0; i < coughJSON.length(); i++) {
                        CoughDataPoint c = new CoughDataPoint(coughJSON.getJSONObject(i));
                        if(i % 100 == 0) {
                            Log.d("http", "got: " + c.getCoughCount());
                        }
                        coughDataList.add(c);
                    }
                    //TODO SET LOAD VERSION TO LOAD VERSION FROM HTTP REQUEST
                    session.setLoadVersion(1);

                    //TODO Collections.sort(coughDataList);
                    setCoughDataList(coughDataList);

                } else {
                    error = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
            if(error){
                errorMessage();
            }else {
                //Signal that data is loaded to the main activity
                Log.d("http", "finished parsing!");
                mainActivity.signalDataLoaded();
            }

        }

        private void errorMessage(){
            //if bad then
            messageTextView.setTextColor(resources.getColor(R.color.declining_red));
            messageTextView.setText(resources.getText(R.string.message_http_error));
        }

    }

}
