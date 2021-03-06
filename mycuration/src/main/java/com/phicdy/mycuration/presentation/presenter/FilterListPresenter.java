package com.phicdy.mycuration.presentation.presenter;

import android.support.annotation.NonNull;

import com.phicdy.mycuration.data.db.DatabaseAdapter;
import com.phicdy.mycuration.data.filter.Filter;
import com.phicdy.mycuration.presentation.view.FilterListView;

public class FilterListPresenter implements Presenter {
    private FilterListView view;
    private final DatabaseAdapter dbAdapter;
    public FilterListPresenter(DatabaseAdapter dbAdapter) {
        this.dbAdapter = dbAdapter;
    }

    public void setView(FilterListView view) {
        this.view = view;
    }

    @Override
    public void create() {
    }

    @Override
    public void resume() {
        view.initList(dbAdapter.getAllFilters());
    }

    @Override
    public void pause() {
    }

    public void onDeleteMenuClicked(int position, @NonNull Filter selectedFilter) {
        if (position < 0) return;
        dbAdapter.deleteFilter(selectedFilter.getId());
        view.remove(position);
        view.notifyListChanged();
    }

    public void onEditMenuClicked(@NonNull Filter selectedFilter) {
        int id = selectedFilter.getId();
        // Database table ID starts with 1, ID under 1 means invalid
        if (id <= 0) return;
        view.startEditActivity(id);
    }

    public void onFilterCheckClicked(@NonNull Filter clickedFilter, boolean isChecked) {
        clickedFilter.setEnabled(isChecked);
        dbAdapter.updateFilterEnabled(clickedFilter.getId(), isChecked);
    }
}
