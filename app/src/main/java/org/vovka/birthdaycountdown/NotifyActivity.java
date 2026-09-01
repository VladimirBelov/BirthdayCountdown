/*
 * *
 *  * Created by Vladimir Belov on 01.09.2026, 03:49
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 01.09.2026, 03:09
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import org.vovka.birthdaycountdown.utils.StringUtils;

/**
 * NotifyActivity - это служебная Activity, которая вызывается только
 * через ярлык на значке приложения, и ее основная цель - обработка логики уведомлений,
 * а не отображение пользовательского интерфейса. Она немедленно завершает работу после обработки логики уведомлений.
 */
public final class NotifyActivity extends Activity {

    private static final String TAG = "NotifyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);
            boolean isNeedNotify = !eventsData.preferences_notifications_days.isEmpty();
            boolean isNeedNotify2 = eventsData.isFeatureEnabled(Constants.FEATURE_NOTIFY_Q2) && !eventsData.preferences_notifications2_days.isEmpty();

            if (isNeedNotify || isNeedNotify2) {
                // Загружаем события асинхронно.
                // Колбэк выполнится в фоне, даже когда Activity уже будет уничтожена.
                eventsData.getEventsAsync(() -> {
                    //Этот класс используется только для shortcut на иконке. Текущие уведомления не нужны
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.cancelAll();
                    if (isNeedNotify) {
                        eventsData.showNotifications(1, true, Integer.toString(eventsData.preferences_notifications_channel_id));
                    }
                    if (isNeedNotify2) {
                        eventsData.showNotifications(2, true, Integer.toString(eventsData.preferences_notifications2_channel_id));
                    }
                });
            } else {
                // Используем Application Context, так как Activity сразу закроется
                ToastExpander.showInfoMsg(getApplicationContext(), getString(R.string.msg_notifications_disabled));
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getApplicationContext(), StringUtils.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        // ВАЖНО: finish() должен вызываться синхронно в конце onCreate(),
        // иначе Android выбросит IllegalStateException для Activity без UI.
        finish();
    }
}
