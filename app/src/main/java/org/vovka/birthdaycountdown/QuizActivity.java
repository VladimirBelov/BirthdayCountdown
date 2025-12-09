/*
 * *
 *  * Created by Vladimir Belov on 09.12.2025, 03:04
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 09.12.2025, 02:48
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * QuizActivity отвечает за запуск викторины из иконки приложения
 */
//todo: переделать под диалоговую активность
public class QuizActivity extends Activity {

    private static final String TAG = "QuizActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);

            if (eventsData.needUpdateEventList || eventsData.isEmptyEventList()) {
                eventsData.getEvents();
            }
            eventsData.quizCheckAndGo(null, null, null);

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

    }
}
