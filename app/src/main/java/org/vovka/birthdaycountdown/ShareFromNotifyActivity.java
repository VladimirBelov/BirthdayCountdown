/*
 * *
 *  * Created by Vladimir Belov on 31.03.2025, 10:49
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 31.03.2025, 10:12
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * ShareFromNotifyActivity - это Activity, которое обрабатывает передачу текста, полученного из уведомления.
 * Оно извлекает текстовые данные из дополнительных параметров (extras) интента и инициирует интент для
 * передачи (share intent), позволяя пользователю выбрать приложение, с которым нужно поделиться текстом.
 * <p>
 * Это Activity предназначено для запуска из уведомления и имеет определенный порядок действий:
 * 1. Оно получает интент, содержащий текстовые данные в своих дополнительных параметрах (extras)
 *    (идентифицируемые по ключу `Intent.EXTRA_TEXT`).
 * 2. Оно проверяет, пусты ли полученные текстовые данные. Если они пусты, Activity немедленно завершается.
 * 3. Если текст присутствует, оно создает неявный интент для передачи (sharing Intent), используя
 *    `Intent.ACTION_SEND`, и устанавливает данные и MIME-тип.
 * 4. Пользователю отображается окно выбора (chooser), чтобы он мог выбрать приложение,
 *    с которым поделиться полученным текстом.
 * 5. Наконец, оно завершает свою работу после запуска процесса передачи.
 */
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
            intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
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
