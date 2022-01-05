/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 26.12.2021, 0:22
 *
 */

package org.vovka.birthdaycountdown;

final class Constants {

    static final String STRING_EMPTY = "";
    static final String STRING_SPACE = " ";
    static final String STRING_NULL = "null";
    static final String STRING_EOL = "\n";
    static final String STRING_MINUS1 = "-1";
    static final String STRING_0 = "0";
    static final String STRING_00 = "00";
    static final String STRING_0000_MINUS = "0000-";
    static final String STRING_0000 = "0000";
    static final String STRING_EQ = "=?";
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

    static final String LANG_EN = "en";
    static final String LANG_RU = "ru";
    static final String LANG_US = "en_US";
    static final String LANG_UA = "uk_UA";

    //https://en.wikipedia.org/wiki/Date_format_by_country
    static final String DATETIME_DD_MM_YYYY_HH_MM = "dd.MM.yyyy HH:mm";
    static final String DATE_DD_MM_YYYY = "dd.MM.yyyy";
    static final String DATE_MM_DD_YYYY = "MM.dd.yyyy";
    static final String DATE_MM_DD = "MM.dd";
    static final String DATE_DD_MM = "dd.MM";
    static final String DATE_DD_MMM_YYYY = "dd MMM yyyy";
    static final String DATE_DD_MMM = "dd MMM";
    static final String DATE_D_MMMM_YYYY = "d MMMM yyyy";
    static final String DATE_D_MMMM = "d MMMM";
    static final String DATE_RUS = "dd MMMMM yyyy г.";
    static final String DATE_US = "MMM dd, yyyy";
    static final String DATE_UK = "dd/MM/yyyy";
    static final String DATE_UK_NO_YEAR = "dd/MM";
    static final String DATE_IND = "MM/dd/yyyy";
    static final String DATE_IND_NO_YEAR = "MM/dd";
    static final String DATE_JAVA = "yyyy-MM-dd";
    static final String DATE_JAVA_NO_YEAR = "--MM-dd";

    static final String STRING_ID = "id";
    static final String STRING_TYPE_WEDDING = "event_type_wedding_";
    static final String STRING_STORAGE_CONTACTS = "contacts";
    static final String STRING_STORAGE_CALENDAR = "calendar";
    static final String STRING_STORAGE_FILE = "file";
    static final String EVENT_PREFIX_CALENDAR_EVENT = "calendar event";
    static final String EVENT_PREFIX_FILE_EVENT = "file event";

    static final String WIDGET_TEXT_VIEW = "textView";
    static final String WIDGET_TEXT_VIEW_CENTERED = "textViewCentered";
    static final String WIDGET_TEXT_VIEW_2_ND = "textView2nd";
    static final String WIDGET_TEXT_VIEW_2_ND_CENTERED = "textView2ndCentered";
    static final String WIDGET_EVENT_INFO = "eventInfo";
    static final String WIDGET_IMAGE_VIEW = "imageView";
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
    static final int Type_CalendarEvent = 20;
    static final int Type_FileEvent = 21;

    static final String STRING_2HASH = "##"; //https://coolefriend.com/know-names-of-symbols-in-your-computer-keyboard/
    static final String STRING_2TILDA = "~~";
    static final String STRING_COLON_SPACE = ": ";
    static final String STRING_COLON = ":";
    static final String STRING_COMMA = ",";
    static final String STRING_COMMA_SPACE = ", ";
    static final String STRING_PERIOD = ".";
    static final String REGEX_COMMAS = " *, *";
    static final String STRING_PARENTHESIS_OPEN = " (";
    static final String STRING_PARENTHESIS_START = "(";
    static final String STRING_PARENTHESIS_CLOSE = ")";
    static final String STRING_2MINUS = "--";
    static final String STRING_MINUS = "-";
    static final String STRING_PIPE = "\\|";
    static final String STRING_BAR = "|";

    static final String ColumnNames_CONTACT_ID = "contact_id";
    static final String ColumnNames_ACCOUNT_TYPE = "account_type";
    static final String ColumnNames_ACCOUNT_NAME = "account_name";

    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 101;
    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 102;
    static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 103;

    static final int RESULT_PICK_CONTACT = 200;
    static final int RESULT_PICK_OTHER_CONTACT = 202;
    static final int RESULT_PICK_FILE = 201;
    static final int RESULT_PICK_RINGTONE = 203;

