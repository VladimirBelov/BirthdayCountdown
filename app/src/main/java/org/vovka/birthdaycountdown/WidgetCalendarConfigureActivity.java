/*
 * *
 *  * Created by Vladimir Belov on 31.12.2023, 17:27
 *  * Copyright (c) 2018 - 2023. All rights reserved.
 *  * Last modified 31.12.2023, 17:27
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;
import java.util.Locale;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class WidgetCalendarConfigureActivity extends AppCompatActivity {

    private static final String TAG = "CalendarConfigActivity";
    private int widgetId = 0;
    private ContactsEvents eventsData;
    List<String> widgetPref;
    private static DisplayMetrics displayMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        TypedArray ta = null;

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources applicationRes = getBaseContext().getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeMain);

            setDisplayMetrics(this.getResources().getDisplayMetrics());
            setContentView(R.layout.widget_calendar_config);

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);
            toolbar.setTitle(R.string.window_widget_settings);

            //Цвет заголовка окна
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            //Отступы всего окна
            if (eventsData.preferences_list_marging > 0) {
                RelativeLayout main = findViewById(R.id.layout_main);
                main.setPadding(
                        main.getPaddingLeft() + (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                        main.getPaddingTop(),
                        main.getPaddingRight() + (int) (eventsData.preferences_list_marging * displayMetrics.density + 0.5f),
                        main.getPaddingBottom()
                );
            }

            setResult(RESULT_CANCELED);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) widgetId = extras.getInt(Constants.PARAM_APP_WIDGET_ID, 0);
            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId);
            String widgetType;
            if (appWidgetInfo != null) {
                widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
            } else {
                widgetType = Constants.WIDGET_TYPE_CALENDAR;
            }

            widgetPref = eventsData.getWidgetPreference(widgetId, widgetType);
            if (widgetId > 0 && eventsData.preferences_debug_on) {
                toolbar.setTitle(getString(R.string.window_widget_settings)
                        .concat(Constants.STRING_PARENTHESIS_OPEN)
                        .concat(Constants.STRING_ID)
                        .concat(Constants.STRING_COLON_SPACE)
                        .concat(String.valueOf(widgetId))
                        .concat(Constants.STRING_PARENTHESIS_CLOSE)
                );
            }

            //Количество месяцев

            //Стартовый месяц

            //Источники событий

            //Коэффициент масштабирования размера шрифта

            //Цвета

            //Скрываем недоступные параметры

            if (this.eventsData.checkNoBatteryOptimization()) findViewById(R.id.hintBatteryOptimization).setVisibility(View.GONE);

        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        } finally {
            if (ta != null) ta.recycle();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void openBatteryOptimisationsSettings(@SuppressWarnings("unused") final View view) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        try {
            startActivity(intent);
        } catch (final android.content.ActivityNotFoundException e) { /**/ }
    }

    private synchronized static void setDisplayMetrics(DisplayMetrics ds) {displayMetrics = ds;}
}
