/*
 * *
 *  * Created by Vladimir Belov on 22.03.20 23:03
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 22.03.20 22:12
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
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Random;
import java.util.Set;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //https://stackoverflow.com/questions/26564400/creating-a-preference-screen-with-support-v21-toolbar

    private String testChannelId = Constants.STRING_EMPTY;
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
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
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

            } else if (getString(R.string.pref_AboutActivity_key).equals(key)) { //О приложении

                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);

            } else if (getString(R.string.pref_Accounts_key).equals(key)) { //Аккаунты

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, Constants.MY_PERMISSIONS_REQUEST__GET_ACCOUNTS);
                } else {
                    selectAccounts();
                }

            } else if (getString(R.string.pref_CustomEvents_Anniversary_List_key).equals(key)) { //Список всех годовщин свадеб

                eventsData.showAnniversaryList(this);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !testChannelId.equals(Constants.STRING_EMPTY)) {
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
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
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

        //Toast.makeText(this, key, Toast.LENGTH_LONG).show();
        if (getString(R.string.pref_Language_key).equals(key)) {

            eventsData.getPreferences();
            this.recreate();

        } else if (getString(R.string.pref_Theme_key).equals(key)) {

            eventsData.getPreferences();
            this.setTheme(eventsData.preferences_theme.themeMain);
            this.recreate();

        }
        /* bug. так не работает https://stackoverflow.com/questions/6725105/ringtonepreference-not-firing-onsharedpreferencechanged
        else if (getString(R.string.pref_Notifications_Ringtone_key).equals(key)) {
        }*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.MY_PERMISSIONS_REQUEST__GET_ACCOUNTS || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            //todo: наверное, убрать - не работает тут: https://stackoverflow.com/questions/46003114/how-should-one-request-permissions-from-a-custom-preference
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectAccounts();
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
            //TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

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
            Toast.makeText(this, getString(R.string.msg_notifications_disabled), Toast.LENGTH_LONG).show();
        }
    }

    private void selectAccounts() {

        try {

            List<String> accountNames = new ArrayList<>();
            List<Integer> accountIcons = new ArrayList<>();
            List<String> accountPackages = new ArrayList<>();
            ContactsEvents eventsData = ContactsEvents.getInstance();

            //https://stackoverflow.com/questions/10657096/how-to-get-an-icon-associated-with-specific-account-from-accountmanager-getaccou
            Account[] accounts = AccountManager.get(this).getAccounts();
            AuthenticatorDescription[] descriptions =  AccountManager.get(this).getAuthenticatorTypes();
            for (Account account : accounts) {
                accountNames.add(account.name + Constants.STRING_PARENTHESIS_OPEN + account.type + Constants.STRING_PARENTHESIS_CLOSE);
                for (AuthenticatorDescription desc : descriptions) {
                    if (account.type.equals(desc.type)) {
                        accountIcons.add(desc.iconId);
                        accountPackages.add(desc.packageName);
                        break;
                    }
                }
                if (accountNames.size() != accountIcons.size()) { //Не нашли иконку, что ОЧЕНЬ странно
                    accountIcons.add(0);
                    accountPackages.add(Constants.STRING_EMPTY);
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

                    for (int i = 0; i < accountNames.size(); i++) {
                        final Set<String> preferences_accounts = eventsData.getPreferences_Accounts();
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


}