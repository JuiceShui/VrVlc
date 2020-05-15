package com.demo.vlcvr;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.core.view.GestureDetectorCompat;

import org.videolan.libvlc.IVLCVout;

public class TouchEventDelegate implements ScaleGestureDetector.OnScaleGestureListener {
    private VideoPlayer videoPlayer;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mDetector = null;
    private float mFov = 80f;
    private float DEFAULT_FOV = 80f;
    private static final float MIN_FOV = 20f;
    private static final float MAX_FOV = 150f;
    private int mSurfaceYDisplayRange, mSurfaceXDisplayRange;
    DisplayMetrics mScreen = new DisplayMetrics();
    private float mInitTouchY, mTouchY = -1f, mTouchX = -1f;
    //Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_MOVE = 3;
    private static final int TOUCH_SEEK = 4;
    private int mTouchAction = TOUCH_NONE;
    private MainActivity activity;

    public TouchEventDelegate(VideoPlayer videoPlayer, MainActivity activity) {
        this.videoPlayer = videoPlayer;
        this.activity = activity;
        activity.getWindowManager().getDefaultDisplay().getMetrics(mScreen);
        mSurfaceYDisplayRange = Math.min(mScreen.widthPixels, mScreen.heightPixels);
        mSurfaceXDisplayRange = Math.max(mScreen.widthPixels, mScreen.heightPixels);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (videoPlayer.getMediaPlayer() == null)
            return false;
        if (mDetector == null) {
            mDetector = new GestureDetectorCompat(activity, mGestureListener);
            mDetector.setOnDoubleTapListener(mGestureListener);
        }
        if (mFov != 0f && mScaleGestureDetector == null)
            mScaleGestureDetector = new ScaleGestureDetector(activity, this);
        /*if (mPlaybackSetting != DelayState.OFF) {
            if (event.getAction() == MotionEvent.ACTION_UP)
                endPlaybackSetting();
            return true;
        } else if (mPlaylist.getVisibility() == View.VISIBLE) {
            togglePlaylist();
            return true;
        }
        if (mTouchControls == 0 || mIsLocked) {
            // locked or swipe disabled, only handle show/hide & ignore all actions
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!mShowing) {
                    showOverlay();
                } else {
                    hideOverlay(true);
                }
            }
            return false;
        }*/
        if (mFov != 0f && mScaleGestureDetector != null)
            mScaleGestureDetector.onTouchEvent(event);
        if ((mScaleGestureDetector != null && mScaleGestureDetector.isInProgress()) ||
                (mDetector != null && mDetector.onTouchEvent(event)))
            return true;

        final float x_changed = mTouchX != -1f && mTouchY != -1f ? event.getRawX() - mTouchX : 0f;
        final float y_changed = x_changed != 0f ? event.getRawY() - mTouchY : 0f;

        // coef is the gradient's move to determine a neutral zone
        final float coef = Math.abs(y_changed / x_changed);
        final float xgesturesize = ((x_changed / mScreen.xdpi) * 2.54f);
        final float delta_y = Math.max(1f, (Math.abs(mInitTouchY - event.getRawY()) / mScreen.xdpi + 0.5f) * 2f);

        final int xTouch = Math.round(event.getRawX());
        final int yTouch = Math.round(event.getRawY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Audio
                mTouchY = mInitTouchY = event.getRawY();
               /* if (mService.getVolume() <= 100) {
                    mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mOriginalVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                } else {
                    mVol = ((float) mService.getVolume()) * mAudioMax / 100;
                }*/
                mTouchAction = TOUCH_NONE;
                // Seek
                mTouchX = event.getRawX();
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_DOWN, 0, xTouch, yTouch);
                break;
            case MotionEvent.ACTION_MOVE:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_MOVE, 0, xTouch, yTouch);

