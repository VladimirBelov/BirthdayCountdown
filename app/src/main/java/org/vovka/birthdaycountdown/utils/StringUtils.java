/*
 * *
 *  * Created by Vladimir Belov on 26.12.2025, 20:59
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.12.2025, 19:38
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.vovka.birthdaycountdown.Constants;

import java.util.Locale;

public class StringUtils {

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

    @NonNull
    public static String substringBefore(String text, String sep) {
        if (text == null) return Constants.STRING_EMPTY;
        if (sep == null) return text;
        return text.contains(sep) ? text.substring(0, text.indexOf(sep)) : text;
    }

    @NonNull
    public static String substringAfter(String text, String sep) {
        if (text == null) return Constants.STRING_EMPTY;
        if (sep == null) return text;
        return text.contains(sep) ? text.substring(text.indexOf(sep) + sep.length()) : text;
    }

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

    @Nullable
    public static String normalizeName(String inName) {

        if (inName == null) return null;

        String normalName = inName.toLowerCase(Locale.ROOT);
        if (normalName.contains(Constants.STRING_COMMA)) {
            normalName = normalName.replace(Constants.STRING_COMMA, Constants.STRING_EMPTY);
        }
        if (normalName.contains("ё")) {
            normalName = normalName.replace("ё", "е");
        }
        if (normalName.contains("é")) {
            normalName = normalName.replace("é", "e");
        }
        if (normalName.contains(Constants.STRING_EOL)) {
            normalName = normalName.replace(Constants.STRING_EOL, Constants.STRING_EMPTY);
        }
        if (normalName.contains("\r")) {
            normalName = normalName.replace("\r", Constants.STRING_EMPTY);
        }
        if (normalName.contains("\t")) {
            normalName = normalName.replace("\t", Constants.STRING_SPACE);
        }
        return normalName;
    }
}
