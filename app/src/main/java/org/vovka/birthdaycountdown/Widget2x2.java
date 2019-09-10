package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class Widget2x2 extends AppWidgetProvider {

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, @SuppressWarnings("SameParameterValue") int eventsCount) {

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        RemoteViews views = getRemoteViews(context, getCellsForSize(minWidth));
        new WidgetUpdater(context, ContactsEvents.getInstance(), views, eventsCount, minWidth, minHeight, appWidgetId).invoke();
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {

            //Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
            //int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            updateAppWidget(context, appWidgetManager, appWidgetId, 1);

        }
    }

    @Override
    public void onDeleted (Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().remove(context.getString(R.string.widget_config_PrefName) + appWidgetId).apply();
            //Toast.makeText(context, "widget " + appWidgetId + " removed", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);

        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

        RemoteViews views = getRemoteViews(context, getCellsForSize(minWidth));
        //Toast.makeText(context, "2x2 minWidth=" + minWidth + ", getCellsForSize=" + getCellsForSize(minWidth) + ", layoutId=" + views.getLayoutId(), Toast.LENGTH_SHORT).show();
        new WidgetUpdater(context, ContactsEvents.getInstance(), views, 1, minWidth, minHeight, appWidgetId).invoke();
        appWidgetManager.updateAppWidget(appWidgetId, views);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    static private RemoteViews getRemoteViews(Context context, @SuppressWarnings("unused") int eventsCount) {

        //switch (eventsCount) {
            /*case 1:  return new RemoteViews(context.getPackageName(), R.layout.widget2x2);
            case 2:  return new RemoteViews(context.getPackageName(), R.layout.widget2x1);
            case 4:  return new RemoteViews(context.getPackageName(), R.layout.widget4x1);
            case 5:  return new RemoteViews(context.getPackageName(), R.layout.widget5x1);*/
        //    default: return new RemoteViews(context.getPackageName(), R.layout.widget2x2);
        //}
        return new RemoteViews(context.getPackageName(), R.layout.widget2x2);

    }

    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < (size + 6)) {
            ++n;
        }
        return n - 1;
    }
}

