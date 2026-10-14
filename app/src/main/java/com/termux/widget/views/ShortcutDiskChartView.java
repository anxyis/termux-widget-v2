package com.termux.widget.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.termux.widget.utils.BouncyAnimationHelper;

public class ShortcutDiskChartView extends View {

    private final Paint mTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSlicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mBounds = new RectF();

    private int mScriptsCount = 0;
    private int mTogglesCount = 0;
    private int mCommandsCount = 0;
    private int mTotalCount = 0;

    private float mSweepFraction = 1f;
    private ValueAnimator mAnimator;

    private boolean mIsAmoled = false;

    private int mColorTrack;
    private int mColorScripts;
    private int mColorToggles;
    private int mColorCommands;
    private int mColorTextTitle;
    private int mColorTextSub;

    private float mStrokeWidthPx;

    public ShortcutDiskChartView(Context context) {
        super(context);
        init(context);
    }

    public ShortcutDiskChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ShortcutDiskChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mStrokeWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(mStrokeWidthPx);
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        mSlicePaint.setStyle(Paint.Style.STROKE);
        mSlicePaint.setStrokeWidth(mStrokeWidthPx);
        mSlicePaint.setStrokeCap(Paint.Cap.ROUND);

        mTextTitlePaint.setTextAlign(Paint.Align.CENTER);
        mTextTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTextTitlePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()));

        mTextSubPaint.setTextAlign(Paint.Align.CENTER);
        mTextSubPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        mTextSubPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9, getResources().getDisplayMetrics()));
        mTextSubPaint.setLetterSpacing(0.12f);

        resolveColors();

        BouncyAnimationHelper.applySpringPress(this, 0.94f);
        setOnClickListener(v -> animateSweep());
    }

    public void setAmoledMode(boolean isAmoled) {
        if (mIsAmoled != isAmoled) {
            mIsAmoled = isAmoled;
            resolveColors();
            invalidate();
        }
    }

    private void resolveColors() {
        if (mIsAmoled) {
            mColorTrack = Color.parseColor("#1C1C1E");
            mColorScripts = Color.WHITE;
            mColorToggles = Color.parseColor("#9E9E9E");
            mColorCommands = Color.parseColor("#545458");
            mColorTextTitle = Color.WHITE;
            mColorTextSub = Color.parseColor("#9E9E9E");
        } else {
            TypedValue typedValue = new TypedValue();
            Context context = getContext();

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHighest, typedValue, true);
            mColorTrack = typedValue.data != 0 ? typedValue.data : Color.parseColor("#E7E0EC");

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            mColorScripts = typedValue.data != 0 ? typedValue.data : Color.parseColor("#6750A4");

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorTertiary, typedValue, true);
            mColorToggles = typedValue.data != 0 ? typedValue.data : Color.parseColor("#7D5260");

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSecondary, typedValue, true);
            mColorCommands = typedValue.data != 0 ? typedValue.data : Color.parseColor("#625B71");

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            mColorTextTitle = typedValue.data != 0 ? typedValue.data : Color.BLACK;

            context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
            mColorTextSub = typedValue.data != 0 ? typedValue.data : Color.DKGRAY;
        }

        mTrackPaint.setColor(mColorTrack);
        mTextTitlePaint.setColor(mColorTextTitle);
        mTextSubPaint.setColor(mColorTextSub);
    }

    public void setData(int scripts, int toggles, int commands) {
        boolean changed = (mScriptsCount != scripts || mTogglesCount != toggles || mCommandsCount != commands);
        mScriptsCount = Math.max(0, scripts);
        mTogglesCount = Math.max(0, toggles);
        mCommandsCount = Math.max(0, commands);
        mTotalCount = mScriptsCount + mTogglesCount + mCommandsCount;

        if (changed || mSweepFraction == 0f) {
            animateSweep();
        } else {
            invalidate();
        }
    }

    public void animateSweep() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(700);
        mAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mAnimator.addUpdateListener(anim -> {
            mSweepFraction = (float) anim.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = mStrokeWidthPx / 2f + 4f;
        mBounds.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = mBounds.centerX();
        float centerY = mBounds.centerY();

        // 1. Draw base track circle
        canvas.drawOval(mBounds, mTrackPaint);

        if (mTotalCount > 0 && mSweepFraction > 0.001f) {
            int nonZeroSlices = (mScriptsCount > 0 ? 1 : 0) + (mTogglesCount > 0 ? 1 : 0) + (mCommandsCount > 0 ? 1 : 0);
            float gapDegrees = (nonZeroSlices > 1) ? 6f : 0f;
            float totalGapDegrees = gapDegrees * nonZeroSlices;
            float availableSweep = 360f - totalGapDegrees;

            float currentAngle = -90f; // Start at top 12 o'clock

            // Helper slice drawer
            currentAngle = drawSlice(canvas, mScriptsCount, mColorScripts, currentAngle, availableSweep, gapDegrees);
            currentAngle = drawSlice(canvas, mTogglesCount, mColorToggles, currentAngle, availableSweep, gapDegrees);
            drawSlice(canvas, mCommandsCount, mColorCommands, currentAngle, availableSweep, gapDegrees);
        }

        // 2. Draw Center Text (Calculated with density offsets to prevent any font metric collisions)
        float density = getResources().getDisplayMetrics().density;
        String countStr = String.valueOf(mTotalCount);
        float textY = centerY - (2f * density);
        canvas.drawText(countStr, centerX, textY, mTextTitlePaint);

        String subStr = "ITEMS";
        float subY = centerY + (13f * density);
        canvas.drawText(subStr, centerX, subY, mTextSubPaint);
    }

    private float drawSlice(Canvas canvas, int count, int color, float startAngle, float availableSweep, float gapDegrees) {
        if (count <= 0) return startAngle;

        float fullSliceAngle = (count / (float) mTotalCount) * availableSweep;
        float animatedAngle = fullSliceAngle * mSweepFraction;

        if (animatedAngle > 0.5f) {
            mSlicePaint.setColor(color);
            canvas.drawArc(mBounds, startAngle + (gapDegrees / 2f), animatedAngle, false, mSlicePaint);
        }

        return startAngle + fullSliceAngle + gapDegrees;
    }

    public int getScriptsColor() {
        return mColorScripts;
    }

    public int getTogglesColor() {
        return mColorToggles;
    }

    public int getCommandsColor() {
        return mColorCommands;
    }
}
