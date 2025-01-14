/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 23.08.2023, 19:34
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class NotifyActivity extends Activity {

    private static final String TAG = "NotifyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();
            eventsData.setLocale(true);

            boolean isNeedNotify = !eventsData.preferences_notifications_days.isEmpty();
            boolean isNeedNotify2 = !eventsData.preferences_notifications2_days.isEmpty();

            if (isNeedNotify || isNeedNotify2) {
                if (eventsData.getEvents(null)) {

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.cancelAll();

                    if (isNeedNotify) {
                        eventsData.showNotifications(1, true, Integer.toString(eventsData.preferences_notifications_channel_id));
                    }
                    if (isNeedNotify2) {
                        eventsData.showNotifications(2, true, Integer.toString(eventsData.preferences_notifications2_channel_id));
                    }
                }

            } else {

                ToastExpander.showInfoMsg(this, getString(R.string.msg_notifications_disabled));

            }

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
