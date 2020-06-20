package com.demo.vlcvr;


import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.widget.Toast;


import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class VLCVideoActivity extends BaseActivity implements IVLCVout.Callback {
    public final static String TAG = "LibVLCAndroidSample/VideoActivity";
    protected Uri mUri;

    private boolean isVr = true;

    public void setVr(boolean vr) {
        isVr = vr;
    }

    // media player
    private LibVLC libvlc;
    protected MediaPlayer mMediaPlayer = null;

    /*************
     * Activity
     *************/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    protected void init() {
    }

    /*************
     * Player
     *************/

    protected void createPlayer(Uri media) {
        if (media == null) {
            return;
        }

        releasePlayer();
        try {
            if (!TextUtils.isEmpty(media.toString())) {
                Toast toast = Toast.makeText(this, media.toString(), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            /*options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            options.add("--rtsp-tcp");
            options.add("--no-drop-late-frames");
            options.add("--no-skip-frames");
            options.add("--http-reconnect");
            options.add("--network-caching=3000");*/
            options.add("-vvv"); // verbosity
            options.add("--drop-late-frames");
            options.add("--skip-frames");
            options.add("--rtsp-tcp");
            options.add("--http-reconnect");
            options.add("--network-caching=300");
            libvlc = new LibVLC(mContext, options);
            // libvlc.setOnHardwareAccelerationError(this);

            // Create media player
            libvlc.setUserAgent(isVr ? "1" : "0", "http");
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setScale(0);
            mMediaPlayer.getVLCVout().setWindowSize(ViewUtil.getScreenWidth(mActivity), ViewUtil.getSceenHeight(mActivity));
            mMediaPlayer.setEventListener(mPlayerListener);
            Media m;
            if (!media.toString().startsWith("rtsp")) {
                m = new Media(libvlc, media.toString());
            } else {
                m = new Media(libvlc, media);
            }
            m.setHWDecoderEnabled(true, true);
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();

        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    public void vlcSetSurface(Surface surface) {
        // Set up video output
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoSurface(surface, null);
        //vout.setSubtitlesView(mSurfaceSubtitles);
        vout.addCallback(this);
        vout.attachViews();
    }

    // TODO: handle this cleaner
    protected void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        libvlc.release();
        libvlc = null;
    }

    /*************
     * Events
     *************/

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);


    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<VLCVideoActivity> mOwner;

        public MyPlayerListener(VLCVideoActivity owner) {
            mOwner = new WeakReference<VLCVideoActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            VLCVideoActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    //Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    protected void onEmptyPath() {

    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }
}
