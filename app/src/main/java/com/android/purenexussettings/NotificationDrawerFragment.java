/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.purenexussettings;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.android.purenexussettings.qs.QSTiles;

public class NotificationDrawerFragment extends PreferenceFragment implements OnPreferenceChangeListener {
    private static final String QS_QUICK_PULLDOWN = "qs_quick_pulldown";
    private static final String QUICK_PULLDOWN = "quick_pulldown";
    private static final String QS_ORDER = "qs_order";

    private Preference mQSTiles;
    private ListPreference mQuickPulldown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notidrawer_fragment);

        PreferenceScreen prefSet = getPreferenceScreen();
        mQSTiles = prefSet.findPreference(QS_ORDER);

        ContentResolver resolver = getActivity().getContentResolver();
        mQuickPulldown = (ListPreference) prefSet.findPreference(QUICK_PULLDOWN);

        mQuickPulldown.setOnPreferenceChangeListener(this);
        int quickPulldownValue = Settings.System.getIntForUser(resolver, QS_QUICK_PULLDOWN, 0, UserHandle.USER_CURRENT);
        mQuickPulldown.setValue(String.valueOf(quickPulldownValue));
        updatePulldownSummary(quickPulldownValue);

    }

    public NotificationDrawerFragment(){}

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference) {
        if (preference == mQSTiles) {
            ((TinkerActivity)getActivity()).displayQSTile();
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, QS_QUICK_PULLDOWN, quickPulldownValue, UserHandle.USER_CURRENT);
            updatePulldownSummary(quickPulldownValue);
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        int qsTileCount = QSTiles.determineTileCount(getActivity());
        mQSTiles.setSummary(getResources().getQuantityString(R.plurals.qs_tiles_summary, qsTileCount, qsTileCount));
    }

    private void updatePulldownSummary(int value) {
        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(getString(R.string.quick_pulldown_off));
        } else {
            String direction = getString(value == 2 ? R.string.quick_pulldown_summary_left : R.string.quick_pulldown_summary_right);
            mQuickPulldown.setSummary(getString(R.string.quick_pulldown_summary, direction));
        }
    }
}
