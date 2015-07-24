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

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.content.ContentResolver;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.NonNull;

public class NavigationBarFragment extends PreferenceFragment implements OnPreferenceChangeListener {
    private static final String NAVIGATION_BAR_DIMEN = "navigation_bar_dimen";

    // kill-app long press back
    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    // Navigation bar left
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";

    private static final String CATEGORY_NAVBAR = "navigation_bar";

    // kill-app long press back
    private SwitchPreference mKillAppLongPressBack;

    private Preference mNavDimen;

    private ContentResolver resolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.navbar_fragment_prefs);

        resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory navbarCategory = (PreferenceCategory) prefScreen.findPreference(CATEGORY_NAVBAR);

        mNavDimen = prefScreen.findPreference(NAVIGATION_BAR_DIMEN);

        // kill-app long press back
        mKillAppLongPressBack = (SwitchPreference) findPreference(KILL_APP_LONGPRESS_BACK);
        mKillAppLongPressBack.setOnPreferenceChangeListener(this);
        int killAppLongPressBack = Settings.Secure.getInt(resolver, KILL_APP_LONGPRESS_BACK, 0);
        mKillAppLongPressBack.setChecked(killAppLongPressBack != 0);


        // remove if tablet
        if ((getActivity().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            navbarCategory.removePreference(findPreference(KEY_NAVIGATION_BAR_LEFT));
        }

    }

    public NavigationBarFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {

        // kill-app long press back
        if (preference == mKillAppLongPressBack) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(resolver, KILL_APP_LONGPRESS_BACK, value ? 1 : 0);
            return true;
        }
        return false;
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, @NonNull Preference pref) {
        if (pref == mNavDimen) {
            ((TinkerActivity)getActivity()).displayNavDimen();

            return true;
        }

        return false;
    }
}
