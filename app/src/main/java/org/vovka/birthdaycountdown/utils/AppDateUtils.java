/*
 * *
 *  * Created by Vladimir Belov on 31.07.2026, 00:26
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 29.07.2026, 19:24
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.vovka.birthdaycountdown.Constants;
import org.vovka.birthdaycountdown.ContactsEvents;
import org.vovka.birthdaycountdown.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AppDateUtils {
    static final String TAG = "DateUtils";
    private static Calendar cacheCalendar1 = null;
    private static Calendar cacheCalendar2 = null;

    public static int countLeapYearsBetween(int y1, int y2) {

        int yearStart;
        int yearEnd;

        // ensure y1 <= y2
        if (y1 > y2) {
            yearStart = y2;
            yearEnd = y1;
        } else {
            yearStart = y1;
            yearEnd = y2;
        }

        int diff;

        int firstDivisibleBy4 = yearStart;
        if (firstDivisibleBy4 % 4 != 0) {
            firstDivisibleBy4 += 4 - (yearStart % 4);
        }
        diff = yearEnd - firstDivisibleBy4 - 1;
        int divisibleBy4 = diff < 0 ? 0 : diff / 4 + 1;

        int firstDivisibleBy100 = yearStart;
        if (firstDivisibleBy100 % 100 != 0) {
            firstDivisibleBy100 += 100 - (firstDivisibleBy100 % 100);
        }
        diff = yearEnd - firstDivisibleBy100 - 1;
        int divisibleBy100 = diff < 0 ? 0 : diff / 100 + 1;

        int firstDivisibleBy400 = yearStart;
        if (firstDivisibleBy400 % 400 != 0) {
            firstDivisibleBy400 += 400 - (yearStart % 400);
        }
        diff = yearEnd - firstDivisibleBy400 - 1;
        int divisibleBy400 = diff < 0 ? 0 : diff / 400 + 1;

        return divisibleBy4 - divisibleBy100 + divisibleBy400;
    }

    /**
     * Возвращает календарь из даты
     *
     * @param date Дата
     * @return Calendar
     */
    public static Calendar getCalendarFromDate(@NonNull Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c;
    }

    /**
     * Возвращает дату с нулевым временем
     *
     * @param c Дата
     * @return Дата с нулевым временем
     */
    public static Calendar getWithoutTime(@NonNull Calendar c) {
        clearTime(c);
        return c;
    }

    public static boolean isLeapYear(int year) {
        return year % 400 == 0 || (year % 100 != 0 && (year % 4 == 0));
    }

    /**
     * Проверяет, что две даты относятся к одному дню
     *
     * @param cal1 Первая дата
     * @param cal2 Вторая дата
     * @return Результат проверки
     */
    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            return false;
        }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /** Возвращает количество дней между датами
     * @param dateFrom Начальная дата
     * @param dateTo Конечная дата
     * @return Количество дней
     */
    public static long countDaysDiff(@NonNull Date dateFrom, @NonNull Date dateTo) {
        //https://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances/43681941#43681941

        try {

            if (cacheCalendar1 == null) cacheCalendar1 = Calendar.getInstance();
            if (cacheCalendar2 == null) cacheCalendar2 = Calendar.getInstance();

            cacheCalendar1.setTime(dateFrom);
            cacheCalendar2.setTime(dateTo);

            Calendar c1;
            Calendar c2;
            int distanceSign = 1;
            if (dateFrom.before(dateTo)) {
                c1 = getWithoutTime(cacheCalendar1);
                c2 = getWithoutTime(cacheCalendar2);
            } else {
                c1 = getWithoutTime(cacheCalendar2);
                c2 = getWithoutTime(cacheCalendar1);
                distanceSign = -1;
            }

            if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) {
                return distanceSign * (c2.get(Calendar.DAY_OF_YEAR) - c1.get(Calendar.DAY_OF_YEAR));
            } else {
                int y1 = c1.get(Calendar.YEAR);
                int y2 = c2.get(Calendar.YEAR);
                int d1 = c1.get(Calendar.DAY_OF_YEAR);
                int d2 = c2.get(Calendar.DAY_OF_YEAR);

                int minorYearSign = c1.get(Calendar.ERA) == GregorianCalendar.AD ? 1 : -1;
                int resD = d2 + ((y2 - minorYearSign * y1) * 365) - d1;
                return distanceSign * (resD + countLeapYearsBetween(minorYearSign * y1, y2));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return 0;
        }
    }

    public static Date addYear(@NonNull Date date, int year) {
        try {
            Calendar c = getCalendarFromDate(date);
            c.add(Calendar.YEAR, year);
            return c.getTime();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return date;
        }
    }

    public static int countYearsDiff(@NonNull Date date1, @NonNull Date date2) {
        try {

            Calendar c1;
            Calendar c2;

            if (date2.after(date1)) {
                c1 = getWithoutTime(getCalendarFromDate(date1));
                c2 = getWithoutTime(getCalendarFromDate(date2));
            } else {
                c1 = getWithoutTime(getCalendarFromDate(date2));
                c2 = getWithoutTime(getCalendarFromDate(date1));
            }

            int subst = 0;
            int minorYearSign = c1.get(Calendar.ERA) == GregorianCalendar.AD ? 1 : -1;

            if (c1.get(Calendar.MONTH) > c2.get(Calendar.MONTH)) {
                subst = 1;
            } else if (c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)) {
                if (c1.get(Calendar.DATE) > c2.get(Calendar.DATE)) {
                    subst = 1;
                }
            }
            return Math.max(c2.get(Calendar.YEAR) - minorYearSign * c1.get(Calendar.YEAR) - subst, 0);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return 0;
        }
    }

    /** Обнуляет поля времени для Calendar
     * @param c Дата
     */
    public static void clearTime(@NonNull Calendar c) {

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

    }

    @Nullable
    public static Calendar getEasterDateFor(float Y, boolean getOrthodox) {
        //https://www.geeksforgeeks.org/how-to-calculate-the-easter-date-for-a-given-year-using-gauss-algorithm/
        //https://ru.wikipedia.org/wiki/Алгоритм_Гаусса_вычисления_даты_Пасхи

        try {

            float A, B, C, P, Q, M, N, D, E;
            Calendar cal = Calendar.getInstance();

            // All calculations done on the basis of Gauss Easter Algorithm
            A = Y % 19;
            B = Y % 4;
            C = Y % 7;
            P = (float) Math.floor(Y / 100);
            Q = (float) Math.floor((13 + 8 * P) / 25);
            if (getOrthodox) {
                M = 15;
                N = 6;
            } else {
                M = (int) (15 - Q + P - Math.floor(P / 4)) % 30;
                N = (int) (4 + P - Math.floor(P / 4)) % 7;
            }
            D = (19 * A + M) % 30;
            E = (2 * B + 4 * C + 6 * D + N) % 7;
            int days = (int) (22 + D + E);

            if (!getOrthodox && D == 29 && E == 6) { //A corner case, when D is 29
                cal.set((int) Y, 3, 19);
                return cal;
            } else if (!getOrthodox && D == 28 && E == 6) { //Another corner case, when D is 28
                cal.set((int) Y, 3, 18);
                return cal;
            } else {
                if (days > 31) { //If days > 31, move to April
                    cal.set((int) Y, 3, days - 31);
                } else { // Otherwise, stay on March
                    cal.set((int) Y, 2, days);
                }
                if (getOrthodox) {
                    cal.add(Calendar.DAY_OF_YEAR, 13);
                }
                return cal;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    public static void fillCalendarList(Context context, HashMap<String, String> mapCalendars, HashMap<String, Integer> mapCalendarsColors) {

        Cursor cursor = null;

        try {

            if (DeviceTools.checkNoCalendarAccess(context)) return;

            ContentResolver contentResolver = context.getContentResolver();
            ContactsEvents.ColumnIndexCache cache = new ContactsEvents.ColumnIndexCache();
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            cursor = contentResolver.query(
                    uri,
                    new String[]{
                            android.provider.BaseColumns._ID,
                            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                            CalendarContract.Calendars.ACCOUNT_NAME,
                            CalendarContract.Calendars.CALENDAR_COLOR,
                            CalendarContract.Calendars.VISIBLE,
                            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
                    },
                    null,
                    null,
                    null);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        String calId = cursor.getString(cache.getColumnIndex(cursor, android.provider.BaseColumns._ID));
                        int columnAccessLevelIndex = cache.getColumnIndex(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL);
                        String isReadOnly = Constants.STRING_EMPTY;
                        if (columnAccessLevelIndex > 0) {
                            isReadOnly = cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)) < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR ? Constants.STRING_1 : Constants.STRING_0;
                        }
                        mapCalendars.put(calId, cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                                .concat(Constants.STRING_EOT)
                                .concat(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Calendars.ACCOUNT_NAME)))
                                .concat(Constants.STRING_EOT)
                                .concat(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Calendars.VISIBLE)))
                                .concat(Constants.STRING_EOT)
                                .concat(isReadOnly)
                        );
                        String calendarId = StringUtils.getHash(Constants.eventSourceCalendarPrefix.concat(calId));
                        mapCalendarsColors.put(calendarId, cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Calendars.CALENDAR_COLOR)));
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }

        } catch (SecurityException se) {
            if (cursor != null && !cursor.isClosed()) cursor.close();
        } catch (Exception e) {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Пытается распарсить дату, перебирая форматы по очереди.
     * @return Распарсенная дата или null, если ни один формат не подошел
     */
    public static Date parseDateWithFormats(String dateStr, SimpleDateFormat... formats) {
        for (SimpleDateFormat format : formats) {
            try {
                return format.parse(dateStr);
            } catch (ParseException ignored) {
                // Пробуем следующий формат
            }
        }
        return null;
    }

    /** Возвращает количество дней между датами в виде форматированного текста
     * @param dateFrom   Начальная дата
     * @param dateTo     Конечная дата
     * @param components 1 - only DMY, 2 - only days count, 3 - "DMY (days count)"
     * @param res Ресурсы контекста
     * @param locale Локаль
     * @return Строка с количеством дней между датами
     */
    public static String countDaysDiffText(@NonNull Date dateFrom, @NonNull Date dateTo, int components, @NonNull Resources res, @NonNull String locale) {

        try {

            StringBuilder eventDistance = new StringBuilder();
            long daysDiff;
            boolean diffOnlyDays = true;

            //если включить desugaring https://www.youtube.com/watch?v=heCvGfOGH0s, то размер приложения +200К
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                //https://stackoverflow.com/questions/4710206/calculate-age-in-years-months-days-hours-minutes-and-seconds
                LocalDate dateStart = dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate dateEnd = dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                if (dateEnd.isBefore(dateStart)) return Constants.STRING_EMPTY;

                daysDiff = ChronoUnit.DAYS.between(dateStart, dateEnd);

                if (components == 1 || components == 3) {

                    Period p = Period.between(dateStart, dateEnd);

                    if (p.getYears() > 0) {
                        eventDistance.append(StringUtils.getAgeString(p.getYears(), R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20, locale, res)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    if (p.getMonths() > 0) {
                        eventDistance.append(StringUtils.getAgeString(p.getMonths(), R.string.msg_after_month_prefix_1, R.string.msg_after_month_prefix_1_, R.string.msg_after_month_prefix_2_3_4, R.string.msg_after_month_prefix_5_20, locale, res)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    if (p.getDays() > 0) {
                        eventDistance.append(StringUtils.getAgeString(p.getDays(), R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, locale, res)).append(Constants.STRING_SPACE);
                    }
                }

            } else {

                Calendar calendarDateFrom;
                Calendar calendarDateTo;

                if (dateTo.before(dateFrom)) return Constants.STRING_EMPTY;

                calendarDateFrom = getWithoutTime(getCalendarFromDate(dateFrom));
                calendarDateTo = getWithoutTime(getCalendarFromDate(dateTo));

                int yearFrom = calendarDateFrom.get(Calendar.YEAR);
                int yearTo = calendarDateTo.get(Calendar.YEAR);
                int daysFromNYFrom = calendarDateFrom.get(Calendar.DAY_OF_YEAR);
                int daysFromNYTo = calendarDateTo.get(Calendar.DAY_OF_YEAR);

                if (yearFrom == yearTo) {
                    daysDiff = daysFromNYTo - daysFromNYFrom;
                } else {
                    int resD = daysFromNYTo + ((yearTo - yearFrom) * 365) - daysFromNYFrom;
                    daysDiff = resD + countLeapYearsBetween(yearFrom, yearTo);
                }

                if (components == 1 || components == 3) {

                    long delta = yearTo - yearFrom - (daysFromNYTo < daysFromNYFrom ? 1 : 0);
                    if (delta > 0) {
                        eventDistance.append(StringUtils.getAgeString(delta, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20, locale, res)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    final int dayOfMonthTo = calendarDateTo.get(Calendar.DAY_OF_MONTH);
                    final int dayOfMonthFrom = calendarDateFrom.get(Calendar.DAY_OF_MONTH);
                    if (daysFromNYFrom > daysFromNYTo) {
                        delta = 12 - calendarDateFrom.get(Calendar.MONTH) + calendarDateTo.get(Calendar.MONTH) - (dayOfMonthFrom > dayOfMonthTo ? 1 : 0);
                    } else {
                        delta = calendarDateTo.get(Calendar.MONTH) - calendarDateFrom.get(Calendar.MONTH);
                    }
                    if (delta > 0) {
                        eventDistance.append(StringUtils.getAgeString(delta, R.string.msg_after_month_prefix_1, R.string.msg_after_month_prefix_1_, R.string.msg_after_month_prefix_2_3_4, R.string.msg_after_month_prefix_5_20, locale, res)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }

                    if (dayOfMonthTo >= dayOfMonthFrom) {
                        delta = dayOfMonthTo - dayOfMonthFrom;
                    } else {
                        Calendar calendarMonthFrom = Calendar.getInstance();
                        calendarMonthFrom.set(Calendar.YEAR, yearFrom);
                        calendarMonthFrom.set(Calendar.MONTH, calendarDateFrom.get(Calendar.MONTH) - 1);
                        int numDays = calendarMonthFrom.getActualMaximum(Calendar.DATE);
                        delta = numDays - dayOfMonthFrom + dayOfMonthTo - 1;
                    }
                    if (delta > 0) {
                        eventDistance.append(StringUtils.getAgeString(delta, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, locale, res)).append(Constants.STRING_SPACE);
                    }
                }
            }

            //(X days)
            if (!diffOnlyDays || components == 2) {
                if (components == 3) {
                    eventDistance.append(Constants.STRING_PARENTHESIS_START);
                }
                if (components == 2 || components == 3) {
                    eventDistance.append(StringUtils.getAgeString(daysDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, locale, res));
                }
                if (components == 3) {
                    eventDistance.append(Constants.STRING_PARENTHESIS_CLOSE);
                }
            }

            return eventDistance.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return Constants.STRING_EMPTY;
        }
    }

    /** Возвращает количество предыдущих дней для сканирования событий
     * @param params Параметры отображения событий до "сегодня" (в виде XdYe, за X дней, не больше Y событий)
     * @return Количество дней
     */
    public static int getPreviousDaysScanDays(@NonNull String params) {
        int result = 0;
        try {

            switch (params) {
                case "":
                    break;
                case "1d":
                    result = 1;
                    break;
                case "2d":
                    result = 2;
                    break;
                case "3d":
                case "3d1e":
                case "3d2e":
                    result = 3;
                    break;
                default:
                    result = Constants.PREV_EVENTS_MAX_DAYS;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return result;
    }

    /** Возвращает отформатированную дату
     * @param dateIn     Дата строкой DDMMYYY
     * @param dateFormat Формат даты (с годом или без)
     * @param preferencesDateFormat Формат отображения даты в приложении
     * @param context Контекст
     * @param res Ресурсы контекста
     * @param lang Язык
     * @return Отформатированная дата, согласно указанному формату и настройки формата даты
     */
    @NonNull
    public static String getDateFormatted(String dateIn, ContactsEvents.FormatDate dateFormat, int preferencesDateFormat,
                                          @NonNull Context context, @NonNull Resources res, String lang) {

        String resultString = Constants.STRING_EMPTY;
        if (TextUtils.isEmpty(dateIn)) return resultString;
        if (preferencesDateFormat == 2 && dateFormat == ContactsEvents.FormatDate.WithYear)
            return dateIn; // DD.MM.YYYY

        String postfixBC = res.getString(R.string.msg_after_year_bc);
        if (!dateIn.endsWith(postfixBC)) postfixBC = Constants.STRING_EMPTY;

        try {

            final Locale locale = Locale.forLanguageTag(lang);
            //todo: переместить в поля класса + initLocaleStrings()
            SimpleDateFormat sdfInY = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale);
            SimpleDateFormat sdfIn = new SimpleDateFormat(Constants.DATE_DD_MM, locale);
            SimpleDateFormat sdfOut = null;
            Date eventDate = null;
            boolean isYearPresent = false;

            switch (preferencesDateFormat) {

                case 2: // DD.MM.YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_DD_MM, locale);
                    try {
                        eventDate = sdfInY.parse(dateIn);
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null)
                        resultString = sdfOut.format(eventDate).concat(postfixBC);
                    break;

                case 3: // MM.DD.YYYY

                    try {
                        eventDate = sdfInY.parse(dateIn);
                        isYearPresent = true;
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) {
                        if (dateFormat == ContactsEvents.FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_MM_DD_YYYY, locale);
                        } else if (!isYearPresent || dateFormat == ContactsEvents.FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_MM_DD, locale);
                        }
                        if (sdfOut != null)
                            resultString = sdfOut.format(eventDate).concat(postfixBC);
                    }
                    break;

                case 4: // DD/MM/YYYY

                    try {
                        eventDate = sdfInY.parse(dateIn);
                        isYearPresent = true;
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) {
                        if (dateFormat == ContactsEvents.FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_UK, locale);
                        } else if (!isYearPresent || dateFormat == ContactsEvents.FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_UK_NO_YEAR, locale);
                        }
                        if (sdfOut != null)
                            resultString = sdfOut.format(eventDate).concat(postfixBC);
                    }
                    break;

                case 5: // MM/DD/YYYY

                    try {
                        eventDate = sdfInY.parse(dateIn);
                        isYearPresent = true;
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) {
                        if (dateFormat == ContactsEvents.FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_IND, locale);
                        } else if (!isYearPresent || dateFormat == ContactsEvents.FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_IND_NO_YEAR, locale);
                        }
                        if (sdfOut != null)
                            resultString = sdfOut.format(eventDate).concat(postfixBC);
                    }
                    break;

                case 6: // DD MMM YYYY

                    try {
                        eventDate = sdfInY.parse(dateIn);
                        isYearPresent = true;
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) {
                        if (dateFormat == ContactsEvents.FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_DD_MMM_YYYY, locale);
                        } else if (!isYearPresent || dateFormat == ContactsEvents.FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_DD_MMM, locale);
                        }
                        if (sdfOut != null)
                            resultString = sdfOut.format(eventDate).concat(postfixBC);
                    }
                    break;

                case 7: // D MMMM YYYY

                    try {
                        eventDate = sdfInY.parse(dateIn);
                        isYearPresent = true;
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) {
                        if (dateFormat == ContactsEvents.FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_D_MMMM_YYYY, locale);
                        } else if (!isYearPresent || dateFormat == ContactsEvents.FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_D_MMMM, locale);
                        }
                        if (sdfOut != null)
                            resultString = sdfOut.format(eventDate).concat(postfixBC);
                    }
                    break;

                default:

                    //https://stackoverflow.com/questions/3790918/format-date-without-year
                    try {
                        eventDate = sdfInY.parse(dateIn);
                        isYearPresent = true;
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) {
                        if (dateFormat == ContactsEvents.FormatDate.WithYear && isYearPresent) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        } else { //if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        }
                    }
            }

        } catch (Exception e) { /**/ }

        return TextUtils.isEmpty(resultString) ? resultString : resultString.concat(postfixBC);
    }

    /** Возвращает отформатированную дату и время
     * @param dateIn     Дата строкой DDMMYYY
     * @param preferencesDateFormat Формат отображения даты в приложении
     * @param context Контекст
     * @param lang Язык
     * @return Отформатированная дата, согласно указанному формату и настройки формата даты
     */
    @NonNull
    public static String getDateTimePreferable(@NonNull Date dateIn, int preferencesDateFormat, Context context, String lang) {

        String resultString = Constants.STRING_EMPTY;

        try {

            final Locale locale = Locale.forLanguageTag(lang);
            SimpleDateFormat sdfOut;
            final String timeFormat = " HH:mm";

            switch (preferencesDateFormat) {

                case 2: // DD.MM.YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_DD_MM + timeFormat, locale);
                    resultString = sdfOut.format(dateIn);
                    break;

                case 3: // MM.DD.YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_MM_DD_YYYY + timeFormat, locale);
                    resultString = sdfOut.format(dateIn);
                    break;

                case 4: // DD/MM/YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_UK + timeFormat, locale);
                    resultString = sdfOut.format(dateIn);
                    break;

                case 5: // MM/DD/YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_IND + timeFormat, locale);
                    resultString = sdfOut.format(dateIn);
                    break;

                case 6: // DD MMM YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_DD_MMM_YYYY + timeFormat, locale);
                    resultString = sdfOut.format(dateIn);
                    break;

                case 7: // D MMMM YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_D_MMMM_YYYY + timeFormat, locale);
                    resultString = sdfOut.format(dateIn);
                    break;

                default:

                    resultString = DateUtils.formatDateTime(context, dateIn.getTime(),
                            DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);

            }

        } catch (Exception e) { /**/ }

        return resultString;
    }

    /**
     * Возвращает следующую дату плавающего события
     *
     * @param eventDateString Изначальная дата в формате DD.MM.YYYY
     * @param yearShift       Сколько лет прибавить или отнять
     * @param weekDaysShort Массив коротких имён дней недели
     * @return Дата в формате DD.MM.YYYY
     */
    @NonNull
    public static String computeFloatingDate(String eventDateString, int yearShift, String[] weekDaysShort) {

        try {

            String[] eventDateComponents = eventDateString.split(Constants.REGEX_PERIOD, -1);
            final String eventDayString = eventDateComponents[0].toLowerCase();
            Calendar dateRubicon = Calendar.getInstance(); //От какой даты считаем "сегодня"
            if (yearShift != 0) {
                dateRubicon.add(Calendar.YEAR, yearShift);
            }
            Calendar cal;
            int eventMonth;
            int eventYear = dateRubicon.get(Calendar.YEAR);

            //Именные события
            if (eventDateComponents.length == 2) {
                if (EventAliases.startsWithAlias(eventDayString, EventAliases.CANONICAL_EASTER)) {

                    //Православная Пасха

                    //Определяем смещение в днях
                    int daysShift = 0;
                    final String strAfterEventName = eventDayString.substring(EventAliases.getMatchedAlias(eventDayString,
                            EventAliases.CANONICAL_EASTER).length());
                    if (strAfterEventName.startsWith(Constants.STRING_PLUS)) {
                        try {
                            daysShift = Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_PLUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    } else if (strAfterEventName.startsWith(Constants.STRING_MINUS)) {
                        try {
                            daysShift = -Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_MINUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    }

                    cal = getEasterDateFor(eventYear, true);
                    if (cal != null) {
                        cal.add(Calendar.DAY_OF_YEAR, daysShift);
                        if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                            cal = getEasterDateFor(eventYear + 1, true);
                            if (cal != null) {
                                cal.add(Calendar.DAY_OF_YEAR, daysShift);
                                return Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());
                            }
                        } else {
                            return Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());
                        }
                    }

                } else if (EventAliases.startsWithAlias(eventDayString, EventAliases.CANONICAL_CATHOLIC_EASTER)) {

                    //Католическая Пасха

                    //Определяем смещение в днях
                    int daysShift = 0;
                    final String strAfterEventName = eventDayString.substring(EventAliases.getMatchedAlias(eventDayString,
                            EventAliases.CANONICAL_CATHOLIC_EASTER).length());
                    if (strAfterEventName.startsWith(Constants.STRING_PLUS)) {
                        try {
                            daysShift = Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_PLUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    } else if (strAfterEventName.startsWith(Constants.STRING_MINUS)) {
                        try {
                            daysShift = -Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_MINUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    }

                    cal = getEasterDateFor(eventYear, false);
                    if (cal != null) {
                        cal.add(Calendar.DAY_OF_YEAR, daysShift);
                        if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                            cal = getEasterDateFor(eventYear + 1, false);
                            if (cal != null) {
                                cal.add(Calendar.DAY_OF_YEAR, daysShift);
                                return Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());
                            }
                        } else {
                            return Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());
                        }
                    }

                } else if (EventAliases.startsWithAlias(eventDayString, EventAliases.CANONICAL_NY)) {

                    //XX день от начала года

                    //Определяем смещение в днях
                    int daysShift = 0;
                    final String strAfterEventName = eventDayString.substring(EventAliases.getMatchedAlias(eventDayString, EventAliases.CANONICAL_NY).length());
                    if (strAfterEventName.startsWith(Constants.STRING_PLUS)) {
                        try {
                            daysShift = Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_PLUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    } else if (strAfterEventName.startsWith(Constants.STRING_MINUS)) {
                        try {
                            daysShift = -Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_MINUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    }

                    cal = (Calendar) dateRubicon.clone();
                    cal.set(eventYear, Calendar.JANUARY, 1);
                    cal.add(Calendar.DAY_OF_YEAR, daysShift);

                    if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                        cal.set(eventYear + 1, Calendar.JANUARY, 1);
                        cal.add(Calendar.DAY_OF_YEAR, daysShift);
                    }

                    return Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());
                }
            }

            //NWW[+-OFFSET].ММ.ГГГГ
            if (eventDateComponents.length < 3) return eventDateString;

            try {
                eventMonth = Integer.parseInt(eventDateComponents[1]);
                if (eventMonth < 1 || eventMonth > 12) {
                    return eventDateString;
                }
            } catch (NumberFormatException ignored) {
                return eventDateString;
            }

            //Определяем день недели
            int weekDayToGet = 0;
            int indexWeekDay = -1;
            int countWeekdays = weekDaysShort.length;
            for (int i = 1; i <= countWeekdays; i++) {
                String weekDayName = weekDaysShort[i - 1].toLowerCase();
                if (eventDayString.contains(weekDayName)) {
                    weekDayToGet = i - 1;
                    if (weekDayToGet == 0) weekDayToGet = 7;
                    indexWeekDay = eventDayString.indexOf(weekDayName);
                    break;
                }
            }
            if (weekDayToGet == 0) {
                //Обычная ДД.ММ.ГГГГ дата
                return eventDateString;
            }

            //Определяем смещение в днях
            int daysShift = 0;
            String strAfterWeekName = eventDayString.substring(indexWeekDay + 2);
            if (strAfterWeekName.startsWith(Constants.STRING_PLUS)) {
                try {
                    daysShift = Integer.parseInt(strAfterWeekName.substring(strAfterWeekName.indexOf(Constants.STRING_PLUS) + 1));
                } catch (NumberFormatException ignored) { /**/ }
            } else if (strAfterWeekName.startsWith(Constants.STRING_MINUS)) {
                try {
                    daysShift = -Integer.parseInt(strAfterWeekName.substring(strAfterWeekName.indexOf(Constants.STRING_MINUS) + 1));
                } catch (NumberFormatException ignored) { /**/ }
            }

            String weekDayNumberString = eventDayString.substring(0, indexWeekDay);

            if (weekDayNumberString.equalsIgnoreCase(Constants.STRING_Z)) { //Последняя неделя

                cal = (Calendar) dateRubicon.clone();
                cal.set(eventYear, eventMonth, 1);
                int weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                int daysToSub = weekDayStartOfNextMonth > weekDayToGet ? weekDayStartOfNextMonth - weekDayToGet : 7 - (weekDayToGet - weekDayStartOfNextMonth);
                cal.add(Calendar.DAY_OF_MONTH, -daysToSub + daysShift);

                if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                    cal.set(eventYear + 1, eventMonth, 1);
                    weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                    daysToSub = weekDayStartOfNextMonth > weekDayToGet ? weekDayStartOfNextMonth - weekDayToGet : 7 - (weekDayToGet - weekDayStartOfNextMonth);
                    cal.add(Calendar.DAY_OF_MONTH, -daysToSub + daysShift);
                }

            } else if (weekDayNumberString.equalsIgnoreCase(Constants.STRING_Y)) { //Предпоследняя неделя

                cal = (Calendar) dateRubicon.clone();
                cal.set(eventYear, eventMonth, 1);
                int weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                int daysToSub = weekDayStartOfNextMonth > weekDayToGet ? 7 + weekDayStartOfNextMonth - weekDayToGet : 14 - (weekDayToGet - weekDayStartOfNextMonth);
                cal.add(Calendar.DAY_OF_MONTH, -daysToSub + daysShift);

                if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                    cal.set(eventYear + 1, eventMonth, 1);
                    weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                    daysToSub = weekDayStartOfNextMonth > weekDayToGet ? 7 + weekDayStartOfNextMonth - weekDayToGet : 14 - (weekDayToGet - weekDayStartOfNextMonth);
                    cal.add(Calendar.DAY_OF_MONTH, -daysToSub + daysShift);
                }

            } else {

                int weekNumberToGet;
                try {
                    weekNumberToGet = Integer.parseInt(weekDayNumberString); //Номер недели 1..5
                    if (weekNumberToGet < 1 || weekNumberToGet > 5) {
                        return eventDateString;
                    }
                } catch (NumberFormatException ignored) {
                    return eventDateString;
                }
                cal = (Calendar) dateRubicon.clone();
                cal.set(eventYear, eventMonth - 1, 1);
                int weekDayStartOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDayStartOfMonth == 0) weekDayStartOfMonth = 7;
                int daysToAdd = weekDayStartOfMonth <= weekDayToGet ? weekDayToGet - weekDayStartOfMonth + 7 * (weekNumberToGet - 1) : 7 - (weekDayStartOfMonth - weekDayToGet) + 7 * (weekNumberToGet - 1);
                cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
                if (weekNumberToGet == 5 && (cal.get(Calendar.MONTH)) != eventMonth - 1)
                    cal.add(Calendar.DAY_OF_MONTH, -7);
                cal.add(Calendar.DAY_OF_MONTH, daysShift);

                if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                    cal.set(eventYear + 1, eventMonth - 1, 1);
                    weekDayStartOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    if (weekDayStartOfMonth == 0) weekDayStartOfMonth = 7;
                    daysToAdd = weekDayStartOfMonth <= weekDayToGet ? weekDayToGet - weekDayStartOfMonth + 7 * (weekNumberToGet - 1) : 7 - (weekDayStartOfMonth - weekDayToGet) + 7 * (weekNumberToGet - 1);
                    cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
                    if (weekNumberToGet == 5 && (cal.get(Calendar.MONTH)) != eventMonth - 1)
                        cal.add(Calendar.DAY_OF_MONTH, -7);
                    cal.add(Calendar.DAY_OF_MONTH, daysShift);
                }

            }

            return Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return eventDateString;
        }
    }

    /** Разделить события на несколько дней на отдельные дни
     * @param eventsArray Массив с событиями (даты начала и конца события идут через "-", если события без года, то вместо года стоит 0000)
     * 01.06.2025-06.06.2025 Событие 1
     * 01.06.0000-06.06.0000,! Праздник на несколько дней
     * @param dateFormats Массив форматов дат
     * @param today Сегодня
     */
    public static List<String> splitMultidayEventsAsSeparateLine(String[] eventsArray, SimpleDateFormat[] dateFormats, Calendar today) {
        try {
        List<String> result = new ArrayList<>(eventsArray.length);
        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        final String year = String.valueOf(today.get(Calendar.YEAR));

        for (String line : eventsArray) {
            int indexMinus = line.indexOf(Constants.STRING_MINUS);
            if (indexMinus == -1) {
                result.add(line);
                continue;
            }

            int indexSpace = line.indexOf(Constants.STRING_SPACE);
            int indexComma = line.indexOf(Constants.STRING_COMMA);

            if (indexSpace == -1 && indexComma == -1) {
                result.add(line);
                continue;
            }

            int indexEndDate = (indexSpace != -1 && indexComma != -1)
                    ? Math.min(indexSpace, indexComma)
                    : (indexSpace != -1 ? indexSpace : indexComma);

            String dates = line.substring(0, indexEndDate);
            int rangeMinus = dates.indexOf(Constants.STRING_MINUS);
            if (rangeMinus == -1) {
                result.add(line);
                continue;
            }

            String strDateStart = dates.substring(0, rangeMinus);
            String strDateEnd = dates.substring(rangeMinus + 1);
            int indexNoYear = strDateStart.indexOf(Constants.STRING_0000);
            if (indexNoYear != -1) {
                strDateStart = strDateStart.replace(Constants.STRING_0000, year);
                strDateEnd = strDateEnd.replace(Constants.STRING_0000, year);
            }

            Date dateStart = parseDateWithFormats(strDateStart, dateFormats);
            Date dateEnd = parseDateWithFormats(strDateEnd, dateFormats);

            if (dateStart == null || dateEnd == null || dateStart.after(dateEnd)) {
                result.add(line);
                continue;
            }

            calStart.setTime(dateStart);
            calEnd.setTime(dateEnd);

            String eventBody = line.substring(indexEndDate);
            StringBuilder sb = new StringBuilder(64);

            while (!calStart.after(calEnd)) {
                sb.setLength(0);
                if (indexNoYear == -1) {
                    sb.append(Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(calStart.getTime()));
                } else {
                    sb.append(Objects.requireNonNull(ContactsEvents.sdf_DDMM.get()).format(calStart.getTime()));
                    sb.append(Constants.STRING_PERIOD);
                    sb.append(Constants.STRING_0000);
                }
                sb.append(eventBody);
                result.add(sb.toString());
                calStart.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return Arrays.asList(eventsArray);
        }
    }

    static class EventAliases {

        public static final String CANONICAL_EASTER = "EASTER";
        public static final String CANONICAL_CATHOLIC_EASTER = "CATHOLIC_EASTER";
        public static final String CANONICAL_NY = "NY";

        private static final Map<String, Set<String>> aliasesMap = new HashMap<>();

        static {
            aliasesMap.put(CANONICAL_EASTER, new HashSet<>(Arrays.asList(
                    "easter", "ostern", "pascua", "páscoa", "pâques", "velikonoce",
                    "wielkanoc", "великдень", "вялікдзень", "пасха"
            )));

            aliasesMap.put(CANONICAL_CATHOLIC_EASTER, new HashSet<>(Arrays.asList(
                    "c_easter", "c_pâques", "catholic_easter", "catholique_pâques", "k_ostern",
                    "k_velikonoce", "katholisches_ostern", "katolické_velikonoce", "pascua_c",
                    "pascua_católica", "páscoa_c", "páscoa_católica", "wielkanoc", "к_великдень",
                    "к_вялікдзень", "к_пасха", "католицький_великдень"
            )));

            aliasesMap.put(CANONICAL_NY, new HashSet<>(Arrays.asList(
                    "новый_год", "new_year", "jour_de_l'an", "an", "na", "nj", "ny", "nie", "нг", "нр"
            )));

        }

        /**
         * Проверяет, начинается ли строка с одного из алиасов для заданного канонического события.
         */
        public static boolean startsWithAlias(String inputString, String canonicalName) {
            if (inputString == null || inputString.isEmpty()) return false;

            Set<String> aliases = aliasesMap.get(canonicalName);
            if (aliases == null) return false;

            // Подготовка строки для поиска
            String properInput = inputString.toLowerCase().replace(Constants.STRING_SPACE, Constants.STRING_UNDERSCORE);

            for (String alias : aliases) {
                if (properInput.startsWith(alias)) {
                    return true;
                }
            }
            return false;
        }

        /** Возвращает подошедший алиас для заданного канонического события
         * @param inputString Строка, начинающаяся с алиаса события
         * @param canonicalName Наименование типа события
         * @return Подошедший алиас события
         */
        // Метод, если нужно получить именно совпавший алиас (например, чтобы знать его длину и откусить его от строки)
        @NonNull public static String getMatchedAlias(String inputString, String canonicalName) {
            if (inputString == null || inputString.isEmpty()) return Constants.STRING_EMPTY;
            Set<String> aliases = aliasesMap.get(canonicalName);
            if (aliases == null) return Constants.STRING_EMPTY;

            String properInput = inputString.toLowerCase().replace(Constants.STRING_SPACE, Constants.STRING_UNDERSCORE);
            for (String alias : aliases) {
                if (properInput.startsWith(alias)) {
                    return alias; // Возвращаем оригинальный алиас из сета
                }
            }
            return Constants.STRING_EMPTY;
        }
    }
}
