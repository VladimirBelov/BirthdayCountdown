/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 09.06.2025, 11:40
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;

import java.util.ArrayList;
import java.util.List;


/**
 * Пользовательская настройка для выбора цвета из предопределенного набора или путем указания пользовательского цвета RGB.
 * <p>
 * Эта настройка отображает предварительный просмотр цвета и открывает диалоговое окно для выбора цвета при нажатии.
 * Цвет можно выбрать из сетки предопределенных цветов или путем настройки значений RGB и альфа с помощью полос прокрутки.
 * </p>
 *
 * <p>
 * <b>Атрибуты XML:</b>
 * <ul>
 *   <li>{@code app:dialogTitle} - String: Заголовок диалогового окна выбора цвета.</li>
 *   <li>{@code app:dialogIcon} - Reference: Иконка, отображаемая в диалоговом окне выбора цвета.</li>
 *   <li>{@code app:itemLayout} - Reference: ID ресурса макета для каждого элемента цвета в сетке.</li>
 *   <li>{@code app:numColumns} - Integer: Количество столбцов в сетке цветов.</li>
 *   <li>{@code app:choices} - Reference: Ресурс массива, содержащий предопределенные варианты выбора цвета в виде целых чисел.</li>
 * </ul>
 * </p>
 */
public class ColorPreference extends Preference {
    private static final String TAG = "ColorPreference";
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.item_color;
    private int mNumColumns = 5;
    private String mSelectDialogTitle = "";
    private int mSelectDialogIcon;
    private final Context context;

    /** @noinspection unused*/
    public ColorPreference(Context context) {
        super(context);
        this.context = context;
        initAttrs(null, 0);
    }

