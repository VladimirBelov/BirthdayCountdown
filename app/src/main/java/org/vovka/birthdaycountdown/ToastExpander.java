/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 07.09.2022, 21:36
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

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

    public static synchronized void showText(@NonNull Context context, @NonNull String text) {

        try {
            Log.i(Thread.currentThread().getStackTrace()[3].getMethodName(), text);
        } catch (Exception e) { /**/ }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContactsEvents eventsData = ContactsEvents.getInstance();
            try {
                if (eventsData.isUIopen && eventsData.coordinator != null) {
                    final Snackbar snackbar = Snackbar.make(eventsData.coordinator, text, Snackbar.LENGTH_LONG);
                    try {
                        TypedArray ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
                        snackbar.setBackgroundTint(ta.getColor(R.styleable.Theme_colorPrimary, 0));
                    } catch (Resources.NotFoundException e) {/**/}
                    snackbar.show();
                }
            } catch (IllegalArgumentException e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
            }
        } else {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
        }
    }

}
