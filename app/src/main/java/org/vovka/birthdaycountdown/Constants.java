/*
 * *
 *  * Created by Vladimir Belov on 28.02.20 23:49
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 27.02.20 23:46
 *
 */

package org.vovka.birthdaycountdown;

class Constants {

    //Константы

    static final String STRING_EMPTY = "";
    static final String STRING_SPACE = " ";
    static final String STRING_NULL = "null";
    static final String STRING_EOF = "\n";
    static final String STRING_00 = "00";
    static final String STRING_2HASH = "##"; //https://coolefriend.com/know-names-of-symbols-in-your-computer-keyboard/
    static final String STRING_2TILDA = "~~";
    static final String STRING_COLON_SPACE = ": ";
    static final String STRING_COMMA = ",";
    static final String STRING_COMMA_SPACE = ", ";
    static final String STRING_PERIOD = ".";
    static final String REGEX_COMMAS = " *, *";
    static final String STRING_PARENTHESIS_OPEN = " (";
    static final String STRING_PARENTHESIS_START = "(";
    static final String STRING_PARENTHESIS_CLOSE = ")";
    static final String STRING_3MINUS = "--";
    static final String STRING_DIALOG_TAB = "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

    static final String ColumnNames_CONTACT_ID = "contact_id";
    static final String ColumnNames_ACCOUNT_TYPE = "account_type";
    static final String ColumnNames_ACCOUNT_NAME = "account_name";
    static final String ColumnNames_PHOTO_URI = "photo_uri";

    final static int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    final static int MY_PERMISSIONS_REQUEST__GET_ACCOUNTS = 101;

    static final int defaultNotificationID = 1000;
    static final String ACTION_SNOOZE = "ACTION_SNOOZE";
    static final String ACTION_NOTIFY = "ACTION_NOTIFY";
    static final String ACTION_LAUNCH = "LAUNCH_ACTIVITY";
    static final String EXTRA_NOTIFICATION_ID = "notificationID";
    static final String EXTRA_NOTIFICATION_DATA = "notificationData";

    static final int HTML_COLOR_RED = 1;
    static final int HTML_COLOR_YELLOW = 2;

    static final String HTML_BOLD_START = "<b>";
    static final String HTML_BOLD_END = "</b>";
    static final String HTML_BR = "<br>";
    static final String HTML_COLOR = "<font color=\"#%s\">%s</font>"; //https://dzone.com/articles/java-string-format-examples

    static final String Broadcast_ANDROID_INTENT_ACTION_TIME_SET = "android.intent.action.TIME_SET";
    static final String Broadcast_ANDROID_INTENT_ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    static final String Broadcast_ANDROID_INTENT_ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    static final String Broadcast_ANDROID_INTENT_ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    static final int pref_Events_Scope_NotHidden = 0;
    static final int pref_Events_Scope_All = 1;
    static final int pref_Events_Scope_Hidden = 2;

    //Сообщения

    static final String MSG_YEAR_MUST_BE_GREATER_0 = "Year must be > 0.";
    static final String MSG_NOTIFICATIONS_WERE_ENABLED = "Notifications were enabled\n";
    static final String MSG_NOTIFICATIONS_WERE_DISABLED = "Notifications were disabled\n";
    static final String MSG_WIDGET_PREFS_REMOVED = "Preferences for widget #%s has been removed";
    static final String MSG_WIDGET_PREFS_SAVED = "Saved widget #%s preferences: ";
    //static final String MSG_WIDGET_PREFS_DATA = "Prefs for widget #%s: ";
    static final String MSG_RINGTONE = "Ringtone: ";
    static final String MSG_DELETED_CHANNEL_ = "Deleted channel ";
    static final String MSG_CREATED_CHANNEL_ = "Created channel ";
    static final String MSG_SENT_WIDGETS_UPDATE_REQUEST = "Sent widgets update request";
    static final String MSG_NEXT_NOTIFICATION = "Next notification: ";

    //Ошибки

    static final String ALARM_RECEIVER_ON_RECEIVE_ERROR = "AlarmReceiver->onReceive error:\n";

