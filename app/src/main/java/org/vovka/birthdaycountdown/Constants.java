/*
 * *
 *  * Created by Vladimir Belov on 21.02.2025, 13:56
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 21.02.2025, 12:52
 *
 */

package org.vovka.birthdaycountdown;

/** @noinspection HardCodedStringLiteral*/
final class Constants {

    //https://coolefriend.com/know-names-of-symbols-in-your-computer-keyboard/
    static final String REGEX_COMMAS = " *, *";
    static final String REGEX_PLUS = "\\+";
    static final String REGEX_INTER = "\\Z|";
    static final String REGEX_LAST = "\\Z";
    static final String REGEX_EVENTS_SCOPE = "(\\d+?)[e](\\d+?)[d]";
    static final String REGEX_EVENTS_SCOPE_PLUS = "(\\d+?)[e](\\d+?)[d]([\\+\\-])";
    static final String REGEX_EVENTS_SCOPE_RAND = "(\\d+?)[e](\\d+?)[d](\\d+?)[r]";
    static final String REGEX_CALENDAR_LAYOUT = "(\\d+?)[r](\\d+?)[c]";
    static final String REGEX_PERIOD = "\\.";
    static final String STRING_00 = "00";
    static final String STRING_000 = "000";
    static final String STRING_0000 = "0000";
    static final String STRING_0000_MINUS = "0000-";
    static final String STRING_HASH = "#";
    static final String STRING_2HASH = "##";
    static final String STRING_2MINUS = "--";
    static final String STRING_MINUS = "-";
    static final String STRING_PLUS = "+";
    static final String STRING_2TILDA = "~~";
    static final String STRING_0 = "0";
    static final String STRING_1 = "1";
    static final String STRING_2 = "2";
    static final String STRING_3 = "3";
    static final String STRING_4 = "4";
    static final String STRING_5 = "5";
    static final String STRING_6 = "6";
    static final String STRING_7 = "7";
    static final String STRING_8 = "8";
    static final String STRING_9 = "9";
    static final String STRING_10 = "10";
    static final String STRING_11 = "11";
    static final String STRING_12 = "12";
    static final String STRING_13 = "13";
    static final String STRING_BAR = "|";
    static final String STRING_COLON = ":";
    static final String STRING_COLON_SPACE = ": ";
    static final String STRING_COMMA = ",";
    static final String STRING_COMMA_SPACE = ", ";
    static final String STRING_EMPTY = "";
    static final String STRING_EOL = "\n";
    static final String STRING_EOT = "\u0004";
    static final String STRING_EQ = "=?";
    static final String STRING_MINUS1 = "-1";
    static final String STRING_NULL = "null";
    static final String STRING_PARENTHESIS_CLOSE = ")";
    static final String STRING_PARENTHESIS_OPEN = " (";
    static final String STRING_PARENTHESIS_START = "(";
    static final String STRING_BRACKETS_OPEN = " [";
    static final String STRING_BRACKETS_START = "[";
    static final String STRING_BRACKETS_CLOSE = "]";
    static final String STRING_PERIOD = ".";
    static final String STRING_PIPE = "\\|";
    static final String STRING_SLASH = "/";
    static final String STRING_DSLASH = "//";
    static final String STRING_SPACE = " ";
    static final String STRING_BC = "BC";
    static final String STRING_UNDERSCORE = "_";
    static final String STRING_BDP_DIV = "❙"; //Birthdays Plus: |ДДДД-ММ-ДД|ИОФ|тип (Birthday, Anniversary, Custom)|наименование события или null|
    static final String STRING_BDP_EOL = "❚";
    static final String STRING_BDP_NO_YEAR = "1900";
    static final String STRING_BDP_CUSTOM = "Custom";

    static final String STRING_Z = "Z";
    static final String STRING_Y = "Y";

    static final String LANG_EN = "en";
    static final String LANG_RU = "ru";
    static final String LANG_US = "en_US";
    static final String LANG_UA = "uk_UA";

