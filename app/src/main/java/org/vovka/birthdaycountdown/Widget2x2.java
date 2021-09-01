/*
 * *
 *  * Created by Vladimir Belov on 17.08.2021, 10:49
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 11.08.2021, 22:23
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

// На 1 событие масштабируемый
public class Widget2x2 extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId) {

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.getPreferences();
            eventsData.setLocale(true);
            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            RemoteViews views = getRemoteViews(context);
            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
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
                eventsData.setLocale(true);
            }

            if (eventsData.preferences_debug_on) {
                List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId);
                final Resources resources = context.getResources();
                final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
                final float density = displayMetrics.density;
                Toast.makeText(context, this.getClass().getName() +
                                "\n appWidgetId=" + appWidgetId +
                                "\n screen: " + displayMetrics.heightPixels + "x" + displayMetrics.widthPixels + " (density " + density + ")" +
                                "\n layout=" + resources.getResourceEntryName(views.getLayoutId()) +
                                "\n minWidth=" + minWidth + ", minHeight=" + minHeight +
                                "\n widgetPref=" + widgetPref.toString()
                        , Toast.LENGTH_LONG).show();
            }

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invokePhotoEventsUpdate();
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

