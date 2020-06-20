package com.demo.vlcvr;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected Context mContext;
    protected Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = this;
        this.mActivity = this;
        setContentView(getLayout());
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void setBack(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 获取布局
     *
     * @return
     */
    protected abstract int getLayout();

    /**
     * 处理事件数据
     */
    protected abstract void init();

    protected void onRxCall(int code, Bundle bundle) {
    }
}
