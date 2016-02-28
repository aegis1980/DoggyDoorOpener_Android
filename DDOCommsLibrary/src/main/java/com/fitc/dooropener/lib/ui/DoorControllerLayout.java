package com.fitc.dooropener.lib.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.view.View;
import android.widget.SeekBar;

import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.R;

/**
 * Created by Jon on 30/11/2015.
 */
public class DoorControllerLayout extends RelativeLayout {

    private LayoutInflater mInflater;
    private View mView;

    private SeekBar mDoorOpenCloseSeekbar;
    private Button mDoorOpenButton, mDoorCloseButton;

    public DoorControllerLayout(Context context) {
        super(context);
        init(context);
    }

    public DoorControllerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DoorControllerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DoorControllerLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        mInflater = LayoutInflater.from(context);
        mView = mInflater.inflate(R.layout.door_controller_layout, this, true);

        mDoorOpenCloseSeekbar = (SeekBar) findViewById(R.id.seekBar);

        // This prevents user interation with the seekBar.
        mDoorOpenCloseSeekbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mDoorOpenButton = (Button) findViewById(R.id.openButton);

        mDoorCloseButton = (Button) findViewById(R.id.closeButton);
    }

    public void startCloseDoorAnim(){
        SeekBarAnimation anim = new SeekBarAnimation(mDoorOpenCloseSeekbar, mDoorOpenCloseSeekbar.getProgress(), mDoorOpenCloseSeekbar.getMax());
        float p = (mDoorOpenCloseSeekbar.getMax()-mDoorOpenCloseSeekbar.getProgress())/ (mDoorOpenCloseSeekbar.getMax());
        anim.setDuration((long)(CommonApplication.DOOR_ANIM_LENGTH * p));
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDoorOpenButton.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mDoorOpenCloseSeekbar.clearAnimation();
        mDoorOpenCloseSeekbar.startAnimation(anim);
        mDoorCloseButton.setEnabled(false);
        mDoorOpenButton.setEnabled(false);
    }

    public void startOpenDoorAnim(){
        SeekBarAnimation anim = new SeekBarAnimation(mDoorOpenCloseSeekbar, mDoorOpenCloseSeekbar.getProgress(), 0);
        float p = (mDoorOpenCloseSeekbar.getProgress())/ (mDoorOpenCloseSeekbar.getMax());
        anim.setDuration((long)(CommonApplication.DOOR_ANIM_LENGTH * p));
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDoorCloseButton.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mDoorOpenCloseSeekbar.clearAnimation();
        mDoorOpenCloseSeekbar.startAnimation(anim);
        mDoorCloseButton.setEnabled(false);
        mDoorOpenButton.setEnabled(false);
    }
}
