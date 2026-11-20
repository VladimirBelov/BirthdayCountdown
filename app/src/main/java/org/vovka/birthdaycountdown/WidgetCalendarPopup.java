/*
 * *
 *  * Created by Vladimir Belov on 20.11.2026, 13:23
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 20.11.2026, 13:20
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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.StringUtils;
import org.vovka.birthdaycountdown.utils.UiTools;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 `WidgetCalendarPopup` - это Activity, которое отображает всплывающее окно со списком событий
 выбранного дня из виджета календаря.
 <p>Каждое событие — отдельная строка списка. По нажатию открывается {@link WidgetMenuActivity}.</p>
 Ожидаемые extras:
 {@link AppWidgetManager#EXTRA_APPWIDGET_ID}, {@link Constants#EXTRA_DAY_INFO},
 {@link Constants#EXTRA_DAY_CAPTION}, {@link Constants#EXTRA_VALUES},
 {@link Constants#EXTRA_LIST}, {@link Constants#EXTRA_MAP},
 {@link Constants#EXTRA_DAY1}, {@link Constants#EXTRA_DAY2}
 */
public class WidgetCalendarPopup extends Activity {

    private static final String TAG = "WidgetCalendarPopup";

    ContactsEvents eventsData;
    Intent intent;
    TextView viewCaption;
    TextView viewInfo;              //заглушка "нет событий"
    ListView listEvents;
    TextView buttonCalendar;
    TextView buttonShare;
    TextView buttonPrevDay;
    TextView buttonSelectDay;
    TextView buttonSelectColor;
    TextView buttonNextDay;

    private DayEventsAdapter adapter;
    private final List<EventRow> dayEvents = new ArrayList<>();

    private int appWidgetId = 0;
    private String dayInfo = null;
    private String dayCaption = null;
    private String dayMills = null;
    private ArrayList<String> listEventsPacks;
    private HashMap<String, Integer> eventsColorsInMonth = null;
    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int REQ_EVENT_MENU = 1001;
    private boolean dayTypesReady = false;
    public WidgetCalendarPopup() {
    }

    /** Строка списка: сырое событие для меню + HTML для показа */
    private static class EventRow {
        @Nullable final String rawEvent;
        @NonNull String displayHtml;
        @Nullable Bitmap photo = null;
        boolean photoLoaded = false;

        EventRow(@Nullable String rawEvent, @NonNull String displayHtml) {
            this.rawEvent = rawEvent;
            this.displayHtml = displayHtml;
        }
    }

    /** Адаптер списка событий дня */
    private class DayEventsAdapter extends ArrayAdapter<EventRow> {

