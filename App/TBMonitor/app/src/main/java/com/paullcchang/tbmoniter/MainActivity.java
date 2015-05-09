package com.paullcchang.tbmoniter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.TextView;

import java.lang.reflect.Field;


public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, GraphFragment.OnCompleteListener {

    public static String TAB_POSITION = "position";
    public static String DATABASE = "database";
    public static String FIRST_DAY = "firstDay";
    public static String BUTTON_INDEX = "buttonIndex";
    public static String DATA_LOADED = "dataLoaded";
    public static int NUM_TABS = 4;

    private SwipeViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;
    private GraphFragment[] fragments;
    private Bundle[] savedFragmentArgs;
    private TextView messageTextView;

    private DBInterface DB;
    public DBInterface getDB(){
        return DB;
    }

    private boolean dataLoaded;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(getApplicationContext());
        session.checkLogin();

        //Forces the action bar to have the overflow menu for the logout/settings
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        fragments = new GraphFragment[NUM_TABS];
        if (savedInstanceState == null) {
            //Add 4 fragments
            for (int i = 0; i < NUM_TABS; i++) {
                //Store their tab Position so fragments can customize based on day/week/month/yeah
                Bundle args = new Bundle();
                args.putInt(TAB_POSITION, i);
                GraphFragment graphFragment = new GraphFragment();
                graphFragment.setArguments(args);
                fragments[i] = graphFragment;
            }
        } else {
            for (int i = 0; i < NUM_TABS; i++) {
                fragments[i] = (GraphFragment) getSupportFragmentManager().findFragmentByTag(getFragmentTag(i));
            }
        }

        savedFragmentArgs = new Bundle[NUM_TABS];
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        viewPager = (SwipeViewPager) findViewById(R.id.pager);
        viewPager.enableSwipe(false);
        viewPager.setAdapter(mAdapter);

        messageTextView = (TextView) findViewById(R.id.messageTextView);

        setUpTabs();

        //if data is already loaded, then don't make another http request
        if (savedInstanceState == null) {
            loadData();
        }else {
            dataLoaded = savedInstanceState.getBoolean(DATA_LOADED);
            if (!dataLoaded) {
                loadData();
            }else{
                calculateMessage();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * On selecting action bar icons
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                //TODO requery the server
                return true;
            case R.id.action_settings:
                //attach settings page
                return true;
            case R.id.action_logout:
                if(session != null) {
                    session.logoutUser();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean(DATA_LOADED, dataLoaded);
    }

    //Hacky code to get the Fragment by its fragment ID
    private String getFragmentTag(int fragmentIndex)
    {
        return "android:switcher:" + R.id.pager + ":" + fragmentIndex;
    }

    private void setUpTabs(){
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setElevation(0);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        String[] TAB_NAMES = { getResources().getString(R.string.tab_days),
                getResources().getString(R.string.tab_weeks),
                getResources().getString(R.string.tab_months),
                getResources().getString(R.string.tab_all)};

        // Adding Tabs
        for (String tab_name : TAB_NAMES) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }

        //On swiping the viewpager make respective tab selected
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                Log.d("tab", "onPageChangeListener: " + position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }

    private void loadData(){
        //Hook up to the database
        DB = new DBInterface(this);

        //Loading data
        //TODO set spinner
        messageTextView.setTextColor(getResources().getColor(R.color.loading_blue));
        messageTextView.setText(getResources().getText(R.string.message_loading));

        dataLoaded = false;

        //If version is up to date then don't load the data
        if(session.isLoadVersionUpToDate(DB.getLoadVersion())){
            signalDataLoaded();
        }else {
            //Sets off thread to make an http request from the server
            DB.loadCoughData(this, getResources(), messageTextView);
        }
    }

    //Getters and Setters in order to persist fragment state
    public void saveMainFragmentState(Bundle args, int tabIndex) {
        savedFragmentArgs[tabIndex] = args;
    }

    public Bundle getSavedMainFragmentState(int tabIndex) {
        return savedFragmentArgs[tabIndex];
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
        Log.d("tab", "onTabSelected: " +  tab.getPosition());
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onComplete(int tabIndex) {
        if(dataLoaded == false) return;
        Log.d("fragments", "onComplete setting up " + tabIndex +  " for the first time");
        GraphFragment fragment = fragments[tabIndex];
        fragment.setUpFirstTime();
    }

    public void signalDataLoaded() {
        Log.d("fragments", "data finished loading");
        dataLoaded = true;
        if(fragments != null){
            for(int i = 0; i < fragments.length; i++){
                if(fragments[i] != null){
                    if(fragments[i].fragmentRequiresFirstLoad()) {
                        Log.d("fragments", "setting up " + i + " for the first time");
                        fragments[i].setUpFirstTime();
                    }
                }
            }
        }
        calculateMessage();
        Log.d("fragments", "data loaded and fragments initialized");
    }

    private void calculateMessage() {
        //TODO call DB method to determine the message
        //if bad then
        messageTextView.setTextColor(getResources().getColor(R.color.declining_red));
        messageTextView.setText(getResources().getText(R.string.message_declining));
    }

    //Page adapter for ViewPager to get the correct fragments
    public class TabsPagerAdapter extends FragmentPagerAdapter {

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }
    }

}

