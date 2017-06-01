package com.ivianuu.recyclerviewhelpers.endlessscroll;

import android.support.v7.widget.GridLayoutManager;

class WrapperSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

    private final GridLayoutManager.SpanSizeLookup mWrappedSpanSizeLookup;
    private final int mLoadingItemSpan;
    private final WrapperAdapter mWrapperAdapter;

    WrapperSpanSizeLookup(GridLayoutManager.SpanSizeLookup gridSpanSizeLookup,
                          int loadingItemSpan,
                          WrapperAdapter wrapperAdapter) {
        mWrappedSpanSizeLookup = gridSpanSizeLookup;
        mLoadingItemSpan = loadingItemSpan;
        mWrapperAdapter = wrapperAdapter;
    }

    @Override
    public int getSpanSize(int position) {
        if (mWrapperAdapter.isLoadingItem(position)) {
            return mLoadingItemSpan;
        } else {
            return mWrappedSpanSizeLookup.getSpanSize(position);
        }
    }

    GridLayoutManager.SpanSizeLookup getWrappedSpanSizeLookup() {
        return mWrappedSpanSizeLookup;
    }
}