    //https://en.wikipedia.org/wiki/Date_format_by_country
    static final String DATETIME_DD_MM_YYYY_HH_MM = "dd.MM.yyyy HH:mm";
    static final String DATE_DD_MM_Y = "dd.MM.y";
    static final String DATE_DD_MM_YYYY = "dd.MM.yyyy";
    static final String DATE_DD_MM_YYYY_G = "dd.MM.yyyy G";
    static final String DATE_MM_DD_YYYY = "MM.dd.yyyy";
    static final String DATE_MM_DD = "MM.dd";
    static final String DATE_DD_MM = "dd.MM";
    static final String DATE_DD_MMM_YYYY = "dd MMM yyyy";
    static final String DATE_DD_MMM = "dd MMM";
    static final String DATE_D_MMMM_YYYY = "d MMMM yyyy";
    static final String DATE_MMMM_D_YYYY = "MMMM d, yyyy";
    static final String DATE_MMMM_D = "MMMM d";
    static final String DATE_D_MMMM = "d MMMM";
    static final String DATE_RUS = "dd MMMMM yyyy г.";
    static final String DATE_US = "MMM dd, yyyy";
    static final String DATE_UK = "dd/MM/yyyy";
    static final String DATE_UK_G = "dd/MM/yyyy G";
    static final String DATE_UK_NO_YEAR = "dd/MM";
    static final String DATE_IND = "MM/dd/yyyy";
    static final String DATE_IND_G = "MM/dd/yyyy G";
    static final String DATE_IND_NO_YEAR = "MM/dd";
    static final String DATE_JAVA = "yyyy-MM-dd";
    static final String DATE_JAVA_G = "yyyy-MM-dd G";
    static final String DATE_JAVA_NO_YEAR = "--MM-dd";
    static final String DATE_NO_DIV = "yyyyMMdd";
    static final String DATE_YY_MM_DD_HH_MM = " yy.MM.dd HH:mm";
    static final String DATE_LLLL_YYYY = "LLLL yyyy";

    static final String STRING_ID = "id";
    static final String STRING_TYPE_WEDDING = "event_type_wedding_";
    static final String STRING_TYPE_HOLIDAY = "holidays";
    static final String STRING_TYPE_FACT = "facts";
    static final String STRING_STORAGE_CONTACTS = "contacts";
    static final String STRING_STORAGE_CALENDAR = "calendar";
    static final String STRING_STORAGE_FILE = "file";
    static final String STRING_STORAGE_XDAYS = "x days";
    static final String STRING_STORAGE_HOLIDAYS = "holidays";
    static final String STRING_STORAGE_PREF = "pref";
    static final int Storage_Calendar = 0;
    static final int Storage_Contacts = 1;
    static final int Storage_File = 2;
    static final int Storage_Prefs = 3;
    static final String EVENT_PREFIX_CALENDAR_EVENT = "calendar event";
    static final String EVENT_PREFIX_FILE_EVENT = "file event";
    static final String EVENT_PREFIX_HOLIDAY_EVENT = "holiday";
    static final String EVENT_PREFIX_PREF_EVENT = "pref";
    static final String PREFIX_FileEventID = "f";
    static final String PREFIX_HolidayEventID = "h";
    static final String WIDGET_TEXT_VIEW = "textView";
    static final String WIDGET_TEXT_VIEW_LAYOUT = "textViewLayout";
    static final String WIDGET_TEXT_VIEW_2_ND = "textView2nd";
    static final String WIDGET_TEXT_VIEW_2_ND_LAYOUT = "textView2ndLayout";
    static final String WIDGET_EVENT_INFO = "eventInfo";
    static final String WIDGET_IMAGE_VIEW = "imageView";
    static final String WIDGET_IMAGE_VIEW_CENTERED = "imageViewCentered";
    static final String WIDGET_IMAGE_VIEW_START = "imageViewStart";
    static final String WIDGET_TEXT_VIEW_AGE = "textViewAge";
    static final String WIDGET_ICON_EVENT_TYPE = "iconEventType";
    static final String WIDGET_ICON_ZODIAC = "iconZodiac";
    static final String WIDGET_ICON_ZODIAC_YEAR = "iconZodiacYear";
    static final String WIDGET_ICON_FAV = "iconFav";
    static final String WIDGET_ICON_SILENCED = "iconSilenced";
    static final String WIDGET_TEXT_VIEW_DISTANCE = "textViewDistance";

