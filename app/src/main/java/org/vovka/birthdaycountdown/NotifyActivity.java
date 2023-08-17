/*
 * *
 *  * Created by Vladimir Belov on 18.08.2023, 00:50
 *  * Copyright (c) 2018 - 2023. All rights reserved.
 *  * Last modified 30.07.2023, 12:41
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class NotifyActivity extends Activity {

    private static final String TAG = "NotifyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.setLocale(true);

            if (eventsData.getEvents(null)) {
                eventsData.computeDates();
                if (eventsData.preferences_notifications_days.size() != 0) {

                    eventsData.showNotifications(true, Integer.toString(eventsData.preferences_notification_channel_id));

                } else {

                    ToastExpander.showInfoMsg(this, getString(R.string.msg_notifications_disabled));

                }
            }

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
