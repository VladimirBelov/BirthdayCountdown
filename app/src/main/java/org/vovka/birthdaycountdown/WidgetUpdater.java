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
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

class WidgetUpdater {

    private static final String TAG = "WidgetUpdater";
    public static final String ALIGN_LEFT = "Left";
    public static final String ALIGN_CENTER = "Center";
    public static final String ALIGN_RIGHT = "Right";
    final private Context context;
    final private ContactsEvents eventsData;
    final private RemoteViews views;
    private final int eventsCount;
    private int daysCount = 0;
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
    private final Map<Integer, Integer> captionsPrefMap = new HashMap<>();

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
            eventsData.getEvents(context);
        }

        try {
            //Скрываем все события
            resources = context.getResources();
            packageName = context.getPackageName();
            for (int e = 0; e < Constants.WIDGET_EVENTS_MAX; e++) {
                views.setViewVisibility(resources.getIdentifier(Constants.WIDGET_EVENT_INFO + e, Constants.STRING_ID, packageName), View.GONE);
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

            //Объём событий
            String prefScope = Constants.STRING_EMPTY;
            if (widgetPref.size() > 8) prefScope = widgetPref.get(8);
            if (!TextUtils.isEmpty(prefScope)) {
                Matcher matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE_PLUS).matcher(prefScope);
                boolean found = matchScopes.find();
                if (!found) {
                    matchScopes = Pattern.compile(Constants.REGEX_EVENTS_SCOPE).matcher(prefScope).reset();
                    found = matchScopes.find();
                }
                if (found) {
                    final String scopeDays = matchScopes.group(2);
                    if (scopeDays != null) {
                        if (!scopeDays.equals(Constants.STRING_0)){ //Дни
                            try {
                                this.daysCount = Integer.parseInt(scopeDays);
                            } catch (NumberFormatException ignored) { /**/ }
                        }
                    }
                    if (widgetType.equals(Constants.WIDGET_TYPE_5X1) || widgetType.equals(Constants.WIDGET_TYPE_4X1)
                            || widgetType.equals(Constants.WIDGET_TYPE_2X2)) {
                        final String scopeLayout = matchScopes.group(3);
                        if (scopeLayout != null) {
                            if (scopeLayout.equals(Constants.STRING_MINUS)) { //Оставить пустоту
                                for (int e = 0; e < Constants.WIDGET_EVENTS_MAX; e++) {
                                    views.setViewVisibility(resources.getIdentifier(Constants.WIDGET_EVENT_INFO + e, Constants.STRING_ID, packageName), View.INVISIBLE);
                                }
                            }
                        }
                    }
                }
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

            //Параметры заголовков
            List<String> prefCaptions = new ArrayList<>();
            if (widgetPref.size() > 11) prefCaptions.addAll(Arrays.asList(widgetPref.get(11).split(Constants.REGEX_PLUS)));

            List<String> listBottomInfo = Arrays.asList(resources.getStringArray(R.array.pref_Widgets_BottomInfo_values));

            //Из общих настроек
            captionsPrefMap.put(Constants.PhotoWidget_Upper_Caption, Integer.valueOf(eventsData.preferences_widgets_bottom_info_2nd));
            if (!eventsData.preferences_widgets_bottom_info_2nd.equals(resources.getString(R.string.pref_Widgets_BottomInfo_none))) {
                captionsPrefMap.put(Constants.PhotoWidget_Upper_Aligning, eventsData.getDefaultAligningForEventInfo(eventsData.preferences_widgets_bottom_info_2nd));
                captionsPrefMap.put(Constants.PhotoWidget_Upper_Rows, 3);
                captionsPrefMap.put(Constants.PhotoWidget_Upper_FontStyle, Typeface.NORMAL);
                captionsPrefMap.put(Constants.PhotoWidget_Upper_FontSize, (int) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));
                captionsPrefMap.put(Constants.PhotoWidget_Upper_Color, colorDefault);
            }

            captionsPrefMap.put(Constants.PhotoWidget_Bottom_Caption, Integer.valueOf(eventsData.preferences_widgets_bottom_info));
            if (!eventsData.preferences_widgets_bottom_info.equals(resources.getString(R.string.pref_Widgets_BottomInfo_none))) {
                captionsPrefMap.put(Constants.PhotoWidget_Bottom_Aligning, eventsData.getDefaultAligningForEventInfo(eventsData.preferences_widgets_bottom_info));
                captionsPrefMap.put(Constants.PhotoWidget_Bottom_Rows, 3);
                captionsPrefMap.put(Constants.PhotoWidget_Bottom_FontStyle, Typeface.NORMAL);
                captionsPrefMap.put(Constants.PhotoWidget_Bottom_FontSize, (int) (Constants.WIDGET_TEXT_SIZE_TINY * fontMagnify));
                captionsPrefMap.put(Constants.PhotoWidget_Bottom_Color, colorDefault);
            }

            //Из настройки виджета
            if (prefCaptions.size() == Constants.PhotoWidget_Bottom_Color + 1) {
                int pref;
                if (listBottomInfo.contains(prefCaptions.get(Constants.PhotoWidget_Upper_Caption))) {
                    try {
                        pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Caption));
                        captionsPrefMap.put(Constants.PhotoWidget_Upper_Caption, pref);
                    } catch (NumberFormatException ignored) { /**/ }
                }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Aligning));
                    captionsPrefMap.put(Constants.PhotoWidget_Upper_Aligning, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Rows));
                    captionsPrefMap.put(Constants.PhotoWidget_Upper_Rows, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_FontStyle));
                    captionsPrefMap.put(Constants.PhotoWidget_Upper_FontStyle, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_FontSize));
                    captionsPrefMap.put(Constants.PhotoWidget_Upper_FontSize, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Upper_Color));
                    captionsPrefMap.put(Constants.PhotoWidget_Upper_Color, pref);
                } catch (NumberFormatException ignored) { /**/ }
                if (listBottomInfo.contains(prefCaptions.get(Constants.PhotoWidget_Bottom_Caption))) {
                    try {
                        pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Caption));
                        captionsPrefMap.put(Constants.PhotoWidget_Bottom_Caption, pref);
                    } catch (NumberFormatException ignored) { /**/ }
                }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Aligning));
                    captionsPrefMap.put(Constants.PhotoWidget_Bottom_Aligning, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Rows));
                    captionsPrefMap.put(Constants.PhotoWidget_Bottom_Rows, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_FontStyle));
                    captionsPrefMap.put(Constants.PhotoWidget_Bottom_FontStyle, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_FontSize));
                    captionsPrefMap.put(Constants.PhotoWidget_Bottom_FontSize, pref);
                } catch (NumberFormatException ignored) { /**/ }
                try {
                    pref = Integer.parseInt(prefCaptions.get(Constants.PhotoWidget_Bottom_Color));
                    captionsPrefMap.put(Constants.PhotoWidget_Bottom_Color, pref);
                } catch (NumberFormatException ignored) { /**/ }
            }

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
            if (eventsDisplayed > 0 && (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_Border_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Border_ID)))) {
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
                    eventSubType.equals(ContactsEvents.getEventType(Constants.Type_FileEvent)) ||
                            eventSubType.equals(ContactsEvents.getEventType(Constants.Type_HolidayEvent))) {
                //пропускаем события календарей, из файлов и праздники
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

            if (isVisibleEvent && daysCount > 0) {
                Date eventDate = null;
                try {
                    eventDate = eventsData.sdf_DDMMYYYY.parse(singleEventArray[ContactsEvents.Position_eventDateNextTime]);
                } catch (Exception e) { /**/ }

                if (eventDate != null) {
                    Calendar now = Calendar.getInstance();
                    Date currentDay = new Date(now.getTimeInMillis());
                    long countDays = eventsData.countDaysDiff(currentDay, eventDate);
                    if (countDays + 1 > daysCount) isVisibleEvent = false;
                }
            }

            if (!isVisibleEvent) {
                return;
            } else if (eventsToSkip > 0) {
                eventsToSkip--;
                return;
            }

            Person person = new Person(context, event);
            @Nullable String rowValue;
            int indSpace;
            List<String> aligns = new ArrayList<>();
            aligns.add(ALIGN_LEFT);
            aligns.add(ALIGN_CENTER);
            aligns.add(ALIGN_RIGHT);

            //Надпись (верхний ряд)

            int layoutCaptionUpper = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND_LAYOUT + eventsDisplayed, Constants.STRING_ID, packageName);
            views.setViewVisibility(layoutCaptionUpper, View.INVISIBLE);

            int textCaptionUpper = 0;

            for (String align: aligns) {
                textCaptionUpper = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND + align + eventsDisplayed, Constants.STRING_ID, packageName);
                views.setViewVisibility(textCaptionUpper, View.GONE);
            }

            boolean isUpperCaption = false;
            rowValue = null;
            String captionUpper = String.valueOf(captionsPrefMap.get(Constants.PhotoWidget_Upper_Caption));
            String captionBottom = String.valueOf(captionsPrefMap.get(Constants.PhotoWidget_Bottom_Caption));
            if (!captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_none))) {

                //Надпись
                if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecond))) { //Фамилия Имя Отчество
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecondNick))) { //Фамилия Имя Отчество (Псевдоним)
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    if (!singleEventArray[ContactsEvents.Position_nickname].trim().isEmpty()) {
                        rowValue = rowValue
                                .concat(Constants.STRING_PARENTHESIS_OPEN)
                                .concat(singleEventArray[ContactsEvents.Position_nickname])
                                .concat(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventDate))) { //Дата события
                    rowValue = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFS))) { //Фамилия И.О. (Имя Отчество, если нет фамилии)
                    rowValue = person.getFullNameShort();
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_FirstSecondLast))) { //Имя Отчество Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_First))) { //Имя
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Last))) { //Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Nick))) { //Псевдоним (Имя, если отсутствует)
                    if (!singleEventArray[ContactsEvents.Position_nickname].trim().isEmpty()) {
                        rowValue = singleEventArray[ContactsEvents.Position_nickname];
                    } else if (!captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecond))
                            && !captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecondNick))
                            && !captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFS))
                            && !captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_FirstSecondLast))
                            && !captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_First))) {
                        rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                        indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                        if (indSpace > -1) {
                            rowValue = rowValue.substring(0, indSpace);
                        }
                    }
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventType))) { //Тип события
                    rowValue = singleEventArray[ContactsEvents.Position_eventCaption];
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventLabel))) { //Наименование события
                    rowValue =
                            singleEventArray[ContactsEvents.Position_eventLabel].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_eventCaption] :
                                    singleEventArray[ContactsEvents.Position_eventLabel];
                } else if (captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Organization))) { //Организация (Должность, если отсутствует)
                    rowValue = !singleEventArray[ContactsEvents.Position_organization].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_organization] : singleEventArray[ContactsEvents.Position_title];
                }

            }
            if (!TextUtils.isEmpty(rowValue)) {

                textCaptionUpper = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND + ALIGN_CENTER + eventsDisplayed, Constants.STRING_ID, packageName);

                //Выравнивание
                //TextView до Android 12 не понимает setGravity через setInt, по-этому, для каждого выравнивания - свой TextView
                //https://stackoverflow.com/questions/7825508/remoteviews-textview-setgravity

                Integer aligning = captionsPrefMap.get(Constants.PhotoWidget_Upper_Aligning);
                if (aligning != null) {
                    if (aligning == Constants.Align_Left) {
                        textCaptionUpper = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND + ALIGN_LEFT + eventsDisplayed, Constants.STRING_ID, packageName);
                    } else if (aligning == Constants.Align_Right) {
                        textCaptionUpper = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_2_ND + ALIGN_RIGHT + eventsDisplayed, Constants.STRING_ID, packageName);
                    }
                }
                //Размер
                Integer fontSize = captionsPrefMap.get(Constants.PhotoWidget_Upper_FontSize);
                if (fontSize != null) {
                    views.setTextViewTextSize(textCaptionUpper, COMPLEX_UNIT_SP, (float) fontSize);
                }
                //Количество строк
                Integer rows = captionsPrefMap.get(Constants.PhotoWidget_Upper_Rows);
                if (rows != null) {
                    views.setBoolean(textCaptionUpper,"setSingleLine", rows == 1);
                    views.setInt(textCaptionUpper,"setMaxLines", rows);
                }
                //Цвет
                Integer color = captionsPrefMap.get(Constants.PhotoWidget_Upper_Color);
                if (color != null) {
                    views.setTextColor(textCaptionUpper, color);
                }

                //Стиль текста
                Integer style = captionsPrefMap.get(Constants.PhotoWidget_Upper_FontStyle);
                if (style != null && style != Typeface.NORMAL) {
                    SpannableString spanValue = new SpannableString(rowValue);
                    spanValue.setSpan(new StyleSpan(style), 0, rowValue.length() - 1, 0);
                    views.setTextViewText(textCaptionUpper, spanValue);
                } else {
                    views.setTextViewText(textCaptionUpper, rowValue);
                }

                views.setViewVisibility(layoutCaptionUpper, View.VISIBLE);
                views.setViewVisibility(textCaptionUpper, View.VISIBLE);
                isUpperCaption = true;
            }

            //Надпись (нижний ряд)

            int layoutCaptionBottom = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_LAYOUT + eventsDisplayed, Constants.STRING_ID, packageName);
            views.setViewVisibility(layoutCaptionBottom, View.INVISIBLE);

            int textCaptionBottom = 0;
            for (String align: aligns) {
                textCaptionBottom = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + align + eventsDisplayed, Constants.STRING_ID, packageName);
                views.setViewVisibility(textCaptionBottom, View.GONE);
            }

            boolean isBottomCaption = false;
            rowValue = null;
            if (!captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_none))) {

                //Надпись
                if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecond))) { //Фамилия Имя Отчество
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecondNick))) { //Фамилия Имя Отчество (Псевдоним)
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    if (!singleEventArray[ContactsEvents.Position_nickname].trim().isEmpty()) {
                        rowValue = rowValue
                                .concat(Constants.STRING_PARENTHESIS_OPEN)
                                .concat(singleEventArray[ContactsEvents.Position_nickname])
                                .concat(Constants.STRING_PARENTHESIS_CLOSE);
                    }
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventDate))) { //Дата события
                    rowValue = eventsData.getDateFormatted(singleEventArray[ContactsEvents.Position_eventDateFirstTime], ContactsEvents.FormatDate.WithYear);
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFS))) { //Фамилия И.О. (Имя Отчество, если нет фамилии)
                    rowValue = person.getFullNameShort();
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_FirstSecondLast))) { //Имя Отчество Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_First))) { //Имя
                    rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Last))) { //Фамилия
                    rowValue = singleEventArray[ContactsEvents.Position_personFullNameAlt];
                    indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                    if (indSpace > -1) {
                        rowValue = rowValue.substring(0, indSpace);
                    }
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Nick))) { //Псевдоним (Имя, если отсутствует)
                    if (!singleEventArray[ContactsEvents.Position_nickname].trim().isEmpty()) {
                        rowValue = singleEventArray[ContactsEvents.Position_nickname];
                    } else if (!captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecond))
                            && !captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFirstSecondNick))
                            && !captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_LastFS))
                            && !captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_FirstSecondLast))
                            && !captionUpper.equals(resources.getString(R.string.pref_Widgets_BottomInfo_First))) {
                        rowValue = singleEventArray[ContactsEvents.Position_personFullName];
                        indSpace = rowValue.indexOf(Constants.STRING_SPACE);
                        if (indSpace > -1) {
                            rowValue = rowValue.substring(0, indSpace);
                        }
                    }
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventType))) { //Тип события
                    rowValue = singleEventArray[ContactsEvents.Position_eventCaption];
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_EventLabel))) { //Наименование события
                    rowValue =
                            singleEventArray[ContactsEvents.Position_eventLabel].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_eventCaption] :
                                    singleEventArray[ContactsEvents.Position_eventLabel];
                } else if (captionBottom.equals(resources.getString(R.string.pref_Widgets_BottomInfo_Organization))) { //Организация (Должность, если отсутствует)
                    rowValue = !singleEventArray[ContactsEvents.Position_organization].trim().isEmpty() ? singleEventArray[ContactsEvents.Position_organization] : singleEventArray[ContactsEvents.Position_title];
                }

            }
            if (!TextUtils.isEmpty(rowValue)) {

                textCaptionBottom = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + ALIGN_CENTER + eventsDisplayed, Constants.STRING_ID, packageName);

                //Выравнивание
                Integer aligning = captionsPrefMap.get(Constants.PhotoWidget_Bottom_Aligning);
                if (aligning != null) {
                    if (aligning == Constants.Align_Left) {
                        textCaptionBottom = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + ALIGN_LEFT + eventsDisplayed, Constants.STRING_ID, packageName);
                    } else if (aligning == Constants.Align_Right) {
                        textCaptionBottom = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW + ALIGN_RIGHT + eventsDisplayed, Constants.STRING_ID, packageName);
                    }
                }
                //Размер
                Integer fontSize = captionsPrefMap.get(Constants.PhotoWidget_Bottom_FontSize);
                if (fontSize != null) {
                    views.setTextViewTextSize(textCaptionBottom, COMPLEX_UNIT_SP, (float) fontSize);
                }
                //Количество строк
                Integer rows = captionsPrefMap.get(Constants.PhotoWidget_Bottom_Rows);
                if (rows != null) {
                    views.setBoolean(textCaptionBottom,"setSingleLine", rows == 1);
                    views.setInt(textCaptionBottom,"setMaxLines", rows);
                }
                //Цвет
                Integer color = captionsPrefMap.get(Constants.PhotoWidget_Bottom_Color);
                if (color != null) {
                    views.setTextColor(textCaptionBottom, color);
                }

                //Стиль текста
                Integer style = captionsPrefMap.get(Constants.PhotoWidget_Bottom_FontStyle);
                if (style != null && style != Typeface.NORMAL) {
                    SpannableString spanValue = new SpannableString(rowValue);
                    spanValue.setSpan(new StyleSpan(style), 0, rowValue.length() - 1, 0);
                    views.setTextViewText(textCaptionBottom, spanValue);
                } else {
                    views.setTextViewText(textCaptionBottom, rowValue);
                }

                views.setViewVisibility(layoutCaptionBottom, View.VISIBLE);
                views.setViewVisibility(textCaptionBottom, View.VISIBLE);
                isBottomCaption = true;
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

            Bitmap photo = eventsData.getEventPhoto(event, widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_Photo_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Photo_ID)), true, true, roundingFactor);
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

            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_EventIcon_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_EventIcon_ID))) {

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

            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_ZodiacSign_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_ZodiacSign_ID))) {

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

            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_ZodiacYear_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_ZodiacYear_ID))) {

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
            if ((widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_FavIcon_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_FavIcon_ID))) && eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred])) {
                views.setViewVisibility(id_widget_FavIcon, View.VISIBLE);
            } else {
                views.setViewVisibility(id_widget_FavIcon, View.GONE);
            }

            //Иконка события без уведомления
            int id_widget_SilencedIcon = resources.getIdentifier(Constants.WIDGET_ICON_SILENCED + eventsDisplayed, Constants.STRING_ID, packageName);
            if ((widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_SilentedIcon_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_SilentedIcon_ID))) && eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {
                views.setTextViewText(id_widget_SilencedIcon, "\uD83D\uDEAB"); //https://emojipedia.org/prohibited/
                views.setViewVisibility(id_widget_SilencedIcon, View.VISIBLE);
            } else {
                views.setViewVisibility(id_widget_SilencedIcon, View.GONE);
            }

            //Цвета по-умолчанию
            int id_widget_Age = resources.getIdentifier(Constants.WIDGET_TEXT_VIEW_AGE + eventsDisplayed, Constants.STRING_ID, packageName);

            views.setTextColor(id_widget_Age, colorDefault);

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
                        views.setTextColor(textCaptionUpper, colorEventToday);
                        //views.setTextColor(id_widget_Caption_centered, colorEventToday);
                        views.setTextColor(textCaptionBottom, colorEventToday);
                        //views.setTextColor(id_widget_Caption2nd_centered, colorEventToday);
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
            if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(context.getString(R.string.pref_EventInfo_Age_ID))
                    : widgetPref_eventInfo.contains(context.getString(R.string.pref_EventInfo_Age_ID))) {

                String ageCaption = singleEventArray[ContactsEvents.Position_age_caption];
                if (ageCaption.contains(Constants.STRING_SPACE)) {
                    views.setTextViewText(id_widget_Age, ageCaption.substring(0, ageCaption.lastIndexOf(Constants.STRING_SPACE)));
                } else {
                    views.setTextViewText(id_widget_Age, ageCaption);
                }

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
                if (isUpperCaption) {
                    views.setOnClickPendingIntent(textCaptionUpper, PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));
                }
                if (isBottomCaption) {
                    views.setOnClickPendingIntent(textCaptionBottom, PendingIntent.getActivity(context, widgetId, intentConfig, PendingIntentImmutable));
                }

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
