/*
 * *
 *  * Created by Vladimir Belov on 28.04.2026, 23:17
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 28.04.2026, 22:38
 *
 */

package org.vovka.birthdaycountdown;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Класс WidgetList - это AppWidgetProvider, который отображает список событий в виджете.
 * <p>
 * Виджет использует RemoteViews для отображения контента и {@link EventListDataProvider} для заполнения списка.
 * Он поддерживает настраиваемые заголовки, цвета, границы и другие визуальные атрибуты.
 * Он использует класс {@link ContactsEvents} для обработки логики приложения, данных событий и настроек.
 */
public class WidgetList extends AppWidgetProvider {

    private static final String TAG = "WidgetList";
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    private void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        long statCurrentModuleStart = System.currentTimeMillis();
        final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        final int PendingIntentMutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;

        try {
            eventsData.initLanguage(context);

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(eventsData.getContext()).getAppWidgetInfo(appWidgetId);
            String widgetType = Constants.WIDGET_TYPE_LIST;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            List<String> widgetPref_eventInfo = new ArrayList<>();
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
            }

            RemoteViews views;
            // https://stackoverflow.com/questions/9953892/how-to-put-divider-at-particular-position-in-an-android-list-view
            if (widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Dividers_ID))) {
                views = new RemoteViews(eventsData.getContext().getPackageName(), R.layout.widgetlist_dividers);
            } else {
                views = new RemoteViews(eventsData.getContext().getPackageName(), R.layout.widgetlist);
            }

            //Кнопка настроек
            if (DeviceTools.isWidgetSupportConfig() && !widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_ButtonConfig_ID))) {
                views.setViewVisibility(R.id.config_button, View.GONE);
            } else {
                views.setViewVisibility(R.id.config_button, View.VISIBLE);
                Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
                intentConfig.setAction(Constants.ACTION_LAUNCH);
                intentConfig.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                views.setOnClickPendingIntent(R.id.config_button, PendingIntent.getActivity(context, appWidgetId, intentConfig, PendingIntentImmutable));
            }
            //todo: https://stackoverflow.com/questions/5070413/widget-double-click

            List<String> filteredEventList = eventsData.getFilteredEventList(eventsData.eventList, widgetPref);
            int eventsToShow = filteredEventList.size();

            if (eventsData.preferences_debug_on) {
                views.setTextViewText(R.id.info, context.getString(R.string.widget_msg_updated)
                        + new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, Locale.forLanguageTag(eventsData.currentLocale)).format(new Date(Calendar.getInstance().getTimeInMillis()))
                        + Constants.STRING_EOL + context.getString(R.string.widget_msg_events) + eventsToShow + Constants.STRING_SLASH + eventsData.eventList.size());
            } else {
                views.setTextViewText(R.id.info, Constants.STRING_EMPTY);
            }

            String prefWidgetCaption = Constants.STRING_EMPTY;
            if (widgetPref.size() > 9) {
                prefWidgetCaption = widgetPref.get(9);
            }
            double defaultMagnify = 1.6;
            float sizeForWidgetElement = ImageUtils.getSizeForWidgetElement(widgetPref, 1, Constants.WIDGET_TEXT_SIZE_TINY, defaultMagnify);
            if (!prefWidgetCaption.isEmpty()) {
                views.setViewVisibility(R.id.caption, View.VISIBLE);
                views.setTextViewText(R.id.caption, prefWidgetCaption);
                views.setTextViewTextSize(R.id.caption, TypedValue.COMPLEX_UNIT_SP, sizeForWidgetElement);
                views.setTextColor(R.id.caption, eventsData.preferences_widgets_color_widget_caption);
            } else {
                views.setViewVisibility(R.id.caption, View.INVISIBLE);
            }
            //Отступ списка от заголовка
            if (!prefWidgetCaption.isEmpty() || eventsData.preferences_debug_on) {
                int paddingTop = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        (float) (22 * sizeForWidgetElement / (Constants.WIDGET_TEXT_SIZE_TINY * defaultMagnify)), eventsData.getResources().getDisplayMetrics()
                );
                views.setViewPadding(R.id.widget_layout, 0, paddingTop, 0, 0);
            } else {
                views.setViewPadding(R.id.widget_layout, 0, 0, 0, 0);
            }

            //Реакция на нажатие
            Intent listClickIntent = new Intent(context, WidgetList.class);
            listClickIntent.setAction(Constants.ACTION_CLICK);
            PendingIntent listClickPIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, PendingIntentMutable);
            views.setPendingIntentTemplate(R.id.widget_list, listClickPIntent);

            //Сообщение при отсутствии событий
            String prefZeroEventsMessage = Constants.STRING_EMPTY;
            if (widgetPref.size() > 7) prefZeroEventsMessage = widgetPref.get(7).replace(Constants.STRING_EOT, Constants.STRING_COMMA);
            views.setTextViewText(R.id.empty_view, TextUtils.isEmpty(prefZeroEventsMessage) ? context.getString(R.string.msg_no_events) : prefZeroEventsMessage);

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
            //Иначе не скрывается caption_bar
            views.setInt(R.id.caption_bar, Constants.METHOD_SET_BACKGROUND_COLOR, !prefWidgetCaption.isEmpty() ? colorWidgetBackground : 0);
            views.setInt(R.id.widget_list,Constants.METHOD_SET_BACKGROUND_COLOR, colorWidgetBackground);

            //Если события есть - рисуем бордюр, иначе - прозрачность
            if (eventsToShow > 0 && (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_Border_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Border_ID)))) {
                views.setInt(R.id.widget_layout,Constants.METHOD_SET_BACKGROUND_RES, R.drawable.layout_bg);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    views.setViewPadding(R.id.widget_frame, 10, 10, 10, 10);
                }
            } else {
                views.setInt(R.id.widget_layout,Constants.METHOD_SET_BACKGROUND_RES, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    views.setViewPadding(R.id.widget_frame, 0, 0, 0, 0);
                }
            }

            //Фото ближайшего события
            views.setViewVisibility(R.id.widgetPhoto, View.GONE);
            if (widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_ShowNearestEventPhoto_ID))
                    && !filteredEventList.isEmpty()) {
                String eventInfo = filteredEventList.get(0);
                Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId);
                int widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                int roundingFactor = ImageUtils.getRoundingFactor(widgetPref);
                Bitmap photo = eventsData.getEventPhoto(eventInfo, true, true, false, roundingFactor);
                if (photo != null) {
                    int outWidth;
                    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                    if (widgetWidth > 0) {
                        float floatDensity = displayMetrics.density;
                        outWidth = (int) ((widgetWidth * floatDensity * 1.2) / 6);
                    } else {
                        outWidth = (int) (displayMetrics.widthPixels * 1.2 / 7);
                    }

                    int inWidth = photo.getWidth();
                    int inHeight = photo.getHeight();
                    double resizeFactor = ImageUtils.getSizeForWidgetElement(widgetPref, 2, 1, 1);
                    if (inHeight > 0 && inWidth > 0) {
                        int outHeight = inHeight * outWidth / inWidth;

                        if (outHeight > 0 && outWidth > 0) {
                            Bitmap photo_small = Bitmap.createScaledBitmap(photo, (int) (outWidth * resizeFactor), (int) (outHeight * resizeFactor), true);
                            views.setImageViewBitmap(R.id.widgetPhoto, photo_small);
                            views.setViewVisibility(R.id.widgetPhoto, View.VISIBLE);

                            if (!prefWidgetCaption.isEmpty()) {
                                int captionPadding = (int) (outWidth * resizeFactor)
                                        + (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, displayMetrics);
                                views.setViewPadding(R.id.caption, captionPadding, 0, 0, 0);
                            }
                        }
                    }
                }
            }

            //Привязываем адаптер
            //в Android 16 setRemoteAdapter принудительно (внутри framework) конвертируется в RemoteCollectionItems
            // https://issuetracker.google.com/issues/398066578
            // list widget it now calls ListWidgetRemoteViewsFactory#getViewAt for all items, regardless of how many are actually visible
            // https://github.com/UweTrottmann/SeriesGuide/issues/1118
            Intent adapter = new Intent(context, EventListWidgetService.class);
            adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
            adapter.setData(data); //Чтобы разные виджеты одного адаптера отличались для системы
            views.setRemoteAdapter(R.id.widget_list, adapter);
            views.setEmptyView(R.id.widget_list, R.id.empty_view);

            ToastExpander.showDebugMsg(context, Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    context.getResources().getString(R.string.msg_debug_widget_list_config, widgetType, appWidgetId,
                            context.getResources().getResourceEntryName(views.getLayoutId()), TextUtils.join(Constants.STRING_COMMA, widgetPref))
                    : widgetType.concat(Constants.STRING_COLON)
                    .concat(String.valueOf(appWidgetId)).concat(Constants.STRING_EOL)
                    .concat(TextUtils.join(Constants.STRING_COMMA, widgetPref))
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
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            int pref_onClick = 0;
            try {
                pref_onClick = intent.getIntExtra(Constants.EXTRA_CLICKED_PREFS, Integer.parseInt(context.getString(R.string.pref_Widgets_OnClick_default)));
            } catch (NumberFormatException ignored) { /**/ }
            if (pref_onClick == 0 || eventInfo == null || eventInfo.isEmpty()) return;

            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);
            String eventText = StringUtils.getNotNullString(intent.getStringExtra(Constants.EXTRA_CLICKED_TEXT));
            Intent intentAction = ContactsEvents.getViewActionIntent(eventInfo, eventText, singleEventArray, pref_onClick, context);
            if (intentAction != null) {
                try {
                    intentAction.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    context.getApplicationContext().startActivity(intentAction);
                } catch (android.content.ActivityNotFoundException e) { /**/ }
            }
        }
    }

}
