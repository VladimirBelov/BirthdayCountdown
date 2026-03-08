/*
 * *
 *  * Created by Vladimir Belov on 08.03.2026, 21:23
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 08.03.2026, 17:50
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import androidx.annotation.NonNull;

import org.vovka.birthdaycountdown.R;

public class UiTools {
    private static final int TAG_ORIGINAL_DRAWABLE = R.id.tag_original_drawable;
    public static void addClickEffect(@NonNull View view) {

        // 👇 Получаем оригинальный drawable из Tag или из текущего фона
        Drawable originalDrawable;
        if (view.getTag(TAG_ORIGINAL_DRAWABLE) instanceof Drawable) {
            originalDrawable = (Drawable) view.getTag(TAG_ORIGINAL_DRAWABLE);
        } else {
            originalDrawable = view.getBackground();
            // Сохраняем оригинал в Tag для будущих вызовов
            if (originalDrawable != null && originalDrawable.getConstantState() != null) {
                view.setTag(TAG_ORIGINAL_DRAWABLE, originalDrawable);
            }
        }

        if (originalDrawable == null || originalDrawable.getConstantState() == null) {
            return; // не можем создать эффект
        }

        // Создаём pressed-состояние из ОРИГИНАЛА (не из обёртки!)
        Drawable drawablePressed = originalDrawable.getConstantState().newDrawable();
        drawablePressed.mutate();
        //drawablePressed.setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        drawablePressed.setColorFilter(Color.argb(100, 128, 128, 128), PorterDuff.Mode.MULTIPLY);

        // Создаём новый StateListDrawable
        StateListDrawable listDrawable = new StateListDrawable();
        listDrawable.addState(new int[]{android.R.attr.state_pressed}, drawablePressed);
        listDrawable.addState(new int[]{}, originalDrawable);

        // Применяем
        view.setBackground(listDrawable);
    }
}
