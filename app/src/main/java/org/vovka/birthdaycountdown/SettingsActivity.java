/*
 * *
 *  * Created by Vladimir Belov on 27.11.19 13:35
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 27.11.19 13:35
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Random;

public class SettingsActivity extends AppCompatPreferenceActivity {

    //https://stackoverflow.com/questions/26564400/creating-a-preference-screen-with-support-v21-toolbar

    private String testChannelId = "";

    @SuppressLint("PrivateResource")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            ContactsEvents eventsData = ContactsEvents.getInstance();
            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeMain);

            setContentView(R.layout.activity_settings);
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

            //Цвет заголовка окна
            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            Window w = getWindow();
            w.setStatusBarColor(ta.getColor(R.styleable.Theme_windowStatusbarColor, 0)); //почему-то сама из темы не ставится
            w.setNavigationBarColor(ta.getColor(R.styleable.Theme_windowStatusbarColor, 0));
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setHomeButtonEnabled(true);
                bar.setDisplayHomeAsUpEnabled(true);
                bar.setDisplayShowTitleEnabled(true);
                bar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_material);
                bar.setTitle(R.string.window_settings);
            }

            addPreferencesFromResource(R.xml.settings);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    //todo: сделать изменение темы и языка сразу при изменении

    //todo: запоминать, что было хоть какое изменение настроек. если не было - resume без обновления данных

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        try {

            if (preference instanceof PreferenceScreen) {
                setUpNestedScreen((PreferenceScreen) preference);
            } else if ("NotifyTest".equals(preference.getKey())) {
                ContactsEvents eventsData = ContactsEvents.getInstance();
                eventsData.getPreferences(); //перечитываем настройки, если их меняли для показа уведомлений

                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        NotificationManager notificationManager = getSystemService(NotificationManager.class);

                        //если был предыдущий тест
                        notificationManager.deleteNotificationChannel(testChannelId);

                        Random r = new Random();
                        testChannelId = Integer.toString(r.nextInt(1000));

                        NotificationChannel channel = new NotificationChannel(testChannelId, getString(R.string.pref_Notifications_Notification_Channel_Name), NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription(getString(R.string.pref_Notifications_Notification_Channel_Description));
                        channel.setSound(Uri.parse(eventsData.preferences_notifications_ringtone), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                        channel.enableVibration(true);

                        notificationManager.createNotificationChannel(channel);
                    }
                    eventsData.showNotifications(true, testChannelId);

//                    if (isMIUI(eventsData.context)) {
//                        TypedArray ta = eventsData.context.getTheme().obtainStyledAttributes(R.styleable.Theme);
//                        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(eventsData.context, ContactsEvents.getInstance().preferences_theme.themeDialog))
//                                .setTitle(R.string.title_notifications_MIUI)
//                                .setMessage(R.string.msg_notifications_MIUI)
//                                .setIcon(android.R.drawable.ic_menu_info_details)
//                                .setPositiveButton(R.string.button_OK, (dialog, which) -> dialog.cancel())
//                                .setCancelable(true);
//
//                        AlertDialog alertToShow = builder.create();
//
//                        alertToShow.setOnShowListener(arg0 -> {
//                            alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
//                        });
//
//                        alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
//                        alertToShow.show();
//                    }

                } else {
                    Toast.makeText(this, getString(R.string.msg_notifications_disabled), Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_ON_PREFERENCE_TREE_CLICK_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

        return false;
    }

    private void setUpNestedScreen(@NonNull PreferenceScreen preferenceScreen) {

        try {
            //Добавляем тулбар
            //https://code.i-harness.com/en/q/1cfc0dc

            Dialog dialog = preferenceScreen.getDialog();
            ListView list = dialog.findViewById(android.R.id.list);
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) { //Для Android > 6

                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) list.getLayoutParams();
                marginParams.setMargins(0, (int) (42 * displayMetrics.density + 0.5f), 0, 0);
                list.setPadding(0, (int) (10 * displayMetrics.density + 0.5f), 0, 0);
                ViewGroup root = (ViewGroup) list.getParent();
                Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(bar, 0); // insert at top
                bar.setTitle(preferenceScreen.getTitle());
                bar.setNavigationOnClickListener(v -> dialog.dismiss());
                root.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.white)));

            } else { //Для Android <= 6

                list.setPadding(0, (int) (10 * displayMetrics.density + 0.5f), 0, 0);
                LinearLayout root = (LinearLayout) list.getParent();
                Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
                root.addView(bar, 0); // insert at top
                bar.setTitle(preferenceScreen.getTitle());
                bar.setNavigationOnClickListener(v -> dialog.dismiss());
                root.setBackgroundColor(ta.getColor(R.styleable.Theme_backgroundColor, ContextCompat.getColor(this, R.color.white)));

            }
            list.setDivider(new ColorDrawable(ta.getColor(R.styleable.Theme_listDividerColor, ContextCompat.getColor(this, R.color.light_gray_trans))));
            list.setDividerHeight((int) (1 * displayMetrics.density));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.SETTINGS_ACTIVITY_SET_UP_NESTED_SCREEN_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onStop() {

        //удаляем временный канал оповещений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !testChannelId.equals("")) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(testChannelId);
        }

        super.onStop();
    }

//    private static boolean isIntentResolved(Context context, Intent intent ){
//        return (intent != null && context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null);
//    }
//
//    public static boolean isMIUI(Context ctx) {
//        //https://stackoverflow.com/questions/47610456/how-to-detect-miui-rom-programmatically-in-android
//        isIntentResolved(ctx, new Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
//                || isIntentResolved(ctx, new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
//                || isIntentResolved(ctx, new Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").addCategory(Intent.CATEGORY_DEFAULT))
//                || isIntentResolved(ctx, new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")))}

}