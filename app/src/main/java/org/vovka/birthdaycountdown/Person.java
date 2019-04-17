package org.vovka.birthdaycountdown;

import android.content.Context;
import android.widget.Toast;

import java.util.regex.Pattern;

class Person {
    private String LastName;
    private String FirstName;
    private String SecondName;
    private int Gender = 0; // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён
    int Age = -1;
    String Age_str;
    private Context context;

    Person(Context context, String eventData) {

        try {
            this.context = context;
            String[] singleRowArray = eventData.split(ContactsEvents.Div1);
            Integer ind = ContactsEvents.dataMap.get("fio");
            String strFIO;
            if (ind != null) {
                strFIO = singleRowArray[ind];
            } else {
                strFIO = "";
            }
            int spaceFirst = strFIO.indexOf(" ");
            if (spaceFirst == -1) { //Имя из одного слова
                FirstName = strFIO;
                LastName = "";
                SecondName = "";
            } else {
                int spaceLast = strFIO.lastIndexOf(" ");
                if (spaceFirst != spaceLast) { //Есть отчество
                    LastName = strFIO.substring(0, spaceFirst);
                    FirstName = strFIO.substring(spaceFirst + 1, spaceLast);
                    SecondName = strFIO.substring(spaceLast + 1);
                } else {
                    LastName = strFIO.substring(0, spaceFirst);
                    FirstName = strFIO.substring(spaceFirst + 1);
                    SecondName = "";
                }
            }

            ind = ContactsEvents.dataMap.get("age");
            if (ind != null) {
                try {
                    Age = Integer.parseInt(singleRowArray[ind]);
                } catch (NumberFormatException e) {
                    //Пусто
                }
            }

            ind = ContactsEvents.dataMap.get("age_caption");
            Age_str = ind != null ? singleRowArray[ind] : "";

            /*ind = ContactsEvents.dataMap.get("age");
            if (ind != null) {
                if (ind <= singleRowArray.length) Age_str = singleRowArray[ind];
                if (Age_str == null || Age_str.equals(" ")) Age_str = "";
            } else {
                Age_str = "";
            }

            Age = -1;
            if (!Age_str.equals("")) {
                if (Age_str.contains(" ")) {
                    Age = Integer.parseInt(Age_str.substring(0, Age_str.indexOf(" ")));
                }
            }*/

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Person->Constructor error: " + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
        }

    }

