/*
 * *
 *  * Created by Vladimir Belov on 18.06.2026, 20:20
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 18.06.2026, 20:17
 *
 */

package org.vovka.birthdaycountdown;


import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.LocaleManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.text.HtmlCompat;

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Синглтон {@link ContactsEvents} для управления данными и настройками приложения
 */
public class ContactsEvents {

    /**
     * Сортировка события в общем списке
     * Устанавливается в {@link ContactsEvents#getSortKey}
     *
     */
    static final int Position_eventDate_sorted = 0;
    /**
     * Имя Отчество Фамилия (или Заголовок события)
     */
    public static final int Position_personFullName = 1;
    /**
     * Фамилия Имя Отчество
     */
    public static final int Position_personFullNameAlt = 2;
    /**
     * Тип события
     */
    static final int Position_eventCaption = 3;
    /**
     * Исходное наименование события
     */
    static final int Position_eventLabel = 4;
    /**
     * Ник
     */
    static final int Position_nickname = 5;
    /**
     * Массив дат события: accountType: date: eventHash
     */
    static final int Position_dates = 6;
    /**
     * Следующая дата появления события
     */
    static final int Position_eventDateNextTime = 7;
    /**
     * Дата возникновения события
     */
    static final int Position_eventDateFirstTime = 8;
    /**
     * Число дней до события
     */
    static final int Position_eventDistance = 9;
    /**
     * Число дней до события с дополнительной информацией.
     * Устанавливается в {@link ContactsEvents#getEventDistanceText}
     */
    static final int Position_eventDistanceText = 10;
    /**
     * Наступающий возраст (число лет)
     */
    static final int Position_age = 11;
    /**
     * Наступающий возраст (полный формат)
     */
    static final int Position_age_caption = 12;
    /**
     * Организация
     */
    static final int Position_organization = 13;
    static final int Position_title = 14;
    /**
     * Путь до фото контакта или события
     */
    static final int Position_photo_uri = 15;
    /**
     * Иконка события
     */
    static final int Position_eventIcon = 16;
    /**
     * Эмодзи события
     */
    static final int Position_eventEmoji = 17; //https://www.piliapp.com/emoji/list/
    static final int Position_starred = 18;
    /**
     * Текущий возраст
     */
    static final int Position_age_current = 19;
    /**
     * Тип события
     */
    static final int Position_eventType = 20;
    /**
     * Подтип события
     */
    static final int Position_eventSubType = 21;
    /**
     * ID контакта из адресной книги
     */
    static final int Position_contactID = 22;
    /**
     * ID события
     */
    static final int Position_eventID = 23;
    /**
     * Raw ID контакта из адресной книги (для составных контактов от нескольких провайдеров)
     */
    static final int Position_rawContactID = 24;
    /**
     * Место хранения события
     */
    static final int Position_eventStorage = 25;
    /**
     * Источник события
     */
    static final int Position_eventSource = 26;
    /**
     * Знак зодиака контакта
     */
    static final int Position_zodiacSign = 27;
    /**
     * Зодиакальный год дня рождения контакта
     */
    static final int Position_zodiacYear = 28;
    /**
     * Web ссылка на событие
     */
    static final int Position_eventURL = 29;
    /**
     * Дополнительное описание события
     */
    static final int Position_eventDescription = 30;
    /**
     * Не ежегодное событие
     */
    static final int Position_notAnnualEvent = 31;
    /**
     * Данные фото контакта или события
     */
    static final int Position_photo = 32;
    /**
     * Размерность массива с данными события (для проверки целостности)
     */
    static final int Position_attrAmount = 33; //MAX

    /**
     * Хранимые Id типа события
     */
    private static final Map<Integer, String> eventTypesStoredIDs = createEventTypeStoredIDsMap();

    private static Map<Integer, String> createEventTypeStoredIDsMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(Constants.Type_BirthDay, Constants.EventType_BirthDay);
        map.put(Constants.Type_Anniversary, Constants.EventType_Anniversary);
        map.put(Constants.Type_Another, Constants.EventType_Another);
        map.put(Constants.Type_Custom, Constants.EventType_Custom);
        map.put(Constants.Type_5K, Constants.EventType_5K); //todo: можно удалить
        map.put(Constants.Type_Death, Constants.EventType_Death);
        map.put(Constants.Type_NameDay, Constants.EventType_NameDay);
        map.put(Constants.Type_Crowning, Constants.EventType_Crowning);
        map.put(Constants.Type_Custom1, Constants.EventType_Custom1);
        map.put(Constants.Type_Custom2, Constants.EventType_Custom2);
        map.put(Constants.Type_Custom3, Constants.EventType_Custom3);
        map.put(Constants.Type_Custom4, Constants.EventType_Custom4);
        map.put(Constants.Type_Custom5, Constants.EventType_Custom5);
        map.put(Constants.Type_CalendarEvent, Constants.EventType_Calendar); //todo: можно удалить
        map.put(Constants.Type_FileEvent, Constants.EventType_File); //todo: можно удалить
        map.put(Constants.Type_Xdays, Constants.EventType_Xdays); //todo: можно удалить
        map.put(Constants.Type_Other, Constants.EventType_Other);
        map.put(Constants.Type_Fact, Constants.EventType_Fact); //todo: можно удалить
        map.put(Constants.Type_HolidayEvent, Constants.EventType_Holiday);
        map.put(Constants.Type_Unrecognized, Constants.EventType_Unrecognized);
        return Collections.unmodifiableMap(map);
    }

    private static final Map<String, Integer> eventTypesIDs = createEventTypeIDsMap();

    private static Map<String, Integer> createEventTypeIDsMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<Integer, String> entry : eventTypesStoredIDs.entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        return Collections.unmodifiableMap(map);
    }


    /**
     * Возвращает идентификатор типа события
     *
     * @param typeId Хранимый Id типа события
     * @return Идентификатор
     */
    @NonNull
    static String getEventType(int typeId) {
        return StringUtils.getNotNullString(eventTypesStoredIDs.get(typeId));
    }

    /**
     * Возвращает хранимый Id типа события
     *
     * @param typeStr Идентификатор типа события
     * @return Хранимый Id типа события
     */
    @NonNull
    static Integer getEventTypeInt(@NonNull String typeStr) {
        Integer value = eventTypesIDs.get(typeStr);
        if (value != null) {
            return value;
        } else {
            return Constants.Type_Unrecognized;
        }
    }

    private static final String TAG = "ContactsEvents";
    private static final ContactsEvents ourInstance = new ContactsEvents();
    boolean flagIsUpdating = false;
    private Calendar today = AppDateUtils.getWithoutTime(new GregorianCalendar());

    //Константы
    final Set<String> prefs_EventTypes_Default = new HashSet<String>() {{
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER));
        add(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM));
    }};
    static final int Rules_Unrecognized_Type_Other = 1;
    static final int Rules_Unrecognized_Type_Unrecognized = 2;
    static final int Rules_Unrecognized_Skip = 3;

    final List<String> eventList = new ArrayList<>(); //Список всех событий
    final List<String> eventListUpdated = new ArrayList<>(); //Список всех событий (обновлённый)
    final List<String> eventListFacts = new ArrayList<>(); //Факты
    final List<String> eventListPrev = new ArrayList<>(); //Список предыдущих событий
    //final HashSet<String> idsWithDeathEvent = new HashSet<>(); //ID контактов с годовщиной смерти
    /**
     * Даты годовщин смерти по ID
     */
    final HashMap<String, Date> deathDatesForIds = new HashMap<>();
    /**
     * Даты годовщин смерти по имени
     */
    final HashMap<String, Date> deathDatesForNames = new HashMap<>();
    /**
     * Даты дней рождений по ID
     */
    final HashMap<String, Date> birthdayDatesForIds = new HashMap<>();
    /**
     * Даты дней рождений по имени
     */
    final HashMap<String, Date> birthdayDatesForNames = new HashMap<>();
    final HashSet<String> idsAllCalendarEvents = new HashSet<>(); //ID всех найденных событий календаря
    final HashMap<String, String> map_contacts_names = new HashMap<>(); //связка имён контактов с ID
    final HashMap<String, String> map_calendars = new HashMap<>(); //список всех календарей
    final HashMap<String, Integer> map_calendars_colors = new HashMap<>(); //цвета календарей
    /**
     * Индекс события до сортировки (или для eventListUnsorted)
     */
    final HashMap<String, Integer> map_eventsBySubtypeAndPersonID_offset = new HashMap<>();
    /**
     * Индекс события по типу и имени
     */
    final HashMap<String, Integer> map_eventsBySubtypeAndPersonName_offset = new HashMap<>();
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
    final Map<Integer, Integer> preferences_IconPackImages_M = new TreeMap<>();
    final Map<Integer, Integer> preferences_IconPackImages_F = new TreeMap<>();
    /**
     * Типы дней для календаря
     */
    final Map<String, DayType.Type> preferences_DaysTypes = new HashMap<>();
    /**
     * Данные о событиях для календаря
     */
    private final Map<String, String> preferences_DaysInfo = new HashMap<>();

    //Даты
    public void setToday() {
        today = AppDateUtils.getWithoutTime(new GregorianCalendar());
    }
    public Calendar getToday() {
        return today;
    }
    static final ThreadLocal<SimpleDateFormat> sdf_java = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_JAVA, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_java_G = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_JAVA_G, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_java_no_year = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_JAVA_NO_YEAR, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    final ThreadLocal<SimpleDateFormat> sdf_DDMMY = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_DD_MM_Y, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_DDMMYYYY = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_DDMMYYYY_G = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY_G, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    final ThreadLocal<SimpleDateFormat> sdf_DDMMYYYYHHMM = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_DDMM = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_DD_MM, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_MMMMDYYYY = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_MMMM_D_YYYY, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_ru = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_RUS, new Locale(Constants.LANG_RU));
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_uk = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_UK, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_uk_G = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_UK_G, Locale.UK);
            sdf.setLenient(false);
            return sdf;
        }
    };
    final ThreadLocal<SimpleDateFormat> sdf_uk_no_year = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_UK_NO_YEAR, Locale.UK);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_india = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_IND, Locale.UK);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_india_G = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_IND_G, Locale.UK);
            sdf.setLenient(false);
            return sdf;
        }
    };
    final ThreadLocal<SimpleDateFormat> sdf_india_no_year = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_IND_NO_YEAR, Locale.UK);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_YYYYMMDD_noDiv = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_NO_DIV, Locale.UK);
            sdf.setLenient(false);
            return sdf;
        }
    };
    static final ThreadLocal<SimpleDateFormat> sdf_YYYY = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_YYYY, Locale.US);
            sdf.setLenient(false);
            return sdf;
        }
    };

    /**
     * Форматтер для "MMMM d" (например: "February 15")
     */
    private final ThreadLocal<SimpleDateFormat> sdfOut_MMMM_d = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            // Дефолтное значение, будет перезаписано в initLocaleStrings()
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_MMMM_D, Locale.getDefault());
            sdf.setLenient(false);
            return sdf;
        }
    };

    /**
     * Форматтер для "d MMMM" (например: "15 февраля")
     */
    private final ThreadLocal<SimpleDateFormat> sdfOut_d_MMMM = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            // Дефолтное значение, будет перезаписано в initLocaleStrings()
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_D_MMMM, Locale.getDefault());
            sdf.setLenient(false);
            return sdf;
        }
    };

    /**
     * Возвращает нужный форматтер в зависимости от настроек даты
     */
    public SimpleDateFormat getSdfOut() {
        return (preferences_date_format == 3 || preferences_date_format == 5)
                ? sdfOut_MMMM_d.get()
                : sdfOut_d_MMMM.get();
    }

    private final HashMap<String, String> map_contacts_data = new HashMap<>(); //кеш данных о контактах
    private final HashMap<String, String> preferences_mergedIDs = new HashMap<>(); //жёсткая привязка события к определённому контакту по ContactID
    private final HashMap<String, String> preferences_mergedRawIDs = new HashMap<>(); //жёсткая привязка события к определённому контакту RawContactID
    private final HashMap<String, String> preferences_xDaysEvents = new HashMap<>();
    int currentTheme = 0;
    boolean needUpdateEventList = true;

    //Общие настройки
    boolean preferences_debug_on;
    boolean preferences_info_on;
    /**
     * Список включённых дополнительных функций, например {@link Constants#FEATURE_QUIZ}
     */
    Set<String> preferences_enabled_features = new HashSet<>();
    String preferences_language;
    String preferences_Icon;
    boolean preferences_menustyle_compact;
    public ColorTheme preferences_theme;
    int preferences_quiz_difficulty;
    int preferences_quiz_AutoNext;
    /**
     * Цвета дней
     */
    final HashMap<String, String> preferences_days_info = new HashMap<>();
    Set<String> preferences_quiz_questions = new HashSet<>();
    Set<String> preferences_quiz_sources = new HashSet<>();
    String preferences_first_names_female_custom;
    String preferences_first_names_male_custom;
    Matcher preferences_last_name_completions_male;
    Matcher preferences_last_name_completions_female;
    Matcher preferences_first_names_male;
    Matcher preferences_first_names_female;
    Matcher preferences_second_name_completions_male;
    Matcher preferences_second_name_completions_female;
    int preferences_jubilee_algorithm;
    private int preferences_sad_photo;
    FormatName preferences_name_format;
    int preferences_date_format;
    @Nullable
    Matcher preferences_death_labels;
    String preferences_birthday_calendars_rules;
    FormatName preferences_rules_calendars_name_format;
    FormatName preferences_rules_files_name_format;
    int preferences_rules_unrecognized;
    String preferences_customevent1_caption;
    String preferences_customevent2_caption;
    String preferences_customevent3_caption;
    String preferences_customevent4_caption;
    String preferences_customevent5_caption;
    @Nullable
    private Matcher preferences_birthday_labels;
    @Nullable
    private Matcher preferences_wedding_labels;
    @Nullable
    private Matcher preferences_nameday_labels;
    @Nullable
    private Matcher preferences_crowning_labels;
    @Nullable
    private Matcher preferences_another_event_labels;
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
    int preferences_local_events_photo_size;
    final private Set<String> preferences_hiddenEvents = new HashSet<>();
    final private Set<String> preferences_hiddenEventsRawIds = new HashSet<>();
    final private Set<String> preferences_silentEvents = new HashSet<>();
    final private Set<String> preferences_silentEventsRawIds = new HashSet<>();
    final private Set<String> preferences_favoriteEvents = new HashSet<>();
    final private Set<String> preferences_favoriteEvents_ids = new HashSet<>();
    final private Set<String> preferences_favoriteEventsRawIds = new HashSet<>();
    final private Set<String> preferences_favoriteEventsRawIds_ids = new HashSet<>();
    final Set<String> preferences_Accounts = new HashSet<>();
    Set<String> preferences_BirthDay_calendars = new HashSet<>();
    Set<String> preferences_OtherEvent_calendars = new HashSet<>();
    Set<String> preferences_HolidayEvent_calendars = new HashSet<>();
    Set<String> preferences_MultiType_calendars = new HashSet<>();
    Set<String> preferences_Birthday_files = new HashSet<>();
    Set<String> preferences_OtherEvent_files = new HashSet<>();
    Set<String> preferences_HolidayEvent_files = new HashSet<>();
    Set<String> preferences_MultiType_files = new HashSet<>();
    Set<String> preferences_FactEvent_files = new HashSet<>();
    /** Список id справочников встроенных государственных праздников */
    Set<String> preferences_HolidayEvent_ids = new HashSet<>();
    /** Список id справочников встроенных других праздников */
    Set<String> preferences_HolidayEvent_Other_ids = new HashSet<>();
    /** Список id справочников встроенных других событий */
    Set<String> preferences_OtherEvent_ids = new HashSet<>();
    Set<String> preferences_FactEvent_ids = new HashSet<>();
    final private Set<String> preferences_eventsWithoutYear = new HashSet<>();
    private int preferences_IconPackNumber;
    final List<Integer> preferences_RecentColors = new ArrayList<>();

    //Список событий
    int preferences_list_events_scope;
    Set<String> preferences_list_event_types;
    Set<String> preferences_list_event_info;
    String preferences_list_prev_events;
    int preferences_list_prev_events_scan_distance = 0;
    String preferences_list_custom_caption;
    String preferences_list_custom_todayevent_caption;
    int preferences_list_style;
    int preferences_list_photostyle;
    int preferences_list_filling;
    int preferences_list_margin;
    int preferences_list_top_padding;
    Set<String> preferences_list_age_format;
    @ColorInt
    int preferences_list_color_eventtoday;
    @ColorInt
    int preferences_list_color_eventsoon;
    int preferences_list_color_eventjubilee;
    int preferences_list_on_click_action;
    int preferences_list_magnify_distance;
    int preferences_list_magnify_name;
    int preferences_list_magnify_details;
    int preferences_list_magnify_date;
    int preferences_list_magnify_age;
    boolean preference_list_fastscroll;

    //Номера брать из R.string.pref_List_EventInfo_XXX
    private final Set<String> pref_List_Event_Info_Default = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("6", "1", "2", "3", "12", "5")));
    //Номера брать из R.string.pref_List_AgeFormat_XXX
    private final Set<String> pref_List_Age_Format_Default = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("1", "2", "3")));

    Set<String> preferences_list_EventSources = new HashSet<>();
    SearchDepth preferences_list_search_depth;
    int preferences_list_quick_action;

    //Уведомления
    int preferences_notifications_channel_id;
    int preferences_notifications2_channel_id;
    Set<String> preferences_notifications_days = new HashSet<>();
    Set<String> preferences_notifications2_days = new HashSet<>();
    int preferences_notifications_fact_event_count;
    int preferences_notifications2_fact_event_count;
    int preferences_notifications_alarm_hour;
    int preferences_notifications2_alarm_hour;
    int preferences_notifications_alarm_minute;
    int preferences_notifications2_alarm_minute;
    String preferences_notifications_ringtone;
    String preferences_notifications2_ringtone;
    int preferences_notifications_on_click_action;
    int preferences_notifications2_on_click_action;
    int preferences_notifications_smallicons_style;
    int preferences_notifications2_smallicons_style;

    /* preferences_notifications_style:
     *   0 - Одно общее уведомление
     *   1 - Каждое событие в отдельном уведомлении
     *   2 - Если событий меньше 3 => отдельные, иначе - общее
     *   3 - Если событий меньше 4 => отдельные, иначе - общее
     * */
    private int preferences_notifications_style;
    private int preferences_notifications2_style;
    private int preferences_notifications_priority;
    private int preferences_notifications2_priority;
    Set<String> preferences_notifications_types = new HashSet<>();
    Set<String> preferences_notifications2_types = new HashSet<>();
    Set<String> preferences_notifications_sources = new HashSet<>();
    Set<String> preferences_notifications2_sources = new HashSet<>();
    Set<String> preferences_notifications_details = new HashSet<>();
    Set<String> preferences_notifications2_details = new HashSet<>();
    private Set<String> preferences_notifications_quick_actions;
    private Set<String> preferences_notifications2_quick_actions;

    //Виджеты

    // Номера брать из R.string.pref_EventInfo_XXX_ID
    private final Set<String> pref_Widgets_EventInfo_Info_Default = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("1", "2", "3", "10")));

    int preferences_widgets_update_period;
    Set<String> preferences_widgets_event_info;
    String preferences_widgets_bottom_info;
    String preferences_widgets_bottom_info_2nd;
    int preferences_widgets_days_event_soon;
    @ColorInt
    int preferences_widgets_color_default;
    @ColorInt
    int preferences_widgets_color_widget_caption;
    @ColorInt
    int preferences_widgets_color_event_today;
    @ColorInt
    int preferences_widgets_color_event_soon;
    @ColorInt
    int preferences_widgets_color_event_far;
    int preferences_widgets_on_click_action;
    int pinnedWidgetId;

    //Статистика
    long statTimeGetContactEvents = 0;
    long statTimeGetCalendarEvents = 0;
    long statTimeGetFileEvents = 0;
    long statTimeGetHolidayEvents = 0;
    long statTimeGetFactEvents = 0;
    long statTimeComputeDates = 0;
    long statTimeUpdateWidgets = 0;
    long statLastComputeDates = 0;
    long statLastSearchSuggestion = 0;
    int statContactsEventCount = 0;
    int statCalendarsEventCount = 0;
    int statFilesEventCount = 0;
    int statLocalEventCount = 0;
    int statContactsTitleCount = 0;
    int statContactsOrganizationCount = 0;
    int statContactsNicknameCount = 0;
    int statContactsCount = 0;
    int statContactsURLCount = 0;
    int statEventsCount = 0;
    long statLastPausedForOtherActivity = 0;
    int statEventsPrevEventsFound = 0;
    int statFavoriteEventsCount = 0;
    int statActiveWidgets = 0;
    int statUnrecognizedEvents = 0;
    final HashMap<String, Integer> statEventSources = new HashMap<>();
    final HashMap<String, Integer> statEventSourcesIds = new HashMap<>();
    final HashMap<String, Integer> statEventTypes = new HashMap<>();
    private static final TreeMap<Integer, Date> chineseZodiacNewYearsDates = new TreeMap<>();

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
    protected ViewGroup coordinator;

    //Зависимые от языка константы
    String[] weekDaysShort;
    static final Map<String, Integer> zodiacSignStrings = new HashMap<>();
    private static final Map<Integer, Integer> chineseZodiacYearStrings = new HashMap<>();

    //Оптимизация обработки
    private final ExecutorService widgetUpdateExecutor = Executors.newSingleThreadExecutor();
    private Future<?> pendingUpdateTask = null; // Для отслеживания текущей задачи

    public interface EventsLoadCallback {
        void onEventsLoaded(boolean success);
    }

    private static final ExecutorService eventsExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EventsLoader");
                t.setPriority(Thread.NORM_PRIORITY);
                t.setDaemon(true);
                return t;
            });

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ContactsEvents() {
    }

    @NonNull
    public static ContactsEvents getInstance() {
        return ourInstance;
    }

    enum FormatDate {
        WithYear, WithoutYear
    }

    public enum FormatName {
        NameFirst, LastnameFirst
    }

    enum SearchDepth {
        ListEvents, AllEvents
    }

    public static class ColorTheme {
        int prefNumber; //Номер в shared preferences
        public int themeMain; //Тема основной активности
        int themePopup; //Тема всплывающего меню
        int themeDialog; //Тема диалогов
        int themeEditText; //Тема для EditText (если 0, то используется тема по-умолчанию)
    }

    static final int themeEditText_default = R.style.EditText_Default;

    public static class ColumnIndexCache implements AutoCloseable {
        //https://android.jlelse.eu/using-a-cache-to-optimize-data-retrieval-from-cursors-56f9eaa1e0d2

        final private HashMap<String, Integer> mMap = new HashMap<>();

        /**
         * Возвращает номер колонки
         *
         * @param cursor     Курсор
         * @param columnName Название колонки
         * @return Номер колонки или -1
         */
        public int getColumnIndex(Cursor cursor, String columnName) {
            if (!mMap.containsKey(columnName))
                mMap.put(columnName, cursor.getColumnIndex(columnName));
            Integer ind = mMap.get(columnName);
            return ind != null ? ind : -1;
        }

        void clear() {
            mMap.clear();
        }

        @Override
        public void close() {
            this.clear();
        }
    }

    static class Event implements Cloneable {
        /**
         * Наименование события
         */
        String caption = Constants.STRING_EMPTY;
        /**
         * Заголовок пользовательского события
         */
        String label = Constants.STRING_EMPTY;
        String type = Constants.STRING_EMPTY;
        String subType = Constants.STRING_EMPTY;
        @DrawableRes
        int icon = 0;
        String emoji = Constants.STRING_EMPTY;
        Date date;
        String distance;
        boolean needScanContacts = false;
        boolean useEventYear = true;

        public Event() {
        }

        public Event(Date date, String distance) {
            this.date = date;
            this.distance = distance;
        }

        @NonNull
        @Override
        public Event clone() {
            try {
                // Создаем поверхностную копию объекта
                Event cloned = (Event) super.clone();

                // Делаем глубокую копию для изменяемого поля Date
                if (this.date != null) {
                    cloned.date = (Date) this.date.clone();
                }

                return cloned;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    static class DayType {
        enum Type {
            Holiday, Workday, Common
        }

        final String sourceId;
        final Type type;

        public DayType(String sourceId, Type type) {
            this.sourceId = sourceId;
            this.type = type;
        }
    }

    /**
     * Дополнительные функции приложения
     */
    enum EnabledFeatures {

        QUIZ(Constants.FEATURE_QUIZ, R.string.pref_Quiz_title,
                R.string.pref_Feature_Quiz_description, android.R.drawable.ic_menu_compass),
        NOTIFY_Q2(Constants.FEATURE_NOTIFY_Q2, R.string.pref_Notifications2_title,
                R.string.pref_Feature_Notify_Q2_description, R.drawable.ic_menu_notifications2),
        SELECT_SOURCES(Constants.FEATURE_SELECT_SOURCES, R.string.pref_List_EventSources_title,
                R.string.pref_Feature_Select_Sources_description, android.R.drawable.ic_menu_agenda),
        MORE_SETTINGS(Constants.FEATURE_MORE_SETTINGS, R.string.pref_Feature_More_Settings_title,
                R.string.pref_Feature_More_Settings_description, R.drawable.ic_sysbar_quicksettings),
        NOTIFY_MORE_SETTINGS(Constants.FEATURE_NOTIFY_MORE_SETTINGS, R.string.pref_Feature_Notify_More_Settings_title,
                R.string.pref_Feature_Notify_More_Settings_description, R.drawable.ic_menu_notifications1),
        WIDGETS_MORE_SETTINGS(Constants.FEATURE_WIDGETS_MORE_SETTINGS, R.string.pref_Feature_Widgets_More_Settings_title,
                R.string.pref_Feature_Widgets_More_Settings_description, android.R.drawable.ic_menu_crop),
        TOOLS(Constants.FEATURE_TOOLS, R.string.pref_Tools_title,
                R.string.pref_Feature_Tools_description, android.R.drawable.ic_menu_manage),
        ADV_INFO(Constants.FEATURE_ADV_INFO, R.string.pref_Feature_Adv_Info_title,
                R.string.pref_Feature_Adv_Info_description, android.R.drawable.ic_menu_info_details),
        ADV_ACTIONS(Constants.FEATURE_ADV_ACTIONS, R.string.pref_Feature_Adv_Actions_title,
                R.string.pref_Feature_Adv_Actions_description, android.R.drawable.ic_menu_add);

        private final String code;
        private final int nameResId;
        private final int descriptionResId;
        private final int iconResId;

        EnabledFeatures(@NonNull String code, int nameResId, int descriptionResId, int iconResId) {
            this.code = code;
            this.nameResId = nameResId;
            this.descriptionResId = descriptionResId;
            this.iconResId = iconResId;
        }

        public String getCode() {
            return code;
        }

        public String getName(Context context) {
            return context.getResources().getString(nameResId);
        }

        public String getDescription(Context context) {
            return context.getResources().getString(descriptionResId);
        }

        public int getIcon() {
            return iconResId;
        }

    }

    public void setEnabledFeatures(Set<String> newSet) {
        preferences_enabled_features.clear();
        preferences_enabled_features.addAll(newSet);
    }

    /**
     * Включена ли доп. функция
     *
     * @param code Код доп. функции
     * @return True - включена
     */
    public boolean isFeatureEnabled(String code) {
        return preferences_enabled_features.contains(code);
    }

    /**
     * Адаптер для множественного выбора значений
     */
    static class MultiCheckboxesAdapter extends ArrayAdapter<String> {

        private static final String TAG = "MultiCheckboxesAdapter";
        private final List<String> descriptions;
        private final List<Integer> images;
        private final List<String> packages;
        private final List<Integer> colorDots;
        private final TypedArray ta;
        private final PackageManager pm = getContext().getPackageManager();

        /**
         * @param context      Контекст вызова
         * @param items        Список заголовков
         * @param descriptions Список описаний к заголовкам
         * @param images       Список иконок к заголовкам
         * @param packages     Пакеты для иконок
         * @param colorDots    Список цветов для показа цветных индикаторов слева от заголовков
         * @param theme        Цветовая тема
         */
        MultiCheckboxesAdapter(Context context,
                               @NonNull List<String> items,
                               List<String> descriptions,
                               List<Integer> images,
                               List<String> packages,
                               List<Integer> colorDots,
                               TypedArray theme) {
            super(context, R.layout.settings_list_item_multiple_choice, items);
            this.descriptions = descriptions;
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

                if (ta != null) {
                    textView.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                }
                textView.setTextSize(16);

                if (descriptions != null) {
                    textView.setMaxLines(10);
                    // 👇 Формируем двухстрочный текст: <b>Заголовок</b><br/>Описание
                    String title = getItem(position);
                    String desc = (position < descriptions.size())
                            ? descriptions.get(position) : Constants.STRING_EMPTY;

                    String htmlText = "<b>" + StringUtils.escapeHtml(title) + "</b><br/><small>" +
                            StringUtils.escapeHtml(desc) + "</small>";
                    textView.setText(HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY));
                }

                if (this.colorDots != null && this.colorDots.size() >= position - 1) {
                    @ColorInt Integer dotColor = this.colorDots.get(position);
                    if (dotColor != null) {
                        if (Color.alpha(dotColor) == 0 && ta != null)
                            dotColor = ta.getColor(R.styleable.Theme_dialogBackgroundColor, dotColor);
                        textView.setText(HtmlCompat.fromHtml(
                                Constants.FONT_COLOR_DOT_START
                                        + Integer.toHexString(dotColor & 0x00ffffff)
                                        + Constants.FONT_COLOR_DOT_END
                                        + textView.getText().toString()
                                , HtmlCompat.FROM_HTML_MODE_LEGACY));
                    }
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

    static class ZodiacHelper {
        private static final String TAG = "ZodiacHelper";

        /**
         * Получает символ знака зодиака по дате рождения
         *
         * @param strBirthday Дата рождения в формате "ДД.ММ.ГГГГ" или "ДД.ММ"
         * @return Символ знака зодиака или пустая строка в случае ошибки или некорректной даты
         * <a href="https://habr.com/ru/post/397729/">НАСА объясняет, что положение «знаков зодиака» давно изменилось</a>
         * <a href="https://ru.astro-seek.com/vychislit-kitayskiy-goroskop/">Календарь по дате</a>
         * <a href="https://www.astronet.ru/db/msg/1196222">Таблица дат начала года по китайскому календарю</a>
         */
        @NonNull
        public static String getZodiacSign(@NonNull String strBirthday) {
            try {
                char delimiter = Constants.STRING_PERIOD.charAt(0);
                String strDate;
                if (strBirthday.length() >= 10 && strBirthday.charAt(2) == delimiter && strBirthday.charAt(5) == delimiter) {
                    strDate = strBirthday;
                } else if (strBirthday.length() == 5 && strBirthday.charAt(2) == delimiter) {
                    strDate = strBirthday.concat(Constants.STRING_PERIOD);
                } else {
                    return Constants.STRING_EMPTY; //Некорректный формат даты
                }

                int eventDay = Integer.parseInt(strDate.substring(0, 2));
                int eventMonth = Integer.parseInt(strDate.substring(3, 5));

                if (eventMonth > 12 || eventMonth < 1 || eventDay > 31 || eventDay < 1) {
                    return Constants.STRING_EMPTY; //Некорректный день или месяц
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
                        "♐" :
                        "♏" :
                        "♎" :
                        "♍" :
                        "♌" :
                        "♋" :
                        "♊" :
                        "♉" :
                        "♈" :
                        "♓" :
                        "♒" :
                        "♑";

            } catch (Exception e) {
                Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
                return Constants.STRING_EMPTY;
            }
        }

        /**
         * Получает наименование знака зодиака по дате рождения.
         *
         * @param context     Контекст
         * @param strBirthday Дата рождения в формате "ДД.ММ.ГГГГ" (например, "21.03.1990").
         * @return Иконка и наименование знака зодиака или пустая строка в случае ошибки или некорректной даты.
         */
        @NonNull
        public static String getZodiacSignTitle(@NonNull Context context, @NonNull String strBirthday) {
            String zodiacSign = getZodiacSign(strBirthday);
            Integer stringResourceId = zodiacSignStrings.get(zodiacSign);
            if (stringResourceId != null) return zodiacSign + context.getString(stringResourceId);
            return Constants.STRING_EMPTY;
        }

        /**
         * Получает начало года по китайскому календарю
         *
         * @param year Год
         * @return Дата начала года по китайскому календарю
         */
        @Nullable
        private static Date getLunarNewYear(@NonNull Context context, int year) {
            try {

                if (chineseZodiacNewYearsDates.isEmpty()) initChineseZodiacNewYears(context);

                return chineseZodiacNewYearsDates.get(year);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
                return null;
            }
        }

        /**
         * Инициализирует данные начала годов в китайском календаре
         */
        private static void initChineseZodiacNewYears(@NonNull Context context) {
            try {

                chineseZodiacNewYearsDates.clear();

                String[] dateParts = context.getString(R.string.chinese_years_info).split(",", -1);
                for (String datePart : dateParts) {
                    try {
                        Date date = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(datePart.trim());
                        if (date != null) {
                            int year = Integer.parseInt(Objects.requireNonNull(sdf_YYYY.get()).format(date)); //Извлекаем год
                            chineseZodiacNewYearsDates.put(year, date);
                        }
                    } catch (ParseException | IllegalArgumentException e) {
                        Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }

        /**
         * Получает номер года по китайскому календарю по дате рождения.
         *
         * @param strBirthday Дата рождения в формате "ДД.ММ.ГГГГ" (например, "21.03.1990").
         * @return Номер года по китайскому календарю (от 0 до 11) или -1 в случае ошибки или некорректной даты.
         */
        private static int getChineseZodiacYearNumber(@NonNull Context context, @NonNull String strBirthday) {
            try {
                if (strBirthday.length() != 10 || strBirthday.charAt(2) != '.' || strBirthday.charAt(5) != '.') {
                    return -1; //Некорректный формат даты
                }

                Date eventDate;
                int eventYear = 0;
                Date lunarNewYear = null;
                try {
                    eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(strBirthday.trim());
                    if (eventDate != null) {
                        eventYear = Integer.parseInt(Objects.requireNonNull(sdf_YYYY.get()).format(eventDate));
                        lunarNewYear = getLunarNewYear(context, eventYear);
                    }
                } catch (ParseException | NumberFormatException e) {
                    Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
                    return -1;
                }
                if (lunarNewYear == null) return -1;

                int effectiveYear = eventYear;
                // Если дата рождения до Лунного Нового года — относится к предыдущему знаку
                if (!eventDate.after(lunarNewYear)) {
                    effectiveYear = eventYear - 1;
                }

                // Универсальный расчёт индекса знака Зодиака (работает и до 1900)
                return ((effectiveYear - 1900) % 12 + 12) % 12;

            } catch (Exception e) {
                Log.e(TAG, e.getMessage() != null ? e.getMessage() : e.toString());
                return -1;
            }
        }

        /**
         * Получает символ года по китайскому календарю по дате рождения.
         *
         * @param strBirthday Дата рождения в формате "ДД.ММ.ГГГГ" (например, "21.03.1990").
         * @return Символ года по китайскому календарю или пустая строка в случае ошибки или некорректной даты.
         */
        @NonNull
        public static String getChineseZodiacYearSymbol(@NonNull Context context, @NonNull String strBirthday) {
            int yearNumber = getChineseZodiacYearNumber(context, strBirthday);

            switch (yearNumber) {
                case 0:
                    return "🐀"; // Rat
                case 1:
                    return "🐂"; // Ox
                case 2:
                    return "🐅"; // Tiger
                case 3:
                    return "🐇"; // Rabbit
                case 4:
                    return "🐉"; // Dragon
                case 5:
                    return "🐍"; // Snake
                case 6:
                    return "🐎"; // Horse
                case 7:
                    return "🐑"; // Sheep
                case 8:
                    return "🐒"; // Monkey
                case 9:
                    return "🐓"; // Rooster
                case 10:
                    return "🐕"; // Dog
                case 11:
                    return "🐖"; // Pig
                default:
                    return Constants.STRING_EMPTY;
            }
        }

        /**
         * Получает символ и наименование года по китайскому календарю по дате рождения.
         *
         * @param strBirthday Дата рождения в формате "ДД.ММ.ГГГГ" (например, "21.03.1990").
         * @return Символ и наименование года по китайскому календарю или пустая строка в случае ошибки или некорректной даты.
         */
        @NonNull
        public static String getChineseZodiacYear(@NonNull Context context, @NonNull String strBirthday) {
            int yearNumber = getChineseZodiacYearNumber(context, strBirthday);
            String yearSymbol = getChineseZodiacYearSymbol(context, strBirthday);
            Integer stringResourceId = chineseZodiacYearStrings.get(yearNumber);
            if (stringResourceId != null)
                return yearSymbol.concat(context.getString(stringResourceId));
            return Constants.STRING_EMPTY;
        }
    }

    void initEventTypes(@NonNull List<String> eventTypesValues, @NonNull List<Integer> eventTypesIds, @NonNull List<Integer> eventSubTypesIds) {
        eventTypesValues.add(getResources().getString(R.string.event_type_birthday_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_birthday));
        eventTypesIds.add(Constants.Type_BirthDay);
        eventSubTypesIds.add(Constants.Type_BirthDay);
        eventTypesValues.add(getResources().getString(R.string.event_type_wedding_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_anniversary));
        eventTypesIds.add(Constants.Type_Anniversary);
        eventSubTypesIds.add(Constants.Type_Anniversary);
        eventTypesValues.add(getResources().getString(R.string.event_type_death_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_death));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Death);
        eventTypesValues.add(getResources().getString(R.string.event_type_crowning_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_crowning));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Crowning);
        eventTypesValues.add(getResources().getString(R.string.event_type_nameday_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_nameday));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_NameDay);
        eventTypesValues.add(getResources().getString(R.string.event_type_other_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_another));
        eventTypesIds.add(Constants.Type_Another);
        eventSubTypesIds.add(Constants.Type_Another);
        eventTypesValues.add(getResources().getString(R.string.event_type_holiday_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_holiday));
        eventTypesIds.add(Constants.Type_HolidayEvent);
        eventSubTypesIds.add(Constants.Type_HolidayEvent);
        eventTypesValues.add(getResources().getString(R.string.event_type_other_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.event_type_other));
        eventTypesIds.add(Constants.Type_Other);
        eventSubTypesIds.add(Constants.Type_Other);
        eventTypesValues.add(getResources().getString(R.string.event_type_custom1_emoji) + Constants.STRING_SPACE + (preferences_customevent1_caption.isEmpty() ? getResources().getString(R.string.event_type_custom) : preferences_customevent1_caption));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Custom1);
        eventTypesValues.add(getResources().getString(R.string.event_type_custom2_emoji) + Constants.STRING_SPACE + (preferences_customevent2_caption.isEmpty() ? getResources().getString(R.string.event_type_custom) : preferences_customevent2_caption));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Custom2);
        eventTypesValues.add(getResources().getString(R.string.event_type_custom3_emoji) + Constants.STRING_SPACE + (preferences_customevent3_caption.isEmpty() ? getResources().getString(R.string.event_type_custom) : preferences_customevent3_caption));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Custom3);
        eventTypesValues.add(getResources().getString(R.string.event_type_custom4_emoji) + Constants.STRING_SPACE + (preferences_customevent4_caption.isEmpty() ? getResources().getString(R.string.event_type_custom) : preferences_customevent4_caption));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Custom4);
        eventTypesValues.add(getResources().getString(R.string.event_type_custom5_emoji) + Constants.STRING_SPACE + (preferences_customevent5_caption.isEmpty() ? getResources().getString(R.string.event_type_custom) : preferences_customevent5_caption));
        eventTypesIds.add(Constants.Type_Custom);
        eventSubTypesIds.add(Constants.Type_Custom5);
    }

    /**
     * Считывает текущие настройки приложения и устанавливает язык для текущего контекста
     *
     * @param context Контекст
     */
    void initLanguage(@NonNull Context context) {
        setContext(context);
        getPreferences_Language(context);
        setLocale();
        getPreferences();
        initLocaleStrings();
    }

    void setContext(@NonNull Context con) {
        context = con.getApplicationContext();
        contentResolver = context.getContentResolver();
        displayMetrics = con.getResources().getDisplayMetrics();
        displayMetrics_density = displayMetrics.density;
    }

    /**
     * Установка языка (локали) приложению
     */
    void setLocale() {
        if (context == null) return;

        boolean isAutoMode = preferences_language.equals(context.getString(R.string.pref_Language_default));
        Locale targetLocale = isAutoMode ? Locale.getDefault() : new Locale(preferences_language);

        String targetLang = targetLocale.getLanguage();
        if (targetLang.isEmpty()) targetLang = "en";

        // Для Android 14+ — используем LocaleManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            LocaleManager lm = context.getSystemService(LocaleManager.class);
            if (!isAutoMode) {
                lm.setApplicationLocales(new LocaleList(targetLocale));
            } else {
                // Сбрасываем application locales → возвращаемся к системному языку
                lm.setApplicationLocales(new LocaleList());
            }
            // На Android 14+ не трогаем Resources — система сама управляет
        }

        // === Для ВСЕХ версий: обновляем наш resources ===
        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(targetLocale));
        } else {
            config.locale = targetLocale;
        }
        Locale.setDefault(targetLocale);

        // Обновляем наш внутренний resources
        Context localizedContext = context.createConfigurationContext(config);
        resources = localizedContext.getResources();

        // === ДОПОЛНИТЕЛЬНО: для Android < 14 — обновляем конфигурацию контекста ===
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Resources res = context.getResources();
            // updateConfiguration устарел, но необходим для Android < 14
            res.updateConfiguration(config, res.getDisplayMetrics());
        }

        currentLocale = targetLang;
    }

    /**
     * Без этого на Android 8 и 9 не меняет динамически язык
     *
     * @param context ContextWrapper
     */
    public void applyLocaleWorkaround(ContextWrapper context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Resources applicationRes = context.getBaseContext().getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            String localeCode = currentLocale != null && currentLocale.isEmpty()
                    ? currentLocale
                    : Locale.getDefault().getLanguage();
            applicationConf.setLocales(new LocaleList(new Locale(localeCode)));
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());
        }
    }

    private void initLocaleStrings() {

        weekDaysShort = resources.getStringArray(R.array.weekDaysShort);

        zodiacSignStrings.clear();
        zodiacSignStrings.put("♐", R.string.zodiac_sign_sagittarius);
        zodiacSignStrings.put("♏", R.string.zodiac_sign_scorpio);
        zodiacSignStrings.put("♎", R.string.zodiac_sign_libra);
        zodiacSignStrings.put("♍", R.string.zodiac_sign_virgo);
        zodiacSignStrings.put("♌", R.string.zodiac_sign_leo);
        zodiacSignStrings.put("♋", R.string.zodiac_sign_cancer);
        zodiacSignStrings.put("♊", R.string.zodiac_sign_gemini);
        zodiacSignStrings.put("♉", R.string.zodiac_sign_taurus);
        zodiacSignStrings.put("♈", R.string.zodiac_sign_aries);
        zodiacSignStrings.put("♓", R.string.zodiac_sign_pisces);
        zodiacSignStrings.put("♒", R.string.zodiac_sign_aquarius);
        zodiacSignStrings.put("♑", R.string.zodiac_sign_capricorn);

        chineseZodiacYearStrings.clear();
        chineseZodiacYearStrings.put(0, R.string.zodiac_year_rat);
        chineseZodiacYearStrings.put(1, R.string.zodiac_year_ox);
        chineseZodiacYearStrings.put(2, R.string.zodiac_year_tiger);
        chineseZodiacYearStrings.put(3, R.string.zodiac_year_rabbit);
        chineseZodiacYearStrings.put(4, R.string.zodiac_year_dragon);
        chineseZodiacYearStrings.put(5, R.string.zodiac_year_snake);
        chineseZodiacYearStrings.put(6, R.string.zodiac_year_horse);
        chineseZodiacYearStrings.put(7, R.string.zodiac_year_sheep);
        chineseZodiacYearStrings.put(8, R.string.zodiac_year_monkey);
        chineseZodiacYearStrings.put(9, R.string.zodiac_year_rooster);
        chineseZodiacYearStrings.put(10, R.string.zodiac_year_dog);
        chineseZodiacYearStrings.put(11, R.string.zodiac_year_pig);

        // Пересоздаём форматтеры с актуальной локалью
        final Locale currentLocale = Locale.forLanguageTag(this.currentLocale);
        sdfOut_MMMM_d.set(new SimpleDateFormat(Constants.DATE_MMMM_D, currentLocale));
        sdfOut_d_MMMM.set(new SimpleDateFormat(Constants.DATE_D_MMMM, currentLocale));
    }

    /**
     * Возвращает действие при нажатии на событие в виджете или списке событий
     *
     * @param eventInfo        Данные о событии в виде строки
     * @param eventText        Отображаемые данные о событии
     * @param singleEventArray Данные о событии в виде массива
     * @param prefAction       Предпочитаемое действие из настроек
     * @param context          Контекст
     * @return Действие
     */
    @Nullable
    static Intent getViewActionIntent(@NonNull String eventInfo, @NonNull String eventText, @NonNull String[] singleEventArray, int prefAction, Context context) {

        try {

            if (prefAction == 0) {

                return null;

            } else if (prefAction == 7) { // Запуск приложения

                Intent intentAction = new Intent(context, MainActivity.class);
                intentAction.setAction(Constants.ACTION_LAUNCH);
                intentAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return intentAction;

            } else if (prefAction == 8) { // Открыть меню

                Intent intentAction = new Intent(context, WidgetMenuActivity.class);
                intentAction.putExtra(Constants.EXTRA_CLICKED_EVENT, eventInfo);
                intentAction.putExtra(Constants.EXTRA_CLICKED_TEXT, eventText);
                intentAction.setAction(Constants.ACTION_MENU);
                intentAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                return intentAction;

            }

            if (singleEventArray.length < Position_attrAmount) {
                if (eventInfo.startsWith(context.getString(R.string.event_type_fact_emoji) + Constants.STRING_SPACE)) {
                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                    intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    intentShare.putExtra(Intent.EXTRA_TEXT, eventInfo);
                    intentShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent intentChooser = Intent.createChooser(intentShare, "");
                    intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    return intentChooser;
                } else {
                    return null;
                }
            }

            Uri uri = null;
            final String contactID = singleEventArray[Position_contactID];
            final boolean notEmptyContactID = !TextUtils.isEmpty(contactID);
            final String eventId = singleEventArray[Position_eventID];
            final boolean notEmptyEventId = !TextUtils.isEmpty(eventId);
            final String eventUrl = singleEventArray[Position_eventURL].trim();
            final boolean notEmptyEventUrl = !TextUtils.isEmpty(eventUrl);
            final boolean isFileOrHoliday = notEmptyEventId && (eventId.startsWith(Constants.PREFIX_FileEventID) || eventId.startsWith(Constants.PREFIX_HolidayEventID));

            if (Constants.STRING_STORAGE_EMBEDDED.equals(singleEventArray[Position_eventStorage])) {

                return null;

            } else if (Constants.EVENT_PREFIX_LOCAL_EVENT.equals(singleEventArray[Position_eventStorage])) {

                Intent intent = new Intent(context, LocalEventActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.EXTRA_EVENT_DATA, singleEventArray[Position_eventID]);
                return intent;

            } else if (prefAction == 1) { //Контакт, календарь, ссылка
                if (notEmptyContactID) {
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
                } else if (notEmptyContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                }

            } else if (prefAction == 3) { //Ссылка, контакт, календарь

                if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                } else if (notEmptyContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (notEmptyEventId && !isFileOrHoliday) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId);
                }

            } else if (prefAction == 4) { //Контакт, ссылка, календарь
                if (notEmptyContactID) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (notEmptyEventUrl) {
                    String[] eventURLs = eventUrl.split(Constants.STRING_2TILDA);
                    uri = Uri.parse(eventURLs[0].trim());
                } else if (notEmptyEventId && !isFileOrHoliday) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId);
                }
            }

            return uri != null ? new Intent(Intent.ACTION_VIEW, uri).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK) : null;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

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

    Context getContext() {
        return context;
    }

    @NonNull
    Resources getResources() {
        return context.getResources();
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
                        eventDistance.append(StringUtils.getAgeString(p.getYears(), R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20, currentLocale, resources)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    if (p.getMonths() > 0) {
                        eventDistance.append(StringUtils.getAgeString(p.getMonths(), R.string.msg_after_month_prefix_1, R.string.msg_after_month_prefix_1_, R.string.msg_after_month_prefix_2_3_4, R.string.msg_after_month_prefix_5_20, currentLocale, resources)).append(Constants.STRING_SPACE);
                        diffOnlyDays = false;
                    }
                    if (p.getDays() > 0) {
                        eventDistance.append(StringUtils.getAgeString(p.getDays(), R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, currentLocale, resources)).append(Constants.STRING_SPACE);
                    }
                }

            } else {

                Calendar calendarDateFrom;
                Calendar calendarDateTo;

                if (dateTo.before(dateFrom)) return Constants.STRING_EMPTY;

                calendarDateFrom = AppDateUtils.getWithoutTime(AppDateUtils.getCalendarFromDate(dateFrom));
                calendarDateTo = AppDateUtils.getWithoutTime(AppDateUtils.getCalendarFromDate(dateTo));

                int yearFrom = calendarDateFrom.get(Calendar.YEAR);
                int yearTo = calendarDateTo.get(Calendar.YEAR);
                int daysFromNYFrom = calendarDateFrom.get(Calendar.DAY_OF_YEAR);
                int daysFromNYTo = calendarDateTo.get(Calendar.DAY_OF_YEAR);

                if (yearFrom == yearTo) {
                    daysDiff = daysFromNYTo - daysFromNYFrom;
                } else {
                    int resD = daysFromNYTo + ((yearTo - yearFrom) * 365) - daysFromNYFrom;
                    daysDiff = resD + AppDateUtils.countLeapYearsBetween(yearFrom, yearTo);
                }

                if (components == 1 || components == 3) {

                    long delta = yearTo - yearFrom - (daysFromNYTo < daysFromNYFrom ? 1 : 0);
                    if (delta > 0) {
                        eventDistance.append(StringUtils.getAgeString(delta, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20, currentLocale, resources)).append(Constants.STRING_SPACE);
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
                        eventDistance.append(StringUtils.getAgeString(delta, R.string.msg_after_month_prefix_1, R.string.msg_after_month_prefix_1_, R.string.msg_after_month_prefix_2_3_4, R.string.msg_after_month_prefix_5_20, currentLocale, resources)).append(Constants.STRING_SPACE);
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
                        eventDistance.append(StringUtils.getAgeString(delta, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, currentLocale, resources)).append(Constants.STRING_SPACE);
                    }
                }
            }

            //(X days)
            if (!diffOnlyDays || components == 2) {
                if (components == 3) {
                    eventDistance.append(Constants.STRING_PARENTHESIS_START);
                }
                if (components == 2 || components == 3) {
                    eventDistance.append(StringUtils.getAgeString(daysDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, currentLocale, resources));
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

    void getPreferences_Language(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences_language = getPreferenceString(preferences, context.getString(R.string.pref_Language_key), context.getString(R.string.pref_Language_default));
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

            //Общие настройки
            preferences_debug_on = getPreferenceBoolean(preferences, context.getString(R.string.pref_Help_Debug_On_key), getResources().getBoolean(R.bool.pref_Help_Debug_On_default));
            preferences_info_on = getPreferenceBoolean(preferences, context.getString(R.string.pref_Help_InfoMsg_On_key), getResources().getBoolean(R.bool.pref_Help_InfoMsg_On_default));
            preferences_enabled_features = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_EnabledFeatures_key), new HashSet<>());
            preferences_Icon = getPreferenceString(preferences, context.getString(R.string.pref_Icon_key), context.getString(R.string.pref_Icon_default));
            preferences_IconPackNumber = getPreferenceInt(preferences, context.getString(R.string.pref_IconPack_key), 0);
            initIconPack();
            preferences_menustyle_compact = getPreferenceBoolean(preferences, context.getString(R.string.pref_MenuStyle_key), getResources().getBoolean(R.bool.pref_MenuStyle_default));
            preferences_days_info.clear();
            preferences_days_info.putAll(getPreferenceStringSetAsMap(preferences, getResources().getString(R.string.pref_DaysInfo_key), Constants.STRING_EOT));

            //Список событий
            preferences_list_event_types = getPreferenceStringSet(preferences, context.getString(R.string.pref_List_Events_key), prefs_EventTypes_Default);
            preferences_list_event_info = getPreferenceStringSet(preferences, context.getString(R.string.pref_List_EventInfo_key), pref_List_Event_Info_Default);
            preferences_list_prev_events = getPreferenceString(preferences, context.getString(R.string.pref_List_PrevEvents_key), context.getString(R.string.pref_List_PrevEvents_default));
            preferences_list_prev_events_scan_distance = getPreviousDaysScanDays(preferences_list_prev_events);
            preferences_list_style = getPreferenceInt(preferences, context.getString(R.string.pref_List_Style_key), context.getString(R.string.pref_List_Style_default));
            preferences_list_photostyle = getPreferenceInt(preferences, context.getString(R.string.pref_List_PhotoStyle_key), context.getString(R.string.pref_List_PhotoStyle_default));
            preferences_list_filling = getPreferenceInt(preferences, context.getString(R.string.pref_List_Filling_key), context.getString(R.string.pref_List_Filling_default));
            preferences_jubilee_algorithm = getPreferenceInt(preferences, context.getString(R.string.pref_List_Jubilee_Algorithm_key), context.getString(R.string.pref_List_Jubilee_Algorithm_default));
            preferences_list_margin = getPreferenceInt(preferences, context.getString(R.string.pref_List_Margin_key), context.getString(R.string.pref_List_Margin_default));
            preferences_list_top_padding = getPreferenceInt(preferences, context.getString(R.string.pref_List_TopPadding_key), 0);
            preferences_sad_photo = getPreferenceInt(preferences, context.getString(R.string.pref_List_SadPhoto_key), context.getString(R.string.pref_List_SadPhoto_default));
            preferences_name_format = getPreferenceInt(preferences, context.getString(R.string.pref_List_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default)) == 1 ? FormatName.NameFirst : FormatName.LastnameFirst;
            preferences_date_format = getPreferenceInt(preferences, context.getString(R.string.pref_List_DateFormat_key), context.getString(R.string.pref_List_DateFormat_default));
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
            preference_list_fastscroll = getPreferenceBoolean(preferences, context.getString(R.string.pref_List_FastScroll_key), getResources().getBoolean(R.bool.pref_List_FastScroll_default));
            preferences_list_EventSources = getPreferenceStringSet(preferences, context.getString(R.string.pref_List_EventSources_key), new HashSet<>());
            preferences_list_events_scope = getPreferenceInt(preferences, context.getString(R.string.pref_Events_Scope), Constants.pref_Events_Scope_NotHidden);
            preferences_list_search_depth = getSearchDepthFromPrefs(preferences);
            preferences_list_quick_action = getPreferenceInt(preferences, context.getString(R.string.pref_List_QuickAction_key), Constants.MainMenu_AddEvent);
            if (preferences_list_quick_action > 0 && preferences_list_quick_action > 100) {
                preferences_list_quick_action = Constants.MainMenu_AddEvent;
            }

            //Уведомления
            preferences_notifications_channel_id = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_ChannelID), Constants.defaultNotificationID);
            preferences_notifications2_channel_id = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_ChannelID), Constants.defaultNotification2ID);
            preferences_notifications_days = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications_Days_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_Days_values_default))));
            preferences_notifications_days.removeAll(new HashSet<String>() {{
                add(Constants.STRING_EMPTY);
            }});
            preferences_notifications2_days = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications2_Days_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications2_Days_values_default))));
            preferences_notifications2_days.removeAll(new HashSet<String>() {{
                add(Constants.STRING_EMPTY);
            }});

            preferences_notifications_sources = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_Notifications_EventSources_key), new HashSet<>());
            preferences_notifications2_sources = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_Notifications2_EventSources_key), new HashSet<>());
            preferences_notifications_details = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_Notifications_EventInfo_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_EventInfo_values_default))));
            preferences_notifications2_details = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_Notifications2_EventInfo_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_EventInfo_values_default))));
            preferences_notifications_style = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_Type_key), context.getString(R.string.pref_Notifications_Type_default));
            preferences_notifications2_style = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_Type_key), context.getString(R.string.pref_Notifications_Type_default));
            preferences_notifications_priority = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_Priority_key), context.getString(R.string.pref_Notifications_Priority_default));
            preferences_notifications2_priority = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_Priority_key), context.getString(R.string.pref_Notifications_Priority_default));
            preferences_notifications_fact_event_count = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_FactEvents_Count_key), context.getString(R.string.pref_Notifications_FactEvents_Count_default));
            preferences_notifications2_fact_event_count = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_FactEvents_Count_key), context.getString(R.string.pref_Notifications_FactEvents_Count_default));
            preferences_notifications_alarm_hour = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_AlarmHour_key), context.getString(R.string.pref_Notifications_AlarmHour_default));
            if (preferences_notifications_alarm_hour < 0)
                preferences_notifications_alarm_hour = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmHour_default));
            preferences_notifications2_alarm_hour = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_AlarmHour_key), context.getString(R.string.pref_Notifications_AlarmHour_default));
            if (preferences_notifications2_alarm_hour < 0)
                preferences_notifications2_alarm_hour = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmHour_default));
            preferences_notifications_alarm_minute = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_AlarmMinute_key), context.getString(R.string.pref_Notifications_AlarmMinute_default));
            if (preferences_notifications_alarm_minute < 0)
                preferences_notifications_alarm_minute = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmMinute_default));
            preferences_notifications2_alarm_minute = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_AlarmMinute_key), context.getString(R.string.pref_Notifications_AlarmMinute_default));
            if (preferences_notifications2_alarm_minute < 0)
                preferences_notifications2_alarm_minute = Integer.parseInt(context.getString(R.string.pref_Notifications_AlarmMinute_default));
            preferences_notifications_ringtone = getPreferenceString(preferences, context.getString(R.string.pref_Notifications_Ringtone_key), Settings.System.DEFAULT_NOTIFICATION_URI.toString());
            preferences_notifications2_ringtone = getPreferenceString(preferences, context.getString(R.string.pref_Notifications2_Ringtone_key), Settings.System.DEFAULT_NOTIFICATION_URI.toString());
            preferences_notifications_types = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications_Events_key), preferences_list_event_types); //По-умолчанию берём из списка событий
            preferences_notifications2_types = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications2_Events_key), preferences_list_event_types); //По-умолчанию берём из списка событий
            preferences_notifications_quick_actions = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications_QuickActions_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_QuickActions_values_default))));
            preferences_notifications2_quick_actions = getPreferenceStringSet(preferences, context.getString(R.string.pref_Notifications2_QuickActions_key), new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.pref_Notifications_QuickActions_values_default))));
            preferences_notifications_on_click_action = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_OnClick_key), context.getString(R.string.pref_Notifications_OnClick_default));
            preferences_notifications2_on_click_action = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_OnClick_key), context.getString(R.string.pref_Notifications_OnClick_default));
            preferences_notifications_smallicons_style = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications_SmallIconsStyle_key), context.getString(R.string.pref_Notifications_SmallIconsStyle_default));
            preferences_notifications2_smallicons_style = getPreferenceInt(preferences, context.getString(R.string.pref_Notifications2_SmallIconsStyle_key), context.getString(R.string.pref_Notifications_SmallIconsStyle_default));

            //Виджеты
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

            //Викторина
            preferences_quiz_difficulty = getPreferenceInt(preferences, getResources().getString(R.string.pref_Quiz_Difficulty_key), getResources().getInteger(R.integer.pref_Quiz_Difficulty_default));
            preferences_quiz_AutoNext = getPreferenceInt(preferences, getResources().getString(R.string.pref_Quiz_AutoNext_key), getResources().getInteger(R.integer.pref_Quiz_AutoNext_default));
            preferences_quiz_questions = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_Quiz_Questions_key), new HashSet<>());
            preferences_quiz_sources = getPreferenceStringSet(preferences, getResources().getString(R.string.pref_Quiz_EventSources_key), new HashSet<>());

            //Определения событий

            boolean useInternal;
            String customLabels;
            final String div_inter = "|"; //"\\Z|";
            //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

            //День рождения
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_default)));
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

            preferences_birthday_calendars_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_default)));
            preferences_Birthday_files = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key), new HashSet<>());

            //Свадьба
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_default)));
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
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_default)));
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
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_default)));
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
            useInternal = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Death_UseInternal_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_default)));
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

            //Другие события контакта
            customLabels = getPreferenceString(preferences, context.getString(R.string.pref_CustomEvents_Another_Labels_key), Constants.STRING_EMPTY).replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA);
            if (customLabels.isEmpty()) {
                preferences_another_event_labels = null;
            } else {
                preferences_another_event_labels = Pattern.compile(customLabels.replace(Constants.STRING_COMMA, div_inter), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
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
            preferences_HolidayEvent_Other_ids = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Holiday_Other_Ids_key), new HashSet<>());
            preferences_OtherEvent_ids = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Other_Embedded_key), new HashSet<>());

            //Факты
            preferences_FactEvent_files = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Fact_LocalFiles_key), new HashSet<>());
            preferences_FactEvent_ids = getPreferenceStringSet(preferences, context.getString(R.string.pref_CustomEvents_Fact_Bundled_Ids_key), new HashSet<>());

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
            preferences_customevent1_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom1_UseYear_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

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
            preferences_customevent2_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom2_UseYear_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

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
            preferences_customevent3_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom3_UseYear_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

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
            preferences_customevent4_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom4_UseYear_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

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
            preferences_customevent5_useyear = getPreferenceBoolean(preferences, context.getString(R.string.pref_CustomEvents_Custom5_UseYear_key), Boolean.parseBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            preferences_local_events_photo_size = getPreferenceInt(preferences, context.getString(R.string.pref_LocalEvents_PhotoSize_key), resources.getInteger(R.integer.pref_LocalEvents_PhotoSize_default));

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

            //Темы
            preferences_theme = new ColorTheme();
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

            preferences_hiddenEvents.clear();
            preferences_hiddenEvents.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Hidden_key), new HashSet<>()));
            preferences_hiddenEventsRawIds.clear();
            preferences_hiddenEventsRawIds.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Hidden_rawIds_key), new HashSet<>()));
            preferences_silentEvents.clear();
            preferences_silentEvents.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Silent_key), new HashSet<>()));
            preferences_silentEventsRawIds.clear();
            preferences_silentEventsRawIds.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Silent_rawIds_key), new HashSet<>()));
            preferences_favoriteEvents.clear();
            preferences_favoriteEvents.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Favorite_key), new HashSet<>()));
            preferences_favoriteEventsRawIds.clear();
            preferences_favoriteEventsRawIds.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Events_Favorite_rawIds_key), new HashSet<>()));
            preferences_eventsWithoutYear.clear();
            preferences_eventsWithoutYear.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_EventsWithoutYear_key), new HashSet<>()));

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

            preferences_Accounts.clear();
            preferences_Accounts.addAll(getPreferenceStringSet(preferences, context.getString(R.string.pref_Accounts_key), new HashSet<>()));

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
            preferences_rules_calendars_name_format = getPreferenceInt(preferences, context.getString(R.string.pref_CustomEvents_Rules_Calendars_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default)) == 1 ? FormatName.NameFirst : FormatName.LastnameFirst;
            preferences_rules_files_name_format = getPreferenceInt(preferences, context.getString(R.string.pref_CustomEvents_Rules_LocalFiles_NameFormat_key), context.getString(R.string.pref_List_NameFormat_default)) == 1 ? FormatName.NameFirst : FormatName.LastnameFirst;

            getRecentColors();

            dimen_List_details = resources.getDimension(R.dimen.event_details);
            dimen_List_name = resources.getDimension(R.dimen.event_name);
            dimen_list_date = resources.getDimension(R.dimen.event_date);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @NonNull
    private SearchDepth getSearchDepthFromPrefs(@NonNull SharedPreferences preferences) {
        SearchDepth result = SearchDepth.ListEvents;
        try {

            String depthDefault = context.getString(R.string.pref_List_SearchDepth_default);
            String depthAllEvents = context.getString(R.string.pref_List_SearchDeath_allEvents);
            String depthStored;

            if (preferences.contains(context.getString(R.string.pref_List_SearchDepth_pre186_key))) {
                depthStored = preferences.getString(context.getString(R.string.pref_List_SearchDepth_pre186_key), depthDefault);
            } else {
                depthStored = preferences.getString(context.getString(R.string.pref_List_SearchDepth_key), depthDefault);
            }
            if (depthStored.equals(depthAllEvents)) {
                result = SearchDepth.AllEvents;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return result;
    }

    /**
     * Обновление ярлыков действий для иконки приложения
     */
    void updateShortcuts() {
        //https://habr.com/ru/articles/593863/
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

            List<String> shortcutIdsToRemove = new ArrayList<>();

            if (!preferences_notifications_days.isEmpty() || (isFeatureEnabled(Constants.FEATURE_NOTIFY_Q2) && !preferences_notifications2_days.isEmpty())) {

                Intent intentNotify = new Intent(context, NotifyActivity.class);
                intentNotify.setAction(Intent.ACTION_VIEW);

                ShortcutInfoCompat shortcutNotify = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_NOTIFY)
                        .setShortLabel(resources.getString(R.string.shortcut_notify))
                        .setIcon(getTintedIcon(R.drawable.shortcut_notify))
                        .setIntent(intentNotify)
                        .setRank(1)

                        .build();
                try {
                    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutNotify);
                } catch (RuntimeException ignored) { /**/ }

            } else {
                shortcutIdsToRemove.add(Constants.SHORTCUT_NOTIFY);
            }

            if (!preferences_FactEvent_ids.isEmpty() || !preferences_FactEvent_files.isEmpty()) {

                Intent intentFactsPopup = new Intent(context, FactsPopupActivity.class);
                intentFactsPopup.setAction(Intent.ACTION_VIEW);
                ShortcutInfoCompat shortcutFactsPopup = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_FACTS)
                        .setShortLabel(resources.getString(R.string.shortcut_facts))
                        .setIcon(getTintedIcon(R.drawable.shortcut_facts))
                        .setIntent(intentFactsPopup)
                        .setRank(2)
                        .build();
                try {
                    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutFactsPopup);
                } catch (RuntimeException ignored) { /**/ }

            } else {
                shortcutIdsToRemove.add(Constants.SHORTCUT_FACTS);
            }

            if (isFeatureEnabled(Constants.FEATURE_QUIZ)) {
                Intent intentQuiz = new Intent(context, QuizActivity.class);
                intentQuiz.setAction(Intent.ACTION_VIEW);
                ShortcutInfoCompat shortcutQuiz = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_QUIZ)
                        .setShortLabel(resources.getString(R.string.shortcut_quiz))
                        .setIcon(getTintedIcon(R.drawable.shortcut_quiz))
                        .setIntent(intentQuiz)
                        .setRank(3)
                        .build();
                try {
                    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutQuiz);
                } catch (RuntimeException ignored) { /**/ }
            } else {
                shortcutIdsToRemove.add(Constants.SHORTCUT_QUIZ);
            }

            if (isFeatureEnabled(Constants.FEATURE_ADV_ACTIONS)) {
                Intent intentSettings = new Intent(context, SettingsActivity.class);
                intentSettings.setAction(Intent.ACTION_VIEW);
                ShortcutInfoCompat shortcutSettings = new ShortcutInfoCompat.Builder(context, Constants.SHORTCUT_SETTINGS)
                        .setShortLabel(resources.getString(R.string.shortcut_settings))
                        .setIcon(getTintedIcon(R.drawable.shortcut_settings))
                        .setIntent(intentSettings)
                        .setRank(4)
                        .build();
                try {
                    ShortcutManagerCompat.pushDynamicShortcut(context, shortcutSettings);
                } catch (RuntimeException ignored) { /**/ }

            } else {
                shortcutIdsToRemove.add(Constants.SHORTCUT_SETTINGS);
            }

            if (!shortcutIdsToRemove.isEmpty()) {
                ShortcutManagerCompat.removeDynamicShortcuts(context, shortcutIdsToRemove);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Возвращает иконку с фоном цвета текущей темы
     *
     * @param resId Id иконки
     * @return Изменённая иконка
     */
    IconCompat getTintedIcon(@DrawableRes int resId) {

        Drawable originalDrawable = ContextCompat.getDrawable(context, resId);
        Bitmap modifiedBitmap = null;
        if (originalDrawable instanceof BitmapDrawable) {
            Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
            int colorToReplace = context.getResources().getColor(R.color.dark_green);
            int newColor = getThemeBackColor();
            int colorTolerance = 10; // Допуск для "похожих" цветов (0 для точного совпадения)
            if (newColor != 0) {
                modifiedBitmap = ImageUtils.replaceColorInBitmap(originalBitmap, colorToReplace, newColor, colorTolerance);
            }
        }
        if (modifiedBitmap != null) {
            return IconCompat.createWithBitmap(modifiedBitmap);
        } else {
            return IconCompat.createWithResource(context, resId);
        }
    }

    /**
     * Возвращает цвет фона текущей темы
     *
     * @return Цвет
     */
    @ColorInt
    int getThemeBackColor() {
        if (context == null) return 0;
        Resources.Theme theme = context.getResources().newTheme();
        theme.applyStyle(preferences_theme.themeMain, true);
        try (TypedArray ta = theme.obtainStyledAttributes(R.styleable.Theme)) {
            int resId = ta.getResourceId(R.styleable.Theme_windowStatusbarColor, 0);
            return resId != 0
                    ? ResourcesCompat.getColor(getResources(), resId, theme)
                    : ta.getColor(R.styleable.Theme_windowStatusbarColor, 0);
        }
    }

    /**
     * Возвращает цвет заголовка окна текущей темы
     *
     * @return Цвет
     */
    @ColorInt
    int getThemeWindowTitleColor() {
        if (context == null) return 0;
        Resources.Theme theme = context.getResources().newTheme();
        theme.applyStyle(preferences_theme.themeMain, true);
        try (TypedArray ta = theme.obtainStyledAttributes(R.styleable.Theme)) {
            int resId = ta.getResourceId(R.styleable.Theme_windowTitleColor, 0);
            return ResourcesCompat.getColor(getResources(), resId, theme);
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
            editor.putInt(context.getString(R.string.pref_Notifications_ChannelID), preferences_notifications_channel_id);
            editor.putInt(context.getString(R.string.pref_Notifications2_ChannelID), preferences_notifications2_channel_id);
            editor.putString(context.getString(R.string.pref_Notifications_AlarmHour_key), Integer.toString(preferences_notifications_alarm_hour));
            editor.putString(context.getString(R.string.pref_Notifications2_AlarmHour_key), Integer.toString(preferences_notifications2_alarm_hour));
            editor.putString(context.getString(R.string.pref_Notifications_AlarmMinute_key), Integer.toString(preferences_notifications_alarm_minute));
            editor.putString(context.getString(R.string.pref_Notifications2_AlarmMinute_key), Integer.toString(preferences_notifications2_alarm_minute));
            editor.putString(context.getString(R.string.pref_Notifications_Ringtone_key), preferences_notifications_ringtone);
            editor.putString(context.getString(R.string.pref_Notifications2_Ringtone_key), preferences_notifications2_ringtone);
            editor.putStringSet(context.getString(R.string.pref_Accounts_key), getPreferences_Accounts());
            editor.putInt(context.getString(R.string.pref_IconPack_key), preferences_IconPackNumber);
            editor.putString(context.getString(R.string.pref_Theme_key), Integer.toString(preferences_theme.prefNumber));
            editor.putString(context.getString(R.string.pref_Icon_key), preferences_Icon);
            editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            editor.putStringSet(context.getString(R.string.pref_Events_Favorite_key), preferences_favoriteEvents);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_key), preferences_BirthDay_calendars);
            editor.putString(context.getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key), preferences_birthday_calendars_rules);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_Calendars_key), preferences_OtherEvent_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_Calendars_key), preferences_HolidayEvent_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_MultiType_Calendars_key), preferences_MultiType_calendars);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key), preferences_Birthday_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_Embedded_key), preferences_OtherEvent_ids);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Other_LocalFiles_key), preferences_OtherEvent_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_LocalFiles_key), preferences_HolidayEvent_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_MultiType_LocalFiles_key), preferences_MultiType_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Fact_LocalFiles_key), preferences_FactEvent_files);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_Other_Ids_key), preferences_HolidayEvent_Other_ids);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Holiday_Public_Ids_key), preferences_HolidayEvent_ids);
            editor.putStringSet(context.getString(R.string.pref_CustomEvents_Fact_Bundled_Ids_key), preferences_FactEvent_ids);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Distance_key), preferences_list_magnify_distance);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Name_key), preferences_list_magnify_name);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Details_key), preferences_list_magnify_details);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Date_key), preferences_list_magnify_date);
            editor.putInt(context.getString(R.string.pref_List_FontMagnify_Age_key), preferences_list_magnify_age);
            editor.putInt(context.getString(R.string.pref_List_QuickAction_key), preferences_list_quick_action);
            editor.putString(context.getString(R.string.pref_Notifications_FactEvents_Count_key), Integer.toString(preferences_notifications_fact_event_count));
            editor.putString(context.getString(R.string.pref_Notifications2_FactEvents_Count_key), Integer.toString(preferences_notifications2_fact_event_count));
            editor.putStringSet(context.getString(R.string.pref_List_EventSources_key), preferences_list_EventSources);
            editor.putStringSet(context.getString(R.string.pref_List_Events_key), preferences_list_event_types);
            editor.putStringSet(context.getString(R.string.pref_Notifications_EventSources_key), preferences_notifications_sources);
            editor.putStringSet(context.getString(R.string.pref_Notifications2_EventSources_key), preferences_notifications2_sources);
            editor.putStringSet(context.getString(R.string.pref_Quiz_Questions_key), preferences_quiz_questions);
            editor.putStringSet(context.getString(R.string.pref_Quiz_EventSources_key), preferences_quiz_sources);
            editor.putStringSet(context.getString(R.string.pref_EnabledFeatures_key), preferences_enabled_features);

            //Чистка
            editor.putString("ColorsResent", null);
            Map<String, ?> prefs = preferences.getAll();
            if (prefs.get(context.getString(R.string.pref_List_SearchDepth_key)) instanceof Integer) {
                editor.putString(context.getString(R.string.pref_List_SearchDepth_key), context.getString(R.string.pref_List_SearchDepth_default));
            }
            if (preferences.contains(context.getString(R.string.pref_List_SearchDepth_pre186_key))) {
                editor.putString(context.getString(R.string.pref_List_SearchDepth_key), preferences.getString(context.getString(R.string.pref_List_SearchDepth_pre186_key), context.getString(R.string.pref_List_SearchDepth_default)));
                editor.putString(context.getString(R.string.pref_List_SearchDepth_pre186_key), null);
            }

            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Считывание всех доступных событий
     */
    synchronized boolean getEvents() {

        if (flagIsUpdating || getContext() == null) return false;
        flagIsUpdating = true;

        try {

            eventListUpdated.clear();
            eventListPrev.clear();
            map_organizations.clear();
            map_contacts_titles.clear();
            map_contacts_aliases.clear();
            map_contacts_data.clear();
            map_contacts_ids.clear();
            map_contacts_rawIds.clear();
            deathDatesForIds.clear();
            deathDatesForNames.clear();
            birthdayDatesForIds.clear();
            birthdayDatesForNames.clear();
            map_events_weblinks.clear();
            map_notes.clear();
            idsAllCalendarEvents.clear();
            map_contacts_names.clear();
            map_eventsBySubtypeAndPersonID_offset.clear();
            map_eventsBySubtypeAndPersonName_offset.clear();
            clearDayInfoByHash(StringUtils.getHash(Constants.eventSourceFavoritePrefix).concat(Constants.STRING_COLON));

            statEventSources.clear();
            statEventSourcesIds.clear();
            statEventTypes.clear();
            statEventsCount = 0;
            statContactsEventCount = 0;
            statCalendarsEventCount = 0;
            statFilesEventCount = 0;
            statLocalEventCount = 0;
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

            boolean result = getContactsEvents()
                    | getLocalEvents()
                    | getCalendarEvents(Constants.EventType_BirthDay)
                    | getCalendarEvents(Constants.EventType_Other)
                    | getCalendarEvents(Constants.EventType_Holiday)
                    | getCalendarEvents(Constants.Type_MultiEvent)
                    | getFileEvents(Constants.EventType_BirthDay)
                    | getFileEvents(Constants.EventType_Other)
                    | getFileEvents(Constants.EventType_Holiday)
                    | getFileEvents(Constants.Type_MultiEvent)
                    | getHolidayEvents(preferences_HolidayEvent_ids, Constants.STRING_TYPE_HOLIDAY)
                    | getHolidayEvents(preferences_HolidayEvent_Other_ids, Constants.STRING_TYPE_OTHER_HOLIDAY)
                    | getOtherEvents(preferences_OtherEvent_ids, Constants.STRING_TYPE_OTHER_EVENT)
                    | getFactsEvents(true);

            statFavoriteEventsCount += getFavoritesEventsCount();

            if (result) {
                eventList.clear();
                eventList.addAll(eventListUpdated);
                eventListUpdated.clear();
                computeDates();
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        } finally {
            flagIsUpdating = false;
        }
    }

    private final AtomicBoolean pendingRefresh = new AtomicBoolean(false);

    public void getEventsAsync(@Nullable EventsLoadCallback callback) {
        if (pendingRefresh.getAndSet(true)) {
            // уже стоит в очереди — просто добавим колбэк в список, если нужно
            return;
        }
        eventsExecutor.execute(() -> {
            try {
                boolean result = getEvents();
                if (callback != null) {
                    mainHandler.post(() -> callback.onEventsLoaded(result));
                }
            } finally {
                pendingRefresh.set(false);
            }
        });
    }

    private boolean getContactsEvents() {
        //Получаем требуемые события (дни рождения, и т.п.)
        //todo: попробовать добраться до ДР стандартными способами https://stackoverflow.com/questions/35448250/how-to-get-whatsapp-contacts-from-android
        //todo: сделать импорт ДР одноклассники https://ruseller.com/lessons.php?id=1661 https://apiok.ru/ext/oauth/
        //todo: попробовать сделать агрегацию контактов

        // https://stackoverflow.com/questions/9419305/how-do-you-get-contacts-to-aggregate-properly-when-programmatically-adding-them
        // https://stackoverflow.com/questions/39804979/how-i-can-programmatically-merge-two-different-contactsandroid/39805494

        try {

            if (DeviceTools.checkNoContactsAccess(context)) return false;
            if (preferences_Accounts.contains(Constants.account_none)) return false;

            long statCurrentModuleStart = System.currentTimeMillis();
            TreeMap<Integer, String> eventData = new TreeMap<>();
            List<String> dataList = new ArrayList<>();

            if (contentResolver == null) contentResolver = context.getContentResolver();
            ColumnIndexCache cache = new ColumnIndexCache();

            //Организации и должности
            final String[] projectionOrgTitle = {
                    Constants.ColumnNames_CONTACT_ID,
                    ContactsContract.CommonDataKinds.Organization.COMPANY,
                    ContactsContract.CommonDataKinds.Organization.TITLE
            };
            Cursor contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionOrgTitle,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ_Q,
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
            final String[] projectionNick = {Constants.ColumnNames_CONTACT_ID, ContactsContract.CommonDataKinds.Nickname.NAME};
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionNick,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ_Q,
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
            final String[] projectionURL = {ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Website.URL};
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionURL,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ_Q,
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
            final String[] projectionNotes = {Constants.ColumnNames_CONTACT_ID, ContactsContract.CommonDataKinds.Note.NOTE};
            contactData = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projectionNotes,
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ_Q,
                    new String[]{ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE},
                    null
            );
            if (contactData != null) {
                if (contactData.moveToFirst()) {
                    do {

                        String personID = contactData.getString(cache.getColumnIndex(contactData, Constants.ColumnNames_CONTACT_ID));
                        String note = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.CommonDataKinds.Note.NOTE));
                        if (!map_notes.containsKey(personID))
                            map_notes.put(personID, note != null ? note.replace(Constants.STRING_EOL, Constants.STRING_SPACE) : Constants.STRING_EMPTY);

                    } while (contactData.moveToNext());
                    contactData.close();
                }
            }
            cache.clear();

            //Контакты
            final String[] projectionAllContacts = {
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
                        if (personName != null && personID != null) {
                            final String personNameNormalized = StringUtils.normalizeString(personName);
                            if (!TextUtils.isEmpty(personNameNormalized) && !map_contacts_names.containsKey(personNameNormalized)) {
                                map_contacts_names.put(personNameNormalized, personID);
                            }
                            map_contacts_data.put(personID.concat(ContactsContract.Data.DISPLAY_NAME), StringUtils.getNotNullString(personName));

                            //ИФ
                            if (!TextUtils.isEmpty(personNameNormalized)) {
                                final String personNameShortNormalized = Person.getShortName(personNameNormalized, Constants.pref_List_NameFormat_FirstSecondLast);
                                if (!map_contacts_names.containsKey(personNameShortNormalized)) {
                                    map_contacts_names.put(personNameShortNormalized, personID);
                                }
                            }
                        }

                        //ФИО
                        final String personNameAlt = contactData.getString(cache.getColumnIndex(contactData, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE));
                        if (personNameAlt != null) {
                            final String personNameAltNormalized = StringUtils.normalizeString(personNameAlt);
                            if (!TextUtils.isEmpty(personNameAltNormalized) && !map_contacts_names.containsKey(personNameAltNormalized)) {
                                map_contacts_names.put(personNameAltNormalized, personID);
                            }
                            map_contacts_data.put(personID + ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE, StringUtils.getNotNullString(personNameAlt));

                            //ФИ
                            if (!TextUtils.isEmpty(personNameAltNormalized)) {
                                final String personNameAltShortNormalized = Person.getShortName(personNameAltNormalized, Constants.pref_List_NameFormat_LastFirstSecond);
                                if (!map_contacts_names.containsKey(personNameAltShortNormalized)) {
                                    map_contacts_names.put(personNameAltShortNormalized, personID);
                                }
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
            final String[] projectionContactsEvents = {
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
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ_Q,
                    new String[]{ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE},
                    ContactsContract.Data.DISPLAY_NAME + Constants.SQL_SORT_ASC_CONT
                            + ContactsContract.CommonDataKinds.Event.TYPE + Constants.SQL_SORT_ASC_CONT
                            + ContactsContract.CommonDataKinds.Event.LABEL + Constants.SQL_SORT_ASC
            );
            if (cursor == null) return false;

            int countErrors = 0;
            String eventKey = Constants.STRING_EMPTY;

            if (cursor.moveToFirst()) {
                do {
                    try {
                        eventKey = getContactEventFromCursor(cursor, eventData, dataList, cache, eventKey);
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

                if (!eventData.isEmpty()) { // Данные последнего контакта
                    if (dataList.add(getEventData(eventData))) {
                        //Добавляем для поиска календарных событий (дни рождения)
                        String personID = eventData.get(Position_contactID);
                        if (!TextUtils.isEmpty(personID))
                            map_eventsBySubtypeAndPersonID_offset.put(personID + Constants.STRING_2HASH + eventData.get(Position_eventSubType), dataList.size() - 1);
                    }
                    eventData.clear();
                }
            }
            cache.clear();
            cursor.close();

            eventListUpdated.addAll(dataList);
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
        if (DeviceTools.checkNoContactsAccess(context)) return count;

        try {

            if (contentResolver == null) contentResolver = context.getContentResolver();

            final StringBuilder selection = new StringBuilder();
            selection.append(ContactsContract.Data.MIMETYPE).append(" = '").append(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE).append("' ");
            if (accountType != null) {
                if (Constants.STRING_NULL.equalsIgnoreCase(accountType)) {
                    selection.append(Constants.QUERY_PARAM_AND).append(Constants.ColumnNames_ACCOUNT_TYPE).append(" is null ");
                } else {
                    selection.append(Constants.QUERY_PARAM_AND).append(Constants.ColumnNames_ACCOUNT_TYPE).append(" = '").append(accountType).append("' ");
                }
            }
            if (accountName != null) {
                selection.append(Constants.QUERY_PARAM_AND).append(Constants.ColumnNames_ACCOUNT_NAME).append(" = '").append(accountName).append("' ");
            }
            Cursor cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    new String[]{BaseColumns._ID},
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
        if (DeviceTools.checkNoCalendarAccess(context)) return count;

        try {

            if (contentResolver == null) contentResolver = context.getContentResolver();

            Calendar startPeriod = Calendar.getInstance();
            startPeriod.set(Calendar.HOUR_OF_DAY, 0);
            startPeriod.set(Calendar.MINUTE, 0);
            startPeriod.set(Calendar.SECOND, 0);
            startPeriod.set(Calendar.MILLISECOND, 0);
            final int zoneOffset = TimeZone.getDefault().getOffset(startPeriod.getTimeInMillis()); //событие на весь день начинается в 00:00:00 UTC, надо скорректировать часовую зону
            startPeriod.add(Calendar.MILLISECOND, zoneOffset);

            Calendar endPeriod = (Calendar) startPeriod.clone();
            endPeriod.set(Calendar.YEAR, startPeriod.get(Calendar.YEAR) + 1);
            endPeriod.set(Calendar.HOUR_OF_DAY, 0);
            endPeriod.set(Calendar.MINUTE, 0);
            endPeriod.set(Calendar.SECOND, 0);
            endPeriod.set(Calendar.MILLISECOND, 0);
            endPeriod.add(Calendar.MILLISECOND, zoneOffset);
            endPeriod.add(Calendar.SECOND, -1);

            String[] projection = {CalendarContract.Instances.EVENT_ID};
            String selection = CalendarContract.Events.CALENDAR_ID + Constants.SQL_EQUAL + calID;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startPeriod.getTimeInMillis());
            ContentUris.appendId(builder, endPeriod.getTimeInMillis());

            Cursor cursor = contentResolver.query(
                    builder.build(),
                    projection,
                    selection, null,
                    CalendarContract.Events.DTSTART + Constants.SQL_SORT_ASC
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

    int getFileEventsCount(String file, @NonNull String eventType, boolean needEventLabel) {

        int count = 0;
        try {

            String fileContent = readFileToString(file, Constants.STRING_EOL);
            String[] eventsArray = fileContent.split(Constants.STRING_EOL, -1);
            if (eventsArray[0].isEmpty()) return count;
            @Nullable Event event = null;
            boolean isMultiTypeSource = eventType.equals(Constants.Type_MultiEvent);

            if (fileContent.startsWith(Constants.iCal_CalendarBegin)) {
                return fileContent.split(Constants.iCal_EventBegin, -1).length - 1;
            } else if (fileContent.startsWith(Constants.vCard_EventBegin)) {
                return fileContent.split(Constants.vCard_Birthday, -1).length - 1;
            }

            for (String eventRow : eventsArray) {

                String eventLine = eventRow.trim().replace("\uFEFF", Constants.STRING_EMPTY);
                if (eventLine.isEmpty() || eventLine.startsWith(Constants.STRING_HASH) || eventLine.startsWith(Constants.STRING_DSLASH))
                    continue;

                if (eventType.equals(Constants.EventType_Fact)) {
                    count++;
                    continue;
                }

                String eventDateString = Constants.STRING_EMPTY;
                @Nullable Date dateEvent = null;
                String eventLabel_forSearch = Constants.STRING_EMPTY;
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

                            if (!flags.isEmpty()) {
                                if (flags.contains(Constants.STRING_1)) {
                                    flags = flags.replace(Constants.STRING_1, Constants.STRING_EMPTY);
                                }
                                if (flags.contains(Constants.STRING_BC)) {
                                    isAD = false;
                                    flags = flags.replace(Constants.STRING_BC, Constants.STRING_EMPTY);
                                }
                                if (isMultiTypeSource || needEventLabel) {
                                    eventLabel_forSearch = flags.replace(Constants.STRING_UNDERSCORE, Constants.STRING_SPACE);
                                }
                            }

                        } else {

                            eventDateString = eventLine.substring(0, indexFirstSpace);

                        }
                    }

                } else { //Birthdays Plus: |ДДДД-ММ-ДД|ИОФ|тип (Birthday, Anniversary, Custom)|наименование события или null|

                    final String[] eventBDPdetails = eventLine.split(Constants.STRING_BDP_DIV, -1);

                    if (eventBDPdetails.length == 5) {

                        eventDateString = eventBDPdetails[1];
                        if (eventBDPdetails[3].equals(Constants.STRING_BDP_CUSTOM)) {
                            eventLabel_forSearch = eventBDPdetails[4].replace(Constants.STRING_BDP_EOL, Constants.STRING_EMPTY);
                        } else {
                            eventLabel_forSearch = eventBDPdetails[3];
                        }

                    }
                }

                if (eventDateString.isEmpty()) continue;

                if (isMultiTypeSource || needEventLabel) {

                    event = recognizeEventByLabel(eventLabel_forSearch, true, true);

                } else if (eventType.equals(Constants.EventType_BirthDay)) {

                    event = createTypedEvent(Constants.Type_BirthDay, Constants.STRING_EMPTY);

                } else if (eventType.equals(Constants.EventType_Other)) {

                    event = createTypedEvent(Constants.Type_Other, Constants.STRING_EMPTY);

                } else if (eventType.equals(Constants.EventType_Holiday)) {

                    event = createTypedEvent(Constants.Type_HolidayEvent, Constants.STRING_EMPTY);

                }

                if (preferences_rules_unrecognized == Rules_Unrecognized_Skip && (event == null || event.icon == R.drawable.ic_event_unknown)) {
                    continue;
                }

                int indexDateNoYear = isBirthdaysPlusEvent ? eventDateString.indexOf(Constants.STRING_BDP_NO_YEAR) : eventDateString.indexOf(Constants.STRING_0000);
                if (indexDateNoYear == -1) { //С годом
                    try {
                        if (isAD) {

                            if (!isBirthdaysPlusEvent) {
                                String dateNextFloatingEvent = computeFloatingDate(eventDateString, 0);
                                if (!eventDateString.equals(dateNextFloatingEvent)) {
                                    Date eventDateFirstTime = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(dateNextFloatingEvent); //Пытаемся определить год первоначального события
                                    if (eventDateFirstTime != null) {
                                        try {
                                            eventDateFirstTime.setYear(Integer.parseInt(eventDateString.substring(eventDateString.lastIndexOf(Constants.STRING_PERIOD) + 1)) - 1900);
                                        } catch (NumberFormatException ignored) { /**/ }
                                    }
                                    eventDateString = dateNextFloatingEvent;
                                }
                            }

                            dateEvent = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(eventDateString);
                        } else {
                            dateEvent = Objects.requireNonNull(sdf_DDMMYYYY_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                        }
                    } catch (ParseException e1) {
                        try {
                            if (isAD) {
                                dateEvent = Objects.requireNonNull(sdf_india.get()).parse(eventDateString);
                            } else {
                                dateEvent = Objects.requireNonNull(sdf_india_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                            }
                        } catch (ParseException e2) {
                            try {
                                if (isAD) {
                                    dateEvent = Objects.requireNonNull(sdf_uk.get()).parse(eventDateString);
                                } else {
                                    dateEvent = Objects.requireNonNull(sdf_uk_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                }
                            } catch (ParseException e3) {
                                try {
                                    if (isAD) {
                                        dateEvent = Objects.requireNonNull(sdf_java.get()).parse(eventDateString);
                                    } else {
                                        dateEvent = Objects.requireNonNull(sdf_java_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                    }
                                } catch (ParseException e4) {
                                    //Не получилось распознать
                                }
                            }
                        }
                    }

                } else { //Без года

                    String dateNextEvent = eventDateString.substring(0, indexDateNoYear) + getToday().get(Calendar.YEAR);
                    try {
                        if (!isBirthdaysPlusEvent) {
                            String dateNextFloatingEvent = computeFloatingDate(dateNextEvent, 0);
                            if (!dateNextEvent.equals(dateNextFloatingEvent)) {
                                dateNextEvent = dateNextFloatingEvent;
                            }
                        }
                        dateEvent = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(dateNextEvent);
                    } catch (ParseException e1) {
                        try {
                            dateEvent = Objects.requireNonNull(sdf_india.get()).parse(dateNextEvent);
                        } catch (ParseException e2) {
                            try {
                                dateEvent = Objects.requireNonNull(sdf_uk.get()).parse(dateNextEvent);
                            } catch (ParseException e3) {
                                try {
                                    dateNextEvent = eventDateString.replace(Constants.STRING_BDP_NO_YEAR, Integer.toString(getToday().get(Calendar.YEAR)));
                                    dateEvent = Objects.requireNonNull(sdf_java.get()).parse(dateNextEvent);
                                } catch (ParseException e4) {
                                    //Не получилось распознать
                                }
                            }
                        }
                    }
                    if (dateEvent != null && getToday().after(AppDateUtils.getCalendarFromDate(dateEvent)))
                        dateEvent = AppDateUtils.addYear(dateEvent, 1);
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
            @NonNull TreeMap<Integer, String> eventData,
            @NonNull List<String> dataList,
            @NonNull ColumnIndexCache cache,
            @NonNull String eventKey) {

        String eventKey_current = eventKey;
        String eventDateStr = null;
        String eventType = null;
        String accountKey = null;

        try {
            eventDateStr = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.DATA));
            eventType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));
            String accountType = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_TYPE));
            if (accountType == null) accountType = Constants.STRING_NULL;
            String accountName = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_ACCOUNT_NAME));
            if (accountName == null)
                accountName = getResources().getString(R.string.account_type_local);
            accountKey = accountName + Constants.STRING_PARENTHESIS_OPEN + accountType + Constants.STRING_PARENTHESIS_CLOSE;

            if (eventDateStr != null && eventType != null && (preferences_Accounts.isEmpty() || preferences_Accounts.contains(accountKey))) {

                Event event = new Event();
                String contactName = StringUtils.getNotNullString(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME)));
                String contactNameAlt = StringUtils.getNotNullString(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)));
                if (contactNameAlt.contains(Constants.STRING_PARENTHESIS_START)) {
                    contactNameAlt = contactNameAlt.substring(0, contactNameAlt.indexOf(Constants.STRING_PARENTHESIS_START)).trim();
                }
                String eventLabel = StringUtils.getNotNullString(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.LABEL)));
                boolean isEventLabel = !TextUtils.isEmpty(eventLabel);
                statContactsEventCount++;
                boolean isUnrecognized = false;

                if (eventType.equals(Constants.EventType_BirthDay)
                        || (isEventLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel.toLowerCase()).find())) {

                    event = createTypedEvent(Constants.Type_BirthDay, eventLabel);

                } else if (eventType.equals(Constants.EventType_Death)
                        || (isEventLabel && preferences_death_labels != null && preferences_death_labels.reset(eventLabel.toLowerCase()).find())) {

                    event = createTypedEvent(Constants.Type_Death, eventLabel);

                } else if (eventType.equals(Constants.EventType_Anniversary)
                        || (isEventLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel.toLowerCase()).find())) {

                    event = createTypedEvent(Constants.Type_Anniversary, eventLabel);

                } else if (eventType.equals(Constants.EventType_Another)
                        || (isEventLabel && preferences_another_event_labels != null && preferences_another_event_labels.reset(eventLabel.toLowerCase()).find())) {

                    event = createTypedEvent(Constants.Type_Another, eventLabel);

                } else if (eventType.equals(Constants.EventType_Other)
                        || (isEventLabel && preferences_other_event_labels != null && preferences_other_event_labels.reset(eventLabel.toLowerCase()).find())) {

                    event = createTypedEvent(Constants.Type_Other, eventLabel);

                } else if (eventType.equals(Constants.EventType_Holiday)
                        || (isEventLabel && preferences_holiday_event_labels != null && preferences_holiday_event_labels.reset(eventLabel.toLowerCase()).find())) {

                    event = createTypedEvent(Constants.Type_HolidayEvent, eventLabel);

                } else if (isEventLabel) {

                    if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_Custom1, eventLabel);
                        if (!preferences_customevent1_useyear && !eventDateStr.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDateStr = Constants.STRING_2MINUS + eventDateStr.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_Custom2, eventLabel);
                        if (!preferences_customevent2_useyear && !eventDateStr.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDateStr = Constants.STRING_2MINUS + eventDateStr.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_Custom3, eventLabel);
                        if (!preferences_customevent3_useyear && !eventDateStr.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDateStr = Constants.STRING_2MINUS + eventDateStr.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_Custom4, eventLabel);
                        if (!preferences_customevent4_useyear && !eventDateStr.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDateStr = Constants.STRING_2MINUS + eventDateStr.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_Custom5, eventLabel);
                        if (!preferences_customevent5_useyear && !eventDateStr.startsWith(Constants.STRING_2MINUS)) { //Если год не нужен, а он есть в событии
                            eventDateStr = Constants.STRING_2MINUS + eventDateStr.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                        }

                    } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_NameDay, eventLabel);

                    } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel.toLowerCase()).find()) {

                        event = createTypedEvent(Constants.Type_Crowning, eventLabel);

                    } else {

                        isUnrecognized = true;
                        if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Other) {

                            event = createTypedEvent(Constants.Type_Other, eventLabel);

                        } else if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Unrecognized) {

                            event = createTypedEvent(Constants.Type_Unrecognized, eventLabel);

                        } else {
                            return eventKey_current; //Пропускаем событие
                        }

                    }

                }
                if (isEventLabel && event.caption.isEmpty()) event.caption = event.label;

                String eventKey_next = contactName.concat(Constants.STRING_COMMA).concat(eventType);

                //Наименование события в ключе только для пользовательских событий
                if (eventType.equals(Constants.EventType_Custom) || isUnrecognized) {
                    eventKey_next = eventKey_next.concat(Constants.STRING_COMMA).concat(eventLabel);
                }

                String newEventDate = accountType + Constants.STRING_COLON_SPACE + eventDateStr + Constants.STRING_COLON_SPACE
                        + StringUtils.getHash(((!accountType.equals(Constants.STRING_NULL) && !accountType.equals(accountName)) ? Constants.eventSourceContactPrefix : Constants.eventSourcePhonePrefix) + accountKey);

                if (!eventKey_next.equalsIgnoreCase(eventKey_current)) { //Начало данных нового контакта

                    if (!eventData.isEmpty()) { // Уже есть накопленные данные. Нужно сохранить всё, что накопили и обнулить UserData
                        if (dataList.add(getEventData(eventData))) {
                            String personID = eventData.get(Position_contactID);
                            if (personID != null && !personID.isEmpty())
                                map_eventsBySubtypeAndPersonID_offset.put(personID + Constants.STRING_2HASH + eventData.get(Position_eventSubType), dataList.size() - 1);
                        }
                        eventData.clear();
                    }

                    String contactID = cursor.getString(cache.getColumnIndex(cursor, Constants.ColumnNames_CONTACT_ID));
                    if (contactID == null) return eventKey_current;
                    String contactFIO = contactName;

                    String contactTitle = map_contacts_titles.get(contactID);
                    if (contactTitle == null || contactTitle.isEmpty()) {

                        contactTitle = Constants.STRING_EMPTY;

                        //всё, что внутри скобок в имени - в должность
                        int pStartFirst = contactFIO.indexOf(Constants.STRING_PARENTHESIS_START);
                        int pStartLast = contactFIO.lastIndexOf(Constants.STRING_PARENTHESIS_START);
                        int pEndFirst = contactFIO.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                        int pEndLast = contactFIO.lastIndexOf(Constants.STRING_PARENTHESIS_CLOSE);

                        if (pStartFirst > -1 && pEndFirst > pStartFirst) { //хотя бы пара скобок
                            if (pStartFirst == pStartLast && pEndFirst == pEndLast) { //одна пара скобок

                                contactTitle = contactFIO.substring(pStartFirst + 1, pEndFirst);
                                contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                                eventData.put(Position_title, contactTitle);

                            } else if (pStartLast < pEndFirst && pStartLast < pEndLast) { //скобки внутри скобок

                                contactTitle = contactFIO.substring(pStartFirst + 1, pEndLast);
                                contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                                eventData.put(Position_title, contactTitle);

                            } else if (pEndFirst < pStartLast) { //пара скобок за другой парой

                                contactTitle = contactFIO.substring(pStartLast + 1, pEndLast);
                                contactFIO = contactFIO.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                                eventData.put(Position_title, contactTitle);

                            }
                        }
                    }
                    contactFIO = contactFIO.replace(Constants.STRING_COMMA_SPACE, Constants.STRING_SPACE);

                    eventKey_current = eventKey_next;

                    eventData.put(Position_personFullName, contactFIO);
                    eventData.put(Position_personFullNameAlt, contactNameAlt.replace(Constants.STRING_COMMA_SPACE, Constants.STRING_SPACE));
                    eventData.put(Position_contactID, contactID);
                    eventData.put(Position_photo_uri, cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.PHOTO_URI)));
                    eventData.put(Position_eventCaption, event.caption); //Наименование события
                    eventData.put(Position_eventLabel, event.label); //Заголовок пользовательского события
                    eventData.put(Position_eventType, event.type); //Тип события
                    eventData.put(Position_eventSubType, event.subType); //Подтип события
                    eventData.put(Position_organization, StringUtils.getNotNullString(map_organizations.get(contactID)));
                    eventData.put(Position_title, contactTitle);
                    eventData.put(Position_dates, newEventDate);
                    eventData.put(Position_eventIcon, Integer.toString(event.icon));
                    eventData.put(Position_eventEmoji, event.emoji);
                    if (Constants.STRING_1.equals(cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Contacts.STARRED)))) {
                        eventData.put(Position_starred, Constants.STRING_1);
                        statFavoriteEventsCount++;
                    }
                    eventData.put(Position_nickname, StringUtils.getNotNullString(map_contacts_aliases.get(contactID)));
                    eventData.put(Position_eventStorage, Constants.STRING_STORAGE_CONTACTS); //Где искать событие по ID
                    eventData.put(Position_eventSource, getResources().getString(R.string.msg_account_info, accountName));
                    eventData.put(Position_eventURL, StringUtils.getNotNullString(map_events_weblinks.get(contactID)));
                    eventData.put(Position_eventDescription, StringUtils.getNotNullString(map_notes.get(contactID)));
                    eventData.put(Position_rawContactID, StringUtils.getNotNullString(map_contacts_ids.get(contactID)));

                    fillEmptyEventData(eventData);

                } else { //Продолжаем добавлять даты контакта

                    String existingDates = eventData.get(Position_dates);
                    if (existingDates != null && !existingDates.contains(newEventDate))
                        eventData.put(Position_dates, existingDates.concat(Constants.STRING_2TILDA).concat(newEventDate));

                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + resources.getString(R.string.msg_errors_details, accountKey, eventType, eventDateStr));
        }
        return eventKey_current;

    }

    private boolean getCalendarEvents(@NonNull String eventType) {
        //todo: использовать цвета календарей https://www.javatips.net/api/android.provider.calendarcontract.instances

        Cursor cursor = null;
        long statCurrentModuleStart = System.currentTimeMillis();
        try (ColumnIndexCache cache = new ColumnIndexCache()) {

            if (DeviceTools.checkNoCalendarAccess(context)) return false;

            Set<String> preferences_calendars = getPreferences_Calendars(eventType);
            if (preferences_calendars.isEmpty()) return false;

            Event event = new Event();

            if (map_calendars.isEmpty())
                AppDateUtils.fillCalendarList(context, map_calendars, map_calendars_colors);

            //https://stackoverflow.com/questions/25734285/how-to-get-the-real-time-of-recurring-events
            //https://stackoverflow.com/questions/10133616/reading-all-of-todays-events-using-calendarcontract-android-4-0

            if (contentResolver == null) contentResolver = context.getContentResolver();
            String[] projection = {
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.DESCRIPTION, //todo: доделать правила и под это поле
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Events.DTSTART, //начало первоначального события
                    CalendarContract.Events.ALL_DAY
            };


            Calendar startPeriod = AppDateUtils.getWithoutTime(Calendar.getInstance());

            //событие на весь день начинается в 00:00:00 UTC, надо скорректировать часовую зону
            final int zoneOffset = TimeZone.getDefault().getOffset(startPeriod.getTimeInMillis());

            Calendar endPeriod = (Calendar) startPeriod.clone();
            endPeriod.add(Calendar.YEAR, 1);
            endPeriod.add(Calendar.SECOND, -1);

            Calendar dateRubicon = (Calendar) startPeriod.clone();
            if (preferences_list_prev_events_scan_distance > 0) {
                startPeriod.add(Calendar.DAY_OF_YEAR, -preferences_list_prev_events_scan_distance);
            }

            String[] arrRules;
            List<Matcher> matcherNames = new ArrayList<>();
            List<Matcher> matcherTypes = new ArrayList<>();
            List<Matcher> matcherNameAndTypes = new ArrayList<>();
            List<Matcher> matcherTypeAndNames = new ArrayList<>();
            boolean useEventYear;

            boolean isMultiTypeSource = eventType.equals(Constants.Type_MultiEvent);
            if (eventType.equals(Constants.EventType_BirthDay)) {
                event = createTypedEvent(Constants.Type_BirthDay, Constants.STRING_EMPTY);
                useEventYear = preferences_birthday_calendars_useyear;
            } else if (eventType.equals(Constants.EventType_Other)) {
                event = createTypedEvent(Constants.Type_Other, Constants.STRING_EMPTY);
                useEventYear = true;
            } else if (eventType.equals(Constants.EventType_Holiday)) {
                event = createTypedEvent(Constants.Type_HolidayEvent, Constants.STRING_EMPTY);
                useEventYear = true;
            } else if (isMultiTypeSource) {
                useEventYear = true;
            } else {
                return false;
            }

            if (eventType.equals(Constants.EventType_BirthDay) || isMultiTypeSource) {
                arrRules = preferences_birthday_calendars_rules.split(Constants.REGEX_BAR, -1);
                if (!arrRules[0].isEmpty()) {
                    for (String rule : arrRules) {
                        final int indName = rule.indexOf(Constants.RULE_TAG_NAME);
                        final int indType = rule.indexOf(Constants.RULE_TAG_TYPE);

                        if (indName > -1) {
                            if (indType > -1) {
                                final String ruleRegexp = rule.replace(Constants.RULE_TAG_NAME, "(.*)").replace(Constants.RULE_TAG_TYPE, "(.*)");
                                if (indName < indType) {
                                    matcherNameAndTypes.add(Pattern.compile(ruleRegexp).matcher(Constants.STRING_EMPTY));
                                } else {
                                    matcherTypeAndNames.add(Pattern.compile(ruleRegexp).matcher(Constants.STRING_EMPTY));
                                }
                            } else {
                                matcherNames.add(Pattern.compile(rule.replace(Constants.RULE_TAG_NAME, "(.*)")).matcher(Constants.STRING_EMPTY));
                            }
                        } else if (indType > -1) {
                            matcherTypes.add(Pattern.compile(rule.replace(Constants.RULE_TAG_TYPE, "(.*)")).matcher(Constants.STRING_EMPTY));
                        }
                    }
                }
            }

            StringBuilder calIDs = new StringBuilder();
            for (String calID : preferences_calendars) {
                if (calIDs.length() > 0)
                    calIDs.append(Constants.QUERY_PARAM_OR + CalendarContract.Instances.CALENDAR_ID + Constants.SQL_EQUAL);
                calIDs.append(calID);
            }
            String selection = CalendarContract.Instances.CALENDAR_ID + Constants.SQL_EQUAL + calIDs;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, dateRubicon.getTimeInMillis());
            ContentUris.appendId(builder, endPeriod.getTimeInMillis());

            cursor = contentResolver.query(
                    builder.build(),
                    projection,
                    selection,
                    null,
                    CalendarContract.Events.DTSTART + Constants.SQL_SORT_ASC
            );

            int counterTotalAddedEvents = 0;
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        Event eventToCompose = event.clone();
                        counterTotalAddedEvents += addCalendarEventsFromCursor(cursor, cache, zoneOffset, dateRubicon, endPeriod,
                                useEventYear, isMultiTypeSource, eventToCompose, matcherNameAndTypes, matcherTypes, matcherTypeAndNames, matcherNames);
                    }
                }
            }

            statCalendarsEventCount += counterTotalAddedEvents;
            statEventsCount += counterTotalAddedEvents;
            return true;

        } catch (SecurityException se) {
            return false;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
            statTimeGetCalendarEvents += System.currentTimeMillis() - statCurrentModuleStart;
        }
    }

    /**
     * Добавляет календарное событие из курсора. Если событие длится несколько дней - добавляет несколько событий
     *
     * @param cursor              Курсор с данными события
     * @param cache               Кэш номеров колонок в курсоре
     * @param zoneOffset          Смещение текущей временной зоны от зоны startPeriod в мс
     * @param dateRubicon         Дата начала периода минус количество предыдущих дней для показа из настроек
     * @param endPeriod           Дата конца периода (обычно - +1 год от сегодня)
     * @param useEventYear        Использовать год в датах
     * @param isMultiTypeSource   Содержит ли источник событий события разных типов
     * @param event               Заготовка для события
     * @param matcherNameAndTypes Правила распознавания ИМЯ + ТИП СОБЫТИЯ
     * @param matcherTypes        Правила распознавания ТИП СОБЫТИЯ
     * @param matcherTypeAndNames Правила распознавания ТИП СОБЫТИЯ + ИМЯ
     * @param matcherNames        Правила распознавания ИМЯ
     * @return Количество добавленных событий
     */
    private int addCalendarEventsFromCursor(@NonNull Cursor cursor, @NonNull ColumnIndexCache cache, int zoneOffset,
                                            @NonNull Calendar dateRubicon, @NonNull Calendar endPeriod,
                                            boolean useEventYear, boolean isMultiTypeSource, Event event,
                                            List<Matcher> matcherNameAndTypes, List<Matcher> matcherTypes,
                                            List<Matcher> matcherTypeAndNames, List<Matcher> matcherNames) {
        int counterAddedEvents = 0;
        try {

            String eventTitle = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.TITLE));
            if (!StringUtils.hasContent(eventTitle)) return 0;

            int importMethod_Standalone = 0; //Календарное событие без контакта
            int importMethod_NewContactEvent = 1; //Контакт найден, но у него нет данных о событии этого типа - обновляем событие по карточке контакта
            int importMethod_AdditionalDateToContactEvent = 2; //Контакт найден, у него есть такое же событие - добавляем к источникам дат ещё одно значение
            String calendarId = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.CALENDAR_ID));
            String calendarInfoString = map_calendars.get(calendarId);

            final String eventSource = calendarInfoString != null
                    ? getResources().getString(R.string.msg_calendar_info, StringUtils.getKeyParts(calendarInfoString)[0])
                    : getResources().getString(R.string.event_type_calendar);
            Calendar dateStartNextTime = AppDateUtils.getCalendarFromDate(new Date(StringUtils.parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.BEGIN)))));
            Calendar dateEndNextTime = AppDateUtils.getCalendarFromDate(new Date(StringUtils.parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.END)))));
            Calendar dateFirstTime = AppDateUtils.getCalendarFromDate(new Date(StringUtils.parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DTSTART)))));
            final TreeMap<Integer, String> eventData = new TreeMap<>();

            boolean isAllDayEvent = false;
            if (cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Events.ALL_DAY)) == 1) {
                isAllDayEvent = true;

                //У AllDay событий зона всегда UTC
                dateFirstTime.add(Calendar.MILLISECOND, -zoneOffset);
                dateStartNextTime.add(Calendar.MILLISECOND, -zoneOffset);
                dateEndNextTime.add(Calendar.MILLISECOND, -zoneOffset);

                //Событие на весь день заканчивается на следующий день, а не в 23:59:59
                dateEndNextTime.add(Calendar.SECOND, -1);
            }

            boolean isPassedEvent = false;
            if (dateStartNextTime.after(endPeriod)) return 0; //Если событие выпало из периода
            if (dateEndNextTime.before(dateRubicon)) {
                isPassedEvent = true;
            } else if (dateStartNextTime.before(dateRubicon) && dateEndNextTime.after(dateRubicon)) {
                //Если событие начинается до "сегодня", но заканчивается после
                while (dateStartNextTime.before(dateRubicon)) {
                    dateStartNextTime.add(Calendar.DATE, 1);
                }
            }

            final List<String> eventURLs = new ArrayList<>();
            String eventURL;
            String eventDescription = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.DESCRIPTION));
            boolean setOtherIfUnknown = preferences_rules_unrecognized == Rules_Unrecognized_Type_Other;
            if (StringUtils.hasContent(eventDescription)) {
                eventDescription = eventDescription.replace(Constants.STRING_EOL, Constants.STRING_SPACE);
                int indURL;
                int indSpace;

                for (String prefix : new String[]{Constants.STRING_HTTPS, Constants.STRING_HTTP}) {
                    indURL = StringUtils.indexOfIgnoreCase(eventDescription, prefix);
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
                    //Пытаемся распознать по описанию события. Если не получится - ниже будем извлекать тип из заголовка
                    event = recognizeEventByLabel(eventDescription, false, useEventYear);
                }

            } else if (isMultiTypeSource) {
                event.icon = R.drawable.ic_event_unknown;
            }

            String foundName = null;
            if (isMultiTypeSource && event.icon == R.drawable.ic_event_unknown) {
                String foundLabel = null;
                if (matcherNameAndTypes != null && !matcherNameAndTypes.isEmpty()) { // ..[name]..[type]..
                    for (Matcher matcher : matcherNameAndTypes) {
                        if (matcher.reset(eventTitle).find()) {
                            foundName = matcher.group(1);
                            foundLabel = matcher.group(2);
                            if (StringUtils.hasContent(foundName) && StringUtils.hasContent(foundLabel)) {
                                eventTitle = foundName;
                                break;
                            }
                        }
                    }
                }
                if (foundName == null && matcherTypeAndNames != null && !matcherTypeAndNames.isEmpty()) { // ..[type]..[name]..
                    for (Matcher matcher : matcherTypeAndNames) {
                        if (matcher.reset(eventTitle).find()) {
                            foundName = matcher.group(2);
                            foundLabel = matcher.group(1);
                            if (StringUtils.hasContent(foundName) && StringUtils.hasContent(foundLabel)) {
                                eventTitle = foundName;
                                break;
                            }
                        }
                    }
                }
                if (foundLabel == null && matcherTypes != null && !matcherTypes.isEmpty()) { // ..[type]..
                    for (Matcher matcher : matcherTypes) {
                        if (matcher.reset(eventTitle).find()) {
                            foundLabel = matcher.group(1);
                            if (StringUtils.hasContent(foundLabel)) {
                                eventTitle = eventTitle.replace(foundLabel, Constants.STRING_EMPTY);
                                break;
                            }
                        }
                    }
                }

                event = recognizeEventByLabel(StringUtils.getNotNullString(foundLabel), setOtherIfUnknown, useEventYear);
            }

            if (preferences_rules_unrecognized == Rules_Unrecognized_Skip && event.icon == R.drawable.ic_event_unknown)
                return 0;

            //Если:
            // событие на весь день
            // год начала и окончания события совпадают
            // тип события "Праздник" или "Другое событие"
            // календарь "только для чтения"
            // - считаем, что это событие без начальной даты
            if (isAllDayEvent
                    && calendarInfoString != null
                    && dateFirstTime.get(Calendar.YEAR) == dateEndNextTime.get(Calendar.YEAR)
                    && (Objects.equals(event.type, Constants.EventType_Other) || Objects.equals(event.type, Constants.EventType_Holiday))
            ) {

                String[] calendarInfo = StringUtils.getKeyParts(calendarInfoString);
                if (calendarInfo.length > 3 && calendarInfo[3].equals(Constants.STRING_1)) {
                    event.useEventYear = false;
                }
            }

            do {
                eventData.clear();
                final String eventID = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.EVENT_ID));
                if (checkIsEventWithoutYear(eventID + Constants.STRING_2HASH + event.subType)) { //Событие без года
                    event.useEventYear = false;
                }
                final String eventNewDate = Constants.EVENT_PREFIX_CALENDAR_EVENT + Constants.STRING_COLON_SPACE
                        + (event.useEventYear ? Objects.requireNonNull(sdf_java.get()).format(dateFirstTime.getTime()) : Objects.requireNonNull(sdf_java_no_year.get()).format(dateFirstTime.getTime())) + Constants.STRING_COLON_SPACE
                        + StringUtils.getHash(Constants.eventSourceCalendarPrefix + calendarId);
                int importMethod = importMethod_Standalone;

                idsAllCalendarEvents.add(eventID);

                eventData.put(Position_personFullName, eventTitle);
                eventData.put(Position_personFullNameAlt, eventTitle);
                eventData.put(Position_eventStorage, Constants.STRING_STORAGE_CALENDAR);
                eventData.put(Position_eventID, eventID);

                String contactID = getMergedID(eventID);
                String contactTitle = Constants.STRING_EMPTY;
                boolean namedFromEvent = false;

                if (foundName == null && !matcherNames.isEmpty()) { // ..[name]..
                    for (Matcher matcherName : matcherNames) {
                        if (matcherName.reset(eventTitle).find()) {
                            foundName = matcherName.group(1);
                            //тут без break - используем последнее подошедшее совпадение
                        }
                    }
                }

                if (map_contacts_names.isEmpty()) event.needScanContacts = false;

                if (contactID == null && event.needScanContacts && foundName != null) {

                    //всё, что внутри скобок в имени - в должность
                    int pStart = foundName.indexOf(Constants.STRING_PARENTHESIS_START);
                    int pEnd = foundName.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                    if (pStart > -1 && pEnd > pStart) {
                        contactTitle = foundName.substring(pStart + 1, pEnd);
                        foundName = foundName.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                    }

                    String personFullNameNormalized;
                    String personFullNameAltNormalized;
                    if (preferences_rules_calendars_name_format == FormatName.NameFirst) {
                        personFullNameNormalized = StringUtils.normalizeString(foundName);
                        String personFullNameAlt = Person.getAltName(foundName, FormatName.NameFirst);
                        personFullNameAltNormalized = StringUtils.normalizeString(personFullNameAlt);
                        eventData.put(Position_personFullName, foundName);
                        eventData.put(Position_personFullNameAlt, personFullNameAlt);
                    } else {
                        String personFullNameAlt = Person.getAltName(foundName, FormatName.LastnameFirst);
                        personFullNameNormalized = StringUtils.normalizeString(personFullNameAlt);
                        personFullNameAltNormalized = StringUtils.normalizeString(foundName);
                        eventData.put(Position_personFullName, personFullNameAlt);
                        eventData.put(Position_personFullNameAlt, foundName);
                    }
                    namedFromEvent = true;

                    contactID = getContactID(personFullNameNormalized, personFullNameAltNormalized);
                    if (TextUtils.isEmpty(contactTitle) && !TextUtils.isEmpty(contactID)) {
                        contactTitle = StringUtils.getNotNullString(map_contacts_titles.get(contactID));
                    }
                }

                if (contactID != null && event.needScanContacts) {
                    importMethod = importMethod_NewContactEvent;
                    eventData.put(Position_contactID, contactID);
                    eventData.put(Position_rawContactID, StringUtils.getNotNullString(map_contacts_ids.get(contactID)));

                    //Ищем событие контакта в списке событий и добавляем в него
                    Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(contactID + Constants.STRING_2HASH + event.subType);
                    if (eventIndex != null && eventIndex <= eventListUpdated.size() && !isPassedEvent) {

                        addNewDateToExistingEvent(eventIndex, eventID, eventSource, eventNewDate, eventURLs, contactID, contactTitle, eventDescription);
                        importMethod = importMethod_AdditionalDateToContactEvent;
                        counterAddedEvents++;

                    } else { //Такого события ещё не было

                        //Добавляем данные контакта
                        HashMap<String, String> contactDataMap = getContactDataMulti(StringUtils.parseToLong(contactID), new String[]{
                                ContactsContract.Contacts.PHOTO_URI,
                                ContactsContract.Data.DISPLAY_NAME,
                                ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE,
                                ContactsContract.Contacts.STARRED
                        });

                        eventData.put(Position_photo_uri, StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Contacts.PHOTO_URI)));

                        if (contactDataMap.containsKey(ContactsContract.Contacts.STARRED)) {
                            if (Constants.STRING_1.equals(StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Contacts.STARRED)))) {
                                eventData.put(Position_starred, Constants.STRING_1);
                                if (!isPassedEvent) statFavoriteEventsCount++;
                            }
                        }

                        if (!namedFromEvent) {
                            String contactFIO = StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME));
                            eventData.put(Position_personFullName, contactFIO);
                            eventData.put(Position_personFullNameAlt, StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)).replace(Constants.STRING_COMMA, Constants.STRING_EMPTY));
                        }
                        eventData.put(Position_title, contactTitle);
                        eventData.put(Position_organization, StringUtils.getNotNullString(map_organizations.get(contactID)));
                        eventData.put(Position_nickname, StringUtils.getNotNullString(map_contacts_aliases.get(contactID)));
                        eventData.put(Position_eventDescription, StringUtils.getNotNullString(map_notes.get(contactID)));

                        if (!eventURLs.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (String url : eventURLs) {
                                sb.append(url).append(Constants.STRING_2TILDA);
                                if (!isPassedEvent) statContactsURLCount++;
                            }
                            sb.delete(sb.length() - Constants.STRING_2TILDA.length(), sb.length());
                            eventData.put(Position_eventURL, sb.toString());
                            map_events_weblinks.put(contactID, sb.toString());
                        }

                        contactDataMap.clear();
                    }
                }

                if (importMethod != importMethod_AdditionalDateToContactEvent) {

                    if (importMethod != importMethod_NewContactEvent) {
                        eventData.put(Position_eventStorage, Constants.STRING_STORAGE_CALENDAR);
                    }

                    eventData.put(Position_eventCaption, event.caption); //Наименование события
                    eventData.put(Position_eventLabel, event.label); //Заголовок события
                    eventData.put(Position_eventType, event.type); //Тип события
                    eventData.put(Position_eventSubType, event.subType); //Подтип события
                    eventData.put(Position_dates, eventNewDate);
                    eventData.put(Position_eventIcon, Integer.toString(event.icon));
                    eventData.put(Position_eventEmoji, event.emoji);
                    eventData.put(Position_eventDateNextTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(dateStartNextTime.getTime()));
                    eventData.put(Position_eventDateFirstTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(dateFirstTime.getTime()));
                    eventData.put(Position_eventSource, eventSource);
                    eventData.put(Position_eventDescription, eventDescription);

                    if (importMethod == importMethod_Standalone) {
                        if (!eventURLs.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (String url : eventURLs) {
                                sb.append(url).append(Constants.STRING_2TILDA);
                                if (!isPassedEvent) statContactsURLCount++;
                            }
                            sb.delete(sb.length() - Constants.STRING_2TILDA.length(), sb.length());
                            eventData.put(Position_eventURL, sb.toString());
                            map_events_weblinks.put(eventID, sb.toString());
                        }
                    }

                    fillEmptyEventData(eventData);

                    if (!isPassedEvent) {
                        final String eventRow = getEventData(eventData);
                        if (!eventListUpdated.contains(eventRow)) {
                            eventListUpdated.add(eventRow);
                            counterAddedEvents++;

                            if (importMethod == importMethod_NewContactEvent) {  //Добавляем событие
                                if (!TextUtils.isEmpty(contactID)) {
                                    map_eventsBySubtypeAndPersonID_offset.put(contactID + Constants.STRING_2HASH + event.subType, eventListUpdated.size() - 1);
                                }
                                //todo: если будет eventID такой же как и какой-то contactID, то будет конфликт и события объединятся
                                //} else {
                                //    map_eventsBySubtypeAndPersonID_offset.put(eventID + Constants.STRING_2HASH + event.subType, eventListUpdated.size() - 1);
                            }
                        }
                    } else {

                        long eventDistance = AppDateUtils.countDaysDiff(dateEndNextTime.getTime(), dateRubicon.getTime());
                        eventData.put(Position_eventDistance, Long.toString(-eventDistance));
                        eventData.put(Position_eventDistanceText, getEventDistanceText(-eventDistance, dateEndNextTime.getTime()));
                        //todo: двойная конвертация
                        eventData.put(Position_eventDate_sorted, getSortKey(getEventData(eventData).split(Constants.STRING_EOT, -1)));
                        eventListPrev.add(getEventData(eventData));
                    }
                }
                //Ставим на начало следующего дня
                AppDateUtils.clearTime(dateStartNextTime);
                dateStartNextTime.add(Calendar.DATE, 1);
            } while (dateStartNextTime.compareTo(dateEndNextTime) <= 0 && dateStartNextTime.compareTo(endPeriod) <= 0);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return counterAddedEvents;
    }

    /**
     * Добавляет новую дату из календарного события в существующее событие контакта (и другие атрибуты, если указаны).
     * Отличается от {@link #updateExistEvent} тем, что тут новая дата - обязательна
     *
     * @param eventIndex       Индекс события в общем списке событий
     * @param eventID          ID события
     * @param eventSource      Источник события
     * @param eventNewDate     Новая дата события
     * @param eventURLs        Web-ссылки для события
     * @param contactID        ID контакта
     * @param eventDescription Описание события
     * @param contactTitle     Должность контакта
     */
    private void addNewDateToExistingEvent(@NonNull Integer eventIndex, String eventID, String eventSource,
                                           @NonNull String eventNewDate, List<String> eventURLs, String contactID,
                                           String contactTitle, String eventDescription) {
        List<String> singleRowList = Arrays.asList(eventListUpdated.get(eventIndex).split(Constants.STRING_EOT, -1));
        final String eventDates = singleRowList.get(Position_dates);

        if (eventDates.contains(eventNewDate)) return;

        singleRowList.set(Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
        singleRowList.set(Position_eventStorage, singleRowList.get(Position_eventStorage)
                + Constants.STRING_COMMA_SPACE + Constants.STRING_STORAGE_CALENDAR);

        if (eventID != null && singleRowList.get(Position_eventID).isEmpty())
            singleRowList.set(Position_eventID, eventID);

        if (eventURLs != null && !eventURLs.isEmpty()) {
            String eventURL_stored = StringUtils.getNotNullString(singleRowList.get(Position_eventURL)).trim();
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
            singleRowList.set(Position_eventURL, sb.toString());
            map_events_weblinks.put(contactID, sb.toString());
        }

        if (StringUtils.hasContent(eventDescription)) {
            String eventDescription_stored = StringUtils.getNotNullString(singleRowList.get(Position_eventDescription)).trim();
            if (eventDescription_stored.isEmpty()) {
                singleRowList.set(Position_eventDescription, eventDescription);
                map_notes.put(contactID, eventDescription);
            } else {
                final String eventDescription_new = eventDescription_stored.concat(Constants.STRING_SPACE).concat(eventDescription);
                singleRowList.set(Position_eventDescription, eventDescription_new);
                map_notes.put(contactID, eventDescription_new);
            }
        }

        if (StringUtils.hasContent(contactTitle)) {
            String contactTitle_stored = StringUtils.getNotNullString(singleRowList.get(Position_title)).trim();
            if (contactTitle_stored.isEmpty()) {
                singleRowList.set(Position_title, contactTitle);
            }
        }

        if (StringUtils.hasContent(eventSource)) {
            String eventSource_stored = StringUtils.getNotNullString(singleRowList.get(Position_eventSource)).trim();
            if (eventSource_stored.isEmpty()) {
                singleRowList.set(Position_eventSource, eventSource);
            } else if (!eventSource_stored.contains(eventSource)) {
                singleRowList.set(Position_eventSource, eventSource_stored.concat(Constants.STRING_2TILDA).concat(eventSource));
            }
        }

        if (eventID != null && TextUtils.isEmpty(singleRowList.get(Position_eventID))) {
            singleRowList.set(Position_eventID, eventID);
        }

        StringBuilder dataRow = new StringBuilder();
        int rNum = 0;
        for (String entry : singleRowList) {
            rNum++;
            if (rNum != 1) dataRow.append(Constants.STRING_EOT);
            dataRow.append(entry);
        }
        eventListUpdated.set(eventIndex, dataRow.toString());
    }

    private boolean getLocalEvents() {
        try {

            SharedPreferences preferences = context.getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);
            Map<String, ?> prefs = preferences.getAll();
            String nowYearString = Constants.STRING_PERIOD.concat(String.valueOf(getToday().get(Calendar.YEAR)));

            for (String eventId : prefs.keySet()) {
                if (prefs.get(eventId) instanceof String) {
                    String eventString = (String) prefs.get(eventId);
                    if (eventString != null) {
                        try {
                            String[] singleEventArray = eventString.split(Constants.STRING_EOT, -1);

                            boolean eventUseYear = false;
                            boolean eventIsBC = false;
                            Date dateEventFirstTime = null;
                            String eventDateString = singleEventArray[Position_eventDateFirstTime];

                            try {
                                dateEventFirstTime = Objects.requireNonNull(sdf_DDMMYYYY_G.get()).parse(eventDateString);
                                if (dateEventFirstTime != null) {
                                    eventUseYear = true;
                                    eventIsBC = true;
                                }
                            } catch (ParseException peg) {
                                try {
                                    dateEventFirstTime = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(eventDateString);
                                    if (dateEventFirstTime != null) {
                                        eventUseYear = true;
                                    }
                                } catch (ParseException pe) {
                                    try {
                                        dateEventFirstTime = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(eventDateString.substring(0, 5)
                                                .concat(nowYearString));
                                    } catch (ParseException ignored) { /**/ }
                                }
                            }

                            if (dateEventFirstTime == null) {
                                continue;
                            }

                            String eventDates = Constants.EVENT_PREFIX_LOCAL_EVENT + Constants.STRING_COLON_SPACE
                                    + (eventUseYear ? eventIsBC ? Objects.requireNonNull(sdf_java_G.get()).format(dateEventFirstTime) : Objects.requireNonNull(sdf_java.get()).format(dateEventFirstTime) : Objects.requireNonNull(sdf_java_no_year.get()).format(dateEventFirstTime))
                                    + Constants.STRING_COLON_SPACE
                                    + StringUtils.getHash(Constants.eventSourceLocalPrefix);

                            int eventSubType = Constants.Type_BirthDay;
                            try {
                                eventSubType = Integer.parseInt(singleEventArray[Position_eventSubType]);
                            } catch (NumberFormatException ignored) { /**/ }
                            Event event = createTypedEvent(eventSubType, Constants.STRING_EMPTY);
                            TreeMap<Integer, String> eventData = getEventData(eventString);

                            final String eventTitleAlt = eventData.get(Position_personFullNameAlt);
                            String personFullName = eventData.get(Position_personFullName);
                            if (eventTitleAlt == null || eventTitleAlt.isEmpty()) {
                                eventData.put(Position_personFullNameAlt, personFullName);
                            }

                            String eventPhoto = eventData.get(Position_photo);
                            if (eventPhoto != null && !eventPhoto.isEmpty()) {
                                eventData.put(Position_photo, context.getString(R.string.event_photo_details, eventPhoto.length()));
                            }

                            eventData.put(Position_eventDateFirstTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(dateEventFirstTime.getTime()));
                            eventData.put(Position_dates, eventDates);
                            eventData.put(Position_eventCaption, event.caption);
                            eventData.put(Position_eventLabel, event.label);
                            eventData.put(Position_eventIcon, Integer.toString(event.icon));
                            eventData.put(Position_eventEmoji, event.emoji);
                            eventData.put(Position_eventType, event.type);
                            eventData.put(Position_eventSubType, event.subType);
                            eventData.put(Position_eventStorage, Constants.EVENT_PREFIX_LOCAL_EVENT);
                            eventData.put(Position_eventSource, getResources().getString(R.string.msg_source_local));

                            fillEmptyEventData(eventData);
                            String eventRow = getEventData(eventData);
                            if (!eventListUpdated.contains(eventRow)) {
                                if (eventListUpdated.add(eventRow)) {
                                    statEventsCount++;
                                    statLocalEventCount++;
                                    if (event.subType.equals(Constants.EventType_BirthDay)) {
                                        birthdayDatesForNames.put(personFullName, dateEventFirstTime);
                                    } else if (event.subType.equals(Constants.EventType_Death)) {
                                        deathDatesForNames.put(personFullName, dateEventFirstTime);
                                    }
                                    map_eventsBySubtypeAndPersonName_offset.put(personFullName + Constants.STRING_2HASH + eventData.get(Position_eventSubType), eventListUpdated.size() - 1);
                                }
                            }
                        } catch (Exception e) {
                            ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventString));
                        }
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    /**
     * Сохраняет локальное событие
     *
     * @param eventData Данные события
     */
    @SuppressLint("ApplySharedPref")
    void saveLocalEvent(@NonNull TreeMap<Integer, String> eventData) {
        try {

            SharedPreferences preferences = context.getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(eventData.get(Position_eventID), getEventData(eventData));
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Удаляет локальное событие
     *
     * @param eventData Данные события
     */
    void removeLocalEvent(@NonNull TreeMap<Integer, String> eventData) {
        try {

            final String eventId = eventData.get(Position_eventID);
            if (eventId == null) return;
            final String eventKey = getEventKey(getEventData(eventData).split(Constants.STRING_EOT, -1));

            SharedPreferences preferences = context.getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);
            if (!preferences.contains(eventId)) {
                ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_not_found));
                return;
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(eventId, null);
            if (editor.commit()) {
                unsetHiddenEvent(eventKey, null);
                unsetSilencedEvent(eventKey, null);
                unsetFavoriteEvent(eventKey, null);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Nullable
    TreeMap<Integer, String> getLocalEvent(String eventId) {
        try {

            SharedPreferences preferences = context.getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);
            String eventData = null;
            try {
                eventData = preferences.getString(eventId, null);
            } catch (ClassCastException ignored) { /**/ }

            return eventData == null ? null : getEventData(eventData);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return null;
        }
    }


    /**
     * Получает список ID всех похожих локальных событий, исключая текущее событие
     *
     * @param eventData       Данные события, по которым надо искать
     * @param fieldsToCompare Набор полей, по которым выполнять сравнение.
     *                        Если null или пуст — используется сравнение по всем полям.
     * @return Список ID похожих событий или null, если совпадений нет
     */
    List<String> getSimilarLocalEventIds(String eventData, Set<getSimilarFields> fieldsToCompare) {
        // Если не указано — сравниваем по всем полям
        if (fieldsToCompare == null || fieldsToCompare.isEmpty()) {
            fieldsToCompare = EnumSet.allOf(getSimilarFields.class);
        }

        List<String> result = new ArrayList<>();
        try {
            TreeMap<Integer, String> eventDataToFind = getEventData(eventData);

            SharedPreferences preferences = context.getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);
            Map<String, ?> prefs = preferences.getAll();

            String currentEventId = eventDataToFind.get(ContactsEvents.Position_eventID);

            for (String eventId : prefs.keySet()) {
                if (eventId.equalsIgnoreCase(currentEventId)) continue; // исключаем само себя

                Object value = prefs.get(eventId);
                if (!(value instanceof String)) continue;

                String eventString = (String) value;

                try {
                    String[] singleEventArray = eventString.split(Constants.STRING_EOT, -1);

                    // Убедимся, что массив достаточно длинный для безопасного доступа
                    if (singleEventArray.length < ContactsEvents.Position_attrAmount) continue;

                    boolean matches = true;

                    if (fieldsToCompare.contains(getSimilarFields.PERSON_FULL_NAME))
                        if (!Objects.equals(eventDataToFind.get(ContactsEvents.Position_personFullName),
                                singleEventArray[ContactsEvents.Position_personFullName])) {
                            matches = false;
                        }

                    if (matches && fieldsToCompare.contains(getSimilarFields.TITLE)) {
                        if (!Objects.equals(eventDataToFind.get(ContactsEvents.Position_title),
                                singleEventArray[ContactsEvents.Position_title])) {
                            matches = false;
                        }
                    }

                    if (matches && fieldsToCompare.contains(getSimilarFields.ORGANIZATION)) {
                        if (!Objects.equals(eventDataToFind.get(ContactsEvents.Position_organization),
                                singleEventArray[ContactsEvents.Position_organization])) {
                            matches = false;
                        }
                    }

                    if (matches && fieldsToCompare.contains(getSimilarFields.PHOTO)) {
                        if (!Objects.equals(eventDataToFind.get(ContactsEvents.Position_photo),
                                singleEventArray[ContactsEvents.Position_photo])) {
                            matches = false;
                        }
                    }

                    if (matches && fieldsToCompare.contains(getSimilarFields.EVENT_TYPE)) {
                        if (!Objects.equals(eventDataToFind.get(ContactsEvents.Position_eventType),
                                singleEventArray[ContactsEvents.Position_eventType])) {
                            matches = false;
                        }
                    }

                    if (matches && fieldsToCompare.contains(getSimilarFields.EVENT_SUBTYPE)) {
                        if (!Objects.equals(eventDataToFind.get(ContactsEvents.Position_eventSubType),
                                singleEventArray[ContactsEvents.Position_eventSubType])) {
                            matches = false;
                        }
                    }

                    if (matches) {
                        result.add(singleEventArray[ContactsEvents.Position_eventID]);
                    }

                } catch (Exception ignored) { /* Игнорируем повреждённые записи */ }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return result.isEmpty() ? null : result;
    }

    public enum getSimilarFields {
        PERSON_FULL_NAME,
        TITLE,
        ORGANIZATION,
        PHOTO,
        EVENT_TYPE,
        EVENT_SUBTYPE
    }

    int getLocalEventsCount() {
        SharedPreferences preferences = context.getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);
        return preferences.getAll().size();
    }

    @NonNull
    String getEventData(@NonNull TreeMap<Integer, String> eventData) {
        StringBuilder dataRow = new StringBuilder();
        try {

            int rNum = 0;
            for (Map.Entry<Integer, String> entry : eventData.entrySet()) {
                rNum++;
                if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                dataRow.append(entry.getValue());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return dataRow.toString();
    }

    @NonNull
    TreeMap<Integer, String> getEventData(String eventString) {
        TreeMap<Integer, String> eventData = new TreeMap<>();
        try {

            String[] singleEventArray = eventString.split(Constants.STRING_EOT, -1);
            int size = singleEventArray.length;
            for (int i = 0; i < size; i++) {
                eventData.put(i, singleEventArray[i]);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return eventData;
    }

    /** Возвращает данные события в виде строки (разделитель: \n)
     * @param event Массив с данными события
     * @return Строка
     */
    @NonNull
    String getEventDataAsString(@NonNull String[] event) {
        StringBuilder eventInfo = new StringBuilder();
        try {
            int eventRows = event.length;
            for (int i = 0; i < eventRows; i++) {
                String row = event[i];
                if (i == ContactsEvents.Position_photo && !TextUtils.isEmpty(row)) {
                    eventInfo.append(i)
                            .append(Constants.STRING_COLON_SPACE)
                            .append(getResources().getString(R.string.event_photo_details, row.length()))
                            .append(Constants.STRING_EOL);
                } else {
                    eventInfo.append(i).append(Constants.STRING_COLON_SPACE).append(row).append(Constants.STRING_EOL);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return eventInfo.toString();
    }

    void initNotifications() {
        //https://stackoverflow.com/questions/51343550/how-to-give-notifications-on-android-on-specific-time-in-android-oreo/51645875#51645875

        try {
            StringBuilder log = new StringBuilder();

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                log.append(context.getString(R.string.msg_notifications_disabled));
            } else {

                Set<String> daysQ2 = isFeatureEnabled(Constants.FEATURE_NOTIFY_Q2) ? preferences_notifications2_days : new HashSet<>();

                initNotificationChannel(log, 1, preferences_notifications_days, preferences_notifications_ringtone); //для Android 8+
                initNotificationChannel(log, 2, daysQ2, preferences_notifications2_ringtone); //для Android 8+

                initBootReceiver(log);

                initNotificationSchedule(log, 1, preferences_notifications_days, preferences_notifications_alarm_hour, preferences_notifications_alarm_minute);
                initNotificationSchedule(log, 2, daysQ2, preferences_notifications2_alarm_hour, preferences_notifications2_alarm_minute);
            }
            initWidgetUpdate(log);

            if (log.length() > 0) ToastExpander.showDebugMsg(context, log.toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public long getLastNotifyForQueue(int i) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            if (i == 1) {
                return preferences.getLong(resources.getString(R.string.pref_Notifications_LastNotify), 0);
            } else if (i == 2) {
                return preferences.getLong(resources.getString(R.string.pref_Notifications2_LastNotify), 0);
            } else {
                return 0;
            }
        } catch (ClassCastException e) {
            return 0;
        }
    }

    public void setLastNotifyForQueue(int i, long lastNotifyDate) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        if (i == 1) {
            editor.putLong(resources.getString(R.string.pref_Notifications_LastNotify), lastNotifyDate);
        } else if (i == 2) {
            editor.putLong(resources.getString(R.string.pref_Notifications2_LastNotify), lastNotifyDate);
        } else {
            return;
        }
        editor.apply();
    }

    void launchIntentOnFile(@NonNull Uri uri) {
        try {
            String mime = context.getContentResolver().getType(uri);

            Intent intentEdit = new Intent();
            intentEdit.setAction(Intent.ACTION_EDIT);
            intentEdit.setDataAndType(uri, mime);

            Intent intentView = new Intent();
            intentView.setAction(Intent.ACTION_VIEW);
            intentView.setDataAndType(uri, mime);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> appToEdit = packageManager.queryIntentActivities(intentEdit, PackageManager.MATCH_DEFAULT_ONLY);
            List<ResolveInfo> appsToView = packageManager.queryIntentActivities(intentView, PackageManager.MATCH_DEFAULT_ONLY);

            if (!appToEdit.isEmpty()) {
                //https://stackoverflow.com/questions/24604346/issue-opening-document-using-flag-grant-write-uri-permission-intent-android
                final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
                intentEdit.addFlags(flags);
                intentEdit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    for (ResolveInfo resolveInfo : context.getPackageManager().queryIntentActivities(intentEdit, PackageManager.MATCH_ALL)) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        context.grantUriPermission(packageName, uri, flags);
                    }
                }

                try {
                    context.startActivity(intentEdit);
                } catch (ActivityNotFoundException e) { /**/ }
            } else if (!appsToView.isEmpty()) {
                try {
                    final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
                    intentView.addFlags(flags);
                    intentEdit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intentView);
                } catch (ActivityNotFoundException e) { /**/ }
            } else {
                //https://www.codeproject.com/Tips/1097808/Custom-App-Chooser-in-Android
                ToastExpander.showInfoMsg(context, context.getText(R.string.msg_file_no_app_for_file).toString());
            }

        } catch (SecurityException se) {
            ToastExpander.showInfoMsg(context, context.getText(R.string.msg_file_access_write_error).toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }


    /** Добавляет в общий список события из файлов, указанных в настройках
     * @param eventType Тип событий
     * @return True - в настройках указаны файлы для указанного типа событий и не было ошибки перебора этих файлов
     */
    private boolean getFileEvents(@NonNull String eventType) {

        long statCurrentModuleStart = System.currentTimeMillis();
        try {

            Set<String> fileList = null;
            boolean isMultiTypeSource = eventType.equals(Constants.Type_MultiEvent);

            if (eventType.equals(Constants.EventType_BirthDay)) {

                fileList = preferences_Birthday_files;

            } else if (eventType.equals(Constants.EventType_Other)) {

                fileList = preferences_OtherEvent_files;

            } else if (eventType.equals(Constants.EventType_Holiday)) {

                fileList = preferences_HolidayEvent_files;

            } else if (isMultiTypeSource) {

                fileList = preferences_MultiType_files;

            }
            if (fileList == null || fileList.isEmpty()) return false;


            // Кэшируем массив форматов
            final SimpleDateFormat[] dateFormats = {
                    Objects.requireNonNull(sdf_DDMMYYYY.get()),
                    Objects.requireNonNull(sdf_india.get()),
                    Objects.requireNonNull(sdf_uk.get()),
                    Objects.requireNonNull(sdf_java.get())
            };

            for (String file : fileList) {

                String[] fileDetails = file.split(Constants.REGEX_BAR);
                final String fileName = fileDetails[0].lastIndexOf(Constants.STRING_SLASH) > -1 ?
                        fileDetails[0].substring(fileDetails[0].lastIndexOf(Constants.STRING_SLASH) + 1) : fileDetails[0];
                final String eventSource = !fileName.isEmpty() ? getResources().getString(R.string.msg_file_info, fileName) : getResources().getString(R.string.event_type_file);

                if (fileName.toLowerCase().endsWith(".vcf") || fileName.toLowerCase().endsWith(".vcard")) {

                    streamVCardEvents(file, getToday(), eventSource);

                } else {

                    String[] eventsArray = readFileToString(file, Constants.STRING_EOL).split(Constants.STRING_EOL, -1);
                    if (eventsArray[0].isEmpty()) {
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                        continue;
                    }

                    if (eventsArray[0].startsWith(Constants.iCal_CalendarBegin)) {

                        streamICalEvents(file, eventType, getToday(), eventSource);

                    } else {
                        List<String> expandedEvents = splitMultidayEventsAsSeparateLine(eventsArray, dateFormats);
                        for (String eventString : expandedEvents) {
                            addFileEventFromLine(
                                    file,
                                    eventSource,
                                    eventString,
                                    eventType,
                                    Constants.PREFIX_FileEventID,
                                    Constants.EVENT_PREFIX_FILE_EVENT,
                                    Constants.eventSourceFilePrefix,
                                    Constants.STRING_STORAGE_FILE,
                                    null,
                                    getToday()
                            );
                        }
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        } finally {
            statTimeGetFileEvents += System.currentTimeMillis() - statCurrentModuleStart;
        }
    }

    /** Разделить события на несколько дней на отдельные дни
     * @param eventsArray Массив с событиями (даты начала и конца события идут через "-": 21.01.2000-23.01.2000 Название события)
     */
    private List<String> splitMultidayEventsAsSeparateLine(String[] eventsArray, SimpleDateFormat[] dateFormats) {
        List<String> result = new ArrayList<>(eventsArray.length);
        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();

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

            Date dateStart = AppDateUtils.parseDateWithFormats(strDateStart, dateFormats);
            Date dateEnd = AppDateUtils.parseDateWithFormats(strDateEnd, dateFormats);

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
                sb.append(Objects.requireNonNull(sdf_DDMMYYYY.get()).format(calStart.getTime()));
                sb.append(eventBody);
                result.add(sb.toString());
                calStart.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return result;
    }

    /**
     * Потоковое чтение iCal (.ics) файла без загрузки всего файла в память.
     *
     * @param file        Путь до файла и URI
     * @param eventType   Тип событий, которым добавлять
     * @param today       Дата сегодня
     * @param eventSource Источник событий
     */
    private void streamICalEvents(@NonNull String file, @NonNull String eventType,
                                  @NonNull Calendar today, @NonNull String eventSource) {
        try {
            String[] fileDetails = file.split(Constants.REGEX_BAR);
            Uri uri = Uri.parse(fileDetails.length > 1 ? fileDetails[1] : fileDetails[0]);

            if (context.getContentResolver() == null) {
                ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                return;
            }

            boolean isVkExport = false;

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                StringBuilder currentEventBlock = new StringBuilder();
                boolean inEvent = false;

                while ((line = reader.readLine()) != null) {
                    // Проверяем экспорт VK на уровне всего файла
                    if (line.startsWith(Constants.iCal_PROD_ID_VK)) {
                        isVkExport = true;
                    }

                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) continue;

                    if (trimmedLine.equalsIgnoreCase(Constants.iCal_EventBegin)) {
                        inEvent = true;
                        currentEventBlock.setLength(0);
                        currentEventBlock.append(line).append("\n");
                    } else if (inEvent) {
                        currentEventBlock.append(line).append("\n");

                        if (trimmedLine.equalsIgnoreCase(Constants.iCal_EventEnd)) {
                            inEvent = false;
                            processSingleICalEvent(currentEventBlock.toString(), file, eventType, today, eventSource, isVkExport);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error streaming iCal: " + file, e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Обработка блока одного события iCal
     */
    private void processSingleICalEvent(@NonNull String eventBlock, @NonNull String file,
                                        @NonNull String eventType, @NonNull Calendar today,
                                        @NonNull String eventSource, boolean isVkExport) {
        try {
            final int nowYear = today.get(Calendar.YEAR);
            final TreeMap<Integer, String> eventData = new TreeMap<>();

            Event event = null;
            Date eventDateFirstTime = null;
            Date eventDateThisTime = null;
            String eventTitle = null;
            StringBuilder eventDescription = new StringBuilder();
            String eventURL = Constants.STRING_EMPTY;
            boolean useEventYear = true;
            String currentProp = ""; // Для отслеживания текущего свойства (чтобы корректно обрабатывать переносы строк)

            String[] lines = eventBlock.split("\n", -1);

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Обработка многострочных значений (folded lines в iCal начинаются с пробела или табуляции)
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    if (currentProp.equals("DESCRIPTION") || currentProp.equals("SUMMARY")) {
                        eventDescription.append(StringUtils.substringAfter(line, " "));
                    }
                    continue;
                }

                if (trimmed.equalsIgnoreCase(Constants.iCal_EventBegin)) {
                    if (eventType.equals(Constants.EventType_BirthDay)) {
                        event = createTypedEvent(Constants.Type_BirthDay, Constants.STRING_EMPTY);
                        useEventYear = true;
                    } else if (eventType.equals(Constants.EventType_Holiday)) {
                        event = createTypedEvent(Constants.Type_HolidayEvent, Constants.STRING_EMPTY);
                        useEventYear = false;
                    } else {
                        event = createTypedEvent(Constants.Type_Other, Constants.STRING_EMPTY);
                        useEventYear = false;
                    }
                } else if (line.startsWith(Constants.iCal_Summary)) {
                    eventTitle = StringUtils.substringAfter(line, Constants.iCal_Summary);
                    currentProp = "SUMMARY";
                } else if (line.startsWith(Constants.iCal_Description)) {
                    eventDescription = new StringBuilder(StringUtils.substringAfter(line, Constants.iCal_Description));
                    currentProp = "DESCRIPTION";
                } else if (line.startsWith(Constants.iCal_Url)) {
                    eventURL = StringUtils.substringAfter(line, Constants.iCal_Url);
                    currentProp = "URL";
                } else if (line.startsWith(Constants.iCal_Date)) {
                    String storedDate = StringUtils.substringAfter(line, Constants.STRING_COLON);
                    try {
                        eventDateFirstTime = Objects.requireNonNull(sdf_YYYYMMDD_noDiv.get()).parse(storedDate);
                        try {
                            eventDateThisTime = Objects.requireNonNull(sdf_YYYYMMDD_noDiv.get()).parse(nowYear + storedDate.substring(4));
                        } catch (ParseException e) {
                            // Не получилось распознать
                        }
                        if (eventDateThisTime != null) {
                            if (today.getTime().after(eventDateThisTime)) {
                                eventDateThisTime = AppDateUtils.addYear(eventDateThisTime, 1);
                            }
                        }
                        if (useEventYear && isVkExport && storedDate.startsWith("2000")) {
                            useEventYear = false;
                        }
                    } catch (ParseException ignored) {
                        /**/
                    }
                    currentProp = "DTSTART";
                } else if (trimmed.equalsIgnoreCase(Constants.iCal_EventEnd) && event != null) {

                    if (eventDateFirstTime == null || eventDateThisTime == null || eventTitle == null) {
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventBlock));
                    } else {
                        String eventNewDate = Constants.EVENT_PREFIX_FILE_EVENT + Constants.STRING_COLON_SPACE
                                + (useEventYear ? Objects.requireNonNull(sdf_java.get()).format(eventDateFirstTime)
                                : Objects.requireNonNull(sdf_java_no_year.get()).format(eventDateFirstTime))
                                + Constants.STRING_COLON_SPACE
                                + StringUtils.getHash((eventType.equals(Constants.Type_MultiEvent) ? Constants.eventSourceMultiFilePrefix : Constants.eventSourceFilePrefix) + file);

                        eventDescription = new StringBuilder(eventDescription.toString().replace(eventURL, Constants.STRING_EMPTY));

                        String personFullNameAlt = null;
                        String personFullNameNormalized = null;
                        String personFullNameAltNormalized = null;
                        String contactID = null;
                        String eventID = Constants.PREFIX_FileEventID + StringUtils.getHash(StringUtils.substringBefore(file, Constants.STRING_BAR) + eventTitle);

                        eventData.put(Position_personFullName, eventTitle);
                        if (eventType.equals(Constants.EventType_BirthDay)) {
                            personFullNameAlt = Person.getAltName(eventTitle, FormatName.NameFirst);
                            eventData.put(Position_personFullNameAlt, personFullNameAlt);
                        }

                        if (event.needScanContacts) {
                            personFullNameNormalized = StringUtils.normalizeString(eventTitle);
                            personFullNameAltNormalized = StringUtils.normalizeString(personFullNameAlt);
                        }

                        eventData.put(Position_eventDescription, eventDescription.toString().replace(Constants.REGEX_BS, Constants.STRING_EMPTY));
                        eventData.put(Position_eventStorage, Constants.STRING_STORAGE_FILE);
                        eventData.put(Position_eventCaption, event.caption);
                        eventData.put(Position_eventLabel, event.label);
                        eventData.put(Position_eventSource, eventSource);
                        eventData.put(Position_eventType, event.type);
                        eventData.put(Position_eventSubType, event.subType);
                        eventData.put(Position_dates, eventNewDate);
                        eventData.put(Position_eventIcon, Integer.toString(event.icon));
                        eventData.put(Position_eventEmoji, event.emoji);
                        eventData.put(Position_eventURL, eventURL);
                        eventData.put(Position_eventID, eventID);

                        if (useEventYear) {
                            eventData.put(Position_eventDateFirstTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDateFirstTime));
                            eventData.put(Position_eventDateNextTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDateThisTime));
                        }

                        if (event.needScanContacts) {
                            contactID = getContactID(personFullNameNormalized, personFullNameAltNormalized);
                            if (contactID == null) {
                                contactID = getMergedID(eventID);
                            }

                            if (!TextUtils.isEmpty(contactID)) {
                                eventData.put(Position_contactID, contactID);
                                eventData.put(Position_rawContactID, StringUtils.getNotNullString(map_contacts_ids.get(contactID)));

                                Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(contactID + Constants.STRING_2HASH + event.subType);
                                if (eventIndex != null && eventIndex <= eventListUpdated.size()) {
                                    if (updateExistEvent(eventIndex, eventID, eventSource, eventNewDate, null, null, eventURL)) {
                                        eventData.clear();
                                    }
                                } else {
                                    final Long contactIDLong = StringUtils.parseToLong(contactID);
                                    HashMap<String, String> contactDataMap = getContactDataMulti(contactIDLong, new String[]{
                                            ContactsContract.Contacts.PHOTO_URI,
                                            ContactsContract.Contacts.STARRED
                                    });

                                    eventData.put(Position_photo_uri, contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));
                                    if (contactDataMap.containsKey(ContactsContract.Contacts.STARRED)) {
                                        if (Constants.STRING_1.equals(StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Contacts.STARRED)))) {
                                            eventData.put(Position_starred, Constants.STRING_1);
                                            statFavoriteEventsCount++;
                                        }
                                    }
                                    contactDataMap.clear();

                                    eventData.put(Position_nickname, StringUtils.getNotNullString(map_contacts_aliases.get(contactID)));
                                    if (TextUtils.isEmpty(eventData.get(Position_organization))) {
                                        eventData.put(Position_organization, StringUtils.getNotNullString(map_organizations.get(contactID)));
                                    }
                                    if (TextUtils.isEmpty(eventData.get(Position_title))) {
                                        eventData.put(Position_title, StringUtils.getNotNullString(map_contacts_titles.get(contactID)));
                                    }
                                }
                            }
                        }

                        if (!eventData.isEmpty()) {
                            statEventsCount++;
                            statFilesEventCount++;
                            fillEmptyEventData(eventData);
                            String eventRow = getEventData(eventData);
                            if (!eventListUpdated.contains(eventRow)) {
                                eventListUpdated.add(eventRow);
                                if (!TextUtils.isEmpty(contactID)) {
                                    map_eventsBySubtypeAndPersonID_offset.put(contactID + Constants.STRING_2HASH + event.subType, eventListUpdated.size() - 1);
                                }
                            }
                        }
                    }

                    // Сброс переменных для следующего события
                    eventData.clear();
                    eventDateFirstTime = null;
                    eventDateThisTime = null;
                    eventTitle = null;
                    eventDescription.setLength(0);
                    eventURL = Constants.STRING_EMPTY;
                    event = null;
                    currentProp = "";
                } else {
                    currentProp = ""; // Сброс, если это неизвестное свойство
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing single iCal event: " + file, e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Потоковое чтение vCard файла без загрузки всего файла в память.
     * (поддерживаются дни рождения, версии 2.1 и 3.0, включая Quoted-Printable фолдинг)
     *
     * @param file        Путь до файла и URI
     * @param today       Дата сегодня
     * @param eventSource Источник событий
     */
    private void streamVCardEvents(@NonNull String file, @NonNull Calendar today, @NonNull String eventSource) {
        try {
            String[] fileDetails = file.split(Constants.REGEX_BAR);
            Uri uri = Uri.parse(fileDetails.length > 1 ? fileDetails[1] : fileDetails[0]);

            if (context.getContentResolver() == null) {
                ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                return;
            }

            ByteArrayOutputStream reusableBaos = new ByteArrayOutputStream(8192);

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                StringBuilder currentVCard = new StringBuilder();
                boolean inVCard = false;
                boolean previousLineEndedWithEquals = false;

                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        previousLineEndedWithEquals = false;
                        continue;
                    }

                    boolean isContinuation = false;

                    if (inVCard && currentVCard.length() > 0) {
                        if (previousLineEndedWithEquals) {
                            if (line.equals(" =")) {
                                // На текущей строке только "=", который оторвался от "=" с предыдущей строки
                                currentVCard.deleteCharAt(currentVCard.length() - 1); // удаляем '\n'
                                currentVCard.append("=").append("\n");
                                previousLineEndedWithEquals = false;
                                continue;
                            } else {
                                // Удаляем ДВА символа: '=' и '\n'
                                currentVCard.delete(currentVCard.length() - 2, currentVCard.length());
                                currentVCard.append(line).append("\n");  // ← добавили \n
                                isContinuation = true;
                            }
                        } else if (line.endsWith("==")) {
                            // Конец Base64
                            currentVCard.deleteCharAt(currentVCard.length() - 1); // удаляем '\n'
                            currentVCard.append(line.trim()).append("\n");  // ← добавили \n
                            continue;
                        } else if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                            currentVCard.deleteCharAt(currentVCard.length() - 1); // удаляем '\n'
                            currentVCard.append(line.substring(1)).append("\n");  // ← добавили \n
                            isContinuation = true;
                        }
                    }

                    if (isContinuation) {
                        previousLineEndedWithEquals = line.endsWith("=");
                        continue;
                    }

                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        previousLineEndedWithEquals = false;
                        continue;
                    }

                    if (trimmedLine.equalsIgnoreCase(Constants.vCard_EventBegin)) {
                        inVCard = true;
                        currentVCard.setLength(0);
                        currentVCard.append(trimmedLine).append("\n");
                        previousLineEndedWithEquals = false;
                    } else if (inVCard) {
                        currentVCard.append(trimmedLine).append("\n");
                        previousLineEndedWithEquals = trimmedLine.endsWith("=");

                        if (trimmedLine.equalsIgnoreCase(Constants.vCard_EventEnd)) {
                            inVCard = false;
                            previousLineEndedWithEquals = false;
                            processSingleVCardString(currentVCard.toString(), today, eventSource, file, reusableBaos);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error streaming vCard: " + file, e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Обработка строки одного контакта vCard
     */
    private void processSingleVCardString(@NonNull String vCardString, @NonNull Calendar today,
                                          @NonNull String eventSource, @NonNull String file,
                                          @NonNull ByteArrayOutputStream reusableBaos) {
        try {
            final int nowYear = today.get(Calendar.YEAR);
            final TreeMap<Integer, String> eventData = new TreeMap<>();

            Event event = null;
            Date eventDateFirstTime = null;
            String url = Constants.STRING_EMPTY;
            String lastName = Constants.STRING_EMPTY;
            String firstName = Constants.STRING_EMPTY;
            String middleName = Constants.STRING_EMPTY;
            String fullName = Constants.STRING_EMPTY;
            String organization = Constants.STRING_EMPTY;
            String title = Constants.STRING_EMPTY;
            String photo = Constants.STRING_EMPTY;
            boolean useEventYear = true;

            String[] lines = vCardString.split("\n", -1);

            for (String line : lines) {
                if (line.startsWith(Constants.vCard_EventBegin)) { // Constants.vCard_EventBegin
                    event = createTypedEvent(Constants.Type_BirthDay, Constants.STRING_EMPTY);
                } else if (line.startsWith(Constants.vCard_EventEnd) && event != null) { // Constants.vCard_EventEnd

                    if (eventDateFirstTime != null && (!firstName.isEmpty() || !lastName.isEmpty() || !fullName.isEmpty())) {
                        String eventNewDate = Constants.EVENT_PREFIX_FILE_EVENT + Constants.STRING_COLON_SPACE
                                + (useEventYear ? Objects.requireNonNull(sdf_java.get()).format(eventDateFirstTime)
                                : Objects.requireNonNull(sdf_java_no_year.get()).format(eventDateFirstTime))
                                + Constants.STRING_COLON_SPACE
                                + StringUtils.getHash(Constants.eventSourceFilePrefix + file);

                        String personFullName;
                        String personFullNameAlt;
                        String contactID = null;

                        if (!firstName.isEmpty()) {
                            if (!middleName.isEmpty()) {
                                personFullName = firstName + Constants.STRING_SPACE + middleName;
                            } else {
                                personFullName = firstName;
                            }
                            if (!lastName.isEmpty()) {
                                personFullName += Constants.STRING_SPACE + lastName;
                            }
                        } else if (!lastName.isEmpty()) {
                            personFullName = lastName;
                        } else {
                            personFullName = fullName;
                        }
                        personFullNameAlt = Person.getAltName(personFullName, FormatName.NameFirst);

                        String eventID = Constants.PREFIX_FileEventID + StringUtils.getHash(StringUtils.substringBefore(file, Constants.STRING_BAR) + personFullNameAlt);

                        eventData.put(Position_personFullName, personFullName);
                        eventData.put(Position_personFullNameAlt, personFullNameAlt);
                        eventData.put(Position_eventStorage, Constants.STRING_STORAGE_FILE);
                        eventData.put(Position_eventCaption, event.caption);
                        eventData.put(Position_eventLabel, event.label);
                        eventData.put(Position_eventSource, eventSource);
                        eventData.put(Position_eventType, event.type);
                        eventData.put(Position_eventSubType, event.subType);
                        eventData.put(Position_dates, eventNewDate);
                        eventData.put(Position_eventIcon, Integer.toString(event.icon));
                        eventData.put(Position_eventEmoji, event.emoji);
                        eventData.put(Position_eventURL, url);
                        eventData.put(Position_eventID, eventID);

                        if (!TextUtils.isEmpty(organization)) eventData.put(Position_organization, organization);
                        if (!TextUtils.isEmpty(title)) eventData.put(Position_title, title);

                        if (event.needScanContacts) {
                            contactID = getContactID(StringUtils.normalizeString(personFullName), StringUtils.normalizeString(personFullNameAlt));
                            if (contactID == null) contactID = getMergedID(eventID);

                            if (!TextUtils.isEmpty(contactID)) {
                                eventData.put(Position_contactID, contactID);
                                eventData.put(Position_rawContactID, StringUtils.getNotNullString(map_contacts_ids.get(contactID)));

                                Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(contactID + Constants.STRING_2HASH + event.subType);
                                if (eventIndex != null && eventIndex <= eventListUpdated.size()) {
                                    if (updateExistEvent(eventIndex, eventID, eventSource, eventNewDate, null, null, url)) {
                                        eventData.clear();
                                    }
                                } else {
                                    final Long contactIDLong = StringUtils.parseToLong(contactID);
                                    HashMap<String, String> contactDataMap = getContactDataMulti(contactIDLong, new String[]{
                                            ContactsContract.Contacts.PHOTO_URI, ContactsContract.Contacts.STARRED
                                    });
                                    eventData.put(Position_photo_uri, contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));
                                    if (contactDataMap.containsKey(ContactsContract.Contacts.STARRED) && Constants.STRING_1.equals(StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Contacts.STARRED)))) {
                                        eventData.put(Position_starred, Constants.STRING_1);
                                        statFavoriteEventsCount++;
                                    }
                                    contactDataMap.clear();
                                    eventData.put(Position_nickname, StringUtils.getNotNullString(map_contacts_aliases.get(contactID)));
                                    if (TextUtils.isEmpty(eventData.get(Position_organization)))
                                        eventData.put(Position_organization, StringUtils.getNotNullString(map_organizations.get(contactID)));
                                    if (TextUtils.isEmpty(eventData.get(Position_title)))
                                        eventData.put(Position_title, StringUtils.getNotNullString(map_contacts_titles.get(contactID)));
                                }
                            }
                        }

                        if (!TextUtils.isEmpty(photo)) {
                            eventData.put(ContactsEvents.Position_photo, photo);
                        }

                        if (!eventData.isEmpty()) {
                            statEventsCount++;
                            statFilesEventCount++;
                            fillEmptyEventData(eventData);
                            String eventRow = getEventData(eventData);
                            if (!eventListUpdated.contains(eventRow)) {
                                eventListUpdated.add(eventRow);
                                if (!TextUtils.isEmpty(contactID)) {
                                    map_eventsBySubtypeAndPersonID_offset.put(contactID + Constants.STRING_2HASH + event.subType, eventListUpdated.size() - 1);
                                }
                            }
                        }
                    }

                    // Сброс переменных для следующего контакта
                    eventData.clear();
                    eventDateFirstTime = null;
                    url = Constants.STRING_EMPTY;
                    lastName = Constants.STRING_EMPTY;
                    firstName = Constants.STRING_EMPTY;
                    middleName = Constants.STRING_EMPTY;
                    fullName = Constants.STRING_EMPTY;
                    organization = Constants.STRING_EMPTY;
                    title = Constants.STRING_EMPTY;
                    photo = Constants.STRING_EMPTY;
                    event = null;

                } else if (event != null) {

                    // Парсинг полей текущего контакта
                    String nValue = StringUtils.getTagValue(line, Constants.vCard_Name);
                    if (nValue != null) {
                        if (StringUtils.isQuotedPrintable(line)) nValue = StringUtils.decodeQuotedPrintable(nValue, StandardCharsets.UTF_8);
                        List<String> nameParts = StringUtils.splitWithEscape(nValue, Constants.STRING_SEMICOLON);
                        if (nameParts.size() > 2) {
                            lastName = StringUtils.cleanValue(nameParts.get(0));
                            firstName = StringUtils.cleanValue(nameParts.get(1));
                            middleName = StringUtils.cleanValue(nameParts.get(2));
                        }
                    }

                    String fnValue = StringUtils.getTagValue(line, Constants.vCard_FormattedName);
                    if (fnValue != null && lastName.isEmpty() && firstName.isEmpty()) {
                        if (StringUtils.isQuotedPrintable(line)) fnValue = StringUtils.decodeQuotedPrintable(fnValue, StandardCharsets.UTF_8);
                        fullName = StringUtils.cleanValue(fnValue);
                    }

                    String bdayValue = StringUtils.getTagValue(line, Constants.vCard_Birthday);
                    if (bdayValue != null) {
                        boolean hasYear = !bdayValue.startsWith(Constants.STRING_MINUS);
                        String dateToParse = hasYear ? bdayValue : nowYear + bdayValue.substring(1);
                        eventDateFirstTime = AppDateUtils.parseDateWithFormats(dateToParse,
                                Objects.requireNonNull(sdf_YYYYMMDD_noDiv.get()),
                                Objects.requireNonNull(sdf_java.get()));
                        if (eventDateFirstTime != null) {
                            useEventYear = hasYear;
                        } else {
                            ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_date_parse_error) + line);
                        }
                    }

                    String orgValue = StringUtils.getTagValue(line, Constants.vCard_Org);
                    if (orgValue != null) {
                        if (StringUtils.isQuotedPrintable(line)) orgValue = StringUtils.decodeQuotedPrintable(orgValue, StandardCharsets.UTF_8);
                        List<String> orgParts = StringUtils.splitWithEscape(orgValue, Constants.STRING_SEMICOLON);
                        if (!orgParts.isEmpty()) organization = StringUtils.cleanValue(orgParts.get(0));
                    }

                    String titleValue = StringUtils.getTagValue(line, Constants.vCard_Title);
                    if (titleValue != null) {
                        if (StringUtils.isQuotedPrintable(line)) titleValue = StringUtils.decodeQuotedPrintable(titleValue, StandardCharsets.UTF_8);
                        title = StringUtils.cleanValue(titleValue);
                    }

                    String urlValue = StringUtils.getTagValue(line, Constants.vCard_URL);
                    if (urlValue != null) url = urlValue;

                    String photoValue = StringUtils.getTagValue(line, Constants.vCard_Photo);
                    if (!TextUtils.isEmpty(photoValue)) {

                        String cleanBase64 = photoValue;
                        int base64Index = photoValue.indexOf("BASE64,");
                        if (base64Index == -1) base64Index = photoValue.indexOf("base64,");
                        if (base64Index != -1) {
                            cleanBase64 = photoValue.substring(base64Index + 7);
                        }

                        try {
                            byte[] imageBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);

                            // Читаем только размеры (не аллоцируем память под пиксели!)
                            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                            boundsOptions.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, boundsOptions);

                            if (boundsOptions.outWidth == -1 || boundsOptions.outHeight == -1) {
                                photo = photoValue;
                            } else {
                                // Вычисляем целевой размер
                                int step = context.getResources().getInteger(R.integer.pref_LocalEvents_PhotoSize_step);
                                int maxSize = step + step * preferences_local_events_photo_size;

                                // Вычисляем inSampleSize ===
                                int sampleSize = ImageUtils.calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxSize, maxSize);

                                // Декодируем СРАЗУ уменьшенное изображение
                                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                                decodeOptions.inSampleSize = sampleSize;
                                Bitmap scaledBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, decodeOptions);

                                if (scaledBitmap != null) {
                                    // Переиспользуем ByteArrayOutputStream (очищаем вместо создания нового)
                                    reusableBaos.reset();
                                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, reusableBaos);
                                    byte[] scaledBytes = reusableBaos.toByteArray();

                                    photo = android.util.Base64.encodeToString(scaledBytes, android.util.Base64.DEFAULT);
                                    scaledBitmap.recycle();
                                } else {
                                    photo = photoValue;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing vCard photo: " + e.getMessage());
                            photo = photoValue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing single vCard", e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Добавляет данные события к существующему событию (даты, Url, ...).
     * Отличается от {@link #addNewDateToExistingEvent} тем, что тут новая дата - необязательна
     *
     * @param eventIndex   Смещение обновляемого события в общем списке событий
     * @param eventID      ID события
     * @param eventSource  Источник добавляемых данных
     * @param eventNewDate Добавляемая дата с префиксом и хеш источника события
     * @param orgName      Организация
     * @param title        Должность
     * @param eventURL     URL для события
     * @return true - данные были обновлены
     */
    private boolean updateExistEvent(@NonNull Integer eventIndex, String eventID, String eventSource, @NonNull String eventNewDate, String orgName, String title, String eventURL) {
        try {

            List<String> singleRowList = Arrays.asList(eventListUpdated.get(eventIndex).split(Constants.STRING_EOT, -1));
            final String eventDates = singleRowList.get(Position_dates);
            boolean needUpdate = false;

            if (!eventDates.contains(eventNewDate)) { //Пропускаем дубли
                singleRowList.set(Position_dates, eventDates.concat(Constants.STRING_2TILDA).concat(eventNewDate));
                singleRowList.set(Position_eventStorage, singleRowList.get(Position_eventStorage)
                        + Constants.STRING_COMMA_SPACE + Constants.STRING_STORAGE_FILE);
                needUpdate = true;
            }

            if (StringUtils.hasContent(eventURL)) {
                String eventURL_stored = StringUtils.getNotNullString(singleRowList.get(Position_eventURL)).trim();
                if (eventURL_stored.isEmpty()) {
                    singleRowList.set(Position_eventURL, eventURL);
                } else if (!eventURL_stored.contains(eventURL)) {
                    singleRowList.set(Position_eventURL, eventURL_stored.concat(Constants.STRING_2TILDA).concat(eventURL));
                }
                statContactsURLCount++;
                needUpdate = true;
            }

            if (StringUtils.hasContent(eventSource)) {
                String eventSource_stored = StringUtils.getNotNullString(singleRowList.get(Position_eventSource)).trim();
                if (eventSource_stored.isEmpty()) {
                    singleRowList.set(Position_eventSource, eventSource);
                    needUpdate = true;
                } else if (!eventSource_stored.contains(eventSource)) {
                    singleRowList.set(Position_eventSource, eventSource_stored.concat(Constants.STRING_2TILDA).concat(eventSource));
                    needUpdate = true;
                }
            }

            if (StringUtils.hasContent(eventID)) {
                if (TextUtils.isEmpty(singleRowList.get(Position_eventID))) {
                    singleRowList.set(Position_eventID, eventID);
                    needUpdate = true;
                }
            }

            if (StringUtils.hasContent(orgName) && !StringUtils.hasContent(singleRowList.get(Position_organization))) {
                singleRowList.set(Position_organization, orgName);
                needUpdate = true;
            }

            if (StringUtils.hasContent(title) && !StringUtils.hasContent(singleRowList.get(Position_title))) {
                singleRowList.set(Position_title, title);
                needUpdate = true;
            }

            if (needUpdate) {
                StringBuilder dataRow = new StringBuilder();
                int rNum = 0;
                for (String entry : singleRowList) {
                    rNum++;
                    if (rNum != 1) dataRow.append(Constants.STRING_EOT);
                    dataRow.append(entry);
                }
                eventListUpdated.set(eventIndex, dataRow.toString());
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    /**
     * Возвращает ID контакта из таблицы имён по ФИО и ИОФ
     *
     * @param personFullNameNormalized    ИОФ
     * @param personFullNameAltNormalized ФИО
     * @return ID контакта
     */
    @Nullable
    private String getContactID(String personFullNameNormalized, String personFullNameAltNormalized) {
        String contactID = null;
        if (personFullNameNormalized != null) {
            contactID = map_contacts_names.get(personFullNameNormalized);
            if (TextUtils.isEmpty(contactID) && !personFullNameNormalized.equals(personFullNameAltNormalized)) {
                contactID = map_contacts_names.get(personFullNameAltNormalized);
            }
            if (TextUtils.isEmpty(contactID)) {
                contactID = map_contacts_names.get(Person.getShortName(personFullNameNormalized, Constants.pref_List_NameFormat_FirstSecondLast));
            }
            if (TextUtils.isEmpty(contactID) && personFullNameAltNormalized != null && !personFullNameNormalized.equals(personFullNameAltNormalized)) {
                contactID = map_contacts_names.get(Person.getShortName(personFullNameAltNormalized, Constants.pref_List_NameFormat_LastFirstSecond));
            }
        }
        return contactID;
    }

    /**
     * Добавляет в общий список событие из строки файла
     *
     * @param eventSource           Источник события (путь до файла + "|" + URI, или ID источника, если событие из внутреннего ресурса)
     * @param eventSourceCaption    Источник события (заголовок)
     * @param eventString           Строка с событием
     * @param eventType             Тип события, которым добавлять
     * @param eventIdPrefix         Префикс ID
     * @param eventSourcePrefix     Префикс источника
     * @param eventIdHashPrefix     Префикс ID для hash
     * @param eventStorage          Тип источника события
     * @param eventEmoji            Эмоджи события
     * @param today                 Дата сегодня
     */
    private void addFileEventFromLine(@NonNull String eventSource, @NonNull String eventSourceCaption,
                                      @NonNull String eventString, @NonNull String eventType,
                                      @NonNull String eventIdPrefix, @NonNull String eventSourcePrefix,
                                      @NonNull String eventIdHashPrefix, @NonNull String eventStorage,
                                      String eventEmoji, @NonNull Calendar today) {
        try {
            String eventLine = eventString.startsWith("\uFEFF") ? eventString.substring(1).trim() : eventString.trim();

            if (eventLine.isEmpty() || eventLine.startsWith(Constants.STRING_HASH) || eventLine.startsWith(Constants.STRING_DSLASH)) {
                return;
            }

            TreeMap<Integer, String> eventData = new TreeMap<>();
            String eventLabel_forSearch = Constants.STRING_EMPTY;
            String eventTitle = Constants.STRING_EMPTY;
            String eventDateString = Constants.STRING_EMPTY;
            String eventNewDate;
            @Nullable String contactID = null;
            String eventURL = Constants.STRING_EMPTY;
            boolean isEndless = true;
            boolean isAD = true;
            @Nullable Event event = null;
            boolean isMultiTypeSource = eventType.equals(Constants.Type_MultiEvent);

            int indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
            boolean isBirthdaysPlusEvent = eventLine.startsWith(Constants.STRING_BDP_DIV) && eventLine.endsWith(Constants.STRING_BDP_EOL);

            if (!isBirthdaysPlusEvent) {
                if (indexFirstSpace == -1) return;

                final int indexComma = eventLine.indexOf(Constants.STRING_COMMA);
                if (indexComma > -1 && indexComma < indexFirstSpace) { // Есть флаги

                    if (indexFirstSpace - indexComma == 1) { // После запятой пробел - убираем
                        eventLine = eventLine.substring(0, indexComma + 1) + eventLine.substring(indexFirstSpace + 1);
                        indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                        if (indexFirstSpace == -1) {
                            ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventString));
                            return;
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

            } else { // Birthdays Plus
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
                ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventString));
                return;
            }

            if (isMultiTypeSource) {
                boolean setOtherIfUnknown = preferences_rules_unrecognized == Rules_Unrecognized_Type_Other;
                event = recognizeEventByLabel(eventLabel_forSearch, setOtherIfUnknown, true);
            } else if (eventType.equals(Constants.EventType_BirthDay)) {
                event = createTypedEvent(Constants.Type_BirthDay, Constants.STRING_EMPTY);
            } else if (eventType.equals(Constants.EventType_Other)) {
                event = createTypedEvent(Constants.Type_Other, Constants.STRING_EMPTY);
            } else if (eventType.equals(Constants.EventType_Holiday)) {
                event = createTypedEvent(Constants.Type_HolidayEvent, Constants.STRING_EMPTY);
            }

            if (preferences_rules_unrecognized == Rules_Unrecognized_Skip && (event == null || event.icon == R.drawable.ic_event_unknown)) {
                return;
            }
            if (event != null && eventEmoji != null) {
                event.emoji = eventEmoji;
            }

            boolean useEventYear = true;
            int indexDateNoYear = isBirthdaysPlusEvent ? eventDateString.indexOf(Constants.STRING_BDP_NO_YEAR) : eventDateString.indexOf(Constants.STRING_0000);
            if (indexDateNoYear != -1) useEventYear = false;

            ComputedDateForFileEvent result = getComputedDateForFileEvent(today, indexDateNoYear, isAD, !isBirthdaysPlusEvent, eventDateString, eventData, isEndless);
            if (result.dateEvent == null || event == null) {
                ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, eventString));
                return;
            }
            if (preferences_list_prev_events_scan_distance == 0 && result.isPassedEvent) {
                return; // Событие прошло и показ прошедших выключен
            }

            String eventID = eventIdPrefix + StringUtils.getHash(StringUtils.substringBefore(eventSource, Constants.STRING_BAR) + eventLine);
            eventNewDate = eventSourcePrefix + Constants.STRING_COLON_SPACE
                    + (useEventYear ? isAD ? Objects.requireNonNull(sdf_java.get()).format(result.dateEvent) : Objects.requireNonNull(sdf_java_G.get()).format(result.dateEvent) : Objects.requireNonNull(sdf_java_no_year.get()).format(result.dateEvent))
                    + Constants.STRING_COLON_SPACE
                    + StringUtils.getHash((isMultiTypeSource ? Constants.eventSourceMultiFilePrefix : eventIdHashPrefix) + eventSource);

            eventData.put(Position_eventStorage, eventStorage);
            eventData.put(Position_eventCaption, event.caption);
            eventData.put(Position_eventLabel, event.label);
            eventData.put(Position_eventSource, eventSourceCaption);
            eventData.put(Position_eventType, event.type);
            eventData.put(Position_eventSubType, event.subType);
            eventData.put(Position_dates, eventNewDate);
            eventData.put(Position_eventIcon, Integer.toString(event.icon));
            eventData.put(Position_eventEmoji, event.emoji);

            int urlOffset = StringUtils.indexOfIgnoreCase(eventTitle, Constants.STRING_HTTP);
            if (urlOffset == -1) {
                urlOffset = StringUtils.indexOfIgnoreCase(eventTitle, Constants.STRING_HTTPS);
            }

            if (urlOffset > -1) {
                eventURL = eventTitle.substring(urlOffset);
                eventURL = StringUtils.substringBefore(eventURL, Constants.STRING_SPACE);
                eventData.put(Position_eventURL, eventURL);
                eventTitle = eventTitle.substring(0, urlOffset).trim(); // Обрезаем оригинальную строку
                if (!result.isPassedEvent) statContactsURLCount++;
            }

            // Описание события
            int indStartDescription = eventTitle.indexOf(Constants.STRING_BAR);
            if (indStartDescription > -1) {
                String eventDescription = eventTitle.substring(indStartDescription + 1);
                if (!eventDescription.isEmpty()) {
                    eventData.put(Position_eventDescription, eventDescription.trim());
                    eventTitle = eventTitle.substring(0, indStartDescription).trim();
                }
            }

            if (map_contacts_names.isEmpty()) event.needScanContacts = false;

            if (event.needScanContacts) {
                int pStartFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_START);
                int pStartLast = eventTitle.lastIndexOf(Constants.STRING_PARENTHESIS_START);
                int pEndFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                int pEndLast = eventTitle.lastIndexOf(Constants.STRING_PARENTHESIS_CLOSE);
                String contactTitle = null;

                if (pStartFirst > -1 && pEndFirst > pStartFirst) {
                    if (pStartFirst == pStartLast && pEndFirst == pEndLast) {
                        contactTitle = eventTitle.substring(pStartFirst + 1, pEndFirst);
                        eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                    } else if (pStartLast < pEndFirst && pStartLast < pEndLast) {
                        contactTitle = eventTitle.substring(pStartFirst + 1, pEndLast);
                        eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                    } else if (pEndFirst < pStartLast) {
                        contactTitle = eventTitle.substring(pStartLast + 1, pEndLast);
                        eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                    }
                    if (contactTitle != null) {
                        int cStart = contactTitle.indexOf(Constants.STRING_COMMA);
                        if (cStart > 0) {
                            eventData.put(Position_organization, contactTitle.substring(0, cStart).trim());
                            eventData.put(Position_title, contactTitle.substring(cStart + 1).trim());
                        } else {
                            eventData.put(Position_title, contactTitle.trim());
                        }
                    }
                }

                String personFullNameNormalized;
                String personFullNameAltNormalized;
                if (preferences_rules_files_name_format == FormatName.NameFirst) {
                    personFullNameNormalized = StringUtils.normalizeString(eventTitle);
                    String personFullNameAlt = Person.getAltName(eventTitle, FormatName.NameFirst);
                    personFullNameAltNormalized = StringUtils.normalizeString(personFullNameAlt);
                    eventData.put(Position_personFullName, eventTitle);
                    eventData.put(Position_personFullNameAlt, personFullNameAlt);
                } else {
                    String personFullNameAlt = Person.getAltName(eventTitle, FormatName.LastnameFirst);
                    personFullNameNormalized = StringUtils.normalizeString(personFullNameAlt);
                    personFullNameAltNormalized = StringUtils.normalizeString(eventTitle);
                    eventData.put(Position_personFullName, personFullNameAlt);
                    eventData.put(Position_personFullNameAlt, eventTitle);
                }
                contactID = getContactID(personFullNameNormalized, personFullNameAltNormalized);

                if (contactID == null) {
                    contactID = getMergedID(eventID);
                }

            } else {
                eventData.put(Position_personFullName, eventTitle);
                eventData.put(Position_personFullNameAlt, eventTitle);
            }

            eventData.put(Position_notAnnualEvent, !result.isEndless ? Constants.STRING_1 : Constants.STRING_EMPTY);
            eventData.put(Position_eventID, eventID);

            String orgNameFile = Constants.STRING_EMPTY;
            String titleFile = Constants.STRING_EMPTY;
            if (!TextUtils.isEmpty(contactID)) {
                orgNameFile = StringUtils.getNotNullString(eventData.get(Position_organization)).trim();
                titleFile = StringUtils.getNotNullString(eventData.get(Position_title)).trim();
                String orgNameContact = StringUtils.getNotNullString(map_organizations.get(contactID)).trim().toLowerCase();

                if (!orgNameContact.isEmpty() && !orgNameFile.isEmpty() && !orgNameContact.contains(orgNameFile.toLowerCase())) {
                    contactID = null;
                }
            }

            if (!TextUtils.isEmpty(contactID)) {
                eventData.put(Position_contactID, contactID);
                eventData.put(Position_rawContactID, StringUtils.getNotNullString(map_contacts_ids.get(contactID)));

                Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(contactID + Constants.STRING_2HASH + event.subType);
                if (eventIndex != null && eventIndex <= eventListUpdated.size()) {
                    if (updateExistEvent(eventIndex, eventID, eventSourceCaption, eventNewDate, orgNameFile, titleFile, eventURL)) {
                        eventData.clear();
                    }
                } else {
                    final Long contactIDLong = StringUtils.parseToLong(contactID);
                    HashMap<String, String> contactDataMap = getContactDataMulti(contactIDLong, new String[]{
                            ContactsContract.Contacts.PHOTO_URI,
                            ContactsContract.Contacts.STARRED
                    });

                    eventData.put(Position_photo_uri, contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));
                    if (contactDataMap.containsKey(ContactsContract.Contacts.STARRED)) {
                        if (Constants.STRING_1.equals(StringUtils.getNotNullString(contactDataMap.get(ContactsContract.Contacts.STARRED)))) {
                            eventData.put(Position_starred, Constants.STRING_1);
                            if (!result.isPassedEvent) statFavoriteEventsCount++;
                        }
                    }
                    contactDataMap.clear();

                    eventData.put(Position_nickname, StringUtils.getNotNullString(map_contacts_aliases.get(contactID)));
                    if (TextUtils.isEmpty(eventData.get(Position_organization))) {
                        eventData.put(Position_organization, StringUtils.getNotNullString(map_organizations.get(contactID)));
                    }
                    if (TextUtils.isEmpty(eventData.get(Position_title))) {
                        eventData.put(Position_title, StringUtils.getNotNullString(map_contacts_titles.get(contactID)));
                    }
                }
            }

            if (!eventData.isEmpty()) {
                if (!result.isPassedEvent) {
                    fillEmptyEventData(eventData);
                    String eventRow = getEventData(eventData);

                    if (eventListUpdated.add(eventRow)) {
                        statEventsCount++;
                        if (eventSourcePrefix.equals(Constants.EVENT_PREFIX_FILE_EVENT)) {
                            statFilesEventCount++;
                        }
                        increaseStatForEventSources(eventSourcePrefix);
                        increaseStatForEventSourcesIds(StringUtils.getHash(eventIdHashPrefix + eventSource));

                        if (!TextUtils.isEmpty(contactID)) {
                            map_eventsBySubtypeAndPersonID_offset.put(contactID + Constants.STRING_2HASH + event.subType, eventListUpdated.size() - 1);
                        }
                        String personFullName = eventData.get(Position_personFullName);
                        if (result.dateEventFirstTime != null) {
                            if (event.subType.equals(Constants.EventType_BirthDay)) {
                                birthdayDatesForNames.put(personFullName, result.dateEventFirstTime);
                            } else if (event.subType.equals(Constants.EventType_Death)) {
                                deathDatesForNames.put(personFullName, result.dateEventFirstTime);
                            }
                        }
                        map_eventsBySubtypeAndPersonName_offset.put(personFullName + Constants.STRING_2HASH + eventData.get(Position_eventSubType), eventListUpdated.size() - 1);

                        if (result.datePrevFloatingEvent != null) {
                            Date eventDatePrev = null;
                            try {
                                eventDatePrev = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(result.datePrevFloatingEvent);
                            } catch (ParseException pe) { /**/ }
                            if (eventDatePrev != null) {
                                long eventDistance = AppDateUtils.countDaysDiff(eventDatePrev, today.getTime());
                                if (eventDistance > 0 && eventDistance <= preferences_list_prev_events_scan_distance) {
                                    eventData.put(Position_eventDateNextTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDatePrev));
                                    eventData.put(Position_eventDistance, Long.toString(-eventDistance));
                                    eventData.put(Position_eventDistanceText, getEventDistanceText(-eventDistance, eventDatePrev));
                                    eventData.put(Position_eventDate_sorted, getSortKey(getEventData(eventData).split(Constants.STRING_EOT, -1)));
                                    eventRow = getEventData(eventData);
                                    if (!eventListPrev.contains(eventRow)) {
                                        eventListPrev.add(eventRow);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    long eventDistance = AppDateUtils.countDaysDiff(result.dateEvent, today.getTime());
                    if (eventDistance <= preferences_list_prev_events_scan_distance) {
                        fillEmptyEventData(eventData);
                        eventData.put(Position_eventDateNextTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(result.dateEvent));
                        eventData.put(Position_eventDistance, Long.toString(-eventDistance));
                        eventData.put(Position_eventDistanceText, getEventDistanceText(-eventDistance, result.dateEvent));
                        eventData.put(Position_eventDate_sorted, getSortKey(getEventData(eventData).split(Constants.STRING_EOT, -1)));
                        final String eventRow = getEventData(eventData);
                        if (!eventListPrev.contains(eventRow)) {
                            eventListPrev.add(eventRow);
                        }
                    }
                }
                eventData.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Парсит дату из строки
     *
     * @param today                  Дата сегодня
     * @param indexDateNoYear        Смещение в дате на 0000 год
     * @param isAD                   Дата нашей эры
     * @param tryComputeFloatingDate Пытаться определить плавающую дату
     * @param eventDateString        Дата строкой в формате
     * @param eventData              Данные события
     * @param isEndless              Ежегодное событие
     * @return Результат парсинга даты
     */
    @NonNull
    ComputedDateForFileEvent getComputedDateForFileEvent(
            @NonNull Calendar today, int indexDateNoYear, boolean isAD,
            boolean tryComputeFloatingDate, @NonNull String eventDateString,
            TreeMap<Integer, String> eventData, boolean isEndless) {

        Date eventDate = null;
        Date eventDateFirstTime = null;
        String datePrevFloatingEvent = null;
        boolean isPassedEvent = false;

        if (indexDateNoYear == -1) { //С годом
            try {
                if (isAD) {
                    if (tryComputeFloatingDate) {
                        String dateNextFloatingEvent = computeFloatingDate(eventDateString, 0);
                        if (!eventDateString.equals(dateNextFloatingEvent)) {
                            eventDateFirstTime = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(dateNextFloatingEvent); //Пытаемся определить год первоначального события
                            if (eventDateFirstTime != null && eventData != null) {
                                try {
                                    eventDateFirstTime.setYear(Integer.parseInt(eventDateString.substring(eventDateString.lastIndexOf(Constants.STRING_PERIOD) + 1)) - 1900);
                                    eventData.put(Position_eventDateFirstTime, Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDateFirstTime));
                                    eventData.put(Position_eventDateNextTime, dateNextFloatingEvent);
                                    isEndless = false;
                                } catch (NumberFormatException ignored) { /**/ }
                            }

                            //Дата предыдущего года
                            if (preferences_list_prev_events_scan_distance > 0) {
                                datePrevFloatingEvent = computeFloatingDate(eventDateString, -1);
                            }
                            eventDateString = dateNextFloatingEvent;
                        }
                    }
                    eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(eventDateString);
                } else {
                    eventDate = Objects.requireNonNull(sdf_DDMMYYYY_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                }
            } catch (ParseException e1) {
                try {
                    if (isAD) {
                        eventDate = Objects.requireNonNull(sdf_india.get()).parse(eventDateString);
                    } else {
                        eventDate = Objects.requireNonNull(sdf_india_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                    }
                } catch (ParseException e2) {
                    try {
                        if (isAD) {
                            eventDate = Objects.requireNonNull(sdf_uk.get()).parse(eventDateString);
                        } else {
                            eventDate = Objects.requireNonNull(sdf_uk_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                        }
                    } catch (ParseException e3) {
                        try {
                            if (isAD) {
                                eventDate = Objects.requireNonNull(sdf_java.get()).parse(eventDateString);
                            } else {
                                eventDate = Objects.requireNonNull(sdf_java_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                            }
                        } catch (ParseException ignored) { /**/ }
                    }
                }
            }
            if (eventDate != null) {
                if (!isEndless && today.after(AppDateUtils.getCalendarFromDate(eventDate))) {
                    isPassedEvent = true;
                }
                eventDateFirstTime = eventDate;
            }

        } else { //Без года

            String dateNextEvent = eventDateString.substring(0, indexDateNoYear) + today.get(Calendar.YEAR);
            try {
                if (tryComputeFloatingDate) {
                    String dateNextFloatingEvent = computeFloatingDate(dateNextEvent, 0);
                    if (!dateNextEvent.equals(dateNextFloatingEvent)) {
                        if (eventData != null) {
                            eventData.put(Position_eventDateFirstTime, dateNextFloatingEvent.substring(0, dateNextFloatingEvent.lastIndexOf(Constants.STRING_PERIOD)));
                            eventData.put(Position_eventDateNextTime, dateNextFloatingEvent);
                        }
                        isEndless = false;
                        dateNextEvent = dateNextFloatingEvent;

                        //Дата предыдущего года
                        if (preferences_list_prev_events_scan_distance > 0) {
                            datePrevFloatingEvent = computeFloatingDate(eventDateString, -1);
                        }
                    }
                }
                eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(dateNextEvent);
            } catch (ParseException e1) {
                try {
                    eventDate = Objects.requireNonNull(sdf_india.get()).parse(dateNextEvent);
                } catch (ParseException e2) {
                    try {
                        eventDate = Objects.requireNonNull(sdf_uk.get()).parse(dateNextEvent);
                    } catch (ParseException e3) {
                        try {
                            dateNextEvent = eventDateString.replace(Constants.STRING_BDP_NO_YEAR, Integer.toString(today.get(Calendar.YEAR)));
                            eventDate = Objects.requireNonNull(sdf_java.get()).parse(dateNextEvent);
                        } catch (ParseException ignored) { /**/ }
                    }
                }
            }
            if (eventDate != null && isEndless && today.after(AppDateUtils.getCalendarFromDate(eventDate)))
                eventDate = AppDateUtils.addYear(eventDate, 1);
        }
        return new ComputedDateForFileEvent(eventDate, eventDateFirstTime, isEndless, isPassedEvent, datePrevFloatingEvent);
    }

    static class ComputedDateForFileEvent {
        public final String datePrevFloatingEvent;
        public final boolean isEndless;
        public final boolean isPassedEvent;
        public final Date dateEvent;
        public final Date dateEventFirstTime;

        public ComputedDateForFileEvent(Date dateEvent, Date dateEventFirstTime, boolean isEndless, boolean isPassedEvent, String datePrevFloatingEvent) {
            this.dateEvent = dateEvent;
            this.dateEventFirstTime = dateEventFirstTime;
            this.isEndless = isEndless;
            this.isPassedEvent = isPassedEvent;
            this.datePrevFloatingEvent = datePrevFloatingEvent;
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

    /**
     * Возвращает следующую дату плавающего события
     *
     * @param eventDateString Изначальная дата в формате DD.MM.YYYY
     * @param yearShift       Сколько лет прибавить или отнять
     * @return Дата в формате DD.MM.YYYY
     */
    @NonNull
    String computeFloatingDate(String eventDateString, int yearShift) {

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

                    cal = AppDateUtils.getEasterDateFor(eventYear, true);
                    if (cal != null) {
                        cal.add(Calendar.DAY_OF_YEAR, daysShift);
                        if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                            cal = AppDateUtils.getEasterDateFor(eventYear + 1, true);
                            if (cal != null) {
                                cal.add(Calendar.DAY_OF_YEAR, daysShift);
                                return Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal.getTime());
                            }
                        } else {
                            return Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal.getTime());
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

                    cal = AppDateUtils.getEasterDateFor(eventYear, false);
                    if (cal != null) {
                        cal.add(Calendar.DAY_OF_YEAR, daysShift);
                        if (cal.before(dateRubicon)) { //В этом году уже прошло, берём следующий год
                            cal = AppDateUtils.getEasterDateFor(eventYear + 1, false);
                            if (cal != null) {
                                cal.add(Calendar.DAY_OF_YEAR, daysShift);
                                return Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal.getTime());
                            }
                        } else {
                            return Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal.getTime());
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

                    return Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal.getTime());
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

            return Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal.getTime());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return eventDateString;
        }
    }

    /**
     * Определение типа события по заголовку
     *
     * @param eventLabel        Заголовок события
     * @param setOtherIfUnknown Ставить тип "Другое событие", если не определили
     * @param useEventYear      Дата - с годом
     * @return Объект {@link Event} с предзаполненными атрибутами события
     */
    @NonNull
    Event recognizeEventByLabel(@NonNull String eventLabel, boolean setOtherIfUnknown, boolean useEventYear) {
        //todo: Убрать useEventYear из параметров
        final boolean isEmptyLabel = eventLabel.isEmpty();

        try {

            if (!isEmptyLabel) {
                if (preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_BirthDay, eventLabel);

                } else if (preferences_other_event_labels != null && preferences_other_event_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_Other, eventLabel);

                } else if (preferences_holiday_event_labels != null && preferences_holiday_event_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_HolidayEvent, eventLabel);

                } else if (preferences_death_labels != null && preferences_death_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_Death, eventLabel);

                } else if (preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_Anniversary, eventLabel);

                } else if (preferences_another_event_labels != null && preferences_another_event_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_Another, eventLabel);

                } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_NameDay, eventLabel);

                } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel).find()) {

                    return createTypedEvent(Constants.Type_Crowning, eventLabel);

                } else if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel).find()) {

                    Event typedEvent = createTypedEvent(Constants.Type_Custom1, eventLabel);
                    typedEvent.useEventYear = preferences_customevent1_useyear;
                    return typedEvent;

                } else if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel).find()) {

                    Event typedEvent = createTypedEvent(Constants.Type_Custom2, eventLabel);
                    typedEvent.useEventYear = preferences_customevent2_useyear;
                    return typedEvent;

                } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel).find()) {

                    Event typedEvent = createTypedEvent(Constants.Type_Custom3, eventLabel);
                    typedEvent.useEventYear = preferences_customevent3_useyear;
                    return typedEvent;

                } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel).find()) {

                    Event typedEvent = createTypedEvent(Constants.Type_Custom4, eventLabel);
                    typedEvent.useEventYear = preferences_customevent4_useyear;
                    return typedEvent;

                } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel).find()) {

                    Event typedEvent = createTypedEvent(Constants.Type_Custom5, eventLabel);
                    typedEvent.useEventYear = preferences_customevent5_useyear;
                    return typedEvent;

                }
            }

            if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Other && setOtherIfUnknown) {

                return createTypedEvent(Constants.Type_Other, eventLabel);

            } else if (preferences_rules_unrecognized == Rules_Unrecognized_Type_Unrecognized) {

                return createTypedEvent(Constants.Type_Unrecognized, eventLabel);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        Event unrecognizedEvent = createTypedEvent(Constants.Type_Unrecognized, eventLabel);
        unrecognizedEvent.useEventYear = useEventYear;
        return unrecognizedEvent;
    }

    /**
     * Создаёт событие указанного типа с предопределёнными атрибутами
     *
     * @param eventType   Тип события
     * @param eventLabel  Заголовок пользовательского события
     * @return Предзаполненное событие
     */
    @NonNull
    Event createTypedEvent(int eventType, @NonNull String eventLabel) {

        Event event = new Event();

        try {

            event.label = eventLabel;

            switch (eventType) {
                case Constants.Type_BirthDay:

                    event.caption = getResources().getString(R.string.event_type_birthday);
                    event.type = Constants.EventType_BirthDay;
                    event.subType = Constants.EventType_BirthDay;
                    event.icon = R.drawable.ic_event_birthday;
                    event.emoji = getResources().getString(R.string.event_type_birthday_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Other:

                    event.caption = getResources().getString(R.string.event_type_other);
                    event.type = Constants.EventType_Other;
                    event.subType = Constants.EventType_Other;
                    event.icon = R.drawable.ic_event_other;
                    event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                    event.needScanContacts = false;

                    break;
                case Constants.Type_HolidayEvent:

                    event.caption = getResources().getString(R.string.event_type_holiday);
                    event.type = Constants.EventType_Holiday;
                    event.subType = Constants.EventType_Holiday;
                    event.icon = R.drawable.ic_event_holiday;
                    event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_holiday_emoji) : "\uD83C\uDFD6️";
                    event.needScanContacts = false;

                    break;
                case Constants.Type_Death:

                    event.caption = getResources().getString(R.string.event_type_death);
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Death;
                    event.icon = R.drawable.ic_event_death;
                    event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_death_emoji) : "\uD83D\uDCC5";
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Anniversary:

                    event.caption = getResources().getString(R.string.event_type_anniversary);
                    event.type = Constants.EventType_Anniversary;
                    event.subType = Constants.EventType_Anniversary;
                    event.icon = R.drawable.ic_event_wedding;
                    event.emoji = getResources().getString(R.string.event_type_wedding_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_NameDay:

                    event.caption = getResources().getString(R.string.event_type_nameday);
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_NameDay;
                    event.icon = R.drawable.ic_event_nameday;
                    event.emoji = getResources().getString(R.string.event_type_nameday_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Crowning:

                    event.caption = getResources().getString(R.string.event_type_crowning);
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Crowning;
                    event.icon = R.drawable.ic_event_crowning;
                    event.emoji = getResources().getString(R.string.event_type_crowning_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Another:

                    event.caption = getResources().getString(R.string.event_type_another);
                    event.type = Constants.EventType_Another;
                    event.subType = Constants.EventType_Another;
                    event.icon = R.drawable.ic_event_other;
                    event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Custom1:

                    event.caption = preferences_customevent1_caption;
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Custom1;
                    event.icon = R.drawable.ic_event_custom1;
                    event.emoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_custom1_emoji) : "\uD83D\uDCC6";
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Custom2:

                    event.caption = preferences_customevent2_caption;
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Custom2;
                    event.icon = R.drawable.ic_event_custom2;
                    event.emoji = getResources().getString(R.string.event_type_custom2_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Custom3:

                    event.caption = preferences_customevent3_caption;
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Custom3;
                    event.icon = R.drawable.ic_event_custom3;
                    event.emoji = getResources().getString(R.string.event_type_custom3_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Custom4:

                    event.caption = preferences_customevent4_caption;
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Custom4;
                    event.icon = R.drawable.ic_event_custom4;
                    event.emoji = getResources().getString(R.string.event_type_custom4_emoji);
                    event.needScanContacts = true;

                    break;
                case Constants.Type_Custom5:

                    event.caption = preferences_customevent5_caption;
                    event.type = Constants.EventType_Custom;
                    event.subType = Constants.EventType_Custom5;
                    event.icon = R.drawable.ic_event_custom5;
                    event.emoji = getResources().getString(R.string.event_type_custom5_emoji);
                    event.needScanContacts = true;

                    break;
                default:

                    event.caption = getResources().getString(R.string.event_type_unrecognized);
                    event.type = Constants.EventType_Unrecognized;
                    event.subType = Constants.EventType_Unrecognized;
                    event.icon = R.drawable.ic_event_unknown;
                    event.emoji = getResources().getString(R.string.event_type_unknown_emoji);
                    event.needScanContacts = false;

                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return event;
    }

    /**
     * Является ли тип события событием контакта
     *
     * @param eventType Тип события
     * @return true, false
     */
    static boolean isContactEventType(int eventType) {
        return eventType == Constants.Type_BirthDay ||
                eventType == Constants.Type_Death ||
                eventType == Constants.Type_Anniversary ||
                eventType == Constants.Type_NameDay ||
                eventType == Constants.Type_Crowning ||
                eventType == Constants.Type_Another ||
                eventType == Constants.Type_Custom1 ||
                eventType == Constants.Type_Custom2 ||
                eventType == Constants.Type_Custom3 ||
                eventType == Constants.Type_Custom4 ||
                eventType == Constants.Type_Custom5;
    }

    @NonNull
    String readFileToString(@NonNull String file, String delimiter) {

        StringBuilder sb = new StringBuilder();

        try {

            String[] fileDetails = file.split(Constants.REGEX_BAR);
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
                        if (!line.startsWith(Constants.STRING_HASH) || !line.startsWith(Constants.STRING_DSLASH)) {
                            sb.append(line);
                            if (delimiter != null) sb.append(delimiter);
                        }
                        line = reader.readLine();
                    }
                    if (inputStream != null) inputStream.close();
                } catch (SecurityException se) {
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

    enum PhotoType {
        CONTACT_PHOTO, EVENT_PHOTO, SILHUETE, ICON
    }

    static class EventPhoto {
        public final Bitmap bitmap;
        public final PhotoType type;

        public EventPhoto(Bitmap bitmap, PhotoType type) {
            this.bitmap = bitmap;
            this.type = type;
        }
    }

    /**
     * Возвращает фото для события
     *
     * @param event            Данные о событии
     * @param showPhotos       Показывать фото (иначе - пиктограммы)
     * @param suggestSquared   Делать фото квадратным
     * @param addFavoritesSign Добавить значок избранного контакта
     * @param roundingFactor   Параметры скругления углов
     * @return Фото
     */
    Bitmap getEventPhoto(@NonNull String event, boolean showPhotos, boolean suggestSquared, boolean addFavoritesSign, int roundingFactor) {
        return getEventPhotoInternal(event, showPhotos, suggestSquared, addFavoritesSign, roundingFactor).bitmap;
    }

    /**
     * Возвращает фото для события
     *
     * @param event            Данные о событии
     * @param showPhotos       Показывать фото (иначе - пиктограммы)
     * @param suggestSquared   Делать фото квадратным
     * @param addFavoritesSign Добавить значок избранного контакта
     * @param roundingFactor   Параметры скругления углов
     * @return EventPhoto (bitmap + question)
     */
    @NonNull
    EventPhoto getEventPhotoInternal(@NonNull String event, boolean showPhotos, boolean suggestSquared, boolean addFavoritesSign, int roundingFactor) {

        boolean makeSquared = suggestSquared;
        boolean addMourningTape = false;
        Bitmap bm = null;
        PhotoType type = null;

        try {

            String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
            String eventType = singleEventArray[Position_eventType];
            String eventSubType = singleEventArray[Position_eventSubType];

            String eventPhotoData = singleEventArray[Position_photo];
            String eventPhoto = null;
            if (!TextUtils.isEmpty(eventPhotoData) && showPhotos) {
                try {
                    if (eventPhotoData.startsWith(Constants.STRING_BRACKETS_START)) { //Описание фото
                        TreeMap<Integer, String> localEvent = getLocalEvent(singleEventArray[Position_eventID]);
                        if (localEvent != null) {
                            eventPhoto = localEvent.get(Position_photo);
                            localEvent.clear();
                        }
                    } else { //Само фото (BASE64)
                        eventPhoto = eventPhotoData;
                    }
                    if (eventPhoto != null && !eventPhoto.isEmpty()) {
                        byte[] decodedBytes = Base64.decode(eventPhoto, Base64.DEFAULT);
                        bm = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    }
                    if (bm != null) type = PhotoType.EVENT_PHOTO;
                } catch (Exception ignored) { /**/ }
            }

            if (bm == null) {
                if (eventType.equals(Constants.EventType_Unrecognized)) {

                    bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_unknown);
                    type = PhotoType.ICON;

                } else if ((
                        eventSubType.equals(Constants.EventType_Calendar)
                                || eventSubType.equals(Constants.EventType_File)
                                || eventSubType.equals(Constants.EventType_Other)
                ) && TextUtils.isEmpty(singleEventArray[Position_photo_uri])) {

                    bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_other);
                    type = PhotoType.ICON;

                } else if (eventSubType.equals(Constants.EventType_Holiday)) {

                    bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_event_holiday);
                    type = PhotoType.ICON;

                    //todo: https://stackoverflow.com/questions/77168650/draw-emoji-to-image-in-android
                    //https://stackoverflow.com/questions/41212092/drawing-emojis-on-android-canvas-using-unicode-values
                    //https://stackoverflow.com/questions/47807621/draw-emoji-on-bitmap-with-drawtextonpath

                } else {

                    @NonNull String contactID = StringUtils.getNotNullString(singleEventArray[Position_contactID]);
                    String personFullName = singleEventArray[Position_personFullName];

                    addMourningTape = (preferences_sad_photo == 1 && eventSubType.equals(Constants.EventType_Death)) ||
                            (preferences_sad_photo == 2 && (deathDatesForIds.containsKey(contactID) || deathDatesForNames.containsKey(personFullName)));

                    if (showPhotos && !TextUtils.isEmpty(singleEventArray[Position_photo_uri]) && !singleEventArray[Position_photo_uri].equalsIgnoreCase(Constants.STRING_NULL)) {
                        //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                        if (contentResolver == null) contentResolver = context.getContentResolver();
                        Uri contactUri;
                        if (!contactID.isEmpty()) {
                            contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactID);
                        } else {
                            contactUri = Uri.parse(singleEventArray[Position_photo_uri].replace("/photo", ""));
                        }
                        InputStream photo_stream = null;
                        try {
                            photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri, true);
                        } catch (SecurityException ignored) { /**/ }
                        if (photo_stream != null) {
                            BufferedInputStream buf = new BufferedInputStream(photo_stream);
                            bm = BitmapFactory.decodeStream(buf);
                            buf.close();
                            photo_stream.close();
                            if (bm != null) type = PhotoType.CONTACT_PHOTO;
                        }
                    }

                    if (bm == null) {
                        //Если событие - не день рождения, пытаемся достать возраст из дня рождения
                        Date birthDate = null;
                        Date BDay = null;
                        boolean setNoAge = false;

                        if (!eventSubType.equals(Constants.EventType_BirthDay)) {
                            if (!contactID.isEmpty() && birthdayDatesForIds.containsKey(contactID)) {
                                birthDate = birthdayDatesForIds.get(contactID);
                            } else if (birthdayDatesForNames.containsKey(personFullName)) {
                                birthDate = birthdayDatesForNames.get(personFullName);
                            } else {
                                setNoAge = true;
                            }
                        }
                        try {
                            BDay = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                        } catch (ParseException ignored) { /**/ }
                        if (birthDate != null && BDay != null) {

                            List<String> singleRowList = Arrays.asList(singleEventArray);
                            final int countYearsDiff = AppDateUtils.countYearsDiff(birthDate, BDay);
                            if (countYearsDiff > 0) {
                                singleRowList.set(Position_age, String.valueOf(countYearsDiff));
                            } else {
                                //Если день рождения без года - мы об этом никак не узнаем
                                singleRowList.set(Position_age, Constants.STRING_MINUS1);
                            }
                            singleEventArray = singleRowList.toArray(new String[0]);

                        } else if (eventSubType.equals(Constants.EventType_Death) && setNoAge) {

                            //Если у персоны с годовщиной смерти нет дня рождения
                            List<String> singleRowList = Arrays.asList(singleEventArray);
                            singleRowList.set(Position_age, Constants.STRING_MINUS1);
                            singleEventArray = singleRowList.toArray(new String[0]);

                        }

                        int resIconPack_event = -1;
                        try {
                            resIconPack_event = Integer.parseInt(this.resources.getString(R.string.pref_IconPack_event));
                        } catch (NumberFormatException ignored) { /**/ }
                        if (preferences_IconPackNumber == resIconPack_event) { //Иконка типа события
                            bm = BitmapFactory.decodeResource(getResources(), getEventIcon(eventType, eventSubType));
                            if (bm != null) type = PhotoType.ICON;
                        }
                        if (bm == null) { //Случайное фото в соответствии с возрастом и полом

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
                            boolean foundInPeriod = false;
                            int beforeAge = 0;
                            if (person.Age >= 0) {
                                if (gender == 2) {
                                    for (Map.Entry<Integer, Integer> entry : preferences_IconPackImages_F.entrySet()) {
                                        beforeAge = entry.getKey();
                                        if (beforeAge > 0 && person.Age <= beforeAge) {
                                            idPhoto = preferences_IconPackImages_F.get(beforeAge);
                                            foundInPeriod = true;
                                            break;
                                        }
                                    }
                                    if (!foundInPeriod) {
                                        idPhoto = preferences_IconPackImages_F.get(beforeAge);
                                    }
                                } else {
                                    for (Map.Entry<Integer, Integer> entry : preferences_IconPackImages_M.entrySet()) {
                                        beforeAge = entry.getKey();
                                        if (beforeAge > 0 && person.Age <= beforeAge) {
                                            idPhoto = preferences_IconPackImages_M.get(beforeAge);
                                            foundInPeriod = true;
                                            break;
                                        }
                                    }
                                    if (!foundInPeriod) {
                                        idPhoto = preferences_IconPackImages_M.get(beforeAge);
                                    }
                                }
                            }
                            if (idPhoto == null) return new EventPhoto(null, null);
                            bm = ImageUtils.getBitmap(context, idPhoto);
                            if (bm == null) {
                                return new EventPhoto(null, null);
                            } else {
                                type = PhotoType.SILHUETE;
                            }

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
                }
            }
            if (bm == null) return new EventPhoto(null, null);

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

            if (addMourningTape && bm.getConfig() != null) {
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
                canvas.drawLine((float) (bmWidth * 1.25 - widthCorrection * 1.4), (float) bmHeight / 2, (float) ((double) bmWidth / 2 - widthCorrection * 1.4), (float) (bmHeight * 1.25), paintStroke);
                canvas.drawLine((float) (bmWidth * 1.25 + widthCorrection * 1.4), (float) bmHeight / 2, (float) ((double) bmWidth / 2 + widthCorrection * 1.4), (float) (bmHeight * 1.25), paintStroke);

                bm.recycle();
                bm = bmOverlay;
            }

            //Добавление иконки избранного
            final String eventKey = getEventKey(singleEventArray);
            final String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);
            if (addFavoritesSign && preferences_list_event_info.contains(context.getString(R.string.pref_List_EventInfo_FavoritesIcon))
                    && checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[Position_starred])
                    && bm.getConfig() != null) {
                Bitmap bmOverlay = Bitmap.createBitmap(bmWidth, bmHeight, bm.getConfig());
                Canvas canvas = new Canvas(bmOverlay);
                canvas.drawBitmap(bm, new Matrix(), null);
                bm.recycle();
                Bitmap bmStar = BitmapFactory.decodeResource(getResources(), R.drawable.fav_star);
                final Bitmap bmStarScaled = Bitmap.createScaledBitmap(bmStar, bmOverlay.getWidth() / 4 - (bmOverlay.getWidth() - bmOverlay.getHeight()) / 4, bmOverlay.getHeight() / 4, true);

                if (roundingFactor < 3) { //Не круг - рисуем в левом нижнем углу

                    canvas.drawBitmap(bmStarScaled, 2 + (float) ((bmOverlay.getWidth() - bmOverlay.getHeight()) / 4), (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

                } else if (roundingFactor < 9) { //Закругление - рисуем в левом нижнем углу правее

                    canvas.drawBitmap(bmStarScaled, 10 + (float) ((bmOverlay.getWidth() - bmOverlay.getHeight()) / 8), (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

                } else { //Круг - рисуем внизу по центру

                    canvas.drawBitmap(bmStarScaled, (float) (bmOverlay.getWidth() * 3 / 4) / 2, (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

                }
                bmStar.recycle();
                bmStarScaled.recycle();
                bm = bmOverlay;

            }

            return new EventPhoto(ImageUtils.toRoundCorner(bm, roundingRadiusX, roundingRadiusY), type);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return new EventPhoto(null, null);
        }
    }

    /**
     * Возвращает иконку события по типу и подтипу
     *
     * @param eventType    Тип события
     * @param eventSubType Подтип события
     * @return Ссылка на ресурс иконки события
     */
    private static int getEventIcon(@NonNull String eventType, @NonNull String eventSubType) {
        switch (eventSubType) {
            case Constants.EventType_BirthDay:
                return R.drawable.ic_event_birthday;
            case Constants.EventType_Anniversary:
                return R.drawable.ic_event_wedding;
            case Constants.EventType_NameDay:
                return R.drawable.ic_event_nameday;
            case Constants.EventType_Crowning:
                return R.drawable.ic_event_crowning;
            case Constants.EventType_Death:
                return R.drawable.ic_event_death;
            case Constants.EventType_Holiday:
                return R.drawable.ic_event_holiday;
            case Constants.EventType_Custom1:
                return R.drawable.ic_event_custom1;
            case Constants.EventType_Custom2:
                return R.drawable.ic_event_custom2;
            case Constants.EventType_Custom3:
                return R.drawable.ic_event_custom3;
            case Constants.EventType_Custom4:
                return R.drawable.ic_event_custom4;
            case Constants.EventType_Custom5:
                return R.drawable.ic_event_custom5;
            case Constants.EventType_5K:
                return R.drawable.ic_event_medal;
            case Constants.EventType_Xdays:
                return R.drawable.ic_event_xdays;
            case Constants.EventType_Another:
                return R.drawable.ic_event_other;
        }
        if (eventType.equals(Constants.EventType_Other)) {
            return R.drawable.ic_event_other;
        }
        return R.drawable.ic_event_unknown;
    }

    @NonNull
    HashMap<String, String> getContactDataMulti(@NonNull Long contactId, @NonNull String[] columnNames) throws SecurityException {

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
                    resultMap.put(columnName, StringUtils.getNotNullString(dataUri.toString()));
                    map_contacts_data.put(contactId + columnName, StringUtils.getNotNullString(dataUri.toString()));
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
                    ContactsContract.Data.MIMETYPE + Constants.STRING_EQ_Q,
                    new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                    null
            );

            if (dataCursor != null) {
                while (dataCursor.moveToNext()) {
                    for (String columnName : columnNamesToFind) {
                        int columnIndex = dataCursor.getColumnIndex(columnName);
                        if (columnIndex > -1) {
                            contactData = dataCursor.getString(columnIndex);
                            resultMap.put(columnName, StringUtils.getNotNullString(contactData));
                            map_contacts_data.put(contactId + columnName, StringUtils.getNotNullString(contactData));
                        }
                    }
                }
                dataCursor.close();
            }

        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return resultMap;
    }

    @NonNull
    String getContactPhone(@NonNull Long contactId) {

        try {

            if (contactId == 0) return Constants.STRING_EMPTY;

            String phone = Constants.STRING_EMPTY;

            //https://stackoverflow.com/questions/8735683/retrieving-a-phone-number-with-contactscontract-in-android-function-doesnt-wo
            if (contentResolver == null) contentResolver = context.getContentResolver();
            Cursor phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + Constants.SQL_EQUAL + contactId,
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
            return StringUtils.getNotNullString(phone);

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
            Date currentDay = getToday().getTime();

            //setLocale();

            for (int i = 0; i < eventList.size(); i++) {
                computeDateForEvent(i, magicList, getToday(), currentDay);
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

    void computeDateForEvent(int eventIndex, @NonNull List<String> magicList, @NonNull Calendar now, @NonNull Date currentDay) {

        String singleEvent = Constants.STRING_EMPTY;

        try {
            long dayDiff = -1;
            boolean isYear = false;
            boolean isAD = true;
            Date eventDateFirstTime = null; //оригинальная дата события
            Date eventDateThisTime = null; //следующая дата события
            int age = 0;

            singleEvent = eventList.get(eventIndex);
            if (singleEvent == null) return;

            String[] singleEventArray = singleEvent.split(Constants.STRING_EOT, -1);
            if (singleEventArray.length < Position_attrAmount) {
                eventList.set(eventIndex, Constants.STRING_EMPTY);
                return;
            }

            String[] dayArray = singleEventArray[Position_dates].split(Constants.STRING_2TILDA, -1);
            final String eventCaption = singleEventArray[Position_eventCaption];
            final String eventType = singleEventArray[Position_eventType];
            final String eventSubType = singleEventArray[Position_eventSubType];
            @NonNull final String contactID = StringUtils.getNotNullString(singleEventArray[Position_contactID]);
            increaseStatForEventTypes(eventType);

            if (TextUtils.isEmpty(singleEventArray[Position_eventDateNextTime])) { //Если дата следующего события не была посчитана при импорте события
                //перебираем все даты и находим максимальную
                final int nowYear = now.get(Calendar.YEAR);
                for (String dayValue : dayArray) {
                    String accountType = StringUtils.substringBefore(dayValue, Constants.STRING_COLON_SPACE);
                    String storedDate = StringUtils.substringBetween(dayValue, Constants.STRING_COLON_SPACE, Constants.STRING_COLON_SPACE);

                    increaseStatForEventSources(accountType);

                    Date storedDate_Date = null;
                    boolean storedDate_isYear = false;

                    if (storedDate.startsWith(Constants.STRING_2MINUS) || //Нет года, формат --MM-dd
                            storedDate.startsWith(Constants.STRING_0000_MINUS) || //Нет года, формат 0000-MM-dd
                            (storedDate.startsWith("1604-") && (accountType.contains(Constants.account_exchange) || accountType.contains(Constants.account_google))) || //Нет года, формат 1604-MM-dd - com.google.android.gm.exchange https://stackoverflow.com/questions/14023390/nsdate-return-1604-for-year-value
                            (storedDate.startsWith("1904-") && accountType.contains(Constants.account_huawei)) || //Нет года, формат 1904-MM-dd - com.android.huawei.phone
                            (!TextUtils.isEmpty(eventCaption) && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventCaption.toLowerCase()).find()) //Именины считаем без года
                    ) {

                        try {
                            eventDateThisTime = Objects.requireNonNull(sdf_java.get()).parse(nowYear + Constants.STRING_MINUS + storedDate.substring(storedDate.startsWith(Constants.STRING_2MINUS) ? 2 : 5));
                        } catch (ParseException e) {
                            //Не получилось распознать
                        }
                        if (eventDateThisTime != null) {
                            long dayDiff_tmp = AppDateUtils.countDaysDiff(currentDay, eventDateThisTime);
                            if (dayDiff_tmp < 0)
                                eventDateThisTime = AppDateUtils.addYear(eventDateThisTime, 1);
                            storedDate_Date = eventDateThisTime;
                        }

                    } else { //Обычный формат yyyy-MM-dd

                        storedDate_isYear = true;

                        // 1. Сначала пытаемся распарсить формат с указанием эры (до нашей эры)
                        try {
                            storedDate_Date = Objects.requireNonNull(sdf_java_G.get()).parse(storedDate);
                            isAD = false; // Если парсер съел строку с эрой, значит это BC (до нашей эры)
                        } catch (ParseException ignored) {
                            // Эры в строке нет или формат не подошел, идем дальше
                        }

                        // 2. Если дата с эрой не распарсилась, пробуем остальные стандартные форматы С годом
                        if (storedDate_Date == null) {
                            SimpleDateFormat[] formatsWithYear = {
                                    sdf_java.get(),
                                    sdf_ru.get(),
                                    sdf_uk.get(),
                                    sdf_india.get(),
                                    sdf_YYYYMMDD_noDiv.get(),
                                    sdf_MMMMDYYYY.get(),
                                    sdf_DDMMYYYY.get()
                            };
                            storedDate_Date = AppDateUtils.parseDateWithFormats(storedDate, formatsWithYear);
                        }

                        // 3. Если ни один формат с годом не подошел, пробуем форматы БЕЗ года
                        if (storedDate_Date == null) {
                            storedDate_isYear = false; // Помечаем, что год не определен
                            SimpleDateFormat[] formatsNoYear = {
                                    sdf_uk_no_year.get(),
                                    sdf_india_no_year.get()
                                    // Добавьте сюда остальные ваши форматы без года, если они есть
                            };
                            storedDate_Date = AppDateUtils.parseDateWithFormats(storedDate, formatsNoYear);
                        }
                    }

                    if (storedDate_Date != null) {
                        if (eventDateFirstTime == null) {
                            eventDateFirstTime = storedDate_Date;
                            isYear = storedDate_isYear;
                        } else if (storedDate_isYear
                                && storedDate_Date.before(now.getTime())
                                && (!isYear || AppDateUtils.countDaysDiff(eventDateFirstTime, storedDate_Date) > 0)) { //Если у пользователя несколько дат, берём наименьший возраст todo: можно вынести в настройку - в какую сторону округлять
                            eventDateFirstTime = storedDate_Date;
                            isYear = true;
                        }
                    }
                }

                if (eventDateFirstTime != null) {

                    if (isYear) { //Дата с годом
                        if (isAD) {
                            singleEventArray[Position_eventDateFirstTime] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDateFirstTime); //оригинальное событие
                        } else {
                            singleEventArray[Position_eventDateFirstTime] = Objects.requireNonNull(sdf_DDMMY.get()).format(eventDateFirstTime) + resources.getString(R.string.msg_after_year_bc); //до н.э.
                        }
                    } else { //Дата без года
                        singleEventArray[Position_eventDateFirstTime] = Objects.requireNonNull(sdf_DDMM.get()).format(eventDateFirstTime); //оригинальное событие без года
                    }

                    if (isYear) { //в eventDateFirstTime - оригинальное событие

                        Calendar cal = AppDateUtils.getCalendarFromDate(eventDateFirstTime);
                        try {
                            eventDateThisTime = Objects.requireNonNull(sdf_java.get()).parse(nowYear + Constants.STRING_MINUS + (cal.get(Calendar.MONTH) + 1) + Constants.STRING_MINUS + cal.get(Calendar.DAY_OF_MONTH));
                            if (eventDateThisTime != null) {
                                long dayDiff_tmp = AppDateUtils.countDaysDiff(currentDay, eventDateThisTime);
                                if (dayDiff_tmp < 0)
                                    eventDateThisTime = AppDateUtils.addYear(eventDateThisTime, 1);
                            }
                        } catch (ParseException e) {
                            if (cal.get(Calendar.MONTH) == Calendar.FEBRUARY && cal.get(Calendar.DAY_OF_MONTH) == 29) {
                                try {
                                    eventDateThisTime = Objects.requireNonNull(sdf_java.get()).parse(nowYear + "-03-01");
                                    if (eventDateThisTime != null) {
                                        long dayDiff_tmp = AppDateUtils.countDaysDiff(currentDay, eventDateThisTime);
                                        if (dayDiff_tmp < 0)
                                            eventDateThisTime = AppDateUtils.addYear(eventDateThisTime, 1);
                                    }
                                } catch (ParseException ignored) { /**/ }
                            }
                        }
                    }

                }

                if (eventDateThisTime != null) {
                    if (TextUtils.isEmpty(singleEventArray[Position_eventDateNextTime]))
                        singleEventArray[Position_eventDateNextTime] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDateThisTime); //следующая дата события
                }

            } else { //Дата следующего события уже посчитана (обычно такое с событиями из календарей)
                try {
                    isYear = true;
                    for (String dayValue : dayArray) {
                        String storedDate = StringUtils.substringBetween(dayValue, Constants.STRING_COLON_SPACE, Constants.STRING_COLON_SPACE);
                        if (storedDate.startsWith(Constants.STRING_2MINUS)) {
                            isYear = false;
                        } else {
                            isYear = true;
                            break;
                        }
                    }
                    if (isYear) {
                        eventDateFirstTime = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateFirstTime]);
                    } else {
                        String strDateFirstTime = singleEventArray[Position_eventDateFirstTime];
                        if (strDateFirstTime.length() > 5) {
                            singleEventArray[Position_eventDateFirstTime] = strDateFirstTime.substring(0, 5); //оригинальное событие без года
                        }
                    }
                } catch (ParseException e) { /**/ }
                try {
                    eventDateThisTime = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                } catch (ParseException e) { /**/ }

                if (!dayArray[0].isEmpty()) {
                    increaseStatForEventSources(StringUtils.substringBefore(dayArray[0], Constants.STRING_COLON_SPACE));
                }

            }

            final String eventKey = getEventKey(singleEventArray);
            final String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);

            if (eventDateThisTime != null) {
                dayDiff = AppDateUtils.countDaysDiff(currentDay, eventDateThisTime);
                //Если до события больше года - убираем его
                if (dayDiff > 365 + (AppDateUtils.isLeapYear(eventDateThisTime.getYear()) ? 1 : 0)) {
                    eventList.set(eventIndex, Constants.STRING_EMPTY);
                    return;
                }

                if (eventDateFirstTime != null && isYear) {
                    age = AppDateUtils.countYearsDiff(eventDateFirstTime, eventDateThisTime); //Считаем, сколько будет лет
                    if (!isAD) age--;
                    if (!TextUtils.isEmpty(contactID)) {
                        if (eventSubType.equals(Constants.EventType_BirthDay) && !birthdayDatesForIds.containsKey(contactID)) {
                            birthdayDatesForIds.put(contactID, eventDateFirstTime);
                        } else if (eventSubType.equals(Constants.EventType_Death)) {
                            deathDatesForIds.put(contactID, eventDateFirstTime);
                        }
                    }
                }
            }

            if (dayDiff == -1) {

                Log.i(TAG, resources.getString(R.string.msg_date_parse_error) + getEventDataAsString(singleEventArray));

                StringBuilder sb = new StringBuilder();
                String dates;
                if (TextUtils.isEmpty(singleEventArray[Position_eventDateNextTime])) {
                    dates = singleEventArray[Position_dates];
                } else {
                    dates = singleEventArray[Position_eventDateNextTime];
                }
                sb.append(resources.getString(R.string.msg_date_parse_error))
                        .append(singleEventArray[Position_eventSource])
                        .append(Constants.STRING_COMMA_SPACE)
                        .append(dates)
                        .append(Constants.STRING_COMMA_SPACE)
                        .append(singleEventArray[Position_personFullName]
                        );
                ToastExpander.showInfoMsg(context, sb.toString());

                eventList.set(eventIndex, Constants.STRING_EMPTY);
                return;

            }

            singleEventArray[Position_eventDistance] = Long.toString(dayDiff);
            singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDiff, eventDateThisTime);

            if (isYear) {
                if (age > 0) { //Возраст больше 1 года
                    singleEventArray[Position_age] = Integer.toString(age);
                    singleEventArray[Position_age_caption] = setAgeFormatting(StringUtils.getAgeString(age, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20, currentLocale, resources));

                    if (eventType.equals(Constants.EventType_Anniversary)) {
                        @Nullable String anCaption = getWeddingName(age);
                        if (StringUtils.hasContent(anCaption) && !eventCaption.contains(Constants.STRING_PARENTHESIS_OPEN)) {
                            singleEventArray[Position_eventCaption] = eventCaption.concat(Constants.STRING_PARENTHESIS_OPEN).concat(anCaption).concat(Constants.STRING_PARENTHESIS_CLOSE);
                        }
                    }
                } else if (eventDateFirstTime != null && AppDateUtils.countDaysDiff(eventDateFirstTime, eventDateThisTime) > 0) { //Возраст до года
                    //todo: если это календарное событие на несколько дней, то 1 день, это, по факту, 2й день события. додумать
                    singleEventArray[Position_age_caption] = setAgeFormatting(countDaysDiffText(eventDateFirstTime, eventDateThisTime, 1));
                } else {
                    singleEventArray[Position_age] = Constants.STRING_MINUS1;
                    singleEventArray[Position_age_caption] = Constants.STRING_EMPTY;
                }
                if (eventDateFirstTime != null) {
                    singleEventArray[Position_age_current] = fillCurrentAge(singleEventArray, eventSubType, countDaysDiffText(eventDateFirstTime, currentDay, 3), currentDay);
                }
            }

            if (eventSubType.equals(Constants.EventType_BirthDay)) {
                singleEventArray[Position_zodiacSign] = ZodiacHelper.getZodiacSignTitle(context, singleEventArray[Position_eventDateFirstTime]);
                singleEventArray[Position_zodiacYear] = ZodiacHelper.getChineseZodiacYear(context, singleEventArray[Position_eventDateFirstTime]);
            }

            //Сортировка: дней до даты + (с уведомлением, не скрыт, скрыт)
            singleEventArray[Position_eventDate_sorted] = getSortKey(singleEventArray);

            eventList.set(eventIndex, TextUtils.join(Constants.STRING_EOT, singleEventArray));

            if (checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[Position_starred])) {
                //Избранные для календарного виджета
                final String packHash = StringUtils.getHash(Constants.eventSourceFavoritePrefix);

                String anCaption = singleEventArray[Position_eventCaption];
                // Если это годовщина свадьбы - убираем название свадьбы. Оно будет вычислено
                // для конкретного для в {@link WidgetCalendar}
                if (eventType.equals(Constants.EventType_Anniversary)) {
                    anCaption = eventCaption;
                }
                String eventTitle = Constants.eventTitleFavoritePrefix
                        .concat(anCaption)
                        .concat(Constants.STRING_COLON_SPACE)
                        .concat(StringUtils.getFullName(singleEventArray, preferences_name_format));
                if (age > 0) {
                    String strDateFirstTime = singleEventArray[Position_eventDateFirstTime];
                    eventTitle += Constants.STRING_PARENTHESIS_OPEN
                            + strDateFirstTime.substring(strDateFirstTime.lastIndexOf(Constants.STRING_PERIOD) + 1)
                            + Constants.STRING_PARENTHESIS_CLOSE;
                }
                final DayType.Type dayType = DayType.Type.Holiday;
                final String key = packHash.concat(Constants.STRING_COLON).concat(Objects.requireNonNull(sdf_java_no_year.get()).format(eventDateThisTime));
                fillDayTypeAndInfo(key, dayType, eventTitle);
            } else if (Constants.EventType_Holiday.equals(singleEventArray[Position_eventSubType])
                    && singleEventArray[Position_dates].contains(Constants.eventSourceLocalPrefix)) {
                //Праздники в локальном событии для календарного виджета
                String[] dates = singleEventArray[Position_dates].split(Constants.STRING_2TILDA, -1);
                for (String date : dates) {
                    String[] dateElements = date.split(Constants.STRING_COLON_SPACE, -1);
                    if (dateElements.length == 3 && dateElements[0].equals(Constants.EVENT_PREFIX_LOCAL_EVENT)) {
                        String key = dateElements[2].concat(Constants.STRING_COLON).concat(dateElements[1]);
                        String eventTitle = Constants.eventTitleLocalPrefix
                                .concat(singleEventArray[Position_personFullName]);
                        fillDayTypeAndInfo(key, DayType.Type.Holiday, eventTitle);
                        break;
                    }
                }
            }

            if (age > 0) {

                if (eventType.equals(Constants.EventType_BirthDay) && (TextUtils.isEmpty(contactID) || !deathDatesForIds.containsKey(contactID))) {

                    //Вычисляем 5K даты
                    long days = AppDateUtils.countDaysDiff(eventDateFirstTime, currentDay);
                    long k = (days + 365) / 5000;
                    long closestMagicDayDistance = (days + 365) % 5000;

                    boolean isInsideYear = closestMagicDayDistance >= 0 && closestMagicDayDistance <= 365;
                    boolean isPrevious = preferences_list_prev_events_scan_distance > 0 && closestMagicDayDistance - 365 <= preferences_list_prev_events_scan_distance;
                    int magicDayDistance;
                    if (isInsideYear || isPrevious) {
                        //Формируем новую запись
                        Calendar cal5K = Calendar.getInstance();
                        magicDayDistance = (int) (365 - closestMagicDayDistance);
                        cal5K.add(Calendar.DATE, magicDayDistance);

                        String[] singleEventArray5K = singleEventArray.clone();

                        singleEventArray5K[Position_eventType] = Constants.EventType_5K;
                        singleEventArray5K[Position_eventSubType] = Constants.EventType_5K;
                        singleEventArray5K[Position_eventCaption] = "5K+";
                        singleEventArray5K[Position_eventLabel] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal5K.getTime());
                        //для выдачи даты юбилея, а не первоначального события: sdfYear.format(sdf.parse(cal5K.get(YEAR) + "-" + (cal5K.get(Calendar.MONTH) + 1) + "-" + cal5K.get(Calendar.DAY_OF_MONTH)));
                        singleEventArray5K[Position_eventDateNextTime] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(cal5K.getTime());
                        singleEventArray5K[Position_eventDateFirstTime] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDateFirstTime);
                        singleEventArray5K[Position_age] = Integer.toString(age);
                        singleEventArray5K[Position_age_caption] = setAgeFormatting(StringUtils.getAgeString(5 * k * 1000, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, currentLocale, resources));
                        singleEventArray5K[Position_eventDistance] = Integer.toString(magicDayDistance);
                        singleEventArray5K[Position_eventDistanceText] = getEventDistanceText(magicDayDistance, cal5K.getTime());
                        singleEventArray5K[Position_eventIcon] = Integer.toString(R.drawable.ic_event_medal); //https://www.flaticon.com/free-icon/medal_610333
                        singleEventArray5K[Position_eventEmoji] = resources.getString(R.string.event_type_5k_emoji);
                        singleEventArray5K[Position_age_current] = fillCurrentAge(singleEventArray, eventSubType, countDaysDiffText(eventDateFirstTime, currentDay, 3), currentDay); //Возраст текущий
                        singleEventArray5K[Position_eventDate_sorted] = getSortKey(singleEventArray5K);

                        if (isInsideYear) {
                            if (Constants.STRING_1.equals(singleEventArray5K[Position_starred])) {
                                statFavoriteEventsCount++;
                            }
                            magicList.add(TextUtils.join(Constants.STRING_EOT, singleEventArray5K));
                            increaseStatForEventTypes(Constants.EventType_5K);
                        } else if (isEventVisibleInList(singleEventArray5K)) {
                            eventListPrev.add(TextUtils.join(Constants.STRING_EOT, singleEventArray5K));
                        }
                    }
                }

                //Счётчики дней
                if (getXDaysEventsCount() > 0 && isXDaysEvent(eventKey)) {
                    final List<String> valuePeriods = getXDaysEvent(eventKey);
                    Calendar dateStart = AppDateUtils.getWithoutTime(Calendar.getInstance());
                    Calendar dateEnd = (Calendar) dateStart.clone();
                    dateEnd.add(Calendar.YEAR, 1);
                    int toRepeat = 365;
                    try {
                        if (!valuePeriods.get(1).isEmpty())
                            toRepeat = -Integer.parseInt(valuePeriods.get(1));
                    } catch (NumberFormatException e) { /**/ }

                    ArrayList<Event> events = getNextRepeatsForEvent(
                            dateStart,
                            dateEnd,
                            AppDateUtils.getCalendarFromDate(eventDateFirstTime),
                            valuePeriods.get(0),
                            toRepeat
                    );
                    if (!events.isEmpty()) {
                        for (Event event : events) {
                            String[] singleEventArrayXdays = singleEventArray.clone();
                            long xDaysDistance = AppDateUtils.countDaysDiff(currentDay, event.date);

                            singleEventArrayXdays[Position_eventDateNextTime] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(event.date);
                            singleEventArrayXdays[Position_age_caption] = setAgeFormatting(event.distance);
                            singleEventArrayXdays[Position_eventDistance] = Long.toString(xDaysDistance);
                            singleEventArrayXdays[Position_eventDistanceText] = getEventDistanceText(xDaysDistance, event.date);
                            singleEventArrayXdays[Position_eventEmoji] = resources.getString(R.string.event_type_xdays_emoji);
                            singleEventArrayXdays[Position_eventIcon] = Integer.toString(R.drawable.ic_event_xdays);
                            singleEventArrayXdays[Position_eventDescription] = Constants.STRING_EMPTY;
                            singleEventArrayXdays[Position_eventDate_sorted] = getSortKey(singleEventArrayXdays);
                            if (Constants.STRING_1.equals(singleEventArrayXdays[Position_starred])) {
                                statFavoriteEventsCount++;
                            }
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

    /**
     * Возвращает название годовщины свадьбы
     *
     * @param age Год годовщины
     * @return Название свадьбы
     */
    @SuppressLint("DiscouragedApi")
    @Nullable
    String getWeddingName(int age) {
        try {
            return context.getString(resources.getIdentifier(Constants.STRING_TYPE_WEDDING + age, Constants.RES_TYPE_STRING, context.getPackageName()));
        } catch (Resources.NotFoundException nfe) {
            return null;
        }
    }

    /**
     * Возвращает ключ сортировки события в общем списке
     *
     * @param singleEventArray данные события
     * @return ключ сортировки
     */
    @NonNull
    private String getSortKey(@NonNull String[] singleEventArray) {
        try {

            final String eventKey = getEventKey(singleEventArray);
            final String eventKeyWithRawIs = getEventKeyWithRawId(singleEventArray);
            boolean isFavoriteEvent = Constants.STRING_1.equals(singleEventArray[Position_starred]);

            // Оптимизируем textDistance: берём последние 3 символа, дополняя нулями слева
            String distStr = singleEventArray[Position_eventDistance];
            // Убираем минус (если есть)
            if (distStr.startsWith(Constants.STRING_MINUS)) {
                distStr = distStr.substring(1);
            }
            // Обрезаем до 3 символов, если длиннее
            if (distStr.length() > 3) {
                distStr = distStr.substring(distStr.length() - 3);
            }
            // Дополняем слева нулями до 3
            String textDistancePart;
            switch (distStr.length()) {
                case 1:
                    textDistancePart = "00" + distStr;
                    break;
                case 2:
                    textDistancePart = "0" + distStr;
                    break;
                case 3:
                    textDistancePart = distStr;
                    break;
                default:
                    textDistancePart = "000";
                    break; // на случай пустой строки
            }

            final String eventTypeStr = singleEventArray[Position_eventType];
            Integer typeId = getEventTypeInt(eventTypeStr);
            String eventTypeSort;
            switch (typeId) {
                case Constants.Type_BirthDay:
                    eventTypeSort = "1";
                    break;
                case Constants.Type_Anniversary:
                    eventTypeSort = "2";
                    break;
                case Constants.Type_Custom:
                    eventTypeSort = "3";
                    break;
                case Constants.Type_5K:
                    eventTypeSort = "5";
                    break;
                case Constants.Type_Other:
                    eventTypeSort = "6";
                    break;
                default:
                    eventTypeSort = "4";
                    break;
            }

            // Priority part: "0", "1", "2", "3"
            String priority;
            if (isFavoriteEvent) {
                priority = "0";
            } else if (checkIsHiddenEvent(eventKey, eventKeyWithRawIs)) {
                priority = "3";
            } else if (checkIsSilencedEvent(eventKey, eventKeyWithRawIs)) {
                priority = "2";
            } else {
                priority = "1";
            }

            return textDistancePart + priority + eventTypeSort;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + Arrays.toString(singleEventArray));
            return Constants.STRING_EMPTY;
        }
    }

    /**
     * Возвращает текущий возраст по данным события
     *
     * @param singleEventArray данные события
     * @param eventSubType     подтип события
     * @param currentAge       количество дней до события или после события
     * @param today            дата сегодня
     * @return текущий возраст с префиксом
     */
    @NonNull
    private String fillCurrentAge(@NonNull String[] singleEventArray, @NonNull String eventSubType, @NonNull String currentAge, Date today) {

        String agePrefix = "";
        try {

            @NonNull final String contactID = StringUtils.getNotNullString(singleEventArray[Position_contactID]);
            @NonNull final String personFullName = StringUtils.getNotNullString(singleEventArray[Position_personFullName]);

            if (eventSubType.equals(Constants.EventType_BirthDay) //Если это день рождения или 5K
                    || eventSubType.equals(Constants.EventType_5K)) {
                if (!currentAge.isEmpty() && !currentAge.startsWith(Constants.STRING_0)) {
                    if (deathDatesForIds.containsKey(contactID)) { //Но есть годовщина смерти
                        agePrefix = resources.getString(R.string.msg_age_could_be_now);

                        //Если годовщина смерти попалась раньше дня рождения, то у неё currentAge будет содержать текущий возраст - надо обновить
                        final String key = contactID + Constants.STRING_2HASH + Constants.EventType_Death;
                        if (map_eventsBySubtypeAndPersonID_offset.containsKey(key)) {
                            Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(key);
                            if (eventIndex != null && eventIndex <= eventList.size()) {
                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                Date birthDate = birthdayDatesForIds.get(contactID);
                                Date deathDate = deathDatesForIds.get(contactID);
                                if (birthDate != null && deathDate != null) {
                                    final String wasAge = countDaysDiffText(birthDate, deathDate, 3);
                                    singleRowList.set(Position_age_current, resources.getString(R.string.msg_age_was).concat(wasAge));
                                    eventList.set(eventIndex, TextUtils.join(Constants.STRING_EOT, singleRowList));
                                }
                            }
                        }

                    } else {
                        if (deathDatesForNames.containsKey(personFullName)) {
                            agePrefix = resources.getString(R.string.msg_age_could_be_now);

                            final String key = personFullName + Constants.STRING_2HASH + Constants.EventType_Death;
                            Integer eventIndex = map_eventsBySubtypeAndPersonName_offset.get(key);
                            if (eventIndex != null && eventIndex <= eventList.size()) {
                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                Date birthDate = birthdayDatesForNames.get(personFullName);
                                Date deathDate = deathDatesForNames.get(personFullName);
                                if (birthDate != null && deathDate != null) {
                                    final String wasAge = countDaysDiffText(birthDate, deathDate, 3);
                                    singleRowList.set(Position_age_current, resources.getString(R.string.msg_age_was).concat(wasAge));
                                    eventList.set(eventIndex, TextUtils.join(Constants.STRING_EOT, singleRowList));
                                }
                            }

                        } else {
                            agePrefix = resources.getString(R.string.msg_age_now);
                        }
                    }
                    agePrefix = agePrefix.concat(currentAge);
                }
            } else if (birthdayDatesForIds.containsKey(contactID)) {
                Date birthDate = birthdayDatesForIds.get(contactID);
                if (birthDate != null) {
                    if (eventSubType.equals(Constants.EventType_Death)) { //Если это годовщина смерти
                        Date eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateFirstTime]);
                        if (eventDate != null) {
                            agePrefix = resources.getString(R.string.msg_age_was).concat(countDaysDiffText(birthDate, eventDate, 3));
                        }
                        //Необходимо обновить текущий возраст в дне рождении
                        final String key = contactID + Constants.STRING_2HASH + Constants.EventType_BirthDay;
                        if (map_eventsBySubtypeAndPersonID_offset.containsKey(key)) {
                            Integer eventIndex = map_eventsBySubtypeAndPersonID_offset.get(key);
                            if (eventIndex != null && eventIndex <= eventList.size()) {
                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                singleRowList.set(Position_age_current, resources.getString(R.string.msg_age_could_be_now).concat(countDaysDiffText(birthDate, today, 3)));
                                eventList.set(eventIndex, TextUtils.join(Constants.STRING_EOT, singleRowList));
                            }
                        }
                    } else { //Другие события
                        Date eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                        if (eventDate != null) {
                            if (deathDatesForIds.containsKey(contactID)) { //Но есть годовщина смерти
                                agePrefix = resources.getString(R.string.msg_age_could_be);
                            } else if (eventDate.compareTo(today) == 0) {
                                agePrefix = resources.getString(R.string.msg_age_now);
                            } else {
                                agePrefix = resources.getString(R.string.msg_age_will_be);
                            }
                            agePrefix = agePrefix.concat(countDaysDiffText(birthDate, eventDate, 3));
                        }
                    }
                }
            } else if (birthdayDatesForNames.containsKey(personFullName)) {
                Date birthDate = birthdayDatesForNames.get(personFullName);
                if (birthDate != null) {
                    if (eventSubType.equals(Constants.EventType_Death)) { //Если это годовщина смерти
                        Date eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateFirstTime]);
                        if (eventDate != null) {
                            agePrefix = resources.getString(R.string.msg_age_was).concat(countDaysDiffText(birthDate, eventDate, 3));
                        }
                        //Необходимо обновить текущий возраст в дне рождении
                        final String key = personFullName + Constants.STRING_2HASH + Constants.EventType_BirthDay;
                        if (map_eventsBySubtypeAndPersonName_offset.containsKey(key)) {
                            Integer eventIndex = map_eventsBySubtypeAndPersonName_offset.get(key);
                            if (eventIndex != null && eventIndex <= eventList.size()) {
                                List<String> singleRowList = Arrays.asList(eventList.get(eventIndex).split(Constants.STRING_EOT, -1));
                                singleRowList.set(Position_age_current, resources.getString(R.string.msg_age_could_be_now).concat(countDaysDiffText(birthDate, today, 3)));
                                eventList.set(eventIndex, TextUtils.join(Constants.STRING_EOT, singleRowList));
                            }
                        }
                    } else { //Другие события
                        Date eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                        if (eventDate != null) {
                            if (deathDatesForNames.containsKey(personFullName)) { //Но есть годовщина смерти
                                agePrefix = resources.getString(R.string.msg_age_could_be);
                            } else if (eventDate.compareTo(today) == 0) {
                                agePrefix = resources.getString(R.string.msg_age_now);
                            } else {
                                agePrefix = resources.getString(R.string.msg_age_will_be);
                            }
                            agePrefix = agePrefix.concat(countDaysDiffText(birthDate, eventDate, 3));
                        }
                    }
                }
            } else if (Constants.STRING_STORAGE_CONTACTS.equals(singleEventArray[Position_eventStorage])
                    || Constants.STRING_STORAGE_LOCAL.equals(singleEventArray[Position_eventStorage])) {
                if (eventSubType.equals(Constants.EventType_Death)) { //Если это годовщина смерти
                    agePrefix = resources.getString(R.string.msg_age_passed).concat(currentAge);
                } else {
                    agePrefix = resources.getString(R.string.msg_age_now).concat(currentAge);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + TextUtils.join(Constants.STRING_EOT, singleEventArray));
        }
        return agePrefix;
    }

    private void increaseStatForEventSources(@NonNull String sourceType) {
        if (!statEventSources.containsKey(sourceType)) {
            statEventSources.put(sourceType, 1);
        } else {
            Integer oldCount = statEventSources.get(sourceType);
            statEventSources.put(sourceType, (oldCount == null ? 0 : oldCount) + 1);
        }
    }

    private void increaseStatForEventSourcesIds(@NonNull String sourceId) {
        if (!statEventSourcesIds.containsKey(sourceId)) {
            statEventSourcesIds.put(sourceId, 1);
        } else {
            Integer oldCount = statEventSourcesIds.get(sourceId);
            statEventSourcesIds.put(sourceId, (oldCount == null ? 0 : oldCount) + 1);
        }
    }

    private void increaseStatForEventTypes(@NonNull String eventType) {
        if (!statEventTypes.containsKey(eventType)) {
            statEventTypes.put(eventType, 1);
        } else {
            Integer oldCount = statEventTypes.get(eventType);
            statEventTypes.put(eventType, (oldCount == null ? 0 : oldCount) + 1);
        }
    }

    /**
     * Возвращает подробности даты предстоящего события
     *
     * @param dayDiff   Дней до события
     * @param eventDate Дата события
     * @return Детали разделены |, например: через 5 дней|в понедельник|15 февраля|вт
     */
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
                            .append(StringUtils.getAgeString(dayDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, currentLocale, resources))
                            .append(currentLanguage.equals(getResources().getString(R.string.pref_Language_de)) ? "n" : Constants.STRING_EMPTY); //для немецкого "in 10 TageN"
                } else if (dayDiff == -1) { //Вчера
                    eventDistance.append(getResources().getString(R.string.msg_yesterday));
                } else if (dayDiff == -2) { //Позавчера
                    eventDistance.append(getResources().getString(R.string.msg_before_yesterday));
                } else { //Подальше назад
                    eventDistance
                            .append(getResources().getString(R.string.msg_after_event_prefix))
                            .append(StringUtils.getAgeString(-dayDiff, R.string.msg_after_day_prefix_1, R.string.msg_after_day_prefix_1_, R.string.msg_after_day_prefix_2_3_4, R.string.msg_after_day_prefix_5_20, currentLocale, resources))
                            .append(getResources().getString(R.string.msg_after_event_postfix));
                }
            }

            SimpleDateFormat sdfOut = ContactsEvents.getInstance().getSdfOut();
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
                    .append(getResources().getStringArray(R.array.weekDaysShort)[c1.get(Calendar.DAY_OF_WEEK) - 1])
                    .append(Constants.STRING_BAR)
                    .append(c1.get(Calendar.YEAR));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e + Constants.STRING_EOL + dayDiff + Constants.STRING_EOL + eventDate);
        }
        return eventDistance.toString();
    }

    List<String> getPreviousEvents(@NonNull List<String> dataList) {

        List<String> result = new ArrayList<>();
        statEventsPrevEventsFound = 0;
        if (dataList.isEmpty()) return result;

        try {

            //Собираем события
            int params_days = Constants.PREV_EVENTS_MAX_DAYS;
            int params_events = 10000;
            //todo: переделать под regexp
            switch (preferences_list_prev_events) {
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

            Date currentDay = getToday().getTime();

            List<String> listPrevEventsPreparatory = new ArrayList<>();
            List<String> listPrevEventsDates = new ArrayList<>();

            //События внизу списка событий (ежегодные)
            for (int i = dataList.size() - 1; i >= 0 && statEventsPrevEventsFound < params_events; i--) {
                String li = dataList.get(i);
                String[] singleEventArray = li.split(Constants.STRING_EOT, -1);
                if (!singleEventArray[Position_eventSubType].equals(Constants.EventType_5K) //пропускаем 5K+
                        && !singleEventArray[Position_eventSubType].equals(Constants.EventType_Calendar) //пропускаем события календаря
                        && !singleEventArray[Position_notAnnualEvent].equals(Constants.STRING_1) //пропускаем не ежегодные события
                ) {
                    Date eventDate = null;
                    try {
                        eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                        if (eventDate != null) {
                            eventDate = AppDateUtils.addYear(eventDate, -1);
                        }
                    } catch (Exception e) { /**/ }

                    if (eventDate != null && !eventDate.equals(currentDay)) {
                        long eventDistance = AppDateUtils.countDaysDiff(eventDate, currentDay);
                        if (eventDistance <= params_days) {

                            String textDistance = Constants.STRING_00 + Math.abs(eventDistance);
                            singleEventArray[Position_eventDate_sorted] = textDistance.substring(textDistance.length() - 3)
                                    + singleEventArray[Position_eventDate_sorted].substring(3);
                            singleEventArray[Position_eventDateNextTime] = Objects.requireNonNull(sdf_DDMMYYYY.get()).format(eventDate);
                            //todo: уменьшить год во всех датах Position_dates

                            listPrevEventsPreparatory.add(TextUtils.join(Constants.STRING_EOT, singleEventArray));
                            String eventContactTypeDate = singleEventArray[Position_contactID]
                                    .concat(Constants.STRING_2TILDA)
                                    .concat(singleEventArray[Position_eventSubType])
                                    .concat(Constants.STRING_2TILDA)
                                    .concat(singleEventArray[Position_eventDateNextTime]);
                            listPrevEventsDates.add(eventContactTypeDate);
                            statEventsPrevEventsFound++;
                        } else {
                            break;
                        }
                    }
                }
            }

            //Дополнительно заготовленные предыдущие события (5k, переходящие, календарные не ежегодные)
            for (String event : eventListPrev) {
                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);

                //Пропускаем дубли
                String eventContactTypeDate = singleEventArray[Position_contactID]
                        .concat(Constants.STRING_2TILDA)
                        .concat(singleEventArray[Position_eventSubType])
                        .concat(Constants.STRING_2TILDA)
                        .concat(singleEventArray[Position_eventDateNextTime]);

                if (listPrevEventsDates.contains(eventContactTypeDate)) continue;

                //Берём не скрытые
                if (isEventVisibleInList(singleEventArray)) listPrevEventsPreparatory.add(event);
            }

            Collections.sort(listPrevEventsPreparatory);

            //Окончательный отбор после сортировки
            List<String> listPrevEvents = new ArrayList<>();
            statEventsPrevEventsFound = 0;
            for (int i = 0; i < listPrevEventsPreparatory.size() && statEventsPrevEventsFound < params_events; i++) {
                String li = listPrevEventsPreparatory.get(i);
                String[] singleEventArray = li.split(Constants.STRING_EOT, -1);

                //Фильтр по источникам
                if (!preferences_list_EventSources.isEmpty()) {
                    final String eventDates = singleEventArray[Position_dates];
                    boolean isVisibleEvent = false;
                    for (String source : preferences_list_EventSources) {
                        if (eventDates.contains(source)) {
                            isVisibleEvent = true;
                            break;
                        }
                    }
                    if (!isVisibleEvent) continue;
                }

                Date eventDate = null;
                try {
                    eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                } catch (Exception e) { /**/ }

                if (eventDate != null) {
                    if (AppDateUtils.countDaysDiff(eventDate, currentDay) <= params_days) {
                        listPrevEvents.add(li);
                        statEventsPrevEventsFound++;
                    } else {
                        break;
                    }
                }
            }

            //Подправляем надписи и дату
            if (!listPrevEvents.isEmpty()) {

                for (String li : listPrevEvents) {
                    String[] singleEventArray = li.split(Constants.STRING_EOT, -1);
                    Date eventDate = null;
                    try {
                        eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                    } catch (Exception e) { /**/ }

                    if (eventDate != null) {

                        if (!singleEventArray[Position_eventSubType].equals(Constants.EventType_5K)) {
                            long dayDistance = AppDateUtils.countDaysDiff(currentDay, eventDate);
                            singleEventArray[Position_eventDistance] = Long.toString(dayDistance);
                            singleEventArray[Position_eventDistanceText] = getEventDistanceText(dayDistance, eventDate);
                        }

                        int Age = 0;
                        try {
                            Age = Integer.parseInt(singleEventArray[Position_age]);
                        } catch (NumberFormatException e) { /**/ }
                        if (Age > 1) {
                            Age--;
                            if (!singleEventArray[Position_eventSubType].equals(Constants.EventType_5K)) {
                                singleEventArray[Position_age] = Integer.toString(Age);
                                singleEventArray[Position_age_caption] = setAgeFormatting(StringUtils.getAgeString(Age, R.string.msg_after_year_prefix_1, R.string.msg_after_year_prefix_1_, R.string.msg_after_year_prefix_2_3_4, R.string.msg_after_year_prefix_5_20, currentLocale, resources));
                            }

                            if (singleEventArray[Position_eventType].equals(Constants.EventType_Anniversary)) {
                                @Nullable String anCaption = getWeddingName(Age);
                                String eventCaption = getResources().getString(R.string.event_type_anniversary);
                                if (StringUtils.hasContent(anCaption)) {
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

    int getPreviousDaysScanDays(@NonNull String params) {
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return result;
    }

    private boolean isEventVisibleInList(@NonNull String[] singleEventArray) {
        boolean result = false;
        try {

            //Фильтр по типам
            if (preferences_list_event_types.contains(singleEventArray[Position_eventType])) {

                String eventKey = getEventKey(singleEventArray);
                String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);

                //Фильтр по режиму отображения
                switch (preferences_list_events_scope) {
                    case Constants.pref_Events_Scope_NotHidden: //Показывать нескрытые
                        return !checkIsHiddenEvent(eventKey, eventKeyWithRawId);
                    case Constants.pref_Events_Scope_Hidden: //Показывать только скрытые
                        return checkIsHiddenEvent(eventKey, eventKeyWithRawId);
                    case Constants.pref_Events_Scope_Silenced: //Показывать только без уведомлений
                        return checkIsSilencedEvent(eventKey, eventKeyWithRawId);
                    case Constants.pref_Events_Scope_XDays: //Показывать только счётчики дней
                        return isXDaysEvent(eventKey)
                                && resources.getString(R.string.event_type_xdays_emoji).equals(singleEventArray[Position_eventEmoji]);
                    case Constants.pref_Events_Scope_Favorite: //Показывать только избранные
                        return checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[Position_starred]);
                    case Constants.pref_Events_Scope_All:
                        return true;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return result;
    }

    void updateWidgets(int widgetID, StringBuilder log) {

        //https://stackoverflow.com/questions/21300924/difference-between-executors-newfixedthreadpool1-and-executors-newsinglethread

        // Отменяем предыдущую задачу, если она еще выполняется
        if (pendingUpdateTask != null && !pendingUpdateTask.isDone()) {
            pendingUpdateTask.cancel(true); // true - позволяет прервать выполняющийся поток
            Log.d(TAG, "Предыдущая задача отменена");
        }

        if (context == null) return;

        try {
            // Создаем новую задачу
            Runnable updateTask = () -> {
                // Проверяем, была ли задача отменена перед началом работы
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "Задача была прервана до старта");
                    return;
                }

                ContactsEvents.getInstance().initLanguage(context);
                int[] ids;

                if (widgetID == 0) {
                    try {
                        // Используем Thread.sleep внутри задачи
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // Восстанавливаем флаг прерывания и выходим
                        Thread.currentThread().interrupt();
                        Log.d(TAG, "Сон задачи был прерван");
                        return;
                    }
                    // Проверяем снова после сна
                    if (Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "Задача была прервана после сна");
                        return;
                    }
                }

                statTimeUpdateWidgets = 0;
                statActiveWidgets = 0;

                ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget2x2.class));
                if (ids != null && ((widgetID > 0 && ids.length > 0 && StringUtils.contains(ids, widgetID)) || widgetID == 0)) {
                    //Toast.makeText(context, "Widget2x2:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                    Widget2x2 myWidget = new Widget2x2();
                    myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                }

                ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget5x1.class));
                if (ids != null && ((widgetID > 0 && ids.length > 0 && StringUtils.contains(ids, widgetID)) || widgetID == 0)) {
                    //Toast.makeText(context, "Widget5x1:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                    Widget5x1 myWidget = new Widget5x1();
                    myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                }

                ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, Widget4x1.class));
                if (ids != null && ((widgetID > 0 && ids.length > 0 && StringUtils.contains(ids, widgetID)) || widgetID == 0)) {
                    //Toast.makeText(context, "Widget4x1:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                    Widget4x1 myWidget = new Widget4x1();
                    myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                }

                ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetList.class));
                if (ids != null && ((widgetID > 0 && ids.length > 0 && StringUtils.contains(ids, widgetID)) || widgetID == 0)) {
                    //Toast.makeText(context, "WidgetList:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                    WidgetList myWidget = new WidgetList();
                    myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                }

                ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetPhotoList.class));
                if (ids != null && ((widgetID > 0 && ids.length > 0 && StringUtils.contains(ids, widgetID)) || widgetID == 0)) {
                    //Toast.makeText(context, "WidgetPhotoList:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                    WidgetPhotoList myWidget = new WidgetPhotoList();
                    myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                }

                ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetCalendar.class));
                if (ids != null && ((widgetID > 0 && ids.length > 0 && StringUtils.contains(ids, widgetID)) || widgetID == 0)) {
                    //Toast.makeText(context, "WidgetCalendar:" + Arrays.toString(ids), Toast.LENGTH_LONG).show();
                    WidgetCalendar myWidget = new WidgetCalendar();
                    myWidget.onUpdate(context, AppWidgetManager.getInstance(context), widgetID > 0 ? new int[]{widgetID} : ids);
                }

            };

            // Запускаем задачу и сохраняем Future
            pendingUpdateTask = widgetUpdateExecutor.submit(updateTask);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (log != null && statActiveWidgets > 0)
                log.append(context.getString(R.string.msg_sent_widgets_update_request)).append(Constants.STRING_EOL);
        }
    }

    void initNotificationChannel(StringBuilder log, int queueNumber, @NonNull Set<String> prefDays, String prefRingtone) {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //для Android 8+

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

                //Находим канал. Если канала нет или рингтон там другой - пересоздаём канал
                int prefChannelId = queueNumber == 1 ? preferences_notifications_channel_id : preferences_notifications2_channel_id;
                String channelId = Integer.toString(prefChannelId);
                @Nullable NotificationChannel channel = notificationManager.getNotificationChannel(channelId);

                if (!prefDays.isEmpty() && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                    //https://developer.android.com/training/notify-user/channels.html
                    //After you create a notification channel, you cannot change the notification behaviors—the user has complete control at that point. Though you can still change a channel's name and description
                    //https://stackoverflow.com/questions/46234254/android-oreo-notification-keep-making-sound-even-if-i-do-not-set-sound-on-older

                    if (channel != null && !channel.getSound().toString().equals(prefRingtone)) {
                        notificationManager.deleteNotificationChannel(channelId);
                        channel = null;
                        log.append(resources.getString(R.string.msg_deleted_channel, channelId));
                    }

                    if (channel == null) {
                        prefChannelId = generator.nextInt(1000);
                        if (queueNumber == 1) {
                            preferences_notifications_channel_id = prefChannelId;
                        } else if (queueNumber == 2) {
                            preferences_notifications2_channel_id = prefChannelId;
                        }
                        channelId = Integer.toString(prefChannelId);

                        channel = new NotificationChannel(channelId, context.getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription(context.getString(R.string.pref_Notifications_Notification_Channel_Description));
                        if (prefRingtone != null)
                            channel.setSound(
                                    Uri.parse(prefRingtone),
                                    new AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                            );
                        channel.enableVibration(true);
                        notificationManager.createNotificationChannel(channel);

                        log.append(resources.getString(R.string.msg_created_channel, channelId));
                        if (prefRingtone != null)
                            log
                                    .append(resources.getString(R.string.msg_ringtone))
                                    .append(Uri.parse(prefRingtone))
                                    .append(Constants.STRING_EOL);
                        savePreferences();
                    }

                } else if (channel != null) {
                    notificationManager.deleteNotificationChannel(channelId);
                    log.append(resources.getString(R.string.msg_deleted_channel, channelId));
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

            if ((!preferences_notifications_days.isEmpty() || !preferences_notifications2_days.isEmpty()) && NotificationManagerCompat.from(context).areNotificationsEnabled()) {

                //To enable Boot Receiver class
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    log.append(resources.getString(R.string.msg_notifications_were_enabled)).append(Constants.STRING_EOL);
                }

            } else { //Disable Daily Notifications
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //для Android 8+
                    NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                    List<NotificationChannel> listChannels = notificationManager.getNotificationChannels();
                    for (NotificationChannel channel : listChannels) {
                        String id = channel.getId();
                        notificationManager.deleteNotificationChannel(id);
                        log.append(resources.getString(R.string.msg_deleted_channel, id));
                    }
                }
                log.append(resources.getString(R.string.msg_notifications_were_disabled)).append(Constants.STRING_EOL);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Вспомогательный метод для установки будильника
     *
     * @param alarmManager  AlarmManager
     * @param triggerTime   Время срабатывания
     * @param pendingIntent Intent для запуска
     */
    @SuppressLint("MissingPermission")
    private void scheduleExactAlarm(AlarmManager alarmManager, long triggerTime, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                // Пользователь запретил точные будильники → используем неточный
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    void initWidgetUpdate(@NonNull StringBuilder log) {

        try {

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            final int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            int requestCode = 100;
            int requestCodeDaily = 200;

            Intent hourlyIntent = new Intent(context, WidgetUpdateReceiver.class);
            Intent dailyIntent = new Intent(context, WidgetUpdateDailyReceiver.class);

            if (preferences_widgets_update_period > 0) {

                //По часам
                long hourlyTrigger = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(preferences_widgets_update_period);
                PendingIntent hourlyPendingIntent = PendingIntent.getBroadcast(context, requestCode, hourlyIntent, pendingIntentFlags);
                scheduleExactAlarm(alarmManager, hourlyTrigger, hourlyPendingIntent);

                //В полночь
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 1);
                cal.set(Calendar.MILLISECOND, 0);
                long dailyTrigger = cal.getTimeInMillis();

                PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(context, requestCodeDaily, dailyIntent, pendingIntentFlags);
                scheduleExactAlarm(alarmManager, dailyTrigger, dailyPendingIntent);

                // Лог
                Date nextUpdate = new Date(Math.min(hourlyTrigger, dailyTrigger));
                log.append(resources.getString(R.string.msg_next_widgetupdate, Objects.requireNonNull(sdf_DDMMYYYYHHMM.get()).format(nextUpdate)));

            } else {
                // Отмена будильников
                PendingIntent pi1 = PendingIntent.getBroadcast(context, requestCode, hourlyIntent, pendingIntentFlags);
                alarmManager.cancel(pi1);

                PendingIntent pi2 = PendingIntent.getBroadcast(context, requestCodeDaily, dailyIntent, pendingIntentFlags);
                alarmManager.cancel(pi2);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    void initNotificationSchedule(@NonNull StringBuilder log, int queueNumber, @NonNull Set<String> prefDays, int prefAlarmHour, int prefAlarmMinute) {

        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            final int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            boolean needToNotify = !prefDays.isEmpty() && NotificationManagerCompat.from(context).areNotificationsEnabled();

            Intent intent = new Intent(context, AlarmReceiver.class);
            if (needToNotify) {
                intent.putExtra(Constants.QUEUE, queueNumber);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, queueNumber, intent, flags);

                // Вычисляем время следующего срабатывания
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, prefAlarmHour);
                cal.set(Calendar.MINUTE, prefAlarmMinute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                if (cal.before(Calendar.getInstance())) {
                    cal.add(Calendar.DATE, 1);
                }

                scheduleExactAlarm(alarmManager, cal.getTimeInMillis(), pendingIntent);

                Objects.requireNonNull(sdf_DDMMYYYYHHMM.get()).setTimeZone(cal.getTimeZone());
                log.append(resources.getString(R.string.msg_next_notification, Objects.requireNonNull(sdf_DDMMYYYYHHMM.get()).format(cal.getTime())));

            } else {
                // Отменяем будильник
                PendingIntent pi = PendingIntent.getBroadcast(context, queueNumber, intent, flags);
                alarmManager.cancel(pi);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (preferences_debug_on) {
                ToastExpander.showDebugMsg(context, getMethodName(3) + ": " + e);
            }
        }
    }

    private static class NotifyEvent {
        final String eventInfo;
        final String[] singleEventArray;
        final Date eventDate;

        public NotifyEvent(@NonNull String eventInfo, @NonNull String[] singleEventArray, @NonNull Date eventDate) {
            this.eventInfo = eventInfo;
            this.singleEventArray = singleEventArray;
            this.eventDate = eventDate;
        }

        String eventDay() {
            //todo: выводить в соответствии с настройками
            return Objects.requireNonNull(sdf_DDMM.get()).format(eventDate);
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

    void showNotifications(int queueNumber, boolean forceNoEventsMessage, String channelId) {

        if (queueNumber == 1) {
            showNotificationsForParams(
                    forceNoEventsMessage,
                    channelId,
                    preferences_notifications_days,
                    preferences_notifications_sources,
                    preferences_notifications_types,
                    preferences_notifications_style,
                    preferences_notifications_priority,
                    preferences_notifications_ringtone,
                    preferences_notifications_on_click_action,
                    preferences_notifications_quick_actions,
                    preferences_notifications_details,
                    preferences_notifications_smallicons_style,
                    preferences_notifications_types.contains(resources.getString(R.string.pref_EventTypes_Facts)) ? preferences_notifications_fact_event_count : 0
            );
        } else if (queueNumber == 2) {
            showNotificationsForParams(
                    forceNoEventsMessage,
                    channelId,
                    preferences_notifications2_days,
                    preferences_notifications2_sources,
                    preferences_notifications2_types,
                    preferences_notifications2_style,
                    preferences_notifications2_priority,
                    preferences_notifications2_ringtone,
                    preferences_notifications2_on_click_action,
                    preferences_notifications2_quick_actions,
                    preferences_notifications2_details,
                    preferences_notifications2_smallicons_style,
                    preferences_notifications2_types.contains(resources.getString(R.string.pref_EventTypes_Facts)) ? preferences_notifications2_fact_event_count : 0
            );
        }

    }

    void showNotificationsForParams(boolean forceNoEventsMessage, String channelId, Set<String> prefDays, Set<String> prefEventSources,
                                    Set<String> prefEventTypes, int prefType, int prefPriority,
                                    String prefRingtone, int prefOnClickAction, Set<String> prefQuickActions, Set<String> prefEventDetails,
                                    int prefSmallIconStyle, int randomFactsCount) {
        //https://startandroid.ru/ru/uroki/vse-uroki-spiskom/511-urok-186-notifications-rasshirennye-uvedomlenija.html

        if (DeviceTools.checkNoNotificationAccess(context)) return;

        try {

            Set<String> notifications_days = new HashSet<>(prefDays); //За сколько дней уведомлять
            if (notifications_days.isEmpty()) return;

            Date currentDay = getToday().getTime();

            List<NotifyEvent> listNotify = new ArrayList<>();
            for (String event : eventList) {
                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                if (singleEventArray.length == Position_attrAmount) {

                    final String eventKey = getEventKey(singleEventArray);
                    final String eventKeyWithRawId = getEventKeyWithRawId(singleEventArray);

                    //Фильтр по источникам
                    if (!prefEventSources.isEmpty()) {
                        final String eventDates = singleEventArray[Position_dates];
                        boolean isVisibleEvent = false;
                        for (String source : prefEventSources) {
                            if (eventDates.contains(source)) {
                                isVisibleEvent = true;
                                break;
                            }
                        }
                        if (!isVisibleEvent) continue;
                    }

                    //Фильтр по типам событий
                    if (prefEventTypes.contains(singleEventArray[Position_eventType])
                            && (getHiddenEventsCount() == 0 || !checkIsHiddenEvent(eventKey, eventKeyWithRawId))
                            && (getSilencedEventsCount() == 0 || !checkIsSilencedEvent(eventKey, eventKeyWithRawId))) {

                        Date eventDate = null;
                        try {
                            eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            if (listNotify.size() >= 50)
                                break; //https://stackoverflow.com/questions/33364368/android-system-notification-limit-per-app

                            long countDays = AppDateUtils.countDaysDiff(currentDay, eventDate);
                            if (countDays > 14) {
                                break;
                            } else if (notifications_days.contains(String.valueOf(countDays))) {
                                listNotify.add(new NotifyEvent(event, singleEventArray, eventDate));
                            }
                        }
                    }
                }
            }
            List<String> listFacts = new ArrayList<>();
            if (randomFactsCount != 0) {
                listFacts = getNextRandomFacts(randomFactsCount, prefEventSources);
            }
            if (listNotify.isEmpty() && !forceNoEventsMessage && listFacts.isEmpty()) return;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            //notificationManager.cancelAll();

            if (listNotify.isEmpty() || //Тестовое уведомление
                    prefType == 0 || //Одно общее уведомление
                    prefType == 2 && listNotify.size() >= 3 || //Одно общее уведомление (событий >= 3)
                    prefType == 3 && listNotify.size() >= 4 || //Одно общее уведомление (событий >= 4)
                    prefType == 4 //За сегодня -> отдельные, остальные -> общее
            ) {

                final ArrayList<String> eventsList = new ArrayList<>();
                StringBuilder textBig = new StringBuilder();
                String textSmall = null;
                int countEvents = 0;
                if (!listFacts.isEmpty()) {
                    String factsDetails = composeFactsAsString(listFacts);
                    textBig.append(factsDetails);
                    eventsList.addAll(Arrays.asList(factsDetails.split(Constants.STRING_EOL)));
                }
                boolean noEventsMsg = false;
                final Map<String, Integer> mostEventIcons = new HashMap<>();
                if (!listNotify.isEmpty()) {
                    for (NotifyEvent event : listNotify) {
                        if (prefType != 4 || event.eventDate.after(currentDay)) {
                            countEvents++;
                            final String eventDetails = composeNotifyEventDetails(event, prefEventDetails);
                            final String eventIcon = event.singleEventArray[Position_eventIcon];

                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            textBig.append(eventDetails);
                            eventsList.add(eventDetails);

                            if (eventIcon != null) {
                                Integer currentCount = mostEventIcons.get(eventIcon);
                                if (currentCount != null) {
                                    mostEventIcons.put(eventIcon, currentCount + 1);
                                } else {
                                    mostEventIcons.put(eventIcon, 1);
                                }
                            }
                        }
                    }

                    if (countEvents > 0) {
                        if (prefType == 4) {
                            textSmall = context.getString(R.string.msg_notifications_soon) + countEvents;
                        } else {
                            textSmall = context.getString(R.string.msg_notifications_all) + countEvents;
                        }
                        textBig.insert(0, textSmall + ":\n");
                    }
                } else if (prefType != 4) {
                    if (listFacts.isEmpty()) {
                        textSmall = context.getString(R.string.msg_notifications_soon_no_events);
                        noEventsMsg = true;
                    } else {
                        textSmall = context.getString(R.string.pref_CustomEvents_Fact_title);
                    }
                }

                if (textSmall != null) {
                    //Запуск основной активности
                    //Intent intent = new Intent(context, MainActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);

                    //Запуск диалога со списком событий
                    Intent intent = new Intent(context, EventsPopupActivity.class);
                    intent.putStringArrayListExtra(Constants.EXTRA_LIST, eventsList);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                    final String notificationDetails = textBig.toString().concat(Constants.STRING_EOL).concat(textSmall);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                            .setContentText(textSmall)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(textBig)) //Ограничение 5120 символов https://stackoverflow.com/questions/27124887/whats-the-max-size-of-a-bigtextstyle-notification
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setWhen(0) //https://stackoverflow.com/questions/18249871/android-notification-buttons-not-showing-up/18603076#18603076
                            .setAutoCancel(true);

                    @ColorInt int eventIcon = R.drawable.ic_icon_notify;
                    if (prefSmallIconStyle == 1) {
                        builder.setColor(this.getResources().getColor(R.color.dark_green));
                    } else if (prefSmallIconStyle == 2) {
                        builder.setColor(getThemeBackColor());
                    } else {
                        builder.setColor(getThemeBackColor());
                        try {
                            String mostIcon = null;
                            int mostIconCount = 0;
                            for (Map.Entry<String, Integer> entry : mostEventIcons.entrySet()) {
                                if (entry.getValue() > mostIconCount) {
                                    mostIconCount = entry.getValue();
                                    mostIcon = entry.getKey();
                                }
                            }
                            if (mostIcon != null) {
                                eventIcon = Integer.parseInt(mostIcon);
                            }
                        } catch (NumberFormatException ignored) { /**/ }
                        mostEventIcons.clear();
                    }
                    builder.setSmallIcon(eventIcon);

                    if (preferences_debug_on) {
                        builder.setSubText(Constants.NOTIFY_ID + notificationID);
                    }

                    if (countEvents > 1) {
                        builder.setNumber(countEvents);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        builder.setTimeoutAfter(85800000); //Сутки без 10 мин
                    }

                    if (prefPriority > 1 && !listNotify.isEmpty()) {
                        builder.setOngoing(true);
                        builder.setPriority(NotificationCompat.PRIORITY_MAX);
                    }

                    if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Close))) {
                        Intent intentClose = new Intent(context, NotifyActionReceiver.class);
                        intentClose.setAction(Constants.ACTION_CLOSE);
                        intentClose.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                        intentClose.putExtra(Constants.EXTRA_NOTIFICATION_DATA, notificationDetails);
                        PendingIntent pendingClose = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentClose, PendingIntentImmutable);
                        NotificationCompat.Action actionClose = new NotificationCompat.Action(0, context.getString(R.string.button_close), pendingClose);
                        builder.addAction(actionClose);
                    }

                    if (!noEventsMsg && prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Share))) {
                        Intent intentShare = new Intent(context, NotifyActionReceiver.class);
                        intentShare.setAction(Constants.ACTION_SHARE);
                        intentShare.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);

                        intentShare.putExtra(Constants.EXTRA_NOTIFICATION_DATA, notificationDetails);
                        PendingIntent pendingShare = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentShare, PendingIntentImmutable);
                        NotificationCompat.Action actionShare = new NotificationCompat.Action(0, context.getString(R.string.button_share), pendingShare);
                        builder.addAction(actionShare);
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        if (prefRingtone != null)
                            builder.setSound(Uri.parse(prefRingtone));
                    }

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    notificationManager.notify(notificationID, builder.build());
                }
            }

            if (!listNotify.isEmpty() && (prefType == 1 || //Несколько отдельных уведомлений
                    listNotify.size() < 3 && prefType == 2 || //Если событий меньше 3 -> отдельные
                    listNotify.size() < 4 && prefType == 3 || //Если событий меньше 4 -> отдельные
                    prefType == 4 //За сегодня -> отдельные
            )) {

                for (int i = listNotify.size() - 1; i >= 0; i--) {

                    NotifyEvent event = listNotify.get(i);

                    if (prefType != 4 || event.eventDate.equals(currentDay)) {

                        int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                        final String[] eventDistance = event.singleEventArray[Position_eventDistanceText].split(Constants.REGEX_BAR, -1);
                        final String eventDetails = composeNotifyEventDetails(event, prefEventDetails);
                        final String eventTitle = event.singleEventArray[Position_eventDistance].equals(Constants.STRING_0) ? eventDistance[0] : eventDistance[0] + Constants.STRING_SPACE + eventDistance[1];

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                .setContentText(eventDetails)
                                .setContentTitle(eventTitle)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(eventDetails))
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setAutoCancel(true);

                        @ColorInt int eventIcon = R.drawable.ic_icon_notify;
                        if (prefSmallIconStyle == 1) {
                            builder.setColor(this.getResources().getColor(R.color.dark_green));
                        } else if (prefSmallIconStyle == 2) {
                            builder.setColor(getThemeBackColor());
                        } else {
                            builder.setColor(getThemeBackColor());
                            try {
                                eventIcon = Integer.parseInt(event.singleEventArray[Position_eventIcon]);
                            } catch (NumberFormatException ignored) { /**/ }
                        }
                        builder.setSmallIcon(eventIcon);

                        if (prefPriority > 2) {
                            builder.setOngoing(true);
                            builder.setPriority(NotificationCompat.PRIORITY_MAX);
                        }

                        if (preferences_debug_on) {
                            builder.setSubText(Constants.NOTIFY_ID + notificationID);
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            builder.setTimeoutAfter(85800000); //Сутки без 10 мин
                        }

                        Intent intent = null;

                        if (prefOnClickAction == 7) { //Основной список событий

                            intent = new Intent(context, MainActivity.class);
                            intent.setAction(Constants.ACTION_LAUNCH);

                        } else if (prefOnClickAction >= 1 & prefOnClickAction <= 4) {

                            intent = getViewActionIntent(
                                    event.eventInfo,
                                    eventTitle,
                                    event.singleEventArray,
                                    prefOnClickAction,
                                    context);

                        } else if (prefOnClickAction == 6) { //Закрыть уведомление
                            intent = new Intent();
                        }

                        if (intent != null) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                            builder.setContentIntent(pendingIntent);
                        }

                        final String eventAsString = event.toString();

                        if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Dial))
                                && !event.singleEventArray[Position_eventSubType].equals(Constants.EventType_Calendar)
                                && !TextUtils.isEmpty(event.singleEventArray[Position_contactID])
                                && !TextUtils.isEmpty(getContactPhone(StringUtils.parseToLong(event.singleEventArray[Position_contactID])))) {

                            Intent intentDial = new Intent(context, NotifyActionReceiver.class);
                            intentDial.setAction(Constants.ACTION_DIAL);
                            intentDial.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentDial.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingDial = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentDial, PendingIntentImmutable);
                            NotificationCompat.Action actionDial = new NotificationCompat.Action(0, context.getString(R.string.button_dial), pendingDial);
                            builder.addAction(actionDial);

                        }

                        final String eventKey = getEventKey(event.singleEventArray);
                        if (!eventKey.isEmpty() && prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Silent))) {
                            Intent intentSilent = new Intent(context, NotifyActionReceiver.class);
                            intentSilent.setAction(Constants.ACTION_SILENT);
                            intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingSilent = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSilent, PendingIntentImmutable);
                            NotificationCompat.Action actionSilent = new NotificationCompat.Action(0, context.getString(R.string.button_silent), pendingSilent);
                            builder.addAction(actionSilent);
                        }

                        if (!eventKey.isEmpty() && prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Hide))) {
                            Intent intentHide = new Intent(context, NotifyActionReceiver.class);
                            intentHide.setAction(Constants.ACTION_HIDE);
                            intentHide.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentHide.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingHide = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentHide, PendingIntentImmutable);
                            NotificationCompat.Action actionHide = new NotificationCompat.Action(0, context.getString(R.string.button_hide), pendingHide);
                            builder.addAction(actionHide);
                        }

                        if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Remind))) {
                            Intent intentSnooze = new Intent(context, NotifyActionReceiver.class);
                            intentSnooze.setAction(Constants.ACTION_SNOOZE);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DETAILS, prefEventDetails.toArray(new String[0]));
                            intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ACTIONS, prefQuickActions.toArray(new String[0]));
                            PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSnooze, PendingIntentImmutable);
                            NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                            builder.addAction(actionSnooze);
                        }

                        if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Share))) {
                            Intent intentShare = new Intent(context, NotifyActionReceiver.class);
                            intentShare.setAction(Constants.ACTION_SHARE);
                            intentShare.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentShare.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventTitle.concat(Constants.STRING_EOL).concat(eventDetails));
                            PendingIntent pendingShare = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentShare, PendingIntentImmutable);
                            NotificationCompat.Action actionShare = new NotificationCompat.Action(0, context.getString(R.string.button_share), pendingShare);
                            builder.addAction(actionShare);
                        }

                        if (prefPriority <= 2 && prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Attach))) {
                            Intent intentAttach = new Intent(context, NotifyActionReceiver.class);
                            intentAttach.setAction(Constants.ACTION_ATTACH);
                            intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_DETAILS, prefEventDetails.toArray(new String[0]));
                            intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_ACTIONS, prefQuickActions.toArray(new String[0]));
                            PendingIntent pendingAttach = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentAttach, PendingIntentImmutable);
                            NotificationCompat.Action actionAttach = new NotificationCompat.Action(0, context.getString(R.string.button_attach), pendingAttach);
                            builder.addAction(actionAttach);
                        }

                        if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Close))) {
                            Intent intentClose = new Intent(context, NotifyActionReceiver.class);
                            intentClose.setAction(Constants.ACTION_CLOSE);
                            intentClose.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                            intentClose.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventAsString);
                            PendingIntent pendingClose = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentClose, PendingIntentImmutable);
                            NotificationCompat.Action actionClose = new NotificationCompat.Action(0, context.getString(R.string.button_close), pendingClose);
                            builder.addAction(actionClose);
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            if (prefRingtone != null)
                                builder.setSound(Uri.parse(prefRingtone));
                        }

                        String eventSubType = event.singleEventArray[Position_eventSubType];
                        int roundingFactor;
                        if (eventSubType.equals(Constants.EventType_Calendar) || eventSubType.equals(Constants.EventType_File)) {
                            roundingFactor = 1;
                        } else {
                            roundingFactor = preferences_list_photostyle;
                        }
                        builder.setLargeIcon(getEventPhoto(eventAsString, true, true, true, roundingFactor));

                        notificationManager.notify(notificationID, builder.build());
                    }
                }

                //Только факты
                if (prefType != 4 && !listFacts.isEmpty()) {
                    int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
                    final String factsDetails = composeFactsAsString(listFacts);
                    ArrayList<String> eventsList = new ArrayList<>(Arrays.asList(factsDetails.split(Constants.STRING_EOL)));

                    //Запуск диалога со списком событий
                    Intent intent = new Intent(context, EventsPopupActivity.class);
                    intent.putStringArrayListExtra(Constants.EXTRA_LIST, eventsList);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                            .setColor(getThemeBackColor())
                            .setSmallIcon(R.drawable.ic_event_fact)
                            .setContentText(factsDetails)
                            .setContentTitle(context.getString(R.string.pref_CustomEvents_Fact_title))
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(factsDetails))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    if (preferences_debug_on) {
                        builder.setSubText(Constants.NOTIFY_ID + notificationID);
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        if (prefRingtone != null)
                            builder.setSound(Uri.parse(prefRingtone));
                    }

                    notificationManager.notify(notificationID, builder.build());
                }

            }
            listNotify.clear();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @NonNull
    List<String> getNextRandomFacts(int randomFactsCount, @NonNull Set<String> eventSources) {
        List<String> listSelectedFacts = new ArrayList<>();
        try {

            List<String> listAllFacts = new ArrayList<>(eventListFacts);
            if (!eventSources.isEmpty()) { //Фильтрация по источникам
                List<String> listFactsToRemove = new ArrayList<>();
                for (String factToFilter : listAllFacts) {
                    String[] fact = factToFilter.split(Constants.STRING_EOT, -1);
                    if (fact.length < 2 || !eventSources.contains(fact[1])) {
                        listFactsToRemove.add(factToFilter);
                    }
                }
                if (!listFactsToRemove.isEmpty()) {
                    listAllFacts.removeAll(listFactsToRemove);
                }
            }

            int tryFact = 0;
            while (tryFact < Math.min(randomFactsCount, listAllFacts.size())) {
                String[] fact = listAllFacts.get(generator.nextInt(listAllFacts.size())).split(Constants.STRING_EOT, -1);
                if (!listSelectedFacts.contains(fact[0])) {
                    tryFact++;
                    listSelectedFacts.add(fact[0]);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return listSelectedFacts;
    }

    @NonNull
    private String composeNotifyEventDetails(NotifyEvent event, Set<String> prefEventInfo) {

        StringBuilder eventDetails = new StringBuilder();
        try {

            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_EventIcon_ID))) {
                eventDetails.append(event.singleEventArray[Position_eventEmoji]);
            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_SourceIcon_ID))) {
                eventDetails.append(getEventSourceIcon(event.singleEventArray));
            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_FavIcon_ID))) {
                if (checkIsFavoriteEvent(getEventKey(event.singleEventArray), getEventKeyWithRawId(event.singleEventArray), event.singleEventArray[Position_starred])) {
                    eventDetails.append(StringUtils.substringBefore(resources.getString(R.string.pref_EventInfo_FavIcon), Constants.STRING_SPACE));
                }
            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_ZodiacSign_ID))) {
                eventDetails.append(StringUtils.substringBefore(event.singleEventArray[Position_zodiacSign], Constants.STRING_SPACE));
            }
            if (eventDetails.length() > 0) eventDetails.append(Constants.STRING_SPACE);
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_EventDate_ID))) {
                eventDetails.append(event.eventDay());
            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_EventTitle_ID))) {
                if (!eventDetails.toString().endsWith(Constants.STRING_SPACE))
                    eventDetails.append(Constants.STRING_SPACE);
                eventDetails.append(StringUtils.getFullName(event.singleEventArray, preferences_name_format));
            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_Age_ID))
                    && !TextUtils.isEmpty(event.singleEventArray[Position_age_caption].trim())) {
                if (!eventDetails.toString().endsWith(Constants.STRING_SPACE))
                    eventDetails.append(Constants.STRING_COLON_SPACE);
                eventDetails.append(event.singleEventArray[Position_age_caption]);
            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_WeddingName_ID))
                    && event.singleEventArray[Position_eventSubType].equals(Constants.EventType_Anniversary)) {

                if (!eventDetails.toString().endsWith(Constants.STRING_SPACE))
                    eventDetails.append(Constants.STRING_SPACE);

                if (event.singleEventArray[Position_eventCaption].endsWith(Constants.STRING_PARENTHESIS_CLOSE)) {
                    if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_EventCaption_ID))) {
                        eventDetails
                                .append(Constants.STRING_COMMA_SPACE)
                                .append(event.singleEventArray[Position_eventCaption]);
                    } else {
                        eventDetails
                                .append(Constants.STRING_PARENTHESIS_OPEN)
                                .append(StringUtils.substringAfter(event.singleEventArray[Position_eventCaption], Constants.STRING_PARENTHESIS_OPEN));
                    }
                }

            }
            if (prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_EventCaption_ID))
                    && !event.singleEventArray[Position_eventSubType].equals(Constants.EventType_Anniversary)) {
                eventDetails
                        .append(Constants.STRING_COMMA_SPACE)
                        .append(!StringUtils.hasContent(event.singleEventArray[Position_eventLabel]) ? event.singleEventArray[Position_eventCaption] :
                                event.singleEventArray[Position_eventLabel]);
            }
            final boolean addTitle = prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_JobTitle_ID))
                    && !TextUtils.isEmpty(event.singleEventArray[Position_title]);
            final boolean addOrganization = prefEventInfo.contains(resources.getString(R.string.pref_EventInfo_Organization_ID))
                    && !TextUtils.isEmpty(event.singleEventArray[Position_organization]);
            if (addOrganization || addTitle) {
                if (!eventDetails.toString().endsWith(Constants.STRING_SPACE))
                    eventDetails.append(Constants.STRING_SPACE);
                eventDetails.append(Constants.STRING_PARENTHESIS_START);
                if (addTitle) eventDetails.append(event.singleEventArray[Position_title]);
                if (addTitle && addOrganization) eventDetails.append(Constants.STRING_COMMA_SPACE);
                if (addOrganization)
                    eventDetails.append(event.singleEventArray[Position_organization]);
                eventDetails.append(Constants.STRING_PARENTHESIS_CLOSE);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return eventDetails.toString();
    }

    /**
     * Возвращает список фактов как единую строку с иконками и переводом строк
     *
     * @param listFacts Список фактов
     * @return Единая строка с фактами
     */
    @NonNull
    private String composeFactsAsString(@NonNull List<String> listFacts) {
        StringBuilder eventDetails = new StringBuilder();
        try {

            for (String fact : listFacts) {
                if (eventDetails.length() > 0) {
                    eventDetails.append(Constants.STRING_EOL);
                }
                eventDetails.append(resources.getString(R.string.event_type_fact_emoji));
                eventDetails.append(Constants.STRING_SPACE);
                eventDetails.append(fact);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return eventDetails.toString();
    }

    @NonNull
    String getEventSourceIcon(String[] singleEventArray) {

        List<String> icons = new ArrayList<>();
        try {

            if (singleEventArray != null && singleEventArray.length > Position_dates) {
                String[] dates = singleEventArray[Position_dates].split(Constants.STRING_2TILDA, -1);
                for (String date : dates) {
                    if (date.startsWith(Constants.EVENT_PREFIX_CALENDAR_EVENT)) {
                        icons.add(resources.getString(R.string.event_source_calendar));
                    } else if (date.startsWith(Constants.EVENT_PREFIX_FILE_EVENT)) {
                        icons.add(resources.getString(R.string.event_source_file));
                    } else if (date.startsWith(Constants.EVENT_PREFIX_LOCAL_EVENT)) {
                        icons.add(resources.getString(R.string.event_source_local));
                    } else if (date.startsWith(Constants.EVENT_PREFIX_EMBEDDED_EVENT)) {

                        String icon = Constants.STRING_EMPTY;
                        try {
                            String eventSource = singleEventArray[Position_eventSource];
                            String prefix = resources.getString(R.string.msg_source_info);
                            prefix = prefix.substring(0, prefix.indexOf("%1$s"));
                            eventSource = eventSource.substring(prefix.length());
                            icon = eventSource.substring(0, eventSource.indexOf(Constants.STRING_SPACE));
                        } catch (IndexOutOfBoundsException e) { /**/ }

                        if (icon.length() == 4) {
                            icons.add(icon);
                        } else {
                            icons.add(resources.getString(R.string.event_source_internal));
                        }
                    } else {
                        icons.add(resources.getString(R.string.event_source_contact));
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return TextUtils.join(Constants.STRING_EMPTY, new HashSet<>(icons));
    }

    String getEventKey(@NonNull String[] singleEventArray) {
        String eventSubType = singleEventArray[Position_eventSubType];
        if (!StringUtils.hasContent(eventSubType)) {
            return Constants.STRING_EMPTY;
        }

        String contactId = singleEventArray[Position_contactID];
        if (StringUtils.hasContent(contactId)) {
            return contactId + Constants.STRING_2HASH + eventSubType;
        }

        String eventId = singleEventArray[Position_eventID];
        if (StringUtils.hasContent(eventId)) {
            return eventId + Constants.STRING_2HASH + eventSubType;
        }

        return Constants.STRING_EMPTY;
    }

    String getEventKeyWithRawId(@NonNull String[] singleEventArray) {
        String eventSubType = singleEventArray[Position_eventSubType];
        if (!StringUtils.hasContent(eventSubType)) {
            return Constants.STRING_EMPTY;
        }

        String rawContactId = singleEventArray[Position_rawContactID];
        if (StringUtils.hasContent(rawContactId)) {
            return rawContactId + Constants.STRING_2HASH + eventSubType;
        }

        return Constants.STRING_EMPTY;
    }

    void snoozeNotification(@NonNull String notifyData, String[] notifyDetails, String[] notifyActions, int snoozeHours, Date wakeDateTime) {

        try {

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;
            if (TextUtils.isEmpty(notifyData) || (snoozeHours <= 0 && wakeDateTime == null)) return;

            Intent alarmIntent = new Intent(context, NotifyActionReceiver.class);
            alarmIntent.setAction(Constants.ACTION_NOTIFY);
            alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_DATA, notifyData);
            if (notifyDetails != null) {
                alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_DETAILS, notifyDetails);
            } else {
                alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_DETAILS, preferences_notifications_details.toArray(new String[0])); //Берём из основных
            }
            if (notifyActions != null) {
                alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_ACTIONS, notifyActions);
            } else {
                alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_ACTIONS, preferences_notifications_quick_actions.toArray(new String[0])); //Берём из основных
            }

            final int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            int requestCode = (Math.abs(notifyData.hashCode()) % 9000) + 1000; // диапазон 1000–9999
            alarmIntent.putExtra(Constants.EXTRA_NOTIFICATION_ID, requestCode);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, alarmIntent, flags);

            boolean isSnoozed;
            long triggerTime;
            if (snoozeHours > 0) {
                triggerTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(snoozeHours);
                isSnoozed = true;
            } else {
                isSnoozed = false;
                triggerTime = wakeDateTime.getTime();
            }

            scheduleExactAlarm(alarmManager, triggerTime, pendingIntent);

            //handler.post(() -> Toast.makeText(context, context.getNotNullString(isSnoozed ? R.string.msg_snoozed_until : R.string.msg_notify_time, sdf_DDMMYYYYHHMM.get().format(triggerTime)), Toast.LENGTH_LONG).show());
            Objects.requireNonNull(sdf_DDMMYYYYHHMM.get()).setTimeZone(getToday().getTimeZone());
            ToastExpander.showInfoMsg(context, context.getString(isSnoozed ? R.string.msg_snoozed_until : R.string.msg_notify_time, Objects.requireNonNull(sdf_DDMMYYYYHHMM.get()).format(triggerTime)));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @SuppressLint("MissingPermission")
    void showNotification(String dataNotify, String[] actions, String[] details, String channelId, boolean setOnGoing) {

        try {

            if (dataNotify == null || dataNotify.isEmpty()) return;

            String[] singleEventArray = dataNotify.split(Constants.STRING_EOT, -1);
            Date eventDate = null;
            try {
                eventDate = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(singleEventArray[Position_eventDateNextTime]);
            } catch (Exception e) { /**/ }
            if (eventDate == null) return;

            final String eventDetails = composeNotifyEventDetails(new NotifyEvent(dataNotify, singleEventArray, eventDate), new HashSet<>(Arrays.asList(details)));
            int notificationID = Constants.defaultNotificationID + generator.nextInt(100);
            final String[] eventDistance = singleEventArray[Position_eventDistanceText].split(Constants.REGEX_BAR, -1);
            Set<String> prefEventDetails = preferences_notifications_details;
            final String eventTitle = singleEventArray[Position_eventDistance].equals(Constants.STRING_0) ? eventDistance[0] : eventDistance[0] + Constants.STRING_SPACE + eventDistance[1];

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setContentText(eventDetails)
                    .setContentTitle(eventDistance[0] + Constants.STRING_SPACE + eventDistance[1])
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(eventDetails))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            int prefSmallIconStyle = preferences_notifications_smallicons_style;
            @ColorInt int eventIcon = R.drawable.ic_icon_notify;
            if (prefSmallIconStyle == 1) {
                builder.setColor(this.getResources().getColor(R.color.dark_green));
            } else if (prefSmallIconStyle == 2) {
                builder.setColor(getThemeBackColor());
            } else {
                builder.setColor(getThemeBackColor());
                try {
                    eventIcon = Integer.parseInt(singleEventArray[Position_eventIcon]);
                } catch (NumberFormatException ignored) { /**/ }
            }
            builder.setSmallIcon(eventIcon);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setTimeoutAfter(85800000); //Сутки без 10 мин
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (preferences_notifications_ringtone != null)
                    builder.setSound(Uri.parse(preferences_notifications_ringtone));
            }

            String eventSubType = singleEventArray[Position_eventSubType];
            int roundingFactor;
            if (eventSubType.equals(Constants.EventType_Calendar) || eventSubType.equals(Constants.EventType_File)) {
                roundingFactor = 1;
            } else {
                roundingFactor = preferences_list_photostyle;
            }
            builder.setLargeIcon(getEventPhoto(dataNotify, true, true, true, roundingFactor));

            Set<String> prefQuickActions = actions == null ? new HashSet<>() : new HashSet<>(Arrays.asList(actions));
            Intent intent = null;

            if (preferences_notifications_on_click_action == 7) { //Основной список событий
                intent = new Intent(context, MainActivity.class);
                intent.setAction(Constants.ACTION_LAUNCH);
            } else if (preferences_notifications_on_click_action >= 1 & preferences_notifications_on_click_action <= 4) {
                intent = getViewActionIntent(dataNotify, eventTitle, singleEventArray, preferences_notifications_on_click_action, context);
            } else if (preferences_notifications_on_click_action == 6) { //Закрыть уведомление
                intent = new Intent();
            }

            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable);
                builder.setContentIntent(pendingIntent);
            }

            if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Dial))
                    && !singleEventArray[Position_eventSubType].equals(Constants.EventType_Calendar)
                    && !TextUtils.isEmpty(singleEventArray[Position_contactID])
                    && !TextUtils.isEmpty(getContactPhone(StringUtils.parseToLong(singleEventArray[Position_contactID])))) {

                Intent intentDial = new Intent(context, NotifyActionReceiver.class);
                intentDial.setAction(Constants.ACTION_DIAL);
                intentDial.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentDial.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                PendingIntent pendingDial = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentDial, PendingIntentImmutable);
                NotificationCompat.Action actionDial = new NotificationCompat.Action(0, context.getString(R.string.button_dial), pendingDial);
                builder.addAction(actionDial);

            }

            final String eventKey = getEventKey(singleEventArray);
            if (!eventKey.isEmpty() && prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Silent))) {
                Intent intentSilent = new Intent(context, NotifyActionReceiver.class);
                intentSilent.setAction(Constants.ACTION_SILENT);
                intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentSilent.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                PendingIntent pendingSilent = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSilent, PendingIntentImmutable);
                NotificationCompat.Action actionSilent = new NotificationCompat.Action(0, context.getString(R.string.button_silent), pendingSilent);
                builder.addAction(actionSilent);
            }

            if (!eventKey.isEmpty() && prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Hide))) {
                Intent intentHide = new Intent(context, NotifyActionReceiver.class);
                intentHide.setAction(Constants.ACTION_HIDE);
                intentHide.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentHide.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                PendingIntent pendingHide = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentHide, PendingIntentImmutable);
                NotificationCompat.Action actionHide = new NotificationCompat.Action(0, context.getString(R.string.button_hide), pendingHide);
                builder.addAction(actionHide);
            }

            if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Remind))) {
                Intent intentSnooze = new Intent(context, NotifyActionReceiver.class);
                intentSnooze.setAction(Constants.ACTION_SNOOZE);
                intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_DETAILS, prefEventDetails.toArray(new String[0]));
                intentSnooze.putExtra(Constants.EXTRA_NOTIFICATION_ACTIONS, prefQuickActions.toArray(new String[0]));
                PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentSnooze, PendingIntentImmutable);
                NotificationCompat.Action actionSnooze = new NotificationCompat.Action(0, context.getString(R.string.button_snooze), pendingSnooze);
                builder.addAction(actionSnooze);
            }

            if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Share))) {
                Intent intentShare = new Intent(context, NotifyActionReceiver.class);
                intentShare.setAction(Constants.ACTION_SHARE);
                intentShare.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentShare.putExtra(Constants.EXTRA_NOTIFICATION_DATA, eventTitle.concat(Constants.STRING_EOL).concat(eventDetails));
                PendingIntent pendingShare = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentShare, PendingIntentImmutable);
                NotificationCompat.Action actionShare = new NotificationCompat.Action(0, context.getString(R.string.button_share), pendingShare);
                builder.addAction(actionShare);
            }

            if (prefQuickActions.contains(context.getString(R.string.pref_Notifications_QuickActions_Attach))) {
                Intent intentAttach = new Intent(context, NotifyActionReceiver.class);
                intentAttach.setAction(Constants.ACTION_ATTACH);
                intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_DETAILS, prefEventDetails.toArray(new String[0]));
                intentAttach.putExtra(Constants.EXTRA_NOTIFICATION_ACTIONS, prefQuickActions.toArray(new String[0]));
                PendingIntent pendingAttach = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentAttach, PendingIntentImmutable);
                NotificationCompat.Action actionAttach = new NotificationCompat.Action(0, context.getString(R.string.button_attach), pendingAttach);
                builder.addAction(actionAttach);
            }

            if (setOnGoing) {
                if (actions != null && Arrays.asList(actions).contains(context.getString(R.string.pref_Notifications_QuickActions_Close))) {
                    Intent intentClose = new Intent(context, NotifyActionReceiver.class);
                    intentClose.setAction(Constants.ACTION_CLOSE);
                    intentClose.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationID);
                    intentClose.putExtra(Constants.EXTRA_NOTIFICATION_DATA, dataNotify);
                    PendingIntent pendingClose = PendingIntent.getBroadcast(context, Constants.defaultNotificationID + generator.nextInt(100), intentClose, PendingIntentImmutable);
                    NotificationCompat.Action actionClose = new NotificationCompat.Action(0, context.getString(R.string.button_close), pendingClose);
                    builder.addAction(actionClose);
                }
                builder.setSilent(true);
                builder.setAutoCancel(false);
                builder.setOngoing(true);
                builder.setPriority(NotificationCompat.PRIORITY_MAX);
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (!DeviceTools.checkNoNotificationAccess(context)) {
                notificationManager.notify(notificationID, builder.build());
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
        return preferences_hiddenEvents.size();
    }

    boolean checkIsHiddenEvent(@NonNull String key, String keyWithRawId) {

        try {

            return (!key.isEmpty() && preferences_hiddenEvents.contains(key))
                    || (!TextUtils.isEmpty(keyWithRawId) && preferences_hiddenEventsRawIds.contains(keyWithRawId));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setHiddenEvent(@NonNull String key, String keyWithRawId) {

        try {

            SharedPreferences.Editor editor = null;

            if (!key.isEmpty()) {
                preferences_hiddenEvents.add(key);
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Hidden_key), preferences_hiddenEvents);
            }

            if (!TextUtils.isEmpty(keyWithRawId)) {
                preferences_hiddenEventsRawIds.add(keyWithRawId);
                if (editor == null)
                    editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
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

            if (!checkIsHiddenEvent(key, keyWithRawId)) return false;

            boolean idremoved = preferences_hiddenEvents.remove(key);

            if (!TextUtils.isEmpty(keyWithRawId))
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
        return preferences_silentEvents.size();
    }

    boolean checkIsSilencedEvent(@NonNull String key, String keyWithRawId) {

        try {

            return !key.isEmpty() && preferences_silentEvents.contains(key)
                    || !TextUtils.isEmpty(keyWithRawId) && preferences_silentEventsRawIds.contains(keyWithRawId);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setSilencedEvent(@NonNull String key, String keyWithRawId) {

        try {

            SharedPreferences.Editor editor = null;

            if (!key.isEmpty()) {
                preferences_silentEvents.add(key);
                editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putStringSet(context.getString(R.string.pref_Events_Silent_key), preferences_silentEvents);
            }

            if (!TextUtils.isEmpty(keyWithRawId)) {
                preferences_silentEventsRawIds.add(keyWithRawId);
                if (editor == null)
                    editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
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

            boolean idremoved = preferences_silentEvents.remove(key);

            if (!TextUtils.isEmpty(keyWithRawId))
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

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    boolean checkIsFavoriteEvent(@NonNull String key, String keyWithRawId, String isFavoriteContactEvent) {

        try {

            if (preferences_favoriteEvents_ids.isEmpty()) {
                cacheFavoriteEventsIds();
            }

            return (!key.isEmpty() && preferences_favoriteEvents_ids.contains(
                    key.substring(0, key.indexOf(Constants.STRING_2HASH)).concat(Constants.STRING_2HASH)))
                    || (!TextUtils.isEmpty(keyWithRawId) && preferences_favoriteEventsRawIds_ids.contains(
                    keyWithRawId.substring(0, keyWithRawId.indexOf(Constants.STRING_2HASH)).concat(Constants.STRING_2HASH)))
                    || (Constants.STRING_1.equals(isFavoriteContactEvent));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    boolean setFavoriteEvent(@NonNull String key, String keyWithRawId) {

        try {

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            boolean needSave = false;

            if (!key.isEmpty()) {
                if (preferences_favoriteEvents.add(key)) {
                    editor.putStringSet(context.getString(R.string.pref_Events_Favorite_key), preferences_favoriteEvents);
                    needSave = true;
                }
            }

            if (!TextUtils.isEmpty(keyWithRawId)) {
                if (preferences_favoriteEventsRawIds.add(keyWithRawId)) {
                    editor.putStringSet(context.getString(R.string.pref_Events_Favorite_rawIds_key), preferences_favoriteEventsRawIds);
                    needSave = true;
                }
            }

            if (!needSave) return false;

            if (editor.commit()) {
                statFavoriteEventsCount++;
                preferences_favoriteEvents_ids.clear();
                preferences_favoriteEventsRawIds_ids.clear();
                return true;
            }
            return false;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    boolean unsetFavoriteEvent(@NonNull String key, String keyWithRawId) {

        try {

            boolean idRemoved = false;
            HashSet<String> newValues = new HashSet<>();

            if (key.contains(Constants.STRING_2HASH)) {
                String keyPrefix = key.substring(0, key.indexOf(Constants.STRING_2HASH)).concat(Constants.STRING_2HASH);
                for (String event : preferences_favoriteEvents) {
                    if (event.startsWith(keyPrefix)) {
                        idRemoved = true;
                    } else {
                        newValues.add(event);
                    }
                }
                if (idRemoved) {
                    preferences_favoriteEvents.clear();
                    preferences_favoriteEvents.addAll(newValues);
                }
            }

            if (!TextUtils.isEmpty(keyWithRawId)) {
                boolean idRawRemoved = false;
                newValues.clear();
                String keyPrefix = keyWithRawId.substring(0, keyWithRawId.indexOf(Constants.STRING_2HASH)).concat(Constants.STRING_2HASH);
                for (String event : preferences_favoriteEventsRawIds) {
                    if (event.startsWith(keyPrefix)) {
                        idRawRemoved = true;
                    } else {
                        newValues.add(event);
                    }
                }
                if (idRawRemoved) {
                    preferences_favoriteEventsRawIds.clear();
                    preferences_favoriteEventsRawIds.addAll(newValues);
                    idRemoved = true;
                }
            }

            if (idRemoved) {

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
                cacheFavoriteEventsIds();
            }

            return idRemoved;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }

    }

    int getFavoritesEventsCount() {
        return preferences_favoriteEvents.size();
    }

    void clearFavoriteEvents() {

        try {

            preferences_favoriteEvents.clear();
            preferences_favoriteEventsRawIds.clear();
            preferences_favoriteEvents_ids.clear();
            preferences_favoriteEventsRawIds_ids.clear();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    int getEventsWithoutYearCount() {
        return preferences_eventsWithoutYear.size();
    }

    void clearEventsWithoutYear() {
        preferences_eventsWithoutYear.clear();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putStringSet(context.getString(R.string.pref_EventsWithoutYear_key), preferences_eventsWithoutYear).apply();
    }

    boolean checkIsEventWithoutYear(@NonNull String key) {
        return !key.isEmpty() && preferences_eventsWithoutYear.contains(key);
    }

    boolean setEventWithoutYear(@NonNull String key) {

        try {

            if (key.isEmpty()) return false;
            if (checkIsEventWithoutYear(key)) return false;

            preferences_eventsWithoutYear.add(key);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_EventsWithoutYear_key), preferences_eventsWithoutYear).apply();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    boolean unsetEventWithoutYear(@NonNull String key) {

        try {

            if (key.isEmpty()) return false;
            if (!checkIsEventWithoutYear(key)) return false;

            preferences_eventsWithoutYear.remove(key);
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_EventsWithoutYear_key), preferences_eventsWithoutYear).apply();
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    private void cacheFavoriteEventsIds() {
        try {

            preferences_favoriteEvents_ids.clear();

            for (String id : preferences_favoriteEvents) {
                preferences_favoriteEvents_ids.add(id.substring(0, id.indexOf(Constants.STRING_2HASH)).concat(Constants.STRING_2HASH));
            }

            preferences_favoriteEventsRawIds_ids.clear();

            for (String id : preferences_favoriteEventsRawIds) {
                preferences_favoriteEventsRawIds_ids.add(id.substring(0, id.indexOf(Constants.STRING_2HASH)).concat(Constants.STRING_2HASH));
            }

            if (preferences_favoriteEvents_ids.isEmpty() && preferences_favoriteEventsRawIds_ids.isEmpty()) {
                preferences_favoriteEvents_ids.add(Constants.STRING_UNDERSCORE);
                preferences_favoriteEventsRawIds_ids.add(Constants.STRING_UNDERSCORE);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Возвращает ID контакта, связанный с linkID
     *
     * @param linkID ID события
     * @return ID контакта или null
     */
    String getMergedID(String linkID) {

        try {

            if (linkID == null || preferences_mergedIDs.isEmpty()) return null;

            String mergedID = preferences_mergedIDs.get(linkID);
            if (!TextUtils.isEmpty(mergedID) && map_contacts_ids.containsKey(mergedID)) {
                return mergedID;
            } else {
                mergedID = preferences_mergedRawIDs.get(linkID);
                if (!TextUtils.isEmpty(mergedID)) {
                    String contactIDMerged = map_contacts_rawIds.get(mergedID);
                    if (!TextUtils.isEmpty(contactIDMerged) && map_contacts_ids.containsKey(contactIDMerged)) {
                        return contactIDMerged;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return null;
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

    int getXDaysEventsCount() {

        try {

            return preferences_xDaysEvents.isEmpty() ? 0 : preferences_xDaysEvents.size();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return 0;
        }
    }

    boolean isXDaysEvent(@NonNull String eventId) {
        return preferences_xDaysEvents.containsKey(eventId);
    }

    @NonNull
    List<String> getXDaysEvent(@NonNull String eventId) {

        List<String> result = new ArrayList<>();
        try {

            final String eventRow = preferences_xDaysEvents.get(eventId);
            if (eventRow != null)
                result.addAll(Arrays.asList(eventRow.split(Constants.REGEX_BAR, -1)));
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

    boolean setXDaysEvent(@NonNull String eventKey, String eventRow) {
        try {

            if (isXDaysEvent(eventKey)) {
                if (eventRow == null) { //Удаляем существующее
                    preferences_xDaysEvents.remove(eventKey);
                } else { //Заменяем
                    preferences_xDaysEvents.remove(eventKey);
                    preferences_xDaysEvents.put(eventKey, eventRow.replace(Constants.STRING_COLON_SPACE, Constants.STRING_SPACE));
                }
            } else if (eventRow != null) { //Добавляем новое
                preferences_xDaysEvents.put(eventKey, eventRow.replace(Constants.STRING_COLON_SPACE, Constants.STRING_SPACE));
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
     * @param endDate   End date for period
     * @param eventDate Original eventInfo date
     * @param periods   Events repeat periods (by comma)
     * @param toRepeat  Positive: how many events to return (total), Negative: how many events of every period to return (from startDate)
     * @return ArrayList of events inside [startDate] ... [endDate] period
     */
    @NonNull
    ArrayList<Event> getNextRepeatsForEvent(@NonNull Calendar startDate, @NonNull Calendar endDate, @NonNull Calendar eventDate, @NonNull String periods, int toRepeat) {
        ArrayList<Event> result = new ArrayList<>();
        try {

            Set<Long> selectedDates = new HashSet<>();

            if (toRepeat == 0) return result;
            if (startDate.after(endDate)) return result;

            String[] allPeriods = periods.split(Constants.STRING_COMMA, -1);
            for (String period : allPeriods) {
                int days = 0;
                int repeated = 0;
                try {
                    days = Integer.parseInt(period.trim());
                } catch (NumberFormatException e) { /**/ }
                if (days == 0) continue;

                Calendar date = (Calendar) eventDate.clone();

                if (toRepeat > 0) {
                    boolean isContinue = true;
                    while (isContinue) {
                        date.add(Calendar.DAY_OF_YEAR, days);
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
                    for (int i = 1; i <= -toRepeat; i++) {
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

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return result;
    }

    void clearDeadLinksSilencedEvents() {

        try {

            int countRemoved = 0;

            //IDs
            Set<String> toRemoveIds = new HashSet<>();
            for (String event : preferences_silentEvents) {
                if (event.isEmpty()) {
                    toRemoveIds.add(event);
                    continue;
                }
                final String[] keyParts = StringUtils.getKeyParts(event);
                if (keyParts[1].equals(Constants.EventType_Calendar)) {
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
                if (event.isEmpty()) {
                    toRemoveRawIds.add(event);
                    continue;
                }
                final String[] keyParts = StringUtils.getKeyParts(event);
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
        try {

            int countRemoved = 0;

            //IDs
            Set<String> toRemoveIds = new HashSet<>();
            for (String event : preferences_hiddenEvents) {
                if (event.isEmpty()) {
                    toRemoveIds.add(event);
                    continue;
                }
                final String[] keyParts = StringUtils.getKeyParts(event);
                if (keyParts[1].equals(Constants.EventType_Calendar)) {
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
                if (event.isEmpty()) {
                    toRemoveRawIds.add(event);
                    continue;
                }
                final String[] keyParts = StringUtils.getKeyParts(event);
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

            Iterator<Map.Entry<String, String>> iterator = preferences_xDaysEvents.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
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

            //todo: добавить заполнение значениями по-умолчанию, как в getWidgetPreference
            setPreferenceString(context.getString(R.string.widget_config_PrefName) + id, value);
            ToastExpander.showDebugMsg(context, resources.getString(R.string.msg_widget_prefs_saved, String.valueOf(id), value));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @NonNull
    List<String> getWidgetPreference(int widgetID, String widgetType) {

        String defaultPrefString = context.getString(R.string.widget_config_defaultPref);
        if (widgetType != null) {
            switch (widgetType) {
                case Constants.WIDGET_TYPE_LIST:
                    defaultPrefString = context.getString(R.string.widget_config_defaultPref_List);
                    break;
                case Constants.WIDGET_TYPE_PHOTO_LIST:
                    defaultPrefString = context.getString(R.string.widget_config_defaultPref_PhotoList);
                    break;
                case Constants.WIDGET_TYPE_CALENDAR:
                    defaultPrefString = context.getString(R.string.widget_config_defaultPref_Calendar);
                    break;
            }
        }

        List<String> defaultPref = Arrays.asList(defaultPrefString.split(Constants.STRING_COMMA, -1));
        if (context == null) return defaultPref;

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String prefKey = context.getString(R.string.widget_config_PrefName) + widgetID;
            if (!preferences.contains(prefKey)) {
                pinnedWidgetId = widgetID;
            }
            String strPref = preferences.getString(prefKey, defaultPrefString);
            String[] pref = strPref.split(Constants.STRING_COMMA, -1);
            List<String> prefWidget = new ArrayList<>(Arrays.asList(pref));

            //Заполнение значениями по умолчанию
            while (prefWidget.size() < defaultPref.size()) {
                prefWidget.add(defaultPref.get(prefWidget.size()));
            }

            //Совместимость с версиями до 1.83
            if (widgetType != null && prefWidget.get(8).length() > 6 && prefWidget.get(11).isEmpty()
                    && (widgetType.equals(Constants.WIDGET_TYPE_5X1) || widgetType.equals(Constants.WIDGET_TYPE_4X1) || widgetType.equals(Constants.WIDGET_TYPE_2X2))) {
                ToastExpander.showDebugMsg(context, "set compatibility for pre 1.8.3 version");
                prefWidget.set(11, prefWidget.get(8));
                prefWidget.set(8, defaultPref.get(8));
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

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences != null) {
                preferences.edit().remove(context.getString(R.string.widget_config_PrefName) + id).apply();
                ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_widget_prefs_removed, String.valueOf(id)));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    Set<String> getPreferences_Accounts() {
        return preferences_Accounts;
    }

    void setPreferences_Accounts(Set<String> preferences_Accounts) {
        this.preferences_Accounts.clear();
        this.preferences_Accounts.addAll(preferences_Accounts);
    }

    Set<String> getPreferences_Calendars(@NonNull String eventType) {

        switch (eventType) {
            case Constants.EventType_BirthDay:
                return preferences_BirthDay_calendars;
            case Constants.EventType_Other:
                return preferences_OtherEvent_calendars;
            case Constants.EventType_Holiday:
                return preferences_HolidayEvent_calendars;
            case Constants.Type_MultiEvent:
                return preferences_MultiType_calendars;
            default:
                return new HashSet<>();
        }

    }

    void setPreferences_Calendars(@NonNull String eventType, Set<String> preferences_Calendars) {

        switch (eventType) {
            case Constants.EventType_BirthDay:
                this.preferences_BirthDay_calendars = preferences_Calendars;
                break;
            case Constants.EventType_Other:
                this.preferences_OtherEvent_calendars = preferences_Calendars;
                break;
            case Constants.EventType_Holiday:
                this.preferences_HolidayEvent_calendars = preferences_Calendars;
                break;
            case Constants.Type_MultiEvent:
                this.preferences_MultiType_calendars = preferences_Calendars;
                break;
        }

    }

    void setPreferences_Files(String eventType, Set<String> preferences_Files) {

        switch (eventType) {
            case Constants.EventType_BirthDay:
                this.preferences_Birthday_files = preferences_Files;
                break;
            case Constants.EventType_Other:
                this.preferences_OtherEvent_files = preferences_Files;
                break;
            case Constants.EventType_Holiday:
                this.preferences_HolidayEvent_files = preferences_Files;
                break;
            case Constants.EventType_Fact:
                this.preferences_FactEvent_files = preferences_Files;
                break;
            case Constants.Type_MultiEvent:
                this.preferences_MultiType_files = preferences_Files;
                break;
        }

    }

    void setPreferences_AlarmTime(int queueNumber, int alarmHour, int alarmMinute) {
        if (queueNumber == 1) {
            this.preferences_notifications_alarm_hour = alarmHour;
            this.preferences_notifications_alarm_minute = alarmMinute;
        } else if (queueNumber == 2) {
            this.preferences_notifications2_alarm_hour = alarmHour;
            this.preferences_notifications2_alarm_minute = alarmMinute;
        }
    }

    int getPreferences_IconPackNumber() {
        return preferences_IconPackNumber;
    }

    void setPreferences_IconPackNumber(int packNumber) {
        preferences_IconPackNumber = packNumber;
    }

    void setPreferences_Icon(String iconName) {
        preferences_Icon = iconName;
    }

    Bitmap getPreferences_Icon() {
        Bitmap defaultIcon = ImageUtils.getBitmap(context, R.mipmap.ic_launcher_spring_round);
        try {

            List<String> iconIDs = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_Icon_values)));
            List<Integer> iconImages = new ArrayList<>();
            iconImages.add(R.mipmap.ic_launcher_spring_round);
            iconImages.add(R.mipmap.ic_launcher_summer_round);
            iconImages.add(R.mipmap.ic_launcher_autumn_round);
            iconImages.add(R.mipmap.ic_launcher_winter_round);
            iconImages.add(R.mipmap.ic_launcher_grey_round);
            iconImages.add(R.mipmap.ic_launcher_black_round);

            if (iconIDs.contains(preferences_Icon)) {
                Bitmap icon = ImageUtils.getBitmap(context, iconImages.get(iconIDs.indexOf(preferences_Icon)));
                if (icon != null) return icon;
            }
            return defaultIcon;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return defaultIcon;
        }
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

    void showAnniversaryList(Context context, String age) {

        int selectedAge = -1;
        int selectedPosition = -1;

        try {

            class HolidayAdapter extends ArrayAdapter<String> {

                final int selectedPosition;

                public HolidayAdapter(Context context, int resource, String[] objects, int selectedPosition) {
                    super(context, resource, objects);
                    this.selectedPosition = selectedPosition;
                }

                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = view.findViewById(android.R.id.text1);

                    String item = getItem(position);
                    if (position == this.selectedPosition) {
                        SpannableString spannableString = new SpannableString(item);
                        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableString.length(), 0);
                        textView.setText(spannableString);
                    } else {
                        textView.setText(item);
                    }

                    return view;
                }
            }

            List<String> items = new ArrayList<>();
            if (age != null) {
                try {
                    selectedAge = Integer.parseInt(age);
                } catch (NumberFormatException ignored) { /**/ }
            }

            for (int i = 1; i <= 100; i++) {
                @Nullable String anCaption = getWeddingName(i);
                if (StringUtils.hasContent(anCaption)) {
                    String holiday = i + Constants.STRING_COLON_SPACE + anCaption;
                    if (i == selectedAge) {
                        selectedPosition = items.size();
                    }
                    items.add(holiday);
                } else if (selectedAge > -1 && i == selectedAge) {
                    selectedPosition = items.size();
                    String holiday = i + Constants.STRING_COLON_SPACE + " ???";
                    items.add(holiday);
                }
            }
            if (age != null && selectedPosition == -1) {
                selectedPosition = items.size();
                String holiday = age + Constants.STRING_COLON_SPACE + " ???";
                items.add(holiday);
            }

            HolidayAdapter adapter = new HolidayAdapter(context, R.layout.dialog_list_item, items.toArray(new String[0]), selectedPosition);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, preferences_theme.themeDialog))
                    .setTitle(R.string.pref_CustomEvents_Anniversary_List_summary)
                    .setIcon(R.drawable.ic_event_wedding)
                    .setAdapter(adapter, null)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);
            AlertDialog alertToShow = builder.create();

            int finalSelectedPosition = selectedPosition;
            alertToShow.setOnShowListener(arg0 -> {
                try (TypedArray ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme)) {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                }
                if (finalSelectedPosition > -1) {
                    alertToShow.getListView().smoothScrollToPosition(finalSelectedPosition + 4);
                }
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
                    return msg;
            }
            return Constants.HTML_COLOR_START + Integer.toHexString(ContextCompat.getColor(context, colorId) & 0x00ffffff)
                    + Constants.HTML_COLOR_MIDDLE + msg + Constants.HTML_COLOR_END;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return msg;
    }

    /**
     * Возвращает отформатированную дату
     *
     * @param dateIn     Дата строкой DDMMYYY
     * @param dateFormat Формат даты (с годом или без)
     * @return Отформатированная дата, согласно указанному формату и настройки формата даты
     */
    @NonNull
    String getDateFormatted(String dateIn, FormatDate dateFormat) {

        String resultString = Constants.STRING_EMPTY;
        if (TextUtils.isEmpty(dateIn)) return resultString;
        if (preferences_date_format == 2 && dateFormat == FormatDate.WithYear)
            return dateIn; // DD.MM.YYYY

        String postfixBC = getResources().getString(R.string.msg_after_year_bc);
        if (!dateIn.endsWith(postfixBC)) postfixBC = Constants.STRING_EMPTY;

        try {

            final Locale locale = Locale.forLanguageTag(currentLocale);
            //todo: переметить в поля класса + initLocaleStrings()
            SimpleDateFormat sdfInY = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale);
            SimpleDateFormat sdfIn = new SimpleDateFormat(Constants.DATE_DD_MM, locale);
            SimpleDateFormat sdfOut = null;
            Date eventDate = null;
            boolean isYearPresent = false;

            switch (preferences_date_format) {

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
                        if (dateFormat == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_MM_DD_YYYY, locale);
                        } else if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
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
                        if (dateFormat == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_UK, locale);
                        } else if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
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
                        if (dateFormat == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_IND, locale);
                        } else if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
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
                        if (dateFormat == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_DD_MMM_YYYY, locale);
                        } else if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
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
                        if (dateFormat == FormatDate.WithYear && isYearPresent) {
                            sdfOut = new SimpleDateFormat(Constants.DATE_D_MMMM_YYYY, locale);
                        } else if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
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
                        if (dateFormat == FormatDate.WithYear && isYearPresent) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        } else { //if (!isYearPresent || dateFormat == FormatDate.WithoutYear) {
                            resultString = DateUtils.formatDateTime(context, eventDate.getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_NUMERIC_DATE);
                        }
                    }
            }

        } catch (Exception e) { /**/ }

        return TextUtils.isEmpty(resultString) ? resultString : resultString.concat(postfixBC);

    }

    @NonNull
    String getDateTimePreferable(@NonNull Date dateIn) {

        String resultString = Constants.STRING_EMPTY;

        try {

            final Locale locale = Locale.forLanguageTag(currentLocale);
            SimpleDateFormat sdfOut;
            final String timeFormat = " HH:mm";

            switch (preferences_date_format) {

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
                    for (String source : sourcesPrefList) {
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

    void fillEmptyEventData(TreeMap<Integer, String> eventData) {

        try {

            if (eventData == null) return;

            for (int i = 0; i < Position_attrAmount; i++) {
                if (!eventData.containsKey(i)) {
                    eventData.put(i, Constants.STRING_EMPTY);
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

                case 4:

                    preferences_IconPackImages_F.put(0, R.drawable.ic_pack00_f1);

                    preferences_IconPackImages_M.put(0, R.drawable.ic_pack00_m1);

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

            StringBuilder listEventsTypes = new StringBuilder();
            if (!preferences_list_event_types.isEmpty()) {
                String[] typeIDs = resources.getStringArray(R.array.pref_List_EventTypes_values);
                String[] typeNames = resources.getStringArray(R.array.pref_List_EventTypes_entries);
                int countTypes = typeIDs.length;
                for (int i = 0; i < countTypes; i++) {
                    if (preferences_list_event_types.contains(typeIDs[i])) {
                        if (listEventsTypes.length() > 0)
                            listEventsTypes.append(Constants.STRING_COMMA_SPACE);
                        listEventsTypes.append(typeNames[i]);
                    }
                }
            }

            final String msg_not_selected = resources.getString(R.string.msg_not_selected);

            // Параметры для строки msg_zero_events_body
            String eventTypesParam = (preferences_list_event_types.isEmpty()
                    ? Constants.FONT_COLOR_RED + resources.getString(R.string.msg_none)
                    : Constants.FONT_COLOR_GREEN + listEventsTypes) + Constants.HTML_COLOR_END;

            String permissionsAccountsAndContactsParam = resources.getString(R.string.stats_permissions_accounts,
                    ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED
                            ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END
                            : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END)
                    + resources.getString(R.string.stats_permissions_contacts,
                    !DeviceTools.checkNoContactsAccess(context)
                            ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END
                            : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END);

            String accountsParam = (preferences_Accounts.isEmpty()
                    ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_all)
                    : !preferences_Accounts.contains(Constants.account_none)
                      ? Constants.HTML_BR + Constants.FONT_COLOR_GREEN + TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_Accounts)
                      : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_none))
                    + Constants.HTML_COLOR_END;

            String calendarPermissionParam = resources.getString(R.string.stats_permissions_calendar,
                    !DeviceTools.checkNoCalendarAccess(context)
                            ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_on) + Constants.HTML_COLOR_END
                            : Constants.FONT_COLOR_RED + resources.getString(R.string.msg_off) + Constants.HTML_COLOR_END);

            if (map_calendars.isEmpty())
                AppDateUtils.fillCalendarList(context, map_calendars, map_calendars_colors);

            String birthdayCalendarsParam = preferences_BirthDay_calendars.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + StringUtils.replaceCalendarIDtoTitle(preferences_BirthDay_calendars, map_calendars) + Constants.HTML_COLOR_END;

            String otherEventCalendarsParam = preferences_OtherEvent_calendars.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + StringUtils.replaceCalendarIDtoTitle(preferences_OtherEvent_calendars, map_calendars) + Constants.HTML_COLOR_END;

            String holidayEventCalendarsParam = preferences_HolidayEvent_calendars.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + StringUtils.replaceCalendarIDtoTitle(preferences_HolidayEvent_calendars, map_calendars) + Constants.HTML_COLOR_END;

            String multiTypeCalendarsParam = preferences_MultiType_calendars.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + StringUtils.replaceCalendarIDtoTitle(preferences_MultiType_calendars, map_calendars) + Constants.HTML_COLOR_END;

            String birthdayFilesParam = preferences_Birthday_files.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_Birthday_files) + Constants.HTML_COLOR_END;

            String otherEventFilesParam = preferences_OtherEvent_files.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_OtherEvent_files) + Constants.HTML_COLOR_END;

            String holidayEventFilesParam = preferences_HolidayEvent_files.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_HolidayEvent_files) + Constants.HTML_COLOR_END;

            String multiTypeFilesParam = preferences_MultiType_files.isEmpty()
                    ? msg_not_selected
                    : Constants.HTML_BR + Constants.FONT_COLOR_GREEN + TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_MultiType_files) + Constants.HTML_COLOR_END;

            List<String> allFiltersList = Arrays.asList(
                    resources.getString(R.string.events_scope_not_hidden),
                    resources.getString(R.string.events_scope_all),
                    resources.getString(R.string.events_scope_hidden),
                    resources.getString(R.string.events_scope_silenced),
                    resources.getString(R.string.events_scope_xdays),
                    resources.getString(R.string.events_scope_unrecognized),
                    resources.getString(R.string.events_scope_favorite));

            String eventScopeFilterParam = (preferences_list_events_scope < 2
                    ? Constants.FONT_COLOR_GREEN
                    : Constants.FONT_COLOR_RED) + StringUtils.substringBefore(allFiltersList.get(preferences_list_events_scope), Constants.STRING_PARENTHESIS_OPEN) + Constants.HTML_COLOR_END;

            StringBuilder listEventsSources = new StringBuilder();
            if (!preferences_list_EventSources.isEmpty()) {
                final EventSources eventSources = new EventSources();
                eventSources.loadEventSources(resources.getString(R.string.pref_List_EventSources_key));
                List<String> eventSourcesHashes = eventSources.getHashes();
                for (int i = 0; i < eventSourcesHashes.size(); i++) {
                    String hash = eventSourcesHashes.get(i);
                    if (preferences_list_EventSources.contains(hash)) {
                        if (listEventsSources.length() > 0)
                            listEventsSources.append(Constants.HTML_BR);
                        listEventsSources.append(eventSources.getTitles().get(i));
                    }
                }
            }

            String eventSourcesParam = listEventsSources.length() == 0
                    ? Constants.FONT_COLOR_GREEN + resources.getString(R.string.msg_all) + Constants.HTML_COLOR_END
                    : Constants.HTML_BR + Constants.FONT_COLOR_RED + listEventsSources + Constants.HTML_COLOR_END;

            // Формирование результата
            final String result = resources.getString(R.string.msg_zero_events_body,
                    eventTypesParam,
                    permissionsAccountsAndContactsParam,
                    accountsParam,
                    calendarPermissionParam,
                    birthdayCalendarsParam,
                    otherEventCalendarsParam,
                    holidayEventCalendarsParam,
                    multiTypeCalendarsParam,
                    birthdayFilesParam,
                    otherEventFilesParam,
                    holidayEventFilesParam,
                    multiTypeFilesParam,
                    eventScopeFilterParam,
                    eventSourcesParam
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }

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

    @NonNull
    HashMap<String, String> getPreferenceStringSetAsMap(@NonNull SharedPreferences preferences, @NonNull String key, @SuppressWarnings("SameParameterValue") @NonNull String divider) {
        HashMap<String, String> result = new HashMap<>();
        try {

            Set<String> pref = preferences.getStringSet(key, null);
            if (pref == null) return result;
            for (String value : pref) {
                int ind = value.indexOf(divider);
                if (ind > -1) {
                    result.put(value.substring(0, ind), value.substring(ind + divider.length()));
                }
            }

        } catch (Exception ignored) { /**/ }
        return result;
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
                    if (preferences_Icon.equals(iconID)) {
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
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_icon_changed, resources.getString(R.string.pref_Icon_default) + " (default)"));
                    pm.setComponentEnabledSetting(new ComponentName(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "." + resources.getString(R.string.pref_Icon_default)), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } catch (IllegalArgumentException e) { /**/ }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

    @NonNull
    String setAgeFormatting(@NonNull String strAge) {

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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return strAge;
        }

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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @NonNull
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

        return decorString != 0 ? context.getString(decorString, installer) : installer;

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
                resultInfo = resources.getString(R.string.msg_event_type_label_set, eventLabel);
            } else {
                setPreferenceString(keyForLabels, customLabels.concat(Constants.STRING_COMMA).concat(eventLabel));
                resultInfo = resources.getString(R.string.msg_event_type_label_added, eventLabel);
            }

            if (eventTypeId > 6 && eventTitle != null) {
                setPreferenceString(keyForTitle, eventTitle);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return resultInfo;

    }

    boolean isJubilee(int age, @NonNull String eventSubType) {
        try {
            if (age <= 0) return false;
            final String algorithm = String.valueOf(preferences_jubilee_algorithm);
            if (algorithm.equals(context.getString(R.string.pref_List_Jubilee_Algorithm_Every_10))) {
                return age % 10 == 0;
            } else if (algorithm.equals(context.getString(R.string.pref_List_Jubilee_Algorithm_Every_5))) {
                return age % 5 == 0;
            } else if (algorithm.equals(context.getString(R.string.pref_List_Jubilee_Algorithm_Every_Flex))) {
                if (eventSubType.equals(Constants.EventType_BirthDay)) {
                    if (age < 45) {
                        return Arrays.asList(1, 3, 5, 10, 14, 18, 21, 30).contains(age);
                    } else {
                        return age % 5 == 0;
                    }
                } else if (eventSubType.equals(Constants.EventType_Anniversary)) {
                    return age % 10 == 0;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void setRecentColor(int newValue) {

        try {

            if (!preferences_RecentColors.contains(newValue)) {
                while (preferences_RecentColors.size() >= resources.getInteger(R.integer.pref_Colors_Recent_max)) {
                    preferences_RecentColors.remove(0);
                }
                preferences_RecentColors.add(newValue);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putString(context.getString(R.string.pref_Colors_Recent_key), TextUtils.join(Constants.STRING_COMMA_SPACE, preferences_RecentColors));
                editor.apply();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Получение массива типов событий для даты
     *
     * @param day       Дата в формате yyyy-MM-dd
     * @param fromPacks Список источников, откуда брать события
     * @return Список типов событий
     */
    @NonNull
    List<DayType> getDayTypes(@NonNull String day, @NonNull List<String> fromPacks) {
        List<DayType> types = new ArrayList<>();
        try {

            for (String packId : fromPacks) {
                final String key = packId.concat(Constants.STRING_COLON).concat(day);
                final String key_noYear = packId.concat(Constants.STRING_COLON).concat("-").concat(day.substring(4));
                if (preferences_DaysTypes.containsKey(key)) {
                    types.add(new DayType(packId, preferences_DaysTypes.get(key)));
                }
                if (preferences_DaysTypes.containsKey(key_noYear)) {
                    types.add(new DayType(packId, preferences_DaysTypes.get(key_noYear)));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return types;
    }

    /**
     * Получение списка событий из общего массива
     *
     * @param day       Дата в формате yyyy-MM-dd
     * @param fromPacks Список источников, откуда брать события
     * @param colors    Массив цветов для источников (устанавливается в настройках конкретного виджета)
     * @return Список событий
     */
    @NonNull
    List<String> getDayInfo(@NonNull String day, @NonNull List<String> fromPacks, HashMap<String, Integer> colors) {
        List<String> dayInfo = new ArrayList<>();
        try {

            for (String packId : fromPacks) {

                final String key = packId.concat(Constants.STRING_COLON).concat(day);
                final String key_noYear = packId.concat(Constants.STRING_COLON).concat("-").concat(day.substring(4));

                @ColorInt Integer colorValue = null;
                if (colors != null && colors.containsKey(packId) && colors.get(packId) != null) {
                    Integer colorFromPack = colors.get(packId);
                    if (colorFromPack != null && Color.alpha(colorFromPack) != 0) {
                        colorValue = colorFromPack;
                    }
                }

                String colorRGB = colorValue != null ? Integer.toHexString(colorValue & 0x00ffffff) : Constants.TRANSPARENT;
                if (preferences_DaysInfo.containsKey(key) && preferences_DaysInfo.get(key) != null) {
                    String[] eventsList = StringUtils.getNotNullString(preferences_DaysInfo.get(key)).split(Constants.STRING_EOT, -1);
                    for (String eventInfo : eventsList) {
                        dayInfo.add(Constants.FONT_COLOR_DOT_START + colorRGB + Constants.FONT_COLOR_DOT_END + eventInfo);
                    }
                }
                if (preferences_DaysInfo.containsKey(key_noYear) && preferences_DaysInfo.get(key_noYear) != null) {
                    String[] eventsList = StringUtils.getNotNullString(preferences_DaysInfo.get(key_noYear)).split(Constants.STRING_EOT, -1);
                    for (String eventInfo : eventsList) {
                        dayInfo.add(Constants.FONT_COLOR_DOT_START + colorRGB + Constants.FONT_COLOR_DOT_END + eventInfo);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return dayInfo;
    }

    void clearDaysTypesAndInfo() {
        preferences_DaysTypes.clear();
        preferences_DaysInfo.clear();
    }

    /**
     * Считывание праздников для {@link WidgetCalendar} из внутренних справочников
     *
     * @param packsHashes Хэши источников для получения событий
     * @param packPrefix Префикс массива событий в ресурсах
     * @param eventSourcePrefix Префикс типа события для составления hash
     * @param eventPrefix Префикс, добавляемый перед названием события из этого массива
     */
    @SuppressWarnings("SameParameterValue")
    @SuppressLint("DiscouragedApi")
    void fillDaysTypesFromHolidays(@NonNull List<String> packsHashes, @NonNull String packPrefix, @NonNull String eventSourcePrefix, String eventPrefix) {
        try {

            if (packsHashes.isEmpty()) return;

            //Справочники праздников и выходных
            int eventsPackCount = 1;
            int packId = getResources().getIdentifier(packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            while (packId > 0) {
                try {

                    String[] eventsPack = getResources().getStringArray(packId);
                    int countEvents = eventsPack.length;
                    if (countEvents > 1) {
                        final String packHash = StringUtils.getHash(eventSourcePrefix + eventsPack[0]);
                        if (packsHashes.contains(packHash)) {
                            Log.i("HOLIDAY", eventsPack[0] + Constants.STRING_PARENTHESIS_OPEN + packHash + Constants.STRING_PARENTHESIS_CLOSE);
                            for (int i = 1; i < countEvents; i++) {
                                String eventsArray = eventsPack[i];
                                String[] events = eventsArray.split(Constants.STRING_EOL, -1);

                                String titlePrefix = Constants.STRING_EMPTY;
                                if (StringUtils.hasContent(eventPrefix)) {
                                    titlePrefix = eventPrefix;
                                }
                                if (eventsPack[0].indexOf(Constants.STRING_SPACE) == 4) {
                                    titlePrefix = eventsPack[0].substring(0, eventsPack[0].indexOf(Constants.STRING_SPACE) + 1);
                                }
                                fillDaysTypesFromFile(packHash, events, titlePrefix, DayType.Type.Common);
                            }
                            preferences_DaysTypes.put(packHash, DayType.Type.Holiday); //todo: для других праздников - продумать
                        }
                    }

                } catch (Resources.NotFoundException ignored) { /**/ }

                eventsPackCount++;
                packId = getResources().getIdentifier(packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Считывание событий для {@link WidgetCalendar} из файлов
     *
     * @param fileHashes Хэши источников для получения событий
     */
    @SuppressLint("DiscouragedApi")
    void fillDaysTypesFromFiles(@NonNull List<String> fileHashes) {
        try {

            if (fileHashes.isEmpty()) return;
            Set<String> fileList = preferences_HolidayEvent_files;
            if (fileList == null || fileList.isEmpty()) return;

            for (String file : fileList) {
                final String packHash = StringUtils.getHash(Constants.eventSourceFilePrefix + file);
                if (fileHashes.contains(packHash)) {
                    if (!preferences_DaysTypes.containsKey(packHash)) {
                        String[] fileDetails = file.split(Constants.REGEX_BAR);
                        Log.i("FILE", fileDetails[0] + Constants.STRING_PARENTHESIS_OPEN + packHash + Constants.STRING_PARENTHESIS_CLOSE);
                        String[] eventsArray = readFileToString(file, Constants.STRING_EOL).split(Constants.STRING_EOL, -1);
                        if (eventsArray[0].isEmpty()) {
                            ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                            continue;
                        }
                        fillDaysTypesFromFile(packHash, eventsArray, Constants.eventTitleFilePrefix, DayType.Type.Holiday);
                    }
                    preferences_DaysTypes.put(packHash, DayType.Type.Holiday);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Добавление массива событий в общие массивы событий и типов событий для {@link WidgetCalendar}
     *
     * @param packHash       Хэш источника (тип источника + путь до источника (файла или внутреннего ресурса))
     * @param events         Массив событий (дата + флаги события + описание события)
     * @param titlePrefix    Префикс, добавляемый для всех событий (например: иконка)
     * @param defaultDayType Тип дня по-умолчанию (если не стоят флаги "!" или "?")
     */
    private void fillDaysTypesFromFile(String packHash, String[] events, @NonNull String titlePrefix, @NonNull DayType.Type defaultDayType) {
        try {

            if (preferences_DaysTypes.containsKey(packHash)) return;

            for (String eventLine : events) {
                String day = eventLine.trim();

                if (day.isEmpty() || day.startsWith(Constants.STRING_HASH) || day.startsWith(Constants.STRING_DSLASH))
                    continue;

                final int indexComma = day.indexOf(Constants.STRING_COMMA);
                final int indexFirstSpace = day.indexOf(Constants.STRING_SPACE);
                String flags = Constants.STRING_EMPTY;
                if (indexComma > -1 && indexComma < indexFirstSpace) {
                    flags = day.substring(indexComma + 1, indexFirstSpace);
                } else if (indexComma > -1) {
                    flags = day.substring(indexComma + 1);
                }

                Date dateEvent = null;
                String eventDateString;
                if (indexComma > -1) {
                    eventDateString = day.substring(0, indexComma);
                } else if (indexFirstSpace > -1) {
                    eventDateString = day.substring(0, indexFirstSpace);
                } else {
                    ToastExpander.showInfoMsg(getContext().getApplicationContext(), resources.getString(R.string.msg_event_parse_error, day));
                    continue;
                }

                boolean isFloating = false;
                try {
                    String dateNextFloatingEvent = computeFloatingDate(eventDateString, 0);
                    if (!eventDateString.equals(dateNextFloatingEvent)) {
                        eventDateString = dateNextFloatingEvent;
                        isFloating = true;
                    } else if (eventDateString.contains(Constants.STRING_0000)) {
                        eventDateString = eventDateString.replace(Constants.STRING_0000, String.valueOf(getToday().get(Calendar.YEAR)));
                    }
                    dateEvent = Objects.requireNonNull(sdf_DDMMYYYY.get()).parse(eventDateString);
                } catch (Exception e1) {
                    try {
                        dateEvent = Objects.requireNonNull(sdf_india.get()).parse(eventDateString);
                    } catch (Exception e2) {
                        try {
                            dateEvent = Objects.requireNonNull(sdf_uk.get()).parse(eventDateString);
                        } catch (Exception e3) {
                            try {
                                dateEvent = Objects.requireNonNull(sdf_java.get()).parse(eventDateString);
                            } catch (Exception e4) {
                                //Не получилось распознать
                            }
                        }
                    }
                }
                if (dateEvent != null) {
                    final String eventTitle = titlePrefix
                            + StringUtils.substringBefore(day.substring(indexFirstSpace + 1).trim(), Constants.STRING_BAR)
                            + Constants.STRING_PARENTHESIS_OPEN
                            + (dateEvent.getYear() + 1900)
                            + Constants.STRING_PARENTHESIS_CLOSE;
                    final DayType.Type dayType = flags.contains("!") ? DayType.Type.Holiday :
                            flags.contains("?") ? DayType.Type.Workday : defaultDayType;
                    String key;
                    if (flags.contains(Constants.STRING_1) || isFloating) {
                        key = packHash.concat(Constants.STRING_COLON).concat(Objects.requireNonNull(sdf_java.get()).format(dateEvent));
                    } else {
                        key = packHash.concat(Constants.STRING_COLON).concat(Objects.requireNonNull(sdf_java_no_year.get()).format(dateEvent));
                    }
                    //todo: если ежегодное событие начинается с какой-то даты в прошлом, то в календаре будут отражаться это событие и до этой даты
                    fillDayTypeAndInfo(key, dayType, eventTitle);
                } else {
                    ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_event_parse_error, day));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Добавление события в общие массивы событий и типов событий
     *
     * @param key        Ключ (packHash:yyyy-MM-dd)
     * @param dayType    Тип дня (праздник, рабочий, ...)
     * @param eventTitle Данные о событии
     */
    private void fillDayTypeAndInfo(String key, DayType.Type dayType, String eventTitle) {
        try {

            DayType.Type dayTypeStored = preferences_DaysTypes.get(key);
            if (dayTypeStored != null) {
                if (dayTypeStored == DayType.Type.Common) {
                    preferences_DaysTypes.put(key, dayType);
                }
            } else {
                preferences_DaysTypes.put(key, dayType);
            }
            String dayInfo = preferences_DaysInfo.get(key);
            if (dayInfo != null) {
                if (!dayInfo.contains(eventTitle)) {
                    preferences_DaysInfo.put(key, dayInfo.concat(Constants.STRING_EOT).concat(eventTitle));
                }
            } else {
                preferences_DaysInfo.put(key, eventTitle);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Считывание событий из календарей
     *
     * @param calendarHashes Хэши источников для получения событий
     * @param startPeriod    Первый день периода считывания
     * @param endPeriod      Последний день периода считывания
     */
    void fillDaysTypesFromCalendars(List<String> calendarHashes, @NonNull Calendar startPeriod, @NonNull Calendar endPeriod) {
        try {

            if (DeviceTools.checkNoCalendarAccess(context)) return;

            StringBuilder calIDs = new StringBuilder();
            for (String calHash : calendarHashes) {
                String calKey = calHash + Objects.requireNonNull(sdf_DDMMYYYY.get()).format(startPeriod.getTime()) + Objects.requireNonNull(sdf_DDMMYYYY.get()).format(endPeriod.getTime());
                if (!preferences_DaysTypes.containsKey(calKey)) {
                    for (String calId : preferences_HolidayEvent_calendars) {
                        if (StringUtils.getHash(Constants.eventSourceCalendarPrefix + calId).equals(calHash)) {
                            Log.i("CALENDAR", calId + Constants.STRING_PARENTHESIS_OPEN + calHash + Constants.STRING_PARENTHESIS_CLOSE);
                            if (calIDs.length() > 0)
                                calIDs.append(Constants.QUERY_PARAM_OR + CalendarContract.Events.CALENDAR_ID + Constants.SQL_EQUAL);
                            calIDs.append(calId);
                            break;
                        }
                    }
                    preferences_DaysTypes.put(calKey, DayType.Type.Holiday);
                }
            }
            if (calIDs.length() == 0) return;

            if (contentResolver == null) contentResolver = context.getContentResolver();
            String[] projection = {
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Instances.TITLE
            };
            ColumnIndexCache cache = new ColumnIndexCache();
            String selection = CalendarContract.Instances.CALENDAR_ID + Constants.SQL_EQUAL + calIDs;
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startPeriod.getTimeInMillis());
            ContentUris.appendId(builder, endPeriod.getTimeInMillis());

            Cursor cursor = contentResolver.query(
                    builder.build(),
                    projection,
                    selection,
                    null,
                    CalendarContract.Instances.BEGIN + Constants.SQL_SORT_ASC
            );
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    Log.i("EVENTS", String.valueOf(cursor.getCount()));

                    while (cursor.moveToNext()) {
                        Calendar dateStart = AppDateUtils.getCalendarFromDate(new Date(StringUtils.parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.BEGIN)))));
                        Calendar dateEnd = AppDateUtils.getCalendarFromDate(new Date(StringUtils.parseToLong(cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.END)))));

                        if (cursor.getInt(cache.getColumnIndex(cursor, CalendarContract.Events.ALL_DAY)) == 1) { //У AllDay событий зона всегда UTC
                            if (TimeZone.getDefault().getRawOffset() < 0) { //Для отрицательных зон надо прибавлять день
                                dateStart.add(Calendar.DATE, 1);
                            }
                            dateStart.set(Calendar.HOUR_OF_DAY, 0);
                            dateStart.set(Calendar.MINUTE, 0);
                            dateStart.set(Calendar.SECOND, 0);
                            dateStart.set(Calendar.MILLISECOND, 0);
                            dateEnd.set(Calendar.HOUR_OF_DAY, 0);
                            dateEnd.set(Calendar.MINUTE, 0);
                            dateEnd.set(Calendar.SECOND, 0);
                            dateEnd.set(Calendar.MILLISECOND, 0);
                            dateEnd.add(Calendar.SECOND, -1);
                        }
                        if (dateEnd.before(startPeriod)) continue;

                        final String calId = cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Events.CALENDAR_ID));
                        final String calHash = StringUtils.getHash(Constants.eventSourceCalendarPrefix + calId);
                        String eventTitle = Constants.eventTitleCalendarPrefix + cursor.getString(cache.getColumnIndex(cursor, CalendarContract.Instances.TITLE));

                        if (!AppDateUtils.isSameDay(dateStart, dateEnd)) {
                            final FormatDate formatDate = dateStart.get(Calendar.YEAR) == dateEnd.get(Calendar.YEAR) ? FormatDate.WithoutYear : FormatDate.WithYear;
                            eventTitle = eventTitle
                                    .concat(Constants.STRING_PARENTHESIS_OPEN)
                                    .concat(getDateFormatted(Objects.requireNonNull(sdf_DDMMYYYY.get()).format(dateStart.getTime()), formatDate))
                                    .concat(Constants.STRING_MINUS)
                                    .concat(getDateFormatted(Objects.requireNonNull(sdf_DDMMYYYY.get()).format(dateEnd.getTime()), formatDate))
                                    .concat(Constants.STRING_PARENTHESIS_CLOSE);
                        }

                        while (dateStart.before(startPeriod)) {
                            dateStart.add(Calendar.DATE, 1);
                        }
                        dateEnd.add(Calendar.SECOND, 1);

                        do {
                            String key = calHash.concat(Constants.STRING_COLON).concat(Objects.requireNonNull(sdf_java.get()).format(dateStart.getTime()));
                            preferences_DaysTypes.put(key, DayType.Type.Holiday);
                            String eventInfo = preferences_DaysInfo.get(key);
                            if (eventInfo != null) {
                                if (!eventInfo.contains(eventTitle)) {
                                    preferences_DaysInfo.put(key, eventInfo.concat(Constants.STRING_EOT).concat(eventTitle));
                                }
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

    /**
     * Удаляет из массивов информации о днях календаря данные по ключу
     *
     * @param hash Начало ключа
     */
    void clearDayInfoByHash(@NonNull String hash) {
        try {

            Map<String, String> updatedInfoMap = new HashMap<>();
            for (String key : preferences_DaysInfo.keySet()) {
                if (!key.startsWith(hash)) updatedInfoMap.put(key, preferences_DaysInfo.get(key));
            }
            if (updatedInfoMap.size() != preferences_DaysInfo.size()) {
                preferences_DaysInfo.clear();
                preferences_DaysInfo.putAll(updatedInfoMap);
            }
            updatedInfoMap.clear();

            Map<String, DayType.Type> updatedTypesMap = new HashMap<>();
            for (String key : preferences_DaysTypes.keySet()) {
                if (!key.startsWith(hash)) updatedTypesMap.put(key, preferences_DaysTypes.get(key));
            }
            if (updatedTypesMap.size() != preferences_DaysTypes.size()) {
                preferences_DaysTypes.clear();
                preferences_DaysTypes.putAll(updatedTypesMap);
            }
            updatedTypesMap.clear();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Возвращает цвет дня
     *
     * @param date Дата (yyyy-MM-dd)
     * @return Строка с цветом или пусто
     */
    @NonNull
    String getDayInfo(String date) {
        return StringUtils.getNotNullString(preferences_days_info.get(date));
    }

    /**
     * Сохраняет цвет дня
     *
     * @param date  Дата (yyyy-MM-dd)
     * @param value Значение или null (для удаления)
     */
    @SuppressLint("ApplySharedPref")
    void setDayInfo(String date, String value) {
        try {

            if (value == null) {
                preferences_days_info.remove(date);
            } else {
                preferences_days_info.put(date, value);
            }

            Set<String> values = new HashSet<>();
            for (String key : preferences_days_info.keySet()) {
                values.add(key + Constants.STRING_EOT + StringUtils.getNotNullString(preferences_days_info.get(key)));
            }

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putStringSet(context.getString(R.string.pref_DaysInfo_key), values);
            editor.commit();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Заполняет глобальный список фактов {@link ContactsEvents#eventListFacts}
     *
     * @param setCounters Увеличивать счётчики
     * @return True - ошибок заполнения не было
     */
    @SuppressLint("DiscouragedApi")
    protected boolean getFactsEvents(boolean setCounters) {
        try {

            if (preferences_FactEvent_ids.isEmpty() && preferences_FactEvent_files.isEmpty())
                return false;

            long statCurrentModuleStart = System.currentTimeMillis();
            List<String> factsBundled = new ArrayList<>();

            if (!preferences_FactEvent_ids.isEmpty()) {
                int eventsPackCount = 1;
                int packId = getResources().getIdentifier(Constants.STRING_TYPE_FACT + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
                while (packId > 0) {
                    try {

                        String[] eventsPack = getResources().getStringArray(packId);
                        int countEvents = eventsPack.length;
                        if (countEvents > 1) {
                            final String packHash = StringUtils.getHash(Constants.eventSourceFactPrefix + eventsPack[0]);
                            if (preferences_FactEvent_ids.contains(packHash)) {
                                for (int i = 1; i < countEvents; i++) {
                                    String eventsArray = eventsPack[i];
                                    String[] days = eventsArray.split(Constants.STRING_EOL, -1);
                                    for (String eventLine : days) {
                                        String fact = eventLine.trim();

                                        if (fact.isEmpty() || fact.startsWith(Constants.STRING_HASH) || fact.startsWith(Constants.STRING_DSLASH))
                                            continue;

                                        fact = fact.concat(Constants.STRING_EOT).concat(packHash);

                                        if (!factsBundled.contains(fact)) {
                                            factsBundled.add(fact);
                                            increaseStatForEventSources(StringUtils.substringBefore(Constants.eventSourceFactPrefix, Constants.STRING_COLON));
                                            increaseStatForEventSourcesIds(packHash);
                                            increaseStatForEventTypes(resources.getString(R.string.pref_EventTypes_Facts));
                                            //if (setCounters) statEventsCount++;
                                        }
                                    }
                                }
                            }
                        }

                    } catch (Resources.NotFoundException ignored) { /**/ }

                    eventsPackCount++;
                    packId = getResources().getIdentifier(Constants.STRING_TYPE_FACT + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
                }
            }

            List<String> factsFiles = new ArrayList<>();

            if (!preferences_FactEvent_files.isEmpty()) {
                for (String file : preferences_FactEvent_files) {

                    String[] fileDetails = file.split(Constants.REGEX_BAR);
                    String[] eventsArray = readFileToString(file, Constants.STRING_EOL).split(Constants.STRING_EOL, -1);
                    if (eventsArray[0].isEmpty()) {
                        ToastExpander.showInfoMsg(context, resources.getString(R.string.msg_file_open_error) + fileDetails[0]);
                        continue;
                    }
                    final String packHash = StringUtils.getHash(Constants.eventSourceFilePrefix + file);

                    for (String eventRow : eventsArray) {
                        String fact = eventRow.trim().replace("\uFEFF", Constants.STRING_EMPTY);

                        if (fact.isEmpty() || fact.startsWith(Constants.STRING_HASH) || fact.startsWith(Constants.STRING_DSLASH))
                            continue;

                        fact = fact.concat(Constants.STRING_EOT).concat(packHash);

                        if (!factsFiles.contains(fact)) {
                            factsFiles.add(fact);
                            increaseStatForEventSources(StringUtils.substringBefore(Constants.eventSourceFactPrefix, Constants.STRING_COLON));
                            increaseStatForEventSourcesIds(packHash);
                            increaseStatForEventTypes(resources.getString(R.string.pref_EventTypes_Facts));
                            if (setCounters) {
                                //statEventsCount++;
                                statFilesEventCount++;
                            }
                        }
                    }

                }
            }

            eventListFacts.clear();
            eventListFacts.addAll(factsBundled);
            eventListFacts.addAll(factsFiles);

            if (setCounters) {
                statTimeGetFactEvents += System.currentTimeMillis() - statCurrentModuleStart;
                statEventsCount += eventListFacts.size();
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    /**
     * Добавляет в общий список праздники из внутренних справочников
     *
     * @param packsHashes Хэши источников для получения событий
     * @param packPrefix  Префикс массива событий в ресурсах
     */
    @SuppressLint("DiscouragedApi")
    private boolean getHolidayEvents(@NonNull Set<String> packsHashes, @NonNull String packPrefix) {
        try {

            if (packsHashes.isEmpty()) return false;

            long statCurrentModuleStart = System.currentTimeMillis();

            // Кэшируем массив форматов
            final SimpleDateFormat[] dateFormats = {
                    Objects.requireNonNull(sdf_DDMMYYYY.get()),
                    Objects.requireNonNull(sdf_india.get()),
                    Objects.requireNonNull(sdf_uk.get()),
                    Objects.requireNonNull(sdf_java.get())
            };

            int eventsPackCount = 1;
            int packId = getResources().getIdentifier(packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            while (packId > 0) {
                try {

                    String[] eventsPack = getResources().getStringArray(packId);
                    int countEvents = eventsPack.length;
                    if (countEvents > 1) {
                        final String packHash = StringUtils.getHash(Constants.eventSourceHolidayPrefix + eventsPack[0]);
                        if (packsHashes.contains(packHash)) {

                            String eventEmoji = StringUtils.extractLeadingEmoji(eventsPack[0]);
                            if (!StringUtils.hasContent(eventEmoji)) {
                                eventEmoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_holiday_emoji) : "\uD83C\uDFD6️";
                            }

                            for (int i = 1; i < countEvents; i++) {
                                String eventsArray = eventsPack[i];
                                String[] days = eventsArray.split(Constants.STRING_EOL, -1);
                                List<String> expandedEvents = splitMultidayEventsAsSeparateLine(days, dateFormats);
                                for (String eventString : expandedEvents) {
                                    addFileEventFromLine(
                                            eventsPack[0],
                                            getResources().getString(R.string.msg_source_info, eventsPack[0]),
                                            eventString,
                                            Constants.EventType_Holiday,
                                            Constants.PREFIX_HolidayEventID,
                                            Constants.EVENT_PREFIX_EMBEDDED_EVENT,
                                            Constants.eventSourceHolidayPrefix,
                                            Constants.STRING_STORAGE_EMBEDDED,
                                            eventEmoji,
                                            getToday()
                                    );
                                }
                            }
                        }
                    }

                } catch (Resources.NotFoundException ignored) { /**/ }

                eventsPackCount++;
                packId = getResources().getIdentifier(packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            }

            statTimeGetHolidayEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    /**
     * Добавляет в общий список другие события из внутренних справочников
     *
     * @param packsHashes Хэши источников для получения событий
     * @param packPrefix  Префикс массива событий в ресурсах
     */
    @SuppressWarnings("SameParameterValue")
    @SuppressLint("DiscouragedApi")
    private boolean getOtherEvents(@NonNull Set<String> packsHashes, @NonNull String packPrefix) {
        try {

            if (packsHashes.isEmpty()) return false;

            long statCurrentModuleStart = System.currentTimeMillis();

            // Кэшируем массив форматов
            final SimpleDateFormat[] dateFormats = {
                    Objects.requireNonNull(sdf_DDMMYYYY.get()),
                    Objects.requireNonNull(sdf_india.get()),
                    Objects.requireNonNull(sdf_uk.get()),
                    Objects.requireNonNull(sdf_java.get())
            };

            int eventsPackCount = 1;
            int packId = getResources().getIdentifier(packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            while (packId > 0) {
                try {

                    String[] eventsPack = getResources().getStringArray(packId);
                    int countEvents = eventsPack.length;
                    if (countEvents > 1) {
                        final String packHash = StringUtils.getHash(Constants.eventSourceOtherEventPrefix + eventsPack[0]);
                        if (packsHashes.contains(packHash)) {

                            String eventEmoji = StringUtils.extractLeadingEmoji(eventsPack[0]);
                            if (!StringUtils.hasContent(eventEmoji)) {
                                eventEmoji = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getResources().getString(R.string.event_type_other_emoji) : "\uD83D\uDCC6";
                            }

                            for (int i = 1; i < countEvents; i++) {
                                String eventsArray = eventsPack[i];
                                String[] days = eventsArray.split(Constants.STRING_EOL, -1);
                                List<String> expandedEvents = splitMultidayEventsAsSeparateLine(days, dateFormats);
                                for (String eventString : expandedEvents) {
                                    addFileEventFromLine(
                                            eventsPack[0],
                                            getResources().getString(R.string.msg_source_info, eventsPack[0]),
                                            eventString,
                                            Constants.EventType_Other,
                                            Constants.PREFIX_OtherEventID,
                                            Constants.EVENT_PREFIX_EMBEDDED_EVENT,
                                            Constants.eventSourceOtherEventPrefix,
                                            Constants.STRING_STORAGE_EMBEDDED,
                                            eventEmoji,
                                            getToday()
                                    );
                                }
                            }
                        }
                    }

                } catch (Resources.NotFoundException ignored) { /**/ }

                eventsPackCount++;
                packId = getResources().getIdentifier(packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, context.getPackageName());
            }

            statTimeGetHolidayEvents += System.currentTimeMillis() - statCurrentModuleStart;

            return true;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    class EventSources {
        public EventSources() {
        }

        public List<String> getIds() {
            return ids;
        }

        public List<String> getTitles() {
            return titles;
        }

        public List<String> getPackages() {
            return packages;
        }

        public List<String> getHashes() {
            return hashes;
        }

        public List<Integer> getIcons() {
            return icons;
        }

        private final List<String> ids = new ArrayList<>();
        private final List<String> titles = new ArrayList<>();
        private final List<String> packages = new ArrayList<>();
        private final List<String> hashes = new ArrayList<>();
        private final List<Integer> icons = new ArrayList<>();

        private class Source {
            final String packPrefix;
            final String eventIdHashPrefix;
            @DrawableRes
            final int eventIcon;
            final Set<String> prefSelected;

            public Source(@NonNull String packPrefix, @NonNull String eventIdHashPrefix,
                          @DrawableRes int eventIcon, @NonNull Set<String> prefSelected) {
                this.packPrefix = packPrefix;
                this.eventIdHashPrefix = eventIdHashPrefix;
                this.eventIcon = eventIcon;
                this.prefSelected = prefSelected;
            }
        }

        @SuppressLint("DiscouragedApi")
        void loadEventSources(String eventConsumer) {
            try {

                ids.clear();
                titles.clear();
                packages.clear();
                hashes.clear();
                icons.clear();
                String packageName = context.getPackageName();

                //Локальные события
                titles.add(
                        getResources().getString(R.string.msg_title_local_events)
                                + Constants.STRING_BRACKETS_OPEN
                                + getLocalEventsCount()
                                + Constants.STRING_BRACKETS_CLOSE);
                ids.add(Constants.eventSourceLocalPrefix);
                icons.add(android.R.drawable.ic_menu_add);
                packages.add(packageName);
                hashes.add(StringUtils.getHash(Constants.eventSourceLocalPrefix));

                //Справочники праздников и выходных и других событий
                final ArrayList<Source> sources = new ArrayList<>();
                sources.add(new Source(Constants.STRING_TYPE_HOLIDAY, Constants.eventSourceHolidayPrefix,
                        R.drawable.ic_event_holiday, preferences_HolidayEvent_ids));
                sources.add(new Source(Constants.STRING_TYPE_OTHER_HOLIDAY, Constants.eventSourceHolidayPrefix,
                        R.drawable.ic_event_holiday, preferences_HolidayEvent_Other_ids));
                sources.add(new Source(Constants.STRING_TYPE_OTHER_EVENT, Constants.eventSourceOtherEventPrefix,
                        R.drawable.ic_event_other, preferences_OtherEvent_ids));

                for (Source source: sources) {
                    int eventsPackCount = 1;
                    int packId = getResources().getIdentifier(source.packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, packageName);
                    while (packId > 0) {
                        try {
                            String[] eventsPack = getResources().getStringArray(packId);
                            String packHash = StringUtils.getHash(source.eventIdHashPrefix + eventsPack[0]);

                            if (source.prefSelected.contains(packHash)) {
                                ids.add(source.eventIdHashPrefix + eventsPack[0]);
                                String sourceTitle = eventsPack[0];
                                if (statEventSourcesIds.containsKey(packHash)) {
                                    sourceTitle = sourceTitle
                                            + Constants.STRING_BRACKETS_OPEN
                                            + statEventSourcesIds.get(packHash)
                                            + Constants.STRING_BRACKETS_CLOSE;
                                }
                                titles.add(sourceTitle);
                                icons.add(source.eventIcon);
                                packages.add(packageName);
                                hashes.add(StringUtils.getHash(source.eventIdHashPrefix + eventsPack[0]));
                            }

                        } catch (Resources.NotFoundException ignored) { /**/ }

                        eventsPackCount++;
                        packId = getResources().getIdentifier(source.packPrefix + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, packageName);
                    }
                }

                if (eventConsumer.equals(resources.getString(R.string.pref_Notifications_EventSources_key))
                        || eventConsumer.equals(resources.getString(R.string.pref_Notifications2_EventSources_key))
                        || eventConsumer.equals(Constants.WIDGET_TYPE_LIST)) {

                    //Факты
                    int eventsPackCount = 1;
                    int packId = getResources().getIdentifier(Constants.STRING_TYPE_FACT + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, packageName);
                    while (packId > 0) {
                        try {
                            String[] eventsPack = getResources().getStringArray(packId);
                            String packHash = StringUtils.getHash(Constants.eventSourceFactPrefix + eventsPack[0]);

                            if (preferences_FactEvent_ids.contains(packHash)) {
                                ids.add(Constants.eventSourceFactPrefix + eventsPack[0]);
                                String sourceTitle = eventsPack[0];
                                if (statEventSourcesIds.containsKey(packHash)) {
                                    sourceTitle = sourceTitle
                                            + Constants.STRING_BRACKETS_OPEN
                                            + statEventSourcesIds.get(packHash)
                                            + Constants.STRING_BRACKETS_CLOSE;
                                }
                                titles.add(sourceTitle);
                                icons.add(R.drawable.ic_event_fact);
                                packages.add(packageName);
                                hashes.add(StringUtils.getHash(Constants.eventSourceFactPrefix + eventsPack[0]));
                            }

                        } catch (Resources.NotFoundException ignored) { /**/ }

                        eventsPackCount++;
                        packId = getResources().getIdentifier(Constants.STRING_TYPE_FACT + eventsPackCount, Constants.RES_TYPE_STRING_ARRAY, packageName);
                    }
                }


                if (!DeviceTools.checkNoContactsAccess(ContactsEvents.this.context)) {
                    final Set<String> preferences_accounts = getPreferences_Accounts();
                    AuthenticatorDescription[] descriptions = AccountManager.get(context).getAuthenticatorTypes();

                    //Online аккаунты
                    Account[] accounts = AccountManager.get(context).getAccounts();
                    for (Account account : accounts) {
                        final String accountName = account.name + Constants.STRING_PARENTHESIS_OPEN + account.type + Constants.STRING_PARENTHESIS_CLOSE;
                        if (preferences_accounts.isEmpty() || preferences_accounts.contains(accountName)) {
                            for (AuthenticatorDescription desc : descriptions) {
                                if (account.type.equals(desc.type)) {
                                    String eventId = Constants.eventSourceContactPrefix + accountName;
                                    ids.add(eventId);
                                    titles.add(accountName);
                                    icons.add(desc.iconId > 0 ? desc.iconId : desc.smallIconId);
                                    packages.add(desc.packageName);
                                    hashes.add(StringUtils.getHash(eventId));
                                    break;
                                }
                            }
                        }
                    }

                    //Raw аккаунты
                    ContentResolver contentResolver = context.getContentResolver();
                    Cursor cursor = contentResolver.query(
                            ContactsContract.RawContacts.CONTENT_URI,
                            new String[]{ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE},
                            Constants.QUERY_PARAM_DELETED_0,
                            null,
                            null
                    );
                    if (cursor != null && cursor.getCount() > 0) {
                        if (cursor.moveToFirst()) {
                            final int indexNameColumn = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME);
                            final int indexTypeColumn = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE);
                            do {
                                String sysAccountName = cursor.getString(indexNameColumn);
                                if (sysAccountName == null)
                                    sysAccountName = resources.getString(R.string.account_type_local);
                                String accountName = sysAccountName + Constants.STRING_PARENTHESIS_OPEN
                                        + cursor.getString(indexTypeColumn) + Constants.STRING_PARENTHESIS_CLOSE;
                                if (!titles.contains(accountName)) {
                                    if (preferences_accounts.isEmpty() || preferences_accounts.contains(accountName)) {
                                        String eventId = Constants.eventSourcePhonePrefix + accountName;
                                        ids.add(eventId);
                                        titles.add(accountName);
                                        if (eventId.toLowerCase().contains(Constants.account_sim)) {
                                            icons.add(R.drawable.sim_card);
                                        } else {
                                            icons.add(R.drawable.emo_im_happy);
                                        }
                                        packages.add(packageName);
                                        hashes.add(StringUtils.getHash(eventId));
                                    }
                                }
                            } while (cursor.moveToNext());
                            cursor.close();
                        }
                    }
                }

                //Календари
                if (!DeviceTools.checkNoCalendarAccess(ContactsEvents.this.context)) {
                    if (map_calendars.isEmpty())
                        AppDateUtils.fillCalendarList(ContactsEvents.this.context, ContactsEvents.this.map_calendars, ContactsEvents.this.map_calendars_colors);
                    List<String> allCalendars = new ArrayList<>();
                    allCalendars.addAll(preferences_MultiType_calendars);
                    allCalendars.addAll(preferences_BirthDay_calendars);
                    allCalendars.addAll(preferences_OtherEvent_calendars);
                    allCalendars.addAll(preferences_HolidayEvent_calendars);
                    if (!allCalendars.isEmpty()) {
                        for (String calendar : allCalendars) {
                            if (map_calendars.containsKey(calendar)) {
                                titles.add(StringUtils.substringBefore(map_calendars.get(calendar), Constants.STRING_EOT));
                                ids.add(Constants.eventSourceCalendarPrefix + calendar);
                                icons.add(android.R.drawable.ic_menu_month);
                                packages.add(packageName);
                                hashes.add(StringUtils.getHash(Constants.eventSourceCalendarPrefix + calendar));
                            }
                        }
                    }
                }

                //Файлы
                if (!preferences_MultiType_files.isEmpty()) {
                    for (String file : preferences_MultiType_files) {
                        ids.add(Constants.eventSourceMultiFilePrefix + file);
                        titles.add(StringUtils.substringBefore(file, Constants.STRING_BAR));
                        icons.add(android.R.drawable.ic_menu_save);
                        packages.add(packageName);
                        hashes.add(StringUtils.getHash(Constants.eventSourceMultiFilePrefix + file));
                    }
                }
                if (!preferences_Birthday_files.isEmpty()) {
                    for (String file : preferences_Birthday_files) {
                        ids.add(Constants.eventSourceFilePrefix + file);
                        titles.add(StringUtils.substringBefore(file, Constants.STRING_BAR));
                        icons.add(android.R.drawable.ic_menu_save);
                        packages.add(packageName);
                        hashes.add(StringUtils.getHash(Constants.eventSourceFilePrefix + file));
                    }
                }
                if (!preferences_OtherEvent_files.isEmpty()) {
                    for (String file : preferences_OtherEvent_files) {
                        ids.add(Constants.eventSourceFilePrefix + file);
                        titles.add(StringUtils.substringBefore(file, Constants.STRING_BAR));
                        icons.add(android.R.drawable.ic_menu_save);
                        packages.add(packageName);
                        hashes.add(StringUtils.getHash(Constants.eventSourceFilePrefix + file));
                    }
                }
                if (!preferences_HolidayEvent_files.isEmpty()) {
                    //Праздники
                    for (String file : preferences_HolidayEvent_files) {
                        ids.add(Constants.eventSourceFilePrefix + file);
                        titles.add(StringUtils.substringBefore(file, Constants.STRING_BAR));
                        icons.add(android.R.drawable.ic_menu_save);
                        packages.add(packageName);
                        hashes.add(StringUtils.getHash(Constants.eventSourceFilePrefix + file));
                    }
                }
                if (eventConsumer.equals(resources.getString(R.string.pref_Notifications_EventSources_key))
                        || eventConsumer.equals(resources.getString(R.string.pref_Notifications2_EventSources_key))
                        || eventConsumer.equals(Constants.WIDGET_TYPE_LIST)) {
                    //Факты
                    if (!preferences_FactEvent_files.isEmpty()) {
                        for (String file : preferences_FactEvent_files) {
                            ids.add(Constants.eventSourceFilePrefix + file);
                            titles.add(StringUtils.substringBefore(file, Constants.STRING_BAR));
                            icons.add(android.R.drawable.ic_menu_save);
                            packages.add(packageName);
                            hashes.add(StringUtils.getHash(Constants.eventSourceFilePrefix + file));
                        }
                    }
                }

            } catch (final Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }
        }

    }

    /**
     * Интерфейс обратного вызова для выбора источников событий
     */
    public interface OnEventSourcesSelectedListener {
        void onEventSourcesSelected(@NonNull List<String> selectedSources);
    }

    /**
     * Диалог выбора источников событий
     *
     * @param eventSources       Доступные источники событий
     * @param preselectedSources Предвыбранные источники
     * @param baseContext        Контекст вызова (для наследования темы и возврата результата)
     * @param listener           Интерфейс обратного вызова для выбора источников событий
     */
    void selectEventSources(@NonNull EventSources eventSources, @NonNull List<String> preselectedSources, @NonNull Context baseContext, @Nullable OnEventSourcesSelectedListener listener) {
        //todo: переделать последний параметр на Runnable, как в selectQuizQuestions
        final List<String> eventSourcesSelected = new ArrayList<>();
        try {

            if (eventSources.getIds().isEmpty()) return;

            TypedArray ta = baseContext.getTheme().obtainStyledAttributes(R.styleable.Theme);
            final List<String> sourceChoices = new ArrayList<>();
            boolean isAdvInfo = isFeatureEnabled(Constants.FEATURE_ADV_INFO);

            for (int i = 0; i < eventSources.getIds().size(); i++) {
                String sourceId = eventSources.getIds().get(i);
                String sourceTitle = eventSources.getTitles().get(i);

                if (!isAdvInfo) {

                    sourceChoices.add(StringUtils.substringBefore(sourceTitle, Constants.STRING_BRACKETS_OPEN));

                } else if (sourceId.startsWith(Constants.eventSourceContactPrefix)) {

                    final String accountType = StringUtils.substringBetween(sourceId, Constants.STRING_PARENTHESIS_OPEN, Constants.STRING_PARENTHESIS_CLOSE);
                    sourceChoices.add(sourceTitle
                            + Constants.STRING_BRACKETS_OPEN
                            + getContactsEventsCount(accountType, StringUtils.substringBefore(sourceTitle, Constants.STRING_PARENTHESIS_OPEN))
                            + Constants.STRING_BRACKETS_CLOSE);

                } else if (sourceId.startsWith(Constants.eventSourcePhonePrefix)) {

                    final String accountType = StringUtils.substringBetween(sourceId, Constants.STRING_PARENTHESIS_OPEN, Constants.STRING_PARENTHESIS_CLOSE);
                    sourceChoices.add(sourceTitle
                            + Constants.STRING_BRACKETS_OPEN
                            + getContactsEventsCount(accountType, null)
                            + Constants.STRING_BRACKETS_CLOSE);

                } else if (sourceId.startsWith(Constants.eventSourceCalendarPrefix)) {

                    sourceChoices.add(sourceTitle
                            + Constants.STRING_BRACKETS_OPEN
                            + getCalendarEventsCount(StringUtils.substringAfter(sourceId, Constants.eventSourceCalendarPrefix))
                            + Constants.STRING_BRACKETS_CLOSE);

                } else if (sourceId.startsWith(Constants.eventSourceFilePrefix)) {

                    sourceChoices.add(sourceTitle
                            + Constants.STRING_BRACKETS_OPEN
                            //todo: тут жертвуем типом события в пользу "фактов" и скорости
                            + getFileEventsCount(sourceId, Constants.EventType_Fact, false)
                            + Constants.STRING_BRACKETS_CLOSE);

                } else if (sourceId.startsWith(Constants.eventSourceMultiFilePrefix)) {

                    sourceChoices.add(sourceTitle
                            + Constants.STRING_BRACKETS_OPEN
                            + getFileEventsCount(sourceId, Constants.Type_MultiEvent, true)
                            + Constants.STRING_BRACKETS_CLOSE);

                } else {

                    //количество событий входит в заголовок
                    sourceChoices.add(sourceTitle);

                }
            }

            ListAdapter adapter = new MultiCheckboxesAdapter(baseContext, sourceChoices, null,
                    eventSources.getIcons(), eventSources.getPackages(), null, ta);

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(baseContext, preferences_theme.themeDialog))
                    .setTitle(R.string.widget_config_events_sources_label)
                    .setIcon(R.drawable.btn_zoom_page_press)
                    .setAdapter(adapter, null)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                        //https://stackoverflow.com/questions/8326830/how-to-uncheck-item-checked-by-setitemchecked
                        SparseBooleanArray checked = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                        for (int i = 0; i < checked.size(); i++) {
                            if (checked.get(checked.keyAt(i))) {
                                eventSourcesSelected.add(eventSources.getHashes().get(checked.keyAt(i)));
                            }
                        }

                        if (listener != null) {
                            try {
                                listener.onEventSourcesSelected(eventSourcesSelected);
                            } catch (Exception e) {
                                Log.e(TAG, "Error in event sources listener", e);
                            }
                        }

                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setNeutralButton(R.string.msg_all, null)
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();

            ListView listView = alertToShow.getListView();
            listView.setItemsCanFocus(false);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            // 👇 Создаем InsetDrawable с отступами по бокам (5% с каждой стороны = 90% ширина)
            int dividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
            int insetWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.05); // 5% с каждой стороны
            Drawable divider = new InsetDrawable(
                    new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)),
                    insetWidth, 0, insetWidth, 0);
            listView.setDivider(divider);
            listView.setDividerHeight(dividerHeight);

            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                //Только здесь работает
                for (int i = 0; i < eventSources.getHashes().size(); i++) {
                    if (preselectedSources.contains(eventSources.getHashes().get(i))) {
                        listView.setItemChecked(i, true);
                    }
                }

                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> listView.post(() -> {
                    if (listView.getCheckedItemCount() < listView.getCount()) {
                        for (int i = 0; i < listView.getCount(); i++) {
                            listView.setItemChecked(i, true);
                        }
                    } else {
                        listView.clearChoices();
                    }
                    listView.invalidateViews();
                }));
            });

            alertToShow.setOnDismissListener(dialog -> ta.recycle());
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(baseContext, getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Показывает диалог выбора типов вопросов
     *
     * @param baseContext контекст
     * @param onConfirmed коллбэк, который выполнится при нажатии "ОК"
     */
    void selectQuizQuestions(@NonNull Context baseContext, @Nullable Runnable onConfirmed) {
        try {

            TypedArray ta = baseContext.getTheme().obtainStyledAttributes(R.styleable.Theme);

            List<String> questTitles = new ArrayList<>();
            List<String> questIds = new ArrayList<>();
            List<Integer> questIcons = new ArrayList<>();
            List<String> questIconsPackages = new ArrayList<>();
            String packageName = context.getPackageName();

            for (QuizActivity.QuestionType type : QuizActivity.QuestionType.values()) {
                questTitles.add(type.getDisplayName(context));
                questIds.add(type.getCode(context));
                questIcons.add(type.getIcon());
                questIconsPackages.add(packageName);
            }

            ListAdapter adapter = new MultiCheckboxesAdapter(baseContext, questTitles, null, questIcons, questIconsPackages, null, ta);

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(baseContext, preferences_theme.themeDialog))
                    .setTitle(R.string.pref_Quiz_Questions_title)
                    .setIcon(android.R.drawable.ic_menu_agenda)
                    .setAdapter(adapter, null)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                        List<String> questIdsSelected = new ArrayList<>();
                        SparseBooleanArray checked = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                        for (int i = 0; i < checked.size(); i++) {
                            if (checked.get(checked.keyAt(i))) {
                                questIdsSelected.add(questIds.get(checked.keyAt(i)));
                            }
                        }
                        preferences_quiz_questions.clear();
                        preferences_quiz_questions.addAll(questIdsSelected);
                        savePreferences();

                        // 👇 Вызываем коллбэк, если передан
                        if (onConfirmed != null) {
                            onConfirmed.run();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setNeutralButton(R.string.msg_all, null)
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();

            ListView listView = alertToShow.getListView();
            listView.setItemsCanFocus(false);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            alertToShow.setOnShowListener(arg0 -> {

                //Только здесь работает
                int i = 0;
                for (QuizActivity.QuestionType type : QuizActivity.QuestionType.values()) {
                    if (preferences_quiz_questions.contains(type.getCode(context))) {
                        listView.setItemChecked(i, true);
                    }
                    i++;
                }

                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> listView.post(() -> {
                    if (listView.getCheckedItemCount() < listView.getCount()) {
                        for (int item = 0; item < listView.getCount(); item++) {
                            listView.setItemChecked(item, true);
                        }
                    } else {
                        listView.clearChoices();
                    }
                    listView.invalidateViews();
                }));

            });

            alertToShow.setOnDismissListener(dialog -> ta.recycle());
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    int getDefaultAligningForEventInfo(@NonNull String info) {
        if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecond))) { //Фамилия Имя Отчество
            return Constants.Align_Left;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventDate))) { //Дата события
            return Constants.Align_Center;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFS))) { //Фамилия И.О. (Имя Отчество, если нет фамилии)
            return Constants.Align_Center;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_FirstSecondLast))) { //Имя Отчество Фамилия
            return Constants.Align_Left;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_First))) { //Имя
            return Constants.Align_Center;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Last))) { //Фамилия
            return Constants.Align_Center;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Nick))) { //Псевдоним (Имя, если отсутствует)
            return Constants.Align_Center;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventType))) { //Тип события
            return Constants.Align_Left;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventLabel))) { //Наименование события
            return Constants.Align_Left;
        } else if (info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Organization))) { //Организация (Должность, если отсутствует)
            return Constants.Align_Center;
        }
        return Constants.Align_Left;
    }

    /**
     * Сохраняет факт в список недавно показанных
     *
     * @param factToSave Факт
     */
    void saveRecentFact(@NonNull String factToSave) {
        try {

            if (!StringUtils.hasContent(factToSave)) return;

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            LinkedHashSet<String> currentList;
            String storedString = getPreferenceString(preferences, context.getString(R.string.pref_Facts_Recent_key), Constants.STRING_EMPTY);
            if (storedString.isEmpty()) {
                currentList = new LinkedHashSet<>();
            } else {
                String[] parts = storedString.split(Constants.STRING_EOT);
                currentList = new LinkedHashSet<>(Arrays.asList(parts));
            }
            currentList.add(factToSave);

            while (currentList.size() > Constants.RECENT_FACTS_MAX) {
                Iterator<String> iterator = currentList.iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }

            String joinedString = TextUtils.join(Constants.STRING_EOT, currentList);
            preferences.edit().putString(context.getString(R.string.pref_Facts_Recent_key), joinedString).apply();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    ArrayList<String> getRecentFacts() {
        ArrayList<String> factsList = new ArrayList<>();

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String storedString = getPreferenceString(preferences, context.getString(R.string.pref_Facts_Recent_key), Constants.STRING_EMPTY);
            if (!storedString.isEmpty()) {
                String[] parts = storedString.split(Constants.STRING_EOT);
                factsList.addAll(Arrays.asList(parts));
            }

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return factsList;
    }

    public void shutdown() {
        if (widgetUpdateExecutor != null) {
            widgetUpdateExecutor.shutdown(); // Завершает плавно
            try {
                if (!widgetUpdateExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    widgetUpdateExecutor.shutdownNow(); // Принудительно завершает
                }
            } catch (InterruptedException e) {
                widgetUpdateExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (eventsExecutor != null) {
            eventsExecutor.shutdown();
        }
    }

}