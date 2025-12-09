/*
 * *
 *  * Created by Vladimir Belov on 09.12.2025, 03:04
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 09.12.2025, 00:54
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
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
        int statEventsSkipped = 0;
        int statEventsDoubles = 0;
        int statEventsUnRecognized = 0;

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
            if (statEventsSkipped > 0) {
                details.add("Событий пропущено: " + statEventsSkipped);
            }
            if (statEventsUnRecognized > 0) {
                details.add("Событий не распознано: " + statEventsUnRecognized);
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
}
