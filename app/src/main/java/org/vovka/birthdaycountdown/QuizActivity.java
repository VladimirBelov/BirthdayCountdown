/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 25.04.2023, 10:18
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class QuizActivity extends Activity {

    private static final String TAG = "QuizActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();
            eventsData.setLocale(true);

            if (eventsData.getEvents(null)) {
                eventsData.computeDates();
                eventsData.quizCheckAndGo(null, null);
            }

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
