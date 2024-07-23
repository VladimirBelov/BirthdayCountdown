/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 10.10.2023, 16:23
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            StringBuilder log = new StringBuilder();

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);
            eventsData.getEvents(context);

            //Переинициализируем обновления виджетов
            eventsData.initWidgetUpdate(log);

            //Посылаем сообщения на обновление виджетов
            eventsData.updateWidgets(0, log);

            if (log.length() > 0) ToastExpander.showDebugMsg(context, log.toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
