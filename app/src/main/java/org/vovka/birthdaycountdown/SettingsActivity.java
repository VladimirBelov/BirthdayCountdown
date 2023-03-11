/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 17.09.2022, 0:21
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


@SuppressWarnings("deprecation")
public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    //https://stackoverflow.com/questions/26564400/creating-a-preference-screen-with-support-v21-toolbar

    private static final String TAG = "SettingsActivity";
    private String testChannelId = Constants.STRING_EMPTY;
    private TypedArray ta = null;
    private static DisplayMetrics displayMetrics;
    private ContactsEvents eventsData;
    private String eventTypeForSelect;
    private Set<String> filesList;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources applicationRes = getBaseContext().getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeMain);

            setDisplayMetrics(this.getResources().getDisplayMetrics());
            setContentView(R.layout.activity_settings);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            Window w = getWindow();
            w.setStatusBarColor(ta.getColor(R.styleable.Theme_windowStatusbarColor, 0)); //почему-то сама из темы не ставится
            w.setNavigationBarColor(ta.getColor(R.styleable.Theme_windowStatusbarColor, 0));
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setHomeButtonEnabled(true);
                bar.setDisplayHomeAsUpEnabled(true);
                bar.setDisplayShowTitleEnabled(true);
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back);
                bar.setTitle(R.string.window_settings);
            }

            if (eventsData.preferences_menustyle_compact) {
                addPreferencesFromResource(R.xml.settings_compact);
            } else {
                addPreferencesFromResource(R.xml.settings);
            }
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            Preference notificationSoundPref = findPreference(getString(R.string.pref_Notifications_Ringtone_key));
            if (notificationSoundPref != null) {
                notificationSoundPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    eventsData.getPreferences();
                    //Toast.makeText(this, eventsData.preferences_notifications_ringtone, Toast.LENGTH_LONG).show();
                    if (eventsData.preferences_notifications_ringtone.contains("/media/external/") &&
                            eventsData.checkNoStorageAccess()) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                            builder.setTitle(getString(R.string.msg_no_access_contacts));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setMessage(getString(R.string.msg_no_access_storage_hint));
                            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                            builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                                try {
                                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(Constants.URI_PACKAGE + this.getPackageName())));
                                } catch (android.content.ActivityNotFoundException e) { /**/ }
                            });
                            AlertDialog alertToShow = builder.create();
                            alertToShow.setOnShowListener(arg0 -> {
                                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                            });
                            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            alertToShow.show();

                        } else {

                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                        }

                    }
                    return true;
                });
            }


        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.getPreferences();

            updateTitles();
            updateVisibility();
        }
    }

