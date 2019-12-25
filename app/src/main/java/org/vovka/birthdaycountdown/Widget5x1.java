/*
 * *
 *  * Created by Vladimir Belov on 26.12.19 2:44
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 26.12.19 0:19
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import android.util.DisplayMetrics;
import android.widget.RemoteViews;
import android.widget.Toast;

// На 1-7 событий масштабируемый
public class Widget5x1 extends AppWidgetProvider {

    private static void updateAppWidget(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager, int appWidgetId, int eventsCountSuggested) {

        try {

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int eventsCount = (int) (minWidth / getCellSize(context, eventsCountSuggested)) > eventsCountSuggested ? eventsCountSuggested - 1 : eventsCountSuggested;

            RemoteViews views = getRemoteViews(context, eventsCount);
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

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) {
                eventsData.context = context;
                eventsData.getPreferences();
            }

            Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int eventsCountSuggested = getCellsForSize(minWidth);
            int eventsCount = (int) (minWidth / getCellSize(context, eventsCountSuggested)) > eventsCountSuggested ? eventsCountSuggested - 1 : eventsCountSuggested;

            RemoteViews views = getRemoteViews(context, eventsCount);

            if (eventsData.preferences_debug_on) {

                final Resources resources = context.getResources();
                final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
                final float density = displayMetrics.density;
                Toast.makeText(context, this.getClass().getName() +
                                "\n appWidgetId=" + appWidgetId +
                                "\n screen: " + displayMetrics.heightPixels + "x" + displayMetrics.widthPixels + " (density " + density + ")" +
                                "\n dimenSet=" + resources.getString(R.string.dimenSet) +
                                "\n layout=" + resources.getResourceEntryName(views.getLayoutId()) +
                                "\n minWidth=" + minWidth +
                                "\n minHeight=" + minHeight +
                                "\n eventsCountSuggested=" + eventsCountSuggested +
                                "\n eventsCount=" + eventsCount +
                                "\n cellSize=" + getCellSize(context, eventsCountSuggested) +
                        "\n widget_21cellWidth=" + (resources.getDimension(R.dimen.widget_21cellWidth) / density) + "dip or " + resources.getDimension(R.dimen.widget_21cellWidth) + "px" +
                        "\n widget_31cellWidth=" + (resources.getDimension(R.dimen.widget_31cellWidth) / density) + "dip or " + resources.getDimension(R.dimen.widget_31cellWidth) + "px" +
                        "\n widget_41cellWidth=" + (resources.getDimension(R.dimen.widget_41cellWidth) / density) + "dip or " + resources.getDimension(R.dimen.widget_41cellWidth) + "px" +
                        "\n widget_51cellWidth=" + (resources.getDimension(R.dimen.widget_51cellWidth) / density) + "dip or " + resources.getDimension(R.dimen.widget_51cellWidth) + "px" +
                        "\n widget_61cellWidth=" + (resources.getDimension(R.dimen.widget_61cellWidth) / density) + "dip or " + resources.getDimension(R.dimen.widget_61cellWidth) + "px" +
                        "\n widget_71cellWidth=" + (resources.getDimension(R.dimen.widget_71cellWidth) / density) + "dip or " + resources.getDimension(R.dimen.widget_71cellWidth) + "px"
                , Toast.LENGTH_LONG).show();
            }

            new WidgetUpdater(context, ContactsEvents.getInstance(), views, eventsCount, minWidth, minHeight, appWidgetId).invoke();
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
            case 5:  return new RemoteViews(context.getPackageName(), R.layout.widget5x1);
            case 6:  return new RemoteViews(context.getPackageName(), R.layout.widget6x1);
            default:  return new RemoteViews(context.getPackageName(), R.layout.widget7x1);
        }

    }

    static private float getCellSize(@NonNull Context context, int eventsCount) {

        Resources resources = context.getResources();
        switch (eventsCount) {
            case 1:
            case 2:
                     return resources.getDimension(R.dimen.widget_21cellWidth);
            case 3:  return resources.getDimension(R.dimen.widget_31cellWidth);
            case 4:  return resources.getDimension(R.dimen.widget_41cellWidth);
            case 5:  return resources.getDimension(R.dimen.widget_51cellWidth);
            case 6:  return resources.getDimension(R.dimen.widget_61cellWidth);
            default: return resources.getDimension(R.dimen.widget_71cellWidth);
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