    static final String CONTACTS_EVENTS_CHECK_IS_HIDDEN_EVENTS_ERROR = "ContactsEvents->checkIsHiddenEvents error:\n";
    static final String CONTACTS_EVENTS_CHECK_IS_HIDDEN_EVENT_ERROR = "ContactsEvents->checkIsHiddenEvent error:\n";
    static final String CONTACTS_EVENTS_COMPUTE_DATES_ERROR = "ContactsEvents->computeDates error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACTS_EVENTS_ERROR = "ContactsEvents->getContactsEvents error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_PHOTO_ERROR = "ContactsEvents->getContactPhoto error:\n";
    static final String CONTACTS_EVENTS_GET_CONTACT_NAME_ERROR = "ContactsEvents->getContactFirstName error:\n";
    static final String CONTACTS_EVENTS_GET_EVENT_DISTANCE_TEXT_ERROR = "ContactsEvents->getEventDistanceText error:\n";
    static final String CONTACTS_EVENTS_GET_HIDDEN_EVENTS_COUNT_ERROR = "ContactsEvents->getHiddenEventsCount error:\n";
    static final String CONTACTS_EVENTS_GET_PREFERENCES_ERROR = "ContactsEvents->getPreferences error:\n";
    static final String CONTACTS_EVENTS_INIT_BOOT_RECEIVER_ERROR = "ContactsEvents->initBootReceiver error:\n";
    static final String CONTACTS_EVENTS_INIT_NOTIFICATIONS_ERROR = "ContactsEvents->initNotifications error:\n";
    static final String CONTACTS_EVENTS_INIT_NOTIFICATION_CHANNEL_ERROR = "ContactsEvents->initNotificationChannel error:\n";
    static final String CONTACTS_EVENTS_INSERT_PREVIOUS_EVENTS_ERROR = "ContactsEvents->insertPreviousEvents error:\n";
    static final String CONTACTS_EVENTS_SET_HIDDEN_EVENT_ERROR = "ContactsEvents->setHiddenEvent error:\n";
    static final String CONTACTS_EVENTS_SET_WIDGET_PREFERENCE_ERROR = "ContactsEvents->setWidgetPreference error:\n";
    static final String CONTACTS_EVENTS_GET_WIDGET_PREFERENCE_ERROR = "ContactsEvents->getWidgetPreference error:\n";
    static final String CONTACTS_EVENTS_REMOVE_WIDGET_PREFERENCE_ERROR = "ContactsEvents->removeWidgetPreference error:\n";
    static final String CONTACTS_EVENTS_SET_LOCALE_ERROR = "ContactsEvents->setLocale error:\n";
    static final String CONTACTS_EVENTS_SET_PREFERENCES_ERROR = "ContactsEvents->setPreferences error:\n";
    static final String CONTACTS_EVENTS_SHOW_NOTIFICATIONS_ERROR = "ContactsEvents->showNotifications error:\n";
    static final String CONTACTS_EVENTS_SNOOZE_NOTIFICATION_ERROR = "ContactsEvents->SnoozeNotification error:\n";
    static final String CONTACTS_EVENTS_SHOW_NOTIFICATION_ERROR = "ContactsEvents->ShowNotification error:\n";
    static final String CONTACTS_EVENTS_COUNT_DAYS_DIFF_ERROR = "ContactsEvents->countDaysDiff error:\n";
    static final String CONTACTS_EVENTS_ADD_YEAR_ERROR = "ContactsEvents->addYear error:\n";
    static final String CONTACTS_EVENTS_COUNT_YEARS_DIFF_ERROR = "ContactsEvents->countYearsDiff error:\n";

    static final String DEVICE_BOOT_RECEIVER_ON_RECEIVE_ERROR = "DeviceBootReceiver->onReceive error:\n";
    static final String DATE_RECEIVER_ON_RECEIVE_ERROR = "DateReceiver->onReceive error:\n";

    static final String GET_ACCOUNTS_LIST_ADAPTER_GET_VIEW_ERROR = "GetAccountsListAdapter->getView error:\n";

