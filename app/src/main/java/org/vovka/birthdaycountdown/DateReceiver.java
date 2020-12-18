/*
 * *
 *  * Created by Vladimir Belov on 17.12.20 22:05
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 27.10.20 0:43
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import static org.vovka.birthdaycountdown.Constants.STRING_EOF;

public class DateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent_In) {

        final String action = intent_In.getAction();

        if (action != null && (
                action.equalsIgnoreCase(Constants.Broadcast_ANDROID_INTENT_ACTION_TIME_SET) ||
                        action.equalsIgnoreCase(Constants.Broadcast_ANDROID_INTENT_ACTION_DATE_CHANGED) ||
                        action.equalsIgnoreCase(Constants.Broadcast_ANDROID_INTENT_ACTION_TIMEZONE_CHANGED))
        ) {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            try {

                StringBuilder log = new StringBuilder();

                if (eventsData.context == null) eventsData.context = context;
                eventsData.getPreferences();

                //Переинициализируем уведомления
                eventsData.initNotifications(log);

                //Посылаем сообщения на обновление виджетов
                eventsData.updateWidgets();
                log.append(Constants.MSG_SENT_WIDGETS_UPDATE_REQUEST).append(STRING_EOF);

                if (eventsData.preferences_debug_on && log.length() > 0) Toast.makeText(context, log.toString(), Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.DATE_RECEIVER_ON_RECEIVE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            }

        }

    }
}
