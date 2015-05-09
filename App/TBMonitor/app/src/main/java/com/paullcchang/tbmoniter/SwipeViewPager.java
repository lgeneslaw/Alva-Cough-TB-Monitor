package com.paullcchang.tbmoniter;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by paul on 4/2/2015.
 * Custom ViewPager that enables/disables swiping between fragments.
 */
public class SwipeViewPager extends ViewPager {

    private boolean swipeEnabled;

    public SwipeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.swipeEnabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.swipeEnabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.swipeEnabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void enableSwipe(boolean swipeEnabled) {
        this.swipeEnabled = swipeEnabled;
    }
}