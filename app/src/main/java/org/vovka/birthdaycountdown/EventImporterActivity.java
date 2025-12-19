/*
 * *
 *  * Created by Vladimir Belov on 20.12.2025, 01:54
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 20.12.2025, 01:45
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeMap;

public class EventImporterActivity extends AppCompatActivity {

    private static final String TAG = "EventImporterActivity";
    private static final ContactsEvents eventsData;
    EventListAdapter adapter = null;
    private int firstVisiblePosition = -1;
    private int topOffset = 0;
    final List<String> dataForImport = new ArrayList<>();
    private final List<String> eventTypesValues = new ArrayList<>();
    private final List<Integer> eventTypesIds = new ArrayList<>();
    private final List<Integer> eventSubTypesIds = new ArrayList<>();
    boolean hasUnrecognizedEvents = false;
    RecyclerView recyclerView;
    TextView viewEventType;
    Spinner spinnerEventTypes;
    TextView buttonSelectAll;
    TextView buttonSelectNone;
    TextView buttonCancel;
    TextView buttonImport;

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
            dataForImport.addAll(getEventsToImport(Uri.parse(extras.getString(Constants.EXTRA_URL))));
            summary.setText(dataForImport.get(0));
            dataForImport.remove(0);

            viewEventType = findViewById(R.id.captionType);
            spinnerEventTypes = findViewById(R.id.spinnerEventType);
            buttonSelectAll = findViewById(R.id.buttonFirstAction);
            buttonSelectNone = findViewById(R.id.buttonSecondAction);
            buttonCancel = findViewById(R.id.buttonThirdAction);
            buttonImport = findViewById(R.id.buttonFourthAction);

            if (!dataForImport.isEmpty()) {
                try {
                    recyclerView = findViewById(R.id.listEvents);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));

                    DividerItemDecoration divider = new DividerItemDecoration(
                            recyclerView.getContext(),
                            DividerItemDecoration.VERTICAL
                    );
                    recyclerView.addItemDecoration(divider);

                    List<EventItem> events = new ArrayList<>();
                    for (String eventStr : dataForImport) {
                        TreeMap<Integer, String> eventData = eventsData.getEventData(eventStr);

                        String details = ContactsEvents.getNotNullString(eventData.get(ContactsEvents.Position_eventCaption))
                                .concat(Constants.STRING_COLON_SPACE)
                                .concat(ContactsEvents.getNotNullString(eventData.get(ContactsEvents.Position_eventDateFirstTime)));
                        events.add(new EventItem(
                                eventData.get(ContactsEvents.Position_eventIcon),
                                eventData.get(ContactsEvents.Position_personFullName), //todo: вывод Position_personFullNameAlt
                                details)
                        );
                        if (!hasUnrecognizedEvents && ContactsEvents.getNotNullString(eventData.get(ContactsEvents.Position_eventType))
                                .equals(ContactsEvents.getEventType(Constants.Type_Unrecognized))) {
                            hasUnrecognizedEvents = true;
                        }
                    }

                    adapter = new EventListAdapter(events);
                    adapter.setOnSelectionChangedListener((allSelected, noneSelected) -> {
                        buttonSelectAll.setAlpha(allSelected ? 0.4f : 1.0f);
                        buttonSelectNone.setAlpha(noneSelected ? 0.4f : 1.0f);
                        buttonImport.setAlpha(noneSelected ? 0.4f : 1.0f);
                    });

                    recyclerView.setAdapter(adapter);
                    recyclerView.setVisibility(View.VISIBLE);

                    buttonSelectAll.setText("✅ Все");
                    addClickEffect(buttonSelectAll);
                    buttonSelectAll.setVisibility(View.VISIBLE);
                    buttonSelectAll.setOnClickListener(v -> {
                        saveRecyclerViewScrollPosition();
                        updateSelectionWithoutAnimation(true);
                        restoreRecyclerViewScrollPosition();
                    });

                    buttonSelectNone.setText("⭕ Ни одного");
                    addClickEffect(buttonSelectNone);
                    buttonSelectNone.setVisibility(View.VISIBLE);
                    buttonSelectNone.setOnClickListener(v -> {
                        saveRecyclerViewScrollPosition();
                        updateSelectionWithoutAnimation(false);
                        restoreRecyclerViewScrollPosition();
                    });

                    buttonImport.setText("↩️ Импорт");
                    addClickEffect(buttonImport);
                    buttonImport.setVisibility(View.VISIBLE);
                    buttonImport.setOnClickListener(v -> doImport());

                    adapter.selectAll();

                    if (hasUnrecognizedEvents) {
                        viewEventType.setVisibility(View.VISIBLE);

                        eventsData.initEventTypes(eventTypesValues, eventTypesIds, eventSubTypesIds);
                        eventTypesValues.add(0, getResources().getString(R.string.event_type_unknown_emoji) + Constants.STRING_SPACE + getResources().getString(R.string.pref_Tools_Events_Import_Unrecognized_type));
                        eventTypesIds.add(0, Constants.Type_Unrecognized);
                        eventSubTypesIds.add(0, Constants.Type_Unrecognized);

                        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, eventTypesValues);
                        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                        spinnerEventTypes.setAdapter(spinnerArrayAdapter);
                        spinnerEventTypes.setVisibility(View.VISIBLE);
                    }

                } catch (Exception e) {
                    ContextThemeWrapper context = new ContextThemeWrapper(this, eventsData.preferences_theme.themeMain);
                    ToastExpander.showDebugMsg(context, e.getMessage() != null ? e.getMessage() : e.toString());
                }
            }

            buttonCancel.setText("❌ Отмена");
            addClickEffect(buttonCancel);
            buttonCancel.setVisibility(View.VISIBLE);
            buttonCancel.setOnClickListener(this::buttonCancelOnClick);

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

    private void updateSelectionWithoutAnimation(boolean selectAll) {
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        recyclerView.setItemAnimator(null);
        try {
            if (selectAll) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        } finally {
            recyclerView.setItemAnimator(animator);
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

                    boolean isUnrecognizedEvent = false;
                    ContactsEvents.Event event = eventsData.recognizeEventByLabel(eventLabel_forSearch, Constants.Storage_File, false, useEventYear);
                    if (event.type.equals(ContactsEvents.getEventType(Constants.Type_Unrecognized))) {
                        isUnrecognizedEvent = true;
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

                    //URLs
                    String eventURL = Constants.STRING_EMPTY;
                    String eventTitle_lowered = eventTitle.toLowerCase();
                    int urlOffset = eventTitle_lowered.indexOf(Constants.STRING_HTTP);
                    if (urlOffset > -1) {
                        eventURL = eventTitle.substring(urlOffset);
                    } else {
                        urlOffset = eventTitle_lowered.indexOf(Constants.STRING_HTTPS);
                        if (urlOffset > -1) {
                            eventURL = eventTitle.substring(urlOffset);
                        }
                    }
                    if (urlOffset > -1) {
                        eventURL = ContactsEvents.substringBefore(eventURL, Constants.STRING_SPACE);
                        eventData.put(ContactsEvents.Position_eventURL, eventURL);
                        eventTitle = eventTitle.replace(eventURL, Constants.STRING_EMPTY).trim();
                    }

                    //Описание события
                    int indStartDescription = eventTitle.indexOf(Constants.STRING_BAR);
                    if (indStartDescription > -1) {
                        int pStartFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_START);
                        if (pStartFirst > -1) {
                            if (indStartDescription < pStartFirst) { //"|" до "("
                                String eventDescription = eventTitle.substring(indStartDescription + 1, pStartFirst);
                                if (!eventDescription.isEmpty()) {
                                    eventData.put(ContactsEvents.Position_eventDescription, eventDescription.trim());
                                    eventTitle = eventTitle.replace(eventDescription.concat(Constants.STRING_BAR), Constants.STRING_EMPTY).trim();
                                }
                            }
                        } else {
                            String eventDescription = eventTitle.substring(indStartDescription + 1);
                            if (!eventDescription.isEmpty()) {
                                eventData.put(ContactsEvents.Position_eventDescription, eventDescription.trim());
                                eventTitle = eventTitle.replace(eventDescription, Constants.STRING_EMPTY).trim();
                            }
                        }
                    }

                    if (event.needScanContacts) {
                        //всё, что внутри скобок в имени - в должность и организацию
                        int pStartFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_START);
                        int pStartLast = eventTitle.lastIndexOf(Constants.STRING_PARENTHESIS_START);
                        int pEndFirst = eventTitle.indexOf(Constants.STRING_PARENTHESIS_CLOSE);
                        int pEndLast = eventTitle.lastIndexOf(Constants.STRING_PARENTHESIS_CLOSE);
                        String contactTitle = null;

                        if (pStartFirst > -1 && pEndFirst > pStartFirst) { //хотя бы пара скобок
                            if (pStartFirst == pStartLast && pEndFirst == pEndLast) { //одна пара скобок
                                contactTitle = eventTitle.substring(pStartFirst + 1, pEndFirst);
                                eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                            } else if (pStartLast < pEndFirst && pStartLast < pEndLast) { //скобки внутри скобок
                                contactTitle = eventTitle.substring(pStartFirst + 1, pEndLast);
                                eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                            } else if (pEndFirst < pStartLast) { //пара скобок за другой парой
                                contactTitle = eventTitle.substring(pStartLast + 1, pEndLast);
                                eventTitle = eventTitle.replace(Constants.STRING_PARENTHESIS_START + contactTitle + Constants.STRING_PARENTHESIS_CLOSE, Constants.STRING_EMPTY).trim();
                            }
                            if (contactTitle != null) {
                                int cStart = contactTitle.indexOf(Constants.STRING_COMMA);
                                if (cStart > 0) {
                                    eventData.put(ContactsEvents.Position_organization, contactTitle.substring(0, cStart).trim());
                                    eventData.put(ContactsEvents.Position_title, contactTitle.substring(cStart + 1).trim());
                                } else {
                                    eventData.put(ContactsEvents.Position_title, contactTitle.trim());
                                }
                            }
                        }
                    }

                    if (!event.needScanContacts) {
                        eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                        eventData.put(ContactsEvents.Position_personFullNameAlt, Constants.STRING_EMPTY);
                    } else if (eventsData.preferences_rules_files_name_format == ContactsEvents.FormatName.LastnameFirst) {
                        eventData.put(ContactsEvents.Position_personFullNameAlt, eventTitle);
                        String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.LastnameFirst, this);
                        eventData.put(ContactsEvents.Position_personFullName, personFullNameAlt);
                    } else {
                        eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                        String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.NameFirst, this);
                        eventData.put(ContactsEvents.Position_personFullNameAlt, personFullNameAlt);
                    }

                    eventData.put(ContactsEvents.Position_eventCaption, event.caption);
                    eventData.put(ContactsEvents.Position_eventType, event.type);
                    eventData.put(ContactsEvents.Position_eventSubType, event.subType);
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
                    List<String> similarEventIds;
                    if (isUnrecognizedEvent) {
                        similarEventIds = eventsData.getSimilarLocalEventIds(eventDataAsString,
                                EnumSet.of(
                                        ContactsEvents.getSimilarFields.PERSON_FULL_NAME,
                                        ContactsEvents.getSimilarFields.ORGANIZATION
                                ));
                    } else {
                        similarEventIds = eventsData.getSimilarLocalEventIds(eventDataAsString,
                                EnumSet.of(
                                        ContactsEvents.getSimilarFields.PERSON_FULL_NAME,
                                        ContactsEvents.getSimilarFields.ORGANIZATION,
                                        ContactsEvents.getSimilarFields.EVENT_TYPE,
                                        ContactsEvents.getSimilarFields.EVENT_SUBTYPE
                                ));
                    }

                    if (similarEventIds != null) {
                        statEventsDoubles++;
                    } else {
                        if (isUnrecognizedEvent) {
                            statEventsUnRecognized++;
                        }
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

    void doImport() {
        try {

            if (adapter == null || adapter.getSelectedPositions().isEmpty()) return;
            List<Integer> selectedPositions = adapter.getSelectedPositions();
            int selectedEventTypeIndex = spinnerEventTypes.getSelectedItemPosition();
            if (hasUnrecognizedEvents && selectedEventTypeIndex == 0) {
                boolean unrecognizedSelected = false;
                for (Integer pos: selectedPositions) {
                    if (pos < dataForImport.size()) {
                        TreeMap<Integer, String> eventData = eventsData.getEventData(dataForImport.get(pos));
                        if (ContactsEvents.getNotNullString(eventData.get(ContactsEvents.Position_eventType))
                                .equals(ContactsEvents.getEventType(Constants.Type_Unrecognized))) {
                            unrecognizedSelected = true;
                            break;
                        }
                    }
                }
               if (unrecognizedSelected) {
                   Toast.makeText(this, "Выберите тип для нераспознанных событий!", Toast.LENGTH_LONG).show();
                   return;
               }
            }

            int countImported = 0;
            for (Integer pos: selectedPositions) {
                if (pos < dataForImport.size()) {
                    TreeMap<Integer, String> eventData = eventsData.getEventData(dataForImport.get(pos));
                    if (ContactsEvents.getNotNullString(eventData.get(ContactsEvents.Position_eventType))
                            .equals(ContactsEvents.getEventType(Constants.Type_Unrecognized))) {

                        eventData.put(ContactsEvents.Position_eventType, String.valueOf(eventTypesIds.get(selectedEventTypeIndex)));
                        eventData.put(ContactsEvents.Position_eventSubType, String.valueOf(eventSubTypesIds.get(selectedEventTypeIndex)));
                    } else {
                        //Обратное преобразование типа события в хранимый id
                        String typeStr = eventData.get(ContactsEvents.Position_eventType);
                        if (typeStr != null) {
                            eventData.put(ContactsEvents.Position_eventType, String.valueOf(ContactsEvents.getEventTypeInt(typeStr)));
                        }
                        String subtypeStr = eventData.get(ContactsEvents.Position_eventSubType);
                        if (subtypeStr != null) {
                            eventData.put(ContactsEvents.Position_eventSubType, String.valueOf(ContactsEvents.getEventTypeInt(subtypeStr)));
                        }
                    }
                    eventData.put(ContactsEvents.Position_eventID, ContactsEvents.getHash(String.valueOf(System.currentTimeMillis())));
                    eventData.put(ContactsEvents.Position_eventCaption, Constants.STRING_EMPTY);
                    eventData.put(ContactsEvents.Position_eventIcon, Constants.STRING_EMPTY);
                    eventData.put(ContactsEvents.Position_eventEmoji, Constants.STRING_EMPTY);
                    eventsData.fillEmptyEventData(eventData);
                    eventsData.saveLocalEvent(eventData);
                    eventsData.needUpdateEventList = true;
                    countImported++;
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
            builder.setTitle(getString(R.string.msg_title_success));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setMessage(getString(R.string.pref_Tools_Events_Import_result, countImported));
            builder.setPositiveButton(R.string.button_ok, (dialog, which) -> {
                dialog.dismiss();
                setResult(RESULT_OK);
                finish();
            });
            AlertDialog alertToShow = builder.create();
            alertToShow.setOnShowListener(arg0 -> {
                TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                ta.recycle();
            });
            alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertToShow.show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
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

    public interface OnSelectionChangedListener {
        void onSelectionChanged(boolean allSelected, boolean noneSelected);
    }

    private void saveRecyclerViewScrollPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            View firstVisibleView = layoutManager.findViewByPosition(layoutManager.findFirstVisibleItemPosition());
            firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            topOffset = (firstVisibleView != null) ? firstVisibleView.getTop() : 0;
        }
    }

    private void restoreRecyclerViewScrollPosition() {
        if (firstVisiblePosition != -1 && recyclerView.getAdapter() != null) {
            recyclerView.post(() -> {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(firstVisiblePosition, topOffset);
                }
            });
        }
    }

    static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

        private final List<EventItem> eventList;
        private final boolean[] selectedItems;
        private int selectedCount;
        private OnSelectionChangedListener selectionListener;

        public EventListAdapter(List<EventItem> eventList) {
            this.eventList = eventList;
            this.selectedItems = new boolean[eventList.size()];
            this.selectedCount = 0;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
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
                if (pos == RecyclerView.NO_POSITION) return;

                boolean newSelected = !selectedItems[pos];
                selectedItems[pos] = newSelected;

                if (newSelected) {
                    selectedCount++;
                } else {
                    selectedCount--;
                }

                holder.checkbox.setChecked(newSelected);

                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(
                            selectedCount == eventList.size(),
                            selectedCount == 0
                    );
                }
            });
        }

        public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
            this.selectionListener = listener;
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
            if (selectedCount > 0) {
                Arrays.fill(selectedItems, false);
                selectedCount = 0;
                notifyItemRangeChanged(0, eventList.size());
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(false, true);
                }
            }
        }

        public void selectAll() {
            boolean wasNotAll = selectedCount != eventList.size();
            if (wasNotAll) {
                Arrays.fill(selectedItems, true);
                selectedCount = eventList.size();
                notifyItemRangeChanged(0, eventList.size());
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(true, false);
                }
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

    private void addClickEffect(@NonNull View view)
    {
        Drawable drawableNormal = view.getBackground();

        if (view.getBackground().getConstantState() != null) {
            Drawable drawablePressed = view.getBackground().getConstantState().newDrawable();
            drawablePressed.mutate();
            drawablePressed.setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);

            StateListDrawable listDrawable = new StateListDrawable();
            listDrawable.addState(new int[]{android.R.attr.state_pressed}, drawablePressed);
            listDrawable.addState(new int[]{}, drawableNormal);
            view.setBackground(listDrawable);
        }
    }
}
