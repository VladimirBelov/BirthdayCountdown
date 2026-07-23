/*
 * *
 *  * Created by Vladimir Belov on 23.07.2026, 14:22
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 23.07.2026, 13:42
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Этот класс предоставляет сервис для заполнения виджета {@link WidgetList} данными.
 * Он действует как мост между виджетом и источником данных. Он использует
 * реализацию {@link RemoteViewsFactory} для управления списком представлений,
 * которые отображаются в виджете.
 */
public class WidgetListService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetListDataProvider(this.getApplicationContext(), intent);
    }

}
