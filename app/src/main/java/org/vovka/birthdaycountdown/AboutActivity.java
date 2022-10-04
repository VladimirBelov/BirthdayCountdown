/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 15.09.2022, 21:54
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

//todo: подсветка нововведений в интерфейсе
// https://stackoverflow.com/questions/44826452/highlight-new-feature-in-android/44826950
// https://github.com/apuder/Highlight

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";
    ContactsEvents eventsData;
    int counterClicks = 0;
    private Toast mToast = null;

    @SuppressLint("PrivateResource")
    public void onCreate(Bundle savedInstanceState) {

        TypedArray ta = null;

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            this.setTheme(eventsData.preferences_theme.themeMain);
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

            setContentView(R.layout.activity_changelog);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setHomeButtonEnabled(true);
                bar.setDisplayHomeAsUpEnabled(true);
                bar.setDisplayShowTitleEnabled(true);
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back);
            }

            eventsData.setLocale(true); //Без этого на Android 9+ при первом показе webview грузит дефолтный язык
            SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, eventsData.getResources().getConfiguration().locale);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+3"));

            //https://stackoverflow.com/questions/14652894/using-html-in-android-alert-dialog
            //https://commonsware.com/blog/Android/2010/05/26/html-tags-supported-by-textview.html
            //https://stackoverflow.com/a/21119027/4928833
            //https://stackoverflow.com/questions/3540739/how-to-programmatically-read-the-date-when-my-android-apk-was-built
            //todo: добавить откуда поставили https://stackoverflow.com/questions/37539949/detect-if-an-app-is-installed-from-play-store
            TextView txtInfo = findViewById(R.id.textVersionInfo);
            txtInfo.setText(HtmlCompat.fromHtml(getString(R.string.changelog_version,
                    BuildConfig.VERSION_NAME, Integer.toString(BuildConfig.VERSION_CODE), formatter.format(BuildConfig.BUILD_TIME)), 0));
            txtInfo.setMovementMethod(LinkMovementMethod.getInstance());
            txtInfo.setClickable(true);

            TextView tv = findViewById(R.id.textShowPreferences);
            tv.setVisibility(eventsData.preferences_debug_on ? View.VISIBLE : View.GONE);

            //https://stackoverflow.com/questions/58340558/how-to-detect-android-go
            //https://stackoverflow.com/questions/39036411/activitymanagercompat-islowramdevice-is-useless-is-always-returns-false
            //ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            //if (am.isLowRamDevice()) {
            //   webView.setVisibility(View.GONE);

            StringBuilder sb = new StringBuilder();
            int color = ta.getColor(R.styleable.Theme_eventDateColor, 0); // почему-то #RRGGBB с webView не работает вообще - пустой экран
            sb.append(getString(R.string.changelog_header, Color.red(color) + "," + Color.green(color) + "," + Color.blue(color)));

            if (eventsData.preferences_extrafun) {

                //Statistics information
                sb.append(getString(R.string.stats_title));

                sb.append(getString(R.string.stats_speed_title));
                try {
                    if (eventsData.statTimeGetContactEvents > 0)
                        sb.append(getString(R.string.stats_speed_contacts, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeGetContactEvents)), eventsData.statTimeGetContactEvents > Constants.TIME_SPEED_LOAD_CRITICAL ? Constants.HTML_COLOR_RED : Constants.HTML_COLOR_DEFAULT).replace("#", "")));
                    if (eventsData.statTimeGetCalendarEvents > 0)
                        sb.append(getString(R.string.stats_speed_calendar, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeGetCalendarEvents)), eventsData.statTimeGetCalendarEvents > Constants.TIME_SPEED_LOAD_CRITICAL ? Constants.HTML_COLOR_RED : Constants.HTML_COLOR_DEFAULT).replace("#", "")));
                    if (eventsData.statTimeGetFileEvents > 0)
                        sb.append(getString(R.string.stats_speed_files, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeGetFileEvents)), eventsData.statTimeGetFileEvents > Constants.TIME_SPEED_LOAD_CRITICAL ? Constants.HTML_COLOR_RED : Constants.HTML_COLOR_DEFAULT).replace("#", "")));
                    sb.append(getString(R.string.stats_speed_dates, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeComputeDates)), eventsData.statTimeComputeDates > Constants.TIME_SPEED_LOAD_CRITICAL ? Constants.HTML_COLOR_RED : Constants.HTML_COLOR_DEFAULT).replace("#", "")));
                } catch (Exception e) { /**/ }
                sb.append(Constants.HTML_UL_END);

                sb.append(getString(R.string.stats_counters_title));
                try {
                    if (eventsData.statContactsCount > 0)
                        sb.append(getString(R.string.stats_counters_contacts, eventsData.statContactsCount));
                    if (eventsData.statContactsTitleCount > 0)
                        sb.append(getString(R.string.stats_counters_titles, eventsData.statContactsTitleCount));
                    if (eventsData.statContactsOrganizationCount > 0)
                        sb.append(getString(R.string.stats_counters_organizations, eventsData.statContactsOrganizationCount));
                    if (eventsData.statContactsNicknameCount > 0)
                        sb.append(getString(R.string.stats_counters_nicknames, eventsData.statContactsNicknameCount));
                    if (eventsData.statContactsURLCount > 0)
                        sb.append(getString(R.string.stats_counters_URLs, eventsData.statContactsURLCount));
                    sb.append(getString(R.string.stats_counters_events, eventsData.statEventsCount));
                    if (eventsData.statContactsEventCount > 0)
                        sb.append(getString(R.string.stats_counters_contacts_events, eventsData.statContactsEventCount));
                    if (eventsData.statCalendarsEventCount > 0)
                        sb.append(getString(R.string.stats_counters_calendars_events, eventsData.statCalendarsEventCount));
                    if (eventsData.statFilesEventCount > 0)
                        sb.append(getString(R.string.stats_counters_files_events, eventsData.statFilesEventCount));
                    sb.append(Constants.HTML_UL_END);

                    if (eventsData.statEventTypes.entrySet().size() > 0) {
                        sb.append(getString(R.string.stats_counters_events_title));
                        for (Map.Entry<String, Integer> entry : eventsData.statEventTypes.entrySet()) {
                            sb.append(Constants.HTML_LI).append(entry.getKey()).append(Constants.STRING_COLON_SPACE).append(entry.getValue());
                        }
                        sb.append(Constants.HTML_UL_END);
                    }
                } catch (Exception e) { /**/ }

                //Permissions
                sb.append(getString(R.string.stats_permissions_title));
                try {

                    sb.append(getString(R.string.stats_permissions_accounts, ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED)).replace("#", ""));

                    sb.append(getString(R.string.stats_permissions_contacts, ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED)).replace("#", ""));

                    sb.append(getString(R.string.stats_permissions_calendar, ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED)).replace("#", ""));

                    sb.append(getString(R.string.stats_permissions_files, ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED)).replace("#", ""));

                    sb.append(getString(R.string.stats_permissions_notifications, NotificationManagerCompat.from(this).areNotificationsEnabled()
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED)).replace("#", ""));

                    sb.append(getString(R.string.stats_permissions_wakelock, ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED)).replace("#", ""));

                    sb.append(getString(R.string.stats_permissions_battery, !eventsData.checkNoBatteryOptimization()
                            ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_RED) : eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_GREEN)).replace("#", ""));

                    //https://stackoverflow.com/questions/39366231/how-to-check-miui-autostart-permission-programmatically
                    if (isXiaomi()) {
                        final State state = getMIUIAutoStartState();
                        sb.append(getString(R.string.stats_permissions_xiaomi_autostart,
                                state == State.ENABLED ? eventsData.setHTMLColor(getString(R.string.msg_on), Constants.HTML_COLOR_GREEN) :
                                        state == State.DISABLED  ? eventsData.setHTMLColor(getString(R.string.msg_off), Constants.HTML_COLOR_RED) :
                                                eventsData.setHTMLColor(getString(R.string.msg_unknown), Constants.HTML_COLOR_DEFAULT)).replace("#", ""));

                    }

                } catch (Exception e) { /**/ }
                sb.append(Constants.HTML_UL_END);

            }

            //Change log
            //todo: когда количество строк превысит 700 - https://stackoverflow.com/questions/3522181/should-i-be-using-something-other-than-getresource-getstringarray-to-populat
            String[] arrChangeLog;
            try {
                arrChangeLog = eventsData.getResources().getStringArray(R.array.changelog);
            } catch (Resources.NotFoundException e) {
                arrChangeLog = new String[]{};
            }

            if (arrChangeLog.length > 0) {

                sb.append(getString(R.string.changelog_title));
                int countRows = 0;
                for (String strChange : arrChangeLog) {

                    countRows++;
                    if (strChange.charAt(0) == '#') {

                        if (countRows > 1) sb.append(Constants.HTML_UL_END);
                        sb.append(getString(R.string.changelog_release_title, strChange.substring(1)));

                    } else {

                        sb.append(Constants.HTML_LI).append(strChange.replace(Constants.STRING_EOL, Constants.HTML_BR));

                    }
                }
                if (countRows > 0) sb.append(Constants.HTML_UL_END);

            }
            sb.append("</body></html>");

            WebView webView = findViewById(R.id.webView);
            if (webView != null) {
                webView.setVerticalScrollBarEnabled(true);
                webView.setBackgroundColor(Color.TRANSPARENT);
                webView.loadData(sb.toString(), "text/html; charset=utf-8", "utf-8");

                //https://stackoverflow.com/questions/5107651/android-disable-text-selection-in-a-webview
                webView.setOnLongClickListener(v -> true);
                webView.setLongClickable(false);
            }

            findViewById(R.id.buttonMail).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:belov.vladimir@mail.ru?subject=" +
                            getString(R.string.app_name) + "%20" + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + ")")));
                } catch (RuntimeException e) { /**/ }
                finish();
            });

            findViewById(R.id.buttonRate).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                } catch (ActivityNotFoundException e) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
                    } catch (android.content.ActivityNotFoundException e2) { /**/ }
                }
                finish();
            });

            findViewById(R.id.buttonAppGallery).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://appgallery.huawei.com/app/C101143661")));
                } catch (android.content.ActivityNotFoundException e) { /**/ }
                finish();
            });

            findViewById(R.id.button4PDA).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://4pda.to/forum/index.php?showtopic=939391")));
                } catch (android.content.ActivityNotFoundException e) { /**/ }
                finish();
            });

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (ta != null) ta.recycle();
        }
    }

    public void setDebug(@SuppressWarnings("unused") android.view.View view) {

        try {

            counterClicks++;
            eventsData = ContactsEvents.getInstance();

            if (counterClicks == 3 || counterClicks == 4) {

                if (mToast != null) mToast.cancel();
                mToast = Toast.makeText(this, getString(R.string.pref_Debug_On_hint,
                        5 - counterClicks,
                        (5 - counterClicks) > 1 ? getString(R.string.msg_plural_postfix) : Constants.STRING_EMPTY,
                        getString(!eventsData.preferences_debug_on ? R.string.msg_on : R.string.msg_off)
                ), Toast.LENGTH_SHORT);
                mToast.show();

            } else if (counterClicks > 4) {

                counterClicks = 0;
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                if (eventsData != null && preferences != null) {
                    eventsData.preferences_debug_on = !eventsData.preferences_debug_on;
                    preferences
                            .edit()
                            .putBoolean(getString(R.string.pref_Help_Debug_On_key), eventsData.preferences_debug_on)
                            .apply();
                    this.recreate();
                    if (mToast != null) mToast.cancel();
                    mToast = Toast.makeText(this, getString(R.string.pref_Debug_On_title).concat(Constants.STRING_COLON_SPACE).concat(getString(eventsData.preferences_debug_on ? R.string.msg_on : R.string.msg_off)), Toast.LENGTH_SHORT);
                    mToast.show();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public void showPreferences (@SuppressWarnings("unused") android.view.View view) {

        try {

            StringBuilder sb = new StringBuilder();
            Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
            SortedSet<String> keys = new TreeSet<>(prefs.keySet());
            for (String key : keys) {
                sb.append(key).append(Constants.STRING_COLON_SPACE).append(prefs.get(key)).append(Constants.HTML_BR);
            }

            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
            builder.setTitle(R.string.msg_title_settings);
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setMessage(HtmlCompat.fromHtml(sb.toString(), 0));
            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> dialog.cancel());
            AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(arg0 -> alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0)));
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();
            TextView textView = alertToShow.findViewById(android.R.id.message);
            if (textView != null) textView.setTextSize(11);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private enum State {
        ENABLED, DISABLED, NO_INFO, UNEXPECTED_RESULT
    }

    private static boolean isXiaomi() {return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");}

    @SuppressLint("PrivateApi")
    public State getMIUIAutoStartState() throws Exception {

        Class<?> clazz = null;
        try {
            clazz = Class.forName("android.miui.AppOpsUtils");
        } catch (ClassNotFoundException ignored) { /**/ }
        if (clazz == null) return State.NO_INFO;

        Method method = null;
        try {
            method = clazz.getDeclaredMethod("getApplicationAutoStart", Context.class, String.class);
            method.setAccessible(true);
        } catch (Exception ignored) { /**/ }
        if (method == null) return State.NO_INFO;

        final Object result = method.invoke(null, this, this.getPackageName());

        if (!(result instanceof Integer)) {return State.UNEXPECTED_RESULT;}
        final int _int = (int) result;
        if (_int == 0) {
            return State.ENABLED;
        } else if (_int == 1) {
            return State.DISABLED;
        }
        return State.UNEXPECTED_RESULT;
    }

}
