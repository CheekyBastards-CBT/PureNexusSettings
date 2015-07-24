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
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.purenexussettings.utils.SlidingTabLayout;
import com.android.purenexussettings.utils.ViewPagerAdapter;

public class AppPickerFragment extends Fragment {

    ViewPager pager;
    SlidingTabLayout slidingTabLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.slidetab, container, false);
        // slide menu items
        String[] tabTitles = getResources().getStringArray(R.array.app_pick_items);
        String[] tabFrags = getResources().getStringArray(R.array.apppick_tab_fragments);

        pager = (ViewPager) v.findViewById(R.id.viewpager);
        pager.setOffscreenPageLimit(2);

        slidingTabLayout = (SlidingTabLayout) v.findViewById(R.id.sliding_tabs);
        slidingTabLayout.setInitFrag(this);

        pager.setAdapter(new ViewPagerAdapter(getChildFragmentManager(), tabTitles, tabFrags));

        slidingTabLayout.setViewPager(pager);

        slidingTabLayout.setSelectedIndicatorColors(Color.WHITE);
        slidingTabLayout.setDistributeEvenly(false);

        return v;
    }
}
