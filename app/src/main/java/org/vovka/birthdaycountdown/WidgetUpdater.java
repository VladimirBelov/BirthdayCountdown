/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 30.11.2023, 23:23
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

class WidgetUpdater {

    private static final String TAG = "WidgetUpdater";
    final private Context context;
    final private ContactsEvents eventsData;
    final private RemoteViews views;
    final private int eventsCount;
    final private int width;
    final private int height;
    private final int widgetId;
    private Resources resources;
    private String packageName;
    private double fontMagnify;
    private int colorDefault;
    private int colorEventToday;
    private int colorEventSoon;
    private int colorEventFar;
    private int eventsDisplayed;
    private int eventsToShow;
    private int eventsToSkip;
    private List<String> widgetPref;
    private List<String> widgetPref_eventInfo = new ArrayList<>();
    private List<String> eventsPrefList = new ArrayList<>();
    private List<String> sourcesPrefList = new ArrayList<>();

    final int PendingIntentImmutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;

    WidgetUpdater(@NonNull Context context, @NonNull ContactsEvents eventsData, @NonNull RemoteViews views, int eventsCount, int width, int height, int widgetId) {
        this.context = context;
        this.eventsData = eventsData;
        this.views = views;
        this.eventsCount = eventsCount > Constants.WIDGET_EVENTS_MAX ? Constants.WIDGET_EVENTS_MAX : eventsCount > 0 ? eventsCount : 1;
        this.width = width;
        this.height = height;
        this.widgetId = widgetId;
    }

    @SuppressLint("DiscouragedApi")
    void invokePhotoEventsUpdate() {
        //По нажатию на виджет открываем основное окно
        //http://flowovertop.blogspot.com/2013/04/android-widget-with-button-click-to.html
        Intent intentView = new Intent(context, MainActivity.class);
        intentView.setAction(Constants.ACTION_LAUNCH);
        views.setOnClickPendingIntent(R.id.appwidget_main, PendingIntent.getActivity(context, 0, intentView, PendingIntentImmutable | PendingIntent.FLAG_UPDATE_CURRENT));

        if (eventsData.isEmptyEventList() || System.currentTimeMillis() - eventsData.statLastComputeDates > Constants.TIME_FORCE_UPDATE + eventsData.statTimeComputeDates) {
            if (eventsData.getContext() == null) eventsData.setContext(context);
            eventsData.getPreferences();
            eventsData.setLocale(true);
            if (eventsData.getEvents(context)) eventsData.computeDates();
        }

        try {
            //Скрываем все события
            resources = context.getResources();
            packageName = context.getPackageName();
            for (int e = 0; e < this.eventsCount; e++) {
                views.setViewVisibility(resources.getIdentifier(Constants.WIDGET_EVENT_INFO + e, Constants.STRING_ID, packageName), View.INVISIBLE);
            }

            //Получаем настройки отображения виджета
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId);
            if (appWidgetInfo == null) return;
            String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);

            int startingIndex = 1;
            try {
                if (!widgetPref.isEmpty()) startingIndex = Integer.parseInt(widgetPref.get(0));
            } catch (Exception e) {/**/}

