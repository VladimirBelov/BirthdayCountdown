/*
 * *
 *  * Created by Vladimir Belov on 18.06.2025, 15:45
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 18.06.2025, 15:21
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ColorPicker - это пользовательский элемент управления, который позволяет пользователю выбрать цвет из предопределенного набора вариантов
 * или выбрать цвет с помощью RGB-слайдеров. Его можно использовать в макетах или программно.
 *
 * <p>
 * Этот класс предоставляет визуальный интерфейс выбора цвета в приложении Android. Он предлагает
 * несколько способов выбора цвета, включая сетку предопределенных вариантов цветов и RGB-селектор.
 * </p>
 *
 * <p>
 *   <b>Основные характеристики:</b>
 *   <ul>
 *     <li><b>Предопределенные варианты цветов:</b> Представляет сетку цветов для удобного выбора.</li>
 *     <li><b>RGB-селектор:</b> Позволяет пользователям точно настроить выбор цвета с помощью слайдеров Красный, Зеленый, Синий и Альфа.</li>
 *     <li><b>Настраиваемый:</b> Может быть настроен через атрибуты XML или программно.</li>
 *   </ul>
 * </p>
 */
class ColorPicker extends FrameLayout implements View.OnClickListener {

    private static final String TAG = "ColorPicker";
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.item_color;
    private int mNumColumns = 5;
    private String mSelectDialogTitle = "";
    private int mSelectDialogIcon;
    private ColorGridAdapter mAdapter;
    private final Context context;
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    public ColorPicker(@NonNull Context context) {
        super(context);
        this.context = context;
        initAttrs(null, 0, 0);
    }

    public ColorPicker(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initAttrs(attrs, 0, 0);
    }

