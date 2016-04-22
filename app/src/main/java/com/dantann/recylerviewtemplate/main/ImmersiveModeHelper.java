package com.dantann.recylerviewtemplate.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * TODO: Create hooks
 * TODO: Make customizable
 * TODO: Make public method for entering and exiting Immersive mode
 */
public class ImmersiveModeHelper {

    private static final long TRANSLATION_ANIMATION_DURATION = 700L;


    private RecyclerView mRecyclerView;
    private InternalOnItemTouchListener mOnItemTouchListener = new InternalOnItemTouchListener();
    private InternalChildDrawingOrderCallback mChildDrawingOrderCallback = new InternalChildDrawingOrderCallback();

    private GestureDetector mGestureDetector;
    private boolean mIsImmersiveMode;

    @Nullable private View mTouchedView;
    private int mTouchedIndex;

    @Nullable private View mSelectedView;
    private int mSelectedIndex;

    private ObjectAnimator mAnimator;
    private float mTargetTranslation;
    private MaskItemDecoration mMaskItemDecoration;
    private Animator.AnimatorListener mEnterAnimatorListener = new EnterAnimatorListener();
    private Animator.AnimatorListener mExitAnimatorListener = new ExitAnimatorListener();

    public void attachToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);
        mRecyclerView.setChildDrawingOrderCallback(mChildDrawingOrderCallback);

        mGestureDetector = new GestureDetector(mRecyclerView.getContext(),new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                ImmersiveModeHelper.this.onSingleTapUp();
                return false;
            }
        });
        mMaskItemDecoration = new MaskItemDecoration();
        mRecyclerView.addItemDecoration(mMaskItemDecoration);
    }

    public void attachToRecyclerViewImmersiveMode(RecyclerView recyclerView, final int adapterPosition) {
        attachToRecyclerView(recyclerView);
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
            float targetTranslation = calculateTargetTranslationY(mSelectedView);
            mTargetTranslation = targetTranslation;
            mSelectedView.setTranslationY(targetTranslation);
            mIsImmersiveMode = true;
            mMaskItemDecoration.setTargetView(mSelectedView);
            mMaskItemDecoration.animateMaskIn(0);
        } else {
            //TODO: Log error
        }
    }

    public RecyclerView.ViewHolder getSelectedViewHolder() {
        if (mSelectedView != null && mRecyclerView != null) {
            return mRecyclerView.findContainingViewHolder(mSelectedView);
        }
        return null;
    }

    public void detachFromRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.removeOnItemTouchListener(mOnItemTouchListener);
            mRecyclerView.setChildDrawingOrderCallback(null);
            mRecyclerView.removeItemDecoration(mMaskItemDecoration);
            mRecyclerView = null;
        }

        mTouchedView = null;
        mSelectedView = null;
    }

    private void onViewTouchEvent(View view,int index, MotionEvent event) {
        mTouchedView = view;
        mTouchedIndex = index;
        mGestureDetector.onTouchEvent(event);
    }

    private void onSingleTapUp() {
        if (mTouchedView == null) return;

        if (mIsImmersiveMode) {
            if (mSelectedView != null && mSelectedView == mTouchedView) {
                animateSelectedViewBack();
            }
        } else {
            mSelectedView = mTouchedView;
            mSelectedIndex = mTouchedIndex;
            animateSelectedViewToCenter();
        }
    }

    private void animateSelectedViewToCenter() {
        if (mSelectedView == null
                || mSelectedView.getParent() == null
                || mTargetTranslation != 0) {
            return;
        }

        float targetTranslation =  calculateTargetTranslationY(mSelectedView);
        //Invalidate to change drawing order, see InternalChildDrawingOrderCallback
        ((View)mSelectedView.getParent()).invalidate();
        animateTranslation(targetTranslation,mEnterAnimatorListener);
        mMaskItemDecoration.setTargetView(mSelectedView);
        mMaskItemDecoration.animateMaskIn(0);
    }

    private float calculateTargetTranslationY(View view) {

        //Calculate Target Translation
        float parentCenter = 0;
        View parent = (View) view.getParent();
        parentCenter = parent.getHeight()/2.0f;
        float offset = view.getHeight()/2.0f;
        return parentCenter - (view.getTop() + offset);
    }

    private void animateSelectedViewBack() {
        if (mSelectedView == null || mTargetTranslation == 0) return;

        animateTranslation(0,mExitAnimatorListener);
        mMaskItemDecoration.animateMaskOut();
    }

    private void animateTranslation(float targetTranslation, Animator.AnimatorListener animatorListener) {
        if (mSelectedView == null || mTargetTranslation == targetTranslation) return;

        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mTargetTranslation = targetTranslation;
        mAnimator = ObjectAnimator.ofFloat(mSelectedView,"translationY",mSelectedView.getTranslationY(),mTargetTranslation);
        mAnimator.setDuration(TRANSLATION_ANIMATION_DURATION);
        mAnimator.addListener(animatorListener);
        mAnimator.start();
    }

    private class InternalOnItemTouchListener implements RecyclerView.OnItemTouchListener {

        @Override
        public boolean onInterceptTouchEvent(android.support.v7.widget.RecyclerView rv, MotionEvent e) {

            if (mIsImmersiveMode) {
                //Only the selected view can be tapped
                if (isViewHit(mSelectedView,e)) {
                    onViewTouchEvent(mSelectedView,mSelectedIndex,e);
                    return mIsImmersiveMode;
                }
            } else {
                //Find view that was touched.
                final int count = rv.getChildCount();
                for (int i = count - 1; i >= 0; i--) {
                    final View child = rv.getChildAt(i);
                    if (isViewHit(child,e)) {
                        onViewTouchEvent(child,i,e);
                        return mIsImmersiveMode;
                    }
                }
            }

            return mIsImmersiveMode;
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
        public void onTouchEvent(android.support.v7.widget.RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    /**
     * This class changes drawing order of the selected view to make it appear on top.
     */
    private class InternalChildDrawingOrderCallback implements RecyclerView.ChildDrawingOrderCallback {

        private int nextChildIndexToRender;

        @Override
        public int onGetChildDrawingOrder(int childCount, int iteration) {

            if (!mIsImmersiveMode) {
                return iteration;
            }

            if (iteration == childCount - 1) {
                // in the last iteration return the index of the child
                // we want to bring to front (and reset nextChildIndexToRender)
                nextChildIndexToRender = 0;
                return mSelectedIndex;
            } else {
                if (nextChildIndexToRender == mSelectedIndex) {
                    // skip this index; we will render it during last iteration
                    nextChildIndexToRender++;
                }
                return nextChildIndexToRender++;
            }
        }
    }

    private class EnterAnimatorListener extends AnimatorListenerAdapter {

        @Override
        public void onAnimationStart(Animator animation) {
            mIsImmersiveMode = true;
        }
    }

    private class ExitAnimatorListener extends AnimatorListenerAdapter {

        @Override
        public void onAnimationEnd(Animator animation) {
            mIsImmersiveMode = false;
            mSelectedView = null;
            mSelectedIndex = -1;
            mMaskItemDecoration.setTargetView(null);
        }
    }

}
