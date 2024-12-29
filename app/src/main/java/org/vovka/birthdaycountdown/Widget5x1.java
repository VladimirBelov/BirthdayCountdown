/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 14.01.2024, 13:04
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

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

// На 1-7 событий масштабируемый
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
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
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