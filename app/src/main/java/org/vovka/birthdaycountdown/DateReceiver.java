/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 22.11.2021, 22:34
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.STRING_EOL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

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

                if (eventsData.getContext() == null) eventsData.setContext(context);
                eventsData.getPreferences();
                eventsData.setLocale(true);

                //Переинициализируем уведомления
                eventsData.initNotifications(log);

                //Переинициализируем обновления виджетов
                eventsData.initWidgetUpdate(log);

                //Посылаем сообщения на обновление виджетов
                eventsData.updateWidgets(0);
                log.append(Constants.MSG_SENT_WIDGETS_UPDATE_REQUEST).append(STRING_EOL);

                if (eventsData.preferences_debug_on && log.length() > 0) Toast.makeText(context, log.toString(), Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.DATE_RECEIVER_ON_RECEIVE_ERROR + e, Toast.LENGTH_LONG).show();
            }

        }

    }
}
