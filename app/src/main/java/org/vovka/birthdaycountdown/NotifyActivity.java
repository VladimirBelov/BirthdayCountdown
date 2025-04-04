/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 08:08
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

/**
 * NotifyActivity - это служебная Activity, предназначенная для управления и отображения уведомлений,
 * связанных с событиями контактов. Она проверяет настройки пользователя для параметров уведомлений и
 * запускает отображение уведомлений, если они включены. Эта Activity вызывается только
 * через ярлык на значке приложения, и ее основная цель - обработка логики уведомлений,
 * а не отображение пользовательского интерфейса. Она немедленно завершает работу после обработки логики уведомлений.
 */
public final class NotifyActivity extends Activity {

    private static final String TAG = "NotifyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();
            eventsData.setLocale(true);

            boolean isNeedNotify = !eventsData.preferences_notifications_days.isEmpty();
            boolean isNeedNotify2 = !eventsData.preferences_notifications2_days.isEmpty();

            if (isNeedNotify || isNeedNotify2) {
                if (eventsData.getEvents(null)) {

                    //Этот класс используется только для shortcut на иконке. Текущие уведомления не нужны
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.cancelAll();

                    if (isNeedNotify) {
                        eventsData.showNotifications(1, true, Integer.toString(eventsData.preferences_notifications_channel_id));
                    }
                    if (isNeedNotify2) {
                        eventsData.showNotifications(2, true, Integer.toString(eventsData.preferences_notifications2_channel_id));
                    }
                }

            } else {

                ToastExpander.showInfoMsg(this, getString(R.string.msg_notifications_disabled));

            }

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
