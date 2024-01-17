/*
 * *
 *  * Created by Vladimir Belov on 17.01.2024, 23:29
 *  * Copyright (c) 2018 - 2024. All rights reserved.
 *  * Last modified 11.10.2022, 00:28
 *
 */

package org.vovka.birthdaycountdown;

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

        adapter = new RecyclerListAdapter(
                fa,
                this,
                args.getStringArrayList(Constants.EXTRA_LIST),
                args.getIntegerArrayList(Constants.EXTRA_CHECKS),
                args.getStringArrayList(Constants.EXTRA_COLORED),
                args.getInt(Constants.EXTRA_COLOR),
                args.getStringArrayList(Constants.EXTRA_NON_SORTED)
        );

        Toolbar toolbar = fa.findViewById(R.id.toolbar);
        if (toolbar != null) {
            parentTitle = toolbar.getTitle().toString();
            toolbar.setTitle(args.getString(Constants.EXTRA_TITLE));
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
        if (args != null && !isStateSaved()) {
            args.putStringArrayList(Constants.EXTRA_RESULTS, adapter.getAllSelectedItems());
            setArguments(args);
        }

        Toolbar toolbar = fa.findViewById(R.id.toolbar);
        if (toolbar != null && parentTitle != null) toolbar.setTitle(parentTitle);
        View layoutMain = fa.findViewById(R.id.layout_main);
        if (layoutMain != null) layoutMain.setVisibility(View.VISIBLE);
        View layoutFragment = fa.findViewById(R.id.layout_fragment);
        if (layoutFragment != null) layoutFragment.setVisibility(View.GONE);
        fa.invalidateOptionsMenu();

        super.onDestroyView();
    }
}
