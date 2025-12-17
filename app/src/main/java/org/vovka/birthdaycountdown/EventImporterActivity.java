/*
 * *
 *  * Created by Vladimir Belov on 18.12.2025, 02:05
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 18.12.2025, 02:05
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeMap;

public class EventImporterActivity extends AppCompatActivity {

    private static final String TAG = "EventImporterActivity";
    private static final ContactsEvents eventsData;

    static {
        eventsData = ContactsEvents.getInstance();
    }

    public EventImporterActivity() {}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);

            eventsData.initLanguage(this);

            setTheme(eventsData.preferences_theme.themeMain);
            this.getTheme().applyStyle(R.style.FloatingActivity, true);
            setContentView(R.layout.activity_import);

            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            final String action = intent.getAction();

            if (extras == null || !Constants.ACTION_IMPORT_EVENTS.equals(action) ) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            TextView title = findViewById(R.id.textCaption);
            if (title != null) title.setText(R.string.pref_Tools_Events_Import_Dialog_title);

            TextView summary = findViewById(R.id.summary);
            List<String> dataForImport = getEventsToImport(Uri.parse(extras.getString(Constants.EXTRA_URL)));
            summary.setText(dataForImport.get(0));

            try {
                RecyclerView recyclerView = findViewById(R.id.listEvents);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));

                DividerItemDecoration divider = new DividerItemDecoration(
                        recyclerView.getContext(),
                        DividerItemDecoration.VERTICAL
                );
                recyclerView.addItemDecoration(divider);

                List<EventItem> events = new ArrayList<>();

                for (int i = 1; i < dataForImport.size(); i++) {
                    TreeMap<Integer, String> eventData = eventsData.getEventData(dataForImport.get(i));

                    String details = ContactsEvents.getString(eventData.get(ContactsEvents.Position_eventCaption))
                            .concat(Constants.STRING_COLON_SPACE)
                            .concat(ContactsEvents.getString(eventData.get(ContactsEvents.Position_eventDateFirstTime)));
                    events.add(new EventItem(
                            eventData.get(ContactsEvents.Position_eventIcon),
                            eventData.get(ContactsEvents.Position_personFullName),
                            details)
                    );
                }

                EventListAdapter adapter = new EventListAdapter(events);
                adapter.selectAll();
                recyclerView.setAdapter(adapter);

            } catch (Exception e) {
                ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
                ToastExpander.showDebugMsg(context, e.getMessage() != null ? e.getMessage() : e.toString());
            }

            //Ширина диалога
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            layoutParams.width = displayMetrics.widthPixels;
            getWindow().setAttributes(layoutParams);

            TextView buttonCloseX = findViewById(R.id.buttonClose);
            if (buttonCloseX != null) {
                buttonCloseX.setText(Constants.BUTTON_X);
                buttonCloseX.setOnClickListener(this::buttonCancelOnClick);
            }

            setFinishOnTouchOutside(false);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
            ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /** Возвращает список событий, которые могут быть импортированы из файла
     * @param uri Путь до файла
     * @return Список событий для импорта. Первым элементом списка идёт результат анализа файла
     */
    @NotNull
    private List<String> getEventsToImport(Uri uri) {
        List<String> eventsList = new ArrayList<>();
        List<String> details = new ArrayList<>();
        int statEventsSkipped = 0; //Не поддерживается или с ошибкой
        int statEventsDoubles = 0; //Дубль с существующим событием
        int statEventsUnRecognized = 0; //Тип не распознан или без типа

        try {

            details.add("Файл: " + eventsData.getPath(this, uri));
            String fileContent = Constants.STRING_EMPTY;

            if (uri != null) fileContent = eventsData.readFileToString(uri.toString(), Constants.STRING_EOL);

            if (fileContent.isEmpty()) {
                details.add("🚫 Файл пустой или нет доступа");
                return eventsList;
            }

            if (fileContent.startsWith(Constants.iCal_CalendarBegin)) {
                details.add("🛑 Пока не поддерживается");

            } else {

                //BirthdayPro, DarkBirthday: <Дата без пробелов>[,<пробел>флаги[тип события]] название праздника или ФИО [(должность)] [http:// или https:// ссылка]
                Calendar today = ContactsEvents.getWithoutTime(new GregorianCalendar());
                String[] eventsArray =  fileContent.split(Constants.STRING_EOL, -1);
                for (String eventString : eventsArray) {

                    String eventLine = eventString.trim().replace("\uFEFF", Constants.STRING_EMPTY);
                    if (eventLine.isEmpty() || eventLine.startsWith(Constants.STRING_HASH) || eventLine.startsWith(Constants.STRING_DSLASH)) continue;
                    int indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                    if (indexFirstSpace == -1) continue;

                    String eventDateString;
                    String eventLabel_forSearch = Constants.STRING_EMPTY;
                    String eventTitle;
                    boolean isEndless = true;
                    boolean isAD = true;

                    final int indexComma = eventLine.indexOf(Constants.STRING_COMMA);
                    if (indexComma > -1 && indexComma < indexFirstSpace) { //Есть флаги

                        if (indexFirstSpace - indexComma == 1) { //После запятой пробел - убираем
                            eventLine = eventLine.substring(0, indexComma + 1) + eventLine.substring(indexFirstSpace + 1);
                            indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
                            if (indexFirstSpace == -1) {
                                statEventsSkipped++;
                                continue;
                            }
                        }

                        eventDateString = eventLine.substring(0, indexComma);
                        String flags = eventLine.substring(indexComma + 1, indexFirstSpace);
                        eventTitle = eventLine.substring(indexFirstSpace + 1).trim();

                        if (!flags.isEmpty()) {
                            if (flags.contains(Constants.STRING_1)) {
                                isEndless = false;
                                flags = flags.replace(Constants.STRING_1, Constants.STRING_EMPTY);
                            }
                            if (flags.contains(Constants.STRING_BC)) {
                                isAD = false;
                                flags = flags.replace(Constants.STRING_BC, Constants.STRING_EMPTY);
                            }
                            eventLabel_forSearch = flags.replace(Constants.STRING_UNDERSCORE, Constants.STRING_SPACE);
                        }

                    } else {

                        eventDateString = eventLine.substring(0, indexFirstSpace);
                        eventTitle = eventLine.substring(indexFirstSpace + 1).trim();

                    }

                    if (eventDateString.isEmpty() || eventTitle.isEmpty()) {
                        statEventsSkipped++;
                        continue;
                    }

                    boolean useEventYear = true;
                    int indexDateNoYear = eventDateString.indexOf(Constants.STRING_0000);
                    if (indexDateNoYear != -1) useEventYear = false;

                    ContactsEvents.ComputedDateForFileEvent result = eventsData.getComputedDateForFileEvent(today, indexDateNoYear, isAD, false, eventDateString, null, isEndless);
                    if (result.dateEvent == null) {
                        statEventsSkipped++;
                        continue;
                    }

                    ContactsEvents.Event event = eventsData.recognizeEventByLabel(eventLabel_forSearch, Constants.Storage_File, false, useEventYear);
                    if (event.type.equals(ContactsEvents.getEventType(Constants.Type_Unrecognized))) {
                        statEventsUnRecognized++;
                    }

                    @Nullable Date dateEvent = null;
                    if (indexDateNoYear == -1) { //С годом
                        try {
                            if (isAD) {
                                    String dateNextFloatingEvent = eventsData.computeFloatingDate(eventDateString, 0);
                                    if (!eventDateString.equals(dateNextFloatingEvent)) {
                                        //Пока не поддерживается
                                        statEventsSkipped++;
                                        continue;
                                    }
                                dateEvent = ContactsEvents.sdf_DDMMYYYY.parse(eventDateString);
                            } else {
                                dateEvent = ContactsEvents.sdf_DDMMYYYY_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                            }
                        } catch (ParseException e1) {
                            try {
                                if (isAD) {
                                    dateEvent = ContactsEvents.sdf_india.parse(eventDateString);
                                } else {
                                    dateEvent = ContactsEvents.sdf_india_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                }
                            } catch (ParseException e2) {
                                try {
                                    if (isAD) {
                                        dateEvent = ContactsEvents.sdf_uk.parse(eventDateString);
                                    } else {
                                        dateEvent = ContactsEvents.sdf_uk_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                    }
                                } catch (ParseException e3) {
                                    try {
                                        if (isAD) {
                                            dateEvent = ContactsEvents.sdf_java.parse(eventDateString);
                                        } else {
                                            dateEvent = ContactsEvents.sdf_java_G.parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                                        }
                                    } catch (ParseException e4) {
                                        //Не получилось распознать
                                    }
                                }
                            }
                        }

                    } else { //Без года

                        String dateNextEvent = eventDateString.replace(Constants.STRING_0000, String.valueOf(today.get(Calendar.YEAR)));
                        try {
                                String dateNextFloatingEvent = eventsData.computeFloatingDate(dateNextEvent, 0);
                                if (!dateNextEvent.equals(dateNextFloatingEvent)) {
                                    //Пока не поддерживается
                                    statEventsSkipped++;
                                    continue;
                                }
                            dateEvent = ContactsEvents.sdf_DDMMYYYY.parse(dateNextEvent);
                        } catch (ParseException e1) {
                            try {
                                dateEvent = ContactsEvents.sdf_india.parse(dateNextEvent);
                            } catch (ParseException e2) {
                                try {
                                    dateEvent = ContactsEvents.sdf_uk.parse(dateNextEvent);
                                } catch (ParseException e3) {
                                    try {
                                        dateEvent = ContactsEvents.sdf_java.parse(dateNextEvent);
                                    } catch (ParseException e4) {
                                        //Не получилось распознать
                                    }
                                }
                            }
                        }
                    }

                    if (dateEvent == null) {
                        statEventsSkipped++;
                        continue;
                    } else {
                        eventDateString = ContactsEvents.sdf_DDMMYYYY.format(dateEvent);
                    }

                    //Собираем событие
                    TreeMap<Integer, String> eventData = new TreeMap<>();
                    if (!event.needScanContacts) {
                        eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                        eventData.put(ContactsEvents.Position_personFullNameAlt, Constants.STRING_EMPTY);
                    } else if (eventsData.preferences_name_format == ContactsEvents.FormatName.LastnameFirst) {
                        eventData.put(ContactsEvents.Position_personFullNameAlt, eventTitle);
                        String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.LastnameFirst, this);
                        eventData.put(ContactsEvents.Position_personFullName, personFullNameAlt);
                    } else {
                        eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                        String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.NameFirst, this);
                        eventData.put(ContactsEvents.Position_personFullNameAlt, personFullNameAlt);
                    }

                    eventData.put(ContactsEvents.Position_eventCaption, event.caption);
                    //eventData.put(Position_eventLabel, event.label);
                    //eventData.put(Position_eventSource, eventSource);
                    eventData.put(ContactsEvents.Position_eventType, event.type);
                    eventData.put(ContactsEvents.Position_eventSubType, event.subType);
                    //eventData.put(Position_dates, eventNewDate);
                    eventData.put(ContactsEvents.Position_eventIcon, Integer.toString(event.icon));
                    eventData.put(ContactsEvents.Position_eventEmoji, event.emoji);
                    if (useEventYear) {
                        eventData.put(ContactsEvents.Position_eventDateFirstTime, eventDateString);
                    } else {
                        eventData.put(ContactsEvents.Position_eventDateFirstTime, eventDateString.substring(0, indexDateNoYear - 1));
                    }

                    eventsData.fillEmptyEventData(eventData);
                    String eventDataAsString = eventsData.getEventData(eventData);

                    //Проверка на существующие локальные события
                    List<String> similarEventIds = eventsData.getSimilarLocalEventIds(eventDataAsString,
                            EnumSet.of(
                                    ContactsEvents.getSimilarFields.PERSON_FULL_NAME,
                                    ContactsEvents.getSimilarFields.ORGANIZATION
                            ));

                    if (similarEventIds != null) {
                        statEventsDoubles++;
                    } else {
                        eventsList.add(eventDataAsString);
                    }
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            details.add("Ошибка: " + e.getMessage());
        } finally {
            if (!eventsList.isEmpty()) {
                details.add("Событий найдено: " + eventsList.size());
            }
            if (statEventsUnRecognized > 0) {
                details.add("Типов событий не распознано: " + statEventsUnRecognized);
            }
            if (statEventsSkipped > 0) {
                details.add("Событий пропущено: " + statEventsSkipped);
            }
            if (statEventsDoubles > 0) {
                details.add("Дублей пропущено: " + statEventsDoubles);
            }
            eventsList.add(0, String.join(Constants.STRING_EOL, details));
        }
        return eventsList;
    }

    public void buttonCancelOnClick(final View view) {
            setResult(RESULT_CANCELED);
            finish();
    }

    static class EventItem {
        private int iconResId;
        private final String title;
        private final String subtitle;

        public EventItem(String iconResIdString, String title, String subtitle) {
            try {
                this.iconResId = Integer.parseInt(iconResIdString);
            } catch (NumberFormatException pe) {
                this.iconResId = R.drawable.ic_event_unknown;
            }
            this.title = title;
            this.subtitle = subtitle;
        }

        public int getIconResId() { return iconResId; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
    }

    static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

        private final List<EventItem> eventList;
        private final boolean[] selectedItems;

        public EventListAdapter(List<EventItem> eventList) {
            this.eventList = eventList;
            this.selectedItems = new boolean[eventList.size()];
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            EventItem item = eventList.get(position);
            holder.icon.setImageResource(item.getIconResId());
            holder.title.setText(item.getTitle());
            holder.subtitle.setText(item.getSubtitle());
            holder.checkbox.setChecked(selectedItems[position]);

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    selectedItems[pos] = !selectedItems[pos];
                    notifyItemChanged(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return eventList.size();
        }

        public List<Integer> getSelectedPositions() {
            List<Integer> selected = new ArrayList<>();
            for (int i = 0; i < selectedItems.length; i++) {
                if (selectedItems[i]) {
                    selected.add(i);
                }
            }
            return selected;
        }

        public void clearSelection() {
            List<Integer> previouslySelected = new ArrayList<>();
            for (int i = 0; i < selectedItems.length; i++) {
                if (selectedItems[i]) {
                    selectedItems[i] = false;
                    previouslySelected.add(i);
                }
            }
            for (int position : previouslySelected) {
                notifyItemChanged(position);
            }
        }

        public void selectAll() {
            boolean needNotify = false;
            for (int i = 0; i < selectedItems.length; i++) {
                if (!selectedItems[i]) {
                    selectedItems[i] = true;
                    needNotify = true;
                }
            }
            if (needNotify) {
                notifyItemRangeChanged(0, selectedItems.length);
            }
        }

        static class EventViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkbox;
            ImageView icon;
            TextView title;
            TextView subtitle;

            EventViewHolder(@NonNull View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.checkbox);
                icon = itemView.findViewById(R.id.icon);
                title = itemView.findViewById(R.id.title);
                subtitle = itemView.findViewById(R.id.subtitle);
            }
        }
    }
}
