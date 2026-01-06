/*
 * *
 *  * Created by Vladimir Belov on 07.01.2026, 01:04
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 01.01.2026, 22:47
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

/**
 * NotifyActivity - это служебная Activity, которая вызывается только
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
            eventsData.initLanguage(this);

            boolean isNeedNotify = !eventsData.preferences_notifications_days.isEmpty();
            boolean isNeedNotify2 = !eventsData.preferences_notifications2_days.isEmpty();

            if (isNeedNotify || isNeedNotify2) {
                if (eventsData.getEvents()) {

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
