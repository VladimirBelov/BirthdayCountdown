package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.Toast;

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

@SuppressWarnings("ConstantConditions")
class ContactsEvents {
    @SuppressLint("StaticFieldLeak")
    private static final ContactsEvents ourInstance = new ContactsEvents();


    static ContactsEvents getInstance() {
        return ourInstance;
    }

    private ContactsEvents() {
    }

    //Разделители
    static final String Div1 = "###";
    static final String Div2 = "~~~";
    private static final String Div3 = ": ";
    static final String Div4 = ",";
    static final String Div5 = ", ";

    //Константы
    final private Set<String> prefs_EventTypes_Default = new HashSet<String>() {{
        add(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY + "");
        add(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY + "");
        add(ContactsContract.CommonDataKinds.Event.TYPE_OTHER + "");
        add(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM + "");
    }};
    final private boolean[] prefs_EventTypes_DefaultB = {true,true,true,true,false};

    final private Set<String> pref_List_Bottom_Info = new HashSet<String>() {{
        add("1");
        add("2");
        add("3");
    }};

    static final Map<String, Integer> dataMap = new HashMap<>();
    static {
        dataMap.put("eventDate_sorted", 0);
        dataMap.put("fio", 1);
        dataMap.put("eventLabel", 2);
        dataMap.put("eventType", 3);
        dataMap.put("dates", 4); //account_type: data1
        dataMap.put("eventDate", 5);
        dataMap.put("eventDistance", 6);
        dataMap.put("eventDistanceText", 7);
        dataMap.put("age", 8);
        dataMap.put("age_caption", 9);
        dataMap.put("organization", 10);
        dataMap.put("title", 11);
        dataMap.put("note", 12);
        dataMap.put("photo_uri", 13);
        dataMap.put("contact_id", 14);
        dataMap.put("eventIcon", 15);
        dataMap.put("lastName", 16);
        dataMap.put("secondName", 17);
        dataMap.put("firstName", 18);
    }

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
    //private final String typeCustom = Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM);

    //Хранилища данных
    String[] dataArray = null; //todo: переделать на объект или хотя бы ArrayList
    private boolean[] event_types_on;
    private String currentLocale = "";
    int currentTheme = 0;
    final private String systemLocale = Locale.getDefault().getLanguage();

    //Настройки
    private String preferences_language;

    Set<String> preferences_list_bottom_info;

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
    Matcher preferences_death_labels;
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

    class MyTheme {
        int prefNumber; //Номер в shared preferences
        int themeMain; //Тема основной активности
        int themePopup; //Тема вплывающего меню
        int themeDialog; //Тема диалогов
    }
    MyTheme preferences_theme;


    //Статистика
    long statGetContacts = 0;
    long statComputeDates = 0;
    long statDrawList = 0;
    long statLastComputeDates = 0;

    //UI объекты
    Context context;

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
            throw new IllegalArgumentException("Year must be > 0.");
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

    private static Calendar from(Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c;
    }

    private static Calendar removeTime(Calendar c) {

        c.set(HOUR_OF_DAY, 0);
        c.set(MINUTE, 0);
        c.set(SECOND, 0);
        c.set(MILLISECOND, 0);

        return c;
    }

