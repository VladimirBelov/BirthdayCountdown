/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:04
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.core.app.NotificationManagerCompat;

/**
 * NotifyActionReceiver - это BroadcastReceiver, отвечающий за обработку действий,
 * вызванных из уведомлений. Он получает интенты с определенными действиями
 * (например, snooze, silent, hide, notify, share, attach, dial, close) и выполняет
 * соответствующие операции.
 *
 * <p>Этот класс взаимодействует с {@link ContactsEvents} для управления данными
 * событий и настройками. Он также использует {@link NotificationManagerCompat}
 * для управления уведомлениями.
 *
 * <p>Действия, обрабатываемые этим приемником:
 * <ul>
 *   <li>{@link Constants#ACTION_SNOOZE}: Откладывает (snooze) уведомление для
 *       определенного события.</li>
 *   <li>{@link Constants#ACTION_SILENT}: Отключает звук уведомления для
 *       определенного события.</li>
 *   <li>{@link Constants#ACTION_HIDE}: Скрывает уведомление для определенного
 *       события.</li>
 *   <li>{@link Constants#ACTION_NOTIFY}: Повторно показывает уведомление для
 *       определенного события.</li>
 *   <li>{@link Constants#ACTION_SHARE}: Запускает активность для обмена данными
 *       о событии.</li>
 *   <li>{@link Constants#ACTION_ATTACH}: Закрепляет уведомление для предотвращения удаления
 *      при очистке всех уведомлений пользователем</li>
 *   <li>{@link Constants#ACTION_DIAL}: Инициирует телефонный вызов контакту,
 *       связанному с событием.</li>
 *   <li>{@link Constants#ACTION_CLOSE}: Закрывает уведомление.</li>
 * </ul>
 */
public class NotifyActionReceiver extends BroadcastReceiver {

    private static final String TAG = "ActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            final String action = intent.getAction();
            if (action == null) return;

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context.getApplicationContext());
            eventsData.getPreferences();
            eventsData.setLocale(true);

            //Получаем входные параметры
            Bundle extras = intent.getExtras();
            int notificationID = 0;
            String notificationData = Constants.STRING_EMPTY;
            String[] notificationActions = null;
            String[] notificationDetails = null;
            String[] singleEventArray = null;
            String eventKey = Constants.STRING_EMPTY;
            String eventKeyWithRawId = Constants.STRING_EMPTY;

            if (extras != null) {
                notificationID = extras.getInt(Constants.EXTRA_NOTIFICATION_ID, 0);
                notificationData = extras.getString(Constants.EXTRA_NOTIFICATION_DATA, Constants.STRING_EMPTY);

                if (!notificationData.equals(Constants.STRING_EMPTY)) {
                    singleEventArray = notificationData.split(Constants.STRING_EOT, -1);
                    if (singleEventArray.length == ContactsEvents.Position_attrAmount) {
                        eventKey = eventsData.getEventKey(singleEventArray);
                        eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                    }
                }

                notificationActions = extras.getStringArray(Constants.EXTRA_NOTIFICATION_ACTIONS);
                notificationDetails = extras.getStringArray(Constants.EXTRA_NOTIFICATION_DETAILS);
            }

            if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                ToastExpander.showDebugMsg(context, eventsData.getContext().getString(R.string.msg_debug_notify_action_empty, action));
                return;
            } else {
                ToastExpander.showDebugMsg(context, action.concat(Constants.STRING_COLON_SPACE).concat(notificationData));
            }

            if (action.equalsIgnoreCase(Constants.ACTION_SNOOZE)) {

                //https://stackoverflow.com/questions/5746582/implementing-snooze-in-android-notifications
                //https://stackoverflow.com/questions/44232699/specific-snooze-functionality-in-notification-button
                eventsData.snoozeNotification(notificationData, notificationDetails, notificationActions, 1, null);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if  (action.equalsIgnoreCase(Constants.ACTION_SILENT)) {

                eventsData.setSilencedEvent(eventKey, eventKeyWithRawId);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if  (action.equalsIgnoreCase(Constants.ACTION_HIDE)) {

                eventsData.setHiddenEvent(eventKey, eventKeyWithRawId);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if (action.equalsIgnoreCase(Constants.ACTION_NOTIFY)) {

                eventsData.showNotification(
                        notificationData,
                        notificationActions,
                        notificationDetails,
                        Integer.toString(eventsData.preferences_notifications_channel_id),
                        false
                );

            } else if (action.equalsIgnoreCase(Constants.ACTION_SHARE)) {

                Intent intentShare = new Intent(context, ShareFromNotifyActivity.class);
                intentShare.putExtra(Intent.EXTRA_TEXT, notificationData);
                intentShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {
                    context.startActivity(intentShare);
                } catch (android.content.ActivityNotFoundException e) { /**/ }

            } else if (action.equalsIgnoreCase(Constants.ACTION_ATTACH)) {

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

                eventsData.showNotification(
                        notificationData,
                        notificationActions,
                        notificationDetails,
                        Integer.toString(eventsData.preferences_notifications_channel_id),
                        true
                );

            } else if (action.equalsIgnoreCase(Constants.ACTION_DIAL)) {

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

                if (!singleEventArray[ContactsEvents.Position_contactID].isEmpty()) {
                    String phone = eventsData.getContactPhone(Long.parseLong(singleEventArray[ContactsEvents.Position_contactID]));
                    if (!phone.equals(Constants.STRING_EMPTY)) {
                        //https://stackoverflow.com/questions/4275678/how-to-make-a-phone-call-using-intent-in-android
                        Intent intentDial = new Intent(Intent.ACTION_DIAL);
                        intentDial.setData(Uri.parse(WebView.SCHEME_TEL.concat(Uri.encode(phone.trim()))));
                        intentDial.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            context.startActivity(intentDial);
                        } catch (android.content.ActivityNotFoundException e) { /**/ }
                    }
                }

            } else if (action.equalsIgnoreCase(Constants.ACTION_CLOSE)) {

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}
