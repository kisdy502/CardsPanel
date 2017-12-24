package com.sdt.libweiget;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

/**
 * 卡片View项
 *
 * @author xmuSistone
 */
@SuppressLint("NewApi")
public class CardItemView extends FrameLayout {
    final static String TAG = "CardItemView";

    private CardSlidePanel parentView;

    private Spring springX, springY;

    public CardItemView(Context context) {
        this(context, null);
    }

    public CardItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSpring();
    }

    /**
     * 弹性动画效果  Facebook 开源框架
     */
    private void initSpring() {
        SpringConfig springConfig = SpringConfig.fromBouncinessAndSpeed(15, 20);
        SpringSystem mSpringSystem = SpringSystem.create();
        springX = mSpringSystem.createSpring().setSpringConfig(springConfig);
        springY = mSpringSystem.createSpring().setSpringConfig(springConfig);
        springX.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int xPos = (int) spring.getCurrentValue();
                Log.d(TAG, "xPos:" + xPos);
                setScreenX(xPos);
                parentView.onViewPosChanged(CardItemView.this);
            }
        });

        springY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int yPos = (int) spring.getCurrentValue();
                setScreenY(yPos);
                parentView.onViewPosChanged(CardItemView.this);
            }
        });
    }

    public void setScreenX(int screenX) {
        this.offsetLeftAndRight(screenX - getLeft());
    }

    public void setScreenY(int screenY) {
        this.offsetTopAndBottom(screenY - getTop());
    }

    public void setParentView(CardSlidePanel parentView) {
        this.parentView = parentView;
    }


    public void bindLayoutResId(int layoutResId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(layoutResId, null);
        addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    /**
     * 如果上一次拖拽,动画正在执行,再去拖拽,会不响应,执行了setAtRest方法后就可以立即响应拖拽了
     */
    public void onStartDragging() {
        springX.setAtRest();
        springY.setAtRest();
    }

    /**
     * 动画移动到某个位置
     */
    public void animTo(int xPos, int yPos) {
        setCurrentSpringPos(getLeft(), getTop());
        springX.setEndValue(xPos);
        springY.setEndValue(yPos);
    }

    /**
     * 设置当前spring位置
     */
    private void setCurrentSpringPos(int xPos, int yPos) {
        springX.setCurrentValue(xPos);
        springY.setCurrentValue(yPos);
    }


}
