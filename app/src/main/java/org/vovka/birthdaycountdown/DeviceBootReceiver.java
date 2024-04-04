/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 26.11.2023, 20:05
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

public class DeviceBootReceiver extends BroadcastReceiver {

    private static final String TAG = "DeviceBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        ContactsEvents eventsData = ContactsEvents.getInstance();
        try {
            if (Objects.equals(intent.getAction(), Constants.Broadcast_ANDROID_INTENT_ACTION_BOOT_COMPLETED)) { // on device boot complete, reset the alarm

                StringBuilder log = new StringBuilder();
                if (eventsData.getContext() == null) eventsData.setContext(context);
                eventsData.getPreferences();
                eventsData.setLocale(true);

                if (!eventsData.preferences_notifications_days.isEmpty()) {
                    eventsData.initNotificationSchedule(log,
                            1,
                            eventsData.preferences_notifications_days,
                            eventsData.preferences_notifications_alarm_hour,
                            eventsData.preferences_notifications_alarm_minute);
                }
                if (!eventsData.preferences_notifications2_days.isEmpty()) {
                    eventsData.initNotificationSchedule(log,
                            2,
                            eventsData.preferences_notifications2_days,
                            eventsData.preferences_notifications2_alarm_hour,
                            eventsData.preferences_notifications2_alarm_minute);
                }

                eventsData.initWidgetUpdate(log);

                if (log.length() > 0) ToastExpander.showDebugMsg(context, log.toString());

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}