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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.purenexussettings.R;

import java.util.ArrayList;

public class NavDrawerListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItems;
    private String[] navDrawerCats;
    private String[] navDrawerCatItems;

    public NavDrawerListAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItems, String[] navDrawerCats, String[] navDrawerCatItems){
        this.context = context;
        this.navDrawerItems = navDrawerItems;
        this.navDrawerCats = navDrawerCats;
        this.navDrawerCatItems = navDrawerCatItems;
    }

    private class ViewHolderItem {
        TextView txtCatTitle;
        TextView txtTitle;
        ImageView imgIcon;
    }

    @Override
    public int getCount() {
        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return navDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolderItem viewHolder;

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            // null root is fine for this
            convertView = mInflater.inflate(R.layout.drawer_list_item, null);

            viewHolder = new ViewHolderItem();

            viewHolder.imgIcon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.txtTitle = (TextView) convertView.findViewById(R.id.title);
            viewHolder.txtCatTitle = (TextView) convertView.findViewById(R.id.category);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolderItem) convertView.getTag();
        }

        // remove imageview if no icon was passed for it
        if (navDrawerItems.get(position).getIcon() != -1) {
            viewHolder.imgIcon.setVisibility(View.VISIBLE);
            viewHolder.imgIcon.setImageResource(navDrawerItems.get(position).getIcon());
        } else {
            viewHolder.imgIcon.setVisibility(View.GONE);
        }

        viewHolder.txtTitle.setText(navDrawerItems.get(position).getTitle());

        // load and show category textview if entry is one flagged for including it
        viewHolder.txtCatTitle.setVisibility(View.GONE);
        for (int i = 0; i < navDrawerCats.length; i++) {
            if ( viewHolder.txtTitle.getText().toString().equals(navDrawerCatItems[i]) ) {
                viewHolder.txtCatTitle.setText(navDrawerCats[i]);
                viewHolder.txtCatTitle.setVisibility(View.VISIBLE);
            }
        }

        return convertView;
    }
}
