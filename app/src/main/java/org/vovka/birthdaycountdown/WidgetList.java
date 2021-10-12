/*
 * *
 *  * Created by Vladimir Belov on 12.10.2021, 0:19
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 12.10.2021, 0:16
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.ACTION_CLICK;
import static org.vovka.birthdaycountdown.Constants.ACTION_LAUNCH;
import static org.vovka.birthdaycountdown.Constants.DATETIME_DD_MM_YYYY_HH_MM;
import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_EVENT;
import static org.vovka.birthdaycountdown.Constants.PARAM_APP_WIDGET_ID;
import static org.vovka.birthdaycountdown.Constants.STRING_2HASH;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_EOF;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_attrAmount;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_contactID;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventID;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

// Список событий масштабируемый
public class WidgetList extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.getPreferences();
            eventsData.setLocale(true);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);

            //Привязываем адаптер
            Intent adapter = new Intent(context, EventListWidgetService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
            adapter.setData(data); //Чтобы разные виджеты одного адаптера отличались для системы
            views.setRemoteAdapter(R.id.widget_list, adapter);

            views.setTextViewText(R.id.empty_view, context.getString(R.string.msg_no_events));
            views.setEmptyView(R.id.widget_list, R.id.empty_view);

            //Кнопка настроек
            Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
            intentConfig.setAction(ACTION_LAUNCH);
            intentConfig.putExtra(PARAM_APP_WIDGET_ID, appWidgetId);
            views.setOnClickPendingIntent(R.id.config_button, PendingIntent.getActivity(context, appWidgetId, intentConfig, PendingIntentImmutable));

            if (eventsData.preferences_debug_on) {
                List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId);
                views.setTextViewText(R.id.info, context.getString(R.string.widget_msg_updated) + new SimpleDateFormat(DATETIME_DD_MM_YYYY_HH_MM, eventsData.getResources().getConfiguration().locale).format(new Date(Calendar.getInstance().getTimeInMillis()))
                        + STRING_EOF + context.getString(R.string.widget_msg_events) + eventsData.getFilteredEventList(eventsData.eventList, widgetPref).size());
            } else {
                views.setTextViewText(R.id.info, STRING_EMPTY);
            }

            //Реакция на нажатие
            Intent listClickIntent = new Intent(context, WidgetList.class);
            listClickIntent.setAction(ACTION_CLICK);
            PendingIntent listClickPIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, PendingIntentMutable);
            views.setPendingIntentTemplate(R.id.widget_list, listClickPIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_LIST_UPDATE_APP_WIDGET_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted (Context context, int[] appWidgetIds) {

        ContactsEvents eventsData = ContactsEvents.getInstance();
        for (int appWidgetId : appWidgetIds) {
            eventsData.removeWidgetPreference(appWidgetId);
        }

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        try {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_LIST_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(ACTION_CLICK)) {
            String eventInfo = intent.getStringExtra(EXTRA_CLICKED_EVENT);
            if (eventInfo == null || eventInfo.isEmpty()) return;

            //Toast.makeText(context, "Clicked on item: " + eventInfo, Toast.LENGTH_SHORT).show();

            String[] singleEventArray = eventInfo.split(STRING_2HASH);
            if (singleEventArray.length == Position_attrAmount) {

                Intent intentView = new Intent(Intent.ACTION_VIEW);
                Uri uri = null;
                if (!singleEventArray[Position_contactID].isEmpty()) {
                    uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[Position_contactID]);
                } else if (!singleEventArray[Position_eventID].isEmpty()) {
                    uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleEventArray[Position_eventID]);
                }
                if (uri != null) {
                    intentView.setData(uri);
                    intentView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.getApplicationContext().startActivity(intentView);
                }

            }

        }
    }

}
