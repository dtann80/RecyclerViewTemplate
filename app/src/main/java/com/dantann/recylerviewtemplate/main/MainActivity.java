package com.dantann.recylerviewtemplate.main;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dantann.recylerviewtemplate.R;
import com.dantann.recylerviewtemplate.framework.AbstractActivityLifecycleCallbacks;
import com.dantann.recylerviewtemplate.framework.BaseViewHolder;
import com.dantann.recylerviewtemplate.framework.SpacesItemDecoration;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    private ImmersiveModeHelper mImmersiveModeHelper;
    private int mRestoreImmersiveModeAdapterPosition = RecyclerView.NO_POSITION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        mImmersiveModeHelper = new ImmersiveModeHelper();
        mImmersiveModeHelper.setImmersiveModeListener(new ImmersiveTransitionCallbackInternal());

        recyclerView.setLayoutManager(new SimpleLinearLayoutManager(this));
        recyclerView.setAdapter(new RandomCardAdapter());
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                10, this.getResources().getDisplayMetrics());

        recyclerView.addItemDecoration(new SpacesItemDecoration(padding));

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isChangingConfigurations()) {
            RecyclerView.ViewHolder holder = mImmersiveModeHelper.getSelectedViewHolder();
            if (holder != null) {
                final int adapterPosition = holder.getAdapterPosition();
                getApplication().registerActivityLifecycleCallbacks(new AbstractActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                        getApplication().unregisterActivityLifecycleCallbacks(this);
                        ((MainActivity)activity).mRestoreImmersiveModeAdapterPosition = adapterPosition;
                    }
                });
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mRestoreImmersiveModeAdapterPosition >= 0) {
            mImmersiveModeHelper.attachToRecyclerViewImmersiveMode(recyclerView,mRestoreImmersiveModeAdapterPosition);
            mRestoreImmersiveModeAdapterPosition = RecyclerView.NO_POSITION;
        } else {
            mImmersiveModeHelper.attachToRecyclerView(recyclerView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {

            case R.id.action_reset:
                ProcessPhoenix.triggerRebirth(this);
                break;

            case R.id.action_select:
                mImmersiveModeHelper.selectView(recyclerView.getChildAt(0));
                break;

            case R.id.action_select_pos:
                mImmersiveModeHelper.selectPosition(0);
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private class RandomCardAdapter extends RecyclerView.Adapter<BaseViewHolder> {

        @Override
        public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new BaseViewHolder(new RandomCardView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(BaseViewHolder holder, final int position) {
            RandomCardView cardView = (RandomCardView) holder.itemView;
            cardView.titleTextView.setText("Position: " + position);
            cardView.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(),"Clicked on button " + position,Toast.LENGTH_SHORT).show();
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mImmersiveModeHelper.selectView(v);
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return 25;
        }

    }

    private class SimpleLinearLayoutManager extends LinearLayoutManager {

        public SimpleLinearLayoutManager(Context context) {
            super(context);
        }

        public SimpleLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public SimpleLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private class ImmersiveTransitionCallbackInternal implements ImmersiveModeHelper.ImmersiveTransitionCallback {

        @Override
        public void onCreateAnimators(RecyclerView.ViewHolder viewHolder, boolean selected, List<Animator> animators) {
            final View view = viewHolder.itemView;
            float targetScale = selected ? calculateImmersiveScale(view) : 1.0f;

            //Scale animation
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX",targetScale);
            scaleXAnimator.setDuration(ImmersiveModeHelper.DEFAULT_ANIMATION_DURATION);
            animators.add(scaleXAnimator);
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", targetScale);
            scaleYAnimator.setDuration(ImmersiveModeHelper.DEFAULT_ANIMATION_DURATION);
            animators.add(scaleYAnimator);
        }

        @Override
        public float onGetImmersiveTranslationY(RecyclerView.ViewHolder viewHolder, float defaultTranslationY) {
            return defaultTranslationY;
        }

        private float calculateImmersiveScale(View view) {
            float totalPadding = view.getLeft() + (recyclerView.getWidth() - view.getRight());
            return 1.0f + (totalPadding/view.getWidth());
        }

    }

}
