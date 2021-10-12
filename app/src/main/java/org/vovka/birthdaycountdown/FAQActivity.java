/*
 * *
 *  * Created by Vladimir Belov on 12.10.2021, 0:19
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 12.10.2021, 0:16
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import static org.vovka.birthdaycountdown.Constants.HTML_BR;
import static org.vovka.birthdaycountdown.Constants.HTML_H1_END;
import static org.vovka.birthdaycountdown.Constants.HTML_H1_START;
import static org.vovka.birthdaycountdown.Constants.HTML_H2_END;
import static org.vovka.birthdaycountdown.Constants.HTML_H2_START;
import static org.vovka.birthdaycountdown.Constants.HTML_H3_END;
import static org.vovka.birthdaycountdown.Constants.HTML_H3_START;

public class FAQActivity extends AppCompatActivity {

    @SuppressLint("PrivateResource")
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            this.setTheme(eventsData.preferences_theme.themeMain);
            eventsData.setLocale(true);

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
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_material);
            }

            eventsData.setLocale(true); //Без этого на Android 9+ при первом показе webview грузит дефолтный язык
            WebView webView = findViewById(R.id.webView);
            if (webView != null) {
                webView.setVerticalScrollBarEnabled(true);
                webView.setBackgroundColor(Color.TRANSPARENT);
            }

            findViewById(R.id.buttonMail).setOnClickListener(view -> {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:belov.vladimir@mail.ru?subject=" + getString(R.string.app_name) + "%20" + BuildConfig.VERSION_NAME + Constants.STRING_PARENTHESIS_OPEN + BuildConfig.VERSION_CODE + ")")));
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
                        sb.append(HTML_BR).append(HTML_H1_START).append(strRow.substring(3)).append(HTML_H1_END);
                    } else if (strRow.length() >= 2 && strRow.startsWith("##")) {
                        sb.append(HTML_BR).append(HTML_H2_START).append(strRow.substring(2)).append(HTML_H2_END);
                    } else if (strRow.length() >= 1 && strRow.startsWith("#")) {
                        sb.append(HTML_H3_START).append(strRow.substring(1)).append(HTML_H3_END);
                    } else {
                        sb.append(strRow).append(HTML_BR);
                    }
                }
            }
            sb.append("</body></html>");

            if (webView != null) {
                webView.loadDataWithBaseURL("file:///android_res/drawable/", sb.toString(), "text/html; charset=utf-8", "utf-8", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.ABOUT_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

}
