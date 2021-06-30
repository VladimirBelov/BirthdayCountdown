/*
 * *
 *  * Created by Vladimir Belov on 30.06.2021, 13:04
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 30.06.2021, 12:43
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WidgetConfigureActivity extends AppCompatActivity {

    private int widgetId = 0;
    private ContactsEvents eventsData;
    private List<String> eventTypesIDs;
    private List<String> eventTypesValues;
    private final String APP_WIDGET_ID = "appWidgetId";

    public void onCreate(Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = getApplicationContext();
            eventsData.getPreferences();
            eventsData.setLocale(true);

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

            this.setTheme(eventsData.preferences_theme.themeMain);

            setContentView(R.layout.widget_config);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);
            toolbar.setTitle(R.string.window_widget_settings);

            //Цвет заголовка окна
            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            //todo: цвет spinner https://stackoverflow.com/questions/9476665/how-to-change-spinner-text-size-and-text-color

            setResult(RESULT_CANCELED);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) widgetId = extras.getInt(APP_WIDGET_ID, 0);

            List<String> widgetPref = eventsData.getWidgetPreference(widgetId);

            //Заполняем стартовый номер
            int prefStartingIndex = 1;
            try {
                if (widgetPref.size() > 0) prefStartingIndex = Integer.parseInt(widgetPref.get(0));
            } catch (Exception e1) {/**/}

            Spinner spinnerIndex = findViewById(R.id.spinnerEventShift);
            spinnerIndex.setSelection(prefStartingIndex - 1);

            //Заполняем коэффициент масштабирования размера шрифта
            int prefMagnifyIndex = 0;
            try {
                if (widgetPref.size() > 1) prefMagnifyIndex = Integer.parseInt(widgetPref.get(1));
            } catch (Exception e2) {/**/}

            Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            spinnerMagnify.setSelection(prefMagnifyIndex);

            //Заполняем количество событий
            int prefEventsCountIndex = 0;
            try {
                if (widgetPref.size() > 2) prefEventsCountIndex = Integer.parseInt(widgetPref.get(2));
            } catch (Exception e2) {/**/}

            Spinner spinnerEventsCount = findViewById(R.id.spinnerEventsCount);
            spinnerEventsCount.setSelection(prefEventsCountIndex);

            //Заполняем тип событий
            eventTypesIDs = Arrays.asList(getResources().getStringArray(R.array.pref_EventTypes_values));
            eventTypesValues = Arrays.asList(getResources().getStringArray(R.array.pref_EventTypes_entries));

            MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            List<String> list = new ArrayList<>();

            String[] eventsArray = null;
            try {
                if (widgetPref.size() > 3) eventsArray = widgetPref.get(3).split("\\+");
                if (eventsArray != null) {
                    for (String item : eventsArray) {
                        if (eventTypesIDs.contains(item)) list.add(eventTypesValues.get(eventTypesIDs.indexOf(item)));
                    }
                }
            } catch (Exception e2) {/**/}

            spinnerEventTypes.setZeroSelectedTitle(getString(R.string.widget_config_event_types_empty));
            spinnerEventTypes.setItems(eventTypesValues);
            spinnerEventTypes.setSelection(list);

            //Скрываем недоступные параметры
            String widgetType = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId).provider.getShortClassName();

            if (widgetType.equals(".Widget4x1")) {

                //Скрываем Коэффициент масштабирования размера шрифта
                findViewById(R.id.dividerFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.captionFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.spinnerFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.hintFontMagnify).setVisibility(View.GONE);

            }

            if (!widgetType.equals(".Widget5x1")) {

                //Скрываем количество событий
                findViewById(R.id.dividerEventsCount).setVisibility(View.GONE);
                findViewById(R.id.captionEventsCount).setVisibility(View.GONE);
                findViewById(R.id.spinnerEventsCount).setVisibility(View.GONE);
                findViewById(R.id.hintEventsCount).setVisibility(View.GONE);

            }

            if (widgetType.equals(".WidgetList")) {

                //Скрываем стартовый номер
                findViewById(R.id.dividerEventShift).setVisibility(View.GONE);
                findViewById(R.id.captionEventShift).setVisibility(View.GONE);
                findViewById(R.id.spinnerEventShift).setVisibility(View.GONE);
                findViewById(R.id.hintEventShift).setVisibility(View.GONE);

            }

            if (eventsData.hasPreferences(getString(R.string.widget_config_PrefName) + widgetId) || widgetType.equals(".WidgetList")) {

                //Скрываем подсказку для существующих виджетов
                findViewById(R.id.widget_hint).setVisibility(View.GONE);

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.WIDGET_CONFIGURE_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void buttonOkOnClick(View view) {
        try {

            MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            Spinner spinnerIndex = findViewById(R.id.spinnerEventShift);
            Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            Spinner spinnerEventsCount = findViewById(R.id.spinnerEventsCount);
            int selectedItemPosition = spinnerIndex.getSelectedItemPosition();

            StringBuilder eventTypes = new StringBuilder();
            for(String item: spinnerEventTypes.getSelectedStrings()) {
                if (eventTypes.length() > 0) eventTypes.append("+");
                eventTypes.append(eventTypesIDs.get(eventTypesValues.indexOf(item)));
            }

            //Проверки

            if (widgetId == 0) {
                Toast.makeText(this, "widgetId is unknown!", Toast.LENGTH_LONG).show();
                return;
            }

            if (selectedItemPosition == -1) {
                Toast.makeText(this, "selectedItemPosition is undefined!", Toast.LENGTH_LONG).show();
                return;
            }

            /*if (spinnerEventTypes.getSelectedStrings().isEmpty()) {
                Toast.makeText(this, "Choose at least 1 event type!", Toast.LENGTH_LONG).show();
                return;
            }*/

            //Сохраняем настройки

            eventsData.setWidgetPreference(widgetId,
                    spinnerIndex.getItemAtPosition(selectedItemPosition).toString() //тут именно значение в списке
                    .concat(Constants.STRING_COMMA)
                    .concat(String.valueOf(spinnerMagnify.getSelectedItemPosition())) //тут позиция в списке
                    .concat(Constants.STRING_COMMA)
                    .concat(String.valueOf(spinnerEventsCount.getSelectedItemPosition())) //тут позиция в списке
                    .concat(Constants.STRING_COMMA)
                    .concat(eventTypes.toString())
            );

            Intent intent = new Intent();
            intent.putExtra(APP_WIDGET_ID, widgetId);
            setResult(RESULT_OK, intent);

            //Посылаем сообщения на обновление виджетов

            eventsData.updateWidgets();

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.WIDGET_CONFIGURE_ACTIVITY_BUTTON_OK_ON_CLICK_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void buttonCancelOnClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
