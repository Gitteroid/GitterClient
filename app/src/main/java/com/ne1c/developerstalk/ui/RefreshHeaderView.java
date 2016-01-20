package com.ne1c.developerstalk.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aspsine.swipetoloadlayout.SwipeRefreshHeaderLayout;
import com.aspsine.swipetoloadlayout.SwipeRefreshTrigger;
import com.aspsine.swipetoloadlayout.SwipeTrigger;
import com.ne1c.developerstalk.R;

public class RefreshHeaderView extends SwipeRefreshHeaderLayout implements SwipeRefreshTrigger, SwipeTrigger {
    private ImageView mArrowImageView;
    private ImageView mSuccessImageView;
    private TextView mRefreshTextView;
    private ProgressBar mProgressBar;

    private int mHeaderHeight;

    private Animation rotateUp;
    private Animation rotateDown;

    private boolean rotated = false;

    public RefreshHeaderView(Context context) {
        super(context, null);
    }

    public RefreshHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);

        mHeaderHeight = getResources().getDimensionPixelOffset(R.dimen.refresh_header_height);
        rotateUp = AnimationUtils.loadAnimation(context, R.anim.rotate_up);
        rotateDown = AnimationUtils.loadAnimation(context, R.anim.rotate_down);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mRefreshTextView = (TextView) findViewById(R.id.refresh_textView);
        mArrowImageView = (ImageView) findViewById(R.id.arrow_imageView);
        mSuccessImageView = (ImageView) findViewById(R.id.success_imageView);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_header);
    }

    @Override
    public void onRefresh() {
        mSuccessImageView.setVisibility(GONE);
        mArrowImageView.clearAnimation();
        mArrowImageView.setVisibility(GONE);
        mProgressBar.setVisibility(VISIBLE);
        mRefreshTextView.setText("REFRESHING");
    }

    @Override
    public void onPrepare() {
    }

    @Override
    public void onSwipe(int y, boolean isComplete) {
        if (!isComplete) {
            mArrowImageView.setVisibility(VISIBLE);
            mProgressBar.setVisibility(GONE);
            mSuccessImageView.setVisibility(GONE);
            if (y > mHeaderHeight) {
                mRefreshTextView.setText("RELEASE TO REFRESH");
                if (!rotated) {
                    mArrowImageView.clearAnimation();
                    mArrowImageView.startAnimation(rotateUp);
                    rotated = true;
                }
            } else if (y < mHeaderHeight) {
                if (rotated) {
                    mArrowImageView.clearAnimation();
                    mArrowImageView.startAnimation(rotateDown);
                    rotated = false;
                }

                mRefreshTextView.setText("SWIPE TO REFRESH");
            }
        }
    }

    @Override
    public void onRelease() {

    }

    @Override
    public void complete() {
        rotated = false;
        mSuccessImageView.setVisibility(VISIBLE);
        mArrowImageView.clearAnimation();
        mArrowImageView.setVisibility(GONE);
        mProgressBar.setVisibility(GONE);
        mRefreshTextView.setText(R.string.complete);
    }

    @Override
    public void onReset() {

    }
}
