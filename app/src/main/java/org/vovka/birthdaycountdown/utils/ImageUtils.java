/*
 * *
 *  * Created by Vladimir Belov on 10.02.2026, 14:03
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 10.02.2026, 13:25
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.vovka.birthdaycountdown.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ImageUtils {
    static final String TAG = "ImageUtils";
    public static String toARGBString(int color) {
        // format: #AARRGGBB
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));
        if (alpha.length() == 1) alpha = "0" + alpha;
        if (red.length() == 1) red = "0" + red;
        if (green.length() == 1) green = "0" + green;
        if (blue.length() == 1) blue = "0" + blue;
        return Constants.STRING_HASH + alpha + red + green + blue;
    }

    static Bitmap getBitmap(Drawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    @Nullable
    public static Bitmap getBitmap(Context context, int drawableId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableId);
            if (drawable == null) return null;
            if (drawable instanceof BitmapDrawable) {
                return BitmapFactory.decodeResource(context.getResources(), drawableId);
            } else { //if (drawable instanceof VectorDrawable || drawable instanceof AdaptiveIconDrawable) {
                return getBitmap(drawable);
                //} else {
                //    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    //https://stackoverflow.com/questions/21633637/rounded-corners-android-image-buttons
    public static Bitmap toRoundCorner(@NonNull Bitmap bitmap, int pixelsX, int pixelsY) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, (float) pixelsX, (float) pixelsY, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * Преобразует значение из DIP в фактические пиксели
     *
     * @param res    Объект Resources, используемый для получения метрик дисплея (плотности)
     * @param sizeDP Размер в DIP
     * @return Размер в пикселях
     */
    public static int Dip2Px(Resources res, int sizeDP) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeDP, res.getDisplayMetrics()));
    }

    /**
     * Преобразует значение из SP в фактические пиксели на основе текущей плотности экрана и пользовательских настроек масштаба шрифта
     *
     * @param res    Объект Resources, используемый для получения метрик дисплея (плотности)
     * @param sizeSP Размер в SP
     * @return Размер в пикселях
     */
    public static int Sp2Px(Resources res, int sizeSP) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSP, res.getDisplayMetrics()));
    }

    @Nullable
    public static String encodeImageToBase64(Context context, Uri imageUri, int maxSize) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            bitmap = scaleDownBitmap(bitmap, maxSize); // уменьшаем изображение
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            bitmap.recycle();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    // Функция уменьшения размера изображения
    static Bitmap scaleDownBitmap(Bitmap bitmap, int maxPixels) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = Math.min((float) maxPixels / width, (float) maxPixels / height);
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        if (newWidth >= width && newHeight >= height) return bitmap; //без изменений

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /** Заменяет один цвет на другой в Bitmap
     * @param originalBitmap Исходный Bitmap.
     * @param oldColor Цвет, который нужно заменить (например, Color.RED).
     * @param newColor Цвет, на который нужно заменить (например, Color.BLUE).
     * @param tolerance Допуск для "схожих" цветов (0 = точное совпадение).
     * @return Новый Bitmap с замененными цветами.
     */
     public static Bitmap replaceColorInBitmap(Bitmap originalBitmap, int oldColor, int newColor, int tolerance) {
        if (originalBitmap == null) {
            return null;
        }

        // Создаем изменяемую копию Bitmap
        Bitmap resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        int width = resultBitmap.getWidth();
        int height = resultBitmap.getHeight();
        int[] pixels = new int[width * height];
        resultBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Разделяем компоненты старого цвета
        int oldA = Color.alpha(oldColor);
        int oldR = Color.red(oldColor);
        int oldG = Color.green(oldColor);
        int oldB = Color.blue(oldColor);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int currentA = Color.alpha(pixel);
            int currentR = Color.red(pixel);
            int currentG = Color.green(pixel);
            int currentB = Color.blue(pixel);

            // Сравниваем цвета с учетом допуска
            if (Math.abs(currentR - oldR) <= tolerance &&
                    Math.abs(currentG - oldG) <= tolerance &&
                    Math.abs(currentB - oldB) <= tolerance &&
                    Math.abs(currentA - oldA) <= tolerance) { // Учитываем и альфа-канал, если нужно
                pixels[i] = newColor;
            }
        }

        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return resultBitmap;
    }

    /** Возвращает уровень закругления для фото из настроек виджета
     * @param widgetPref Настройки виджета
     * @return Уровень закругления
     */
    public static int getRoundingFactor(List<String> widgetPref) {
        int roundingFactor = 1;
        if (widgetPref != null && widgetPref.size() > 6) {
            switch (widgetPref.get(6)) {
                case Constants.STRING_1: roundingFactor = 2; break;
                case Constants.STRING_2: roundingFactor = 3; break;
                case Constants.STRING_3: roundingFactor = 4; break;
                case Constants.STRING_4: roundingFactor = 9; break;
            }
        }
        return roundingFactor;
    }

    /** Возвращает коэффициент масштабирования размера элементов виджета
     * @param widgetPref Настройки виджета
     * @param elementNumber Порядковый номер мультипликатора размера в настройке (они хранятся как размер1+размер2+...)
     * @param baseSize Базовый размер, который нужно изменять. например {Constants#WIDGET_TEXT_SIZE_TINY}
     * @param defaultMagnify Мультипликатор по-умолчанию ("Авто")
     * @return Коэффициент масштабирования
     */
    public static float getSizeForWidgetElement(List<String> widgetPref, int elementNumber, int baseSize, double defaultMagnify) {
        double magnify = defaultMagnify;
        try {

            if (widgetPref != null && widgetPref.size() > elementNumber) {
                String[] prefArrayMagnify = widgetPref.get(1).split(Constants.REGEX_PLUS, -1);
                if (prefArrayMagnify.length >= elementNumber) {
                    String prefMagnify = prefArrayMagnify[elementNumber - 1];

                    if (prefMagnify.contains(Constants.STRING_PERIOD)) { //В настройке - сам мультипликатор

                        try {
                            double value = Double.parseDouble(prefMagnify);
                            if (value > 0) magnify *= value;
                        } catch (NumberFormatException ignored) { /**/ }

                    } else { //В настройке - индекс из списка выбора

                        switch (prefMagnify) {
                            case Constants.STRING_1:
                                magnify *= 0.5;
                                break;
                            case Constants.STRING_2:
                                magnify *= 0.65;
                                break;
                            case Constants.STRING_3:
                                magnify *= 0.75;
                                break;
                            case Constants.STRING_4:
                                magnify *= 0.85;
                                break;
                            case Constants.STRING_5:
                                magnify *= 1; //То же, что и "Авто"
                                break;
                            case Constants.STRING_6:
                                magnify *= 1.1;
                                break;
                            case Constants.STRING_7:
                                magnify *= 1.2;
                                break;
                            case Constants.STRING_8:
                                magnify *= 1.3;
                                break;
                            case Constants.STRING_9:
                                magnify *= 1.4;
                                break;
                            case Constants.STRING_10:
                                magnify *= 1.5;
                                break;
                            case Constants.STRING_11:
                                magnify *= 1.6;
                                break;
                            case Constants.STRING_12:
                                magnify *= 1.75;
                                break;
                            case Constants.STRING_13:
                                magnify *= 2.0;
                                break;
                        }
                    }
                }
            }
            return (float) (baseSize * magnify);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return baseSize;
    }
}
