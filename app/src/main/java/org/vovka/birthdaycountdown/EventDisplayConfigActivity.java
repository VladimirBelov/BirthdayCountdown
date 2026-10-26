/*
 * *
 *  * Created by Vladimir Belov on 26.10.2026, 16:14
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 26.10.2026, 16:07
 *
 */

package org.vovka.birthdaycountdown;

import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventDisplayConfigActivity extends AppCompatActivity {

    private static final ContactsEvents eventsData;
    private EventDisplayConfig config;
    private RecyclerView recyclerView;
    private ConfigAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private final List<DisplayItem> items = new ArrayList<>();
    private boolean isModified = false;

    enum SECTION {
        common, before, details, dates
    }

    // Описание параметров: id → [название, секция, hasColor, hasSettings]
    private static final String[][] PARAMS = {
            // Общие (не перетаскиваются)
            {EventDisplayConfig.KEY_PHOTO, SECTION.common.toString(), "0", "0"},
            {EventDisplayConfig.KEY_AGE_POS, SECTION.common.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NAME, SECTION.common.toString(), "1", "1"},
            // До события
            {EventDisplayConfig.PARAM_WHEN, SECTION.before.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_DAY_OF_WEEK, SECTION.before.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_DATE_BEFORE, SECTION.before.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_NEWLINE, SECTION.before.toString(), "0", "0"},
            // Детали
            {EventDisplayConfig.PARAM_JOB_TITLE, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_ORGANIZATION, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_NICKNAME, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_EVENT_CAPTION, SECTION.details.toString(), "1", "0"},
            //{EventDisplayConfig.PARAM_EVENT_LABEL, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_EVENT_SOURCE, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_EVENT_EMOJI, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_EVENT_TYPE, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_FAVORITE_ICON, SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_AGE, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_ZODIAC, SECTION.details.toString(), "1", "1"},
            {EventDisplayConfig.PARAM_WEB_LINK, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_DESCRIPTION, SECTION.details.toString(), "1", "1"},
            {EventDisplayConfig.PARAM_DEBUG_INFO, SECTION.details.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_1", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_2", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_3", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_4", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_5", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_8", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_9", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_10", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_11", SECTION.details.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_12", SECTION.details.toString(), "0", "0"},
            // Даты
            {EventDisplayConfig.PARAM_DATE_ORIGINAL, SECTION.dates.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_EVENT_ICON, SECTION.dates.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_AGE_NEXT, SECTION.dates.toString(), "1", "1"},
            {EventDisplayConfig.PARAM_DATE_EVENT, SECTION.dates.toString(), "1", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_6", SECTION.dates.toString(), "0", "0"},
            {EventDisplayConfig.PARAM_NEWLINE + "_7", SECTION.dates.toString(), "0", "0"},
    };

    static {
        eventsData = ContactsEvents.getInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventsData.initLanguage(this);

        setTheme(eventsData.preferences_theme.themeMain);
        this.getTheme().applyStyle(R.style.FloatingActivity, true);
        setContentView(R.layout.activity_event_display_config);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setPopupTheme(eventsData.preferences_theme.themePopup);

        //Цвет заголовка окна
        TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
        toolbar.setTitleTextColor(ta.getColor(R.styleable.Theme_windowTitleTextColor, ContextCompat.getColor(this, R.color.white)));
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(true);

            // Устанавливаем иконку "назад" с правильным цветом
            Drawable backIcon = AppCompatResources.getDrawable(this, R.drawable.abc_ic_ab_back);
            if (backIcon != null) {
                backIcon = backIcon.mutate();
                // Берём цвет заголовка из темы — для белой темы это чёрный, для тёмных — белый
                int iconColor = ta.getColor(R.styleable.Theme_windowTitleTextColor,
                        ContextCompat.getColor(this, R.color.white));
                backIcon.setTint(iconColor);
                bar.setHomeAsUpIndicator(backIcon);
            }
            bar.setTitle(R.string.event_display_config_title);
        }
        ta.recycle();

        config = new EventDisplayConfig(this);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        rebuildItems();
        adapter = new ConfigAdapter(items);
        recyclerView.setAdapter(adapter);

        setupDragAndDrop();
    }

    /** Собирает плоский список элементов UI из конфигурации */
    private void rebuildItems() {
        items.clear();

        // --- Секция "Общие параметры" ---
        items.add(new DisplayItem(DisplayItem.TYPE_HEADER, SECTION.common.toString(), null,
                getString(R.string.section_common)));
        items.add(new DisplayItem(DisplayItem.TYPE_COMMON, SECTION.common.toString(),
                EventDisplayConfig.KEY_PHOTO, getString(R.string.pref_EventInfo_Photo)));
        items.add(new DisplayItem(DisplayItem.TYPE_COMMON, SECTION.common.toString(),
                EventDisplayConfig.KEY_AGE_POS, getString(R.string.param_dates_pos)));
        items.add(new DisplayItem(DisplayItem.TYPE_COMMON, SECTION.common.toString(),
                EventDisplayConfig.PARAM_NAME, getString(R.string.pref_EventInfo_EventTitle)));

        // --- Секция "До события" ---
        items.add(new DisplayItem(DisplayItem.TYPE_HEADER, SECTION.before.toString(), null,
                getString(R.string.section_before_event)));
        List<String> beforeOrder = config.getBeforeOrder();
        Set<String> beforeSet = new HashSet<>(beforeOrder);
        // Добавляем в порядке из конфига
        for (String id : beforeOrder) {
            if (isParamInSection(id, SECTION.before.toString())) {
                items.add(makeParameterItem(SECTION.before.toString(), id));
            }
        }
        // Добавляем недостающие (включённые по умолчанию, но не сохранённые)
        for (String[] p : PARAMS) {
            if (SECTION.before.toString().equals(p[1]) && !beforeSet.contains(p[0])) {
                items.add(makeParameterItem(SECTION.before.toString(), p[0]));
            }
        }

        // --- Секция "Детали события" ---
        items.add(new DisplayItem(DisplayItem.TYPE_HEADER, SECTION.details.toString(), null,
                getString(R.string.section_event_details)));
        List<String> detailsOrder = config.getDetailsOrder();
        Set<String> detailsSet = new HashSet<>(detailsOrder);
        for (String id : detailsOrder) {
            if (isParamInSection(id, SECTION.details.toString())) {
                items.add(makeParameterItem(SECTION.details.toString(), id));
            }
        }
        for (String[] p : PARAMS) {
            if (SECTION.details.toString().equals(p[1]) && !detailsSet.contains(p[0])) {
                items.add(makeParameterItem(SECTION.details.toString(), p[0]));
            }
        }

        // --- Секция "Даты" ---
        items.add(new DisplayItem(DisplayItem.TYPE_HEADER, SECTION.dates.toString(), null,
                getString(R.string.section_dates)));
        List<String> datesOrder = config.getDatesOrder();
        Set<String> datesSet = new HashSet<>(datesOrder);
        for (String id : datesOrder) {
            if (isParamInSection(id, SECTION.dates.toString())) {
                items.add(makeParameterItem(SECTION.dates.toString(), id));
            }
        }
        for (String[] p : PARAMS) {
            if (SECTION.dates.toString().equals(p[1]) && !datesSet.contains(p[0])) {
                items.add(makeParameterItem(SECTION.dates.toString(), p[0]));
            }
        }
    }

    private DisplayItem makeParameterItem(String section, String id) {
        String title = getParamTitle(id);
        boolean hasColor = "1".equals(getParamProp(id, 2));
        boolean hasSettings = "1".equals(getParamProp(id, 3));
        // "newline" параметры всегда включены визуально, но их "включённость" = присутствие в списке
        boolean enabled = isParamEnabled(section, id);
        return new DisplayItem(DisplayItem.TYPE_PARAMETER, section, id, title,
                enabled, hasColor, hasSettings);
    }

    private boolean isParamEnabled(String section, String id) {
        List<String> order;
        if (section.equals(SECTION.before.toString())) {
            order = config.getBeforeOrder();
        } else if (section.equals(SECTION.details.toString())) {
            order = config.getDetailsOrder();
        } else if (section.equals(SECTION.dates.toString())) {
            order = config.getDatesOrder();
        } else {
            return false;
        }
        return order.contains(id);
    }

    private String getParamTitle(String id) {
        // Маппинг ID → строковый ресурс
        switch (id) {
            case EventDisplayConfig.KEY_PHOTO: return getString(R.string.pref_EventInfo_Photo);
            case EventDisplayConfig.KEY_AGE_POS: return getString(R.string.param_dates_pos);
            case EventDisplayConfig.PARAM_WHEN: return getString(R.string.param_when);
            case EventDisplayConfig.PARAM_DAY_OF_WEEK: return getString(R.string.param_day_of_week);
            case EventDisplayConfig.PARAM_DATE_BEFORE:
            case EventDisplayConfig.PARAM_DATE_EVENT:
                return getString(R.string.pref_EventInfo_EventDate);
            case EventDisplayConfig.PARAM_NEWLINE:
            case EventDisplayConfig.PARAM_NEWLINE + "_1":
            case EventDisplayConfig.PARAM_NEWLINE + "_2":
            case EventDisplayConfig.PARAM_NEWLINE + "_3":
            case EventDisplayConfig.PARAM_NEWLINE + "_4":
            case EventDisplayConfig.PARAM_NEWLINE + "_5":
            case EventDisplayConfig.PARAM_NEWLINE + "_6":
            case EventDisplayConfig.PARAM_NEWLINE + "_7":
            case EventDisplayConfig.PARAM_NEWLINE + "_8":
            case EventDisplayConfig.PARAM_NEWLINE + "_9":
            case EventDisplayConfig.PARAM_NEWLINE + "_10":
            case EventDisplayConfig.PARAM_NEWLINE + "_11":
            case EventDisplayConfig.PARAM_NEWLINE + "_12":
                return getString(R.string.pref_EventInfo_NewLine1);
            case EventDisplayConfig.PARAM_NAME: return getString(R.string.pref_EventInfo_EventTitle);
            case EventDisplayConfig.PARAM_JOB_TITLE: return getString(R.string.pref_EventInfo_JobTitle);
            case EventDisplayConfig.PARAM_ORGANIZATION: return getString(R.string.pref_EventInfo_Organization);
            case EventDisplayConfig.PARAM_NICKNAME: return getString(R.string.pref_EventInfo_Nickname);
            case EventDisplayConfig.PARAM_EVENT_CAPTION: return getString(R.string.pref_EventInfo_EventCaption);
            //case EventDisplayConfig.PARAM_EVENT_LABEL: return getString(R.string.pref_EventInfo_EventLabel);
            case EventDisplayConfig.PARAM_EVENT_SOURCE: return getString(R.string.param_event_source);
            case EventDisplayConfig.PARAM_EVENT_EMOJI: return getString(R.string.pref_EventInfo_EventEmoji);
            case EventDisplayConfig.PARAM_EVENT_TYPE: return getString(R.string.pref_EventInfo_EventType);
            case EventDisplayConfig.PARAM_FAVORITE_ICON: return getString(R.string.pref_EventInfo_FavIcon);
            case EventDisplayConfig.PARAM_AGE: return getString(R.string.pref_EventInfo_CurrentAge);
            case EventDisplayConfig.PARAM_ZODIAC: return getString(R.string.pref_EventInfo_ZodiacSign);
            case EventDisplayConfig.PARAM_WEB_LINK: return getString(R.string.pref_EventInfo_WebLink);
            case EventDisplayConfig.PARAM_DESCRIPTION: return getString(R.string.param_description);
            case EventDisplayConfig.PARAM_DEBUG_INFO: return getString(R.string.param_debug_info);
            case EventDisplayConfig.PARAM_DATE_ORIGINAL: return getString(R.string.pref_EventInfo_EventDate_Original);
            case EventDisplayConfig.PARAM_EVENT_ICON: return getString(R.string.pref_EventInfo_EventIcon);
            case EventDisplayConfig.PARAM_AGE_NEXT: return getString(R.string.pref_EventInfo_Age);
            default: return id;
        }
    }

    private String getParamProp(String id, int index) {
        for (String[] p : PARAMS) {
            if (p[0].equals(id)) return p[index];
        }
        return "0";
    }

    private boolean isParamInSection(String id, String section) {
        for (String[] p : PARAMS) {
            if (p[0].equals(id)) return p[1].equals(section);
        }
        return false;
    }

    // ===== Drag & Drop =====
    private void setupDragAndDrop() {
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = vh.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // Защита от невалидных позиций
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }

                DisplayItem fromItem = items.get(fromPosition);
                DisplayItem toItem = items.get(toPosition);

                // Перетаскиваем только параметры внутри одной секции
                if (fromItem.type != DisplayItem.TYPE_PARAMETER) return false;
                if (toItem.type != DisplayItem.TYPE_PARAMETER) return false;
                if (!fromItem.section.equals(toItem.section)) return false;

                java.util.Collections.swap(items, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);

                return true;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                boolean isDragging = (actionState == ItemTouchHelper.ACTION_STATE_DRAG);
                recyclerView.setNestedScrollingEnabled(!isDragging);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Страховка от невалидной позиции в момент завершения drag-and-drop
                int pos = viewHolder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    saveSectionOrder(items.get(pos).section);
                }

                recyclerView.setNestedScrollingEnabled(true);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // не используется
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void saveSectionOrder(String section) {
        setModified();
        List<String> order = new ArrayList<>();
        for (DisplayItem item : items) {
            if (item.type == DisplayItem.TYPE_PARAMETER && section.equals(item.section) && item.enabled) {
                order.add(item.paramId);
            }
        }
        if (section.equals(SECTION.before.toString())) {
            config.setBeforeOrder(order);
        } else if (section.equals(SECTION.details.toString())) {
            config.setDetailsOrder(order);
        } else if (section.equals(SECTION.dates.toString())) {
            config.setDatesOrder(order);
        }
    }

    // ===== Адаптер =====
    private class ConfigAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        /** Заголовок секции */
        private static final int TYPE_HEADER = 0;
        /** Общий для всего события параметр */
        private static final int TYPE_COMMON = 1;
        /** Параметр события для секции. Может перетаскиваться для изменения порядка отображения в секции */
        private static final int TYPE_PARAMETER = 2;
        private final List<DisplayItem> data;

        ConfigAdapter(List<DisplayItem> data) { this.data = data; }

        @Override
        public int getItemViewType(int position) {
            return data.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater li = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                return new HeaderHolder(li.inflate(R.layout.item_section_header, parent, false));
            } else if (viewType == TYPE_COMMON) {
                return new CommonHolder(li.inflate(R.layout.item_common_param, parent, false));
            } else if (viewType == TYPE_PARAMETER) {
                return new ParamHolder(li.inflate(R.layout.item_parameter, parent, false));
            }
            throw new IllegalArgumentException("Unknown viewType: " + viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            DisplayItem item = data.get(position);
            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).bind(item.title);
            } else if (holder instanceof CommonHolder) {
                ((CommonHolder) holder).bind(item);
            } else if (holder instanceof ParamHolder) {
                ((ParamHolder) holder).bind(item);
            }
        }

        @Override
        public int getItemCount() { return data.size(); }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;
        HeaderHolder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.section_title);
        }
        void bind(String text) { title.setText(text); }
    }

    class CommonHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final ImageView colorIcon;
        final ImageView settingsIcon;
        final TextView valueLabel;

        CommonHolder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.common_name);
            colorIcon = v.findViewById(R.id.common_color_icon);
            settingsIcon = v.findViewById(R.id.common_settings_icon);
            valueLabel = v.findViewById(R.id.common_value);
        }

        void bind(final DisplayItem item) {
            name.setText(item.title);

            if (EventDisplayConfig.PARAM_NAME.equals(item.paramId)) {
                // Для Name: только выбор цвета + кнопка настроек, без переключателя значения
                colorIcon.setVisibility(View.VISIBLE);
                colorIcon.setOnClickListener(v -> showColorPicker(item));

                settingsIcon.setVisibility(View.VISIBLE);
                settingsIcon.setOnClickListener(v -> showSettings(item));

                valueLabel.setVisibility(View.GONE);
            } else {
                // Для Photo и Dates position: только переключатель значения, без цвета и настроек
                colorIcon.setVisibility(View.GONE);
                settingsIcon.setVisibility(View.GONE);

                String val;
                if (EventDisplayConfig.KEY_PHOTO.equals(item.paramId)) {
                    val = config.getPhotoPosition();
                } else {
                    val = config.getAgePosition();
                }
                valueLabel.setText(val);
                valueLabel.setVisibility(View.VISIBLE);
                valueLabel.setOnClickListener(v -> cycleCommonValue(item));
            }
        }
    }

    class ParamHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView name;
        final ImageView colorIcon;
        final ImageView settingsIcon;
        final ImageView dragHandle;

        ParamHolder(@NonNull View v) {
            super(v);
            checkBox = v.findViewById(R.id.checkbox);
            name = v.findViewById(R.id.parameter_name);
            colorIcon = v.findViewById(R.id.icon_color);
            settingsIcon = v.findViewById(R.id.icon_settings);
            dragHandle = v.findViewById(R.id.drag_handle);
        }

        void bind(final DisplayItem item) {
            checkBox.setChecked(item.enabled);
            name.setText(item.title);

            colorIcon.setVisibility(item.hasColor ? View.VISIBLE : View.GONE);
            settingsIcon.setVisibility(item.hasSettings ? View.VISIBLE : View.GONE);

            if (item.hasColor) {
                colorIcon.setOnClickListener(v -> showColorPicker(item));
                colorIcon.setOnLongClickListener(v -> {
                    Toast.makeText(v.getContext(), R.string.msg_hint_select_color, Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
            if (item.hasSettings) {
                settingsIcon.setOnClickListener(v -> showSettings(item));
                settingsIcon.setOnLongClickListener(v -> {
                    Toast.makeText(v.getContext(), R.string.msg_hint_additional_settings, Toast.LENGTH_SHORT).show();
                    return true;
                });
            }

            // noinspection ClickableViewAccessibility
            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (itemTouchHelper != null) {
                        // используем getAdapterPosition() вместо захваченного position,
                        // чтобы получить актуальную позицию после анимации перемещения!
                        int currentPosition = getAdapterPosition();
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(currentPosition);
                            if (holder != null) {
                                itemTouchHelper.startDrag(holder);
                            }
                        }
                    }
                }
                // Возвращаем false, чтобы сохранился визуальный эффект нажатия (ripple)
                // и событие могло быть обработано дальше, если нужно.
                return false;
            });

            // используем setOnClickListener вместо setOnCheckedChangeListener,
            // чтобы обработчик не срабатывал при программной установке состояния через setChecked()
            checkBox.setOnClickListener(v -> {
                boolean isChecked = !item.enabled;
                item.enabled = isChecked;
                checkBox.setChecked(isChecked);
                toggleParamInSection(item.section, item.paramId, isChecked);
            });
        }
    }

    private void toggleParamInSection(String section, String paramId, boolean enabled) {
        setModified();
        List<String> order;
        if (section.equals(SECTION.before.toString())) {
            order = new ArrayList<>(config.getBeforeOrder());
        } else if (section.equals(SECTION.details.toString())) {
            order = new ArrayList<>(config.getDetailsOrder());
        } else if (section.equals(SECTION.dates.toString())) {
            order = new ArrayList<>(config.getDatesOrder());
        } else {
            return;
        }
        if (enabled) {
            if (!order.contains(paramId)) order.add(paramId);
        } else {
            order.remove(paramId);
        }
        if (section.equals(SECTION.before.toString())) {
            config.setBeforeOrder(order);
        } else if (section.equals(SECTION.details.toString())) {
            config.setDetailsOrder(order);
        } else if (section.equals(SECTION.dates.toString())) {
            config.setDatesOrder(order);
        }
    }

    // ===== Диалоги =====
    private void showColorPicker(final DisplayItem item) {
        int currentColor;
        if (item.type == DisplayItem.TYPE_COMMON && !EventDisplayConfig.PARAM_NAME.equals(item.paramId)) {
            // Для общих параметров (кроме Name) цвет не хранится
            currentColor = 0;
        } else {
            currentColor = config.getColor(item.paramId, 0);
        }
        ColorPicker picker = new ColorPicker(this);
        picker.selectColor(currentColor, 0, true, item.paramId,
                (id, color) -> {
                    if (item.type == DisplayItem.TYPE_PARAMETER || EventDisplayConfig.PARAM_NAME.equals(item.paramId)) {
                        config.setColor(item.paramId, color);
                        setModified();
                        Toast.makeText(EventDisplayConfigActivity.this,
                                getString(R.string.msg_event_color_selected,
                                        String.format("#%08X", color),
                                        item.title),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSettings(DisplayItem item) {
        if (EventDisplayConfig.PARAM_NAME.equals(item.paramId)) {
            showNameSettings(item);
        } else if (EventDisplayConfig.PARAM_ZODIAC.equals(item.paramId)) {
            showZodiacSettings(item);
        } else if (EventDisplayConfig.PARAM_AGE_NEXT.equals(item.paramId)) {
            showAgeNextSettings(item);
        } else if (EventDisplayConfig.PARAM_DESCRIPTION.equals(item.paramId)) {
            showDescriptionSettings(item);
        }
    }

    private void showNameSettings(final DisplayItem item) {
        String current = config.getSetting(item.paramId);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_name_settings, null);
        final CheckBox cbSplit = view.findViewById(R.id.cb_name_split);
        cbSplit.setChecked(EventDisplayConfig.PARAM_NAME_SPLIT.equals(current));

        new AlertDialog.Builder(new ContextThemeWrapper(this,
                ContactsEvents.getInstance().preferences_theme.themeDialog))
                .setTitle(R.string.name_settings_title)
                .setView(view)
                .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                    config.setSetting(item.paramId, cbSplit.isChecked() ? EventDisplayConfig.PARAM_NAME_SPLIT : "");
                    setModified();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private void showDescriptionSettings(final DisplayItem item) {
        String current = config.getSetting(item.paramId);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_description_settings, null);
        RadioGroup rg = view.findViewById(R.id.rg_desc_mode);
        RadioButton rbInline = view.findViewById(R.id.rb_desc_inline);
        RadioButton rbTriangle = view.findViewById(R.id.rb_desc_triangle);

        if (EventDisplayConfig.DESC_TRIANGLE.equals(current)) {
            rbTriangle.setChecked(true);
        } else {
            rbInline.setChecked(true);
        }

        new AlertDialog.Builder(new ContextThemeWrapper(this,
                ContactsEvents.getInstance().preferences_theme.themeDialog))
                .setTitle(R.string.desc_settings_title)
                .setView(view)
                .setPositiveButton(R.string.button_ok, (d, w) -> {
                    String val = rbTriangle.isChecked()
                            ? EventDisplayConfig.DESC_TRIANGLE
                            : EventDisplayConfig.DESC_INLINE;
                    config.setSetting(item.paramId, val);
                    setModified();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private void showZodiacSettings(final DisplayItem item) {
        String current = config.getSetting(item.paramId);
        Set<String> currentSet = new HashSet<>(Arrays.asList(current.split(",")));

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_zodiac_settings, null);
        final CheckBox cbSign = view.findViewById(R.id.cb_zodiac_sign);
        final CheckBox cbAnimal = view.findViewById(R.id.cb_zodiac_animal);
        cbSign.setChecked(currentSet.contains(EventDisplayConfig.PARAM_ZODIAC_SIGN));
        cbAnimal.setChecked(currentSet.contains(EventDisplayConfig.PARAM_ZODIAC_ANIMAL));

        new AlertDialog.Builder(new ContextThemeWrapper(this,
                ContactsEvents.getInstance().preferences_theme.themeDialog))
                .setTitle(R.string.zodiac_settings_title)
                .setView(view)
                .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                    List<String> vals = new ArrayList<>();
                    if (cbSign.isChecked()) vals.add(EventDisplayConfig.PARAM_ZODIAC_SIGN);
                    if (cbAnimal.isChecked()) vals.add(EventDisplayConfig.PARAM_ZODIAC_ANIMAL);
                    config.setSetting(item.paramId, joinCsv(vals));
                    setModified();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private void showAgeNextSettings(final DisplayItem item) {
        String current = config.getSetting(item.paramId);
        Set<String> currentSet = new HashSet<>(Arrays.asList(current.split(",")));

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_age_next_settings, null);
        final CheckBox cbFull = view.findViewById(R.id.cb_age_next_full);
        final RadioButton rbNormal = view.findViewById(R.id.rb_age_next_normal);
        final RadioButton rbBold = view.findViewById(R.id.rb_age_next_bold);
        final RadioButton rbShadow = view.findViewById(R.id.rb_age_next_shadow);

        cbFull.setChecked(currentSet.contains(EventDisplayConfig.PARAM_AGE_NEXT_FULL));

        // Применяем стили к RadioButton для наглядности
        rbBold.setTypeface(null, android.graphics.Typeface.BOLD);
        rbShadow.setShadowLayer(4, 2, 2, Color.DKGRAY);

        // Устанавливаем выбранный стиль
        if (currentSet.contains(EventDisplayConfig.PARAM_AGE_NEXT_BOLD)) {
            rbBold.setChecked(true);
        } else if (currentSet.contains(EventDisplayConfig.PARAM_AGE_NEXT_SHADOW)) {
            rbShadow.setChecked(true);
        } else {
            rbNormal.setChecked(true);
        }

        new AlertDialog.Builder(new ContextThemeWrapper(this,
                ContactsEvents.getInstance().preferences_theme.themeDialog))
                .setTitle(R.string.age_next_settings_title)
                .setView(view)
                .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                    List<String> vals = new ArrayList<>();
                    if (cbFull.isChecked()) vals.add(EventDisplayConfig.PARAM_AGE_NEXT_FULL);

                    // Добавляем выбранный стиль
                    if (rbBold.isChecked()) {
                        vals.add(EventDisplayConfig.PARAM_AGE_NEXT_BOLD);
                    } else if (rbShadow.isChecked()) {
                        vals.add(EventDisplayConfig.PARAM_AGE_NEXT_SHADOW);
                    }
                    // Если выбран Normal - ничего не добавляем (это дефолт)

                    config.setSetting(item.paramId, joinCsv(vals));
                    setModified();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    /** Переключает значение параметра в секции "Общие"
     * @param item Параметр
     */
    private void cycleCommonValue(DisplayItem item) {
        setModified();
        if (EventDisplayConfig.KEY_PHOTO.equals(item.paramId)) {
            String cur = config.getPhotoPosition();
            String next;
            if (EventDisplayConfig.PHOTO_TOP.equals(cur)) next = EventDisplayConfig.PHOTO_CENTER;
            else if (EventDisplayConfig.PHOTO_CENTER.equals(cur)) next = EventDisplayConfig.PHOTO_HIDDEN;
            else next = EventDisplayConfig.PHOTO_TOP;
            config.setPhotoPosition(next);
        } else {
            String cur = config.getAgePosition();
            String next = EventDisplayConfig.AGE_TOP.equals(cur)
                    ? EventDisplayConfig.AGE_CENTER : EventDisplayConfig.AGE_TOP;
            config.setAgePosition(next);
        }
        // Обновляем только этот элемент
        int pos = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).paramId != null && items.get(i).paramId.equals(item.paramId)) {
                pos = i; break;
            }
        }
        if (pos >= 0) adapter.notifyItemChanged(pos);
    }

    private String joinCsv(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    // ===== Меню =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event_display_config, menu);
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            config.save();
            isModified = false;
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return true;
        } else if (id == R.id.action_reset) {
            new AlertDialog.Builder(new ContextThemeWrapper(this,
                    ContactsEvents.getInstance().preferences_theme.themeDialog))
                    .setTitle(R.string.msg_title_confirmation)
                    .setMessage(R.string.msg_reset_config_confirmation)
                    .setPositiveButton(R.string.button_yes, (d, w) -> {
                        config.resetToDefaults();
                        rebuildItems();
                        adapter.notifyDataSetChanged();
                        setModified();
                    })
                    .setNegativeButton(R.string.button_no, null)
                    .show();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ===== Модель элемента списка =====
    static class DisplayItem {
        /** Секция */
        static final int TYPE_HEADER = 0;
        /** Общий параметр */
        static final int TYPE_COMMON = 1;
        /** Параметр в секции */
        static final int TYPE_PARAMETER = 2;

        final int type;
        final String section;       // "common", "before", "details", "dates"
        final String paramId;       // null для header
        final String title;
        boolean enabled;            // для parameter
        final boolean hasColor;
        final boolean hasSettings;

        DisplayItem(int type, String section, String paramId, String title) {
            this(type, section, paramId, title, false, false, false);
        }

        DisplayItem(int type, String section, String paramId, String title,
                    boolean enabled, boolean hasColor, boolean hasSettings) {
            this.type = type;
            this.section = section;
            this.paramId = paramId;
            this.title = title;
            this.enabled = enabled;
            this.hasColor = hasColor;
            this.hasSettings = hasSettings;
        }
    }

    @Override
    public void onBackPressed() {
        if (isModified) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void setModified() {
        isModified = true;
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(new ContextThemeWrapper(this,
                ContactsEvents.getInstance().preferences_theme.themeDialog))
                .setTitle(R.string.msg_title_confirmation)
                .setIcon(android.R.drawable.ic_menu_help)
                .setMessage(R.string.local_event_dialog_confirmation_cancel)
                .setPositiveButton(R.string.button_yes, (d, w) -> finish())
                .setNegativeButton(R.string.button_no, null)
                .show();
    }
}
