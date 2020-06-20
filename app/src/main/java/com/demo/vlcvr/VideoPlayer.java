package com.demo.vlcvr;

import android.net.Uri;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static org.videolan.libvlc.Media.State.Opening;
import static org.videolan.libvlc.MediaList.Event.EndReached;
import static org.videolan.libvlc.MediaPlayer.Event.Buffering;
import static org.videolan.libvlc.MediaPlayer.Event.ESAdded;
import static org.videolan.libvlc.MediaPlayer.Event.ESDeleted;
import static org.videolan.libvlc.MediaPlayer.Event.ESSelected;
import static org.videolan.libvlc.MediaPlayer.Event.EncounteredError;
import static org.videolan.libvlc.MediaPlayer.Event.MediaChanged;
import static org.videolan.libvlc.MediaPlayer.Event.PausableChanged;
import static org.videolan.libvlc.MediaPlayer.Event.Paused;
import static org.videolan.libvlc.MediaPlayer.Event.Playing;
import static org.videolan.libvlc.MediaPlayer.Event.PositionChanged;
import static org.videolan.libvlc.MediaPlayer.Event.SeekableChanged;
import static org.videolan.libvlc.MediaPlayer.Event.Stopped;
import static org.videolan.libvlc.MediaPlayer.Event.TimeChanged;
import static org.videolan.libvlc.MediaPlayer.Event.Vout;

public class VideoPlayer implements IVLCVout.Callback {
    private MediaPlayer mediaPlayer;
    private LibVLC libVLC;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private String audioSource;
    private Disposable mRetryDisposable;
    private boolean isPlayingCalled = false;
    private boolean isErr = false;
    private boolean isRePlay = false;
    private int voutCount = 0;
    private int rePlayVoutCount = 5;//连续vout多少次就重启
    private boolean isHasPlayed;//播放过
    private long lastStop = 0;
    private long stopRetryDelay = 1000;
    private int STATE_PLAY = 1, STATE_STOP = 2, STATE_BOTH = 3;
    private Disposable mPlayDisposable;
    private Disposable mStartPlayDisposable;
    private Media media;
    private TextureView mTexture;
    private boolean isVr = true;

