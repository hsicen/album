package com.yanzhenjie.album.record;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.yanzhenjie.album.R;

/**
 * <p>作者：hsicen  2019/11/20 9:12
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：倒计时Button
 */
public class CountDownButton extends View implements Runnable {
    private int backgroundColor;
    private int gradientStartColor;
    private int gradientEndColor;
    private float borderWidth;
    private int strokeColor;
    private int progressColor;
    private int progressHeadColor;
    private long countDownMilliseconds;
    private long minCountDownMilliseconds;
    private float stopIconSize;

    private long startCountDownTime;

    private State state = State.IDLE;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect rect = new Rect();
    private LinearGradient gradient;
    private OnCountDownListener onCountDownListener;
    private float targetRadius;
    private float radiusX;
    private float radiusY;

    private float animationFraction;

    public interface OnCountDownListener {
        void onTimeEnd();
    }

    public enum State {
        IDLE, COUNTING
    }

    public CountDownButton(Context context) {
        this(context, null);
    }

    public CountDownButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.countDownButtonStyle);
    }

    public CountDownButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CountDownButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CountDownButton, defStyleAttr, defStyleRes);
        backgroundColor = a.getColor(R.styleable.CountDownButton_backgroundColor, Color.WHITE);
        gradientStartColor = a.getColor(R.styleable.CountDownButton_gradientStartColor, Color.parseColor("#58A4FF"));
        gradientEndColor = a.getColor(R.styleable.CountDownButton_gradientEndColor, Color.parseColor("#437AEB"));
        borderWidth = a.getDimension(R.styleable.CountDownButton_border, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, displayMetrics));
        countDownMilliseconds = a.getInteger(R.styleable.CountDownButton_countDownSeconds, 30) * DateUtils.SECOND_IN_MILLIS;
        strokeColor = a.getColor(R.styleable.CountDownButton_strokeColor, Color.parseColor("#31ffffff"));
        progressColor = a.getColor(R.styleable.CountDownButton_progressColor, Color.parseColor("#437AEB"));
        progressHeadColor = a.getColor(R.styleable.CountDownButton_progressHeadColor, Color.WHITE);
        minCountDownMilliseconds = a.getInteger(R.styleable.CountDownButton_minCountDownSeconds, 3) * DateUtils.SECOND_IN_MILLIS;
        stopIconSize = a.getDimension(R.styleable.CountDownButton_stopIconSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics));
        targetRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, displayMetrics);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(View.resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), View.resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        gradient = new LinearGradient(rect.centerX(), borderWidth, rect.centerX(), rect.bottom - borderWidth, gradientStartColor, gradientEndColor, Shader.TileMode.CLAMP);
        radiusX = (rect.width() - borderWidth * 2) / 2;
        radiusY = (rect.height() - borderWidth * 2) / 2;
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() - startCountDownTime < countDownMilliseconds) {
            invalidate();
            postDelayed(this, 16);
        } else {
            if (null != onCountDownListener) {
                onCountDownListener.onTimeEnd();
            }
            stopCountDown();
        }
    }

    public void startCountDown() {
        if (state == State.IDLE) {
            stopCountDown();
            state = State.COUNTING;
            startCountDownTime = System.currentTimeMillis();
            post(this);
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1).setDuration(200);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    animationFraction = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            valueAnimator.start();
        }
    }

    public void stopCountDown() {
        if (state == State.COUNTING) {
            state = State.IDLE;
            startCountDownTime = 0;
            removeCallbacks(this);
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0).setDuration(200);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    animationFraction = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            valueAnimator.start();
        }
    }

    public void setOnCountDownListener(OnCountDownListener onCountDownListener) {
        this.onCountDownListener = onCountDownListener;
    }

    public State getState() {
        return state;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(strokeColor);
        canvas.drawOval(rect.left, rect.top, rect.right, rect.bottom, paint);

        paint.setColor(strokeColor);
        canvas.drawArc(rect.left, rect.top, rect.right, rect.bottom, -90, 360f * minCountDownMilliseconds / countDownMilliseconds, true, paint);
        canvas.drawArc(rect.left, rect.top, rect.right, rect.bottom, -90, 360f * minCountDownMilliseconds / countDownMilliseconds, true, paint);
        canvas.drawArc(rect.left, rect.top, rect.right, rect.bottom, -90, 360f * minCountDownMilliseconds / countDownMilliseconds, true, paint);

        if (state == State.COUNTING) {
            paint.setColor(progressColor);
            float sweepAngle = Math.min(360, 360 * (System.currentTimeMillis() - startCountDownTime) / countDownMilliseconds);
            canvas.drawArc(rect.left, rect.top, rect.right, rect.bottom, -90f, sweepAngle, true, paint);
        }

        paint.setColor(progressHeadColor);
        canvas.drawArc(rect.left, rect.top, rect.right, rect.bottom, 360f * minCountDownMilliseconds / countDownMilliseconds - 95, 5, true, paint);

        paint.setColor(backgroundColor);
        canvas.drawOval(rect.left + borderWidth, rect.top + borderWidth, rect.right - borderWidth, rect.bottom - borderWidth, paint);

        paint.setColor(backgroundColor);
        paint.setAlpha((int) (255 - 255 * animationFraction));
        canvas.drawOval(rect.left, rect.top, rect.right, rect.bottom, paint);
        paint.setAlpha(255);

        paint.setShader(gradient);
        canvas.drawRoundRect((rect.left + borderWidth + (rect.centerX() - stopIconSize / 2 - rect.left - borderWidth) * animationFraction), rect.top + borderWidth + (rect.centerY() - stopIconSize / 2 - rect.top - borderWidth) * animationFraction, rect.right - borderWidth + (rect.centerX() + stopIconSize / 2 - rect.right + borderWidth) * animationFraction, rect.bottom - borderWidth + (rect.centerY() + stopIconSize / 2 - rect.bottom + borderWidth) * animationFraction, radiusX + (targetRadius - radiusX) * animationFraction, radiusY + (targetRadius - radiusY) * animationFraction, paint);
        paint.setShader(null);
    }
}
