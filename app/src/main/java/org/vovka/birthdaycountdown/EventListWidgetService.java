/*
 * *
 *  * Created by Vladimir Belov on 30.06.2021, 12:42
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 24.06.2021, 23:24
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
