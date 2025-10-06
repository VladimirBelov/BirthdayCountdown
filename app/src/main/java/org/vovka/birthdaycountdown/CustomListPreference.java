/*
 * *
 *  * Created by Vladimir Belov on 06.10.2025, 03:14
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 05.10.2025, 21:17
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

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
