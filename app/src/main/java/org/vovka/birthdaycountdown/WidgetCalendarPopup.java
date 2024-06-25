/*
 * *
 *  * Created by Vladimir Belov on 21.02.2024, 20:48
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 21.02.2024, 20:48
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.util.Locale;

import androidx.annotation.Nullable;

public class WidgetCalendarPopup extends Activity {

    private static final String TAG = "WidgetCalendarPopup";
    ContactsEvents eventsData;

    public WidgetCalendarPopup() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
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

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.activity_popup);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            String dayInfo = null;
            String dayCaption = null;
            @Nullable String dayMills;
            if (extras != null) {
                dayInfo = extras.getString(Constants.EXTRA_DAY_INFO);
                dayCaption = extras.getString(Constants.EXTRA_DAY_CAPTION);
                dayMills = extras.getString(Constants.EXTRA_VALUES);
            } else {
                dayMills = null;
            }
            if (!TextUtils.isEmpty(dayInfo)) {
                TextView txtInfo = findViewById(R.id.textInfo);
                if (txtInfo != null) {
                    if (dayInfo.contains(Constants.TRANSPARENT)) {
                        TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                        dayInfo = dayInfo.replaceAll(Constants.TRANSPARENT,
                                Integer.toHexString(ta.getColor(R.styleable.Theme_backgroundColor, 0)  & 0x00ffffff));
                    }
                    txtInfo.setText(Html.fromHtml(dayInfo));
                }
            } else {
                ToastExpander.showInfoMsg(getApplicationContext(), "No extras!");
                finish();
            }
            if (!TextUtils.isEmpty(dayCaption)) {
                TextView txtCaption = findViewById(R.id.textCaption);
                if (txtCaption != null) {
                    txtCaption.setText(dayCaption);
                }
            }

            if (dayMills != null) {
                TextView buttonCalendar = findViewById(R.id.buttonCalendar);
                if (buttonCalendar != null) {
                    buttonCalendar.setText(getString(R.string.event_type_other_emoji).concat(Constants.STRING_SPACE).concat(getString(R.string.appwidget_label_Calendar)));
                    buttonCalendar.setOnClickListener(view -> {
                        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                        builder.appendPath("time");
                        builder.appendPath(dayMills);
                        Intent calendarIntent = new Intent(Intent.ACTION_VIEW, builder.build());
                        calendarIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(calendarIntent);
                        finish();
                    });
                }
            }

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText("X");
                buttonClose.setOnClickListener(view -> finish());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
