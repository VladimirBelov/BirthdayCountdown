/*
 * *
 *  * Created by Vladimir Belov on 02.09.2026, 01:33
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 02.09.2026, 01:33
 *
 */
package org.vovka.birthdaycountdown;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * База данных эмодзи, разбитых по категориям.
 */
public class EmojiData {

    public static final String CATEGORY_SMILEYS = "smileys";
    public static final String CATEGORY_PEOPLE = "people";
    public static final String CATEGORY_ANIMALS = "animals";
    public static final String CATEGORY_FOOD = "food";
    public static final String CATEGORY_ACTIVITY = "activity";
    public static final String CATEGORY_TRAVEL = "travel";
    public static final String CATEGORY_OBJECTS = "objects";
    public static final String CATEGORY_CALENDAR = "calendar";
    public static final String CATEGORY_CELEBRATION = "celebration";
    public static final String CATEGORY_SYMBOLS = "symbols";
    public static final String CATEGORY_FLAGS = "flags";

    public static final Map<String, String> CATEGORY_ICONS = new HashMap<>();
    static {
        CATEGORY_ICONS.put(CATEGORY_SMILEYS, "😀");
        CATEGORY_ICONS.put(CATEGORY_PEOPLE, "🙋");
        CATEGORY_ICONS.put(CATEGORY_ANIMALS, "🐶");
        CATEGORY_ICONS.put(CATEGORY_FOOD, "🍏");
        CATEGORY_ICONS.put(CATEGORY_ACTIVITY, "⚽");
        CATEGORY_ICONS.put(CATEGORY_TRAVEL, "✈️");
        CATEGORY_ICONS.put(CATEGORY_OBJECTS, "💡");
        CATEGORY_ICONS.put(CATEGORY_CALENDAR, "📅");
        CATEGORY_ICONS.put(CATEGORY_CELEBRATION, "🎉");
        CATEGORY_ICONS.put(CATEGORY_SYMBOLS, "❤️");
        CATEGORY_ICONS.put(CATEGORY_FLAGS, "🚩");
    }

    public static final Map<String, Integer> CATEGORY_NAMES = new HashMap<>();
    static {
        CATEGORY_NAMES.put(CATEGORY_SMILEYS, R.string.pref_Emoji_Category_smileys);
        CATEGORY_NAMES.put(CATEGORY_PEOPLE, R.string.pref_Emoji_Category_people);
        CATEGORY_NAMES.put(CATEGORY_ANIMALS, R.string.pref_Emoji_Category_animals);
        CATEGORY_NAMES.put(CATEGORY_FOOD, R.string.pref_Emoji_Category_food);
        CATEGORY_NAMES.put(CATEGORY_ACTIVITY, R.string.pref_Emoji_Category_activity);
        CATEGORY_NAMES.put(CATEGORY_TRAVEL, R.string.pref_Emoji_Category_travel);
        CATEGORY_NAMES.put(CATEGORY_OBJECTS, R.string.pref_Emoji_Category_objects);
        CATEGORY_NAMES.put(CATEGORY_CALENDAR, R.string.pref_Emoji_Category_calendar);
        CATEGORY_NAMES.put(CATEGORY_CELEBRATION, R.string.pref_Emoji_Category_celebration);
        CATEGORY_NAMES.put(CATEGORY_SYMBOLS, R.string.pref_Emoji_Category_symbols);
        CATEGORY_NAMES.put(CATEGORY_FLAGS, R.string.pref_Emoji_Category_flags);
    }

    /**
     * Возвращает локализованное название категории эмодзи.
     *
     * @param context  контекст для доступа к ресурсам
     * @param category ключ категории (одна из CATEGORY_* констант)
     * @return строка с названием категории на текущем языке устройства
     */
    public static String getCategoryName(@NonNull android.content.Context context, @NonNull String category) {
        Integer resId = CATEGORY_NAMES.get(category);
        if (resId == null) {
            return category;
        }
        return context.getString(resId);
    }

