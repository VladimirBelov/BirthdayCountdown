/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:07
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * QuizReceiver - это BroadcastReceiver, который прослушивает широковещательные сообщения, связанные с викторинами.
 * Он получает вопросы и ответы викторины, а затем обрабатывает их, используя класс ContactsEvents.
 */
public class QuizReceiver extends BroadcastReceiver {

    private static final String TAG = "QuizReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
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
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
