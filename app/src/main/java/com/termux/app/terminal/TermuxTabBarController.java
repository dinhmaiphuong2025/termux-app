package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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
    private final HorizontalScrollView mScrollView;
    private final LinearLayout mTabsContainer;
    private final FrameLayout mNewSessionBtn;
    private final FrameLayout mSettingsBtn;

    public TermuxTabBarController(@NonNull TermuxActivity activity, @NonNull View rootView) {
        this.mActivity = activity;
        this.mRootView = rootView;
        this.mScrollView = rootView.findViewById(R.id.terminal_tabs_scroll_view);
        this.mTabsContainer = rootView.findViewById(R.id.terminal_tabs_container);
        this.mNewSessionBtn = rootView.findViewById(R.id.tab_new_session_btn);
        this.mSettingsBtn = rootView.findViewById(R.id.tab_settings_btn);

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

    private static GradientDrawable createTabDrawable(boolean isActive, int cornerRadius, int strokeWidth, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(cornerRadius);

        if (isActive) {
            int bgTint = Color.argb(0x33, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
            gd.setColor(bgTint);
            if (strokeWidth > 0) {
                gd.setStroke(strokeWidth, strokeColor);
            }
        } else {
            gd.setColor(0x22FFFFFF);
            if (strokeWidth > 0) {
                gd.setStroke(strokeWidth, 0x33FFFFFF);
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
        if (mTabsContainer == null || !mActivity.isVisible()) return;

        // Check if tab bar is enabled in preferences
        boolean isEnabled = mActivity.getPreferences() != null && mActivity.getPreferences().isTabBarEnabled();
        if (!isEnabled) {
            setVisibility(View.GONE);
            return;
        }

        setVisibility(View.VISIBLE);
        mTabsContainer.removeAllViews();

        TermuxService service = mActivity.getTermuxService();
        if (service == null) return;

        List<TermuxSession> termuxSessions = service.getTermuxSessions();
        if (termuxSessions == null || termuxSessions.isEmpty()) return;

        TerminalSession currentSession = mActivity.getCurrentSession();
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        float density = mActivity.getResources().getDisplayMetrics().density;

        int cornerRadius = (int) (16 * density);
        int borderWidth = (int) (1 * density);
        int borderColor = Color.parseColor("#3D82F6");

        if (mActivity.getPreferences() != null) {
            cornerRadius = (int) (mActivity.getPreferences().getTabCornerRadius() * density);
            borderWidth = (int) (mActivity.getPreferences().getTabBorderWidth() * density);
            String customColor = mActivity.getPreferences().getTabBorderColor();
            try {
                borderColor = Color.parseColor(customColor);
            } catch (Exception ignored) {}
        }

        // Style the new-session and settings buttons
        if (mNewSessionBtn != null) {
            mNewSessionBtn.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor));
        }
        if (mSettingsBtn != null) {
            mSettingsBtn.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor));
        }

        View activeTabView = null;

        for (int i = 0; i < termuxSessions.size(); i++) {
            TermuxSession termuxSession = termuxSessions.get(i);
            TerminalSession session = termuxSession.getTerminalSession();
            if (session == null) continue;

            View tabItem = inflater.inflate(R.layout.item_terminal_tab, mTabsContainer, false);
            TextView titleView = tabItem.findViewById(R.id.tab_item_title);
            ImageView closeBtn = tabItem.findViewById(R.id.tab_item_close);

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
                tabItem.setBackground(createTabDrawable(true, cornerRadius, borderWidth, borderColor));
                tabItem.setPadding((int) (10 * density), 0, (int) (6 * density), 0);
                titleView.setTextColor(0xFFFFFFFF);
                titleView.setTextSize(12);

                String fullTitle;
                if (!TextUtils.isEmpty(rawName)) {
                    fullTitle = sessionNum + ". " + truncateCodePoints(rawName, 12);
                } else {
                    fullTitle = String.valueOf(sessionNum);
                }
                titleView.setText(fullTitle);

                // Active tab displays the close button
                closeBtn.setVisibility(View.VISIBLE);
            } else {
                tabItem.setBackground(createTabDrawable(false, cornerRadius, borderWidth, borderColor));
                tabItem.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
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

                // Inactive tabs hide the close button to save space
                closeBtn.setVisibility(View.GONE);
            }

            // Tab click -> switch session
            final int sessionIndex = i;
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

            // Close button click (only visible on active tab)
            closeBtn.setOnClickListener(v -> {
                if (session.isRunning()) {
                    new AlertDialog.Builder(mActivity)
                        .setTitle(R.string.title_rename_session)
                        .setMessage("Close session " + (sessionIndex + 1) + "?")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            session.finishIfRunning();
                            if (mActivity.getTermuxTerminalSessionClient() != null) {
                                mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                            }
                            updateTabs();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                } else {
                    if (mActivity.getTermuxTerminalSessionClient() != null) {
                        mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                    }
                    updateTabs();
                }
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
