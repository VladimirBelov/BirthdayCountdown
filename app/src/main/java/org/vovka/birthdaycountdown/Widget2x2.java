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

// На 1 событие масштабируемый
public class Widget2x2 extends AppWidgetProvider {

    private static final String TAG = "Widget2x2";
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
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);
            RemoteViews views = getRemoteViews(context);

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

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
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

        try {

            updateAppWidget(context, appWidgetManager, appWidgetId, newOptions);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    static private RemoteViews getRemoteViews(@NonNull Context context) {

        return new RemoteViews(context.getPackageName(), R.layout.widget2x2);

    }

}

