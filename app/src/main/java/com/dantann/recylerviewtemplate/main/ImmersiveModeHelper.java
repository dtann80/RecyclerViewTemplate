package com.dantann.recylerviewtemplate.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper class for creating Immersive mode. It must be attached to a RecyclerView and once it
 * has been attached, call {@link #selectView(View)} or {@link #selectPosition(int)} to enter/leave
 * immersive mode. A {@link SelectionChangeListener} can be added to listen for changes in the
 * selected view for immersive mode. {@link ImmersiveTransitionCallback} can be set to change or
 * add behavior with the immersive mode transition.
 */
public class ImmersiveModeHelper {

    public static final long DEFAULT_ANIMATION_DURATION = 500L;

    private RecyclerView mRecyclerView;
    private InternalChildDrawingOrderCallback mChildDrawingOrderCallback = new InternalChildDrawingOrderCallback();
    private InternalOnItemTouchListener mOnItemTouchListener = new InternalOnItemTouchListener();

    private boolean mIsImmersiveMode;

    @Nullable private View mSelectedView;
    private int mSelectedIndex;

    @Nullable
    private AnimatorSet mAnimatorSet;
    private float mTargetTranslation;
    private MaskItemDecoration mMaskItemDecoration;

    private final AnimatorListenerAdapter mEnterAnimatorListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationStart(Animator animation) {
            mIsImmersiveMode = true;
        }

    };

    private final AnimatorListenerAdapter mExitAnimatorListener = new AnimatorListenerAdapter(){

        @Override
        public void onAnimationEnd(Animator animation) {
            mIsImmersiveMode = false;
            mSelectedView = null;
            mSelectedIndex = -1;
            mMaskItemDecoration.setVisibleChild(null);
        }

    };

    private ImmersiveTransitionCallback mImmersiveTransitionCallback;
    private CopyOnWriteArrayList<SelectionChangeListener> mSelectionChangeListeners = new CopyOnWriteArrayList<>();

    /**
     * Attaches to a RecyclerView.
     *
     * @param recyclerView RecyclerView
     */
    public void attachToRecyclerView(RecyclerView recyclerView) {
       attachToRecyclerViewImmersiveMode(recyclerView, RecyclerView.NO_POSITION);
    }

    /**
     * Attaches to a RecyclerView.
     *
     * @param recyclerView RecyclerView
     * @param adapterPosition - providing a valid adapter position will start it in immersive mode
     */
    public void attachToRecyclerViewImmersiveMode(RecyclerView recyclerView, final int adapterPosition) {
        mRecyclerView = recyclerView;
        mRecyclerView.setChildDrawingOrderCallback(mChildDrawingOrderCallback);
        mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);

        if (adapterPosition == RecyclerView.NO_POSITION) {
            mMaskItemDecoration = new MaskItemDecoration();
            mRecyclerView.addItemDecoration(mMaskItemDecoration);
        } else {
            mMaskItemDecoration = new MaskItemDecoration(MaskItemDecoration.DEFAULT_VISIBLE_MASK_ALPHA,true);
            mRecyclerView.addItemDecoration(mMaskItemDecoration);
            restoreImmersiveMode(adapterPosition);
        }
    }

    private void restoreImmersiveMode(final int adapterPosition) {
        if (mRecyclerView.getChildCount() != 0) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(adapterPosition);
            restoreImmersiveWithViewHolder(holder);
        } else {
            //Attempt to restore immersive by using GlobalLayout listener.
            final ViewTreeObserver observer = mRecyclerView.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(adapterPosition);
                        restoreImmersiveWithViewHolder(holder);
                    }
                });
            }
        }
    }

    private void restoreImmersiveWithViewHolder(RecyclerView.ViewHolder holder) {
        if (holder == null) return;

        mSelectedView = holder.itemView;
        if (mSelectedView != null) {
            mSelectedIndex = mRecyclerView.indexOfChild(mSelectedView);
            float targetTranslation = calculateImmersiveTranslationY(mSelectedView);
            mTargetTranslation = targetTranslation;
            mSelectedView.setTranslationY(targetTranslation);
            mIsImmersiveMode = true;
            mMaskItemDecoration.setVisibleChild(mSelectedView);
            notifyOnViewHolderSelectionChanged(mSelectedView,true);
        } else {
            //TODO: Log error
        }
    }

    /**
     * Selects the view, or un-selects it if it has already been selected.
     * If there are any changes in the selected state,
     * {@link SelectionChangeListener#onViewHolderSelectionChanged(RecyclerView.ViewHolder, boolean)}
     * will be called
     *
     * @param view must be a view that is a child of the RecyclerView that is attached.
     */
    public void selectView(View view) {
        if (mRecyclerView == null || view == null || isAnimating()) {
            return;
        }

        if (mIsImmersiveMode) {
            if (mSelectedView == view) {
                onViewSelectionChanged(mSelectedView, mSelectedIndex,false);
            }
        } else {
            int indexOfView = mRecyclerView.indexOfChild(view);
            if (indexOfView >= 0 ) {
                onViewSelectionChanged(view, indexOfView, true);
            } else {
                //TODO: Log error
            }
        }
    }

    /**
     * Selects the position, or un-select it if it has already been selected.
     * If there are any changes in the selected state,
     * {@link SelectionChangeListener#onViewHolderSelectionChanged(RecyclerView.ViewHolder, boolean)}
     * will be called
     *
     * @param position adapter position of view to select.
     */
    public void selectPosition(int position) {
        if (mRecyclerView == null || position < 0 || isAnimating()) {
            return;
        }

        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            selectView(holder.itemView);
        }
    }

    /**
     *
     * @return current selected ViewHolder, null if non is selected.
     */
    @Nullable
    public RecyclerView.ViewHolder getSelectedViewHolder() {
        if (mSelectedView != null && mRecyclerView != null) {
            return mRecyclerView.getChildViewHolder(mSelectedView);
        }
        return null;
    }

    public void addSelectionChangeListener(SelectionChangeListener selectionChangeListener) {
        if (selectionChangeListener != null && !mSelectionChangeListeners.contains(selectionChangeListener)) {
            mSelectionChangeListeners.add(selectionChangeListener);
        }
    }

    public void removeSelectionChangeListener(SelectionChangeListener selectionChangeListener) {
        if (selectionChangeListener != null) {
            mSelectionChangeListeners.remove(selectionChangeListener);
        }
    }

    public void setImmersiveModeListener(ImmersiveTransitionCallback listener) {
        mImmersiveTransitionCallback = listener;
    }

    /**
     * This should be called when the RecyclerView is being destroyed.
     */
    public void detachFromRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.setChildDrawingOrderCallback(null);
            mRecyclerView.removeItemDecoration(mMaskItemDecoration);
            mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
            mRecyclerView = null;
        }

        mSelectedView = null;
    }

    private void animateSelectedViewToCenter() {
        if (mSelectedView == null
                || mSelectedView.getParent() == null
                || mTargetTranslation != 0) {
            return;
        }

        float targetTranslation =  calculateImmersiveTranslationY(mSelectedView);
        //Invalidate to change drawing order, see InternalChildDrawingOrderCallback
        ((View)mSelectedView.getParent()).invalidate();
        animateSelectedView(true, targetTranslation,mEnterAnimatorListener);
    }

    private float calculateImmersiveTranslationY(View view) {
        float parentCenter;
        View parent = (View) view.getParent();
        parentCenter = parent.getHeight()/2.0f;
        float offset = view.getHeight()/2.0f;
        float newTranslationY = parentCenter - (view.getTop() + offset);

        if (mImmersiveTransitionCallback != null) {
            return mImmersiveTransitionCallback.onGetImmersiveTranslationY(getSelectedViewHolder(), newTranslationY);
        }

        return newTranslationY;
    }

    private void animateSelectedViewBack() {
        if (mSelectedView == null || mTargetTranslation == 0) return;

        animateSelectedView(false, 0, mExitAnimatorListener);
    }

    private void animateSelectedView(boolean selected,
                                     float targetTranslationY,
                                     Animator.AnimatorListener animatorListener) {
        if (mSelectedView == null
                || (mTargetTranslation == targetTranslationY)) return;

        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }

        mTargetTranslation = targetTranslationY;

        ArrayList<Animator> animators = new ArrayList<>();

        //Translation animation
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(mSelectedView, "translationY",
                mSelectedView.getTranslationY(), targetTranslationY);
        translationAnimator.setDuration(DEFAULT_ANIMATION_DURATION);
        animators.add(translationAnimator);

        //Mask Animation
        if (selected) {
            mMaskItemDecoration.setVisibleChild(mSelectedView);
        }

        int targetAlpha = selected ? MaskItemDecoration.DEFAULT_VISIBLE_MASK_ALPHA : 0;
        animators.add(mMaskItemDecoration.setupAnimator(targetAlpha));

        if (mImmersiveTransitionCallback != null) {
            RecyclerView.ViewHolder holder = getSelectedViewHolder();
            mImmersiveTransitionCallback.onCreateAnimators(holder, selected, animators);
        }

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
        mAnimatorSet.addListener(animatorListener);
        mAnimatorSet.start();
    }

    /**
     *
     * @return true if in immersive mode or in the middle of transition.
     */
    public boolean isImmersiveMode() {
        return mIsImmersiveMode;
    }

    public boolean isAnimating() {
        return (mAnimatorSet != null && (mAnimatorSet.isRunning() || mAnimatorSet.isStarted()));
    }

    /**
     * This class changes drawing order of the selected view to make it appear on top.
     */
    private class InternalChildDrawingOrderCallback implements RecyclerView.ChildDrawingOrderCallback {

        private int mNextChildIndexToRender;

        @Override
        public int onGetChildDrawingOrder(int childCount, int iteration) {

            if (!mIsImmersiveMode) {
                return iteration;
            }

            if (iteration == childCount - 1) {
                // in the last iteration return the index of the child
                // we want to bring to front (and reset nextChildIndexToRender)
                mNextChildIndexToRender = 0;
                return mSelectedIndex;
            } else {
                if (mNextChildIndexToRender == mSelectedIndex) {
                    // skip this index; we will render it during last iteration
                    mNextChildIndexToRender++;
                }
                return mNextChildIndexToRender++;
            }
        }
    }

    @CallSuper
    protected void onViewSelectionChanged(View selectedView, int selectedIndex, boolean selected) {
        if (selected) {
            mSelectedView = selectedView;
            mSelectedIndex = selectedIndex;
            animateSelectedViewToCenter();
        } else {
            animateSelectedViewBack();
        }

        notifyOnViewHolderSelectionChanged(selectedView,selected);
    }

    private void notifyOnViewHolderSelectionChanged(final View view, boolean selected) {
        final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
        for (SelectionChangeListener selectionChangeListener : mSelectionChangeListeners) {
            selectionChangeListener.onViewHolderSelectionChanged(holder, selected);
        }
    }

    /**
     * Used for disabling scrolling of the RecyclerView. Also used to prevent touch events on other
     * RecyclerView children that aren't visible in immersive mode.
     */
    private class InternalOnItemTouchListener implements RecyclerView.OnItemTouchListener {

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {

            if (mIsImmersiveMode) {
                mRecyclerView.requestDisallowInterceptTouchEvent(true); //This disables scrolling
                if (!isViewHit(mSelectedView, e)) {
                    return true;//Eat any touch events that aren't on the selected view.
                }
            } else {
                mRecyclerView.requestDisallowInterceptTouchEvent(false);
            }

            return false;
        }

        private boolean isViewHit(View view, MotionEvent event) {
            final float x = event.getX();
            final float y = event.getY();
            final float translationX = ViewCompat.getTranslationX(view);
            final float translationY = ViewCompat.getTranslationY(view);
            if (x >= view.getLeft() + translationX &&
                    x <= view.getRight() + translationX &&
                    y >= view.getTop() + translationY &&
                    y <= view.getBottom() + translationY) {
                return true;
            }

            return false;
        }


        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            //Do nothing
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            //Do nothing
        }
    }


    public interface SelectionChangeListener {

        /**
         * Called whenever there are changes in the selected ViewHolder.
         *
         * @param holder ViewHolder
         * @param selected true if selected
         */
        void onViewHolderSelectionChanged(RecyclerView.ViewHolder holder, boolean selected);

    }

    /**
     * Callbacks to change or add behavior with the immersive transition
     * for the {@link ImmersiveModeHelper}
     */
    public interface ImmersiveTransitionCallback {

        /**
         * Called when creating animators for animating in/out immersive mode.
         *
         * @param viewHolder ViewHolder that is being animated.
         * @param selected true if animating into immersive mode, false means otherwise
         * @param animators list of animators that will play together. Initial list will contain
         *                  default animators. Derived classes can add their own animators to the
         *                  list to be animated.
         */
        void onCreateAnimators(RecyclerView.ViewHolder viewHolder, boolean selected, List<Animator> animators);


        /**
         * Called when the view is being animated into immersive mode to get the new translationY.
         *
         * @param viewHolder ViewHolder being animated.
         * @param defaultTranslationY default translationY that centers the view in the center of
         *                            the RecyclerView, return this if there is no change in the
         *                            default translationY.
         * @return the final translationY it should animate to after entering immersive mode.
         */
        float onGetImmersiveTranslationY(RecyclerView.ViewHolder viewHolder, float defaultTranslationY);

    }

}