    static final int Type_BirthDay = 0;
    static final int Type_Anniversary = 1;
    static final int Type_Other = 2;
    static final int Type_Custom = 3;
    static final int Type_5K = 4;
    static final int Type_Death = 5;
    static final int Type_NameDay = 6;
    static final int Type_Crowning = 7;
    static final int Type_Custom1 = 8;
    static final int Type_Custom2 = 9;
    static final int Type_Custom3 = 10;
    static final int Type_Custom4 = 11;
    static final int Type_Custom5 = 12;
    static final int Type_HolidayEvent = 23;
    static final int Type_CalendarEvent = 20;
    static final int Type_FileEvent = 21;
    static final int Type_Xdays = 22;
    static final int Type_Fact = 24;
    static final int Type_Unrecognized = 99;
    static final String Type_MultiEvent = "30";
    static final int pref_List_NameFormat_FirstSecondLast = 1;
    static final int pref_List_NameFormat_LastFirstSecond = 2;

    static final String ColumnNames_CONTACT_ID = "contact_id";
    static final String ColumnNames_ACCOUNT_TYPE = "account_type";
    static final String ColumnNames_ACCOUNT_NAME = "account_name";

    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 101;
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 102;
    static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 103;
    static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 104;
    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS_2 = 105;
    static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR_2 = 106;

    static final int RESULT_PICK_CONTACT = 200;
    static final int RESULT_PICK_FILE = 201;
    static final int RESULT_PICK_OTHER_CONTACT = 202;
    static final int RESULT_PICK_RINGTONE = 203;
    static final int RESULT_PICK_FILE_FOR_EXPORT = 204;
    static final int RESULT_PICK_FILE_FOR_IMPORT = 205;
    static final int RESULT_EDIT_EVENT = 206;

    static final int defaultNotificationID = 1000;
    static final int defaultNotification2ID = 1100;
    static final int defaultQuizID = 2000;
    static final String ACTION_SNOOZE = "action.SNOOZE";
    static final String ACTION_ATTACH = "action.ATTACH";
    static final String ACTION_NOTIFY = "action.NOTIFY";
    static final String ACTION_LAUNCH = "action.LAUNCH_ACTIVITY";
    static final String ACTION_HIDE = "action.HIDE";
    static final String ACTION_SILENT = "action.SILENT";
    static final String ACTION_DIAL = "action.DIAL";
    static final String ACTION_CLOSE = "action.CLOSE";
    static final String ACTION_CLICK = "action.CLICK";
    static final String ACTION_PREVIOUS_MONTH = "action.PREVIOUS_MONTH";
    static final String ACTION_NEXT_MONTH = "action.NEXT_MONTH";
    static final String ACTION_RESET_MONTH = "action.RESET_MONTH";
    static final String ACTION_SHARE = "action.SHARE";

