/*
 * *
 *  * Created by Vladimir Belov on 26.12.2025, 20:59
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.12.2025, 20:48
 *
 */

package org.vovka.birthdaycountdown.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.vovka.birthdaycountdown.Constants;

import java.lang.reflect.Method;

public class DeviceTools {
    private static final String TAG = "DeviceTools";
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

    @NonNull
    public static String getPath(Context context, Uri uri) {
    //https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri

        if (uri == null) return Constants.STRING_EMPTY;
        try {

            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(Constants.STRING_COLON);
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + Constants.STRING_SLASH + split[1];
                    } else {
                        return "/storage/" + split[0] + Constants.STRING_SLASH + split[1];
                    }
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + Constants.STRING_SLASH + getDataColumn(context, uri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(Constants.STRING_COLON);
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
        return Constants.STRING_EMPTY;

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

    public enum MIUIAutoStartState {
        ENABLED, DISABLED, NO_INFO, UNEXPECTED_RESULT
    }
}