    static final int defaultNotificationID = 1000;
    static final int defaultQuizID = 2000;
    static final String ACTION_SNOOZE = "ACTION_SNOOZE";
    static final String ACTION_NOTIFY = "ACTION_NOTIFY";
    static final String ACTION_LAUNCH = "LAUNCH_ACTIVITY";
    static final String ACTION_HIDE = "ACTION_HIDE";
    static final String ACTION_SILENT = "ACTION_SILENT";
    static final String ACTION_DIAL = "ACTION_DIAL";
    static final String ACTION_CLOSE = "ACTION_CLOSE";
    static final String ACTION_CLICK = "ACTION_CLICK";

    static final String EXTRA_NOTIFICATION_ID = "notificationID";
    static final String EXTRA_NOTIFICATION_DATA = "notificationData";
    static final String EXTRA_FILTER = "filterText";
    static final String EXTRA_QUIZ_QUESTION = "quizQuestion";
    static final String EXTRA_QUIZ_RESULT = "quizResult";
    static final String EXTRA_CLICKED_EVENT = "eventDetails";
    static final String EXTRA_CLICKED_PREFS = "actionPreference";

    static final int HTML_COLOR_DEFAULT = 0;
    static final int HTML_COLOR_RED = 1;
    static final int HTML_COLOR_YELLOW = 2;
    static final int HTML_COLOR_BROWN = 3;

    static final String HTML_BOLD_START = "<b>";
    static final String HTML_BOLD_END = "</b>";
    static final String HTML_BR = "<br>";
    static final String HTML_COLOR = "<font color=\"#%s\">%s</font>"; //https://dzone.com/articles/java-string-format-examples
    static final String HTML_COLOR_START = "<font color=\"#%s\">";
    static final String HTML_LI = "<li>";
    static final String HTML_UL_END = "</ul>";
    static final String HTML_UL_START = "<ul>";
    static final String HTML_FONT_END = "</font>";
    static final String HTML_H1_START = "<h1>";
    static final String HTML_H1_END = "</h1>";
    static final String HTML_H2_START = "<h2>";
    static final String HTML_H2_END = "</h2>";
    static final String HTML_H3_START = "<h3>";
    static final String HTML_H3_END = "</h3>";

    static final String Broadcast_ANDROID_INTENT_ACTION_TIME_SET = "android.intent.action.TIME_SET";
    static final String Broadcast_ANDROID_INTENT_ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    static final String Broadcast_ANDROID_INTENT_ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    static final String Broadcast_ANDROID_INTENT_ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    static final int pref_Events_Scope_NotHidden = 0;
    static final int pref_Events_Scope_All = 1;
    static final int pref_Events_Scope_Hidden = 2;
    static final int pref_Events_Scope_Silenced = 3;
    static final int pref_Events_Scope_Clear = 10;
    static final int pref_Events_Scope_Clean = 11;

    static final int MENU_MAIN_SEARCH = 0;
    static final int MENU_MAIN_ADD_EVENT = 1;
    static final int MENU_MAIN_REFRESH = 2;
    static final int MENU_MAIN_QUIZ = 3;
    static final int MENU_MAIN_SETTINGS = 4;
    static final int MENU_MAIN_FILTER = 5;
    static final int MENU_MAIN_HINTS = 6;

    static final int WIDGET_TEXT_SIZE_TINY = 10;
    static final int WIDGET_TEXT_SIZE_SMALL = 12;
    static final int WIDGET_TEXT_SIZE_BIG = 18;
    static final int WIDGET_EVENTS_MAX = 7;
    static final int SPEED_LOAD_CRITICAL = 700;

    //Отладочные сообщения

    static final String MSG_YEAR_MUST_BE_GREATER_0 = "Year must be > 0.";
    static final String MSG_NOTIFICATIONS_WERE_ENABLED = "Notifications were enabled\n";
    static final String MSG_NOTIFICATIONS_WERE_DISABLED = "Notifications were disabled\n";
    static final String MSG_WIDGET_PREFS_REMOVED = "Preferences for widget #%s has been removed";
    static final String MSG_WIDGET_PREFS_SAVED = "Saved widget #%s preferences: ";
    static final String MSG_RINGTONE = "Ringtone: ";
    static final String MSG_DELETED_CHANNEL_ = "Deleted channel ";
    static final String MSG_CREATED_CHANNEL_ = "Created channel ";
    static final String MSG_SENT_WIDGETS_UPDATE_REQUEST = "Sent widgets update request";
    static final String MSG_NEXT_NOTIFICATION = "Next notification: ";
    static final String MSG_NEXT_WIDGETUPDATE = "Next widget update: ";

