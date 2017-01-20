package com.phicdy.mycuration.view;


import android.support.annotation.NonNull;

public interface RegisterFilterView {
    String filterKeyword();
    String filterUrl();
    String filterTitle();
    void setFilterTitle(@NonNull String title);
    void setFilterTargetRss(@NonNull String rss);
    void setFilterUrl(@NonNull String url);
    void setFilterKeyword(@NonNull String keyword);
    void handleEmptyTitle();
    void handleEmptyCondition();
    void handlePercentOnly();
    void finish();
    void showSaveSuccessToast();
    void showSaveErrorToast();
    void trackEdit();
    void trackRegister();
}