/*
 * *
 *  * Created by Vladimir Belov on 17.08.2021, 10:49
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 11.08.2021, 22:23
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.DATETIME_DD_MM_YYYY_HH_MM;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_DEFAULT;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_RED;
import static org.vovka.birthdaycountdown.Constants.SPEED_LOAD_CRITICAL;
import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_DIALOG_TAB;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

//todo: подсветка нововведений в интерфейсе
// https://stackoverflow.com/questions/44826452/highlight-new-feature-in-android/44826950
// https://github.com/apuder/Highlight

public class AboutActivity extends AppCompatActivity {

    ContactsEvents eventsData;
    int counterClicks = 0;

    @SuppressLint("PrivateResource")
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            this.setTheme(eventsData.preferences_theme.themeMain);
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

            setContentView(R.layout.activity_changelog);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна
            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setHomeButtonEnabled(true);
                bar.setDisplayHomeAsUpEnabled(true);
                bar.setDisplayShowTitleEnabled(true);
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_material);
            }

            eventsData.setLocale(true); //Без этого на Android 9+ при первом показе webview грузит дефолтный язык
            SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_DD_MM_YYYY_HH_MM, eventsData.getResources().getConfiguration().locale);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+3"));

            //https://stackoverflow.com/questions/14652894/using-html-in-android-alert-dialog
            //https://commonsware.com/blog/Android/2010/05/26/html-tags-supported-by-textview.html
            //https://stackoverflow.com/a/21119027/4928833
            //https://stackoverflow.com/questions/3540739/how-to-programmatically-read-the-date-when-my-android-apk-was-built
            TextView txtInfo = findViewById(R.id.textVersionInfo);
            txtInfo.setText(HtmlCompat.fromHtml(STRING_EMPTY +
                            STRING_DIALOG_TAB + "&nbsp;&nbsp;created by Vladimir Belov" +
                            STRING_DIALOG_TAB + "&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"mailto:belov.vladimir@mail.ru?subject=" + this.getString(R.string.app_name) + "%20" + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + ")\">belov.vladimir@mail.ru</a>" +
                            "<br>&nbsp;"      +
                            STRING_DIALOG_TAB + "version: " + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + Constants.STRING_PARENTHESIS_CLOSE +
                            STRING_DIALOG_TAB + "built: " + formatter.format(BuildConfig.BUILD_TIME)
                    , 0));
            txtInfo.setMovementMethod(LinkMovementMethod.getInstance());
            txtInfo.setClickable(true);

            //https://stackoverflow.com/questions/58340558/how-to-detect-android-go
            //https://stackoverflow.com/questions/39036411/activitymanagercompat-islowramdevice-is-useless-is-always-returns-false
            //ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            //if (am.isLowRamDevice()) {
            //   webView.setVisibility(View.GONE);

            StringBuilder sb = new StringBuilder();
            int color = ta.getColor(R.styleable.Theme_eventDateColor, 0); // почему-то #RRGGBB с webView не работает вообще - пустой экран
            sb.append(getString(R.string.changelog_header, Color.red(color) + "," + Color.green(color) + "," + Color.blue(color)));

            //Statistics information
            sb.append(getString(R.string.stats_title));

            sb.append(getString(R.string.stats_speed_title));
            if (eventsData.statTimeGetContactEvents > 0)
                sb.append(getString(R.string.stats_speed_contacts, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeGetContactEvents)), eventsData.statTimeGetContactEvents > SPEED_LOAD_CRITICAL ? HTML_COLOR_RED : HTML_COLOR_DEFAULT).replace("#", "")));
            if (eventsData.statTimeGetCalendarEvents > 0)
                sb.append(getString(R.string.stats_speed_calendar, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeGetCalendarEvents)), eventsData.statTimeGetCalendarEvents > SPEED_LOAD_CRITICAL ? HTML_COLOR_RED : HTML_COLOR_DEFAULT).replace("#", "")));
            sb.append(getString(R.string.stats_speed_dates, eventsData.setHTMLColor(String.valueOf(Math.round(eventsData.statTimeComputeDates)), eventsData.statTimeComputeDates > SPEED_LOAD_CRITICAL ? HTML_COLOR_RED : HTML_COLOR_DEFAULT).replace("#", "")));
            sb.append(Constants.HTML_UL_END);

            sb.append(getString(R.string.stats_counters_title));
            if (eventsData.statAllEvents > 0)
                sb.append(getString(R.string.stats_counters_events, eventsData.statAllEvents));
            if (eventsData.statAllContacts > 0)
                sb.append(getString(R.string.stats_counters_contacts, eventsData.statAllContacts));
            if (eventsData.statAllTitles > 0)
                sb.append(getString(R.string.stats_counters_titles, eventsData.statAllTitles));
            if (eventsData.statAllOrganizations > 0)
                sb.append(getString(R.string.stats_counters_organizations, eventsData.statAllOrganizations));
            if (eventsData.statAllNicknames > 0)
                sb.append(getString(R.string.stats_counters_nicknames, eventsData.statAllNicknames));
            sb.append(Constants.HTML_UL_END);

            if (eventsData.statEventTypes.entrySet().size() > 0) {
                sb.append(getString(R.string.stats_counters_events_title));
                for (Map.Entry<String, Integer> entry : eventsData.statEventTypes.entrySet()) {
                    sb.append(Constants.HTML_LI).append(entry.getKey()).append(STRING_COLON_SPACE).append(entry.getValue());
                }
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

                        sb.append(Constants.HTML_LI).append(strChange);

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
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:belov.vladimir@mail.ru?subject=" + getString(R.string.app_name) + "%20" + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + ")")));
                finish();
            });

            findViewById(R.id.buttonRate).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
                }
                finish();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.ABOUT_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void setDebug(@SuppressWarnings("unused") android.view.View view) {

        try {

            counterClicks++;
            eventsData = ContactsEvents.getInstance();

            if (counterClicks == 3 || counterClicks == 4) {

                Toast.makeText(this, getString(R.string.pref_Debug_On_hint,
                        5 - counterClicks,
                        (5 - counterClicks) > 1 ? getString(R.string.msg_plural_postfix) : STRING_EMPTY,
                        getString(!eventsData.preferences_debug_on ? R.string.msg_on : R.string.msg_off)
                ), Toast.LENGTH_SHORT).show();

            } else if (counterClicks > 4) {

                counterClicks = 0;
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                if (eventsData != null && preferences != null) {
                    eventsData.preferences_debug_on = !eventsData.preferences_debug_on;
                    preferences
                            .edit()
                            .putBoolean(getString(R.string.pref_Help_Debug_On_key), eventsData.preferences_debug_on)
                            .apply();
                    Toast.makeText(this, getString(R.string.pref_Debug_On_title).concat(STRING_COLON_SPACE).concat(getString(eventsData.preferences_debug_on ? R.string.msg_on : R.string.msg_off)), Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.ABOUT_ACTIVITY_SET_DEBUG_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

}
