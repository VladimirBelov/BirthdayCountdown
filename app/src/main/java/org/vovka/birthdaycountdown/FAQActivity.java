/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 15.09.2022, 21:56
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class FAQActivity extends AppCompatActivity {

    private static final String TAG = "FAQActivity";

    @SuppressLint("PrivateResource")
    public void onCreate(Bundle savedInstanceState) {

        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            super.onCreate(savedInstanceState);

            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
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

            this.setTheme(eventsData.preferences_theme.themeMain);

            setContentView(R.layout.activity_faq);

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
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back);
            }

            eventsData.setLocale(true); //Без этого на Android 9+ при первом показе webview грузит дефолтный язык
            WebView webView = findViewById(R.id.webView);
            if (webView != null) {
                webView.setVerticalScrollBarEnabled(true);
                webView.setBackgroundColor(Color.TRANSPARENT);
            }

            findViewById(R.id.buttonMail).setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:belov.vladimir@mail.ru?subject=" + getString(R.string.app_name) + "%20" + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + ")")));
                } catch (android.content.ActivityNotFoundException e) { /**/ }
                finish();
            });

            StringBuilder sb = new StringBuilder();
            int color = ta.getColor(R.styleable.Theme_eventDateColor, 0); // почему-то #RRGGBB с webView не работает вообще - пустой экран
            sb.append(getString(R.string.faq_header, Color.red(color) + "," + Color.green(color) + "," + Color.blue(color)));

            String[] arrFAQrows;
            try {
                arrFAQrows = getResources().getStringArray(R.array.faq);
            } catch (Resources.NotFoundException e) {
                arrFAQrows = new String[]{};
            }

            if (arrFAQrows.length > 0) {
                for (String strRow : arrFAQrows) {
                    if (strRow.length() >= 3 && strRow.startsWith("###")) {
                        sb.append(Constants.HTML_BR).append(Constants.HTML_H1_START).append(strRow.substring(3)).append(Constants.HTML_H1_END);
                    } else if (strRow.length() >= 2 && strRow.startsWith("##")) {
                        sb.append(Constants.HTML_BR).append(Constants.HTML_H2_START).append(strRow.substring(2)).append(Constants.HTML_H2_END);
                    } else if (strRow.length() >= 1 && strRow.startsWith("#")) {
                        sb.append(Constants.HTML_H3_START).append(strRow.substring(1)).append(Constants.HTML_H3_END);
                    } else {
                        sb.append(strRow).append(Constants.HTML_BR);
                    }
                }
            }
            sb.append("</body></html>");

            if (webView != null) {
                webView.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html; charset=utf-8", "utf-8", null);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}