    static final String EXTRA_NOTIFICATION_ID = "notificationID";
    static final String EXTRA_NOTIFICATION_DATA = "notificationData";
    static final String EXTRA_NOTIFICATION_DETAILS = "notificationDetails";
    static final String EXTRA_NOTIFICATION_ACTIONS = "notificationActions";
    static final String EXTRA_FILTER = "filterText";
    static final String EXTRA_QUIZ_QUESTION = "quizQuestion";
    static final String EXTRA_QUIZ_RESULT = "quizResult";
    static final String EXTRA_CLICKED_EVENT = "eventDetails";
    static final String EXTRA_CLICKED_PREFS = "actionPreference";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_VALUES = "values";
    static final String EXTRA_RESULTS = "results";
    static final String EXTRA_LIST = "list";
    static final String EXTRA_CHECKS = "checks";
    static final String EXTRA_COLORED = "colored";
    static final String EXTRA_NON_SORTED = "nonsorted";
    static final String EXTRA_COLOR = "color";
    static final String EXTRA_ANCHOR = "anchor";
    static final String EXTRA_CONTACT_ON_SAVE_RESULT = "finishActivityOnSaveCompleted";
    static final String EXTRA_DAY_CAPTION = "dayCaption";
    static final String EXTRA_DAY_INFO = "dayInfo";
    static final String EXTRA_NEW_WIDGET = "isNewWidget";
    static final String EXTRA_EVENT_DATA = "eventData";
    static final String EXTRA_DAY = "day";
    static final String EXTRA_MONTH = "month";
    static final String EXTRA_YEAR = "year";
    static final String EXTRA_USE_YEAR = "useYear";

    static final int HTML_COLOR_DEFAULT = 0;
    static final int HTML_COLOR_RED = 1;
    static final int HTML_COLOR_YELLOW = 2;
    static final int HTML_COLOR_BROWN = 3;
    static final int HTML_COLOR_GREEN = 4;

    static final String HTML_BOLD_START = "<b>";
    static final String HTML_BOLD_END = "</b>";
    static final String HTML_BR = "<br>";
    static final String HTML_COLOR = "<font color=\"#%s\">%s</font>"; //https://dzone.com/articles/java-string-format-examples
    static final String HTML_COLOR_START = "<font color=\"#%s\">";
    static final String HTML_COLOR_END = "</font>";
    static final String FONT_COLOR_RED = "<font color=red>";
    static final String FONT_COLOR_DOT = "<bold><font color=#%s>●</font></bold> ";
    static final String FONT_COLOR_GREEN = "<font color=#2ed70e>";
    static final String HTML_LI = "<li>";
    static final String HTML_LI_API21 = "&nbsp;-&nbsp;";
    static final String HTML_LI_END = "</li>";
    static final String HTML_UL_END = "</ul>";
    static final String HTML_UL_START = "<ul>";
    static final String HTML_FONT_END = "</font>";
    static final String HTML_H1_START = "<h1>";
    static final String HTML_H1_END = "</h1>";
    static final String HTML_H2_START = "<h2>";
    static final String HTML_H2_END = "</h2>";
    static final String HTML_H3_START = "<h3>";
    static final String HTML_H3_END = "</h3>";
    static final String HTML_LI_ITEM = "<br>&nbsp;-&nbsp;";

    static final String Broadcast_ANDROID_INTENT_ACTION_TIME_SET = "android.intent.action.TIME_SET";
    static final String Broadcast_ANDROID_INTENT_ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    static final String Broadcast_ANDROID_INTENT_ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    //static final String Broadcast_ANDROID_INTENT_ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    static final int pref_Events_Scope_NotHidden = 0;
    static final int pref_Events_Scope_All = 1;
    static final int pref_Events_Scope_Hidden = 2;
    static final int pref_Events_Scope_Silenced = 3;
    static final int pref_Events_Scope_XDays = 4;
    static final int pref_Events_Scope_Unrecognized = 5;
    static final int pref_Events_Scope_Favorite = 6;
    static final int pref_Events_Scope_Clear = 10;
    static final int pref_Events_Scope_Clean = 11;

    static final int WIDGET_TEXT_SIZE_TINY = 10;
    static final int WIDGET_TEXT_SIZE_SMALL = 12;
    static final int WIDGET_TEXT_SIZE_BIG = 18;
    static final int WIDGET_EVENTS_MAX = 7;
    static final int WIDGET_CALENDAR_OUT_MONTH_TINT = (int) (255 * 0.4);
    static final int TIME_SPEED_LOAD_CRITICAL = 700;
    static final int TIME_SPEED_LOAD_OVERTIME = 3000;
    static final int TIME_FORCE_UPDATE = 60000;
    static final int PREV_EVENTS_MAX_DAYS = 14;
    static final String account_google = "com.google";
    static final String account_skype = "com.skype.";
    static final String account_vk = "com.vkontakte.account";
    static final String account_exchange = "com.google.android.gm.exchange";
    static final String account_huawei = "com.android.huawei.phone";
    static final String account_sim = ".sim";
    static final String account_none = "none";

