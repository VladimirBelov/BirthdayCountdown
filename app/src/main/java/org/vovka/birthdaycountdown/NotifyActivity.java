/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 25.06.2022, 1:08
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class NotifyActivity extends Activity {

    private static final String TAG = "NotifyActivity";
    private ContactsEvents eventsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.setLocale(true);

            if (eventsData.getEvents(null)) {
                eventsData.computeDates();
                if (eventsData.preferences_notifications_days.size() != 0) {

                    eventsData.showNotifications(true, Integer.toString(eventsData.preferences_notification_channel_id));

                } else {

                    Toast.makeText(this, R.string.msg_notifications_disabled, Toast.LENGTH_LONG).show();

                }
            }

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
