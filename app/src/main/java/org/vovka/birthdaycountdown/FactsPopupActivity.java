/*
 * *
 *  * Created by Vladimir Belov on 09.12.2025, 03:04
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 09.12.2025, 01:04
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  `FactsPopupActivity` - это всплывающее окно, отображающее случайные факты пользователю.
 *  Обрабатывает отображение фактов, навигацию между ними, а также отправку фактов в другие приложения.
 */
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
            eventsData.initLanguage(this);

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
                    intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    intentShare.putExtra(Intent.EXTRA_TEXT, txtInfo.getText());
                    startActivity(Intent.createChooser(intentShare, ""));
                });
                addClickEffect(buttonShare);
                buttonShare.getBackground().setAlpha(50);
                buttonShare.setVisibility(View.VISIBLE);

                TextView buttonAction = findViewById(R.id.buttonThirdAction);
                buttonAction.setText(R.string.facts_popup_action_next_fact);
                addClickEffect(buttonAction);
                buttonAction.getBackground().setAlpha(50);
                buttonAction.setVisibility(View.VISIBLE);

                TextView buttonPrev = findViewById(R.id.buttonFirstAction);
                buttonPrev.setText(R.string.popup_action_prev);
                addClickEffect(buttonPrev);
                buttonPrev.getBackground().setAlpha(50);

                buttonAction.setOnClickListener(view -> {
                    List<String> factsNext = eventsData.getNextRandomFacts(1, eventSources);
                    if (!factsNext.isEmpty()) {
                        listPrevFacts.add(txtInfo.getText().toString());
                        txtInfo.setText(getString(R.string.event_type_fact_emoji).concat(Constants.STRING_SPACE).concat(factsNext.get(0)));
                        buttonPrev.setVisibility(View.VISIBLE);
                    }
                });

                buttonPrev.setOnClickListener(viewPrev -> {
                    txtInfo.setText(listPrevFacts.remove(listPrevFacts.size() - 1));
                    if (listPrevFacts.isEmpty()) {
                        buttonPrev.setVisibility(View.GONE);
                    }
                });

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

    void addClickEffect(@NonNull View view)
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