/*    @Override
    public void onBackPressed()
    {
        //если используется compacted menu, то без этого после recreate на выходе из активности по back nav key вылетает ANR
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        //super.onBackPressed();
        finish();
    }*/

    @Override
    protected void onDestroy() {
        if (ta != null) ta.recycle();
        super.onDestroy();
    }

    private void updateTitles() {

        try {

            PreferenceCategory prefCat;

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Custom1_key));
            if (!eventsData.preferences_customevent1_caption.isEmpty()) {
                prefCat.setTitle(eventsData.preferences_customevent1_caption);
            } else {
                prefCat.setTitle(getString(R.string.pref_CustomEvents_Custom_title));
            }

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Custom2_key));
            if (!eventsData.preferences_customevent2_caption.isEmpty()) {
                prefCat.setTitle(eventsData.preferences_customevent2_caption);
            } else {
                prefCat.setTitle(getString(R.string.pref_CustomEvents_Custom_title));
            }

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Custom3_key));
            if (!eventsData.preferences_customevent3_caption.isEmpty()) {
                prefCat.setTitle(eventsData.preferences_customevent3_caption);
            } else {
                prefCat.setTitle(getString(R.string.pref_CustomEvents_Custom_title));
            }

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Custom4_key));
            if (!eventsData.preferences_customevent4_caption.isEmpty()) {
                prefCat.setTitle(eventsData.preferences_customevent4_caption);
            } else {
                prefCat.setTitle(getString(R.string.pref_CustomEvents_Custom_title));
            }

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Custom5_key));
            if (!eventsData.preferences_customevent5_caption.isEmpty()) {
                prefCat.setTitle(eventsData.preferences_customevent5_caption);
            } else {
                prefCat.setTitle(getString(R.string.pref_CustomEvents_Custom_title));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateVisibility() {

        try {

            PreferenceCategory prefCat;
            Preference pref;
            eventsData.preferences_notifications_days.removeAll(new HashSet<String>() {{add("");}});
            boolean isNotifyEnabled = eventsData.preferences_notifications_days.size() > 0;

            this.setTheme(eventsData.preferences_theme.themeMain);

            final boolean pref_menu_isCompact = eventsData.preferences_menustyle_compact;
            if (pref_menu_isCompact) {
                PreferenceScreen prefScreen = (PreferenceScreen) findPreference(getString(R.string.pref_Notifications_key));
                if (prefScreen != null) {
                    pref = findPreference(getString(R.string.pref_Notifications_Type_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_Priority_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_Events_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_AlarmHour_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_QuickActions_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_Ringtone_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_OnClick_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);

                    pref = findPreference(getString(R.string.pref_Notifications_NotifyTest_key));
                    if (pref != null) pref.setEnabled(isNotifyEnabled);
                }

            } else {
                prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_Notifications_key));
                if (prefCat != null && !isNotifyEnabled) {
                    pref = findPreference(getString(R.string.pref_Notifications_Type_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_Priority_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_Events_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_AlarmHour_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_QuickActions_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_Ringtone_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_OnClick_key));
                    if (pref != null) prefCat.removePreference(pref);

                    pref = findPreference(getString(R.string.pref_Notifications_NotifyTest_key));
                    if (pref != null) prefCat.removePreference(pref);
                }

            }

            /*
            //Динамическое скрытие настроек
            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Birthday_key));
            if (prefCat != null) {

                if (eventsData.preferences_BirthDay_calendars.size() == 0
                        && eventsData.preferences_MultiType_calendars.size() == 0) {

                    pref = findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key));
                    if (pref != null) prefCat.removePreference(pref);

                } else {

                    if (findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key)) == null) {
                        pref = new CustomEditTextPreference(eventsData.getContext());
                        pref.setTitle(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_title));
                        pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_summary));
                        pref.setKey(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key));
                        prefCat.addPreference(pref);
                    }

                    //Удаляем "Файлы", чтобы они могли (ниже) создастся после календарных настроек
                    pref = findPreference(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key));
                    if (pref != null) prefCat.removePreference(pref);

                }

                if (eventsData.preferences_BirthDay_calendars.size() == 0) {

                    pref = findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key));
                    if (pref != null) prefCat.removePreference(pref);

                } else {

                    if (findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key)) == null) {
                        pref = new SwitchPreference(eventsData.getContext());
                        pref.setTitle(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_title));
                        pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_description));
                        pref.setKey(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key));
                        prefCat.addPreference(pref);
                    }

                    //Удаляем "Файлы", чтобы они могли (ниже) создасться после календарных настроек
                    pref = findPreference(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key));
                    if (pref != null) prefCat.removePreference(pref);

                }

                if (findPreference(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key)) == null) {
                    pref = new Preference(eventsData.getContext());
                    pref.setTitle(getString(R.string.pref_CustomEvents_LocalFiles_title));
                    pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_description));
                    pref.setKey(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key));
                    prefCat.addPreference(pref);
                }

            }*/

            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Common_key, R.string.pref_Icon_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Common_key, R.string.pref_List_DateFormat_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Common_key, R.string.pref_Female_Names_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Common_key, R.string.pref_Male_Names_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Common_key, R.string.pref_List_NameFormat_key);

            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_key, R.string.pref_CustomEvents_Rules_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_Birthday_key, R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_Custom1_key, R.string.pref_CustomEvents_Custom1_UseYear_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_Custom2_key, R.string.pref_CustomEvents_Custom2_UseYear_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_Custom3_key, R.string.pref_CustomEvents_Custom3_UseYear_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_Custom4_key, R.string.pref_CustomEvents_Custom4_UseYear_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_CustomEvents_Custom5_key, R.string.pref_CustomEvents_Custom5_UseYear_key);

            hidePreference(!eventsData.preferences_extrafun, R.string.pref_EventList_key, R.string.pref_List_CustomCaption_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_EventList_key, R.string.pref_List_CustomTodayEventCaption_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_EventList_key, R.string.pref_List_OnClick_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_EventList_key, R.string.pref_List_FastScroll_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_EventList_key, R.string.pref_List_Margin_key);

            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Widgets_key, R.string.pref_Widgets_Days_EventSoon_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Widgets_key, R.string.pref_Widgets_OnClick_key);

            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Notifications_key, R.string.pref_Notifications_Priority_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Notifications_key, R.string.pref_Notifications_QuickActions_key);
            hidePreference(!eventsData.preferences_extrafun, R.string.pref_Notifications_key, R.string.pref_Notifications_OnClick_key);

            hidePreference(eventsData.checkNoBatteryOptimization(), R.string.pref_Help_key, R.string.pref_Help_BatteryOptimization_key);
            hidePreference(!eventsData.checkNoContactsAccess(), R.string.pref_Help_key, R.string.pref_Help_ContactsAccess_key);
            hidePreference(!eventsData.checkNoCalendarAccess(), R.string.pref_Help_key, R.string.pref_Help_CalendarAccess_key);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void hidePreference(boolean condition, @StringRes int parentId, @StringRes int resId) {

        try {

            if (!condition) return;

            Preference pref = findPreference(getString(resId));
            if (pref != null) {
                Preference prefParent = findPreference(getString(parentId));
                if (prefParent != null) {
                    if (prefParent instanceof PreferenceScreen) {
                        ((PreferenceScreen) prefParent).removePreference(pref);
                    } else if (prefParent instanceof PreferenceCategory) {
                        ((PreferenceCategory) prefParent).removePreference(pref);
                    }

                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @Override
    protected void onPause() {

        super.onPause();
        if (getPreferenceScreen() != null)
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        super.onPreferenceTreeClick(preferenceScreen, preference);

        try {

            String key = preference.getKey();

            if (preference instanceof PreferenceScreen) { //Подуровень

                setUpNestedScreen((PreferenceScreen) preference);

            } else if (getString(R.string.pref_Notifications_NotifyTest_key).equals(key)) { //Уведомления

                testNotify();
                return true;

            } else if (getString(R.string.pref_FAQActivity_key).equals(key)) { //FAQ

                Intent intent = new Intent(this, FAQActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }

            } else if (getString(R.string.pref_AboutActivity_key).equals(key)) { //О приложении

                Intent intent = new Intent(this, AboutActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }

            } else if (getString(R.string.pref_Accounts_key).equals(key)) { //Аккаунты

                if (eventsData.checkNoContactsAccess()) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.GET_ACCOUNTS}, Constants.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    return true;
                }

                selectAccounts();
                return true;

            } else if (getString(R.string.pref_Theme_key).equals(key)) { //Цветовая тема

                selectTheme();
                return true;

            } else if (getString(R.string.pref_Icon_key).equals(key)) { //Иконка приложения

                selectIcon();
                return true;

            } else if (getString(R.string.pref_IconPack_key).equals(key)) { //Силуэты

                selectIconPack();
                return true;

            } else if (getString(R.string.pref_CustomEvents_Anniversary_List_key).equals(key)) { //Список всех годовщин свадеб

                eventsData.showAnniversaryList(this);
                return true;

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_key).equals(key)) { //Календари (Дни рождения)

                this.eventTypeForSelect = ContactsEvents.getEventType(Constants.Type_BirthDay);
                if (eventsData.checkNoCalendarAccess()) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);

                } else {

                    selectCalendars(this.eventTypeForSelect);

                }
                return true;

            } else if (getString(R.string.pref_CustomEvents_Other_Calendars_key).equals(key)) { //Календари (Другие события)

                this.eventTypeForSelect = ContactsEvents.getEventType(Constants.Type_Other);

                if (eventsData.checkNoCalendarAccess()) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                    return true;

                } else {

                    selectCalendars(this.eventTypeForSelect);
                }

            } else if (getString(R.string.pref_CustomEvents_MultiType_Calendars_key).equals(key)) { //Календари (Разные события)

                this.eventTypeForSelect = Constants.Type_MultiEvent;

                if (eventsData.checkNoCalendarAccess()) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                    return true;

                } else {

                    selectCalendars(this.eventTypeForSelect);
                }

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key).equals(key)) {

                editRules();
                return true;

            } else if (getString(R.string.pref_Help_BatteryOptimization_key).equals(key)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) { /**/ }
                }

            } else if (getString(R.string.pref_Help_ContactsAccess_key).equals(key)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.GET_ACCOUNTS},
                            Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS_2
                    );
                } else {
                    try {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(Constants.URI_PACKAGE + this.getPackageName())));
                    } catch (android.content.ActivityNotFoundException e) { /**/ }
                }

            } else if (getString(R.string.pref_Help_CalendarAccess_key).equals(key)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.READ_CALENDAR},
                            Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR_2
                    );
                } else {
                    try {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(Constants.URI_PACKAGE + this.getPackageName())));
                    } catch (android.content.ActivityNotFoundException e) { /**/ }
                }

            } else if (getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key).equals(key)) {

                if (eventsData.preferences_Birthday_files != null) {
                    filesList = new HashSet<>(eventsData.preferences_Birthday_files);
                } else {
                    filesList = new HashSet<>();
                }
                this.eventTypeForSelect = ContactsEvents.getEventType(Constants.Type_BirthDay);
                selectFiles(this.eventTypeForSelect);
                return true;

            } else if (getString(R.string.pref_CustomEvents_Other_LocalFiles_key).equals(key)) {

                if (eventsData.preferences_OtherEvent_files != null) {
                    filesList = new HashSet<>(eventsData.preferences_OtherEvent_files);
                } else {
                    filesList = new HashSet<>();
                }
                this.eventTypeForSelect = ContactsEvents.getEventType(Constants.Type_Other);
                selectFiles(this.eventTypeForSelect);
                return true;

            } else if (getString(R.string.pref_CustomEvents_MultiType_LocalFiles_key).equals(key)) {

                if (eventsData.preferences_MultiType_files != null) {
                    filesList = new HashSet<>(eventsData.preferences_MultiType_files);
                } else {
                    filesList = new HashSet<>();
                }
                this.eventTypeForSelect = Constants.Type_MultiEvent;
                selectFiles(this.eventTypeForSelect);
                return true;

            } else if (getString(R.string.pref_Notifications_Ringtone_key).equals(key)) {

                selectRingtone();
                return true;

            } else if (getString(R.string.pref_Notifications_AlarmHour_key).equals(key)) {

                selectAlarmTime();
                return true;

            } else if (getString(R.string.pref_List_FontMagnify_key).equals(key)) {

                selectFontMagnify();
                return true;

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return false;
    }

    @Override
    public void onStop() {

        try {
            //удаляем временный канал оповещений
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !testChannelId.equals(Constants.STRING_EMPTY)) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null && notificationManager.getNotificationChannel(testChannelId) != null) {
                    notificationManager.deleteNotificationChannel(testChannelId);
                }
            }

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            super.onStop();
        }

    }

    @Override
    public void applyOverrideConfiguration(@Nullable Configuration overrideConfiguration) {
        //https://stackoverflow.com/questions/57973627/configuration-setlocalelocale-doesnt-work-with-appcompatdelegate-setdefaultni
        if (overrideConfiguration != null) {
            int uiMode = overrideConfiguration.uiMode;
            overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
            overrideConfiguration.uiMode = uiMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        try {
            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.statLastPausedForOtherActivity = 0;
            eventsData.getPreferences();
            eventsData.needUpdateEventList = true;

            if (getString(R.string.pref_Language_key).equals(key)) {

                //https://stackoverflow.com/questions/2486934/programmatically-relaunch-recreate-an-activity
                //не доверяйте this.recreate(), если в настройках несколько вложенных PreferenceScreen!
                Intent intent = getIntent();
                finish();
                startActivity(intent);

            } else if (getString(R.string.pref_Theme_key).equals(key)) {

                this.setTheme(eventsData.preferences_theme.themeMain);
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                //todo: созданные программно настройки не подхватывают стиль

            } else if (getString(R.string.pref_MenuStyle_key).equals(key)) {

                Intent intent = getIntent();
                finish();
                startActivity(intent);

            } else if (getString(R.string.pref_Help_ExtraFun_On_key).equals(key)) {

                Intent intent = getIntent();
                finish();
                startActivity(intent);

            } else if (getString(R.string.pref_CustomEvents_Custom1_Caption_key).equals(key) ||
                    getString(R.string.pref_CustomEvents_Custom2_Caption_key).equals(key) ||
                    getString(R.string.pref_CustomEvents_Custom3_Caption_key).equals(key) ||
                    getString(R.string.pref_CustomEvents_Custom4_Caption_key).equals(key) ||
                    getString(R.string.pref_CustomEvents_Custom5_Caption_key).equals(key)) {

                updateTitles();
                updateVisibility();

            } else if (getString(R.string.pref_Notifications_Days_key).equals(key)) {

                if (eventsData.preferences_notifications_days.size() > 0) {
                    //Нет доступа
                    if (eventsData.checkNoNotificationAccess() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, Constants.MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
                    }
                    //Уведомления выключены
                    if (!NotificationManagerCompat.from(this).areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS, Uri.parse(Constants.URI_PACKAGE + this.getPackageName())));
                        } catch (ActivityNotFoundException e) { /**/ }
                    }

                }

                if (eventsData.preferences_menustyle_compact) {
                    updateVisibility();
                } else {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_key).equals(key)
                    || getString(R.string.pref_CustomEvents_MultiType_Calendars_key).equals(key)) {

                updateVisibility();

            }
        /* bug. вот так с выбором рингтона не работает https://stackoverflow.com/questions/6725105/ringtonepreference-not-firing-onsharedpreferencechanged
        else if (getString(R.string.pref_Notifications_Ringtone_key).equals(key)) {
        }*/
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    //https://stackoverflow.com/questions/46003114/how-should-one-request-permissions-from-a-custom-preference
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) return;

        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS) {

            selectAccounts();

        } else if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR) {

            if (this.eventTypeForSelect != null && !this.eventTypeForSelect.isEmpty()) selectCalendars(this.eventTypeForSelect);

        } else if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR_2 || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS_2) {

            updateVisibility();

        }

    }

    private void setUpNestedScreen(@NonNull PreferenceScreen preferenceScreen) {

        try {
            //Добавляем тулбар
            //https://code.i-harness.com/en/q/1cfc0dc

            Dialog dialog = preferenceScreen.getDialog();
            ListView list = dialog.findViewById(android.R.id.list);
            Toolbar bar;
            //this.setTheme(eventsData.preferences_theme.themeMain);
            //ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) { //Для Android > 6

                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) list.getLayoutParams();
                marginParams.setMargins(0, (int) (48 * displayMetrics.density + 0.5f), 0, 0);
                list.setPadding(0, (int) (10 * displayMetrics.density + 0.5f), 0, 0);
                ViewGroup root = (ViewGroup) list.getParent();
                bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(bar, 0); // insert at top
                bar.setTitle(preferenceScreen.getTitle());
                bar.setNavigationOnClickListener(v -> dialog.dismiss());
                root.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.white)));

            } else { //Для Android <= 6

                list.setPadding(0, (int) (10 * displayMetrics.density + 0.5f), 0, 0);
                LinearLayout root = (LinearLayout) list.getParent();
                bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(bar, 0); // insert at top
                bar.setTitle(preferenceScreen.getTitle());
                bar.setNavigationOnClickListener(v -> dialog.dismiss());
                root.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.white)));

            }
            list.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, ContextCompat.getColor(this, R.color.light_gray_transp))));
            list.setDividerHeight((int) (1 * displayMetrics.density));

            if (preferenceScreen.getKey().equals(getString(R.string.pref_CustomEvents_key))) {
                if (eventsData.isContextHelpAvailable()) {
                    bar.setPopupTheme(eventsData.preferences_theme.themePopup);
                    bar.inflateMenu(R.menu.menu_settings);
                    bar.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.menu_help_events_calendar) {

                            Intent intent = new Intent(this, FAQActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            intent.putExtra(Constants.EXTRA_ANCHOR, getString(R.string.faq_anchor_events_calendar));
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) { /**/ }

                        } else if (item.getItemId() == R.id.menu_help_events_files) {

                            Intent intent = new Intent(this, FAQActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            intent.putExtra(Constants.EXTRA_ANCHOR, getString(R.string.faq_anchor_events_files));
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) { /**/ }

                        }
                        return false;
                    });
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void testNotify() {
        ContactsEvents eventsData = ContactsEvents.getInstance();
        eventsData.getPreferences(); //перечитываем настройки, если их меняли для показа уведомлений

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationManager notificationManager = getSystemService(NotificationManager.class);

                if (notificationManager != null) {

                    //если был предыдущий тест
                    if (!testChannelId.equals(Constants.STRING_EMPTY) && notificationManager.getNotificationChannel(testChannelId) != null) {
                        notificationManager.deleteNotificationChannel(testChannelId);
                    }

                    Random r = new Random();
                    testChannelId = Integer.toString(r.nextInt(1000));

                    NotificationChannel channel = new NotificationChannel(testChannelId, getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription(getString(R.string.pref_Notifications_Notification_Channel_Description));
                    channel.setSound(Uri.parse(eventsData.preferences_notifications_ringtone), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                    channel.enableVibration(true);

                    notificationManager.createNotificationChannel(channel);

                }
            }
            eventsData.showNotifications(true, testChannelId);

        } else {
            ToastExpander.showText(this, getString(R.string.msg_notifications_disabled));
        }
    }

    private void selectAccounts() {

        try {

            List<String> accountNames = new ArrayList<>();
            List<Integer> accountIcons = new ArrayList<>();
            List<String> accountPackages = new ArrayList<>();
            List<String> choiceList = new ArrayList<>();
            ContactsEvents eventsData = ContactsEvents.getInstance();

            if (!eventsData.checkNoContactsAccess()) {
                //https://stackoverflow.com/questions/10657096/how-to-get-an-icon-associated-with-specific-account-from-accountmanager-getaccou

                AuthenticatorDescription[] descriptions = AccountManager.get(this).getAuthenticatorTypes();

                //user's online accounts
                Account[] accounts = AccountManager.get(this).getAccounts();
                for (Account account : accounts) {

                    final String accountName = account.name + Constants.STRING_PARENTHESIS_OPEN + account.type + Constants.STRING_PARENTHESIS_CLOSE;
                    accountNames.add(accountName);
                    choiceList.add(accountName
                            + Constants.STRING_BRACKETS_OPEN
                            + eventsData.getContactsEventsCount(account.type, account.name)
                            + Constants.STRING_BRACKETS_CLOSE
                    );
                    for (AuthenticatorDescription desc : descriptions) {
                        if (account.type.equals(desc.type)) {
                            accountIcons.add(desc.iconId > 0 ? desc.iconId : desc.smallIconId);
                            accountPackages.add(desc.packageName);
                            break;
                        }
                    }
                    if (accountNames.size() != accountIcons.size()) { //Не нашли иконку
                        accountIcons.add(0);
                        accountPackages.add(Constants.STRING_EMPTY);
                    }
                }

                //raw accounts
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                Cursor cursor = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI,
                        new String[]{ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE},
                        "deleted=0",
                        null,
                        null);
                Set<String> accountsList = new HashSet<>();
                if (cursor != null && cursor.getCount() >0) {

                    if (cursor.moveToFirst()) {
                        final int columnName = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME);
                        final int columnType = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE);
                        do {
                            String accountString = cursor.getString(columnName)
                                    + Constants.STRING_PARENTHESIS_OPEN
                                    + cursor.getString(columnType)
                                    + Constants.STRING_PARENTHESIS_CLOSE;

                            accountsList.add(accountString);

                        } while (cursor.moveToNext());
                        cursor.close();
                    }

                }
                for (String accountString: accountsList) {
                    if (!accountNames.contains(accountString)) {
                        accountNames.add(accountString);
                        choiceList.add(accountString
                                + Constants.STRING_BRACKETS_OPEN
                                + eventsData.getContactsEventsCount(accountString.substring(accountString.indexOf(Constants.STRING_PARENTHESIS_OPEN) + Constants.STRING_PARENTHESIS_OPEN.length(), accountString.indexOf(Constants.STRING_PARENTHESIS_CLOSE)), null)
                                + Constants.STRING_BRACKETS_CLOSE
                        );
                        if (accountString.toLowerCase().contains(Constants.account_sim)) {
                            accountIcons.add(R.drawable.sim_card);
                        } else {
                            accountIcons.add(R.drawable.emo_im_happy);
                        }
                        accountPackages.add(getPackageName());
                    }
                }
                accountsList.add(getString(R.string.msg_none));
                choiceList.add(getString(R.string.msg_none));
                accountNames.add(Constants.account_none);
                accountIcons.add(android.R.drawable.ic_delete);
                accountPackages.add(getPackageName());


            }

            if (accountNames.size() > 0) {
                ListAdapter adapter = new AccountsListAdapter(this, choiceList, accountIcons, accountPackages, ta);

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.pref_Accounts_title)
                        .setIcon(android.R.drawable.ic_menu_my_calendar)
                        .setAdapter(adapter, null)
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                            //https://stackoverflow.com/questions/8326830/how-to-uncheck-item-checked-by-setitemchecked
                            SparseBooleanArray checked = ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                            Set<String> checkedAccounts = new HashSet<>();

                            for (int i = 0; i < checked.size(); i++) {
                                if (checked.get(checked.keyAt(i))) {
                                    checkedAccounts.add(accountNames.get(checked.keyAt(i)));
                                }
                            }
                            eventsData.setPreferences_Accounts(checkedAccounts);
                            eventsData.savePreferences();

                        })
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                        .setNeutralButton(getString(R.string.button_all) + Constants.STRING_BRACKETS_OPEN
                                + eventsData.getContactsEventsCount(null, null)
                                + Constants.STRING_BRACKETS_CLOSE, (dialog, which) -> {
                            eventsData.setPreferences_Accounts(new HashSet<>());
                            eventsData.savePreferences();
                        })
                        .setCancelable(true);

                AlertDialog alertToShow = builder.create();

                ListView listView = alertToShow.getListView();
                listView.setItemsCanFocus(false);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                    //Только здесь работает

                    final Set<String> preferences_accounts = eventsData.getPreferences_Accounts();
                    for (int i = 0; i < accountNames.size(); i++) {
                        if (preferences_accounts.isEmpty() || preferences_accounts.contains(accountNames.get(i))) {
                            listView.setItemChecked(i, true);
                        }
                    }
                    if (preferences_accounts.isEmpty()) {
                        listView.setItemChecked(accountNames.size() - 1, false);
                    }

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        if (position == listView.getCount() - 1) {
                            for (int i = 0; i < accountNames.size(); i++) {
                                listView.setItemChecked(i, i >= accountNames.size() - 1);
                            }
                        } else {
                            listView.setItemChecked(accountNames.size() - 1, false);
                        }
                    });
                });

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.pref_Accounts_title)
                        .setMessage(R.string.msg_no_accounts_hint)
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel())
                        .setCancelable(true);

                AlertDialog alertToShow = builder.create();

                alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectTheme() {

       try {

           List<String> themeNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_Theme_entries)));
           List<String> themeNumbers = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_Theme_values)));
           List<Integer> themeColors = new ArrayList<>();
           themeColors.add(getResources().getColor(R.color.theme_brown_primary));
           themeColors.add(getResources().getColor(R.color.theme_orange_primary));
           themeColors.add(getResources().getColor(R.color.theme_green_primary));
           themeColors.add(getResources().getColor(R.color.theme_teal_primary));
           themeColors.add(getResources().getColor(R.color.theme_blue_primary));
           themeColors.add(getResources().getColor(R.color.theme_indigo_primary));
           themeColors.add(getResources().getColor(R.color.theme_blue_gray_primary));
           themeColors.add(getResources().getColor(R.color.theme_grey_primary));
           themeColors.add(getResources().getColor(R.color.theme_black_primary));

           ListAdapter adapter = new ThemeListAdapter(this, themeNames, themeColors, ta);

           AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                   .setTitle(R.string.pref_Theme_title)
                   .setAdapter(adapter, null)
                   .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                   .setCancelable(true);

           AlertDialog alertToShow = builder.create();

           ListView listView = alertToShow.getListView();
           listView.setItemsCanFocus(false);
           listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

           listView.setOnItemClickListener((parent, view, position, id) -> {
               String s1 = themeNumbers.get(position);
               eventsData.setPreferences_ThemeNumber(Integer.parseInt(themeNumbers.get(position)));
               eventsData.savePreferences();
               alertToShow.dismiss();
           });

           alertToShow.setOnShowListener(arg0 -> {
               alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
               listView.setItemChecked(themeNumbers.indexOf(Integer.toString(eventsData.preferences_theme.prefNumber)), true);
           });

           alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
           alertToShow.show();

       } catch (Exception e) {
           Log.e(TAG, e.getMessage(), e);
           if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
       }
    }

    private void selectIconPack() {

        try {

            List<String> packNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_IconPack_entries)));
            List<Integer> packIcons = new ArrayList<>();
            packIcons.add(R.drawable.ic_pack00_f1);
            packIcons.add(R.drawable.ic_pack01_f2);
            packIcons.add(R.drawable.ic_pack02_f2);
            packIcons.add(R.drawable.ic_pack03_f3);

            ListAdapter adapter = new ImageSelectAdapter(this, packNames, packIcons, false, ta);

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_IconPack_title)
                    .setAdapter(adapter, null)
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();

            ListView listView = alertToShow.getListView();
            listView.setItemsCanFocus(false);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                eventsData.setPreferences_IconPackNumber(position);
                eventsData.savePreferences();
                eventsData.initIconPack();
                alertToShow.dismiss();
            });

            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                listView.setItemChecked(eventsData.getPreferences_IconPackNumber(), true);
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectIcon() {

        try {
            List<String> iconNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_Icon_entries)));
            List<String> iconIDs = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_Icon_values)));
            List<Integer> iconImages = new ArrayList<>();
            iconImages.add(R.mipmap.ic_launcher_spring_round);
            iconImages.add(R.mipmap.ic_launcher_summer_round);
            iconImages.add(R.mipmap.ic_launcher_winter_round);
            iconImages.add(R.mipmap.ic_launcher_grey_round);
            iconImages.add(R.mipmap.ic_launcher_black_round);

            ListAdapter adapter = new ImageSelectAdapter(this, iconNames, iconImages, true, ta);

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_Icon_title)
                    .setAdapter(adapter, null)
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();

            ListView listView = alertToShow.getListView();
            listView.setItemsCanFocus(false);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                eventsData.setPreferences_Icon(iconIDs.get(position));
                eventsData.savePreferences();
                alertToShow.dismiss();
                eventsData.setAppIcon();
            });

            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                listView.setItemChecked(iconIDs.indexOf(eventsData.preferences_icon), true);
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectCalendars(String eventType) {

        try {

            eventsData.recieveCalendarList();

            if (eventsData.map_calendars.size() == 0) {

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.pref_CustomEvents_Calendars_title)
                        .setMessage(R.string.msg_no_calendars_hint)
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel())
                        .setCancelable(true);

                AlertDialog alertToShow = builder.create();
                alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));
                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
                return;
            }

            ArrayList<String> calIDs = new ArrayList<>();
            ArrayList<String> calTitles = new ArrayList<>();
            ArrayList<Boolean> calSelected = new ArrayList<>();

            Set<String> preferences_calendars = eventsData.getPreferences_Calendars(eventType);
            boolean[] sel = new boolean[eventsData.map_calendars.size()];
            int ind = 0;
            for (Map.Entry<String,String> entry: eventsData.map_calendars.entrySet()) {
                calIDs.add(entry.getKey());
                calTitles.add(
                        entry.getValue().replace(Constants.STRING_EOT, Constants.STRING_PARENTHESIS_OPEN)
                                + Constants.STRING_PARENTHESIS_CLOSE
                                + Constants.STRING_BRACKETS_OPEN
                                + eventsData.getCalendarEventsCount(entry.getKey())
                                + Constants.STRING_BRACKETS_CLOSE
                );
                calSelected.add(preferences_calendars.contains(entry.getKey()));
                sel[ind] = calSelected.get(ind);
                ind++;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_CustomEvents_Calendars_title)
                    .setIcon(android.R.drawable.ic_menu_month)
                    .setMultiChoiceItems(calTitles.toArray(new CharSequence[0]), sel, (dialog, which, isChecked) -> calSelected.set(which, isChecked))
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                        Set<String> toStore = new HashSet<>();
                        for (int i = 0; i < calSelected.size(); i++) {
                            if (calSelected.get(i)) toStore.add(calIDs.get(i));
                        }
                        eventsData.setPreferences_Calendars(eventType, toStore);
                        eventsData.savePreferences();

                        dialog.cancel();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();

            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectRingtone() {

        try {

            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

            String existingValue = eventsData.preferences_notifications_ringtone;
            if (existingValue.length() == 0) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
            }
            try {
                startActivityForResult(intent, Constants.RESULT_PICK_RINGTONE);
            } catch (android.content.ActivityNotFoundException e) { /**/ }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void editRules() {

        try {

            int themeEditText;
            if (ContactsEvents.getInstance().preferences_theme.themeEditText != 0) {
                themeEditText = ContactsEvents.getInstance().preferences_theme.themeEditText;
            } else {
                themeEditText = ContactsEvents.themeEditText_default;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, themeEditText));
            builder.setTitle(R.string.pref_CustomEvents_Birthday_Calendars_Rules_title);
            builder.setIcon(android.R.drawable.ic_menu_edit);
            builder.setMessage(R.string.pref_CustomEvents_Birthday_Calendars_Rules_hint);

            //https://stackoverflow.com/questions/10903754/input-text-dialog-android
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(eventsData.preferences_birthday_calendars_rules);
            //input.setHint(R.string.msg_hint_search);
            input.setSingleLine(false);
            input.setHintTextColor(ta.getColor(R.styleable.Theme_dialogHintColor, 0));
            input.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));

            builder.setView(input);

            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {/**/});
            builder.setNegativeButton(R.string.button_cancel, (dialog1, which) -> dialog1.cancel());

            AlertDialog  alertToShow = builder.create();

            //https://stackoverflow.com/questions/27965662/how-can-i-change-default-dialog-button-text-color-in-android-5
            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                //alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            //https://stackoverflow.com/questions/4054662/displaying-soft-keyboard-whenever-alertdialog-builder-object-is-opened/6123935#6123935
            //https://stackoverflow.com/questions/5593053/open-soft-keyboard-programmatically
            input.requestFocus();
            if (alertToShow.getWindow() != null) alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alertToShow.show();
            alertToShow.getWindow().setBackgroundDrawable(new ColorDrawable(ta.getColor(R.styleable.Theme_editTextBackgroundCustom, 0)));

            //https://stackoverflow.com/questions/15362122/change-font-size-for-an-alertdialog-message
            TextView textView = alertToShow.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setTextSize(12);
                textView.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                textView.setHintTextColor(ta.getColor(R.styleable.Theme_dialogHintColor, 0));
            }

            //https://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
            alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                String rules = input.getText().toString().trim();

                if (!rules.isEmpty()) {
                    final int rStartIndex = rules.indexOf(Constants.RULE_TAG_NAME);
                    if (rStartIndex == -1) { //todo: && !rules.toLowerCase().contains(Constants.RULE_TAG_ALIAS)) {
                        Toast.makeText(this, getText(R.string.pref_CustomEvents_Birthday_Calendars_Rules_msg_no_tags), Toast.LENGTH_LONG).show();
                        return;
                    } else if (rules.indexOf(Constants.RULE_TAG_NAME, rStartIndex + 1) > -1 && rules.indexOf(Constants.STRING_BAR, rStartIndex) == -1) {
                        Toast.makeText(this, getText(R.string.pref_CustomEvents_Birthday_Calendars_Rules_msg_tags_error), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                eventsData.preferences_birthday_calendars_rules = rules;
                eventsData.savePreferences();
                alertToShow.dismiss();

            });

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    //https://habr.com/ru/post/203884/
    private void selectFiles(@NonNull String eventType) {

        try {

            ArrayList<String> filesPaths = new ArrayList<>(); //Только видимая часть
            ArrayList<String> filesFullData = new ArrayList<>(); //Вся информация о файле
            ArrayList<Boolean> filesSelected = new ArrayList<>();
            boolean[] sel = new boolean[0];

            if (filesList != null) {
                sel = new boolean[filesList.size()];
                int ind = 0;
                for (String file : filesList) {
                    filesPaths.add(
                            file.split(Constants.STRING_PIPE)[0]
                                + Constants.STRING_BRACKETS_OPEN
                                + eventsData.getFileEventsCount(file, eventType.equals(Constants.Type_MultiEvent))
                                + Constants.STRING_BRACKETS_CLOSE
                    );
                    filesFullData.add(file);
                    filesSelected.add(true);
                    sel[ind] = filesSelected.get(ind);
                    ind++;
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_CustomEvents_LocalFiles_title)
                    .setIcon(android.R.drawable.ic_menu_save)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                        Set<String> toStore = new HashSet<>();
                        String file;
                        Uri uri = null;
                        for (int i = 0; i < filesSelected.size(); i++) {
                            file = filesFullData.get(i);
                            if (filesSelected.get(i)) {
                                toStore.add(file);
                            } else {
                                String[] fileDetails = file.split(Constants.STRING_PIPE);
                                try {
                                    uri = Uri.parse(fileDetails[1]);
                                } catch (NullPointerException e) { /**/ }
                                if (uri != null) {
                                    try {
                                        getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    } catch (RuntimeException e) { /**/ }
                                }
                            }

                        }
                        eventsData.setPreferences_Files(eventType, toStore);
                        eventsData.savePreferences();

                        dialog.cancel();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setNeutralButton(R.string.button_choose, (dialog, which) -> {

                        filesList = new HashSet<>();
                        String file;
                        Uri uri = null;
                        for (int i = 0; i < filesSelected.size(); i++) {
                            file = filesFullData.get(i);
                            if (filesSelected.get(i)) {
                                filesList.add(file);
                            } else {
                                String[] fileDetails = file.split(Constants.STRING_PIPE);
                                try {
                                    uri = Uri.parse(fileDetails[1]);
                                } catch (NullPointerException e) { /**/ }
                                if (uri != null) {
                                    try {
                                        getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    } catch (RuntimeException e) { /**/ }
                                }
                            }
                        }

                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                        try {
                            startActivityForResult(intent, Constants.RESULT_PICK_FILE);
                        } catch (android.content.ActivityNotFoundException e) { /**/ }
                    })
                    .setCancelable(true);

            if (filesPaths.isEmpty()) {
                builder.setMessage(R.string.msg_no_files_selected);
            } else {
                builder.setMultiChoiceItems(filesPaths.toArray(new CharSequence[0]), sel, (dialog, which, isChecked) -> filesSelected.set(which, isChecked));
            }

            AlertDialog alertToShow = builder.create();

            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void selectAlarmTime() {

        try {

            final TimePicker timePicker = new TimePicker(this);
            timePicker.setIs24HourView(DateFormat.is24HourFormat(this));
            timePicker.setCurrentHour(eventsData.preferences_notifications_alarm_hour);
            timePicker.setCurrentMinute(eventsData.preferences_notifications_alarm_minute);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.pref_Notifications_AlarmHour_title)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                        int hour = Build.VERSION.SDK_INT >= 23 ? timePicker.getHour() : timePicker.getCurrentHour();
                        int minute = Build.VERSION.SDK_INT >= 23 ? timePicker.getMinute() : timePicker.getCurrentMinute();
                        eventsData.setPreferences_AlarmTime(hour, minute);
                        eventsData.savePreferences();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.dismiss())
                    .setView(timePicker)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectFontMagnify() {

        try {

            int dimen_details = (int) (eventsData.dimen_List_details / eventsData.DisplayMetrics_density);
            int dimen_name = (int) (eventsData.dimen_List_name / eventsData.DisplayMetrics_density);
            int dimen_date = (int) (eventsData.dimen_list_date / eventsData.DisplayMetrics_density);

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setPositiveButton(R.string.button_ok, null)
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setNeutralButton(R.string.button_reset, null);

            AlertDialog dialog = builder.create();
            View view = View.inflate(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog), R.layout.dialog_fontmagnify, null);
            dialog.setCustomTitle(view);

            ImageView icon = view.findViewById(R.id.icon);
            if (icon != null) icon.setImageBitmap(ContactsEvents.getBitmap(this, R.drawable.ic_menu_find));
            TextView title = view.findViewById(R.id.title);
            if (title != null) title.setText(R.string.pref_List_FontMagnify_title);

            //Данные события
            ImageView iconEvent = view.findViewById(R.id.entryEventIcon);
            if (iconEvent != null) iconEvent.setImageBitmap(ContactsEvents.getBitmap(this, R.drawable.ic_event_birthday));

            ImageView photoEvent = view.findViewById(R.id.entryPhotoImageView);
            if (photoEvent != null) {
                final Integer idPhoto = eventsData.preferences_IconPackImages_M.get(0);
                if (idPhoto != null) {
                    photoEvent.setImageResource(idPhoto);
                } else {
                    photoEvent.setImageResource(R.drawable.ic_pack00_m1);
                }
            }

            //Размер: Срок до события
            TextView seek1_label = view.findViewById(R.id.seek1_label);
            seek1_label.setText(getString(R.string.pref_List_FontMagnify_seek_distance));

            SeekBar seek1 = view.findViewById(R.id.seek1);
            seek1.setProgress(eventsData.preferences_list_magnify_distance + 5);

            TextView seek1_progress = view.findViewById(R.id.seek1_progress);
            seek1_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek1.getProgress() - 5) * 10)));

            TextView event_distance = view.findViewById(R.id.entryDayDistanceTextView);
            event_distance.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + (seek1.getProgress() - 5) * 0.1)));

            seek1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    seek1_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek1.getProgress() - 5) * 10)));
                    event_distance.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + (seek1.getProgress() - 5) * 0.1)));

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            //Размер: ФИО
            TextView seek2_label = view.findViewById(R.id.seek2_label);
            seek2_label.setText(getString(R.string.pref_List_FontMagnify_seek_name));

            SeekBar seek2 = view.findViewById(R.id.seek2);
            seek2.setProgress(eventsData.preferences_list_magnify_name + 5);

            TextView seek2_progress = view.findViewById(R.id.seek2_progress);
            seek2_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek2.getProgress() - 5) * 10)));

            TextView event_title = view.findViewById(R.id.entryNameTextView);
            event_title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + (seek2.getProgress() - 5) * 0.1)));

            seek2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    seek2_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek2.getProgress() - 5) * 10)));
                    event_title.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + (seek2.getProgress() - 5) * 0.1)));

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            //Размер: Детали
            TextView seek3_label = view.findViewById(R.id.seek3_label);
            seek3_label.setText(getString(R.string.pref_List_FontMagnify_seek_details));

            SeekBar seek3 = view.findViewById(R.id.seek3);
            seek3.setProgress(eventsData.preferences_list_magnify_details + 5);

            TextView seek3_progress = view.findViewById(R.id.seek3_progress);
            seek3_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek3.getProgress() - 5) * 10)));

            TextView event_details = view.findViewById(R.id.entryEventDetailsTextView);
            event_details.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + (seek3.getProgress() - 5) * 0.1)));

            seek3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    seek3_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek3.getProgress() - 5) * 10)));
                    event_details.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + (seek3.getProgress() - 5) * 0.1)));

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });


            //Размер: Дата события
            TextView seek4_label = view.findViewById(R.id.seek4_label);
            seek4_label.setText(getString(R.string.pref_List_FontMagnify_seek_date));

            SeekBar seek4 = view.findViewById(R.id.seek4);
            seek4.setProgress(eventsData.preferences_list_magnify_date + 5);

            TextView seek4_progress = view.findViewById(R.id.seek4_progress);
            seek4_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek4.getProgress() - 5) * 10)));

            TextView event_date = view.findViewById(R.id.entryDateTextView);
            event_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_date * (1 + (seek4.getProgress() - 5) * 0.1)));

            seek4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    seek4_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek4.getProgress() - 5) * 10)));
                    event_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_date * (1 + (seek4.getProgress() - 5) * 0.1)));

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            //Размер: Возраст
            TextView seek5_label = view.findViewById(R.id.seek5_label);
            seek5_label.setText(getString(R.string.pref_List_FontMagnify_seek_age));

            SeekBar seek5 = view.findViewById(R.id.seek5);
            seek5.setProgress(eventsData.preferences_list_magnify_age + 5);

            TextView seek5_progress = view.findViewById(R.id.seek5_progress);
            seek5_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek5.getProgress() - 5) * 10)));

            TextView event_age = view.findViewById(R.id.entryDetailsCounter);
            event_age.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + (seek5.getProgress() - 5) * 0.1)));

            seek5.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    seek5_progress.setText(getString(R.string.pref_List_FontMagnify_progress, String.valueOf(100 + (seek5.getProgress() - 5) * 10)));
                    event_age.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + (seek5.getProgress() - 5) * 0.1)));

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            dialog.setOnShowListener(arg0 -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    eventsData.setPreferences_List_FontMagnify(
                            seek1.getProgress() - 5,
                            seek2.getProgress() - 5,
                            seek3.getProgress() - 5,
                            seek4.getProgress() - 5,
                            seek5.getProgress() - 5
                    );
                    eventsData.savePreferences();
                    dialog.dismiss();
                });
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    seek1.setProgress(5);
                    seek2.setProgress(5);
                    seek3.setProgress(5);
                    seek4.setProgress(5);
                    seek5.setProgress(5);
                });

            });

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        try {

            if (requestCode == Constants.RESULT_PICK_FILE && resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    Uri uri = resultData.getData();
                    if (uri != null) {
                        if (eventsData.readFileToString(uri, null).length() > 0) {
                            String filename = eventsData.getPath(this, uri);
                            if (!filename.isEmpty()) {
                                this.grantUriPermission(this.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                this.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                filesList.add(filename.concat(Constants.STRING_BAR).concat(uri.toString()));
                                selectFiles(this.eventTypeForSelect);
                            }
                        } else {
                            Toast.makeText(this, getText(R.string.msg_file_open_error) + uri.getPath(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else if (requestCode == Constants.RESULT_PICK_RINGTONE && resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    Uri ringtone = resultData.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (ringtone != null) {
                        eventsData.preferences_notifications_ringtone = ringtone.toString();
                    } else {
                        eventsData.preferences_notifications_ringtone = ""; //Беззвучный
                    }
                    eventsData.savePreferences();
                }
            } else {

                super.onActivityResult(requestCode, resultCode, resultData);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    //https://stackoverflow.com/questions/10932832/multiple-choice-alertdialog-with-custom-adapter
    //https://stackoverflow.com/questions/8533394/icons-in-a-list-dialog
    //https://stackoverflow.com/questions/16932895/how-to-override-the-style-of-android-r-layout-simple-list-item-multiple-choice
    //https://stackoverflow.com/questions/7021578/resize-drawable-in-android/23570811
    //https://stackoverflow.com/questions/50077917/android-graphics-drawable-adaptiveicondrawable-cannot-be-cast-to-android-graphic
    private static class AccountsListAdapter extends ArrayAdapter<String> {

        private static final String TAG = "AccountsListAdapter";
        private final List<Integer> images;
        private final List<String> packages;
        private final TypedArray ta;
        private final PackageManager pm = getContext().getPackageManager();

        AccountsListAdapter(Context context, List<String> items, List<Integer> images, List<String> packages, TypedArray theme) {
            super(context, R.layout.settings_list_item_multiple_choice, items); //simple_list_item_multiple_choice
            this.images = images;
            this.packages = packages;
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

                //Context packageContext = this.context.createPackageContext(packages.get(position), 0);
                //Resources resources = packageContext.getResources();
                //Drawable icon = null; //androidx.core.content.res.ResourcesCompat.getDrawable(resources, images.get(position), null);
                Drawable icon = pm.getDrawable(packages.get(position), images.get(position), null);
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

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showText(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }

            return view;
        }

    }

    private static class ThemeListAdapter extends ArrayAdapter<String> {

        private static final String TAG = "ImageSelectAdapter";
        private final List<Integer> colors;
        private final TypedArray ta;

        ThemeListAdapter(Context context, List<String> items, List<Integer> colors, TypedArray theme) {
            super(context, R.layout.settings_list_item_single_choice, items);
            this.colors = colors;
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

                int color = colors.get(position);
                int darkenedColor = Color.rgb(
                        Color.red(color) * 192 / 256,
                        Color.green(color) * 192 / 256,
                        Color.blue(color) * 192 / 256);

                GradientDrawable oval = new GradientDrawable();
                oval.setShape(GradientDrawable.OVAL);
                oval.setSize(80, 80);
                oval.setColor(color);
                oval.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics), darkenedColor);
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(oval, null, null, null);

                textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, displayMetrics));

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showText(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }

            return view;
        }

    }

    private static class ImageSelectAdapter extends ArrayAdapter<String> {

        private static final String TAG = "ImageSelectAdapter";
        private final List<Integer> images;
        private final TypedArray ta;
        private final boolean makeSquared;

        ImageSelectAdapter(Context context, List<String> items, List<Integer> images, boolean makeSquared, TypedArray theme) {
            super(context, R.layout.settings_list_item_single_choice, items);
            this.images = images;
            this.ta = theme;
            this.makeSquared = makeSquared;
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

                //Context packageContext = this.context.createPackageContext(packages.get(position), 0);
                //Resources resources = packageContext.getResources();
                //Drawable icon = null; //androidx.core.content.res.ResourcesCompat.getDrawable(resources, images.get(position), null);
                //Drawable icon = pm.getDrawable(packages.get(position), images.get(position), null);
                Bitmap bmp = ContactsEvents.getBitmap(getContext(), images.size() <= position ? 0 : images.get(position));
                if (bmp != null) {
                    //Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    /*Canvas canvas = new Canvas(bmp);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.draw(canvas);*/
                    //Bitmap bitmapResized = Bitmap.createScaledBitmap(bmp, 100, 150, false);
                    //Bitmap bitmapScaled = Bitmap.createBitmap(bmp, bmp.getWidth() / 3, 0, bmp.getWidth() / 3, bmp.getHeight());
                    Bitmap bitmapResized;
                    if (makeSquared) {
                        final int bmWidth = bmp.getWidth();
                        final int bmHeight = bmp.getHeight();
                        //final int bmPadding = 20;
                        Bitmap bitmapSquared;
                        if (bmHeight > bmWidth) {
                            //noinspection SuspiciousNameCombination
                            bitmapSquared = Bitmap.createBitmap(bmp, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
                        } else {
                            //noinspection SuspiciousNameCombination
                            bitmapSquared = Bitmap.createBitmap(bmp, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
                        }
                        //bitmapResized = Bitmap.createBitmap(bitmapSquared.getWidth(), bitmapSquared.getHeight(), Bitmap.Config.ARGB_8888);
                        //Canvas can = new Canvas(bitmapResized);
                        //can.drawBitmap(bitmapSquared, 0, 0, null);
                        bitmapResized = Bitmap.createScaledBitmap(bitmapSquared, 130, 130, true);
                        bitmapSquared.recycle();
                    } else {
                        bitmapResized = Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, bmp.getWidth() / 3, 0, bmp.getWidth() / 3, bmp.getHeight()), 90, 130, true);
                    }
                    //bitmapScaled.recycle();
                    bmp.recycle();
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(new BitmapDrawable(getContext().getResources(), bitmapResized), null, null, null);
                }
                textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, displayMetrics));

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showText(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }

            return view;
        }

    }

    private synchronized static void setDisplayMetrics(DisplayMetrics ds) {displayMetrics = ds;}

}