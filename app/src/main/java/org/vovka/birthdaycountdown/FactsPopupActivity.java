/*
 * *
 *  * Created by Vladimir Belov on 28.03.2026, 18:43
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 28.03.2026, 13:38
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.vovka.birthdaycountdown.utils.StringUtils;
import org.vovka.birthdaycountdown.utils.UiTools;

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
            eventsData.applyLocaleWorkaround(this);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            setContentView(R.layout.widget_calendar_popup);

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(view -> finish());
            }

            eventsData.getFactsEvents(false);

            Set<String> eventSources = new HashSet<String>(){};
            for (String file: eventsData.preferences_FactEvent_files) {
                eventSources.add(StringUtils.getHash(Constants.eventSourceFilePrefix + file));
            }

            TextView txtCaption = findViewById(R.id.textCaption);
            txtCaption.setText(R.string.facts_popup_caption);

            TextView txtInfo = findViewById(R.id.textInfo);
            List<String> facts = eventsData.getNextRandomFacts(1, eventSources);
            if (!facts.isEmpty()) {
                txtInfo.setText(getString(R.string.event_type_fact_emoji).concat(Constants.STRING_SPACE).concat(facts.get(0)));
            } else {
                txtInfo.setText(R.string.facts_popup_empty);
                return;
            }

            List<String> listPrevFacts = new ArrayList<String>(){};

            TextView buttonShare = findViewById(R.id.button2);
            buttonShare.setText(R.string.facts_popup_action_share);
            buttonShare.setOnClickListener(v -> {
                Intent intentShare = new Intent(Intent.ACTION_SEND);
                intentShare.setType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                intentShare.putExtra(Intent.EXTRA_TEXT, txtInfo.getText());
                startActivity(Intent.createChooser(intentShare, ""));
            });
            UiTools.addClickEffect(buttonShare);
            buttonShare.getBackground().setAlpha(50);
            buttonShare.setVisibility(View.VISIBLE);

            TextView buttonNext = findViewById(R.id.button3);
            buttonNext.setText(R.string.facts_popup_action_next_fact);
            UiTools.addClickEffect(buttonNext);
            buttonNext.getBackground().setAlpha(50);
            buttonNext.setVisibility(View.VISIBLE);

            TextView buttonPrev = findViewById(R.id.button1);
            buttonPrev.setText(R.string.popup_action_prev);
            UiTools.addClickEffect(buttonPrev);
            buttonPrev.getBackground().setAlpha(50);
            if (!listPrevFacts.isEmpty()) {
                buttonPrev.setVisibility(View.VISIBLE);
            }

            buttonNext.setOnClickListener(view -> {
                List<String> factsNext = eventsData.getNextRandomFacts(1, eventSources);
                if (!factsNext.isEmpty()) {
                    listPrevFacts.add(txtInfo.getText().toString());
                    txtInfo.setText(getString(R.string.event_type_fact_emoji).concat(Constants.STRING_SPACE).concat(factsNext.get(0)));
                    buttonPrev.setVisibility(View.VISIBLE);
                }
            });

            buttonPrev.setOnClickListener(viewPrev -> {
                if (!listPrevFacts.isEmpty()) {
                    txtInfo.setText(listPrevFacts.remove(listPrevFacts.size() - 1));
                }
                if (listPrevFacts.isEmpty()) {
                    buttonPrev.setVisibility(View.GONE);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

}
