/*
 * *
 *  * Created by Vladimir Belov on 17.12.20 22:05
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 13.12.20 21:51
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.vovka.birthdaycountdown.Constants.STRING_2HASH;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_CLOSE;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_OPEN;
import static org.vovka.birthdaycountdown.Constants.Type_BirthDay;
import static org.vovka.birthdaycountdown.Constants.Type_Other;


@SuppressWarnings("deprecation")
public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //https://stackoverflow.com/questions/26564400/creating-a-preference-screen-with-support-v21-toolbar

    private String testChannelId = STRING_EMPTY;
    private TypedArray ta;
    private ContactsEvents eventsData;

    @SuppressLint("PrivateResource")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = getApplicationContext();
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

            //Toast.makeText(this, "locale=" + locale.toString() + ", pref=" + eventsData.preferences_language, Toast.LENGTH_LONG).show();
            //Toast.makeText(this, getString(R.string.button_cancel), Toast.LENGTH_LONG).show();
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
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_material);
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
                        builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) ->
                                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + this.getPackageName()))));
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
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_UPDATE_TITLES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateVisibility() {
        try {

            PreferenceCategory prefCat;
            Preference pref;

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
                        pref = new Preference(eventsData.context);
                        pref.setTitle(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_title));
                        pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_description));
                        pref.setKey(getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key));
                        prefCat.addPreference(pref);
                    }

                    if (findPreference(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key)) == null) {
                        pref = new SwitchPreference(eventsData.context);
                        pref.setTitle(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_title));
                        pref.setSummary(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_description));
                        pref.setKey(getString(R.string.pref_CustomEvents_Birthday_Calendars_UseYear_key));
                        prefCat.addPreference(pref);
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_UPDATE_VISIBILITY_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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

            } else if (getString(R.string.pref_FAQActivity_key).equals(key)) { //FAQ

                Intent intent = new Intent(this, FAQActivity.class);
                startActivity(intent);

            } else if (getString(R.string.pref_AboutActivity_key).equals(key)) { //О приложении

                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);

            } else if (getString(R.string.pref_Accounts_key).equals(key)) { //Аккаунты

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, Constants.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    return true;
                }

                selectAccounts();

            } else if (getString(R.string.pref_CustomEvents_Anniversary_List_key).equals(key)) { //Список всех годовщин свадеб

                eventsData.showAnniversaryList(this);
                return true;

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_key).equals(key)) { //Календари (Дни рождения)

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                    return true;
                }

                selectCalendars(ContactsEvents.eventTypesIDs.get(Type_BirthDay));

            } else if (getString(R.string.pref_CustomEvents_Other_Calendars_key).equals(key)) { //Календари (Другие события)

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR);
                    return true;
                }

                selectCalendars(ContactsEvents.eventTypesIDs.get(Type_Other));

            } else if (getString(R.string.pref_CustomEvents_Birthday_Calendars_Rules_key).equals(key)) {

                editRules();
                return true;

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_PREFERENCE_TREE_CLICK_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_STOP_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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
        eventsData.getPreferences();

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
            if (eventsData.preferences_notifications_days.size() == 0) {
                updateVisibility();
            } else {
                this.recreate();
            }

        } else if (key.equals(getString(R.string.pref_CustomEvents_Birthday_Calendars_key))) {

            // if (eventsData.preferences_Calendars_BirthDay.size() == 0) {
            updateVisibility();
            //     } else {
            //       this.recreate();
            //       setUpNestedScreen((PreferenceScreen) findPreference(getString(R.string.pref_CustomEvents_Birthday_key)));
            //    }

        }
        /* bug. вот так с выбором рингтона не работает https://stackoverflow.com/questions/6725105/ringtonepreference-not-firing-onsharedpreferencechanged
        else if (getString(R.string.pref_Notifications_Ringtone_key).equals(key)) {
        }*/

    }

    //todo: наверное, убрать - не работает тут: https://stackoverflow.com/questions/46003114/how-should-one-request-permissions-from-a-custom-preference
/*    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_GET_ACCOUNTS || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
//
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                selectAccounts();
//            }
//        }

        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "TEST", Toast.LENGTH_LONG).show();
        }

    }*/

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
                marginParams.setMargins(0, (int) (42 * displayMetrics.density + 0.5f), 0, 0);
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
            list.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, ContextCompat.getColor(this, R.color.light_gray_trans))));
            list.setDividerHeight((int) (1 * displayMetrics.density));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SET_UP_NESTED_SCREEN_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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
                            accountIcons.add(desc.iconId);
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
                ListAdapter adapter = new GetAccountsListAdapter(this, accountNames, accountIcons, accountPackages, ta);

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
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_GET_ACCOUNTS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void selectCalendars(String eventType) {

        try {

            eventsData.map_calendars = eventsData.getCalendars();

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
                calTitles.add(entry.getValue().replace(STRING_2HASH, STRING_PARENTHESIS_OPEN).concat(STRING_PARENTHESIS_CLOSE));
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
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_GET_CALENDARS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void editRules() {

        //todo: https://github.com/VladimirBelov/BirthdayCountdown/blob/97d2f61dd1538384e5595fca46ae0902d99d5183/app/src/main/java/org/vovka/birthdaycountdown/MainActivity.java

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
                    if (!rules.toLowerCase().contains(Constants.RULE_TAG_NAME) && !rules.toLowerCase().contains(Constants.RULE_TAG_ALIAS)) {
                        Toast.makeText(this, getText(R.string.pref_CustomEvents_Birthday_Calendars_Rules_msg_no_tags), Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                eventsData.preferences_birthday_calendars_rules = rules;
                eventsData.setPreferences();
                alertToShow.dismiss();

            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_EDIT_RULES_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

}