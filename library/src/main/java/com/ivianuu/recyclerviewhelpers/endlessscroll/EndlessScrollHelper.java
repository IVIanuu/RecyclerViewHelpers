package com.ivianuu.recyclerviewhelpers.endlessscroll;

import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

public final class EndlessScrollHelper {

    private static final String TAG = EndlessScrollHelper.class.getSimpleName();

    private final RecyclerView recyclerView;

    private final Callbacks callbacks;

    private final int loadingTriggerThreshold;

    private WrapperAdapter wrapperAdapter;
    private WrapperSpanSizeLookup wrapperSpanSizeLookup;

    private boolean loading;
    private int visibleItemCount;
    private int previousTotal;
    private int mTotalItemCount;
    private int currentPage = 1;
    private boolean allItemsLoaded;

    private EndlessScrollHelper(Builder builder) {
        recyclerView = builder.recyclerView;
        callbacks = builder.callbacks;
        loadingTriggerThreshold = builder.loadingTriggerThreshold;

        // Attach scrolling listener in order to perform end offset check on each scroll event
        recyclerView.addOnScrollListener(mOnScrollListener);

        if (builder.addLoadingItem) {
            // Wrap existing adapter with new adapter that will add loading row
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            wrapperAdapter = new WrapperAdapter(adapter, builder.loadingItemCreator);
            adapter.registerAdapterDataObserver(mDataObserver);
            recyclerView.setAdapter(wrapperAdapter);

            // For GridLayoutManager use separate/customisable span lookup for loading row
            if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                wrapperSpanSizeLookup = new WrapperSpanSizeLookup(
                        ((GridLayoutManager) recyclerView.getLayoutManager()).getSpanSizeLookup(),
                        builder.loadingItemSpan,
                        wrapperAdapter);
                ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(wrapperSpanSizeLookup);
            }
        }

