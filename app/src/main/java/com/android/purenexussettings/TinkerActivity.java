/*
 * Copyright (C) 2015 The Pure Nexus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.purenexussettings;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.purenexussettings.utils.NavDrawerItem;
import com.android.purenexussettings.utils.NavDrawerListAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import eu.chainfire.libsuperuser.Shell;

public class TinkerActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    public static String mPackageName;
    private FragmentManager fragmentManager;
    // stuff for widget calls to open fragments
    public static final String EXTRA_START_FRAGMENT = "com.android.purenexussettings.tinkerings.EXTRA_START_FRAGMENT";
    public static final int REQUEST_CREATE_SHORTCUT = 3;
    // this allows first # entries in stringarray to be skipped from navdrawer/widget
    public static int FRAG_ARRAY_START = 4;

    public static final String KEY_LOCK_CLOCK_PACKAGE_NAME = "com.cyanogenmod.lockclock";

    // example - used to retain slidetab position
    public static int LAST_SLIDE_BAR_TAB;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title/position
    private CharSequence mTitle;
    private int mItemPosition;

    // for backstack tracking
    private Stack<String> fragmentStack;

    // various bools for this or that
    private boolean mBackPress;
    private boolean mIgnoreBack;
    private boolean mFromClick;
    private boolean mIgnore;
    private boolean mMenu;
    private boolean fullyClosed;
    private boolean openingHalf;

    // slide menu items
    private String[] navMenuTitles;
    private String[] navMenuFrags;

    // info for buildprop editor
    public static String mEditName;
    public static String mEditKey;

    // info for app picker
    public static String mPrefKey;
    public static int mTitleArray;
    public static int mIconArray;
    public static int mKeyArray;

    // For handling quick back/About presses
    Handler myHandler = new Handler();

    private class Startup extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Context context = null;
        private boolean suAvailable = false;

        public Startup setContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        protected void onPreExecute() {
            // The progress dialog here is so the user will wait until root stuff is figured out.

            dialog = new ProgressDialog(context);
            dialog.setTitle("Hold on a sec");
            dialog.setMessage("Trying to find root...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);

            // A semi-hack way to prevent FCs when orientation changes during progress dialog showing
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                ((TinkerActivity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                ((TinkerActivity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Let's check to see if SU is there...
            suAvailable = Shell.SU.available();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ((TinkerActivity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            dialog.dismiss();

            // output - maybe make this a snackbar thing...?
            if (!suAvailable) {
                String str = "Root not available?!? Some functions may not work as intended!";
                Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tinker);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set up some defaults
        boolean cLockInstalled;
        mTitle = mDrawerTitle = getTitle();
        mPackageName = getPackageName();
        LAST_SLIDE_BAR_TAB = 0;
        mBackPress = false;
        mIgnoreBack = false;
        mFromClick = false;
        mMenu = false;
        fullyClosed = true;
        openingHalf = true;

        // for backstack tracking
        fragmentStack = new Stack<String>();

        // load slide menu items
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);
        navMenuFrags = getResources().getStringArray(R.array.nav_drawer_fragments);

        // nav drawer icons from resources
        TypedArray navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
        ArrayList<NavDrawerItem> navDrawerItems = new ArrayList<NavDrawerItem>();

        // check if cLock installed
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(KEY_LOCK_CLOCK_PACKAGE_NAME, 0);
            cLockInstalled = pi.applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            cLockInstalled = false;
        }

        List<String> tempTitleList = new LinkedList<>(Arrays.asList(navMenuTitles));
        List<String> tempFragList = new LinkedList<>(Arrays.asList(navMenuFrags));

        // adding nav drawer items to array - skip cLock if package not installed
        for (int i=FRAG_ARRAY_START; i < navMenuTitles.length; i++) {
            if ( !(navMenuTitles[i].equals("cLock") && !cLockInstalled) ) {
                navDrawerItems.add(new NavDrawerItem(navMenuTitles[i], navMenuIcons.getResourceId(i, -1)));
            } else {
                // Sets up removing bits that are skipped from string array
                // CAUTION: This will remove FIRST entry that matches!
                tempTitleList.remove(navMenuTitles[i]);
                tempFragList.remove(navMenuFrags[i]);
            }
        }

        // repopulate string arrays in case anything was removed
        navMenuTitles = tempTitleList.toArray(new String[tempTitleList.size()]);
        navMenuFrags = tempFragList.toArray(new String[tempFragList.size()]);

        // Recycle the typed array
        navMenuIcons.recycle();

        // Pull in info on categories - names and what entries they should attach to
        String[] navMenuCats = getResources().getStringArray(R.array.nav_drawer_cats);
        String[] navMenuCatItems = getResources().getStringArray(R.array.nav_drawer_cat_items);

        // setting the nav drawer list adapter
        NavDrawerListAdapter adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems, navMenuCats, navMenuCatItems);
        mDrawerList.setAdapter(adapter);

        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                toolbar, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ){
            @Override
            public void onDrawerClosed(View view) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerList);
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                openingHalf = true;
                invalidateOptionsMenu();
                // now that the drawer animation is done - load fragment
                if (mIgnore || !mFromClick ) {
                    mIgnore = false;
                } else {
                    displayView(mItemPosition);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                openingHalf = false;
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                fullyClosed = (slideOffset == 0.0f);
                if (slideOffset < 0.5f && !openingHalf) {
                    openingHalf = true;
                    invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                } else if (slideOffset > 0.5f && openingHalf) {
                    openingHalf = false;
                    invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
                }
            }

        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

        fragmentManager = getFragmentManager();

        // check for root stuff
        (new Startup()).setContext(this).execute();

        if (savedInstanceState == null) {
            // on first time display view for first nav item
            displayView(mItemPosition = getIntent().getIntExtra(EXTRA_START_FRAGMENT, 0));
        } else if (savedInstanceState.getSerializable("fragstack") != null) {
            try {
                // brings in the pre-orientation change stack list
                fragmentStack = (Stack<String>) savedInstanceState.getSerializable("fragstack");
                mItemPosition = (int) savedInstanceState.getSerializable("currpos");
                setTitle(navMenuTitles[mItemPosition]);
            } catch (Exception e) {}
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // not sure serializable is the way to go... but it works for now
        // lets the stack list stick around after orientation change
        outState.putSerializable("fragstack", fragmentStack);
        // toss in position number for title update
        outState.putSerializable("currpos", mItemPosition);
    }

    /* Slide menu item click listener */
    private class SlideMenuClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // check for change in position, if changed set selected/title remove frag then close drawer
            int origposition = position;
            position += FRAG_ARRAY_START;
            if (mItemPosition == position) {
                mIgnore = true;
            } else {
                //check for external app launching navdrawer items
                if ( navMenuTitles[position].equals("App Ops") ) {
                    mIgnore = true;
                    launchAppOps();
                }
                if ( navMenuTitles[position].equals("cLock") ) {
                    mIgnore = true;
                    launchcLock();
                }

                // if nothing was caught in the above, do the usual prep to show frag stuff
                if (!mIgnore) {
                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerList);
                    mFromClick = true;
                    mDrawerList.setItemChecked(origposition, true);
                    mDrawerList.setSelection(origposition);
                    mItemPosition = position;
                    setTitle(navMenuTitles[position]);
                    removeCurrent();
                }
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    /* Displaying fragment view for selected nav drawer list item */
    private void displayView(int position) {
        // before anything else - check to see if position matches intent-launching "frags"
        if ( navMenuTitles[position].equals("App Ops") ) {
            position = 0;
            launchAppOps();
        }
        if ( navMenuTitles[position].equals("cLock") ) {
            position = 0;
            launchcLock();
        }

        // update the main content by replacing fragments
        Fragment frags = null;
        String fragname = navMenuFrags[position];
        try {
            frags = (Fragment)Class.forName(mPackageName + "." + fragname).newInstance();
            }
        catch (Exception e) {
            frags = null;
            }
        if (frags != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            FragmentTransaction fragtrans = fragmentManager.beginTransaction();
            if (mFromClick || mMenu || mBackPress) {
                fragtrans.setCustomAnimations(R.anim.fadein, R.anim.fadeout, R.anim.fadein, R.anim.fadeout);
            }
            fragtrans.add(R.id.frame_container, frags);
            // add fragment name to custom stack for backstack tracking
            if (!mBackPress && position != 2 && position != 3) { //Avoid adding EditProp or AppPicker fragment to stack
                fragmentStack.push(navMenuFrags[position]);
            }
            fragtrans.commit();

            // update selected item and title, then close the drawer
            if (mFromClick || mBackPress) {
                mFromClick = false;
                mBackPress = false;
            } else {
                if (position < FRAG_ARRAY_START) {
                    mDrawerList.clearChoices();
                    for (int i = 0; i < mDrawerList.getCount(); i++)
                        mDrawerList.setItemChecked(i, false);
                } else {
                    // only stuff that makes it in here stems from widget or back stack
                    mDrawerList.setItemChecked(position - FRAG_ARRAY_START, true);
                    mDrawerList.setSelection(position - FRAG_ARRAY_START);
                }
                setTitle(navMenuTitles[position]);
                if (mMenu) {
                    mMenu = false;
                    mItemPosition = position;
                } else {
                    mDrawerLayout.closeDrawer(mDrawerList);
                }
            }
            invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
        } else {
            // error in creating fragment
            Log.e("TinkerActivity", "Error in creating fragment");
        }
    }

    private void removeCurrent() {
        // update the main content by replacing fragments, first by removing the old
        FragmentTransaction fragtrans = fragmentManager.beginTransaction();
        fragtrans.setCustomAnimations(R.anim.fadein,R.anim.fadeout,R.anim.fadein,R.anim.fadeout);
        fragtrans.remove(fragmentManager.findFragmentById(R.id.frame_container));
        fragtrans.commit();
    }

    private void backup() {
        try {
            Shell.SU.run("cp -f /system/build.prop " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/build.prop.bak");
            Toast.makeText(getApplicationContext(), "build.prop Backup at " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/build.prop.bak", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void displayBuildPropEditor() {
        myHandler.removeCallbacksAndMessages(null);
        mMenu = true;
        removeCurrent();
        // below replicates the visual delay seen when launching frags from navdrawer
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayView(1);
            }
        }, 400);
    }

    public void displayAppPicker(Preference object, int titles, int icons, int keys) {
        // stuff for apppicker fragment
        mPrefKey = object.getKey();
        mTitleArray = titles;
        mIconArray = icons;
        mKeyArray = keys;

        myHandler.removeCallbacksAndMessages(null);
        mMenu = true;
        removeCurrent();
        // below replicates the visual delay seen when launching frags from navdrawer
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayView(3);
            }
        }, 400);
    }

    public void displayEditProp(String name, String key) {
        //put the name and key strings in here for editprop access
        mEditName = name;
        mEditKey = key;

        myHandler.removeCallbacksAndMessages(null);
        mMenu = true;
        removeCurrent();
        // below replicates the visual delay seen when launching frags from navdrawer
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                displayView(2);
            }
        }, 400);
    }

    public void launchAppOps() {
        Intent link = new Intent();
        ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.SubSettings");
        link.setComponent(cn);
        link.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "com.android.settings.applications.AppOpsSummary");
        link.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(link);
    }

    public void launchcLock() {
        Intent link = new Intent(Intent.ACTION_MAIN);
        ComponentName cn = new ComponentName("com.cyanogenmod.lockclock", "com.cyanogenmod.lockclock.preference.Preferences");
        link.setComponent(cn);
        startActivity(link);
    }

    @Override
    public void onBackPressed() {
        if (!fullyClosed) {
            // backpress closes drawer if open
            mDrawerLayout.closeDrawer(mDrawerList);
        } else if (fragmentStack.size() > 1) {
            if (!mIgnoreBack) {
                mIgnoreBack = true;

                // cancels any pending postdelays just in case
                myHandler.removeCallbacksAndMessages(null);

                // removes latest (current) entry in custom stack
                if (mItemPosition != 2 && mItemPosition != 3) { //but not for editprop or apppicker
                    fragmentStack.pop();
                }
                // uses fragment name to find displayview-relevant position
                final int position = Arrays.asList(navMenuFrags).indexOf(fragmentStack.lastElement());
                // a setup similar to onclickitem
                mDrawerList.setItemChecked(position - FRAG_ARRAY_START, true);
                mDrawerList.setSelection(position - FRAG_ARRAY_START);
                mItemPosition = position;
                setTitle(navMenuTitles[position]);
                removeCurrent();
                // below replicates the visual delay seen when launching frags from navdrawer
                myHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBackPress = true;
                        displayView(position);
                        mIgnoreBack = false;
                    }
                }, 400);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tinker, menu);
        menu.findItem(R.id.action_launchhide).setChecked(!isLauncherIconEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_about:
                if ( mItemPosition != 0 ) {
                    myHandler.removeCallbacksAndMessages(null);
                    mMenu = true;
                    removeCurrent();
                    // below replicates the visual delay seen when launching frags from navdrawer
                    myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            displayView(0);
                        }
                    }, 400);
                }
                return true;
            case R.id.action_launchhide:
                boolean checked = item.isChecked();
                item.setChecked(!checked);
                setLauncherIconEnabled(checked);
                return true;
            case R.id.action_backup:
                backup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Called when invalidateOptionsMenu() is triggered */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened/opening, hide the action items
        // add in bits to enable/disable menu items that are fragment specific
        if ( openingHalf ) {
            menu.setGroupVisible(R.id.action_items, true);
            boolean isbuildprop = (mItemPosition == 1);
            boolean iseditprop = (mItemPosition == 2);
            boolean isapppicker = (mItemPosition == 3);
            menu.findItem(R.id.action_backup).setVisible(isbuildprop);
            menu.findItem(R.id.action_restore).setVisible(isbuildprop);
            menu.findItem(R.id.action_discard).setVisible(iseditprop);
            menu.findItem(R.id.action_delete).setVisible(iseditprop);
            menu.findItem(R.id.action_launchhide).setVisible(!(isbuildprop || iseditprop || isapppicker));
            menu.findItem(R.id.action_about).setVisible(!(isbuildprop || iseditprop || isapppicker));
        } else {
            menu.setGroupVisible(R.id.action_items, false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /* When using the mDrawerToggle, you must call it during onPostCreate() and onConfigurationChanged()... */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /* Methods related to showing/hiding app from app drawer */
    public void setLauncherIconEnabled(boolean enabled) {
        int newState;
        PackageManager packman = getPackageManager();
        if (enabled) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }
        packman.setComponentEnabledSetting(new ComponentName(this, LauncherActivity.class), newState, PackageManager.DONT_KILL_APP);
    }

    public boolean isLauncherIconEnabled() {
        PackageManager packman = getPackageManager();
        return (packman.getComponentEnabledSetting(new ComponentName(this, LauncherActivity.class)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Override
    protected void onActivityResult(int paramRequest, int paramResult, Intent paramData) {
        super.onActivityResult(paramRequest, paramResult, paramData);
        if (paramResult != -1 || paramData == null) {
            return;
        }
        switch (paramRequest)
        {
            case REQUEST_CREATE_SHORTCUT:
                Intent localIntent = paramData.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                localIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, paramData.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));

                String keystring = localIntent.toUri(0).replaceAll("com.android.contacts.action.QUICK_CONTACT", Intent.ACTION_VIEW);

                Settings.Global.putString(getContentResolver(), TinkerActivity.mPrefKey, keystring);
                onBackPressed();

                return;
            default:
        }
    }
}