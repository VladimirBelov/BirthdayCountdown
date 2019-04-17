package org.vovka.birthdaycountdown;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;

@SuppressWarnings("ConstantConditions")
class WidgetUpdater {
    final private Context context;
    final private ContactsEvents eventsData;
    final private RemoteViews views;
    final private int eventsCount;
    final private int width;
    final private int height;
    final private int widgetId;

    WidgetUpdater(Context context, ContactsEvents eventsData, RemoteViews views, int eventsCount, int width, int height, int widgetId) {
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
        if (canReadContacts && (eventsData.dataArray == null || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000)) {
            eventsData.context = context;
            if (eventsData.getContactsEvents()) eventsData.computeDates();
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
            String[] widgetPref = (widgetPref_raw == null || widgetPref_raw.equals("") ? context.getString(R.string.widget_config_defaultPref) : widgetPref_raw).split(ContactsEvents.Div4);
            //Toast.makeText(context, "Prefs for " + widgetId + " is " + widgetPref_raw, Toast.LENGTH_SHORT).show();
            try {
                startingIndex = Integer.parseInt(widgetPref[0]);
            } catch (Exception e) {
                startingIndex = 1;
            }

            if (!canReadContacts) {

                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.msg_no_access));
                views.setViewVisibility(R.id.appwidget_text, View.VISIBLE);

            } else if (eventsData.dataArray == null || eventsData.dataArray.length == 0 || eventsData.dataArray.length < startingIndex) {

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
                for (int i = 0; i < eventsToShow; i++) {
                    try {
                        String[] singleRowArray = eventsData.dataArray[i + startingIndex - 1].split(ContactsEvents.Div1);
                        String eventType = singleRowArray[ContactsEvents.dataMap.get("eventType")];
                        Person person = new Person(context, eventsData.dataArray[i + startingIndex - 1]);

                        //Под фото
                        int id_widget_FIO = resources.getIdentifier("textView" + i, "id", packageName);
                        int id_widget_Date = resources.getIdentifier("textViewCentered" + i, "id", packageName);

                        switch (eventsData.preferences_widgets_bottom_info) {
                            case "1": //Фамилия Имя Отчество
                                views.setTextViewText(id_widget_FIO, person.getFullName());
                                views.setViewVisibility(id_widget_FIO, View.VISIBLE);
                                views.setViewVisibility(id_widget_Date, View.GONE);
                                break;
                            case "2": //Дата события
                                views.setTextViewText(id_widget_Date, singleRowArray[ContactsEvents.dataMap.get("eventDate")]);
                                views.setViewVisibility(id_widget_FIO, View.GONE);
                                views.setViewVisibility(id_widget_Date, View.VISIBLE);
                                break;
                            case "4": //Имя Отчество Фамилия
                                views.setTextViewText(id_widget_FIO, person.getFullNameAlt());
                                views.setViewVisibility(id_widget_FIO, View.VISIBLE);
                                views.setViewVisibility(id_widget_Date, View.GONE);
                                break;
                            case "3": //Фамилия И.О.
                                views.setTextViewText(id_widget_FIO, person.getFullNameShort());
                                views.setViewVisibility(id_widget_FIO, View.VISIBLE);
                                views.setViewVisibility(id_widget_Date, View.GONE);
                                break;
                            case "99": //Ничего
                            default:
                                views.setTextViewText(id_widget_FIO, "");
                                views.setViewVisibility(id_widget_FIO, View.GONE);
                                views.setViewVisibility(id_widget_Date, View.GONE);
                                break;
                        }
                        views.setTextViewTextSize(id_widget_Date, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify));
                        views.setTextViewTextSize(id_widget_FIO, TypedValue.COMPLEX_UNIT_SP, (float) (10 * fontMagnify)); //todo: убрать размер в ресурсы

                        //Фото
                        // todo: сделать закругления углов фото https://stackoverflow.com/questions/2459916/how-to-make-an-imageview-with-rounded-corners
                        int id_widget_Photo = resources.getIdentifier("imageView" + i, "id", packageName);
                        if (eventsData.preferences_widgets_contactsphotos && !singleRowArray[ContactsEvents.dataMap.get("photo_uri")].equalsIgnoreCase("null")) {
                            //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                            Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleRowArray[ContactsEvents.dataMap.get("contact_id")]);
                            InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), contactUri, true);
                            BufferedInputStream buf = new BufferedInputStream(photo_stream);
                            Bitmap bm = BitmapFactory.decodeStream(buf);
                            buf.close();
                            photo_stream.close();
                            if (cells > 1) {
                                views.setImageViewBitmap(id_widget_Photo, bm);
                            } else {
                                //потому что вот: https://stackoverflow.com/questions/13494898/remoteviews-for-widget-update-exceeds-max-bitmap-memory-usage-error
                                Bitmap bm_small = Bitmap.createScaledBitmap(bm, 120, 120, true);
                                bm.recycle();
                                views.setImageViewBitmap(id_widget_Photo, bm_small);
                            }

                        } else {

                            //случайное фото с соответствиии с возрастом и полом

                            int idPhoto = R.drawable.photo_man01;
                            int growAge = 16;

                            String eventLabel = eventType.equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM)) ? singleRowArray[ContactsEvents.dataMap.get("eventLabel")].toLowerCase() : "#~#";
                            boolean notDeath = eventsData.preferences_death_labels == null || !eventsData.preferences_death_labels.reset(eventLabel.toLowerCase()).find();

                            if (person.getGender() == 1 && (person.Age >= 0 && person.Age < growAge) && notDeath) {
                                idPhoto = R.drawable.photo_boy01;
                            } else if (person.getGender() == 2 && (person.Age >= 0 && person.Age < growAge) && notDeath) {
                                idPhoto = R.drawable.photo_girl01;
                            } else if (person.getGender() == 2) {
                                idPhoto = R.drawable.photo_woman01;
                            }

                            views.setImageViewResource(id_widget_Photo, idPhoto);
                        }
                        //views.setInt(id_widget_Photo, "setBackgroundResource", R.drawable.selection_rectangle); //не работает

                        //Определяем иконку события
                        int id_widget_EventIcon = resources.getIdentifier("iconEventType" + i, "id", packageName);
                        int id_widget_Age = resources.getIdentifier("textViewAge" + i, "id", packageName);

                        if (eventsData.preferences_widgets_eventicons) {

                            int eventIcon;
                            try {
                                eventIcon = Integer.parseInt(singleRowArray[ContactsEvents.dataMap.get("eventIcon")]);
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
                                views.setViewPadding(id_widget_Age, convertDipToPixels(i == 0 ? 4 : 2, displayMetrics), convertDipToPixels(1, displayMetrics), 0, 0);
                            }
                            //}
                        }

                        //Цвета по-умолчанию
                        views.setTextColor(id_widget_Age, colorDefault);
                        views.setTextColor(id_widget_FIO, colorDefault);
                        views.setTextColor(id_widget_Date, colorDefault);
                        //}

                        //Возраст
                        views.setTextViewTextSize(id_widget_Age, TypedValue.COMPLEX_UNIT_SP, (float) (12 * fontMagnify)); //todo: убрать размер в настройку
                        if (eventType.equals(Integer.toString(eventsData.event_types_id[4]))) {
                            views.setTextViewText(id_widget_Age, singleRowArray[ContactsEvents.dataMap.get("age_caption")]);
                        } else if (person.Age > -1) {
                            views.setTextViewText(id_widget_Age, Integer.toString(person.Age));
                        } else {
                            views.setTextViewText(id_widget_Age, "");
                        }

                        views.setTextColor(resources.getIdentifier("textView" + i, "id", packageName), colorDefault);
                        //views.setInt(context.getResources().getIdentifier("textViewAge" + i, "id", context.getPackageName()),"setShadowColor", context.getResources().getColor(R.color.dark_gray));

                        //Сколько осталось до события
                        int idViewDistance = resources.getIdentifier("textViewDistance" + i, "id", packageName);

                        String eventDistance = singleRowArray[ContactsEvents.dataMap.get("eventDistance")];
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
                                    views.setTextColor(id_widget_FIO, colorEventToday);
                                    views.setTextColor(id_widget_Date, colorEventToday);
                                }
                            }
                            views.setTextViewText(idViewDistance, "");

                        } else if ( 1 <= eventDistance_Days && eventDistance_Days <= eventsData.preferences_widgets_days_eventsoon) { //Скоро

                                if (colorEventSoon != 0) views.setTextColor(idViewDistance, colorEventSoon);
                                views.setTextViewText(idViewDistance, eventDistance);
                                views.setTextViewTextSize(idViewDistance, TypedValue.COMPLEX_UNIT_SP, (float) (24 * fontMagnify));

                        } else { //Попозже

                                views.setTextColor(idViewDistance, colorDefault);
                                views.setTextViewText(idViewDistance, eventDistance);
                                views.setTextViewTextSize(idViewDistance, TypedValue.COMPLEX_UNIT_SP, (float) ((Integer.parseInt(eventDistance) < 10 ? 18 : 12) * fontMagnify));

                        }

                        //Если не последнее событие - по нажатию на фото открываем карточку контакта
                        if (eventsToShow > 1 && i < (eventsToShow - 1)) {

                            intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleRowArray[ContactsEvents.dataMap.get("contact_id")]);
                            intent.setData(uri);
                            views.setOnClickPendingIntent(resources.getIdentifier("eventInfo" + i, "id", packageName), PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                        }

                        //Показываем событие
                        views.setViewVisibility(resources.getIdentifier("eventInfo" + i, "id", packageName), View.VISIBLE);
                    } catch (Resources.NotFoundException nfe) {
                        //todo: сделать вывод в лог
                    }
                }

                views.setViewVisibility(R.id.appwidget_text, View.GONE);

                //Если события есть - рисуем бордюр, иначе - прозрачность
                //https://stackoverflow.com/questions/12523005/how-set-background-drawable-programmatically-in-android
                views.setInt(R.id.appwidget_main,"setBackgroundResource", eventsToShow > 0 ? R.drawable.layout_bg : 0);

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "WidgetUpdater->invoke error: " + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }
    }

    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < (size)) {
            ++n;
        }
        return n - 1;
    }

    private static int convertDipToPixels(float dips, DisplayMetrics displayMetrics)
    {
        return (int) (dips * displayMetrics.density + 0.5f);
    }
}
