/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 24.12.2021, 18:20
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_EVENT;
import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_PREFS;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR;
import static org.vovka.birthdaycountdown.Constants.REGEX_PLUS;
import static org.vovka.birthdaycountdown.Constants.STRING_0;
import static org.vovka.birthdaycountdown.Constants.STRING_1;
import static org.vovka.birthdaycountdown.Constants.STRING_2;
import static org.vovka.birthdaycountdown.Constants.STRING_2HASH;
import static org.vovka.birthdaycountdown.Constants.STRING_3;
import static org.vovka.birthdaycountdown.Constants.STRING_5;
import static org.vovka.birthdaycountdown.Constants.STRING_6;
import static org.vovka.birthdaycountdown.Constants.STRING_7;
import static org.vovka.birthdaycountdown.Constants.STRING_8;
import static org.vovka.birthdaycountdown.Constants.STRING_BAR;
import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_COMMA;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_PARENTHESIS_OPEN;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.Constants.Type_Anniversary;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_TINY;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_age_caption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_attrAmount;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventCaption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventDate;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventDateText;
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
import java.util.Arrays;
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
    private List<String> widgetPref_eventInfo = new ArrayList<>();
    ContactsEvents eventsData;

    public EventListDataProvider(Context context, Intent intent) {
        this.context = context;
        widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

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
                    case STRING_1: fontMagnify = fontMagnify * 0.5; break;
                    case STRING_2: fontMagnify = fontMagnify * 0.75; break;
                    case STRING_3: fontMagnify = fontMagnify * 0.85; break;
                    case STRING_5: fontMagnify = fontMagnify * 1.2; break;
                    case STRING_6: fontMagnify = fontMagnify * 1.5; break;
                    case STRING_7: fontMagnify = fontMagnify * 1.75; break;
                    case STRING_8: fontMagnify = fontMagnify * 2.0;  break;
                    default: //STRING_4: fontMagnify = fontMagnify * 1;
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

                //Дата события
                String eventDistance = singleEventArray[ContactsEvents.Position_eventDistance];
                int eventDistance_Days;
                try {
                    eventDistance_Days = Integer.parseInt(eventDistance);
                } catch (Exception e) {
                    eventDistance_Days = 365;
                }

                String dateColor = null;
                int dateColorId = 3;
                try {
                    if (eventDistance_Days == 0) { //Сегодня

                        dateColor = Integer.toHexString(eventsData.preferences_widgets_color_eventtoday & 0x00ffffff);
                        dateColorId = 1;

                    } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_eventsoon) { //Скоро

                        dateColor = Integer.toHexString(eventsData.preferences_widgets_color_eventsoon & 0x00ffffff);
                        dateColorId = 2;

                    } else { //Попозже

                        dateColor = Integer.toHexString(eventsData.preferences_widgets_color_eventfar & 0x00ffffff);

                    }
                } catch (Resources.NotFoundException nfe) { /**/ }

                //Составление события

                for (String eventItem: widgetPref_eventInfo) {
                    if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID))) {

                        if (sb.length() > 0) sb.append(STRING_SPACE);
                        sb.append(singleEventArray[Position_eventEmoji]).append(STRING_SPACE);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventDate_Original_ID))) {

                        if (sb.length() > 0) sb.append(STRING_SPACE);
                        String eventDay = eventsData.getDateFormated(singleEventArray[Position_eventDateText], ContactsEvents.FormatDate.WithoutYear);
                        sb.append(dateColor != null ? String.format(HTML_COLOR, dateColor, eventDay) : eventDay);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventDate_Original_WithYear_ID))) {

                        if (sb.length() > 0) sb.append(STRING_SPACE);
                        String eventDay = eventsData.getDateFormated(singleEventArray[Position_eventDateText], ContactsEvents.FormatDate.WithYear);
                        sb.append(dateColor != null ? String.format(HTML_COLOR, dateColor, eventDay) : eventDay);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventDate_ID))) {

                        if (sb.length() > 0) sb.append(STRING_SPACE);
                        String eventDay = eventsData.getDateFormated(singleEventArray[Position_eventDate], ContactsEvents.FormatDate.WithoutYear);
                        sb.append(dateColor != null ? String.format(HTML_COLOR, dateColor, eventDay) : eventDay);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventDate_WithYear_ID))) {

                        if (sb.length() > 0) sb.append(STRING_SPACE);
                        String eventDay = eventsData.getDateFormated(singleEventArray[Position_eventDate], ContactsEvents.FormatDate.WithYear);
                        sb.append(dateColor != null ? String.format(HTML_COLOR, dateColor, eventDay) : eventDay);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventTitle_ID))) {

                        if (sb.length() > 0) sb.append(STRING_SPACE);
                        sb.append(eventsData.preferences_list_nameformat == 2 ? singleEventArray[Position_personFullNameAlt] : singleEventArray[Position_personFullName]);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_Age_ID))) {

                        if (!singleEventArray[Position_age_caption].trim().isEmpty()) sb.append(STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);

                    } else if (eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_WeddingName_ID))) {

                        if (singleEventArray[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Type_Anniversary))) {
                            int ind1 = singleEventArray[Position_eventCaption].indexOf(STRING_PARENTHESIS_OPEN);
                            if (ind1 > -1) {
                                if (sb.length() > 0) sb.append(STRING_SPACE);
                                sb.append(singleEventArray[Position_eventCaption].substring(ind1));
                            }
                        }

                    } else if (dateColorId > 2 && eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent_ID))) {

                        String text = singleEventArray[ContactsEvents.Position_eventDistanceText];
                        final int barIndex = text.indexOf(STRING_BAR);
                        if (barIndex > -1) {
                            if (sb.length() > 0) sb.append(STRING_SPACE);
                            text = text.substring(0, barIndex);
                            sb.append(dateColor != null ? String.format(HTML_COLOR, dateColor, text) : text);
                        }

                    } else if (dateColorId > 2 && eventItem.equals(context.getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek_ID))) {

                        String text = singleEventArray[ContactsEvents.Position_eventDistanceText];
                        final int barIndex = text.indexOf(STRING_BAR);
                        if (barIndex > -1) {
                            if (sb.length() > 0) sb.append(STRING_SPACE);
                            sb.append(text.substring(barIndex + 1));
                        }

                    }

                }



