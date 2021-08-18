/*
 * *
 *  * Created by Vladimir Belov on 17.08.2021, 10:49
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 11.08.2021, 22:23
 *
 */

package org.vovka.birthdaycountdown;

import android.util.Log;
import android.widget.Toast;

public class ToastExpander {

    public static final String TAG = "ToastExpander";

    public static void showFor(final Toast aToast, final long durationInMilliseconds) {

        aToast.setDuration(Toast.LENGTH_SHORT);

        Thread t = new Thread() {
            long timeElapsed = 0;

            public void run() {
                try {
                    while (timeElapsed <= durationInMilliseconds) {
                        long start = System.currentTimeMillis();
                        aToast.show();
                        //noinspection BusyWait
                        sleep(1750);
                        timeElapsed += System.currentTimeMillis() - start;
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                }
            }
        };
        t.start();
    }

}
