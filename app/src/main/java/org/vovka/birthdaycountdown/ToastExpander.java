/*
 * *
 *  * Created by Vladimir Belov on 12.03.2025, 19:58
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 12.03.2025, 17:31
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

import java.util.LinkedHashSet;
import java.util.Set;

public class ToastExpander {

    public static final String TAG = "ToastExpander";
    private static final ToastExpander ourInstance = new ToastExpander();
    @Nullable
    FluentSnackbar mFluentSnackbar;
    private static final int msgTypeDebug = 1;
    private static final int msgTypeInfo = 2;
    private static final int TOAST_SHOW_INTERVAL_MS = 1750;

    @NonNull
    static ToastExpander getInstance() {
        return ourInstance;
    }

    public static void showFor(final Toast aToast, final long durationInMilliseconds) {

        if (durationInMilliseconds <= 0) return;

        aToast.setDuration(Toast.LENGTH_SHORT);

        new Thread(() -> {
            long timeElapsed = 0;

                try {
                    while (timeElapsed <= durationInMilliseconds) {
                        long start = System.currentTimeMillis();
                        aToast.show();
                        Thread.sleep(TOAST_SHOW_INTERVAL_MS);
                        timeElapsed += System.currentTimeMillis() - start;
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, e.toString());
                    Thread.currentThread().interrupt();
                }
        }).start();
    }

    private synchronized void showText(@NonNull Context context, @NonNull String msg, int type) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContactsEvents eventsData = ContactsEvents.getInstance();
            try {
                if (eventsData.isUIOpen && eventsData.coordinator != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (mFluentSnackbar == null) {
                            mFluentSnackbar = FluentSnackbar.create(ContactsEvents.getInstance().coordinator);
                        }

                        @ColorInt int colorBack = 0;
                        @ColorInt int colorAction = 0;
                        try {
                            TypedArray ta = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
                            colorBack = ta.getColor(R.styleable.Theme_colorPrimary, 0);
                            colorAction = ta.getColor(R.styleable.Theme_windowTitleColor, 0);
                        } catch (Resources.NotFoundException ignored) { /**/ }

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
                                    } else if (type == msgTypeInfo) {
                                        eventsData.disableInfoMsg();
                                    }
                                    mFluentSnackbar.removeAllMessagesByType(type);
                                })
                                .type(type)
                                .show();
                    });
                } else {
                    showToast(context, msg);
                }
            } catch (IllegalArgumentException e) {
                showToast(context, msg);
            }
        } else {
            showToast(context, msg);
        }
    }

    private void showToast(@NonNull Context context, @NonNull String msg){
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show());
    }

    static public void showInfoMsg(@NonNull Context context, @NonNull String msg) {
        /*try {
            Log.i(Thread.currentThread().getStackTrace()[3].getMethodName(), msg);
        } catch (Exception ignored) { *//**//* }*/
        if (ContactsEvents.getInstance().preferences_info_on) getInstance().showText(context, removeDuplicateLines(msg), msgTypeInfo);
    }

    static public void showMsg(@NonNull Context context, @NonNull String msg) {
        /*try {
            Log.i(Thread.currentThread().getStackTrace()[3].getMethodName(), msg);
        } catch (Exception ignored) { *//**//* }*/
        getInstance().showText(context, removeDuplicateLines(msg), msgTypeInfo);
    }

    static public void showDebugMsg(@NonNull Context context, @NonNull String msg) {
        /*try {
            Log.i(Thread.currentThread().getStackTrace()[3].getMethodName(), msg);
        } catch (Exception ignored) { *//**//* }*/
        if (ContactsEvents.getInstance().preferences_debug_on) getInstance().showText(context, removeDuplicateLines(msg), msgTypeDebug);
    }

    void dismissSnackBar() {
        if (mFluentSnackbar != null) mFluentSnackbar = null;
    }

    private static String removeDuplicateLines(String inputString) {
        if (inputString == null || inputString.isEmpty()) {
            return Constants.STRING_EMPTY;
        }

        String[] lines = inputString.split(Constants.STRING_EOL_RN, -1);
        Set<String> uniqueLines = new LinkedHashSet<>();
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (uniqueLines.add(trimmedLine)) {
                result.append(trimmedLine).append(Constants.STRING_EOL);
            }
        }

        if (result.length() > 0 && result.lastIndexOf(Constants.STRING_EOL) == result.length() - Constants.STRING_EOL.length()) {
            result.delete(result.length() - Constants.STRING_EOL.length(), result.length());
        }

        return result.toString();
    }
}