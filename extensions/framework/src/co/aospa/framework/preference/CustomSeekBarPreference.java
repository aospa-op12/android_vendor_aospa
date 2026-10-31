/*
 * Copyright (C) 2016-2017 The Dirty Unicorns Project
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
 * limitations under the License
 */

package co.aospa.framework.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.*;

import com.google.android.material.slider.Slider;

import co.aospa.framework.R;

public class CustomSeekBarPreference extends Preference implements Slider.OnChangeListener,
        Slider.OnSliderTouchListener,
        View.OnClickListener, View.OnLongClickListener {
    protected final String TAG = getClass().getName();
    private static final String SETTINGS_NS = "http://schemas.android.com/apk/res/com.android.settings";
    private static final String SETTINGS_NS_ALT = "http://schemas.android.com/apk/res-auto";
    protected static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";

    protected int mInterval = 1;
    protected boolean mShowSign = false;
    protected String mUnits = "";
    protected boolean mContinuousUpdates = false;
    protected String mTextStart, mTextEnd;
    protected boolean mShowButtons;

    protected int mMinValue = 0;
    protected int mMaxValue = 100;
    protected int mDefaultValue;

    protected int mValue;

    protected TextView mValueTextView;
    protected ImageView mResetImageView;
    protected ImageView mMinusImageView;
    protected ImageView mPlusImageView;
    protected Slider mSlider;

    protected boolean mTrackingTouch = false;
    protected int mTrackingValue;

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference);
        try {
            mShowSign = a.getBoolean(R.styleable.CustomSeekBarPreference_showSign, mShowSign);
            String units = a.getString(R.styleable.CustomSeekBarPreference_units);
            if (units != null) mUnits = units;
            mContinuousUpdates = a.getBoolean(
                    R.styleable.CustomSeekBarPreference_continuousUpdates, false);
            mTextStart = a.getString(R.styleable.CustomSeekBarPreference_textStart);
            mTextEnd = a.getString(R.styleable.CustomSeekBarPreference_textEnd);
            mShowButtons = a.getBoolean(R.styleable.CustomSeekBarPreference_showButtons, true);
        } finally {
            a.recycle();
        }

        try {
            String newInterval = attrs.getAttributeValue(SETTINGS_NS, "interval");
            if (newInterval != null) {
                mInterval = Integer.parseInt(newInterval);
            } else {
                newInterval = attrs.getAttributeValue(SETTINGS_NS_ALT, "interval");
                if (newInterval != null) mInterval = Integer.parseInt(newInterval);
            }
        } catch (Exception e) {
            Log.e(TAG, "Invalid interval value", e);
        }
        mInterval = Math.max(1, mInterval);
        mMinValue = attrs.getAttributeIntValue(SETTINGS_NS, "min", mMinValue);
        if (mMinValue == 0) {
            int min = attrs.getAttributeIntValue(SETTINGS_NS_ALT, "min", mMinValue);
            if (min != 0) mMinValue = min;
        }
        mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", mMaxValue);
        if (mMaxValue < mMinValue)
            mMaxValue = mMinValue;

        setLayoutResource(R.layout.preference_custom_seekbar);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public CustomSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);

        mSlider = (Slider) holder.findViewById(R.id.slider);
        mSlider.setValueFrom(mMinValue);
        mSlider.setValueTo(mMaxValue);
        mSlider.setStepSize(getSliderStepSize());
        mSlider.setTickVisible(getSliderStepSize() > 0);
        mSlider.setValue(getLimitedValue(mValue));
        mSlider.setEnabled(isEnabled());
        mSlider.setClickable(isSelectable());

        mValueTextView = (TextView) holder.findViewById(R.id.value);
        mResetImageView = (ImageView) holder.findViewById(R.id.reset);
        mMinusImageView = (ImageView) holder.findViewById(R.id.minus);
        mPlusImageView = (ImageView) holder.findViewById(R.id.plus);

        if (mTextEnd != null || mTextStart != null) {
            holder.findViewById(R.id.label_frame).setVisibility(View.VISIBLE);
            TextView startText = (TextView) holder.findViewById(android.R.id.text1);
            TextView endText = (TextView) holder.findViewById(android.R.id.text2);
            startText.setText(mTextStart);
            endText.setText(mTextEnd);
        }

        if (!mShowButtons) {
            mMinusImageView.setVisibility(View.GONE);
            mPlusImageView.setVisibility(View.GONE);
        }

        updateValueViews();

        mSlider.removeOnChangeListener(this);
        mSlider.removeOnSliderTouchListener(this);
        mSlider.addOnChangeListener(this);
        mSlider.addOnSliderTouchListener(this);
        mResetImageView.setOnClickListener(this);
        mMinusImageView.setOnClickListener(this);
        mPlusImageView.setOnClickListener(this);
        mResetImageView.setOnLongClickListener(this);
        mMinusImageView.setOnLongClickListener(this);
        mPlusImageView.setOnLongClickListener(this);
    }

    protected int getLimitedValue(int v) {
        return v < mMinValue ? mMinValue : (v > mMaxValue ? mMaxValue : v);
    }

    protected float getSliderStepSize() {
        int range = mMaxValue - mMinValue;
        return range > 0 && range % mInterval == 0 ? mInterval : 0;
    }

    protected String getTextValue(int v) {
        return String.valueOf(v) + mUnits;
    }

    protected void updateValueViews() {
        if (mValueTextView != null) {
            String textValue = getTextValue(mValue);
            if (mTrackingTouch && !mContinuousUpdates) {
                textValue = getTextValue(mTrackingValue);
            }
            mValueTextView.setText(textValue);
        }

        if (mResetImageView != null) {
            if (mValue == mDefaultValue || mTrackingTouch)
                mResetImageView.setVisibility(View.INVISIBLE);
            else
                mResetImageView.setVisibility(View.VISIBLE);
        }

        if (mMinusImageView != null) {
            if (mValue == mMinValue || mTrackingTouch) {
                mMinusImageView.setClickable(false);
                mMinusImageView.setAlpha(0.38f);
            } else {
                mMinusImageView.setClickable(true);
                mMinusImageView.setAlpha(1f);
            }
        }

        if (mPlusImageView != null) {
            if (mValue == mMaxValue || mTrackingTouch) {
                mPlusImageView.setClickable(false);
                mPlusImageView.setAlpha(0.38f);
            } else {
                mPlusImageView.setClickable(true);
                mPlusImageView.setAlpha(1f);
            }
        }
    }

    protected void changeValue(int newValue) {
        // for subclasses
    }

    @Override
    public void onValueChange(Slider slider, float value, boolean fromUser) {
        int newValue = getLimitedValue(Math.round(value));
        if (mTrackingTouch && !mContinuousUpdates) {
            mTrackingValue = newValue;
            updateValueViews();
        } else if (mValue != newValue) {
            // change rejected, revert to the previous value
            if (!callChangeListener(newValue)) {
                mSlider.setValue(mValue);
                return;
            }
            // change accepted, store it
            changeValue(newValue);
            persistInt(newValue);

            mValue = newValue;
            updateValueViews();
        }
    }

    @Override
    public void onStartTrackingTouch(Slider slider) {
        mTrackingValue = mValue;
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(Slider slider) {
        mTrackingTouch = false;
        if (!mContinuousUpdates && mValue != mTrackingValue) {
            if (callChangeListener(mTrackingValue)) {
                changeValue(mTrackingValue);
                persistInt(mTrackingValue);
                mValue = mTrackingValue;
            } else {
                mSlider.setValue(mValue);
            }
            updateValueViews();
        }
        notifyChanged();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.reset) {
            Toast.makeText(getContext(), getContext().getString(
                    R.string.custom_seekbar_default_value_to_set, getTextValue(mDefaultValue)),
                    Toast.LENGTH_LONG).show();
        } else if (id == R.id.minus) {
            setValue(mValue - mInterval, true);
        } else if (id == R.id.plus) {
            setValue(mValue + mInterval, true);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.reset) {
            setValue(mDefaultValue, true);
        } else if (id == R.id.minus) {
            int value = mMinValue;
            if (mMaxValue - mMinValue > mInterval * 2 && mMaxValue + mMinValue < mValue * 2) {
                value = Math.floorDiv(mMaxValue + mMinValue, 2);
            }
            setValue(value, true);
        } else if (id == R.id.plus) {
            int value = mMaxValue;
            if (mMaxValue - mMinValue > mInterval * 2 && mMaxValue + mMinValue > mValue * 2) {
                value = -1 * Math.floorDiv(-1 * (mMaxValue + mMinValue), 2);
            }
            setValue(value, true);
        }
        return true;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        mDefaultValue = ta.getInt(index, mMinValue);
        return mDefaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        mValue = getPersistedInt(mDefaultValue);
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        setDefaultValue((Integer) defaultValue, mSlider != null);
    }

    public void setDefaultValue(int newValue, boolean update) {
        newValue = getLimitedValue(newValue);
        if (mDefaultValue != newValue) {
            mDefaultValue = newValue;
            if (update)
                updateValueViews();
        }
    }

    public void setMax(int max) {
        mMaxValue = Math.max(max, mMinValue);
        if (mSlider != null) {
            mSlider.setValueTo(mMaxValue);
            mSlider.setValue(getLimitedValue(mValue));
        }
    }

    public int getMax() {
        return mMaxValue;
    }

    public void setMin(int min) {
        mMinValue = Math.min(min, mMaxValue);
        if (mSlider != null) {
            mSlider.setValueFrom(mMinValue);
            mSlider.setValue(getLimitedValue(mValue));
        }
    }

    public void setValue(int newValue) {
        mValue = getLimitedValue(newValue);
        if (mSlider != null) mSlider.setValue(mValue);
    }

    public void setValue(int newValue, boolean update) {
        newValue = getLimitedValue(newValue);
        if (mValue != newValue) {
            if (update) {
                if (mSlider != null) {
                    mSlider.setValue(newValue);
                } else {
                    mValue = newValue;
                }
            } else {
                mValue = newValue;
            }
        }
    }

    public int getValue() {
        return mValue;
    }

    public void setUnits(String units) {
        mUnits = units;
        updateValueViews();
    }

    public String getUnits() {
        return mUnits;
    }

    // need some methods here to set/get other attrs at runtime,
    // but who really need this ...

    public void refresh(int newValue) {
        // this will ...
        setValue(newValue, mSlider != null);
    }
}
