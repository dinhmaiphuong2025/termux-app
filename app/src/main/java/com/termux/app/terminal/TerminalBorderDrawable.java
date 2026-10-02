package com.termux.app.terminal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom Drawable for terminal window border.
 * Supports:
 * - Standard full rounded rectangle border
 * - Hermes / Brackets style border (open left and right sides with corner accents)
 * - Custom header / footer text centered on top or bottom border
 */
public class TerminalBorderDrawable extends Drawable {

    public static final String STYLE_FULL = "full";
    public static final String STYLE_BRACKETS = "brackets";

    public static final String TITLE_POS_NONE = "none";
    public static final String TITLE_POS_TOP_CENTER = "top_center";
    public static final String TITLE_POS_BOTTOM_CENTER = "bottom_center";

    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBadgeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mStrokeWidth = 1;
    private int mCornerRadius = 12;
    private int mStrokeColor = 0xFF3D82F6;
    private String mStyle = STYLE_FULL;
    private String mTitlePosition = TITLE_POS_NONE;
    private String mTitleText = "";

    private float mDensity = 1.0f;

    public TerminalBorderDrawable() {
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mBadgeBgPaint.setStyle(Paint.Style.FILL);
        mBadgeBgPaint.setColor(Color.BLACK); // Badge background cuts through the stroke

        mTextPaint.setTypeface(Typeface.MONOSPACE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
    }

    public void setConfig(float density, int strokeWidth, int cornerRadius, int strokeColor,
                          String style, String titlePosition, String titleText) {
        mDensity = density;
        mStrokeWidth = Math.max(1, strokeWidth);
        mCornerRadius = Math.max(0, cornerRadius);
        mStrokeColor = strokeColor;
        mStyle = style != null ? style : STYLE_FULL;
        mTitlePosition = titlePosition != null ? titlePosition : TITLE_POS_NONE;
        mTitleText = titleText != null ? titleText.trim() : "";

        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setColor(mStrokeColor);

        mTextPaint.setColor(mStrokeColor);
        mTextPaint.setTextSize(9 * mDensity); // Compact badge size to fit nicely in border

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

        boolean hasTitle = !TextUtils.isEmpty(mTitleText) && !TITLE_POS_NONE.equals(mTitlePosition);
        boolean isTopTitle = hasTitle && TITLE_POS_TOP_CENTER.equals(mTitlePosition);
        boolean isBottomTitle = hasTitle && TITLE_POS_BOTTOM_CENTER.equals(mTitlePosition);

        float textWidth = hasTitle ? mTextPaint.measureText(mTitleText) : 0;
        float badgePaddingH = 6 * mDensity;
        float badgeWidth = textWidth + badgePaddingH * 2;
        float centerX = (left + right) / 2.0f;
        float badgeLeft = centerX - badgeWidth / 2.0f;
        float badgeRight = centerX + badgeWidth / 2.0f;

        // Ensure top and bottom stroke inset slightly if title is present so badge is fully visible
        float titleInset = hasTitle ? 4 * mDensity : 0;
        if (isTopTitle) {
            top += titleInset;
        } else if (isBottomTitle) {
            bottom -= titleInset;
        }

        if (STYLE_BRACKETS.equals(mStyle)) {
            // Hermes / Bracket style:
            // Top full line (or with title cut-out)
            // Bottom full line (or with title cut-out)
            // Left & Right: only draw corner teeth / accents (bracket tips)
            float cornerArm = Math.max(r + 14 * mDensity, 20 * mDensity);
            cornerArm = Math.min(cornerArm, (bottom - top) / 3.0f);

            Path path = new Path();

            // 1. Top edge & top-left corner
            if (isTopTitle) {
                // Left portion of top edge
                path.moveTo(badgeLeft, top);
                path.lineTo(left + r, top);
                if (r > 0) {
                    path.quadTo(left, top, left, top + r);
                } else {
                    path.lineTo(left, top);
                }
                path.lineTo(left, top + cornerArm);

                // Right portion of top edge
                path.moveTo(badgeRight, top);
                path.lineTo(right - r, top);
                if (r > 0) {
                    path.quadTo(right, top, right, top + r);
                } else {
                    path.lineTo(right, top);
                }
                path.lineTo(right, top + cornerArm);
            } else {
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
            }

            // 2. Bottom edge & bottom-left/right corners
            if (isBottomTitle) {
                // Left portion of bottom edge
                path.moveTo(badgeLeft, bottom);
                path.lineTo(left + r, bottom);
                if (r > 0) {
                    path.quadTo(left, bottom, left, bottom - r);
                } else {
                    path.lineTo(left, bottom);
                }
                path.lineTo(left, bottom - cornerArm);

                // Right portion of bottom edge
                path.moveTo(badgeRight, bottom);
                path.lineTo(right - r, bottom);
                if (r > 0) {
                    path.quadTo(right, bottom, right, bottom - r);
                } else {
                    path.lineTo(right, bottom);
                }
                path.lineTo(right, bottom - cornerArm);
            } else {
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
            }

            canvas.drawPath(path, mStrokePaint);

        } else {
            // Full rectangle / rounded rectangle border
            if (!hasTitle) {
                if (r > 0) {
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), r, r, mStrokePaint);
                } else {
                    canvas.drawRect(left, top, right, bottom, mStrokePaint);
                }
            } else {
                // Draw rounded rect with title cut-out
                Path path = new Path();
                if (isTopTitle) {
                    // Start from right of top title cut-out, go clockwise
                    path.moveTo(badgeRight, top);
                    path.lineTo(right - r, top);
                    if (r > 0) path.quadTo(right, top, right, top + r);
                    else path.lineTo(right, top);

                    path.lineTo(right, bottom - r);
                    if (r > 0) path.quadTo(right, bottom, right - r, bottom);
                    else path.lineTo(right, bottom);

                    path.lineTo(left + r, bottom);
                    if (r > 0) path.quadTo(left, bottom, left, bottom - r);
                    else path.lineTo(left, bottom);

                    path.lineTo(left, top + r);
                    if (r > 0) path.quadTo(left, top, left + r, top);
                    else path.lineTo(left, top);

                    path.lineTo(badgeLeft, top);
                } else { // isBottomTitle
                    // Start from left of bottom title cut-out, go clockwise
                    path.moveTo(badgeLeft, bottom);
                    path.lineTo(left + r, bottom);
                    if (r > 0) path.quadTo(left, bottom, left, bottom - r);
                    else path.lineTo(left, bottom);

                    path.lineTo(left, top + r);
                    if (r > 0) path.quadTo(left, top, left + r, top);
                    else path.lineTo(left, top);

                    path.lineTo(right - r, top);
                    if (r > 0) path.quadTo(right, top, right, top + r);
                    else path.lineTo(right, top);

                    path.lineTo(right, bottom - r);
                    if (r > 0) path.quadTo(right, bottom, right - r, bottom);
                    else path.lineTo(right, bottom);

                    path.lineTo(badgeRight, bottom);
                }
                canvas.drawPath(path, mStrokePaint);
            }
        }

        // Draw title text badge if present
        if (hasTitle) {
            float textY;
            Paint.FontMetrics fm = mTextPaint.getFontMetrics();
            float fontHeight = fm.descent - fm.ascent;
            float textBaselineOffset = (fontHeight / 2.0f) - fm.descent;

            if (isTopTitle) {
                textY = top + textBaselineOffset;
            } else {
                textY = bottom + textBaselineOffset;
            }

            // Draw clean background pill/badge cutout for text
            float bgPadV = 2 * mDensity;
            float bgTop = textY + fm.ascent - bgPadV;
            float bgBottom = textY + fm.descent + bgPadV;
            canvas.drawRect(badgeLeft, bgTop, badgeRight, bgBottom, mBadgeBgPaint);

            canvas.drawText(mTitleText, centerX, textY, mTextPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mStrokePaint.setAlpha(alpha);
        mTextPaint.setAlpha(alpha);
        mBadgeBgPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mStrokePaint.setColorFilter(colorFilter);
        mTextPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
