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
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Widget5x1 - это поставщик виджетов приложения (App Widget), который отображает до 7 событий,
 * масштабируя свой макет и содержимое в зависимости от доступного размера виджета и настроек пользователя.
 *
 * <p>
 * Этот виджет динамически настраивает количество отображаемых событий (от 1 до 7)
 * в зависимости от ширины виджета и настроек пользователя. Он также обрабатывает изменения языка
 * и соответствующим образом обновляет свое содержимое. Виджет использует удаленные представления (RemoteViews)
 * для отображения своего контента.
 * </p>
 *
 * <p>
 * Основные характеристики:
 * <ul>
 *     <li><b>Масштабируемое отображение событий:</b> Отображает от 1 до 7 событий в зависимости от доступной ширины.</li>
 *     <li><b>Динамическая поддержка языка:</b> Обрабатывает изменения языка на основе системной локали или настроек пользователя.</li>
 *     <li><b>Настраиваемое количество событий:</b> Позволяет пользователям зафиксировать количество отображаемых событий.</li>
 *     <li><b>На основе RemoteViews:</b> Использует RemoteViews для отображения событий.</li>
 *     <li><b>Управление настройками:</b> Получает и использует пользовательские настройки, связанные с виджетом.</li>
 *     <li><b>Обработка ошибок:</b> Регистрирует ошибки и отображает отладочные сообщения.</li>
 * </ul>
 * </p>
 *
 * <p>
 *  Виджет может отображать различный макет в зависимости от количества событий.
 *  <ul>
 *      <li>1 событие: R.layout.widget_1x1</li>
 *      <li>2 события: R.layout.widget_2x1</li>
 *      <li>3 события: R.layout.widget_3x1</li>
 *  </ul>
 * </p>
 * На 1-7 событий масштабируемый
 */
public class Widget5x1 extends AppWidgetProvider {

    private static final String TAG = "Widget5x1";
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
            int eventsCount = Math.min(getCellsForSize(minWidth), Constants.WIDGET_EVENTS_MAX);

            //Уточняем количество событий в настройке
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
            String widgetType = Constants.WIDGET_TYPE_5X1;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            }
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);

            //Объём событий

            String prefScope = Constants.STRING_EMPTY;
            boolean fixedEventsCount = false;
            if (widgetPref.size() > 8) prefScope = widgetPref.get(8);
            if (!TextUtils.isEmpty(prefScope)) {
                Matcher matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE).matcher(prefScope);
                if (matchScopes.find()) {
                    final String scopeEvents = matchScopes.group(1);
                    if(scopeEvents != null) {
                        if (!scopeEvents.equals(Constants.STRING_0)){
                            try {
                                eventsCount = Integer.parseInt(scopeEvents);
                                fixedEventsCount = true;
                            } catch (NumberFormatException ignored) { /**/ }
                        }
                    }
                }
            }

            if (!fixedEventsCount) {
                int prefEventsCountIndex = 0;
                try {
                    if (widgetPref.size() > 2) prefEventsCountIndex = Integer.parseInt(widgetPref.get(2));
                } catch (Exception e2) { /**/ }

                switch (prefEventsCountIndex) {
                    case 1:
                        eventsCount -= 2;
                        break;
                    case 2:
                        eventsCount -= 1;
                        break;
                    case 3:
                        eventsCount += 1;
                        break;
                    case 4:
                        eventsCount += 2;
                        break;
                }
            }

            RemoteViews views = getRemoteViews(context, eventsCount);

            ToastExpander.showDebugMsg(context, Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    context.getResources().getString(R.string.msg_debug_widget_photo_config, widgetType, appWidgetId,
                            context.getResources().getResourceEntryName(views.getLayoutId()), minWidth, minHeight, String.join(Constants.STRING_COMMA, widgetPref))
                    : widgetType.concat(Constants.STRING_COLON).concat(String.valueOf(appWidgetId)).concat(Constants.STRING_EOL).concat(String.join(Constants.STRING_COMMA, widgetPref))
            );

            new WidgetUpdater(context, eventsData, views, eventsCount, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context.getApplicationContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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

        try{

            updateAppWidget(context, appWidgetManager, appWidgetId);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    static private RemoteViews getRemoteViews(@NonNull Context context, int eventsCount) {

        switch (eventsCount) {
            case -1:
            case 0:
            case 1:
                    return new RemoteViews(context.getPackageName(), R.layout.widget_1x1);
            case 2:  return new RemoteViews(context.getPackageName(), R.layout.widget_2x1);
            case 3:  return new RemoteViews(context.getPackageName(), R.layout.widget_3x1);
            case 4:  return new RemoteViews(context.getPackageName(), R.layout.widget_4x1);
            case 5:  return new RemoteViews(context.getPackageName(), R.layout.widget_5x1);
            case 6:  return new RemoteViews(context.getPackageName(), R.layout.widget_6x1);
            default:  return new RemoteViews(context.getPackageName(), R.layout.widget_7x1);
        }

    }

    private static int getCellsForSize(int size) {

        int n = 2;
        while (70 * n - 30 < (size + 6)) {
            ++n;
        }
        return n - 1;
    }

}