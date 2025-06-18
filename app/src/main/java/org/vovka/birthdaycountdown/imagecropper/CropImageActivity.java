/*
 * *
 *  * Created by Vladimir Belov on 18.06.2025, 15:45
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 18.06.2025, 14:10
 *
 */
package org.vovka.birthdaycountdown.imagecropper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.vovka.birthdaycountdown.Constants;
import org.vovka.birthdaycountdown.ContactsEvents;
import org.vovka.birthdaycountdown.R;
import org.vovka.birthdaycountdown.ToastExpander;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

class CropImageActivity extends Activity {

    private static final String TAG = "CropImageActivity";
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

        try {
            ContactsEvents eventsData = ContactsEvents.getInstance();
            setTheme(eventsData.preferences_theme.themeMain);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_cropimage);

            View layoutMain = findViewById(R.id.layout_main);
            if (ContactsEvents.isEdgeToEdge()) {
                ViewCompat.setOnApplyWindowInsetsListener(layoutMain, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures());
                    layoutMain.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
            }

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

            TextView buttonSave = findViewById(R.id.buttonSave);
            addClickEffect(buttonSave);
            buttonSave.getBackground().setAlpha(125);
            TextView buttonRotate = findViewById(R.id.buttonRotate);
            addClickEffect(buttonRotate);
            buttonRotate.getBackground().setAlpha(125);
            TextView buttonReset = findViewById(R.id.buttonReset);
            addClickEffect(buttonReset);
            buttonReset.getBackground().setAlpha(125);
            TextView buttonCrop = findViewById(R.id.buttonCrop);
            addClickEffect(buttonCrop);
            buttonCrop.getBackground().setAlpha(125);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
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

    private void addClickEffect(@NonNull View view)
    {
        Drawable drawableNormal = view.getBackground();

        if (view.getBackground().getConstantState() != null) {
            Drawable drawablePressed = view.getBackground().getConstantState().newDrawable();
            drawablePressed.mutate();
            drawablePressed.setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);

            StateListDrawable listDrawable = new StateListDrawable();
            listDrawable.addState(new int[]{android.R.attr.state_pressed}, drawablePressed);
            listDrawable.addState(new int[]{}, drawableNormal);
            view.setBackground(listDrawable);
        }
    }
}
