package com.termux.app.terminal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Liquid Glass button surface: translucent dark fill, a 135° diagonal gradient
 * rim (bright top-left, faint middle, mid bottom-right) and a thin inner
 * highlight band along the top edge. Used for tab bar buttons when the Liquid
 * Glass effect is enabled.
 */
public class LiquidGlassButtonDrawable extends Drawable {

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int mCornerRadius;
    private final int mStrokeWidth;
    private final int mFillColor;
    private final int mActiveFillColor;
    private final boolean mActive;
    private final float mDensity;

    public LiquidGlassButtonDrawable(int cornerRadius, int strokeWidth, float density,
                                     int fillColor, int activeFillColor, boolean active) {
        mCornerRadius = Math.max(0, cornerRadius);
        mStrokeWidth = strokeWidth > 0 ? strokeWidth : Math.max(1, Math.round(1.2f * density));
        mDensity = density;
        mFillColor = fillColor;
        mActiveFillColor = activeFillColor;
        mActive = active;
        mStrokePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        RectF bounds = new RectF(getBounds());
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        float hw = mStrokeWidth / 2f;
        float r = Math.min(mCornerRadius, Math.min(bounds.width(), bounds.height()) / 2f);

        // Translucent dark glass fill (slightly stronger when active).
        mFillPaint.setColor(mActive ? mActiveFillColor : mFillColor);
        canvas.drawRoundRect(bounds, r, r, mFillPaint);

        // Specular rim: 135° diagonal gradient like light wrapping the droplet.
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
            new int[]{0xCCFFFFFF, 0x1AFFFFFF, 0x80FFFFFF},
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(bounds.left + hw, bounds.top + hw, bounds.right - hw, bounds.bottom - hw),
            r, r, mStrokePaint);

        // Inner highlight band along the top edge (droplet thickness).
        float hlTop = bounds.top + mStrokeWidth;
        float hlBottom = hlTop + 3f * mDensity;
        if (hlBottom > bounds.bottom) hlBottom = bounds.bottom;
        if (hlTop < hlBottom) {
            mInnerPaint.setShader(new LinearGradient(0, hlTop, 0, hlBottom,
                0x4DFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds.left + mStrokeWidth, hlTop, bounds.right - mStrokeWidth, hlBottom, mInnerPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
