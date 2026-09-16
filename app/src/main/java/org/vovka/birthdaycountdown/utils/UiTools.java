/*
 * *
 *  * Created by Vladimir Belov on 17.09.2026, 00:15
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 16.09.2026, 23:18
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.SeekBar;

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

    /**
     * Метод для связывания кнопок "+" и "-" с ползунком SeekBar.
     *
     * @param activity активность
     * @param seek     объект SeekBar, которым нужно управлять
     * @param minusId  R.id кнопки уменьшения
     * @param plusId   R.id кнопки увеличения
     */
    public static void setupSeekBarButtons(@NonNull android.app.Activity activity, @NonNull final SeekBar seek, int minusId, int plusId) {
        View btnMinus = activity.findViewById(minusId);
        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (!seek.isEnabled()) return;
                int progress = seek.getProgress();
                if (progress > 0) {
                    seek.setProgress(progress - 1);
                }
            });
        }

        View btnPlus = activity.findViewById(plusId);
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                if (!seek.isEnabled()) return;
                int progress = seek.getProgress();
                if (progress < seek.getMax()) {
                    seek.setProgress(progress + 1);
                }
            });
        }
    }

    /**
     * Метод для связывания кнопок "+" и "-" с ползунком SeekBar.
     *
     * @param view     view, который указан у AlertDialog как CustomTitle
     * @param seek     объект SeekBar, которым нужно управлять
     * @param minusId  R.id кнопки уменьшения
     * @param plusId   R.id кнопки увеличения
     */
    public static void setupSeekBarButtons(@NonNull View view, @NonNull final SeekBar seek, int minusId, int plusId) {
        View btnMinus = view.findViewById(minusId);
        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (!seek.isEnabled()) return;
                int progress = seek.getProgress();
                if (progress > 0) {
                    seek.setProgress(progress - 1);
                }
            });
        }

        View btnPlus = view.findViewById(plusId);
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                if (!seek.isEnabled()) return;
                int progress = seek.getProgress();
                if (progress < seek.getMax()) {
                    seek.setProgress(progress + 1);
                }
            });
        }
    }
}
