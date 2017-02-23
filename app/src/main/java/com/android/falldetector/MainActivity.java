package com.android.falldetector;

import android.annotation.TargetApi;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        isAYOActive = false;
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, EditTemplate.class);
            startActivity(intent);

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                View rootView = inflater.inflate(R.layout.fragment_wave, container, false);
                return rootView;
            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
                View rootView = inflater.inflate(R.layout.fragment_alert, container, false);
                return rootView;
            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER) == 3) {
                View rootView = inflater.inflate(R.layout.fragment_history, container, false);
                return rootView;
            }
            else if(getArguments().getInt(ARG_SECTION_NUMBER) == 4) {
                View rootView = inflater.inflate(R.layout.fragment_statistics, container, false);
                return rootView;
            }
            else
            {
                View rootView = inflater.inflate(R.layout.fragment_main, container, false);
                return rootView;
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "WAVE";
                case 1:
                    return "ALERT";
                case 2:
                    return "HISTORY";
                case 3:
                    return "STATISTICS";
            }
            return null;
        }
    }

    /*******************************************Added from SmartFall*********************************************/
    private Timer checkImmobile = new Timer();
    private TimerTask ok;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private final int MAX_RECORDS = 200;
    private final int NUM_FALL_THRESHOLD = 16;
    private final double FALL_MAG_THRESHOLD = 35;
    private final int REST_THRESHOLD = 20;

    private int currRecordInd;
    private int accel_count; // fall occurs if accel_count >= NUM_ACCEL_THRESHOLD
    private int idle_count;
    private boolean cycle;

    private float[] accel_data;
    //private float[] accel_diff;

    private boolean isAYOActive;



    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override

    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        isAYOActive = false;



        currRecordInd = 0;
        accel_count = 0;
        cycle = false;
        idle_count = 0;

        accel_data = new float[MAX_RECORDS];
    }

    protected void onPause() {
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // leave this method empty, don't delete (required for implementing interface)
    }

    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;

        float mSensorX = event.values[0];
        float mSensorY = event.values[1];
        float mSensorZ = event.values[2];

        // for debugging
//        TextView tv = (TextView) findViewById(R.id.accelerometer_values);

        // big loop for checking threshold begins here

        // 1) get new accelerometer reading
        float accelValue = mSensorX*mSensorX + mSensorY*mSensorY + mSensorZ*mSensorZ;

        // 2) record accelerometer difference, then increment currRecordInd
        if(currRecordInd != 0) { // if not the very first record

            // 3) update accel_count
            // check if in cycle, and if so if existing record is above or below DIFF_THRESHOLD as well
            boolean newRecordTap = accelValue < FALL_MAG_THRESHOLD;
            boolean oldRecordTap = accel_data[currRecordInd] < FALL_MAG_THRESHOLD;
            if (newRecordTap) {
                //boolean oldRecordTap = accel_diff[(currRecordInd + MAX_RECORDS - 1) % MAX_RECORDS] < FALL_THRESHOLD;
                if(!oldRecordTap || !cycle) {
                    accel_count++;
                }
                idle_count = 0;
            }
            else {
                //boolean oldRecordTap = accel_diff[(currRecordInd + MAX_RECORDS - 1) % MAX_RECORDS] < FALL_THRESHOLD;
                if(oldRecordTap && cycle) {
                    accel_count--;
                }
                idle_count++;
                if(idle_count >= REST_THRESHOLD)
                    accel_count = Math.max(0, accel_count - 2);
            }
        }
        accel_data[currRecordInd] = accelValue;
        currRecordInd = (currRecordInd + 1) % MAX_RECORDS;

//        if(currRecordInd == 0)
//            tv.setText("");

//		tv.setText(tv.getText() + "   " + accel_count);

        // 4) check if accel_count threshold is met, if so switch activity
        if(accel_count >= NUM_FALL_THRESHOLD) {
            //Need to check if the "are you okay is already called"
            if (!isAYOActive){
                isAYOActive = true;
                Intent verification = new Intent(this, Verification.class);
                startActivity(verification);
                currRecordInd++; //Remove this line IF text of Accelerometer is different.
            }
        }
    }

    public void onSettingsButtonClick(View v) {
        Intent settingsIntent = new Intent(this, EditTemplate.class);
        startActivity(settingsIntent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) { //Whenever ANYTHING is pressed!

        if(ok != null)
            ok.cancel();

        ok = new TimerTask()
        {
            public void run()
            {
                int from = 100;
                int to = 601;
                Calendar c = Calendar.getInstance();
                int t = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
                if(t < from && t > to) {
                    Intent notif = new Intent(MainActivity.this, Verification.class);
                    startActivity(notif);
                }
                else dispatchTouchEvent(null); //Resets timer if sleeping
            }
        };
        if(event == null) { //If sleeping, sets timer to 10:00am
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 10);
            c.set(Calendar.MINUTE,0);
            c.set(Calendar.SECOND,0);
            checkImmobile.schedule(ok,c.getTime());
        }
        else checkImmobile.schedule(ok,14400000); //4 Hours == 14400000


        return super.dispatchTouchEvent(event);  //Allows event to continue propagating
    }

    /*
    @Override
	public boolean onTouchEvent(MotionEvent event) //Whenever anything is pressed
	{
    	if(ok != null)
    		ok.cancel();

    	ok = new TimerTask()
    	{
			public void run()
			{
				int from = 100;
		    	int to = 601;
		    	Calendar c = Calendar.getInstance();
		    	int t = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
		    	if(t < from && t > to) {
		    		//Intent notif = new Intent(MainActivity.this, Verification.class);
		    		//startActivity(notif);
		    	}
		    	else onTouchEvent(null); //Resets timer if sleeping
			}
		};
		if(event == null) { //If sleeping, sets timer to 10:00am
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 10);
			c.set(Calendar.MINUTE,0);
			c.set(Calendar.SECOND,0);
			checkImmobile.schedule(ok,c.getTime());
		}
		else checkImmobile.schedule(ok,5000); //4 Hours == 14400000
		return false; //Allows event to continue propagating
	}
	*/

}
