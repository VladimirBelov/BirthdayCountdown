/*
 * *
 *  * Created by Vladimir Belov on 22.03.20 23:03
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 18.03.20 23:08
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import static org.vovka.birthdaycountdown.Constants.EXTRA_NOTIFICATION_DATA;
import static org.vovka.birthdaycountdown.Constants.EXTRA_NOTIFICATION_ID;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

public class AlarmReceiver extends BroadcastReceiver {

    private ContactsEvents eventsData;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            final String action = intent.getAction();
            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.setLocale(true);

            if (action != null && action.equalsIgnoreCase(Constants.ACTION_SNOOZE)) {

                Bundle extras = intent.getExtras();
                int notificationID = 0;
                String notificationData = STRING_EMPTY;
                if (extras != null) {
                    notificationID = extras.getInt(EXTRA_NOTIFICATION_ID, 0);
                    notificationData = extras.getString(EXTRA_NOTIFICATION_DATA, STRING_EMPTY);
                }
                if (notificationID == 0 || notificationData.equals(STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.ACTION_SNOOZE + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
                    return;
                }

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

                //https://stackoverflow.com/questions/5746582/implementing-snooze-in-android-notifications
                //https://stackoverflow.com/questions/44232699/specific-snooze-functionality-in-notification-button
                eventsData.snoozeNotification(notificationData, 1, null);

            } else if (action != null && action.equalsIgnoreCase(Constants.ACTION_NOTIFY)) {

                Bundle extras = intent.getExtras();
                String notificationData = STRING_EMPTY;
                if (extras != null) {
                    notificationData = extras.getString(EXTRA_NOTIFICATION_DATA, STRING_EMPTY);
                }
                if (notificationData.equals(STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.ACTION_NOTIFY + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
                    return;
                }

                eventsData.showNotification(notificationData, Integer.toString(eventsData.preferences_notification_channel_id));

            } else {

                if (eventsData.getContactsEvents(context)) {
                    eventsData.computeDates();
                    if (eventsData.preferences_notifications_days >= 0)
                        eventsData.showNotifications(false, Integer.toString(eventsData.preferences_notification_channel_id));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.ALARM_RECEIVER_ON_RECEIVE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}