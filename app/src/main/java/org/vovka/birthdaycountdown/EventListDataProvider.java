/*
 * *
 *  * Created by Vladimir Belov on 19.03.2025, 01:25
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 19.03.2025, 01:25
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//https://developer.android.com/guide/topics/appwidgets
//https://www.androidauthority.com/create-an-android-widget-1020839/
//https://startandroid.ru/ru/uroki/vse-uroki-spiskom/212-urok-121-vidzhety-spisok.html
//https://stackoverflow.com/questions/12980025/replacing-remoteviewsfactory-on-app-widget-update
public class EventListDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "EventListDataProvider";
    final List<String> eventListView = new ArrayList<>();
    final Context context;
    final Resources resources;
    final int widgetID;
    List<String> widgetPref;
    private List<String> widgetPref_eventInfo = new ArrayList<>();
    boolean widgetPref_DatesInBrackets = false;
    int widgetPref_onClick = 0;
    ContactsEvents eventsData;

    public EventListDataProvider(Context context, Intent intent) {
        this.context = context;
        this.resources = context.getResources();
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

    private void initData() {

        try {

            if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) return;

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            //Получаем данные
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetID);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);

            widgetPref = eventsData.getWidgetPreference(widgetID, widgetType);
            if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {
                eventsData.getEvents(context);
            }

            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                if (widgetPref.get(4).equals(Constants.STRING_EMPTY) || widgetPref.get(4).equals(resources.getString(R.string.pref_EventInfo_None_ID))) {
                    widgetPref.set(4, resources.getString(R.string.widget_config_defaultPref_List).split(Constants.STRING_COMMA)[4]);
                }

                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
                widgetPref_DatesInBrackets = widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_DatesInBrackets_ID));
            }

            widgetPref_onClick = eventsData.preferences_widgets_on_click_action;
            //Если = Constants.STRING_1, значит используем общие настройки
            if (widgetPref.size() > 12 && !widgetPref.get(12).isEmpty() && !widgetPref.get(12).equals(Constants.STRING_1)) {
                try {
                    widgetPref_onClick = Integer.parseInt(widgetPref.get(12));
                } catch (NumberFormatException ignored) { /**/ }
            }

            eventListView.clear();
            List<String> filteredEventList = eventsData.getFilteredEventList(eventsData.eventList, widgetPref);

            //Ограничения объёма
            int maxEvents = 0;
            int maxDays = 0;
            int maxFacts = 0;
            String prefScope = Constants.STRING_EMPTY;
            if (widgetPref.size() > 8) prefScope = widgetPref.get(8);

            if (!TextUtils.isEmpty(prefScope)) {
                Matcher matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE_RAND).matcher(prefScope);
                boolean found = matchScopes.find();
                if (!found) {
                    matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE).matcher(prefScope).reset();
                    found = matchScopes.find();
                }
                if (found) {
                    final String scopeEvents = matchScopes.group(1);
                    if (scopeEvents != null) {
                        List<String> scopeEventsItems = new ArrayList<>(Arrays.asList(resources.getString(R.string.widget_config_scope_events_items).split(Constants.STRING_COMMA, -1)));
                        if (!scopeEvents.equals(Constants.STRING_0) && scopeEventsItems.contains(scopeEvents)) {
                            try {
                                maxEvents = Integer.parseInt(scopeEvents);
                            } catch (NumberFormatException e) { /**/ }
                        }
                    }
                    final String scopeDays = matchScopes.group(2);
                    if (scopeDays != null) {
                        List<String> scopeDaysItems = new ArrayList<>(Arrays.asList(resources.getString(R.string.widget_config_scope_days_items).split(Constants.STRING_COMMA, -1)));
                        if (!scopeDays.equals(Constants.STRING_0) && scopeDaysItems.contains(scopeDays)) {
                            try {
                                maxDays = Integer.parseInt(scopeDays);
                            } catch (NumberFormatException e) { /**/ }
                        }
                    }
                    try {
                        final String scopeFacts = matchScopes.group(3);
                        if (scopeFacts != null) {
                            maxFacts = Integer.parseInt(scopeFacts);
                        }
                    } catch (IndexOutOfBoundsException ignored) { /**/ }
                }
            }

            List<String> eventsPrefList = new ArrayList<>();
            if (widgetPref.size() > 3 && !widgetPref.get(3).isEmpty()) {
                eventsPrefList = Arrays.asList(widgetPref.get(3).split(Constants.REGEX_PLUS));
            }
            if (maxFacts > 0 && eventsPrefList.contains(resources.getString(R.string.pref_EventTypes_Facts))) {
                List<String> sourcesPrefList = new ArrayList<>();
                if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                    sourcesPrefList = Arrays.asList(widgetPref.get(10).split(Constants.REGEX_PLUS));
                }
                List<String> listFacts = eventsData.getNextRandomFacts(maxFacts, new HashSet<>(sourcesPrefList));
                for (String fact: listFacts) {
                    eventListView.add(resources.getString(R.string.event_type_fact_emoji) + Constants.STRING_SPACE + fact);
                }
            }
            if (maxEvents == 0 && maxDays == 0) {
                eventListView.addAll(filteredEventList);
            } else {
                Calendar now = Calendar.getInstance();
                Date currentDay = new Date(now.getTimeInMillis());
                for (int i = 0, filteredEventListSize = filteredEventList.size(); i < filteredEventListSize; i++) {
                    if (maxEvents > 0 && i >= maxEvents) break;
                    String event = filteredEventList.get(i);
                    if (maxDays > 0) {
                        String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                        Date eventDate = null;
                        try {
                            eventDate = ContactsEvents.sdf_DDMMYYYY.parse(singleEventArray[ContactsEvents.Position_eventDateNextTime]);
                        } catch (Exception e) { /**/ }

                        if (eventDate != null) {
                            long countDays = eventsData.countDaysDiff(currentDay, eventDate);
                            if (countDays + 1 > maxDays) break;
                        }
                    }
                    eventListView.add(event);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews views = new RemoteViews(this.context.getPackageName(), R.layout.widgetlist_item);

        try {

            eventsData.setLocale(false);

            //Размер
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP,
                    ContactsEvents.getSizeForWidgetElement(widgetPref, 1, Constants.WIDGET_TEXT_SIZE_TINY, 1.6));

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);
            String eventText;

            if (singleEventArray.length < ContactsEvents.Position_attrAmount) {

                eventText = eventInfo;
                views.setTextViewText(R.id.eventCaption, eventText);

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

                String colorDate = Integer.toHexString(eventsData.preferences_widgets_color_event_far & 0x00ffffff);
                int dateColorId = 3;
                try {
                    if (eventDistance_Days == 0) { //Сегодня

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_event_today & 0x00ffffff);
                        dateColorId = 1;

                    } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_event_soon) { //Скоро

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_event_soon & 0x00ffffff);
                        dateColorId = 2;

                    }
                } catch (Resources.NotFoundException nfe) { /**/ }

                //Составление события
                final String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];
                final String eventKey = eventsData.getEventKey(singleEventArray);
                final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                final String[] eventDistanceInfo = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                boolean colorizeEntireRow = false;

                for (String eventItem: widgetPref_eventInfo) {

                    if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventIcon_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(singleEventArray[ContactsEvents.Position_eventEmoji]).append(Constants.STRING_SPACE);

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_FavIcon_ID))) {

                        if (eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred])) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            sb.append(Constants.eventTitleFavoritePrefix);
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_ZodiacSign_ID))) {

                        if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            final String zodiacSign = singleEventArray[ContactsEvents.Position_zodiacSign];
                            int indexSpace = zodiacSign.indexOf(Constants.STRING_SPACE);
                            if (indexSpace > -1) {
                                sb.append(zodiacSign.substring(0, indexSpace)).append(Constants.STRING_SPACE);
                            } else {
                                sb.append(zodiacSign).append(Constants.STRING_SPACE);
                            }
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_ZodiacYear_ID))) {

                        if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            final String zodiacYear = singleEventArray[ContactsEvents.Position_zodiacYear];
                            int indexSpace = zodiacYear.indexOf(Constants.STRING_SPACE);
                            if (indexSpace > -1) {
                                sb.append(zodiacYear.substring(0, indexSpace)).append(Constants.STRING_SPACE);
                            } else {
                                sb.append(zodiacYear).append(Constants.STRING_SPACE);
                            }
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventCaption_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (!singleEventArray[ContactsEvents.Position_eventCaption].trim().isEmpty()) {
                            sb.append(singleEventArray[ContactsEvents.Position_eventCaption]);
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_Original_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithoutYear);
                        sb.append(surround(String.format(Constants.HTML_COLOR, colorDate, eventDay), widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_Original_WithYear_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
                        sb.append(surround(String.format(Constants.HTML_COLOR, colorDate, eventDay), widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateNextTime], ContactsEvents.FormatDate.WithoutYear);
                        sb.append(surround(String.format(Constants.HTML_COLOR, colorDate, eventDay), widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_WithYear_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateNextTime], ContactsEvents.FormatDate.WithYear);
                        sb.append(surround(String.format(Constants.HTML_COLOR, colorDate, eventDay), widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_DaysBeforeEventShort_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistance_Days < 2) {
                            sb.append(eventDistanceInfo[0]);
                        } else {
                            sb.append(eventDistance).append(eventsData.getResources().getString(R.string.msg_after_day_prefix_short));
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventTitle_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(eventsData.getFullName(singleEventArray));

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_Age_ID))) {

                        if (!singleEventArray[ContactsEvents.Position_age_caption].trim().isEmpty()) {
                            if (sb.length() - sb.lastIndexOf(Constants.HTML_BR) != Constants.HTML_BR.length()) sb.append(Constants.STRING_COLON_SPACE);
                            sb.append(singleEventArray[ContactsEvents.Position_age_caption]);
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_WeddingName_ID))) {

                        if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_Anniversary))) {
                            int ind1 = singleEventArray[ContactsEvents.Position_eventCaption].indexOf(Constants.STRING_PARENTHESIS_OPEN);
                            if (ind1 > -1) {
                                if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                                sb.append(singleEventArray[ContactsEvents.Position_eventCaption].substring(ind1));
                            }
                        }

                    } else if (dateColorId > 2 && eventItem.equals(resources.getString(R.string.pref_EventInfo_DaysBeforeEventFar_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(String.format(Constants.HTML_COLOR, colorDate, eventDistanceInfo[0]));

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_DaysBeforeEvent_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        //sb.append("<div style=\"text-align: end;\">");
                        sb.append(String.format(Constants.HTML_COLOR, colorDate, eventDistanceInfo[0]));

                    } else if (dateColorId > 2 && eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDayOfWeekFar_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistanceInfo.length >= 1) sb.append(eventDistanceInfo[1]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDayOfWeek_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistanceInfo.length >= 1) sb.append(eventDistanceInfo[1]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDayOfWeekShort_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistanceInfo.length >= 3) sb.append(eventDistanceInfo[3]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_SourceIcon_ID))) {

                        String[] dates = singleEventArray[ContactsEvents.Position_dates].split(Constants.STRING_2TILDA, -1);
                        if (dates.length > 0) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) {
                                sb.append(Constants.STRING_SPACE);
                            }
                            sb.append(eventsData.getEventSourceIcon(singleEventArray));
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_LinkIcon_ID))) {

                        if (!singleEventArray[ContactsEvents.Position_eventURL].trim().isEmpty()) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            sb.append("🔗");
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_NewLine1_ID))
                            || eventItem.equals(resources.getString(R.string.pref_EventInfo_NewLine2_ID))
                            || eventItem.equals(resources.getString(R.string.pref_EventInfo_NewLine3_ID))) {

                        sb.append(Constants.HTML_BR);

                    } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_ColorizeEntireRow_ID))) {
                        colorizeEntireRow = true;
                    }

                }

                if (sb.length() - sb.lastIndexOf(Constants.HTML_BR) == Constants.HTML_BR.length()) {
                    sb.setLength(sb.lastIndexOf(Constants.HTML_BR));
                }

                if (colorizeEntireRow) {
                    eventText = String.format(Constants.HTML_COLOR, colorDate, sb);
                } else {
                    eventText = sb.toString();
                }

                views.setTextViewText(R.id.eventCaption, HtmlCompat.fromHtml(eventText, 0));
                views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);

            }

            Intent clickIntent = new Intent();
            clickIntent.putExtra(Constants.EXTRA_CLICKED_EVENT, eventInfo);
            clickIntent.putExtra(Constants.EXTRA_CLICKED_TEXT, eventText);
            clickIntent.putExtra(Constants.EXTRA_CLICKED_PREFS, widgetPref_onClick);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            views.setOnClickFillInIntent(R.id.eventCaption, clickIntent);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return views;

    }

    @Nullable
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

    String surround(@NonNull String str, boolean condition) {
        return !condition ? str : Constants.STRING_PARENTHESIS_START + str + Constants.STRING_PARENTHESIS_CLOSE;
    }
}
