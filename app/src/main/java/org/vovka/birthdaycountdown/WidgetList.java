/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 22.12.2021, 0:41
 *
 */

package org.vovka.birthdaycountdown;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

// Список событий масштабируемый
public class WidgetList extends AppWidgetProvider {

    private static final String TAG = "WidgetList";
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    private void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int widgetId) {

        final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);

            //Привязываем адаптер
            Intent adapter = new Intent(context, EventListWidgetService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
            adapter.setData(data); //Чтобы разные виджеты одного адаптера отличались для системы
            views.setRemoteAdapter(R.id.widget_list, adapter);

            views.setTextViewText(R.id.empty_view, context.getString(R.string.msg_no_events));
            views.setEmptyView(R.id.widget_list, R.id.empty_view);

            //Кнопка настроек
            Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
            intentConfig.setAction(Constants.ACTION_LAUNCH);
            intentConfig.putExtra(Constants.PARAM_APP_WIDGET_ID, widgetId);
            views.setOnClickPendingIntent(R.id.config_button, PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName();
            List<String> widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);
            int eventsToShow = eventsData.getFilteredEventList(eventsData.eventList, widgetPref).size();

            if (eventsData.preferences_debug_on) {

                views.setTextViewText(R.id.info, context.getString(R.string.widget_msg_updated) + new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, eventsData.getResources().getConfiguration().locale).format(new Date(Calendar.getInstance().getTimeInMillis()))
                        + Constants.STRING_EOL + context.getString(R.string.widget_msg_events) + eventsToShow);
            } else {
                views.setTextViewText(R.id.info, Constants.STRING_EMPTY);
            }

            //Реакция на нажатие
            Intent listClickIntent = new Intent(context, WidgetList.class);
            listClickIntent.setAction(Constants.ACTION_CLICK);
            PendingIntent listClickPIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, PendingIntentMutable);
            views.setPendingIntentTemplate(R.id.widget_list, listClickPIntent);

            //Цвет подложки
            int colorWidgetBackground = 0;
            if (widgetPref.size() > 5 && !widgetPref.get(5).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(5));
                } catch (Exception e) { /* */}
            }
            if (colorWidgetBackground == 0) {
                colorWidgetBackground = ContextCompat.getColor(context, R.color.pref_Widgets_Color_WidgetBackground_default);
            }
            views.setInt(R.id.widget_list,"setBackgroundColor", colorWidgetBackground);

            //Если события есть - рисуем бордюр, иначе - прозрачность
            List<String> widgetPref_eventInfo = new ArrayList<>();
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
            }
            if (eventsToShow > 0 && (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_Border)
                    : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_Border))) {
                views.setInt(R.id.widget_layout,"setBackgroundResource", R.drawable.layout_bg);
            } else {
                views.setInt(R.id.widget_layout,"setBackgroundResource", 0);
            }

            //Запуск обновления
            appWidgetManager.updateAppWidget(widgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] widgetIds) {

        for (int widgetId : widgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId);
        }
    }

    @Override
    public void onDeleted (Context context, int[] widgetIds) {

        for (int widgetId : widgetIds) {
            eventsData.removeWidgetPreference(widgetId);
        }

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int widgetId, Bundle newOptions) {

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);
            appWidgetManager.updateAppWidget(widgetId, views);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);
        if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(Constants.ACTION_CLICK)) {
            String eventInfo = intent.getStringExtra(Constants.EXTRA_CLICKED_EVENT);
            int actionPref = intent.getIntExtra(Constants.EXTRA_CLICKED_PREFS, Integer.parseInt(context.getString(R.string.pref_Widgets_OnClick_default)));
            if (eventInfo == null || eventInfo.isEmpty()) return;

            //Toast.makeText(context, "Clicked on item: " + eventInfo, Toast.LENGTH_SHORT).show();

            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);
            if (singleEventArray.length == ContactsEvents.Position_attrAmount) {

                Intent intentView = null;

                if (actionPref == 7) { //Основной список событий
                    intentView = new Intent(context, MainActivity.class);
                    intentView.setAction(Constants.ACTION_LAUNCH);
                } else if (actionPref >= 1 & actionPref <=4) {
                    intentView = ContactsEvents.getViewActionIntent(singleEventArray, actionPref);
                }

                if (intentView != null) {
                    intentView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        context.getApplicationContext().startActivity(intentView);
                    } catch (android.content.ActivityNotFoundException e) { /**/ }
                }

            }

        }
    }

}
