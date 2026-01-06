/*
 * *
 *  * Created by Vladimir Belov on 07.01.2026, 01:04
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 01.01.2026, 23:04
 *
 */

package org.vovka.birthdaycountdown;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.vovka.birthdaycountdown.utils.UiTools;

import java.util.List;

public class EventsPopupActivity extends Activity {
    private static final String TAG = "EventsPopupActivity";
    ContactsEvents eventsData;
    private List<String> eventsList;
    private int currentIndex = 0;
    TextView txtCaption;
    TextView txtInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        try {
            super.onCreate(savedInstanceState);

            eventsData = ContactsEvents.getInstance();
            eventsData.initLanguage(this);

            this.setTheme(eventsData.preferences_theme.themeDialog);

            // Получаем данные из Intent
            eventsList = getIntent().getStringArrayListExtra(Constants.EXTRA_LIST);
            if (eventsList == null || eventsList.isEmpty()) {
                finish();
                return;
            }

            setContentView(R.layout.activity_popup);

            TextView buttonClose = findViewById(R.id.buttonClose);
            if (buttonClose != null) {
                buttonClose.setText(Constants.BUTTON_X);
                buttonClose.setOnClickListener(v -> finish());
            }

            txtCaption = findViewById(R.id.textCaption);
            txtInfo = findViewById(R.id.textInfo);

            updateInfoText();

            //Кнопка "Поделиться"
            TextView buttonShare = findViewById(R.id.buttonSecondAction);
            buttonShare.setText(R.string.facts_popup_action_share);
            buttonShare.setOnClickListener(v -> shareCurrentEvent());
            UiTools.addClickEffect(buttonShare);
            buttonShare.getBackground().setAlpha(50);
            buttonShare.setVisibility(View.VISIBLE);

            if (eventsList.size() > 1) {
                //Кнопка "Следующий"
                TextView buttonNext = findViewById(R.id.buttonThirdAction);
                buttonNext.setText(R.string.popup_action_next);
                buttonNext.setOnClickListener(v -> showNext());
                UiTools.addClickEffect(buttonNext);
                buttonNext.getBackground().setAlpha(50);
                buttonNext.setVisibility(View.VISIBLE);

                //Кнопка "Предыдущий"
                TextView buttonPrev = findViewById(R.id.buttonFirstAction);
                buttonPrev.setText(R.string.popup_action_prev);
                buttonPrev.setOnClickListener(v -> showPrev());
                UiTools.addClickEffect(buttonPrev);
                buttonPrev.getBackground().setAlpha(50);
                buttonPrev.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            ToastExpander.showDebugMsg(this, ContactsEvents.getMethodName(3) + Constants.STRING_COLON_SPACE + e);
        }
    }

    private void updateInfoText() {
        String fact = eventsList.get(currentIndex);
        txtInfo.setText(fact);
        txtCaption.setText(getString(R.string.popup_events_title, currentIndex + 1, eventsList.size()));
    }

    private void showNext() {
        if (currentIndex < eventsList.size() - 1) {
            currentIndex++;
        } else {
            currentIndex = 0;
        }
        updateInfoText();
    }

    private void showPrev() {
        if (currentIndex > 0) {
            currentIndex--;
        } else {
            currentIndex = eventsList.size() - 1;
        }
        updateInfoText();
    }

    private void shareCurrentEvent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, txtInfo.getText());
        startActivity(Intent.createChooser(intent, ""));
    }
}
