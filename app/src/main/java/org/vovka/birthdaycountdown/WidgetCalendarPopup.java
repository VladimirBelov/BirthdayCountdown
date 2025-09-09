/*
 * *
 *  * Created by Vladimir Belov on 10.09.2025, 01:38
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 10.09.2025, 01:33
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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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
    TextView viewCaption;
    TextView viewInfo;
    TextView buttonCalendar;
    TextView buttonShare;
    TextView buttonPrevDay;
    TextView buttonNextDay;
    String dayInfo = null;
    String dayCaption = null;
    String dayMills = null;
    ArrayList<String> listEventsPacks;
    HashMap<String, Integer> eventsColorsInMonth = null;

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
            this.dayInfo = intent.getStringExtra(Constants.EXTRA_DAY_INFO);
            this.dayCaption = intent.getStringExtra(Constants.EXTRA_DAY_CAPTION);
            this.dayMills = intent.getStringExtra(Constants.EXTRA_VALUES);
            this.listEventsPacks = intent.getStringArrayListExtra(Constants.EXTRA_LIST);
            this.eventsColorsInMonth = getHashMapFromIntent(intent);

            if (this.dayInfo == null || this.dayMills == null) {
                ToastExpander.showInfoMsg(getApplicationContext(), "No extras!");
                finish();
            }

            this.viewInfo = findViewById(R.id.textInfo);
            this.viewCaption = findViewById(R.id.textCaption);

            //Календарь
            this.buttonCalendar = findViewById(R.id.buttonSecondAction);
            this.buttonCalendar.setText(getString(R.string.event_type_other_emoji).concat(Constants.STRING_SPACE).concat(getString(R.string.appwidget_label_Calendar)));
            addClickEffect(this.buttonCalendar);
            this.buttonCalendar.getBackground().setAlpha(50);
            this.buttonCalendar.setVisibility(View.VISIBLE);

            //Поделиться
            this.buttonShare = findViewById(R.id.buttonThirdAction);
            this.buttonShare.setText(R.string.facts_popup_action_share);
            addClickEffect(this.buttonShare);
            this.buttonShare.getBackground().setAlpha(50);
            this.buttonShare.setVisibility(View.VISIBLE);

            //Предыдущий день
            this.buttonPrevDay = findViewById(R.id.buttonFirstAction);
            this.buttonPrevDay.setText(R.string.popup_action_prev);
            addClickEffect(this.buttonPrevDay);
            this.buttonPrevDay.getBackground().setAlpha(50);
            this.buttonPrevDay.setVisibility(View.VISIBLE);

            //Следующий день
            this.buttonNextDay = findViewById(R.id.buttonFourthAction);
            this.buttonNextDay.setText(R.string.popup_action_next);
            addClickEffect(this.buttonNextDay);
            this.buttonNextDay.getBackground().setAlpha(50);
            this.buttonNextDay.setVisibility(View.VISIBLE);

            //Закрыть окно
            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

            showDayInfo();

            if (eventsData.preferences_DaysTypes.isEmpty()) {
                //Заполнение типов дней из календарей по периоду
                Calendar calFirstDay = null;
                Calendar calLastDay = null;
                if (intent.hasExtra(Constants.EXTRA_DAY1) && intent.hasExtra(Constants.EXTRA_DAY2)) {
                    calFirstDay = (Calendar) intent.getSerializableExtra(Constants.EXTRA_DAY1);
                    calLastDay = (Calendar) intent.getSerializableExtra(Constants.EXTRA_DAY2);
                }
                if (calFirstDay != null && calLastDay != null) {
                    eventsData.fillDaysTypesFromCalendars(this.listEventsPacks, calFirstDay, calLastDay);
                }
                //Заполнение типов дней из файлов
                eventsData.fillDaysTypesFromFiles(this.listEventsPacks);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void showDayInfo() {

        try {
            if (this.dayInfo.contains(Constants.TRANSPARENT)) {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                this.dayInfo = this.dayInfo.replaceAll(Constants.TRANSPARENT,
                        Integer.toHexString(ta.getColor(R.styleable.Theme_backgroundColor, 0) & 0x00ffffff));
                ta.recycle();
            }
            viewInfo.setText(HtmlCompat.fromHtml(this.dayInfo, HtmlCompat.FROM_HTML_MODE_LEGACY));

            if (!TextUtils.isEmpty(this.dayCaption)) {
                this.viewCaption.setText(dayCaption);
            }

            buttonCalendar.setOnClickListener(view -> {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath(Constants.QUERY_PARAM_TIME);
                builder.appendPath(this.dayMills);
                Intent intentCalendar = new Intent(Intent.ACTION_VIEW, builder.build());
                intentCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentCalendar);
                finish();
            });

            buttonShare.setOnClickListener(v -> {
                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                intentShare.putExtra(Intent.EXTRA_TEXT,
                        this.viewCaption.getText().toString().concat(Constants.STRING_EOL).concat(this.viewInfo.getText().toString()));
                startActivity(Intent.createChooser(intentShare, ""));
            });

            long millis = Long.parseLong(this.dayMills);
            Calendar newCal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(" (EEE)", Locale.getDefault());

            buttonPrevDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, -1);

                List<String> dayInfo = eventsData.getDayInfo(eventsData.sdf_java.format(newCal.getTime()), this.listEventsPacks, this.eventsColorsInMonth);
                this.dayInfo = dayInfo.isEmpty() ? getString(R.string.month_event_empty) : String.join(Constants.HTML_BR, dayInfo);
                this.dayCaption = getString(R.string.month_event_popup_prefix)
                        .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                        .concat(sdf.format(newCal.getTime()));
                this.dayMills = Long.toString(newCal.getTimeInMillis());

                showDayInfo();
            });

            buttonNextDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, +1);

                List<String> dayInfo = eventsData.getDayInfo(eventsData.sdf_java.format(newCal.getTime()), this.listEventsPacks, this.eventsColorsInMonth);
                this.dayInfo = dayInfo.isEmpty() ? getString(R.string.month_event_empty) : String.join(Constants.HTML_BR, dayInfo);
                this.dayCaption = getString(R.string.month_event_popup_prefix)
                        .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                        .concat(sdf.format(newCal.getTime()));
                this.dayMills = Long.toString(newCal.getTimeInMillis());

                showDayInfo();
            });

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

    /**
     * Вспомогательный метод для безопасного извлечения HashMap с подавлением предупреждения
     *
     * @param intent Intent
     * @return Карта
     */
    @SuppressWarnings("unchecked")
    private static HashMap<String, Integer> getHashMapFromIntent(Intent intent) {
        Serializable serializable = intent.getSerializableExtra(Constants.EXTRA_MAP);
        if (serializable instanceof HashMap) {
            return (HashMap<String, Integer>) serializable;
        }
        return null;
    }
}
