package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    //https://medium.com/@kevalpatel2106/how-you-can-decrease-application-size-by-60-in-only-5-minutes-47eff3e7874e
    static final String EMPTY_STRING = "";
    static final String SPACE_STRING = " ";
    static final String Div1 = "###";
    static final String Div2 = "~~~";
    static final String Div3 = ": ";
    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    List<String> dataList = new ArrayList<>();
    SwipeRefreshLayout swiperefresh;
    SwipeRefreshLayout.OnRefreshListener swipeRefreshListner;
    String filterNames = EMPTY_STRING;
    private Menu menu;

    Map <String, Integer> DataMap = new HashMap <>();
    {
        DataMap.put("eventDate_sorted", 0);
        DataMap.put("display_name_alt", 1);
        DataMap.put("contact_id", 2);
        DataMap.put("photo_uri", 3);
        DataMap.put("dates", 4); //account_type: data1
        DataMap.put("eventDate", 5);
        DataMap.put("eventDistance", 6);
        DataMap.put("age", 7);
        DataMap.put("eventType", 8);
        DataMap.put("eventLabel", 9);
    }
    TreeMap<Integer, String> UserData = new TreeMap <>();
    String[] event_types;
    boolean[] event_types_on;

    public int countDaysDiff(Date date1, Date date2) {
        //https://stackoverflow.com/questions/1555262/calculating-the-difference-between-two-java-date-instances/43681941#43681941

        try {
            boolean isNegative = false;
            Calendar c1 = removeTime(from(date1));
            Calendar c2 = removeTime(from(date2));

            if (c1.get(YEAR) == c2.get(YEAR)) {

                return c2.get(DAY_OF_YEAR) - c1.get(DAY_OF_YEAR);
            }
            // ensure c1 <= c2
            if (c1.get(YEAR) > c2.get(YEAR)) {
                isNegative = true;
                Calendar c = c1;
                c1 = c2;
                c2 = c;
            }
            int y1 = c1.get(YEAR);
            int y2 = c2.get(YEAR);
            int d1 = c1.get(DAY_OF_YEAR);
            int d2 = c2.get(DAY_OF_YEAR);

            if (isNegative) {
                return -(d2 + ((y2 - y1) * 365) - d1 + countLeapYearsBetween(y1, y2));
            } else {
                return d2 + ((y2 - y1) * 365) - d1 + countLeapYearsBetween(y1, y2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->countDaysDiff error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            return 0;
        }
    }

    public int countYearsDiff(Date date1, Date date2) {
       try {

           Calendar c1 = removeTime(from(date1));
           Calendar c2 = removeTime(from(date2));

           return c2.get(YEAR) - c1.get(YEAR);

       } catch (Exception e) {
           e.printStackTrace();
           Toast.makeText(this, "MainActivity->countYearsDiff error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
           return 0;
       }
    }

    private static int countLeapYearsBetween(int y1, int y2) {

        if (y1 < 1 || y2 < 1) {
            throw new IllegalArgumentException("Year must be > 0.");
        }
        // ensure y1 <= y2
        if (y1 > y2) {
            int i = y1;
            y1 = y2;
            y2 = i;
        }

        int diff;

        int firstDivisibleBy4 = y1;
        if (firstDivisibleBy4 % 4 != 0) {
            firstDivisibleBy4 += 4 - (y1 % 4);
        }
        diff = y2 - firstDivisibleBy4 - 1;
        int divisibleBy4 = diff < 0 ? 0 : diff / 4 + 1;

        int firstDivisibleBy100 = y1;
        if (firstDivisibleBy100 % 100 != 0) {
            firstDivisibleBy100 += 100 - (firstDivisibleBy100 % 100);
        }
        diff = y2 - firstDivisibleBy100 - 1;
        int divisibleBy100 = diff < 0 ? 0 : diff / 100 + 1;

        int firstDivisibleBy400 = y1;
        if (firstDivisibleBy400 % 400 != 0) {
            firstDivisibleBy400 += 400 - (y1 % 400);
        }
        diff = y2 - firstDivisibleBy400 - 1;
        int divisibleBy400 = diff < 0 ? 0 : diff / 400 + 1;

        return divisibleBy4 - divisibleBy100 + divisibleBy400;
    }

    public static Calendar from(Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        return c;
    }

    public static Calendar removeTime(Calendar c) {

        c.set(HOUR_OF_DAY, 0);
        c.set(MINUTE, 0);
        c.set(SECOND, 0);
        c.set(MILLISECOND, 0);

        return c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            swiperefresh = findViewById(R.id.swiperefresh);
            swiperefresh.setOnRefreshListener(this); //Set the listener to be notified when a refresh is triggered via the swipe gesture

            //https://stackoverflow.com/questions/24587925/swiperefreshlayout-trigger-programmatically/35621309#35621309
            swipeRefreshListner = new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    getContacts();
                    drawList();
                }
            };


            //About
            findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.app_name);
                    builder.setIcon(R.drawable.ic_birthdaycountdowngreen77);

                    //https://stackoverflow.com/a/21119027/4928833
                    builder.setMessage("\nCreated by Vladimir Belov\nbelov.vladimir@mail.ru\nvovka.org\n\n" + "version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");

                    builder.setPositiveButton(R.string.button_OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });


            //Инициализация и считывание настроек

            event_types = new String[]{
                    "Дни рождения",
                    "Годовщины свадьб",
                    "Другие события",
                    "Пользовательские события",
                    "Круглые даты (10K дней)"
            };

            event_types_on = new boolean[]{
                    true,
                    true,
                    true,
                    true,
                    false
            };


            //Получение и отображение контактных данных
            swiperefresh.post(new Runnable() {
                @Override
                public void run() {
                    swiperefresh.setRefreshing(true);
                    swipeRefreshListner.onRefresh();
                    swiperefresh.setRefreshing(false);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->onCreate error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void onRefresh() {

        try {

         swiperefresh.setRefreshing(true);
         swipeRefreshListner.onRefresh();
         swiperefresh.setRefreshing(false); // Disables the refresh icon

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "MyAdapter->onRefresh error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //https://stackoverflow.com/a/43411336/4928833
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        AlertDialog.Builder builder;

        switch (item.getItemId()) {
            case R.id.menu_refresh:
                getContacts();
                drawList();
                return true;

            case R.id.menu_settings:
                //https://alvinalexander.com/android/android-tutorial-preferencescreen-preferenceactivity-preferencefragment
                //https://android--code.blogspot.ru/2015/08/android-alertdialog-multichoice.html

                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_activity_settings);
                builder.setIcon(android.R.drawable.ic_menu_week);

                //final List<String> eventList = Arrays.asList(event_types);

                builder.setMultiChoiceItems(event_types, event_types_on, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                        event_types_on[which] = isChecked;

                    }
                });

                builder.setPositiveButton(R.string.button_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        /*
                        for (int i = 0; i<event_types_on.length; i++){
                            boolean checked = event_types_on[i];
                            if (checked) {
                                Toast.makeText(getApplicationContext(), eventList.get(i), Toast.LENGTH_SHORT).show();
                            }
                        }
                        */

                        getContacts();
                        drawList();

                    }
                });

                builder.setNegativeButton(R.string.button_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;

            case R.id.menu_search:
                //https://stackoverflow.com/questions/10903754/input-text-dialog-android

                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_activity_search);
                builder.setIcon(android.R.drawable.ic_menu_search);

                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(filterNames);
                if (!filterNames.equals(EMPTY_STRING)) input.selectAll();
                builder.setView(input);

                builder.setPositiveButton(R.string.button_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        filterNames = input.getText().toString();

                        if (filterNames.equals(EMPTY_STRING)) {
                            //https://stackoverflow.com/questions/19882443/how-to-change-menuitem-icon-in-actionbar-programmatically/19882555#19882555
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                        } else {
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_zoom));
                        }

                        drawList();
                    }
                });

                if (!filterNames.equals(EMPTY_STRING)) {
                    builder.setNeutralButton(R.string.button_Clear, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            filterNames = EMPTY_STRING;
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                            dialog.dismiss();
                            drawList();
                        }
                    });
                }

                builder.setNegativeButton(R.string.button_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                //builder.show()
                //https://stackoverflow.com/questions/4054662/displaying-soft-keyboard-whenever-alertdialog-builder-object-is-opened/6123935#6123935
                AlertDialog alertToShow = builder.create();
                if (alertToShow.getWindow() != null) alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                alertToShow.show();

                return true;

            case R.id.menu_exit:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onBackPressed()
    {
        //https://stackoverflow.com/questions/18337536/android-overriding-onbackpressed
        if (filterNames.equals(EMPTY_STRING)) {

            super.onBackPressed();

        } else {
            filterNames = EMPTY_STRING;
            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
            drawList();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContacts();
                    drawList();
                }
            }
        }
    }

    public void drawList() {
        try {

            //Фильтруем данные
            String[] array;
            if (filterNames.equals(EMPTY_STRING)) {
                array = dataList.toArray(new String[0]);
            } else {
                List<String> dataList_filtered = new ArrayList<>();
                for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.get(i).toUpperCase().replace("Ё", "Е").contains(filterNames.toUpperCase().replace("Ё", "Е"))) dataList_filtered.add(dataList.get(i));
                }
                array = dataList_filtered.toArray(new String[0]);
            }

            //Вычисляем даты
            String[] dataArray;
            String[] dayArray;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", getResources().getConfiguration().locale);
            SimpleDateFormat skypedf = new SimpleDateFormat("dd MMM yyyy", getResources().getConfiguration().locale);
            SimpleDateFormat sdfYear = new SimpleDateFormat("dd.MM.yyyy", getResources().getConfiguration().locale);
            SimpleDateFormat sdfNoYear = new SimpleDateFormat("dd.MM", getResources().getConfiguration().locale);
            Calendar calendar = Calendar.getInstance();
            Date currentDay = new Date(calendar.getTimeInMillis()); //new Date(System.currentTimeMillis());
            Date BDay = null;
            String accountType;
            String storedDate;
            Date storedDate_Date;
            int Age;
            String AgeString;
            int dayDiff;
            int dayDiff_tmp;
            int storedYear;

            boolean isYear;

            for (int i = 0; i < array.length; i++) {
                dayDiff = -1;
                isYear = false;
                storedYear = 0;
                Age = 0;

                dataArray = array[i].split(Div1);
                dayArray = dataArray[4].split(Div2);

                for (int d = 0; d < dayArray.length && storedYear == 0; d++) {
                    accountType = dayArray[d].substring(0, dayArray[d].indexOf(Div3));
                    storedDate = dayArray[d].substring(dayArray[d].indexOf(Div3) + Div3.length());
                    storedDate_Date = null;

                    if (accountType.equalsIgnoreCase(getResources().getString(R.string.account_google)) || accountType.equalsIgnoreCase(getResources().getString(R.string.account_xiaomi))) {

                        if (storedDate.substring(0, 1).equals("-")) { //Нет года, формат --mm-dd

                            BDay = sdf.parse(Integer.toString(calendar.get(YEAR)) + "-" + storedDate.substring(2));
                            dayDiff_tmp = countDaysDiff(currentDay, BDay);
                            if (dayDiff_tmp < 0) {
                                dayDiff = 365 + dayDiff_tmp;
                            } else {
                                dayDiff = dayDiff_tmp;
                            }

                        } else if (storedDate.substring(0, 5).equals("0000-")) { //Нет года, формат 0000-mm-dd

                            BDay = sdf.parse(Integer.toString(calendar.get(YEAR)) + "-" + storedDate.substring(5));
                            dayDiff_tmp = countDaysDiff(currentDay, BDay);
                            if (dayDiff_tmp < 0) {
                                dayDiff = 365 + dayDiff_tmp;
                            } else {
                                dayDiff = dayDiff_tmp;
                            }

                        } else {

                            try {
                                storedDate_Date = sdf.parse(storedDate);
                            } catch (ParseException e) {
                                try {
                                    storedDate_Date = skypedf.parse(storedDate);
                                } catch (ParseException e2) {
                                    storedDate_Date = null;
                                }
                            }
                        }

                     } else if (accountType.equalsIgnoreCase(getResources().getString(R.string.account_skype))) {

                        try {
                            storedDate_Date = skypedf.parse(storedDate);
                        } catch (ParseException e2) {
                            storedDate_Date = null;
                        }

                    } else if (accountType.equalsIgnoreCase(getResources().getString(R.string.account_vk))) {

                        if (storedDate.substring(0, 5).equals("0000-")) { //Нет года, формат 0000-mm-dd

                            BDay = sdf.parse(Integer.toString(calendar.get(YEAR)) + "-" + storedDate.substring(5));
                            dayDiff_tmp = countDaysDiff(currentDay, BDay);
                            if (dayDiff_tmp < 0) {
                                dayDiff = 365 + dayDiff_tmp;
                            } else {
                                dayDiff = dayDiff_tmp;
                            }

                        } else {

                            try {
                                storedDate_Date = sdf.parse(storedDate);
                            } catch (ParseException e) {
                                try {
                                    storedDate_Date = skypedf.parse(storedDate);
                                } catch (ParseException e2) {
                                    storedDate_Date = null;
                                }
                            }
                        }

                    }

                    if (storedDate_Date != null) {
                        isYear = true;
                        Calendar cal = from(storedDate_Date);
                        storedYear = cal.get(Calendar.YEAR); //storedDate_Date.getYear(); // Integer.parseInt(storedDate.substring(0, 4));
                        BDay = sdf.parse(calendar.get(YEAR) + "-" + (cal.get(Calendar.MONTH)+1) + "-" + cal.get(Calendar.DAY_OF_MONTH)); //storedDate.substring(5));
                        dayDiff_tmp = countDaysDiff(currentDay, BDay);
                        Age = countYearsDiff(storedDate_Date, BDay); //Считаем, сколько будет лет
                        if (dayDiff_tmp < 0) {
                            dayDiff = 365 + dayDiff_tmp;
                            Age += 1;
                        } else {
                            dayDiff = dayDiff_tmp;
                        }
                        BDay = storedDate_Date; //Для вывода пользователю
                    }

                }

                if (dayDiff == -1) {

                    array[i] = EMPTY_STRING;

                } else {

                    dataArray[0] = ("00" + dayDiff).substring(("00" + dayDiff).length() - 3); //Для сортировки
                    if (isYear) { //Дата с годом
                        dataArray[5] = sdfYear.format(BDay);
                    } else { //Дата без года
                        dataArray[5] = sdfNoYear.format(BDay);
                    }

                    if (dayDiff < 3) {
                        dataArray[6] = dayDiff + ""; //Ближайшие 3 дня
                    } else {
                        if (dataArray[8].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {

                            dataArray[6] = getResources().getString(R.string.msg_before_event_prefix) + SPACE_STRING +
                                    getResources().getString(R.string.event_type_birthday_2) + getResources().getString(R.string.msg_before_event_postfix) + SPACE_STRING + dayDiff; //До ДР

                        } else if (dataArray[8].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY))) {

                            dataArray[6] = getResources().getString(R.string.msg_before_event_prefix) + SPACE_STRING +
                                    getResources().getString(R.string.event_type_anniversary_2) + getResources().getString(R.string.msg_before_event_postfix) + SPACE_STRING + dayDiff; //До юбилея

                        } else if (dataArray[8].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM))) {

                            if (dataArray[9].equalsIgnoreCase("дата смерти") || dataArray[9].equalsIgnoreCase("день смерти")) {
                                dataArray[6] = getResources().getString(R.string.msg_before_event_prefix) + SPACE_STRING +
                                        getResources().getString(R.string.event_type_death_2) + getResources().getString(R.string.msg_before_event_postfix) + SPACE_STRING + dayDiff; //До даты

                            } else if (dataArray[9].equalsIgnoreCase("wedding")) {

                                dataArray[6] = getResources().getString(R.string.msg_before_event_prefix) + SPACE_STRING +
                                        getResources().getString(R.string.event_type_anniversary_2) + getResources().getString(R.string.msg_before_event_postfix) + SPACE_STRING + dayDiff; //До юбилея

                            } else {

                                dataArray[6] = getResources().getString(R.string.msg_before_event_prefix) + SPACE_STRING +
                                        getResources().getString(R.string.event_type_other_2) + getResources().getString(R.string.msg_before_event_postfix) + SPACE_STRING + dayDiff; //До даты

                            }

                        } else {

                            dataArray[6] = getResources().getString(R.string.msg_before_event_prefix) + SPACE_STRING +
                                    getResources().getString(R.string.event_type_other_2) + getResources().getString(R.string.msg_before_event_postfix) + SPACE_STRING + dayDiff; //До даты

                        }
                    }

                    if (Age > 0) {
                        String Age_tmp = Integer.toString(Age);
                        String Age_end = Age_tmp.substring(Age_tmp.length() - 1);

                        if (Age > 4 && Age < 21) {
                            AgeString = Age_tmp + " лет";
                        } else if (Age_end.equals("1")) {
                            AgeString = Age_tmp + " год";
                        } else if (Age_end.equals("2") || Age_end.equals("3") || Age_end.equals("4")) {
                            AgeString = Age_tmp + " года";
                        } else {
                            AgeString = Age_tmp + " лет";
                        }
                        dataArray[7] = AgeString;

                    }
                    array[i] = TextUtils.join(Div1, dataArray);
                }
            }

            //Удаляем пустые
            List<String> tmpList = new ArrayList<>();
            for (String s : array) {
                if(s != null && s.length() > 0) {
                    tmpList.add(s);
                }
            }
            array = tmpList.toArray(new String[tmpList.size()]);
            tmpList.clear();

            //Сортируем
            Arrays.sort(array);

            //Статистика
            if (filterNames.equals(EMPTY_STRING)) {
                if (array.length > 0) {
                    showMsgbox(getResources().getString(R.string.msg_stats_prefix) + SPACE_STRING + String.valueOf(array.length), EMPTY_STRING);
                } else {
                    showMsgbox(getString(R.string.msg_no_events), EMPTY_STRING);
                }

            } else {
                if (array.length > 0) {
                    showMsgbox(getResources().getString(R.string.msg_stats_prefix) + SPACE_STRING + String.valueOf(array.length) + getString(R.string.msg_filter), filterNames);
                } else {
                    showMsgbox(getString(R.string.msg_no_events) + getString(R.string.msg_filter), filterNames);
                }
            }

            //Выводим данные
            ListAdapter adapter = new MyAdapter(this, Arrays.copyOf(array, array.length));

            ListView listView = findViewById(R.id.mainListView);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> l, View v, int position, long id)
                {
                    try {

                        String[] dataArray = ((String) l.getItemAtPosition(position)).split(Div1);

                        //https://stackoverflow.com/questions/4275167/how-to-open-a-contact-card-in-android-by-id?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, dataArray[2]);
                        intent.setData(uri);
                        MainActivity.this.startActivity(intent);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "drawList->onItemClick error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> l, View view, int position, long id) {

                    String s = (String) l.getItemAtPosition(position);
                    Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG * 5).show();

                    return true;
                }

            });

            listView.requestFocus();


        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->drawList error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void getContacts() {
        try {
            //StringBuilder output;
            dataList.clear();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                //https://developer.android.com/training/permissions/requesting.html#java

                showMsgbox(getString(R.string.msg_no_access), EMPTY_STRING);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                return;

            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                showMsgbox(getString(R.string.msg_no_access), EMPTY_STRING);
                return;
            }

            int BDindex;
            String FIO = EMPTY_STRING;
            StringBuilder dataRow;

            //Получаем требуемые события (дни рождения, и т.п.)
            List<String> EventTypes = new ArrayList<>();
            if (event_types_on[0]) EventTypes.add(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY + EMPTY_STRING);
            if (event_types_on[1]) EventTypes.add(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY + EMPTY_STRING);
            if (event_types_on[2]) EventTypes.add(ContactsContract.CommonDataKinds.Event.TYPE_OTHER + EMPTY_STRING);
            if (event_types_on[3]) EventTypes.add(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM + EMPTY_STRING);

            ContentResolver contentResolver = getContentResolver();

            for (int t = 0; t < EventTypes.size(); t++) {
                Cursor cursor = contentResolver.query(
                        android.provider.ContactsContract.Data.CONTENT_URI,
                        null,
                        ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "' AND " + ContactsContract.CommonDataKinds.Event.TYPE + " = " + EventTypes.get(t),
                        null,
                        ContactsContract.Data.DISPLAY_NAME_ALTERNATIVE
                ); //android.provider.ContactsContract.Data.CONTACT_ID+" = "+contact_id+" AND

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            BDindex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.DATA);
                            if (cursor.getString(BDindex) != null) {
                                if (!cursor.getString(cursor.getColumnIndex("display_name_alt")).equalsIgnoreCase(FIO)) { //Начало данных контакта

                                    if (!UserData.isEmpty()) { // Следующий контакт. Нужно сохранить всё, что накопили и обнулить UserData

                                        dataRow = new StringBuilder();
                                        int rNum = 0;
                                        for (Map.Entry<Integer, String> entry : UserData.entrySet()) {
                                            rNum++;
                                            if (rNum == 1) {
                                                dataRow.append(entry.getValue());
                                            } else {
                                                dataRow.append(Div1);
                                                dataRow.append(entry.getValue());
                                            }
                                        }
                                        dataList.add(dataRow.toString());
                                    }

                                    FIO = cursor.getString(cursor.getColumnIndex("display_name_alt"));

                                    UserData.clear();

                                    UserData.put(DataMap.get("eventDate_sorted"), SPACE_STRING);
                                    UserData.put(DataMap.get("display_name_alt"), FIO.replace(", ", " "));
                                    UserData.put(DataMap.get("contact_id"), cursor.getString(cursor.getColumnIndex("contact_id")));
                                    UserData.put(DataMap.get("photo_uri"), cursor.getString(cursor.getColumnIndex("photo_uri")));
                                    UserData.put(DataMap.get("eventDate"), SPACE_STRING); //Дата
                                    UserData.put(DataMap.get("eventDistance"), SPACE_STRING); //Дней до даты
                                    UserData.put(DataMap.get("age"), SPACE_STRING); //Возраст
                                    UserData.put(DataMap.get("eventType"), EventTypes.get(t)); //Тип события

                                    if (EventTypes.get(t).equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {
                                        UserData.put(DataMap.get("eventLabel"), getResources().getString(R.string.event_type_birthday));
                                    } else if (EventTypes.get(t).equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY))) {
                                        UserData.put(DataMap.get("eventLabel"), getResources().getString(R.string.event_type_anniversary));
                                    } else if (EventTypes.get(t).equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER))) {
                                        UserData.put(DataMap.get("eventLabel"), getResources().getString(R.string.event_type_other));
                                    } else {
                                        UserData.put(DataMap.get("eventLabel"), cursor.getString(cursor.getColumnIndex("data3"))); //Заголовок события
                                    }

                                /* if (FIO.equalsIgnoreCase("Белов Владимир")) {
                                    for (int i = 1; i <= cursor.getColumnCount() - 1; i++) {
                                        if (cursor.getString(i) != null) {
                                            output.append("\n" + cursor.getColumnName(i) + ": " + cursor.getString(i));
                                        }
                                    }
                                } */

                                    UserData.put(DataMap.get("dates"), cursor.getString(cursor.getColumnIndex("account_type")).concat(Div3).concat(cursor.getString(BDindex)));

                                } else { //Продолжаем добавлять даты контакта

                                    UserData.put(DataMap.get("dates"), UserData.get(DataMap.get("dates")).concat(Div2).concat(cursor.getString(cursor.getColumnIndex("account_type")).concat(Div3).concat(cursor.getString(BDindex))));

                                }

                            }
                        } while (cursor.moveToNext());

                        if (!UserData.isEmpty()) { // Данные последнего контакта

                            dataRow = new StringBuilder();
                            int rNum = 0;
                            for (Map.Entry<Integer, String> entry : UserData.entrySet()) {
                                rNum++;
                                if (rNum == 1) {
                                    dataRow.append(entry.getValue());
                                } else {
                                    dataRow.append(Div1);
                                    dataRow.append(entry.getValue());
                                }
                            }
                            dataList.add(dataRow.toString());
                            UserData.clear();

                        }

                    } else {
                        showMsgbox(getString(R.string.msg_no_events), EMPTY_STRING);
                    }
                    cursor.close();
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->getContacts error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void showMsgbox(String msg, String msg_red) {

        StringBuffer output = new StringBuffer();
        output.append(msg);
        TextView stats = findViewById(R.id.mainStatsTextView);
        stats.setText(output);

        output = new StringBuffer();
        output.append(msg_red);
        stats = findViewById(R.id.mainStatsRedTextView);
        stats.setText(output);

    }

    private static class ViewHolder {
        //https://stackoverflow.com/questions/21501316/what-is-the-benefit-of-viewholder

        final TextView NameTextView;
        final TextView DayDistanceTextView;
        final TextView DateTextView;
        final TextView DetailsTextView;
        final ImageView PhotoImageView;
        final TextView CounterTextView;
        final ImageView EventIconImageView;

        ViewHolder(TextView NameTextView, TextView DayDistanceTextView, TextView DateTextView, TextView DetailsTextView, ImageView PhotoImageView, TextView CounterTextView, ImageView EventIconImageView) {
            this.NameTextView = NameTextView;
            this.DayDistanceTextView = DayDistanceTextView;
            this.DateTextView = DateTextView;
            this.DetailsTextView = DetailsTextView;
            this.PhotoImageView = PhotoImageView;
            this.CounterTextView = CounterTextView;
            this.EventIconImageView = EventIconImageView;
        }
    }

    class MyAdapter extends ArrayAdapter<String>
    {
        private MyAdapter(Context context, String[] values)
        {
            super(context, R.layout.entry_main, values);
        }

        @NonNull public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            String[] dataArray;

            try {

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.entry_main, parent, false);
                    holder = createViewHolderFrom(convertView);
                    convertView.setTag(holder);
                }
                holder = (ViewHolder) convertView.getTag();

                final String dataRow = getItem(position);
                if (dataRow != null) {
                    dataArray = dataRow.split(Div1);
                } else {
                    return convertView;
                }

                if (dataArray.length <= 9) {
                    Toast.makeText(getContext(), "MyAdapter->getView error:\nAbnormal dimension of string:\n" + dataRow, Toast.LENGTH_LONG).show();
                    return convertView;
                }

                holder.NameTextView.setText(dataArray[DataMap.get("display_name_alt")]);

                switch (dataArray[DataMap.get("eventDistance")]) {
                    case "0": //Сегодня

                        holder.DayDistanceTextView.setText(getResources().getString(R.string.msg_today));
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                        break;

                    case "1": //Завтра

                        holder.DayDistanceTextView.setText(getResources().getString(R.string.msg_tomorrow));
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                        break;

                    case "2": //Послезавтра

                        holder.DayDistanceTextView.setText(getResources().getString(R.string.msg_day_after_tomorrow));
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                        break;

                     default: //Попозже

                         holder.DayDistanceTextView.setText(dataArray[DataMap.get("eventDistance")]);
                         holder.DayDistanceTextView.setTypeface(null, Typeface.NORMAL);
                         holder.DayDistanceTextView.setTextColor(Color.DKGRAY);

                }

                holder.DateTextView.setText(dataArray[DataMap.get("eventDate")]);
                holder.DetailsTextView.setText(dataArray[DataMap.get("eventLabel")].concat("\n").concat(dataArray[DataMap.get("dates")].replace(Div2, "\n")));

                if (!dataArray[DataMap.get("photo_uri")].equalsIgnoreCase("null")) {
                    //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, dataArray[2]);
                    InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contactUri);
                    BufferedInputStream buf = new BufferedInputStream(photo_stream);
                    Bitmap bm = BitmapFactory.decodeStream(buf);
                    buf.close();
                    photo_stream.close();
                    holder.PhotoImageView.setImageBitmap(bm);
                } else {
                    holder.PhotoImageView.setImageResource(R.drawable.ic_action_name);
                }

                if (!dataArray[DataMap.get("age")].equals(SPACE_STRING)) {

                    if (Integer.parseInt(dataArray[DataMap.get("age")].substring(0, dataArray[DataMap.get("age")].indexOf(SPACE_STRING))) % 10 == 0) {
                        holder.CounterTextView.setTextColor(getResources().getColor(R.color.dark_red));
                    } else {
                        holder.CounterTextView.setTextColor(Color.DKGRAY);
                    }
                    holder.CounterTextView.setText(dataArray[DataMap.get("age")]);

                } else {

                    holder.CounterTextView.setText(EMPTY_STRING);
                }

                if (dataArray[DataMap.get("eventType")].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {

                    //https://icons8.com/icon/21460/birthday
                    holder.EventIconImageView.setImageResource(R.mipmap.ic_event_birthday);

                } else if (dataArray[DataMap.get("eventType")].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY))) {

                    //https://www.flaticon.com/free-icon/wedding-rings_224802
                    //holder.EventIconImageView.setImageResource(R.mipmap.ic_event_anniversary);
                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_wedding);

                } else if (dataArray[DataMap.get("eventType")].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER))) {

                    holder.EventIconImageView.setImageResource(R.mipmap.ic_event_other);

                } else if (dataArray[DataMap.get("eventType")].equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM))) {

                    String eventLabel = dataArray[DataMap.get("eventLabel")];

                    if (eventLabel.equalsIgnoreCase("дата смерти") || eventLabel.equalsIgnoreCase("день смерти")) {

                        holder.EventIconImageView.setImageResource(R.mipmap.ic_event_death);

                    } else if (eventLabel.equalsIgnoreCase("именины")) {

                        holder.EventIconImageView.setImageResource(R.mipmap.ic_event_other);

                    } else if (eventLabel.equalsIgnoreCase("wedding")) {

                        //holder.EventIconImageView.setImageResource(R.mipmap.ic_event_anniversary);
                        holder.EventIconImageView.setImageResource(R.drawable.ic_event_wedding);

                    } else {

                        holder.EventIconImageView.setImageDrawable(null);

                    }

                } else {
                    //https://stackoverflow.com/questions/6643432/remove-the-image-from-a-imageview-android
                    holder.EventIconImageView.setImageDrawable(null);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "MyAdapter->getView error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                return inflater.inflate(R.layout.entry_main, parent, false);
            } else {
                return convertView;
            }
        }

        /*
        //https://www.tutorialspoint.com/android/android_list_fragment.htm
        //https://stackoverflow.com/questions/17268110/how-to-setup-the-onclicklistener-for-the-imageview-in-the-listview/17268172#17268172
        public class onMyClick implements View.OnClickListener {

            private final int pos;
            public onMyClick(int pos) {
                this.pos = pos;
            }

            @Override
            public void onClick(View v) {
                //mMyListFragment.imagepos(pos);
            }

        }
        */

        private ViewHolder createViewHolderFrom(View view) {

            TextView NameTextView = view.findViewById(R.id.entryNameTextView);
            TextView DayDistanceTextView = view.findViewById(R.id.entryDayDistanceTextView);
            TextView DateTextView = view.findViewById(R.id.entryDateTextView);
            TextView DetailsTextView = view.findViewById(R.id.entryEventDetailsTextView);
            TextView CounterTextView = view.findViewById(R.id.entryDetailsCounter);
            ImageView PhotoImageView = view.findViewById(R.id.entryPhotoImageView);
            ImageView EventIconImageView = view.findViewById(R.id.entryEventIcon);

            return new ViewHolder(NameTextView, DayDistanceTextView, DateTextView, DetailsTextView, PhotoImageView, CounterTextView, EventIconImageView);
        }

    }
}

