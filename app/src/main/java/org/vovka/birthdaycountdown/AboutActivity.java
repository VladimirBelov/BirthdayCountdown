/*
 * *
 *  * Created by Vladimir Belov on 22.03.20 23:03
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 18.03.20 23:11
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import java.util.TimeZone;

import static org.vovka.birthdaycountdown.Constants.STRING_DIALOG_TAB;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

public class AboutActivity extends AppCompatActivity {

    @SuppressLint("PrivateResource")
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            this.setTheme(eventsData.preferences_theme.themeMain);
            eventsData.setLocale(true);

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources resources = getBaseContext().getResources();
            Configuration applicationConf = resources.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            resources.updateConfiguration(applicationConf, resources.getDisplayMetrics());

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

            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm", resources.getConfiguration().locale);
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

            StringBuilder sb = new StringBuilder();
            //todo: на Android 9 и 10 при первом показе грузит дефолтный язык
            int color = ta.getColor(R.styleable.Theme_eventDateColor, 0); // почему-то #RRGGBB с webView не работает вообще - пустой экран
            sb.append(getString(R.string.changelog_header, Color.red(color) + "," + Color.green(color) + "," + Color.blue(color)));

            //Debug information
            if (eventsData.preferences_debug_on) {

                sb.append(getString(R.string.debuglog_body,
                        String.valueOf(Math.round(eventsData.statTimeGetContacts)),
                        String.valueOf(Math.round(eventsData.statTimeComputeDates)),
                        String.valueOf(Math.round(eventsData.statTimeDrawList)),
                        eventsData.statAllEvents,
                        eventsData.statAllTitles,
                        eventsData.statAllOrganizations,
                        eventsData.statAllNicknames
                        /*String.valueOf(resources.getDisplayMetrics().heightPixels),
                        String.valueOf(resources.getDisplayMetrics().widthPixels),
                        String.valueOf(resources.getDisplayMetrics().density)*/
                ));

            }

            //Change log
            String[] arrChangeLog = resources.getStringArray(R.array.changelog);
            if (arrChangeLog.length > 0) {

                sb.append(getString(R.string.changelog_title));
                int countRows = 0;
                for(String strChange: arrChangeLog) {

                    countRows++;
                    if (strChange.charAt(0) == '#') {

                        if (countRows > 1) sb.append("</ul>");
                        sb.append(getString(R.string.changelog_release_title, strChange.substring(1)));

                    } else {

                        sb.append("<li>").append(strChange).append("</li>");

                    }
                }
                if (countRows > 0) sb.append("</ul>");

            }

            sb.append("</body></html>");

            WebView webView = findViewById(R.id.webView);
            webView.setVerticalScrollBarEnabled(true);
            webView.setBackgroundColor(Color.TRANSPARENT);
            webView.loadData(sb.toString(), "text/html; charset=utf-8", "utf-8");

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

}
