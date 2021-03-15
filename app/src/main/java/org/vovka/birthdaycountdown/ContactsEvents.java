/*
 * *
 *  * Created by Vladimir Belov on 15.03.21 8:51
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 14.03.21 16:56
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
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.text.TextUtils.isEmpty;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import static org.vovka.birthdaycountdown.Constants.ACTION_DIAL;
import static org.vovka.birthdaycountdown.Constants.ACTION_HIDE;
import static org.vovka.birthdaycountdown.Constants.ACTION_SILENT;
import static org.vovka.birthdaycountdown.Constants.ACTION_SNOOZE;
import static org.vovka.birthdaycountdown.Constants.CONTACTS_EVENTS_CLEAR_UNEXISTING_HIDDEN_EVENTS_ERROR;
import static org.vovka.birthdaycountdown.Constants.CONTACTS_EVENTS_CLEAR_UNEXISTING_SILENCED_EVENTS_ERROR;
import static org.vovka.birthdaycountdown.Constants.ColumnNames_ACCOUNT_NAME;
import static org.vovka.birthdaycountdown.Constants.ColumnNames_ACCOUNT_TYPE;
import static org.vovka.birthdaycountdown.Constants.EXTRA_NOTIFICATION_DATA;
import static org.vovka.birthdaycountdown.Constants.EXTRA_NOTIFICATION_ID;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_BROWN;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_RED;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_YELLOW;
import static org.vovka.birthdaycountdown.Constants.RULE_TAG_NAME;
import static org.vovka.birthdaycountdown.Constants.STRING_0;
import static org.vovka.birthdaycountdown.Constants.STRING_1;
import static org.vovka.birthdaycountdown.Constants.STRING_2;
import static org.vovka.birthdaycountdown.Constants.STRING_2HASH;
import static org.vovka.birthdaycountdown.Constants.STRING_2MINUS;
import static org.vovka.birthdaycountdown.Constants.STRING_3;
import static org.vovka.birthdaycountdown.Constants.STRING_7;
import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_EOF;
import static org.vovka.birthdaycountdown.Constants.STRING_MINUS;
import static org.vovka.birthdaycountdown.Constants.STRING_NULL;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_CLOSE;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_OPEN;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_START;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_STORAGE_CALENDAR;
import static org.vovka.birthdaycountdown.Constants.STRING_STORAGE_CONTACTS;
import static org.vovka.birthdaycountdown.Constants.Type_5K;
import static org.vovka.birthdaycountdown.Constants.Type_Anniversary;
import static org.vovka.birthdaycountdown.Constants.Type_BirthDay;
import static org.vovka.birthdaycountdown.Constants.Type_CalendarEvent;
import static org.vovka.birthdaycountdown.Constants.Type_Crowning;
import static org.vovka.birthdaycountdown.Constants.Type_Custom;
import static org.vovka.birthdaycountdown.Constants.Type_Custom1;
import static org.vovka.birthdaycountdown.Constants.Type_Custom2;
import static org.vovka.birthdaycountdown.Constants.Type_Custom3;
import static org.vovka.birthdaycountdown.Constants.Type_Custom4;
import static org.vovka.birthdaycountdown.Constants.Type_Custom5;
import static org.vovka.birthdaycountdown.Constants.Type_Death;
import static org.vovka.birthdaycountdown.Constants.Type_NameDay;
import static org.vovka.birthdaycountdown.Constants.Type_Other;
import static org.vovka.birthdaycountdown.Constants.defaultNotificationID;

class ContactsEvents {
    private static final ContactsEvents ourInstance = new ContactsEvents();
    private static final String MSG_UPDATED_PREFERENCES = "Updated preferences: ";

    static ContactsEvents getInstance() {
        return ourInstance;
    }

    private ContactsEvents() {}

    void setContext(Context con) {
        this.context = con;
        this.resources = con.getResources();
    }

    @NonNull private Resources getResources() {return resources != null ? resources : (resources = context.getResources());}

    //Константы
    final Set<String> prefs_EventTypes_Default = new HashSet<String>() {{
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM));
    }};
    //final private boolean[] prefs_EventTypes_DefaultB = {true,true,true,true,false};

    static final String pref_List_EventInfo_Photo = "6";
    static final String pref_List_EventInfo_JobTitle = "1";
    static final String pref_List_EventInfo_Organization = "2";
    static final String pref_List_EventInfo_EventCaption = "3";
    static final String pref_List_EventInfo_FavoritesIcon = "5";
    static final String pref_List_EventInfo_ZodiacSign = "9";
    static final String pref_List_EventInfo_ZodiacYear = "10";

    final private Set<String> pref_List_Event_Info = new HashSet<String>() {{
        add(pref_List_EventInfo_Photo);
        add(pref_List_EventInfo_JobTitle);
        add(pref_List_EventInfo_Organization);
        add(pref_List_EventInfo_EventCaption);
        add(pref_List_EventInfo_FavoritesIcon);
    }};

    static final String pref_Widgets_EventInfo_Photo = "1";
    static final String pref_Widgets_EventInfo_EventIcon = "2";
    static final String pref_Widgets_EventInfo_FavoritesIcon = "3";
    static final String pref_Widgets_EventInfo_ZodiacSign = "4";
    static final String pref_Widgets_EventInfo_ZodiacYear = "5";
    static final String pref_Widgets_EventInfo_Border = "10";

    final private Set<String> pref_Widgets_EventInfo_Info = new HashSet<String>() {{
        add(pref_Widgets_EventInfo_Photo);
        add(pref_Widgets_EventInfo_EventIcon);
        add(pref_Widgets_EventInfo_FavoritesIcon);
        add(pref_Widgets_EventInfo_Border);
    }};

    static final int Position_eventDate_sorted = 0;
    static final int Position_personFullName = 1; //ИОФ
    static final int Position_personFullNameAlt = 2; //ФИО
    static final int Position_eventCaption = 3;
    static final int Position_eventLabel = 4;
    static final int Position_nickname = 5;
    static final int Position_dates = 6; //account_type: data1
    static final int Position_eventDate = 7;
    static final int Position_eventDateText = 8;
    static final int Position_eventDistance = 9;
    static final int Position_eventDistanceText = 10;
    static final int Position_age = 11;
    static final int Position_age_caption = 12;
    static final int Position_organization = 13;
    static final int Position_title = 14;
    static final int Position_photo_uri = 15;
    static final int Position_eventIcon = 16;
    static final int Position_eventEmoji = 17; //https://www.piliapp.com/emoji/list/
    static final int Position_starred = 18;
    static final int Position_age_current = 19;
    static final int Position_eventType = 20;
    static final int Position_eventSubType = 21;
    static final int Position_contactID = 22;
    static final int Position_eventID = 23;
    static final int Position_eventStorage = 24;
    static final int Position_zodiacSign = 25;
    static final int Position_zodiacYear = 26;

    static final HashMap<Integer, String> eventTypesIDs = new HashMap<Integer, String>() {{
        put(Type_BirthDay, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY));
        put(Type_Anniversary, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY));
        put(Type_Other, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER));
        put(Type_Custom, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM));
        put(Type_5K, "11");
        put(Type_Death, "12");
        put(Type_NameDay, "13");
        put(Type_Crowning, "14");
        put(Type_Custom1, "15");
        put(Type_Custom2, "16");
        put(Type_Custom3, "17");
        put(Type_Custom4, "18");
        put(Type_Custom5, "19");
        put(Type_CalendarEvent, "20");
    }};

    final List<String> eventList = new ArrayList<>(); //Список всех событий

    private String currentLocale = STRING_EMPTY;
    int currentTheme = 0;
    final String systemLocale = Locale.getDefault().getLanguage();
    HashSet<String> set_events_deaths; //ID контактов с годовщиной смерти
    HashMap<String, Date> set_events_birthdays; //дни рождения
    private HashSet<String> set_contacts_ids; //ID всех контактов в адресной книге
    private HashSet<String> set_events_ids; //ID всех найденных событий календаря
    private HashMap<String, String> map_contacts_names; //связка имён контактов с ID
    HashMap<String, String> map_calendars = new HashMap<>(); //список всех календарей
    private HashMap<String, Integer> map_eventsBySubtypeAndPersonID_offset; //индекс события до сортировки

    //Настройки
    boolean preferences_debug_on;
    String preferences_language;

    //boolean[] preferences_list_event_types_on;
    Set<String> preferences_list_event_types; // = new HashSet<>();
    Set<String> preferences_list_event_info;
    String preferences_list_prev_events;
    int preferences_list_prev_events_found = 0;
    private int preferences_list_sad_photo;
    int preferences_events_scope;
    int preferences_notification_channel_id;
    int preferences_list_caption;
    String preferences_birthday_calendars_rules;
    private boolean preferences_birthday_calendars_useyear;

    Set<String> preferences_widgets_event_info;
    String preferences_widgets_bottom_info;
    String preferences_widgets_bottom_info_2nd;
    int preferences_widgets_days_eventsoon;
    int preferences_widgets_color_default;
    int preferences_widgets_color_eventtoday;
    int preferences_widgets_color_eventsoon;
    int preferences_widgets_color_eventfar;


    Matcher preferences_last_name_comletions_man;
    Matcher preferences_last_name_comletions_female;
    Matcher preferences_first_names_man;
    Matcher preferences_first_names_female;
    Matcher preferences_second_name_comletions_man;
    Matcher preferences_second_name_comletions_female;
    Matcher preferences_death_labels;
    private Matcher preferences_birthday_labels;
    private Matcher preferences_wedding_labels;
    private Matcher preferences_nameday_labels;
    private Matcher preferences_crowning_labels;
    private Matcher preferences_other_labels;

    private boolean preferences_customevent1_enabled;
    String preferences_customevent1_caption;
    private Matcher preferences_customevent1_labels;
    private boolean preferences_customevent1_useyear;

    private boolean preferences_customevent2_enabled;
    String preferences_customevent2_caption;
    private Matcher preferences_customevent2_labels;
    private boolean preferences_customevent2_useyear;

    private boolean preferences_customevent3_enabled;
    String preferences_customevent3_caption;
    private Matcher preferences_customevent3_labels;
    private boolean preferences_customevent3_useyear;

    private boolean preferences_customevent4_enabled;
    String preferences_customevent4_caption;
    private Matcher preferences_customevent4_labels;
    private boolean preferences_customevent4_useyear;

    private boolean preferences_customevent5_enabled;
    String preferences_customevent5_caption;
    private Matcher preferences_customevent5_labels;
    private boolean preferences_customevent5_useyear;

    Set<String> preferences_notifications_days;

    /* preferences_notifications_type:
     *   0 - Одно общее уведомление
     *   1 - Каждое событие в отдельном уведомлении
     *   2 - Если собыий меньше 3 => отдельные, иначе - общее
     *   3 - Если собыий меньше 4 => отдельные, иначе - общее
     * */
    private int preferences_notifications_type;
    private int preferences_notifications_priority;
    //boolean[] preferences_notifications_event_types_on;
    private Set<String> preferences_notifications_event_types; // = new HashSet<>();
    private Set<String> preferences_notifications_quick_actions; // = new HashSet<>();
    private int preferences_notifications_alarm_hour;
    String preferences_notifications_ringtone;

    static class MyTheme {
        int prefNumber; //Номер в shared preferences
        int themeMain; //Тема основной активности
        int themePopup; //Тема вплывающего меню
        int themeDialog; //Тема диалогов
    }
    MyTheme preferences_theme;

    private Set<String> preferences_hiddenEvents = new HashSet<>();
    private Set<String> preferences_silentEvents = new HashSet<>();
    private final HashMap<String, String> preferences_mergedIDs = new HashMap<>();
    private Set<String> preferences_Accounts = new HashSet<>();
    Set<String> preferences_Calendars_BirthDay = new HashSet<>();
    private Set<String> preferences_Calendars_Other = new HashSet<>();

    //Статистика
    long statTimeGetContactEvents = 0;
    long statTimeGetCalendarEvents = 0;
    long statTimeComputeDates = 0;
    long statLastComputeDates = 0;
    int statAllEvents = 0;
    int statAllTitles = 0;
    int statAllOrganizations = 0;
    int statAllNicknames = 0;
    int statAllContacts = 0;
    final HashMap<String, Integer> statEventTypes = new HashMap<>();

    //UI объекты
    Context context;
    private Resources resources;
    private ContentResolver contentResolver;

    static class ColumnIndexCache implements AutoCloseable {
        //https://android.jlelse.eu/using-a-cache-to-optimize-data-retrieval-from-cursors-56f9eaa1e0d2

        final private HashMap<String, Integer> mMap = new HashMap<>();

        int getColumnIndex(Cursor cursor, String columnName) {
            if (!mMap.containsKey(columnName))
                mMap.put(columnName, cursor.getColumnIndex(columnName));
            Integer ind = mMap.get(columnName);
            return ind != null ? ind : 0;
        }

        void clear() {
            mMap.clear();
        }

        @Override
        public void close() {
            this.clear();
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

    private long countDaysDiff(@NonNull Date date1, @NonNull Date date2) {
        //https://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances/43681941#43681941

        try {

            boolean isNegative = false;
            Calendar c1 = removeTime(from(date1));
            Calendar c2 = removeTime(from(date2));

            if (c1.get(YEAR) == c2.get(YEAR)) {
                return c2.get(DAY_OF_YEAR) - c1.get(DAY_OF_YEAR);
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
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_COUNT_DAYS_DIFF_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    String countDaysDiffText(@NonNull Date date1, @NonNull Date date2) {
        try {

            StringBuilder eventDistance = new StringBuilder();
            long dayDiff;

            //если включить desugaring https://www.youtube.com/watch?v=heCvGfOGH0s то размер приложения +200К
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                //https://stackoverflow.com/questions/4710206/calculate-age-in-years-months-days-hours-minutes-and-seconds
                LocalDate dateStart = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate dateEnd = date2.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                Period p;
                if (dateStart.isBefore(dateEnd)) {
                    p = Period.between(dateStart, dateEnd);
                    dayDiff = DAYS.between(dateStart, dateEnd);
                } else {
                    p = Period.between(dateEnd, dateStart);
                    dayDiff = DAYS.between(dateEnd, dateStart);
                }

                if (p.getYears() > 0 ) {
                    eventDistance
                            .append(getAgeString(
                                    p.getYears(),
                                    R.string.msg_after_year_prefix_1,
                                    R.string.msg_after_year_prefix_1_,
                                    R.string.msg_after_year_prefix_2_3_4,
                                    R.string.msg_after_year_prefix_4_21
                            ))
                            .append(STRING_SPACE);
                }
                if (p.getMonths() > 0) {
                    eventDistance
                            .append(getAgeString(
                                    p.getMonths(),
                                    R.string.msg_after_month_prefix_1,
                                    R.string.msg_after_month_prefix_1_,
                                    R.string.msg_after_month_prefix_2_3_4,
                                    R.string.msg_after_month_prefix_4_21
                            ))
                            .append(STRING_SPACE);
                }
                if (p.getDays() > 0) {
                    eventDistance
                            .append(getAgeString(
                                    p.getDays(),
                                    R.string.msg_after_day_prefix_1,
                                    R.string.msg_after_day_prefix_1_,
                                    R.string.msg_after_day_prefix_2_3_4,
                                    R.string.msg_after_day_prefix_4_21
                            ))
                            .append(STRING_SPACE);
                }

            } else {

                //todo: считать точнее годы и месяцы

                boolean isNegative = false;
                Calendar c1 = removeTime(from(date1));
                Calendar c2 = removeTime(from(date2));

                if (c1.get(YEAR) == c2.get(YEAR)) {
                    dayDiff = c2.get(DAY_OF_YEAR) - c1.get(DAY_OF_YEAR);
                } else {
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
                        dayDiff = -(resD + countLeapYearsBetween(y1, y2));
                    } else {
                        dayDiff = resD + countLeapYearsBetween(y1, y2);
                    }
                }

                long delta = dayDiff;
                if (delta > 365) {
                    eventDistance
                            .append(getAgeString(
                                    delta / 365,
                                    R.string.msg_after_year_prefix_1,
                                    R.string.msg_after_year_prefix_1_,
                                    R.string.msg_after_year_prefix_2_3_4,
                                    R.string.msg_after_year_prefix_4_21
                            ))
                            .append(STRING_SPACE);
                    delta %= 365;
                }
                if (delta > 30) {
                    eventDistance
                            .append(getAgeString(
                                    delta / 30,
                                    R.string.msg_after_month_prefix_1,
                                    R.string.msg_after_month_prefix_1_,
                                    R.string.msg_after_month_prefix_2_3_4,
                                    R.string.msg_after_month_prefix_4_21
                            ))
                            .append(STRING_SPACE);
                    delta %= 30;
                }
                if (delta > 0) {
                    eventDistance
                            .append(getAgeString(
                                    delta,
                                    R.string.msg_after_day_prefix_1,
                                    R.string.msg_after_day_prefix_1_,
                                    R.string.msg_after_day_prefix_2_3_4,
                                    R.string.msg_after_day_prefix_4_21
                            ))
                            .append(STRING_SPACE);
                }
            }

            return eventDistance
                    .append(STRING_PARENTHESIS_START)
                    .append(getAgeString(
                            dayDiff,
                            R.string.msg_after_day_prefix_1,
                            R.string.msg_after_day_prefix_1_,
                            R.string.msg_after_day_prefix_2_3_4,
                            R.string.msg_after_day_prefix_4_21
                    ))
                    .append(STRING_PARENTHESIS_CLOSE)
                    .toString();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_COUNT_DAYS_DIFF_TEXT_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }
    }

    private int countYearsDiff(@NonNull Date date1, @NonNull Date date2) {
        try {

            Calendar c1 = removeTime(from(date1));
            Calendar c2 = removeTime(from(date2));

            return c2.get(YEAR) - c1.get(YEAR);

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_COUNT_YEARS_DIFF_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
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
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_ADD_YEAR_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
            return date;
        }
    }

    @NonNull
    private String getAgeString(long age, int id_prefix_1, int id_prefix_1_, int id_prefix_2_3_4, int id_prefix_4_21) {
        try {

            StringBuilder result = new StringBuilder();

            String count_str = Long.toString(age);
            String count_end = count_str.substring(count_str.length() - 1);
            boolean isEnd234 = count_end.equals(STRING_2) || count_end.equals(STRING_3) || count_end.equals(Constants.STRING_4);

            result.append(age);
            if (age == 1) { //Единственное число
                result.append(getResources().getString(id_prefix_1));
            } else if (age > 4 && age < 21) {
                result.append(getResources().getString(id_prefix_4_21));
            } else if (count_end.equals(STRING_1)) { //Если заканчивается на 1, но не между 5-20
                result.append(getResources().getString(id_prefix_1_));
            } else if (isEnd234) { //Если заканчивается на 2, 3, 4
                result.append(getResources().getString(id_prefix_2_3_4));
            } else {
                result.append(getResources().getString(id_prefix_4_21));
            }
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_AGE_STRING_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }
    }

    void getPreferences() {
        //Инициализация и считывание настроек

        if (context == null) return;

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            //https://medium.com/@anupamchugh/a-nightmare-with-shared-preferences-and-stringset-c53f39f1ef52
            //https://stackoverflow.com/questions/19949182/android-sharedpreferences-string-set-some-items-are-removed-after-app-restart
            Set<String> someSets;

            preferences_debug_on = preferences.getBoolean(context.getString(R.string.pref_Debug_On_key), false);

           /* preferences_list_event_types_on = prefs_EventTypes_DefaultB;
            savedTypes = preferences.getStringSet(context.getString(R.string.pref_List_Events_key), prefs_EventTypes_Default);

            if (savedTypes != null) {
                for (int i = 0; i < preferences_list_event_types_on.length; i++) {
                    preferences_list_event_types_on[i] = savedTypes.contains(event_types_id[i] + STRING_EMPTY);
                }
            }*/
            someSets = preferences.getStringSet(context.getString(R.string.pref_List_Events_key), prefs_EventTypes_Default);
            preferences_list_event_types = new HashSet<>(Objects.requireNonNull(someSets));

            try {
                preferences_list_event_info = preferences.getStringSet(context.getString(R.string.pref_List_EventInfo_key), pref_List_Event_Info);
            } catch (ClassCastException e) {
                preferences_list_event_info = pref_List_Event_Info;
            }
            preferences_list_prev_events = preferences.getString(context.getString(R.string.pref_List_PrevEvents_key), context.getString(R.string.pref_List_PrevEvents_default));
            preferences_list_sad_photo = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_List_SadPhoto_key), context.getString(R.string.pref_List_SadPhoto_default))));
            preferences_list_caption = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_List_Caption_key), context.getString(R.string.pref_List_Caption_default))));
            preferences_language = preferences.getString(context.getString(R.string.pref_Language_key), context.getString(R.string.pref_Language_default));

            try {
                preferences_widgets_event_info = preferences.getStringSet(context.getString(R.string.pref_Widgets_EventInfo_key), pref_Widgets_EventInfo_Info);
            } catch (ClassCastException e) {
                preferences_widgets_event_info = pref_Widgets_EventInfo_Info;
            }

            preferences_widgets_bottom_info = preferences.getString(context.getString(R.string.pref_Widgets_BottomInfo_key), context.getString(R.string.pref_Widgets_BottomInfo_default));
            preferences_widgets_bottom_info_2nd = preferences.getString(context.getString(R.string.pref_Widgets_BottomInfo2nd_key), context.getString(R.string.pref_Widgets_BottomInfo2nd_default));
            preferences_widgets_days_eventsoon = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Widgets_Days_EventSoon_key), context.getString(R.string.pref_Widgets_Days_EventSoon_default))));

            try {

                preferences_widgets_color_eventtoday = preferences.getInt(getResources().getString(R.string.pref_Widgets_Color_EventToday_key), getResources().getColor(R.color.pref_Widgets_Color_EventToday_default));

            } catch (ClassCastException e) { //1.5.4 update

                String old_preferences_color_eventtoday = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Widgets_Color_EventToday_key), STRING_EMPTY));
                preferences_widgets_color_eventtoday = 0;
                switch (old_preferences_color_eventtoday) {
                    case "red":
                        preferences_widgets_color_eventtoday = getResources().getColor(R.color.red);
                        break;
                    case "yellow":
                        preferences_widgets_color_eventtoday = getResources().getColor(R.color.yellow);
                        break;
                    case "white":
                        preferences_widgets_color_eventtoday = getResources().getColor(R.color.white);
                        break;
                    default:
                        preferences_widgets_color_eventtoday = getResources().getColor(R.color.pref_Widgets_Color_EventToday_default);
                }
                if (preferences_widgets_color_eventtoday != 0) {
                    preferences
                            .edit()
                            .remove(context.getString(R.string.pref_Widgets_Color_EventToday_key))
                            .putInt(context.getString(R.string.pref_Widgets_Color_EventToday_key), preferences_widgets_color_eventtoday)
                            .apply();
                    if (preferences_debug_on) Toast.makeText(context, MSG_UPDATED_PREFERENCES + R.string.pref_Widgets_Color_EventToday_key, Toast.LENGTH_LONG).show();
                }

            }

            try {
                preferences_widgets_color_eventsoon = preferences.getInt(getResources().getString(R.string.pref_Widgets_Color_EventSoon_key), getResources().getColor(R.color.pref_Widgets_Color_EventSoon_default));
            } catch (ClassCastException e) { //1.5.4 update
                String old_preferences_color_eventsoon = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Widgets_Color_EventSoon_key), STRING_EMPTY));
                preferences_widgets_color_eventtoday = 0;
                switch (old_preferences_color_eventsoon) {
                    case "red":
                        preferences_widgets_color_eventsoon = getResources().getColor(R.color.red);
                        break;
                    case "green":
                        preferences_widgets_color_eventsoon = getResources().getColor(R.color.green);
                        break;
                    case "white":
                        preferences_widgets_color_eventsoon = getResources().getColor(R.color.white);
                        break;
                    default:
                        preferences_widgets_color_eventsoon = getResources().getColor(R.color.pref_Widgets_Color_EventSoon_default);
                }
                if (preferences_widgets_color_eventsoon != 0) {
                    preferences
                            .edit()
                            .remove(context.getString(R.string.pref_Widgets_Color_EventSoon_key))
                            .putInt(context.getString(R.string.pref_Widgets_Color_EventSoon_key), preferences_widgets_color_eventsoon)
                            .apply();
                    if (preferences_debug_on) Toast.makeText(context, MSG_UPDATED_PREFERENCES + R.string.pref_Widgets_Color_EventSoon_key, Toast.LENGTH_LONG).show();
                }
            }

            preferences_widgets_color_eventfar = preferences.getInt(getResources().getString(R.string.pref_Widgets_Color_EventFar_key), getResources().getColor(R.color.pref_Widgets_Color_EventFar_default));
            preferences_widgets_color_default = preferences.getInt(getResources().getString(R.string.pref_Widgets_Color_EventCaption_key), getResources().getColor(R.color.pref_Widgets_Color_EventCaption_default));

            preferences_events_scope = preferences.getInt(context.getString(R.string.pref_Events_Scope), Constants.pref_Events_Scope_NotHidden);
            preferences_notification_channel_id = preferences.getInt(context.getString(R.string.pref_Notifications_ChannelID), defaultNotificationID);

            boolean useInternal;
            String customLabels;
            final String regex_inter = "|"; //"\\Z|";
            //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

            //Определения событий

            //День рождения
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_default)));
            customLabels = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Birthday_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && isEmpty(customLabels)) {
                preferences_birthday_labels = null;
            } else {
                if (customLabels.isEmpty())
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                else if (!useInternal) {
                    preferences_birthday_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else {
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                }
            }
            preferences_birthday_calendars_rules = preferences.getString(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_default));
            preferences_birthday_calendars_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_default)));

            //Свадьба
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_default)));
            customLabels = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Anniversary_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_wedding_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_wedding_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                }
            }

            //Именины
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_default)));
            customLabels = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_NameDay_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_nameday_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_nameday_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                }
            }

            //Венчание
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_default)));
            customLabels = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Crowning_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_crowning_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_crowning_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                }
            }

            //Годовщина смерти
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_default)));
            customLabels = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Death_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_death_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_death_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                } else {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).concat(STRING_COMMA).concat(customLabels).replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                }
            }

            //Другие события
            customLabels = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Other_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
            if (customLabels.isEmpty()) {
                preferences_other_labels = null;
            } else {
                preferences_other_labels = Pattern.compile(customLabels.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
            }

            //Пользовательские события
            //1
            preferences_customevent1_caption = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom1_Caption_key), STRING_EMPTY)).trim();
            preferences_customevent1_enabled = false;

            if (!preferences_customevent1_caption.isEmpty()) {
                String preferences_customevent1_labels_str = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom1_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent1_labels_str.isEmpty()) {
                    try {
                        preferences_customevent1_labels = Pattern.compile(preferences_customevent1_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                        preferences_customevent1_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent1_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom1_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //2
            preferences_customevent2_caption = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom2_Caption_key), STRING_EMPTY)).trim();
            preferences_customevent2_enabled = false;

            if (!preferences_customevent2_caption.isEmpty()) {
                String preferences_customevent2_labels_str = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom2_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent2_labels_str.isEmpty()) {
                    try {
                        preferences_customevent2_labels = Pattern.compile(preferences_customevent2_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                        preferences_customevent2_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent2_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom2_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //3
            preferences_customevent3_caption = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom3_Caption_key), STRING_EMPTY)).trim();
            preferences_customevent3_enabled = false;

            if (!preferences_customevent3_caption.isEmpty()) {
                String preferences_customevent3_labels_str = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom3_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent3_labels_str.isEmpty()) {
                    try {
                        preferences_customevent3_labels = Pattern.compile(preferences_customevent3_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                        preferences_customevent3_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent3_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom3_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //4
            preferences_customevent4_caption = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom4_Caption_key), STRING_EMPTY)).trim();
            preferences_customevent4_enabled = false;

            if (!preferences_customevent4_caption.isEmpty()) {
                String preferences_customevent4_labels_str = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom4_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent4_labels_str.isEmpty()) {
                    try {
                        preferences_customevent4_labels = Pattern.compile(preferences_customevent4_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                        preferences_customevent4_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent4_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom4_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //5
            preferences_customevent5_caption = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom5_Caption_key), STRING_EMPTY)).trim();
            preferences_customevent5_enabled = false;

            if (!preferences_customevent5_caption.isEmpty()) {
                String preferences_customevent5_labels_str = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_CustomEvents_Custom5_Labels_key), STRING_EMPTY)).replaceAll(Constants.REGEX_COMMAS, STRING_COMMA);
                if (!preferences_customevent5_labels_str.isEmpty()) {
                    try {
                        preferences_customevent5_labels = Pattern.compile(preferences_customevent5_labels_str.replace(STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                        preferences_customevent5_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent5_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom5_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //Уведомления
           //preferences.edit().remove(context.getString(R.string.pref_Notifications_Days_key)).putString(context.getString(R.string.pref_Notifications_Days_key), STRING_3).apply();

            try {
                someSets = preferences.getStringSet(context.getString(R.string.pref_Notifications_Days_key), new HashSet<>());
                preferences_notifications_days = new HashSet<>(Objects.requireNonNull(someSets));
            } catch (ClassCastException e) {//1.5.3 update
                String old_preferences_notifications_days = Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Notifications_Days_key), STRING_EMPTY));
                preferences_notifications_days = new HashSet<>();
                switch (old_preferences_notifications_days) {
                    case STRING_0:
                        preferences_notifications_days.add(STRING_0);
                        break;
                    case STRING_1:
                        preferences_notifications_days.add(STRING_0);
                        preferences_notifications_days.add(STRING_1);
                        break;
                    case STRING_2:
                        preferences_notifications_days.add(STRING_0);
                        preferences_notifications_days.add(STRING_2);
                        break;
                    case STRING_3:
                        preferences_notifications_days.add(STRING_0);
                        preferences_notifications_days.add(STRING_3);
                        break;
                    case STRING_7:
                        preferences_notifications_days.add(STRING_0);
                        preferences_notifications_days.add(STRING_7);
                        break;
                }
                preferences_notifications_days.removeAll(new HashSet<String>() {{add("");}});
                preferences
                        .edit()
                        .remove(context.getString(R.string.pref_Notifications_Days_key))
                        .putStringSet(context.getString(R.string.pref_Notifications_Days_key), preferences_notifications_days)
                        .apply();
                if (preferences_debug_on) Toast.makeText(context, MSG_UPDATED_PREFERENCES + R.string.pref_Notifications_Days_key, Toast.LENGTH_LONG).show();
            }
            preferences_notifications_days.removeAll(new HashSet<String>() {{add(STRING_EMPTY);}});

            preferences_notifications_type = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Notifications_Type_key), context.getString(R.string.pref_Notifications_Type_default))));
            preferences_notifications_priority = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Notifications_Priority_key), context.getString(R.string.pref_Notifications_Priority_default))));
            preferences_notifications_alarm_hour = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Notifications_AlarmHour_key), context.getString(R.string.pref_Notifications_AlarmHour_default))));
            if (preferences_notifications_alarm_hour < 0) preferences_notifications_alarm_hour = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmHour_default));
            preferences_notifications_ringtone = preferences.getString(context.getString(R.string.pref_Notifications_Ringtone_key), Settings.System.DEFAULT_NOTIFICATION_URI.toString());

            /*savedTypes = preferences.getStringSet(context.getString(R.string.pref_Notifications_Events_key), prefs_EventTypes_Default);
            preferences_notifications_event_types_on = prefs_EventTypes_DefaultB;
            if (savedTypes != null) {
                for (int i = 0; i < preferences_notifications_event_types_on.length; i++) {
                    preferences_notifications_event_types_on[i] = savedTypes.contains(event_types_id[i] + STRING_EMPTY);
                }
            }*/
            someSets = preferences.getStringSet(context.getString(R.string.pref_Notifications_Events_key), preferences_list_event_types); //По-умолчанию берём из списка событий
            preferences_notifications_event_types = new HashSet<>(Objects.requireNonNull(someSets));

            someSets = preferences.getStringSet(context.getString(R.string.pref_Notifications_QuickActions_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_QuickActions_values_default))));
            preferences_notifications_quick_actions = new HashSet<>(Objects.requireNonNull(someSets));

            //Запоминаем информацию о темах
            preferences_theme = new MyTheme();
            try {
                preferences_theme.prefNumber = Integer.parseInt(Objects.requireNonNull(preferences.getString(context.getString(R.string.pref_Theme_key), context.getString(R.string.pref_Theme_default))));
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
                case 4:
                    preferences_theme.themeMain = R.style.AppTheme_Blue_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Blue_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Blue;
                    break;
            }

            someSets = preferences.getStringSet(context.getString(R.string.pref_Events_Hidden_key), new HashSet<>());
            preferences_hiddenEvents = new HashSet<>(Objects.requireNonNull(someSets)); // THIS LINE CREATE A COPY

            someSets = preferences.getStringSet(context.getString(R.string.pref_Events_Silent_key), new HashSet<>());
            preferences_silentEvents = new HashSet<>(Objects.requireNonNull(someSets));

            someSets = preferences.getStringSet(context.getString(R.string.pref_Accounts_key), new HashSet<>());
            preferences_Accounts = new HashSet<>(Objects.requireNonNull(someSets));

            someSets = preferences.getStringSet(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key), new HashSet<>());
            preferences_Calendars_BirthDay = new HashSet<>(Objects.requireNonNull(someSets));

            someSets = preferences.getStringSet(context.getString(R.string.pref_CustomEvents_Other_Calendars_key), new HashSet<>());
            preferences_Calendars_Other = new HashSet<>(Objects.requireNonNull(someSets));

            someSets = preferences.getStringSet(context.getString(R.string.pref_MergedID_key), new HashSet<>());
            if (someSets != null) {
                //StringBuilder sb = new StringBuilder();
                for(String element: someSets) {
                    int indexDiv = element.indexOf(STRING_COLON_SPACE);
                    if (indexDiv > -1){
                        preferences_mergedIDs.put(element.substring(0, indexDiv), element.substring(indexDiv + STRING_COLON_SPACE.length()));
                        //sb.append(element.substring(0, indexDiv)).append("=").append(element.substring(indexDiv + STRING_COLON_SPACE.length())).append("\n");
                    }
                }
                //Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e){
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_PREFERENCES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    void setPreferences() {
        //Сохранение настроек

        if (context == null) return;

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_events_scope);
            editor.putInt(context.getString(R.string.pref_Notifications_ChannelID), preferences_notification_channel_id);
            editor.putStringSet(context.getString(R.string.pref_Accounts_key), getPreferences_Accounts());
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key), preferences_Calendars_BirthDay);
            editor.putString(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), preferences_birthday_calendars_rules);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_Calendars_key), preferences_Calendars_Other);

            editor.apply();

        } catch (Exception e){
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_PREFERENCES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void setLocale(boolean force) {

        if (context == null) return;

        //сделать так: https://stackoverflow.com/questions/39705739/android-n-change-language-programmatically/
        //для Android > N переделать выбор локали https://stackoverflow.com/questions/47165311/how-to-change-android-o-oreo-api-26-app-language
        //посмотреть https://stackoverflow.com/questions/9475589/how-to-get-string-from-different-locales-in-android и сделать нормальным переключение языков
        try {
            //getPreferences();

            // http://developer.alexanderklimov.ru/android/locale.php
            if (force || !preferences_language.equals(currentLocale)) {

                Configuration configuration = context.getResources().getConfiguration(); //new Configuration();
                Locale locale;
                if (preferences_language.equals(context.getString(R.string.pref_Language_default))) {
                    //Auto
                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //    locale = configuration.getLocales().get(0);
                    //} else {
                    locale = new Locale(systemLocale);
                    //}
                } else {
                    locale = new Locale(preferences_language);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    configuration.setLocales(new android.os.LocaleList(locale));
                } else {
                    configuration.setLocale(locale);
                }
                Locale.setDefault(locale);
                resources = context.getResources();
                resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                currentLocale = preferences_language;

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_LOCALE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    boolean getEvents(Context in_context) {

        context = in_context;
        if (context == null) return false;

        try {

            eventList.clear();
            set_events_deaths = new HashSet<>();
            set_events_birthdays = new HashMap<>();
            set_contacts_ids = new HashSet<>();
            set_events_ids = new HashSet<>();
            map_contacts_names = new HashMap<>();
            map_eventsBySubtypeAndPersonID_offset = new HashMap<>();
            statAllEvents = 0;
            statAllTitles = 0;
            statAllOrganizations = 0;
            statAllNicknames = 0;
            statTimeGetContactEvents = 0;
            statTimeGetCalendarEvents = 0;

            getPreferences();

            return getContactsEvents() | getCalendarEvents(eventTypesIDs.get(Type_BirthDay)) | getCalendarEvents(eventTypesIDs.get(Type_Other));

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean getContactsEvents() {
        //Получаем требуемые события (дни рождения, и т.п.)
        //todo: попробовать добраться до ДР стандартными способами https://stackoverflow.com/questions/35448250/how-to-get-whatsapp-contacts-from-android
        //todo: сделать импорт ДР одноклассники https://ruseller.com/lessons.php?id=1661 https://apiok.ru/ext/oauth/

        //todo: попробовать сделать агрегацию контактов
        // https://stackoverflow.com/questions/9419305/how-do-you-get-contacts-to-aggregate-properly-when-programmatically-adding-them
        // https://stackoverflow.com/questions/39804979/how-i-can-programmatically-merge-two-different-contactsandroid/39805494

        try{

            long statCurrentModuleStart = System.currentTimeMillis();
            StringBuilder dataRow;
            TreeMap<Integer, String> userData = new TreeMap <>();
            List<String> dataList = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return false;

            if (contentResolver == null) contentResolver = context.getContentResolver();
            ColumnIndexCache cache = new ColumnIndexCache();

            //Перебираем все данные и кэшируем: организацию, должность, ник
            HashMap<String, String> orgMap = new HashMap<>();
            HashMap<String, String> titleMap = new HashMap<>();
            HashMap<String, String> nickMap = new HashMap<>();
            //https://stackoverflow.com/a/20260606/4928833

            final String[] projectionOrgTitle = new String[] {
                    Constants.ColumnNames_CONTACT_ID,
                    ContactsContract.CommonDataKinds.Organization.COMPANY,
                    ContactsContract.CommonDataKinds.Organization.TITLE
            };
            Cursor contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionOrgTitle,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE + "'",
                    null,
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, Constants.ColumnNames_CONTACT_ID));

                        String organization = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Organization.COMPANY));
                        if (!orgMap.containsKey(personID) && organization != null && !organization.isEmpty()) orgMap.put(personID, organization);

                        String title = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Organization.TITLE));
                        if (!titleMap.containsKey(personID) && title != null && !title.isEmpty()) titleMap.put(personID, title);

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statAllOrganizations = orgMap.size();
            statAllTitles = titleMap.size();

            cache = new ColumnIndexCache();
            final String[] projectionNick = new String[] {
                    Constants.ColumnNames_CONTACT_ID,
                    ContactsContract.CommonDataKinds.Nickname.NAME
            };
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionNick,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE + "'",
                    null,
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, Constants.ColumnNames_CONTACT_ID));
                        String nick = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Nickname.NAME));
                        if (nick != null && !nick.isEmpty()) {
                            if (!nickMap.containsKey(personID)) nickMap.put(personID, nick);
                            //todo: добавлять ники в map_contacts_names
                        }

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statAllNicknames = nickMap.size();

            //Собираем id всех контактов
            cache = new ColumnIndexCache();
            String[] projectionAllContacts = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Data.DISPLAY_NAME,
                    ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE
            };
            contactData = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projectionAllContacts,
                    null,
                    null,
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Contacts._ID));
                        if (personID != null) set_contacts_ids.add(personID);

                        String personName = normalizeName(contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Data.DISPLAY_NAME)));
                        if (personName != null && !personName.isEmpty() & !map_contacts_names.containsKey(personName)) map_contacts_names.put(personName, personID);

                        String personNameAlt = normalizeName(contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)));
                        if (personNameAlt != null && !personNameAlt.isEmpty() & !map_contacts_names.containsKey(personNameAlt)) map_contacts_names.put(personNameAlt, personID);

                        //todo: добавить имена латиницей (для мэппинга)

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statAllContacts = set_contacts_ids.size();

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
            cache = new ColumnIndexCache();
            final String[] projectionContactsEvents = new String[] {
                    ContactsContract.CommonDataKinds.Event.DATA,
                    ContactsContract.CommonDataKinds.Event.TYPE,
                    ColumnNames_ACCOUNT_TYPE,
                    ColumnNames_ACCOUNT_NAME,
                    ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE,
                    ContactsContract.Data.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Event.LABEL,
                    Constants.ColumnNames_CONTACT_ID,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
            };
            Cursor cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionContactsEvents,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "'",
                    null,
                    ContactsContract.Data.DISPLAY_NAME + " ASC, " +
                            ContactsContract.CommonDataKinds.Event.TYPE + " ASC, " +
                            ContactsContract.CommonDataKinds.Event.LABEL + " ASC"
            );
            if (cursor == null) return false;

            int countErrors = 0;
            String eventKey = STRING_EMPTY;

            if (cursor.moveToFirst()) {
                do {
                    try {

                        statAllEvents++;
                        eventKey = addContactEventToEventList(cursor, userData, dataList, cache, orgMap, titleMap, nickMap, eventKey);

                    } catch (NullPointerException e) {
                        countErrors++;
                        if (preferences_debug_on && countErrors < 3) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(Constants.CONTACTS_EVENTS_GET_CONTACTS_EVENTS_ERROR).append(e.toString()).append(STRING_EOF);
                            for(String name: cursor.getColumnNames()) {
                                String data = cursor.getString(cache.getColumnIndex(cursor, name));
                                if (data != null && !data.equals(STRING_0)) sb.append(name).append(STRING_COLON_SPACE).append(data).append(STRING_EOF);
                            }
                            Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                } while (cursor.moveToNext());

                if (!userData.isEmpty()) { // Данные последнего контакта

                    dataRow = new StringBuilder();
                    int rNum = 0;
                    for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                        rNum++;
                        if (rNum != 1) {
                            dataRow.append(Constants.STRING_2HASH);
                        }
                        dataRow.append(entry.getValue());
                    }

                    if (dataList.add(dataRow.toString())) { //Добавляем для поиска календарных событий (дни рождения)
                        String personID = userData.get(Position_contactID);
                        if (personID != null && !personID.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + personID, dataList.size() - 1);
                        //String personNameAlt = userData.get(Position_personFullNameAlt);
                        //if (personNameAlt != null && !personNameAlt.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + normalizeName(personNameAlt), dataList.size() - 1);
                    }

                    userData.clear();

                }
            }
            cursor.close();

            eventList.addAll(dataList);
            dataList.clear();
            statTimeGetContactEvents = System.currentTimeMillis() - statCurrentModuleStart;

            if (preferences_debug_on && countErrors > 1) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACTS_EVENTS_ERROR + "Total errors: " + countErrors, Toast.LENGTH_LONG).show();

            return true;

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACTS_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @NonNull private String addContactEventToEventList(@NonNull Cursor cursor, @NonNull TreeMap<Integer, String> userData, @NonNull List<String> dataList, @NonNull ColumnIndexCache cache, @NonNull HashMap<String, String> orgMap, @NonNull HashMap<String, String> titleMap, @NonNull HashMap<String, String> nickMap, @NonNull String eventKey) {

        try {
            StringBuilder dataRow;
            String eventDate = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.DATA));
            String eventType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));
            String eventSubType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));
            String accountType = cursor.getString(cache.getColumnIndex(cursor, ColumnNames_ACCOUNT_TYPE));
            String accountName = cursor.getString(cache.getColumnIndex(cursor, ColumnNames_ACCOUNT_NAME));
            String accountKey = accountName + STRING_PARENTHESIS_OPEN + accountType + STRING_PARENTHESIS_CLOSE;

            //if (countErrors == 0) throw new NullPointerException("test");
            if (eventDate != null && (preferences_Accounts.isEmpty() || preferences_Accounts.contains(accountKey))) {

                //String contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)); //бывает пусто
                //if (contactName == null) contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME));
                String contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME));
                if (contactName == null) contactName = STRING_EMPTY;
                String contactNameAlt = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE));
                if (contactNameAlt == null) contactNameAlt = STRING_EMPTY;
                String eventLabel = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.LABEL));
                if (eventLabel == null) eventLabel = STRING_EMPTY;
                boolean nonemptyEventLabel = !isEmpty(eventLabel);
                String eventCaption = STRING_EMPTY;
                int eventIcon = 0;
                String eventEmoji = "📆";

                if (eventType.equals(eventTypesIDs.get(Type_BirthDay)) ||
                        (nonemptyEventLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_birthday);
                    eventIcon = R.drawable.ic_event_birthday; //https://icons8.com/icon/21460/birthday
                    eventEmoji = "🎂";
                    eventSubType = eventTypesIDs.get(Type_BirthDay);

                } else if (eventType.equals(eventTypesIDs.get(Type_Anniversary)) ||
                        (nonemptyEventLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_anniversary);
                    eventIcon = R.drawable.ic_event_wedding; //https://www.flaticon.com/free-icon/wedding-rings_224802
                    eventEmoji = "💑";
                    eventSubType = eventTypesIDs.get(Type_Anniversary);

                } else if (eventType.equals(eventTypesIDs.get(Type_Other)) ||
                        (nonemptyEventLabel && preferences_other_labels != null && preferences_other_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_other);
                    eventIcon = R.drawable.ic_event_other; //https://icons8.com/icon/set/event/office
                    eventEmoji = "🗓️";
                    eventSubType = eventTypesIDs.get(Type_Other);

                } else if (nonemptyEventLabel) {

                    if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent1_caption;
                        eventIcon = R.drawable.ic_event_custom1;
                        eventEmoji = "🗓️";
                        eventSubType = eventTypesIDs.get(Type_Custom1);
                        if (!preferences_customevent1_useyear && !eventDate.startsWith(STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else  if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent2_caption;
                        eventIcon = R.drawable.ic_event_custom2;
                        eventEmoji = "🔔";
                        eventSubType = eventTypesIDs.get(Type_Custom2);
                        if (!preferences_customevent2_useyear && !eventDate.startsWith(STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent3_caption;
                        eventIcon = R.drawable.ic_event_custom3;
                        eventEmoji = "⏰";
                        eventSubType = eventTypesIDs.get(Type_Custom3);
                        if (!preferences_customevent3_useyear && !eventDate.startsWith(STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent4_caption;
                        eventIcon = R.drawable.ic_event_custom4;
                        eventEmoji = "❤️";
                        eventSubType = eventTypesIDs.get(Type_Custom4);
                        if (!preferences_customevent4_useyear && !eventDate.startsWith(STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent5_caption;
                        eventIcon = R.drawable.ic_event_custom5;
                        eventEmoji = "🎁";
                        eventSubType = eventTypesIDs.get(Type_Custom5);
                        if (!preferences_customevent5_useyear && !eventDate.startsWith(STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_nameday);
                        eventIcon = R.drawable.ic_event_nameday;
                        eventEmoji = "🎈";
                        eventSubType = eventTypesIDs.get(Type_NameDay);

                    } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_crowning);
                        eventIcon = R.drawable.ic_event_crowning; //https://iconscout.com/icon/wedding-destination-romance-building-emoj-symbol
                        eventEmoji = "💒";
                        eventSubType = eventTypesIDs.get(Type_Crowning);

                    } else if (preferences_death_labels != null && preferences_death_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_death);
                        eventIcon = R.drawable.ic_event_death;
                        eventEmoji = "⚰️";
                        eventSubType = eventTypesIDs.get(Type_Death);
                        set_events_deaths.add(cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID))); //Запоминаем событие контакта

                    }

                }
                if (nonemptyEventLabel && eventCaption.isEmpty()) eventCaption = eventLabel;

                String eventKey_next = contactName.concat(STRING_COMMA).concat(eventType);

                //Наименование события в ключе только для пользовательских событий
                if (eventType.equals(eventTypesIDs.get(Type_Custom))) {
                    eventKey_next = eventKey_next.concat(STRING_COMMA).concat(eventLabel);
                }

                String newEventDate = accountType + STRING_COLON_SPACE + eventDate;

                if (!eventKey_next.equalsIgnoreCase(eventKey)) { //Начало данных нового контакта

                    if (!userData.isEmpty()) { // Уже есть накопленные данные. Нужно сохранить всё, что накопили и обнулить UserData
                        dataRow = new StringBuilder();
                        int rNum = 0;
                        for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                            rNum++;
                            if (rNum != 1) {
                                dataRow.append(Constants.STRING_2HASH);
                            }
                            dataRow.append(entry.getValue());
                        }
                        if (dataList.add(dataRow.toString())) { //Добавляем для поиска календарных событий (дни рожденя)
                            String personID = userData.get(Position_contactID);
                            if (personID != null && !personID.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + personID, dataList.size() - 1);
                            //String personNameAlt = userData.get(Position_personFullNameAlt);
                            //if (personNameAlt != null && !personNameAlt.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + normalizeName(personNameAlt), dataList.size() - 1);
                        }
                        userData.clear();
                    }

                    String contactID = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID));
                    String contactFIO = contactName.replace(STRING_COMMA_SPACE, STRING_SPACE);

                    String contactTitle = STRING_EMPTY;
                    if (titleMap.get(contactID) != null) contactTitle = titleMap.get(contactID);
                    if (Objects.requireNonNull(contactTitle).isEmpty()) {
                        //всё, что внутри скобок в имени - в должность
                        int pStart = contactFIO.indexOf(STRING_PARENTHESIS_START);
                        int pEnd = contactFIO.indexOf(STRING_PARENTHESIS_CLOSE);
                        if (pStart > -1 && pEnd > pStart) {
                            contactTitle = contactFIO.substring(pStart + 1, pEnd);
                            contactFIO = contactFIO.replace(STRING_PARENTHESIS_START + contactTitle + STRING_PARENTHESIS_CLOSE, STRING_EMPTY);
                        }
                    }

                    eventKey = eventKey_next;

                    userData.put(Position_eventDate_sorted, STRING_SPACE);
                    userData.put(Position_personFullName, contactFIO);
                    userData.put(Position_personFullNameAlt, contactNameAlt.replace(STRING_COMMA_SPACE, STRING_SPACE));
                    userData.put(Position_contactID, contactID);
                    userData.put(Position_eventID, STRING_EMPTY);
                    userData.put(Position_photo_uri, cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.PHOTO_URI)));
                    userData.put(Position_eventDate, STRING_SPACE); //Полная дата события
                    userData.put(Position_eventDateText, STRING_SPACE); //Дата для отображения
                    userData.put(Position_eventDistance, STRING_SPACE); //Дней до даты
                    userData.put(Position_eventDistanceText, STRING_SPACE); //Через сколько событие и какой будет день недели
                    userData.put(Position_age, STRING_SPACE); //Возраст
                    userData.put(Position_age_caption, STRING_SPACE); //Подпись для возраста
                    userData.put(Position_eventCaption, eventCaption); //Наименование события
                    //подпорка: почему-то для одиноких Skype событий в eventLabel находится дата события
                    userData.put(Position_eventLabel, !eventLabel.equals(eventCaption) & !newEventDate.contains(eventLabel) ? eventLabel : STRING_EMPTY); //Заголовок пользовательского события
                    userData.put(Position_eventType, eventType); //Тип события
                    userData.put(Position_eventSubType, eventSubType); //Подтип события
                    userData.put(Position_organization, orgMap.containsKey(contactID) ? orgMap.get(contactID) : STRING_SPACE);
                    userData.put(Position_title, contactTitle);
                    //userData.put(dataMap.get("note"), ""); //noteMap.containsKey(contactID) ? noteMap.get(contactID) : "");
                    userData.put(Position_dates, newEventDate);
                    userData.put(Position_eventIcon, Integer.toString(eventIcon));
                    userData.put(Position_eventEmoji, eventEmoji);
                    userData.put(Position_starred, cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.STARRED)));
                    //userData.put(Position_lastContacted, STRING_SPACE); //todo: получать через историю звонков
                    userData.put(Position_nickname, nickMap.containsKey(contactID) ? nickMap.get(contactID) : STRING_SPACE);
                    userData.put(Position_age_current, STRING_SPACE); //Возраст текущий
                    userData.put(Position_eventStorage, STRING_STORAGE_CONTACTS); //Где искать событие по ID
                    userData.put(Position_zodiacSign, STRING_SPACE); //Знак зодиака
                    userData.put(Position_zodiacYear, STRING_SPACE); //Зодиакальный год

                } else { //Продолжаем добавлять даты контакта

                    String existingDates = userData.get(Position_dates);
                    if (existingDates != null && !existingDates.contains(newEventDate))
                        userData.put(Position_dates, existingDates.concat(Constants.STRING_2TILDA).concat(newEventDate));

                }
            }
            return eventKey;

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_ADD_CONTACT_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return eventKey;
        }
    }

    private boolean getCalendarEvents(String eventType) {

        try {

            long statCurrentModuleStart = System.currentTimeMillis();

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return false;

            Set<String> preferences_calendars = getPreferences_Calendars(Objects.requireNonNull(eventType));
            if (!preferences_list_event_types.contains(eventType) || preferences_calendars.size() == 0) return false;

            ColumnIndexCache cache = new ColumnIndexCache();
            List<String> dataList = new ArrayList<>();
            StringBuilder dataRow;
            TreeMap<Integer, String> userData = new TreeMap <>();
            map_calendars = getCalendars();

            //https://stackoverflow.com/questions/25734285/how-to-get-the-real-time-of-recurring-events
            //https://stackoverflow.com/questions/10133616/reading-all-of-todays-events-using-calendarcontract-android-4-0

            Locale locale_en = new Locale(Constants.LANG_EN);
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_JAVA, locale_en);
            SimpleDateFormat sdfny = new SimpleDateFormat(Constants.DATE_JAVA_NO_YEAR, locale_en);
            if (contentResolver == null) contentResolver = context.getContentResolver();
            String[] projection = new String[] {
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.DESCRIPTION, //todo: доделать правила и под это поле
                    CalendarContract.Instances.BEGIN, //начало именно этого события
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Events.DTSTART //начало первоначального события
            };

            Calendar startTime = Calendar.getInstance();
            startTime.set(HOUR_OF_DAY, 0);
            startTime.set(MINUTE, 0);
            startTime.set(SECOND, 0);
            startTime.set(MILLISECOND, 0);
            final int zoneOffset = TimeZone.getDefault().getOffset(startTime.getTimeInMillis()); //событие на весь день начинается в 00:00:00 UTC, надо скорректировать часовую зону
            startTime.add(MILLISECOND, zoneOffset);

            Calendar endTime = Calendar.getInstance();
            endTime.set(YEAR, startTime.get(YEAR) + 1);
            endTime.set(HOUR_OF_DAY, 0);
            endTime.set(MINUTE, 0);
            endTime.set(SECOND, 0);
            endTime.set(MILLISECOND, 0);
            endTime.add(MILLISECOND, zoneOffset);
            endTime.add(SECOND, -1);

            String eventSubType;
            int eventIcon;
            String eventEmoji;

            int importStorage;
            String[] arrRules;
            HashSet<Matcher> matcherNames = new HashSet<>();
            boolean useEventYear = true;

            if (eventType.equals(eventTypesIDs.get(Type_BirthDay))) {

                eventIcon = R.drawable.ic_event_birthday;
                eventEmoji = "🎂";
                eventSubType = eventTypesIDs.get(Type_BirthDay);
                importStorage = 1;
                useEventYear = preferences_birthday_calendars_useyear;
                final String regex_delim = "\\|";
                arrRules = preferences_birthday_calendars_rules.split(regex_delim);
                if (arrRules.length > 0) {
                    for (String rule : arrRules) {
                        if (rule.contains(RULE_TAG_NAME)) {
                            matcherNames.add(Pattern.compile(rule.replace(RULE_TAG_NAME, "(.*?)")).matcher(STRING_EMPTY));
                        }
                    }
                }
            } else {

                eventSubType = eventTypesIDs.get(Type_CalendarEvent);
                eventIcon = R.drawable.ic_event_other;
                eventEmoji = "🗓️";
                importStorage = 0;

            }

            //todo: переделать на https://developer.android.com/reference/android/provider/CalendarContract.Instances
            StringBuilder calIDs = new StringBuilder();
            for (String calID: preferences_calendars) {
                if (calIDs.length() > 0) calIDs.append(" OR " + CalendarContract.Events.CALENDAR_ID + " = ");
                calIDs.append(calID);
            }

            String selection = "("
                    //+ "( " + CalendarContract.Events.DTSTART + " >= ? ) "
                    // + "AND ( " + CalendarContract.Events.DTSTART + " <= ? ) "
                    // + "AND"
                    + " ( " + CalendarContract.Events.ALL_DAY + " = 1 )"
                    + "AND ( " + CalendarContract.Events.CALENDAR_ID + " = " + calIDs.toString() + " )"
                    + ")";

            //CalendarContract.Events.CONTENT_URI
            //CalendarContract.Instances.CONTENT_URI
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startTime.getTimeInMillis());
            ContentUris.appendId(builder, endTime.getTimeInMillis());

            Cursor cursor = contentResolver.query(builder.build(), projection, selection, null, "dtstart ASC");
            if (cursor != null) {
                if (cursor.getCount() > 0) {

                    while (cursor.moveToNext()) {
                        userData.clear();
                        Date date = new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.BEGIN))));
                        Date dateFirst = new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DTSTART))));
                        userData.put(Position_eventDate_sorted, STRING_SPACE);

                        //0. Календарное событие
                        //1. Контакт не найден - это новое событие
                        //2. Контакт найден, но у него нет данных о дне рождении - обновляем событие по карточке контакта
                        //3. Контакт найден, у него есть такое же событие - добавляем к источникам дат ещё одно значение
                        int importMethod = 0;
                        final String eventTitle = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.TITLE));
                        final String eventID = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.EVENT_ID));
                        set_events_ids.add(eventID);

                        String contactID = null;
                        userData.put(Position_personFullName, eventTitle);
                        userData.put(Position_personFullNameAlt, eventTitle);
                        userData.put(Position_eventStorage, STRING_STORAGE_CALENDAR); //STRING_STORAGE_CONTACTS);

                        if (importStorage == 1 && !getMergedID(eventID).isEmpty()) {

                            contactID = getMergedID(eventID);

                        } else if (importStorage == 1 && matcherNames.size() > 0) {

                            String foundName;

                            for (Matcher matcherName: matcherNames) {

                                if (matcherName.reset(eventTitle).find()) {
                                    foundName = matcherName.group(1);

                                    if (foundName != null) {

/*                                        userData.put(Position_personFullName, foundName);

                                        //если 2 компонента - просто меняем местами
                                        int spaceFirst = foundName.indexOf(STRING_SPACE);
                                        int spaceLast = foundName.lastIndexOf(STRING_SPACE);

                                        if (spaceFirst != -1 && spaceFirst == spaceLast) {
                                            userData.put(Position_personFullNameAlt, foundName.substring(spaceFirst + 1).concat(STRING_SPACE).concat(foundName.substring(0, spaceFirst)));
                                        } else {
                                            userData.put(Position_personFullNameAlt, foundName);
                                        }*/

                                        //Ищем контакт
                                        final String foundNameNormalyzed = normalizeName(foundName);
                                        contactID = map_contacts_names.get(foundNameNormalyzed);
                                        break;
                                    }

                                }

                            }
                        }

                        if (contactID != null) {
                            importMethod = 2;
                            userData.put(Position_contactID, contactID);

                            //Ищем событие контакта в списке событий и добавляем в него
                            Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(eventTypesIDs.get(Type_BirthDay) + STRING_2HASH + contactID);
                            if (eventIndex != null && eventIndex <= eventList.size()) {

                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_2HASH));
                                final String eventDates = singleRowList.get(ContactsEvents.Position_dates);
                                final String eventNewDate = Constants.EVENT_PREFIX_CALENDAR_EVENT + STRING_COLON_SPACE + (useEventYear ? sdf.format(dateFirst) : sdfny.format(date));

                                if (!eventDates.contains(eventNewDate)) { //Пропускаем дубли
                                    singleRowList.set(ContactsEvents.Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
                                    if (singleRowList.get(ContactsEvents.Position_eventID).isEmpty()) {
                                        singleRowList.set(ContactsEvents.Position_eventID, eventID);
                                    }
                                    dataRow = new StringBuilder();
                                    int rNum = 0;
                                    for (String entry : singleRowList) {
                                        rNum++;
                                        if (rNum != 1) {
                                            dataRow.append(Constants.STRING_2HASH);
                                        }
                                        dataRow.append(entry);
                                    }
                                    eventList.set(eventIndex, dataRow.toString());
                                }

                                importMethod = 3;

                            } else {
                                //Добавляем данные контакта
                                userData.put(Position_photo_uri, getContactData(parseToLong(contactID), ContactsContract.Contacts.PHOTO_URI));
                                userData.put(Position_personFullName, getContactData(parseToLong(contactID), ContactsContract.Data.DISPLAY_NAME));
                                userData.put(Position_personFullNameAlt, getContactData(parseToLong(contactID), ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE).replace(STRING_COMMA, STRING_EMPTY));
                            }
                        }

                        if (importMethod != 3) {

                            if (importMethod != 2) {
                                userData.put(Position_photo_uri, STRING_EMPTY);
                                userData.put(Position_eventStorage, STRING_STORAGE_CALENDAR);
                                userData.put(Position_contactID, STRING_EMPTY);
                            }

                            userData.put(Position_eventID, eventID);
                            userData.put(Position_eventDate, STRING_SPACE); //Полная дата события
                            userData.put(Position_eventDateText, STRING_SPACE); //Дата для отображения
                            userData.put(Position_eventDistance, STRING_SPACE); //Дней до даты
                            userData.put(Position_eventDistanceText, STRING_SPACE); //Через сколько событие и какой будет день недели
                            userData.put(Position_age, STRING_SPACE); //Возраст
                            userData.put(Position_age_caption, STRING_SPACE); //Подпись для возраста

                            String calendarTitle = map_calendars.get(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.CALENDAR_ID)));
                            userData.put(Position_eventCaption, calendarTitle != null ?
                                    getResources().getString(R.string.msg_calendar_info, getKeyParts(calendarTitle)[0]) :
                                    getResources().getString(R.string.event_type_calendar)
                            ); //Наименование события
                            userData.put(Position_eventLabel, eventTitle != null ? eventTitle : STRING_EMPTY); //Заголовок пользовательского события
                            userData.put(Position_eventType, eventType); //Тип события
                            userData.put(Position_eventSubType, eventSubType); //Подтип события
                            userData.put(Position_organization, STRING_SPACE);
                            userData.put(Position_title, STRING_EMPTY);
                            userData.put(Position_dates, Constants.EVENT_PREFIX_CALENDAR_EVENT + STRING_COLON_SPACE + (useEventYear ? sdf.format(dateFirst) : sdfny.format(date)));
                            userData.put(Position_eventIcon, Integer.toString(eventIcon));
                            userData.put(Position_eventEmoji, eventEmoji);
                            userData.put(Position_starred, STRING_EMPTY);
                            userData.put(Position_nickname, STRING_EMPTY);
                            userData.put(Position_age_current, STRING_SPACE);
                            userData.put(Position_zodiacSign, STRING_SPACE); //Знак зодиака
                            userData.put(Position_zodiacYear, STRING_SPACE); //Зодиакальный год

                            dataRow = new StringBuilder();
                            int rNum = 0;
                            for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                                rNum++;
                                if (rNum != 1) {
                                    dataRow.append(Constants.STRING_2HASH);
                                }
                                dataRow.append(entry.getValue());
                            }
                            final String eventData = dataRow.toString();
                            if (!dataList.contains(eventData)) {
                                dataList.add(eventData);
                            }
                        }
                        userData.clear();
                    }
                }

                cursor.close();
            }

            eventList.addAll(dataList);
            dataList.clear();
            statTimeGetCalendarEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CALENDAR_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    HashMap<String, String> getCalendars() {

        HashMap<String, String> newList = new HashMap<>();
        Cursor cursor = null;

        try {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return newList;

            if (contentResolver == null) contentResolver = context.getContentResolver();
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            cursor = contentResolver.query(
                    uri,
                    new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.VISIBLE, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME},
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        if (cursor.getInt(1) == 1) {
                            newList.put(cursor.getInt(0) + STRING_EMPTY, cursor.getString(2).concat(STRING_2HASH).concat(cursor.getString(3)));
                        }
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }

            return newList;

        } catch (Exception e) {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CALENDARS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return newList;
        }
    }

    Bitmap getContactPhoto(@NonNull String event, boolean showPhotos, boolean forWidget) {

        Bitmap bm = null;

        try {

            if (event.isEmpty()) return null;

            String[] singleEventArray = event.split(Constants.STRING_2HASH);
            String eventSubType = singleEventArray[Position_eventSubType];

            if (eventSubType.equals(eventTypesIDs.get(Type_CalendarEvent))) {
                return BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_other);
            }

            boolean isDeath = eventSubType.equals(eventTypesIDs.get(Type_Death));
            float offsetWidget = forWidget ? (9 * getResources().getDisplayMetrics().density) : 0;

            if (!singleEventArray[Position_contactID].isEmpty() && !singleEventArray[Position_photo_uri].isEmpty() && showPhotos && !singleEventArray[Position_photo_uri].equalsIgnoreCase(STRING_NULL)) {
                //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                if (contentResolver == null) contentResolver = context.getContentResolver();
                Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri, true);
                if (photo_stream != null) {
                    BufferedInputStream buf = new BufferedInputStream(photo_stream);
                    bm = BitmapFactory.decodeStream(buf);
                    buf.close();
                    photo_stream.close();
                }
            }

            if (bm == null) {
                int growAge = 16;
                int elderAge = 65;

                //Если событие - не день рождения, пытаемся достать возраст из дня рождения
                if (!eventSubType.equals(eventTypesIDs.get(Type_BirthDay))) {
                    final Date birthDate = set_events_birthdays.get(singleEventArray[Position_contactID]);
                    Locale locale_en = new Locale(Constants.LANG_EN);
                    SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);
                    Date BDay = sdfYear.parse(singleEventArray[Position_eventDate]);

                    List<String> singleRowList = Arrays.asList(singleEventArray);
                    if (birthDate != null && BDay != null) {
                        //todo: если день рождения без года - мы об этом никак не узнаем. надо придумать что-то
                        singleRowList.set(Position_age, String.valueOf(countYearsDiff(birthDate, BDay)));
                    } else {
                        singleRowList.set(Position_age, String.valueOf(growAge));
                    }
                    singleEventArray = singleRowList.toArray(new String[0]);
                }

                //Случайное фото с соответствиии с возрастом и полом
                Person person = new Person(context, singleEventArray);
                int idPhoto = R.drawable.photo_man01;

                if (person.getGender() == 1 && (person.Age >= 0 && person.Age < growAge) && !isDeath) { //Обойдёмся без силуэтов мёртвых детей
                    idPhoto = R.drawable.photo_boy01;
                } else if (person.getGender() == 1 && person.Age >= 0 && person.Age >= elderAge) {
                    idPhoto = R.drawable.photo_elderman01;
                } else if (person.getGender() == 2 && (person.Age >= 0 && person.Age < growAge) && !isDeath) {
                    idPhoto = R.drawable.photo_girl01;
                } else if (person.getGender() == 2 && person.Age >= 0 && person.Age >= elderAge) {
                    idPhoto = R.drawable.photo_elderwoman01;
                } else if (person.getGender() == 2) {
                    idPhoto = R.drawable.photo_woman01;
                }

                bm = BitmapFactory.decodeResource(getResources(), idPhoto);
            }

            if (!singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CALENDAR) &&
                    set_events_deaths != null &&
                    (preferences_list_sad_photo == 2 || (preferences_list_sad_photo == 1 && isDeath)) &&
                    set_events_deaths.contains(singleEventArray[Position_contactID]))
            {
                //Если контакт умер, добавлять чёрную ленточку
                //https://stackoverflow.com/questions/3089991/how-to-draw-a-shape-or-bitmap-into-another-bitmap-java-android
                Bitmap bmOverlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth((float) bm.getWidth() / 6 /*1 /getResources().getDisplayMetrics().density*/);
                canvas.drawBitmap(bm, new Matrix(), null);
                canvas.drawLine((float) (bm.getWidth() * 1.25), (float) bm.getHeight() / 2, (float) bm.getWidth() / 2, (float) (bm.getHeight() * 1.25), paint);
                bm.recycle();
                bm = bmOverlay;
            }

            //Добавление иконки внутрь фото
            if (!forWidget &&
                    preferences_list_event_info.contains(ContactsEvents.pref_List_EventInfo_FavoritesIcon) &&
                    singleEventArray[Position_starred].equals(STRING_1)) {

                Bitmap bmOverlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bm, new Matrix(), null);
                bm.recycle();
                Bitmap bmStar = BitmapFactory.decodeResource(getResources(), R.drawable.fav_star);
                canvas.drawBitmap(Bitmap.createScaledBitmap(bmStar, bmOverlay.getWidth() / 4, bmOverlay.getHeight() / 4, true), 2 + offsetWidget, bmOverlay.getHeight() - (float)(bmOverlay.getHeight() / 4) - 2 - offsetWidget, null);
                bmStar.recycle();
                bm = bmOverlay;

            }

            return bm;

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACT_PHOTO_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @NonNull String getContactData(@NonNull Long contactId, @NonNull String ColumnName) {

        try {

            if (contactId == 0) return STRING_EMPTY;

            String contactData = STRING_EMPTY;
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

            if (ColumnName.equals(ContactsContract.Contacts.PHOTO_URI)) {
                Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                return dataUri != null ? dataUri.toString() : STRING_EMPTY;
            } else {
                Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
                Cursor dataCursor = contentResolver.query(
                        dataUri,
                        null,
                        ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                        new String[]{ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                        null);
                if (dataCursor != null) {
                    while (dataCursor.moveToNext()) {
                        contactData = dataCursor.getString(dataCursor.getColumnIndex(ColumnName));
                        if (contactData != null && !contactData.isEmpty()) {
                            //исключаем всё, что внутри скобок
                            int pStart = contactData.indexOf(STRING_PARENTHESIS_OPEN);
                            int pEnd = contactData.indexOf(STRING_PARENTHESIS_CLOSE);
                            if (pStart > -1 && pEnd > pStart) {
                                contactData = contactData.substring(0, pStart);
                            }
                            break;
                        }
                    }
                    dataCursor.close();
                }
                return contactData != null ? contactData : STRING_EMPTY;
            }

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACT_DATA_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }

    }

    @NonNull String getContactFullNameShort(@NonNull Long contactId) {

        try {

            if (contactId == 0) return STRING_EMPTY;

            String lastName;
            String firstName;
            String secondName;
            String result = null;
            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Cursor nameCursor = contentResolver.query(
                    dataUri,
                    null,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE },
                    null);
            if (nameCursor != null) {
                while (nameCursor.moveToNext()) {
                    lastName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                    firstName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                    secondName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));

                    final String secondNameWord = (!isEmpty(secondName) ? STRING_SPACE + secondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : STRING_EMPTY);
                    if (!isEmpty(lastName)) {
                        result = lastName + (!isEmpty(firstName) ? STRING_SPACE + firstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : STRING_EMPTY) + secondNameWord;
                    } else if (!isEmpty(firstName)) {
                        result = firstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD + secondNameWord;
                    }

                    if (!isEmpty(result)) break;
                }
                nameCursor.close();
            }
            return result != null ? result : STRING_EMPTY;

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACT_NAME_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }

    }

    @NonNull String getContactPhone(@NonNull Long contactId) {

        try {

            if (contactId == 0) return STRING_EMPTY;

            String phone = STRING_EMPTY;

            //https://stackoverflow.com/questions/8735683/retrieving-a-phone-number-with-contactscontract-in-android-function-doesnt-wo
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Cursor phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,
                    null,
                    null
            );

            if (phoneCursor != null) {
                //todo: сделать получение основного телефона
                if (phoneCursor.moveToFirst()) {
                    phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
                phoneCursor.close();
            }
            return phone != null ? phone : STRING_EMPTY;

        } catch (Exception e) {
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_CONTACT_PHONE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }

    }

    void computeDates() {
        //Вычисляем даты

        if (isEmptyArray()) return;

        long statCurrentModuleStart = System.currentTimeMillis();
        statEventTypes.clear();
        String singleEvent = null;

        try {

            List<String> magicList = new ArrayList<>(); //Для 5k событий

            Locale locale_en = new Locale(Constants.LANG_EN); //Все даты Android хранит в этой локали, типа 11 Jan 1991
            Locale locale_ru = new Locale(Constants.LANG_RU); //Skype хранит даты в той локале, которая указана в приложении Skype
            Locale locale_us = new Locale(Constants.LANG_US); // Jan 11, 1991
            Locale locale_ukr = new Locale(Constants.LANG_UA);
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_JAVA, locale_en);
            SimpleDateFormat skypedf = new SimpleDateFormat(Constants.DATETIME_DD_MMM_YYYY, locale_en);
            SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);
            SimpleDateFormat sdfNoYear = new SimpleDateFormat(Constants.DATETIME_DD_MM, locale_en);
            SimpleDateFormat sdf_ru = new SimpleDateFormat(Constants.DATETIME_RUS, locale_ru);
            SimpleDateFormat sdf_us = new SimpleDateFormat(Constants.DATETIME_US, locale_us);
            SimpleDateFormat sdf_ukr = new SimpleDateFormat(Constants.DATETIME_RUS, locale_ukr);

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());

            setLocale(false);
            final Resources resources = getResources();

            for (int i = 0; i < eventList.size(); i++) {
                long dayDiff = -1;
                boolean isYear = false;
                Date eventDate = null; //оригинальная дата события
                Date BDay = null; //следующая дата события
                int Age = 0;

                singleEvent = eventList.get(i);
                String[] singleEventArray = singleEvent.split(Constants.STRING_2HASH);
                String[] dayArray = singleEventArray[Position_dates].split(Constants.STRING_2TILDA);
                final String eventCaption = singleEventArray[Position_eventCaption];
                final String eventSubType = singleEventArray[Position_eventSubType];

                //перебираем все даты и находим максимальную
                for (String dayValue : dayArray) {
                    String accountType = dayValue.substring(0, dayValue.indexOf(STRING_COLON_SPACE));
                    String storedDate = dayValue.substring(dayValue.indexOf(STRING_COLON_SPACE) + STRING_COLON_SPACE.length());
                    Date storedDate_Date = null;
                    boolean storedDate_isYear = false;

                    if (!statEventTypes.containsKey(accountType)) {
                        statEventTypes.put(accountType, 1);
                    } else {
                        Integer oldCount = statEventTypes.get(accountType);
                        statEventTypes.put(accountType, (oldCount == null ? 0 : oldCount) + 1);
                    }

                    if (accountType.toLowerCase().contains(Objects.requireNonNull(resources).getString(R.string.account_skype))) {

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
                                    try {
                                        storedDate_Date = sdf_us.parse(storedDate);
                                    } catch (ParseException e4) {
                                        //Не получилось распознать
                                    }
                                }
                            }
                        }

                    } else if (accountType.equalsIgnoreCase(resources.getString(R.string.account_vk))) {

                        if (storedDate.startsWith(Constants.STRING_0000)) { //Нет года, формат 0000-mm-dd

                            try {
                                BDay = sdf.parse(now.get(YEAR) + STRING_MINUS + storedDate.substring(5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (BDay != null) {
                                long dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);
                                storedDate_Date = BDay;
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
                        //com.android.huawei.phone

                        if (storedDate.startsWith(STRING_2MINUS) || //Нет года, формат --MM-dd
                                storedDate.startsWith(Constants.STRING_0000) || //Нет года, формат 0000-MM-dd
                                (storedDate.startsWith("1604-") && accountType.equalsIgnoreCase(resources.getString(R.string.account_exchange))) || //Нет года, формат 1604-MM-dd - com.google.android.gm.exchange https://stackoverflow.com/questions/14023390/nsdate-return-1604-for-year-value
                                (storedDate.startsWith("1904-") && accountType.equalsIgnoreCase(resources.getString(R.string.account_huawei))) || //Нет года, формат 1904-MM-dd - com.android.huawei.phone
                                (!isEmpty(eventCaption) && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventCaption.toLowerCase()).find()) //Именины считаем без года
                        ) {

                            try {
                                BDay = sdf.parse(now.get(YEAR) + STRING_MINUS + storedDate.substring(storedDate.startsWith(STRING_2MINUS) ? 2 : 5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (BDay != null) {
                                long dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);
                                storedDate_Date = BDay;
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
                        } else if (storedDate_isYear & (!isYear || countDaysDiff(eventDate, storedDate_Date) > 0)) { //Если у пользователя несколько дат, берём наименьший возраст todo: можно вынести в настройку - в какую сторону округлять
                            eventDate = storedDate_Date;
                            isYear = true;
                        }
                    }
                }

                if (eventDate != null) {

                    if (isYear) { //в eventDate - оригинальное событие

                        Calendar cal = from(eventDate);
                        try {

                            BDay = sdf.parse(now.get(YEAR) + STRING_MINUS + (cal.get(Calendar.MONTH) + 1) + STRING_MINUS + cal.get(Calendar.DAY_OF_MONTH));
                            long dayDiff_tmp;
                            if (BDay != null) {
                                dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);
                            }

                        } catch (ParseException e) {
                            //Не получилось распознать
                        }

                    }

                    if (BDay != null) {
                        dayDiff = countDaysDiff(currentDay, BDay);
                        Age = countYearsDiff(eventDate, BDay); //Считаем, сколько будет лет
                        if (eventSubType.equals(eventTypesIDs.get(Type_BirthDay)))
                            set_events_birthdays.put(singleEventArray[Position_contactID], eventDate);
                    }

                }

                if (dayDiff == -1) {

                    eventList.set(i, STRING_EMPTY);

                } else {

                    singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + dayDiff).substring((Constants.STRING_00 + dayDiff).length() - 3); //Для сортировки
                    singleEventArray[Position_eventDate] = sdfYear.format(BDay); //следующая дата события
                    if (isYear) { //Дата с годом
                        singleEventArray[Position_eventDateText] = sdfYear.format(eventDate); //оригинальное событие
                    } else { //Дата без года
                        singleEventArray[Position_eventDateText] = sdfNoYear.format(eventDate); //оригинальное событие без года
                    }

                    //Если событие в ближайшие 3 дня, то в eventDistance будет <число дней до события>, иначе: "Дней до <тип события>: " +  <число дней до события> + <день недели>
                    singleEventArray[Position_eventDistance] = Long.toString(dayDiff);
                    singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDiff, BDay);

                    if (Age > 0) {
                        singleEventArray[Position_age] = Integer.toString(Age);
                        singleEventArray[Position_age_caption] = getAgeString(
                                Age,
                                R.string.msg_after_year_prefix_1,
                                R.string.msg_after_year_prefix_1_,
                                R.string.msg_after_year_prefix_2_3_4,
                                R.string.msg_after_year_prefix_4_21
                        );

                        if (!eventSubType.equals(eventTypesIDs.get(Type_Death))) {
                            singleEventArray[Position_age_current] = countDaysDiffText(eventDate, currentDay);
                        }
                        if (singleEventArray[Position_eventType].equals(eventTypesIDs.get(Type_Anniversary))) {
                            String anCaption;
                            try {
                                anCaption = context.getString(resources.getIdentifier(Constants.STRING_TYPE_WEDDING + Age, "string", context.getPackageName()));
                            } catch (Resources.NotFoundException nfe) {
                                anCaption = null;
                            }
                            if (anCaption != null && !isEmpty(anCaption) && !eventCaption.contains(STRING_PARENTHESIS_OPEN)) {
                                singleEventArray[Position_eventCaption] = eventCaption.concat(STRING_PARENTHESIS_OPEN).concat(anCaption).concat(STRING_PARENTHESIS_CLOSE);
                            }
                        }
                    } else {
                        singleEventArray[Position_age] = Constants.STRING_MINUS1;
                        singleEventArray[Position_age_caption] = STRING_SPACE;
                        singleEventArray[Position_age_current] = STRING_SPACE;
                    }
                    if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_BirthDay))) {
                        //if (preferences_list_event_info.contains(pref_List_EventInfo_ZodiacSign)) {
                            final String zodiacSign = getZodiacInfo(ContactsEvents.ZodiacInfo.SIGN_TITLE, singleEventArray[Position_eventDateText]);
                            singleEventArray[Position_zodiacSign] = zodiacSign.equals(STRING_EMPTY) ? STRING_SPACE : zodiacSign;
                        //}
                        //if (preferences_list_event_info.contains(pref_List_EventInfo_ZodiacYear)) {
                            final String zodiacYear = getZodiacInfo(ContactsEvents.ZodiacInfo.YEAR_TITLE, singleEventArray[Position_eventDateText]);
                            singleEventArray[Position_zodiacYear] = zodiacYear.equals(STRING_EMPTY) ? STRING_SPACE : zodiacYear;
                        //}
                    }

                    eventList.set(i, TextUtils.join(Constants.STRING_2HASH, singleEventArray));

                    //Вычисляем 5K даты
                    if (Age > 0 && singleEventArray[Position_eventType].equals(eventTypesIDs.get(Type_BirthDay))) {

                        //todo: подумать: надо ли считать 5K для смертей и.т.п.?
                        long days = countDaysDiff(eventDate, currentDay);
                        long k = (days + 365) / 5000;
                        long mdays = (days + 365) % 5000;

                        if (mdays >= 0 && mdays <= 365) {
                            //Формируем новую запись
                            Calendar cal5K = Calendar.getInstance();
                            int magicDayDistance = (int) (365 - mdays);
                            cal5K.add(Calendar.DATE, magicDayDistance);

                            //String[] newDataArray = dataArray[i].split(STRING_2HASH);
                            singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + magicDayDistance).substring((Constants.STRING_00 + magicDayDistance).length() - 3);
                            singleEventArray[Position_eventType] = eventTypesIDs.get(Type_5K);
                            singleEventArray[Position_eventSubType] = eventTypesIDs.get(Type_5K);
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
                            singleEventArray[Position_age_current] = countDaysDiffText(eventDate, currentDay); //Возраст текущий
                            singleEventArray[Position_eventStorage] = STRING_STORAGE_CONTACTS; //Где искать событие по ID

                            magicList.add(TextUtils.join(Constants.STRING_2HASH, singleEventArray));
                        }

                    }
                }

            }

            //Удаляем пустые
            for (int i = eventList.size() - 1; i >=0 ; i--) {
                if (eventList.get(i).isEmpty()) eventList.remove(i);
            }

            //Добавляем 5k+
            if (magicList.size() > 0) {
                eventList.addAll(magicList);
                magicList.clear();
            }

            //Сортируем
            Collections.sort(eventList);

            //Очищаем ненужное
            map_eventsBySubtypeAndPersonID_offset.clear();

            statLastComputeDates = System.currentTimeMillis();
            statTimeComputeDates = statLastComputeDates - statCurrentModuleStart;

        } catch (Exception e) {
            statLastComputeDates = System.currentTimeMillis();
            statTimeComputeDates = statLastComputeDates - statCurrentModuleStart;
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_COMPUTE_DATES_ERROR + e.toString() + (singleEvent != null ? "\n" + singleEvent : STRING_EMPTY), Toast.LENGTH_LONG).show();
        }
    }

    private String getEventDistanceText(long dayDiff, @NonNull Date eventDate){
        //Если событие в ближайшие 3 дня, то вернёт "сегодня", "завтра", "послезавтра", если позже, то "через X дней в " + <день недели>

        StringBuilder eventDistance = new StringBuilder();
        try {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(eventDate);

            if (dayDiff == 0) { //Сегодня
                eventDistance.append(getResources().getString(R.string.msg_today));
            } else if (dayDiff == 1) { //Завтра
                eventDistance.append(getResources().getString(R.string.msg_tomorrow));
            } else if (dayDiff == 2) { //Послезавтра
                eventDistance.append(getResources().getString(R.string.msg_day_after_tomorrow));
            } else {
                if (dayDiff > 0) { //Подальше вперёд
                    eventDistance
                            .append(getResources().getString(R.string.msg_before_event_prefix))
                            .append(getAgeString(
                                    dayDiff,
                                    R.string.msg_after_day_prefix_1,
                                    R.string.msg_after_day_prefix_1_,
                                    R.string.msg_after_day_prefix_2_3_4,
                                    R.string.msg_after_day_prefix_4_21
                            ))
                            .append(Locale.getDefault().getLanguage().equals(getResources().getString(R.string.pref_Language_de)) ? "n" : "") //для немецкого "in 10 TageN"
                            .append(getResources().getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1]);
                } else if (dayDiff == -1) { //Вчера
                    eventDistance.append(getResources().getString(R.string.msg_yesterday));
                } else if (dayDiff == -2) { //Позавчера
                    eventDistance.append(getResources().getString(R.string.msg_before_yesterday));
                } else { //Подальше назад
                    eventDistance
                            .append(getResources().getString(R.string.msg_after_event_prefix))
                            .append(getAgeString(
                                    -dayDiff,
                                    R.string.msg_after_day_prefix_1,
                                    R.string.msg_after_day_prefix_1_,
                                    R.string.msg_after_day_prefix_2_3_4,
                                    R.string.msg_after_day_prefix_4_21
                            ))
                            .append(getResources().getString(R.string.msg_after_event_postfix))
                            .append(getResources().getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_EVENT_DISTANCE_TEXT_ERROR + e.toString() + STRING_EOF + dayDiff + STRING_EOF + eventDate, Toast.LENGTH_LONG).show();
        }
        return  eventDistance.toString();
    }

    List<String> insertPreviousEvents(@NonNull List<String> dataList, @NonNull String params) {

        if (isEmpty(params) || dataList.isEmpty()) return dataList;

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
            Locale locale_en = new Locale(Constants.LANG_EN);
            SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);

            List<String> newList = new ArrayList<>();
            preferences_list_prev_events_found = 0;
            for (int i = dataList.size() - 1; i >= 0 && preferences_list_prev_events_found < params_events; i--) {
                String li = dataList.get(i);
                String[] singleEventArray = li.split(Constants.STRING_2HASH);
                if (!singleEventArray[Position_eventSubType].equals(eventTypesIDs.get(Type_5K)) //пропускаем 5K+
                        && !singleEventArray[Position_eventSubType].equals(eventTypesIDs.get(Type_CalendarEvent)) //пропускаем события календаря
                ) {
                    if (params_days == 365) { //нет ограничения по дням
                        newList.add(li);
                        preferences_list_prev_events_found++;
                    } else {
                        Date eventDate = null;
                        try {
                            eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                            if (eventDate != null) {
                                eventDate = addYear(eventDate, -1);
                            }
                        } catch (Exception e) {
                            //
                        }

                        if (eventDate != null) {
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
                        long dayDistance = countDaysDiff(currentDay, eventDate);
                        singleEventArray[Position_eventDistance] = Long.toString(dayDistance);
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
                            singleEventArray[Position_age_caption] = getAgeString(
                                    Age,
                                    R.string.msg_after_year_prefix_1,
                                    R.string.msg_after_year_prefix_1_,
                                    R.string.msg_after_year_prefix_2_3_4,
                                    R.string.msg_after_year_prefix_4_21
                            );

                            if (singleEventArray[Position_eventType].equals(eventTypesIDs.get(Type_Anniversary))) {
                                String anCaption;
                                try {
                                    anCaption = context.getString(getResources().getIdentifier(Constants.STRING_TYPE_WEDDING + Age, "string", context.getPackageName()));
                                } catch (Resources.NotFoundException nfe) {
                                    anCaption = null;
                                }
                                String eventCaption = getResources().getString(R.string.event_type_anniversary);
                                if (anCaption != null && !anCaption.isEmpty()) {
                                    singleEventArray[Position_eventCaption] = eventCaption.concat(STRING_PARENTHESIS_OPEN).concat(anCaption).concat(STRING_PARENTHESIS_CLOSE);
                                } else {
                                    singleEventArray[Position_eventCaption] = eventCaption;
                                }
                            }
                        } else { //Сейчас идёт первый год жизни

                            singleEventArray[Position_age] = Constants.STRING_MINUS1;
                            singleEventArray[Position_age_caption] = STRING_SPACE;

                        }

                        dataList.add(0, TextUtils.join(Constants.STRING_2HASH, singleEventArray));
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_INSERT_PREVIOUS_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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

                if (preferences_notifications_days.size() > 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

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
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_INIT_NOTIFICATION_CHANNEL_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void initBootReceiver(StringBuilder log) {

        try {

            PackageManager pm = context.getPackageManager();
            ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);

            if (preferences_notifications_days.size() != 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

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
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_INIT_BOOT_RECEIVER_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void initNotifications(StringBuilder log) {

        try {

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (preferences_notifications_days.size() > 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

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
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM);
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
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_INIT_NOTIFICATIONS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void showNotifications(boolean forceNoEventsMessage, String channelId) {
        //https://www.journaldev.com/15468/android-notification-styling

        try {

            Set<String> notifications_days = new HashSet<>(preferences_notifications_days); //За сколько дней уведомлять
            if (preferences_debug_on) Toast.makeText(context, "notify days: " + notifications_days, Toast.LENGTH_LONG).show();
            if (notifications_days.size() == 0) return;

            setLocale(true);

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());
            Locale locale_en = new Locale(Constants.LANG_EN);
            SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);
            SimpleDateFormat sdfDDMM = new SimpleDateFormat(Constants.DATETIME_DD_MM, locale_en);

            List<String> listNotify = new ArrayList<>();
            for (String event: eventList) {
                String[] singleEventArray = event.split(Constants.STRING_2HASH);
                final String eventKey = getEventKey(singleEventArray);
                if (preferences_notifications_event_types.contains(singleEventArray[Position_eventType]) &&
                        (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey)) &&
                        (getSilencedEventsCount() == 0 || !checkIsSilencedEvent(eventKey))) {
                    Date eventDate = null;
                    try {
                        eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                    } catch (Exception e) {/**/}

                    if (eventDate != null) {
                        if (listNotify.size() >= 50) break; //https://stackoverflow.com/questions/33364368/android-system-notification-limit-per-app

                        long countDays = countDaysDiff(currentDay, eventDate);
                        if (countDays > 7) {
                            break;
                        } else if (notifications_days.contains(String.valueOf(countDays))) {
                            listNotify.add(event);
                        }
                    }
                }
            }
            if (listNotify.size() == 0 && !forceNoEventsMessage) return;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            //notificationManager.cancelAll(); //todo: пока уберу, надо разобраться - нужно ли оно здесь?
            Random r = new Random();

            String[] dataNotify = listNotify.toArray(new String[0]); //Список уведомлений, подходящих по дням
            StringBuilder textBig;
            if (dataNotify.length == 0 || //Тестовое уведомление
                    preferences_notifications_type == 0 || //Каждое событие в отдельном уведомлении
                    dataNotify.length >= 3 && preferences_notifications_type == 2 || //Если собыий меньше 3 => отдельные, иначе - общее
                    dataNotify.length >= 4 && preferences_notifications_type == 3 //Если собыий меньше 4 => отдельные, иначе - общее
            ) {

                textBig = new StringBuilder();
                String textSmall;
                if (dataNotify.length > 0) {
                    textSmall = context.getString(R.string.msg_notifications_soon) + dataNotify.length;
                    textBig.append(textSmall).append(":\n");
                    for (String event : dataNotify) {
                        String[] singleEventArray = event.split(Constants.STRING_2HASH);
                        Date eventDate = null;
                        String eventDay = null;
                        try {
                            eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                            if (eventDate != null) {
                                eventDay = sdfDDMM.format(eventDate);
                            }
                        } catch (Exception e) {
                            //
                        }

                        if (eventDate != null) {
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOF);
                            textBig.append(singleEventArray[Position_eventEmoji])
                                    .append(STRING_SPACE)
                                    .append(eventDay).append(STRING_SPACE)
                                    .append(singleEventArray[Position_personFullName]);
                            if (!singleEventArray[Position_age_caption].trim().isEmpty())
                                textBig.append(STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);
                        }
                    }
                } else {
                    textSmall = context.getString(R.string.msg_notifications_soon_no_events);
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
                        .setWhen(0) //https://stackoverflow.com/questions/18249871/android-notification-buttons-not-showing-up/18603076#18603076
                        .setAutoCancel(true);

                if (preferences_notifications_priority > 1) {
                    builder.setOngoing(true);
                    builder.setPriority(NotificationCompat.PRIORITY_MAX);
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    builder.setSound(Uri.parse(preferences_notifications_ringtone));
                }

                notificationManager.notify(defaultNotificationID, builder.build());

            } else { //Несколько отдельных уведомлений

                for (int i = dataNotify.length - 1; i >= 0; i--) {
                    String[] singleEventArray = dataNotify[i].split(Constants.STRING_2HASH);
                    Date eventDate = null;
                    String eventDay = null;
                    try {
                        eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                        if (eventDate != null) {
                            eventDay = sdfDDMM.format(eventDate);
                        }
                    } catch (Exception e) {
                        //
                    }

                    if (eventDate != null) {
                        //if (countDaysDiff(currentDay, eventDate) <= notifications_days) {
                            textBig = new StringBuilder();
                            textBig.append(singleEventArray[Position_eventEmoji])
                                    .append(STRING_SPACE)
                                    .append(eventDay)
                                    .append(STRING_SPACE)
                                    .append(singleEventArray[Position_personFullName]);
                            if (!singleEventArray[Position_age_caption].trim().isEmpty())
                                textBig.append(STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);

                        int notificationID = defaultNotificationID + r.nextInt(100);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                .setColor(this.getResources().getColor(R.color.dark_green))
                                .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                                .setContentText(textBig)
                                .setContentTitle(singleEventArray[Position_eventDistanceText])
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = null;
                            if (singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) && !singleEventArray[Position_contactID].isEmpty()) {
                                uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                            } else if (singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CALENDAR) && !singleEventArray[Position_eventID].isEmpty()) {
                                uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                            }
                            if (uri != null) {
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                                builder.setContentIntent(pendingIntent);
                            }

                            //todo: .addPerson для телефона и почты

                            if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Dial))
                                    && !singleEventArray[Position_eventSubType].equals(eventTypesIDs.get(Type_CalendarEvent))
                                    && !singleEventArray[Position_contactID].isEmpty()
                                    && !getContactPhone(parseToLong(singleEventArray[Position_contactID])).isEmpty()) {

                                Intent intentDial = new Intent(context, AlarmReceiver.class);
                                intentDial.setAction(ACTION_DIAL);
                                intentDial.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
                                intentDial.putExtra(EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                                PendingIntent pendingDial = PendingIntent.getBroadcast(context, defaultNotificationID + r.nextInt(100), intentDial, 0);
                                NotificationCompat.Action actionDial = new NotificationCompat.Action(0, context.getString(R.string.button_dial), pendingDial);
                                builder.addAction(actionDial);

                            }

                            if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Silent))) {
                                Intent intentSilent = new Intent(context, AlarmReceiver.class);
                                intentSilent.setAction(ACTION_SILENT);
                                intentSilent.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
                                intentSilent.putExtra(EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                                PendingIntent pendingSilent = PendingIntent.getBroadcast(context, defaultNotificationID + r.nextInt(100), intentSilent, 0);
                                NotificationCompat.Action actionSilent = new NotificationCompat.Action(0, context.getString(R.string.button_silent), pendingSilent);
                                builder.addAction(actionSilent);
                            }

                            if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Hide))) {
                                Intent intentHide = new Intent(context, AlarmReceiver.class);
                                intentHide.setAction(ACTION_HIDE);
                                intentHide.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
                                intentHide.putExtra(EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                                PendingIntent pendingHide = PendingIntent.getBroadcast(context, defaultNotificationID + r.nextInt(100), intentHide, 0);
                                NotificationCompat.Action actionHide = new NotificationCompat.Action(0, context.getString(R.string.button_hide), pendingHide);
                                builder.addAction(actionHide);
                            }

                            if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Remind))) {
                                Intent intentSnooze = new Intent(context, AlarmReceiver.class);
                                intentSnooze.setAction(ACTION_SNOOZE);
                                intentSnooze.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
                                intentSnooze.putExtra(EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                                PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, defaultNotificationID + r.nextInt(100), intentSnooze, 0);
                                NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                                builder.addAction(actionSnooze);
                            }

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                builder.setSound(Uri.parse(preferences_notifications_ringtone));
                            }

                            builder.setLargeIcon(getContactPhoto(dataNotify[i], true, false));

                            if (preferences_notifications_priority > 2) {
                                builder.setOngoing(true);
                                builder.setPriority(NotificationCompat.PRIORITY_MAX);
                            }

                            notificationManager.notify(notificationID, builder.build());

                       // } else {
                       //     break;
                      //  }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SHOW_NOTIFICATIONS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    String getEventKey(@NonNull String[] singleEventArray) {

        try {

            if (singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) && !singleEventArray[Position_contactID].isEmpty()) {
                return singleEventArray[Position_contactID] + Constants.STRING_2HASH + singleEventArray[Position_eventSubType];
            } else if (singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CALENDAR) && !singleEventArray[Position_eventID].isEmpty()) {
                return singleEventArray[Position_eventID] + Constants.STRING_2HASH + singleEventArray[Position_eventSubType];
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_EVENT_KEY_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return STRING_EMPTY;
    }

    private String[] getKeyParts(@NonNull String eventKey) {
        return eventKey.split(Constants.STRING_2HASH);
    }

    void snoozeNotification(@NonNull String dataNotify, int snoozeHours, Date wakeDateTime) {

        try {

            if (isEmpty(dataNotify) || (snoozeHours <= 0 && wakeDateTime == null)) return;

            Random r = new Random();
            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            alarmIntent.setAction(Constants.ACTION_NOTIFY);
            alarmIntent.putExtra(EXTRA_NOTIFICATION_DATA, dataNotify);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, defaultNotificationID + r.nextInt(100), alarmIntent, 0); //PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long currentTimeMillis = System.currentTimeMillis();
            long nextUpdateTimeMillis;
            boolean isSnoozed = false;
            if (snoozeHours > 0) {
                nextUpdateTimeMillis = currentTimeMillis + snoozeHours * 60 * DateUtils.MINUTE_IN_MILLIS; //* DateUtils.HOUR_IN_MILLIS;
                isSnoozed = true;
            } else { //if (wakeDateTime != null)
                nextUpdateTimeMillis = wakeDateTime.getTime();
            }
            Time nextUpdateTime = new Time();
            nextUpdateTime.set(nextUpdateTimeMillis);

            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
                }
                SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, Locale.US);
                Toast.makeText(context, context.getString(isSnoozed ? R.string.msg_snoozed_until : R.string.msg_notify_time, sdf.format(nextUpdateTimeMillis)), Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SNOOZE_NOTIFICATION_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    void showNotification(String dataNotify, String channelId) {

        try {

            if (dataNotify == null || dataNotify.isEmpty()) return;

            //Toast.makeText(context, "TEST: " + dataNotify, Toast.LENGTH_LONG).show();

            Random r = new Random();

            String[] singleEventArray = dataNotify.split(Constants.STRING_2HASH);
            Date eventDate = null;
            String eventDay = null;
            try {
                Locale locale_en = new Locale(Constants.LANG_EN);
                SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);
                SimpleDateFormat sdfDDMM = new SimpleDateFormat(Constants.DATETIME_DD_MM, locale_en);
                eventDate = sdfYear.parse(singleEventArray[Position_eventDate]);
                if (eventDate != null) {
                    eventDay = sdfDDMM.format(eventDate);
                }
            } catch (Exception e) {
                //
            }

            if (eventDate != null) {
                StringBuilder textBig = new StringBuilder();
                textBig.append(singleEventArray[Position_eventEmoji])
                        .append(STRING_SPACE)
                        .append(eventDay)
                        .append(STRING_SPACE)
                        .append(singleEventArray[Position_personFullName]);
                if (!singleEventArray[Position_age_caption].trim().isEmpty())
                    textBig.append(": ").append(singleEventArray[Position_age_caption]);

                int notificationID = defaultNotificationID + r.nextInt(100);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                        .setColor(this.getResources().getColor(R.color.dark_green))
                        .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                        .setContentText(textBig)
                        .setContentTitle(singleEventArray[Position_eventDistanceText])
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = null;
                if (singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) && !singleEventArray[Position_contactID].isEmpty()) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CALENDAR) && !singleEventArray[Position_eventID].isEmpty()) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                }
                if (uri != null) {
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                    builder.setContentIntent(pendingIntent);
                }

                Intent intentSnooze = new Intent(context, AlarmReceiver.class);
                intentSnooze.setAction(ACTION_SNOOZE); //todo: добавить все кнопки
                intentSnooze.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
                intentSnooze.putExtra(EXTRA_NOTIFICATION_DATA, dataNotify);
                PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, notificationID, intentSnooze, 0);
                NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                builder.addAction(actionSnooze);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    builder.setSound(Uri.parse(preferences_notifications_ringtone));
                }

                builder.setLargeIcon(getContactPhoto(dataNotify, true, false));
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(notificationID, builder.build());

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SHOW_NOTIFICATION_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    boolean isEmptyArray() {return eventList.isEmpty(); /*dataArray == null || dataArray.length == 0;*/}

    void clearHiddenEvents() {

        try {

            if (getHiddenEventsCount() > 0) preferences_hiddenEvents.clear();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_CLEAR_HIDDEN_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    int getHiddenEventsCount() {

        try {

            return preferences_hiddenEvents == null || preferences_hiddenEvents.isEmpty() ? 0 : preferences_hiddenEvents.size();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_HIDDEN_EVENTS_COUNT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    boolean checkIsHiddenEvent(@NonNull String key) {

        try {

            return preferences_hiddenEvents != null && preferences_hiddenEvents.contains(key);

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_CHECK_IS_HIDDEN_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    boolean setHiddenEvent(@NonNull String key) {

        try {

            if (preferences_hiddenEvents == null || !preferences_hiddenEvents.add(key)) return false;

            clearNobodyHiddenEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, "Hided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_HIDDEN_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

    boolean unsetHiddenEvent(@NonNull String key) {

        try {

            if (!checkIsHiddenEvent(key)) return false;
            if (preferences_hiddenEvents == null || !preferences_hiddenEvents.remove(key)) return false;

            clearNobodyHiddenEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);

            //Если удалили последнее событие - скидываем режим на стандартный
            if (preferences_events_scope == Constants.pref_Events_Scope_Hidden && preferences_hiddenEvents.isEmpty()) {
                preferences_events_scope = Constants.pref_Events_Scope_NotHidden;
                editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_events_scope);
            }

            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, "Unhided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_UNSET_HIDDEN_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

    void clearSilencedEvents() {

        try {

            if (getSilencedEventsCount() > 0) preferences_silentEvents.clear();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_CLEAR_SILENCED_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    int getSilencedEventsCount() {

        try {

            return preferences_silentEvents == null || preferences_silentEvents.isEmpty() ? 0 : preferences_silentEvents.size();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_SILENT_EVENTS_COUNT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    boolean checkIsSilencedEvent(@NonNull String key) {

        try {

            return preferences_silentEvents != null && preferences_silentEvents.contains(key);

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_CHECK_IS_SILENT_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    boolean setSilencedEvent(@NonNull String key) {

        try {

            if (preferences_silentEvents == null || !preferences_silentEvents.add(key)) return false;

            clearNobodySilencedEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, "Silenced event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_SILENT_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

    boolean unsetSilencedEvent(@NonNull String key) {

        try {

            if (!checkIsSilencedEvent(key)) return false;
            if (preferences_silentEvents == null || !preferences_silentEvents.remove(key)) return false;

            clearNobodySilencedEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);

            //Если удалили последнее событие - скидываем режим на стандартный
            if (preferences_events_scope == Constants.pref_Events_Scope_Silenced && preferences_silentEvents.isEmpty()) {
                preferences_events_scope = Constants.pref_Events_Scope_NotHidden;
                editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_events_scope);
            }

            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, "Unsilenced event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_UNSET_SILENT_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

    @NonNull
    String getMergedID(@NonNull String linkID){

        try {

            final String element =  preferences_mergedIDs.get(linkID);
            if (element == null || element.isEmpty()) {
                return STRING_EMPTY;
            } else {
                return element;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_MERGED_ID_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }
    }

    boolean setMergedID(@NonNull String ID, String IDtoLink) {
        try {

            if (preferences_mergedIDs.get(ID) != null) {
                if (IDtoLink == null) { //Удаляем существующий
                    preferences_mergedIDs.remove(ID);
                } else { //Заменяем
                    preferences_mergedIDs.remove(ID);
                    preferences_mergedIDs.put(ID, IDtoLink);
                }
            } else if (IDtoLink != null) { //Добавляем новый
                preferences_mergedIDs.put(ID, IDtoLink);
            } else {
                return false;
            }

            Set<String> someSets = new HashSet<>();
            int i = 0;
            for (String elementID: preferences_mergedIDs.keySet()) {
                if (preferences_mergedIDs.get(elementID) != null) {
                    someSets.add(elementID + STRING_COLON_SPACE + preferences_mergedIDs.get(elementID));
                    if (preferences_debug_on) Toast.makeText(context, (++i) + ". " + elementID + STRING_COLON_SPACE + preferences_mergedIDs.get(elementID), Toast.LENGTH_LONG).show();
                }
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_MergedID_key), someSets);
            editor.apply();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_MERGED_ID_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void clearNobodySilencedEvents() {

        try {

            if (getSilencedEventsCount() == 0 || set_contacts_ids == null) return;

            Set<String> toRemove = new HashSet<>();
            for (String event: preferences_silentEvents) {
                final String[] keyParts = getKeyParts(event);
                if (
                        !keyParts[1].equals(eventTypesIDs.get(Type_CalendarEvent)) &&
                                !(set_contacts_ids.contains(keyParts[0]) || set_events_ids.contains(keyParts[0]))
                ) toRemove.add(event);
            }
            if (toRemove.size() > 0) {
                if (preferences_debug_on) Toast.makeText(context, "Cleared silenced events for absent contacts: " + toRemove.size(), Toast.LENGTH_LONG).show();
                preferences_silentEvents.removeAll(toRemove);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, CONTACTS_EVENTS_CLEAR_UNEXISTING_SILENCED_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearNobodyHiddenEvents() {

        try {

            if (getHiddenEventsCount() == 0 || set_contacts_ids == null) return;

            Set<String> toRemove = new HashSet<>();
            for (String event: preferences_hiddenEvents) {
                final String[] keyParts = getKeyParts(event);
                if (
                        !keyParts[1].equals(eventTypesIDs.get(Type_CalendarEvent)) &&
                                !(set_contacts_ids.contains(keyParts[0]) || set_events_ids.contains(keyParts[0]))
                ) toRemove.add(event);
            }
            if (toRemove.size() > 0) {
                if (preferences_debug_on) Toast.makeText(context, "Cleared hidden events for absent contacts: " + toRemove.size(), Toast.LENGTH_LONG).show();
                preferences_hiddenEvents.removeAll(toRemove);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, CONTACTS_EVENTS_CLEAR_UNEXISTING_HIDDEN_EVENTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void setWidgetPreference(int id, @NonNull String value) {

        if (context == null) return;

        try {

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(context.getString(R.string.widget_config_PrefName) + id, value);
            editor.apply();

            if (preferences_debug_on) Toast.makeText(context, String.format(Constants.MSG_WIDGET_PREFS_SAVED, id) + value, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_WIDGET_PREFERENCE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    List<String> getWidgetPreference(int id) {

        if (context == null) return null;
        List<String> defaultPref = Arrays.asList(context.getString(R.string.widget_config_defaultPref).split(STRING_COMMA));

        try {

            //if (preferences_debug_on) Toast.makeText(context, String.format(Constants.MSG_WIDGET_PREFS_DATA, id) + pref, Toast.LENGTH_SHORT).show();

            String strPref = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.widget_config_PrefName) + id, context.getString(R.string.widget_config_defaultPref));
            if (strPref == null) strPref = context.getString(R.string.widget_config_defaultPref);
            String[] pref = strPref.split(STRING_COMMA);
            List<String> prefWidget = new ArrayList<>(Arrays.asList(pref));
            //prefWidget.addAll(Arrays.asList(pref));

            //Добиваем дефолтными значениями
            while (prefWidget.size() < defaultPref.size()) {
                prefWidget.add(defaultPref.get(prefWidget.size() - 1));
            }

            return prefWidget;

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_GET_WIDGET_PREFERENCE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return defaultPref;
        }

    }

    boolean hasPreferences(String name) {
        return PreferenceManager.getDefaultSharedPreferences(context).getAll().containsKey(name);
    }

    void removeWidgetPreference(int id) {

        try {

            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(context.getString(R.string.widget_config_PrefName) + id).apply();
            if (preferences_debug_on) Toast.makeText(context, String.format(Constants.MSG_WIDGET_PREFS_REMOVED, id), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_REMOVE_WIDGET_PREFERENCE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    Set<String> getPreferences_Accounts() {
        return preferences_Accounts;
    }

    void setPreferences_Accounts(Set<String> preferences_Accounts) {
        this.preferences_Accounts = preferences_Accounts;
    }

    Set<String> getPreferences_Calendars(String eventType) {

        if (eventType.equals(eventTypesIDs.get(Type_BirthDay))) {
            return preferences_Calendars_BirthDay;
        } else if (eventType.equals(eventTypesIDs.get(Type_Other))) {
            return preferences_Calendars_Other;
        } else {
            return new HashSet<>();
        }

    }
    void setPreferences_Calendars(String eventType, Set<String> preferences_Calendars) {

        if (eventType.equals(eventTypesIDs.get(Type_BirthDay))) {
            this.preferences_Calendars_BirthDay = preferences_Calendars;
        } else if (eventType.equals(eventTypesIDs.get(Type_Other))) {
            this.preferences_Calendars_Other = preferences_Calendars;
        }

    }

    void showAnniversaryList(Context context) {

        try {

            ArrayList<String> items = new ArrayList<>();
            for(int i = 1; i <= 100; i++) {
                String anCaption;
                try {
                    anCaption = context.getString(getResources().getIdentifier(Constants.STRING_TYPE_WEDDING + i, "string", context.getPackageName()));
                } catch (Resources.NotFoundException nfe) {
                    anCaption = null;
                }
                if (anCaption != null && !anCaption.equals(Constants.STRING_EMPTY)) items.add(i + Constants.STRING_COLON_SPACE + anCaption);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_CustomEvents_Anniversary_List_description)
                    .setIcon(R.drawable.ic_event_wedding)
                    .setItems(items.toArray(new CharSequence[0]), null)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();

            TypedArray ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
            alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.CONTACTS_EVENTS_SHOW_ANNIVERSARY_LIST_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    String setHTMLColor(String msg, int color) {
        try {

            int colorId;
            switch (color) {
                case HTML_COLOR_RED:
                    colorId = R.color.dark_red;
                    break;
                case HTML_COLOR_YELLOW:
                    colorId = R.color.yellow;
                    break;
                case HTML_COLOR_BROWN:
                    colorId = R.color.brown;
                    break;
                default:
                    /*int[] attrs = {android.R.attr.textColorSecondary};
                    TypedArray ta = context.obtainStyledAttributes(attrs);
                    colorId = ta.getColor(0, 0);
                    ta.recycle();*/
                    return msg;
            }
            return String.format(HTML_COLOR, Integer.toHexString(ContextCompat.getColor(context, colorId) & 0x00ffffff), msg);

        } catch (Exception e) {
            e.printStackTrace();
            if (preferences_debug_on) Toast.makeText(context, Constants.CONTACTS_EVENTS_SET_HTML_COLOR_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return msg;
    }

    static String normalizeName(String inName) {

        if (inName == null) {
            return null;
        } else {
            String normalName = inName.toLowerCase().replace(",", "");
            if (normalName.contains("ё")) {
                normalName = normalName.replace("ё", "е");
            }
            if (normalName.contains("é")) {
                normalName = normalName.replace("é", "e");
            }
            return normalName;
        }
    }

    @NonNull
    static Long parseToLong(String strIn) {

        try {
            return Long.parseLong(strIn);
        } catch (NumberFormatException e) {
            return Long.parseLong("0");
        }

    }

    enum ZodiacInfo{
        SIGN,
        SIGN_TITLE,
        YEAR,
        YEAR_TITLE
    }

    @NonNull
    String getZodiacInfo(ZodiacInfo requestInfo, String strBirthday) {

        try {

            if (requestInfo == ZodiacInfo.SIGN || requestInfo == ZodiacInfo.SIGN_TITLE) {
                final int eventDay = Integer.parseInt(strBirthday.substring(0, 2));
                final int eventMonth = Integer.parseInt(strBirthday.substring(3, 5));

                return (eventMonth != 12 || eventDay < 23) && (eventMonth != 1 || eventDay > 20) ?
                        eventMonth != 1 && (eventMonth != 2 || eventDay > 19) ?
                                eventMonth != 2 && (eventMonth != 3 || eventDay > 20) ?
                                        eventMonth != 3 && (eventMonth != 4 || eventDay > 20) ?
                                                eventMonth != 4 && (eventMonth != 5 || eventDay > 21) ?
                                                        eventMonth != 5 && (eventMonth != 6 || eventDay > 21) ?
                                                                eventMonth != 6 && (eventMonth != 7 || eventDay > 22) ?
                                                                        eventMonth != 7 && (eventMonth != 8 || eventDay > 21) ?
                                                                                eventMonth != 8 && (eventMonth != 9 || eventDay > 23) ?
                                                                                        eventMonth != 9 && (eventMonth != 10 || eventDay > 23) ?
                                                                                                eventMonth != 10 && (eventMonth != 11 || eventDay > 22) ?
                                                                                                        eventMonth != 11 && eventMonth != 12 ?
                                                                                                                "" :
                                                                                                                "♐".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_sagittarius) : STRING_EMPTY) :
                                                                                                        "♏".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_scorpio) : STRING_EMPTY) :
                                                                                                "♎".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_libra) : STRING_EMPTY) :
                                                                                        "♍".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_virgo) : STRING_EMPTY) :
                                                                                "♌".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_leo) : STRING_EMPTY) :
                                                                        "♋".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_cancer) : STRING_EMPTY) :
                                                                "♊".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_gemini) : STRING_EMPTY) :
                                                        "♉".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_taurus) : STRING_EMPTY) :
                                                "♈".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_aries) : STRING_EMPTY) :
                                        "♓".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_pisces) : STRING_EMPTY) :
                                "♒".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_aquarius) : STRING_EMPTY) :
                        "♑".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_capricorn) : STRING_EMPTY);

            } else if (requestInfo == ZodiacInfo.YEAR || requestInfo == ZodiacInfo.YEAR_TITLE) {

                final int eventYear = Integer.parseInt("0" + strBirthday.substring(6));
                if (eventYear == 0) return STRING_EMPTY;

                switch (eventYear % 12) {
                    case 0: return "\uD83D\uDC12".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_monkey) : STRING_EMPTY);
                    case 1: return "\uD83D\uDC13".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rooster) : STRING_EMPTY);
                    case 2: return "\uD83D\uDC15".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_dog) : STRING_EMPTY);
                    case 3: return "\uD83D\uDC16".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_pig) : STRING_EMPTY);
                    case 4: return "\uD83D\uDC00".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rat) : STRING_EMPTY);
                    case 5: return "\uD83D\uDC02".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_ox) : STRING_EMPTY);
                    case 6: return "\uD83D\uDC05".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_tiger) : STRING_EMPTY);
                    case 7: return "\uD83D\uDC07".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rabbit) : STRING_EMPTY);
                    case 8: return "\uD83D\uDC09".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_dragon) : STRING_EMPTY);
                    case 9: return "\uD83D\uDC0D".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_snake) : STRING_EMPTY);
                    case 10: return "\uD83D\uDC0E".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_horse) : STRING_EMPTY);
                    case 11: return "\uD83D\uDC11".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_sheep) : STRING_EMPTY);
                    default: return STRING_EMPTY;
                }

            } else {

                return STRING_EMPTY;

            }

        } catch (Exception e) {
            e.printStackTrace();
            return STRING_EMPTY;
        }
    }

}
