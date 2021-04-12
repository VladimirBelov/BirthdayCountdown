/*
 * *
 *  * Created by Vladimir Belov on 21.03.21 19:00
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 21.03.21 19:00
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class QuizActivity extends Activity {

    private ContactsEvents eventsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = getApplicationContext();
            eventsData.getPreferences();
            eventsData.setLocale(true);

            if (eventsData.getEvents(eventsData.context)) {
                eventsData.computeDates();
                eventsData.quizCheckAndGo(null, null);
            }

            finish();

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(this, Constants.QUIZ_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }
}