    public void startPlay(String source) {
        this.audioSource = source;
        mPlayDisposable = Observable.just("1")
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        if (libVLC == null) {
                            mStartPlayDisposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                                    .observeOn(Schedulers.single())
                                    .subscribe(new Consumer<Long>() {
                                        @Override
                                        public void accept(Long aLong) throws Exception {
                                            if (mSurface != null) {
                                                init(mSurface,isVr);
                                            } else if (mTexture != null) {
                                                init(mTexture);
                                            }
                                            startPlay(audioSource);
                                            Log.e("重试", "重试");
                                        }
                                    });
                            return;
                        }
                        //TODO
                        if (audioSource.startsWith("rtsp://")) {
                            media = new Media(libVLC, Uri.parse(audioSource));//远程
                        } else {
                            media = new Media(libVLC, audioSource);//本地
                        }
                        mediaPlayer.setMedia(media);
                        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                            @Override
                            public void onEvent(MediaPlayer.Event event) {
                                Log.e("MediaPlayer", "onEvent: " + getEventName(event));
                                switch (event.type) {
                                    case EndReached:

                                        break;
                                    case EncounteredError://打开错误
                                        isErr = true;
                                        break;

                                    case Stopped:
                                        if (System.currentTimeMillis() - lastStop < stopRetryDelay) {
                                            return;
                                        }
                                        lastStop = System.currentTimeMillis();
                                        if (mRetryDisposable != null) {
                                            mRetryDisposable.dispose();
                                            mRetryDisposable = null;
                                        }
                                        mRetryDisposable = Observable.interval(500, TimeUnit.MILLISECONDS)
                                                .subscribeOn(Schedulers.newThread())
                                                .observeOn(Schedulers.single())
                                                .subscribe(new Consumer<Long>() {
                                                    @Override
                                                    public void accept(Long aLong) throws Exception {
                                                        if (mediaPlayer != null) {
                                                            try {
                                                                mediaPlayer.stop();
                                                                mediaPlayer.play();
                                                                Log.e("MediaPlayer", "mRetryDisposable");
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                });
                                        break;
                                    case Vout:
                                        if (isPlayingCalled) {
                                            // Log.e("onEvent", "isPlayingCalled");
                                            if (isErr || isRePlay) {
                                                //  Log.e("onEvent", "isErr");
                                                playOrStop(mediaPlayer, STATE_BOTH);
                                                if (isErr) {
                                                    isErr = false;
                                                }
                                                if (isRePlay) {
                                                    isRePlay = false;
                                                }
                                            } else {
                                                mediaPlayer.pause();
                                                playOrStop(mediaPlayer, STATE_PLAY);
                                                Log.e("MediaPlayer", "VoutPlay");
                                            }
                                            isPlayingCalled = false;
                                        }
                                        voutCount++;
                                        if (voutCount > rePlayVoutCount) {//如果vout次数大于20  则表示链接成功但是无画面的情况
                                            if (mSurface != null) {
                                                init(mSurface, isVr);
                                            } else {
                                                init(mTexture);
                                            }
                                            startPlay(audioSource);
                                            voutCount = 0;
                                            Log.e("MediaPlayer", "replay");
                                        }
                                        break;
                                    case Playing:
                                        isHasPlayed = true;
                                        if (mRetryDisposable != null) {
                                            mRetryDisposable.dispose();
                                            mRetryDisposable = null;
                                        }
                                        isPlayingCalled = true;
                        /*if (isErr) {
                            Log.e("onEvent", "isErr");
                            mediaPlayer.pause();
                            mediaPlayer.play();
                            isErr = false;
                        }*/
                                        voutCount = 0;
                                        break;
                                }
                            }
                        });
                        playOrStop(mediaPlayer, STATE_PLAY);
                    }
                });
    }

    public void init(TextureView textureView) {
        mTexture = textureView;
        final int width = mTexture.getWidth();
        final int height = mTexture.getHeight();
        mTexture.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (width == 0 || height == 0) {
                    if (mTexture.getWidth() != 0 && mTexture.getHeight() != 0) {
                        mediaPlayer.getVLCVout().setWindowSize(mTexture.getWidth(), mTexture.getHeight());
                        mediaPlayer.setAspectRatio(mTexture.getWidth() + ":" + mTexture.getHeight());
                    }
                }
            }
        });
        final ArrayList<String> options = new ArrayList<>();
        options.add("-vvv"); // verbosity
        options.add("--drop-late-frames");
        options.add("--skip-frames");
        options.add("--rtsp-tcp");
        options.add("--http-reconnect");
        options.add("--network-caching=300");
        Observable.just("1")
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        if (mTexture == null) {
                            return;
                        }
                        if (libVLC != null) {
                            libVLC.release();
                            libVLC = null;
                        }
                        libVLC = new LibVLC(mTexture.getContext(), options);
                        try {
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                                mediaPlayer.getVLCVout().detachViews();
                            }
                           /* if (holder == null) {
                                holder = mSurface.getHolder();
                            }*/
                            if (mediaPlayer == null) {
                                mediaPlayer = new MediaPlayer(libVLC);
                            }
                            mediaPlayer.setScale(0);
                            mediaPlayer.getVLCVout().setWindowSize(mTexture.getWidth(), mTexture.getHeight());
                            mediaPlayer.setAspectRatio(mTexture.getWidth() + ":" + mTexture.getHeight());
                            //holder.setKeepScreenOn(true);
                            mediaPlayer.getVLCVout().setVideoSurface(mTexture.getSurfaceTexture());
                            mediaPlayer.getVLCVout().addCallback(VideoPlayer.this);
                            //播放前还要调用这个方法
                            mediaPlayer.getVLCVout().attachViews();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public void init(final SurfaceView surfaceView, final boolean isVr) {
        mSurface = surfaceView;
        final int width = mSurface.getWidth();
        final int height = mSurface.getHeight();
        mSurface.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (width == 0 || height == 0) {
                    if (mSurface.getWidth() != 0 && mSurface.getHeight() != 0) {
                        mediaPlayer.getVLCVout().setWindowSize(mSurface.getWidth(), mSurface.getHeight());
                        mediaPlayer.setAspectRatio(mSurface.getWidth() + ":" + mSurface.getHeight());
                    }
                }
            }
        });
        final ArrayList<String> options = new ArrayList<>();
        options.add("-vvv"); // verbosity
        options.add("--drop-late-frames");
        options.add("--skip-frames");
        options.add("--rtsp-tcp");
        options.add("--http-reconnect");
        options.add("--network-caching=300");
        Observable.just("1")
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        if (mSurface == null) {
                            return;
                        }
                        if (libVLC != null) {
                            libVLC.release();
                            libVLC = null;
                        }
                        libVLC = new LibVLC(mSurface.getContext(), options);
                        libVLC.setUserAgent(isVr ? "forceVr" : "normal", "http");
                        try {
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                                mediaPlayer.getVLCVout().detachViews();
                            }
                            if (holder == null) {
                                holder = mSurface.getHolder();
                            }
                            if (mediaPlayer == null) {
                                mediaPlayer = new MediaPlayer(libVLC);
                            }
                            mediaPlayer.setScale(0);
                            mediaPlayer.getVLCVout().setWindowSize(surfaceView.getWidth(), surfaceView.getHeight());
                            mediaPlayer.setAspectRatio(surfaceView.getWidth() + ":" + surfaceView.getHeight());
                            holder.setKeepScreenOn(true);
                            mediaPlayer.getVLCVout().setVideoSurface(holder.getSurface(), mSurface.getHolder());
                            mediaPlayer.getVLCVout().addCallback(VideoPlayer.this);
                            //播放前还要调用这个方法
                            mediaPlayer.getVLCVout().attachViews();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public void stopPlay() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    public void destroy() {
        Observable.just("1")
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        try {
                            if (mediaPlayer != null) {
                                if (mediaPlayer.isPlaying() || isHasPlayed) {
                                    mediaPlayer.stop();
                                    mediaPlayer.release();
                                }
                                mediaPlayer.getVLCVout().detachViews();
                                mediaPlayer.getVLCVout().removeCallback(VideoPlayer.this);
                                Log.e("FRAGMENT", "Vdestroy");
                            }
                            mediaPlayer = null;
                            holder = null;
                            if (libVLC != null) {
                                libVLC.release();
                                libVLC = null;
                            }
                            if (mRetryDisposable != null) {
                                mRetryDisposable.dispose();
                                mRetryDisposable = null;
                            }
                            if (mStartPlayDisposable != null) {
                                mStartPlayDisposable.dispose();
                                mStartPlayDisposable = null;
                            }
                            if (mPlayDisposable != null) {
                                mPlayDisposable.dispose();
                                mPlayDisposable = null;
                            }
                        } catch (Exception e) {
                            Log.e("FRAGMENT", "Exception" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    private onRetryListener mListener;

    public interface onRetryListener {
        void onRetry();
    }

    public void setOnRetryListener(onRetryListener listener) {
        mListener = listener;
    }

    public void rePlay() {
        if (mediaPlayer != null) {
            isRePlay = true;
            playOrStop(mediaPlayer, STATE_BOTH);
        }
    }

    public void onPause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void onPlay() {
        if (mediaPlayer != null) {
            playOrStop(mediaPlayer, STATE_PLAY);
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }


    private void playOrStop(final MediaPlayer mediaPlayer, final int isPlay) {
        Observable.just("1")
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        Log.e("Thread", Thread.currentThread() + "  "
                                + Thread.currentThread().getId() + " : "
                                + Thread.currentThread().getName());
                        if (isPlay == STATE_PLAY) {
                            mediaPlayer.play();
                        } else if (isPlay == STATE_STOP) {
                            mediaPlayer.stop();
                        } else {//both
                            mediaPlayer.stop();
                            mediaPlayer.play();
                        }

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    public static String getEventName(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaChanged:
                return "MediaChanged";
            case Opening:
                return "Opening";
            case Buffering:
                return "Buffering";
            case Playing:
                return "Playing";
            case Paused:
                return "Paused";
            case Stopped:
                return "Stopped";
            case MediaPlayer.Event.EndReached:
                return "EndReached";
            case EncounteredError:
                return "EncounteredError";
            case TimeChanged:
                return "TimeChanged";
            case PositionChanged:
                return "PositionChanged";
            case SeekableChanged:
                return "SeekableChanged";
            case PausableChanged:
                return "PausableChanged";
            case Vout:
                return "Vout";
            case ESAdded:
                return "ESAdded";
            case ESDeleted:
                return "ESDeleted";
            case ESSelected:
                return "ESSelected";
        }
        return String.format("%02x ", event.type);
    }
}
