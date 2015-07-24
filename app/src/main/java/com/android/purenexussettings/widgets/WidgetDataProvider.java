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

package com.android.purenexussettings.widgets;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.android.purenexussettings.R;
import com.android.purenexussettings.TinkerActivity;
import com.android.purenexussettings.utils.NavDrawerItem;

import java.util.ArrayList;

public class WidgetDataProvider implements RemoteViewsFactory {
    ArrayList<NavDrawerItem> mCollections = new ArrayList<>();
    Context mContext = null;

    private String[] navMenuCats;
    private String[] navMenuCatItems;


    public WidgetDataProvider(Context context, Intent intent) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mCollections.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews mView = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

        // set title text
        mView.setTextViewText(R.id.title, mCollections.get(position).getTitle());

        // remove imageview if no image there
        if (mCollections.get(position).getIcon() != -1) {
            mView.setViewVisibility(R.id.icon, View.VISIBLE);
            mView.setImageViewResource(R.id.icon, mCollections.get(position).getIcon());
        } else {
            mView.setViewVisibility(R.id.icon, View.GONE);
        }

        // remove category view - default
        mView.setViewVisibility(R.id.category, View.GONE);

        // add in category bits if needed
        for (int i = 0; i < navMenuCats.length; i++) {
            if ( mCollections.get(position).getTitle().equals(navMenuCatItems[i]) ) {
                mView.setTextViewText(R.id.category, navMenuCats[i]);
                mView.setViewVisibility(R.id.category, View.VISIBLE);
            }
        }

        // set up connected intent
        final Intent fillInIntent = new Intent();
        fillInIntent.setAction(WidgetProvider.FRAG_START);

        position += TinkerActivity.FRAG_ARRAY_START;

        final Bundle bundle = new Bundle();
        bundle.putInt(WidgetProvider.EXTRA_STRING, position);
        fillInIntent.putExtras(bundle);
        mView.setOnClickFillInIntent(R.id.backgroundImage, fillInIntent);

        return mView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
        initData();
    }

    @Override
    public void onDataSetChanged() {
        initData();
    }

    private void initData() {
        boolean cLockInstalled;
        mCollections.clear();

        // nav drawer items from resources
        String[] navMenuTitles = mContext.getResources().getStringArray(R.array.nav_drawer_items);

        // nav drawer icons from resources
        TypedArray navMenuIcons = mContext.getResources().obtainTypedArray(R.array.nav_drawer_icons);

        // check if cLock installed
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo(TinkerActivity.KEY_LOCK_CLOCK_PACKAGE_NAME, 0);
            cLockInstalled = pi.applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            cLockInstalled = false;
        }

        // adding nav drawer items to array - start at i=FRAG_ARRAY_START to skip items not in the navdrawer
        for (int i= TinkerActivity.FRAG_ARRAY_START; i < navMenuTitles.length; i++) {
            // skip cLock if not installed
            if ( !(navMenuTitles[i].equals("cLock") && !cLockInstalled) ) {
                mCollections.add(new NavDrawerItem(navMenuTitles[i], navMenuIcons.getResourceId(i, -1)));
            }
        }

        // Recycle the typed array
        navMenuIcons.recycle();

        // Pull in info on categories - names and what entries they should attach to
        navMenuCats = mContext.getResources().getStringArray(R.array.nav_drawer_cats);
        navMenuCatItems = mContext.getResources().getStringArray(R.array.nav_drawer_cat_items);
    }

    @Override
    public void onDestroy() {
    }
}
