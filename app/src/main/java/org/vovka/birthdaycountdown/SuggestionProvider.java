/*
 * *
 *  * Created by Vladimir Belov on 07.01.2026, 16:55
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 07.01.2026, 15:55
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

import org.vovka.birthdaycountdown.utils.StringUtils;

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
        if (getContext() != null) {
            eventsData.initLanguage(getContext());
        }

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

            if (query == null) return matrixCursor;

            final String queryString = StringUtils.normalizeString(query);
            if (!StringUtils.hasContent(queryString)) return matrixCursor;

            if (eventsData.isEmptyEventList()) {
                eventsData.getEvents();
                if (eventsData.isEmptyEventList()) return matrixCursor;
            }

            int eventNum = -1;
            for (String event : eventsData.eventList) {
                eventNum++;
                final String eventDataNormalized = StringUtils.normalizeString(event);
                if (eventDataNormalized == null || !eventDataNormalized.contains(queryString)) {
                    continue;
                }

                String[] singleEventArray = event.split(Constants.STRING_EOT, -1);
                String eventKey = eventsData.getEventKey(singleEventArray);
                String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                if (eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {
                    eventNum--; //Если событие скрыто, то оно будет скрыто и в основной активности со списком событий
                    continue;
                }
                final String primaryLine = eventsData.getFullName(singleEventArray);
                String secondaryLine = singleEventArray[ContactsEvents.Position_eventEmoji]
                        .concat(Constants.STRING_SPACE)
                        .concat(singleEventArray[ContactsEvents.Position_eventCaption])
                        .concat(Constants.STRING_COLON)
                        .concat(StringUtils.hasContent(singleEventArray[ContactsEvents.Position_age_caption]) ?
                                Constants.STRING_SPACE.concat(singleEventArray[ContactsEvents.Position_age_caption]) :
                                Constants.STRING_EMPTY
                        );
                final String eventDistanceText = singleEventArray[ContactsEvents.Position_eventDistanceText];
                if (eventDistanceText != null && eventDistanceText.contains(Constants.STRING_BAR)) {
                    final String eventDistance = StringUtils.substringBefore(eventDistanceText, Constants.STRING_BAR);
                    if (!eventDistance.isEmpty()) {
                        secondaryLine = secondaryLine.concat(Constants.STRING_SPACE)
                                .concat(eventDistance.toLowerCase());
                    }
                }
                final String icon = !(StringUtils.hasContent(singleEventArray[ContactsEvents.Position_photo_uri])
                        || singleEventArray[ContactsEvents.Position_photo_uri].equals(Constants.STRING_NULL)) ?
                        singleEventArray[ContactsEvents.Position_photo_uri] :
                        Constants.STRING_EMPTY;

                matrixCursor.addRow(new Object[]{
                        (long) eventNum,
                        primaryLine,
                        secondaryLine,
                        icon,
                        Integer.toString(eventNum).concat(Constants.STRING_EOT).concat(primaryLine).concat(Constants.STRING_EOT)
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate suggestions for query: " + query, e);
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