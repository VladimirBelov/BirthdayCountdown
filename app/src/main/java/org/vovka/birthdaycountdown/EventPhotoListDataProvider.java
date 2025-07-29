/*
 * *
 *  * Created by Vladimir Belov on 30.07.2025, 01:18
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 30.07.2025, 01:18
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EventPhotoListDataProvider - это поставщик данных для спискового виджета с фото.
 * Он реализует интерфейс RemoteViewsService.RemoteViewsFactory для предоставления данных ListView виджета.
 * Он получает данные о событиях, обрабатывает их и предоставляет отформатированные данные и изображения для отображения в виджете.
 */
public class EventPhotoListDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "EventPhotoListProvider";
    final List<String> eventListView = new ArrayList<>();
    final Context context;
    final Resources resources;
    final int widgetID;
    int widgetWidth;
    float floatDensity;
    List<String> widgetPref;
    private List<String> widgetPref_eventInfo = new ArrayList<>();
    int widgetPref_onClick = 0;
    ContactsEvents eventsData;

    public EventPhotoListDataProvider(Context context, Intent intent) {
        this.context = context;
        this.resources = context.getResources();
        this.widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
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

        RemoteViews views = new RemoteViews(this.context.getPackageName(), R.layout.widgetlist_photo_item);

        try {

            //Размер
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP,
                    ContactsEvents.getSizeForWidgetElement(widgetPref, 1, Constants.WIDGET_TEXT_SIZE_SMALL, 1.2));
            views.setTextViewTextSize(R.id.eventDetails, TypedValue.COMPLEX_UNIT_SP,
                    ContactsEvents.getSizeForWidgetElement(widgetPref, 1, Constants.WIDGET_TEXT_SIZE_TINY, 1.2));

            views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);
            views.setTextColor(R.id.eventDetails, eventsData.preferences_widgets_color_default);

            if (eventListView.size() < position + 1) return views;

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);
            String colorDate = Integer.toHexString(eventsData.preferences_widgets_color_event_far & 0x00ffffff);

            if (singleEventArray.length < ContactsEvents.Position_attrAmount) {

                views.setTextViewText(R.id.eventCaption, eventInfo);

            } else {

                //Цвет даты события
                String eventDistance = singleEventArray[ContactsEvents.Position_eventDistance];
                int eventDistance_Days;
                try {
                    eventDistance_Days = Integer.parseInt(eventDistance);
                } catch (Exception e) {
                    eventDistance_Days = 365;
                }

                try {
                    if (eventDistance_Days == 0) { //Сегодня

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_event_today & 0x00ffffff);

                    } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_event_soon) { //Скоро

                        colorDate = Integer.toHexString(eventsData.preferences_widgets_color_event_soon & 0x00ffffff);

                    }
                } catch (Resources.NotFoundException nfe) { /**/ }
                final boolean colorizeEntireRow = widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_ColorizeEntireRow_ID));

                //Заголовок
                final String eventKey = eventsData.getEventKey(singleEventArray);
                final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                String eventCaption = eventsData.getFullName(singleEventArray);
                //Иконка избранного
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_FavIcon_ID))) {
                    if (eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred])) {
                        eventCaption = eventCaption
                                .concat(Constants.STRING_SPACE)
                                .concat(ContactsEvents.substringBefore(resources.getString(R.string.pref_EventInfo_FavIcon), Constants.STRING_SPACE));
                    }
                }
                if (colorizeEntireRow) {
                    views.setTextViewText(R.id.eventCaption, HtmlCompat.fromHtml(
                                    Constants.HTML_COLOR_START + colorDate + Constants.HTML_COLOR_MIDDLE + eventCaption + Constants.HTML_COLOR_END
                            , HtmlCompat.FROM_HTML_MODE_LEGACY));
                } else {
                    views.setTextViewText(R.id.eventCaption, eventCaption);
                }

                StringBuilder sbDetails = new StringBuilder();

                //Организация и должность
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_Organization_ID))) {
                    final String contactOrganization = ContactsEvents.checkForNull(singleEventArray[ContactsEvents.Position_organization]).trim();
                    if (!contactOrganization.isEmpty()) sbDetails.append(contactOrganization.trim());
                }
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_JobTitle_ID))) {
                    final String positionJobTitle = singleEventArray[ContactsEvents.Position_title];
                    if (!TextUtils.isEmpty(positionJobTitle)) {
                        if (sbDetails.length() > 0) sbDetails.append(Constants.STRING_COMMA_SPACE);
                        sbDetails.append(positionJobTitle.trim());
                    }
                }

                //Иконка
                boolean isEventTypeIcon = false;
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_EventInfo_EventIcon_ID))
                        : widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventIcon_ID))) {
                    if (sbDetails.length() > 0) sbDetails.append(Constants.HTML_BR);
                    sbDetails.append(singleEventArray[ContactsEvents.Position_eventEmoji]).append(Constants.STRING_SPACE);
                    isEventTypeIcon = true;
                }

                //Наименование события и возраст
                boolean isLabel = false;
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventCaption_ID))) {
                    if (sbDetails.length() > 0 && !isEventTypeIcon) sbDetails.append(Constants.HTML_BR);
                    sbDetails.append(singleEventArray[ContactsEvents.Position_eventCaption]);
                    if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventLabel_ID))) {
                        if (!singleEventArray[ContactsEvents.Position_eventLabel].trim().isEmpty()) {
                            sbDetails
                                    .append(Constants.STRING_PARENTHESIS_OPEN)
                                    .append(singleEventArray[ContactsEvents.Position_eventLabel])
                                    .append(Constants.STRING_PARENTHESIS_CLOSE);
                            isLabel = true;
                        }
                    }
                } else if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventLabel_ID))) {
                    if (!singleEventArray[ContactsEvents.Position_eventLabel].trim().isEmpty()) {
                        sbDetails.append(singleEventArray[ContactsEvents.Position_eventLabel]);
                        isLabel = true;
                    }
                }
                if (!singleEventArray[ContactsEvents.Position_age_caption].trim().isEmpty()) {
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_EventInfo_Age_ID))
                            : widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_Age_ID))) {
                        if ((widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventCaption_ID)) || isLabel))
                            sbDetails.append(Constants.STRING_COLON_SPACE);
                        sbDetails.append(singleEventArray[ContactsEvents.Position_age_caption]);
                    }
                }
                //Текущий возраст
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_CurrentAge_ID)) && !singleEventArray[ContactsEvents.Position_eventDistance].equals(Constants.STRING_0)) {
                    final String currentAge = singleEventArray[ContactsEvents.Position_age_current];
                    if (!TextUtils.isEmpty(currentAge)) {
                        if (sbDetails.length() > 0) sbDetails.append(Constants.HTML_BR);
                        int ind = currentAge.indexOf(Constants.STRING_PARENTHESIS_OPEN);
                        sbDetails.append(ind != -1 ? currentAge.substring(0, ind) : currentAge);
                    }
                }

                //Знак зодиака и животное в восточном календаре
                String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];

                if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {

                    String strZodiacInfo = Constants.STRING_EMPTY;
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_EventInfo_ZodiacSign_ID))
                            : widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_ZodiacSign_ID))) {
                        strZodiacInfo = singleEventArray[ContactsEvents.Position_zodiacSign].trim();
                    }

                    String strZodiacYearInfo = Constants.STRING_EMPTY;
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_EventInfo_ZodiacYear_ID))
                            : widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_ZodiacYear_ID))) {
                        strZodiacYearInfo = singleEventArray[ContactsEvents.Position_zodiacYear].trim();
                    }

                    if (!strZodiacInfo.isEmpty() || !strZodiacYearInfo.isEmpty()) {
                        if (sbDetails.length() > 0) sbDetails.append(Constants.HTML_BR);
                        sbDetails.append((strZodiacInfo.concat(Constants.STRING_SPACE).concat(strZodiacYearInfo)).trim());
                    }
                }

                //Срок до события и день недели
                final String[] eventDistanceText = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                final boolean showDistance = widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_DaysBeforeEvent_ID));
                final boolean showDayOfWeek = widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventDayOfWeek_ID));
                final boolean showEventDate = widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_EventDate_ID));

                if ((showDistance || showDayOfWeek || showEventDate) && eventDistanceText.length >= 3) {
                    if (sbDetails.length() > 0) sbDetails.append(Constants.HTML_BR);

                    StringBuilder textDistance = new StringBuilder();
                    if (showDistance) textDistance.append(eventDistanceText[0]);
                    if (!singleEventArray[ContactsEvents.Position_eventDistance].equals(Constants.STRING_0)) {
                        if (showDayOfWeek) {
                            if (textDistance.length() > 0) textDistance.append(Constants.STRING_SPACE);
                            textDistance.append(eventDistanceText[1]);
                        }
                        if (showEventDate) {
                            if (textDistance.length() > 0) textDistance.append(Constants.STRING_COMMA_SPACE);
                            textDistance.append(eventDistanceText[2]);
                        }
                    }

                    sbDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE)
                            .append(textDistance).append(Constants.HTML_COLOR_END);
                }

                String eventDetails;
                if (colorizeEntireRow) {
                    eventDetails = Constants.HTML_COLOR_START + colorDate + Constants.HTML_COLOR_MIDDLE + sbDetails + Constants.HTML_COLOR_END;
                } else {
                    eventDetails = sbDetails.toString();
                }

                views.setTextViewText(R.id.eventDetails, HtmlCompat.fromHtml(eventDetails, HtmlCompat.FROM_HTML_MODE_LEGACY));

                //Фото
                views.setImageViewBitmap(R.id.eventPhoto, null);
                views.setViewVisibility(R.id.eventPhoto, View.GONE);
                Bitmap photo = null;
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_EventInfo_Photo_ID))
                        : widgetPref_eventInfo.contains(resources.getString(R.string.pref_EventInfo_Photo_ID))) {
                    int roundingFactor = getRoundingFactor();
                    photo = eventsData.getEventPhoto(eventInfo, true, true, false, roundingFactor);
                }
                if (photo != null) {
                    int outWidth;
                    if (widgetWidth > 0) {
                        outWidth = (int) ((widgetWidth * floatDensity * 1.2) / 6);
                    } else {
                        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                        outWidth = (int) (displayMetrics.widthPixels * 1.2 / 7);
                    }

                    int inWidth = photo.getWidth();
                    int inHeight = photo.getHeight();
                    double resizeFactor = ContactsEvents.getSizeForWidgetElement(widgetPref, 2, 1, 1);
                    if (inHeight > 0 && inWidth > 0) {
                        int outHeight = inHeight * outWidth / inWidth;

                        if (outHeight > 0 && outWidth > 0) {
                            Bitmap photo_small = Bitmap.createScaledBitmap(photo, (int) (outWidth * resizeFactor), (int) (outHeight * resizeFactor), true);
                            views.setImageViewBitmap(R.id.eventPhoto, photo_small);
                            views.setViewVisibility(R.id.eventPhoto, View.VISIBLE);
                        }
                    }
                }
            }

            final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
            String eventText = singleEventArray[ContactsEvents.Position_eventEmoji] +
                    Constants.STRING_SPACE +
                    Constants.HTML_COLOR_START + colorDate + Constants.HTML_COLOR_MIDDLE + eventDay + Constants.HTML_COLOR_END +
                    Constants.STRING_SPACE +
                    singleEventArray[ContactsEvents.Position_eventCaption] +
                    Constants.STRING_COLON_SPACE +
                    eventsData.getFullName(singleEventArray);

            Intent clickIntent = new Intent();
            clickIntent.putExtra(Constants.EXTRA_CLICKED_EVENT, eventInfo);
            clickIntent.putExtra(Constants.EXTRA_CLICKED_TEXT, eventText);
            clickIntent.putExtra(Constants.EXTRA_CLICKED_PREFS, widgetPref_onClick);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            views.setOnClickFillInIntent(R.id.eventEntry, clickIntent);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return views;

    }

    private int getRoundingFactor() {
        int roundingFactor = 1;
        if (widgetPref != null && widgetPref.size() > 6) {
            switch (widgetPref.get(6)) {
                case Constants.STRING_1: roundingFactor = 2; break;
                case Constants.STRING_2: roundingFactor = 3; break;
                case Constants.STRING_3: roundingFactor = 4; break;
                case Constants.STRING_4: roundingFactor = 9; break;
            }
        }
        return roundingFactor;
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

    private void initData() {

        try {

            if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) return;

            Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetID);
            this.widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            this.floatDensity = displayMetrics.density;

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
            widgetPref_eventInfo = new ArrayList<>();
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
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
}
