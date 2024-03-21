/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 17.01.2024, 20:35
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
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.text.HtmlCompat;

class ContactsEvents {

    static final int Position_eventDate_sorted = 0;
    static final int Position_personFullName = 1; //ИОФ
    static final int Position_personFullNameAlt = 2; //ФИО
    static final int Position_eventCaption = 3;
    static final int Position_eventLabel = 4;
    static final int Position_nickname = 5;
    static final int Position_dates = 6; //accountType: date: eventHash
    static final int Position_eventDateThisTime = 7;
    static final int Position_eventDateFirstTime = 8;
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
    static final int Position_rawContactID = 24;
    static final int Position_eventStorage = 25;
    static final int Position_eventSource = 26;
    static final int Position_zodiacSign = 27;
    static final int Position_zodiacYear = 28;
    static final int Position_eventURL = 29;
    static final int Position_eventDescription = 30;
    static final int Position_attrAmount = 31; //MAX

    private static final HashMap<Integer, String> eventTypesIDs = new HashMap<Integer, String>() {{
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
        put(Constants.Type_Xdays, "22");
        put(Constants.Type_HolidayEvent, "4");
        put(Constants.Type_Unrecognized, "99");
    }};

    @NonNull
    static String getEventType(int typeId) {
        return checkForNull(eventTypesIDs.get(typeId));
    }

    private static final String TAG = "ContactsEvents";
    private static final ContactsEvents ourInstance = new ContactsEvents();

