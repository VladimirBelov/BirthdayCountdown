/*
 * *
 *  * Created by Vladimir Belov on 18.03.2022, 9:04
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 18.03.2022, 9:04
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

// https://habr.com/ru/post/111961/
// https://stackoverflow.com/questions/47917200/android-custom-suggestions-wont-show-up

public class SuggestionProvider extends ContentProvider{

    private static final String TAG = "SuggestionProvider";
    ContactsEvents eventsData;

    @Override
    public boolean onCreate() {
        eventsData = ContactsEvents.getInstance();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
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

            if (!eventsData.isUIopen && query != null) {

                final String queryString = query.toLowerCase();

                //Получаем данные
                eventsData = ContactsEvents.getInstance();
                if (eventsData.getContext() == null) eventsData.setContext(getContext().getApplicationContext());

                if (eventsData.isEmptyEventList()) {
                    if (eventsData.getEvents(null)) eventsData.computeDates();
                }

                if (!eventsData.isEmptyEventList()) {
                    int eventNum = -1;
                    for (String event : eventsData.eventList) {
                        eventNum++;
                        if (event != null && event.toLowerCase().contains(queryString)) {
                            String[] singleEventArray = event.split(Constants.STRING_EOT, -1);

                            if (eventsData.checkIsHiddenEvent(eventsData.getEventKey(singleEventArray))) {
                                eventNum--;
                            } else {
                                final String[] eventDistance = singleEventArray[ContactsEvents.Position_eventDistanceText].split(Constants.STRING_PIPE, -1);
                                matrixCursor.addRow(new Object[]{
                                        (long) eventNum,
                                        singleEventArray[ContactsEvents.Position_personFullName],
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
                                        Integer.toString(eventNum).concat(Constants.STRING_EOT).concat(singleEventArray[ContactsEvents.Position_personFullName]).concat(Constants.STRING_EOT)
                                });
                            }
                        }
                    }

                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return matrixCursor;
    }

    @Override
    public String getType(Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}