package com.sdt.libweiget;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 卡片View项
 *
 * @author xmuSistone
 */
@SuppressLint("NewApi")
public class CardItemView extends FrameLayout {

    private CardSlidePanel parentView;

    public CardItemView(Context context) {
        this(context, null);
    }

    public CardItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setParentView(CardSlidePanel parentView) {
        this.parentView = parentView;
    }


    public void bindLayoutResId(int layoutResId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(layoutResId, null);
        addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

}
