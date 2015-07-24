/*
 * Copyright (C) 2015 The Pure Nexus Project
 * Borrows parts from work by Matthew Wiggins
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

package com.android.purenexussettings.preferences;


import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.IWindowManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.purenexussettings.R;

import java.math.BigDecimal;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, OnClickListener
{
    private static final String androidns="http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mValueText;
    private Context mContext;

    private String mDialogMessage;
    private String mSuffix;
    private String mKey;
    private int mDefault;
    private int mMax;
    private int mValue;
    private int mDisplayFactor;
    private int mDecimalPlaces;
    private int mAnimType;
    private int mMin;
    private int mStep;
    private boolean mPercent;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context,attrs);
        mContext = context;

        // For the custom seekbar attributes
        TypedArray seekBarStyle = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPref, 0, 0);

        /* Get string value for dialogMessage */
        int mDialogMessageId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if(mDialogMessageId == 0) {
            mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        } else {
            mDialogMessage = mContext.getString(mDialogMessageId);
        }

        /* Get string value for suffix (text attribute in xml file) */
        int mSuffixId = attrs.getAttributeResourceValue(androidns, "text", 0);
        if(mSuffixId == 0) {
            mSuffix = attrs.getAttributeValue(androidns, "text");
        } else {
            mSuffix = mContext.getString(mSuffixId);
        }

        /* Get string value for key (text attribute in xml file) */
        int mKeyId = attrs.getAttributeResourceValue(androidns, "key", 0);
        if(mKeyId == 0) {
            mKey = attrs.getAttributeValue(androidns, "key");
        } else {
            mKey = mContext.getString(mKeyId);
        }

        /* Get default and max seekbar values (from xml file) */
        /* This assumes they are entered in directly, NOT as resources */
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "max", 100);

        /* Get custom attribute for display factor from xml to translate from what is shown to what is putInt */
        mDisplayFactor = seekBarStyle.getInt(R.styleable.SeekBarPref_displayfactor, 1);

        /* Get custom attribute for decimal places from xml for display */
        mDecimalPlaces = seekBarStyle.getInt(R.styleable.SeekBarPref_decimalsshown, 0);

        // Get flag for animation seekbars
        mAnimType = seekBarStyle.getInt(R.styleable.SeekBarPref_animtype, -1);

        // Get flag for force percentage scale for seekbar
        mPercent = seekBarStyle.getBoolean(R.styleable.SeekBarPref_percentage, false);

        // Get min and step values for seekbar
        mMin = seekBarStyle.getInt(R.styleable.SeekBarPref_minval, 0);
        mStep = seekBarStyle.getInt(R.styleable.SeekBarPref_stepval, 1);

        seekBarStyle.recycle();
    }

    /* DialogPreference methods */
    @Override
    protected View onCreateDialogView() {

        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        TextView mSplashText = new TextView(mContext);
        mSplashText.setPadding(30, 10, 30, 10);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(20);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        int i;
        Float f;
        BigDecimal j;
        BigDecimal k;

        if (mAnimType == -1) {
            if (mDisplayFactor != 0) {
                i = mDefault * mDisplayFactor;
                mValue = Settings.Global.getInt(mContext.getContentResolver(), mKey, i);
                mValue = mValue / mDisplayFactor;
            } else {
                mValue = Settings.Global.getInt(mContext.getContentResolver(), mKey, mDefault);
            }
        } else {
            IWindowManager mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            try {
                f = mWindowManager.getAnimationScale(mAnimType);
            } catch (RemoteException e) {
                f = 0.0F;
            }
            j = new BigDecimal(Math.pow(10, (double) mDecimalPlaces)).setScale(0, BigDecimal.ROUND_HALF_EVEN);
            k = new BigDecimal(f).setScale(mDecimalPlaces, BigDecimal.ROUND_HALF_EVEN);

            j = j.multiply(k);
            mValue = j.intValue();
        }

        mSeekBar.setMax((mMax-mMin)/mStep);
        mSeekBar.setProgress(mValue-mMin);

        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax((mMax-mMin)/mStep);
        mSeekBar.setProgress(mValue-mMin);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);

        if (mDisplayFactor != 0) {
            int i = mDefault * mDisplayFactor;
            mValue = Settings.Global.getInt(mContext.getContentResolver(), mKey, i);
            mValue = (mValue / mDisplayFactor) - mMin;
        } else {
            mValue = (Settings.Global.getInt(mContext.getContentResolver(), mKey, mDefault)) - mMin;
        }
    }

    /* OnSeekBarChangeListener methods */
    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        String t;
        BigDecimal j;
        BigDecimal k;

        j = new BigDecimal(mMin + (value * mStep));
        if (mPercent) {
            k = new BigDecimal(100);
            j = j.multiply(k);
        }

        k = (mPercent) ? new BigDecimal(mMax-mMin) : new BigDecimal(Math.pow(10, (double)mDecimalPlaces));
        j = j.divide(k, mDecimalPlaces, BigDecimal.ROUND_HALF_EVEN);
        t = String.valueOf(j);

        if (mSuffix == null) {
            mValueText.setText(t);
        } else {
            t = t.concat(" " + mSuffix);
            mValueText.setText(t);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {}

    @Override
    public void onStopTrackingTouch(SeekBar seek) {}

    public int getAnimType()
    {
        return mAnimType;
    }

    public int getDefault()
    {
        return mDefault;
    }

    public int getDecimals()
    {
        return mDecimalPlaces;
    }

    public int getFactor()
    {
        return mDisplayFactor;
    }

    public int getMax()
    {
        return mMax;
    }

    public int getMin()
    {
        return mMin;
    }

    public boolean getPercent()
    {
        return mPercent;
    }

    public int getStep()
    {
        return mStep;
    }

    public String getUnits()
    {
        return mSuffix;
    }

    public void setMax(int max)
    {
        mMax = max;
    }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
    }

    public int getProgress()
    {
        return mValue;
    }

    /* Set the positive button listener and onClick action */
    @Override
    public void showDialog(Bundle state) {
        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        callChangeListener(mSeekBar.getProgress());

        (getDialog()).dismiss();
    }
}
