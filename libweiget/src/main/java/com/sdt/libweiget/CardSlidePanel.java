package com.sdt.libweiget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/12/21.
 */

public class CardSlidePanel extends ViewGroup {

    final static String TAG = "CardSlidePanel";

    private List<CardItemView> viewList = new ArrayList<>(); // 存放的是每一层的view，从顶到底
    private List<View> releasedViewList = new ArrayList<>(); // 手指松开后存放的view列表

    /* 拖拽工具类 */
    private final ViewDragHelper mDragHelper; // 这个跟原生的ViewDragHelper差不多，我仅仅只是修改了Interpolator
    private int initCenterViewX = 0, initCenterViewY = 0; // 最初时，中间View的x位置,y位置
    private int allWidth = 0; // 面板的宽度
    private int allHeight = 0; // 面板的高度
    private int childWith = 0; // 每一个子View对应的宽度

    private static final float SCALE_STEP = 0.08f; // view叠加缩放的步长
    private static final int MAX_SLIDE_DISTANCE_LINKAGE = 500; // 水平距离+垂直距离

    private int itemMarginTop = 10; // 卡片距离顶部的偏移量
    private int bottomMarginTop = 40; // 底部按钮与卡片的margin值
    private int yOffsetStep = 40; // view叠加垂直偏移量的步长
    private int mTouchSlop = 5; // 判定为滑动的阈值，单位是像素

    private static final int X_VEL_THRESHOLD = 800;
    private static final int X_DISTANCE_THRESHOLD = 300;

    public static final int VANISH_TYPE_LEFT = 0;
    public static final int VANISH_TYPE_RIGHT = 1;

    private int isShowing = 0; // 当前正在显示的小项
    private boolean btnLock = false;
    private GestureDetectorCompat moveDetector;
    private Point downPoint = new Point();
    private CardAdapter adapter;
    private static final int VIEW_COUNT = 4;
    private Rect draggableArea;
    private WeakReference<Object> savedFirstItemData;

    public CardSlidePanel(Context context) {
        this(context, null);
    }

    public CardSlidePanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardSlidePanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.card);

        itemMarginTop = (int) a.getDimension(R.styleable.card_itemMarginTop, itemMarginTop);
        bottomMarginTop = (int) a.getDimension(R.styleable.card_bottomMarginTop, bottomMarginTop);
        yOffsetStep = (int) a.getDimension(R.styleable.card_yOffsetStep, yOffsetStep);
        // 滑动相关类
        mDragHelper = ViewDragHelper
                .create(this, 10f, new DragHelperCallback());
        mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);
        a.recycle();

        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getChildCount() != VIEW_COUNT) {
                    doBindAdapter();
                }
            }
        });
    }


    private void doBindAdapter() {
        if (adapter == null || allWidth <= 0 || allHeight <= 0) {
            return;
        }

        // 1. addView添加到ViewGroup中
        for (int i = 0; i < VIEW_COUNT; i++) {
            CardItemView itemView = new CardItemView(getContext());
            itemView.bindLayoutResId(adapter.getLayoutId());
            itemView.setParentView(this);
            addView(itemView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            if (i == 0) {
                itemView.setAlpha(0);
            }
        }

        // 2. viewList初始化
        viewList.clear();
        for (int i = 0; i < VIEW_COUNT; i++) {
            viewList.add((CardItemView) getChildAt(VIEW_COUNT - 1 - i));
        }


        // 3. 填充数据
        int count = adapter.getCount();
        for (int i = 0; i < VIEW_COUNT; i++) {
            if (i < count) {
                adapter.bindView(viewList.get(i), i);
                if (i == 0) {
                    savedFirstItemData = new WeakReference<>(adapter.getItem(i));
                }
            } else {
                viewList.get(i).setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setAdapter(final CardAdapter adapter) {
        this.adapter = adapter;
        doBindAdapter();
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });

    }

    public CardAdapter getAdapter() {
        return adapter;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        allWidth = getMeasuredWidth();
        allHeight = getMeasuredHeight();
        Log.d(TAG, "allWidth::" + allWidth + ",allHeight::" + allHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View viewItem = viewList.get(i);
            // 1. 先layout出来
            int childHeight = viewItem.getMeasuredHeight();
            int viewLeft = (getWidth() - viewItem.getMeasuredWidth()) / 2;
            viewItem.layout(viewLeft, itemMarginTop, viewLeft + viewItem.getMeasuredWidth(),
                    itemMarginTop + childHeight);

            // 2. 调整位置
            int offset = yOffsetStep * i;
            float scale = 1 - SCALE_STEP * i;
            Log.d(TAG, "i::" + i + ",scale::" + scale);
            viewItem.offsetTopAndBottom(offset);

            // 3. 调整缩放、重心等
            viewItem.setPivotY(viewItem.getMeasuredHeight());
            viewItem.setPivotX(viewItem.getMeasuredWidth() / 2);
            viewItem.setScaleX(scale);
            viewItem.setScaleY(scale);

        }
    }


    class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return false;
        }
    }

}