    /**
     * Проверяет, поддерживается ли эмодзи на текущем устройстве.
     * Paint.hasGlyph() некорректно работает с emoji sequences и флагами,
     * поэтому показываем все эмодзи без фильтрации.
     */
    public static boolean isEmojiSupported(@NonNull String emoji) {
        return !emoji.isEmpty();
    }

    @NonNull
    public static List<String> getEmojisForCategory(@NonNull String category) {
        List<String> allEmojis = getRawEmojisForCategory(category);
        List<String> result = new ArrayList<>(allEmojis.size());
        for (String emoji : allEmojis) {
            if (isEmojiSupported(emoji)) {
                result.add(emoji);
            }
        }
        return result;
    }

    public static int findCategoryIndexForEmoji(String emoji) {
        if (emoji == null || emoji.isEmpty()) return 0;
        List<String> allCategories = getAllCategories();
        for (int i = 0; i < allCategories.size(); i++) {
            List<String> emojis = getEmojisForCategory(allCategories.get(i));
            if (emojis.contains(emoji)) {
                return i;
            }
        }
        return 0;
    }

    public static List<String> getAllCategories() {
        return Arrays.asList(
                CATEGORY_SMILEYS,
                CATEGORY_PEOPLE,
                CATEGORY_ANIMALS,
                CATEGORY_FOOD,
                CATEGORY_ACTIVITY,
                CATEGORY_TRAVEL,
                CATEGORY_OBJECTS,
                CATEGORY_CALENDAR,
                CATEGORY_CELEBRATION,
                CATEGORY_SYMBOLS,
                CATEGORY_FLAGS
        );
    }

