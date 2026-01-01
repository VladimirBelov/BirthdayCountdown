/*
 * *
 *  * Created by Vladimir Belov on 01.01.2026, 21:25
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 01.01.2026, 18:01
 *
 */

package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import org.vovka.birthdaycountdown.imagecropper.CropIntent;
import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;
import org.vovka.birthdaycountdown.utils.UiTools;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс `LocalEventActivity` - это Activity, отвечающее за управление созданием, редактированием и просмотром локальных событий.
 * Он предоставляет пользовательский интерфейс для ввода деталей события, таких как имя, заголовок, организация, дата и тип события.
 *
 * <p>
 *   Это Activity поддерживает различные действия, включая:
 *   <ul>
 *     <li>Создание нового локального события.</li>
 *     <li>Редактирование существующего локального события.</li>
 *     <li>Просмотр деталей локального события (только для чтения).</li>
 *     <li>Выбор даты события с возможностью указать или скрыть год.</li>
 *     <li>Выбор типа события из предопределенного списка.</li>
 *     <li>Сохранение и удаление локальных событий.</li>
 *     <li>Обработку изменений в полях ввода.</li>
 *     <li>Поддержку различных языков и тем оформления.</li>
 *     <li>Динамическое изменение ширины диалогового окна в зависимости от режима (редактирование/просмотр).</li>
 */
public class LocalEventActivity extends AppCompatActivity {

    private static final String TAG = "LocalEventActivity";
    private static final ContactsEvents eventsData;
    private static final TreeMap<Integer, String> eventData = new TreeMap<>();
    private static int eventYear;
    private static int eventMonth;
    private static int eventDay;
    private static boolean eventUseYear = true;
    private static boolean eventIsBC = false;
    private static final List<String> eventTypesValues = new ArrayList<>();
    private static final List<Integer> eventTypesIds = new ArrayList<>();
    private static final List<Integer> eventSubTypesIds = new ArrayList<>();
    private boolean isReadOnly;
    private String eventDataSaved = null;

    TextView viewActivityTitle;
    ImageView imagePhoto;
    TextView viewName;
    EditText editName;
    CardView cardTitle;
    TextView viewTitle;
    EditText editTitle;
    CardView cardOrganization;
    TextView viewOrganization;
    EditText editOrganization;
    TextView editDate;
    TextView viewEventType;
    Spinner spinnerEventTypes;
    CardView cardDescription;
    TextView viewDescription;
    EditText editDescription;
    CardView cardURL;
    TextView viewURL;
    EditText editURL;
    Button buttonPickContactData;
    Button buttonPickPhoto;
    Button buttonClearPhoto;

    static {
        eventsData = ContactsEvents.getInstance();
    }

    public LocalEventActivity() {}

    public static class DatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private static final String TAG = "DatePicker";
        View viewActivity = null;
        View spinnerYear = null;
        final AtomicInteger selectedYear = new AtomicInteger();
        final AtomicInteger selectedMonth = new AtomicInteger();
        final AtomicInteger selectedDay = new AtomicInteger();
        final AtomicBoolean useYear = new AtomicBoolean(true);
        final AtomicBoolean isBC = new AtomicBoolean(false);

