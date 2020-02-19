/*
 * *
 *  * Created by Vladimir Belov on 20.02.20 1:25
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 10.02.20 21:53
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try{

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContactsEvents(context)) {
                eventsData.computeDates();
                if (eventsData.preferences_notifications_days >= 0) eventsData.showNotifications(false, Integer.toString(eventsData.preferences_notification_channel_id));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.ALARM_RECEIVER_ON_RECEIVE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}