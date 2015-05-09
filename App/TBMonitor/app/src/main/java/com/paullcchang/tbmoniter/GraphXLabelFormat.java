package com.paullcchang.tbmoniter;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * Created by paul on 3/7/2015.
 */
public class GraphXLabelFormat extends Format {

    private GraphFragment.TAB tabPosition;
    public static String[] DAY_LABELS = {"12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
            "12m", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
    public static String[] WEEK_LABELS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SAB", "DOM"};
    public static String[] MONTH_LABELS = {"1", "8", "15", "22", "29"}; // CREATE NUMBER OF WEEKS DYNAMICALLY BASED ON MONTH
    public static String[] YEAR_LABELS = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};
    public static String[] MONTHS_FULL = {"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};

    public GraphXLabelFormat(GraphFragment.TAB tabPosition){
        this.tabPosition = tabPosition;
    }

    @Override
    public StringBuffer format(Object object, StringBuffer buffer, FieldPosition field) {
        String[] LABELS = getLabels();

        int parsedInt = Math.round(Float.parseFloat(object.toString()));
        String labelString =  LABELS[parsedInt];

        buffer.append(labelString);
        return buffer;
    }

    @Override
    public Object parseObject(String string, ParsePosition position) {
        String[] LABELS = getLabels();
        return java.util.Arrays.asList(LABELS).indexOf(string);
    }

    private String[] getLabels(){
        switch(tabPosition){
            case DAY:
                return DAY_LABELS;
            case WEEK:
                return WEEK_LABELS;
            case MONTH:
                return MONTH_LABELS;
            case YEAR:
                return YEAR_LABELS;
        }
        return null;
    }
}
