package org.vovka.birthdaycountdown.helper;

import android.os.Looper;

public final class ThreadHelper {
    private ThreadHelper() {}

    public static void verifyMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
        }
    }
}