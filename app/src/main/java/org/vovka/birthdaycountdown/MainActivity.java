package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.vovka.birthdaycountdown.ContactsEvents.Div4;

//todo: сделать вывод ошибок в стандартный лог

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    //https://medium.com/@kevalpatel2106/how-you-can-decrease-application-size-by-60-in-only-5-minutes-47eff3e7874e

    //Константы
    private static final String SPACE_STRING = " ";
    static final String NOTIFICATION_CHANNEL_ID = "BirthdayCountdown";
    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    static final int notificationId = 1000;

    //UI объекты
    private SwipeRefreshLayout swipeRefresh;
    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener;
    private Menu menu;

    //Переменные
    private String filterNames = "";
    private ContactsEvents eventsData;

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
            filterNames = "";

            //Устанавливаем язык приложения
            eventsData.setLocale(true);

            //Устанавливаем тему
            //https://carthrottle.io/how-to-implement-flexible-night-mode-in-your-android-app-f00f0f83b70e
            //https://medium.com/@pkjvit/https-medium-com-pkjvit-android-multi-theme-night-mode-and-material-design-c186bf9fd678
            //https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94
            this.setTheme(eventsData.preferences_theme.themeMain);
            eventsData.currentTheme = eventsData.preferences_theme.themeMain;

            setContentView(R.layout.activity_main);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна https://github.com/neokree/MaterialNavigationDrawer/issues/5
            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            swipeRefresh = findViewById(R.id.swiperefresh);
            swipeRefresh.setOnRefreshListener(this); //Set the listener to be notified when a refresh is triggered via the swipe gesture
            //todo: убрать setRefreshing на старте и починить на пункте меню "обновить"

            //Обновляем меню https://stackoverflow.com/questions/14867458/android-refresh-options-menu-without-calling-invalidateoptionsmenu
            this.invalidateOptionsMenu();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                //https://developer.android.com/training/permissions/requesting.html#java

                showMsgbox(getString(R.string.msg_no_access), "");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                //swipeRefresh.setRefreshing(false);
                return;

            }

            //https://stackoverflow.com/questions/24587925/swiperefreshlayout-trigger-programmatically/35621309#35621309
            swipeRefreshListener = () -> {
                if (eventsData.getContactsEvents(this)) {
                    eventsData.computeDates();
                    drawList();
                    eventsData.updateWidgets();
                    swipeRefresh = findViewById(R.id.swiperefresh);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false); // Disables the refresh icon
                }
            };

            //About
            findViewById(R.id.toolbar).setOnClickListener(v -> {

                //https://stackoverflow.com/questions/2422562/how-to-change-theme-for-alertdialog
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                builder.setTitle(R.string.app_name);
                builder.setIcon(R.drawable.ic_birthdaycountdowngreen77);

                SimpleDateFormat formater = new SimpleDateFormat("dd MMM yyyy HH:mm", getResources().getConfiguration().locale);
                formater.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                //https://stackoverflow.com/questions/14652894/using-html-in-android-alert-dialog
                //https://commonsware.com/blog/Android/2010/05/26/html-tags-supported-by-textview.html
                TextView msg = new TextView(this);
                final String tab = "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                msg.setText(Html.fromHtml("" +
                        "<font color=\"#" + Integer.toHexString(ta.getColor(R.styleable.Theme_dialogTextColor, 0) & 0x00ffffff) + "\">" +
                        tab + "&nbsp;&nbsp;created by Vladimir Belov" +
                        tab + "&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"mailto:belov.vladimir@mail.ru?subject=" + this.getString(R.string.app_name) + "%20" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\">belov.vladimir@mail.ru</a>" +
                        "<br>&nbsp;" +
                        //https://stackoverflow.com/a/21119027/4928833
                        //https://stackoverflow.com/questions/3540739/how-to-programmatically-read-the-date-when-my-android-apk-was-built
                        tab + "version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")" +
                        tab + "build date: " + formater.format(BuildConfig.BUILD_TIME) +
                        tab + "load speed:" +
                        tab + "&nbsp;&nbsp;contacts scanning: " + Math.round(eventsData.statGetContacts * 100.0) / 100.0 + "msec" +
                        tab + "&nbsp;&nbsp;dates computing: " + Math.round(eventsData.statComputeDates * 100.0) / 100.0 + "msec" +
                        tab + "&nbsp;&nbsp;list drawing: " + Math.round(eventsData.statDrawList * 100.0) / 100.0 + "msec" +
                        tab + "screen density: " + this.getResources().getDisplayMetrics().density +
                        tab + "dimension set: " + getString(R.string.dimenSet) + "</font>"));
                msg.setMovementMethod(LinkMovementMethod.getInstance());
                msg.setClickable(true);
                builder.setView(msg);

                builder.setPositiveButton(R.string.button_OK, (dialog, which) -> dialog.cancel());

                builder.setNeutralButton(R.string.button_Rate, (dialog, which) -> {
                    dialog.cancel();
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
                    }
                });

                AlertDialog alertToShow = builder.create();

                alertToShow.setOnShowListener(arg0 -> {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                    alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                });

                alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertToShow.show();
                //alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_eventDateColor, 0));
            });

            //Получение и отображение контактных данных
            swipeRefresh.post(() -> {
                //swipeRefresh.setRefreshing(true);
                swipeRefreshListener.onRefresh();
                //swipeRefresh.setRefreshing(false);
            });

            //Уведомления
            initNotifications();

        } catch (Exception e) {
            e.printStackTrace();
            //if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "MainActivity->onCreate error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }

    }

    private void initNotifications() {
        //https://stackoverflow.com/a/51645875/4928833

        try{
            //StringBuilder log = new StringBuilder();
            PackageManager pm = this.getPackageManager();
            ComponentName receiver = new ComponentName(this, DeviceBootReceiver.class);
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (eventsData.preferences_notifications_days >= 0 && NotificationManagerCompat.from(this).areNotificationsEnabled()) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    //todo: проверять, что пользователь не выключил канал
                    //if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).getImportance() != NotificationManager.IMPORTANCE_NONE) {

                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription(getString(R.string.pref_Notifications_Notification_Channel_Description));
                    notificationManager.createNotificationChannel(channel);
                }

                //To enable Boot Receiver class
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    //log.append("Notifications were enabled\n");
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, eventsData.preferences_notifications_alarm_hour);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);

                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                    //@SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    //log.append("Next notification: ").append(sdf.format(calendar.getTime()));
                }

            } else { //Disable Daily Notifications
                if (PendingIntent.getBroadcast(this, 0, alarmIntent, 0) != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
                }
                if (pm.getComponentEnabledSetting(receiver) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    //log.append("Notifications were disabled\n");
                }
            }
            //if (log.length() > 0) Toast.makeText(this,log.toString(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->initNotifications error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }
    }

    public void onRefresh() {

        if (swipeRefreshListener == null) return; //swipeRefresh == null ||
        try {

            //swipeRefresh.setRefreshing(true);
            //setLocale();
            swipeRefreshListener.onRefresh();
            //swipeRefresh.setRefreshing(false); // Disables the refresh icon

        } catch (Exception e) {
            e.printStackTrace();
            //swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "MainActivity->onRefresh error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
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
        this.menu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {

            switch (item.getItemId()) {
                case R.id.menu_refresh:
                    //todo: показывать анимацию обновления: https://github.com/googlesamples/android-SwipeRefreshLayoutBasic/blob/master/Application/src/main/java/com/example/android/swiperefreshlayoutbasic/SwipeRefreshLayoutBasicFragment.java
                    /*swipeRefresh.setRefreshing(true);
                    if (eventsData.getContactsEvents()) {
                        eventsData.computeDates();
                        drawList();
                        eventsData.updateWidgets();
                    }
                    swipeRefresh.setRefreshing(false);*/

                    swipeRefresh.post(() -> {
                        //swipeRefresh = findViewById(R.id.swiperefresh);
                        swipeRefresh.setRefreshing(true);
                        swipeRefreshListener.onRefresh();
                        //swipeRefresh.setRefreshing(false);
                    });
                    return true;

                case R.id.menu_settings:

                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);

                    return true;

                case R.id.menu_search:
                    //https://stackoverflow.com/questions/10903754/input-text-dialog-android

                    TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(R.string.title_activity_search);
                    builder.setIcon(android.R.drawable.ic_menu_search);
                    builder.setMessage(R.string.msg_label_search);

                    final EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(filterNames);
                    input.setHint(R.string.msg_hint_search);
                    input.setHintTextColor(ta.getColor(R.styleable.Theme_dialogHintColor, 0));
                    input.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));

                    if (!filterNames.equals("")) input.selectAll();
                    builder.setView(input);

                    builder.setPositiveButton(R.string.button_OK, (dialog13, which) -> {
                        filterNames = input.getText().toString();

                        if (filterNames.equals("")) {
                            //https://stackoverflow.com/questions/19882443/how-to-change-menuitem-icon-in-actionbar-programmatically/19882555#19882555
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                        } else {
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_zoom));
                        }

                        //eventsData.computeDates(); при задании фильтра даты пересчитывать не надо
                        drawList();
                    });

                    if (!filterNames.equals("")) {
                        builder.setNeutralButton(R.string.button_Clear, (dialog12, which) -> {
                            filterNames = "";
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                            dialog12.dismiss();
                            //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо
                            drawList();
                        });
                    }

                    builder.setNegativeButton(R.string.button_Cancel, (dialog1, which) -> dialog1.cancel());

                    builder.setCancelable(true);

                    AlertDialog alertToShow = builder.create();

                    //https://stackoverflow.com/questions/27965662/how-can-i-change-default-dialog-button-text-color-in-android-5
                    alertToShow.setOnShowListener(arg0 -> {
                        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
                    });

                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    //https://stackoverflow.com/questions/4054662/displaying-soft-keyboard-whenever-alertdialog-builder-object-is-opened/6123935#6123935
                    //https://stackoverflow.com/questions/5593053/open-soft-keyboard-programmatically
                    input.requestFocus();
                    if (alertToShow.getWindow() != null) alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    alertToShow.show();

                    //https://stackoverflow.com/questions/15362122/change-font-size-for-an-alertdialog-message
                    alertToShow.getWindow().getAttributes();
                    TextView textView = alertToShow.findViewById(android.R.id.message);
                    textView.setTextSize(14);

                    return true;

                case R.id.menu_exit:
                    finish();
                    return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            //swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "MainActivity->onOptionsItemSelected error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onBackPressed()
    {
        //https://stackoverflow.com/questions/18337536/android-overriding-onbackpressed
        if (filterNames.equals("")) {

            super.onBackPressed();
            finish();

        } else {
            filterNames = "";
            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
            //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо
            drawList();
        }

    }

    @Override
    protected void onResume() {
        try {
            super.onResume();

            eventsData = ContactsEvents.getInstance();
            eventsData.context = this; //getApplicationContext();
            eventsData.getPreferences();

            //Устанавливаем язык приложения
            eventsData.setLocale(true);

            //Устанавливаем тему
            if (eventsData.currentTheme != eventsData.preferences_theme.themeMain) {
                this.setTheme(eventsData.preferences_theme.themeMain);
                this.recreate();
                return;
            }

            //Тему не меняли, просто обновляем данные

            //Обновляем меню
            this.invalidateOptionsMenu();

            boolean canReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
            if (canReadContacts && (eventsData.dataArray == null || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000)) {
                //swipeRefresh.setRefreshing(true);
                if (eventsData.getContactsEvents(this)) {
                    eventsData.computeDates();
                    drawList();
                    eventsData.updateWidgets();
                }
                //swipeRefresh.setRefreshing(false);

                //Уведомления
                initNotifications();
            }


        } catch (Exception e) {
            e.printStackTrace();
            //swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "MainActivity->onResume error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }
    }

/*
    @Override
    protected void onRestart() {
        super.onRestart();
    }
*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (eventsData.getContactsEvents(this)) {
                    eventsData.computeDates();
                    drawList();
                    eventsData.updateWidgets();
                }
            }
        }
    }

    private void drawList() {
        long statCurrentModuleStart = System.currentTimeMillis();

        String[] dataArray_final = {};
        eventsData.preferences_list_prev_events_found = 0;
        try {

            //Проверки
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                showMsgbox(getString(R.string.msg_no_access), "");
            else if (eventsData.dataArray == null || eventsData.dataArray.length == 0)
                showMsgbox(getString(R.string.msg_no_events), "");
            else {

                List<String> dataList = new ArrayList<>(Arrays.asList(eventsData.dataArray));

                //Фильтруем
                if (!filterNames.equals("")) {

                    List<String> dataList_filtered = new ArrayList<>();

                    //для поиска AND используем <строка1>.*<строка2>
                    Matcher filter = Pattern.compile(filterNames.replaceAll(" *, *", ",").toUpperCase().replace("Ё", "Е").replace(Div4, "|"), Pattern.CASE_INSENSITIVE).matcher("");
                    for (String listItem : dataList)
                        if (filter.reset(listItem).find()){
                            if (!dataList_filtered.contains(listItem)) {
                                dataList_filtered.add(listItem);
                            }
                        }

                    dataArray_final = dataList_filtered.toArray(new String[0]);
                    if (dataArray_final.length > 0) {
                        showMsgbox(getResources().getString(R.string.msg_stats_prefix) + SPACE_STRING + dataArray_final.length + getString(R.string.msg_filter), filterNames);
                    } else {
                        showMsgbox(getString(R.string.msg_no_events) + getString(R.string.msg_filter), filterNames);
                    }

                } else {

                    //Получаем предыдцщие события
                    if (!eventsData.preferences_list_prev_events.equals("")) {
                        //todo: придумать, как ловить прошедшие 5K+
                        dataList = eventsData.insertPreviousEvents(dataList, eventsData.preferences_list_prev_events);
                    }

                    dataArray_final = dataList.toArray(new String[0]);
                    showMsgbox(getResources().getString(R.string.msg_stats_prefix) + SPACE_STRING + dataArray_final.length, "");

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

                    String[] dataArray1 = ((String) l.getItemAtPosition(position)).split(ContactsEvents.Div1);

                    //https://stackoverflow.com/questions/4275167/how-to-open-a-contact-card-in-android-by-id?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, dataArray1[ContactsEvents.dataMap.get("contact_id")]);
                    intent.setData(uri);
                    MainActivity.this.startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "drawList->onItemClick error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            listView.setOnItemLongClickListener((l, view, position, id) -> {
                try {

                    //todo: сделать вместо ЭТОГО контекстное меню или всплывающий диплог с полной инфо о событии
                    //https://startandroid.ru/ru/uroki/vse-uroki-spiskom/47-urok-15-kontekstnoe-menju.html

                    //todo: подсказки про именины на основе имени и даты рождения

                    //todo: знаки зодиака и года

                    //todo: ссылки с имени и фамилии на web справочник

                    String s = (String) l.getItemAtPosition(position);
                    Toast.makeText(this, s, Toast.LENGTH_LONG).show();

                    //https://stackoverflow.com/a/7173248/4928833
                /*new CountDownTimer(4000, 1000)
                {

                    public void onTick(long millisUntilFinished) {tag.show();}
                    public void onFinish() {tag.show();}

                }.start();*/
                } catch (Exception e) {
                    //
                }
                return true;
            });

            listView.requestFocus();
            //Возвращаемся к ранее сохранённой позиции после обновления
            listView.setSelectionFromTop(index, top);

            eventsData.statDrawList = System.currentTimeMillis() - statCurrentModuleStart;

        } catch (Exception e) {
            eventsData.statDrawList = System.currentTimeMillis() - statCurrentModuleStart;
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->drawList error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }
    }

    private void showMsgbox(String msg, String msg_red) {

        StringBuilder output = new StringBuilder();
        output.append(msg);
        TextView stats = findViewById(R.id.mainStatsTextView);
        stats.setText(output);

        output = new StringBuilder();
        output.append(msg_red);
        stats = findViewById(R.id.mainStatsRedTextView);
        stats.setText(output);

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

        final TypedArray ta;
        String tag_Bold_start;
        final String tag_Bold_end = "</font>";
        final ContactsEvents eventsData;
        final Resources resources;

        private MyAdapter(Context context, String[] values)
        {
            super(context, R.layout.entry_main, values);
            ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
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
                    singleRowArray = event.split(ContactsEvents.Div1);
                    person = new Person(context, getItem(position));
                } else {
                    return convertView;
                }

                if (singleRowArray.length <= 9) {
                    Toast.makeText(context, "MyAdapter->getView error:\nAbnormal dimension of string: " + singleRowArray.length + "\nData: " + singleRowArray[0], Toast.LENGTH_SHORT).show();
                    return convertView;
                }

                //todo: сделать отображение Имя Фамилия + настройка
                holder.NameTextView.setText(singleRowArray[ContactsEvents.dataMap.get("fio")]);

                if (tag_Bold_start == null) {
                    //https://stackoverflow.com/questions/5026995/android-get-color-as-string-value
                    tag_Bold_start = "<font color=\"#" + Integer.toHexString(ta.getColor(R.styleable.Theme_eventFullNameColor, ContextCompat.getColor(context, R.color.medium_gray)) & 0x00ffffff) + "\">";
                }

                String eventDistance = singleRowArray[ContactsEvents.dataMap.get("eventDistance")];
                String eventDistanceText = singleRowArray[ContactsEvents.dataMap.get("eventDistanceText")];
                switch (eventDistance) {

                    case "0": //Сегодня

                        holder.DayDistanceTextView.setText(eventDistanceText);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(context, R.color.dark_red));
                        break;

                    case "1": //Завтра и послезавтра
                    case "2":

                        holder.DayDistanceTextView.setText(eventDistanceText);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(context, R.color.dark_green));
                        break;

                    //todo: сделать предыдщие

                    default: //Попозже
                        holder.DayDistanceTextView.setText(eventDistanceText);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.NORMAL);
                        holder.DayDistanceTextView.setTextColor(ta.getColor(R.styleable.Theme_eventDistanceColor, ContextCompat.getColor(context, R.color.dark_gray)));

                }

                //Дата оригинального события
                holder.DateTextView.setText(singleRowArray[ContactsEvents.dataMap.get("eventDateText")]);

                //Инфо под именем
                //todo: добавить ник
                StringBuilder eventDetails = new StringBuilder();
                if (eventsData.preferences_list_bottom_info.contains("1") && eventsData.preferences_list_bottom_info.contains("2")) {
                    if (singleRowArray[ContactsEvents.dataMap.get("organization")].trim().length() > 0) {
                        eventDetails.append(singleRowArray[ContactsEvents.dataMap.get("organization")].trim());
                    }
                    if (singleRowArray[ContactsEvents.dataMap.get("title")].trim().length() > 0) {
                        if (eventDetails.length() > 0) eventDetails.append(ContactsEvents.Div5);
                        eventDetails.append(singleRowArray[ContactsEvents.dataMap.get("title")].trim());
                    }
                    if (eventDetails.length() > 0) {
                        eventDetails.insert(0, tag_Bold_start).append(tag_Bold_end);
                    }
                } else if (eventsData.preferences_list_bottom_info.contains("1") && singleRowArray[ContactsEvents.dataMap.get("organization")].trim().length() > 0) {
                    eventDetails.append("<b>").append(singleRowArray[ContactsEvents.dataMap.get("organization")].trim()).append("</b>");
                } else if (eventsData.preferences_list_bottom_info.contains("2") && singleRowArray[ContactsEvents.dataMap.get("title")].trim().length() > 0) {
                    eventDetails.append("<b>").append(singleRowArray[ContactsEvents.dataMap.get("title")].trim()).append("</b>");
                }

                String eventLabel = singleRowArray[ContactsEvents.dataMap.get("eventLabel")];
                String eventCaption = singleRowArray[ContactsEvents.dataMap.get("eventCaption")];
                if (eventsData.preferences_list_bottom_info.contains("3")) {
                    if (eventDetails.length() > 0) eventDetails.append("<br>");
                    if (eventsData.preferences_list_bottom_info.contains("4") && !eventCaption.equals(eventLabel) && !eventLabel.equals("")) {
                        eventDetails.append(eventCaption).append(" (").append(eventLabel).append(")");
                    } else {
                        eventDetails.append(eventCaption);
                    }
                } else if (eventsData.preferences_list_bottom_info.contains("4") && !eventLabel.equals("")) {
                    if (eventDetails.length() > 0) eventDetails.append("<br>");
                    eventDetails.append(eventLabel);
                }

                if (eventsData.preferences_list_bottom_info.contains("91")) {
                    if (eventDetails.length() > 0) eventDetails.append("<br>");
                    eventDetails.append(singleRowArray[ContactsEvents.dataMap.get("dates")].replace(ContactsEvents.Div2, "<br>").trim());
                }

                if (eventDetails.length() == 0) {
                    holder.DetailsTextView.setText("");
                } else {
                    //https://stackoverflow.com/questions/2116162/how-to-display-html-in-textview
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        holder.DetailsTextView.setText(Html.fromHtml(eventDetails.toString(), Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        holder.DetailsTextView.setText(Html.fromHtml(eventDetails.toString()));
                    }
                }

                //Определяем иконку события

                //Фото
                holder.PhotoImageView.setImageBitmap(eventsData.getContactPhoto(event, true));

                if (person.Age > -1 && person.Age % 10 == 0) {
                    holder.CounterTextView.setTextColor(resources.getColor(R.color.dark_red));
                } else {
                    holder.CounterTextView.setTextColor(ta.getColor(R.styleable.Theme_eventAgeColor, ContextCompat.getColor(context, R.color.medium_gray)));
                }
                holder.CounterTextView.setText(person.Age_str);

                //Определяем иконку события
                int eventIcon;
                try {
                    eventIcon = Integer.parseInt(singleRowArray[ContactsEvents.dataMap.get("eventIcon")]);
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
                Toast.makeText(context, "MyAdapter->getView error:\n" + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
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

        private ViewHolder createViewHolderFrom(View view) {

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