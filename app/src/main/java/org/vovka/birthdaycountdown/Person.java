/*
 * *
 *  * Created by Vladimir Belov on 20.07.20 1:05
 *  * Copyright (c) 2018 - 2020. All rights reserved.
 *  * Last modified 19.07.20 23:43
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import static android.text.TextUtils.isEmpty;
import static org.vovka.birthdaycountdown.Constants.STRING_EMPTY;
import static org.vovka.birthdaycountdown.Constants.STRING_SPACE;
import static org.vovka.birthdaycountdown.Constants.Type_CalendarEvent;

class Person {

    private String LastName;
    private String FirstName;
    private String SecondName;
    private int Gender = 0; // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён
    int Age = -1;
    String Age_str;
    private Context context;
    private String[] eventArray;
    ContactsEvents eventsData;

    private Person(@NonNull Context context, @NonNull String[] eventArray) {

        try {

            this.context = context;
            this.eventsData = ContactsEvents.getInstance();
            this.eventArray = eventArray;
            String strFIO;
            strFIO = this.eventArray[ContactsEvents.Position_fio];
            int spaceFirst = strFIO.indexOf(STRING_SPACE);
            if (spaceFirst == -1) { //Имя из одного слова
                FirstName = strFIO;
                LastName = STRING_EMPTY;
                SecondName = STRING_EMPTY;
            } else {
                int spaceLast = strFIO.lastIndexOf(STRING_SPACE);
                if (spaceFirst != spaceLast) { //Есть отчество
                    LastName = strFIO.substring(0, spaceFirst);
                    FirstName = strFIO.substring(spaceFirst + 1, spaceLast);
                    SecondName = strFIO.substring(spaceLast + 1);
                } else {
                    LastName = strFIO.substring(0, spaceFirst);
                    FirstName = strFIO.substring(spaceFirst + 1);
                    SecondName = STRING_EMPTY;
                }
            }

            try {
                Age = Integer.parseInt(this.eventArray[ContactsEvents.Position_age]);
            } catch (NumberFormatException e) {
                //Пусто
            }
            Age_str = this.eventArray[ContactsEvents.Position_age_caption];

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, Constants.PERSON_CONSTRUCTOR_ERROR + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    Person(@NonNull Context context, @NonNull String eventData) {
        this(context, eventData.split(Constants.STRING_2HASH));
    }

    String getFullName () { //Фамилия Имя Отчество

        try{
            if (this.eventArray[ContactsEvents.Position_eventSubType].equals(eventsData.eventTypesIDs.get(Type_CalendarEvent))) {
                return this.eventArray[ContactsEvents.Position_fio];
            } else if (!LastName.equals(STRING_EMPTY)) {
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

    }

    String getFullNameAlt () { //Имя Отчество Фамилия

        try{
            if (this.eventArray[ContactsEvents.Position_eventSubType].equals(eventsData.eventTypesIDs.get(Type_CalendarEvent))) {
                return this.eventArray[ContactsEvents.Position_fio];
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

    }

    /*
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
    */

    int getGender() { //Определение пола по фамилии, имени, отчеству
        // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён
        //https://github.com/vadimiztveri/sex_by_russian_name/blob/master/src/sex_by_russian_name.js
        //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

        try {
            if (Gender != 0) return Gender;

            if (eventsData.preferences_last_name_comletions_man == null) {//Ищем первый раз
                //eventsData.preferences_first_names_female = new HashSet<>(Arrays.asList(context.getString(R.string.first_names_female).split(ContactsEvents.STRING_COMMA)));

                final String regex_inter = "\\Z|";
                final String regex_last = "\\Z";

                eventsData.preferences_last_name_comletions_man = Pattern.compile(context.getString(R.string.last_name_comletions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                eventsData.preferences_last_name_comletions_female = Pattern.compile(context.getString(R.string.last_name_comletions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                eventsData.preferences_first_names_man = Pattern.compile(context.getString(R.string.first_names_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                eventsData.preferences_first_names_female = Pattern.compile(context.getString(R.string.first_names_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                eventsData.preferences_second_name_comletions_man = Pattern.compile(context.getString(R.string.second_name_comletions_man).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);
                eventsData.preferences_second_name_comletions_female = Pattern.compile(context.getString(R.string.second_name_comletions_female).replace(Constants.STRING_COMMA, regex_inter) + regex_last).matcher(STRING_EMPTY);

            }

            int ind = 0;
            if (!this.LastName.equals(STRING_EMPTY)) {
                if (eventsData.preferences_last_name_comletions_man.reset(this.LastName.toLowerCase()).find()) {ind++;}
                else if (eventsData.preferences_last_name_comletions_female.reset(this.LastName.toLowerCase()).find()) {ind--;}
            }

            if (!this.SecondName.equals(STRING_EMPTY)) {
                if (eventsData.preferences_second_name_comletions_man.reset(this.SecondName.toLowerCase()).find()) {ind++;}
                else if (eventsData.preferences_second_name_comletions_female.reset(this.SecondName.toLowerCase()).find()) {ind--;}
            }

            if (!this.FirstName.equals(STRING_EMPTY)) {
                if (eventsData.preferences_first_names_man.reset(this.FirstName.toLowerCase()).find()) {ind++;}
                else if (eventsData.preferences_first_names_female.reset(this.FirstName.toLowerCase()).find()) {ind--;}
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