/*
 * *
 *  * Created by Vladimir Belov on 26.02.2025, 13:18
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 26.02.2025, 13:06
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LocaleManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalEventActivity extends Activity {

    private static final String TAG = "LocalEventActivity";
    private static final ContactsEvents eventsData;
    private TreeMap<Integer, String> eventData = new TreeMap<>();
    private static int eventYear;
    private static int eventMonth;
    private static int eventDay;
    private static boolean eventUseYear = true;
    private final List<String> eventTypesValues = new ArrayList<>();
    private final List<Integer> eventTypesIds = new ArrayList<>();
    boolean isReadOnly;

    static {
        eventsData = ContactsEvents.getInstance();
    }

    public LocalEventActivity() {
    }

    public static class DatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        final String TAG = "DatePicker";

        @Nullable
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            try {

                AtomicInteger yearToChange = new AtomicInteger();
                AtomicInteger monthToChange = new AtomicInteger();
                AtomicInteger dayToChange = new AtomicInteger();
                AtomicBoolean useYear = new AtomicBoolean(true);
                AtomicInteger yearBeforeHide = new AtomicInteger(0);
                final Calendar today = Calendar.getInstance();

                Bundle bundle = getArguments();
                if (bundle.containsKey(Constants.EXTRA_DAY)) {
                    yearToChange.set(bundle.getInt(Constants.EXTRA_YEAR));
                    monthToChange.set(bundle.getInt(Constants.EXTRA_MONTH));
                    dayToChange.set(bundle.getInt(Constants.EXTRA_DAY));
                    useYear.set(bundle.getBoolean(Constants.EXTRA_USE_YEAR));
                } else {
                    yearToChange.set(today.get(Calendar.YEAR));
                    monthToChange.set(today.get(Calendar.MONTH));
                    dayToChange.set(today.get(Calendar.DAY_OF_MONTH));
                    useYear.set(true);
                }

                getActivity().setTheme(eventsData.preferences_theme.themeDialog);
                View viewActivity = getActivity().getLayoutInflater().inflate(R.layout.datepicker, null);

                TextView editDate = getActivity().findViewById(R.id.editDate);
                CheckBox checkUseYear = viewActivity.findViewById(R.id.check_use_year);
                android.widget.DatePicker datePicker = viewActivity.findViewById(R.id.datePicker);
                @SuppressLint("DiscouragedApi")
                View spinnerYear = datePicker.findViewById(getResources().getIdentifier(Constants.RES_TYPE_YEAR, Constants.RES_TYPE_ID, Constants.RES_PACKAGE_ANDROID));

                datePicker.init(yearToChange.get(), monthToChange.get(), dayToChange.get(), (
                                view, year, monthOfYear, dayOfMonth) -> {
                            yearToChange.set(year);
                            monthToChange.set(monthOfYear);
                            dayToChange.set(dayOfMonth);
                            useYear.set(checkUseYear.isChecked());
                        }
                );
                if (useYear.get()) {
                    spinnerYear.setVisibility(View.VISIBLE);
                    checkUseYear.setChecked(true);
                } else {
                    spinnerYear.setVisibility(View.GONE);
                    checkUseYear.setChecked(false);
                    yearBeforeHide.set(today.get(Calendar.YEAR));
                }

                checkUseYear.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        datePicker.updateDate(yearBeforeHide.get(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        spinnerYear.setVisibility(View.VISIBLE);
                    } else {
                        yearBeforeHide.set(datePicker.getYear());
                        datePicker.updateDate(2000, datePicker.getMonth(), datePicker.getDayOfMonth());
                        spinnerYear.setVisibility(View.GONE);
                    }
                });

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setView(viewActivity)
                        .setTitle(R.string.local_event_date_picker_title)
                        .setPositiveButton(R.string.button_ok, (dialog, which) ->
                                updateEventDate(editDate, dayToChange.get(), monthToChange.get(), yearToChange.get(), useYear.get()))
                        .setNegativeButton(R.string.button_cancel, (dialog, which) -> dismiss());

                return builder.create();

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(getActivity(), ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
                return null;
            }
        }

        public static DatePicker newInstance() {
            return new DatePicker();
        }

        @Override
        public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {}
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);

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
                applicationConf.setLocales(new LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            setTheme(eventsData.preferences_theme.themeMain);
            this.getTheme().applyStyle(R.style.FloatingActivity, true);
            setContentView(R.layout.activity_event);

            TextView viewActivityTitle = findViewById(R.id.textCaption);
            TextView viewEventTitle = findViewById(R.id.captionTitle);
            EditText editCaption = findViewById(R.id.editTitle);
            TextView editDate = findViewById(R.id.editDate);
            ImageView imagePhoto = findViewById(R.id.imagePhoto);

            eventTypesValues.add(getString(R.string.event_type_birthday_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_birthday));
            eventTypesIds.add(Constants.Type_BirthDay);
            eventTypesValues.add(getString(R.string.event_type_wedding_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_anniversary));
            eventTypesIds.add(Constants.Type_Anniversary);
            eventTypesValues.add(getString(R.string.event_type_death_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_death));
            eventTypesIds.add(Constants.Type_Death);
            eventTypesValues.add(getString(R.string.event_type_crowning_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_crowning));
            eventTypesIds.add(Constants.Type_Crowning);
            eventTypesValues.add(getString(R.string.event_type_nameday_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_nameday));
            eventTypesIds.add(Constants.Type_NameDay);
            eventTypesValues.add(getString(R.string.event_type_other_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_other));
            eventTypesIds.add(Constants.Type_Other);
            eventTypesValues.add(getString(R.string.event_type_holiday_emoji) + Constants.STRING_SPACE + getString(R.string.event_type_holiday));
            eventTypesIds.add(Constants.Type_HolidayEvent);

            Spinner spinnerEventTypes = findViewById(R.id.spinnerEventType);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, eventTypesValues);
            spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerEventTypes.setAdapter(spinnerArrayAdapter);
            TextView viewEventType = findViewById(R.id.viewEventType);

            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            boolean useYear = true;

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            final String action = intent.getAction();
            isReadOnly = Intent.ACTION_VIEW.equals(action);
            if (Intent.ACTION_INSERT.equals(action)) {

                eventData.put(ContactsEvents.Position_eventID, ContactsEvents.getHash(String.valueOf(c.getTimeInMillis())));
                viewActivityTitle.setText(R.string.local_event_dialog_title_new_event);

            } else if (Intent.ACTION_EDIT.equals(action) || isReadOnly) {
                if (extras != null) {
                    if (extras.containsKey(Constants.EXTRA_EVENT_DATA)) {
                        eventData = eventsData.getLocalEvent(extras.getString(Constants.EXTRA_EVENT_DATA));
                        if (eventData != null) {
                            String oldCaption;
                            if (eventsData.preferences_name_format == ContactsEvents.FormatName.NameFirst) {
                                oldCaption = eventData.get(ContactsEvents.Position_personFullName);
                            } else {
                                oldCaption = eventData.get(ContactsEvents.Position_personFullNameAlt);
                            }
                            if (oldCaption != null) editCaption.setText(oldCaption);

                            String eventDateString = eventData.get(ContactsEvents.Position_eventDateFirstTime);
                            if (eventDateString != null) {
                                Date dateEventFirstTime;

                                try {
                                    dateEventFirstTime = eventsData.sdf_DDMMYYYY.parse(eventDateString);
                                    if (dateEventFirstTime != null) {
                                        day = dateEventFirstTime.getDate();
                                        month = dateEventFirstTime.getMonth();
                                        year = dateEventFirstTime.getYear() + 1900;
                                    }
                                } catch (ParseException pe) {
                                    try {
                                        dateEventFirstTime = eventsData.sdf_DDMMYYYY.parse(eventDateString
                                                .concat(Constants.STRING_PERIOD).concat(String.valueOf(c.get(Calendar.YEAR))));
                                        if (dateEventFirstTime != null) {
                                            day = dateEventFirstTime.getDate();
                                            month = dateEventFirstTime.getMonth();
                                            useYear = false;
                                        }
                                    } catch (ParseException ignored) { /**/ }
                                }
                            }

                            String oldEventType = eventData.get(ContactsEvents.Position_eventType);
                            if (oldEventType != null && eventTypesIds.contains(Integer.valueOf(oldEventType))) {
                                spinnerEventTypes.setSelection(eventTypesIds.indexOf(Integer.valueOf(oldEventType)));
                                viewEventType.setText(eventTypesValues.get(eventTypesIds.indexOf(Integer.valueOf(oldEventType))));
                            }
                        }
                    }
                }
                if (!isReadOnly) viewActivityTitle.setText(R.string.local_event_dialog_title_edit_event);
            }

            updateEventDate(editDate, day, month, year, useYear);

            if (imagePhoto != null) imagePhoto.setImageBitmap(ContactsEvents.getBitmap(this, R.drawable.ic_pack00_m1));

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(this::buttonCancelOnClick);
            }

            if (isReadOnly) {

                viewEventTitle.setVisibility(View.GONE);

                setReadOnly(editCaption, true);
                editCaption.setBackgroundColor(Color.TRANSPARENT);
                setReadOnly(editDate, true);
                //setReadOnly(spinnerEventTypes, true);
                spinnerEventTypes.setVisibility(View.GONE);
                viewEventType.setVisibility(View.VISIBLE);

                TextView buttonSave = findViewById(R.id.buttonThirdAction);
                buttonSave.setText(R.string.button_ok);
                addClickEffect(buttonSave);
                buttonSave.getBackground().setAlpha(50);
                buttonSave.setVisibility(View.VISIBLE);
                buttonSave.setOnClickListener(view -> finish());

                setFinishOnTouchOutside(true);

            } else {

                if (eventsData.preferences_name_format == ContactsEvents.FormatName.LastnameFirst) {
                    viewEventTitle.setText(R.string.local_event_dialog_caption_title_alt);
                }

                editDate.setOnClickListener(v -> {

                    DatePicker dialogFragment = DatePicker.newInstance();
                    Bundle bundle = new Bundle();
                    bundle.putInt(Constants.EXTRA_DAY, eventDay);
                    bundle.putInt(Constants.EXTRA_MONTH, eventMonth);
                    bundle.putInt(Constants.EXTRA_YEAR, eventYear);
                    bundle.putBoolean(Constants.EXTRA_USE_YEAR, eventUseYear);
                    dialogFragment.setArguments(bundle);

                    LocalEventActivity.this.getFragmentManager()
                            .beginTransaction()
                            .add(dialogFragment, null)
                            .commit();

                });

                spinnerEventTypes.setVisibility(View.VISIBLE);
                viewEventType.setVisibility(View.GONE);

                editDate.setPadding(ContactsEvents.Dip2Px(getResources(), 10), 0, 0, 0);

                if (Intent.ACTION_EDIT.equals(action)) {
                    TextView buttonRemove = findViewById(R.id.buttonFirstAction);
                    buttonRemove.setText(R.string.button_remove);
                    addClickEffect(buttonRemove);
                    buttonRemove.getBackground().setAlpha(50);
                    buttonRemove.setVisibility(View.VISIBLE);
                    buttonRemove.setOnClickListener(this::buttonRemoveOnClick);
                }

                TextView buttonCancel = findViewById(R.id.buttonSecondAction);
                buttonCancel.setText(R.string.button_cancel);
                addClickEffect(buttonCancel);
                buttonCancel.getBackground().setAlpha(50);
                buttonCancel.setVisibility(View.VISIBLE);
                buttonCancel.setOnClickListener(this::buttonCancelOnClick);

                TextView buttonSave = findViewById(R.id.buttonThirdAction);
                buttonSave.setText(R.string.button_save);
                addClickEffect(buttonSave);
                buttonSave.getBackground().setAlpha(50);
                buttonSave.setVisibility(View.VISIBLE);
                buttonSave.setOnClickListener(this::buttonSaveOnClick);

                setFinishOnTouchOutside(false);
                editCaption.requestFocus();
                if (getWindow() != null) getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private static void updateEventDate(@NonNull TextView editDate, int day, int month, int year, boolean useYear) {
        try {

            eventDay = day;
            eventMonth = month;
            eventYear = year;
            eventUseYear = useYear;
            String dateFormated;

            if (eventUseYear) {
                dateFormated = eventsData.getDateFormatted(
                        eventsData.sdf_DDMMYYYY.format(new Date(eventYear - 1900, eventMonth, eventDay)), ContactsEvents.FormatDate.WithYear);
            } else {
                dateFormated = eventsData.getDateFormatted(
                        eventsData.sdf_DDMM.format(new Date(eventYear - 1900, eventMonth, eventDay)), ContactsEvents.FormatDate.WithoutYear);
            }

            editDate.setText("📆 ".concat(dateFormated));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ContextThemeWrapper context = new ContextThemeWrapper(editDate.getContext(), eventsData.preferences_theme.themeMain);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void addClickEffect(@NonNull View view)
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

    public static void setReadOnly(@NonNull final View view, final boolean readOnly) {
        view.setFocusable(!readOnly);
        view.setFocusableInTouchMode(!readOnly);
        view.setClickable(!readOnly);
        view.setLongClickable(!readOnly);
        if (view instanceof TextView) {
            ((TextView) view).setCursorVisible(!readOnly);
        }
        view.setBackgroundResource(android.R.color.transparent);
        view.setEnabled(!readOnly);
    }

    void buttonCancelOnClick(final View view) {
        if (!isReadOnly) {
            //todo: если добавление или редактирование - предупреждение
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    void buttonRemoveOnClick(final View view) {
        ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeDialog);
        try {

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.msg_title_confirmation));
            builder.setIcon(android.R.drawable.ic_menu_help);
            builder.setMessage(getString(R.string.local_event_dialog_confirmation_remove));
            builder.setPositiveButton(R.string.button_yes, (dialog, which) -> {
                eventsData.removeLocalEvent(eventData);
                setResult(RESULT_OK);
                finish();
            });
            builder.setNegativeButton(R.string.button_no, (dialog, which) -> dialog.dismiss());
            androidx.appcompat.app.AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(arg0 -> {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                alertToShow.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void buttonSaveOnClick(final View view) {
        ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
        try {

            EditText editCaption = findViewById(R.id.editTitle);
            Spinner spinnerEventTypes = findViewById(R.id.spinnerEventType);

            String eventTitle = editCaption.getText().toString();
            if (eventTitle.isEmpty()) {
                TextView viewEventTitle = findViewById(R.id.captionTitle);
                ToastExpander.showMsg(context, getString(R.string.msg_empty_required_field, viewEventTitle.getText()));
                return;
            }

            if (eventsData.preferences_name_format == ContactsEvents.FormatName.LastnameFirst) {
                eventData.put(ContactsEvents.Position_personFullNameAlt, eventTitle);
                String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.LastnameFirst, context);
                eventData.put(ContactsEvents.Position_personFullName, personFullNameAlt);
            } else {
                eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.NameFirst, context);
                eventData.put(ContactsEvents.Position_personFullNameAlt, personFullNameAlt);
            }

            if (eventUseYear) {
                eventData.put(ContactsEvents.Position_eventDateFirstTime,
                        eventsData.sdf_DDMMYYYY.format(new Date(eventYear - 1900, eventMonth, eventDay)));
            } else {
                eventData.put(ContactsEvents.Position_eventDateFirstTime,
                        eventsData.sdf_DDMM.format(new Date(eventYear - 1900, eventMonth, eventDay)));
            }

            eventData.put(ContactsEvents.Position_eventType,
                    String.valueOf(eventTypesIds.get(eventTypesValues.indexOf((String) spinnerEventTypes.getSelectedItem()))));

            eventsData.fillEmptyEventData(eventData);
            eventsData.saveLocalEvent(eventData);

            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}