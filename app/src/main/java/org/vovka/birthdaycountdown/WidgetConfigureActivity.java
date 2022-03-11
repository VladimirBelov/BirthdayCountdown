/*
 * *
 *  * Created by Vladimir Belov on 07.03.2022, 22:54
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 07.03.2022, 22:24
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.PARAM_APP_WIDGET_ID;
import static org.vovka.birthdaycountdown.Constants.REGEX_PLUS;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TYPE_4X1;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TYPE_5X1;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TYPE_LIST;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TYPE_PHOTO_LIST;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
    private List<String> eventInfoIDs;
    private List<String> eventInfoValues;

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
            if (extras != null) widgetId = extras.getInt(PARAM_APP_WIDGET_ID, 0);
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            String widgetType;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName();
            } else {
                widgetType = WIDGET_TYPE_PHOTO_LIST;
            }

            List<String> widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);

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

            //Типы событий
            eventTypesIDs = Arrays.asList(getResources().getStringArray(R.array.pref_EventTypes_values));
            eventTypesValues = Arrays.asList(getResources().getStringArray(R.array.pref_EventTypes_entries));

            MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            List<String> listEventTypes = new ArrayList<>();

            String[] eventsArray = null;
            try {
                if (widgetPref.size() > 3) eventsArray = widgetPref.get(3).split("\\+");
                if (eventsArray != null) {
                    for (String item : eventsArray) {
                        if (eventTypesIDs.contains(item)) listEventTypes.add(eventTypesValues.get(eventTypesIDs.indexOf(item)));
                    }
                }
            } catch (Exception e2) {/**/}

            spinnerEventTypes.setZeroSelectedTitle(getString(R.string.widget_config_event_types_empty));
            spinnerEventTypes.setItems(eventTypesValues);
            spinnerEventTypes.setSelection(listEventTypes);

            //Детали события
            eventInfoIDs = new ArrayList<>();
            eventInfoValues = new ArrayList<>();

            switch (widgetType) {

                case WIDGET_TYPE_LIST:

                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDate_Original_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDate_Original));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDate_Original_WithYear_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDate_Original_WithYear));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDate_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDate));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDate_WithYear_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDate_WithYear));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventTitle_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventTitle));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventCaption_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventCaption));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Age));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_WeddingName_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_WeddingName));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEventFar_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEventFar));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDayOfWeekFar_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDayOfWeekFar));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_SourceIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_SourceIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_LinkIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_LinkIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_NewLine1_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_NewLine1));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_NewLine2_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_NewLine2));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_NewLine3_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_NewLine3));
                    break;

                case WIDGET_TYPE_PHOTO_LIST:

                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_None_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_None));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Photo_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Photo));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Organization_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Organization));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_JobTitle_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_JobTitle));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventCaption_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventCaption));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Age));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_ZodiacIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_ZodiacIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_ZodiacYear));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek));
                    break;

                default:

                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_None_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_None));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Age));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_Photo_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_Photo));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_FavIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_FavIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_SilentedIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_SilentedIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_ZodiacIcon_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_ZodiacIcon));
                    eventInfoIDs.add(getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID)); eventInfoValues.add(getString(R.string.pref_Widgets_EventInfo_ZodiacYear));
            }

            MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            List<String> eventInfoSelections = new ArrayList<>();
            String[] infoArray = null;
            try {
                if (widgetPref.size() > 4) {
                    if (widgetType.equals(WIDGET_TYPE_LIST) && (widgetPref.get(4).equals(STRING_EMPTY) || widgetPref.get(4).equals(getString(R.string.pref_Widgets_EventInfo_None_ID)))) {
                        widgetPref.set(4, getString(R.string.widget_config_defaultPref_List).split(STRING_COMMA, -1)[4]);
                    }

                    infoArray = widgetPref.get(4).split(REGEX_PLUS);
                }
                if (infoArray != null) {
                    for (String item : infoArray) {
                        if (eventInfoIDs.contains(item)) eventInfoSelections.add(eventInfoValues.get(eventInfoIDs.indexOf(item)));
                    }
                }
            } catch (Exception e2) {/**/}

            if (widgetType.equals(WIDGET_TYPE_LIST)) {
                spinnerEventInfo.setSortable(true);
                spinnerEventInfo.fm = getSupportFragmentManager();
                spinnerEventInfo.setZeroSelectedIndex(-1);

                spinnerEventInfo.setItems(eventInfoValues);
                spinnerEventInfo.moveToBeginning(eventInfoSelections); //Двигаем выбранные вперёд
                spinnerEventInfo.setColored(new ArrayList<String>(){{
                    add(getString(R.string.pref_Widgets_EventInfo_EventDate_Original));
                    add(getString(R.string.pref_Widgets_EventInfo_EventDate_Original_WithYear));
                    add(getString(R.string.pref_Widgets_EventInfo_EventDate));
                    add(getString(R.string.pref_Widgets_EventInfo_EventDate_WithYear));
                    add(getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent));
                }}, eventsData.preferences_widgets_color_eventtoday);

            } else {
                spinnerEventInfo.setZeroSelectedTitle(getString(R.string.widget_config_event_info_empty));
                spinnerEventInfo.setZeroSelectedIndex(0);
                spinnerEventInfo.setItems(eventInfoValues);
            }
            spinnerEventInfo.setSelection(eventInfoSelections);


            //Цвета
            int colorWidgetBackground = 0;
            if (widgetPref.size() > 5 && !widgetPref.get(5).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(5));
                } catch (Exception e) { /* */}
            }
            if (colorWidgetBackground == 0) {
                colorWidgetBackground = ContextCompat.getColor(eventsData.context, R.color.pref_Widgets_Color_WidgetBackground_default);
            }
            ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
            colorWidgetBackgroundPicker.setColor(colorWidgetBackground);

            SeekBar colorWidgetBackgroundAlpha = findViewById(R.id.colorWidgetBackgroundAlpha);
            colorWidgetBackgroundAlpha.setProgress(Color.alpha(colorWidgetBackground));
            colorWidgetBackgroundPicker.setAlphaSeekBar(colorWidgetBackgroundAlpha);

            colorWidgetBackgroundAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
                    int colorWidgetBackground = colorWidgetBackgroundPicker.getColor();
                    int newColor = Color.argb(
                            progress,
                            Color.red(colorWidgetBackground),
                            Color.green(colorWidgetBackground),
                            Color.blue(colorWidgetBackground)
                    );
                    colorWidgetBackgroundPicker.setColor(newColor);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            //Скрываем недоступные параметры

            if (eventsData.checkNoBatteryOptimization()) findViewById(R.id.hintBatteryOptimization).setVisibility(View.GONE);

            if (widgetType.equals(WIDGET_TYPE_4X1)) {

                //Скрываем Коэффициент масштабирования размера шрифта
                findViewById(R.id.dividerFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.captionFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.spinnerFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.hintFontMagnify).setVisibility(View.GONE);

            }

            if (!widgetType.equals(WIDGET_TYPE_5X1)) {

                //Скрываем количество событий
                findViewById(R.id.dividerEventsCount).setVisibility(View.GONE);
                findViewById(R.id.captionEventsCount).setVisibility(View.GONE);
                findViewById(R.id.spinnerEventsCount).setVisibility(View.GONE);
                findViewById(R.id.hintEventsCount).setVisibility(View.GONE);

            }

            if (widgetType.equals(WIDGET_TYPE_LIST) || widgetType.equals(WIDGET_TYPE_PHOTO_LIST)) {

                //Скрываем стартовый номер
                findViewById(R.id.dividerEventShift).setVisibility(View.GONE);
                findViewById(R.id.captionEventShift).setVisibility(View.GONE);
                findViewById(R.id.spinnerEventShift).setVisibility(View.GONE);
                findViewById(R.id.hintEventShift).setVisibility(View.GONE);

            }

            if (eventsData.hasPreferences(getString(R.string.widget_config_PrefName) + widgetId) || widgetType.equals(WIDGET_TYPE_LIST) || widgetType.equals(WIDGET_TYPE_PHOTO_LIST)) {

                //Скрываем подсказку для существующих виджетов
                findViewById(R.id.widget_hint).setVisibility(View.GONE);

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.WIDGET_CONFIGURE_ACTIVITY_ON_CREATE_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    public void buttonOkOnClick(View view) {
        try {

            MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            Spinner spinnerIndex = findViewById(R.id.spinnerEventShift);
            Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            Spinner spinnerEventsCount = findViewById(R.id.spinnerEventsCount);
            MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            int selectedItemPosition = spinnerIndex.getSelectedItemPosition();

            StringBuilder eventTypes = new StringBuilder();
            for(String item: spinnerEventTypes.getSelectedStrings()) {
                if (eventTypes.length() > 0) eventTypes.append("+");
                eventTypes.append(eventTypesIDs.get(eventTypesValues.indexOf(item)));
            }

            StringBuilder eventInfo = new StringBuilder();
            for(String item: spinnerEventInfo.getSelectedStrings()) {
                if (eventInfo.length() > 0) eventInfo.append("+");
                eventInfo.append(eventInfoIDs.get(eventInfoValues.indexOf(item)));
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

            ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
            int colorWidgetBackground = colorWidgetBackgroundPicker.getColor();

            //Сохраняем настройки

            eventsData.setWidgetPreference(widgetId,
                    spinnerIndex.getItemAtPosition(selectedItemPosition).toString() //Стартовый номер события
                    .concat(Constants.STRING_COMMA)
                    .concat(String.valueOf(spinnerMagnify.getSelectedItemPosition())) //Коэффициент масштабирования (позиция в списке выбора)
                    .concat(Constants.STRING_COMMA)
                    .concat(String.valueOf(spinnerEventsCount.getSelectedItemPosition())) //Количество событий (позиция в списке выбора)
                    .concat(Constants.STRING_COMMA)
                    .concat(eventTypes.toString()) //Типы событий (через +)
                    .concat(Constants.STRING_COMMA)
                    .concat(eventInfo.toString()) //Детали события (через +)
                    .concat(Constants.STRING_COMMA)
                    .concat(colorWidgetBackground != ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBackground_default) ? ContactsEvents.toARGBString(colorWidgetBackground) : STRING_EMPTY) //Цвет подложки
            );

            Intent intent = new Intent();
            intent.putExtra(PARAM_APP_WIDGET_ID, widgetId);
            setResult(RESULT_OK, intent);

            //Посылаем сообщение на обновление виджета
            eventsData.updateWidgets(widgetId);

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.WIDGET_CONFIGURE_ACTIVITY_BUTTON_OK_ON_CLICK_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    public void buttonCancelOnClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public  void openBatteryOptimisationsSettings(View view) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) { /**/ }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(PARAM_APP_WIDGET_ID, widgetId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        widgetId = savedInstanceState.getInt(PARAM_APP_WIDGET_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_widget_config, menu);

        MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
        spinnerEventInfo.menu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_ok) {

            MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            final ArrayList<String> allSelectedItems = ((RecyclerListFragment) spinnerEventInfo.fragment).adapter.getAllSelectedItems();
            allSelectedItems.remove(getString(R.string.pref_Widgets_EventInfo_Border));
            if (allSelectedItems.isEmpty()) {

                Toast.makeText(getApplicationContext(), getString(R.string.msg_no_selection), Toast.LENGTH_LONG).show();

            } else {

                onBackPressed();
                spinnerEventInfo.setSelectedFromFragmentResults();
                item.setVisible(false);
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
