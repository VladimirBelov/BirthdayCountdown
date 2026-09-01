/*
 * *
 *  * Created by Vladimir Belov on 02.09.2026, 01:33
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 02.09.2026, 00:53
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
 * Адаптер для Spinner выбора категории эмодзи
 */
public class EmojiCategoryAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> categories;
    private final LayoutInflater inflater;

    public EmojiCategoryAdapter(Context context, List<String> categories) {
        this.context = context;
        this.categories = categories;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public String getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_emoji_category, parent, false);
        }
        TextView iconView = convertView.findViewById(R.id.category_icon);
        TextView nameView = convertView.findViewById(R.id.category_name);

        String category = categories.get(position);
        iconView.setText(EmojiData.CATEGORY_ICONS.get(category));
        nameView.setText(EmojiData.getCategoryName(context, category));

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}