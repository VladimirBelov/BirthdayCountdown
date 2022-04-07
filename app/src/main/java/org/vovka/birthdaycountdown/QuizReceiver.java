/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 22.11.2021, 22:34
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.EXTRA_QUIZ_QUESTION;
import static org.vovka.birthdaycountdown.Constants.EXTRA_QUIZ_RESULT;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class QuizReceiver extends BroadcastReceiver {

    private ContactsEvents eventsData;

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(context);

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
            if (eventsData.preferences_debug_on) Toast.makeText(context, Constants.QUIZ_RECEIVER_ON_RECEIVE_ERROR + e, Toast.LENGTH_LONG).show();
        }
    }
}
