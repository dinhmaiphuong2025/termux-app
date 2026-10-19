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
    private final Paint mEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mStrokeWidth = 0;
    private int mCornerRadius = 1;
    private int mStrokeColor = 0xFFFFFFFF;
    private String mStyle = STYLE_FULL;
    private boolean mGlassEdge = false;

    private float mDensity = 1.0f;

    public TerminalBorderDrawable() {
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setStyle(Paint.Style.STROKE);
    }

    public void setConfig(float density, int strokeWidth, int cornerRadius, int strokeColor, String style) {
        mDensity = density;
        mStrokeWidth = Math.max(0, strokeWidth);
        mCornerRadius = Math.max(0, cornerRadius);
        mStrokeColor = strokeColor;
        mStyle = style != null ? style : STYLE_FULL;

        mStrokePaint.setStrokeWidth(Math.max(0, mStrokeWidth));
        mStrokePaint.setColor(mStrokeColor);

        invalidateSelf();
    }

    /**
     * Enable or disable the liquid-glass specular rim: a two-layer highlight
     * (diagonal gradient outer rim + bright inner band along the top edge) that
     * gives the window the 3D thickness of a glass droplet. Drawn independently
     * of the user border, so it also shows when the border stroke width is 0
     * (border disabled).
     */
    public void setGlassEdge(boolean enabled) {
        if (mGlassEdge == enabled) return;
        mGlassEdge = enabled;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        if (mStrokeWidth <= 0 && !mGlassEdge) return;

        float halfStroke = mStrokeWidth / 2.0f;
        float left = bounds.left + halfStroke;
        float top = bounds.top + halfStroke;
        float right = bounds.right - halfStroke;
        float bottom = bounds.bottom - halfStroke;
        float r = Math.min(mCornerRadius, Math.min(bounds.width(), bounds.height()) / 2.0f);

        canvas.save();
        if (r > 0) {
            Path clipPath = new Path();
            clipPath.addRoundRect(new RectF(bounds), r, r, Path.Direction.CW);
            canvas.clipPath(clipPath);
        }

        if (mStrokeWidth > 0) {
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
        }

        if (mGlassEdge) {
            // Liquid-glass specular rim, two layers:
            // 1. Outer rim: 1.2dp stroke with a 135° diagonal gradient
            //    (bright top-left, faint middle, mid bottom-right) like light
            //    wrapping around a glass droplet.
            float inset = mStrokeWidth / 2.0f + Math.max(1f, mDensity);
            float eLeft = left + inset;
            float eTop = top + inset;
            float eRight = right - inset;
            float eBottom = bottom - inset;
            float eRadius = Math.max(0f, r - inset);
            float rimWidth = 1.2f * mDensity;
            float hw = rimWidth / 2f;
            if (eLeft < eRight && eTop < eBottom) {
                mEdgePaint.setStrokeWidth(rimWidth);
                mEdgePaint.setShader(new LinearGradient(eLeft, eTop, eRight, eBottom,
                    new int[]{0xCCFFFFFF, 0x1AFFFFFF, 0x80FFFFFF},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
                if (eRadius > 0) {
                    canvas.drawRoundRect(new RectF(eLeft + hw, eTop + hw, eRight - hw, eBottom - hw),
                        eRadius, eRadius, mEdgePaint);
                } else {
                    canvas.drawRect(eLeft + hw, eTop + hw, eRight - hw, eBottom - hw, mEdgePaint);
                }
            }
            // 2. Inner highlight: a thin bright band just inside the top edge,
            //    giving the pane its 3D droplet thickness.
            float hlTop = eTop + rimWidth;
            float hlBottom = hlTop + 5f * mDensity;
            if (hlTop < eBottom) {
                mInnerPaint.setShader(new LinearGradient(0, hlTop, 0, Math.min(hlBottom, eBottom),
                    0x73FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
                canvas.drawRect(eLeft + rimWidth, hlTop, eRight - rimWidth, Math.min(hlBottom, eBottom), mInnerPaint);
            }
        }
        canvas.restore();
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
