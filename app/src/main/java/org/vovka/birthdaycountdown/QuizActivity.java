/*
 * *
 *  * Created by Vladimir Belov on 26.02.2026, 18:28
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 26.02.2026, 18:02
 *
 */

package org.vovka.birthdaycountdown;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.vovka.birthdaycountdown.ContactsEvents.sdf_DDMMYYYY;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;
import org.vovka.birthdaycountdown.utils.UiTools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class QuizActivity extends Activity {

    private static final String TAG = "QuizActivity";
    ContactsEvents eventsData;
    private static String currentLocale;
    private QuizQuestionDispatcher dispatcher;
    private List<String> masterEventList;
    private ImageView imageQuestion;
    private TextView titleQuestion;
    private TextView eventInfo;
    private TextView answerInfo;
    private TextView buttonSources;
    private TextView buttonQuestions;
    private TextView buttonPrevQuestion;
    private TextView buttonNextQuestion;
    private TextView[] answerButtons;
    private ColorStateList defaultAnswerTextColor;
    int colorTrue;
    int colorFalse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);
            currentLocale = eventsData.currentLocale;

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.activity_quiz);

            if (eventsData.needUpdateEventList || eventsData.isEmptyEventList()) {
                eventsData.getEvents();
            }

            TextView viewCaption = findViewById(R.id.textCaption);
            if (viewCaption != null) {
                viewCaption.setText(R.string.pref_Quiz_title);
            }

            //Закрыть окно
            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

            imageQuestion = findViewById(R.id.imageQuestion);
            titleQuestion = findViewById(R.id.titleQuestion);
            eventInfo = findViewById(R.id.eventInfo);
            defaultAnswerTextColor = eventInfo.getTextColors();
            answerInfo = findViewById(R.id.answerInfo);
            initAnswerButtons();
            buttonNextQuestion = findViewById(R.id.buttonNextQuestion);
            colorTrue = ContextCompat.getColor(this, R.color.dark_green);
            colorFalse = ContextCompat.getColor(this, R.color.dark_red);

            //Загрузка первого вопроса
            masterEventList = loadMasterEventList();
            dispatcher = new QuizQuestionDispatcher(this, filterDeps, eventsData);
            refreshQuizPool();
            showNextQuestion();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            finish();
        }
    }

    private void initAnswerButtons() {
        answerButtons = new TextView[] {
                findViewById(R.id.buttonFirstAnswer),
                findViewById(R.id.buttonSecondAnswer),
                findViewById(R.id.buttonThirdAnswer),
                findViewById(R.id.buttonFourthAnswer),
                findViewById(R.id.buttonFifthAnswer)
        };

        // Общий OnClickListener для всех кнопок
        View.OnClickListener answerClickListener = v -> {
            QuizAnswerData answerData = (QuizAnswerData) v.getTag();
            if (answerData != null) {
                handleAnswerSelected(answerData);
            }
        };

        // Назначаем listener всем кнопкам
        for (TextView btn : answerButtons) {
            if (btn != null) {
                btn.setOnClickListener(answerClickListener);
                UiTools.addClickEffect(btn);
            }
        }
    }

    /**
     * Контейнер для данных одного варианта ответа
     */
    private static class QuizAnswerData {
        final boolean isCorrect;
        final String displayText;  // то, что показываем на кнопке
        final String feedbackText; // пояснение для показа после выбора
        final String rawAnswer;    // исходная строка из actions (если нужна)

        QuizAnswerData(boolean isCorrect, String displayText, String feedbackText, String rawAnswer) {
            this.isCorrect = isCorrect;
            this.displayText = displayText;
            this.feedbackText = feedbackText;
            this.rawAnswer = rawAnswer;
        }
    }

    /**
     * Центральный метод обновления пула вопросов.
     * Вызывается при старте и после изменения настроек.
     */
    private void refreshQuizPool() {
        Set<String> activeTypeCodes = QuizSettings.loadActiveQuestionTypeCodes(this);
        dispatcher.refreshQuestionPool(masterEventList, activeTypeCodes);
    }

    /**
     * Загрузка мастер-списка событий и фильтрация по источникам
     */
    private List<String> loadMasterEventList() {
        List<String> events = new ArrayList<>(eventsData.eventList);
        //todo: заглушка. сделать фильтрацию по источникам событий
        return events;
    }

    /**
     * Обработчик: настройки источников изменены
     */
    private void onSourcesSettingsChanged() {
        masterEventList = loadMasterEventList(); // перезагружаем с новыми фильтрами
        refreshQuizPool();
    }

    /**
     * Обработчик: типы вопросов изменены
     */
    private void onQuestionTypesSettingsChanged() {
        refreshQuizPool(); // activeTypeCodes загрузятся из QuizSettings
    }

    /**
     * Показать следующий вопрос
     */
    private void showNextQuestion() {
        QuizQuestion question = dispatcher.getRandomQuestion();
        if (question != null) {
            renderQuestion(question);
        } else {
            showNoQuestionsMessage();
        }
    }

    private void renderQuestion(QuizQuestion q) {
        titleQuestion.setText(q.question);

        eventInfo.setText(q.eventDetails);
        eventInfo.setVisibility(VISIBLE);

        answerInfo.setVisibility(INVISIBLE);

        if (!TextUtils.isEmpty(q.event)) {
            imageQuestion.setImageBitmap(eventsData.getEventPhoto(q.event, true,
                    true, true, eventsData.preferences_list_photostyle));
            imageQuestion.setVisibility(VISIBLE);
        }

        // Парсим ответы из формата: "isCorrect" + EOT + "displayText" + EOT + "feedbackText"
        List<QuizAnswerData> answers = new ArrayList<>(q.actions.size());
        for (String action : q.actions) {
            String[] parts = action.split(Constants.STRING_EOT, -1);
            if (parts.length >= 3) {
                boolean isCorrect = Constants.STRING_1.equals(parts[0]);
                String displayText = parts[1];
                String feedbackText = parts[2];
                answers.add(new QuizAnswerData(isCorrect, displayText, feedbackText, action));
            }
        }

        // Привязываем ответы к кнопкам
        for (int i = 0; i < answerButtons.length; i++) {
            TextView btn = answerButtons[i];
            if (btn == null) continue;

            if (i < answers.size()) {
                // 👇 Есть ответ для этой кнопки — показываем
                QuizAnswerData answer = answers.get(i);
                btn.setText(answer.displayText);
                btn.setTag(answer);
                btn.setVisibility(View.VISIBLE);
                btn.setEnabled(true);
            } else {
                // 👇 Ответов меньше, чем кнопок — скрываем лишние
                btn.setVisibility(View.GONE);
                btn.setTag(null);
                btn.setEnabled(false);
            }
            btn.setTextColor(defaultAnswerTextColor);
        }

        for (TextView btn : answerButtons) {
            if (btn != null && btn.getVisibility() == View.VISIBLE) {
                // Если текст не влезает — уменьшаем шрифт
                //btn.setMaxLines(1);
                btn.setEllipsize(TextUtils.TruncateAt.END);
                // Или используем AutoSizeText (Android 8.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    btn.setAutoSizeTextTypeUniformWithConfiguration(
                            10, 16, 2, TypedValue.COMPLEX_UNIT_SP
                    );
                }
            }
        }

        // Активируем кнопки
        buttonNextQuestion.setOnClickListener(v -> showNextQuestion());
    }

    /**
     * Обработчик выбора ответа с логикой «подсказки при последнем варианте»
     */
    private void handleAnswerSelected(QuizAnswerData selectedAnswer) {
        // 1️⃣ Считаем видимые и активные кнопки ДО обработки выбора
        int visibleEnabledCount = 0;
        TextView selectedButton = null;

        for (TextView btn : answerButtons) {
            if (btn != null && btn.getVisibility() == View.VISIBLE && btn.isEnabled()) {
                visibleEnabledCount++;
                QuizAnswerData tagData = (QuizAnswerData) btn.getTag();
                if (tagData != null && tagData.rawAnswer.equals(selectedAnswer.rawAnswer)) {
                    selectedButton = btn;
                }
            }
        }

        // 2️⃣ Обрабатываем выбранную кнопку
        if (selectedButton != null) {
            selectedButton.setEnabled(false);
            QuizAnswerData answerData = (QuizAnswerData) selectedButton.getTag();
            if (answerData == null) return;

            if (answerData.isCorrect) {
                // ✅ ПРАВИЛЬНЫЙ ОТВЕТ
                selectedButton.setTextColor(colorTrue);
                answerInfo.setText(answerData.feedbackText);
                answerInfo.setTextColor(colorTrue);
                answerInfo.setVisibility(VISIBLE);
                // 🔒 Блокируем все остальные кнопки
                disableRemainingAnswerButtons();

            } else {
                // ❌ НЕПРАВИЛЬНЫЙ ОТВЕТ
                selectedButton.setTextColor(colorFalse);

                // Проверяем: осталась ли только одна не нажатая кнопка?
                int remainingCount = visibleEnabledCount - 1;
                if (remainingCount == 1) {
                    // 👉 Показываем фидбек из НАЖАТОЙ кнопки + подсвечиваем оставшуюся
                    answerInfo.setText(answerData.feedbackText);
                    answerInfo.setTextColor(colorFalse);
                    answerInfo.setVisibility(VISIBLE);
                    highlightLastRemainingButton();
                }
            }
        }
    }

    /**
     * Блокирует все видимые и активные кнопки ответов
     */
    private void disableRemainingAnswerButtons() {
        for (TextView btn : answerButtons) {
            if (btn != null && btn.getVisibility() == View.VISIBLE && btn.isEnabled()) {
                btn.setEnabled(false);
            }
        }
    }

    /**
     * Находит последнюю оставшуюся не нажатую кнопку и делает её неактивной + подсвечивает зелёным
     * (правильный ответ уже известен из feedbackText нажатой кнопки)
     */
    private void highlightLastRemainingButton() {
        for (TextView btn : answerButtons) {
            if (btn != null && btn.getVisibility() == View.VISIBLE && btn.isEnabled()) {
                // Это последняя доступная кнопка — подсвечиваем как «правильную»
                btn.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
                btn.setEnabled(false);
                break; // нашли одну — выходим
            }
        }
    }

    private void showNoQuestionsMessage() {
        titleQuestion.setText(getString(R.string.quiz_msg_error_get_question));
        eventInfo.setVisibility(GONE);
        imageQuestion.setVisibility(GONE);
    }

    static class QuizSettings {
        private static final String PREF_ACTIVE_QUESTION_TYPES = "pref_active_question_types";

        /**
         * Загрузка активных типов вопросов.
         */
        public static Set<String> loadActiveQuestionTypeCodes(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> codes = prefs.getStringSet(PREF_ACTIVE_QUESTION_TYPES, null);

            Set<String> result;
            if (codes == null || codes.isEmpty()) {
                result = new HashSet<>();
                for (QuestionType type : QuestionType.values()) {
                    result.add(type.getCode());
                }
            } else {
                result = new HashSet<>(codes);
            }
            return result;
        }

        /**
         * Сохранение выбранных типов вопросов.
         */
        public static void saveActiveQuestionTypes(Context context, Set<QuestionType> selectedTypes) {
            Set<String> codes = new HashSet<>(selectedTypes.size());
            for (QuestionType type : selectedTypes) {
                codes.add(type.getCode());
            }
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putStringSet(PREF_ACTIVE_QUESTION_TYPES, codes)
                    .apply();
        }

        protected static int getConfiguredAnswerCount() {
            return 5; // todo: читать из SharedPreferences
        }
    }

    enum QuestionType {
        BIRTHDAY_MONTH("birthday_month"),
        BIRTHDAY_YEAR("birthday_year"),
        CONTACT_AGE("contact_age");

        private final String code;

        QuestionType(String code) {
            this.code = code;
        }

        public String getCode() { return code; }

        public static QuestionType fromCode(String code) {
            for (QuestionType type : values()) {
                if (type.code.equals(code)) return type;
            }
            return null;
        }
    }

    enum PoolType {
        ALL_EVENTS,      // Все типы событий
        BIRTHDAYS_ONLY   // Только дни рождения
    }

    interface QuestionGenerator {
        @Nullable
        QuizQuestion generate();
        QuestionType getType();
        PoolType getRequiredPoolType();
    }

    interface PoolAwareGenerator extends QuestionGenerator {
        void onPoolRefresh(PoolType poolType, List<String> newPool);
    }

    static class QuizQuestion {
        /** Вопрос */
        final String question;
        /** Отображаемые детали события */
        final String eventDetails;
        /** Возможные варианты ответов на вопрос */
        final List<String> actions;
        /** Все данные о событии */
        String event;

        QuizQuestion(String question, String eventDetails) {
            this.question = question;
            this.eventDetails = eventDetails;
            this.actions = new ArrayList<>();
        }

        QuizQuestion(String question, String eventDetails, String action) {
            this(question, eventDetails);
            this.actions.add(action);
        }

        @NonNull
        public String toString() {
            return this.question + Constants.STRING_COMMA + this.eventDetails + Constants.STRING_COMMA + this.actions.toString();
        }
    }

    /** Зависимости, которые нужны для фильтрации */
    private final QuizQuestionDispatcher.FilterDependencies filterDeps = new QuizQuestionDispatcher.FilterDependencies() {
        @Override
        public String getEventKey(String[] eventInfo) {
            return eventsData.getEventKey(eventInfo);
        }
        @Override
        public String getEventKeyWithRawId(String[] eventInfo) {
            return eventsData.getEventKeyWithRawId(eventInfo);
        }
        @Override
        public int getHiddenEventsCount() {
            return eventsData.getHiddenEventsCount();
        }
        @Override
        public boolean checkIsHiddenEvent(String eventKey, String eventKeyWithRawId) {
            return eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId);
        }
    };

    private static class QuizQuestionDispatcher {

        private final Map<QuestionType, QuestionGenerator> generators = new HashMap<>();
        private final Set<QuestionType> activeTypes = new HashSet<>();
        private final Random generator = new Random();
        private final Context context;
        ContactsEvents eventsData;
        private List<String> filteredAllEvents = new ArrayList<>();    // Все не-скрытые события
        private List<String> filteredBirthdays = new ArrayList<>();    // Только дни рождения

        private final FilterDependencies filterDeps;

        public interface FilterDependencies {
            String getEventKey(String[] eventInfo);
            String getEventKeyWithRawId(String[] eventInfo);
            int getHiddenEventsCount();
            boolean checkIsHiddenEvent(String eventKey, String eventKeyWithRawId);
        }

        public QuizQuestionDispatcher(Context context, FilterDependencies filterDeps, ContactsEvents eventsData) {
            this.context = context.getApplicationContext();
            this.filterDeps = filterDeps;
            this.eventsData = eventsData;

            registerGenerator(new BirthdayMonthGenerator());
            registerGenerator(new BirthdayYearGenerator());
            registerGenerator(new ContactAgeGenerator());
        }

        private void registerGenerator(QuestionGenerator gen) {
            generators.put(gen.getType(), gen);
        }

        /**
         * Обновление пулов вопросов.
         * Фильтрация происходит один раз, затем каждый генератор получает нужный ему пул.
         */
        public void refreshQuestionPool(List<String> allEvents, Set<String> activeTypeCodes) {
            loadActiveTypes(activeTypeCodes);

            filteredAllEvents = filterEventsFromList(allEvents, null); // null = все подтипы
            filteredBirthdays = filterEventsFromList(allEvents, Constants.EventType_BirthDay);

            for (QuestionGenerator gen : generators.values()) {
                if (gen instanceof PoolAwareGenerator) {
                    PoolType poolType = gen.getRequiredPoolType();
                    List<String> pool = (poolType == PoolType.BIRTHDAYS_ONLY) ? filteredBirthdays : filteredAllEvents;
                    ((PoolAwareGenerator) gen).onPoolRefresh(poolType, pool);
                }
            }
        }

        /**
         * Универсальный фильтр событий.
         * @param events исходный список
         * @param requiredSubType если не null — фильтровать только по этому подтипу
         */
        private List<String> filterEventsFromList(List<String> events, @Nullable String requiredSubType) {
            List<String> result = new ArrayList<>(events.size());
            for (String event : events) {
                String[] eventInfo = event.split(Constants.STRING_EOT, -1);

                if (requiredSubType != null && !requiredSubType.equals(eventInfo[ContactsEvents.Position_eventSubType])) {
                    continue;
                }

                final String eventKey = filterDeps.getEventKey(eventInfo);
                final String eventKeyWithRawId = filterDeps.getEventKeyWithRawId(eventInfo);
                if (filterDeps.getHiddenEventsCount() == 0 ||
                        !filterDeps.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {
                    result.add(event);
                }
            }
            return result;
        }

        private void loadActiveTypes(Set<String> activeTypeCodes) {
            activeTypes.clear();
            if (activeTypeCodes == null || activeTypeCodes.isEmpty()) {
                activeTypes.addAll(Arrays.asList(QuestionType.values()));
                return;
            }
            for (String code : activeTypeCodes) {
                QuestionType type = QuestionType.fromCode(code);
                if (type != null && generators.containsKey(type)) {
                    activeTypes.add(type);
                }
            }
        }

        @Nullable
        public QuizQuestion getRandomQuestion() {
            if (activeTypes.isEmpty()) {
                return null;
            }

            // Собираем активные генераторы + проверяем, есть ли у них данные
            List<QuestionGenerator> availableGenerators = new ArrayList<>(activeTypes.size());
            for (QuestionType type : activeTypes) {
                QuestionGenerator gen = generators.get(type);
                if (gen == null) continue;

                // Проверяем, что нужный пул не пуст
                PoolType poolType = gen.getRequiredPoolType();
                List<String> pool = (poolType == PoolType.BIRTHDAYS_ONLY) ? filteredBirthdays : filteredAllEvents;
                if (!pool.isEmpty()) {
                    availableGenerators.add(gen);
                }
            }

            if (availableGenerators.isEmpty()) {
                return null;
            }

            // 🔁 Пробуем до 10 раз получить валидный вопрос
            final int MAX_ATTEMPTS = 10;
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                QuestionGenerator selected = availableGenerators.get(generator.nextInt(availableGenerators.size()));
                try {
                    QuizQuestion question = selected.generate();
                    if (question != null) {
                        return question; // ✅ Успех!
                    }
                    // Генератор вернул null (например, не смог распарсить дату) — пробуем ещё раз
                } catch (Exception e) {
                    Log.e("QuizDispatcher", "Error on attempt " + (attempt + 1), e);
                }
            }

            // ❌ Не удалось получить вопрос
            return null;
        }

        private QuizQuestion createErrorQuestion() {
            return new QuizQuestion(
                    context.getResources().getString(R.string.quiz_msg_error_title),
                    context.getResources().getString(R.string.quiz_msg_error_get_question),
                    Constants.quiz_error_button_OK
            );
        }

        // Геттеры для отладки / UI
        public List<String> getFilteredAllEvents() { return filteredAllEvents; }
        public List<String> getFilteredBirthdays() { return filteredBirthdays; }
        public Random getGenerator() { return generator; }
        public Context getContext() { return context; }

        /**
         * Базовый класс генератора
         */
        private abstract class BaseQuestionGenerator implements PoolAwareGenerator {
            protected List<String> currentPool = new ArrayList<>();
            protected PoolType currentPoolType;

            @Override
            public void onPoolRefresh(PoolType poolType, List<String> newPool) {
                this.currentPoolType = poolType;
                this.currentPool = new ArrayList<>(newPool); // копия для безопасности
            }

            protected String buildPersonInfo(String[] eventInfo) {
                StringBuilder personInfo = new StringBuilder(StringUtils.getFullName(eventInfo, eventsData.preferences_name_format));
                final boolean isOrg = StringUtils.hasContent(eventInfo[ContactsEvents.Position_organization]);
                final boolean isTitle = StringUtils.hasContent(eventInfo[ContactsEvents.Position_title]);
                if (isOrg || isTitle) {
                    personInfo.append(Constants.STRING_PARENTHESIS_OPEN);
                    if (isOrg) {
                        personInfo.append(eventInfo[ContactsEvents.Position_organization].trim());
                        if (isTitle) {
                            personInfo.append(Constants.STRING_COMMA_SPACE)
                                    .append(eventInfo[ContactsEvents.Position_title].trim());
                        }
                    } else {
                        personInfo.append(eventInfo[ContactsEvents.Position_title].trim());
                    }
                    personInfo.append(Constants.STRING_PARENTHESIS_CLOSE);
                }
                return personInfo.toString();
            }

            /** Генерирует требуемое количество возможных ответов и перемешивает их
             * @param correctValue Правильное значение
             * @param minRange Нижняя граница возможных ответов
             * @param maxRange Верхняя граница возможных ответов
             * @param answerCount Количество возвращаемых ответов
             * @param excludeValues Пропускаемые значения
             * @return Список ответов
             */
            protected List<Integer> generateUniqueAnswers(int correctValue, int minRange, int maxRange,
                                                          int answerCount, Set<Integer> excludeValues) {
                Set<Integer> answers = new HashSet<>();
                answers.add(correctValue);
                Random localRandom = QuizQuestionDispatcher.this.generator;
                while (answers.size() < answerCount) {
                    int candidate = minRange + localRandom.nextInt(maxRange - minRange + 1);
                    if (candidate != correctValue && !excludeValues.contains(candidate)) {
                        answers.add(candidate);
                    }
                }
                List<Integer> result = new ArrayList<>(answers);
                Collections.shuffle(result, localRandom);
                return result;
            }

        }

        /**
         * Вопрос: Какой месяц рождения?
         */
        private class BirthdayMonthGenerator extends BaseQuestionGenerator {
            @Override
            public QuestionType getType() { return QuestionType.BIRTHDAY_MONTH; }

            @Override
            public PoolType getRequiredPoolType() { return PoolType.BIRTHDAYS_ONLY; }

            @Override
            @Nullable
            public QuizQuestion generate() {
                if (currentPool.isEmpty()) return null;

                try {
                    String event = currentPool.get(generator.nextInt(currentPool.size()));
                    String[] eventInfo = event.split(Constants.STRING_EOT, -1);

                    Date BDay = null;
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[ContactsEvents.Position_eventDateNextTime]);
                    } catch (ParseException ignored) { /**/ }
                    if (BDay == null) return null;

                    Calendar cal = AppDateUtils.getCalendarFromDate(BDay);
                    final int correctMonth = cal.get(Calendar.MONTH);

                    SimpleDateFormat sdfMonthLong = new SimpleDateFormat("LLLL", Locale.forLanguageTag(currentLocale));
                    String correctMonthName = sdfMonthLong.format(cal.getTime()).toUpperCase();

                    String personInfo = buildPersonInfo(eventInfo);
                    QuizQuestion result = new QuizQuestion(
                            context.getResources().getString(R.string.quiz_month01_title),
                            personInfo
                    );
                    result.event = event;

                    int answerCount = QuizSettings.getConfiguredAnswerCount();
                    List<Integer> answerMonths = generateUniqueAnswers(correctMonth, 0, 11, answerCount, Collections.emptySet());

                    Calendar calAns = Calendar.getInstance();
                    for (Integer m : answerMonths) {
                        boolean isCorrect = m.equals(correctMonth);
                        StringBuilder sb = new StringBuilder(isCorrect ? Constants.STRING_1 : Constants.STRING_0)
                                .append(Constants.STRING_EOT);
                        calAns.set(Calendar.DATE, 1);
                        calAns.set(Calendar.MONTH, m);
                        String monthName = sdfMonthLong.format(calAns.getTime()).toUpperCase();
                        sb.append(monthName).append(Constants.STRING_EOT);

                        if (isCorrect) {
                            sb.append(context.getResources().getString(R.string.quiz_answer_true,
                                    correctMonthName, eventInfo[ContactsEvents.Position_eventDateFirstTime]));
                        } else {
                            sb.append(context.getResources().getString(R.string.quiz_answer_false,
                                    correctMonthName, eventInfo[ContactsEvents.Position_eventDateFirstTime]));
                        }
                        result.actions.add(sb.toString());
                    }
                    return result;

                } catch (Exception e) {
                    Log.e(TAG, "BirthdayMonthGenerator error", e);
                    return createErrorQuestion();
                }
            }
        }

        /**
         * Вопрос: Какой год рождения?
         */
        private class BirthdayYearGenerator extends BaseQuestionGenerator {
            @Override
            public QuestionType getType() { return QuestionType.BIRTHDAY_YEAR; }

            @Override
            public PoolType getRequiredPoolType() { return PoolType.BIRTHDAYS_ONLY; }

            @Override
            @Nullable
            public QuizQuestion generate() {
                if (currentPool.isEmpty()) return null;

                try {
                    String event = currentPool.get(generator.nextInt(currentPool.size()));
                    String[] eventInfo = event.split(Constants.STRING_EOT, -1);

                    Date BDay = null;
                    try {
                        BDay = sdf_DDMMYYYY.parse(eventInfo[ContactsEvents.Position_eventDateFirstTime]);
                    } catch (ParseException ignored) { /**/ }
                    if (BDay == null) return null;

                    Calendar cal = AppDateUtils.getCalendarFromDate(BDay);
                    final int correctYear = cal.get(Calendar.YEAR);
                    if (correctYear <= 0) return null;

                    String personInfo = buildPersonInfo(eventInfo);
                    QuizQuestion result = new QuizQuestion(
                            context.getResources().getString(R.string.quiz_year01_title),
                            personInfo
                    );
                    result.event = event;

                    int answerCount = QuizSettings.getConfiguredAnswerCount();
                    List<Integer> answerYears = generateUniqueAnswers(
                            correctYear, correctYear - 50, correctYear + 10, answerCount, Collections.emptySet());

                    String correctYearStr = Integer.toString(correctYear);
                    for (Integer year : answerYears) {
                        boolean isCorrect = year.equals(correctYear);
                        StringBuilder sb = new StringBuilder(isCorrect ? Constants.STRING_1 : Constants.STRING_0)
                                .append(Constants.STRING_EOT)
                                .append(year)
                                .append(Constants.STRING_EOT);

                        if (isCorrect) {
                            sb.append(context.getResources().getString(R.string.quiz_answer_true,
                                    correctYearStr, eventInfo[ContactsEvents.Position_eventDateFirstTime]));
                        } else {
                            sb.append(context.getResources().getString(R.string.quiz_answer_false,
                                    correctYearStr, eventInfo[ContactsEvents.Position_eventDateFirstTime]));
                        }
                        result.actions.add(sb.toString());
                    }
                    return result;

                } catch (Exception e) {
                    Log.e(TAG, "BirthdayYearGenerator error", e);
                    return createErrorQuestion();
                }
            }
        }

        /**
         * Вопросы: Сколько было бы сейчас лет? Сколько сейчас лет? Сколько лет исполнится?
         */
        private class ContactAgeGenerator extends BaseQuestionGenerator {
            @Override
            public QuestionType getType() { return QuestionType.CONTACT_AGE; }

            @Override
            public PoolType getRequiredPoolType() { return PoolType.BIRTHDAYS_ONLY; }

            @Override
            @Nullable
            public QuizQuestion generate() {
                if (currentPool.isEmpty()) return null;

                try {
                    String event = currentPool.get(generator.nextInt(currentPool.size()));
                    String[] eventInfo = event.split(Constants.STRING_EOT, -1);

                    Date birthDate = null;
                    Date eventDate = null;
                    try {
                        birthDate = sdf_DDMMYYYY.parse(eventInfo[ContactsEvents.Position_eventDateFirstTime]);
                        eventDate = sdf_DDMMYYYY.parse(eventInfo[ContactsEvents.Position_eventDateNextTime]);
                    } catch (ParseException ignored) { /**/ }
                    if (birthDate == null || eventDate == null) return null;

                    Date today = AppDateUtils.getWithoutTime(Calendar.getInstance()).getTime();
                    boolean isDead = eventsData.deathDatesForIds.containsKey(eventInfo[ContactsEvents.Position_contactID]);
                    boolean isBirthdayPassed = AppDateUtils.getCalendarFromDate(eventDate).get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)
                            || eventDate.equals(today);

                    int correctAge;
                    String quizTitle;

                    if (isDead) {
                        quizTitle = context.getResources().getString(R.string.quiz_age01_title_dead);
                        correctAge = AppDateUtils.countYearsDiff(birthDate, today);
                    } else if (isBirthdayPassed) {
                        quizTitle = context.getResources().getString(R.string.quiz_age01_title_past);
                        correctAge = AppDateUtils.countYearsDiff(birthDate, today);
                    } else {
                        quizTitle = context.getResources().getString(R.string.quiz_age01_title_future);
                        correctAge = AppDateUtils.countYearsDiff(birthDate, today) + 1;
                    }

                    String personInfo = buildPersonInfo(eventInfo);
                    QuizQuestion result = new QuizQuestion(quizTitle, personInfo);
                    result.event = event;

                    int answerCount = QuizSettings.getConfiguredAnswerCount();
                    int minAge = Math.max(0, correctAge - 15);
                    int maxAge = correctAge + 15;
                    List<Integer> answerAges = generateUniqueAnswers(correctAge, minAge, maxAge, answerCount, Collections.emptySet());

                    String correctAgeStr = StringUtils.getAgeString(correctAge,
                            R.string.msg_after_year_prefix_1,
                            R.string.msg_after_year_prefix_1_,
                            R.string.msg_after_year_prefix_2_3_4,
                            R.string.msg_after_year_prefix_5_20,
                            currentLocale,
                            context.getResources()).toUpperCase();

                    for (Integer age : answerAges) {
                        boolean isCorrect = age.equals(correctAge);
                        String ageStr = StringUtils.getAgeString(age,
                                R.string.msg_after_year_prefix_1,
                                R.string.msg_after_year_prefix_1_,
                                R.string.msg_after_year_prefix_2_3_4,
                                R.string.msg_after_year_prefix_5_20,
                                currentLocale,
                                context.getResources()).toUpperCase();

                        StringBuilder sb = new StringBuilder(isCorrect ? Constants.STRING_1 : Constants.STRING_0)
                                .append(Constants.STRING_EOT)
                                .append(ageStr)
                                .append(Constants.STRING_EOT);

                        if (isCorrect) {
                            sb.append(context.getResources().getString(R.string.quiz_answer_true,
                                    ageStr, eventInfo[ContactsEvents.Position_eventDateFirstTime]));
                        } else {
                            sb.append(context.getResources().getString(R.string.quiz_answer_false,
                                    correctAgeStr, eventInfo[ContactsEvents.Position_eventDateFirstTime]));
                        }
                        result.actions.add(sb.toString());
                    }
                    return result;

                } catch (Exception e) {
                    Log.e(TAG, "ContactAgeGenerator error", e);
                    return createErrorQuestion();
                }
            }
        }
    }
}
