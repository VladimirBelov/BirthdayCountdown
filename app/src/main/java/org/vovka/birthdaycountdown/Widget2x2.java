/*
 * *
 *  * Created by Vladimir Belov on 20.02.20 1:25
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 19.02.20 0:04
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;

// На 1 событие масштабируемый
public class Widget2x2 extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        try {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            RemoteViews views = getRemoteViews(context);
            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_2_X_2_UPDATE_APP_WIDGET_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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

            eventsData.removeWidgetPreference(appWidgetId);

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
                        "\n appWidgetId=" + appWidgetId +
                        "\n screen: " + context.getResources().getDisplayMetrics().heightPixels + "x" + context.getResources().getDisplayMetrics().widthPixels + " (density " + context.getResources().getDisplayMetrics().density + ")" +
                        "\n layout=" + context.getResources().getResourceEntryName(views.getLayoutId()) +
                        "\n minWidth=" + minWidth + ", minHeight=" + minHeight
                , Toast.LENGTH_LONG).show();
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

