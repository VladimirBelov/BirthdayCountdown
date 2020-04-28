/*
 * *
 *  * Created by Vladimir Belov on 28.04.20 23:21
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 08.04.20 0:58
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.List;

//https://stackoverflow.com/questions/10932832/multiple-choice-alertdialog-with-custom-adapter
//https://stackoverflow.com/questions/8533394/icons-in-a-list-dialog
//https://stackoverflow.com/questions/16932895/how-to-override-the-style-of-android-r-layout-simple-list-item-multiple-choice
//https://stackoverflow.com/questions/7021578/resize-drawable-in-android/23570811
//https://stackoverflow.com/questions/50077917/android-graphics-drawable-adaptiveicondrawable-cannot-be-cast-to-android-graphic
class GetAccountsListAdapter extends ArrayAdapter<String> {

    private final List<Integer> images;
    private final List<String> packages;
    private final TypedArray ta;
    private final PackageManager pm = getContext().getPackageManager();

    GetAccountsListAdapter(Context context, List<String> items, List<Integer> images, List<String> packages, TypedArray theme) {
        super(context, R.layout.settings_list_item_multiple_choice, items); //simple_list_item_multiple_choice
        this.images = images;
        this.packages = packages;
        this.ta = theme;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        try {

            CheckedTextView textView = view.findViewById(android.R.id.text1);

            if (ta != null) textView.setTextColor(ta.getColor(R.styleable.Theme_dialogTextColor, 0));
            textView.setTextSize(16);
            textView.setMaxLines(5);

            Drawable icon = pm.getDrawable(packages.get(position), images.get(position), null);
            if (icon != null) {
                Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);
                Bitmap bitmapResized = Bitmap.createScaledBitmap(bmp, 100, 100, false);
                bmp.recycle();
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(new BitmapDrawable(getContext().getResources(), bitmapResized), null, null, null);
            }
            textView.setCompoundDrawablePadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics()));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), Constants.GET_ACCOUNTS_LIST_ADAPTER_GET_VIEW_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

        return view;
    }

}