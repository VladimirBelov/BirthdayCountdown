/*
 * *
 *  * Created by Vladimir Belov on 19.03.2025, 12:53
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 19.03.2025, 11:25
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.LocaleManager;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ClipDescription;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WidgetMenuActivity extends Activity {
    private static final String TAG = "WidgetMenuActivity";
    private int appWidgetId;
    final ArrayList<Integer> menuActions = new ArrayList<>();
    String eventText = null;
    String[] singleEventArray = null;
    ContactsEvents eventsData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Получаем данные из Intent
        Intent intent = getIntent();
        if (intent == null || intent.getAction() == null || !intent.getAction().equals(Constants.ACTION_MENU)) {
            finish();
            return;
        }
        try {

            eventsData = ContactsEvents.getInstance();
            if (eventsData.getContext() == null) eventsData.setContext(getApplicationContext());
            eventsData.getPreferences();

            //Без этого на Android 8 и 9 не меняет динамически язык
            Locale locale;
            if (eventsData.preferences_language.equals(getString(R.string.pref_Language_default))) {
                locale = new Locale(eventsData.systemLocale);
            } else {
                locale = new Locale(eventsData.preferences_language);
            }
            Resources applicationRes = getBaseContext().getResources();
            Configuration applicationConf = applicationRes.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    LocaleList list = getSystemService(LocaleManager.class).getApplicationLocales();
                    if (!list.isEmpty()) {
                        locale = getSystemService(LocaleManager.class).getApplicationLocales().get(0);
                    }
                }
                applicationConf.setLocales(new LocaleList(locale));
            } else {
                applicationConf.setLocale(locale);
            }
            applicationRes.updateConfiguration(applicationConf, applicationRes.getDisplayMetrics());

            eventsData.setLocale(true);

            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            String eventInfo = intent.getStringExtra(Constants.EXTRA_CLICKED_EVENT);
            eventText = intent.getStringExtra(Constants.EXTRA_CLICKED_TEXT);
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || eventInfo == null || eventText == null) {
                finish();
                return;
            }

            this.setTheme(eventsData.preferences_theme.themeMain);
            this.getTheme().applyStyle(R.style.FloatingActivity, true);
            setContentView(R.layout.widget_menu);
            setFinishOnTouchOutside(true);

            //Ширина диалога
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            layoutParams.width = (int) (displayMetrics.widthPixels * 0.8);
            getWindow().setAttributes(layoutParams);

            TextView titleView = findViewById(R.id.titleView);
            //todo: добавить ограничение на длину
            titleView.setText(HtmlCompat.fromHtml(eventText, 0));

            ArrayList<String> menuItems = new ArrayList<>();
            List<Drawable> menuIcons = new ArrayList<>();

            singleEventArray = eventInfo.split(Constants.STRING_EOT, -1);

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

                if (!singleEventArray[ContactsEvents.Position_contactID].isEmpty()) {

                    menuItems.add(getString(R.string.menu_context_open_contact));
                    menuIcons.add(getDrawable(R.drawable.ic_menu_friendslist));
                    menuActions.add(Constants.ContextMenu_OpenContact);

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

                }

            }

            menuItems.add(getString(R.string.menu_context_share));
            menuIcons.add(getDrawable(android.R.drawable.ic_menu_share));
            menuActions.add(Constants.ContextMenu_ShareAsText);



            IconArrayAdapter adapter = new IconArrayAdapter(this, menuItems, menuIcons);
            ListView menuListView = findViewById(R.id.menuListView);
            menuListView.setAdapter(adapter);
            menuListView.setOnItemClickListener((parent, view, position, id) -> onMenuItemClick(position, appWidgetId));

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            finish();
        }
    }

    private void onMenuItemClick(int itemId, int appWidgetId) {
        try{

            if (singleEventArray == null) return;
            final String eventKey = eventsData.getEventKey(singleEventArray);
            final String eventKeyWithRawId = eventsData.getEventKeyWithRawId(singleEventArray);

            switch (menuActions.get(itemId)) {
                case Constants.ContextMenu_ShareAsText:

                    final String plainText = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                            ? Html.fromHtml(eventText, Html.FROM_HTML_MODE_LEGACY).toString() : Html.fromHtml(eventText).toString();
                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                    intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    intentShare.putExtra(Intent.EXTRA_TEXT, plainText);
                    intentShare.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    try {
                        Intent intentChooser = Intent.createChooser(intentShare, "");
                        intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intentChooser);
                    } catch (ActivityNotFoundException ignored) { /**/ }
                    break;

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
                            ContactsEvents.parseToLong(singleEventArray[ContactsEvents.Position_eventID]));
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

                    if (eventsData.unsetFavoriteEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

                case Constants.ContextMenu_AddToFavorites:

                    if (eventsData.setFavoriteEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

                case Constants.ContextMenu_HideEvent:

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, ContactsEvents.getInstance().preferences_theme.themeDialog));
                    builder.setTitle(getString(R.string.msg_title_confirmation));
                    builder.setIcon(android.R.drawable.ic_menu_help);
                    builder.setMessage(getString(R.string.msg_event_hide_confirmation));
                    builder.setPositiveButton(R.string.button_yes, (dialog, which) -> {
                        if (eventsData.setHiddenEvent(eventKey, eventKeyWithRawId)) {
                            if (eventsData.checkIsSilencedEvent(eventKey, eventKeyWithRawId)) {
                                //Если скрываем - убираем из списка без уведомления
                                eventsData.unsetSilencedEvent(eventKey, eventKeyWithRawId);
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
                        TypedArray ta = this.getTheme().obtainStyledAttributes(R.styleable.Theme);
                        alertToShow.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                        alertToShow.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ta.getColor(R.styleable.Theme_dialogButtonColor, 0));
                        ta.recycle();
                    });
                    alertToShow.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertToShow.show();

                    return;

                case Constants.ContextMenu_SilentEvent:

                    if (eventsData.setSilencedEvent(eventKey, eventKeyWithRawId)) {
                        eventsData.updateWidgets(appWidgetId, null);
                    }
                    break;

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
                textView.setPadding(ContactsEvents.Dip2Px(context.getResources(), 6), 0, 0, 0);
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icons.get(position), null, null, null);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                ToastExpander.showDebugMsg(context, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
            }
            return listItemView;
        }
    }
}
