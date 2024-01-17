package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

/** @noinspection deprecation*/
public class WidgetCalendar extends AppWidgetProvider {
    private static final String ACTION_PREVIOUS_MONTH = "action.PREVIOUS_MONTH";
    private static final String ACTION_NEXT_MONTH = "action.NEXT_MONTH";
    private static final String ACTION_RESET_MONTH = "action.RESET_MONTH";
    private static final String DAYS_OFFSET = "days_offset";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void redrawWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetCalendar.class));

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();

        if (ACTION_PREVIOUS_MONTH.equals(action)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int monthOffset = sp.getInt(DAYS_OFFSET,0) - 1;
            sp.edit()
                    .putInt(DAYS_OFFSET, monthOffset)
                    .apply();
            redrawWidgets(context);

        } else if (ACTION_NEXT_MONTH.equals(action)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int monthOffset = sp.getInt(DAYS_OFFSET,0) + 1;
            sp.edit()
                    .putInt(DAYS_OFFSET, monthOffset)
                    .apply();
            redrawWidgets(context);

        } else if (ACTION_RESET_MONTH.equals(action)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().remove(DAYS_OFFSET).apply();
            redrawWidgets(context);
        } else {
            redrawWidgets(context);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    @SuppressLint("DiscouragedApi")
    private void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            int columnsMax = 3;
            int columnsToDraw = 3;
            int rowsMax = 4;
            int rowsToDraw = 4;
            int numWeeks = 6;
            boolean isSamsung = Build.BRAND.toLowerCase().contains("samsung");

            Resources res = context.getResources();
            //SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            //Настройки

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            //Количество месяцев
            String prefLayout = res.getString(R.string.widget_config_layout_default);
            try {
                if (widgetPref.size() > 0) prefLayout = widgetPref.get(0);
            } catch (Exception e) {/**/}

            if (prefLayout.equals(res.getString(R.string.widget_config_layout_default))) {

                Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
                if (widgetOptions != null) {
                    int minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                    int minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
                    int minWidthPixel = Dip2Px(res, minWidthDp);
                    int minHeightPixel = Dip2Px(res, minHeightDp);
                    float heightRatio = (float)res.getDisplayMetrics().heightPixels / minWidthPixel;

                    Log.i("SIZES", "minHeightDp=" + minHeightDp
                            + ", screenHeight=" + res.getDisplayMetrics().heightPixels
                            + ", ratioHeight=" + heightRatio
                            + ", minHeightPixel=" + minHeightPixel
                            + ", samsung=" + isSamsung
                    );

                    if (heightRatio > 4.5) {
                        columnsToDraw = 1;
                    } else if (heightRatio > 3) {
                        columnsToDraw = 2;
                    }

                    if (minHeightDp < (isSamsung ? 125 : 60)) { //s: 118, 8.1: 58, 13+12+11: 54
                        rowsToDraw = 1;
                    } else if (minHeightDp < (isSamsung ? 255 : 140)) { //s:249, 8.1: 133, 13+12+11: 125
                        rowsToDraw = 2;
                    } else if (minHeightDp < (isSamsung ? 385 : 210)) { //s:379, 8.1: 207, 13: 195, 12: 196
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
            Log.i("rowsToDraw", "rowsToDraw=" + rowsToDraw);
            Log.i("columnsToDraw", "columnsToDraw=" + columnsToDraw);

            //Стартовый месяц
            int prefMonthsShift = 0;
            try {
                if (widgetPref.size() > 1) prefMonthsShift = Integer.parseInt(widgetPref.get(1));
            } catch (Exception e) {/**/}

            //Положение
            //todo

            //Цвета
            @ColorInt int colorBack = res.getColor(R.color.pref_Widgets_Color_Calendar_Back_default);
                    //Color.argb((int) (255 * 0.5), 0, 0, 0);
            @ColorInt int colorHeaderBack = res.getColor(R.color.pref_Widgets_Color_Calendar_HeaderBack_default);

            @ColorInt int colorCommon = res.getColor(R.color.pref_Widgets_Color_Calendar_Common_default);
            @ColorInt int colorCommonOutMonth = Color.argb((int) (255 * 0.6), Color.red(colorCommon), Color.green(colorCommon), Color.blue(colorCommon));
            @ColorInt int colorMonthTitle = res.getColor(R.color.pref_Widgets_Color_Calendar_Header_default);
            @ColorInt int colorToday = res.getColor(R.color.pref_Widgets_Color_Calendar_Today_default);
            @ColorInt int colorArrows = res.getColor(R.color.pref_Widgets_Color_Calendar_Arrows_default);
            @ColorInt int colorWeeks = res.getColor(R.color.pref_Widgets_Color_Calendar_Weeks_default);
                    //Color.argb((int) (255 * 0.4), Color.red(colorCommon), Color.green(colorCommon), Color.blue(colorCommon));

            @ColorInt int colorSaturday = res.getColor(R.color.dark_yellow);
            @ColorInt int colorSaturdayOutMonth = Color.argb((int) (255 * 0.6), Color.red(colorSaturday), Color.green(colorSaturday), Color.blue(colorSaturday));
            @ColorInt int colorSunday = res.getColor(R.color.dark_red);
            @ColorInt int colorSundayOutMonth = Color.argb((int) (255 * 0.6), Color.red(colorSunday), Color.green(colorSunday), Color.blue(colorSunday));

            int sidePadding = 0;

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

            //int monthOffset = sp.getInt(DAYS_OFFSET,0);
            int today = cal.get(Calendar.DAY_OF_YEAR);
            int todayYear = cal.get(Calendar.YEAR);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);
            //rv.setInt(R.id.calendarAll,"setBackgroundResource", R.color.background_calendar);
  //          rv.setInt(R.id.calendarAll,"setBackgroundColor", R.color.background_calendar);

            for (int row = 1; row <= rowsMax; row++) {
                int id = res.getIdentifier("calendar" + row, "id", context.getPackageName());
                if (row <= rowsToDraw) {
                    rv.setViewVisibility(id, View.VISIBLE);
                    rv.setInt(id,"setBackgroundColor", colorBack);
                    for (int column = 1; column <= columnsMax; column++) {
                        id = res.getIdentifier("calendar" + row + "x" + column, "id", context.getPackageName());
                        int idDiv = res.getIdentifier("calendar" + row + "x" + column + "div", "id", context.getPackageName());
                        if (id != 0) {
                            rv.removeAllViews(id);
                            if (column > columnsToDraw) {
                                rv.setViewVisibility(id, View.GONE);
                                rv.setViewVisibility(idDiv, View.GONE);
                            } else {
                                rv.setViewVisibility(id, View.VISIBLE);
                                rv.setViewVisibility(idDiv, View.VISIBLE);
                                if (column == 1) { //Отступ слева
                                    rv.setViewPadding(id, Dip2Px(res, sidePadding), 0, 0, Dip2Px(res, 4));
                                } else if (column == columnsToDraw) { //Отступ справа
                                    rv.setViewPadding(id, 0, 0, Dip2Px(res, sidePadding), Dip2Px(res, 4));
                                } else {
                                    rv.setViewPadding(id, 0, 0, 0, Dip2Px(res, 4));
                                }

                            }
                        }
                    }
                }
            }

            for (int row = 1; row <= rowsToDraw; row++) {
                for (int column = 1; column <= columnsToDraw; column++) {

                    //rv.removeAllViews(res.getIdentifier("calendar1x" + column, "id", context.getPackageName()));
                    if (column * row > 1) {
                        cal = Calendar.getInstance();
                        cal.setMinimalDaysInFirstWeek(1);
                        cal.add(Calendar.MONTH, ((row - 1) * columnsToDraw) + column - 1);
                    }
                    int thisMonth;
                    cal.add(Calendar.MONTH, prefMonthsShift);
                    thisMonth = cal.get(Calendar.MONTH);
                    cal.set(Calendar.DAY_OF_MONTH, 1);

                    //Шапка
                    RemoteViews calendarRv = new RemoteViews(context.getPackageName(), R.layout.calendar);
                    calendarRv.setInt(R.id.month_bar, "setBackgroundColor", colorHeaderBack);
                    calendarRv.setTextColor(R.id.month_label, colorMonthTitle);
                    calendarRv.setTextColor(R.id.prev_month_button, colorArrows);
                    calendarRv.setTextColor(R.id.next_month_button, colorArrows);
                    calendarRv.setTextViewText(R.id.month_label, DateFormat.format("LLLL yyyy", cal).toString().toUpperCase());
                    calendarRv.setTextViewTextSize(R.id.month_label, COMPLEX_UNIT_SP, 12);
                    if (row == rowsToDraw) {
                        calendarRv.setViewVisibility(R.id.bottom_divider, View.GONE);
                    }
                    //calendarRv.setInt(R.id.calendarMonth,"setBackgroundColor", R.color.background_calendar);

                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    int monthStartDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                    if (cal.getFirstDayOfWeek() == Calendar.SUNDAY) { //вс - в начале
                        cal.add(Calendar.DAY_OF_MONTH, 1 - monthStartDayOfWeek);
                    } else {
                        cal.add(Calendar.DAY_OF_MONTH, 2 - monthStartDayOfWeek);
                    }

                    //Дни недели
                    RemoteViews headerRowRv = new RemoteViews(context.getPackageName(), R.layout.row_weeks);
                    for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                        RemoteViews dayRv = new RemoteViews(context.getPackageName(), R.layout.cell_day);
                        dayRv.setTextColor(android.R.id.text1, colorWeeks);
                        dayRv.setTextViewText(android.R.id.text1, weekdays[day]);
                        dayRv.setTextViewTextSize(android.R.id.text1, COMPLEX_UNIT_SP, 10);
                        headerRowRv.addView(R.id.row_container, dayRv);
                    }
                    calendarRv.addView(R.id.days, headerRowRv);

                    //Дни
                    for (int week = 0; week < numWeeks; week++) {
                        RemoteViews rowRv = new RemoteViews(context.getPackageName(), R.layout.row_week);
                        boolean atLeastOneDayInMonth = false;
                        for (int day = 0; day < 7; day++) {
                            boolean inMonth = cal.get(Calendar.MONTH) == thisMonth;
                            boolean inYear = cal.get(Calendar.YEAR) == todayYear;
                            boolean isToday = inYear && inMonth && (cal.get(Calendar.DAY_OF_YEAR) == today);

                            RemoteViews cellRv = new RemoteViews(context.getPackageName(), R.layout.cell_day);
                            if (isToday) {
                                cellRv.setTextColor(android.R.id.text1, colorToday);
                                cellRv.setInt(android.R.id.text1, "setBackgroundResource", R.drawable.cell_today);
                                atLeastOneDayInMonth = true;
                            } else if (inMonth) {
                                cellRv.setTextColor(android.R.id.text1, colorCommon);
                                cellRv.setInt(android.R.id.text1, "setBackgroundResource", R.drawable.cell_day_this_month);
                                atLeastOneDayInMonth = true;
                            } else {
                                cellRv.setTextColor(android.R.id.text1, colorCommonOutMonth);
                                cellRv.setInt(android.R.id.text1, "setBackgroundResource", R.drawable.cell_day);
                            }

                            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                            builder.appendPath("time");
                            builder.appendPath(Long.toString(cal.getTimeInMillis()));
                            Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            cellRv.setOnClickPendingIntent(android.R.id.text1, PendingIntent.getActivity(context, 0, intent,
                                    PendingIntentImmutable));

                            cellRv.setTextViewText(android.R.id.text1, Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
                            cellRv.setTextViewTextSize(android.R.id.text1, COMPLEX_UNIT_SP, 10);
                            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                                if (inMonth) {
                                    cellRv.setTextColor(android.R.id.text1, colorSaturday);
                                } else {
                                    cellRv.setTextColor(android.R.id.text1, colorSaturdayOutMonth);
                                }
                            } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                if (inMonth) {
                                    cellRv.setTextColor(android.R.id.text1, colorSunday);
                                } else {
                                    cellRv.setTextColor(android.R.id.text1, colorSundayOutMonth);
                                }
                            }
                            rowRv.addView(R.id.row_container, cellRv);
                            cal.add(Calendar.DAY_OF_MONTH, 1);
                        }
                        if (week < numWeeks - 1 || atLeastOneDayInMonth) { //Если не последняя неделя или есть хоть 1 день в месяце
                            calendarRv.addView(R.id.days, rowRv);
                        }
                    }

                    if (row > 1 || (column != 1 && columnsToDraw > 1)) {
                        calendarRv.setViewVisibility(R.id.prev_month_button, View.GONE);
                    } else {
                        calendarRv.setViewVisibility(R.id.prev_month_button, View.VISIBLE);
                        calendarRv.setTextViewText(R.id.prev_month_button, res.getText(R.string.previous_month_arrow));
                        calendarRv.setTextViewTextSize(R.id.prev_month_button, COMPLEX_UNIT_SP, 12);
                        calendarRv.setOnClickPendingIntent(R.id.prev_month_button, PendingIntent.getBroadcast(context, 0,
                                new Intent(context, WidgetCalendar.class).setAction(ACTION_PREVIOUS_MONTH), PendingIntentImmutable));
                    }

                    if (row > 1 || (column < columnsToDraw && columnsToDraw > 1)) {
                        calendarRv.setViewVisibility(R.id.next_month_button, View.GONE);
                    } else {
                        calendarRv.setViewVisibility(R.id.next_month_button, View.VISIBLE);
                        calendarRv.setTextViewText(R.id.next_month_button, res.getText(R.string.next_month_arrow));
                        calendarRv.setTextViewTextSize(R.id.next_month_button, COMPLEX_UNIT_SP, 12);
                        calendarRv.setOnClickPendingIntent(R.id.next_month_button, PendingIntent.getBroadcast(context, 0,
                                new Intent(context, WidgetCalendar.class).setAction(ACTION_NEXT_MONTH), PendingIntentImmutable));
                    }

                    calendarRv.setOnClickPendingIntent(R.id.month_label, PendingIntent.getBroadcast(context, 0,
                            new Intent(context, WidgetCalendar.class).setAction(ACTION_RESET_MONTH), PendingIntentImmutable));
                    //calendarRv.setViewVisibility(R.id.month_bar, numWeeks == 2 ? View.GONE : View.VISIBLE);

                    int id = res.getIdentifier("calendar1x" + column, "id", context.getPackageName());
                    if (id != 0) {
                        rv.addView(id, calendarRv);
                    }

                }
            }

            /*ToastExpander.showDebugMsg(context, Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    widgetType + Constants.STRING_COLON_SPACE + appWidgetId +
                            ", layout=" + context.getResources().getResourceEntryName(views.getLayoutId()) +
                            "\n widgetPref=" + widgetPref
                    : widgetType + Constants.STRING_COLON + appWidgetId + Constants.STRING_EOL + widgetPref
            );*/

            appWidgetManager.updateAppWidget(appWidgetId, rv);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int Dip2Px(Resources res, int sizeDP) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeDP, res.getDisplayMetrics()));
    }

}
