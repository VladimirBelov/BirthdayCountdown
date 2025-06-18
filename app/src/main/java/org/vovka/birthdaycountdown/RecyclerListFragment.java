/*
 * *
 *  * Created by Vladimir Belov on 18.06.2025, 15:45
 *  * Copyright (c) 2018 - 2025. All rights reserved.
 *  * Last modified 18.06.2025, 15:21
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

import org.vovka.birthdaycountdown.helpers.ItemTouchHelperCallback;
import org.vovka.birthdaycountdown.helpers.OnStartDragListener;

/**
 * Фрагмент, который отображает список элементов в RecyclerView.
 * Этот фрагмент позволяет изменять порядок элементов путем перетаскивания (drag and drop).
 * Он использует пользовательский адаптер {@link RecyclerListAdapter} для управления данными списка.
 * <p>
 * Фрагмент ожидает получения данных через аргументы при создании.
 * Эти аргументы должны включать:
 * <ul>
 *     <li>{@link Constants#EXTRA_LIST}: ArrayList строк, представляющий элементы списка.</li>
 *     <li>{@link Constants#EXTRA_CHECKS}: ArrayList целых чисел, представляющий состояние "отмеченности" (checked) каждого элемента.</li>
 *     <li>{@link Constants#EXTRA_COLORED}: ArrayList строк, представляющий состояние "окрашенности" (colored) каждого элемента.</li>
 *     <li>{@link Constants#EXTRA_COLOR}: Целое число, представляющее основной цвет для этого списка.</li>
 *     <li>{@link Constants#EXTRA_NON_SORTED}: ArrayList строк, представляющий исходный, неотсортированный список для управления перетаскиванием.</li>
 *     <li>{@link Constants#EXTRA_TITLE}: Строка, представляющая заголовок, который должен отображаться на панели инструментов (toolbar).</li>
 * </ul>
 * <p>
 * При уничтожении фрагмента он сохраняет результаты (выбранные элементы) обратно в аргументы
 * под ключом {@link Constants#EXTRA_RESULTS}.
 * <p>
 * Он также управляет видимостью элементов макета и заголовком панели инструментов.
 */
class RecyclerListFragment extends Fragment implements OnStartDragListener {

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