            if (eventsData.isEmptyEventList() || eventsData.eventList.size() < startingIndex) {

                String prefZeroEventsMessage = null;
                if (widgetPref.size() > 7) prefZeroEventsMessage = widgetPref.get(7).replaceAll(Constants.STRING_EOT, Constants.STRING_COMMA);
                if (TextUtils.isEmpty(prefZeroEventsMessage)) prefZeroEventsMessage = context.getString(R.string.msg_no_events);
                views.setTextViewText(R.id.appwidget_text, prefZeroEventsMessage);
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);
                return;

            }

            if (widgetPref.size() > 3 && !widgetPref.get(3).isEmpty()) {
                eventsPrefList = Arrays.asList(widgetPref.get(3).split(Constants.REGEX_PLUS));
            }

            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
                widgetPref_eventInfo = Arrays.asList(widgetPref.get(4).split(Constants.REGEX_PLUS));
            }

            if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                sourcesPrefList = Arrays.asList(widgetPref.get(10).split(Constants.REGEX_PLUS));
            }

            eventsToShow = Math.min(this.eventsCount, eventsData.eventList.size());

            //Увеличение шрифтов в зависимости от размеров окна
            fontMagnify = 1;
            int cells = getCellsForSize(Math.min(width, height));
            if (widgetPref.size() > 1 && !widgetPref.get(1).equals(Constants.STRING_0)) {
                switch (widgetPref.get(1)) {
                    case Constants.STRING_1:
                        fontMagnify = cells * 0.5;
                        break;
                    case Constants.STRING_2:
                        fontMagnify = cells * 0.65;
                        break;
                    case Constants.STRING_3:
                        fontMagnify = cells * 0.75;
                        break;
                    case Constants.STRING_4:
                        fontMagnify = cells * 0.85;
                        break;
                    case Constants.STRING_5:
                        fontMagnify = cells * 1.0;
                        break;
                    case Constants.STRING_6:
                        fontMagnify = cells * 1.2;
                        break;
                    case Constants.STRING_7:
                        fontMagnify = cells * 1.5;
                        break;
                    case Constants.STRING_8:
                        fontMagnify = cells * 1.75;
                        break;
                    case Constants.STRING_9:
                        fontMagnify = cells * 2.0;
                        break;
                }
            } else {
                fontMagnify = 1 + 1.0 * (cells - 1);
            }

            colorDefault = eventsData.preferences_widgets_color_default;
            colorEventToday = eventsData.preferences_widgets_color_event_today;
            colorEventSoon = eventsData.preferences_widgets_color_event_soon;
            colorEventFar = eventsData.preferences_widgets_color_event_far;

            //Отрисовываем информацию о событиях
            eventsDisplayed = 0;
            eventsToSkip = startingIndex - 1;

            int i = 0;
            while (i < eventsData.eventList.size() & eventsDisplayed <= eventsToShow) {
                drawPhotoEvent(i);
                i++;
            }

            if (eventsDisplayed == 0) { //вообще ничего не нашли

                String prefZeroEventsMessage = null;
                if (widgetPref.size() > 7) prefZeroEventsMessage = widgetPref.get(7).replaceAll(Constants.STRING_EOT, Constants.STRING_COMMA);
                if (TextUtils.isEmpty(prefZeroEventsMessage)) prefZeroEventsMessage = context.getString(R.string.msg_no_events);
                views.setTextViewText(R.id.appwidget_text, prefZeroEventsMessage);
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

                Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
                intentConfig.setAction(Constants.ACTION_LAUNCH);
                intentConfig.putExtra(Constants.PARAM_APP_WIDGET_ID, widgetId);
                views.setOnClickPendingIntent(R.id.appwidget_text, PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));

            } else {

                views.setViewVisibility(R.id.appwidget_text, View.GONE);

            }

            //Цвет подложки
            int colorWidgetBackground = 0;
            if (widgetPref.size() > 5 && !widgetPref.get(5).isEmpty()) {
                try {
                    colorWidgetBackground = Color.parseColor(widgetPref.get(5));
                } catch (Exception e) { /* */}
            }
            if (colorWidgetBackground == 0) {
                colorWidgetBackground = ContextCompat.getColor(context, R.color.pref_Widgets_Color_WidgetBackground_default);
            }
            views.setInt(R.id.events,"setBackgroundColor", colorWidgetBackground);

            //https://stackoverflow.com/questions/12523005/how-set-background-drawable-programmatically-in-android
            //Если события есть - рисуем бордюр, иначе - прозрачность
            if (eventsToShow > 0 && (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_Border_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_Border_ID)))) {
                views.setInt(R.id.appwidget_main,"setBackgroundResource", R.drawable.layout_bg);
            } else {
                views.setInt(R.id.appwidget_main,"setBackgroundResource", 0);
            }

            if (eventsData.preferences_debug_on) {
                views.setTextViewText(R.id.info, (width > 70 ? context.getString(R.string.widget_msg_updated) : Constants.STRING_EMPTY) + new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY_HH_MM, resources.getConfiguration().locale).format(new Date(Calendar.getInstance().getTimeInMillis())));
                views.setViewVisibility(R.id.info, View.VISIBLE);
            } else {
                views.setTextViewText(R.id.info, Constants.STRING_EMPTY);
                views.setViewVisibility(R.id.info, View.INVISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @SuppressLint("DiscouragedApi")
    private void drawPhotoEvent(int i) {
        //Отрисовка одного события
        try {

            String event = eventsData.eventList.get(i);
            String[] singleEventArray = event.split(Constants.STRING_EOT, -1);

            boolean isVisibleEvent = false;
            boolean useEventListPrefs = true;

            final String eventSubType = singleEventArray[ContactsEvents.Position_eventSubType];
            final String eventKey = eventsData.getEventKey(singleEventArray);
            final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);

            //Типы событий
            if  (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_CalendarEvent)) ||
                    eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent))) { //пропускаем события календарей и из файлов
                useEventListPrefs = false;
            } else if (!eventsPrefList.isEmpty()) {
                useEventListPrefs = false;
                isVisibleEvent = eventsPrefList.contains(singleEventArray[ContactsEvents.Position_eventType]) &&
                        (eventsData.getHiddenEventsCount() == 0 || !eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId));
            }
            if (useEventListPrefs) isVisibleEvent = eventsData.preferences_list_event_types.contains(singleEventArray[ContactsEvents.Position_eventType]) &&
                    (eventsData.getHiddenEventsCount() == 0 || !eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId));

            if (isVisibleEvent && !sourcesPrefList.isEmpty()) {
                final String eventDates = singleEventArray[ContactsEvents.Position_dates];
                isVisibleEvent = false;
                for (String source: sourcesPrefList) {
                    if (eventDates.contains(source)) {
                        isVisibleEvent = true;
                        break;
                    }
                }
            }

            if (!isVisibleEvent) {
                return;
            } else if (eventsToSkip > 0) {
                eventsToSkip--;
                return;
            }

            Person person = new Person(context, event);
            //if (eventsData.preferences_debug_on) Toast.makeText(context, eventsDisplayed + " " + event, Toast.LENGTH_LONG).show();
            int visibleCell = 1; //2 - top left, 3 - top center, 5 - bottom left, 7 - bottom center
            String rowValue;

            //Под фото
            int id_widget_Caption_left = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + eventsDisplayed, Constants.STRING_ID, packageName);
            int id_widget_Caption_centered = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_CENTERED + eventsDisplayed, Constants.STRING_ID, packageName);

            views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
            views.setViewVisibility(id_widget_Caption_centered, View.INVISIBLE);

            views.setTextViewTextSize(id_widget_Caption_left, COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));
            views.setTextViewTextSize(id_widget_Caption_centered, COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));

            //todo: оптимизировать количество элементов на разметке
            //RemoteViews TextView не понимает gravity, но LinearLayout - понимает
            //https://stackoverflow.com/questions/7825508/remoteviews-textview-setgravity

            int indSpace;
            switch (eventsData.preferences_widgets_bottom_info) {
                case Constants.STRING_1: //Фамилия Имя Отчество
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_left, rowValue);
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        visibleCell *= 2;
                    }
                    break;
                case Constants.STRING_2: //Дата события
                    rowValue = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell *= 3;
                    }
                    break;
                case Constants.STRING_3: //Фамилия И.О. (Имя Отчество, если нет фамилии)
                    rowValue = person.getFullNameShort();
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell *= 3;
                    }
                    break;
                case Constants.STRING_4: //Имя Отчество Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_left, rowValue);
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        visibleCell *= 2;
                    }
                    break;
                case Constants.STRING_5: //Имя
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell *= 3;
                    }
                    break;
                case Constants.STRING_6: //Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }

                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell *= 3;
                    }
                    break;
                case Constants.STRING_7: //Псевдоним (Имя, если отсутствует)
                    if (!singleEventArray[ContactsEvents.Position_nickname].trim().isEmpty()) {
                        rowValue = singleEventArray[ContactsEvents.Position_nickname];
                    } else {
                        rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                        indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                        if (indSpace > -1) {
                            rowValue = rowValue.substring(0, indSpace);
                        }
                    }
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell *= 3;
                    }
                    break;
                case Constants.STRING_8: //Тип события
                    rowValue = singleEventArray[ContactsEvents.Position_eventCaption];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_left, rowValue);
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        visibleCell *= 2;
                    }
                    break;
                case Constants.STRING_9: //Наименование события
                    rowValue =
                        singleEventArray[ContactsEvents.Position_eventLabel].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_eventCaption] :
                        singleEventArray[ContactsEvents.Position_eventLabel];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_left, rowValue);
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        visibleCell *= 2;
                    }
                    break;
                case Constants.STRING_10: //Организация (Должность, если отсутствует)
                    rowValue = !singleEventArray[ContactsEvents.Position_organization].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_organization] : singleEventArray[ContactsEvents.Position_title];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell *= 3;
                    }
                    break;
            }

            //Под фото (верхний ряд)
            int id_widget_Caption2nd_left = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND + eventsDisplayed, Constants.STRING_ID, packageName);
            int id_widget_Caption2nd_centered = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND_CENTERED + eventsDisplayed, Constants.STRING_ID, packageName);

            views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
            views.setViewVisibility(id_widget_Caption2nd_centered, View.INVISIBLE);

            views.setTextViewTextSize(id_widget_Caption2nd_left, COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));
            views.setTextViewTextSize(id_widget_Caption2nd_centered, COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));

            switch (eventsData.preferences_widgets_bottom_info_2nd) {
                case Constants.STRING_1: //Фамилия Имя Отчество
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_left, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        visibleCell *= 5;
                    }
                    break;
                case Constants.STRING_2: //Дата события
                    rowValue = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell *= 7;
                    }
                    break;
                case Constants.STRING_3: //Фамилия И.О. (Имя Отчество, если нет фамилии)
                    rowValue = person.getFullNameShort();
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                    }
                    break;
                case Constants.STRING_4: //Имя Отчество Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_left, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        visibleCell *= 5;
                    }
                    break;
                case Constants.STRING_5: //Имя
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell *= 7;
                    }
                    break;
                case Constants.STRING_6: //Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell *= 7;
                    }
                    break;
                case Constants.STRING_7: //Псевдоним (Имя, если отсутствует)
                    if (!singleEventArray[ContactsEvents.Position_nickname].trim().isEmpty()) {
                        rowValue = singleEventArray[ContactsEvents.Position_nickname];
                    } else {
                        rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                        indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                        if (indSpace > -1) {
                            rowValue = rowValue.substring(0, indSpace);
                        }
                    }
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell *= 7;
                    }
                    break;
                case Constants.STRING_8: //Тип события
                    rowValue = singleEventArray[ContactsEvents.Position_eventCaption];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_left, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        visibleCell *= 5;
                    }
                    break;
                case Constants.STRING_9: //Наименование события
                    rowValue =
                            singleEventArray[ContactsEvents.Position_eventLabel].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_eventCaption] :
                            singleEventArray[ContactsEvents.Position_eventLabel];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_left, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        visibleCell*=5;
                    }
                    break;
                case Constants.STRING_10: //Организация (Должность, если отсутствует)
                    rowValue = !singleEventArray[ContactsEvents.Position_organization].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_organization] : singleEventArray[ContactsEvents.Position_title];
                    if (!rowValue.trim().isEmpty()) {
                        views.setTextViewText(id_widget_Caption2nd_centered, rowValue);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell *= 7;
                    }
                    break;
            }

            //Фото

            int roundingFactor = 0;
            if (widgetPref != null && widgetPref.size() > 6) {
                switch (widgetPref.get(6)) {
                    case Constants.STRING_1: roundingFactor = 2; break;
                    case Constants.STRING_2: roundingFactor = 3; break;
                    case Constants.STRING_3: roundingFactor = 4; break;
                    case Constants.STRING_4: roundingFactor = 9; break;
                }
            }

            Bitmap photo = eventsData.getEventPhoto(event, widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_Photo_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_Photo_ID)), true, true, roundingFactor);
            if (photo != null) {

                //https://stackoverflow.com/questions/2459916/how-to-make-an-imageview-with-rounded-corners
                //https://stackoverflow.com/questions/7895118/android-remoteviews-how-to-set-scaletype-of-an-imageview-inside-a-widget
                int id_widget_Photo = resources.getIdentifier(Constants.WIDGET_IMAGE_VIEW + eventsDisplayed, Constants.STRING_ID, packageName);
                int id_widget_Photo_Centered = resources.getIdentifier(Constants.WIDGET_IMAGE_VIEW_CENTERED + eventsDisplayed, Constants.STRING_ID, packageName);
                int id_widget_Photo_Start = resources.getIdentifier(Constants.WIDGET_IMAGE_VIEW_START + eventsDisplayed, Constants.STRING_ID, packageName);
                int id_Photo;
                if (roundingFactor < 1) {
                    views.setViewVisibility(id_widget_Photo, View.VISIBLE);
                    views.setViewVisibility(id_widget_Photo_Centered, View.GONE);
                    views.setViewVisibility(id_widget_Photo_Start, View.GONE);
                    id_Photo = id_widget_Photo;
                } else if (roundingFactor > 8) {
                    views.setViewVisibility(id_widget_Photo, View.GONE);
                    views.setViewVisibility(id_widget_Photo_Centered, View.VISIBLE);
                    views.setViewVisibility(id_widget_Photo_Start, View.GONE);
                    id_Photo = id_widget_Photo_Centered;
                } else {
                    views.setViewVisibility(id_widget_Photo, View.GONE);
                    views.setViewVisibility(id_widget_Photo_Centered, View.GONE);
                    views.setViewVisibility(id_widget_Photo_Start, View.VISIBLE);
                    id_Photo = id_widget_Photo_Start;
                }

                //необходимо уменьшать, потому что вот: https://stackoverflow.com/questions/13494898/remoteviews-for-widget-update-exceeds-max-bitmap-memory-usage-error
                final int dstWidth = eventsToShow > 1 ? (4 * width / eventsToShow) : (2 * width);
                final int dstHeight = eventsToShow > 1 ? (4 * photo.getHeight() * width) / (photo.getWidth() * eventsToShow) : (2 * photo.getHeight() * width / photo.getWidth());
                if (dstHeight > 0 && dstWidth > 0) {
                    Bitmap photo_small = Bitmap.createScaledBitmap(photo, dstWidth, dstHeight, true);
                    views.setImageViewBitmap(id_Photo, photo_small);
                } else {
                    Bitmap photo_icon = eventsData.getEventPhoto(event, false, true, true, roundingFactor);
                    views.setImageViewBitmap(id_Photo, photo_icon);

                }
                //photo.recycle(); //https://stackoverflow.com/questions/38784302/cant-parcel-a-recycled-bitmap

            }

            //Иконка события
            int id_widget_EventIcon = resources.getIdentifier(Constants.WIDGET_ICON_EVENT_TYPE + eventsDisplayed, Constants.STRING_ID, packageName);

            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_EventIcon_ID))) {

                int eventIcon;
                try {
                    eventIcon = Integer.parseInt(singleEventArray[ContactsEvents.Position_eventIcon]);
                } catch (NumberFormatException e) {
                    eventIcon = 0;
                }
                if (eventIcon != 0) {
                    views.setImageViewResource(id_widget_EventIcon, eventIcon);
                } else {
                    views.setImageViewResource(id_widget_EventIcon, android.R.color.transparent);
                }

                views.setViewVisibility(id_widget_EventIcon, View.VISIBLE);

            } else {

                views.setViewVisibility(id_widget_EventIcon, View.GONE);

            }

            //Иконка знака зодиака
            //https://emojipedia.org/microsoft/windows-10-may-2019-update/aquarius/
            String strZodiacInfo = Constants.STRING_EMPTY;
            int id_widget_ZodiacIcon = resources.getIdentifier(Constants.WIDGET_ICON_ZODIAC + eventsDisplayed, Constants.STRING_ID, packageName);

            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_ZodiacSign_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_ZodiacSign_ID))) {

                if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {

                    strZodiacInfo = eventsData.getZodiacInfo(ContactsEvents.ZodiacInfo.SIGN, singleEventArray[ContactsEvents.Position_eventDateFirstTime]); //нам нужна только иконка

                } else if (eventsData.birthdayDatesForIds.containsKey(singleEventArray[ContactsEvents.Position_contactID])) {

                    Date birthDate = eventsData.birthdayDatesForIds.get(singleEventArray[ContactsEvents.Position_contactID]);
                    if (birthDate != null) {
                        Locale locale_en = new Locale(Constants.LANG_EN);
                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                        strZodiacInfo = eventsData.getZodiacInfo(ContactsEvents.ZodiacInfo.SIGN, sdfYear.format(birthDate));
                    }
                }
            }

            if (!strZodiacInfo.isEmpty()) {

                views.setTextViewText(id_widget_ZodiacIcon, strZodiacInfo);
                views.setViewVisibility(id_widget_ZodiacIcon, View.VISIBLE);

            } else{

                views.setViewVisibility(id_widget_ZodiacIcon, View.GONE);

            }

            //Иконка зодиакального года
            String strZodiacYearInfo = Constants.STRING_EMPTY;
            int id_widget_ZodiacYearIcon = resources.getIdentifier(Constants.WIDGET_ICON_ZODIAC_YEAR + eventsDisplayed, Constants.STRING_ID, packageName);

            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_ZodiacYear_ID))) {

                if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_BirthDay)) || eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {

                    strZodiacYearInfo = eventsData.getZodiacInfo(ContactsEvents.ZodiacInfo.YEAR, singleEventArray[ContactsEvents.Position_eventDateFirstTime]); //нам нужна только иконка

                } else if (eventsData.birthdayDatesForIds.containsKey(singleEventArray[ContactsEvents.Position_contactID])) {

                    Date birthDate = eventsData.birthdayDatesForIds.get(singleEventArray[ContactsEvents.Position_contactID]);
                    if (birthDate != null) {
                        Locale locale_en = new Locale(Constants.LANG_EN);
                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATE_DD_MM_YYYY, locale_en);
                        strZodiacYearInfo = eventsData.getZodiacInfo(ContactsEvents.ZodiacInfo.YEAR, sdfYear.format(birthDate));
                    }
                }
            }

            if (!strZodiacYearInfo.isEmpty()) {

                views.setTextViewText(id_widget_ZodiacYearIcon, strZodiacYearInfo);
                views.setViewVisibility(id_widget_ZodiacYearIcon, View.VISIBLE);

            } else{

                views.setViewVisibility(id_widget_ZodiacYearIcon, View.GONE);

            }

            //Иконка фаворита
            int id_widget_FavIcon = resources.getIdentifier(Constants.WIDGET_ICON_FAV + eventsDisplayed, Constants.STRING_ID, packageName);
            if ((widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_FavIcon_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_FavIcon_ID))) && eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred])) {

                views.setViewVisibility(id_widget_FavIcon, View.VISIBLE);

            } else {

                views.setViewVisibility(id_widget_FavIcon, View.GONE);

            }

            //Иконка события без уведомления
            int id_widget_SilencedIcon = resources.getIdentifier(Constants.WIDGET_ICON_SILENCED + eventsDisplayed, Constants.STRING_ID, packageName);
            if ((widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_SilentedIcon_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_SilentedIcon_ID))) && eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {

                views.setTextViewText(id_widget_SilencedIcon, "\uD83D\uDEAB"); //https://emojipedia.org/prohibited/
                views.setViewVisibility(id_widget_SilencedIcon, View.VISIBLE);

            } else {

                views.setViewVisibility(id_widget_SilencedIcon, View.GONE);

            }

            //Цвета по-умолчанию
            int id_widget_Age = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_AGE + eventsDisplayed, Constants.STRING_ID, packageName);

            views.setTextColor(id_widget_Age, colorDefault);
            views.setTextColor(id_widget_Caption_left, colorDefault);
            views.setTextColor(id_widget_Caption_centered, colorDefault);
            views.setTextColor(id_widget_Caption2nd_left, colorDefault);
            views.setTextColor(id_widget_Caption2nd_centered, colorDefault);

            //todo: сделать цвет тени зависимым от цвета текста
            //https://stackoverflow.com/questions/44417666/change-properties-of-view-inside-remoteview
            //https://stackoverflow.com/questions/6435648/any-way-to-set-the-text-shadow-for-a-spannablestring - не работает
            // @android.view.RemotableViewMethod
            //views.setInt(id_widget_Caption2nd_centered, "setShadowColor", resources.getColor(R.color.white));

            //Сколько осталось до события
            int id_widget_Distance = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_DISTANCE + eventsDisplayed, Constants.STRING_ID, packageName);
            String eventDistance = singleEventArray[ContactsEvents.Position_eventDistance];
            int eventDistance_Days;
            try {
                eventDistance_Days = Integer.parseInt(eventDistance);
            } catch (Exception e) {
                eventDistance_Days = 365;
            }

            if (eventDistance_Days == 0) { //Сегодня

                if (colorEventToday != 0) {
                    if (person.Age > -1) {
                        views.setTextColor(id_widget_Age, colorEventToday);
                    } else {
                        //Если возраста нет и событие уже сегодня - ставим цвет для ФИО
                        views.setTextColor(id_widget_Caption_left, colorEventToday);
                        views.setTextColor(id_widget_Caption_centered, colorEventToday);
                        views.setTextColor(id_widget_Caption2nd_left, colorEventToday);
                        views.setTextColor(id_widget_Caption2nd_centered, colorEventToday);
                    }
                }
                views.setTextViewText(id_widget_Distance, Constants.STRING_EMPTY);

            } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_event_soon) { //Скоро

                views.setTextColor(id_widget_Distance, colorEventSoon);
                views.setTextViewText(id_widget_Distance, eventDistance);
                views.setTextViewTextSize(id_widget_Distance, COMPLEX_UNIT_SP, (float) (Constants.WIDGET_TEXT_SIZE_BIG * fontMagnify));

            } else { //Попозже

                views.setTextColor(id_widget_Distance, eventsData.preferences_widgets_days_event_soon != 0 ? colorEventFar : colorDefault);
                views.setTextViewText(id_widget_Distance, eventDistance);
                views.setTextViewTextSize(id_widget_Distance, COMPLEX_UNIT_SP, (float) ((Integer.parseInt(eventDistance) < Constants.WIDGET_TEXT_SIZE_TINY ? Constants.WIDGET_TEXT_SIZE_BIG : Constants.WIDGET_TEXT_SIZE_SMALL) * fontMagnify));

            }

            //Возраст
            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_Widgets_EventInfo_Age_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_Widgets_EventInfo_Age_ID))) {

                String ageCaption = singleEventArray[ContactsEvents.Position_age_caption];
                if (ageCaption.contains(Constants.STRING_SPACE)) {
                    views.setTextViewText(id_widget_Age, ageCaption.substring(0, ageCaption.lastIndexOf(Constants.STRING_SPACE)));
                } else {
                    views.setTextViewText(id_widget_Age, ageCaption);
                }

                /*if (eventSubType.equals(ContactsEvents.getEventType(Constants.Type_5K))) {
                    views.setTextViewText(id_widget_Age, singleEventArray[ContactsEvents.Position_age_caption]);
                } else if (person.Age > -1) {
                    views.setTextViewText(id_widget_Age, Integer.toString(person.Age));
                } else {
                    views.setTextViewText(id_widget_Age, Constants.STRING_EMPTY);
                }*/
                //views.setTextColor(resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + eventsDisplayed, Constants.STRING_ID, packageName), colorDefault);
                views.setTextViewTextSize(id_widget_Age, COMPLEX_UNIT_SP, (float) ((eventDistance_Days == 0 ? Constants.WIDGET_TEXT_SIZE_BIG : Constants.WIDGET_TEXT_SIZE_SMALL) * fontMagnify));
                views.setViewVisibility(id_widget_Age, View.VISIBLE);

            } else {

                views.setViewVisibility(id_widget_Age, View.GONE);

            }

            //Если не последнее событие - по нажатию на фото открываем событие
            if (eventsToShow > 1 && eventsDisplayed < (eventsToShow - 1)) {

                Intent intent = null;

                if (eventsData.preferences_widgets_on_click_action == 7) { //Основной список событий
                    intent = new Intent(context, MainActivity.class);
                    intent.setAction(Constants.ACTION_LAUNCH);
                } else if (eventsData.preferences_widgets_on_click_action >= 1 & eventsData.preferences_widgets_on_click_action <=4) {
                    intent = ContactsEvents.getViewActionIntent(singleEventArray, eventsData.preferences_widgets_on_click_action);
                }

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    views.setOnClickPendingIntent(resources.getIdentifier(Constants.WIDGET_EVENT_INFO + eventsDisplayed, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, 0, intent, PendingIntentImmutable));
                } else { //Ничего не показываем
                    views.setOnClickPendingIntent(resources.getIdentifier(Constants.WIDGET_EVENT_INFO + eventsDisplayed, Constants.STRING_ID, packageName), null);
                }

            } else {

                Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
                intentConfig.setAction(Constants.ACTION_LAUNCH);
                intentConfig.putExtra(Constants.PARAM_APP_WIDGET_ID, widgetId);
                if (visibleCell % 2 == 0) views.setOnClickPendingIntent(resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + eventsDisplayed, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));
                if (visibleCell % 3 == 0) views.setOnClickPendingIntent(resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_CENTERED + eventsDisplayed, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));
                if (visibleCell % 5 == 0) views.setOnClickPendingIntent(resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND + eventsDisplayed, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));
                if (visibleCell % 7 == 0) views.setOnClickPendingIntent(resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND_CENTERED + eventsDisplayed, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));

            }

            //Показываем событие
            views.setViewVisibility(resources.getIdentifier(Constants.WIDGET_EVENT_INFO + eventsDisplayed, Constants.STRING_ID, packageName), View.VISIBLE);
            eventsDisplayed++;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < (size)) {
            ++n;
        }
        return n - 1;
    }

}
