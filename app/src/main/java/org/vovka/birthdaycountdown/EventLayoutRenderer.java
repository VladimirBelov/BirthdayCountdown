/*
 * *
 *  * Created by Vladimir Belov on 26.10.2026, 16:14
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 26.10.2026, 16:07
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

import java.util.List;
import java.util.Set;

public class EventLayoutRenderer {
    private final Context context;
    private final ContactsEvents eventsData;
    private final TypedArray ta;
    private final EventDisplayConfig config;

    public static class RenderResult {
        public String beforeEventText = "";
        public int beforeEventColor = 0;
        public int beforeEventStyle = Typeface.NORMAL;

        public String nameText = "";
        public int nameColor = 0;

        public CharSequence detailsText = "";
        public int detailsColor = 0;

        public String dateText = "";
        public int dateColor = 0;
        public String datesPosition = EventDisplayConfig.AGE_TOP;
        public String photoPosition = EventDisplayConfig.PHOTO_TOP;

        public String counterText = "";
        public int counterColor = 0;
        public int counterStyle = Typeface.NORMAL;
        public float counterShadowDx = 0, counterShadowDy = 0;
        public int counterShadowColor = 0;

        public boolean photoVisible = true;
        public boolean eventIconVisible = false;
        public boolean descriptionButtonVisible = false;
        public boolean descriptionInlineVisible = false;
        public String inlineDescription = "";
        public String eventIconUri = "";
        public String eventIconPosition = "before_age"; // before_age, after_age, hidden
    }

    public EventLayoutRenderer(Context ctx, TypedArray themeAttrs) {
        this.context = ctx;
        this.eventsData = ContactsEvents.getInstance();
        this.ta = themeAttrs;
        this.config = new EventDisplayConfig(ctx);
    }

    public EventDisplayConfig getConfig() {
        return config;
    }

    public RenderResult render(String[] singleEventArray, Person person, String eventKey) {
        RenderResult r = new RenderResult();
        boolean useLayoutMode = eventsData.isFeatureEnabled(Constants.FEATURE_EDIT_LAYOUT);

        if (!useLayoutMode) {
            renderLegacy(singleEventArray, person, r);
            r.photoPosition = EventDisplayConfig.PHOTO_TOP;
            r.datesPosition = EventDisplayConfig.AGE_TOP;
            return r;
        }

        renderBeforeSection(singleEventArray, r);
        renderNameSection(singleEventArray, r);
        renderDetailsSection(singleEventArray, person, eventKey, r);
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
        boolean autoNewline = shouldAutoInsertNewlines(order);

        for (int i = 0; i < order.size(); i++) {
            String id = order.get(i);
            boolean fromNewLine = i == 0;
            /*} else if (needsSeparator(order, i, autoNewline)) {
                sb.append("\n");
                fromNewLine = true;*/

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
                case EventDisplayConfig.PARAM_NEWLINE + "_1":
                case EventDisplayConfig.PARAM_NEWLINE + "_2":
                case EventDisplayConfig.PARAM_NEWLINE + "_3":
                case EventDisplayConfig.PARAM_NEWLINE + "_4":
                case EventDisplayConfig.PARAM_NEWLINE + "_5":
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

    private void renderDetailsSection(String[] ev, Person person, String eventKey, RenderResult r) {
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
            String text = extractDetailText(id, ev, subType, eventLabel, eventCaption, eventKey);
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
    }

    private void renderDatesSection(String[] ev, Person person, RenderResult r) {
        List<String> order = config.getDatesOrder();
        StringBuilder sb = new StringBuilder();
        boolean hasAgeNext = false;
        boolean hasEventIcon = false;
        int ageNextIndex = -1;
        int eventIconIndex = -1;

        for (int i = 0; i < order.size(); i++) {
            String id = order.get(i);
            if (id.equals(EventDisplayConfig.PARAM_AGE_NEXT)) {
                hasAgeNext = true;
                ageNextIndex = i;
            } else if (id.equals(EventDisplayConfig.PARAM_EVENT_ICON)) {
                hasEventIcon = true;
                eventIconIndex = i;
            } else if (id.startsWith(EventDisplayConfig.PARAM_NEWLINE)) {
                sb.append("\n");
                continue;
            }

            if (i > 0 && !id.startsWith(EventDisplayConfig.PARAM_NEWLINE)) {
                String prevId = order.get(i - 1);
                if (!prevId.startsWith(EventDisplayConfig.PARAM_NEWLINE)) {
                    sb.append("\n");
                }
            }

            String text = extractDateText(id, ev);
            if (!text.isEmpty()) {
                sb.append(text);
            }
        }

        // Определяем позицию иконки
        if (!hasEventIcon) {
            r.eventIconPosition = "hidden";
        } else if (!hasAgeNext) {
            r.eventIconPosition = "before_age"; // по умолчанию
        } else if (eventIconIndex < ageNextIndex) {
            r.eventIconPosition = "before_age";
        } else {
            r.eventIconPosition = "after_age";
        }

        r.dateText = sb.toString();
        r.dateColor = config.getColor(EventDisplayConfig.PARAM_DATE_EVENT,
                ta.getColor(R.styleable.Theme_eventDateColor,
                        ContextCompat.getColor(context, R.color.light_gray_darker)));

        setupAgeNextCounter(ev, person, r);

        // Определяем, должна ли быть видна иконка события в новом режиме
        r.eventIconVisible = config.getDatesOrder().contains(EventDisplayConfig.PARAM_EVENT_ICON) &&
                !ev[ContactsEvents.Position_eventIcon].isEmpty();

    }

    private void renderCommonSections(RenderResult r) {
        r.photoPosition = config.getPhotoPosition();
        r.datesPosition = config.getAgePosition();
        r.photoVisible = !EventDisplayConfig.PHOTO_HIDDEN.equals(r.photoPosition);
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
                                     String eventLabel, String eventCaption, String eventKey) {
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
                        && !cap.equalsIgnoreCase(eventLabel)) { //&& isParamEnabledInDetails(EventDisplayConfig.PARAM_EVENT_LABEL))
                    cap += " (" + eventLabel + ")";
                }
                return cap;

            /*case EventDisplayConfig.PARAM_EVENT_LABEL:
                return eventLabel.isEmpty() ? eventCaption : eventLabel;*/

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

            case EventDisplayConfig.PARAM_DESCRIPTION:
                String desc = safeTrim(ev[ContactsEvents.Position_eventDescription]);
                String setting = config.getSetting(id);
                if (EventDisplayConfig.DESC_INLINE.equals(setting) || setting.isEmpty()) {
                    return desc;
                }
                return "";

            case EventDisplayConfig.PARAM_DEBUG_INFO:
                return buildDebugInfo(ev);

            default:
                return "";
        }
    }

    // ИСПРАВЛЕНИЕ: добавлен метод extractDateText
    private String extractDateText(String id, String[] ev) {
        switch (id) {
            case EventDisplayConfig.PARAM_DATE_ORIGINAL:
                return safeTrim(ev[ContactsEvents.Position_eventDateFirstTime]);
            case EventDisplayConfig.PARAM_DATE_EVENT:
                return safeTrim(ev[ContactsEvents.Position_eventDateNextTime]);
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

    private void setupAgeNextCounter(String[] ev, Person person, RenderResult r) {
        List<String> datesOrder = config.getDatesOrder();
        if (!datesOrder.contains(EventDisplayConfig.PARAM_AGE_NEXT)) {
            r.counterText = "";
            return;
        }

        String setting = config.getSetting(EventDisplayConfig.PARAM_AGE_NEXT);
        Set<String> opts = new java.util.HashSet<>(java.util.Arrays.asList(setting.split(",")));

        String ageStr;
        if (opts.contains(EventDisplayConfig.PARAM_AGE_NEXT_FULL)) {
            ageStr = ev[ContactsEvents.Position_age_caption]; // полный формат: "24 года"
        } else {
            ageStr = ev[ContactsEvents.Position_age]; // только число: "24"
        }

        r.counterText = ageStr;

        int defaultColor = eventsData.isJubilee(person.Age, ev[ContactsEvents.Position_eventSubType])
                ? eventsData.preferences_list_color_eventjubilee
                : ta.getColor(R.styleable.Theme_eventAgeColor,
                ContextCompat.getColor(context, R.color.theme_grey_primary));
        r.counterColor = config.getColor(EventDisplayConfig.PARAM_AGE_NEXT, defaultColor);

        if (opts.contains(EventDisplayConfig.PARAM_AGE_NEXT_BOLD)) {
            r.counterStyle = Typeface.BOLD;
        } else {
            r.counterStyle = Typeface.NORMAL;
        }
        if (opts.contains(EventDisplayConfig.PARAM_AGE_NEXT_SHADOW)) {
            r.counterShadowDx = 4f;
            r.counterShadowDy = 4f;
            r.counterShadowColor = 0xCC000000;
        }
    }

    private void appendHiddenSilencedLabels(SpannableStringBuilder sb, String[] ev, String eventKey) {
        String keyWithRaw = eventsData.getEventKeyWithRawId(ev);
        if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Hidden
                && eventsData.getHiddenEventsCount() > 0
                && eventsData.checkIsHiddenEvent(eventKey, keyWithRaw)) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(StringUtils.getHTMLColor(context.getString(R.string.msg_label_hidden),
                    R.color.dark_red, context));
        }
        if (eventsData.preferences_list_events_scope != Constants.pref_Events_Scope_Silenced
                && eventsData.getSilencedEventsCount() > 0
                && eventsData.checkIsSilencedEvent(eventKey, keyWithRaw)) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(StringUtils.getHTMLColor(context.getString(R.string.msg_label_silenced),
                    R.color.brown, context));
        }
    }

    // ===== LEGACY РЕЖИМ =====

    private void renderLegacy(String[] ev, Person person, RenderResult r) {
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

        // Details - ВСЕ поля
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

        int htmlMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? HtmlCompat.FROM_HTML_MODE_COMPACT
                : HtmlCompat.FROM_HTML_MODE_LEGACY;

        //holder.NameTextView.setText(HtmlCompat.fromHtml(result.nameText, htmlMode));
        //holder.DetailsTextView.setText(HtmlCompat.fromHtml(result.detailsText, htmlMode));

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

        // Event Icon
        r.eventIconVisible = info.contains(context.getString(R.string.pref_List_EventInfo_EventIcon)) &&
                !ev[ContactsEvents.Position_eventIcon].isEmpty();

        // Description
        String desc = safeTrim(ev[ContactsEvents.Position_eventDescription]);
        boolean descInInfo = info.contains(context.getString(R.string.pref_List_EventInfo_Description));
        if (descInInfo) {
            r.descriptionInlineVisible = !desc.isEmpty();
            r.inlineDescription = desc;
            r.descriptionButtonVisible = false;
        } else {
            r.descriptionButtonVisible = !desc.isEmpty();
            r.descriptionInlineVisible = false;
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

    private boolean isParamEnabledInDetails(String id) {
        return config.getDetailsOrder().contains(id);
    }
}