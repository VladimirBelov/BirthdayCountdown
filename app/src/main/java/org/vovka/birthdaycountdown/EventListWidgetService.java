/*
 * *
 *  * Created by Vladimir Belov on 30.06.2021, 13:04
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 30.06.2021, 12:43
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
