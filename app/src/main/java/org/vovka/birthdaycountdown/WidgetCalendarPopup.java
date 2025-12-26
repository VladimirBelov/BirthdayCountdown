/*
 * *
 *  * Created by Vladimir Belov on 26.12.2025, 23:42
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.12.2025, 21:17
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import org.vovka.birthdaycountdown.utils.UiTools;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    Intent intent;
    TextView viewCaption;
    TextView viewInfo;
    TextView buttonCalendar;
    TextView buttonShare;
    TextView buttonPrevDay;
    TextView buttonSelectDay;
    TextView buttonNextDay;
    String dayInfo = null;
    String dayCaption = null;
    String dayMills = null;
    ArrayList<String> listEventsPacks;
    HashMap<String, Integer> eventsColorsInMonth = null;
    private ExecutorService executorService;

    public WidgetCalendarPopup() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();
            eventsData.initLanguage(this);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.activity_popup);

            intent = getIntent();
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
            UiTools.addClickEffect(this.buttonCalendar);
            this.buttonCalendar.getBackground().setAlpha(50);
            this.buttonCalendar.setVisibility(View.VISIBLE);

            //Поделиться
            this.buttonShare = findViewById(R.id.buttonThirdAction);
            this.buttonShare.setText(R.string.facts_popup_action_share);
            UiTools.addClickEffect(this.buttonShare);
            this.buttonShare.getBackground().setAlpha(50);
            this.buttonShare.setVisibility(View.VISIBLE);

            //Предыдущий день
            this.buttonPrevDay = findViewById(R.id.buttonFirstAction);
            this.buttonPrevDay.setText(R.string.popup_action_prev);
            UiTools.addClickEffect(this.buttonPrevDay);
            this.buttonPrevDay.getBackground().setAlpha(50);
            this.buttonPrevDay.setVisibility(View.VISIBLE);

            //Выбрать день
            this.buttonSelectDay = findViewById(R.id.buttonFourthAction);
            this.buttonSelectDay.setText(R.string.popup_action_calendar);
            UiTools.addClickEffect(this.buttonSelectDay);
            this.buttonSelectDay.getBackground().setAlpha(50);
            this.buttonSelectDay.setVisibility(View.VISIBLE);

            //Следующий день
            this.buttonNextDay = findViewById(R.id.buttonFirthAction);
            this.buttonNextDay.setText(R.string.popup_action_next);
            UiTools.addClickEffect(this.buttonNextDay);
            this.buttonNextDay.getBackground().setAlpha(50);
            this.buttonNextDay.setVisibility(View.VISIBLE);

            //Закрыть окно
            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

            executorService = Executors.newSingleThreadExecutor();

            showDayInfo();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    public void onResume() {
        try {
            super.onResume();

            if (eventsData.isEmptyEventList() || eventsData.preferences_DaysTypes.isEmpty()) {
                if (executorService != null && !executorService.isShutdown()) {
                    executorService.execute(() -> {
                        //Background work
                        if (eventsData.preferences_DaysTypes.isEmpty() && intent != null) {
                            //Заполнение типов дней из календарей по периоду
                            Calendar calFirstDay = null;
                            Calendar calLastDay = null;
                            if (intent.hasExtra(Constants.EXTRA_DAY1) && intent.hasExtra(Constants.EXTRA_DAY2)) {
                                calFirstDay = (Calendar) intent.getSerializableExtra(Constants.EXTRA_DAY1);
                                calLastDay = (Calendar) intent.getSerializableExtra(Constants.EXTRA_DAY2);
                            }
                            if (calFirstDay != null && calLastDay != null) {
                                eventsData.fillDaysTypesFromCalendars(listEventsPacks, calFirstDay, calLastDay);
                            }
                            //Заполнение типов дней из файлов
                            eventsData.fillDaysTypesFromFiles(listEventsPacks);
                        }

                        if (eventsData.isEmptyEventList()) {
                            eventsData.getEvents();
                        }
                    });
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void showDayInfo() {

        try {
            if (this.dayInfo.contains(Constants.TRANSPARENT)) {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                this.dayInfo = this.dayInfo.replace(Constants.TRANSPARENT,
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

            buttonSelectDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, dayOfMonth) -> {
                    newCal.clear();
                    newCal.set(selectedYear, selectedMonth, dayOfMonth);

                    List<String> dayInfo = eventsData.getDayInfo(ContactsEvents.sdf_java.format(newCal.getTime()), this.listEventsPacks, this.eventsColorsInMonth);
                    this.dayInfo = dayInfo.isEmpty() ? getString(R.string.month_event_empty) : TextUtils.join(Constants.HTML_BR, dayInfo);
                    this.dayCaption = getString(R.string.month_event_popup_prefix)
                            .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                            .concat(sdf.format(newCal.getTime()));
                    this.dayMills = Long.toString(newCal.getTimeInMillis());

                    showDayInfo();
                }, newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), newCal.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            });
            buttonSelectDay.setOnLongClickListener(v -> {
                Toast.makeText(this, getString(R.string.select_day), Toast.LENGTH_LONG).show();
                return true;
            });

            buttonPrevDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, -1);

                List<String> dayInfo = eventsData.getDayInfo(ContactsEvents.sdf_java.format(newCal.getTime()), this.listEventsPacks, this.eventsColorsInMonth);
                this.dayInfo = dayInfo.isEmpty() ? getString(R.string.month_event_empty) : TextUtils.join(Constants.HTML_BR, dayInfo);
                this.dayCaption = getString(R.string.month_event_popup_prefix)
                        .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                        .concat(sdf.format(newCal.getTime()));
                this.dayMills = Long.toString(newCal.getTimeInMillis());

                showDayInfo();
            });
            buttonPrevDay.setOnLongClickListener(v -> {
                Toast.makeText(this, getString(R.string.previous_day), Toast.LENGTH_LONG).show();
                return true;
            });

            buttonNextDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, +1);

                List<String> dayInfo = eventsData.getDayInfo(ContactsEvents.sdf_java.format(newCal.getTime()), this.listEventsPacks, this.eventsColorsInMonth);
                this.dayInfo = dayInfo.isEmpty() ? getString(R.string.month_event_empty) : TextUtils.join(Constants.HTML_BR, dayInfo);
                this.dayCaption = getString(R.string.month_event_popup_prefix)
                        .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                        .concat(sdf.format(newCal.getTime()));
                this.dayMills = Long.toString(newCal.getTimeInMillis());

                showDayInfo();
            });
            buttonNextDay.setOnLongClickListener(v -> {
                Toast.makeText(this, getString(R.string.next_day), Toast.LENGTH_LONG).show();
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
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
