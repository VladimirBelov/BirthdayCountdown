/*
 * *
 *  * Created by Vladimir Belov on 18.03.2025, 02:16
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 15.03.2025, 16:32
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetPinnedReceiver extends BroadcastReceiver {

    private static final String TAG = "WidgetPinnedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            int widgetId = eventsData.pinnedWidgetId;
            if (widgetId == 0) return;

            String widgetType = null;
                final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
                if (appWidgetInfo != null) {
                    widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
                }

            Intent intentConfig = new Intent(context, Constants.WIDGET_TYPE_CALENDAR.equals(widgetType) ?
                    WidgetCalendarConfigureActivity.class : WidgetConfigureActivity.class);
            intentConfig.setAction(Constants.ACTION_LAUNCH);
            intentConfig.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intentConfig.putExtra(Constants.EXTRA_NEW_WIDGET, Constants.STRING_1);
            intentConfig.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intentConfig);
            } catch (android.content.ActivityNotFoundException e) { /**/ }

                eventsData.pinnedWidgetId = 0;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
