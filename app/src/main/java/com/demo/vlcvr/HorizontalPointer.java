package com.demo.vlcvr;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.demo.vlcvr.R;

import java.util.ArrayList;
import java.util.List;

public class HorizontalPointer extends View {
    private float mOuterRaidus;//最外环半径
    private float mRadius; //外圆半径
    private float mInnerRadius;//表盘半径
    private float mCenterRadius;//最内圆半径

    private float mPadding; //边距
    private float mTextSize; //文字大小

    private int mColorLong; //长线的颜色
    private int mColorShort; //短线的颜色
    private List<String> mLocations = new ArrayList<>();
    private Paint mPaint; //画笔
    private float mDegreeCenter = 0;//中间仪器旋转的角度
    private float mDegreeBoard = 0;//表盘旋转的角度

    public HorizontalPointer(Context context) {
        this(context, null);
    }

    public HorizontalPointer(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainStyledAttrs(attrs); //获取自定义的属性
        init(); //初始化画笔
    }

    private void obtainStyledAttrs(AttributeSet attrs) {
        TypedArray array = null;
        try {
            array = getContext().obtainStyledAttributes(attrs, R.styleable.HorizontalPointer);
            mPadding = array.getDimension(R.styleable.HorizontalPointer_hp_padding, DptoPx(5));
            mTextSize = array.getDimension(R.styleable.HorizontalPointer_hp_text_size, SptoPx(9));
            mColorLong = array.getColor(R.styleable.HorizontalPointer_hp_scale_long_color, Color.argb(225, 0, 0, 0));
            mColorShort = array.getColor(R.styleable.HorizontalPointer_hp_scale_short_color, Color.argb(125, 0, 0, 0));
        } catch (Exception e) {
            //一旦出现错误全部使用默认值
            mPadding = DptoPx(5);
            mTextSize = SptoPx(2);
            mColorLong = Color.argb(225, 0, 0, 0);
            mColorShort = Color.argb(125, 0, 0, 0);
        } finally {
            if (array != null) {
                array.recycle();
            }
        }
    }

    //Dp转px
    private float DptoPx(int value) {
        return SizeUtil.Dp2Px(getContext(), value);
    }

    //sp转px
    private float SptoPx(int value) {
        return SizeUtil.Sp2Px(getContext(), value);
    }

    //画笔初始化
    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = 1000; //设定一个最小值
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED || heightMeasureSpec == MeasureSpec.AT_MOST || heightMeasureSpec == MeasureSpec.UNSPECIFIED) {
            try {
                throw new NoDetermineSizeException("宽度高度至少有一个确定的值,不能同时为wrap_content");
            } catch (NoDetermineSizeException e) {
                e.printStackTrace();
            }
        } else { //至少有一个为确定值,要获取其中的最小值
            if (widthMode == MeasureSpec.EXACTLY) {
                width = Math.min(widthSize, width);
            }
            if (heightMode == MeasureSpec.EXACTLY) {
                width = Math.min(heightSize, width);
            }
        }
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mOuterRaidus = (Math.min(w, h) - getPaddingLeft() - getPaddingRight()) / 2;//最外层半径
        mRadius = mOuterRaidus * 12 / 13;//实际可用半径
        mInnerRadius = mRadius * 4 / 5;//刻度尺半径
        mCenterRadius = mRadius / 8;//内圆修饰半径
    }

    //绘制外圆背景
    public void paintCircle(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_black_mask_dark));
        canvas.drawCircle(0, 0, mOuterRaidus, mPaint);
        mPaint.setColor(Color.TRANSPARENT);
        canvas.drawCircle(0, 0, mInnerRadius, mPaint);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_black_mask));
        canvas.drawCircle(0, 0, mRadius, mPaint);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_red_mask));
        canvas.drawCircle(0, 0, mCenterRadius, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(getWidth() / 2, getHeight() / 2);
        //绘制外圆背景
        paintCircle(canvas);
        //绘制刻度
        paintArc(canvas);
        paintScale(canvas);

        canvas.restore();
    }

    //绘制刻度
    private void paintScale(Canvas canvas) {
        canvas.rotate(mDegreeBoard);
        mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1));
        int lineWidth = 0;
        for (int i = 0; i < 60; i++) {
            if (i % 5 == 0) { //整点
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
                mPaint.setColor(mColorLong);
                lineWidth = 40;
                mPaint.setTextSize(mTextSize);
                // String text = ((i / 5) == 0 ? 12 : (i / 5)) + "";
                String text = "";
                if (i < 30) {
                    text = 90 - (i * 6) + "";
                } else {
                    text = -270 + (i * 6) + "";
                }
                Rect textBound = new Rect();
                mPaint.getTextBounds(text, 0, text.length(), textBound);
                mPaint.setColor(mColorLong);
                canvas.save();
//                KLog.i((textBound.bottom - textBound.top) / 2);
                canvas.translate(0, -mRadius + mPadding + (textBound.bottom - textBound.top) / 2);
                mPaint.setStyle(Paint.Style.FILL);
                canvas.rotate(-6 * i);
                canvas.drawText(text, -(textBound.right + textBound.left) / 2, -(textBound.bottom + textBound.top) / 2, mPaint);
                if (i == 0) {
                    String tian = "天";
                    Rect tianBound = new Rect();
                    mPaint.getTextBounds(tian, 0, tian.length(), tianBound);
                    mPaint.setColor(mColorLong);
                    mPaint.setTextSize(mTextSize + 3);
                    canvas.drawText(tian, -(tianBound.right + tianBound.left) / 2,
                            -(tianBound.bottom + tianBound.top) / 2 + 2 * lineWidth + mPadding, mPaint);
                } else if (i == 30) {
                    String di = "地";
                    Rect diBound = new Rect();
                    mPaint.getTextBounds(di, 0, di.length(), diBound);
                    mPaint.setColor(mColorLong);
                    mPaint.setTextSize(mTextSize + 3);
                    canvas.drawText(di, -(diBound.right + diBound.left) / 2,
                            -(diBound.bottom + diBound.top) / 2 - 2 * lineWidth - mPadding, mPaint);
                }
                canvas.restore();
            } else { //非整点
                lineWidth = 30;
                mPaint.setColor(mColorShort);
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1));
            }
            canvas.drawLine(0, -mInnerRadius + mPadding, 0, -mInnerRadius + mPadding + lineWidth, mPaint);
            canvas.rotate(6);
        }
    }

    private void paintArc(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_red_mask));
        canvas.save();
        canvas.translate(-mRadius / 2, -mRadius / 2);
        RectF oval = new RectF(0, 0, mRadius, mRadius);
        canvas.drawArc(oval, mDegreeCenter, 180, true, mPaint);//画圆弧，这个时候，绘制没有经过圆心
        canvas.restore();
    }

    public void changeDegreeCenter(float degree) {
        if (this.mDegreeCenter == degree) {
            return;
        }
        mDegreeCenter = degree;
        invalidate();
    }

    public void changeDegreeBoard(float degree) {
        if (this.mDegreeBoard == degree) {
            return;
        }
        mDegreeBoard = degree;
        invalidate();
    }

    class NoDetermineSizeException extends Exception {
        public NoDetermineSizeException(String message) {
            super(message);
        }
    }
}