    String getFullName () { //Фамилия Имя Отчество

        try{
            if (!LastName.equals("")) {
                return LastName + (!FirstName.equals("") ? " " + FirstName : "") + (!SecondName.equals("") ? " " + SecondName : "");
            } else if (!FirstName.equals("")) {
                return FirstName + (!SecondName.equals("") ? " " + SecondName : "");
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Person->getFullName error: " + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
            return "";
        }

    }

    String getFullNameAlt () { //Имя Отчество Фамилия

        try{
            if (!FirstName.equals("")) {
                return FirstName + (!SecondName.equals("") ? " " + SecondName : "") + (!LastName.equals("") ? " " + LastName : "");
            } else {
                return LastName;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Person->getFullNameAlt error: " + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
            return "";
        }

    }

    String getFullNameShort () { //Фамилия И. О.

        /*
        //todo: сделать поддержку двойных фамилий и имён
        // Fetch contact name with a specific ID
        Cursor nameFIO = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = " + contactID,
                new String[] {ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                null
        );
        while (nameFIO.moveToNext()) {
            String given = nameFIO.getString(cache.getColumnIndex(nameFIO, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            String family = nameFIO.getString(cache.getColumnIndex(nameFIO, ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            String display = nameFIO.getString(cache.getColumnIndex(nameFIO, ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
            //Toast.makeText(this, "Name: " + given + " Family: " +  family + " Displayname: "  + display, Toast.LENGTH_LONG).show();
            if (family != null) {
                userData.put(dataMap.get("lastName"), family);
                break;
            }
        }
        nameFIO.close();
        */

        try {
            if (!LastName.equals("")) {
                return LastName + (!FirstName.equals("") ? " " + FirstName.substring(0, 1).toUpperCase() + "." : "") + (!SecondName.equals("") ? " " + SecondName.substring(0, 1).toUpperCase() + "." : "");
            } else if (!FirstName.equals("")) {
                return FirstName.substring(0, 1).toUpperCase() + "." + (!SecondName.equals("") ? " " + SecondName.substring(0, 1).toUpperCase() + "." : "");
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Person->getFullNameShort error: " + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
            return "";
        }

    }

    int getGender() { //Определение пола по фамилии, имени, отчеству
        // 1 - мужской, 2 - женский, 0 - не определяли, -1 - не определён
        //https://github.com/vadimiztveri/sex_by_russian_name/blob/master/src/sex_by_russian_name.js
        //https://stackoverflow.com/questions/19829892/java-regular-expressions-performance-and-alternative

        try {
            if (Gender != 0) return Gender;

            ContactsEvents eventsData = ContactsEvents.getInstance();

            if (eventsData.preferences_last_name_comletions_man == null) {//Ищем первый раз
                //eventsData.preferences_first_names_female = new HashSet<>(Arrays.asList(context.getString(R.string.first_names_female).split(ContactsEvents.Div4)));

                final String regex_inter = "\\Z|";
                final String regex_last = "\\Z";

                eventsData.preferences_last_name_comletions_man = Pattern.compile(context.getString(R.string.last_name_comletions_man).replace(ContactsEvents.Div4, regex_inter) + regex_last).matcher("");
                eventsData.preferences_last_name_comletions_female = Pattern.compile(context.getString(R.string.last_name_comletions_female).replace(ContactsEvents.Div4, regex_inter) + regex_last).matcher("");
                eventsData.preferences_first_names_man = Pattern.compile(context.getString(R.string.first_names_man).replace(ContactsEvents.Div4, regex_inter) + regex_last).matcher("");
                eventsData.preferences_first_names_female = Pattern.compile(context.getString(R.string.first_names_female).replace(ContactsEvents.Div4, regex_inter) + regex_last).matcher("");
                eventsData.preferences_second_name_comletions_man = Pattern.compile(context.getString(R.string.second_name_comletions_man).replace(ContactsEvents.Div4, regex_inter) + regex_last).matcher("");
                eventsData.preferences_second_name_comletions_female = Pattern.compile(context.getString(R.string.second_name_comletions_female).replace(ContactsEvents.Div4, regex_inter) + regex_last).matcher("");

            }

//            Toast.makeText(context, "Pattern: " + eventsData.preferences_last_name_comletions_female.pattern().toString(), Toast.LENGTH_LONG).show();
//            Toast.makeText(context, "Gender: " + (eventsData.preferences_last_name_comletions_female.reset("Белова").find() ? "Female" : "Man"), Toast.LENGTH_LONG).show();

            int ind = 0;
            if (!this.LastName.equals("")) {
                if (eventsData.preferences_last_name_comletions_man.reset(this.LastName.toLowerCase()).find()) {ind++;}
                else if (eventsData.preferences_last_name_comletions_female.reset(this.LastName.toLowerCase()).find()) {ind--;}
            }

            if (!this.SecondName.equals("")) {
                if (eventsData.preferences_second_name_comletions_man.reset(this.SecondName.toLowerCase()).find()) {ind++;}
                else if (eventsData.preferences_second_name_comletions_female.reset(this.SecondName.toLowerCase()).find()) {ind--;}
            }

            if (!this.FirstName.equals("")) {
                if (eventsData.preferences_first_names_man.reset(this.FirstName.toLowerCase()).find()) {ind++;}
                else if (eventsData.preferences_first_names_female.reset(this.FirstName.toLowerCase()).find()) {ind--;}
            }

            if (ind > 0) {Gender = 1;} else if (ind < 0) {Gender = 2;} else {Gender = -1;}
            return Gender;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Person->getGender error: " + e.getMessage() + " in line " + e.getStackTrace()[0].getLineNumber(), Toast.LENGTH_LONG).show();
            return -1;
        }

    }

}