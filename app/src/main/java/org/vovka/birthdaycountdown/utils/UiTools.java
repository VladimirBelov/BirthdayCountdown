/*
 * *
 *  * Created by Vladimir Belov on 26.12.2025, 20:59
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.12.2025, 16:20
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import androidx.annotation.NonNull;

public class UiTools {
    public static void addClickEffect(@NonNull View view) {
        Drawable drawableNormal = view.getBackground();

        if (view.getBackground().getConstantState() != null) {
            Drawable drawablePressed = view.getBackground().getConstantState().newDrawable();
            drawablePressed.mutate();
            drawablePressed.setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);

            StateListDrawable listDrawable = new StateListDrawable();
            listDrawable.addState(new int[]{android.R.attr.state_pressed}, drawablePressed);
            listDrawable.addState(new int[]{}, drawableNormal);
            view.setBackground(listDrawable);
        }
    }
}
