/*
 * Copyright (C) 2015 The Pure Nexus Project
 * Heavily borrowed from terkinarslan material sample
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

package com.android.purenexussettings.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.android.purenexussettings.TinkerActivity;


public class ViewPagerAdapter extends FragmentPagerAdapter {

    private String titles[] ;
    private String frags[] ;
    private String mPackageName;

    public ViewPagerAdapter(FragmentManager fm, String[] titles2, String[] frags2) {
        super(fm);
        titles=titles2;
        frags=frags2;
        mPackageName = TinkerActivity.mPackageName;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment frag = null;
        String fragname = frags[position];
        try {
            frag = (Fragment)Class.forName(mPackageName + "." + fragname).newInstance();
        }
        catch (Exception e) {
            frag = null;
        }

        return frag;
    }

    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    @Override
    public int getCount() {
        return frags.length;
    }

}