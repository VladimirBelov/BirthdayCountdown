/*
 * *
 *  * Created by Vladimir Belov on 27.11.19 13:35
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 27.11.19 13:35
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA;
import static org.vovka.birthdaycountdown.Constants.STRING_EOF;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_OPEN;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.Constants.defaultNotificationID;

@SuppressWarnings("ConstantConditions")
class ContactsEvents {
    @SuppressLint("StaticFieldLeak")
    private static final ContactsEvents ourInstance = new ContactsEvents();


    static ContactsEvents getInstance() {
        return ourInstance;
    }

    private ContactsEvents() {
    }

    private Resources getResources(){
        if (context == null) return null;
        if (resources == null) resources = context.getResources();
        return resources;
    }

    //Константы
    final private Set<String> prefs_EventTypes_Default = new HashSet<String>() {{
        add(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY + Constants.STRING_EMPTY);
        add(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY + Constants.STRING_EMPTY);
        add(ContactsContract.CommonDataKinds.Event.TYPE_OTHER + Constants.STRING_EMPTY);
        add(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM + Constants.STRING_EMPTY);
    }};
    final private boolean[] prefs_EventTypes_DefaultB = {true,true,true,true,false};

    final private Set<String> pref_List_Bottom_Info = new HashSet<String>() {{
        add("1");
        add("2");
        add("3");
    }};

    private static final int Position_eventDate_sorted = 0;
    static final int Position_fio = 1;
    static final int Position_eventCaption = 2;
    static final int Position_eventLabel = 3;
    static final int Position_eventType = 4;
    static final int Position_dates = 5; //account_type: data1
    private static final int Position_eventDate = 6;
    static final int Position_eventDateText = 7;
    static final int Position_eventDistance = 8;
    static final int Position_eventDistanceText = 9;
    static final int Position_age = 10;
    static final int Position_age_caption = 11;
    static final int Position_organization = 12;
    static final int Position_title = 13;
    private static final int Position_photo_uri = 14;
    static final int Position_contact_id = 15;
    static final int Position_eventIcon = 16;
    private static final int Position_eventEmoji = 17; //https://www.piliapp.com/emoji/list/

    final int[] event_types_id = new int[]{
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
            ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
            ContactsContract.CommonDataKinds.Event.TYPE_OTHER,
            ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,
            11
    };

    private final String typeBirthday = Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
    private final String typeAnniversary = Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY);
    private final String typeOther = Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER);
    //final String typeCustom = Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM);

    //Хранилища данных
    String[] dataArray = null; //todo: переделать на объект или хотя бы ArrayList
    private boolean[] event_types_on;
    private String currentLocale = Constants.STRING_EMPTY;
    int currentTheme = 0;
    final private String systemLocale = Locale.getDefault().getLanguage();
    private HashSet<String> set_events_deaths;

    //Настройки
    boolean preferences_debug_on;
    private String preferences_language;

    Set<String> preferences_list_bottom_info;
    String preferences_list_prev_events;
    int preferences_list_prev_events_found = 0;
    private int preferences_list_sad_photo;
    int preferences_events_scope;
    int preferences_notification_channel_id;

    String preferences_widgets_bottom_info;
    String preferences_widgets_color_eventtoday;
    String preferences_widgets_color_eventsoon;
    boolean preferences_widgets_contactsphotos;
    boolean preferences_widgets_eventicons;
    int preferences_widgets_days_eventsoon;

    Matcher preferences_last_name_comletions_man;
    Matcher preferences_last_name_comletions_female;
    Matcher preferences_first_names_man;
    Matcher preferences_first_names_female;
    Matcher preferences_second_name_comletions_man;
    Matcher preferences_second_name_comletions_female;
    private Matcher preferences_death_labels;
    private Matcher preferences_birthday_labels = null;
    private Matcher preferences_wedding_labels;
    private Matcher preferences_nameday_labels;
    private Matcher preferences_crowning_labels;

    private boolean preferences_customevent1_enabled;
    private String preferences_customevent1_caption;
    private Matcher preferences_customevent1_labels;
    private boolean preferences_customevent1_useyear;

    private boolean preferences_customevent2_enabled;
    private String preferences_customevent2_caption;
    private Matcher preferences_customevent2_labels;
    private boolean preferences_customevent2_useyear;

    private boolean preferences_customevent3_enabled;
    private String preferences_customevent3_caption;
    private Matcher preferences_customevent3_labels;
    private boolean preferences_customevent3_useyear;

    private boolean preferences_customevent4_enabled;
    private String preferences_customevent4_caption;
    private Matcher preferences_customevent4_labels;
    private boolean preferences_customevent4_useyear;

    private boolean preferences_customevent5_enabled;
    private String preferences_customevent5_caption;
    private Matcher preferences_customevent5_labels;
    private boolean preferences_customevent5_useyear;

    /* preferences_notifications_type:
     *   0 - Одно общее уведомление
     *   1 - Каждое событие в отдельном уведомлении
     *   2 - Если собыий меньше 3 => отдельные, иначе - общее
     *   4 - Если собыий меньше 4 => отдельные, иначе - общее
     * */
    private int preferences_notifications_type;
    int preferences_notifications_days;
    private int preferences_notifications_alarm_hour;
    private int preferences_notifications_days_test;
    String preferences_notifications_ringtone;

    class MyTheme {
        int prefNumber; //Номер в shared preferences
        int themeMain; //Тема основной активности
        int themePopup; //Тема вплывающего меню
        int themeDialog; //Тема диалогов
    }
    MyTheme preferences_theme;

    private Set<String> preferences_hiddenEvents = new HashSet<>();


    //Статистика
    long statGetContacts = 0;
    long statComputeDates = 0;
    long statDrawList = 0;
    long statLastComputeDates = 0;

    //UI объекты
    Context context;
    private Resources resources;

    class ColumnIndexCache {
        //https://android.jlelse.eu/using-a-cache-to-optimize-data-retrieval-from-cursors-56f9eaa1e0d2

        final private HashMap<String, Integer> mMap = new HashMap<>();

        int getColumnIndex(Cursor cursor, String columnName) {
            if (!mMap.containsKey(columnName))
                mMap.put(columnName, cursor.getColumnIndex(columnName));
            return mMap.get(columnName);
        }

        void clear() {
            mMap.clear();
        }
    }

    private static int countLeapYearsBetween(int y1, int y2) {

        if (y1 < 1 || y2 < 1) {
            throw new IllegalArgumentException(Constants.MSG_YEAR_MUST_BE_GREATER_0);
        }
        // ensure y1 <= y2
        if (y1 > y2) {
            int i = y1;
            y1 = y2;
            y2 = i;
        }

        int diff;

        int firstDivisibleBy4 = y1;
        if (firstDivisibleBy4 % 4 != 0) {
            firstDivisibleBy4 += 4 - (y1 % 4);
        }
        diff = y2 - firstDivisibleBy4 - 1;
        int divisibleBy4 = diff < 0 ? 0 : diff / 4 + 1;

        int firstDivisibleBy100 = y1;
        if (firstDivisibleBy100 % 100 != 0) {
            firstDivisibleBy100 += 100 - (firstDivisibleBy100 % 100);
        }
        diff = y2 - firstDivisibleBy100 - 1;
        int divisibleBy100 = diff < 0 ? 0 : diff / 100 + 1;

        int firstDivisibleBy400 = y1;
        if (firstDivisibleBy400 % 400 != 0) {
            firstDivisibleBy400 += 400 - (y1 % 400);
        }
        diff = y2 - firstDivisibleBy400 - 1;
        int divisibleBy400 = diff < 0 ? 0 : diff / 400 + 1;

        return divisibleBy4 - divisibleBy100 + divisibleBy400;
    }

    private static Calendar from(@NonNull Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c;
    }

    private static Calendar removeTime(@NonNull Calendar c) {

        c.set(HOUR_OF_DAY, 0);
        c.set(MINUTE, 0);
        c.set(SECOND, 0);
        c.set(MILLISECOND, 0);

        return c;
    }

    private int countDaysDiff(@NonNull Date date1, @NonNull Date date2) {
        //https://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances/43681941#43681941

        try {
            boolean isNegative = false;
            Calendar c1 = removeTime(from(date1));
            Calendar c2 = removeTime(from(date2));

            if (c1.get(YEAR) == c2.get(YEAR)) {
                return  c2.get(DAY_OF_YEAR) - c1.get(DAY_OF_YEAR);
            }
            // ensure c1 <= c2
            if (c1.get(YEAR) > c2.get(YEAR)) {
                isNegative = true;
                Calendar c = c1;
                c1 = c2;
                c2 = c;
            }
            int y1 = c1.get(YEAR);
            int y2 = c2.get(YEAR);
            int d1 = c1.get(DAY_OF_YEAR);
            int d2 = c2.get(DAY_OF_YEAR);

            int resD = d2 + ((y2 - y1) * 365) - d1;
            if (isNegative) {
                return -(resD + countLeapYearsBetween(y1, y2));
            } else {
                return resD + countLeapYearsBetween(y1, y2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this, "ContactsEvents->countDaysDiff error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    private Date addYear(@NonNull Date date, int year) {
        try {
            Calendar c = from(date);
            c.add(Calendar.YEAR, year);
            return c.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this, "ContactsEvents->addYear error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return date;
        }
    }

    private int countYearsDiff(@NonNull Date date1, @NonNull Date date2) {
        try {

            Calendar c1 = removeTime(from(date1));
            Calendar c2 = removeTime(from(date2));

            return c2.get(YEAR) - c1.get(YEAR);

        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this, "ContactsEvents->countYearsDiff error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    void getPreferences() {

        if (context == null) return;

        //Инициализация и считывание настроек

        try {

            SharedPreferences preferences;
            event_types_on = prefs_EventTypes_DefaultB;

            try {
                PreferenceManager.setDefaultValues(context, R.xml.settings, false);
            } catch (ClassCastException e) { //Для старой версии, где настройки были другого типа
                //Toast.makeText(context, e.getClass().getName(), Toast.LENGTH_SHORT).show();
                preferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(context.getString(R.string.pref_Events_key), prefs_EventTypes_Default);
                editor.apply();
            }

            preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences_debug_on = preferences.getBoolean(context.getString(R.string.pref_Debug_On_key), false);
            Set<String> savedTypes;
            try {
                savedTypes = preferences.getStringSet(context.getString(R.string.pref_Events_key), prefs_EventTypes_Default);
            } catch (ClassCastException e) {
                savedTypes = prefs_EventTypes_Default;
            }

            if (savedTypes != null) {
                for (int i = 0; i < event_types_on.length; i++) {
                    event_types_on[i] = savedTypes.contains(event_types_id[i] + Constants.STRING_EMPTY);
                }
            }

            try {
                preferences_list_bottom_info = preferences.getStringSet(context.getString(R.string.pref_List_BottomInfo_key), pref_List_Bottom_Info);
            } catch (ClassCastException e) {
                preferences_list_bottom_info = pref_List_Bottom_Info;
            }
            preferences_list_prev_events = preferences.getString(context.getString(R.string.pref_List_PrevEvents_key), context.getString(R.string.pref_List_PrevEvents_default));
            preferences_list_sad_photo = Integer.parseInt(preferences.getString(context.getString(R.string.pref_List_SadPhoto_key), context.getString(R.string.pref_List_SadPhoto_default)));
            preferences_language = preferences.getString(context.getString(R.string.pref_Language_key), context.getString(R.string.pref_Language_default));
            preferences_widgets_bottom_info = preferences.getString(context.getString(R.string.pref_Widgets_BottomInfo_key), context.getString(R.string.pref_Widgets_BottomInfo_default));
            preferences_widgets_color_eventtoday = preferences.getString(context.getString(R.string.pref_Widgets_Color_EventToday_key), context.getString(R.string.pref_Widgets_Color_EventToday_default));
            preferences_widgets_color_eventsoon = preferences.getString(context.getString(R.string.pref_Widgets_Color_EventSoon_key), context.getString(R.string.pref_Widgets_Color_EventSoon_default));
            preferences_widgets_contactsphotos = preferences.getBoolean(context.getString(R.string.pref_Widgets_ContactPhotos_key), Boolean.getBoolean(context.getString(R.string.pref_Widgets_ContactPhotos_default)));
            preferences_widgets_eventicons = preferences.getBoolean(context.getString(R.string.pref_Widgets_EventIcons_key), Boolean.getBoolean(context.getString(R.string.pref_Widgets_EventIcons_default)));
            preferences_widgets_days_eventsoon = Integer.parseInt(preferences.getString(context.getString(R.string.pref_Widgets_Days_EventSoon_key), context.getString(R.string.pref_Widgets_Days_EventSoon_default)));
            preferences_events_scope = preferences.getInt(context.getString(R.string.pref_Events_Scope), Constants.pref_Events_Scope_NotHidden);
            preferences_notification_channel_id = preferences.getInt(context.getString(R.string.pref_Notifications_ChannelID), Constants.defaultNotificationID);

            boolean useInternal;
            String customLabels;
            final String regex_inter = "|"; //"\\Z|";
            //final String regex_last = ""; //"\\Z";
            //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

            //Определения событий

            //День рождения
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Birthday_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.equals(Constants.STRING_EMPTY)) {
                preferences_birthday_labels = null;
            } else {
                if (customLabels.equals(Constants.STRING_EMPTY))
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                else if (!useInternal) {
                    preferences_birthday_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Свадьба
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Anniversary_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.equals(Constants.STRING_EMPTY)) {
                preferences_wedding_labels = null;
            } else {
                if (customLabels.equals(Constants.STRING_EMPTY)) {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_wedding_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Именины
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_NameDay_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.equals(Constants.STRING_EMPTY)) {
                preferences_nameday_labels = null;
            } else {
                if (customLabels.equals(Constants.STRING_EMPTY)) {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_nameday_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Венчание
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Crowning_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.equals(Constants.STRING_EMPTY)) {
                preferences_crowning_labels = null;
            } else {
                if (customLabels.equals(Constants.STRING_EMPTY)) {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_crowning_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Годовщина смерти
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Death_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.equals(Constants.STRING_EMPTY)) {
                preferences_death_labels = null;
            } else {
                if (customLabels.equals(Constants.STRING_EMPTY)) {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_death_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Пользовательские события
            //1
            preferences_customevent1_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom1_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent1_enabled = false;

            if (!preferences_customevent1_caption.equals(Constants.STRING_EMPTY)) {
                String preferences_customevent1_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom1_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent1_labels_str.equals(Constants.STRING_EMPTY)) {
                    try {
                        preferences_customevent1_labels = Pattern.compile(preferences_customevent1_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent1_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent1_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom1_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //2
            preferences_customevent2_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom2_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent2_enabled = false;

            if (!preferences_customevent2_caption.equals(Constants.STRING_EMPTY)) {
                String preferences_customevent2_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom2_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent2_labels_str.equals(Constants.STRING_EMPTY)) {
                    try {
                        preferences_customevent2_labels = Pattern.compile(preferences_customevent2_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent2_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent2_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom2_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //3
            preferences_customevent3_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom3_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent3_enabled = false;

            if (!preferences_customevent3_caption.equals(Constants.STRING_EMPTY)) {
                String preferences_customevent3_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom3_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent3_labels_str.equals(Constants.STRING_EMPTY)) {
                    try {
                        preferences_customevent3_labels = Pattern.compile(preferences_customevent3_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent3_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent3_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom3_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //4
            preferences_customevent4_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom4_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent4_enabled = false;

            if (!preferences_customevent4_caption.equals(Constants.STRING_EMPTY)) {
                String preferences_customevent4_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom4_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent4_labels_str.equals(Constants.STRING_EMPTY)) {
                    try {
                        preferences_customevent4_labels = Pattern.compile(preferences_customevent4_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent4_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent4_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom4_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //5
            preferences_customevent5_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom5_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent5_enabled = false;

            if (!preferences_customevent5_caption.equals(Constants.STRING_EMPTY)) {
                String preferences_customevent5_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom5_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent5_labels_str.equals(Constants.STRING_EMPTY)) {
                    try {
                        preferences_customevent5_labels = Pattern.compile(preferences_customevent5_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent5_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent5_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom5_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //Уведомления
            preferences_notifications_type = Integer.parseInt(preferences.getString(context.getString(R.string.pref_Notifications_Type_key), context.getString(R.string.pref_Notifications_Type_default)));
            preferences_notifications_days = Integer.parseInt(preferences.getString(context.getString(R.string.pref_Notifications_Days_key), context.getString(R.string.pref_Notifications_Days_default)));
            preferences_notifications_days_test = Integer.parseInt(context.getString(R.string.pref_Notifications_Days_test));
            preferences_notifications_alarm_hour = Integer.parseInt(preferences.getString(context.getString(R.string.pref_Notifications_AlarmHour_key), context.getString(R.string.pref_Notifications_AlarmHour_default)));
            if (preferences_notifications_alarm_hour < 0) preferences_notifications_alarm_hour = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmHour_default));
            preferences_notifications_ringtone = preferences.getString(context.getString(R.string.pref_Notifications_Ringtone_key), Settings.System.DEFAULT_NOTIFICATION_URI.toString());

            //Запоминаем информацию о темах
            preferences_theme = new MyTheme();
            try {
                preferences_theme.prefNumber = Integer.parseInt(preferences.getString(context.getString(R.string.pref_Theme_key), context.getString(R.string.pref_Theme_default)));
            } catch (Exception e) {
                preferences_theme.prefNumber = 1;
            }
            switch (preferences_theme.prefNumber) {
                case 1:
                    preferences_theme.themeMain = R.style.AppTheme_Light_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Light_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Light;
                    break;
                case 2:
                    preferences_theme.themeMain = R.style.AppTheme_DarkGray_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_DarkGray_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_DarkGray;
                    break;
                case 3:
                    preferences_theme.themeMain = R.style.AppTheme_Black_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Black_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Black;
                    break;
            }

            //Скрытые события

            //https://medium.com/@anupamchugh/a-nightmare-with-shared-preferences-and-stringset-c53f39f1ef52
            //https://stackoverflow.com/questions/19949182/android-sharedpreferences-string-set-some-items-are-removed-after-app-restart
            Set<String> someSets = preferences.getStringSet(context.getString(R.string.pref_Events_Hidden_key), new HashSet<>());
            preferences_hiddenEvents = new HashSet<>(someSets); // THIS LINE CREATE A COPY


        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_PREFERENCES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    void setPreferences() {
        if (context == null) return;

        //Сохранение настроек

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_events_scope);
            editor.putInt(context.getString(R.string.pref_Notifications_ChannelID), preferences_notification_channel_id);
            editor.apply();

        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_PREFERENCES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    void setLocale(boolean force) {

        if (context == null) return;

        //todo: сделать так: https://stackoverflow.com/questions/39705739/android-n-change-language-programmatically/
        //todo: для Android > N переделать выбор локали https://stackoverflow.com/questions/47165311/how-to-change-android-o-oreo-api-26-app-language
        //todo: (?) переделать под onSharedPreferenceChanged https://stackoverflow.com/a/13862572
        //todo: посмотреть https://stackoverflow.com/questions/9475589/how-to-get-string-from-different-locales-in-android и сделать нормальным переключение языков
        try {
            //getPreferences();

            // http://developer.alexanderklimov.ru/android/locale.php
            if (force || !preferences_language.equals(currentLocale)) {

                Configuration configuration = new Configuration();
                Locale locale;
                if (preferences_language.equals(context.getString(R.string.pref_Language_default))) {
                    //Auto
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        locale = configuration.getLocales().get(0);
                    } else {
                        locale = new Locale(systemLocale);
                    }
                } else {
                    locale = new Locale(preferences_language);
                }

                configuration.setLocale(locale);

                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //    context = context.createConfigurationContext(configuration);
                //} else {
                resources = context.getResources();
                resources.updateConfiguration(configuration, null);
                //}

                currentLocale = preferences_language;

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_LOCALE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    //todo: попробовать добраться до ДР стандартными способами https://stackoverflow.com/questions/35448250/how-to-get-whatsapp-contacts-from-android
    //todo: сделать импорт ДР одновлассники https://ruseller.com/lessons.php?id=1661 https://apiok.ru/ext/oauth/
    boolean getContactsEvents(Context in_context) {

        context = in_context;
        if (context == null) return false;

        long statCurrentModuleStart = System.currentTimeMillis();

        try {

            StringBuilder dataRow;
            TreeMap<Integer, String> userData = new TreeMap <>();
            //TreeMap<String, TreeMap<Integer, String>> fullUserData = new TreeMap<>();
            List<String> dataList = new ArrayList<>();

            dataArray = null;
            dataList.clear();
            userData.clear();
            set_events_deaths = new HashSet<>();

            //todo: упростить. много лишних переменных-массивов
            //Получаем требуемые события (дни рождения, и т.п.)
            getPreferences();

            List<String> EventTypes = new ArrayList<>();
            if (event_types_on[0] || event_types_on[4]) EventTypes.add(Integer.toString(event_types_id[0])); //если нужны магические даты - собираем ДР, но не выводим их
            if (event_types_on[1]) EventTypes.add(Integer.toString(event_types_id[1]));
            if (event_types_on[2]) EventTypes.add(Integer.toString(event_types_id[2]));
            if (event_types_on[3]) EventTypes.add(Integer.toString(event_types_id[3]));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return false;
            } //todo: что делать с API меньше 23, пока не знаю

            ContentResolver contentResolver = context.getContentResolver();
            ColumnIndexCache cache = new ColumnIndexCache();
            //resources = context.getResources(); TEST

            //Перебираем все данные и кэшируем организацию и должность
            HashMap<String, String> orgMap = new HashMap<>();
            HashMap<String, String> titleMap = new HashMap<>();
            //https://stackoverflow.com/a/20260606/4928833
            Cursor orgCur = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE + "'",
                    null,
                    null
            );
            if (orgCur != null) {
                if (orgCur.moveToFirst()) {
                    do {

                        String personID = orgCur.getString(cache.getColumnIndex(orgCur, Constants.ColumnNames_CONTACT_ID));

                        String organization = orgCur.getString(cache.getColumnIndex(orgCur, ContactsContract.CommonDataKinds.Organization.COMPANY));
                        if (!orgMap.containsKey(personID)) orgMap.put(personID, organization != null ? organization : Constants.STRING_EMPTY);

                        String title = orgCur.getString(cache.getColumnIndex(orgCur, ContactsContract.CommonDataKinds.Organization.TITLE));
                        if (!titleMap.containsKey(personID)) titleMap.put(personID, title != null ? title : Constants.STRING_EMPTY);

                    } while (orgCur.moveToNext());
                    orgCur.close();
                }
            }

            //Перебираем все данные и кэшируем заметки
            //пока уберу, нужно только для поиска
/*
            HashMap<String, String> noteMap = new HashMap<>();
            //https://stackoverflow.com/a/6301244/4928833
            Cursor noteCur = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE + "'",
                    null,
                    null
            );
            if (noteCur != null) {
                if (noteCur.moveToFirst()) {
                    do {

                        String personID = noteCur.getString(cache.getColumnIndex(noteCur, "contact_id"));

                        String note = noteCur.getString(cache.getColumnIndex(noteCur, ContactsContract.CommonDataKinds.Note.NOTE));
                        if (!noteMap.containsKey(personID)) noteMap.put(personID, note != null ? note.replace("\n", "") : "");

                    } while (noteCur.moveToNext());
                    noteCur.close();
                }
            }
*/

            //Собираем данные о событиях

            Cursor cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "'",
                    null,
                    ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE + " ASC, " +
                            ContactsContract.CommonDataKinds.Event.TYPE + " ASC, " +
                            ContactsContract.CommonDataKinds.Event.LABEL + " ASC"
            );

            if (cursor != null) {
                String eventKey = Constants.STRING_EMPTY;


                if (cursor.moveToFirst()) {
                    do {
                        String eventDate = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.DATA));
                        String eventType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));

                        if (eventDate != null) {

                            String contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)); //бывает пусто
                            if (contactName == null) contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME));
                            if (contactName == null) contactName = Constants.STRING_EMPTY;
                            String accountType = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_TYPE));
                            String eventLabel = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.LABEL));
                            if (eventLabel == null) eventLabel = Constants.STRING_EMPTY;
                            boolean nonemptyEventLabel = !eventLabel.equals(Constants.STRING_EMPTY);
                            String eventCaption = Constants.STRING_EMPTY;
                            int eventIcon = 0;
                            String eventEmoji = "📆";

                            if (eventType.equals(typeBirthday) || (nonemptyEventLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel.toLowerCase()).find())) {

                                eventCaption = getResources().getString(R.string.event_type_birthday);
                                eventIcon = R.drawable.ic_event_birthday; //https://icons8.com/icon/21460/birthday
                                eventEmoji = "🎂";

                            } else if (eventType.equals(typeAnniversary) || (nonemptyEventLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel.toLowerCase()).find())) {

                                eventCaption = getResources().getString(R.string.event_type_anniversary);
                                eventIcon = R.drawable.ic_event_wedding; //https://www.flaticon.com/free-icon/wedding-rings_224802
                                eventEmoji = "💑";

                            } else if (eventType.equals(typeOther)) {

                                eventCaption = getResources().getString(R.string.event_type_other);
                                eventIcon = R.drawable.ic_event_custom1; //https://icons8.com/icon/set/event/office
                                eventEmoji = "🗓️";

                            } else if (nonemptyEventLabel) {

                                if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent1_caption;
                                    eventIcon = R.drawable.ic_event_custom1;
                                    eventEmoji = "🗓️";
                                    if (!preferences_customevent1_useyear && !eventDate.substring(0, 2).equals(Constants.STRING_3MINUS)) { //Если год не нужен, а он есть в событии
                                        eventDate = Constants.STRING_3MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else  if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent2_caption;
                                    eventIcon = R.drawable.ic_event_custom2;
                                    eventEmoji = "🔔";
                                    if (!preferences_customevent2_useyear && !eventDate.substring(0, 2).equals(Constants.STRING_3MINUS)) { //Если год не нужен, а он есть в событии
                                        eventDate = Constants.STRING_3MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent3_caption;
                                    eventIcon = R.drawable.ic_event_custom3;
                                    eventEmoji = "⏰";
                                    if (!preferences_customevent3_useyear && !eventDate.substring(0, 2).equals(Constants.STRING_3MINUS)) { //Если год не нужен, а он есть в событии
                                        eventDate = Constants.STRING_3MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent4_caption;
                                    eventIcon = R.drawable.ic_event_custom4;
                                    eventEmoji = "❤️";
                                    if (!preferences_customevent4_useyear && !eventDate.substring(0, 2).equals(Constants.STRING_3MINUS)) { //Если год не нужен, а он есть в событии
                                        eventDate = Constants.STRING_3MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent5_caption;
                                    eventIcon = R.drawable.ic_event_custom5;
                                    eventEmoji = "🎁";
                                    if (!preferences_customevent5_useyear && !eventDate.substring(0, 2).equals(Constants.STRING_3MINUS)) { //Если год не нужен, а он есть в событии
                                        eventDate = Constants.STRING_3MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = getResources().getString(R.string.event_type_nameday);
                                    eventIcon = R.drawable.ic_event_other;
                                    eventEmoji = "🎈";

                                } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = getResources().getString(R.string.event_type_crowning);
                                    eventIcon = R.drawable.ic_event_crowning; //https://iconscout.com/icon/wedding-destination-romance-building-emoj-symbol
                                    eventEmoji = "💒";

                                } else if (preferences_death_labels != null && preferences_death_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = getResources().getString(R.string.event_type_death);
                                    eventIcon = R.drawable.ic_event_death;
                                    eventEmoji = "⚰️";
                                    set_events_deaths.add(cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID))); //Запоминаем событие контакта

                                }

                            }
                            if (nonemptyEventLabel && eventCaption.equals(Constants.STRING_EMPTY)) eventCaption = eventLabel;

                            if (EventTypes.contains(eventType)) {

                                String eventKey_next = contactName.concat(STRING_COMMA).concat(eventType);

                                //Наименование события в ключе только для пользовательских событий
                                if (!eventType.equals(typeBirthday) && !eventType.equals(typeAnniversary) && !eventType.equals(typeOther))
                                    eventKey_next = eventKey_next.concat(STRING_COMMA).concat(eventLabel);

                                String newEventDate = accountType.concat(Constants.STRING_COLON_SPACE).concat(eventDate);

                                if (!eventKey_next.equalsIgnoreCase(eventKey)) { //Начало данных нового контакта

                                    if (!userData.isEmpty()) { // Уже есть накопленные данные. Нужно сохранить всё, что накопили и обнулить UserData
                                        dataRow = new StringBuilder();
                                        int rNum = 0;
                                        for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                                            rNum++;
                                            if (rNum == 1) {
                                                dataRow.append(entry.getValue());
                                            } else {
                                                dataRow.append(Constants.STRING_2HASH);
                                                dataRow.append(entry.getValue());
                                            }
                                        }
                                        dataList.add(dataRow.toString());
                                        userData.clear();
                                    }

                                    String contactID = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID));

                                    eventKey = eventKey_next;

                                    userData.put(Position_eventDate_sorted, STRING_SPACE);
                                    userData.put(Position_fio, contactName.replace(Constants.STRING_COMMA_SPACE, STRING_SPACE));
                                    userData.put(Position_contact_id, contactID);
                                    userData.put(Position_photo_uri, cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_PHOTO_URI)));
                                    userData.put(Position_eventDate, STRING_SPACE); //Полная дата события
                                    userData.put(Position_eventDateText, STRING_SPACE); //Дата для отображения
                                    userData.put(Position_eventDistance, STRING_SPACE); //Дней до даты
                                    userData.put(Position_eventDistanceText, STRING_SPACE); //Через сколько событие и какой будет день недели
                                    userData.put(Position_age, STRING_SPACE); //Возраст
                                    userData.put(Position_age_caption, STRING_SPACE); //Подпись для возраста
                                    userData.put(Position_eventCaption, eventCaption); //Наименование события
                                    //подпорка: почему-то для одиноких skype событий в eventLabel находится дата события
                                    userData.put(Position_eventLabel, !eventLabel.equals(eventCaption) & !newEventDate.contains(eventLabel) ? eventLabel : Constants.STRING_EMPTY); //Заголовок пользовательского события
                                    userData.put(Position_eventType, eventType); //Тип события

                                    userData.put(Position_organization, orgMap.containsKey(contactID) ? orgMap.get(contactID) : Constants.STRING_EMPTY);
                                    userData.put(Position_title, titleMap.containsKey(contactID) ? titleMap.get(contactID) : Constants.STRING_EMPTY);
                                    //userData.put(dataMap.get("note"), ""); //noteMap.containsKey(contactID) ? noteMap.get(contactID) : "");
                                    userData.put(Position_dates, newEventDate);
                                    userData.put(Position_eventIcon, Integer.toString(eventIcon));
                                    userData.put(Position_eventEmoji, eventEmoji);

                                } else { //Продолжаем добавлять даты контакта

                                    String existingDates = userData.get(Position_dates);
                                    if (existingDates != null && !existingDates.contains(newEventDate))
                                        userData.put(Position_dates, existingDates.concat(Constants.STRING_2TILDA).concat(newEventDate));

                                }
                            }
                        }
                    } while (cursor.moveToNext());

                    if (!userData.isEmpty()) { // Данные последнего контакта

                        dataRow = new StringBuilder();
                        int rNum = 0;
                        for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                            rNum++;
                            if (rNum == 1) {
                                dataRow.append(entry.getValue());
                            } else {
                                dataRow.append(Constants.STRING_2HASH);
                                dataRow.append(entry.getValue());
                            }
                        }
                        dataList.add(dataRow.toString());
                        userData.clear();

                    }
                }
                cursor.close();
            }

            dataArray = dataList.toArray(new String[0]);
            dataList.clear();
            cache.clear();
            statGetContacts = System.currentTimeMillis() - statCurrentModuleStart;
            return true;

        } catch (Exception e) {
            statGetContacts = System.currentTimeMillis() - statCurrentModuleStart;
            Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACTS_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    Bitmap getContactPhoto(@NonNull String event, boolean showPhotos) {

        Bitmap bm;

        try {

            if (event.equals(Constants.STRING_EMPTY)) return null;
            String[] singleEventArray = event.split(Constants.STRING_2HASH);
            String eventType = singleEventArray[Position_eventType];
            String eventCaptionCustom = eventType.equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM)) ? singleEventArray[Position_eventCaption].toLowerCase() : "#~#";
            boolean isDeath = preferences_death_labels != null && preferences_death_labels.reset(eventCaptionCustom.toLowerCase()).find();

            if (showPhotos && !singleEventArray[Position_photo_uri].equalsIgnoreCase(Constants.STRING_NULL)) {
                //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contact_id]);
                InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri, true);
                BufferedInputStream buf = new BufferedInputStream(photo_stream);
                bm = BitmapFactory.decodeStream(buf);
                buf.close();
                photo_stream.close();


            } else {

                //случайное фото с соответствиии с возрастом и полом

                Person person = new Person(context, event);
                int idPhoto = R.drawable.photo_man01;
                int growAge = 16;

                if (person.getGender() == 1 && (person.Age >= 0 && person.Age < growAge) && !isDeath) {
                    idPhoto = R.drawable.photo_boy01;
                } else if (person.getGender() == 2 && (person.Age >= 0 && person.Age < growAge) && !isDeath) {
                    idPhoto = R.drawable.photo_girl01;
                } else if (person.getGender() == 2) {
                    idPhoto = R.drawable.photo_woman01;
                }

                bm = BitmapFactory.decodeResource(getResources(), idPhoto);
            }


            if (set_events_deaths != null &&
                    (preferences_list_sad_photo == 2 || (preferences_list_sad_photo == 1 && isDeath)) &&
                    set_events_deaths.contains(singleEventArray[Position_contact_id]))
            {
                //если контакт умер - выводить чёрную рамочку
                //https://stackoverflow.com/questions/3089991/how-to-draw-a-shape-or-bitmap-into-another-bitmap-java-android
                Bitmap bmOverlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(bm.getWidth() / 6 /*1 /getResources().getDisplayMetrics().density*/);
                canvas.drawBitmap(bm, new Matrix(), null);
                canvas.drawLine(bm.getWidth() + bm.getWidth() / 4, bm.getHeight() - bm.getHeight() / 2, bm.getWidth() - bm.getWidth() / 2, bm.getHeight() + bm.getHeight() / 4, paint);
                bm.recycle();
                return bmOverlay;
            } else {
                return bm;
            }

        } catch (Exception e) {
            Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACT_PHOTO_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    void computeDates() {
        //Вычисляем даты

        if (dataArray == null || dataArray.length == 0) return;

        long statCurrentModuleStart = System.currentTimeMillis();

        try {

            List<String> magicList = new ArrayList<>(); //Для 5k событий

            Locale locale_en = new Locale("en"); //Все даты Android хранит в этой локали, типа 11 Jan 1991
            Locale locale_ru = new Locale("ru"); //Skype хранит даты в той локале, которая указана в приложении Skype
            Locale locale_ukr = new Locale("uk_UA");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", locale_en);
            SimpleDateFormat skypedf = new SimpleDateFormat("dd MMM yyyy", locale_en);
            SimpleDateFormat sdfYear = new SimpleDateFormat("dd.MM.yyyy", locale_en);
            SimpleDateFormat sdfNoYear = new SimpleDateFormat("dd.MM", locale_en);
            SimpleDateFormat sdf_ru = new SimpleDateFormat("dd MMMMM yyyy г.", locale_ru);
            SimpleDateFormat sdf_ukr = new SimpleDateFormat("dd MMMMM yyyy г.", locale_ukr);

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis()); //new Date(System.currentTimeMillis());

            List<String> enabledTypes = new ArrayList<>();
            if (event_types_on[0]) enabledTypes.add(Integer.toString(event_types_id[0]));
            if (event_types_on[1]) enabledTypes.add(Integer.toString(event_types_id[1]));
            if (event_types_on[2]) enabledTypes.add(Integer.toString(event_types_id[2]));
            if (event_types_on[3]) enabledTypes.add(Integer.toString(event_types_id[3]));

            setLocale(false);
            //Resources resources = context.getResources(); TEST

            for (int i = 0; i < dataArray.length; i++) {
                int dayDiff = -1;
                boolean isYear = false;
                Date eventDate = null; //оригинальная дата события
                Date BDay = null; //следующая дата события
                int Age = 0;

                String[] singleEventArray = dataArray[i].split(Constants.STRING_2HASH);
                String[] dayArray = singleEventArray[Position_dates].split(Constants.STRING_2TILDA);
                String eventCaption = singleEventArray[Position_eventCaption];

                //перебираем все даты и находим максимальную
                for (String dayValue : dayArray) {
                    String accountType = dayValue.substring(0, dayValue.indexOf(Constants.STRING_COLON_SPACE));
                    String storedDate = dayValue.substring(dayValue.indexOf(Constants.STRING_COLON_SPACE) + Constants.STRING_COLON_SPACE.length());
                    Date storedDate_Date = null;
                    boolean storedDate_isYear = false;

                    if (accountType.toLowerCase().contains(getResources().getString(R.string.account_skype))) {

                        storedDate_isYear = true;
                        try {
                            storedDate_Date = skypedf.parse(storedDate);
                        } catch (ParseException e) {
                            try {
                                storedDate_Date = sdf_ru.parse(storedDate);
                            } catch (ParseException e2) {
                                try {
                                    storedDate_Date = sdf_ukr.parse(storedDate);
                                } catch (ParseException e3) {
                                    //Не получилось распознать
                                }
                            }
                        }

                    } else if (accountType.equalsIgnoreCase(getResources().getString(R.string.account_vk))) {

                        if (storedDate.substring(0, 5).equals("0000-")) { //Нет года, формат 0000-mm-dd

                            try {
                                BDay = sdf.parse(now.get(YEAR) + "-" + storedDate.substring(5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (BDay != null) {
                                int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);
                                storedDate_Date = BDay;
                                /*int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) {
                                    dayDiff = 365 + dayDiff_tmp;
                                } else {
                                    dayDiff = dayDiff_tmp;
                                }*/
                            }

                        } else {

                            storedDate_isYear = true;
                            try {
                                storedDate_Date = sdf.parse(storedDate);
                            } catch (ParseException e) {
                                try {
                                    storedDate_Date = skypedf.parse(storedDate);
                                } catch (ParseException e2) {
                                    //Не получилось распознать
                                }
                            }
                        }

                    } else {
                        //Стандартные аккаунты. Если есть год пробуем сначала yyyy-MM-dd, потом dd MM yyyy, потом известные локали
                        //com.google
                        //com.xiaomi
                        //vnd.sec.contact.phone
                        //asus.local.phone
                        //com.google.android.gm.exchange
                        //com.lotus.sync.notes
                        //com.whatsapp

                        if (storedDate.substring(0, 2).equals(Constants.STRING_3MINUS) || //Нет года, формат --MM-dd
                                storedDate.substring(0, 5).equals("0000-") || //Нет года, формат 0000-MM-dd
                                storedDate.substring(0, 5).equals("1604-") || //Нет года, формат 1604-MM-dd
                                (!eventCaption.equals(Constants.STRING_EMPTY) && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventCaption.toLowerCase()).find()) //Именины считаем без года
                        ) {

                            try {
                                BDay = sdf.parse(now.get(YEAR) + "-" + storedDate.substring(storedDate.substring(0, 2).equals(Constants.STRING_3MINUS) ? 2 : 5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (BDay != null) {
                                int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);
                                storedDate_Date = BDay;
                                /*
                                int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) {
                                    dayDiff = 365 + dayDiff_tmp;
                                } else {
                                    dayDiff = dayDiff_tmp;
                                }*/
                            }

                        } else { //Обычный формат yyyy-MM-dd

                            storedDate_isYear = true;
                            try {
                                storedDate_Date = sdf.parse(storedDate);
                            } catch (ParseException e) {
                                try {
                                    storedDate_Date = skypedf.parse(storedDate);
                                } catch (ParseException e2) {
                                    try {
                                        storedDate_Date = sdf_ru.parse(storedDate);
                                    } catch (ParseException e3) {
                                        //Не получилось распознать
                                    }
                                }
                            }
                        }

                    }

                    if (storedDate_Date != null) {
                        if (eventDate == null) {
                            eventDate = storedDate_Date;
                            isYear = storedDate_isYear;
                        } else if ((storedDate_isYear & !isYear) || countDaysDiff(eventDate, storedDate_Date) > 0) { //Если у пользователя несколько дат, берём наименьший возраст todo: можно вынести в настройку - в какую сторону округлять
                            eventDate = storedDate_Date;
                            isYear = storedDate_isYear;
                        }
                    }
                }

                if (eventDate != null) {

                    if (isYear) { //в eventDate - оригинальное событие

                        Calendar cal = from(eventDate);
                        try {

                            BDay = sdf.parse(now.get(YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
                            int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                            if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);

                        } catch (ParseException e) {
                            //dayDiff = -1; //Не получилось распознать
                        }

                    }

                    dayDiff = countDaysDiff(currentDay, BDay);
                    Age = countYearsDiff(eventDate, BDay); //Считаем, сколько будет лет

                }

                if (dayDiff == -1) {

                    dataArray[i] = Constants.STRING_EMPTY;

                } else {

                    singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + dayDiff).substring((Constants.STRING_00 + dayDiff).length() - 3); //Для сортировки
                    singleEventArray[Position_eventDate] = sdfYear.format(BDay); //следующая дата события
                    if (isYear) { //Дата с годом
                        singleEventArray[Position_eventDateText] = sdfYear.format(eventDate); //оригинальное событие
                    } else { //Дата без года
                        singleEventArray[Position_eventDateText] = sdfNoYear.format(eventDate); //оригинальное событие без года
                    }

                    //Если событие в ближайшие 3 дня, то в eventDistance будет <число дней до события>, иначе: "Дней до <тип события>: " +  <число дней до события> + <день недели>
                    singleEventArray[Position_eventDistance] = Integer.toString(dayDiff);
                    singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDiff, BDay);

                    if (Age > 0) {
                        singleEventArray[Position_age] = Integer.toString(Age);
                        singleEventArray[Position_age_caption] = getAgeString(Age);
                        if (singleEventArray[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY))) {
                            String anCaption;
                            try {
                                anCaption = context.getString(getResources().getIdentifier("event_type_wedding_" + Age, "string", context.getPackageName()));
                            } catch (Resources.NotFoundException nfe) {
                                anCaption = null;
                            }
                            if (anCaption != null && !anCaption.equals(Constants.STRING_EMPTY) && !eventCaption.contains(STRING_PARENTHESIS_OPEN)) {
                                singleEventArray[Position_eventCaption] = eventCaption.concat(STRING_PARENTHESIS_OPEN).concat(anCaption).concat(")");
                            }
                        }
                    } else {
                        singleEventArray[Position_age] = "-1";
                        singleEventArray[Position_age_caption] = STRING_SPACE;
                    }
                    //если дату вычисляли только для 5K - не записываем её
                    if (enabledTypes.contains(singleEventArray[Position_eventType])) {
                        dataArray[i] = TextUtils.join(Constants.STRING_2HASH, singleEventArray);
                    } else {
                        dataArray[i] = Constants.STRING_EMPTY;
                    }

                    //Вычисляем 5K даты
                    if (event_types_on[4] && eventDate != null && Age > 0 && singleEventArray[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {
                        //&& singleEventArray[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {

                        //todo: подумать: надо ли считать 5K для смертей и.т.п.?
                        int days = countDaysDiff(eventDate, currentDay);
                        int k = (days + 365) / 5000;
                        int mdays = (days + 365) % 5000;

                        if (mdays >= 0 && mdays <= 365) {
                            //Формируем новую запись
                            Calendar cal5K = Calendar.getInstance();
                            int magicDayDistance = 365 - mdays;
                            cal5K.add(Calendar.DATE, magicDayDistance);

                            //String[] newDataArray = dataArray[i].split(STRING_2HASH);
                            singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + magicDayDistance).substring((Constants.STRING_00 + magicDayDistance).length() - 3);
                            singleEventArray[Position_eventType] = Integer.toString(event_types_id[4]);
                            singleEventArray[Position_eventCaption] = "5K+";
                            singleEventArray[Position_eventLabel] = sdfYear.format(cal5K.getTime());
                            //для выдачи даты юбилея,а не первоначального события: sdfYear.format(sdf.parse(cal5K.get(YEAR) + "-" + (cal5K.get(Calendar.MONTH) + 1) + "-" + cal5K.get(Calendar.DAY_OF_MONTH)));
                            singleEventArray[Position_eventDate] = sdfYear.format(cal5K.getTime());
                            singleEventArray[Position_eventDateText] = sdfYear.format(eventDate);
                            singleEventArray[Position_age] = Integer.toString(Age);
                            singleEventArray[Position_age_caption] = 5 * k + "K";
                            singleEventArray[Position_eventDistance] = Integer.toString(magicDayDistance);
                            singleEventArray[Position_eventDistanceText] = getEventDistanceText(magicDayDistance, cal5K.getTime());
                            singleEventArray[Position_eventIcon] = Integer.toString(R.drawable.ic_event_medal); //https://www.flaticon.com/free-icon/medal_610333
                            singleEventArray[Position_eventEmoji] = "🏆";

                            magicList.add(TextUtils.join(Constants.STRING_2HASH, singleEventArray));
                        }

                    }
                }

            }

            //Удаляем пустые
            List<String> tmpList = new ArrayList<>();
            for (String s : dataArray) {
                if (s != null && s.length() > 0) {
                    tmpList.add(s);
                }
            }

            //Добавляем 5k+
            if (magicList.size() > 0) {
                tmpList.addAll(magicList);
                magicList.clear();
            }

            dataArray = tmpList.toArray(new String[0]); //tmpList.size()
            tmpList.clear();

            //todo: добавить события нескольких предыдущих дней или несколько событий (предыдущие события хранить отдельным списком)

            //Сортируем
            Arrays.sort(dataArray);

            //todo: добавить уведомления https://stackoverflow.com/questions/33364368/android-system-notification-limit-per-app/33365915

            statLastComputeDates = System.currentTimeMillis();
            statComputeDates = statLastComputeDates - statCurrentModuleStart;

        } catch (Exception e) {
            statLastComputeDates = System.currentTimeMillis();
            statComputeDates = statLastComputeDates - statCurrentModuleStart;
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_COMPUTE_DATES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private String getAgeString(int age) {

        String Age_tmp = Integer.toString(age);
        String Age_end = Age_tmp.substring(Age_tmp.length() - 1);

        if (age == 1) { //Единственное число
            return Age_tmp + getResources().getString(R.string.msg_after_age_prefix_1);
        } else if (age > 4 && age < 21) {
            return Age_tmp + getResources().getString(R.string.msg_after_age_prefix_4_21);
        } else if (Age_end.equals("1")) { //Если заканчивается на 1, но не между 5-20
            return Age_tmp + getResources().getString(R.string.msg_after_age_prefix_1_);
        } else if (Age_end.equals("2") || Age_end.equals("3") || Age_end.equals("4")) { //Если заканчивается на 2, 3, 4
            return Age_tmp + getResources().getString(R.string.msg_after_age_prefix_2_3_4);
        } else { //Всё остальное
            return Age_tmp + getResources().getString(R.string.msg_after_age_prefix_4_21);
        }
    }

    private String getEventDistanceText(int dayDiff, @NonNull Date eventDate){
        //Если событие в ближайшие 3 дня, то вернёт "сегодня", "завтра", "послезавтра", если позже, то "через X дней в " + <день недели>

        StringBuilder eventDistance = new StringBuilder();
        try {
            String dayDiff_tmp = Integer.toString(dayDiff);
            String dayDiff_end = dayDiff_tmp.substring(dayDiff_tmp.length() - 1);
            Calendar c1 = Calendar.getInstance();
            c1.setTime(eventDate);

            if (dayDiff == 0) { //Сегодня
                eventDistance.append(getResources().getString(R.string.msg_today));
            } else if (dayDiff == 1) { //Завтра
                eventDistance.append(getResources().getString(R.string.msg_tomorrow));
            } else if (dayDiff == 2) { //Послезавтра
                eventDistance.append(getResources().getString(R.string.msg_day_after_tomorrow));
            } else {
                boolean isEnd234 = dayDiff_end.equals("2") || dayDiff_end.equals("3") || dayDiff_end.equals("4");
                if (dayDiff > 0) { //Подальше вперёд
                    eventDistance.append(getResources().getString(R.string.msg_before_event_prefix)).append(dayDiff);
                    if (dayDiff > 4 && dayDiff < 21) {
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_4_21));
                    } else if (dayDiff_end.equals("1")) { //Если заканчивается на 1, но не между 5-20
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_1_));
                    } else if (isEnd234) { //Если заканчивается на 2, 3, 4
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_2_3_4));
                    } else {
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_4_21));
                    }
                    eventDistance.append(getResources().getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1]);
                } else if (dayDiff == -1) { //Вчера
                    eventDistance.append(getResources().getString(R.string.msg_yesterday));
                } else if (dayDiff == -2) { //Позавчера
                    eventDistance.append(getResources().getString(R.string.msg_beforeyesterday));
                } else if (dayDiff < 0) { //Подальше
                    eventDistance.append(getResources().getString(R.string.msg_after_event_prefix)).append(-dayDiff);
                    if (dayDiff > 4 && dayDiff < 21) {
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_4_21));
                    } else if (dayDiff_end.equals("1")) { //Если заканчивается на 1, но не между 5-20
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_1_));
                    } else if (isEnd234) { //Если заканчивается на 2, 3, 4
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_2_3_4));
                    } else {
                        eventDistance.append(getResources().getString(R.string.msg_before_event_prefix_4_21));
                    }
                    eventDistance.append(getResources().getString(R.string.msg_after_event_postfix)).append(getResources().getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_EVENT_DISTANCE_TEXT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return  eventDistance.toString();
    }

    List<String> insertPreviousEvents(@NonNull List<String> dataList, @NonNull String params) {
        //todo: Добавляем в начало списка предыдущие события, согласно параметров

        if (params.equals(Constants.STRING_EMPTY) || dataList.isEmpty()) return dataList;

        try {

            //Собираем события
            int params_days = 365;
            int params_events = 1000;
            switch (params) {
                case "1d":
                    params_days = 1;
                    break;
                case "2d":
                    params_days = 2;
                    break;
                case "3d":
                    params_days = 3;
                    break;
                case "3d1e":
                    params_days = 3;
                    params_events = 1;
                    break;
                case "3d2e":
                    params_days = 3;
                    params_events = 2;
                    break;
                case "1e":
                    params_events = 1;
                    break;
                case "2e":
                    params_events = 2;
                    break;
                case "3e":
                    params_events = 3;
                    break;
            }

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());
            Locale locale_en = new Locale("en");
            SimpleDateFormat sdfYear = new SimpleDateFormat("dd.MM.yyyy", locale_en);

            List<String> newList = new ArrayList<>();
            preferences_list_prev_events_found = 0;
            for (int i = dataList.size() - 1; i >= 0 && preferences_list_prev_events_found < params_events; i--) {
                String li = dataList.get(i);
                if (params_events > 0) {
                    String[] singleEventArray = li.split(Constants.STRING_2HASH);
                    if (!singleEventArray[Position_eventType].equals(Integer.toString(event_types_id[4]))) { //пропускаем 5K+
                        if (params_days == 365) { //нет ограничения по дням
                            newList.add(li);
                            preferences_list_prev_events_found++;
                        } else {
                            Date eventDate = null;
                            try {
                                eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                                eventDate = addYear(eventDate, -1);
                            } catch (Exception e) {
                                //
                            }

                            if (eventDate != null) {
                                //Toast.makeText(context, eventDate.toString() + ": " + (-countDaysDiff(currentDay, eventDate)) + " <= " + params_days, Toast.LENGTH_LONG).show();
                                if (-countDaysDiff(currentDay, eventDate) <= params_days) {
                                    newList.add(li);
                                    preferences_list_prev_events_found++;
                                } else {
                                    i = 0;
                                }
                            }
                        }
                    }
                }
            }

            //Подправляем надписи и дату
            if (!newList.isEmpty()) {

                for (String li : newList) {
                    String[] singleEventArray = li.split(Constants.STRING_2HASH);
                    Date eventDate = null;
                    try {
                        eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                    } catch (Exception e) {
                        //
                    }

                    if (eventDate != null) {

                        eventDate = addYear(eventDate, -1);
                        singleEventArray[Position_eventDate] = sdfYear.format(eventDate);
                        int dayDistance = countDaysDiff(currentDay, eventDate);
                        singleEventArray[Position_eventDistance] = Integer.toString(dayDistance);
                        singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDistance, eventDate);

                        int Age = 0;
                        try {
                            Age = Integer.parseInt(singleEventArray[Position_age]);
                        } catch (NumberFormatException e) {
                            //
                        }
                        if (Age > 1) {
                            Age--;
                            singleEventArray[Position_age] = Integer.toString(Age);
                            singleEventArray[Position_age_caption] = getAgeString(Age);

                            if (singleEventArray[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY))) {
                                String anCaption;
                                try {
                                    anCaption = context.getString(getResources().getIdentifier("event_type_wedding_" + Age, "string", context.getPackageName()));
                                } catch (Resources.NotFoundException nfe) {
                                    anCaption = null;
                                }
                                String eventCaption = getResources().getString(R.string.event_type_anniversary);
                                if (anCaption != null && !anCaption.equals(Constants.STRING_EMPTY)) {
                                    singleEventArray[Position_eventCaption] = eventCaption.concat(STRING_PARENTHESIS_OPEN).concat(anCaption).concat(")");
                                } else {
                                    singleEventArray[Position_eventCaption] = eventCaption;
                                }
                            }
                        } else { //Сейчас идёт первый год жизни

                            singleEventArray[Position_age] = "-1";
                            singleEventArray[Position_age_caption] = STRING_SPACE;

                        }

                        dataList.add(0, TextUtils.join(Constants.STRING_2HASH, singleEventArray));
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_INSERT_PREVIOUS_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return dataList;
    }

    void updateWidgets() {

        if (context == null) return;

        //Посылаем сообщения на обновление виджетов
        // https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver

        int[] ids;
        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget2x2.class));
        if (ids != null && ids.length > 0 && ids[0] != 0) {
            Widget2x2 myWidget = new Widget2x2();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), ids);
        }

        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget5x1.class));
        if (ids != null && ids.length > 0 && ids[0] != 0) {
            Widget5x1 myWidget = new Widget5x1();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), ids);
        }

        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget4x1.class));
        if (ids != null && ids.length > 0 && ids[0] != 0) {
            Widget4x1 myWidget = new Widget4x1();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), ids);
        }

    }

    void initNotificationChannel(StringBuilder log) {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //для Android 8+

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

                //находим канал. если канала нет или рингтон там другой - пересоздаём канал
                String channel_id = Integer.toString(preferences_notification_channel_id);
                NotificationChannel channel = notificationManager.getNotificationChannel(channel_id);

                if (preferences_notifications_days >= 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                    //https://developer.android.com/training/notify-user/channels.html
                    //After you create a notification channel, you cannot change the notification behaviors—the user has complete control at that point. Though you can still change a channel's name and description
                    //https://stackoverflow.com/questions/46234254/android-oreo-notification-keep-making-sound-even-if-i-do-not-set-sound-on-older

                    if (channel == null || !channel.getSound().toString().equals(preferences_notifications_ringtone)) {

                        notificationManager.deleteNotificationChannel(channel_id);
                        if (preferences_debug_on) log.append(Constants.MSG_DELETED_CHANNEL_).append(channel_id).append(STRING_EOF);

                        Random r = new Random();
                        preferences_notification_channel_id = r.nextInt(1000);
                        channel_id = Integer.toString(preferences_notification_channel_id);

                        channel = new NotificationChannel(channel_id, context.getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription(context.getString(R.string.pref_Notifications_Notification_Channel_Description));
                        channel.setSound(Uri.parse(preferences_notifications_ringtone), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                        if (preferences_debug_on) log.append(Constants.MSG_RINGTONE).append(Uri.parse(preferences_notifications_ringtone)).append(STRING_EOF);
                        channel.enableVibration(true);

                        notificationManager.createNotificationChannel(channel);
                        if (preferences_debug_on) log.append(Constants.MSG_CREATED_CHANNEL_).append(preferences_notification_channel_id).append(STRING_EOF);

                        setPreferences();
                    }

                } else if (channel != null) {

                    notificationManager.deleteNotificationChannel(channel_id);
                    if (preferences_debug_on) log.append(Constants.MSG_DELETED_CHANNEL_).append(channel_id).append(STRING_EOF);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_INIT_NOTIFICATION_CHANNEL_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void initBootReceiver(StringBuilder log) {

        try {

            PackageManager pm = context.getPackageManager();
            ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);

            if (preferences_notifications_days >= 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                //To enable Boot Receiver class
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    if (preferences_debug_on) log.append(Constants.MSG_NOTIFICATIONS_WERE_ENABLED).append(STRING_EOF);
                }

            } else { //Disable Daily Notifications
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    if (preferences_debug_on) log.append(Constants.MSG_NOTIFICATIONS_WERE_DISABLED).append(STRING_EOF);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_INIT_BOOT_RECEIVER_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void initNotifications(StringBuilder log) {

        try {

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (preferences_notifications_days >= 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, preferences_notifications_alarm_hour);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }

                    if (preferences_debug_on) {
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                        log.append(Constants.MSG_NEXT_NOTIFICATION).append(sdf.format(calendar.getTime())).append(STRING_EOF);
                    }
                }

            } else { //Disable Daily Notifications
                if (PendingIntent.getBroadcast(context, 0, alarmIntent, 0) != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_INIT_NOTIFICATIONS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void showNotifications(boolean forceNoEventsMessage, String channelId) {

        try {

            int notifications_days;
            if (forceNoEventsMessage && preferences_notifications_days < 0) {
                notifications_days = preferences_notifications_days_test;
            } else {
                notifications_days = preferences_notifications_days;
            }
            if (notifications_days < 0) {
                Toast.makeText(context, "!" + notifications_days, Toast.LENGTH_LONG).show();
                return;
            }

            setLocale(true);

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());
            Locale locale_en = new Locale("en");
            SimpleDateFormat sdfYear = new SimpleDateFormat("dd.MM.yyyy", locale_en);
            SimpleDateFormat sdfDDMM = new SimpleDateFormat("dd.MM", locale_en);

            List<String> listNotify = new ArrayList<>();
            for (String event: dataArray) {
                String[] singleEventArray = event.split(Constants.STRING_2HASH);
                if (!checkIsHiddenEvents() || !checkIsHiddenEvent(singleEventArray[Position_contact_id] + Constants.STRING_2HASH + singleEventArray[Position_eventType])) {
                    Date eventDate = null;
                    try {
                        eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                    } catch (Exception e) {
                        //
                    }

                    if (eventDate != null) {
                        //https://stackoverflow.com/questions/33364368/android-system-notification-limit-per-app
                        if (listNotify.size() >= 50 || countDaysDiff(currentDay, eventDate) > notifications_days)
                            break;
                        listNotify.add(event);
                    }
                }
            }
            if (listNotify.size() == 0 && !forceNoEventsMessage) return;

            String[] dataNotify = listNotify.toArray(new String[0]);
            StringBuilder textBig;
            if (dataNotify.length == 0 || //Общее уведомление
                    preferences_notifications_type == 0 ||
                    dataNotify.length > 3 && preferences_notifications_type == 2 ||
                    dataNotify.length > 4 && preferences_notifications_type == 3
            ) {

                textBig = new StringBuilder();
                String textSmall;
                if (dataNotify.length > 0) {
                    textSmall = (notifications_days == 0 ? context.getString(R.string.msg_notifications_today) : context.getString(R.string.msg_notifications_soon)) + dataNotify.length;
                    textBig.append(textSmall).append(":\n");
                    for (String event : dataNotify) {
                        String[] singleEventArray = event.split(Constants.STRING_2HASH);
                        Date eventDate = null;
                        String eventDay = null;
                        try {
                            eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                            eventDay = sdfDDMM.format(eventDate);
                        } catch (Exception e) {
                            //
                        }

                        if (eventDate != null) {
                            textBig.append(singleEventArray[ContactsEvents.Position_eventEmoji])
                                    .append(STRING_SPACE)
                                    .append(eventDay).append(STRING_SPACE)
                                    .append(singleEventArray[Position_fio]);
                            if (!singleEventArray[ContactsEvents.Position_age_caption].trim().equals(Constants.STRING_EMPTY))
                                textBig.append(": ").append(singleEventArray[ContactsEvents.Position_age_caption]);
                            textBig.append(Constants.STRING_EOF);
                        }
                    }
                } else {
                    textSmall = notifications_days == 0 ? context.getString(R.string.msg_notifications_today_no_events) : context.getString(R.string.msg_notifications_soon_no_events) + STRING_PARENTHESIS_OPEN + notifications_days + ")";
                }

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                        .setColor(this.getResources().getColor(R.color.dark_green))
                        .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                        .setContentText(textSmall)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig)) //Ограничение 5120 символов https://stackoverflow.com/questions/27124887/whats-the-max-size-of-a-bigtextstyle-notification
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    builder.setSound(Uri.parse(preferences_notifications_ringtone));
                }

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(defaultNotificationID, builder.build());

            } else { //Несколько отдельных уведомлений
                Random r = new Random();
                for (int i = dataNotify.length - 1; i >= 0; i--) {
                    String[] singleEventArray = dataNotify[i].split(Constants.STRING_2HASH);
                    Date eventDate = null;
                    String eventDay = null;
                    try {
                        eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                        eventDay = sdfDDMM.format(eventDate);
                    } catch (Exception e) {
                        //
                    }

                    if (eventDate != null) {
                        if (countDaysDiff(currentDay, eventDate) <= notifications_days) {
                            textBig = new StringBuilder();
                            textBig.append(singleEventArray[ContactsEvents.Position_eventEmoji])
                                    .append(STRING_SPACE)
                                    .append(eventDay)
                                    .append(STRING_SPACE)
                                    .append(singleEventArray[Position_fio]);
                            if (!singleEventArray[ContactsEvents.Position_age_caption].trim().equals(Constants.STRING_EMPTY))
                                textBig.append(": ").append(singleEventArray[ContactsEvents.Position_age_caption]);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contact_id]);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                    .setColor(this.getResources().getColor(R.color.dark_green))
                                    .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                                    .setContentText(textBig)
                                    .setContentTitle(singleEventArray[ContactsEvents.Position_eventDistanceText])
                                    .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig))
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true);

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                builder.setSound(Uri.parse(preferences_notifications_ringtone));
                            }

                            builder.setLargeIcon(getContactPhoto(dataNotify[i], true));

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            notificationManager.notify(defaultNotificationID + r.nextInt(100), builder.build());

                        } else {
                            break;
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_SHOW_NOTIFICATIONS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    boolean checkIsHiddenEvents() {

        try {

            return !preferences_hiddenEvents.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_CHECK_IS_HIDDEN_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return true;
        }
    }

    int getHiddenEventsCount() {

        try {

            return preferences_hiddenEvents.size();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_HIDDEN_EVENTS_COUNT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    boolean checkIsHiddenEvent(@NonNull String key) {

        try {

            return preferences_hiddenEvents.contains(key);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_CHECK_IS_HIDDEN_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    boolean setHiddenEvent(@NonNull String key) {

        try {

            if (!preferences_hiddenEvents.add(key)) return false;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, "Hided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_HIDDEN_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

    boolean unsetHiddenEvent(@NonNull String key) {

        try {

            if (!checkIsHiddenEvent(key)) return false;

            if (!preferences_hiddenEvents.remove(key)) return false;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);

            //Если удалили последнее - скидываем режим на стандартный
            if (preferences_hiddenEvents.isEmpty()) {
                preferences_events_scope = Constants.pref_Events_Scope_NotHidden;
                editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_events_scope);
            }

            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, "Unhided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_HIDDEN_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

}
