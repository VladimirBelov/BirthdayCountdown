/*
 * *
 *  * Created by Vladimir Belov on 23.10.2026, 23:46
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 23.10.2026, 22:20
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Конфигурация отображения события.
 * Хранится в одном StringSet ключа "List_EventInfo_Config" в формате "ключ=значение". *
 * <p></p>Формат элементов:
 *   <br>photo=top|center|hidden       — положение фото
 *   <br>dates_pos=top|center          — положение блока дат
 *   <br>before=when,date_before       — порядок параметров секции "До события"
 *   <br>details=job_title,age,zodiac  — порядок параметров секции "Детали"
 *   <br>dates=age_next,date_event     — порядок параметров секции "Даты"
 *   <br>color.age_next=#FFFF0000      — цвет параметра
 *   <br>setting.zodiac=sign,animal    — доп. настройки параметра
 *   <br>version=1                     — версия формата
 */
public class EventDisplayConfig {
    private static final String PREF_KEY = "List_EventInfo_Config";
    private static final String VERSION = "1";

    // Ключи общих параметров
    public static final String KEY_PHOTO = "photo";
    public static final String KEY_DATES_POS = "dates_pos";

    // Ключи секций
    public static final String KEY_BEFORE = "before";
    public static final String KEY_DETAILS = "details";
    public static final String KEY_DATES = "dates";

    private static final String PREFIX_COLOR = "color.";
    private static final String PREFIX_SETTING = "setting.";
    private static final String KEY_VERSION = "version";

    // ID параметров
    public static final String PARAM_WHEN = "when";
    public static final String PARAM_DAY_OF_WEEK = "day_of_week";
    public static final String PARAM_DATE_BEFORE = "date_before";
    public static final String PARAM_NEWLINE = "newline";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_JOB_TITLE = "job_title";
    public static final String PARAM_ORGANIZATION = "organization";
    public static final String PARAM_NICKNAME = "nickname";
    public static final String PARAM_EVENT_CAPTION = "event_caption";
    public static final String PARAM_EVENT_LABEL = "event_lbel";
    public static final String PARAM_EVENT_SOURCE = "event_source";
    public static final String PARAM_EVENT_EMOJI = "event_emoji";
    public static final String PARAM_EVENT_TYPE = "event_type";
    public static final String PARAM_FAVORITE_ICON = "favorite_icon";
    public static final String PARAM_AGE = "age";
    public static final String PARAM_ZODIAC = "zodiac";
    public static final String PARAM_ZODIAC_ANIMAL = "animal";
    public static final String PARAM_ZODIAC_SIGN = "sign";
    public static final String PARAM_WEB_LINK = "web_link";
    public static final String PARAM_DESCRIPTION = "description";
    public static final String PARAM_DEBUG_INFO = "debug_info";
    public static final String PARAM_DATE_ORIGINAL = "date_original";
    public static final String PARAM_EVENT_ICON = "event_icon";
    public static final String PARAM_AGE_NEXT = "age_next";
    public static final String PARAM_AGE_NEXT_FULL = "full";
    public static final String PARAM_AGE_NEXT_BOLD = "bold";
    public static final String PARAM_AGE_NEXT_SHADOW = "shadow";
    public static final String PARAM_DATE_EVENT = "date_event";
    public static final String PARAM_NAME_SPLIT = "split";

    // Значения общих параметров
    public static final String PHOTO_TOP = "top";
    public static final String PHOTO_CENTER = "center";
    public static final String PHOTO_HIDDEN = "hidden";
    public static final String DATES_TOP = "top";
    public static final String DATES_CENTER = "center";

    // Значения по умолчанию
    private static final List<String> DEFAULT_BEFORE = Arrays.asList(
            PARAM_WHEN,
            PARAM_DAY_OF_WEEK
    );

    private static final List<String> DEFAULT_DETAILS = Arrays.asList(
            PARAM_JOB_TITLE,
            PARAM_ORGANIZATION,
            PARAM_NICKNAME,
            PARAM_EVENT_CAPTION
    );

    private static final List<String> DEFAULT_DATES = Arrays.asList(
            PARAM_DATE_ORIGINAL,
            PARAM_AGE_NEXT
    );

    // Значения по умолчанию для доп. настроек
    private static final List<String> DEFAULT_SETTING_ZODIAC = Arrays.asList(
            PARAM_ZODIAC_SIGN,
            PARAM_ZODIAC_ANIMAL
    );

    private static final List<String> DEFAULT_SETTING_AGE_NEXT = Collections.singletonList(
            PARAM_AGE_NEXT_FULL
    );

    private static final String DEFAULT_SETTING_NAME = "";

    private final SharedPreferences prefs;
    private final Map<String, String> cache = new HashMap<>();

    public EventDisplayConfig(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        load();
    }

    private void load() {
        cache.clear();
        Set<String> raw = prefs.getStringSet(PREF_KEY, Collections.emptySet());
        for (String item : raw) {
            int eq = item.indexOf('=');
            if (eq > 0) {
                cache.put(item.substring(0, eq), item.substring(eq + 1));
            }
        }
        // Если кэш пуст (первый запуск или после очистки), загружаем значения по умолчанию
        if (cache.isEmpty()) {
            loadDefaults();
        }
    }

