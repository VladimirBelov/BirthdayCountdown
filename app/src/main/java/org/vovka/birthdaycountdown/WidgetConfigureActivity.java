/*
 * *
 *  * Created by Vladimir Belov on 08.12.19 16:02
 *  * Copyright (c) 2018 - 2019. All rights reserved.
 *  * Last modified 01.12.19 23:31
 *
 */

package org.vovka.birthdaycountdown;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

public class WidgetConfigureActivity extends AppCompatActivity {

    private int widgetId;
    private String widgetType;
    private ContactsEvents eventsData;

    public void onCreate(Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.context == null) {
                eventsData.context = this; //if (eventsData.context == null) eventsData.context = getApplicationContext();
                eventsData.getPreferences();
            }

            //todo: на новых андроидах локаль ставится системной, а не из настроек
            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeMain);

            setContentView(R.layout.widget_config);

           /* ActionBar bar = getSupportActionBar();
            if (bar != null) bar.setTitle(R.string.window_widget_settings);*/

            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);
            toolbar.setTitle(R.string.window_widget_settings);

            //Цвет заголовка окна
            TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleColor, ContextCompat.getColor(this, R.color.white)));
            setSupportActionBar(toolbar);

            setSupportActionBar(toolbar);

            setResult(RESULT_CANCELED);
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras == null) {
                //this.widgetName = null;
                this.widgetId = 0;
            } else {
                this.widgetId = extras.getInt("appWidgetId", 0);
            }

            widgetType = AppWidgetManager.getInstance(this).getAppWidgetInfo(this.widgetId).provider.getShortClassName();

            if (!widgetType.equals(".Widget2x2")) {
                findViewById(R.id.dividerFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.spinnerFontMagnifyLabel).setVisibility(View.GONE);
                findViewById(R.id.spinnerFontMagnify).setVisibility(View.GONE);
                findViewById(R.id.textViewFontMagnify).setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.WIDGET_CONFIGURE_ACTIVITY_ON_CREATE_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("unused")
    public void buttonOkOnClick(View view) {
        try {
            //ContactsEvents eventsData = ContactsEvents.getInstance();
            //if (eventsData.context == null) eventsData.context = getApplicationContext();

            Spinner spinnerIndex = findViewById(R.id.spinnerEventShift);
            Spinner spinnerMagnify = findViewById(R.id.spinnerFontMagnify);
            int selectedItemPosition = spinnerIndex.getSelectedItemPosition();
            if (this.widgetId != 0 && selectedItemPosition != -1) {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString(this.getString(R.string.widget_config_PrefName) + this.widgetId,
                        spinnerIndex.getItemAtPosition(selectedItemPosition).toString()
                                .concat(Constants.STRING_COMMA)
                                .concat(widgetType.equals(".Widget2x2") ? spinnerMagnify.getSelectedItemPosition() + "" : "0")
                ).apply();
            }

            Intent intent = new Intent();
            intent.putExtra("appWidgetId", this.widgetId);
            setResult(RESULT_OK, intent);

            //Посылаем сообщения на обновление виджетов

            eventsData.updateWidgets();

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, Constants.WIDGET_CONFIGURE_ACTIVITY_BUTTON_OK_ON_CLICK_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("unused")
    public void buttonCancelOnClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