        @SuppressLint("InflateParams")
        @Nullable
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            try {

                AtomicInteger yearBeforeHide = new AtomicInteger(0);
                final Calendar today = Calendar.getInstance();

                Bundle bundle = getArguments();
                if (bundle.containsKey(Constants.EXTRA_DAY)) {
                    selectedYear.set(bundle.getInt(Constants.EXTRA_YEAR));
                    selectedMonth.set(bundle.getInt(Constants.EXTRA_MONTH));
                    selectedDay.set(bundle.getInt(Constants.EXTRA_DAY));
                    useYear.set(bundle.getBoolean(Constants.EXTRA_USE_YEAR));
                    isBC.set(bundle.getBoolean(Constants.EXTRA_IS_BC));
                } else {
                    selectedYear.set(today.get(Calendar.YEAR));
                    selectedMonth.set(today.get(Calendar.MONTH));
                    selectedDay.set(today.get(Calendar.DAY_OF_MONTH));
                    useYear.set(true);
                }

                getActivity().setTheme(eventsData.preferences_theme.themeDialog);
                viewActivity = getActivity().getLayoutInflater().inflate(R.layout.datepicker, null);

                TextView editDate = getActivity().findViewById(R.id.editDate);
                CheckBox checkUseYear = viewActivity.findViewById(R.id.check_use_year);
                CheckBox checkIsBC = viewActivity.findViewById(R.id.check_bc);

                checkIsBC.setChecked(isBC.get());

                android.widget.DatePicker datePicker = viewActivity.findViewById(R.id.datePicker);
                //В разных версиях Android этот spinner назывался по-разному. Попробуем найти его
                @SuppressLint("DiscouragedApi")
                int yearSpinnerId = getResources().getIdentifier(Constants.RES_TYPE_YEAR, Constants.RES_TYPE_ID, Constants.RES_PACKAGE_ANDROID);
                if (yearSpinnerId!=0){
                    spinnerYear = datePicker.findViewById(yearSpinnerId);
                }
                Calendar minDate = Calendar.getInstance();
                minDate.set(1, Calendar.JANUARY, 1); //java.util.Calendar не поддерживает годы до нашей эры (0 или отрицательные годы)
                datePicker.setMinDate(minDate.getTimeInMillis());
                datePicker.init(selectedYear.get(), selectedMonth.get(), selectedDay.get(), (
                                view, year, monthOfYear, dayOfMonth) -> {
                            selectedYear.set(year);
                            selectedMonth.set(monthOfYear);
                            selectedDay.set(dayOfMonth);
                            useYear.set(checkUseYear.isChecked());
                            isBC.set(checkIsBC.isChecked());
                        }
                );
                if (spinnerYear != null) {
                    if (useYear.get()) {
                        spinnerYear.setVisibility(View.VISIBLE);
                        checkUseYear.setChecked(true);
                        checkIsBC.setVisibility(View.VISIBLE);
                    } else {
                        spinnerYear.setVisibility(View.GONE);
                        checkUseYear.setChecked(false);
                        yearBeforeHide.set(today.get(Calendar.YEAR));
                        checkIsBC.setVisibility(View.GONE);
                    }
                }
                datePicker.post(() -> handleDateChanged(checkIsBC.isChecked(), selectedYear.get(), selectedMonth.get(), selectedDay.get()));

                checkUseYear.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        datePicker.updateDate(yearBeforeHide.get() != 0 ? yearBeforeHide.get() : today.get(Calendar.YEAR), datePicker.getMonth(), datePicker.getDayOfMonth());
                        useYear.set(true);
                        spinnerYear.setVisibility(View.VISIBLE);
                        checkIsBC.setVisibility(View.VISIBLE);
                    } else {
                        yearBeforeHide.set(datePicker.getYear());
                        useYear.set(false);
                        datePicker.updateDate(today.get(Calendar.YEAR), datePicker.getMonth(), datePicker.getDayOfMonth());
                        spinnerYear.setVisibility(View.GONE);
                        isBC.set(false);
                        checkIsBC.setVisibility(View.GONE);
                    }
                    handleDateChanged(isBC.get(), selectedYear.get(), selectedMonth.get(), selectedDay.get());
                });

                checkIsBC.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    isBC.set(isChecked);
                    handleDateChanged(isBC.get(), selectedYear.get(), selectedMonth.get(), selectedDay.get());
                });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    datePicker.setOnDateChangedListener((view, year, monthOfYear, dayOfMonth) -> handleDateChanged(checkIsBC.isChecked(), year, monthOfYear, dayOfMonth));
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setView(viewActivity)
                        .setTitle(R.string.local_event_date_picker_title)
                        .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                            updateEventDate(editDate, selectedDay.get(), selectedMonth.get(), selectedYear.get(), useYear.get(), isBC.get());
                            updateEventPhoto((LocalEventActivity) getActivity());
                        })
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dismiss());

                return builder.create();

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getActivity(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                return null;
            }
        }

        private void handleDateChanged(boolean isBC, int year, int month, int day) {
            selectedYear.set(year);
            selectedMonth.set(month);
            selectedDay.set(day);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //Почему-то на младших android не обновляет view
                TextView textWeekDay = viewActivity.findViewById(R.id.week_day);
                if (useYear.get()) {
                    textWeekDay.setVisibility(View.VISIBLE);
                    int dow = DayOfWeekCalculator.getDayOfWeek(isBC, year, month + 1, day);
                    String name = DayOfWeekCalculator.getDayName(dow, Locale.getDefault());
                    textWeekDay.setText(getResources().getString(R.string.local_event_date_picker_week_day,
                            StringUtils.toProperCase(name)));
                } else {
                    textWeekDay.setVisibility(View.INVISIBLE);
                }
            }
        }

        public static DatePicker newInstance() {
            return new DatePicker();
        }

        @Override
        public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {}
    }

    static class DayOfWeekCalculator {

        /** Вычисляет день недели для заданной даты по алгоритму Зеллера
         * @param year  Положительный — н.э., отрицательный или 0 — до н.э.
         *              Например: 1900 = 1900 н.э., 0 = 1 г. до н.э., -1 = 2 г. до н.э.
         *              Но для удобства ввода используем другой метод (см. ниже).
         * @param month 1–12 (январь = 1)
         * @param day   1–31
         * @return день недели: 1 = воскресенье, 2 = понедельник, ..., 7 = суббота (как в Calendar)
         */
        public static int getDayOfWeek(int year, int month, int day) {
            // Определяем, какой календарь использовать
            boolean isGregorian = isGregorianDate(year, month, day);

            // Алгоритм Зеллера (универсальный)
            if (month <= 2) {
                month += 12;
                year--;
            }

            int K = year % 100;
            int J = year / 100;
            int h;
            if (isGregorian) {
                h = day + (13 * (month + 1)) / 5 + K + (K / 4) + (J / 4) + 5 * J;
            } else {
                h = day + (13 * (month + 1)) / 5 + K + (K / 4) + 5 + 6 * J;
            }
            h = h % 7;
            if (h < 0) h += 7;

            // h: 0 = суббота, 1 = воскресенье, 2 = понедельник, ..., 6 = пятница
            int[] zellerToCalendar = {7, 1, 2, 3, 4, 5, 6}; // SAT=7, SUN=1, MON=2, ...
            return zellerToCalendar[h];
        }

        /** Проверяет, используется ли григорианский календарь для даты.
         * Григорианский календарь введён 15 октября 1582 г.
         * Всё до 1582-10-04 — юлианский.
         * 1582-10-05 — 1582-10-14 — не существуют (пропущены).
         */
        private static boolean isGregorianDate(int year, int month, int day) {
            if (year < 1582) return false;
            if (year > 1582) return true;
            if (month < 10) return false;
            if (month > 10) return true;
            return day >= 15; // 15 октября 1582 и позже — григорианский
        }

        /** Удобный метод для вызова с исторической нотацией
         * @param yearBC если true, то year — это год до н.э. (1 = 1 г. до н.э.)
         * @param year    год (положительное число)
         * @param month   1–12
         * @param day     1–31
         * @return день недели (1–7, как в Calendar)
         */
        public static int getDayOfWeek(boolean yearBC, int year, int month, int day) {
            if (year <= 0) {
                throw new IllegalArgumentException("Year must be positive");
            }
            int prolepticYear = yearBC ? 1 - year : year;
            return getDayOfWeek(prolepticYear, month, day);
        }

        /** Возвращает краткое название дня на локальном языке
         * @param dayOfWeek Номер дня (1 - воскресенье)
         * @param locale Локаль
         * @return название для
         */
        static String getShortDayName(int dayOfWeek, Locale locale) {
            return new DateFormatSymbols(locale).getShortWeekdays()[dayOfWeek];
        }

        /** Возвращает название дня на локальном языке
         * @param dayOfWeek Номер дня (1 - воскресенье)
         * @param locale Локаль
         * @return название для
         */
        static String getDayName(int dayOfWeek, Locale locale) {
            return new DateFormatSymbols(locale).getWeekdays()[dayOfWeek];
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);

            eventsData.initLanguage(this);

            setTheme(eventsData.preferences_theme.themeMain);
            this.getTheme().applyStyle(R.style.FloatingActivity, true);
            setContentView(R.layout.activity_event);

            viewActivityTitle = findViewById(R.id.textCaption);
            viewName = findViewById(R.id.captionName);
            editName = findViewById(R.id.editName);
            cardTitle = findViewById(R.id.cardTitle);
            viewTitle = findViewById(R.id.captionTitle);
            editTitle = findViewById(R.id.editTitle);
            cardOrganization = findViewById(R.id.cardOrganization);
            viewOrganization = findViewById(R.id.captionOrganization);
            editOrganization = findViewById(R.id.editOrganization);
            editDate = findViewById(R.id.editDate);
            spinnerEventTypes = findViewById(R.id.spinnerEventType);
            viewEventType = findViewById(R.id.viewEventType);
            cardDescription = findViewById(R.id.cardDescription);
            viewDescription = findViewById(R.id.captionDescription);
            editDescription = findViewById(R.id.editDescription);
            cardURL = findViewById(R.id.cardUrl);
            viewURL = findViewById(R.id.captionUrl);
            editURL = findViewById(R.id.editUrl);
            imagePhoto = findViewById(R.id.imagePhoto);
            buttonPickContactData = findViewById(R.id.buttonPickContactData);
            buttonPickPhoto = findViewById(R.id.buttonPickPhoto);
            buttonClearPhoto = findViewById(R.id.buttonClearPhoto);

            eventsData.initEventTypes(eventTypesValues, eventTypesIds, eventSubTypesIds);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, eventTypesValues);
            spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerEventTypes.setAdapter(spinnerArrayAdapter);

            final Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            boolean useYear = true;
            boolean isBC = false;

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            final String action = intent.getAction();
            isReadOnly = Intent.ACTION_VIEW.equals(action);

            if (Intent.ACTION_INSERT.equals(action)) {

                eventData.put(ContactsEvents.Position_eventID, StringUtils.getHash(String.valueOf(cal.getTimeInMillis())));
                viewActivityTitle.setText(R.string.local_event_dialog_title_new_event);

            } else if (Intent.ACTION_INSERT_OR_EDIT.equals(action)) {

                eventData.put(ContactsEvents.Position_eventID, StringUtils.getHash(String.valueOf(cal.getTimeInMillis())));
                viewActivityTitle.setText(R.string.local_event_dialog_title_new_event);
                if (extras != null && extras.containsKey(Constants.EXTRA_EVENT_DATA)) {
                    TreeMap<Integer, String> eventDataTemplate;
                    eventDataTemplate = eventsData.getLocalEvent(extras.getString(Constants.EXTRA_EVENT_DATA));
                    if (eventDataTemplate != null) {
                        String oldCaption;
                        if (eventsData.preferences_name_format == ContactsEvents.FormatName.NameFirst) {
                            oldCaption = eventDataTemplate.get(ContactsEvents.Position_personFullName);
                        } else {
                            oldCaption = eventDataTemplate.get(ContactsEvents.Position_personFullNameAlt);
                        }
                        if (oldCaption != null) editName.setText(oldCaption);
                        editTitle.setText(eventDataTemplate.get(ContactsEvents.Position_title));
                        editOrganization.setText(eventDataTemplate.get(ContactsEvents.Position_organization));
                        eventData.put(ContactsEvents.Position_photo, eventDataTemplate.get(ContactsEvents.Position_photo));
                        eventDataTemplate.clear();
                    }
                }

            } else if (Intent.ACTION_EDIT.equals(action) || isReadOnly) {
                if (!isReadOnly) viewActivityTitle.setText(R.string.local_event_dialog_title_edit_event);
                if (extras != null && extras.containsKey(Constants.EXTRA_EVENT_DATA)) {
                    TreeMap<Integer, String> eventDataStored = eventsData.getLocalEvent(extras.getString(Constants.EXTRA_EVENT_DATA));
                    if (eventDataStored == null) {
                        Toast.makeText(LocalEventActivity.this, getString(R.string.msg_event_not_found), Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    eventData.putAll(eventDataStored);
                    eventDataStored.clear();
                    String storedEventSubType = eventData.get(ContactsEvents.Position_eventSubType);
                    String storedEventType = eventData.get(ContactsEvents.Position_eventType);
                    boolean isNotContactEvent = false;
                    if (storedEventType != null) {
                        isNotContactEvent = storedEventType.equals(String.valueOf(Constants.Type_Other))
                                || storedEventType.equals(String.valueOf(Constants.Type_HolidayEvent));
                    }

                    String oldCaption;
                    if (isNotContactEvent || eventsData.preferences_name_format == ContactsEvents.FormatName.NameFirst) {
                        oldCaption = eventData.get(ContactsEvents.Position_personFullName);
                    } else {
                        oldCaption = eventData.get(ContactsEvents.Position_personFullNameAlt);
                    }
                    if (oldCaption != null) editName.setText(oldCaption);

                    editTitle.setText(eventData.get(ContactsEvents.Position_title));
                    editOrganization.setText(eventData.get(ContactsEvents.Position_organization));
                    editDescription.setText(eventData.get(ContactsEvents.Position_eventDescription));
                    editURL.setText(eventData.get(ContactsEvents.Position_eventURL));

                    String eventDateString = eventData.get(ContactsEvents.Position_eventDateFirstTime);
                    if (eventDateString != null) {
                        Date dateEventFirstTime;

                        try {
                            dateEventFirstTime = ContactsEvents.sdf_DDMMYYYY_G.parse(eventDateString);
                            if (dateEventFirstTime != null) {
                                day = dateEventFirstTime.getDate();
                                month = dateEventFirstTime.getMonth();
                                year = dateEventFirstTime.getYear() + 1900;
                                isBC = true;
                            }
                        } catch (ParseException peg) {
                            try {
                                dateEventFirstTime = ContactsEvents.sdf_DDMMYYYY.parse(eventDateString);
                                if (dateEventFirstTime != null) {
                                    day = dateEventFirstTime.getDate();
                                    month = dateEventFirstTime.getMonth();
                                    year = dateEventFirstTime.getYear() + 1900;
                                }
                            } catch (ParseException pe) {
                                try {
                                    dateEventFirstTime = ContactsEvents.sdf_DDMMYYYY.parse(eventDateString
                                            .concat(Constants.STRING_PERIOD).concat(String.valueOf(cal.get(Calendar.YEAR))));
                                    if (dateEventFirstTime != null) {
                                        day = dateEventFirstTime.getDate();
                                        month = dateEventFirstTime.getMonth();
                                        useYear = false;
                                    }
                                } catch (ParseException ignored) { /**/ }
                            }
                        }
                    }
                    if (storedEventSubType != null) {
                        try {
                            Integer eventSubTypeId = Integer.valueOf(storedEventSubType);
                            if (eventSubTypesIds.contains(eventSubTypeId)) {
                                int indexEventSubType = eventSubTypesIds.indexOf(eventSubTypeId);
                                spinnerEventTypes.setSelection(indexEventSubType);
                                viewEventType.setText(eventTypesValues.get(indexEventSubType));
                            }
                        } catch (NumberFormatException ignored) { /**/ }
                    }
                }
            }

            //Ширина диалога
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            layoutParams.width = (int) (displayMetrics.widthPixels * (isReadOnly ? 0.8 : 0.9));
            getWindow().setAttributes(layoutParams);

            TextView buttonCloseX = findViewById(R.id.buttonClose);
            if (buttonCloseX != null) {
                buttonCloseX.setText(Constants.BUTTON_X);
                buttonCloseX.setOnClickListener(this::buttonCancelOnClick);
            }

            if (isReadOnly) {

                viewName.setVisibility(View.GONE);
                setReadOnly(editName);
                editName.setBackgroundColor(Color.TRANSPARENT);

                if (editTitle.getText().toString().isEmpty()) {
                    cardTitle.setVisibility(View.GONE);
                } else {
                    viewTitle.setVisibility(View.GONE);
                    setReadOnly(editTitle);
                }

                if (editOrganization.getText().toString().isEmpty()) {
                    cardOrganization.setVisibility(View.GONE);
                } else {
                    viewOrganization.setVisibility(View.GONE);
                    setReadOnly(editOrganization);
                }

                if (editDescription.getText().toString().isEmpty()) {
                    cardDescription.setVisibility(View.GONE);
                } else {
                    viewDescription.setVisibility(View.GONE);
                    setReadOnly(editDescription);
                }

                if (editURL.getText().toString().isEmpty()) {
                    cardURL.setVisibility(View.GONE);
                } else {
                    viewURL.setVisibility(View.GONE);
                    setReadOnly(editURL);
                    editURL.setEnabled(true);
                    editURL.setOnClickListener(v -> {
                        try {
                            String url = ((EditText) v).getText().toString().trim();
                            if (!url.startsWith("http")) {
                                url = "https://".concat(url);
                            }
                            if (eventsData.preferences_debug_on) {
                                Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
                            }
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        } catch (ActivityNotFoundException e) { /**/ }
                    });
                }

                setReadOnly(editDate);
                spinnerEventTypes.setVisibility(View.GONE);
                viewEventType.setVisibility(View.VISIBLE);

                TextView buttonEdit = findViewById(R.id.buttonSecondAction);
                buttonEdit.setText(R.string.button_edit);
                UiTools.addClickEffect(buttonEdit);
                buttonEdit.getBackground().setAlpha(50);
                buttonEdit.setVisibility(View.VISIBLE);
                buttonEdit.setOnClickListener(this::buttonSwitchToEdit);

                TextView buttonClose = findViewById(R.id.buttonThirdAction);
                buttonClose.setText(R.string.button_ok);
                buttonClose.setPadding(
                        ImageUtils.Dip2Px(getResources(), 15),
                        buttonClose.getPaddingTop(),
                        ImageUtils.Dip2Px(getResources(), 15),
                        buttonClose.getPaddingBottom()
                );
                UiTools.addClickEffect(buttonClose);
                buttonClose.getBackground().setAlpha(50);
                buttonClose.setVisibility(View.VISIBLE);
                buttonClose.setOnClickListener(view -> finish());

                setFinishOnTouchOutside(true);

            } else {

                editName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /**/ }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) { /**/ }

                    @Override
                    public void afterTextChanged(Editable s) {
                        updateEventPhoto(LocalEventActivity.this);
                    }
                });

                editDate.setOnClickListener(v -> {

                    DatePicker dialogFragment = DatePicker.newInstance();
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.EXTRA_DAY, eventDay);
                    bundle.putInt(Constants.EXTRA_MONTH, eventMonth);
                    bundle.putInt(Constants.EXTRA_YEAR, eventYear);
                    bundle.putBoolean(Constants.EXTRA_USE_YEAR, eventUseYear);
                    bundle.putBoolean(Constants.EXTRA_IS_BC, eventIsBC);
                    dialogFragment.setArguments(bundle);

                    LocalEventActivity.this.getFragmentManager()
                            .beginTransaction()
                            .add(dialogFragment, null)
                            .commit();

                });

                spinnerEventTypes.setVisibility(View.VISIBLE);
                spinnerEventTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        updateCaptionsAndVisibility(LocalEventActivity.this);
                        updateEventPhoto(LocalEventActivity.this);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                viewEventType.setVisibility(View.GONE);

                editDate.setPadding(ImageUtils.Dip2Px(getResources(), 10), 0, 0, 0);

                if (Intent.ACTION_EDIT.equals(action)) {
                    TextView buttonRemove = findViewById(R.id.buttonFirstAction);
                    buttonRemove.setText(R.string.button_remove);
                    UiTools.addClickEffect(buttonRemove);
                    buttonRemove.getBackground().setAlpha(50);
                    buttonRemove.setVisibility(View.VISIBLE);
                    buttonRemove.setOnClickListener(this::buttonRemoveOnClick);
                }

                TextView buttonCancel = findViewById(R.id.buttonSecondAction);
                buttonCancel.setText(R.string.button_cancel);
                UiTools.addClickEffect(buttonCancel);
                buttonCancel.getBackground().setAlpha(50);
                buttonCancel.setVisibility(View.VISIBLE);
                buttonCancel.setOnClickListener(this::buttonCancelOnClick);

                TextView buttonSave = findViewById(R.id.buttonThirdAction);
                buttonSave.setText(R.string.button_save);
                UiTools.addClickEffect(buttonSave);
                buttonSave.getBackground().setAlpha(50);
                buttonSave.setVisibility(View.VISIBLE);
                buttonSave.setOnClickListener(this::buttonSaveOnClick);

                buttonPickContactData.setOnClickListener(v -> pickContactData());
                buttonPickContactData.setOnLongClickListener(v -> {
                    Toast.makeText(LocalEventActivity.this, getString(R.string.event_photo_pick_contact_hint), Toast.LENGTH_LONG).show();
                    return true;
                });

                buttonPickPhoto.setOnClickListener(v -> LocalEventActivity.this.pickPhoto());
                buttonPickPhoto.setOnLongClickListener(v -> {
                    Toast.makeText(LocalEventActivity.this, getString(R.string.event_photo_select_photo_hint), Toast.LENGTH_LONG).show();
                    return true;
                });

                buttonClearPhoto.setOnClickListener(v -> LocalEventActivity.this.clearPhoto());
                buttonClearPhoto.setOnLongClickListener(v -> {
                    Toast.makeText(LocalEventActivity.this, getString(R.string.event_photo_clear_photo_hint), Toast.LENGTH_LONG).show();
                    return true;
                });

                setFinishOnTouchOutside(false);
                editName.requestFocus();
                if (getWindow() != null) getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }

            updateCaptionsAndVisibility(this);
            updateEventDate(editDate, day, month, year, useYear, isBC);
            updateEventPhoto(this);
            this.eventDataSaved = eventsData.getEventData(eventData);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void clearPhoto() {
        try {

            eventData.put(ContactsEvents.Position_photo, Constants.STRING_EMPTY);
            eventData.put(ContactsEvents.Position_photo_uri, Constants.STRING_EMPTY);
            updateCaptionsAndVisibility(this);
            updateEventPhoto(this);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void pickPhoto() {
        try {

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(Constants.MIME_IMAGE_ALL);

            if (intent.resolveActivity(getPackageManager()) != null) {
                photoPickerLauncher.launch(intent);
            } else {
                Toast.makeText(this, getString(R.string.msg_no_image_picker), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        CropIntent intent = new CropIntent();
                        intent.setImagePath(selectedImageUri);
                        startActivityForResult(intent.getIntent(this), Constants.RESULT_CROP_PHOTO);
                    }
                }
            }
    );

    private void pickContactData() {

        if (DeviceTools.checkNoContactsAccess(eventsData.getContext())) {
            //https://issuetracker.google.com/issues/118400813 - без доступа к контактам не работает
            requestContactsPermission();
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivityForResult(intent, Constants.RESULT_PICK_CONTACT);
            } catch (ActivityNotFoundException ignored) { /**/ }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            boolean granted = false;

            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                pickPhoto();
            } else {
                // Разрешение отклонено
                Toast.makeText(this, getString(R.string.msg_no_access_storage), Toast.LENGTH_LONG).show();

                //Доступа до фото нет. Выбираем просто файл
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(Constants.MIME_IMAGE_ALL);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivityForResult(intent, Constants.RESULT_PICK_FILE);
                } catch (android.content.ActivityNotFoundException e) { /**/ }
            }
        } else if (requestCode == Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            pickContactData();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        try {

            if (resultCode == RESULT_OK) {
                if (requestCode == Constants.RESULT_PICK_CONTACT) {
                    Uri contactUri = data.getData();
                    if (contactUri != null) {
                        String contactID = contactUri.toString().substring(contactUri.toString().lastIndexOf(Constants.STRING_SLASH) + 1);
                        if (!contactID.isEmpty()) {
                            try {
                                HashMap<String, String> contactDataMap = eventsData.getContactDataMulti(StringUtils.parseToLong(contactID), new String[]{
                                        ContactsContract.Contacts.PHOTO_URI,
                                        ContactsContract.Data.DISPLAY_NAME,
                                        ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE
                                });

                                Uri selectedImageUri = Uri.parse(contactDataMap.get(ContactsContract.Contacts.PHOTO_URI));
                                if (selectedImageUri != null) {
                                    CropIntent intent = new CropIntent();
                                    intent.setImagePath(selectedImageUri);
                                    startActivityForResult(intent.getIntent(this), Constants.RESULT_CROP_PHOTO);
                                }
                            } catch (SecurityException e) {
                                ToastExpander.showInfoMsg(this, getString(R.string.msg_no_access_contacts));
                            }
                        }
                    }
                } else if (requestCode == Constants.RESULT_PICK_FILE) {
                    Uri selectedImageUri = data.getData();
                    CropIntent intent = new CropIntent();
                    intent.setImagePath(selectedImageUri);
                    startActivityForResult(intent.getIntent(this), Constants.RESULT_CROP_PHOTO);

                } else if (requestCode == Constants.RESULT_CROP_PHOTO) {
                    if (data.getExtras() != null) {
                        Uri croppedUri = data.getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
                        if (croppedUri != null) {
                            int maxSize = 500;
                            try {
                                int step = eventsData.getResources().getInteger(R.integer.pref_LocalEvents_PhotoSize_step);
                                maxSize = step + step * eventsData.preferences_local_events_photo_size;
                            } catch (NumberFormatException ignored) { /**/ }

                            eventData.put(ContactsEvents.Position_photo, ImageUtils.encodeImageToBase64(this, croppedUri, maxSize));
                            eventData.put(ContactsEvents.Position_photo_uri, Constants.STRING_EMPTY);
                            updateCaptionsAndVisibility(this);
                            updateEventPhoto(this);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static void updateEventDate(@NonNull TextView editDate, int day, int month, int year, boolean useYear, boolean isBC) {
        try {

            eventDay = day;
            eventMonth = month;
            eventYear = year;
            eventUseYear = useYear;
            eventIsBC = isBC;
            String dateFormated;

            Date date = new Date(eventYear - 1900, eventMonth, eventDay);
            if (eventUseYear) {
                int dow = DayOfWeekCalculator.getDayOfWeek(eventIsBC, eventYear, eventMonth + 1, eventDay);
                String name = DayOfWeekCalculator.getShortDayName(dow, Locale.getDefault());

                dateFormated = eventsData.getDateFormatted(
                        ContactsEvents.sdf_DDMMYYYY.format(date), ContactsEvents.FormatDate.WithYear)
                        + (eventIsBC ? eventsData.getResources().getString(R.string.msg_after_year_bc) : Constants.STRING_EMPTY)
                        + Constants.STRING_PARENTHESIS_OPEN
                        + name.toLowerCase()
                        + Constants.STRING_PARENTHESIS_CLOSE;
            } else {
                dateFormated = eventsData.getDateFormatted(
                        ContactsEvents.sdf_DDMM.format(date), ContactsEvents.FormatDate.WithoutYear);
            }

            editDate.setText("📆 ".concat(dateFormated));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ContextThemeWrapper context = new ContextThemeWrapper(editDate.getContext(), eventsData.preferences_theme.themeMain);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static void setReadOnly(@NonNull final View view) {
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.setClickable(false);
        view.setLongClickable(false);

        if (view instanceof TextView) {
            ((TextView) view).setCursorVisible(false);
        }
        view.setBackgroundResource(android.R.color.transparent);
        view.setEnabled(false);
    }

    public void buttonCancelOnClick(final View view) {

        if (isReadOnly) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog);
        try {

            prepareEventData(this);
            if (eventsData.getEventData(eventData).equals(this.eventDataSaved)) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            Builder builder = new Builder(themedContext);
            builder
                    .setTitle(getString(R.string.msg_title_confirmation))
                    .setIcon(android.R.drawable.ic_menu_help)
                    .setMessage(getString(R.string.local_event_dialog_confirmation_cancel))
                    .setPositiveButton(R.string.button_yes, (dialog, which) -> {
                        dialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    })
                    .setNegativeButton(R.string.button_no, (dialog, which) -> dialog.dismiss());
            androidx.appcompat.app.AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(dialog -> {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(themedContext, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void buttonRemoveOnClick(final View view) {
        ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog);
        try {

            Builder builder = new Builder(context);
            builder
                    .setTitle(getString(R.string.msg_title_confirmation))
                    .setIcon(android.R.drawable.ic_menu_help)
                    .setMessage(getString(R.string.local_event_dialog_confirmation_remove))
                    .setPositiveButton(R.string.button_yes, (dialog, which) -> {
                        //Хранимый тип события и используемый при отрисовке отличаются
                        //Subtype используется в getEventKey(), который используется в removeLocalEvent()
                        String eventType = eventData.get(ContactsEvents.Position_eventType);
                        if (eventType != null) {
                            try {
                                eventData.put(ContactsEvents.Position_eventType, ContactsEvents.getEventType(Integer.parseInt(eventType)));
                            } catch (NumberFormatException ignored) { /**/ }
                        }
                        String eventSubtype = eventData.get(ContactsEvents.Position_eventSubType);
                        if (eventSubtype != null) {
                            try {
                                eventData.put(ContactsEvents.Position_eventSubType, ContactsEvents.getEventType(Integer.parseInt(eventSubtype)));
                            } catch (NumberFormatException ignored) { /**/ }
                        }
                        eventsData.removeLocalEvent(eventData);
                        setResult(RESULT_OK);
                        finish();
                    })
                    .setNegativeButton(R.string.button_no, (dialog, which) -> dialog.dismiss());
            androidx.appcompat.app.AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(dialog -> {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void buttonSaveOnClick(final View view) {
        ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
        try {

            EditText editEventTitle = findViewById(R.id.editName);
            String eventEventTitle = editEventTitle.getText().toString();

            if (eventEventTitle.isEmpty()) {
                TextView captionEventTitle = findViewById(R.id.captionName);
                Toast.makeText(context, getString(R.string.msg_empty_required_field,
                        StringUtils.substringBefore(captionEventTitle.getText().toString(), Constants.STRING_COLON)), Toast.LENGTH_LONG).show();
                return;
            }

            prepareEventData(this);

            List<String> similarEventIds;
            if (!eventsData.getEventData(eventData).equals(this.eventDataSaved)) {
                similarEventIds = eventsData.getSimilarLocalEventIds(this.eventDataSaved, EnumSet.of(
                        ContactsEvents.getSimilarFields.PERSON_FULL_NAME,
                        ContactsEvents.getSimilarFields.ORGANIZATION
                ));
            } else {
                similarEventIds = null;
            }
            if (similarEventIds == null) {
                saveEvent();
                return;
            }

            ContextThemeWrapper themedContext = new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog);
            Builder builder = new Builder(themedContext);
            builder
                    .setTitle(getString(R.string.msg_title_confirmation))
                    .setIcon(android.R.drawable.ic_menu_help)
                    .setMessage(getString(R.string.local_event_dialog_confirmation_similar))
                    .setPositiveButton(R.string.button_yes, (dialog, which) -> {
                        dialog.dismiss();
                        updateSimilarEvents(similarEventIds);
                        saveEvent();
                    })
                    .setNegativeButton(R.string.button_no, (dialog, which) -> {
                        dialog.dismiss();
                        saveEvent();
                    })
                    .setNeutralButton(R.string.button_cancel, (dialog, which) -> dialog.dismiss());
            androidx.appcompat.app.AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(dialog -> {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Обновляет события данными
     *
     * @param similarEventIds Список Id событий для обновления
     */
    private void updateSimilarEvents(List<String> similarEventIds) {
        try {

            if (similarEventIds == null || similarEventIds.isEmpty() || LocalEventActivity.eventData == null || LocalEventActivity.eventData.isEmpty()) return;

            SharedPreferences preferences = getSharedPreferences(Constants.LocalEventsFilename, Context.MODE_PRIVATE);

            for (String eventId: similarEventIds) {
                String eventString = preferences.getString(eventId, null);
                if (eventString != null) {
                    try {

                        TreeMap<Integer, String> eventDataToUpdate = eventsData.getEventData(eventString);

                        eventDataToUpdate.put(ContactsEvents.Position_personFullName, LocalEventActivity.eventData.get(ContactsEvents.Position_personFullName));
                        eventDataToUpdate.put(ContactsEvents.Position_personFullNameAlt, LocalEventActivity.eventData.get(ContactsEvents.Position_personFullNameAlt));
                        eventDataToUpdate.put(ContactsEvents.Position_title, LocalEventActivity.eventData.get(ContactsEvents.Position_title));
                        eventDataToUpdate.put(ContactsEvents.Position_organization, LocalEventActivity.eventData.get(ContactsEvents.Position_organization));
                        eventDataToUpdate.put(ContactsEvents.Position_photo, LocalEventActivity.eventData.get(ContactsEvents.Position_photo));

                        eventsData.saveLocalEvent(eventDataToUpdate);
                        eventDataToUpdate.clear();

                    } catch (Exception ignored) { /**/ }
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void saveEvent() {
        eventsData.saveLocalEvent(eventData);
        eventsData.needUpdateEventList = true;

        setResult(RESULT_OK);
        finish();
    }

    private void buttonSwitchToEdit(final View view) {
        try {
            Intent intent = getIntent();
            intent.setAction(Intent.ACTION_EDIT);
            recreate();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static void prepareEventData(LocalEventActivity activity) {
        try {

            String eventTitle = activity.editName.getText().toString();
            int indexType = eventTypesValues.indexOf((String) activity.spinnerEventTypes.getSelectedItem());
            Integer subType = eventSubTypesIds.get(indexType);
            boolean isNotContactEvent = subType.equals(Constants.Type_HolidayEvent) || subType.equals(Constants.Type_Other);

            if (isNotContactEvent) {
                eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                eventData.put(ContactsEvents.Position_personFullNameAlt, Constants.STRING_EMPTY);
            } else if (eventsData.preferences_name_format == ContactsEvents.FormatName.LastnameFirst) {
                eventData.put(ContactsEvents.Position_personFullNameAlt, eventTitle);
                String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.LastnameFirst, activity);
                eventData.put(ContactsEvents.Position_personFullName, personFullNameAlt);
            } else {
                eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.NameFirst, activity);
                eventData.put(ContactsEvents.Position_personFullNameAlt, personFullNameAlt);
            }

            String eventDateString;
            if (eventUseYear) {
                eventDateString = ContactsEvents.sdf_DDMMYYYY.format(new Date(eventYear - 1900, eventMonth, eventDay));
            } else {
                eventDateString = ContactsEvents.sdf_DDMM.format(new Date(eventYear - 1900, eventMonth, eventDay));
            }
            eventData.put(ContactsEvents.Position_eventDateFirstTime,
                    eventDateString.concat(eventIsBC ? Constants.STRING_SPACE.concat(Constants.STRING_BC) : Constants.STRING_EMPTY));

            eventData.put(ContactsEvents.Position_eventType, String.valueOf(eventTypesIds.get(indexType)));
            eventData.put(ContactsEvents.Position_eventSubType, String.valueOf(subType));

            if (isNotContactEvent) {
                eventData.put(ContactsEvents.Position_title, Constants.STRING_EMPTY);
                eventData.put(ContactsEvents.Position_organization, Constants.STRING_EMPTY);
            } else {
                eventData.put(ContactsEvents.Position_title, activity.editTitle.getText().toString());
                eventData.put(ContactsEvents.Position_organization, activity.editOrganization.getText().toString());
            }
            eventData.put(ContactsEvents.Position_eventDescription, activity.editDescription.getText().toString());
            eventData.put(ContactsEvents.Position_eventURL, activity.editURL.getText().toString());
            eventData.put(ContactsEvents.Position_age, Constants.STRING_EMPTY);

            eventsData.fillEmptyEventData(eventData);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(eventsData.getContext(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static void updateEventPhoto(LocalEventActivity activity) {
        try {

            if (activity.imagePhoto != null) {
                prepareEventData(activity);
                TreeMap<Integer, String> eventDataForPhoto = new TreeMap<>(eventData);

                if (eventUseYear) {
                    final Date eventDate = new Date(eventYear - 1900, eventMonth, eventDay);
                    final Date today = AppDateUtils.getWithoutTime(Calendar.getInstance()).getTime();
                    int age = -1;
                    if (eventDate.before(today)) {
                        age = AppDateUtils.countYearsDiff(eventDate, today);
                    }
                    eventDataForPhoto.put(ContactsEvents.Position_age, String.valueOf(age));
                }

                String eventType = eventDataForPhoto.get(ContactsEvents.Position_eventType);
                if (eventType != null) {
                    eventDataForPhoto.put(ContactsEvents.Position_eventType, ContactsEvents.getEventType(Integer.parseInt(eventType)));
                }
                String eventSubType = eventDataForPhoto.get(ContactsEvents.Position_eventSubType);
                if (eventSubType != null) {
                    eventDataForPhoto.put(ContactsEvents.Position_eventSubType, ContactsEvents.getEventType(Integer.parseInt(eventSubType)));
                }

                activity.imagePhoto.setImageBitmap(eventsData.getEventPhoto(eventsData.getEventData(eventDataForPhoto), true, false, true, 1));
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(activity, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static void updateCaptionsAndVisibility(LocalEventActivity activity) {
        try {

            int indexType = eventTypesValues.indexOf((String) activity.spinnerEventTypes.getSelectedItem());

            Integer subType = eventSubTypesIds.get(indexType);
            if (subType.equals(Constants.Type_HolidayEvent) || subType.equals(Constants.Type_Other)) {
                activity.viewName.setText(R.string.local_event_dialog_caption_name_holiday);
                activity.cardTitle.setVisibility(View.GONE);
                activity.cardOrganization.setVisibility(View.GONE);
            } else {
                if (eventsData.preferences_name_format == ContactsEvents.FormatName.LastnameFirst) {
                    activity.viewName.setText(R.string.local_event_dialog_caption_name_alt);
                } else {
                    activity.viewName.setText(R.string.local_event_dialog_caption_name);
                }
                if (!activity.isReadOnly) {
                    activity.cardTitle.setVisibility(View.VISIBLE);
                    activity.cardOrganization.setVisibility(View.VISIBLE);
                }
            }

            if (!activity.isReadOnly) {
                activity.buttonPickContactData.setVisibility(View.VISIBLE);
                activity.buttonPickPhoto.setVisibility(View.VISIBLE);

                String imageData = eventData.get(ContactsEvents.Position_photo);
                String imageUrl = eventData.get(ContactsEvents.Position_photo_uri);
                if ((imageData != null && !imageData.isEmpty()) || (imageUrl != null && !imageUrl.isEmpty())) {
                    activity.buttonClearPhoto.setVisibility(View.VISIBLE);
                } else {
                    activity.buttonClearPhoto.setVisibility(View.GONE);
                }

                activity.findViewById(R.id.iconUrl).setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(activity, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void requestContactsPermission() {
        try {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS
                );
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS
                );
            } else {
                ToastExpander.showMsg(this, getString(R.string.msg_no_access_contacts_hint));
                try {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(Constants.URI_PACKAGE + this.getPackageName())));
                } catch (ActivityNotFoundException e) { /**/ }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    @Override
    protected void onDestroy() {
        eventData.clear();
        eventTypesValues.clear();
        eventTypesIds.clear();
        eventSubTypesIds.clear();

        super.onDestroy();
    }

}