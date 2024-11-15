/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 15.01.2024, 00:56
 *
 */

package org.vovka.birthdaycountdown;

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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

public class WidgetConfigureActivity extends AppCompatActivity {

    private static final String TAG = "WidgetConfigureActivity";
    private static final String UPPER_ROW = "upperRow";
    private static final String BOTTOM_ROW = "bottomRow";
    private int widgetId = 0;
    private String widgetType = Constants.WIDGET_TYPE_PHOTO_LIST;
    List<String> widgetPref;
    private boolean isListWidget = false;
    private final ContactsEvents eventsData = ContactsEvents.getInstance();
    private List<String> eventTypesIDs;
    private List<String> eventTypesValues;
    private List<String> eventInfoIDs;
    private List<String> eventInfoValues;
    private final ContactsEvents.EventSources eventSources = eventsData.new EventSources();
    private List<String> eventSourcesSelected = new ArrayList<>();
    @ColorInt private int colorCaptionUpper;
    @ColorInt private int colorCaptionBottom;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        TypedArray ta = null;

        try {

            super.onCreate(savedInstanceState);

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
            setContentView(R.layout.widget_config);

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

            //todo: цвет spinner https://stackoverflow.com/questions/9476665/how-to-change-spinner-text-size-and-text-color

            setResult(RESULT_CANCELED);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) widgetId = extras.getInt(Constants.PARAM_APP_WIDGET_ID, 0);
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
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

            //Стартовый номер
            int prefStartingIndex = 1;
            try {
                if (!widgetPref.isEmpty()) prefStartingIndex = Integer.parseInt(widgetPref.get(0));
            } catch (Exception e) {/**/}

            Spinner spinnerIndex = findViewById(R.id.spinnerEventShift);
            spinnerIndex.setSelection(prefStartingIndex - 1, true);

            //Коэффициент масштабирования размера шрифта
            int prefMagnifyIndex = 0;
            try {
                if (widgetPref.size() > 1) prefMagnifyIndex = Integer.parseInt(widgetPref.get(1));
            } catch (Exception e) {/**/}

            Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            spinnerMagnify.setSelection(prefMagnifyIndex, true);

            //Стиль фото
            int prefPhotoStyle = 0;
            try {
                if (widgetPref.size() > 6) prefPhotoStyle = Integer.parseInt(widgetPref.get(6));
            } catch (Exception e) {/**/}

            Spinner spinnerPhotoStyle = findViewById(R.id.spinnerPhotoStyle);
            spinnerPhotoStyle.setSelection(prefPhotoStyle, true);

            //Количество событий в ширину (фото виджет)
            int prefEventsCountIndex = 0;
            try {
                if (widgetPref.size() > 2) prefEventsCountIndex = Integer.parseInt(widgetPref.get(2));
            } catch (Exception e) {/**/}

            Spinner spinnerEventsCount = findViewById(R.id.spinnerScopeEventsCount);
            spinnerEventsCount.setSelection(prefEventsCountIndex, true);

