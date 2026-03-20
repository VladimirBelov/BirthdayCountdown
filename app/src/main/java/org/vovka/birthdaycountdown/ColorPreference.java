/*
 * *
 *  * Created by Vladimir Belov on 20.03.2026, 21:02
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 20.03.2026, 20:46
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
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
import android.view.MotionEvent;
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

import org.vovka.birthdaycountdown.utils.ImageUtils;

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
class ColorPreference extends Preference {
    private static final String TAG = "ColorPreference";
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mDefaultValue = 0;
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
            mDefaultValue = ta.getInt(R.styleable.ColorPreference_defaultValue, 0);
            int choicesResId = ta.getResourceId(R.styleable.ColorPreference_choices, R.array.default_color_choice_values);
            if (choicesResId > 0) {
                //https://stackoverflow.com/questions/9114587/how-can-i-save-colors-in-array-xml-and-get-its-back-to-color-array
                mColorChoices = ta.getResources().getIntArray(choicesResId);
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
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

    public int getDefaultValue() {
        return mDefaultValue;
    }

    public static class ColorDialogFragment extends DialogFragment {
        private ColorPreference mPreference;
        private ColorGridAdapter mAdapter;
        private GridView mColorGrid;

        public static ColorDialogFragment newInstance() {
            return new ColorDialogFragment();
        }

        void setPreference(ColorPreference preference) {
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
                LayoutInflater layoutInflater = getActivity().getLayoutInflater();
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
                                    ImageUtils.toARGBString(mAdapter.getItem(position))
                            , Toast.LENGTH_SHORT).show();
                    return true;
                });

                tryBindLists();

                colorDialogBuilder.setView(rootView);
                colorDialogBuilder.setNeutralButton(R.string.button_rgb, (dialog, which) -> {
                    dialog.dismiss();
                    selectRGBColor(ContactsEvents.getInstance());
                });
                int defaultValue = mPreference.getDefaultValue();
                if (defaultValue != 0 && defaultValue != mPreference.getValue()) {
                    colorDialogBuilder.setPositiveButton(R.string.button_reset, (dialog, which) -> {
                        mPreference.setValue(defaultValue);
                    });
                }

            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getActivity(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }

            return colorDialogBuilder.create();
        }

        @SuppressLint("ClickableViewAccessibility")
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
                color_edit.setText(ImageUtils.toARGBString(colorValue[0]));

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

                // === ЦВЕТОВОЙ СПЕКТР ===
                ImageView colorSpectrum = view.findViewById(R.id.color_spectrum);
                final View colorMarker = view.findViewById(R.id.color_marker);
                final int spectrumWidth = 400;
                final int spectrumHeight = 200;
                final float[] currentHsv = new float[3];
                Color.colorToHSV(colorValue[0], currentHsv);

                // Создаём полный HSV спектр
                Bitmap spectrumBitmap = createFullHSVSpectrumBitmap(spectrumWidth, spectrumHeight);
                colorSpectrum.setImageBitmap(spectrumBitmap);

                // Метод для обновления позиции маркера
                Runnable updateMarkerPosition = () -> {
                    if (colorSpectrum.getWidth() > 0 && colorSpectrum.getHeight() > 0) {
                        Color.colorToHSV(colorValue[0], currentHsv);

                        float hue = currentHsv[0];        // 0-360
                        float saturation = currentHsv[1]; // 0-1
                        float value = currentHsv[2];      // 0-1

                        float x;
                        // === ИСПРАВЛЕНО: порог saturation = 1.0, а не 0.15 ===
                        if (saturation < 1.0f) {
                            // Мы в левой 15% зоне (серая зона)
                            // saturation: 0..1.0 -> x: 0..15% ширины
                            x = saturation * (colorSpectrum.getWidth() * 0.15f);
                        } else {
                            // Мы в правой 85% зоне (цветовая зона)
                            // hue: 0-360 -> x: 15%-100% ширины
                            x = (0.15f * colorSpectrum.getWidth()) +
                                    ((hue / 360.0f) * (0.85f * colorSpectrum.getWidth()));
                        }
                        // =================================================================

                        float y = (1.0f - value) * colorSpectrum.getHeight();

                        float markerX = x - colorMarker.getWidth() / 2f;
                        float markerY = y - colorMarker.getHeight() / 2f;

                        markerX = Math.max(0, Math.min(markerX, colorSpectrum.getWidth() - colorMarker.getWidth()));
                        markerY = Math.max(0, Math.min(markerY, colorSpectrum.getHeight() - colorMarker.getHeight()));

                        colorMarker.setX(markerX);
                        colorMarker.setY(markerY);
                    }
                };

                // Устанавливаем начальную позицию маркера
                colorSpectrum.post(updateMarkerPosition);

                // Обработчик касаний по спектру
                colorSpectrum.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN ||
                            event.getAction() == MotionEvent.ACTION_MOVE) {

                        float x = event.getX();
                        float y = event.getY();

                        int viewWidth = colorSpectrum.getWidth();
                        int viewHeight = colorSpectrum.getHeight();

                        if (viewWidth > 0 && viewHeight > 0) {
                            // Ограничиваем координаты границами ImageView
                            x = Math.max(0, Math.min(x, viewWidth - 1));
                            y = Math.max(0, Math.min(y, viewHeight - 1));

                            float hue;
                            float saturation;

                            // Левые 15%: градиент от белого к цвету (насыщенность растёт)
                            if (x < viewWidth * 0.15f) {
                                saturation = x / (viewWidth * 0.15f);
                                hue = 0; // Для белого/серого hue не важен
                            } else {
                                // Правые 85%: полная насыщенность, меняется hue
                                saturation = 1.0f;
                                // === ВАЖНО: hue считаем от правой 85% зоны ===
                                hue = ((x - (viewWidth * 0.15f)) / (viewWidth * 0.85f)) * 360.0f;
                            }

                            float value = 1.0f - (y / viewHeight);
                            int alpha = Color.alpha(colorValue[0]);
                            int newColor = Color.HSVToColor(alpha, new float[]{hue, saturation, value});

                            colorValue[0] = newColor;

                            seek1.setProgress(Color.red(newColor));
                            seek2.setProgress(Color.green(newColor));
                            seek3.setProgress(Color.blue(newColor));

                            seek1_progress.setText(String.valueOf(Color.red(newColor)));
                            seek2_progress.setText(String.valueOf(Color.green(newColor)));
                            seek3_progress.setText(String.valueOf(Color.blue(newColor)));

                            color_edit.setText(ImageUtils.toARGBString(newColor));
                            setColorViewValue(color_preview, newColor);

                            // Обновляем позицию маркера
                            updateMarkerPosition.run();
                        }
                    }
                    return true;
                });
                // === КОНЕЦ ЦВЕТОВОГО СПЕКТРА ===

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
                            // Обновляем позицию маркера
                            colorSpectrum.post(updateMarkerPosition);
                        } catch (Exception e) { /**/ }
                    }
                });

                seek1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek1_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        //color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                        // Обновляем позицию маркера
                        colorSpectrum.post(updateMarkerPosition);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Обновляем текст только когда пользователь отпустил ползунок
                        color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
                    }
                });

                seek2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek2_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        //color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                        // Обновляем позицию маркера
                        colorSpectrum.post(updateMarkerPosition);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Обновляем текст только когда пользователь отпустил ползунок
                        color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
                    }
                });

                seek3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek3_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        //color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
                        setColorViewValue(color_preview, colorValue[0]);
                        // Обновляем позицию маркера
                        colorSpectrum.post(updateMarkerPosition);
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Обновляем текст только когда пользователь отпустил ползунок
                        color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
                    }
                });

                seek4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seek4_progress.setText(String.valueOf(progress));
                        colorValue[0] = Color.argb(255 - seek4.getProgress(), seek1.getProgress(), seek2.getProgress(), seek3.getProgress());
                        color_edit.setText(ImageUtils.toARGBString(colorValue[0]));
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

        /**
         * Создаёт Bitmap с полным HSV спектром:
         * - По горизонтали: слева насыщенный белый -> цвет, справа полный цвет
         * - По вертикали: Value (1.0-0.0) - сверху яркий, снизу тёмный
         * Левые 15%: градиент насыщенности от 0 до 1
         * Остальное: полная насыщенность с изменением Hue
         */
        @SuppressWarnings("SameParameterValue")
        private Bitmap createFullHSVSpectrumBitmap(int width, int height) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int[] pixels = new int[width * height];

            float grayscaleWidth = width * 0.15f;

            for (int y = 0; y < height; y++) {
                float value = 1.0f - (float) y / height;
                for (int x = 0; x < width; x++) {
                    int color;

                    if (x < grayscaleWidth) {
                        // Левая часть: градиент от белого к цвету
                        float saturation = x / grayscaleWidth;
                        float hue = 0; // Для белого/серого hue не важен
                        color = Color.HSVToColor(new float[]{hue, saturation, value});
                    } else {
                        // Правая часть: полная насыщенность, меняем hue
                        float hue = ((x - grayscaleWidth) / (width - grayscaleWidth)) * 360.0f;
                        color = Color.HSVToColor(new float[]{hue, 1.0f, value});
                    }

                    pixels[y * width + x] = color;
                }
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
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