/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 17.09.2022, 22:55
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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
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

class ContactsEvents {

    private static final String TAG = "ContactsEvents";
    private static final ContactsEvents ourInstance = new ContactsEvents();

    @NonNull static ContactsEvents getInstance() {
        return ourInstance;
    }

    private ContactsEvents() {}

    public Context getContext() {
        return context;
    }

    void setContext(@NonNull Context con) {
        context = con;
        resources = con.getResources();
        DisplayMetrics_density = resources.getDisplayMetrics().density;
    }

    @NonNull
    public Resources getResources() {return this.resources != null ? this.resources : (this.resources = context.getResources());}

    private final Handler handler = new Handler(Looper.getMainLooper());

    //Константы
    final Set<String> prefs_EventTypes_Default = new HashSet<String>() {{
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM));
    }};

    //todo: убрать дублирование тут и в ресурсах (тут вычистить)
    static final String pref_List_EventInfo_Photo = "6";
    static final String pref_List_EventInfo_JobTitle = "1";
    static final String pref_List_EventInfo_Organization = "2";
    static final String pref_List_EventInfo_EventCaption = "3";
    static final String pref_List_EventInfo_EventIcon = "12";
    static final String pref_List_EventInfo_FavoritesIcon = "5";
    static final String pref_List_EventInfo_ZodiacSign = "9";
    static final String pref_List_EventInfo_ZodiacYear = "10";

    final private Set<String> pref_List_Event_Info = new HashSet<String>() {{
        add(pref_List_EventInfo_Photo);
        add(pref_List_EventInfo_JobTitle);
        add(pref_List_EventInfo_Organization);
        add(pref_List_EventInfo_EventCaption);
        add(pref_List_EventInfo_EventIcon);
        add(pref_List_EventInfo_FavoritesIcon);
    }};

    static final String pref_Widgets_EventInfo_Photo = "1";
    static final String pref_Widgets_EventInfo_EventIcon = "2";
    static final String pref_Widgets_EventInfo_FavoritesIcon = "3";
    static final String pref_Widgets_EventInfo_ZodiacSign = "4";
    static final String pref_Widgets_EventInfo_ZodiacYear = "5";
    static final String pref_Widgets_EventInfo_SilencedIcon = "6";
    static final String pref_Widgets_EventInfo_Border = "10";
    static final String pref_Widgets_EventInfo_Organization = "20";
    static final String pref_Widgets_EventInfo_JobTitle = "21";

    //в общих настройках не используются
    static final String pref_Widgets_EventInfo_EventCaption = "7";
    static final String pref_Widgets_EventInfo_Age = "16";

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
    static final int Position_eventURL = 27;
    static final int Position_attrAmount = 28; //MAX

    static final HashMap<Integer, String> eventTypesIDs = new HashMap<Integer, String>() {{
        put(Constants.Type_BirthDay, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY));
        put(Constants.Type_Anniversary, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY));
        put(Constants.Type_Other, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER));
        put(Constants.Type_Custom, Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM));
        put(Constants.Type_5K, "11");
        put(Constants.Type_Death, "12");
        put(Constants.Type_NameDay, "13");
        put(Constants.Type_Crowning, "14");
        put(Constants.Type_Custom1, "15");
        put(Constants.Type_Custom2, "16");
        put(Constants.Type_Custom3, "17");
        put(Constants.Type_Custom4, "18");
        put(Constants.Type_Custom5, "19");
        put(Constants.Type_CalendarEvent, "20");
        put(Constants.Type_FileEvent, "21");
    }};

    final List<String> eventList = new ArrayList<>(); //Список всех событий
    List<String> eventListUnsorted = new ArrayList<>(); //Несортированный список

    private String currentLocale = Constants.STRING_EMPTY;
    int currentTheme = 0;
    final String systemLocale = Locale.getDefault().getLanguage();
    final HashSet<String> set_events_deaths = new HashSet<>(); //ID контактов с годовщиной смерти
    final HashMap<String, Date> set_events_birthdays = new HashMap<>(); //дни рождения
    final HashSet<String> set_contacts_ids = new HashSet<>(); //ID всех контактов в адресной книге
    final HashSet<String> set_events_ids = new HashSet<>(); //ID всех найденных событий календаря
    final HashMap<String, String> map_contacts_names = new HashMap<>(); //связка имён контактов с ID
    final HashMap<String, String> map_calendars = new HashMap<>(); //список всех календарей
    final HashMap<String, Integer> map_eventsBySubtypeAndPersonID_offset = new HashMap<>(); //индекс события до сортировки (или для eventListUnsorted)
    final HashMap<String, String> map_organizations = new HashMap<>();
    final HashMap<String, String> map_contacts_titles = new HashMap<>();
    final HashMap<String, String> map_contacts_aliases = new HashMap<>();
    final HashMap<String, String> map_events_weblinks = new HashMap<>();
    private final HashMap<String, String> map_contacts_data = new HashMap<>(); //кеш данных о контактах
    final Random generator = new Random();
    boolean needUpdateEventList = false;

    //Настройки
    boolean preferences_debug_on;
    boolean preferences_extrafun;
    String preferences_language;
    int preferences_list_events_scope;
    boolean preferences_menustyle_compact;
    Set<String> preferences_list_event_types;
    Set<String> preferences_list_event_info;
    String preferences_list_prev_events;
    private int preferences_list_sad_photo;
    String preferences_list_custom_caption;
    int preferences_list_style;
    int preferences_list_photostyle;
    int preferences_list_filling;

    int preferences_list_nameformat;
    int preferences_list_dateformat;
    int preferences_list_color_eventtoday;
    int preferences_list_color_eventsoon;
    int preferences_list_color_eventjubilee;
    int preferences_list_on_click_action;
    int preferences_list_magnify_distance;
    int preferences_list_magnify_name;
    int preferences_list_magnify_details;
    int preferences_list_magnify_date;
    int preferences_list_magnify_age;
    boolean preference_list_fastscroll;

    int preferences_widgets_update_period;
    Set<String> preferences_widgets_event_info;
    String preferences_widgets_bottom_info;
    String preferences_widgets_bottom_info_2nd;
    int preferences_widgets_days_eventsoon;
    int preferences_widgets_color_default;
    int preferences_widgets_color_eventtoday;
    int preferences_widgets_color_eventsoon;
    int preferences_widgets_color_eventfar;
    int preferences_widgets_on_click_action;

    String preferences_first_names_female_custom;
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
    private Matcher preferences_otherevent_labels;

    String preferences_birthday_calendars_rules;
    private boolean preferences_birthday_calendars_useyear;
    Set<String> preferences_Birthday_files;
    Set<String> preferences_Otherevent_files;
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

    int preferences_notification_channel_id;
    Set<String> preferences_notifications_days;
    /* preferences_notifications_type:
     *   0 - Одно общее уведомление
     *   1 - Каждое событие в отдельном уведомлении
     *   2 - Если собыий меньше 3 => отдельные, иначе - общее
     *   3 - Если собыий меньше 4 => отдельные, иначе - общее
     * */
    private int preferences_notifications_type;
    private int preferences_notifications_priority;
    private Set<String> preferences_notifications_event_types;
    private Set<String> preferences_notifications_quick_actions;
    int preferences_notifications_alarm_hour;
    int preferences_notifications_alarm_minute;
    String preferences_notifications_ringtone;
    int preferences_notifications_on_click_action;

    String preferences_quiz_interface;

    //https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
    final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;

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
    Set<String> preferences_BirthDay_calendars = new HashSet<>();
    private Set<String> preferences_Otherevent_calendars = new HashSet<>();
    private int preferences_IconPackNumber;
    final Map<Integer, Integer> preferences_IconPackImages_M = new TreeMap<>();
    final Map<Integer, Integer> preferences_IconPackImages_F = new TreeMap<>();

    //Статистика
    long statTimeGetContactEvents = 0;
    long statTimeGetCalendarEvents = 0;
    long statTimeGetFileEvents = 0;
    long statTimeComputeDates = 0;
    long statLastComputeDates = 0;
    int statContactsEventCount = 0;
    int statCalendarsEventCount = 0;
    int statFilesEventCount = 0;
    int statContactsTitleCount = 0;
    int statContactsOrganizationCount = 0;
    int statContactsNicknameCount = 0;
    int statContactsCount = 0;
    int statContactsURLCount = 0;
    int statEventsCount = 0;
    final HashMap<String, Integer> statEventTypes = new HashMap<>();
    long statLastPausedForOtherActivity = 0;
    int statEventsPrevEventsFound = 0;
    //int temp_int = 0;

    //UI объекты
    private Context context;
    private Resources resources;
    float DisplayMetrics_density;
    private ContentResolver contentResolver;
    boolean isUIopen = false;
    float dimen_List_details;
    float dimen_List_name;
    float dimen_list_date;

    //Даты
    //todo: подумать про массивы https://tproger.ru/translations/java-tips-and-tricks-for-begginer/
    //final Locale locale_en = new Locale(Constants.LANG_EN); //Все даты Android хранит в этой локали, типа 11 Jan 1991
    final Locale locale_ru = new Locale(Constants.LANG_RU); //Skype хранит даты в той локале, которая указана в приложении Skype
    //final Locale locale_us = new Locale(Constants.LANG_US); // Jan 11, 1991
    final Locale locale_ukr = new Locale(Constants.LANG_UA);
    final SimpleDateFormat sdf_java = new SimpleDateFormat(Constants.DATE_JAVA, Locale.US);
    final SimpleDateFormat sdf_java_G = new SimpleDateFormat(Constants.DATE_JAVA_G, Locale.US);
    final SimpleDateFormat sdf_java_no_year = new SimpleDateFormat(Constants.DATE_JAVA_NO_YEAR, Locale.US);
    final SimpleDateFormat sdf_skype = new SimpleDateFormat(Constants.DATE_DD_MMM_YYYY, Locale.US);
    final SimpleDateFormat sdf_DDMMY = new SimpleDateFormat(Constants.DATE_DD_MM_Y, Locale.US);
    final SimpleDateFormat sdf_DDMMYYYY = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, Locale.US);
    final SimpleDateFormat sdf_DDMMYYYY_G = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY_G, Locale.US);
    final SimpleDateFormat sdf_DDMMYYYYHHMM = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, Locale.US);
    final SimpleDateFormat sdf_DDMM = new SimpleDateFormat(Constants.DATE_DD_MM, Locale.US);
    final SimpleDateFormat sdf_MMMMDYYYY = new SimpleDateFormat(Constants.DATE_MMMM_D_YYYY, Locale.US);
    final SimpleDateFormat sdf_ru = new SimpleDateFormat(Constants.DATE_RUS, locale_ru);
    final SimpleDateFormat sdf_us = new SimpleDateFormat(Constants.DATE_US, Locale.US);
    final SimpleDateFormat sdf_ukr = new SimpleDateFormat(Constants.DATE_RUS, locale_ukr);
    final SimpleDateFormat sdf_uk = new SimpleDateFormat(Constants.DATE_UK, Locale.UK);
    final SimpleDateFormat sdf_uk_G = new SimpleDateFormat(Constants.DATE_UK_G, Locale.UK);
    final SimpleDateFormat sdf_uk_no_year = new SimpleDateFormat(Constants.DATE_UK_NO_YEAR, Locale.UK);
    final SimpleDateFormat sdf_india = new SimpleDateFormat(Constants.DATE_IND, Locale.UK);
    final SimpleDateFormat sdf_india_G = new SimpleDateFormat(Constants.DATE_IND_G, Locale.UK);
    final SimpleDateFormat sdf_india_no_year = new SimpleDateFormat(Constants.DATE_IND_NO_YEAR, Locale.UK);
    final SimpleDateFormat sdf_YYYYMMDD_noDiv = new SimpleDateFormat(Constants.DATE_NO_DIV, Locale.UK);

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

    private static Calendar getCalendarFromDate(@NonNull Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c;
    }

    private static Calendar removeTime(@NonNull Calendar c) {

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c;
    }

    long countDaysDiff(@NonNull Date date1, @NonNull Date date2) {
        //https://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances/43681941#43681941

        try {

            boolean isNegative = false;
            Calendar c1 = removeTime(getCalendarFromDate(date1));
            Calendar c2 = removeTime(getCalendarFromDate(date2));

            if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)) {
                return c2.get(Calendar.DAY_OF_YEAR) - c1.get(Calendar.DAY_OF_YEAR);
            }
            // ensure c1 <= c2
            if (c1.get(Calendar.YEAR) > c2.get(Calendar.YEAR)) {
                isNegative = true;
                Calendar c = c1;
                c1 = c2;
                c2 = c;
            }
            int y1 = c1.get(Calendar.YEAR);
            int y2 = c2.get(Calendar.YEAR);
            int d1 = c1.get(Calendar.DAY_OF_YEAR);
            int d2 = c2.get(Calendar.DAY_OF_YEAR);

            int minorYearSign = c1.get(Calendar.ERA) == GregorianCalendar.AD ? 1 : -1;

            int resD = d2 + ((y2 - minorYearSign*y1) * 365) - d1;
            if (isNegative) {
                return -(resD + countLeapYearsBetween(minorYearSign*y1, y2));
            } else {
                return resD + countLeapYearsBetween(minorYearSign*y1, y2);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    /**
     *
     * @param dateFrom date from
     * @param dateTo date to
     * @param components 1 - only DMY, 2 - only days count, 3 - "DMY (days count)"
     * @return distance between two days in locale text format
     */
    String countDaysDiffText(@NonNull Date dateFrom, @NonNull Date dateTo, int components) {

        try {

            StringBuilder eventDistance = new StringBuilder();
            long daysDiff = 0;

            //если включить desugaring https://www.youtube.com/watch?v=heCvGfOGH0s то размер приложения +200К
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                //https://stackoverflow.com/questions/4710206/calculate-age-in-years-months-days-hours-minutes-and-seconds
                LocalDate dateStart = dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate dateEnd = dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                Period p;
                if (dateStart.isBefore(dateEnd)) {
                    p = Period.between(dateStart, dateEnd);
                } else {
                    p = Period.between(dateEnd, dateStart);
                }

                if (components == 1 || components == 3) {
                    if (dateStart.isBefore(dateEnd)) {
                        daysDiff = ChronoUnit.DAYS.between(dateStart, dateEnd);
                    } else {
                        daysDiff = ChronoUnit.DAYS.between(dateEnd, dateStart);
                    }

                    if (p.getYears() > 0) {
                        eventDistance
                                .append(getAgeString(
                                        p.getYears(),
                                        R.string.msg_after_year_prefix_1,
                                        R.string.msg_after_year_prefix_1_,
                                        R.string.msg_after_year_prefix_2_3_4,
                                        R.string.msg_after_year_prefix_4_21
                                ))
                                .append(Constants.STRING_SPACE);
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
                                .append(Constants.STRING_SPACE);
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
                                .append(Constants.STRING_SPACE);
                    }
                }

            } else {

                if (components == 1 || components == 3) {
                    Calendar calendarDateFrom;
                    Calendar calendarDateTo;
                    if (dateFrom.before(dateTo)) {
                        calendarDateFrom = removeTime(getCalendarFromDate(dateFrom));
                        calendarDateTo = removeTime(getCalendarFromDate(dateTo));
                    } else {
                        calendarDateTo = removeTime(getCalendarFromDate(dateFrom));
                        calendarDateFrom = removeTime(getCalendarFromDate(dateTo));
                    }

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

                    long delta = yearTo - yearFrom - (daysFromNYTo < daysFromNYFrom ? 1 : 0);
                    if (delta > 0) {
                        eventDistance
                                .append(getAgeString(
                                        delta,
                                        R.string.msg_after_year_prefix_1,
                                        R.string.msg_after_year_prefix_1_,
                                        R.string.msg_after_year_prefix_2_3_4,
                                        R.string.msg_after_year_prefix_4_21
                                ))
                                .append(Constants.STRING_SPACE);
                    }
                    final int dayOfMonthTo = calendarDateTo.get(Calendar.DAY_OF_MONTH);
                    final int dayOfMonthFrom = calendarDateFrom.get(Calendar.DAY_OF_MONTH);
                    if (daysFromNYFrom > daysFromNYTo) {
                        delta = 12 - calendarDateFrom.get(Calendar.MONTH) + calendarDateTo.get(Calendar.MONTH) - (dayOfMonthFrom > dayOfMonthTo ? 1 : 0);
                    } else {
                        delta = calendarDateTo.get(Calendar.MONTH) - calendarDateFrom.get(Calendar.MONTH);
                    }
                    if (delta > 0) {
                        eventDistance
                                .append(getAgeString(
                                        delta,
                                        R.string.msg_after_month_prefix_1,
                                        R.string.msg_after_month_prefix_1_,
                                        R.string.msg_after_month_prefix_2_3_4,
                                        R.string.msg_after_month_prefix_4_21
                                ))
                                .append(Constants.STRING_SPACE);
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
                        eventDistance
                                .append(getAgeString(
                                        delta,
                                        R.string.msg_after_day_prefix_1,
                                        R.string.msg_after_day_prefix_1_,
                                        R.string.msg_after_day_prefix_2_3_4,
                                        R.string.msg_after_day_prefix_4_21
                                ))
                                .append(Constants.STRING_SPACE);
                    }
                }
            }

            //(X days)
            if (components == 3) {
                eventDistance.append(Constants.STRING_PARENTHESIS_START);
            }
            if (components == 2 || components == 3) {
                eventDistance.append(getAgeString(
                        daysDiff,
                        R.string.msg_after_day_prefix_1,
                        R.string.msg_after_day_prefix_1_,
                        R.string.msg_after_day_prefix_2_3_4,
                        R.string.msg_after_day_prefix_4_21
                ));
            }
            if (components == 3) {
                eventDistance.append(Constants.STRING_PARENTHESIS_CLOSE);
            }

            return eventDistance.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    private int countYearsDiff(@NonNull Date date1, @NonNull Date date2) {
        try {

            Calendar c1;
            Calendar c2;

            if (date2.after(date1)) {
                c1 = removeTime(getCalendarFromDate(date1));
                c2 = removeTime(getCalendarFromDate(date2));
            } else {
                c1 = removeTime(getCalendarFromDate(date2));
                c2 = removeTime(getCalendarFromDate(date1));
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
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    private Date addYear(@NonNull Date date, int year) {
        try {
            Calendar c = getCalendarFromDate(date);
            c.add(Calendar.YEAR, year);
            return c.getTime();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return date;
        }
    }

    @NonNull
    private String getAgeString(long age, int id_prefix_1, int id_prefix_1_, int id_prefix_2_3_4, int id_prefix_4_21) {

        try {

            StringBuilder result = new StringBuilder();
            String count_str = Long.toString(age);
            String count_end = count_str.substring(count_str.length() - 1);
            boolean isEnd234 = count_end.equals(Constants.STRING_2) || count_end.equals(Constants.STRING_3) || count_end.equals(Constants.STRING_4);

            result.append(age);

            long ageMinus100 = age;
            while (ageMinus100 > 100) {ageMinus100 = ageMinus100 - 100;}

            if (ageMinus100 == 1) { //Единственное число
                result.append(getResources().getString(id_prefix_1));
            } else if (ageMinus100 > 4 && ageMinus100 < 21) {
                result.append(getResources().getString(id_prefix_4_21));
            } else if (count_end.equals(Constants.STRING_1)) { //Если заканчивается на 1, но не между 5-20
                result.append(getResources().getString(id_prefix_1_));
            } else if (isEnd234) { //Если заканчивается на 2, 3, 4
                result.append(getResources().getString(id_prefix_2_3_4));
            } else {
                result.append(getResources().getString(id_prefix_4_21));
            }
            return result.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    /** Инициализация и считывание настроек из SharedPreferences
     */
    void getPreferences() {

        if (context == null) return;

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            //https://medium.com/@anupamchugh/a-nightmare-with-shared-preferences-and-stringset-c53f39f1ef52
            //https://stackoverflow.com/questions/19949182/android-sharedpreferences-string-set-some-items-are-removed-after-app-restart

            preferences_debug_on = preferences.getBoolean(context.getString(R.string.pref_Help_Debug_On_key), false);
            preferences_extrafun = preferences.getBoolean(context.getString(R.string.pref_Help_ExtraFun_On_key), false);
            preferences_language = getPreferenceString(preferences, context.getString(R.string.pref_Language_key), context.getString(R.string.pref_Language_default));
            preferences_IconPackNumber = getPreferenceInt(preferences, context.getString(R.string.pref_IconPack_key), 0);
            initIconPack();
            preferences_menustyle_compact = preferences.getBoolean(context.getString(R.string.pref_MenuStyle_key), resources.getBoolean(R.bool.pref_MenuStyle_default));

            preferences_list_event_types = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_List_Events_key),
                    prefs_EventTypes_Default);
            preferences_list_event_info = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_List_EventInfo_key),
                    pref_List_Event_Info);
            preferences_list_prev_events = getPreferenceString(preferences, context.getString(R.string.pref_List_PrevEvents_key), context.getString(R.string.pref_List_PrevEvents_default));
            preferences_list_style = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_Style_key), context.getString(R.string.pref_List_Style_default)));
            preferences_list_photostyle = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_PhotoStyle_key), context.getString(R.string.pref_List_PhotoStyle_default)));
            preferences_list_filling = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_Filling_key), context.getString(R.string.pref_List_Filling_default)));
            preferences_list_sad_photo = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_SadPhoto_key), context.getString(R.string.pref_List_SadPhoto_default)));
            preferences_list_nameformat = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default)));
            preferences_list_dateformat = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_DateFormat_key), context.getString(R.string.pref_List_DateFormat_default)));
            preferences_list_custom_caption = getPreferenceString(preferences, context.getString(R.string.pref_List_CustomCaption_key), Constants.STRING_EMPTY);
            preferences_list_color_eventtoday = getPreferenceInt(preferences, getResources().getString(R.string.pref_List_Color_EventToday_key), getResources().getColor(R.color.pref_List_Color_EventToday_default));
            preferences_list_color_eventsoon = getPreferenceInt(preferences, getResources().getString(R.string.pref_List_Color_EventSoon_key), getResources().getColor(R.color.pref_List_Color_EventSoon_default));
            preferences_list_color_eventjubilee = getPreferenceInt(preferences, getResources().getString(R.string.pref_List_Color_EventJubilee_key), getResources().getColor(R.color.pref_List_Color_EventJubilee_default));
            preferences_list_on_click_action = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_List_OnClick_key), context.getString(R.string.pref_List_OnClick_default)));
            preferences_list_magnify_distance = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Distance_key), 0);
            preferences_list_magnify_name = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Name_key), 0);
            preferences_list_magnify_details = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Details_key), 0);
            preferences_list_magnify_date = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Date_key), 0);
            preferences_list_magnify_age = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Age_key), 0);
            preference_list_fastscroll = preferences.getBoolean(context.getString(R.string.pref_List_FastScroll_key), resources.getBoolean(R.bool.pref_List_FastScroll_default));

            preferences_widgets_event_info = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Widgets_EventInfo_key),
                    pref_Widgets_EventInfo_Info);
            preferences_widgets_bottom_info = getPreferenceString(preferences, context.getString(R.string.pref_Widgets_BottomInfo_key), context.getString(R.string.pref_Widgets_BottomInfo_default));
            preferences_widgets_bottom_info_2nd = getPreferenceString(preferences, context.getString(R.string.pref_Widgets_BottomInfo2nd_key), context.getString(R.string.pref_Widgets_BottomInfo2nd_default));
            preferences_widgets_days_eventsoon = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Widgets_Days_EventSoon_key), context.getString(R.string.pref_Widgets_Days_EventSoon_default)));
            preferences_widgets_update_period = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Widgets_UpdateInterval_key), context.getString(R.string.pref_Widgets_UpdateInterval_default)));
            preferences_widgets_on_click_action = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Widgets_OnClick_key), context.getString(R.string.pref_Widgets_OnClick_default)));
            preferences_widgets_color_eventtoday = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventToday_key), getResources().getColor(R.color.pref_Widgets_Color_EventToday_default));
            preferences_widgets_color_eventsoon = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventSoon_key), getResources().getColor(R.color.pref_Widgets_Color_EventSoon_default));
            preferences_widgets_color_eventfar = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventFar_key), getResources().getColor(R.color.pref_Widgets_Color_EventFar_default));
            preferences_widgets_color_default = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventCaption_key), getResources().getColor(R.color.pref_Widgets_Color_EventCaption_default));
            preferences_list_events_scope = getPreferenceInt(preferences, context.getString(R.string.pref_Events_Scope), Constants.pref_Events_Scope_NotHidden);
            preferences_notification_channel_id = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_ChannelID), Constants.defaultNotificationID);
            preferences_quiz_interface = getPreferenceString(preferences, getResources().getString(R.string.pref_Quiz_Interface_key), Constants.STRING_EMPTY);
            if (preferences_quiz_interface.isEmpty()) {
                preferences_quiz_interface = getResources().getString(Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Build.VERSION.SDK_INT > Build.VERSION_CODES.R ? R.string.pref_Quiz_Interface_Dialog : R.string.pref_Quiz_Interface_Notify);
                preferences.edit().putString(context.getString(R.string.pref_Quiz_Interface_key), preferences_quiz_interface).apply();
            }

            //Определения событий

            boolean useInternal;
            String customLabels;
            final String regex_inter = "|"; //"\\Z|";
            //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

            //День рождения
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && TextUtils.isEmpty(customLabels)) {
                preferences_birthday_labels = null;
            } else {
                if (customLabels.isEmpty())
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                else if (!useInternal) {
                    preferences_birthday_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }
            preferences_birthday_calendars_rules = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_default));
            if (TextUtils.isEmpty(preferences_birthday_calendars_rules)) {
                preferences_birthday_calendars_rules = context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_default);
            }

            preferences_birthday_calendars_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_default)));
            preferences_Birthday_files = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key),
                    new HashSet<>());

            //Свадьба
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Anniversary_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_wedding_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_wedding_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Именины
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_NameDay_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_nameday_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_nameday_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Венчание
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Crowning_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_crowning_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_crowning_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Годовщина смерти
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Death_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_death_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_death_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Другие события
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Other_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (customLabels.isEmpty()) {
                preferences_otherevent_labels = null;
            } else {
                preferences_otherevent_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
            }
            preferences_Otherevent_files = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_CustomEvents_Other_LocalFiles_key),
                    new HashSet<>());

            //Пользовательские события
            //1
            preferences_customevent1_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom1_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent1_enabled = false;

            if (!preferences_customevent1_caption.isEmpty()) {
                String preferences_customevent1_labels_str = getPreferenceString(preferences,context.getString(R.string.pref_CustomEvents_Custom1_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent1_labels_str.isEmpty()) {
                    try {
                        preferences_customevent1_labels = Pattern.compile(preferences_customevent1_labels_str.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent1_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent1_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom1_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //2
            preferences_customevent2_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom2_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent2_enabled = false;

            if (!preferences_customevent2_caption.isEmpty()) {
                String preferences_customevent2_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom2_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent2_labels_str.isEmpty()) {
                    try {
                        preferences_customevent2_labels = Pattern.compile(preferences_customevent2_labels_str.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent2_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent2_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom2_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //3
            preferences_customevent3_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom3_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent3_enabled = false;

            if (!preferences_customevent3_caption.isEmpty()) {
                String preferences_customevent3_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom3_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent3_labels_str.isEmpty()) {
                    try {
                        preferences_customevent3_labels = Pattern.compile(preferences_customevent3_labels_str.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent3_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent3_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom3_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //4
            preferences_customevent4_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom4_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent4_enabled = false;

            if (!preferences_customevent4_caption.isEmpty()) {
                String preferences_customevent4_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom4_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent4_labels_str.isEmpty()) {
                    try {
                        preferences_customevent4_labels = Pattern.compile(preferences_customevent4_labels_str.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent4_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent4_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom4_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //5
            preferences_customevent5_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom5_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent5_enabled = false;

            if (!preferences_customevent5_caption.isEmpty()) {
                String preferences_customevent5_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom5_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent5_labels_str.isEmpty()) {
                    try {
                        preferences_customevent5_labels = Pattern.compile(preferences_customevent5_labels_str.replace(Constants.STRING_COMMA, regex_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent5_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent5_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom5_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //Уведомления
            preferences_notifications_days = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Notifications_Days_key),
                    new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_Days_values_default))));
            preferences_notifications_days.removeAll(new HashSet<String>() {{add(Constants.STRING_EMPTY);}});

            preferences_notifications_type = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Notifications_Type_key), context.getString(R.string.pref_Notifications_Type_default)));
            preferences_notifications_priority = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Notifications_Priority_key), context.getString(R.string.pref_Notifications_Priority_default)));
            preferences_notifications_alarm_hour = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Notifications_AlarmHour_key), context.getString(R.string.pref_Notifications_AlarmHour_default)));
            if (preferences_notifications_alarm_hour < 0) preferences_notifications_alarm_hour = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmHour_default));
            preferences_notifications_alarm_minute = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Notifications_AlarmMinute_key), context.getString(R.string.pref_Notifications_AlarmMinute_default)));
            if (preferences_notifications_alarm_minute < 0) preferences_notifications_alarm_minute = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmMinute_default));
            preferences_notifications_ringtone = getPreferenceString(preferences, context.getString(R.string.pref_Notifications_Ringtone_key), Settings.System.DEFAULT_NOTIFICATION_URI.toString());
            preferences_notifications_event_types = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Notifications_Events_key),
                    preferences_list_event_types //По-умолчанию берём из списка событий
            );
            preferences_notifications_quick_actions = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Notifications_QuickActions_key),
                    new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_QuickActions_values_default)))
            );
            preferences_notifications_on_click_action = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Notifications_OnClick_key), context.getString(R.string.pref_Notifications_OnClick_default)));
            preferences_first_names_female_custom = getPreferenceString(preferences, context.getString(R.string.pref_Female_Names_key), Constants.STRING_EMPTY);

            //Запоминаем информацию о темах
            preferences_theme = new MyTheme();
            try {
                preferences_theme.prefNumber = Integer.parseInt(getPreferenceString(preferences, context.getString(R.string.pref_Theme_key), context.getString(R.string.pref_Theme_default)));
            } catch (ClassCastException e) {
                preferences_theme.prefNumber = Integer.parseInt(context.getString(R.string.pref_Theme_default));
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
                case 5:
                    preferences_theme.themeMain = R.style.AppTheme_BlueGrey_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_BlueGrey_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_BlueGrey;
                    break;
            }

            preferences_hiddenEvents = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Events_Hidden_key),
                    new HashSet<>());

            preferences_silentEvents = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Events_Silent_key),
                    new HashSet<>());

            preferences_Accounts = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_Accounts_key),
                    new HashSet<>());

            preferences_BirthDay_calendars = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key),
                    new HashSet<>());

            preferences_Otherevent_calendars = getPreferenceStringSet(preferences,
                    context.getString(R.string.pref_CustomEvents_Other_Calendars_key),
                    new HashSet<>());

            for(String element: getPreferenceStringSet(preferences, context.getString(R.string.pref_MergedID_key), new HashSet<>())) {
                int indexDiv = element.indexOf(Constants.STRING_COLON_SPACE);
                if (indexDiv > -1){
                    preferences_mergedIDs.put(element.substring(0, indexDiv), element.substring(indexDiv + Constants.STRING_COLON_SPACE.length()));
                }
            }

            dimen_List_details = resources.getDimension(R.dimen.event_details);
            dimen_List_name = resources.getDimension(R.dimen.event_name);
            dimen_list_date = resources.getDimension(R.dimen.event_date);

        } catch (Exception e){
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    /** Сохранение настроек в SharedPreferences
     */
    void setPreferences() {

        if (context == null) return;

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
            editor.putInt(context.getString(R.string.pref_Notifications_ChannelID), preferences_notification_channel_id);
            editor.putString(context.getString(R.string.pref_Notifications_AlarmHour_key), Integer.toString(preferences_notifications_alarm_hour));
            editor.putString(context.getString(R.string.pref_Notifications_AlarmMinute_key), Integer.toString(preferences_notifications_alarm_minute));
            editor.putString(context.getString(R.string.pref_Notifications_Ringtone_key), preferences_notifications_ringtone);
            editor.putStringSet(context.getString(R.string.pref_Accounts_key), getPreferences_Accounts());
            editor.putInt(context.getString(R.string.pref_IconPack_key), preferences_IconPackNumber);
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key), preferences_BirthDay_calendars);
            editor.putString(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), preferences_birthday_calendars_rules);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_Calendars_key), preferences_Otherevent_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_LocalFiles_key), preferences_Otherevent_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key), preferences_Birthday_files);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Distance_key), preferences_list_magnify_distance);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Name_key), preferences_list_magnify_name);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Details_key), preferences_list_magnify_details);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Date_key), preferences_list_magnify_date);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Age_key), preferences_list_magnify_age);

            editor.apply();

        } catch (Exception e){
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /** Установка языка (локали) приложению
     *
     * @param force Принудительно, даже если этот язык уже устанавливали ранее
     */
    void setLocale(boolean force) {

        if (context == null) return;

        //сделать так: https://stackoverflow.com/questions/39705739/android-n-change-language-programmatically/
        //для Android > N переделать выбор локали https://stackoverflow.com/questions/47165311/how-to-change-android-o-oreo-api-26-app-language
        //посмотреть https://stackoverflow.com/questions/9475589/how-to-get-string-from-different-locales-in-android и сделать нормальным переключение языков
        try {

            // http://developer.alexanderklimov.ru/android/locale.php
            if (force || !preferences_language.equals(currentLocale)) {

                Configuration configuration = context.getResources().getConfiguration();
                Locale locale;
                if (preferences_language.equals(context.getString(R.string.pref_Language_default))) {
                    locale = new Locale(systemLocale);
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
                DisplayMetrics_density = resources.getDisplayMetrics().density;
                resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                currentLocale = preferences_language;

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    synchronized boolean getEvents(Context in_context) {

        if (in_context != null) context = in_context;
        if (context == null) context = getContext().getApplicationContext();
        if (context == null) return false;

        try {

            eventList.clear();
            eventListUnsorted.clear();
            map_organizations.clear();
            map_contacts_titles.clear();
            map_contacts_aliases.clear();
            map_contacts_data.clear();
            set_contacts_ids.clear();
            set_events_deaths.clear();
            set_events_birthdays.clear();
            map_events_weblinks.clear();
            set_events_ids.clear();
            map_contacts_names.clear();
            map_calendars.clear();
            map_eventsBySubtypeAndPersonID_offset.clear();

            statEventTypes.clear();
            statEventsCount = 0;
            statContactsEventCount = 0;
            statCalendarsEventCount = 0;
            statFilesEventCount = 0;
            statContactsTitleCount = 0;
            statContactsOrganizationCount = 0;
            statContactsNicknameCount = 0;
            statContactsURLCount = 0;
            statTimeGetContactEvents = 0;
            statTimeGetCalendarEvents = 0;
            statTimeGetFileEvents = 0;

            needUpdateEventList = false;

            getPreferences();

            //todo: сделать через отдельный thread, но сделать это опциональным
            //https://stackoverflow.com/questions/61252550/android-how-to-use-kotlin-coroutine-in-java
            //https://stackoverflow.com/questions/58767733/the-asynctask-api-is-deprecated-in-android-11-what-are-the-alternatives

            return getContactsEvents()
                    | getCalendarEvents(eventTypesIDs.get(Constants.Type_BirthDay))
                    | getCalendarEvents(eventTypesIDs.get(Constants.Type_Other))
                    | getFileEvents(eventTypesIDs.get(Constants.Type_BirthDay))
                    | getFileEvents(eventTypesIDs.get(Constants.Type_Other));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

            //Организации и должности
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
                        if (!map_organizations.containsKey(personID) && organization != null && !organization.isEmpty()) map_organizations.put(personID, organization);

                        String title = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Organization.TITLE));
                        if (!map_contacts_titles.containsKey(personID) && title != null && !title.isEmpty()) map_contacts_titles.put(personID, title);

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statContactsOrganizationCount = map_organizations.size();
            statContactsTitleCount = map_contacts_titles.size();
            cache.clear();

            //Псевдонимы
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
                            if (!map_contacts_aliases.containsKey(personID)) map_contacts_aliases.put(personID, nick);
                            //todo: добавлять ники в map_contacts_names
                        }

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statContactsNicknameCount = map_contacts_aliases.size();
            cache.clear();

            //Web ссылки
            final String[] projectionURL = new String[] {
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Website.URL
            };
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionURL,
                    ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE + "'",
                    null,
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Data.CONTACT_ID));
                        String URL = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Website.URL));
                        if (URL != null && !URL.isEmpty()) {
                            if (!map_events_weblinks.containsKey(personID)) {
                                map_events_weblinks.put(personID, URL);
                            } else {
                                String URlstored = map_events_weblinks.get(personID);
                                if (!TextUtils.isEmpty(URlstored)) map_events_weblinks.put(personID, URlstored.concat(Constants.STRING_2TILDA).concat(URL));
                            }
                            statContactsURLCount++;
                        }

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            cache.clear();

            //Контакты
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
            statContactsCount = set_contacts_ids.size();
            cache.clear();

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

            //События
            final String[] projectionContactsEvents = new String[] {
                    ContactsContract.CommonDataKinds.Event.DATA,
                    ContactsContract.CommonDataKinds.Event.TYPE,
                    Constants.ColumnNames_ACCOUNT_TYPE,
                    Constants.ColumnNames_ACCOUNT_NAME,
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
            String eventKey = Constants.STRING_EMPTY;
            statEventsCount+= cursor.getCount();

            if (cursor.moveToFirst()) {
                do {
                    try {

                        statContactsEventCount++;
                        eventKey = addContactEventToEventList(cursor, userData, dataList, cache, map_organizations, map_contacts_titles, map_contacts_aliases, eventKey);

                    } catch (RuntimeException e) {
                        countErrors++;
                        if (preferences_debug_on && countErrors < 3) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(getMethodName(3)).append(Constants.STRING_COLON_SPACE).append(e).append(Constants.STRING_EOL);
                            for(String name: cursor.getColumnNames()) {
                                String data = cursor.getString(cache.getColumnIndex(cursor, name));
                                if (data != null && !data.equals(Constants.STRING_0)) sb.append(name).append(Constants.STRING_COLON_SPACE).append(data).append(Constants.STRING_EOL);
                            }
                            ToastExpander.showText(context, sb.toString());
                        }
                    }
                } while (cursor.moveToNext());

                if (!userData.isEmpty()) { // Данные последнего контакта

                    dataRow = new StringBuilder();
                    int rNum = 0;
                    for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                        rNum++;
                        if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                        dataRow.append(entry.getValue());
                    }

                    if (dataList.add(dataRow.toString())) { //Добавляем для поиска календарных событий (дни рождения)
                        String personID = userData.get(Position_contactID);
                        if (personID != null && !personID.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + Constants.STRING_EOT + personID, dataList.size() - 1);
                        //String personNameAlt = userData.get(Position_personFullNameAlt);
                        //if (personNameAlt != null && !personNameAlt.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + normalizeName(personNameAlt), dataList.size() - 1);
                    }

                    userData.clear();

                }
            }
            cache.clear();
            cursor.close();

            eventList.addAll(dataList);
            dataList.clear();
            statTimeGetContactEvents = System.currentTimeMillis() - statCurrentModuleStart;

            if (preferences_debug_on && countErrors > 1) {
                ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + resources.getString(R.string.msg_errors_total) + countErrors);
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    @NonNull private String addContactEventToEventList(@NonNull Cursor cursor, @NonNull TreeMap<Integer, String> userData, @NonNull List<String> dataList, @NonNull ColumnIndexCache cache, @NonNull HashMap<String, String> orgMap, @NonNull HashMap<String, String> titleMap, @NonNull HashMap<String, String> nickMap, @NonNull String eventKey) {

        String eventDate = null;
        String eventType = null;
        String accountKey = null;

        try {
            StringBuilder dataRow;
            eventDate = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.DATA));
            eventType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));
            String eventSubType = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE)));
            String accountType = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_TYPE));
            String accountName = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_NAME));
            accountKey = accountName + Constants.STRING_PARENTHESIS_OPEN + accountType + Constants.STRING_PARENTHESIS_CLOSE;

            if (eventDate != null && eventType != null && (preferences_Accounts.isEmpty() || preferences_Accounts.contains(accountKey))) {

                String contactName = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME)));
                String contactNameAlt = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)));
                String eventLabel = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.LABEL)));
                boolean nonemptyEventLabel = !TextUtils.isEmpty(eventLabel);
                String eventCaption = Constants.STRING_EMPTY;
                int eventIcon = 0;
                String eventEmoji = "📆";

                if (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay)) ||
                        (nonemptyEventLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_birthday);
                    eventIcon = R.drawable.ic_event_birthday; //https://icons8.com/icon/21460/birthday
                    eventEmoji = "🎂";
                    eventSubType = eventTypesIDs.get(Constants.Type_BirthDay);

                } else if (eventType.equals(eventTypesIDs.get(Constants.Type_Anniversary)) ||
                        (nonemptyEventLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_anniversary);
                    eventIcon = R.drawable.ic_event_wedding; //https://www.flaticon.com/free-icon/wedding-rings_224802
                    eventEmoji = "💑";
                    eventSubType = eventTypesIDs.get(Constants.Type_Anniversary);

                } else if (eventType.equals(eventTypesIDs.get(Constants.Type_Other)) ||
                        (nonemptyEventLabel && preferences_otherevent_labels != null && preferences_otherevent_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_other);
                    eventIcon = R.drawable.ic_event_other; //https://icons8.com/icon/set/event/office
                    eventEmoji = "🗓️";
                    eventSubType = eventTypesIDs.get(Constants.Type_Other);

                } else if (nonemptyEventLabel) {

                    if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent1_caption;
                        eventIcon = R.drawable.ic_event_custom1;
                        eventEmoji = "🗓️";
                        eventSubType = eventTypesIDs.get(Constants.Type_Custom1);
                        if (!preferences_customevent1_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else  if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent2_caption;
                        eventIcon = R.drawable.ic_event_custom2;
                        eventEmoji = "🔔";
                        eventSubType = eventTypesIDs.get(Constants.Type_Custom2);
                        if (!preferences_customevent2_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent3_caption;
                        eventIcon = R.drawable.ic_event_custom3;
                        eventEmoji = "⏰";
                        eventSubType = eventTypesIDs.get(Constants.Type_Custom3);
                        if (!preferences_customevent3_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent4_caption;
                        eventIcon = R.drawable.ic_event_custom4;
                        eventEmoji = "❤️";
                        eventSubType = eventTypesIDs.get(Constants.Type_Custom4);
                        if (!preferences_customevent4_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent5_caption;
                        eventIcon = R.drawable.ic_event_custom5;
                        eventEmoji = "🎁";
                        eventSubType = eventTypesIDs.get(Constants.Type_Custom5);
                        if (!preferences_customevent5_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_nameday);
                        eventIcon = R.drawable.ic_event_nameday;
                        eventEmoji = "🎈";
                        eventSubType = eventTypesIDs.get(Constants.Type_NameDay);

                    } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_crowning);
                        eventIcon = R.drawable.ic_event_crowning; //https://iconscout.com/icon/wedding-destination-romance-building-emoj-symbol
                        eventEmoji = "💒";
                        eventSubType = eventTypesIDs.get(Constants.Type_Crowning);

                    } else if (preferences_death_labels != null && preferences_death_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_death);
                        eventIcon = R.drawable.ic_event_death;
                        //https://emojipedia.org/google/android-6.0.1/new/
                        eventEmoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? "⚰️" : "\uD83D\uDCC5";
                        eventSubType = eventTypesIDs.get(Constants.Type_Death);
                        set_events_deaths.add(cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID))); //Запоминаем событие контакта

                    }

                }
                if (nonemptyEventLabel && eventCaption.isEmpty()) eventCaption = eventLabel;

                String eventKey_next = contactName.concat(Constants.STRING_COMMA).concat(eventType);

                //Наименование события в ключе только для пользовательских событий
                if (eventType.equals(eventTypesIDs.get(Constants.Type_Custom))) {
                    eventKey_next = eventKey_next.concat(Constants.STRING_COMMA).concat(eventLabel);
                }

                String newEventDate = accountType + Constants.STRING_COLON_SPACE + eventDate;

                if (!eventKey_next.equalsIgnoreCase(eventKey)) { //Начало данных нового контакта

                    if (!userData.isEmpty()) { // Уже есть накопленные данные. Нужно сохранить всё, что накопили и обнулить UserData
                        dataRow = new StringBuilder();
                        int rNum = 0;
                        for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                            rNum++;
                            if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                            dataRow.append(entry.getValue());
                        }
                        if (dataList.add(dataRow.toString())) { //Добавляем для поиска календарных событий (дни рожденя)
                            String personID = userData.get(Position_contactID);
                            if (personID != null && !personID.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + Constants.STRING_EOT + personID, dataList.size() - 1);
                            //String personNameAlt = userData.get(Position_personFullNameAlt);
                            //if (personNameAlt != null && !personNameAlt.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + normalizeName(personNameAlt), dataList.size() - 1);
                        }
                        userData.clear();
                    }

                    String contactID = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID));
                    if (contactID == null) return eventKey;
                    String contactFIO = contactName.replace(Constants.STRING_COMMA_SPACE, Constants.STRING_SPACE);

                    String contactTitle = titleMap.get(contactID);
                    if (contactTitle != null && !contactTitle.isEmpty()) {

                        //всё, что внутри скобок в имени - в должность
                        int pStartFirst = contactFIO.indexOf(Constants.STRING_PARENTHESIS_START);
                        int pStartLast = contactFIO.lastIndexOf(Constants.STRING_PARENTHESIS_START);
                        int pEndFirst = contactFIO.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                        int pEndLast = contactFIO.lastIndexOf(Constants.STRING_PARENTHESIS_CLOSE);

                        if (pStartFirst > -1 && pEndFirst > pStartFirst) { //хотя бы пара скобок
                            if (pStartFirst == pStartLast && pEndFirst == pEndLast) { //одна пара скобок

                                contactTitle = contactFIO.substring(pStartFirst + 1, pEndFirst);
                                contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                                userData.put(Position_title, contactTitle);

                            } else if (pStartLast < pEndFirst && pStartLast < pEndLast) { //скобки внутри скобок

                                contactTitle = contactFIO.substring(pStartFirst + 1, pEndLast);
                                contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                                userData.put(Position_title, contactTitle);

                            } else if (pEndFirst < pStartLast) { //пара скобок за другой парой

                                contactTitle = contactFIO.substring(pStartLast + 1, pEndLast);
                                contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                                userData.put(Position_title, contactTitle);

                            }
                        }

                    } else {
                        contactTitle = Constants.STRING_EMPTY;
                    }

                    eventKey = eventKey_next;

                    userData.put(Position_personFullName, contactFIO);
                    userData.put(Position_personFullNameAlt, contactNameAlt.replace(Constants.STRING_COMMA_SPACE, Constants.STRING_SPACE));
                    userData.put(Position_contactID, contactID);
                    userData.put(Position_photo_uri, cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.PHOTO_URI)));
                    userData.put(Position_eventCaption, eventCaption); //Наименование события
                    //подпорка: почему-то для одиноких Skype событий в eventLabel находится дата события
                    userData.put(Position_eventLabel, !eventLabel.equals(eventCaption) & !newEventDate.contains(eventLabel) ? eventLabel : Constants.STRING_EMPTY); //Заголовок пользовательского события
                    userData.put(Position_eventType, eventType); //Тип события
                    userData.put(Position_eventSubType, eventSubType); //Подтип события
                    userData.put(Position_organization, checkForNull(orgMap.get(contactID)));
                    userData.put(Position_title, contactTitle);
                    userData.put(Position_dates, newEventDate);
                    userData.put(Position_eventIcon, Integer.toString(eventIcon));
                    userData.put(Position_eventEmoji, eventEmoji);
                    userData.put(Position_starred, cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.STARRED)));
                    userData.put(Position_nickname, checkForNull(nickMap.get(contactID)));
                    userData.put(Position_eventStorage, Constants.STRING_STORAGE_CONTACTS); //Где искать событие по ID
                    userData.put(Position_eventURL, checkForNull(map_events_weblinks.get(contactID)));

                    fillEmptyUserData(userData);

                } else { //Продолжаем добавлять даты контакта

                    String existingDates = userData.get(Position_dates);
                    if (existingDates != null && !existingDates.contains(newEventDate))
                        userData.put(Position_dates, existingDates.concat(Constants.STRING_2TILDA).concat(newEventDate));

                }
            }

        } catch (Exception e) {
            if (preferences_debug_on) {
                Log.e(TAG, e.getMessage(), e);
                if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e
                        + Constants.STRING_EOL + resources.getString(R.string.msg_errors_details, accountKey, eventType, eventDate));
            }
        }
        return eventKey;

    }

    private boolean getCalendarEvents(String eventType) {
        //todo: использовать цвета календарей https://www.javatips.net/api/android.provider.calendarcontract.instances
        final TreeMap<Integer, String> userData = new TreeMap <>();

        try {

            long statCurrentModuleStart = System.currentTimeMillis();

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return false;

            Set<String> preferences_calendars = getPreferences_Calendars(Objects.requireNonNull(eventType));
            if (preferences_calendars.size() == 0) return false;

            ColumnIndexCache cache = new ColumnIndexCache();
            StringBuilder dataRow;

            recieveCalendarList();

            //https://stackoverflow.com/questions/25734285/how-to-get-the-real-time-of-recurring-events
            //https://stackoverflow.com/questions/10133616/reading-all-of-todays-events-using-calendarcontract-android-4-0

            if (contentResolver == null) contentResolver = context.getContentResolver();
            String[] projection = new String[] {
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.DESCRIPTION, //todo: доделать правила и под это поле
                    CalendarContract.Instances.BEGIN, //начало именно этого события
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Events.DTSTART, //начало первоначального события
                    CalendarContract.Events.ALL_DAY
            };

            Calendar startTime = Calendar.getInstance();
            startTime.set(Calendar.HOUR_OF_DAY, 0);
            startTime.set(Calendar.MINUTE, 0);
            startTime.set(Calendar.SECOND, 0);
            startTime.set(Calendar.MILLISECOND, 0);
            final int zoneOffset = TimeZone.getDefault().getOffset(startTime.getTimeInMillis()); //событие на весь день начинается в 00:00:00 UTC, надо скорректировать часовую зону
            startTime.add(Calendar.MILLISECOND, zoneOffset);

            Calendar endTime = Calendar.getInstance();
            endTime.set(Calendar.YEAR, startTime.get(Calendar.YEAR) + 1);
            endTime.set(Calendar.HOUR_OF_DAY, 0);
            endTime.set(Calendar.MINUTE, 0);
            endTime.set(Calendar.SECOND, 0);
            endTime.set(Calendar.MILLISECOND, 0);
            endTime.add(Calendar.MILLISECOND, zoneOffset);
            endTime.add(Calendar.SECOND, -1);

            String eventSubType;
            int eventIcon;
            String eventEmoji;

            int importStorage;
            String[] arrRules;
            ArrayList<Matcher> matcherNames = new ArrayList<>();
            boolean useEventYear = false;
            final ArrayList<String> eventURLs = new ArrayList<>();

            if (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {

                eventIcon = R.drawable.ic_event_birthday;
                eventEmoji = "🎂";
                eventSubType = eventTypesIDs.get(Constants.Type_BirthDay);
                importStorage = 1;
                useEventYear = preferences_birthday_calendars_useyear;

                arrRules = preferences_birthday_calendars_rules.split(Constants.STRING_PIPE, -1);
                if (!arrRules[0].isEmpty()) {
                    for (String rule : arrRules) {
                        if (rule.contains(Constants.RULE_TAG_NAME)) {
                            matcherNames.add(Pattern.compile(rule.replace(Constants.RULE_TAG_NAME, "(.*)")).matcher(Constants.STRING_EMPTY));
                        }
                    }
                }
            } else {

                eventSubType = eventTypesIDs.get(Constants.Type_CalendarEvent);
                eventIcon = R.drawable.ic_event_other;
                eventEmoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? "🗓️" : "\uD83D\uDCC6";
                importStorage = 0;

            }

            //todo: переделать на https://developer.android.com/reference/android/provider/CalendarContract.Instances
            StringBuilder calIDs = new StringBuilder();
            for (String calID: preferences_calendars) {
                if (calIDs.length() > 0) calIDs.append(" OR " + CalendarContract.Events.CALENDAR_ID + " = ");
                calIDs.append(calID);
            }

            String selection = CalendarContract.Events.CALENDAR_ID + " = " + calIDs;
                    /*"("
                    + " ( " + CalendarContract.Events.ALL_DAY + " = 1 )"
                    + " AND "
                    + "( " + CalendarContract.Events.CALENDAR_ID + " = " + calIDs + " )"
                    + ")";*/

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startTime.getTimeInMillis());
            ContentUris.appendId(builder, endTime.getTimeInMillis());

            Cursor cursor = contentResolver.query(builder.build(), projection, selection, null, "dtstart ASC");
            if (cursor != null) {
                if (cursor.getCount() > 0) {

                    statCalendarsEventCount+= cursor.getCount();
                    statEventsCount+= cursor.getCount();
                    //Calendar c = Calendar.getInstance();

                    int importMethod_Standalone = 0; //Календарное событие без контакта
                    int importMethod_NewContactEvent = 1; //Контакт найден, но у него нет данных о cобытии этого типа - обновляем событие по карточке контакта
                    int importMethod_AdditionalDateToContactEvent = 2; //Контакт найден, у него есть такое же событие - добавляем к источникам дат ещё одно значение

                    while (cursor.moveToNext()) {
                        userData.clear();
                        Date date = new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.BEGIN))));
                        Calendar dateCal = getCalendarFromDate(date);
                        Date dateFirst = new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DTSTART))));
                        Calendar dateFirstCal = getCalendarFromDate(dateFirst);

                        if (cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Events.ALL_DAY)) == 1) { //У AllDay событий зона всегда UTC
                            if (TimeZone.getDefault().getRawOffset() < 0) { //Для отрицательных зон надо прибавлять день
                                dateCal.add(Calendar.DATE, 1);
                                date = dateCal.getTime();
                                dateFirstCal.add(Calendar.DATE, 1);
                                dateFirst = dateFirstCal.getTime();
                            }
                        }
                        boolean isInstance = dateCal.get(Calendar.DAY_OF_MONTH) != dateFirstCal.get(Calendar.DAY_OF_MONTH)
                                || dateCal.get(Calendar.MONTH) != dateFirstCal.get(Calendar.MONTH);

                        int importMethod = importMethod_Standalone;
                        final String eventTitle = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.TITLE));
                        if (eventTitle == null || eventTitle.trim().isEmpty()) continue;
                        final String eventID = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.EVENT_ID));
                        set_events_ids.add(eventID);

                        String contactID = null;
                        userData.put(Position_personFullName, eventTitle);
                        userData.put(Position_personFullNameAlt, eventTitle);
                        userData.put(Position_eventStorage, Constants.STRING_STORAGE_CALENDAR);

                        eventURLs.clear();
                        String eventURL;
                        String eventDescription = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DESCRIPTION));
                        if (eventDescription != null) {
                            eventDescription = eventDescription.toLowerCase().replace(Constants.STRING_EOL, Constants.STRING_SPACE);
                            int indURL;
                            int indSpace;

                            for (String prefix: new String[] {Constants.STRING_HTTPS, Constants.STRING_HTTP}) {
                                indURL = eventDescription.indexOf(prefix);
                                while (indURL > -1) {
                                    indSpace = eventDescription.indexOf(Constants.STRING_SPACE, indURL);

                                    if (indSpace == -1) {
                                        eventURL = eventDescription.substring(indURL);
                                    } else {
                                        eventURL = eventDescription.substring(indURL, indSpace);
                                    }

                                    if (eventURL.isEmpty()) break;
                                    if (!eventURLs.contains(eventURL)) eventURLs.add(eventURL);
                                    eventDescription = eventDescription.replace(eventURL, Constants.STRING_EMPTY);
                                    indURL = eventDescription.indexOf(prefix);
                                }
                            }
                        }

                        if (importStorage == 1 && !getMergedID(eventID).isEmpty()) {

                            contactID = getMergedID(eventID);

                        } else if (importStorage == 1 && matcherNames.size() > 0) {

                            String foundName;
                            for (Matcher matcherName : matcherNames) {
                                if (matcherName.reset(eventTitle).find()) {
                                    foundName = matcherName.group(1);

                                    if (foundName != null) {
                                        userData.put(Position_personFullName, foundName);

                                        //если 2 компонента - просто меняем местами
                                        int spaceFirst = foundName.indexOf(Constants.STRING_SPACE);
                                        int spaceLast = foundName.lastIndexOf(Constants.STRING_SPACE);

                                        if (spaceFirst != -1 && spaceFirst == spaceLast) {
                                            userData.put(Position_personFullNameAlt, foundName.substring(spaceFirst + 1).concat(Constants.STRING_SPACE).concat(foundName.substring(0, spaceFirst)));
                                        } else {
                                            userData.put(Position_personFullNameAlt, foundName);
                                        }

                                        //Ищем контакт
                                        final String foundNameNormalyzed = normalizeName(foundName);
                                        contactID = map_contacts_names.get(foundNameNormalyzed);
                                        if (contactID != null) break;
                                    }
                                }
                            }
                        }

                        if (contactID != null) {
                            importMethod = importMethod_NewContactEvent;
                            userData.put(Position_contactID, contactID);

                            //Ищем событие контакта в списке событий и добавляем в него
                            Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(eventSubType + Constants.STRING_EOT + contactID);
                            if (eventIndex != null && eventIndex <= eventList.size() && !isInstance) {
                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                final String eventDates = singleRowList.get(ContactsEvents.Position_dates);
                                final String eventNewDate = Constants.EVENT_PREFIX_CALENDAR_EVENT + Constants.STRING_COLON_SPACE + (useEventYear ? sdf_java.format(dateFirst) : sdf_java_no_year.format(date));
                                boolean needUpdate = false;

                                if (!eventDates.contains(eventNewDate)) { //Пропускаем дубли
                                    singleRowList.set(ContactsEvents.Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
                                    needUpdate = true;
                                }
                                if (singleRowList.get(ContactsEvents.Position_eventID).isEmpty()) {
                                    singleRowList.set(ContactsEvents.Position_eventID, eventID);
                                    needUpdate = true;
                                }

                                if (!eventURLs.isEmpty()) {
                                    String eventURL_stored = checkForNull(singleRowList.get(ContactsEvents.Position_eventURL)).trim();
                                    StringBuilder sb = new StringBuilder(eventURL_stored);
                                    if (eventURL_stored.isEmpty()) {
                                        for (String url: eventURLs) {
                                            sb.append(url).append(Constants.STRING_2TILDA);
                                            statContactsURLCount++;
                                        }
                                        sb.delete(sb.length() - Constants.STRING_2TILDA.length(), sb.length());
                                    } else {
                                        for (String url: eventURLs) {
                                            if (!eventURL_stored.contains(url)) {
                                                sb.append(Constants.STRING_2TILDA).append(url);
                                                statContactsURLCount++;
                                            }
                                        }
                                    }
                                    singleRowList.set(ContactsEvents.Position_eventURL, sb.toString());
                                    map_events_weblinks.put(contactID, sb.toString());
                                    needUpdate = true;
                                }

                                if (!needUpdate) continue;

                                dataRow = new StringBuilder();
                                int rNum = 0;
                                for (String entry : singleRowList) {
                                    rNum++;
                                    if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                                    dataRow.append(entry);
                                }
                                eventList.set(eventIndex, dataRow.toString());
                                importMethod = importMethod_AdditionalDateToContactEvent;

                            } else { //Такого события ещё не было

                                //Добавляем данные контакта
                                final Long contactIDLong = parseToLong(contactID);
                                HashMap<String, String> contactDataMap = getContactDataMulti(contactIDLong, new String[] {
                                        ContactsContract.Contacts.PHOTO_URI,
                                        ContactsContract.Data.DISPLAY_NAME,
                                        ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE
                                });

                                userData.put(Position_photo_uri, contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));

                                String contactFIO = checkForNull(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME));
                                String contactTitle = checkForNull(map_contacts_titles.get(contactID));
                                if (!contactTitle.isEmpty()) {
                                    //всё, что внутри скобок в имени - в должность
                                    int pStart = contactFIO.indexOf(Constants.STRING_PARENTHESIS_START);
                                    int pEnd = contactFIO.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                                    if (pStart > -1 && pEnd > pStart) {
                                        contactTitle = contactFIO.substring(pStart + 1, pEnd);
                                        contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY);
                                    }
                                }

                                userData.put(Position_personFullName, contactFIO);
                                userData.put(Position_personFullNameAlt, checkForNull(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)).replace(Constants.STRING_COMMA, Constants.STRING_EMPTY));
                                userData.put(Position_title, contactTitle);
                                userData.put(Position_organization, checkForNull(map_organizations.get(contactID)));
                                userData.put(Position_nickname, checkForNull(map_contacts_aliases.get(contactID)));

                                if (!eventURLs.isEmpty()) {
                                    StringBuilder sb = new StringBuilder();
                                    for (String url: eventURLs) {
                                        sb.append(url).append(Constants.STRING_2TILDA);
                                        statContactsURLCount++;
                                    }
                                    sb.delete(sb.length() - Constants.STRING_2TILDA.length(), sb.length());
                                    userData.put(Position_eventURL, sb.toString());
                                    map_events_weblinks.put(contactID, sb.toString());
                                }

                                contactDataMap.clear();
                            }
                        }

                        if (importMethod != importMethod_AdditionalDateToContactEvent) {

                            if (importMethod != importMethod_NewContactEvent) {
                                userData.put(Position_eventStorage, Constants.STRING_STORAGE_CALENDAR);
                            }

                            String calendarTitle = map_calendars.get(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.CALENDAR_ID)));
                            userData.put(Position_eventCaption, calendarTitle != null ?
                                    getResources().getString(R.string.msg_calendar_info, getKeyParts(calendarTitle)[0]) :
                                    getResources().getString(R.string.event_type_calendar)
                            ); //Наименование события
                            userData.put(Position_eventID, eventID);
                            userData.put(Position_eventLabel, eventTitle); //Заголовок пользовательского события
                            userData.put(Position_eventType, eventType); //Тип события
                            userData.put(Position_eventSubType, eventSubType); //Подтип события
                            userData.put(Position_dates, Constants.EVENT_PREFIX_CALENDAR_EVENT
                                    + Constants.STRING_COLON_SPACE + (useEventYear ? sdf_java.format(dateFirst) : sdf_java_no_year.format(dateFirst)));
                            userData.put(Position_eventIcon, Integer.toString(eventIcon));
                            userData.put(Position_eventEmoji, eventEmoji);
                            if (isInstance) { //Уже известна дата следующего события
                                userData.put(Position_eventDate, sdf_DDMMYYYY.format(date));
                                userData.put(Position_eventDateText, sdf_DDMMYYYY.format(dateFirst));
                            }

                            if (importMethod == importMethod_Standalone) {
                                if (!eventURLs.isEmpty()) {
                                    StringBuilder sb = new StringBuilder();
                                    for (String url: eventURLs) {
                                        sb.append(url).append(Constants.STRING_2TILDA);
                                        statContactsURLCount++;
                                    }
                                    sb.delete(sb.length() - Constants.STRING_2TILDA.length(), sb.length());
                                    userData.put(Position_eventURL, sb.toString());
                                    map_events_weblinks.put(eventID, sb.toString());
                                }
                            }

                            fillEmptyUserData(userData);

                            dataRow = new StringBuilder();
                            int rNum = 0;
                            for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                                rNum++;
                                if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                                dataRow.append(entry.getValue());
                            }
                            final String eventData = dataRow.toString();
                            if (!eventList.contains(eventData)) {
                                eventList.add(eventData);

                                if (importMethod == importMethod_NewContactEvent & !isInstance) {  //Добавляем событие
                                    //map_eventsBySubtypeAndPersonID_offset.put(eventTypesIDs.get(Type_BirthDay) + STRING_EOT + contactID, eventList.size() - 1);
                                    map_eventsBySubtypeAndPersonID_offset.put(eventSubType + Constants.STRING_EOT + contactID, eventList.size() - 1);
                                }
                            }
                        }
                        userData.clear();
                    }
                }

                cursor.close();
            }

            statTimeGetCalendarEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (SecurityException se) {
            return false;
        } catch (Exception e) {
            if (preferences_debug_on) {
                Log.e(TAG, e.getMessage(), e);
                StringBuilder dataRow = new StringBuilder();
                if (!userData.isEmpty()) {
                    int rNum = 0;
                    for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                        rNum++;
                        dataRow.append(Constants.STRING_EOL);
                        dataRow.append(entry.getKey()).append("=");
                        dataRow.append(entry.getValue());
                    }}

                if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + dataRow);
            }
            return false;
        } finally {
            userData.clear();
        }
    }

    void recieveCalendarList() {

        map_calendars.clear();
        Cursor cursor = null;

        try {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return;

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
                            map_calendars.put(cursor.getInt(0) + Constants.STRING_EMPTY, cursor.getString(2).concat(Constants.STRING_EOT).concat(cursor.getString(3)));
                        }
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
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private boolean getFileEvents(String eventType) {

        try {

            long statCurrentModuleStart = System.currentTimeMillis();

            String eventSubType;
            int eventIcon;
            String eventEmoji;
            int importStorage;
            TreeMap<Integer, String> userData = new TreeMap <>();
            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());

            Set<String> fileList;
            if (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {

                fileList = preferences_Birthday_files;
                eventSubType = eventTypesIDs.get(Constants.Type_BirthDay);
                eventIcon = R.drawable.ic_event_birthday;
                eventEmoji = "🎂";
                importStorage = 1;

            } else if (eventType.equals(eventTypesIDs.get(Constants.Type_Other))) {

                fileList = preferences_Otherevent_files;
                eventSubType = eventTypesIDs.get(Constants.Type_FileEvent);
                eventIcon = R.drawable.ic_event_other;
                eventEmoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? "🗓️" : "\uD83D\uDCC6";
                importStorage = 0;

            } else {
                return false;
            }
            if (fileList == null || fileList.size() == 0) return false;
            if (contentResolver == null) contentResolver = context.getContentResolver();

            for (String file: fileList) {

                String[] fileDetails = file.split(Constants.STRING_PIPE);
                Uri uri = null;
                String[] eventsArray = null;
                try {
                    uri = Uri.parse(fileDetails[1]);
                } catch (NullPointerException e) { /**/ }
                if (uri != null) eventsArray = readFileToString(uri, Constants.STRING_EOL).split(Constants.STRING_EOL);
                if (eventsArray == null || eventsArray[0].isEmpty()) continue;

                String fileName = fileDetails[0].lastIndexOf("/") > -1 ? fileDetails[0].substring(fileDetails[0].lastIndexOf("/") + 1) : fileDetails[0];

                for (String line: eventsArray) {
                    //BirthdayPro, DarkBirthday: <Дата без пробелов>[,флаги] название праздника или ИФ [(должность)]
                    //Birthdays Plus: |ДДДД-ММ-ДД|ИОФ|тип (Birthday, Anniversaly, Custom)|наименование события или null|

                    int indexDateEnd = line.indexOf(Constants.STRING_SPACE);
                    String eventTitle;
                    String eventDateOriginal;
                    boolean useEventYear = true;
                    Date dateEvent = null;
                    String eventNewDate = null;
                    String contactID = null;
                    String eventURL = Constants.STRING_EMPTY;
                    userData.clear();

                    if (!line.isEmpty()) {
                        if (line.charAt(0) != '#' && indexDateEnd > -1) { //DarkBirthday, переходящие пропускаем

                            eventTitle = line.substring(indexDateEnd + 1).trim();
                            eventDateOriginal = line.substring(0, indexDateEnd);
                            boolean isEndless = true;
                            boolean isAD = true;
                            //todo: сделать поддержку дат до 1900 http://rsdn.org/forum/java/981164.all
                            if (eventDateOriginal.contains(Constants.STRING_COMMA)) { //Флаги события
                                final String flags = eventDateOriginal.substring(eventDateOriginal.indexOf(Constants.STRING_COMMA) + 1);
                                if (flags.contains(Constants.STRING_1)) isEndless = false;
                                if (flags.contains(Constants.STRING_BC)) isAD = false;
                                eventDateOriginal = eventDateOriginal.substring(0, eventDateOriginal.indexOf(Constants.STRING_COMMA));
                            }

                            int indexDateNoYear = eventDateOriginal.indexOf(Constants.STRING_0000);
                            if (indexDateNoYear == -1) { //С годом

                                try {
                                    if (isAD) {
                                        dateEvent = sdf_DDMMYYYY.parse(eventDateOriginal);
                                    } else {
                                        dateEvent = sdf_DDMMYYYY_G.parse(eventDateOriginal.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                    }
                                } catch (ParseException e1) {
                                    try {
                                        if (isAD) {
                                            dateEvent = sdf_india.parse(eventDateOriginal);
                                        } else {
                                            dateEvent = sdf_india_G.parse(eventDateOriginal.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                        }
                                    } catch (ParseException e2) {
                                        try {
                                            if (isAD) {
                                                dateEvent = sdf_uk.parse(eventDateOriginal);
                                            } else {
                                                dateEvent = sdf_uk_G.parse(eventDateOriginal.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                            }
                                        } catch (ParseException e3) {
                                            //Не получилось распознать
                                        }
                                    }
                                }
                                if (dateEvent != null && !isEndless && now.after(getCalendarFromDate(dateEvent))) dateEvent = null; //Одиночное событие и оно прошло

                            } else { //Без года

                                useEventYear = false;
                                final String dateNextEvent = eventDateOriginal.substring(0, indexDateNoYear) + now.get(Calendar.YEAR);
                                try {
                                    dateEvent = sdf_DDMMYYYY.parse(dateNextEvent);
                                } catch (ParseException e1) {
                                    try {
                                        dateEvent = sdf_india.parse(dateNextEvent);
                                    } catch (ParseException e2) {
                                        try {
                                            dateEvent = sdf_uk.parse(dateNextEvent);
                                        } catch (ParseException e3) {
                                            //Не получилось распознать
                                        }
                                    }
                                }
                                if (dateEvent != null && now.after(getCalendarFromDate(dateEvent))) dateEvent = addYear(dateEvent, 1);
                            }

                            if (dateEvent != null) {
                                eventNewDate = Constants.EVENT_PREFIX_FILE_EVENT + Constants.STRING_COLON_SPACE + (useEventYear ? isAD ? sdf_java.format(dateEvent) : sdf_java_G.format(dateEvent) : sdf_java_no_year.format(dateEvent));

                                userData.put(Position_eventStorage, Constants.STRING_STORAGE_FILE);
                                //userData.put(Position_eventID, STRING_EMPTY); //todo: вычислять по md5 имени файла + md5 имени
                                userData.put(Position_eventCaption, !fileName.isEmpty() ? //Наименование события
                                        getResources().getString(R.string.msg_file_info, fileName) :
                                        getResources().getString(R.string.event_type_file)
                                );
                                userData.put(Position_eventType, eventType); //Тип события
                                userData.put(Position_eventSubType, eventSubType); //Подтип события
                                userData.put(Position_dates, eventNewDate);
                                userData.put(Position_eventIcon, Integer.toString(eventIcon));
                                userData.put(Position_eventEmoji, eventEmoji);

                                String eventTitle_lowered = eventTitle.toLowerCase();
                                int urlOffset = eventTitle_lowered.indexOf(Constants.STRING_HTTP);
                                if (urlOffset > -1) {
                                    eventURL = eventTitle.substring(urlOffset);
                                } else {
                                    urlOffset = eventTitle_lowered.indexOf(Constants.STRING_HTTPS);
                                    if (urlOffset > -1) {
                                        eventURL = eventTitle.substring(urlOffset);
                                    }
                                }
                                if (urlOffset > -1) {
                                    if (eventURL.contains(Constants.STRING_SPACE)) eventURL = eventURL.substring(0, eventURL.indexOf(Constants.STRING_SPACE));
                                    userData.put(Position_eventURL, eventURL);
                                    eventTitle = eventTitle.replace(eventURL, Constants.STRING_EMPTY).trim();
                                    statContactsURLCount++;
                                }

                                if (importStorage == 1) { //День рождения

                                    //всё, что внутри скобок в имени - в должность
                                    int pStartFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_START);
                                    int pStartLast = eventTitle.lastIndexOf(Constants.STRING_PARENTHESIS_START);
                                    int pEndFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                                    int pEndLast = eventTitle.lastIndexOf(Constants.STRING_PARENTHESIS_CLOSE);
                                    String contactTitle = null;

                                    if (pStartFirst > -1 && pEndFirst > pStartFirst) { //хотя бы пара скобок
                                        if (pStartFirst == pStartLast && pEndFirst == pEndLast) { //одна пара скобок

                                            contactTitle = eventTitle.substring(pStartFirst + 1, pEndFirst);
                                            eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();

                                        } else if (pStartLast < pEndFirst && pStartLast < pEndLast) { //скобки внутри скобок

                                            contactTitle = eventTitle.substring(pStartFirst + 1, pEndLast);
                                            eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();

                                        } else if (pEndFirst < pStartLast) { //пара скобок за другой парой

                                            contactTitle = eventTitle.substring(pStartLast + 1, pEndLast);
                                            eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();

                                        }
                                        if (contactTitle != null) {
                                            int cStart = contactTitle.indexOf(Constants.STRING_COMMA);
                                            if (cStart > 0) {
                                                userData.put(Position_organization, contactTitle.substring(0, cStart).trim());
                                                userData.put(Position_title, contactTitle.substring(cStart + 1).trim());
                                            } else {
                                                userData.put(Position_title, contactTitle.trim());
                                            }
                                        }

                                    }

                                    userData.put(Position_personFullName, eventTitle);

                                    int spaceFirst = eventTitle.indexOf(Constants.STRING_SPACE);
                                    int spaceLast = eventTitle.lastIndexOf(Constants.STRING_SPACE);
                                    String personFullNameAlt = eventTitle;
                                    if (spaceFirst != -1 && spaceFirst == spaceLast) { //если 2 компонента - просто меняем местами
                                        personFullNameAlt = eventTitle.substring(spaceFirst + 1).concat(Constants.STRING_SPACE).concat(eventTitle.substring(0, spaceFirst));
                                    }
                                    userData.put(Position_personFullNameAlt, personFullNameAlt);

                                    //Ищем в контактах
                                    contactID = map_contacts_names.get(normalizeName(eventTitle));
                                    if (contactID == null && !personFullNameAlt.equals(eventTitle)) {
                                        contactID = map_contacts_names.get(normalizeName(personFullNameAlt));
                                    }

                                } else { //Событие

                                    userData.put(Position_personFullName, eventTitle);
                                    userData.put(Position_personFullNameAlt, eventTitle);

                                }

                            }

                            //} else if (line.charAt(0) == '|') { //Birthdays Plus

                        }

                        //Проверка по организации, что нашли именно требуемый контакт
                        String orgNameFile = Constants.STRING_EMPTY;
                        String titleFile = Constants.STRING_EMPTY;
                        if (contactID != null) {
                            orgNameFile = checkForNull(userData.get(Position_organization)).trim().toLowerCase();
                            titleFile = checkForNull(userData.get(Position_title)).trim();
                            String orgNameContact = checkForNull(map_organizations.get(contactID)).trim().toLowerCase();

                            if (!orgNameContact.isEmpty() && !orgNameFile.isEmpty() && !orgNameContact.contains(orgNameFile))
                                contactID = null;
                        }

                        if (contactID != null) {
                            userData.put(Position_contactID, contactID);

                            //Ищем событие контакта в списке событий и добавляем в него
                            //Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(eventTypesIDs.get(Type_BirthDay) + STRING_EOT + contactID);
                            Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(eventSubType + Constants.STRING_EOT + contactID);
                            if (eventIndex != null && eventIndex <= eventList.size()) {

                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                final String eventDates = singleRowList.get(ContactsEvents.Position_dates);
                                boolean needUpdate = false;

                                if (!eventDates.contains(eventNewDate)) { //Пропускаем дубли
                                    singleRowList.set(ContactsEvents.Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
                                    needUpdate = true;
                                }

                                if (singleRowList.get(ContactsEvents.Position_organization).trim().isEmpty() && !orgNameFile.isEmpty()) {
                                    singleRowList.set(ContactsEvents.Position_organization, orgNameFile);
                                    needUpdate = true;
                                }

                                if (singleRowList.get(ContactsEvents.Position_title).trim().isEmpty() && !titleFile.isEmpty()) {
                                    singleRowList.set(ContactsEvents.Position_title, titleFile);
                                    needUpdate = true;
                                }

                                if (!eventURL.isEmpty()) {
                                    String eventURL_stored = checkForNull(singleRowList.get(ContactsEvents.Position_eventURL)).trim();
                                    if (eventURL_stored.isEmpty()) {
                                        singleRowList.set(ContactsEvents.Position_eventURL, eventURL);
                                    } else if (!eventURL_stored.contains(eventURL)) {
                                        singleRowList.set(ContactsEvents.Position_eventURL, eventURL_stored.concat(Constants.STRING_2TILDA).concat(eventURL));
                                    }
                                    needUpdate = true;
                                }

                                if (!needUpdate) continue;

                                StringBuilder dataRow = new StringBuilder();
                                int rNum = 0;
                                for (String entry : singleRowList) {
                                    rNum++;
                                    if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                                    dataRow.append(entry);
                                }
                                eventList.set(eventIndex, dataRow.toString());
                                userData.clear();

                            } else { //Такого события ещё не было

                                //Добавляем данные контакта
                                final Long contactIDLong = parseToLong(contactID);
                                HashMap<String, String> contactDataMap = getContactDataMulti(contactIDLong, new String[] {
                                        ContactsContract.Contacts.PHOTO_URI,
                                        ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE
                                });

                                userData.put(Position_photo_uri, contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));
                                userData.put(Position_personFullNameAlt, checkForNull(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)).replace(Constants.STRING_COMMA, Constants.STRING_EMPTY));
                                userData.put(Position_nickname, checkForNull(map_contacts_aliases.get(contactID)));
                                if (TextUtils.isEmpty(userData.get(Position_organization)))
                                    userData.put(Position_organization, checkForNull(map_organizations.get(contactID)));
                                if (TextUtils.isEmpty(userData.get(Position_title)))
                                    userData.put(Position_title, checkForNull(map_contacts_titles.get(contactID)));

                                contactDataMap.clear();

                            }
                        }

                        if (!userData.isEmpty()) {
                            statEventsCount++;
                            statFilesEventCount++;
                            fillEmptyUserData(userData);

                            StringBuilder dataRow = new StringBuilder();
                            int rNum = 0;
                            for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                                rNum++;
                                if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                                dataRow.append(entry.getValue());
                            }
                            final String eventData = dataRow.toString();
                            if (!eventList.contains(eventData)) {
                                eventList.add(eventData);
                            }
                        }
                    }
                }
            }

            statTimeGetFileEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    @NonNull
    public String readFileToString(Uri uri, String delimeter) {

        StringBuilder sb = new StringBuilder();
        String line;

        try {

            if (contentResolver == null) contentResolver = context.getContentResolver();
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try (
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
            ) {

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    if (delimeter != null) sb.append(delimeter);
                }

            } catch (java.lang.SecurityException se) {
                if (preferences_debug_on) {
                    handler.post(() -> Toast.makeText(context, resources.getText(R.string.msg_file_open_error) + getPath(context, uri), Toast.LENGTH_LONG).show());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return sb.toString();
    }

    Bitmap getContactPhoto(@NonNull String event, boolean showPhotos, boolean makeSquared, boolean forWidget, int roundingFactor) {

        Bitmap bm = null;

        try {

            if (event.isEmpty()) return null;

            String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
            String eventSubType = singleEventArray[Position_eventSubType];

            if (eventSubType.equals(eventTypesIDs.get(Constants.Type_CalendarEvent)) || eventSubType.equals(eventTypesIDs.get(Constants.Type_FileEvent))) {
                return BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_other);
            }

            boolean isDeath = eventSubType.equals(eventTypesIDs.get(Constants.Type_Death));
            float offsetWidget = forWidget ? (9 * DisplayMetrics_density) : 0;

            if (showPhotos &&
                    !TextUtils.isEmpty(singleEventArray[Position_contactID]) &&
                    !TextUtils.isEmpty(singleEventArray[Position_photo_uri]) &&
                    !singleEventArray[Position_photo_uri].equalsIgnoreCase(Constants.STRING_NULL)) {
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
                //Если событие - не день рождения, пытаемся достать возраст из дня рождения
                if (!eventSubType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {
                    final Date birthDate = set_events_birthdays.get(singleEventArray[Position_contactID]);
                    Date BDay = null;
                    try {
                        BDay = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                    } catch (java.text.ParseException e) { /**/ }

                    List<String> singleRowList = Arrays.asList(singleEventArray);
                    if (birthDate != null && BDay != null) {
                        final int countYearsDiff = countYearsDiff(birthDate, BDay);
                        if (countYearsDiff > 0) {
                            singleRowList.set(Position_age, String.valueOf(countYearsDiff));
                        } else {
                            //если день рождения без года - мы об этом никак не узнаем
                            singleRowList.set(Position_age, Constants.STRING_MINUS1);
                            // ToastExpander.showText(context, Arrays.toString(singleEventArray));
                        }
                        singleEventArray = singleRowList.toArray(new String[0]);
                    }


                }

                //Случайное фото с соответствиии с возрастом и полом
                Person person = new Person(context, singleEventArray);
                int gender = person.getGender();

                //По-умолчанию
                Integer idPhoto = R.drawable.ic_pack00_m1;
                if (gender == 2 && preferences_IconPackImages_F.get(0) != null) {
                    idPhoto = preferences_IconPackImages_F.get(0);
                } else if (preferences_IconPackImages_M.get(0) != null) {
                    idPhoto = preferences_IconPackImages_M.get(0);
                }

                //Фото для года
                if (person.Age >= 0) {
                    if (gender == 2) {
                        for (Map.Entry<Integer, Integer> entry: preferences_IconPackImages_F.entrySet()) {
                            int beforeAge = entry.getKey();
                            if (beforeAge > 0 && person.Age <= beforeAge) {
                                idPhoto = preferences_IconPackImages_F.get(beforeAge);
                                break;
                            }
                        }
                    } else {
                        for (Map.Entry<Integer, Integer> entry: preferences_IconPackImages_M.entrySet()) {
                            int beforeAge = entry.getKey();
                            if (beforeAge > 0 && person.Age <= beforeAge) {
                                idPhoto = preferences_IconPackImages_M.get(beforeAge);
                                break;
                            }
                        }
                    }
                }
                if (idPhoto == null) return null;
                bm = getBitmap(context, idPhoto);
                if (bm == null) return null;

            }

            int roundingRadiusX = 0;
            int roundingRadiusY = 0;
            if (roundingFactor > 1) {

                final int width = bm.getWidth();
                final int height = bm.getHeight();
                final String roundingFactorStr = String.valueOf(roundingFactor);
                if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Rounded1))) {
                    roundingRadiusX = width / 12;
                    roundingRadiusY = height / 12;
                } else if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Rounded2))) {
                    roundingRadiusX = width / 8;
                    roundingRadiusY = height / 8;
                } else if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Rounded3))) {
                    roundingRadiusX = width / 4;
                    roundingRadiusY = height / 4;
                } else if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Circle))) {
                    roundingRadiusX = width / 2;
                    roundingRadiusY = height / 2;
                    makeSquared = true;
                }

            }

            if (makeSquared) {
                final int bmWidth = bm.getWidth();
                final int bmHeight = bm.getHeight();

                if (bmHeight > bmWidth) {
                    //noinspection SuspiciousNameCombination
                    bm = Bitmap.createBitmap(bm, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
                } else {
                    //noinspection SuspiciousNameCombination
                    bm = Bitmap.createBitmap(bm, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
                }

            }

            if (!singleEventArray[Position_eventStorage].equals(Constants.STRING_STORAGE_CALENDAR) &&
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
                paint.setStrokeWidth((float) bm.getWidth() / 6);
                canvas.drawBitmap(bm, new Matrix(), null);
                canvas.drawLine((float) (bm.getWidth() * 1.25), (float) bm.getHeight() / 2, (float) bm.getWidth() / 2, (float) (bm.getHeight() * 1.25), paint);
                bm.recycle();
                bm = bmOverlay;
            }

            //Добавление иконки избранного
            if (!forWidget &&
                    preferences_list_event_info.contains(ContactsEvents.pref_List_EventInfo_FavoritesIcon) &&
                    singleEventArray[Position_starred].equals(Constants.STRING_1)) {

                Bitmap bmOverlay = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bm, new Matrix(), null);
                bm.recycle();
                Bitmap bmStar = BitmapFactory.decodeResource(getResources(), R.drawable.fav_star);
                final Bitmap bmStarScaled = Bitmap.createScaledBitmap(bmStar, bmOverlay.getWidth() / 4, bmOverlay.getHeight() / 4, true);

                if (roundingFactor < 9) { //Не круг - рисуем в левом нижнем углу

                    canvas.drawBitmap(
                            bmStarScaled,
                            2,
                            (float) (bmOverlay.getHeight() * 3 / 4) - 2,
                            null
                    );

                } else { //Круг - рисуем внизу по центру

                    canvas.drawBitmap(
                            bmStarScaled,
                            (float) (bmOverlay.getWidth() * 3 / 4) / 2,
                            (float) (bmOverlay.getHeight() * 3 / 4) - 2,
                            null
                    );

                }
                bmStar.recycle();
                bmStarScaled.recycle();
                bm = bmOverlay;

            }

            return toRoundCorner(bm, roundingRadiusX, roundingRadiusY);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return null;
        }
    }

    @NonNull String getContactData(@NonNull Long contactId, @NonNull String columnName) {

        try {

            if (contactId == 0) return Constants.STRING_EMPTY;

            String contactData;
            if (map_contacts_data.containsKey(contactId + columnName)) {
                contactData = map_contacts_data.get(contactId + columnName);
                if (contactData != null) return contactData;
            }

            contactData = Constants.STRING_EMPTY;
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

            if (columnName.equals(ContactsContract.Contacts.PHOTO_URI)) {
                Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                return dataUri != null ? dataUri.toString() : Constants.STRING_EMPTY;
            } else {
                Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
                Cursor dataCursor = contentResolver.query(
                        dataUri,
                        null,
                        ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                        new String[]{ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                        null);
                if (dataCursor != null) {
                    int columnIndex = dataCursor.getColumnIndex(columnName);
                    if (columnIndex > 0) {
                        while (dataCursor.moveToNext()) {
                            contactData = dataCursor.getString(columnIndex);
                            if (contactData != null && !contactData.isEmpty()) {
                                //исключаем всё, что внутри скобок
                                int pStart = contactData.indexOf(Constants.STRING_PARENTHESIS_OPEN);
                                int pEnd = contactData.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                                if (pStart > -1 && pEnd > pStart) {
                                    contactData = contactData.substring(0, pStart);
                                }
                                break;
                            }
                        }
                    }
                    dataCursor.close();
                }
                return contactData != null ? contactData : Constants.STRING_EMPTY;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }

    @NonNull HashMap<String, String> getContactDataMulti(@NonNull Long contactId, @NonNull String[] columnNames) {

        HashMap<String, String> resultMap = new HashMap<>();

        try {

            if (contactId == 0) return resultMap;
            if (contentResolver == null) contentResolver = context.getContentResolver();

            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

            //Собираем собранное ранее
            for (String columnName: columnNames) {
                if (map_contacts_data.containsKey(contactId + columnName)) {
                    resultMap.put(columnName, map_contacts_data.get(contactId + columnName));
                }
            }
            if (Arrays.asList(columnNames).contains(ContactsContract.Contacts.PHOTO_URI) && !map_contacts_data.containsKey(contactId + ContactsContract.Contacts.PHOTO_URI)) {
                Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                resultMap.put(ContactsContract.Contacts.PHOTO_URI, dataUri != null ? dataUri.toString() : Constants.STRING_EMPTY);
            }

            if (resultMap.size() == columnNames.length) return resultMap; //Всё уже есть

            Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            String contactData;
            Cursor dataCursor = contentResolver.query(
                    dataUri,
                    null,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                    null);
            if (dataCursor != null) {
                while (dataCursor.moveToNext()) {
                    for (String columnName: columnNames) {
                        if (!columnName.equals(ContactsContract.Contacts.PHOTO_URI) && !resultMap.containsKey(columnName)) {
                            int columnIndex = dataCursor.getColumnIndex(columnName);
                            if (columnIndex > 0) {
                                contactData = dataCursor.getString(columnIndex);
                                if (contactData != null && !contactData.isEmpty()) {
                                    resultMap.put(columnName, contactData);
                                }
                            }
                        }
                    }
                }
                dataCursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return resultMap;
    }


    @NonNull String getContactFullNameShort(@NonNull Long contactId) {

        try {

            if (contactId == 0) return Constants.STRING_EMPTY;

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
                int columnIndexFamily = nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
                int columnIndexGiven = nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
                int columnIndexMiddle = nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME);
                if (columnIndexFamily > 0 && columnIndexGiven > 0 && columnIndexMiddle > 0) {
                    while (nameCursor.moveToNext()) {
                        lastName = nameCursor.getString(columnIndexFamily);
                        firstName = nameCursor.getString(columnIndexGiven);
                        secondName = nameCursor.getString(columnIndexMiddle);

                        final String secondNameWord = (!TextUtils.isEmpty(secondName) ? Constants.STRING_SPACE + secondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY);
                        if (!TextUtils.isEmpty(lastName)) {
                            result = lastName + (!TextUtils.isEmpty(firstName) ? Constants.STRING_SPACE + firstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY) + secondNameWord;
                        } else if (!TextUtils.isEmpty(firstName)) {
                            result = firstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD + secondNameWord;
                        }

                        if (!TextUtils.isEmpty(result)) break;
                    }
                }
                nameCursor.close();
            }
            return result != null ? result : Constants.STRING_EMPTY;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }

    @NonNull String getContactPhone(@NonNull Long contactId) {

        try {

            if (contactId == 0) return Constants.STRING_EMPTY;

            String phone = Constants.STRING_EMPTY;

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
                int columnIndexPhone = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (columnIndexPhone > 0 && phoneCursor.moveToFirst()) {
                    phone = phoneCursor.getString(columnIndexPhone);
                }
                phoneCursor.close();
            }
            return phone != null ? phone : Constants.STRING_EMPTY;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }

    synchronized void computeDates() {
        //Вычисляем даты

        long statCurrentModuleStart = System.currentTimeMillis();

        try {

            if (isEmptyEventList()) return;

            List<String> magicList = new ArrayList<>(); //Для 5k событий

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());

            setLocale(false);
            final Resources resources = getResources();

            for (int i = 0; i < eventList.size(); i++) {
                computeDateForEvent(i, magicList, now, currentDay);
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

            eventListUnsorted = new ArrayList<>(eventList);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {

            //Сортируем
            Collections.sort(eventList);

            statLastComputeDates = System.currentTimeMillis();
            statTimeComputeDates = statLastComputeDates - statCurrentModuleStart;
        }
    }

    void computeDateForEvent(int i, @NonNull List<String> magicList, @NonNull Calendar now, @NonNull Date currentDay) {

        String singleEvent = Constants.STRING_EMPTY;

        try {
            long dayDiff = -1;
            boolean isYear = false;
            boolean isAD = true;
            Date eventDate_Date = null; //оригинальная дата события
            Date BDay = null; //следующая дата события
            int Age = 0;

            singleEvent = eventList.get(i);
            if (singleEvent == null) return;

            String[] singleEventArray = singleEvent.split(Constants.STRING_EOT, -1);
            if (singleEventArray.length < Position_attrAmount) {
                eventList.set(i, Constants.STRING_EMPTY);
                return;
            }

            String[] dayArray = singleEventArray[Position_dates].split(Constants.STRING_2TILDA, -1);
            final String eventCaption = singleEventArray[Position_eventCaption];
            final String eventType = singleEventArray[Position_eventType];
            final String eventSubType = singleEventArray[Position_eventSubType];

            if (TextUtils.isEmpty(singleEventArray[Position_eventDate])) {
                //перебираем все даты и находим максимальную
                final int nowYear = now.get(Calendar.YEAR);
                for (String dayValue : dayArray) {
                    String accountType = dayValue.substring(0, dayValue.indexOf(Constants.STRING_COLON_SPACE));
                    String storedDate = dayValue.substring(dayValue.indexOf(Constants.STRING_COLON_SPACE) + Constants.STRING_COLON_SPACE.length());
                    Date storedDate_Date = null;
                    boolean storedDate_isYear = false;

                    increaseStatForAccountType(accountType);

                    if (accountType.contains(Constants.account_skype)) {

                        storedDate_isYear = true;

                        if (storedDate.contains("Sept")) {
                            //https://stackoverflow.com/questions/67089932/simpledateformat-format-month-september-jdk16
                            storedDate = storedDate.replace("Sept", "Sep");
                        }

                        try {
                            storedDate_Date = sdf_skype.parse(storedDate);
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
                                        try {
                                            storedDate_Date = sdf_uk.parse(storedDate);
                                        } catch (ParseException e5) {
                                            try {
                                                storedDate_Date = sdf_india.parse(storedDate);
                                            } catch (ParseException e6) {
                                                try {
                                                    storedDate_Date = sdf_uk_no_year.parse(storedDate);
                                                } catch (ParseException e7) {
                                                    try {
                                                        storedDate_Date = sdf_india_no_year.parse(storedDate);
                                                    } catch (ParseException e8) {
                                                        //Не получилось распознать
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else if (accountType.contains(Constants.account_vk)) {

                        if (storedDate.startsWith(Constants.STRING_0000_MINUS)) { //Нет года, формат 0000-mm-dd

                            try {
                                BDay = sdf_java.parse(nowYear + Constants.STRING_MINUS + storedDate.substring(5));
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
                                storedDate_Date = sdf_java.parse(storedDate);
                            } catch (ParseException e) {
                                try {
                                    storedDate_Date = sdf_skype.parse(storedDate);
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
                        //com.android.local

                        if (
                                storedDate.startsWith(Constants.STRING_2MINUS) || //Нет года, формат --MM-dd
                                        storedDate.startsWith(Constants.STRING_0000_MINUS) || //Нет года, формат 0000-MM-dd
                                        (storedDate.startsWith("1604-") && (accountType.contains(Constants.account_exchange) || accountType.contains(Constants.account_google))) || //Нет года, формат 1604-MM-dd - com.google.android.gm.exchange https://stackoverflow.com/questions/14023390/nsdate-return-1604-for-year-value
                                        (storedDate.startsWith("1904-") && accountType.contains(Constants.account_huawei)) || //Нет года, формат 1904-MM-dd - com.android.huawei.phone
                                        (!TextUtils.isEmpty(eventCaption) && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventCaption.toLowerCase()).find()) //Именины считаем без года
                        ) {

                            try {
                                BDay = sdf_java.parse(nowYear + Constants.STRING_MINUS + storedDate.substring(storedDate.startsWith(Constants.STRING_2MINUS) ? 2 : 5));
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
                                storedDate_Date = sdf_java_G.parse(storedDate);
                                isAD = false;
                            } catch (ParseException e0) {
                                try {
                                    storedDate_Date = sdf_java.parse(storedDate);
                                } catch (ParseException e) {
                                    try {
                                        storedDate_Date = sdf_skype.parse(storedDate);
                                    } catch (ParseException e2) {
                                        try {
                                            storedDate_Date = sdf_ru.parse(storedDate);
                                        } catch (ParseException e3) {
                                            try {
                                                storedDate_Date = sdf_uk.parse(storedDate);
                                            } catch (ParseException e4) {
                                                try {
                                                    storedDate_Date = sdf_india.parse(storedDate);
                                                } catch (ParseException e5) {
                                                    try {
                                                        storedDate_Date = sdf_YYYYMMDD_noDiv.parse(storedDate);
                                                    } catch (ParseException e6) {
                                                        try {
                                                            storedDate_Date = sdf_MMMMDYYYY.parse(storedDate);
                                                        } catch (ParseException e7) {
                                                            try {
                                                                storedDate_Date = sdf_uk_no_year.parse(storedDate);
                                                                storedDate_isYear = false;
                                                            } catch (ParseException e8) {
                                                                try {
                                                                    storedDate_Date = sdf_india_no_year.parse(storedDate);
                                                                    storedDate_isYear = false;
                                                                } catch (ParseException e9) {
                                                                    //Не получилось распознать
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }

                    if (storedDate_Date != null) {
                        if (eventDate_Date == null) {
                            eventDate_Date = storedDate_Date;
                            isYear = storedDate_isYear;
                        } else if (storedDate_isYear & (!isYear || countDaysDiff(eventDate_Date, storedDate_Date) > 0)) { //Если у пользователя несколько дат, берём наименьший возраст todo: можно вынести в настройку - в какую сторону округлять
                            eventDate_Date = storedDate_Date;
                            isYear = true;
                        }
                    }
                }

                if (eventDate_Date != null) {

                    if (isYear) { //Дата с годом
                        if (isAD) {
                            singleEventArray[Position_eventDateText] = sdf_DDMMYYYY.format(eventDate_Date); //оригинальное событие
                        } else {
                            singleEventArray[Position_eventDateText] = sdf_DDMMY.format(eventDate_Date) + resources.getString(R.string.msg_after_year_bc); //до н.э.
                        }
                    } else { //Дата без года
                        singleEventArray[Position_eventDateText] = sdf_DDMM.format(eventDate_Date); //оригинальное событие без года
                    }

                    if (isYear) { //в eventDate_Date - оригинальное событие

                        Calendar cal = getCalendarFromDate(eventDate_Date);
                        try {
                            BDay = sdf_java.parse(nowYear + Constants.STRING_MINUS + (cal.get(Calendar.MONTH) + 1) + Constants.STRING_MINUS + cal.get(Calendar.DAY_OF_MONTH));
                            if (BDay != null) {
                                long dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) BDay = addYear(BDay, 1);
                            }
                        } catch (ParseException e) { /**/ }
                    }

                }

                if (BDay != null) {
                    if (TextUtils.isEmpty(singleEventArray[Position_eventDate])) singleEventArray[Position_eventDate] = sdf_DDMMYYYY.format(BDay); //следующая дата события
                }

            } else {
                try {
                    eventDate_Date = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateText]);
                } catch (ParseException e) { /**/ }
                try {
                    BDay = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                } catch (ParseException e) { /**/ }

                String dayValue = dayArray[0];
                if (!dayValue.isEmpty()) {
                    String accountType = dayValue.substring(0, dayValue.indexOf(Constants.STRING_COLON_SPACE));
                    increaseStatForAccountType(accountType);
                }

            }

            if (eventDate_Date != null && BDay != null) {
                dayDiff = countDaysDiff(currentDay, BDay);
                Age = countYearsDiff(eventDate_Date, BDay); //Считаем, сколько будет лет
                if (eventSubType.equals(eventTypesIDs.get(Constants.Type_BirthDay)) && !set_events_birthdays.containsKey(singleEventArray[Position_contactID]))
                    set_events_birthdays.put(singleEventArray[Position_contactID], eventDate_Date);
            }

            if (dayDiff == -1) {

                if (preferences_debug_on) {
                    StringBuilder sb = new StringBuilder();
                    sb
                            .append(resources.getString(R.string.msg_date_parse_error))
                            .append(singleEventArray[Position_dates])
                            .append(Constants.STRING_COMMA_SPACE)
                            .append(singleEventArray[Position_personFullName]);

                    Log.i(TAG, sb.toString());
                    ToastExpander.showText(context, sb.toString());
                }

                eventList.set(i, Constants.STRING_EMPTY);
                return;

            }

            //Если событие в ближайшие 3 дня, то в eventDistance будет <число дней до события>, иначе: "Дней до <тип события>: " +  <число дней до события> + <день недели>
            singleEventArray[Position_eventDistance] = Long.toString(dayDiff);
            singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDiff, BDay);

            if (Age > 0) { //Возраст больше 1 года
                singleEventArray[Position_age] = Integer.toString(Age);
                singleEventArray[Position_age_caption] = getAgeString(
                        Age,
                        R.string.msg_after_year_prefix_1,
                        R.string.msg_after_year_prefix_1_,
                        R.string.msg_after_year_prefix_2_3_4,
                        R.string.msg_after_year_prefix_4_21
                );

                if (eventType.equals(eventTypesIDs.get(Constants.Type_Anniversary))) {
                    String anCaption;
                    try {
                        anCaption = context.getString(resources.getIdentifier(Constants.STRING_TYPE_WEDDING + Age, Constants.RES_TYPE_STRING, context.getPackageName()));
                    } catch (Resources.NotFoundException nfe) {
                        anCaption = null;
                    }
                    if (anCaption != null && !TextUtils.isEmpty(anCaption) && !eventCaption.contains(Constants.STRING_PARENTHESIS_OPEN)) {
                        singleEventArray[Position_eventCaption] = eventCaption.concat(Constants.STRING_PARENTHESIS_OPEN).concat(anCaption).concat(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                }
            } else if (countDaysDiff(eventDate_Date, BDay) > 0) { //Возраст до года
                singleEventArray[Position_age_caption] = countDaysDiffText(eventDate_Date, BDay, 1);
            } else {
                singleEventArray[Position_age] = Constants.STRING_MINUS1;
                singleEventArray[Position_age_caption] = Constants.STRING_EMPTY;
            }
            if (!eventSubType.equals(eventTypesIDs.get(Constants.Type_Death)) && isYear) {
                singleEventArray[Position_age_current] = countDaysDiffText(eventDate_Date, currentDay, 3);
            } else {
                singleEventArray[Position_age_current] = Constants.STRING_EMPTY;
            }

            if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay))) {
                final String zodiacSign = getZodiacInfo(ContactsEvents.ZodiacInfo.SIGN_TITLE, singleEventArray[Position_eventDateText]);
                singleEventArray[Position_zodiacSign] = zodiacSign.equals(Constants.STRING_EMPTY) ? Constants.STRING_EMPTY : zodiacSign;
                final String zodiacYear = getZodiacInfo(ContactsEvents.ZodiacInfo.YEAR_TITLE, singleEventArray[Position_eventDateText]);
                singleEventArray[Position_zodiacYear] = zodiacYear.equals(Constants.STRING_EMPTY) ? Constants.STRING_EMPTY : zodiacYear;
            }

            //Сортировка: дней до даты + (с уведомлением, не скрыт, скрыт)
            String eventKey = getEventKey(singleEventArray);

            singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + dayDiff).substring((Constants.STRING_00 + dayDiff).length() - 3)
                    + (checkIsHiddenEvent(eventKey) ? "3" : checkIsSilencedEvent(eventKey) ? "2" : "1")
                    + (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay)) ? "1"
                    : eventType.equals(eventTypesIDs.get(Constants.Type_Anniversary)) ? "2"
                    : eventType.equals(eventTypesIDs.get(Constants.Type_Custom)) ? "3"
                    : eventType.equals(eventTypesIDs.get(Constants.Type_Other)) ? "6"
                    : "4");

            eventList.set(i, TextUtils.join(Constants.STRING_EOT, singleEventArray));

            //Вычисляем 5K даты
            if (Age > 0 && eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {

                //todo: подумать: надо ли считать 5K для смертей и.т.п.?
                long days = countDaysDiff(eventDate_Date, currentDay);
                long k = (days + 365) / 5000;
                long mdays = (days + 365) % 5000;

                if (mdays >= 0 && mdays <= 365) {
                    //Формируем новую запись
                    Calendar cal5K = Calendar.getInstance();
                    int magicDayDistance = (int) (365 - mdays);
                    cal5K.add(Calendar.DATE, magicDayDistance);

                    singleEventArray[Position_eventType] = eventTypesIDs.get(Constants.Type_5K);
                    singleEventArray[Position_eventSubType] = eventTypesIDs.get(Constants.Type_5K);
                    singleEventArray[Position_eventCaption] = "5K+";
                    singleEventArray[Position_eventLabel] = sdf_DDMMYYYY.format(cal5K.getTime());
                    //для выдачи даты юбилея,а не первоначального события: sdfYear.format(sdf.parse(cal5K.get(YEAR) + "-" + (cal5K.get(Calendar.MONTH) + 1) + "-" + cal5K.get(Calendar.DAY_OF_MONTH)));
                    singleEventArray[Position_eventDate] = sdf_DDMMYYYY.format(cal5K.getTime());
                    singleEventArray[Position_eventDateText] = sdf_DDMMYYYY.format(eventDate_Date);
                    singleEventArray[Position_age] = Integer.toString(Age);
                    singleEventArray[Position_age_caption] = 5 * k + "K";
                    singleEventArray[Position_eventDistance] = Integer.toString(magicDayDistance);
                    singleEventArray[Position_eventDistanceText] = getEventDistanceText(magicDayDistance, cal5K.getTime());
                    singleEventArray[Position_eventIcon] = Integer.toString(R.drawable.ic_event_medal); //https://www.flaticon.com/free-icon/medal_610333
                    singleEventArray[Position_eventEmoji] = "🏆";
                    singleEventArray[Position_age_current] = countDaysDiffText(eventDate_Date, currentDay, 3); //Возраст текущий
                    //singleEventArray[Position_eventStorage] = STRING_STORAGE_CONTACTS; //Где искать событие по ID

                    eventKey = getEventKey(singleEventArray);
                    singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + magicDayDistance).substring((Constants.STRING_00 + magicDayDistance).length() - 3)
                            + (checkIsHiddenEvent(eventKey) ? "3" : checkIsSilencedEvent(eventKey) ? "2" : "1")
                            + "5";

                    magicList.add(TextUtils.join(Constants.STRING_EOT, singleEventArray));
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + singleEvent);
        }
    }

    private void increaseStatForAccountType(@NonNull String accountType) {
        if (!statEventTypes.containsKey(accountType)) {
            statEventTypes.put(accountType, 1);
        } else {
            Integer oldCount = statEventTypes.get(accountType);
            statEventTypes.put(accountType, (oldCount == null ? 0 : oldCount) + 1);
        }
    }

    private String getEventDistanceText(long dayDiff, @NonNull Date eventDate){
        //Если событие в ближайшие 3 дня, то вернёт "сегодня", "завтра", "послезавтра", если позже, то "через X дней" + "|в " + <день недели> + | + <MM dddd>

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
                            .append(Locale.getDefault().getLanguage().equals(getResources().getString(R.string.pref_Language_de)) ? "n" : ""); //для немецкого "in 10 TageN"
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
                            .append(getResources().getString(R.string.msg_after_event_postfix));
                }
            }
            final SimpleDateFormat sdfOut = new SimpleDateFormat(
                    preferences_list_dateformat == 3 || preferences_list_dateformat == 5 ? Constants.DATE_MMMM_D : Constants.DATE_D_MMMM,
                    Locale.forLanguageTag(currentLocale)
            );
            eventDistance
                    .append(Constants.STRING_BAR)
                    .append(getResources().getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1])
                    .append(Constants.STRING_BAR)
                    .append(sdfOut.format(c1.getTime()));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + dayDiff + Constants.STRING_EOL + eventDate);
        }
        return  eventDistance.toString();
    }

    List<String> getPreviousEvents(@NonNull List<String> dataList, @NonNull String params) {

        List<String> result = new ArrayList<>();
        if (dataList.isEmpty()) return result;

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

            List<String> newList = new ArrayList<>();
            statEventsPrevEventsFound = 0;
            for (int i = dataList.size() - 1; i >= 0 && statEventsPrevEventsFound < params_events; i--) {
                String li = dataList.get(i);
                String[] singleEventArray = li.split(Constants.STRING_EOT, -1);
                //todo: переделать на получение событий "сегодня минус предыдущие дни"
                if (!singleEventArray[Position_eventSubType].equals(eventTypesIDs.get(Constants.Type_5K)) //пропускаем 5K+
                        && !singleEventArray[Position_eventSubType].equals(eventTypesIDs.get(Constants.Type_CalendarEvent)) //пропускаем события календаря
                ) {
                    if (params_days == 365) { //нет ограничения по дням
                        newList.add(li);
                        statEventsPrevEventsFound++;
                    } else {
                        Date eventDate = null;
                        try {
                            eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                            if (eventDate != null) {
                                eventDate = addYear(eventDate, -1);
                            }
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            if (-countDaysDiff(currentDay, eventDate) <= params_days) {
                                newList.add(li);
                                statEventsPrevEventsFound++;
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
                    String[] singleEventArray = li.split(Constants.STRING_EOT, -1);
                    Date eventDate = null;
                    try {
                        eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                    } catch (Exception e) { /**/ }

                    if (eventDate != null) {

                        eventDate = addYear(eventDate, -1);
                        singleEventArray[Position_eventDate] = sdf_DDMMYYYY.format(eventDate);
                        long dayDistance = countDaysDiff(currentDay, eventDate);
                        singleEventArray[Position_eventDistance] = Long.toString(dayDistance);
                        singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDistance, eventDate);

                        int Age = 0;
                        try {
                            Age = Integer.parseInt(singleEventArray[Position_age]);
                        } catch (NumberFormatException e) { /**/ }
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

                            if (singleEventArray[Position_eventType].equals(eventTypesIDs.get(Constants.Type_Anniversary))) {
                                String anCaption;
                                try {
                                    anCaption = context.getString(getResources().getIdentifier(Constants.STRING_TYPE_WEDDING + Age, Constants.RES_TYPE_STRING, context.getPackageName()));
                                } catch (Resources.NotFoundException nfe) {
                                    anCaption = null;
                                }
                                String eventCaption = getResources().getString(R.string.event_type_anniversary);
                                if (anCaption != null && !anCaption.isEmpty()) {
                                    singleEventArray[Position_eventCaption] = eventCaption.concat(Constants.STRING_PARENTHESIS_OPEN).concat(anCaption).concat(Constants.STRING_PARENTHESIS_CLOSE);
                                } else {
                                    singleEventArray[Position_eventCaption] = eventCaption;
                                }
                            }
                        } else { //Сейчас идёт первый год жизни

                            singleEventArray[Position_age] = Constants.STRING_MINUS1;
                            singleEventArray[Position_age_caption] = Constants.STRING_EMPTY;

                        }

                        result.add(0, TextUtils.join(Constants.STRING_EOT, singleEventArray));
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return result;
    }

    void updateWidgets(int widgetID) {

        if (context == null) return;

        //Посылаем сообщения на обновление виджетов
        // https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver

        int[] ids;
        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget2x2.class));
        if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
            //Toast.makeText(context, "Widget2x2:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
            Widget2x2 myWidget = new Widget2x2();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[] {widgetID} : ids);
        }

        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget5x1.class));
        if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
            //Toast.makeText(context, "Widget5x1:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
            Widget5x1 myWidget = new Widget5x1();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[] {widgetID} : ids);
        }

        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget4x1.class));
        if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
            //Toast.makeText(context, "Widget4x1:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
            Widget4x1 myWidget = new Widget4x1();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[] {widgetID} : ids);
        }

        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetList.class));
        if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
            //Toast.makeText(context, "WidgetList:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
            WidgetList myWidget = new WidgetList();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[] {widgetID} : ids);
        }

        ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetPhotoList.class));
        if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
            //Toast.makeText(context, "WidgetPhotoList:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
            WidgetPhotoList myWidget = new WidgetPhotoList();
            myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[] {widgetID} : ids);
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
                        if (preferences_debug_on) log.append(Constants.MSG_DELETED_CHANNEL_).append(channel_id).append(Constants.STRING_EOL);

                        preferences_notification_channel_id = generator.nextInt(1000);
                        channel_id = Integer.toString(preferences_notification_channel_id);

                        channel = new NotificationChannel(channel_id, context.getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription(context.getString(R.string.pref_Notifications_Notification_Channel_Description));
                        if (preferences_notifications_ringtone != null) {
                            channel.setSound(Uri.parse(preferences_notifications_ringtone), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                            if (preferences_debug_on)
                                log.append(Constants.MSG_RINGTONE).append(Uri.parse(preferences_notifications_ringtone)).append(Constants.STRING_EOL);
                        }
                        channel.enableVibration(true);

                        notificationManager.createNotificationChannel(channel);
                        if (preferences_debug_on) log.append(Constants.MSG_CREATED_CHANNEL_).append(preferences_notification_channel_id).append(Constants.STRING_EOL);

                        setPreferences();
                    }

                } else if (channel != null) {

                    notificationManager.deleteNotificationChannel(channel_id);
                    if (preferences_debug_on) log.append(Constants.MSG_DELETED_CHANNEL_).append(channel_id).append(Constants.STRING_EOL);

                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
                    if (preferences_debug_on) log.append(Constants.MSG_NOTIFICATIONS_WERE_ENABLED).append(Constants.STRING_EOL);
                }

            } else { //Disable Daily Notifications
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    if (preferences_debug_on) log.append(Constants.MSG_NOTIFICATIONS_WERE_DISABLED).append(Constants.STRING_EOL);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void initWidgetUpdate(StringBuilder log) {

        try {

            Intent alarmIntent = new Intent(context, WidgetUpdateReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentMutable);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (preferences_widgets_update_period > 0) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.add(Calendar.HOUR_OF_DAY, preferences_widgets_update_period);

                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HOUR * preferences_widgets_update_period, pendingIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }

                    if (preferences_debug_on) {
                        if (log != null) log.append(Constants.MSG_NEXT_WIDGETUPDATE).append(sdf_DDMMYYYYHHMM.format(calendar.getTime())).append(Constants.STRING_EOL);
                    }
                }

            } else { //Disable
                if (PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentImmutable) != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    void initNotifications(StringBuilder log) {

        try {

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentMutable);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (preferences_notifications_days.size() > 0 && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, preferences_notifications_alarm_hour);
                calendar.set(Calendar.MINUTE, preferences_notifications_alarm_minute);
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
                        log.append(Constants.MSG_NEXT_NOTIFICATION).append(sdf_DDMMYYYYHHMM.format(calendar.getTime())).append(Constants.STRING_EOL);
                    }
                }

            } else { //Disable Daily Notifications
                if (PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentImmutable) != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void showNotifications(boolean forceNoEventsMessage, String channelId) {
        //https://www.journaldev.com/15468/android-notification-styling

        try {

            Set<String> notifications_days = new HashSet<>(preferences_notifications_days); //За сколько дней уведомлять
            //if (preferences_debug_on) Toast.makeText(context, "notify days: " + notifications_days, Toast.LENGTH_LONG).show();
            if (notifications_days.size() == 0) return;

            setLocale(true);

            Calendar now = Calendar.getInstance();
            Date currentDay = new Date(now.getTimeInMillis());

            List<String> listNotify = new ArrayList<>();
            for (String event: eventList) {
                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                if (singleEventArray.length == Position_attrAmount) {

                    final String eventKey = getEventKey(singleEventArray);
                    if (preferences_notifications_event_types.contains(singleEventArray[Position_eventType]) &&
                            (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey)) &&
                            (getSilencedEventsCount() == 0 || !checkIsSilencedEvent(eventKey))) {
                        Date eventDate = null;
                        try {
                            eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            if (listNotify.size() >= 50)
                                break; //https://stackoverflow.com/questions/33364368/android-system-notification-limit-per-app

                            long countDays = countDaysDiff(currentDay, eventDate);
                            if (countDays > 14) {
                                break;
                            } else if (notifications_days.contains(String.valueOf(countDays))) {
                                listNotify.add(event);
                            }
                        }
                    }
                }
            }
            if (listNotify.size() == 0 && !forceNoEventsMessage) return;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancelAll();

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
                        String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                        Date eventDate = null;
                        String eventDay = null;
                        try {
                            eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                            if (eventDate != null) {
                                eventDay = sdf_DDMM.format(eventDate);
                            }
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            textBig.append(singleEventArray[Position_eventEmoji])
                                    .append(Constants.STRING_SPACE)
                                    .append(eventDay).append(Constants.STRING_SPACE)
                                    .append(preferences_list_nameformat == 2 ? singleEventArray[Position_personFullNameAlt] : singleEventArray[Position_personFullName]);
                            if (!TextUtils.isEmpty(singleEventArray[Position_age_caption].trim()))
                                textBig.append(Constants.STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);
                            if (singleEventArray[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Constants.Type_Anniversary))) {
                                int ind1 = singleEventArray[Position_eventCaption].indexOf(Constants.STRING_PARENTHESIS_OPEN);
                                if (ind1 > -1) {
                                    textBig.append(Constants.STRING_SPACE).append(singleEventArray[Position_eventCaption].substring(ind1));
                                }
                            }
                        }
                    }
                } else {
                    textSmall = context.getString(R.string.msg_notifications_soon_no_events);
                }

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                        .setColor(this.getResources().getColor(R.color.dark_green))
                        .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                        .setContentText(textSmall)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig)) //Ограничение 5120 символов https://stackoverflow.com/questions/27124887/whats-the-max-size-of-a-bigtextstyle-notification
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setWhen(0) //https://stackoverflow.com/questions/18249871/android-notification-buttons-not-showing-up/18603076#18603076
                        .setAutoCancel(true);

                if (preferences_notifications_priority > 1 && dataNotify.length > 0) {
                    builder.setOngoing(true);
                    builder.setPriority(NotificationCompat.PRIORITY_MAX);
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if (preferences_notifications_ringtone != null) builder.setSound(Uri.parse(preferences_notifications_ringtone));
                }

                notificationManager.notify(Constants.defaultNotificationID, builder.build());

            } else { //Несколько отдельных уведомлений

                for (int i = dataNotify.length - 1; i >= 0; i--) {
                    String[] singleEventArray = dataNotify[i].split(Constants.STRING_EOT, -1);
                    Date eventDate = null;
                    String eventDay = null;
                    try {
                        eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                        if (eventDate != null) {
                            eventDay = sdf_DDMM.format(eventDate);
                        }
                    } catch (Exception e) { /**/ }

                    if (eventDate != null) {

                        textBig = new StringBuilder();
                        textBig.append(singleEventArray[Position_eventEmoji])
                                .append(Constants.STRING_SPACE)
                                .append(eventDay)
                                .append(Constants.STRING_SPACE)
                                .append(preferences_list_nameformat == 2 ? singleEventArray[Position_personFullNameAlt] : singleEventArray[Position_personFullName]);
                        if (!TextUtils.isEmpty(singleEventArray[Position_age_caption].trim()))
                            textBig.append(Constants.STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);
                        if (singleEventArray[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Constants.Type_Anniversary))) {
                            int ind1 = singleEventArray[Position_eventCaption].indexOf(Constants.STRING_PARENTHESIS_OPEN);
                            if (ind1 > -1) {
                                textBig.append(Constants.STRING_SPACE).append(singleEventArray[Position_eventCaption].substring(ind1));
                            }
                        }

                        int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                        final String[] eventDistance = singleEventArray[Position_eventDistanceText].split(Constants.STRING_PIPE, -1);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                .setColor(this.getResources().getColor(R.color.dark_green))
                                .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                                .setContentText(textBig)
                                .setContentTitle(singleEventArray[Position_eventDistance].equals(Constants.STRING_0) ?
                                        eventDistance[0] : eventDistance[0] + Constants.STRING_SPACE + eventDistance[1]
                                )
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true);

                        Intent intent = null;

                        if (preferences_notifications_on_click_action == 7) { //Основной список событий
                            intent = new Intent(context, MainActivity.class);
                            intent.setAction(Constants.ACTION_LAUNCH);
                        } else if (preferences_notifications_on_click_action >= 1 & preferences_notifications_on_click_action <=4) {
                            intent = ContactsEvents.getViewActionIntent(singleEventArray, preferences_notifications_on_click_action);
                        } else if (preferences_notifications_on_click_action == 6) { //Закрыть уведомление
                            intent = new Intent();
                        }

                        if (intent != null) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                            builder.setContentIntent(pendingIntent);
                        }

                        //todo: .addPerson для телефона и почты

                        if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Dial))
                                && !singleEventArray[Position_eventSubType].equals(eventTypesIDs.get(Constants.Type_CalendarEvent))
                                && !TextUtils.isEmpty(singleEventArray[Position_contactID])
                                && !TextUtils.isEmpty(getContactPhone(parseToLong(singleEventArray[Position_contactID])))) {

                            Intent intentDial = new Intent(context, ActionReceiver.class);
                            intentDial.setAction(Constants.ACTION_DIAL);
                            intentDial.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentDial.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                            PendingIntent pendingDial = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentDial, PendingIntentImmutable);
                            NotificationCompat.Action actionDial = new NotificationCompat.Action(0, context.getString(R.string.button_dial), pendingDial);
                            builder.addAction(actionDial);

                        }

                        final String eventKey = getEventKey(singleEventArray);
                        if (!eventKey.isEmpty() && preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Silent))) {
                            Intent intentSilent = new Intent(context, ActionReceiver.class);
                            intentSilent.setAction(Constants.ACTION_SILENT);
                            intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                            PendingIntent pendingSilent = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSilent, PendingIntentImmutable);
                            NotificationCompat.Action actionSilent = new NotificationCompat.Action(0, context.getString(R.string.button_silent), pendingSilent);
                            builder.addAction(actionSilent);
                        }

                        if (!eventKey.isEmpty() && preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Hide))) {
                            Intent intentHide = new Intent(context, ActionReceiver.class);
                            intentHide.setAction(Constants.ACTION_HIDE);
                            intentHide.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentHide.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                            PendingIntent pendingHide = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentHide, PendingIntentImmutable);
                            NotificationCompat.Action actionHide = new NotificationCompat.Action(0, context.getString(R.string.button_hide), pendingHide);
                            builder.addAction(actionHide);
                        }

                        if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Remind))) {
                            Intent intentSnooze = new Intent(context, ActionReceiver.class);
                            intentSnooze.setAction(Constants.ACTION_SNOOZE);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                            PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSnooze, PendingIntentImmutable);
                            NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                            builder.addAction(actionSnooze);
                        }

                        if (preferences_notifications_priority > 2 && preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Close))) {
                            Intent intentClose = new Intent(context, ActionReceiver.class);
                            intentClose.setAction(Constants.ACTION_CLOSE);
                            intentClose.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentClose.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify[i]);
                            PendingIntent pendingClose = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentClose, PendingIntentImmutable);
                            NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_close), pendingClose);
                            builder.addAction(actionSnooze);
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            if (preferences_notifications_ringtone != null) builder.setSound(Uri.parse(preferences_notifications_ringtone));
                        }

                        String eventSubType = singleEventArray[Position_eventSubType];
                        int roundingFactor;
                        if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_FileEvent))) {
                            roundingFactor = 1;
                        } else {
                            roundingFactor = preferences_list_photostyle;
                        }
                        builder.setLargeIcon(getContactPhoto(dataNotify[i], true, true,false, roundingFactor));

                        if (preferences_notifications_priority > 2) {
                            builder.setOngoing(true);
                            builder.setPriority(NotificationCompat.PRIORITY_MAX);
                        }

                        notificationManager.notify(notificationID, builder.build());

                    }
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    String getEventKey(@NonNull String[] singleEventArray) {

        try {

            if (!TextUtils.isEmpty(singleEventArray[Position_eventSubType].trim())) {
                if (!TextUtils.isEmpty(singleEventArray[Position_contactID].trim())) {
                    return singleEventArray[Position_contactID] + Constants.STRING_2HASH + singleEventArray[Position_eventSubType];
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventID].trim())) {
                    return singleEventArray[Position_eventID] + Constants.STRING_2HASH + singleEventArray[Position_eventSubType];
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return Constants.STRING_EMPTY;
    }

    private String[] getKeyParts(@NonNull String eventKey) {
        return eventKey.replace(Constants.STRING_2HASH, Constants.STRING_EOT).split(Constants.STRING_EOT, -1);
    }

    void snoozeNotification(@NonNull String dataNotify, int snoozeHours, Date wakeDateTime) {

        try {

            if (TextUtils.isEmpty(dataNotify) || (snoozeHours <= 0 && wakeDateTime == null)) return;

            Intent alarmIntent = new Intent(context, ActionReceiver.class);
            alarmIntent.setAction(Constants.ACTION_NOTIFY);
            alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), alarmIntent, PendingIntentMutable); //PendingIntent.FLAG_UPDATE_CURRENT);
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
                boolean finalIsSnoozed = isSnoozed;
                handler.post(() -> Toast.makeText(context, context.getString(finalIsSnoozed ? R.string.msg_snoozed_until : R.string.msg_notify_time, sdf_DDMMYYYYHHMM.format(nextUpdateTimeMillis)), Toast.LENGTH_LONG).show());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    void showNotification(String dataNotify, String channelId) {

        try {

            if (dataNotify == null || dataNotify.isEmpty()) return;

            //Toast.makeText(context, "TEST: " + dataNotify, Toast.LENGTH_LONG).show();

            String[] singleEventArray = dataNotify.split(Constants.STRING_EOT, -1);
            Date eventDate = null;
            String eventDay = null;
            try {
                eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
                if (eventDate != null) {
                    eventDay = sdf_DDMM.format(eventDate);
                }
            } catch (Exception e) { /**/ }

            if (eventDate != null) {
                StringBuilder textBig = new StringBuilder();
                textBig.append(singleEventArray[Position_eventEmoji])
                        .append(Constants.STRING_SPACE)
                        .append(eventDay)
                        .append(Constants.STRING_SPACE)
                        .append(singleEventArray[Position_personFullName]);
                if (!TextUtils.isEmpty(singleEventArray[Position_age_caption].trim()))
                    textBig.append(": ").append(singleEventArray[Position_age_caption]);

                int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                final String[] eventDistance = singleEventArray[Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                        .setColor(this.getResources().getColor(R.color.dark_green))
                        .setSmallIcon(R.drawable.ic_birthdaycountdown_icon)
                        .setContentText(textBig)
                        .setContentTitle(eventDistance[0] + Constants.STRING_SPACE + eventDistance[1])
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = null;
                if (singleEventArray[Position_eventStorage].equals(Constants.STRING_STORAGE_CONTACTS) && !TextUtils.isEmpty(singleEventArray[Position_contactID])) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (singleEventArray[Position_eventStorage].equals(Constants.STRING_STORAGE_CALENDAR) && !TextUtils.isEmpty(singleEventArray[Position_eventID])) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                }
                if (uri != null) {
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                    builder.setContentIntent(pendingIntent);
                }

                Intent intentSnooze = new Intent(context, ActionReceiver.class);
                intentSnooze.setAction(Constants.ACTION_SNOOZE); //todo: добавить все кнопки
                intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, notificationID, intentSnooze, PendingIntentImmutable);
                NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                builder.addAction(actionSnooze);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if (preferences_notifications_ringtone != null) builder.setSound(Uri.parse(preferences_notifications_ringtone));
                }

                String eventSubType = singleEventArray[Position_eventSubType];
                int roundingFactor;
                if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_FileEvent))) {
                    roundingFactor = 1;
                } else {
                    roundingFactor = preferences_list_photostyle;
                }
                builder.setLargeIcon(getContactPhoto(dataNotify, true, true, false, roundingFactor));
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(notificationID, builder.build());

            }


        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    boolean isEmptyEventList() {return eventList.isEmpty();}

    @NonNull static String checkForNull(String strIn) {return !TextUtils.isEmpty(strIn) ? strIn : Constants.STRING_EMPTY;}

    void clearHiddenEvents() {

        try {

            if (getHiddenEventsCount() > 0) preferences_hiddenEvents.clear();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    int getHiddenEventsCount() {

        try {

            return preferences_hiddenEvents == null || preferences_hiddenEvents.isEmpty() ? 0 : preferences_hiddenEvents.size();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    boolean checkIsHiddenEvent(@NonNull String key) {

        try {

            return !key.isEmpty() && preferences_hiddenEvents != null && preferences_hiddenEvents.contains(key);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setHiddenEvent(@NonNull String key) {

        try {

            if (key.isEmpty() || preferences_hiddenEvents == null || !preferences_hiddenEvents.add(key)) return false;

            //clearDeadlinkHiddenEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.apply();

            //if (preferences_debug_on) Toast.makeText(context, "Hided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    boolean unsetHiddenEvent(@NonNull String key) {

        try {

            if (!checkIsHiddenEvent(key)) return false;
            if (preferences_hiddenEvents == null || !preferences_hiddenEvents.remove(key)) return false;

            //clearDeadlinkHiddenEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);

            //Если удалили последнее событие - скидываем режим на стандартный
            if (preferences_list_events_scope == Constants.pref_Events_Scope_Hidden && preferences_hiddenEvents.isEmpty()) {
                preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
            }

            editor.apply();

            //if (preferences_debug_on) Toast.makeText(context, "Unhided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    void clearSilencedEvents() {

        try {

            if (getSilencedEventsCount() > 0) preferences_silentEvents.clear();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    int getSilencedEventsCount() {

        try {

            return preferences_silentEvents == null || preferences_silentEvents.isEmpty() ? 0 : preferences_silentEvents.size();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    boolean checkIsSilencedEvent(@NonNull String key) {

        try {

            return !key.isEmpty() && preferences_silentEvents != null && preferences_silentEvents.contains(key);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setSilencedEvent(@NonNull String key) {

        try {

            if (key.isEmpty() || preferences_silentEvents == null || !preferences_silentEvents.add(key)) return false;

            //clearDeadlinkSilencedEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            editor.apply();

            //if (preferences_debug_on) Toast.makeText(context, "Silenced event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    boolean unsetSilencedEvent(@NonNull String key) {

        try {

            if (!checkIsSilencedEvent(key)) return false;
            if (preferences_silentEvents == null || !preferences_silentEvents.remove(key)) return false;

            //clearDeadlinkSilencedEvents();

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);

            //Если удалили последнее событие - скидываем режим на стандартный
            if (preferences_list_events_scope == Constants.pref_Events_Scope_Silenced && preferences_silentEvents.isEmpty()) {
                preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
            }

            editor.apply();

            //if (preferences_debug_on) Toast.makeText(context, "Unsilenced event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    @NonNull
    String getMergedID(@NonNull String linkID){

        try {

            return checkForNull(preferences_mergedIDs.get(linkID));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
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
                    someSets.add(elementID + Constants.STRING_COLON_SPACE + preferences_mergedIDs.get(elementID));
                }
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_MergedID_key), someSets);
            editor.apply();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    void clearDeadlinkSilencedEvents() {

        try {

            if (getSilencedEventsCount() == 0) return;

            Set<String> toRemove = new HashSet<>();
            for (String event: preferences_silentEvents) {
                if (event.equals(Constants.STRING_EMPTY)) {
                    toRemove.add(event);
                    continue;
                }
                final String[] keyParts = getKeyParts(event);
                if (keyParts[1].equals(eventTypesIDs.get(Constants.Type_CalendarEvent))) {
                    if (!set_events_ids.contains(keyParts[0])) toRemove.add(event);
                } else {
                    if (!set_contacts_ids.contains(keyParts[0])) toRemove.add(event);
                }
            }
            if (toRemove.size() > 0) {
                preferences_silentEvents.removeAll(toRemove);
                ToastExpander.showText(context, context.getString(R.string.msg_filter_clean_silenced_result) + toRemove.size());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void clearDeadlinkHiddenEvents() {

        try {

            if (getHiddenEventsCount() == 0) return;

            Set<String> toRemove = new HashSet<>();
            for (String event: preferences_hiddenEvents) {
                if (event.equals(Constants.STRING_EMPTY)) {
                    toRemove.add(event);
                    continue;
                }
                final String[] keyParts = getKeyParts(event);
                if (keyParts[1].equals(eventTypesIDs.get(Constants.Type_CalendarEvent))) {
                    if (!set_events_ids.contains(keyParts[0])) toRemove.add(event);
                } else {
                    if (!set_contacts_ids.contains(keyParts[0])) toRemove.add(event);
                }
            }
            if (toRemove.size() > 0) {
                preferences_hiddenEvents.removeAll(toRemove);
                ToastExpander.showText(context,  context.getString(R.string.msg_filter_clean_hidden_result) + toRemove.size());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void setWidgetPreference(int id, @NonNull String value) {

        if (context == null) return;

        try {

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString(context.getString(R.string.widget_config_PrefName) + id, value);
            editor.apply();

            if (preferences_debug_on) ToastExpander.showText(context, String.format(Constants.MSG_WIDGET_PREFS_SAVED, id) + value);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @NonNull List<String> getWidgetPreference(int widgetID, String widgetType) {

        final String defaultPrefString;
        if (widgetType != null && widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref_List);
        } else if (widgetType != null && widgetType.equals(Constants.WIDGET_TYPE_PHOTO_LIST)) {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref_PhotoList);
        } else {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref);
        }

        List<String> defaultPref = Arrays.asList(defaultPrefString.split(Constants.STRING_COMMA, -1));
        if (context == null) return defaultPref;

        try {

            String strPref = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.widget_config_PrefName) + widgetID, defaultPrefString);
            String[] pref = strPref.split(Constants.STRING_COMMA, -1);
            List<String> prefWidget = new ArrayList<>(Arrays.asList(pref));

            //Заполнение дефолтными значениями
            while (prefWidget.size() < defaultPref.size()) {
                prefWidget.add(defaultPref.get(prefWidget.size()));
            }

            return prefWidget;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return defaultPref;
        }

    }

    boolean hasPreferences(String name) {
        return PreferenceManager.getDefaultSharedPreferences(context).getAll().containsKey(name);
    }

    void removeWidgetPreference(int id) {

        try {

            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(context.getString(R.string.widget_config_PrefName) + id).apply();
            if (preferences_debug_on) ToastExpander.showText(context, String.format(Constants.MSG_WIDGET_PREFS_REMOVED, id));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    Set<String> getPreferences_Accounts() {
        return preferences_Accounts;
    }

    void setPreferences_Accounts(Set<String> preferences_Accounts) {
        this.preferences_Accounts = preferences_Accounts;
    }

    Set<String> getPreferences_Calendars(String eventType) {

        if (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {
            return preferences_BirthDay_calendars;
        } else if (eventType.equals(eventTypesIDs.get(Constants.Type_Other))) {
            return preferences_Otherevent_calendars;
        } else {
            return new HashSet<>();
        }

    }
    void setPreferences_Calendars(String eventType, Set<String> preferences_Calendars) {

        if (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {
            this.preferences_BirthDay_calendars = preferences_Calendars;
        } else if (eventType.equals(eventTypesIDs.get(Constants.Type_Other))) {
            this.preferences_Otherevent_calendars = preferences_Calendars;
        }

    }

    void setPreferences_Files(String eventType, Set<String> preferences_Files) {

        if (eventType.equals(eventTypesIDs.get(Constants.Type_BirthDay))) {
            this.preferences_Birthday_files = preferences_Files;
        } else if (eventType.equals(eventTypesIDs.get(Constants.Type_Other))) {
            this.preferences_Otherevent_files = preferences_Files;
        }

    }

    void setPreferences_AlarmTime(int alarmHour, int alarmMinute) {
        this.preferences_notifications_alarm_hour = alarmHour;
        this.preferences_notifications_alarm_minute = alarmMinute;
    }

    public int getPreferences_IconPackNumber() {
        return preferences_IconPackNumber;
    }

    void setPreferences_IconPackNumber(int packNumber) {
        preferences_IconPackNumber = packNumber;
    }

    void setPreferences_List_FontMagnify(int intDistance, int intName, int intDetails, int intDate, int intAge) {
        preferences_list_magnify_distance = intDistance;
        preferences_list_magnify_name = intName;
        preferences_list_magnify_details = intDetails;
        preferences_list_magnify_date = intDate;
        preferences_list_magnify_age = intAge;
    }

    void showAnniversaryList(Context context) {

        try {

            ArrayList<String> items = new ArrayList<>();
            for(int i = 1; i <= 100; i++) {
                String anCaption;
                try {
                    anCaption = context.getString(getResources().getIdentifier(Constants.STRING_TYPE_WEDDING + i, Constants.RES_TYPE_STRING, context.getPackageName()));
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
            ta.recycle();

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    String setHTMLColor(String msg, int color) {
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
                    /*int[] attrs = {android.R.attr.textColorSecondary};
                    TypedArray ta = context.obtainStyledAttributes(attrs);
                    colorId = ta.getColor(0, 0);
                    ta.recycle();*/
                    return msg;
            }
            return String.format(Constants.HTML_COLOR, Integer.toHexString(ContextCompat.getColor(context, colorId) & 0x00ffffff), msg);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return msg;
    }

    static String normalizeName(String inName) {

        if (inName == null) {
            return null;
        } else {
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

    @NonNull
    static Long parseToLong(String strIn) {

        try {
            return Long.parseLong(strIn);
        } catch (NumberFormatException e) {
            return Long.parseLong(Constants.STRING_0);
        }

    }

    enum FormatDate {
        WithYear,
        WithoutYear
    }

    @NonNull
    String getDateFormated(String dateIn, FormatDate format) {

        String resultString = Constants.STRING_EMPTY;
        if (TextUtils.isEmpty(dateIn)) return resultString;
        if (preferences_list_dateformat == 2 && format == FormatDate.WithYear) return dateIn; // DD.MM.YYYY

        try {

            final Locale locale = Locale.forLanguageTag(currentLocale);
            SimpleDateFormat sdfInY = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale);
            SimpleDateFormat sdfIn = new SimpleDateFormat(Constants.DATE_DD_MM, locale);
            SimpleDateFormat sdfOut = null;
            Date eventDate = null;
            boolean isYearPresent = false;

            switch (preferences_list_dateformat) {

                case 2: // DD.MM.YYYY

                    sdfOut = new SimpleDateFormat(Constants.DATE_DD_MM, locale);
                    try {
                        eventDate = sdfInY.parse(dateIn);
                    } catch (Exception e) {
                        try {
                            eventDate = sdfIn.parse(dateIn);
                        } catch (Exception e2) { /**/ }
                    }
                    if (eventDate != null) resultString = sdfOut.format(eventDate);
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
                        if (format == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_MM_DD_YYYY, locale);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_MM_DD, locale);
                        }
                        if (sdfOut != null) resultString = sdfOut.format(eventDate);
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
                        if (format == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_UK, locale);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_UK_NO_YEAR, locale);
                        }
                        if (sdfOut != null) resultString = sdfOut.format(eventDate);
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
                        if (format == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_IND, locale);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_IND_NO_YEAR, locale);
                        }
                        if (sdfOut != null) resultString = sdfOut.format(eventDate);
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
                        if (format == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_DD_MMM_YYYY, locale);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_DD_MMM, locale);
                        }
                        if (sdfOut != null) resultString = sdfOut.format(eventDate);
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
                        if (format == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_D_MMMM_YYYY, locale);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_D_MMMM, locale);
                        }
                        if (sdfOut != null) resultString = sdfOut.format(eventDate);
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
                        if (format == FormatDate.WithYear && isYearPresent) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(),
                                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(),
                                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        }
                    }
            }

        } catch (Exception e) { /**/ }

        return resultString;

    }

    enum ZodiacInfo{
        SIGN,
        SIGN_TITLE,
        YEAR,
        YEAR_TITLE
    }

    @NonNull
    String getZodiacInfo(ZodiacInfo requestInfo, String strBirthday) {

        //todo: сделать вариант получения знака по уточнённым в2016 года датам
        // https://habr.com/ru/post/397591/
        // https://habr.com/ru/post/397729/
        // https://www.stylist.co.uk/astrology/nasa-horoscope-star-sign-zodiac-ophiuchus-personality-astrology-astronomy/248217

        try {

            if (requestInfo == ZodiacInfo.SIGN || requestInfo == ZodiacInfo.SIGN_TITLE) {
                final int eventDay;
                final int eventMonth;
                try {
                    eventDay = Integer.parseInt(strBirthday.substring(0, 2));
                    eventMonth = Integer.parseInt(strBirthday.substring(3, 5));
                } catch (NumberFormatException e) {
                    return Constants.STRING_EMPTY;
                }

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
                                                                                                                "♐".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_sagittarius) : Constants.STRING_EMPTY) :
                                                                                                        "♏".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_scorpio) : Constants.STRING_EMPTY) :
                                                                                                "♎".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_libra) : Constants.STRING_EMPTY) :
                                                                                        "♍".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_virgo) : Constants.STRING_EMPTY) :
                                                                                "♌".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_leo) : Constants.STRING_EMPTY) :
                                                                        "♋".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_cancer) : Constants.STRING_EMPTY) :
                                                                "♊".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_gemini) : Constants.STRING_EMPTY) :
                                                        "♉".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_taurus) : Constants.STRING_EMPTY) :
                                                "♈".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_aries) : Constants.STRING_EMPTY) :
                                        "♓".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_pisces) : Constants.STRING_EMPTY) :
                                "♒".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_aquarius) : Constants.STRING_EMPTY) :
                        "♑".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_capricorn) : Constants.STRING_EMPTY);

            } else if (requestInfo == ZodiacInfo.YEAR || requestInfo == ZodiacInfo.YEAR_TITLE) {

                int eventYear;
                try {
                    eventYear = Integer.parseInt("0" + strBirthday.substring(6));
                } catch (Exception e) {
                    eventYear = 0;
                }
                if (eventYear == 0) return Constants.STRING_EMPTY;

                switch (eventYear % 12) {
                    case 0: return "\uD83D\uDC12".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_monkey) : Constants.STRING_EMPTY);
                    case 1: return "\uD83D\uDC13".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rooster) : Constants.STRING_EMPTY);
                    case 2: return "\uD83D\uDC15".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_dog) : Constants.STRING_EMPTY);
                    case 3: return "\uD83D\uDC16".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_pig) : Constants.STRING_EMPTY);
                    case 4: return "\uD83D\uDC00".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rat) : Constants.STRING_EMPTY);
                    case 5: return "\uD83D\uDC02".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_ox) : Constants.STRING_EMPTY);
                    case 6: return "\uD83D\uDC05".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_tiger) : Constants.STRING_EMPTY);
                    case 7: return "\uD83D\uDC07".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rabbit) : Constants.STRING_EMPTY);
                    case 8: return "\uD83D\uDC09".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_dragon) : Constants.STRING_EMPTY);
                    case 9: return "\uD83D\uDC0D".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_snake) : Constants.STRING_EMPTY);
                    case 10: return "\uD83D\uDC0E".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_horse) : Constants.STRING_EMPTY);
                    case 11: return "\uD83D\uDC11".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_sheep) : Constants.STRING_EMPTY);
                    default: return Constants.STRING_EMPTY;
                }

            } else {

                return Constants.STRING_EMPTY;

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    private static class QuizQuestion {
        final String type;
        final String question;
        final List<String> actions;
        String event;

        QuizQuestion(String type, String question) {

            this.type = type;
            this.question = question;
            this.actions = new ArrayList<>();

        }

        QuizQuestion(String type, String question, @SuppressWarnings("SameParameterValue") String action) {

            this(type, question);
            this.actions.add(action);

        }

        @NonNull
        public String toString() {

            return this.type + Constants.STRING_COMMA +
                    this.question + Constants.STRING_COMMA +
                    this.actions.toString();

        }
    }

    void quizCheckAndGo(String question, String answer) {

        try {

            final boolean isNotifyInterface = preferences_quiz_interface.equals(getResources().getString(R.string.pref_Quiz_Interface_Notify)) || !isUIopen;

            if (isNotifyInterface && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //для Android 8+

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                NotificationChannel channel = notificationManager.getNotificationChannel(Integer.toString(Constants.defaultQuizID));

                if (channel == null) {
                    if (preferences_debug_on) handler.post(() -> Toast.makeText(context, "Create Quiz notification channel", Toast.LENGTH_LONG).show());
                    notificationManager.deleteNotificationChannel(Integer.toString(Constants.defaultQuizID));
                    channel = new NotificationChannel(Integer.toString(Constants.defaultQuizID), context.getString(R.string.pref_Notifications_Quiz_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                    channel.setSound(null, null);
                    channel.setShowBadge(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        channel.setAllowBubbles(true);
                    }
                    notificationManager.createNotificationChannel(channel);
                }
            }

            //Показываем результаты
            if (question != null & answer != null) {
                String[] a = answer.split(Constants.STRING_EOT, -1);

                if (a.length < 2) {

                    handler.post(() -> Toast.makeText(context, question + "\n\n" + answer, Toast.LENGTH_LONG).show());

                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { //deprecated in API level 30 https://developer.android.com/reference/android/widget/Toast#getView()

                    @SuppressLint("ShowToast") Toast toast = Toast.makeText(context, (isNotifyInterface ? question + "\n\n" : Constants.STRING_EMPTY) + a[2], Toast.LENGTH_LONG);
                    View toastView = toast.getView();
                    if (toastView != null) {
                        //https://stackoverflow.com/questions/11288475/custom-toast-on-android-a-simple-example
                        TextView toastMessage = toastView.findViewById(android.R.id.message);
                        toastMessage.setTextColor(getResources().getColor(R.color.white));
                        toastMessage.setTextSize(15);
                        toastView.setBackgroundColor(getResources().getColor(a[0].equals("1") ? R.color.dark_green : R.color.dark_red));
                    }
                    ToastExpander.showFor(toast, 5000);

                } else {

                    //todo: Snackbar
                    //https://stackoverflow.com/questions/31428437/how-to-add-snackbars-in-a-broadcastreceiver
                    //Snackbar snack = Snackbar.make(this, question + "\n\n" + a[2], Snackbar.LENGTH_LONG);

                    //https://commonsware.com/blog/Android/2010/05/26/html-tags-supported-by-textview.html
                    //https://www.w3schools.com/colors/colors_names.asp

                    //todo: https://issuetracker.google.com/190786028 How to check Android 12 API level?
                    handler.post(() -> Toast.makeText(context, HtmlCompat.fromHtml(
                            (isNotifyInterface && Build.VERSION.SDK_INT < Build.VERSION_CODES.S ? "<b>" + question.replace(Constants.STRING_EOL, "</b><br>") + "<br><br>" : Constants.STRING_EMPTY) +
                                    "<font color='" + (a[0].equals("1") ? "#238A10" : "#dd0000") + "'>" + a[2].replace(Constants.STRING_EOL, "<br>") + Constants.HTML_COLOR_END
                            , HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show());
                }
            }

            //Показываем следующий вопрос
            final int maxQuizGets = 10;
            int tryQuiz = 1;
            QuizQuestion quest = null;

            while (quest == null && tryQuiz <= maxQuizGets) {
                quest = quizGetQuestion();
                tryQuiz++;
            }

            if (quest == null) {
                handler.post(() -> Toast.makeText(context, HtmlCompat.fromHtml("<font color='#dd0000'>" + getResources().getString(R.string.quiz_msg_error_get_question) + Constants.HTML_COLOR_END, 0), Toast.LENGTH_LONG).show());
                return;
            }
            //if (preferences_debug_on) Toast.makeText(context, quest.toString(), Toast.LENGTH_LONG).show();

            if (isNotifyInterface) {

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Integer.toString(Constants.defaultQuizID))
                        .setColor(getResources().getColor(R.color.dark_green))
                        .setSmallIcon(R.drawable.quiz_icon)
                        .setContentTitle(quest.type)
                        .setContentText(quest.question)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(quest.question))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setWhen(0)
                        .setAutoCancel(true);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { //
                    builder.setSound(Uri.parse("content://media/internal/audio/media/29"));
                }

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                builder.setContentIntent(pendingIntent);

                if (!TextUtils.isEmpty(quest.event)) {
                    builder.setLargeIcon(getContactPhoto(quest.event, true, true,false, preferences_list_photostyle));

                    String[] eventInfo = quest.event.split(Constants.STRING_EOT, -1);
                    intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = null;
                    if (!TextUtils.isEmpty(eventInfo[Position_contactID])) { //singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) &&
                        uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, eventInfo[Position_contactID]);
                    } else if (!TextUtils.isEmpty(eventInfo[Position_eventID])) { //singleEventArray[Position_eventStorage].equals(STRING_STORAGE_CALENDAR) &&
                        uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventInfo[Position_eventID]);
                    }
                    if (uri != null) {
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                        builder.setContentIntent(pendingIntent);
                    }
                }

                PendingIntent pendingQuiz;
                NotificationCompat.Action actionQuiz;
                Intent intentQuiz;

                for (String action : quest.actions) {
                    String[] a = action.split(Constants.STRING_EOT, -1);

                    if (a.length > 2 && !a[2].equals(Constants.STRING_MINUS)) {
                        intentQuiz = new Intent(context, QuizReceiver.class);
                        intentQuiz.setAction(a[2]);
                        intentQuiz.putExtra(Constants.EXTRA_QUIZ_QUESTION, quest.type + Constants.STRING_EOL + quest.question);
                        intentQuiz.putExtra(Constants.EXTRA_QUIZ_RESULT, action);
                        pendingQuiz = PendingIntent.getBroadcast(context, Constants.defaultQuizID, intentQuiz, PendingIntentImmutable);
                        actionQuiz = new NotificationCompat.Action(0, a[1], pendingQuiz);
                        builder.addAction(actionQuiz);
                    }
                }

                notificationManager.notify(Constants.defaultQuizID, builder.build());

            } else {

                AlertDialog.Builder builder;
                AlertDialog alertToShow;
                QuizQuestion finalQuest = quest;

                builder = new AlertDialog.Builder(new ContextThemeWrapper(context, preferences_theme.themeDialog));
                builder.setTitle(quest.type);
                builder.setMessage(Constants.STRING_EOL + quest.question);
                if (quest.event != null && !quest.event.isEmpty()) {
                    builder.setIcon(new BitmapDrawable(resources, ContactsEvents.getInstance().getContactPhoto(quest.event, true, true,false, preferences_list_photostyle)));
                }

                for (int i = 0; i < quest.actions.size(); i++) {
                    String action = quest.actions.get(i);
                    String[] a = action.split(Constants.STRING_EOT, -1);

                    if (a.length > 2 && !a[2].equals(Constants.STRING_MINUS)) {
                        switch (i) {
                            case 0:
                                builder.setNeutralButton(a[1], (dialog, which) -> {
                                    dialog.dismiss();
                                    quizCheckAndGo(finalQuest.type + Constants.STRING_EOL + finalQuest.question, action);
                                });
                                break;
                            case 1:
                                builder.setNegativeButton(a[1], (dialog, which) -> {
                                    dialog.dismiss();
                                    quizCheckAndGo(finalQuest.type + Constants.STRING_EOL + finalQuest.question, action);
                                });
                                break;
                            case 2:
                                builder.setPositiveButton(a[1], (dialog, which) -> {
                                    dialog.dismiss();
                                    quizCheckAndGo(finalQuest.type + Constants.STRING_EOL + finalQuest.question, action);
                                });
                                break;
                        }
                    }
                }

                alertToShow = builder.create();
                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();


            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    QuizQuestion quizGetQuestion() {

        try {

            int seed = generator.nextInt(3);
            if (seed == 0) {

                return quizGetQuestionMonth01();

            } else if (seed == 1) {

                return quizGetQuestionYear01();

            } else {

                return quizGetQuestionAge01();

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question), Constants.quiz_error_button_OK);
        }
    }

    /** Вопрос "В каком месяце родился?" */
    QuizQuestion quizGetQuestionMonth01() {

        QuizQuestion result;

        try {

            if (set_events_birthdays.size() == 0) return null;

            //Получаем случайный день рождения
            int tryEvent = 0;
            boolean isBirthday = false;
            String event = Constants.STRING_EMPTY;
            String[] eventInfo = null;
            Integer BMonth = -1;
            String BMonthLong = Constants.STRING_EMPTY;
            SimpleDateFormat sdfMonthLong = new SimpleDateFormat( "LLLL", Locale.forLanguageTag(currentLocale));
            Date BDay;

            while (!isBirthday && tryEvent <= eventListUnsorted.size()) {
                tryEvent++;
                int randomInt = generator.nextInt(eventListUnsorted.size());
                event = eventListUnsorted.get(randomInt);
                eventInfo = event.split(Constants.STRING_EOT, -1);
                final String eventKey = getEventKey(eventInfo);
                if (
                        eventInfo[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay)) &&
                                (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey))
                ) {
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDate]);
                        if (BDay != null) {
                            Calendar cal = getCalendarFromDate(BDay);
                            BMonth = cal.get(Calendar.MONTH);
                            BMonthLong = sdfMonthLong.format(cal.getTime()).toUpperCase();
                            isBirthday = true;
                        }
                    } catch (Exception e) { /**/ }
                }
            }
            if (!isBirthday) return null;

            //Формируем информацию о персоне
            StringBuilder personInfo = new StringBuilder(preferences_list_nameformat == 2 ? eventInfo[Position_personFullNameAlt] : eventInfo[Position_personFullName]);
            final boolean isOrg = eventInfo[Position_organization].trim().length() > 0;
            final boolean isTitle = eventInfo[Position_title].trim().length() > 0;
            if (isOrg || isTitle) {
                personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                if (isOrg) {
                    personInfo.append(eventInfo[Position_organization].trim());
                    if (isTitle) personInfo.append(Constants.STRING_COMMA_SPACE).append(eventInfo[Position_title].trim());
                } else {
                    personInfo.append(eventInfo[Position_title].trim());
                }
                personInfo.append(Constants.STRING_PARENTHESIS_CLOSE);
            }

            result = new QuizQuestion(getResources().getString(R.string.quiz_month01_title), personInfo.toString());
            result.event = event;

            //Заполняем варианты ответов
            boolean isUnique = false;
            boolean hasBMonth = false;
            Integer[] rollMonths = new Integer[3];

            while (!(hasBMonth && isUnique)) {
                rollMonths[0] = generator.nextInt(12);
                rollMonths[1] = generator.nextInt(12);
                rollMonths[2] = generator.nextInt(12);

                hasBMonth = rollMonths[0].equals(BMonth) || rollMonths[1].equals(BMonth) || rollMonths[2].equals(BMonth);
                isUnique = !rollMonths[0].equals(rollMonths[1]) && !rollMonths[0].equals(rollMonths[2]) && !rollMonths[1].equals(rollMonths[2]);
            }

            Calendar cal = Calendar.getInstance();
            for (Integer m: rollMonths) {
                boolean isBMonth = m.equals(BMonth);
                StringBuilder sb = new StringBuilder(isBMonth ? Constants.STRING_1 : Constants.STRING_0).append(Constants.STRING_EOT);
                cal.set(Calendar.DATE, 1);
                cal.set(Calendar.MONTH, m);
                String monthName = sdfMonthLong.format(cal.getTime()).toUpperCase();
                sb.append(monthName).append(Constants.STRING_EOT);
                if (isBMonth) {
                    sb.append(getResources().getString(R.string.quiz_answer_true, BMonthLong, eventInfo[Position_eventDateText]));
                } else {
                    sb.append(getResources().getString(R.string.quiz_answer_false, monthName, BMonthLong, eventInfo[Position_eventDateText]));
                }
                result.actions.add(sb.toString());
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question) + Constants.STRING_COLON_SPACE + e, Constants.quiz_error_button_OK);
        }
    }

    /** Вопрос "В каком году родился?" */
    QuizQuestion quizGetQuestionYear01() {

        QuizQuestion result;

        try {

            if (set_events_birthdays.size() == 0) return null;

            //Получаем случайный день рождения
            int tryEvent = 0;
            boolean isBirthday = false;
            String event = Constants.STRING_EMPTY;
            String[] eventInfo = null;
            int BYear = -1;
            String BYearLong = Constants.STRING_EMPTY;
            Date BDay;

            while (!isBirthday && tryEvent <= eventListUnsorted.size()) {
                tryEvent++;
                int randomInt = generator.nextInt(eventListUnsorted.size());
                event = eventListUnsorted.get(randomInt);
                eventInfo = event.split(Constants.STRING_EOT, -1);
                final String eventKey = getEventKey(eventInfo);
                if (
                        eventInfo[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay)) &&
                                (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey))
                ) {
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDateText]);
                        if (BDay != null) {
                            Calendar cal = getCalendarFromDate(BDay);
                            BYear = cal.get(Calendar.YEAR);
                            BYearLong = Integer.toString(BYear);
                            if (BYear > 0 && BYear != 1604 && BYear != 1904) isBirthday = true; //наверное, это dead code
                        }
                    } catch (Exception e) { /**/ }
                }
            }
            if (!isBirthday) return null;

            //Формируем информацию о персоне
            StringBuilder personInfo = new StringBuilder(preferences_list_nameformat == 2 ? eventInfo[Position_personFullNameAlt] : eventInfo[Position_personFullName]);
            final boolean isOrg = eventInfo[Position_organization].trim().length() > 0;
            final boolean isTitle = eventInfo[Position_title].trim().length() > 0;
            if (isOrg || isTitle) {
                personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                if (isOrg) {
                    personInfo.append(eventInfo[Position_organization].trim());
                    if (isTitle) personInfo.append(Constants.STRING_COMMA_SPACE).append(eventInfo[Position_title].trim());
                } else {
                    personInfo.append(eventInfo[Position_title].trim());
                }
                personInfo.append(Constants.STRING_PARENTHESIS_CLOSE);
            }

            result = new QuizQuestion(getResources().getString(R.string.quiz_year01_title), personInfo.toString());
            result.event = event;

            //Заполняем варианты ответов
            ArrayList<Integer> rollYears = new ArrayList<>(3);
            rollYears.add(BYear);
            rollYears.add(BYear);
            rollYears.add(BYear);

            boolean isUnique = false;
            while (!isUnique) {
                rollYears.set(1, BYear - 10 + generator.nextInt(21));
                rollYears.set(2, BYear - 10 + generator.nextInt(21));
                isUnique = !rollYears.get(0).equals(rollYears.get(1)) && !rollYears.get(0).equals(rollYears.get(2)) && !rollYears.get(1).equals(rollYears.get(2));
            }
            Collections.shuffle(rollYears, generator);

            for (Integer year: rollYears) {
                boolean isBYear = year.equals(BYear);
                StringBuilder sb = new StringBuilder(isBYear ? Constants.STRING_1 : Constants.STRING_0).append(Constants.STRING_EOT).append(year).append(Constants.STRING_EOT);
                if (isBYear) {
                    sb.append(getResources().getString(R.string.quiz_answer_true, BYearLong, eventInfo[Position_eventDateText]));
                } else {
                    sb.append(getResources().getString(R.string.quiz_answer_false, Integer.toString(year).toUpperCase(), BYearLong, eventInfo[Position_eventDateText]));
                }
                //Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
                result.actions.add(sb.toString());
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question) + Constants.STRING_COLON_SPACE + e, Constants.quiz_error_button_OK);
        }
    }

    /** Вопрос "Сколько лет исполнится?" */
    QuizQuestion quizGetQuestionAge01() {

        QuizQuestion result;

        try {

            if (set_events_birthdays.size() == 0) return null;

            //Получаем случайный день рождения
            int tryEvent = 0;
            boolean isBirthday = false;
            String event = Constants.STRING_EMPTY;
            String[] eventInfo = null;
            int BYear;
            Date BDay = null;
            Date eventDay = null;

            while (!isBirthday && tryEvent <= eventListUnsorted.size()) {
                tryEvent++;
                int randomInt = generator.nextInt(eventListUnsorted.size());
                event = eventListUnsorted.get(randomInt);
                eventInfo = event.split(Constants.STRING_EOT, -1);
                final String eventKey = getEventKey(eventInfo);
                if (
                        eventInfo[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay)) &&
                                (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey))
                ) {
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDateText]);
                        eventDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDate]);
                        if (BDay != null) {
                            Calendar cal = getCalendarFromDate(BDay);
                            BYear = cal.get(Calendar.YEAR);
                            if (BYear > 0 && BYear != 1604 && BYear != 1904) isBirthday = true; //наверное, это dead code
                        }
                    } catch (Exception e) { /**/ }
                }
            }
            if (!isBirthday || eventDay == null) return null;

            //Формируем информацию о персоне
            Date currentDay = removeTime(Calendar.getInstance()).getTime();
            boolean isDead = set_events_deaths.contains(eventInfo[Position_contactID]); //Но есть годовщина смерти
            boolean isPassedBDay = (getCalendarFromDate(eventDay).get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)) || (eventDay.equals(currentDay));
            //ToastExpander.showFor(Toast.makeText(context, getCalendarFromDate(eventDay).get(YEAR) + "!=" + Calendar.getInstance().get(YEAR) + "=" + isPassedBDay, Toast.LENGTH_LONG), 7000);

            StringBuilder personInfo = new StringBuilder(preferences_list_nameformat == 2 ? eventInfo[Position_personFullNameAlt] : eventInfo[Position_personFullName]);
            final boolean isOrg = eventInfo[Position_organization].trim().length() > 0;
            final boolean isTitle = eventInfo[Position_title].trim().length() > 0;
            if (isOrg || isTitle) {
                personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                if (isOrg) {
                    personInfo.append(eventInfo[Position_organization].trim());
                    if (isTitle) personInfo.append(Constants.STRING_COMMA_SPACE).append(eventInfo[Position_title].trim());
                } else {
                    personInfo.append(eventInfo[Position_title].trim());
                }
                personInfo.append(Constants.STRING_PARENTHESIS_CLOSE);
            }

            String quizTitle;
            int Age;

            if (isDead) {
                quizTitle = getResources().getString(R.string.quiz_age01_title_dead);
                Age = countYearsDiff(BDay, currentDay);
            } else if (isPassedBDay) {
                quizTitle = getResources().getString(R.string.quiz_age01_title_past);
                Age = countYearsDiff(BDay, currentDay);
            } else {
                quizTitle = getResources().getString(R.string.quiz_age01_title_future);
                Age = countYearsDiff(BDay, currentDay) + 1;
            }

            result = new QuizQuestion(quizTitle, personInfo.toString());
            result.event = event;

            //Заполняем варианты ответов
            ArrayList<Integer> rollAges = new ArrayList<>(3);
            rollAges.add(Age);
            rollAges.add(Age);
            rollAges.add(Age);

            boolean isUnique = false;
            while (!isUnique) {
                rollAges.set(1, Age - Math.min(10, Age) + generator.nextInt(Math.min(10, Age) * 2 + 1));
                rollAges.set(2, Age - Math.min(10, Age) + generator.nextInt(Math.min(10, Age) * 2 + 1));
                isUnique = !rollAges.get(0).equals(rollAges.get(1)) && !rollAges.get(0).equals(rollAges.get(2)) && !rollAges.get(1).equals(rollAges.get(2));
            }
            Collections.shuffle(rollAges, generator);

            String ageLong = getAgeString(
                    Age,
                    R.string.msg_after_year_prefix_1,
                    R.string.msg_after_year_prefix_1_,
                    R.string.msg_after_year_prefix_2_3_4,
                    R.string.msg_after_year_prefix_4_21
            ).toUpperCase();

            for (Integer age: rollAges) {
                boolean isAge = age.equals(Age);
                String ageRollLong = getAgeString(
                        age,
                        R.string.msg_after_year_prefix_1,
                        R.string.msg_after_year_prefix_1_,
                        R.string.msg_after_year_prefix_2_3_4,
                        R.string.msg_after_year_prefix_4_21
                ).toUpperCase();
                StringBuilder sb = new StringBuilder(isAge ? Constants.STRING_1 : Constants.STRING_0).append(Constants.STRING_EOT).append(ageRollLong).append(Constants.STRING_EOT);
                if (isAge) {
                    sb.append(getResources().getString(R.string.quiz_answer_true, ageLong, eventInfo[Position_eventDateText]));
                } else {
                    sb.append(getResources().getString(R.string.quiz_answer_false, ageRollLong, ageLong, eventInfo[Position_eventDateText]));
                }
                //Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
                result.actions.add(sb.toString());
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question) + Constants.STRING_COLON_SPACE + e, Constants.quiz_error_button_OK);
        }
    }

    synchronized List<String> getFilteredEventList(List<String> eventList, List<String> widgetPref) {

        List<String> resultList = new ArrayList<>();

        try {

            if (widgetPref == null || eventList.size() == 0) return resultList;

            List<String> eventsPrefList = new ArrayList<>();
            if (widgetPref.size() > 3 && !widgetPref.get(3).isEmpty()) {
                eventsPrefList = Arrays.asList(widgetPref.get(3).split(Constants.REGEX_PLUS));
            }

            for (String event: eventList) {

                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                boolean isVisibleEvent = false;
                boolean useEventListPrefs = true;

                final String eventType = singleEventArray[Position_eventType];
                if (eventsPrefList.size() > 0) {
                    useEventListPrefs = false;
                    isVisibleEvent = eventsPrefList.contains(eventType) &&
                            (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(getEventKey(singleEventArray)));
                }
                if (useEventListPrefs) isVisibleEvent = preferences_list_event_types.contains(eventType) &&
                        (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(getEventKey(singleEventArray)));

                if (isVisibleEvent) {
                    resultList.add(event);
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return resultList;
    }

    boolean checkNoBatteryOptimization() {

        //https://stackoverflow.com/questions/32627342/how-to-whitelist-app-in-doze-mode-android-6-0/32627788#32627788

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String packageName = context.getPackageName();
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                return pm.isIgnoringBatteryOptimizations(packageName);
            } else {
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return true;
        }
    }

    public static boolean contains(final int[] arr, final int key) {
        for (final int i : arr) {
            if (i == key) return true;
        }
        return false;
    }

    @NonNull
    //https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri
    public String getPath(Context context, Uri uri) {

        try {

            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(Constants.STRING_COLON);
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else {
                        return "/storage/"+split[0]+"/"+split[1];
                    }
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + getDataColumn(context, uri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(Constants.STRING_COLON);
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    if (contentUri != null) {
                        final String selection = "_id=?";
                        final String[] selectionArgs = new String[]{split[1]};
                        return getDataColumn(context, contentUri, selection, selectionArgs);
                    } else {
                        return getDataColumn(context, uri, null, null);
                    }
                }
            }

            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                // Return the remote address
                if (isGooglePhotosUri(uri)) return uri.getLastPathSegment();
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            } else if (preferences_debug_on) {
                ToastExpander.showText(context, uri.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return Constants.STRING_EMPTY;

    }

    private boolean isExternalStorageDocument(Uri uri) {
        return Constants.FilePrefix_ExternalStorage.equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return Constants.FilePrefix_Downloads.equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return Constants.FilePrefix_Media.equals(uri.getAuthority());
    }

    private boolean isGooglePhotosUri(Uri uri) {
        return Constants.FilePrefix_GooglePhotos.equals(uri.getAuthority());
    }


    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        try {

            try (Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    final int indexName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (indexName > -1) {
                        List<String> path = uri.getPathSegments();
                        return cursor.getString(indexName);
                    }
                    final int indexData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (indexData > -1) {
                        return cursor.getString(indexData);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return null;

    }

    private void fillEmptyUserData(TreeMap<Integer, String> userData) {

        try {

            if (userData == null) return;

            for(int i = 0; i < Position_attrAmount; i++) {
                if (!userData.containsKey(i)) {
                    //userData.put(i, (i == Position_contactID || i == Position_eventID || i == Position_photo_uri) ? STRING_EMPTY : STRING_SPACE);
                    userData.put(i, Constants.STRING_EMPTY);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    static Intent getViewActionIntent(@NonNull String[] singleEventArray, int prefAction) {

        try {

            if (singleEventArray.length < Position_attrAmount) return null;
            Uri uri = null;

            if (prefAction == 1) { //Контакт, календарь, ссылка
                if (!TextUtils.isEmpty(singleEventArray[Position_contactID])) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventID])) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventURL].trim())) {
                    String[] eventURLs = singleEventArray[Position_eventURL].trim().split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                }

            } else if (prefAction == 2) { //Календарь, контакт, ссылка

                if (!TextUtils.isEmpty(singleEventArray[Position_eventID])) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                } else if (!TextUtils.isEmpty(singleEventArray[Position_contactID])) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventURL].trim())) {
                    String[] eventURLs = singleEventArray[Position_eventURL].trim().split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                }

            } else if (prefAction == 3) { //Ссылка, контакт, календарь

                if (!TextUtils.isEmpty(singleEventArray[Position_eventURL].trim())) {
                    String[] eventURLs = singleEventArray[Position_eventURL].trim().split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                } else  if (!TextUtils.isEmpty(singleEventArray[Position_contactID])) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventID])) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                }

            } else if (prefAction == 4) { //Контакт, ссылка, календарь
                if (!TextUtils.isEmpty(singleEventArray[Position_contactID])) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventURL].trim())) {
                    String[] eventURLs = singleEventArray[Position_eventURL].trim().split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                } else if (!TextUtils.isEmpty(singleEventArray[Position_eventID])) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                }
            }

            return uri != null ? new Intent(Intent.ACTION_VIEW, uri) : null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

    }

    synchronized void initIconPack() {

        try {

            preferences_IconPackImages_F.clear();
            preferences_IconPackImages_M.clear();

            switch (preferences_IconPackNumber) {

                case 1:

                    preferences_IconPackImages_F.put(0, R.drawable.ic_pack01_f2);
                    preferences_IconPackImages_F.put(6, R.drawable.ic_pack01_f0);
                    preferences_IconPackImages_F.put(17, R.drawable.ic_pack01_f1);
                    preferences_IconPackImages_F.put(25, R.drawable.ic_pack01_f2);
                    preferences_IconPackImages_F.put(35, R.drawable.ic_pack01_f3);
                    preferences_IconPackImages_F.put(45, R.drawable.ic_pack01_f4);
                    preferences_IconPackImages_F.put(55, R.drawable.ic_pack01_f5);
                    preferences_IconPackImages_F.put(150, R.drawable.ic_pack01_f6);

                    preferences_IconPackImages_M.put(0, R.drawable.ic_pack01_m2);
                    preferences_IconPackImages_M.put(6, R.drawable.ic_pack01_m0);
                    preferences_IconPackImages_M.put(17, R.drawable.ic_pack01_m1);
                    preferences_IconPackImages_M.put(25, R.drawable.ic_pack01_m2);
                    preferences_IconPackImages_M.put(35, R.drawable.ic_pack01_m3);
                    preferences_IconPackImages_M.put(45, R.drawable.ic_pack01_m4);
                    preferences_IconPackImages_M.put(55, R.drawable.ic_pack01_m5);
                    preferences_IconPackImages_M.put(150, R.drawable.ic_pack01_m6);

                    break;

                case 2:

                    preferences_IconPackImages_F.put(0, R.drawable.ic_pack02_f2);
                    preferences_IconPackImages_F.put(6, R.drawable.ic_pack02_f0);
                    preferences_IconPackImages_F.put(17, R.drawable.ic_pack02_f1);
                    preferences_IconPackImages_F.put(25, R.drawable.ic_pack02_f2);
                    preferences_IconPackImages_F.put(35, R.drawable.ic_pack02_f3);
                    preferences_IconPackImages_F.put(45, R.drawable.ic_pack02_f4);
                    preferences_IconPackImages_F.put(55, R.drawable.ic_pack02_f5);
                    preferences_IconPackImages_F.put(150, R.drawable.ic_pack02_f6);

                    preferences_IconPackImages_M.put(0, R.drawable.ic_pack02_m2);
                    preferences_IconPackImages_M.put(6, R.drawable.ic_pack02_m0);
                    preferences_IconPackImages_M.put(17, R.drawable.ic_pack02_m1);
                    preferences_IconPackImages_M.put(25, R.drawable.ic_pack02_m2);
                    preferences_IconPackImages_M.put(35, R.drawable.ic_pack02_m3);
                    preferences_IconPackImages_M.put(45, R.drawable.ic_pack02_m4);
                    preferences_IconPackImages_M.put(55, R.drawable.ic_pack02_m5);
                    preferences_IconPackImages_M.put(150, R.drawable.ic_pack02_m6);

                    break;

                case 3:

                    preferences_IconPackImages_F.put(0, R.drawable.ic_pack03_f3);
                    preferences_IconPackImages_F.put(6, R.drawable.ic_pack03_f0);
                    preferences_IconPackImages_F.put(17, R.drawable.ic_pack03_f1);
                    preferences_IconPackImages_F.put(25, R.drawable.ic_pack03_f2);
                    preferences_IconPackImages_F.put(35, R.drawable.ic_pack03_f3);
                    preferences_IconPackImages_F.put(45, R.drawable.ic_pack03_f4);
                    preferences_IconPackImages_F.put(55, R.drawable.ic_pack03_f5);
                    preferences_IconPackImages_F.put(150, R.drawable.ic_pack03_f6);

                    preferences_IconPackImages_M.put(0, R.drawable.ic_pack03_m3);
                    preferences_IconPackImages_M.put(6, R.drawable.ic_pack03_m0);
                    preferences_IconPackImages_M.put(17, R.drawable.ic_pack03_m1);
                    preferences_IconPackImages_M.put(25, R.drawable.ic_pack03_m2);
                    preferences_IconPackImages_M.put(35, R.drawable.ic_pack03_m3);
                    preferences_IconPackImages_M.put(45, R.drawable.ic_pack03_m4);
                    preferences_IconPackImages_M.put(55, R.drawable.ic_pack03_m5);
                    preferences_IconPackImages_M.put(150, R.drawable.ic_pack03_m6);

                    break;
            }

            if (preferences_IconPackImages_F.isEmpty()) {
                preferences_IconPackImages_F.put(0, R.drawable.ic_pack00_f1);
                preferences_IconPackImages_F.put(15, R.drawable.ic_pack00_f0);
                preferences_IconPackImages_F.put(60, R.drawable.ic_pack00_f1);
                preferences_IconPackImages_F.put(150, R.drawable.ic_pack00_f2);
            }

            if (preferences_IconPackImages_M.isEmpty()) {
                preferences_IconPackImages_M.put(0, R.drawable.ic_pack00_m1);
                preferences_IconPackImages_M.put(15, R.drawable.ic_pack00_m0);
                preferences_IconPackImages_M.put(60, R.drawable.ic_pack00_m1);
                preferences_IconPackImages_M.put(150, R.drawable.ic_pack00_m2);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) ToastExpander.showText(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public static String toARGBString(int color) {
        // format: #AARRGGBB
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));
        if (alpha.length() == 1)
            alpha = "0" + alpha;
        if (red.length() == 1)
            red = "0" + red;
        if (green.length() == 1)
            green = "0" + green;
        if (blue.length() == 1)
            blue = "0" + blue;
        return "#" + alpha + red + green + blue;
    }

    static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            return null;
        }
    }

    //https://stackoverflow.com/questions/21633637/rounded-corners-android-image-buttons
    static Bitmap toRoundCorner(@NonNull Bitmap bitmap, int pixelsX, int pixelsY) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) pixelsX, (float) pixelsY, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * Get the method name for a depth in call stack.
     * @param depth depth in the call stack (0 means current method, 1 means call method, ...)
     * @return method name
     */
    public static String getMethodName(final int depth)
    {
        StackTraceElement[] ste = null;
        try {
            ste = Thread.currentThread().getStackTrace();
        } catch (SecurityException se) { /**/ }
        return depth >=0 && ste != null ? ste[depth].getClassName() + "->" + ste[depth].getMethodName() : Constants.STRING_EMPTY;
    }

    public String getCurrentParams() {

        try {

            String[] typeIDs = resources.getStringArray(R.array.pref_EventTypes_values);
            String[] typeNames = resources.getStringArray(R.array.pref_EventTypes_entries);
            StringBuilder listEventsTypes = new StringBuilder();
            for (int i = 0; i < typeIDs.length; i++) {
                if (preferences_list_event_types.contains(typeIDs[i])) {
                    if (listEventsTypes.length() > 0) listEventsTypes.append(Constants.STRING_COMMA_SPACE);
                    listEventsTypes.append(typeNames[i]);
                }
            }

            final String result = resources.getString(R.string.msg_zero_events_body,
                    (preferences_list_event_types.isEmpty() ? Constants.FONT_COLOR_RED + resources.getString(R.string.msg_none) : Constants.FONT_COLOR_GREEN + listEventsTypes) + Constants.HTML_COLOR_END,
                    resources.getString(R.string.stats_permissions_accounts, ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED
                            ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END) +
                            resources.getString(R.string.stats_permissions_contacts, ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                                    ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END),
                    (preferences_Accounts.isEmpty() ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_all) : Constants.FONT_COLOR_RED + String.join(Constants.STRING_COMMA_SPACE, preferences_Accounts)) + Constants.HTML_COLOR_END,
                    resources.getString(R.string.stats_permissions_calendar, ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                            ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END),
                    (preferences_BirthDay_calendars.isEmpty() ? Constants.STRING_MINUS : String.join(Constants.STRING_COMMA_SPACE, preferences_BirthDay_calendars)),
                    (preferences_Otherevent_calendars.isEmpty() ? Constants.STRING_MINUS : String.join(Constants.STRING_COMMA_SPACE, preferences_Otherevent_calendars)),
                    (preferences_Birthday_files.isEmpty() ? Constants.STRING_MINUS : String.join(Constants.STRING_COMMA_SPACE, preferences_Birthday_files)),
                    (preferences_Otherevent_files.isEmpty() ? Constants.STRING_MINUS : String.join(Constants.STRING_COMMA_SPACE, preferences_Otherevent_files))
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return result;
            } else {
                return result
                        .replace(Constants.HTML_UL_START + Constants.HTML_LI, "&nbsp;-&nbsp;")
                        .replace(Constants.HTML_LI, Constants.HTML_LI_API21)
                        .replace(Constants.HTML_LI_END, Constants.STRING_EMPTY)
                        .replace(Constants.HTML_UL_START, Constants.STRING_EMPTY)
                        .replace(Constants.HTML_UL_END, Constants.STRING_EMPTY);
            }

        } catch (Exception e) {
            return getMethodName(3) + Constants.STRING_COLON_SPACE + e;
        }

    }

    @NonNull String getPreferenceString(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull String defValue) {
        try {
            return preferences.getString(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    int getPreferenceInt(@NonNull SharedPreferences preferences, @NonNull String key, int defValue) {
        try {
            return preferences.getInt(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    @NonNull Set<String> getPreferenceStringSet(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull Set<String> defValue) {
        try {
            return new HashSet<>(preferences.getStringSet(key, defValue));
        } catch (Exception e) {
            return new HashSet<>(defValue);
        }
    }

}