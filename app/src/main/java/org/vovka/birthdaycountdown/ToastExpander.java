/*
 * *
 *  * Created by Vladimir Belov on 02.04.21 12:53
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 02.04.21 12:53
 *
 *  https://stackoverflow.com/questions/2220560/can-an-android-toast-be-longer-than-toast-length-long
 *  https://github.com/vishalTrivedi88/Toast-Expander/blob/master/src/com/thirtymatches/toasted/ToastExpander.java
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
