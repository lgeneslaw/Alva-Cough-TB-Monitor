package com.paullcchang.tbmoniter;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.androidplot.LineRegion;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by paul on 3/31/2015.
 */
public class GraphFragment extends Fragment implements View.OnTouchListener {
    private DateFormat dateFormatter = new SimpleDateFormat(DBInterface.DATE_FORMAT);

    //Chart specific private vars
    private int GRIDPADDING = 30;

    private XYPlot plot;
    private XYSeries coughSeries = null;
    private Map<Number, XYSeries> linesSeries;
    private LineAndPointFormatter coughSeriesFormat, linesFormat;

    private TextView numCoughsTextView,
            messageTextView;

    //Database specific private vars
    private DBInterface DB;

    //Button list specific private vars
    private HorizontalScrollView buttonList;
    private Button[] buttons;
    private int buttonChosen = 0;

    private static final Map<TAB , Integer> numButtonsMap = new HashMap<TAB , Integer>() {{
        put(TAB.DAY, 30);
        put(TAB.WEEK, 15);
        put(TAB.MONTH, 12);
        put(TAB.YEAR, 3);
    }};

    //Fragment specific private vars
    private View rootView;
    private FragmentActivity activity;

    public enum TAB {
        DAY, WEEK, MONTH, YEAR
    }
    private boolean tabPositionSet = false;
    private int tabIndex;
    private TAB tabPosition;

    private boolean fragmentRequiresFirstLoad;

    //INTERFACE FOR ON COMPLETE CALLBACK
    public static interface OnCompleteListener {
        public abstract void onComplete(int tabIndex);
    }

