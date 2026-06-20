/*
 * *
 *  * Created by Vladimir Belov on 20.06.2026, 19:57
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 20.06.2026, 11:11
 *
 */
package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ClipDescription;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import org.vovka.birthdaycountdown.utils.ImageUtils;
import org.vovka.birthdaycountdown.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 Класс WidgetMenuActivity отвечает за отображение контекстного меню
 для определенного события, на которое нажали на виджете. Он предоставляет различные действия,
 связанные с событием, такие как редактирование, совместное использование, открытие в календаре или контактах,
 добавление/удаление из избранного, скрытие и отключение уведомлений.
 Это Activity запускается, когда пользователь взаимодействует с событием, отображаемым на виджете.
 Оно динамически формирует меню на основе атрибутов события и настроек приложения.
 */
public class WidgetMenuActivity extends Activity {
    private static final String TAG = "WidgetMenuActivity";
    private int appWidgetId = 0;
    final ArrayList<Integer> menuActions = new ArrayList<>();
    String eventText = null;
    String[] singleEventArray = null;
    final Set<String> eventSources = new HashSet<>();
    ContactsEvents eventsData;
    private TypedArray ta = null;
    private ArrayList<String> menuItems;
    private List<Drawable> menuIcons;
    private ArrayList<String> recentFactsLocal;
    private IconArrayAdapter adapter;
    private boolean isFactMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || intent.getAction() == null || !intent.getAction().equals(Constants.ACTION_MENU)) {
            finish();
            return;
        }
        try {
            eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);

            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            String eventInfo = intent.getStringExtra(Constants.EXTRA_CLICKED_EVENT);
            eventText = intent.getStringExtra(Constants.EXTRA_CLICKED_TEXT);
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || eventInfo == null || eventText == null) {
                finish();
                return;
            }

            this.setTheme(eventsData.preferences_theme.themeMain);
            ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
            this.setTheme(eventsData.preferences_theme.themeDialog);
            setContentView(R.layout.widget_menu);

            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            layoutParams.width = (int) (displayMetrics.widthPixels * 0.8);
            getWindow().setAttributes(layoutParams);

            TextView titleView = findViewById(R.id.viewTitle);
            titleView.setText(HtmlCompat.fromHtml(eventText, HtmlCompat.FROM_HTML_MODE_LEGACY));

            singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);

            final AppWidgetProviderInfo appWidgetInfo = AppWidgetManager.getInstance(eventsData.getContext()).getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo != null) {
                String widgetType = appWidgetInfo.provider.getShortClassName().substring(1);
                List<String> widgetPref = eventsData.getWidgetPreference(appWidgetId, widgetType);
                if (widgetPref.size() > 10 && !widgetPref.get(10).isEmpty()) {
                    eventSources.addAll(Arrays.asList(widgetPref.get(10).split(Constants.REGEX_PLUS)));
                }
            }

            menuItems = new ArrayList<>();
            menuIcons = new ArrayList<>();
            menuActions.clear();

            if (singleEventArray.length == ContactsEvents.Position_attrAmount) {
                final String eventKey = eventsData.getEventKey(singleEventArray);
                final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
                String eventStorage = singleEventArray[ContactsEvents.Position_eventStorage];

                if (eventStorage.contains(Constants.EVENT_PREFIX_LOCAL_EVENT)) {
                    menuItems.add(getString(R.string.menu_context_edit_local_event));
                    Drawable actionDrawable = getDrawable(android.R.drawable.ic_menu_edit);
                    if (actionDrawable != null) {
                        actionDrawable.setTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
                    }
                    menuIcons.add(actionDrawable);
                    menuActions.add(Constants.ContextMenu_EditLocalEvent);
                }

                if (!TextUtils.isEmpty(singleEventArray[ContactsEvents.Position_contactID])) {
                    menuItems.add(getString(R.string.menu_context_open_contact));
                    menuIcons.add(getDrawable(R.drawable.ic_menu_friendslist));
                    menuActions.add(Constants.ContextMenu_OpenContact);

                    String phone = eventsData.getContactPhone(Long.parseLong(singleEventArray[ContactsEvents.Position_contactID]));
                    if (!phone.isEmpty()) {
                        menuItems.add(getString(R.string.menu_context_dial));
                        menuIcons.add(getDrawable(android.R.drawable.ic_menu_call));
                        menuActions.add(Constants.ContextMenu_DialContact);
                    }
                }

                if (!singleEventArray[ContactsEvents.Position_eventID].isEmpty() && eventStorage.contains(Constants.STRING_STORAGE_CALENDAR)) {
                    menuItems.add(getString(R.string.menu_context_open_calendar_event));
                    menuIcons.add(getDrawable(android.R.drawable.ic_menu_month));
                    menuActions.add(Constants.ContextMenu_OpenCalendar);
                }

                if (!singleEventArray[ContactsEvents.Position_eventURL].isEmpty()) {
                    menuItems.add(getString(R.string.menu_context_open_url));
                    menuIcons.add(getDrawable(android.R.drawable.ic_menu_directions));
                    menuActions.add(Constants.ContextMenu_OpenURL);
                }

                if (!Constants.STRING_1.equals(singleEventArray[ContactsEvents.Position_starred])) {
                    if (eventsData.checkIsFavoriteEvent(eventKey, eventKeyWithRawId, singleEventArray[ContactsEvents.Position_starred])) {
                        menuItems.add(getString(R.string.menu_context_favorites_remove));
                        menuIcons.add(getDrawable(R.drawable.ic_menu_unstar));
                        menuActions.add(Constants.ContextMenu_RemoveFromFavorites);
                    } else {
                        menuItems.add(getString(R.string.menu_context_favorites_add));
                        Drawable actionDrawable = getDrawable(R.drawable.ic_menu_star);
                        if (actionDrawable != null) {
                            actionDrawable.setTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_yellow)));
                        }
                        menuIcons.add(actionDrawable);
                        menuActions.add(Constants.ContextMenu_AddToFavorites);
                    }
                }

                if (eventsData.getHiddenEventsCount() == 0 || !eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId)) {
                    menuItems.add(getString(R.string.menu_context_hide_event));
                    Drawable actionDrawable = getDrawable(R.drawable.ic_menu_block);
                    if (actionDrawable != null) {
                        actionDrawable.setTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
                    }
                    menuIcons.add(actionDrawable);
                    menuActions.add(Constants.ContextMenu_HideEvent);
                }

                if (eventsData.getSilencedEventsCount() == 0 ||
                        (!eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId) && !eventsData.checkIsHiddenEvent(eventKey, eventKeyWithRawId))) {
                    menuItems.add(getString(R.string.menu_context_silent_event));
                    Drawable actionDrawable = getDrawable(R.drawable.ic_menu_end_conversation);
                    if (actionDrawable != null) {
                        actionDrawable.setTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_red)));
                    }
                    menuIcons.add(actionDrawable);
                    menuActions.add(Constants.ContextMenu_SilentEvent);
                } else if (eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {
                    menuItems.add(getString(R.string.menu_context_unsilent_event));
                    menuIcons.add(getDrawable(android.R.drawable.ic_menu_revert));
                    menuActions.add(Constants.ContextMenu_UnsilentEvent);
                }

                if (!TextUtils.isEmpty(singleEventArray[ContactsEvents.Position_photo_uri])) {
                    ImageView imagePhoto = findViewById(R.id.imagePhoto);
                    Bitmap photo = eventsData.getEventPhoto(eventInfo, true, true, true, 0);
                    if (photo != null) {
                        imagePhoto.setImageBitmap(photo);
                        imagePhoto.setVisibility(View.VISIBLE);
                    }
                }

                if (!TextUtils.isEmpty(singleEventArray[ContactsEvents.Position_eventDescription])) {
                    TextView descriptionView = findViewById(R.id.viewDescription);
                    descriptionView.setText(singleEventArray[ContactsEvents.Position_eventDescription]);
                    ImageView buttonShowDescription = findViewById(R.id.buttonShowDescription);
                    buttonShowDescription.setVisibility(View.VISIBLE);
                    buttonShowDescription.setClickable(true);
                    buttonShowDescription.setFocusable(true);
                    titleView.setClickable(true);
                    titleView.setFocusable(true);

                    View.OnClickListener listener = v -> {
                        if (descriptionView.getVisibility() != View.VISIBLE) {
                            descriptionView.setVisibility(View.VISIBLE);
                            buttonShowDescription.setImageDrawable(getDrawable(android.R.drawable.arrow_up_float));
                        } else {
                            descriptionView.setVisibility(View.GONE);
                            buttonShowDescription.setImageDrawable(getDrawable(android.R.drawable.arrow_down_float));
                        }
                    };
                    buttonShowDescription.setOnClickListener(listener);
                    titleView.setOnClickListener(listener);
                    descriptionView.setOnClickListener(listener);
                }

                menuItems.add(getString(R.string.menu_context_share));
                menuIcons.add(getDrawable(android.R.drawable.ic_menu_share));
                menuActions.add(Constants.ContextMenu_ShareAsText);

            } else {
                // ✅ Режим фактов
                isFactMode = true;
                recentFactsLocal = new ArrayList<>(eventsData.getRecentFacts());
                if (eventsData.eventListFacts.isEmpty()) {
                    eventsData.getFactsEvents(false);
                }

                menuItems.add(getString(R.string.menu_context_share_fact));
                menuIcons.add(getDrawable(android.R.drawable.ic_menu_share));
                menuActions.add(Constants.ContextMenu_ShareAsText);

                if (!recentFactsLocal.isEmpty()) {
                    menuItems.add(getString(R.string.menu_context_prev_fact));
                    menuIcons.add(getDrawable(R.drawable.ic_menu_back));
                    menuActions.add(Constants.ContextMenu_PrevFact);
                }

                menuItems.add(getString(R.string.menu_context_next_fact));
                menuIcons.add(getDrawable(R.drawable.ic_menu_forward));
                menuActions.add(Constants.ContextMenu_NextFact);
            }

            adapter = new IconArrayAdapter(this, menuItems, menuIcons);
            ListView menuListView = findViewById(R.id.menuListView);
            menuListView.setAdapter(adapter);
            menuListView.setOnItemClickListener((parent, view, position, id) -> onMenuItemClick(position, appWidgetId));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (ta != null) ta.recycle();
        super.onDestroy();
    }

    private void onMenuItemClick(int itemId, int appWidgetId) {
        try {
            if (singleEventArray == null) return;

            String eventKey = null;
            String eventKeyWithRawId = null;
            if (singleEventArray.length == ContactsEvents.Position_attrAmount) {
                eventKey = eventsData.getEventKey(singleEventArray);
                eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);
            }

            TextView titleView = findViewById(R.id.viewTitle);

            switch (menuActions.get(itemId)) {
                case Constants.ContextMenu_ShareAsText:
                    final String plainText = HtmlCompat.fromHtml(eventText, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                    intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    intentShare.putExtra(Intent.EXTRA_TEXT, plainText);
                    intentShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    try {
                        Intent intentChooser = Intent.createChooser(intentShare, " ");
                        intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intentChooser);
                    } catch (ActivityNotFoundException ignored) { /**/ }
                    return;

                case Constants.ContextMenu_EditLocalEvent:
                    Intent intent = new Intent(this, LocalEventActivity.class);
                    intent.setAction(Intent.ACTION_EDIT);
                    intent.putExtra(Constants.EXTRA_EVENT_DATA, singleEventArray[ContactsEvents.Position_eventID]);
                    try {
                        startActivityForResult(intent, Constants.RESULT_EDIT_EVENT);
                    } catch (ActivityNotFoundException ignored) { /**/ }
                    return;

                case Constants.ContextMenu_OpenCalendar:
                    Uri eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,
                            StringUtils.parseToLong(singleEventArray[ContactsEvents.Position_eventID]));
                    Intent openCalendarIntent = new Intent(Intent.ACTION_VIEW).setData(eventUri);
                    try {
                        if (openCalendarIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(openCalendarIntent);
                        }
                    } catch (ActivityNotFoundException ignored) { /**/ }
                    break;

                case Constants.ContextMenu_OpenContact:
                    Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, singleEventArray[ContactsEvents.Position_contactID]);
                    Intent openContactIntent = new Intent(Intent.ACTION_VIEW, contactUri);
                    try {
                        if (openContactIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(openContactIntent);
                        }
                    } catch (ActivityNotFoundException ignored) { /**/ }
                    break;

                case Constants.ContextMenu_OpenURL:
                    String[] eventURLs = singleEventArray[ContactsEvents.Position_eventURL].trim().split(Constants.STRING_2TILDA);
                    Intent openURLIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(eventURLs[0]));
                    try {
                        if (openURLIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(openURLIntent);
                        }
                    } catch (ActivityNotFoundException ignored) { /**/ }
                    break;

                case Constants.ContextMenu_RemoveFromFavorites:
                    if (eventKey != null && eventsData.unsetFavoriteEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

                case Constants.ContextMenu_AddToFavorites:
                    if (eventKey != null && eventsData.setFavoriteEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

                case Constants.ContextMenu_HideEvent:
                    if (eventKey == null && eventKeyWithRawId == null) break;

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.msg_title_confirmation));
                    builder.setIcon(android.R.drawable.ic_menu_help);
                    builder.setMessage(getString(R.string.msg_event_hide_confirmation));
                    String finalEventKey = eventKey;
                    String finalEventKeyWithRawId = eventKeyWithRawId;
                    builder.setPositiveButton(R.string.button_yes, (dialog, which) -> {
                        if (eventsData.setHiddenEvent(finalEventKey, finalEventKeyWithRawId)) {
                            if (eventsData.checkIsSilencedEvent(finalEventKey, finalEventKeyWithRawId)) {
                                eventsData.unsetSilencedEvent(finalEventKey, finalEventKeyWithRawId);
                            }
                            eventsData.updateWidgets(appWidgetId, null);
                        }
                        dialog.dismiss();
                        finish();
                    });
                    builder.setNegativeButton(R.string.button_no, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    });
                    AlertDialog alertToShow = builder.create();
                    alertToShow.setOnShowListener(arg0 -> {
                        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                    });
                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertToShow.show();
                    return;

                case Constants.ContextMenu_SilentEvent:
                    if (eventKey != null && eventsData.setSilencedEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

                case Constants.ContextMenu_UnsilentEvent:
                    if (eventKey != null && eventsData.unsetSilencedEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

                case Constants.ContextMenu_DialContact:
                    if (!singleEventArray[ContactsEvents.Position_contactID].isEmpty()) {
                        String phone = eventsData.getContactPhone(Long.parseLong(singleEventArray[ContactsEvents.Position_contactID]));
                        if (!phone.isEmpty()) {
                            Intent intentDial = new Intent(Intent.ACTION_DIAL);
                            intentDial.setData(Uri.parse(WebView.SCHEME_TEL.concat(Uri.encode(phone.trim()))));
                            intentDial.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                startActivity(intentDial);
                            } catch (android.content.ActivityNotFoundException e) { /**/ }
                        }
                    }
                    break;

                case Constants.ContextMenu_PrevFact:
                    if (!isFactMode) break;

                    int indCurrentFact = recentFactsLocal.indexOf(titleView.getText().toString());
                    if (indCurrentFact == -1) indCurrentFact = recentFactsLocal.size() - 1; //Подстраховка

                    if (indCurrentFact > 0) {
                        String prevFact = recentFactsLocal.get(indCurrentFact - 1);
                        titleView.setText(prevFact);
                        eventText = prevFact;

                        if (indCurrentFact == 1) {
                            removeMenuItem(Constants.ContextMenu_PrevFact);
                        }
                    }
                    return;

                case Constants.ContextMenu_NextFact:
                    if (!isFactMode) break;

                    int indCurrentFact2 = recentFactsLocal.indexOf(titleView.getText().toString());
                    //Если в середине списка, то не получаем новый, а показываем следующий после текущего факт
                    if (indCurrentFact2 < recentFactsLocal.size() - 1) {
                        eventText = recentFactsLocal.get(indCurrentFact2 + 1);
                    } else {
                        List<String> factsNext = eventsData.getNextRandomFacts(1, eventSources);
                        if (factsNext.isEmpty()) return; //todo: выдавать сообщение

                        String newFactText = getFactText(factsNext.get(0));
                        if (!recentFactsLocal.contains(newFactText)) {
                            recentFactsLocal.add(newFactText);
                        }
                        eventText = newFactText;
                    }
                    titleView.setText(eventText);

                    // 4. Динамически добавляем пункт "Предыдущий факт", если его не было
                    if (!menuActions.contains(Constants.ContextMenu_PrevFact)) {
                        addMenuItem(getString(R.string.menu_context_prev_fact),
                                R.drawable.ic_menu_back,
                                Constants.ContextMenu_PrevFact,
                                1);
                    }
                    return;
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            finish();
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == RESULT_OK) {
                if (requestCode == Constants.RESULT_EDIT_EVENT) {
                    eventsData.updateWidgets(appWidgetId, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
        finish();
    }

    @NonNull
    private String getFactText(String factValue) {
        return getString(R.string.event_type_fact_emoji)
                .concat(Constants.STRING_SPACE)
                .concat(factValue);
    }

    @SuppressWarnings("SameParameterValue")
    private void addMenuItem(String text, int iconResId, int actionConst, int position) {
        menuItems.add(position, text);
        menuIcons.add(position, getDrawable(iconResId));
        menuActions.add(position, actionConst);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void removeMenuItem(int actionConst) {
        int index = menuActions.indexOf(actionConst);
        if (index != -1) {
            menuItems.remove(index);
            menuIcons.remove(index);
            menuActions.remove(index);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    static class IconArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final List<String> items;
        private final List<Drawable> icons;

        public IconArrayAdapter(Context context, List<String> items, List<Drawable> icons) {
            super(context, android.R.layout.simple_list_item_1, items);
            this.context = context;
            this.items = items;
            this.icons = icons;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            try {
                TextView textView = listItemView.findViewById(android.R.id.text1);
                textView.setText(Constants.STRING_SPACE.concat(items.get(position)));
                textView.setPadding(ImageUtils.Dip2Px(context.getResources(), 6), 0, 0, 0);
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icons.get(position), null, null, null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }
            return listItemView;
        }
    }
}