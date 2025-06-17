/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 17.06.2025, 03:20
 *
 */
package org.vovka.birthdaycountdown.imagecropper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public class CropIntent {

    public static final String ASPECT_X = "aspectX";
    public static final String ASPECT_Y = "aspectY";
    public static final String OUTPUT_X = "outputX";
    public static final String OUTPUT_Y = "outputY";
    public static final String MAX_OUTPUT_X = "maxOutputX";
    public static final String MAX_OUTPUT_Y = "maxOutputY";

    private final Intent mCropIntent = new Intent();

    /** @noinspection unused*/
    public void setImagePath(String filepath) {
        setImagePath(Uri.fromFile(new File(filepath)));
    }

    public void setImagePath(Uri uri) {
        mCropIntent.setData(uri);
    }

    /** @noinspection unused*/
    public void setOutputPath(String filepath) {
        setOutputPath(Uri.fromFile(new File(filepath)));
    }

    public void setOutputPath(Uri uri) {
        mCropIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    }

    /** @noinspection unused*/
    public void setAspect(int x, int y) {
        mCropIntent.putExtra(ASPECT_X, x);
        mCropIntent.putExtra(ASPECT_Y, y);
    }

    /** @noinspection unused*/
    public void setOutputSize(int x, int y) {
        mCropIntent.putExtra(OUTPUT_X, x);
        mCropIntent.putExtra(OUTPUT_Y, y);
    }

    /** @noinspection unused*/
    public void setMaxOutputSize(int x, int y) {
        mCropIntent.putExtra(MAX_OUTPUT_X, x);
        mCropIntent.putExtra(MAX_OUTPUT_Y, y);
    }

    public Intent getIntent(Context context) {
        mCropIntent.setClass(context, CropImageActivity.class);
        return mCropIntent;
    }
}
