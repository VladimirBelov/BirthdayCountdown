/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 18.09.2022, 08:26
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
