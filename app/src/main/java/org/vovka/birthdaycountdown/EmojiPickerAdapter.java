/*
 * *
 *  * Created by Vladimir Belov on 02.09.2026, 01:33
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 01.09.2026, 20:24
 *
 */
package org.vovka.birthdaycountdown;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Адаптер для отображения эмодзи в сетке
 */
public class EmojiPickerAdapter extends BaseAdapter {
    private final List<String> emojis;
    private final LayoutInflater inflater;
    private String selectedEmoji;

    public EmojiPickerAdapter(Context context, List<String> emojis, String selectedEmoji) {
        this.emojis = emojis;
        this.inflater = LayoutInflater.from(context);
        this.selectedEmoji = selectedEmoji;
    }

    /**
     * Обновляет выбранный эмодзи и перерисовывает только видимые элементы.
     * Не пересоздаёт адаптер, поэтому позиция скролла сохраняется.
     */
    public void updateSelectedEmoji(String newSelectedEmoji) {
        if (this.selectedEmoji != null && this.selectedEmoji.equals(newSelectedEmoji)) {
            return; // Ничего не изменилось — выходим
        }
        this.selectedEmoji = newSelectedEmoji;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return emojis.size();
    }

    @Override
    public String getItem(int position) {
        return emojis.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_emoji, parent, false);
        }
        TextView textView = (TextView) convertView;
        String emoji = emojis.get(position);
        textView.setText(emoji);

        if (selectedEmoji != null && selectedEmoji.trim().equals(emoji.trim())) {
            textView.setBackgroundResource(R.drawable.emoji_selected_border);
        } else {
            textView.setBackgroundResource(0);
        }

        return textView;
    }
}