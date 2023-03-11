/*
 * *
 *  * Created by Vladimir Belov on 18.09.2022, 8:26
 *  * Copyright (c) 2018 - 2022. All rights reserved.
 *  * Last modified 25.06.2022, 1:08
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

class Person {

    private static final String TAG = "Person";
    private String LastName;

    public String getFirstName() {
        return FirstName;
    }

    public String getSecondName() {
        return SecondName;
    }

    private String FirstName;
    private String SecondName;
    int Age = -1;
    String Age_str;
    String FIO_str;
    private Context context;

    Person(@NonNull Context context, @NonNull String[] eventArray) {

        try {

            this.context = context;

            FIO_str = eventArray[ContactsEvents.Position_personFullNameAlt];
            int spaceFirst = FIO_str.indexOf(Constants.STRING_SPACE);
            if (spaceFirst == -1) { //Имя из одного слова
                final ContactsEvents contactsEvents = ContactsEvents.getInstance();
                final String normalizedName = ContactsEvents.normalizeName(FIO_str);
                if (contactsEvents.preferences_first_names_male.reset(normalizedName).find()
                        ||contactsEvents.preferences_first_names_female.reset(normalizedName).find()) { //Это имя
                    FirstName = FIO_str;
                    LastName = Constants.STRING_EMPTY;
                } else { //Это фамилия
                    FirstName = Constants.STRING_EMPTY;
                    LastName = FIO_str;
                }
                SecondName = Constants.STRING_EMPTY;
            } else {
                int spaceLast = FIO_str.lastIndexOf(Constants.STRING_SPACE);
                if (spaceFirst != spaceLast && spaceLast != -1) { //Есть отчество
                    LastName = FIO_str.substring(0, spaceFirst);
                    FirstName = FIO_str.substring(spaceFirst + 1, spaceLast);
                    SecondName = FIO_str.substring(spaceLast + 1);
                } else {
                    LastName = FIO_str.substring(0, spaceFirst);
                    FirstName = FIO_str.substring(spaceFirst + 1);
                    SecondName = Constants.STRING_EMPTY;
                }
            }
/*            //--
            FIO_str = eventArray[ContactsEvents.Position_personFullName];
            int spaceFirst = FIO_str.indexOf(Constants.STRING_SPACE);
            if (spaceFirst == -1) { //Имя из одного слова
                FirstName = FIO_str;
                LastName = Constants.STRING_EMPTY;
                SecondName = Constants.STRING_EMPTY;
            } else {
                int spaceLast = FIO_str.lastIndexOf(Constants.STRING_SPACE);
                if (spaceFirst != spaceLast) { //Есть отчество
                    FirstName = FIO_str.substring(0, spaceFirst);
                    SecondName = FIO_str.substring(spaceFirst + 1, spaceLast);
                    LastName = FIO_str.substring(spaceLast + 1);
                } else {
                    FirstName = FIO_str.substring(0, spaceFirst);
                    LastName = FIO_str.substring(spaceFirst + 1);
                    SecondName = Constants.STRING_EMPTY;
                }
            }*/

            try {
                Age = Integer.parseInt(eventArray[ContactsEvents.Position_age]);
            } catch (NumberFormatException e) { /**/ }
            Age_str = eventArray[ContactsEvents.Position_age_caption];

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    public static String getAltName(@NonNull String fullName, int formatName, @NonNull Context context) {

        try{

            final int spaceFirst = fullName.indexOf(Constants.STRING_SPACE);
            if (spaceFirst == -1) { //Имя из одного слова
                return fullName;
            } else {
                final int spaceLast = fullName.lastIndexOf(Constants.STRING_SPACE);

                if (Integer.toString(formatName).equals(context.getString(R.string.pref_List_NameFormat_FirstSecondLast))) {
                    return fullName.substring(spaceLast + 1) + Constants.STRING_SPACE + fullName.substring(0, spaceLast);
                } else {
                    final String fullNameAlt = fullName.substring(spaceFirst + 1) + Constants.STRING_SPACE + fullName.substring(0, spaceFirst);
                    if (spaceFirst != spaceLast) { //Имя из 3+ слов
                        return fullNameAlt;
                    } else { //Имя из двух слов
                        final ContactsEvents contactsEvents = ContactsEvents.getInstance();
                        final String normalizedFirstName = ContactsEvents.normalizeName(fullName.substring(0, spaceFirst));
                        if (contactsEvents.preferences_first_names_male.reset(normalizedFirstName).find()
                                ||contactsEvents.preferences_first_names_female.reset(normalizedFirstName).find()) {
                            return fullName;
                        } else {
                            return fullNameAlt;
                        }
                    }
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    public static String getShortName(@NonNull String fullName, @NonNull Context context) {

        try{

            final int spaceFirst = fullName.indexOf(Constants.STRING_SPACE);
            if (spaceFirst == -1) { //Имя из одного слова
                return fullName;
            } else {
                final int spaceLast = fullName.lastIndexOf(Constants.STRING_SPACE);

                if (spaceFirst == spaceLast) return fullName; //Уже короткое

                return fullName.substring(0, spaceFirst) + fullName.substring(spaceLast);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    Person(@NonNull Context context, @NonNull String eventData) {
        this(context, eventData.split(Constants.STRING_EOT, -1));
    }

    String getFullNameShort () { //Фамилия И. О.
        //поддержка двойных фамилий и имён пока сделана в WidgetUpdater
        try {
            if (!LastName.equals(Constants.STRING_EMPTY)) {
                return LastName
                        + (!FirstName.equals(Constants.STRING_EMPTY) ? Constants.STRING_SPACE + FirstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY)
                        + (!SecondName.equals(Constants.STRING_EMPTY) ? Constants.STRING_SPACE + SecondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY);
            } else if (!FirstName.equals(Constants.STRING_EMPTY)) {
                if (!SecondName.isEmpty()) {
                    return FirstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD + Constants.STRING_SPACE + SecondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD;
                } else {
                    return FirstName;
                }
            } else {
                return Constants.STRING_EMPTY;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return Constants.STRING_EMPTY;
        }
    }

    int getGender() { //Определение пола по фамилии, имени, отчеству
        // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён
        //https://github.com/vadimiztveri/sex_by_russian_name/blob/master/src/sex_by_russian_name.js
        //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

        try {
            // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён

            final ContactsEvents contactsEvents = ContactsEvents.getInstance();

            int ind = 0;
            if (!this.LastName.isEmpty()) {
                final String normalizedLastName = ContactsEvents.normalizeName(this.LastName);
                if (contactsEvents.preferences_last_name_completions_male.reset(normalizedLastName).find()) {ind++;}
                else if (contactsEvents.preferences_last_name_completions_female.reset(normalizedLastName).find()) {ind--;}
            }

            if (!this.SecondName.isEmpty()) {
                final String normalizedSecondName = ContactsEvents.normalizeName(this.SecondName);
                if (contactsEvents.preferences_second_name_completions_male.reset(normalizedSecondName).find()) {ind++;}
                else if (contactsEvents.preferences_second_name_completions_female.reset(normalizedSecondName).find()) {ind--;}
            }

            if (!this.FirstName.isEmpty()) {
                final String normalizedFirstName = ContactsEvents.normalizeName(this.FirstName);
                if (contactsEvents.preferences_first_names_male.reset(normalizedFirstName).find()) {ind++;}
                else if (contactsEvents.preferences_first_names_female.reset(normalizedFirstName).find()) {ind--;}
            }

            return ind > 0 ? 1 : ind < 0 ? 2 : -1;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showText(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            return -1;
        }

    }

}