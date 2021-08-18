/*
 * *
 *  * Created by Vladimir Belov on 17.08.2021, 10:49
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 11.08.2021, 22:23
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_EVENT;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR_START;
import static org.vovka.birthdaycountdown.Constants.HTML_FONT_END;
import static org.vovka.birthdaycountdown.Constants.STRING_0;
import static org.vovka.birthdaycountdown.Constants.STRING_1;
import static org.vovka.birthdaycountdown.Constants.STRING_2;
import static org.vovka.birthdaycountdown.Constants.STRING_2HASH;
import static org.vovka.birthdaycountdown.Constants.STRING_3;
import static org.vovka.birthdaycountdown.Constants.STRING_4;
import static org.vovka.birthdaycountdown.Constants.STRING_5;
import static org.vovka.birthdaycountdown.Constants.STRING_6;
import static org.vovka.birthdaycountdown.Constants.STRING_7;
import static org.vovka.birthdaycountdown.Constants.STRING_8;
import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_OPEN;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.Constants.Type_Anniversary;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_TINY;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_age_caption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_attrAmount;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventCaption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventDate;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventEmoji;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventSubType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_personFullName;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_personFullNameAlt;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.List;

//https://developer.android.com/guide/topics/appwidgets
//https://www.androidauthority.com/create-an-android-widget-1020839/
//https://startandroid.ru/ru/uroki/vse-uroki-spiskom/212-urok-121-vidzhety-spisok.html
//https://stackoverflow.com/questions/12980025/replacing-remoteviewsfactory-on-app-widget-update
public class EventListDataProvider implements RemoteViewsService.RemoteViewsFactory {

    final List<String> eventListView = new ArrayList<>();
    final Context context;
    final int widgetID;
    List<String> widgetPref;
    ContactsEvents eventsData;

    public EventListDataProvider(Context context, Intent intent) {
        this.context = context;
        widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        initData();
    }

    @Override
    public void onDestroy() {}

    @Override
    public int getCount() {
        return eventListView.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews views = new RemoteViews(this.context.getPackageName(), R.layout.widgetlist_item);

        try {

            //Размер
            double fontMagnify = 1.6;
            if (widgetPref != null && widgetPref.size() > 1 && !widgetPref.get(1).equals(STRING_0)) {
                switch (widgetPref.get(1)) {
                    case STRING_1:
                        fontMagnify = fontMagnify * 0.5;
                        break;
                    case STRING_2:
                        fontMagnify = fontMagnify * 0.75;
                        break;
                    case STRING_3:
                        fontMagnify = fontMagnify * 0.85;
                        break;
                    case STRING_4:
                        //fontMagnify = fontMagnify * 1;
                        break;
                    case STRING_5:
                        fontMagnify = fontMagnify * 1.2;
                        break;
                    case STRING_6:
                        fontMagnify = fontMagnify * 1.5;
                        break;
                    case STRING_7:
                        fontMagnify = fontMagnify * 1.75;
                        break;
                    case STRING_8:
                        fontMagnify = fontMagnify * 2.0;
                        break;
                }
            }
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_TINY * fontMagnify));

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(STRING_2HASH);
            if (singleEventArray.length < Position_attrAmount) {

                views.setTextViewText(R.id.eventCaption, eventInfo);

            } else {

                StringBuilder sb = new StringBuilder();

                //Иконка
                if (eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_EventIcon)) {
                    sb.append(singleEventArray[Position_eventEmoji]).append(STRING_SPACE);
                }

                //Дата события
                String eventDistance = singleEventArray[ContactsEvents.Position_eventDistance];
                int eventDistance_Days;
                try {
                    eventDistance_Days = Integer.parseInt(eventDistance);
                } catch (Exception e) {
                    eventDistance_Days = 365;
                }

                String colorDate = null;
                try {
                    if (eventDistance_Days == 0) { //Сегодня

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_eventtoday & 0x00ffffff);

                    } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_eventsoon) { //Скоро

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_eventsoon & 0x00ffffff);

                    } else { //Попозже

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_eventfar & 0x00ffffff);

                    }
                } catch (Resources.NotFoundException nfe) { /**/ }
                String eventDay = eventsData.getDateFormated(singleEventArray[Position_eventDate], ContactsEvents.FormatDate.WithoutYear);
                sb.append(colorDate != null ? String.format(HTML_COLOR, colorDate, eventDay) : eventDay).append(STRING_SPACE);

                //Данные события
                String colorEvent = null;
                try {
                    colorEvent = Integer.toHexString(eventsData.preferences_widgets_color_default & 0x00ffffff);
                } catch (Resources.NotFoundException nfe) { /**/ }

                if (colorEvent != null) sb.append(String.format(HTML_COLOR_START, colorEvent));
                sb.append(eventsData.preferences_list_caption == 2 ? singleEventArray[Position_personFullNameAlt] : singleEventArray[Position_personFullName]);
                if (!singleEventArray[Position_age_caption].trim().isEmpty()) sb.append(STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);
                if (singleEventArray[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Type_Anniversary))) {
                    int ind1 = singleEventArray[Position_eventCaption].indexOf(STRING_PARENTHESIS_OPEN);
                    if (ind1 > -1) {
                        sb.append(STRING_SPACE).append(singleEventArray[Position_eventCaption].substring(ind1));
                    }
                }
                if (colorEvent != null) sb.append(HTML_FONT_END);

                views.setTextViewText(R.id.eventCaption, HtmlCompat.fromHtml(sb.toString(), 0));
                views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);

            }

            Intent clickIntent = new Intent();
            clickIntent.putExtra(EXTRA_CLICKED_EVENT, eventInfo);
            views.setOnClickFillInIntent(R.id.eventCaption, clickIntent);

        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).postDelayed(() -> Toast.makeText(context, Constants.EVENT_LIST_DATA_PROVIDER_GETVIEWAT_ERROR + e.toString(), Toast.LENGTH_LONG).show(), 1000);
        }

        return views;

    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void initData() {

        try {

            if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) return;

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;
            eventsData.getPreferences();
            eventsData.setLocale(true);

            //Получаем данные
            widgetPref = eventsData.getWidgetPreference(widgetID);
            if (eventsData.isEmptyArray() || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000) {
                eventsData.context = context;
                if (eventsData.getEvents(context)) eventsData.computeDates();
            }

            eventListView.clear();
            eventListView.addAll(eventsData.getFilteredEventList(eventsData.eventList, widgetPref));

        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).postDelayed(() -> Toast.makeText(context.getApplicationContext(),Constants.EVENT_PHOTO_LIST_DATA_PROVIDER_INIT_DATA_ERROR + e.toString(), Toast.LENGTH_LONG).show(), 1000);
        }

    }
}
