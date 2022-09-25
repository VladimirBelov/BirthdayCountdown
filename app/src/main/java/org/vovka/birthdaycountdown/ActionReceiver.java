/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 25.06.2022, 1:08
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

public class ActionReceiver extends BroadcastReceiver {

    private static final String TAG = "ActionReceiver";
    private ContactsEvents eventsData;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            final String action = intent.getAction();
            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.setLocale(true);

            //Получаем входные параметры
            Bundle extras = intent.getExtras();
            int notificationID = 0;
            String notificationData = Constants.STRING_EMPTY;
            String[] singleEventArray = null;
            String eventKey = Constants.STRING_EMPTY;

            if (extras != null) {
                notificationID = extras.getInt(Constants.EXTRA_NOTIFICATION_ID, 0);
                notificationData = extras.getString(Constants.EXTRA_NOTIFICATION_DATA, Constants.STRING_EMPTY);

                if (!notificationData.equals(Constants.STRING_EMPTY)) {
                    singleEventArray = notificationData.split(Constants.STRING_EOT, -1);
                    eventKey = eventsData.getEventKey(singleEventArray);
                }
            }

            if (action != null && action.equalsIgnoreCase(Constants.ACTION_SNOOZE)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on)
                        Toast.makeText(context, Constants.ACTION_SNOOZE + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
                    return;
                }

                //https://stackoverflow.com/questions/5746582/implementing-snooze-in-android-notifications
                //https://stackoverflow.com/questions/44232699/specific-snooze-functionality-in-notification-button
                eventsData.snoozeNotification(notificationData, 1, null);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if  (action != null && action.equalsIgnoreCase(Constants.ACTION_SILENT)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on)
                        Toast.makeText(context, Constants.ACTION_SILENT + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
                    return;
                }

                eventsData.setSilencedEvent(eventKey);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if  (action != null && action.equalsIgnoreCase(Constants.ACTION_HIDE)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on)
                        Toast.makeText(context, Constants.ACTION_HIDE + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
                    return;
                }

                eventsData.setHiddenEvent(eventKey);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            } else if (action != null && action.equalsIgnoreCase(Constants.ACTION_NOTIFY)) {

                if (notificationData.equals(Constants.STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.ACTION_NOTIFY + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
                    return;
                }

                eventsData.showNotification(notificationData, Integer.toString(eventsData.preferences_notification_channel_id));

            } else if (action != null && action.equalsIgnoreCase(Constants.ACTION_DIAL)) {

                if (notificationID == 0 || notificationData.equals(Constants.STRING_EMPTY)) {
                    if (eventsData.preferences_debug_on)
                        Toast.makeText(context, Constants.ACTION_HIDE + Constants.STRING_COLON_SPACE + "Empty request", Toast.LENGTH_LONG).show();
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

            } else if (action != null && action.equalsIgnoreCase(Constants.ACTION_CLOSE)) {

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationID);

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}
