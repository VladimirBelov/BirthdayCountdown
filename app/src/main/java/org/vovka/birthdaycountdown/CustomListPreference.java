/*
 * *
 *  * Created by Vladimir Belov on 15.03.2026, 22:05
 *  * Copyright (c) 2018 - 2026. All rights reserved.
 *  * Last modified 15.03.2026, 21:14
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Стандартный ListPreference + возможность использования в Summary CharSequence (например, для отображения цветного текста)
 */
public class CustomListPreference extends ListPreference {

    private CharSequence mSummary;

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        this.mSummary = summary;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView summaryView = view.findViewById(android.R.id.summary);
        if (summaryView != null && mSummary != null) {
            summaryView.setText(mSummary);
        }
    }
}
