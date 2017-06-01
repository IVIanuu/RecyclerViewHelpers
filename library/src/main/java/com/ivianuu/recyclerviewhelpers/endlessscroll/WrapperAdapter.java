package com.ivianuu.recyclerviewhelpers.endlessscroll;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

class WrapperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_VIEW_TYPE_LOADING = Integer.MAX_VALUE - 50; // Magic

    private final RecyclerView.Adapter mWrappedAdapter;

    private final LoadingItemCreator mLoadingItemCreator;

    private boolean mShowLoadingItem = true;

    WrapperAdapter(RecyclerView.Adapter adapter, LoadingItemCreator creator) {
        mWrappedAdapter = adapter;
        mLoadingItemCreator = creator;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_LOADING) {
            return mLoadingItemCreator.onCreateViewHolder(parent, viewType);
        } else {
            return mWrappedAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (isLoadingItem(position)) {
            mLoadingItemCreator.onBindViewHolder(holder, position);
        } else {
            mWrappedAdapter.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return mShowLoadingItem ? mWrappedAdapter.getItemCount() + 1 : mWrappedAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return isLoadingItem(position) ? ITEM_VIEW_TYPE_LOADING : mWrappedAdapter.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        return isLoadingItem(position) ? RecyclerView.NO_ID : mWrappedAdapter.getItemId(position);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
        mWrappedAdapter.setHasStableIds(hasStableIds);
    }

    RecyclerView.Adapter getWrappedAdapter() {
        return mWrappedAdapter;
    }

    boolean isShowingLoadingItem() {
        return mShowLoadingItem;
    }

    void showLoadingItem(boolean showLoadingItem) {
        if (mShowLoadingItem != showLoadingItem) {
            mShowLoadingItem = showLoadingItem;
            notifyDataSetChanged();
        }
    }

    boolean isLoadingItem(int position) {
        return mShowLoadingItem && position == getLoadingItemPosition();
    }

    private int getLoadingItemPosition() {
        return mShowLoadingItem ? getItemCount() - 1 : -1;
    }
}