    private static List<String> getRawEmojisForCategory(String category) {
        switch (category) {
            case CATEGORY_SMILEYS:
                return Arrays.asList(
                        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🙂", "😉", "😊",
                        "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙", "🥲", "😋",
                        "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐",
                        "😐", "😑", "😏", "😒", "😬", "😌", "😔", "😴", "😷", "🤕",
                        "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤠", "🥳", "🥸",
                        "😎", "🤓", "😟", "🙁", "☹️", "😮", "😯", "😲", "😳", "😦",
                        "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞",
                        "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈", "👿",
                        "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👾", "🤖"
                );
            case CATEGORY_PEOPLE:
                return Arrays.asList(
                        "👋", "🤚", "🖐️", "🖖", "👌", "🤌", "✌️", "🤟", "🤙", "👈",
                        "👉", "👆", "🖕", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛",
                        "🤜", "🙌", "👐", "🤲", "🤝", "✍️", "💅", "🤳", "💪", "🦿",
                        "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦴", "👀", "👁️",
                        "👅", "👄", "👶", "🧒", "👦", "🧑", "👱", "👨", "👩", "👵",
                        "🙍", "🙎", "🙅", "🙆", "🙋", "🙇", "🤦", "👮", "🕵️", "💂",
                        "🥷", "👷", "🤴", "👳", "👲", "🧕", "🤵", "🤱", "🎅", "🤶",
                        "🦸", "🦹", "🧙", "🧚", "🧝", "🧞", "🧟", "💆", "💇", "🚶",
                        "🧍", "🧎", "🏃", "💃", "🕺", "🕴️", "👯", "🧗", "🤸", "🤺",
                        "🤾", "🧘", "🛀", "🛌", "👭", "👫", "👬", "💏", "💑", "👪"
                );
            case CATEGORY_ANIMALS:
                return Arrays.asList(
                        "🐵", "🐒", "🦍", "🐶", "🐕", "🦮", "🐕‍🦺", "🐩", "🐱", "🐈‍⬛",
                        "🦁", "🐯", "🐅", "🐆", "🐎", "🦄", "🦓", "🦬", "🐮", "🐂",
                        "🐃", "🐷", "🐖", "🐗", "🐽", "🐏", "🐑", "🐪", "🐫", "🦙",
                        "🦒", "🐘", "🦣", "🦏", "🦛", "🐁", "🐀", "🐹", "🐰", "🦫",
                        "🦔", "🦇", "🐻", "🐻‍❄️", "🐼", "🦥", "🦨", "🦘", "🦡", "🐾",
                        "🦃", "🐓", "🐣", "🐤", "🐦", "🐧", "🕊️", "🦅", "🦆", "🦢",
                        "🦉", "🦤", "🦜", "🐸", "🐊", "🐢", "🐍", "🐲", "🐉", "🦕",
                        "🦖", "🐋", "🦭", "🐟", "🐠", "🐡", "🐙", "🐌", "🦋", "🐛",
                        "🐜", "🐝", "🪲", "🦗", "🕷️", "🕸️", "🦂", "🪰", "🪱", "🦠"
                );
            case CATEGORY_FOOD:
                return Arrays.asList(
                        "🍏", "🍐", "🍊", "🍋", "🍌", "🍇", "🍓", "🫐", "🍒", "🍑",
                        "🥭", "🍍", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️",
                        "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞",
                        "🥖", "🥨", "🧀", "🍳", "🥞", "🧇", "🥓", "🥩", "🍖", "🦴",
                        "🌭", "🍔", "🍟", "🍕", "🫓", "🥪", "🧆", "🌮", "🌯", "🫔",
                        "🥘", "🥫", "🍝", "🍜", "🍲", "🍛", "🍣", "🥟", "🍤", "🍙",
                        "🍚", "🍘", "🥠", "🥮", "🍡", "🥡", "🦞", "🦐", "🦑", "🦪",
                        "🍧", "🍨", "🍩", "🍪", "🍰", "🥧", "🍫", "🍭",
                        "🍼", "🫖", "☕", "🍵", "🧃", "🧋", "🍶", "🍺", "🍷",
                        "🍸", "🧉", "🧊", "🥄", "🍴", "🥣", "🥢", "🧂"
                );
            case CATEGORY_ACTIVITY:
                return Arrays.asList(
                        "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏉", "🎱", "🪀", "🏓",
                        "🏒", "🏑", "🏏", "🪃", "🥅", "🪁", "🎣", "🤿", "🥊", "🥋",
                        "🛹", "🛷", "⛸️", "🥌", "🎿", "⛷️", "🪂", "🤼", "⛹️", "🤺",
                        "🤾", "🏌️", "🏇", "🏄", "🚣", "🏊", "🧗", "🚵", "🚴",
                        "🥈", "🎰", "🎒", "🎓", "🎚️", "🎛️", "🎞️", "🎠", "🎡", "🎢",
                        "🍹", "🩰", "🎼", "🎲", "🎭", "🎨", "🎬", "🎤", "🎧",
                        "🎹", "🥁", "🪘", "🎷", "🎺", "🎸", "🪕", "🎻", "♟️", "🎯",
                        "🎳", "🎮", "🧩"
                        );
            case CATEGORY_TRAVEL:
                return Arrays.asList(
                        "🚗", "🚕", "🚌", "🚎", "🏎️", "🚓", "🚒", "🚐", "🛻", "🚚",
                        "🚛", "🚜", "🦽", "🛴", "🚲", "🛵", "🛺", "🚨", "🚔", "🚍",
                        "🚘", "🚖", "🚠", "🚟", "🚃", "🚋", "🚞", "🚝", "🚅", "🚈",
                        "🚂", "🚆", "🚇", "🚊", "🚉", "✈️", "🛬", "🛩️", "💺", "🛰️",
                        "🛸", "🛶", "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚢", "⚓", "🚧",
                        "🚥", "🗺️", "🗿", "🗽", "🗼", "🏰", "🏟️", "⛲",
                        "⛱️", "🏖️", "🏝️", "🏜️", "🌋", "⛰️", "🏔️", "🗻", "🏕️", "⛺",
                        "🏠", "🏘️", "🏚️", "🏗️", "🏭", "🏢", "🏬", "🏣", "🏤", "🏥",
                        "🏦", "🏪", "🏫", "🏩", "💒", "🏛️", "⛪", "🕍", "🛕", "🕋",
                        "⛩️", "🛤️", "🛣️", "🗾", "🏞️", "🌅", "🌠",
                        "🌇", "🌆", "🏙️", "🌃", "🌌", "🌁"
                );
            case CATEGORY_OBJECTS:
                return Arrays.asList(
                        "💡", "⌚", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🖲️", "🕹️",
                        "💽", "💾", "💿", "📀", "📷", "📸", "📹", "🎥", "📽️", "🎞️",
                        "📞", "☎️", "📟", "📠", "📻", "🎙️", "🎚️", "🧭", "⏱️", "⏲️",
                        "⏰", "🕰️", "⌛", "⏳", "📡", "🔌", "🔦", "🕯️", "🪔", "🧯",
                        "🛢️", "💸", "💵", "💴", "💶", "💷", "🪙", "💰", "💳", "💎",
                        "⚖️", "🪜", "🧰", "🪛", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "🪚",
                        "🔩", "🪤", "🧲", "🔫", "💣", "🪓", "🗡️", "🛡️", "⚰️",
                        "⚱️", "🔮", "📿", "🧿", "💈", "🔭", "🔬", "🕳️", "🩺", "💊",
                        "💉", "🩸", "🩼", "🩻", "🧬", "🦠", "🧫", "🧪", "🌐", "💄",
                        "💋", "💌", "👗", "🥻", "👚", "👕", "🩱", "🩴", "👓", "🕶️",
                        "🥼", "👛", "👜", "👝", "🛍️", "👟", "🥾", "🥿", "👠",
                        "👡", "👑", "🎩", "🎓", "🧢", "🪖", "⛑️", "💼"
                );
            case CATEGORY_CALENDAR:
                return Arrays.asList(
                        "📆", "🗓️", "📇", "📈", "📉", "📊", "📌", "📍", "📎", "🖇️",
                        "📏", "📐", "✂️", "🗃️", "🗑️", "🔓", "🔐", "🔑", "🗝️", "📝",
                        "✏️", "✒️", "🖋️", "🖍️", "📜", "📃", "📑", "📒", "📔", "📕",
                        "📖", "📗", "📘", "📙", "📚", "📓", "📰", "🗞️", "🔖", "💰",
                        "💴", "💵", "💶", "💸", "💳", "💹", "📧", "📨", "📩", "📤",
                        "📥", "📦", "📪", "📬", "📭", "📮", "🗳️", "📁", "📂", "🗂️",
                        "🗄️", "🔒", "📅", "📋"
                );
            case CATEGORY_CELEBRATION:
                return Arrays.asList(
                        "🎉", "🎊", "🎈", "🎁", "🎀", "🎂", "🎄", "🎋", "🎎", "🎐",
                        "🎑", "🎆", "🎇", "🧨", "✨", "🎗️", "🎖️", "🏆", "🥇", "🥉",
                        "🏅", "🏵️", "🎫", "🎟️", "🎪", "🧁", "🍬", "🍮", "🍯", "🥂",
                        "🍾", "🍻", "🎃", "🎍", "🎏", "💍"
                );
            case CATEGORY_SYMBOLS:
                return Arrays.asList(
                        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "❣️",
                        "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️",
                        "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "🛐", "♈", "♉", "♊",
                        "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "🆔",
                        "⚛️", "🉑", "☣️", "📴", "📳", "🈚", "🈸", "🈺", "🈷️", "✴️",
                        "🆚", "💮", "🉐", "㊙️", "㊗️", "🈴", "🈵", "🈹", "🈲", "🅰️",
                        "🆎", "🅾️", "❌", "⭕", "🛑", "⛔", "🚫", "💯", "💢", "♨️",
                        "🚷", "🚯", "🚱", "🔞", "📵", "🚭", "❗", "❕", "❓", "❔",
                        "‼️", "⁉️", "🔅", "🔆", "〽️", "⚠️", "🚸", "🔱", "⚜️", "🔰",
                        "✅", "🈯", "💹", "❇️", "✳️", "❎", "🌐", "Ⓜ️", "💤", "🏧",
                        "🚾", "♿", "🈳", "🈂️", "🛂", "🛄", "🛅", "🚹", "🚺", "🚻",
                        "🚮", "🎦", "📶", "🈁", "🔣", "ℹ️", "🔤", "🔠", "🆖", "🆗",
                        "🆙", "🆕", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣",
                        "8️⃣", "9️⃣", "🔟", "🔢", "#️⃣", "*️⃣", "⏏️", "▶️", "⏸️", "⏯️",
                        "⏹️", "⏺️", "⏭️", "⏩", "⏫", "◀️", "🔼", "⬅️", "⬇️", "↗️",
                        "↘️", "↙️", "↖️", "↕️", "↔️", "↪️", "↩️", "⤴️", "⤵️", "🔁",
                        "🔂", "🔄", "🔃", "➕", "➖", "➗", "♾️", "💲", "💱",
                        "™️", "©️", "®️", "👁️‍🗨️", "🔚", "🔛", "🔜", "〰️", "➰", "➿",
                        "✔️", "☑️", "🔘", "🔴", "🟡", "🟢", "🔵", "⚫", "⚪", "🟤",
                        "🔺", "🔹", "🔶", "🔷", "🔳", "🔲", "▪️", "▫️", "◾", "◽",
                        "◼️", "◻️", "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "⬜", "🟫",
                        "🔈", "🔉", "🔊", "🔔", "🔕", "📢", "💬", "💭", "🗯️", "🎶"
                );
            case CATEGORY_FLAGS:
                return Arrays.asList(
                        "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️",
                        "🇷🇺", "🇺🇸", "🇩🇪", "🇫🇷", "🇮🇹", "🇵🇹", "🇦🇷", "🇲🇽",
                        "🇨🇦", "🇳🇿", "🇯🇵", "🇨🇳", "🇮🇳", "🇧🇾", "🇰🇿", "🇺🇿",
                        "🇦🇿", "🇬🇪", "🇱🇹", "🇱🇻", "🇪🇪", "🇵🇱", "🇨🇿", "🇸🇰",
                        "🇷🇴", "🇭🇷", "🇲🇰", "🇦🇱", "🇹🇷", "🇮🇱", "🇸🇦", "🇦🇪",
                        "🇰🇼", "🇧🇭", "🇴🇲", "🇱🇧", "🇮🇷", "🇧🇩", "🇳🇵", "🇲🇲",
                        "🇹🇭", "🇱🇦", "🇰🇭", "🇸🇬", "🇮🇩", "🇹🇱", "🇲🇳", "🇹🇼",
                        "🇭🇰", "🇿🇦", "🇳🇬", "🇬🇭", "🇪🇹", "🇹🇿", "🇷🇼", "🇲🇬",
                        "🇸🇨", "🇲🇻", "🇸🇩", "🇪🇷", "🇸🇴", "🇹🇳", "🇩🇿", "🇨🇩",
                        "🇨🇬", "🇨🇲", "🇧🇯", "🇨🇮", "🇱🇷", "🇬🇳", "🇬🇲", "🇬🇼",
                        "🇬🇶", "🇬🇦", "🇨🇫", "🇹🇩", "🇳🇪", "🇧🇫", "🇲🇷", "🇦🇴",
                        "🇲🇿", "🇿🇼", "🇿🇲", "🇳🇦", "🇲🇼", "🇲🇦", "🇪🇬", "🇰🇪",
                        "🇧🇮", "🇺🇬", "🇧🇼"
                );
            default:
                return new ArrayList<>();
        }
    }
}