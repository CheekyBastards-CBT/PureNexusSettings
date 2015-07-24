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
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import eu.chainfire.libsuperuser.Shell;

public class EditPropFragment extends Fragment {

    private EditText editName;
    private EditText editKey;
    protected boolean changesPending;
    private String origName;
    private AlertDialog unsavedChangesDialog;
    private AlertDialog deleteItemDialog;

    private class ProcessEdits extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Context context = null;
        private String mOrigName;
        private String mNewName;
        private String mNewKey;
        private boolean mTryCatchFail;

        public ProcessEdits setInits(Context context, String origname, String newname, String newkey) {
            this.context = context;
            mOrigName = origname;
            mNewName = newname;
            mNewKey = newkey;
            return this;
        }

        @Override
        protected void onPreExecute() {
            // The progress dialog here is so the user will wait until the edits are complete.

            dialog = new ProgressDialog(context);
            dialog.setTitle("Hold on a sec");
            dialog.setMessage("Processing stuff...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                editfile(mOrigName, mNewName, mNewKey);
                transferFileToSystem();
                mTryCatchFail = false;
            } catch (Exception e) {
                mTryCatchFail = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //Now that edits are done, exit fragment and dismiss dialog
            ((TinkerActivity) getActivity()).onBackPressed();
            dialog.dismiss();
            if (mTryCatchFail) {
                Toast.makeText(context, "Error occurred", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Edit saved and a backup was made at " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/build.prop.bak", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.propedititem, container, false);

        setUpControls(v);
        setHasOptionsMenu(true);

        return v;
    }

    private void setUpControls(View v) {
        editName = (EditText)v.findViewById(R.id.prop_name);
        editKey = (EditText)v.findViewById(R.id.prop_key);

        origName = TinkerActivity.mEditName;
        String key = TinkerActivity.mEditKey;

        if (origName != null) {
            editName.setText(origName);
            editKey.setText(key);
        }

        editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changesPending = true;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not sure if anything to do here
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not sure if anything to do here
            }
        });

        editKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                changesPending = true;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not sure if anything to do here
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not sure if anything to do here
            }
        });

        FloatingActionButton fabSave = (FloatingActionButton) v.findViewById(R.id.fab);
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (editName.getText().toString().equals("")) {
                    deleteitem();
                } else {
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                    (new ProcessEdits()).setInits(getActivity(), origName, editName.getText().toString(), editKey.getText().toString()).execute();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_discard:
                canceledit();
                return true;
            case R.id.action_delete:
                if (origName != null) {
                    deleteitem();
                } else {
                    canceledit();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public EditPropFragment() {}

    public void canceledit() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (changesPending) {
            unsavedChangesDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.editprop_unsaved_changes_title)
                .setMessage(R.string.editprop_unsaved_changes_message)
                .setPositiveButton(R.string.saveprop, new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        unsavedChangesDialog.dismiss();
                        if (editName.getText().toString().equals("")) {
                            deleteitem();
                        } else {
                            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                            (new ProcessEdits()).setInits(getActivity(), origName, editName.getText().toString(), editKey.getText().toString()).execute();
                        }
                    }
                })
                .setNeutralButton(R.string.discardprop, new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                        unsavedChangesDialog.dismiss();
                        ((TinkerActivity) getActivity()).onBackPressed();
                    }
                })
                .setNegativeButton(R.string.cancelprop, new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        unsavedChangesDialog.dismiss();
                    }
                })
                .create();
            unsavedChangesDialog.show();
        } else {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            ((TinkerActivity)getActivity()).onBackPressed();;
        }
    }

    public void deleteitem() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        deleteItemDialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.editprop_delete_item_title)
            .setMessage(R.string.editprop_delete_item_message)
            .setPositiveButton(R.string.deleteprop, new AlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteItemDialog.dismiss();
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                    (new ProcessEdits()).setInits(getActivity(), origName, null, null).execute();
                }
            })
            .setNegativeButton(R.string.cancelprop, new AlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteItemDialog.dismiss();
                }
            })
            .create();
        deleteItemDialog.show();
    }

    public void transferFileToSystem() {
        try {
            Shell.SU.run("mount -o remount,rw  /system");
            Shell.SU.run("mv -f /system/build.prop /system/build.prop.bak");
            Shell.SU.run("cp -f " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp /system/build.prop");
            Shell.SU.run("chmod 644 /system/build.prop");
            Shell.SU.run("mount -o remount,ro  /system");
        } catch (Exception e) {
        }
    }

    public void editfile(String origkey, String key, String value) {
        if (key == null && value == null && origkey != null) {
            Shell.SU.run("sed -i '/" + origkey + "=/d' " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp");
        } else if (origkey == null) {
            Shell.SU.run("sed -i '$ a\\" + key + "=" + value +"' " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp");
        } else {
            Shell.SU.run("sed -i '/" + origkey + "=/c\\" + key + "=" + value +"' " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/buildprop.tmp");
        }
    }
}
