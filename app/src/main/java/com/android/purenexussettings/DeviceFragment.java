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
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;

public class DeviceFragment extends PreferenceFragment {
    private static final String RADIO_INFO = "radioinfo";
    private static final String BUILDPROPEDITOR = "buildpropeditor";

    private Preference mBuildProp;

    public DeviceFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.device_fragment);

        Preference mRadioInfo = (Preference)findPreference(RADIO_INFO);
        mBuildProp = (Preference)findPreference(BUILDPROPEDITOR);

        PreferenceScreen prefScreen = getPreferenceScreen();

        // remove the radioinfo pref if tablet
        if ( (getActivity().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE ) {
            prefScreen.removePreference(mRadioInfo);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, @NonNull Preference pref) {
        if (pref == mBuildProp) {
            ((TinkerActivity)getActivity()).displayBuildPropEditor();

            return true;
        }

        return false;
    }
}
