/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 23.08.2023, 19:34
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.util.List;

import androidx.annotation.NonNull;

class ImageSelectAdapter extends ArrayAdapter<String> {

    private static final String TAG = "ImageSelectAdapter";
    private final List<Integer> images;
    private final TypedArray ta;
    private final Scale scale;

    enum Scale {
        NO_SCALE, SQUARED, ONE_THIRD
    }

    ImageSelectAdapter(Context context, @NonNull List<String> items, @NonNull List<Integer> images, Scale scale, @NonNull TypedArray theme) {
        super(context, R.layout.settings_list_item_single_choice, items);
        this.images = images;
        this.ta = theme;
        this.scale = scale;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        try {

            CheckedTextView textView = view.findViewById(android.R.id.text1);

            textView.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
            textView.setTextSize(16);
            textView.setMaxLines(5);

            Bitmap bmp;
            int targetBitmapSize = 130;
            if (position < images.size() && images.get(position) != null) {
                bmp = ContactsEvents.getBitmap(getContext(), images.get(position));
            } else {
                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                bmp = Bitmap.createBitmap(targetBitmapSize, targetBitmapSize, conf);
            }
            if (bmp != null) {
                Bitmap bitmapResized;
                if (this.scale.equals(Scale.SQUARED)) {

                    final int bmWidth = bmp.getWidth();
                    final int bmHeight = bmp.getHeight();
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

                } else if (this.scale.equals(Scale.ONE_THIRD)) {

                    bitmapResized = Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, bmp.getWidth() / 3, 0, bmp.getWidth() / 3, bmp.getHeight()), 90, targetBitmapSize, true);

                } else {

                    final int bmWidth = bmp.getWidth();
                    final int bmHeight = bmp.getHeight();
                    float scale = (float) targetBitmapSize / Math.max(bmHeight, bmWidth);
                    bitmapResized = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * scale), (int) (bmp.getHeight() * scale), true);

                 }
                bmp.recycle();
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(new BitmapDrawable(getContext().getResources(), bitmapResized), null, null, null);
            }
            textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, ta.getResources().getDisplayMetrics()));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return view;
    }

}
