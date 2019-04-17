package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent_In) {

        final String action = intent_In.getAction();

        if (action != null && (
                action.equalsIgnoreCase("android.intent.action.TIME_SET") ||
                action.equalsIgnoreCase("android.intent.action.DATE_CHANGED") ||
                action.equalsIgnoreCase("android.intent.action.TIMEZONE_CHANGED"))
        ) {

            //Посылаем сообщения на обновление виджетов

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.context = context;
            eventsData.updateWidgets();

        }

    }
}
