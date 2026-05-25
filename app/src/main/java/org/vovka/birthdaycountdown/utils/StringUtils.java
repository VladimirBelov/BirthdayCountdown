/*
 * *
 *  * Created by Vladimir Belov on 25.05.2026, 23:59
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 21.05.2026, 22:08
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.vovka.birthdaycountdown.Constants;
import org.vovka.birthdaycountdown.ContactsEvents;
import org.vovka.birthdaycountdown.R;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class StringUtils {
    static final String TAG = "StringUtils";
    private static final Pattern DIACRITICS_AND_ACCENTS_PATTERN =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static boolean hasContent(String s) {
        return s != null && TextUtils.getTrimmedLength(s) > 0;
    }

    /** Возвращает значение или пустую строку
     * @param strIn Строка (может быть пустая или null)
     * @return Строка
     */
    @NonNull
    public static String getNotNullString(String strIn) {
        return strIn == null || strIn.isEmpty() ? Constants.STRING_EMPTY : strIn;
    }

    @NonNull
    public static Long parseToLong(String strIn) {

        try {
            return Long.parseLong(strIn);
        } catch (NumberFormatException e) {
            return 0L;
        }

    }

    public static boolean contains(final int[] arr, final int key) {
        for (final int i : arr) {
            if (i == key) return true;
        }
        return false;
    }

    /** Возвращает текст до подстроки sep. Если подстрока не найдена - исходный текст
     * @param text Исходный текст
     * @param sep Подстрока
     * @return Текст до подстроки
     */
    @NonNull
    public static String substringBefore(String text, String sep) {
        if (text == null) return Constants.STRING_EMPTY;
        if (sep == null) return text;
        return text.contains(sep) ? text.substring(0, text.indexOf(sep)) : text;
    }

    /** Возвращает текст после подстроки sep. Если подстрока не найдена - исходный текст
     * @param text Исходный текст
     * @param sep Подстрока
     * @return Текст после подстроки
     */
    @NonNull
    public static String substringAfter(String text, String sep) {
        if (text == null) return Constants.STRING_EMPTY;
        if (sep == null) return text;
        return text.contains(sep) ? text.substring(text.indexOf(sep) + sep.length()) : text;
    }

    /** Возвращает текст между подстроками
     * @param text Исходный текст
     * @param sep1 Подстрока 1
     * @param sep2 Подстрока 2
     * @return Текст между подстроками
     */
    @NonNull
    public static String substringBetween(String text, String sep1, String sep2) {
        if (text == null) return Constants.STRING_EMPTY;
        return substringBefore(substringAfter(text, sep1), sep2);
    }

    @NonNull
    public static String toProperCase(@NonNull String str) {
        if (!str.isEmpty()) {
            char[] chars = str.toLowerCase().toCharArray();
            chars[0] = Character.toUpperCase(chars[0]);
            return new String(chars);
        } else {return str;}
    }

    /** Подготовка строки для поиска
     * @param inName Входная строка
     * @return Нормализованная строка для поиска
     */
    @Nullable
    public static String normalizeString(@Nullable String inName) {
        if (inName == null) return null;

        // Шаг 1: приведение к нижнему регистру
        String normalized = inName.toLowerCase(Locale.ROOT);

        // Шаг 2: декомпозиция символов (разделение буквы и диакритики)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);

        // Шаг 3: удаление всех комбинирующихся диакритических знаков
        // (остаются только "чистые" буквы)
        normalized = DIACRITICS_AND_ACCENTS_PATTERN.matcher(normalized).replaceAll("");

        // Шаг 4: замена кириллической "ё" на "е"
        normalized = normalized.replace("ё", "е");

        // Шаг 5: очистка от нежелательных символов
        normalized = normalized
                .replace(Constants.STRING_COMMA, Constants.STRING_EMPTY)
                .replace(Constants.STRING_EOL, Constants.STRING_EMPTY)
                .replace("\r", Constants.STRING_EMPTY)
                .replace("\t", Constants.STRING_SPACE);

        return normalized;
    }

    @NonNull
    public static String[] getKeyParts(@NonNull String eventKey) {
        return eventKey.replace(Constants.STRING_2HASH, Constants.STRING_EOT).split(Constants.STRING_EOT, -1);
    }

    @NonNull
    public static String getHash(@NonNull String from) {
        return String.valueOf(Math.abs(from.hashCode()));
    }

    /** Заменяет id календарей его наименованием
     * @param setIDs Список id календарей
     * @param mapTitles Список данных о календаре
     * @return Строка с наименованиями календарей через запятую
     */
    public static String replaceCalendarIDtoTitle(Set<String> setIDs, HashMap<String, String> mapTitles){

        StringBuilder sb = new StringBuilder();
        try {

            for(String id: setIDs){
                if (sb.length() > 0) sb.append(Constants.STRING_COMMA_SPACE);
                String calData = mapTitles.get(id);
                if (calData != null) {
                    String[] calInfo = getKeyParts(calData);
                    sb.append(calInfo[0]);
                    if (calInfo.length > 1) sb.append(Constants.STRING_PARENTHESIS_OPEN).append(calInfo[1]).append(Constants.STRING_PARENTHESIS_CLOSE);
                } else sb.append(id);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return sb.toString();
    }

    @NonNull
    public static String getAgeString(long age, int id_prefix_1, int id_prefix_1_, int id_prefix_2_3_4, int id_prefix_5_20, @NonNull String locale, @NonNull Resources resources) {

        try {

            StringBuilder result = new StringBuilder();
            String count_str = Long.toString(age);
            String count_end = count_str.substring(count_str.length() - 1);
            boolean isEnd234 = count_end.equals(Constants.STRING_2) || count_end.equals(Constants.STRING_3) || count_end.equals(Constants.STRING_4);
            long ageMinus100 = age % 100;

            result.append(age);

            if (!resources.getString(R.string.pref_Language_fr).equalsIgnoreCase(locale)) {
                if (ageMinus100 == 1) { //Единственное число
                    result.append(resources.getString(id_prefix_1));
                } else if (ageMinus100 > 4 && ageMinus100 < 21) { //Больше 4, но меньше 21
                    result.append(resources.getString(id_prefix_5_20));
                } else if (count_end.equals(Constants.STRING_1)) { //Если заканчивается на 1, но не между 5-20
                    result.append(resources.getString(id_prefix_1_));
                } else if (isEnd234) { //Если заканчивается на 2, 3, 4
                    result.append(resources.getString(id_prefix_2_3_4));
                } else {
                    result.append(resources.getString(id_prefix_5_20));
                }
            } else { //Французский
                if (ageMinus100 == 1) { //Единственное число
                    result.append(resources.getString(id_prefix_1));
                } else if ((ageMinus100 >= 3 && ageMinus100 <= 5) || (ageMinus100 >= 8 && ageMinus100 <= 10)) { //3-5,8-10
                    result.append(resources.getString(id_prefix_1_));
                } else {
                    result.append(resources.getString(id_prefix_5_20));
                }
            }
            return result.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return Constants.STRING_EMPTY;
        }
    }

    @NonNull
    public static String getFullName(@NonNull String[] singleEventArray, ContactsEvents.FormatName preferences_name_format) {
        if (singleEventArray.length < Math.min(ContactsEvents.Position_personFullNameAlt, ContactsEvents.Position_personFullName)) {
            return Constants.STRING_EMPTY;
        }

        if (preferences_name_format == ContactsEvents.FormatName.LastnameFirst && !TextUtils.isEmpty(singleEventArray[ContactsEvents.Position_personFullNameAlt])) {
            return singleEventArray[ContactsEvents.Position_personFullNameAlt];
        } else {
            return singleEventArray[ContactsEvents.Position_personFullName];
        }
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
