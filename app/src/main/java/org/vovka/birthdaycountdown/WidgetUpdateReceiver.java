/*
 * *
 *  * Created by Vladimir Belov on 10.06.2026, 11:12
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 10.06.2026, 11:02
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Класс WidgetUpdateReceiver - это BroadcastReceiver, отвечающий за обработку запросов на обновление виджетов.
 * Он прослушивает широковещательные сообщения (broadcast), которые инициируют обновление виджетов приложения.
 * После получения широковещательного сообщения он выполняет необходимые действия для обновления данных виджета и,
 * соответственно, самих виджетов. Это включает в себя получение данных о событиях, инициализацию обновления виджетов и
 * отправку сообщений для обновления виджетов на главном экране пользователя.
 */
public class WidgetUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            StringBuilder log = new StringBuilder();

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.setToday();
            eventsData.initLanguage(context);

            eventsData.getEvents();

            //Посылаем сообщения на обновление виджетов
            eventsData.updateWidgets(0, log);

            //Переинициализируем обновления виджетов (не учитываем Doze)
            eventsData.initWidgetUpdate(log);

            if (log.length() > 0) ToastExpander.showDebugMsg(context, log.toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