    static final String RULE_TAG_NAME = "[name]";
    //static final String RULE_TAG_ALIAS = "[alias]";

    //Ошибки

    static final String ALARM_RECEIVER_ON_RECEIVE_ERROR = "AlarmReceiver->onReceive error:\n";
    static final String ACTION_RECEIVER_ON_RECEIVE_ERROR = "ActionReceiver->onReceive error:\n";
    static final String WIDGETUPDATE_RECEIVER_ON_RECEIVE_ERROR = "WidgetUpdaterReceiver->onReceive error:\n";

    static final String CONTACTS_EVENTS_CHECK_IS_HIDDEN_EVENT_ERROR = "ContactsEvents->checkIsHiddenEvent error:\n";
    static final String CONTACTS_EVENTS_CHECK_IS_SILENT_EVENT_ERROR = "ContactsEvents->checkIsSilentEvent error:\n";
    static final String CONTACTS_EVENTS_CHECK_IS_BATTERY_OPTIMIZATION_ERROR = "ContactsEvents->checkNoBatteryOptimization error:\n";
    static final String CONTACTS_EVENTS_COMPUTE_DATES_ERROR = "ContactsEvents->computeDates error:\n";
    static final String CONTACTS_EVENTS_GET_EVENTS_ERROR = "ContactsEvents->getEvents error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACTS_EVENTS_ERROR = "ContactsEvents->getContactsEvents error:\n";
    static final String CONTACTS_EVENTS_GET_FILTERED_EVENT_LIST_ERROR = "ContactsEvents->getFilteredEventList error:\n";
    static final String CONTACTS_EVENTS_READ_TEXT_FROM_URI_ERROR = "ContactsEvents->readFileToString error:\n";
    static final String CONTACTS_EVENTS_ADD_CONTACT_EVENT_ERROR = "ContactsEvents->addContactEventToEventList error:\n";
    static final String CONTACTS_EVENTS_GET_CALENDAR_EVENTS_ERROR = "ContactsEvents->getCalendarEvents error:\n";
    static final String CONTACTS_EVENTS_GET_FILE_EVENTS_ERROR = "ContactsEvents->getFileEvents error:\n";
    static final String CONTACTS_EVENTS_GET_CALENDARS_ERROR = "ContactsEvents->getCalendars error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_PHOTO_ERROR = "ContactsEvents->getContactPhoto error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_DATA_ERROR = "ContactsEvents->getContactData error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_DATA_MULTI_ERROR = "ContactsEvents->getContactDataMulti error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_NAME_ERROR = "ContactsEvents->getContactFirstName error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_PHONE_ERROR = "ContactsEvents->getContactPhone error:\n";
    static final String CONTACTS_EVENTS_GET_EVENT_DISTANCE_TEXT_ERROR = "ContactsEvents->getEventDistanceText error:\n";
    static final String CONTACTS_EVENTS_GET_HIDDEN_EVENTS_COUNT_ERROR = "ContactsEvents->getHiddenEventsCount error:\n";
    static final String CONTACTS_EVENTS_CLEAR_HIDDEN_EVENTS_ERROR = "ContactsEvents->clearHiddenEvents error:\n";
    static final String CONTACTS_EVENTS_CLEAR_SILENCED_EVENTS_ERROR = "ContactsEvents->clearSilencedEvents error:\n";
    static final String CONTACTS_EVENTS_GET_SILENT_EVENTS_COUNT_ERROR = "ContactsEvents->getSilentEventsCount error:\n";
    static final String CONTACTS_EVENTS_GET_PREFERENCES_ERROR = "ContactsEvents->getPreferences error:\n";
    static final String CONTACTS_EVENTS_INIT_BOOT_RECEIVER_ERROR = "ContactsEvents->initBootReceiver error:\n";
    static final String CONTACTS_EVENTS_INIT_NOTIFICATIONS_ERROR = "ContactsEvents->initNotifications error:\n";
    static final String CONTACTS_EVENTS_INIT_WIDGETUPDATE_ERROR = "ContactsEvents->initWidgetUpdate error:\n";
    static final String CONTACTS_EVENTS_INIT_NOTIFICATION_CHANNEL_ERROR = "ContactsEvents->initNotificationChannel error:\n";
    static final String CONTACTS_EVENTS_INSERT_PREVIOUS_EVENTS_ERROR = "ContactsEvents->insertPreviousEvents error:\n";
    static final String CONTACTS_EVENTS_SET_HIDDEN_EVENT_ERROR = "ContactsEvents->setHiddenEvent error:\n";
    static final String CONTACTS_EVENTS_UNSET_HIDDEN_EVENT_ERROR = "ContactsEvents->unsetHiddenEvent error:\n";
    static final String CONTACTS_EVENTS_SET_SILENT_EVENT_ERROR = "ContactsEvents->setSilentEvent error:\n";
    static final String CONTACTS_EVENTS_UNSET_SILENT_EVENT_ERROR = "ContactsEvents->unsetSilentEvent error:\n";
    static final String CONTACTS_EVENTS_GET_MERGED_ID_ERROR = "ContactsEvents->getMergedID error:\n";
    static final String CONTACTS_EVENTS_SET_MERGED_ID_ERROR = "ContactsEvents->setMergedID error:\n";
    static final String CONTACTS_EVENTS_CLEAR_UNEXISTING_SILENCED_EVENTS_ERROR = "ContactsEvents->clearUnexistingSilencedEvents error:\n";
    static final String CONTACTS_EVENTS_CLEAR_UNEXISTING_HIDDEN_EVENTS_ERROR = "ContactsEvents->clearUnexistingHiddenEvents error:\n";
    static final String CONTACTS_EVENTS_SET_WIDGET_PREFERENCE_ERROR = "ContactsEvents->setWidgetPreference error:\n";
    static final String CONTACTS_EVENTS_GET_WIDGET_PREFERENCE_ERROR = "ContactsEvents->getWidgetPreference error:\n";
    static final String CONTACTS_EVENTS_REMOVE_WIDGET_PREFERENCE_ERROR = "ContactsEvents->removeWidgetPreference error:\n";
    static final String CONTACTS_EVENTS_SET_LOCALE_ERROR = "ContactsEvents->setLocale error:\n";
    static final String CONTACTS_EVENTS_SET_PREFERENCES_ERROR = "ContactsEvents->setPreferences error:\n";
    static final String CONTACTS_EVENTS_SHOW_NOTIFICATIONS_ERROR = "ContactsEvents->showNotifications error:\n";
    static final String CONTACTS_EVENTS_SNOOZE_NOTIFICATION_ERROR = "ContactsEvents->SnoozeNotification error:\n";
    static final String CONTACTS_EVENTS_EVENT_KEY_ERROR = "ContactsEvents->getEventKey error:\n";
    static final String CONTACTS_EVENTS_SHOW_NOTIFICATION_ERROR = "ContactsEvents->ShowNotification error:\n";
    static final String CONTACTS_EVENTS_COUNT_DAYS_DIFF_ERROR = "ContactsEvents->countDaysDiff error:\n";
    static final String CONTACTS_EVENTS_COUNT_DAYS_DIFF_TEXT_ERROR = "ContactsEvents->countDaysDiffText error:\n";
    static final String CONTACTS_EVENTS_ADD_YEAR_ERROR = "ContactsEvents->addYear error:\n";
    static final String CONTACTS_EVENTS_GET_AGE_STRING_ERROR = "ContactsEvents->getAgeString error:\n";
    static final String CONTACTS_EVENTS_COUNT_YEARS_DIFF_ERROR = "ContactsEvents->countYearsDiff error:\n";
    static final String CONTACTS_EVENTS_SHOW_ANNIVERSARY_LIST_ERROR = "ContactsEvents->showAnniversaryList error:\n";
    static final String CONTACTS_EVENTS_SET_HTML_COLOR_ERROR = "ContactsEvents->setHTMLColor error:\n";
    static final String CONTACTS_EVENTS_QUIZ_CHECK_AND_GO_ERROR = "ContactsEvents->quizCheckAndGo error:\n";
    static final String CONTACTS_EVENTS_QUIZ_GET_QUESTION_ERROR = "ContactsEvents->quizGetQuestion error:\n";
    static final String CONTACT_EVENTS_GET_DATA_COLUMN_ERROR = "ContactsEvents->getDataColumn error:\n";
    static final String CONTACT_EVENTS_FILL_EMPTY_USERDATA_ERROR = "ContactsEvents->fillEmptyUserData error:\n";
    static final String CONTACT_EVENTS_GET_ACTION_INTENT_ERROR = "ContactsEvents->getViewActionIntent error:\n";

