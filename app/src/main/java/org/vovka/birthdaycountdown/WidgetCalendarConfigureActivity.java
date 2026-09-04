/*
 * *
 *  * Created by Vladimir Belov on 05.09.2026, 00:47
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 04.09.2026, 23:50
 *
 */
package org.vovka.birthdaycountdown;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
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
import androidx.transition.TransitionManager;
import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
/**
 Этот класс предоставляет активность конфигурации для виджета "Календарь".
 Он позволяет пользователям настраивать внешний вид и поведение виджета,
 например, количество отображаемых месяцев, начальный месяц,
 отображаемые элементы, источники событий, цвета, размер шрифта и действия при нажатии.
 */
public class WidgetCalendarConfigureActivity extends AppCompatActivity {
    private static final String TAG =  "CalendarConfigActivity ";
    private int widgetId = 0;
    private String widgetType = Constants.WIDGET_TYPE_CALENDAR;
    private final ContactsEvents eventsData = ContactsEvents.getInstance();
    private List <String > widgetPref;
    private final List <String > eventSourcesIds = new ArrayList < >();
    private final List <String > eventSourcesTitles = new ArrayList < >();
    private List <String > eventSourcesSelected = new ArrayList < >();
    private final HashMap <String, Integer > eventSourcesColors = new HashMap < >();
    private AppCompatActivity thisActivity;
    private int customMonthShift = 0;
    private boolean isNewPinnedWidget;
    private String localeAtCreate =  " ";
    CheckBox checkFontMagnifyManual;
    ColorPicker pickerColorArrows;
    ColorPicker pickerColorCommon;
    ColorPicker pickerColorHeaderBack;
    ColorPicker pickerColorMonthTitle;
    ColorPicker pickerColorToday;
    ColorPicker pickerColorWeeks;
    ColorPicker pickerColorWidgetBackground;
    ColorPicker pickerColorWidgetBorder;
    LinearLayout blockFontMagnifyManual;
    SeekBar seekFontMagnify;
    SeekBar seekFontMagnifyDay;
    SeekBar seekFontMagnifyMonth;
    SeekBar seekFontMagnifyWeek;
    Spinner spinnerLayout;
    Spinner spinnerMonthShift;
    Spinner spinnerOnClickCommon;
    Spinner spinnerOnClickHolidays;
    Spinner spinnerStartingMonthPosition;
    TextView valueFontMagnify;
    TextView valueFontMagnifyDay;
    TextView valueFontMagnifyMonth;
    TextView valueFontMagnifyWeek;
    private Menu menuOptions;
    @Nullable
    private AlertDialog loadTemplateDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TypedArray ta = null;
        try {
            thisActivity = this;
            eventsData.initLanguage(this);
            localeAtCreate = eventsData.currentLocale;
            this.setTheme(eventsData.preferences_theme.themeMain);
            setContentView(R.layout.widget_calendar_config);
            View layoutMain = findViewById(R.id.layout_main);
            if (DeviceTools.isEdgeToEdge()) {
                View layoutCoordinator = findViewById(R.id.coordinator);
                ViewCompat.setOnApplyWindowInsetsListener(layoutCoordinator, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
                    Insets insetsStatus = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                    layoutCoordinator.setPadding(0, insets.top, 0, insets.bottom);
                    layoutMain.setPadding(0, insetsStatus.bottom + ImageUtils.Sp2Px(getResources(), 50), 0, 0);
                    return WindowInsetsCompat.CONSUMED;
                });
            } else {
                layoutMain.setPadding(0, ImageUtils.Dip2Px(getResources(), 50), 0, 0);
            }

            //Отступы всего окна
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

