package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.activities.SettingsActivity;
import com.termux.shared.activity.ActivityUtils;
import com.termux.shared.termux.interact.TextInputDialogUtils;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.List;

/**
 * Controller for the Material 3 Pill-shaped Tab Bar placed at the top of the terminal screen.
 */
public class TermuxTabBarController {

    private final TermuxActivity mActivity;
    private final View mRootView;
    private final View mFullTabBarContainer;
    private final HorizontalScrollView mScrollView;
    private final LinearLayout mTabsContainer;
    private final FrameLayout mNewSessionBtn;
    private final FrameLayout mCloseSessionBtn;
    private final FrameLayout mSettingsBtn;

    private final View mSessionTitleContainer;
    private final TextView mSessionTitleText;

    public TermuxTabBarController(@NonNull TermuxActivity activity, @NonNull View rootView) {
        this.mActivity = activity;
        this.mRootView = rootView;
        this.mFullTabBarContainer = rootView.findViewById(R.id.terminal_tab_bar_full_container);
        this.mScrollView = rootView.findViewById(R.id.terminal_tabs_scroll_view);
        this.mTabsContainer = rootView.findViewById(R.id.terminal_tabs_container);
        this.mNewSessionBtn = rootView.findViewById(R.id.tab_new_session_btn);
        this.mCloseSessionBtn = rootView.findViewById(R.id.tab_close_session_btn);
        this.mSettingsBtn = rootView.findViewById(R.id.tab_settings_btn);

        this.mSessionTitleContainer = rootView.findViewById(R.id.terminal_session_title_container);
        this.mSessionTitleText = rootView.findViewById(R.id.terminal_session_title_text);

        if (mSessionTitleText != null) {
            // Short click: switch to next session
            mSessionTitleText.setOnClickListener(v -> {
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().switchToSession(true);
                }
            });
            // Long click: rename current session
            mSessionTitleText.setOnLongClickListener(v -> {
                TerminalSession currentSession = mActivity.getCurrentSession();
                if (currentSession != null && mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().renameSession(currentSession);
                }
                return true;
            });
        }

        if (mNewSessionBtn != null) {
            // Short click: create a regular new session
            mNewSessionBtn.setOnClickListener(v -> {
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().addNewSession(false, null);
                }
            });

