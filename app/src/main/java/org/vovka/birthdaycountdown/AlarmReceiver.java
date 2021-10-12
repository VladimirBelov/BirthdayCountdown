/*
 * *
 *  * Created by Vladimir Belov on 12.10.2021, 0:19
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 12.10.2021, 0:16
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    private ContactsEvents eventsData;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.getPreferences();
            eventsData.setLocale(true);

            if (eventsData.preferences_notifications_days.size() != 0) {

                eventsData.setLocale(true);
                if (eventsData.getEvents(context)) {
                    eventsData.computeDates();

                    eventsData.showNotifications(false, Integer.toString(eventsData.preferences_notification_channel_id));
                    //eventsData.updateWidgets();
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.ALARM_RECEIVER_ON_RECEIVE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}