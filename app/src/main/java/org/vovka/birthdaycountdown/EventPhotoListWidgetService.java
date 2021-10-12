/*
 * *
 *  * Created by Vladimir Belov on 12.10.2021, 0:19
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 12.10.2021, 0:16
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class EventPhotoListWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new EventPhotoListDataProvider(this, intent);
    }

}
