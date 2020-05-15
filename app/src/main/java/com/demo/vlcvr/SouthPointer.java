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

import java.util.ArrayList;
import java.util.List;

public class SouthPointer extends View {
    private float mOuterRaidus;//最外环半径
    private float mRadius; //外圆半径
    private float mInnerRadius;//表盘半径
    private float mCenterRadius;//最内圆半径
    private float mPadding; //边距
    private float mTextSize; //文字大小
    private float mFootPointWidth; //尾部指针宽度
    private float mHeadPointWidth; //头部宽度
    private int mPointRadius; // 指针圆角
    private float mPointEndLength; //指针末尾的长度

    private int mColorLong; //长线的颜色
    private int mColorShort; //短线的颜色
    private int mFootPointColor; //尾部指针的颜色
    private int mHeadPointColor; //头部指针的颜色
    private List<String> mLocations = new ArrayList<>();
    private Paint mPaint; //画笔
    private float mPointerHeadDegree = 0;//头部指针的旋转角度
    private float mPointerFootDegree = 0;//尾部指针的旋转角度
    private float mBoardDegree = 0;//表盘的旋转角度

    public SouthPointer(Context context) {
        this(context, null);
    }

    public SouthPointer(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainStyledAttrs(attrs); //获取自定义的属性
        init(); //初始化画笔
    }

