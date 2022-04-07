/*
 * *
 *  * Created by Vladimir Belov on 07.03.2022, 22:54
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 06.03.2022, 16:12
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.RESULT_PICK_FILE;
import static org.vovka.birthdaycountdown.Constants.RESULT_PICK_RINGTONE;
import static org.vovka.birthdaycountdown.Constants.RULE_TAG_NAME;
import static org.vovka.birthdaycountdown.Constants.STRING_BAR;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_EOT;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_CLOSE;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_OPEN;
import static org.vovka.birthdaycountdown.Constants.STRING_PIPE;
import static org.vovka.birthdaycountdown.Constants.Type_BirthDay;
import static org.vovka.birthdaycountdown.Constants.Type_Other;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
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
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Objects;
import java.util.Random;
import java.util.Set;


@SuppressWarnings("deprecation")
public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    //https://stackoverflow.com/questions/26564400/creating-a-preference-screen-with-support-v21-toolbar

    private String testChannelId = STRING_EMPTY;
    private TypedArray ta;
    private ContactsEvents eventsData;
    private String eventTypeForSelect;
    private Set<String> filesList;
    //private String selectedColorPicker = "";

    @SuppressLint("PrivateResource")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();
            eventsData.setLocale(true);

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

            addPreferencesFromResource(R.xml.settings);

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            this.setTheme(eventsData.preferences_theme.themeMain);

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

            Preference notificationSoundPref = findPreference(getString(R.string.pref_Notifications_Ringtone_key));
            notificationSoundPref.setOnPreferenceChangeListener((preference, newValue) -> {
                eventsData.getPreferences();
                //Toast.makeText(this, eventsData.preferences_notifications_ringtone, Toast.LENGTH_LONG).show();
                if (eventsData.preferences_notifications_ringtone.contains("/media/external/") &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                        builder.setTitle(getString(R.string.msg_no_access_contacts));
                        builder.setIcon(android.R.drawable.ic_menu_info_details);
                        builder.setMessage(getString(R.string.msg_no_access_storage_hint));
                        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                        builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                            try {
                                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName())));
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

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_CREATE_ERROR + e, Toast.LENGTH_LONG).show();
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
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_UPDATE_TITLES_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    private void updateVisibility() {
        try {

            PreferenceCategory prefCat;
            Preference pref;

            this.setTheme(eventsData.preferences_theme.themeMain);

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_Notifications_key));
            if (prefCat != null && eventsData.preferences_notifications_days.size() == 0) {

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

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_CustomEvents_Birthday_key));
            if (prefCat != null) {

                if (eventsData.preferences_Calendars_BirthDay.size() == 0) {

                    pref = findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key));
                    if (pref != null) prefCat.removePreference(pref);
                    pref = findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key));
                    if (pref != null) prefCat.removePreference(pref);

                } else {

                    if (findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key)) == null) {
                        pref = new Preference(eventsData.getContext());
                        pref.setTitle(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_title));
                        pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_description));
                        pref.setKey(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key));
                        prefCat.addPreference(pref);
                    }

                    if (findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key)) == null) {
                        pref = new SwitchPreference(eventsData.getContext());
                        pref.setTitle(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_title));
                        pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_description));
                        pref.setKey(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key));
                        prefCat.addPreference(pref);
                    }

                }

                if (findPreference(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key)) == null) {
                    pref = new Preference(eventsData.getContext());
                    pref.setTitle(getString(R.string.pref_CustomEvents_LocalFiles_title));
                    pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_description));
                    pref.setKey(getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key));
                    prefCat.addPreference(pref);
                }

            }

            prefCat = (PreferenceCategory) findPreference(getString(R.string.pref_Help_key));
            if (prefCat != null) {
                if (eventsData.checkNoBatteryOptimization()) {
                    pref = findPreference(getString(R.string.pref_BatteryOptimization_key));
                    if (pref != null) prefCat.removePreference(pref);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_UPDATE_VISIBILITY_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getPreferenceScreen() != null)
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                } catch (android.content.ActivityNotFoundException e) { /**/ }

            } else if (getString(R.string.pref_AboutActivity_key).equals(key)) { //О приложении

                Intent intent = new Intent(this, AboutActivity.class);
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) { /**/ }

            } else if (getString(R.string.pref_Accounts_key).equals(key)) { //Аккаунты

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.GET_ACCOUNTS}, Constants.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    return true;
                }

                selectAccounts();

            } else if (getString(R.string.pref_IconPack_key).equals(key)) { //Силуэты

                selectIconPack();
                return true;

            } else if (getString(R.string.pref_CustomEvents_Anniversary_List_key).equals(key)) { //Список всех годовщин свадеб

                eventsData.showAnniversaryList(this);
                return true;

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_key).equals(key)) { //Календари (Дни рождения)

                this.eventTypeForSelect = ContactsEvents.eventTypesIDs.get(Type_BirthDay);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);

                } else {

                    selectCalendars(this.eventTypeForSelect);

                }
                return true;

            } else if (getString(R.string.pref_CustomEvents_Other_Calendars_key).equals(key)) { //Календари (Другие события)

                this.eventTypeForSelect = ContactsEvents.eventTypesIDs.get(Type_Other);

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                    return true;

                } else {

                    selectCalendars(this.eventTypeForSelect);
                }

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key).equals(key)) {

                editRules();
                return true;

            } else if (getString(R.string.pref_BatteryOptimization_key).equals(key)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) { /**/ }
                }

            } else if (getString(R.string.pref_CustomEvents_Other_LocalFiles_key).equals(key)) {

                filesList = new HashSet<>(Objects.requireNonNull(eventsData.preferences_otherevent_files));
                this.eventTypeForSelect = ContactsEvents.eventTypesIDs.get(Type_Other);
                selectFiles(this.eventTypeForSelect);
                return true;

            } else if (getString(R.string.pref_CustomEvents_Birthday_LocalFiles_key).equals(key)) {

                filesList = new HashSet<>(Objects.requireNonNull(eventsData.preferences_birthday_files));
                this.eventTypeForSelect = ContactsEvents.eventTypesIDs.get(Type_BirthDay);
                selectFiles(this.eventTypeForSelect);
                return true;

            } else if (getString(R.string.pref_Notifications_Ringtone_key).equals(key)) {

                selectRingtone();
                return true;

            } else if (getString(R.string.pref_Notifications_AlarmHour_key).equals(key)) {

                selectAlarmTime();
                return true;

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_PREFERENCE_TREE_CLICK_ERROR + e, Toast.LENGTH_LONG).show();
        }

        return false;
    }

    @Override
    public void onStop() {

        try {
            //удаляем временный канал оповещений
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !testChannelId.equals(STRING_EMPTY)) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null && notificationManager.getNotificationChannel(testChannelId) != null) {
                    notificationManager.deleteNotificationChannel(testChannelId);
                }
            }

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_STOP_ERROR + e, Toast.LENGTH_LONG).show();
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

        ContactsEvents eventsData = ContactsEvents.getInstance();
        eventsData.statLastPausedForOtherActivity = 0;
        eventsData.getPreferences();
        updateVisibility();

        if (key.equals(getString(R.string.pref_Language_key))) {

            this.recreate();

        } else if (key.equals(getString(R.string.pref_Theme_key))) {

            this.setTheme(eventsData.preferences_theme.themeMain);
            this.recreate();
            //todo: созданные программно настройки не подхватывают стиль

        } else if (key.equals(getString(R.string.pref_CustomEvents_Custom1_Caption_key)) ||
                key.equals(getString(R.string.pref_CustomEvents_Custom2_Caption_key)) ||
                key.equals(getString(R.string.pref_CustomEvents_Custom3_Caption_key)) ||
                key.equals(getString(R.string.pref_CustomEvents_Custom4_Caption_key)) ||
                key.equals(getString(R.string.pref_CustomEvents_Custom5_Caption_key))) {

            updateTitles();

        } else if (key.equals(getString(R.string.pref_Notifications_Days_key))) {

            eventsData.preferences_notifications_days.removeAll(new HashSet<String>() {{add("");}});
            if (eventsData.preferences_notifications_days.size() != 0) {
                this.recreate();
            //} else {
            //    updateVisibility();
            }

        //} else if (key.equals(getString(R.string.pref_CustomEvents_Birthday_Calendars_key))) {

            // if (eventsData.preferences_Calendars_BirthDay.size() == 0) {

            //     } else {
            //       this.recreate();
            //       setUpNestedScreen((PreferenceScreen) findPreference(getString(R.string.pref_CustomEvents_Birthday_key)));
            //    }

        }
        /* bug. вот так с выбором рингтона не работает https://stackoverflow.com/questions/6725105/ringtonepreference-not-firing-onsharedpreferencechanged
        else if (getString(R.string.pref_Notifications_Ringtone_key).equals(key)) {
        }*/

    }

    //https://stackoverflow.com/questions/46003114/how-should-one-request-permissions-from-a-custom-preference
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectAccounts();
            }

        } else if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (this.eventTypeForSelect != null && !this.eventTypeForSelect.isEmpty()) selectCalendars(this.eventTypeForSelect);
            }

        }

    }

    private void setUpNestedScreen(@NonNull PreferenceScreen preferenceScreen) {

        try {
            //Добавляем тулбар
            //https://code.i-harness.com/en/q/1cfc0dc

            Dialog dialog = preferenceScreen.getDialog();
            ListView list = dialog.findViewById(android.R.id.list);
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            //this.setTheme(eventsData.preferences_theme.themeMain);
            //ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) { //Для Android > 6

                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) list.getLayoutParams();
                marginParams.setMargins(0, (int) (48 * displayMetrics.density + 0.5f), 0, 0);
                list.setPadding(0, (int) (10 * displayMetrics.density + 0.5f), 0, 0);
                ViewGroup root = (ViewGroup) list.getParent();
                Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(bar, 0); // insert at top
                bar.setTitle(preferenceScreen.getTitle());
                bar.setNavigationOnClickListener(v -> dialog.dismiss());
                root.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.white)));

            } else { //Для Android <= 6

                list.setPadding(0, (int) (10 * displayMetrics.density + 0.5f), 0, 0);
                LinearLayout root = (LinearLayout) list.getParent();
                Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(bar, 0); // insert at top
                bar.setTitle(preferenceScreen.getTitle());
                bar.setNavigationOnClickListener(v -> dialog.dismiss());
                root.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.white)));

            }
            list.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, ContextCompat.getColor(this, R.color.light_gray_transp))));
            list.setDividerHeight((int) (1 * displayMetrics.density));

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SET_UP_NESTED_SCREEN_ERROR + e, Toast.LENGTH_LONG).show();
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
                    if (!testChannelId.equals(STRING_EMPTY) && notificationManager.getNotificationChannel(testChannelId) != null) {
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
            Toast.makeText(this, getString(R.string.msg_notifications_disabled), Toast.LENGTH_LONG).show();
        }
    }

    private void selectAccounts() {

        try {

            List<String> accountNames = new ArrayList<>();
            List<Integer> accountIcons = new ArrayList<>();
            List<String> accountPackages = new ArrayList<>();
            ContactsEvents eventsData = ContactsEvents.getInstance();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
                //https://stackoverflow.com/questions/10657096/how-to-get-an-icon-associated-with-specific-account-from-accountmanager-getaccou
                Account[] accounts = AccountManager.get(this).getAccounts();
                AuthenticatorDescription[] descriptions = AccountManager.get(this).getAuthenticatorTypes();
                for (Account account : accounts) {
                    accountNames.add(account.name + STRING_PARENTHESIS_OPEN + account.type + STRING_PARENTHESIS_CLOSE);
                    for (AuthenticatorDescription desc : descriptions) {
                        if (account.type.equals(desc.type)) {
                            accountIcons.add(desc.iconId > 0 ? desc.iconId : desc.smallIconId);
                            accountPackages.add(desc.packageName);
                            break;
                        }
                    }
                    if (accountNames.size() != accountIcons.size()) { //Не нашли иконку, что ОЧЕНЬ странно
                        accountIcons.add(0);
                        accountPackages.add(STRING_EMPTY);
                    }
                }
            }

            if (accountNames.size() > 0) {
                ListAdapter adapter = new AccountsListAdapter(this, accountNames, accountIcons, accountPackages, ta);

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.pref_Accounts_title)
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
                            eventsData.setPreferences();

                        })
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                        .setNeutralButton(R.string.button_all, (dialog, which) -> {
                            eventsData.setPreferences_Accounts(new HashSet<>());
                            eventsData.setPreferences();
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

                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                    //https://stackoverflow.com/questions/33074313/getting-default-padding-for-alertdialog
                    /*float dpi = getResources().getDisplayMetrics().density;
                    ListView listView = alertToShow.getListView();
                    listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)));
                    listView.setDividerHeight(2);
                    listView.setPadding((int)(30*dpi), 0, (int)(20*dpi), 0);*/
                });

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SELECT_ACCOUNTS_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    private void selectIconPack() {

        try {

            List<Integer> packIcons = new ArrayList<>();
            List<String> packNames = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_IconPack_entries)));

            packIcons.add(R.drawable.ic_pack00_f1);
            packIcons.add(R.drawable.ic_pack01_f2);
            packIcons.add(R.drawable.ic_pack02_f2);
            packIcons.add(R.drawable.ic_pack03_f3);

            ListAdapter adapter = new IconPackListAdapter(this, packNames, packIcons, ta);

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
                eventsData.setPreferences();
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
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SELECT_ICONPACK_ERROR + e, Toast.LENGTH_LONG).show();
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
                calTitles.add(entry.getValue().replace(STRING_EOT, STRING_PARENTHESIS_OPEN).concat(STRING_PARENTHESIS_CLOSE));
                calSelected.add(preferences_calendars.contains(entry.getKey()));
                sel[ind] = calSelected.get(ind++);
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
                        eventsData.setPreferences();

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
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SELECT_CALENDARS_ERROR + e, Toast.LENGTH_LONG).show();
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
                startActivityForResult(intent, RESULT_PICK_RINGTONE);
            } catch (android.content.ActivityNotFoundException e) { /**/ }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SELECT_RINGTONE_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    private void editRules() {

        try {

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
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

            //https://stackoverflow.com/questions/15362122/change-font-size-for-an-alertdialog-message
            TextView textView = alertToShow.findViewById(android.R.id.message);
            if (textView != null) textView.setTextSize(12);

            //https://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
            alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                String rules = input.getText().toString().trim();

                if (!rules.isEmpty()) {
                    final int rStartIndex = rules.indexOf(RULE_TAG_NAME);
                    if (rStartIndex == -1) { //todo: && !rules.toLowerCase().contains(Constants.RULE_TAG_ALIAS)) {
                        Toast.makeText(this, getText(R.string.pref_CustomEvents_Birthday_Calendars_Rules_msg_no_tags), Toast.LENGTH_LONG).show();
                        return;
                    } else if (rules.indexOf(RULE_TAG_NAME, rStartIndex + 1) > -1 && rules.indexOf(STRING_BAR, rStartIndex) == -1) {
                        Toast.makeText(this, getText(R.string.pref_CustomEvents_Birthday_Calendars_Rules_msg_tags_error), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                eventsData.preferences_birthday_calendars_rules = rules;
                eventsData.setPreferences();
                alertToShow.dismiss();

            });

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_EDIT_RULES_ERROR + e, Toast.LENGTH_LONG).show();
        }

    }

    //https://habr.com/ru/post/203884/
    private void selectFiles(String eventType) {

        try {

            ArrayList<String> filesPaths = new ArrayList<>(); //Только видимая часть
            ArrayList<String> filesFullData = new ArrayList<>(); //Вся информация о файле
            ArrayList<Boolean> filesSelected = new ArrayList<>();

            boolean[] sel = new boolean[filesList.size()];
            int ind = 0;
            for (String file: filesList) {
                filesPaths.add(file.split(STRING_PIPE)[0]);
                filesFullData.add(file);
                filesSelected.add(true);
                sel[ind] = filesSelected.get(ind++);
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
                                String[] fileDetails = file.split(STRING_PIPE);
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
                        eventsData.setPreferences();

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
                                String[] fileDetails = file.split(STRING_PIPE);
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
                        //intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        try {
                            startActivityForResult(intent, RESULT_PICK_FILE);
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
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SELECT_FILES_ERROR + e, Toast.LENGTH_LONG).show();
        }

    }

    private  void selectAlarmTime() {

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
                        eventsData.setPreferences();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.dismiss())
                    .setView(timePicker)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SELECT_ALARMTIME_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        try {

            if (requestCode == RESULT_PICK_FILE && resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    Uri uri = resultData.getData();
                    if (uri != null) {
                        if (eventsData.readFileToString(uri, null).length() > 0) {
                            String filename = eventsData.getPath(this, uri);
                            if (!filename.isEmpty()) {
                                this.grantUriPermission(this.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                this.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                filesList.add(filename.concat(STRING_BAR).concat(uri.toString()));
                                selectFiles(this.eventTypeForSelect);
                            }
                        } else {
                            Toast.makeText(this, getText(R.string.msg_file_open_error) + uri.getPath(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            } else if (requestCode == RESULT_PICK_RINGTONE && resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    Uri ringtone = resultData.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (ringtone != null) {
                        eventsData.preferences_notifications_ringtone = ringtone.toString();
                    } else {
                        eventsData.preferences_notifications_ringtone = ""; //Беззвучный
                    }
                    eventsData.setPreferences();
                }
            } else {

                super.onActivityResult(requestCode, resultCode, resultData);

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_ACTIVITY_RESULT_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    //https://stackoverflow.com/questions/10932832/multiple-choice-alertdialog-with-custom-adapter
    //https://stackoverflow.com/questions/8533394/icons-in-a-list-dialog
    //https://stackoverflow.com/questions/16932895/how-to-override-the-style-of-android-r-layout-simple-list-item-multiple-choice
    //https://stackoverflow.com/questions/7021578/resize-drawable-in-android/23570811
    //https://stackoverflow.com/questions/50077917/android-graphics-drawable-adaptiveicondrawable-cannot-be-cast-to-android-graphic
    private static class AccountsListAdapter extends ArrayAdapter<String> {

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
                textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), Constants.GET_ACCOUNTS_LIST_ADAPTER_GET_VIEW_ERROR + e, Toast.LENGTH_LONG).show();
            }

            return view;
        }

    }

    private static class IconPackListAdapter extends ArrayAdapter<String> {

        private final List<Integer> images;
        private final TypedArray ta;

        IconPackListAdapter(Context context, List<String> items, List<Integer> images, TypedArray theme) {
            super(context, R.layout.settings_list_item_single_choice, items);
            this.images = images;
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
                //Drawable icon = pm.getDrawable(packages.get(position), images.get(position), null);
                Bitmap bmp = ContactsEvents.getBitmap(getContext(), images.get(position));
                if (bmp != null) {
                    //Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    /*Canvas canvas = new Canvas(bmp);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.draw(canvas);*/
                    //Bitmap bitmapResized = Bitmap.createScaledBitmap(bmp, 100, 150, false);
                    Bitmap bitmapResized = Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, bmp.getWidth() / 3, 0, bmp.getWidth() / 3, bmp.getHeight()), 90, 130, false);
                    bmp.recycle();
                    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(new BitmapDrawable(getContext().getResources(), bitmapResized), null, null, null);
                }
                textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), Constants.GET_ICONPACK_LIST_ADAPTER_GET_VIEW_ERROR + e, Toast.LENGTH_LONG).show();
            }

            return view;
        }

    }

}