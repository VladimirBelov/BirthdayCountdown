/*
 * *
 *  * Created by Vladimir Belov on 21.02.2024, 20:48
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 21.02.2024, 20:48
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.Nullable;

public class FactsPopupActivity extends Activity {

    private static final String TAG = "FactsPopupActivity";
    ContactsEvents eventsData;

    public FactsPopupActivity() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);

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

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.activity_popup);

            eventsData.getFactsEvents(false);

            Set<String> eventSources = new HashSet<String>(){};
            for (String file: eventsData.preferences_FactEvent_files) {
                eventSources.add(ContactsEvents.getHash(Constants.eventSourceFilePrefix + file));
            }

            TextView txtCaption = findViewById(R.id.textCaption);
            txtCaption.setText(R.string.facts_popup_caption);

            TextView txtInfo = findViewById(R.id.textInfo);
            List<String> facts = eventsData.getNextRandomFacts(1, eventSources);
            if (!facts.isEmpty()) {
                txtInfo.setText(getString(R.string.event_type_fact_emoji).concat(Constants.STRING_SPACE).concat(facts.get(0)));
            } else {
                txtInfo.setText(R.string.facts_popup_empty);
            }

            List<String> listPrevFacts = new ArrayList<String>(){};

            if (!facts.isEmpty()) {
                TextView buttonShare = findViewById(R.id.buttonSecondAction);
                buttonShare.setText(R.string.facts_popup_action_share);
                buttonShare.setOnClickListener(v -> {
                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                    intentShare.setType("text/plain");
                    intentShare.putExtra(Intent.EXTRA_TEXT, txtInfo.getText());
                    startActivity(Intent.createChooser(intentShare, ""));
                });
                addClickEffect(buttonShare);
                buttonShare.getBackground().setAlpha(50);
                buttonShare.setVisibility(View.VISIBLE);

                TextView buttonAction = findViewById(R.id.buttonThirdAction);
                buttonAction.setText(R.string.facts_popup_action_next_fact);
                buttonAction.setOnClickListener(view -> {
                    List<String> factsNext = eventsData.getNextRandomFacts(1, eventSources);
                    if (!factsNext.isEmpty()) {

                        listPrevFacts.add(txtInfo.getText().toString());
                        txtInfo.setText(getString(R.string.event_type_fact_emoji).concat(Constants.STRING_SPACE).concat(factsNext.get(0)));

                        TextView buttonPrev = findViewById(R.id.buttonFirstAction);
                        buttonPrev.setText(R.string.facts_popup_action_prev_fact);
                        buttonPrev.setOnClickListener(viewPrev -> {
                            txtInfo.setText(listPrevFacts.remove(listPrevFacts.size() - 1));
                            if (listPrevFacts.isEmpty()) {
                                buttonPrev.setVisibility(View.GONE);
                            }
                        });
                        addClickEffect(buttonPrev);
                        buttonPrev.getBackground().setAlpha(50);
                        buttonPrev.setVisibility(View.VISIBLE);
                    }
                });
                addClickEffect(buttonAction);
                buttonAction.getBackground().setAlpha(50);
                buttonAction.setVisibility(View.VISIBLE);
            }

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    void addClickEffect(View view)
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
