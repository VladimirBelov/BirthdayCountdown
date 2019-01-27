package org.vovka.birthdaycountdown;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static org.vovka.birthdaycountdown.ContactsEvents.Div4;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    //https://medium.com/@kevalpatel2106/how-you-can-decrease-application-size-by-60-in-only-5-minutes-47eff3e7874e

    //Константы
    static final String SPACE_STRING = " ";
    static final String FAKE_STRING = "#~#";

    final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;


    //UI объекты
    SwipeRefreshLayout swipeRefresh;
    SwipeRefreshLayout.OnRefreshListener swipeRefreshListener;
    private Menu menu;

    //Переменные
    String filterNames = "";
    ContactsEvents eventsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            swipeRefresh = findViewById(R.id.swiperefresh);
            swipeRefresh.setOnRefreshListener(this); //Set the listener to be notified when a refresh is triggered via the swipe gesture

            eventsData = ContactsEvents.getInstance();
            eventsData.context = this;
            eventsData.setLocale();

            //Обновляем меню https://stackoverflow.com/questions/14867458/android-refresh-options-menu-without-calling-invalidateoptionsmenu
            this.invalidateOptionsMenu();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                //https://developer.android.com/training/permissions/requesting.html#java

                showMsgbox(getString(R.string.msg_no_access), "");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                swipeRefresh.setRefreshing(false);
                return;

            }

            //https://stackoverflow.com/questions/24587925/swiperefreshlayout-trigger-programmatically/35621309#35621309
            swipeRefreshListener = () -> {
                eventsData.getContactsEvents();
                eventsData.computeDates();
                drawList();
                updateWidgets();
                swipeRefresh = findViewById(R.id.swiperefresh);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false); // Disables the refresh icon
            };

            //About
            findViewById(R.id.toolbar).setOnClickListener(v -> {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setIcon(R.drawable.ic_birthdaycountdowngreen77);

                //https://stackoverflow.com/a/21119027/4928833
                //https://stackoverflow.com/questions/3540739/how-to-programmatically-read-the-date-when-my-android-apk-was-built
                SimpleDateFormat formater = new SimpleDateFormat("dd MMM yyyy HH:mm", getResources().getConfiguration().locale);
                formater.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                builder.setMessage("\nCreated by Vladimir Belov\nbelov.vladimir@mail.ru\nvovka.org\n" +
                        "\nversion: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")" +
                        "\nbuild date: " + formater.format(BuildConfig.BUILD_TIME) +
                        "\nload speed:\n" +
                        "\tcontacts scanning: " + Math.round(eventsData.statGetContacts * 100.0) / 100.0 + "msec\n" +
                        "\tdates computing: " + Math.round(eventsData.statComputeDates * 100.0) / 100.0 + "msec\n" +
                        "\tlist drawing: " + Math.round(eventsData.statDrawList * 100.0) / 100.0 + "msec"
                ); //https://stackoverflow.com/questions/11701399/round-up-to-2-decimal-places-in-java

                builder.setPositiveButton(R.string.button_OK, (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder.create();
                dialog.show();
            });

            //Получение и отображение контактных данных
            swipeRefresh.post(() -> {
                swipeRefresh.setRefreshing(true);
                swipeRefreshListener.onRefresh();
                swipeRefresh.setRefreshing(false);
            });

        } catch (Exception e) {
            e.printStackTrace();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "MainActivity->onCreate error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    public void onRefresh() {

        if (swipeRefresh == null || swipeRefreshListener == null) return;
        try {

            //swipeRefresh.setRefreshing(true);
            //setLocale();
            swipeRefreshListener.onRefresh();
            swipeRefresh.setRefreshing(false); // Disables the refresh icon

        } catch (Exception e) {
            e.printStackTrace();
            swipeRefresh.setRefreshing(false);
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

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {

            AlertDialog.Builder builder;

            switch (item.getItemId()) {
                case R.id.menu_refresh:
                    //todo: показывать анимацию обновления: https://github.com/googlesamples/android-SwipeRefreshLayoutBasic/blob/master/Application/src/main/java/com/example/android/swiperefreshlayoutbasic/SwipeRefreshLayoutBasicFragment.java
                    swipeRefresh.setRefreshing(true);
                    eventsData.getContactsEvents();
                    eventsData.computeDates();
                    drawList();
                    swipeRefresh.setRefreshing(false);
                    updateWidgets();
                /*
                swipeRefresh.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(true);
                        swipeRefreshListener.onRefresh();
                        swipeRefresh.setRefreshing(false);
                    }
                }); */
                    return true;

                case R.id.menu_settings:

                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);

                    return true;

                case R.id.menu_search:
                    //https://stackoverflow.com/questions/10903754/input-text-dialog-android

                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.title_activity_search);
                    builder.setIcon(android.R.drawable.ic_menu_search);
                    builder.setMessage(R.string.msg_hint_search);

                    final EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(filterNames);
                    if (!filterNames.equals("")) input.selectAll();
                    builder.setView(input);

                    builder.setPositiveButton(R.string.button_OK, (dialog13, which) -> {
                        filterNames = input.getText().toString();

                        if (filterNames.equals("")) {
                            //https://stackoverflow.com/questions/19882443/how-to-change-menuitem-icon-in-actionbar-programmatically/19882555#19882555
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                        } else {
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_zoom));
                        }

                        //eventsData.computeDates(); при задании фильтра даты пересчитывать не надо
                        drawList();
                    });

                    if (!filterNames.equals("")) {
                        builder.setNeutralButton(R.string.button_Clear, (dialog12, which) -> {
                            filterNames = "";
                            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
                            dialog12.dismiss();
                            //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо
                            drawList();
                        });
                    }

                    builder.setNegativeButton(R.string.button_Cancel, (dialog1, which) -> dialog1.cancel());

                    builder.setCancelable(true);

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
        } catch (Exception e) {
            e.printStackTrace();
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "MyAdapter->onOptionsItemSelected error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onBackPressed()
    {
        //https://stackoverflow.com/questions/18337536/android-overriding-onbackpressed
        if (filterNames.equals("")) {

            super.onBackPressed();

        } else {
            filterNames = "";
            menu.getItem(2).setIcon(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_menu_search));
            //eventsData.computeDates(); при очистке фильтра даты пересчитывать не надо
            drawList();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        swipeRefresh.setRefreshing(true);
        eventsData.setLocale();
        boolean canReadContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if (canReadContacts && (eventsData.dataArray == null || System.currentTimeMillis() - eventsData.statLastComputeDates > 5000)) {
            eventsData.getContactsEvents();
        }
        eventsData.computeDates();
        drawList();
        updateWidgets();
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    eventsData.getContactsEvents();
                    eventsData.computeDates();
                    drawList();
                    updateWidgets();
                }
            }
        }
    }

    public void drawList() {
        long statCurrentModuleStart = System.currentTimeMillis();

        String[] dataArray_filtered = {};
        try {

            //Статистика
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                showMsgbox(getString(R.string.msg_no_access), "");

            } else if (eventsData.dataArray == null) {

                showMsgbox(getString(R.string.msg_no_events), "");

            } else if (filterNames.equals("")) {
                if (eventsData.dataArray.length > 0) {
                    showMsgbox(getResources().getString(R.string.msg_stats_prefix) + SPACE_STRING + String.valueOf(eventsData.dataArray.length), "");
                    dataArray_filtered = eventsData.dataArray;
                } else {

                    showMsgbox(getString(R.string.msg_no_events), "");

                }
            } else {

                List<String> dataList_filtered = new ArrayList<>();
                String[] filterArray = filterNames.split(Div4);
                for (String listItem : eventsData.dataArray) {
                    for (String filterValue : filterArray) {
                        if (listItem.toUpperCase().replace("Ё", "Е").contains(filterValue.toUpperCase().replace("Ё", "Е"))) {
                            if (!dataList_filtered.contains(listItem)) {
                                dataList_filtered.add(listItem);
                            }
                        }
                    }
                }
                dataArray_filtered = dataList_filtered.toArray(new String[0]);

                if (dataArray_filtered.length > 0) {

                    showMsgbox(getResources().getString(R.string.msg_stats_prefix) + SPACE_STRING + String.valueOf(dataArray_filtered.length) + getString(R.string.msg_filter), filterNames);

                } else {

                    showMsgbox(getString(R.string.msg_no_events) + getString(R.string.msg_filter), filterNames);

                }
            }

            //Выводим данные
            ListAdapter adapter = new MyAdapter(this, Arrays.copyOf(dataArray_filtered, dataArray_filtered.length));

            ListView listView = findViewById(R.id.mainListView);

            //Сохраняем позицию в списке, чтобы вернутся к ней после обновления
            //https://stackoverflow.com/a/3035521/4928833
            int index = listView.getFirstVisiblePosition();
            View v = listView.getChildAt(0);
            int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

            listView.setAdapter(adapter);

            //todo: реализовать SectionIndexer http://androidopentutorials.com/android-listview-fastscroll/
            listView.setFastScrollEnabled(true);

            listView.setOnItemClickListener((l, v1, position, id) -> {
                try {

                    String[] dataArray1 = ((String) l.getItemAtPosition(position)).split(ContactsEvents.Div1);

                    //https://stackoverflow.com/questions/4275167/how-to-open-a-contact-card-in-android-by-id?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, dataArray1[eventsData.dataMap.get("contact_id")]);
                    intent.setData(uri);
                    MainActivity.this.startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "drawList->onItemClick error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            listView.setOnItemLongClickListener((l, view, position, id) -> {

                String s = (String) l.getItemAtPosition(position);

                //todo: сделать вместо ЭТОГО контекстное меню или всплывающий диплог с полной инфо о событии
                //https://startandroid.ru/ru/uroki/vse-uroki-spiskom/47-urok-15-kontekstnoe-menju.html

                //todo: подсказки про именины на основе имени и даты рождения
                //todo: знаки зодиака и года

                final Toast tag = Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG);
                tag.show();

                //https://stackoverflow.com/a/7173248/4928833
                new CountDownTimer(4000, 1000)
                {

                    public void onTick(long millisUntilFinished) {tag.show();}
                    public void onFinish() {tag.show();}

                }.start();

                return true;
            });

            listView.requestFocus();
            //Возвращаемся к ранее сохранённой позиции после обновления
            listView.setSelectionFromTop(index, top);

            eventsData.statDrawList = System.currentTimeMillis() - statCurrentModuleStart;

        } catch (Exception e) {
            eventsData.statDrawList = System.currentTimeMillis() - statCurrentModuleStart;
            e.printStackTrace();
            Toast.makeText(this, "MainActivity->drawList error:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void showMsgbox(String msg, String msg_red) {

        StringBuilder output = new StringBuilder();
        output.append(msg);
        TextView stats = findViewById(R.id.mainStatsTextView);
        stats.setText(output);

        output = new StringBuilder();
        output.append(msg_red);
        stats = findViewById(R.id.mainStatsRedTextView);
        stats.setText(output);

    }

    public void updateWidgets() {

        //Посылаем сообщения на обновление виджетов

        // https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver
        Intent intent = new Intent(this, Widget2x2.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, Widget2x2.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        this.sendBroadcast(intent);

        intent = new Intent(this, Widget5x1.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, Widget5x1.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        this.sendBroadcast(intent);

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
            String[] singleRowArray;

            try {

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.entry_main, parent, false);
                    holder = createViewHolderFrom(convertView);
                    convertView.setTag(holder);
                }
                holder = (ViewHolder) convertView.getTag();

                if (getItem(position) != null) {
                    singleRowArray = getItem(position).split(ContactsEvents.Div1);
                } else {
                    return convertView;
                }

                if (singleRowArray.length <= 9) {
                    Toast.makeText(getContext(), "MyAdapter->getView error:\nAbnormal dimension of string: " + singleRowArray.length, Toast.LENGTH_LONG).show();
                    return convertView;
                }

                holder.NameTextView.setText(singleRowArray[eventsData.dataMap.get("fio")]);

                switch (singleRowArray[eventsData.dataMap.get("eventDistance")]) {
                    case "0": //Сегодня

                        holder.DayDistanceTextView.setText(getResources().getString(R.string.msg_today));
                        holder.DayDistanceTextView.setTypeface(null, Typeface.BOLD);
                        holder.DayDistanceTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.dark_red));
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

                        holder.DayDistanceTextView.setText(singleRowArray[eventsData.dataMap.get("eventDistance")]);
                        holder.DayDistanceTextView.setTypeface(null, Typeface.NORMAL);
                        holder.DayDistanceTextView.setTextColor(Color.DKGRAY);

                }

                holder.DateTextView.setText(singleRowArray[eventsData.dataMap.get("eventDate")]);
                String eventSubLabel = singleRowArray[eventsData.dataMap.get("eventLabel")].trim();
                //todo: если выключают debug, то название свадьбы дублируется (до переоткрытия)
                if (eventsData.preferences_list_debug) eventSubLabel = eventSubLabel.concat("\n").concat(singleRowArray[eventsData.dataMap.get("dates")].replace(ContactsEvents.Div2, "\n")).trim();
                holder.DetailsTextView.setText(eventSubLabel);

                if (!singleRowArray[eventsData.dataMap.get("photo_uri")].equalsIgnoreCase("null")) {
                    //https://stackoverflow.com/questions/3870638/how-to-use-setimageuri-on-android?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleRowArray[eventsData.dataMap.get("contact_id")]);
                    InputStream photo_stream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), contactUri, true); // мистика - поставил preferHires и загрузка фото стала в 2 раза быстрее
                    BufferedInputStream buf = new BufferedInputStream(photo_stream);
                    Bitmap bm = BitmapFactory.decodeStream(buf);
                    buf.close();
                    photo_stream.close();
                    holder.PhotoImageView.setImageBitmap(bm);
                } else {
                    holder.PhotoImageView.setImageResource(R.drawable.no_photo);
                }

                if (!singleRowArray[eventsData.dataMap.get("age")].equals(" ")) {

                    holder.CounterTextView.setTextColor(Color.DKGRAY);
                    if (singleRowArray[eventsData.dataMap.get("age")].contains(" ")) {
                        if (Integer.parseInt(singleRowArray[eventsData.dataMap.get("age")].substring(0, singleRowArray[eventsData.dataMap.get("age")].indexOf(" "))) % 10 == 0) {
                            holder.CounterTextView.setTextColor(getResources().getColor(R.color.dark_red));
                        }
                    }
                    holder.CounterTextView.setText(singleRowArray[eventsData.dataMap.get("age")]);

                } else {

                    holder.CounterTextView.setText("");
                }


                //Определяем иконку события
                String eventType = singleRowArray[eventsData.dataMap.get("eventType")];
                String eventLabel = eventType.equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM)) ? singleRowArray[eventsData.dataMap.get("eventLabel")].toLowerCase() : FAKE_STRING;

                if (eventType.equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY))) {

                    //https://icons8.com/icon/21460/birthday
                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_birthday);

                    //todo: сделать частичное вхождение значения синонима
                } else if (eventType.equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY)) ||
                        getResources().getString(R.string.event_type_wedding_labels).contains(eventLabel)) {

                    //https://www.flaticon.com/free-icon/wedding-rings_224802
                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_wedding);

                } else if (eventType.equals(Integer.toString(ContactsContract.CommonDataKinds.Event.TYPE_OTHER)) ||
                        getResources().getString(R.string.event_type_name_day_labels).contains(eventLabel)) {

                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_other);

                } else if (eventType.equals(Integer.toString(eventsData.event_types_id[4]))) {

                    //https://www.flaticon.com/free-icon/medal_610333
                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_medal);

                } else if (getResources().getString(R.string.event_type_death_labels).contains(eventLabel)) {

                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_death);

                } else if (getResources().getString(R.string.event_type_crowning_labels).contains(eventLabel)) {

                    //https://iconscout.com/icon/wedding-destination-romance-building-emoj-symbol
                    holder.EventIconImageView.setImageResource(R.drawable.ic_event_crowning);

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