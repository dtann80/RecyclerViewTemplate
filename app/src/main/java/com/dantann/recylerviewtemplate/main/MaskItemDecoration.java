package com.dantann.recylerviewtemplate.main;


import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class MaskItemDecoration extends RecyclerView.ItemDecoration {

    private static final long DEFAULT_ALPHA_ANIMATION_DURATION = 700L;
    public static final int DEFAULT_VISIBLE_MASK_ALPHA = 255;

    protected Paint mPaint;
    protected ValueAnimator mAlphaValueAnimator;
    private int mTargetAlpha;
    private View mTargetView;
    private RecyclerView mRecyclerView;
    private int mVisibleMaskAlpha = DEFAULT_VISIBLE_MASK_ALPHA;

    public MaskItemDecoration() {
        this(DEFAULT_VISIBLE_MASK_ALPHA);
    }

    public MaskItemDecoration(int visibleMaskAlpha) {
        this(visibleMaskAlpha, false);
    }

    public MaskItemDecoration(int visibleMaskAlpha, boolean isMaskVisible) {
        initialize();
        if (isMaskVisible) {
            mTargetAlpha = visibleMaskAlpha;
            mPaint.setAlpha(mTargetAlpha);
        }
    }

    protected void initialize() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(0);
        mAlphaValueAnimator = new ValueAnimator();
        mAlphaValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPaint.setAlpha((Integer) animation.getAnimatedValue());
                if (mRecyclerView != null) {
                    mRecyclerView.postInvalidateOnAnimation();
                }
            }
        });
        mAlphaValueAnimator.setDuration(DEFAULT_ALPHA_ANIMATION_DURATION);
        mAlphaValueAnimator.setIntValues(0, mVisibleMaskAlpha);
    }

    public void setTargetView(View view) {
        mTargetView = view;
    }

    public void setAnimationDuration(long duration) {
        mAlphaValueAnimator.setDuration(duration);
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);
        mRecyclerView = parent;

        if (mTargetView != null) {
            final View view = mTargetView;
            final float tY = ViewCompat.getTranslationY(view);
            canvas.drawRect(0, 0, parent.getWidth(), view.getTop() + tY, mPaint);
            canvas.drawRect(0, view.getBottom() + tY, parent.getWidth(), parent.getHeight(), mPaint);
        } else {
            canvas.drawRect(0, 0, parent.getWidth(), parent.getHeight(), mPaint);
        }
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(canvas, parent, state);

        if (mTargetView != null) {
            View view = mTargetView;
            if (mPaint.getAlpha() > 0) {
                final float tY = ViewCompat.getTranslationY(view);
                canvas.drawRect(0, view.getTop() + tY, parent.getWidth(), view.getBottom()+tY, mPaint);
            }
        }
    }

    public void animateMaskIn(long delay) {
        animateMask(delay, DEFAULT_VISIBLE_MASK_ALPHA);
    }

    public void animateMaskOut() {
        animateMask(0, 0);
    }

    private void animateMask(long delay, int targetAlpha) {
        if (mTargetAlpha != targetAlpha) {
            mTargetAlpha = targetAlpha;
            if (mAlphaValueAnimator.isStarted() || mAlphaValueAnimator.isRunning()) {
                mAlphaValueAnimator.cancel();
            }
            int currAlpha = mPaint.getAlpha();
            if (currAlpha != mTargetAlpha) {
                mAlphaValueAnimator.setStartDelay(delay);
                mAlphaValueAnimator.setIntValues(currAlpha, mTargetAlpha);
                mAlphaValueAnimator.start();
            }
        }
    }

    public void cancelAnimation() {
        mAlphaValueAnimator.cancel();
    }


}
