/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 22.12.2021, 0:41
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.ACTION_CLICK;
import static org.vovka.birthdaycountdown.Constants.ACTION_LAUNCH;
import static org.vovka.birthdaycountdown.Constants.DATETIME_DD_MM_YYYY_HH_MM;
import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_EVENT;
import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_PREFS;
import static org.vovka.birthdaycountdown.Constants.PARAM_APP_WIDGET_ID;
import static org.vovka.birthdaycountdown.Constants.REGEX_PLUS;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_EOL;
import static org.vovka.birthdaycountdown.Constants.STRING_EOT;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_attrAmount;

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
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

// Список событий масштабируемый (с фото)
public class WidgetPhotoList extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int widgetId) {

        final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.getPreferences();
            eventsData.setLocale(true);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);

            //Привязываем адаптер
            Intent adapter = new Intent(context, EventPhotoListWidgetService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
            adapter.setData(data); //Чтобы разные виджеты одного адаптера отличались для системы
            //Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId);
            //adapter.putExtra("intWidgetWidth", options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
            //DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            //adapter.putExtra("floatScreenDensity", displayMetrics.density);
            views.setRemoteAdapter(R.id.widget_list, adapter);

            views.setTextViewText(R.id.empty_view, context.getString(R.string.msg_no_events));
            views.setEmptyView(R.id.widget_list, R.id.empty_view);

            //Кнопка настроек
            Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
            intentConfig.setAction(ACTION_LAUNCH);
            intentConfig.putExtra(PARAM_APP_WIDGET_ID, widgetId);
            views.setOnClickPendingIntent(R.id.config_button, PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName();
            List<String> widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);
            int eventsToShow = eventsData.getFilteredEventList(eventsData.eventList, widgetPref).size();

            if (eventsData.preferences_debug_on) {

                views.setTextViewText(R.id.info, context.getString(R.string.widget_msg_updated) + new SimpleDateFormat(DATETIME_DD_MM_YYYY_HH_MM, eventsData.getResources().getConfiguration().locale).format(new Date(Calendar.getInstance().getTimeInMillis()))
                        + STRING_EOL + context.getString(R.string.widget_msg_events) + eventsToShow);
            } else {
                views.setTextViewText(R.id.info, STRING_EMPTY);
            }

            //Реакция на нажатие
            Intent listClickIntent = new Intent(context, WidgetPhotoList.class);
            listClickIntent.setAction(ACTION_CLICK);
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
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(REGEX_PLUS));
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
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_PHOTO_LIST_UPDATE_APP_WIDGET_ERROR + e, Toast.LENGTH_LONG).show();
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

        ContactsEvents eventsData = ContactsEvents.getInstance();
        for (int widgetId : widgetIds) {
            eventsData.removeWidgetPreference(widgetId);
        }

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int widgetId, Bundle newOptions) {

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.getPreferences();
            eventsData.setLocale(true);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);
            appWidgetManager.updateAppWidget(widgetId, views);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, newOptions);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_PHOTO_LIST_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase(ACTION_CLICK)) {
            String eventInfo = intent.getStringExtra(EXTRA_CLICKED_EVENT);
            int actionPref = intent.getIntExtra(EXTRA_CLICKED_PREFS, Integer.parseInt(context.getString(R.string.pref_Widgets_OnClick_default)));
            if (eventInfo == null || eventInfo.isEmpty()) return;

            String[] singleEventArray = eventInfo.split(STRING_EOT, -1);
            if (singleEventArray.length == Position_attrAmount) {

                Intent intentView = null;

                if (actionPref == 7) { //Основной список событий
                    intentView = new Intent(context, MainActivity.class);
                    intentView.setAction(ACTION_LAUNCH);
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
