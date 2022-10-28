/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 16.09.2022, 8:23
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventDate;

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
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews views = new RemoteViews(this.context.getPackageName(), R.layout.widgetlist_item);

        try {

            eventsData.setLocale(false);

            //Размер
            double fontMagnify = 1.6;
            if (widgetPref != null && widgetPref.size() > 1 && !widgetPref.get(1).equals(Constants.STRING_0)) {
                switch (widgetPref.get(1)) {
                    case Constants.STRING_1: fontMagnify = fontMagnify * 0.5; break;
                    case Constants.STRING_2: fontMagnify = fontMagnify * 0.65; break;
                    case Constants.STRING_3: fontMagnify = fontMagnify * 0.75; break;
                    case Constants.STRING_4: fontMagnify = fontMagnify * 0.85; break;
                    case Constants.STRING_5: fontMagnify = fontMagnify * 1; break;
                    case Constants.STRING_6: fontMagnify = fontMagnify * 1.2; break;
                    case Constants.STRING_7: fontMagnify = fontMagnify * 1.5; break;
                    case Constants.STRING_8: fontMagnify = fontMagnify * 1.75; break;
                    case Constants.STRING_9: fontMagnify = fontMagnify * 2.0;  break;
                }
            }
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);

            if (singleEventArray.length < ContactsEvents.Position_attrAmount) {

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
                final String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];
                final String[] eventDistanceInfo = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);

                for (String eventItem: widgetPref_eventInfo) {

                    if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(singleEventArray[ContactsEvents.Position_eventEmoji]).append(Constants.STRING_SPACE);

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_ZodiacSign_ID))) {

                        if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_5K))) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            final String zodiacSign = singleEventArray[ContactsEvents.Position_zodiacSign];
                            int indexSpace = zodiacSign.indexOf(Constants.STRING_SPACE);
                            if (indexSpace > -1) {
                                sb.append(zodiacSign.substring(0, indexSpace)).append(Constants.STRING_SPACE);
                            } else {
                                sb.append(zodiacSign).append(Constants.STRING_SPACE);
                            }
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID))) {

                        if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_5K))) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            final String zodiacYear = singleEventArray[ContactsEvents.Position_zodiacYear];
                            int indexSpace = zodiacYear.indexOf(Constants.STRING_SPACE);
                            if (indexSpace > -1) {
                                sb.append(zodiacYear.substring(0, indexSpace)).append(Constants.STRING_SPACE);
                            } else {
                                sb.append(zodiacYear).append(Constants.STRING_SPACE);
                            }
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventCaption_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (!singleEventArray[ContactsEvents.Position_eventCaption].trim().isEmpty()) {
                            sb.append(singleEventArray[ContactsEvents.Position_eventCaption]);
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDate_Original_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormated(singleEventArray[ContactsEvents.Position_eventDateText], ContactsEvents.FormatDate.WithoutYear);
                        sb.append(surround(dateColor != null ? String.format(Constants.HTML_COLOR, dateColor, eventDay) : eventDay, widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDate_Original_WithYear_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormated(singleEventArray[ContactsEvents.Position_eventDateText], ContactsEvents.FormatDate.WithYear);
                        sb.append(surround(dateColor != null ? String.format(Constants.HTML_COLOR, dateColor, eventDay) : eventDay, widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDate_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormated(singleEventArray[ContactsEvents.Position_eventDate], ContactsEvents.FormatDate.WithoutYear);
                        sb.append(surround(dateColor != null ? String.format(Constants.HTML_COLOR, dateColor, eventDay) : eventDay, widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDate_WithYear_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        final String eventDay = eventsData.getDateFormated(singleEventArray[ContactsEvents.Position_eventDate], ContactsEvents.FormatDate.WithYear);
                        sb.append(surround(dateColor != null ? String.format(Constants.HTML_COLOR, dateColor, eventDay) : eventDay, widgetPref_DatesInBrackets));

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_DaysBeforeEventShort_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistance_Days < 2) {
                            sb.append(eventDistanceInfo[0]);
                        } else {
                            sb.append(eventDistance).append(eventsData.getResources().getString(R.string.msg_after_day_prefix_short));
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventTitle_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(eventsData.preferences_list_nameformat == 2 ? singleEventArray[ContactsEvents.Position_personFullNameAlt] : singleEventArray[ContactsEvents.Position_personFullName]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_Age_ID))) {

                        if (!singleEventArray[ContactsEvents.Position_age_caption].trim().isEmpty()) {
                            if (sb.length() - sb.lastIndexOf(Constants.HTML_BR) != Constants.HTML_BR.length()) sb.append(Constants.STRING_COLON_SPACE);
                            sb.append(singleEventArray[ContactsEvents.Position_age_caption]);
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_WeddingName_ID))) {

                        if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_Anniversary))) {
                            int ind1 = singleEventArray[ContactsEvents.Position_eventCaption].indexOf(Constants.STRING_PARENTHESIS_OPEN);
                            if (ind1 > -1) {
                                if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                                sb.append(singleEventArray[ContactsEvents.Position_eventCaption].substring(ind1));
                            }
                        }

                    } else if (dateColorId > 2 && eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_DaysBeforeEventFar_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(dateColor != null ? String.format(Constants.HTML_COLOR, dateColor, eventDistanceInfo[0]) : eventDistanceInfo[0]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        sb.append(dateColor != null ? String.format(Constants.HTML_COLOR, dateColor, eventDistanceInfo[0]) : eventDistanceInfo[0]);

                    } else if (dateColorId > 2 && eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDayOfWeekFar_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistanceInfo.length >= 1) sb.append(eventDistanceInfo[1]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistanceInfo.length >= 1) sb.append(eventDistanceInfo[1]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_EventDayOfWeekShort_ID))) {

                        if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                        if (eventDistanceInfo.length >= 3) sb.append(eventDistanceInfo[3]);

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_SourceIcon_ID))) {

                        String[] dates = singleEventArray[ContactsEvents.Position_dates].split(Constants.STRING_2TILDA, -1);
                        if (dates.length > 0) {
                            boolean[] sources = new boolean[]{false, false, false};
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            for (String date : dates) {
                                if (date.startsWith(Constants.EVENT_PREFIX_CALENDAR_EVENT) && !sources[1]) {
                                    sb.append("📆");
                                    sources[1] = true;
                                } else if (date.startsWith(Constants.EVENT_PREFIX_FILE_EVENT) && !sources[2]) {
                                    sb.append("📁");
                                    sources[2] = true;
                                } else if (!sources[0]) {
                                    sb.append("👨‍💼");
                                    sources[0] = true;
                                }
                            }
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_LinkIcon_ID))) {

                        if (!singleEventArray[ContactsEvents.Position_eventURL].trim().isEmpty()) {
                            if (sb.length() > 0 && (sb.length() - sb.lastIndexOf(Constants.HTML_BR)) != Constants.HTML_BR.length()) sb.append(Constants.STRING_SPACE);
                            sb.append("🔗");
                        }

                    } else if (eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_NewLine1_ID))
                            || eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_NewLine2_ID))
                            || eventItem.equals(resources.getString(R.string.pref_Widgets_EventInfo_NewLine3_ID))) {

                        sb.append(Constants.HTML_BR);

                    }

                }

                if (sb.length() - sb.lastIndexOf(Constants.HTML_BR) == Constants.HTML_BR.length()) {
                    sb.setLength(sb.lastIndexOf(Constants.HTML_BR));
                }

                views.setTextViewText(R.id.eventCaption, HtmlCompat.fromHtml(sb.toString(), 0));
                views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);

            }

            Intent clickIntent = new Intent();
            clickIntent.putExtra(Constants.EXTRA_CLICKED_EVENT, eventInfo);
            clickIntent.putExtra(Constants.EXTRA_CLICKED_PREFS, eventsData.preferences_widgets_on_click_action);
            views.setOnClickFillInIntent(R.id.eventCaption, clickIntent);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);

            //Получаем данные
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetID);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            widgetPref = eventsData.getWidgetPreference(widgetID, widgetType);
            if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {
                if (eventsData.getEvents(context)) eventsData.computeDates();
            }
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                if (widgetPref.get(4).equals(Constants.STRING_EMPTY) || widgetPref.get(4).equals(resources.getString(R.string.pref_Widgets_EventInfo_None_ID))) {
                    widgetPref.set(4, resources.getString(R.string.widget_config_defaultPref_List).split(Constants.STRING_COMMA)[4]);
                }

                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
                widgetPref_DatesInBrackets = widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_DatesInBrackets_ID));
            }

            eventListView.clear();
            List<String> filteredEventList = eventsData.getFilteredEventList(eventsData.eventList, widgetPref);

            //Ограничения объёма
            int maxEvents = 0;
            int maxDays = 0;
            String prefScope = Constants.STRING_EMPTY;
            if (widgetPref.size() > 8) prefScope = widgetPref.get(8);

            if (!TextUtils.isEmpty(prefScope)) {
                Matcher matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE).matcher(prefScope);
                if (matchScopes.find()) {
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
                }
            }

            if (maxEvents == 0 && maxDays == 0) {
                eventListView.addAll(eventsData.getFilteredEventList(eventsData.eventList, widgetPref));
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
                            eventDate = eventsData.sdf_DDMMYYYY.parse(singleEventArray[Position_eventDate]);
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
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }

    String surround(@NonNull String str, boolean condition) {return !condition ? str : Constants.STRING_PARENTHESIS_START + str + Constants.STRING_PARENTHESIS_CLOSE;}
}
