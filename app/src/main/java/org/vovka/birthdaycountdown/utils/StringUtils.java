/*
 * *
 *  * Created by Vladimir Belov on 30.06.2026, 00:18
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 29.06.2026, 23:51
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.vovka.birthdaycountdown.Constants;
import org.vovka.birthdaycountdown.ContactsEvents;
import org.vovka.birthdaycountdown.R;
import org.vovka.birthdaycountdown.ToastExpander;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Проверяет, является ли code point эмодзи
     */
    @SuppressWarnings("ConstantValue")
    private static boolean isEmoji(int codePoint) {
        // Проверка по диапазонам (охватываем основные блоки эмодзи)
        boolean inRange = (codePoint >= 0x1F000 && codePoint <= 0x1FFFF) || // Supplementary
                (codePoint >= 0x2600 && codePoint <= 0x27BF) ||   // Misc Symbols
                (codePoint >= 0x2B00 && codePoint <= 0x2BFF) ||   // Misc Symbols & Pictographs
                (codePoint >= 0x2100 && codePoint <= 0x21FF) ||   // Letterlike Symbols (тут живёт ℹ️)
                (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF);   // Flags

        // Проверка по Unicode General Category (So = Symbol Other, Sk = Symbol Modifier)
        int type = Character.getType(codePoint);
        boolean isSymbol = (type == Character.OTHER_SYMBOL || type == Character.MODIFIER_SYMBOL);

        return inRange || isSymbol;
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    /** Получает эмоджи из начала строки
     * @param text Исходная строка
     * @return Эмоджи
     */
    public static @NonNull String extractLeadingEmoji(String text) {
        if (text == null || text.isEmpty()) return Constants.STRING_EMPTY;

        // 1. Отделяем часть до первого пробела
        int spaceIndex = text.indexOf(' ');
        String part = (spaceIndex > 0) ? text.substring(0, spaceIndex) : text.trim();
        if (part.isEmpty()) return Constants.STRING_EMPTY;

        // 2. Читаем первый Unicode code point
        int cp1 = part.codePointAt(0);

        // Проверка на эмодзи (если не эмодзи — выход)
        if (!isEmoji(cp1)) {
            return Constants.STRING_EMPTY;
        }

        // 3. Считаем длину в char (1 для BMP, 2 для суррогатных пар)
        int totalLength = Character.charCount(cp1);

        // 4. Проверяем следующий символ: это может быть:
        //    а) Variation Selector (U+FE0E или U+FE0F)
        //    б) Второй Regional Indicator (для флагов 🇷🇺)
        if (totalLength < part.length()) {
            int cp2 = part.codePointAt(totalLength);

            // Вариант А: Вариационный селектор (добавляем его к длине)
            if (cp2 == 0xFE0E || cp2 == 0xFE0F) {
                totalLength += Character.charCount(cp2);
            }
            // Вариант Б: Второй код флага (например, 🇷🇺 = 🇷 + 🇺)
            else if (isRegionalIndicator(cp1) && isRegionalIndicator(cp2)) {
                totalLength += Character.charCount(cp2);
            }
        }

        return totalLength > 0 ? part.substring(0, totalLength) : Constants.STRING_EMPTY;
    }

    /**
     * Безопасно разбивает строку по разделителю, игнорируя экранированные символы (\;)
     */
    public static List<String> splitWithEscape(String str, char delimiter) {
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean isEscaped = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (isEscaped) {
                currentPart.append(c);
                isEscaped = false;
            } else if (c == '\\') {
                isEscaped = true; // Следующий символ экранирован
                currentPart.append(c); // Сохраняем слэш, уберем его позже
            } else if (c == delimiter) {
                parts.add(currentPart.toString());
                currentPart.setLength(0); // Очищаем буфер для следующей части
            } else {
                currentPart.append(c);
            }
        }
        parts.add(currentPart.toString()); // Добавляем последнюю часть
        return parts;
    }

    /**
     * Очищает строку от экранирующих слэшей (\; -> ; и \\ -> \)
     */
    public static String cleanValue(String val) {
        if (val == null) return Constants.STRING_EMPTY;
        return val.replace("\\;", ";")
                .replace("\\,", ",")
                .replace("\\\\", "\\")
                .trim();
    }

    /**
     * Проверяет, закодировано ли значение тега в Quoted-Printable.
     */
    public static boolean isQuotedPrintable(String line) {
        return line.contains("=QUOTED-PRINTABLE");
    }

    /**
     * Декодирует строку из формата Quoted-Printable.
     *
     * @param input Строка из vCard (например: "=D0=98=D0=B2=D0=B0=D0=BD")
     * @param charset Кодировка текста (для Android/Google контактов обычно UTF-8)
     * @return Нормальный читаемый текст
     */
    public static String decodeQuotedPrintable(String input, Charset charset) {
        if (input == null || input.isEmpty()) {
            return Constants.STRING_EMPTY;
        }
        if (!input.contains(Constants.STRING_EQ)) {
            return input;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '=') {
                // Проверяем, что после '=' есть еще как минимум два символа для шестнадцатеричного байта
                if (i + 2 < input.length()) {
                    String hex = input.substring(i + 1, i + 3);
                    try {
                        // Конвертируем "D0" в байт
                        int b = Integer.parseInt(hex, 16);
                        out.write(b);
                        i += 2; // Пропускаем обработанные hex-символы
                    } catch (NumberFormatException e) {
                        // Если это был не hex (ошибка формата), записываем как обычный символ
                        out.write(c);
                    }
                } else {
                    out.write(c);
                }
            } else {
                // Обычные ASCII символы пишем как есть
                out.write(c);
            }
        }

        // Декодируем собранный массив байтов в нужную кодировку
        return new String(out.toByteArray(), charset);
    }

    /**
     * Извлекает значение тега из строки vCard.
     * Поддерживает форматы "TAG:value" и "TAG;PARAMS:value".
     * Возвращает null, если строка не начинается с указанного тега.
     */
    public static String getTagValue(String line, String tag) {
        if (line.startsWith(tag + Constants.STRING_COLON)) {
            return line.substring(tag.length() + 1);
        }
        if (line.startsWith(tag + Constants.STRING_SEMICOLON)) {
            int colonIndex = line.indexOf(Constants.STRING_COLON);
            if (colonIndex != -1) {
                return line.substring(colonIndex + 1);
            }
        }
        return null;
    }

    /**
     * Регистронезависимый поиск подстроки.
     */
    public static int indexOfIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return -1;
        int len = searchStr.length();
        int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return i;
            }
        }
        return -1;
    }

    /** Возвращает исходный текст, обрамлённый в цвет в формате HTML (<font color="">текст</font>)
     * @param msg Исходный текст
     * @param color Цвет
     * @param context Контекст
     * @return Результирующий текст
     */
    public static String getHTMLColor(String msg, int color, Context context) {
        try {

            int colorId;
            switch (color) {
                case Constants.HTML_COLOR_RED:
                    colorId = R.color.dark_red;
                    break;
                case Constants.HTML_COLOR_YELLOW:
                    colorId = R.color.yellow;
                    break;
                case Constants.HTML_COLOR_BROWN:
                    colorId = R.color.brown;
                    break;
                case Constants.HTML_COLOR_GREEN:
                    colorId = R.color.green;
                    break;
                default:
                    return msg;
            }
            return Constants.HTML_COLOR_START + Integer.toHexString(ContextCompat.getColor(context, colorId) & 0x00ffffff)
                    + Constants.HTML_COLOR_MIDDLE + msg + Constants.HTML_COLOR_END;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return msg;
        }
    }

    @NonNull
    public static String getAgeFormated(@NonNull String strAge, @NonNull Set<String> ageFormat, @NonNull Resources res) {
        try {

            String result = strAge;
            final String replacementXK = Constants.STRING_000 + Constants.STRING_SPACE;
            if (ageFormat.contains(res.getString(R.string.pref_List_AgeFormat_Convert000toK)) && result.contains(replacementXK)) {
                result = result.replace(replacementXK, "K ");
            } else if (ageFormat.contains(res.getString(R.string.pref_List_AgeFormat_SeparateThousands))) {
                int indFirstSpace = result.indexOf(Constants.STRING_SPACE);
                int indLastSpace = result.lastIndexOf(Constants.STRING_SPACE);

                if (indFirstSpace > -1 && indFirstSpace == indLastSpace) {
                    //https://stackoverflow.com/questions/5323502/how-to-set-thousands-separator-in-java
                    DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
                    DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
                    symbols.setGroupingSeparator('\u00a0');
                    formatter.setDecimalFormatSymbols(symbols);

                    String postfix = result.substring(indLastSpace);
                    result = formatter.format(Integer.parseInt(result.substring(0, indLastSpace))).concat(postfix);
                }
            }
            if (!ageFormat.contains(res.getString(R.string.pref_List_AgeFormat_AddPostfix))) {
                result = result.substring(0, result.indexOf(Constants.STRING_SPACE));
            }
            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return strAge;
        }
    }

    /** Возвращает данные события в виде строки (разделитель: \n)
     * @param event Массив с данными события
     * @param res Ресурсы
     * @return Строка
     */
    @NonNull
    public static String getEventDataAsString(@NonNull String[] event, Resources res) {
        StringBuilder eventInfo = new StringBuilder();
        try {
            int eventRows = event.length;
            for (int i = 0; i < eventRows; i++) {
                String row = event[i];
                if (i == ContactsEvents.Position_photo && !TextUtils.isEmpty(row) & !row.startsWith(Constants.STRING_BRACKETS_START)) {
                    eventInfo.append(i)
                            .append(Constants.STRING_COLON_SPACE)
                            .append(res.getString(R.string.event_photo_details, row.length()))
                            .append(Constants.STRING_EOL);
                } else {
                    eventInfo.append(i).append(Constants.STRING_COLON_SPACE).append(row).append(Constants.STRING_EOL);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return eventInfo.toString();
    }

    /** Возвращает список фактов как единую строку с иконками и переводом строк
     * @param listFacts Список фактов
     * @param res Ресурсы
     * @return Единая строка с фактами
     */
    @NonNull
    public static String getFactsAsString(@NonNull List<String> listFacts, Resources res) {
        StringBuilder eventDetails = new StringBuilder();
        try {

            for (String fact : listFacts) {
                if (eventDetails.length() > 0) {
                    eventDetails.append(Constants.STRING_EOL);
                }
                eventDetails.append(res.getString(R.string.event_type_fact_emoji));
                eventDetails.append(Constants.STRING_SPACE);
                eventDetails.append(fact);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return eventDetails.toString();
    }

    /** Возвращает содержимое файла в виде строки
     * @param file Данные о файле (имя файла | URL или просто URL)
     * @param delimiter Разделитель строк, обычно EOL
     * @param context Контекст
     * @param res Ресурсы
     * @return Содержимое файла в виде строки
     */
    @NonNull
    public static String readFileToString(@NonNull String file, String delimiter, Context context, Resources res) {

        StringBuilder sb = new StringBuilder();

        try {

            String[] fileDetails = file.split(Constants.REGEX_BAR);
            Uri uri = null;
            ContentResolver contentResolver = context.getContentResolver();
            try {
                if (fileDetails.length < 2) {
                    uri = Uri.parse(fileDetails[0]);
                } else {
                    uri = Uri.parse(fileDetails[1]);
                }
            } catch (NullPointerException ignored) { /**/ }
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line = reader.readLine();
                    while (line != null) {
                        if (!line.startsWith(Constants.STRING_HASH) && !line.startsWith(Constants.STRING_DSLASH)) {
                            sb.append(line);
                            if (delimiter != null) sb.append(delimiter);
                        }
                        line = reader.readLine();
                    }
                    if (inputStream != null) inputStream.close();
                } catch (SecurityException se) {
                    ToastExpander.showDebugMsg(context, res.getString(R.string.msg_file_open_error) + fileDetails[0] + Constants.STRING_COMMA_SPACE +
                            se.getMessage());
                } catch (Exception e) {
                    ToastExpander.showDebugMsg(context, res.getString(R.string.msg_file_access_read_error, fileDetails[0]) + Constants.STRING_COMMA_SPACE +
                            e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return sb.toString();
    }

    /**
     * Get the method name for a depth in call stack.
     *
     * @param depth depth in the call stack (0 means current method, 1 means call method, ...)
     * @return method name
     */
    public static String getMethodName(final int depth) {
        StackTraceElement[] ste = null;
        try {
            ste = Thread.currentThread().getStackTrace();
        } catch (SecurityException se) { /**/ }
        return depth >= 0 && ste != null ? ste[depth].getClassName() + "->" + ste[depth].getMethodName() : Constants.STRING_EMPTY;
    }

    /** Возвращает название годовщины свадьбы
     * @param age Год годовщины
     * @param context Контекст
     * @param res Ресурсы
     * @return Название свадьбы
     */
    @SuppressLint("DiscouragedApi")
    @Nullable
    public static String getWeddingName(int age, Context context, Resources res) {
        try {
            return context.getString(res.getIdentifier(Constants.STRING_TYPE_WEDDING + age, Constants.RES_TYPE_STRING, context.getPackageName()));
        } catch (Resources.NotFoundException nfe) {
            return null;
        }
    }
}
