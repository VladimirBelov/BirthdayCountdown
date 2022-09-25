/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 13.09.2022, 18:50
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.util.List;

// На 1-7 событий масштабируемый
public class Widget5x1 extends AppWidgetProvider {

    private static final String TAG = "Widget5x1";
    final ContactsEvents eventsData = ContactsEvents.getInstance();

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        ContactsEvents eventsData = ContactsEvents.getInstance();

        try {

            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
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
            int prefEventsCountIndex = 0;
            try {
                if (widgetPref.size() > 2) prefEventsCountIndex = Integer.parseInt(widgetPref.get(2));
            } catch (Exception e2) {/**/}

            int prefEventsCountDiff = 0;
            switch (prefEventsCountIndex) {
                case 1:
                    prefEventsCountDiff = -2;
                    break;
                case 2:
                    prefEventsCountDiff = -1;
                    break;
                case 3:
                    prefEventsCountDiff = 1;
                    break;
                case 4:
                    prefEventsCountDiff = 2;
                    break;
            }

            RemoteViews views = getRemoteViews(context, eventsCount + prefEventsCountDiff);

            if (eventsData.preferences_debug_on) {

                final Resources resources = context.getResources();
                final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
                final float density = displayMetrics.density;

                ToastExpander.showText(context, Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                        widgetType + Constants.STRING_COLON + appWidgetId +
                                "\n screen: " + displayMetrics.heightPixels + "x" + displayMetrics.widthPixels + " (density " + density + ")" +
                                "\n layout=" + resources.getResourceEntryName(views.getLayoutId()) +
                                "\n minWidth=" + minWidth + ", minHeight=" + minHeight +
                                "\n widgetPref=" + widgetPref
                        : widgetType + Constants.STRING_COLON + appWidgetId + Constants.STRING_EOL + widgetPref
                );
            }

            new WidgetUpdater(context, eventsData, views, eventsCount + prefEventsCountDiff, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null);
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

            updateAppWidget(context, appWidgetManager, appWidgetId, newOptions);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    static private RemoteViews getRemoteViews(@NonNull Context context, int eventsCount) {

        switch (eventsCount) {
            case -1:
            case 0:
            case 1:
                    return new RemoteViews(context.getPackageName(), R.layout.widget1x1);
            case 2:  return new RemoteViews(context.getPackageName(), R.layout.widget2x1);
            case 3:  return new RemoteViews(context.getPackageName(), R.layout.widget3x1);
            case 4:  return new RemoteViews(context.getPackageName(), R.layout.widget4x1);
            case 5:  return new RemoteViews(context.getPackageName(), R.layout.widget5x1);
            case 6:  return new RemoteViews(context.getPackageName(), R.layout.widget6x1);
            default:  return new RemoteViews(context.getPackageName(), R.layout.widget7x1);
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