    private int countDaysDiff(Date date1, Date date2) {
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

            if (isNegative) {
                return -(d2 + ((y2 - y1) * 365) - d1 + countLeapYearsBetween(y1, y2));
            } else {
                return d2 + ((y2 - y1) * 365) - d1 + countLeapYearsBetween(y1, y2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this, "MainActivity->countDaysDiff error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    private int countYearsDiff(Date date1, Date date2) {
        try {

            Calendar c1 = removeTime(from(date1));
            Calendar c2 = removeTime(from(date2));

            return c2.get(YEAR) - c1.get(YEAR);

        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this, "MainActivity->countYearsDiff error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
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
            Set<String> savedTypes;
            try {
                savedTypes = preferences.getStringSet(context.getString(R.string.pref_Events_key), prefs_EventTypes_Default);
            } catch (ClassCastException e) {
                savedTypes = prefs_EventTypes_Default;
            }

            if (savedTypes != null) {
                for (int i = 0; i < event_types_on.length; i++) {
                    event_types_on[i] = savedTypes.contains(event_types_id[i] + "");
                }
            }

            try {
                preferences_list_bottom_info = preferences.getStringSet(context.getString(R.string.pref_List_BottomInfo_key), pref_List_Bottom_Info);
            } catch (ClassCastException e) {
                preferences_list_bottom_info = pref_List_Bottom_Info;
            }
            preferences_language = preferences.getString(context.getString(R.string.pref_Language_key), context.getString(R.string.pref_Language_default));
            preferences_widgets_bottom_info = preferences.getString(context.getString(R.string.pref_Widgets_BottomInfo_key), context.getString(R.string.pref_Widgets_BottomInfo_default));
            preferences_widgets_color_eventtoday = preferences.getString(context.getString(R.string.pref_Widgets_Color_EventToday_key), context.getString(R.string.pref_Widgets_Color_EventToday_default));
            preferences_widgets_color_eventsoon = preferences.getString(context.getString(R.string.pref_Widgets_Color_EventSoon_key), context.getString(R.string.pref_Widgets_Color_EventSoon_default));
            preferences_widgets_contactsphotos = preferences.getBoolean(context.getString(R.string.pref_Widgets_ContactPhotos_key), Boolean.getBoolean(context.getString(R.string.pref_Widgets_ContactPhotos_default)));
            preferences_widgets_eventicons = preferences.getBoolean(context.getString(R.string.pref_Widgets_EventIcons_key), Boolean.getBoolean(context.getString(R.string.pref_Widgets_EventIcons_default)));
            preferences_widgets_days_eventsoon = Integer.parseInt(preferences.getString(context.getString(R.string.pref_Widgets_Days_EventSoon_key), context.getString(R.string.pref_Widgets_Days_EventSoon_default)));

            boolean useInternal;
            String customLabels;
            final String regex_inter = "\\Z|";
            final String regex_last = "\\Z";

            //Определения событий

            //День рождения
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Birthday_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Birthday_Labels_key), "");
            if (!useInternal && customLabels.equals("")) {
                preferences_birthday_labels = null;
            } else {
                if (customLabels.equals(""))
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).replace(Div4, regex_inter) + regex_last).matcher("");
                else if (!useInternal) {
                    preferences_birthday_labels = Pattern.compile(customLabels.replace(Div4, regex_inter) + regex_last).matcher("");
                } else {
                    preferences_birthday_labels = Pattern.compile(context.getString(R.string.event_type_birthday_labels).concat(Div4).concat(customLabels).replace(Div4, regex_inter) + regex_last).matcher("");
                }
            }

