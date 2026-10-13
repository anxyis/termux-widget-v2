package com.termux.widget.utils;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.view.animation.OvershootInterpolator;

public class BouncyAnimationHelper {

    private static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator(2.2f);
    private static final DecelerateInterpolator DECELERATE = new DecelerateInterpolator();
    private static final FastOutSlowInInterpolator FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();

    /**
     * Applies bouncy spring touch feedback to any interactive view.
     * Scales slightly down on ACTION_DOWN, and springs back with an overshoot on ACTION_UP/CANCEL.
     * Returns false to preserve click and long-click listener handling.
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void applySpringPress(View view) {
        applySpringPress(view, 0.94f);
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void applySpringPress(View view, float pressScale) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(pressScale)
                            .scaleY(pressScale)
                            .setDuration(90)
                            .setInterpolator(DECELERATE)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(220)
                            .setInterpolator(OVERSHOOT)
                            .start();
                    break;
            }
            return false;
        });
    }

    /**
     * Subtle bouncy pop animation when state changes or items are toggled.
     */
    public static void popBounce(View view) {
        if (view == null) return;
        view.setScaleX(0.90f);
        view.setScaleY(0.90f);
        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator(2.5f))
                .start();
    }

    /**
     * Smoothly animates an integer color value (e.g. background or text color).
     */
    public static ValueAnimator animateColor(int startColor, int endColor, int durationMs, ValueAnimator.AnimatorUpdateListener updateListener) {
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        colorAnim.setDuration(durationMs);
        colorAnim.setInterpolator(FAST_OUT_SLOW_IN);
        if (updateListener != null) {
            colorAnim.addUpdateListener(updateListener);
        }
        colorAnim.start();
        return colorAnim;
    }
}