            //Типы событий
            eventTypesIDs = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_List_EventTypes_values)));
            eventTypesValues = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_List_EventTypes_entries)));

            if (widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
                eventTypesIDs.add(getString(R.string.pref_EventTypes_Facts));
                eventTypesValues.add(getString(R.string.pref_Notifications_EventTypes_Facts));
            }

            //Добавление количества событий
            for (int i=0; i < eventTypesValues.size(); i++) {
                if (eventsData.statEventTypes.containsKey(eventTypesIDs.get(i))) {
                    eventTypesValues.set(i, eventTypesValues.get(i)
                            + Constants.STRING_BRACKETS_OPEN
                            + eventsData.statEventTypes.get(eventTypesIDs.get(i))
                            + Constants.STRING_BRACKETS_CLOSE);
                }
            }

            MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            List<String> listEventTypes = new ArrayList<>();

            String[] eventsArray = null;
            try {
                if (widgetPref.size() > 3) eventsArray = widgetPref.get(3).split(Constants.REGEX_PLUS);
                if (eventsArray != null) {
                    for (String item : eventsArray) {
                        if (eventTypesIDs.contains(item)) listEventTypes.add(eventTypesValues.get(eventTypesIDs.indexOf(item)));
                    }
                }
            } catch (Exception e) {/**/}

            spinnerEventTypes.setZeroSelectedTitle(getString(R.string.widget_config_event_types_empty));
            spinnerEventTypes.setItems(eventTypesValues);
            spinnerEventTypes.setSelection(listEventTypes);
            if (widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
                spinnerEventTypes.onDismissListener = dialog -> updateVisibility();
            }

            isListWidget = widgetType.equals(Constants.WIDGET_TYPE_LIST) || widgetType.equals(Constants.WIDGET_TYPE_PHOTO_LIST);
            CheckBox checkCaptionsUsePrefs = findViewById(R.id.checkCaptionsUsePrefs);
            checkCaptionsUsePrefs.setOnClickListener(v -> updateVisibility());
            checkCaptionsUsePrefs.setChecked(true);
            TextView labelCaptionsUsePrefs = findViewById(R.id.labelCaptionsUsePrefs);
            labelCaptionsUsePrefs.setOnClickListener(v -> {
                checkCaptionsUsePrefs.setChecked(!checkCaptionsUsePrefs.isChecked());
                updateVisibility();
            });

            //Ограничения объёма
            List<String> spinnerScopeEventsItems;
            if (isListWidget) {
                spinnerScopeEventsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_events_items).split(Constants.STRING_COMMA, -1)));
            } else {
                spinnerScopeEventsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_photo_scope_events_items).split(Constants.STRING_COMMA, -1)));
            }

            Spinner spinnerScopeEvents = findViewById(R.id.spinnerScopeEvents);
            ArrayAdapter<String> spinnerScopeEventsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerScopeEventsItems);
            spinnerScopeEvents.setAdapter(spinnerScopeEventsAdapter);
            spinnerScopeEvents.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateVisibility();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Spinner spinnerScopeDays = findViewById(R.id.spinnerScopeDays);
            List<String> spinnerScopeDaysItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_days_items).split(Constants.STRING_COMMA, -1)));
            ArrayAdapter<String> spinnerScopeDaysAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerScopeDaysItems);
            spinnerScopeDays.setAdapter(spinnerScopeDaysAdapter);

            Spinner spinnerLayout = findViewById(R.id.spinnerLayout);
            if (widgetType.equals(Constants.WIDGET_TYPE_5X1) || widgetType.equals(Constants.WIDGET_TYPE_4X1)
                    || widgetType.equals(Constants.WIDGET_TYPE_2X2)) {

                List<String> spinnerLayoutItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_layout_items).split(Constants.STRING_COMMA, -1)));
                ArrayAdapter<String> spinnerLayoutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerLayoutItems);
                spinnerLayout.setAdapter(spinnerLayoutAdapter);
                spinnerLayout.setSelection(1); //Оставить пустоту по-умолчанию

            }

            Spinner spinnerFacts = findViewById(R.id.spinnerFacts);
            List<String> spinnerFactsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_facts_items).split(Constants.STRING_COMMA, -1)));
            ArrayAdapter<String> spinnerFactsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerFactsItems);
            spinnerFacts.setAdapter(spinnerFactsAdapter);

            String prefScope = Constants.STRING_EMPTY;
            if (widgetPref.size() > 8) prefScope = widgetPref.get(8);
            if (!TextUtils.isEmpty(prefScope)) {
                Matcher matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE_PLUS).matcher(prefScope);
                boolean found = matchScopes.find();
                if (!found) {
                    matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE_RAND).matcher(prefScope).reset();
                    found = matchScopes.find();
                }
                if (!found) {
                    matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE).matcher(prefScope).reset();
                    found = matchScopes.find();
                }
                if (found) {
                    final String scopeEvents = matchScopes.group(1);
                    if (scopeEvents != null) {
                        if (scopeEvents.equals(Constants.STRING_0)) { //Без ограничений
                            spinnerScopeEvents.setSelection(0);
                        } else if (spinnerScopeEventsItems.contains(scopeEvents)) {
                            spinnerScopeEvents.setSelection(spinnerScopeEventsItems.indexOf(scopeEvents), true);
                        }
                    }
                    final String scopeDays = matchScopes.group(2);
                    if (scopeDays != null) {
                        if (scopeDays.equals(Constants.STRING_0)) { //Без ограничений
                            spinnerScopeDays.setSelection(0);
                        } else if (spinnerScopeDaysItems.contains(scopeDays)) {
                            spinnerScopeDays.setSelection(spinnerScopeDaysItems.indexOf(scopeDays), true);
                        }
                    }
                    if (widgetType.equals(Constants.WIDGET_TYPE_5X1) || widgetType.equals(Constants.WIDGET_TYPE_4X1)
                            || widgetType.equals(Constants.WIDGET_TYPE_2X2)) {
                        final String scopeLayout = matchScopes.group(3);
                        if (scopeLayout != null) {
                            if (scopeLayout.equals(Constants.STRING_PLUS)) { //Расширить
                                spinnerLayout.setSelection(0);
                            } else if (scopeLayout.equals(Constants.STRING_MINUS)) { //Оставить пустоту
                                spinnerLayout.setSelection(1);
                            }
                        }
                    } else if (widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
                        try {
                            final String scopeFacts = matchScopes.group(3);
                            if (scopeFacts != null) {
                                if (spinnerFactsItems.contains(scopeFacts)) {
                                    spinnerFacts.setSelection(spinnerFactsItems.indexOf(scopeFacts), true);
                                }
                            }
                        } catch (IndexOutOfBoundsException ignored) { /**/ }
                    }
                }
            }

            if (!isListWidget) {

                //Параметры заголовков
                List<String> prefCaptions = new ArrayList<>();
                if (widgetPref.size() > 11) prefCaptions.addAll(Arrays.asList(widgetPref.get(11).split(Constants.REGEX_PLUS)));

                List<String> listBottomInfo = Arrays.asList(getResources().getStringArray(R.array.pref_Widgets_BottomInfo_values));

                Spinner spinnerCaptionsUpper = findViewById(R.id.spinnerCaptionsUpper);
                int position = listBottomInfo.indexOf(eventsData.preferences_widgets_bottom_info_2nd);
                if (position != -1) spinnerCaptionsUpper.setSelection(position);
                Spinner spinnerCaptionsBottom = findViewById(R.id.spinnerCaptionsBottom);
                position = listBottomInfo.indexOf(eventsData.preferences_widgets_bottom_info);
                if (position != -1) spinnerCaptionsBottom.setSelection(position);

                Spinner spinnerCaptionsUpperAligning = findViewById(R.id.spinnerCaptionsUpperAligning);
                spinnerCaptionsUpperAligning.setSelection(eventsData.getDefaultAligningForEventInfo(eventsData.preferences_widgets_bottom_info_2nd) - 1);
                Spinner spinnerCaptionsBottomAligning = findViewById(R.id.spinnerCaptionsBottomAligning);
                spinnerCaptionsBottomAligning.setSelection(eventsData.getDefaultAligningForEventInfo(eventsData.preferences_widgets_bottom_info) - 1);

                Spinner spinnerCaptionsUpperRows = findViewById(R.id.spinnerCaptionsUpperRows);
                Spinner spinnerCaptionsBottomRows = findViewById(R.id.spinnerCaptionsBottomRows);

                Spinner spinnerCaptionsUpperFontStyle = findViewById(R.id.spinnerCaptionsUpperFontStyle);
                Spinner spinnerCaptionsBottomFontStyle = findViewById(R.id.spinnerCaptionsBottomFontStyle);

                EditText editCaptionsUpperFontSize = findViewById(R.id.editCaptionsUpperFontSize);
                editCaptionsUpperFontSize.setText(String.valueOf(Constants.WIDGET_TEXT_SIZE_SMALL));
                EditText editCaptionsBottomFontSize = findViewById(R.id.editCaptionsBottomFontSize);
                editCaptionsBottomFontSize.setText(String.valueOf(Constants.WIDGET_TEXT_SIZE_SMALL));

                updateCaptionsColors(eventsData.preferences_widgets_color_default, eventsData.preferences_widgets_color_default);

                if (prefCaptions.size() == Constants.PhotoWidget_Bottom_Color + 1) {

                    checkCaptionsUsePrefs.setChecked(false);

                    position = listBottomInfo.indexOf(prefCaptions.get(Constants.PhotoWidget_Upper_Caption));
                    if (position != -1 && spinnerCaptionsUpper.getAdapter().getCount() > position) spinnerCaptionsUpper.setSelection(position);

                    try {
                        position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Aligning));
                        if (spinnerCaptionsUpperAligning.getAdapter().getCount() > position - 1) spinnerCaptionsUpperAligning.setSelection(position - 1);
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Rows));
                        if (spinnerCaptionsUpperRows.getAdapter().getCount() > position - 1) spinnerCaptionsUpperRows.setSelection(position - 1);
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_FontStyle));
                        if (spinnerCaptionsUpperFontStyle.getAdapter().getCount() > position) spinnerCaptionsUpperFontStyle.setSelection(position);
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        int prefSize = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_FontSize));
                        if (prefSize > 0 && prefSize < 100) editCaptionsUpperFontSize.setText(String.valueOf(prefSize));
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        @ColorInt int prefColor = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Color));
                        colorCaptionUpper = prefColor;
                    } catch (NumberFormatException ignored) { /**/ }

                    position = listBottomInfo.indexOf(prefCaptions.get(Constants.PhotoWidget_Bottom_Caption));
                    if (position != -1 && spinnerCaptionsBottom.getAdapter().getCount() > position) spinnerCaptionsBottom.setSelection(position);

                    try {
                        position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Aligning));
                        if (spinnerCaptionsBottomAligning.getAdapter().getCount() > position - 1) spinnerCaptionsBottomAligning.setSelection(position - 1);
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Rows));
                        if (spinnerCaptionsBottomRows.getAdapter().getCount() > position - 1) spinnerCaptionsBottomRows.setSelection(position - 1);
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_FontStyle));
                        if (spinnerCaptionsBottomFontStyle.getAdapter().getCount() > position) spinnerCaptionsBottomFontStyle.setSelection(position);
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        int prefSize = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_FontSize));
                        if (prefSize > 0 && prefSize < 100) editCaptionsBottomFontSize.setText(String.valueOf(prefSize));
                    } catch (NumberFormatException ignored) { /**/ }

                    try {
                        @ColorInt int prefColor = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Color));
                        colorCaptionBottom = prefColor;
                    } catch (NumberFormatException ignored) { /**/ }

                    updateCaptionsColors(0, 0);

                }

                //Выбор цвета
                ColorPicker picker = new ColorPicker(this);
                TextView captionCaptionsUpperColor = findViewById(R.id.captionCaptionsUpperColor);
                captionCaptionsUpperColor.setOnClickListener(v -> picker.selectColor(colorCaptionUpper, eventsData.preferences_widgets_color_default, "updateSelectedColor", UPPER_ROW));
                TextView captionCaptionsBottomColor = findViewById(R.id.captionCaptionsBottomColor);
                captionCaptionsBottomColor.setOnClickListener(v -> picker.selectColor(colorCaptionBottom, eventsData.preferences_widgets_color_default, "updateSelectedColor", BOTTOM_ROW));

            }

            //Заголовок виджета
            String prefWidgetCaption = Constants.STRING_EMPTY;
            if (widgetPref.size() > 9) prefWidgetCaption = widgetPref.get(9).replaceAll(Constants.STRING_EOT, Constants.STRING_COMMA);
            EditText editCustomWidgetCaption = findViewById(R.id.editCustomWidgetCaption);
            editCustomWidgetCaption.setText(prefWidgetCaption);

            //Сообщение при отсутствии событий
            String prefZeroEventsMessage = Constants.STRING_EMPTY;
            if (widgetPref.size() > 7) prefZeroEventsMessage = widgetPref.get(7).replaceAll(Constants.STRING_EOT, Constants.STRING_COMMA);
            EditText editCustomZeroEvents = findViewById(R.id.editCustomZeroEventsMessage);
            editCustomZeroEvents.setText(prefZeroEventsMessage);

            //Детали события
            eventInfoIDs = new ArrayList<>();
            eventInfoValues = new ArrayList<>();

            switch (widgetType) {

                case Constants.WIDGET_TYPE_LIST:

                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Dividers_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Dividers));
                    if (ContactsEvents.isWidgetSupportConfig()) {
                        eventInfoIDs.add(getString(R.string.pref_EventInfo_ButtonConfig_ID));
                        eventInfoValues.add(getString(R.string.pref_EventInfo_ButtonConfig));
                    }
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_DatesInBrackets_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_DatesInBrackets));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ColorizeEntireRow_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ColorizeEntireRow));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_Original_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_Original));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_Original_WithYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_Original_WithYear));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_WithYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_WithYear));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventTitle_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventTitle));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventCaption_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventCaption));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Age));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_WeddingName_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_WeddingName));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_DaysBeforeEventFar_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_DaysBeforeEventFar));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_DaysBeforeEvent_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_DaysBeforeEvent));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_DaysBeforeEventShort_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_DaysBeforeEventShort));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDayOfWeekFar_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDayOfWeekFar));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDayOfWeek_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDayOfWeek));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDayOfWeekShort_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDayOfWeekShort));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_SourceIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_SourceIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_FavIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_FavIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacSign_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacSign));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacYear));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_LinkIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_LinkIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_NewLine1_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_NewLine1));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_NewLine2_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_NewLine2));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_NewLine3_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_NewLine3));
                    break;

                case Constants.WIDGET_TYPE_PHOTO_LIST:

                    eventInfoIDs.add(getString(R.string.pref_EventInfo_None_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_None));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Dividers_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Dividers));
                    if (ContactsEvents.isWidgetSupportConfig()) {
                        eventInfoIDs.add(getString(R.string.pref_EventInfo_ButtonConfig_ID));
                        eventInfoValues.add(getString(R.string.pref_EventInfo_ButtonConfig));
                    }
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ColorizeEntireRow_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ColorizeEntireRow));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Photo_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Photo));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Organization_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Organization));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_JobTitle_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_JobTitle));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_FavIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_FavIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventCaption_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventCaption));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventLabel_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventLabel));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Age));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_CurrentAge_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_CurrentAge));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacSign_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacSign));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacYear));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_DaysBeforeEvent_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_DaysBeforeEvent));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDayOfWeek_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_EventDayOfWeek));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate));
                    break;

                default:

                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_None_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_None));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_Border_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_Border));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_Age_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_Age));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_Photo_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_Photo));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_EventIcon_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_EventIcon));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_FavIcon_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_FavIcon));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_SilentedIcon_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_SilentedIcon));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacSign_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacSign));
                    this.eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacYear_ID));
                    this.eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacYear));
            }

            final MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            final List<String> eventInfoSelections = new ArrayList<>();
            String[] infoArray = null;
            try {
                if (widgetPref.size() > 4) {
                    if (widgetType.equals(Constants.WIDGET_TYPE_LIST) && (widgetPref.get(4).equals(Constants.STRING_EMPTY) || widgetPref.get(4).equals(getString(R.string.pref_EventInfo_None_ID)))) {
                        widgetPref.set(4, getString(R.string.widget_config_defaultPref_List).split(Constants.STRING_COMMA, -1)[4]);
                    }

                    infoArray = widgetPref.get(4).split(Constants.REGEX_PLUS);
                }
                if (infoArray != null) {
                    for (final String item : infoArray) {
                        if (this.eventInfoIDs.contains(item)) eventInfoSelections.add(this.eventInfoValues.get(this.eventInfoIDs.indexOf(item)));
                    }
                }
            } catch (final Exception e) {/**/}

            if (widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
                spinnerEventInfo.setSortable(true);
                spinnerEventInfo.fm = getSupportFragmentManager();
                spinnerEventInfo.setZeroSelectedIndex(-1);

                spinnerEventInfo.setItems(this.eventInfoValues);
                spinnerEventInfo.moveToBeginning(eventInfoSelections); //Двигаем выбранные вперёд
                spinnerEventInfo.setColored(new ArrayList<String>(){{
                    add(getString(R.string.pref_EventInfo_EventDate_Original));
                    add(getString(R.string.pref_EventInfo_EventDate_Original_WithYear));
                    add(getString(R.string.pref_EventInfo_EventDate));
                    add(getString(R.string.pref_EventInfo_EventDate_WithYear));
                    add(getString(R.string.pref_EventInfo_DaysBeforeEvent));
                }}, this.eventsData.preferences_widgets_color_event_today);

                ArrayList<String> listNonSorted = new ArrayList<String>() {{
                    add(getString(R.string.pref_EventInfo_Border));
                    add(getString(R.string.pref_EventInfo_Dividers));
                    if (ContactsEvents.isWidgetSupportConfig()) {
                        add(getString(R.string.pref_EventInfo_ButtonConfig));
                    }
                    add(getString(R.string.pref_EventInfo_DatesInBrackets));
                    add(getString(R.string.pref_EventInfo_ColorizeEntireRow));
                }};
                spinnerEventInfo.setNonSorted(listNonSorted);
                spinnerEventInfo.moveToBeginning(listNonSorted); //Двигаем зафиксированные элементы вперёд

            } else {
                spinnerEventInfo.setZeroSelectedTitle(getString(R.string.widget_config_event_info_empty));
                spinnerEventInfo.setZeroSelectedIndex(0);
                spinnerEventInfo.setItems(this.eventInfoValues);
            }
            spinnerEventInfo.setSelection(eventInfoSelections);


            //Цвета
            int colorWidgetBackground = 0;
            if (widgetPref.size() > 5 && !widgetPref.get(5).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(5));
                } catch (final Exception e) { /* */}
            }
            if (colorWidgetBackground == 0) {
                colorWidgetBackground = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_WidgetBackground_default);
            }
            final ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
            colorWidgetBackgroundPicker.setColor(colorWidgetBackground);

            //Источники событий
            if (widgetPref.size() > 10) {
                String pref = widgetPref.get(10);
                if (!pref.isEmpty()) eventSourcesSelected = new ArrayList<>(Arrays.asList(pref.split(Constants.REGEX_PLUS)));
            }
            eventSources.getEventSources(widgetType);
            updateEventSources();
            TextView listEventSources = findViewById(R.id.listEventSources);
            listEventSources.setOnClickListener(v -> selectEventSources());

        } catch (final Exception e) {
            Log.e(WidgetConfigureActivity.TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            //Обновляем видимость элементов
            updateVisibility();
            if (ta != null) ta.recycle();
        }
    }

    private void updateEventSources() {
        try {

            TextView listEventSources = findViewById(R.id.listEventSources);
            StringBuilder sb = new StringBuilder();
            for (String source: eventSourcesSelected) {
                int ind = eventSources.getHashes().indexOf(source);
                if (ind > -1) {
                    if (sb.length() > 0) sb.append(Constants.STRING_EOL);

                    String sourceId = ContactsEvents.checkForNull(eventSources.getIds().get(ind));
                    if (sourceId.startsWith(Constants.eventSourceCalendarPrefix)) {
                        sb.append("📆 ");
                    } else if (sourceId.startsWith(Constants.eventSourceFilePrefix) || sourceId.startsWith(Constants.eventSourceMultiFilePrefix)) {
                        sb.append("📁 ");
                    } else if (sourceId.startsWith(Constants.eventSourceContactPrefix)) {
                        sb.append("👨‍💼 ");
                    }
                    sb.append(eventSources.getTitles().get(ind));
                }
            }

            if (sb.length() == 0) {
                listEventSources.setText(R.string.widget_config_event_sources_empty);
            } else {
                listEventSources.setText(sb.toString());
            }

        } catch (final Exception e) {
            Log.e(WidgetConfigureActivity.TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateVisibility() {
        try {

            boolean isNotPhotoWidget = !widgetType.equals(Constants.WIDGET_TYPE_5X1) && !widgetType.equals(Constants.WIDGET_TYPE_4X1)
                    && !widgetType.equals(Constants.WIDGET_TYPE_2X2);

            if (isNotPhotoWidget) {
                findViewById(R.id.blockLayout).setVisibility(View.GONE);
            }

            if (this.isListWidget) {

                //Скрываем стартовый номер
                findViewById(R.id.dividerEventShift).setVisibility(View.GONE);
                findViewById(R.id.captionEventShift).setVisibility(View.GONE);
                findViewById(R.id.spinnerEventShift).setVisibility(View.GONE);
                findViewById(R.id.hintEventShift).setVisibility(View.GONE);

                findViewById(R.id.dividerCaptions).setVisibility(View.GONE);
                findViewById(R.id.blockCaptionsUsePrefs).setVisibility(View.GONE);

            } else {

                //Скрываем ограничение объёма
                /*findViewById(R.id.dividerScope).setVisibility(View.GONE);
                findViewById(R.id.captionScope).setVisibility(View.GONE);
                findViewById(R.id.blockScopeEvents).setVisibility(View.GONE);
                findViewById(R.id.blockScopeDays).setVisibility(View.GONE);*/

                final TextView tv = findViewById(R.id.hintPhotoStyle);
                if (tv != null) tv.setText(R.string.widget_config_photostyle_with_align_description);

                findViewById(R.id.dividerCaptions).setVisibility(View.VISIBLE);
                findViewById(R.id.blockCaptionsUsePrefs).setVisibility(View.VISIBLE);

            }

            if (widgetType.equals(Constants.WIDGET_TYPE_LIST)) {

                //Скрываем стиль фото
                findViewById(R.id.dividerPhotoStyle).setVisibility(View.GONE);
                findViewById(R.id.captionPhotoStyle).setVisibility(View.GONE);
                findViewById(R.id.spinnerPhotoStyle).setVisibility(View.GONE);
                findViewById(R.id.hintPhotoStyle).setVisibility(View.GONE);

            }

            if (!this.eventsData.preferences_extrafun) {

                //Скрываем стартовый номер события
                findViewById(R.id.dividerEventShift).setVisibility(View.GONE);
                findViewById(R.id.captionEventShift).setVisibility(View.GONE);
                findViewById(R.id.spinnerEventShift).setVisibility(View.GONE);
                findViewById(R.id.hintEventShift).setVisibility(View.GONE);

                //Скрываем заголовок виджета
                findViewById(R.id.dividerCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.captionCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.editCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.hintCustomWidgetCaption).setVisibility(View.GONE);

                //Скрываем своё сообщение об отсутствии событий
                findViewById(R.id.dividerCustomZeroEventsMessage).setVisibility(View.GONE);
                findViewById(R.id.captionCustomZeroEventsMessage).setVisibility(View.GONE);
                findViewById(R.id.editCustomZeroEventsMessage).setVisibility(View.GONE);
                findViewById(R.id.hintCustomZeroEventsMessage).setVisibility(View.GONE);

                //Скрываем источники событий
                findViewById(R.id.dividerEventSources).setVisibility(View.GONE);
                findViewById(R.id.captionEventSources).setVisibility(View.GONE);
                findViewById(R.id.listEventSources).setVisibility(View.GONE);
                findViewById(R.id.hintEventSources).setVisibility(View.GONE);

            }

            if (widgetType.equals(Constants.WIDGET_TYPE_5X1) || widgetType.equals(Constants.WIDGET_TYPE_4X1)
                    || widgetType.equals(Constants.WIDGET_TYPE_2X2)) {

                //Скрываем заголовок виджета
                findViewById(R.id.dividerCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.captionCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.editCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.hintCustomWidgetCaption).setVisibility(View.GONE);

            }

            if (this.eventsData.hasPreferences(getString(R.string.widget_config_PrefName) + this.widgetId)
                    || widgetType.equals(Constants.WIDGET_TYPE_LIST)
                    || widgetType.equals(Constants.WIDGET_TYPE_PHOTO_LIST)) {

                //Скрываем фото подсказку для существующих виджетов
                findViewById(R.id.widget_hint).setVisibility(View.GONE);

            }

            //Параметры заголовков
            CheckBox checkCaptionsUsePrefs = findViewById(R.id.checkCaptionsUsePrefs);
            int visibilityCaptionsPrefs = checkCaptionsUsePrefs.isChecked() ? View.GONE : View.VISIBLE;
            //https://habr.com/ru/articles/243363/
            TransitionManager.beginDelayedTransition(findViewById(R.id.layout_main));
            findViewById(R.id.blockCaptionsUpper).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperAligning).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperRows).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperFontStyle).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperSize).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottom).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomAligning).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomRows).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomFontStyle).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomSize).setVisibility(visibilityCaptionsPrefs);

            //Ограничение объёма
            final Spinner spinnerScopeEvents = findViewById(R.id.spinnerScopeEvents);
            findViewById(R.id.blockScopeEventsCount).setVisibility(
                    isNotPhotoWidget || spinnerScopeEvents.getSelectedItemPosition() != 0 ? View.GONE : View.VISIBLE
            );

            //Факты
            final MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            findViewById(R.id.blockFacts).setVisibility(
                    widgetType.equals(Constants.WIDGET_TYPE_LIST)
                            && spinnerEventTypes.getSelectedStrings().contains(getString(R.string.pref_Notifications_EventTypes_Facts)) ? View.VISIBLE : View.GONE
            );

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void buttonOkOnClick(@SuppressWarnings("unused") final View view) {
        try {

            final MultiSelectionSpinner spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            final Spinner spinnerIndex = findViewById(R.id.spinnerEventShift);
            final Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            final Spinner spinnerEventsCount = findViewById(R.id.spinnerScopeEventsCount);
            final MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            final Spinner spinnerPhotoStyle = findViewById(R.id.spinnerPhotoStyle);
            final EditText editCustomWidgetCaption = findViewById(R.id.editCustomWidgetCaption);
            final EditText editCustomZeroEvents = findViewById(R.id.editCustomZeroEventsMessage);
            final int selectedItemPosition = spinnerIndex.getSelectedItemPosition();

            final StringBuilder eventTypes = new StringBuilder();
            for(final String item: spinnerEventTypes.getSelectedStrings()) {
                if (eventTypes.length() > 0) eventTypes.append("+");
                eventTypes.append(this.eventTypesIDs.get(this.eventTypesValues.indexOf(item)));
            }

            StringBuilder eventInfo = new StringBuilder();
            for(final String item: spinnerEventInfo.getSelectedStrings()) {
                if (eventInfo.length() > 0) eventInfo.append(Constants.STRING_PLUS);
                eventInfo.append(this.eventInfoIDs.get(this.eventInfoValues.indexOf(item)));
            }

            final StringBuilder scopeInfo = new StringBuilder();
            //Объём событий
            final Spinner spinnerScopeEvents = findViewById(R.id.spinnerScopeEvents);
            final Spinner spinnerScopeDays = findViewById(R.id.spinnerScopeDays);

            scopeInfo.append(spinnerScopeEvents.getSelectedItemPosition() == 0 ? "0" : spinnerScopeEvents.getSelectedItem()).append("e");
            scopeInfo.append(spinnerScopeDays.getSelectedItemPosition() == 0 ? "0" : spinnerScopeDays.getSelectedItem()).append("d");

            Spinner spinnerLayout = findViewById(R.id.spinnerLayout);
            if (widgetType.equals(Constants.WIDGET_TYPE_5X1) || widgetType.equals(Constants.WIDGET_TYPE_4X1)
                    || widgetType.equals(Constants.WIDGET_TYPE_2X2)) {

                List<String> spinnerLayoutItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_layout_items).split(Constants.STRING_COMMA, -1)));
                if (spinnerLayout.getSelectedItem().equals(spinnerLayoutItems.get(0))) {
                    scopeInfo.append(Constants.STRING_PLUS);
                } else if (spinnerLayout.getSelectedItem().equals(spinnerLayoutItems.get(1))) {
                    scopeInfo.append(Constants.STRING_MINUS);
                }

            } else if (widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
                final Spinner spinnerFacts = findViewById(R.id.spinnerFacts);
                scopeInfo.append(spinnerFacts.getSelectedItem()).append("r");
            }

            //Параметры заголовков
            List<String> selectedCaptionsDetails = new ArrayList<>();
            if (!this.isListWidget) {
                final CheckBox checkCaptionsUsePrefs = findViewById(R.id.checkCaptionsUsePrefs);
                if (!checkCaptionsUsePrefs.isChecked()) {
                    List<String> listBottomInfo = Arrays.asList(getResources().getStringArray(R.array.pref_Widgets_BottomInfo_values));

                    Spinner spinnerCaptionsUpper = findViewById(R.id.spinnerCaptionsUpper);
                    selectedCaptionsDetails.add(listBottomInfo.get(spinnerCaptionsUpper.getSelectedItemPosition()));

                    Spinner spinnerCaptionsUpperAligning = findViewById(R.id.spinnerCaptionsUpperAligning);
                    selectedCaptionsDetails.add(String.valueOf(spinnerCaptionsUpperAligning.getSelectedItemPosition() + 1));

                    Spinner spinnerCaptionsUpperRows = findViewById(R.id.spinnerCaptionsUpperRows);
                    selectedCaptionsDetails.add(String.valueOf(spinnerCaptionsUpperRows.getSelectedItemPosition() + 1));

                    Spinner spinnerCaptionsUpperFontStyle = findViewById(R.id.spinnerCaptionsUpperFontStyle);
                    selectedCaptionsDetails.add(String.valueOf(spinnerCaptionsUpperFontStyle.getSelectedItemPosition()));

                    EditText editCaptionsUpperFontSize = findViewById(R.id.editCaptionsUpperFontSize);
                    String prefSize = String.valueOf(Constants.WIDGET_TEXT_SIZE_TINY);
                    try {
                        int selectedSize = Integer.parseInt(editCaptionsUpperFontSize.getText().toString());
                        if (selectedSize > 0 && selectedSize < 100) prefSize = String.valueOf(selectedSize);
                    } catch (NumberFormatException ignored) { /**/ }
                    selectedCaptionsDetails.add(prefSize);

                    selectedCaptionsDetails.add(String.valueOf(colorCaptionUpper));

                    Spinner spinnerCaptionsBottom = findViewById(R.id.spinnerCaptionsBottom);
                    selectedCaptionsDetails.add(listBottomInfo.get(spinnerCaptionsBottom.getSelectedItemPosition()));

                    Spinner spinnerCaptionsBottomAligning = findViewById(R.id.spinnerCaptionsBottomAligning);
                    selectedCaptionsDetails.add(String.valueOf(spinnerCaptionsBottomAligning.getSelectedItemPosition() + 1));

                    Spinner spinnerCaptionsBottomRows = findViewById(R.id.spinnerCaptionsBottomRows);
                    selectedCaptionsDetails.add(String.valueOf(spinnerCaptionsBottomRows.getSelectedItemPosition() + 1));

                    Spinner spinnerCaptionsBottomFontStyle = findViewById(R.id.spinnerCaptionsBottomFontStyle);
                    selectedCaptionsDetails.add(String.valueOf(spinnerCaptionsBottomFontStyle.getSelectedItemPosition()));

                    EditText editCaptionsBottomFontSize = findViewById(R.id.editCaptionsBottomFontSize);
                    prefSize = String.valueOf(Constants.WIDGET_TEXT_SIZE_TINY);
                    try {
                        int selectedSize = Integer.parseInt(editCaptionsBottomFontSize.getText().toString());
                        if (selectedSize > 0 && selectedSize < 100) prefSize = String.valueOf(selectedSize);
                    } catch (NumberFormatException ignored) { /**/ }
                    selectedCaptionsDetails.add(prefSize);

                    selectedCaptionsDetails.add(String.valueOf(colorCaptionBottom));
                    //scopeInfo.append(String.join(Constants.STRING_PLUS, selectedCaptionsDetails));
                }
            }

            final String eventSources = String.join(Constants.STRING_PLUS, eventSourcesSelected);

            //Проверки
            if (this.widgetId == 0) {
                ToastExpander.showInfoMsg(this, "widgetId is unknown!");
                return;
            }

            if (selectedItemPosition ==  android.widget.AdapterView.INVALID_POSITION) {
                ToastExpander.showInfoMsg(this, "selectedItemPosition is undefined!");
                return;
            }

            final ColorPicker colorWidgetBackgroundPicker = findViewById(R.id.colorWidgetBackgroundColor);
            final int colorWidgetBackground = colorWidgetBackgroundPicker.getColor();

            //Сохранение настроек
            List<String> prefsToStore = new ArrayList<>();

            prefsToStore.add(spinnerIndex.getItemAtPosition(selectedItemPosition).toString()); //Стартовый номер события
            prefsToStore.add(String.valueOf(spinnerMagnify.getSelectedItemPosition())); //Коэффициент масштабирования (позиция в списке выбора)
            prefsToStore.add(String.valueOf(spinnerEventsCount.getSelectedItemPosition())); //Количество событий (позиция в списке выбора)
            prefsToStore.add(eventTypes.toString()); //Типы событий (через +)
            prefsToStore.add(eventInfo.toString()); //Детали события (через +)
            prefsToStore.add(colorWidgetBackground != ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBackground_default) ? ContactsEvents.toARGBString(colorWidgetBackground) : Constants.STRING_EMPTY); //Цвет подложки
            prefsToStore.add(String.valueOf(spinnerPhotoStyle.getSelectedItemPosition())); //Стиль фото
            prefsToStore.add(editCustomZeroEvents.getText().toString().replaceAll(Constants.STRING_COMMA, Constants.STRING_EOT)); //Сообщение, когда нет событий
            prefsToStore.add(scopeInfo.toString()); //Объём событий
            prefsToStore.add(editCustomWidgetCaption.getText().toString().replaceAll(Constants.STRING_COMMA, Constants.STRING_EOT)); //Заголовок виджета
            prefsToStore.add(eventSources); //Источники событий (через +)
            prefsToStore.add(String.join(Constants.STRING_PLUS, selectedCaptionsDetails)); //Параметры заголовков (через +)

            this.eventsData.setWidgetPreference(this.widgetId, String.join(Constants.STRING_COMMA, prefsToStore));

            final Intent intent = new Intent();
            intent.putExtra(Constants.PARAM_APP_WIDGET_ID, this.widgetId);
            setResult(Activity.RESULT_OK, intent);

            //Посылаем сообщение на обновление виджета
            this.eventsData.updateWidgets(this.widgetId, null);

            finish();
        } catch (final Exception e) {
            Log.e(WidgetConfigureActivity.TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void buttonCancelOnClick(@SuppressWarnings("unused") final View view) {
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

        final MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
        if (spinnerEventInfo != null) spinnerEventInfo.menu = menu;

        MenuItem itemHelp = menu.findItem(R.id.menu_help_widgets);
        if (itemHelp != null) {
            itemHelp.setVisible(eventsData.isContextHelpAvailable());
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {

        final int itemId = item.getItemId();

        if (itemId == R.id.menu_ok) {

            final MultiSelectionSpinner spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            List<String> allSelectedItems = ((RecyclerListFragment) spinnerEventInfo.fragment).adapter.getAllSelectedItems();
            allSelectedItems.remove(getString(R.string.pref_EventInfo_Border));
            if (allSelectedItems.isEmpty()) {

                ToastExpander.showInfoMsg(getApplicationContext(), getString(R.string.msg_no_selection));

            } else {

                onBackPressed();
                spinnerEventInfo.setSelectedFromFragmentResults();
                item.setVisible(false);

                final MenuItem item2 = spinnerEventInfo.menu.getItem(1);
                if (item2 != null) item2.setVisible(true);
            }

            return true;

        } else if (itemId == R.id.menu_help_widgets) {

                Intent intent = new Intent(this, FAQActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                intent.putExtra(Constants.EXTRA_ANCHOR, getString(R.string.faq_anchor_widgets));
                try {
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) { /**/ }

        }

        return super.onOptionsItemSelected(item);
    }

    private void selectEventSources() {
        try {

            eventsData.selectEventSources(eventSources, eventSourcesSelected, this, null);

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void getSelectedSources(List<String> newSelectedSources) {
        try {

            if (newSelectedSources != null) {
                eventSourcesSelected.clear();
                eventSourcesSelected.addAll(newSelectedSources);
                updateEventSources();
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void updateCaptionsColors(@ColorInt int colorUpper, @ColorInt int colorBottom) {
        try {

            if (colorUpper != 0) colorCaptionUpper = colorUpper;
            if (colorBottom != 0) colorCaptionBottom = colorBottom;

            TextView captionCaptionsUpperColor = findViewById(R.id.captionCaptionsUpperColor);
            captionCaptionsUpperColor.setText(Html.fromHtml("<bold><font color=#"
                            + Integer.toHexString(colorCaptionUpper & 0x00ffffff) + ">●</font></bold>"));

            TextView captionCaptionsBottomColor = findViewById(R.id.captionCaptionsBottomColor);
            captionCaptionsBottomColor.setText(Html.fromHtml("<bold><font color=#"
                    + Integer.toHexString(colorCaptionBottom & 0x00ffffff) + ">●</font></bold>"));

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void updateSelectedColor(@NonNull String colorId, int colorValue) {
        try {

            if (!colorId.isEmpty()) {
                ToastExpander.showDebugMsg(getApplicationContext(), "Выбран цвет:" + colorValue + " для " + colorId);

                if (colorId.equals(UPPER_ROW)) {
                    colorCaptionUpper = colorValue;
                    TextView captionCaptionsUpperColor = findViewById(R.id.captionCaptionsUpperColor);
                    captionCaptionsUpperColor.setText(Html.fromHtml("<bold><font color=#"
                            + Integer.toHexString(colorCaptionUpper & 0x00ffffff) + ">●</font></bold>"));
                } else if (colorId.equals(BOTTOM_ROW)) {
                    colorCaptionBottom = colorValue;
                    TextView captionCaptionsBottomColor = findViewById(R.id.captionCaptionsBottomColor);
                    captionCaptionsBottomColor.setText(Html.fromHtml("<bold><font color=#"
                            + Integer.toHexString(colorCaptionBottom & 0x00ffffff) + ">●</font></bold>"));
                }
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}