    static final String MAIN_ACTIVITY_DRAW_LIST_ERROR = "MainActivity->drawList error:\n";
    static final String MAIN_ACTIVITY_DRAW_LIST_ON_ITEM_CLICK_ERROR = "MainActivity->drawList->onItemClick error:\n";
    static final String MAIN_ACTIVITY_INIT_NOTIFICATIONS_ERROR = "MainActivity->initNotifications error:\n";
    static final String MAIN_ACTIVITY_ON_BACK_PRESSED_ERROR = "MainActivity->onBackPressed error:\n";
    static final String MAIN_ACTIVITY_ON_CONTEXT_ITEM_SELECTED_ERROR = "MainActivity->onContextItemSelected error:\n";
    static final String MAIN_ACTIVITY_ON_CREATE_CONTEXT_MENU_ERROR = "MainActivity->onCreateContextMenu error:\n";
    static final String MAIN_ACTIVITY_ON_CREATE_ERROR = "MainActivity->onCreate error:\n";
    static final String MAIN_ACTIVITY_ON_OPTIONS_ITEM_SELECTED_ERROR = "MainActivity->onOptionsItemSelected error:\n";
    static final String MAIN_ACTIVITY_ON_REFRESH_ERROR = "MainActivity->onRefresh error:\n";
    static final String MAIN_ACTIVITY_ON_RESUME_ERROR = "MainActivity->onResume error:\n";
    static final String MAIN_ACTIVITY_SET_HINT_ERROR = "MainActivity->setHint error:\n";
    static final String MAIN_ACTIVITY_SET_HTML_COLOR = "MainActivity->setHTMLColor error:\n";
    static final String MAIN_ACTIVITY_CHECK_NEW_VERSION_ERROR = "MainActivity->checkNewVersion error:\n";
    static final String MAIN_ACTIVITY_SET_LASTRUN_VERSION_ERROR = "MainActivity->setLastRunVersion error:\n";
    static final String MAIN_ACTIVITY_SHOW_WELCOME_SCREEN_ERROR = "MainActivity->showWelcomeScreen error:\n";

    static final String MY_ADAPTER_GET_VIEW_ERROR = "MyAdapter->getView error:\n";
    static final String MY_ADAPTER_GET_VIEW_ERROR_ABNORMAL_DIMENSION_OF_STRING_END = "\nData: ";
    static final String MY_ADAPTER_GET_VIEW_ERROR_ABNORMAL_DIMENSION_OF_STRING_START = "MyAdapter->getView error:\nAbnormal dimension of string:\n";

    static final String PERSON_CONSTRUCTOR_ERROR = "Person->Constructor error:\n";
    static final String PERSON_GET_FULL_NAME_ALT_ERROR = "Person->getFullNameAlt error:\n";
    static final String PERSON_GET_FULL_NAME_ERROR = "Person->getFullName error: ";
    static final String PERSON_GET_FULL_NAME_SHORT_ERROR = "Person->getFullNameShort error:\n";
    static final String PERSON_GET_GENDER_ERROR = "Person->getGender error:\n";

    static final String SETTINGS_ACTIVITY_ON_CREATE_ERROR = "SettingsActivity->onCreate error:\n";
    static final String SETTINGS_ACTIVITY_ON_PREFERENCE_TREE_CLICK_ERROR = "SettingsActivity->onPreferenceTreeClick error:\n";
    static final String SETTINGS_ACTIVITY_SET_UP_NESTED_SCREEN_ERROR = "SettingsActivity->setUpNestedScreen error:\n";
    static final String SETTINGS_ACTIVITY_ON_STOP_ERROR = "SettingsActivity->onStop error:\n";
    static final String SETTINGS_ACTIVITY_GET_ACCOUNTS_ERROR = "SettingsActivity->getAccounts error:\n";
    static final String SHOW_ANNIVERSARY_LIST_ERROR = "SettingsActivity->showAnniversaryList error:\n";

    static final String ABOUT_ACTIVITY_ON_CREATE_ERROR = "AboutActivity->onCreate error:\n";

    static final String WIDGET_2_X_2_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "Widget2x2->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_2_X_2_UPDATE_APP_WIDGET_ERROR = "Widget2x2->updateAppWidget error:\n";
    static final String WIDGET_4_X_1_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "Widget4x1->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_4_X_1_UPDATE_APP_WIDGET_ERROR = "Widget4x1->updateAppWidget error:\n";
    static final String WIDGET_5_X_1_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR = "Widget5x1->onAppWidgetOptionsChanged error:\n";
    static final String WIDGET_5_X_1_UPDATE_APP_WIDGET_ERROR = "Widget5x1->updateAppWidget error:\n";
    static final String WIDGET_CONFIGURE_ACTIVITY_BUTTON_OK_ON_CLICK_ERROR = "WidgetConfigureActivity->buttonOkOnClick error:\n";
    static final String WIDGET_CONFIGURE_ACTIVITY_ON_CREATE_ERROR = "WidgetConfigureActivity->onCreate error:\n";
    static final String WIDGET_UPDATER_INVOKE_ERROR = "WidgetUpdater->invoke error:\n";
    static final String WIDGET_UPDATER_DRAW_EVENT_ERROR = "WidgetUpdater->drawEvent error:\n";
}
