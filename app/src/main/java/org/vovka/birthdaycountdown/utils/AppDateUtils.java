/*
 * *
 *  * Created by Vladimir Belov on 26.12.2025, 20:59
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.12.2025, 16:03
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

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
}
