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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    ContactsEvents eventsData;

    public EventPhotoListDataProvider(Context context, Intent intent) {
        this.context = context;
        this.resources = context.getResources();
        this.widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        //this.widgetWidth = intent.getIntExtra("intWidgetWidth", 0);
        //this.floatDensity = intent.getFloatExtra("floatScreenDensity", 1);
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
            double fontMagnify = 1.2;
            if (widgetPref != null && widgetPref.size() > 1 && !widgetPref.get(1).equals(Constants.STRING_0)) {
                switch (widgetPref.get(1)) {
                    case Constants.STRING_1:
                        fontMagnify = fontMagnify * 0.5;
                        break;
                    case Constants.STRING_2:
                        fontMagnify = fontMagnify * 0.65;
                        break;
                    case Constants.STRING_3:
                        fontMagnify = fontMagnify * 0.75;
                        break;
                    case Constants.STRING_4:
                        fontMagnify = fontMagnify * 0.85;
                        break;
                    case Constants.STRING_5:
                        fontMagnify = fontMagnify * 1;
                        break;
                    case Constants.STRING_6:
                        fontMagnify = fontMagnify * 1.2;
                        break;
                    case Constants.STRING_7:
                        fontMagnify = fontMagnify * 1.5;
                        break;
                    case Constants.STRING_8:
                        fontMagnify = fontMagnify * 1.75;
                        break;
                    case Constants.STRING_9:
                        fontMagnify = fontMagnify * 2.0;
                        break;
                }
            }
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_SMALL * fontMagnify));
            views.setTextViewTextSize(R.id.eventDetails, TypedValue.COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));

            views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);
            views.setTextColor(R.id.eventDetails, eventsData.preferences_widgets_color_default);

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);

            if (singleEventArray.length < ContactsEvents.Position_attrAmount) {

                views.setTextViewText(R.id.eventCaption, eventInfo);

            } else {

                switch (eventsData.preferences_list_nameformat) {
                    case 2: //Фамилия Имя Отчество
                        views.setTextViewText(R.id.eventCaption, singleEventArray[ContactsEvents.Position_personFullNameAlt]);
                        break;
                    case 1: //Имя Отчество Фамилия
                    default:
                        views.setTextViewText(R.id.eventCaption, singleEventArray[ContactsEvents.Position_personFullName]);
                        break;
                }


                //Цвет даты события
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

                StringBuilder sbDetails = new StringBuilder();

                //Организация и должность

                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_Organization_ID))) {
                    final String contactOrganization = ContactsEvents.checkForNull(singleEventArray[ContactsEvents.Position_organization]).trim();
                    if (!contactOrganization.isEmpty()) sbDetails.append(contactOrganization.trim());
                }
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_JobTitle_ID))) {
                    final String positionJobTitle = singleEventArray[ContactsEvents.Position_title];
                    if (!TextUtils.isEmpty(positionJobTitle)) {
                        if (sbDetails.length() > 0) sbDetails.append(Constants.STRING_COMMA_SPACE);
                        sbDetails.append(positionJobTitle.trim());
                    }
                }

                //Иконка
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID))
                        : widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID))) {
                    if (sbDetails.length() > 0) sbDetails.append(Constants.HTML_BR);
                    sbDetails.append(singleEventArray[ContactsEvents.Position_eventEmoji]).append(Constants.STRING_SPACE);
                }

                //Наименование события и возраст
                if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_EventCaption_ID))) {
                    sbDetails.append(singleEventArray[ContactsEvents.Position_eventCaption]);
                }
                if (!singleEventArray[ContactsEvents.Position_age_caption].trim().isEmpty() && widgetPref_eventInfo.isEmpty()
                        ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_Widgets_EventInfo_Age_ID))
                        : widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_Age_ID))) {
                    if (!singleEventArray[ContactsEvents.Position_age_caption].trim().isEmpty()) {
                        if (widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_EventCaption_ID)))
                            sbDetails.append(Constants.STRING_COLON_SPACE);
                        sbDetails.append(singleEventArray[ContactsEvents.Position_age_caption]);
                    }
                }

                //Знак зодиака и животное в восточном календаре
                String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];

                if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Constants.Type_5K))) {

                    String strZodiacInfo = Constants.STRING_EMPTY;
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_Widgets_EventInfo_ZodiacSign_ID))
                            : widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_ZodiacSign_ID))) {
                        strZodiacInfo = singleEventArray[ContactsEvents.Position_zodiacSign].trim();
                    }

                    String strZodiacYearInfo = Constants.STRING_EMPTY;
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID))
                            : widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID))) {
                        strZodiacYearInfo = singleEventArray[ContactsEvents.Position_zodiacYear].trim();
                    }

                    if (!strZodiacInfo.isEmpty() || !strZodiacYearInfo.isEmpty()) {
                        if (sbDetails.length() > 0) sbDetails.append(Constants.HTML_BR);
                        sbDetails.append((strZodiacInfo.concat(Constants.STRING_SPACE).concat(strZodiacYearInfo)).trim());
                    }
                }

                //Срок до события и день недели
                final String[] eventDistanceText = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                final boolean showDistance = widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_DaysBeforeEvent_ID));
                final boolean showDayOfWeek = widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_EventDayOfWeek_ID));
                final boolean showEventDate = widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_EventDate_ID));

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

                    sbDetails.append(colorDate != null ? String.format(Constants.HTML_COLOR, colorDate, textDistance).concat(Constants.HTML_FONT_END) : textDistance.toString());
                }

                views.setTextViewText(R.id.eventDetails, HtmlCompat.fromHtml(sbDetails.toString(), 0));

                //Фото
                views.setImageViewBitmap(R.id.eventPhoto, null);
                views.setViewVisibility(R.id.eventPhoto, View.GONE);
                Bitmap photo = null;
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(resources.getString(R.string.pref_Widgets_EventInfo_Photo_ID))
                        : widgetPref_eventInfo.contains(resources.getString(R.string.pref_Widgets_EventInfo_Photo_ID))) {
                    int roundingFactor = 1;
                    if (widgetPref != null && widgetPref.size() > 6) {
                        switch (widgetPref.get(6)) {
                            case Constants.STRING_1: roundingFactor = 2; break;
                            case Constants.STRING_2: roundingFactor = 3; break;
                            case Constants.STRING_3: roundingFactor = 4; break;
                            case Constants.STRING_4: roundingFactor = 9; break;
                        }
                    }

                    photo = eventsData.getContactPhoto(eventInfo, true, true, true, roundingFactor);
                }
                if (photo != null) {
                    int outWidth;
                    if (widgetWidth > 0) {
                        outWidth = (int) (widgetWidth * floatDensity / 6);
                    } else {
                        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                        outWidth = displayMetrics.widthPixels / 7;
                    }
                    outWidth *= fontMagnify;

                    int inWidth = photo.getWidth();
                    int inHeight = photo.getHeight();
                    if (inHeight > 0 && inWidth > 0) {
                        int outHeight = inHeight * outWidth / inWidth;

                        if (outHeight > 0 && outWidth > 0) {

                            Bitmap photo_small = Bitmap.createScaledBitmap(photo, outWidth, outHeight, true);
                            views.setImageViewBitmap(R.id.eventPhoto, photo_small);
                            views.setViewVisibility(R.id.eventPhoto, View.VISIBLE);
                        }
                    }
                }
            }

            Intent clickIntent = new Intent();
            clickIntent.putExtra(Constants.EXTRA_CLICKED_EVENT, eventInfo);
            clickIntent.putExtra(Constants.EXTRA_CLICKED_PREFS, eventsData.preferences_widgets_on_click_action);
            views.setOnClickFillInIntent(R.id.eventEntry, clickIntent);

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
                if (eventsData.getEvents(context)) eventsData.computeDates();
            }
            widgetPref_eventInfo = new ArrayList<>();
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
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
}
