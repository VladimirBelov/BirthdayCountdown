/*
 * *
 *  * Created by Vladimir Belov on 30.06.2021, 13:04
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 30.06.2021, 12:43
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import static org.vovka.birthdaycountdown.Constants.EXTRA_QUIZ_QUESTION;
import static org.vovka.birthdaycountdown.Constants.EXTRA_QUIZ_RESULT;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

public class QuizReceiver extends BroadcastReceiver {

    private ContactsEvents eventsData;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) eventsData.context = context;

            Bundle extras = intent.getExtras();
            String quizQuestion = null;
            String quizAnswer = null;
            if (extras != null) {
                quizQuestion = extras.getString(EXTRA_QUIZ_QUESTION, STRING_EMPTY);
                quizAnswer = extras.getString(EXTRA_QUIZ_RESULT, STRING_EMPTY);
            }

            eventsData.quizCheckAndGo(quizQuestion, quizAnswer);

        } catch (Exception e) {
            e.printStackTrace();
            if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.QUIZ_RECEIVER_ON_RECEIVE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
