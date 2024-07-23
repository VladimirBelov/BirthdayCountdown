/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 26.11.2023, 20:05
 *
 */

package org.vovka.birthdaycountdown;

import android.app.LocaleManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

// Список событий масштабируемый (с фото)
public class WidgetPhotoList extends AppWidgetProvider {

    private static final String TAG = "WidgetPhotoList";
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        long statCurrentModuleStart = System.currentTimeMillis();
        final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(context.getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources applicationRes = context.getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    LocaleList list = context.getSystemService(LocaleManager.class).getApplicationLocales();
                    if (!list.isEmpty()) {
                        locale = context.getSystemService(LocaleManager.class).getApplicationLocales().get(0);
                    }
                }
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            List<String> widgetPref_eventInfo = new ArrayList<>();
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
            }

            RemoteViews views;
            if (widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Dividers_ID))) {
                views = new RemoteViews(context.getPackageName(), R.layout.widgetlist_dividers);
            } else {
                views = new RemoteViews(context.getPackageName(), R.layout.widgetlist);
            }

            //Привязываем адаптер
            Intent adapter = new Intent(context, EventPhotoListWidgetService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
            adapter.setData(data); //Чтобы разные виджеты одного адаптера отличались для системы
            views.setRemoteAdapter(R.id.widget_list, adapter);

            views.setEmptyView(R.id.widget_list, R.id.empty_view);

            //Кнопка настроек
            if (ContactsEvents.isWidgetSupportConfig() && !widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_ButtonConfig_ID))) {
                views.setViewVisibility(R.id.config_button, View.GONE);
            } else {
                views.setViewVisibility(R.id.config_button, View.VISIBLE);
                Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
                intentConfig.setAction(Constants.ACTION_LAUNCH);
                intentConfig.putExtra(Constants.PARAM_APP_WIDGET_ID, appWidgetId);
                views.setOnClickPendingIntent(R.id.config_button, PendingIntent.getActivity(context, appWidgetId, intentConfig, PendingIntentImmutable));
            }

            int eventsToShow = eventsData.getFilteredEventList(eventsData.eventList, widgetPref).size();

            if (eventsData.preferences_debug_on) {

                views.setTextViewText(R.id.info, context.getString(R.string.widget_msg_updated) + new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, eventsData.getResources().getConfiguration().locale).format(new Date(Calendar.getInstance().getTimeInMillis()))
                        + Constants.STRING_EOL + context.getString(R.string.widget_msg_events) + eventsToShow);
            } else {
                views.setTextViewText(R.id.info, Constants.STRING_EMPTY);
            }

            String prefWidgetCaption = Constants.STRING_EMPTY;
            if (widgetPref.size() > 9) {
                prefWidgetCaption = widgetPref.get(9);
            }
            if (!prefWidgetCaption.isEmpty()) {
                views.setViewVisibility(R.id.caption, View.VISIBLE);
                views.setTextViewText(R.id.caption, prefWidgetCaption);
                views.setTextViewTextSize(R.id.caption, TypedValue.COMPLEX_UNIT_SP, eventsData.getTextSizeForWidgetText(widgetPref, Constants.WIDGET_TEXT_SIZE_TINY, 1.6));
                views.setViewPadding(R.id.widget_layout, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22, context.getResources().getDisplayMetrics()), 0, 0);
                views.setTextColor(R.id.caption, eventsData.preferences_widgets_color_widget_caption);
            } else {
                views.setViewVisibility(R.id.caption, View.INVISIBLE);
                views.setViewPadding(R.id.widget_layout, 0, 0, 0, 0);
            }

            //Реакция на нажатие
            Intent listClickIntent = new Intent(context, WidgetPhotoList.class);
            listClickIntent.setAction(Constants.ACTION_CLICK);
            PendingIntent listClickPIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, PendingIntentMutable);
            views.setPendingIntentTemplate(R.id.widget_list, listClickPIntent);

            //Сообщение при отсутствии событий
            String prefZeroEventsMessage = context.getString(R.string.msg_no_events);
            if (widgetPref.size() > 7) prefZeroEventsMessage = widgetPref.get(7).replaceAll(Constants.STRING_EOT, Constants.STRING_COMMA);
            views.setTextViewText(R.id.empty_view, prefZeroEventsMessage);

            //Цвет подложки
            int colorWidgetBackground = 0;
            if (widgetPref.size() > 5 && !widgetPref.get(5).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(5));
                } catch (Exception e) { /**/ }
            }
            if (colorWidgetBackground == 0) {
                colorWidgetBackground = ContextCompat.getColor(context, R.color.pref_Widgets_Color_WidgetBackground_default);
            }
            //Иначе не скрывается caption_bar
            views.setInt(R.id.caption_bar, "setBackgroundColor", !prefWidgetCaption.isEmpty() ? colorWidgetBackground : 0);
            views.setInt(R.id.widget_list,"setBackgroundColor", colorWidgetBackground);

            //Если события есть - рисуем бордюр, иначе - прозрачность
            if (eventsToShow > 0 && (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_Border_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Border_ID)))) {
                views.setInt(R.id.widget_layout,"setBackgroundResource", R.drawable.layout_bg);
            } else {
                views.setInt(R.id.widget_layout,"setBackgroundResource", 0);
            }

            ToastExpander.showDebugMsg(context, Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    widgetType + Constants.STRING_COLON_SPACE + appWidgetId +
                            ", layout=" + context.getResources().getResourceEntryName(views.getLayoutId()) +
                            "\n widgetPref=" + widgetPref
                    : widgetType + Constants.STRING_COLON + appWidgetId + Constants.STRING_EOL + widgetPref
            );

            //Запуск обновления
            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            eventsData.statTimeUpdateWidgets += System.currentTimeMillis() - statCurrentModuleStart;
            eventsData.statActiveWidgets++;
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
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        try {

            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            updateAppWidget(context, appWidgetManager, appWidgetId);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();
        if (action != null && action.equalsIgnoreCase(Constants.ACTION_CLICK)) {
            String eventInfo = intent.getStringExtra(Constants.EXTRA_CLICKED_EVENT);
            int actionPref = intent.getIntExtra(Constants.EXTRA_CLICKED_PREFS, Integer.parseInt(context.getString(R.string.pref_Widgets_OnClick_default)));
            if (eventInfo == null || eventInfo.isEmpty()) return;

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
