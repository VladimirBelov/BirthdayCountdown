/*
 * *
 *  * Created by Vladimir Belov on 08.12.19 16:02
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 01.12.19 19:06
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

// На 1-7 событий масштабируемый
public class Widget5x1 extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId, int eventsCount) {

        try {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            RemoteViews views = getRemoteViews(context, getCellsForSize(minWidth));
            new WidgetUpdater(context, ContactsEvents.getInstance(), views, eventsCount, minWidth, minHeight, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_5_X_1_UPDATE_APP_WIDGET_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            updateAppWidget(context, appWidgetManager, appWidgetId, getCellsForSize(minWidth));

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

        try{

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

            RemoteViews views = getRemoteViews(context, getCellsForSize(minWidth));

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) {
                eventsData.context = context;
                eventsData.getPreferences();
            }
            if (eventsData.preferences_debug_on) {
                Toast.makeText(context, this.getClass().getName() +
                        "\n minWidth=" + minWidth +
                        "\n getCellsForSize=" + getCellsForSize(minWidth) +
                        "\n layout=" + context.getResources().getResourceEntryName(views.getLayoutId()) +
                        "\n appWidgetId=" + appWidgetId +
                        "\n screen: " + context.getResources().getDisplayMetrics().heightPixels + "x" + context.getResources().getDisplayMetrics().widthPixels + " (density " + context.getResources().getDisplayMetrics().density + ")" +
                        "\n dimenSet=" + context.getResources().getString(R.string.dimenSet), Toast.LENGTH_LONG).show();
            }

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, getCellsForSize(minWidth), minWidth, minHeight, appWidgetId).invoke();
            appWidgetManager.updateAppWidget(appWidgetId, views);
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_5_X_1_ON_APP_WIDGET_OPTIONS_CHANGED_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    static private RemoteViews getRemoteViews(@NonNull Context context, int eventsCount) {

        switch (eventsCount) {
            case 1:  return new RemoteViews(context.getPackageName(), R.layout.widget1x1);
            case 2:  return new RemoteViews(context.getPackageName(), R.layout.widget2x1);
            case 3:  return new RemoteViews(context.getPackageName(), R.layout.widget3x1);
            case 4:  return new RemoteViews(context.getPackageName(), R.layout.widget4x1);
            //case 5:  return new RemoteViews(context.getPackageName(), R.layout.widget5x1);
            case 6:  return new RemoteViews(context.getPackageName(), R.layout.widget6x1);
            case 7:  return new RemoteViews(context.getPackageName(), R.layout.widget7x1);
            default: return new RemoteViews(context.getPackageName(), R.layout.widget5x1);
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

