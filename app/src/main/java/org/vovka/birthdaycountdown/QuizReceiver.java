/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 25.06.2022, 1:08
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class QuizReceiver extends BroadcastReceiver {

    private static final String TAG = "QuizReceiver";
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
                quizQuestion = extras.getString(Constants.EXTRA_QUIZ_QUESTION, Constants.STRING_EMPTY);
                quizAnswer = extras.getString(Constants.EXTRA_QUIZ_RESULT, Constants.STRING_EMPTY);
            }

            eventsData.quizCheckAndGo(quizQuestion, quizAnswer);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            if (eventsData.preferences_debug_on) ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
