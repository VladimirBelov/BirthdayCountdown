/*
 * *
 *  * Created by Vladimir Belov on 26.12.2025, 20:59
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.12.2025, 14:35
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;

import org.vovka.birthdaycountdown.utils.ImageUtils;

import java.util.List;

/**
 * {@link ImageSelectAdapter} - это пользовательский BaseAdapter, предназначенный для отображения
 * списка элементов с прикрепленным изображением. Он обрабатывает масштабирование изображений и
 * отображает текст элемента рядом с отмасштабированным изображением. Адаптер поддерживает
 * различные варианты масштабирования изображений.
 */
class ImageSelectAdapter extends BaseAdapter {

    private final Context context;
    private static final String TAG = "ImageSelectAdapter";
    private final List<String> items;
    private final List<Integer> images;
    private final TypedArray ta;
    private final Scale scale;

    enum Scale {
        NO_SCALE, SQUARED, ONE_THIRD
    }

    ImageSelectAdapter(Context context, @NonNull List<String> items, @NonNull List<Integer> images, Scale scale, @NonNull TypedArray theme) {
        this.context = context;
        this.items = items;
        this.images = images;
        this.ta = theme;
        this.scale = scale;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public String getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ViewHolder holder;

        try {

            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.settings_list_item_single_choice_base, parent, false);
                holder = new ViewHolder();
                holder.icon = view.findViewById(R.id.icon);
                holder.text = view.findViewById(R.id.text);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.text.setText(items.get(position));
            holder.text.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
            holder.text.setTextSize(16);
            holder.text.setMaxLines(5);

            Bitmap bmp;
            int targetBitmapSize = 130;
            if (position < images.size() && images.get(position) != null) {
                bmp = ImageUtils.getBitmap(this.context, images.get(position));
            } else {
                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                bmp = Bitmap.createBitmap(targetBitmapSize, targetBitmapSize, conf);
            }
            if (bmp != null) {
                Bitmap bitmapResized;
                final int bmWidth = bmp.getWidth();
                final int bmHeight = bmp.getHeight();

                if (this.scale.equals(Scale.SQUARED) && bmWidth != bmHeight) {

                    Bitmap bitmapSquared;
                    if (bmHeight > bmWidth) {
                        //noinspection SuspiciousNameCombination
                        bitmapSquared = Bitmap.createBitmap(bmp, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
                    } else {
                        //noinspection SuspiciousNameCombination
                        bitmapSquared = Bitmap.createBitmap(bmp, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
                    }
                    bitmapResized = Bitmap.createScaledBitmap(bitmapSquared, targetBitmapSize, targetBitmapSize, true);
                    bitmapSquared.recycle();

                } else if (this.scale.equals(Scale.ONE_THIRD) && bmWidth != bmHeight) {

                    bitmapResized = Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, bmWidth / 3, 0, bmWidth / 3, bmHeight), 90, targetBitmapSize, true);

                } else {

                    float scale = (float) targetBitmapSize / Math.max(bmHeight, bmWidth);
                    bitmapResized = Bitmap.createScaledBitmap(bmp, (int) (bmWidth * scale), (int) (bmHeight * scale), true);

                 }
                bmp.recycle();
            holder.icon.setImageDrawable(new BitmapDrawable(this.context.getResources(), bitmapResized));
            }
            if (parent instanceof ListView) {
                holder.text.setChecked(((ListView) parent).isItemChecked(position));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this.context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return view;
    }

    static class ViewHolder {
        ImageView icon;
        CheckedTextView text;
    }
}
