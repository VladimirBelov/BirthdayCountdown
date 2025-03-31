/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:33
 *
 */

package org.vovka.birthdaycountdown;

import android.app.LocaleManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

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

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_4x1bc);

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
            String widgetType = Constants.WIDGET_TYPE_4X1;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            ToastExpander.showDebugMsg(context, Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    context.getResources().getString(R.string.msg_debug_widget_photo_config, widgetType, appWidgetId,
                            context.getResources().getResourceEntryName(views.getLayoutId()), minWidth, minHeight, String.join(Constants.STRING_COMMA, widgetPref))
                    : widgetType.concat(Constants.STRING_COLON)
                    .concat(String.valueOf(appWidgetId)).concat(Constants.STRING_EOL)
                    .concat(String.join(Constants.STRING_COMMA, widgetPref))
            );

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 5, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

