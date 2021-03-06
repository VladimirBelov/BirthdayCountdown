/*
 * *
 *  * Created by Vladimir Belov on 30.06.2021, 13:04
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 30.06.2021, 12:43
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class NotifyActivity extends Activity {

    private ContactsEvents eventsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = getApplicationContext();
            eventsData.setLocale(true);

            if (eventsData.getEvents(eventsData.context)) {
                eventsData.computeDates();
                if (eventsData.preferences_notifications_days.size() != 0) {

                    eventsData.showNotifications(true, Integer.toString(eventsData.preferences_notification_channel_id));

                } else {

                    Toast.makeText(this, R.string.msg_notifications_disabled, Toast.LENGTH_LONG).show();

                }
            }

            finish();

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.NOTIFY_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }
}
