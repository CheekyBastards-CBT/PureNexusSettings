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

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.purenexussettings.utils.AppPickerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppPickerShortFragment extends Fragment {

    private class LoadList extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Activity context = null;
        private RecyclerView mList = null;
        private PackageManager packageManager;
        private ArrayList<ResolveInfo> shortcutList = new ArrayList<ResolveInfo>();
        private ArrayList<Intent> intentList = new ArrayList<Intent>();

        public LoadList setInits(Activity context, RecyclerView list, PackageManager pmanager) {
            this.context = context;
            mList = list;
            packageManager = pmanager;
            return this;
        }

        @Override
        protected void onPreExecute() {
            // The progress dialog here is so the user will wait until the prop stuff has loaded

            dialog = new ProgressDialog(context);
            dialog.setTitle("Hold on a sec");
            dialog.setMessage("Loading stuff...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);

            // A semi-hack way to prevent FCs when orientation changes during progress dialog showing
            TinkerActivity.lockCurrentOrientation((TinkerActivity) context);

            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // this loads up the listview - can take a sec

            List<ResolveInfo> shorts = packageManager.queryIntentActivities(new Intent(Intent.ACTION_CREATE_SHORTCUT),0);
            //alphabetizes list
            Collections.sort(shorts, new ResolveInfo.DisplayNameComparator(packageManager));

            for (ResolveInfo info : shorts) {
                shortcutList.add(info);
                // creates intent list with shortcut creating intents
                Intent shortintent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
                shortintent.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name));
                intentList.add(shortintent);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            LinearLayoutManager llm = new LinearLayoutManager(context);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            mList.setLayoutManager(llm);

            mList.setAdapter(new AppPickerAdapter(context, packageManager, null, shortcutList, intentList, 0, 0));

            ((TinkerActivity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            dialog.dismiss();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return (View) inflater.inflate(R.layout.recyclerviewmain, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView listView = (RecyclerView) view.findViewById(R.id.recyclerList);
        (new LoadList()).setInits(getActivity(), listView, getActivity().getPackageManager()).execute();
    }

    public AppPickerShortFragment() {}
}
