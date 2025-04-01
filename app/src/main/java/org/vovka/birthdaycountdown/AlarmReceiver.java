/*
 * *
 *  * Created by Vladimir Belov on 01.04.2025, 12:43
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 01.04.2025, 12:22
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Класс AlarmReceiver является BroadcastReceiver, который обрабатывает события будильника,
 * срабатывающие AlarmManager системы. Он отвечает за проверку наличия ожидающих событий и
 * отображение уведомлений, а также за обновление виджетов приложения.
 *
 * <p>
 * Этот приемник обычно вызывается, когда срабатывает ранее установленный будильник. Он проверяет
 * настройки приложения, чтобы определить, нужно ли отображать уведомления для различных очередей
 * уведомлений (например, очередь 1 или очередь 2).
 * </p>
 * <p>
 * Этот класс в значительной степени полагается на синглтон {@link ContactsEvents} для управления
 * событиями, настройками и уведомлениями.
 * </p>
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            StringBuilder log = new StringBuilder();

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            boolean isNeedNotify = !eventsData.preferences_notifications_days.isEmpty();
            boolean isNeedNotify2 = !eventsData.preferences_notifications2_days.isEmpty();

            if (isNeedNotify || isNeedNotify2) {
                if (eventsData.getEvents(context)) {

                    long today = System.currentTimeMillis();

                    if (isNeedNotify && intent.getIntExtra(Constants.QUEUE, 0) == 1) {
                        long lastNotify = eventsData.getLastNotifyForQueue(1);
                        if (lastNotify == 0 || today - lastNotify > Constants.TIME_NOTIFY_COOLDOWN) {
                            eventsData.showNotifications(1, false, Integer.toString(eventsData.preferences_notifications_channel_id));

                            eventsData.setLastNotifyForQueue(1, today);

                            //Переинициализируем уведомления
                            eventsData.initNotificationSchedule(log,
                                    1,
                                    eventsData.preferences_notifications_days,
                                    eventsData.preferences_notifications_alarm_hour,
                                    eventsData.preferences_notifications_alarm_minute);
                        } else {
                            log.append(eventsData.getResources().getString(R.string.msg_skipped_notification, 1));
                        }
                    }

                    if (isNeedNotify2 && intent.getIntExtra(Constants.QUEUE, 0) == 2) {
                        long lastNotify = eventsData.getLastNotifyForQueue(2);
                        if (lastNotify == 0 || today - lastNotify > Constants.TIME_NOTIFY_COOLDOWN) {
                            eventsData.showNotifications(2, false, Integer.toString(eventsData.preferences_notifications2_channel_id));

                            eventsData.setLastNotifyForQueue(2, today);

                            //Переинициализируем уведомления
                            eventsData.initNotificationSchedule(log,
                                    2,
                                    eventsData.preferences_notifications2_days,
                                    eventsData.preferences_notifications2_alarm_hour,
                                    eventsData.preferences_notifications2_alarm_minute);
                        } else {
                            log.append(eventsData.getResources().getString(R.string.msg_skipped_notification, 2));
                        }
                    }
                }
            }

            //Переинициализируем обновления виджетов
            eventsData.initWidgetUpdate(log);

            //Посылаем сообщения на обновление виджетов
            eventsData.updateWidgets(0, log);

            if (log.length() > 0) ToastExpander.showDebugMsg(context, log.deleteCharAt(log.length() - 1).toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}