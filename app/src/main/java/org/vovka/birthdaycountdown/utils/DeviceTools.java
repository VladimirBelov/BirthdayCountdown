/*
 * *
 *  * Created by Vladimir Belov on 03.11.2026, 10:59
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 03.11.2026, 10:10
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import org.vovka.birthdaycountdown.Constants;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceTools {
    private static final String TAG = "DeviceTools";

    /**
     * MIME-типы таблицы событий календаря.
     * Публичных констант в CalendarContract.Events нет (есть только в ContactsContract)
     */
    private static final String MIME_CALENDAR_EVENTS_DIR = "vnd.android.cursor.dir/event";
    private static final String MIME_CALENDAR_EVENT_ITEM = "vnd.android.cursor.item/event";
    @SuppressLint("PrivateApi")
    public static MIUIAutoStartState getMIUIAutoStartState(@NonNull AppCompatActivity activity) throws Exception {
        //https://stackoverflow.com/questions/39366231/how-to-check-miui-autostart-permission-programmatically

        Class<?> clazz = null;
        try {
            clazz = Class.forName("android.miui.AppOpsUtils");
        } catch (ClassNotFoundException ignored) { /**/ }
        if (clazz == null) return MIUIAutoStartState.NO_INFO;

        Method method = null;
        try {
            method = clazz.getDeclaredMethod("getApplicationAutoStart", Context.class, String.class);
            method.setAccessible(true);
        } catch (Exception ignored) { /**/ }
        if (method == null) return MIUIAutoStartState.NO_INFO;

        final Object result = method.invoke(null, activity, activity.getPackageName());

        if (!(result instanceof Integer)) {
            return MIUIAutoStartState.UNEXPECTED_RESULT;
        }
        final int _int = (int) result;
        if (_int == 0) {
            return MIUIAutoStartState.ENABLED;
        } else if (_int == 1) {
            return MIUIAutoStartState.DISABLED;
        }
        return MIUIAutoStartState.UNEXPECTED_RESULT;
    }

    public static boolean isXiaomi() {
        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi");
    }

    public static boolean isSamsung() {
        return Build.MANUFACTURER.equalsIgnoreCase("samsung");
    }

    public static boolean isWidgetSupportConfig() {
        return isSamsung()
                || (!isXiaomi() & Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                || (isXiaomi() & Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    }

    public static boolean isEdgeToEdge() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM;
    }

    /** Возвращает полный путь до файла
     * @param context Контекст
     * @param uri URI файла
     * @return Путь до файла
     */
    @NonNull
    public static String getPath(Context context, Uri uri) {
    //https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri

        if (uri == null) return "";
        try {

            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else {
                        return "/storage/" + split[0] + "/" + split[1];
                    }
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + getDataColumn(context, uri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    if (contentUri != null) {
                        final String selection = "_id=?";
                        final String[] selectionArgs = {split[1]};
                        String dataColumn = getDataColumn(context, contentUri, selection, selectionArgs);
                        if (dataColumn != null) return dataColumn;
                    } else {
                        String dataColumn = getDataColumn(context, uri, null, null);
                        if (dataColumn != null) return dataColumn;
                    }
                }
            }

            // MediaStore (and general)
            else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
                // Return the remote address
                if (isGooglePhotosUri(uri) && uri.getLastPathSegment() != null) return uri.getLastPathSegment();
                return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getDataColumn(context, uri, null, null);
            }
            // File
            else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
                return uri.getPath();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return "";

    }

    static private boolean isExternalStorageDocument(Uri uri) {
        return Constants.FilePrefix_ExternalStorage.equals(uri.getAuthority());
    }

    static private boolean isDownloadsDocument(Uri uri) {
        return Constants.FilePrefix_Downloads.equals(uri.getAuthority());
    }

    static private boolean isMediaDocument(Uri uri) {
        return Constants.FilePrefix_Media.equals(uri.getAuthority());
    }

    static private boolean isGooglePhotosUri(Uri uri) {
        return Constants.FilePrefix_GooglePhotos.equals(uri.getAuthority());
    }

    @Nullable
    static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        try {

            try (
                    Cursor cursor = context.getContentResolver().query(
                            uri,
                            null,
                            selection,
                            selectionArgs,
                            null
                    )
            ) {
                if (cursor != null && cursor.moveToFirst()) {
                    final int indexName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (indexName > -1) {
                        return cursor.getString(indexName);
                    }
                    final int indexData = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (indexData > -1) {
                        return cursor.getString(indexData);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;

    }

    /** Возвращает невозможность доступа до данных календарей
     * @param context Контекст
     * @return true, если нет доступа до данных календарей
     */
    public static boolean checkNoCalendarAccess(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @return True if battery optimization for this application is OFF
     * @param context Контекст
     */
    public static boolean checkNoBatteryOptimization(Context context) {

        //https://stackoverflow.com/questions/32627342/how-to-whitelist-app-in-doze-mode-android-6-0/32627788#32627788

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String packageName = context.getPackageName();
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                return pm.isIgnoringBatteryOptimizations(packageName);
            } else {
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return true;
        }
    }

    /**
     * @return True if no access to contacts
     * @param context Контекст
     */
    public static boolean checkNoContactsAccess(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkNoStorageAccess(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkNoNotificationAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    public static boolean checkCanExactAlarm(Context context) {
        //Если стоит isIgnoringBatteryOptimizations, то canScheduleExactAlarms возвращает true
        boolean canExact = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager.canScheduleExactAlarms()) {
                canExact = true;
            }
        } else {
            canExact = true;
        }
        return canExact;
    }

    /**
     * Проверяет, используется ли навигация кнопками (3 или 2 кнопки).
     * На Android 10+ считывается из системных настроек.
     * На более старых версиях по умолчанию считаем, что используются кнопки.
     */
    public static boolean isButtonNavigation(Context context) {
        // Строковая константа, так как в старых версиях SDK её может не быть напрямую в Settings.Secure
        final String NAVIGATION_MODE = "navigation_mode";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int mode = Settings.Secure.getInt(
                    context.getContentResolver(),
                    NAVIGATION_MODE,
                    1 // Безопасный фолбэк: если настройка не найдена, считаем, что это кнопки
            );
            // 0 = 3 кнопки, 1 = 2 кнопки. Оба варианта требуют классической обработки цвета.
            return mode == 0 || mode == 1;
        }
        return true; // До Android 10 жестовой навигации в системном понимании не было
    }

    public enum MIUIAutoStartState {
        ENABLED, DISABLED, NO_INFO, UNEXPECTED_RESULT
    }

    /**
     * Типы действий, для которых мы хотим показывать иконки реально назначенных приложений.
     */
    public enum AppActionIcon {
        OPEN_CONTACT,
        EDIT_CONTACT,
        CREATE_CONTACT,
        DIAL,
        OPEN_CALENDAR,
        OPEN_BROWSER
    }

    private static final Map<AppActionIcon, Drawable.ConstantState> appActionIconCache = new ConcurrentHashMap<>();
    private static final Map<AppActionIcon, String> appActionPackageCache = new ConcurrentHashMap<>();

    private static final AtomicBoolean appActionIconsPreloadStarted = new AtomicBoolean(false);

    private static volatile ExecutorService appActionIconsExecutor;

    private static volatile int appActionIconSizePx = 0;

    /**
     * Предзагрузка иконок дефолтных приложений.
     */
    @SuppressWarnings("unused")
    public static void preloadAppActionIcons(@NonNull Context context) {
        preloadAppActionIcons(context, 0);
    }

    /**
     * Предзагрузка иконок дефолтных приложений с указанием размера.
     *
     * @param context     контекст
     * @param iconSizePx  размер иконки в px. Если 0 или меньше, используется системный/дефолтный размер.
     */
    public static void preloadAppActionIcons(@NonNull Context context, int iconSizePx) {
        final Context appContext = context.getApplicationContext();

        if (iconSizePx > 0) {
            appActionIconSizePx = iconSizePx;
        }

        if (appActionIconsPreloadStarted.compareAndSet(false, true)) {
            getAppActionIconsExecutor().execute(() -> {
                for (AppActionIcon action : AppActionIcon.values()) {
                    loadAndCacheAppActionIcon(appContext, action);
                }
            });
        }
    }

    /**
     * Получить иконку назначенного приложения для действия.
     * Если иконка ещё не загружена, вернёт null и запустит асинхронную загрузку.
     */
    @Nullable
    public static Drawable getAppActionIcon(@NonNull Context context,
                                            @NonNull AppActionIcon action) {
        Drawable.ConstantState cs = appActionIconCache.get(action);

        if (cs != null) {
            try {
                return cs.newDrawable().mutate();
            } catch (Exception e) {
                Log.e(TAG, "getAppActionIcon newDrawable error: " + e.getMessage(), e);
                appActionIconCache.remove(action);
            }
        }

        loadAppActionIconAsync(context, action);
        return null;
    }

    /**
     * Получить иконку назначенного приложения, а если она ещё не загружена — fallback-иконку.
     */
    @Nullable
    public static Drawable getAppActionIcon(@NonNull Context context,
                                            @NonNull AppActionIcon action,
                                            @DrawableRes int fallbackResId) {
        Drawable icon = getAppActionIcon(context, action);
        if (icon != null) {
            return icon;
        }

        if (fallbackResId == 0) {
            return null;
        }

        try {
            Drawable fallback = AppCompatResources.getDrawable(context, fallbackResId);
            return fallback != null ? fallback.mutate() : null;
        } catch (Exception e) {
            Log.e(TAG, "getAppActionIcon fallback error: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Асинхронная подгрузка иконки для конкретного действия.
     */
    public static void loadAppActionIconAsync(@NonNull Context context,
                                              @NonNull AppActionIcon action) {
        final Context appContext = context.getApplicationContext();

        getAppActionIconsExecutor().execute(() ->
                loadAndCacheAppActionIcon(appContext, action)
        );
    }

    /**
     * Очистить кэш иконок.
     */
    public static void clearAppActionIconCache() {
        appActionIconCache.clear();
        appActionPackageCache.clear();
        appActionIconsPreloadStarted.set(false);
    }

    private static synchronized ExecutorService getAppActionIconsExecutor() {
        if (appActionIconsExecutor == null) {
            appActionIconsExecutor = Executors.newSingleThreadExecutor();
        }
        return appActionIconsExecutor;
    }

    private static void loadAndCacheAppActionIcon(@NonNull Context context,
                                                  @NonNull AppActionIcon action) {
        try {
            if (appActionIconCache.containsKey(action)) {
                return;
            }

            Drawable.ConstantState cs = resolveAppActionIconConstantState(context, action);
            if (cs != null) {
                appActionIconCache.put(action, cs);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadAndCacheAppActionIcon error: " + e.getMessage(), e);
        }
    }

    @Nullable
    private static Drawable.ConstantState resolveAppActionIconConstantState(@NonNull Context context,
                                                                            @NonNull AppActionIcon action) {
        PackageManager pm = context.getPackageManager();

        List<Intent> intents = getAppActionIntents(action);

        for (Intent intent : intents) {
            ResolveInfo info = resolveActivityInfo(context, intent);
            if (info == null) {
                continue;
            }

            Drawable icon = null;

            try {
                icon = info.loadIcon(pm);
            } catch (Exception ignored) {
                // Некоторые компоненты могут отдавать битую иконку.
            }

            if (icon == null && info.activityInfo != null) {
                try {
                    icon = info.activityInfo.loadIcon(pm);
                } catch (Exception ignored) {
                }
            }

            if (icon == null && info.activityInfo != null && info.activityInfo.applicationInfo != null) {
                try {
                    icon = info.activityInfo.applicationInfo.loadIcon(pm);
                } catch (Exception ignored) {
                }
            }

            if (icon != null) {
                try {
                    Drawable prepared = prepareAppIconDrawable(context, icon);
                    Drawable.ConstantState cs = prepared.getConstantState();
                    if (cs != null) {
                        return cs;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "resolveAppActionIcon prepare icon error: " + e.getMessage(), e);
                }
            }
        }

        return null;
    }

    @Nullable
    private static ResolveInfo resolveActivityInfo(@NonNull Context context,
                                                   @NonNull Intent intent) {
        PackageManager pm = context.getPackageManager();
        final String mime = intent.getType();

        // 1) Системный дефолт — но только если он не "всеядный" (не */*)
        try {
            ResolveInfo def = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (def != null && getFilterSpecificity(def, mime) >= 0) {
                return def;
            }
        } catch (Exception ignored) {
        }

        // 2) Иначе выбираем из списка по специфичности фильтра
        List<ResolveInfo> list = null;
        try {
            list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        } catch (Exception ignored) {
        }
        if (list == null || list.isEmpty()) {
            try {
                list = pm.queryIntentActivities(intent, 0);
            } catch (Exception ignored) {
            }
        }
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);

        ResolveInfo best = null;
        int bestScore = Integer.MIN_VALUE;
        for (ResolveInfo info : list) {
            int score = getFilterSpecificity(info, mime) * 4 + (info.isDefault ? 2 : 0);
            if (score > bestScore) {
                best = info;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * Специфичность intent-фильтра, по которому кандидат совпал с intent:
     *  2 - фильтр объявляет ТОЧНО тот MIME-тип, что у intent (vnd.android.cursor.item/event и т.п.);
     *  1 - фильтр объявляет конкретные (не wildcard) MIME-типы;
     *  0 - у фильтра вообще нет MIME-типов (матчинг по scheme/категории — браузеры, звонилки, MAIN+CATEGORY);
     * -1 - фильтр "всеядный" (только wildcard-типы вида * / *) — типично для файловых
     *      менеджеров, перехватывающих любые content:// URI.
     */
    private static int getFilterSpecificity(@NonNull ResolveInfo info, @Nullable String mime) {
        IntentFilter filter = info.filter;
        if (filter == null) return 0;
        Iterator<String> types = filter.typesIterator();
        if (types == null || !types.hasNext()) return 0;
        boolean exact = false, specific = false, wildcard = false;
        while (types.hasNext()) {
            String t = types.next();
            if (t == null) continue;
            if (mime != null && mime.equalsIgnoreCase(t)) {
                exact = true;
            } else if (t.indexOf('*') >= 0) {
                wildcard = true;
            } else {
                specific = true;
            }
        }
        if (exact) return 2;
        if (specific) return 1;
        if (wildcard) return -1;
        return 0;
    }

    @NonNull
    private static List<Intent> getAppActionIntents(@NonNull AppActionIcon action) {
        List<Intent> intents = new ArrayList<>();
        switch (action) {
            case OPEN_CONTACT: {
                // Просмотр конкретного контакта (как в рантайме)
                intents.add(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, 1L),
                                ContactsContract.Contacts.CONTENT_ITEM_TYPE
                        ));
                // Просмотр списка контактов
                intents.add(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                                ContactsContract.Contacts.CONTENT_URI,
                                ContactsContract.Contacts.CONTENT_TYPE
                        ));
                // Fallback: приложение контактов по категории
                intents.add(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_CONTACTS));
                break;
            }
            case EDIT_CONTACT: {
                Intent editIntent = new Intent(Intent.ACTION_EDIT);
                editIntent.setDataAndType(
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, 1L),
                        ContactsContract.Contacts.CONTENT_ITEM_TYPE
                );
                intents.add(editIntent);
                Intent insertOrEditIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                insertOrEditIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                intents.add(insertOrEditIntent);
                intents.add(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_CONTACTS));
                intents.add(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                                ContactsContract.Contacts.CONTENT_URI,
                                ContactsContract.Contacts.CONTENT_TYPE
                        ));
                break;
            }
            case CREATE_CONTACT: {
                Intent insertIntent = new Intent(Intent.ACTION_INSERT);
                insertIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intents.add(insertIntent);
                Intent insertDirIntent = new Intent(Intent.ACTION_INSERT);
                insertDirIntent.setDataAndType(
                        ContactsContract.Contacts.CONTENT_URI,
                        ContactsContract.Contacts.CONTENT_TYPE
                );
                intents.add(insertDirIntent);
                intents.add(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_CONTACTS));
                break;
            }
            case DIAL: {
                Uri telUri = Uri.parse("tel:123");
                intents.add(new Intent(Intent.ACTION_DIAL, telUri));
                intents.add(new Intent(Intent.ACTION_VIEW, telUri));
                break;
            }
            case OPEN_CALENDAR: {
                // Просмотр конкретного события (как в рантайме)
                intents.add(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 1L),
                                MIME_CALENDAR_EVENT_ITEM
                        ));
                // Просмотр списка событий
                intents.add(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                                CalendarContract.Events.CONTENT_URI,
                                MIME_CALENDAR_EVENTS_DIR
                        ));
                // Fallback: приложение календаря по категории (как было в старом коде)
                intents.add(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_CALENDAR));
                break;
            }
            case OPEN_BROWSER: {
                Uri url = Uri.parse("https://example.com");
                intents.add(new Intent(Intent.ACTION_VIEW, url));
                intents.add(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_BROWSER));
                break;
            }
        }
        return intents;
    }

    @NonNull
    private static Drawable prepareAppIconDrawable(@NonNull Context context,
                                                   @NonNull Drawable source) {
        int size = getAppActionIconSizePx(context);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Drawable drawable = source.mutate();
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static int getAppActionIconSizePx(@NonNull Context context) {
        int size = appActionIconSizePx;

        if (size > 0) {
            return size;
        }

        try {
            size = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        } catch (Exception ignored) {
        }

        if (size <= 0) {
            size = (int) (48 * context.getResources().getDisplayMetrics().density + 0.5f);
        }

        appActionIconSizePx = size;
        return size;
    }

    /**
     * Проверяет, есть ли на устройстве приложение, способное обработать данное действие.
     * Используется для скрытия/показа UI-элементов.
     *
     * @param context контекст
     * @param action  тип действия
     * @return true, если приложение найдено
     */
    public static boolean isAppActionAvailable(@NonNull Context context,
                                               @NonNull AppActionIcon action) {
        List<Intent> intents = getAppActionIntents(action);
        for (Intent intent : intents) {
            if (resolveActivityInfo(context, intent) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Получить имя пакета дефолтного приложения для данного действия.
     * Результат кэшируется.
     */
    @Nullable
    public static String getAppActionPackageName(@NonNull Context context,
                                                 @NonNull AppActionIcon action) {
        String pkg = appActionPackageCache.get(action);
        if (pkg != null) {
            return pkg;
        }

        pkg = resolveAppActionPackage(context.getApplicationContext(), action);
        if (pkg != null) {
            appActionPackageCache.put(action, pkg);
        }
        return pkg;
    }

    @Nullable
    private static String resolveAppActionPackage(@NonNull Context context,
                                                  @NonNull AppActionIcon action) {
        for (Intent intent : getAppActionIntents(action)) {
            ResolveInfo info = resolveActivityInfo(context, intent);
            if (info != null && info.activityInfo != null) {
                return info.activityInfo.packageName;
            }
        }
        return null;
    }
}
