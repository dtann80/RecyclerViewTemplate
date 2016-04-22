package com.dantann.recylerviewtemplate.main;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextView;

import com.dantann.recylerviewtemplate.R;
import com.dantann.recylerviewtemplate.util.MaterialColorPalette;


public class RandomCardView extends CardView {

    public final TextView titleTextView;

    public RandomCardView(Context context) {
        this(context,null);
    }

    public RandomCardView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RandomCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_random_card,this);
        findViewById(R.id.container).setBackgroundColor(MaterialColorPalette.randomColor());
        titleTextView = (TextView) findViewById(R.id.title);
    }
}
