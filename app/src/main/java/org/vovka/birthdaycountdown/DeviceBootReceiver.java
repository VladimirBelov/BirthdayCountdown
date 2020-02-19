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

import java.util.Objects;

public class DeviceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            if (Objects.equals(intent.getAction(), Constants.Broadcast_ANDROID_INTENT_ACTION_BOOT_COMPLETED)) { // on device boot complete, reset the alarm

                StringBuilder log = new StringBuilder();
                ContactsEvents eventsData = ContactsEvents.getInstance();
                if (eventsData.context == null) eventsData.context = context;
                eventsData.getPreferences();
                eventsData.initNotifications(log);

                if (eventsData.preferences_debug_on && log.length() > 0) Toast.makeText(context, log.toString(), Toast.LENGTH_LONG).show();

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.DEVICE_BOOT_RECEIVER_ON_RECEIVE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}