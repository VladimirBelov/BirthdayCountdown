/*
 * *
 *  * Created by Vladimir Belov on 18.08.2023, 00:50
 *  * Copyright (c) 2018 - 2023. All rights reserved.
 *  * Last modified 30.07.2023, 12:41
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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ToastExpander {

    public static final String TAG = "ToastExpander";
    private static final ToastExpander ourInstance = new ToastExpander();
    @Nullable
    FluentSnackbar mFluentSnackbar;
    private static final int msgTypeDebug = 1;
    private static final int msgTypeInfo = 2;

    @NonNull
    static ToastExpander getInstance() {
        return ourInstance;
    }

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

    private synchronized void showText(@NonNull Context context, @NonNull String msg, int type) {

        try {
            Log.i(Thread.currentThread().getStackTrace()[3].getMethodName(), msg);
        } catch (Exception e) { /**/ }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContactsEvents eventsData = ContactsEvents.getInstance();
            try {
                if (eventsData.isUIOpen && eventsData.coordinator != null) {

                    if (mFluentSnackbar == null) {
                        mFluentSnackbar = FluentSnackbar.create(ContactsEvents.getInstance().coordinator);
                    }

                    @ColorInt int colorBack = 0;
                    @ColorInt int colorAction = 0;
                    try {
                        TypedArray ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
                        colorBack = ta.getColor(R.styleable.Theme_colorPrimary, 0);
                        colorAction = ta.getColor(R.styleable.Theme_windowTitleColor, 0);
                    } catch (Resources.NotFoundException e) { /**/ }

                    mFluentSnackbar
                            .create(msg)
                            .maxLines(8)
                            .backgroundColor(colorBack)
                            .important()
                            .actionText(context.getText(R.string.button_off).toString())
                            .actionTextColor(colorAction)
                            .action(v -> {
                                if (type == msgTypeDebug) {
                                    eventsData.disableDebugMsg();
                                    mFluentSnackbar.removeAllMessagesByType(type);
                                } else if (type == msgTypeInfo) {
                                    eventsData.disableInfoMsg();
                                    mFluentSnackbar.removeAllMessagesByType(type);
                                }
                            })
                            .type(type)
                            .show();
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show());
                }
            } catch (IllegalArgumentException e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show());
            }
        } else {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show());
        }
    }

    static public void showInfoMsg(@NonNull Context context, @NonNull String msg) {
        if (ContactsEvents.getInstance().preferences_info_on) getInstance().showText(context, msg.trim(), msgTypeInfo);
    }

    static public void showMsg(@NonNull Context context, @NonNull String msg) {
        getInstance().showText(context, msg.trim(), msgTypeInfo);
    }

    static public void showDebugMsg(@NonNull Context context, @NonNull String msg) {
        if (ContactsEvents.getInstance().preferences_debug_on) getInstance().showText(context, msg.trim(), msgTypeDebug);
    }

    void dismissSnackBar() {
        if (mFluentSnackbar != null) mFluentSnackbar = null;
    }

}