            // Long click: create a named or failsafe new session
            mNewSessionBtn.setOnLongClickListener(v -> {
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    TextInputDialogUtils.textInput(mActivity, R.string.title_create_named_session, null,
                        R.string.action_create_named_session_confirm, text -> mActivity.getTermuxTerminalSessionClient().addNewSession(false, text),
                        R.string.action_new_session_failsafe, text -> mActivity.getTermuxTerminalSessionClient().addNewSession(true, text),
                        -1, null, null);
                }
                return true;
            });
        }

        if (mCloseSessionBtn != null) {
            mCloseSessionBtn.setOnClickListener(v -> {
                TermuxService service = mActivity.getTermuxService();
                if (service == null) return;
                TerminalSession currentSession = mActivity.getCurrentSession();
                if (currentSession == null) return;
                int sessionIndex = service.getIndexOfSession(currentSession);
                if (sessionIndex < 0) return;

                if (currentSession.isRunning()) {
                    new AlertDialog.Builder(mActivity)
                        .setTitle("Close Session")
                        .setMessage("Close session " + (sessionIndex + 1) + "?")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            currentSession.finishIfRunning();
                            if (mActivity.getTermuxTerminalSessionClient() != null) {
                                mActivity.getTermuxTerminalSessionClient().removeFinishedSession(currentSession);
                            }
                            updateTabs();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                } else {
                    if (mActivity.getTermuxTerminalSessionClient() != null) {
                        mActivity.getTermuxTerminalSessionClient().removeFinishedSession(currentSession);
                    }
                    updateTabs();
                }
            });
        }

        if (mSettingsBtn != null) {
            mSettingsBtn.setOnClickListener(v -> {
                ActivityUtils.startActivity(mActivity, new Intent(mActivity, SettingsActivity.class));
            });
        }
    }

    public void setVisibility(int visibility) {
        if (mRootView != null) {
            mRootView.setVisibility(visibility);
        }
    }

    private Drawable createTabDrawable(boolean isActive, int cornerRadius, int strokeWidth, int strokeColor, int activeAccentColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);

        boolean cornerEnabled = mActivity.getPreferences() == null || mActivity.getPreferences().isTabCornerEnabled();
        int effectiveCornerRadius = cornerEnabled ? cornerRadius : 0;
        gd.setCornerRadius(effectiveCornerRadius);

        boolean glassEnabled = mActivity.getPreferences() != null
            && mActivity.getPreferences().isGlassEffectEnabled()
            && mActivity.getPreferences().isTabBarGlassEnabled();
        float density = mActivity.getResources().getDisplayMetrics().density;

        boolean borderEnabled = mActivity.getPreferences() == null || mActivity.getPreferences().isTabBorderEnabled();
        boolean keepLayout = mActivity.getPreferences() != null && mActivity.getPreferences().isKeepBorderLayoutEnabled();

        int effectiveStrokeWidth = (borderEnabled || keepLayout)
            ? (strokeWidth > 0 ? strokeWidth : (glassEnabled ? Math.max(1, Math.round(1f * density)) : 0))
            : 0;

        int targetStrokeColor = isActive ? activeAccentColor : strokeColor;

        if (isActive) {
            if (glassEnabled) {
                float tintFactor = mActivity.getPreferences() != null
                    ? Math.max(0f, Math.min(1f, mActivity.getPreferences().getGlassTintOpacity() / 100f)) : 1f;
                int alpha = Math.round(0x55 * tintFactor);
                int activeTint = strokeWidth > 0
                    ? Color.argb(Math.round(0x60 * tintFactor), Color.red(activeAccentColor), Color.green(activeAccentColor), Color.blue(activeAccentColor))
                    : Color.argb(alpha, Color.red(activeAccentColor), Color.green(activeAccentColor), Color.blue(activeAccentColor));
                gd.setColor(activeTint);
                if (effectiveStrokeWidth > 0) {
                    gd.setStroke(effectiveStrokeWidth, (!borderEnabled && keepLayout) ? Color.TRANSPARENT : activeAccentColor);
                }
            } else {
                int bgTint = Color.argb(0x3D, Color.red(activeAccentColor), Color.green(activeAccentColor), Color.blue(activeAccentColor));
                gd.setColor(bgTint);
                if (effectiveStrokeWidth > 0) {
                    gd.setStroke(effectiveStrokeWidth, (!borderEnabled && keepLayout) ? Color.TRANSPARENT : activeAccentColor);
                }
            }
        } else {
            if (glassEnabled) {
                float tintFactor = mActivity.getPreferences() != null
                    ? Math.max(0f, Math.min(1f, mActivity.getPreferences().getGlassTintOpacity() / 100f)) : 1f;
                int alpha = Math.round(0x22 * tintFactor);
                gd.setColor(Color.argb(alpha, 0x0A, 0x0F, 0x19));
                if (effectiveStrokeWidth > 0) {
                    int baseBorder = strokeWidth > 0
                        ? Color.argb(0x44, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
                        : 0x2AFFFFFF;
                    gd.setStroke(effectiveStrokeWidth, (!borderEnabled && keepLayout) ? Color.TRANSPARENT : baseBorder);
                }
            } else {
                gd.setColor(0x22FFFFFF);
                if (effectiveStrokeWidth > 0) {
                    gd.setStroke(effectiveStrokeWidth, (!borderEnabled && keepLayout) ? Color.TRANSPARENT : 0x33FFFFFF);
                }
            }
        }
        return gd;
    }

    /**
     * Safely cuts a string to at most maxCodePoints code points without breaking surrogate pairs.
     */
    private static String truncateCodePoints(String str, int maxCodePoints) {
        if (str == null) return "";
        int codePointCount = str.codePointCount(0, str.length());
        if (codePointCount <= maxCodePoints) return str;
        int endOffset = str.offsetByCodePoints(0, maxCodePoints);
        return str.substring(0, endOffset);
    }

    @SuppressLint("SetTextI18n")
    public void updateTabs() {
        if (mRootView == null || !mActivity.isVisible()) return;

        float density = mActivity.getResources().getDisplayMetrics().density;

        // Check if tab bar is enabled in preferences
        boolean isEnabled = mActivity.getPreferences() != null && mActivity.getPreferences().isTabBarEnabled();
        boolean isShowSessionTitleWithoutTabBar = mActivity.getPreferences() != null && mActivity.getPreferences().isShowSessionNameWithoutTabBar();

        if (!isEnabled) {
            if (!isShowSessionTitleWithoutTabBar) {
                setVisibility(View.GONE);
                return;
            }

            // Compact text-only mode: shrink height to 20dp
            ViewGroup.LayoutParams rootLp = mRootView.getLayoutParams();
            if (rootLp != null) {
                rootLp.height = (int) (20 * density);
                mRootView.setLayoutParams(rootLp);
                mRootView.setPadding(0, 0, 0, 0);
            }
            if (mSessionTitleContainer != null) {
                mSessionTitleContainer.setPadding((int) (6 * density), (int) (1 * density), (int) (6 * density), (int) (1 * density));
            }

            setVisibility(View.VISIBLE);
            if (mFullTabBarContainer != null) mFullTabBarContainer.setVisibility(View.GONE);
            if (mSessionTitleContainer != null) mSessionTitleContainer.setVisibility(View.VISIBLE);

            TermuxService service = mActivity.getTermuxService();
            TerminalSession currentSession = mActivity.getCurrentSession();
            if (service == null || currentSession == null) return;

            int indexOfSession = service.getIndexOfSession(currentSession);
            int sessionNum = (indexOfSession >= 0 ? indexOfSession : 0) + 1;

            String rawName = currentSession.mSessionName;
            if (TextUtils.isEmpty(rawName)) {
                rawName = currentSession.getTitle();
            }
            if (rawName != null) {
                rawName = rawName.trim();
            }

            int activeTitleLength = 12;
            String alignment = "center";

            if (mActivity.getPreferences() != null) {
                activeTitleLength = mActivity.getPreferences().getTabActiveTitleLength();
                alignment = mActivity.getPreferences().getSessionNameAlignment();
            }

            String fullTitle;
            if (!TextUtils.isEmpty(rawName) && activeTitleLength > 0) {
                fullTitle = sessionNum + ". " + truncateCodePoints(rawName, activeTitleLength);
            } else {
                fullTitle = String.valueOf(sessionNum);
            }

            if (mSessionTitleText != null) {
                mSessionTitleText.setText(fullTitle);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSessionTitleText.getLayoutParams();
                if (lp != null) {
                    if ("left".equalsIgnoreCase(alignment)) {
                        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                    } else if ("right".equalsIgnoreCase(alignment)) {
                        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
                    } else {
                        lp.gravity = Gravity.CENTER;
                    }
                    mSessionTitleText.setLayoutParams(lp);
                }
            }
            return;
        }

        // Full Tab Bar Mode: reset height to 36dp and standard padding
        ViewGroup.LayoutParams rootLp = mRootView.getLayoutParams();
        if (rootLp != null) {
            rootLp.height = (int) (36 * density);
            mRootView.setLayoutParams(rootLp);
            mRootView.setPadding(0, 0, 0, 0);
        }
        int pad = (int) (4 * density);
        if (mFullTabBarContainer != null) {
            mFullTabBarContainer.setPadding(pad, pad, pad, pad);
        }

        setVisibility(View.VISIBLE);
        if (mFullTabBarContainer != null) mFullTabBarContainer.setVisibility(View.VISIBLE);
        if (mSessionTitleContainer != null) mSessionTitleContainer.setVisibility(View.GONE);
        if (mTabsContainer == null) return;
        mTabsContainer.removeAllViews();

        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        List<TermuxSession> termuxSessions = service.getTermuxSessions();
        if (termuxSessions == null || termuxSessions.isEmpty()) return;

        TerminalSession currentSession = mActivity.getCurrentSession();
        LayoutInflater inflater = LayoutInflater.from(mActivity);

        boolean cornerEnabled = mActivity.getPreferences() == null || mActivity.getPreferences().isTabCornerEnabled();
        int cornerRadius = cornerEnabled ? (int) (1 * density) : 0;
        boolean borderEnabled = mActivity.getPreferences() == null || mActivity.getPreferences().isTabBorderEnabled();
        boolean keepLayout = mActivity.getPreferences() != null && mActivity.getPreferences().isKeepBorderLayoutEnabled();
        int configuredBorderWidth = (int) (1 * density);
        int borderColor = Color.parseColor("#FFFFFF");
        int activeAccentColor = Color.parseColor("#FFFFFF");
        int buttonWidth = (int) (28 * density);
        int newBtnWidth = (int) (28 * density);
        int closeBtnWidth = (int) (28 * density);
        int settingsBtnWidth = (int) (28 * density);
        int activeTitleLength = 12;

        if (mActivity.getPreferences() != null) {
            cornerEnabled = mActivity.getPreferences().isTabCornerEnabled();
            cornerRadius = cornerEnabled ? (int) (mActivity.getPreferences().getTabCornerRadius() * density) : 0;
            borderEnabled = mActivity.getPreferences().isTabBorderEnabled();
            keepLayout = mActivity.getPreferences().isKeepBorderLayoutEnabled();
            configuredBorderWidth = (int) (mActivity.getPreferences().getTabBorderWidth() * density);
            buttonWidth = (int) (mActivity.getPreferences().getTabButtonWidth() * density);
            newBtnWidth = (int) (mActivity.getPreferences().getTabNewButtonWidth() * density);
            closeBtnWidth = (int) (mActivity.getPreferences().getTabCloseButtonWidth() * density);
            settingsBtnWidth = (int) (mActivity.getPreferences().getTabSettingsButtonWidth() * density);
            activeTitleLength = mActivity.getPreferences().getTabActiveTitleLength();
            String customColor = mActivity.getPreferences().getTabBorderColor();
            try {
                borderColor = Color.parseColor(customColor);
            } catch (Exception ignored) {}
            String customActiveColor = mActivity.getPreferences().getTabActiveColor();
            try {
                activeAccentColor = Color.parseColor(customActiveColor);
            } catch (Exception ignored) {}
        }
        int borderWidth = (borderEnabled || keepLayout) ? configuredBorderWidth : 0;

        // Style the new-session, close-session and settings buttons
        if (mNewSessionBtn != null) {
            ViewGroup.LayoutParams lp = mNewSessionBtn.getLayoutParams();
            if (lp != null) {
                lp.width = newBtnWidth;
                mNewSessionBtn.setLayoutParams(lp);
            }
            mNewSessionBtn.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor, activeAccentColor));
        }
        if (mCloseSessionBtn != null) {
            ViewGroup.LayoutParams lp = mCloseSessionBtn.getLayoutParams();
            if (lp != null) {
                lp.width = closeBtnWidth;
                mCloseSessionBtn.setLayoutParams(lp);
            }
            mCloseSessionBtn.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor, activeAccentColor));
        }
        if (mSettingsBtn != null) {
            ViewGroup.LayoutParams lp = mSettingsBtn.getLayoutParams();
            if (lp != null) {
                lp.width = settingsBtnWidth;
                mSettingsBtn.setLayoutParams(lp);
            }
            mSettingsBtn.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor, activeAccentColor));
        }

        View activeTabView = null;

        for (int i = 0; i < termuxSessions.size(); i++) {
            TermuxSession termuxSession = termuxSessions.get(i);
            TerminalSession session = termuxSession.getTerminalSession();
            if (session == null) continue;

            View tabItem = inflater.inflate(R.layout.item_terminal_tab, mTabsContainer, false);
            TextView titleView = tabItem.findViewById(R.id.tab_item_title);

            boolean isCurrent = (session == currentSession);

            String rawName = session.mSessionName;
            if (TextUtils.isEmpty(rawName)) {
                rawName = session.getTitle();
            }
            if (rawName != null) {
                rawName = rawName.trim();
            }

            int sessionNum = i + 1;

            if (isCurrent) {
                activeTabView = tabItem;
                tabItem.setBackground(createTabDrawable(true, cornerRadius, borderWidth, borderColor, activeAccentColor));
                tabItem.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
                tabItem.setMinimumWidth(buttonWidth);
                titleView.setTextColor(0xFFFFFFFF);
                titleView.setTextSize(12);

                String fullTitle;
                if (!TextUtils.isEmpty(rawName) && activeTitleLength > 0) {
                    fullTitle = sessionNum + ". " + truncateCodePoints(rawName, activeTitleLength);
                } else {
                    fullTitle = String.valueOf(sessionNum);
                }
                titleView.setText(fullTitle);
            } else {
                tabItem.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor, activeAccentColor));
                tabItem.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
                tabItem.setMinimumWidth(buttonWidth);
                titleView.setTextColor(0xB3FFFFFF);
                titleView.setTextSize(11);

                // Compact format for inactive tabs: number + first 4 characters of name
                String compactTitle;
                if (!TextUtils.isEmpty(rawName)) {
                    compactTitle = sessionNum + ". " + truncateCodePoints(rawName, 4);
                } else {
                    compactTitle = String.valueOf(sessionNum);
                }
                titleView.setText(compactTitle);
            }

            // Tab click -> switch session
            tabItem.setOnClickListener(v -> {
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().setCurrentSession(session);
                }
            });

            // Long click -> rename session dialog
            tabItem.setOnLongClickListener(v -> {
                if (mActivity.getTermuxTerminalSessionClient() != null) {
                    mActivity.getTermuxTerminalSessionClient().renameSession(session);
                }
                return true;
            });

            mTabsContainer.addView(tabItem);
        }

        // Auto-scroll to ensure active tab is visible and not hidden behind buttons
        if (mScrollView != null && activeTabView != null) {
            final View targetView = activeTabView;
            mScrollView.post(() -> {
                int targetLeft = targetView.getLeft();
                int scrollX = Math.max(0, targetLeft - (int) (12 * density));
                mScrollView.smoothScrollTo(scrollX, 0);
            });
        }
    }
}
