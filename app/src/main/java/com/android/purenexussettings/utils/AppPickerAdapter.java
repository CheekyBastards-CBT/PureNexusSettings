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

package com.android.purenexussettings.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.purenexussettings.R;
import com.android.purenexussettings.TinkerActivity;

import java.util.ArrayList;
import java.util.List;


public class AppPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<ApplicationInfo> packageList;
    List<ResolveInfo> shortcutList;
    ArrayList<Intent> intentList;
    Activity context;
    PackageManager packageManager;
    int extratitle;
    int extraicon;
    int xtracount;
    final static int APPPICKER_APP = 1;
    final static int APPPICKER_SHORT = 2;
    final static int APPPICKER_CUSTOM = 3;
    final static int REQUEST_CREATE_SHORTCUT = 3;
    Handler myHandler = new Handler();

    public AppPickerAdapter(Activity context, PackageManager packageManager, List<ApplicationInfo> packageList, List<ResolveInfo> shortcutList, ArrayList<Intent> shortIntentList, int titles, int icons) {

        this.context = context;
        this.packageList = packageList;
        this.shortcutList = shortcutList;
        this.intentList = shortIntentList;
        this.packageManager = packageManager;

        if (titles != 0 && icons != 0) {
            extratitle = titles;
            extraicon = icons;
            TypedArray a = context.getResources().obtainTypedArray(titles);
            this.xtracount = a.length();
            a.recycle();
        } else {
            this.extratitle = 0;
            this.extraicon = 0;
            this.xtracount = 0;
        }
    }

    private class AppPickItem extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView apkName;
        private int position;
        private int sourcetype;

        public AppPickItem(View itemView) {
            super(itemView);
            apkName = (TextView) itemView.findViewById(R.id.appname);
            LinearLayout cardClick = (LinearLayout) itemView.findViewById(R.id.clicklayout);
            cardClick.setOnClickListener(this);
        }

        public void bindPickInfo(int pos, int src) {
            this.position = pos;
            this.sourcetype = src;
        }

        @Override
        public void onClick(View v) {
            switch (this.sourcetype) {
                case APPPICKER_APP:
                    myHandler.removeCallbacksAndMessages(null);
                    String target = packageManager.getLaunchIntentForPackage(((ApplicationInfo)getItem(this.position)).packageName).toUri(0);
                    Settings.Global.putString(context.getContentResolver(), TinkerActivity.mPrefKey, target);
                    myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((TinkerActivity) context).onBackPressed();
                        }
                    }, 400);
                    return;
                case APPPICKER_SHORT:
                    myHandler.removeCallbacksAndMessages(null);
                    myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            context.startActivityForResult(intentList.get(position), REQUEST_CREATE_SHORTCUT);
                        }
                    }, 400);
                    return;
                case APPPICKER_CUSTOM:
                    myHandler.removeCallbacksAndMessages(null);
                    String mEntry = null;
                    String[] mKeys = context.getResources().getStringArray(TinkerActivity.mKeyArray);

                    switch (this.position) {
                        case 0:
                            mEntry = mKeys[this.position];
                            break;
                        case 1:
                            Intent fragintent = new Intent(context, TinkerActivity.class);
                            fragintent.putExtra(TinkerActivity.EXTRA_START_FRAGMENT, 0);
                            fragintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);

                            mEntry = fragintent.toUri(0);
                            break;
                        case 2:
                            mEntry = mKeys[position];
                            break;
                        case 3:
                            mEntry = mKeys[position];
                            break;
                        default:
                    }

                    Settings.Global.putString(context.getContentResolver(), TinkerActivity.mPrefKey, mEntry);
                    myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((TinkerActivity) context).onBackPressed();
                        }
                    }, 400);
                    return;
                default:
            }
        }
    }

    @Override
    public AppPickItem onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recyclerview_app_item, viewGroup, false);

        return new AppPickItem(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder picked, int i) {
        Drawable appIcon = null;
        String appName = null;
        AppPickItem pickedItem = (AppPickItem)picked;

        if (packageList != null) {
            ApplicationInfo appInfo = (ApplicationInfo) getItem(i);
            appIcon = packageManager.getApplicationIcon(appInfo);
            appName = packageManager.getApplicationLabel(appInfo).toString();
            pickedItem.bindPickInfo(i, APPPICKER_APP);
        } else if (shortcutList != null) {
            ResolveInfo shortInfo = (ResolveInfo) getItem(i);
            appIcon = shortInfo.loadIcon(packageManager);
            appName = shortInfo.loadLabel(packageManager).toString();
            pickedItem.bindPickInfo(i, APPPICKER_SHORT);
        } else if (xtracount > 0) {
            TypedArray title = context.getResources().obtainTypedArray(extratitle);
            TypedArray icon = context.getResources().obtainTypedArray(extraicon);

            appName = context.getString(title.getResourceId(i, -1));
            appIcon = context.getDrawable(icon.getResourceId(i, -1));

            pickedItem.bindPickInfo(i, APPPICKER_CUSTOM);

            icon.recycle();
            title.recycle();
        }

        if (appIcon != null) {
            int bound = (int)(context.getResources().getDimension(R.dimen.apppicker_cardview_height) - (5 * context.getResources().getDisplayMetrics().density));
            appIcon.setBounds(0, 0, bound, bound); //try to shave a bit off the bound to appear padded
        }

        pickedItem.apkName.setCompoundDrawables(appIcon, null, null, null);
        pickedItem.apkName.setCompoundDrawablePadding((int)(10 * context.getResources().getDisplayMetrics().density));
        pickedItem.apkName.setText(appName);
    }

    @Override
    public int getItemCount() {
        if (packageList != null) {
            return packageList.size();
        } else if (shortcutList != null) {
            return shortcutList.size();
        } else {
            return xtracount;
        }
    }

    public Object getItem(int position) {
        if (packageList != null) {
            return packageList.get(position);
        } else if (shortcutList != null) {
            return shortcutList.get(position);
        } else {
            return null;
        }
    }
}
