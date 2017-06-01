package com.ivianuu.recyclerviewhelpers.endlessscroll;

import android.support.v7.widget.GridLayoutManager;

class WrapperSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

    private final GridLayoutManager.SpanSizeLookup wrappedSpanSizeLookup;
    private final int loadingItemSpan;
    private final WrapperAdapter wrapperAdapter;

    WrapperSpanSizeLookup(GridLayoutManager.SpanSizeLookup wrappedSpanSizeLookup,
                          int loadingItemSpan,
                          WrapperAdapter wrapperAdapter) {
        this.wrappedSpanSizeLookup = wrappedSpanSizeLookup;
        this.loadingItemSpan = loadingItemSpan;
        this.wrapperAdapter = wrapperAdapter;
    }

    @Override
    public int getSpanSize(int position) {
        if (wrapperAdapter.isLoadingItem(position)) {
            return loadingItemSpan;
        } else {
            return wrappedSpanSizeLookup.getSpanSize(position);
        }
    }

    GridLayoutManager.SpanSizeLookup getWrappedSpanSizeLookup() {
        return wrappedSpanSizeLookup;
    }
}
