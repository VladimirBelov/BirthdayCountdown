/*
 * *
 *  * Created by Vladimir Belov on 27.11.19 13:35
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 27.11.19 13:35
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.Toast;

// На 5 событий
public class Widget4x1 extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        try {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget4x1bc);
            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 5, -1, -1, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_4_X_1_UPDATE_APP_WIDGET_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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
        ContactsEvents eventsData = ContactsEvents.getInstance();

        for (int appWidgetId : appWidgetIds) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(context.getString(R.string.widget_config_PrefName) + appWidgetId).apply();
            if (eventsData.preferences_debug_on) Toast.makeText(context, String.format(Constants.MSG_WIDGETS_REMOVED, appWidgetId), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        try {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget4x1bc);

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, getCellsForSize(minWidth), minWidth, minHeight, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_4_X_1_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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

