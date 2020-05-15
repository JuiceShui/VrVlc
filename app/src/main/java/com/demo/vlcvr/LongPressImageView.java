package com.demo.vlcvr;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;


import androidx.appcompat.widget.AppCompatImageView;

import java.lang.ref.WeakReference;

public class LongPressImageView extends AppCompatImageView {
    /**
     * 间隔时间（ms）
     */
    private long intervalTime = 50;
    private MyHandler handler;

    public LongPressImageView(Context context) {
        super(context);
        init();
    }

    public LongPressImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LongPressImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化监听
     */
    private void init() {
        handler = new MyHandler(this);
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new Thread(new LongClickThread()).start();
                return true;
            }
        });
    }

    /**
     * 长按时，该线程将会启动
     */
    private class LongClickThread implements Runnable {
        private int num;

        @Override
        public void run() {
            while (LongPressImageView.this.isPressed()) {
                num++;
                if (num % 5 == 0) {
                    handler.sendEmptyMessage(1);
                }
                SystemClock.sleep(intervalTime / 5);
            }
        }
    }

    /**
     * 通过handler，使监听的事件响应在主线程中进行
     */
    private static class MyHandler extends Handler {
        private WeakReference<LongPressImageView> ref;

        MyHandler(LongPressImageView view) {
            ref = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LongPressImageView view = ref.get();
            if (view != null) {
                //直接调用普通点击事件
                view.performClick();
            }
        }
    }

    public void setIntervalTime(long intervalTime) {
        this.intervalTime = intervalTime;
    }
}
