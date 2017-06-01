package com.ivianuu.recyclerviewhelpers.undo;

import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Manuel Wrage (IVIanuu)
 */
public class UndoHelper<A extends RecyclerView.Adapter & UndoHelper.UndoAdapter<M>, M> {

    private static final int ACTION_ADD = 1;
    private static final int ACTION_REMOVE = 2;
    private static final int ACTION_MOVE = 3;

    private A mAdapter;

    private UndoListener<M> mUndoListener;

    // Snackbar
    private Snackbar mSnackbar;
    private View mSnackbarContainer;
    private String mSnackbarText;
    private String mSnackbarActionText;
    private int mSnackbarDuration;
    private int mSnackbarActionTextColor;

    private History mHistory = null;

    private UndoHelper(Builder<A, M> builder) {
        mAdapter = builder.adapter;
        mUndoListener = builder.undoListener;

        mSnackbarContainer = builder.snackbarContainer;
        mSnackbarText = builder.snackbarText;

        // use default text
        mSnackbarActionText = builder.snackbarActionText;

        // use default action text

        mSnackbarDuration = builder.snackbarDuration;
        mSnackbarActionTextColor = builder.snackbarActionTextColor;

        mSnackbar = Snackbar.make(mSnackbarContainer, mSnackbarText, mSnackbarDuration)
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        switch (event) {
                            case Snackbar.Callback.DISMISS_EVENT_ACTION:
                                //we can ignore it
                                break;
                            case Snackbar.Callback.DISMISS_EVENT_TIMEOUT:
                                notifyCommit();
                                break;
                            case Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE:
                                notifyCommit();
                                break;
                            case Snackbar.Callback.DISMISS_EVENT_SWIPE:
                                notifyCommit();
                                break;
                        }
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        super.onShown(snackbar);
                        doChange();
                    }
                });

        mSnackbar.setAction(mSnackbarActionText, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoChange();
            }
        });

        // apply the action text color
        if (mSnackbarActionTextColor != Color.TRANSPARENT) {
            mSnackbar.setActionTextColor(mSnackbarActionTextColor);
        }
    }

    /**
     * Cancels the current operation
     */
    public void cancel() {
        mSnackbar.dismiss();
        mHistory = null;
    }

    /**
     * Removes the items on the given positions
     * @param positions
     */
    public void remove(List<Integer> positions) {
        Set<Integer> positionsSet = new TreeSet<>();
        for (Integer position : positions)
            positionsSet.add(position);
        remove(positionsSet);
    }

    /**
     * Removes the items on the given positions
     * @param positions
     */
    public void remove(int... positions) {
        Set<Integer> positionsSet = new TreeSet<>();
        for (Integer position : positions)
            positionsSet.add(position);
        remove(positionsSet);
    }

    /**
     * Removes the items on the given positions
     * @param positions
     */
    public void remove(final Set<Integer> positions) {
        // Notify old history
        if (mHistory != null) notifyCommit();

        History history = new History(ACTION_REMOVE);
        for (int position : positions) {
            history.items.add(new ItemInfo<>(mAdapter.getDataSet().get(position), position));
        }

        Collections.sort(history.items, new Comparator<ItemInfo<M>>() {
            @Override
            public int compare(ItemInfo<M> lhs, ItemInfo<M> rhs) {
                return Integer.valueOf(lhs.position).compareTo(rhs.position);
            }
        });

        mHistory = history;

        if (mSnackbar.isShown()) {
            // Snackbar is currently shown so do change directly
            doChange();
        } else {
            mSnackbar.show();
        }
    }

    private void notifyCommit() {
        if (mHistory != null) {
            if (mHistory.action == ACTION_REMOVE) {
                SortedSet<Integer> positions = new TreeSet<>(new Comparator<Integer>() {
                    @Override
                    public int compare(Integer lhs, Integer rhs) {
                        return lhs.compareTo(rhs);
                    }
                });

                List<M> removedModels = new ArrayList<>();

                for (ItemInfo<M> itemInfo : mHistory.items) {
                    positions.add(itemInfo.position);
                    removedModels.add(itemInfo.model);
                }

                if (mUndoListener != null)
                    mUndoListener.commitRemove(positions, removedModels);

                mHistory = null;
            }
        }
    }

    private void doChange() {
        if (mHistory != null) {
            switch (mHistory.action) {
                case ACTION_REMOVE:
                    for (int i = mHistory.items.size() - 1; i >= 0; i--) {
                        ItemInfo<M> itemInfo = mHistory.items.get(i);
                        mAdapter.getDataSet().remove(itemInfo.position);
                        mAdapter.notifyItemRemoved(itemInfo.position);
                    }
                    break;
            }
        }
    }

    private void undoChange() {
        if (mHistory != null) {
            switch (mHistory.action) {
                case ACTION_REMOVE:
                    for (int i = 0, size = mHistory.items.size(); i < size; i++) {
                        ItemInfo<M> itemInfo = mHistory.items.get(i);
                        mAdapter.getDataSet().add(itemInfo.position, itemInfo.model);
                        mAdapter.notifyItemInserted(itemInfo.position);
                    }
                    break;
            }
        }
        mHistory = null;
    }

    public interface UndoAdapter<M> {
        List<M> getDataSet();
    }

    public interface UndoListener<M> {
        void commitRemove(Set<Integer> positions, List<M> removed);
    }

    private static class ItemInfo<M> {
        M model;
        int position;

        ItemInfo(M model, int position) {
            this.model = model;
            this.position = position;
        }
    }

    private class History {

        private int action;
        private List<ItemInfo<M>> items = new ArrayList<>();

        private History(int action) {
            this.action = action;
        }
    }

    public static class Builder<A extends RecyclerView.Adapter & UndoHelper.UndoAdapter<M>, M> {

        private A adapter;

        private UndoListener<M> undoListener;

        // Snackbar
        private View snackbarContainer;
        private String snackbarText;
        private String snackbarActionText;
        private int snackbarDuration = Snackbar.LENGTH_LONG;
        private int snackbarActionTextColor = Color.TRANSPARENT;

        public Builder<A, M> withAdapter(A adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder<A, M> withUndoListener(UndoListener<M> undoListener) {
            this.undoListener = undoListener;
            return this;
        }

        public Builder<A, M> withSnackbarContainer(View snackbarContainer) {
            this.snackbarContainer = snackbarContainer;
            return this;
        }

        public Builder<A, M> withSnackbarText(String snackbarText) {
            this.snackbarText = snackbarText;
            return this;
        }

        public Builder<A, M> withSnackbarActionText(String snackbarActionText) {
            this.snackbarActionText = snackbarActionText;
            return this;
        }

        public Builder<A, M> withSnackbarDuration(int snackbarDuration) {
            this.snackbarDuration = snackbarDuration;
            return this;
        }

        public Builder<A, M> withSnackbarActionTextColor(int snackbarActionTextColor) {
            this.snackbarActionTextColor = snackbarActionTextColor;
            return this;
        }

        public UndoHelper<A, M> build() {
            if (adapter == null) {
                throw new IllegalStateException("adapter has to be set");
            }
            if (snackbarContainer == null) {
                throw new IllegalStateException("snackbarContainer has to be set");
            }

            return new UndoHelper<>(this);
        }
    }

}