            //Свадьба
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Anniversary_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Anniversary_Labels_key), "");
            if (!useInternal && customLabels.equals("")) {
                preferences_wedding_labels = null;
            } else {
                if (customLabels.equals("")) {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).replace(Div4, regex_inter) + regex_last).matcher("");
                } else if (!useInternal) {
                    preferences_wedding_labels = Pattern.compile(customLabels.replace(Div4, regex_inter) + regex_last).matcher("");
                } else {
                    preferences_wedding_labels = Pattern.compile(context.getString(R.string.event_type_wedding_labels).concat(Div4).concat(customLabels).replace(Div4, regex_inter) + regex_last).matcher("");
                }
            }

            //Именины
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_NameDay_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_NameDay_Labels_key), "");
            if (!useInternal && customLabels.equals("")) {
                preferences_nameday_labels = null;
            } else {
                if (customLabels.equals("")) {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).replace(Div4, regex_inter) + regex_last).matcher("");
                } else if (!useInternal) {
                    preferences_nameday_labels = Pattern.compile(customLabels.replace(Div4, regex_inter) + regex_last).matcher("");
                } else {
                    preferences_nameday_labels = Pattern.compile(context.getString(R.string.event_type_nameday_labels).concat(Div4).concat(customLabels).replace(Div4, regex_inter) + regex_last).matcher("");
                }
            }

            //Венчание
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Crowning_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Crowning_Labels_key), "");
            if (!useInternal && customLabels.equals("")) {
                preferences_crowning_labels = null;
            } else {
                if (customLabels.equals("")) {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).replace(Div4, regex_inter) + regex_last).matcher("");
                } else if (!useInternal) {
                    preferences_crowning_labels = Pattern.compile(customLabels.replace(Div4, regex_inter) + regex_last).matcher("");
                } else {
                    preferences_crowning_labels = Pattern.compile(context.getString(R.string.event_type_crowning_labels).concat(Div4).concat(customLabels).replace(Div4, regex_inter) + regex_last).matcher("");
                }
            }

            //Годовщина смерти
            useInternal = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_Death_UseInternal_default)));
            customLabels = preferences.getString(context.getString(R.string.pref_CustomEvents_Death_Labels_key), "");
            if (!useInternal && customLabels.equals("")) {
                preferences_death_labels = null;
            } else {
                if (customLabels.equals("")) {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).replace(Div4, regex_inter) + regex_last).matcher("");
                } else if (!useInternal) {
                    preferences_death_labels = Pattern.compile(customLabels.replace(Div4, regex_inter) + regex_last).matcher("");
                } else {
                    preferences_death_labels = Pattern.compile(context.getString(R.string.event_type_death_labels).concat(Div4).concat(customLabels).replace(Div4, regex_inter) + regex_last).matcher("");
                }
            }

            //Пользовательские события
            //1
            preferences_customevent1_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom1_Caption_key), "").trim();
            preferences_customevent1_enabled = false;

            if (!preferences_customevent1_caption.equals("")) {
                String preferences_customevent1_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom1_Labels_key), "").toLowerCase();
                if (!preferences_customevent1_labels_str.equals("")) {
                    try {
                        preferences_customevent1_labels = Pattern.compile(preferences_customevent1_labels_str.replace(Div4, regex_inter) + regex_last).matcher("");
                        preferences_customevent1_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent1_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom1_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //2
            preferences_customevent2_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom2_Caption_key), "").trim();
            preferences_customevent2_enabled = false;

            if (!preferences_customevent2_caption.equals("")) {
                String preferences_customevent2_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom2_Labels_key), "").toLowerCase();
                if (!preferences_customevent2_labels_str.equals("")) {
                    try {
                        preferences_customevent2_labels = Pattern.compile(preferences_customevent2_labels_str.replace(Div4, regex_inter) + regex_last).matcher("");
                        preferences_customevent2_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent2_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom2_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //3
            preferences_customevent3_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom3_Caption_key), "").trim();
            preferences_customevent3_enabled = false;

            if (!preferences_customevent3_caption.equals("")) {
                String preferences_customevent3_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom3_Labels_key), "").toLowerCase();
                if (!preferences_customevent3_labels_str.equals("")) {
                    try {
                        preferences_customevent3_labels = Pattern.compile(preferences_customevent3_labels_str.replace(Div4, regex_inter) + regex_last).matcher("");
                        preferences_customevent3_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent3_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom3_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //4
            preferences_customevent4_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom4_Caption_key), "").trim();
            preferences_customevent4_enabled = false;

            if (!preferences_customevent4_caption.equals("")) {
                String preferences_customevent4_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom4_Labels_key), "").toLowerCase();
                if (!preferences_customevent4_labels_str.equals("")) {
                    try {
                        preferences_customevent4_labels = Pattern.compile(preferences_customevent4_labels_str.replace(Div4, regex_inter) + regex_last).matcher("");
                        preferences_customevent4_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent4_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom4_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));

            //5
            preferences_customevent5_caption = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom5_Caption_key), "").trim();
            preferences_customevent5_enabled = false;

            if (!preferences_customevent5_caption.equals("")) {
                String preferences_customevent5_labels_str = preferences.getString(context.getString(R.string.pref_CustomEvents_Custom5_Labels_key), "").toLowerCase();
                if (!preferences_customevent5_labels_str.equals("")) {
                    try {
                        preferences_customevent5_labels = Pattern.compile(preferences_customevent5_labels_str.replace(Div4, regex_inter) + regex_last).matcher("");
                        preferences_customevent5_enabled = true;
                    } catch (Exception e) {
                        //
                    }
                }
            }
            preferences_customevent5_useyear = preferences.getBoolean(context.getString(R.string.pref_CustomEvents_Custom5_UseYear_key), Boolean.getBoolean(context.getString(R.string.pref_CustomEvents_UseYear_default)));


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

        } catch (Exception e){
            e.printStackTrace();
            Toast.makeText(context, "ContactsEvents->getPreferences error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
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
                context.getResources().updateConfiguration(configuration, null);
                //}

                currentLocale = preferences_language;

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "ContactsEvents->setLocale error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }

    }

    //todo: попробовать добраться до ДР стандартными способами https://stackoverflow.com/questions/35448250/how-to-get-whatsapp-contacts-from-android
    //todo: сделать импорт ДР одновлассники https://ruseller.com/lessons.php?id=1661 https://apiok.ru/ext/oauth/
    boolean getContactsEvents() {

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
            final Resources resources = context.getResources();

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

                        String personID = orgCur.getString(cache.getColumnIndex(orgCur, "contact_id"));

                        String organization = orgCur.getString(cache.getColumnIndex(orgCur, ContactsContract.CommonDataKinds.Organization.COMPANY));
                        if (!orgMap.containsKey(personID)) orgMap.put(personID, organization != null ? organization : "");

                        String title = orgCur.getString(cache.getColumnIndex(orgCur, ContactsContract.CommonDataKinds.Organization.TITLE));
                        if (!titleMap.containsKey(personID)) titleMap.put(personID, title != null ? title : "");

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
                String eventKey = "";


                if (cursor.moveToFirst()) {
                    do {
                        String eventDate = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.DATA));
                        String eventType = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.TYPE));

                        if (eventDate != null) {

                            String contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE)); //бывает пусто
                            if (contactName == null) contactName = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.Data.DISPLAY_NAME));
                            if (contactName == null) contactName = "";
                            String accountType = cursor.getString(cache.getColumnIndex(cursor, "account_type"));
                            String eventLabel = cursor.getString(cache.getColumnIndex(cursor, ContactsContract.CommonDataKinds.Event.LABEL));
                            if (eventLabel == null) eventLabel = "";

                            boolean nonemptyEventLabel = !eventLabel.equals("");
                            String eventCaption = "";
                            int eventIcon = 0;

                            //todo: добавить 5 касточных событий
                            if (eventType.equals(typeBirthday) || (nonemptyEventLabel && preferences_birthday_labels != null && preferences_birthday_labels.reset(eventLabel.toLowerCase()).find())) {

                                eventCaption = resources.getString(R.string.event_type_birthday);
                                eventIcon = R.drawable.ic_event_birthday; //https://icons8.com/icon/21460/birthday

                            } else if (eventType.equals(typeAnniversary) || (nonemptyEventLabel && preferences_wedding_labels != null && preferences_wedding_labels.reset(eventLabel.toLowerCase()).find())) {

                                eventCaption = resources.getString(R.string.event_type_anniversary);
                                eventIcon = R.drawable.ic_event_wedding; //https://www.flaticon.com/free-icon/wedding-rings_224802

                            } else if (eventType.equals(typeOther)) {

                                eventCaption = resources.getString(R.string.event_type_other);
                                eventIcon = R.drawable.ic_event_custom1; //https://icons8.com/icon/set/event/office

                            } else if (nonemptyEventLabel) {

                                if (preferences_customevent1_enabled && preferences_customevent1_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent1_caption;
                                    eventIcon = R.drawable.ic_event_custom1;
                                    if (!preferences_customevent1_useyear && !eventDate.substring(0, 2).equals("--")) { //Если год не нужен, а он есть в событии
                                        eventDate = "--" + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else  if (preferences_customevent2_enabled && preferences_customevent2_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent2_caption;
                                    eventIcon = R.drawable.ic_event_custom2;
                                    if (!preferences_customevent2_useyear && !eventDate.substring(0, 2).equals("--")) { //Если год не нужен, а он есть в событии
                                        eventDate = "--" + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_customevent3_enabled && preferences_customevent3_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent3_caption;
                                    eventIcon = R.drawable.ic_event_custom3;
                                    if (!preferences_customevent3_useyear && !eventDate.substring(0, 2).equals("--")) { //Если год не нужен, а он есть в событии
                                        eventDate = "--" + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_customevent4_enabled && preferences_customevent4_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent4_caption;
                                    eventIcon = R.drawable.ic_event_custom4;
                                    if (!preferences_customevent4_useyear && !eventDate.substring(0, 2).equals("--")) { //Если год не нужен, а он есть в событии
                                        eventDate = "--" + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_customevent5_enabled && preferences_customevent5_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = preferences_customevent5_caption;
                                    eventIcon = R.drawable.ic_event_custom5;
                                    if (!preferences_customevent5_useyear && !eventDate.substring(0, 2).equals("--")) { //Если год не нужен, а он есть в событии
                                        eventDate = "--" + eventDate.substring(5); //Предполагается, что пользовательские события могут быть только YYYY-MM-DD
                                    }

                                } else if (preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = resources.getString(R.string.event_type_nameday);
                                    eventIcon = R.drawable.ic_event_other;

                                } else if (preferences_crowning_labels != null && preferences_crowning_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = resources.getString(R.string.event_type_crowning);
                                    eventIcon = R.drawable.ic_event_crowning; //https://iconscout.com/icon/wedding-destination-romance-building-emoj-symbol

                                } else if (preferences_death_labels != null && preferences_death_labels.reset(eventLabel.toLowerCase()).find()) {

                                    eventCaption = resources.getString(R.string.event_type_death);
                                    eventIcon = R.drawable.ic_event_death;

                                }

                            }
                            if (nonemptyEventLabel && eventCaption.equals("")) eventCaption = eventLabel;

                            if (EventTypes.contains(eventType)) {

                                String eventKey_next = contactName.concat(Div4).concat(eventType);

                                //Наименование события в ключе только для пользовательских событий
                                if (!eventType.equals(typeBirthday) && !eventType.equals(typeAnniversary) && !eventType.equals(typeOther))
                                    eventKey_next = eventKey_next.concat(Div4).concat(eventLabel);

                                String newEventDate = accountType.concat(Div3).concat(eventDate);

                                if (!eventKey_next.equalsIgnoreCase(eventKey)) { //Начало данных нового контакта

                                    if (!userData.isEmpty()) { // Уже есть накопленные данные. Нужно сохранить всё, что накопили и обнулить UserData
                                        dataRow = new StringBuilder();
                                        int rNum = 0;
                                        for (Map.Entry<Integer, String> entry : userData.entrySet()) {
                                            rNum++;
                                            if (rNum == 1) {
                                                dataRow.append(entry.getValue());
                                            } else {
                                                dataRow.append(Div1);
                                                dataRow.append(entry.getValue());
                                            }
                                        }
                                        dataList.add(dataRow.toString());
                                        userData.clear();
                                    }

                                    String contactID = cursor.getString(cache.getColumnIndex(cursor, "contact_id"));

                                    eventKey = eventKey_next;

                                    //todo: брать отдельно компоненты имени https://stackoverflow.com/questions/4301064/how-to-get-the-first-name-and-last-name-from-android-contacts
                                    userData.put(dataMap.get("eventDate_sorted"), " ");
                                    userData.put(dataMap.get("fio"), contactName.replace(Div5, " "));
                                    userData.put(dataMap.get("contact_id"), contactID);
                                    userData.put(dataMap.get("photo_uri"), cursor.getString(cache.getColumnIndex(cursor, "photo_uri")));
                                    userData.put(dataMap.get("eventDate"), " "); //Дата
                                    userData.put(dataMap.get("eventDistance"), " "); //Дней до даты
                                    userData.put(dataMap.get("eventDistanceText"), " "); //Через сколько событие и какой будет день недели
                                    userData.put(dataMap.get("age"), " "); //Возраст
                                    userData.put(dataMap.get("age_caption"), " "); //Подпись для возраста
                                    userData.put(dataMap.get("eventType"), eventType); //Тип события
                                    userData.put(dataMap.get("eventLabel"), eventCaption);
                                    userData.put(dataMap.get("organization"), orgMap.containsKey(contactID) ? orgMap.get(contactID) : "");
                                    userData.put(dataMap.get("title"), titleMap.containsKey(contactID) ? titleMap.get(contactID) : "");
                                    userData.put(dataMap.get("note"), ""); //noteMap.containsKey(contactID) ? noteMap.get(contactID) : "");
                                    userData.put(dataMap.get("dates"), newEventDate);
                                    userData.put(dataMap.get("eventIcon"), Integer.toString(eventIcon));

                                } else { //Продолжаем добавлять даты контакта

                                    String existingDates = userData.get(dataMap.get("dates"));
                                    if (existingDates != null && !existingDates.contains(newEventDate))
                                        userData.put(dataMap.get("dates"), existingDates.concat(Div2).concat(newEventDate));

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
                                dataRow.append(Div1);
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
            Toast.makeText(context, "ContactsEvents->getContactsEvents error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
            return false;
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
            Resources resources = context.getResources();

            for (int i = 0; i < dataArray.length; i++) {
                int dayDiff = -1;
                boolean isYear = false;
                Date eventDate = null; //истинная дата события
                Date BDay = null; //дата события в текущем году
                int Age = 0;

                String[] singleEventArray = dataArray[i].split(Div1);
                String[] dayArray = singleEventArray[dataMap.get("dates")].split(Div2);
                String eventLabel = singleEventArray[dataMap.get("eventLabel")];

                //перебираем все даты и находим максимальную
                for (String dayValue : dayArray) {
                    String accountType = dayValue.substring(0, dayValue.indexOf(Div3));
                    String storedDate = dayValue.substring(dayValue.indexOf(Div3) + Div3.length());
                    Date storedDate_Date = null;

                    if (accountType.toLowerCase().contains(resources.getString(R.string.account_skype))) {

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

                    } else if (accountType.equalsIgnoreCase(resources.getString(R.string.account_vk))) {

                        if (storedDate.substring(0, 5).equals("0000-")) { //Нет года, формат 0000-mm-dd

                            try {
                                BDay = sdf.parse(Integer.toString(now.get(YEAR)) + "-" + storedDate.substring(5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (BDay != null) {
                                int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) {
                                    dayDiff = 365 + dayDiff_tmp;
                                } else {
                                    dayDiff = dayDiff_tmp;
                                }
                            }

                        } else {

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

                        if (storedDate.substring(0, 2).equals("--") || //Нет года, формат --MM-dd
                                storedDate.substring(0, 5).equals("0000-") || //Нет года, формат 0000-MM-dd
                                storedDate.substring(0, 5).equals("1604-") || //Нет года, формат 1604-MM-dd
                                (!eventLabel.equals("") && preferences_nameday_labels != null && preferences_nameday_labels.reset(eventLabel.toLowerCase()).find()) //Именины считаем без года
                        ) {

                            try {
                                BDay = sdf.parse(Integer.toString(now.get(YEAR)) + "-" + storedDate.substring(storedDate.substring(0, 2).equals("--") ? 2 : 5));
                            } catch (ParseException e) {
                                //Не получилось распознать
                            }
                            if (BDay != null) {
                                int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                                if (dayDiff_tmp < 0) {
                                    dayDiff = 365 + dayDiff_tmp;
                                } else {
                                    dayDiff = dayDiff_tmp;
                                }
                            }

                        } else { //Обычный формат yyyy-MM-dd

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
                        } else if (countDaysDiff(eventDate, storedDate_Date) > 0) { //Если у пользователя несколько дат, берём наименьший возраст todo: можно вынести в настройку - в какую сторону округлять
                            eventDate = storedDate_Date;
                        }
                    }
                }

                if (eventDate != null) {

                    isYear = true;
                    Calendar cal = from(eventDate);
                    try {

                        BDay = sdf.parse(now.get(YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));

                        int dayDiff_tmp = countDaysDiff(currentDay, BDay);
                        Age = countYearsDiff(eventDate, BDay); //Считаем, сколько будет лет
                        if (dayDiff_tmp < 0) {
                            dayDiff = 365 + dayDiff_tmp; //todo: что-то мне подсказывает, что для високосного года тут надо +1, но я не уверен
                            Age += 1;
                        } else {
                            dayDiff = dayDiff_tmp;
                        }

                    } catch (ParseException e) {
                        dayDiff = -1; //Не получилось распознать
                    }
                }

                if (dayDiff == -1) {

                    dataArray[i] = "";

                } else {

                    singleEventArray[dataMap.get("eventDate_sorted")] = ("00" + dayDiff).substring(("00" + dayDiff).length() - 3); //Для сортировки
                    if (isYear) { //Дата с годом
                        singleEventArray[dataMap.get("eventDate")] = sdfYear.format(eventDate);
                    } else { //Дата без года
                        singleEventArray[dataMap.get("eventDate")] = sdfNoYear.format(BDay);
                    }

                    //Если событие в ближайшие 3 дня, то в eventDistance будет <число дней до события>, иначе: "Дней до <тип события>: " +  <число дней до события>
                    singleEventArray[dataMap.get("eventDistance")] = Integer.toString(dayDiff);

                    switch (dayDiff) {
                        case 0: //Сегодня
                            singleEventArray[dataMap.get("eventDistanceText")] = resources.getString(R.string.msg_today);
                            break;
                        case 1: //Завтра
                            singleEventArray[dataMap.get("eventDistanceText")] = resources.getString(R.string.msg_tomorrow);
                            break;
                        case 2: //Послезавтра
                            singleEventArray[dataMap.get("eventDistanceText")] = resources.getString(R.string.msg_day_after_tomorrow);
                            break;
                        default: //Подальше
                            Calendar c1 = Calendar.getInstance();
                            c1.setTime(BDay);
                            singleEventArray[dataMap.get("eventDistanceText")] = getEventDistance(resources, dayDiff).concat(resources.getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1]);
                    }

                    if (Age > 0) {
                        String Age_tmp = Integer.toString(Age);
                        singleEventArray[dataMap.get("age")] = Age_tmp;

                        String Age_end = Age_tmp.substring(Age_tmp.length() - 1);
                        String AgeString;

                        if (Age == 1) { //Единственное число
                            AgeString = Age_tmp + resources.getString(R.string.msg_after_age_prefix_1);
                        } else if (Age > 4 && Age < 21) {
                            AgeString = Age_tmp + resources.getString(R.string.msg_after_age_prefix_4_21);
                        } else if (Age_end.equals("1")) { //Если заканчивается на 1, но не между 5-20
                            AgeString = Age_tmp + resources.getString(R.string.msg_after_age_prefix_1_);
                        } else if (Age_end.equals("2") || Age_end.equals("3") || Age_end.equals("4")) { //Если заканчивается на 2, 3, 4
                            AgeString = Age_tmp + resources.getString(R.string.msg_after_age_prefix_2_3_4);
                        } else { //Всё остальное
                            AgeString = Age_tmp + resources.getString(R.string.msg_after_age_prefix_4_21);
                        }
                        singleEventArray[dataMap.get("age_caption")] = AgeString;

                        if (singleEventArray[dataMap.get("eventType")].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY))) {
                            String anCaption;
                            try {
                                anCaption = context.getString(resources.getIdentifier("event_type_wedding_" + Age, "string", context.getPackageName()));
                            } catch (Resources.NotFoundException nfe) {
                                anCaption = null;
                            }
                            if (anCaption != null && !anCaption.equals("") && !eventLabel.contains(" (")) {
                                singleEventArray[dataMap.get("eventLabel")] = eventLabel.concat(" (").concat(anCaption).concat(")");
                            }
                        }
                    } else {
                        singleEventArray[dataMap.get("age")] = "-1";
                        singleEventArray[dataMap.get("age_caption")] = " ";
                    }
                    //если дату вычисляли только для 5K - не записываем её
                    if (enabledTypes.contains(singleEventArray[dataMap.get("eventType")])) {
                        dataArray[i] = TextUtils.join(Div1, singleEventArray);
                    } else {
                        dataArray[i] = "";
                    }

                    //Вычисляем 5K даты
                    if (Age > 0 && singleEventArray[dataMap.get("eventType")].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)) && eventDate != null) {

                        //todo: подумать: надо ли считать 5K для смертей и.т.п.?
                        int days = countDaysDiff(eventDate, currentDay);
                        int k = (days + 365) / 5000;
                        int mdays = (days + 365) % 5000;

                        if (mdays >= 0 && mdays <= 365) {
                            //Формируем новую запись
                            //Calendar cal5K = Calendar.getInstance();
                            int magicDayDistance = 365 - mdays;
                            //cal5K.add(Calendar.DATE, magicDayDistance);

                            //String[] newDataArray = dataArray[i].split(Div1);
                            singleEventArray[dataMap.get("eventDate_sorted")] = ("00" + magicDayDistance).substring(("00" + magicDayDistance).length() - 3);
                            singleEventArray[dataMap.get("eventType")] = Integer.toString(event_types_id[4]);
                            singleEventArray[dataMap.get("eventLabel")] = "5K+";
                            //для выдачи даты юбилея,а не первоначального события: sdfYear.format(sdf.parse(cal5K.get(YEAR) + "-" + (cal5K.get(Calendar.MONTH) + 1) + "-" + cal5K.get(Calendar.DAY_OF_MONTH)));
                            singleEventArray[dataMap.get("eventDate")] = sdfYear.format(eventDate);
                            singleEventArray[dataMap.get("age")] = Integer.toString(Age);
                            singleEventArray[dataMap.get("age_caption")] = 5 * k + "K";

                            singleEventArray[dataMap.get("eventDistance")] = Integer.toString(magicDayDistance);

                            switch (magicDayDistance) {
                                case 0: //Сегодня
                                    singleEventArray[dataMap.get("eventDistanceText")] = resources.getString(R.string.msg_today);
                                    break;
                                case 1: //Завтра
                                    singleEventArray[dataMap.get("eventDistanceText")] = resources.getString(R.string.msg_tomorrow);
                                    break;
                                case 2: //Послезавтра
                                    singleEventArray[dataMap.get("eventDistanceText")] = resources.getString(R.string.msg_day_after_tomorrow);
                                    break;
                                default: //Подальше
                                    Calendar c1 = Calendar.getInstance();
                                    c1.add(Calendar.DATE, magicDayDistance);
                                    singleEventArray[dataMap.get("eventDistanceText")] = getEventDistance(resources, magicDayDistance).concat(resources.getStringArray(R.array.weekDays)[c1.get(Calendar.DAY_OF_WEEK) - 1]);
                            }

                            singleEventArray[dataMap.get("eventIcon")] = Integer.toString(R.drawable.ic_event_medal); //https://www.flaticon.com/free-icon/medal_610333

                            magicList.add(TextUtils.join(Div1, singleEventArray));
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
            Toast.makeText(context, "ContactsEvents->computeDates error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }
    }

    private String getEventDistance(Resources resources, int dayDiff) {
        // Возвращает "Через  Х дней"
        String dayDiff_tmp = Integer.toString(dayDiff);
        String dayDiff_end = dayDiff_tmp.substring(dayDiff_tmp.length() - 1);
        StringBuilder eventDistance = new StringBuilder();
        eventDistance.append(resources.getString(R.string.msg_before_event_prefix)).append(dayDiff_tmp);

        if (dayDiff == 1) { //Единственное число
            eventDistance.append(resources.getString(R.string.msg_before_event_prefix_1));
        } else if (dayDiff > 4 && dayDiff < 21) {
            eventDistance.append(resources.getString(R.string.msg_before_event_prefix_4_21));
        } else if (dayDiff_end.equals("1")) { //Если заканчивается на 1, но не между 5-20
            eventDistance.append(resources.getString(R.string.msg_before_event_prefix_1_));
        } else if (dayDiff_end.equals("2") || dayDiff_end.equals("3") || dayDiff_end.equals("4")) { //Если заканчивается на 2, 3, 4
            eventDistance.append(resources.getString(R.string.msg_before_event_prefix_2_3_4));
        } else {
            eventDistance.append(resources.getString(R.string.msg_before_event_prefix_4_21));
        }
        return eventDistance.toString();
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

}
