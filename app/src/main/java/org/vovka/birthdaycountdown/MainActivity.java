/*
 * *
 *  * Created by Vladimir Belov on 14.07.2026, 12:14
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 14.07.2026, 10:43
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MainActivity - это активность для отображения списка событий.
 */
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainActivity";

    //UI объекты
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Resources resources;
    private SwipeRefreshLayout swipeRefresh;
    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener;
    private ListView listView;
    private TextView emptyView;

    //Переменные
    private String filterNames = Constants.STRING_EMPTY;
    private ContactsEvents eventsData;
    private String selectedEvent_str;
    private String[] selectedEvent;
    private int selectedEvent_num;
    private final List<String> dataList = new ArrayList<>();
    private final List<String> dataListFull = new ArrayList<>();
    private EventsAdapter adapter;
    private int statsAllEvents = 0; //Всего событий (для выбранных источников и типов)
    private int statsHiddenEvents = 0; //Всего скрытых событий (для выбранных источников и типов)
    private int statsAllHiddenEvents = 0; //Всего скрытых событий (всего)
    private int statsSilencedEvents = 0;
    private int statsXDaysEvents = 0;
    private int statsUnrecognizedEvents = 0;
    private boolean triggeredMsgNoEvents = false;
    private TypedArray ta = null;
    private DisplayMetrics displayMetrics;
    boolean scrolledToToday = false;
    private String localeAtCreate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {

            //https://habr.com/ru/post/648535/
            //SplashScreen splashScreen = SplashScreen.installSplashScreen(this); - с вызовом этого не ставится цвет статуса и кнопок

            //Оформление стиля окна приложения
            //https://stackoverflow.com/questions/22192291/how-to-change-the-status-bar-color-in-android
            //https://stackoverflow.com/questions/29069070/completely-transparent-status-bar-and-navigation-bar-on-lollipop

            eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);
            localeAtCreate = eventsData.currentLocale;
            resources = eventsData.getResources();
            displayMetrics = resources.getDisplayMetrics();

            //Устанавливаем тему
            //https://carthrottle.io/how-to-implement-flexible-night-mode-in-your-android-app-f00f0f83b70e
            //https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94

            this.setTheme(eventsData.preferences_theme.themeMain);
            eventsData.currentTheme = eventsData.preferences_theme.themeMain;
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            try {
                super.onCreate(savedInstanceState);
            } catch (Exception e) {
                // Если super.onCreate() упал — не маскируем, а логируем и пробрасываем
                Log.e(TAG, "super.onCreate() failed", e);
                throw e;
            }

            filterNames = savedInstanceState == null ? Constants.STRING_EMPTY : savedInstanceState.getString(Constants.EXTRA_FILTER, Constants.STRING_EMPTY);

            setContentView(R.layout.activity_main);

            View layoutMain = findViewById(R.id.layout_main);
            if (DeviceTools.isEdgeToEdge()) {
                View layoutCoordinator = findViewById(R.id.coordinator);
                ViewCompat.setOnApplyWindowInsetsListener(layoutCoordinator, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                    layoutCoordinator.setPadding(0, insets.top, 0, insets.bottom);
                    layoutMain.setPadding(0, insets.bottom + ImageUtils.Sp2Px(resources, 62), 0, 0);
                    return WindowInsetsCompat.CONSUMED;
                });
            } else {
                layoutMain.setPadding(0, ImageUtils.Dip2Px(resources, 62), 0, 0);
            }
            //Цвет CutoutAppearance на повёрнутом экране
            //https://stackoverflow.com/questions/58896621/how-can-i-color-the-cutout-notch-area-in-non-full-screen-landscape-mode
            getWindow().setBackgroundDrawable(new ColorDrawable(ta.getColor(R.styleable.Theme_colorPrimary, ContextCompat.getColor(this, R.color.white))));

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна https://github.com/neokree/MaterialNavigationDrawer/issues/5
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            swipeRefresh = findViewById(R.id.swiperefresh);
            if (swipeRefresh != null) {
                swipeRefresh.setColorSchemeColors(
                        ta.getColor(R.styleable.Theme_colorPrimary, Color.BLACK),
                        eventsData.preferences_list_color_eventtoday,
                        eventsData.preferences_list_color_eventsoon
                );
                swipeRefresh.setOnRefreshListener(this);
            }

            //About
            findViewById(R.id.toolbar).setOnClickListener(v -> {
                Intent intent = new Intent(this, AboutActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }
            });

            swipeRefreshListener = () -> {
                if (eventsData.flagIsUpdating) {
                    swipeRefresh.post(() -> swipeRefresh.setRefreshing(true));
                    return;
                }
                try {

                    eventsData.needUpdateEventList = true;
                    updateList(false);

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    ToastExpander.showDebugMsg(MainActivity.this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                } finally {
                    swipeRefresh.setRefreshing(false);
                }
            };

            listView = findViewById(R.id.mainListView);
            emptyView = findViewById(R.id.mainListViewEmpty);

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
                        String eventInfo = (String) l.getItemAtPosition(position);
                        Intent intent = ContactsEvents.getViewActionIntent(
                                eventInfo,
                                Constants.STRING_EMPTY,
                                eventInfo.split(Constants.STRING_EOT, -1),
                                eventsData.preferences_list_on_click_action,
                                this
                        );
                        if (intent != null) {
                            try {
                                MainActivity.this.startActivity(intent);
                            } catch (ActivityNotFoundException e) { /**/ }
                        } else {
                            ToastExpander.showInfoMsg(this, resources.getString(R.string.msg_no_action));
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                }
            });

            listView.setOnItemLongClickListener((parent, v, position, id) -> onCreatePopupMenu(listView, v, position));

            //Приветственное сообщение или описание новой версии
            showWelcomeScreen();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private boolean onCreatePopupMenu(ListView listView, View view, int position) {
        try {

            //https://developer.alexanderklimov.ru/android/popupmenu.php
            //https://stackoverflow.com/questions/1245543/add-context-menu-icon-in-android
            //https://stackoverflow.com/questions/49706495/how-to-pass-a-custom-layout-to-a-popupmenu
            //https://stackoverflow.com/questions/64236522/how-to-implement-android-custom-popup-menu
            //https://stackoverflow.com/questions/23516247/how-change-position-of-popup-menu-on-android-overflow-button
            //https://stackoverflow.com/questions/4604562/override-context-menu-colors-in-android
            //todo: подсказки про именины на основе имени и даты рождения
            //todo: ссылки с имени и фамилии на web справочник

            //eventsData.setLocale(true); //без установки слетает язык на поворотах экрана
            PopupMenu popupMenu = new PopupMenu(
                    new ContextThemeWrapper(MainActivity.this, eventsData.preferences_theme.themePopup),
                    view,
                    Gravity.RIGHT | Gravity.FILL_VERTICAL
            );
            final Menu menu = popupMenu.getMenu();
            setMenuIconsVisible(menu);

            selectedEvent_num = position;
            selectedEvent_str = (String) listView.getItemAtPosition(position);
            selectedEvent = selectedEvent_str.split(Constants.STRING_EOT, -1);
            String eventSubtype = selectedEvent[ContactsEvents.Position_eventSubType];
            final String eventKey = eventsData.getEventKey(selectedEvent);
            final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(selectedEvent);
            boolean isContactEvent = !eventSubtype.equals(Constants.EventType_Other) &&
                    !eventSubtype.equals(Constants.EventType_Holiday);

            //https://stackoverflow.com/questions/18632331/using-contextmenu-with-listview-in-android
            //menu.setHeaderTitle(dataArray1[ContactsEvents.dataMap.get("fio")] + ":");

            String eventStorage = selectedEvent[ContactsEvents.Position_eventStorage];
            if (eventStorage.contains(Constants.EVENT_PREFIX_LOCAL_EVENT)) {
                MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_EditLocalEvent, Menu.NONE, getString(R.string.menu_context_edit_local_event))
                        .setIcon(android.R.drawable.ic_menu_edit);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                }

                if (isContactEvent) {
                    menuItem = menu.add(Menu.NONE, Constants.ContextMenu_CreateFromEventLocalEvent, Menu.NONE, getString(R.string.menu_context_create_from_local_event))
                            .setIcon(android.R.drawable.ic_menu_add);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                    }
                }
            }

            String contactID = selectedEvent[ContactsEvents.Position_contactID];
            if (!TextUtils.isEmpty(contactID)) {
                menu.add(Menu.NONE, Constants.ContextMenu_EditContact, Menu.NONE, getString(R.string.menu_context_edit_contact))
                        .setIcon(android.R.drawable.ic_menu_edit);
            } else if (isContactEvent && eventsData.isFeatureEnabled(Constants.FEATURE_ADV_ACTIONS)) {
                MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_CreateContact, Menu.NONE, getString(R.string.menu_context_create_contact))
                        .setIcon(R.drawable.ic_menu_invite);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                }
            }

            if (!selectedEvent[ContactsEvents.Position_eventID].isEmpty()) {

                if (eventStorage.contains(Constants.STRING_STORAGE_CALENDAR)) {
                    menu.add(Menu.NONE, Constants.ContextMenu_OpenCalendar, Menu.NONE, getString(R.string.menu_context_open_calendar_event))
                            .setIcon(android.R.drawable.ic_menu_month);
                    if (eventsData.isFeatureEnabled(Constants.FEATURE_ADV_ACTIONS)) {
                        final String eventKeyId = selectedEvent[ContactsEvents.Position_eventID]
                                + Constants.STRING_2HASH + eventSubtype;
                        if (eventsData.checkIsEventWithoutYear(eventKeyId)) {
                            menu.add(Menu.NONE, Constants.ContextMenu_UnsetEventWithoutYear, Menu.NONE, getString(R.string.menu_context_unset_without_year))
                                    .setIcon(android.R.drawable.ic_menu_revert);
                        } else {
                            MenuItem menuItem = menu.add(Menu.NONE, Constants.ContextMenu_SetEventWithoutYear, Menu.NONE, getString(R.string.menu_context_set_without_year))
                                    .setIcon(R.drawable.ic_menu_block);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
                            }
                        }
                    }
                }

                if (Constants.EventType_BirthDay.equals(eventSubtype)) {
                    String mergedID = eventsData.getMergedID(selectedEvent[ContactsEvents.Position_eventID]);
                    if (!TextUtils.isEmpty(mergedID)) {
                        menu.add(Menu.NONE, Constants.ContextMenu_UnmergeEvent, Menu.NONE, getString(R.string.menu_context_unmerge_event))
                                .setIcon(R.drawable.ic_menu_chat_dashboard);
                        menu.add(Menu.NONE, Constants.ContextMenu_RemergeEvent, Menu.NONE, getString(R.string.menu_context_remerge_event))
                                .setIcon(R.drawable.ic_menu_copy);
                    } else if ((eventStorage.contains(Constants.STRING_STORAGE_CALENDAR) || eventStorage.contains(Constants.STRING_STORAGE_FILE))
                            && selectedEvent[ContactsEvents.Position_contactID].isEmpty()) {
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

            MenuItem menuItem;
            if (!eventKey.isEmpty()) {

                if (!Constants.STRING_1.equals(selectedEvent[ContactsEvents.Position_starred])) {
                    if (eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, selectedEvent[ContactsEvents.Position_starred])) {
                        menu.add(Menu.NONE, Constants.ContextMenu_RemoveFromFavorites, Menu.NONE, getString(R.string.menu_context_favorites_remove))
                                .setIcon(R.drawable.ic_menu_unstar);
                    } else {
                        menuItem = menu.add(Menu.NONE, Constants.ContextMenu_AddToFavorites, Menu.NONE, getString(R.string.menu_context_favorites_add))
                                .setIcon(R.drawable.ic_menu_star);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_yellow)));
                        }
                    }
                }

                if (eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {

                    menu.add(Menu.NONE, Constants.ContextMenu_UnhideEvent, Menu.NONE, getString(R.string.menu_context_unhide_event))
                            .setIcon(android.R.drawable.ic_menu_revert);

                } else {

                    menuItem = menu.add(Menu.NONE, Constants.ContextMenu_HideEvent, Menu.NONE, getString(R.string.menu_context_hide_event))
                            .setIcon(R.drawable.ic_menu_block);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
                    }

                }

                if (eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {

                    menu.add(Menu.NONE, Constants.ContextMenu_UnsilentEvent, Menu.NONE, getString(R.string.menu_context_unsilent_event))
                            .setIcon(android.R.drawable.ic_menu_revert);

                } else if (!eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {
                    menuItem = menu.add(Menu.NONE, Constants.ContextMenu_SilentEvent, Menu.NONE, getString(R.string.menu_context_silent_event))
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

            if (selectedEvent[ContactsEvents.Position_eventType].equals(Constants.EventType_Anniversary) ) {
                menu.add(Menu.NONE, Constants.ContextMenu_AnniversaryList, Menu.NONE, getString(R.string.menu_context_anniversary_list))
                        .setIcon(android.R.drawable.ic_menu_info_details);
            }

            if (!selectedEvent[ContactsEvents.Position_age].equals(Constants.STRING_MINUS1)) {
                if (!eventsData.isXDaysEvent(eventKey)) {
                    if (eventsData.isFeatureEnabled(Constants.FEATURE_ADV_ACTIONS) && !eventSubtype.equals(Constants.EventType_5K)) {
                        menuItem = menu.add(Menu.NONE, Constants.ContextMenu_xDaysEvent, Menu.NONE, getString(R.string.menu_context_xDaysEvent_add))
                                .setIcon(android.R.drawable.ic_menu_myplaces);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            menuItem.setIconTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                        }
                    }
                } else {
                    menu.add(Menu.NONE, Constants.ContextMenu_xDaysEvent, Menu.NONE, getString(R.string.menu_context_xDaysEvent_edit))
                            .setIcon(android.R.drawable.ic_menu_myplaces);
                }
            }

            if (isUnrecognizedEvent(selectedEvent) && !Constants.STRING_EMPTY.equals(selectedEvent[ContactsEvents.Position_eventLabel])) {
                menu.add(Menu.NONE, Constants.ContextMenu_SetEvenType, Menu.NONE, getString(R.string.menu_context_set_event_type))
                        .setIcon(android.R.drawable.ic_menu_mylocation);
            }

            if (eventsData.isFeatureEnabled(Constants.FEATURE_ADV_ACTIONS)) {
                menu.add(Menu.NONE, Constants.ContextMenu_EventInfo, Menu.NONE, getString(R.string.menu_context_event_info))
                        .setIcon(android.R.drawable.ic_menu_view);
            }

            popupMenu.setOnMenuItemClickListener(item -> onPopupMenuItemSelected(listView, contactID, eventURLs, eventKey, eventKeyWithRawId, item));
            popupMenu.show();
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return false;
        }
    }

    private boolean onPopupMenuItemSelected(ListView listView, String contactID, String[] eventURLs, String eventKey, String eventKeyWithRawId, MenuItem item) {
        try {

            int itemId = item.getItemId();

            if (itemId == Constants.ContextMenu_EditLocalEvent) {

                Intent intent = new Intent(this, LocalEventActivity.class);
                intent.setAction(Intent.ACTION_EDIT);
                intent.putExtra(Constants.EXTRA_EVENT_DATA, selectedEvent[ContactsEvents.Position_eventID]);
                try {
                    startActivityForResult(intent, Constants.RESULT_EDIT_EVENT);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.ContextMenu_CreateFromEventLocalEvent) {

                Intent intent = new Intent(this, LocalEventActivity.class);
                intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                intent.putExtra(Constants.EXTRA_EVENT_DATA, selectedEvent[ContactsEvents.Position_eventID]);
                try {
                    startActivityForResult(intent, Constants.RESULT_EDIT_EVENT);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.ContextMenu_EditContact) {

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
                createContactIntent.putExtra(ContactsContract.Intents.Insert.NAME, selectedEvent[ContactsEvents.Position_personFullName]);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, selectedEvent[ContactsEvents.Position_title]);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.COMPANY, selectedEvent[ContactsEvents.Position_organization]);
                createContactIntent.putExtra(ContactsContract.Intents.Insert.NOTES, selectedEvent[ContactsEvents.Position_eventDateFirstTime]);

                try {
                    startActivity(createContactIntent);
                } catch (ActivityNotFoundException ignored) { /**/ }
                return true;

            } else if (itemId == Constants.ContextMenu_OpenCalendar) {

                Uri selectedEventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, StringUtils.parseToLong(selectedEvent[ContactsEvents.Position_eventID]));
                Intent openCalendarIntent = new Intent(Intent.ACTION_VIEW).setData(selectedEventUri);
                try {
                    if (openCalendarIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(openCalendarIntent);
                    }
                } catch (ActivityNotFoundException ignored) { /**/ }
                return true;

            } else if (itemId == Constants.ContextMenu_OpenURL) {

                int groupId = item.getGroupId();
                //todo: доделать для несколько ссылок String[] eventURLs = selectedEvent[ContactsEvents.Position_eventURL].trim().split(Constants.STRING_2TILDA);
                if (eventURLs.length >= groupId) {
                    try {
                        String url = eventURLs[groupId].trim();
                        if (!url.startsWith("http")) {
                            url = "https://".concat(url);
                        }
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (ActivityNotFoundException e) { /**/ }
                }

            } else if (itemId == Constants.ContextMenu_EventInfo) {

                showEventDetails();
                return true;

            } else if (itemId == Constants.ContextMenu_AddToFavorites) {

                if (eventsData.setFavoriteEvent(eventKey, eventKeyWithRawId)) {
                    this.invalidateOptionsMenu();
                    filterEventsList();
                    drawList();
                    eventsData.updateWidgets(0, null);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_RemoveFromFavorites) {

                if (eventsData.unsetFavoriteEvent(eventKey, eventKeyWithRawId)) {
                    this.invalidateOptionsMenu();
                    filterEventsList();
                    drawList();
                    eventsData.updateWidgets(0, null);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_HideEvent) {

                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                builder.setTitle(getString(R.string.msg_title_confirmation));
                builder.setIcon(android.R.drawable.ic_menu_help);
                builder.setMessage(getString(R.string.msg_event_hide_confirmation));
                builder.setPositiveButton(R.string.button_yes, (dialog, which) -> {
                    if (eventsData.setHiddenEvent(eventKey, eventKeyWithRawId)) {
                        if (eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {
                            //Если скрываем - убираем из списка без уведомления
                            eventsData.unsetSilencedEvent(eventKey, eventKeyWithRawId);
                        }
                        this.invalidateOptionsMenu();
                        filterEventsList();
                        drawList();
                        eventsData.updateWidgets(0, null);
                    }
                    dialog.dismiss();
                });
                builder.setNegativeButton(R.string.button_no, (dialog, which) -> dialog.dismiss());
                AlertDialog alertToShow = builder.create();
                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                });
                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
                return true;

            } else if (itemId == Constants.ContextMenu_UnhideEvent) {

                if (eventsData.unsetHiddenEvent(eventKey, eventKeyWithRawId)) {

                    // Если удалили последнего скрытого и дополнительные функции выключены - используем для показа все источники
                    if (eventsData.getHiddenEventsCount() == 0 && !eventsData.preferences_list_EventSources.isEmpty()) {
                        eventsData.preferences_list_EventSources.clear();
                        eventsData.savePreferences();
                    }

                    this.invalidateOptionsMenu();
                    filterEventsList();
                    drawList();
                    eventsData.updateWidgets(0, null);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_Remind_1H) {

                eventsData.snoozeNotification(selectedEvent_str, null, null, 1, null);
                return true;

            } else if (itemId == Constants.ContextMenu_Remind_Morning) {

                Calendar wakeTime = Calendar.getInstance();
                wakeTime.add(Calendar.DAY_OF_MONTH, 1);
                wakeTime.set(Calendar.HOUR_OF_DAY, 9);
                wakeTime.set(Calendar.MINUTE, 0);
                wakeTime.set(Calendar.SECOND, 0);
                wakeTime.set(Calendar.MILLISECOND, 0);

                eventsData.snoozeNotification(selectedEvent_str, null, null, 0, wakeTime.getTime());
                return true;

            } else if (itemId == Constants.ContextMenu_AnniversaryList) {

                eventsData.showAnniversaryList(this, selectedEvent[ContactsEvents.Position_age]);
                return true;

            } else if (itemId == Constants.ContextMenu_SilentEvent) {

                if (eventsData.setSilencedEvent(eventKey, eventKeyWithRawId)) {
                    this.invalidateOptionsMenu();
                    filterEventsList();
                    drawList();
                    eventsData.updateWidgets(0, null);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_UnsilentEvent) {

                if (eventsData.unsetSilencedEvent(eventKey, eventKeyWithRawId)) {
                    this.invalidateOptionsMenu();
                    filterEventsList();
                    drawList();
                    eventsData.updateWidgets(0, null);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_MergeEvent) {

                //https://developer.android.com/guide/components/intents-common#PickContact
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                try {
                    startActivityForResult(intent, Constants.RESULT_PICK_CONTACT);
                } catch (ActivityNotFoundException ignored) { /**/ }

            } else if (itemId == Constants.ContextMenu_UnmergeEvent) {

                if (eventsData.setMergedID(selectedEvent[ContactsEvents.Position_eventID], null, null)) {
                    this.invalidateOptionsMenu();
                    filterEventsList();
                    drawList();
                    eventsData.updateWidgets(0, null);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_RemergeEvent) {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                try {
                    startActivityForResult(intent, Constants.RESULT_PICK_OTHER_CONTACT);
                } catch (ActivityNotFoundException ignored) { /**/ }
                return true;

            } else if (itemId == Constants.ContextMenu_ShareAsImage) {

                shareEventAsImage(listView);
                return true;

            } else if (itemId == Constants.ContextMenu_ShareAsText) {

                shareEventAsText(contactID);
                return true;

            } else if (itemId == Constants.ContextMenu_xDaysEvent) {

                showDialogXDaysCounter(adapter.getView(selectedEvent_num, null, listView), selectedEvent);
                return true;

            } else if (itemId == Constants.ContextMenu_SetEvenType) {

                if (Constants.STRING_EMPTY.equals(selectedEvent[ContactsEvents.Position_eventLabel])) {
                    ToastExpander.showInfoMsg(this, getString(R.string.msg_event_type_label_absent));
                    return true;
                }

                selectEventTypeForEvent();
                return true;

            } else if (itemId == Constants.ContextMenu_SetEventWithoutYear) {

                final String eventKeyId = selectedEvent[ContactsEvents.Position_eventID]
                        + Constants.STRING_2HASH + selectedEvent[ContactsEvents.Position_eventSubType];
                if (eventsData.setEventWithoutYear(eventKeyId)) {
                    eventsData.needUpdateEventList = true;
                    updateList(true);
                }
                return true;

            } else if (itemId == Constants.ContextMenu_UnsetEventWithoutYear) {

                final String eventKeyId = selectedEvent[ContactsEvents.Position_eventID]
                        + Constants.STRING_2HASH + selectedEvent[ContactsEvents.Position_eventSubType];
                if (eventsData.unsetEventWithoutYear(eventKeyId)) {
                    eventsData.needUpdateEventList = true;
                    updateList(true);
                }
                return true;

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return false;
    }

    private void showEventDetails() {
        try {

            final String eventInfo = StringUtils.getEventDataAsString(selectedEvent, eventsData.getResources());
            final String eventSubType = selectedEvent[ContactsEvents.Position_eventSubType];
            int roundingFactor;
            if (eventSubType.equals(Constants.EventType_Calendar) || eventSubType.equals(Constants.EventType_File)) {
                roundingFactor = 1;
            } else {
                roundingFactor = eventsData.preferences_list_photostyle;
            }

            TextView customTitle = new TextView(this);
            customTitle.setText(StringUtils.getFullName(selectedEvent, eventsData.preferences_name_format));
            customTitle.setTextIsSelectable(true);
            customTitle.setSingleLine(false);
            customTitle.setEllipsize(null);
            customTitle.setMaxLines(2);

            TypedValue tv = new TypedValue();
            this.getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, tv, true);
            TextViewCompat.setTextAppearance(customTitle, tv.resourceId);

            int iconSizePx = (int) (48 * getResources().getDisplayMetrics().density);
            Drawable icon = new BitmapDrawable(resources, ContactsEvents.getInstance().getEventPhoto(selectedEvent_str, true, false, false, roundingFactor));
            icon.setBounds(0, 0, iconSizePx, iconSizePx);
            customTitle.setCompoundDrawablesRelative(icon, null, null, null);
            customTitle.setCompoundDrawablePadding((int) (16 * getResources().getDisplayMetrics().density));

            int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
            customTitle.setPadding(paddingPx, paddingPx, paddingPx, 0);

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCustomTitle(customTitle)
                    .setMessage(eventInfo)
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.dismiss());
            AlertDialog alertToShow = builder.create();

            Window window = alertToShow.getWindow();
            Context themeContext = new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog);
            int textColor = ImageUtils.resolveThemeColor(themeContext, android.R.attr.textColor);
            if (window != null) {
                int dialogBgColor = ImageUtils.resolveThemeColor(themeContext, android.R.attr.background);
                if (dialogBgColor != 0) {
                    window.setBackgroundDrawable(new ColorDrawable(dialogBgColor));
                }
            }

            alertToShow.setOnShowListener(arg0 -> {

                customTitle.setTextColor(textColor);
                TextView messageView = alertToShow.findViewById(android.R.id.message);
                if (messageView != null) {
                    messageView.setTextColor(textColor);
                    messageView.setTextIsSelectable(true);
                }

                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
            });
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void shareEventAsImage(ListView listView) {
        try {

            //https://stackoverflow.com/questions/12742343/android-get-screenshot-of-all-listview-items
            //https://demonuts.com/android-take-screenshot/
            //https://stackoverflow.com/questions/19514174/convert-listview-items-into-a-single-bitmap-image

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
                ToastExpander.showInfoMsg(this, getString(R.string.msg_error_get_event_image));
                return;
            }

            Uri bitmapShareUri;

            File file = new File(this.getCacheDir(), "event.jpg");
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();
                bitmapShareUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                return;
            }
            childView.destroyDrawingCache();

            if (bitmapShareUri != null) {
                //https://stackoverflow.com/questions/48045626/chooser-created-with-createchooserintent-title-doesnt-display-a-title
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(Constants.MIME_IMAGE_ALL);
                final String[] mimeTypes = {Constants.MIME_IMAGE_JPEG, Constants.MIME_IMAGE_PNG};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes); //https://stackoverflow.com/questions/54478638/effect-of-intent-settype-on-androids-intent-chooser
                intent.putExtra(Intent.EXTRA_STREAM, bitmapShareUri);

                // ClipData для preview в chooser'е
                intent.setClipData(ClipData.newRawUri("", bitmapShareUri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Грант права приложениям-получателям
                List<ResolveInfo> resInfoList = getPackageManager()
                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, bitmapShareUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                }
                try {
                    Intent chooser = Intent.createChooser(intent, Constants.STRING_EMPTY);
                    startActivity(chooser);
                } catch (ActivityNotFoundException e) { /**/ }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void shareEventAsText(String contactID) {
        try {

            StringBuilder textBig = new StringBuilder();
            textBig
                    .append(StringUtils.getFullName(selectedEvent, eventsData.preferences_name_format))
                    .append(Constants.STRING_EOL)
                    .append(selectedEvent[ContactsEvents.Position_eventEmoji])
                    .append(Constants.STRING_SPACE)
                    .append(selectedEvent[ContactsEvents.Position_eventDateNextTime])
                    .append(Constants.STRING_SPACE)
                    .append(selectedEvent[ContactsEvents.Position_eventCaption]);
            if (!TextUtils.isEmpty(selectedEvent[ContactsEvents.Position_age_caption].trim()))
                textBig
                        .append(Constants.STRING_COLON_SPACE)
                        .append(selectedEvent[ContactsEvents.Position_age_caption]);

            if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Age))) {
                String eventSubType = selectedEvent[ContactsEvents.Position_eventSubType];

                if (eventSubType.equals(Constants.EventType_BirthDay) || eventSubType.equals(Constants.EventType_5K)) { //Если это день рождения или 5K
                    final String currentAge = selectedEvent[ContactsEvents.Position_age_current];
                    if (!currentAge.isEmpty() && !currentAge.startsWith(Constants.STRING_0)) {
                        if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                        if (eventsData.deathDatesForIds.containsKey(selectedEvent[ContactsEvents.Position_contactID])) { //Но есть годовщина смерти
                            textBig.append(getString(R.string.msg_age_could_be_now));
                        } else {
                            textBig.append(getString(R.string.msg_age_now));
                        }
                        textBig.append(currentAge);
                    }
                } else if (eventsData.birthdayDatesForIds.containsKey(contactID)) {
                    Date birthDate = eventsData.birthdayDatesForIds.get(contactID);
                    Date eventDate = null;
                    if (eventSubType.equals(Constants.EventType_Death)) { //Если это годовщина смерти
                        try {
                            eventDate = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).parse(selectedEvent[ContactsEvents.Position_eventDateFirstTime]);
                        } catch (ParseException ignored) { /**/ }
                        if (eventDate != null && birthDate != null) {
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            textBig.append(getString(R.string.msg_age_was)).append(AppDateUtils.countDaysDiffText(birthDate, eventDate, 3, eventsData.getResources(), eventsData.currentLocale));
                        }
                    } else { //Другие события
                        try {
                            eventDate = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).parse(selectedEvent[ContactsEvents.Position_eventDateNextTime]);
                        } catch (ParseException ignored) { /**/ }
                        if (eventDate != null && birthDate != null) {
                            Date today = AppDateUtils.getWithoutTime(Calendar.getInstance()).getTime();
                            if (textBig.length() > 0) textBig.append(Constants.STRING_EOL);
                            if (eventsData.deathDatesForIds.containsKey(contactID)) { //Но есть годовщина смерти
                                textBig.append(getString(R.string.msg_age_could_be));
                            } else if (eventDate.compareTo(today) == 0) {
                                textBig.append(getString(R.string.msg_age_now));
                            } else {
                                textBig.append(getString(R.string.msg_age_will_be));
                            }
                            textBig.append(AppDateUtils.countDaysDiffText(birthDate, eventDate, 3, eventsData.getResources(), eventsData.currentLocale));
                        }
                    }
                }
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            intent.putExtra(Intent.EXTRA_TEXT, textBig.toString());
            startActivity(Intent.createChooser(intent, null));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void selectEventTypeForEvent() {
        try {

            List<String> eventNames = new ArrayList<>();
            List<Integer> eventIcons = new ArrayList<>();

            eventNames.add(getString(R.string.event_type_birthday));
            eventIcons.add(R.drawable.ic_event_birthday);
            eventNames.add(getString(R.string.event_type_anniversary));
            eventIcons.add(R.drawable.ic_event_wedding);
            eventNames.add(getString(R.string.event_type_nameday));
            eventIcons.add(R.drawable.ic_event_nameday);
            eventNames.add(getString(R.string.event_type_crowning));
            eventIcons.add(R.drawable.ic_event_crowning);
            eventNames.add(getString(R.string.event_type_death));
            eventIcons.add(R.drawable.ic_event_death);
            eventNames.add(getString(R.string.event_type_other));
            eventIcons.add(R.drawable.ic_event_other);
            eventNames.add(getString(R.string.event_type_holiday));
            eventIcons.add(R.drawable.ic_event_holiday);
            eventNames.add(eventsData.preferences_customevent1_caption.isEmpty() ? getString(R.string.event_type_custom) : eventsData.preferences_customevent1_caption);
            eventIcons.add(R.drawable.ic_event_custom1);
            eventNames.add(eventsData.preferences_customevent2_caption.isEmpty() ? getString(R.string.event_type_custom) : eventsData.preferences_customevent2_caption);
            eventIcons.add(R.drawable.ic_event_custom2);
            eventNames.add(eventsData.preferences_customevent3_caption.isEmpty() ? getString(R.string.event_type_custom) : eventsData.preferences_customevent3_caption);
            eventIcons.add(R.drawable.ic_event_custom3);
            eventNames.add(eventsData.preferences_customevent4_caption.isEmpty() ? getString(R.string.event_type_custom) : eventsData.preferences_customevent4_caption);
            eventIcons.add(R.drawable.ic_event_custom4);
            eventNames.add(eventsData.preferences_customevent5_caption.isEmpty() ? getString(R.string.event_type_custom) : eventsData.preferences_customevent5_caption);
            eventIcons.add(R.drawable.ic_event_custom5);

            ListAdapter adapter = new ImageSelectAdapter(this, eventNames, eventIcons, ImageSelectAdapter.Scale.SQUARED, ta);

            AlertDialog.Builder builderForEventTypeDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.msg_event_type_select_title)
                    .setAdapter(adapter, null)
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setCancelable(true);

            AlertDialog alertForEventTypeDialog = builderForEventTypeDialog.create();

            ListView eventList = alertForEventTypeDialog.getListView();
            eventList.setItemsCanFocus(false);
            eventList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            eventList.setOnItemClickListener((parent, view, position, id) -> {

                final String[] result = {null};
                if (position > 5) {
                    int needConfirmEventTitleIcon = 0;
                    if (position == 7 && eventsData.preferences_customevent1_caption.isEmpty()) {
                        needConfirmEventTitleIcon = R.drawable.ic_event_custom1;
                    } else if (position == 8 && eventsData.preferences_customevent2_caption.isEmpty()) {
                        needConfirmEventTitleIcon = R.drawable.ic_event_custom2;
                    } else if (position == 9 && eventsData.preferences_customevent3_caption.isEmpty()) {
                        needConfirmEventTitleIcon = R.drawable.ic_event_custom3;
                    } else if (position == 10 && eventsData.preferences_customevent4_caption.isEmpty()) {
                        needConfirmEventTitleIcon = R.drawable.ic_event_custom4;
                    } else if (position == 11 && eventsData.preferences_customevent5_caption.isEmpty()) {
                        needConfirmEventTitleIcon = R.drawable.ic_event_custom5;
                    }
                    if (needConfirmEventTitleIcon > 0) {

                        final EditText editText = new EditText(MainActivity.this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        editText.setLayoutParams(lp);
                        editText.setText(selectedEvent[ContactsEvents.Position_eventLabel]);
                        editText.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                        editText.setHintTextColor(ta.getColor(R.styleable.Theme_dialogHintColor, 0));
                        editText.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, displayMetrics));

                        AlertDialog.Builder builderForEventTitleDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                                .setTitle(R.string.pref_CustomEvents_Custom_Caption_title)
                                .setIcon(needConfirmEventTitleIcon)
                                .setView(editText)
                                .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                                    result[0] = eventsData.addLabelToEventType((int) id, selectedEvent[ContactsEvents.Position_eventLabel], editText.getText().toString());
                                    dialog.dismiss();
                                })
                                .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                                .setCancelable(true);

                        if (eventsData.preferences_theme.themeEditText != 0) {
                            builderForEventTitleDialog.getContext().setTheme(eventsData.preferences_theme.themeEditText);
                        } else {
                            builderForEventTitleDialog.getContext().setTheme(ContactsEvents.themeEditText_default);
                        }

                        AlertDialog alertForEventTitleDialog = builderForEventTitleDialog.create();

                        alertForEventTitleDialog.setOnDismissListener(listener -> {
                            if (result[0] != null) {
                                ToastExpander.showInfoMsg(MainActivity.this, result[0]);
                                eventsData.getPreferences();
                                updateList(true);
                            }
                        });

                        alertForEventTitleDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        alertForEventTitleDialog.show();

                    } else {
                        result[0] = eventsData.addLabelToEventType((int) id, selectedEvent[ContactsEvents.Position_eventLabel], null);
                    }

                } else {
                    result[0] = eventsData.addLabelToEventType((int) id, selectedEvent[ContactsEvents.Position_eventLabel], null);
                }

                alertForEventTypeDialog.dismiss();
                if (result[0] != null && !result[0].isEmpty()) {
                    ToastExpander.showInfoMsg(MainActivity.this, result[0]);
                    eventsData.getPreferences();
                    updateList(true);
                }
            });

            alertForEventTypeDialog.setOnShowListener(arg0 -> alertForEventTypeDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));

            alertForEventTypeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertForEventTypeDialog.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void showDialogXDaysCounter(@NonNull View selectedEventView, @NonNull String[] selectedEvent) {
        try {

            String eventKey = eventsData.getEventKey(selectedEvent);
            boolean isEdit = eventsData.isXDaysEvent(eventKey);

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setPositiveButton(R.string.button_ok, null)
                    .setNegativeButton(R.string.button_cancel, null);

            if (isEdit) builder.setNeutralButton(R.string.button_remove, null);

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
                        ta.getColor(R.styleable.Theme_borderCardColor, ContextCompat.getColor(this, R.color.light_gray_darker)));
                drawableBack.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, displayMetrics));
                entryLayout.setBackground(drawableBack);
            }

            //Иконка и заголовок
            ImageView icon = view.findViewById(R.id.icon);
            if (icon != null) icon.setImageBitmap(ImageUtils.getBitmap(this, android.R.drawable.ic_menu_myplaces));
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
            if (entryDayDistanceTextView != null) entryDayDistanceTextView.setVisibility(View.GONE);

            TextView entryDetailsCounter = view.findViewById(R.id.entryDetailsCounter);
            if (entryDetailsCounter != null) entryDetailsCounter.setVisibility(View.GONE);

            //Данные счётчика
            List<String> eventData = eventsData.getXDaysEvent(eventKey);

            final TextView captionNearestEvents = view.findViewById(R.id.captionNearestEvents);
            final TextView listNearestEvents = view.findViewById(R.id.listNearestEvents);
            final EditText valuesPeriods = view.findViewById(R.id.repeats_values);
            final EditText valuesTimes = view.findViewById(R.id.times_values);

            if (valuesTimes != null) {
                valuesTimes.setText(eventData.get(1));

                valuesTimes.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /**/ }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { /**/ }

                    @Override
                    public void afterTextChanged(Editable s) {
                        valuesPeriods.setText(valuesPeriods.getText().toString());
                    }
                });
            }

            if (valuesPeriods != null) {
                valuesPeriods.requestFocus();
                valuesPeriods.setSingleLine(false);
                if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

                valuesPeriods.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /**/ }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { /**/ }

                    @Override
                    public void afterTextChanged(Editable s) {
                        final String valuePeriods = valuesPeriods.getText().toString().trim();

                        if (valuePeriods.length() < 2) {
                            if (captionNearestEvents != null) captionNearestEvents.setVisibility(View.GONE);
                            if (listNearestEvents != null) listNearestEvents.setVisibility(View.GONE);
                            return;
                        }

                        ArrayList<ContactsEvents.Event> events = null;
                        try {
                            Date eventDate = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).parse(selectedEvent[ContactsEvents.Position_eventDateFirstTime]);
                            if (eventDate != null) {
                                Calendar dateEnd = AppDateUtils.getWithoutTime(Calendar.getInstance());
                                dateEnd.add(Calendar.YEAR, 15);
                                int toRepeat = 8;
                                try {
                                    if (valuesTimes != null && !valuesTimes.getText().toString().isEmpty()) {
                                        toRepeat = - Integer.parseInt(valuesTimes.getText().toString());
                                    }
                                } catch (NumberFormatException e) { /**/ }
                                events = ContactsEvents.getInstance().getNextRepeatsForEvent(
                                        eventsData.getToday(),
                                        dateEnd,
                                        AppDateUtils.getCalendarFromDate(eventDate),
                                        valuePeriods,
                                        toRepeat
                                );
                            }
                        } catch (ParseException e) { /**/ }

                        if (events == null || events.isEmpty()) {
                            if (captionNearestEvents != null) captionNearestEvents.setVisibility(View.GONE);
                            if (listNearestEvents != null) listNearestEvents.setVisibility(View.GONE);
                        } else {
                            if (captionNearestEvents != null) captionNearestEvents.setVisibility(View.VISIBLE);
                            if (listNearestEvents != null) {
                                StringBuilder sb = new StringBuilder();
                                for (ContactsEvents.Event e: events) {
                                    if (sb.length() > 0) sb.append(Constants.STRING_EOL);
                                    sb.append(AppDateUtils.getDateFormatted(Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get())
                                            .format(e.date), ContactsEvents.FormatDate.WithYear, eventsData.preferences_date_format,
                                            eventsData.getContext(), eventsData.getResources(), eventsData.currentLocale));
                                    sb.append(Constants.STRING_COLON_SPACE);
                                    sb.append(e.distance);
                                }
                                listNearestEvents.setText(sb.toString());
                                listNearestEvents.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });

                valuesPeriods.setText(eventData.get(0));
                valuesPeriods.setSelection(valuesPeriods.getText().length());
            }

            dialog.setOnShowListener(arg0 -> {
                final Button buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(v -> {
                    final String valueRepeats = valuesPeriods != null ? valuesPeriods.getText().toString().trim() : Constants.STRING_EMPTY;
                    final String valueTimes = valuesTimes != null ? valuesTimes.getText().toString().trim() : Constants.STRING_EMPTY;
                    boolean result;
                    if (valueRepeats.isEmpty()) {
                        result = eventsData.setXDaysEvent(eventKey, null);
                    } else {
                        result = eventsData.setXDaysEvent(eventKey, valueRepeats + Constants.STRING_BAR + valueTimes);
                    }
                    dialog.dismiss();
                    if (result) updateList(true);
                });
                buttonPositive.setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                final Button buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(v -> dialog.dismiss());
                buttonNegative.setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                if (isEdit) {
                    final Button buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                    buttonNeutral.setOnClickListener(v -> {
                        boolean result = eventsData.setXDaysEvent(eventKey, null);
                        dialog.dismiss();
                        if (result) updateList(true);
                    });
                    buttonNeutral.setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                }

                final View buttonBar = (View) buttonPositive.getParent();
                buttonBar.setBackgroundColor(ta.getColor(R.styleable.Theme_editTextBackgroundCustom, 0));

            });
            dialog.show();
            if (dialog.getWindow() != null)
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setMenuIconsVisible(Menu menu) {
        //https://stackoverflow.com/a/43411336/4928833
        //https://stackoverflow.com/questions/48607853/menubuilder-setoptionaliconsvisible-can-only-be-called-from-within-the-same-libr
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
            builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                try {
                    startActivity(new Intent(this, SettingsActivity.class));
                } catch (ActivityNotFoundException e) { /**/ }
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
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
                } catch (ActivityNotFoundException e) { /**/ }
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
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
                            builder.setMessage(HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                            builder.setNeutralButton(R.string.button_open_version_history, (dialog, which) -> {
                                try {
                                    startActivity(new Intent(this, AboutActivity.class));
                                } catch (ActivityNotFoundException e) { /**/ }
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

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.msg_welcome_title));
                    builder.setIcon(android.R.drawable.ic_menu_info_details);
                    builder.setCancelable(false);
                    builder.setMessage(getString(R.string.msg_welcome_text));
                    builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
                        dialog.cancel();
                        if (DeviceTools.checkNoContactsAccess(eventsData.getContext())) {
                            //https://developer.android.com/training/permissions/requesting.html#java
                            swipeRefresh.setEnabled(false);
                            swipeRefresh.setRefreshing(false);
                            setHint(StringUtils.getHTMLColor(getString(R.string.msg_no_access_contacts).toLowerCase(), Constants.HTML_COLOR_RED, eventsData.getContext()));
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.GET_ACCOUNTS}, Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                        }
                    });
                    builder.setNeutralButton(R.string.button_open_app_settings, (dialog, which) -> {
                        try {
                            startActivity(new Intent(this, SettingsActivity.class));
                        } catch (ActivityNotFoundException e) { /**/ }
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
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setLastRunVersion() {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getString(R.string.pref_Version_LastRun), BuildConfig.VERSION_NAME);
            editor.putString(getString(R.string.pref_VersionCode_LastRun), Integer.toString(BuildConfig.VERSION_CODE));
            editor.apply();

            ToastExpander.showDebugMsg(this, String.format(getString(R.string.msg_version), BuildConfig.VERSION_NAME));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return -1;
        }
    }

    public void onRefresh() {
        if (swipeRefreshListener == null) return;
        try {
            swipeRefreshListener.onRefresh();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onStart() {
        try {
            super.onStart();
            if (eventsData != null) {
                eventsData.isUIOpen = true;
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
                                    ToastExpander.showInfoMsg(this, getString(R.string.msg_jump_to_event_error));
                                    return;
                                }
                            }

                            listView.setSelectionFromTop(jumpToEvent, listView.getTop() + listView.getPaddingTop());
                        }, 200);
                    }
                }
                intent.setAction(Constants.STRING_EMPTY);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            eventsData.isUIOpen = false;
            eventsData.coordinator = null;
        }
        ToastExpander.getInstance().dismissSnackBar();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) executor.shutdown();
        if (ta != null) ta.recycle();
        super.onDestroy();
    }

    @SuppressLint({"RestrictedApi", "AlwaysShowAction"})
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {

            setMenuIconsVisible(menu);

            MenuItem menuItem;
            menuItem = menu.add(Menu.NONE, Constants.MainMenu_Search, Menu.NONE, R.string.menu_search).setIcon(android.R.drawable.ic_menu_search)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                    .setActionView(new SearchView(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themePopup)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                menuItem.setContentDescription(getString(R.string.hint_SearchPanel));
            }
            menu.add(Menu.NONE, Constants.MainMenu_Refresh, Menu.NONE, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate);
            SubMenu subMenuFile = menu.addSubMenu(Menu.NONE, Constants.MainMenu_AddEvent, Menu.NONE, R.string.menu_add_event).setIcon(android.R.drawable.ic_menu_add);
            subMenuFile.add(Menu.NONE, Constants.MainMenu_AddEvent_Local, Menu.NONE, R.string.menu_add_event_to_local)
                    .setIcon(android.R.drawable.ic_menu_add);
            subMenuFile.add(Menu.NONE, Constants.MainMenu_AddEvent_ToContact, Menu.NONE, R.string.menu_add_event_to_contact)
                    .setIcon(android.R.drawable.ic_menu_my_calendar)
                    .setTitleCondensed(getString(R.string.menu_add_event_to_contact_short));
            subMenuFile.add(Menu.NONE, Constants.MainMenu_AddEvent_ToCalendar, Menu.NONE, R.string.menu_add_event_to_calendar)
                    .setIcon(android.R.drawable.ic_menu_month)
                    .setTitleCondensed(getString(R.string.menu_add_event_to_calendar_short));
            subMenuFile.add(Menu.NONE, Constants.MainMenu_AddEvent_OpenFile, Menu.NONE, R.string.menu_open_file_with_events)
                    .setIcon(android.R.drawable.ic_menu_save)
                    .setTitleCondensed(getString(R.string.menu_open_file_with_events_short));
            menu.add(Menu.NONE, Constants.MainMenu_Settings, Menu.NONE, R.string.menu_settings).setIcon(R.drawable.ic_sysbar_quicksettings);
            menu.add(Menu.NONE, Constants.MainMenu_Quiz, Menu.NONE, R.string.menu_quiz).setIcon(android.R.drawable.ic_menu_compass);
            menu.add(Menu.NONE, Constants.MainMenu_Filter, Menu.NONE, R.string.menu_filter_events).setIcon(android.R.drawable.ic_menu_view);
            menu.add(Menu.NONE, Constants.MainMenu_EventsSources, Menu.NONE, R.string.menu_events_sources).setIcon(android.R.drawable.ic_menu_agenda);
            menu.add(Menu.NONE, Constants.MainMenu_EventsTypes, Menu.NONE, R.string.menu_events_types).setIcon(R.drawable.ic_menu_copy);
            menu.add(Menu.NONE, Constants.MainMenu_Hints, Menu.NONE, R.string.menu_hints).setIcon(android.R.drawable.ic_menu_info_details);

            //Быстрое действие
            if (eventsData.preferences_list_quick_action > 0) {
                menuItem = menu.findItem(eventsData.preferences_list_quick_action);
                if (menuItem != null) {
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
            }

            final boolean isItemQuizVisible = !eventsData.isEmptyEventList()
                    && (eventsData.isFeatureEnabled(Constants.FEATURE_QUIZ) || eventsData.preferences_list_quick_action == Constants.MainMenu_Quiz);
            //показывать, если есть события или выбран фильтр
            final boolean isItemFilterVisible = eventsData != null && !eventsData.isEmptyEventList() &&
                    (
                            (eventsData.getHiddenEventsCount() > 0 || eventsData.getSilencedEventsCount() > 0)
                                    || (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_All && eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_NotHidden)
                                    || statsUnrecognizedEvents > 0
                                    || eventsData.statFavoriteEventsCount > 0
                    );
            final boolean isItemSourcesVisible = eventsData != null
                    && (eventsData.isFeatureEnabled(Constants.FEATURE_SELECT_SOURCES) || eventsData.preferences_list_quick_action == Constants.MainMenu_EventsSources);
            final boolean isItemTypesVisible = eventsData != null
                    && (eventsData.isFeatureEnabled(Constants.FEATURE_ADV_ACTIONS) || eventsData.preferences_list_quick_action == Constants.MainMenu_EventsTypes);

            MenuItem searchItem = menu.findItem(Constants.MainMenu_Search);
            if (searchItem != null) {
                SearchView searchView = (SearchView) searchItem.getActionView();
                searchItem.setVisible(!eventsData.isEmptyEventList());
                if (searchView != null) {
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
                }

                searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

                    @Override
                    public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                        try {
                            menu.findItem(Constants.MainMenu_AddEvent).setVisible(false);
                            menu.findItem(Constants.MainMenu_Refresh).setVisible(false);
                            menu.findItem(Constants.MainMenu_Settings).setVisible(false);
                            menu.findItem(Constants.MainMenu_Quiz).setVisible(false);
                            menu.findItem(Constants.MainMenu_Filter).setVisible(false);
                            menu.findItem(Constants.MainMenu_EventsSources).setVisible(false);
                            menu.findItem(Constants.MainMenu_EventsTypes).setVisible(false);
                            menu.findItem(Constants.MainMenu_Hints).setVisible(true).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            ToastExpander.showDebugMsg(getApplicationContext(), StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                        }
                        return true;
                    }

                    //работает, только если showAsAction="always" https://stackoverflow.com/questions/9327826/searchviews-oncloselistener-doesnt-work/18186164
                    @Override
                    public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                        try {
                            menu.findItem(Constants.MainMenu_AddEvent).setVisible(true);
                            menu.findItem(Constants.MainMenu_Refresh).setVisible(true);
                            menu.findItem(Constants.MainMenu_Settings).setVisible(true);
                            menu.findItem(Constants.MainMenu_Quiz).setVisible(isItemQuizVisible);
                            menu.findItem(Constants.MainMenu_Filter).setVisible(isItemFilterVisible);
                            menu.findItem(Constants.MainMenu_EventsSources).setVisible(isItemSourcesVisible);
                            menu.findItem(Constants.MainMenu_EventsTypes).setVisible(isItemTypesVisible);
                            menu.findItem(Constants.MainMenu_Hints).setVisible(false).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                            filterNames = "";
                            filterEventsList();
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            ToastExpander.showDebugMsg(getApplicationContext(), StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                        }
                        return true;
                    }

                });

                if (searchView != null) {
                    if (eventsData.preferences_list_search_depth == ContactsEvents.SearchDepth.AllEvents) {
                        searchView.setQueryHint(getString(R.string.msg_hint_search_all));
                    } else {
                        searchView.setQueryHint(getString(R.string.msg_hint_search));
                    }
                    searchView.setMaxWidth(Integer.MAX_VALUE);
                }
            }

            menu.findItem(Constants.MainMenu_AddEvent_OpenFile).setVisible(
                    !eventsData.preferences_Birthday_files.isEmpty()
                            || !eventsData.preferences_OtherEvent_files.isEmpty()
                            || !eventsData.preferences_MultiType_files.isEmpty()
                            || !eventsData.preferences_HolidayEvent_files.isEmpty());

            // https://stackoverflow.com/questions/3721963/how-to-add-calendar-events-in-android
            // https://developer.android.com/training/contacts-provider/modify-data
            // https://stackoverflow.com/questions/54475665/how-to-insert-contact-birthday-date-by-intent
            // https://stackoverflow.com/questions/20890855/adding-a-contactscontract-commondatakinds-event-to-android-contacts-does-not-sh

            menu.findItem(Constants.MainMenu_Quiz).setVisible(isItemQuizVisible);
            menu.findItem(Constants.MainMenu_Filter).setVisible(isItemFilterVisible);
            menu.findItem(Constants.MainMenu_EventsSources).setVisible(isItemSourcesVisible);
            menu.findItem(Constants.MainMenu_EventsTypes).setVisible(isItemTypesVisible);
            menu.findItem(Constants.MainMenu_Hints).setVisible(false);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            AlertDialog.Builder builder;
            AlertDialog alertToShow;

            int itemId = item.getItemId();
            if (itemId == Constants.MainMenu_Refresh) {

                //https://github.com/googlesamples/android-SwipeRefreshLayoutBasic/blob/master/Application/src/main/java/com/example/android/swiperefreshlayoutbasic/SwipeRefreshLayoutBasicFragment.java
                //https://medium.com/mobile-app-development-publication/swipe-to-refresh-not-showing-why-96b76c5c93e7
                if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {
                    swipeRefresh.postDelayed(() -> {
                        eventsData.needUpdateEventList = true;
                        eventsData.clearDaysTypesAndInfo();
                        updateList(true);
                        eventsData.updateWidgets(0, null);
                    }, 300);
                }
                return true;

            } else if (itemId == Constants.MainMenu_Settings) {

                Intent intent = new Intent(this, SettingsActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.MainMenu_Quiz) {

                //eventsData.quizCheckAndGo(null, null, MainActivity.this);
                Intent intent = new Intent(this, QuizActivity.class);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.MainMenu_Filter) {

                setFilter();
                return true;

            } else if (itemId == Constants.MainMenu_EventsSources) {

                final ContactsEvents.EventSources eventSources = eventsData.new EventSources();
                eventSources.loadEventSources(getString(R.string.pref_List_EventSources_key));
                eventsData.selectEventSources(eventSources, new ArrayList<>(eventsData.preferences_list_EventSources),
                        this,
                        selectedSources -> {
                            eventsData.preferences_list_EventSources.clear();
                            eventsData.preferences_list_EventSources.addAll(selectedSources);
                            eventsData.savePreferences();
                            eventsData.needUpdateEventList = true;
                            updateList(true);
                        });
                return true;

            } else if (itemId == Constants.MainMenu_EventsTypes) {

                selectEventsTypes();
                return true;

            } else if (itemId == Constants.MainMenu_AddEvent_Local) {

                Intent intent = new Intent(this, LocalEventActivity.class);
                intent.setAction(Intent.ACTION_INSERT);
                try {
                    startActivityForResult(intent, Constants.RESULT_EDIT_EVENT);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.MainMenu_AddEvent_ToContact) {

                Intent editIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                editIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                editIntent.putExtra(Constants.EXTRA_CONTACT_ON_SAVE_RESULT, true);
                try {
                    startActivity(editIntent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.MainMenu_AddEvent_ToCalendar) {

                // https://developer.android.com/guide/topics/providers/calendar-provider#java
                // https://stackoverflow.com/questions/20563476/how-to-add-a-calendar-event-using-intents
                // https://github.com/roomorama/Caldroid/issues/128

                Intent addEventIntent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.ALL_DAY, true)
                        .putExtra(CalendarContract.Events.RRULE, Constants.QUERY_PARAM_YEARLY);
                try {
                    startActivity(addEventIntent);
                } catch (ActivityNotFoundException e) { /**/ }
                return true;

            } else if (itemId == Constants.MainMenu_AddEvent_OpenFile) {

                openFileWithEvents();
                return true;

            } else if (itemId == Constants.MainMenu_Hints) {

                StringBuilder sb = new StringBuilder();
                String[] arrFAQ = resources.getStringArray(R.array.faq);
                if (!arrFAQ[0].isEmpty()) {

                    int countHintLines = 0;
                    String headerStart = Constants.STRING_HASH.concat(getString(R.string.menu_search));

                    for (String strLine : arrFAQ) {

                        if (countHintLines == 0 && !strLine.equals(headerStart)) continue;
                        if (!StringUtils.hasContent(strLine)) {
                            break;
                        } else if (strLine.equals(headerStart)) {
                            countHintLines++;
                        } else {
                            sb.append(Constants.HTML_BR);
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                sb.append(strLine.replace(Constants.HTML_LI, Constants.HTML_LI_ITEM));
                            } else {
                                sb.append(strLine);
                            }
                            countHintLines++;
                        }
                    }

                    builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.window_search_hints));
                    builder.setIcon(android.R.drawable.ic_menu_info_details);
                    builder.setMessage(HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                    builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
                    alertToShow = builder.create();
                    alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));
                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertToShow.show();
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFileWithEvents() {
        try {

            AlertDialog.Builder builder;
            AlertDialog alertToShow;
            //https://www.androidsnippets.com/open-any-type-of-file-with-default-intent.html
            //https://www.androidsnippets.com/open-file-with-default-application-using-intents.html

            List<String> fileNames = new ArrayList<>();
            List<String> fileURIs = new ArrayList<>();
            for (String file: eventsData.preferences_Birthday_files) {
                String[] fileDetails = file.split(Constants.REGEX_BAR);
                if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                    fileNames.add(getString(R.string.event_type_birthday_emoji) + Constants.STRING_SPACE + fileDetails[0]);
                    fileURIs.add(fileDetails[1]);
                }
            }
            for (String file: eventsData.preferences_OtherEvent_files) {
                String[] fileDetails = file.split(Constants.REGEX_BAR);
                if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                    fileNames.add(getString(R.string.event_type_other_emoji) + Constants.STRING_SPACE + fileDetails[0]);
                    fileURIs.add(fileDetails[1]);
                }
            }
            for (String file: eventsData.preferences_MultiType_files) {
                String[] fileDetails = file.split(Constants.REGEX_BAR);
                if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                    fileNames.add(getString(R.string.event_type_multi_emoji) + Constants.STRING_SPACE + fileDetails[0]);
                    fileURIs.add(fileDetails[1]);
                }
            }
            for (String file: eventsData.preferences_HolidayEvent_files) {
                String[] fileDetails = file.split(Constants.REGEX_BAR);
                if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                    fileNames.add(getString(R.string.event_type_holiday_emoji) + Constants.STRING_SPACE + fileDetails[0]);
                    fileURIs.add(fileDetails[1]);
                }
            }
            for (String file: eventsData.preferences_FactEvent_files) {
                String[] fileDetails = file.split(Constants.REGEX_BAR);
                if (!fileDetails[0].isEmpty() && !fileURIs.contains(fileDetails[1])) {
                    fileNames.add(getString(R.string.event_type_fact_emoji) + Constants.STRING_SPACE + fileDetails[0]);
                    fileURIs.add(fileDetails[1]);
                }
            }
            if (fileURIs.isEmpty()) return;

            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_CustomEvents_LocalFiles_title)
                    .setIcon(android.R.drawable.ic_menu_save)
                    .setItems(fileNames.toArray(new CharSequence[0]), (dialog, which) -> {
                        dialog.cancel();
                        Uri uri = Uri.parse(fileURIs.get(which));
                        if (uri != null) {
                            eventsData.launchIntentOnFile(uri);
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

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setFilter() {
        try {

            AlertDialog.Builder builder;
            AlertDialog alertToShow;
            List<String> filterVariants = new ArrayList<>();
            List<Integer> filterValues = new ArrayList<>();

            filterVariants.add(getString(R.string.events_scope_not_hidden, statsAllEvents - statsHiddenEvents));
            filterValues.add(Constants.pref_Events_Scope_NotHidden);

            filterVariants.add(getString(R.string.events_scope_all, statsAllEvents));
            filterValues.add(Constants.pref_Events_Scope_All);

            //Анализ на мёртвые связи
            boolean isDeadLinks = false;
            final boolean isDebugOrExtraFun = eventsData.preferences_debug_on || eventsData.isFeatureEnabled(Constants.FEATURE_ADV_INFO);

            final int hiddenEventsCount = eventsData.getHiddenEventsCount();
            if (hiddenEventsCount > 0) {
                if (statsHiddenEvents != hiddenEventsCount && isDebugOrExtraFun) {
                    filterVariants.add(getString(R.string.events_scope_hidden_dead, statsAllHiddenEvents, hiddenEventsCount));
                    isDeadLinks = true;
                } else
                    filterVariants.add(getString(R.string.events_scope_hidden, statsAllHiddenEvents));
                filterValues.add(Constants.pref_Events_Scope_Hidden);
            }
            final int silencedEventsCount = eventsData.getSilencedEventsCount();
            if (silencedEventsCount > 0) {
                if (statsSilencedEvents != silencedEventsCount && isDebugOrExtraFun) {
                    filterVariants.add(getString(R.string.events_scope_silenced_dead, statsSilencedEvents, silencedEventsCount));
                    isDeadLinks = true;
                } else
                    filterVariants.add(getString(R.string.events_scope_silenced, statsSilencedEvents));
                filterValues.add(Constants.pref_Events_Scope_Silenced);
            }
            final int xDaysEventsCount = eventsData.getXDaysEventsCount();
            if (xDaysEventsCount > 0) {
                if (statsXDaysEvents != xDaysEventsCount && isDebugOrExtraFun) {
                    filterVariants.add(getString(R.string.events_scope_xdays_dead, statsXDaysEvents, xDaysEventsCount));
                    isDeadLinks = true;
                } else {
                    filterVariants.add(getString(R.string.events_scope_xdays, xDaysEventsCount));
                }
                filterValues.add(Constants.pref_Events_Scope_XDays);
            }
            if (eventsData.statFavoriteEventsCount > 0) {
                filterVariants.add(getString(R.string.events_scope_favorite, eventsData.statFavoriteEventsCount));
                filterValues.add(Constants.pref_Events_Scope_Favorite);
            }
            if (statsUnrecognizedEvents > 0) {
                filterVariants.add(getString(R.string.events_scope_unrecognized, statsUnrecognizedEvents));
                filterValues.add(Constants.pref_Events_Scope_Unrecognized);
            }

            if (isDebugOrExtraFun && (hiddenEventsCount > 0 || silencedEventsCount > 0 || eventsData.getFavoritesEventsCount() > 0)) {
                filterVariants.add(getString(R.string.events_scope_clear));
                filterValues.add(Constants.pref_Events_Scope_Clear);
            }
            if (isDebugOrExtraFun && isDeadLinks) {
                filterVariants.add(getString(R.string.events_scope_clean));
                filterValues.add(Constants.pref_Events_Scope_Clean);
            }

            builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.activity_title_events_scope)
                    .setIcon(android.R.drawable.ic_menu_sort_by_size)
                    .setSingleChoiceItems(filterVariants.toArray(new CharSequence[0]), filterValues.indexOf(eventsData.preferences_list_events_scope), (dialog, which) -> {
                        final int choice = filterValues.get(((AlertDialog) dialog).getListView().getCheckedItemPosition());
                        if (choice == Constants.pref_Events_Scope_Clear) {

                            ArrayList<String> filterToSelect = new ArrayList<>();
                            filterToSelect.add(getString(R.string.msg_filter_clear_hidden)
                                    + Constants.STRING_BRACKETS_OPEN
                                    + eventsData.getHiddenEventsCount()
                                    + Constants.STRING_BRACKETS_CLOSE
                            );
                            filterToSelect.add(getString(R.string.msg_filter_clear_silenced)
                                    + Constants.STRING_BRACKETS_OPEN
                                    + eventsData.getSilencedEventsCount()
                                    + Constants.STRING_BRACKETS_CLOSE
                            );
                            filterToSelect.add(getString(R.string.msg_filter_clear_favorites)
                                    + Constants.STRING_BRACKETS_OPEN
                                    + eventsData.getFavoritesEventsCount()
                                    + Constants.STRING_BRACKETS_CLOSE
                            );
                            filterToSelect.add(getString(R.string.msg_filter_clear_withoutYear)
                                    + Constants.STRING_BRACKETS_OPEN
                                    + eventsData.getEventsWithoutYearCount()
                                    + Constants.STRING_BRACKETS_CLOSE
                            );
                            ArrayList<Boolean> filterSelected = new ArrayList<>(Collections.nCopies(filterToSelect.size(), false));

                            AlertDialog.Builder clearDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                                    .setTitle(R.string.msg_filter_clear_confirmation)
                                    .setIcon(android.R.drawable.ic_menu_help)
                                    .setMultiChoiceItems(filterToSelect.toArray(new CharSequence[0]), null, (clear_dialog, clear_which, isChecked) -> filterSelected.set(clear_which, isChecked))
                                    .setNegativeButton(R.string.button_cancel, (clear_dialog, clear_which) -> dialog.cancel())
                                    .setPositiveButton(R.string.button_ok, (clear_dialog, clear_which) -> {

                                        boolean needSave = false;
                                        boolean needUpdate = false;
                                        if (filterSelected.get(0)) {
                                            eventsData.clearHiddenEvents();
                                            needSave = true;
                                        }
                                        if (filterSelected.get(1)) {
                                            eventsData.clearSilencedEvents();
                                            needSave = true;
                                        }
                                        if (filterSelected.get(2)) {
                                            eventsData.clearFavoriteEvents();
                                            needSave = true;
                                        }
                                        if (filterSelected.get(3)) {
                                            eventsData.clearEventsWithoutYear();
                                            needUpdate = true;
                                        }

                                        if (!needSave && !needUpdate) return;
                                        eventsData.preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                                        if (needSave) {
                                            eventsData.savePreferences();
                                        }
                                        if (needUpdate) {
                                            eventsData.needUpdateEventList = true;
                                            updateList(true);
                                        } else {
                                            this.invalidateOptionsMenu();
                                            filterEventsList();
                                            drawList();
                                        }
                                    });

                            AlertDialog clearDialog = clearDialogBuilder.create();

                            clearDialog.setOnShowListener(arg0 -> {
                                try (TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme)) {
                                    clearDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                    clearDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                }
                            });

                            clearDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            clearDialog.show();

                        } else if (choice == Constants.pref_Events_Scope_Clean) {

                            AlertDialog.Builder confirm = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                                    .setTitle(R.string.msg_title_confirmation)
                                    .setMessage(R.string.msg_filter_clean_confirmation)
                                    .setIcon(android.R.drawable.ic_menu_help)
                                    .setNegativeButton(R.string.button_cancel, (confirm_dialog, confirm_which) -> dialog.cancel())
                                    .setPositiveButton(R.string.button_ok, (confirm_dialog, confirm_which) -> {
                                        eventsData.clearDeadLinksHiddenEvents();
                                        eventsData.clearDeadLinksSilencedEvents();
                                        eventsData.clearDeadLinksXDaysEvents();
                                        eventsData.preferences_list_events_scope = Constants.pref_Events_Scope_NotHidden;
                                        eventsData.savePreferences();
                                        this.invalidateOptionsMenu();
                                        filterEventsList();
                                        drawList();
                                    });

                            AlertDialog confirm_dialog = confirm.create();

                            confirm_dialog.setOnShowListener(arg0 -> {
                                try (TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme)) {
                                    confirm_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                    confirm_dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                                }
                            });

                            confirm_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            confirm_dialog.show();

                        } else {

                            eventsData.preferences_list_events_scope = choice;

                        }
                        eventsData.savePreferences();
                        dialog.cancel();
                        filterEventsList();
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
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {

            if (resultCode == RESULT_OK) {
                if ((requestCode == Constants.RESULT_PICK_CONTACT || requestCode == Constants.RESULT_PICK_OTHER_CONTACT)) {

                    Uri contactUri = data.getData();
                    if (contactUri != null) {
                        String contactID = contactUri.toString().substring(contactUri.toString().lastIndexOf(Constants.STRING_SLASH) + 1);
                        if (!contactID.isEmpty() && !selectedEvent[ContactsEvents.Position_eventID].isEmpty()) {
                            if (eventsData.setMergedID(
                                    selectedEvent[ContactsEvents.Position_eventID],
                                    contactID,
                                    eventsData.map_contacts_ids.get(contactID))) {
                                eventsData.needUpdateEventList = true;
                                updateList(true);
                            }
                        }
                    }

                } else if (requestCode == Constants.RESULT_EDIT_EVENT) {

                    eventsData.needUpdateEventList = true;
                    updateList(true);

                } else {

                    super.onActivityResult(requestCode, resultCode, data);

                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    protected void onResume() {
        try {
            super.onResume();
            if (!filterNames.isEmpty()) return; //Чтобы не было обновления списка после просмотра контакта (при непустой строке поиска)

            if (eventsData == null) eventsData = ContactsEvents.getInstance();

            //если "выходили" посмотреть карточку контакта или события на 5 сек
            if (eventsData.statLastPausedForOtherActivity > 0 && !this.dataList.isEmpty()
                    && System.currentTimeMillis() - eventsData.statLastPausedForOtherActivity < Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates
                    && !eventsData.needUpdateEventList) {

                eventsData.statLastPausedForOtherActivity = 0;
                return;
            }

            //Устанавливаем язык приложения
            eventsData.initLanguage(this);
            if (!localeAtCreate.equals(eventsData.currentLocale)) {
                recreate();
                return;
            }
            resources = eventsData.getResources();

            //Устанавливаем тему и переоткрываем окно
            this.setTheme(eventsData.preferences_theme.themeMain);
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            if (eventsData.currentTheme != eventsData.preferences_theme.themeMain) {
                eventsData.currentTheme = eventsData.preferences_theme.themeMain;
                eventsData.updateShortcuts();

                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                this.finish();
                startActivity(intent);
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

            LinearLayout mainLayout = findViewById(R.id.layout_main);
            LinearLayout.MarginLayoutParams marginParams = (LinearLayout.MarginLayoutParams) mainLayout.getLayoutParams();
            marginParams.setMargins(
                    (int) (eventsData.preferences_list_margin * displayMetrics.density + 0.5f),
                    ImageUtils.Dip2Px(resources, eventsData.preferences_list_top_padding),
                    (int) (eventsData.preferences_list_margin * displayMetrics.density + 0.5f),
                    marginParams.bottomMargin);
            mainLayout.setLayoutParams(marginParams);

            //Тему не меняли, просто обновляем данные
            if (eventsData.needUpdateEventList || this.dataList.isEmpty() != eventsData.isEmptyEventList()
                    || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {

                    updateList(true);

                if (!scrolledToToday && eventsData.statEventsPrevEventsFound > 0 &&
                        eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_NotHidden
                        && listView.getFirstVisiblePosition() == 0) {
                    listView.post(() -> {
                        listView.setSelectionFromTop(eventsData.statEventsPrevEventsFound, 0);
                        scrolledToToday = true;
                    });
                }

                eventsData.initNotifications();
                eventsData.updateWidgets(0, null);

            }
            this.invalidateOptionsMenu();
            eventsData.updateShortcuts();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) return;

            if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS || requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CALENDAR) {

                registerForContextMenu(listView);
                updateList(true);

            } else if (requestCode == Constants.MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {

                eventsData.initNotifications();

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void filterEventsList() {
        try {
            int statsVisibleEvents = 0;
            statsAllEvents = 0;
            statsAllHiddenEvents = 0;
            statsHiddenEvents = 0;
            statsSilencedEvents = 0;
            statsXDaysEvents = 0;
            statsUnrecognizedEvents = 0;
            dataList.clear();

            if (!eventsData.isEmptyEventList()) {
                boolean isSearchAllTypesAndSources = (!filterNames.isEmpty() && eventsData.preferences_list_search_depth == ContactsEvents.SearchDepth.AllEvents)
                        || eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Favorite
                        || eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_XDays
                        || eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden;

                for (String event : eventsData.eventList) {
                    String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                    String eventKey = eventsData.getEventKey(singleEventArray);
                    String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);

                    boolean isHiddenEvent = eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId);
                    boolean isSilencedEvent = eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId);
                    boolean isXDayEvent = eventsData.isXDaysEvent(eventKey)
                            && resources.getString(R.string.event_type_xdays_emoji).equals(singleEventArray[ContactsEvents.Position_eventEmoji]);
                    boolean isFavoriteEvent = eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred]);
                    boolean isEventTypeToShow = eventsData.preferences_list_event_types.contains(singleEventArray[ContactsEvents.Position_eventType]);

                    if (isHiddenEvent) statsAllHiddenEvents++;
                    if (isSilencedEvent) statsSilencedEvents++;
                    if (isXDayEvent) statsXDaysEvents++;

                    //Фильтр по источникам
                    boolean isEventSourceToShow = false;
                    if (!eventsData.preferences_list_EventSources.isEmpty()) {
                        final String eventDates = singleEventArray[ContactsEvents.Position_dates];
                        for (String source: eventsData.preferences_list_EventSources) {
                            if (eventDates.contains(source)) {
                                isEventSourceToShow = true;
                                break;
                            }
                        }
                        if (!isSearchAllTypesAndSources && !isEventSourceToShow) continue;
                    } else {
                        isEventSourceToShow = true;
                    }

                    if (isEventTypeToShow && isEventSourceToShow) {
                        statsAllEvents++;
                        if (isHiddenEvent) statsHiddenEvents++;
                    }

                    boolean isUnrecognized = isUnrecognizedEvent(singleEventArray);
                    boolean skipAdd = false;

                    if (isUnrecognized) statsUnrecognizedEvents++;
                    if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Unrecognized) {
                        if (isUnrecognized) {
                            dataList.add(event);
                            skipAdd = true;
                        }
                    }

                    //Фильтр по типам
                    if (isEventTypeToShow || isSearchAllTypesAndSources) {

                        //Фильтр по режиму отображения
                        if ((eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_NotHidden && !isHiddenEvent) || //Показывать нескрытые
                                (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden && isHiddenEvent) || //Показывать только скрытые
                                (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Silenced && isSilencedEvent) || //Показывать только без уведомлений
                                (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_XDays && isXDayEvent) || //Показывать только счётчики дней
                                (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Favorite && isFavoriteEvent) || //Показывать только избранные
                                eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_All) {
                                    if (!skipAdd) dataList.add(event);
                                    statsVisibleEvents++;
                        }
                    }

                }
            }

            if (dataList.isEmpty()) { // && eventsData.eventListPrev.isEmpty()

                listView.setVisibility(View.GONE);

                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(HtmlCompat.fromHtml(
                        getString(R.string.msg_zero_events_title) +
                                eventsData.getCurrentParams() +
                                getString(R.string.msg_zero_events_footer)
                        , HtmlCompat.FROM_HTML_MODE_LEGACY));
                    //https://stackoverflow.com/questions/1748977/making-textview-scrollable-on-android
                    emptyView.setMovementMethod(new ScrollingMovementMethod());
                }

                setHint(StringUtils.getHTMLColor(getString(R.string.msg_no_events).toLowerCase(), Constants.HTML_COLOR_YELLOW, eventsData.getContext()));

            } else {

                listView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);

                if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden) {
                    setHint(resources.getString(R.string.msg_stats_hidden_prefix) + statsVisibleEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_All) {
                    setHint(resources.getString(R.string.msg_stats_prefix) + statsVisibleEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Silenced) {
                    setHint(resources.getString(R.string.msg_stats_silenced_prefix) + statsVisibleEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_XDays) {
                    setHint(resources.getString(R.string.msg_stats_xdays_prefix) + statsVisibleEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Unrecognized) {
                    setHint(resources.getString(R.string.msg_stats_unrecognized_prefix) + statsUnrecognizedEvents + Constants.STRING_SPACE);
                } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Favorite) {
                    setHint(resources.getString(R.string.msg_stats_favorite_prefix) + statsVisibleEvents + Constants.STRING_SPACE);
                } else {
                    setHint(resources.getString(R.string.msg_stats_prefix) + statsVisibleEvents + Constants.STRING_SPACE);
                }

                //Получаем предыдущие события
                if (eventsData.preferences_list_prev_events_scan_distance > 0 && eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Hidden) {
                    dataList.addAll(0, eventsData.getPreviousEvents(dataList));
                }

            }

            if (statsUnrecognizedEvents > 0 && eventsData.preferences_rules_unrecognized == ContactsEvents.Rules_Unrecognized_Type_Unrecognized) {
                ToastExpander.showInfoMsg(this, StringUtils.toProperCase(resources.getString(R.string.msg_stats_unrecognized_prefix)) + statsUnrecognizedEvents);
            }

            eventsData.statUnrecognizedEvents = statsUnrecognizedEvents;
            dataListFull.clear();
            dataListFull.addAll(dataList);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private boolean isUnrecognizedEvent(@NonNull String[] singleEventArray) {
        return Constants.EventType_Unrecognized.equals(singleEventArray[ContactsEvents.Position_eventType]);
    }

    private void drawList() {
        try {

            //Сохраняем позицию в списке, чтобы вернутся к ней после обновления
            //https://stackoverflow.com/a/3035521/4928833

            int index = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
            int top = (v == null) ? 0 : (v.getTop()) - listView.getPaddingTop();

            listView.setVisibility(dataList.isEmpty() ? View.GONE : View.VISIBLE);
            emptyView.setVisibility(dataList.isEmpty() && filterNames.isEmpty() ? View.VISIBLE : View.GONE);

            //Если использовать тут adapter.notifyDataSetChanged(); при поиске список ломается и показывает не то
            adapter = new EventsAdapter(this, dataListFull, dataList);
            listView.setAdapter(adapter);

            //Возвращаемся к ранее сохранённой позиции после обновления
            listView.post(() -> {
                if (index  < listView.getCount()) {
                    //Почему-то при index = 0 идёт сдвиг вверх на getPaddingTop
                    listView.setSelectionFromTop(index, index > 0 ? top : top + listView.getPaddingTop());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setHint(@NonNull String msg) {
        try {
            TextView stats = findViewById(R.id.mainStatsTextView);
            stats.setText(HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);

            if (eventsData.preferences_list_margin > 0) {
                    stats.setPadding(
                            stats.getPaddingLeft(),
                            stats.getPaddingTop(),
                            (int) (eventsData.preferences_list_margin * displayMetrics.density + 0.5f),
                            stats.getPaddingBottom());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    private void updateUIAfterEventsLoad() {
        filterEventsList();
        drawList();

        swipeRefresh.setRefreshing(false);

        invalidateOptionsMenu();

        if (eventsData.isEmptyEventList()) {
            showZeroEventsHints();
        }
    }

    synchronized void updateList(boolean setRefreshing) {
        try {
            boolean coldStart = false;
            if (setRefreshing) {
                if (swipeRefresh.isLaidOut()) {
                    swipeRefresh.setRefreshing(true);
                } else {
                    coldStart = true;
                }
            }

            boolean needLoad = eventsData.needUpdateEventList || eventsData.isEmptyEventList();

            if (needLoad) {
                if (coldStart) {
                    eventsData.getEvents();
                    if (!isFinishing() && !isDestroyed()) {
                        updateUIAfterEventsLoad();
                    }
                } else {
                    eventsData.getEventsAsync(success -> {
                        if (!isFinishing() && !isDestroyed()) {
                            updateUIAfterEventsLoad();
                        }
                    });
                }
            } else {
                updateUIAfterEventsLoad();
            }

        } catch (Exception e) {
            if (swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void showZeroEventsHints() {
        try {
            //Сообщение для тех, у кого не найдено ни одного события контактов
            if (!triggeredMsgNoEvents && !DeviceTools.checkNoContactsAccess(eventsData.getContext()) && dataListFull.isEmpty()) {
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
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    void selectEventsTypes() {
        try {

            final List<String> eventTypesIDs = new ArrayList<>(Arrays.asList(resources.getStringArray(R.array.pref_List_EventTypes_values)));
            final List<String> eventTypesTitles = new ArrayList<>(Arrays.asList(resources.getStringArray(R.array.pref_List_EventTypes_entries)));
            ArrayList<Boolean> eventTypesSelected = new ArrayList<>();
            final boolean[] isProgrammaticChange = {false};

            Set<String> preferences_list_types = eventsData.preferences_list_event_types;
            boolean[] sel = new boolean[eventTypesIDs.size()];
            int ind = 0;
            for (String eventType: eventTypesIDs) {
                eventTypesTitles.set(ind,
                        eventTypesTitles.get(ind)
                                + Constants.STRING_BRACKETS_OPEN
                                + (eventsData.statEventTypes.containsKey(eventType) ? eventsData.statEventTypes.get(eventType) : Constants.STRING_0)
                                + Constants.STRING_BRACKETS_CLOSE
                );
                sel[ind] = preferences_list_types.contains(eventType);
                eventTypesSelected.add(sel[ind]);
                ind++;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.pref_List_EventTypes_title)
                    .setIcon(R.drawable.ic_menu_copy)
                    .setMultiChoiceItems(eventTypesTitles.toArray(new CharSequence[0]), sel, (dialog, which, isChecked) -> {
                        // Обновляем eventTypesSelected ТОЛЬКО при ручном изменении пользователем
                        if (!isProgrammaticChange[0]) {
                            eventTypesSelected.set(which, isChecked);
                        }
                    })
                    .setPositiveButton(R.string.button_ok, (dialog, which) -> {

                        Set<String> toStore = new HashSet<>();
                        for (int i = 0; i < eventTypesSelected.size(); i++) {
                            if (eventTypesSelected.get(i)) toStore.add(eventTypesIDs.get(i));
                        }

                        eventsData.preferences_list_event_types = toStore;
                        eventsData.savePreferences();
                        eventsData.needUpdateEventList = true;
                        updateList(true);

                        dialog.cancel();
                    })
                    .setNegativeButton(R.string.button_cancel, (dialog, which) -> dialog.cancel())
                    .setNeutralButton(R.string.msg_all, null)
                    .setCancelable(true);

            AlertDialog alertToShow = builder.create();
            ListView listView = alertToShow.getListView();

            alertToShow.setOnShowListener(arg0 -> {
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));

                alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    // Определяем целевое состояние
                    boolean hasUnchecked = false;
                    for (boolean selected : eventTypesSelected) {
                        if (!selected) {
                            hasUnchecked = true;
                            break;
                        }
                    }
                    boolean stateTo = hasUnchecked;

                    isProgrammaticChange[0] = true;

                    for (int i = 0; i < listView.getCount(); i++) {
                        // Если текущее состояние не совпадает с целевым — "кликаем"
                        if (listView.isItemChecked(i) != stateTo) {
                            // Получаем View для элемента (может быть null для невидимых — это ОК)
                            View view = listView.getAdapter().getView(i, null, listView);
                            // Имитируем клик
                            listView.performItemClick(view, i, listView.getAdapter().getItemId(i));
                        }
                        // На всякий случай синхронизируем нашу модель
                        eventTypesSelected.set(i, stateTo);
                    }

                    isProgrammaticChange[0] = false;
                });
            });

            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static class ViewHolder {
        //https://stackoverflow.com/questions/21501316/what-is-the-benefit-of-viewholder
        //https://metanit.com/java/android/5.8.php
        //todo: перейти на RecycleView https://www.spreys.com/listview-to-recyclerview/ https://metanit.com/java/android/5.12.php

        final TextView NameTextView;
        final TextView DayDistanceTextView;
        final TextView DateTextView;
        final TextView DetailsTextView;
        final TextView DescriptionTextView;
        final ImageView PhotoImageView;
        final TextView CounterTextView;
        final ImageView EventIconImageView;
        final ImageView ShowDescriptionImageView;

        ViewHolder(TextView NameTextView, TextView DayDistanceTextView, TextView DateTextView,
                   TextView DetailsTextView, TextView DescriptionTextView, ImageView PhotoImageView, TextView CounterTextView,
                   ImageView EventIconImageView, ImageView ShowDescriptionImageView) {
            this.NameTextView = NameTextView;
            this.DayDistanceTextView = DayDistanceTextView;
            this.DateTextView = DateTextView;
            this.DetailsTextView = DetailsTextView;
            this.DescriptionTextView = DescriptionTextView;
            this.PhotoImageView = PhotoImageView;
            this.CounterTextView = CounterTextView;
            this.EventIconImageView = EventIconImageView;
            this.ShowDescriptionImageView = ShowDescriptionImageView;
        }
    }

    class EventsAdapter extends ArrayAdapter<String> implements Filterable {

        private final List<String> listAll;
        /** Хранилище состояний: какие события имеют раскрытое описание */
        private final Set<String> expandedDescriptions = new HashSet<>();
        final int dimen_details;
        final int dimen_name;
        final int dimen_date;

        private EventsAdapter(@NonNull Context context, @NonNull List<String> eventsListFull, @NonNull List<String> eventsList)
        {
            super(context, R.layout.entry_main, eventsList);

                listAll = new ArrayList<>(eventsListFull);
                dimen_details = (int) (eventsData.dimen_List_details / eventsData.displayMetrics_density);
                dimen_name = (int) (eventsData.dimen_List_name / eventsData.displayMetrics_density);
                dimen_date = (int) (eventsData.dimen_list_date / eventsData.displayMetrics_density);

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

                if (convertedView == null) {
                    //https://stackoverflow.com/questions/10641144/difference-between-getcontext-getapplicationcontext-getbasecontext-and
                    convertedView = LayoutInflater.from(getContext()).inflate(R.layout.entry_main, parent, false);
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

                String eventKey = eventsData.getEventKey(singleEventArray);
                String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                String eventDistance = singleEventArray[ContactsEvents.Position_eventDistance];
                String[] eventDistanceText = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.REGEX_BAR, -1);
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

                //Дата оригинального события или предстоящая дата (для 5K и счётчиков дней)
                String eventEmoji = singleEventArray[ContactsEvents.Position_eventEmoji];
                String eventDateFirstTime = singleEventArray[ContactsEvents.Position_eventDateFirstTime];
                String eventDateNextTime = singleEventArray[ContactsEvents.Position_eventDateNextTime];

                if (resources.getString(R.string.event_type_xdays_emoji).equals(eventEmoji) || resources.getString(R.string.event_type_5k_emoji).equals(eventEmoji)) {
                    holder.DateTextView.setText(AppDateUtils.getDateFormatted(eventDateNextTime, ContactsEvents.FormatDate.WithYear,
                            eventsData.preferences_date_format, eventsData.getContext(), eventsData.getResources(), eventsData.currentLocale));
                } else if (eventDateFirstTime.length() < 10) {
                    holder.DateTextView.setText(AppDateUtils.getDateFormatted(eventDateNextTime, ContactsEvents.FormatDate.WithoutYear,
                            eventsData.preferences_date_format, eventsData.getContext(), eventsData.getResources(), eventsData.currentLocale));
                } else {
                    holder.DateTextView.setText(AppDateUtils.getDateFormatted(eventDateFirstTime, ContactsEvents.FormatDate.WithYear,
                            eventsData.preferences_date_format, eventsData.getContext(), eventsData.getResources(), eventsData.currentLocale));
                }

                //Фамилия Имя Отчество
                holder.NameTextView.setText(StringUtils.getFullName(singleEventArray, eventsData.preferences_name_format));

                //Информация под именем
                StringBuilder eventDetails = new StringBuilder();

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Organization))) {
                    final String contactOrganization = StringUtils.getNotNullString(singleEventArray[ContactsEvents.Position_organization]).trim();
                    if (!contactOrganization.isEmpty()) eventDetails.append(contactOrganization.trim());
                }
                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_JobTitle))) {
                    final String positionJobTitle = StringUtils.getNotNullString(singleEventArray[ContactsEvents.Position_title]).trim();
                    if (!positionJobTitle.isEmpty()) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.STRING_COMMA_SPACE);
                        eventDetails.append(positionJobTitle);
                    }
                }
                if (eventDetails.length() > 0) {
                    eventDetails.insert(0, Constants.HTML_BOLD_START).append(Constants.HTML_BOLD_END);
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Nickname)) && StringUtils.hasContent(singleEventArray[ContactsEvents.Position_nickname])) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(singleEventArray[ContactsEvents.Position_nickname]);
                }

                String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];
                String eventLabel = singleEventArray[ContactsEvents.Position_eventLabel].trim();
                String eventCaption = singleEventArray[ContactsEvents.Position_eventCaption].trim();

                String strZodiac = Constants.STRING_EMPTY;
                if (eventSubType.equals(Constants.EventType_BirthDay) || eventSubType.equals(Constants.EventType_5K)) {
                    final String strZodiacInfo = eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_ZodiacSign)) ?
                            singleEventArray[ContactsEvents.Position_zodiacSign].trim() : Constants.STRING_EMPTY;
                    final String strZodiacYearInfo = eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_ZodiacYear)) ?
                            singleEventArray[ContactsEvents.Position_zodiacYear].trim() : Constants.STRING_EMPTY;
                    strZodiac = strZodiacInfo.concat(Constants.STRING_SPACE).concat(strZodiacYearInfo).trim();
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_EventCaption))) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(eventCaption);
                    if (!eventLabel.isEmpty() && eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_StoredEventTitle)) && !eventCaption.equalsIgnoreCase(eventLabel)) {
                        eventDetails.append(Constants.STRING_PARENTHESIS_OPEN).append(eventLabel).append(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                    if (!strZodiac.isEmpty()) {
                        eventDetails.append(Constants.STRING_PARENTHESIS_OPEN).append(strZodiac).append(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                } else if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_StoredEventTitle))) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    if (!eventLabel.isEmpty()) {
                        eventDetails.append(eventLabel);
                    } else {
                        eventDetails.append(eventCaption);
                    }
                    if (!strZodiac.isEmpty()) {
                        eventDetails.append(Constants.STRING_PARENTHESIS_OPEN).append(strZodiac).append(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                } else if (!strZodiac.isEmpty()) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(strZodiac);
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Age))) {
                    final String currentAge = singleEventArray[ContactsEvents.Position_age_current];
                    if (!currentAge.isEmpty() && !currentAge.contains(": 0")) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        eventDetails.append(currentAge);
                    }
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_URL))) {
                    final String eventURL = singleEventArray[ContactsEvents.Position_eventURL].trim();
                    if (!eventURL.isEmpty()) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        eventDetails.append(eventURL.replace(Constants.STRING_2TILDA, Constants.HTML_BR));
                    }
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_EventSource))) {
                    final String eventSource = singleEventArray[ContactsEvents.Position_eventSource].trim();
                    if (!eventSource.isEmpty()) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        eventDetails.append(eventSource.replace(Constants.STRING_2TILDA, Constants.HTML_BR));
                    }
                }

                final String eventDescription = singleEventArray[ContactsEvents.Position_eventDescription].trim();
                holder.ShowDescriptionImageView.setVisibility(View.GONE);
                holder.DescriptionTextView.setVisibility(View.GONE);

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Description))) {
                    //Если включено - показываем описание сразу
                    if (!eventDescription.isEmpty()) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        eventDetails.append(eventDescription);
                    }
                } else if (!eventDescription.isEmpty()) {
                    //Если выключено, но описание не пустое - показываем кнопку
                    holder.ShowDescriptionImageView.setVisibility(View.VISIBLE);
                    holder.DescriptionTextView.setText(eventDescription);

                    // Восстановление состояния из глобального хранилища
                    if (expandedDescriptions.contains(eventKey)) {
                        holder.DescriptionTextView.setVisibility(View.VISIBLE);
                        holder.ShowDescriptionImageView.setImageDrawable(
                                AppCompatResources.getDrawable(MainActivity.this, android.R.drawable.arrow_up_float));
                    } else {
                        holder.DescriptionTextView.setVisibility(View.GONE);
                        holder.ShowDescriptionImageView.setImageDrawable(
                                AppCompatResources.getDrawable(MainActivity.this, android.R.drawable.arrow_down_float));
                    }

                    View.OnClickListener listener = v -> {
                        // Переключаем состояние в глобальном хранилище
                        if (expandedDescriptions.contains(eventKey)) {
                            expandedDescriptions.remove(eventKey);
                            holder.DescriptionTextView.setVisibility(View.GONE);
                            holder.ShowDescriptionImageView.setImageDrawable(
                                    AppCompatResources.getDrawable(MainActivity.this, android.R.drawable.arrow_down_float));
                        } else {
                            expandedDescriptions.add(eventKey);
                            holder.DescriptionTextView.setVisibility(View.VISIBLE);
                            holder.ShowDescriptionImageView.setImageDrawable(
                                    AppCompatResources.getDrawable(MainActivity.this, android.R.drawable.arrow_up_float));
                        }
                    };
                    holder.ShowDescriptionImageView.setOnClickListener(listener);
                    holder.DescriptionTextView.setOnClickListener(listener);
                }

                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_DebugInfo))) {
                    String[] dates = singleEventArray[ContactsEvents.Position_dates].split(Constants.STRING_2TILDA, -1);
                    int eventDates = dates.length;
                    if (eventDates > 0) {
                        if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                        for (int i = 0; i < eventDates; i++) {
                            if (i > 0) eventDetails.append(Constants.HTML_BR);
                            String date = dates[i];
                            int ind = date.lastIndexOf(Constants.STRING_COLON_SPACE);
                            eventDetails.append((ind > -1 ? date.substring(0, ind) : date).trim());
                        }
                    }
                }

                if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Hidden && eventsData.getHiddenEventsCount() > 0 && eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(StringUtils.getHTMLColor(getString(R.string.msg_label_hidden), Constants.HTML_COLOR_RED, eventsData.getContext()));
                }
                if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Silenced && eventsData.getSilencedEventsCount() > 0 && eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {
                    if (eventDetails.length() > 0) eventDetails.append(Constants.HTML_BR);
                    eventDetails.append(StringUtils.getHTMLColor(getString(R.string.msg_label_silenced), Constants.HTML_COLOR_BROWN, eventsData.getContext()));
                }

                if (eventDetails.length() == 0) {
                    holder.DetailsTextView.setText(Constants.STRING_EMPTY);
                } else {
                    //https://stackoverflow.com/questions/2116162/how-to-display-html-in-textview
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        holder.DetailsTextView.setText(HtmlCompat.fromHtml(eventDetails.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT));
                    } else {
                        holder.DetailsTextView.setText(HtmlCompat.fromHtml(eventDetails.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY));
                    }
                }

                //Фото
                if (eventsData.preferences_list_event_info.contains(getString(R.string.pref_List_EventInfo_Photo))) {
                    int roundingFactor;
                    if (eventSubType.equals(Constants.EventType_Calendar)
                            || eventSubType.equals(Constants.EventType_File)
                            || eventSubType.equals(Constants.EventType_Holiday)) {
                        roundingFactor = 1;
                    } else {
                        roundingFactor = eventsData.preferences_list_photostyle;
                    }

                    holder.PhotoImageView.setImageBitmap(eventsData.getEventPhoto(event, true, false, true, roundingFactor));
                    holder.PhotoImageView.setVisibility(View.VISIBLE);
                } else {
                    holder.PhotoImageView.setImageDrawable(null);
                    holder.PhotoImageView.setVisibility(View.GONE);
                }

                //Годовщина
                if (eventsData.isJubilee(person.Age, eventSubType)) {
                    holder.CounterTextView.setTextColor(eventsData.preferences_list_color_eventjubilee);
                } else {
                    holder.CounterTextView.setTextColor(ta.getColor(R.styleable.Theme_eventAgeColor, ContextCompat.getColor(eventsData.getContext(), R.color.theme_grey_primary)));
                }
                holder.CounterTextView.setText(singleEventArray[ContactsEvents.Position_age_caption]);

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
                            ta.getColor(R.styleable.Theme_gradientEndColor, ContextCompat.getColor(eventsData.getContext(), R.color.light_gray_darker))
                    });

                } else if (eventsData.preferences_list_filling == Integer.parseInt(getString(R.string.pref_List_Filling_BottomToTop))) {

                    drawableBack.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                    drawableBack.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                    drawableBack.setColors(new int[] {
                            ta.getColor(R.styleable.Theme_gradientStartColor, ContextCompat.getColor(eventsData.getContext(), R.color.lighter_gray)),
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

                if (eventsData.statEventsPrevEventsFound > 0) {
                    int eventDistanceDays = 0;
                    try {
                        eventDistanceDays = Integer.parseInt(eventDistance);
                    } catch (NumberFormatException ignored) { /**/ }
                    if (eventDistanceDays < 0) {
                        final float alphaPrev = (float) 0.7;
                        holder.NameTextView.setAlpha(alphaPrev);
                        holder.CounterTextView.setAlpha(alphaPrev);
                        holder.DateTextView.setAlpha(alphaPrev);
                        holder.DayDistanceTextView.setAlpha(alphaPrev);
                        holder.DetailsTextView.setAlpha(alphaPrev);
                        holder.PhotoImageView.setImageAlpha((int)(255*alphaPrev));
                        holder.EventIconImageView.setImageAlpha((int)(255*alphaPrev));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(eventsData.getContext(), StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }
            return convertedView;
        }

        private ViewHolder createViewHolderFrom(@NonNull View view) {

            TextView NameTextView = view.findViewById(R.id.entryNameTextView);
            TextView DayDistanceTextView = view.findViewById(R.id.entryDayDistanceTextView);
            TextView DateTextView = view.findViewById(R.id.entryDateTextView);
            TextView DetailsTextView = view.findViewById(R.id.entryEventDetailsTextView);
            TextView DescriptionTextView = view.findViewById(R.id.entryDescriptionTextView);
            TextView CounterTextView = view.findViewById(R.id.entryDetailsCounter);
            ImageView PhotoImageView = view.findViewById(R.id.entryPhotoImageView);
            ImageView EventIconImageView = view.findViewById(R.id.entryEventIcon);
            ImageView ShowDescriptionImageView = view.findViewById(R.id.entryButtonShowDescription);

            DayDistanceTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + eventsData.preferences_list_magnify_distance * 0.1)));
            NameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + eventsData.preferences_list_magnify_name * 0.1)));
            DetailsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + eventsData.preferences_list_magnify_details * 0.1)));
            DescriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_details * (1 + eventsData.preferences_list_magnify_details * 0.1)));
            DateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_date * (1 + eventsData.preferences_list_magnify_date * 0.1)));
            CounterTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) (dimen_name * (1 + eventsData.preferences_list_magnify_age * 0.1)));

            return new ViewHolder(NameTextView, DayDistanceTextView, DateTextView, DetailsTextView, DescriptionTextView,
                    PhotoImageView, CounterTextView, EventIconImageView, ShowDescriptionImageView);
        }

        @Override
        @NonNull
        public Filter getFilter() {
            return EventsFilter;
        }

        private final Filter EventsFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                List<String> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() < 2) {
                    filteredList.addAll(listAll);
                    filterNames = Constants.STRING_EMPTY;
                    results.values = filteredList;
                    return results;
                }

                try {

                    List<String> searchSource = (eventsData.preferences_list_search_depth == ContactsEvents.SearchDepth.AllEvents)
                            ? eventsData.eventList
                            : listAll;

                    filterNames = constraint.toString().trim();

                    //для поиска AND используем <строка1>+<строка2>
                    //для поиска OR используем <строка1>,<строка2>

                    if (filterNames.contains(Constants.STRING_PLUS)) {
                        String[] rawTerms = filterNames.split(Constants.REGEX_PLUS, -1);
                        List<String> normalizedTerms = new ArrayList<>();
                        for (String term : rawTerms) {
                            String norm = StringUtils.normalizeString(term.trim());
                            if (norm != null && !norm.isEmpty()) {
                                normalizedTerms.add(norm);
                            }
                        }
                        if (normalizedTerms.isEmpty()) {
                            results.values = filteredList; // пустой список
                            return results;
                        }
                        for (String itemRaw : searchSource) {
                            String itemNorm = StringUtils.normalizeString(itemRaw);
                            if (itemNorm != null && matchesAll(itemNorm, normalizedTerms)) {
                                filteredList.add(itemRaw);
                            }
                        }

                    } else if (filterNames.contains(Constants.STRING_COMMA)) {
                        String[] rawTerms = filterNames.split(Constants.STRING_COMMA, -1);
                        List<String> normalizedTerms = new ArrayList<>();
                        for (String term : rawTerms) {
                            String norm = StringUtils.normalizeString(term.trim());
                            if (norm != null && !norm.isEmpty()) {
                                normalizedTerms.add(norm);
                            }
                        }
                        if (normalizedTerms.isEmpty()) {
                            results.values = filteredList; // пустой список
                            return results;
                        }
                        for (String itemRaw : searchSource) {
                            String itemNorm = StringUtils.normalizeString(itemRaw);
                            if (itemNorm != null && matchesAny(itemNorm, normalizedTerms)) {
                                filteredList.add(itemRaw);
                            }
                        }

                    } else {
                        // Простой поиск: нормализуем весь запрос как один терм
                        String searchTerm = StringUtils.normalizeString(filterNames);
                        if (searchTerm != null && !searchTerm.isEmpty()) {
                            for (String itemRaw : searchSource) {
                                String itemNorm = StringUtils.normalizeString(itemRaw);
                                if (itemNorm != null && itemNorm.contains(searchTerm)) {
                                    filteredList.add(itemRaw);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Filtering error", e);
                    ToastExpander.showDebugMsg(eventsData.getContext(), StringUtils.getMethodName(3) + ": " + e.getMessage());
                }
                results.values = filteredList;
                return results;
            }

            private boolean matchesAll(String text, List<String> terms) {
                for (String term : terms) {
                    if (term.isEmpty()) continue;
                    if (!text.contains(term)) {
                        return false;
                    }
                }
                return true;
            }

            private boolean matchesAny(String text, List<String> terms) {
                for (String term : terms) {
                    if (term.isEmpty()) continue;
                    if (text.contains(term)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                try {
                    if (results != null && results.values != null) {
                        dataList.clear();
                        List<?> result = (List<?>) results.values;
                        for (Object object : result) {
                            if (object instanceof String) {
                                dataList.add((String) object);
                            }
                        }
                    }

                    if (!dataList.isEmpty()) {
                        boolean isSearchAllTypesAndSources = eventsData.preferences_list_search_depth == ContactsEvents.SearchDepth.AllEvents;
                        String hintPrefix;

                        if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Hidden) {
                            hintPrefix = filterNames.isEmpty() ? resources.getString(R.string.msg_stats_hidden_prefix) : isSearchAllTypesAndSources ?
                                    resources.getString(R.string.msg_stats_filtered_prefix) : resources.getString(R.string.msg_stats_hidden_filtered_prefix);
                        } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Silenced) {
                            hintPrefix = filterNames.isEmpty() ? resources.getString(R.string.msg_stats_silenced_prefix) : isSearchAllTypesAndSources ?
                                    resources.getString(R.string.msg_stats_filtered_prefix) : resources.getString(R.string.msg_stats_silenced_filtered_prefix);
                        } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_XDays) {
                            hintPrefix = filterNames.isEmpty() ? resources.getString(R.string.msg_stats_xdays_prefix) : isSearchAllTypesAndSources ?
                                    resources.getString(R.string.msg_stats_filtered_prefix) : resources.getString(R.string.msg_stats_xdays_filtered_prefix);
                        } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Unrecognized) {
                            hintPrefix = filterNames.isEmpty() ? resources.getString(R.string.msg_stats_unrecognized_prefix) : isSearchAllTypesAndSources ?
                                    resources.getString(R.string.msg_stats_filtered_prefix) : resources.getString(R.string.msg_stats_unrecognized_filtered_prefix);
                        } else if (eventsData.preferences_list_events_scope == Constants.pref_Events_Scope_Favorite) {
                            hintPrefix = filterNames.isEmpty() ? resources.getString(R.string.msg_stats_favorite_prefix) : isSearchAllTypesAndSources ?
                                    resources.getString(R.string.msg_stats_filtered_prefix) : resources.getString(R.string.msg_stats_favorite_filtered_prefix);
                        } else {
                            hintPrefix = filterNames.isEmpty() ? resources.getString(R.string.msg_stats_prefix) : resources.getString(R.string.msg_stats_filtered_prefix);
                        }

                        setHint(hintPrefix
                                .concat(filterNames.isEmpty() ? String.valueOf(dataList.size()) : StringUtils.getHTMLColor(String.valueOf(dataList.size()), Constants.HTML_COLOR_YELLOW, eventsData.getContext()))
                                .concat(Constants.STRING_SPACE)
                        );

                    } else {
                        setHint(StringUtils.getHTMLColor(getString(R.string.msg_no_events).toLowerCase(), Constants.HTML_COLOR_YELLOW, eventsData.getContext()).concat(Constants.STRING_SPACE));
                    }

                    drawList();

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    ToastExpander.showDebugMsg(eventsData.getContext(), StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                }

            }
        };
    }

    @Override
    public void applyOverrideConfiguration(@Nullable Configuration overrideConfiguration) {
        try {
            if (overrideConfiguration != null) {
                int uiMode = overrideConfiguration.uiMode;
                overrideConfiguration.setTo(resources.getConfiguration());
                overrideConfiguration.uiMode = uiMode;
            }
            super.applyOverrideConfiguration(overrideConfiguration);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(2) + Constants.STRING_COLON_SPACE + e);
        }
    }

}