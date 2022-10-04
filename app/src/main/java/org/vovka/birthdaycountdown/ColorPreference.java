/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 24.01.2022, 20:58
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ColorPreference extends Preference {
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.item_color;
    private int mNumColumns = 4;
    private String mSelectDialogTitle = "";
    private int mSelectDialogIcon;

    public ColorPreference(Context context) {
        super(context);
        initAttrs(null, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs, 0);
    }

    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs, defStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ColorPreference, defStyle, defStyle);

        try {
            mSelectDialogTitle = ta.getString(R.styleable.ColorPreference_dialogTitle);
            mSelectDialogIcon = ta.getResourceId(R.styleable.ColorPreference_dialogIcon, 0);
            mItemLayoutId = ta.getResourceId(R.styleable.ColorPreference_itemLayout, mItemLayoutId);
            mNumColumns = ta.getInteger(R.styleable.ColorPreference_numColumns, mNumColumns);
            int choicesResId = ta.getResourceId(R.styleable.ColorPreference_choices, R.array.default_color_choice_values);
            if (choicesResId > 0) {
                //https://stackoverflow.com/questions/9114587/how-can-i-save-colors-in-array-xml-and-get-its-back-to-color-array
                mColorChoices = ta.getResources().getIntArray(choicesResId);
            }

        } finally {
            ta.recycle();
        }

        setWidgetLayoutResource(mItemLayoutId);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View mPreviewView = view.findViewById(R.id.color_view);
        setColorViewValue(mPreviewView, mValue);
    }

    public void setValue(int value) {
        if (callChangeListener(value)) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    protected void onClick() {
        super.onClick();

        ColorDialogFragment fragment = ColorDialogFragment.newInstance();
        fragment.setPreference(this);

        Activity activity = (Activity) getContext();
        activity.getFragmentManager().beginTransaction()
                .add(fragment, getFragmentTag())
                .commit();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        Activity activity = (Activity) getContext();
        ColorDialogFragment fragment = (ColorDialogFragment) activity
                .getFragmentManager().findFragmentByTag(getFragmentTag());
        if (fragment != null) {
            // re-bind preference to fragment
            fragment.setPreference(this);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    public String getFragmentTag() {
        return "color_" + getKey();
    }

    public int getValue() {
        return mValue;
    }

    public static class ColorDialogFragment extends DialogFragment {
        private ColorPreference mPreference;
        private ColorGridAdapter mAdapter;
        private GridView mColorGrid;

        @SuppressWarnings("deprecation")
        public ColorDialogFragment() {
        }

        public static ColorDialogFragment newInstance() {
            return new ColorDialogFragment();
        }

        public void setPreference(ColorPreference preference) {
            mPreference = preference;
            tryBindLists();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            tryBindLists();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View rootView = layoutInflater.inflate(R.layout.dialog_colors, null);

            if (mPreference.mSelectDialogTitle.isEmpty()) {
                View caption = rootView.findViewById(R.id.caption);
                caption.setVisibility(View.GONE);
            } else {
                TextView titleView = rootView.findViewById(R.id.title);
                titleView.setText(mPreference.mSelectDialogTitle);
            }
            ImageView icon = rootView.findViewById(R.id.icon);
            if (mPreference.mSelectDialogIcon != 0) {
                icon.setImageResource(mPreference.mSelectDialogIcon);
            } else {
                icon.setVisibility(View.GONE);
            }

            mColorGrid = rootView.findViewById(R.id.color_grid);
            mColorGrid.setNumColumns(mPreference.mNumColumns);
            
            mColorGrid.setOnItemClickListener((listView, view, position, itemId) -> {
                    mPreference.setValue(mAdapter.getItem(position));
                    dismiss();
            });

            tryBindLists();

            return new AlertDialog.Builder(getActivity())
                    .setView(rootView)
                    .create();
        }

        private void tryBindLists() {
            if (mPreference == null) {
                return;
            }

            if (isAdded() && mAdapter == null) {
                mAdapter = new ColorGridAdapter();
            }

            if (mAdapter != null && mColorGrid != null) {
                mAdapter.setSelectedColor(mPreference.getValue());
                mColorGrid.setAdapter(mAdapter);
            }
        }

        private class ColorGridAdapter extends BaseAdapter {
            private final List<Integer> mChoices = new ArrayList<>();
            private int mSelectedColor;

            private ColorGridAdapter() {
                for (int color : mPreference.mColorChoices) {
                    mChoices.add(color);
                }
            }

            @Override
            public int getCount() {
                return mChoices.size();
            }

            @Override
            public Integer getItem(int position) {
                return mChoices.get(position);
            }

            @Override
            public long getItemId(int position) {
                return mChoices.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup container) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity())
                            .inflate(mPreference.mItemLayoutId, container, false);
                }

                int color = getItem(position);
                setColorViewValue(convertView.findViewById(R.id.color_view), color);
                convertView.setBackgroundColor(color == mSelectedColor ? 0x6633b5e5 : 0);
                return convertView;
            }

            public void setSelectedColor(int selectedColor) {
                mSelectedColor = selectedColor;
                notifyDataSetChanged();
            }
        }
    }

    private static void setColorViewValue(View view, int color) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Resources res = imageView.getContext().getResources();

            Drawable currentDrawable = imageView.getDrawable();
            GradientDrawable colorChoiceDrawable;
            if (currentDrawable instanceof GradientDrawable) {
                colorChoiceDrawable = (GradientDrawable) currentDrawable;
            } else {
                colorChoiceDrawable = new GradientDrawable();
                colorChoiceDrawable.setShape(GradientDrawable.OVAL);
            }

            // Set stroke to dark version of color
            int darkenedColor = Color.rgb(
                    Color.red(color) * 192 / 256,
                    Color.green(color) * 192 / 256,
                    Color.blue(color) * 192 / 256);

            colorChoiceDrawable.setColor(color);
            colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics()), darkenedColor);
            imageView.setImageDrawable(colorChoiceDrawable);
            imageView.setContentDescription(Integer.toString(color));

        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
    }
}