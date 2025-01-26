/*
 * *
 *  * Created by Vladimir Belov on 24.01.2025, 22:00
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 24.01.2025, 22:00
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LocaleManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LocalEventActivity extends Activity {

    private static final String TAG = "LocalEventActivity";
    ContactsEvents eventsData;

    public LocalEventActivity() {
    }

    public static class MyDatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            /*DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getActivity(),
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        TextView editDate = getActivity().findViewById(R.id.editDate);
                        editDate.setText(dayOfMonth + "." + (monthOfYear + 1) + "." + year1);
                    },
                    year, month, day);*/

            //DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), R.style.AlertDialog_BlueGrey, this, year, month, day );
            //return datePickerDialog;

            View v = getActivity().getLayoutInflater().inflate(R.layout.datepicker, null);
            return new AlertDialog.Builder(getActivity()).setView(v).create();
        }

        public static MyDatePicker newInstance() {
            return new MyDatePicker();
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

        }
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
                applicationConf.setLocales(new LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            this.setFinishOnTouchOutside(false);

            setContentView(R.layout.activity_event);

            ImageView imagePhoto = findViewById(R.id.imagePhoto);
            if (imagePhoto != null) imagePhoto.setImageBitmap(ContactsEvents.getBitmap(this, R.drawable.ic_pack00_m1));

            TextView editDate = findViewById(R.id.editDate);
            editDate.setOnClickListener(v -> {
                /*final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        LocalEventActivity.this,
                        R.style.MySpinnerDatePickerStyle,
                        (view, year1, monthOfYear, dayOfMonth) -> {
                            editDate.setText(dayOfMonth + "." + (monthOfYear + 1) + "." + year1);
                        },
                        year, month, day);
                datePickerDialog.show();*/

                MyDatePicker dialogFragment = MyDatePicker.newInstance();

                LocalEventActivity.this.getFragmentManager()
                        .beginTransaction()
                        .add(dialogFragment, "date-picker")
                        .commit();


            });


            List<String> eventTypesValues = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.pref_List_EventTypes_entries)));
            Spinner spinnerEventTypes = findViewById(R.id.spinnerEventType);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, eventTypesValues);
            spinnerEventTypes.setAdapter(spinnerArrayAdapter);

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> {
                    //todo: если добавление или редактирование - предупреждение

                    finish();
                });
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}
