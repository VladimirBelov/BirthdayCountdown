/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vovka.birthdaycountdown;

import static android.view.View.GONE;
import static org.vovka.birthdaycountdown.Constants.EXTRA_CHECKS;
import static org.vovka.birthdaycountdown.Constants.EXTRA_COLOR;
import static org.vovka.birthdaycountdown.Constants.EXTRA_COLORED;
import static org.vovka.birthdaycountdown.Constants.EXTRA_LIST;
import static org.vovka.birthdaycountdown.Constants.EXTRA_RESULTS;
import static org.vovka.birthdaycountdown.Constants.EXTRA_TITLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.vovka.birthdaycountdown.helper.ItemTouchHelperCallback;
import org.vovka.birthdaycountdown.helper.OnStartDragListener;

public class RecyclerListFragment extends Fragment implements OnStartDragListener {

    private ItemTouchHelper mItemTouchHelper;
    private String parentTitle = null;
    RecyclerListAdapter adapter;
    private FragmentActivity fa;

    public RecyclerListFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        assert container != null;
        return new RecyclerView(container.getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() == null) return;
        this.fa = getActivity();

        Bundle args = getArguments();
        if (args == null) {
            fa.onBackPressed();
            return;
        }

        adapter = new RecyclerListAdapter(fa, this, args.getStringArrayList(EXTRA_LIST), args.getIntegerArrayList(EXTRA_CHECKS), args.getStringArrayList(EXTRA_COLORED), args.getInt(EXTRA_COLOR));

        Toolbar toolbar = fa.findViewById(R.id.toolbar);
        if (toolbar != null) {
            parentTitle = toolbar.getTitle().toString();
            toolbar.setTitle(args.getString(EXTRA_TITLE));
        }
        View layoutMain = fa.findViewById(R.id.layout_main);
        if (layoutMain != null) layoutMain.setVisibility(View.GONE);
        View layoutFragment = fa.findViewById(R.id.layout_fragment);
        if (layoutFragment != null) layoutFragment.setVisibility(View.VISIBLE);

        RecyclerView recyclerView = (RecyclerView) view;
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(fa));

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onDestroyView() {
        Bundle args = getArguments();
        if (args != null) {
            args.putStringArrayList(EXTRA_RESULTS, adapter.getAllSelectedItems());
            setArguments(args);
        }

        Toolbar toolbar = fa.findViewById(R.id.toolbar);
        if (toolbar != null && parentTitle != null) toolbar.setTitle(parentTitle);
        View layoutMain = fa.findViewById(R.id.layout_main);
        if (layoutMain != null) layoutMain.setVisibility(View.VISIBLE);
        View layoutFragment = fa.findViewById(R.id.layout_fragment);
        if (layoutFragment != null) layoutFragment.setVisibility(GONE);
        fa.invalidateOptionsMenu();

        super.onDestroyView();
    }
}
