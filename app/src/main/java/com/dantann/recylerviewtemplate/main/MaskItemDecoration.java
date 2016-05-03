package com.dantann.recylerviewtemplate.main;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * ItemDecoration that provides a black mask over the RecyclerView. If a view is set as a visible
 * child the mask will be drawn around the the view.
 */
public class MaskItemDecoration extends RecyclerView.ItemDecoration {

    private static final long DEFAULT_ALPHA_ANIMATION_DURATION = 700L;
    public static final int DEFAULT_VISIBLE_MASK_ALPHA = 255;

    protected Paint mPaint;
    protected ValueAnimator mAlphaValueAnimator;
    private int mTargetAlpha;
    private View mVisibleChild;
    private RecyclerView mRecyclerView;
    private int mVisibleMaskAlpha = DEFAULT_VISIBLE_MASK_ALPHA;

    public MaskItemDecoration() {
        this(DEFAULT_VISIBLE_MASK_ALPHA, false);
    }

    /**
     * Constructor for creating an ItemDecoration with custom mask alpha and initial mask visibility.
     *
     * @param visibleMaskAlpha - alpha of mask when its visible, Value must be in the range 0-255.
     * @param isMaskVisible - true if the mask is visible initially.
     */
    public MaskItemDecoration(int visibleMaskAlpha, boolean isMaskVisible) {
        initialize();
        mVisibleMaskAlpha = clamp(visibleMaskAlpha, 0, 255);
        if (isMaskVisible) {
            mTargetAlpha = visibleMaskAlpha;
            mPaint.setAlpha(mTargetAlpha);
        }
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
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
    }

    /**
     * Set a child view to be revealed when the mask is drawn.
     *
     * @param view - Child view of the RecyclerView.
     */
    public void setVisibleChild(View view) {
        mVisibleChild = view;
    }

    /**
     * Set duration of the Mask animation
     *
     * @param duration long
     */
    public void setAnimationDuration(long duration) {
        mAlphaValueAnimator.setDuration(duration);
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

        mRecyclerView = parent;

        if (mVisibleChild != null) {
            //Draw mask
            final View view = mVisibleChild;
            final float tY = ViewCompat.getTranslationY(view);
            //Above
            canvas.drawRect(0, 0, parent.getWidth(), view.getTop() + tY, mPaint);
            //Below
            canvas.drawRect(0, view.getBottom() + tY, parent.getWidth(), parent.getHeight(), mPaint);

        } else {
            canvas.drawRect(0, 0, parent.getWidth(), parent.getHeight(), mPaint);
        }
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(canvas, parent, state);

        if (mVisibleChild != null) {
            View view = mVisibleChild;
            if (mPaint.getAlpha() > 0) {
                final float tY = ViewCompat.getTranslationY(view);
                //Draw background to match mask
                canvas.drawRect(0, view.getTop() + tY, parent.getWidth(), view.getBottom()+tY, mPaint);
            }
        }
    }

    /**
     * Starts the animating the mask in with the default visible alpha mask.
     */
    public void animateMaskIn() {
        animateMask(mVisibleMaskAlpha);
    }

    /**
     * Animates the mask out if visible.
     */
    public void animateMaskOut() {
        animateMask(0);
    }

    private void animateMask(int targetAlpha) {
        setupAnimator(targetAlpha);
        mAlphaValueAnimator.start();
    }

    /**
     * Creates the animator for the mask with the specified target alpha.
     *
     * @param targetAlpha float value between 0-255
     * @return Animator
     */
    public Animator setupAnimator(int targetAlpha) {
        if (mTargetAlpha != targetAlpha) {
            mTargetAlpha = targetAlpha;
            if (mAlphaValueAnimator.isStarted() || mAlphaValueAnimator.isRunning()) {
                mAlphaValueAnimator.cancel();
            }
            int currAlpha = mPaint.getAlpha();
            if (currAlpha != mTargetAlpha) {
                mAlphaValueAnimator.setIntValues(currAlpha, mTargetAlpha);
            }
        }
        return mAlphaValueAnimator;
    }

    /**
     * Cancel any current mask animations.
     */
    public void cancelAnimation() {
        mAlphaValueAnimator.cancel();
    }

}
