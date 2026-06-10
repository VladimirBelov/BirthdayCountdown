/*
 * *
 *  * Created by Vladimir Belov on 10.06.2026, 11:12
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 10.06.2026, 11:02
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
 * Widget4x1 - это класс AppWidgetProvider, который обрабатывает виджет размером 4x1 для отображения событий контактов.
 * Он расширяет {@link AppWidgetProvider} для управления жизненным циклом и обновлениями виджета.
 * Этот виджет отображает до 5 событий и динамически обновляется на основе пользовательских настроек и изменений данных.
 *
 * <p><b>Основные характеристики:</b></p>
 * <ul>
 *     <li><b>Динамические обновления:</b> Обновляет содержимое виджета, когда система транслирует обновление,
 *         когда изменяются параметры виджета или когда виджет добавляется впервые.</li>
 *     <li><b>Поддержка локализации:</b> Адаптируется к предпочитаемому языку пользователя и поддерживает динамическое
 *         изменение языка.</li>
 *     <li><b>Отображение событий:</b> Показывает до 5 событий контактов, полученных из синглтона {@link ContactsEvents}.</li>
 *     <li><b>Обработка конфигурации:</b> Управляет настройками и параметрами виджета, включая данные,
 *          связанные с конкретными идентификаторами виджетов.</li>
 *     <li><b>Обработка исключений:</b> Обеспечивает надежную обработку исключений для предотвращения сбоев и
 *         регистрирует ошибки для отладки.</li>
 *     <li><b>Отладочное логирование:</b> Включает отладочные сообщения для отслеживания поведения виджета,
 *         особенно во время разработки.</li>
 *     <li><b>Обработка удаления виджета:</b> Удаляет специфичные для виджета настройки при удалении виджета.</li>
 * </ul>
 *
 * <p><b>Методы:</b></p>
 * <ul>
 *     <li>{@link #updateAppWidget(Context, AppWidgetManager, int)}: Обновляет конкретный экземпляр виджета.</li>
 *     <li>{@link #onUpdate(Context, AppWidgetManager, int[])}: Вызывается для обновления виджетов приложения.</li>
 *     <li>{@link #onDeleted(Context, int[])}: Вызывается при удалении одного или нескольких виджетов.</li>
 */
public class Widget4x1 extends AppWidgetProvider {

    private static final String TAG = "Widget4x1";
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        long statCurrentModuleStart = System.currentTimeMillis();
        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            eventsData.setToday();
            eventsData.initLanguage(context);

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

            RemoteViews views = new RemoteViews(eventsData.getContext().getPackageName(), R.layout.widget_4x1bc);

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(eventsData.getContext()).getAppWidgetInfo(appWidgetId);
            String widgetType = Constants.WIDGET_TYPE_4X1;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            ToastExpander.showDebugMsg(eventsData.getContext(), Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    context.getResources().getString(R.string.msg_debug_widget_photo_config, widgetType, appWidgetId,
                            context.getResources().getResourceEntryName(views.getLayoutId()), minWidth, minHeight, TextUtils.join(Constants.STRING_COMMA, widgetPref))
                    : widgetType.concat(Constants.STRING_COLON)
                    .concat(String.valueOf(appWidgetId)).concat(Constants.STRING_EOL)
                    .concat(TextUtils.join(Constants.STRING_COMMA, widgetPref))
            );

            new WidgetUpdater(eventsData.getContext(), ContactsEvents.getInstance(), views, 5, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
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

}

