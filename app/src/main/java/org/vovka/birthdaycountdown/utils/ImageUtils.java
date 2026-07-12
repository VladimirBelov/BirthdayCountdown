/*
 * *
 *  * Created by Vladimir Belov on 12.07.2026, 13:14
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 10.07.2026, 22:43
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.util.SparseIntArray;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.vovka.birthdaycountdown.Constants;
import org.vovka.birthdaycountdown.Person;
import org.vovka.birthdaycountdown.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ImageUtils {
    static final String TAG = "ImageUtils";
    private static SparseIntArray colorToBorderMap;      // версионные (drawable-v31, -v36)
    private static SparseIntArray colorToBaseBorderMap;  // базовые (drawable)

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
    public static Bitmap scaleDownBitmap(Bitmap bitmap, int maxPixels) {
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
     * @param baseSize Базовый размер, который нужно изменять, например {@link Constants#WIDGET_TEXT_SIZE_TINY}
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

    /**
     * Добавляет значение ко всем компонентам RGB цвета
     * @param color Исходный цвет в формате 0xAARRGGBB
     * @param add Значение для добавления (например, 50)
     * @return Новый цвет с изменёнными компонентами RGB
     */
    public static int addColorValue(int color, int add) {
        int alpha = (color >> 24) & 0xFF;  // A
        int red   = (color >> 16) & 0xFF;  // R
        int green = (color >> 8)  & 0xFF;  // G
        int blue  = color & 0xFF;          // B

        // Добавляем с проверкой на переполнение (максимум 255)
        red   = Math.min(255, red + add);
        green = Math.min(255, green + add);
        blue  = Math.min(255, blue + add);

        // Собираем обратно в int
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Вычисление inSampleSize для BitmapFactory
     */
    public static int calculateInSampleSize(int imageWidth, int imageHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (imageHeight > reqHeight || imageWidth > reqWidth) {
            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Добавление чёрной ленточки к фото
     *
     * @param bm       Исходное фото
     * @param bmWidth  Ширина фото
     * @param bmHeight Высота фото
     * @return Результирующее фото
     */
    @NonNull
    public static Bitmap applyMourningTape(@NonNull Bitmap bm, int bmWidth, int bmHeight) {
        //https://stackoverflow.com/questions/3089991/how-to-draw-a-shape-or-bitmap-into-another-bitmap-java-android
        Bitmap bmOverlay = Bitmap.createBitmap(bmWidth, bmHeight, bm.getConfig() != null ? bm.getConfig() : Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bm, new Matrix(), null);

        Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(Color.BLACK);
        float widthCorrection = (float) bmWidth / 12;
        paintFill.setStrokeWidth(widthCorrection * 2);
        canvas.drawLine((float) (bmWidth * 1.25), (float) bmHeight / 2, (float) bmWidth / 2, (float) (bmHeight * 1.25), paintFill);

        Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(Color.WHITE);
        paintStroke.setStrokeWidth(3);
        canvas.drawLine((float) (bmWidth * 1.25 - widthCorrection * 1.4), (float) bmHeight / 2, (float) ((double) bmWidth / 2 - widthCorrection * 1.4), (float) (bmHeight * 1.25), paintStroke);
        canvas.drawLine((float) (bmWidth * 1.25 + widthCorrection * 1.4), (float) bmHeight / 2, (float) ((double) bmWidth / 2 + widthCorrection * 1.4), (float) (bmHeight * 1.25), paintStroke);

        bm.recycle();
        bm = bmOverlay;
        return bm;
    }

    /**
     * Возвращает иконку события по типу и подтипу
     *
     * @param eventType    Тип события
     * @param eventSubType Подтип события
     * @return Ссылка на ресурс иконки события
     */
    public static int getEventIcon(@NonNull String eventType, @NonNull String eventSubType) {
        switch (eventSubType) {
            case Constants.EventType_BirthDay:
                return R.drawable.ic_event_birthday;
            case Constants.EventType_Anniversary:
                return R.drawable.ic_event_wedding;
            case Constants.EventType_NameDay:
                return R.drawable.ic_event_nameday;
            case Constants.EventType_Crowning:
                return R.drawable.ic_event_crowning;
            case Constants.EventType_Death:
                return R.drawable.ic_event_death;
            case Constants.EventType_Holiday:
                return R.drawable.ic_event_holiday;
            case Constants.EventType_Custom1:
                return R.drawable.ic_event_custom1;
            case Constants.EventType_Custom2:
                return R.drawable.ic_event_custom2;
            case Constants.EventType_Custom3:
                return R.drawable.ic_event_custom3;
            case Constants.EventType_Custom4:
                return R.drawable.ic_event_custom4;
            case Constants.EventType_Custom5:
                return R.drawable.ic_event_custom5;
            case Constants.EventType_5K:
                return R.drawable.ic_event_medal;
            case Constants.EventType_Xdays:
                return R.drawable.ic_event_xdays;
            case Constants.EventType_Another:
                return R.drawable.ic_event_other;
        }
        if (eventType.equals(Constants.EventType_Other)) {
            return R.drawable.ic_event_other;
        }
        return R.drawable.ic_event_unknown;
    }

    public synchronized static void initIconPack(int packNumber, Map<Integer, Integer> packMales, Map<Integer, Integer> packFemales) {

        try {

            packFemales.clear();
            packMales.clear();

            switch (packNumber) {

                case 1:

                    packFemales.put(0, R.drawable.ic_pack01_f2);
                    packFemales.put(6, R.drawable.ic_pack01_f0);
                    packFemales.put(17, R.drawable.ic_pack01_f1);
                    packFemales.put(25, R.drawable.ic_pack01_f2);
                    packFemales.put(35, R.drawable.ic_pack01_f3);
                    packFemales.put(45, R.drawable.ic_pack01_f4);
                    packFemales.put(55, R.drawable.ic_pack01_f5);
                    packFemales.put(150, R.drawable.ic_pack01_f6);

                    packMales.put(0, R.drawable.ic_pack01_m2);
                    packMales.put(6, R.drawable.ic_pack01_m0);
                    packMales.put(17, R.drawable.ic_pack01_m1);
                    packMales.put(25, R.drawable.ic_pack01_m2);
                    packMales.put(35, R.drawable.ic_pack01_m3);
                    packMales.put(45, R.drawable.ic_pack01_m4);
                    packMales.put(55, R.drawable.ic_pack01_m5);
                    packMales.put(150, R.drawable.ic_pack01_m6);

                    break;

                case 2:

                    packFemales.put(0, R.drawable.ic_pack02_f2);
                    packFemales.put(6, R.drawable.ic_pack02_f0);
                    packFemales.put(17, R.drawable.ic_pack02_f1);
                    packFemales.put(25, R.drawable.ic_pack02_f2);
                    packFemales.put(35, R.drawable.ic_pack02_f3);
                    packFemales.put(45, R.drawable.ic_pack02_f4);
                    packFemales.put(55, R.drawable.ic_pack02_f5);
                    packFemales.put(150, R.drawable.ic_pack02_f6);

                    packMales.put(0, R.drawable.ic_pack02_m2);
                    packMales.put(6, R.drawable.ic_pack02_m0);
                    packMales.put(17, R.drawable.ic_pack02_m1);
                    packMales.put(25, R.drawable.ic_pack02_m2);
                    packMales.put(35, R.drawable.ic_pack02_m3);
                    packMales.put(45, R.drawable.ic_pack02_m4);
                    packMales.put(55, R.drawable.ic_pack02_m5);
                    packMales.put(150, R.drawable.ic_pack02_m6);

                    break;

                case 3:

                    packFemales.put(0, R.drawable.ic_pack03_f3);
                    packFemales.put(6, R.drawable.ic_pack03_f0);
                    packFemales.put(17, R.drawable.ic_pack03_f1);
                    packFemales.put(25, R.drawable.ic_pack03_f2);
                    packFemales.put(35, R.drawable.ic_pack03_f3);
                    packFemales.put(45, R.drawable.ic_pack03_f4);
                    packFemales.put(55, R.drawable.ic_pack03_f5);
                    packFemales.put(150, R.drawable.ic_pack03_f6);

                    packMales.put(0, R.drawable.ic_pack03_m3);
                    packMales.put(6, R.drawable.ic_pack03_m0);
                    packMales.put(17, R.drawable.ic_pack03_m1);
                    packMales.put(25, R.drawable.ic_pack03_m2);
                    packMales.put(35, R.drawable.ic_pack03_m3);
                    packMales.put(45, R.drawable.ic_pack03_m4);
                    packMales.put(55, R.drawable.ic_pack03_m5);
                    packMales.put(150, R.drawable.ic_pack03_m6);

                    break;

                case 4:

                    packFemales.put(0, R.drawable.ic_pack00_f1);
                    packMales.put(0, R.drawable.ic_pack00_m1);

                    break;

            }

            if (packFemales.isEmpty()) {
                packFemales.put(0, R.drawable.ic_pack00_f1);
                packFemales.put(15, R.drawable.ic_pack00_f0);
                packFemales.put(60, R.drawable.ic_pack00_f1);
                packFemales.put(150, R.drawable.ic_pack00_f2);
            }

            if (packMales.isEmpty()) {
                packMales.put(0, R.drawable.ic_pack00_m1);
                packMales.put(15, R.drawable.ic_pack00_m0);
                packMales.put(60, R.drawable.ic_pack00_m1);
                packMales.put(150, R.drawable.ic_pack00_m2);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /** Добавление иконки избранного
     * @param bm       Исходное фото
     * @param roundingFactor Параметры скругления углов
     * @param bmWidth  Ширина фото
     * @param bmHeight Высота фото
     * @param res Ресурсы
     * @return Результирующее фото
     */
    @NonNull
    public static Bitmap applyFavoriteStar(Bitmap bm, int roundingFactor, int bmWidth, int bmHeight, @NonNull Resources res) {
        try {

            Bitmap bmOverlay = Bitmap.createBitmap(bmWidth, bmHeight, bm.getConfig() != null ? bm.getConfig() : Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmOverlay);
            canvas.drawBitmap(bm, new Matrix(), null);
            bm.recycle();
            Bitmap bmStar = BitmapFactory.decodeResource(res, R.drawable.fav_star);
            final Bitmap bmStarScaled = Bitmap.createScaledBitmap(bmStar,
                    bmOverlay.getWidth() / 4 - (bmOverlay.getWidth() - bmOverlay.getHeight()) / 4, bmOverlay.getHeight() / 4, true);

            if (roundingFactor < 3) { //Не круг - рисуем в левом нижнем углу

                canvas.drawBitmap(bmStarScaled, 2 + (float) ((bmOverlay.getWidth() - bmOverlay.getHeight()) / 4),
                        (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

            } else if (roundingFactor < 9) { //Закругление - рисуем в левом нижнем углу правее

                canvas.drawBitmap(bmStarScaled, 10 + (float) ((bmOverlay.getWidth() - bmOverlay.getHeight()) / 8),
                        (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

            } else { //Круг - рисуем внизу по центру

                canvas.drawBitmap(bmStarScaled, (float) (bmOverlay.getWidth() * 3 / 4) / 2, (float) (bmOverlay.getHeight() * 3 / 4) - 2, null);

            }
            bmStar.recycle();
            bmStarScaled.recycle();
            bm = bmOverlay;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return bm;
    }

    /** Возвращает силуэт по возрасту и полу
     * @param singleEventArray Данные события
     * @param context Контекст
     * @param packMales Массив мужских фото
     * @param packFemales Массив женских фото
     * @return Силуэт
     */
    public static Bitmap getSilhouetteBitmap(@NonNull String[] singleEventArray, @NonNull Context context,
                                             Map<Integer, Integer> packMales, Map<Integer, Integer> packFemales) {
        try {

            Person person = new Person(context, singleEventArray);
            int gender = person.getGender();

            //По-умолчанию
            Integer idPhoto = R.drawable.ic_pack00_m1;
            if (gender == 2 && packFemales.get(0) != null) {
                idPhoto = packFemales.get(0);
            } else if (packMales.get(0) != null) {
                idPhoto = packMales.get(0);
            }

            //Если определён возраст
            boolean foundInPeriod = false;
            int beforeAge = 0;
            if (person.Age >= 0) {
                if (gender == 2) {
                    for (Map.Entry<Integer, Integer> entry : packFemales.entrySet()) {
                        beforeAge = entry.getKey();
                        if (beforeAge > 0 && person.Age <= beforeAge) {
                            idPhoto = packFemales.get(beforeAge);
                            foundInPeriod = true;
                            break;
                        }
                    }
                    if (!foundInPeriod) {
                        idPhoto = packFemales.get(beforeAge);
                    }
                } else {
                    for (Map.Entry<Integer, Integer> entry : packMales.entrySet()) {
                        beforeAge = entry.getKey();
                        if (beforeAge > 0 && person.Age <= beforeAge) {
                            idPhoto = packMales.get(beforeAge);
                            foundInPeriod = true;
                            break;
                        }
                    }
                    if (!foundInPeriod) {
                        idPhoto = packMales.get(beforeAge);
                    }
                }
            }
            if (idPhoto == null) return null;
            Bitmap bm = getBitmap(context, idPhoto);
            if (bm == null) return null;

            int bmWidth = bm.getWidth();
            int bmHeight = bm.getHeight();
            if (bmHeight > bmWidth) {
                //noinspection SuspiciousNameCombination
                return Bitmap.createBitmap(bm, 0, (bmHeight - bmWidth) / 2, bmWidth, bmWidth);
            } else {
                //noinspection SuspiciousNameCombination
                return Bitmap.createBitmap(bm, (bmWidth - bmHeight) / 2, 0, bmHeight, bmHeight);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    private static void initColorMaps(Context context) {
        colorToBorderMap = new SparseIntArray(3);
        colorToBaseBorderMap = new SparseIntArray(3);

        // Версионные (система сама выберет из drawable-v31 / -v36)
        colorToBorderMap.put(ContextCompat.getColor(context, R.color.widget_border_grey), R.drawable.widget_border_grey);
        colorToBorderMap.put(ContextCompat.getColor(context, R.color.widget_border_white), R.drawable.widget_border_white);
        colorToBorderMap.put(ContextCompat.getColor(context, R.color.widget_border_orange), R.drawable.widget_border_orange);

        // Базовые (строго из drawable/, без квалификаторов)
        colorToBaseBorderMap.put(ContextCompat.getColor(context, R.color.widget_border_grey), R.drawable.widget_border_base_grey);
        colorToBaseBorderMap.put(ContextCompat.getColor(context, R.color.widget_border_white), R.drawable.widget_border_base_white);
        colorToBaseBorderMap.put(ContextCompat.getColor(context, R.color.widget_border_orange), R.drawable.widget_border_base_orange);
    }

    /** Возвращает ресурс бордюра для виджетов
     * @param color Цвет бордюра из списка widget_border_values
     * @param context Контекст
     * @param useBase Не учитывать классификатор версии Android
     * @return Бордюр
     */
    @DrawableRes
    public static int getWidgetBorder(@ColorInt int color, Context context, boolean useBase) {
        ensureInitialized(context);
        SparseIntArray map = useBase ? colorToBaseBorderMap : colorToBorderMap;
        int fallback = useBase ? R.drawable.widget_border_base_grey : R.drawable.widget_border_grey;
        int res = map.get(color);
        return res != 0 ? res : fallback;
    }

    private static synchronized void ensureInitialized(Context context) {
        if (colorToBorderMap == null) {
            initColorMaps(context.getApplicationContext());
        }
    }
}
