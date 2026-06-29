/*
 * *
 *  * Created by Vladimir Belov on 30.06.2026, 00:18
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 29.06.2026, 23:47
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.vovka.birthdaycountdown.utils.StringUtils;

/**
 * {@code WidgetUpdateDailyReceiver} - это {@link BroadcastReceiver}, который обрабатывает ежедневные обновления виджетов.
 * <p>
 * Этот приемник предназначен для запуска по расписанию (например, через AlarmManager) или другим механизмом через
 * регулярные интервалы (например, ежедневно), чтобы гарантировать, что виджеты отображают актуальную информацию.
 * Он выполняет следующие действия:
 * </p>
 * <ul>
 *     <li>Инициализирует или переинициализирует источник данных для обновлений виджетов.</li>
 *     <li>Получает самые свежие данные из {@link ContactsEvents}.</li>
 *     <li>Запускает фактическое обновление виджетов.</li>
 *     <li>Опционально отображает отладочные сообщения, если в процессе обновления были сгенерированы какие-либо логи.</li>
 *     <li>Обрабатывает исключения, которые могут возникнуть во время обновления, регистрирует ошибки и отображает их.</li>
 * </ul>
 * <p>
 * Приемник взаимодействует с классом {@link ContactsEvents} для управления данными и запуска обновлений виджетов.
 * </p>
 */
public class WidgetUpdateDailyReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetUpdateDReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            StringBuilder log = new StringBuilder();

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(context);

            eventsData.getEvents();

            //Посылаем сообщения на обновление виджетов
            eventsData.updateWidgets(0, log);

            //Переинициализируем обновления виджетов
            eventsData.initWidgetUpdate(log);

            if (log.length() > 0) ToastExpander.showDebugMsg(context, log.toString());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
