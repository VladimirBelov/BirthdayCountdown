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
import android.os.Bundle;
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

        //TypedArray ta = null;

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
            if (extras != null) {
                dayInfo = extras.getString(Constants.ACTION_DAY_INFO);
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

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        //} finally {
         //   if (ta != null) ta.recycle();
        }

    }
}
