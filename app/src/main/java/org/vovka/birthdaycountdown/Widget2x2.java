/*
 * *
 *  * Created by Vladimir Belov on 20.03.2026, 21:02
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 20.03.2026, 18:58
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Widget2x2 - это реализация AppWidgetProvider, предоставляющая масштабируемый виджет 2x2.
 * Этот виджет динамически обновляет свое содержимое на основе пользовательских предпочтений и системной локали.
 * Он поддерживает изменения конфигурации, удаление виджета и изменение размера.
 *
 * <p>
 * Ключевые особенности:
 * <ul>
 *     <li><b>Масштабируемость:</b> Виджет может адаптироваться к различным размерам, заданным пользователем.</li>
 *     <li><b>Поддержка локали:</b> Виджет обновляет свой интерфейс на основе выбранного языка, либо определенного пользователем, либо системного по умолчанию.</li>
 *     <li><b>Управление данными:</b> Содержимое виджета управляется {@link ContactsEvents}, обеспечивая согласованность данных.</li>
 *     <li><b>Сохранение конфигурации:</b> Предпочтения виджета сохраняются и извлекаются для каждого экземпляра виджета.</li>
 *     <li><b>Отладочные сообщения:</b> Полезная отладочная информация отображается через {@link ToastExpander} для упрощения устранения неполадок.</li>
 *     <li><b>Метрики производительности:</b> Записывает статистику, связанную со временем обновления виджета и активными виджетами.</li>
 * </ul>
 * </p>
 */
public class Widget2x2 extends AppWidgetProvider {

    private static final String TAG = "Widget2x2";
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        long statCurrentModuleStart = System.currentTimeMillis();
        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            eventsData.initLanguage(context);

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(eventsData.getContext()).getAppWidgetInfo(appWidgetId);
            String widgetType = Constants.WIDGET_TYPE_2X2;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);
            RemoteViews views = getRemoteViews(eventsData.getContext());

            ToastExpander.showDebugMsg(eventsData.getContext(), Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                            context.getResources().getString(R.string.msg_debug_widget_photo_config, widgetType, appWidgetId,
                                    context.getResources().getResourceEntryName(views.getLayoutId()), minWidth, minHeight, TextUtils.join(Constants.STRING_COMMA, widgetPref))
                    : widgetType.concat(Constants.STRING_COLON)
                    .concat(String.valueOf(appWidgetId)).concat(Constants.STRING_EOL)
                    .concat(TextUtils.join(Constants.STRING_COMMA, widgetPref))
            );

            new WidgetUpdater(eventsData.getContext(), ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            eventsData.statTimeUpdateWidgets += System.currentTimeMillis() - statCurrentModuleStart;
            eventsData.statActiveWidgets++;
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

        for (int appWidgetId : appWidgetIds) {
            eventsData.removeWidgetPreference(appWidgetId);
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

    static private RemoteViews getRemoteViews(@NonNull Context context) {

        return new RemoteViews(context.getPackageName(), R.layout.widget_2x2);

    }

}

