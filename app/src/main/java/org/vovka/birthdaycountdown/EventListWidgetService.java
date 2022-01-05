/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 22.11.2021, 22:34
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class EventListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new EventListDataProvider(this, intent);
    }

}