                if (mFov == 0f) {
                    // No volume/brightness action if coef < 2 or a secondary display is connected
                    //TODO : Volume action when a secondary display is connected
                    if (mTouchAction != TOUCH_SEEK && coef > 2 /*&& mPresentation == null*/) {
                        if (Math.abs(y_changed / mSurfaceYDisplayRange) < 0.05)
                            return false;
                        mTouchY = event.getRawY();
                        mTouchX = event.getRawX();
                        //  doVerticalTouchAction(y_changed);
                    } else {
                        // Seek (Right or Left move)
                        // doSeekTouch(Math.round(delta_y), mIsRtl ? -xgesturesize : xgesturesize, false);
                    }
                } else {
                    mTouchY = event.getRawY();
                    mTouchX = event.getRawX();
                    mTouchAction = TOUCH_MOVE;
                    final float yaw = mFov * -x_changed / (float) mSurfaceXDisplayRange;
                    final float pitch = mFov * -y_changed / (float) mSurfaceXDisplayRange;
                    Log.e("Tag yaw pitch", yaw + "  " + pitch);
                    Message message = new Message();
                    message.what = MainActivity.CHANGE_POINTER;
                    Bundle bundle = new Bundle();
                    bundle.putFloat("yaw", yaw);
                    bundle.putFloat("pitch", pitch);
                    bundle.putFloat("roll", 0);
                    message.setData(bundle);
                    activity.getHandler().sendMessage(message);
                    videoPlayer.getMediaPlayer().updateViewpoint(yaw, pitch, pitch, 0, false);
                }
                break;
            case MotionEvent.ACTION_UP:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_UP, 0, xTouch, yTouch);
                // Seek
                if (mTouchAction == TOUCH_SEEK)
                    //  doSeekTouch(Math.round(delta_y), mIsRtl ? -xgesturesize : xgesturesize, true);
                    mTouchX = -1f;
                mTouchY = -1f;
                break;
        }
        return mTouchAction != TOUCH_NONE;
    }

    private void sendMouseEvent(int action, int button, int x, int y) {
        if (videoPlayer.getMediaPlayer() == null)
            return;
        final IVLCVout vlcVout = videoPlayer.getMediaPlayer().getVLCVout();
        vlcVout.sendMouseEvent(action, button, x, y);
    }

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            activity.getHandler().sendEmptyMessageDelayed(MainActivity.TOGGLE_OVERLAY, 200);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
           /* mHandler.removeMessages(HIDE_INFO);
            mHandler.removeMessages(SHOW_INFO);
            float range = mCurrentScreenOrientation == Configuration.ORIENTATION_LANDSCAPE ? mSurfaceXDisplayRange : mSurfaceYDisplayRange;
            if (mService == null)
                return false;
            if (!mIsLocked) {
                if ((mTouchControls & TOUCH_FLAG_SEEK) == 0) {
                    doPlayPause();
                    return true;
                }
                float x = e.getX();
                if (x < range/4f)
                    seekDelta(-10000);
                else if (x > range*0.75)
                    seekDelta(10000);
                else
                    doPlayPause();
                return true;
            }*/
            if (videoPlayer.getMediaPlayer().isPlaying()) {
                videoPlayer.getMediaPlayer().pause();
            } else {
                videoPlayer.getMediaPlayer().play();
            }
            return false;
        }
    };


    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float diff = DEFAULT_FOV * (1 - detector.getScaleFactor());
        if (videoPlayer.getMediaPlayer().updateViewpoint(0, 0, 0, diff, false)) {
            mFov = Math.min(Math.max(MIN_FOV, mFov + diff), MAX_FOV);
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return mSurfaceXDisplayRange != 0 && mFov != 0f;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    public void resetAngle() {
        videoPlayer.getMediaPlayer().updateViewpoint(0, 0, 0, DEFAULT_FOV, true);
        Message message = new Message();
        message.what = MainActivity.CHANGE_POINTER;
        Bundle bundle = new Bundle();
        bundle.putFloat("yaw", 0);
        bundle.putFloat("pitch", 0);
        bundle.putFloat("roll", 0);
        message.setData(bundle);
        activity.getHandler().sendMessage(message);
    }

    public void rollAngle(float roll) {
        if (videoPlayer.getMediaPlayer().updateViewpoint(0, 0, roll, 0, false)) {
            Message message = new Message();
            message.what = MainActivity.CHANGE_POINTER;
            Bundle bundle = new Bundle();
            bundle.putFloat("yaw", 0);
            bundle.putFloat("pitch", 0);
            bundle.putFloat("roll", roll);
            message.setData(bundle);
            activity.getHandler().sendMessage(message);
        }
    }
}
