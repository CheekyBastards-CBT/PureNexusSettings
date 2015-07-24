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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class NavBarEdit extends Fragment implements View.OnClickListener {

    private LinearLayout mRestore, mSave, mEdit;
    private boolean mEditMode;
    private static final String NAV_BUTTONS = "nav_buttons";
    private Activity mActivity;
    private final static Intent mIntent = new Intent("android.intent.action.NAVBAR_EDIT");

    public NavBarEdit(){}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.navbaredit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mEdit = (LinearLayout) view.findViewById(R.id.navbar_edit);
        mEdit.setOnClickListener(this);
        mSave = (LinearLayout) view.findViewById(R.id.navbar_save);
        mSave.setOnClickListener(this);
        mRestore = (LinearLayout) view.findViewById(R.id.navbar_restore);
        mRestore.setOnClickListener(this);
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        toggleEditMode(false, false);
    }

    @Override
    public void onClick(View v) {
        if (v == mEdit) {
            mEditMode = !mEditMode;
            toggleEditMode(mEditMode, false);
        } else if (v == mSave) {
            mEditMode = !mEditMode;
            toggleEditMode(mEditMode, true);
        } else if (v == mRestore) {
            new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.reset)
                    .setIcon(R.drawable.ic_navbar_restore)
                    .setMessage(R.string.navigation_bar_reset_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (mEditMode) {
                                toggleEditMode(false, false);
                            }
                            Settings.System.putString(getActivity().getContentResolver(), NAV_BUTTONS, null);
                            toggleEditMode(true, false);
                            toggleEditMode(false, false);
                            mEditMode = false;
                        }
                    }).setNegativeButton(R.string.cancelprop, null)
                    .create().show();
        }
    }

    /**
     * Toggles navbar edit mode
     * @param on True to enter edit mode / false to exit
     * @param save True to save changes / false to discard them
     */
    private void toggleEditMode(boolean on, boolean save) {
        mIntent.putExtra("edit", on);
        mIntent.putExtra("save", save);
        mActivity.sendBroadcast(mIntent);
        if (on) {
            TinkerActivity.lockCurrentOrientation(mActivity);
        } else {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        toggleEditSaveViews(on);
    }

    private void toggleEditSaveViews(boolean on) {
        mEdit.setVisibility(!on ? View.VISIBLE : View.GONE);
        mSave.setVisibility(on ? View.VISIBLE : View.GONE);
    }
}