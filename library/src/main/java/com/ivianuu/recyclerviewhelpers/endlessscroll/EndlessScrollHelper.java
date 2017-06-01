package com.ivianuu.recyclerviewhelpers.endlessscroll;

import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

public final class EndlessScrollHelper {

    private static final String TAG = EndlessScrollHelper.class.getSimpleName();

    private final RecyclerView mRecyclerView;

    private final Callbacks mCallbacks;

    private final int mLoadingTriggerThreshold;

    private WrapperAdapter mWrapperAdapter;
    private WrapperSpanSizeLookup mWrapperSpanSizeLookup;

    private boolean mLoading;
    private int mVisibleItemCount;
    private int mPreviousTotal;
    private int mTotalItemCount;
    private int mCurrentPage = 1;
    private boolean mAllItemsLoaded;

    private EndlessScrollHelper(Builder builder) {
        mRecyclerView = builder.recyclerView;
        mCallbacks = builder.callbacks;
        mLoadingTriggerThreshold = builder.loadingTriggerThreshold;

        // Attach scrolling listener in order to perform end offset check on each scroll event
        mRecyclerView.addOnScrollListener(mOnScrollListener);

        if (builder.addLoadingItem) {
            // Wrap existing adapter with new adapter that will add loading row
            RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            mWrapperAdapter = new WrapperAdapter(adapter, builder.mLoadingItemCreator);
            adapter.registerAdapterDataObserver(mDataObserver);
            mRecyclerView.setAdapter(mWrapperAdapter);

            // For GridLayoutManager use separate/customisable span lookup for loading row
            if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
                mWrapperSpanSizeLookup = new WrapperSpanSizeLookup(
                        ((GridLayoutManager) mRecyclerView.getLayoutManager()).getSpanSizeLookup(),
                        builder.loadingItemSpan,
                        mWrapperAdapter);
                ((GridLayoutManager) mRecyclerView.getLayoutManager()).setSpanSizeLookup(mWrapperSpanSizeLookup);
            }
        }

        // Trigger initial check since adapter might not have any items initially so no scrolling events upon
        // RecyclerView (that triggers check) will occur
        checkEndOffset();
    }

    private void checkEndOffset() {
        mVisibleItemCount = mRecyclerView.getChildCount();
        mTotalItemCount = mRecyclerView.getLayoutManager().getItemCount();

        int firstVisibleItemPosition;
        if (mRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            firstVisibleItemPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else if (mRecyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            // https://code.google.com/p/android/issues/detail?id=181461
            if (mRecyclerView.getLayoutManager().getChildCount() > 0) {
                firstVisibleItemPosition = ((StaggeredGridLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPositions(null)[0];
            } else {
                firstVisibleItemPosition = 0;
            }
        } else {
            throw new IllegalStateException("LayoutManager needs to subclass LinearLayoutManager or StaggeredGridLayoutManager");
        }

        // check if were finished with loading
        if (mLoading) {
            if (mTotalItemCount > mPreviousTotal) {
                mLoading = false;
                mPreviousTotal = mTotalItemCount;
                Log.d(TAG, "were finished with loading");
            } else {
                Log.d(TAG, "were actually loading");
            }
        }
        // Check if end of the list is reached (counting threshold) or if there is no items at all
        if ((mTotalItemCount - mVisibleItemCount) <= (firstVisibleItemPosition + mLoadingTriggerThreshold)
                || mTotalItemCount == 0) {
            Log.d(TAG, "first check passed");
            // Call load more only if loading is not currently in progress and if there is more items to load
            if (!mLoading && !mAllItemsLoaded) {
                mCurrentPage ++;
                mCallbacks.onLoadMore(mCurrentPage);
                mLoading = true;
                Log.d(TAG, "we should load some more items now current page " + mCurrentPage);
            } else {
                Log.d(TAG, "were already loading or all items have loaded");
            }
        }
    }

    private void onAdapterDataChanged() {
        mWrapperAdapter.showLoadingItem(!mAllItemsLoaded);
        checkEndOffset();
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            checkEndOffset(); // Each time when list is scrolled check if end of the list is reached
        }
    };

    public void setAllItemsLoaded() {
        mAllItemsLoaded = true;
        if (mWrapperAdapter != null) {
            mWrapperAdapter.showLoadingItem(false);
        }
    }

    public int getTotalItemCount() {
        return mTotalItemCount;
    }

    public int getPreviousTotal() {
        return mPreviousTotal;
    }

    public int getVisibleItemCount() {
        return mVisibleItemCount;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void resetPageCount() {
        resetPageCount(0);
    }

    public void resetPageCount(int page) {
        // resetting values
        mTotalItemCount = 0;
        mPreviousTotal = 0;
        mLoading = true;
        mCurrentPage = page;
        mAllItemsLoaded = false;
        if (mWrapperAdapter != null) {
            mWrapperAdapter.showLoadingItem(true);
        }
        mCallbacks.onLoadMore(mCurrentPage);
    }

    public void unbind() {
        mRecyclerView.removeOnScrollListener(mOnScrollListener);   // Remove scroll listener
        if (mRecyclerView.getAdapter() instanceof WrapperAdapter) {
            WrapperAdapter mWrapperAdapter = (WrapperAdapter) mRecyclerView.getAdapter();
            RecyclerView.Adapter adapter = mWrapperAdapter.getWrappedAdapter();
            adapter.unregisterAdapterDataObserver(mDataObserver); // Remove data observer
            mRecyclerView.setAdapter(adapter);                     // Swap back original adapter
        }
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager && mWrapperSpanSizeLookup != null) {
            // Swap back original SpanSizeLookup
            GridLayoutManager.SpanSizeLookup spanSizeLookup = mWrapperSpanSizeLookup.getWrappedSpanSizeLookup();
            ((GridLayoutManager) mRecyclerView.getLayoutManager()).setSpanSizeLookup(spanSizeLookup);
        }
    }

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            mWrapperAdapter.notifyDataSetChanged();
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapperAdapter.notifyItemRangeInserted(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapperAdapter.notifyItemRangeChanged(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mWrapperAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapperAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapperAdapter.notifyItemMoved(fromPosition, toPosition);
            onAdapterDataChanged();
        }
    };

    public static class Builder {

        private RecyclerView recyclerView;
        private EndlessScrollHelper.Callbacks callbacks;

        private int loadingTriggerThreshold = 1;
        private boolean addLoadingItem = true;
        private LoadingItemCreator mLoadingItemCreator;
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
            this.mLoadingItemCreator = creator;
            return this;
        }

        public Builder withLoadingItemSpan(@NonNull int loadingItemSpan) {
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

            if (mLoadingItemCreator == null) {
                mLoadingItemCreator = LoadingItemCreator.DEFAULT;
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
