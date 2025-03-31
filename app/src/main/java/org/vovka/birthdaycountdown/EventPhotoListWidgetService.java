/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 09:49
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Этот класс предоставляет сервис для заполнения виджета "Списковый виджет с фото" данными.
 * Он действует как мост между виджетом и источником данных. Он использует
 * реализацию {@link android.widget.RemoteViewsService.RemoteViewsFactory} для управления списком представлений,
 * которые отображаются в виджете.
 */
public class EventPhotoListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new EventPhotoListDataProvider(this, intent);
    }

}
