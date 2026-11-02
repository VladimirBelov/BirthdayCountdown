/*
 * *
 *  * Created by Vladimir Belov on 03.11.2026, 00:26
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 02.11.2026, 23:02
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Application;

import org.vovka.birthdaycountdown.utils.DeviceTools;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        int iconSizePx = (int) (32 * getResources().getDisplayMetrics().density + 0.5f);
        DeviceTools.preloadAppActionIcons(this, iconSizePx);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Вызываем shutdown у singleton при завершении приложения
        // Это происходит только в условиях отладки или если приложение полностью убирается из памяти системой.
        ContactsEvents.getInstance().shutdown();
    }

}
