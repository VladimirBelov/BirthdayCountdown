/*
 * *
 *  * Created by Vladimir Belov on 18.03.2022, 9:04
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 18.03.2022, 9:04
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.STRING_COLON_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_EOT;
import static org.vovka.birthdaycountdown.Constants.STRING_NULL;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.ContactsEvents.Position_age_caption;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

// https://habr.com/ru/post/111961/
// https://stackoverflow.com/questions/47917200/android-custom-suggestions-wont-show-up

public class SuggestionProvider extends ContentProvider{

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
                            String[] singleEventArray = event.split(STRING_EOT, -1);

                            if (eventsData.checkIsHiddenEvent(eventsData.getEventKey(singleEventArray))) {
                                eventNum--;
                            } else {
                                matrixCursor.addRow(new Object[]{
                                        (long) eventNum,
                                        singleEventArray[ContactsEvents.Position_personFullName],
                                        singleEventArray[ContactsEvents.Position_eventEmoji]
                                                .concat(STRING_SPACE).concat(singleEventArray[ContactsEvents.Position_eventCaption])
                                                .concat(!singleEventArray[Position_age_caption].trim().isEmpty() ? STRING_COLON_SPACE.concat(singleEventArray[Position_age_caption]) : STRING_EMPTY),
                                        !(singleEventArray[ContactsEvents.Position_photo_uri].trim().equals(STRING_EMPTY)
                                                || singleEventArray[ContactsEvents.Position_photo_uri].equals(STRING_NULL)) ? singleEventArray[ContactsEvents.Position_photo_uri] : STRING_EMPTY,
                                        Integer.toString(eventNum).concat(STRING_EOT).concat(singleEventArray[ContactsEvents.Position_personFullName]).concat(STRING_EOT)
                                });
                            }
                        }
                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) ToastExpander.showText(eventsData.getContext(), Constants.SUGGESTIONPROVIDER_GETSUGGESTIONS_ERROR + e);

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