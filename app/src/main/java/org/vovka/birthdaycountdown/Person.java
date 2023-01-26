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

import java.util.regex.Pattern;

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
            }

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
                    return fullName.substring(spaceFirst + 1) + Constants.STRING_SPACE + fullName.substring(0, spaceFirst);
                }
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
                return LastName + (!FirstName.equals(Constants.STRING_EMPTY) ? Constants.STRING_SPACE + FirstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY) + (!SecondName.equals(Constants.STRING_EMPTY) ? Constants.STRING_SPACE + SecondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY);
            } else if (!FirstName.equals(Constants.STRING_EMPTY)) {
                return FirstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD + (!SecondName.equals(Constants.STRING_EMPTY) ? Constants.STRING_SPACE + SecondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : Constants.STRING_EMPTY);
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
            if (contactsEvents.preferences_last_name_comletions_man == null) {//Ищем первый раз
                //eventsData.preferences_first_names_female = new HashSet<>(Arrays.asList(context.getString(R.string.first_names_female).split(ContactsEvents.STRING_COMMA)));

                final String regex_inter = "\\Z|";
                final String regex_last = "\\Z";

                contactsEvents.preferences_last_name_comletions_man = Pattern.compile(context.getString(R.string.last_name_completions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(Constants.STRING_EMPTY);
                contactsEvents.preferences_last_name_comletions_female = Pattern.compile(context.getString(R.string.last_name_completions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(Constants.STRING_EMPTY);
                contactsEvents.preferences_first_names_man = Pattern.compile(context.getString(R.string.first_names_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(Constants.STRING_EMPTY);
                final String names = contactsEvents.preferences_first_names_female_custom.isEmpty() ?
                        context.getString(R.string.first_names_female) :
                        context.getString(R.string.first_names_female).concat(Constants.STRING_COMMA).concat(contactsEvents.preferences_first_names_female_custom.toLowerCase().replace(Constants.STRING_COMMA_SPACE, Constants.STRING_COMMA)) ;
                contactsEvents.preferences_first_names_female = Pattern.compile(names.replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(Constants.STRING_EMPTY);
                contactsEvents.preferences_second_name_comletions_man = Pattern.compile(context.getString(R.string.second_name_completions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(Constants.STRING_EMPTY);
                contactsEvents.preferences_second_name_comletions_female = Pattern.compile(context.getString(R.string.second_name_completions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(Constants.STRING_EMPTY);

            }

            int ind = 0;
            if (!this.LastName.isEmpty()) {
                final String normalizedLastName = ContactsEvents.normalizeName(this.LastName);
                if (contactsEvents.preferences_last_name_comletions_man.reset(normalizedLastName).find()) {ind++;}
                else if (contactsEvents.preferences_last_name_comletions_female.reset(normalizedLastName).find()) {ind--;}
            }

            if (!this.SecondName.isEmpty()) {
                final String normalizedSecondName = ContactsEvents.normalizeName(this.SecondName);
                if (contactsEvents.preferences_second_name_comletions_man.reset(normalizedSecondName).find()) {ind++;}
                else if (contactsEvents.preferences_second_name_comletions_female.reset(normalizedSecondName).find()) {ind--;}
            }

            if (!this.FirstName.isEmpty()) {
                final String normalizedFirstName = ContactsEvents.normalizeName(this.FirstName);
                if (contactsEvents.preferences_first_names_man.reset(normalizedFirstName).find()) {ind++;}
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