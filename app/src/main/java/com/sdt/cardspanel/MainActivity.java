package com.sdt.cardspanel;

import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sdt.libweiget.CardAdapter;
import com.sdt.libweiget.CardSlidePanel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String imagePaths[] = {"file:///android_asset/wall01.jpg",
            "file:///android_asset/wall02.jpg", "file:///android_asset/wall03.jpg",
            "file:///android_asset/wall04.jpg", "file:///android_asset/wall05.jpg",
            "file:///android_asset/wall06.jpg", "file:///android_asset/wall07.jpg",
            "file:///android_asset/wall08.jpg", "file:///android_asset/wall09.jpg",
            "file:///android_asset/wall10.jpg", "file:///android_asset/wall11.jpg",
            "file:///android_asset/wall12.jpg"}; // 12个图片资源

    private String names[] = {"AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF", "GGGG",
            "HHHH", "IIII", "JJJJ", "KKKK", "LLLL"}; // 12个人名

    private List<CardDataItem> dataList = new ArrayList<>();


    CardSlidePanel slidePanel;
    CardAdapter cardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        slidePanel = (CardSlidePanel) findViewById(R.id.image_slide_panel);

        init();
    }


    private void init() {
        prepareDataList();
        cardAdapter = new TCardAdapter();
        slidePanel.setAdapter(cardAdapter);
    }

    class TCardAdapter extends CardAdapter {

        @Override
        public int getLayoutId() {
            return R.layout.card_item;
        }

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public void bindView(View view, int index) {
            Object tag = view.getTag();
            ViewHolder viewHolder;
            if (null != tag) {
                viewHolder = (ViewHolder) tag;
            } else {
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            }

            viewHolder.bindData(dataList.get(index));
        }

        @Override
        public Object getItem(int index) {
            return dataList.get(index);
        }

        @Override
        public Rect obtainDraggableArea(View view) {
            View contentView = view.findViewById(R.id.card_item_content);
            View topLayout = view.findViewById(R.id.card_top_layout);
            View bottomLayout = view.findViewById(R.id.card_bottom_layout);
            int left = view.getLeft() + contentView.getPaddingLeft() + topLayout.getPaddingLeft();
            int right = view.getRight() - contentView.getPaddingRight() - topLayout.getPaddingRight();
            int top = view.getTop() + contentView.getPaddingTop() + topLayout.getPaddingTop();
            int bottom = view.getBottom() - contentView.getPaddingBottom() - bottomLayout.getPaddingBottom();
            Log.d("mainActivity", "l.r.t.b:" + left + "," + top + "," + right + "," + bottom);
            return new Rect(left, top, right, bottom);
        }
    }


    private void prepareDataList() {
        for (int i = 0; i < 6; i++) {
            CardDataItem dataItem = new CardDataItem();
            dataItem.userName = names[i];
            dataItem.imagePath = imagePaths[i];
            dataItem.likeNum = (int) (Math.random() * 10);
            dataItem.imageNum = (int) (Math.random() * 6);
            dataList.add(dataItem);
        }
    }

    private void appendDataList() {
        for (int i = 0; i < 6; i++) {
            CardDataItem dataItem = new CardDataItem();
            dataItem.userName = "From Append";
            dataItem.imagePath = imagePaths[8];
            dataItem.likeNum = (int) (Math.random() * 10);
            dataItem.imageNum = (int) (Math.random() * 6);
            dataList.add(dataItem);
        }
    }

    class ViewHolder {

        ImageView imageView;
        View maskView;
        TextView userNameTv;
        TextView imageNumTv;
        TextView likeNumTv;

        public ViewHolder(View view) {
            imageView = (ImageView) view.findViewById(R.id.card_image_view);
            maskView = view.findViewById(R.id.maskView);
            userNameTv = (TextView) view.findViewById(R.id.card_user_name);
            imageNumTv = (TextView) view.findViewById(R.id.card_pic_num);
            likeNumTv = (TextView) view.findViewById(R.id.card_like);
        }

        public void bindData(CardDataItem itemData) {
            userNameTv.setText(itemData.userName);
            imageNumTv.setText(itemData.imageNum + "");
            likeNumTv.setText(itemData.likeNum + "");
            Glide.with(MainActivity.this).load(itemData.imagePath).into(imageView);
        }
    }


}