    static final String DEVICE_BOOT_RECEIVER_ON_RECEIVE_ERROR = "DeviceBootReceiver->onReceive error:\n";

    static final String DATE_RECEIVER_ON_RECEIVE_ERROR = "DateReceiver->onReceive error:\n";

    static final String EVENT_LIST_DATA_PROVIDER_GETVIEWAT_ERROR = "EventListDataProvider->getViewAt error:\n";
    static final String EVENT_LIST_DATA_PROVIDER_INIT_DATA_ERROR = "EventListDataProvider->initData error:\n";

    static final String EVENT_PHOTO_LIST_DATA_PROVIDER_GETVIEWAT_ERROR = "EventPhotoListDataProvider->getViewAt error:\n";
    static final String EVENT_PHOTO_LIST_DATA_PROVIDER_INIT_DATA_ERROR = "EventPhotoListDataProvider->initData error:\n";

    static final String GET_ACCOUNTS_LIST_ADAPTER_GET_VIEW_ERROR = "AccountsListAdapter->getView error:\n";

    static final String MAIN_ACTIVITY_DRAW_LIST_ERROR = "MainActivity->drawList error:\n";
    static final String MAIN_ACTIVITY_PREPARE_LIST_ERROR = "MainActivity->prepareList error:\n";
    static final String MAIN_ACTIVITY_DRAW_LIST_ON_ITEM_CLICK_ERROR = "MainActivity->drawList->onItemClick error:\n";
    static final String MAIN_ACTIVITY_INIT_NOTIFICATIONS_ERROR = "MainActivity->initNotifications error:\n";
    static final String MAIN_ACTIVITY_ON_CONTEXT_ITEM_SELECTED_ERROR = "MainActivity->onContextItemSelected error:\n";
    static final String MAIN_ACTIVITY_ON_CREATE_CONTEXT_MENU_ERROR = "MainActivity->onCreateContextMenu error:\n";
    static final String MAIN_ACTIVITY_ON_CREATE_ERROR = "MainActivity->onCreate error:\n";
    static final String MAIN_ACTIVITY_ON_OPTIONS_ITEM_SELECTED_ERROR = "MainActivity->onOptionsItemSelected error:\n";
    static final String MAIN_ACTIVITY_ON_ACTIVITY_RESULT_ERROR = "MainActivity->onActivityResult error:\n";
    static final String MAIN_ACTIVITY_ON_CREATE_OPTIONS_MENU_ERROR = "MainActivity->onCreateOptionsMenu error:\n";
    static final String MAIN_ACTIVITY_ON_REFRESH_ERROR = "MainActivity->onRefresh error:\n";
    static final String MAIN_ACTIVITY_ON_RESUME_ERROR = "MainActivity->onResume error:\n";
    static final String MAIN_ACTIVITY_SET_HINT_ERROR = "MainActivity->setHint error:\n";
    static final String MAIN_ACTIVITY_CHECK_NEW_VERSION_ERROR = "MainActivity->checkNewVersion error:\n";
    static final String MAIN_ACTIVITY_SET_LASTRUN_VERSION_ERROR = "MainActivity->setLastRunVersion error:\n";
    //static final String MAIN_ACTIVITY_CHECK_BATTERY_OPTIMIZATION_ERROR = "MainActivity->checkBatteryOptimization error:\n";
    static final String MAIN_ACTIVITY_SHOW_WELCOME_SCREEN_ERROR = "MainActivity->showWelcomeScreen error:\n";

