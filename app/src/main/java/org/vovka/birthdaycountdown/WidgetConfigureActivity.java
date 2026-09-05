/*
 * *
 *  * Created by Vladimir Belov on 05.09.2026, 15:55
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 05.09.2026, 15:22
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WidgetConfigureActivity - это Activity, которое позволяет пользователю настроить
 * параметры виджета перед его добавлением на главный экран. Оно предоставляет
 * опции для настройки внешнего вида, поведения виджета и данных, которые он
 * отображает.
 *
 * <p>
 * Используется для настройки следующих виджетов:
 * </p>
 * <ul>
 *   <li>{@link WidgetList}</li>
 *   <li>{@link WidgetPhotoList}</li>
 *   <li>{@link Widget2x2}</li>
 *   <li>{@link Widget4x1}</li>
 *   <li>{@link Widget5x1}</li>
 * </ul>
 */
public class WidgetConfigureActivity extends AppCompatActivity {
    private static final String TAG = "WidgetConfigureActivity";
    private static final String UPPER_ROW = "upperRow";
    private static final String BOTTOM_ROW = "bottomRow";
    private int widgetId = 0;
    private String widgetType = Constants.WIDGET_TYPE_PHOTO_LIST;
    private List<String> widgetPref;
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
    private boolean isNewPinnedWidget;
    private final int minValueSeekOffset = 49;
    private String localeAtCreate = "";
    CheckBox checkCaptionsUsePrefs;
    ColorPicker pickerColorWidgetBackground;
    ColorPicker pickerColorWidgetBorder;
    EditText editCustomWidgetCaption;
    EditText editCustomZeroEvents;
    MultiSelectionSpinner spinnerEventInfo;
    MultiSelectionSpinner spinnerEventTypes;
    SeekBar seekFontMagnifyPhoto;
    SeekBar seekFontMagnifyText;
    Spinner spinnerEventShift;
    Spinner spinnerEventsCount;
    Spinner spinnerPhotoStyle;
    Spinner spinnerScopeDays;
    Spinner spinnerScopeEvents;
    TextView listEventSources;
    private Menu menuOptions;
    private boolean isSpinnerInEditMode = false;
    @Nullable
    private AlertDialog loadTemplateDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypedArray ta = null;
        try {
            eventsData.initLanguage(this);
            localeAtCreate = eventsData.currentLocale;
            setTheme(eventsData.preferences_theme.themeMain);
            setContentView(R.layout.widget_config);

            View layoutMain = findViewById(R.id.layout_main);
            if (DeviceTools.isEdgeToEdge()) {
                View layoutCoordinator = findViewById(R.id.coordinator);
                ViewCompat.setOnApplyWindowInsetsListener(layoutCoordinator, (v, windowInsets) -> {
                    Insets insetsStatus = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                    Insets insetsGestures = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
                    Insets insetsIme = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                    int bottomPadding = Math.max(insetsGestures.bottom, insetsIme.bottom);
                    layoutCoordinator.setPadding(0, insetsStatus.top, 0, bottomPadding);
                    layoutMain.setPadding(0, insetsStatus.bottom + ImageUtils.Sp2Px(getResources(), 50), 0, 0);
                    return WindowInsetsCompat.CONSUMED;
                });
            } else {
                layoutMain.setPadding(0, ImageUtils.Dip2Px(getResources(), 50), 0, 0);
            }

            RelativeLayout.MarginLayoutParams marginParams = (RelativeLayout.MarginLayoutParams) layoutMain.getLayoutParams();
            marginParams.setMargins(
                    (int) (eventsData.preferences_list_margin * eventsData.displayMetrics_density + 0.5f),
                    ImageUtils.Dip2Px(getResources(), eventsData.preferences_list_top_padding),
                    (int) (eventsData.preferences_list_margin * eventsData.displayMetrics_density + 0.5f),
                    marginParams.bottomMargin);
            layoutMain.setLayoutParams(marginParams);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);
            toolbar.setTitle(R.string.window_widget_settings);

