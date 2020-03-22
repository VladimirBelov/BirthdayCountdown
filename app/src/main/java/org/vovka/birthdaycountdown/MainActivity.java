/*
 * *
 *  * Created by Vladimir Belov on 22.03.20 23:03
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 22.03.20 22:10
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.vovka.birthdaycountdown.Constants.HTML_BR;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_BROWN;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_RED;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_YELLOW;
import static org.vovka.birthdaycountdown.Constants.REGEX_COMMAS;
import static org.vovka.birthdaycountdown.Constants.STRING_0;
import static org.vovka.birthdaycountdown.Constants.STRING_1;
import static org.vovka.birthdaycountdown.Constants.STRING_2;
import static org.vovka.birthdaycountdown.Constants.STRING_3;
import static org.vovka.birthdaycountdown.Constants.STRING_4;
import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_EOF;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_CLOSE;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_START;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_contact_id;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_fio;

//todo: сделать вывод ошибок в стандартный лог

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    //UI объекты
    private Resources resources;
    private SwipeRefreshLayout swipeRefresh;
    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener;
    private Menu menu;

    //Переменные
    private String filterNames = STRING_EMPTY;
    private ContactsEvents eventsData;
    private String selectedEvent_str;
    private String[] selectedEvent;

    private int statsAllEvents = 0;
    private int statsHiddenEvents = 0;
    private boolean triggeredMsgNoEvents = false;

    private TypedArray ta = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            //Оформление стиля окна приложения
            //https://stackoverflow.com/questions/22192291/how-to-change-the-status-bar-color-in-android
            //https://stackoverflow.com/questions/29069070/completely-transparent-status-bar-and-navigation-bar-on-lollipop
            //Window w = getWindow();
            //w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            //w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            eventsData = ContactsEvents.getInstance();
            eventsData.context = this; //getApplicationContext();
            eventsData.getPreferences();
            filterNames = STRING_EMPTY;

            //Устанавливаем язык приложения
            eventsData.setLocale(true);
            resources = getResources();

            //Устанавливаем тему
            //https://carthrottle.io/how-to-implement-flexible-night-mode-in-your-android-app-f00f0f83b70e
            //https://medium.com/@pkjvit/https-medium-com-pkjvit-android-multi-theme-night-mode-and-material-design-c186bf9fd678
            //https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94

            this.setTheme(eventsData.preferences_theme.themeMain);
            eventsData.currentTheme = eventsData.preferences_theme.themeMain;
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            setContentView(R.layout.activity_main);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна https://github.com/neokree/MaterialNavigationDrawer/issues/5
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            swipeRefresh = findViewById(R.id.swiperefresh);
            swipeRefresh.setOnRefreshListener(this); //Set the listener to be notified when a refresh is triggered via the swipe gesture

            //Обновляем меню https://stackoverflow.com/questions/14867458/android-refresh-options-menu-without-calling-invalidateoptionsmenu
            this.invalidateOptionsMenu();

            //About
            findViewById(R.id.toolbar).setOnClickListener(v -> {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            });

            swipeRefreshListener = () -> {
                //https://stackoverflow.com/questions/24587925/swiperefreshlayout-trigger-programmatically/35621309#35621309
                boolean canReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
                if (canReadContacts && (eventsData.isEmpty() || System.currentTimeMillis() - eventsData.statLastComputeDates > 1000)) {
                    if (eventsData.getContactsEvents(this)) {
                        eventsData.computeDates();
                        drawList();
                        eventsData.updateWidgets();
                        swipeRefresh = findViewById(R.id.swiperefresh);
                        if (swipeRefresh != null)
                            swipeRefresh.setRefreshing(false); // Disables the refresh icon

                        //Сообщение для тех, у кого не заведено ни одного события
                        if (!triggeredMsgNoEvents && eventsData.isEmpty()) {

                            boolean isTypesOn = false;
                            for (boolean t : eventsData.event_types_on)
                                if (t) {
                                    isTypesOn = true;
                                    break;
                                }

                            if (!eventsData.getPreferences_Accounts().isEmpty()) {

                                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                                builder.setTitle(getString(R.string.msg_no_events));
                                builder.setIcon(android.R.drawable.ic_menu_info_details);
                                builder.setMessage(getString(R.string.msg_no_events_check_prefs, getString(R.string.pref_Accounts_title), getString(R.string.pref_EventTypes_title)));
                                builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                                builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> startActivity(new Intent(this, SettingsActivity.class)));
                                AlertDialog alertToShow = builder.create();
                                alertToShow.setOnShowListener(arg0 -> {
                                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                    alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                });
                                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                alertToShow.show();
                                triggeredMsgNoEvents = true;

                            } else if (isTypesOn) {

                                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                                builder.setTitle(getString(R.string.msg_no_events));
                                builder.setIcon(android.R.drawable.ic_menu_info_details);
                                builder.setMessage(getString(R.string.msg_no_events_hint));
                                builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                                builder.setNeutralButton(R.string.button_open_addresbook, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.contacts/contacts"))));
                                AlertDialog alertToShow = builder.create();
                                alertToShow.setOnShowListener(arg0 -> {
                                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                    alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                });
                                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                alertToShow.show();
                                triggeredMsgNoEvents = true;
                            }

                        }
                    }
                }

            };
            swipeRefresh.post(() -> swipeRefreshListener.onRefresh());

            //Доступы
            if (checkAndShowNoAccessHint()) return;

            //Уведомления
            initNotifications();

            //Контексное меню
            registerForContextMenu(findViewById(R.id.mainListView));

            //Приветственное сообщение или описание новой версии
            showWelcomeScreen();

            //todo: сделать разные иконки приложения https://github.com/guardianproject/CameraV/commit/98d8c545c1901d03d9d238204bb45d502a623e59#diff-7ab4bf3d594a968a90e0250af33fcb9bR399
            // https://stackoverflow.com/questions/1103027/how-to-change-an-application-icon-programmatically-in-android

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    private void showWelcomeScreen() {

        try {

            AlertDialog.Builder builder;
            AlertDialog alertToShow;
            switch (checkNewVersion()) {
                case +1: //при запуске новой версии показывать what's new

                    StringBuilder sb = new StringBuilder();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) sb.append(HTML_BR);

                    String[] arrChangeLog = resources.getStringArray(R.array.changelog);
                    if (arrChangeLog.length >= 0) {

                        String currentVersion = STRING_EMPTY;
                        int countChanges = 0;

                        for(String strChange: arrChangeLog) {

                            if (strChange.charAt(0) == '#') {

                                if (!currentVersion.equals(STRING_EMPTY)) break;
                                currentVersion = strChange.substring(1);
                                if (!currentVersion.equals(BuildConfig.VERSION_NAME)) break;
                                sb.append("<ul>");

                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    sb.append("<li>&nbsp;").append(strChange).append("</li>");
                                } else {
                                    sb.append("<br>&nbsp;-&nbsp;").append(strChange);
                                }
                                countChanges++;

                            }
                        }
                        if (countChanges > 0) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                sb.append("</ul>");
                            } else {
                                sb.append(HTML_BR);
                            }

                            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                            builder.setTitle(getString(R.string.msg_newversion_title, currentVersion));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setMessage(HtmlCompat.fromHtml(sb.toString(), 0));
                            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                            builder.setNeutralButton(R.string.button_open_version_history, (dialog, which) -> startActivity(new Intent(this, AboutActivity.class)));
                            alertToShow = builder.create();
                            alertToShow.setOnShowListener(arg0 -> {
                                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                            });
                            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            alertToShow.show();
                        }

                    }
                    setLastRunVersion();
                    break;

                case 0: //при первом запуске показывать welcome screen

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.msg_welcome_title));
                    builder.setIcon(android.R.drawable.ic_menu_info_details);
                    builder.setMessage(getString(R.string.msg_welcome_text));
                    builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                    builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> startActivity(new Intent(this, SettingsActivity.class)));
                    alertToShow = builder.create();
                    alertToShow.setOnShowListener(arg0 -> {
                        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    });
                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertToShow.show();

                    setLastRunVersion();
                    break;

                default:
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_SHOW_WELCOME_SCREEN_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLastRunVersion() {

        try {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getString(R.string.pref_Version_LastRun), BuildConfig.VERSION_NAME);
            editor.putString(getString(R.string.pref_VersionCode_LastRun), Integer.toString(BuildConfig.VERSION_CODE));
            editor.apply();

            if (eventsData.preferences_debug_on) Toast.makeText(this, "Set last run version: " + BuildConfig.VERSION_NAME, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_SET_LASTRUN_VERSION_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    private int checkNewVersion() {
        // +1 - новая версия, 0 - первый запуск, -1 - такую версию уже запускали

        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final String pref_LastRunVersion = preferences.getString(getString(R.string.pref_VersionCode_LastRun), STRING_EMPTY);
            if (STRING_EMPTY.equals(pref_LastRunVersion)) {//если нет сохранённой версии, но настройки меняли - это не первый запуск
                if (!eventsData.preferences_list_prev_events.equals(getString(R.string.pref_List_PrevEvents_default)) ||
                        !eventsData.preferences_language.equals(getString(R.string.pref_Language_default)) ||
                        eventsData.preferences_notifications_days != Integer.parseInt(getString(R.string.pref_Notifications_Days_default)) ||
                        eventsData.preferences_theme.prefNumber != Integer.parseInt(getString(R.string.pref_Theme_default)) ||
                        eventsData.getHiddenEventsCount() == 0 ||
                        !eventsData.getPreferences_Accounts().isEmpty()
                ) {
                    return +1;
                } else {
                    return 0;
                }
            } else if (Integer.toString(BuildConfig.VERSION_CODE).equals(pref_LastRunVersion)) {
                return -1;
            }
            return +1;

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_CHECK_NEW_VERSION_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return -1;
        }
    }

    private boolean checkAndShowNoAccessHint() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            //https://developer.android.com/training/permissions/requesting.html#java
            swipeRefresh.setEnabled(false);
            setHint(setHTMLColor(getString(R.string.msg_no_access_contacts), HTML_COLOR_RED));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                builder.setTitle(getString(R.string.msg_no_access_contacts));
                builder.setIcon(android.R.drawable.ic_menu_info_details);
                builder.setMessage(getString(R.string.msg_no_access_contacts_hint));
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
                triggeredMsgNoEvents = true;

            } else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
            return true;
        }
        return false;
    }

    private void initNotifications() {
        //https://stackoverflow.com/questions/51343550/how-to-give-notifications-on-android-on-specific-time-in-android-oreo/51645875#51645875

        try{
            StringBuilder log = new StringBuilder();

            eventsData.initNotificationChannel(log); //для Android 8+
            eventsData.initBootReceiver(log);
            eventsData.initNotifications(log);

            if (eventsData.preferences_debug_on && log.length() > 0) Toast.makeText(this, log.toString(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_INIT_NOTIFICATIONS_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void onRefresh() {

        if (swipeRefreshListener == null) return;
        try {

            swipeRefreshListener.onRefresh();

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_REFRESH_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //https://stackoverflow.com/a/43411336/4928833
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!filterNames.equals(STRING_EMPTY) && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_zoom));
        }

        if (!eventsData.isEmpty() && eventsData.getHiddenEventsCount() > 0) { //показывать, если список скрытых не пустой
            menu.getItem(3).setVisible(true);
        }
        this.menu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        try {

            AlertDialog.Builder builder;
            AlertDialog alertToShow;

            switch (item.getItemId()) {
                case R.id.menu_refresh:

                    //Permissions
                    if (checkAndShowNoAccessHint()) return true;

                    //https://github.com/googlesamples/android-SwipeRefreshLayoutBasic/blob/master/Application/src/main/java/com/example/android/swiperefreshlayoutbasic/SwipeRefreshLayoutBasicFragment.java
                    //https://medium.com/@elye.project/swipe-to-refresh-not-showing-why-96b76c5c93e7
                    if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {

                        swipeRefresh.post(() -> {
                            swipeRefresh.setEnabled(false); // setEnable(false) need to be before setRefreshing
                            swipeRefresh.setRefreshing(true);

                            if (eventsData.getContactsEvents(getApplicationContext())) {
                                eventsData.computeDates();
                                drawList();
                                eventsData.updateWidgets();
                            }

                            swipeRefresh.setRefreshing(false); //Disables the refresh icon
                            swipeRefresh.setEnabled(true);
                        });
                    }
                    return true;

                case R.id.menu_settings:

                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);

                    return true;

                case R.id.menu_search:

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(R.string.title_activity_search);
                    builder.setIcon(android.R.drawable.ic_menu_search);
                    builder.setMessage(R.string.msg_label_search);

                    //https://stackoverflow.com/questions/10903754/input-text-dialog-android
                    final EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(filterNames);
                    input.setHint(R.string.msg_hint_search);
                    input.setHintTextColor(ta.getColor(R.styleable.Theme_dialogHintColor, 0));
                    input.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));

                    if (!filterNames.equals(STRING_EMPTY)) input.selectAll();
                    builder.setView(input);

                    builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
                        filterNames = input.getText().toString().replace(STRING_PARENTHESIS_START, "\\(").replace(STRING_PARENTHESIS_CLOSE, "\\)");

                        if (menu != null) {
                            //https://stackoverflow.com/questions/19882443/how-to-change-menuitem-icon-in-actionbar-programmatically/19882555#19882555
                            if (!filterNames.equals(STRING_EMPTY) && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_zoom));
                            } else {
                                menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                            }
                        }
                        drawList(); //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо
                    });

                    if (!filterNames.equals(STRING_EMPTY)) {
                        builder.setNeutralButton(R.string.button_clear, (dialog, which) -> {
                            filterNames = STRING_EMPTY;
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                            dialog.dismiss();
                            drawList(); //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо
                        });
                    }

                    builder.setNegativeButton(R.string.button_cancel, (dialog1, which) -> dialog1.cancel());

                    builder.setCancelable(true);

                    alertToShow = builder.create();

                    //https://stackoverflow.com/questions/27965662/how-can-i-change-default-dialog-button-text-color-in-android-5
                    alertToShow.setOnShowListener(arg0 -> {
                        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    });

                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    //https://stackoverflow.com/questions/4054662/displaying-soft-keyboard-whenever-alertdialog-builder-object-is-opened/6123935#6123935
                    //https://stackoverflow.com/questions/5593053/open-soft-keyboard-programmatically
                    input.requestFocus();
                    if (alertToShow.getWindow() != null) alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    alertToShow.show();

                    //https://stackoverflow.com/questions/15362122/change-font-size-for-an-alertdialog-message
                    TextView textView = alertToShow.findViewById(android.R.id.message);
                    textView.setTextSize(14);

                    return true;

                case R.id.menu_exit:

                    finish();
                    return true;

                case R.id.menu_hidden_events:

                    final CharSequence[] scopesArr = new CharSequence[]{
                            getString(R.string.events_scope_not_hidden, statsAllEvents - statsHiddenEvents),
                            getString(R.string.events_scope_all, statsAllEvents),
                            statsHiddenEvents == eventsData.getHiddenEventsCount() ? getString(R.string.events_scope_hidden, statsHiddenEvents) : getString(R.string.events_scope_hidden2, statsHiddenEvents, eventsData.getHiddenEventsCount())
                    };

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                            .setTitle(R.string.title_activity_events_scope)
                            .setIcon(android.R.drawable.ic_menu_sort_by_size)
                            .setSingleChoiceItems(scopesArr, eventsData.preferences_events_scope, (dialog, which) -> {
                                eventsData.preferences_events_scope = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                eventsData.setPreferences();
                                dialog.cancel();
                                drawList();
                            })
                            .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                            .setCancelable(true);

                    alertToShow = builder.create();

                    alertToShow.setOnShowListener(arg0 -> {
                        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                        float dpi = resources.getDisplayMetrics().density;
                        ListView listView = alertToShow.getListView();
                        listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)));
                        listView.setDividerHeight(2);
                        listView.setPadding((int)(30*dpi), 0, (int)(20*dpi), 0);
                    });

                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertToShow.show();

                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_OPTIONS_ITEM_SELECTED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        try {
            //https://stackoverflow.com/questions/18337536/android-overriding-onbackpressed
            if (filterNames.equals(STRING_EMPTY)) {

                super.onBackPressed();
                finish();

            } else {

                filterNames = STRING_EMPTY;
                if (menu != null) menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                drawList(); //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо

            }
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_BACK_PRESSED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        try {
            super.onResume();

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = getApplicationContext(); //this
            eventsData.getPreferences();

            //Устанавливаем язык приложения
            eventsData.setLocale(true);
            resources = getResources();

            //Устанавливаем тему и переоткрываем окно
            if (eventsData.currentTheme != eventsData.preferences_theme.themeMain) {
                this.setTheme(eventsData.preferences_theme.themeMain);
                this.recreate();
                return;
            }

            this.invalidateOptionsMenu();

            //Тему не меняли, просто обновляем данные
            boolean canReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
            if (canReadContacts && (eventsData.isEmpty() || System.currentTimeMillis() - eventsData.statLastComputeDates > 1000)) {
                if (eventsData.getContactsEvents(this)) {
                    eventsData.computeDates();
                    drawList();
                    eventsData.updateWidgets();
                }

                //Уведомления
                initNotifications();
            }


        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_RESUME_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerForContextMenu(findViewById(R.id.mainListView));
                if (eventsData.getContactsEvents(this)) {
                    eventsData.computeDates();
                    drawList();
                    eventsData.updateWidgets();
                }
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false); //Disables the refresh icon
                    swipeRefresh.setEnabled(true);
                }

                showWelcomeScreen();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //https://stackoverflow.com/questions/18632331/using-contextmenu-with-listview-in-android
        try {
            if (v.getId() == R.id.mainListView) {

                ListView l = (ListView) v;
                AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
                selectedEvent_str = (String)l.getItemAtPosition(acmi.position);
                selectedEvent = selectedEvent_str.split(Constants.STRING_2HASH);

                //todo: добавить стиль меню https://stackoverflow.com/questions/4604562/override-context-menu-colors-in-android
                //todo: добавить иконки https://stackoverflow.com/questions/1245543/add-context-menu-icon-in-android
                //todo: подсказки про именины на основе имени и даты рождения
                //todo: знаки зодиака и года
                //todo: ссылки с имени и фамилии на web справочник
                //menu.setHeaderTitle(dataArray1[ContactsEvents.dataMap.get("fio")] + ":");
                menu.add(Menu.NONE, R.integer.menu_context_id_open_card, Menu.NONE, getString(R.string.menu_context_open_card));
                menu.add(Menu.NONE, R.integer.menu_context_id_events, Menu.NONE, getString(R.string.menu_context_events));

                if (eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(selectedEvent[Position_contact_id] + Constants.STRING_2HASH + selectedEvent[Position_eventType])) {

                    menu.add(Menu.NONE, R.integer.menu_context_id_unhide_event, Menu.NONE, getString(R.string.menu_context_unhide_event));

                } else if (!selectedEvent[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM))) { //для кастомных пока подумаю

                    menu.add(Menu.NONE, R.integer.menu_context_id_hide_event, Menu.NONE, getString(R.string.menu_context_hide_event));

                }

                if (eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(selectedEvent[Position_contact_id] + Constants.STRING_2HASH + selectedEvent[Position_eventType])) {

                    menu.add(Menu.NONE, R.integer.menu_context_id_unsilent_event, Menu.NONE, getString(R.string.menu_context_unsilent_event));

                } else if (!selectedEvent[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM))) { //для кастомных пока подумаю

                    menu.add(Menu.NONE, R.integer.menu_context_id_silent_event, Menu.NONE, getString(R.string.menu_context_silent_event));

                }

                //https://stackoverflow.com/questions/7042958/android-adding-a-submenu-to-a-menuitem-where-is-addsubmenu
                SubMenu sub = menu.addSubMenu(Menu.NONE, R.integer.menu_context_id_remind, Menu.NONE, getString(R.string.menu_context_remind));
                sub.add(Menu.NONE, R.integer.menu_context_id_remind_1h, Menu.NONE, getString(R.string.menu_context_remind_1h));
                sub.add(Menu.NONE, R.integer.menu_context_id_remind_morning, Menu.NONE, getString(R.string.menu_context_remind_morning));

                if (selectedEvent[Position_eventType].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY)) ) {
                    menu.add(Menu.NONE, R.integer.menu_context_id_anniversary_list , Menu.NONE, getString(R.string.menu_context_anniversary_list));
                }

                if (eventsData.preferences_debug_on) {
                    menu.add(Menu.NONE, R.integer.menu_context_id_event_info, Menu.NONE, getString(R.string.menu_context_event_info));
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_CREATE_CONTEXT_MENU_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try {

            switch (item.getItemId()) {
                case R.integer.menu_context_id_open_card:

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, selectedEvent[Position_contact_id]);
                    intent.setData(uri);
                    MainActivity.this.startActivity(intent);
                    return true;

                case R.integer.menu_context_id_events:

                    filterNames = Constants.STRING_2HASH + selectedEvent[Position_contact_id] + Constants.STRING_2HASH;
                    if (menu != null) menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_zoom));
                    drawList();
                    return true;

                case R.integer.menu_context_id_event_info:

                    StringBuilder eventInfo = new StringBuilder();

                    for (int i = 0; i < selectedEvent.length; i++) {
                        eventInfo.append(i).append(STRING_COLON_SPACE);
                        //eventInfo.append(i != ContactsEvents.Position_lastContacted || selectedEvent[i].equals(STRING_3MINUS) ? selectedEvent[i] : new Date(Long.parseLong(selectedEvent[i])).toString());
                        eventInfo.append(selectedEvent[i]);
                        eventInfo.append(STRING_EOF);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                            .setTitle(selectedEvent[Position_fio])
                            .setIcon(new BitmapDrawable(resources, ContactsEvents.getInstance().getContactPhoto(selectedEvent_str, true, false )))
                            .setMessage(eventInfo.toString())
                            .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());

                    AlertDialog alertToShow = builder.create();
                    alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0)));
                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    alertToShow.show();

                    TextView textView = alertToShow.findViewById(android.R.id.message);
                    textView.setTextSize(14);

                    return true;

                case R.integer.menu_context_id_hide_event:

                    if (eventsData.setHiddenEvent(selectedEvent[Position_contact_id] + Constants.STRING_2HASH + selectedEvent[Position_eventType])) {
                        this.invalidateOptionsMenu();
                        drawList();
                        eventsData.updateWidgets();
                    }
                    return true;

                case R.integer.menu_context_id_unhide_event:

                    if (eventsData.unsetHiddenEvent(selectedEvent[Position_contact_id] + Constants.STRING_2HASH + selectedEvent[Position_eventType])) {
                        this.invalidateOptionsMenu();
                        drawList();
                        eventsData.updateWidgets();
                    }
                    return true;

                case R.integer.menu_context_id_remind_1h:

                    eventsData.snoozeNotification(selectedEvent_str, 1, null);
                    return true;

                case R.integer.menu_context_id_remind_morning:

                    Calendar now = Calendar.getInstance();
                    now.add(Calendar.DAY_OF_MONTH, 1);
                    now.set(Calendar.HOUR_OF_DAY, 9);
                    now.set(Calendar.MINUTE, 0);
                    now.set(Calendar.SECOND, 0);
                    now.set(Calendar.MILLISECOND, 0);

                    eventsData.snoozeNotification(selectedEvent_str, 0, now.getTime());
                    return true;

                case R.integer.menu_context_id_anniversary_list:

                    eventsData.showAnniversaryList(this);
                    return true;

                case R.integer.menu_context_id_silent_event:

                    if (eventsData.setSilencedEvent(selectedEvent[Position_contact_id] + Constants.STRING_2HASH + selectedEvent[Position_eventType])) {
                        this.invalidateOptionsMenu();
                        drawList();
                        eventsData.updateWidgets();
                    }
                    return true;

                case R.integer.menu_context_id_unsilent_event:

                    if (eventsData.unsetSilencedEvent(selectedEvent[Position_contact_id] + Constants.STRING_2HASH + selectedEvent[Position_eventType])) {
                        this.invalidateOptionsMenu();
                        drawList();
                        eventsData.updateWidgets();
                    }
                    return true;

                default:
                    return super.onContextItemSelected(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_ON_CONTEXT_ITEM_SELECTED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private void drawList() {
        long statCurrentModuleStart = System.currentTimeMillis();

        String[] dataArray_final = {};
        eventsData.preferences_list_prev_events_found = 0;
        try {

            statsAllEvents = 0;
            statsHiddenEvents = 0;

            //Проверки
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                setHint(setHTMLColor(getString(R.string.msg_no_access_contacts), HTML_COLOR_RED));
            else if (eventsData.isEmpty())
                setHint(setHTMLColor(getString(R.string.msg_no_events), HTML_COLOR_YELLOW));
            else {

                statsAllEvents = eventsData.dataArray.length;

                //Обрабатываем скрытые события
                List<String> dataList = new ArrayList<>();
                for (int i = 0; i < eventsData.dataArray.length; i++) {
                    String event = eventsData.dataArray[i];
                    String[] singleRowArray = event.split(Constants.STRING_2HASH);
                    final String eventKey = singleRowArray[Position_contact_id] + Constants.STRING_2HASH + singleRowArray[Position_eventType];

                    if (eventsData.getHiddenEventsCount() == 0) { //Скрытых нет
                        dataList.add(event);
                    } else if (eventsData.preferences_events_scope == Constants.pref_Events_Scope_NotHidden) { //Показывать нескрытые
                        if (!eventsData.checkIsHiddenEvent(eventKey)) {
                            dataList.add(event);
                        } else {
                            statsHiddenEvents++;
                        }
                    } else if (eventsData.preferences_events_scope == Constants.pref_Events_Scope_Hidden) { //Показывать только скрытые
                        if (eventsData.checkIsHiddenEvent(eventKey)) {
                            dataList.add(event);
                            statsHiddenEvents++;
                        }
                    } else { //Показывать все
                        dataList.add(event);
                        if (eventsData.checkIsHiddenEvent(eventKey)) statsHiddenEvents++;
                    }
                }

                //Фильтруем
                if (!filterNames.equals(STRING_EMPTY)) {

                    List<String> dataList_filtered = new ArrayList<>();

                    //для поиска AND используем <строка1>.*<строка2>
                    Matcher filter = Pattern.compile(filterNames.replaceAll(REGEX_COMMAS, ",").toUpperCase().replace("Ё", "Е").replace(STRING_COMMA, "|"), Pattern.CASE_INSENSITIVE).matcher(STRING_EMPTY);
                    for (String listItem : dataList)
                        if (filter.reset(listItem).find()){
                            if (!dataList_filtered.contains(listItem)) {
                                dataList_filtered.add(listItem);
                            }
                        }

                    dataArray_final = dataList_filtered.toArray(new String[0]);
                    if (dataArray_final.length > 0) {
                        //statsAllEvents = dataArray_final.length;
                        if (eventsData.preferences_events_scope == Constants.pref_Events_Scope_Hidden) {
                            setHint(resources.getString(R.string.msg_stats_hidden_prefix) + Constants.STRING_SPACE +
                                    dataArray_final.length + getString(R.string.msg_filter) + setHTMLColor(filterNames, HTML_COLOR_YELLOW));
                        } else {
                            setHint(resources.getString(R.string.msg_stats_prefix) + Constants.STRING_SPACE +
                                    dataArray_final.length + getString(R.string.msg_filter) + setHTMLColor(filterNames, HTML_COLOR_YELLOW));
                        }

                    } else {
                        setHint(getString(R.string.msg_no_events) + getString(R.string.msg_filter) + setHTMLColor(filterNames, HTML_COLOR_YELLOW));
                    }

                } else {

                    //Получаем предыдцщие события
                    if (!eventsData.preferences_list_prev_events.equals(STRING_EMPTY) && eventsData.preferences_events_scope != Constants.pref_Events_Scope_Hidden) {
                        //todo: придумать, как ловить прошедшие 5K+
                        dataList = eventsData.insertPreviousEvents(dataList, eventsData.preferences_list_prev_events);
                    }

                    dataArray_final = dataList.toArray(new String[0]);
                    //statsAllEvents = dataArray_final.length;
                    if (eventsData.preferences_events_scope == Constants.pref_Events_Scope_Hidden) {
                        setHint(resources.getString(R.string.msg_stats_hidden_prefix) + Constants.STRING_SPACE + statsHiddenEvents);
                    } else if (eventsData.preferences_events_scope == Constants.pref_Events_Scope_All) {
                        setHint(resources.getString(R.string.msg_stats_prefix) + Constants.STRING_SPACE + statsAllEvents);
                    } else {
                        setHint(resources.getString(R.string.msg_stats_prefix) + Constants.STRING_SPACE + (statsAllEvents - statsHiddenEvents));
                    }

                }
            }

            //Выводим данные
            ListAdapter adapter = new MyAdapter(this, Arrays.copyOf(dataArray_final, dataArray_final.length));

            ListView listView = findViewById(R.id.mainListView);

            //Сохраняем позицию в списке, чтобы вернутся к ней после обновления
            //https://stackoverflow.com/a/3035521/4928833
            int index = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
            int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

            listView.setAdapter(adapter);

            //http://androidopentutorials.com/android-listview-fastscroll/
            listView.setFastScrollEnabled(true);

            listView.setOnItemClickListener((l, v1, position, id) -> {
                try {

                    String[] dataArray1 = ((String) l.getItemAtPosition(position)).split(Constants.STRING_2HASH);

                    //https://stackoverflow.com/questions/4275167/how-to-open-a-contact-card-in-android-by-id?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, dataArray1[Position_contact_id]);
                    intent.setData(uri);
                    MainActivity.this.startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, Constants.MAIN_ACTIVITY_DRAW_LIST_ON_ITEM_CLICK_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            listView.requestFocus();
            //Возвращаемся к ранее сохранённой позиции после обновления
            listView.setSelectionFromTop(index, top);

            eventsData.statTimeDrawList = System.currentTimeMillis() - statCurrentModuleStart;

        } catch (Exception e) {
            eventsData.statTimeDrawList = System.currentTimeMillis() - statCurrentModuleStart;
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_DRAW_LIST_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void setHint(@NonNull String msg) {

        try {

            TextView stats = findViewById(R.id.mainStatsTextView);
            stats.setText(HtmlCompat.fromHtml(msg, 0), TextView.BufferType.SPANNABLE);

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_SET_HINT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    private String setHTMLColor(String msg, int color) {
        try {

            int colorId;
            switch (color) {
                case HTML_COLOR_RED:
                    colorId = R.color.dark_red;
                    break;
                case HTML_COLOR_YELLOW:
                    colorId = R.color.yellow;
                    break;
                case HTML_COLOR_BROWN:
                    colorId = R.color.brown;
                    break;

                default:
                    return msg;
            }
            return String.format(HTML_COLOR, Integer.toHexString(ContextCompat.getColor(this, colorId) & 0x00ffffff), msg);

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.MAIN_ACTIVITY_SET_HTML_COLOR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return msg;
    }

    private static class ViewHolder {
        //https://stackoverflow.com/questions/21501316/what-is-the-benefit-of-viewholder

        final TextView NameTextView;
        final TextView DayDistanceTextView;
        final TextView DateTextView;
        final TextView DetailsTextView;
        final ImageView PhotoImageView;
        final TextView CounterTextView;
        final ImageView EventIconImageView;

        ViewHolder(TextView NameTextView, TextView DayDistanceTextView, TextView DateTextView, TextView DetailsTextView, ImageView PhotoImageView, TextView CounterTextView, ImageView EventIconImageView) {
            this.NameTextView = NameTextView;
            this.DayDistanceTextView = DayDistanceTextView;
            this.DateTextView = DateTextView;
            this.DetailsTextView = DetailsTextView;
            this.PhotoImageView = PhotoImageView;
            this.CounterTextView = CounterTextView;
            this.EventIconImageView = EventIconImageView;
        }
    }

    class MyAdapter extends ArrayAdapter<String>
    {

        String tag_Bold_start;
        final String tag_Bold_end = "</font>";
        final ContactsEvents eventsData;
        final Resources resources;

        private MyAdapter(Context context, String[] values)
        {
            super(context, R.layout.entry_main, values);
            resources = getResources();
            eventsData = ContactsEvents.getInstance();
        }

        @NonNull public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            String[] singleRowArray;
            Context context = getContext();
            Person person;
            String event;

            try {

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(context);
                    convertView = inflater.inflate(R.layout.entry_main, parent, false);
                    holder = createViewHolderFrom(convertView);
                    convertView.setTag(holder);
                }
                holder = (ViewHolder) convertView.getTag();

                if (getItem(position) != null) {
                    event = getItem(position);
                    singleRowArray = event.split(Constants.STRING_2HASH);
                    person = new Person(context, getItem(position));
                } else {
                    return convertView;
                }

//                if (singleRowArray.length <= 9) {
//                    Toast.makeText(context, Constants.MY_ADAPTER_GET_VIEW_ERROR_ABNORMAL_DIMENSION_OF_STRING_START + singleRowArray.length + Constants.MY_ADAPTER_GET_VIEW_ERROR_ABNORMAL_DIMENSION_OF_STRING_END + singleRowArray[0], Toast.LENGTH_SHORT).show();
//                    return convertView;
//                }

                if (tag_Bold_start == null) {
                    //https://stackoverflow.com/questions/5026995/android-get-color-as-string-value
                    tag_Bold_start = "<font color=\"#" + Integer.toHexString(ta.getColor(R.styleable.Theme_eventFullNameColor, ContextCompat.getColor(context, R.color.medium_gray)) & 0x00ffffff) + "\">";
                }

                String eventDistance = singleRowArray[ContactsEvents.Position_eventDistance];
                String eventDistanceText = singleRowArray[ContactsEvents.Position_eventDistanceText];
                switch (eventDistance) {

                    case STRING_0: //Сегодня

                        holder.DayDistanceTextView.setText(eventDistanceText);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(context, R.color.dark_red));
                        break;

                    case STRING_1: //Завтра и послезавтра
                    case STRING_2:

                        holder.DayDistanceTextView.setText(eventDistanceText);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(context, R.color.dark_green));
                        break;

                    default: //Попозже
                        holder.DayDistanceTextView.setText(eventDistanceText.toLowerCase());
                        holder.DayDistanceTextView.setTypeface(null, Typeface.NORMAL);
                        holder.DayDistanceTextView.setTextColor(ta.getColor(R.styleable.Theme_eventDistanceColor, ContextCompat.getColor(context, R.color.dark_gray)));

                }

                //Дата оригинального события
                holder.DateTextView.setText(singleRowArray[ContactsEvents.Position_eventDateText]);

                switch (eventsData.preferences_list_caption) {
                    case 2: //Фамилия Имя Отчество
                        holder.NameTextView.setText(person.getFullName());
                        break;
                    case 1: //Имя Отчество Фамилия
                    default:
                        holder.NameTextView.setText(person.getFullNameAlt());
                        break;
                }

                //holder.NameTextView.setText(singleRowArray[ContactsEvents.Position_fio]);

                //Инфо под именем
                StringBuilder eventDetails = new StringBuilder();
                if (eventsData.preferences_list_event_info.contains(STRING_1) && eventsData.preferences_list_event_info.contains(STRING_2)) {
                    if (singleRowArray[ContactsEvents.Position_organization].trim().length() > 0) {
                        eventDetails.append(singleRowArray[ContactsEvents.Position_organization].trim());
                    }
                    if (singleRowArray[ContactsEvents.Position_title].trim().length() > 0) {
                        if (eventDetails.length() > 0) eventDetails.append(STRING_COMMA_SPACE);
                        eventDetails.append(singleRowArray[ContactsEvents.Position_title].trim());
                    }
                    if (eventDetails.length() > 0) {
                        eventDetails.insert(0, tag_Bold_start).append(tag_Bold_end);
                    }
                } else if (eventsData.preferences_list_event_info.contains(STRING_1) && singleRowArray[ContactsEvents.Position_organization].trim().length() > 0) {
                    eventDetails.append(Constants.HTML_BOLD_START).append(singleRowArray[ContactsEvents.Position_organization].trim()).append(Constants.HTML_BOLD_END);
                } else if (eventsData.preferences_list_event_info.contains(STRING_2) && singleRowArray[ContactsEvents.Position_title].trim().length() > 0) {
                    eventDetails.append(Constants.HTML_BOLD_START).append(singleRowArray[ContactsEvents.Position_title].trim()).append(Constants.HTML_BOLD_END);
                }
                if (eventsData.preferences_list_event_info.contains("7") && singleRowArray[ContactsEvents.Position_nickname].trim().length() > 0) {
                    if (eventDetails.length() > 0) eventDetails.append(HTML_BR);
                    eventDetails.append(singleRowArray[ContactsEvents.Position_nickname]);
                }

                String eventLabel = singleRowArray[ContactsEvents.Position_eventLabel];
                String eventCaption = singleRowArray[ContactsEvents.Position_eventCaption];
                if (eventsData.preferences_list_event_info.contains(STRING_3)) {
                    if (eventDetails.length() > 0) eventDetails.append(HTML_BR);
                    if (eventsData.preferences_list_event_info.contains(STRING_4) && !eventCaption.equals(eventLabel) && !eventLabel.equals(STRING_EMPTY)) {
                        eventDetails.append(eventCaption).append(Constants.STRING_PARENTHESIS_OPEN).append(eventLabel).append(STRING_PARENTHESIS_CLOSE);
                    } else {
                        eventDetails.append(eventCaption);
                    }
                } else if (eventsData.preferences_list_event_info.contains(STRING_4) && !eventLabel.equals(STRING_EMPTY)) {
                    if (eventDetails.length() > 0) eventDetails.append(HTML_BR);
                    eventDetails.append(eventLabel);
                }

                if (eventsData.preferences_list_event_info.contains("91")) {
                    if (eventDetails.length() > 0) eventDetails.append(HTML_BR);
                    eventDetails.append(singleRowArray[ContactsEvents.Position_dates].replace(Constants.STRING_2TILDA, HTML_BR).trim());
                }

                if (eventsData.preferences_events_scope == Constants.pref_Events_Scope_All && eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(singleRowArray[Position_contact_id] + Constants.STRING_2HASH + singleRowArray[Position_eventType])) {
                    if (eventDetails.length() > 0) eventDetails.append(HTML_BR);
                    eventDetails.append(setHTMLColor(getString(R.string.msg_label_hidden), HTML_COLOR_RED));
                }
                if (eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(singleRowArray[Position_contact_id] + Constants.STRING_2HASH + singleRowArray[Position_eventType])) {
                    if (eventDetails.length() > 0) eventDetails.append(HTML_BR);
                    eventDetails.append(setHTMLColor(getString(R.string.msg_label_silenced), HTML_COLOR_BROWN));
                }

                if (eventDetails.length() == 0) {
                    holder.DetailsTextView.setText(STRING_EMPTY);
                } else {
                    //https://stackoverflow.com/questions/2116162/how-to-display-html-in-textview
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        holder.DetailsTextView.setText(HtmlCompat.fromHtml(eventDetails.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT));
                    } else {
                        holder.DetailsTextView.setText(HtmlCompat.fromHtml(eventDetails.toString(), 0));
                    }
                }

                //Определяем иконку события

                //Фото
                holder.PhotoImageView.setImageBitmap(eventsData.getContactPhoto(event, eventsData.preferences_list_event_info.contains("6"), false));

                if (person.Age > -1 && person.Age % 10 == 0) {
                    holder.CounterTextView.setTextColor(resources.getColor(R.color.dark_red));
                } else {
                    holder.CounterTextView.setTextColor(ta.getColor(R.styleable.Theme_eventAgeColor, ContextCompat.getColor(context, R.color.medium_gray)));
                }
                holder.CounterTextView.setText(person.Age_str);

                //Определяем иконку события
                int eventIcon;
                try {
                    eventIcon = Integer.parseInt(singleRowArray[ContactsEvents.Position_eventIcon]);
                } catch (NumberFormatException e) {
                    eventIcon = 0;
                }
                if (eventIcon != 0) {
                    holder.EventIconImageView.setImageResource(eventIcon);
                } else {
                    holder.EventIconImageView.setImageDrawable(null);
                }

            } catch (Exception e) {
                e.printStackTrace();
                //todo: исправить IllegalStateException на API28+ https://stackoverflow.com/questions/39689494/unable-to-add-window-android https://geekscompete.blogspot.com/2018/08/unable-to-add-window-token.html
                //if (Build.VERSION.SDK_INT >= 28) {Toast.cancel();}
                if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.MY_ADAPTER_GET_VIEW_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            }
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                return inflater.inflate(R.layout.entry_main, parent, false);
            } else {

                convertView.setBackground(null);
                convertView.setAlpha(1);
                if (eventsData.preferences_list_prev_events_found > 0) {

                    if (position <= eventsData.preferences_list_prev_events_found - 1) convertView.setAlpha((float)0.6);
                    if (position == eventsData.preferences_list_prev_events_found - 1)  convertView.setBackground(context.getDrawable(R.drawable.prev_event_border));

                }
                return convertView;
            }
        }

        private ViewHolder createViewHolderFrom(@NonNull View view) {

            TextView NameTextView = view.findViewById(R.id.entryNameTextView);
            TextView DayDistanceTextView = view.findViewById(R.id.entryDayDistanceTextView);
            TextView DateTextView = view.findViewById(R.id.entryDateTextView);
            TextView DetailsTextView = view.findViewById(R.id.entryEventDetailsTextView);
            TextView CounterTextView = view.findViewById(R.id.entryDetailsCounter);
            ImageView PhotoImageView = view.findViewById(R.id.entryPhotoImageView);
            ImageView EventIconImageView = view.findViewById(R.id.entryEventIcon);

            return new ViewHolder(NameTextView, DayDistanceTextView, DateTextView, DetailsTextView, PhotoImageView, CounterTextView, EventIconImageView);
        }

    }

}