/*
                //Иконка
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_EventIcon)
                        : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_EventIcon)) {
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

                String dateColor = null;
                try {
                    if (eventDistance_Days == 0) { //Сегодня

                        dateColor = Integer.toHexString(eventsData.preferences_widgets_color_eventtoday & 0x00ffffff);

                    } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_eventsoon) { //Скоро

                        dateColor = Integer.toHexString(eventsData.preferences_widgets_color_eventsoon & 0x00ffffff);

                    } else { //Попозже

                        dateColor = Integer.toHexString(eventsData.preferences_widgets_color_eventfar & 0x00ffffff);

                    }
                } catch (Resources.NotFoundException nfe) { *//**//* }
                String eventDay = eventsData.getDateFormated(singleEventArray[Position_eventDateText], singleEventArray[Position_age].equals(STRING_MINUS1) ?
                        ContactsEvents.FormatDate.WithoutYear : ContactsEvents.FormatDate.WithYear);
                sb.append(dateColor != null ? String.format(HTML_COLOR, dateColor, eventDay) : eventDay).append(STRING_SPACE);

                //Данные события
                String colorEvent = null;
                try {
                    colorEvent = Integer.toHexString(eventsData.preferences_widgets_color_default & 0x00ffffff);
                } catch (Resources.NotFoundException nfe) { *//**//* }

                if (colorEvent != null) sb.append(String.format(HTML_COLOR_START, colorEvent));
                sb.append(eventsData.preferences_list_nameformat == 2 ? singleEventArray[Position_personFullNameAlt] : singleEventArray[Position_personFullName]);
                if (!singleEventArray[Position_age_caption].trim().isEmpty()) sb.append(STRING_COLON_SPACE).append(singleEventArray[Position_age_caption]);
                if (singleEventArray[Position_eventSubType].equals(ContactsEvents.eventTypesIDs.get(Type_Anniversary))) {
                    int ind1 = singleEventArray[Position_eventCaption].indexOf(STRING_PARENTHESIS_OPEN);
                    if (ind1 > -1) {
                        sb.append(STRING_SPACE).append(singleEventArray[Position_eventCaption].substring(ind1));
                    }
                }
                if (colorEvent != null) sb.append(HTML_FONT_END);*/

                views.setTextViewText(R.id.eventCaption, HtmlCompat.fromHtml(sb.toString(), 0));
                views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);

            }

            Intent clickIntent = new Intent();
            clickIntent.putExtra(EXTRA_CLICKED_EVENT, eventInfo);
            clickIntent.putExtra(EXTRA_CLICKED_PREFS, eventsData.preferences_widgets_on_click_action);
            views.setOnClickFillInIntent(R.id.eventCaption, clickIntent);

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) handler.post(() -> Toast.makeText(context, Constants.EVENT_LIST_DATA_PROVIDER_GETVIEWAT_ERROR + e, Toast.LENGTH_LONG).show());
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
            String widgetType = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetID).provider.getShortClassName();
            widgetPref = eventsData.getWidgetPreference(widgetID, widgetType);
            if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000) {
                eventsData.context = context;
                if (eventsData.getEvents(context)) eventsData.computeDates();
            }
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                if (widgetPref.get(4).equals(STRING_EMPTY) || widgetPref.get(4).equals(context.getString(R.string.pref_Widgets_EventInfo_None_ID))) {
                    widgetPref.set(4, context.getString(R.string.widget_config_defaultPref_List).split(STRING_COMMA)[4]);
                }

                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(REGEX_PLUS));
            }

            eventListView.clear();
            eventListView.addAll(eventsData.getFilteredEventList(eventsData.eventList, widgetPref));

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) handler.post(() -> Toast.makeText(context.getApplicationContext(),Constants.EVENT_PHOTO_LIST_DATA_PROVIDER_INIT_DATA_ERROR + e, Toast.LENGTH_LONG).show());
        }

    }
}
