/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 17.09.2022, 20:32
 *
 */

package org.vovka.birthdaycountdown;

import static android.view.View.GONE;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_age_caption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventCaption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventDate;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventEmoji;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventSubType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_personFullName;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_personFullNameAlt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainActivity";

    //UI объекты
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Resources resources;
    private SwipeRefreshLayout swipeRefresh;
    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener;
    private EventsAdapter adapter;

    //Переменные
    private String filterNames = Constants.STRING_EMPTY;
    private ContactsEvents eventsData;
    private String selectedEvent_str;
    private String[] selectedEvent;
    private int selectedEvent_num;
    final private List<String> dataList = new ArrayList<>();
    private final List<String> dataListFull = new ArrayList<>();

    private int statsAllEvents = 0;
    private int statsHiddenEvents = 0;
    private int statsSilencedEvents = 0;
    private boolean triggeredMsgNoEvents = false;

    private TypedArray ta = null;
    DisplayMetrics displayMetrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            //https://habr.com/ru/post/648535/
            //SplashScreen splashScreen = SplashScreen.installSplashScreen(this); - с вызовом этого не ставится цвет статуса и кнопок

            //Оформление стиля окна приложения
            //https://stackoverflow.com/questions/22192291/how-to-change-the-status-bar-color-in-android
            //https://stackoverflow.com/questions/29069070/completely-transparent-status-bar-and-navigation-bar-on-lollipop
            //Window w = getWindow();
            //w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            //w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(this);
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
                applicationConf.setLocales(new LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            //Устанавливаем язык приложения
            eventsData.setLocale(true);
            resources = getResources();
            displayMetrics = resources.getDisplayMetrics();

            //Устанавливаем тему
            //https://carthrottle.io/how-to-implement-flexible-night-mode-in-your-android-app-f00f0f83b70e
            //https://medium.com/@pkjvit/https-medium-com-pkjvit-android-multi-theme-night-mode-and-material-design-c186bf9fd678
            //https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94

            this.setTheme(eventsData.preferences_theme.themeMain);
            eventsData.currentTheme = eventsData.preferences_theme.themeMain;
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            //https://developer.android.com/topic/performance/vitals/launch-time#java
            //A common way to implement a themed launch screen is to use the windowDisablePreview theme attribute to turn off the initial blank screen that the system process draws when launching the app.
            // However, this approach can result in a longer startup time than apps that don’t suppress the preview window
            super.onCreate(savedInstanceState);
            filterNames = savedInstanceState == null ? Constants.STRING_EMPTY : savedInstanceState.getString(Constants.EXTRA_FILTER, Constants.STRING_EMPTY);

            //eventsData.setAppIcon();

            setContentView(R.layout.activity_main);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна https://github.com/neokree/MaterialNavigationDrawer/issues/5
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            if (eventsData.preferences_list_custom_caption.isEmpty()) {
                toolbar.setTitle(R.string.app_name);
            } else {
                toolbar.setTitle(eventsData.preferences_list_custom_caption);
            }

            setSupportActionBar(toolbar);

            swipeRefresh = findViewById(R.id.swiperefresh);
            if (swipeRefresh != null) {
                swipeRefresh.setColorSchemeColors(
                        ta.getColor(R.styleable.Theme_colorPrimary, Color.BLACK),
                        eventsData.preferences_list_color_eventtoday,
                        eventsData.preferences_list_color_eventsoon
                );
                swipeRefresh.setOnRefreshListener(this); //Set the listener to be notified when a refresh is triggered via the swipe gesture
            }

            //Обновляем меню https://stackoverflow.com/questions/14867458/android-refresh-options-menu-without-calling-invalidateoptionsmenu
            this.invalidateOptionsMenu();

            //About
            findViewById(R.id.toolbar).setOnClickListener(v -> {
                Intent intent = new Intent(this, AboutActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }
            });

            swipeRefreshListener = () -> {
                try {
                    //https://stackoverflow.com/questions/24587925/swiperefreshlayout-trigger-programmatically/35621309#35621309
                    if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates >= Constants.TIME_SPEED_LOAD_OVERTIME) {

                        updateList(false, eventsData.statTimeComputeDates >= Constants.TIME_SPEED_LOAD_OVERTIME);

                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    if (eventsData.preferences_debug_on)
                        ToastExpander.showText(MainActivity.this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                } finally {
                    swipeRefresh.setRefreshing(false);
                }
            };
            swipeRefresh.post(() -> swipeRefreshListener.onRefresh());

            //Уведомления
            //initNotifications();

            ListView listView = findViewById(R.id.mainListView);

            //Разделитель списка зависит от стиля отображения
            if (eventsData.preferences_list_style == Integer.parseInt(getString(R.string.pref_List_Style_Card))) {

                listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_backgroundColor, 0)));
                listView.setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics));
                int listPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics);
                listView.setPadding(listPadding, listPadding, listPadding, listPadding);

            } else {

                listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)));
                listView.setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics));
                listView.setPadding(0, 0, 0, 0);

            }

            listView.setOnItemClickListener((l, v1, position, id) -> {
                try {

                    if (eventsData.preferences_list_on_click_action == 8) { //Контекстное меню
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            l.showContextMenuForChild(v1, v1.getRight()-v1.getLeft(), (v1.getBottom() - v1.getTop()) >> 1);
                        } else {
                            openContextMenu(v1);
                        }
                    } else if (eventsData.preferences_list_on_click_action >= 1 & eventsData.preferences_list_on_click_action <= 4) {
                        Intent intent = ContactsEvents.getViewActionIntent(
                                ((String) l.getItemAtPosition(position)).split(Constants.STRING_EOT, -1),
                                eventsData.preferences_list_on_click_action
                        );
                        if (intent != null) {
                            try {
                                MainActivity.this.startActivity(intent);
                            } catch (ActivityNotFoundException e) { /**/ }
                        } else {
                            if (eventsData.preferences_debug_on) ToastExpander.showText(this, Constants.MSG_NO_ACTION);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                }
            });

            listView.setOnItemLongClickListener((parent, v, position, id) -> {
                try {

                    //https://developer.alexanderklimov.ru/android/popupmenu.php
                    //https://stackoverflow.com/questions/1245543/add-context-menu-icon-in-android
                    //https://stackoverflow.com/questions/49706495/how-to-pass-a-custom-layout-to-a-popupmenu
                    //https://stackoverflow.com/questions/64236522/how-to-implement-android-custom-popup-menu
                    //https://stackoverflow.com/questions/23516247/how-change-position-of-popup-menu-on-android-overflow-button
                    PopupMenu popupMenu = new PopupMenu(MainActivity.this, v, Gravity.RIGHT | Gravity.FILL_VERTICAL);
                    final Menu menu = popupMenu.getMenu();
                    setMenuIconsVisible(menu);

                    selectedEvent_num = position;
                    selectedEvent_str = (String)listView.getItemAtPosition(position);
                    selectedEvent = selectedEvent_str.split(Constants.STRING_EOT, -1);

                    //https://stackoverflow.com/questions/18632331/using-contextmenu-with-listview-in-android
                    //menu.setHeaderTitle(dataArray1[ContactsEvents.dataMap.get("fio")] + ":");

                    String contactID = selectedEvent[ContactsEvents.Position_contactID];
                    boolean isRealContactID = contactID != null && !contactID.isEmpty() && ContactsEvents.isRealContactEventID(contactID);
                    if (isRealContactID) {
                        menu.add(Menu.NONE, Constants.ContextMenu_EditContact, Menu.NONE, getString(R.string.menu_context_edit_contact))
                                .setIcon(android.R.drawable.ic_menu_edit);
                    } else {
                        MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_CreateContact, Menu.NONE, getString(R.string.menu_context_create_contact))
                                .setIcon(android.R.drawable.ic_menu_add);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                        }
                    }

                    if (!selectedEvent[ContactsEvents.Position_eventID].isEmpty()) { //(selectedEvent[Position_eventStorage].equals(STRING_STORAGE_CALENDAR)) {
                        menu.add(Menu.NONE, Constants.ContextMenu_EditEvent, Menu.NONE, getString(R.string.menu_context_edit_event))
                                .setIcon(android.R.drawable.ic_menu_month);

                        if (selectedEvent[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay))) {
                            if (!eventsData.getMergedID(selectedEvent[ContactsEvents.Position_eventID]).isEmpty()) {
                                menu.add(Menu.NONE, Constants.ContextMenu_UnmergeEvent, Menu.NONE, getString(R.string.menu_context_unmerge_event))
                                        .setIcon(R.drawable.ic_menu_chat_dashboard);
                                menu.add(Menu.NONE, Constants.ContextMenu_RemergeEvent, Menu.NONE, getString(R.string.menu_context_remerge_event))
                                        .setIcon(R.drawable.ic_menu_copy);
                            } else if (selectedEvent[ContactsEvents.Position_eventStorage].equals(Constants.STRING_STORAGE_CALENDAR) && selectedEvent[ContactsEvents.Position_contactID].isEmpty()) {
                                menu.add(Menu.NONE, Constants.ContextMenu_MergeEvent, Menu.NONE, getString(R.string.menu_context_merge_event))
                                        .setIcon(R.drawable.ic_menu_copy);
                            }
                        }
                    }

                    String[] eventURLs = selectedEvent[ContactsEvents.Position_eventURL].trim().split(Constants.STRING_2TILDA);
                    if (!eventURLs[0].isEmpty()) {
                        if (eventURLs.length == 1) {
                            menu.add(Menu.NONE, Constants.ContextMenu_OpenURL, Menu.NONE, getString(R.string.menu_context_open_url))
                                    .setIcon(android.R.drawable.ic_menu_directions);
                        } else {
                            SubMenu sub = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.menu_context_open_url))
                                    .setIcon(android.R.drawable.ic_menu_directions);
                            for (int i = 0, eventURLsLength = eventURLs.length; i < eventURLsLength; i++) {
                                String url = eventURLs[i];
                                sub.add(i, Constants.ContextMenu_OpenURL, Menu.NONE, url.replace(Constants.STRING_HTTP, Constants.STRING_EMPTY).replace(Constants.STRING_HTTPS, Constants.STRING_EMPTY));
                            }
                        }
                    }

                    final String eventKey = eventsData.getEventKey(selectedEvent);
                    if (!eventKey.isEmpty()) {
                        if (eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(eventKey)) {

                            menu.add(Menu.NONE, Constants.ContextMenu_UnhideEvent, Menu.NONE, getString(R.string.menu_context_unhide_event))
                                    .setIcon(android.R.drawable.ic_menu_revert);

                        } else {

                            MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_HideEvent, Menu.NONE, getString(R.string.menu_context_hide_event))
                                    .setIcon(R.drawable.ic_menu_block);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
                            }

                        }

                        if (eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(eventKey)) {

                            menu.add(Menu.NONE, Constants.ContextMenu_UnsilentEvent, Menu.NONE, getString(R.string.menu_context_unsilent_event))
                                    .setIcon(android.R.drawable.ic_menu_revert);

                        } else if (!eventsData.checkIsHiddenEvent(eventKey)) {
                            MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_SilentEvent, Menu.NONE, getString(R.string.menu_context_silent_event))
                                    .setIcon(R.drawable.ic_menu_end_conversation);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
                            }

                        }
                    }

                    //https://stackoverflow.com/questions/7042958/android-adding-a-submenu-to-a-menuitem-where-is-addsubmenu
                    SubMenu sub = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.menu_context_remind))
                            .setIcon(android.R.drawable.ic_menu_recent_history);
                    sub.add(Menu.NONE, Constants.ContextMenu_Remind_1H, Menu.NONE, getString(R.string.menu_context_remind_1h));
                    sub.add(Menu.NONE, Constants.ContextMenu_Remind_Morning, Menu.NONE, getString(R.string.menu_context_remind_morning));

                    sub = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.menu_context_share))
                            .setIcon(android.R.drawable.ic_menu_share);
                    sub.add(Menu.NONE, Constants.ContextMenu_ShareAsImage, Menu.NONE, getString(R.string.menu_context_share_as_image));
                    sub.add(Menu.NONE, Constants.ContextMenu_ShareAsText, Menu.NONE, getString(R.string.menu_context_share_as_text));

                    if (selectedEvent[ContactsEvents.Position_eventType].equals(ContactsEvents.getEventType(Constants.Type_Anniversary)) ) {
                        menu.add(Menu.NONE, Constants.ContextMenu_AnniversaryList, Menu.NONE, getString(R.string.menu_context_anniversary_list))
                                .setIcon(android.R.drawable.ic_menu_info_details);
                    }

                    if (selectedEvent[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay))) {
                        MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_xDaysEvent, Menu.NONE, getString(R.string.menu_context_xDaysEvent_add))
                                .setIcon(android.R.drawable.ic_menu_myplaces);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                        }
                    }

                    if (eventsData.preferences_extrafun) {
                        menu.add(Menu.NONE, Constants.ContextMenu_EventInfo, Menu.NONE, getString(R.string.menu_context_event_info))
                                .setIcon(android.R.drawable.ic_menu_view);
                    }

                    popupMenu.setOnMenuItemClickListener(item -> {

                        try {
                            final String eventKey1 = eventsData.getEventKey(selectedEvent);
                            int itemId = item.getItemId();

                            if (itemId == Constants.ContextMenu_EditContact) {

                                Uri selectedContactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, selectedEvent[ContactsEvents.Position_contactID]);
                                Intent editContactIntent = new Intent(Intent.ACTION_EDIT);
                                editContactIntent.setDataAndType(selectedContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                                editContactIntent.putExtra(Constants.EXTRA_CONTACT_ON_SAVE_RESULT, true);
                                try {
                                    startActivity(editContactIntent);
                                } catch (ActivityNotFoundException e) { /**/ }
                                return true;

                            } else if (itemId == Constants.ContextMenu_CreateContact) {

                                Intent createContactIntent = new Intent(Intent.ACTION_INSERT);
                                createContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                                createContactIntent.putExtra(Constants.EXTRA_CONTACT_ON_SAVE_RESULT, true);
                                createContactIntent.putExtra(ContactsContract.Intents.Insert.NAME, selectedEvent[Position_personFullName]);
                                createContactIntent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, selectedEvent[ContactsEvents.Position_title]);
                                createContactIntent.putExtra(ContactsContract.Intents.Insert.COMPANY, selectedEvent[ContactsEvents.Position_organization]);
                                createContactIntent.putExtra(ContactsContract.Intents.Insert.NOTES, selectedEvent[ContactsEvents.Position_eventDateText]);

                                try {
                                    startActivity(createContactIntent);
                                } catch (ActivityNotFoundException e) { /**/ }
                                return true;

                            } else if (itemId == Constants.ContextMenu_EditEvent) {

                                Uri selectedEventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, ContactsEvents.parseToLong(selectedEvent[ContactsEvents.Position_eventID]));
                                Intent editEventIntent = new Intent(Intent.ACTION_VIEW).setData(selectedEventUri);
                                try {
                                    startActivity(editEventIntent);
                                } catch (ActivityNotFoundException e) { /**/ }
                                return true;

                            } else if (itemId == Constants.ContextMenu_OpenURL) {

                                int groupId = item.getGroupId();
                                //String[] eventURLs = selectedEvent[ContactsEvents.Position_eventURL].trim().split(Constants.STRING_2TILDA);
                                if (eventURLs.length >= groupId) {
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(eventURLs[groupId].trim())));
                                    } catch (ActivityNotFoundException e) { /**/ }
                                }

                            } else if (itemId == Constants.ContextMenu_EventInfo) {

                                StringBuilder eventInfo = new StringBuilder();

                                for (int i = 0; i < selectedEvent.length; i++) {
                                    eventInfo.append(i).append(Constants.STRING_COLON_SPACE).append(selectedEvent[i]).append(Constants.STRING_EOL);
                                }

                                String eventSubType = selectedEvent[Position_eventSubType];
                                int roundingFactor;
                                if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent))) {
                                    roundingFactor = 1;
                                } else {
                                    roundingFactor = eventsData.preferences_list_photostyle;
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                                        .setTitle(selectedEvent[Position_personFullName])
                                        .setIcon(new BitmapDrawable(resources, ContactsEvents.getInstance().getContactPhoto(selectedEvent_str, true, false, true, roundingFactor)))
                                        .setMessage(eventInfo.toString())
                                        .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.dismiss());

                                AlertDialog alertToShow = builder.create();
                                alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0)));
                                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);

                                alertToShow.show();

                                TextView textView = alertToShow.findViewById(android.R.id.message);
                                if (textView != null) textView.setTextSize(14);

                                return true;

                            } else if (itemId == Constants.ContextMenu_HideEvent) {

                                if (eventsData.setHiddenEvent(eventKey)) {
                                    if (eventsData.checkIsSilencedEvent(eventKey))
                                        eventsData.unsetSilencedEvent(eventKey); //Если скрываем - убираем из списка без уведомления
                                    this.invalidateOptionsMenu();
                                    prepareList();
                                    drawList();
                                    eventsData.updateWidgets(0);
                                }
                                return true;

                            } else if (itemId == Constants.ContextMenu_UnhideEvent) {

                                if (eventsData.unsetHiddenEvent(eventKey)) {
                                    this.invalidateOptionsMenu();
                                    prepareList();
                                    drawList();
                                    eventsData.updateWidgets(0);
                                }
                                return true;

                            } else if (itemId == Constants.ContextMenu_Remind_1H) {

                                eventsData.snoozeNotification(selectedEvent_str, 1, null);
                                return true;

                            } else if (itemId == Constants.ContextMenu_Remind_Morning) {

                                Calendar now = Calendar.getInstance();
                                now.add(Calendar.DAY_OF_MONTH, 1);
                                now.set(Calendar.HOUR_OF_DAY, 9);
                                now.set(Calendar.MINUTE, 0);
                                now.set(Calendar.SECOND, 0);
                                now.set(Calendar.MILLISECOND, 0);

                                eventsData.snoozeNotification(selectedEvent_str, 0, now.getTime());
                                return true;

                            } else if (itemId == Constants.ContextMenu_AnniversaryList) {

                                eventsData.showAnniversaryList(this);
                                return true;

                            } else if (itemId == Constants.ContextMenu_SilentEvent) {

                                if (eventsData.setSilencedEvent(eventKey)) {
                                    this.invalidateOptionsMenu();
                                    prepareList();
                                    drawList();
                                    eventsData.updateWidgets(0);
                                }
                                return true;

                            } else if (itemId == Constants.ContextMenu_UnsilentEvent) {

                                if (eventsData.unsetSilencedEvent(eventKey)) {
                                    this.invalidateOptionsMenu();
                                    prepareList();
                                    drawList();
                                    eventsData.updateWidgets(0);
                                }
                                return true;

                            } else if (itemId == Constants.ContextMenu_MergeEvent) {

                                //https://developer.android.com/guide/components/intents-common#PickContact
                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                                try {
                                    startActivityForResult(intent, Constants.RESULT_PICK_CONTACT);
                                } catch (ActivityNotFoundException e) { /**/ }

                            } else if (itemId == Constants.ContextMenu_UnmergeEvent) {

                                if (eventsData.setMergedID(selectedEvent[ContactsEvents.Position_eventID], null)) {
                                    this.invalidateOptionsMenu();
                                    prepareList();
                                    drawList();
                                    eventsData.updateWidgets(0);
                                }
                                return true;

                            } else if (itemId == Constants.ContextMenu_RemergeEvent) {

                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                                try {
                                    startActivityForResult(intent, Constants.RESULT_PICK_OTHER_CONTACT);
                                } catch (ActivityNotFoundException e) { /**/ }
                                return true;

                            } else if (itemId == Constants.ContextMenu_ShareAsImage) {

                                //https://stackoverflow.com/questions/12742343/android-get-screenshot-of-all-listview-items
                                //https://demonuts.com/android-take-screenshot/
                                //https://stackoverflow.com/questions/19514174/convert-listview-items-into-a-single-bitmap-image

                                //ListView listView = findViewById(R.id.mainListView);
                                View childView = adapter.getView(selectedEvent_num, null, listView);

                                childView.measure(
                                        View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                                childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
                                childView.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.theme_secondary)));
                                childView.setDrawingCacheEnabled(true);
                                childView.buildDrawingCache(true);
                                Bitmap bmp = childView.getDrawingCache(true);
                                if (bmp == null) {
                                    ToastExpander.showText(this, "Error getting event image");
                                    return false;
                                }

                                Uri bitmapShareUri = null;

                                File file = new File(this.getCacheDir(), "event.jpg");
                                try {
                                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                                    fileOutputStream.close();
                                    bitmapShareUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage(), e);
                                    if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                                }
                                childView.destroyDrawingCache();

                                if (bitmapShareUri != null) {
                                    //https://stackoverflow.com/questions/48045626/chooser-created-with-createchooserintent-title-doesnt-display-a-title
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("image/*");
                                    final String[] mimeTypes = {"image/jpeg", "image/png"};
                                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes); //https://stackoverflow.com/questions/54478638/effect-of-intent-settype-on-androids-intent-chooser
                                    intent.putExtra(Intent.EXTRA_STREAM, bitmapShareUri);
                                    //intent.putExtra(Intent.EXTRA_TITLE, "Share event as image");
                                    Intent chooser = Intent.createChooser(intent, "");
                                    //https://stackoverflow.com/questions/57689792/permission-denial-while-sharing-file-with-fileprovider
                                    List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
                                    for (ResolveInfo resolveInfo : resInfoList) {
                                        String packageName = resolveInfo.activityInfo.packageName;
                                        this.grantUriPermission(packageName, bitmapShareUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    }
                                    try {
                                        startActivity(chooser);
                                        return true;
                                    } catch (ActivityNotFoundException e) { /**/ }
                                }
                                return true;

                            } else if (itemId == Constants.ContextMenu_ShareAsText) {


                                StringBuilder textBig = new StringBuilder();
                                textBig
                                        .append(eventsData.preferences_list_nameformat == 2 ? selectedEvent[Position_personFullNameAlt] : selectedEvent[Position_personFullName])
                                        .append(Constants.STRING_EOL)
                                        .append(selectedEvent[Position_eventEmoji])
                                        .append(Constants.STRING_SPACE)
                                        .append(selectedEvent[Position_eventDate])
                                        .append(Constants.STRING_SPACE)
                                        .append(selectedEvent[Position_eventCaption]);
                                if (!TextUtils.isEmpty(selectedEvent[Position_age_caption].trim()))
                                    textBig
                                            .append(Constants.STRING_COLON_SPACE)
                                            .append(selectedEvent[Position_age_caption]);

                                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Age))) {
                                    String eventSubType = selectedEvent[Position_eventSubType];

                                    if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) { //Если это день рождения или 5K
                                        final String currentAge = selectedEvent[ContactsEvents.Position_age_current];
                                        if (!currentAge.isEmpty() && !currentAge.startsWith(Constants.STRING_0)) {
                                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                                            if (eventsData.set_events_deaths.contains(selectedEvent[ContactsEvents.Position_contactID])) { //Но есть годовщина смерти
                                                textBig.append(getString(R.string.msg_age_could_be));
                                            } else {
                                                textBig.append(getString(R.string.msg_age_now));
                                            }
                                            textBig.append(currentAge);
                                        }
                                    } else if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_Death)) && eventsData.set_events_birthdays.containsKey(selectedEvent[ContactsEvents.Position_contactID])) { //Если это годовщина смерти
                                        Locale locale_en = new Locale(Constants.LANG_EN);
                                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                                        Date eventDate = sdfYear.parse(selectedEvent[ContactsEvents.Position_eventDateText]);
                                        Date birthDate = eventsData.set_events_birthdays.get(selectedEvent[ContactsEvents.Position_contactID]);
                                        if (eventDate != null && birthDate != null) {
                                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                                            textBig.append(getString(R.string.msg_age_was)).append(eventsData.countDaysDiffText(birthDate, eventDate, 3));
                                        }
                                    }
                                }


                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(Intent.EXTRA_TEXT, textBig.toString());
                                //intent.putExtra(Intent.EXTRA_TITLE, "Share event as text");
                                startActivity(Intent.createChooser(intent, ""));
                                return true;

                            } else if (itemId == Constants.ContextMenu_xDaysEvent) {

                                showDialogXDaysCounter(adapter.getView(selectedEvent_num, null, listView), selectedEvent);
                                return true;

                            }

                            return false;
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                            return false;
                        }

                    });

                    popupMenu.show();
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                    return false;
                }
            });

            //registerForContextMenu(listView);

            //Приветственное сообщение или описание новой версии
            showWelcomeScreen();

            //todo: сделать разные иконки приложения https://github.com/guardianproject/CameraV/commit/98d8c545c1901d03d9d238204bb45d502a623e59#diff-7ab4bf3d594a968a90e0250af33fcb9bR399
            //https://stackoverflow.com/questions/1103027/how-to-change-an-application-icon-programmatically-in-android

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void showDialogXDaysCounter(@NonNull View selectedEventView, @NonNull String[] selectedEvent) {
        try {

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setPositiveButton(R.string.button_ok, null)
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel());
                    //.setNeutralButton(R.string.button_reset, null);

            if (eventsData.preferences_theme.themeEditText != 0) {
                builder.getContext().setTheme(eventsData.preferences_theme.themeEditText);
            } else {
                builder.getContext().setTheme(ContactsEvents.themeEditText_default);
            }

            AlertDialog dialog = builder.create();
            View view = View.inflate(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog), R.layout.dialog_xdays, null);
            dialog.setCustomTitle(view);

            //Фон
            LinearLayout entryLayout = view.findViewById(R.id.entryLayout);
            if (entryLayout != null) {
                GradientDrawable drawableBack = new GradientDrawable();
                drawableBack.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics),
                        ta.getColor(R.styleable.Theme_borderCardColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker)));
                drawableBack.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, displayMetrics));
                entryLayout.setBackground(drawableBack);
            }

            //Иконка и заголовок
            ImageView icon = view.findViewById(R.id.icon);
            if (icon != null) icon.setImageBitmap(ContactsEvents.getBitmap(this, android.R.drawable.ic_menu_myplaces));
            TextView title = view.findViewById(R.id.title);
            if (title != null) title.setText(R.string.xDaysCounter_Dialog_Title);

            //Данные события
            ImageView entryEventIcon = view.findViewById(R.id.entryEventIcon);
            ImageView entryEventIconSelected = selectedEventView.findViewById(R.id.entryEventIcon);
            if (entryEventIcon != null) entryEventIcon.setImageDrawable(entryEventIconSelected.getDrawable());

            ImageView entryPhotoImageView = view.findViewById(R.id.entryPhotoImageView);
            ImageView entryPhotoImageViewSelected = selectedEventView.findViewById(R.id.entryPhotoImageView);
            if (entryPhotoImageView != null) entryPhotoImageView.setImageDrawable(entryPhotoImageViewSelected.getDrawable());

            TextView entryNameTextView = view.findViewById(R.id.entryNameTextView);
            TextView entryNameTextViewSelected = selectedEventView.findViewById(R.id.entryNameTextView);
            if (entryNameTextView != null) {
                entryNameTextView.setText(entryNameTextViewSelected.getText());
                entryNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, entryNameTextView.getTextSize() * 4 / 5);
            }

            TextView entryDateTextView = view.findViewById(R.id.entryDateTextView);
            TextView entryDateTextViewSelected = selectedEventView.findViewById(R.id.entryDateTextView);
            if (entryDateTextView != null) {
                entryDateTextView.setText(entryDateTextViewSelected.getText());
                entryDateTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, entryDateTextView.getTextSize() * 4 / 5);
            }

            TextView entryEventDetailsTextView = view.findViewById(R.id.entryEventDetailsTextView);
            TextView entryEventDetailsTextViewSelected = selectedEventView.findViewById(R.id.entryEventDetailsTextView);
            if (entryEventDetailsTextView != null) {
                entryEventDetailsTextView.setText(entryEventDetailsTextViewSelected.getText());
                entryEventDetailsTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, entryEventDetailsTextView.getTextSize() * 4 / 5);
            }

            TextView entryDayDistanceTextView = view.findViewById(R.id.entryDayDistanceTextView);
            if (entryDayDistanceTextView != null) entryDayDistanceTextView.setVisibility(GONE);

            TextView entryDetailsCounter = view.findViewById(R.id.entryDetailsCounter);
            if (entryDetailsCounter != null) entryDetailsCounter.setVisibility(GONE);

            final EditText editText = view.findViewById(R.id.repeats);
            if (editText != null) {
                editText.requestFocus();
                editText.setSingleLine(false);
                if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /**/ }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { /**/ }

                    @Override
                    public void afterTextChanged(Editable s) {
                        final String valueRepeats = editText.getText().toString().trim();
                        ((TextView) view.findViewById(R.id.listNextEvents)).setText(String.valueOf(System.currentTimeMillis()));
                    }
                });
            }

            dialog.setOnShowListener(arg0 -> {
                final Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(v -> {
                    final String valueRepeats = editText != null ? editText.getText().toString().trim() : Constants.STRING_EMPTY;
                    /*eventsData.savePreferences();*/
                    dialog.dismiss();
                });
                buttonPositive.setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                final Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(v -> {
                    dialog.dismiss();
                });
                buttonNegative.setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                final View buttonBar = (View) buttonPositive.getParent();
                buttonBar.setBackgroundColor(ta.getColor(R.styleable.Theme_editTextBackgroundCustom, 0));

            });
            dialog.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @SuppressLint("RestrictedApi") //https://stackoverflow.com/questions/48607853/menubuilder-setoptionaliconsvisible-can-only-be-called-from-within-the-same-libr
    private void setMenuIconsVisible(Menu menu) {
        try {
            if (menu instanceof MenuBuilder) {
                ((MenuBuilder) menu).setOptionalIconsVisible(true);
            }
        } catch (Exception e) { /**/ }
    }

    private void showAlertNoEvents() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
            builder.setTitle(getString(R.string.msg_no_events));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setMessage(getString(R.string.msg_no_events_hint));
            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
            /*builder.setNeutralButton(R.string.button_open_addressbook, (dialog, which) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI));
                } catch (android.content.ActivityNotFoundException e) { *//**//* }
            });*/
            builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (android.content.ActivityNotFoundException e) { /**/ }
            });
            AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
            });
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void showAlertNoEventsWithAccounts() {

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
            builder.setTitle(getString(R.string.msg_no_events));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setMessage(getString(R.string.msg_no_events_check_prefs, getString(R.string.pref_Accounts_title), getString(R.string.pref_CustomEvents_title)));
            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
            builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (android.content.ActivityNotFoundException e) { /**/ }
            });
            AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
            });
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void showWelcomeScreen() {

        try {
            AlertDialog.Builder builder;
            AlertDialog alertToShow;

            switch (checkNewVersion()) {
                case +1: //при запуске новой версии показывать what's new

                    StringBuilder sb = new StringBuilder();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) sb.append(Constants.HTML_BR);

                    String[] arrChangeLog = resources.getStringArray(R.array.changelog);
                    if (!arrChangeLog[0].isEmpty()) {

                        String currentVersion = Constants.STRING_EMPTY;
                        int countChanges = 0;

                        for (String strChange : arrChangeLog) {

                            if (strChange.charAt(0) == '#') {

                                if (!currentVersion.isEmpty()) break;
                                currentVersion = strChange.substring(1);
                                if (!currentVersion.equals(BuildConfig.VERSION_NAME)) break;
                                sb.append(Constants.HTML_UL_START);

                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    sb.append(Constants.HTML_LI).append(strChange.replace(Constants.STRING_EOL, Constants.HTML_BR)).append(Constants.HTML_LI_END);
                                } else {
                                    sb.append(Constants.HTML_BR).append(Constants.HTML_LI_API21).append(strChange.replace(Constants.STRING_EOL, Constants.HTML_BR));
                                }
                                countChanges++;

                            }
                        }
                        if (countChanges > 0) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                sb.append(Constants.HTML_UL_END);
                            } else {
                                sb.append(Constants.HTML_BR);
                            }

                            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                            builder.setTitle(getString(R.string.msg_new_version_title, currentVersion));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setMessage(HtmlCompat.fromHtml(sb.toString(), 0));
                            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                            builder.setNeutralButton(R.string.button_open_version_history, (dialog, which) -> {
                                try {
                                    startActivity(new Intent(this, AboutActivity.class));
                                } catch (android.content.ActivityNotFoundException e) { /**/ }
                            });
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

                    if (eventsData.checkNoContactsAccess()) {
                        //https://developer.android.com/training/permissions/requesting.html#java
                        swipeRefresh.setEnabled(false);
                        swipeRefresh.setRefreshing(false);
                        setHint(eventsData.setHTMLColor(getString(R.string.msg_no_access_contacts).toLowerCase(), Constants.HTML_COLOR_RED));
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.GET_ACCOUNTS}, Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                    }

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.msg_welcome_title));
                    builder.setIcon(android.R.drawable.ic_menu_info_details);
                    builder.setCancelable(false);
                    builder.setMessage(getString(R.string.msg_welcome_text));
                    builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                    builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                        try {
                            startActivity(new Intent(this, SettingsActivity.class));
                        } catch (android.content.ActivityNotFoundException e) { /**/ }
                    });

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
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setLastRunVersion() {

        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getString(R.string.pref_Version_LastRun), BuildConfig.VERSION_NAME);
            editor.putString(getString(R.string.pref_VersionCode_LastRun), Integer.toString(BuildConfig.VERSION_CODE));
            editor.apply();

            if (eventsData.preferences_debug_on)
                ToastExpander.showText(this, "Set last run version: " + BuildConfig.VERSION_NAME);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on)
                ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private int checkNewVersion() {
        // +1 - новая версия, 0 - первый запуск, -1 - такую версию уже запускали

        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final String pref_LastRunVersion = preferences.getString(getString(R.string.pref_VersionCode_LastRun), Constants.STRING_EMPTY);
            if (Constants.STRING_EMPTY.equals(pref_LastRunVersion)) {
                return 0;
            } else if (Integer.toString(BuildConfig.VERSION_CODE).equals(pref_LastRunVersion)) {
                return -1;
            } else {
                return +1;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return -1;
        }
    }

    private void initNotifications() {
        //https://stackoverflow.com/questions/51343550/how-to-give-notifications-on-android-on-specific-time-in-android-oreo/51645875#51645875

        try {
            StringBuilder log = new StringBuilder();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                log.append(getString(R.string.msg_notifications_disabled));
            } else {
                eventsData.initNotificationChannel(log); //для Android 8+
                eventsData.initBootReceiver(log);
                eventsData.initNotifications(log);
            }
            eventsData.initWidgetUpdate(log);

            if (eventsData.preferences_debug_on && log.length() > 0)
                ToastExpander.showText(this, log.deleteCharAt(log.length() - 1).toString());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void onRefresh() {
        if (swipeRefreshListener == null) return;
        try {
            swipeRefreshListener.onRefresh();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onStart() {

        try {
            super.onStart();
            if (eventsData != null) {
                eventsData.isUIopen = true;
                eventsData.coordinator = this.findViewById(R.id.coordinator);
            }

            Intent intent = getIntent();
            if (intent.getAction() != null && intent.getData() != null && intent.getAction() != null) {

                final String eventInfo = intent.getData().toString();

                if (getString(R.string.content_provider_intent_view).equals(intent.getAction())) {

                    final String eventNum = eventInfo.substring(0, eventInfo.indexOf(Constants.STRING_EOT));
                    final String eventTitle = eventInfo.substring(eventInfo.indexOf(Constants.STRING_EOT));

                    AtomicInteger index = new AtomicInteger(-1);
                    try {
                        index.set(Integer.parseInt(eventNum));
                    } catch (NumberFormatException e) { /**/}

                    if (index.get() > -1) {
                        new Handler().postDelayed(() -> {
                            ListView listView = findViewById(R.id.mainListView);
                            int jumpToEvent = index.get() + eventsData.statEventsPrevEventsFound;

                            //Находим в списке (бежим к началу)
                            ListAdapter adapter = listView.getAdapter();
                            if (adapter != null && adapter.getCount() > 0) {
                                boolean isFound = false;
                                if (index.get() >= adapter.getCount()) index.set(adapter.getCount() - 1);
                                while (index.get() > 0) {
                                    if (((String)adapter.getItem(index.get())).contains(eventTitle)) {
                                        jumpToEvent = index.get();
                                        isFound = true;
                                        break;
                                    }
                                    index.getAndDecrement();
                                }
                                if (!isFound) {
                                    ToastExpander.showText(this, getString(R.string.msg_jump_to_event_error));
                                    return;
                                }
                            }

                            listView.setSelectionFromTop(jumpToEvent, listView.getTop() + listView.getPaddingTop());
                        }, 200); //https://stackoverflow.com/questions/36426129/recyclerview-scroll-to-position-not-working-every-time
                    }
                }
                intent.setAction(Constants.STRING_EMPTY);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onPause() {
        if (eventsData != null) eventsData.statLastPausedForOtherActivity = System.currentTimeMillis();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
        if (eventsData != null) {
            eventsData.isUIopen = false;
            eventsData.coordinator = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        if (ta != null) ta.recycle();
        super.onDestroy();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        try {

            //https://stackoverflow.com/a/43411336/4928833
            if (menu instanceof MenuBuilder) {
                ((MenuBuilder) menu).setOptionalIconsVisible(true);
            }
            try {
                getMenuInflater().inflate(R.menu.menu_main, menu);
            } catch (InflateException e) { /**/ }

            MenuItem searchItem = menu.getItem(Constants.MENU_MAIN_SEARCH);
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchItem.setVisible(!eventsData.isEmptyEventList());
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.getFilter().filter(newText);
                    return false;
                }
            });
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener()
            {

                @Override
                public boolean onMenuItemActionExpand(MenuItem item){
                    menu.getItem(Constants.MENU_MAIN_ADD_EVENT).setVisible(false);
                    menu.getItem(Constants.MENU_MAIN_REFRESH).setVisible(false);
                    menu.getItem(Constants.MENU_MAIN_SETTINGS).setVisible(false);
                    menu.getItem(Constants.MENU_MAIN_QUIZ).setVisible(false);
                    menu.getItem(Constants.MENU_MAIN_FILTER).setVisible(false);
                    menu.getItem(Constants.MENU_MAIN_HINTS).setVisible(true);
                    return true;
                }

                //работает, только если showAsAction="always" https://stackoverflow.com/questions/9327826/searchviews-oncloselistener-doesnt-work/18186164
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item){
                    menu.getItem(Constants.MENU_MAIN_ADD_EVENT).setVisible(true);
                    menu.getItem(Constants.MENU_MAIN_REFRESH).setVisible(true);
                    menu.getItem(Constants.MENU_MAIN_SETTINGS).setVisible(true);
                    menu.getItem(Constants.MENU_MAIN_QUIZ).setVisible(true);
                    //показывать, если есть скрытые или без уведомлений
                    menu.getItem(Constants.MENU_MAIN_FILTER).setVisible(
                            eventsData != null &&
                                    !eventsData.isEmptyEventList() &&
                                    (eventsData.getHiddenEventsCount() > 0 || eventsData.getSilencedEventsCount() > 0)
                    );
                    menu.getItem(Constants.MENU_MAIN_HINTS).setVisible(false);
                    prepareList();
                    return true;
                }

            });
            searchView.setQueryHint(getString (R.string.msg_hint_search));
            searchView.setMaxWidth(Integer.MAX_VALUE);

            menu.findItem(R.id.menu_open_file_with_events).setVisible(
                    !eventsData.preferences_Birthday_files.isEmpty()
                            || !eventsData.preferences_OtherEvent_files.isEmpty()
                            || !eventsData.preferences_MultiType_files.isEmpty());

            //https://stackoverflow.com/questions/17845980/how-to-implement-voice-search-to-searchview
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

            // https://stackoverflow.com/questions/3721963/how-to-add-calendar-events-in-android
            // https://developer.android.com/training/contacts-provider/modify-data
            // https://stackoverflow.com/questions/54475665/how-to-insert-contact-birthday-date-by-intent
            // https://stackoverflow.com/questions/20890855/adding-a-contactscontract-commondatakinds-event-to-android-contacts-does-not-sh

            menu.getItem(Constants.MENU_MAIN_QUIZ).setVisible(!this.dataList.isEmpty());

            //показывать, если есть скрытые или без уведомлений
            menu.getItem(Constants.MENU_MAIN_FILTER).setVisible(
                    !eventsData.isEmptyEventList() &&
                            (eventsData.getHiddenEventsCount() > 0 || eventsData.getSilencedEventsCount() > 0)
            );
            menu.getItem(Constants.MENU_MAIN_HINTS).setVisible(false);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        try {
            AlertDialog.Builder builder;
            AlertDialog alertToShow;

            int itemId = item.getItemId();
            if (itemId == R.id.menu_refresh) {

                //https://github.com/googlesamples/android-SwipeRefreshLayoutBasic/blob/master/Application/src/main/java/com/example/android/swiperefreshlayoutbasic/SwipeRefreshLayoutBasicFragment.java
                //https://medium.com/@elye.project/swipe-to-refresh-not-showing-why-96b76c5c93e7
                if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {
                    swipeRefresh.post(() -> updateList(true, eventsData.statTimeComputeDates >= Constants.TIME_SPEED_LOAD_OVERTIME));
                }
                return true;

            } else if (itemId == R.id.menu_settings) {

                Intent intent = new Intent(this, SettingsActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == R.id.menu_quiz) {

                Intent intent = new Intent(this, QuizActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == R.id.menu_filter_events) {

                List<String> filterVariants = new ArrayList<String>() {{
                    add(getString(R.string.events_scope_not_hidden, statsAllEvents - statsHiddenEvents));
                    add(getString(R.string.events_scope_all, statsAllEvents));
                }};

                List<Integer> filterValues = new ArrayList<Integer>() {{
                    add(Constants.pref_Events_Scope_NotHidden);
                    add(Constants.pref_Events_Scope_All);
                }};

                boolean isDeadLinks = false;
                if (eventsData.getHiddenEventsCount() > 0) {
                    if (statsHiddenEvents != eventsData.getHiddenEventsCount() && eventsData.preferences_debug_on) {
                        filterVariants.add(getString(R.string.events_scope_hidden2, statsHiddenEvents, eventsData.getHiddenEventsCount()));
                        isDeadLinks = true;
                    } else
                        filterVariants.add(getString(R.string.events_scope_hidden, statsHiddenEvents));
                    filterValues.add(Constants.pref_Events_Scope_Hidden);
                }
                if (eventsData.getSilencedEventsCount() > 0) {
                    if (statsSilencedEvents != eventsData.getSilencedEventsCount() && eventsData.preferences_debug_on) {
                        filterVariants.add(getString(R.string.events_scope_silenced2, statsSilencedEvents, eventsData.getSilencedEventsCount()));
                        isDeadLinks = true;
                    } else
                        filterVariants.add(getString(R.string.events_scope_silenced, statsSilencedEvents));
                    filterValues.add(Constants.pref_Events_Scope_Silenced);
                }

                if (eventsData.preferences_debug_on && (eventsData.getHiddenEventsCount() > 0 || eventsData.getSilencedEventsCount() > 0)) {
                    filterVariants.add(getString(R.string.events_scope_clear));
                    filterValues.add(Constants.pref_Events_Scope_Clear);
                }
                if (eventsData.preferences_debug_on && isDeadLinks) {
                    filterVariants.add(getString(R.string.events_scope_clean));
                    filterValues.add(Constants.pref_Events_Scope_Clean);
                }

                builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.activity_title_events_scope)
                        .setIcon(android.R.drawable.ic_menu_sort_by_size)
                        .setSingleChoiceItems(filterVariants.toArray(new CharSequence[0]), eventsData.preferences_list_events_scope, (dialog, which) -> {
                            final int choice = filterValues.get(((AlertDialog) dialog).getListView().getCheckedItemPosition());
                            if (choice == Constants.pref_Events_Scope_Clear) {

                                AlertDialog.Builder confirm = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                                        .setTitle(R.string.msg_title_confirmation)
                                        .setMessage(R.string.msg_filter_clear_confirmation)
                                        .setIcon(android.R.drawable.ic_menu_help)
                                        .setNegativeButton(R.string.button_cancel, (confirm_dialog, confirm_which) -> dialog.cancel())
                                        .setPositiveButton(R.string.button_ok, (confirm_dialog, confirm_which) -> {
                                            eventsData.clearHiddenEvents();
                                            eventsData.clearSilencedEvents();
                                            eventsData.preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                                            eventsData.savePreferences();
                                            this.invalidateOptionsMenu();
                                            prepareList();
                                            drawList();
                                        });

                                AlertDialog confirm_dialog = confirm.create();

                                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                                confirm_dialog.setOnShowListener(arg0 -> {
                                    confirm_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                    confirm_dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                });

                                confirm_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                confirm_dialog.show();

                            } else if (choice == Constants.pref_Events_Scope_Clean) {

                                AlertDialog.Builder confirm = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                                        .setTitle(R.string.msg_title_confirmation)
                                        .setMessage(R.string.msg_filter_clean_confirmation)
                                        .setIcon(android.R.drawable.ic_menu_help)
                                        .setNegativeButton(R.string.button_cancel, (confirm_dialog, confirm_which) -> dialog.cancel())
                                        .setPositiveButton(R.string.button_ok, (confirm_dialog, confirm_which) -> {
                                            eventsData.clearDeadlinkHiddenEvents();
                                            eventsData.clearDeadlinkSilencedEvents();
                                            eventsData.preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                                            eventsData.savePreferences();
                                            this.invalidateOptionsMenu();
                                            prepareList();
                                            drawList();
                                        });

                                AlertDialog confirm_dialog = confirm.create();

                                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                                confirm_dialog.setOnShowListener(arg0 -> {
                                    confirm_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                    confirm_dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                });

                                confirm_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                confirm_dialog.show();

                            } else {

                                eventsData.preferences_list_events_scope = choice;

                            }
                            eventsData.savePreferences();
                            dialog.cancel();
                            prepareList();
                            drawList();
                        })
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                        .setCancelable(true);

                alertToShow = builder.create();

                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                    ListView listView = alertToShow.getListView();
                    listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)));
                    listView.setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics));
                    /*listView.setPadding(
                            0, //(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics),
                            0,
                            0, //(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics),
                            0
                    );*/
                });

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();

                //Ширина диалога
                /*Rect displayRectangle = new Rect();
                Window window = this.getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
                alertToShow.getWindow().setLayout((int) (displayRectangle.width() * 0.9f), alertToShow.getWindow().getAttributes().height);*/

                return true;

            } else if (itemId == R.id.menu_add_event_to_contact) {

                Intent editIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                editIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                editIntent.putExtra(Constants.EXTRA_CONTACT_ON_SAVE_RESULT, true);
                try {
                    startActivity(editIntent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == R.id.menu_add_event_to_calendar) {

                // https://developer.android.com/guide/topics/providers/calendar-provider#java
                // https://stackoverflow.com/questions/20563476/how-to-add-a-calendar-event-using-intents
                // https://github.com/roomorama/Caldroid/issues/128

                Intent addEventIntent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.ALL_DAY, true)
                        .putExtra(CalendarContract.Events.RRULE, "FREQ=YEARLY");
                try {
                    startActivity(addEventIntent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == R.id.menu_open_file_with_events) {

                //https://www.androidsnippets.com/open-any-type-of-file-with-default-intent.html
                //https://www.androidsnippets.com/open-file-with-default-application-using-intents.html

                List<String> fileNames = new ArrayList<>();
                List<String> fileURIs = new ArrayList<>();
                for (String file: eventsData.preferences_Birthday_files) {
                    String[] fileDetails = file.split(Constants.STRING_PIPE);
                    if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                        fileNames.add(fileDetails[0]);
                        fileURIs.add(fileDetails[1]);
                    }
                }
                for (String file: eventsData.preferences_OtherEvent_files) {
                    String[] fileDetails = file.split(Constants.STRING_PIPE);
                    if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                        fileNames.add(fileDetails[0]);
                        fileURIs.add(fileDetails[1]);
                    }
                }
                for (String file: eventsData.preferences_MultiType_files) {
                    String[] fileDetails = file.split(Constants.STRING_PIPE);
                    if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                        fileNames.add(fileDetails[0]);
                        fileURIs.add(fileDetails[1]);
                    }
                }
                if (fileURIs.isEmpty()) return true;

                builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(R.string.pref_CustomEvents_LocalFiles_title)
                        .setIcon(android.R.drawable.ic_menu_save)
                        .setItems(fileNames.toArray(new CharSequence[0]), (dialog, which) -> {
                            Uri uri = Uri.parse(fileURIs.get(which));
                            if (uri != null) {
                                try {
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_EDIT);
                                    intent.setDataAndType(uri, "text/plain");
                                    //https://stackoverflow.com/questions/24604346/issue-opening-document-using-flag-grant-write-uri-permission-intent-android
                                    final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
                                    intent.addFlags(flags);
                                    dialog.cancel();
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        for (ResolveInfo resolveInfo : getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL)) {
                                            String packageName = resolveInfo.activityInfo.packageName;
                                            grantUriPermission(packageName, uri, flags);
                                        }
                                    }
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) { /**/ }
                                } catch (SecurityException se) {
                                    ToastExpander.showText(this, getText(R.string.msg_file_access_error).toString());
                                }
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                        .setCancelable(true);

                alertToShow = builder.create();

                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                    ListView listView = alertToShow.getListView();
                    listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)));
                    listView.setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics));
                    listView.setPadding(
                            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics),
                            0,
                            (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics),
                            0
                    );
                });

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();

            } else if (itemId == R.id.menu_hints) {

                StringBuilder sb = new StringBuilder();
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) sb.append(HTML_BR);

                String[] arrFAQ = resources.getStringArray(R.array.faq);
                if (!arrFAQ[0].isEmpty()) {

                    int countHintLines = 0;
                    String headerStart = "#".concat(getString(R.string.menu_search));

                    for (String strLine : arrFAQ) {

                        if (countHintLines == 0 && !strLine.equals(headerStart)) continue;
                        if (strLine.trim().isEmpty()) {
                            break;
                        } else if (strLine.equals(headerStart)) {
                            countHintLines++;
                        } else {
                            sb.append(Constants.HTML_BR);
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                sb.append(strLine.replaceAll(Constants.HTML_LI, "<br>&nbsp;-&nbsp;"));
                            } else {
                                sb.append(strLine);
                            }
                            countHintLines++;
                        }
                    }

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.window_search_hints));
                    builder.setIcon(android.R.drawable.ic_menu_info_details);
                    builder.setMessage(HtmlCompat.fromHtml(sb.toString(), 0));
                    builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                    alertToShow = builder.create();
                    alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));
                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertToShow.show();
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {
            if ((requestCode == Constants.RESULT_PICK_CONTACT || requestCode == Constants.RESULT_PICK_OTHER_CONTACT) && resultCode == RESULT_OK) {

                Uri contactUri = data.getData();
                if (contactUri != null) {
                    String contactID = contactUri.toString().substring(contactUri.toString().lastIndexOf("/") + 1);
                    if (!contactID.isEmpty() && !selectedEvent[ContactsEvents.Position_eventID].isEmpty()) {
                        if (eventsData.setMergedID(selectedEvent[ContactsEvents.Position_eventID], contactID)) {
                            prepareList();
                            updateList(true, true);
                        }
                    }
                }

            } else {

                super.onActivityResult(requestCode, resultCode, data);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @Override
    protected void onResume() {

        try {
            super.onResume();
            if (!filterNames.isEmpty()) return; //чтобы параметра поиска не сбрасывал после просмотра контакта

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(this);
            if (eventsData.statLastPausedForOtherActivity > 0 && !this.dataList.isEmpty()
                    && System.currentTimeMillis() - eventsData.statLastPausedForOtherActivity < Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) return; //если "выходили" посмотреть карточку контакта или события на 5 сек

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

            //Общий заголовок
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (eventsData.preferences_list_custom_caption.isEmpty()) {
                toolbar.setTitle(R.string.app_name);
            } else {
                toolbar.setTitle(eventsData.preferences_list_custom_caption);
            }

            //Разделитель списка зависит от стиля отображения
            //Тут повтор из onCreate, потому что иногда при смене настроек изменения не подхватываются
            ListView listView = findViewById(R.id.mainListView);
            if (eventsData.preferences_list_style == Integer.parseInt(getString(R.string.pref_List_Style_Card))) {

                listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_backgroundColor, 0)));
                listView.setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics));
                int listPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics);
                listView.setPadding(listPadding, listPadding, listPadding, listPadding);

            } else {

                listView.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, 0)));
                listView.setDividerHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics));
                listView.setPadding(0, 0, 0, 0);

            }

            if (eventsData.preference_list_fastscroll) {

                //http://androidopentutorials.com/android-listview-fastscroll/
                //https://stackoverflow.com/questions/33619453/scrollbar-touch-area-in-android-6
                //https://stackoverflow.com/questions/6883785/android-sectionindexer-tutorial
                //todo: разобраться. почему из-за FastScroll иногда падает приложение

                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    listView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                        @Override
                        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                            //включаем fast scroll, только когда скроллят
                            if (!listView.isFastScrollEnabled()) {
                                listView.setFastScrollEnabled(true);
                                listView.postDelayed(() -> listView.setFastScrollEnabled(false), 4000);
                            }
                        }
                    });
                }*/

                listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                    int mCurrentState = 0;

                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int state) {

                        if (state == SCROLL_STATE_IDLE && mCurrentState != state && listView.isFastScrollEnabled()) {
                            listView.postDelayed(() -> listView.setFastScrollEnabled(false), 3000);
                        }

                        mCurrentState = state;
                    }

                    @Override
                    public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                        if (mCurrentState == SCROLL_STATE_TOUCH_SCROLL) {
                            if (!listView.isFastScrollEnabled())
                                listView.setFastScrollEnabled(true);
                        }
                    }
                });

            } else {

                listView.setOnScrollListener(null);
                listView.setFastScrollEnabled(false);

            }

            //Отступы списка событий
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) swipeRefresh.getLayoutParams();
            marginParams.setMargins(
                    (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                    marginParams.topMargin,
                    (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                    marginParams.bottomMargin);
            swipeRefresh.setLayoutParams(marginParams);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    && eventsData.preferences_notifications_days.size() > 0) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, Constants.MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
            }

            //this.invalidateOptionsMenu();

            //Тему не меняли, просто обновляем данные
            if (this.dataList.isEmpty() || eventsData.needUpdateEventList || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {

                updateList(true, !eventsData.isUIopen || eventsData.statTimeComputeDates >= Constants.TIME_SPEED_LOAD_OVERTIME);

                //Уведомления
                initNotifications();

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);



            if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR) {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerForContextMenu(findViewById(R.id.mainListView));
                    updateList(true, eventsData.statTimeComputeDates >= Constants.TIME_SPEED_LOAD_OVERTIME);
                    showWelcomeScreen();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        && eventsData.preferences_notifications_days.size() > 0) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, Constants.MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
                }

            } else if (requestCode == Constants.MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initNotifications();
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
/*
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        //todo: добавить стиль меню https://stackoverflow.com/questions/4604562/override-context-menu-colors-in-android
        //todo: добавить иконки https://stackoverflow.com/questions/1245543/add-context-menu-icon-in-android
        //todo: подсказки про именины на основе имени и даты рождения
        //todo: знаки зодиака и года
        //todo: ссылки с имени и фамилии на web справочник


        try {
            if (v == null || v.getId() != R.id.mainListView) return;

            ListView l = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            selectedEvent_num = acmi.position;
            selectedEvent_str = (String)l.getItemAtPosition(acmi.position);
            selectedEvent = selectedEvent_str.split(Constants.STRING_EOT, -1);

            //https://stackoverflow.com/questions/18632331/using-contextmenu-with-listview-in-android
            //menu.setHeaderTitle(dataArray1[ContactsEvents.dataMap.get("fio")] + ":");

            String contactID = selectedEvent[ContactsEvents.Position_contactID];
            boolean isRealContactID = contactID != null && !contactID.isEmpty() && ContactsEvents.isRealContactEventID(contactID);
            if (isRealContactID) {
                menu.add(Menu.NONE, Constants.ContextMenu_EditContact, Menu.NONE, getString(R.string.menu_context_edit_contact)).setIcon(android.R.drawable.ic_menu_edit);
            } else {
                menu.add(Menu.NONE, Constants.ContextMenu_CreateContact, Menu.NONE, getString(R.string.menu_context_create_contact)).setIcon(android.R.drawable.ic_menu_add);
            }

            if (!selectedEvent[ContactsEvents.Position_eventID].isEmpty()) { //(selectedEvent[Position_eventStorage].equals(STRING_STORAGE_CALENDAR)) {
                menu.add(Menu.NONE, Constants.ContextMenu_EditEvent, Menu.NONE, getString(R.string.menu_context_edit_event)).setIcon(android.R.drawable.ic_menu_month);

                if (selectedEvent[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay))) {
                    if (!eventsData.getMergedID(selectedEvent[ContactsEvents.Position_eventID]).isEmpty()) {
                        menu.add(Menu.NONE, Constants.ContextMenu_UnmergeEvent, Menu.NONE, getString(R.string.menu_context_unmerge_event)).setIcon(android.R.drawable.ic_menu_revert);
                        menu.add(Menu.NONE, Constants.ContextMenu_RemergeEvent, Menu.NONE, getString(R.string.menu_context_remerge_event)).setIcon(android.R.drawable.ic_menu_set_as);
                    } else if (selectedEvent[ContactsEvents.Position_eventStorage].equals(Constants.STRING_STORAGE_CALENDAR) && selectedEvent[ContactsEvents.Position_contactID].isEmpty()) {
                        menu.add(Menu.NONE, Constants.ContextMenu_MergeEvent, Menu.NONE, getString(R.string.menu_context_merge_event)).setIcon(android.R.drawable.ic_menu_set_as);
                    }
                }
            }

            String[] eventURLs = selectedEvent[ContactsEvents.Position_eventURL].trim().split(Constants.STRING_2TILDA);
            if (!eventURLs[0].isEmpty()) {
                if (eventURLs.length == 1) {
                    menu.add(Menu.NONE, Constants.ContextMenu_OpenURL, Menu.NONE, getString(R.string.menu_context_open_url)).setIcon(android.R.drawable.ic_menu_set_as);
                } else {
                    SubMenu sub = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.menu_context_open_url)).setIcon(android.R.drawable.ic_menu_set_as);
                    for (int i = 0, eventURLsLength = eventURLs.length; i < eventURLsLength; i++) {
                        String url = eventURLs[i];
                        sub.add(i, Constants.ContextMenu_OpenURL, Menu.NONE, url.replace(Constants.STRING_HTTP, Constants.STRING_EMPTY).replace(Constants.STRING_HTTPS, Constants.STRING_EMPTY));
                    }
                }
            }

            final String eventKey = eventsData.getEventKey(selectedEvent);
            if (!eventKey.isEmpty()) {
                if (eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(eventKey)) {

                    menu.add(Menu.NONE, Constants.ContextMenu_UnhideEvent, Menu.NONE, getString(R.string.menu_context_unhide_event)).setIcon(android.R.drawable.ic_menu_revert);

                } else {

                    menu.add(Menu.NONE, Constants.ContextMenu_HideEvent, Menu.NONE, getString(R.string.menu_context_hide_event)).setIcon(R.drawable.ic_menu_clear_playlist);

                }

                if (eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(eventKey)) {

                    menu.add(Menu.NONE, Constants.ContextMenu_UnsilentEvent, Menu.NONE, getString(R.string.menu_context_unsilent_event)).setIcon(R.drawable.ic_menu_notifications);

                } else if (!eventsData.checkIsHiddenEvent(eventKey)) {

                    menu.add(Menu.NONE, Constants.ContextMenu_SilentEvent, Menu.NONE, getString(R.string.menu_context_silent_event)).setIcon(R.drawable.ic_menu_block);

                }
            }

            //https://stackoverflow.com/questions/7042958/android-adding-a-submenu-to-a-menuitem-where-is-addsubmenu
            SubMenu sub = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.menu_context_remind)).setIcon(android.R.drawable.ic_menu_recent_history);
            sub.add(Menu.NONE, Constants.ContextMenu_Remind_1H, Menu.NONE, getString(R.string.menu_context_remind_1h));
            sub.add(Menu.NONE, Constants.ContextMenu_Remind_Morning, Menu.NONE, getString(R.string.menu_context_remind_morning));

            sub = menu.addSubMenu(Menu.NONE, Menu.NONE, Menu.NONE, getString(R.string.menu_context_share)).setIcon(android.R.drawable.ic_menu_share);
            sub.add(Menu.NONE, Constants.ContextMenu_ShareAsImage, Menu.NONE, getString(R.string.menu_context_share_as_image));
            sub.add(Menu.NONE, Constants.ContextMenu_ShareAsText, Menu.NONE, getString(R.string.menu_context_share_as_text));

            if (selectedEvent[ContactsEvents.Position_eventType].equals(ContactsEvents.getEventType(Constants.Type_Anniversary)) ) {
                menu.add(Menu.NONE, Constants.ContextMenu_AnniversaryList, Menu.NONE, getString(R.string.menu_context_anniversary_list)).setIcon(android.R.drawable.ic_menu_info_details);
            }

            if (selectedEvent[Position_eventSubType].equals(ContactsEvents.getEventType(Constants.Type_BirthDay))) {
                menu.add(Menu.NONE, Constants.ContextMenu_xDaysEvent, Menu.NONE, "Добавить счётчик…");//getString(R.string.menu_context_xDaysEvent_add));
            }

            if (eventsData.preferences_extrafun) {
                menu.add(Menu.NONE, Constants.ContextMenu_EventInfo, Menu.NONE, getString(R.string.menu_context_event_info)).setIcon(android.R.drawable.ic_menu_view);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }*/
/*
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        try {
            final String eventKey = eventsData.getEventKey(selectedEvent);
            int itemId = item.getItemId();

            if (itemId == Constants.ContextMenu_EditContact) {

                Uri selectedContactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, selectedEvent[ContactsEvents.Position_contactID]);
                Intent editContactIntent = new Intent(Intent.ACTION_EDIT);
                editContactIntent.setDataAndType(selectedContactUri, ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                editContactIntent.putExtra("finishActivityOnSaveCompleted", true);
                try {
                    startActivity(editContactIntent);
                } catch (android.content.ActivityNotFoundException e) { *//**//* }
                return true;

            } else if (itemId == Constants.ContextMenu_CreateContact) {

                Intent createContactIntent = new Intent(Intent.ACTION_INSERT);
                createContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                createContactIntent.putExtra("finishActivityOnSaveCompleted", true);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.NAME, selectedEvent[Position_personFullName]);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, selectedEvent[ContactsEvents.Position_title]);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.COMPANY, selectedEvent[ContactsEvents.Position_organization]);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.NOTES, selectedEvent[ContactsEvents.Position_eventDateText]);

                try {
                    startActivity(createContactIntent);
                } catch (android.content.ActivityNotFoundException e) { *//**//* }
                return true;

            } else if (itemId == Constants.ContextMenu_EditEvent) {

                Uri selectedEventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, ContactsEvents.parseToLong(selectedEvent[ContactsEvents.Position_eventID]));
                Intent editEventIntent = new Intent(Intent.ACTION_VIEW).setData(selectedEventUri);
                try {
                    startActivity(editEventIntent);
                } catch (android.content.ActivityNotFoundException e) { *//**//* }
                return true;

            } else if (itemId == Constants.ContextMenu_OpenURL) {

                int groupId = item.getGroupId();
                String[] eventURLs = selectedEvent[ContactsEvents.Position_eventURL].trim().split(Constants.STRING_2TILDA);
                if (eventURLs.length >= groupId) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(eventURLs[groupId].trim())));
                    } catch (android.content.ActivityNotFoundException e) { *//**//* }
                }

            } else if (itemId == Constants.ContextMenu_EventInfo) {

                StringBuilder eventInfo = new StringBuilder();

                for (int i = 0; i < selectedEvent.length; i++) {
                    eventInfo.append(i).append(Constants.STRING_COLON_SPACE).append(selectedEvent[i]).append(Constants.STRING_EOL);
                }

                String eventSubType = selectedEvent[Position_eventSubType];
                int roundingFactor;
                if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent))) {
                    roundingFactor = 1;
                } else {
                    roundingFactor = eventsData.preferences_list_photostyle;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                        .setTitle(selectedEvent[Position_personFullName])
                        .setIcon(new BitmapDrawable(resources, ContactsEvents.getInstance().getContactPhoto(selectedEvent_str, true, false, true, roundingFactor)))
                        .setMessage(eventInfo.toString())
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.dismiss());

                AlertDialog alertToShow = builder.create();
                alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0)));
                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);

                alertToShow.show();

                TextView textView = alertToShow.findViewById(android.R.id.message);
                if (textView != null) textView.setTextSize(14);

                return true;

            } else if (itemId == Constants.ContextMenu_HideEvent) {

                if (eventsData.setHiddenEvent(eventKey)) {
                    if (eventsData.checkIsSilencedEvent(eventKey))
                        eventsData.unsetSilencedEvent(eventKey); //Если скрываем - убираем из списка без уведомления
                    this.invalidateOptionsMenu();
                    prepareList();
                    drawList();
                    eventsData.updateWidgets(0);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_UnhideEvent) {

                if (eventsData.unsetHiddenEvent(eventKey)) {
                    this.invalidateOptionsMenu();
                    prepareList();
                    drawList();
                    eventsData.updateWidgets(0);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_Remind_1H) {

                eventsData.snoozeNotification(selectedEvent_str, 1, null);
                return true;

            } else if (itemId == Constants.ContextMenu_Remind_Morning) {

                Calendar now = Calendar.getInstance();
                now.add(Calendar.DAY_OF_MONTH, 1);
                now.set(Calendar.HOUR_OF_DAY, 9);
                now.set(Calendar.MINUTE, 0);
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);

                eventsData.snoozeNotification(selectedEvent_str, 0, now.getTime());
                return true;

            } else if (itemId == Constants.ContextMenu_AnniversaryList) {

                eventsData.showAnniversaryList(this);
                return true;

            } else if (itemId == Constants.ContextMenu_SilentEvent) {

                if (eventsData.setSilencedEvent(eventKey)) {
                    this.invalidateOptionsMenu();
                    prepareList();
                    drawList();
                    eventsData.updateWidgets(0);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_UnsilentEvent) {

                if (eventsData.unsetSilencedEvent(eventKey)) {
                    this.invalidateOptionsMenu();
                    prepareList();
                    drawList();
                    eventsData.updateWidgets(0);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_MergeEvent) {

                //https://developer.android.com/guide/components/intents-common#PickContact
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                try {
                    startActivityForResult(intent, Constants.RESULT_PICK_CONTACT);
                } catch (android.content.ActivityNotFoundException e) { *//**//* }

            } else if (itemId == Constants.ContextMenu_UnmergeEvent) {

                if (eventsData.setMergedID(selectedEvent[ContactsEvents.Position_eventID], null)) {
                    this.invalidateOptionsMenu();
                    prepareList();
                    drawList();
                    eventsData.updateWidgets(0);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_RemergeEvent) {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                try {
                    startActivityForResult(intent, Constants.RESULT_PICK_OTHER_CONTACT);
                } catch (android.content.ActivityNotFoundException e) { *//**//* }
                return true;

            } else if (itemId == Constants.ContextMenu_ShareAsImage) {

                //https://stackoverflow.com/questions/12742343/android-get-screenshot-of-all-listview-items
                //https://demonuts.com/android-take-screenshot/
                //https://stackoverflow.com/questions/19514174/convert-listview-items-into-a-single-bitmap-image

                ListView listView = findViewById(R.id.mainListView);
                View childView = adapter.getView(selectedEvent_num, null, listView);

                childView.measure(
                        View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                childView.layout(0, 0, childView.getMeasuredWidth(), childView.getMeasuredHeight());
                childView.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.theme_secondary)));
                childView.setDrawingCacheEnabled(true);
                childView.buildDrawingCache(true);
                Bitmap bmp = childView.getDrawingCache(true);
                if (bmp == null) {
                    ToastExpander.showText(this, "Error getting event image");
                    return false;
                }

                Uri bitmapShareUri = null;

                File file = new File(this.getCacheDir(), "event.jpg");
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                    bitmapShareUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                }
                childView.destroyDrawingCache();

                if (bitmapShareUri != null) {
                    //https://stackoverflow.com/questions/48045626/chooser-created-with-createchooserintent-title-doesnt-display-a-title
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/*");
                    final String[] mimeTypes = {"image/jpeg", "image/png"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes); //https://stackoverflow.com/questions/54478638/effect-of-intent-settype-on-androids-intent-chooser
                    intent.putExtra(Intent.EXTRA_STREAM, bitmapShareUri);
                    //intent.putExtra(Intent.EXTRA_TITLE, "Share event as image");
                    Intent chooser = Intent.createChooser(intent, "");
                    //https://stackoverflow.com/questions/57689792/permission-denial-while-sharing-file-with-fileprovider
                    List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        this.grantUriPermission(packageName, bitmapShareUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    try {
                        startActivity(chooser);
                        return true;
                    } catch (ActivityNotFoundException e) { *//**//* }
                }
                return true;

            } else if (itemId == Constants.ContextMenu_ShareAsText) {

                StringBuilder textBig = new StringBuilder();
                textBig
                        .append(eventsData.preferences_list_nameformat == 2 ? selectedEvent[Position_personFullNameAlt] : selectedEvent[Position_personFullName])
                        .append(Constants.STRING_EOL)
                        .append(selectedEvent[Position_eventEmoji])
                        .append(Constants.STRING_SPACE)
                        .append(selectedEvent[Position_eventDate])
                        .append(Constants.STRING_SPACE)
                        .append(selectedEvent[Position_eventCaption]);
                if (!TextUtils.isEmpty(selectedEvent[Position_age_caption].trim()))
                    textBig
                            .append(Constants.STRING_COLON_SPACE)
                            .append(selectedEvent[Position_age_caption]);

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Age))) {
                    String eventSubType = selectedEvent[Position_eventSubType];

                    if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) { //Если это день рождения или 5K
                        final String currentAge = selectedEvent[ContactsEvents.Position_age_current];
                        if (!currentAge.isEmpty() && !currentAge.startsWith(Constants.STRING_0)) {
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            if (eventsData.set_events_deaths.contains(selectedEvent[ContactsEvents.Position_contactID])) { //Но есть годовщина смерти
                                textBig.append(getString(R.string.msg_age_could_be));
                            } else {
                                textBig.append(getString(R.string.msg_age_now));
                            }
                            textBig.append(currentAge);
                        }
                    } else if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_Death)) && eventsData.set_events_birthdays.containsKey(selectedEvent[ContactsEvents.Position_contactID])) { //Если это годовщина смерти
                        Locale locale_en = new Locale(Constants.LANG_EN);
                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                        Date eventDate = sdfYear.parse(selectedEvent[ContactsEvents.Position_eventDateText]);
                        Date birthDate = eventsData.set_events_birthdays.get(selectedEvent[ContactsEvents.Position_contactID]);
                        if (eventDate != null && birthDate != null) {
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            textBig.append(getString(R.string.msg_age_was)).append(eventsData.countDaysDiffText(birthDate, eventDate, 3));
                        }
                    }
                }


                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, textBig.toString());
                //intent.putExtra(Intent.EXTRA_TITLE, "Share event as text");
                startActivity(Intent.createChooser(intent, ""));
                return true;

            }
            return super.onContextItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return true;
        }
    }*/

    synchronized private void prepareList() {

        try{
            statsAllEvents = 0;
            statsHiddenEvents = 0;
            statsSilencedEvents = 0;
            eventsData.statEventsPrevEventsFound = 0;
            final List<String> eventList_toPrepare = new ArrayList<>(eventsData.eventList);
            statsAllEvents = eventList_toPrepare.size();
            dataList.clear();
            dataListFull.clear();

            if (!eventsData.isEmptyEventList()) {
                for (String event : eventList_toPrepare) {
                    String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                    String eventKey = eventsData.getEventKey(singleEventArray);

                    if (eventsData.preferences_list_event_types.contains(singleEventArray[ContactsEvents.Position_eventType])) {
                        if (eventsData.getHiddenEventsCount() == 0 && eventsData.getSilencedEventsCount() == 0) { //Скрытых и без уведомлений нет
                            dataList.add(event);
                        } else {

                            if (eventsData.checkIsHiddenEvent(eventKey)) statsHiddenEvents++;
                            if (eventsData.checkIsSilencedEvent(eventKey)) statsSilencedEvents++;

                            if ((eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_NotHidden && !eventsData.checkIsHiddenEvent(eventKey)) || //Показывать нескрытые
                                    (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden && eventsData.checkIsHiddenEvent(eventKey)) || //Показывать только скрытые
                                    (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Silenced && eventsData.checkIsSilencedEvent(eventKey)) || //Показывать только без уведомлений
                                    eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_All) {
                                dataList.add(event);
                            }
                        }
                    }
                }
            }
            if (dataList.isEmpty()) {

                findViewById(R.id.mainListView).setVisibility(GONE);

                TextView viewZero = findViewById(R.id.mainListViewEmpty);
                if (viewZero != null) {
                    viewZero.setVisibility(View.VISIBLE);
                    viewZero.setText(HtmlCompat.fromHtml(
                        getString(R.string.msg_zero_events_title) +
                                eventsData.getCurrentParams() +
                                getString(R.string.msg_zero_events_footer)
                        , 0));
                    //https://stackoverflow.com/questions/1748977/making-textview-scrollable-on-android
                    viewZero.setMovementMethod(new ScrollingMovementMethod());
                }

                setHint(eventsData.setHTMLColor(getString(R.string.msg_no_events).toLowerCase(), Constants.HTML_COLOR_YELLOW));

            } else {

                findViewById(R.id.mainListView).setVisibility(View.VISIBLE);
                findViewById(R.id.mainListViewEmpty).setVisibility(View.GONE);

                if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden) {
                    setHint(resources.getString(R.string.msg_stats_hidden_prefix) + statsHiddenEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_All) {
                    setHint(resources.getString(R.string.msg_stats_prefix) + statsAllEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Silenced) {
                    setHint(resources.getString(R.string.msg_stats_silenced_prefix) + statsSilencedEvents + Constants.STRING_SPACE);
                } else {
                    setHint(resources.getString(R.string.msg_stats_prefix) + (statsAllEvents-statsHiddenEvents) + Constants.STRING_SPACE);
                }

                //Получаем предыдущие события
                if (!TextUtils.isEmpty(eventsData.preferences_list_prev_events) && eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Hidden) {
                    //todo: придумать, как ловить прошедшие 5K+
                    dataList.addAll(0, eventsData.getPreviousEvents(dataList, eventsData.preferences_list_prev_events));
                }

            }
            //}
            dataListFull.addAll(dataList);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    synchronized private void drawList() {

        try {

            //Выводим данные
            ListView listView = findViewById(R.id.mainListView);

            //Сохраняем позицию в списке, чтобы вернутся к ней после обновления
            //https://stackoverflow.com/a/3035521/4928833
            int index = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
            int top = (v == null) ? 0 : (v.getTop()) - listView.getPaddingTop();

            adapter = new EventsAdapter(this, dataListFull, dataList);
            listView.setAdapter(adapter);

            //Возвращаемся к ранее сохранённой позиции после обновления
            //Почему-то при index = 0 идёт сдвиг вверх на getPaddingTop
            listView.setSelectionFromTop(index, index > 0 ? top : top + listView.getPaddingTop());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setHint(@NonNull String msg) {

        try {
            TextView stats = findViewById(R.id.mainStatsTextView);
            stats.setText(HtmlCompat.fromHtml(msg, 0), TextView.BufferType.SPANNABLE);

            if (eventsData.preferences_list_marging > 0) {
                    stats.setPadding(
                            stats.getPaddingLeft(),
                            stats.getPaddingTop(),
                            (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                            stats.getPaddingBottom());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    synchronized void updateList(boolean disableSwipeRefresh, boolean useBackgroundThread) {

        try {

            if (disableSwipeRefresh) {
                swipeRefresh.setEnabled(false); // setEnable(false) need to be before setRefreshing
                swipeRefresh.setRefreshing(true);
            }

            if (useBackgroundThread) {

                executor.execute(() -> {

                    //Background work
                    if (eventsData.getEvents(this))
                        eventsData.computeDates();

                    handler.post(() -> {

                        //UI Thread
                        prepareList();
                        drawList();

                        swipeRefresh.setRefreshing(false);
                        if (disableSwipeRefresh) swipeRefresh.setEnabled(true);

                        this.invalidateOptionsMenu();
                        eventsData.updateWidgets(0);

                        if (eventsData.isEmptyEventList())
                            showZeroEventsHints();

                    });
                });

            } else {

                if (eventsData.getEvents(this))
                    eventsData.computeDates();
                prepareList();
                drawList();

                swipeRefresh.setRefreshing(false);
                if (disableSwipeRefresh) swipeRefresh.setEnabled(true);

                this.invalidateOptionsMenu();
                eventsData.updateWidgets(0);

                showZeroEventsHints();
            }

        } catch (Exception e) {
            swipeRefresh.setRefreshing(false);
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void showZeroEventsHints() {

        try {
            //Сообщение для тех, у кого не найдено ни одного события контактов
            if (!triggeredMsgNoEvents && !eventsData.checkNoContactsAccess() && dataListFull.isEmpty()) {
                triggeredMsgNoEvents = true;
                if (!eventsData.getPreferences_Accounts().isEmpty()
                        && !eventsData.getPreferences_Accounts().contains(Constants.account_none)
                        && eventsData.statContactsEventCount > 0
                        && eventsData.statFilesEventCount == 0
                        && eventsData.statCalendarsEventCount == 0) { //... но выбраны конкретные аккаунты (или "ничего")

                    showAlertNoEventsWithAccounts();

                } else {

                    showAlertNoEvents();

                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private static class ViewHolder {
        //https://stackoverflow.com/questions/21501316/what-is-the-benefit-of-viewholder
        //todo: перейти на RecycleView https://www.spreys.com/listview-to-recyclerview/

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

    class EventsAdapter extends ArrayAdapter<String> implements Filterable {

        final ContactsEvents eventsData;
        final Resources resources;
        private final List<String> listAll;
        final int dimen_details;
        final int dimen_name;
        final int dimen_date;

        private EventsAdapter(@NonNull Context context, List<String> eventsListFull, List<String> eventsList)
        {

            super(context, R.layout.entry_main, eventsList.toArray(new String[0]));

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            resources = eventsData.getContext().getResources();

            listAll = new ArrayList<>(eventsListFull);
            dimen_details = (int) (eventsData.dimen_List_details / eventsData.DisplayMetrics_density);
            dimen_name = (int) (eventsData.dimen_List_name / eventsData.DisplayMetrics_density);
            dimen_date = (int) (eventsData.dimen_list_date / eventsData.DisplayMetrics_density);

        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View convertedView = convertView;
            ViewHolder holder;
            String[] singleEventArray;
            Person person;
            String event;

            try {

                if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());

                if (convertedView == null) {
                    LayoutInflater inflater = LayoutInflater.from(eventsData.getContext());
                    convertedView = inflater.inflate(R.layout.entry_main, parent, false);
                    holder = createViewHolderFrom(convertedView);
                    convertedView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertedView.getTag();
                }

                event = getItem(position);
                if (event == null) return convertedView;
                singleEventArray = event.split(Constants.STRING_EOT, -1);
                if (singleEventArray.length < ContactsEvents.Position_attrAmount) return convertedView;

                person = new Person(eventsData.getContext(), event);

                String eventDistance = singleEventArray[ContactsEvents.Position_eventDistance];
                String[] eventDistanceText = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                switch (eventDistance) {

                    case Constants.STRING_0: //Сегодня

                        final String caption = eventsData.preferences_list_custom_todayevent_caption;
                        if (caption.isEmpty()) {
                            holder.DayDistanceTextView.setText(eventDistanceText[0]);
                        } else {
                            holder.DayDistanceTextView.setText(caption);
                        }
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(eventsData.preferences_list_color_eventtoday);
                        break;

                    case Constants.STRING_1: //Завтра и послезавтра
                    case Constants.STRING_2:

                        final String distance_near = eventDistanceText[0] + (eventDistanceText.length > 1 ? Constants.STRING_SPACE + eventDistanceText[1] : Constants.STRING_EMPTY);
                        holder.DayDistanceTextView.setText(distance_near);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(eventsData.preferences_list_color_eventsoon);
                        break;

                    default: //Попозже

                        final String distance_far = eventDistanceText[0] + (eventDistanceText.length > 1 ? Constants.STRING_SPACE + eventDistanceText[1] : Constants.STRING_EMPTY);
                        holder.DayDistanceTextView.setText(distance_far);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.NORMAL);
                        holder.DayDistanceTextView.setTextColor(ta.getColor(R.styleable.Theme_eventDistanceColor, ContextCompat.getColor(eventsData.getContext(), R.color.theme_black_primary)));

                }

                //Дата оригинального события
                holder.DateTextView.setText(eventsData.getDateFormated(singleEventArray[ContactsEvents.Position_eventDateText], ContactsEvents.FormatDate.WithYear));

                switch (eventsData.preferences_list_nameformat) {
                    case 2: //Фамилия Имя Отчество
                        holder.NameTextView.setText(singleEventArray[Position_personFullNameAlt]);
                        break;
                    case 1: //Имя Отчество Фамилия
                    default:
                        holder.NameTextView.setText(singleEventArray[Position_personFullName]);
                        break;
                }

                //Инфо под именем
                StringBuilder eventDetails = new StringBuilder();

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_JobTitle))) {
                    final String contactOrganization = ContactsEvents.checkForNull(singleEventArray[ContactsEvents.Position_organization]).trim();
                    if (!contactOrganization.isEmpty()) eventDetails.append(contactOrganization.trim());
                }
                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Organization))) {
                    final String positionJobTitle = ContactsEvents.checkForNull(singleEventArray[ContactsEvents.Position_title]).trim();
                    if (!positionJobTitle.isEmpty()) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.STRING_COMMA_SPACE);
                        eventDetails.append(positionJobTitle);
                    }
                }
                if (eventDetails.length() > 0) {
                    eventDetails.insert(0, Constants.HTML_BOLD_START).append(Constants.HTML_BOLD_END);
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Nickname)) && singleEventArray[ContactsEvents.Position_nickname].trim().length() > 0) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(singleEventArray[ContactsEvents.Position_nickname]);
                }

                String eventSubType = singleEventArray[Position_eventSubType];
                String eventLabel = singleEventArray[ContactsEvents.Position_eventLabel].trim();
                String eventCaption = singleEventArray[Position_eventCaption].trim();
                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_EventCaption))) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(eventCaption);
                    if (!eventLabel.isEmpty() && eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_StoredEventTitle)) && !eventCaption.equals(eventLabel)) {
                        eventDetails.append(Constants.STRING_PARENTHESIS_OPEN).append(eventLabel).append(Constants.STRING_PARENTHESIS_CLOSE);
                    }

                    if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {
                        final String strZodiacInfo = eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_ZodiacSign)) ?
                                singleEventArray[ContactsEvents.Position_zodiacSign].trim() : Constants.STRING_EMPTY;
                        final String strZodiacYearInfo = eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_ZodiacYear)) ?
                                singleEventArray[ContactsEvents.Position_zodiacYear].trim() : Constants.STRING_EMPTY;

                        if (!strZodiacInfo.isEmpty() || !strZodiacYearInfo.isEmpty()) {
                            eventDetails.append(Constants.STRING_PARENTHESIS_OPEN).append((strZodiacInfo.concat(Constants.STRING_SPACE).concat(strZodiacYearInfo)).trim()).append(Constants.STRING_PARENTHESIS_CLOSE);
                        }
                    }

                } else if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_StoredEventTitle)) && !eventLabel.isEmpty()) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(eventLabel);
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Age))) {
                    if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) { //Если это день рождения или 5K
                        final String currentAge = singleEventArray[ContactsEvents.Position_age_current];
                        if (!currentAge.isEmpty() && !currentAge.startsWith(Constants.STRING_0)) {
                            if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                            if (eventsData.set_events_deaths.contains(singleEventArray[ContactsEvents.Position_contactID])) { //Но есть годовщина смерти
                                eventDetails.append(getString(R.string.msg_age_could_be));
                            } else {
                                eventDetails.append(getString(R.string.msg_age_now));
                            }
                            eventDetails.append(currentAge);
                        }
                    } else if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_Death)) && eventsData.set_events_birthdays.containsKey(singleEventArray[ContactsEvents.Position_contactID])) { //Если это годовщина смерти
                        Locale locale_en = new Locale(Constants.LANG_EN);
                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                        Date eventDate = sdfYear.parse(singleEventArray[ContactsEvents.Position_eventDateText]);
                        Date birthDate = eventsData.set_events_birthdays.get(singleEventArray[ContactsEvents.Position_contactID]);
                        if (eventDate != null && birthDate != null) {
                            if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                            eventDetails.append(getString(R.string.msg_age_was)).append(eventsData.countDaysDiffText(birthDate, eventDate, 3));
                        }
                    }
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_URL))) {
                    final String eventURL = singleEventArray[ContactsEvents.Position_eventURL].trim();
                    if (eventURL.length() > 0) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        eventDetails.append(eventURL.replace(Constants.STRING_2TILDA, Constants.HTML_BR));
                    }
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_EventSource))) {
                    final String eventSource = singleEventArray[ContactsEvents.Position_eventSource].trim();
                    if (eventSource.length() > 0) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        eventDetails.append(eventSource.replace(Constants.STRING_2TILDA, Constants.HTML_BR));
                    }
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_DebugInfo))) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(singleEventArray[ContactsEvents.Position_dates].replace(Constants.STRING_2TILDA, Constants.HTML_BR).trim());
                }

                String eventKey = eventsData.getEventKey(singleEventArray);
                if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_All && eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(eventKey)) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(eventsData.setHTMLColor(getString(R.string.msg_label_hidden), Constants.HTML_COLOR_RED));
                }
                if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Silenced && eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(eventKey)) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(eventsData.setHTMLColor(getString(R.string.msg_label_silenced), Constants.HTML_COLOR_BROWN));
                }

                if (eventDetails.length() == 0) {
                    holder.DetailsTextView.setText(Constants.STRING_EMPTY);
                } else {
                    //https://stackoverflow.com/questions/2116162/how-to-display-html-in-textview
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        holder.DetailsTextView.setText(HtmlCompat.fromHtml(eventDetails.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT));
                    } else {
                        holder.DetailsTextView.setText(HtmlCompat.fromHtml(eventDetails.toString(), 0));
                    }
                }

                //Фото
                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Photo))) {
                    int roundingFactor;
                    if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_CalendarEvent)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent))) {
                        roundingFactor = 1;
                    } else {
                        roundingFactor = eventsData.preferences_list_photostyle;
                    }

                    holder.PhotoImageView.setImageBitmap(eventsData.getContactPhoto(event, true, false, false, roundingFactor));
                    holder.PhotoImageView.setVisibility(View.VISIBLE);
                } else {
                    holder.PhotoImageView.setImageBitmap(null);
                    holder.PhotoImageView.setVisibility(GONE);
                }

                if (person.Age > -1 && person.Age % 10 == 0) {
                    holder.CounterTextView.setTextColor(eventsData.preferences_list_color_eventjubilee);
                } else {
                    holder.CounterTextView.setTextColor(ta.getColor(R.styleable.Theme_eventAgeColor, ContextCompat.getColor(eventsData.getContext(), R.color.theme_grey_primary)));
                }
                holder.CounterTextView.setText(person.Age_str);

                //Определяем иконку события
                int eventIcon;
                try {
                    eventIcon = Integer.parseInt(singleEventArray[ContactsEvents.Position_eventIcon]);
                } catch (NumberFormatException e) {
                    eventIcon = 0;
                }
                if (eventIcon != 0 && eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_EventIcon))) {
                    holder.EventIconImageView.setImageResource(eventIcon);
                } else {
                    holder.EventIconImageView.setImageDrawable(null);
                }

                //Фон
                GradientDrawable drawableBack = new GradientDrawable();

                if (eventsData.preferences_list_style == Integer.parseInt(getString(R.string.pref_List_Style_Card))) {

                    drawableBack.setStroke((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, displayMetrics),
                            ta.getColor(R.styleable.Theme_borderCardColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker)));
                    drawableBack.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, displayMetrics));

                }

                if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_None))) {

                    drawableBack.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    drawableBack.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                    drawableBack.setColors(new int[]{
                            ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(eventsData.getContext(), R.color.theme_secondary)),
                            ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(eventsData.getContext(), R.color.theme_secondary)),
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_LeftToRight))) {

                    drawableBack.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    drawableBack.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                    drawableBack.setColors(new int[]{
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray)),
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker))
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_RightToLeft))) {

                    drawableBack.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    drawableBack.setOrientation(GradientDrawable.Orientation.RIGHT_LEFT);
                    drawableBack.setColors(new int[] {
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray)),
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker))
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_TopToBottom))) {

                    drawableBack.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    drawableBack.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                    drawableBack.setColors(new int[] {
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray)),
                            //ta.getColor(R.styleable.Theme_gradientCenterColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_transp)),
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker))
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_BottomToTop))) {

                    drawableBack.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    drawableBack.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                    drawableBack.setColors(new int[] {
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray)),
                            //ta.getColor(R.styleable.Theme_gradientCenterColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_transp)),
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker))
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_FromCenter))) {

                    drawableBack.setGradientType(GradientDrawable.RADIAL_GRADIENT);
                    drawableBack.setGradientRadius((float)parent.getWidth()/2);
                    drawableBack.setColors(new int[] {
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray)),
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker))
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_ToCenter))) {

                    drawableBack.setGradientType(GradientDrawable.RADIAL_GRADIENT);
                    drawableBack.setGradientRadius((float)parent.getWidth()/2);
                    drawableBack.setColors(new int[] {
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker)),
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray))
                    });

                }

                convertedView.setBackground(drawableBack);
                convertedView.setAlpha(1);

                //Прозрачность для прошедших событий
                holder.NameTextView.setAlpha(1);
                holder.CounterTextView.setAlpha(1);
                holder.DateTextView.setAlpha(1);
                holder.DayDistanceTextView.setAlpha(1);
                holder.DetailsTextView.setAlpha(1);
                holder.PhotoImageView.setImageAlpha(255);
                holder.EventIconImageView.setImageAlpha(255);

                if (eventsData.statEventsPrevEventsFound > 0 && filterNames.isEmpty()) {

                    if (position <= eventsData.statEventsPrevEventsFound - 1) {
                        final float alphaPrev = (float) 0.6;
                        holder.NameTextView.setAlpha(alphaPrev);
                        holder.CounterTextView.setAlpha(alphaPrev);
                        holder.DateTextView.setAlpha(alphaPrev);
                        holder.DayDistanceTextView.setAlpha(alphaPrev);
                        holder.DetailsTextView.setAlpha(alphaPrev);
                        holder.PhotoImageView.setImageAlpha((int)(255*alphaPrev));
                        holder.EventIconImageView.setImageAlpha((int)(255*alphaPrev));
                    }

                    //if (position == eventsData.preferences_list_prev_events_found - 1)  convertView.setBackground(eventsData.context.getDrawable(R.drawableBack.prev_event_border));

                }

            } catch (InflateException ie) {
                /**/
            } catch (Exception e) {
                e.printStackTrace();
                if (eventsData.preferences_debug_on)
                    ToastExpander.showText(eventsData.getContext(), ContactsEvents.getMethodName(2) + Constants.STRING_COLON_SPACE + e);
            }

            if (convertedView != null) return convertedView;
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            LayoutInflater inflater = LayoutInflater.from(eventsData.getContext());
            try {
                return inflater.inflate(R.layout.entry_main, parent, false);
            } catch (InflateException ie) {
                return parent;
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

            DayDistanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + eventsData.preferences_list_magnify_distance * 0.1)));
            NameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + eventsData.preferences_list_magnify_name * 0.1)));
            DetailsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + eventsData.preferences_list_magnify_details * 0.1)));
            DateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_date * (1 + eventsData.preferences_list_magnify_date * 0.1)));
            CounterTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + eventsData.preferences_list_magnify_age * 0.1)));

            /*if (eventsData.preferences_list_marging > 0) {
                LinearLayout main = view.findViewById(R.id.entryLayout);
                *//*if (main != null)
                    main.setPadding(
                        main.getPaddingLeft() + (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                        main.getPaddingTop(),
                        main.getPaddingRight() + (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                        main.getPaddingBottom());*//*
            }*/

            return new ViewHolder(NameTextView, DayDistanceTextView, DateTextView, DetailsTextView, PhotoImageView, CounterTextView, EventIconImageView);
        }

        //https://www.youtube.com/watch?v=sJ-Z9G0SDhc
        @Override
        @NonNull
        public Filter getFilter() {
            return EventsFilter;
        }

        private final Filter EventsFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                List<String> dataList_filtered = new ArrayList<>();

                try {

                    if (constraint == null || constraint.length() == 0) {
                        dataList_filtered.addAll(listAll);
                        filterNames = Constants.STRING_EMPTY;
                    } else {
                        //для поиска AND используем <строка1>+<строка2>
                        //для поиска OR используем <строка1>,<строка2>
                        filterNames = ContactsEvents.normalizeName(constraint.toString());
                        if (filterNames.contains("+")) {
                            String[] params = filterNames.split(Constants.REGEX_PLUS);
                            for (String listItem : listAll) {
                                final String item = listItem.toLowerCase();
                                int matches = 0;
                                for (String param: params) {
                                    if (!item.contains(param)) {
                                        break;
                                    }
                                    matches++;
                                }
                                if (matches == params.length) {
                                    dataList_filtered.add(listItem);
                                }
                            }

                        } else {
                            Matcher filter = Pattern.compile(filterNames.replaceAll(Constants.REGEX_COMMAS, Constants.STRING_COMMA).replace(Constants.STRING_COMMA, "|"), Pattern.CASE_INSENSITIVE).matcher(Constants.STRING_EMPTY);
                            for (String listItem : listAll) {
                                if (filter.reset(listItem).find()) {
                                    if (!dataList_filtered.contains(listItem)) {
                                        dataList_filtered.add(listItem);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    if (eventsData.preferences_debug_on) ToastExpander.showText(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                }

                FilterResults results = new FilterResults();
                results.values = dataList_filtered;
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                try {
                    if (results.values != null) {
                        dataList.clear();
                        dataList.addAll((ArrayList<String>) results.values);
                    }

                    if (dataList.size() > 0) {
                        if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden) {
                            setHint(
                                    (filterNames.isEmpty() ? resources.getString(R.string.msg_stats_hidden_prefix) : resources.getString(R.string.msg_stats_hidden_filtered_prefix))
                                    .concat(filterNames.isEmpty() ? String.valueOf(dataList.size()) : eventsData.setHTMLColor(String.valueOf(dataList.size()), Constants.HTML_COLOR_YELLOW))
                                    .concat(Constants.STRING_SPACE)
                            );
                        } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Silenced) {
                            setHint(
                                    (filterNames.isEmpty() ? resources.getString(R.string.msg_stats_silenced_prefix) : resources.getString(R.string.msg_stats_silenced_filtered_prefix))
                                    .concat(filterNames.isEmpty() ? String.valueOf(dataList.size()) : eventsData.setHTMLColor(String.valueOf(dataList.size()), Constants.HTML_COLOR_YELLOW))
                                    .concat(Constants.STRING_SPACE)
                            );
                        } else {
                            setHint(
                                    (filterNames.isEmpty() ? resources.getString(R.string.msg_stats_prefix) : resources.getString(R.string.msg_stats_filtered_prefix))
                                    .concat(filterNames.isEmpty() ? ""+(statsAllEvents - statsHiddenEvents) : eventsData.setHTMLColor(String.valueOf(dataList.size()), Constants.HTML_COLOR_YELLOW))
                                    .concat(Constants.STRING_SPACE)
                            );
                        }

                    } else {
                        setHint(eventsData.setHTMLColor(getString(R.string.msg_no_events).toLowerCase(), Constants.HTML_COLOR_YELLOW).concat(Constants.STRING_SPACE));
                    }
                    drawList();

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    if (eventsData.preferences_debug_on) ToastExpander.showText(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                }

            }
        };
    }

    @Override
    public void applyOverrideConfiguration(@Nullable Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            int uiMode = overrideConfiguration.uiMode;
            overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
            overrideConfiguration.uiMode = uiMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }
}