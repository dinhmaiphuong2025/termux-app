package com.termux.app.terminal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom Drawable for terminal window border.
 * Supports:
 * - Standard full rounded rectangle border
 * - Hermes / Brackets style border (open left and right sides with corner accents)
 */
public class TerminalBorderDrawable extends Drawable {

    public static final String STYLE_FULL = "full";
    public static final String STYLE_BRACKETS = "brackets";

    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mStrokeWidth = 1;
    private int mCornerRadius = 1;
    private int mStrokeColor = 0xFFFFFFFF;
    private String mStyle = STYLE_FULL;
    private boolean mGlassHighlight = false;

    private float mDensity = 1.0f;

    public TerminalBorderDrawable() {
        mStrokePaint.setStyle(Paint.Style.STROKE);
    }

    public void setConfig(float density, int strokeWidth, int cornerRadius, int strokeColor, String style) {
        mDensity = density;
        mStrokeWidth = Math.max(1, strokeWidth);
        mCornerRadius = Math.max(0, cornerRadius);
        mStrokeColor = strokeColor;
        mStyle = style != null ? style : STYLE_FULL;

        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setColor(mStrokeColor);

        invalidateSelf();
    }

    /**
     * Enable or disable the Apple-style specular rim used by the liquid glass
     * effect: a thin light band along the top edge that fades downward, like
     * light catching the upper lip of a glass pane. No full-box glow.
     */
    public void setGlassHighlight(boolean enabled) {
        if (mGlassHighlight == enabled) return;
        mGlassHighlight = enabled;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0 || mStrokeWidth <= 0) return;

        float halfStroke = mStrokeWidth / 2.0f;
        float left = bounds.left + halfStroke;
        float top = bounds.top + halfStroke;
        float right = bounds.right - halfStroke;
        float bottom = bounds.bottom - halfStroke;
        float r = Math.min(mCornerRadius, Math.min(bounds.width(), bounds.height()) / 2.0f);

        if (STYLE_BRACKETS.equals(mStyle)) {
            // Hermes / Bracket style:
            // Top full line
            // Bottom full line
            // Left & Right: only draw corner accents
            float cornerArm = Math.max(r + 14 * mDensity, 20 * mDensity);
            cornerArm = Math.min(cornerArm, (bottom - top) / 3.0f);

            Path path = new Path();

            // 1. Top edge & top-left / top-right corners
            path.moveTo(left, top + cornerArm);
            path.lineTo(left, top + r);
            if (r > 0) {
                path.quadTo(left, top, left + r, top);
            } else {
                path.lineTo(left, top);
            }
            path.lineTo(right - r, top);
            if (r > 0) {
                path.quadTo(right, top, right, top + r);
            } else {
                path.lineTo(right, top);
            }
            path.lineTo(right, top + cornerArm);

            // 2. Bottom edge & bottom-left / bottom-right corners
            path.moveTo(left, bottom - cornerArm);
            path.lineTo(left, bottom - r);
            if (r > 0) {
                path.quadTo(left, bottom, left + r, bottom);
            } else {
                path.lineTo(left, bottom);
            }
            path.lineTo(right - r, bottom);
            if (r > 0) {
                path.quadTo(right, bottom, right, bottom - r);
            } else {
                path.lineTo(right, bottom);
            }
            path.lineTo(right, bottom - cornerArm);

            canvas.drawPath(path, mStrokePaint);

        } else {
            // Full rectangle / rounded rectangle border
            if (r > 0) {
                canvas.drawRoundRect(new RectF(left, top, right, bottom), r, r, mStrokePaint);
            } else {
                canvas.drawRect(left, top, right, bottom, mStrokePaint);
            }
        }

        if (mGlassHighlight) {
            // Specular rim: light concentrated along the top edge, fading downward
            // over ~6dp. Subtle by design — no glow around the whole window.
            float inset = mStrokeWidth / 2.0f + Math.max(1f, mDensity);
            float rimLeft = left + inset;
            float rimRight = right - inset;
            float rimTop = top + inset;
            float rimBottom = Math.min(bottom, rimTop + 6 * mDensity);
            if (rimLeft < rimRight && rimTop < rimBottom) {
                mRimPaint.setShader(new LinearGradient(0, rimTop, 0, rimBottom,
                    0x59FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
                canvas.drawRect(rimLeft, rimTop, rimRight, rimBottom, mRimPaint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mStrokePaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mStrokePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
