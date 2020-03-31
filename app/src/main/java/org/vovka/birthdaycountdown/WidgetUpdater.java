/*
 * *
 *  * Created by Vladimir Belov on 22.03.20 23:03
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 22.03.20 22:07
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
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

import static org.vovka.birthdaycountdown.Constants.STRING_0;
import static org.vovka.birthdaycountdown.Constants.STRING_1;
import static org.vovka.birthdaycountdown.Constants.STRING_2;
import static org.vovka.birthdaycountdown.Constants.STRING_3;
import static org.vovka.birthdaycountdown.Constants.STRING_4;
import static org.vovka.birthdaycountdown.Constants.STRING_5;
import static org.vovka.birthdaycountdown.Constants.STRING_6;
import static org.vovka.birthdaycountdown.Constants.STRING_7;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_contact_id;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventType;
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
    private DisplayMetrics displayMetrics;
    private String packageName;
    private double fontMagnify;
    private int colorDefault;
    private int colorEventToday;
    private int colorEventSoon;
    private int eventsHidden;
    private int eventsToShow;

    WidgetUpdater(@NonNull Context context, @NonNull ContactsEvents eventsData, @NonNull RemoteViews views, int eventsCount, int width, int height, int widgetId) {
        this.context = context;
        this.eventsData = eventsData;
        this.views = views;
        this.eventsCount = eventsCount > 7 ? 7 : eventsCount > 0 ? eventsCount : 1;
        this.width = width;
        this.height = height;
        this.widgetId = widgetId;
    }

    void invoke() {
        //По нажатию на виджет открываем основное окно
        //http://flowovertop.blogspot.com/2013/04/android-widget-with-button-click-to.html
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Constants.ACTION_LAUNCH);
        views.setOnClickPendingIntent(R.id.appwidget_main, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Получаем данные
        boolean canReadContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if (canReadContacts && (eventsData.isEmptyArray() || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000)) {
            eventsData.context = context;
            if (eventsData.getContactsEvents(context)) eventsData.computeDates();
        }

        //Отрисовываем события
        try {

            //Скрываем все события
            resources = context.getResources();
            displayMetrics = resources.getDisplayMetrics();
            packageName = context.getPackageName();
            for (int e = 0; e < eventsCount; e++) {
                views.setViewVisibility(resources.getIdentifier(Constants.STRING_EVENT_INFO + e, Constants.STRING_ID, packageName), View.GONE);
            }

            //Получаем настройки отображения виджета
            List<String> widgetPref = eventsData.getWidgetPreference(widgetId);

            int startingIndex = 1;
            try {
                if (widgetPref.size() > 0) startingIndex = Integer.parseInt(widgetPref.get(0));
            } catch (Exception e) {/**/}

            if (!canReadContacts) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_access_contacts));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else if (eventsData.isEmptyArray() || eventsData.dataArray.length < startingIndex) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_events));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else {

                eventsToShow = Math.min(eventsCount, eventsData.dataArray.length);

                //Увеличение шрифтов в зависимости от размеров окна
                fontMagnify = 1;
                int cells = getCellsForSize(Math.min(width, height));
                if (widgetPref.size() > 1 && !widgetPref.get(1).equals(STRING_0)) {
                    switch (widgetPref.get(1)) {
                        case STRING_1:
                            fontMagnify = cells * 0.75;
                            break;
                        case STRING_2:
                            fontMagnify = cells * 1.0;
                            break;
                        case STRING_3:
                            fontMagnify = cells * 1.2;
                            break;
                        case STRING_4:
                            fontMagnify = cells * 1.5;
                            break;
                        case STRING_5:
                            fontMagnify = cells * 1.75;
                            break;
                        case STRING_6:
                            fontMagnify = cells * 2.0;
                            break;
                    }
                } else {
                    fontMagnify = 1 + 1.0 * (cells - 1);
                }

                colorDefault = resources.getColor(R.color.white);
                colorEventToday = resources.getColor(resources.getIdentifier(eventsData.preferences_widgets_color_eventtoday, "color", packageName));
                colorEventSoon = resources.getColor(resources.getIdentifier(eventsData.preferences_widgets_color_eventsoon, "color", packageName));

                //Отрисовываем информацию о событиях
                eventsHidden = 0;
                for (int i = 0; i < (eventsToShow + eventsHidden); i++) {
                    eventsHidden = drawEvent(i, startingIndex);
                }

                views.setViewVisibility(R.id.appwidget_text, View.GONE);

                //Если события есть - рисуем бордюр, иначе - прозрачность
                //https://stackoverflow.com/questions/12523005/how-set-background-drawable-programmatically-in-android
                views.setInt(R.id.appwidget_main,"setBackgroundResource", eventsToShow > 0 && eventsData.preferences_widgets_event_info.contains(Constants.STRING_10) ? R.drawable.layout_bg : 0);

            }

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.WIDGET_UPDATER_INVOKE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private int drawEvent(int i, int startingIndex) {
        //Отрисовываем одно событие
        try {
            String event = eventsData.dataArray[i + startingIndex - 1];
            String[] singleRowArray = event.split(Constants.STRING_2HASH);
            if (eventsData.getHiddenEventsCount() == 0 || !eventsData.checkIsHiddenEvent(singleRowArray[Position_contact_id] + Constants.STRING_2HASH + singleRowArray[Position_eventType])) {
                Person person = new Person(context, event);
                int eventCell = i - eventsHidden;
                int visibleCell = 1; //2 - top left, 3 - top center, 5 - bottom left, 7 - bottom center

                //Под фото
                int id_widget_Caption_left = resources.getIdentifier(Constants.STRING_TEXT_VIEW + eventCell, Constants.STRING_ID, packageName);
                int id_widget_Caption_centered = resources.getIdentifier("textViewCentered" + eventCell, Constants.STRING_ID, packageName);

                switch (eventsData.preferences_widgets_bottom_info) {
                    case STRING_1: //Фамилия Имя Отчество
                        views.setTextViewText(id_widget_Caption_left, person.getFullName());
                        views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.INVISIBLE);
                        visibleCell*=2;
                        break;
                    case STRING_2: //Дата события
                        views.setTextViewText(id_widget_Caption_centered, singleRowArray[ContactsEvents.Position_eventDateText]);
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case STRING_3: //Фамилия И.О.
                        views.setTextViewText(id_widget_Caption_left, eventsData.getContactFullNameShort(Long.parseLong(singleRowArray[Position_contact_id])));
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
                        views.setTextViewText(id_widget_Caption_centered, eventsData.getContactFirstName(Long.parseLong(singleRowArray[Position_contact_id])));
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case STRING_6: //Фамилия
                        views.setTextViewText(id_widget_Caption_centered, eventsData.getContactLastName(Long.parseLong(singleRowArray[Position_contact_id])));
                        views.setViewVisibility(id_widget_Caption_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                        visibleCell*=3;
                        break;
                    case STRING_7: //Псевдоним (Имя)
                        views.setTextViewText(id_widget_Caption_centered, singleRowArray[Position_nickname].trim().length() > 0 ? singleRowArray[Position_nickname] :
                                eventsData.getContactFirstName(Long.parseLong(singleRowArray[Position_contact_id])));
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

                views.setTextViewTextSize(id_widget_Caption_centered, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify));
                views.setTextViewTextSize(id_widget_Caption_left, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify)); //todo: убрать размер в ресурсы

                //Под фото (верхний ряд)
                int id_widget_Caption2nd_left = resources.getIdentifier("textView2nd" + eventCell, Constants.STRING_ID, packageName);
                int id_widget_Caption2nd_centered = resources.getIdentifier("textView2ndCentered" + eventCell, Constants.STRING_ID, packageName);

                switch (eventsData.preferences_widgets_bottom_info_2nd) {
                    case STRING_1: //Фамилия Имя Отчество
                        views.setTextViewText(id_widget_Caption2nd_left, person.getFullName());
                        views.setViewVisibility(id_widget_Caption2nd_left, View.VISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.INVISIBLE);
                        visibleCell*=5;
                        break;
                    case STRING_2: //Дата события
                        views.setTextViewText(id_widget_Caption2nd_centered, singleRowArray[ContactsEvents.Position_eventDateText]);
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case STRING_3: //Фамилия И.О.
                        views.setTextViewText(id_widget_Caption2nd_left, eventsData.getContactFullNameShort(Long.parseLong(singleRowArray[Position_contact_id])));
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
                        views.setTextViewText(id_widget_Caption2nd_centered, eventsData.getContactFirstName(Long.parseLong(singleRowArray[Position_contact_id])));
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case STRING_6: //Фамилия
                        views.setTextViewText(id_widget_Caption2nd_centered, eventsData.getContactLastName(Long.parseLong(singleRowArray[Position_contact_id])));
                        views.setViewVisibility(id_widget_Caption2nd_left, View.INVISIBLE);
                        views.setViewVisibility(id_widget_Caption2nd_centered, View.VISIBLE);
                        visibleCell*=7;
                        break;
                    case STRING_7: //Псевдоним (Имя)
                        views.setTextViewText(id_widget_Caption2nd_centered, singleRowArray[Position_nickname].trim().length() > 0 ? singleRowArray[Position_nickname] :
                                eventsData.getContactFirstName(Long.parseLong(singleRowArray[Position_contact_id])));
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

                views.setTextViewTextSize(id_widget_Caption2nd_centered, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify));
                views.setTextViewTextSize(id_widget_Caption2nd_left, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify));

                //Фото
                // todo: сделать закругления углов фото https://stackoverflow.com/questions/2459916/how-to-make-an-imageview-with-rounded-corners
                int id_widget_Photo = resources.getIdentifier("imageView" + eventCell, Constants.STRING_ID, packageName);

                Bitmap photo = eventsData.getContactPhoto(event, eventsData.preferences_widgets_event_info.contains(STRING_1), true);
                if (photo != null) {
                    if (eventsToShow == 1) {
                        views.setImageViewBitmap(id_widget_Photo, photo);
                    } else {
                        //потому что вот: https://stackoverflow.com/questions/13494898/remoteviews-for-widget-update-exceeds-max-bitmap-memory-usage-error
                        Bitmap bm_small = Bitmap.createScaledBitmap(photo, 2*width/eventsToShow, (2*photo.getHeight()*width)/(photo.getWidth()*eventsToShow) , true);
                        photo.recycle();
                        views.setImageViewBitmap(id_widget_Photo, bm_small);
                    }
                }
                //views.setInt(id_widget_Photo, "setBackgroundResource", R.drawable.selection_rectangle); //не работает

                //Определяем иконку события
                int id_widget_EventIcon = resources.getIdentifier("iconEventType" + eventCell, Constants.STRING_ID, packageName);
                int id_widget_Age = resources.getIdentifier("textViewAge" + eventCell, Constants.STRING_ID, packageName);

                if (eventsData.preferences_widgets_event_info.contains(STRING_2)) {

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
                    //if (width != -1) {//Если это масштабируемый виджет, приходится делать padding
                    if (eventsCount == 1) {
                        views.setViewPadding(id_widget_Age, 0, convertDipToPixels(2, displayMetrics), 0, 0);
                    } else {
                        views.setViewPadding(id_widget_Age, 0, convertDipToPixels(1, displayMetrics), 0, 0);
                    }
                    //}

                } else {

                    views.setViewVisibility(id_widget_EventIcon, View.GONE);
                    //if (width != -1) {//Если это масштабируемый виджет, приходится делать padding
                    if (eventsCount == 1) {
                        views.setViewPadding(id_widget_Age, convertDipToPixels(1, displayMetrics), 0, 0, 0);
                    } else {
                        views.setViewPadding(id_widget_Age, convertDipToPixels(eventCell == 0 ? 4 : 2, displayMetrics), convertDipToPixels(1, displayMetrics), 0, 0);
                    }
                    //}
                }

                //Цвета по-умолчанию
                views.setTextColor(id_widget_Age, colorDefault);
                views.setTextColor(id_widget_Caption_left, colorDefault);
                views.setTextColor(id_widget_Caption_centered, colorDefault);
                views.setTextColor(id_widget_Caption2nd_left, colorDefault);
                views.setTextColor(id_widget_Caption2nd_centered, colorDefault);
                //}

                //Возраст
                views.setTextViewTextSize(id_widget_Age, TypedValue.COMPLEX_UNIT_SP, (float) (12 * fontMagnify)); //todo: убрать размер в настройку
                if (singleRowArray[Position_eventType].equals(Integer.toString(eventsData.event_types_id[4]))) {
                    views.setTextViewText(id_widget_Age, singleRowArray[ContactsEvents.Position_age_caption]);
                } else if (person.Age > -1) {
                    views.setTextViewText(id_widget_Age, Integer.toString(person.Age));
                } else {
                    views.setTextViewText(id_widget_Age, STRING_EMPTY);
                }

                views.setTextColor(resources.getIdentifier(Constants.STRING_TEXT_VIEW + eventCell, Constants.STRING_ID, packageName), colorDefault);
                //views.setInt(context.getResources().getIdentifier("textViewAge" + i, "id", context.getPackageName()),"setShadowColor", context.getResources().getColor(R.color.dark_gray));

                //Сколько осталось до события
                int idViewDistance = resources.getIdentifier("textViewDistance" + eventCell, Constants.STRING_ID, packageName);

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

                } else if (1 <= eventDistance_Days && eventDistance_Days <= eventsData.preferences_widgets_days_eventsoon) { //Скоро

                    if (colorEventSoon != 0)
                        views.setTextColor(idViewDistance, colorEventSoon);
                    views.setTextViewText(idViewDistance, eventDistance);
                    views.setTextViewTextSize(idViewDistance, TypedValue.COMPLEX_UNIT_SP, (float) (24 * fontMagnify));

                } else { //Попозже

                    views.setTextColor(idViewDistance, colorDefault);
                    views.setTextViewText(idViewDistance, eventDistance);
                    views.setTextViewTextSize(idViewDistance, TypedValue.COMPLEX_UNIT_SP, (float) ((Integer.parseInt(eventDistance) < 10 ? 18 : 12) * fontMagnify));

                }

                //Иконка фаворита
                int id_widget_FavIcon = resources.getIdentifier("iconFav" + eventCell, Constants.STRING_ID, packageName);
                views.setViewVisibility(id_widget_FavIcon, eventsData.preferences_widgets_event_info.contains(STRING_3) && singleRowArray[Position_starred].equals(STRING_1) ? View.VISIBLE : View.GONE);

                //Если не последнее событие - по нажатию на фото открываем карточку контакта
                if (eventsToShow > 1 && eventCell < (eventsToShow - 1)) {

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleRowArray[Position_contact_id]);
                    intent.setData(uri);
                    views.setOnClickPendingIntent(resources.getIdentifier(Constants.STRING_EVENT_INFO + eventCell, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

                } else {

                    Intent intent = new Intent(context, WidgetConfigureActivity.class);
                    intent.setAction(Constants.ACTION_LAUNCH);
                    intent.putExtra("appWidgetId", widgetId);
                    if (visibleCell % 2 == 0) views.setOnClickPendingIntent(resources.getIdentifier(Constants.STRING_TEXT_VIEW + eventCell, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intent, 0));
                    if (visibleCell % 3 == 0) views.setOnClickPendingIntent(resources.getIdentifier("textViewCentered" + eventCell, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intent, 0));
                    if (visibleCell % 5 == 0) views.setOnClickPendingIntent(resources.getIdentifier("textView2nd" + eventCell, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intent, 0));
                    if (visibleCell % 7 == 0) views.setOnClickPendingIntent(resources.getIdentifier("textView2ndCentered" + eventCell, Constants.STRING_ID, packageName), PendingIntent.getActivity(context, widgetId, intent, 0));

                }

                //Показываем событие
                views.setViewVisibility(resources.getIdentifier(Constants.STRING_EVENT_INFO + eventCell, Constants.STRING_ID, packageName), View.VISIBLE);

            } else
                return eventsHidden + 1;

        } catch (Resources.NotFoundException | NullPointerException e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.WIDGET_UPDATER_DRAW_EVENT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
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

    private static int convertDipToPixels(float dips, @NonNull DisplayMetrics displayMetrics)
    {
        return (int) (dips * displayMetrics.density + 0.5f);
    }
}
