/*
 * *
 *  * Created by Vladimir Belov on 18.06.2025, 15:45
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 18.06.2025, 14:10
 *
 */
package org.vovka.birthdaycountdown.imagecropper;

import android.graphics.Bitmap;
import android.graphics.Matrix;

class RotateBitmap {

    private Bitmap bitmap;
    private int rotation;

    public RotateBitmap(Bitmap bitmap, int rotation) {
        this.bitmap = bitmap;
        this.rotation = rotation % 360;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getRotation() {
        return rotation;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Matrix getRotateMatrix() {
        // By default this is an identity matrix
        Matrix matrix = new Matrix();
        if (bitmap != null && rotation != 0) {
            // We want to do the rotation at origin, but since the bounding
            // rectangle will be changed after rotation, so the delta values
            // are based on old & new width/height respectively.
            int cx = bitmap.getWidth() / 2;
            int cy = bitmap.getHeight() / 2;
            matrix.preTranslate(-cx, -cy);
            matrix.postRotate(rotation);
            matrix.postTranslate((float) getWidth() / 2, (float) getHeight() / 2);
        }
        return matrix;
    }

    public boolean isOrientationChanged() {
        return (rotation / 90) % 2 != 0;
    }

    public int getHeight() {
        if (bitmap == null) return 0;
        if (isOrientationChanged()) {
            return bitmap.getWidth();
        } else {
            return bitmap.getHeight();
        }
    }

    public int getWidth() {
        if (bitmap == null) return 0;
        if (isOrientationChanged()) {
            return bitmap.getHeight();
        } else {
            return bitmap.getWidth();
        }
    }

    public void recycle() {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }
}