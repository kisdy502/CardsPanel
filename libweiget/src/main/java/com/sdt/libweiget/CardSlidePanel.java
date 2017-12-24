package com.sdt.libweiget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * 0
 * 1
 * 2
 * 3
 * Created by Administrator on 2017/12/21.
 */

public class CardSlidePanel extends ViewGroup {

    final static String TAG = "CardSlidePanel";

    private List<CardItemView> viewList = new ArrayList<>(); // 存放的是每一层的view，从顶到底
    private List<View> releasedViewList = new ArrayList<>(); // 手指松开后存放的view列表

    /* 拖拽工具类 */
    private final ViewDragHelper mDragHelper; // 这个跟原生的ViewDragHelper差不多，我仅仅只是修改了Interpolator
    private int initViewX = 0, initViewY = 0; // 最初时，中间View的x位置,y位置
    private int panelWidth = 0; // 面板的宽度
    private int panelHeight = 0; // 面板的高度
    private int childWith = 0; // 每一个子View对应的宽度

    private static final float SCALE_STEP = 0.08f; // view叠加缩放的步长
    private static final int MAX_SLIDE_DISTANCE_LINKAGE = 500; // 水平距离+垂直距离
    private static final float XYRATE = 3f;  //x方向和y方向的速度比例关系

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

        moveDetector = new GestureDetectorCompat(context,
                new MoveDetector());
        moveDetector.setIsLongpressEnabled(false);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getChildCount() != VIEW_COUNT) {
                    doBindAdapter();
                }
            }
        });
    }

    /**
     * 数据绑定
     */
    private void doBindAdapter() {
        if (adapter == null || panelWidth <= 0 || panelHeight <= 0) {
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

    private void orderViewStack() {

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        panelWidth = getMeasuredWidth();
        panelHeight = getMeasuredHeight();
        Log.d(TAG, "panelWidth::" + panelWidth + ",panelHeight::" + panelHeight);
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
            Log.d(TAG, "i::" + i + ",scale::" + scale + ",offset::" + offset);
            viewItem.offsetTopAndBottom(offset);

            // 3. 调整缩放、重心等
            viewItem.setPivotY(viewItem.getMeasuredHeight());
            viewItem.setPivotX(viewItem.getMeasuredWidth() / 2);
            viewItem.setScaleX(scale);
            viewItem.setScaleY(scale);

            Log.d(TAG, "i::" + i + ",getLeft::" + viewItem.getLeft() + ",getTop::" + viewItem.getTop());

        }

        if (childCount > 0) {
            // 初始化一些中间参数
            initViewX = viewList.get(0).getLeft();
            initViewY = viewList.get(0).getTop();
            childWith = viewList.get(0).getMeasuredWidth();
            Log.d(TAG, "initViewX::" + initViewX + ",initViewY::" + initViewY + ",childWith::" + childWith);
        }
    }

    /**
     * 分发事件 记录下首次点击的位置
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent");
        int action = ev.getActionMasked();
        // 按下时保存坐标信息
        if (action == MotionEvent.ACTION_DOWN) {
            this.downPoint.x = (int) ev.getX();
            this.downPoint.y = (int) ev.getY();
            Log.d(TAG, "downPoint.x::" + downPoint.x + ",downPoint.y::" + downPoint.y);
        }
        return super.dispatchTouchEvent(ev);
    }

    /* touch事件的拦截与处理都交给mDraghelper来处理 */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent");
        boolean shouldIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        boolean moveFlag = moveDetector.onTouchEvent(ev);
        Log.i(TAG, "shouldIntercept::" + shouldIntercept);
        Log.i(TAG, "moveFlag::" + moveFlag);
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // ACTION_DOWN的时候就对view重新排序
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING) {
                mDragHelper.abort();
            }
            orderViewStack();
            // 保存初次按下时arrowFlagView的Y坐标
            // action_down时就让mDragHelper开始工作，否则有时候导致异常
            mDragHelper.processTouchEvent(ev);
        }
        return shouldIntercept && moveFlag;
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        Log.d(TAG, "onTouchEvent");
        // 统一交给mDragHelper处理，由DragHelperCallback实现拖动效果
        // 该行代码可能会抛异常，正式发布时请将这行代码加上try catch
        mDragHelper.processTouchEvent(e);
        return true;
    }


    class MoveDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            Log.d(TAG, "onScroll");
            // 拖动了，touch不往下传递
            boolean isMove = Math.abs(dy) + Math.abs(dx) > mTouchSlop;
            Log.d(TAG, "isMove::" + isMove);
            return isMove;
        }
    }

    class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (adapter == null || adapter.getCount() == 0 || getChildCount() == 0 || child.getScaleX() < 1) {
                return false;
            }

            // 2. 获取可滑动区域
            ((CardItemView) child).onStartDragging();
            if (draggableArea == null) {
                draggableArea = adapter.obtainDraggableArea(child);
            }
            return true;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            // 这个用来控制拖拽过程中松手后，自动滑行的速度
            return 256;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            animToSide((CardItemView) releasedChild, (int) xvel, (int) yvel);
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }
    }

    /**
     * 松开手之后依靠初速度自己运动
     *
     * @param changedView
     * @param xvel        x方向的初速度
     * @param yvel        y方向的初速度
     */
    private void animToSide(CardItemView changedView, int xvel, int yvel) {
        int finalX = initViewX;
        int finalY = initViewY;
        int flyType = -1;
        Log.d(TAG, "fx,fy::" + finalX + "," + finalY);
        //松开手指时,已经移动的距离
        int dx = changedView.getLeft() - initViewX;
        int dy = changedView.getTop() - initViewY;
        Log.d(TAG, "dx,dy::" + dx + "," + dy);
        if (xvel > MAX_SLIDE_DISTANCE_LINKAGE && Math.abs(yvel) < xvel * XYRATE) {
            //x方向速度足够够大
            finalX = panelWidth;
            finalY = yvel * (panelWidth - dx) / xvel;
            int testY = yvel * (childWith + changedView.getLeft()) / xvel + changedView.getTop();
            Log.d(TAG, "finalY,testY::" + finalY + "," + testY);
            flyType = VANISH_TYPE_RIGHT;
        }

        // 如果斜率太高，就折中处理
        if (finalY > panelHeight) {
            finalY = panelHeight;
        } else if (finalY < -panelHeight / 2) {
            finalY = -panelHeight / 2;
        }

        // 如果没有飞向两侧，而是回到了中间，需要谨慎处理
        if (finalX == initViewX) {
            changedView.animTo(initViewX, initViewY);
        } else {
            // 2. 向两边消失的动画
            releasedViewList.add(changedView);
            if (mDragHelper.smoothSlideViewTo(changedView, finalX, finalY)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    public void onViewPosChanged(CardItemView cardItemView) {

    }


}