    private OnCompleteListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCompleteListener)activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentRequiresFirstLoad = false;
        rootView = inflater.inflate(R.layout.fragment_graph, container, false);
        activity = getActivity();

        numCoughsTextView = (TextView) rootView.findViewById(R.id.numCoughsTextView);
        messageTextView =  (TextView) activity.findViewById(R.id.messageTextView);

        //Hook up to database
        DB = ((MainActivity) activity).getDB();

        //Get tab Position which determines whether chart is used for day/week/month/all
        tabIndex = getArguments().getInt(MainActivity.TAB_POSITION, 0);
        switch(tabIndex){
            case 0:
                tabPosition = TAB.DAY;
                break;
            case 1:
                tabPosition = TAB.WEEK;
                break;
            case 2:
                tabPosition = TAB.MONTH;
                break;
            case 3:
                tabPosition = TAB.YEAR;
                break;
        }

        setUpPlot();

        //Check if there is saved state.
        Bundle args = ((MainActivity) getActivity()).getSavedMainFragmentState(tabIndex);

        Calendar firstDay = null;
        int buttonToSelect = 0;
        if (args != null) { //View was destroyed so use the saved data to restore plot
            firstDay = (Calendar) args.getSerializable(MainActivity.FIRST_DAY);
            buttonToSelect = args.getInt(MainActivity.BUTTON_INDEX);
            setUpFromSavedData(firstDay, buttonToSelect);
        } else if (savedInstanceState != null) { //Out of memory get from saved instance state
            firstDay = (Calendar) savedInstanceState.getSerializable(MainActivity.FIRST_DAY);
            buttonToSelect = savedInstanceState.getInt(MainActivity.BUTTON_INDEX);
            setUpFromSavedData(firstDay, buttonToSelect);
        } else { //Created for the first time or never received data.
            fragmentRequiresFirstLoad = true;
            mListener.onComplete(tabIndex);
        }

        return rootView;
    }

    //If the fragment hasn't loaded it's data yet, this will return true, and will set it to false.
    public boolean fragmentRequiresFirstLoad(){
        if(fragmentRequiresFirstLoad){
            fragmentRequiresFirstLoad = false;
            return true;
        }
        return false;
    }

    public void setUpFirstTime(){
        Calendar firstDay = getFirstDayInCurrentPeriod();
        Number[] coughData = DB.getCoughs(firstDay, tabPosition);
        Number baseLine = DB.getBaseLine(tabPosition);
        boolean FINDMIN = false, FINDMAX = true;
        Number rangeMax = DB.getMaxOrMin(tabPosition, FINDMAX);
        Number rangeMin = DB.getMaxOrMin(tabPosition, FINDMIN);
        addDataToPlot(coughData, baseLine, rangeMin, rangeMax);
        setUpButtons();
    }

    public void setUpFromSavedData(Calendar firstDay, int buttonToSelect){
        //events like screen changes make firstDay inputted as null, so we have to firstLoad it.
        if(firstDay == null){
            fragmentRequiresFirstLoad = true;
            mListener.onComplete(tabIndex);
        }else { //else we load the chart from the persisted data
            Number[] coughData = DB.getCoughs(firstDay, tabPosition);
            Number baseLine = DB.getBaseLine(tabPosition);
            boolean FINDMIN = false, FINDMAX = true;
            Number rangeMax = DB.getMaxOrMin(tabPosition, FINDMAX);
            Number rangeMin = DB.getMaxOrMin(tabPosition, FINDMIN);
            addDataToPlot(coughData, baseLine, rangeMin, rangeMax);
            setUpButtons();
            setButtonSelected(buttonToSelect);
        }
    }

    //When the view is destroyed, save the plot data
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Bundle args = saveInstance();
        ((MainActivity) getActivity()).saveMainFragmentState(args, tabIndex);
    }

    //When the view is destroyed, save the plot data
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState = saveInstance();
    }

    private Bundle saveInstance() {
        //If there is data to be saved, save it to the bundle
        if (buttons != null) {
            Bundle data = new Bundle();
            Calendar firstDay = (Calendar) ((Calendar) buttons[buttonChosen].getTag(R.id.TAG_FIRST_DAY)).clone();
            data.putSerializable(MainActivity.FIRST_DAY, firstDay);
            data.putInt(MainActivity.BUTTON_INDEX, buttonChosen);
            return data;
        }
        return null;
    }

    //Getters used when the http request returns with data to load the ui with data points.
    public TAB getTabPosition(){
        return tabPosition;
    }

    public FragmentActivity getFragmentActivity(){
        return getActivity();
    }

    public void setUpButtons(){
        buttonList = (HorizontalScrollView) rootView.findViewById(R.id.buttonsScrollView);
        buttonList.addView(createButtonListView());
    }

    private View createButtonListView(){
        final int numButtons = numButtonsMap.get(tabPosition);
        buttons = new Button[numButtons];
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        Calendar lastDay = Calendar.getInstance(); //Initialize with today
        Calendar firstDay = getFirstDayInCurrentPeriod();
        for(int i = 0; i < numButtons; i++) {
            //Create and format the button
            buttons[i] = new Button(activity);
            buttons[i].setTag(i);
            buttons[i].setText(getButtonText(firstDay, lastDay));
            buttons[i].setBackgroundColor(getResources().getColor(R.color.graph_background_lightblue));
            buttons[i].setAlpha((float) 0.3); //initialize all buttons to unselected
            buttons[i].setTextColor(getResources().getColor(R.color.domain_label_blue));
            buttons[i].setTag(R.id.TAG_FIRST_DAY, firstDay.clone());
            buttons[i].setTag(R.id.TAG_LAST_DAY, lastDay.clone());

            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int buttonSelected = (int) v.getTag();
                    if (buttonChosen == buttonSelected) return;
                    setButtonSelected(buttonSelected);
                    Log.d("button", "button: " + buttonSelected);
                    Calendar firstDay = (Calendar) ((Calendar) v.getTag(R.id.TAG_FIRST_DAY)).clone();
                    //Calendar lastDay = (Calendar) ((Calendar) v.getTag(R.id.TAG_LAST_DAY)).clone();
                    Log.d("datas", "button wants from: " + dateFormatter.format(firstDay.getTime()).toString());
                    Number[] coughData = DB.getCoughs(firstDay, tabPosition);
                    Number baseLine = DB.getBaseLine(tabPosition);
                    boolean FINDMIN = false, FINDMAX = true;
                    Number rangeMax = DB.getMaxOrMin(tabPosition, FINDMAX);
                    Number rangeMin = DB.getMaxOrMin(tabPosition, FINDMIN);
                    addDataToPlot(coughData, baseLine, rangeMin, rangeMax);
                }
            });
            //Add the button to the UI
            layout.addView(buttons[i]);

            //Change to next period for the next button
            lastDay = (Calendar) firstDay.clone();
            firstDay = getFirstDayInPreviousPeriod(firstDay);
        }

        setButtonSelected(0); //select first button by default

        return layout;
    }

    private Calendar getFirstDayInPreviousPeriod(Calendar currentFirstDay){
        switch(tabPosition){
            case DAY:
                currentFirstDay.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case WEEK:
                currentFirstDay.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case MONTH:
                currentFirstDay.add(Calendar.MONTH, -1);
                break;
            case YEAR:
                currentFirstDay.add(Calendar.YEAR, -1);
                break;
        }
        return (Calendar) currentFirstDay.clone();
    }

    private Calendar getFirstDayInCurrentPeriod(){
        Calendar firstDay = Calendar.getInstance();
        firstDay.set(Calendar.HOUR_OF_DAY, 0);
        firstDay.set(Calendar.MINUTE, 0);
        firstDay.set(Calendar.SECOND, 0);
        firstDay.set(Calendar.MILLISECOND, 0);
        switch(tabPosition){
            case DAY:
                //Current day at 00:00:00
                break;
            case WEEK:
                int dayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK);
                dayOfWeek -= 1; //Start on Mondays
                if (dayOfWeek == 0) {
                    dayOfWeek = 7;
                }
                dayOfWeek -= 1; //Subtracting days need to decrement by 1
                firstDay.add(Calendar.DAY_OF_YEAR, -(dayOfWeek));
                break;
            case MONTH:
                firstDay.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case YEAR:
                firstDay.set(Calendar.MONTH, Calendar.JANUARY);
                firstDay.set(Calendar.DAY_OF_MONTH, 1);
                break;
        }
        Log.d("dates", "getting first day in period: " + dateFormatter.format(firstDay.getTime()).toString());
        return (Calendar)firstDay.clone();
    }

    private void setButtonSelected(int button){
        if (buttons == null) return;
        buttons[buttonChosen].setAlpha((float) 0.3);
        buttons[button].setAlpha((float) 0.8);
        buttonChosen = button;
    }

    private String getButtonText(Calendar startDate, Calendar endDate){
        //TODO: redo button format and then change text here
        String buttonText = null;
        switch(tabPosition){
            case DAY:
                buttonText = startDate.get(Calendar.DAY_OF_MONTH) + " "  +
                        GraphXLabelFormat.YEAR_LABELS[startDate.get(Calendar.MONTH)];
                break;
            case WEEK:
                buttonText = startDate.get(Calendar.DAY_OF_MONTH) + " "  +
                        GraphXLabelFormat.YEAR_LABELS[startDate.get(Calendar.MONTH)] + " - "  +
                        endDate.get(Calendar.DAY_OF_MONTH) + " "  +
                        GraphXLabelFormat.YEAR_LABELS[endDate.get(Calendar.MONTH)];
                break;
            case MONTH:
                buttonText = GraphXLabelFormat.MONTHS_FULL[startDate.get(Calendar.MONTH)];
                break;
            case YEAR:
                buttonText = "" + startDate.get(Calendar.YEAR);
                break;
        }
        return buttonText;
    }

    private void setUpPlot(){
        plot = (XYPlot) rootView.findViewById(R.id.coughPlot);

        //Remove all default padding
        plot.setPlotMargins(0, 0, 0, 0);
        plot.setPlotPadding(-40, 0, 0, -15);
        plot.getGraphWidget().setMargins(0, 0, 0, 0);
        plot.getGraphWidget().setPadding(0, 0, 0, 0);
        plot.getGraphWidget().position(0, XLayoutStyle.ABSOLUTE_FROM_LEFT,
                0, YLayoutStyle.ABSOLUTE_FROM_TOP,
                AnchorPosition.LEFT_TOP);
        plot.getGraphWidget().setSize(new SizeMetrics(0, SizeLayoutType.FILL,
                0, SizeLayoutType.FILL));
        plot.setLegendWidget(null);

        //Adjust axis padding
        int gridPaddingLeft = GRIDPADDING,
                gridPaddingTop = 0,
                gridPaddingRight = GRIDPADDING,
                gridPaddingBottom = GRIDPADDING;
        plot.getGraphWidget().setGridPadding(gridPaddingLeft, gridPaddingTop,
                gridPaddingRight, gridPaddingBottom);

        //Create a formatter to use for drawing a series using LineAndPointRenderer
        //and configure it from xml:
        //Formatting for the CoughSeries
        coughSeriesFormat = new LineAndPointFormatter();
        coughSeriesFormat.setPointLabelFormatter(new PointLabelFormatter());
        coughSeriesFormat.configure(activity.getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);
        //Hide vertex and labels
        coughSeriesFormat.setVertexPaint(null);
        coughSeriesFormat.setPointLabeler(null);
        //Formatting for the lines
        linesFormat = new LineAndPointFormatter();
        linesFormat.setPointLabelFormatter(new PointLabelFormatter());
        linesFormat.configure(activity.getApplicationContext(),
                R.xml.line_point_formatter_with_lines);
        //Hide vertex and labels
        linesFormat.setVertexPaint(null);
        linesFormat.setPointLabeler(null);

        // Hide the range labels
        plot.getGraphWidget().setRangeLabelPaint(null);
        // reduce the number of range labels to 6
        plot.setRangeStep(XYStepMode.SUBDIVIDE, 7);

        int numXs = 0;
        int xIncrementBy = 0;
        switch(tabPosition){
            case DAY:
                numXs = 24;
                xIncrementBy = 3;
                break;
            case WEEK:
                numXs = 7;
                xIncrementBy = 1;
                break;
            case MONTH:
                numXs = 5;
                xIncrementBy = 1;
                break;
            case YEAR:
                numXs = 12;
                xIncrementBy = 2;
                break;
        }
        //Set 7 days for the x axis and increment by 1
        plot.setDomainBoundaries(0, numXs - 1, BoundaryMode.FIXED);
        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, xIncrementBy);
        //Change labels to Words
        plot.setDomainValueFormat(new GraphXLabelFormat(tabPosition));

        //Move xAxis up
        int xAxisVerticalOffset = -37;//-(R.dimen.domain_tick_label_font_size + 100);
        plot.getGraphWidget().setDomainLabelVerticalOffset(xAxisVerticalOffset);
        plot.getGraphWidget().getDomainLabelPaint().setColor(getResources().getColor(R.color.domain_label_blue));
        plot.getGraphWidget().getRangeGridLinePaint().setColor(getResources().getColor(R.color.grid_lightblue));

        //Remove grids
        plot.getGraphWidget().setDomainGridLinePaint(null);
        plot.getGraphWidget().setDomainOriginLinePaint(null);
        plot.getGraphWidget().setRangeOriginLinePaint(null);

        plot.getGraphWidget().setBackgroundPaint(null);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(getResources().getColor(R.color.graph_background_lightblue));
        plot.setBackgroundPaint(null);

        plot.setOnTouchListener(this);
    }


    //TODO coughdata will be changed from Number[] to an object with various flags for the data.
    public void addDataToPlot(Number[] coughData, Number baseLine, Number rangeMin, Number rangeMax){
        plot.clear();

        Log.d("datas", "adding data to plot from: " + coughData.toString());
        //count the number of legitimate entries
        int numGoodData = coughData.length;
        for(int i = 0; i < coughData.length; i++){
            //TODO when there are flags in the coughData, check for flags instead
            //if there is no reliable cough data for that point
            if(coughData[i] == null || (int)coughData[i] < 0){
                numGoodData--;
            }
        }

        //TODO grey out the labels for unreliable cough points.
        //Generate the xAxis values for the coughData
        int iter = 0;
        Number[] yAxis = new Number[numGoodData];
        Number[] xAxis = new Number[numGoodData];
        for(int i = 0; i < coughData.length; i++){
            //if there is reliable then store the x value cough data
            if((int)coughData[i] >= 0){
                xAxis[iter] = i;
                yAxis[iter] = coughData[i];
                iter++;
            }
        }

        // Turn the cough array into XYSeries':
        coughSeries = new SimpleXYSeries(
                Arrays.asList(xAxis),
                Arrays.asList(yAxis),
                "");                             // Series Name

        // add a new series' to the xyplot:
        plot.addSeries(coughSeries, coughSeriesFormat);

        Number[] baseLineY = {baseLine, baseLine};
        //TODO change -100 and 100 hacky..
        Number[] baseLineX = {-100,100};

        SimpleXYSeries baseLineSeries = new SimpleXYSeries(
                Arrays.asList(baseLineX),
                Arrays.asList(baseLineY),
                "");

        plot.addSeries(baseLineSeries, linesFormat);

        //Set Range Max and Mins
        plot.setRangeBoundaries(rangeMin, rangeMax, BoundaryMode.FIXED);

        //Creates the lineSeries to be displayed when a data point is pressed
        linesSeries = new HashMap<>();

        int buffer = ((int)rangeMax - (int)rangeMin) / 10;
        if(buffer < 1) {
            buffer = 1;
        }
        rangeMax = (int)rangeMax - buffer;
        for(int i = 0; i < xAxis.length; i++) {
            Number[] linesY = {rangeMax, rangeMin};
            Number[] linesX = {xAxis[i], xAxis[i]};
            //Map the x Axis value to the Line
            linesSeries.put(xAxis[i], new SimpleXYSeries(
                    Arrays.asList(linesX),
                    Arrays.asList(linesY),
                    ""));
        }

        plot.redraw();
    }

    //default at -1 if no line should be shown.
    private int lineToShow = -1;

    //@Override
    //When the user touches the graph, show the value of the point closest and a line which
    //indicates which point is being displayed
    public boolean onTouch(View v, MotionEvent event) {
        if(coughSeries == null || coughSeries.size() == 0){
            return true;
        }
        Number numCoughs[] = new Number[2];
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                numCoughs = getClosestCoughValuePair(new PointF(event.getX(), event.getY()));
                if(lineToShow != (int) numCoughs[0]){
                    //Hide Current line and show the new line
                    if(lineToShow != -1) {
                        plot.removeSeries(linesSeries.get(lineToShow));
                    }
                    lineToShow = (int) numCoughs[0];
                    plot.addSeries(linesSeries.get(lineToShow), linesFormat);
                    plot.redraw();
                }
                //Set textView to the Number of Coughs
                numCoughsTextView.setText(String.valueOf(numCoughs[1]));
                numCoughsTextView.setVisibility(View.VISIBLE);

                break;
            case MotionEvent.ACTION_MOVE:
                numCoughs = getClosestCoughValuePair(new PointF(event.getX(), event.getY()));
                if(lineToShow != (int) numCoughs[0]){
                    //Hide Current line and show the new line
                    if(lineToShow != -1) {
                        plot.removeSeries(linesSeries.get(lineToShow));
                    }
                    lineToShow = (int) numCoughs[0];
                    plot.addSeries(linesSeries.get(lineToShow), linesFormat);
                    plot.redraw();
                }
                //Set textView to the Number of Coughs
                numCoughsTextView.setText(String.valueOf(numCoughs[1]));
                numCoughsTextView.setVisibility(View.VISIBLE);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                //Remove current line and redraw and reset lineToShow
                if(lineToShow != -1) {
                    plot.removeSeries(linesSeries.get(lineToShow));
                    plot.redraw();
                    lineToShow = -1;
                }
                //Hide TextView
                numCoughsTextView.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
        return true;
    }

    //Returns the closest point to a given point
    private Number[] getClosestCoughValuePair(PointF point){
        if(plot == null) return null;
        //Fix bug of going negative.
        if(point.x <= GRIDPADDING + 1){
            point.x = GRIDPADDING + 1;
        }
        Number xValue = plot.getXVal(point);
        Number[] closestNumCoughs = {coughSeries.getX(0), coughSeries.getY(0)};
        double closestDistance = LineRegion.measure(xValue, coughSeries.getX(0)).doubleValue();
        for (int i = 1; i < coughSeries.size(); i++) {
            Number pointX = coughSeries.getX(i);
            if (pointX != null) {
                double distance = LineRegion.measure(xValue, pointX).doubleValue();
                if (distance < closestDistance) {
                    closestDistance = distance;
                    //get the xAxis value and yAxis value
                    closestNumCoughs[0] = coughSeries.getX(i);
                    closestNumCoughs[1] = coughSeries.getY(i);
                }
            }
        }
        return closestNumCoughs;
    }
}