            ta = getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            setResult(RESULT_CANCELED);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
                if (extras.containsKey(Constants.EXTRA_NEW_WIDGET)) isNewPinnedWidget = true;
            }
            if (widgetId == 0) return;

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }
            widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);

            if (widgetId > 0 && eventsData.preferences_debug_on) {
                toolbar.setTitle(getString(R.string.window_widget_settings)
                        .concat(Constants.STRING_PARENTHESIS_OPEN)
                        .concat(Constants.RES_TYPE_ID)
                        .concat(Constants.STRING_COLON_SPACE)
                        .concat(String.valueOf(widgetId))
                        .concat(Constants.STRING_PARENTHESIS_CLOSE)
                );
            }

            Button buttonCancel = findViewById(R.id.button_cancel);
            if (buttonCancel != null) {
                buttonCancel.setOnClickListener(v -> buttonCancelOnClick());
            }
            Button buttonOk = findViewById(R.id.button_ok);
            if (buttonOk != null) {
                buttonOk.setOnClickListener(v -> buttonOkOnClick());
            }

            // ===== 1. ИНИЦИАЛИЗАЦИЯ ДАННЫХ ДЛЯ СПИННЕРОВ =====

            //Типы событий
            eventTypesIDs = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_List_EventTypes_values)));
            eventTypesValues = eventsData.getEventTypesWithEmoji();
            if (Constants.WIDGET_TYPE_5X1.equals(widgetType) || Constants.WIDGET_TYPE_4X1.equals(widgetType)
                    || Constants.WIDGET_TYPE_2X2.equals(widgetType)) {
                eventTypesIDs.remove(getString(R.string.pref_EventTypes_Other));
                eventTypesValues.remove(eventsData.getEventEmojiForType(Constants.EventType_Other)
                        .concat(Constants.STRING_SPACE).concat(getString(R.string.pref_List_EventTypes_Other)));
                eventTypesIDs.remove(getString(R.string.pref_EventTypes_Holiday));
                eventTypesValues.remove(eventsData.getEventEmojiForType(Constants.EventType_Holiday)
                        .concat(Constants.STRING_SPACE).concat(getString(R.string.pref_List_EventTypes_Holidays)));
            }
            if (Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                eventTypesIDs.add(getString(R.string.pref_EventTypes_Facts));
                eventTypesValues.add(eventsData.getEventEmojiForType(Constants.EventType_Fact)
                        .concat(Constants.STRING_SPACE).concat(getString(R.string.pref_List_EventTypes_Facts)));
            }
            if (eventsData.isFeatureEnabled(Constants.FEATURE_ADV_INFO)) {
                for (int i = 0; i < eventTypesValues.size(); i++) {
                    if (eventsData.statEventTypes.containsKey(eventTypesIDs.get(i))) {
                        eventTypesValues.set(i, eventTypesValues.get(i)
                                + Constants.STRING_BRACKETS_OPEN
                                + eventsData.statEventTypes.get(eventTypesIDs.get(i))
                                + Constants.STRING_BRACKETS_CLOSE);
                    }
                }
            }

            //Детали события (заполняет eventInfoIDs и eventInfoValues)
            initEventDetailsOptions();

            isListWidget = Constants.WIDGET_TYPE_LIST.equals(widgetType) || Constants.WIDGET_TYPE_PHOTO_LIST.equals(widgetType);

            // ===== 2. ИНИЦИАЛИЗАЦИЯ ЭЛЕМЕНТОВ UI (адаптеры, слушатели) =====

            //Стартовый номер (спиннер уже в layout, адаптер из XML)
            //Масштабирование
            int maxValueSeek = 200;
            seekFontMagnifyText = findViewById(R.id.seekFontMagnifyText);
            seekFontMagnifyText.setMax(maxValueSeek - minValueSeekOffset);
            seekFontMagnifyText.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    TextView valueFontMagnifyText = findViewById(R.id.valueFontMagnifyText);
                    valueFontMagnifyText.setText(
                            seekFontMagnifyText.getProgress() == 0 ? getString(R.string.widget_config_magnify_auto) :
                                    getString(R.string.pref_List_FontMagnify_progress, String.valueOf(minValueSeekOffset + seekFontMagnifyText.getProgress()))
                    );
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekFontMagnifyPhoto = findViewById(R.id.seekFontMagnifyPhoto);
            seekFontMagnifyPhoto.setMax(maxValueSeek - minValueSeekOffset);
            seekFontMagnifyPhoto.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    TextView valueFontMagnifyPhoto = findViewById(R.id.valueFontMagnifyPhoto);
                    valueFontMagnifyPhoto.setText(
                            seekFontMagnifyPhoto.getProgress() == 0 ? getString(R.string.widget_config_magnify_auto) :
                                    getString(R.string.pref_List_FontMagnify_progress, String.valueOf(minValueSeekOffset + seekFontMagnifyPhoto.getProgress()))
                    );
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            spinnerEventShift = findViewById(R.id.spinnerEventShift);
            spinnerEventsCount = findViewById(R.id.spinnerScopeEventsCount);
            spinnerPhotoStyle = findViewById(R.id.spinnerPhotoStyle);
            editCustomWidgetCaption = findViewById(R.id.editCustomWidgetCaption);
            editCustomZeroEvents = findViewById(R.id.editCustomZeroEventsMessage);
            pickerColorWidgetBackground = findViewById(R.id.colorWidgetBackground);
            pickerColorWidgetBorder = findViewById(R.id.colorWidgetBorder);

            //Спиннер типов событий
            spinnerEventTypes = findViewById(R.id.spinnerEventTypes);
            spinnerEventTypes.setZeroSelectedTitle(getString(R.string.widget_config_event_types_empty));
            spinnerEventTypes.setItems(eventTypesValues);
            if (Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                spinnerEventTypes.onDismissListener = dialog -> updateVisibility();
            }

            //Чекбокс "использовать настройки приложения" для заголовков
            checkCaptionsUsePrefs = findViewById(R.id.checkCaptionsUsePrefs);
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
            spinnerScopeEvents = findViewById(R.id.spinnerScopeEvents);
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

            spinnerScopeDays = findViewById(R.id.spinnerScopeDays);
            List<String> spinnerScopeDaysItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_days_items).split(Constants.STRING_COMMA, -1)));
            ArrayAdapter<String> spinnerScopeDaysAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerScopeDaysItems);
            spinnerScopeDays.setAdapter(spinnerScopeDaysAdapter);

            Spinner spinnerLayout = findViewById(R.id.spinnerLayout);
            if (Constants.WIDGET_TYPE_5X1.equals(widgetType) || Constants.WIDGET_TYPE_4X1.equals(widgetType)
                    || Constants.WIDGET_TYPE_2X2.equals(widgetType)) {
                List<String> spinnerLayoutItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_layout_items).split(Constants.STRING_COMMA, -1)));
                ArrayAdapter<String> spinnerLayoutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerLayoutItems);
                spinnerLayout.setAdapter(spinnerLayoutAdapter);
            }

            Spinner spinnerFacts = findViewById(R.id.spinnerFacts);
            List<String> spinnerFactsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_facts_items).split(Constants.STRING_COMMA, -1)));
            ArrayAdapter<String> spinnerFactsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerFactsItems);
            spinnerFacts.setAdapter(spinnerFactsAdapter);

            //Заголовки (для фото-виджетов) — пикеры цветов
            if (!isListWidget) {
                ColorPicker picker = new ColorPicker(this);
                TextView captionCaptionsUpperColor = findViewById(R.id.captionCaptionsUpperColor);
                captionCaptionsUpperColor.setOnClickListener(v ->
                        picker.selectColor(colorCaptionUpper, eventsData.preferences_widgets_color_default, true, UPPER_ROW, this::updateSelectedColor));
                TextView captionCaptionsBottomColor = findViewById(R.id.captionCaptionsBottomColor);
                captionCaptionsBottomColor.setOnClickListener(v ->
                        picker.selectColor(colorCaptionBottom, eventsData.preferences_widgets_color_default, true, BOTTOM_ROW, this::updateSelectedColor));
            }

            //Детали события
            spinnerEventInfo = findViewById(R.id.spinnerEventInfo);
            if (Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                spinnerEventInfo.setSortable(true);
                spinnerEventInfo.fm = getSupportFragmentManager();
                spinnerEventInfo.setZeroSelectedIndex(-1);
                spinnerEventInfo.setItems(this.eventInfoValues);
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
                    add(getString(R.string.pref_EventInfo_ButtonConfig));
                    add(getString(R.string.pref_EventInfo_ColorizeEntireRow));
                    add(getString(R.string.pref_EventInfo_ShowNearestEventPhoto));
                    add(getString(R.string.pref_EventInfo_Photo));
                }};
                spinnerEventInfo.setNonSorted(listNonSorted);
                spinnerEventInfo.setBold(new ArrayList<String>() {{
                    add(getString(R.string.pref_EventInfo_BoldStart));
                    add(getString(R.string.pref_EventInfo_BoldEnd));
                }});
                TextView hintEventInfo = findViewById(R.id.hintEventInfo);
                if (hintEventInfo != null) {
                    hintEventInfo.setText(R.string.pref_Widgets_EventInfo_list_summary);
                }
            } else {
                spinnerEventInfo.setZeroSelectedTitle(getString(R.string.widget_config_event_info_empty));
                spinnerEventInfo.setZeroSelectedIndex(0);
                spinnerEventInfo.setItems(this.eventInfoValues);
            }

            //Источники событий
            eventSources.loadEventSources(widgetType);
            listEventSources = findViewById(R.id.listEventSources);
            listEventSources.setOnClickListener(v ->
                    eventsData.selectEventSources(eventSources, eventSourcesSelected, this,
                            selectedSources -> {
                                eventSourcesSelected.clear();
                                eventSourcesSelected.addAll(selectedSources);
                                updateEventSources();
                            })
            );

            findViewById(R.id.adv_hint).setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (ActivityNotFoundException e) { /**/ }
            });

            // ===== 3. ЗАПОЛНЕНИЕ UI ИЗ СОХРАНЁННЫХ НАСТРОЕК ВИДЖЕТА =====
            populateUIFromWidgetPref();

            // ===== 4. ОБНОВЛЕНИЕ ВИДИМОСТИ =====
            updateVisibility();

            // Подписываемся на изменения стека фрагментов
            getSupportFragmentManager().addOnBackStackChangedListener(() -> {
                // Проверяем, находится ли наш фрагмент редактирования сейчас на экране
                boolean isFragmentVisible = getSupportFragmentManager().findFragmentById(R.id.layout_fragment) instanceof RecyclerListFragment;

                if (isSpinnerInEditMode != isFragmentVisible) {
                    isSpinnerInEditMode = isFragmentVisible;
                    updateMenuState(); // Перерисовываем меню
                }
            });

            // Подключаем наш новый listener к спиннеру
            spinnerEventInfo.editModeListener = isInEditMode -> {
                isSpinnerInEditMode = isInEditMode;
                updateMenuState();
            };

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (ta != null) ta.recycle();
        }
    }

    private void initEventDetailsOptions() {
        try {

            eventInfoIDs = new ArrayList<>();
            eventInfoValues = new ArrayList<>();

            switch (widgetType) {

                case Constants.WIDGET_TYPE_LIST:

                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Dividers_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Dividers));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ButtonConfig_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ButtonConfig));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ColorizeEntireRow_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ColorizeEntireRow));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ShowNearestEventPhoto_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ShowNearestEventPhoto));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Photo_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Photo));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_Original_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_Original));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_Original_WithYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_Original_WithYear));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_WithYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_WithYear));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventTitle_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventTitle));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventCaption_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventCaption));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventLabel_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventLabel));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Age_Full));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_AgeShort_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Age_Short));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_CurrentAge_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_CurrentAge));
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
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_LeftBracket_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_LeftBracket));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_LeftBracket2_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_LeftBracket2));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_RightBracket_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_RightBracket));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_RightBracket2_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_RightBracket2));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Colon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Colon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_BoldStart_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_BoldStart));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_BoldEnd_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_BoldEnd));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Tab_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Tab));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Tab2_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Tab2));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Expander_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Expander));
                    break;

                case Constants.WIDGET_TYPE_PHOTO_LIST:

                    eventInfoIDs.add(getString(R.string.pref_EventInfo_None_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_None));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Dividers_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Dividers));
                    if (DeviceTools.isWidgetSupportConfig()) {
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
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_DaysBeforeEvent_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_DaysBeforeEvent));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDayOfWeek_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDayOfWeek));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventYear));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventDate_Original_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventDate_Original));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventYear_Original_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventYear_Original));
                    break;

                default:

                    eventInfoIDs.add(getString(R.string.pref_EventInfo_None_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_None));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Border_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Border));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Age_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Age));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_Photo_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_Photo));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_EventIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_EventIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_FavIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_FavIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_SilencedIcon_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_SilentedIcon));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacSign_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacSign));
                    eventInfoIDs.add(getString(R.string.pref_EventInfo_ZodiacYear_ID)); eventInfoValues.add(getString(R.string.pref_EventInfo_ZodiacYear));
            }
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateEventSources() {
        try {

            StringBuilder sb = new StringBuilder();
            for (String source: eventSourcesSelected) {
                int ind = eventSources.getHashes().indexOf(source);
                if (ind > -1) {
                    if (sb.length() > 0) sb.append(Constants.STRING_EOL);

                    String sourceId = StringUtils.getNotNullString(eventSources.getIds().get(ind));
                    if (sourceId.startsWith(Constants.eventSourceCalendarPrefix)) {
                        sb.append(Constants.eventTitleCalendarPrefix);
                    } else if (sourceId.startsWith(Constants.eventSourceFilePrefix) || sourceId.startsWith(Constants.eventSourceMultiFilePrefix)) {
                        sb.append(Constants.eventTitleFilePrefix);
                    } else if (sourceId.startsWith(Constants.eventSourceContactPrefix)) {
                        sb.append(Constants.eventTitleContactPrefix);
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
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateVisibility() {
        try {

            boolean isAdvSettings = eventsData.isFeatureEnabled(Constants.FEATURE_WIDGETS_MORE_SETTINGS);

            //https://habr.com/ru/articles/243363/
            androidx.transition.AutoTransition transition = new androidx.transition.AutoTransition();
            // Исключаем скрытые спиннеры из анимации изменения границ
            transition.excludeTarget(R.id.spinnerCaptionsUpper, true);
            transition.excludeTarget(R.id.spinnerCaptionsBottom, true);
            androidx.transition.TransitionManager.beginDelayedTransition(findViewById(R.id.layout_main), transition);

            //Параметры заголовков
            int advSettingsVisibility = isAdvSettings ? View.VISIBLE : View.GONE;
            int visibilityCaptionsTitles = checkCaptionsUsePrefs.isChecked() ? View.GONE : View.VISIBLE;
            int visibilityCaptionsPrefs = checkCaptionsUsePrefs.isChecked() | !isAdvSettings ? View.GONE : View.VISIBLE;

            findViewById(R.id.blockCaptionsUpper).setVisibility(visibilityCaptionsTitles);
            findViewById(R.id.blockCaptionsUpperAligning).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperRows).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperFontStyle).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsUpperSize).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottom).setVisibility(visibilityCaptionsTitles);
            findViewById(R.id.blockCaptionsBottomAligning).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomRows).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomFontStyle).setVisibility(visibilityCaptionsPrefs);
            findViewById(R.id.blockCaptionsBottomSize).setVisibility(visibilityCaptionsPrefs);

            //Доп. настройки
            findViewById(R.id.blockEventShift).setVisibility(advSettingsVisibility);

            //Скрываем заголовок виджета
            findViewById(R.id.dividerCustomWidgetCaption).setVisibility(advSettingsVisibility);
            findViewById(R.id.captionCustomWidgetCaption).setVisibility(advSettingsVisibility);
            findViewById(R.id.editCustomWidgetCaption).setVisibility(advSettingsVisibility);
            findViewById(R.id.hintCustomWidgetCaption).setVisibility(advSettingsVisibility);

            //Скрываем своё сообщение об отсутствии событий
            findViewById(R.id.dividerCustomZeroEventsMessage).setVisibility(advSettingsVisibility);
            findViewById(R.id.captionCustomZeroEventsMessage).setVisibility(advSettingsVisibility);
            findViewById(R.id.editCustomZeroEventsMessage).setVisibility(advSettingsVisibility);
            findViewById(R.id.hintCustomZeroEventsMessage).setVisibility(advSettingsVisibility);

            //Скрываем доп. параметры ограничения объёма событий
            findViewById(R.id.blockScopeEventsCount).setVisibility(advSettingsVisibility);
            findViewById(R.id.blockLayout).setVisibility(advSettingsVisibility);

            //Скрываем реакцию на нажатие
            findViewById(R.id.dividerOnClick).setVisibility(advSettingsVisibility);
            findViewById(R.id.captionOnClick).setVisibility(advSettingsVisibility);
            findViewById(R.id.blockOnClickCommon).setVisibility(advSettingsVisibility);
            findViewById(R.id.blockOnClickLastEvent).setVisibility(advSettingsVisibility);

            //Скрываем изменения цвета
            findViewById(R.id.dividerWidgetBackground).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorWidgetBackground).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorWidgetBorder).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorWidgetBorder).setVisibility(advSettingsVisibility);

            //Подсказки
            findViewById(R.id.adv_hint).setVisibility(isAdvSettings ? View.GONE : View.VISIBLE);
            if (this.eventsData.hasPreferences(getString(R.string.widget_config_PrefName) + this.widgetId)
                    || Constants.WIDGET_TYPE_LIST.equals(widgetType)
                    || Constants.WIDGET_TYPE_PHOTO_LIST.equals(widgetType)) {

                //Скрываем фото подсказку для существующих виджетов
                findViewById(R.id.widget_hint).setVisibility(View.GONE);

                if (advSettingsVisibility != View.GONE) {
                    findViewById(R.id.hints).setVisibility(View.GONE);
                }
            }

            if (!Constants.WIDGET_TYPE_5X1.equals(widgetType)) {
                findViewById(R.id.blockLayout).setVisibility(View.GONE);
            }

            if (this.isListWidget) {

                findViewById(R.id.blockEventShift).setVisibility(View.GONE);
                findViewById(R.id.dividerCaptions).setVisibility(View.GONE);
                findViewById(R.id.blockCaptionsUsePrefs).setVisibility(View.GONE);

            } else {

                final TextView tv = findViewById(R.id.hintPhotoStyle);
                if (tv != null) tv.setText(R.string.widget_config_photostyle_with_align_description);

                findViewById(R.id.dividerCaptions).setVisibility(View.VISIBLE);
                findViewById(R.id.blockCaptionsUsePrefs).setVisibility(View.VISIBLE);

            }

            if (!Constants.WIDGET_TYPE_PHOTO_LIST.equals(widgetType) && !Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                findViewById(R.id.blockFontMagnifyPhoto).setVisibility(View.GONE);
            }

            //Скрываем реакцию на нажатие
            if (Constants.WIDGET_TYPE_PHOTO_LIST.equals(widgetType) || Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                findViewById(R.id.blockOnClickLastEvent).setVisibility(View.GONE);
            }

            if (!eventsData.isFeatureEnabled(Constants.FEATURE_SELECT_SOURCES)) {
                //Источники событий
                findViewById(R.id.dividerEventSources).setVisibility(View.GONE);
                findViewById(R.id.captionEventSources).setVisibility(View.GONE);
                findViewById(R.id.listEventSources).setVisibility(View.GONE);
                findViewById(R.id.hintEventSources).setVisibility(View.GONE);
            }

            if (Constants.WIDGET_TYPE_5X1.equals(widgetType) || Constants.WIDGET_TYPE_4X1.equals(widgetType)
                    || Constants.WIDGET_TYPE_2X2.equals(widgetType)) {

                //Скрываем заголовок виджета
                findViewById(R.id.dividerCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.captionCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.editCustomWidgetCaption).setVisibility(View.GONE);
                findViewById(R.id.hintCustomWidgetCaption).setVisibility(View.GONE);

            }

            //Ограничение объёма
            final LinearLayout blockScopeEvents = findViewById(R.id.blockScopeEvents);
            blockScopeEvents.setVisibility(Constants.WIDGET_TYPE_5X1.equals(widgetType) || isListWidget ? View.VISIBLE : View.GONE);
            final LinearLayout blockScopeEventsCount = findViewById(R.id.blockScopeEventsCount);
            blockScopeEventsCount.setVisibility(
                    !Constants.WIDGET_TYPE_5X1.equals(widgetType) || spinnerScopeEvents.getSelectedItemPosition() != 0 ? View.GONE : View.VISIBLE
            );

            //Факты
            List<String> selectedEventTypes = new ArrayList<>();
            for (String eventType: spinnerEventTypes.getSelectedStrings()) {
                selectedEventTypes.add(StringUtils.substringBefore(eventType, Constants.STRING_BRACKETS_OPEN));
            }
            findViewById(R.id.blockFacts).setVisibility(
                    Constants.WIDGET_TYPE_LIST.equals(widgetType)
                            && selectedEventTypes.contains(eventsData.getEventEmojiForType(Constants.EventType_Fact)
                            .concat(Constants.STRING_SPACE)
                            .concat(getString(R.string.pref_List_EventTypes_Facts))) ? View.VISIBLE : View.GONE
            );

            if (isNewPinnedWidget) {
                findViewById(R.id.button_cancel).setVisibility(View.GONE);
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void buttonOkOnClick() {
        try {

            String currentConfig = getCurrentConfig();
            if (currentConfig == null) return;
            this.eventsData.setWidgetPreference(this.widgetId, currentConfig);

            final Intent intent = new Intent();
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetId);
            setResult(Activity.RESULT_OK, intent);

            // Логи ServiceConnectionLeaked в AppWidgetManager — игнорировать, это фальшивая ошибка
            this.eventsData.updateWidgets(this.widgetId, null);

            finish();
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /** Возвращает текущую конфигурацию виджета в виде строки
     * @return Текущая конфигурация виджета
     */
    @Nullable
    private String getCurrentConfig() {
        try {

            //Проверки
            if (this.widgetId == 0) {
                ToastExpander.showInfoMsg(this, getString(R.string.msg_widget_bad_id));
                return null;
            }

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

            //Объём событий
            final StringBuilder scopeInfo = new StringBuilder();
            scopeInfo.append(spinnerScopeEvents.getSelectedItemPosition() == 0 ? Constants.STRING_0 : spinnerScopeEvents.getSelectedItem()).append("e");
            scopeInfo.append(spinnerScopeDays.getSelectedItemPosition() == 0 ? Constants.STRING_0 : spinnerScopeDays.getSelectedItem()).append("d");

            Spinner spinnerLayout = findViewById(R.id.spinnerLayout);
            boolean isPhotoWidget = Constants.WIDGET_TYPE_5X1.equals(widgetType) || Constants.WIDGET_TYPE_4X1.equals(widgetType)
                    || Constants.WIDGET_TYPE_2X2.equals(widgetType);
            if (isPhotoWidget) {

                List<String> spinnerLayoutItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_layout_items).split(Constants.STRING_COMMA, -1)));
                if (spinnerLayout.getSelectedItem().equals(spinnerLayoutItems.get(0))) {
                    scopeInfo.append(Constants.STRING_PLUS);
                } else if (spinnerLayout.getSelectedItem().equals(spinnerLayoutItems.get(1))) {
                    scopeInfo.append(Constants.STRING_MINUS);
                }

            } else if (Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                final Spinner spinnerFacts = findViewById(R.id.spinnerFacts);
                scopeInfo.append(spinnerFacts.getSelectedItem()).append("r");
            }

            //Масштабирование
            int textValue = seekFontMagnifyText.getProgress() + (seekFontMagnifyText.getProgress() != 0 ? minValueSeekOffset : 0);
            int photoValue = seekFontMagnifyPhoto.getProgress() + (seekFontMagnifyPhoto.getProgress() != 0 ? minValueSeekOffset : 0);
            String magnifyParams = String.format(Locale.US, "%.2f+%.2f", textValue / 100.0, photoValue / 100.0);

            //Параметры заголовков
            List<String> selectedCaptionsDetails = new ArrayList<>();
            if (!this.isListWidget) {
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
                    //scopeInfo.append(TextUtils.join(Constants.STRING_PLUS, selectedCaptionsDetails));
                }
            }

            final String eventSources = TextUtils.join(Constants.STRING_PLUS, eventSourcesSelected);

            //Реакция на нажатие
            String[] prefActionsEntries = getResources().getStringArray(R.array.pref_widget_list_onclick_entries);
            String[] prefActionsValues = getResources().getStringArray(R.array.pref_widget_list_onclick_values);
            Spinner spinnerOnClickCommon = findViewById(R.id.spinnerOnClickCommon);
            StringBuilder spinnerOnClickCommonValue = new StringBuilder(Constants.STRING_EMPTY);

            if (spinnerOnClickCommon.getSelectedItemPosition() != Spinner.INVALID_POSITION) {
                int ind = -1;
                String selectedValue = spinnerOnClickCommon.getSelectedItem().toString();
                for (String value: prefActionsEntries) {
                    ind++;
                    if (value.equals(selectedValue)) {
                        try {
                            spinnerOnClickCommonValue = new StringBuilder(prefActionsValues[ind]);
                        } catch (IndexOutOfBoundsException ignored) { /**/ }
                        break;
                    }
                }
            }
            if (isPhotoWidget) {
                Spinner spinnerOnClickLastEvent = findViewById(R.id.spinnerOnClickLastEvent);
                if (spinnerOnClickLastEvent.getSelectedItemPosition() != Spinner.INVALID_POSITION) {
                    int ind = -1;
                    String selectedValue = spinnerOnClickLastEvent.getSelectedItem().toString();
                    for (String value: prefActionsEntries) {
                        ind++;
                        if (value.equals(selectedValue)) {
                            try {
                                spinnerOnClickCommonValue.append(Constants.STRING_PLUS
                                        .concat(prefActionsValues[ind]));
                            } catch (IndexOutOfBoundsException ignored) { /**/ }
                            break;
                        }
                    }
                }
            }

            final int colorWidgetBackground = pickerColorWidgetBackground.getColor();
            final String selectedWidgetBackground = colorWidgetBackground != ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBackground_default)
                    ? ImageUtils.toARGBString(colorWidgetBackground) : Constants.STRING_EMPTY;

            final int colorWidgetBorder = pickerColorWidgetBorder.getColor();
            final String selectedWidgetBorder = colorWidgetBorder != ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBorder_default)
                    ? ImageUtils.toARGBString(colorWidgetBorder) : Constants.STRING_EMPTY;

            //Сохранение настроек
            List<String> prefsToStore = new ArrayList<>();

            prefsToStore.add(spinnerEventShift.getItemAtPosition(spinnerEventShift.getSelectedItemPosition()).toString()); //Стартовый номер события
            prefsToStore.add(magnifyParams); //Масштабирование (позиции в списке выбора)
            prefsToStore.add(String.valueOf(spinnerEventsCount.getSelectedItemPosition())); //Количество событий (позиция в списке выбора)
            prefsToStore.add(eventTypes.toString()); //Типы событий (через +)
            prefsToStore.add(eventInfo.toString()); //Детали события (через +)
            prefsToStore.add(selectedWidgetBackground + Constants.STRING_PLUS + selectedWidgetBorder); //Цвет подложки + бордюра
            prefsToStore.add(String.valueOf(spinnerPhotoStyle.getSelectedItemPosition())); //Стиль фото
            prefsToStore.add(editCustomZeroEvents.getText().toString().replace(Constants.STRING_COMMA, Constants.STRING_EOT)); //Сообщение, когда нет событий
            prefsToStore.add(scopeInfo.toString()); //Объём событий
            prefsToStore.add(editCustomWidgetCaption.getText().toString().replace(Constants.STRING_COMMA, Constants.STRING_EOT)); //Заголовок виджета
            prefsToStore.add(eventSources); //Источники событий (через +)
            prefsToStore.add(TextUtils.join(Constants.STRING_PLUS, selectedCaptionsDetails)); //Параметры заголовков (через +)
            prefsToStore.add(spinnerOnClickCommonValue.toString()); //Реакция на нажатие

            return TextUtils.join(Constants.STRING_COMMA, prefsToStore);

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return null;
        }
    }

    public void buttonCancelOnClick() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        eventsData.initLanguage(this);
        if (!localeAtCreate.equals(eventsData.currentLocale)) {
            recreate();
        } else {
            updateVisibility();
        }
    }
    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.widgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_widget_config, menu);
        this.menuOptions = menu;

        updateMenuState();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.menu_ok) {
            List<String> allSelectedItems = ((RecyclerListFragment) spinnerEventInfo.fragment).adapter.getAllSelectedItems();
            allSelectedItems.remove(getString(R.string.pref_EventInfo_Border));
            if (allSelectedItems.isEmpty()) {
                ToastExpander.showInfoMsg(getApplicationContext(), getString(R.string.msg_no_selection));
            } else {
                spinnerEventInfo.setSelectedFromList(allSelectedItems);
                onBackPressed();
            }
            return true;

        } else if (itemId == R.id.menu_cancel) {

            onBackPressed();
            return true;

        } else if (itemId == R.id.menu_save_template) {
            showSaveTemplateDialog();
            return true;

        } else if (itemId == R.id.menu_load_template) {
            showLoadTemplateDialog();
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

// ==================== Шаблоны конфигурации виджетов ====================

    /**
     * Возвращает имя шаблона по умолчанию: текущая дата и время (дд.ММ.гггг чч:мм)
     */
    @NonNull
    private String getDefaultTemplateName() {
        try {
            return AppDateUtils.getDateTimePreferable(new java.util.Date(),
                    eventsData.preferences_date_format, eventsData.getContext(), eventsData.currentLocale);
        } catch (Exception e) {
            return getString(R.string.msg_template_save_hint);
        }
    }

    /**
     * Диалог сохранения шаблона
     */
    private void showSaveTemplateDialog() {
        try {
            final String currentConfig = getCurrentConfig();
            if (currentConfig == null) {
                ToastExpander.showInfoMsg(this, getString(R.string.msg_widget_bad_id));
                return;
            }

            final EditText editName = new EditText(this);
            editName.setText(getDefaultTemplateName());
            editName.setHint(R.string.msg_template_save_hint);
            editName.setSingleLine(true);
            editName.selectAll();

            int padding = (int) (20 * eventsData.displayMetrics_density);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(padding, padding / 2, padding, 0);
            layout.addView(editName, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog))
                    .setTitle(R.string.msg_template_save_title)
                    .setIcon(android.R.drawable.ic_menu_save)
                    .setView(layout)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                        String templateName = editName.getText().toString().trim();
                        if (templateName.isEmpty()) {
                            ToastExpander.showInfoMsg(WidgetConfigureActivity.this,
                                    getString(R.string.msg_template_empty_name));
                            return;
                        }
                        List<String> existingNames = eventsData.getWidgetTemplateNames(widgetType);
                        if (existingNames.contains(templateName)) {
                            confirmOverwriteTemplate(templateName, currentConfig);
                        } else {
                            boolean saved = eventsData.saveWidgetTemplate(widgetType, templateName, currentConfig);
                            if (saved) {
                                ToastExpander.showInfoMsg(WidgetConfigureActivity.this,
                                        getString(R.string.msg_template_saved, templateName));
                                updateMenuState();
                            } else {
                                // Лимит достигнут
                                showTemplateLimitDialog();
                            }
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            if (eventsData.preferences_theme.themeEditText != 0) {
                builder.getContext().setTheme(eventsData.preferences_theme.themeEditText);
            } else {
                builder.getContext().setTheme(ContactsEvents.themeEditText_default);
            }

            AlertDialog alert = builder.create();
            alert.setOnShowListener(arg0 -> {
                TypedArray ta = getTheme().obtainStyledAttributes(R.styleable.Theme);
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
            editName.requestFocus();
            if (alert.getWindow() != null) alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alert.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Диалог о достижении лимита шаблонов
     */
    private void showTemplateLimitDialog() {
        try {
            new AlertDialog.Builder(new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog))
                    .setTitle(R.string.msg_template_save_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.msg_template_limit_reached, Constants.MAX_WIDGET_TEMPLATES))
                    .setPositiveButton(R.string.button_ok, null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Подтверждение перезаписи существующего шаблона
     */
    private void confirmOverwriteTemplate(@NonNull final String templateName, @NonNull final String config) {
        try {
            new AlertDialog.Builder(new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog))
                    .setTitle(R.string.msg_template_save_title)
                    .setMessage(getString(R.string.msg_template_overwrite, templateName))
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                        boolean saved = eventsData.saveWidgetTemplate(widgetType, templateName, config);
                        if (saved) {
                            ToastExpander.showInfoMsg(WidgetConfigureActivity.this,
                                    getString(R.string.msg_template_saved, templateName));
                            updateMenuState();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Диалог загрузки шаблона
     */
    private void showLoadTemplateDialog() {
        try {
            List<String> templates = eventsData.getWidgetTemplates(widgetType);
            if (templates.isEmpty()) {
                // Закрываем старый диалог, если он ещё открыт
                if (loadTemplateDialog != null && loadTemplateDialog.isShowing()) {
                    loadTemplateDialog.dismiss();
                    loadTemplateDialog = null;
                }
                ToastExpander.showInfoMsg(this, getString(R.string.msg_template_no_templates));
                return;
            }

            // Формируем список имён
            final List<String> templateNames = new ArrayList<>();
            for (String template : templates) {
                int idx = template.indexOf(Constants.STRING_EOT);
                templateNames.add(idx > 0 ? template.substring(0, idx) : template);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.dialog_list_item_template,
                    android.R.id.text1,
                    templateNames.toArray(new String[0]));

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog))
                    .setTitle(R.string.msg_template_load_title)
                    .setIcon(android.R.drawable.ic_menu_upload)
                    .setAdapter(adapter, (dialog, which) -> {
                        // Короткое нажатие — загрузка шаблона
                        String selectedName = templateNames.get(which);
                        String config = eventsData.getWidgetTemplateConfig(widgetType, selectedName);
                        if (config != null && !config.isEmpty()) {
                            applyTemplateConfig(config);
                            ToastExpander.showInfoMsg(WidgetConfigureActivity.this,
                                    getString(R.string.msg_template_loaded, selectedName));
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            AlertDialog alert = builder.create();

            // Долгое нажатие — удаление шаблона
            alert.setOnShowListener(arg0 -> {
                try (TypedArray ta = getTheme().obtainStyledAttributes(R.styleable.Theme)) {
                    alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                }
                // Устанавливаем слушатель долгого нажатия на элементы списка
                if (alert.getListView() != null) {
                    alert.getListView().setOnItemLongClickListener((parent, view, position, id) -> {
                        confirmDeleteTemplate(templateNames.get(position));
                        return true;
                    });
                }
            });
            alert.setOnDismissListener(d -> {
                if (loadTemplateDialog == d) loadTemplateDialog = null;
            });

            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loadTemplateDialog = alert;
            alert.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Подтверждение удаления шаблона
     */
    private void confirmDeleteTemplate(@NonNull final String templateName) {
        try {
            new AlertDialog.Builder(new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog))
                    .setTitle(R.string.msg_template_delete)
                    .setMessage(getString(R.string.msg_template_delete_confirm, templateName))
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                        eventsData.deleteWidgetTemplate(widgetType, templateName);
                        updateMenuState();
                        // Закрываем старый диалог загрузки перед пересозданием
                        if (loadTemplateDialog != null && loadTemplateDialog.isShowing()) {
                            loadTemplateDialog.dismiss();
                            loadTemplateDialog = null;
                        }
                        // Перезапускаем диалог выбора с обновлённым списком
                        showLoadTemplateDialog();
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Обновляет отображение цветов заголовков (текстовые метки)
     * на основе текущих значений colorCaptionUpper и colorCaptionBottom
     */
    void updateCaptionsColorsDisplay() {
        try {
            TextView captionCaptionsUpperColor = findViewById(R.id.captionCaptionsUpperColor);
            captionCaptionsUpperColor.setText(HtmlCompat.fromHtml(
                    (Constants.FONT_COLOR_DOT_START
                            + Integer.toHexString(colorCaptionUpper & 0x00ffffff)
                            + Constants.FONT_COLOR_DOT_END).trim(),
                    HtmlCompat.FROM_HTML_MODE_LEGACY));

            TextView captionCaptionsBottomColor = findViewById(R.id.captionCaptionsBottomColor);
            captionCaptionsBottomColor.setText(HtmlCompat.fromHtml(
                    (Constants.FONT_COLOR_DOT_START
                            + Integer.toHexString(colorCaptionBottom & 0x00ffffff)
                            + Constants.FONT_COLOR_DOT_END).trim(),
                    HtmlCompat.FROM_HTML_MODE_LEGACY));
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void updateSelectedColor(@NonNull String colorId, int colorValue) {
        try {

            if (!colorId.isEmpty()) {
                ToastExpander.showDebugMsg(getApplicationContext(), getString(R.string.msg_event_color_selected, Integer.toHexString(colorValue & 0x00ffffff), colorId));

                if (colorId.equals(UPPER_ROW)) {
                    colorCaptionUpper = colorValue;
                    TextView captionCaptionsUpperColor = findViewById(R.id.captionCaptionsUpperColor);
                    captionCaptionsUpperColor.setText(HtmlCompat.fromHtml(
                            (Constants.FONT_COLOR_DOT_START + Integer.toHexString(colorCaptionUpper & 0x00ffffff) + Constants.FONT_COLOR_DOT_END).trim(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                } else if (colorId.equals(BOTTOM_ROW)) {
                    colorCaptionBottom = colorValue;
                    TextView captionCaptionsBottomColor = findViewById(R.id.captionCaptionsBottomColor);
                    captionCaptionsBottomColor.setText(HtmlCompat.fromHtml(
                            (Constants.FONT_COLOR_DOT_START + Integer.toHexString(colorCaptionBottom & 0x00ffffff) + Constants.FONT_COLOR_DOT_END).trim(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Заполняет все элементы UI из текущего содержимого widgetPref.
     * Вызывается из onCreate() при первой загрузке и из applyTemplateConfig() при загрузке шаблона.
     */
    private void populateUIFromWidgetPref() {
        try {
            if (widgetPref == null || widgetPref.isEmpty()) return;

            // 0: Стартовый номер события
            try {
                int startIdx = Integer.parseInt(widgetPref.get(0));
                if (startIdx >= 1 && startIdx <= spinnerEventShift.getAdapter().getCount()) {
                    spinnerEventShift.setSelection(startIdx - 1, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            // 1: Масштабирование
            populateMagnifyFromPref(widgetPref.size() > 1 ? widgetPref.get(1) : Constants.STRING_EMPTY);

            // 2: Количество событий в ширину
            try {
                int countIdx = Integer.parseInt(widgetPref.size() > 2 ? widgetPref.get(2) : "0");
                if (countIdx >= 0 && countIdx < spinnerEventsCount.getAdapter().getCount()) {
                    spinnerEventsCount.setSelection(countIdx, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            // 3: Типы событий
            populateEventTypesFromPref(widgetPref.size() > 3 ? widgetPref.get(3) : Constants.STRING_EMPTY);

            // 4: Детали события
            populateEventInfoFromPref(widgetPref.size() > 4 ? widgetPref.get(4) : Constants.STRING_EMPTY);

            // 5: Цвета (подложка + бордюр)
            populateColorsFromPref(widgetPref.size() > 5 ? widgetPref.get(5) : Constants.STRING_EMPTY);

            // 6: Стиль фото
            try {
                int photoStyle = Integer.parseInt(widgetPref.size() > 6 ? widgetPref.get(6) : "0");
                if (photoStyle >= 0 && photoStyle < spinnerPhotoStyle.getAdapter().getCount()) {
                    spinnerPhotoStyle.setSelection(photoStyle, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            // 7: Сообщение при отсутствии событий
            String prefZeroEventsMessage = widgetPref.size() > 7 ? widgetPref.get(7) : Constants.STRING_EMPTY;
            editCustomZeroEvents.setText(prefZeroEventsMessage.replace(Constants.STRING_EOT, Constants.STRING_COMMA));

            // 8: Ограничения объёма
            populateScopeFromPref(widgetPref.size() > 8 ? widgetPref.get(8) : Constants.STRING_EMPTY);

            // 9: Заголовок виджета
            String prefWidgetCaption = widgetPref.size() > 9 ? widgetPref.get(9) : Constants.STRING_EMPTY;
            editCustomWidgetCaption.setText(prefWidgetCaption.replace(Constants.STRING_EOT, Constants.STRING_COMMA));

            // 10: Источники событий
            if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                eventSourcesSelected = new ArrayList<>(Arrays.asList(widgetPref.get(10).split(Constants.REGEX_PLUS)));
            } else {
                eventSourcesSelected = new ArrayList<>();
            }
            updateEventSources();

            // 11: Параметры заголовков (для фото-виджетов)
            populateCaptionsFromPref(widgetPref.size() > 11 ? widgetPref.get(11) : Constants.STRING_EMPTY);

            // 12: Реакция на нажатие
            populateOnClickFromPref(widgetPref.size() > 12 ? widgetPref.get(12) : Constants.STRING_EMPTY);

            // Обновляем видимость элементов
            updateVisibility();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Заполняет параметры масштабирования из строки настроек
     */
    private void populateMagnifyFromPref(@NonNull String magnifyStr) {
        try {
            int maxValueSeek = 200;
            int prefMagnifyText = 0;
            int prefMagnifyPhoto = 0;

            if (!magnifyStr.isEmpty()) {
                String[] prefMagnify = magnifyStr.split(Constants.REGEX_PLUS);
                if (magnifyStr.contains(Constants.STRING_PERIOD)) { //В настройках - мультипликатор
                    prefMagnifyText = Math.min((int) Math.round(Double.parseDouble(prefMagnify[0]) * 100), maxValueSeek);
                    if (prefMagnifyText > 0) prefMagnifyText -= minValueSeekOffset;
                    if (prefMagnify.length > 1) {
                        prefMagnifyPhoto = Math.min((int) Math.round(Double.parseDouble(prefMagnify[1]) * 100), maxValueSeek);
                        if (prefMagnifyPhoto > 0) prefMagnifyPhoto -= minValueSeekOffset;
                    }
                } else { //В настройках - индекс в списке (старый формат)
                    List<String> listMagnifyValues = Arrays.asList("0+0.5+0.65+0.75+0.85+1.0+1.1+1.2+1.3+1.4+1.5+1.6+1.75+2.0".split(Constants.REGEX_PLUS));
                    int prefMagnifyIndex = Integer.parseInt(prefMagnify[0]);
                    if (prefMagnifyIndex >= 0 && prefMagnifyIndex < listMagnifyValues.size()) {
                        prefMagnifyText = Math.min((int) Math.round(Double.parseDouble(listMagnifyValues.get(prefMagnifyIndex)) * 100), maxValueSeek);
                        if (prefMagnifyText > 0) prefMagnifyText -= minValueSeekOffset;
                    }
                    if (prefMagnify.length > 1) {
                        prefMagnifyIndex = Integer.parseInt(prefMagnify[1]);
                        if (prefMagnifyIndex >= 0 && prefMagnifyIndex < listMagnifyValues.size()) {
                            prefMagnifyPhoto = Math.min((int) Math.round(Double.parseDouble(listMagnifyValues.get(prefMagnifyIndex)) * 100), maxValueSeek);
                            if (prefMagnifyPhoto > 0) prefMagnifyPhoto -= minValueSeekOffset;
                        }
                    }
                }
            }

            seekFontMagnifyText.setProgress(Math.max(0, prefMagnifyText));
            TextView valueFontMagnifyText = findViewById(R.id.valueFontMagnifyText);
            valueFontMagnifyText.setText(
                    seekFontMagnifyText.getProgress() == 0 ? getString(R.string.widget_config_magnify_auto) :
                            getString(R.string.pref_List_FontMagnify_progress, String.valueOf(minValueSeekOffset + seekFontMagnifyText.getProgress())));

            seekFontMagnifyPhoto.setProgress(Math.max(0, prefMagnifyPhoto));
            TextView valueFontMagnifyPhoto = findViewById(R.id.valueFontMagnifyPhoto);
            valueFontMagnifyPhoto.setText(
                    seekFontMagnifyPhoto.getProgress() == 0 ? getString(R.string.widget_config_magnify_auto) :
                            getString(R.string.pref_List_FontMagnify_progress, String.valueOf(minValueSeekOffset + seekFontMagnifyPhoto.getProgress())));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Заполняет типы событий из строки настроек
     */
    private void populateEventTypesFromPref(@NonNull String typesStr) {
        try {
            List<String> listEventTypes = new ArrayList<>();
            if (!typesStr.isEmpty()) {
                String[] eventsArray = typesStr.split(Constants.REGEX_PLUS);
                for (String item : eventsArray) {
                    if (eventTypesIDs.contains(item)) {
                        listEventTypes.add(eventTypesValues.get(eventTypesIDs.indexOf(item)));
                    }
                }
            }
            spinnerEventTypes.setSelection(listEventTypes);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Заполняет детали события из строки настроек
     */
    private void populateEventInfoFromPref(@NonNull String infoStr) {
        try {
            List<String> eventInfoSelections = new ArrayList<>();
            if (!infoStr.isEmpty()) {
                String[] infoArray = infoStr.split(Constants.REGEX_PLUS);
                for (String item : infoArray) {
                    if (eventInfoIDs.contains(item)) {
                        eventInfoSelections.add(eventInfoValues.get(eventInfoIDs.indexOf(item)));
                    }
                }
            }
            if (Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                spinnerEventInfo.moveToBeginning(eventInfoSelections);
                ArrayList<String> listNonSorted = new ArrayList<String>() {{
                    add(getString(R.string.pref_EventInfo_Border));
                    add(getString(R.string.pref_EventInfo_Dividers));
                    add(getString(R.string.pref_EventInfo_ButtonConfig));
                    add(getString(R.string.pref_EventInfo_ColorizeEntireRow));
                    add(getString(R.string.pref_EventInfo_ShowNearestEventPhoto));
                    add(getString(R.string.pref_EventInfo_Photo));
                }};
                spinnerEventInfo.moveToBeginning(listNonSorted);
            }
            spinnerEventInfo.setSelection(eventInfoSelections);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Заполняет цвета из строки настроек
     */
    private void populateColorsFromPref(@NonNull String colorsStr) {
        try {
            @ColorInt int colorWidgetBackground = ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBackground_default);
            @ColorInt int colorWidgetBorder = ContextCompat.getColor(eventsData.getContext(), R.color.pref_Widgets_Color_WidgetBorder_default);

            if (!colorsStr.isEmpty()) {
                String[] prefColors = colorsStr.split(Constants.REGEX_PLUS, -1);
                if (!prefColors[0].isEmpty()) {
                    colorWidgetBackground = Color.parseColor(prefColors[0]);
                }
                if (prefColors.length > 1 && !prefColors[1].isEmpty()) {
                    colorWidgetBorder = Color.parseColor(prefColors[1]);
                }
            }
            pickerColorWidgetBackground.setColor(colorWidgetBackground);
            pickerColorWidgetBorder.setColor(colorWidgetBorder);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Заполняет ограничения объёма из строки настроек
     */
    private void populateScopeFromPref(@NonNull String scopeStr) {
        try {
            if (TextUtils.isEmpty(scopeStr)) return;

            List<String> spinnerScopeEventsItems;
            if (isListWidget) {
                spinnerScopeEventsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_events_items).split(Constants.STRING_COMMA, -1)));
            } else {
                spinnerScopeEventsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_photo_scope_events_items).split(Constants.STRING_COMMA, -1)));
            }
            List<String> spinnerScopeDaysItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_days_items).split(Constants.STRING_COMMA, -1)));

            Matcher matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE_PLUS).matcher(scopeStr);
            boolean found = matchScopes.find();
            if (!found) {
                matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE_RAND).matcher(scopeStr).reset();
                found = matchScopes.find();
            }
            if (!found) {
                matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE).matcher(scopeStr).reset();
                found = matchScopes.find();
            }
            if (found) {
                final String scopeEvents = matchScopes.group(1);
                if (scopeEvents != null) {
                    if (scopeEvents.equals(Constants.STRING_0)) {
                        spinnerScopeEvents.setSelection(0, true);
                    } else if (spinnerScopeEventsItems.contains(scopeEvents)) {
                        spinnerScopeEvents.setSelection(spinnerScopeEventsItems.indexOf(scopeEvents), true);
                    }
                }
                final String scopeDays = matchScopes.group(2);
                if (scopeDays != null) {
                    if (scopeDays.equals(Constants.STRING_0)) {
                        spinnerScopeDays.setSelection(0, true);
                    } else if (spinnerScopeDaysItems.contains(scopeDays)) {
                        spinnerScopeDays.setSelection(spinnerScopeDaysItems.indexOf(scopeDays), true);
                    }
                }

                if (Constants.WIDGET_TYPE_5X1.equals(widgetType) || Constants.WIDGET_TYPE_4X1.equals(widgetType)
                        || Constants.WIDGET_TYPE_2X2.equals(widgetType)) {
                    Spinner spinnerLayout = findViewById(R.id.spinnerLayout);
                    final String scopeLayout = matchScopes.group(3);
                    if (scopeLayout != null) {
                        if (scopeLayout.equals(Constants.STRING_PLUS)) {
                            spinnerLayout.setSelection(0, true);
                        } else if (scopeLayout.equals(Constants.STRING_MINUS)) {
                            spinnerLayout.setSelection(1, true);
                        }
                    }
                } else if (Constants.WIDGET_TYPE_LIST.equals(widgetType)) {
                    try {
                        Spinner spinnerFacts = findViewById(R.id.spinnerFacts);
                        List<String> spinnerFactsItems = new ArrayList<>(Arrays.asList(getString(R.string.widget_config_scope_facts_items).split(Constants.STRING_COMMA, -1)));
                        final String scopeFacts = matchScopes.group(3);
                        if (scopeFacts != null && spinnerFactsItems.contains(scopeFacts)) {
                            spinnerFacts.setSelection(spinnerFactsItems.indexOf(scopeFacts), true);
                        }
                    } catch (IndexOutOfBoundsException ignored) { /**/ }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Заполняет параметры заголовков из строки настроек (для фото-виджетов)
     */
    private void populateCaptionsFromPref(@NonNull String captionsStr) {
        try {
            if (isListWidget) return;

            // 1. Сначала устанавливаем цвета по умолчанию
            colorCaptionUpper = eventsData.preferences_widgets_color_default;
            colorCaptionBottom = eventsData.preferences_widgets_color_default;

            if (TextUtils.isEmpty(captionsStr)) {
                // Нет сохранённых параметров — просто отображаем дефолтные цвета
                updateCaptionsColorsDisplay();
                return;
            }

            List<String> prefCaptions = new ArrayList<>(Arrays.asList(captionsStr.split(Constants.REGEX_PLUS)));
            if (prefCaptions.size() < Constants.PhotoWidget_Bottom_Color + 1) {
                updateCaptionsColorsDisplay();
                return;
            }

            checkCaptionsUsePrefs.setChecked(false);

            List<String> listBottomInfo = Arrays.asList(getResources().getStringArray(R.array.pref_Widgets_BottomInfo_values));

            // Верхний заголовок
            Spinner spinnerCaptionsUpper = findViewById(R.id.spinnerCaptionsUpper);
            int position = listBottomInfo.indexOf(prefCaptions.get(Constants.PhotoWidget_Upper_Caption));
            if (position != -1 && spinnerCaptionsUpper.getAdapter().getCount() > position) {
                spinnerCaptionsUpper.setSelection(position, true);
            }

            try {
                Spinner spinnerCaptionsUpperAligning = findViewById(R.id.spinnerCaptionsUpperAligning);
                position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Aligning));
                if (spinnerCaptionsUpperAligning.getAdapter().getCount() > position - 1) {
                    spinnerCaptionsUpperAligning.setSelection(position - 1, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            try {
                Spinner spinnerCaptionsUpperRows = findViewById(R.id.spinnerCaptionsUpperRows);
                position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Rows));
                if (spinnerCaptionsUpperRows.getAdapter().getCount() > position - 1) {
                    spinnerCaptionsUpperRows.setSelection(position - 1, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            try {
                Spinner spinnerCaptionsUpperFontStyle = findViewById(R.id.spinnerCaptionsUpperFontStyle);
                position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_FontStyle));
                if (spinnerCaptionsUpperFontStyle.getAdapter().getCount() > position) {
                    spinnerCaptionsUpperFontStyle.setSelection(position, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            try {
                EditText editCaptionsUpperFontSize = findViewById(R.id.editCaptionsUpperFontSize);
                int prefSize = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_FontSize));
                if (prefSize > 0 && prefSize < 100) editCaptionsUpperFontSize.setText(String.valueOf(prefSize));
            } catch (NumberFormatException ignored) { /**/ }

            try {
                colorCaptionUpper = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Color));
            } catch (NumberFormatException ignored) { /**/ }

            // Нижний заголовок
            Spinner spinnerCaptionsBottom = findViewById(R.id.spinnerCaptionsBottom);
            position = listBottomInfo.indexOf(prefCaptions.get(Constants.PhotoWidget_Bottom_Caption));
            if (position != -1 && spinnerCaptionsBottom.getAdapter().getCount() > position) {
                spinnerCaptionsBottom.setSelection(position, true);
            }

            try {
                Spinner spinnerCaptionsBottomAligning = findViewById(R.id.spinnerCaptionsBottomAligning);
                position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Aligning));
                if (spinnerCaptionsBottomAligning.getAdapter().getCount() > position - 1) {
                    spinnerCaptionsBottomAligning.setSelection(position - 1, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            try {
                Spinner spinnerCaptionsBottomRows = findViewById(R.id.spinnerCaptionsBottomRows);
                position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Rows));
                if (spinnerCaptionsBottomRows.getAdapter().getCount() > position - 1) {
                    spinnerCaptionsBottomRows.setSelection(position - 1, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            try {
                Spinner spinnerCaptionsBottomFontStyle = findViewById(R.id.spinnerCaptionsBottomFontStyle);
                position = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_FontStyle));
                if (spinnerCaptionsBottomFontStyle.getAdapter().getCount() > position) {
                    spinnerCaptionsBottomFontStyle.setSelection(position, true);
                }
            } catch (NumberFormatException ignored) { /**/ }

            try {
                EditText editCaptionsBottomFontSize = findViewById(R.id.editCaptionsBottomFontSize);
                int prefSize = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_FontSize));
                if (prefSize > 0 && prefSize < 100) editCaptionsBottomFontSize.setText(String.valueOf(prefSize));
            } catch (NumberFormatException ignored) { /**/ }

            try {
                colorCaptionBottom = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Color));
            } catch (NumberFormatException ignored) { /**/ }

            // 2. Обновляем отображение цветов (текстовые метки)
            updateCaptionsColorsDisplay();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Заполняет реакцию на нажатие из строки настроек
     */
    private void populateOnClickFromPref(@NonNull String onClickStr) {
        try {
            if (TextUtils.isEmpty(onClickStr)) return;

            String[] prefActionsValues = getResources().getStringArray(R.array.pref_widget_list_onclick_values);
            String[] selectedValues = onClickStr.split(Constants.REGEX_PLUS, -1);

            Spinner spinnerOnClickCommon = findViewById(R.id.spinnerOnClickCommon);
            int ind = -1;
            for (String value : prefActionsValues) {
                ind++;
                if (value.equals(selectedValues[0])) {
                    spinnerOnClickCommon.setSelection(ind, true);
                    break;
                }
            }

            if (selectedValues.length > 1) {
                Spinner spinnerOnClickLastEvent = findViewById(R.id.spinnerOnClickLastEvent);
                ind = -1;
                for (String value : prefActionsValues) {
                    ind++;
                    if (value.equals(selectedValues[1])) {
                        spinnerOnClickLastEvent.setSelection(ind, true);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Применяет конфигурацию шаблона к текущим элементам UI активности
     *
     * @param config Строка конфигурации (формат как у getCurrentConfig())
     */
    private void applyTemplateConfig(@NonNull String config) {
        try {
            String[] parts = config.split(Constants.STRING_COMMA, -1);
            List<String> prefList = new ArrayList<>(Arrays.asList(parts));
            // Дополняем до нужного размера (13 полей)
            while (prefList.size() < 13) {
                prefList.add(Constants.STRING_EMPTY);
            }
            widgetPref = prefList;

            // Используем общий метод заполнения
            populateUIFromWidgetPref();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Централизованный метод управления состоянием меню.
     * Вызывается при любом изменении состояния (открытие спиннера, закрытие, смена языка и т.д.)
     */
    private void updateMenuState() {
        if (menuOptions == null) return;

        // Режим редактирования спиннера (показываем только ОК и Отмена)
        menuOptions.findItem(R.id.menu_cancel).setVisible(isSpinnerInEditMode);
        menuOptions.findItem(R.id.menu_ok).setVisible(isSpinnerInEditMode);

        // Обычный режим (скрываем ОК/Отмена, показываем остальное)
        boolean isNormalMode = !isSpinnerInEditMode;

        MenuItem itemHelp = menuOptions.findItem(R.id.menu_help_widgets);
        if (itemHelp != null) itemHelp.setVisible(isNormalMode && eventsData.isContextHelpAvailable());

        MenuItem itemSave = menuOptions.findItem(R.id.menu_save_template);
        if (itemSave != null) itemSave.setVisible(isNormalMode);

        MenuItem itemLoad = menuOptions.findItem(R.id.menu_load_template);
        if (itemLoad != null) itemLoad.setVisible(isNormalMode && eventsData.hasWidgetTemplates(widgetType));
    }

}
