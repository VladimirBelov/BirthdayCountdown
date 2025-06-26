/*
 * *
 *  * Created by Vladimir Belov on 26.06.2025, 13:04
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.06.2025, 12:48
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.LocaleManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * FAQActivity - это активность для отображения справочной информации о функциях приложения.
 */
public class FAQActivity extends AppCompatActivity {

    private static final String TAG = "FAQActivity";

    @SuppressLint({"PrivateResource", "SetJavaScriptEnabled"})
    public void onCreate(Bundle savedInstanceState) {

        ContactsEvents eventsData = ContactsEvents.getInstance();
        TypedArray ta = null;

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    LocaleList list = getSystemService(LocaleManager.class).getApplicationLocales();
                    if (!list.isEmpty()) {
                        locale = getSystemService(LocaleManager.class).getApplicationLocales().get(0);
                    }
                }
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeMain);
            setContentView(R.layout.activity_faq);

            View layoutMain = findViewById(R.id.layout_main);
            if (ContactsEvents.isEdgeToEdge()) {
                View layoutCoordinator = findViewById(R.id.coordinator);
                ViewCompat.setOnApplyWindowInsetsListener(layoutCoordinator, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
                    layoutCoordinator.setPadding(0, insets.top, 0, insets.bottom);
                    layoutMain.setPadding(0, insets.bottom, 0, 0);
                    return WindowInsetsCompat.CONSUMED;
                });
            } else {
                layoutMain.setPadding(0, ContactsEvents.Dip2Px(getResources(), 50), 0, 0);
            }

            //Отступы всего окна
            RelativeLayout.MarginLayoutParams marginParams = (RelativeLayout.MarginLayoutParams) layoutMain.getLayoutParams();
            marginParams.setMargins(
                    (int) (eventsData.preferences_list_margin * eventsData.displayMetrics_density + 0.5f),
                    ContactsEvents.Dip2Px(getResources(), eventsData.preferences_list_top_padding),
                    (int) (eventsData.preferences_list_margin * eventsData.displayMetrics_density + 0.5f),
                    marginParams.bottomMargin);
            layoutMain.setLayoutParams(marginParams);

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

            //Цвет CutoutAppearance на повёрнутом экране
            getWindow().setBackgroundDrawable(new ColorDrawable(ta.getColor(R.styleable.Theme_colorPrimary, ContextCompat.getColor(this, R.color.white))));

            eventsData.setLocale(true); //Без этого на Android 9+ при первом показе webview грузит язык по-умолчанию
            WebView webView = findViewById(R.id.webView);
            if (webView != null) {
                webView.setVerticalScrollBarEnabled(true);
                webView.setBackgroundColor(Color.TRANSPARENT);

                StringBuilder sb = new StringBuilder();
                int color = ta.getColor(R.styleable.Theme_eventDateColor, 0); // почему-то #RRGGBB с webView не работает вообще - пустой экран
                sb.append(getString(R.string.faq_header, Color.red(color) + "," + Color.green(color) + "," + Color.blue(color)));

                String[] arrFAQ;
                try {
                    arrFAQ = getResources().getStringArray(R.array.faq);
                } catch (Resources.NotFoundException e) {
                    arrFAQ = new String[]{};
                }

                for (String strRow : arrFAQ) {
                    if (strRow.length() >= 3 && strRow.startsWith("###")) {
                        sb.append(Constants.HTML_BR).append(Constants.HTML_H1_START).append(strRow.substring(3)).append(Constants.HTML_H1_END);
                    } else if (strRow.length() >= 2 && strRow.startsWith(Constants.STRING_2HASH)) {
                        sb.append(Constants.HTML_BR).append(Constants.HTML_H2_START).append(strRow.substring(2)).append(Constants.HTML_H2_END);
                    } else if (strRow.startsWith(Constants.STRING_HASH)) {
                        sb.append(Constants.HTML_H3_START).append(strRow.substring(1)).append(Constants.HTML_H3_END);
                    } else {
                        sb.append(strRow).append(Constants.HTML_BR);
                    }
                }

                Intent intent = getIntent();
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String anchor;
                    anchor = extras.getString(Constants.EXTRA_ANCHOR, Constants.STRING_EMPTY);
                    if (!anchor.isEmpty()) {
                        // https://stackoverflow.com/questions/14062901/webview-jump-to-anchor-using-loaddatawithbaseurl
                        webView.getSettings().setJavaScriptEnabled(true);
                        sb.append(Constants.ANCHOR_LINK_START).append(anchor).append(Constants.ANCHOR_LINK_END);
                    }
                }
                sb.append("</body></html>");

                webView.loadDataWithBaseURL(
                        Constants.DRAWABLE_BASE_URL,
                        sb.toString(),
                        Constants.CHARSET_HTML_UTF_8,
                        Constants.CHARSET_UTF_8,
                        null
                );
            }

            Button buttonMail = findViewById(R.id.buttonMail);
            buttonMail.setText(R.string.button_question);
            buttonMail.setOnClickListener(view -> {
                try {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(Constants.MAILTO_TEMPLATE + getString(R.string.app_name) + "%20"
                            + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + ")")));
                } catch (android.content.ActivityNotFoundException e) { /**/ }
                finish();
            });

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (ta != null) ta.recycle();
        }
    }

}