        DayEventsAdapter() {
            super(WidgetCalendarPopup.this, R.layout.widget_calendar_popup_item, dayEvents);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            EventRow row = getItem(position);
            return row != null && row.rawEvent != null; //без данных о событии меню не открыть
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView != null ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.widget_calendar_popup_item, parent, false);
            try {
                EventRow row = getItem(position);
                if (row != null) {
                    TextView textEvent = view.findViewById(R.id.textEvent);
                    ImageView imagePhoto = view.findViewById(R.id.imageEventPhoto);
                    textEvent.setText(HtmlCompat.fromHtml(row.displayHtml, HtmlCompat.FROM_HTML_MODE_LEGACY));

                    if (!row.photoLoaded) {
                        if (row.rawEvent != null) {
                            String[] singleEventArray = row.rawEvent.split(Constants.STRING_EOT, -1);
                            boolean isHoliday = singleEventArray.length > ContactsEvents.Position_eventType
                                    && Constants.EventType_Holiday.equals(singleEventArray[ContactsEvents.Position_eventType]);
                            if (!isHoliday) {
                                row.photo = eventsData.getEventPhoto(row.rawEvent, true, true, false, 0);
                            }
                        }
                        row.photoLoaded = true;
                    }
                    if (row.photo != null) {
                        imagePhoto.setImageBitmap(row.photo);
                        imagePhoto.setVisibility(View.VISIBLE);
                    } else {
                        imagePhoto.setImageDrawable(null);
                        imagePhoto.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return view;
        }
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
            if (listEventsPacks == null) listEventsPacks = new ArrayList<>();
            eventsColorsInMonth = getHashMapFromIntent(intent);

            if (dayInfo == null || dayMills == null) {
                ToastExpander.showInfoMsg(getApplicationContext(), "No extras!");
                finish();
                return;
            }

            viewInfo = findViewById(R.id.textInfo);
            viewCaption = findViewById(R.id.textCaption);

            //Список событий дня
            listEvents = findViewById(R.id.listEvents);
            adapter = new DayEventsAdapter();
            listEvents.setAdapter(adapter);
            listEvents.setEmptyView(viewInfo);
            viewInfo.setText(R.string.month_event_empty);
            listEvents.setOnItemClickListener((parent, view, position, id) -> openEventMenu(position));

            //Первичное заполнение из intent (до отработки onResume) — строки ещё без ключей
            if (!dayInfo.equals(getString(R.string.month_event_empty))) {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(Long.parseLong(dayMills));
                    List<String> texts = new ArrayList<>(Arrays.asList(
                            dayInfo.split(Pattern.quote(Constants.HTML_BR), -1)));
                    dayEvents.clear();
                    for (String text : texts) dayEvents.add(new EventRow(null, text));
                } catch (NumberFormatException ignored) { /**/ }
            }

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
                if (DeviceTools.isAppActionAvailable(this, DeviceTools.AppActionIcon.OPEN_CALENDAR)) {
                    Drawable icon = DeviceTools.getAppActionIcon(
                            this,
                            DeviceTools.AppActionIcon.OPEN_CALENDAR,
                            android.R.drawable.ic_menu_month
                    );
                    if (icon != null) {
                        int iconSize = (int) (20 * getResources().getDisplayMetrics().density);
                        icon.setBounds(0, 0, iconSize, iconSize);
                        buttonCalendar.setCompoundDrawables(icon, null, null, null);
                        buttonCalendar.setCompoundDrawablePadding(
                                (int) (4 * getResources().getDisplayMetrics().density)
                        );
                    }
                    buttonCalendar.setText(" ".concat(getString(R.string.appwidget_label_Calendar)));
                    UiTools.addClickEffect(buttonCalendar);
                    buttonCalendar.getBackground().setAlpha(50);
                    buttonCalendar.setOnLongClickListener(v -> {
                        Toast.makeText(this, getString(R.string.widget_calendar_popup_open_calendar), Toast.LENGTH_LONG).show();
                        return true;
                    });
                    buttonCalendar.setVisibility(View.VISIBLE);
                } else {
                    buttonCalendar.setVisibility(View.GONE);
                }
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

            showDayInfo();
            setupClickListeners();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + ": " + e);
        }
    }

    /** Открывает меню события (как в остальных виджетах) */
    private void openEventMenu(int position) {
        try {
            EventRow row = adapter.getItem(position);
            if (row == null || row.rawEvent == null) return;

            Intent menuIntent = new Intent(this, WidgetMenuActivity.class);
            menuIntent.setAction(Constants.ACTION_MENU);
            menuIntent.putExtra(Constants.EXTRA_CLICKED_EVENT, row.rawEvent);
            menuIntent.putExtra(Constants.EXTRA_CLICKED_TEXT, row.displayHtml);
            menuIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            try {
                startActivityForResult(menuIntent, REQ_EVENT_MENU);
            } catch (ActivityNotFoundException ignored) { /**/ }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + ": " + e);
        }
    }

    private void setupClickListeners() {
        try {
            //Календарь
            if (buttonCalendar != null && DeviceTools.isAppActionAvailable(this, DeviceTools.AppActionIcon.OPEN_CALENDAR)) {
                buttonCalendar.setOnClickListener(view -> {
                    if (dayMills != null) {
                        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                        builder.appendPath(Constants.QUERY_PARAM_TIME);
                        Calendar calForUri = Calendar.getInstance();
                        calForUri.setTimeInMillis(Long.parseLong(dayMills));
                        AppDateUtils.clearTime(calForUri);
                        builder.appendPath(Long.toString(calForUri.getTimeInMillis()));
                        Intent intentCalendar = new Intent(Intent.ACTION_VIEW, builder.build());
                        intentCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        String calendarPackage = DeviceTools.getAppActionPackageName(
                                this, DeviceTools.AppActionIcon.OPEN_CALENDAR
                        );
                        if (calendarPackage != null) {
                            intentCalendar.setPackage(calendarPackage);
                        }
                        try {
                            startActivity(intentCalendar);
                            finish();
                        } catch (ActivityNotFoundException e) {
                            try {
                                Intent fallbackIntent = new Intent(Intent.ACTION_MAIN);
                                fallbackIntent.addCategory(Intent.CATEGORY_APP_CALENDAR);
                                fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(fallbackIntent);
                                finish();
                            } catch (ActivityNotFoundException ignored) {
                                ToastExpander.showInfoMsg(getApplicationContext(), getString(R.string.msg_no_calendar));
                            }
                        }
                    }
                });
            }

            //Поделиться
            if (buttonShare != null) {
                buttonShare.setOnClickListener(v -> {
                    if (!dayEvents.isEmpty()) {
                        Intent intentShare = new Intent(Intent.ACTION_SEND);
                        intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                        intentShare.putExtra(Intent.EXTRA_TEXT,
                                viewCaption.getText().toString().concat("\n")
                                        .concat(HtmlCompat.fromHtml(dayInfo, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()));
                        try {
                            startActivity(Intent.createChooser(intentShare, " "));
                        } catch (ActivityNotFoundException ignored) { /**/ }
                    }
                });
            }

            //Выбрать день
            if (buttonSelectDay != null) {
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
            }

            //Выбрать цвет
            if (buttonSelectColor != null) {
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
                        picker.selectColor(colorValue, colorDefaultValue, true, date, (id, color) -> {
                            if (StringUtils.hasContent(id)) {
                                ToastExpander.showDebugMsg(getApplicationContext(),
                                        getString(R.string.msg_event_color_selected, String.format("%06x", (color & 0x00ffffff)), id));
                                int colorDefaultValue1 = ContextCompat.getColor(WidgetCalendarPopup.this, android.R.color.transparent);
                                eventsData.setDayInfo(id, color != colorDefaultValue1 ? String.valueOf(color) : null);
                                eventsData.updateWidgets(appWidgetId, null);
                            }
                            showDayInfo();
                        });
                    }
                });
            }

            //Предыдущий день
            if (buttonPrevDay != null) {
                buttonPrevDay.setOnClickListener(v -> {
                    if (dayMills != null) {
                        Calendar newCal = Calendar.getInstance();
                        newCal.setTimeInMillis(Long.parseLong(dayMills));
                        newCal.add(Calendar.DAY_OF_YEAR, -1);
                        updateDayData(newCal);
                        showDayInfo();
                    }
                });
            }

            //Следующий день
            if (buttonNextDay != null) {
                buttonNextDay.setOnClickListener(v -> {
                    if (dayMills != null) {
                        Calendar newCal = Calendar.getInstance();
                        newCal.setTimeInMillis(Long.parseLong(dayMills));
                        newCal.add(Calendar.DAY_OF_YEAR, +1);
                        updateDayData(newCal);
                        showDayInfo();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + ": " + e);
        }
    }

    /** Однократное заполнение типов дней: избранные, календари, справочники, файлы */
    private void prepareDayTypes() {
        if (executorService == null || executorService.isShutdown()) return;
        executorService.execute(() -> {
            try {
                if (eventsData.isEmptyEventList()) eventsData.getEvents();
                eventsData.fillDayTypesForCalendarWidget();

                if (intent != null && intent.hasExtra(Constants.EXTRA_DAY1) && intent.hasExtra(Constants.EXTRA_DAY2)) {
                    Calendar calFirstDay = (Calendar) intent.getSerializableExtra(Constants.EXTRA_DAY1);
                    Calendar calLastDay  = (Calendar) intent.getSerializableExtra(Constants.EXTRA_DAY2);
                    if (calFirstDay != null && calLastDay != null) {
                        eventsData.fillDaysTypesFromCalendars(listEventsPacks, calFirstDay, calLastDay);
                    }
                }
                eventsData.fillDaysTypesFromHolidays(listEventsPacks, Constants.STRING_TYPE_HOLIDAY,
                        Constants.eventSourceHolidayPrefix, Constants.eventTitleHolidayPrefix);
                eventsData.fillDaysTypesFromHolidays(listEventsPacks, Constants.STRING_TYPE_OTHER_HOLIDAY,
                        Constants.eventSourceHolidayPrefix, Constants.eventTitleHolidayPrefix);
                eventsData.fillDaysTypesFromFiles(listEventsPacks);

                dayTypesReady = true;

                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    rebuildCurrentDay();
                });
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    /** Пересборка только текущего дня — как при нажатии стрелок */
    private void rebuildCurrentDay() {
        if (dayMills == null) return;
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(Long.parseLong(dayMills));
            updateDayData(cal);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        showDayInfo();
    }

    /** Пересчитывает данные дня: тексты событий + сырые события для меню */
    private void updateDayData(Calendar cal) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(" (EEE) ", Locale.getDefault());
            final String dateJava = Objects.requireNonNull(ContactsEvents.sdf_java.get()).format(cal.getTime());
            List<ContactsEvents.DayEvent> allEventsThisDay =
                    eventsData.getDayEvents(dateJava, listEventsPacks, eventsColorsInMonth);

            // Аналогичный блок есть в WidgetCalendar#getAction
            if (!allEventsThisDay.isEmpty()) {
                final String weddingPrefix = Constants.eventTitleFavoritePrefix.concat(getString(R.string.event_type_anniversary));
                final String birthdayPrefix = Constants.eventTitleFavoritePrefix.concat(getString(R.string.event_type_birthday));
                for (ContactsEvents.DayEvent dayEvent : allEventsThisDay) {
                    String event = dayEvent.text;
                    int indParOpen = event.lastIndexOf(" (");
                    int indParClose = event.lastIndexOf(")");
                    if (indParOpen > -1 && indParClose > -1) {
                        int indMinus = event.indexOf("-", indParOpen);
                        if (event.contains(weddingPrefix)) {
                            //Подставляем в годовщину свадьбы её название
                            String strYear = event.substring(indParOpen + " (".length(), indParClose);
                            try {
                                int year = Integer.parseInt(strYear);
                                String anCaption = StringUtils.getWeddingName(cal.get(Calendar.YEAR) - year,
                                        eventsData.getContext(), eventsData.getResources());
                                if (StringUtils.hasContent(anCaption)) {
                                    dayEvent.text = event.concat(" (").concat(anCaption).concat(")");
                                }
                            } catch (NumberFormatException ignored) { /**/ }
                        } else if (indMinus == -1 && !event.contains(birthdayPrefix)) {
                            //Если событие на один день - удаляем год в скобках
                            dayEvent.text = event.substring(0, indParOpen);
                        }
                    }
                }
            }

            //Сопоставление с реальными событиями (одним проходом по общему списку)
            final Set<String> keys = new HashSet<>();
            final List<String> texts = new ArrayList<>();
            for (ContactsEvents.DayEvent dayEvent : allEventsThisDay) {
                texts.add(dayEvent.text);
                if (StringUtils.hasContent(dayEvent.eventKey)) keys.add(dayEvent.eventKey);
            }
            final Map<String, String> resolved = eventsData.findEventsByKeys(keys);
            final String dateNext = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime());

            dayEvents.clear();
            final Set<String> addedKeys = new HashSet<>();

            for (ContactsEvents.DayEvent dayEvent : allEventsThisDay) {
                // Пропускаем дубликаты
                if (StringUtils.hasContent(dayEvent.eventKey) && !addedKeys.add(dayEvent.eventKey)) {
                    continue;
                }

                String raw = resolved.get(dayEvent.eventKey);
                if (raw == null && StringUtils.hasContent(dayEvent.eventKey)) {
                    raw = eventsData.makeDayEventInfo(dayEvent.eventKey, toPlainTitle(dayEvent.text), dateNext);
                }
                dayEvents.add(new EventRow(raw, dayEvent.text));
            }

            dayInfo = texts.isEmpty()
                    ? getString(R.string.month_event_empty)
                    : TextUtils.join(Constants.HTML_BR, texts);

            dayCaption = getString(R.string.month_event_popup_prefix)
                    .concat(AppDateUtils.getDateFormatted(
                            Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(cal.getTime()),
                            ContactsEvents.FormatDate.WithYear, eventsData.preferences_date_format, eventsData.getContext(),
                            eventsData.getResources(), eventsData.currentLocale))
                    .concat(sdf.format(cal.getTime()));

            dayMills = Long.toString(cal.getTimeInMillis());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + ": " + e);
        }
    }

    /** HTML события -> заголовок без цветной точки источника */
    @NonNull
    private String toPlainTitle(@NonNull String html) {
        String plain = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim();
        if (plain.startsWith("●")) plain = plain.substring(1).trim();
        return plain;
    }

    /** Подставляет цвет фона темы вместо заглушки "transparent" (без TypedArray.close() — он только с API 31) */
    @NonNull
    private String replaceTransparent(@NonNull String html) {
        if (!html.contains(Constants.TRANSPARENT)) return html;
        TypedArray ta = null;
        try {
            ta = getTheme().obtainStyledAttributes(R.styleable.Theme);
            return html.replace(Constants.TRANSPARENT,
                    String.format("%06x", (ta.getColor(R.styleable.Theme_backgroundColor, 0) & 0x00ffffff)));
        } finally {
            if (ta != null) ta.recycle();
        }
    }

    @Override
    public void onResume() {
        try {
            super.onResume();
            if (!dayTypesReady) {
                prepareDayTypes();  // выполнится и при первом запуске, и после изменений
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + ": " + e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EVENT_MENU && resultCode == RESULT_OK) {
            dayTypesReady = false;   // обработается в onResume
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
            if (dayInfo != null) dayInfo = replaceTransparent(dayInfo);
            for (EventRow row : dayEvents) row.displayHtml = replaceTransparent(row.displayHtml);
            adapter.notifyDataSetChanged(); //видимость списка/заглушки разруливает setEmptyView()

            if (!TextUtils.isEmpty(dayCaption)) {
                viewCaption.setText(dayCaption);
            }

            boolean isEmptyDay = dayEvents.isEmpty();
            if (buttonShare != null) {
                buttonShare.setVisibility(isEmptyDay ? View.GONE : View.VISIBLE);
            }

            if (buttonSelectColor != null && dayMills != null) {
                Calendar newCal = Calendar.getInstance();
                newCal.setTimeInMillis(Long.parseLong(dayMills));
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
                                    + String.format("%06x", (colorValue & 0x00ffffff))
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
            ToastExpander.showDebugMsg(this, StringUtils.getMethodName(3) + ": " + e);
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