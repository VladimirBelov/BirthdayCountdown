/*
 * *
 *  * Created by Vladimir Belov on 12.10.2021, 0:19
 *  * Copyright (c) 2018 - 2021. All rights reserved.
 *  * Last modified 12.10.2021, 0:16
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import static org.vovka.birthdaycountdown.Constants.STRING_COMMA_SPACE;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;

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
    private int Gender = 0; // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён
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
            if (Gender != 0) return Gender;

            if (ContactsEvents.getInstance().preferences_last_name_comletions_man == null) {//Ищем первый раз
                //eventsData.preferences_first_names_female = new HashSet<>(Arrays.asList(context.getString(R.string.first_names_female).split(ContactsEvents.STRING_COMMA)));

                final String regex_inter = "\\Z|";
                final String regex_last = "\\Z";

                ContactsEvents.getInstance().preferences_last_name_comletions_man = Pattern.compile(context.getString(R.string.last_name_completions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                ContactsEvents.getInstance().preferences_last_name_comletions_female = Pattern.compile(context.getString(R.string.last_name_completions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                ContactsEvents.getInstance().preferences_first_names_man = Pattern.compile(context.getString(R.string.first_names_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                final String names = ContactsEvents.getInstance().preferences_first_names_female_custom.isEmpty() ?
                        context.getString(R.string.first_names_female) :
                        context.getString(R.string.first_names_female).concat(Constants.STRING_COMMA).concat(ContactsEvents.getInstance().preferences_first_names_female_custom.toLowerCase().replace(STRING_COMMA_SPACE, Constants.STRING_COMMA)) ;
                ContactsEvents.getInstance().preferences_first_names_female = Pattern.compile(names.replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                ContactsEvents.getInstance().preferences_second_name_comletions_man = Pattern.compile(context.getString(R.string.second_name_completions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                ContactsEvents.getInstance().preferences_second_name_comletions_female = Pattern.compile(context.getString(R.string.second_name_completions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);

            }

            int ind = 0;
            if (!this.LastName.isEmpty()) {
                if (ContactsEvents.getInstance().preferences_last_name_comletions_man.reset(ContactsEvents.normalizeName(this.LastName)).find()) {ind++;}
                else if (ContactsEvents.getInstance().preferences_last_name_comletions_female.reset(ContactsEvents.normalizeName(this.LastName)).find()) {ind--;}
            }

            if (!this.SecondName.isEmpty()) {
                if (ContactsEvents.getInstance().preferences_second_name_comletions_man.reset(ContactsEvents.normalizeName(this.SecondName)).find()) {ind++;}
                else if (ContactsEvents.getInstance().preferences_second_name_comletions_female.reset(ContactsEvents.normalizeName(this.SecondName)).find()) {ind--;}
            }

            if (!this.FirstName.isEmpty()) {
                if (ContactsEvents.getInstance().preferences_first_names_man.reset(ContactsEvents.normalizeName(this.FirstName)).find()) {ind++;}
                else if (ContactsEvents.getInstance().preferences_first_names_female.reset(ContactsEvents.normalizeName(this.FirstName)).find()) {ind--;}
            }

            if (ind > 0) {Gender = 1;} else if (ind < 0) {Gender = 2;} else {Gender = -1;}
            return Gender;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_GET_GENDER_ERROR + e.toString(), Toast.LENGTH_LONG).show();
            return -1;
        }

    }

}