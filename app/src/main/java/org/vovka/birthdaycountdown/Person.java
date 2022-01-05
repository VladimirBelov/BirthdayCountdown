/*
 * *
 *  * Created by Vladimir Belov on 26.12.2021, 1:01
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 26.12.2021, 0:28
 *
 */

package org.vovka.birthdaycountdown;

import static org.vovka.birthdaycountdown.Constants.STRING_COMMA_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

class Person {

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
    //String eventSubType;
    private Context context;
    //private String[] eventArray;
    //ContactsEvents eventsData;

    Person(@NonNull Context context, @NonNull String[] eventArray) {

        try {

            this.context = context;
            //this.eventsData = ContactsEvents.getInstance();
            //this.eventArray = eventArray;

            FIO_str = eventArray[ContactsEvents.Position_personFullName];
            int spaceFirst = FIO_str.indexOf(STRING_SPACE);
            if (spaceFirst == -1) { //Имя из одного слова
                FirstName = FIO_str;
                LastName = STRING_EMPTY;
                SecondName = STRING_EMPTY;
            } else {
                int spaceLast = FIO_str.lastIndexOf(STRING_SPACE);
                if (spaceFirst != spaceLast) { //Есть отчество
                    FirstName = FIO_str.substring(0, spaceFirst);
                    SecondName = FIO_str.substring(spaceFirst + 1, spaceLast);
                    LastName = FIO_str.substring(spaceLast + 1);
                } else {
                    FirstName = FIO_str.substring(0, spaceFirst);
                    LastName = FIO_str.substring(spaceFirst + 1);
                    SecondName = STRING_EMPTY;
                }
            }

            try {
                Age = Integer.parseInt(eventArray[ContactsEvents.Position_age]);
            } catch (NumberFormatException e) {
                //Пусто
            }
            Age_str = eventArray[ContactsEvents.Position_age_caption];
            //eventSubType = eventArray[ContactsEvents.Position_eventSubType];

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_CONSTRUCTOR_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    Person(@NonNull Context context, @NonNull String eventData) {
        this(context, eventData.split(Constants.STRING_2HASH));
    }

/*    String getFullName () { //Фамилия Имя Отчество

        try{
            if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_CalendarEvent))) {
                return this.FIO_str;
            } else if (!LastName.isEmpty()) {
                return LastName + (!isEmpty(FirstName) ? STRING_SPACE + FirstName : STRING_EMPTY) + (!isEmpty(SecondName) ? STRING_SPACE + SecondName : STRING_EMPTY);
            } else if (!isEmpty(FirstName)) {
                return FirstName + (!isEmpty(SecondName) ? STRING_SPACE + SecondName : STRING_EMPTY);
            } else {
                return STRING_EMPTY;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_GET_FULL_NAME_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }

    }*/

/*    String getFullNameAlt () { //Имя Отчество Фамилия

        try{
            if (eventSubType.equals(ContactsEvents.eventTypesIDs.get(Type_CalendarEvent))) {
                return this.FIO_str;
            } else if (!isEmpty(FirstName)) {
                return FirstName + (!isEmpty(SecondName) ? STRING_SPACE + SecondName : STRING_EMPTY) + (!isEmpty(LastName) ? STRING_SPACE + LastName : STRING_EMPTY);
            } else {
                return LastName;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_GET_FULL_NAME_ALT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
        }

    }*/


    String getFullNameShort () { //Фамилия И. О.
        //поддержка двойных фамилий и имён пока сделана в WidgetUpdater
        try {
            if (!LastName.equals(STRING_EMPTY)) {
                return LastName + (!FirstName.equals(STRING_EMPTY) ? STRING_SPACE + FirstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : STRING_EMPTY) + (!SecondName.equals(STRING_EMPTY) ? STRING_SPACE + SecondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : STRING_EMPTY);
            } else if (!FirstName.equals(STRING_EMPTY)) {
                return FirstName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD + (!SecondName.equals(STRING_EMPTY) ? STRING_SPACE + SecondName.substring(0, 1).toUpperCase() + Constants.STRING_PERIOD : STRING_EMPTY);
            } else {
                return STRING_EMPTY;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_GET_FULL_NAME_SHORT_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return STRING_EMPTY;
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

                contactsEvents.preferences_last_name_comletions_man = Pattern.compile(context.getString(R.string.last_name_completions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                contactsEvents.preferences_last_name_comletions_female = Pattern.compile(context.getString(R.string.last_name_completions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                contactsEvents.preferences_first_names_man = Pattern.compile(context.getString(R.string.first_names_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                final String names = contactsEvents.preferences_first_names_female_custom.isEmpty() ?
                        context.getString(R.string.first_names_female) :
                        context.getString(R.string.first_names_female).concat(Constants.STRING_COMMA).concat(contactsEvents.preferences_first_names_female_custom.toLowerCase().replace(STRING_COMMA_SPACE, Constants.STRING_COMMA)) ;
                contactsEvents.preferences_first_names_female = Pattern.compile(names.replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                contactsEvents.preferences_second_name_comletions_man = Pattern.compile(context.getString(R.string.second_name_completions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                contactsEvents.preferences_second_name_comletions_female = Pattern.compile(context.getString(R.string.second_name_completions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);

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
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_GET_GENDER_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return -1;
        }

    }

}