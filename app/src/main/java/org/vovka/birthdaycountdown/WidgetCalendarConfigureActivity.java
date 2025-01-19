/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 17.01.2024, 22:18
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LocaleManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class WidgetCalendarConfigureActivity extends AppCompatActivity {

    private static final String TAG = "CalendarConfigActivity";
    private int widgetId = 0;
    private ContactsEvents eventsData;
    List<String> widgetPref;
    private final List<String> eventSourcesIds = new ArrayList<>();
    private final List<String> eventSourcesTitles = new ArrayList<>();
    private List<String> eventSourcesSelected = new ArrayList<>();
    private final HashMap<String, Integer> eventSourcesColors = new HashMap<>();
    private AppCompatActivity thisActivity;
    private int customMonthShift = 0;
    private boolean isNewPinnedWidget;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        TypedArray ta = null;

        try {

            super.onCreate(savedInstanceState);

            thisActivity = this;
            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources applicationRes = getBaseContext().getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    LocaleList list = getSystemService(LocaleManager.class).getApplicationLocales();
                    if (!list.isEmpty()) {
                        locale = getSystemService(LocaleManager.class).getApplicationLocales().get(0);
                    }
                }
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeMain);
            setContentView(R.layout.widget_calendar_config);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);
            toolbar.setTitle(R.string.window_widget_settings);

            //Цвет заголовка окна
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            //Отступы всего окна
            if (eventsData.preferences_list_margin > 0) {
                RelativeLayout main = findViewById(R.id.layout_main);
                main.setPadding(
                        main.getPaddingLeft() + (int) (eventsData.preferences_list_margin * eventsData.displayMetrics_density + 0.5f),
                        main.getPaddingTop(),
                        main.getPaddingRight() + (int) (eventsData.preferences_list_margin * eventsData.displayMetrics_density + 0.5f),
                        main.getPaddingBottom()
                );
            }

            setResult(RESULT_CANCELED);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                widgetId = extras.getInt(Constants.PARAM_APP_WIDGET_ID, 0);
                if (extras.containsKey(Constants.EXTRA_NEW_WIDGET)) isNewPinnedWidget = true;
            }
            if (widgetId == 0) return;

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            String widgetType;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            } else {
                widgetType = Constants.WIDGET_TYPE_CALENDAR;
            }

            widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);
            if (widgetId > 0 && eventsData.preferences_debug_on) {
                toolbar.setTitle(getString(R.string.window_widget_settings)
                        .concat(Constants.STRING_PARENTHESIS_OPEN)
                        .concat(Constants.STRING_ID)
                        .concat(Constants.STRING_COLON_SPACE)
                        .concat(String.valueOf(widgetId))
                        .concat(Constants.STRING_PARENTHESIS_CLOSE)
                );
            }

            //Количество месяцев
            try {
                String prefLayout = getString(R.string.widget_config_layout_default);
                if (!widgetPref.isEmpty()) prefLayout = widgetPref.get(0);

                final Spinner spinnerLayout = findViewById(R.id.spinnerMonthsLayout);
                List<String> layouts = Arrays.asList(getResources().getStringArray(R.array.widget_config_layout_values));
                spinnerLayout.setSelection(layouts.lastIndexOf(prefLayout));
            } catch (Exception e) {/**/}

            //Стартовый месяц
            try {
                String prefMonthsShift = getString(R.string.widget_config_month_shift_current_month_id);
                if (widgetPref.size() > 1) prefMonthsShift = widgetPref.get(1);

                final Spinner spinnerMonthShift = findViewById(R.id.spinnerMonthsShift);
                List<String> shifts = Arrays.asList(getResources().getStringArray(R.array.widget_config_month_shift_entries));
                if (shifts.lastIndexOf(prefMonthsShift) > -1) {
                    spinnerMonthShift.setSelection(shifts.lastIndexOf(prefMonthsShift));
                } else if (prefMonthsShift.equals(getString(R.string.widget_config_month_shift_current_month_id))) { //Текущий месяц
                    spinnerMonthShift.setSelection(shifts.lastIndexOf(getString(R.string.widget_config_month_shift_current_month)));
                } else if (prefMonthsShift.equals(getString(R.string.widget_config_month_shift_january_id))) { //Начало года
                    spinnerMonthShift.setSelection(shifts.lastIndexOf(getString(R.string.widget_config_month_shift_january)));
                }
            } catch (Exception e) {/**/}

            //Положение
            try {
                int prefStartingMonthPosition = 0;
                if (widgetPref.size() > 2) prefStartingMonthPosition = Integer.parseInt(widgetPref.get(2));

                final Spinner spinnerStartingMonthPosition = findViewById(R.id.spinnerStartingMonthPosition);
                spinnerStartingMonthPosition.setSelection(prefStartingMonthPosition);
            } catch (Exception e) {/**/}

            //Ручное смещение месяцев
            try {
                if (widgetPref.size() > 3) customMonthShift = Integer.parseInt(widgetPref.get(3));
            } catch (Exception e) {/**/}

            //Элементы календаря
            try {
                List<String> elementsIDs = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_values));
                List<String> elementsValues = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_entries));
                String[] prefElements = null;
                List<String> selectedElements = new ArrayList<>();

                if (widgetPref.size() > 4) prefElements = widgetPref.get(4).split(Constants.REGEX_PLUS, -1);
                if (prefElements != null) {
                    for (String item : prefElements) {
                        if (elementsIDs.contains(item)) selectedElements.add(elementsValues.get(elementsIDs.indexOf(item)));
                    }
                }

                final MultiSelectionSpinner spinnerElements = findViewById(R.id.spinnerElements);
                spinnerElements.setItems(elementsValues);
                spinnerElements.setSelection(selectedElements);
            } catch (Exception e) {/**/}

            //Источники событий
            getEventSources();
            if (widgetPref.size() > 5) {
                String pref = widgetPref.get(5);
                if (!pref.isEmpty()) eventSourcesSelected = new ArrayList<>(Arrays.asList(pref.split(Constants.REGEX_PLUS, -1)));
            }

            //Цвета событий
            try {
                if (widgetPref.size() > 14) {
                    String pref = widgetPref.get(14);
                    if (!pref.isEmpty()) {
                        List<String> prefEventsColors = new ArrayList<>(Arrays.asList(pref.split(Constants.REGEX_PLUS, -1)));
                        for (String color : prefEventsColors) {
                            String[] colors = color.split(Constants.STRING_COLON, -1);
                            if (colors.length == 2) {
                                try {
                                    Integer colorValue = Integer.parseInt(colors[1]);
                                    eventSourcesColors.put(colors[0], colorValue);
                                } catch (NumberFormatException ignored) {/**/}
                            }
                        }
                    }
                }

                updateEventSources();
                TextView listEventSources = findViewById(R.id.listEventSources);
                listEventSources.setOnClickListener(v -> selectEventSources());
            } catch (Exception e) {/**/}

            //Размер шрифта
            try {
                int prefFontMagnify = 0;
                if (widgetPref.size() > 6) prefFontMagnify = Integer.parseInt(widgetPref.get(6));

                SeekBar seekFontMagnify = findViewById(R.id.seekFontMagnify);
                seekFontMagnify.setMax(25);
                seekFontMagnify.setProgress(prefFontMagnify + 5);

                TextView valueFontMagnify = findViewById(R.id.valueFontMagnify);
                valueFontMagnify.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnify.getProgress() - 5) * 10)));

                seekFontMagnify.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        valueFontMagnify.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnify.getProgress() - 5) * 10)));
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            } catch (Exception e) {/**/}

            //Реакция на нажатие
            try {
                List<String> onclickHolidaysIDs = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_holidays_values));
                List<String> onclickCommonIDs = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_common_values));
                String[] prefOnClick = null;
                int prefOnClickCommon = Constants.onClick_None;
                int prefOnClickHolidays = Constants.onClick_None;

                if (widgetPref.size() > 15) prefOnClick = widgetPref.get(15).split(Constants.REGEX_PLUS, -1);
                if (prefOnClick != null && prefOnClick.length == 2) {
                    if (onclickCommonIDs.contains(prefOnClick[0])) {
                        prefOnClickCommon = onclickCommonIDs.indexOf(prefOnClick[0]); //Смещение, не значение
                    }
                    if (onclickHolidaysIDs.contains(prefOnClick[1])) {
                        prefOnClickHolidays = onclickHolidaysIDs.indexOf(prefOnClick[1]);
                    }
                }

                final Spinner spinnerOnClickCommon = findViewById(R.id.spinnerOnClickCommon);
                spinnerOnClickCommon.setSelection(prefOnClickCommon);
                final Spinner spinnerOnClickHolidays = findViewById(R.id.spinnerOnClickHolidays);
                spinnerOnClickHolidays.setSelection(prefOnClickHolidays);
            } catch (Exception e) { /**/ }

            //Цвета

            //Фон виджета
            try {
                int colorWidgetBackground = 0;

                if (widgetPref.size() > 7 && !widgetPref.get(7).isEmpty()) {
                    try {
                        colorWidgetBackground = Color.parseColor(widgetPref.get(7));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorWidgetBackground == 0) {
                    colorWidgetBackground = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_Back_default);
                }
                final ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackground);
                colorWidgetBackgroundPicker.setColor(colorWidgetBackground);
            } catch (final Exception e) { /**/ }

            //Обычные дни
            try {
                int colorCommon = 0;
                if (widgetPref.size() > 8 && !widgetPref.get(8).isEmpty()) {
                    try {
                        colorCommon = Color.parseColor(widgetPref.get(8));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorCommon == 0) {
                    colorCommon = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_Common_default);
                }
                final ColorPicker colorCommonPicker = findViewById(R.id.colorCommon);
                colorCommonPicker.setColor(colorCommon);
            } catch (final Exception e) { /**/ }

            //Сегодня
            try {
                int colorToday = 0;
                if (widgetPref.size() > 13 && !widgetPref.get(13).isEmpty()) {
                    try {
                        colorToday = Color.parseColor(widgetPref.get(13));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorToday == 0) {
                    colorToday = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_Today_default);
                }
                final ColorPicker colorTodayPicker = findViewById(R.id.colorToday);
                colorTodayPicker.setColor(colorToday);
            } catch (final Exception e) { /**/ }

            //Заголовок
            try {
                int colorMonthTitle = 0;
                if (widgetPref.size() > 9 && !widgetPref.get(9).isEmpty()) {
                    try {
                        colorMonthTitle = Color.parseColor(widgetPref.get(9));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorMonthTitle == 0) {
                    colorMonthTitle = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_MonthTitle_default);
                }
                final ColorPicker colorHeaderPicker = findViewById(R.id.colorMonthTitle);
                colorHeaderPicker.setColor(colorMonthTitle);
            } catch (final Exception e) { /**/ }

            //Фон заголовка
            try {
                int colorHeaderBack = 0;
                if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                    try {
                        colorHeaderBack = Color.parseColor(widgetPref.get(10));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorHeaderBack == 0) {
                    colorHeaderBack = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_HeaderBack_default);
                }
                final ColorPicker colorHeaderBackPicker = findViewById(R.id.colorHeaderBack);
                colorHeaderBackPicker.setColor(colorHeaderBack);
            } catch (final Exception e) { /**/ }

            //Стрелки
            try {
                int colorArrows = 0;
                if (widgetPref.size() > 11 && !widgetPref.get(11).isEmpty()) {
                    try {
                        colorArrows = Color.parseColor(widgetPref.get(11));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorArrows == 0) {
                    colorArrows = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_Arrows_default);
                }
                final ColorPicker colorArrowsPicker = findViewById(R.id.colorArrows);
                colorArrowsPicker.setColor(colorArrows);
            } catch (final Exception e) { /**/ }

            //Дни недели
            try {
                int colorWeeks = 0;
                if (widgetPref.size() > 12 && !widgetPref.get(12).isEmpty()) {
                    try {
                        colorWeeks = Color.parseColor(widgetPref.get(12));
                    } catch (IllegalArgumentException ignored) { /**/ }
                }

                if (colorWeeks == 0) {
                    colorWeeks = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_Weeks_default);
                }
                final ColorPicker colorWeeksPicker = findViewById(R.id.colorWeeks);
                colorWeeksPicker.setColor(colorWeeks);
            } catch (final Exception e) { /**/ }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            updateVisibility();
            if (ta != null) ta.recycle();
        }
    }

    public void buttonOkOnClick(final View view) {
        try {

            final Spinner spinnerLayout = findViewById(R.id.spinnerMonthsLayout);
            List<String> layouts = Arrays.asList(getResources().getStringArray(R.array.widget_config_layout_values));
            final String layoutId = spinnerLayout.getSelectedItemPosition() <= layouts.size() - 1 ?
                    layouts.get(spinnerLayout.getSelectedItemPosition()) : getString(R.string.widget_config_layout_default);

            final Spinner spinnerMonthShift = findViewById(R.id.spinnerMonthsShift);
            String selectedShift = (String) spinnerMonthShift.getSelectedItem();
            if (selectedShift.equals(getString(R.string.widget_config_month_shift_current_month))) {
                selectedShift = getString(R.string.widget_config_month_shift_current_month_id);
            } else if (selectedShift.equals(getString(R.string.widget_config_month_shift_january))) {
                selectedShift = getString(R.string.widget_config_month_shift_january_id);
            }

            final Spinner spinnerStartingMonthPosition = findViewById(R.id.spinnerStartingMonthPosition);
            int prefStartingMonthPosition = spinnerStartingMonthPosition.getSelectedItemPosition();
            if (prefStartingMonthPosition == Spinner.INVALID_POSITION) prefStartingMonthPosition = 0;

            List<String> elementsIDs = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_values));
            List<String> elementsValues = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_entries));
            final MultiSelectionSpinner spinnerElements = findViewById(R.id.spinnerElements);
            final StringBuilder selectedElements = new StringBuilder();
            for(final String item: spinnerElements.getSelectedStrings()) {
                if (selectedElements.length() > 0) selectedElements.append(Constants.STRING_PLUS);
                selectedElements.append(elementsIDs.get(elementsValues.indexOf(item)));
            }

            final String eventSources = String.join(Constants.STRING_PLUS, eventSourcesSelected);

            SeekBar seekFontMagnify = findViewById(R.id.seekFontMagnify);

            List<String> onclickHolidaysIDs = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_holidays_values));
            List<String> onclickCommonIDs = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_common_values));
            List<String> onclickValues = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_holidays_entries));
            final Spinner spinnerOnClickCommon = findViewById(R.id.spinnerOnClickCommon);
            final Spinner spinnerOnClickHolidays = findViewById(R.id.spinnerOnClickHolidays);
            final String selectedOnClick = onclickCommonIDs.get(spinnerOnClickCommon.getSelectedItemPosition())
                    .concat(Constants.STRING_PLUS).concat(onclickHolidaysIDs.get(spinnerOnClickHolidays.getSelectedItemPosition()));

            final ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackground);
            final int colorWidgetBackground = colorWidgetBackgroundPicker.getColor();
            final String selectedWidgetBackground = colorWidgetBackground != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Back_default)
                    ? ContactsEvents.toARGBString(colorWidgetBackground) : Constants.STRING_EMPTY;

            final ColorPicker colorCommonPicker = findViewById(R.id.colorCommon);
            final int colorCommon = colorCommonPicker.getColor();
            final String selectedCommon = colorCommon != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Common_default)
                    ? ContactsEvents.toARGBString(colorCommon) : Constants.STRING_EMPTY;

            final ColorPicker colorMonthTitlePicker = findViewById(R.id.colorMonthTitle);
            final int colorMonthTitle = colorMonthTitlePicker.getColor();
            final String selectedMonthTitle = colorMonthTitle != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_MonthTitle_default)
                    ? ContactsEvents.toARGBString(colorMonthTitle) : Constants.STRING_EMPTY;

            final ColorPicker colorHeaderBackPicker = findViewById(R.id.colorHeaderBack);
            final int colorHeaderBack = colorHeaderBackPicker.getColor();
            final String selectedHeaderBack = colorHeaderBack != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_HeaderBack_default)
                    ? ContactsEvents.toARGBString(colorHeaderBack) : Constants.STRING_EMPTY;

            final ColorPicker colorArrowsPicker = findViewById(R.id.colorArrows);
            final int colorArrows = colorArrowsPicker.getColor();
            final String selectedArrows = colorArrows != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Arrows_default)
                    ? ContactsEvents.toARGBString(colorArrows) : Constants.STRING_EMPTY;

            final ColorPicker colorWeeksPicker = findViewById(R.id.colorWeeks);
            final int colorWeeks = colorWeeksPicker.getColor();
            final String selectedWeeks = colorWeeks != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Weeks_default)
                    ? ContactsEvents.toARGBString(colorWeeks) : Constants.STRING_EMPTY;

            final ColorPicker colorTodayPicker = findViewById(R.id.colorToday);
            final int colorToday = colorTodayPicker.getColor();
            final String selectedToday = colorToday != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Today_default)
                    ? ContactsEvents.toARGBString(colorToday) : Constants.STRING_EMPTY;

            List<String> listColors = new ArrayList<>();
            for (String sourceId : eventSourcesColors.keySet()) {
                @Nullable Integer colorValue = eventSourcesColors.get(sourceId);

                if (colorValue != null) {

                    if (sourceId.equals(getString(R.string.widget_config_month_events_saturday_id)) && colorValue ==
                            ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Saturday_default)) {
                        colorValue = null;
                    } else if (sourceId.equals(getString(R.string.widget_config_month_events_sunday_id)) && colorValue ==
                            ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Sunday_default)) {
                        colorValue = null;
                    } else if (colorValue == ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_default)) {
                        colorValue = null;
                    } else if (!eventSourcesSelected.contains(sourceId)) {
                        colorValue = null;
                    }

                    if (colorValue != null) listColors.add(sourceId.concat(Constants.STRING_COLON).concat(String.valueOf(colorValue)));
                }
            }


            //Сохранение настроек

            List<String> prefsToStore = new ArrayList<>();

            prefsToStore.add(layoutId); //Количество месяцев
            prefsToStore.add(selectedShift); //Стартовый месяц
            prefsToStore.add(String.valueOf(prefStartingMonthPosition)); //Положение (позиция в списке выбора)
            prefsToStore.add(String.valueOf(customMonthShift)); //Ручное смещение месяцев
            prefsToStore.add(selectedElements.toString()); //Элементы
            prefsToStore.add(eventSources); //Источники событий (через +)
            prefsToStore.add(String.valueOf(seekFontMagnify.getProgress() - 5)); //Размер шрифта
            prefsToStore.add(selectedWidgetBackground); //Цвет подложки
            prefsToStore.add(selectedCommon); //Обычные дни
            prefsToStore.add(selectedMonthTitle); //Заголовок
            prefsToStore.add(selectedHeaderBack); //Фон заголовка
            prefsToStore.add(selectedArrows); //Стрелки
            prefsToStore.add(selectedWeeks); //Дни недели
            prefsToStore.add(selectedToday); //Сегодня
            prefsToStore.add(String.join(Constants.STRING_PLUS, listColors));
            prefsToStore.add(selectedOnClick);

            this.eventsData.setWidgetPreference(this.widgetId, String.join(Constants.STRING_COMMA, prefsToStore));

            final Intent intent = new Intent();
            intent.putExtra(Constants.PARAM_APP_WIDGET_ID, this.widgetId);
            setResult(Activity.RESULT_OK, intent);

            //Посылаем сообщение на обновление виджета
            this.eventsData.updateWidgets(this.widgetId, null);

            finish();
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void buttonCancelOnClick(final View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putInt(Constants.PARAM_APP_WIDGET_ID, this.widgetId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.widgetId = savedInstanceState.getInt(Constants.PARAM_APP_WIDGET_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_widget_config, menu);

        MenuItem itemHelp = menu.findItem(R.id.menu_help_widgets);
        if (itemHelp != null) {
            itemHelp.setVisible(eventsData.isContextHelpAvailable());
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {

        final int itemId = item.getItemId();

        if (itemId == R.id.menu_help_widgets) {

            Intent intent = new Intent(this, FAQActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(Constants.EXTRA_ANCHOR, getString(R.string.faq_anchor_widgets_calendar));
            try {
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) { /**/ }

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("DiscouragedApi")
    private void getEventSources() {
        try {

            eventSourcesIds.add(getString(R.string.widget_config_month_events_saturday_id));
            eventSourcesTitles.add(getString(R.string.month_event_saturdays));
            eventSourcesColors.put(getString(R.string.widget_config_month_events_saturday_id),
                    ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Saturday_default));

            eventSourcesIds.add(getString(R.string.widget_config_month_events_sunday_id));
            eventSourcesTitles.add(getString(R.string.month_event_sundays));
            eventSourcesColors.put(getString(R.string.widget_config_month_events_sunday_id),
                    ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Sunday_default));

            //Справочники праздников и выходных
            int eventsPackCount = 1;
            int packId = getResources().getIdentifier(Constants.STRING_TYPE_HOLIDAY + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, getPackageName());
            while (packId > 0) {
                try {
                    String[] eventsPack = getResources().getStringArray(packId);

                    eventSourcesIds.add(ContactsEvents.getHash(Constants.eventSourceHolidayPrefix + eventsPack[0]));
                    eventSourcesTitles.add(eventsPack[0]);

                } catch (Resources.NotFoundException ignored) { /**/ }

                eventsPackCount++;
                packId = getResources().getIdentifier(Constants.STRING_TYPE_HOLIDAY + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, getPackageName());
            }

            //События избранных контактов
            eventSourcesIds.add(ContactsEvents.getHash(Constants.eventSourceFavoritePrefix));
            eventSourcesTitles.add(getString(R.string.widget_config_events_favorites));

            //Календари
            if (!eventsData.checkNoCalendarAccess()){
                if (eventsData.map_calendars.isEmpty()) eventsData.fillCalendarList();
                List<String> allCalendars = new ArrayList<>(eventsData.preferences_HolidayEvent_calendars);
                if (!allCalendars.isEmpty()) {
                    for (String calendar: allCalendars) {
                        if (eventsData.map_calendars.containsKey(calendar)) {
                            String calendarId = ContactsEvents.getHash(Constants.eventSourceCalendarPrefix + calendar);
                            eventSourcesIds.add(calendarId);
                            eventSourcesTitles.add(Constants.eventTitleCalendarPrefix + ContactsEvents.substringBefore(eventsData.map_calendars.get(calendar), Constants.STRING_EOT));
                            if (eventsData.map_calendars_colors.containsKey(calendarId)) {
                                eventSourcesColors.put(calendarId, eventsData.map_calendars_colors.get(calendarId));
                            }
                        }
                    }
                }
            }

            //Файлы
            if (!eventsData.preferences_HolidayEvent_files.isEmpty()) {
                for (String file: eventsData.preferences_HolidayEvent_files) {
                    eventSourcesIds.add(ContactsEvents.getHash(Constants.eventSourceFilePrefix + file));
                    eventSourcesTitles.add(Constants.eventTitleFilePrefix.concat(ContactsEvents.substringBefore(file, Constants.STRING_BAR)));
                }
            }


        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateEventSources() {
        try {

            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            TextView listEventSources = findViewById(R.id.listEventSources);
            StringBuilder sb = new StringBuilder();
            for (String sourceId: eventSourcesSelected) {
                int ind = eventSourcesIds.indexOf(sourceId);
                if (ind > -1) {
                    if (sb.length() > 0) sb.append(Constants.HTML_BR);

                    @ColorInt Integer colorValue;
                    if (eventSourcesColors.containsKey(sourceId) && eventSourcesColors.get(sourceId) != null) {
                        colorValue = eventSourcesColors.get(sourceId);
                    } else {
                        colorValue = ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_default);
                    }

                    if (colorValue != null) {
                        if (Color.alpha(colorValue) == 0) {
                            colorValue = ta.getColor(R.styleable.Theme_backgroundColor, colorValue);
                        }
                        sb.append(String.format(Constants.FONT_COLOR_DOT, Integer.toHexString(colorValue & 0x00ffffff)));
                    }
                    sb.append(eventSourcesTitles.get(ind));

                }
            }
            ta.recycle();

            if (sb.length() == 0) {
                listEventSources.setText(R.string.widget_config_month_sources_empty);
            } else {
                listEventSources.setText(Html.fromHtml(sb.toString()));
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectEventSources() {
        try {

            if (!eventSourcesIds.isEmpty()) {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                List<String> sourceChoices = new ArrayList<>();
                List<Integer> colorDots = new ArrayList<>();

                for (int i = 0; i < eventSourcesIds.size(); i++) {
                    String sourceId = eventSourcesIds.get(i);
                    sourceChoices.add(eventSourcesTitles.get(i));

                    if (eventSourcesColors.containsKey(sourceId)) {
                        colorDots.add(eventSourcesColors.get(sourceId));
                    } else {
                        colorDots.add(ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_default));
                    }

                }

                ListAdapter adapter = new ContactsEvents.MultiCheckboxesAdapter(this, sourceChoices, null, null, colorDots, ta);

                //todo: заголовок на несколько строк https://stackoverflow.com/questions/14439538/how-can-i-change-the-color-of-alertdialog-title-and-the-color-of-the-line-under
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.widget_config_month_events_sources_label)
                        .setIcon(R.drawable.btn_zoom_page_press)
                        .setAdapter(adapter, null)
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                            //https://stackoverflow.com/questions/8326830/how-to-uncheck-item-checked-by-setitemchecked
                            SparseBooleanArray checked = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                            eventSourcesSelected.clear();
                            for (int i = 0; i < checked.size(); i++) {
                                if (checked.get(checked.keyAt(i))) {
                                    eventSourcesSelected.add(eventSourcesIds.get(checked.keyAt(i)));
                                }
                            }
                            updateEventSources();

                        })
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                        .setCancelable(true);

                AlertDialog alertToShow = builder.create();

                ListView listView = alertToShow.getListView();
                listView.setItemsCanFocus(false);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                    //Только здесь работает
                    for (int i = 0; i < sourceChoices.size(); i++) {
                        String title = ContactsEvents.substringBefore(sourceChoices.get(i), Constants.STRING_BRACKETS_OPEN);
                        if (eventSourcesTitles.contains(title)) {
                            if (eventSourcesSelected.contains(eventSourcesIds.get(eventSourcesTitles.indexOf(title)))) {
                                listView.setItemChecked(i, true);
                            }
                        }
                    }

                    listView.setOnItemLongClickListener((parent, view, position, id) -> {

                        ColorPicker picker = new ColorPicker(thisActivity);
                        String sourceId = eventSourcesIds.get(position);
                        Integer colorValue;
                        if (eventSourcesColors.containsKey(sourceId) && eventSourcesColors.get(sourceId) != null) {
                            colorValue = eventSourcesColors.get(sourceId);
                        } else {
                            colorValue = ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_default);
                        }
                        if (colorValue != null) {

                            SparseBooleanArray checked = listView.getCheckedItemPositions();
                            eventSourcesSelected.clear();
                            for (int i = 0; i < checked.size(); i++) {
                                if (checked.get(checked.keyAt(i))) {
                                    eventSourcesSelected.add(eventSourcesIds.get(checked.keyAt(i)));
                                }
                            }
                            alertToShow.dismiss();

                            int colorDefault;
                            Integer colorCalendar = eventsData.map_calendars_colors.get(sourceId);
                            if (sourceId.equals(getString(R.string.widget_config_month_events_saturday_id))) {
                                colorDefault = ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Saturday_default);
                            } else if (sourceId.equals(getString(R.string.widget_config_month_events_sunday_id))) {
                                colorDefault = ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Sunday_default);
                            } else if (colorCalendar != null) {
                                colorDefault = colorCalendar;
                            } else {
                                colorDefault = ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_default);
                            }

                            picker.selectColor(colorValue, colorDefault, "setCustomColor", sourceId);

                        //} else {
                        //    ToastExpander.showInfoMsg(getApplicationContext(), "Ошибка выбора цвета для '" + eventSourcesTitles.get(position) + "'");
                        }

                        return true;
                    });
                });

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateVisibility() {
        try {

            if (isNewPinnedWidget) {
                findViewById(R.id.button_cancel).setVisibility(View.GONE);
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void setCustomColor(@NonNull String colorId, int colorValue) {
        if (!colorId.isEmpty()) {
            ToastExpander.showDebugMsg(getApplicationContext(), getString(R.string.msg_event_color_selected, Integer.toHexString(colorValue & 0x00ffffff), colorId));
            eventSourcesColors.put(colorId, colorValue);
        }
        selectEventSources();
    }

}
