/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 16.06.2025, 15:05
 *
 */

package org.vovka.birthdaycountdown;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

import android.annotation.SuppressLint;
import android.app.LocaleManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.CalendarContract;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.intellij.lang.annotations.JdkConstants;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Виджет Календарь является поставщиком виджетов приложений, который отображает настраиваемый календарь.
 * Он позволяет пользователям просматривать календарь за один или несколько месяцев, настраивать
 * внешний вид календаря и взаимодействовать с календарем для перемещения по месяцам и
 * просмотра событий.
 */
public class WidgetCalendar extends AppWidgetProvider {

    private static final String TAG = "WidgetCalendar";
    private final ContactsEvents eventsData = ContactsEvents.getInstance();
    private Context context;
    private Resources res;
    private final HashMap<String, Integer> eventsColorsInMonth = new HashMap<>();
    private final HashMap<String, Integer> eventsColorsOutMonth = new HashMap<>();
    private boolean showUpdateProgress = false;
    private final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    @ColorInt private int colorCommon;
    @ColorInt private int colorCommonOutMonth;
    @ColorInt private int colorToday;
    private boolean colorizeSaturdays;
    private boolean colorizeSundays;
    private boolean enabledFillDays;
    private float fontMagnify;
    private float fontMagnifyMonthRow = 1; //0.6f;
    private List<String> prefOtherEvents;
    private int prefOnClickCommon = Constants.onClick_None;
    private int prefOnClickHolidays = Constants.onClick_None;
    private boolean atLeastOneDayInMonth;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted (Context context, int[] widgetIds) {

        for (int widgetId : widgetIds) {
            eventsData.removeWidgetPreference(widgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        try {

            //Log.i("onReceive", "appWidgetId = " + appWidgetId);
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

            final AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);
            boolean needSavePref = false;

            //Ручное смещение месяцев
            int customMonthShift = 0;
            try {
                if (widgetPref.size() > 3) customMonthShift = Integer.parseInt(widgetPref.get(3));
            } catch (Exception e) {/**/}

            String action = intent.getAction();
            //Log.i("action", "action = " + action);
            if (Constants.ACTION_PREVIOUS_MONTH.equals(action)) {

                customMonthShift--;
                needSavePref = true;
                showUpdateProgress = true;

            } else if (Constants.ACTION_NEXT_MONTH.equals(action)) {

                customMonthShift++;
                needSavePref = true;
                showUpdateProgress = true;

            } else if (Constants.ACTION_RESET_MONTH.equals(action)) {

                customMonthShift = 0;
                needSavePref = true;
                showUpdateProgress = true;

            }

            if (needSavePref) {
                widgetPref.set(3, Integer.toString(customMonthShift));
                eventsData.setWidgetPreference(appWidgetId, String.join(Constants.STRING_COMMA, widgetPref));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @SuppressLint("DiscouragedApi")
    private void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        long statCurrentModuleStart = System.currentTimeMillis();
        this.context = context;
        this.res = context.getResources();

        try {

            AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
            String widgetType = Constants.WIDGET_TYPE_CALENDAR;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }

            if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {
                if (eventsData.getContext() == null) eventsData.setContext(context);
                eventsData.getPreferences();
                eventsData.setLocale(true);
                eventsData.getEvents(context);
            }

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(context.getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources applicationRes = context.getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    LocaleList list = context.getSystemService(LocaleManager.class).getApplicationLocales();
                    if (!list.isEmpty()) {
                        locale = context.getSystemService(LocaleManager.class).getApplicationLocales().get(0);
                    }
                }
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            int columnsMax = 4;
            int columnsToDraw = 3;
            int rowsMax = 4;
            int rowsToDraw = 4;
            int numWeeks = 6;

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);

            //Прогресс обновления
            if (showUpdateProgress) {
                rv.setInt(R.id.calendarAll, Constants.METHOD_SET_BACKGROUND_COLOR, 0);
                for (int row = 1; row <= rowsMax; row++) {
                    int id = res.getIdentifier(Constants.RES_TYPE_CALENDAR.concat(String.valueOf(row)), Constants.STRING_ID, context.getPackageName());
                    rv.setViewVisibility(id, View.GONE);
                }
                rv.setViewVisibility(R.id.progressUpdate, View.VISIBLE);
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, rv);
            }

            //Настройки
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            //Размер шрифта
            int prefFontMagnify = 0;
            try {
                if (widgetPref.size() > 6) prefFontMagnify = Integer.parseInt(widgetPref.get(6));
            } catch (Exception e) {/**/}
            fontMagnify = (float) (1 + prefFontMagnify * 0.1);

            //Количество месяцев
            String prefLayout = res.getString(R.string.widget_config_layout_default);
            try {
                if (!widgetPref.isEmpty()) prefLayout = widgetPref.get(0);
            } catch (Exception e) {/**/}

            if (prefLayout.equals(res.getString(R.string.widget_config_layout_default))) {

                Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
                if (widgetOptions != null) {
                    int minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                    float minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) / fontMagnify;
                    int minWidthPixel = ContactsEvents.Dip2Px(res, minWidthDp);
                    float heightRatio = (float)res.getDisplayMetrics().heightPixels * fontMagnify / minWidthPixel;

                    if (heightRatio > 4.5) {
                        columnsToDraw = 1;
                    } else if (heightRatio > 3) {
                        columnsToDraw = 2;
                    }

                    if (minHeightDp < (ContactsEvents.isSamsung() ? 125 : 60)) { //s: 118, 8.1: 58, 13+12+11: 54
                        rowsToDraw = 1;
                    } else if (minHeightDp < (ContactsEvents.isSamsung() ? 255 : 140)) { //s:249, 8.1: 133, 13+12+11: 125
                        rowsToDraw = 2;
                    } else if (minHeightDp < (ContactsEvents.isSamsung() ? 385 : 210)) { //s:379, 8.1: 207, 13: 195, 12: 196
                        rowsToDraw = 3;
                    }
                }

            } else {

                rowsToDraw = 1;
                columnsToDraw = 1;

                Matcher matchLayout = Pattern.compile(Constants.REGEX_CALENDAR_LAYOUT).matcher(prefLayout);
                if (matchLayout.find()) {
                    final String rows = matchLayout.group(1);
                    if (rows != null) {
                        try {
                            rowsToDraw = Integer.parseInt(rows);
                            if (rowsToDraw < 1) rowsToDraw = 1;
                        } catch (NumberFormatException ignored) { /**/ }
                    }
                    final String columns = matchLayout.group(2);
                    if (columns != null) {
                        try {
                            columnsToDraw = Integer.parseInt(columns);
                            if (columnsToDraw < 1) columnsToDraw = 1;
                        } catch (NumberFormatException ignored) { /**/ }
                    }
                }
            }

            //Стартовый месяц
            int prefMonthsShift = 0;
            try {
                if (widgetPref.size() > 1) prefMonthsShift = Integer.parseInt(widgetPref.get(1));
                if (prefMonthsShift == Integer.parseInt(res.getString(R.string.widget_config_month_shift_january_id))) {
                    Calendar cal = Calendar.getInstance();
                    prefMonthsShift = - cal.get(Calendar.MONTH);
                }
            } catch (Exception e) {/**/}

            //Положение
            int prefStartingMonthPosition = 0;
            try {
                if (widgetPref.size() > 2) prefStartingMonthPosition = Integer.parseInt(widgetPref.get(2));
            } catch (Exception e) {/**/}
            if (prefStartingMonthPosition == 1) { //Вверху по центру
                prefMonthsShift -= columnsToDraw / 2;
            } else if (prefStartingMonthPosition == 2) { //По центру
                prefMonthsShift -= (columnsToDraw / 2) + ((rowsToDraw - 1) / 2) * columnsToDraw;
            }

            //Ручное смещение месяцев
            int customMonthShift = 0;
            try {
                if (widgetPref.size() > 3) customMonthShift = Integer.parseInt(widgetPref.get(3));
            } catch (Exception e) {/**/}
            prefMonthsShift += customMonthShift * rowsToDraw * columnsToDraw;

            //Элементы календаря
            List<String> prefElements = new ArrayList<>();
            try {
                if (widgetPref.size() > 4) prefElements = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS, -1));
             } catch (Exception e) {/**/}
            boolean enabledHeader = prefElements.contains(res.getString(R.string.widget_config_elements_month));
            boolean enabledWeeks = prefElements.contains(res.getString(R.string.widget_config_elements_weeks));
            boolean enabledMargins = prefElements.contains(res.getString(R.string.widget_config_elements_margins));
            enabledFillDays = prefElements.contains(res.getString(R.string.widget_config_elements_fill_days));

            //Источники событий и цвета по умолчанию
            List<String> prefEvents = new ArrayList<>();
            try {
                if (widgetPref.size() > 5) prefEvents = Arrays.asList(widgetPref.get(5).split(Constants.REGEX_PLUS, -1));

                @ColorInt int colorSaturday_default = res.getColor(R.color.pref_Widgets_Color_Calendar_Events_Saturday_default);
                @ColorInt int colorSunday_default = res.getColor(R.color.pref_Widgets_Color_Calendar_Events_Sunday_default);
                @ColorInt int colorEvents_default = res.getColor(R.color.pref_Widgets_Color_Calendar_Events_default);

                for (String eventId: prefEvents) {

                    if (eventId.equals(res.getString(R.string.widget_config_month_events_saturday_id))) {

                        eventsColorsInMonth.put(res.getString(R.string.widget_config_month_events_saturday_id), colorSaturday_default);
                        eventsColorsOutMonth.put(res.getString(R.string.widget_config_month_events_saturday_id),
                                Color.argb(Constants.WIDGET_CALENDAR_OUT_MONTH_TINT, Color.red(colorSaturday_default), Color.green(colorSaturday_default), Color.blue(colorSaturday_default)));

                    } else if (eventId.equals(res.getString(R.string.widget_config_month_events_sunday_id))) {

                        eventsColorsInMonth.put(res.getString(R.string.widget_config_month_events_sunday_id), colorSunday_default);
                        eventsColorsOutMonth.put(res.getString(R.string.widget_config_month_events_sunday_id),
                                Color.argb(Constants.WIDGET_CALENDAR_OUT_MONTH_TINT, Color.red(colorSunday_default), Color.green(colorSunday_default), Color.blue(colorSunday_default)));

                    } else {

                        eventsColorsInMonth.put(eventId, colorEvents_default);
                        eventsColorsOutMonth.put(eventId,
                                Color.argb(Constants.WIDGET_CALENDAR_OUT_MONTH_TINT, Color.red(colorEvents_default), Color.green(colorEvents_default), Color.blue(colorEvents_default)));

                    }

                }
            } catch (Exception e) {/**/}
            colorizeSaturdays = prefEvents.contains(res.getString(R.string.widget_config_month_events_saturday_id));
            colorizeSundays = prefEvents.contains(res.getString(R.string.widget_config_month_events_sunday_id));
            prefOtherEvents = new ArrayList<>(prefEvents);
            prefOtherEvents.remove(res.getString(R.string.widget_config_month_events_saturday_id));
            prefOtherEvents.remove(res.getString(R.string.widget_config_month_events_sunday_id));

            //Реакция на нажатие
            try {
                List<String> onclickIDs = Arrays.asList(res.getStringArray(R.array.pref_widget_month_onclick_holidays_values));
                String[] prefOnClick = null;

                if (widgetPref.size() > 15) prefOnClick = widgetPref.get(15).split(Constants.REGEX_PLUS, -1);
                if (prefOnClick != null && prefOnClick.length == 2) {
                    if (onclickIDs.contains(prefOnClick[0])) {
                        prefOnClickCommon = onclickIDs.indexOf(prefOnClick[0]);  //Смещение, не значение
                    }
                    if (onclickIDs.contains(prefOnClick[1])) {
                        prefOnClickHolidays = onclickIDs.indexOf(prefOnClick[1]);
                    }
                }
            } catch (Exception e) { /**/ }

            //Цвета

            //Фон виджета
            @ColorInt int colorWidgetBackground = res.getColor(R.color.pref_Widgets_Color_Calendar_Back_default);
            if (widgetPref.size() > 7 && !widgetPref.get(7).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(7));
                } catch (final Exception e) {/* */}
            }

            //Обычные дни
            colorCommon = res.getColor(R.color.pref_Widgets_Color_Calendar_Common_default);
            if (widgetPref.size() > 8 && !widgetPref.get(8).isEmpty()) {
                try {
                    colorCommon = Color.parseColor(widgetPref.get(8));
                } catch (final Exception e) {/* */}
            }
            colorCommonOutMonth = Color.argb(Constants.WIDGET_CALENDAR_OUT_MONTH_TINT, Color.red(colorCommon), Color.green(colorCommon), Color.blue(colorCommon));

            //Сегодня
            colorToday = res.getColor(R.color.pref_Widgets_Color_Calendar_Today_default);
            if (widgetPref.size() > 13 && !widgetPref.get(13).isEmpty()) {
                try {
                    colorToday = Color.parseColor(widgetPref.get(13));
                } catch (final Exception e) {/* */}
            }

            //Заголовок
            int colorMonthTitle = res.getColor(R.color.pref_Widgets_Color_Calendar_MonthTitle_default);
            if (widgetPref.size() > 9 && !widgetPref.get(9).isEmpty()) {
                try {
                    colorMonthTitle = Color.parseColor(widgetPref.get(9));
                } catch (final Exception e) {/* */}
            }

            //Фон заголовка
            @ColorInt int colorHeaderBack = res.getColor(R.color.pref_Widgets_Color_Calendar_HeaderBack_default);
            if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                try {
                    colorHeaderBack = Color.parseColor(widgetPref.get(10));
                } catch (final Exception e) {/* */}
            }

            //Стрелки
            @ColorInt int colorArrows = res.getColor(R.color.pref_Widgets_Color_Calendar_Arrows_default);
            if (widgetPref.size() > 11 && !widgetPref.get(11).isEmpty()) {
                try {
                    colorArrows = Color.parseColor(widgetPref.get(11));
                } catch (final Exception e) {/* */}
            }

            //Дни недели
            @ColorInt int colorWeeks = res.getColor(R.color.pref_Widgets_Color_Calendar_Weeks_default);
            if (widgetPref.size() > 12 && !widgetPref.get(12).isEmpty()) {
                try {
                    colorWeeks = Color.parseColor(widgetPref.get(12));
                } catch (final Exception e) {/* */}
            }

            //Пользовательские цвета событий
            List<String> prefEventsColors = new ArrayList<>();
            try {
                if (widgetPref.size() > 14) prefEventsColors = Arrays.asList(widgetPref.get(14).split(Constants.REGEX_PLUS, -1));

                for (String color: prefEventsColors) {
                    String[] colors = color.split(Constants.STRING_COLON, -1);
                    if (colors.length == 2) {
                        String eventId = colors[0];
                        if (prefEvents.contains(eventId)) {
                            try {

                                @ColorInt int colorValue = Integer.parseInt(colors[1]);
                                eventsColorsInMonth.put(eventId, colorValue);
                                //Если цвет календаря прозрачный - оставляем прозрачным
                                if (Color.alpha(colorValue) == 0) {
                                    eventsColorsOutMonth.put(eventId, colorValue);
                                } else {
                                    //Иначе делаем затемнение для дней вне месяца
                                    eventsColorsOutMonth.put(eventId,
                                            Color.argb(Constants.WIDGET_CALENDAR_OUT_MONTH_TINT, Color.red(colorValue),
                                                    Color.green(colorValue), Color.blue(colorValue)));
                                }

                            } catch (NumberFormatException ignored) {/**/}
                        }
                    }
                }
            } catch (Exception e) {/**/}

            int sidePadding = enabledMargins ? (int) (4 * fontMagnify) : 0 ;

            Calendar cal = Calendar.getInstance();
            cal.setMinimalDaysInFirstWeek(1);
            DateFormatSymbols dfs = DateFormatSymbols.getInstance();
            String[] weekdays;
            if (cal.getFirstDayOfWeek() == Calendar.SUNDAY) {
                weekdays = dfs.getShortWeekdays();
            } else { //Воскресенье - в конец списка
                List<String> listWeekDays = new ArrayList<>(Arrays.asList(dfs.getShortWeekdays()));
                String daySun = listWeekDays.remove(1);
                listWeekDays.add(daySun);
                weekdays = listWeekDays.toArray(new String[0]);
            }
            //Убираем точки (pt, fr)
            for (int i = 0, weekdaysLength = weekdays.length; i < weekdaysLength; i++) {
                String day = weekdays[i];
                if (day.contains(Constants.STRING_PERIOD)) {
                    weekdays[i] = day.replace(Constants.STRING_PERIOD, Constants.STRING_EMPTY);
                }
            }

            int today = cal.get(Calendar.DAY_OF_YEAR);
            int todayYear = cal.get(Calendar.YEAR);

            rv.setInt(R.id.calendarAll, Constants.METHOD_SET_BACKGROUND_COLOR, colorWidgetBackground);

            for (int row = 1; row <= rowsMax; row++) {
                int id = res.getIdentifier(Constants.RES_TYPE_CALENDAR.concat(String.valueOf(row)), Constants.STRING_ID, context.getPackageName());
                if (row <= rowsToDraw) {
                    rv.setViewVisibility(id, View.VISIBLE);
                    for (int column = 1; column <= columnsMax; column++) {
                        String idRow = Constants.RES_TYPE_CALENDAR.concat(String.valueOf(row)).concat("x").concat(String.valueOf(column));
                        id = res.getIdentifier(idRow, Constants.STRING_ID, context.getPackageName());
                        int idDiv = res.getIdentifier(idRow.concat("div"), Constants.STRING_ID, context.getPackageName());
                        if (id != 0) {
                            rv.removeAllViews(id);
                            if (column > columnsToDraw) {
                                rv.setViewVisibility(id, View.GONE);
                                rv.setViewVisibility(idDiv, View.GONE);
                            } else {
                                rv.setViewVisibility(id, View.VISIBLE);
                                rv.setViewVisibility(idDiv, View.VISIBLE);
                                if (column == 1) { //Отступ слева
                                    rv.setViewPadding(id, ContactsEvents.Dip2Px(res, sidePadding), 0, 0, ContactsEvents.Dip2Px(res, 4));
                                } else if (column == columnsToDraw) { //Отступ справа
                                    rv.setViewPadding(id, 0, 0, ContactsEvents.Dip2Px(res, sidePadding), ContactsEvents.Dip2Px(res, 4));
                                } else {
                                    rv.setViewPadding(id, 0, 0, 0, ContactsEvents.Dip2Px(res, 4));
                                }

                            }
                        }
                    }
                } else {
                    rv.setViewVisibility(id, View.GONE);
                }
            }

            //Определение периода показа дней
            int monthsInYear = rowsToDraw * columnsToDraw;
            {
                Calendar calFirstDay = (Calendar) cal.clone();
                calFirstDay.add(Calendar.MONTH, prefMonthsShift);
                calFirstDay.set(Calendar.DAY_OF_MONTH, 1);
                int monthStartDayOfWeek = calFirstDay.get(Calendar.DAY_OF_WEEK);
                Calendar calLastDay = (Calendar) calFirstDay.clone();
                calLastDay.add(Calendar.MONTH, monthsInYear - 1);
                calLastDay.set(Calendar.DATE, calLastDay.getActualMaximum(Calendar.DATE));

                if (calFirstDay.getFirstDayOfWeek() == Calendar.SUNDAY) { //вс - в начале
                    calFirstDay.add(Calendar.DAY_OF_MONTH, 1 - monthStartDayOfWeek);
                } else {
                    if (monthStartDayOfWeek == 1) {
                        calFirstDay.add(Calendar.DAY_OF_MONTH, -6);
                    } else {
                        calFirstDay.add(Calendar.DAY_OF_MONTH, 2 - monthStartDayOfWeek);
                    }
                }

                if (calLastDay.getFirstDayOfWeek() == Calendar.SUNDAY) { //вс - в начале
                    calLastDay.add(Calendar.DAY_OF_MONTH, 7 - calLastDay.get(Calendar.DAY_OF_WEEK));
                } else {
                    if (calLastDay.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                        calLastDay.add(Calendar.DAY_OF_MONTH, 8 - calLastDay.get(Calendar.DAY_OF_WEEK));
                    }
                }
                Log.i(TAG + ".Period", ContactsEvents.sdf_DDMMYYYY.format(calFirstDay.getTime()) + " : " + ContactsEvents.sdf_DDMMYYYY.format(calLastDay.getTime()));

                //Заполнение типов дней из календарей по периоду
                eventsData.fillDaysTypesFromCalendars(prefOtherEvents, calFirstDay, calLastDay);
                //Заполнение типов дней из файлов
                eventsData.fillDaysTypesFromFiles(prefOtherEvents);
            }

            for (int row = 1; row <= rowsToDraw; row++) {
                for (int column = 1; column <= columnsToDraw; column++) {

                    if (column * row > 1) {
                        cal = Calendar.getInstance();
                        cal.setMinimalDaysInFirstWeek(1);
                        cal.add(Calendar.MONTH, ((row - 1) * columnsToDraw) + column - 1);
                    }
                    cal.add(Calendar.MONTH, prefMonthsShift);
                    final int thisMonth = cal.get(Calendar.MONTH);
                    cal.set(Calendar.DAY_OF_MONTH, 1);

                    //Шапка
                    RemoteViews calendarRv = new RemoteViews(context.getPackageName(), R.layout.widget_calendar_month);
                    calendarRv.setInt(R.id.month_bar, Constants.METHOD_SET_BACKGROUND_COLOR, colorHeaderBack);

                    if (enabledHeader) {
                        calendarRv.setViewVisibility(R.id.month_bar, View.VISIBLE);
                        calendarRv.setTextColor(R.id.month_label, colorMonthTitle);
                        calendarRv.setTextColor(R.id.prev_month_button, colorArrows);
                        calendarRv.setTextColor(R.id.next_month_button, colorArrows);
                        if (prefElements.contains(res.getString(R.string.widget_config_elements_year)) || (monthsInYear == 12 && cal.get(Calendar.MONTH) == Calendar.JANUARY)) {
                            calendarRv.setTextViewText(R.id.month_label, DateFormat.format(Constants.DATE_LLLL_YYYY, cal).toString().toUpperCase());
                        } else {
                            calendarRv.setTextViewText(R.id.month_label, DateFormat.format("LLLL", cal).toString().toUpperCase());
                        }
                        //todo: добавить масштабирование для отдельных элементов календаря: заголовка, дней недели, дней
                        calendarRv.setTextViewTextSize(R.id.month_label, COMPLEX_UNIT_SP, 12 * fontMagnify * fontMagnifyMonthRow);
                        //calendarRv.setFloat(R.id.month_label, Constants.METHOD_SET_SCALE_Y, 0.66f);
                        if (row == rowsToDraw) {
                            calendarRv.setViewVisibility(R.id.bottom_divider, View.GONE);
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                calendarRv.setViewLayoutHeight(R.id.bottom_divider, 2 * fontMagnify, COMPLEX_UNIT_SP);
                            }
                        }
                    } else {
                        calendarRv.setViewVisibility(R.id.month_bar, View.GONE);
                    }

                    //Первый день недели - ПН или ВСК
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    int monthStartDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

                    if (cal.getFirstDayOfWeek() == Calendar.SUNDAY) { //вс - в начале
                        cal.add(Calendar.DAY_OF_MONTH, 1 - monthStartDayOfWeek);
                    } else {
                        if (monthStartDayOfWeek == 1) {
                            cal.add(Calendar.DAY_OF_MONTH, -6);
                        } else {
                            cal.add(Calendar.DAY_OF_MONTH, 2 - monthStartDayOfWeek);

                        }
                    }

                    //Дни недели
                    if (enabledWeeks) {
                        RemoteViews headerRowRv = new RemoteViews(context.getPackageName(), R.layout.row_weekdays);
                        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                            RemoteViews dayRv = new RemoteViews(context.getPackageName(), R.layout.cell_day);
                            dayRv.setTextColor(android.R.id.text1, colorWeeks);
                            dayRv.setTextViewText(android.R.id.text1, weekdays[day]);
                            dayRv.setTextViewTextSize(android.R.id.text1, COMPLEX_UNIT_SP, 10 * fontMagnify);
                            headerRowRv.addView(R.id.row_container, dayRv);
                        }
                        headerRowRv.setInt(R.id.row_container, Constants.METHOD_SET_BACKGROUND_COLOR, colorHeaderBack);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            headerRowRv.setViewLayoutMargin(R.id.row_container, RemoteViews.MARGIN_TOP, -4, COMPLEX_UNIT_SP);
                        }
                        calendarRv.addView(R.id.days, headerRowRv);
                    }

                    //Дни
                    for (int week = 0; week < numWeeks; week++) {
                        RemoteViews rowRv = new RemoteViews(context.getPackageName(), R.layout.row_week);
                        atLeastOneDayInMonth = false;
                        for (int day = 0; day < 7; day++) {
                            RemoteViews rvCell = composeDayCell(cal, todayYear, thisMonth, today, appWidgetId);
                            if (rvCell != null) rowRv.addView(R.id.row_container, rvCell);
                            cal.add(Calendar.DAY_OF_MONTH, 1);
                        }
                        if (week < numWeeks - 1 || atLeastOneDayInMonth) { //Если не последняя неделя или есть хоть 1 день в месяце
                            calendarRv.addView(R.id.days, rowRv);
                        }
                    }

                    // не подходит - высота не меняется, только визуальный размер
                    //calendarRv.setFloat(R.id.prev_month_button, Constants.METHOD_SET_SCALE_Y, 0.66f);
                    //calendarRv.setFloat(R.id.next_month_button, Constants.METHOD_SET_SCALE_Y, 0.66f);

                    if (row > 1 || (column != 1 && columnsToDraw > 1)) {
                        calendarRv.setViewVisibility(R.id.prev_month_button, View.INVISIBLE);
                        calendarRv.setInt(R.id.prev_month_button, Constants.METHOD_SET_MIN_WIDTH, 1);
                    } else {
                        calendarRv.setInt(R.id.prev_month_button, Constants.METHOD_SET_MIN_WIDTH, ContactsEvents.Dip2Px(res, 17));
                        //calendarRv.setViewVisibility(R.id.prev_month_button, View.VISIBLE);
                        calendarRv.setTextViewText(R.id.prev_month_button, res.getText(R.string.previous_month_arrow));
                        calendarRv.setTextViewTextSize(R.id.prev_month_button, COMPLEX_UNIT_SP, 12 * fontMagnify * fontMagnifyMonthRow);
                        calendarRv.setInt(R.id.prev_month_button, Constants.METHOD_SET_BACKGROUND_RES, R.drawable.cell_day);
                        calendarRv.setOnClickPendingIntent(R.id.prev_month_button, PendingIntent.getBroadcast(context, appWidgetId,
                                new Intent(context, WidgetCalendar.class)
                                        .setAction(Constants.ACTION_PREVIOUS_MONTH)
                                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                , PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));
                    }

                    if (row > 1 || (column < columnsToDraw && columnsToDraw > 1)) {
                        calendarRv.setViewVisibility(R.id.next_month_button, View.INVISIBLE);
                        calendarRv.setInt(R.id.next_month_button, Constants.METHOD_SET_MIN_WIDTH, 1);
                    } else {
                        calendarRv.setInt(R.id.next_month_button, Constants.METHOD_SET_MIN_WIDTH, ContactsEvents.Dip2Px(res, 17));
                        //calendarRv.setViewVisibility(R.id.next_month_button, View.VISIBLE);
                        calendarRv.setTextViewText(R.id.next_month_button, res.getText(R.string.next_month_arrow));
                        calendarRv.setTextViewTextSize(R.id.next_month_button, COMPLEX_UNIT_SP, 12 * fontMagnify * fontMagnifyMonthRow);
                        calendarRv.setInt(R.id.next_month_button, Constants.METHOD_SET_BACKGROUND_RES, R.drawable.cell_day);
                        calendarRv.setOnClickPendingIntent(R.id.next_month_button, PendingIntent.getBroadcast(context, appWidgetId,
                                new Intent(context, WidgetCalendar.class)
                                        .setAction(Constants.ACTION_NEXT_MONTH)
                                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                , PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));
                    }

                    calendarRv.setInt(R.id.month_label, Constants.METHOD_SET_BACKGROUND_RES, R.drawable.cell_day);
                    if (!ContactsEvents.isWidgetSupportConfig() && row == 1 && column == columnsToDraw) {
                        Intent intentConfig = new Intent(context, WidgetCalendarConfigureActivity.class);
                        intentConfig.setAction(Constants.ACTION_LAUNCH);
                        intentConfig.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        calendarRv.setOnClickPendingIntent(R.id.month_label, PendingIntent.getActivity(context, appWidgetId, intentConfig, PendingIntentImmutable));
                    } else {
                        calendarRv.setOnClickPendingIntent(R.id.month_label, PendingIntent.getBroadcast(context, appWidgetId,
                                new Intent(context, WidgetCalendar.class)
                                        .setAction(Constants.ACTION_RESET_MONTH)
                                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                , PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));
                    }

                    int id = res.getIdentifier(Constants.RES_TYPE_CALENDAR + row + "x" + column, Constants.STRING_ID, context.getPackageName());
                    if (id != 0) {
                        rv.addView(id, calendarRv);
                    }
                }
            }

            ToastExpander.showDebugMsg(context, widgetType + Constants.STRING_COLON + appWidgetId + Constants.STRING_EOL + widgetPref);

            rv.setViewVisibility(R.id.progressUpdate, View.GONE);
            appWidgetManager.updateAppWidget(appWidgetId, rv);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            eventsData.statTimeUpdateWidgets += System.currentTimeMillis() - statCurrentModuleStart;
            eventsData.statActiveWidgets++;
        }
    }

    private RemoteViews composeDayCell(Calendar cal, int todayYear, @JdkConstants.CalendarMonth int thisMonth, int today, int appWidgetId) {
        RemoteViews cellRv = null;

        try {

            boolean inMonth = cal.get(Calendar.MONTH) == thisMonth;
            boolean inYear = cal.get(Calendar.YEAR) == todayYear;
            boolean isToday = inYear && inMonth && (cal.get(Calendar.DAY_OF_YEAR) == today);
            @ColorInt Integer color;

            cellRv = new RemoteViews(context.getPackageName(), R.layout.cell_day);
            if (isToday) {
                color = colorToday;
                //todo: сделать различные варианты выделения "сегодня": рамка, подчёркивание
                cellRv.setInt(android.R.id.text1, Constants.METHOD_SET_BACKGROUND_RES, R.drawable.cell_today);
                atLeastOneDayInMonth = true;
            } else if (inMonth) {
                color = colorCommon;
                cellRv.setInt(android.R.id.text1, Constants.METHOD_SET_BACKGROUND_RES, R.drawable.cell_day_this_month);
                atLeastOneDayInMonth = true;
            } else {
                color = colorCommonOutMonth;
                cellRv.setInt(android.R.id.text1, Constants.METHOD_SET_BACKGROUND_RES, R.drawable.cell_day);
            }

            if (enabledFillDays || inMonth) {
                final String dayValue = Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
                if (isToday) {
                    SpannableString spanValue = new SpannableString(dayValue);
                    spanValue.setSpan(new StyleSpan(Typeface.BOLD), 0, dayValue.length() - 1, 0);
                    cellRv.setTextViewText(android.R.id.text1, spanValue);
                } else {
                    cellRv.setTextViewText(android.R.id.text1, dayValue);
                }
            }
            cellRv.setTextViewTextSize(android.R.id.text1, COMPLEX_UNIT_SP, 10 * fontMagnify);

            //Цвет дня
            List<ContactsEvents.DayType> dayTypes = eventsData.getDayTypes(eventsData.sdf_java.format(cal.getTime()), prefOtherEvents);

            boolean isColoredByEvent = false;
            if (!dayTypes.isEmpty()) {
                int maxTypeIndex = -1;
                for (ContactsEvents.DayType dayType : dayTypes) {
                    if (dayType.type != ContactsEvents.DayType.Type.Common) {
                        if (prefOtherEvents.indexOf(dayType.sourceId) > maxTypeIndex) {
                            Integer colorOfDay;
                            if (inMonth) {
                                colorOfDay = eventsColorsInMonth.get(dayType.sourceId);
                            } else {
                                colorOfDay = eventsColorsOutMonth.get(dayType.sourceId);
                            }
                            if (colorOfDay == null || dayType.type == ContactsEvents.DayType.Type.Workday) break;
                            if (Color.alpha(colorOfDay) > 0) { //Цвет дня не полностью прозрачный
                                isColoredByEvent = true;
                                maxTypeIndex = prefOtherEvents.indexOf(dayType.sourceId);
                                color = colorOfDay;
                            }
                        }
                    }
                }
            }
            if (!isColoredByEvent && colorizeSaturdays && cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                if (inMonth) {
                    color = eventsColorsInMonth.get(res.getString(R.string.widget_config_month_events_saturday_id));
                } else {
                    color = eventsColorsOutMonth.get(res.getString(R.string.widget_config_month_events_saturday_id));
                }
            } else if (!isColoredByEvent && colorizeSundays && cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                if (inMonth) {
                    color = eventsColorsInMonth.get(res.getString(R.string.widget_config_month_events_sunday_id));
                } else {
                    color = eventsColorsOutMonth.get(res.getString(R.string.widget_config_month_events_sunday_id));
                }
            }
            if (color != null) {
                if (isToday) {
                    cellRv.setInt(android.R.id.text1, Constants.METHOD_SET_BACKGROUND_COLOR, color);

                    if (Color.red(color) + Color.green(color) + Color.blue(color) > 180 * 3) {
                        cellRv.setTextColor(android.R.id.text1, res.getColor(R.color.black));
                    } else {
                        cellRv.setTextColor(android.R.id.text1, res.getColor(R.color.white));
                    }
                } else {
                    cellRv.setTextColor(android.R.id.text1, color);
                }
            }

            //Реакция на нажатие
            if (enabledFillDays || inMonth) {
                PendingIntent pendingIntent = getOnClickPendingIntent(context, appWidgetId, res, prefOtherEvents, cal, dayTypes);
                if (pendingIntent != null) {
                    cellRv.setOnClickPendingIntent(android.R.id.text1, pendingIntent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return cellRv;
    }

    private PendingIntent getOnClickPendingIntent(Context context, int appWidgetId, Resources res, List<String> prefOtherEvents, Calendar cal, List<ContactsEvents.DayType> dayTypes) {
        PendingIntent pendingIntent = null;

        try {

            int action;
            if (!dayTypes.isEmpty()) {
                action = prefOnClickHolidays;
            } else {
                action = prefOnClickCommon;
            }

            if (action == Constants.onClick_Popup) {
                List<String> dayInfo = eventsData.getDayInfo(eventsData.sdf_java.format(cal.getTime()), prefOtherEvents, eventsColorsInMonth);
                if (!dayInfo.isEmpty()) {
                    Intent intent = new Intent(context, WidgetCalendarPopup.class);
                    intent.putExtra(Constants.EXTRA_DAY_CAPTION,  res.getString(R.string.month_event_popup_prefix)
                            .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(cal.getTime()), ContactsEvents.FormatDate.WithYear)));
                    intent.putExtra(Constants.EXTRA_DAY_INFO, String.join(Constants.HTML_BR, dayInfo));
                    intent.putExtra(Constants.EXTRA_VALUES, Long.toString(cal.getTimeInMillis()));
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    pendingIntent = PendingIntent.getActivity(context, cal.get(Calendar.DAY_OF_YEAR), intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntentImmutable);
                }
            } else if (action == Constants.onClick_Calendar) {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath(Constants.QUERY_PARAM_TIME);
                builder.appendPath(Long.toString(cal.getTimeInMillis()));
                Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return pendingIntent;
    }

}
