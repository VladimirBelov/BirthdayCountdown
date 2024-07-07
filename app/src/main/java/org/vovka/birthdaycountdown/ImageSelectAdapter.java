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
    private final boolean makeSquared;

    ImageSelectAdapter(Context context, @NonNull List<String> items, @NonNull List<Integer> images, boolean makeSquared, @NonNull TypedArray theme) {
        super(context, R.layout.settings_list_item_single_choice, items);
        this.images = images;
        this.ta = theme;
        this.makeSquared = makeSquared;
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

            //Context packageContext = this.context.createPackageContext(packages.get(position), 0);
            //Resources resources = packageContext.getResources();
            //Drawable icon = null; //androidx.core.content.res.ResourcesCompat.getDrawable(resources, images.get(position), null);
            //Drawable icon = pm.getDrawable(packages.get(position), images.get(position), null);
            Bitmap bmp;
            if (position < images.size() && images.get(position) != null) {
                bmp = ContactsEvents.getBitmap(getContext(), images.get(position));
            } else {
                Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                bmp = Bitmap.createBitmap(130, 130, conf);
            }
            if (bmp != null) {
                //Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                /*Canvas canvas = new Canvas(bmp);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);*/
                //Bitmap bitmapResized = Bitmap.createScaledBitmap(bmp, 100, 150, false);
                //Bitmap bitmapScaled = Bitmap.createBitmap(bmp, bmp.getWidth() / 3, 0, bmp.getWidth() / 3, bmp.getHeight());
                Bitmap bitmapResized;
                if (makeSquared) {
                    final int bmWidth = bmp.getWidth();
                    final int bmHeight = bmp.getHeight();
                    //final int bmPadding = 20;
                    Bitmap bitmapSquared;
                    if (bmHeight > bmWidth) {
                        //noinspection SuspiciousNameCombination
                        bitmapSquared = Bitmap.createBitmap(bmp, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
                    } else {
                        //noinspection SuspiciousNameCombination
                        bitmapSquared = Bitmap.createBitmap(bmp, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
                    }
                    //bitmapResized = Bitmap.createBitmap(bitmapSquared.getWidth(), bitmapSquared.getHeight(), Bitmap.Config.ARGB_8888);
                    //Canvas can = new Canvas(bitmapResized);
                    //can.drawBitmap(bitmapSquared, 0, 0, null);
                    bitmapResized = Bitmap.createScaledBitmap(bitmapSquared, 130, 130, true);
                    bitmapSquared.recycle();
                } else {
                    bitmapResized = Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, bmp.getWidth() / 3, 0, bmp.getWidth() / 3, bmp.getHeight()), 90, 130, true);
                }
                //bitmapScaled.recycle();
                bmp.recycle();
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(new BitmapDrawable(getContext().getResources(), bitmapResized), null, null, null);
          //  } else {
          //      textView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            }
            textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, ta.getResources().getDisplayMetrics()));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }

        return view;
    }

}
