/*
 * *
 *  * Created by Vladimir Belov on 29.06.2026, 14:56
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 29.06.2026, 14:11
 *
 */
package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
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
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import org.vovka.birthdaycountdown.utils.StringUtils;
import org.vovka.birthdaycountdown.utils.UiTools;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 `WidgetCalendarPopup` - это Activity, которое отображает всплывающее окно с подробной информацией
 о выбранном дне из виджета календаря. Оно получает данные, связанные с этим днем,
 включая события или другую сопутствующую информацию, и представляет их пользователю.
 Это Activity обрабатывает:
 Отображение информации, полученной из экземпляра {@link ContactsEvents}.
 Предоставление действий, таких как просмотр дня в приложении календаря или обмен информацией о дне.
 Данные передаются этому Activity через Intent extras. Ожидаемые extras:
 {@link AppWidgetManager#EXTRA_APPWIDGET_ID}: Строка, содержащая ID вызвавшего виджета.
 {@link Constants#EXTRA_DAY_INFO}: Строка, содержащая основную информацию для отображения.
 {@link Constants#EXTRA_DAY_CAPTION}: Строка, содержащая подпись или заголовок для дня.
 {@link Constants#EXTRA_VALUES}: Строка, представляющая миллисекунды для выбранного дня.
 {@link Constants#EXTRA_LIST}: Массив, представляющая список источников для событий.
 */
public class WidgetCalendarPopup extends Activity {
    private static final String TAG = "WidgetCalendarPopup ";
    ContactsEvents eventsData;
    Intent intent;
    TextView viewCaption;
    TextView viewInfo;
    TextView buttonCalendar;
    TextView buttonShare;
    TextView buttonPrevDay;
    TextView buttonSelectDay;
    TextView buttonSelectColor;
    TextView buttonNextDay;
    private int appWidgetId = 0;
    private String dayInfo = null;
    private String dayCaption = null;
    private String dayMills = null;
    private ArrayList<String> listEventsPacks;
    private HashMap<String, Integer> eventsColorsInMonth = null;
    private ExecutorService executorService;

    public WidgetCalendarPopup() {
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.widget_calendar_popup);

            intent = getIntent();
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            dayInfo = intent.getStringExtra(Constants.EXTRA_DAY_INFO);
            dayCaption = intent.getStringExtra(Constants.EXTRA_DAY_CAPTION);
            dayMills = intent.getStringExtra(Constants.EXTRA_VALUES);
            listEventsPacks = intent.getStringArrayListExtra(Constants.EXTRA_LIST);
            eventsColorsInMonth = getHashMapFromIntent(intent);

            if (dayInfo == null || dayMills == null) {
                ToastExpander.showInfoMsg(getApplicationContext(), "No extras!");
                finish();
            }

            viewInfo = findViewById(R.id.textInfo);
            viewCaption = findViewById(R.id.textCaption);

            //Предыдущий день
            buttonPrevDay = findViewById(R.id.button1);
            if (buttonPrevDay != null) {
                buttonPrevDay.setText(R.string.popup_action_prev);
                UiTools.addClickEffect(buttonPrevDay);
                buttonPrevDay.getBackground().setAlpha(50);
                buttonPrevDay.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.widget_calendar_popup_previous_day), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonPrevDay.setVisibility(View.VISIBLE);
            }

            //Календарь
            buttonCalendar = findViewById(R.id.button2);
            if (buttonCalendar != null) {
                buttonCalendar.setText(getString(R.string.event_type_other_emoji).concat(Constants.STRING_SPACE).concat(getString(R.string.appwidget_label_Calendar)));
                UiTools.addClickEffect(buttonCalendar);
                buttonCalendar.getBackground().setAlpha(50);
                buttonCalendar.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.widget_calendar_popup_open_calendar), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonCalendar.setVisibility(View.VISIBLE);
            }

            //Поделиться
            buttonShare = findViewById(R.id.button3);
            if (buttonShare != null) {
                buttonShare.setText(R.string.popup_action_share);
                UiTools.addClickEffect(buttonShare);
                buttonShare.getBackground().setAlpha(50);
                buttonShare.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.widget_calendar_popup_share_day), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonShare.setVisibility(View.VISIBLE);
            }

            //Выбрать цвет
            buttonSelectColor = findViewById(R.id.button4);
            if (buttonSelectColor != null) {
                buttonSelectColor.setText(R.string.popup_action_color);
                UiTools.addClickEffect(buttonSelectColor);
                buttonSelectColor.getBackground().setAlpha(50);
                buttonSelectColor.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.widget_calendar_popup_select_color), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonSelectColor.setVisibility(View.VISIBLE);
            }

            //Выбрать день
            buttonSelectDay = findViewById(R.id.button5);
            if (buttonSelectDay != null) {
                buttonSelectDay.setText(R.string.popup_action_calendar);
                UiTools.addClickEffect(buttonSelectDay);
                buttonSelectDay.getBackground().setAlpha(50);
                buttonSelectDay.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.widget_calendar_popup_select_day), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonSelectDay.setVisibility(View.VISIBLE);
            }

            //Следующий день
            buttonNextDay = findViewById(R.id.button6);
            if (buttonNextDay != null) {
                buttonNextDay.setText(R.string.popup_action_next);
                UiTools.addClickEffect(buttonNextDay);
                buttonNextDay.getBackground().setAlpha(50);
                buttonNextDay.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.widget_calendar_popup_next_day), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonNextDay.setVisibility(View.VISIBLE);
            }

            //Закрыть окно
            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

            executorService = Executors.newSingleThreadExecutor();

            setupClickListeners();
            showDayInfo();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void setupClickListeners() {
        //Календарь
        buttonCalendar.setOnClickListener(view -> {
            if (dayMills != null) {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath(Constants.QUERY_PARAM_TIME);
                builder.appendPath(dayMills);
                Intent intentCalendar = new Intent(Intent.ACTION_VIEW, builder.build());
                intentCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intentCalendar);
                    finish();
                } catch (ActivityNotFoundException ignored) { /**/ }
            }
        });
        //Поделиться
        buttonShare.setOnClickListener(v -> {
            if (!dayInfo.equals(getString(R.string.month_event_empty))) {
                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                intentShare.putExtra(Intent.EXTRA_TEXT,
                        viewCaption.getText().toString().concat(Constants.STRING_EOL)
                                .concat(viewInfo.getText().toString()));
                try {
                    startActivity(Intent.createChooser(intentShare, " "));
                } catch (ActivityNotFoundException ignored) { /**/ }
            }
        });

        //Выбрать день
        buttonSelectDay.setOnClickListener(v -> {
            if (dayMills != null) {
                long millis = Long.parseLong(dayMills);
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(millis);

                DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                        (view, selectedYear, selectedMonth, dayOfMonth) -> {
                            newCal.clear();
                            newCal.set(selectedYear, selectedMonth, dayOfMonth);
                            updateDayData(newCal);
                            showDayInfo();
                        },
                        newCal.get(Calendar.YEAR),
                        newCal.get(Calendar.MONTH),
                        newCal.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });

        //Выбрать цвет
        buttonSelectColor.setOnClickListener(v -> {
            if (dayMills != null) {
                long millis = Long.parseLong(dayMills);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);
                String date = Objects.requireNonNull(ContactsEvents.sdf_java.get()).format(cal.getTime());

                int colorDefaultValue = ContextCompat.getColor(this, android.R.color.transparent);
                int colorValue = colorDefaultValue;

                String storedColorValue = eventsData.getDayInfo(date);
                if (!storedColorValue.isEmpty()) {
                    try {
                        colorValue = Integer.parseInt(storedColorValue);
                    } catch (NumberFormatException ignored) { /**/ }
                }

                ColorPicker picker = new ColorPicker(
                        new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain)
                );
                picker.setDialogTitle(getString(R.string.widget_calendar_popup_select_color_title));
                picker.setDialogIcon(R.drawable.ic_menu_paste);
                picker.selectColor(colorValue, colorDefaultValue, date, (id, color) -> {
                    if (StringUtils.hasContent(id)) {
                        ToastExpander.showDebugMsg(getApplicationContext(),
                                getString(R.string.msg_event_color_selected, Integer.toHexString(color & 0x00ffffff), id));
                        int colorDefaultValue1 = ContextCompat.getColor(WidgetCalendarPopup.this, android.R.color.transparent);
                        eventsData.setDayInfo(id, color != colorDefaultValue1 ? String.valueOf(color) : null);
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    showDayInfo();
                });
            }
        });

        //Предыдущий день
        buttonPrevDay.setOnClickListener(v -> {
            if (dayMills != null) {
                long millis = Long.parseLong(dayMills);
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, -1);

                updateDayData(newCal);
                showDayInfo();
            }
        });

        //Следующий день
        buttonNextDay.setOnClickListener(v -> {
            if (dayMills != null) {
                long millis = Long.parseLong(dayMills);
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, +1);

                updateDayData(newCal);
                showDayInfo();
            }
        });
    }

    private void updateDayData(Calendar newCal) {
        SimpleDateFormat sdf = new SimpleDateFormat(" (EEE) ", Locale.getDefault());
        List<String> allEventsThisDay = eventsData.getDayInfo(
                Objects.requireNonNull(ContactsEvents.sdf_java.get()).format(newCal.getTime()),
                listEventsPacks,
                eventsColorsInMonth
        );
        // Аналогичный блок есть в WidgetCalendar#getAction
        if (!allEventsThisDay.isEmpty()) {
            //Подставляем в годовщину свадьбы её название
            final String weddingPrefix = Constants.eventTitleFavoritePrefix.concat(getString(R.string.event_type_anniversary));
            for (int i = 0; i < allEventsThisDay.size(); i++) {
                String event = allEventsThisDay.get(i);
                if (!event.contains(weddingPrefix)) continue;

                //Вытаскиваем год первоначального события
                int indParOpen = event.lastIndexOf(Constants.STRING_PARENTHESIS_OPEN);
                int indParClose = event.lastIndexOf(Constants.STRING_PARENTHESIS_CLOSE);
                if (indParOpen > -1 && indParClose > -1) {
                    String strYear = event.substring(indParOpen + Constants.STRING_PARENTHESIS_OPEN.length(), indParClose);
                    try {
                        int year = Integer.parseInt(strYear);
                        String anCaption = eventsData.getWeddingName(newCal.get(Calendar.YEAR) - year);
                        if (StringUtils.hasContent(anCaption)) {
                            allEventsThisDay.set(i, event.concat(Constants.STRING_PARENTHESIS_OPEN).concat(anCaption)
                                    .concat(Constants.STRING_PARENTHESIS_CLOSE));
                        }
                    } catch (NumberFormatException ignored) { /**/ }
                }
            }
        }

        dayInfo = allEventsThisDay.isEmpty()
                ? getString(R.string.month_event_empty)
                : TextUtils.join(Constants.HTML_BR, allEventsThisDay);
        dayCaption = getString(R.string.month_event_popup_prefix)
                .concat(eventsData.getDateFormatted(
                        Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(newCal.getTime()),
                        ContactsEvents.FormatDate.WithYear))
                .concat(sdf.format(newCal.getTime()));
        dayMills = Long.toString(newCal.getTimeInMillis());
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
                            //Заполнение типов дней из справочников
                            eventsData.fillDaysTypesFromHolidays(listEventsPacks, Constants.STRING_TYPE_HOLIDAY, Constants.eventSourceHolidayPrefix, Constants.eventTitleHolidayPrefix);
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
    /** Показывает информацию о дне */
    private void showDayInfo() {
        try {
            if (dayInfo.contains(Constants.TRANSPARENT)) {
                try (TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme)) {
                    dayInfo = dayInfo.replace(Constants.TRANSPARENT,
                            Integer.toHexString(ta.getColor(R.styleable.Theme_backgroundColor, 0) & 0x00ffffff));
                }
            }
            viewInfo.setText(HtmlCompat.fromHtml(dayInfo, HtmlCompat.FROM_HTML_MODE_LEGACY));

            if (!TextUtils.isEmpty(dayCaption)) {
                viewCaption.setText(dayCaption);
            }

            boolean isEmptyDay = dayInfo.equals(getString(R.string.month_event_empty));

            // Показать/скрыть кнопку Share
            buttonShare.setVisibility(isEmptyDay ? View.GONE : View.VISIBLE);

            if (dayMills != null) {
                long millis = Long.parseLong(dayMills);
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(millis);

                String date = Objects.requireNonNull(ContactsEvents.sdf_java.get()).format(newCal.getTime());
                String storedColorValue = eventsData.getDayInfo(date);

                int colorDefaultValue = ContextCompat.getColor(this, android.R.color.transparent);
                int colorValue = colorDefaultValue;

                if (!storedColorValue.isEmpty()) {
                    try {
                        colorValue = Integer.parseInt(storedColorValue);
                    } catch (NumberFormatException ignored) { /**/ }
                }

                if (colorValue != colorDefaultValue) {
                    buttonSelectColor.setText(HtmlCompat.fromHtml(
                            Constants.FONT_COLOR_DOT_START
                                    + Integer.toHexString(colorValue & 0x00ffffff)
                                    + Constants.FONT_COLOR_DOT_END
                                    + getString(R.string.popup_action_color),
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                    ));
                } else {
                    buttonSelectColor.setText(R.string.popup_action_color);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }
    /**
     Вспомогательный метод для безопасного извлечения HashMap с подавлением предупреждения
     @param intent Intent
     @return Карта
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