    //Константы
    final Set<String> prefs_EventTypes_Default = new HashSet<String>() {{
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM));
    }};
    final List<String> eventList = new ArrayList<>(); //Список всех событий
    final String systemLocale = Locale.getDefault().getLanguage();
    //final HashSet<String> idsWithDeathEvent = new HashSet<>(); //ID контактов с годовщиной смерти
    final HashMap<String, Date> deathDatesForIds = new HashMap<>(); //Даты годовщин смерти по ID
    final HashMap<String, Date> birthdayDatesForIds = new HashMap<>(); //Даты дней рождений по ID
    //final HashSet<String> idsAllContacts = new HashSet<>(); //ID всех контактов в адресной книге
    final HashSet<String> idsAllCalendarEvents = new HashSet<>(); //ID всех найденных событий календаря
    final HashMap<String, String> map_contacts_names = new HashMap<>(); //связка имён контактов с ID
    final HashMap<String, String> map_calendars = new HashMap<>(); //список всех календарей
    final HashMap<String, Integer> map_eventsBySubtypeAndPersonID_offset = new HashMap<>(); //Индекс события до сортировки (или для eventListUnsorted)
    final HashMap<String, String> map_organizations = new HashMap<>();
    final HashMap<String, String> map_contacts_titles = new HashMap<>();
    final HashMap<String, String> map_contacts_rawIds = new HashMap<>(); //ID всех контактов в адресной книге: rawId -> contactId
    final HashMap<String, String> map_contacts_ids = new HashMap<>(); //ID всех контактов в адресной книге: contactId -> rawId
    final HashMap<String, String> map_contacts_aliases = new HashMap<>();
    final HashMap<String, String> map_events_weblinks = new HashMap<>();
    final HashMap<String, String> map_notes = new HashMap<>();
    final Random generator = new Random();
    //https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
    final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
    final Map<Integer, Integer> preferences_IconPackImages_M = new TreeMap<>();
    final Map<Integer, Integer> preferences_IconPackImages_F = new TreeMap<>();
    private final Map<String, DayType.Type> preferences_DaysTypes = new HashMap<>(); //Типы дней для календаря
    private final Map<String, String> preferences_DaysInfo = new HashMap<>(); //Данные о событиях для календаря

    //Даты
    //todo: подумать про массивы https://tproger.ru/translations/java-tips-and-tricks-for-begginer/
    //final Locale locale_en = new Locale(Constants.LANG_EN); //Все даты Android хранит в этой локали, типа 11 Jan 1991
    final Locale locale_ru = new Locale(Constants.LANG_RU); //Skype хранит даты в той локали, которая указана в приложении Skype
    //final Locale locale_us = new Locale(Constants.LANG_US); // Jan 11, 1991
    final Locale locale_ukr = new Locale(Constants.LANG_UA);
    final SimpleDateFormat sdf_java = new SimpleDateFormat(Constants.DATE_JAVA, Locale.US);
    final SimpleDateFormat sdf_java_G = new SimpleDateFormat(Constants.DATE_JAVA_G, Locale.US);
    final SimpleDateFormat sdf_java_no_year = new SimpleDateFormat(Constants.DATE_JAVA_NO_YEAR, Locale.US);
    final SimpleDateFormat sdf_skype = new SimpleDateFormat(Constants.DATE_DD_MMM_YYYY, Locale.US);
    final SimpleDateFormat sdf_DDMMY = new SimpleDateFormat(Constants.DATE_DD_MM_Y, Locale.US);
    final SimpleDateFormat sdf_DDMMYYYY = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, Locale.US);
    final SimpleDateFormat sdf_DDMMYYYY_G = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY_G, Locale.US);
    @SuppressLint("SimpleDateFormat")
    final SimpleDateFormat sdf_DDMMYYYYHHMM = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM);
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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HashMap<String, String> map_contacts_data = new HashMap<>(); //кеш данных о контактах
    private final HashMap<String, String> preferences_mergedIDs = new HashMap<>(); //жёсткая привязка события к определённому контакту по ContactID
    private final HashMap<String, String> preferences_mergedRawIDs = new HashMap<>(); //жёсткая привязка события к определённому контакту RawContactID
    private final HashMap<String, String> preferences_xDaysEvents = new HashMap<>();
    List<String> eventListUnsorted = new ArrayList<>(); //Несортированный список
    int currentTheme = 0;
    boolean needUpdateEventList = true;

    //Настройки
    boolean preferences_debug_on;
    boolean preferences_info_on;
    boolean preferences_extrafun;
    String preferences_language;
    String preferences_icon;
    int preferences_list_events_scope;
    boolean preferences_menustyle_compact;
    Set<String> preferences_list_event_types;
    Set<String> preferences_list_event_info;
    String preferences_list_prev_events;
    String preferences_list_custom_caption;
    String preferences_list_custom_todayevent_caption;
    int preferences_list_style;
    int preferences_list_photostyle;
    int preferences_list_filling;
    int preferences_list_marging;
    int preferences_list_nameformat;
    int preferences_list_dateformat;
    Set<String> preferences_list_age_format;
    @ColorInt int preferences_list_color_eventtoday;
    @ColorInt int preferences_list_color_eventsoon;
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
    int preferences_widgets_days_event_soon;
    @ColorInt int preferences_widgets_color_default;
    @ColorInt int preferences_widgets_color_widget_caption;
    @ColorInt int preferences_widgets_color_event_today;
    @ColorInt int preferences_widgets_color_event_soon;
    @ColorInt int preferences_widgets_color_event_far;
    int preferences_widgets_on_click_action;
    String preferences_first_names_female_custom;
    String preferences_first_names_male_custom;
    Matcher preferences_last_name_completions_male;
    Matcher preferences_last_name_completions_female;
    Matcher preferences_first_names_male;
    Matcher preferences_first_names_female;
    Matcher preferences_second_name_completions_male;
    Matcher preferences_second_name_completions_female;
    @Nullable
    Matcher preferences_death_labels;
    String preferences_birthday_calendars_rules;
    int preferences_rules_calendars_nameformat;
    int preferences_rules_files_nameformat;
    int preferences_rules_unrecognized;
    static final int Rules_Unrecognized_Type_Other = 1;
    static final int Rules_Unrecognized_Type_Unrecognized = 2;
    static final int Rules_Unrecognized_Skip = 3;
    String preferences_customevent1_caption;
    String preferences_customevent2_caption;
    String preferences_customevent3_caption;
    String preferences_customevent4_caption;
    String preferences_customevent5_caption;
    int preferences_notification_channel_id;
    Set<String> preferences_notifications_days;
    int preferences_notifications_alarm_hour;
    int preferences_notifications_alarm_minute;
    String preferences_notifications_ringtone;
    int preferences_notifications_on_click_action;
    String preferences_quiz_interface;
    MyTheme preferences_theme;
    int preferences_list_jubilee_algorithm;
    private int preferences_list_sad_photo;
    @Nullable
    private Matcher preferences_birthday_labels;
    @Nullable
    private Matcher preferences_wedding_labels;
    @Nullable
    private Matcher preferences_nameday_labels;
    @Nullable
    private Matcher preferences_crowning_labels;
    @Nullable
    private Matcher preferences_other_event_labels;
    @Nullable
    private Matcher preferences_holiday_event_labels;
    private boolean preferences_birthday_calendars_useyear;
    private boolean preferences_customevent1_enabled;
    private Matcher preferences_customevent1_labels;
    private boolean preferences_customevent1_useyear;
    private boolean preferences_customevent2_enabled;
    private Matcher preferences_customevent2_labels;
    private boolean preferences_customevent2_useyear;
    private boolean preferences_customevent3_enabled;
    private Matcher preferences_customevent3_labels;
    private boolean preferences_customevent3_useyear;
    private boolean preferences_customevent4_enabled;
    private Matcher preferences_customevent4_labels;
    private boolean preferences_customevent4_useyear;
    private boolean preferences_customevent5_enabled;
    private Matcher preferences_customevent5_labels;
    private boolean preferences_customevent5_useyear;
    /* preferences_notifications_type:
     *   0 - Одно общее уведомление
     *   1 - Каждое событие в отдельном уведомлении
     *   2 - Если событий меньше 3 => отдельные, иначе - общее
     *   3 - Если событий меньше 4 => отдельные, иначе - общее
     * */
    private int preferences_notifications_type;
    private int preferences_notifications_priority;
    private Set<String> preferences_notifications_event_types;
    private Set<String> preferences_notifications_quick_actions;
    private Set<String> preferences_hiddenEvents = new HashSet<>();
    private Set<String> preferences_hiddenEventsRawIds = new HashSet<>();
    private Set<String> preferences_silentEvents = new HashSet<>();
    private Set<String> preferences_silentEventsRawIds = new HashSet<>();
    private Set<String> preferences_favoriteEvents = new HashSet<>();
    private Set<String> preferences_favoriteEventsRawIds = new HashSet<>();
    private Set<String> preferences_Accounts = new HashSet<>();
    Set<String> preferences_BirthDay_calendars = new HashSet<>();
    Set<String> preferences_OtherEvent_calendars = new HashSet<>();
    Set<String> preferences_HolidayEvent_calendars = new HashSet<>();
    Set<String> preferences_MultiType_calendars = new HashSet<>();
    Set<String> preferences_Birthday_files = new HashSet<>();
    Set<String> preferences_OtherEvent_files = new HashSet<>();
    Set<String> preferences_HolidayEvent_files = new HashSet<>();
    Set<String> preferences_MultiType_files = new HashSet<>();
    Set<String> preferences_HolidayEvent_ids = new HashSet<>();
    private Set<String> pref_List_Event_Info_Default;
    private Set<String> pref_List_Age_Format_Default;
    private Set<String> pref_Widgets_EventInfo_Info_Default;
    private int preferences_IconPackNumber;
    final List<Integer> preferences_RecentColors = new ArrayList<>();

    //Статистика
    long statTimeGetContactEvents = 0;
    long statTimeGetCalendarEvents = 0;
    long statTimeGetFileEvents = 0;
    long statTimeGetHolidayEvents = 0;
    long statTimeComputeDates = 0;
    long statTimeUpdateWidgets = 0;
    long statLastComputeDates = 0;
    long statLastSearchSuggestion = 0;
    int statContactsEventCount = 0;
    int statCalendarsEventCount = 0;
    int statFilesEventCount = 0;
    int statContactsTitleCount = 0;
    int statContactsOrganizationCount = 0;
    int statContactsNicknameCount = 0;
    int statContactsCount = 0;
    int statContactsURLCount = 0;
    int statEventsCount = 0;
    long statLastPausedForOtherActivity = 0;
    int statEventsPrevEventsFound = 0;
    int statFavoriteEventsCount = 0;
    final HashMap<String, Integer> statEventTypes = new HashMap<>();

    private static DisplayMetrics displayMetrics;
    float displayMetrics_density;
    boolean isUIOpen = false;
    float dimen_List_details;
    float dimen_List_name;
    float dimen_list_date;
    String currentLocale = Constants.STRING_EMPTY;

    //UI объекты
    private Context context;
    private Resources resources;
    private ContentResolver contentResolver;
    @Nullable
    protected CoordinatorLayout coordinator;

    //Зависимые от языка константы

    String[] weekDaysShort;
    String eventNameNY;
    String eventNameEaster;
    String eventNameCatholicEaster;

    private Thread widgetsUpdateThread;

    private ContactsEvents() {
    }

    @NonNull
    static ContactsEvents getInstance() {
        return ourInstance;
    }

    private static int countLeapYearsBetween(int y1, int y2) {

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

    static Calendar getCalendarFromDate(@NonNull Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c;
    }

    static Calendar removeTime(@NonNull Calendar c) {

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c;
    }

    @NonNull
    static String checkForNull(String strIn) {
        return strIn == null || strIn.isEmpty() ? Constants.STRING_EMPTY : strIn;
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

    static boolean contains(final int[] arr, final int key) {
        for (final int i : arr) {
            if (i == key) return true;
        }
        return false;
    }

    static Intent getViewActionIntent(@NonNull String[] singleEventArray, int prefAction) {

        try {

            if (singleEventArray.length < Position_attrAmount) return null;
            if (Constants.STRING_STORAGE_HOLIDAYS.equals(singleEventArray[Position_eventStorage])) return null;

            Uri uri = null;
            final String contactID = singleEventArray[ContactsEvents.Position_contactID];
            final boolean isRealContactID = contactID != null && !contactID.isEmpty() && isRealContactEventID(contactID);
            final String eventId = singleEventArray[Position_eventID];
            final boolean notEmptyEventId = !TextUtils.isEmpty(eventId);
            final String eventUrl = singleEventArray[Position_eventURL].trim();
            final boolean notEmptyEventUrl = !TextUtils.isEmpty(eventUrl);
            final boolean isFileOrHoliday = notEmptyEventId && (eventId.startsWith(Constants.PREFIX_FileEventID) || eventId.startsWith(Constants.PREFIX_HolidayEventID));

            if (prefAction == 1) { //Контакт, календарь, ссылка
                if (isRealContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactID);
                } else if (notEmptyEventId && !isFileOrHoliday) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId);
                } else if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                }

            } else if (prefAction == 2) { //Календарь, контакт, ссылка

                if (notEmptyEventId && !isFileOrHoliday) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId);
                } else if (isRealContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                }

            } else if (prefAction == 3) { //Ссылка, контакт, календарь

                if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                } else if (isRealContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (notEmptyEventId && !isFileOrHoliday) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId);
                }

            } else if (prefAction == 4) { //Контакт, ссылка, календарь
                if (isRealContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                } else if (notEmptyEventId && !isFileOrHoliday) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId);
                }
            }

            return uri != null ? new Intent(Intent.ACTION_VIEW, uri) : null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

    }

    static String toARGBString(int color) {
        // format: #AARRGGBB
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));
        if (alpha.length() == 1) alpha = "0" + alpha;
        if (red.length() == 1) red = "0" + red;
        if (green.length() == 1) green = "0" + green;
        if (blue.length() == 1) blue = "0" + blue;
        return Constants.STRING_HASH + alpha + red + green + blue;
    }

    static Bitmap getBitmap(Drawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    static Bitmap getBitmap(Context context, int drawableId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableId);
            if (drawable == null) return null;
            if (drawable instanceof BitmapDrawable) {
                return BitmapFactory.decodeResource(context.getResources(), drawableId);
            } else { //if (drawable instanceof VectorDrawable || drawable instanceof AdaptiveIconDrawable) {
                return getBitmap(drawable);
                //} else {
                //    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
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
     *
     * @param depth depth in the call stack (0 means current method, 1 means call method, ...)
     * @return method name
     */
    static String getMethodName(final int depth) {
        StackTraceElement[] ste = null;
        try {
            ste = Thread.currentThread().getStackTrace();
        } catch (SecurityException se) { /**/ }
        return depth >= 0 && ste != null ? ste[depth].getClassName() + "->" + ste[depth].getMethodName() : Constants.STRING_EMPTY;
    }

    static boolean isXiaomi() {
        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
    }

    static boolean isSamsung() {
        return Build.MANUFACTURER.equalsIgnoreCase("samsung");
    }

    static boolean isWidgetSupportConfig() {
        return isSamsung() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    Context getContext() {
        return context;
    }

    void setContext(@NonNull Context con) {
        context = con;
        resources = con.getResources();
        contentResolver = context.getContentResolver();
        setDisplayMetrics(this.getResources().getDisplayMetrics());
        displayMetrics_density = displayMetrics.density;

        pref_Widgets_EventInfo_Info_Default = new HashSet<>();
        pref_Widgets_EventInfo_Info_Default.add(context.getString(R.string.pref_Widgets_EventInfo_Photo_ID));
        pref_Widgets_EventInfo_Info_Default.add(context.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID));
        pref_Widgets_EventInfo_Info_Default.add(context.getString(R.string.pref_Widgets_EventInfo_FavIcon_ID));
        pref_Widgets_EventInfo_Info_Default.add(context.getString(R.string.pref_Widgets_EventInfo_Border_ID));

        pref_List_Event_Info_Default = new HashSet<>();
        pref_List_Event_Info_Default.add(context.getString(R.string.pref_List_EventInfo_Photo));
        pref_List_Event_Info_Default.add(context.getString(R.string.pref_List_EventInfo_JobTitle));
        pref_List_Event_Info_Default.add(context.getString(R.string.pref_List_EventInfo_Organization));
        pref_List_Event_Info_Default.add(context.getString(R.string.pref_List_EventInfo_EventCaption));
        pref_List_Event_Info_Default.add(context.getString(R.string.pref_List_EventInfo_EventIcon));
        pref_List_Event_Info_Default.add(context.getString(R.string.pref_List_EventInfo_FavoritesIcon));

        pref_List_Age_Format_Default = new HashSet<>();
        pref_List_Age_Format_Default.add(context.getString(R.string.pref_List_AgeFormat_AddPostfix));
        pref_List_Age_Format_Default.add(context.getString(R.string.pref_List_AgeFormat_Convert000toK));
        pref_List_Age_Format_Default.add(context.getString(R.string.pref_List_AgeFormat_SeparateThousands));

    }

    @NonNull
    Resources getResources() {
        if (this.resources == null) this.resources = context.getResources();
        return this.resources;
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

            int resD = d2 + ((y2 - minorYearSign * y1) * 365) - d1;
            if (isNegative) {
                return -(resD + countLeapYearsBetween(minorYearSign * y1, y2));
            } else {
                return resD + countLeapYearsBetween(minorYearSign * y1, y2);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    /**
     * @param dateFrom   date from
     * @param dateTo     date to
     * @param components 1 - only DMY, 2 - only days count, 3 - "DMY (days count)"
     * @return distance between two days in locale text format
     */
    String countDaysDiffText(@NonNull Date dateFrom, @NonNull Date dateTo, int components) {

        try {

            StringBuilder eventDistance = new StringBuilder();
            long daysDiff;
            boolean diffOnlyDays = true;

            //если включить desugaring https://www.youtube.com/watch?v=heCvGfOGH0s то размер приложения +200К
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                //https://stackoverflow.com/questions/4710206/calculate-age-in-years-months-days-hours-minutes-and-seconds
                LocalDate dateStart = dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate dateEnd = dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                if (dateEnd.isBefore(dateStart)) return Constants.STRING_EMPTY;

                daysDiff = ChronoUnit.DAYS.between(dateStart, dateEnd);

                if (components == 1 || components == 3) {

                    Period p = Period.between(dateStart, dateEnd);

                    if (p.getYears() > 0) {
                        eventDistance.append(getAgeString(p.getYears(), R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    if (p.getMonths() > 0) {
                        eventDistance.append(getAgeString(p.getMonths(), R.string.msg_after_month_prefix_1, R.string.msg_after_month_prefix_1_, R.string.msg_after_month_prefix_2_3_4, R.string.msg_after_month_prefix_5_20)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    if (p.getDays() > 0) {
                        eventDistance.append(getAgeString(p.getDays(), R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20)).append(Constants.STRING_SPACE);
                    }
                }

            } else {

                Calendar calendarDateFrom;
                Calendar calendarDateTo;

                if (dateTo.before(dateFrom)) return Constants.STRING_EMPTY;

                calendarDateFrom = removeTime(getCalendarFromDate(dateFrom));
                calendarDateTo = removeTime(getCalendarFromDate(dateTo));

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
                        eventDistance.append(getAgeString(delta, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20)).append(Constants.STRING_SPACE);
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
                        eventDistance.append(getAgeString(delta, R.string.msg_after_month_prefix_1, R.string.msg_after_month_prefix_1_, R.string.msg_after_month_prefix_2_3_4, R.string.msg_after_month_prefix_5_20)).append(Constants.STRING_SPACE);
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
                        eventDistance.append(getAgeString(delta, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20)).append(Constants.STRING_SPACE);
                    }
                }
            }

            //(X days)
            if (!diffOnlyDays || components == 2) {
                if (components == 3) {
                    eventDistance.append(Constants.STRING_PARENTHESIS_START);
                }
                if (components == 2 || components == 3) {
                    eventDistance.append(getAgeString(daysDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20));
                }
                if (components == 3) {
                    eventDistance.append(Constants.STRING_PARENTHESIS_CLOSE);
                }
            }

            return eventDistance.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return date;
        }
    }

    @NonNull
    String getAgeString(long age, int id_prefix_1, int id_prefix_1_, int id_prefix_2_3_4, int id_prefix_5_20) {

        try {

            StringBuilder result = new StringBuilder();
            String count_str = Long.toString(age);
            String count_end = count_str.substring(count_str.length() - 1);
            boolean isEnd234 = count_end.equals(Constants.STRING_2) || count_end.equals(Constants.STRING_3) || count_end.equals(Constants.STRING_4);
            long ageMinus100 = age % 100;

            result.append(age);

            if (!getResources().getString(R.string.pref_Language_fr).equals(currentLocale)) {
                if (ageMinus100 == 1) { //Единственное число
                    result.append(getResources().getString(id_prefix_1));
                } else if (ageMinus100 > 4 && ageMinus100 < 21) { //Больше 4, но меньше 21
                    result.append(getResources().getString(id_prefix_5_20));
                } else if (count_end.equals(Constants.STRING_1)) { //Если заканчивается на 1, но не между 5-20
                    result.append(getResources().getString(id_prefix_1_));
                } else if (isEnd234) { //Если заканчивается на 2, 3, 4
                    result.append(getResources().getString(id_prefix_2_3_4));
                } else {
                    result.append(getResources().getString(id_prefix_5_20));
                }
            } else { //Французский
                if (ageMinus100 == 1) { //Единственное число
                    result.append(getResources().getString(id_prefix_1));
                } else if ((ageMinus100 >= 3 && ageMinus100 <= 5) || (ageMinus100 >= 8 && ageMinus100 <= 10)) { //3-5,8-10
                    result.append(getResources().getString(id_prefix_1_));
                } else {
                    result.append(getResources().getString(id_prefix_5_20));
                }
            }
            return result.toString();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    /**
     * Инициализация и считывание настроек из SharedPreferences
     */
    void getPreferences() {

        if (context == null) return;

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            //https://medium.com/@anupamchugh/a-nightmare-with-shared-preferences-and-stringset-c53f39f1ef52
            //https://stackoverflow.com/questions/19949182/android-sharedpreferences-string-set-some-items-are-removed-after-app-restart

            preferences_debug_on = getPreferenceBoolean(preferences, context.getString(R.string.pref_Help_Debug_On_key), resources.getBoolean(R.bool.pref_Help_Debug_On_default));
            preferences_info_on = getPreferenceBoolean(preferences, context.getString(R.string.pref_Help_InfoMsg_On_key), resources.getBoolean(R.bool.pref_Help_InfoMsg_On_default));
            preferences_extrafun = getPreferenceBoolean(preferences, context.getString(R.string.pref_Help_ExtraFun_On_key), resources.getBoolean(R.bool.pref_Help_ExtraFun_On_default));
            preferences_language = getPreferenceString(preferences, context.getString(R.string.pref_Language_key), context.getString(R.string.pref_Language_default));
            preferences_icon = getPreferenceString(preferences, context.getString(R.string.pref_Icon_key), context.getString(R.string.pref_Icon_default));
            preferences_IconPackNumber = getPreferenceInt(preferences, context.getString(R.string.pref_IconPack_key), 0);
            initIconPack();
            preferences_menustyle_compact = getPreferenceBoolean(preferences, context.getString(R.string.pref_MenuStyle_key), resources.getBoolean(R.bool.pref_MenuStyle_default));

            preferences_list_event_types = getPreferenceStringSet(preferences, context.getString(R.string.pref_List_Events_key), prefs_EventTypes_Default);
            preferences_list_event_info = getPreferenceStringSet(preferences, context.getString(R.string.pref_List_EventInfo_key), pref_List_Event_Info_Default);
            preferences_list_prev_events = getPreferenceString(preferences, context.getString(R.string.pref_List_PrevEvents_key), context.getString(R.string.pref_List_PrevEvents_default));
            preferences_list_style = getPreferenceInt(preferences, context.getString(R.string.pref_List_Style_key), context.getString(R.string.pref_List_Style_default));
            preferences_list_photostyle = getPreferenceInt(preferences, context.getString(R.string.pref_List_PhotoStyle_key), context.getString(R.string.pref_List_PhotoStyle_default));
            preferences_list_filling = getPreferenceInt(preferences, context.getString(R.string.pref_List_Filling_key), context.getString(R.string.pref_List_Filling_default));
            preferences_list_jubilee_algorithm = getPreferenceInt(preferences, context.getString(R.string.pref_List_Jubilee_Algorithm_key), context.getString(R.string.pref_List_Jubilee_Algorithm_default));
            preferences_list_marging = getPreferenceInt(preferences, context.getString(R.string.pref_List_Margin_key), context.getString(R.string.pref_List_Margin_default));
            preferences_list_sad_photo = getPreferenceInt(preferences, context.getString(R.string.pref_List_SadPhoto_key), context.getString(R.string.pref_List_SadPhoto_default));
            preferences_list_nameformat = getPreferenceInt(preferences, context.getString(R.string.pref_List_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default));
            preferences_list_dateformat = getPreferenceInt(preferences, context.getString(R.string.pref_List_DateFormat_key), context.getString(R.string.pref_List_DateFormat_default));
            preferences_list_age_format = getPreferenceStringSet(preferences, context.getString(R.string.pref_List_AgeFormat_key), pref_List_Age_Format_Default);
            preferences_list_custom_caption = getPreferenceString(preferences, context.getString(R.string.pref_List_CustomCaption_key), Constants.STRING_EMPTY);
            preferences_list_custom_todayevent_caption = getPreferenceString(preferences, context.getString(R.string.pref_List_CustomTodayEventCaption_key), Constants.STRING_EMPTY);
            preferences_list_color_eventtoday = getPreferenceInt(preferences, getResources().getString(R.string.pref_List_Color_EventToday_key), getResources().getColor(R.color.pref_List_Color_EventToday_default));
            preferences_list_color_eventsoon = getPreferenceInt(preferences, getResources().getString(R.string.pref_List_Color_EventSoon_key), getResources().getColor(R.color.pref_List_Color_EventSoon_default));
            preferences_list_color_eventjubilee = getPreferenceInt(preferences, getResources().getString(R.string.pref_List_Color_EventJubilee_key), getResources().getColor(R.color.pref_List_Color_EventJubilee_default));
            preferences_list_on_click_action = getPreferenceInt(preferences, context.getString(R.string.pref_List_OnClick_key), context.getString(R.string.pref_List_OnClick_default));
            preferences_list_magnify_distance = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Distance_key), 0);
            preferences_list_magnify_name = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Name_key), 0);
            preferences_list_magnify_details = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Details_key), 0);
            preferences_list_magnify_date = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Date_key), 0);
            preferences_list_magnify_age = getPreferenceInt(preferences, context.getString(R.string.pref_List_FontMagnify_Age_key), 0);
            preference_list_fastscroll = getPreferenceBoolean(preferences, context.getString(R.string.pref_List_FastScroll_key), resources.getBoolean(R.bool.pref_List_FastScroll_default));

            preferences_widgets_event_info = getPreferenceStringSet(preferences, context.getString(R.string.pref_Widgets_EventInfo_key), pref_Widgets_EventInfo_Info_Default);
            preferences_widgets_bottom_info = getPreferenceString(preferences, context.getString(R.string.pref_Widgets_BottomInfo_key), context.getString(R.string.pref_Widgets_BottomInfo_default));
            preferences_widgets_bottom_info_2nd = getPreferenceString(preferences, context.getString(R.string.pref_Widgets_BottomInfo2nd_key), context.getString(R.string.pref_Widgets_BottomInfo2nd_default));
            preferences_widgets_days_event_soon = getPreferenceInt(preferences, context.getString(R.string.pref_Widgets_Days_EventSoon_key), context.getString(R.string.pref_Widgets_Days_EventSoon_default));
            preferences_widgets_update_period = getPreferenceInt(preferences, context.getString(R.string.pref_Widgets_UpdateInterval_key), context.getString(R.string.pref_Widgets_UpdateInterval_default));
            preferences_widgets_on_click_action = getPreferenceInt(preferences, context.getString(R.string.pref_Widgets_OnClick_key), context.getString(R.string.pref_Widgets_OnClick_default));
            preferences_widgets_color_event_today = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventToday_key), getResources().getColor(R.color.pref_Widgets_Color_EventToday_default));
            preferences_widgets_color_event_soon = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventSoon_key), getResources().getColor(R.color.pref_Widgets_Color_EventSoon_default));
            preferences_widgets_color_event_far = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventFar_key), getResources().getColor(R.color.pref_Widgets_Color_EventFar_default));
            preferences_widgets_color_default = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_EventCaption_key), getResources().getColor(R.color.pref_Widgets_Color_EventCaption_default));
            preferences_widgets_color_widget_caption = getPreferenceInt(preferences, getResources().getString(R.string.pref_Widgets_Color_WidgetCaption_key), getResources().getColor(R.color.pref_Widgets_Color_WidgetCaption_default));
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
            final String div_inter = "|"; //"\\Z|";
            //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

            //День рождения
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && TextUtils.isEmpty(customLabels)) {
                preferences_birthday_labels = null;
            } else {
                if (customLabels.isEmpty())
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                else if (!useInternal) {
                    preferences_birthday_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            preferences_birthday_calendars_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_default)));
            preferences_Birthday_files = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key), new HashSet<>());

            //Свадьба
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Anniversary_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_wedding_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_wedding_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Именины
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_NameDay_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_nameday_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_nameday_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Венчание
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Crowning_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_crowning_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_crowning_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Годовщина смерти
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Death_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_default)));
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Death_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (!useInternal && customLabels.isEmpty()) {
                preferences_death_labels = null;
            } else {
                if (customLabels.isEmpty()) {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else if (!useInternal) {
                    preferences_death_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                } else {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).concat(Constants.STRING_COMMA).concat(customLabels).replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                }
            }

            //Другие события
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Other_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (customLabels.isEmpty()) {
                preferences_other_event_labels = null;
            } else {
                preferences_other_event_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
            }
            preferences_OtherEvent_files = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Other_LocalFiles_key), new HashSet<>());

            //Праздники
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Holiday_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (customLabels.isEmpty()) {
                preferences_holiday_event_labels = null;
            } else {
                preferences_holiday_event_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
            }
            preferences_HolidayEvent_files = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Holiday_LocalFiles_key), new HashSet<>());

            preferences_HolidayEvent_ids = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Holiday_Public_Ids_key), new HashSet<>());

            //Файлы с разными типами событий
            preferences_MultiType_files = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_MultiType_LocalFiles_key), new HashSet<>());

            //Пользовательские события
            //1
            preferences_customevent1_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom1_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent1_enabled = false;

            if (!preferences_customevent1_caption.isEmpty()) {
                String preferences_customevent1_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom1_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent1_labels_str.isEmpty()) {
                    try {
                        preferences_customevent1_labels = Pattern.compile(preferences_customevent1_labels_str.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent1_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent1_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom1_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //2
            preferences_customevent2_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom2_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent2_enabled = false;

            if (!preferences_customevent2_caption.isEmpty()) {
                String preferences_customevent2_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom2_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent2_labels_str.isEmpty()) {
                    try {
                        preferences_customevent2_labels = Pattern.compile(preferences_customevent2_labels_str.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent2_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent2_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom2_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //3
            preferences_customevent3_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom3_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent3_enabled = false;

            if (!preferences_customevent3_caption.isEmpty()) {
                String preferences_customevent3_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom3_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent3_labels_str.isEmpty()) {
                    try {
                        preferences_customevent3_labels = Pattern.compile(preferences_customevent3_labels_str.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent3_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent3_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom3_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //4
            preferences_customevent4_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom4_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent4_enabled = false;

            if (!preferences_customevent4_caption.isEmpty()) {
                String preferences_customevent4_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom4_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent4_labels_str.isEmpty()) {
                    try {
                        preferences_customevent4_labels = Pattern.compile(preferences_customevent4_labels_str.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent4_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent4_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom4_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //5
            preferences_customevent5_caption = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom5_Caption_key), Constants.STRING_EMPTY).trim();
            preferences_customevent5_enabled = false;

            if (!preferences_customevent5_caption.isEmpty()) {
                String preferences_customevent5_labels_str = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Custom5_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
                if (!preferences_customevent5_labels_str.isEmpty()) {
                    try {
                        preferences_customevent5_labels = Pattern.compile(preferences_customevent5_labels_str.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                        preferences_customevent5_enabled = true;
                    } catch (Exception e) { /**/ }
                }
            }
            preferences_customevent5_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom5_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //Уведомления
            preferences_notifications_days = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications_Days_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_Days_values_default))));
            preferences_notifications_days.removeAll(new HashSet<String>() {{
                add(Constants.STRING_EMPTY);
            }});

            preferences_notifications_type = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_Type_key), context.getString(R.string.pref_Notifications_Type_default));
            preferences_notifications_priority = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_Priority_key), context.getString(R.string.pref_Notifications_Priority_default));
            preferences_notifications_alarm_hour = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_AlarmHour_key), context.getString(R.string.pref_Notifications_AlarmHour_default));
            if (preferences_notifications_alarm_hour < 0)
                preferences_notifications_alarm_hour = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmHour_default));
            preferences_notifications_alarm_minute = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_AlarmMinute_key), context.getString(R.string.pref_Notifications_AlarmMinute_default));
            if (preferences_notifications_alarm_minute < 0)
                preferences_notifications_alarm_minute = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmMinute_default));
            preferences_notifications_ringtone = getPreferenceString(preferences, context.getString(R.string.pref_Notifications_Ringtone_key), Settings.System.DEFAULT_NOTIFICATION_URI.toString());
            preferences_notifications_event_types = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications_Events_key), preferences_list_event_types //По-умолчанию берём из списка событий
            );
            preferences_notifications_quick_actions = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications_QuickActions_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_QuickActions_values_default))));
            preferences_notifications_on_click_action = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_OnClick_key), context.getString(R.string.pref_Notifications_OnClick_default));

            //Имена
            preferences_first_names_female_custom = getPreferenceString(preferences, context.getString(R.string.pref_Female_Names_key), Constants.STRING_EMPTY);
            preferences_first_names_male_custom = getPreferenceString(preferences, context.getString(R.string.pref_Male_Names_key), Constants.STRING_EMPTY);
            preferences_last_name_completions_male = Pattern.compile(context.getString(R.string.last_name_completions_man).replace(Constants.STRING_COMMA, Constants.REGEX_INTER) + Constants.REGEX_LAST).matcher(Constants.STRING_EMPTY);
            preferences_last_name_completions_female = Pattern.compile(context.getString(R.string.last_name_completions_female).replace(Constants.STRING_COMMA, Constants.REGEX_INTER) + Constants.REGEX_LAST).matcher(Constants.STRING_EMPTY);

            final String namesMale = preferences_first_names_male_custom.isEmpty() ?
                    context.getString(R.string.first_names_male) :
                    context.getString(R.string.first_names_male).concat(Constants.STRING_COMMA).concat(preferences_first_names_male_custom.toLowerCase().replace(Constants.STRING_COMMA_SPACE, Constants.STRING_COMMA));
            preferences_first_names_male = Pattern.compile(namesMale.replace(Constants.STRING_COMMA, Constants.REGEX_INTER) + Constants.REGEX_LAST).matcher(Constants.STRING_EMPTY);

            final String namesFemale = preferences_first_names_female_custom.isEmpty() ?
                    context.getString(R.string.first_names_female) :
                    context.getString(R.string.first_names_female).concat(Constants.STRING_COMMA).concat(preferences_first_names_female_custom.toLowerCase().replace(Constants.STRING_COMMA_SPACE, Constants.STRING_COMMA));
            preferences_first_names_female = Pattern.compile(namesFemale.replace(Constants.STRING_COMMA, Constants.REGEX_INTER) + Constants.REGEX_LAST).matcher(Constants.STRING_EMPTY);

            preferences_second_name_completions_male = Pattern.compile(context.getString(R.string.second_name_completions_man).replace(Constants.STRING_COMMA, Constants.REGEX_INTER) + Constants.REGEX_LAST).matcher(Constants.STRING_EMPTY);
            preferences_second_name_completions_female = Pattern.compile(context.getString(R.string.second_name_completions_female).replace(Constants.STRING_COMMA, Constants.REGEX_INTER) + Constants.REGEX_LAST).matcher(Constants.STRING_EMPTY);

            //Запоминаем информацию о темах
            preferences_theme = new MyTheme();
            try {
                preferences_theme.prefNumber = getPreferenceInt(preferences, context.getString(R.string.pref_Theme_key), context.getString(R.string.pref_Theme_default));
            } catch (ClassCastException e) {
                preferences_theme.prefNumber = Integer.parseInt(context.getString(R.string.pref_Theme_default));
            }
            switch (preferences_theme.prefNumber) {
                case 2:
                    preferences_theme.themeMain = R.style.AppTheme_DarkGray_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_DarkGray_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_DarkGray;
                    preferences_theme.themeEditText = R.style.EditText_DarkGrey;
                    break;
                case 3:
                    preferences_theme.themeMain = R.style.AppTheme_Black_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Black_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Black;
                    preferences_theme.themeEditText = R.style.EditText_Black;
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
                case 6:
                    preferences_theme.themeMain = R.style.AppTheme_Orange_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Orange_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Orange;
                    break;
                case 7:
                    preferences_theme.themeMain = R.style.AppTheme_Teal_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Teal_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Teal;
                    break;
                case 8:
                    preferences_theme.themeMain = R.style.AppTheme_Brown_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Brown_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Brown;
                    break;
                case 9:
                    preferences_theme.themeMain = R.style.AppTheme_Indigo_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Indigo_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Indigo;
                    break;
                default:
                    preferences_theme.themeMain = R.style.AppTheme_Green_NoActionBar;
                    preferences_theme.themePopup = R.style.AppTheme_Green_PopupOverlay;
                    preferences_theme.themeDialog = R.style.AlertDialog_Green;
            }

            preferences_hiddenEvents = getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Hidden_key), new HashSet<>());
            preferences_hiddenEventsRawIds = getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Hidden_rawIds_key), new HashSet<>());

            preferences_silentEvents = getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Silent_key), new HashSet<>());
            preferences_silentEventsRawIds = getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Silent_rawIds_key), new HashSet<>());

            preferences_favoriteEvents = getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Favorite_key), new HashSet<>());
            preferences_favoriteEventsRawIds = getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Favorite_rawIds_key), new HashSet<>());

            preferences_mergedIDs.clear();
            for (String element : getPreferenceStringSet(preferences, context.getString(R.string.pref_MergedID_key), new HashSet<>())) {
                int indexDiv = element.indexOf(Constants.STRING_COLON_SPACE);
                if (indexDiv > -1) {
                    preferences_mergedIDs.put(element.substring(0, indexDiv), element.substring(indexDiv + Constants.STRING_COLON_SPACE.length()));
                }
            }

            preferences_mergedRawIDs.clear();
            for (String element : getPreferenceStringSet(preferences, context.getString(R.string.pref_MergedRawID_key), new HashSet<>())) {
                int indexDiv = element.indexOf(Constants.STRING_COLON_SPACE);
                if (indexDiv > -1) {
                    preferences_mergedRawIDs.put(element.substring(0, indexDiv), element.substring(indexDiv + Constants.STRING_COLON_SPACE.length()));
                }
            }

            preferences_xDaysEvents.clear();
            for (String element : getPreferenceStringSet(preferences, context.getString(R.string.pref_xDaysEvents_key), new HashSet<>())) {
                int indexDiv = element.indexOf(Constants.STRING_COLON_SPACE);
                if (indexDiv > -1) {
                    preferences_xDaysEvents.put(element.substring(0, indexDiv), element.substring(indexDiv + Constants.STRING_COLON_SPACE.length()));
                }
            }

            //Источники событий

            preferences_Accounts = getPreferenceStringSet(preferences, context.getString(R.string.pref_Accounts_key), new HashSet<>());

            preferences_BirthDay_calendars = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key), new HashSet<>());

            preferences_OtherEvent_calendars = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Other_Calendars_key), new HashSet<>());

            preferences_HolidayEvent_calendars = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Holiday_Calendars_key), new HashSet<>());

            preferences_MultiType_calendars = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_MultiType_Calendars_key), new HashSet<>());

            //Настройки импорта

            preferences_rules_unrecognized = getPreferenceInt(preferences, context.getString(R.string.pref_CustomEvents_Rules_Unrecognized_key), context.getString(R.string.pref_CustomEvents_Rules_Unrecognized_default));
            preferences_birthday_calendars_rules = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_default));
            if (TextUtils.isEmpty(preferences_birthday_calendars_rules)) {
                preferences_birthday_calendars_rules = context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_default);
            }
            preferences_rules_calendars_nameformat = getPreferenceInt(preferences, context.getString(R.string.pref_CustomEvents_Rules_Calendars_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default));
            preferences_rules_files_nameformat = getPreferenceInt(preferences, context.getString(R.string.pref_CustomEvents_Rules_LocalFiles_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default));

            getRecentColors();

            dimen_List_details = resources.getDimension(R.dimen.event_details);
            dimen_List_name = resources.getDimension(R.dimen.event_name);
            dimen_list_date = resources.getDimension(R.dimen.event_date);

            updateShortCuts(preferences_extrafun);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void updateShortCuts(boolean enableExtraShortcuts) {
        //https://habr.com/ru/articles/593863/
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {

                Intent intentNotify = new Intent(context, NotifyActivity.class);
                intentNotify.setAction(Intent.ACTION_VIEW);
                ShortcutInfoCompat shortcutNotify = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_NOTIFY)
                        .setShortLabel(resources.getString(R.string.shortcut_notify))
                        .setIcon(IconCompat.createWithResource(context, R.drawable.shortcut_notify))
                        .setIntent(intentNotify)
                        .setRank(1)
                        .build();
                ShortcutManagerCompat.pushDynamicShortcut(context, shortcutNotify);

                if (enableExtraShortcuts) {

                    Intent intentQuiz = new Intent(context, QuizActivity.class);
                    intentQuiz.setAction(Intent.ACTION_VIEW);
                    ShortcutInfoCompat shortcutQuiz = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_QUIZ)
                            .setShortLabel(resources.getString(R.string.shortcut_quiz))
                            .setIcon(IconCompat.createWithResource(context, R.drawable.shortcut_quiz))
                            .setIntent(intentQuiz)
                            .setRank(2)
                            .build();
                    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutQuiz);

                    Intent intentSettings = new Intent(context, SettingsActivity.class);
                    intentSettings.setAction(Intent.ACTION_VIEW);
                    ShortcutInfoCompat shortcutSettings = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_SETTINGS)
                            .setShortLabel(resources.getString(R.string.shortcut_settings))
                            .setIcon(IconCompat.createWithResource(context, R.drawable.shortcut_settings))
                            .setIntent(intentSettings)
                            .setRank(3)
                            .build();
                    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutSettings);

                } else {

                    List<String> shortcutIds = new ArrayList<>();
                    shortcutIds.add(Constants.SHORTCUT_QUIZ);
                    shortcutIds.add(Constants.SHORTCUT_SETTINGS);
                    ShortcutManagerCompat.removeDynamicShortcuts(context, shortcutIds);
                    //context.getSystemService(ShortcutManager.class).disableShortcuts(shortcutIds);

                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Сохранение настроек в SharedPreferences
     */
    @SuppressLint("ApplySharedPref")
    void savePreferences() {

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
            editor.putString(context.getString(R.string.pref_Theme_key), Integer.toString(preferences_theme.prefNumber));
            editor.putString(context.getString(R.string.pref_Icon_key), preferences_icon);
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key), preferences_BirthDay_calendars);
            editor.putString(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), preferences_birthday_calendars_rules);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_Calendars_key), preferences_OtherEvent_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_Calendars_key), preferences_HolidayEvent_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_MultiType_Calendars_key), preferences_MultiType_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key), preferences_Birthday_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_LocalFiles_key), preferences_OtherEvent_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_LocalFiles_key), preferences_HolidayEvent_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_MultiType_LocalFiles_key), preferences_MultiType_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_Public_Ids_key), preferences_HolidayEvent_ids);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Distance_key), preferences_list_magnify_distance);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Name_key), preferences_list_magnify_name);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Details_key), preferences_list_magnify_details);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Date_key), preferences_list_magnify_date);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Age_key), preferences_list_magnify_age);

            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Установка языка (локали) приложению
     *
     * @param force Принудительно, даже если этот язык уже устанавливали ранее
     */
    void setLocale(boolean force) {

        if (context == null) return;

        //сделать так: https://stackoverflow.com/questions/39705739/android-n-change-language-programmatically/
        //для Android > N переделать выбор локали https://stackoverflow.com/questions/47165311/how-to-change-android-o-oreo-api-26-app-language
        //http://developer.alexanderklimov.ru/android/locale.php
        //https://stackoverflow.com/questions/9475589/how-to-get-string-from-different-locales-in-android

        try {

            Configuration configuration = context.getResources().getConfiguration();
            if (force || !preferences_language.equals(currentLocale) || !currentLocale.equals(configuration.locale.toString())) {

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
                setDisplayMetrics(this.getResources().getDisplayMetrics());
                displayMetrics_density = displayMetrics.density;
                resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                currentLocale = locale.toString();

                weekDaysShort = resources.getStringArray(R.array.weekDaysShort);
                eventNameNY = resources.getString(R.string.Event_NY).toLowerCase();
                eventNameEaster = resources.getString(R.string.Event_Easter).toLowerCase();
                eventNameCatholicEaster = resources.getString(R.string.Event_CatholicEaster).toLowerCase();

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    synchronized boolean getEvents(Context in_context) {

        if (in_context != null) setContext(in_context);
        if (getContext() == null) setContext(getContext().getApplicationContext());
        if (getContext() == null) return false;

        try {

            eventList.clear();
            eventListUnsorted.clear();
            map_organizations.clear();
            map_contacts_titles.clear();
            map_contacts_aliases.clear();
            map_contacts_data.clear();
            map_contacts_ids.clear();
            map_contacts_rawIds.clear();
            deathDatesForIds.clear();
            birthdayDatesForIds.clear();
            map_events_weblinks.clear();
            map_notes.clear();
            idsAllCalendarEvents.clear();
            map_contacts_names.clear();
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
            statTimeGetHolidayEvents = 0;
            statFavoriteEventsCount = 0;

            needUpdateEventList = false;

            getPreferences();

            //todo: сделать через отдельный thread, но сделать это опциональным
            //https://stackoverflow.com/questions/61252550/android-how-to-use-kotlin-coroutine-in-java
            //https://stackoverflow.com/questions/58767733/the-asynctask-api-is-deprecated-in-android-11-what-are-the-alternatives

            final String idBirthday = getEventType(Constants.Type_BirthDay);
            final String idOther = getEventType(Constants.Type_Other);
            final String idHoliday = getEventType(Constants.Type_HolidayEvent);

            boolean result = getContactsEvents()
                    | getCalendarEvents(idBirthday)
                    | getCalendarEvents(idOther)
                    | getCalendarEvents(idHoliday)
                    | getFileEvents(idBirthday)
                    | getFileEvents(idOther)
                    | getFileEvents(idHoliday)
                    | (!preferences_MultiType_files.isEmpty() && getFileEvents(Constants.Type_MultiEvent))
                    | (!preferences_MultiType_calendars.isEmpty() && getCalendarEvents(Constants.Type_MultiEvent))
                    | getHolidayEvents();

            statFavoriteEventsCount += preferences_favoriteEvents.size();

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

        try {

            long statCurrentModuleStart = System.currentTimeMillis();
            StringBuilder dataRow;
            TreeMap<Integer, String> userData = new TreeMap<>();
            List<String> dataList = new ArrayList<>();

            if (checkNoContactsAccess()) return false;

            if (contentResolver == null) contentResolver = context.getContentResolver();
            ColumnIndexCache cache = new ColumnIndexCache();

            //Организации и должности
            final String[] projectionOrgTitle = new String[]{
                    Constants.ColumnNames_CONTACT_ID,
                    ContactsContract.CommonDataKinds.Organization.COMPANY,
                    ContactsContract.CommonDataKinds.Organization.TITLE
            };
            Cursor contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionOrgTitle,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, Constants.ColumnNames_CONTACT_ID));

                        String organization = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Organization.COMPANY));
                        if (!map_organizations.containsKey(personID) && organization != null && !organization.isEmpty())
                            map_organizations.put(personID, organization);

                        String title = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Organization.TITLE));
                        if (!map_contacts_titles.containsKey(personID) && title != null && !title.isEmpty())
                            map_contacts_titles.put(personID, title);

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statContactsOrganizationCount = map_organizations.size();
            statContactsTitleCount = map_contacts_titles.size();
            cache.clear();

            //Псевдонимы
            final String[] projectionNick = new String[]{Constants.ColumnNames_CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME};
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionNick,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE},
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, Constants.ColumnNames_CONTACT_ID));
                        String nick = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Nickname.NAME));
                        if (nick != null && !nick.isEmpty()) {
                            if (!map_contacts_aliases.containsKey(personID))
                                map_contacts_aliases.put(personID, nick);
                            //todo: добавлять ники в map_contacts_names
                        }

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statContactsNicknameCount = map_contacts_aliases.size();
            cache.clear();

            //Web ссылки
            final String[] projectionURL = new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Website.URL};
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionURL,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE},
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
                                if (!TextUtils.isEmpty(URlstored))
                                    map_events_weblinks.put(personID, URlstored.concat(Constants.STRING_2TILDA).concat(URL));
                            }
                            statContactsURLCount++;
                        }

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            cache.clear();

            //Заметки
            //https://stackoverflow.com/a/6301244/4928833
            final String[] projectionNotes = new String[]{Constants.ColumnNames_CONTACT_ID, ContactsContract.CommonDataKinds.Note.NOTE};
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionNotes,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE},
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, Constants.ColumnNames_CONTACT_ID));
                        String note = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Note.NOTE));
                        if (!map_notes.containsKey(personID)) map_notes.put(personID, note != null ? note.replace(Constants.STRING_EOL, Constants.STRING_SPACE) : Constants.STRING_EMPTY);

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            cache.clear();

            //Контакты
            final String[] projectionAllContacts = new String[]{
                    ContactsContract.RawContacts.CONTACT_ID,
                    ContactsContract.RawContacts._ID,
                    ContactsContract.Data.DISPLAY_NAME,
                    ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE
            };
            contactData = contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    projectionAllContacts,
                    null,
                    null,
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        final String personID = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.RawContacts.CONTACT_ID));
                        final String personRawID = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.RawContacts._ID));

                        if (personID != null && personRawID != null && !map_contacts_ids.containsKey(personID)) {
                            map_contacts_ids.put(personID, personRawID);
                            map_contacts_rawIds.put(personRawID, personID);
                        }

                        //ИОФ
                        final String personName = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Data.DISPLAY_NAME));
                        if (personName != null) {
                            final String personNameNormalized = normalizeName(personName);
                            if (!checkForNull(personNameNormalized).isEmpty() && !map_contacts_names.containsKey(personNameNormalized)) {
                                map_contacts_names.put(personNameNormalized, personID);
                            }
                            map_contacts_data.put(personID + ContactsContract.Data.DISPLAY_NAME, checkForNull(personName));

                            //ИФ
                            final String personNameShortNormalized = Person.getShortName(personNameNormalized, Constants.pref_List_NameFormat_FirstSecondLast, context);
                            if (!map_contacts_names.containsKey(personNameShortNormalized)) {
                                map_contacts_names.put(personNameShortNormalized, personID);
                            }
                        }

                        //ФИО
                        final String personNameAlt = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE));
                        if (personNameAlt != null) {
                            final String personNameAltNormalized = normalizeName(personNameAlt);
                            if (!checkForNull(personNameAltNormalized).isEmpty() && !map_contacts_names.containsKey(personNameAltNormalized)) {
                                map_contacts_names.put(personNameAltNormalized, personID);
                            }
                            map_contacts_data.put(personID + ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE, checkForNull(personNameAlt));

                            //ФИ
                            final String personNameAltShortNormalized = Person.getShortName(personNameAltNormalized, Constants.pref_List_NameFormat_LastFirstSecond, context);
                            if (!map_contacts_names.containsKey(personNameAltShortNormalized)) {
                                map_contacts_names.put(personNameAltShortNormalized, personID);
                            }
                        }

                        //todo: добавить имена латиницей (для мэппинга)

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            statContactsCount = map_contacts_ids.size();
            cache.clear();

            //События
            final String[] projectionContactsEvents = new String[]{
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
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE},
                    ContactsContract.Data.DISPLAY_NAME + " ASC, " + ContactsContract.CommonDataKinds.Event.TYPE + " ASC, " + ContactsContract.CommonDataKinds.Event.LABEL + " ASC"
            );
            if (cursor == null) return false;

            int countErrors = 0;
            String eventKey = Constants.STRING_EMPTY;

            if (cursor.moveToFirst()) {
                do {
                    try {
                        eventKey = getContactEventFromCursor(cursor, userData, dataList, cache, eventKey);
                    } catch (RuntimeException e) {
                        countErrors++;
                        if (countErrors < 3) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(getMethodName(3)).append(Constants.STRING_COLON_SPACE).append(e).append(Constants.STRING_EOL);
                            for (String name : cursor.getColumnNames()) {
                                String data = cursor.getString(cache.getColumnIndex(cursor, name));
                                if (data != null && !data.equals(Constants.STRING_0))
                                    sb.append(name).append(Constants.STRING_COLON_SPACE).append(data).append(Constants.STRING_EOL);
                            }
                            ToastExpander.showInfoMsg(context, sb.toString());
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
                        if (personID != null && !personID.isEmpty())
                            map_eventsBySubtypeAndPersonID_offset.put(personID + Constants.STRING_2HASH + userData.get(Position_eventSubType), dataList.size() - 1);
                        //String personNameAlt = userData.get(Position_personFullNameAlt);
                        //if (personNameAlt != null && !personNameAlt.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + normalizeName(personNameAlt), dataList.size() - 1);
                    }

                    userData.clear();

                }
            }
            cache.clear();
            cursor.close();

            eventList.addAll(dataList);
            statEventsCount += statContactsEventCount;
            dataList.clear();
            statTimeGetContactEvents = System.currentTimeMillis() - statCurrentModuleStart;

            if (countErrors > 1)
                ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + resources.getString(R.string.msg_errors_total) + countErrors);

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    int getContactsEventsCount(String accountType, String accountName) {

        int count = 0;
        if (checkNoContactsAccess()) return count;

        try {

            if (contentResolver == null) contentResolver = context.getContentResolver();

            final StringBuilder selection = new StringBuilder();
            selection.append(ContactsContract.Data.MIMETYPE).append(" = '").append(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE).append("' ");
            if (accountType != null) {
                if (Constants.STRING_NULL.equalsIgnoreCase(accountType)) {
                    selection.append(" AND ").append(Constants.ColumnNames_ACCOUNT_TYPE).append(" is null ");
                } else {
                    selection.append(" AND ").append(Constants.ColumnNames_ACCOUNT_TYPE).append(" = '").append(accountType).append("' ");
                }
            }
            if (accountName != null) {
                selection.append(" AND ").append(Constants.ColumnNames_ACCOUNT_NAME).append(" = '").append(accountName).append("' ");
            }
            Cursor cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{android.provider.BaseColumns._ID},
                    selection.toString(),
                    null,
                    null
            );
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return count;
    }

    int getCalendarEventsCount(String calID) {

        int count = 0;
        if (checkNoCalendarAccess()) return count;

        try {

            if (contentResolver == null) contentResolver = context.getContentResolver();

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

            String[] projection = new String[]{CalendarContract.Instances.EVENT_ID};
            String selection = CalendarContract.Events.CALENDAR_ID + " = " + calID;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startTime.getTimeInMillis());
            ContentUris.appendId(builder, endTime.getTimeInMillis());

            Cursor cursor = contentResolver.query(builder.build(), projection, selection, null, "dtstart ASC");
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return count;
    }

    int getFileEventsCount(String file, boolean needEventLabel) {

        int count = 0;
        try {

            String[] eventsArray = readFileToString(file, Constants.STRING_EOL).split(Constants.STRING_EOL, -1);
            if (eventsArray[0].isEmpty()) return count;
            @Nullable Event event = null;
            Calendar now = new GregorianCalendar();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);

            for (String eventRow : eventsArray) {

                String eventLine = eventRow.trim().replace("\uFEFF", Constants.STRING_EMPTY);
                if (eventLine.isEmpty() || eventLine.startsWith(Constants.STRING_HASH) || eventLine.startsWith(Constants.STRING_DSLASH))
                    continue;

                String eventDateString = Constants.STRING_EMPTY;
                @Nullable Date dateEvent = null;
                String eventNewDate = null;
                String eventLabel_forSearch = Constants.STRING_EMPTY;
                String eventTitle = Constants.STRING_EMPTY;
                boolean isAD = true;

                int indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                boolean isBirthdaysPlusEvent = eventLine.startsWith(Constants.STRING_BDP_DIV)
                        && eventLine.endsWith(Constants.STRING_BDP_EOL);

                //BirthdayPro, DarkBirthday: <Дата без пробелов>[,<пробел>флаги] название праздника или ФИО [(должность)]
                if (!isBirthdaysPlusEvent) {

                    if (indexFirstSpace > -1) {

                        final int indexComma = eventLine.indexOf(Constants.STRING_COMMA);
                        if (indexComma > -1 && indexComma < indexFirstSpace) { //Есть флаги

                            if (indexFirstSpace - indexComma == 1) { //После запятой пробел - убираем
                                eventLine = eventLine.substring(0, indexComma + 1) + eventLine.substring(indexFirstSpace + 1);
                                indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                                if (indexFirstSpace == -1) continue;
                            }

                            eventDateString = eventLine.substring(0, indexComma);
                            String flags = eventLine.substring(indexComma + 1, indexFirstSpace);
                            eventTitle = eventLine.substring(indexFirstSpace + 1).trim();

                            if (!flags.isEmpty()) {
                                if (flags.contains(Constants.STRING_1)) {
                                    flags = flags.replace(Constants.STRING_1, Constants.STRING_EMPTY);
                                }
                                if (flags.contains(Constants.STRING_BC)) {
                                    isAD = false;
                                    flags = flags.replace(Constants.STRING_BC, Constants.STRING_EMPTY);
                                }
                                if (needEventLabel) {
                                    eventLabel_forSearch = flags.replace(Constants.STRING_UNDERSCORE, Constants.STRING_SPACE);
                                }
                            }

                        } else {

                            eventDateString = eventLine.substring(0, indexFirstSpace);
                            eventTitle = eventLine.substring(indexFirstSpace + 1).trim();

                        }
                    }

                } else { //Birthdays Plus: |ДДДД-ММ-ДД|ИОФ|тип (Birthday, Anniversary, Custom)|наименование события или null|

                    final String[] eventBDPdetails = eventLine.split(Constants.STRING_BDP_DIV, -1);

                    if (eventBDPdetails.length == 5) {

                        eventDateString = eventBDPdetails[1];
                        eventTitle = eventBDPdetails[2];
                        if (eventBDPdetails[3].equals(Constants.STRING_BDP_CUSTOM)) {
                            eventLabel_forSearch = eventBDPdetails[4].replace(Constants.STRING_BDP_EOL, Constants.STRING_EMPTY);
                        } else {
                            eventLabel_forSearch = eventBDPdetails[3];
                        }

                    }
                }

                if (eventDateString.isEmpty()) continue;

                if (needEventLabel) {
                    event = recognizeEventByLabel(eventLabel_forSearch, Constants.Storage_File, eventTitle);
                }

                if (preferences_rules_unrecognized == Rules_Unrecognized_Skip && (event == null || event.icon == R.drawable.ic_event_unknown)) {
                    continue;
                }

                int indexDateNoYear = isBirthdaysPlusEvent ? eventDateString.indexOf(Constants.STRING_BDP_NO_YEAR) : eventDateString.indexOf(Constants.STRING_0000);
                if (indexDateNoYear == -1) { //С годом
                    try {
                        if (isAD) {

                            if (!isBirthdaysPlusEvent) {
                                String dateNextFloatingEvent = computeFloatingDate(eventDateString);
                                if (!eventDateString.equals(dateNextFloatingEvent)) {
                                    Date eventDateFirstTime = sdf_DDMMYYYY.parse(dateNextFloatingEvent); //Пытаемся определить год первоначального события
                                    if (eventDateFirstTime != null) {
                                        try {
                                            eventDateFirstTime.setYear(Integer.parseInt(eventDateString.substring(eventDateString.lastIndexOf(Constants.STRING_PERIOD) + 1)) - 1900);
                                        } catch (NumberFormatException ignored) { /**/ }
                                    }
                                    eventDateString = dateNextFloatingEvent;
                                }
                            }

                            dateEvent = sdf_DDMMYYYY.parse(eventDateString);
                        } else {
                            dateEvent = sdf_DDMMYYYY_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                        }
                    } catch (ParseException e1) {
                        try {
                            if (isAD) {
                                dateEvent = sdf_india.parse(eventDateString);
                            } else {
                                dateEvent = sdf_india_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                            }
                        } catch (ParseException e2) {
                            try {
                                if (isAD) {
                                    dateEvent = sdf_uk.parse(eventDateString);
                                } else {
                                    dateEvent = sdf_uk_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                }
                            } catch (ParseException e3) {
                                try {
                                    if (isAD) {
                                        dateEvent = sdf_java.parse(eventDateString);
                                    } else {
                                        dateEvent = sdf_java_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                    }
                                } catch (ParseException e4) {
                                    //Не получилось распознать
                                }
                            }
                        }
                    }

                } else { //Без года

                    String dateNextEvent = eventDateString.substring(0, indexDateNoYear) + now.get(Calendar.YEAR);
                    try {
                        if (!isBirthdaysPlusEvent) {
                            String dateNextFloatingEvent = computeFloatingDate(dateNextEvent);
                            if (!dateNextEvent.equals(dateNextFloatingEvent)) {
                                dateNextEvent = dateNextFloatingEvent;
                            }
                        }
                        dateEvent = sdf_DDMMYYYY.parse(dateNextEvent);
                    } catch (ParseException e1) {
                        try {
                            dateEvent = sdf_india.parse(dateNextEvent);
                        } catch (ParseException e2) {
                            try {
                                dateEvent = sdf_uk.parse(dateNextEvent);
                            } catch (ParseException e3) {
                                try {
                                    dateNextEvent = eventDateString.replace(Constants.STRING_BDP_NO_YEAR, Integer.toString(now.get(Calendar.YEAR)));
                                    dateEvent = sdf_java.parse(dateNextEvent);
                                } catch (ParseException e4) {
                                    //Не получилось распознать
                                }
                            }
                        }
                    }
                    if (dateEvent != null && now.after(getCalendarFromDate(dateEvent)))
                        dateEvent = addYear(dateEvent, 1);
                }

                if (dateEvent != null) {
                    count++;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return count;
    }

    @NonNull
    private String getContactEventFromCursor(
            @NonNull Cursor cursor,
            @NonNull TreeMap<Integer, String> userData,
            @NonNull List<String> dataList,
            @NonNull ColumnIndexCache cache,
            @NonNull String eventKey) {

        String eventKey_current = eventKey;
        String eventDate = null;
        String eventType = null;
        String accountKey = null;

        try {
            StringBuilder dataRow;
            eventDate = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.DATA));
            eventType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));
            String eventSubType = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE)));
            String accountType = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_TYPE));
            if (accountType == null) accountType = Constants.STRING_NULL;
            String accountName = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_NAME));
            if (accountName == null) accountName = getResources().getString(R.string.account_type_local);
            accountKey = accountName + Constants.STRING_PARENTHESIS_OPEN + accountType + Constants.STRING_PARENTHESIS_CLOSE;

            if (eventDate != null && eventType != null && (preferences_Accounts.isEmpty() || preferences_Accounts.contains(accountKey))) {

                String contactName = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME)));
                String contactNameAlt = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)));
                String eventLabel = checkForNull(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.LABEL)));
                boolean isEventLabel = !TextUtils.isEmpty(eventLabel);
                String eventCaption = Constants.STRING_EMPTY;
                int eventIcon = R.drawable.ic_event_unknown;
                String eventEmoji = getResources().getString(R.string.event_type_unknown_emoji);
                statContactsEventCount++;
                boolean isUnrecognized = false;

                if (eventType.equals(getEventType(Constants.Type_BirthDay))
                        || (isEventLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_birthday);
                    eventIcon = R.drawable.ic_event_birthday; //https://icons8.com/icon/21460/birthday
                    eventEmoji = getResources().getString(R.string.event_type_birthday_emoji);
                    eventSubType = getEventType(Constants.Type_BirthDay);

                } else if (eventType.equals(getEventType(Constants.Type_Death))
                        || (isEventLabel && preferences_death_labels != null && preferences_death_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_death);
                    eventIcon = R.drawable.ic_event_death;
                    //https://emojipedia.org/google/android-6.0.1/new/
                    eventEmoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_death_emoji) : "\uD83D\uDCC5";
                    eventSubType = getEventType(Constants.Type_Death);

                } else if (eventType.equals(getEventType(Constants.Type_Anniversary))
                        || (isEventLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_anniversary);
                    eventIcon = R.drawable.ic_event_wedding; //https://www.flaticon.com/free-icon/wedding-rings_224802
                    eventEmoji = getResources().getString(R.string.event_type_wedding_emoji);
                    eventSubType = getEventType(Constants.Type_Anniversary);

                } else if (eventType.equals(getEventType(Constants.Type_Other))
                        || (isEventLabel && preferences_other_event_labels != null && preferences_other_event_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_other);
                    eventIcon = R.drawable.ic_event_other; //https://icons8.com/icon/set/event/office
                    eventEmoji = getResources().getString(R.string.event_type_other_emoji);
                    eventSubType = getEventType(Constants.Type_Other);

                } else if (eventType.equals(getEventType(Constants.Type_HolidayEvent))
                        || (isEventLabel && preferences_holiday_event_labels != null && preferences_holiday_event_labels.reset(eventLabel.toLowerCase()).find())) {

                    eventCaption = getResources().getString(R.string.event_type_holiday);
                    eventIcon = R.drawable.ic_event_holiday;
                    eventEmoji = getResources().getString(R.string.event_type_holiday_emoji);
                    eventSubType = getEventType(Constants.Type_HolidayEvent);

                } else if (isEventLabel) {

                    if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent1_caption;
                        eventIcon = R.drawable.ic_event_custom1;
                        eventEmoji = getResources().getString(R.string.event_type_custom1_emoji);
                        eventSubType = getEventType(Constants.Type_Custom1);
                        if (!preferences_customevent1_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent2_caption;
                        eventIcon = R.drawable.ic_event_custom2;
                        eventEmoji = getResources().getString(R.string.event_type_custom2_emoji);
                        eventSubType = getEventType(Constants.Type_Custom2);
                        if (!preferences_customevent2_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent3_caption;
                        eventIcon = R.drawable.ic_event_custom3;
                        eventEmoji = getResources().getString(R.string.event_type_custom3_emoji);
                        eventSubType = getEventType(Constants.Type_Custom3);
                        if (!preferences_customevent3_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent4_caption;
                        eventIcon = R.drawable.ic_event_custom4;
                        eventEmoji = getResources().getString(R.string.event_type_custom4_emoji);
                        eventSubType = getEventType(Constants.Type_Custom4);
                        if (!preferences_customevent4_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = preferences_customevent5_caption;
                        eventIcon = R.drawable.ic_event_custom5;
                        eventEmoji = getResources().getString(R.string.event_type_custom5_emoji);
                        eventSubType = getEventType(Constants.Type_Custom5);
                        if (!preferences_customevent5_useyear && !eventDate.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDate = Constants.STRING_2MINUS + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_nameday);
                        eventIcon = R.drawable.ic_event_nameday;
                        eventEmoji = getResources().getString(R.string.event_type_nameday_emoji);
                        eventSubType = getEventType(Constants.Type_NameDay);

                    } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel.toLowerCase()).find()) {

                        eventCaption = getResources().getString(R.string.event_type_crowning);
                        eventIcon = R.drawable.ic_event_crowning; //https://iconscout.com/icon/wedding-destination-romance-building-emoj-symbol
                        eventEmoji = getResources().getString(R.string.event_type_crowning_emoji);
                        eventSubType = getEventType(Constants.Type_Crowning);

                    } else {

                        isUnrecognized = true;
                        if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Other) {

                            eventCaption = getResources().getString(R.string.event_type_other);
                            eventIcon = R.drawable.ic_event_other;
                            eventEmoji = getResources().getString(R.string.event_type_other_emoji);
                            eventType = getEventType(Constants.Type_Other);
                            eventSubType = getEventType(Constants.Type_Other);

                        } else if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Unrecognized) {

                            eventCaption = getResources().getString(R.string.event_type_unrecognized);
                            eventIcon = R.drawable.ic_event_unknown;
                            eventEmoji = getResources().getString(R.string.event_type_unknown_emoji);
                            eventType = getEventType(Constants.Type_Unrecognized);
                            eventSubType = getEventType(Constants.Type_Unrecognized);

                        } else {
                            return eventKey_current; //Пропускаем событие
                        }

                    }

                }
                if (isEventLabel && eventCaption.isEmpty()) eventCaption = eventLabel;

                if (eventIcon == R.drawable.ic_event_unknown)
                    ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_type_parse_error, eventLabel, contactName));

                String eventKey_next = contactName.concat(Constants.STRING_COMMA).concat(eventType);

                //Наименование события в ключе только для пользовательских событий
                if (eventType.equals(getEventType(Constants.Type_Custom)) || isUnrecognized) {
                    eventKey_next = eventKey_next.concat(Constants.STRING_COMMA).concat(eventLabel);
                }

                String newEventDate = accountType + Constants.STRING_COLON_SPACE + eventDate + Constants.STRING_COLON_SPACE
                        + getHash((!accountType.equals(Constants.STRING_NULL) ? Constants.eventSourceContactPrefix : Constants.eventSourceLocalPrefix) + accountKey);

                if (!eventKey_next.equalsIgnoreCase(eventKey_current)) { //Начало данных нового контакта

                    if (!userData.isEmpty()) { // Уже есть накопленные данные. Нужно сохранить всё, что накопили и обнулить UserData
                        dataRow = new StringBuilder();
                        int rNum = 0;
                        for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                            rNum++;
                            if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                            dataRow.append(entry.getValue());
                        }
                        if (dataList.add(dataRow.toString())) {
                            String personID = userData.get(Position_contactID);
                            if (personID != null && !personID.isEmpty())
                                map_eventsBySubtypeAndPersonID_offset.put(personID + Constants.STRING_2HASH + userData.get(Position_eventSubType), dataList.size() - 1);
                            //String personNameAlt = userData.get(Position_personFullNameAlt);
                            //if (personNameAlt != null && !personNameAlt.isEmpty()) map_eventsBySubtypeAndPersonID_offset.put(userData.get(Position_eventSubType) + STRING_2HASH + normalizeName(personNameAlt), dataList.size() - 1);
                        }
                        userData.clear();
                    }

                    String contactID = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID));
                    if (contactID == null) return eventKey_current;
                    String contactFIO = contactName.replace(Constants.STRING_COMMA_SPACE, Constants.STRING_SPACE);

                    String contactTitle = map_contacts_titles.get(contactID);
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

                    eventKey_current = eventKey_next;

                    userData.put(Position_personFullName, contactFIO);
                    userData.put(Position_personFullNameAlt, contactNameAlt.replace(Constants.STRING_COMMA_SPACE, Constants.STRING_SPACE));
                    userData.put(Position_contactID, contactID);
                    userData.put(Position_photo_uri, cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.PHOTO_URI)));
                    userData.put(Position_eventCaption, eventCaption); //Наименование события
                    //подпорка: почему-то для одиноких Skype событий в eventLabel находится дата события
                    userData.put(Position_eventLabel, !eventLabel.equals(eventCaption) & !newEventDate.contains(eventLabel) ? eventLabel : Constants.STRING_EMPTY); //Заголовок пользовательского события
                    userData.put(Position_eventType, eventType); //Тип события
                    userData.put(Position_eventSubType, eventSubType); //Подтип события
                    userData.put(Position_organization, checkForNull(map_organizations.get(contactID)));
                    userData.put(Position_title, contactTitle);
                    userData.put(Position_dates, newEventDate);
                    userData.put(Position_eventIcon, Integer.toString(eventIcon));
                    userData.put(Position_eventEmoji, eventEmoji);
                    if (Constants.STRING_1.equals(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.STARRED)))) {
                        userData.put(Position_starred, Constants.STRING_1);
                        statFavoriteEventsCount++;
                    }
                    userData.put(Position_nickname, checkForNull(map_contacts_aliases.get(contactID)));
                    userData.put(Position_eventStorage, Constants.STRING_STORAGE_CONTACTS); //Где искать событие по ID
                    userData.put(Position_eventSource, getResources().getString(R.string.msg_account_info, accountName));
                    userData.put(Position_eventURL, checkForNull(map_events_weblinks.get(contactID)));
                    userData.put(Position_eventDescription, checkForNull(map_notes.get(contactID)));
                    userData.put(Position_rawContactID, checkForNull(map_contacts_ids.get(contactID)));

                    fillEmptyUserData(userData);

                } else { //Продолжаем добавлять даты контакта

                    String existingDates = userData.get(Position_dates);
                    if (existingDates != null && !existingDates.contains(newEventDate))
                        userData.put(Position_dates, existingDates.concat(Constants.STRING_2TILDA).concat(newEventDate));

                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + resources.getString(R.string.msg_errors_details, accountKey, eventType, eventDate));
        }
        return eventKey_current;

    }

    private boolean getCalendarEvents(@NonNull String eventType) {
        //todo: использовать цвета календарей https://www.javatips.net/api/android.provider.calendarcontract.instances
        final TreeMap<Integer, String> userData = new TreeMap<>();
        Cursor cursor = null;

        try (ColumnIndexCache cache = new ColumnIndexCache()) {

            long statCurrentModuleStart = System.currentTimeMillis();
            int statEventsCountByType = 0;

            if (checkNoCalendarAccess()) return false;

            Set<String> preferences_calendars = getPreferences_Calendars(eventType);
            if (preferences_calendars.isEmpty()) return false;

            StringBuilder dataRow;
            Event event = new Event();

            if (map_calendars.isEmpty()) recieveCalendarList();
            final boolean isFirstSecondLastFormat = Integer.toString(preferences_rules_calendars_nameformat).equals(context.getString(R.string.pref_List_NameFormat_FirstSecondLast));

            //https://stackoverflow.com/questions/25734285/how-to-get-the-real-time-of-recurring-events
            //https://stackoverflow.com/questions/10133616/reading-all-of-todays-events-using-calendarcontract-android-4-0

            if (contentResolver == null) contentResolver = context.getContentResolver();
            String[] projection = new String[]{
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.DESCRIPTION, //todo: доделать правила и под это поле
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Events.DTSTART, //начало первоначального события
                    CalendarContract.Events.ALL_DAY
            };

            Calendar startPeriod = Calendar.getInstance();
            startPeriod.set(Calendar.HOUR_OF_DAY, 0);
            startPeriod.set(Calendar.MINUTE, 0);
            startPeriod.set(Calendar.SECOND, 0);
            startPeriod.set(Calendar.MILLISECOND, 0);
            final int zoneOffset = TimeZone.getDefault().getOffset(startPeriod.getTimeInMillis()); //событие на весь день начинается в 00:00:00 UTC, надо скорректировать часовую зону
            startPeriod.add(Calendar.MILLISECOND, zoneOffset);

            Calendar endPeriod = Calendar.getInstance();
            endPeriod.set(Calendar.YEAR, startPeriod.get(Calendar.YEAR) + 1);
            endPeriod.set(Calendar.HOUR_OF_DAY, 0);
            endPeriod.set(Calendar.MINUTE, 0);
            endPeriod.set(Calendar.SECOND, 0);
            endPeriod.set(Calendar.MILLISECOND, 0);
            endPeriod.add(Calendar.MILLISECOND, zoneOffset);
            //endPeriod.add(Calendar.SECOND, -1);

            String[] arrRules;
            List<Matcher> matcherNames = new ArrayList<>();
            boolean useEventYear = false;
            final List<String> eventURLs = new ArrayList<>();

            boolean isMultiTypeSource = eventType.equals(Constants.Type_MultiEvent);
            if (eventType.equals(getEventType(Constants.Type_BirthDay))) {

                event.caption = getResources().getString(R.string.event_type_birthday);
                event.type = eventType;
                event.subType = getEventType(Constants.Type_BirthDay);
                event.icon = R.drawable.ic_event_birthday;
                event.emoji = getResources().getString(R.string.event_type_birthday_emoji);
                event.needScanContacts = true;
                useEventYear = preferences_birthday_calendars_useyear;

            } else if (eventType.equals(getEventType(Constants.Type_Other))) {

                event.caption = getResources().getString(R.string.event_type_calendar);
                event.type = eventType;
                event.subType = getEventType(Constants.Type_CalendarEvent);
                event.icon = R.drawable.ic_event_other;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";

            } else if (eventType.equals(getEventType(Constants.Type_HolidayEvent))) {

                event.caption = getResources().getString(R.string.event_type_holiday);
                event.type = eventType;
                event.subType = eventType; //getEventType(Constants.Type_HolidayEvent);
                event.icon = R.drawable.ic_event_holiday;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_holiday_emoji) : "\uD83C\uDFD6️";

            } else if (isMultiTypeSource) {

                useEventYear = true;

            } else {
                return false;
            }

            if (eventType.equals(getEventType(Constants.Type_BirthDay)) || isMultiTypeSource) {
                arrRules = preferences_birthday_calendars_rules.split(Constants.STRING_PIPE, -1);
                if (!arrRules[0].isEmpty()) {
                    for (String rule : arrRules) {
                        if (rule.contains(Constants.RULE_TAG_NAME)) {
                            matcherNames.add(Pattern.compile(rule.replace(Constants.RULE_TAG_NAME, "(.*)")).matcher(Constants.STRING_EMPTY));
                        }
                    }
                }
            }

            StringBuilder calIDs = new StringBuilder();
            for (String calID : preferences_calendars) {
                if (calIDs.length() > 0)
                    calIDs.append(" OR " + CalendarContract.Instances.CALENDAR_ID + " = ");
                calIDs.append(calID);
            }
            String selection = CalendarContract.Instances.CALENDAR_ID + " = " + calIDs;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startPeriod.getTimeInMillis());
            ContentUris.appendId(builder, endPeriod.getTimeInMillis());

            cursor = contentResolver.query(
                    builder.build(),
                    projection,
                    selection,
                    null,
                    "dtstart ASC"
            );

            if (cursor != null) {
                if (cursor.getCount() > 0) {

                    int importMethod_Standalone = 0; //Календарное событие без контакта
                    int importMethod_NewContactEvent = 1; //Контакт найден, но у него нет данных о cобытии этого типа - обновляем событие по карточке контакта
                    int importMethod_AdditionalDateToContactEvent = 2; //Контакт найден, у него есть такое же событие - добавляем к источникам дат ещё одно значение

                    cursor_loop: while (cursor.moveToNext()) {
                        String calendarId = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.CALENDAR_ID));
                        String calendarTitle = map_calendars.get(calendarId);
                        final String eventTitle = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.TITLE));
                        if (eventTitle == null || eventTitle.trim().isEmpty()) continue;
                        final String eventSource = calendarTitle != null
                                ? getResources().getString(R.string.msg_calendar_info, getKeyParts(calendarTitle)[0])
                                : getResources().getString(R.string.event_type_calendar);
                        Calendar dateStart = getCalendarFromDate(new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.BEGIN)))));
                        dateStart.add(Calendar.MILLISECOND, zoneOffset);
                        Calendar dateEnd = getCalendarFromDate(new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.END)))));
                        dateEnd.add(Calendar.MILLISECOND, zoneOffset);
                        Date dtStart = new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DTSTART))));

                        if (cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Events.ALL_DAY)) == 1) { //У AllDay событий зона всегда UTC
                            if (TimeZone.getDefault().getRawOffset() < 0) { //Для отрицательных зон надо прибавлять день
                                dateStart.add(Calendar.DATE, 1);
                                dateEnd.add(Calendar.DATE, 1);
                            }

                            //Событие на весь день заканчивается на следующий день, а не в 23:59:59. Исправляем
                            dateEnd.add(Calendar.DATE, -1);
                            dateEnd.set(Calendar.HOUR_OF_DAY, 23);
                            dateEnd.set(Calendar.MINUTE, 59);
                            dateEnd.set(Calendar.SECOND, 59);
                            dateEnd.set(Calendar.MILLISECOND, 0);
                            dateEnd.add(Calendar.MILLISECOND, zoneOffset);
                        }

                        if (dateEnd.before(startPeriod)) continue; //Если событие выпало из периода
                        if (dateStart.before(startPeriod) && dateEnd.compareTo(startPeriod) >= 0) { //Если событие начинается до периода, но заканчивает после старта периода
                            while (dateStart.before(startPeriod)) {
                                dateStart.add(Calendar.DATE, 1);
                            }
                        }

                        do {
                            userData.clear();
                            final String eventNewDate = Constants.EVENT_PREFIX_CALENDAR_EVENT + Constants.STRING_COLON_SPACE
                                    + (useEventYear ? sdf_java.format(dateStart.getTime()) : sdf_java_no_year.format(dateStart.getTime())) + Constants.STRING_COLON_SPACE
                                    + getHash(Constants.eventSourceCalendarPrefix + calendarId);
                            int importMethod = importMethod_Standalone;
                            final String eventID = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.EVENT_ID));
                            idsAllCalendarEvents.add(eventID);

                            String contactID = null;
                            statEventsCountByType++;
                            userData.put(Position_personFullName, eventTitle);
                            userData.put(Position_personFullNameAlt, eventTitle);
                            userData.put(Position_eventStorage, Constants.STRING_STORAGE_CALENDAR);

                            eventURLs.clear();
                            String eventURL;
                            String eventDescription = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DESCRIPTION));
                            if (eventDescription != null) {
                                eventDescription = eventDescription.replace(Constants.STRING_EOL, Constants.STRING_SPACE);
                                int indURL;
                                int indSpace;

                                for (String prefix : new String[]{Constants.STRING_HTTPS, Constants.STRING_HTTP}) {
                                    indURL = eventDescription.toLowerCase().indexOf(prefix);
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

                                if (isMultiTypeSource) {
                                    event = recognizeEventByLabel(eventDescription, Constants.Storage_Calendar, eventTitle);
                                }

                            }

                            if (preferences_rules_unrecognized == Rules_Unrecognized_Skip && event.icon == R.drawable.ic_event_unknown) {
                                continue cursor_loop;
                            }

                            String mergedID = getMergedID(eventID);
                            if (!mergedID.isEmpty()) contactID = mergedID;
                            String contactTitle = Constants.STRING_EMPTY;
                            boolean namedFromEvent = false;

                            if (contactID == null && event.needScanContacts && !matcherNames.isEmpty()) {
                                String foundName;
                                for (Matcher matcherName : matcherNames) {
                                    if (matcherName.reset(eventTitle).find()) {
                                        foundName = matcherName.group(1);

                                        if (foundName != null) {

                                            //всё, что внутри скобок в имени - в должность
                                            int pStart = foundName.indexOf(Constants.STRING_PARENTHESIS_START);
                                            int pEnd = foundName.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                                            if (pStart > -1 && pEnd > pStart) {
                                                contactTitle = foundName.substring(pStart + 1, pEnd);
                                                foundName = foundName.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY);
                                            }

                                            String personFullNameNormalized;
                                            String personFullNameAltNormalized;
                                            final String personFullNameAlt = Person.getAltName(foundName, preferences_rules_calendars_nameformat, context);
                                            if (isFirstSecondLastFormat) {
                                                personFullNameNormalized = normalizeName(foundName);
                                                personFullNameAltNormalized = normalizeName(personFullNameAlt);
                                                userData.put(Position_personFullName, foundName);
                                                userData.put(Position_personFullNameAlt, personFullNameAlt);
                                            } else {
                                                personFullNameNormalized = normalizeName(personFullNameAlt);
                                                personFullNameAltNormalized = normalizeName(foundName);
                                                userData.put(Position_personFullName, personFullNameAlt);
                                                userData.put(Position_personFullNameAlt, foundName);
                                            }
                                            namedFromEvent = true;

                                            //Ищем контакт
                                            contactID = map_contacts_names.get(personFullNameNormalized);
                                            if (contactID == null && !personFullNameNormalized.equals(personFullNameAltNormalized)) {
                                                contactID = map_contacts_names.get(personFullNameAltNormalized);
                                            }
                                            if (contactID == null) {
                                                contactID = map_contacts_names.get(Person.getShortName(personFullNameNormalized, Constants.pref_List_NameFormat_FirstSecondLast, context));
                                            }
                                            if (contactID == null && !personFullNameNormalized.equals(personFullNameAltNormalized)) {
                                                contactID = map_contacts_names.get(Person.getShortName(personFullNameAltNormalized, Constants.pref_List_NameFormat_LastFirstSecond, context));
                                            }

                                            if (contactID != null) {

                                                if (contactTitle.isEmpty()) {
                                                    contactTitle = checkForNull(map_contacts_titles.get(contactID));
                                                }

                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (contactID != null) {
                                importMethod = importMethod_NewContactEvent;
                                userData.put(Position_contactID, contactID);
                                userData.put(Position_rawContactID, checkForNull(map_contacts_ids.get(contactID)));

                                //Ищем событие контакта в списке событий и добавляем в него
                                Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(contactID + Constants.STRING_2HASH + event.subType);
                                if (eventIndex != null && eventIndex <= eventList.size()) {
                                    List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                    final String eventDates = singleRowList.get(ContactsEvents.Position_dates);

                                    if (!eventDates.contains(eventNewDate)) { //Нет такой даты из такого источника
                                        singleRowList.set(ContactsEvents.Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
                                        singleRowList.set(ContactsEvents.Position_eventStorage, singleRowList.get(ContactsEvents.Position_eventStorage)
                                                + Constants.STRING_COMMA_SPACE + Constants.STRING_STORAGE_CALENDAR);
                                    } else {
                                        continue cursor_loop;
                                    }
                                    if (singleRowList.get(ContactsEvents.Position_eventID).isEmpty()) {
                                        singleRowList.set(ContactsEvents.Position_eventID, eventID);
                                    }

                                    if (!eventURLs.isEmpty()) {
                                        String eventURL_stored = checkForNull(singleRowList.get(ContactsEvents.Position_eventURL)).trim();
                                        StringBuilder sb = new StringBuilder(eventURL_stored);
                                        if (eventURL_stored.isEmpty()) {
                                            for (String url : eventURLs) {
                                                sb.append(url).append(Constants.STRING_2TILDA);
                                                statContactsURLCount++;
                                            }
                                            sb.delete(sb.length() - Constants.STRING_2TILDA.length(), sb.length());
                                        } else {
                                            for (String url : eventURLs) {
                                                if (!eventURL_stored.contains(url)) {
                                                    sb.append(Constants.STRING_2TILDA).append(url);
                                                    statContactsURLCount++;
                                                }
                                            }
                                        }
                                        singleRowList.set(ContactsEvents.Position_eventURL, sb.toString());
                                        map_events_weblinks.put(contactID, sb.toString());
                                    }

                                    if (!eventDescription.isEmpty()) {
                                        String eventDescription_stored = checkForNull(singleRowList.get(ContactsEvents.Position_eventDescription)).trim();
                                        if (eventDescription_stored.isEmpty()) {
                                            singleRowList.set(ContactsEvents.Position_eventDescription, eventDescription);
                                            map_notes.put(contactID, eventDescription);
                                        } else {
                                            final String eventDescription_new = eventDescription_stored.concat(Constants.STRING_SPACE).concat(eventDescription);
                                            singleRowList.set(ContactsEvents.Position_eventDescription, eventDescription_new);
                                            map_notes.put(contactID, eventDescription_new);
                                        }
                                    }

                                    String eventSource_stored = checkForNull(singleRowList.get(ContactsEvents.Position_eventSource)).trim();
                                    if (eventSource_stored.isEmpty()) {
                                        singleRowList.set(ContactsEvents.Position_eventSource, eventSource);
                                    } else if (!eventSource_stored.contains(eventSource)) {
                                        singleRowList.set(ContactsEvents.Position_eventSource, eventSource_stored.concat(Constants.STRING_2TILDA).concat(eventSource));
                                    }

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
                                    HashMap<String, String> contactDataMap = getContactDataMulti(parseToLong(contactID), new String[]{
                                            ContactsContract.Contacts.PHOTO_URI,
                                            ContactsContract.Data.DISPLAY_NAME,
                                            ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE,
                                            ContactsContract.Contacts.STARRED
                                    });

                                    userData.put(Position_photo_uri, checkForNull(contactDataMap.get(ContactsContract.Contacts.PHOTO_URI)));

                                    if (contactDataMap.containsKey(ContactsContract.Contacts.STARRED)) {
                                        if (Constants.STRING_1.equals(checkForNull(contactDataMap.get(ContactsContract.Contacts.STARRED)))) {
                                            userData.put(Position_starred, Constants.STRING_1);
                                            statFavoriteEventsCount++;
                                        }
                                    }

                                    if (!namedFromEvent) {
                                        String contactFIO = checkForNull(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME));
                                        userData.put(Position_personFullName, contactFIO);
                                        userData.put(Position_personFullNameAlt, checkForNull(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)).replace(Constants.STRING_COMMA, Constants.STRING_EMPTY));
                                    }
                                    userData.put(Position_title, contactTitle);
                                    userData.put(Position_organization, checkForNull(map_organizations.get(contactID)));
                                    userData.put(Position_nickname, checkForNull(map_contacts_aliases.get(contactID)));
                                    userData.put(Position_eventDescription, checkForNull(map_notes.get(contactID)));

                                    if (!eventURLs.isEmpty()) {
                                        StringBuilder sb = new StringBuilder();
                                        for (String url : eventURLs) {
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

                                userData.put(Position_eventCaption, event.caption); //Наименование события
                                userData.put(Position_eventID, eventID);
                                userData.put(Position_eventLabel, event.label); //Заголовок события
                                userData.put(Position_eventType, event.type); //Тип события
                                userData.put(Position_eventSubType, event.subType); //Подтип события
                                userData.put(Position_dates, eventNewDate);
                                userData.put(Position_eventIcon, Integer.toString(event.icon));
                                userData.put(Position_eventEmoji, event.emoji);
                                //todo: получается, что useYear больше не нужен, т.к. дата всегда с годом
                                userData.put(Position_eventDateThisTime, sdf_DDMMYYYY.format(dateStart.getTime()));
                                userData.put(Position_eventDateFirstTime, sdf_DDMMYYYY.format(dtStart));
                                userData.put(Position_eventSource, eventSource);
                                userData.put(Position_eventDescription, eventDescription);

                                if (importMethod == importMethod_Standalone) {
                                    if (!eventURLs.isEmpty()) {
                                        StringBuilder sb = new StringBuilder();
                                        for (String url : eventURLs) {
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

                                    if (importMethod == importMethod_NewContactEvent) {  //Добавляем событие
                                        if (!contactID.isEmpty()) {
                                            map_eventsBySubtypeAndPersonID_offset.put(contactID + Constants.STRING_2HASH + event.subType, eventList.size() - 1);
                                        }
                                    } else {
                                        map_eventsBySubtypeAndPersonID_offset.put(eventID + Constants.STRING_2HASH + event.subType, eventList.size() - 1);
                                    }
                                }
                            }
                            dateStart.add(Calendar.DATE, 1);
                        } while (dateStart.before(dateEnd) && dateStart.compareTo(endPeriod) <= 0);
                    }
                }
            }

            statCalendarsEventCount += statEventsCountByType;
            statEventsCount += statEventsCountByType;
            statTimeGetCalendarEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (SecurityException se) {
            return false;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            StringBuilder dataRow = new StringBuilder();
            if (!userData.isEmpty()) {
                for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                    dataRow.append(Constants.STRING_EOL);
                    dataRow.append(entry.getKey()).append("=");
                    dataRow.append(entry.getValue());
                }
            }
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + dataRow);
            return false;
        } finally {
            userData.clear();
            if (cursor != null) cursor.close();
        }
    }

    void recieveCalendarList() {

        Cursor cursor = null;

        try {

            if (checkNoCalendarAccess()) return;

            if (contentResolver == null) contentResolver = context.getContentResolver();
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            cursor = contentResolver.query(
                    uri,
                    new String[]{
                            android.provider.BaseColumns._ID,
                            CalendarContract.Calendars.VISIBLE,
                            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                            CalendarContract.Calendars.ACCOUNT_NAME
                    },
                    null,
                    null,
                    null);

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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void initNotifications() {
        //https://stackoverflow.com/questions/51343550/how-to-give-notifications-on-android-on-specific-time-in-android-oreo/51645875#51645875

        try {
            StringBuilder log = new StringBuilder();

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                log.append(context.getString(R.string.msg_notifications_disabled));
            } else {
                initNotificationChannel(log); //для Android 8+
                initBootReceiver(log);
                initNotificationSchedule(log);
            }
            initWidgetUpdate(log);

            if (log.length() > 0) ToastExpander.showDebugMsg(context, log.toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    static class Event {
        String caption = Constants.STRING_EMPTY;
        String label = Constants.STRING_EMPTY;
        String type = Constants.STRING_EMPTY;
        String subType = Constants.STRING_EMPTY;
        @DrawableRes int icon = 0;
        String emoji = Constants.STRING_EMPTY;
        Date date;
        String distance;
        boolean needScanContacts = false;

        public Event() {
        }

        public Event(Date date, String distance) {
            this.date = date;
            this.distance = distance;
        }

    }

    static class DayType {
        enum Type {
            Holiday, Workday
        }
        final String sourceId;
        final Type type;

        public DayType(String sourceId, Type type) {
            this.sourceId = sourceId;
            this.type = type;
        }
    }

    private boolean getFileEvents(@NonNull String eventType) {

        try {

            long statCurrentModuleStart = System.currentTimeMillis();

            @Nullable Event event = null;

            TreeMap<Integer, String> userData = new TreeMap<>();
            Set<String> fileList;
            Calendar now = new GregorianCalendar();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            final boolean isFirstSecondLastFormat = Integer.toString(preferences_rules_files_nameformat).equals(context.getString(R.string.pref_List_NameFormat_FirstSecondLast));

            boolean isMultiTypeSource = eventType.equals(Constants.Type_MultiEvent);
            if (eventType.equals(getEventType(Constants.Type_BirthDay))) {

                fileList = preferences_Birthday_files;

                event = new Event();
                event.caption = getResources().getString(R.string.event_type_birthday);
                event.type = eventType;
                event.subType = getEventType(Constants.Type_BirthDay);
                event.icon = R.drawable.ic_event_birthday;
                event.emoji = getResources().getString(R.string.event_type_birthday_emoji);
                event.needScanContacts = true;

            } else if (eventType.equals(getEventType(Constants.Type_Other))) {

                fileList = preferences_OtherEvent_files;

                event = new Event();
                event.caption = getResources().getString(R.string.event_type_other);
                event.type = eventType;
                event.subType = getEventType(Constants.Type_FileEvent);
                event.icon = R.drawable.ic_event_other;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                event.needScanContacts = false;

            } else if (eventType.equals(getEventType(Constants.Type_HolidayEvent))) {

                fileList = preferences_HolidayEvent_files;

                event = new Event();
                event.caption = getResources().getString(R.string.event_type_holiday);
                event.type = eventType;
                event.subType = eventType; //getEventType(Constants.Type_FileEvent);
                event.icon = R.drawable.ic_event_holiday;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_holiday_emoji) : "\uD83C\uDFD6️";
                event.needScanContacts = false;

            } else if (isMultiTypeSource) {

                fileList = preferences_MultiType_files;

            } else {
                return false;
            }
            if (fileList == null || fileList.isEmpty()) return false;

            for (String file : fileList) {

                String[] fileDetails = file.split(Constants.STRING_PIPE);
                String[] eventsArray =  readFileToString(file, Constants.STRING_EOL).split(Constants.STRING_EOL, -1);
                if (eventsArray[0].isEmpty()) {
                    ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                    continue;
                }

                String fileName = fileDetails[0].lastIndexOf(Constants.STRING_SLASH) > -1 ? fileDetails[0].substring(fileDetails[0].lastIndexOf(Constants.STRING_SLASH) + 1) : fileDetails[0];
                final String eventSource = !fileName.isEmpty() ? getResources().getString(R.string.msg_file_info, fileName) :
                        getResources().getString(R.string.event_type_file);
                int indexFileNameEnd = file.indexOf(Constants.STRING_BAR);
                if (indexFileNameEnd < 0) indexFileNameEnd = 0;

                for (String eventRow : eventsArray) {

                    String eventLine = eventRow.trim().replace("\uFEFF", Constants.STRING_EMPTY);
                    if (eventLine.isEmpty() || eventLine.startsWith(Constants.STRING_HASH) || eventLine.startsWith(Constants.STRING_DSLASH))
                        continue;

                    if (!userData.isEmpty()) userData.clear();

                    String eventLabel_forSearch = Constants.STRING_EMPTY;
                    String eventTitle = Constants.STRING_EMPTY;
                    String eventDateString = Constants.STRING_EMPTY;
                    boolean useEventYear = true;
                    @Nullable Date dateEvent = null;
                    String eventNewDate;
                    @Nullable String contactID = null;
                    String eventURL = Constants.STRING_EMPTY;
                    boolean isEndless = true;
                    boolean isAD = true;

                    int indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                    boolean isBirthdaysPlusEvent = eventLine.startsWith(Constants.STRING_BDP_DIV)
                            && eventLine.endsWith(Constants.STRING_BDP_EOL);

                    //todo: сделать поддержку дат до 1900 http://rsdn.org/forum/java/981164.all
                    //BirthdayPro, DarkBirthday: <Дата без пробелов>[,<пробел>флаги] название праздника или ФИО [(должность)]
                    if (!isBirthdaysPlusEvent) {

                        if (indexFirstSpace > -1) {

                            final int indexComma = eventLine.indexOf(Constants.STRING_COMMA);
                            if (indexComma > -1 && indexComma < indexFirstSpace) { //Есть флаги

                                if (indexFirstSpace - indexComma == 1) { //После запятой пробел - убираем
                                    eventLine = eventLine.substring(0, indexComma + 1) + eventLine.substring(indexFirstSpace + 1);
                                    indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                                    if (indexFirstSpace == -1) {
                                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventRow));
                                        continue;
                                    }
                                }

                                eventDateString = eventLine.substring(0, indexComma);
                                String flags = eventLine.substring(indexComma + 1, indexFirstSpace);
                                eventTitle = eventLine.substring(indexFirstSpace + 1).trim();

                                if (!flags.isEmpty()) {
                                    if (flags.contains(Constants.STRING_1)) {
                                        isEndless = false;
                                        flags = flags.replace(Constants.STRING_1, Constants.STRING_EMPTY);
                                    }
                                    if (flags.contains(Constants.STRING_BC)) {
                                        isAD = false;
                                        flags = flags.replace(Constants.STRING_BC, Constants.STRING_EMPTY);
                                    }
                                    if (isMultiTypeSource) {
                                        eventLabel_forSearch = flags.replace(Constants.STRING_UNDERSCORE, Constants.STRING_SPACE);
                                    }
                                }

                            } else {

                                eventDateString = eventLine.substring(0, indexFirstSpace);
                                eventTitle = eventLine.substring(indexFirstSpace + 1).trim();

                            }
                        }

                    } else { //Birthdays Plus: ❙ДДДД-ММ-ДД❙ИОФ❙тип (Birthday, Anniversary, Custom)❙наименование события или null❚

                        final String[] eventBDPdetails = eventLine.split(Constants.STRING_BDP_DIV, -1);

                        if (eventBDPdetails.length == 5) {

                            eventDateString = eventBDPdetails[1];
                            eventTitle = eventBDPdetails[2];
                            if (eventBDPdetails[3].equals(Constants.STRING_BDP_CUSTOM)) {
                                eventLabel_forSearch = eventBDPdetails[4].replace(Constants.STRING_BDP_EOL, Constants.STRING_EMPTY);
                            } else {
                                eventLabel_forSearch = eventBDPdetails[3];
                            }

                        }

                    }

                    if (eventDateString.isEmpty()) {
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventRow));
                        continue;
                    }

                    if (isMultiTypeSource) {
                        event = recognizeEventByLabel(eventLabel_forSearch, Constants.Storage_File, eventTitle);
                    }

                    if (preferences_rules_unrecognized == Rules_Unrecognized_Skip && (event == null || event.icon == R.drawable.ic_event_unknown)) {
                        continue;
                    }

                    int indexDateNoYear = isBirthdaysPlusEvent ? eventDateString.indexOf(Constants.STRING_BDP_NO_YEAR) : eventDateString.indexOf(Constants.STRING_0000);
                    if (indexDateNoYear == -1) { //С годом
                        try {
                            if (isAD) {

                                if (!isBirthdaysPlusEvent) {
                                    String dateNextFloatingEvent = computeFloatingDate(eventDateString);
                                    if (!eventDateString.equals(dateNextFloatingEvent)) {
                                        Date eventDateFirstTime = sdf_DDMMYYYY.parse(dateNextFloatingEvent); //Пытаемся определить год первоначального события
                                        if (eventDateFirstTime != null) {
                                            try {
                                                eventDateFirstTime.setYear(Integer.parseInt(eventDateString.substring(eventDateString.lastIndexOf(Constants.STRING_PERIOD) + 1)) - 1900);
                                                userData.put(Position_eventDateFirstTime, sdf_DDMMYYYY.format(eventDateFirstTime));
                                                userData.put(Position_eventDateThisTime, dateNextFloatingEvent);
                                            } catch (NumberFormatException ignored) { /**/ }
                                        }
                                        eventDateString = dateNextFloatingEvent;
                                    }
                                }

                                dateEvent = sdf_DDMMYYYY.parse(eventDateString);
                            } else {
                                dateEvent = sdf_DDMMYYYY_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                            }
                        } catch (ParseException e1) {
                            try {
                                if (isAD) {
                                    dateEvent = sdf_india.parse(eventDateString);
                                } else {
                                    dateEvent = sdf_india_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                }
                            } catch (ParseException e2) {
                                try {
                                    if (isAD) {
                                        dateEvent = sdf_uk.parse(eventDateString);
                                    } else {
                                        dateEvent = sdf_uk_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                    }
                                } catch (ParseException e3) {
                                    try {
                                        if (isAD) {
                                            dateEvent = sdf_java.parse(eventDateString);
                                        } else {
                                            dateEvent = sdf_java_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                        }
                                    } catch (ParseException e4) {
                                        //Не получилось распознать
                                    }
                                }
                            }
                        }
                        if (dateEvent != null && !isEndless && now.after(getCalendarFromDate(dateEvent)))
                            continue; //Одиночное событие и оно прошло

                    } else { //Без года

                        useEventYear = false;
                        String dateNextEvent = eventDateString.substring(0, indexDateNoYear) + now.get(Calendar.YEAR);
                        try {
                            if (!isBirthdaysPlusEvent) {
                                String dateNextFloatingEvent = computeFloatingDate(dateNextEvent);
                                if (!dateNextEvent.equals(dateNextFloatingEvent)) {
                                    userData.put(Position_eventDateFirstTime, dateNextFloatingEvent.substring(0, dateNextFloatingEvent.lastIndexOf(Constants.STRING_PERIOD)));
                                    userData.put(Position_eventDateThisTime, dateNextFloatingEvent);
                                    dateNextEvent = dateNextFloatingEvent;
                                }
                            }
                            dateEvent = sdf_DDMMYYYY.parse(dateNextEvent);
                        } catch (ParseException e1) {
                            try {
                                dateEvent = sdf_india.parse(dateNextEvent);
                            } catch (ParseException e2) {
                                try {
                                    dateEvent = sdf_uk.parse(dateNextEvent);
                                } catch (ParseException e3) {
                                    try {
                                        dateNextEvent = eventDateString.replace(Constants.STRING_BDP_NO_YEAR, Integer.toString(now.get(Calendar.YEAR)));
                                        dateEvent = sdf_java.parse(dateNextEvent);
                                    } catch (ParseException e4) {
                                        //Не получилось распознать
                                    }
                                }
                            }
                        }
                        if (dateEvent != null && now.after(getCalendarFromDate(dateEvent)))
                            dateEvent = addYear(dateEvent, 1);
                    }
                    if (dateEvent == null || event == null) {
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventRow));
                        continue;
                    }

                    eventNewDate = Constants.EVENT_PREFIX_FILE_EVENT + Constants.STRING_COLON_SPACE
                            + (useEventYear ? isAD ? sdf_java.format(dateEvent) : sdf_java_G.format(dateEvent) : sdf_java_no_year.format(dateEvent))
                            + Constants.STRING_COLON_SPACE
                            + getHash((isMultiTypeSource ? Constants.eventSourceMultiFilePrefix : Constants.eventSourceFilePrefix) + file);

                    userData.put(Position_eventStorage, Constants.STRING_STORAGE_FILE);
                    userData.put(Position_eventCaption, event.caption);
                    userData.put(Position_eventLabel, event.label);
                    userData.put(Position_eventSource, eventSource);
                    userData.put(Position_eventType, event.type);
                    userData.put(Position_eventSubType, event.subType);
                    userData.put(Position_dates, eventNewDate);
                    userData.put(Position_eventIcon, Integer.toString(event.icon));
                    userData.put(Position_eventEmoji, event.emoji);

                    //URLs
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
                        eventURL = substringBefore(eventURL, Constants.STRING_SPACE);
                        userData.put(Position_eventURL, eventURL);
                        eventTitle = eventTitle.replace(eventURL, Constants.STRING_EMPTY).trim();
                        statContactsURLCount++;
                    }

                    //Description
                    int indStartDescription = eventTitle.indexOf(Constants.STRING_BAR);
                    if (indStartDescription > -1) {
                        userData.put(Position_eventDescription, eventTitle.substring(indStartDescription + 1).trim());
                        eventTitle = eventTitle.substring(0, indStartDescription).trim();
                    }

                    if (event.needScanContacts) { //Нужно искать в контактах

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

                        String personFullNameNormalized;
                        String personFullNameAltNormalized;
                        final String personFullNameAlt = Person.getAltName(eventTitle, preferences_rules_files_nameformat, context);
                        if (isFirstSecondLastFormat) {
                            personFullNameNormalized = normalizeName(eventTitle);
                            personFullNameAltNormalized = normalizeName(personFullNameAlt);
                            userData.put(Position_personFullName, eventTitle);
                            userData.put(Position_personFullNameAlt, personFullNameAlt);
                        } else {
                            personFullNameNormalized = normalizeName(personFullNameAlt);
                            personFullNameAltNormalized = normalizeName(eventTitle);
                            userData.put(Position_personFullName, personFullNameAlt);
                            userData.put(Position_personFullNameAlt, eventTitle);
                        }

                        //Ищем контакт
                        contactID = map_contacts_names.get(personFullNameNormalized);
                        if (TextUtils.isEmpty(contactID) && !personFullNameNormalized.equals(personFullNameAltNormalized)) {
                            contactID = map_contacts_names.get(personFullNameAltNormalized);
                        }
                        if (TextUtils.isEmpty(contactID)) {
                            contactID = map_contacts_names.get(Person.getShortName(personFullNameNormalized, Constants.pref_List_NameFormat_FirstSecondLast, context));
                        }
                        if (TextUtils.isEmpty(contactID) && !personFullNameNormalized.equals(personFullNameAltNormalized)) {
                            contactID = map_contacts_names.get(Person.getShortName(personFullNameAltNormalized, Constants.pref_List_NameFormat_LastFirstSecond, context));
                        }

                        //hashCode -> ID
                        if (TextUtils.isEmpty(contactID)) {
                            contactID = Constants.PREFIX_FileEventID + getHash(file.substring(indexFileNameEnd) + eventTitle);
                        }

                    } else { //Просто событие

                        userData.put(Position_personFullName, eventTitle);
                        userData.put(Position_personFullNameAlt, eventTitle);
                        userData.put(Position_eventID, Constants.PREFIX_FileEventID + getHash(file.substring(indexFileNameEnd) + eventTitle));

                    }

                    //Проверка по организации, что нашли именно требуемый контакт
                    String orgNameFile = Constants.STRING_EMPTY;
                    String titleFile = Constants.STRING_EMPTY;
                    if (contactID != null) {
                        orgNameFile = checkForNull(userData.get(Position_organization)).trim().toLowerCase();
                        titleFile = checkForNull(userData.get(Position_title)).trim();
                        String orgNameContact = checkForNull(map_organizations.get(contactID)).trim().toLowerCase();

                        //Организации не совпадают
                        if (!orgNameContact.isEmpty() && !orgNameFile.isEmpty() && !orgNameContact.contains(orgNameFile)) {
                            contactID = null;
                            userData.put(Position_eventID, Constants.PREFIX_FileEventID + getHash(file.substring(indexFileNameEnd) + eventTitle));
                        }
                    }

                    if (!TextUtils.isEmpty(contactID)) {
                        userData.put(Position_contactID, contactID);
                        userData.put(Position_rawContactID, checkForNull(map_contacts_ids.get(contactID)));

                        //Ищем событие контакта в списке событий и добавляем в него
                        Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(contactID + Constants.STRING_2HASH + event.subType);
                        if (eventIndex != null && eventIndex <= eventList.size()) {

                            List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                            final String eventDates = singleRowList.get(ContactsEvents.Position_dates);
                            boolean needUpdate = false;

                            if (!eventDates.contains(eventNewDate)) { //Пропускаем дубли
                                singleRowList.set(ContactsEvents.Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
                                singleRowList.set(ContactsEvents.Position_eventStorage, singleRowList.get(ContactsEvents.Position_eventStorage)
                                        + Constants.STRING_COMMA_SPACE + Constants.STRING_STORAGE_FILE);
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

                            String eventSource_stored = checkForNull(singleRowList.get(ContactsEvents.Position_eventSource)).trim();
                            if (eventSource_stored.isEmpty()) {
                                singleRowList.set(ContactsEvents.Position_eventSource, eventSource);
                                needUpdate = true;
                            } else if (!eventSource_stored.contains(eventSource)) {
                                singleRowList.set(ContactsEvents.Position_eventSource, eventSource_stored.concat(Constants.STRING_2TILDA).concat(eventSource));
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
                            if (isRealContactEventID(contactID)) {
                                final Long contactIDLong = parseToLong(contactID);
                                HashMap<String, String> contactDataMap = getContactDataMulti(contactIDLong, new String[]{
                                        ContactsContract.Contacts.PHOTO_URI,
                                        //ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE,
                                        ContactsContract.Contacts.STARRED
                                });

                                userData.put(Position_photo_uri, contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));
                                /*if (contactDataMap.containsKey(ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE) && !userData.containsKey(Position_personFullNameAlt)) {
                                    userData.put(Position_personFullNameAlt, checkForNull(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)).replace(Constants.STRING_COMMA, Constants.STRING_EMPTY));
                                }*/
                                if (contactDataMap.containsKey(ContactsContract.Contacts.STARRED)) {
                                    if (Constants.STRING_1.equals(checkForNull(contactDataMap.get(ContactsContract.Contacts.STARRED)))) {
                                        userData.put(Position_starred, Constants.STRING_1);
                                        statFavoriteEventsCount++;
                                    }
                                }
                                contactDataMap.clear();
                            }

                            userData.put(Position_nickname, checkForNull(map_contacts_aliases.get(contactID)));
                            if (TextUtils.isEmpty(userData.get(Position_organization)))
                                userData.put(Position_organization, checkForNull(map_organizations.get(contactID)));
                            if (TextUtils.isEmpty(userData.get(Position_title)))
                                userData.put(Position_title, checkForNull(map_contacts_titles.get(contactID)));

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
                            if (contactID != null && !contactID.isEmpty()) {
                                map_eventsBySubtypeAndPersonID_offset.put(contactID + Constants.STRING_2HASH + event.subType, eventList.size() - 1);
                            }
                        }
                        userData.clear();
                    }
                }
            }

            statTimeGetFileEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    @NonNull private String computeFloatingDate(String eventDateString) {

        try {

            String[] eventDateComponents = eventDateString.split(Constants.REGEX_PERIOD, -1);
            final String eventDayString = eventDateComponents[0].toLowerCase();
            Calendar now = Calendar.getInstance();
            Calendar cal = Calendar.getInstance();
            int eventMonth;
            int eventYear = now.get(Calendar.YEAR);

            //Именные события

            if (eventDateComponents.length == 2 && eventDayString.startsWith(eventNameEaster)) {

                //Православная Пасха

                //Определяем смещение в днях
                int daysShift = 0;
                String strAfterEventName = eventDayString.substring(eventNameEaster.length());
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
                    if (cal.before(now)) { //В этом году уже прошло, берём следующий год
                        cal = getEasterDateFor(eventYear + 1, true);
                        if (cal != null) {
                            cal.add(Calendar.DAY_OF_YEAR, daysShift);
                            return sdf_DDMMYYYY.format(cal.getTime());
                        }
                    } else {
                        return sdf_DDMMYYYY.format(cal.getTime());
                    }
                }

            } else if (eventDateComponents.length == 2 && eventDayString.startsWith(eventNameCatholicEaster)) {

                //Католическая Пасха

                //Определяем смещение в днях
                int daysShift = 0;
                String strAfterEventName = eventDayString.substring(eventNameCatholicEaster.length());
                if (strAfterEventName.startsWith(Constants.STRING_PLUS)) {
                    try {
                        daysShift = Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_PLUS) + 1));
                    } catch (NumberFormatException ignored) { /**/ }
                } else if (strAfterEventName.startsWith(Constants.STRING_MINUS)) {
                    try {
                        daysShift = - Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_MINUS) + 1));
                    } catch (NumberFormatException ignored) { /**/ }
                }

                cal = getEasterDateFor(eventYear, false);
                if (cal != null) {
                    cal.add(Calendar.DAY_OF_YEAR, daysShift);
                    if (cal.before(now)) { //В этом году уже прошло, берём следующий год
                        cal = getEasterDateFor(eventYear + 1, false);
                        if (cal != null) {
                            cal.add(Calendar.DAY_OF_YEAR, daysShift);
                            return sdf_DDMMYYYY.format(cal.getTime());
                        }
                    } else {
                        return sdf_DDMMYYYY.format(cal.getTime());
                    }
                }

            } else if (eventDateComponents.length == 2 && eventDayString.startsWith(eventNameNY)) {

                    //XX день от начала года

                    //Определяем смещение в днях
                    int daysShift = 0;
                    String strAfterEventName = eventDayString.substring(eventNameNY.length());
                    if (strAfterEventName.startsWith(Constants.STRING_PLUS)) {
                        try {
                            daysShift = Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_PLUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    } else if (strAfterEventName.startsWith(Constants.STRING_MINUS)) {
                        try {
                            daysShift = - Integer.parseInt(strAfterEventName.substring(strAfterEventName.indexOf(Constants.STRING_MINUS) + 1));
                        } catch (NumberFormatException ignored) { /**/ }
                    }

                    cal.set(eventYear, Calendar.JANUARY, 1);
                    cal.add(Calendar.DAY_OF_YEAR, daysShift);

                    if (cal.before(now)) { //В этом году уже прошло, берём следующий год
                        cal.set(eventYear + 1, Calendar.JANUARY, 1);
                        cal.add(Calendar.DAY_OF_YEAR, daysShift);
                    }

                    return sdf_DDMMYYYY.format(cal.getTime());
            }

            //NWW[+-OFFSET].ММ.ГГГГ
            if (eventDateComponents.length < 3) return eventDateString;

            try {
                eventMonth = Integer.parseInt(eventDateComponents[1]);
                if (eventMonth < 1 || eventMonth > 12) { return eventDateString; }
            } catch (NumberFormatException ignored) { return eventDateString; }

            //Определяем день недели
            int weekDayToGet = 0;
            int indexWeekDay = -1;
            for (int i = 1; i <= weekDaysShort.length; i++) {
                String weekDayName = weekDaysShort[i - 1].toLowerCase();
                if (eventDayString.contains(weekDayName)) {
                    weekDayToGet = i - 1;
                    if (weekDayToGet == 0) weekDayToGet = 7;
                    indexWeekDay = eventDayString.indexOf(weekDayName);
                    break;
                }
            }
            if (weekDayToGet == 0) return eventDateString;

            //Определяем смещение в днях
            int daysShift = 0;
            String strAfterWeekName = eventDayString.substring(indexWeekDay + 2);
            if (strAfterWeekName.startsWith(Constants.STRING_PLUS)) {
                try {
                    daysShift = Integer.parseInt(strAfterWeekName.substring(strAfterWeekName.indexOf(Constants.STRING_PLUS) + 1));
                } catch (NumberFormatException ignored) { /**/ }
            } else if (strAfterWeekName.startsWith(Constants.STRING_MINUS)) {
                try {
                    daysShift = - Integer.parseInt(strAfterWeekName.substring(strAfterWeekName.indexOf(Constants.STRING_MINUS) + 1));
                } catch (NumberFormatException ignored) { /**/ }
            }

            String weekDayNumberString = eventDayString.substring(0, indexWeekDay);

            if (weekDayNumberString.equalsIgnoreCase(Constants.STRING_Z)) { //Последняя неделя

                cal.set(eventYear, eventMonth, 1);
                int weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                int daysToSub = weekDayStartOfNextMonth >= weekDayToGet ? weekDayStartOfNextMonth - weekDayToGet : 7 - (weekDayToGet - weekDayStartOfNextMonth);
                cal.add(Calendar.DAY_OF_MONTH, - daysToSub + daysShift);

                if (cal.before(now)) { //В этом году уже прошло, берём следующий год
                    cal.set(eventYear + 1, eventMonth, 1);
                    weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                    daysToSub = weekDayStartOfNextMonth >= weekDayToGet ? weekDayStartOfNextMonth - weekDayToGet : 7 - (weekDayToGet - weekDayStartOfNextMonth);
                    cal.add(Calendar.DAY_OF_MONTH, - daysToSub + daysShift);
                }

            } else if (weekDayNumberString.equalsIgnoreCase(Constants.STRING_Y)) { //Предпоследняя неделя

                cal.set(eventYear, eventMonth, 1);
                int weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                int daysToSub = weekDayStartOfNextMonth >= weekDayToGet ? 7 + weekDayStartOfNextMonth - weekDayToGet : 14 - (weekDayToGet - weekDayStartOfNextMonth);
                cal.add(Calendar.DAY_OF_MONTH, - daysToSub + daysShift);

                if (cal.before(now)) { //В этом году уже прошло, берём следующий год
                    cal.set(eventYear + 1, eventMonth, 1);
                    weekDayStartOfNextMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    if (weekDayStartOfNextMonth == 0) weekDayStartOfNextMonth = 7;
                    daysToSub = weekDayStartOfNextMonth >= weekDayToGet ? 7 + weekDayStartOfNextMonth - weekDayToGet : 14 - (weekDayToGet - weekDayStartOfNextMonth);
                    cal.add(Calendar.DAY_OF_MONTH, - daysToSub + daysShift);
                }

            } else {

                int weekNumberToGet;
                try {
                    weekNumberToGet = Integer.parseInt(weekDayNumberString); //Номер недели 1..5
                    if (weekNumberToGet < 1 || weekNumberToGet > 5) { return eventDateString; }
                } catch (NumberFormatException ignored) { return eventDateString; }
                cal.set(eventYear, eventMonth - 1, 1);
                int weekDayStartOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDayStartOfMonth == 0) weekDayStartOfMonth = 7;
                int daysToAdd = weekDayStartOfMonth <= weekDayToGet ? weekDayToGet - weekDayStartOfMonth + 7 * (weekNumberToGet - 1) : 7 - (weekDayStartOfMonth - weekDayToGet) + 7 * (weekNumberToGet - 1);
                cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
                if (weekNumberToGet == 5 && (cal.get(Calendar.MONTH)) != eventMonth - 1) cal.add(Calendar.DAY_OF_MONTH, -7);
                cal.add(Calendar.DAY_OF_MONTH, daysShift);

                if (cal.before(now)) { //В этом году уже прошло, берём следующий год
                    cal.set(eventYear + 1, eventMonth - 1, 1);
                    weekDayStartOfMonth = cal.get(Calendar.DAY_OF_WEEK) - 1;
                    if (weekDayStartOfMonth == 0) weekDayStartOfMonth = 7;
                    daysToAdd = weekDayStartOfMonth <= weekDayToGet ? weekDayToGet - weekDayStartOfMonth + 7 * (weekNumberToGet - 1) : 7 - (weekDayStartOfMonth - weekDayToGet) + 7 * (weekNumberToGet - 1);
                    cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
                    if (weekNumberToGet == 5 && (cal.get(Calendar.MONTH)) != eventMonth - 1) cal.add(Calendar.DAY_OF_MONTH, -7);
                    cal.add(Calendar.DAY_OF_MONTH, daysShift);
                }


            }

            return sdf_DDMMYYYY.format(cal.getTime());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return eventDateString;
        }
    }

    private Calendar getEasterDateFor(float Y, boolean getOrthodox) {
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return null;
        }
    }

    @NonNull
    private Event recognizeEventByLabel(@NonNull String eventLabel, int eventSource, @NonNull String eventSubject) {

        String eventLabel_forSearch = eventLabel.toLowerCase();
        final boolean isEmptyLabel = eventLabel_forSearch.isEmpty();
        Event event = new Event();
        event.label = eventLabel;

        try {

            if (!isEmptyLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_birthday);
                event.type = getEventType(Constants.Type_BirthDay);
                event.subType = getEventType(Constants.Type_BirthDay);
                event.icon = R.drawable.ic_event_birthday;
                event.emoji = getResources().getString(R.string.event_type_birthday_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_other_event_labels != null && preferences_other_event_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_other);
                event.type = getEventType(Constants.Type_Other);
                event.icon = R.drawable.ic_event_other;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                event.needScanContacts = false;

            } else if (!isEmptyLabel && preferences_holiday_event_labels != null && preferences_holiday_event_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_holiday);
                event.type = getEventType(Constants.Type_HolidayEvent);
                event.icon = R.drawable.ic_event_holiday;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_holiday_emoji) : "\uD83C\uDFD6️";
                event.needScanContacts = false;

            } else if (!isEmptyLabel && preferences_death_labels != null && preferences_death_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_death);
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Death);
                event.icon = R.drawable.ic_event_death;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_death_emoji) : "\uD83D\uDCC5";
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_anniversary);
                event.type = getEventType(Constants.Type_Anniversary);
                event.subType = getEventType(Constants.Type_Anniversary);
                event.icon = R.drawable.ic_event_wedding;
                event.emoji = getResources().getString(R.string.event_type_wedding_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_nameday);
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_NameDay);
                event.icon = R.drawable.ic_event_nameday;
                event.emoji = getResources().getString(R.string.event_type_nameday_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel_forSearch).find()) {

                event.caption = getResources().getString(R.string.event_type_crowning);
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Crowning);
                event.icon = R.drawable.ic_event_crowning;
                event.emoji = getResources().getString(R.string.event_type_crowning_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel_forSearch).find()) {

                event.caption = preferences_customevent1_caption;
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Custom1);
                event.icon = R.drawable.ic_event_custom1;
                event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel_forSearch).find()) {

                event.caption = preferences_customevent2_caption;
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Custom2);
                event.icon = R.drawable.ic_event_custom2;
                event.emoji = getResources().getString(R.string.event_type_custom2_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel_forSearch).find()) {

                event.caption = preferences_customevent3_caption;
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Custom3);
                event.icon = R.drawable.ic_event_custom3;
                event.emoji = getResources().getString(R.string.event_type_custom3_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel_forSearch).find()) {

                event.caption = preferences_customevent4_caption;
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Custom4);
                event.icon = R.drawable.ic_event_custom4;
                event.emoji = getResources().getString(R.string.event_type_custom4_emoji);
                event.needScanContacts = true;

            } else if (!isEmptyLabel && preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel_forSearch).find()) {

                event.caption = preferences_customevent5_caption;
                event.type = getEventType(Constants.Type_Custom);
                event.subType = getEventType(Constants.Type_Custom5);
                event.icon = R.drawable.ic_event_custom5;
                event.emoji = getResources().getString(R.string.event_type_custom5_emoji);
                event.needScanContacts = true;

            } else {

                if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Other) {

                    event.caption = getResources().getString(R.string.event_type_other);
                    event.type = getEventType(Constants.Type_Other);
                    event.icon = R.drawable.ic_event_other;
                    event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                    event.needScanContacts = false;

                } else if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Unrecognized) {

                    event.caption = getResources().getString(R.string.event_type_unrecognized);
                    event.type = getEventType(Constants.Type_Unrecognized);
                    event.icon = R.drawable.ic_event_unknown;
                    event.emoji = getResources().getString(R.string.event_type_unknown_emoji);
                    event.needScanContacts = false;

                    ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_type_parse_error, eventLabel, eventSubject));

                } else {

                    event.type = getEventType(Constants.Type_Unrecognized);
                    event.icon = R.drawable.ic_event_unknown;

                }

            }

            if (event.subType.isEmpty()) {
                if (eventSource == Constants.Storage_Calendar) {
                    event.subType = getEventType(Constants.Type_CalendarEvent);
                } else if (eventSource == Constants.Storage_File) {
                    event.subType = getEventType(Constants.Type_FileEvent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return event;
    }

    @NonNull
    String readFileToString(@NonNull String file, String delimeter) {

        StringBuilder sb = new StringBuilder();

        try {

            String[] fileDetails = file.split(Constants.STRING_PIPE);
            Uri uri = null;
            if (contentResolver == null) contentResolver = context.getContentResolver();
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
                        sb.append(line);
                        if (delimeter != null) sb.append(delimeter);
                        line = reader.readLine();
                    }
                    if (inputStream != null) inputStream.close();
                } catch (java.lang.SecurityException se) {
                    ToastExpander.showDebugMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0] + Constants.STRING_COMMA_SPACE +
                            se.getMessage());
                } catch (Exception e) {
                    ToastExpander.showDebugMsg(context, resources.getString(R.string.msg_file_access_read_error, fileDetails[0]) + Constants.STRING_COMMA_SPACE +
                            e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return sb.toString();
    }

    Bitmap getEventPhoto(@NonNull String event, boolean showPhotos, boolean suggestSquared, boolean forWidget, int roundingFactor) {

        boolean makeSquared = suggestSquared;
        boolean addMourningTape = false;
        Bitmap bm = null;

        try {

            String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
            String eventType = singleEventArray[Position_eventType];
            String eventSubType = singleEventArray[Position_eventSubType];

            if (eventType.equals(getEventType(Constants.Type_Unrecognized))) {

                bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_unknown);

            } else if (eventSubType.equals(getEventType(Constants.Type_CalendarEvent)) || eventSubType.equals(getEventType(Constants.Type_FileEvent))
                    && TextUtils.isEmpty(singleEventArray[Position_photo_uri])) {

                bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_other);

            } else if (eventSubType.equals(getEventType(Constants.Type_HolidayEvent))) {

                bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_holiday);

                //todo: https://stackoverflow.com/questions/77168650/draw-emoji-to-image-in-android
                //https://stackoverflow.com/questions/41212092/drawing-emojis-on-android-canvas-using-unicode-values
                //https://stackoverflow.com/questions/47807621/draw-emoji-on-bitmap-with-drawtextonpath

            } else {

                @NonNull String contactID = checkForNull(singleEventArray[Position_contactID]);

                addMourningTape = (preferences_list_sad_photo == 1 && eventSubType.equals(getEventType(Constants.Type_Death))) ||
                        (preferences_list_sad_photo == 2 && deathDatesForIds.containsKey(contactID));

                if (showPhotos && !contactID.isEmpty() && !TextUtils.isEmpty(singleEventArray[Position_photo_uri]) && !singleEventArray[Position_photo_uri].equalsIgnoreCase(Constants.STRING_NULL)) {
                    //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    if (contentResolver == null) contentResolver = context.getContentResolver();
                    Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactID);
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
                    if (!eventSubType.equals(getEventType(Constants.Type_BirthDay)) && !contactID.isEmpty() && birthdayDatesForIds.containsKey(contactID)) {
                        Date birthDate = birthdayDatesForIds.get(contactID);
                        Date BDay = null;
                        try {
                            BDay = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateThisTime]);
                        } catch (ParseException e) { /**/ }

                        List<String> singleRowList = Arrays.asList(singleEventArray);
                        if (birthDate != null && BDay != null) {
                            final int countYearsDiff = countYearsDiff(birthDate, BDay);
                            if (countYearsDiff > 0) {
                                singleRowList.set(Position_age, String.valueOf(countYearsDiff));
                            } else {
                                //если день рождения без года - мы об этом никак не узнаем
                                singleRowList.set(Position_age, Constants.STRING_MINUS1);
                            }
                            singleEventArray = singleRowList.toArray(new String[0]);
                        }
                    }

                    //Случайное фото с соответствии с возрастом и полом
                    Person person = new Person(context, singleEventArray);
                    int gender = person.getGender();

                    //По-умолчанию
                    Integer idPhoto = R.drawable.ic_pack00_m1;
                    if (gender == 2 && preferences_IconPackImages_F.get(0) != null) {
                        idPhoto = preferences_IconPackImages_F.get(0);
                    } else if (preferences_IconPackImages_M.get(0) != null) {
                        idPhoto = preferences_IconPackImages_M.get(0);
                    }

                    //Если определён возраст
                    if (person.Age >= 0) {
                        if (gender == 2) {
                            for (Map.Entry<Integer, Integer> entry : preferences_IconPackImages_F.entrySet()) {
                                int beforeAge = entry.getKey();
                                if (beforeAge > 0 && person.Age <= beforeAge) {
                                    idPhoto = preferences_IconPackImages_F.get(beforeAge);
                                    break;
                                }
                            }
                        } else {
                            for (Map.Entry<Integer, Integer> entry : preferences_IconPackImages_M.entrySet()) {
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

                    int bmWidth = bm.getWidth();
                    int bmHeight = bm.getHeight();
                    if (bmHeight > bmWidth) {
                        //noinspection SuspiciousNameCombination
                        bm = Bitmap.createBitmap(bm, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
                    } else {
                        //noinspection SuspiciousNameCombination
                        bm = Bitmap.createBitmap(bm, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
                    }
                }
            }
            if (bm == null) return null;

            int roundingRadiusX = 0;
            int roundingRadiusY = 0;

            int bmWidth = bm.getWidth();
            int bmHeight = bm.getHeight();
            if (roundingFactor > 1) {
                final String roundingFactorStr = String.valueOf(roundingFactor);
                if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Rounded1))) {
                    roundingRadiusX = bmWidth / 12;
                    roundingRadiusY = bmHeight / 12;
                } else if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Rounded2))) {
                    roundingRadiusX = bmWidth / 8;
                    roundingRadiusY = bmHeight / 8;
                } else if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Rounded3))) {
                    roundingRadiusX = bmWidth / 4;
                    roundingRadiusY = bmHeight / 4;
                } else if (roundingFactorStr.equals(resources.getString(R.string.pref_List_PhotoStyle_Circle))) {
                    roundingRadiusX = bmWidth / 2;
                    roundingRadiusY = bmHeight / 2;
                    makeSquared = true;
                }
            }

            if (makeSquared) {
                //final int bmWidth = bmWidth;
                //final int bmHeight = bmHeight;

                if (bmHeight > bmWidth) {
                    //noinspection SuspiciousNameCombination
                    bm = Bitmap.createBitmap(bm, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
                } else {
                    //noinspection SuspiciousNameCombination
                    bm = Bitmap.createBitmap(bm, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
                }

                bmWidth = bm.getWidth();
                bmHeight = bm.getHeight();
            }

            if (addMourningTape) {
                //Если контакт умер, добавлять чёрную ленточку
                //https://stackoverflow.com/questions/3089991/how-to-draw-a-shape-or-bitmap-into-another-bitmap-java-android
                Bitmap bmOverlay = Bitmap.createBitmap(bmWidth, bmHeight, bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bm, new Matrix(), null);

                Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintFill.setStyle(Paint.Style.FILL);
                paintFill.setColor(Color.BLACK);
                float widthCorrection = (float) bmWidth / 12;
                paintFill.setStrokeWidth(widthCorrection * 2);
                canvas.drawLine((float) (bmWidth * 1.25), (float) bmHeight / 2, (float) bmWidth / 2, (float) (bmHeight * 1.25), paintFill);

                Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintStroke.setStyle(Paint.Style.STROKE);
                paintStroke.setColor(Color.WHITE);
                paintStroke.setStrokeWidth(3);
                canvas.drawLine((float) (bmWidth * 1.25 - widthCorrection * 1.4), (float) bmHeight / 2, (float) (bmWidth / 2 - widthCorrection * 1.4), (float) (bmHeight * 1.25), paintStroke);
                canvas.drawLine((float) (bmWidth * 1.25 + widthCorrection * 1.4), (float) bmHeight / 2, (float) (bmWidth / 2 + widthCorrection * 1.4), (float) (bmHeight * 1.25), paintStroke);

                bm.recycle();
                bm = bmOverlay;
            }

            //Добавление иконки избранного
            final String eventKey = getEventKey(singleEventArray);
            final String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);
            if (!forWidget && preferences_list_event_info.contains(context.getString(R.string.pref_List_EventInfo_FavoritesIcon)) && checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[Position_starred])) {

                Bitmap bmOverlay = Bitmap.createBitmap(bmWidth, bmHeight, bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bm, new Matrix(), null);
                bm.recycle();
                Bitmap bmStar = BitmapFactory.decodeResource(getResources(), R.drawable.fav_star);
                final Bitmap bmStarScaled = Bitmap.createScaledBitmap(bmStar, bmOverlay.getWidth() / 4 - (bmOverlay.getWidth() - bmOverlay.getHeight()) / 4, bmOverlay.getHeight() / 4, true);

                if (roundingFactor < 3) { //Не круг - рисуем в левом нижнем углу

                    canvas.drawBitmap(bmStarScaled, 2 + (float)((bmOverlay.getWidth() - bmOverlay.getHeight()) / 4), (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

                } else if (roundingFactor < 9) { //Закругление - рисуем в левом нижнем углу правее

                    canvas.drawBitmap(bmStarScaled, 10 + (float)((bmOverlay.getWidth() - bmOverlay.getHeight())/8), (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

                } else { //Круг - рисуем внизу по центру

                    canvas.drawBitmap(bmStarScaled, (float) (bmOverlay.getWidth() * 3 / 4) / 2, (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

                }
                bmStar.recycle();
                bmStarScaled.recycle();
                bm = bmOverlay;

            }

            return toRoundCorner(bm, roundingRadiusX, roundingRadiusY);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return null;
        }
    }

    @NonNull
    String getContactData(@NonNull Long contactId, @NonNull String columnName) {

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
                map_contacts_data.put(contactId + columnName, checkForNull(dataUri.toString()));
                return checkForNull(dataUri.toString());
            } else {
                Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
                Cursor dataCursor = contentResolver.query(
                        dataUri,
                        null,
                        ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                        new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                        null
                );
                if (dataCursor != null) {
                    int columnIndex = dataCursor.getColumnIndex(columnName);
                    if (columnIndex > -1) {
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
                map_contacts_data.put(contactId + columnName, checkForNull(contactData));
                return checkForNull(contactData);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }

    @NonNull
    HashMap<String, String> getContactDataMulti(@NonNull Long contactId, @NonNull String[] columnNames) {

        HashMap<String, String> resultMap = new HashMap<>();

        try {

            if (contactId == 0 || columnNames.length == 0) return resultMap;

            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

            //Получаем собранное ранее
            List<String> columnNamesToFind = new ArrayList<>();
            for (String columnName : columnNames) {
                if (map_contacts_data.containsKey(contactId + columnName)) {
                    resultMap.put(columnName, map_contacts_data.get(contactId + columnName));
                } else if (columnName.equals(ContactsContract.Contacts.PHOTO_URI)) {
                    Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                    resultMap.put(columnName, checkForNull(dataUri.toString()));
                    map_contacts_data.put(contactId + columnName, checkForNull(dataUri.toString()));
                } else {
                    columnNamesToFind.add(columnName);
                }
            }
            if (columnNamesToFind.isEmpty()) return resultMap; //Всё уже есть

            //Запрос новых данных
            Uri dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            String contactData;
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Cursor dataCursor = contentResolver.query(
                    dataUri,
                    columnNamesToFind.toArray(new String[0]),
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ,
                    new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                    null
            );

            if (dataCursor != null) {
                while (dataCursor.moveToNext()) {
                    for (String columnName : columnNamesToFind) {
                        int columnIndex = dataCursor.getColumnIndex(columnName);
                        if (columnIndex > -1) {
                            contactData = dataCursor.getString(columnIndex);
                            resultMap.put(columnName, checkForNull(contactData));
                            map_contacts_data.put(contactId + columnName, checkForNull(contactData));
                        }
                    }
                }
                dataCursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return resultMap;
    }

    /*@NonNull
    String getContactFullNameShort(@NonNull Long contactId) {

        try {

            if (contactId == 0) return Constants.STRING_EMPTY;

            String result = null;

            HashMap<String, String> contactDataMap = getContactDataMulti(contactId, new String[]{
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME
            });

            String lastName = contactDataMap.get(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
            String firstName = contactDataMap.get(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
            String secondName = contactDataMap.get(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME);
            contactDataMap.clear();

            final String secondNameWord = (!TextUtils.isEmpty(secondName) ? Constants.STRING_SPACE + secondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY);
            if (!TextUtils.isEmpty(lastName)) {
                result = lastName + (!TextUtils.isEmpty(firstName) ? Constants.STRING_SPACE + firstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY) + secondNameWord;
            } else if (!TextUtils.isEmpty(firstName)) {
                if (!secondNameWord.isEmpty()) {
                    result = firstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD + secondNameWord;
                } else {
                    result = firstName;
                }
            }
            return checkForNull(result);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }*/

    @NonNull
    String getContactPhone(@NonNull Long contactId) {

        try {

            if (contactId == 0) return Constants.STRING_EMPTY;

            String phone = Constants.STRING_EMPTY;

            //https://stackoverflow.com/questions/8735683/retrieving-a-phone-number-with-contactscontract-in-android-function-doesnt-wo
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Cursor phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
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
            return checkForNull(phone);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }

    synchronized void computeDates() {
        //Вычисляем даты

        long statCurrentModuleStart = System.currentTimeMillis();

        try {

            if (isEmptyEventList()) return;

            List<String> magicList = new ArrayList<>(); //Для 5k событий

            Calendar now = new GregorianCalendar();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Date currentDay = now.getTime();

            setLocale(false);

            for (int i = 0; i < eventList.size(); i++) {
                computeDateForEvent(i, magicList, now, currentDay);
            }

            //Удаляем пустые
            for (int i = eventList.size() - 1; i >= 0; i--) {
                if (eventList.get(i).isEmpty()) eventList.remove(i);
            }

            //Добавляем 5k+
            if (!magicList.isEmpty()) {
                eventList.addAll(magicList);
                magicList.clear();
            }

            eventListUnsorted = new ArrayList<>(eventList);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {

            //Сортируем
            Collections.sort(eventList);

            statLastComputeDates = System.currentTimeMillis();
            statTimeComputeDates = statLastComputeDates - statCurrentModuleStart;
        }
    }

    @SuppressLint("DiscouragedApi")
    void computeDateForEvent(int i, @NonNull List<String> magicList, @NonNull Calendar now, @NonNull Date currentDay) {

        String singleEvent = Constants.STRING_EMPTY;

        try {
            long dayDiff = -1;
            boolean isYear = false;
            boolean isAD = true;
            Date eventDateFirstTime = null; //оригинальная дата события
            Date eventDateThisTime = null; //следующая дата события
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
            @NonNull final String contactID = checkForNull(singleEventArray[ContactsEvents.Position_contactID]);

            if (TextUtils.isEmpty(singleEventArray[Position_eventDateThisTime])) {
                //перебираем все даты и находим максимальную
                final int nowYear = now.get(Calendar.YEAR);
                for (String dayValue : dayArray) {
                    String accountType = substringBefore(dayValue, Constants.STRING_COLON_SPACE);
                    String storedDate = substringBetween(dayValue, Constants.STRING_COLON_SPACE, Constants.STRING_COLON_SPACE);
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
                                                        try {
                                                            storedDate_Date = sdf_DDMMYYYY.parse(storedDate);
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

                    } else if (accountType.contains(Constants.account_vk)) {

                        if (storedDate.startsWith(Constants.STRING_0000_MINUS)) { //Нет года, формат 0000-mm-dd

                            try {
                                eventDateThisTime = sdf_java.parse(nowYear + Constants.STRING_MINUS + storedDate.substring(5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (eventDateThisTime != null) {
                                long dayDiff_tmp = countDaysDiff(currentDay, eventDateThisTime);
                                if (dayDiff_tmp < 0) eventDateThisTime = addYear(eventDateThisTime, 1);
                                storedDate_Date = eventDateThisTime;
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

                        if (storedDate.startsWith(Constants.STRING_2MINUS) || //Нет года, формат --MM-dd
                                storedDate.startsWith(Constants.STRING_0000_MINUS) || //Нет года, формат 0000-MM-dd
                                (storedDate.startsWith("1604-") && (accountType.contains(Constants.account_exchange) || accountType.contains(Constants.account_google))) || //Нет года, формат 1604-MM-dd - com.google.android.gm.exchange https://stackoverflow.com/questions/14023390/nsdate-return-1604-for-year-value
                                (storedDate.startsWith("1904-") && accountType.contains(Constants.account_huawei)) || //Нет года, формат 1904-MM-dd - com.android.huawei.phone
                                (!TextUtils.isEmpty(eventCaption) && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventCaption.toLowerCase()).find()) //Именины считаем без года
                        ) {

                            try {
                                eventDateThisTime = sdf_java.parse(nowYear + Constants.STRING_MINUS + storedDate.substring(storedDate.startsWith(Constants.STRING_2MINUS) ? 2 : 5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (eventDateThisTime != null) {
                                long dayDiff_tmp = countDaysDiff(currentDay, eventDateThisTime);
                                if (dayDiff_tmp < 0) eventDateThisTime = addYear(eventDateThisTime, 1);
                                storedDate_Date = eventDateThisTime;
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
                                                                    try {
                                                                        storedDate_Date = sdf_DDMMYYYY.parse(storedDate);
                                                                    } catch (ParseException e10) {
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

                    }

                    if (storedDate_Date != null) {
                        if (eventDateFirstTime == null) {
                            eventDateFirstTime = storedDate_Date;
                            isYear = storedDate_isYear;
                        } else if (storedDate_isYear
                                && getCalendarFromDate(storedDate_Date).get(Calendar.YEAR) != nowYear
                                && (!isYear || countDaysDiff(eventDateFirstTime, storedDate_Date) > 0)) { //Если у пользователя несколько дат, берём наименьший возраст todo: можно вынести в настройку - в какую сторону округлять
                            eventDateFirstTime = storedDate_Date;
                            isYear = true;
                        }
                    }
                }

                if (eventDateFirstTime != null) {

                    if (isYear) { //Дата с годом
                        if (isAD) {
                            singleEventArray[Position_eventDateFirstTime] = sdf_DDMMYYYY.format(eventDateFirstTime); //оригинальное событие
                        } else {
                            singleEventArray[Position_eventDateFirstTime] = sdf_DDMMY.format(eventDateFirstTime) + resources.getString(R.string.msg_after_year_bc); //до н.э.
                        }
                    } else { //Дата без года
                        singleEventArray[Position_eventDateFirstTime] = sdf_DDMM.format(eventDateFirstTime); //оригинальное событие без года
                    }

                    if (isYear) { //в eventDateFirstTime - оригинальное событие

                        Calendar cal = getCalendarFromDate(eventDateFirstTime);
                        try {
                            eventDateThisTime = sdf_java.parse(nowYear + Constants.STRING_MINUS + (cal.get(Calendar.MONTH) + 1) + Constants.STRING_MINUS + cal.get(Calendar.DAY_OF_MONTH));
                            if (eventDateThisTime != null) {
                                long dayDiff_tmp = countDaysDiff(currentDay, eventDateThisTime);
                                if (dayDiff_tmp < 0) eventDateThisTime = addYear(eventDateThisTime, 1);
                            }
                        } catch (ParseException e) { /**/ }
                    }

                }

                if (eventDateThisTime != null) {
                    if (TextUtils.isEmpty(singleEventArray[Position_eventDateThisTime]))
                        singleEventArray[Position_eventDateThisTime] = sdf_DDMMYYYY.format(eventDateThisTime); //следующая дата события
                }

            } else {
                try {
                    eventDateFirstTime = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateFirstTime]);
                    isYear = true;
                } catch (ParseException e) { /**/ }
                try {
                    eventDateThisTime = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateThisTime]);
                } catch (ParseException e) { /**/ }

                String dayValue = dayArray[0];
                if (!dayValue.isEmpty()) {
                    increaseStatForAccountType(substringBefore(dayValue, Constants.STRING_COLON_SPACE));
                }

            }

            if (eventDateThisTime != null) {
                dayDiff = countDaysDiff(currentDay, eventDateThisTime);

                if (eventDateFirstTime != null) {
                    dayDiff = countDaysDiff(currentDay, eventDateThisTime);
                    Age = countYearsDiff(eventDateFirstTime, eventDateThisTime); //Считаем, сколько будет лет
                    if (!TextUtils.isEmpty(contactID)) {
                        if (eventSubType.equals(getEventType(Constants.Type_BirthDay)) && !birthdayDatesForIds.containsKey(contactID)) {
                            birthdayDatesForIds.put(contactID, eventDateFirstTime);
                        } else if (eventSubType.equals(getEventType(Constants.Type_Death))) {
                            deathDatesForIds.put(contactID, eventDateFirstTime);
                        }
                    }
                }}

            if (dayDiff == -1) {

                StringBuilder sb = new StringBuilder();
                sb.append(resources.getString(R.string.msg_date_parse_error)).append(singleEventArray[Position_dates]).append(Constants.STRING_COMMA_SPACE).append(singleEventArray[Position_personFullName]);

                Log.i(TAG, sb.toString());
                ToastExpander.showInfoMsg(context, sb.toString());

                eventList.set(i, Constants.STRING_EMPTY);
                return;

            }

            //Если событие в ближайшие 3 дня, то в eventDistance будет <число дней до события>, иначе: "Дней до <тип события>: " +  <число дней до события> + <день недели>
            singleEventArray[Position_eventDistance] = Long.toString(dayDiff);
            singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDiff, eventDateThisTime);

            if (Age > 0) { //Возраст больше 1 года
                singleEventArray[Position_age] = Integer.toString(Age);
                singleEventArray[Position_age_caption] = setAgeFormatting(getAgeString(Age, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20));

                if (eventType.equals(getEventType(Constants.Type_Anniversary))) {
                    @Nullable String anCaption;
                    try {
                        anCaption = context.getString(resources.getIdentifier(Constants.STRING_TYPE_WEDDING + Age, Constants.RES_TYPE_STRING, context.getPackageName()));
                    } catch (Resources.NotFoundException nfe) {
                        anCaption = null;
                    }
                    if (anCaption != null && !TextUtils.isEmpty(anCaption) && !eventCaption.contains(Constants.STRING_PARENTHESIS_OPEN)) {
                        singleEventArray[Position_eventCaption] = eventCaption.concat(Constants.STRING_PARENTHESIS_OPEN).concat(anCaption).concat(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                }
            } else if (eventDateFirstTime != null && countDaysDiff(eventDateFirstTime, eventDateThisTime) > 0) { //Возраст до года
                //todo: если это календарное событие на несколько дней, то 1 день, это, по факту, 2й день события. додумать
                singleEventArray[Position_age_caption] = setAgeFormatting(countDaysDiffText(eventDateFirstTime, eventDateThisTime, 1));
            } else {
                singleEventArray[Position_age] = Constants.STRING_MINUS1;
                singleEventArray[Position_age_caption] = Constants.STRING_EMPTY;
            }
            if (eventDateFirstTime != null && isYear) {
                singleEventArray[Position_age_current] = fillCurrentAge(singleEventArray, eventSubType, countDaysDiffText(eventDateFirstTime, currentDay, 3), currentDay);
            }

            if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay))) {
                final String zodiacSign = getZodiacInfo(ContactsEvents.ZodiacInfo.SIGN_TITLE, singleEventArray[Position_eventDateFirstTime]);
                singleEventArray[Position_zodiacSign] = zodiacSign.equals(Constants.STRING_EMPTY) ? Constants.STRING_EMPTY : zodiacSign;
                final String zodiacYear = getZodiacInfo(ContactsEvents.ZodiacInfo.YEAR_TITLE, singleEventArray[Position_eventDateFirstTime]);
                singleEventArray[Position_zodiacYear] = zodiacYear.equals(Constants.STRING_EMPTY) ? Constants.STRING_EMPTY : zodiacYear;
            }

            //Сортировка: дней до даты + (с уведомлением, не скрыт, скрыт)
            final String eventKey = getEventKey(singleEventArray);
            final String eventKeyWithRawIs = getEventKeyWithRawId(singleEventArray);

            boolean isFavoriteEvent = Constants.STRING_1.equals(singleEventArray[Position_starred]);
            singleEventArray[Position_eventDate_sorted] = (Constants.STRING_00 + dayDiff).substring((Constants.STRING_00 + dayDiff).length() - 3)
                    + (isFavoriteEvent ? "0" : checkIsHiddenEvent(eventKey, eventKeyWithRawIs) ? "3" : checkIsSilencedEvent(eventKey, eventKeyWithRawIs) ? "2" : "1")
                    + (eventType.equals(getEventType(Constants.Type_BirthDay)) ? "1"
                    : eventType.equals(getEventType(Constants.Type_Anniversary)) ? "2"
                    : eventType.equals(getEventType(Constants.Type_Custom)) ? "3"
                    : eventType.equals(getEventType(Constants.Type_Other)) ? "6" : "4");

            eventList.set(i, TextUtils.join(Constants.STRING_EOT, singleEventArray));

            if (Age > 0) {

                if (eventType.equals(getEventType(Constants.Type_BirthDay))) {

                    //Вычисляем 5K даты
                    long days = countDaysDiff(eventDateFirstTime, currentDay);
                    long k = (days + 365) / 5000;
                    long mdays = (days + 365) % 5000;

                    if (mdays >= 0 && mdays <= 365) {
                        //Формируем новую запись
                        Calendar cal5K = Calendar.getInstance();
                        int magicDayDistance = (int) (365 - mdays);
                        cal5K.add(Calendar.DATE, magicDayDistance);

                        String[] singleEventArray5K = singleEventArray.clone();

                        singleEventArray5K[Position_eventType] = getEventType(Constants.Type_5K);
                        singleEventArray5K[Position_eventSubType] = getEventType(Constants.Type_5K);
                        singleEventArray5K[Position_eventCaption] = "5K+";
                        singleEventArray5K[Position_eventLabel] = sdf_DDMMYYYY.format(cal5K.getTime());
                        //для выдачи даты юбилея,а не первоначального события: sdfYear.format(sdf.parse(cal5K.get(YEAR) + "-" + (cal5K.get(Calendar.MONTH) + 1) + "-" + cal5K.get(Calendar.DAY_OF_MONTH)));
                        singleEventArray5K[Position_eventDateThisTime] = sdf_DDMMYYYY.format(cal5K.getTime());
                        singleEventArray5K[Position_eventDateFirstTime] = sdf_DDMMYYYY.format(eventDateFirstTime);
                        singleEventArray5K[Position_age] = Integer.toString(Age);
                        singleEventArray5K[Position_age_caption] = setAgeFormatting(getAgeString(5 * k * 1000, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20));
                        singleEventArray5K[Position_eventDistance] = Integer.toString(magicDayDistance);
                        singleEventArray5K[Position_eventDistanceText] = getEventDistanceText(magicDayDistance, cal5K.getTime());
                        singleEventArray5K[Position_eventIcon] = Integer.toString(R.drawable.ic_event_medal); //https://www.flaticon.com/free-icon/medal_610333
                        singleEventArray5K[Position_eventEmoji] = resources.getString(R.string.event_type_5k_emoji);
                        singleEventArray5K[Position_age_current] = fillCurrentAge(singleEventArray, eventSubType, countDaysDiffText(eventDateFirstTime, currentDay, 3), currentDay); //Возраст текущий
                        //singleEventArray[Position_eventStorage] = STRING_STORAGE_CONTACTS; //Где искать событие по ID

                        final String event5kKey = getEventKey(singleEventArray5K);
                        final String event5kKeyWithRawId = getEventKeyWithRawId(singleEventArray5K);

                        singleEventArray5K[Position_eventDate_sorted] = (Constants.STRING_00 + magicDayDistance).substring((Constants.STRING_00 + magicDayDistance).length() - 3)
                                + (checkIsHiddenEvent(event5kKey, event5kKeyWithRawId) ? "3" : checkIsSilencedEvent(event5kKey, event5kKeyWithRawId) ? "2" : "1") + "5";

                        magicList.add(TextUtils.join(Constants.STRING_EOT, singleEventArray5K));
                    }
                }

                //Счётчики дней
                if (getXDaysEventsCount() > 0 && isXDaysEvent(eventKey)) {
                    final List<String> valuePeriods = getXDaysEvent(eventKey);
                    Calendar dateStart = ContactsEvents.removeTime(Calendar.getInstance());
                    Calendar dateEnd = ContactsEvents.removeTime(Calendar.getInstance());
                    dateEnd.add(Calendar.YEAR, 1);
                    int toRepeat = 365;
                    try {
                        if (!valuePeriods.get(1).isEmpty())
                            toRepeat = -Integer.parseInt(valuePeriods.get(1));
                    } catch (NumberFormatException e) { /**/ }

                    ArrayList<ContactsEvents.Event> events = getNextRepeatsForEvent(
                            ContactsEvents.removeTime(Calendar.getInstance()),
                            dateEnd,
                            ContactsEvents.getCalendarFromDate(eventDateFirstTime),
                            valuePeriods.get(0),
                            toRepeat
                    );
                    if (events != null && !events.isEmpty()) {
                        for (Event event : events) {

                            String[] singleEventArrayXdays = singleEventArray.clone();
                            long xDaysDistance = countDaysDiff(currentDay, event.date);

                            singleEventArrayXdays[Position_eventDateThisTime] = sdf_DDMMYYYY.format(event.date);
                            singleEventArrayXdays[Position_age_caption] = setAgeFormatting(event.distance);
                            singleEventArrayXdays[Position_eventStorage] = Constants.STRING_STORAGE_XDAYS;
                            singleEventArrayXdays[Position_eventDistance] = Long.toString(xDaysDistance);
                            singleEventArrayXdays[Position_eventDistanceText] = getEventDistanceText(xDaysDistance, event.date);
                            singleEventArrayXdays[Position_eventEmoji] = resources.getString(R.string.event_type_xdays_emoji);
                            singleEventArrayXdays[Position_eventIcon] = Integer.toString(R.drawable.ic_event_xdays);
                            singleEventArrayXdays[Position_eventDescription] = Constants.STRING_EMPTY;

                            singleEventArrayXdays[Position_eventDate_sorted] = (Constants.STRING_00 + xDaysDistance).substring((Constants.STRING_00 + xDaysDistance).length() - 3)
                                    + (checkIsHiddenEvent(eventKey, eventKeyWithRawIs) ? "3" : checkIsSilencedEvent(eventKey, eventKeyWithRawIs) ? "2" : "1") + "1";

                            magicList.add(TextUtils.join(Constants.STRING_EOT, singleEventArrayXdays));

                        }
                    }
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + singleEvent);
        }
    }

    @NonNull
    private String fillCurrentAge(@NonNull String[] singleEventArray, @NonNull String eventSubType, @NonNull String currentAge, Date today) {

        String age = "";
        try {

            @NonNull final String contactID = checkForNull(singleEventArray[Position_contactID]);

            if (eventSubType.equals(getEventType(Constants.Type_BirthDay)) //Если это день рождения или 5K
                    || eventSubType.equals(getEventType(Constants.Type_5K))) {
                if (!currentAge.isEmpty() && !currentAge.startsWith(Constants.STRING_0)) {
                    if (deathDatesForIds.containsKey(contactID)) { //Но есть годовщина смерти
                        age = resources.getString(R.string.msg_age_could_be_now);

                        //Если годовщина смерти попалась раньше дня рождения, то у неё currentAge будет содержать текущий возраст - надо обновить
                        final String key = contactID + Constants.STRING_2HASH + getEventType(Constants.Type_Death);
                        if (map_eventsBySubtypeAndPersonID_offset.containsKey(key)) {
                            Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(key);
                            if (eventIndex != null && eventIndex <= eventList.size()) {
                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                Date birthDate = birthdayDatesForIds.get(contactID);
                                Date deathDate = deathDatesForIds.get(contactID);
                                if (birthDate != null && deathDate != null) {
                                    final String wasAge = countDaysDiffText(birthDate, deathDate, 3);
                                    singleRowList.set(Position_age_current, resources.getString(R.string.msg_age_was).concat(wasAge));
                                    eventList.set(eventIndex, String.join(Constants.STRING_EOT, singleRowList));
                                }
                            }
                        }

                    } else {
                        age = resources.getString(R.string.msg_age_now);
                    }
                    age = age.concat(currentAge);
                }
            } else if (birthdayDatesForIds.containsKey(contactID)) {
                Date birthDate = birthdayDatesForIds.get(contactID);
                if (eventSubType.equals(getEventType(Constants.Type_Death))) { //Если это годовщина смерти
                    Locale locale_en = new Locale(Constants.LANG_EN);
                    SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                    Date eventDate = sdfYear.parse(singleEventArray[Position_eventDateFirstTime]);
                    if (eventDate != null && birthDate != null) {
                        age = resources.getString(R.string.msg_age_was).concat(countDaysDiffText(birthDate, eventDate, 3));
                    }
                } else { //Другие события
                    Locale locale_en = new Locale(Constants.LANG_EN);
                    SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                    Date eventDate = sdfYear.parse(singleEventArray[Position_eventDateThisTime]);
                    if (eventDate != null && birthDate != null) {
                        if (deathDatesForIds.containsKey(contactID)) { //Но есть годовщина смерти
                            age = resources.getString(R.string.msg_age_could_be);
                        } else if (eventDate.compareTo(today) == 0) {
                            age = resources.getString(R.string.msg_age_now);
                        } else {
                            age = resources.getString(R.string.msg_age_will_be);
                        }
                        age = age.concat(countDaysDiffText(birthDate, eventDate, 3));
                    }
                }
            } else if (Constants.STRING_STORAGE_CONTACTS.equals(singleEventArray[Position_eventStorage])) {
                if (eventSubType.equals(getEventType(Constants.Type_Death))) { //Если это годовщина смерти
                    age = resources.getString(R.string.msg_age_passed).concat(currentAge);
                } else {
                    age = resources.getString(R.string.msg_age_now).concat(currentAge);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + String.join(Constants.STRING_EOT, singleEventArray));
        }
        return age;
    }

    private void increaseStatForAccountType(@NonNull String accountType) {
        if (!statEventTypes.containsKey(accountType)) {
            statEventTypes.put(accountType, 1);
        } else {
            Integer oldCount = statEventTypes.get(accountType);
            statEventTypes.put(accountType, (oldCount == null ? 0 : oldCount) + 1);
        }
    }

    private String getEventDistanceText(long dayDiff, @NonNull Date eventDate) {
        //Если событие в ближайшие 3 дня, то вернёт "сегодня", "завтра", "послезавтра", если позже, то "через X дней" + "|в " + <день недели> + | + <MM dddd> | <день недели кратко>

        StringBuilder eventDistance = new StringBuilder();
        try {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(eventDate);

            String currentLanguage = Locale.getDefault().getLanguage();
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
                            .append(getAgeString(dayDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20))
                            .append(currentLanguage.equals(getResources().getString(R.string.pref_Language_de)) ? "n" : Constants.STRING_EMPTY); //для немецкого "in 10 TageN"
                } else if (dayDiff == -1) { //Вчера
                    eventDistance.append(getResources().getString(R.string.msg_yesterday));
                } else if (dayDiff == -2) { //Позавчера
                    eventDistance.append(getResources().getString(R.string.msg_before_yesterday));
                } else { //Подальше назад
                    eventDistance
                            .append(getResources().getString(R.string.msg_after_event_prefix))
                            .append(getAgeString(-dayDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20))
                            .append(getResources().getString(R.string.msg_after_event_postfix));
                }
            }
            final SimpleDateFormat sdfOut = new SimpleDateFormat(preferences_list_dateformat == 3 || preferences_list_dateformat == 5 ? Constants.DATE_MMMM_D : Constants.DATE_D_MMMM, Locale.forLanguageTag(currentLocale));

            String weekDay = getResources().getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1];
            if (currentLanguage.equals(getResources().getString(R.string.pref_Language_be)) && eventDistance.substring(eventDistance.length() - 1).matches("[аоуіэыяеёю]")) {
                weekDay = weekDay.replace("у ", "ў ");
            }
            eventDistance
                    .append(Constants.STRING_BAR)
                    .append(weekDay)
                    .append(Constants.STRING_BAR)
                    .append(sdfOut.format(c1.getTime()))
                    .append(Constants.STRING_BAR)
                    .append(getResources().getStringArray(R.array.weekDaysShort)[c1.get(Calendar.DAY_OF_WEEK) - 1]);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + dayDiff + Constants.STRING_EOL + eventDate);
        }
        return eventDistance.toString();
    }

    @SuppressLint("DiscouragedApi")
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

            Calendar now = new GregorianCalendar();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Date currentDay = now.getTime();

            List<String> newList = new ArrayList<>();
            statEventsPrevEventsFound = 0;
            for (int i = dataList.size() - 1; i >= 0 && statEventsPrevEventsFound < params_events; i--) {
                String li = dataList.get(i);
                String[] singleEventArray = li.split(Constants.STRING_EOT, -1);
                //todo: переделать на получение событий "сегодня минус предыдущие дни"
                if (!singleEventArray[Position_eventSubType].equals(getEventType(Constants.Type_5K)) //пропускаем 5K+
                        && !singleEventArray[Position_eventSubType].equals(getEventType(Constants.Type_CalendarEvent)) //пропускаем события календаря
                ) {
                    if (params_days == 365) { //нет ограничения по дням
                        newList.add(li);
                        statEventsPrevEventsFound++;
                    } else {
                        Date eventDate = null;
                        try {
                            eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateThisTime]);
                            if (eventDate != null) {
                                eventDate = addYear(eventDate, -1);
                            }
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            if (-countDaysDiff(currentDay, eventDate) <= params_days) {
                                newList.add(li);
                                statEventsPrevEventsFound++;
                            } else {
                                break;
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
                        eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateThisTime]);
                    } catch (Exception e) { /**/ }

                    if (eventDate != null) {

                        eventDate = addYear(eventDate, -1);
                        singleEventArray[Position_eventDateThisTime] = sdf_DDMMYYYY.format(eventDate);
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
                            singleEventArray[Position_age_caption] = setAgeFormatting(getAgeString(Age, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20));

                            if (singleEventArray[Position_eventType].equals(getEventType(Constants.Type_Anniversary))) {
                                @Nullable String anCaption;
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return result;
    }

    void updateWidgets(int widgetID, StringBuilder log) {

        if (context == null) return;
        AtomicInteger countSentRequests = new AtomicInteger();
        statTimeUpdateWidgets = 0;

        //Посылаем сообщения на обновление виджетов
        try {

            if (widgetsUpdateThread != null) {
                if (widgetsUpdateThread.isAlive()) {
                    widgetsUpdateThread.interrupt();
                }
            }

            //https://stackoverflow.com/questions/21300924/difference-between-executors-newfixedthreadpool1-and-executors-newsinglethread
            Thread t = new Thread() {

                public void run() {

                    int[] ids;

                    ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget2x2.class));
                    if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
                        //Toast.makeText(context, "Widget2x2:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                        Widget2x2 myWidget = new Widget2x2();
                        myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                        countSentRequests.getAndIncrement();
                    }

                    ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget5x1.class));
                    if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
                        //Toast.makeText(context, "Widget5x1:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                        Widget5x1 myWidget = new Widget5x1();
                        myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                        countSentRequests.getAndIncrement();
                    }

                    ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget4x1.class));
                    if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
                        //Toast.makeText(context, "Widget4x1:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                        Widget4x1 myWidget = new Widget4x1();
                        myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                        countSentRequests.getAndIncrement();
                    }

                    ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetList.class));
                    if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
                        //Toast.makeText(context, "WidgetList:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                        WidgetList myWidget = new WidgetList();
                        myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                        countSentRequests.getAndIncrement();
                    }

                    ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetPhotoList.class));
                    if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
                        //Toast.makeText(context, "WidgetPhotoList:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                        WidgetPhotoList myWidget = new WidgetPhotoList();
                        myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                        countSentRequests.getAndIncrement();
                    }

                    ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetCalendar.class));
                    if (ids != null && ((widgetID > 0 && ids.length > 0 && contains(ids, widgetID)) || widgetID == 0)) {
                        //Toast.makeText(context, "WidgetCalendar:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                        WidgetCalendar myWidget = new WidgetCalendar();
                        myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                        countSentRequests.getAndIncrement();
                    }

                    interrupt();

                }
            };
            widgetsUpdateThread = t;
            t.start();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (log != null && countSentRequests.get() > 0)
                log.append(context.getString(R.string.msg_sent_widgets_update_request)).append(Constants.STRING_EOL);
        }
    }

    void initNotificationChannel(StringBuilder log) {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //для Android 8+

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

                //находим канал. если канала нет или рингтон там другой - пересоздаём канал
                String channel_id = Integer.toString(preferences_notification_channel_id);
                @Nullable NotificationChannel channel = notificationManager.getNotificationChannel(channel_id);

                if (!preferences_notifications_days.isEmpty() && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                    //https://developer.android.com/training/notify-user/channels.html
                    //After you create a notification channel, you cannot change the notification behaviors—the user has complete control at that point. Though you can still change a channel's name and description
                    //https://stackoverflow.com/questions/46234254/android-oreo-notification-keep-making-sound-even-if-i-do-not-set-sound-on-older

                    if (channel != null && !channel.getSound().toString().equals(preferences_notifications_ringtone)) {
                        notificationManager.deleteNotificationChannel(channel_id);
                        channel = null;
                        log.append(resources.getString(R.string.msg_deleted_channel, channel_id));
                    }

                    if (channel == null) {
                        preferences_notification_channel_id = generator.nextInt(1000);
                        channel_id = Integer.toString(preferences_notification_channel_id);

                        channel = new NotificationChannel(channel_id, context.getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription(context.getString(R.string.pref_Notifications_Notification_Channel_Description));
                        if (preferences_notifications_ringtone != null)
                            channel.setSound(
                                    Uri.parse(preferences_notifications_ringtone),
                                    new AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                            );
                        channel.enableVibration(true);
                        notificationManager.createNotificationChannel(channel);

                        log.append(resources.getString(R.string.msg_created_channel, String.valueOf(preferences_notification_channel_id)));
                        if (preferences_notifications_ringtone != null)
                            log
                                    .append(resources.getString(R.string.msg_ringtone))
                                    .append(Uri.parse(preferences_notifications_ringtone))
                                    .append(Constants.STRING_EOL);
                        savePreferences();
                    }

                } else if (channel != null) {
                    notificationManager.deleteNotificationChannel(channel_id);
                    log.append(resources.getString(R.string.msg_deleted_channel, channel_id));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void initBootReceiver(StringBuilder log) {

        try {

            PackageManager pm = context.getPackageManager();
            ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);

            if (!preferences_notifications_days.isEmpty() && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                //To enable Boot Receiver class
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    log.append(resources.getString(R.string.msg_notifications_were_enabled)).append(Constants.STRING_EOL);
                }

            } else { //Disable Daily Notifications
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    log.append(resources.getString(R.string.msg_notifications_were_disabled)).append(Constants.STRING_EOL);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void initWidgetUpdate(@NonNull StringBuilder log) {

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
                        if (checkCanExactAlarm()) {
                            try {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            } catch (SecurityException se) {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            }
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    }
                    log.append(resources.getString(R.string.msg_next_widgetupdate, sdf_DDMMYYYYHHMM.format(calendar.getTime())));
                }

            } else { //Disable
                if (PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentImmutable) != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    void initNotificationSchedule(@NonNull StringBuilder log) {

        try {

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentMutable);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            boolean needToNotify = false;
            boolean canExactAlarm = false;
            if (!preferences_notifications_days.isEmpty() && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                canExactAlarm = checkCanExactAlarm();
                if (!canExactAlarm) {
                    log.append(context.getString(R.string.msg_exact_alarms_disabled));
                }
                needToNotify = true;
            }

            if (needToNotify) {

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
                        if (canExactAlarm) {
                            try {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            } catch (SecurityException se) {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            }
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    }

                    log.append(resources.getString(R.string.msg_next_notification, sdf_DDMMYYYYHHMM.format(calendar.getTime())));
                }

            } else { //Disable Daily Notifications
                if (PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntentImmutable) != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void showNotifications(boolean forceNoEventsMessage, String channelId) {
        //https://www.journaldev.com/15468/android-notification-styling

        try {

            if (checkNoNotificationAccess()) return;
            Set<String> notifications_days = new HashSet<>(preferences_notifications_days); //За сколько дней уведомлять
            if (notifications_days.isEmpty()) return;

            setLocale(true);

            Calendar now = new GregorianCalendar();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Date currentDay = now.getTime();

            class NotifyEvent {
                final String[] singleEventArray;
                final Date eventDate;

                public NotifyEvent(@NonNull String[] singleEventArray, @NonNull Date eventDate) {
                    this.singleEventArray = singleEventArray;
                    this.eventDate = eventDate;
                }

                String eventDay() {
                    //todo: выводить в соответствии с настройками
                    return sdf_DDMM.format(eventDate);
                }

                @NonNull
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0, il = singleEventArray.length; i < il; i++) {
                        if (i > 0) sb.append(Constants.STRING_EOT);
                        sb.append(singleEventArray[i]);
                    }
                    return sb.toString();
                }
            }

            List<NotifyEvent> listNotify = new ArrayList<>();
            for (String event : eventList) {
                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                if (singleEventArray.length == Position_attrAmount) {

                    final String eventKey = getEventKey(singleEventArray);
                    final String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);

                    if (preferences_notifications_event_types.contains(singleEventArray[Position_eventType])
                            && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId))
                            && (getSilencedEventsCount() == 0 || !checkIsSilencedEvent(eventKey, eventKeyWithRawId))) {

                        Date eventDate = null;
                        try {
                            eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateThisTime]);
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            if (listNotify.size() >= 50)
                                break; //https://stackoverflow.com/questions/33364368/android-system-notification-limit-per-app

                            long countDays = countDaysDiff(currentDay, eventDate);
                            if (countDays > 14) {
                                break;
                            } else if (notifications_days.contains(String.valueOf(countDays))) {
                                listNotify.add(new NotifyEvent(singleEventArray, eventDate));
                            }
                        }
                    }
                }
            }
            if (listNotify.isEmpty() && !forceNoEventsMessage) return;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancelAll();

            StringBuilder textBig;
            if (listNotify.isEmpty() || //Тестовое уведомление
                    preferences_notifications_type == 0 || //Одно общее уведомление
                    listNotify.size() >= 3 && preferences_notifications_type == 2 || //Если событий меньше 3 -> отдельные, иначе - общее
                    listNotify.size() >= 4 && preferences_notifications_type == 3 || //Если событий меньше 4 -> отдельные, иначе - общее
                    preferences_notifications_type == 4 //За сегодня -> отдельные, остальные -> общее
            ) {

                textBig = new StringBuilder();
                String textSmall = null;
                if (!listNotify.isEmpty()) {
                    int countEvents = 0;
                    for (NotifyEvent event : listNotify) {
                        if (preferences_notifications_type != 4 || event.eventDate.after(currentDay)) {
                            countEvents++;
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            textBig.append(event.singleEventArray[Position_eventEmoji]).append(Constants.STRING_SPACE).append(event.eventDay()).append(Constants.STRING_SPACE).append(preferences_list_nameformat == 2 ? event.singleEventArray[Position_personFullNameAlt] : event.singleEventArray[Position_personFullName]);
                            if (!TextUtils.isEmpty(event.singleEventArray[Position_age_caption].trim()))
                                textBig.append(Constants.STRING_COLON_SPACE).append(event.singleEventArray[Position_age_caption]);
                            if (event.singleEventArray[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_Anniversary))) {
                                textBig.append(Constants.STRING_SPACE).append(substringBefore(event.singleEventArray[Position_eventCaption], Constants.STRING_PARENTHESIS_OPEN));
                            }
                        }
                    }
                    if (countEvents > 0) {
                        if (preferences_notifications_type == 4) {
                            textSmall = context.getString(R.string.msg_notifications_count) + countEvents;
                        } else {
                            textSmall = context.getString(R.string.msg_notifications_soon) + countEvents;
                        }
                        textBig.insert(0, textSmall + ":\n");
                    }

                } else if (preferences_notifications_type != 4) {
                    textSmall = context.getString(R.string.msg_notifications_soon_no_events);
                }

                if (textSmall != null) {
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                            .setColor(this.getResources().getColor(R.color.dark_green))
                            .setSmallIcon(R.drawable.ic_icon_notify).setContentText(textSmall)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig)) //Ограничение 5120 символов https://stackoverflow.com/questions/27124887/whats-the-max-size-of-a-bigtextstyle-notification
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setWhen(0) //https://stackoverflow.com/questions/18249871/android-notification-buttons-not-showing-up/18603076#18603076
                            .setAutoCancel(true);

                    if (preferences_notifications_priority > 1 && !listNotify.isEmpty()) {
                        builder.setOngoing(true);
                        builder.setPriority(NotificationCompat.PRIORITY_MAX);
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        if (preferences_notifications_ringtone != null)
                            builder.setSound(Uri.parse(preferences_notifications_ringtone));
                    }

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    notificationManager.notify(Constants.defaultNotificationID, builder.build());
                }
            }

            if (!listNotify.isEmpty() && (preferences_notifications_type == 1 || //Несколько отдельных уведомлений
                    listNotify.size() < 3 && preferences_notifications_type == 2 || //Если событий меньше 3 -> отдельные
                    listNotify.size() < 4 && preferences_notifications_type == 3 || //Если событий меньше 4 -> отдельные
                    preferences_notifications_type == 4 //За сегодня -> отдельные
            )) {

                for (int i = listNotify.size() - 1; i >= 0; i--) {

                    NotifyEvent event = listNotify.get(i);

                    if (preferences_notifications_type != 4 || event.eventDate.equals(currentDay)) {
                        textBig = new StringBuilder();
                        textBig.append(event.singleEventArray[Position_eventEmoji]).append(Constants.STRING_SPACE).append(event.eventDay()).append(Constants.STRING_SPACE).append(preferences_list_nameformat == 2 ? event.singleEventArray[Position_personFullNameAlt] : event.singleEventArray[Position_personFullName]);
                        if (!TextUtils.isEmpty(event.singleEventArray[Position_age_caption].trim()))
                            textBig.append(Constants.STRING_COLON_SPACE).append(event.singleEventArray[Position_age_caption]);
                        if (event.singleEventArray[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_Anniversary))) {
                            textBig.append(Constants.STRING_SPACE).append(substringBefore(event.singleEventArray[Position_eventCaption], Constants.STRING_PARENTHESIS_OPEN));
                        }

                        int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                        final String[] eventDistance = event.singleEventArray[Position_eventDistanceText].split(Constants.STRING_PIPE, -1);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                .setColor(this.getResources().getColor(R.color.dark_green))
                                .setSmallIcon(R.drawable.ic_icon_notify)
                                .setContentText(textBig)
                                .setContentTitle(event.singleEventArray[Position_eventDistance].equals(Constants.STRING_0) ? eventDistance[0] : eventDistance[0] + Constants.STRING_SPACE + eventDistance[1])
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true);

                        Intent intent = null;

                        if (preferences_notifications_on_click_action == 7) { //Основной список событий
                            intent = new Intent(context, MainActivity.class);
                            intent.setAction(Constants.ACTION_LAUNCH);
                        } else if (preferences_notifications_on_click_action >= 1 & preferences_notifications_on_click_action <= 4) {
                            intent = getViewActionIntent(event.singleEventArray, preferences_notifications_on_click_action);
                        } else if (preferences_notifications_on_click_action == 6) { //Закрыть уведомление
                            intent = new Intent();
                        }

                        if (intent != null) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                            builder.setContentIntent(pendingIntent);
                        }

                        final String eventAsString = event.toString();
                        //todo: .addPerson для телефона и почты

                        if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Dial))
                                && !event.singleEventArray[Position_eventSubType].equals(getEventType(Constants.Type_CalendarEvent))
                                && !TextUtils.isEmpty(event.singleEventArray[Position_contactID])
                                && !TextUtils.isEmpty(getContactPhone(parseToLong(event.singleEventArray[Position_contactID])))) {

                            Intent intentDial = new Intent(context, ActionReceiver.class);
                            intentDial.setAction(Constants.ACTION_DIAL);
                            intentDial.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentDial.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingDial = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentDial, PendingIntentImmutable);
                            NotificationCompat.Action actionDial = new NotificationCompat.Action(0, context.getString(R.string.button_dial), pendingDial);
                            builder.addAction(actionDial);

                        }

                        final String eventKey = getEventKey(event.singleEventArray);
                        if (!eventKey.isEmpty() && preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Silent))) {
                            Intent intentSilent = new Intent(context, ActionReceiver.class);
                            intentSilent.setAction(Constants.ACTION_SILENT);
                            intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingSilent = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSilent, PendingIntentImmutable);
                            NotificationCompat.Action actionSilent = new NotificationCompat.Action(0, context.getString(R.string.button_silent), pendingSilent);
                            builder.addAction(actionSilent);
                        }

                        if (!eventKey.isEmpty() && preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Hide))) {
                            Intent intentHide = new Intent(context, ActionReceiver.class);
                            intentHide.setAction(Constants.ACTION_HIDE);
                            intentHide.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentHide.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingHide = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentHide, PendingIntentImmutable);
                            NotificationCompat.Action actionHide = new NotificationCompat.Action(0, context.getString(R.string.button_hide), pendingHide);
                            builder.addAction(actionHide);
                        }

                        if (preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Remind))) {
                            Intent intentSnooze = new Intent(context, ActionReceiver.class);
                            intentSnooze.setAction(Constants.ACTION_SNOOZE);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSnooze, PendingIntentImmutable);
                            NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                            builder.addAction(actionSnooze);
                        }

                        if (preferences_notifications_priority > 2 && preferences_notifications_quick_actions.contains(context.getString(R.string.pref_Notifications_QuickActions_Close))) {
                            Intent intentClose = new Intent(context, ActionReceiver.class);
                            intentClose.setAction(Constants.ACTION_CLOSE);
                            intentClose.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentClose.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingClose = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentClose, PendingIntentImmutable);
                            NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_close), pendingClose);
                            builder.addAction(actionSnooze);
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            if (preferences_notifications_ringtone != null)
                                builder.setSound(Uri.parse(preferences_notifications_ringtone));
                        }

                        String eventSubType = event.singleEventArray[Position_eventSubType];
                        int roundingFactor;
                        if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent))) {
                            roundingFactor = 1;
                        } else {
                            roundingFactor = preferences_list_photostyle;
                        }
                        builder.setLargeIcon(getEventPhoto(eventAsString, true, true, false, roundingFactor));

                        if (preferences_notifications_priority > 2) {
                            builder.setOngoing(true);
                            builder.setPriority(NotificationCompat.PRIORITY_MAX);
                        }

                        notificationManager.notify(notificationID, builder.build());
                    }
                }

            }
            listNotify.clear();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return Constants.STRING_EMPTY;
    }

    String getEventKeyWithRawId(@NonNull String[] singleEventArray) {

        try {

            if (!TextUtils.isEmpty(singleEventArray[Position_eventSubType].trim())) {
                if (!TextUtils.isEmpty(singleEventArray[Position_rawContactID].trim())) {
                    return singleEventArray[Position_rawContactID] + Constants.STRING_2HASH + singleEventArray[Position_eventSubType];
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
                    if (checkCanExactAlarm()) {
                        try {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
                        } catch (SecurityException se) {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
                        }
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextUpdateTimeMillis, pendingIntent);
                    }
                }
                boolean finalIsSnoozed = isSnoozed;
                handler.post(() -> Toast.makeText(context, context.getString(finalIsSnoozed ? R.string.msg_snoozed_until : R.string.msg_notify_time, sdf_DDMMYYYYHHMM.format(nextUpdateTimeMillis)), Toast.LENGTH_LONG).show());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @SuppressLint("MissingPermission")
    void showNotification(String dataNotify, String channelId) {

        try {

            if (dataNotify == null || dataNotify.isEmpty()) return;

            //Toast.makeText(context, "TEST: " + dataNotify, Toast.LENGTH_LONG).show();

            String[] singleEventArray = dataNotify.split(Constants.STRING_EOT, -1);
            Date eventDate = null;
            String eventDay = null;
            try {
                eventDate = sdf_DDMMYYYY.parse(singleEventArray[Position_eventDateThisTime]);
                if (eventDate != null) {
                    eventDay = sdf_DDMM.format(eventDate);
                }
            } catch (Exception e) { /**/ }

            if (eventDate != null) {
                StringBuilder textBig = new StringBuilder();
                textBig.append(singleEventArray[Position_eventEmoji]).append(Constants.STRING_SPACE).append(eventDay).append(Constants.STRING_SPACE).append(singleEventArray[Position_personFullName]);
                if (!TextUtils.isEmpty(singleEventArray[Position_age_caption].trim()))
                    textBig.append(": ").append(singleEventArray[Position_age_caption]);

                int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                final String[] eventDistance = singleEventArray[Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId).setColor(this.getResources().getColor(R.color.dark_green)).setSmallIcon(R.drawable.ic_icon_notify).setContentText(textBig).setContentTitle(eventDistance[0] + Constants.STRING_SPACE + eventDistance[1]).setStyle(new NotificationCompat.BigTextStyle().bigText(textBig)).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = null;
                if (singleEventArray[Position_eventStorage].contains(Constants.STRING_STORAGE_CONTACTS) && !TextUtils.isEmpty(singleEventArray[Position_contactID])) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (singleEventArray[Position_eventStorage].contains(Constants.STRING_STORAGE_CALENDAR) && !TextUtils.isEmpty(singleEventArray[Position_eventID])) {
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
                    if (preferences_notifications_ringtone != null)
                        builder.setSound(Uri.parse(preferences_notifications_ringtone));
                }

                String eventSubType = singleEventArray[Position_eventSubType];
                int roundingFactor;
                if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent))) {
                    roundingFactor = 1;
                } else {
                    roundingFactor = preferences_list_photostyle;
                }
                builder.setLargeIcon(getEventPhoto(dataNotify, true, true, false, roundingFactor));
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (!checkNoNotificationAccess()) {
                    notificationManager.notify(notificationID, builder.build());
                }

            }


        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    boolean isEmptyEventList() {
        return eventList.isEmpty();
    }

    void clearHiddenEvents() {

        try {

            if (getHiddenEventsCount() > 0) {
                preferences_hiddenEvents.clear();
                preferences_hiddenEventsRawIds.clear();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    int getHiddenEventsCount() {

        try {

            return preferences_hiddenEvents == null || preferences_hiddenEvents.isEmpty() ? 0 : preferences_hiddenEvents.size();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    boolean checkIsHiddenEvent(@NonNull String key, String keyWithRawId) {

        try {

            return (!key.isEmpty() && preferences_hiddenEvents != null && preferences_hiddenEvents.contains(key))
                    || (keyWithRawId != null && !keyWithRawId.isEmpty() && preferences_hiddenEventsRawIds != null && preferences_hiddenEventsRawIds.contains(keyWithRawId));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setHiddenEvent(@NonNull String key, String keyWithRawId) {

        try {

            SharedPreferences.Editor editor = null;

            if (!key.isEmpty() && preferences_hiddenEvents != null) {
                preferences_hiddenEvents.add(key);
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            }

            if (!TextUtils.isEmpty(keyWithRawId) && preferences_hiddenEventsRawIds != null) {
                preferences_hiddenEventsRawIds.add(keyWithRawId);
                if (editor == null) editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Hidden_rawIds_key), preferences_hiddenEventsRawIds);
            }

            if (editor != null) {
                editor.apply();
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    boolean unsetHiddenEvent(@NonNull String key, String keyWithRawId) {

        try {

            if (!checkIsHiddenEvent(key,keyWithRawId)) return false;

            boolean idremoved = false;

            if (preferences_hiddenEvents != null)
                idremoved = preferences_hiddenEvents.remove(key);

            if (preferences_hiddenEventsRawIds != null && !TextUtils.isEmpty(keyWithRawId))
                idremoved = idremoved | preferences_hiddenEventsRawIds.remove(keyWithRawId);

            if (idremoved) {

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
                editor.putStringSet(context.getString(R.string.pref_Events_Hidden_rawIds_key), preferences_hiddenEvents);

                //Если удалили последнее событие - переключаем режим на стандартный
                if (preferences_list_events_scope == Constants.pref_Events_Scope_Hidden && preferences_hiddenEvents.isEmpty()) {
                    preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                    editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
                }

                editor.apply();

            }

            //clearDeadLinksHiddenEvents();
            //if (preferences_debug_on) Toast.makeText(context, "Unhided event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    void clearSilencedEvents() {

        try {

            if (getSilencedEventsCount() > 0) {
                preferences_silentEvents.clear();
                preferences_silentEventsRawIds.clear();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    int getSilencedEventsCount() {

        try {

            return preferences_silentEvents == null || preferences_silentEvents.isEmpty() ? 0 : preferences_silentEvents.size();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    boolean checkIsSilencedEvent(@NonNull String key, String keyWithRawId) {

        try {

            return (!key.isEmpty() && preferences_silentEvents != null && preferences_silentEvents.contains(key))
                    || (keyWithRawId != null && !keyWithRawId.isEmpty() && preferences_silentEventsRawIds != null && preferences_silentEventsRawIds.contains(keyWithRawId));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setSilencedEvent(@NonNull String key, String keyWithRawId) {

        try {

            SharedPreferences.Editor editor = null;

            if (!key.isEmpty() && preferences_silentEvents != null) {
                preferences_silentEvents.add(key);
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            }

            if (!TextUtils.isEmpty(keyWithRawId) && preferences_silentEventsRawIds != null) {
                preferences_silentEventsRawIds.add(keyWithRawId);
                if (editor == null) editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Silent_rawIds_key), preferences_silentEventsRawIds);
            }

            if (editor != null) {
                editor.apply();
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    boolean unsetSilencedEvent(@NonNull String key, String keyWithRawId) {

        try {

            if (!checkIsSilencedEvent(key, keyWithRawId)) return false;

            boolean idremoved = false;

            if (preferences_silentEvents != null)
                idremoved = preferences_silentEvents.remove(key);

            if (preferences_silentEventsRawIds != null && !TextUtils.isEmpty(keyWithRawId))
                idremoved = idremoved | preferences_silentEventsRawIds.remove(keyWithRawId);

            if (idremoved) {

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
                editor.putStringSet(context.getString(R.string.pref_Events_Silent_rawIds_key), preferences_silentEventsRawIds);

                //Если удалили последнее событие - переключаем режим на стандартный
                if (preferences_list_events_scope == Constants.pref_Events_Scope_Silenced && preferences_silentEvents.isEmpty()) {
                    preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                    editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
                }

                editor.apply();

            }

            //clearDeadLinksSilencedEvents();
            //if (preferences_debug_on) Toast.makeText(context, "Unsilenced event: " + key, Toast.LENGTH_LONG).show();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    boolean checkIsFavoriteEvent(@NonNull String key, String keyWithRawId, String isFavoriteContactEvent) {

        try {

            return (!key.isEmpty() && preferences_favoriteEvents != null && preferences_favoriteEvents.contains(key))
                    || (keyWithRawId != null && !keyWithRawId.isEmpty() && preferences_favoriteEventsRawIds != null && preferences_favoriteEventsRawIds.contains(keyWithRawId))
                    || (Constants.STRING_1.equals(isFavoriteContactEvent));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setFavoriteEvent(@NonNull String key, String keyWithRawId) {

        try {

            SharedPreferences.Editor editor = null;

            if (!key.isEmpty() && preferences_favoriteEvents != null) {
                preferences_favoriteEvents.add(key);
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Favorite_key), preferences_favoriteEvents);
            }

            if (!TextUtils.isEmpty(keyWithRawId) && preferences_favoriteEventsRawIds != null) {
                preferences_favoriteEventsRawIds.add(keyWithRawId);
                if (editor == null) editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Favorite_rawIds_key), preferences_favoriteEventsRawIds);
            }

            statFavoriteEventsCount++;
            if (editor != null) {
                editor.apply();
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    boolean unsetFavoriteEvent(@NonNull String key, String keyWithRawId) {

        try {

            //if (!checkIsFavoriteEvent(key, keyWithRawId)) return false;

            boolean idremoved = false;

            if (preferences_favoriteEvents != null)
                idremoved = preferences_favoriteEvents.remove(key);

            if (preferences_favoriteEventsRawIds != null && !TextUtils.isEmpty(keyWithRawId))
                idremoved = idremoved | preferences_favoriteEventsRawIds.remove(keyWithRawId);

            if (idremoved) {

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Favorite_key), preferences_favoriteEvents);
                editor.putStringSet(context.getString(R.string.pref_Events_Favorite_rawIds_key), preferences_favoriteEventsRawIds);

                //Если удалили последнее событие - переключаем режим на стандартный
                statFavoriteEventsCount--;
                if (preferences_list_events_scope == Constants.pref_Events_Scope_Favorite && statFavoriteEventsCount == 0) {
                    preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                    editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
                }

                editor.apply();

            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    @NonNull
    String getMergedID(@NonNull String linkID) {

        try {

            if (preferences_mergedIDs.isEmpty()) return Constants.STRING_EMPTY;

            String mergedID = preferences_mergedIDs.get(linkID);
            if (mergedID != null && !mergedID.isEmpty() && map_contacts_ids.containsKey(mergedID)) {
                return mergedID;
            } else {
                mergedID = getMergedRawID(linkID);
                if (!mergedID.isEmpty()) {
                    String contactIDMerged = map_contacts_rawIds.get(mergedID);
                    if (contactIDMerged != null && map_contacts_ids.containsKey(contactIDMerged)) {
                        return contactIDMerged;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return Constants.STRING_EMPTY;
    }

    boolean setMergedID(@NonNull String ID, String IDtoLink, String rawIDtoLink) {

        try {

            boolean idChanged = false;
            boolean rawIdChanged = false;

            if (preferences_mergedIDs.get(ID) != null) {
                if (IDtoLink == null) { //Удаляем существующий
                    preferences_mergedIDs.remove(ID);
                } else { //Заменяем
                    preferences_mergedIDs.remove(ID);
                    preferences_mergedIDs.put(ID, IDtoLink);
                }
                idChanged = true;
            } else if (IDtoLink != null) { //Добавляем новый
                preferences_mergedIDs.put(ID, IDtoLink);
                idChanged = true;
            }

            if (idChanged) {
                Set<String> mergedIDs = new HashSet<>();
                for (String elementID : preferences_mergedIDs.keySet()) {
                    final String elementValue = preferences_mergedIDs.get(elementID);
                    if (elementValue != null) {
                        mergedIDs.add(elementID + Constants.STRING_COLON_SPACE + elementValue);
                    }
                }

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_MergedID_key), mergedIDs);
                editor.apply();
            }

            if (preferences_mergedRawIDs.get(ID) != null) {
                if (rawIDtoLink == null) { //Удаляем существующий
                    preferences_mergedRawIDs.remove(ID);
                } else { //Заменяем
                    preferences_mergedRawIDs.remove(ID);
                    preferences_mergedRawIDs.put(ID, rawIDtoLink);
                }
                rawIdChanged = true;
            } else if (rawIDtoLink != null) { //Добавляем новый
                preferences_mergedRawIDs.put(ID, rawIDtoLink);
                rawIdChanged = true;
            }

            if (rawIdChanged) {
                Set<String> mergedRawIDs = new HashSet<>();
                for (String elementID : preferences_mergedRawIDs.keySet()) {
                    final String elementValue = preferences_mergedRawIDs.get(elementID);
                    if (elementValue != null) {
                        mergedRawIDs.add(elementID + Constants.STRING_COLON_SPACE + elementValue);
                    }
                }

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_MergedRawID_key), mergedRawIDs);
                editor.apply();
            }

            return idChanged || rawIdChanged;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    @NonNull
    String getMergedRawID(@NonNull String linkID) {

        try {

            return checkForNull(preferences_mergedRawIDs.get(linkID));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    int getXDaysEventsCount() {

        try {

            return preferences_xDaysEvents.isEmpty() ? 0 : preferences_xDaysEvents.size();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    boolean isXDaysEvent(@NonNull String eventId) {return preferences_xDaysEvents.containsKey(eventId);}

    @NonNull
    List<String> getXDaysEvent(@NonNull String eventId) {

        List<String> result = new ArrayList<>();
        try {

            final String eventData = preferences_xDaysEvents.get(eventId);
            if (eventData != null)
                result.addAll(Arrays.asList(eventData.split(Constants.STRING_PIPE, -1)));
            while (result.size() < 2) {
                result.add(Constants.STRING_EMPTY);
            }
            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return result;
        }
    }

    boolean setXDaysEvent(@NonNull String eventKey, String eventData) {
        try {

            if (isXDaysEvent(eventKey)) {
                if (eventData == null) { //Удаляем существующее
                    preferences_xDaysEvents.remove(eventKey);
                } else { //Заменяем
                    preferences_xDaysEvents.remove(eventKey);
                    preferences_xDaysEvents.put(eventKey, eventData.replace(Constants.STRING_COLON_SPACE, Constants.STRING_SPACE));
                }
            } else if (eventData != null) { //Добавляем новое
                preferences_xDaysEvents.put(eventKey, eventData.replace(Constants.STRING_COLON_SPACE, Constants.STRING_SPACE));
            } else {
                return false;
            }

            Set<String> someSets = new HashSet<>();
            for (String elementID : preferences_xDaysEvents.keySet()) {
                final String elementValue = preferences_xDaysEvents.get(elementID);
                if (elementValue != null) {
                    someSets.add(elementID + Constants.STRING_COLON_SPACE + elementValue);
                }
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_xDaysEvents_key), someSets);

            //Если удалили последнее событие - переключаем режим на стандартный
            if (preferences_list_events_scope == Constants.pref_Events_Scope_XDays && preferences_xDaysEvents.isEmpty()) {
                preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                editor.putInt(context.getString(R.string.pref_Events_Scope), preferences_list_events_scope);
            }

            editor.apply();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    /**
     * @param startDate Start date for period
     * @param endDate End date for period
     * @param eventDate Original event date
     * @param periods Events repeat periods (by comma)
     * @param toRepeat Positive: how many events to return (total), Negative: how many events of every period to return (from startDate)
     * @return ArrayList of events inside [startDate] ... [endDate] period
     */
    ArrayList<Event> getNextRepeatsForEvent(@NonNull Calendar startDate, @NonNull Calendar endDate, @NonNull Calendar eventDate, @NonNull String periods, int toRepeat) {
        try {

            ArrayList<Event> result = new ArrayList<>();
            Set<Long> selectedDates = new HashSet<>();

            if (toRepeat == 0) return result;
            if (startDate.after(endDate)) return result;

            String[] allPeriods = periods.split(Constants.STRING_COMMA, -1);
            for (String period : allPeriods) {
                int days = 0;
                int repeated = 0;
                int incremented = 0;
                try {
                    days = Integer.parseInt(period.trim());
                } catch (NumberFormatException e) { /**/ }
                if (days == 0) continue;

                Calendar date = (Calendar) eventDate.clone();

                if (toRepeat > 0) {
                    boolean isContinue = true;
                    while (isContinue) {
                        date.add(Calendar.DAY_OF_YEAR, days);
                        incremented += days;
                        if (date.compareTo(startDate) >= 0) {
                            if (date.compareTo(endDate) <= 0) { //Inside period
                                if (!selectedDates.contains(date.getTimeInMillis())) {
                                    repeated++;
                                    result.add(new Event(date.getTime(), countDaysDiffText(eventDate.getTime(), date.getTime(), 2)));
                                    selectedDates.add(date.getTimeInMillis());
                                    if (repeated >= toRepeat) isContinue = false;
                                }
                            } else { //Over
                                isContinue = false;
                            }
                        }
                    }

                } else {
                    for (int i = 1; i <= - toRepeat; i++) {
                        date.add(Calendar.DAY_OF_YEAR, days);
                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0) { //Inside period
                            if (!selectedDates.contains(date.getTimeInMillis())) {
                                result.add(new Event(date.getTime(), countDaysDiffText(eventDate.getTime(), date.getTime(), 2)));
                                selectedDates.add(date.getTimeInMillis());
                            }
                        }
                    }
                }

            }

            Collections.sort(result, (o1, o2) -> o1.date.compareTo(o2.date));
            if (toRepeat > 0) {
                while (result.size() > toRepeat) {
                    result.remove(result.size() - 1);
                }
            }
            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return null;
        }
    }

    void clearDeadLinksSilencedEvents() {

        try {

            int countRemoved = 0;

            //IDs
            Set<String> toRemoveIds = new HashSet<>();
            for (String event : preferences_silentEvents) {
                if (event.equals(Constants.STRING_EMPTY)) {
                    toRemoveIds.add(event);
                    continue;
                }
                final String[] keyParts = getKeyParts(event);
                if (keyParts[1].equals(getEventType(Constants.Type_CalendarEvent))) {
                    if (!idsAllCalendarEvents.contains(keyParts[0])) toRemoveIds.add(event);
                } else {
                    if (!map_contacts_ids.containsKey(keyParts[0])) toRemoveIds.add(event);
                }
            }
            if (!toRemoveIds.isEmpty()) {
                preferences_silentEvents.removeAll(toRemoveIds);
                countRemoved += toRemoveIds.size();
            }

            //RawIDs
            Set<String> toRemoveRawIds = new HashSet<>();
            for (String event : preferences_silentEventsRawIds) {
                if (event.equals(Constants.STRING_EMPTY)) {
                    toRemoveRawIds.add(event);
                    continue;
                }
                final String[] keyParts = getKeyParts(event);
                if (!map_contacts_rawIds.containsKey(keyParts[0])) toRemoveRawIds.add(event);
            }
            if (!toRemoveRawIds.isEmpty()) {
                preferences_silentEventsRawIds.removeAll(toRemoveRawIds);
                countRemoved += toRemoveRawIds.size();
            }


            if (countRemoved > 0) {
                ToastExpander.showInfoMsg(context, context.getString(R.string.msg_filter_clean_silenced_result) + countRemoved);
            }


        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void clearDeadLinksHiddenEvents() {
        //todo: добавить rawIds
        try {

            int countRemoved = 0;

            //IDs
            Set<String> toRemoveIds = new HashSet<>();
            for (String event : preferences_hiddenEvents) {
                if (event.equals(Constants.STRING_EMPTY)) {
                    toRemoveIds.add(event);
                    continue;
                }
                final String[] keyParts = getKeyParts(event);
                if (keyParts[1].equals(getEventType(Constants.Type_CalendarEvent))) {
                    if (!idsAllCalendarEvents.contains(keyParts[0])) toRemoveIds.add(event);
                } else {
                    if (!map_contacts_ids.containsKey(keyParts[0])) toRemoveIds.add(event);
                }
            }
            if (!toRemoveIds.isEmpty()) {
                preferences_hiddenEvents.removeAll(toRemoveIds);
                countRemoved += toRemoveIds.size();
            }

            //RawIDs
            Set<String> toRemoveRawIds = new HashSet<>();
            for (String event : preferences_hiddenEventsRawIds) {
                if (event.equals(Constants.STRING_EMPTY)) {
                    toRemoveRawIds.add(event);
                    continue;
                }
                final String[] keyParts = getKeyParts(event);
                if (!map_contacts_ids.containsKey(keyParts[0])) toRemoveRawIds.add(event);
            }
            if (!toRemoveRawIds.isEmpty()) {
                preferences_hiddenEventsRawIds.removeAll(toRemoveRawIds);
                countRemoved += toRemoveRawIds.size();
            }

            if (countRemoved > 0) {
                ToastExpander.showInfoMsg(context, context.getString(R.string.msg_filter_clean_hidden_result) + countRemoved);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void clearDeadLinksXDaysEvents() {

        try {

            if (getXDaysEventsCount() == 0) return;

            int countRemoved = 0;

            Iterator<Map.Entry<String,String>> iterator = preferences_xDaysEvents.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String,String> entry = iterator.next();
                final String eventID = entry.getKey();
                if (Constants.STRING_EMPTY.equals(eventID)) {
                    iterator.remove();
                    countRemoved++;
                } else if (!map_eventsBySubtypeAndPersonID_offset.containsKey(eventID)) {
                    iterator.remove();
                    countRemoved++;
                }
            }

            if (countRemoved > 0) {

                Set<String> someSets = new HashSet<>();
                for (String elementID : preferences_xDaysEvents.keySet()) {
                    final String elementValue = preferences_xDaysEvents.get(elementID);
                    if (elementValue != null) {
                        someSets.add(elementID + Constants.STRING_COLON_SPACE + elementValue);
                    }
                }

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_xDaysEvents_key), someSets);
                editor.apply();

                ToastExpander.showInfoMsg(context, context.getString(R.string.msg_filter_clean_XDays_result) + countRemoved);
            } else {
                ToastExpander.showInfoMsg(context, context.getString(R.string.msg_filter_clean_XDays_noresult));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void setWidgetPreference(int id, @NonNull String value) {

        if (context == null) return;

        try {

            setPreferenceString(context.getString(R.string.widget_config_PrefName) + id, value);
            ToastExpander.showDebugMsg(context, resources.getString(R.string.msg_widget_prefs_saved, String.valueOf(id), value));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @NonNull
    List<String> getWidgetPreference(int widgetID, String widgetType) {

        final String defaultPrefString;
        if (widgetType != null && widgetType.equals(Constants.WIDGET_TYPE_LIST)) {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref_List);
        } else if (widgetType != null && widgetType.equals(Constants.WIDGET_TYPE_PHOTO_LIST)) {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref_PhotoList);
        } else if (widgetType != null && widgetType.equals(Constants.WIDGET_TYPE_CALENDAR)) {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref_Calendar);
        } else {
            defaultPrefString = context.getString(R.string.widget_config_defaultPref);
        }

        List<String> defaultPref = Arrays.asList(defaultPrefString.split(Constants.STRING_COMMA, -1));
        if (context == null) return defaultPref;

        try {

            String strPref = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.widget_config_PrefName) + widgetID, defaultPrefString);
            String[] pref = strPref.split(Constants.STRING_COMMA, -1);
            List<String> prefWidget = new ArrayList<>(Arrays.asList(pref));

            //Заполнение значениями по умолчанию
            while (prefWidget.size() < defaultPref.size()) {
                prefWidget.add(defaultPref.get(prefWidget.size()));
            }

            return prefWidget;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return defaultPref;
        }

    }

    boolean hasPreferences(String name) {
        return PreferenceManager.getDefaultSharedPreferences(context).getAll().containsKey(name);
    }

    void removeWidgetPreference(int id) {

        try {

            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(context.getString(R.string.widget_config_PrefName) + id).apply();
            ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_widget_prefs_removed, String.valueOf(id)));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    Set<String> getPreferences_Accounts() {
        return preferences_Accounts;
    }

    void setPreferences_Accounts(Set<String> preferences_Accounts) {
        this.preferences_Accounts = preferences_Accounts;
    }

    Set<String> getPreferences_Calendars(@NonNull String eventType) {

        if (eventType.equals(getEventType(Constants.Type_BirthDay))) {
            return preferences_BirthDay_calendars;
        } else if (eventType.equals(getEventType(Constants.Type_Other))) {
            return preferences_OtherEvent_calendars;
        } else if (eventType.equals(getEventType(Constants.Type_HolidayEvent))) {
            return preferences_HolidayEvent_calendars;
        } else if (eventType.equals(Constants.Type_MultiEvent)) {
            return preferences_MultiType_calendars;
        } else {
            return new HashSet<>();
        }

    }

    void setPreferences_Calendars(@NonNull String eventType, Set<String> preferences_Calendars) {

        if (eventType.equals(getEventType(Constants.Type_BirthDay))) {
            this.preferences_BirthDay_calendars = preferences_Calendars;
        } else if (eventType.equals(getEventType(Constants.Type_Other))) {
            this.preferences_OtherEvent_calendars = preferences_Calendars;
        } else if (eventType.equals(getEventType(Constants.Type_HolidayEvent))) {
            this.preferences_HolidayEvent_calendars = preferences_Calendars;
        } else if (eventType.equals(Constants.Type_MultiEvent)) {
            this.preferences_MultiType_calendars = preferences_Calendars;
        }

    }

    void setPreferences_Files(String eventType, Set<String> preferences_Files) {

        if (eventType.equals(getEventType(Constants.Type_BirthDay))) {
            this.preferences_Birthday_files = preferences_Files;
        } else if (eventType.equals(getEventType(Constants.Type_Other))) {
            this.preferences_OtherEvent_files = preferences_Files;
        } else if (eventType.equals(getEventType(Constants.Type_HolidayEvent))) {
            this.preferences_HolidayEvent_files = preferences_Files;
        } else if (eventType.equals(Constants.Type_MultiEvent)) {
            this.preferences_MultiType_files = preferences_Files;
        }

    }

    void setPreferences_AlarmTime(int alarmHour, int alarmMinute) {
        this.preferences_notifications_alarm_hour = alarmHour;
        this.preferences_notifications_alarm_minute = alarmMinute;
    }

    int getPreferences_IconPackNumber() {
        return preferences_IconPackNumber;
    }

    void setPreferences_IconPackNumber(int packNumber) {
        preferences_IconPackNumber = packNumber;
    }

    void setPreferences_Icon(String iconName) {
        preferences_icon = iconName;
    }

    void setPreferences_ThemeNumber(int themeNumber) {
        preferences_theme.prefNumber = themeNumber;
    }

    void setPreferences_List_FontMagnify(int intDistance, int intName, int intDetails, int intDate, int intAge) {
        preferences_list_magnify_distance = intDistance;
        preferences_list_magnify_name = intName;
        preferences_list_magnify_details = intDetails;
        preferences_list_magnify_date = intDate;
        preferences_list_magnify_age = intAge;
    }

    @SuppressLint("DiscouragedApi")
    void showAnniversaryList(Context context) {

        try{

            List<String> items = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                @Nullable String anCaption;
                try {
                    anCaption = context.getString(getResources().getIdentifier(Constants.STRING_TYPE_WEDDING + i, Constants.RES_TYPE_STRING, context.getPackageName()));
                } catch (Resources.NotFoundException nfe) {
                    anCaption = null;
                }
                if (anCaption != null && !anCaption.equals(Constants.STRING_EMPTY))
                    items.add(i + Constants.STRING_COLON_SPACE + anCaption);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_CustomEvents_Anniversary_List_summary)
                    .setIcon(R.drawable.ic_event_wedding)
                    .setItems(items.toArray(new CharSequence[0]), null)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);
            AlertDialog alertToShow = builder.create();

            //todo: https://stackoverflow.com/questions/66504777/how-to-scroll-to-a-specific-list-position-inside-alertdialog

            alertToShow.setOnShowListener(arg0 -> {
                TypedArray ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return msg;
    }

    @NonNull
    String getDateFormatted(String dateIn, FormatDate format) {

        String resultString = Constants.STRING_EMPTY;
        if (TextUtils.isEmpty(dateIn)) return resultString;
        if (preferences_list_dateformat == 2 && format == FormatDate.WithYear)
            return dateIn; // DD.MM.YYYY

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
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        } else if (!isYearPresent || format == FormatDate.WithoutYear) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        }
                    }
            }

        } catch (Exception e) { /**/ }

        return resultString;

    }

    @NonNull
    String getDateTimePreferable(@NonNull Date dateIn) {

        String resultString = Constants.STRING_EMPTY;

        try {

            final Locale locale = Locale.forLanguageTag(currentLocale);
            SimpleDateFormat sdfOut;
            final String timeFormat = " HH:mm";

            switch (preferences_list_dateformat) {

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

                    resultString = DateUtils.formatDateTime(context, dateIn.getTime(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);

            }

        } catch (Exception e) { /**/ }

        return resultString;
    }

    @NonNull
    String getZodiacInfo(ZodiacInfo requestInfo, String strBirthday) {

        //todo: сделать вариант получения знака по уточнённым в 2016 года датам

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
                                                                                                                Constants.STRING_EMPTY : "♐".concat(requestInfo == ZodiacInfo.SIGN_TITLE ? this.resources.getString(R.string.zodiac_sign_sagittarius) : Constants.STRING_EMPTY) :
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
                    case 0:
                        return "\uD83D\uDC12".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_monkey) : Constants.STRING_EMPTY);
                    case 1:
                        return "\uD83D\uDC13".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rooster) : Constants.STRING_EMPTY);
                    case 2:
                        return "\uD83D\uDC15".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_dog) : Constants.STRING_EMPTY);
                    case 3:
                        return "\uD83D\uDC16".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_pig) : Constants.STRING_EMPTY);
                    case 4:
                        return "\uD83D\uDC00".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rat) : Constants.STRING_EMPTY);
                    case 5:
                        return "\uD83D\uDC02".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_ox) : Constants.STRING_EMPTY);
                    case 6:
                        return "\uD83D\uDC05".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_tiger) : Constants.STRING_EMPTY);
                    case 7:
                        return "\uD83D\uDC07".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_rabbit) : Constants.STRING_EMPTY);
                    case 8:
                        return "\uD83D\uDC09".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_dragon) : Constants.STRING_EMPTY);
                    case 9:
                        return "\uD83D\uDC0D".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_snake) : Constants.STRING_EMPTY);
                    case 10:
                        return "\uD83D\uDC0E".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_horse) : Constants.STRING_EMPTY);
                    case 11:
                        return "\uD83D\uDC11".concat(requestInfo == ZodiacInfo.YEAR_TITLE ? this.resources.getString(R.string.zodiac_year_sheep) : Constants.STRING_EMPTY);
                    default:
                        return Constants.STRING_EMPTY;
                }

            } else {

                return Constants.STRING_EMPTY;

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    @SuppressLint("MissingPermission")
    void quizCheckAndGo(String question, String answer) {

        try {

            final boolean isNotifyInterface = preferences_quiz_interface.equals(getResources().getString(R.string.pref_Quiz_Interface_Notify)) || !isUIOpen;

            if (isNotifyInterface && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //для Android 8+

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                NotificationChannel channel = notificationManager.getNotificationChannel(Integer.toString(Constants.defaultQuizID));

                if (channel == null) {
                    notificationManager.deleteNotificationChannel(Integer.toString(Constants.defaultQuizID));
                    channel = new NotificationChannel(Integer.toString(Constants.defaultQuizID), context.getString(R.string.pref_Notifications_Quiz_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                    channel.setSound(null, null);
                    channel.setShowBadge(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        channel.setAllowBubbles(true);
                    }
                    notificationManager.createNotificationChannel(channel);
                    ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_created_quiz_channel));
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
                    handler.post(() -> Toast.makeText(context, HtmlCompat.fromHtml((isNotifyInterface && Build.VERSION.SDK_INT < Build.VERSION_CODES.S ? "<b>" + question.replace(Constants.STRING_EOL, "</b><br>") + "<br><br>" : Constants.STRING_EMPTY) + "<font color='" + (a[0].equals("1") ? "#238A10" : "#dd0000") + "'>" + a[2].replace(Constants.STRING_EOL, "<br>") + Constants.HTML_COLOR_END, HtmlCompat.FROM_HTML_MODE_LEGACY), Toast.LENGTH_LONG).show());
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
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Integer.toString(Constants.defaultQuizID)).setColor(getResources().getColor(R.color.dark_green)).setSmallIcon(R.drawable.quiz_icon).setContentTitle(quest.type).setContentText(quest.question).setStyle(new NotificationCompat.BigTextStyle().bigText(quest.question)).setPriority(NotificationCompat.PRIORITY_MAX).setWhen(0).setAutoCancel(true);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { //
                    builder.setSound(Uri.parse("content://media/internal/audio/media/29"));
                }

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                builder.setContentIntent(pendingIntent);

                if (!TextUtils.isEmpty(quest.event)) {
                    builder.setLargeIcon(getEventPhoto(quest.event, true, true, false, preferences_list_photostyle));

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

                if (!checkNoNotificationAccess()) {
                    notificationManager.notify(Constants.defaultQuizID, builder.build());
                }

            } else {

                AlertDialog.Builder builder;
                AlertDialog alertToShow;
                QuizQuestion finalQuest = quest;

                builder = new AlertDialog.Builder(new ContextThemeWrapper(context, preferences_theme.themeDialog));
                builder.setTitle(quest.type);
                builder.setMessage(Constants.STRING_EOL + quest.question);
                if (quest.event != null && !quest.event.isEmpty()) {
                    builder.setIcon(new BitmapDrawable(resources, ContactsEvents.getInstance().getEventPhoto(quest.event, true, true, false, preferences_list_photostyle)));
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    QuizQuestion quizGetQuestion() {

        try {

            int seed = generator.nextInt(3);
            if (seed == 0) {

                return quizGetQuestionBirthdayMonth();

            } else if (seed == 1) {

                return quizGetQuestionBirthdayYear();

            } else {

                return quizGetQuestionContactAge();

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question), Constants.quiz_error_button_OK);
        }
    }

    /**
     * Вопрос "В каком месяце родился?"
     */
    QuizQuestion quizGetQuestionBirthdayMonth() {

        QuizQuestion result;

        try {

            if (birthdayDatesForIds.isEmpty()) return null;

            //Получаем случайный день рождения
            int tryEvent = 0;
            boolean isBirthday = false;
            String event = Constants.STRING_EMPTY;
            String[] eventInfo = null;
            Integer BMonth = -1;
            String BMonthLong = Constants.STRING_EMPTY;
            SimpleDateFormat sdfMonthLong = new SimpleDateFormat("LLLL", Locale.forLanguageTag(currentLocale));
            Date BDay;

            while (!isBirthday && tryEvent <= eventListUnsorted.size()) {
                tryEvent++;
                int randomInt = generator.nextInt(eventListUnsorted.size());
                event = eventListUnsorted.get(randomInt);
                eventInfo = event.split(Constants.STRING_EOT, -1);
                final String eventKey = getEventKey(eventInfo);
                final String eventKeyWithRawId = getEventKeyWithRawId(eventInfo);
                if (eventInfo[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId))) {
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDateThisTime]);
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
            final boolean isOrg = !eventInfo[Position_organization].trim().isEmpty();
            final boolean isTitle = !eventInfo[Position_title].trim().isEmpty();
            if (isOrg || isTitle) {
                personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                if (isOrg) {
                    personInfo.append(eventInfo[Position_organization].trim());
                    if (isTitle)
                        personInfo.append(Constants.STRING_COMMA_SPACE).append(eventInfo[Position_title].trim());
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
            for (Integer m : rollMonths) {
                boolean isBMonth = m.equals(BMonth);
                StringBuilder sb = new StringBuilder(isBMonth ? Constants.STRING_1 : Constants.STRING_0).append(Constants.STRING_EOT);
                cal.set(Calendar.DATE, 1);
                cal.set(Calendar.MONTH, m);
                String monthName = sdfMonthLong.format(cal.getTime()).toUpperCase();
                sb.append(monthName).append(Constants.STRING_EOT);
                if (isBMonth) {
                    sb.append(getResources().getString(R.string.quiz_answer_true, BMonthLong, eventInfo[Position_eventDateFirstTime]));
                } else {
                    sb.append(getResources().getString(R.string.quiz_answer_false, monthName, BMonthLong, eventInfo[Position_eventDateFirstTime]));
                }
                result.actions.add(sb.toString());
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question) + Constants.STRING_COLON_SPACE + e, Constants.quiz_error_button_OK);
        }
    }

    /**
     * Вопрос "В каком году родился?"
     */
    QuizQuestion quizGetQuestionBirthdayYear() {

        QuizQuestion result;

        try {

            if (birthdayDatesForIds.isEmpty()) return null;

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
                final String eventKeyWithRawId = getEventKeyWithRawId(eventInfo);
                if (eventInfo[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId))) {
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDateFirstTime]);
                        if (BDay != null) {
                            Calendar cal = getCalendarFromDate(BDay);
                            BYear = cal.get(Calendar.YEAR);
                            BYearLong = Integer.toString(BYear);
                            if (BYear > 0 && BYear != 1604 && BYear != 1904)
                                isBirthday = true; //наверное, это dead code
                        }
                    } catch (Exception e) { /**/ }
                }
            }
            if (!isBirthday) return null;

            //Формируем информацию о персоне
            StringBuilder personInfo = new StringBuilder(preferences_list_nameformat == 2 ? eventInfo[Position_personFullNameAlt] : eventInfo[Position_personFullName]);
            final boolean isOrg = !eventInfo[Position_organization].trim().isEmpty();
            final boolean isTitle = !eventInfo[Position_title].trim().isEmpty();
            if (isOrg || isTitle) {
                personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                if (isOrg) {
                    personInfo.append(eventInfo[Position_organization].trim());
                    if (isTitle)
                        personInfo.append(Constants.STRING_COMMA_SPACE).append(eventInfo[Position_title].trim());
                } else {
                    personInfo.append(eventInfo[Position_title].trim());
                }
                personInfo.append(Constants.STRING_PARENTHESIS_CLOSE);
            }

            result = new QuizQuestion(getResources().getString(R.string.quiz_year01_title), personInfo.toString());
            result.event = event;

            //Заполняем варианты ответов
            List<Integer> rollYears = new ArrayList<>(3);
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

            for (Integer year : rollYears) {
                boolean isBYear = year.equals(BYear);
                StringBuilder sb = new StringBuilder(isBYear ? Constants.STRING_1 : Constants.STRING_0).append(Constants.STRING_EOT).append(year).append(Constants.STRING_EOT);
                if (isBYear) {
                    sb.append(getResources().getString(R.string.quiz_answer_true, BYearLong, eventInfo[Position_eventDateFirstTime]));
                } else {
                    sb.append(getResources().getString(R.string.quiz_answer_false, Integer.toString(year).toUpperCase(), BYearLong, eventInfo[Position_eventDateFirstTime]));
                }
                //Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
                result.actions.add(sb.toString());
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question) + Constants.STRING_COLON_SPACE + e, Constants.quiz_error_button_OK);
        }
    }

    /**
     * Вопрос "Сколько лет исполнится?"
     */
    QuizQuestion quizGetQuestionContactAge() {

        QuizQuestion result;

        try {

            if (birthdayDatesForIds.isEmpty()) return null;

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
                final String eventKeyWithRawId = getEventKeyWithRawId(eventInfo);
                if (eventInfo[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId))) {
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDateFirstTime]);
                        eventDay = sdf_DDMMYYYY.parse(eventInfo[Position_eventDateThisTime]);
                        if (BDay != null) {
                            Calendar cal = getCalendarFromDate(BDay);
                            BYear = cal.get(Calendar.YEAR);
                            if (BYear > 0 && BYear != 1604 && BYear != 1904)
                                isBirthday = true; //наверное, это dead code
                        }
                    } catch (Exception e) { /**/ }
                }
            }
            if (!isBirthday || eventDay == null) return null;

            //Формируем информацию о персоне
            Date currentDay = removeTime(Calendar.getInstance()).getTime();
            boolean isDead = deathDatesForIds.containsKey(eventInfo[Position_contactID]); //Но есть годовщина смерти
            boolean isPassedBDay = (getCalendarFromDate(eventDay).get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)) || (eventDay.equals(currentDay));
            StringBuilder personInfo = new StringBuilder(preferences_list_nameformat == 2 ? eventInfo[Position_personFullNameAlt] : eventInfo[Position_personFullName]);
            final boolean isOrg = !eventInfo[Position_organization].trim().isEmpty();
            final boolean isTitle = !eventInfo[Position_title].trim().isEmpty();
            if (isOrg || isTitle) {
                personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                if (isOrg) {
                    personInfo.append(eventInfo[Position_organization].trim());
                    if (isTitle)
                        personInfo.append(Constants.STRING_COMMA_SPACE).append(eventInfo[Position_title].trim());
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
            List<Integer> rollAges = new ArrayList<>(3);
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

            String ageLong = getAgeString(Age, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20).toUpperCase();

            for (Integer age : rollAges) {
                boolean isAge = age.equals(Age);
                String ageRollLong = getAgeString(age, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20).toUpperCase();
                StringBuilder sb = new StringBuilder(isAge ? Constants.STRING_1 : Constants.STRING_0).append(Constants.STRING_EOT).append(ageRollLong).append(Constants.STRING_EOT);
                if (isAge) {
                    sb.append(getResources().getString(R.string.quiz_answer_true, ageLong, eventInfo[Position_eventDateFirstTime]));
                } else {
                    sb.append(getResources().getString(R.string.quiz_answer_false, ageRollLong, ageLong, eventInfo[Position_eventDateFirstTime]));
                }
                //Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
                result.actions.add(sb.toString());
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);

            return new QuizQuestion(getResources().getString(R.string.quiz_msg_error_title), getResources().getString(R.string.quiz_msg_error_get_question) + Constants.STRING_COLON_SPACE + e, Constants.quiz_error_button_OK);
        }
    }

    synchronized List<String> getFilteredEventList(List<String> eventList, List<String> widgetPref) {

        List<String> resultList = new ArrayList<>();

        try {

            if (widgetPref == null || eventList.isEmpty()) return resultList;

            //Типы событий
            List<String> eventsPrefList = new ArrayList<>();
            if (widgetPref.size() > 3 && !widgetPref.get(3).isEmpty()) {
                eventsPrefList = Arrays.asList(widgetPref.get(3).split(Constants.REGEX_PLUS));
            }
            //Источники событий
            List<String> sourcesPrefList = new ArrayList<>();
            if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                sourcesPrefList = Arrays.asList(widgetPref.get(10).split(Constants.REGEX_PLUS));
            }

            for (String event : eventList) {

                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                boolean isVisibleEvent = false;
                boolean useEventListPrefs = true;

                final String eventType = singleEventArray[Position_eventType];
                String eventKey = getEventKey(singleEventArray);
                String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);

                if (!eventsPrefList.isEmpty()) {
                    useEventListPrefs = false;
                    isVisibleEvent = eventsPrefList.contains(eventType) && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId));
                }
                if (useEventListPrefs)
                    isVisibleEvent = preferences_list_event_types.contains(eventType) && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId));

                if (isVisibleEvent && !sourcesPrefList.isEmpty()) {
                    final String eventDates = singleEventArray[Position_dates];
                    isVisibleEvent = false;
                    for (String source: sourcesPrefList) {
                        if (eventDates.contains(source)) {
                            isVisibleEvent = true;
                            break;
                        }
                    }
                }

                if (isVisibleEvent) {
                    resultList.add(event);
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return resultList;
    }

    /**
     * @return True if battery optimization for this application is OFF
     */
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return true;
        }
    }

    /**
     * @return True if no access to contacts
     */
    boolean checkNoContactsAccess() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @return True if no access to calendars
     */
    boolean checkNoCalendarAccess() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED;
    }

    boolean checkNoStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        }
    }

    boolean checkNoNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    boolean checkCanExactAlarm() {
        boolean canExact = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager.canScheduleExactAlarms()) {
            //if (ContextCompat.checkSelfPermission(context, Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED) {
                canExact = true;
            }
        } else {
            canExact = true;
        }
        return canExact;
    }

    @NonNull
    //https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri
    String getPath(Context context, Uri uri) {

        try {

            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(Constants.STRING_COLON);
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + Constants.STRING_SLASH + split[1];
                    } else {
                        return "/storage/" + split[0] + Constants.STRING_SLASH + split[1];
                    }
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + Constants.STRING_SLASH + getDataColumn(context, uri, null, null);
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
                if (isGooglePhotosUri(uri) && uri.getLastPathSegment() != null) return uri.getLastPathSegment();
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
                return uri.getPath();
            } else {
                ToastExpander.showInfoMsg(context, uri.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

    String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        try {

            try (Cursor cursor = context.getContentResolver().query(uri, null, selection, selectionArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    final int indexName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (indexName > -1) {
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return null;

    }

    private void fillEmptyUserData(TreeMap<Integer, String> userData) {

        try {

            if (userData == null) return;

            for (int i = 0; i < Position_attrAmount; i++) {
                if (!userData.containsKey(i)) {
                    userData.put(i, Constants.STRING_EMPTY);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    String getCurrentParams() {

        try {

            String[] typeIDs = resources.getStringArray(R.array.pref_EventTypes_values);
            String[] typeNames = resources.getStringArray(R.array.pref_EventTypes_entries);
            StringBuilder listEventsTypes = new StringBuilder();
            for (int i = 0; i < typeIDs.length; i++) {
                if (preferences_list_event_types.contains(typeIDs[i])) {
                    if (listEventsTypes.length() > 0)
                        listEventsTypes.append(Constants.STRING_COMMA_SPACE);
                    listEventsTypes.append(typeNames[i]);
                }
            }
            if (map_calendars.isEmpty()) recieveCalendarList();
            final String result = resources.getString(R.string.msg_zero_events_body,
                    (preferences_list_event_types.isEmpty() ? Constants.FONT_COLOR_RED + resources.getString(R.string.msg_none) : Constants.FONT_COLOR_GREEN + listEventsTypes) + Constants.HTML_COLOR_END,
                    resources.getString(R.string.stats_permissions_accounts, ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END) + resources.getString(R.string.stats_permissions_contacts, !checkNoContactsAccess() ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END),
                    (
                            preferences_Accounts.isEmpty() ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_all)
                                    : !preferences_Accounts.contains(Constants.account_none) ? Constants.FONT_COLOR_GREEN + String.join(Constants.STRING_COMMA_SPACE, preferences_Accounts)
                                        : Constants.FONT_COLOR_RED + String.join(Constants.STRING_COMMA_SPACE, preferences_Accounts)
                    ) + Constants.HTML_COLOR_END,
                    resources.getString(R.string.stats_permissions_calendar, !checkNoCalendarAccess() ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END
                            : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END),
                    (preferences_BirthDay_calendars.isEmpty() ? Constants.STRING_MINUS
                            : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + replaceCalendarIDtoTitle(preferences_BirthDay_calendars, map_calendars) + Constants.HTML_COLOR_END),
                    (preferences_OtherEvent_calendars.isEmpty() ? Constants.STRING_MINUS
                            : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + replaceCalendarIDtoTitle(preferences_OtherEvent_calendars, map_calendars) + Constants.HTML_COLOR_END),
                    (preferences_HolidayEvent_calendars.isEmpty() ? Constants.STRING_MINUS
                            : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + replaceCalendarIDtoTitle(preferences_HolidayEvent_calendars, map_calendars) + Constants.HTML_COLOR_END),
                    (preferences_MultiType_calendars.isEmpty() ? Constants.STRING_MINUS
                            : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + replaceCalendarIDtoTitle(preferences_MultiType_calendars, map_calendars) + Constants.HTML_COLOR_END),
                    (preferences_Birthday_files.isEmpty() ? Constants.STRING_MINUS
                            : String.join(Constants.STRING_COMMA_SPACE, preferences_Birthday_files)),
                    (preferences_OtherEvent_files.isEmpty() ? Constants.STRING_MINUS
                            : String.join(Constants.STRING_COMMA_SPACE, preferences_OtherEvent_files)),
                    (preferences_HolidayEvent_files.isEmpty() ? Constants.STRING_MINUS
                            : String.join(Constants.STRING_COMMA_SPACE, preferences_HolidayEvent_files)),
                    (preferences_MultiType_files.isEmpty() ? Constants.STRING_MINUS
                            : String.join(Constants.STRING_COMMA_SPACE, preferences_MultiType_files)),
                    (preferences_list_events_scope < 2 ? Constants.FONT_COLOR_GREEN : Constants.FONT_COLOR_RED + substringBefore(Arrays.asList(
                            resources.getString(R.string.events_scope_not_hidden),
                            resources.getString(R.string.events_scope_all),
                            resources.getString(R.string.events_scope_hidden),
                            resources.getString(R.string.events_scope_silenced),
                            resources.getString(R.string.events_scope_xdays),
                            resources.getString(R.string.events_scope_unrecognized),
                            resources.getString(R.string.events_scope_favorite)).get(preferences_list_events_scope), Constants.STRING_PARENTHESIS_OPEN) + Constants.HTML_COLOR_END)
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return result;
            } else {
                return result
                        .replace(Constants.HTML_UL_START + Constants.HTML_LI, Constants.HTML_LI_API21)
                        .replace(Constants.HTML_LI, Constants.HTML_BR + Constants.HTML_LI_API21)
                        .replace(Constants.HTML_LI_END, Constants.STRING_EMPTY)
                        .replace(Constants.HTML_UL_START, Constants.STRING_EMPTY)
                        .replace(Constants.HTML_UL_END, Constants.STRING_EMPTY);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

    }

    private String replaceCalendarIDtoTitle(Set<String> setIDs, HashMap<String, String> mapTitles){

        StringBuilder sb = new StringBuilder();
        try {

            for(String id: setIDs){
                if (sb.length() > 0) sb.append(Constants.STRING_COMMA_SPACE);
                sb.append(mapTitles.containsKey(id)
                        ? checkForNull(mapTitles.get(id)).replace(Constants.STRING_EOT, Constants.STRING_PARENTHESIS_OPEN) + Constants.STRING_PARENTHESIS_CLOSE
                        : id);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return sb.toString();
    }

    @NonNull
    String getPreferenceString(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull String defValue) {
        try {
            return preferences.getString(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    void setPreferenceString(@NonNull String key, String value) {
        if (context == null) return;

        try {

            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(key, value)
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    int getPreferenceInt(@NonNull SharedPreferences preferences, @NonNull String key, int defValue) {
        try {
            return preferences.getInt(key, defValue);
        } catch (Exception e) {
            try {
                return Integer.parseInt(preferences.getString(key, Constants.STRING_0));
            } catch (Exception e2) {
                return defValue;
            }
        }
    }

    int getPreferenceInt(@NonNull SharedPreferences preferences, @NonNull String key, String defValue) {
        int defValueInt = 0;
        try {
            defValueInt = Integer.parseInt(defValue);
        } catch (NumberFormatException ignored) { /**/ }
        try {
            return preferences.getInt(key, defValueInt);
        } catch (Exception e) {
            try {
                return Integer.parseInt(preferences.getString(key, Constants.STRING_0));
            } catch (Exception e2) {
                return defValueInt;
            }
        }
    }

    @NonNull
    Set<String> getPreferenceStringSet(@NonNull SharedPreferences preferences, @NonNull String key, @NonNull Set<String> defValue) {
        try {
            return new HashSet<>(preferences.getStringSet(key, defValue));
        } catch (Exception e) {
            return new HashSet<>(defValue);
        }
    }

    boolean getPreferenceBoolean(@NonNull SharedPreferences preferences, @NonNull String key, boolean defValue) {
        try {
            return preferences.getBoolean(key, defValue);
        } catch (Exception e) {
            return defValue;
        }
    }

    void setAppIcon() {
        try {
            //https://stackoverflow.com/questions/54685889/using-activity-alias-does-not-reflect-on-app-icon
            PackageManager pm = context.getPackageManager();
            boolean atLeastOneActive = false;
            for (String iconID : resources.getStringArray(R.array.pref_Icon_values)) {
                try {
                    final String activityName = BuildConfig.APPLICATION_ID + "." + iconID;
                    if (preferences_icon.equals(iconID)) {
                        int state = pm.getComponentEnabledSetting(new ComponentName(BuildConfig.APPLICATION_ID, activityName));
                        if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                            ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_icon_changed, iconID));
                            pm.setComponentEnabledSetting(new ComponentName(BuildConfig.APPLICATION_ID, activityName), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        }
                        atLeastOneActive = true;
                    } else {
                        pm.setComponentEnabledSetting(new ComponentName(BuildConfig.APPLICATION_ID, activityName), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    }
                } catch (IllegalArgumentException e) { /**/ }
            }
            if (!atLeastOneActive) {
                try {
                    if (preferences_debug_on)
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_icon_changed , resources.getString(R.string.pref_Icon_default) + " (default)"));
                    pm.setComponentEnabledSetting(new ComponentName(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "." + resources.getString(R.string.pref_Icon_default)), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } catch (IllegalArgumentException e) { /**/ }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    enum FormatDate {
        WithYear, WithoutYear
    }

    enum ZodiacInfo {
        SIGN, SIGN_TITLE, YEAR, YEAR_TITLE
    }

    static class MyTheme {
        int prefNumber; //Номер в shared preferences
        int themeMain; //Тема основной активности
        int themePopup; //Тема всплывающего меню
        int themeDialog; //Тема диалогов
        int themeEditText; //Тема для EditText (если 0, то используется тема по-умолчанию)
    }
    static final int themeEditText_default = R.style.EditText_Default;

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

    boolean isContextHelpAvailable() {

        List<String> localesWithFullDocumentation = new ArrayList<>();
        localesWithFullDocumentation.add(resources.getString(R.string.pref_Language_en));
        localesWithFullDocumentation.add(resources.getString(R.string.pref_Language_ru));
        localesWithFullDocumentation.add(resources.getString(R.string.pref_Language_de));
        localesWithFullDocumentation.add(resources.getString(R.string.pref_Language_be));
        localesWithFullDocumentation.add(resources.getString(R.string.pref_Language_es));

        return localesWithFullDocumentation.contains(Locale.getDefault().getLanguage());

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

            return this.type + Constants.STRING_COMMA + this.question + Constants.STRING_COMMA + this.actions.toString();

        }

    }

    static boolean isRealContactEventID(String id) {return id != null && !id.startsWith(Constants.PREFIX_FileEventID);}

    @NonNull String setAgeFormatting(@NonNull String strAge) {

        try {

            String result = strAge;
            final String replacementXK = Constants.STRING_000 + Constants.STRING_SPACE;
            if (preferences_list_age_format.contains(resources.getString(R.string.pref_List_AgeFormat_Convert000toK)) && result.contains(replacementXK)) {
                result = result.replace(replacementXK, "K ");
            } else if (preferences_list_age_format.contains(resources.getString(R.string.pref_List_AgeFormat_SeparateThousands))) {
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
            if (!preferences_list_age_format.contains(resources.getString(R.string.pref_List_AgeFormat_AddPostfix))) {
                result = result.substring(0, result.indexOf(Constants.STRING_SPACE));
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return strAge;
        }

    }

    @NonNull
    static String substringBefore(String text, String sep) {
        if (text == null) return Constants.STRING_EMPTY;
        if (sep == null) return text;
        return text.contains(sep) ? text.substring(0, text.indexOf(sep)) : text;
    }

    @NonNull
    static String substringAfter(String text, String sep) {
        if (text == null) return Constants.STRING_EMPTY;
        if (sep == null) return text;
        return text.contains(sep) ? text.substring(text.indexOf(sep) + sep.length()) : text;
    }

    @NonNull
    static String substringBetween(String text, String sep1, String sep2) {
        if (text == null) return Constants.STRING_EMPTY;
        return substringBefore(substringAfter(text, sep1), sep2);
    }

    void disableDebugMsg() {

        try {

            preferences_debug_on = false;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences != null) {
                preferences
                        .edit()
                        .putBoolean(resources.getString(R.string.pref_Help_Debug_On_key), preferences_debug_on)
                        .apply();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    void disableInfoMsg() {

        try {

            preferences_info_on = false;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences != null) {
                preferences
                        .edit()
                        .putBoolean(resources.getString(R.string.pref_Help_InfoMsg_On_key), preferences_info_on)
                        .apply();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    String getInstallerInfo(@StringRes int decorString) {

        //https://stackoverflow.com/questions/5841161/get-application-name-from-package-name
        final PackageManager packageManager = context.getPackageManager();
        String installer = null;
        try {
            installer = packageManager.getInstallerPackageName(context.getPackageName());
        } catch (IllegalArgumentException ignored) { /**/ }
        if (installer == null) return Constants.STRING_EMPTY;
        try {
            installer = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(installer, PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException ignored) { /**/ }

        return decorString != 0 ? context.getString(decorString, installer)  : installer;

    }
    @NonNull
    String addLabelToEventType(int eventTypeId, @NonNull String eventLabel, String eventTitle) {

        String resultInfo = Constants.STRING_EMPTY;
        if (eventLabel.isEmpty()) return resultInfo;

        try {

            String keyForLabels;
            String keyForTitle = null;

            switch (eventTypeId) {
                case 0:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Birthday_Labels_key);
                    break;
                case 1:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Anniversary_Labels_key);
                    break;
                case 2:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_NameDay_Labels_key);
                    break;
                case 3:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Crowning_Labels_key);
                    break;
                case 4:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Death_Labels_key);
                    break;
                case 5:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Other_Labels_key);
                    break;
                case 6:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Holiday_Labels_key);
                    break;
                case 7:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Custom1_Labels_key);
                    keyForTitle = context.getString(R.string.pref_CustomEvents_Custom1_Caption_key);
                    break;
                case 8:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Custom2_Labels_key);
                    keyForTitle = context.getString(R.string.pref_CustomEvents_Custom2_Caption_key);
                    break;
                case 9:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Custom3_Labels_key);
                    keyForTitle = context.getString(R.string.pref_CustomEvents_Custom3_Caption_key);
                    break;
                case 10:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Custom4_Labels_key);
                    keyForTitle = context.getString(R.string.pref_CustomEvents_Custom4_Caption_key);
                    break;
                case 11:
                    keyForLabels = context.getString(R.string.pref_CustomEvents_Custom5_Labels_key);
                    keyForTitle = context.getString(R.string.pref_CustomEvents_Custom5_Caption_key);
                    break;
                default:
                    return resultInfo;
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String customLabels = getPreferenceString(preferences, keyForLabels, Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (customLabels.isEmpty()) {
                setPreferenceString(keyForLabels, eventLabel);
                resultInfo = resources.getString(R.string.msg_eventtype_label_set, eventLabel);
            } else {
                setPreferenceString(keyForLabels, customLabels.concat(Constants.STRING_COMMA).concat(eventLabel));
                resultInfo = resources.getString(R.string.msg_eventtype_label_added, eventLabel);
            }

            if (eventTypeId > 6 && eventTitle != null) {
                setPreferenceString(keyForTitle, eventTitle);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return resultInfo;

    }

    boolean isJubilee(int age, @NonNull String eventSubType) {
        try {
            if (age <= 0) return false;
            final String algorithm = String.valueOf(preferences_list_jubilee_algorithm);
            if (algorithm.equals(context.getString(R.string.pref_List_Jubilee_Algorithm_Every_10))) {
                return age % 10 == 0;
            } else if (algorithm.equals(context.getString(R.string.pref_List_Jubilee_Algorithm_Every_5))) {
                return age % 5 == 0;
            } else if (algorithm.equals(context.getString(R.string.pref_List_Jubilee_Algorithm_Every_Flex))) {
                if (eventSubType.equals(getEventType(Constants.Type_BirthDay))) {
                    if (age < 45) {
                        return Arrays.asList(1, 3, 5, 10, 14, 18, 21, 30).contains(age);
                    } else {
                        return age % 5 == 0;
                    }
                } else if (eventSubType.equals(getEventType(Constants.Type_Anniversary))) {
                    return age % 10 == 0;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    void getRecentColors() {

        try {
            preferences_RecentColors.clear();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            for (String value : getPreferenceString(preferences, context.getString(R.string.pref_Colors_Recent_key), Constants.STRING_EMPTY).split(Constants.STRING_COMMA_SPACE, -1)) {
                try {
                    preferences_RecentColors.add(Integer.parseInt(value));
                } catch (NumberFormatException ignored) { /**/ }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void setRecentColor(int newValue) {

        try {

           if (!preferences_RecentColors.contains(newValue)) {
               while(preferences_RecentColors.size() >= resources.getInteger(R.integer.pref_Colors_Recent_max)) {
                   preferences_RecentColors.remove(0);
               }
               preferences_RecentColors.add(newValue);
               SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
               editor.putString(context.getString(R.string.pref_Colors_Recent_key), TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_RecentColors));
               editor.apply();
           }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    float getTextSizeForWidgetText(List<String> widgetPref, int sizeFactor, double fontMagnify) {
        double magnify = fontMagnify;
        try {

            if (widgetPref != null && widgetPref.size() > 1 && !widgetPref.get(1).equals(Constants.STRING_0)) {
                switch (widgetPref.get(1)) {
                    case Constants.STRING_1:
                        magnify = magnify * 0.5;
                        break;
                    case Constants.STRING_2:
                        magnify = magnify * 0.65;
                        break;
                    case Constants.STRING_3:
                        magnify = magnify * 0.75;
                        break;
                    case Constants.STRING_4:
                        magnify = magnify * 0.85;
                        break;
                    case Constants.STRING_5:
                        magnify = magnify * 1;
                        break;
                    case Constants.STRING_6:
                        magnify = magnify * 1.2;
                        break;
                    case Constants.STRING_7:
                        magnify = magnify * 1.5;
                        break;
                    case Constants.STRING_8:
                        magnify = magnify * 1.75;
                        break;
                    case Constants.STRING_9:
                        magnify = magnify * 2.0;
                        break;
                }
            }
            return (float) (sizeFactor * magnify);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return sizeFactor;
    }

    @NonNull
    static String getHash(@NonNull String from) {
        return String.valueOf(Math.abs(from.hashCode()));
    }

    static class MultiCheckboxesAdapter extends ArrayAdapter<String> {

        private static final String TAG = "EventSourcesAdapter";
        private final List<Integer> images;
        private final List<String> packages;
        private final List<Integer> colorDots;
        private final TypedArray ta;
        private final PackageManager pm = getContext().getPackageManager();

        MultiCheckboxesAdapter(Context context, @NonNull List<String> items, List<Integer> images, List<String> packages, List<Integer> colorDots, TypedArray theme) {
            super(context, R.layout.settings_list_item_multiple_choice, items);
            this.images = images;
            this.packages = packages;
            this.colorDots = colorDots;
            this.ta = theme;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            try {

                CheckedTextView textView = view.findViewById(android.R.id.text1);

                if (ta != null) textView.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                textView.setTextSize(16);
                textView.setMaxLines(5);

                if (this.colorDots != null && this.colorDots.size() >= position - 1 && this.colorDots.get(position) != null) {
                    textView.setText(Html.fromHtml("<bold><font color=#"
                            + Integer.toHexString(this.colorDots.get(position) & 0x00ffffff)
                            + ">●</font></bold> " + textView.getText()));
                }

                if (this.images != null && this.packages != null && this.images.size() >= position - 1 && this.packages.size() >= position - 1) {
                    if (this.images.get(position) != null && this.images.get(position) != 0) {
                        Drawable icon = pm.getDrawable(this.packages.get(position), this.images.get(position), null);
                        if (icon != null) {
                            Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bmp);
                            icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            icon.draw(canvas);
                            Bitmap bitmapResized = Bitmap.createScaledBitmap(bmp, 100, 100, false);
                            bmp.recycle();
                            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(new BitmapDrawable(getContext().getResources(), bitmapResized), null, null, null);
                        }
                        textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, displayMetrics));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }

            return view;
        }

    }

    private synchronized static void setDisplayMetrics(DisplayMetrics ds) {displayMetrics = ds;}

    /**
     * day - date in yyyy-MM-dd format
     * */
    List<DayType> getDayTypes(@NonNull String day, @NonNull List<String> fromPacks) {
        try {

            List<DayType> types = new ArrayList<>();
            for (String packId: fromPacks) {
                final String key = packId.concat(Constants.STRING_COLON).concat(day);
                final String key_noYear = packId.concat(Constants.STRING_COLON).concat("-").concat(day.substring(4));
                if (preferences_DaysTypes.containsKey(key)){
                    types.add(new DayType(packId, preferences_DaysTypes.get(key)));
                } else if (preferences_DaysTypes.containsKey(key_noYear)) {
                    types.add(new DayType(packId, preferences_DaysTypes.get(key_noYear)));
                }
            }
            return types;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return null;
    }

    /**
     * day - date in yyyy-MM-dd format
     * */
    List<String> getDayInfo(@NonNull String day, @NonNull List<String> fromPacks) {
        try {

            List<String> dayInfo = new ArrayList<>();
            for (String packId: fromPacks) {
                final String key = packId.concat(Constants.STRING_COLON).concat(day);
                final String key_noYear = packId.concat(Constants.STRING_COLON).concat("-").concat(day.substring(4));
                if (preferences_DaysInfo.containsKey(key) && preferences_DaysInfo.get(key) != null){
                    dayInfo.addAll(Arrays.asList(checkForNull(preferences_DaysInfo.get(key)).split(Constants.STRING_EOT, -1)));
                } else if (preferences_DaysInfo.containsKey(key_noYear) && preferences_DaysInfo.get(key_noYear) != null) {
                    dayInfo.addAll(Arrays.asList(checkForNull(preferences_DaysInfo.get(key_noYear)).split(Constants.STRING_EOT, -1)));
                }
            }
            return dayInfo;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return null;
    }

    void clearDaysTypesAndInfo() {
        preferences_DaysTypes.clear();
        preferences_DaysInfo.clear();
    }

    @SuppressLint("DiscouragedApi")
    void fillDaysTypesFromFiles(List<String> fileHashes) {
        try {

            //Справочники праздников и выходных
            int eventsPackCount = 1;
            @SuppressLint("DiscouragedApi") int packId = getResources().getIdentifier(Constants.STRING_TYPE_HOLIDAY + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            while (packId > 0) {
                try {

                    String[] eventsPack = getResources().getStringArray(packId);
                    if (eventsPack.length > 1) {
                        final String packHash = getHash(Constants.eventSourceHolidayPrefix + eventsPack[0]);
                        if (fileHashes == null || fileHashes.contains(packHash)) {
                            Log.i("HOLIDAY", eventsPack[0] + Constants.STRING_PARENTHESIS_OPEN + packHash + Constants.STRING_PARENTHESIS_CLOSE);
                            for (int i = 1; i < eventsPack.length; i++) {
                                String eventsArray = eventsPack[i];
                                String[] days = eventsArray.split(Constants.STRING_EOL, -1);
                                fillDaysTypesFromFile(packHash, days, "🏖️ ");
                            }
                            preferences_DaysTypes.put(packHash, DayType.Type.Holiday);
                        }
                    }

                } catch (Resources.NotFoundException ignored) { /**/ }

                eventsPackCount++;
                packId = getResources().getIdentifier(Constants.STRING_TYPE_HOLIDAY + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            }

            //Файлы
            Set<String> fileList = preferences_HolidayEvent_files;
            if (fileList == null || fileList.isEmpty()) return;

            for (String file : fileList) {

                final String packHash = getHash(Constants.eventSourceFilePrefix + file);
                if (!preferences_DaysTypes.containsKey(packHash)) {
                    String[] fileDetails = file.split(Constants.STRING_PIPE);
                    Log.i("FILE", fileDetails[0] + Constants.STRING_PARENTHESIS_OPEN + packHash + Constants.STRING_PARENTHESIS_CLOSE);
                    String[] eventsArray = readFileToString(file, Constants.STRING_EOL).split(Constants.STRING_EOL, -1);
                    if (eventsArray[0].isEmpty()) {
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                        continue;
                    }
                    fillDaysTypesFromFile(packHash, eventsArray, "📁 ");
                }
                preferences_DaysTypes.put(packHash, DayType.Type.Holiday);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void fillDaysTypesFromFile(String packHash, String[] days, @NonNull String titlePrefix) {
        try {

            if (preferences_DaysTypes.containsKey(packHash)) return;

            for (String eventLine: days) {
                String day = eventLine.trim();

                if (day.isEmpty() || day.startsWith(Constants.STRING_HASH) || day.startsWith(Constants.STRING_DSLASH))
                    continue;

                final int indexComma = day.indexOf(Constants.STRING_COMMA);
                if (indexComma == -1) {
                    ToastExpander.showInfoMsg(getContext().getApplicationContext(), resources.getString(R.string.msg_event_parse_error, day));
                    continue;
                }

                final int indexFirstSpace = day.indexOf(Constants.STRING_SPACE);
                String flags;
                if (indexFirstSpace > -1 && indexFirstSpace > indexComma) {
                    flags = day.substring(indexComma + 1, indexFirstSpace);
                } else {
                    flags = day.substring(indexComma + 1);
                }

                Date dateEvent = null;
                String eventDateString = day.substring(0, indexComma);

                try {
                    dateEvent = sdf_DDMMYYYY.parse(eventDateString);
                } catch (Exception e1) {
                    try {
                        dateEvent = sdf_india.parse(eventDateString);
                    } catch (Exception e2) {
                        try {
                            dateEvent = sdf_uk.parse(eventDateString);
                        } catch (Exception e3) {
                            try {
                                dateEvent = sdf_java.parse(eventDateString);
                            } catch (Exception e4) {
                                //Не получилось распознать
                            }
                        }
                    }
                }
                if (dateEvent != null) {
                    final String eventTitle = titlePrefix + day.substring(indexFirstSpace + 1).trim();
                    final DayType.Type dayType = flags.contains("?") ? DayType.Type.Workday : DayType.Type.Holiday;
                    String key;
                    if (flags.contains(Constants.STRING_1)) {
                        key = packHash.concat(Constants.STRING_COLON).concat(sdf_java.format(dateEvent));
                    } else {
                        key = packHash.concat(Constants.STRING_COLON).concat(sdf_java_no_year.format(dateEvent));
                    }
                    preferences_DaysTypes.put(key, dayType);
                    if (preferences_DaysInfo.containsKey(key) && preferences_DaysInfo.get(key) != null) {
                        preferences_DaysInfo.put(key, checkForNull(preferences_DaysInfo.get(key)).concat(Constants.STRING_EOT).concat(eventTitle));
                    } else {
                        preferences_DaysInfo.put(key, eventTitle);
                    }
                } else {
                    ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, day));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void fillDaysTypesFromCalendars(List<String> calendarHashes, Calendar startPeriod, Calendar endPeriod) {
        try {

            if (checkNoCalendarAccess()) return;

            StringBuilder calIDs = new StringBuilder();
            for (String calHash: calendarHashes) {
                String calKey = calHash + sdf_DDMMYYYY.format(startPeriod.getTime()) + sdf_DDMMYYYY.format(endPeriod.getTime());
                if (!preferences_DaysTypes.containsKey(calKey)) {
                    for (String calId: preferences_HolidayEvent_calendars) {
                        if (getHash(Constants.eventSourceCalendarPrefix + calId).equals(calHash)) {
                            Log.i("CALENDAR", calId + Constants.STRING_PARENTHESIS_OPEN + calHash + Constants.STRING_PARENTHESIS_CLOSE);
                            if (calIDs.length() > 0)
                                calIDs.append(" OR " + CalendarContract.Events.CALENDAR_ID + " = ");
                            calIDs.append(calId);
                           break;
                        }
                    }
                    preferences_DaysTypes.put(calKey, DayType.Type.Holiday);
                }
            }
            if (calIDs.length() == 0) return;

            if (contentResolver == null) contentResolver = context.getContentResolver();
            String[] projection = new String[]{
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Instances.TITLE
            };
            ColumnIndexCache cache = new ColumnIndexCache();
            String selection = CalendarContract.Instances.CALENDAR_ID + " = " + calIDs;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startPeriod.getTimeInMillis());
            ContentUris.appendId(builder, endPeriod.getTimeInMillis());

            Cursor cursor = contentResolver.query(
                    builder.build(),
                    projection,
                    selection,
                    null,
                    "dtstart ASC"
            );
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    Log.i("EVENTS", String.valueOf(cursor.getCount()));

                    while (cursor.moveToNext()) {
                        Calendar dateStart = getCalendarFromDate(new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.BEGIN)))));
                        Calendar dateEnd = getCalendarFromDate(new Date(parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.END)))));

                        if (cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Events.ALL_DAY)) == 1) { //У AllDay событий зона всегда UTC
                            if (TimeZone.getDefault().getRawOffset() < 0) { //Для отрицательных зон надо прибавлять день
                                dateStart.add(Calendar.DATE, 1);
                            }
                        }

                        final String calId = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.CALENDAR_ID));
                        final String calHash = getHash(Constants.eventSourceCalendarPrefix + calId);
                        final String eventTitle = "📆 " + cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.TITLE));

                        while (dateStart.before(startPeriod)) {
                            dateStart.add(Calendar.DATE, 1);
                        }

                        do {
                            String key = calHash.concat(Constants.STRING_COLON).concat(sdf_java.format(dateStart.getTime()));
                            preferences_DaysTypes.put(key, DayType.Type.Holiday);
                            if (preferences_DaysInfo.containsKey(key) && preferences_DaysInfo.get(key) != null) {
                                preferences_DaysInfo.put(key, checkForNull(preferences_DaysInfo.get(key)).concat(Constants.STRING_EOT).concat(eventTitle));
                            } else {
                                preferences_DaysInfo.put(key, eventTitle);
                            }

                            dateStart.add(Calendar.DATE, 1);
                            if (dateStart.after(endPeriod)) break;
                        } while (dateStart.before(dateEnd));

                    }
                }
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @SuppressLint("DiscouragedApi")
    private boolean getHolidayEvents() {
        try {

            long statCurrentModuleStart = System.currentTimeMillis();

            if (preferences_HolidayEvent_ids.isEmpty()) return false;
            final TreeMap<Integer, String> userData = new TreeMap<>();
            Calendar now = new GregorianCalendar();

            int eventsPackCount = 1;
            int packId = getResources().getIdentifier(Constants.STRING_TYPE_HOLIDAY + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            while (packId > 0) {
                try {

                    String[] eventsPack = getResources().getStringArray(packId);
                    if (eventsPack.length > 1) {
                        final String packHash = getHash(Constants.eventSourceHolidayPrefix + eventsPack[0]);
                        if (preferences_HolidayEvent_ids.contains(packHash)) {

                            for (int i = 1; i < eventsPack.length; i++) {
                                String eventsArray = eventsPack[i];
                                String[] days = eventsArray.split(Constants.STRING_EOL, -1);
                                for (String eventLine: days) {
                                    String day = eventLine.trim();
                                    boolean isEndless = true;

                                    if (day.isEmpty() || day.startsWith(Constants.STRING_HASH) || day.startsWith(Constants.STRING_DSLASH))
                                        continue;

                                    final int indexComma = day.indexOf(Constants.STRING_COMMA);
                                    if (indexComma == -1) continue;

                                    final int indexFirstSpace = day.indexOf(Constants.STRING_SPACE);
                                    String flags;
                                    String eventTitle = null;
                                    if (indexFirstSpace > -1 && indexFirstSpace > indexComma) {
                                        flags = day.substring(indexComma + 1, indexFirstSpace);
                                        eventTitle = day.substring(indexFirstSpace + 1);
                                    } else {
                                        flags = day.substring(indexComma + 1);
                                    }
                                    if (flags.contains(Constants.STRING_1)) {
                                        isEndless = false;
                                    }

                                    Date dateEvent = null;
                                    String eventDateString = day.substring(0, indexComma);

                                    try {
                                        dateEvent = sdf_DDMMYYYY.parse(eventDateString);
                                    } catch (ParseException e1) {
                                        try {
                                            dateEvent = sdf_india.parse(eventDateString);
                                        } catch (ParseException e2) {
                                            try {
                                                dateEvent = sdf_uk.parse(eventDateString);
                                            } catch (ParseException e3) {
                                                try {
                                                    dateEvent = sdf_java.parse(eventDateString);
                                                } catch (ParseException e4) {
                                                    //Не получилось распознать
                                                }
                                            }
                                        }
                                    }

                                    if (dateEvent == null) continue;

                                    if (!isEndless && now.after(getCalendarFromDate(dateEvent)))
                                        continue; //Одиночное событие и оно прошло

                                    if (TextUtils.isEmpty(eventTitle)) continue;

                                    final String eventNewDate = Constants.EVENT_PREFIX_HOLIDAY_EVENT + Constants.STRING_COLON_SPACE
                                            + sdf_java.format(dateEvent) + Constants.STRING_COLON_SPACE
                                            + getHash(Constants.eventSourceHolidayPrefix + eventsPack[0]);

                                    userData.put(Position_personFullName, eventTitle);
                                    userData.put(Position_personFullNameAlt, eventTitle);
                                    userData.put(Position_eventCaption, getResources().getString(R.string.event_type_holiday)); //Наименование события
                                    userData.put(Position_eventType, getEventType(Constants.Type_HolidayEvent)); //Тип события
                                    userData.put(Position_eventSubType, getEventType(Constants.Type_HolidayEvent)); //Подтип события
                                    userData.put(Position_eventDateThisTime, eventDateString);
                                    userData.put(Position_eventDateFirstTime, eventDateString);
                                    userData.put(Position_dates, eventNewDate);
                                    userData.put(Position_eventIcon, Integer.toString(R.drawable.ic_event_holiday));
                                    userData.put(Position_eventEmoji, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_holiday_emoji) : "\uD83C\uDFD6️");
                                    userData.put(Position_eventStorage, Constants.STRING_STORAGE_HOLIDAYS); //Где искать событие по ID
                                    userData.put(Position_eventSource, getResources().getString(R.string.msg_source_info, eventsPack[0]));
                                    userData.put(Position_eventID, Constants.PREFIX_HolidayEventID + getHash(packHash + day));

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
                                    userData.clear();

                                }
                            }

                        }
                    }

                } catch (Resources.NotFoundException ignored) { /**/ }

                eventsPackCount++;
                packId = getResources().getIdentifier(Constants.STRING_TYPE_HOLIDAY + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            }

            statTimeGetHolidayEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }
}