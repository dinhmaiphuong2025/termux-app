package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;

/**
 * Floating customizable gesture pill button for quick tab switching and keyboard toggling.
 */
public class TermuxGesturePillView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mPillColor = 0x88FFFFFF;
    private float mCornerRadius = 100f;

    private boolean mIsDragging = false;
    private boolean mDragScheduled = false;

    private float mDownRawX, mDownRawY;
    private float mLastTouchRawX, mLastTouchRawY;
    private long mDownTime;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mDragStartRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDragScheduled) {
                mIsDragging = true;
                mDragScheduled = false;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                animate().scaleX(1.15f).scaleY(1.15f).alpha(1.0f).setDuration(100).start();
            }
        }
    };

    private TermuxActivity mActivity;

    private String mSwipeLeftAction = "prev_tab";
    private String mSwipeRightAction = "next_tab";
    private String mSwipeUpAction = "open_keyboard";
    private String mSwipeDownAction = "close_keyboard";
    private String mTapAction = "toggle_keyboard";

    public TermuxGesturePillView(Context context) {
        super(context);
        init();
    }

    public TermuxGesturePillView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TermuxGesturePillView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setStyle(Paint.Style.FILL);
        mStrokePaint.setStyle(Paint.Style.STROKE);
    }

    public void setActivity(TermuxActivity activity) {
        this.mActivity = activity;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float r = Math.min(mCornerRadius, Math.min(w, h) / 2.0f);
        float halfStroke = mStrokePaint.getStrokeWidth() / 2f;
        RectF rect = new RectF(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
        canvas.drawRoundRect(rect, r, r, mPaint);
        if (mStrokePaint.getStrokeWidth() > 0) {
            canvas.drawRoundRect(rect, r, r, mStrokePaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || getVisibility() != VISIBLE) {
            return super.onTouchEvent(event);
        }

        float density = getResources().getDisplayMetrics().density;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mDownRawX = event.getRawX();
                mDownRawY = event.getRawY();
                mLastTouchRawX = mDownRawX;
                mLastTouchRawY = mDownRawY;
                mDownTime = System.currentTimeMillis();
                mIsDragging = false;
                mDragScheduled = true;
                mHandler.postDelayed(mDragStartRunnable, 220);
                animate().alpha(1.0f).setDuration(80).start();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                float curRawX = event.getRawX();
                float curRawY = event.getRawY();
                float dist = (float) Math.hypot(curRawX - mDownRawX, curRawY - mDownRawY);

                if (!mIsDragging) {
                    if (dist > 16 * density) {
                        mHandler.removeCallbacks(mDragStartRunnable);
                        mDragScheduled = false;
                    }
                } else {
                    float dx = curRawX - mLastTouchRawX;
                    float dy = curRawY - mLastTouchRawY;
                    View parent = (View) getParent();
                    if (parent != null) {
                        int extraReserved = getExtraKeysReservedHeight();
                        int maxW = Math.max(0, parent.getWidth() - getWidth());
                        int maxH = Math.max(0, parent.getHeight() - getHeight() - extraReserved);
                        float newX = getX() + dx;
                        float newY = getY() + dy;
                        newX = Math.max(0, Math.min(maxW, newX));
                        newY = Math.max(0, Math.min(maxH, newY));
                        setX(newX);
                        setY(newY);
                    }
                    mLastTouchRawX = curRawX;
                    mLastTouchRawY = curRawY;
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mHandler.removeCallbacks(mDragStartRunnable);
                animate().scaleX(1.0f).scaleY(1.0f).alpha(0.85f).setDuration(100).start();

                if (mIsDragging) {
                    mIsDragging = false;
                    savePosition();
                    return true;
                }

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    float dx = event.getRawX() - mDownRawX;
                    float dy = event.getRawY() - mDownRawY;
                    float distance = (float) Math.hypot(dx, dy);
                    long duration = System.currentTimeMillis() - mDownTime;
                    float swipeThreshold = 18 * density;

                    if (distance >= swipeThreshold) {
                        if (Math.abs(dx) > Math.abs(dy)) {
                            if (dx > 0) {
                                executeAction(mSwipeRightAction);
                            } else {
                                executeAction(mSwipeLeftAction);
                            }
                        } else {
                            if (dy > 0) {
                                executeAction(mSwipeDownAction);
                            } else {
                                executeAction(mSwipeUpAction);
                            }
                        }
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    } else if (duration < 350) {
                        executeAction(mTapAction);
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                }
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    private int getExtraKeysReservedHeight() {
        if (mActivity == null) return 0;
        View toolbarCard = mActivity.findViewById(com.termux.R.id.terminal_toolbar_card);
        if (toolbarCard != null && toolbarCard.getVisibility() == View.VISIBLE) {
            int h = toolbarCard.getHeight();
            if (h <= 0) {
                View viewPager = mActivity.findViewById(com.termux.R.id.terminal_toolbar_view_pager);
                if (viewPager != null && viewPager.getVisibility() == View.VISIBLE) {
                    ViewGroup.LayoutParams lp = viewPager.getLayoutParams();
                    if (lp != null && lp.height > 0) {
                        h = lp.height;
                    }
                }
            }
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) toolbarCard.getLayoutParams();
            if (lp != null && h > 0) {
                h += lp.bottomMargin;
            }
            return h;
        }
        return 0;
    }

    private void savePosition() {
        View parent = (View) getParent();
        if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0) return;
        int extraReserved = getExtraKeysReservedHeight();
        int maxW = parent.getWidth() - getWidth();
        int maxH = parent.getHeight() - getHeight() - extraReserved;
        if (maxW <= 0 || maxH <= 0) return;

        float xRatio = Math.max(0f, Math.min(1f, getX() / (float) maxW));
        float yRatio = Math.max(0f, Math.min(1f, getY() / (float) maxH));

        if (mActivity != null && mActivity.getPreferences() != null) {
            mActivity.getPreferences().setGesturePillPosXRatio(xRatio);
            mActivity.getPreferences().setGesturePillPosYRatio(yRatio);
        }
    }

    public void applyPosition() {
        View parent = (View) getParent();
        if (parent == null) return;

        parent.post(() -> {
            int parentW = parent.getWidth();
            int parentH = parent.getHeight();
            int pillW = getWidth();
            int pillH = getHeight();
            if (parentW <= 0 || parentH <= 0 || pillW <= 0 || pillH <= 0) return;

            int extraReserved = getExtraKeysReservedHeight();
            float xRatio = -1f;
            float yRatio = -1f;
            if (mActivity != null && mActivity.getPreferences() != null) {
                xRatio = mActivity.getPreferences().getGesturePillPosXRatio();
                yRatio = mActivity.getPreferences().getGesturePillPosYRatio();
            }

            int maxW = Math.max(1, parentW - pillW);
            int maxH = Math.max(1, parentH - pillH - extraReserved);

            float targetX;
            float targetY;

            if (xRatio >= 0f && xRatio <= 1f) {
                targetX = xRatio * maxW;
            } else {
                targetX = (parentW - pillW) / 2f;
            }

            if (yRatio >= 0f && yRatio <= 1f) {
                targetY = yRatio * maxH;
            } else {
                float density = getResources().getDisplayMetrics().density;
                targetY = maxH - (8 * density);
            }

            setX(Math.max(0, Math.min(maxW, targetX)));
            setY(Math.max(0, Math.min(maxH, targetY)));
        });
    }

    public void applyConfig() {
        if (mActivity == null || mActivity.getPreferences() == null) return;
        TermuxAppSharedPreferences prefs = mActivity.getPreferences();

        boolean enabled = prefs.isGesturePillEnabled();
        if (!enabled) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        float density = getResources().getDisplayMetrics().density;
        int pillWidth = (int) (prefs.getGesturePillWidth() * density);
        int pillHeight = (int) (prefs.getGesturePillHeight() * density);
        mCornerRadius = prefs.getGesturePillCornerRadius() * density;

        String colorStr = prefs.getGesturePillColor();
        try {
            mPillColor = Color.parseColor(colorStr);
        } catch (Exception e) {
            mPillColor = 0x88FFFFFF;
        }
        mPaint.setColor(mPillColor);
        mStrokePaint.setColor(0x44FFFFFF);
        mStrokePaint.setStrokeWidth(1.2f * density);

        mSwipeLeftAction = prefs.getGesturePillSwipeLeft();
        mSwipeRightAction = prefs.getGesturePillSwipeRight();
        mSwipeUpAction = prefs.getGesturePillSwipeUp();
        mSwipeDownAction = prefs.getGesturePillSwipeDown();
        mTapAction = prefs.getGesturePillTap();

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null) {
            lp.width = pillWidth;
            lp.height = pillHeight;
            setLayoutParams(lp);
        }

        applyPosition();
        invalidate();
    }

    public void executeAction(String action) {
        if (action == null || mActivity == null) return;

        switch (action) {
            case "prev_tab":
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().switchToSession(false);
                }
                break;
            case "next_tab":
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().switchToSession(true);
                }
                break;
            case "open_keyboard":
                KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);
                if (mActivity.getPreferences() != null) {
                    mActivity.getPreferences().setSoftKeyboardEnabled(true);
                }
                KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
                if (mActivity.getTerminalView() != null) {
                    mActivity.getTerminalView().requestFocus();
                }
                break;
            case "close_keyboard":
                KeyboardUtils.hideSoftKeyboard(mActivity, mActivity.getTerminalView());
                break;
            case "toggle_keyboard":
                if (mActivity.getTermuxTerminalViewClient() != null) {
                    mActivity.getTermuxTerminalViewClient().onToggleSoftKeyboardRequest();
                }
                break;
            case "zoom_in":
                if (mActivity.getTermuxTerminalViewClient() != null) {
                    mActivity.getTermuxTerminalViewClient().changeFontSize(true);
                }
                break;
            case "zoom_out":
                if (mActivity.getTermuxTerminalViewClient() != null) {
                    mActivity.getTermuxTerminalViewClient().changeFontSize(false);
                }
                break;
            case "new_tab":
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().addNewSession(false, null);
                }
                break;
            case "close_tab":
                TerminalSession cur = mActivity.getCurrentSession();
                if (cur != null) {
                    cur.finishIfRunning();
                    if (mActivity.getTermuxTerminalSessionClient() != null) {
                        mActivity.getTermuxTerminalSessionClient().removeFinishedSession(cur);
                    }
                    if (mActivity.getTermuxTabBarController() != null) {
                        mActivity.getTermuxTabBarController().updateTabs();
                    }
                }
                break;
            case "toggle_extra_keys":
                mActivity.toggleTerminalToolbar();
                break;
            case "none":
            default:
                break;
        }
    }
}
