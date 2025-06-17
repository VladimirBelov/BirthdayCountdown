/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 16.06.2025, 23:33
 *
 */

package org.vovka.birthdaycountdown.helpers;

import android.os.Looper;

public final class ThreadHelper {
    private ThreadHelper() {}

    public static void verifyMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
        }
    }
}