            //Цвет заголовка окна
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
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
            } else {
                widgetType = Constants.WIDGET_TYPE_CALENDAR;
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

            // ===== ИНИЦИАЛИЗАЦИЯ UI И СЛУШАТЕЛЕЙ =====
            //Количество месяцев
            spinnerLayout = findViewById(R.id.spinnerMonthsLayout);

            //Стартовый месяц
            spinnerMonthShift = findViewById(R.id.spinnerMonthsShift);

            //Положение
            spinnerStartingMonthPosition = findViewById(R.id.spinnerStartingMonthPosition);

            //Элементы календаря
            List<String> elementsValues = Arrays.asList(getResources().getStringArray(R.array.widget_config_elements_entries));
            final MultiSelectionSpinner spinnerElements = findViewById(R.id.spinnerElements);
            spinnerElements.setItems(elementsValues);

            //Источники событий
            getEventSources();
            TextView listEventSources = findViewById(R.id.listEventSources);
            listEventSources.setOnClickListener(v -> selectEventSources());

            //Размер шрифта
            checkFontMagnifyManual = findViewById(R.id.checkFontMagnifyManual);
            blockFontMagnifyManual = findViewById(R.id.blockFontMagnifyManual);

            seekFontMagnify = findViewById(R.id.seekFontMagnify);
            seekFontMagnify.setMax(25);
            valueFontMagnify = findViewById(R.id.valueFontMagnify);
            this.seekFontMagnify.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    valueFontMagnify.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnify.getProgress() - 5) * 10)));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekFontMagnifyMonth = findViewById(R.id.seekFontMagnifyMonth);
            seekFontMagnifyMonth.setMax(25);
            valueFontMagnifyMonth = findViewById(R.id.valueFontMagnifyMonth);
            seekFontMagnifyMonth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    valueFontMagnifyMonth.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnifyMonth.getProgress() - 5) * 10)));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekFontMagnifyWeek = findViewById(R.id.seekFontMagnifyWeek);
            seekFontMagnifyWeek.setMax(25);
            valueFontMagnifyWeek = findViewById(R.id.valueFontMagnifyWeek);
            seekFontMagnifyWeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    valueFontMagnifyWeek.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnifyWeek.getProgress() - 5) * 10)));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekFontMagnifyDay = findViewById(R.id.seekFontMagnifyDay);
            seekFontMagnifyDay.setMax(25);
            valueFontMagnifyDay = findViewById(R.id.valueFontMagnifyDay);
            seekFontMagnifyDay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    valueFontMagnifyDay.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnifyDay.getProgress() - 5) * 10)));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            checkFontMagnifyManual.setOnCheckedChangeListener((buttonView, isChecked) -> updateVisibility());

            //Реакция на нажатие
            spinnerOnClickCommon = findViewById(R.id.spinnerOnClickCommon);
            spinnerOnClickHolidays = findViewById(R.id.spinnerOnClickHolidays);

            //Цвета
            pickerColorWidgetBackground = findViewById(R.id.colorWidgetBackground);
            pickerColorWidgetBorder = findViewById(R.id.colorWidgetBorder);
            pickerColorCommon = findViewById(R.id.colorCommon);
            pickerColorToday = findViewById(R.id.colorToday);
            pickerColorMonthTitle = findViewById(R.id.colorMonthTitle);
            pickerColorHeaderBack = findViewById(R.id.colorHeaderBack);
            pickerColorArrows = findViewById(R.id.colorArrows);
            pickerColorWeeks = findViewById(R.id.colorWeeks);

            findViewById(R.id.adv_hint).setOnClickListener(v -> {
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (ActivityNotFoundException e) { /**/ }
            });

            // ===== ЗАПОЛНЕНИЕ UI ИЗ СОХРАНЁННЫХ НАСТРОЕК ВИДЖЕТА =====
            populateUIFromWidgetPref();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            updateVisibility();
            if (ta != null) ta.recycle();
        }
    }

    /**
     * Заполняет все элементы UI из текущего содержимого widgetPref.
     * Вызывается из onCreate() при первой загрузке и из applyTemplateConfig() при загрузке шаблона.
     */
    private void populateUIFromWidgetPref() {
        try {
            if (widgetPref == null || widgetPref.isEmpty()) return;

            //Количество месяцев
            try {
                String prefLayout = getString(R.string.widget_config_layout_default);
                if (!widgetPref.isEmpty()) prefLayout = widgetPref.get(0);
                List<String> layouts = Arrays.asList(getResources().getStringArray(R.array.widget_config_layout_values));
                int idx = layouts.lastIndexOf(prefLayout);
                if (idx > -1) spinnerLayout.setSelection(idx);
            } catch (Exception e) {/**/}

            //Стартовый месяц
            try {
                String prefMonthsShift = getString(R.string.widget_config_month_shift_current_month_id);
                if (widgetPref.size() > 1) prefMonthsShift = widgetPref.get(1);
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
                if (prefStartingMonthPosition >= 0 && prefStartingMonthPosition < spinnerStartingMonthPosition.getAdapter().getCount()) {
                    spinnerStartingMonthPosition.setSelection(prefStartingMonthPosition);
                }
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
                spinnerElements.setSelection(selectedElements);
            } catch (Exception e) {/**/}

            //Источники событий
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
            } catch (Exception e) {/**/}

            //Размер шрифта
            try {
                String[] prefFontMagnify;
                int prefFontMagnify_Common = 0;
                int prefFontMagnify_Month = 0;
                int prefFontMagnify_Weekdays = 0;
                int prefFontMagnify_Days = 0;
                if (widgetPref.size() > 6) {
                    prefFontMagnify = widgetPref.get(6).split(Constants.REGEX_PLUS, -1);
                    try {
                        prefFontMagnify_Common = Integer.parseInt(prefFontMagnify[0]);
                        if (prefFontMagnify.length > 3) {
                            prefFontMagnify_Month = Integer.parseInt(prefFontMagnify[1]);
                            prefFontMagnify_Weekdays = Integer.parseInt(prefFontMagnify[2]);
                            prefFontMagnify_Days = Integer.parseInt(prefFontMagnify[3]);
                        } else {
                            this.checkFontMagnifyManual.setChecked(true);
                        }
                    } catch (NumberFormatException ignored) { /**/ }
                }

                seekFontMagnify.setProgress(prefFontMagnify_Common + 5);
                valueFontMagnify.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnify.getProgress() - 5) * 10)));

                seekFontMagnifyMonth.setProgress(prefFontMagnify_Month + 5);
                valueFontMagnifyMonth.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnifyMonth.getProgress() - 5) * 10)));

                seekFontMagnifyWeek.setProgress(prefFontMagnify_Weekdays + 5);
                valueFontMagnifyWeek.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnifyWeek.getProgress() - 5) * 10)));

                seekFontMagnifyDay.setProgress(prefFontMagnify_Days + 5);
                valueFontMagnifyDay.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seekFontMagnifyDay.getProgress() - 5) * 10)));
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
                spinnerOnClickCommon.setSelection(prefOnClickCommon);
                spinnerOnClickHolidays.setSelection(prefOnClickHolidays);
            } catch (Exception e) { /**/ }

            //Цвета
            //Фон виджета + бордюра
            try {
                @ColorInt int colorWidgetBackground = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_Calendar_Back_default);
                @ColorInt int colorWidgetBorder = ContextCompat.getColor(this.eventsData.getContext(), R.color.pref_Widgets_Color_WidgetBorder_default);
                if (widgetPref.size() > 7 && !widgetPref.get(7).isEmpty()) {
                    try {
                        String[] prefColors = widgetPref.get(7).split(Constants.REGEX_PLUS, -1);
                        if (!prefColors[0].isEmpty()) colorWidgetBackground = Color.parseColor(prefColors[0]);
                        if (prefColors.length > 1 && !prefColors[1].isEmpty()) {
                            colorWidgetBorder = Color.parseColor(prefColors[1]);
                        }
                    } catch (IllegalArgumentException ignored) { /**/ }
                }
                pickerColorWidgetBackground.setColor(colorWidgetBackground);
                pickerColorWidgetBorder.setColor(colorWidgetBorder);
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
                pickerColorCommon.setColor(colorCommon);
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
                pickerColorToday.setColor(colorToday);
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
                pickerColorMonthTitle.setColor(colorMonthTitle);
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
                pickerColorHeaderBack.setColor(colorHeaderBack);
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
                pickerColorArrows.setColor(colorArrows);
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
                pickerColorWeeks.setColor(colorWeeks);
            } catch (final Exception e) { /**/ }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void buttonOkOnClick() {
        try {
            String currentConfig = getCurrentConfig();
            if (currentConfig == null) return;
            eventsData.setWidgetPreference(this.widgetId, currentConfig);
            eventsData.clearDaysTypesAndInfo();
            final Intent intent = new Intent();
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.widgetId);
            setResult(Activity.RESULT_OK, intent);
            // Логи ServiceConnectionLeaked в AppWidgetManager — игнорировать, это фальшивая ошибка
            eventsData.updateWidgets(this.widgetId, null);
            finish();
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
    /** Возвращает текущую конфигурацию виджета в виде строки
     @return Текущая конфигурация виджета
     */
    @Nullable
    private String getCurrentConfig() {
        try {
            List<String> layouts = Arrays.asList(getResources().getStringArray(R.array.widget_config_layout_values));
            final String layoutId = spinnerLayout.getSelectedItemPosition() <= layouts.size() - 1 ?
                    layouts.get(spinnerLayout.getSelectedItemPosition()) : getString(R.string.widget_config_layout_default);
            String selectedShift = (String) spinnerMonthShift.getSelectedItem();
            if (selectedShift.equals(getString(R.string.widget_config_month_shift_current_month))) {
                selectedShift = getString(R.string.widget_config_month_shift_current_month_id);
            } else if (selectedShift.equals(getString(R.string.widget_config_month_shift_january))) {
                selectedShift = getString(R.string.widget_config_month_shift_january_id);
            }
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
            final String eventSources = TextUtils.join(Constants.STRING_PLUS, eventSourcesSelected);
            String fontMagnify;
            if (this.checkFontMagnifyManual.isChecked()) {
                fontMagnify = String.valueOf(seekFontMagnify.getProgress() - 5);
            } else {
                fontMagnify =  String.valueOf(seekFontMagnify.getProgress() - 5)
                        .concat(Constants.STRING_PLUS)
                        .concat(String.valueOf(seekFontMagnifyMonth.getProgress() - 5))
                        .concat(Constants.STRING_PLUS)
                        .concat(String.valueOf(seekFontMagnifyWeek.getProgress() - 5))
                        .concat(Constants.STRING_PLUS)
                        .concat(String.valueOf(seekFontMagnifyDay.getProgress() - 5));
            }
            List<String> onclickHolidaysIDs = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_holidays_values));
            List<String> onclickCommonIDs = Arrays.asList(getResources().getStringArray(R.array.pref_widget_month_onclick_common_values));
            final String selectedOnClick = onclickCommonIDs.get(spinnerOnClickCommon.getSelectedItemPosition())
                    .concat(Constants.STRING_PLUS).concat(onclickHolidaysIDs.get(spinnerOnClickHolidays.getSelectedItemPosition()));
            final int colorWidgetBackground = pickerColorWidgetBackground.getColor();
            final String selectedWidgetBackground = colorWidgetBackground != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Back_default)
                    ? ImageUtils.toARGBString(colorWidgetBackground) : Constants.STRING_EMPTY;
            final int colorWidgetBorder = pickerColorWidgetBorder.getColor();
            final String selectedWidgetBorder = colorWidgetBorder != ContextCompat.getColor(this, R.color.pref_Widgets_Color_WidgetBorder_default)
                    ? ImageUtils.toARGBString(colorWidgetBorder) : Constants.STRING_EMPTY;
            final int colorCommon = pickerColorCommon.getColor();
            final String selectedCommon = colorCommon != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Common_default)
                    ? ImageUtils.toARGBString(colorCommon) : Constants.STRING_EMPTY;
            final int colorMonthTitle = pickerColorMonthTitle.getColor();
            final String selectedMonthTitle = colorMonthTitle != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_MonthTitle_default)
                    ? ImageUtils.toARGBString(colorMonthTitle) : Constants.STRING_EMPTY;
            final int colorHeaderBack = pickerColorHeaderBack.getColor();
            final String selectedHeaderBack = colorHeaderBack != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_HeaderBack_default)
                    ? ImageUtils.toARGBString(colorHeaderBack) : Constants.STRING_EMPTY;
            final int colorArrows = pickerColorArrows.getColor();
            final String selectedArrows = colorArrows != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Arrows_default)
                    ? ImageUtils.toARGBString(colorArrows) : Constants.STRING_EMPTY;
            final int colorWeeks = pickerColorWeeks.getColor();
            final String selectedWeeks = colorWeeks != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Weeks_default)
                    ? ImageUtils.toARGBString(colorWeeks) : Constants.STRING_EMPTY;
            final int colorToday = pickerColorToday.getColor();
            final String selectedToday = colorToday != ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Today_default)
                    ? ImageUtils.toARGBString(colorToday) : Constants.STRING_EMPTY;
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
            prefsToStore.add(fontMagnify); //Размер шрифта
            prefsToStore.add(selectedWidgetBackground + Constants.STRING_PLUS + selectedWidgetBorder); //Цвет подложки + бордюра
            prefsToStore.add(selectedCommon); //Цвет обычных дней
            prefsToStore.add(selectedMonthTitle); //Цвет заголовка
            prefsToStore.add(selectedHeaderBack); //Фон заголовка
            prefsToStore.add(selectedArrows); //Цвет стрелок
            prefsToStore.add(selectedWeeks); //Цвет дней недели
            prefsToStore.add(selectedToday); //Цвет дня сегодня
            prefsToStore.add(TextUtils.join(Constants.STRING_PLUS, listColors)); //Цвета календарей
            prefsToStore.add(selectedOnClick); //Действие на нажатие
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
        MenuItem itemHelp = menu.findItem(R.id.menu_help_widgets);
        if (itemHelp != null) {
            itemHelp.setVisible(eventsData.isContextHelpAvailable());
        }
