/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 26.12.2021, 1:01
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
