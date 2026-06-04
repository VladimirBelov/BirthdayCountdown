/*
 * *
 *  * Created by Vladimir Belov on 04.06.2026, 11:47
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 04.06.2026, 10:15
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.content.res.TypedArray;
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
import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.DeviceTools;
import org.vovka.birthdaycountdown.utils.StringUtils;
import org.vovka.birthdaycountdown.utils.UiTools;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    CheckBox checkUseYear;
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

        super.onCreate(savedInstanceState);
        try {

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

            checkUseYear = findViewById(R.id.checkUseYear);
            viewEventType = findViewById(R.id.captionType);
            spinnerEventTypes = findViewById(R.id.spinnerEventType);
            buttonSelectAll = findViewById(R.id.button1);
            buttonSelectNone = findViewById(R.id.button2);
            buttonCancel = findViewById(R.id.button3);
            buttonImport = findViewById(R.id.button4);

            if (!dataForImport.isEmpty()) {
                try {
                    recyclerView = findViewById(R.id.listEvents);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));

                    DividerItemDecoration divider = new DividerItemDecoration(
                            recyclerView.getContext(),
                            DividerItemDecoration.VERTICAL
                    );
                    recyclerView.addItemDecoration(divider);

                    findViewById(R.id.dividerOptions).setVisibility(View.VISIBLE);
                    checkUseYear.setVisibility(View.VISIBLE);

                    List<EventItem> events = new ArrayList<>();
                    for (String eventStr : dataForImport) {
                        TreeMap<Integer, String> eventData = eventsData.getEventData(eventStr);

                        String details = StringUtils.getNotNullString(eventData.get(ContactsEvents.Position_eventCaption))
                                .concat(Constants.STRING_COLON_SPACE)
                                .concat(StringUtils.getNotNullString(eventData.get(ContactsEvents.Position_eventDateFirstTime)));
                        events.add(new EventItem(
                                eventData.get(ContactsEvents.Position_eventIcon),
                                eventData.get(ContactsEvents.Position_personFullName), //todo: вывод Position_personFullNameAlt
                                details)
                        );
                        if (!hasUnrecognizedEvents && StringUtils.getNotNullString(eventData.get(ContactsEvents.Position_eventType))
                                .equals(Constants.EventType_Unrecognized)) {
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

                    buttonSelectAll.setText(R.string.pref_Tools_Events_Import_Button_SelectAll);
                    UiTools.addClickEffect(buttonSelectAll);
                    buttonSelectAll.setVisibility(View.VISIBLE);
                    buttonSelectAll.setOnClickListener(v -> {
                        saveRecyclerViewScrollPosition();
                        updateSelectionWithoutAnimation(true);
                        restoreRecyclerViewScrollPosition();
                    });

                    buttonSelectNone.setText(R.string.pref_Tools_Events_Import_Button_SelectNone);
                    UiTools.addClickEffect(buttonSelectNone);
                    buttonSelectNone.setVisibility(View.VISIBLE);
                    buttonSelectNone.setOnClickListener(v -> {
                        saveRecyclerViewScrollPosition();
                        updateSelectionWithoutAnimation(false);
                        restoreRecyclerViewScrollPosition();
                    });

                    buttonImport.setText(R.string.pref_Tools_Events_Import_Button_Import);
                    UiTools.addClickEffect(buttonImport);
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

            buttonCancel.setText(R.string.pref_Tools_Events_Import_Button_Cancel);
            UiTools.addClickEffect(buttonCancel);
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
        final List<String> eventsList = new ArrayList<>();
        final List<String> details = new ArrayList<>();
        final AtomicInteger statEventsSkipped = new AtomicInteger(); //Не поддерживается или с ошибкой
        final AtomicInteger statEventsDoubles = new AtomicInteger(); //Дубль с существующим событием
        final AtomicInteger statEventsUnRecognized = new AtomicInteger(); //Тип не распознан или без типа

        try {

            details.add(getString(R.string.pref_Tools_Events_Import_result_Filename, DeviceTools.getPath(this, uri)));
            String fileContent = Constants.STRING_EMPTY;

            if (uri != null) fileContent = eventsData.readFileToString(uri.toString(), Constants.STRING_EOL);

            if (fileContent.isEmpty()) {
                details.add(getString(R.string.pref_Tools_Events_Import_result_noAccess));
                return eventsList;
            }

            String[] fileLines =  fileContent.split(Constants.STRING_EOL, -1);

            if (fileContent.startsWith(Constants.iCal_CalendarBegin)) {

                getMultilineEvents(fileLines, eventsList, statEventsSkipped, statEventsDoubles, statEventsUnRecognized);

            } else {

                Calendar today = AppDateUtils.getWithoutTime(new GregorianCalendar());
                for (String eventString : fileLines) {
                    getSingleLineEvent(eventString, eventsList, today, statEventsSkipped, statEventsDoubles, statEventsUnRecognized);
                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            details.add(getString(R.string.pref_Tools_Events_Import_result_Error, e.getMessage()));
        } finally {
            if (!eventsList.isEmpty()) {
                details.add(getString(R.string.pref_Tools_Events_Import_result_EventsFound, eventsList.size()));
            }
            if (statEventsUnRecognized.get() > 0) {
                details.add(getString(R.string.pref_Tools_Events_Import_result_EventsUnrecognized, statEventsUnRecognized.get()));
            }
            if (statEventsSkipped.get() > 0) {
                details.add(getString(R.string.pref_Tools_Events_Import_result_EventsSkipped, statEventsSkipped.get()));
            }
            if (statEventsDoubles.get() > 0) {
                details.add(getString(R.string.pref_Tools_Events_Import_result_EventsDoubles, statEventsDoubles.get()));
            }
            if (details.size() <= 1) {
                details.add(getString(R.string.pref_Tools_Events_Import_result_noEvents));
            }
            eventsList.add(0, String.join(Constants.STRING_EOL, details));
        }
        return eventsList;
    }

    /** Добавляет в список события icalendar
     * @param fileLines Строки с данными событий
     * @param eventsList Список событий
     * @param statEventsSkipped Счётчик пропущенных строк
     * @param statEventsDoubles Счётчик дублирований с существующими локальными событиями
     * @param statEventsUnRecognized Счётчик событий с нераспознанным или неуказанным типом
     */
    private void getMultilineEvents(String[] fileLines, List<String> eventsList, AtomicInteger statEventsSkipped, AtomicInteger statEventsDoubles, AtomicInteger statEventsUnRecognized) {
        try {

            TreeMap<Integer, String> eventData = new TreeMap<>();
            @Nullable ContactsEvents.Event event = null;
            @Nullable Date eventDateFirstTime = null;
            @Nullable String eventTitle = null;
            String eventDescription = Constants.STRING_EMPTY;
            String eventURL = Constants.STRING_EMPTY;
            boolean useEventYear = true;
            StringBuilder eventLines = new StringBuilder();
            String emptyEventYear = null;

            for (String line: fileLines) {

                if (emptyEventYear == null && line.startsWith(Constants.iCal_PROD_ID_VK)) {

                    //Если год рождения скрыт, VkFriendExporter ставит год = 2000
                    emptyEventYear = "2000";

                } else if (line.startsWith(Constants.iCal_EventBegin)) {

                    event = eventsData.createTypedEvent(Constants.Type_Unrecognized, Constants.STRING_EMPTY);
                    useEventYear = true;

                    eventData.clear();
                    eventDateFirstTime = null;
                    eventTitle = null;
                    eventDescription = Constants.STRING_EMPTY;
                    eventURL = Constants.STRING_EMPTY;
                    eventLines.setLength(0);

                } else if (line.startsWith(Constants.iCal_Summary)) {

                    eventTitle = StringUtils.substringAfter(line, Constants.iCal_Summary);
                    eventLines.append(line).append(Constants.STRING_EOL);

                } else if (line.startsWith(Constants.iCal_Description)) {

                    eventDescription = StringUtils.substringAfter(line, Constants.iCal_Description);
                    eventLines.append(line).append(Constants.STRING_EOL);

                } else if (line.startsWith(Constants.STRING_SPACE)) {

                    eventDescription = eventDescription.concat(StringUtils.substringAfter(line, Constants.STRING_SPACE));

                } else if (line.startsWith(Constants.iCal_Url)) {

                    eventURL = StringUtils.substringAfter(line, Constants.iCal_Url);
                    eventLines.append(line).append(Constants.STRING_EOL);

                } else if (line.startsWith(Constants.iCal_Date)) {

                    String storedDate = StringUtils.substringAfter(line, Constants.STRING_COLON);
                    try {
                        eventDateFirstTime = Objects.requireNonNull(ContactsEvents.sdf_YYYYMMDD_noDiv.get()).parse(storedDate);

                        if (useEventYear && emptyEventYear != null && storedDate.startsWith(emptyEventYear)) {
                            useEventYear = false;
                        }
                    } catch (ParseException ignored) { /**/ }
                    eventLines.append(line).append(Constants.STRING_EOL);

                } else if (line.startsWith(Constants.iCal_EventEnd) && event != null) {

                    if (eventDateFirstTime == null || eventTitle == null) {
                        statEventsSkipped.getAndIncrement();
                        continue;
                    }

                    eventDescription = eventDescription.replace(eventURL, Constants.STRING_EMPTY);

                    eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                    eventData.put(ContactsEvents.Position_eventDescription, eventDescription.replace(Constants.REGEX_BS, Constants.STRING_EMPTY));
                    eventData.put(ContactsEvents.Position_eventCaption, event.caption);
                    eventData.put(ContactsEvents.Position_eventURL, eventURL);
                    eventData.put(ContactsEvents.Position_eventType, event.type);
                    eventData.put(ContactsEvents.Position_eventSubType, event.subType);
                    eventData.put(ContactsEvents.Position_eventIcon, Integer.toString(event.icon));
                    eventData.put(ContactsEvents.Position_eventEmoji, event.emoji);

                    String eventDateString = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(eventDateFirstTime);
                    if (emptyEventYear != null && eventDateString.endsWith(emptyEventYear)) {
                        eventData.put(ContactsEvents.Position_eventDateFirstTime, eventDateString.substring(0, eventDateString.indexOf(emptyEventYear) - 1));
                    } else {
                        eventData.put(ContactsEvents.Position_eventDateFirstTime, eventDateString);
                    }

                    eventsData.fillEmptyEventData(eventData);
                    String eventDataAsString = eventsData.getEventData(eventData);

                    //Проверка на существующие локальные события
                    List<String> similarEventIds = eventsData.getSimilarLocalEventIds(eventDataAsString,
                                EnumSet.of(
                                        ContactsEvents.getSimilarFields.PERSON_FULL_NAME
                                ));

                    if (similarEventIds != null) {
                        statEventsDoubles.getAndIncrement();
                    } else {
                            statEventsUnRecognized.getAndIncrement();
                        eventsList.add(eventDataAsString);
                    }

                }

            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    /**
     * Добавляет в список событие, хранящееся одной строкой
     * формат: <Дата без пробелов>[,<пробел>флаги[тип события]] название праздника или ФИО [(должность)] [http:// или https:// ссылка]
     *
     * @param eventString Строка с данными события
     * @param eventsList Список событий
     * @param today Сегодня
     * @param statEventsSkipped Счётчик пропущенных строк
     * @param statEventsDoubles Счётчик дублирований с существующими локальными событиями
     * @param statEventsUnRecognized Счётчик событий с нераспознанным или неуказанным типом
     */
    private void getSingleLineEvent(String eventString, List<String> eventsList, Calendar today, AtomicInteger statEventsSkipped, AtomicInteger statEventsDoubles, AtomicInteger statEventsUnRecognized) {
        try {

            String eventLine = eventString.trim().replace("\uFEFF", Constants.STRING_EMPTY);
            if (eventLine.isEmpty() || eventLine.startsWith(Constants.STRING_HASH) || eventLine.startsWith(Constants.STRING_DSLASH))
                return;
            int indexFirstSpace = eventLine.indexOf(Constants.STRING_SPACE);
            if (indexFirstSpace == -1) return;

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
                        statEventsSkipped.getAndIncrement();
                        return;
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
                statEventsSkipped.getAndIncrement();
                return;
            }

            boolean useEventYear = true;
            int indexDateNoYear = eventDateString.indexOf(Constants.STRING_0000);
            if (indexDateNoYear != -1) useEventYear = false;

            ContactsEvents.ComputedDateForFileEvent result = eventsData.getComputedDateForFileEvent(today, indexDateNoYear, isAD, false, eventDateString, null, isEndless);
            if (result.dateEvent == null) {
                statEventsSkipped.getAndIncrement();
                return;
            }

            boolean isUnrecognizedEvent = false;
            ContactsEvents.Event event = eventsData.recognizeEventByLabel(eventLabel_forSearch, false, useEventYear);
            if (event.type.equals(Constants.EventType_Unrecognized)) {
                isUnrecognizedEvent = true;
            }

            @Nullable Date dateEvent = null;
            if (indexDateNoYear == -1) { //С годом
                try {
                    if (isAD) {
                        String dateNextFloatingEvent = eventsData.computeFloatingDate(eventDateString, 0);
                        if (!eventDateString.equals(dateNextFloatingEvent)) {
                            //Пока не поддерживается
                            statEventsSkipped.getAndIncrement();
                            return;
                        }
                        dateEvent = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).parse(eventDateString);
                    } else {
                        dateEvent = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                    }
                } catch (ParseException e1) {
                    try {
                        if (isAD) {
                            dateEvent = Objects.requireNonNull(ContactsEvents.sdf_india.get()).parse(eventDateString);
                        } else {
                            dateEvent = Objects.requireNonNull(ContactsEvents.sdf_india_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                        }
                    } catch (ParseException e2) {
                        try {
                            if (isAD) {
                                //noinspection DataFlowIssue
                                dateEvent = ContactsEvents.sdf_uk.get().parse(eventDateString);
                            } else {
                                dateEvent = Objects.requireNonNull(ContactsEvents.sdf_uk_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
                            }
                        } catch (ParseException e3) {
                            try {
                                if (isAD) {
                                    dateEvent = Objects.requireNonNull(ContactsEvents.sdf_java.get()).parse(eventDateString);
                                } else {
                                    dateEvent = Objects.requireNonNull(ContactsEvents.sdf_java_G.get()).parse(eventDateString.concat(Constants.STRING_SPACE).concat(Constants.STRING_BC));
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
                        statEventsSkipped.getAndIncrement();
                        return;
                    }
                    dateEvent = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).parse(dateNextEvent);
                } catch (ParseException e1) {
                    try {
                        dateEvent = Objects.requireNonNull(ContactsEvents.sdf_india.get()).parse(dateNextEvent);
                    } catch (ParseException e2) {
                        try {
                            //noinspection DataFlowIssue
                            dateEvent = ContactsEvents.sdf_uk.get().parse(dateNextEvent);
                        } catch (ParseException e3) {
                            try {
                                dateEvent = Objects.requireNonNull(ContactsEvents.sdf_java.get()).parse(dateNextEvent);
                            } catch (ParseException e4) {
                                //Не получилось распознать
                            }
                        }
                    }
                }
            }

            if (dateEvent == null) {
                statEventsSkipped.getAndIncrement();
                return;
            } else {
                eventDateString = Objects.requireNonNull(ContactsEvents.sdf_DDMMYYYY.get()).format(dateEvent);
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
                eventURL = StringUtils.substringBefore(eventURL, Constants.STRING_SPACE);
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
                statEventsDoubles.getAndIncrement();
            } else {
                if (isUnrecognizedEvent) {
                    statEventsUnRecognized.getAndIncrement();
                }
                eventsList.add(eventDataAsString);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void doImport() {
        try {

            if (adapter == null || adapter.getSelectedPositions().isEmpty()) return;
            List<Integer> selectedPositions = adapter.getSelectedPositions();
            int selectedEventTypeIndex = spinnerEventTypes.getSelectedItemPosition();
            if (hasUnrecognizedEvents && selectedEventTypeIndex == 0) {
                boolean isUnrecognizedSelected = false;
                for (Integer pos: selectedPositions) {
                    if (pos < dataForImport.size()) {
                        TreeMap<Integer, String> eventData = eventsData.getEventData(dataForImport.get(pos));
                        if (StringUtils.getNotNullString(eventData.get(ContactsEvents.Position_eventType))
                                .equals(Constants.EventType_Unrecognized)) {
                            isUnrecognizedSelected = true;
                            break;
                        }
                    }
                }
                if (isUnrecognizedSelected) {
                    Toast.makeText(this, getString(R.string.pref_Tools_Events_Import_msg_SelectType), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            boolean useYear = checkUseYear.isChecked();
            int countImported = 0;
            for (Integer pos: selectedPositions) {
                if (pos < dataForImport.size()) {
                    TreeMap<Integer, String> eventData = eventsData.getEventData(dataForImport.get(pos));
                    if (StringUtils.getNotNullString(eventData.get(ContactsEvents.Position_eventType))
                            .equals(Constants.EventType_Unrecognized)) {

                        Integer eventTypeInt = eventTypesIds.get(selectedEventTypeIndex);
                        eventData.put(ContactsEvents.Position_eventType, String.valueOf(eventTypeInt));
                        eventData.put(ContactsEvents.Position_eventSubType, String.valueOf(eventSubTypesIds.get(selectedEventTypeIndex)));

                        //Если выбран тип событий контакта, ещё раз пробуем распарсить Организацию, Должность и Имя
                        if (ContactsEvents.isContactEventType(eventTypeInt)) {
                            String eventTitle = eventData.get(ContactsEvents.Position_personFullName);
                            if (eventTitle != null) {
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

                                if (eventsData.preferences_rules_files_name_format == ContactsEvents.FormatName.LastnameFirst) {
                                    eventData.put(ContactsEvents.Position_personFullNameAlt, eventTitle);
                                    String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.LastnameFirst, this);
                                    eventData.put(ContactsEvents.Position_personFullName, personFullNameAlt);
                                } else {
                                    eventData.put(ContactsEvents.Position_personFullName, eventTitle);
                                    String personFullNameAlt = Person.getAltName(eventTitle, ContactsEvents.FormatName.NameFirst, this);
                                    eventData.put(ContactsEvents.Position_personFullNameAlt, personFullNameAlt);
                                }
                            }
                        }

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
                    eventData.put(ContactsEvents.Position_eventID, StringUtils.getHash(String.valueOf(System.currentTimeMillis())));
                    eventData.put(ContactsEvents.Position_eventCaption, Constants.STRING_EMPTY);
                    eventData.put(ContactsEvents.Position_eventIcon, Constants.STRING_EMPTY);
                    eventData.put(ContactsEvents.Position_eventEmoji, Constants.STRING_EMPTY);

                    //Если не надо использовать год, обрезаем дату
                    if (!useYear) {
                        String eventDateString = eventData.get(ContactsEvents.Position_eventDateFirstTime);
                        if (eventDateString != null && eventDateString.length() > 5) {
                            eventData.put(ContactsEvents.Position_eventDateFirstTime, eventDateString.substring(0, 5));
                        }
                    }

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
                try (TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme)) {
                    alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                }
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
            final CheckBox checkbox;
            final ImageView icon;
            final TextView title;
            final TextView subtitle;

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