    private void obtainStyledAttrs(AttributeSet attrs) {
        TypedArray array = null;
        try {
            array = getContext().obtainStyledAttributes(attrs, R.styleable.SouthPointer);
            mPadding = array.getDimension(R.styleable.SouthPointer_sp_padding, DptoPx(5));
            mTextSize = array.getDimension(R.styleable.SouthPointer_sp_text_size, SptoPx(9));
            mFootPointWidth = array.getDimension(R.styleable.SouthPointer_sp_minute_pointer_width, DptoPx(3));
            mHeadPointWidth = array.getDimension(R.styleable.SouthPointer_sp_second_pointer_width, DptoPx(4));
            mPointRadius = (int) array.getDimension(R.styleable.SouthPointer_sp_pointer_corner_radius, DptoPx(10));
            mPointEndLength = array.getDimension(R.styleable.SouthPointer_sp_pointer_end_length, DptoPx(10));

            mColorLong = array.getColor(R.styleable.SouthPointer_sp_scale_long_color, Color.argb(225, 0, 0, 0));
            mColorShort = array.getColor(R.styleable.SouthPointer_sp_scale_short_color, Color.argb(125, 0, 0, 0));
            mFootPointColor = array.getColor(R.styleable.SouthPointer_sp_minute_pointer_color, Color.WHITE);
            mHeadPointColor = array.getColor(R.styleable.SouthPointer_sp_second_pointer_color, Color.RED);
        } catch (Exception e) {
            //一旦出现错误全部使用默认值
            mPadding = DptoPx(5);
            mTextSize = SptoPx(2);
            mFootPointWidth = DptoPx(3);
            mHeadPointWidth = DptoPx(4);
            mPointRadius = (int) DptoPx(5);
            mPointEndLength = DptoPx(5);

            mColorLong = Color.argb(225, 0, 0, 0);
            mColorShort = Color.argb(125, 0, 0, 0);
            mFootPointColor = Color.BLACK;
            mHeadPointColor = Color.RED;
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
        mLocations.add("成都一路");
        mLocations.add("成都二路");
        mLocations.add("成都三路");
        mLocations.add("成都四路");
        mLocations.add("成都五路");
        mLocations.add("成都六路");
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
        mCenterRadius = mRadius / 3;//内圆修饰半径
        mPointEndLength = mInnerRadius / 6; //尾部指针默认为半径的六分之一
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
        mPaint.setColor(getContext().getResources().getColor(R.color.color_black_mask));
        canvas.drawCircle(0, 0, mCenterRadius, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(getWidth() / 2, getHeight() / 2);
        //绘制外圆背景
        paintCircle(canvas);
       /*
        如果要指针随着转动，则最后绘制指针，否则，先绘制指针
        //绘制刻度
        paintScale(canvas);
        //绘制方位
        paintLocation(canvas);*/
        //绘制指针
        paintPointer(canvas);
        //绘制刻度
        paintScale(canvas);
        //绘制方位
        paintLocation(canvas);

        canvas.restore();
        //刷新
        //  postInvalidateDelayed(1000);
    }

    private void paintPointer(Canvas canvas) {
        //Calendar calendar = Calendar.getInstance();
        // int minute = calendar.get(Calendar.MINUTE); //分
        // int second = calendar.get(Calendar.SECOND); //秒
        //int angleMinute = minute * 360 / 60; //分针转过的角度
        //int angleSecond = second * 360 / 60; //秒针转过的角度
        //绘制分针
        canvas.save();
        canvas.rotate(mPointerFootDegree);
        RectF rectFMinute = new RectF(-mFootPointWidth / 2, -mInnerRadius * 3.5f / 5, mFootPointWidth / 2, mPointEndLength);
        mPaint.setColor(mFootPointColor);
        mPaint.setStrokeWidth(mFootPointWidth);
        canvas.drawRoundRect(rectFMinute, mPointRadius, mPointRadius, mPaint);
        canvas.restore();
        //绘制秒针
        canvas.save();
        canvas.rotate(mPointerHeadDegree);
        RectF rectFSecond = new RectF(-mHeadPointWidth / 2, -mInnerRadius + 15, mHeadPointWidth / 2, mPointEndLength);
        mPaint.setColor(mHeadPointColor);
        mPaint.setStrokeWidth(mHeadPointWidth);
        canvas.drawRoundRect(rectFSecond, mPointRadius, mPointRadius, mPaint);
        canvas.restore();
        //绘制中心小圆
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mHeadPointColor);
        canvas.drawCircle(0, 0, mHeadPointWidth * 2, mPaint);
    }

    //绘制刻度
    private void paintScale(Canvas canvas) {
        canvas.rotate(mBoardDegree);
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
                if (i == 0) {
                    text = "北";
                } else if (i == 15) {
                    text = "东";
                } else if (i == 30) {
                    text = "南";
                } else if (i == 45) {
                    text = "西";
                } else {
                    text = 6 * i + "";
                }
                Rect textBound = new Rect();
                mPaint.getTextBounds(text, 0, text.length(), textBound);
                if (i == 0 || i == 15 || i == 30 || i == 45) {//设置东南西北方向为红色
                    mPaint.setColor(Color.RED);
                } else {
                    mPaint.setColor(mColorLong);
                }
                canvas.save();
//                KLog.i((textBound.bottom - textBound.top) / 2);
                canvas.translate(0, -mInnerRadius + DptoPx(5) + lineWidth + mPadding + (textBound.bottom - textBound.top) / 2);
                mPaint.setStyle(Paint.Style.FILL);
                canvas.rotate(-6 * i);
                canvas.drawText(text, -(textBound.right + textBound.left) / 2, -(textBound.bottom + textBound.top) / 2, mPaint);
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

    //绘制位置
    private void paintLocation(Canvas canvas) {
        mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1));
        for (int i = 0; i < 60; i++) {
            if (i % 10 == 0) { //整点
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
                mPaint.setColor(mColorLong);
                mPaint.setTextSize(mTextSize);
                // String text = ((i / 5) == 0 ? 12 : (i / 5)) + "";
                String text = "";
                if (i == 0) {
                    text = mLocations.get(0);
                } else if (i == 10) {
                    text = mLocations.get(1);
                } else if (i == 20) {
                    text = mLocations.get(2);
                } else if (i == 30) {
                    text = mLocations.get(3);
                } else if (i == 40) {
                    text = mLocations.get(4);
                } else if (i == 50) {
                    text = mLocations.get(5);
                } else {
                    text = 6 * i + "";
                }
                Rect textBound = new Rect();
                mPaint.getTextBounds(text, 0, text.length(), textBound);
                mPaint.setColor(mColorLong);
                canvas.save();
//                KLog.i((textBound.bottom - textBound.top) / 2);
                canvas.translate(0, -mRadius + mPadding + (textBound.bottom - textBound.top) / 2);
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawText(text, -(textBound.right + textBound.left) / 2, -(textBound.bottom + textBound.top) / 2, mPaint);
                canvas.rotate(-6 * i);
                canvas.restore();
            }
            canvas.rotate(6);
        }
    }

    public void changeDegree(float headDegree, float footDegree, float boardDegree) {
        if (mPointerFootDegree == footDegree && mPointerHeadDegree == headDegree && mBoardDegree == boardDegree) {
            return;
        }
        this.mPointerHeadDegree = headDegree;
        this.mPointerFootDegree = footDegree;
        this.mBoardDegree = boardDegree;
        invalidate();
    }

    class NoDetermineSizeException extends Exception {
        public NoDetermineSizeException(String message) {
            super(message);
        }
    }
}
