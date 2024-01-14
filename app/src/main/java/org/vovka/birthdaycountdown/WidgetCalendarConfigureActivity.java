/*
 * *
 *  * Created by Vladimir Belov on 31.12.2023, 17:27
 *  * Copyright (c) 2018 - 2023. All rights reserved.
 *  * Last modified 31.12.2023, 17:27
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
    private final List<String> eventSourcesPackages = new ArrayList<>();
    private final List<Integer> eventSourcesIcons = new ArrayList<>();
    private List<String> eventSourcesSelected = new ArrayList<>();
    private AppCompatActivity thisActivity;

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
            if (eventsData.preferences_list_marging > 0) {
                RelativeLayout main = findViewById(R.id.layout_main);
                main.setPadding(
                        main.getPaddingLeft() + (int) (eventsData.preferences_list_marging * eventsData.displayMetrics_density + 0.5f),
                        main.getPaddingTop(),
                        main.getPaddingRight() + (int) (eventsData.preferences_list_marging * eventsData.displayMetrics_density + 0.5f),
                        main.getPaddingBottom()
                );
            }

            setResult(RESULT_CANCELED);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) widgetId = extras.getInt(Constants.PARAM_APP_WIDGET_ID, 0);
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
//0,0,1,1+2,01+02,1,,
            //Количество месяцев
            String prefLayout = getString(R.string.widget_config_layout_default);
            try {
                if (widgetPref.size() > 0) prefLayout = widgetPref.get(0);
            } catch (Exception e) {/**/}

            final Spinner spinnerLayout = findViewById(R.id.spinnerMonthsLayout);
            List<String> layouts = Arrays.asList(getResources().getStringArray(R.array.widget_config_layout_values));
            spinnerLayout.setSelection(layouts.lastIndexOf(prefLayout));

            //Стартовый месяц
            String prefMonthsShift = getString(R.string.widget_config_month_shift_current_month_id);
            try {
                if (widgetPref.size() > 1) prefMonthsShift = widgetPref.get(1);
            } catch (Exception e) {/**/}

            final Spinner spinnerMonthShift = findViewById(R.id.spinnerMonthsShift);
            List<String> shifts = Arrays.asList(getResources().getStringArray(R.array.widget_config_month_shift_entries));
            if (shifts.lastIndexOf(prefMonthsShift) > -1) {
                spinnerMonthShift.setSelection(shifts.lastIndexOf(prefMonthsShift));
            } else if (prefMonthsShift.equals(getString(R.string.widget_config_month_shift_current_month_id))) { //Текущий месяц
                spinnerMonthShift.setSelection(shifts.lastIndexOf(getString(R.string.widget_config_month_shift_current_month)));
            } else if (prefMonthsShift.equals(getString(R.string.widget_config_month_shift_january_id))) { //Начало года
                spinnerMonthShift.setSelection(shifts.lastIndexOf(getString(R.string.widget_config_month_shift_january)));
            }

            //Положение
            int prefStartingMonthPosition = 0;
            try {
                if (widgetPref.size() > 2) prefStartingMonthPosition = Integer.parseInt(widgetPref.get(2));
            } catch (Exception e) {/**/}

            final Spinner spinnerStartingMonthPosition = findViewById(R.id.spinnerStartingMonthPosition);
            spinnerStartingMonthPosition.setSelection(prefStartingMonthPosition);

            //Элементы календаря
            List<String> elementsIDs = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_values));
            List<String> elementsValues = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_entries));
            String[] prefElements = null;
            List<String> selectedElements = new ArrayList<>();
            try {
                if (widgetPref.size() > 3) prefElements = widgetPref.get(3).split(Constants.REGEX_PLUS, -1);
                if (prefElements != null) {
                    for (String item : prefElements) {
                        if (elementsIDs.contains(item)) selectedElements.add(elementsValues.get(elementsIDs.indexOf(item)));
                    }
                }
            } catch (Exception e) {/**/}

            final MultiSelectionSpinner spinnerElements = findViewById(R.id.spinnerElements);
            spinnerElements.setItems(elementsValues);
            spinnerElements.setSelection(selectedElements);

            //Источники событий
            if (widgetPref.size() > 4) {
                String pref = widgetPref.get(4);
                if (!pref.isEmpty()) eventSourcesSelected = new ArrayList<>(Arrays.asList(pref.split(Constants.REGEX_PLUS, -1)));
            }
            getEventSources();
            updateEventSources();
            TextView listEventSources = findViewById(R.id.listEventSources);
            listEventSources.setOnClickListener(v -> selectEventSources());


            //TextView eventSources = findViewById(R.id.listEventSources);
            //eventSources.setText(HtmlCompat.fromHtml("Субботы<br>Воскресенья<br>\uD83C\uDDF7\uD83C\uDDFA Производственный календарь", 0));

            //Коэффициент масштабирования размера шрифта
            int prefMagnifyIndex = 0;
            try {
                if (widgetPref.size() > 5) prefMagnifyIndex = Integer.parseInt(widgetPref.get(5));
            } catch (Exception e) {/**/}

            Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            spinnerMagnify.setSelection(prefMagnifyIndex, true);

            //Цвета
            int colorWidgetBackground = 0;
            if (widgetPref.size() > 6 && !widgetPref.get(6).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(6));
                } catch (final Exception e) { /* */}
            }
            if (colorWidgetBackground == 0) {
                colorWidgetBackground = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_WidgetBackground_default);
            }
            final ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
            colorWidgetBackgroundPicker.setColor(colorWidgetBackground);

            //Скрываем недоступные параметры

            if (this.eventsData.checkNoBatteryOptimization()) findViewById(R.id.hintBatteryOptimization).setVisibility(View.GONE);

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (ta != null) ta.recycle();
        }
    }

    public void buttonOkOnClick(@SuppressWarnings("unused") final View view) {
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
                if (selectedElements.length() > 0) selectedElements.append("+");
                selectedElements.append(elementsIDs.get(elementsValues.indexOf(item)));
            }

            final String eventSources = String.join(Constants.STRING_PLUS, eventSourcesSelected);

            final Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);

            final ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
            final int colorWidgetBackground = colorWidgetBackgroundPicker.getColor();

            //Сохранение настроек

            List<String> prefsToStore = new ArrayList<>();

            prefsToStore.add(layoutId); //Количество месяцев
            prefsToStore.add(selectedShift); //Стартовый месяц
            prefsToStore.add(String.valueOf(prefStartingMonthPosition)); //Положение (позиция в списке выбора)
            prefsToStore.add(selectedElements.toString()); //Элементы
            prefsToStore.add(eventSources); //Источники событий (через +)
            prefsToStore.add(String.valueOf(spinnerMagnify.getSelectedItemPosition())); //Коэффициент масштабирования (позиция в списке выбора)
            prefsToStore.add(colorWidgetBackground != ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBackground_default) ? ContactsEvents.toARGBString(colorWidgetBackground) : Constants.STRING_EMPTY); //Цвет подложки

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

    public void buttonCancelOnClick(@SuppressWarnings("unused") final View view) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openBatteryOptimisationsSettings(@SuppressWarnings("unused") final View view) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        try {
            startActivity(intent);
        } catch (final android.content.ActivityNotFoundException e) { /**/ }
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

    private void getEventSources() {
        try {

            eventSourcesIds.add("01");
            eventSourcesTitles.add(getString(R.string.month_event_saturdays));
            eventSourcesIcons.add(android.R.drawable.ic_menu_day);
            eventSourcesPackages.add(getPackageName());

            eventSourcesIds.add("02");
            eventSourcesTitles.add(getString(R.string.month_event_sundays));
            eventSourcesIcons.add(android.R.drawable.ic_menu_day);
            eventSourcesPackages.add(getPackageName());

            //todo: справочники

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateEventSources() {
        try {

            TextView listEventSources = findViewById(R.id.listEventSources);
            StringBuilder sb = new StringBuilder();
            for (String source: eventSourcesSelected) {
                int ind = eventSourcesIds.indexOf(source);
                if (ind > -1) {
                    if (sb.length() > 0) sb.append(Constants.STRING_EOL);
                    sb.append(eventSourcesTitles.get(ind));
                }
            }

            if (sb.length() == 0) {
                listEventSources.setText(R.string.widget_config_month_sources_empty);
            } else {
                listEventSources.setText(sb.toString());
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectEventSources() {
        try {

            if (eventSourcesIds.size() > 0) {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                List<String> sourceChoices = new ArrayList<>();

                for (int i = 0; i < eventSourcesIds.size(); i++) {
                    String sourceId = eventSourcesIds.get(i);
                    String sourceTitle = eventSourcesTitles.get(i);
                    sourceChoices.add(sourceTitle);
                }

                ListAdapter adapter = new ContactsEvents.MultiCheckoxesAdapter(this, sourceChoices, eventSourcesIcons, eventSourcesPackages, ta);

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.widget_config_events_sources_label)
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
                        //todo: выбор цвета
                        ToastExpander.showInfoMsg(getApplicationContext(), "long click: " + position);
                        ColorPicker picker = new ColorPicker(thisActivity);
                        //picker.selectRGBColor(eventsData);
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

}
