/*
 * *
 *  * Created by Vladimir Belov on 30.11.2025, 02:33
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 13.11.2025, 21:08
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Application;

public class App extends Application {

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Вызываем shutdown у singleton при завершении приложения
        // Это происходит только в условиях отладки или если приложение полностью убирается из памяти системой.
        ContactsEvents.getInstance().shutdown();
    }

}
