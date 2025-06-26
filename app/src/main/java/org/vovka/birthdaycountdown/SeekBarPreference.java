/*
 * *
 *  * Created by Vladimir Belov on 27.06.2025, 01:34
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 27.06.2025, 01:30
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/** @noinspection unused*/
public class SeekBarPreference extends DialogPreference {

    private static final String TAG = "SeekBarPreference";
    private int defaultValueFromXml = 0;
    private int mProgress;
    private int mMax;
    private int mMin;
    private int step;
    private int minSummaryValue = 0;
    private String mSummaryTemplate; // Поле для хранения шаблона сводки
    private TextView mSeekBarValueTextView;

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SeekBarPreference(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Общая инициализация
     */
    private void init(Context context, AttributeSet attrs) {
        // Устанавливаем макет диалога
        setDialogLayoutResource(R.layout.seekbar_preference_dialog);

        // Получаем пользовательские атрибуты из attrs.xml

        try (
                TypedArray customAttrs = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
                TypedArray defaultAttrs = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.defaultValue})
        ){
            defaultValueFromXml = defaultAttrs.getInt(0, 0);
            mMax = customAttrs.getInt(R.styleable.SeekBarPreference_maxValue, defaultValueFromXml);
            mMin = customAttrs.getInt(R.styleable.SeekBarPreference_minValue, 0);
            step = customAttrs.getInt(R.styleable.SeekBarPreference_step, 1);
            minSummaryValue = customAttrs.getInt(R.styleable.SeekBarPreference_minSummaryValue, 0);
        }

        // --- Захват шаблона сводки ---
        // Получаем исходную строку сводки, которая может содержать "%s"
        CharSequence summary = getSummary();
        if (summary != null) {
            mSummaryTemplate = summary.toString();
        } else {
            mSummaryTemplate = null; // Если summary не установлен, не форматируем
        }

    }

    // Этот метод вызывается для создания View, которое будет отображено в диалоге
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBarValueTextView = view.findViewById(R.id.dialogSeekBarValueTextView);
        SeekBar mSeekBar = view.findViewById(R.id.dialogSeekBar);

        // Устанавливаем минимальное и максимальное значение для SeekBar
        // Note: setMin() доступен с API 26 (Oreo). Для более старых версий нужна своя логика.
        // Если вы поддерживаете API < 26, уберите setMin() и учтите это в логике.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mSeekBar.setMin(mMin);
            mSeekBar.setMax(mMax);
        } else {
            // Для старых API: прогресс будет от 0 до Max-Min, а затем прибавляем Min
            mSeekBar.setMax(mMax - mMin);
            // Чтобы упростить, можно просто установить Max и работать с абсолютными значениями
            // Или реализовать:
            // mProgress = stored_value - mMin; // при загрузке
            // save (mProgress + mMin); // при сохранении
        }

        mSeekBar.setProgress(mProgress); // Устанавливаем текущее значение

        // Обновляем TextView
        mSeekBarValueTextView.setText(String.valueOf(minSummaryValue + step * mProgress));

        // Слушатель для SeekBar
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress; // Обновляем внутреннее состояние
                mSeekBarValueTextView.setText(String.valueOf(minSummaryValue + step * progress)); // Обновляем TextView в диалоге
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Не используем
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Не используем
            }
        });
    }

    // Этот метод вызывается, когда диалог закрывается (OK/Cancel)
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) { // Если пользователь нажал OK
            // Сохраняем значение в SharedPreferences
            // getPersistedInt/persistInt - это методы из Preference, которые работают с SharedPreferences
            if (callChangeListener(mProgress)) { // Уведомляем слушателей (если есть)
                persistInt(mProgress);
            }
            // Обновляем summary для Preference (чтобы оно отображало текущее значение)
            updateSummary(minSummaryValue + step * mProgress);
        }
    }

    // Загружаем начальное значение из SharedPreferences
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // Возвращаем значение по умолчанию, указанное в XML preference:defaultValue
        return a.getInt(index, defaultValueFromXml); // Если defaultValue нет, то defaultValueFromXml
    }

    // Устанавливаем начальное значение для Preference
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int effectiveDefaultValue;

        // Определяем значение по умолчанию, которое будем использовать
        if (restorePersistedValue) {
            // Если восстанавливаем из SharedPreferences, то defaultValue здесь неактуален
            // (или это просто заглушка), используем то, что считали из XML,
            // как дефолт, если в SharedPreferences ничего нет.
            effectiveDefaultValue = defaultValueFromXml;
        } else {
            // Если не восстанавливаем (первая установка), то defaultValue
            // будет содержать значение из android:defaultValue.
            if (defaultValue instanceof Integer) {
                effectiveDefaultValue = (Integer) defaultValue;
            } else if (defaultValue == null) {
                // Этот случай должен быть крайне редким, если android:defaultValue
                // всегда указан в XML. Но мы его обрабатываем.
                effectiveDefaultValue = defaultValueFromXml;
            } else {
                // Если defaultValue другого типа (что тоже крайне редко для int-preference)
                effectiveDefaultValue = defaultValueFromXml;
            }
        }

        mProgress = getPersistedInt(effectiveDefaultValue);

        // Обновляем summary при первом запуске
        updateSummary(minSummaryValue + step * mProgress);
    }

    /**
     * Вспомогательный метод для обновления сводки
     */
    private void updateSummary(int value) {
        if (mSummaryTemplate != null && mSummaryTemplate.contains("%s")) {
            // Если шаблон содержит %s, форматируем его
            setSummary(String.format(mSummaryTemplate, value));
        } else if (mSummaryTemplate != null) {
            // Если шаблон есть, но без %s, просто используем его
            setSummary(mSummaryTemplate);
        } else {
            // Если шаблона нет, можем установить просто числовое значение или оставить пустым
            setSummary(String.valueOf(value));
        }
    }

    // Метод для установки значения программно (если нужно)
    public void setProgress(int progress) {
        this.mProgress = progress;
        persistInt(progress);
        updateSummary(minSummaryValue + step * progress);
    }

    // Метод для получения текущего значения
    public int getProgress() {
        return mProgress;
    }

}
