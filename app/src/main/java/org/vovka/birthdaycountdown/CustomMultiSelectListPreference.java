/*
 * *
 *  * Created by Vladimir Belov on 14.10.2025, 03:34
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 14.10.2025, 01:44
 *
 */

package org.vovka.birthdaycountdown;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CustomMultiSelectListPreference extends MultiSelectListPreference {

    private CharSequence mSummary;

    public CustomMultiSelectListPreference(Context context, AttributeSet attrs) {
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
