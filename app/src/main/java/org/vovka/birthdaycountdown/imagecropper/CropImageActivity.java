/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 17.06.2025, 03:39
 *
 */
package org.vovka.birthdaycountdown.imagecropper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.vovka.birthdaycountdown.ContactsEvents;
import org.vovka.birthdaycountdown.R;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class CropImageActivity extends Activity {

    private Bitmap mBitmap;
    private Uri mInputPath = null;
    private static Uri mOutputPath = null;
    private CropImageView mCropImageView;

    public static class CropParam {
        public int mAspectX = 0;
        public int mAspectY = 0;
        public int mOutputX = 0;
        public int mOutputY = 0;
        public int mMaxOutputX = 0;
        public int mMaxOutputY = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ContactsEvents eventsData = ContactsEvents.getInstance();
        setTheme(eventsData.preferences_theme.themeMain);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_cropimage);
        mCropImageView = findViewById(R.id.CropWindow);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mOutputPath = extras.getParcelable(MediaStore.EXTRA_OUTPUT);
        }
        if (mOutputPath == null) {
            String defaultPath = getCacheDir().getPath() + "cropped.jpg";
            mOutputPath = Uri.fromFile(new File(defaultPath));
        }

        mInputPath = intent.getData();
        if (mInputPath == null) {
            startPickImage();
            return;
        }

        mBitmap = loadBitmap(mInputPath);
        if (mBitmap == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mCropImageView.initialize(mBitmap, getCropParam(intent));
    }

    @Override
    protected void onDestroy() {
        mBitmap = null;
        mCropImageView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == 0) {
            mInputPath = data.getData();
            mBitmap = loadBitmap(mInputPath);
            if (mBitmap == null) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            mCropImageView.initialize(mBitmap, getCropParam(getIntent()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onClickBack(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void onClickSave(View v) {
        new SaveImageTask(this).execute(mCropImageView.getCropBitmap());
    }

    public void onClickRotate(View v) {
        mCropImageView.rotate();
        mCropImageView.invalidate();
    }

    public void onClickReset(View v) {
        mCropImageView.reset();
    }

    public void onClickCrop(View v) {
        mCropImageView.crop();
    }

    private static class SaveImageTask extends AsyncTask<Bitmap, Void, Boolean> {

        private ProgressDialog mProgressDialog = null;
        private final WeakReference<CropImageActivity> activityRef; // Используем WeakReference

        private SaveImageTask(CropImageActivity activity) {
            this.activityRef = new WeakReference<>(activity);
            if (activity != null) {
                mProgressDialog = new ProgressDialog(activity);
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setCancelable(false);
            }
        }

        @Override
        protected void onPreExecute() {
            CropImageActivity activity = activityRef.get();
            if (activity != null) {
                mProgressDialog.setTitle(activity.getString(R.string.crop_image_save));
                mProgressDialog.setMessage(activity.getString(R.string.crop_image_saving));
                mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            CropImageActivity activity = activityRef.get();
            if (activity != null) {
                activity.setResult(RESULT_OK, new Intent().putExtra(MediaStore.EXTRA_OUTPUT, mOutputPath));
                activity.finish();
            }
        }

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            OutputStream outputStream = null;
            try {
                CropImageActivity activity = activityRef.get();
                if (activity != null) {
                    outputStream = activity.getContentResolver().openOutputStream(mOutputPath);
                    if (outputStream != null) {
                        params[0].compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    }
                }
            } catch (IOException ignored) { /**/
            } finally {
                closeSilently(outputStream);
            }

            return Boolean.TRUE;
        }
    }

    protected Bitmap loadBitmap(Uri uri) {

        Bitmap bitmap = null;
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(in);
            if (in != null) {
                in.close();
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Can't found image file !", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Can't load source image !", Toast.LENGTH_LONG).show();
        }
        return bitmap;
    }

    protected static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) { /**/
        }
    }

    public static CropParam getCropParam(Intent intent) {
        CropParam params = new CropParam();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(CropIntent.ASPECT_X) && extras.containsKey(CropIntent.ASPECT_Y)) {
                params.mAspectX = extras.getInt(CropIntent.ASPECT_X);
                params.mAspectY = extras.getInt(CropIntent.ASPECT_Y);
            }
            if (extras.containsKey(CropIntent.OUTPUT_X) && extras.containsKey(CropIntent.OUTPUT_Y)) {
                params.mOutputX = extras.getInt(CropIntent.OUTPUT_X);
                params.mOutputY = extras.getInt(CropIntent.OUTPUT_Y);
            }
            if (extras.containsKey(CropIntent.MAX_OUTPUT_X) && extras.containsKey(CropIntent.MAX_OUTPUT_Y)) {
                params.mMaxOutputX = extras.getInt(CropIntent.MAX_OUTPUT_X);
                params.mMaxOutputY = extras.getInt(CropIntent.MAX_OUTPUT_Y);
            }
        }
        return params;
    }

    protected void startPickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 0);
    }
}