    /** @noinspection unused*/
    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initAttrs(attrs, 0);
    }

    /** @noinspection unused*/
    public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initAttrs(attrs, defStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ColorPreference, defStyle, defStyle);

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

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

        Activity activity = (Activity) context;
        activity.getFragmentManager()
                .beginTransaction()
                .add(fragment, getFragmentTag())
                .commit();
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            ColorDialogFragment fragment = (ColorDialogFragment) activity
                    .getFragmentManager().findFragmentByTag(getFragmentTag());
            if (fragment != null) {
                // re-bind preference to fragment
                fragment.setPreference(this);
            }
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

            AlertDialog.Builder colorDialogBuilder = new AlertDialog.Builder(getActivity());

            try {
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

                mColorGrid.setOnItemLongClickListener((parent, view, position, id) -> {
                    Toast.makeText(getActivity(),
                            getActivity().getString(R.string.pref_Color_title) +
                                    Constants.STRING_SPACE +
                                    ContactsEvents.toARGBString(mAdapter.getItem(position))
                            , Toast.LENGTH_SHORT).show();
                    return true;
                });

                tryBindLists();

                ContactsEvents eventsData = ContactsEvents.getInstance();
                colorDialogBuilder.setView(rootView);

                if (eventsData.preferences_extrafun) {
                    colorDialogBuilder.setNeutralButton(R.string.button_rgb, (dialog, which) -> {
                        dialog.dismiss();
                        selectRGBColor(eventsData);
                    });
                }

            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getActivity(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }

            return colorDialogBuilder.create();
        }

        private void selectRGBColor(ContactsEvents eventsData) {

            try {

                TypedArray ta = getActivity().getTheme().obtainStyledAttributes(R.styleable.Theme);

                final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), eventsData.preferences_theme.themeDialog))
                        .setPositiveButton(R.string.button_ok, null)
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());

                if (eventsData.preferences_theme.themeEditText != 0) {
                    builder.getContext().setTheme(eventsData.preferences_theme.themeEditText);
                } else {
                    builder.getContext().setTheme(ContactsEvents.themeEditText_default);
                }

                AlertDialog dialog = builder.create();
                View view = View.inflate(new ContextThemeWrapper(getActivity(), ContactsEvents.getInstance().preferences_theme.themeMain), R.layout.dialog_rgbcolor, null);
                dialog.setView(view);

                if (mPreference.mSelectDialogTitle.isEmpty()) {
                    View caption = view.findViewById(R.id.caption);
                    caption.setVisibility(View.GONE);
                } else {
                    TextView titleView = view.findViewById(R.id.title);
                    titleView.setText(mPreference.mSelectDialogTitle);
                }
                ImageView icon = view.findViewById(R.id.icon);
                if (mPreference.mSelectDialogIcon != 0) {
                    icon.setImageResource(mPreference.mSelectDialogIcon);
                } else {
                    icon.setVisibility(View.GONE);
                }

                TextView color_label = view.findViewById(R.id.color_label);
                color_label.setText(getString(R.string.pref_Color_title));

                final int[] colorValue = {mPreference.getValue()};
                TextView color_edit = view.findViewById(R.id.color_edit);
                color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));

                ImageView color_preview = view.findViewById(R.id.color_preview);
                setColorViewValue(color_preview, colorValue[0]);

                TextView seek1_label = view.findViewById(R.id.seek1_label);
                seek1_label.setText(getString(R.string.pref_Red_title));
                SeekBar seek1 = view.findViewById(R.id.seek1);
                seek1.setProgress(Color.red(colorValue[0]));
                TextView seek1_progress = view.findViewById(R.id.seek1_progress);
                seek1_progress.setText(String.valueOf(seek1.getProgress()));

                TextView seek2_label = view.findViewById(R.id.seek2_label);
                seek2_label.setText(getString(R.string.pref_Green_title));
                SeekBar seek2 = view.findViewById(R.id.seek2);
                seek2.setProgress(Color.green(colorValue[0]));
                TextView seek2_progress = view.findViewById(R.id.seek2_progress);
                seek2_progress.setText(String.valueOf(Color.green(colorValue[0])));

                TextView seek3_label = view.findViewById(R.id.seek3_label);
                seek3_label.setText(getString(R.string.pref_Blue_title));
                SeekBar seek3 = view.findViewById(R.id.seek3);
                seek3.setProgress(Color.blue(colorValue[0]));
                TextView seek3_progress = view.findViewById(R.id.seek3_progress);
                seek3_progress.setText(String.valueOf(Color.blue(colorValue[0])));

                TextView seek4_label = view.findViewById(R.id.seek4_label);
                seek4_label.setText(getString(R.string.pref_Alpha_title));
                SeekBar seek4 = view.findViewById(R.id.seek4);
                seek4.setProgress(255 - Color.alpha(colorValue[0]));
                TextView seek4_progress = view.findViewById(R.id.seek4_progress);
                seek4_progress.setText(String.valueOf(255 - Color.alpha(colorValue[0])));

                color_edit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /**/ }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { /**/ }
                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            String colorString = color_edit.getText().toString();
                            if (!colorString.startsWith(Constants.STRING_HASH) && colorString.matches("\\d+")) colorString = Constants.STRING_HASH + colorString;
                            int colorInt = Color.parseColor(colorString);
                            colorValue[0] = colorInt;
                            seek1.setProgress(Color.red(colorInt));
                            seek2.setProgress(Color.green(colorInt));
                            seek3.setProgress(Color.blue(colorInt));
                            seek4.setProgress(255 - Color.alpha(colorInt));
                            setColorViewValue(color_preview, colorInt);
                        } catch (Exception e) { /**/ }
                    }
                });

                seek1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek1_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                seek2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek2_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                seek3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek3_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                seek4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek4_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                dialog.setOnShowListener(arg0 -> {
                    final Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    buttonPositive.setOnClickListener(v -> {
                        try {
                            String colorString = color_edit.getText().toString();
                            if (!colorString.startsWith(Constants.STRING_HASH)) colorString = Constants.STRING_HASH + colorString;
                            int colorInt = Color.parseColor(colorString);
                            mPreference.setValue(colorInt);
                            eventsData.setRecentColor(colorInt);
                            dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                            ToastExpander.showInfoMsg(eventsData.getContext(), eventsData.getResources().getString(R.string.msg_color_parse_error));
                        }
                    });
                    final View buttonBar = (View) buttonPositive.getParent();
                    buttonBar.setBackgroundColor(ta.getColor(R.styleable.Theme_editTextBackgroundCustom, 0));
                });

                dialog.setOnDismissListener(dialogM -> ta.recycle());
                dialog.show();

            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }
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

                ContactsEvents eventsData = ContactsEvents.getInstance();

                try {

                    for (int color : mPreference.mColorChoices) {
                        mChoices.add(color);
                    }

                    //Добавляем текущий и недавние цвета
                    if (!mChoices.contains(mPreference.getValue())) {
                        mChoices.add(mPreference.getValue());
                    }

                    for (int valueInt : eventsData.preferences_RecentColors) {
                        if (!mChoices.contains(valueInt)) {
                            mChoices.add(valueInt);
                        }
                    }

                } catch (final Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    ToastExpander.showDebugMsg(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
                View convertedView;
                if (convertView == null) {
                    convertedView = LayoutInflater.from(getActivity()).inflate(mPreference.mItemLayoutId, container, false);
                } else {
                    convertedView = convertView;
                }

                int color = getItem(position);
                setColorViewValue(convertedView.findViewById(R.id.color_view), color);
                convertedView.setBackgroundColor(color == mSelectedColor ? 0x6633b5e5 : 0);
                return convertedView;
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
            int radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, res.getDisplayMetrics());

            if (Color.alpha(color) == 0) {
                Bitmap bm = BitmapFactory.decodeResource(res, R.drawable.transparent);
                imageView.setImageBitmap(Bitmap.createScaledBitmap(bm, radius, radius, false));
                bm.recycle();
                return;
            }
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