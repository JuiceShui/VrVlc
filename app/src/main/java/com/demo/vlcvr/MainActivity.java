package com.demo.vlcvr;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener /*implements ScaleGestureDetector.OnScaleGestureListener*/ {
    public static final int TOGGLE_OVERLAY = 10;//显示隐藏工具
    public static final int CHANGE_POINTER = 11;//用户转动指南针
    private float rotatedAngle = 0;//指南针转动
    private float rotatedAngleVertical = 0;//高度仪转动
    private float rotateAngleHorizontal = 0;
    private SurfaceView surfaceView;
    private SouthPointer sp;
    private HorizontalPointer hp;
    private VideoPlayer videoPlayer;
    private VerticalPointer vp;
    private LinearLayout optionContainer;
    private LinearLayout vrContainer;
    private LinearLayout optionTab;
    private ImageView ivOption;
    private ImageView ivAngleReset;
    private ImageView ivRollLeft;
    private ImageView ivRollRight;
    private AppCompatButton btnChange;
    //private String url = "/sdcard/Download/vr3601.mp4";
    private String url = "rtsp://192.168.2.56/ff_test/123";
    private String url2 = "rtsp://192.168.2.56/ff_test/124";
    private boolean isUrl1 = true;
    private TouchEventDelegate delegate;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (videoPlayer == null)
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
        videoPlayer = new VideoPlayer();
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                videoPlayer.init(surfaceView, isUrl1);
                setUrl();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                if (videoPlayer != null) {
                    if (videoPlayer.getMediaPlayer() != null) {
                        videoPlayer.getMediaPlayer().getVLCVout().setWindowSize(i1, i2);
                        videoPlayer.getMediaPlayer().setAspectRatio(i1 + ":" + i2);
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (videoPlayer != null) {
                    videoPlayer.onPause();
                }
            }
        });
        delegate = new TouchEventDelegate(videoPlayer, this);
        initListener();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return delegate.onTouchEvent(event);
    }

    private void setUrl() {
        // this.url = this.url.replace(".61", ".61:1935");
        if (videoPlayer == null) {
            videoPlayer = new VideoPlayer();
        }
        videoPlayer.startPlay(isUrl1 ? this.url : this.url2);
        Observable.timer(5, TimeUnit.SECONDS)
                .observeOn(Schedulers.single())
                .subscribeOn(Schedulers.single())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        //   change();
                    }
                });
    }

    private void changeSouthPointer() {

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

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_option:
                optionTab.setVisibility(optionTab.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                break;
            case R.id.iv_angle_reset:
                rotatedAngle = 0;
                rotatedAngleVertical = 0;
                rotateAngleHorizontal = 0;
                delegate.resetAngle();
                break;
            case R.id.iv_roll_left:
                delegate.rollAngle(-5);
                break;
            case R.id.iv_roll_right:
                delegate.rollAngle(5);
                break;
            case R.id.btn_camera_change:
                isUrl1 = !isUrl1;
                videoPlayer.stopPlay();
                videoPlayer.destroy();
                videoPlayer.init(surfaceView, isUrl1);
                setUrl();
                break;
        }
    }

    public void toggleOverlay() {
        if (vrContainer.getVisibility() == View.VISIBLE) {
            optionContainer.setVisibility(View.GONE);
            vrContainer.setVisibility(View.GONE);
            optionTab.setVisibility(View.GONE);
        } else {
            optionContainer.setVisibility(View.VISIBLE);
            vrContainer.setVisibility(View.VISIBLE);
        }
    }
}
