/*
 * *
 *  * Created by Vladimir Belov on 07.01.2026, 01:04
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 01.01.2026, 22:47
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

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EventListDataProvider предоставляет данные для ListView, используемого в виджете приложения.
 * Он реализует интерфейс RemoteViewsService.RemoteViewsFactory для предоставления коллекции RemoteViews,
 * которые будут отображаться в ListView виджета.
 *
 * <p>Этот класс отвечает за:</p>
 * <ul>
 *     <li>Инициализацию данных на основе настроек виджета и получение данных о событиях.</li>
 *     <li>Фильтрацию и ограничение количества отображаемых событий на основе настроек пользователя.</li>
 *     <li>Создание и заполнение отдельных RemoteViews для каждого элемента в ListView.</li>
 *     <li>Обработку нажатий пользователя на элементы списка.</li>
 *     <li>Управление жизненным циклом поставщика данных.</li>
 * </ul>
 *
 * <p><b>Ключевые функции:</b></p>
 * <ul>
 *     <li><b>Инициализация данных (initData):</b> Извлекает настройки виджета, получает данные о событиях из {@link ContactsEvents}
 *     и фильтрует список событий на основе критериев, выбранных пользователем.</li>
 *     <li><b>Фильтрация и ограничение событий:</b> Применяет ограничения к количеству событий, дней и фактов, отображаемых на основе настроек виджета.</li>
 *     <li><b>Заполнение представления (getViewAt):</b> Создает и заполняет {@link RemoteViews} для каждого события, отображая детали события,
 *     форматируя даты и применяя пользовательские цвета в зависимости от близости события. Также конструирует намерения для обработки нажатий на каждый элемент списка.</li>
 * </ul>
 */
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
    int widgetWidth;
    float floatDensity;
    List<String> widgetPref;
    private List<String> widgetPref_eventInfo = new ArrayList<>();
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

            Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetID);
            this.widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            this.floatDensity = displayMetrics.density;

            eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(context);

            //Получаем данные
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetID);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            widgetPref = eventsData.getWidgetPreference(widgetID, widgetType);

            if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {
                eventsData.getEvents();
            }

            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                if (widgetPref.get(4).isEmpty() || widgetPref.get(4).equals(resources.getString(R.string.pref_EventInfo_None_ID))) {
                    widgetPref.set(4, resources.getString(R.string.widget_config_defaultPref_List).split(Constants.STRING_COMMA)[4]);
                }

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
                    String factDecorated = resources.getString(R.string.event_type_fact_emoji) + Constants.STRING_SPACE + fact;
                    eventListView.add(factDecorated);
                    eventsData.saveRecentFact(factDecorated);
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
                            long countDays = AppDateUtils.countDaysDiff(currentDay, eventDate);
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

            //Размер
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP,
                    ContactsEvents.getSizeForWidgetElement(widgetPref, 1, Constants.WIDGET_TEXT_SIZE_TINY, 1.6));

            if (eventListView.size() < position + 1) return views;

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);
            String eventText;

            views.setImageViewBitmap(R.id.eventPhoto, null);
            views.setViewVisibility(R.id.eventPhoto, View.GONE);

            if (singleEventArray.length < ContactsEvents.Position_attrAmount) {

                eventText = eventInfo;
                views.setTextViewText(R.id.eventCaption, eventText);

            } else {

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
                final AtomicBoolean colorizeEntireRow = new AtomicBoolean(false);
                StringBuilder eventDetails = getEventDetails(singleEventArray, eventInfo, views,
                        colorDate, eventDistance_Days, eventDistance, dateColorId, colorizeEntireRow);

                if (eventDetails.length() - eventDetails.lastIndexOf(Constants.HTML_BR) == Constants.HTML_BR.length()) {
                    eventDetails.setLength(eventDetails.lastIndexOf(Constants.HTML_BR));
                }

                if (colorizeEntireRow.get()) {
                    eventText = Constants.HTML_COLOR_START + colorDate + Constants.HTML_COLOR_MIDDLE + eventDetails + Constants.HTML_COLOR_END;

                } else {
                    eventText = eventDetails.toString();
                }

                views.setTextViewText(R.id.eventCaption, HtmlCompat.fromHtml(eventText, HtmlCompat.FROM_HTML_MODE_LEGACY));
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

    /** Возвращает детальную информацию о событии для виджета
     * @param singleEventArray Информация о событии массивом
     * @param eventInfo Информация о событии строкой
     * @param views RemoteViews события
     * @param colorDate Цвет дня
     * @param eventDistance_Days Дней до события
     * @param eventDistance Дней до события строкой
     * @param dateColorId Id цвета дня (1 - сегодня, 2 - скоро, 3 - подальше)
     * @param colorizeEntireRow Подсвечивать цветом события всю строку
     * @return Детали события
     */
    private StringBuilder getEventDetails(String[] singleEventArray, String eventInfo, RemoteViews views, String colorDate,
                                          int eventDistance_Days, String eventDistance, int dateColorId, AtomicBoolean colorizeEntireRow) {

        StringBuilder eventDetails = new StringBuilder();

        try {
            final String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];
            final String eventKey = eventsData.getEventKey(singleEventArray);
            final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
            final String[] eventDistanceInfo = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);

            for (String eventItem : widgetPref_eventInfo) {

                final boolean notEndWithBR = !eventDetails.toString().endsWith(Constants.HTML_BR);
                final boolean notEndWithBRorBracket = notEndWithBR && !eventDetails.toString().endsWith(Constants.STRING_PARENTHESIS_START);
                boolean isBirthdayEvent = eventSubType.equals(Constants.EventType_BirthDay) || eventSubType.equals(Constants.EventType_5K);

                if (eventItem.equals(resources.getString(R.string.pref_EventInfo_Photo_ID))) {

                    //Фото
                    int roundingFactor = getRoundingFactor();
                    Bitmap photo = eventsData.getEventPhoto(eventInfo, true, true, false, roundingFactor);
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

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventIcon_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    eventDetails.append(singleEventArray[ContactsEvents.Position_eventEmoji]).append(Constants.STRING_SPACE);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_FavIcon_ID))) {

                    if (eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred])) {
                        if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                        eventDetails.append(Constants.eventTitleFavoritePrefix);
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_ZodiacSign_ID))) {

                    if (isBirthdayEvent) {
                        if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                        final String zodiacSign = singleEventArray[ContactsEvents.Position_zodiacSign];
                        int indexSpace = zodiacSign.indexOf(Constants.STRING_SPACE);
                        if (indexSpace > -1) {
                            eventDetails.append(zodiacSign.substring(0, indexSpace)).append(Constants.STRING_SPACE);
                        } else {
                            eventDetails.append(zodiacSign).append(Constants.STRING_SPACE);
                        }
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_ZodiacYear_ID))) {

                    if (isBirthdayEvent) {
                        if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                        final String zodiacYear = singleEventArray[ContactsEvents.Position_zodiacYear];
                        int indexSpace = zodiacYear.indexOf(Constants.STRING_SPACE);
                        if (indexSpace > -1) {
                            eventDetails.append(zodiacYear.substring(0, indexSpace)).append(Constants.STRING_SPACE);
                        } else {
                            eventDetails.append(zodiacYear).append(Constants.STRING_SPACE);
                        }
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventCaption_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    if (StringUtils.hasContent(singleEventArray[ContactsEvents.Position_eventCaption])) {
                        eventDetails.append(singleEventArray[ContactsEvents.Position_eventCaption]);
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_Original_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithoutYear);
                    eventDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE).append(eventDay).append(Constants.HTML_COLOR_END);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_Original_WithYear_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
                    eventDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE).append(eventDay).append(Constants.HTML_COLOR_END);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateNextTime], ContactsEvents.FormatDate.WithoutYear);
                    eventDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE).append(eventDay).append(Constants.HTML_COLOR_END);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDate_WithYear_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    final String eventDay = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateNextTime], ContactsEvents.FormatDate.WithYear);
                    eventDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE).append(eventDay).append(Constants.HTML_COLOR_END);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_DaysBeforeEventShort_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    if (eventDistance_Days < 2) {
                        eventDetails.append(eventDistanceInfo[0]);
                    } else {
                        eventDetails.append(eventDistance).append(eventsData.getResources().getString(R.string.msg_after_day_prefix_short));
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventTitle_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    eventDetails.append(eventsData.getFullName(singleEventArray));

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_Age_ID))) {

                    if (StringUtils.hasContent(singleEventArray[ContactsEvents.Position_age_caption])) {
                        if (notEndWithBRorBracket)
                            eventDetails.append(Constants.STRING_COLON_SPACE);
                        eventDetails.append(singleEventArray[ContactsEvents.Position_age_caption]);
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_AgeShort_ID))) {

                    String age = singleEventArray[ContactsEvents.Position_age].trim();
                    if (!age.isEmpty() && !Constants.STRING_MINUS1.equals(age)) {
                        if (notEndWithBRorBracket)
                            eventDetails.append(Constants.STRING_COLON_SPACE);
                        eventDetails.append(singleEventArray[ContactsEvents.Position_age]);
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_WeddingName_ID))) {

                    if (eventSubType.equals(Constants.EventType_Anniversary)) {
                        int ind1 = singleEventArray[ContactsEvents.Position_eventCaption].indexOf(Constants.STRING_PARENTHESIS_OPEN);
                        if (ind1 > -1) {
                            if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                            eventDetails.append(singleEventArray[ContactsEvents.Position_eventCaption].substring(ind1));
                        }
                    }

                } else if (dateColorId > 2 && eventItem.equals(resources.getString(R.string.pref_EventInfo_DaysBeforeEventFar_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    eventDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE).append(eventDistanceInfo[0]).append(Constants.HTML_COLOR_END);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_DaysBeforeEvent_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    eventDetails.append(Constants.HTML_COLOR_START).append(colorDate).append(Constants.HTML_COLOR_MIDDLE).append(eventDistanceInfo[0]).append(Constants.HTML_COLOR_END);

                } else if (dateColorId > 2 && eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDayOfWeekFar_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    if (eventDistanceInfo.length >= 1) eventDetails.append(eventDistanceInfo[1]);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDayOfWeek_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    if (eventDistanceInfo.length >= 1) eventDetails.append(eventDistanceInfo[1]);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_EventDayOfWeekShort_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    if (eventDistanceInfo.length >= 3) eventDetails.append(eventDistanceInfo[3]);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_SourceIcon_ID))) {

                    String[] dates = singleEventArray[ContactsEvents.Position_dates].split(Constants.STRING_2TILDA, -1);
                    if (dates.length > 0) {
                        if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                        eventDetails.append(eventsData.getEventSourceIcon(singleEventArray));
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_LinkIcon_ID))) {

                    if (StringUtils.hasContent(singleEventArray[ContactsEvents.Position_eventURL])) {
                        if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                        eventDetails.append("🔗");
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_NewLine1_ID)) || eventItem.equals(resources.getString(R.string.pref_EventInfo_NewLine2_ID)) || eventItem.equals(resources.getString(R.string.pref_EventInfo_NewLine3_ID))) {

                    eventDetails.append(Constants.HTML_BR);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_CurrentAge_ID)) && !singleEventArray[ContactsEvents.Position_eventDistance].equals(Constants.STRING_0)) {

                    final String currentAge = singleEventArray[ContactsEvents.Position_age_current];
                    if (!TextUtils.isEmpty(currentAge)) {
                        if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                        int ind = currentAge.indexOf(Constants.STRING_PARENTHESIS_OPEN);
                        eventDetails.append(ind != -1 ? currentAge.substring(0, ind) : currentAge);
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_LeftBracket_ID)) || eventItem.equals(resources.getString(R.string.pref_EventInfo_LeftBracket2_ID))) {

                    if (notEndWithBR) eventDetails.append(Constants.STRING_SPACE);
                    eventDetails.append(Constants.STRING_PARENTHESIS_START);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_RightBracket_ID)) || eventItem.equals(resources.getString(R.string.pref_EventInfo_RightBracket2_ID))) {

                    if (eventDetails.toString().endsWith(Constants.STRING_PARENTHESIS_START)) {
                        eventDetails.setLength(eventDetails.length() - Constants.STRING_PARENTHESIS_START.length());
                    } else {
                        eventDetails.append(Constants.STRING_PARENTHESIS_CLOSE);
                    }

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_ColorizeEntireRow_ID))) {

                    colorizeEntireRow.set(true);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_BoldStart_ID))) {

                    eventDetails.append(Constants.HTML_BOLD_START);

                } else if (eventItem.equals(resources.getString(R.string.pref_EventInfo_BoldEnd_ID))) {

                    eventDetails.append(Constants.HTML_BOLD_END);

                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        return eventDetails;
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

}
