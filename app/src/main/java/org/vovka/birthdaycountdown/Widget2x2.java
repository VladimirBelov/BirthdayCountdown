/*
 * *
 *  * Created by Vladimir Belov on 17.12.19 8:42
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 08.12.19 16:02
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.Toast;

// На 1 событие масштабируемый
public class Widget2x2 extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId, @SuppressWarnings("SameParameterValue") int eventsCount) {

        try {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            RemoteViews views = getRemoteViews(context);
            new WidgetUpdater(context, ContactsEvents.getInstance(), views, eventsCount, minWidth, minHeight, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_2_X_2_UPDATE_APP_WIDGET_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 1);
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

            RemoteViews views = getRemoteViews(context);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) {
                eventsData.context = context;
                eventsData.getPreferences();
            }
            if (eventsData.preferences_debug_on) {
                Toast.makeText(context, this.getClass().getName() +
                        "\n minWidth=" + minWidth + ", minHeight=" + minHeight +
                        "\n layout=" + context.getResources().getResourceEntryName(views.getLayoutId()) +
                        "\n appWidgetId=" + appWidgetId +
                        "\n screen: " + context.getResources().getDisplayMetrics().heightPixels + "x" + context.getResources().getDisplayMetrics().widthPixels + " (density " + context.getResources().getDisplayMetrics().density + ")" +
                        "\n dimenSet=" + context.getResources().getString(R.string.dimenSet), Toast.LENGTH_LONG).show();
            }

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_2_X_2_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    static private RemoteViews getRemoteViews(@NonNull Context context) {

        return new RemoteViews(context.getPackageName(), R.layout.widget2x2);

    }

}

