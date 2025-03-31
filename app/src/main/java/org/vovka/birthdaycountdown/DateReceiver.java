/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 09:07
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Класс DateReceiver является BroadcastReceiver, который отслеживает системные изменения даты и времени.
 * Он конкретно отслеживает следующие действия:
 * <ul>
 *     <li>{@link Constants#Broadcast_ANDROID_INTENT_ACTION_TIME_SET}: Когда устанавливается системное время.</li>
 *     <li>{@link Constants#Broadcast_ANDROID_INTENT_ACTION_DATE_CHANGED}: Когда меняется системная дата.</li>
 *     <li>{@link Constants#Broadcast_ANDROID_INTENT_ACTION_TIMEZONE_CHANGED}: Когда меняется системный часовой пояс.</li>
 * </ul>
 * <p>
 * При получении любого из этих широковещательных сообщений (broadcast), DateReceiver запускает повторную инициализацию
 * расписаний уведомлений и обновлений виджетов внутри приложения. Это гарантирует, что запланированные
 * уведомления приложения и отображение виджетов остаются точными и синхронизированными с системной датой и временем.
 * </p>
 * <p>
 * Он использует синглтон-класс {@link ContactsEvents} для управления состоянием приложения,
 * настройками, расписаниями уведомлений и обновлениями виджетов.
 * </p>
 */
public class DateReceiver extends BroadcastReceiver {

    private static final String TAG = "DateReceiver";

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
}
