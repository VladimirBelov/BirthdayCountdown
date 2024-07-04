/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 23.08.2023, 19:34
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class ActionReceiver extends BroadcastReceiver {

    private static final String TAG = "ActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            final String action = intent.getAction();
            if (action == null) return;

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.setLocale(true);

            //Получаем входные параметры
            Bundle extras = intent.getExtras();
            int notificationID = 0;
            String notificationData = Constants.STRING_EMPTY;
            String[] singleEventArray = null;
            String eventKey = Constants.STRING_EMPTY;
            String eventKeyWithRawId = Constants.STRING_EMPTY;

            if (extras != null) {
                notificationID = extras.getInt(Constants.EXTRA_NOTIFICATION_ID, 0);
                notificationData = extras.getString(Constants.EXTRA_NOTIFICATION_DATA, Constants.STRING_EMPTY);

                if (!notificationData.equals(Constants.STRING_EMPTY)) {
                    singleEventArray = notificationData.split(Constants.STRING_EOT, -1);
                    eventKey = eventsData.getEventKey(singleEventArray);
                    eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                }
            }

            if (action.equalsIgnoreCase(Constants.ACTION_SNOOZE)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    ToastExpander.showInfoMsg(context, Constants.ACTION_SNOOZE + Constants.STRING_COLON_SPACE + "Empty request");
                    return;
                }

                //https://stackoverflow.com/questions/5746582/implementing-snooze-in-android-notifications
                //https://stackoverflow.com/questions/44232699/specific-snooze-functionality-in-notification-button
                eventsData.snoozeNotification(notificationData, 1, null);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if  (action.equalsIgnoreCase(Constants.ACTION_SILENT)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    ToastExpander.showInfoMsg(context, Constants.ACTION_SILENT + Constants.STRING_COLON_SPACE + "Empty request");
                    return;
                }

                eventsData.setSilencedEvent(eventKey, eventKeyWithRawId);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if  (action.equalsIgnoreCase(Constants.ACTION_HIDE)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    ToastExpander.showInfoMsg(context, Constants.ACTION_HIDE + Constants.STRING_COLON_SPACE + "Empty request");
                    return;
                }

                eventsData.setHiddenEvent(eventKey, eventKeyWithRawId);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if (action.equalsIgnoreCase(Constants.ACTION_NOTIFY)) {

                if (notificationData.equals(Constants.STRING_EMPTY)) {
                    ToastExpander.showInfoMsg(context, Constants.ACTION_NOTIFY + Constants.STRING_COLON_SPACE + "Empty request");
                    return;
                }

                String[] notificationActions = extras.getStringArray(Constants.EXTRA_NOTIFICATION_ACTIONS);
                String[] notificationDetails = extras.getStringArray(Constants.EXTRA_NOTIFICATION_DETAILS);
                eventsData.showNotification(
                        notificationData,
                        notificationActions,
                        notificationDetails,
                        Integer.toString(eventsData.preferences_notifications_channel_id),
                        false
                );

            } else if (action.equalsIgnoreCase(Constants.ACTION_ATTACH)) {

                if (notificationData.equals(Constants.STRING_EMPTY)) {
                    ToastExpander.showInfoMsg(context, Constants.ACTION_NOTIFY + Constants.STRING_COLON_SPACE + "Empty request");
                    return;
                }

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

                String[] notificationActions = extras.getStringArray(Constants.EXTRA_NOTIFICATION_ACTIONS);
                String[] notificationDetails = extras.getStringArray(Constants.EXTRA_NOTIFICATION_DETAILS);
                eventsData.showNotification(
                        notificationData,
                        notificationActions,
                        notificationDetails,
                        Integer.toString(eventsData.preferences_notifications_channel_id),
                        true
                );

            } else if (action.equalsIgnoreCase(Constants.ACTION_DIAL)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    ToastExpander.showInfoMsg(context, Constants.ACTION_HIDE + Constants.STRING_COLON_SPACE + "Empty request");
                    return;
                }

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

                if (!singleEventArray[ContactsEvents.Position_contactID].isEmpty()) {
                    String phone = eventsData.getContactPhone(Long.parseLong(singleEventArray[ContactsEvents.Position_contactID]));
                    if (!phone.equals(Constants.STRING_EMPTY)) {

                        //https://stackoverflow.com/questions/4275678/how-to-make-a-phone-call-using-intent-in-android
                        Intent intentDial = new Intent(Intent.ACTION_DIAL);
                        intentDial.setData(Uri.parse("tel:" + Uri.encode(phone.trim())));
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
