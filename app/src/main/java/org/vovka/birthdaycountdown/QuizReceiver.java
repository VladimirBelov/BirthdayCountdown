/*
 * *
 *  * Created by Vladimir Belov on 07.01.2026, 01:04
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 07.01.2026, 01:00
 *
 */

package org.vovka.birthdaycountdown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * QuizReceiver - это BroadcastReceiver, который прослушивает широковещательные сообщения с ответами викторинами (если викторина включена в режиме уведомлений).
 * Он получает вопросы и ответы викторины, а затем обрабатывает их, используя класс ContactsEvents.
 */
public class QuizReceiver extends BroadcastReceiver {

    private static final String TAG = "QuizReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        try {

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(context);

            Bundle extras = intent.getExtras();
            String quizQuestion = null;
            String quizAnswer = null;
            if (extras != null) {
                quizQuestion = extras.getString(Constants.EXTRA_QUIZ_QUESTION, Constants.STRING_EMPTY);
                quizAnswer = extras.getString(Constants.EXTRA_QUIZ_RESULT, Constants.STRING_EMPTY);
            }

            eventsData.quizCheckAndGo(quizQuestion, quizAnswer, null);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