    static final String MY_ADAPTER_GET_VIEW_ERROR = "MyAdapter->getView error:\n";

    static final String PERSON_CONSTRUCTOR_ERROR = "Person->Constructor error:\n";
    static final String PERSON_GET_FULL_NAME_SHORT_ERROR = "Person->getFullNameShort error: ";
    static final String PERSON_GET_GENDER_ERROR = "Person->getGender error:\n";

    static final String SETTINGS_ACTIVITY_ON_CREATE_ERROR = "SettingsActivity->onCreate error:\n";
    static final String SETTINGS_ACTIVITY_ON_PREFERENCE_TREE_CLICK_ERROR = "SettingsActivity->onPreferenceTreeClick error:\n";
    static final String SETTINGS_ACTIVITY_SET_UP_NESTED_SCREEN_ERROR = "SettingsActivity->setUpNestedScreen error:\n";
    static final String SETTINGS_ACTIVITY_ON_STOP_ERROR = "SettingsActivity->onStop error:\n";
    static final String SETTINGS_ACTIVITY_GET_ACCOUNTS_ERROR = "SettingsActivity->getAccounts error:\n";
    static final String SETTINGS_ACTIVITY_SELECT_CALENDARS_ERROR = "SettingsActivity->selectCalendars error:\n";
    static final String SETTINGS_ACTIVITY_SELECT_RINGTONE_ERROR = "SettingsActivity->selectRingtone error:\n";
    static final String SETTINGS_ACTIVITY_EDIT_RULES_ERROR = "SettingsActivity->editRules error:\n";
    static final String SETTINGS_ACTIVITY_SELECT_FILES_ERROR = "SettingsActivity->selectFiles error:\n";
    static final String SETTINGS_ACTIVITY_ON_ACTIVITY_RESULT_ERROR = "SettingsActivity->onActivityResult error:\n";
    static final String SETTINGS_ACTIVITY_GET_PATH_ERROR = "SettingsActivity->getPath error:\n";
    static final String SETTINGS_ACTIVITY_UPDATE_TITLES_ERROR = "SettingsActivity->updateTitles error:\n";
    static final String SETTINGS_ACTIVITY_UPDATE_VISIBILITY_ERROR = "SettingsActivity->updateVisibility error:\n";