    public ColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initAttrs(attrs, defStyleAttr, 0);
    }

    public ColorPicker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        initAttrs(attrs, defStyleAttr, defStyleRes);
    }

    @SuppressLint("DiscouragedApi")
    private void initAttrs(AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        LayoutInflater.from(getContext()).inflate(R.layout.picker_color, this);
        setOnClickListener(this);

        TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ColorPreference, defStyleAttr, defStyleRes);

        try {
            mSelectDialogTitle = ta.getString(R.styleable.ColorPreference_dialogTitle);
            mSelectDialogIcon = ta.getResourceId(R.styleable.ColorPreference_dialogIcon, 0);
            mItemLayoutId = ta.getResourceId(R.styleable.ColorPreference_itemLayout, mItemLayoutId);
            mNumColumns = ta.getInteger(R.styleable.ColorPreference_numColumns, mNumColumns);
            mValue = ta.getInteger(R.styleable.ColorPreference_defaultValue, ContextCompat.getColor(getContext(), R.color.pref_Widgets_Color_WidgetBackground_default));
            int choicesResId = ta.getResourceId(R.styleable.ColorPreference_choices, R.array.default_color_choice_values);
            if (choicesResId > 0) {
                //https://stackoverflow.com/questions/9114587/how-can-i-save-colors-in-array-xml-and-get-its-back-to-color-array
                mColorChoices = ta.getResources().getIntArray(choicesResId);
            }

            if (ta.hasValue(R.styleable.ColorPreference_title)) {
                int id = getResources().getIdentifier("title", Constants.STRING_ID, Constants.RES_PACKAGE_ANDROID);
                if (id > 0) {
                    TextView view = findViewById(id);
                    if (view != null) view.setText(ta.getString(R.styleable.ColorPreference_title));
                }
            }
            if (ta.hasValue(R.styleable.ColorPreference_summary)) {
                int id = getResources().getIdentifier("summary", Constants.STRING_ID, Constants.RES_PACKAGE_ANDROID);
                if (id > 0) {
                    TextView view = findViewById(id);
                    if (view != null)
                        view.setText(ta.getString(R.styleable.ColorPreference_summary));
                }

            }

            int id = getResources().getIdentifier("icon", Constants.STRING_ID, Constants.RES_PACKAGE_ANDROID);
            if (id > 0) {
                ImageView view = findViewById(id);
                if (view != null) {
                    if (ta.hasValue(R.styleable.ColorPreference_icon)) {
                        view.setImageResource(ta.getResourceId(R.styleable.ColorPreference_icon, 0));
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                }
            }

            setColor(mValue);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            ta.recycle();
        }

    }

    public void setColor(int color) {

        mValue = color;
        @SuppressLint("DiscouragedApi") int id = getResources().getIdentifier("icon1", Constants.STRING_ID, Constants.RES_PACKAGE_ANDROID);
        if (id > 0) {
            View view = findViewById(id);
            if (view != null) {
                setColorViewValue(view, mValue);
            }
        }
    }

    public void selectRGBColor(int initValue, int defaultValue, String methodToInvoke, String idToPass) {

        try {

            TypedArray ta = getContext().getTheme().obtainStyledAttributes(R.styleable.Theme);

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), eventsData.preferences_theme.themeDialog))
                    .setPositiveButton(R.string.button_ok, null)
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                        if (methodToInvoke != null && idToPass != null && context instanceof AppCompatActivity) {
                            try {
                                Method method = context.getClass().getMethod(methodToInvoke, String.class, int.class);
                                method.invoke(context, Constants.STRING_EMPTY, 0);
                            } catch (Exception ignored) {/**/}
                        }
                        dialog.cancel();
                    });

            if (defaultValue != 0) {
                builder.setNeutralButton(R.string.button_reset, null);
            }

            if (eventsData.preferences_theme.themeEditText != 0) {
                builder.getContext().setTheme(eventsData.preferences_theme.themeEditText);
            } else {
                builder.getContext().setTheme(ContactsEvents.themeEditText_default);
            }

            AlertDialog dialog = builder.create();
            View view = View.inflate(new ContextThemeWrapper(getContext(), eventsData.preferences_theme.themeMain), R.layout.dialog_rgbcolor, null);
            dialog.setView(view);

            if (mSelectDialogTitle == null || mSelectDialogTitle.isEmpty()) {
                View caption = view.findViewById(R.id.caption);
                caption.setVisibility(View.GONE);
            } else {
                TextView titleView = view.findViewById(R.id.title);
                titleView.setText(mSelectDialogTitle);
            }
            ImageView icon = view.findViewById(R.id.icon);
            if (mSelectDialogIcon != 0) {
                icon.setImageResource(mSelectDialogIcon);
            } else {
                icon.setVisibility(View.GONE);
            }

            if (initValue != 0) {
                mValue = initValue;
            }

            TextView color_label = view.findViewById(R.id.color_label);
            color_label.setText(context.getString(R.string.pref_Color_title));

            final int[] colorValue = {mValue};
            TextView color_edit = view.findViewById(R.id.color_edit);
            color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));

            ImageView color_preview = view.findViewById(R.id.color_preview);
            setColorViewValue(color_preview, colorValue[0]);

            TextView seek1_label = view.findViewById(R.id.seek1_label);
            seek1_label.setText(context.getString(R.string.pref_Red_title));
            SeekBar seek1 = view.findViewById(R.id.seek1);
            seek1.setProgress(Color.red(colorValue[0]));
            TextView seek1_progress = view.findViewById(R.id.seek1_progress);
            seek1_progress.setText(String.valueOf(seek1.getProgress()));

            TextView seek2_label = view.findViewById(R.id.seek2_label);
            seek2_label.setText(context.getString(R.string.pref_Green_title));
            SeekBar seek2 = view.findViewById(R.id.seek2);
            seek2.setProgress(Color.green(colorValue[0]));
            TextView seek2_progress = view.findViewById(R.id.seek2_progress);
            seek2_progress.setText(String.valueOf(Color.green(colorValue[0])));

            TextView seek3_label = view.findViewById(R.id.seek3_label);
            seek3_label.setText(context.getString(R.string.pref_Blue_title));
            SeekBar seek3 = view.findViewById(R.id.seek3);
            seek3.setProgress(Color.blue(colorValue[0]));
            TextView seek3_progress = view.findViewById(R.id.seek3_progress);
            seek3_progress.setText(String.valueOf(Color.blue(colorValue[0])));

            TextView seek4_label = view.findViewById(R.id.seek4_label);
            seek4_label.setText(context.getString(R.string.pref_Alpha_title));
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
                        setColor(colorInt);
                        eventsData.setRecentColor(colorInt);

                        if (methodToInvoke != null && idToPass != null && context instanceof AppCompatActivity) {
                            try {
                                Method method = context.getClass().getMethod(methodToInvoke, String.class, int.class);
                                method.invoke(context, idToPass, colorInt);
                            } catch (Exception ignored) {/**/}
                        }
                        dialog.cancel();
                    } catch (IllegalArgumentException e) {
                        ToastExpander.showInfoMsg(getContext(), eventsData.getResources().getString(R.string.msg_color_parse_error));
                    }
                });
                final View buttonBar = (View) buttonPositive.getParent();
                buttonBar.setBackgroundColor(ta.getColor(R.styleable.Theme_editTextBackgroundCustom, 0));

                if (defaultValue != 0) {
                    final Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    buttonNeutral.setOnClickListener(v -> {
                        colorValue[0] = defaultValue;
                        color_edit.setText(ContactsEvents.toARGBString(colorValue[0]));
                        seek1.setProgress(Color.red(colorValue[0]));
                        seek2.setProgress(Color.green(colorValue[0]));
                        seek3.setProgress(Color.blue(colorValue[0]));
                        seek4.setProgress(255 - Color.alpha(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                    });
                }
            });

            dialog.setOnDismissListener(dialogM -> ta.recycle());
            dialog.show();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public int getColor() {return mValue;}

    public void onClick(View v) {

        try {

            selectColor(0, 0, null, null);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void selectColor(int initValue, int defaultValue, String methodToInvoke, String idToPass) {
        try {

            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View rootView = layoutInflater.inflate(R.layout.dialog_colors, null);

            mAdapter = new ColorGridAdapter(getContext());
            if (initValue != 0) {
                mAdapter.setSelectedColor(initValue);
            } else if (defaultValue != 0) {
                mAdapter.setSelectedColor(defaultValue);
            } else {
                mAdapter.setSelectedColor(mValue);
            }

            AlertDialog.Builder colorDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), eventsData.preferences_theme.themeDialog))
                    .setTitle(mSelectDialogTitle)
                    .setIcon(mSelectDialogIcon)
                    .setView(rootView);

            if (eventsData.preferences_extrafun) {
                colorDialogBuilder.setNeutralButton(R.string.button_rgb, (dialog, which) -> {
                    dialog.dismiss();
                    selectRGBColor(initValue, defaultValue, methodToInvoke, idToPass);
                });
            }

            AlertDialog alertToShow = colorDialogBuilder.create();

            GridView mColorGrid = rootView.findViewById(R.id.color_grid);
            if (mColorGrid != null) {
                mColorGrid.setNumColumns(mNumColumns);
                mColorGrid.setAdapter(mAdapter);
                mColorGrid.setOnItemClickListener((listView, view, position, itemId) -> {
                    int colorInt = mAdapter.getItem(position);
                    setColor(colorInt);
                    eventsData.setRecentColor(colorInt);

                    if (methodToInvoke != null && idToPass != null && context instanceof AppCompatActivity) {
                        try {
                            Method method = context.getClass().getMethod(methodToInvoke, String.class, int.class);
                            method.invoke(context, idToPass, colorInt);
                        } catch (Exception ignored) {/**/}
                    }
                    alertToShow.dismiss();
                });
                mColorGrid.setOnItemLongClickListener((parent, view, position, id) -> {
                    Toast.makeText(context,
                            context.getString(R.string.pref_Color_title) +
                                    Constants.STRING_SPACE +
                                    ContactsEvents.toARGBString(mAdapter.getItem(position))
                            , Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
            View mCaptionView = rootView.findViewById(R.id.caption);
            if (mCaptionView != null) {
                mCaptionView.setVisibility(View.GONE);
            }

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setColorViewValue(View view, int color) {

        try {
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

                // А stroke to dark version of color
                int darkenedColor = Color.rgb(
                        Color.red(color) * 192 / 256,
                        Color.green(color) * 192 / 256,
                        Color.blue(color) * 192 / 256);


                colorChoiceDrawable.setSize(radius, radius);
                colorChoiceDrawable.setColor(color);
                colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics()), darkenedColor);
                imageView.setImageDrawable(colorChoiceDrawable);
                imageView.setContentDescription(Integer.toString(color));

            } else if (view instanceof TextView) {
                ((TextView) view).setTextColor(color);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private class ColorGridAdapter extends BaseAdapter {
        private final List<Integer> mChoices = new ArrayList<>();
        private int mSelectedColor;

        private ColorGridAdapter(Context context) {
            try {
                for (int color : mColorChoices) {
                    mChoices.add(color);
                }

                //Добавляем текущий и недавние цвета
                if (!mChoices.contains(mValue)) {
                    mChoices.add(mValue);
                }

                for (int valueInt : eventsData.preferences_RecentColors) {
                    if (!mChoices.contains(valueInt)) {
                        mChoices.add(valueInt);
                    }
                }


            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            View convertedView;
            if (convertView == null) {
                convertedView = LayoutInflater.from(getContext()).inflate(mItemLayoutId, container, false);
            } else {
                convertedView = convertView;
            }

            int color = getItem(position);
            setColorViewValue(convertedView.findViewById(R.id.color_view), color);
            convertedView.setBackgroundColor(color == mSelectedColor ? 0x6633b5e5 : 0);
            return convertedView;
        }

        public void setSelectedColor(int selectedColor) {
            if (!mChoices.contains(selectedColor)) {
                mChoices.add(selectedColor);
            }
            mSelectedColor = selectedColor;
            notifyDataSetChanged();
        }

        public int getSelectedColor() {
            return mSelectedColor;
        }

    }

}
