/*
 * *
 *  * Created by Vladimir Belov on 05.06.2025, 00:35
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 05.06.2025, 00:07
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

/**
 * Класс {@code DeviceBootReceiver} является {@link BroadcastReceiver}, который прослушивает широковещательное сообщение
 * {@link Intent#ACTION_BOOT_COMPLETED}, срабатывающее, когда устройство завершает загрузку.
 * После получения этого сообщения он повторно инициализирует и планирует оповещения (alarms) для уведомлений,
 * связанных с контактами, и обновляет виджеты, если они были установлены ранее.
 *
 * <p>
 * Этот приемник гарантирует, что запланированные уведомления и обновления виджетов сохраняются после перезагрузки устройства.
 * Он использует синглтон {@link ContactsEvents} для управления данными и настройками приложения.
 * </p>
 *
 * <p>
 * При завершении загрузки устройства он выполняет следующие действия:
 * <ol>
 *   <li>Инициализирует контекст экземпляра {@link ContactsEvents}, если он равен null.</li>
 *   <li>Загружает пользовательские настройки из общих настроек (shared preferences).</li>
 *   <li>Устанавливает локаль для приложения.</li>
 *   <li>Сбрасывает расписания уведомлений для основного и дополнительного уведомления (если они включены), используя
 *   {@link ContactsEvents#initNotificationSchedule}.</li>
 *   <li>Инициирует обновления виджетов, используя {@link ContactsEvents#initWidgetUpdate(StringBuilder)}.</li>
 *   <li>Отображает отладочное сообщение через {@link ToastExpander#showDebugMsg(Context, String)}, если в процессе инициализации были созданы какие-либо логи.</li>
 * </ol>
 * </p>
 *
 * <p>
 * В случае возникновения исключения в процессе, он запишет подробности ошибки в лог и покажет отладочное сообщение.
 * </p>
 *
 */
public class DeviceBootReceiver extends BroadcastReceiver {

    private static final String TAG = "DeviceBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        ContactsEvents eventsData = ContactsEvents.getInstance();
        try {
            if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) { // on device boot complete, reset the alarm

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