    static final String RULE_TAG_NAME = "[name]";
    static final String RULE_TAG_TYPE = "[type]";
    //static final String RULE_TAG_ALIAS = "[alias]";

    static final String PARAM_APP_WIDGET_ID = "appWidgetId";
    static final String WIDGET_TYPE_LIST = "WidgetList";
    static final String WIDGET_TYPE_PHOTO_LIST = "WidgetPhotoList";
    static final String WIDGET_TYPE_5X1 = "Widget5x1";
    static final String WIDGET_TYPE_4X1 = "Widget4x1";
    static final String WIDGET_TYPE_2X2 = "Widget2x2";
    static final String WIDGET_TYPE_CALENDAR = "WidgetCalendar";

    static final String quiz_error_button_OK = "-##OK##";
    static final String BUTTON_X = "X";
    static final String FilePrefix_Media = "com.android.providers.media.documents";
    static final String FilePrefix_Downloads = "com.android.providers.downloads.documents";
    static final String FilePrefix_ExternalStorage = "com.android.externalstorage.documents";
    static final String FilePrefix_GooglePhotos = "com.google.android.apps.photos.content";

    static final String STRING_HTTP = "http://";
    static final String STRING_HTTPS = "https://";
    static final String RES_PACKAGE_ANDROID = "android";
    static final String RES_TYPE_STRING = "string";
    static final String RES_TYPE_ID = "id";
    static final String RES_TYPE_STRING_ARRAY = "array";
    static final String RES_TYPE_CALENDAR = "calendar";
    static final String RES_TYPE_YEAR = "year";
    static final String URI_PACKAGE = "package:";

    static final int ContextMenu_EditContact = 1;
    static final int ContextMenu_CreateContact = 2;
    static final int ContextMenu_EventInfo = 3;
    static final int ContextMenu_HideEvent = 4;
    static final int ContextMenu_UnhideEvent = 5;
    static final int ContextMenu_Remind_1H = 7;
    static final int ContextMenu_Remind_Morning = 8;
    static final int ContextMenu_AnniversaryList = 9;
    static final int ContextMenu_SilentEvent = 10;
    static final int ContextMenu_UnsilentEvent = 11;
    static final int ContextMenu_OpenCalendar = 12;
    static final int ContextMenu_MergeEvent = 13;
    static final int ContextMenu_UnmergeEvent = 14;
    static final int ContextMenu_RemergeEvent = 15;
    static final int ContextMenu_OpenURL = 16;
    static final int ContextMenu_ShareAsImage = 17;
    static final int ContextMenu_ShareAsText = 18;
    static final int ContextMenu_xDaysEvent = 19;
    static final int ContextMenu_SetEvenType = 20;
    static final int ContextMenu_AddToFavorites = 21;
    static final int ContextMenu_RemoveFromFavorites = 22;
    static final int ContextMenu_EditLocalEvent = 23;

    static final String eventSourceLocalPrefix = "local:";
    static final String eventSourceContactPrefix = "contact:";
    static final String eventSourceCalendarPrefix = "calendar:";
    static final String eventSourceFilePrefix = "file:";
    static final String eventSourceMultiFilePrefix = "multifile:";
    static final String eventSourceHolidayPrefix = "holiday:";
    static final String eventSourceFactPrefix = "fact:";
    static final String eventSourceFavoritePrefix = "fav:";
    static final String eventSourcePrefPrefix = "pref:";

    static final String eventTitleFilePrefix = "📁 ";
    static final String eventTitleCalendarPrefix = "📆 ";
    static final String eventTitleContactPrefix = "👨‍💼 ";
    static final String eventTitleFavoritePrefix = "🌟 ";