    static final String ABOUT_ACTIVITY_ON_CREATE_ERROR = "AboutActivity->onCreate error:\n";
    static final String ABOUT_ACTIVITY_SET_DEBUG_ERROR = "AboutActivity->setDebug error:\n";
    static final String ABOUT_ACTIVITY_SHOW_PREFERENCES_ERROR = "AboutActivity->showPreferences error:\n";

    static final String COLOR_PICKER_INIT_ATTRS_ERROR = "ColorPicker->initAttrs error:\n";
    static final String COLOR_PICKER_ON_CLICK_ERROR = "ColorPicker->onClick error:\n";
    static final String COLOR_PICKER_SET_COLOR_VIEW_VALUE_ERROR = "ColorPicker->setColorViewValue error:\n";

    static final String NOTIFY_ACTIVITY_ON_CREATE_ERROR = "NotifyActivity->onCreate error:\n";

    static final String QUIZ_ACTIVITY_ON_CREATE_ERROR = "QuizActivity->onCreate error:\n";
    static final String QUIZ_RECEIVER_ON_RECEIVE_ERROR = "QuizReceiver->onReceive error:\n";

    static final String WIDGET_2_X_2_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "Widget2x2->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_2_X_2_UPDATE_APP_WIDGET_ERROR = "Widget2x2->updateAppWidget error:\n";
    static final String WIDGET_4_X_1_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "Widget4x1->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_4_X_1_UPDATE_APP_WIDGET_ERROR = "Widget4x1->updateAppWidget error:\n";
    static final String WIDGET_5_X_1_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "Widget5x1->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_5_X_1_UPDATE_APP_WIDGET_ERROR = "Widget5x1->updateAppWidget error:\n";
    static final String WIDGET_LIST_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "WidgetList->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_LIST_UPDATE_APP_WIDGET_ERROR = "WidgetList->updateAppWidget error:\n";
    static final String WIDGET_PHOTO_LIST_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "WidgetPhotoList->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_PHOTO_LIST_UPDATE_APP_WIDGET_ERROR = "WidgetPhotoList->updateAppWidget error:\n";
    static final String WIDGET_CONFIGURE_ACTIVITY_BUTTON_OK_ON_CLICK_ERROR = "WidgetConfigureActivity->buttonOkOnClick error:\n";
    static final String WIDGET_CONFIGURE_ACTIVITY_ON_CREATE_ERROR = "WidgetConfigureActivity->onCreate error:\n";
    static final String WIDGET_UPDATER_INVOKE_ERROR = "WidgetUpdater->invokePhotoEventsUpdate error:\n";
    static final String WIDGET_UPDATER_DRAW_EVENT_ERROR = "WidgetUpdater->drawEvent error:\n";

    static final String PARAM_APP_WIDGET_ID = "appWidgetId";
    static final String REGEX_PLUS = "\\+";

    static final String quiz_error_button_OK = "-##OK##";
    static final String FilePrefix_Media = "com.android.providers.media.documents";
    static final String FilePrefix_Downloads = "com.android.providers.downloads.documents";
    static final String FilePrefix_ExternalStorage = "com.android.externalstorage.documents";
    static final String FilePrefix_GooglePhotos = "com.google.android.apps.photos.content";

    protected static final String STRING_HTTP = "http://";
    protected static final String STRING_HTTPS = "https://";
    protected static final String RES_PACKAGE_ANDROID = "android";
}
