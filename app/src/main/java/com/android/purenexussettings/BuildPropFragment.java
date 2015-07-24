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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import eu.chainfire.libsuperuser.Shell;

public class BuildPropFragment extends Fragment {
    private RecyclerView recyclerView;

    private class LoadProp extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Context context = null;
        private RecyclerView mList = null;
        private boolean mTryCatchFail;
        private boolean mIsRestore;
        private Properties prop;
        private String[] pTitle;
        private ArrayList<Map<String, String>> proplist;

        // custom Comparator object that sorts hashmap arraylist by the title entry
        private Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
            public int compare(Map<String, String> m1, Map<String, String> m2) {
                return m1.get("title").compareTo(m2.get("title"));
            }
        };

        public LoadProp setInits(Context context, RecyclerView list, Boolean restore) {
            this.context = context;
            mList = list;
            mIsRestore = restore;
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
            // copy over backup if from restore trigger
            if (mIsRestore) {
                try {
                    restorefile();
                } catch (Exception e) {
                    mTryCatchFail = true;
                }
            }
            // this loads up the listview - can take a sec
            String fileloc = createTempFile();
            if (fileloc.equalsIgnoreCase("error")) {
                mTryCatchFail = true;
            }

            prop = new Properties();
            File file = new File(fileloc);
            try {
                prop.load(new FileInputStream(file));
                pTitle = (String[])prop.keySet().toArray(new String[prop.keySet().size()]);
                final List<String> pDesc = new ArrayList<String>();
                for (int i = 0; i < pTitle.length; i++) {
                    pDesc.add(prop.getProperty(pTitle[i]));
                }

                proplist = buildData(pTitle, pDesc);
            } catch (IOException e) {
                mTryCatchFail = true;
            }

            //an attempt at sorting the build.prop mess by title
            Collections.sort(proplist, mapComparator);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //if it worked - set up adapter and onitemclick
            if (!mTryCatchFail) {
                LinearLayoutManager llm = new LinearLayoutManager(context);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                mList.setLayoutManager(llm);

                mList.setAdapter(new BuildPropRecyclerAdapter(proplist));
            }

            ((TinkerActivity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            dialog.dismiss();

            if (mTryCatchFail) {
                Toast.makeText(context, "Error occurred", Toast.LENGTH_SHORT).show();
            } else if (mIsRestore){
                Toast.makeText(context, "build.prop restored from " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/build.prop.bak", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class BuildPropItem extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView titleText;
        protected TextView descriptionText;

        public BuildPropItem(View itemView) {
            super(itemView);
            titleText = (TextView) itemView.findViewById(R.id.prop_title);
            descriptionText = (TextView) itemView.findViewById(R.id.prop_desc);
            LinearLayout cardClick = (LinearLayout) itemView.findViewById(R.id.clicklayout);
            cardClick.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            showEdit(titleText.getText().toString(), descriptionText.getText().toString());
        }
    }

    private class BuildPropRecyclerAdapter extends RecyclerView.Adapter<BuildPropItem> {
        private ArrayList<Map<String, String>> proplist;

        public BuildPropRecyclerAdapter(ArrayList<Map<String, String>> data) {
            this.proplist = data;
        }

        @Override
        public BuildPropItem onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.propeditlistitem, viewGroup, false);

            return new BuildPropItem(itemView);
        }

        @Override
        public void onBindViewHolder(BuildPropItem buildPropItem, int i) {
            buildPropItem.titleText.setText(proplist.get(i).get("title"));
            buildPropItem.descriptionText.setText(proplist.get(i).get("description"));
        }

        @Override
        public int getItemCount() {
            return proplist.size();
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return (View) inflater.inflate(R.layout.propeditmain, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerList);

        (new LoadProp()).setInits(getActivity(), recyclerView, false).execute();

        FloatingActionButton fabAdd = (FloatingActionButton) view.findViewById(R.id.fab);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ((TinkerActivity) getActivity()).displayEditProp(null, null);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_restore:
                (new LoadProp()).setInits(getActivity(), recyclerView, true).execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public BuildPropFragment() {}

    public ArrayList<Map<String, String>> buildData(String[] t, List<String> d) {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

        for (int i = 0; i < t.length; ++i) {
            list.add(putData(t[i], d.get(i)));
        }

        return list;
    }

    public HashMap<String, String> putData(String title, String description) {
        HashMap<String, String> item = new HashMap<String, String>();

        item.put("title", title);
        item.put("description", description);

        return item;
    }

    public void restorefile() {
        try {
            Shell.SU.run("mount -o remount,rw  /system");
            Shell.SU.run("mv -f /system/build.prop /system/build.prop.bak");
            Shell.SU.run("cp -f " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/build.prop.bak /system/build.prop");
            Shell.SU.run("chmod 644 /system/build.prop");
            Shell.SU.run("mount -o remount,ro  /system");
        } catch (Exception e) {
        }
    }

    public void showEdit(String name, String key) {
        ((TinkerActivity)getActivity()).displayEditProp(name, key);
    }

    public String createTempFile() {
        try {
            Shell.SU.run("cp -f /system/build.prop " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp");
            Shell.SU.run("chmod 777 " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp");
            return (String) Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp";
        } catch (Exception e) {
            return (String) "error";
        }
    }
}
