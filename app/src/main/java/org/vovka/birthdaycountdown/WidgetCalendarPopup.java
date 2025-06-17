/*
 * *
 *  * Created by Vladimir Belov on 17.06.2025, 10:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 05.06.2025, 23:19
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * `WidgetCalendarPopup` - это Activity, которое отображает всплывающее окно с подробной информацией
 * о выбранном дне из виджета календаря. Оно получает данные, связанные с этим днем,
 * включая события или другую сопутствующую информацию, и представляет их пользователю.
 * <p>
 * Это Activity обрабатывает:
 * <ul>
 *   <li>Отображение информации, полученной из экземпляра {@link ContactsEvents}.</li>
 *   <li>Динамическое изменение языка всплывающего окна на основе предпочтений пользователя или системных настроек.</li>
 *   <li>Применение темы к всплывающему окну на основе предпочтений пользователя.</li>
 *   <li>Предоставление действий, таких как просмотр дня в приложении календаря или обмен информацией о дне.</li>
 *   <li>Отображение кнопки "закрыть" для закрытия всплывающего окна.</li>
 *   <li>Добавление эффекта нажатия к кнопкам.</li>
 * </ul>
 * <p>
 * Данные передаются этому Activity через Intent extras. Ожидаемые extras:
 * <ul>
 *   <li>{@link Constants#EXTRA_DAY_INFO}: Строка, содержащая основную информацию для отображения.</li>
 *   <li>{@link Constants#EXTRA_DAY_CAPTION}: Строка, содержащая подпись или заголовок для дня.</li>
 *   <li>{@link Constants#EXTRA_VALUES}: Строка, представляющая миллисекунды для выбранного дня.</li>
 * </ul>
 * <p>
 */
public class WidgetCalendarPopup extends Activity {

    private static final String TAG = "WidgetCalendarPopup";
    ContactsEvents eventsData;

    public WidgetCalendarPopup() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    LocaleList list = getSystemService(LocaleManager.class).getApplicationLocales();
                    if (!list.isEmpty()) {
                        locale = getSystemService(LocaleManager.class).getApplicationLocales().get(0);
                    }
                }
                applicationConf.setLocales(new android.os.LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.activity_popup);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            String dayInfo = null;
            String dayCaption = null;
            @Nullable String dayMills;
            if (extras != null) {
                dayInfo = extras.getString(Constants.EXTRA_DAY_INFO);
                dayCaption = extras.getString(Constants.EXTRA_DAY_CAPTION);
                dayMills = extras.getString(Constants.EXTRA_VALUES);
            } else {
                dayMills = null;
            }
            if (!TextUtils.isEmpty(dayInfo)) {
                TextView txtInfo = findViewById(R.id.textInfo);
                if (txtInfo != null) {
                    if (dayInfo.contains(Constants.TRANSPARENT)) {
                        TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                        dayInfo = dayInfo.replaceAll(Constants.TRANSPARENT,
                                Integer.toHexString(ta.getColor(R.styleable.Theme_backgroundColor, 0)  & 0x00ffffff));
                        ta.recycle();
                    }
                    txtInfo.setText(Html.fromHtml(dayInfo));
                }
            } else {
                ToastExpander.showInfoMsg(getApplicationContext(), "No extras!");
                finish();
            }
            if (!TextUtils.isEmpty(dayCaption)) {
                TextView txtCaption = findViewById(R.id.textCaption);
                if (txtCaption != null) {
                    txtCaption.setText(dayCaption);
                }
            }

            if (dayMills != null) {
                TextView buttonAction = findViewById(R.id.buttonFirstAction);
                buttonAction.setText(getString(R.string.event_type_other_emoji).concat(Constants.STRING_SPACE).concat(getString(R.string.appwidget_label_Calendar)));
                buttonAction.setOnClickListener(view -> {
                    Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                    builder.appendPath(Constants.QUERY_PARAM_TIME);
                    builder.appendPath(dayMills);
                    Intent intentCalendar = new Intent(Intent.ACTION_VIEW, builder.build());
                    intentCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentCalendar);
                    finish();
                });
                addClickEffect(buttonAction);
                buttonAction.getBackground().setAlpha(50);
                buttonAction.setVisibility(View.VISIBLE);

                TextView buttonShare = findViewById(R.id.buttonSecondAction);
                buttonShare.setText(R.string.facts_popup_action_share);
                buttonShare.setOnClickListener(v -> {
                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                    intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    TextView txtCaption = findViewById(R.id.textCaption);
                    TextView txtInfo = findViewById(R.id.textInfo);
                    intentShare.putExtra(Intent.EXTRA_TEXT,
                            txtCaption.getText().toString().concat(Constants.STRING_EOL).concat(txtInfo.getText().toString()));
                    startActivity(Intent.createChooser(intentShare, ""));
                });
                addClickEffect(buttonShare);
                buttonShare.getBackground().setAlpha(50);
                buttonShare.setVisibility(View.VISIBLE);
            }

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void addClickEffect(View view)
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