// Обновляем видимость кнопки "Загрузить шаблон"
        updateLoadTemplateVisibility(menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_save_template) {
            showSaveTemplateDialog();
            return true;
        } else if (itemId == R.id.menu_load_template) {
            showLoadTemplateDialog();
            return true;
        } else if (itemId == R.id.menu_help_widgets) {
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

// ==================== Шаблоны конфигурации виджетов ====================
    /**
     * Обновляет видимость пункта меню "Загрузить шаблон"
     */
    private void updateLoadTemplateVisibility(@Nullable Menu menu) {
        if (menu == null) return;
        MenuItem itemLoad = menu.findItem(R.id.menu_load_template);
        if (itemLoad != null) {
            itemLoad.setVisible(eventsData.hasWidgetTemplates(widgetType));
        }
    }

    /**
     * Возвращает имя шаблона по умолчанию: текущая дата и время (дд.ММ.гггг чч:мм)
     */
    @NonNull
    private String getDefaultTemplateName() {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            return sdf.format(new java.util.Date());
        } catch (Exception e) {
            return "Template";
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
                            ToastExpander.showInfoMsg(WidgetCalendarConfigureActivity.this,
                                    getString(R.string.msg_template_empty_name));
                            return;
                        }
                        List<String> existingNames = eventsData.getWidgetTemplateNames(widgetType);
                        if (existingNames.contains(templateName)) {
                            confirmOverwriteTemplate(templateName, currentConfig);
                        } else {
                            boolean saved = eventsData.saveWidgetTemplate(widgetType, templateName, currentConfig);
                            if (saved) {
                                ToastExpander.showInfoMsg(WidgetCalendarConfigureActivity.this,
                                        getString(R.string.msg_template_saved, templateName));
                                updateLoadTemplateVisibility(menuOptions);
                            } else {
                                // Лимит достигнут
                                showTemplateLimitDialog();
                            }
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);
            AlertDialog alert = builder.create();
            alert.setOnShowListener(arg0 -> {
                TypedArray ta = getTheme().obtainStyledAttributes(R.styleable.Theme);
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
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
                            ToastExpander.showInfoMsg(WidgetCalendarConfigureActivity.this,
                                    getString(R.string.msg_template_saved, templateName));
                            updateLoadTemplateVisibility(menuOptions);
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
                            ToastExpander.showInfoMsg(WidgetCalendarConfigureActivity.this,
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
                        updateLoadTemplateVisibility(menuOptions);
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
     * Применяет конфигурацию шаблона к текущим элементам UI активности
     *
     * @param config Строка конфигурации (формат как у getCurrentConfig())
     */
    private void applyTemplateConfig(@NonNull String config) {
        try {
            String[] parts = config.split(Constants.STRING_COMMA, -1);
            List<String> prefList = new ArrayList<>(Arrays.asList(parts));
            // Дополняем до нужного размера (16 полей для календаря)
            while (prefList.size() < 16) {
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

    private static class Source {
        final String packPrefix;
        final Set<String> prefSelected;
        /**
         * @param packPrefix Префикс массива событий в ресурсах
         * @param prefSelected Выбранные в настройках приложения источники
         */
        public Source(@NonNull String packPrefix, @NonNull Set<String> prefSelected) {
            this.packPrefix = packPrefix;
            this.prefSelected = prefSelected;
        }
    }
    @SuppressLint("DiscouragedApi")
    private void getEventSources() {
        try {
            //Выходные
            eventSourcesIds.add(getString(R.string.widget_config_month_events_saturday_id));
            eventSourcesTitles.add(getString(R.string.month_event_saturdays));
            eventSourcesColors.put(getString(R.string.widget_config_month_events_saturday_id),
                    ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Saturday_default));
            eventSourcesIds.add(getString(R.string.widget_config_month_events_sunday_id));
            eventSourcesTitles.add(getString(R.string.month_event_sundays));
            eventSourcesColors.put(getString(R.string.widget_config_month_events_sunday_id),
                    ContextCompat.getColor(this, R.color.pref_Widgets_Color_Calendar_Events_Sunday_default));
            //События избранных контактов
            eventSourcesIds.add(StringUtils.getHash(Constants.eventSourceFavoritePrefix));
            eventSourcesTitles.add(getString(R.string.widget_config_events_favorites));
            //Справочники праздников и выходных
            final ArrayList<Source> sources = new ArrayList<>();
            sources.add(new Source(Constants.STRING_TYPE_HOLIDAY, eventsData.preferences_HolidayEvent_ids));
            sources.add(new Source(Constants.STRING_TYPE_OTHER_HOLIDAY, eventsData.preferences_HolidayEvent_Other_ids));
            for (Source source: sources) {
                int eventsPackCount = 1;
                int packId = getResources().getIdentifier(source.packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, getPackageName());
                while (packId > 0) {
                    try {
                        String[] eventsPack = getResources().getStringArray(packId);
                        String packHash = StringUtils.getHash(Constants.eventSourceHolidayPrefix + eventsPack[0]);
                        if (source.prefSelected.contains(packHash)) {
                            eventSourcesIds.add(packHash);
                            eventSourcesTitles.add(eventsPack[0]);
                        }
                    } catch (Resources.NotFoundException ignored) { /**/ }
                    eventsPackCount++;
                    packId = getResources().getIdentifier(source.packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, getPackageName());
                }
            }
            //Локальные события
            eventSourcesIds.add(StringUtils.getHash(Constants.eventSourceLocalPrefix));
            eventSourcesTitles.add(getString(R.string.widget_config_events_local_events));
            //Календари
            if (!DeviceTools.checkNoCalendarAccess(eventsData.getContext())){
                if (eventsData.map_calendars.isEmpty()) AppDateUtils.fillCalendarList(eventsData.getContext(), eventsData.map_calendars, eventsData.map_calendars_colors);
                List<String> allCalendars = new ArrayList<>(eventsData.preferences_HolidayEvent_calendars);
                if (!allCalendars.isEmpty()) {
                    for (String calendar: allCalendars) {
                        if (eventsData.map_calendars.containsKey(calendar)) {
                            String calendarId = StringUtils.getHash(Constants.eventSourceCalendarPrefix + calendar);
                            eventSourcesIds.add(calendarId);
                            eventSourcesTitles.add(Constants.eventTitleCalendarPrefix + StringUtils.substringBefore(eventsData.map_calendars.get(calendar), Constants.STRING_EOT));
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
                    eventSourcesIds.add(StringUtils.getHash(Constants.eventSourceFilePrefix + file));
                    eventSourcesTitles.add(Constants.eventTitleFilePrefix.concat(StringUtils.substringBefore(file, Constants.STRING_BAR)));
                }
            }
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
                        sb.append(Constants.FONT_COLOR_DOT_START).append(Integer.toHexString(colorValue & 0x00ffffff)).append(Constants.FONT_COLOR_DOT_END);
                    }
                    sb.append(eventSourcesTitles.get(ind));
                }
            }
            ta.recycle();
            if (sb.length() == 0) {
                listEventSources.setText(R.string.widget_config_month_sources_empty);
            } else {
                listEventSources.setText(HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));
            }
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
    private void selectEventSources() {
        try {
            if (eventSourcesIds.isEmpty()) return;
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
            ListAdapter adapter = new ContactsEvents.MultiCheckboxesAdapter(this, sourceChoices, null, null, null, colorDots, ta);
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
            AlertDialog dialogEventsList = builder.create();
            ListView listView = dialogEventsList.getListView();
            listView.setItemsCanFocus(false);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            // 👇 Создаем InsetDrawable с отступами по бокам (5% с каждой стороны = 90% ширина)
            int dividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
            int insetWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.05); // 5% с каждой стороны
            Drawable divider = new InsetDrawable(
                    new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)),
                    insetWidth, 0, insetWidth, 0);
            listView.setDivider(divider);
            listView.setDividerHeight(dividerHeight);
            dialogEventsList.setOnShowListener(arg0 -> {
                dialogEventsList.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                dialogEventsList.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                //Только здесь работает
                for (int i = 0; i < sourceChoices.size(); i++) {
                    String title = StringUtils.substringBefore(sourceChoices.get(i), Constants.STRING_BRACKETS_OPEN);
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
                        //Запоминаем включённые события и временно скрываем диалог со списком событий
                        SparseBooleanArray checked = listView.getCheckedItemPositions();
                        eventSourcesSelected.clear();
                        for (int i = 0; i < checked.size(); i++) {
                            if (checked.get(checked.keyAt(i))) {
                                eventSourcesSelected.add(eventSourcesIds.get(checked.keyAt(i)));
                            }
                        }
                        dialogEventsList.dismiss();
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
                        picker.setDialogTitle(eventSourcesTitles.get(position));
                        picker.setDialogIcon(R.drawable.ic_menu_paste);
                        picker.selectColor(colorValue, colorDefault, true, sourceId, (sourceIdToSave, newColorValue) -> {
                            if (!TextUtils.isEmpty(sourceIdToSave)) {
                                ToastExpander.showDebugMsg(getApplicationContext(), getString(R.string.msg_event_color_selected, Integer.toHexString(newColorValue & 0x00ffffff), sourceIdToSave));
                                eventSourcesColors.put(sourceIdToSave, newColorValue);
                            }
                            selectEventSources();
                        });
                    }
                    return true;
                });
            });
            dialogEventsList.setOnDismissListener(dialog -> ta.recycle());
            dialogEventsList.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogEventsList.show();
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
    private void updateVisibility() {
        try {
            if (isNewPinnedWidget) {
                findViewById(R.id.button_cancel).setVisibility(View.GONE);
            }
            TransitionManager.beginDelayedTransition(findViewById(R.id.layout_main));
            int currentPaintFlags = valueFontMagnify.getPaintFlags();
            if (checkFontMagnifyManual.isChecked()) {
                seekFontMagnify.setEnabled(true);
                valueFontMagnify.setPaintFlags(currentPaintFlags & (~Paint.STRIKE_THRU_TEXT_FLAG));
                blockFontMagnifyManual.setVisibility(View.GONE);
            } else {
                seekFontMagnify.setEnabled(false);
                valueFontMagnify.setPaintFlags(currentPaintFlags | Paint.STRIKE_THRU_TEXT_FLAG);
                blockFontMagnifyManual.setVisibility(View.VISIBLE);
            }
            boolean isAdvSettings = eventsData.isFeatureEnabled(Constants.FEATURE_WIDGETS_MORE_SETTINGS);
            int advSettingsVisibility = isAdvSettings ? View.VISIBLE : View.GONE;
            //Скрываем реакцию на нажатие
            findViewById(R.id.dividerOnClick).setVisibility(advSettingsVisibility);
            findViewById(R.id.captionOnClick).setVisibility(advSettingsVisibility);
            findViewById(R.id.blockOnClickCommon).setVisibility(advSettingsVisibility);
            findViewById(R.id.blockOnClickHolidays).setVisibility(advSettingsVisibility);
            //Скрываем изменения цвета
            findViewById(R.id.dividerColorWidgetBackground).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorWidgetBackground).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorWidgetBorder).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorWidgetBorder).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorCommon).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorCommon).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorToday).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorToday).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorHeader).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorMonthTitle).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorHeaderBack).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorHeaderBack).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorArrows).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorArrows).setVisibility(advSettingsVisibility);
            findViewById(R.id.dividerColorWeeks).setVisibility(advSettingsVisibility);
            findViewById(R.id.colorWeeks).setVisibility(advSettingsVisibility);
            //Подсказки
            findViewById(R.id.hints).setVisibility(isAdvSettings ? View.GONE : View.VISIBLE);
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}