    public static final String SHORTCUT_QUIZ = "Quiz";
    public static final String SHORTCUT_SETTINGS = "Settings";
    public static final String SHORTCUT_NOTIFY = "Notify";
    public static final String SHORTCUT_FACTS = "Facts";

    static final int onClick_None = 0;
    static final int onClick_Popup = 1;
    static final int onClick_Calendar = 2;

    public static final String QUEUE = "queue";

    static final int PhotoWidget_Upper_Caption = 0;
    static final int PhotoWidget_Upper_Aligning = 1;
    static final int PhotoWidget_Upper_Rows = 2;
    static final int PhotoWidget_Upper_FontStyle = 3;
    static final int PhotoWidget_Upper_FontSize = 4;
    static final int PhotoWidget_Upper_Color = 5;
    static final int PhotoWidget_Bottom_Caption = 6;
    static final int PhotoWidget_Bottom_Aligning = 7;
    static final int PhotoWidget_Bottom_Rows = 8;
    static final int PhotoWidget_Bottom_FontStyle = 9;
    static final int PhotoWidget_Bottom_FontSize = 10;
    static final int PhotoWidget_Bottom_Color = 11;

    static final int Align_Left = 1;
    static final int Align_Center = 2;
    static final int Align_Right = 3;
    static final String TRANSPARENT = "transparent";

    static final String PATH_MEDIA_EXTERNAL = "/media/external/";

    static final String APP_PACKAGE = "app_package";
    static final String APP_UID = "app_uid";
    static final String QUERY_PARAM_DELETED_0 = "deleted=0";
    static final String QUERY_PARAM_YEARLY = "FREQ=YEARLY";
    static final String QUERY_PARAM_AND = " AND ";
    static final String QUERY_PARAM_OR = " OR ";
    static final String PREF_TYPE_STRING = " (string)";
    static final String PREF_TYPE_BOOLEAN = " (boolean)";
    static final String PREF_TYPE_INT = " (int)";
    static final String PREF_TYPE_SET = " (string set)";
    static final String CHARSET_HTML_UTF_8 = "text/html; charset=utf-8";
    static final String CHARSET_UTF_8 = "utf-8";
    static final String MAILTO_TEMPLATE = "mailto:belov.vladimir@mail.ru?subject=";
    static final String ANCHOR_LINK = "<script>window.location.hash=\"%s\";</script>";
    static final String DRAWABLE_BASE_URL = "file:///android_res/drawable/";

    static final String STORE_LINK_GOOGLE_MARKET = "market://details?id=%s";
    static final String STORE_LINK_PLAY_MARKET = "https://play.google.com/store/apps/details?id=%s";
    static final String STORE_LINK_HUAWEI = "https://appgallery.huawei.com/app/C101143661";
    static final String STORE_NAME_HUAWEI = "Huawei AppGallery";
    static final String STORE_LINK_RUSTORE = "https://www.rustore.ru/catalog/app/org.vovka.birthdaycountdown";
    static final String STORE_NAME_RUSTORE = "RuStore";
    static final String STORE_LINK_SAMSUNG = "https://apps.samsung.com/appquery/appDetail.as?appId=org.vovka.birthdaycountdown";
    static final String STORE_NAME_SAMSUNG = "Galaxy Store";
    static final String STORE_LINK_4PDA = "https://4pda.to/forum/index.php?showtopic=939391";

    static final String METHOD_SET_BACKGROUND_COLOR = "setBackgroundColor";
    static final String METHOD_SET_BACKGROUND_RES = "setBackgroundResource";
    static final String METHOD_SET_MIN_WIDTH = "setMinWidth";

    static final String MIME_IMAGE_ALL = "image/*";
    static final String MIME_IMAGE_JPEG = "image/jpeg";
    static final String MIME_IMAGE_PNG = "image/png";

    public static final String SQL_SORT_ASC = " ASC";
    public static final String SQL_SORT_ASC_CONT = " ASC, ";

    public static String LocalEventsFilename = "Events";
}