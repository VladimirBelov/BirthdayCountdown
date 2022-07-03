/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 22.11.2021, 22:34
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

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
                eventsData.initNotifications(log);
                eventsData.initWidgetUpdate(log);

                if (eventsData.preferences_debug_on && log.length() > 0) Toast.makeText(context, log.toString(), Toast.LENGTH_LONG).show();

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}