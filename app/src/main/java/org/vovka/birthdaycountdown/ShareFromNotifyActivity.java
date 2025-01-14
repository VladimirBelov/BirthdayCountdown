/*
 * *
 *  * Created by Vladimir Belov on 12.01.2025, 14:01
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 12.01.2025, 14:01
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

public class ShareFromNotifyActivity extends Activity {

    private static final String TAG = "ShareFromNotifyActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
        super.onCreate(savedInstanceState);

            Bundle extras = getIntent().getExtras();
            String notificationData = Constants.STRING_EMPTY;
            if (extras != null) {
                notificationData = extras.getString(Intent.EXTRA_TEXT, Constants.STRING_EMPTY);
            }
            //ToastExpander.showDebugMsg(this, TAG.concat(Constants.STRING_COLON_SPACE).concat(notificationData));
            if (notificationData.equals(Constants.STRING_EMPTY)) finish();

            Intent intentShare = new Intent(Intent.ACTION_SEND);
            intentShare.setType("text/plain");
            intentShare.putExtra(Intent.EXTRA_TEXT, notificationData);
            intentShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            try {
                Intent intentChooser = Intent.createChooser(intentShare, "");
                intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intentChooser);
            } catch (android.content.ActivityNotFoundException e) { /**/ }

            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
}
