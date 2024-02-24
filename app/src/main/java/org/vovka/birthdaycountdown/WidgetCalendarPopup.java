/*
 * *
 *  * Created by Vladimir Belov on 21.02.2024, 20:48
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 21.02.2024, 20:48
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

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
                    txtInfo.setText(dayInfo);
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
                    buttonCalendar.setText(getString(R.string.event_type_other_emoji).concat(getString(R.string.appwidget_label_Calendar)));
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
