package com.ivianuu.recyclerviewhelpers.endlessscroll;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

class WrapperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_VIEW_TYPE_LOADING = Integer.MAX_VALUE - 50; // Magic

    private final RecyclerView.Adapter wrappedAdapter;

    private final LoadingItemCreator loadingItemCreator;

    private boolean showLoadingItem = true;

    WrapperAdapter(RecyclerView.Adapter adapter, LoadingItemCreator creator) {
        wrappedAdapter = adapter;
        loadingItemCreator = creator;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_LOADING) {
            return loadingItemCreator.onCreateViewHolder(parent, viewType);
        } else {
            return wrappedAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (isLoadingItem(position)) {
            loadingItemCreator.onBindViewHolder(holder, position);
        } else {
            wrappedAdapter.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return showLoadingItem ? wrappedAdapter.getItemCount() + 1 : wrappedAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return isLoadingItem(position) ? ITEM_VIEW_TYPE_LOADING : wrappedAdapter.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        return isLoadingItem(position) ? RecyclerView.NO_ID : wrappedAdapter.getItemId(position);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
        wrappedAdapter.setHasStableIds(hasStableIds);
    }

    RecyclerView.Adapter getWrappedAdapter() {
        return wrappedAdapter;
    }

    boolean isShowingLoadingItem() {
        return showLoadingItem;
    }

    void showLoadingItem(boolean showLoadingItem) {
        if (this.showLoadingItem != showLoadingItem) {
            this.showLoadingItem = showLoadingItem;
            notifyDataSetChanged();
        }
    }

    boolean isLoadingItem(int position) {
        return showLoadingItem && position == getLoadingItemPosition();
    }

    private int getLoadingItemPosition() {
        return showLoadingItem ? getItemCount() - 1 : -1;
    }
}