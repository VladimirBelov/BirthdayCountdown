/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 24.12.2021, 19:11
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_EVENT;
import static org.vovka.birthdaycountdown.Constants.EXTRA_CLICKED_PREFS;
import static org.vovka.birthdaycountdown.Constants.HTML_BR;
import static org.vovka.birthdaycountdown.Constants.HTML_COLOR;
import static org.vovka.birthdaycountdown.Constants.HTML_FONT_END;
import static org.vovka.birthdaycountdown.Constants.REGEX_PLUS;
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
import static org.vovka.birthdaycountdown.Constants.STRING_BAR;
import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.Constants.Type_5K;
import static org.vovka.birthdaycountdown.Constants.Type_BirthDay;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_SMALL;
import static org.vovka.birthdaycountdown.Constants.WIDGET_TEXT_SIZE_TINY;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_age_caption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_attrAmount;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventCaption;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventEmoji;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventSubType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_personFullName;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_personFullNameAlt;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_zodiacSign;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_zodiacYear;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventPhotoListDataProvider implements RemoteViewsService.RemoteViewsFactory {

    final List<String> eventListView = new ArrayList<>();
    final Context context;
    //final Resources resources;
    final int widgetID;
    int widgetWidth;
    float floatDensity;
    List<String> widgetPref;
    private List<String> widgetPref_eventInfo = new ArrayList<>();
    ContactsEvents eventsData;

    public EventPhotoListDataProvider(Context context, Intent intent) {
        this.context = context;
        //this.resources = context.getResources();
        this.widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        //this.widgetWidth = intent.getIntExtra("intWidgetWidth", 0);
        //this.floatDensity = intent.getFloatExtra("floatScreenDensity", 1);
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

        RemoteViews views = new RemoteViews(this.context.getPackageName(), R.layout.widgetlist_photo_item);

        try {

            //Размер
            double fontMagnify = 1.2;
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
            views.setTextViewTextSize(R.id.eventCaption, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_SMALL * fontMagnify));
            views.setTextViewTextSize(R.id.eventDetails, TypedValue.COMPLEX_UNIT_SP, (float) (WIDGET_TEXT_SIZE_TINY * fontMagnify));

            views.setTextColor(R.id.eventCaption, eventsData.preferences_widgets_color_default);
            views.setTextColor(R.id.eventDetails, eventsData.preferences_widgets_color_default);

            //Информация о событии
            String eventInfo = eventListView.get(position);
            String[] singleEventArray = eventInfo.split(STRING_2HASH, -1);

            if (singleEventArray.length < Position_attrAmount) {

                views.setTextViewText(R.id.eventCaption, eventInfo);

            } else {

                switch (eventsData.preferences_list_nameformat) {
                    case 2: //Фамилия Имя Отчество
                        views.setTextViewText(R.id.eventCaption, singleEventArray[Position_personFullNameAlt]);
                        break;
                    case 1: //Имя Отчество Фамилия
                    default:
                        views.setTextViewText(R.id.eventCaption, singleEventArray[Position_personFullName]);
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

                //Иконка
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_EventIcon)
                        : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_EventIcon)) {
                    sbDetails.append(singleEventArray[Position_eventEmoji]).append(STRING_SPACE);
                }

                //Наименование события и возраст
                if (widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_EventCaption)) {
                    sbDetails.append(singleEventArray[Position_eventCaption]);
                }
                if (!singleEventArray[Position_age_caption].trim().isEmpty() && widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_Age)
                        : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_Age)) {
                    if (widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_EventCaption)) sbDetails.append(STRING_COLON_SPACE);
                    sbDetails.append(singleEventArray[Position_age_caption]);
                }

                //Знак зодиака и животное в восточном календаре
                String eventSubType = singleEventArray[Position_eventSubType];

                if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_BirthDay)) || eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_5K))) {

                    String strZodiacInfo = STRING_EMPTY;
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_ZodiacSign)
                            : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_ZodiacSign)) {
                        strZodiacInfo = singleEventArray[Position_zodiacSign].trim();
                    }

                    String strZodiacYearInfo = STRING_EMPTY;
                    if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_ZodiacYear)
                            : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_ZodiacYear)) {
                        strZodiacYearInfo = singleEventArray[Position_zodiacYear].trim();
                    }

                    if (!strZodiacInfo.isEmpty() || !strZodiacYearInfo.isEmpty()) {
                        if (sbDetails.length() > 0) sbDetails.append(HTML_BR);
                        sbDetails.append((strZodiacInfo.concat(STRING_SPACE).concat(strZodiacYearInfo)).trim());
                    }
                }

                //Срок до события и день недели
                final String eventDistanceText = singleEventArray[ContactsEvents.Position_eventDistanceText];
                final boolean showDistance = widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_DaysBeforeEvent);
                final boolean showDayOfWeek = widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_EventDayOfWeek);
                final int barIndex = eventDistanceText.indexOf(STRING_BAR);
                if ((showDistance || showDayOfWeek) && barIndex > -1) {
                    if (sbDetails.length() > 0) sbDetails.append(HTML_BR);

                    if (showDistance && (!showDayOfWeek || singleEventArray[ContactsEvents.Position_eventDistance].equals(STRING_0))) {
                        sbDetails.append(colorDate != null ? String.format(HTML_COLOR, colorDate, eventDistanceText.substring(0, barIndex)).concat(HTML_FONT_END) : eventDistanceText);
                    } else if (showDistance) {
                        sbDetails.append(colorDate != null ? String.format(HTML_COLOR, colorDate, eventDistanceText.replace(STRING_BAR, STRING_SPACE)).concat(HTML_FONT_END) : eventDistanceText);
                    } else {
                        sbDetails.append(colorDate != null ? String.format(HTML_COLOR, colorDate, eventDistanceText.substring(barIndex + 1)).concat(HTML_FONT_END) : eventDistanceText);
                    }
                }

                views.setTextViewText(R.id.eventDetails, HtmlCompat.fromHtml(sbDetails.toString(), 0));

                //Фото
                views.setImageViewBitmap(R.id.eventPhoto, null);
                views.setViewVisibility(R.id.eventPhoto, View.GONE);
                Bitmap photo = null;
                if (widgetPref_eventInfo.isEmpty() ? eventsData.preferences_widgets_event_info.contains(ContactsEvents.pref_Widgets_EventInfo_Photo)
                        : widgetPref_eventInfo.contains(ContactsEvents.pref_Widgets_EventInfo_Photo)) {
                    photo = eventsData.getContactPhoto(eventInfo, true, true, true);
                }
                if (photo != null) {
                    int outWidth;
                    int minWidth = 0;
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
            clickIntent.putExtra(EXTRA_CLICKED_EVENT, eventInfo);
            clickIntent.putExtra(EXTRA_CLICKED_PREFS, eventsData.preferences_widgets_on_click_action);
            views.setOnClickFillInIntent(R.id.eventEntry, clickIntent);

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) handler.post(() -> Toast.makeText(context, Constants.EVENT_PHOTO_LIST_DATA_PROVIDER_GETVIEWAT_ERROR + e, Toast.LENGTH_LONG).show());
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
            widgetPref_eventInfo = new ArrayList<>();
            if (widgetPref.size() > 4 && !widgetPref.get(4).isEmpty()) {
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
