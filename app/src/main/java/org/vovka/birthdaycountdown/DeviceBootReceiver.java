/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 25.06.2022, 1:08
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