    void save() {
        Set<String> set = new HashSet<>();
        set.add(KEY_VERSION + "=" + VERSION);
        for (Map.Entry<String, String> e : cache.entrySet()) {
            if (!KEY_VERSION.equals(e.getKey())) {
                set.add(e.getKey() + "=" + e.getValue());
            }
        }
        prefs.edit().putStringSet(PREF_KEY, set).apply();
    }

    private String get(String key, String defaultValue) {
        return cache.containsKey(key) ? cache.get(key) : defaultValue;
    }

    // ===== Общие параметры =====
    public String getPhotoPosition() { return get(KEY_PHOTO, PHOTO_TOP); }
    public void setPhotoPosition(String value) { cache.put(KEY_PHOTO, value); }

    public String getDatesPosition() { return get(KEY_DATES_POS, DATES_TOP); }
    public void setDatesPosition(String value) { cache.put(KEY_DATES_POS, value); }

    // ===== Порядок секций =====
    public List<String> getBeforeOrder() {
        return parseCsv(get(KEY_BEFORE, joinCsv(DEFAULT_BEFORE)));
    }

    public void setBeforeOrder(List<String> order) {
        cache.put(KEY_BEFORE, joinCsv(order));
    }

    public List<String> getDetailsOrder() {
        return parseCsv(get(KEY_DETAILS, joinCsv(DEFAULT_DETAILS)));
    }

    public void setDetailsOrder(List<String> order) {
        cache.put(KEY_DETAILS, joinCsv(order));
    }

    public List<String> getDatesOrder() {
        return parseCsv(get(KEY_DATES, joinCsv(DEFAULT_DATES)));
    }

    public void setDatesOrder(List<String> order) {
        cache.put(KEY_DATES, joinCsv(order));
    }

    // ===== Цвета =====
    /**
     * @return цвет параметра, или defaultColor если цвет не задан / полностью прозрачный
     */
    public int getColor(String paramId, int defaultColor) {
        String hex = get(PREFIX_COLOR + paramId, null);
        if (hex == null) return defaultColor;
        try {
            int c = Color.parseColor(hex);
            if (Color.alpha(c) == 0) return defaultColor;
            return c;
        } catch (Exception e) {
            return defaultColor;
        }
    }

    public void setColor(String paramId, int color) {
        if (color == 0 || Color.alpha(color) == 0) {
            cache.remove(PREFIX_COLOR + paramId);
        } else {
            cache.put(PREFIX_COLOR + paramId, String.format("#%08X", color));
        }
    }

    // ===== Доп. настройки =====
    /**
     * Сохранить значение доп. настройки параметра.
     * Пустая строка сохраняется явно — это отличает "пользователь отключил всё"
     * от "настройки не менялись (используется дефолт)".
     */
    public void setSetting(String paramId, String value) {
        // Сохраняем в кэш, даже если значение пустое.
        // Это позволяет отличить "пользователь явно отключил всё" от "настройки не менялись (дефолт)".
        cache.put(PREFIX_SETTING + paramId, value == null ? "" : value);
    }

    /**
     * Получить значение доп. настройки параметра.
     * Если настройка не задана — возвращается значение по умолчанию из getDefaultSetting().
     */
    public String getSetting(String paramId) {
        return get(PREFIX_SETTING + paramId, getDefaultSetting(paramId));
    }

    // ===== Утилиты =====
    private List<String> parseCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.isEmpty()) return result;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private String joinCsv(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /** Сброс всех настроек к значениям по умолчанию */
    public void resetToDefaults() {
        cache.clear();
        loadDefaults();
    }

    private void loadDefaults() {
        cache.put(KEY_PHOTO, PHOTO_TOP);
        cache.put(KEY_DATES_POS, DATES_TOP);
        cache.put(KEY_BEFORE, joinCsv(DEFAULT_BEFORE));
        cache.put(KEY_DETAILS, joinCsv(DEFAULT_DETAILS));
        cache.put(KEY_DATES, joinCsv(DEFAULT_DATES));
        cache.put(PREFIX_SETTING + PARAM_ZODIAC, joinCsv(DEFAULT_SETTING_ZODIAC));
        cache.put(PREFIX_SETTING + PARAM_AGE_NEXT, joinCsv(DEFAULT_SETTING_AGE_NEXT));
    }

    /**
     * Возвращает значение по умолчанию для доп. настройки параметра.
     * Если для параметра дефолт не определён — возвращает пустую строку.
     */
    public String getDefaultSetting(String paramId) {
        switch (paramId) {
            case PARAM_NAME:
                return DEFAULT_SETTING_NAME;
            case PARAM_ZODIAC:
                return joinCsv(DEFAULT_SETTING_ZODIAC);
            case PARAM_AGE_NEXT:
                return joinCsv(DEFAULT_SETTING_AGE_NEXT);
            default:
                return "";
        }
    }

}
