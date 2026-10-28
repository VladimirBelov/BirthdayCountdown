/*
 * *
 *  * Created by Vladimir Belov on 28.10.2026, 17:05
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 28.10.2026, 13:06
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import org.vovka.birthdaycountdown.utils.AppDateUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventLayoutRenderer {
    private final Context context;
    private final ContactsEvents eventsData;
    private final TypedArray ta;
    private final EventDisplayConfig config;

    public static class RenderResult {
        public String datesPosition = EventDisplayConfig.AGE_TOP;
        public String photoPosition = EventDisplayConfig.PHOTO_TOP;
        public String beforeEventText;
        public int beforeEventColor;
        public int beforeEventStyle;
        public String nameText;
        public int nameColor;
        public CharSequence detailsText;
        public int detailsColor;
        public boolean addFavoritesSign;

        // Новые поля для режима макета (Layout Mode)
        public CharSequence dateTextTop;
        public CharSequence dateTextBottom;
        public boolean isDateTextTopEmpty;
        public boolean isDateTextBottomEmpty;
        public boolean isAgeIsTop;
        public boolean showEventIcon;
        public boolean hasAgeShadow; // Флаг для применения тени к нужному TextView

        // Поля для легаси-режима (чтобы ничего не сломать)
        public String dateText;
        public int dateColor;
        public String counterText;
        public int counterColor;
        public int counterStyle;
        public boolean eventIconVisible;
        public boolean photoVisible;
        public String descriptionMode = EventDisplayConfig.DESC_TRIANGLE;
        public String inlineDescription;
        public int descColor;
    }

    public EventLayoutRenderer(Context ctx, TypedArray themeAttrs) {
        this.context = ctx;
        this.eventsData = ContactsEvents.getInstance();
        this.ta = themeAttrs;
        this.config = new EventDisplayConfig(ctx);
    }


    public RenderResult render(String[] singleEventArray, Person person, String eventKey) {
        RenderResult r = new RenderResult();
        boolean useLayoutMode = eventsData.isFeatureEnabled(Constants.FEATURE_EDIT_LAYOUT);

        if (!useLayoutMode) {
            renderLegacy(singleEventArray, person, eventKey, r);
            r.photoPosition = EventDisplayConfig.PHOTO_TOP;
            r.datesPosition = EventDisplayConfig.AGE_TOP;
            return r;
        }

        renderBeforeSection(singleEventArray, r);
        renderNameSection(singleEventArray, r);
        renderDetailsSection(singleEventArray, eventKey, r);
        renderDatesSection(singleEventArray, person, r);
        renderCommonSections(r);
        return r;
    }

    private void renderBeforeSection(String[] ev, RenderResult r) {
        List<String> order = config.getBeforeOrder();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String distance = ev[ContactsEvents.Position_eventDistance];
        String[] distText = ev[ContactsEvents.Position_eventDistanceText].split("\\|", -1);
        boolean isNear = "0".equals(distance) || "1".equals(distance) || "2".equals(distance);

        int baseColor;
        int baseStyle = Typeface.NORMAL;
        if ("0".equals(distance)) {
            baseColor = eventsData.preferences_list_color_eventtoday;
            baseStyle = Typeface.BOLD;
        } else if ("1".equals(distance) || "2".equals(distance)) {
            baseColor = eventsData.preferences_list_color_eventsoon;
            baseStyle = Typeface.BOLD;
        } else {
            baseColor = ta.getColor(R.styleable.Theme_eventDistanceColor,
                    ContextCompat.getColor(context, R.color.theme_black_primary));
        }

        boolean hasWhen = order.contains(EventDisplayConfig.PARAM_WHEN);

        for (int i = 0; i < order.size(); i++) {
            String id = order.get(i);
            boolean fromNewLine = i == 0;
            String text = null;

            switch (id) {
                case EventDisplayConfig.PARAM_WHEN:
                    boolean hasDayOfWeekParam = order.contains(EventDisplayConfig.PARAM_DAY_OF_WEEK);
                    if ("0".equals(distance)) {
                        String custom = eventsData.preferences_list_custom_todayevent_caption;
                        text = custom.isEmpty() ? distText[0] : custom;
                    } else if (hasDayOfWeekParam) {
                        // Если день недели показывается отдельным параметром, не включаем его сюда
                        text = distText[0];
                    } else {
                        // Иначе включаем день недели в основной текст (как в legacy режиме)
                        text = distText[0] + (distText.length > 1 ? " " + distText[1] : "");
                    }
                    break;

                case EventDisplayConfig.PARAM_DAY_OF_WEEK:
                    // День недели берётся из distText[1] (формат: "через N дней|в понедельник|дата|день|год")
                    if (distText.length > 1 && !distText[1].isEmpty()) {
                        text = distText[1];
                    }
                    break;

                case EventDisplayConfig.PARAM_DATE_BEFORE:
                    // Если "when" показан и событие близкое — дата дублируется, скрываем
                    if (hasWhen && isNear) continue;
                    if (distText.length > 2 && !distText[2].isEmpty()) {
                        text = distText[2];
                    }
                    break;

                case EventDisplayConfig.PARAM_NEWLINE:
                    sb.append("\n");
                    continue;
            }

            if (text != null && !text.isEmpty()) {
                if (!fromNewLine) sb.append(" ");
                int start = sb.length();
                sb.append(text);
                int customColor = config.getColor(id, 0);
                if (customColor != 0) {
                    sb.setSpan(new ForegroundColorSpan(customColor), start, sb.length(),
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        r.beforeEventText = sb.toString();
        r.beforeEventColor = baseColor;
        r.beforeEventStyle = baseStyle;
    }

    private void renderNameSection(String[] ev, RenderResult r) {
        String name = StringUtils.getFullName(ev, eventsData.preferences_name_format);

        String setting = config.getSetting(EventDisplayConfig.PARAM_NAME);
        if (EventDisplayConfig.PARAM_NAME_SPLIT.equals(setting)) {
            String subType = ev[ContactsEvents.Position_eventSubType];
            boolean isContactEvent = !subType.equals(Constants.EventType_Other)
                    && !subType.equals(Constants.EventType_Holiday);
            if (isContactEvent) {
                name = splitNameToLines(name);
            }
        }

        r.nameText = name;
        r.nameColor = config.getColor(EventDisplayConfig.PARAM_NAME,
                ta.getColor(R.styleable.Theme_eventFullNameColor,
                        ContextCompat.getColor(context, R.color.theme_grey_primary)));
    }

    private void renderDetailsSection(String[] ev, String eventKey, RenderResult r) {
        List<String> order = config.getDetailsOrder();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean autoNewline = shouldAutoInsertNewlines(order);
        boolean hasAnyContent = false;
        boolean hasContentSinceLastNewline = false;

        String subType = ev[ContactsEvents.Position_eventSubType];
        String eventLabel = ev[ContactsEvents.Position_eventLabel].trim();
        String eventCaption = ev[ContactsEvents.Position_eventCaption].trim();

        for (int i = 0; i < order.size(); i++) {
            String id = order.get(i);
            if (id.startsWith(EventDisplayConfig.PARAM_NEWLINE)) {
                if (hasContentSinceLastNewline) {
                    sb.append("\n");
                    hasContentSinceLastNewline = false; // Сбрасываем после newline
                }
                continue;
            }

            if (i > 0 && needsSeparator(order, i, autoNewline) && hasAnyContent) {
                sb.append("\n");
            }
            String text = extractDetailText(id, ev, subType, eventLabel, eventCaption);
            if (text != null && !text.isEmpty()) {
                int start = sb.length();
                sb.append(text);
                int customColor = config.getColor(id, 0);
                if (customColor != 0) {
                    sb.setSpan(new ForegroundColorSpan(customColor), start, sb.length(),
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                hasAnyContent = true;
                hasContentSinceLastNewline = true;
            }
        }

        //добавляем метки только если есть контент
        if (hasAnyContent) {
            appendHiddenSilencedLabels(sb, ev, eventKey);
        }

        r.detailsText = sb;
        r.detailsColor = ta.getColor(R.styleable.Theme_eventDetailsColor,
                ContextCompat.getColor(context, R.color.light_gray_darker));
        r.descColor = config.getColor(EventDisplayConfig.PARAM_DESCRIPTION, 0);
    }

    private void renderDatesSection(String[] ev, Person person, RenderResult r) {
        List<String> order = config.getDatesOrder();
        int iconIndex = order.indexOf(EventDisplayConfig.PARAM_EVENT_ICON);
        int ageIndex = order.indexOf(EventDisplayConfig.PARAM_AGE_NEXT);
        boolean hasIcon = iconIndex != -1;

        SpannableStringBuilder topBuilder = new SpannableStringBuilder();
        SpannableStringBuilder bottomBuilder = new SpannableStringBuilder();
        r.hasAgeShadow = false;

        // Рассчитываем динамические размеры шрифтов на основе настроек пользователя
        float ageSizeSp = (eventsData.dimen_List_name / eventsData.displayMetrics_density) * (1 + eventsData.preferences_list_magnify_age * 0.1f);
        int ageSizePx = (int) (ageSizeSp * context.getResources().getDisplayMetrics().scaledDensity);
        float dateSizeSp = (eventsData.dimen_list_date / eventsData.displayMetrics_density) * (1 + eventsData.preferences_list_magnify_date * 0.1f);
        int dateSizePx = (int) (dateSizeSp * context.getResources().getDisplayMetrics().scaledDensity);

        // 1. Определяем, должен ли возраст быть в верхнем блоке
        boolean ageIsTop;
        if (hasIcon) {
            // Если есть иконка, возраст сверху, только если он стоит до иконки
            ageIsTop = (ageIndex != -1) && (ageIndex < iconIndex);
        } else {
            // Если иконки нет, возраст считается "верхним", только если он стоит самым первым в списке
            ageIsTop = (ageIndex == 0);
        }
        r.isAgeIsTop = ageIsTop;

        for (int i = 0; i < order.size(); i++) {
            String id = order.get(i);
            if (id.equals(EventDisplayConfig.PARAM_EVENT_ICON)) {
                continue; // Иконку пропускаем, она обрабатывается отдельно
            }

            String text = extractDateText(id, ev);
            if (!text.isEmpty()) {

                // 2. Логика распределения по блокам (верхний/нижний)
                SpannableStringBuilder target;
                if (hasIcon) {
                    target = (i < iconIndex) ? topBuilder : bottomBuilder;
                } else {
                    if (ageIsTop) {
                        // Возраст первый: он идет в верхний, остальные элементы - в нижний
                        target = (i == 0) ? topBuilder : bottomBuilder;
                    } else {
                        // Возраст не первый: всё до него идет в верхний, он и остальное - в нижний
                        target = (i < ageIndex) ? topBuilder : bottomBuilder;
                    }
                }

                if (target.length() > 0) {
                    target.append("\n");
                }

                int start = target.length();
                target.append(text);

                // Применяем цвет
                int customColor = config.getColor(id, 0);
                if (customColor == 0 && id.equals(EventDisplayConfig.PARAM_AGE_NEXT)) {
                    customColor = eventsData.isJubilee(person.Age, ev[ContactsEvents.Position_eventSubType])
                            ? eventsData.preferences_list_color_eventjubilee
                            : ta.getColor(R.styleable.Theme_eventAgeColor,
                            ContextCompat.getColor(context, R.color.theme_grey_primary));
                }
                if (customColor != 0) {
                    target.setSpan(new ForegroundColorSpan(customColor), start, target.length(),
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Специальная обработка для возраста (жирный шрифт через Span, тень через флаг)
                if (id.equals(EventDisplayConfig.PARAM_AGE_NEXT)) {

                    String setting = config.getSetting(EventDisplayConfig.PARAM_AGE_NEXT);
                    Set<String> opts = new HashSet<>(Arrays.asList(setting.split(",")));

                    if (opts.contains(EventDisplayConfig.PARAM_AGE_NEXT_BOLD)) {
                        target.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                start, target.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    // Принудительно задаем крупный размер шрифта для возраста
                    target.setSpan(new android.text.style.AbsoluteSizeSpan(ageSizePx, false),
                            start, target.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);

                    if (opts.contains(EventDisplayConfig.PARAM_AGE_NEXT_SHADOW)) {
                        r.hasAgeShadow = true;
                    }
                } else if (id.equals(EventDisplayConfig.PARAM_DATE_EVENT) || id.equals(EventDisplayConfig.PARAM_DATE_ORIGINAL)) {
                    target.setSpan(new android.text.style.AbsoluteSizeSpan(dateSizePx, false),
                            start, target.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        // Заполняем результаты для режима макета
        r.dateTextTop = topBuilder;
        r.dateTextBottom = bottomBuilder;
        r.isDateTextTopEmpty = topBuilder.length() == 0;
        r.isDateTextBottomEmpty = bottomBuilder.length() == 0;
        r.showEventIcon = hasIcon && !safeTrim(ev[ContactsEvents.Position_eventIcon]).isEmpty();
    }

    private void renderCommonSections(RenderResult r) {
        r.photoPosition = config.getPhotoPosition();
        r.datesPosition = config.getAgePosition();
        r.photoVisible = !EventDisplayConfig.PHOTO_HIDDEN.equals(r.photoPosition);
        r.descriptionMode = config.getDescriptionMode();
        r.addFavoritesSign = !EventDisplayConfig.FAV_HIDDEN.equals(config.getFavIconMode());
    }

    // ===== ХЕЛПЕРЫ =====

    private boolean shouldAutoInsertNewlines(List<String> order) {
        int enabledCount = 0;
        boolean hasExplicitNewline = false;
        for (String id : order) {
            if (id.startsWith(EventDisplayConfig.PARAM_NEWLINE)) {
                hasExplicitNewline = true;
            } else {
                enabledCount++;
            }
        }
        return enabledCount > 2 && !hasExplicitNewline;
    }

    private boolean needsSeparator(List<String> order, int i, boolean autoNewline) {
        String prev = order.get(i - 1);
        if (prev.startsWith(EventDisplayConfig.PARAM_NEWLINE)) return false;
        return autoNewline;
    }

    private String extractDetailText(String id, String[] ev, String subType,
                                     String eventLabel, String eventCaption) {
        switch (id) {
            case EventDisplayConfig.PARAM_JOB_TITLE:
                return safeTrim(ev[ContactsEvents.Position_title]);

            case EventDisplayConfig.PARAM_ORGANIZATION:
                return safeTrim(ev[ContactsEvents.Position_organization]);

            case EventDisplayConfig.PARAM_NICKNAME:
                return safeTrim(ev[ContactsEvents.Position_nickname]);

            case EventDisplayConfig.PARAM_EVENT_CAPTION:
                String cap = eventCaption;
                if (!cap.isEmpty() && !eventLabel.isEmpty()
                        && !cap.equalsIgnoreCase(eventLabel)) {
                    cap += " (" + eventLabel + ")";
                }
                return cap;

            case EventDisplayConfig.PARAM_EVENT_SOURCE:
                return safeTrim(ev[ContactsEvents.Position_eventSource]).replace("~~", "\n");

            case EventDisplayConfig.PARAM_EVENT_EMOJI:
                return safeTrim(ev[ContactsEvents.Position_eventEmoji]);

            case EventDisplayConfig.PARAM_EVENT_TYPE:
                // Название типа события хранится в Position_eventCaption
                return safeTrim(ev[ContactsEvents.Position_eventCaption]);

            case EventDisplayConfig.PARAM_AGE:
                String age = ev[ContactsEvents.Position_age_current];
                return (age.isEmpty() || age.contains(": 0")) ? "" : age;

            case EventDisplayConfig.PARAM_ZODIAC:
                if (!subType.equals(Constants.EventType_BirthDay)
                        && !subType.equals(Constants.EventType_5K)) return "";
                return buildZodiacString(ev);

            case EventDisplayConfig.PARAM_WEB_LINK:
                return safeTrim(ev[ContactsEvents.Position_eventURL]).replace("~~", "\n");

            case EventDisplayConfig.PARAM_DEBUG_INFO:
                return buildDebugInfo(ev);

            default:
                return "";
        }
    }

    private String extractDateText(String id, String[] ev) {
        switch (id) {
            case EventDisplayConfig.PARAM_DATE_ORIGINAL:
                return safeTrim(ev[ContactsEvents.Position_eventDateFirstTime]);
            case EventDisplayConfig.PARAM_DATE_EVENT:
                return safeTrim(ev[ContactsEvents.Position_eventDateNextTime]);
            case EventDisplayConfig.PARAM_AGE_NEXT:
                String setting = config.getSetting(EventDisplayConfig.PARAM_AGE_NEXT);
                Set<String> opts = new HashSet<>(Arrays.asList(setting.split(",")));
                if (opts.contains(EventDisplayConfig.PARAM_AGE_NEXT_FULL)) {
                    return safeTrim(ev[ContactsEvents.Position_age_caption]);
                } else {
                    return safeTrim(ev[ContactsEvents.Position_age]);
                }
            default:
                return "";
        }
    }

    private String buildZodiacString(String[] ev) {
        String setting = config.getSetting(EventDisplayConfig.PARAM_ZODIAC);
        Set<String> parts = new java.util.HashSet<>(java.util.Arrays.asList(setting.split(",")));
        StringBuilder sb = new StringBuilder();
        if (parts.contains(EventDisplayConfig.PARAM_ZODIAC_SIGN)) {
            sb.append(safeTrim(ev[ContactsEvents.Position_zodiacSign]));
        }
        if (parts.contains(EventDisplayConfig.PARAM_ZODIAC_ANIMAL)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(safeTrim(ev[ContactsEvents.Position_zodiacYear]));
        }
        return sb.toString();
    }

    private String buildDebugInfo(String[] ev) {
        String[] dates = ev[ContactsEvents.Position_dates].split("~~", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dates.length; i++) {
            if (i > 0) sb.append("\n");
            int ind = dates[i].lastIndexOf(": ");
            sb.append(ind > -1 ? dates[i].substring(0, ind).trim() : dates[i].trim());
        }
        return sb.toString();
    }

    private void appendHiddenSilencedLabels(SpannableStringBuilder sb, String[] ev, String eventKey) {
        String keyWithRaw = eventsData.getEventKeyWithRawId(ev);
        if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Hidden
                && eventsData.getHiddenEventsCount() > 0
                && eventsData.checkIsHiddenEvent(eventKey, keyWithRaw)) {
            if (sb.length() > 0) sb.append("\n");
            int start = sb.length();
            sb.append(context.getString(R.string.msg_label_hidden));
            int customColor = ContextCompat.getColor(context, R.color.dark_red);
            if (customColor != 0) {
                sb.setSpan(new ForegroundColorSpan(customColor), start, sb.length(),
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Silenced
                && eventsData.getSilencedEventsCount() > 0
                && eventsData.checkIsSilencedEvent(eventKey, keyWithRaw)) {
            if (sb.length() > 0) sb.append("\n");
            int start = sb.length();
            sb.append(context.getString(R.string.msg_label_silenced));
            int customColor = ContextCompat.getColor(context, R.color.brown);
            if (customColor != 0) {
                sb.setSpan(new ForegroundColorSpan(customColor), start, sb.length(),
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // ===== LEGACY РЕЖИМ =====

    private void renderLegacy(String[] ev, Person person, String eventKey, RenderResult r) {
        Set<String> info = eventsData.preferences_list_event_info;

        // Before section
        String distance = ev[ContactsEvents.Position_eventDistance];
        String[] distText = ev[ContactsEvents.Position_eventDistanceText].split("\\|", -1);
        if ("0".equals(distance)) { //Сегодня
            String custom = eventsData.preferences_list_custom_todayevent_caption;
            r.beforeEventText = custom.isEmpty() ? distText[0] : custom;
            r.beforeEventColor = eventsData.preferences_list_color_eventtoday;
            r.beforeEventStyle = Typeface.BOLD;
        } else if ("1".equals(distance) || "2".equals(distance)) { //Скоро
            r.beforeEventText = distText[0] + (distText.length > 1 ? " " + distText[1] : "");
            r.beforeEventColor = eventsData.preferences_list_color_eventsoon;
            r.beforeEventStyle = Typeface.BOLD;
        } else {
            r.beforeEventText = distText[0] + (distText.length > 1 ? " " + distText[1] : "");
            r.beforeEventColor = ta.getColor(R.styleable.Theme_eventDistanceColor,
                    ContextCompat.getColor(context, R.color.theme_black_primary));
            r.beforeEventStyle = Typeface.NORMAL;
        }

        // Name
        r.nameText = StringUtils.getFullName(ev, eventsData.preferences_name_format);
        r.nameColor = ta.getColor(R.styleable.Theme_eventFullNameColor,
                ContextCompat.getColor(context, R.color.theme_grey_primary));

        // Details
        StringBuilder sb = new StringBuilder();
        String eventSubType = ev[ContactsEvents.Position_eventSubType];
        String eventLabel = ev[ContactsEvents.Position_eventLabel].trim();
        String eventCaption = ev[ContactsEvents.Position_eventCaption].trim();

        if (info.contains(context.getString(R.string.pref_List_EventInfo_Organization))) {
            String org = safeTrim(ev[ContactsEvents.Position_organization]);
            if (!org.isEmpty()) sb.append(org);
        }
        if (info.contains(context.getString(R.string.pref_List_EventInfo_JobTitle))) {
            String job = safeTrim(ev[ContactsEvents.Position_title]);
            if (!job.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(job);
            }
        }
        if (sb.length() > 0) {
            sb.insert(0, "<b>").append("</b>");
        }
        if (info.contains(context.getString(R.string.pref_List_EventInfo_Nickname))) {
            String nick = safeTrim(ev[ContactsEvents.Position_nickname]);
            if (!nick.isEmpty()) {
                if (sb.length() > 0) sb.append("<br>");
                sb.append(nick);
            }
        }

        String strZodiac = "";
        if (eventSubType.equals(Constants.EventType_BirthDay) || eventSubType.equals(Constants.EventType_5K)) {
            String strZodiacInfo = info.contains(context.getString(R.string.pref_List_EventInfo_ZodiacSign)) ?
                    safeTrim(ev[ContactsEvents.Position_zodiacSign]) : "";
            String strZodiacYearInfo = info.contains(context.getString(R.string.pref_List_EventInfo_ZodiacYear)) ?
                    safeTrim(ev[ContactsEvents.Position_zodiacYear]) : "";
            strZodiac = strZodiacInfo.concat(" ").concat(strZodiacYearInfo).trim();
        }

        if (info.contains(context.getString(R.string.pref_List_EventInfo_EventCaption))) {
            if (sb.length() > 0) sb.append("<br>");
            sb.append(eventCaption);
            if (!eventLabel.isEmpty() && info.contains(context.getString(R.string.pref_List_EventInfo_StoredEventTitle))
                    && !eventCaption.equalsIgnoreCase(eventLabel)) {
                sb.append(" (").append(eventLabel).append(")");
            }
            if (!strZodiac.isEmpty()) {
                sb.append(" (").append(strZodiac).append(")");
            }
        } else if (info.contains(context.getString(R.string.pref_List_EventInfo_StoredEventTitle))) {
            if (sb.length() > 0) sb.append("<br>");
            sb.append(eventLabel.isEmpty() ? eventCaption : eventLabel);
            if (!strZodiac.isEmpty()) {
                sb.append(" (").append(strZodiac).append(")");
            }
        } else if (!strZodiac.isEmpty()) {
            if (sb.length() > 0) sb.append("<br>");
            sb.append(strZodiac);
        }

        if (info.contains(context.getString(R.string.pref_List_EventInfo_Age))) {
            String currentAge = safeTrim(ev[ContactsEvents.Position_age_current]);
            if (!currentAge.isEmpty() && !currentAge.contains(": 0")) {
                if (sb.length() > 0) sb.append("<br>");
                sb.append(currentAge);
            }
        }

        if (info.contains(context.getString(R.string.pref_List_EventInfo_URL))) {
            String eventURL = safeTrim(ev[ContactsEvents.Position_eventURL]);
            if (!eventURL.isEmpty()) {
                if (sb.length() > 0) sb.append("<br>");
                sb.append(eventURL.replace("~~", "<br>"));
            }
        }

        if (info.contains(context.getString(R.string.pref_List_EventInfo_EventSource))) {
            String eventSource = safeTrim(ev[ContactsEvents.Position_eventSource]);
            if (!eventSource.isEmpty()) {
                if (sb.length() > 0) sb.append("<br>");
                sb.append(eventSource.replace("~~", "<br>"));
            }
        }

        appendHiddenSilencedLabelsLegacy(sb, ev, eventKey);

        int htmlMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? HtmlCompat.FROM_HTML_MODE_COMPACT
                : HtmlCompat.FROM_HTML_MODE_LEGACY;

        r.detailsText = HtmlCompat.fromHtml(sb.toString(), htmlMode);
        r.detailsColor = ta.getColor(R.styleable.Theme_eventDetailsColor,
                ContextCompat.getColor(context, R.color.light_gray_darker));

        // Date
        r.dateText = formatLegacyDate(ev);
        r.dateColor = ta.getColor(R.styleable.Theme_eventDateColor,
                ContextCompat.getColor(context, R.color.light_gray_darker));

        // Counter
        r.counterText = ev[ContactsEvents.Position_age_caption];
        r.counterColor = eventsData.isJubilee(person.Age, ev[ContactsEvents.Position_eventSubType])
                ? eventsData.preferences_list_color_eventjubilee
                : ta.getColor(R.styleable.Theme_eventAgeColor,
                ContextCompat.getColor(context, R.color.theme_grey_primary));
        r.counterStyle = Typeface.BOLD;

        // Photo
        r.photoVisible = info.contains(context.getString(R.string.pref_List_EventInfo_Photo));

        // Fav
        r.addFavoritesSign = info.contains(context.getString(R.string.pref_List_EventInfo_FavoritesIcon));

        // Event Icon
        r.eventIconVisible = info.contains(context.getString(R.string.pref_List_EventInfo_EventIcon)) &&
                !ev[ContactsEvents.Position_eventIcon].isEmpty();

        // Description
        String desc = safeTrim(ev[ContactsEvents.Position_eventDescription]);
        boolean descInInfo = info.contains(context.getString(R.string.pref_List_EventInfo_Description));
        r.inlineDescription = descInInfo ? desc : null;
    }

    private void appendHiddenSilencedLabelsLegacy(StringBuilder sb, String[] ev, String eventKey) {
        String keyWithRaw = eventsData.getEventKeyWithRawId(ev);
        if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Hidden
                && eventsData.getHiddenEventsCount() > 0
                && eventsData.checkIsHiddenEvent(eventKey, keyWithRaw)) {
            if (sb.length() > 0) sb.append("<br>");
            sb.append(StringUtils.getHTMLColor(context.getString(R.string.msg_label_hidden),
                    R.color.dark_red, context));
        }
        if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Silenced
                && eventsData.getSilencedEventsCount() > 0
                && eventsData.checkIsSilencedEvent(eventKey, keyWithRaw)) {
            if (sb.length() > 0) sb.append("<br>");
            sb.append(StringUtils.getHTMLColor(context.getString(R.string.msg_label_silenced),
                    R.color.brown, context));
        }
    }

    // ===== УТИЛИТЫ =====

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private String formatLegacyDate(String[] ev) {
        String emoji = ev[ContactsEvents.Position_eventEmoji];
        String first = ev[ContactsEvents.Position_eventDateFirstTime];
        String next = ev[ContactsEvents.Position_eventDateNextTime];
        String xdays = context.getString(R.string.event_type_xdays_emoji);
        String k5 = context.getString(R.string.event_type_5k_emoji);
        if (xdays.equals(emoji) || k5.equals(emoji)) {
            return formatDate(next, true);
        } else if (first.length() < 10) {
            return formatDate(next, false);
        } else {
            return formatDate(first, true);
        }
    }

    private String formatDate(String dateStr, boolean withYear) {
        return AppDateUtils.getDateFormatted(dateStr,
                withYear ? ContactsEvents.FormatDate.WithYear : ContactsEvents.FormatDate.WithoutYear,
                eventsData.preferences_date_format, context, eventsData.getResources(),
                eventsData.currentLocale);
    }

    private String splitNameToLines(String fullName) {
        String[] parts = fullName.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("\n");
            sb.append(parts[i]);
        }
        return sb.toString();
    }

}