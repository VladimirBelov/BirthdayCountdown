/*
 * *
 *  * Created by Vladimir Belov on 21.03.2026, 02:21
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 21.03.2026, 01:24
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
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
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
 *   <li>Предоставление действий, таких как просмотр дня в приложении календаря или обмен информацией о дне.</li>
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
    TextView buttonSelectColor;
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
            eventsData.initLanguage(this);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.widget_calendar_popup);

            intent = getIntent();
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
                    Toast.makeText(this, getString(R.string.previous_day), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, getString(R.string.open_calendar), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, getString(R.string.share_day), Toast.LENGTH_LONG).show();
                    return true;
                });
                buttonShare.setVisibility(View.VISIBLE);
            }

            //Выбрать цвет
            buttonSelectColor = findViewById(R.id.button4);
            if (buttonSelectColor != null) {
                buttonSelectColor.setText(R.string.popup_action_color); //todo: сделать цветом текущего дня, если задан
                UiTools.addClickEffect(buttonSelectColor);
                buttonSelectColor.getBackground().setAlpha(50);
                buttonSelectColor.setOnLongClickListener(v -> {
                    Toast.makeText(this, getString(R.string.select_color), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, getString(R.string.select_day), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, getString(R.string.next_day), Toast.LENGTH_LONG).show();
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
            if (dayInfo.contains(Constants.TRANSPARENT)) {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                dayInfo = dayInfo.replace(Constants.TRANSPARENT,
                        Integer.toHexString(ta.getColor(R.styleable.Theme_backgroundColor, 0) & 0x00ffffff));
                ta.recycle();
            }
            viewInfo.setText(HtmlCompat.fromHtml(dayInfo, HtmlCompat.FROM_HTML_MODE_LEGACY));

            if (!TextUtils.isEmpty(dayCaption)) {
                viewCaption.setText(dayCaption);
            }

            boolean isEmptyDay = dayInfo.equals(getString(R.string.month_event_empty));

            buttonCalendar.setOnClickListener(view -> {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath(Constants.QUERY_PARAM_TIME);
                builder.appendPath(dayMills);
                Intent intentCalendar = new Intent(Intent.ACTION_VIEW, builder.build());
                intentCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentCalendar);
                finish();
            });

            if (isEmptyDay) {
                buttonShare.setVisibility(View.GONE);
            } else {
                buttonShare.setOnClickListener(v -> {
                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                    intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    intentShare.putExtra(Intent.EXTRA_TEXT,
                            viewCaption.getText().toString().concat(Constants.STRING_EOL).concat(viewInfo.getText().toString()));
                    startActivity(Intent.createChooser(intentShare, ""));
                });
                buttonShare.setVisibility(View.VISIBLE);
            }

            long millis = Long.parseLong(dayMills);
            Calendar newCal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(" (EEE)", Locale.getDefault());

            buttonSelectDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, dayOfMonth) -> {
                    newCal.clear();
                    newCal.set(selectedYear, selectedMonth, dayOfMonth);

                    List<String> allEventsThisDay = eventsData.getDayInfo(ContactsEvents.sdf_java.format(newCal.getTime()), listEventsPacks, eventsColorsInMonth);
                    dayInfo = allEventsThisDay.isEmpty() ? getString(R.string.month_event_empty) : TextUtils.join(Constants.HTML_BR, allEventsThisDay);
                    dayCaption = getString(R.string.month_event_popup_prefix)
                            .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                            .concat(sdf.format(newCal.getTime()));
                    dayMills = Long.toString(newCal.getTimeInMillis());

                    showDayInfo();
                }, newCal.get(Calendar.YEAR), newCal.get(Calendar.MONTH), newCal.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            });

            buttonSelectColor.setOnClickListener( v -> {
                ColorPicker picker = new ColorPicker(new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain));
                picker.setDialogTitle("Цвет дня");
                picker.setDialogIcon(R.drawable.ic_menu_paste);
                Integer colorValue = ContextCompat.getColor(this, android.R.color.transparent);
                picker.selectColor(colorValue, colorValue, null, null);
            });

            buttonPrevDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, -1);

                List<String> allEventsThisDay = eventsData.getDayInfo(ContactsEvents.sdf_java.format(newCal.getTime()), listEventsPacks, eventsColorsInMonth);
                dayInfo = allEventsThisDay.isEmpty() ? getString(R.string.month_event_empty) : TextUtils.join(Constants.HTML_BR, allEventsThisDay);
                dayCaption = getString(R.string.month_event_popup_prefix)
                        .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                        .concat(sdf.format(newCal.getTime()));
                dayMills = Long.toString(newCal.getTimeInMillis());

                showDayInfo();
            });

            buttonNextDay.setOnClickListener(v -> {
                newCal.setTimeInMillis(millis);
                newCal.add(Calendar.DAY_OF_YEAR, +1);

                List<String> allEventsThisDay = eventsData.getDayInfo(ContactsEvents.sdf_java.format(newCal.getTime()), listEventsPacks, eventsColorsInMonth);
                dayInfo = allEventsThisDay.isEmpty() ? getString(R.string.month_event_empty) : TextUtils.join(Constants.HTML_BR, allEventsThisDay);
                dayCaption = getString(R.string.month_event_popup_prefix)
                        .concat(eventsData.getDateFormatted(ContactsEvents.sdf_DDMMYYYY.format(newCal.getTime()), ContactsEvents.FormatDate.WithYear))
                        .concat(sdf.format(newCal.getTime()));
                dayMills = Long.toString(newCal.getTimeInMillis());

                showDayInfo();
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
