/*
 * *
 *  * Created by Vladimir Belov on 18.06.2025, 15:45
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 18.06.2025, 15:43
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.vovka.birthdaycountdown.helpers.ThreadHelper;

/**
 * Обертка с "текучим" (fluent) API для {@link Snackbar} из Android, упрощающая его использование и предоставляющая
 * дополнительные возможности настройки. Этот класс позволяет создавать и отображать Snackbar'ы с более
 * читаемым и цепочечным синтаксисом.
 *
 * <p>
 *     <b>Ключевые особенности:</b>
 *     <ul>
 *         <li><b>"Текучий" (Fluent) интерфейс:</b> Позволяет использовать цепочечный синтаксис для создания и настройки Snackbar'ов.</li>
 *         <li><b>Настройка:</b> Предоставляет широкие возможности для настройки текста, цветов, кнопок действий и поведения.</li>
 *         <li><b>Очередь сообщений:</b> Управляет очередью Snackbar'ов, обеспечивая плавные переходы и избегая наложения.</li>
 *         <li><b>Потокобезопасность:</b> Гарантирует, что все операции выполняются в основном потоке.</li>
 *         <li><b>Важность:</b> Позволяет пометить Snackbar как важный и получить обратный вызов при его закрытии.</li>
 *         <li><b>Тип:</b> Позволяет задать тип для Snackbar'а и удалить все Snackbar'ы определенного типа.</li>
 *     </ul>
 * </p>
 */
final class FluentSnackbar {
    private final View mView;

    private final org.vovka.birthdaycountdown.SnackbarHandler mSnackbarHandler;

    public static FluentSnackbar create(Activity activity) {
        ThreadHelper.verifyMainThread();

        return new FluentSnackbar(activity.findViewById(android.R.id.content));
    }

    public static FluentSnackbar create(View view) {
        ThreadHelper.verifyMainThread();

        return new FluentSnackbar(view);
    }

    private FluentSnackbar(View view) {
        mView = view;
        mSnackbarHandler = new SnackbarHandler(this);
    }

    private void putToMessageQueue(Builder builder) {
        Message message = mSnackbarHandler.obtainMessage(SnackbarHandler.MESSAGE_NEW, builder);

        mSnackbarHandler.sendMessage(message);
    }

    public void removeAllMessagesByType(int type) {
        mSnackbarHandler.setNotImportantByType(type);
    }

    void showSnackbar(Builder builder) {
        @SuppressLint("ShowToast") final Snackbar snackbar = Snackbar.make(mView, builder.getText(), builder.getDuration());
        snackbar.addCallback(builder.mSnackbarCallbackListener);
        View view = snackbar.getView();
        view.setBackgroundColor(builder.getBackgroundColor());
        view.setPadding(0, 0, 0, 0);

        TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setMaxLines(builder.getMaxLines());
        textView.setTextColor(builder.getTextColor());

        if (builder.hasAction()) {
            snackbar.setAction(builder.getActionText(), builder.getActionListener());

            if (builder.hasActionTextColor()) {
                snackbar.setActionTextColor(builder.getActionTextColor());
            } else if (builder.hasActionTextColors()) {
                snackbar.setActionTextColor(builder.getActionColors());
            }
        }

        if (builder.isImportant()) {
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    Message message = mSnackbarHandler.obtainMessage(SnackbarHandler.MESSAGE_DISMISSED);
                    mSnackbarHandler.sendMessage(message);
                }
            });
        }

        // This fix a case if you want the Snackbar to push up the fab/view instead of overlapping
        new Handler().postDelayed(snackbar::show, 100); // the delay does the trick
    }

    public Builder create(@StringRes int text) {
        return create(mView.getContext().getString(text));
    }

    public Builder create(String text) {
        return new Builder(text);
    }

    public class Builder {
        private final CharSequence mText;
        private int mMaxLines;
        @ColorInt
        private int mTextColor;
        @ColorInt
        private int mBackgroundColor;
        private boolean mIsImportant;
        private int mDuration;
        private CharSequence mActionText;
        private View.OnClickListener mActionListener;
        private Snackbar.Callback mSnackbarCallbackListener;
        @ColorInt
        private int mActionTextColor;
        private ColorStateList mActionColors;
        private boolean mHasActionTextColor;
        private int mType;

        private Builder(CharSequence text) {
            mText = text;
            mMaxLines = 3;
            mTextColor = Color.WHITE;
            mBackgroundColor = ContextCompat.getColor(mView.getContext(), R.color.theme_grey_primary);
            mIsImportant = false;
            mDuration = Snackbar.LENGTH_LONG;
            mActionText = "Action";
            mType = 0;
        }

        public Builder maxLines(int maxLines) {
            mMaxLines = maxLines;
            return this;
        }

        public Builder textColorRes(@ColorRes int color) {
            mTextColor = ContextCompat.getColor(mView.getContext(), color);
            return this;
        }

        public Builder textColor(@ColorInt int color) {
            mTextColor = color;
            return this;
        }

        public Builder backgroundColorRes(@ColorRes int color) {
            mBackgroundColor = ContextCompat.getColor(mView.getContext(), color);
            return this;
        }

        public Builder backgroundColor(@ColorInt int color) {
            mBackgroundColor = color;
            return this;
        }

        public Builder important() {
            return important(true);
        }

        public Builder important(boolean isImportant) {
            mIsImportant = isImportant;
            return this;
        }

        public Builder duration(int duration) {
            mDuration = duration;
            return this;
        }

        public Builder action(View.OnClickListener listener) {
            mActionListener = listener;
            return this;
        }

        public Builder setSnackbarCallbackListener(Snackbar.Callback snackbarCallbackListener) {
            this.mSnackbarCallbackListener = snackbarCallbackListener;
            return this;
        }

        public Builder actionTextRes(@StringRes int text) {
            mActionText = mView.getContext().getString(text);
            return this;
        }

        public Builder actionText(String text) {
            mActionText = text;
            return this;
        }

        public Builder actionTextColorRes(@ColorRes int color) {
            return actionTextColor(ContextCompat.getColor(mView.getContext(), color));
        }

        public Builder actionTextColor(@ColorInt int color) {
            mActionTextColor = color;
            mHasActionTextColor = true;
            return this;
        }

        public Builder actionTextColors(ColorStateList actionColors) {
            mActionColors = actionColors;
            return this;
        }

        public Builder type(int type) {
            mType = type;
            return this;
        }

        public void show() {
            putToMessageQueue(this);
        }

        CharSequence getText() {
            return mText;
        }

        int getMaxLines() {
            return mMaxLines;
        }

        int getDuration() {
            return mDuration;
        }

        @ColorInt
        int getBackgroundColor() {
            return mBackgroundColor;
        }

        @ColorInt
        int getTextColor() {
            return mTextColor;
        }

        @ColorInt
        int getActionTextColor() {
            return mActionTextColor;
        }

        CharSequence getActionText() {
            return mActionText;
        }

        View.OnClickListener getActionListener() {
            return mActionListener;
        }

        ColorStateList getActionColors() {
            return mActionColors;
        }

        boolean hasAction() {
            return mActionListener != null;
        }

        boolean isImportant() { return mIsImportant; }

        boolean hasActionTextColor() {
            return mHasActionTextColor;
        }

        boolean hasActionTextColors() {
            return mActionColors != null;
        }

        public int getType() { return mType; }
    }
}
