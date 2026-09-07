/*
 * *
 *  * Created by Vladimir Belov on 07.09.2026, 23:14
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 06.09.2026, 20:36
 *
 */
package org.vovka.birthdaycountdown;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/**
 * Preference с тремя кнопками действий для типа события:
 * иконка, эмодзи, заголовок
 */
public class EventTypeActionsPreference extends Preference {
    private String eventType;
    private String currentEmoji;
    private Drawable currentIconDrawable;
    private boolean iconButtonVisible = true;
    private boolean emojiButtonVisible = true;
    private boolean titleButtonVisible = true;
    private OnActionClickListener listener;

    public EventTypeActionsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setCurrentEmoji(String emoji) {
        this.currentEmoji = emoji;
    }

    void setCurrentIconDrawable(@Nullable Drawable drawable) {
        this.currentIconDrawable = drawable;
        refresh();
    }

    public void setOnActionClickListener(OnActionClickListener listener) {
        this.listener = listener;
    }

    /**
     * Публичный метод для принудительного обновления отображения.
     * Вызывает protected notifyChanged() изнутри класса.
     */
    public void refresh() {
        notifyChanged();
    }


    /**
     * Управляет видимостью кнопки "Иконка"
     * @param visible true - показать, false - скрыть
     */
    public void setIconButtonVisible(boolean visible) {
        this.iconButtonVisible = visible;
        notifyChanged();
    }

    /**
     * Управляет видимостью кнопки "Эмодзи"
     * @param visible true - показать, false - скрыть
     */
    public void setEmojiButtonVisible(boolean visible) {
        this.emojiButtonVisible = visible;
        notifyChanged();
    }

    /**
     * Управляет видимостью кнопки "Заголовок".
     * @param visible true - показать, false - скрыть
     */
    public void setTitleButtonVisible(boolean visible) {
        // Сохраняем флаг, чтобы применить его при следующем bind
        this.titleButtonVisible = visible;
        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        LinearLayout iconAction = view.findViewById(R.id.action_event_icon);
        LinearLayout emojiAction = view.findViewById(R.id.action_event_emoji);
        LinearLayout titleAction = view.findViewById(R.id.action_event_title);
        TextView emojiPreview = view.findViewById(R.id.action_event_emoji_preview);
        ImageView iconPreview = view.findViewById(R.id.action_event_icon_preview);

        if (emojiPreview != null && currentEmoji != null && !currentEmoji.isEmpty()) {
            emojiPreview.setText(currentEmoji);
        }

        if (iconPreview != null && currentIconDrawable != null) {
            iconPreview.setImageDrawable(currentIconDrawable);
        }

        // Управление видимостью кнопок
        if (iconAction != null) {
            iconAction.setVisibility(iconButtonVisible ? View.VISIBLE : View.GONE);
        }
        if (emojiAction != null) {
            emojiAction.setVisibility(emojiButtonVisible ? View.VISIBLE : View.GONE);
        }
        if (titleAction != null) {
            titleAction.setVisibility(titleButtonVisible ? View.VISIBLE : View.GONE);
        }

        if (iconAction != null) {
            iconAction.setOnClickListener(v -> {
                if (listener != null) listener.onIconClick(eventType);
            });
        }
        if (emojiAction != null) {
            emojiAction.setOnClickListener(v -> {
                if (listener != null) listener.onEmojiClick(eventType);
            });
        }
        if (titleAction != null) {
            titleAction.setOnClickListener(v -> {
                if (listener != null) listener.onTitleClick(eventType);
            });
        }
    }

    public interface OnActionClickListener {
        void onIconClick(String eventType);
        void onEmojiClick(String eventType);
        void onTitleClick(String eventType);
    }
}