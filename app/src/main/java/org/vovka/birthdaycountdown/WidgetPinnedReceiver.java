/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:35
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * WidgetPinnedReceiver - это BroadcastReceiver, отвечающий за обработку события, когда виджет закрепляется на главном экране.
 * Он запускает Activity конфигурации для вновь закрепленного виджета.
 * <p>
 * Этот ресивер предназначен для вызова, когда система обнаруживает, что на главный экран пользователя был добавлен новый виджет.
 * При получении широковещательного сообщения он определяет тип закрепленного виджета и запускает соответствующее
 * Activity конфигурации (либо WidgetCalendarConfigureActivity, либо WidgetConfigureActivity), чтобы провести пользователя
 * через процесс настройки.
 * </p>
 * <p>
 * Он полагается на {@link ContactsEvents} для хранения и получения ID вновь закрепленного виджета.
 * После запуска Activity конфигурации он сбрасывает ID закрепленного виджета в 0 в {@link ContactsEvents},
 * чтобы указать, что начальный процесс настройки был запущен.
 * </p>
 * <p>
 * Он обрабатывает потенциальные исключения во время извлечения виджета и запуска Activity, логируя ошибки и отображая отладочные сообщения.
 * </p>
 */
public class WidgetPinnedReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetPinnedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            int widgetId = eventsData.pinnedWidgetId;
            if (widgetId == 0) return;

            String widgetType = null;
                final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
                if (appWidgetInfo != null) {
                    widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
                }

            Intent intentConfig = new Intent(context, Constants.WIDGET_TYPE_CALENDAR.equals(widgetType) ?
                    WidgetCalendarConfigureActivity.class : WidgetConfigureActivity.class);
            intentConfig.setAction(Constants.ACTION_LAUNCH);
            intentConfig.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intentConfig.putExtra(Constants.EXTRA_NEW_WIDGET, Constants.STRING_1);
            intentConfig.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intentConfig);
            } catch (android.content.ActivityNotFoundException e) { /**/ }

                eventsData.pinnedWidgetId = 0;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