        // Trigger initial check since adapter might not have any items initially so no scrolling events upon
        // RecyclerView (that triggers check) will occur
        checkEndOffset();
    }

    private void checkEndOffset() {
        visibleItemCount = recyclerView.getChildCount();
        mTotalItemCount = recyclerView.getLayoutManager().getItemCount();

        int firstVisibleItemPosition;
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            firstVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            // https://code.google.com/p/android/issues/detail?id=181461
            if (recyclerView.getLayoutManager().getChildCount() > 0) {
                firstVisibleItemPosition = ((StaggeredGridLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPositions(null)[0];
            } else {
                firstVisibleItemPosition = 0;
            }
        } else {
            throw new IllegalStateException("LayoutManager needs to subclass LinearLayoutManager or StaggeredGridLayoutManager");
        }

        // check if were finished with loading
        if (loading) {
            if (mTotalItemCount > previousTotal) {
                loading = false;
                previousTotal = mTotalItemCount;
                Log.d(TAG, "were finished with loading");
            } else {
                Log.d(TAG, "were actually loading");
            }
        }
        // Check if end of the list is reached (counting threshold) or if there is no items at all
        if ((mTotalItemCount - visibleItemCount) <= (firstVisibleItemPosition + loadingTriggerThreshold)
                || mTotalItemCount == 0) {
            Log.d(TAG, "first check passed");
            // Call load more only if loading is not currently in progress and if there is more items to load
            if (!loading && !allItemsLoaded) {
                currentPage ++;
                callbacks.onLoadMore(currentPage);
                loading = true;
                Log.d(TAG, "we should load some more items now current page " + currentPage);
            } else {
                Log.d(TAG, "were already loading or all items have loaded");
            }
        }
    }

    private void onAdapterDataChanged() {
        wrapperAdapter.showLoadingItem(!allItemsLoaded);
        checkEndOffset();
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            checkEndOffset(); // Each time when list is scrolled check if end of the list is reached
        }
    };

    public void setAllItemsLoaded() {
        allItemsLoaded = true;
        if (wrapperAdapter != null) {
            wrapperAdapter.showLoadingItem(false);
        }
    }

    public int getTotalItemCount() {
        return mTotalItemCount;
    }

    public int getPreviousTotal() {
        return previousTotal;
    }

    public int getVisibleItemCount() {
        return visibleItemCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void resetPageCount() {
        resetPageCount(0);
    }

    public void resetPageCount(int page) {
        // resetting values
        mTotalItemCount = 0;
        previousTotal = 0;
        loading = true;
        currentPage = page;
        allItemsLoaded = false;
        if (wrapperAdapter != null) {
            wrapperAdapter.showLoadingItem(true);
        }
        callbacks.onLoadMore(currentPage);
    }

    public void unbind() {
        recyclerView.removeOnScrollListener(mOnScrollListener);   // Remove scroll listener
        if (recyclerView.getAdapter() instanceof WrapperAdapter) {
            WrapperAdapter wrapperAdapter = (WrapperAdapter) recyclerView.getAdapter();
            RecyclerView.Adapter adapter = wrapperAdapter.getWrappedAdapter();
            adapter.unregisterAdapterDataObserver(mDataObserver); // Remove data observer
            recyclerView.setAdapter(adapter);                     // Swap back original adapter
        }
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager && wrapperSpanSizeLookup != null) {
            // Swap back original SpanSizeLookup
            GridLayoutManager.SpanSizeLookup spanSizeLookup = wrapperSpanSizeLookup.getWrappedSpanSizeLookup();
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(spanSizeLookup);
        }
    }

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            wrapperAdapter.notifyDataSetChanged();
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            wrapperAdapter.notifyItemRangeInserted(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            wrapperAdapter.notifyItemRangeChanged(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            wrapperAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            wrapperAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            wrapperAdapter.notifyItemMoved(fromPosition, toPosition);
            onAdapterDataChanged();
        }
    };

    public static class Builder {

        private RecyclerView recyclerView;
        private EndlessScrollHelper.Callbacks callbacks;

        private int loadingTriggerThreshold = 1;
        private boolean addLoadingItem = true;
        private LoadingItemCreator loadingItemCreator;
        private int loadingItemSpan = -1;

        public Builder() {

        }

        public Builder withRecyclerView(@NonNull RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
            return this;
        }

        public Builder withCallbacks(@NonNull Callbacks callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        public Builder withLoadingTriggerThreshold(int threshold) {
            this.loadingTriggerThreshold = threshold;
            return this;
        }

        public Builder withAddLoadingItem(boolean addLoadingItem) {
            this.addLoadingItem = addLoadingItem;
            return this;
        }

        public Builder withLoadingItemCreator(@NonNull LoadingItemCreator creator) {
            this.loadingItemCreator = creator;
            return this;
        }

        public Builder withLoadingItemSpan(int loadingItemSpan) {
            this.loadingItemSpan = loadingItemSpan;
            return this;
        }

        public EndlessScrollHelper build() {
            if (recyclerView == null) {
                throw new IllegalStateException("recyclerview has to be set");
            }
            if (callbacks == null) {
                throw new IllegalStateException("callbacks have to be set");
            }
            if (recyclerView.getAdapter() == null) {
                throw new IllegalStateException("Adapter needs to be set!");
            }
            if (recyclerView.getLayoutManager() == null) {
                throw new IllegalStateException("LayoutManager needs to be set on the RecyclerView");
            }

            if (loadingItemCreator == null) {
                loadingItemCreator = LoadingItemCreator.DEFAULT;
            }

            if (loadingItemSpan == -1) {
                // set default span
                loadingItemSpan = recyclerView.getLayoutManager() instanceof GridLayoutManager
                        ? ((GridLayoutManager) recyclerView.getLayoutManager()).getSpanCount() : 1;
            }

            return new EndlessScrollHelper(this);
        }
    }

    public interface Callbacks {
        void onLoadMore(int currentPage);
    }

}
