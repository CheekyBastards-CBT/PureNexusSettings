/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
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

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class ScreenRecorderFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private ContentResolver resolver;
    private static final String KEY_VIDEO_SIZE = "screen_recorder_size";
    private static final String KEY_VIDEO_BITRATE = "screen_recorder_bitrate";
    private static final String KEY_RECORD_AUDIO = "screen_recorder_record_audio";
    public static final String SCREEN_RECORDER_OUTPUT_DIMENSIONS = "screen_recorder_output_dimensions";
    public static final String SCREEN_RECORDER_BITRATE = "screen_recorder_bitrate";
    public static final String SCREEN_RECORDER_RECORD_AUDIO = "screen_recorder_record_audio";


    private ListPreference mVideoSizePref;
    private ListPreference mVideoBitratePref;
    private SwitchPreference mRecordAudioPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.screenrecord_fragment);

        //perhaps change to get...ForUser methods?

        resolver = getActivity().getContentResolver();
        mVideoSizePref = (ListPreference) findPreference(KEY_VIDEO_SIZE);
        mVideoSizePref.setOnPreferenceChangeListener(this);
        String size = Settings.System.getString(resolver, SCREEN_RECORDER_OUTPUT_DIMENSIONS);
        updateVideoSizePreference(size);

        mVideoBitratePref = (ListPreference) findPreference(KEY_VIDEO_BITRATE);
        mVideoBitratePref.setOnPreferenceChangeListener(this);
        String rate= Settings.System.getString(resolver, SCREEN_RECORDER_BITRATE);
        updateVideoBitratePreference(rate);

        mRecordAudioPref = (SwitchPreference) findPreference(KEY_RECORD_AUDIO);
        mRecordAudioPref.setChecked(Settings.System.getInt(resolver, SCREEN_RECORDER_RECORD_AUDIO, 0) == 1);
        mRecordAudioPref.setOnPreferenceChangeListener(this);
        if (!hasMicrophone()) getPreferenceScreen().removePreference(mRecordAudioPref);
    }

    public ScreenRecorderFragment() {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference) {
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mVideoSizePref) {
            updateVideoSizePreference((String) o);
            return true;
        } else if (preference == mVideoBitratePref) {
            updateVideoBitratePreference((String) o);
            return true;
        } else if (preference == mRecordAudioPref) {
            Settings.System.putInt(resolver, SCREEN_RECORDER_RECORD_AUDIO, Boolean.TRUE.equals(o) ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateVideoSizePreference(String value) {
        if (TextUtils.isEmpty(value)) value = getString(R.string.screen_recorder_size_720x1280);
        mVideoSizePref.setSummary(mVideoSizePref.getEntries()[mVideoSizePref.findIndexOfValue(value)]);
        Settings.System.putString(resolver, SCREEN_RECORDER_OUTPUT_DIMENSIONS, value);
    }

    private void updateVideoBitratePreference(String value) {
        if (TextUtils.isEmpty(value)) value = getString(R.string.screen_recorder_bitrate_4000000);
        mVideoBitratePref.setSummary(mVideoBitratePref.getEntries()[mVideoBitratePref.findIndexOfValue(value)]);
        Settings.System.putInt(resolver, SCREEN_RECORDER_BITRATE, Integer.valueOf(value));
    }

    private boolean hasMicrophone() {
        return getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }
}
