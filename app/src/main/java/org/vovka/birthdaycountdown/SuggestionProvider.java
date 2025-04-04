/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:14
 *
 */

package org.vovka.birthdaycountdown;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.NonNull;

// https://habr.com/ru/post/111961/
// https://stackoverflow.com/questions/47917200/android-custom-suggestions-wont-show-up

/**
 * SuggestionProvider - это пользовательский ContentProvider, который предоставляет поисковые подсказки
 * для событий на основе пользовательского ввода. Он взаимодействует с классом {@link ContactsEvents} для
 * получения данных о событиях и форматирования их в структуру, подходящую для отображения в списке
 * поисковых подсказок.
 *
 * <p>
 *     Этот класс в первую очередь отвечает за:
 *     <ul>
 *         <li>Обработку поисковых запросов и генерацию списка подсказок.</li>
 *         <li>Возвращение объекта Cursor, который содержит данные подсказок.</li>
 *         <li>Определение MIME-типа для поисковых подсказок.</li>
 *     </ul>
 * </p>
 */
public class SuggestionProvider extends ContentProvider{

    private static final String TAG = "SuggestionProvider";
    ContactsEvents eventsData;

    @Override
    public boolean onCreate() {
        eventsData = ContactsEvents.getInstance();
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        eventsData = ContactsEvents.getInstance();
        if (eventsData.getContext() == null && getContext() != null) eventsData.setContext(getContext().getApplicationContext());

        //to protect from running twice
        if (System.currentTimeMillis() - eventsData.statLastSearchSuggestion < 500) return null;
        eventsData.statLastSearchSuggestion = System.currentTimeMillis();
        return selectionArgs == null ? null : getSuggestions(selectionArgs[0]);
    }

    private Cursor getSuggestions(String query) {

        String[] mColumnNames = {
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_ICON_1,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA};
        MatrixCursor matrixCursor = new MatrixCursor(mColumnNames);

        try {

            if (!eventsData.isUIOpen && query != null) {

                final String queryString = query.toLowerCase();

                //Получаем данные
                eventsData = ContactsEvents.getInstance();
                if (eventsData.getContext() == null && getContext() != null) eventsData.setContext(getContext().getApplicationContext());

                if (eventsData.isEmptyEventList()) {
                    eventsData.getPreferences();
                    eventsData.setLocale(true);
                    eventsData.getEvents(null);
                }

                if (!eventsData.isEmptyEventList()) {
                    int eventNum = -1;
                    for (String event : eventsData.eventList) {
                        eventNum++;
                        if (event != null && event.toLowerCase().contains(queryString)) {
                            String[] singleEventArray = event.split(Constants.STRING_EOT, -1);

                            String eventKey = eventsData.getEventKey(singleEventArray);
                            String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                            if (eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {
                                eventNum--;
                            } else {
                                final String[] eventDistance = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                                String fullName = eventsData.getFullName(singleEventArray);
                                matrixCursor.addRow(new Object[]{
                                        (long) eventNum,
                                        fullName,
                                        singleEventArray[ContactsEvents.Position_eventEmoji]
                                                .concat(Constants.STRING_SPACE)
                                                .concat(singleEventArray[ContactsEvents.Position_eventCaption])
                                                .concat(Constants.STRING_COLON)
                                                .concat(!singleEventArray[ContactsEvents.Position_age_caption].trim().isEmpty() ?
                                                        Constants.STRING_SPACE.concat(singleEventArray[ContactsEvents.Position_age_caption]) :
                                                        Constants.STRING_EMPTY
                                                )
                                                .concat(Constants.STRING_SPACE)
                                                .concat(eventDistance[0].toLowerCase()),
                                        !(singleEventArray[ContactsEvents.Position_photo_uri].trim().equals(Constants.STRING_EMPTY)
                                                || singleEventArray[ContactsEvents.Position_photo_uri].equals(Constants.STRING_NULL)) ?
                                                    singleEventArray[ContactsEvents.Position_photo_uri] :
                                                    Constants.STRING_EMPTY,
                                        Integer.toString(eventNum).concat(Constants.STRING_EOT).concat(fullName).concat(Constants.STRING_EOT)
                                });
                            }
                        }
                    }

                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return matrixCursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}