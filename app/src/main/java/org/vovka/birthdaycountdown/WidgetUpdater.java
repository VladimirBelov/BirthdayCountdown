/*
 * *
 *  * Created by Vladimir Belov on 17.12.19 8:42
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 17.12.19 2:26
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
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import static org.vovka.birthdaycountdown.ContactsEvents.Position_contact_id;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_eventType;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_starred;

class WidgetUpdater {
    final private Context context;
    final private ContactsEvents eventsData;
    final private RemoteViews views;
    final private int eventsCount;
    final private int width;
    final private int height;
    final private int widgetId;

    WidgetUpdater(@NonNull Context context, @NonNull ContactsEvents eventsData, @NonNull RemoteViews views, int eventsCount, int width, int height, int widgetId) {
        this.context = context;
        this.eventsData = eventsData;
        this.views = views;
        this.eventsCount = eventsCount;
        this.width = width;
        this.height = height;
        this.widgetId = widgetId;
    }

    void invoke() {
        //По нажатию на виджет открываем основное окно
        //http://flowovertop.blogspot.com/2013/04/android-widget-with-button-click-to.html
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("LAUNCH_ACTIVITY");
        views.setOnClickPendingIntent(R.id.appwidget_main, PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        //Получаем данные
        boolean canReadContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if (canReadContacts && (eventsData.isEmpty() || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000)) {
            eventsData.context = context;
            if (eventsData.getContactsEvents(context)) eventsData.computeDates();
        }

        //Отрисовываем события
        int eventsToShow;
        try {

            //Скрываем все события
            Resources resources = context.getResources();
            DisplayMetrics displayMetrics = resources.getDisplayMetrics();
            String packageName = context.getPackageName();
            for (int e = 0; e < eventsCount; e++) {
                views.setViewVisibility(resources.getIdentifier("eventInfo" + e, "id", packageName), View.GONE);
            }

            //Получаем настройки отображения виджета
            int startingIndex;
            String widgetPref_raw = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.widget_config_PrefName) + widgetId, context.getString(R.string.widget_config_defaultPref));
            String[] widgetPref = (widgetPref_raw.equals("") ? context.getString(R.string.widget_config_defaultPref) : widgetPref_raw).split(Constants.STRING_COMMA);
            //Toast.makeText(context, "Prefs for " + widgetId + " is " + widgetPref_raw, Toast.LENGTH_SHORT).show();
            try {
                startingIndex = Integer.parseInt(widgetPref[0]);
            } catch (Exception e) {
                startingIndex = 1;
            }

            if (!canReadContacts) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_access));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else if (eventsData.isEmpty() || eventsData.dataArray.length < startingIndex) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_events));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else {

                eventsToShow = Math.min(eventsCount, eventsData.dataArray.length);

                //Увеличение шрифтов в зависимости от размеров окна
                double fontMagnify = 1;
                int cells = getCellsForSize(Math.min(width, height));
                if (cells != 1) {
                    if (widgetPref.length > 1 && !widgetPref[1].equals("0")) {
                       switch (widgetPref[1]) {
                           case "1":
                               fontMagnify = cells * 0.75;
                               break;
                           case "2":
                               fontMagnify = cells * 1.0;
                               break;
                           case "3":
                               fontMagnify = cells * 1.2;
                               break;
                           case "4":
                               fontMagnify = cells * 1.5;
                               break;
                           case "5":
                               fontMagnify = cells * 1.75;
                               break;
                       }
                    } else if (eventsToShow == 1) {
                        fontMagnify = 1 + 1.0 * (cells - 1);
                    }
                }

                int colorDefault = resources.getColor(R.color.white);
                int colorEventToday = resources.getColor(resources.getIdentifier(eventsData.preferences_widgets_color_eventtoday, "color", packageName));
                int colorEventSoon = resources.getColor(resources.getIdentifier(eventsData.preferences_widgets_color_eventsoon, "color", packageName));

                //Отрисовываем информацию о событиях
                int eventsHidden = 0;
                for (int i = 0; i < (eventsToShow + eventsHidden); i++) {
                    try {
                        String event = eventsData.dataArray[i + startingIndex - 1];
                        String[] singleRowArray = event.split(Constants.STRING_2HASH);
                        if (!eventsData.checkIsHiddenEvents() || !eventsData.checkIsHiddenEvent(singleRowArray[Position_contact_id] + Constants.STRING_2HASH + singleRowArray[Position_eventType])) {
                            Person person = new Person(context, event);
                            int eventCell = i - eventsHidden;

                            //Под фото
                            int id_widget_Caption_left = resources.getIdentifier("textView" + eventCell, "id", packageName);
                            int id_widget_Caption_centered = resources.getIdentifier("textViewCentered" + eventCell, "id", packageName);

                            switch (eventsData.preferences_widgets_bottom_info) {
                                case "1": //Фамилия Имя Отчество
                                    views.setTextViewText(id_widget_Caption_left, person.getFullName());
                                    views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                                    views.setViewVisibility(id_widget_Caption_centered, View.GONE);
                                    break;
                                case "2": //Дата события
                                    views.setTextViewText(id_widget_Caption_centered, singleRowArray[ContactsEvents.Position_eventDateText]);
                                    views.setViewVisibility(id_widget_Caption_left, View.GONE);
                                    views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                                    break;
                                case "4": //Имя Отчество Фамилия
                                    views.setTextViewText(id_widget_Caption_left, person.getFullNameAlt());
                                    views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                                    views.setViewVisibility(id_widget_Caption_centered, View.GONE);
                                    break;
                                case "3": //Фамилия И.О.
                                    views.setTextViewText(id_widget_Caption_left, person.getFullNameShort());
                                    views.setViewVisibility(id_widget_Caption_left, View.VISIBLE);
                                    views.setViewVisibility(id_widget_Caption_centered, View.GONE);
                                    break;
                                case "5": //Имя
                                    views.setTextViewText(id_widget_Caption_centered, eventsData.getContactName(Long.parseLong(singleRowArray[Position_contact_id]), person.getFirstName()));
                                    views.setViewVisibility(id_widget_Caption_left, View.GONE);
                                    views.setViewVisibility(id_widget_Caption_centered, View.VISIBLE);
                                    break;
                                case "99": //Ничего
                                default:
                                    views.setTextViewText(id_widget_Caption_left, "");
                                    views.setViewVisibility(id_widget_Caption_left, View.GONE);
                                    views.setViewVisibility(id_widget_Caption_centered, View.GONE);
                                    break;
                            }
                            views.setTextViewTextSize(id_widget_Caption_centered, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify));
                            views.setTextViewTextSize(id_widget_Caption_left, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify)); //todo: убрать размер в ресурсы

                            //Фото
                            // todo: сделать закругления углов фото https://stackoverflow.com/questions/2459916/how-to-make-an-imageview-with-rounded-corners
                            int id_widget_Photo = resources.getIdentifier("imageView" + eventCell, "id", packageName);

                            Bitmap photo = eventsData.getContactPhoto(event, eventsData.preferences_widgets_contactsphotos, true);
                            if (photo != null) {
                                if (cells == 1) {
                                    views.setImageViewBitmap(id_widget_Photo, photo);
                                } else {
                                    //потому что вот: https://stackoverflow.com/questions/13494898/remoteviews-for-widget-update-exceeds-max-bitmap-memory-usage-error
                                    Bitmap bm_small = Bitmap.createScaledBitmap(photo, 120, 120, true);
                                    photo.recycle();
                                    views.setImageViewBitmap(id_widget_Photo, bm_small);
                                }
                            }
                            //views.setInt(id_widget_Photo, "setBackgroundResource", R.drawable.selection_rectangle); //не работает

                            //Определяем иконку события
                            int id_widget_EventIcon = resources.getIdentifier("iconEventType" + eventCell, "id", packageName);
                            int id_widget_Age = resources.getIdentifier("textViewAge" + eventCell, "id", packageName);

                            if (eventsData.preferences_widgets_eventicons) {

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
                            //}

                            //Возраст
                            views.setTextViewTextSize(id_widget_Age, TypedValue.COMPLEX_UNIT_SP, (float) (12 * fontMagnify)); //todo: убрать размер в настройку
                            if (singleRowArray[Position_eventType].equals(Integer.toString(eventsData.event_types_id[4]))) {
                                views.setTextViewText(id_widget_Age, singleRowArray[ContactsEvents.Position_age_caption]);
                            } else if (person.Age > -1) {
                                views.setTextViewText(id_widget_Age, Integer.toString(person.Age));
                            } else {
                                views.setTextViewText(id_widget_Age, "");
                            }

                            views.setTextColor(resources.getIdentifier("textView" + eventCell, "id", packageName), colorDefault);
                            //views.setInt(context.getResources().getIdentifier("textViewAge" + i, "id", context.getPackageName()),"setShadowColor", context.getResources().getColor(R.color.dark_gray));

                            //Сколько осталось до события
                            int idViewDistance = resources.getIdentifier("textViewDistance" + eventCell, "id", packageName);

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
                                    }
                                }
                                views.setTextViewText(idViewDistance, "");

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
                            int id_widget_FavIcon = resources.getIdentifier("iconFav" + eventCell, "id", packageName);
                            views.setViewVisibility(id_widget_FavIcon, eventsData.preferences_list_fav_icon && singleRowArray[Position_starred].equals("1") ? View.VISIBLE : View.GONE);

                            //Если не последнее событие - по нажатию на фото открываем карточку контакта
                            if (eventsToShow > 1 && eventCell < (eventsToShow - 1)) {

                                intent = new Intent(Intent.ACTION_VIEW);
                                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleRowArray[Position_contact_id]);
                                intent.setData(uri);
                                views.setOnClickPendingIntent(resources.getIdentifier("eventInfo" + eventCell, "id", packageName), PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                            }

                            //Показываем событие
                            views.setViewVisibility(resources.getIdentifier("eventInfo" + eventCell, "id", packageName), View.VISIBLE);
                        } else eventsHidden++;

                    } catch (Resources.NotFoundException nfe) {
                        //todo: сделать вывод в лог
                    }
                }

                views.setViewVisibility(R.id.appwidget_text, View.GONE);

                //Если события есть - рисуем бордюр, иначе - прозрачность
                //https://stackoverflow.com/questions/12523005/how-set-background-drawable-programmatically-in-android
                views.setInt(R.id.appwidget_main,"setBackgroundResource", eventsToShow > 0 && eventsData.preferences_widgets_showborder ? R.drawable.layout_bg : 0);

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.WIDGET_UPDATER_INVOKE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
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
