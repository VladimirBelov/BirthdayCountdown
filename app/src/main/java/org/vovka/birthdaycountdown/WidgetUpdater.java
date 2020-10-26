/*
 * *
 *  * Created by Vladimir Belov on 27.10.20 0:43
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 27.10.20 0:22
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.vovka.birthdaycountdown.Constants.ACTION_LAUNCH;
import static org.vovka.birthdaycountdown.Constants.PARAM_APP_WIDGET_ID;
import static org.vovka.birthdaycountdown.Constants.REGEX_PLUS;
import static org.vovka.birthdaycountdown.Constants.RES_TYPE_COLOR;
import static org.vovka.birthdaycountdown.Constants.STRING_0;
import static org.vovka.birthdaycountdown.Constants.STRING_1;
import static org.vovka.birthdaycountdown.Constants.STRING_2;
import static org.vovka.birthdaycountdown.Constants.STRING_2HASH;
import static org.vovka.birthdaycountdown.Constants.STRING_3;
import static org.vovka.birthdaycountdown.Constants.STRING_4;
import static org.vovka.birthdaycountdown.Constants.STRING_5;
import static org.vovka.birthdaycountdown.Constants.STRING_6;
import static org.vovka.birthdaycountdown.Constants.STRING_7;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_ID;
import static org.vovka.birthdaycountdown.Constants.STRING_STORAGE_CALENDAR;
import static org.vovka.birthdaycountdown.Constants.STRING_STORAGE_CONTACTS;
import static org.vovka.birthdaycountdown.Constants.Type_5K;
import static org.vovka.birthdaycountdown.Constants.Type_BirthDay;
import static org.vovka.birthdaycountdown.Constants.Type_CalendarEvent;
import static org.vovka.birthdaycountdown.Constants.WIDGET_EVENTS_MAX;
import static org.vovka.birthdaycountdown.Constants.WIDGET_EVENT_INFO;
import static org.vovka.birthdaycountdown.Constants.WIDGET_ICON_EVENT_TYPE;
import static org.vovka.birthdaycountdown.Constants.WIDGET_ICON_FAV;
import static org.vovka.birthdaycountdown.Constants.WIDGET_ICON_ZODIAC;
import static org.vovka.birthdaycountdown.Constants.WIDGET_ICON_ZODIAC_YEAR;
import static org.vovka.birthdaycountdown.Constants.WIDGET_IMAGE_VIEW;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_BIG;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_SMALL;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_TINY;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_VIEW;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_VIEW_2_ND;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_VIEW_2_ND_CENTERED;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_VIEW_AGE;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_VIEW_CENTERED;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_VIEW_DISTANCE;
import static org.vovka.birthdaycountdown.Constants.WIDGET_UPDATER_DRAW_EVENT_ERROR;
import static org.vovka.birthdaycountdown.Constants.WIDGET_UPDATER_INVOKE_ERROR;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventDateText;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventStorage;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventSubType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_id;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_nickname;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_starred;

class WidgetUpdater {
    final private Context context;
    final private ContactsEvents eventsData;
    final private RemoteViews views;
    final private int eventsCount;
    final private int width;
    final private int height;
    private final int widgetId;
    private Resources resources;
    //private DisplayMetrics displayMetrics;
    private String packageName;
    private double fontMagnify;
    private int colorDefault;
    private int colorEventToday;
    private int colorEventSoon;
    private int eventsHidden;
    private int eventsDisplayed;
    private int eventsToShow;
    private List<String> widgetPref;

    WidgetUpdater(@NonNull Context context, @NonNull ContactsEvents eventsData, @NonNull RemoteViews views, int eventsCount, int width, int height, int widgetId) {
        this.context = context;
        this.eventsData = eventsData;
        this.views = views;
        this.eventsCount = eventsCount > WIDGET_EVENTS_MAX ? WIDGET_EVENTS_MAX : eventsCount > 0 ? eventsCount : 1;
        this.width = width;
        this.height = height;
        this.widgetId = widgetId;
    }

    void invoke() {
        //По нажатию на виджет открываем основное окно
        //http://flowovertop.blogspot.com/2013/04/android-widget-with-button-click-to.html
        Intent intentView = new Intent(context, MainActivity.class);
        intentView.setAction(ACTION_LAUNCH);
        views.setOnClickPendingIntent(R.id.appwidget_main, PendingIntent.getActivity(context, 0, intentView, PendingIntent.FLAG_UPDATE_CURRENT));

        //Получаем данные
        boolean canReadContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if (canReadContacts && (eventsData.isEmptyArray() || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000)) {
            eventsData.context = context;
            if (eventsData.getEvents(context)) eventsData.computeDates();
        }

        //Отрисовываем события
        try {

            //Скрываем все события
            resources = context.getResources();
            //displayMetrics = resources.getDisplayMetrics();
            packageName = context.getPackageName();
            for (int e = 0; e < this.eventsCount; e++) {
                views.setViewVisibility(resources.getIdentifier(WIDGET_EVENT_INFO + e, STRING_ID, packageName), View.INVISIBLE);
            }

            //Получаем настройки отображения виджета
            widgetPref = eventsData.getWidgetPreference(widgetId);

            int startingIndex = 1;
            try {
                if (widgetPref.size() > 0) startingIndex = Integer.parseInt(widgetPref.get(0));
            } catch (Exception e) {/**/}

            if (!canReadContacts) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_access_contacts));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else if (eventsData.isEmptyArray() || eventsData.eventList.size() < startingIndex) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_events));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else {

                eventsToShow = Math.min(this.eventsCount, eventsData.eventList.size());

                //Увеличение шрифтов в зависимости от размеров окна
                fontMagnify = 1;
                int cells = getCellsForSize(Math.min(width, height));
                if (widgetPref.size() > 1 && !widgetPref.get(1).equals(STRING_0)) {
                    switch (widgetPref.get(1)) {
                        case STRING_1:
                            fontMagnify = cells * 0.5;
                            break;
                        case STRING_2:
                            fontMagnify = cells * 0.75;
                            break;
                        case STRING_3:
                            fontMagnify = cells * 1.0;
                            break;
                        case STRING_4:
                            fontMagnify = cells * 1.2;
                            break;
                        case STRING_5:
                            fontMagnify = cells * 1.5;
                            break;
                        case STRING_6:
                            fontMagnify = cells * 1.75;
                            break;
                        case STRING_7:
                            fontMagnify = cells * 2.0;
                            break;
                    }
                } else {
                    fontMagnify = 1 + 1.0 * (cells - 1);
                }

                colorDefault = resources.getColor(R.color.white);
                try {
                    colorEventToday = resources.getColor(resources.getIdentifier(eventsData.preferences_widgets_color_eventtoday, RES_TYPE_COLOR, packageName));
                } catch (Resources.NotFoundException e) {
                    colorEventToday = colorDefault;
                }
                try {
                    colorEventSoon = resources.getColor(resources.getIdentifier(eventsData.preferences_widgets_color_eventsoon, RES_TYPE_COLOR, packageName));
                } catch (Resources.NotFoundException e) {
                    colorEventSoon = colorDefault;
                }

                //Отрисовываем информацию о событиях
                eventsHidden = 0;
                eventsDisplayed = 0;
                for (int i = 0; i < (eventsToShow + eventsHidden); i++) {
                    eventsHidden = drawEvent(i, startingIndex);
                }

                if (eventsDisplayed == 0) { //вообще ничего не нашли

                    views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_events));
                    views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);
                    Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
                    intentConfig.setAction(ACTION_LAUNCH);
                    intentConfig.putExtra(PARAM_APP_WIDGET_ID, widgetId);
                    views.setOnClickPendingIntent(R.id.appwidget_text, PendingIntent.getActivity(context, widgetId, intentConfig, 0));

                } else {

                    views.setViewVisibility(R.id.appwidget_text, View.GONE);

                }

                //Если события есть - рисуем бордюр, иначе - прозрачность
                //https://stackoverflow.com/questions/12523005/how-set-background-drawable-programmatically-in-android
                views.setInt(R.id.appwidget_main,"setBackgroundResource", eventsToShow > 0 && eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_Border) ? R.drawable.layout_bg : 0);

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, WIDGET_UPDATER_INVOKE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private int drawEvent(int i, int startingIndex) {
        //Отрисовываем одно событие
        try {

            if ((i + startingIndex) > eventsData.eventList.size()) return eventsHidden; //больше ничего не нашли

            String event = eventsData.eventList.get(i + startingIndex - 1);
            String[] singleRowArray = event.split(STRING_2HASH);

            boolean isVisibleEvent = false;
            boolean useEventListPrefs = true;

            final String eventSubType = singleRowArray[Position_eventSubType];
            if  (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_CalendarEvent))) {
                useEventListPrefs = false;
            } else if (widgetPref.size() > 3) {
                List<String> eventsPrefList =  Arrays.asList(widgetPref.get(3).split(REGEX_PLUS));
                if (eventsPrefList.size() > 0) {
                    useEventListPrefs = false;
                    isVisibleEvent = eventsPrefList.contains(singleRowArray[Position_eventType]) &&
                            (eventsData.getHiddenEventsCount() == 0 || !eventsData.checkIsHiddenEvent(eventsData.getEventKey(singleRowArray)));
                }
            }
            if (useEventListPrefs) isVisibleEvent = eventsData.preferences_list_event_types.contains(singleRowArray[Position_eventType]) &&
                    (eventsData.getHiddenEventsCount() == 0 || !eventsData.checkIsHiddenEvent(eventsData.getEventKey(singleRowArray)));

            if (isVisibleEvent) {
                Person person = new Person(context, event);
                int eventCell = i - eventsHidden;
                int visibleCell = 1; //2 - top left, 3 - top center, 5 - bottom left, 7 - bottom center
                eventsDisplayed++;

                //Под фото
                int id_widget_Caption_left = resources.getIdentifier(WIDGET_TEXT_VIEW + eventCell, STRING_ID, packageName);
                int id_widget_Caption_centered = resources.getIdentifier(WIDGET_TEXT_VIEW_CENTERED + eventCell, STRING_ID, packageName);

                switch (eventsData.preferences_widgets_bottom_info) {
                    case STRING_1: //Фамилия Имя Отчество
                        views.setTextViewText(id_widget_Caption_left, person.getFullName());
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.INVISIBLE);
                        visibleCell*=2;
                        break;
                    case STRING_2: //Дата события
                        views.setTextViewText(id_widget_Caption_centered, singleRowArray[Position_eventDateText]);
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case STRING_3: //Фамилия И.О.
                        views.setTextViewText(id_widget_Caption_left, eventsData.getContactFullNameShort(Long.parseLong(singleRowArray[Position_id])));
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.INVISIBLE);
                        visibleCell*=2;
                        break;
                    case STRING_4: //Имя Отчество Фамилия
                        views.setTextViewText(id_widget_Caption_left, person.getFullNameAlt());
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.INVISIBLE);
                        visibleCell*=2;
                        break;
                    case STRING_5: //Имя
                        views.setTextViewText(id_widget_Caption_centered, singleRowArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) ?
                                eventsData.getContactData(Long.parseLong(singleRowArray[Position_id]), ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) :
                                person.getFirstName());
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case STRING_6: //Фамилия
                        views.setTextViewText(id_widget_Caption_centered, singleRowArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) ?
                                eventsData.getContactData(Long.parseLong(singleRowArray[Position_id]), ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) :
                                person.getSecondName());
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case STRING_7: //Псевдоним (Имя)
                        views.setTextViewText(id_widget_Caption_centered, singleRowArray[Position_nickname].trim().length() > 0 ?
                                singleRowArray[Position_nickname] :
                                eventsData.getContactData(Long.parseLong(singleRowArray[Position_id]), ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case "99": //Ничего
                    default:
                        views.setTextViewText(id_widget_Caption_left, STRING_EMPTY);
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.INVISIBLE);
                        break;
                }

                views.setTextViewTextSize(id_widget_Caption_centered, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_TINY * fontMagnify));
                views.setTextViewTextSize(id_widget_Caption_left, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_TINY * fontMagnify));

                //Под фото (верхний ряд)
                int id_widget_Caption2nd_left = resources.getIdentifier(WIDGET_TEXT_VIEW_2_ND + eventCell, STRING_ID, packageName);
                int id_widget_Caption2nd_centered = resources.getIdentifier(WIDGET_TEXT_VIEW_2_ND_CENTERED + eventCell, STRING_ID, packageName);

                switch (eventsData.preferences_widgets_bottom_info_2nd) {
                    case STRING_1: //Фамилия Имя Отчество
                        views.setTextViewText(id_widget_Caption2nd_left, person.getFullName());
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.INVISIBLE);
                        visibleCell*=5;
                        break;
                    case STRING_2: //Дата события
                        views.setTextViewText(id_widget_Caption2nd_centered, singleRowArray[Position_eventDateText]);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case STRING_3: //Фамилия И.О.
                        views.setTextViewText(id_widget_Caption2nd_left, eventsData.getContactFullNameShort(Long.parseLong(singleRowArray[Position_id])));
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.INVISIBLE);
                        visibleCell*=5;
                        break;
                    case STRING_4: //Имя Отчество Фамилия
                        views.setTextViewText(id_widget_Caption2nd_left, person.getFullNameAlt());
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.INVISIBLE);
                        visibleCell*=5;
                        break;
                    case STRING_5: //Имя
                        views.setTextViewText(id_widget_Caption2nd_centered, singleRowArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) ?
                                eventsData.getContactData(Long.parseLong(singleRowArray[Position_id]), ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME) :
                                person.getFirstName());
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case STRING_6: //Фамилия
                        views.setTextViewText(id_widget_Caption2nd_centered, singleRowArray[Position_eventStorage].equals(STRING_STORAGE_CONTACTS) ?
                                eventsData.getContactData(Long.parseLong(singleRowArray[Position_id]), ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME) :
                                person.getSecondName());
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case STRING_7: //Псевдоним (Имя)
                        views.setTextViewText(id_widget_Caption2nd_centered, singleRowArray[Position_nickname].trim().length() > 0 ?
                                singleRowArray[Position_nickname] :
                                eventsData.getContactData(Long.parseLong(singleRowArray[Position_id]), ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case "99": //Ничего
                    default:
                        views.setTextViewText(id_widget_Caption2nd_left, STRING_EMPTY);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.INVISIBLE);
                        break;
                }

                views.setTextViewTextSize(id_widget_Caption2nd_centered, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_TINY * fontMagnify));
                views.setTextViewTextSize(id_widget_Caption2nd_left, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_TINY * fontMagnify));

                //Фото
                // todo: сделать закругления углов фото https://stackoverflow.com/questions/2459916/how-to-make-an-imageview-with-rounded-corners
                int id_widget_Photo = resources.getIdentifier(WIDGET_IMAGE_VIEW + eventCell, STRING_ID, packageName);

                Bitmap photo = eventsData.getContactPhoto(event, eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_Photo), true);
                if (photo != null) {
                    if (eventsToShow == 1) {
                        views.setImageViewBitmap(id_widget_Photo, photo);
                    } else {
                        //потому что вот: https://stackoverflow.com/questions/13494898/remoteviews-for-widget-update-exceeds-max-bitmap-memory-usage-error
                        final int dstWidth = 2 * width / eventsToShow;
                        final int dstHeight = (2 * photo.getHeight() * width) / (photo.getWidth() * eventsToShow);
                        if (dstHeight > 0 && dstWidth > 0) {
                            Bitmap photo_small = Bitmap.createScaledBitmap(photo, dstWidth, dstHeight, true);
                            views.setImageViewBitmap(id_widget_Photo, photo_small);
                        } else {
                            Bitmap photo_icon = eventsData.getContactPhoto(event, false, true);
                            views.setImageViewBitmap(id_widget_Photo, photo_icon);
                        }
                        //photo.recycle(); //https://stackoverflow.com/questions/38784302/cant-parcel-a-recycled-bitmap
                    }
                }
                //views.setInt(id_widget_Photo, "setBackgroundResource", R.drawable.selection_rectangle); //не работает

                //Иконка события
                int id_widget_EventIcon = resources.getIdentifier(WIDGET_ICON_EVENT_TYPE + eventCell, STRING_ID, packageName);


                if (eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_EventIcon)) {

                    int eventIcon;
                    try {
                        eventIcon = Integer.parseInt(singleRowArray[ContactsEvents.Position_eventIcon]);
                    } catch (NumberFormatException e) {
                        eventIcon = 0;
                    }
                    if (eventIcon != 0) {
                        views.setImageViewResource(id_widget_EventIcon, eventIcon);
                    } else {
                        views.setImageViewResource(id_widget_EventIcon, android.R.color.transparent);
                    }

                    views.setViewVisibility(id_widget_EventIcon, View.VISIBLE);

                    //todo: это было сделано для какого-то дремучего xiaomi или sony, наверное, можно выбросить
                    /*//if (width != -1) {//Если это масштабируемый виджет, приходится делать padding
                    if (eventsCount == 1) {
                        views.setViewPadding(id_widget_Age, 0, convertDipToPixels(2, displayMetrics), 0, 0);
                    } else {
                        views.setViewPadding(id_widget_Age, 0, convertDipToPixels(1, displayMetrics), 0, 0);
                    }
                    //}*/

                } else {

                    views.setViewVisibility(id_widget_EventIcon, View.GONE);
                    /*//if (width != -1) {//Если это масштабируемый виджет, приходится делать padding
                    if (eventsCount == 1) {
                        views.setViewPadding(id_widget_Age, convertDipToPixels(1, displayMetrics), 0, 0, 0);
                    } else {
                        views.setViewPadding(id_widget_Age, convertDipToPixels(eventCell == 0 ? 4 : 2, displayMetrics), 0, 0, 0);
                    }
                    //}*/
                }

                //Иконка знака зодиака
                //https://emojipedia.org/microsoft/windows-10-may-2019-update/aquarius/
                String strZodiacInfo = STRING_EMPTY;
                int id_widget_ZodiacIcon = resources.getIdentifier(WIDGET_ICON_ZODIAC + eventCell, STRING_ID, packageName);

                if (eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_ZodiacSign)) {

                    if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_BirthDay)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_5K))) {

                        strZodiacInfo = eventsData.getZodiacInfo(ContactsEvents.ZodiacInfo.SIGN, singleRowArray[Position_eventDateText]);

                    } else if (eventsData.set_events_birthdays.containsKey(singleRowArray[Position_id])) {

                        Locale locale_en = new Locale(Constants.LANG_EN);
                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);
                        final Date birthDate = eventsData.set_events_birthdays.get(singleRowArray[Position_id]);
                        if (birthDate != null) {
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
                String strZodiacYearInfo = STRING_EMPTY;
                int id_widget_ZodiacYearIcon = resources.getIdentifier(WIDGET_ICON_ZODIAC_YEAR + eventCell, STRING_ID, packageName);

                if (eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_ZodiacYear)) {

                    if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_BirthDay)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_5K))) {

                        strZodiacYearInfo = eventsData.getZodiacInfo(ContactsEvents.ZodiacInfo.YEAR, singleRowArray[Position_eventDateText]);

                    } else if (eventsData.set_events_birthdays.containsKey(singleRowArray[Position_id])) {

                        Locale locale_en = new Locale(Constants.LANG_EN);
                        SimpleDateFormat sdfYear = new SimpleDateFormat(Constants.DATETIME_DD_MM_YYYY, locale_en);
                        final Date birthDate = eventsData.set_events_birthdays.get(singleRowArray[Position_id]);
                        if (birthDate != null) {
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
                int id_widget_FavIcon = resources.getIdentifier(WIDGET_ICON_FAV + eventCell, STRING_ID, packageName);
                views.setViewVisibility(id_widget_FavIcon, eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_FavoritesIcon) && singleRowArray[Position_starred].equals(STRING_1) ? View.VISIBLE : View.GONE);

                //Цвета по-умолчанию
                int id_widget_Age = resources.getIdentifier(WIDGET_TEXT_VIEW_AGE + eventCell, STRING_ID, packageName);

                views.setTextColor(id_widget_Age, colorDefault);
                views.setTextColor(id_widget_Caption_left, colorDefault);
                views.setTextColor(id_widget_Caption_centered, colorDefault);
                views.setTextColor(id_widget_Caption2nd_left, colorDefault);
                views.setTextColor(id_widget_Caption2nd_centered, colorDefault);
                //}

                //Сколько осталось до события
                int idViewDistance = resources.getIdentifier(WIDGET_TEXT_VIEW_DISTANCE + eventCell, STRING_ID, packageName);
                String eventDistance = singleRowArray[ContactsEvents.Position_eventDistance];
                int eventDistance_Days;
                try {
                    eventDistance_Days = Integer.parseInt(eventDistance);
                } catch (Exception e) {
                    eventDistance_Days = 999; /* не знаю, почему */
                }

                if (eventDistance_Days == 0) { //Сегодня

                    if (colorEventToday != 0) {
                        if (person.Age > -1) {
                            views.setTextColor(id_widget_Age, colorEventToday);
                            //views.setInt(context.getResources().getIdentifier("textViewAge" + i, "id", context.getPackageName()),"setShadowColor", context.getResources().getColor(R.color.white));
                        } else {
                            //Если возраста нет и событие уже сегодня - ставим цвет для ФИО
                            views.setTextColor(id_widget_Caption_left, colorEventToday);
                            views.setTextColor(id_widget_Caption_centered, colorEventToday);
                            views.setTextColor(id_widget_Caption2nd_left, colorEventToday);
                            views.setTextColor(id_widget_Caption2nd_centered, colorEventToday);
                        }
                    }
                    views.setTextViewText(idViewDistance, STRING_EMPTY);

                } else if (eventDistance_Days >= 1 && eventDistance_Days <= eventsData.preferences_widgets_days_eventsoon) { //Скоро

                    if (colorEventSoon != 0) views.setTextColor(idViewDistance, colorEventSoon);
                    views.setTextViewText(idViewDistance, eventDistance);
                    views.setTextViewTextSize(idViewDistance, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_BIG * fontMagnify));

                } else { //Попозже

                    views.setTextColor(idViewDistance, colorDefault);
                    views.setTextViewText(idViewDistance, eventDistance);
                    views.setTextViewTextSize(idViewDistance, TypedValue.COMPLEX_UNIT_SP, (float) ((Integer.parseInt(eventDistance) < WIDGET_TEXT_SIZE_TINY ? WIDGET_TEXT_SIZE_BIG : WIDGET_TEXT_SIZE_SMALL) * fontMagnify));

                }

                //Возраст
                if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_5K))) {
                    views.setTextViewText(id_widget_Age, singleRowArray[ContactsEvents.Position_age_caption]);
                } else if (person.Age > -1) {
                    views.setTextViewText(id_widget_Age, Integer.toString(person.Age));
                } else {
                    views.setTextViewText(id_widget_Age, STRING_EMPTY);
                }
                views.setTextColor(resources.getIdentifier(WIDGET_TEXT_VIEW + eventCell, STRING_ID, packageName), colorDefault);
                //views.setInt(context.getResources().getIdentifier("textViewAge" + i, "id", context.getPackageName()),"setShadowColor", context.getResources().getColor(R.color.dark_gray));
                views.setTextViewTextSize(id_widget_Age, TypedValue.COMPLEX_UNIT_SP, (float) ((eventDistance_Days == 0 ? WIDGET_TEXT_SIZE_BIG : WIDGET_TEXT_SIZE_SMALL) * fontMagnify));

                //Если не последнее событие - по нажатию на фото открываем карточку контакта
                if (eventsToShow > 1 && eventCell < (eventsToShow - 1)) {

                    Intent intentView = new Intent(Intent.ACTION_VIEW);
                    Uri uri;
                    if (!singleRowArray[Position_eventStorage].equals(STRING_STORAGE_CALENDAR)) {
                        uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleRowArray[Position_id]);
                    } else {
                        uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, singleRowArray[Position_id]);
                    }
                    intentView.setData(uri);
                    views.setOnClickPendingIntent(resources.getIdentifier(WIDGET_EVENT_INFO + eventCell, STRING_ID, packageName), PendingIntent.getActivity(context, 0, intentView, PendingIntent.FLAG_UPDATE_CURRENT));

                } else {

                    Intent intentConfig = new Intent(context, WidgetConfigureActivity.class);
                    intentConfig.setAction(ACTION_LAUNCH);
                    intentConfig.putExtra(PARAM_APP_WIDGET_ID, widgetId);
                    if (visibleCell % 2 == 0) views.setOnClickPendingIntent(resources.getIdentifier(WIDGET_TEXT_VIEW + eventCell, STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, 0));
                    if (visibleCell % 3 == 0) views.setOnClickPendingIntent(resources.getIdentifier(WIDGET_TEXT_VIEW_CENTERED + eventCell, STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, 0));
                    if (visibleCell % 5 == 0) views.setOnClickPendingIntent(resources.getIdentifier(WIDGET_TEXT_VIEW_2_ND + eventCell, STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, 0));
                    if (visibleCell % 7 == 0) views.setOnClickPendingIntent(resources.getIdentifier(WIDGET_TEXT_VIEW_2_ND_CENTERED + eventCell, STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intentConfig, 0));

                }

                //Показываем событие
                views.setViewVisibility(resources.getIdentifier(WIDGET_EVENT_INFO + eventCell, STRING_ID, packageName), View.VISIBLE);

            } else {
                //todo: если событий меньше ширины экрана, то нужно биндить последнее показанное событие на показ настройки виджета
                return eventsHidden + 1;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, WIDGET_UPDATER_DRAW_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
        return eventsHidden;
    }

    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < (size)) {
            ++n;
        }
        return n - 1;
    }

/*    private static int convertDipToPixels(float dips, @NonNull DisplayMetrics displayMetrics)
    {
        return (int) (dips * displayMetrics.density + 0.5f);
    }*/

}
