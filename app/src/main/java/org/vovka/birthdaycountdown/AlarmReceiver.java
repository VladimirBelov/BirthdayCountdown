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

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private ContactsEvents eventsData;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            StringBuilder log = new StringBuilder();

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            if (eventsData.preferences_notifications_days.size() != 0) {

                eventsData.setLocale(true);
                if (eventsData.getEvents(context)) {
                    eventsData.computeDates();

                    eventsData.showNotifications(false, Integer.toString(eventsData.preferences_notification_channel_id));

                    //Переинициализируем уведомления
                    eventsData.initNotifications(log);

                }

            }

            //Переинициализируем обновления виджетов
            eventsData.initWidgetUpdate(log);

            //Посылаем сообщения на обновление виджетов
            eventsData.updateWidgets(0);
            log.append(Constants.MSG_SENT_WIDGETS_UPDATE_REQUEST).append(Constants.STRING_EOL);

            if (eventsData.preferences_debug_on && log.length() > 0) Toast.makeText(context, log.toString(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}