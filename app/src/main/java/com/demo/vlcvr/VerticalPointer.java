package com.demo.vlcvr;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class VerticalPointer extends View {
    private float mOuterRaidus;//最外环半径
    private float mRadius; //外圆半径

    private float mPadding; //边距
    private float mTextSize; //文字大小

    private int mColorLong; //长线的颜色
    private int mColorShort; //短线的颜色
    private Paint mPaint; //画笔
    private float mSpace = DptoPx(20);//刻度尺与圆盘的上下间隔
    private float mTextScalePadding = DptoPx(8);//刻度值与刻度尺的间隔
    private float translateY/*= -mRadius + mSpace*/;//绘制刻度时圆心移动的Y轴方向绝对值
    private float drawHeight/* = 2 * (mRadius - mSpace)*/;//划线的高高度
    private float translateX/* = mRadius / 20*/;//绘制刻度时圆心移动的X轴方向绝对值
    private float scaleTranslateX /*= mRadius / 4*/;//绘制刻度尺时，刻度尺向X轴偏离的数值，左边的刻度尺向左偏移则为负值，右边的向右偏移则为正值
    private int mDegreeBoard = 0;//表盘度数，及用户操作改变的度数
    private int mInitDegree = 90;//默认初始附加度数，表盘显示总度数180，（-90,90），度数0度在中间，所以附加一个90度的附加度数
    private int mDegreeCenter = 0;//中心度数，及设备改变的度数
    int lineWidth = 20;//刻度尺长度
    int longLineWidth = 6;//关键位刻度尺比普通刻度尺的多余长度
    private int[] angle = new int[]{0, 15, 30, 45, 60, 75, 90, 75, 60, 45, 30, 15,
            0, -15, -30, -45, -60, -75, -90, -75, -60, -45, -30, -15};//当度数为正时显示的角度对应关系
    private int[] negeAngle = new int[]{0, -15, -30, -45, -60, -75, -90, -75, -60, -45, -30, -15,
            0, 15, 30, 45, 60, 75, 90, 75, 60, 45, 30, 15};//当度数为负时显示的角度对应关系
    private int[] degree = new int[]{0, 15, 30, 45, 60, 75, 90, 105, 120, 135,
            150, 165, 180, 195, 210, 225, 240, 255, 270, 285, 300, 315, 330, 345};//当度数为正时对应的key表
    private int[] negeDegree = new int[]{0, -15, -30, -45, -60, -75, -90, -105, -120, -135,
            -150, -165, -180, -195, -210, -225, -240, -255, -270, -285, -300, -315, -330, -345};//当度数为负时对应的key表

    public VerticalPointer(Context context) {
        this(context, null);
    }

    public VerticalPointer(Context context, AttributeSet attrs) {
        super(context, attrs);
        obtainStyledAttrs(attrs); //获取自定义的属性
        init(); //初始化画笔
    }

    private void obtainStyledAttrs(AttributeSet attrs) {
        TypedArray array = null;
        try {
            array = getContext().obtainStyledAttributes(attrs, R.styleable.VerticalPointer);
            mPadding = array.getDimension(R.styleable.VerticalPointer_vp_padding, DptoPx(5));
            mTextSize = array.getDimension(R.styleable.VerticalPointer_vp_text_size, SptoPx(9));
            mColorLong = array.getColor(R.styleable.VerticalPointer_vp_scale_long_color, Color.argb(225, 0, 0, 0));
            mColorShort = array.getColor(R.styleable.VerticalPointer_vp_scale_short_color, Color.argb(125, 0, 0, 0));
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
        translateY = -mRadius + mSpace;//绘制刻度时圆心移动的Y轴方向绝对值
        drawHeight = 2 * (mRadius - mSpace);//划线的高高度
        translateX = mRadius / 20;//绘制刻度时圆心移动的X轴方向绝对值
        scaleTranslateX = mRadius / 4;//绘制刻度尺时，刻度尺向X轴偏离的数值，左边的刻度尺向左偏移则为负值，右边的向右偏移则为正值
    }

    //绘制外圆背景
    public void paintCircle(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_black_mask_dark));
        canvas.drawCircle(0, 0, mOuterRaidus, mPaint);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_black_mask));
        canvas.drawCircle(0, 0, mRadius, mPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(getWidth() / 2, getHeight() / 2);
        //绘制外圆背景
        paintCircle(canvas);
        //绘制刻度
        paintScale(canvas);
        paintMask(canvas);
        paintText(canvas);
        paintPointer(canvas);
        canvas.restore();
    }

    //绘制刻度
    private void paintScale(Canvas canvas) {
        int scaleCount = 37;//刻度尺的刻度数量
        float singleScaleSpace = drawHeight / (scaleCount - 1);//单个刻度尺标的间隔高度
        //绘制左边的刻度
        canvas.save();
        canvas.translate(-translateX, translateY);//canvas移动
        int totalDegree = mInitDegree + mDegreeBoard;//度数+默认附加值
        int zeroIndex = findZero(totalDegree);//找到0刻度尺的位置
        for (int i = 0; i < scaleCount; i++) {
            String text = "";
            int realDegree;
            if ((i - zeroIndex) % 3 == 0) {//如果是5的倍数，即要绘制度数的指示的刻度
                realDegree = (totalDegree - (5 * i)) - findMod(totalDegree);//真正绘制的度数
                if (realDegree >= 90) {
                    realDegree = 2 * mInitDegree - realDegree;//转化为-90----90度
                } else if (realDegree < -90 && realDegree >= -180) {
                    realDegree = -(2 * mInitDegree + realDegree);
                } else if (realDegree < -180) {
                    realDegree = -((2 * mInitDegree) + realDegree);
                }
                if (i == zeroIndex) {//如果是0刻度
                    realDegree = 0;
                    text = realDegree + "";
                    mPaint.setColor(getContext().getResources().getColor(R.color.color_red_half));
                } else if (realDegree == 0) {//如果计算出的值为0
                    text = realDegree + "";
                    mPaint.setColor(getContext().getResources().getColor(R.color.color_red_half));
                } else {
                    text = realDegree + "";
                    if (realDegree > 0) {//如果度数>0
                        for (int m = 0; m < degree.length; m++) {
                            if (realDegree == degree[m]) {
                                text = "" + angle[m];//查表获得实际显示的度数
                            }
                        }
                    } else {//度数小于0
                        for (int m = 0; m < negeDegree.length; m++) {
                            if (realDegree == negeDegree[m]) {
                                text = "" + negeAngle[m];//查表获得实际显示的度数
                            }
                        }
                    }
                    mPaint.setColor(mColorLong);
                    if (text.equals("0")) {//如果查表查出的值为0
                        mPaint.setColor(getContext().getResources().getColor(R.color.color_red_half));
                    }
                }
                mPaint.setTextSize(mTextSize);
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
                Rect textBound = new Rect();
                mPaint.getTextBounds(text, 0, text.length(), textBound);
                canvas.drawText(text, -scaleTranslateX - textBound.width() - mTextScalePadding, i * singleScaleSpace + textBound.height() / 2, mPaint);
                canvas.drawLine(-scaleTranslateX - longLineWidth, i * singleScaleSpace, -scaleTranslateX + lineWidth, i * singleScaleSpace, mPaint);

            } else {
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1f));
                mPaint.setColor(mColorShort);
                canvas.drawLine(-scaleTranslateX, i * singleScaleSpace, -scaleTranslateX + lineWidth, i * singleScaleSpace, mPaint);
            }
        }
        canvas.restore();
        //绘制右边的刻度
        canvas.save();
        canvas.translate(translateX, translateY);
        for (int i = 0; i < scaleCount; i++) {
            String text = "";
            int realDegree;
            if ((i - zeroIndex) % 3 == 0) {
                realDegree = (totalDegree - (5 * i)) - findMod(totalDegree);
                if (realDegree >= 90) {
                    realDegree = 2 * mInitDegree - realDegree;
                } else if (realDegree < -90 && realDegree >= -180) {
                    realDegree = -(2 * mInitDegree + realDegree);
                } else if (realDegree < -180) {
                    realDegree = -((2 * mInitDegree) + realDegree);
                }
                if (i == zeroIndex) {
                    realDegree = 0;
                    text = realDegree + "";
                    mPaint.setColor(getContext().getResources().getColor(R.color.color_red_half));
                } else if (realDegree == 0) {
                    text = realDegree + "";
                    mPaint.setColor(getContext().getResources().getColor(R.color.color_red_half));
                } else {
                    text = realDegree + "";
                    if (realDegree > 0) {
                        for (int m = 0; m < degree.length; m++) {
                            if (realDegree == degree[m]) {
                                text = "" + angle[m];
                            }
                        }
                    } else {
                        for (int m = 0; m < negeDegree.length; m++) {
                            if (realDegree == negeDegree[m]) {
                                text = "" + negeAngle[m];
                            }
                        }
                    }
                    mPaint.setColor(mColorLong);
                    if (text.equals("0")) {
                        mPaint.setColor(getContext().getResources().getColor(R.color.color_red_half));
                    }
                }
                mPaint.setTextSize(mTextSize);
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
                Rect textBound = new Rect();
                mPaint.getTextBounds(text, 0, text.length(), textBound);
                canvas.drawText(text, scaleTranslateX + mTextScalePadding, i * singleScaleSpace + textBound.height() / 2, mPaint);
                canvas.drawLine(scaleTranslateX + longLineWidth, i * singleScaleSpace, scaleTranslateX - lineWidth, i * singleScaleSpace, mPaint);
            } else {
                mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1f));
                mPaint.setColor(mColorShort);
                canvas.drawLine(scaleTranslateX, i * singleScaleSpace, scaleTranslateX - lineWidth, i * singleScaleSpace, mPaint);
            }
        }
        canvas.restore();
    }

    private void paintMask(Canvas canvas) {
        int realDegree;
        if (mDegreeCenter > 90) {
            realDegree = 2 * mInitDegree - mDegreeCenter;
        } else if (mDegreeCenter < -90) {
            realDegree = mInitDegree + mDegreeCenter;
        } else {
            realDegree = mDegreeCenter;
        }
        float drawHeightScale = ((float) realDegree) / 90f;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getContext().getResources().getColor(R.color.color_red_mask));
        Rect rect = new Rect((int) Math.ceil(-scaleTranslateX), -(int) (drawHeightScale * drawHeight / 2), (int) Math.ceil(scaleTranslateX), (int) Math.ceil(drawHeight / 2));
        canvas.drawRect(rect, mPaint);
    }

    private void paintText(Canvas canvas) {
        mPaint.setColor(mColorLong);
        String textTian = "天";
        Rect textTianBound = new Rect();
        mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
        mPaint.getTextBounds(textTian, 0, textTian.length(), textTianBound);
        mPaint.setTextSize(mTextSize + 3);
        canvas.drawText(textTian, 0 - (textTianBound.width() / 2), (-drawHeight / 2) - (textTianBound.height() / 2f), mPaint);

        String textDi = "地";
        Rect textDiBound = new Rect();
        mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
        mPaint.getTextBounds(textDi, 0, textDi.length(), textDiBound);
        mPaint.setTextSize(mTextSize + 3);
        canvas.drawText(textDi, 0 - (textDiBound.width() / 2), (drawHeight / 2) + (textDiBound.height() * 1.5f), mPaint);
    }

    private void paintPointer(Canvas canvas) {
        canvas.save();
        canvas.translate(-translateX, translateY);
        mPaint.setStrokeWidth(SizeUtil.Dp2Px(getContext(), 1.5f));
        mPaint.setColor(Color.RED);
        canvas.drawLine(-scaleTranslateX, drawHeight / 2, scaleTranslateX + lineWidth, drawHeight / 2, mPaint);
        canvas.restore();
    }

    public void changeDegreeBoard(float degree) {
        if (mDegreeBoard == -(int) (degree % 360)) {
            return;
        }
        this.mDegreeBoard = -(int) (degree % 360);
        invalidate();
    }

    public void changeDegreeCenter(float degree) {
        if (mDegreeCenter == degree) {
            return;
        }
        this.mDegreeCenter = (int) (degree % 180);
        invalidate();
    }

    class NoDetermineSizeException extends Exception {
        public NoDetermineSizeException(String message) {
            super(message);
        }
    }

    /**
     * 找到0点的位置
     *
     * @param degree
     * @return
     */
    private int findZero(int degree) {
        return degree / 5;
    }

    /**
     * 找到0点后的余数
     *
     * @param degree
     * @return
     */
    private int findMod(int degree) {
        return degree - findZero(degree) * 5;
    }
}
