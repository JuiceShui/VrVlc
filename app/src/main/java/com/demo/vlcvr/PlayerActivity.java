package com.demo.vlcvr;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

public class PlayerActivity extends VLCVideoActivity implements View.OnClickListener, SensorEventHandler.SensorHandlerCallback {
    public static final int TOGGLE_OVERLAY = 10;//显示隐藏工具
    public static final int CHANGE_POINTER = 11;//用户转动指南针
    private float rotatedAngle = 0;//指南针转动
    private float rotatedAngleVertical = 0;//高度仪转动
    private float rotateAngleHorizontal = 0;
    private SurfaceView surfaceView;
    private SouthPointer sp;
    private HorizontalPointer hp;
    private VerticalPointer vp;
    private LinearLayout optionContainer;
    private LinearLayout vrContainer;
    private LinearLayout optionTab;
    private ImageView ivOption;
    private ImageView ivAngleReset;
    private ImageView ivRollLeft;
    private ImageView ivRollRight;
    private ImageView ivMotionMode;
    private Surface surface;
    private AppCompatButton btnChange;
    private String url2 = "/sdcard/Download/vr3601.mp4";
    private String url = "rtsp://192.168.2.56/ff_test/123";
    //private String url2 = "rtsp://192.168.2.56/ff_test/124";
    private boolean isVr = false;
    private TouchEventDelegate delegate;
    private boolean isTouchMode = false;
    private SensorEventHandler sensorEventHandler;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mMediaPlayer == null)
                return true;
            switch (msg.what) {
                case CHANGE_POINTER:
                    Bundle bundle = msg.getData();

                   /* float yaw = (float) msg.obj;
                    rotatedAngle = rotatedAngle + yaw;
                    sp.changeDegree(0, 180, rotatedAngle);*/
                    float yaw = bundle.getFloat("yaw");
                    float pitch = bundle.getFloat("pitch");
                    float roll = bundle.getFloat("roll");
                    rotatedAngle = rotatedAngle + yaw;
                    rotateAngleHorizontal = rotateAngleHorizontal + roll;
                    rotatedAngleVertical = rotatedAngleVertical + pitch;
                    sp.changeDegree(0, 180, rotatedAngle);
                    vp.changeDegreeBoard(rotatedAngleVertical);
                    hp.changeDegreeBoard(rotateAngleHorizontal);
                    break;
                case TOGGLE_OVERLAY:
                    toggleOverlay();
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.setStatusBarFullTransparent(this);
        surfaceView = findViewById(R.id.surface);
        sp = findViewById(R.id.sp);
        vp = findViewById(R.id.vp);
        hp = findViewById(R.id.hp);
        btnChange = findViewById(R.id.btn_camera_change);
        ivRollLeft = findViewById(R.id.iv_roll_left);
        ivRollRight = findViewById(R.id.iv_roll_right);
        ivAngleReset = findViewById(R.id.iv_angle_reset);
        optionContainer = findViewById(R.id.ll_option_container);
        vrContainer = findViewById(R.id.ll_vr_container);
        optionTab = findViewById(R.id.ll_option_tab);
        ivOption = findViewById(R.id.iv_option);
        ivMotionMode = findViewById(R.id.iv_mode_change);
        mUri = Uri.parse(isVr ? url : url2);
        initListener();
        changeToSensorMode();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                PlayerActivity.this.surface = holder.getSurface();
                setVr(isVr);
                createPlayer(mUri);
                if (surface != null) {
                    vlcSetSurface(surface);
                }
                delegate = new TouchEventDelegate(mMediaPlayer, PlayerActivity.this);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchMode) {
            return super.onTouchEvent(event);
        }
        return delegate.onTouchEvent(event);
    }

    public Handler getHandler() {
        return mHandler;
    }

    private void initListener() {
        ivOption.setOnClickListener(this);
        ivAngleReset.setOnClickListener(this);
        ivRollRight.setOnClickListener(this);
        ivRollLeft.setOnClickListener(this);
        btnChange.setOnClickListener(this);
        ivMotionMode.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_option:
                optionTab.setVisibility(optionTab.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                break;
            case R.id.iv_angle_reset:
                if (!isTouchMode) {
                    return;
                }
                rotatedAngle = 0;
                rotatedAngleVertical = 0;
                rotateAngleHorizontal = 0;
                delegate.resetAngle();
                break;
            case R.id.iv_roll_left:
                if (!isTouchMode) {
                    return;
                }
                delegate.rollAngle(-5);
                break;
            case R.id.iv_roll_right:
                if (!isTouchMode) {
                    return;
                }
                delegate.rollAngle(5);
                break;
            case R.id.btn_camera_change:
                isVr = !isVr;
                mUri = Uri.parse(isVr ? url : url2);
                setVr(isVr);
                createPlayer(mUri);
                vlcSetSurface(surface);
                rotatedAngle = 0;
                rotatedAngleVertical = 0;
                rotateAngleHorizontal = 0;
                if (isTouchMode) {
                    delegate = null;
                    delegate = new TouchEventDelegate(mMediaPlayer, PlayerActivity.this);
                } else {
                    sensorEventHandler = null;
                    mMediaPlayer.updateViewpoint(0, 0, 0, TouchEventDelegate.DEFAULT_FOV, true);
                    changeToSensorMode();
                }
                break;
            case R.id.iv_mode_change:
                isTouchMode = !isTouchMode;
                ivMotionMode.setImageResource(isTouchMode ? R.mipmap.ic_motion_mode : R.mipmap.ic_touch_mode);
                resetInitValue();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorEventHandler != null) {
            sensorEventHandler.releaseResources();
        }
    }

    public void toggleOverlay() {
        if (vrContainer.getVisibility() == View.VISIBLE) {
            optionContainer.setVisibility(View.GONE);
            vrContainer.setVisibility(View.GONE);
            optionTab.setVisibility(View.GONE);
            ivAngleReset.setVisibility(View.GONE);
        } else {
            optionContainer.setVisibility(View.VISIBLE);
            vrContainer.setVisibility(View.VISIBLE);
            ivAngleReset.setVisibility(View.VISIBLE);
        }
    }

    private void changeToSensorMode() {
        sensorEventHandler = new SensorEventHandler(mActivity);
        sensorEventHandler.init();
        sensorEventHandler.setSensorHandlerCallback(this);
    }

    @Override
    public void onDegreeChange(float[] degree) {
        mMediaPlayer.updateViewpoint(degree[0], degree[1], degree[2], 0, false);
        Log.e("DDDD", degree[0] + "  " + degree[1] + "    " + degree[2]);
    }

    private void resetInitValue() {
        rotatedAngle = 0;
        rotatedAngleVertical = 0;
        rotateAngleHorizontal = 0;
        if (isTouchMode) {
            if (sensorEventHandler != null) {
                sensorEventHandler.removeCallback();
                sensorEventHandler.releaseResources();
                sensorEventHandler = null;
            }
            if (delegate != null) {
                delegate.resetAngle();
            } else {
                delegate = new TouchEventDelegate(mMediaPlayer, PlayerActivity.this);
            }
        } else {
            if (sensorEventHandler != null) {
                sensorEventHandler = null;
            }
            mMediaPlayer.updateViewpoint(0, 0, 0, TouchEventDelegate.DEFAULT_FOV, true);
            changeToSensorMode();
        }
    }
}
