package com.termux.shared.termux.settings.preferences;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.android.PackageUtils;
import com.termux.shared.settings.preferences.AppSharedPreferences;
import com.termux.shared.settings.preferences.SharedPreferenceUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.logger.Logger;
import com.termux.shared.data.DataUtils;
import com.termux.shared.termux.TermuxUtils;
import com.termux.shared.termux.settings.preferences.TermuxPreferenceConstants.TERMUX_APP;

public class TermuxAppSharedPreferences extends AppSharedPreferences {

    private int MIN_FONTSIZE;
    private int MAX_FONTSIZE;
    private int DEFAULT_FONTSIZE;

    private static final String LOG_TAG = "TermuxAppSharedPreferences";

    private TermuxAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                TermuxConstants.TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                TermuxConstants.TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));

        setFontVariables(context);
    }

    /**
     * Get {@link TermuxAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link TermuxConstants#TERMUX_PACKAGE_NAME}.
     * @return Returns the {@link TermuxAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static TermuxAppSharedPreferences build(@NonNull final Context context) {
        Context termuxPackageContext = PackageUtils.getContextForPackage(context, TermuxConstants.TERMUX_PACKAGE_NAME);
        if (termuxPackageContext == null)
            return null;
        else
            return new TermuxAppSharedPreferences(termuxPackageContext);
    }

    /**
     * Get {@link TermuxAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link TermuxConstants#TERMUX_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link TermuxAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static TermuxAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context termuxPackageContext = TermuxUtils.getContextForPackageOrExitApp(context, TermuxConstants.TERMUX_PACKAGE_NAME, exitAppOnError);
        if (termuxPackageContext == null)
            return null;
        else
            return new TermuxAppSharedPreferences(termuxPackageContext);
    }



    public boolean shouldShowTerminalToolbar() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_TERMINAL_TOOLBAR, TERMUX_APP.DEFAULT_VALUE_SHOW_TERMINAL_TOOLBAR);
    }

    public void setShowTerminalToolbar(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_TERMINAL_TOOLBAR, value, false);
    }

    public boolean toogleShowTerminalToolbar() {
        boolean currentValue = shouldShowTerminalToolbar();
        setShowTerminalToolbar(!currentValue);
        return !currentValue;
    }



    public boolean isTerminalMarginAdjustmentEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_MARGIN_ADJUSTMENT, TERMUX_APP.DEFAULT_TERMINAL_MARGIN_ADJUSTMENT);
    }

    public void setTerminalMarginAdjustment(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_MARGIN_ADJUSTMENT, value, false);
    }



    public boolean isSoftKeyboardEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED);
    }

    public void setSoftKeyboardEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED, value, false);
    }

    public boolean isSoftKeyboardEnabledOnlyIfNoHardware() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE, TERMUX_APP.DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE);
    }

    public void setSoftKeyboardEnabledOnlyIfNoHardware(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE, value, false);
    }



    public boolean shouldKeepScreenOn() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_KEEP_SCREEN_ON, TERMUX_APP.DEFAULT_VALUE_KEEP_SCREEN_ON);
    }

    public void setKeepScreenOn(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_KEEP_SCREEN_ON, value, false);
    }



    public static int[] getDefaultFontSizes(Context context) {
        float dipInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        int[] sizes = new int[3];

        // This is a bit arbitrary and sub-optimal. We want to give a sensible default for minimum font size
        // to prevent invisible text due to zoom be mistake:
        sizes[1] = (int) (4f * dipInPixels); // min

        // http://www.google.com/design/spec/style/typography.html#typography-line-height
        int defaultFontSize = Math.round(12 * dipInPixels);
        // Make it divisible by 2 since that is the minimal adjustment step:
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        sizes[0] = defaultFontSize; // default

        sizes[2] = 256; // max

        return sizes;
    }

    public void setFontVariables(Context context) {
        int[] sizes = getDefaultFontSizes(context);

        DEFAULT_FONTSIZE = sizes[0];
        MIN_FONTSIZE = sizes[1];
        MAX_FONTSIZE = sizes[2];
    }

    public int getFontSize() {
        int fontSize = SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_FONTSIZE, DEFAULT_FONTSIZE);
        return DataUtils.clamp(fontSize, MIN_FONTSIZE, MAX_FONTSIZE);
    }

    public void setFontSize(int value) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, TERMUX_APP.KEY_FONTSIZE, value, false);
    }

    public void changeFontSize(boolean increase) {
        int fontSize = getFontSize();

        fontSize += (increase ? 1 : -1) * 2;
        fontSize = Math.max(MIN_FONTSIZE, Math.min(fontSize, MAX_FONTSIZE));

        setFontSize(fontSize);
    }



    public String getCurrentSession() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_CURRENT_SESSION, null, true);
    }

    public void setCurrentSession(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_CURRENT_SESSION, value, false);
    }



    public int getLogLevel() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_LOG_LEVEL, logLevel, false);
    }



    public int getLastNotificationId() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_LAST_NOTIFICATION_ID, TERMUX_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID);
    }

    public void setLastNotificationId(int notificationId) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_LAST_NOTIFICATION_ID, notificationId, false);
    }


    public synchronized int getAndIncrementAppShellNumberSinceBoot() {
        // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
        return SharedPreferenceUtils.getAndIncrementInt(mSharedPreferences, TERMUX_APP.KEY_APP_SHELL_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT, true, Integer.MAX_VALUE);
    }

    public synchronized void resetAppShellNumberSinceBoot() {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_APP_SHELL_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT, true);
    }

    public synchronized int getAndIncrementTerminalSessionNumberSinceBoot() {
        // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
        return SharedPreferenceUtils.getAndIncrementInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT, true, Integer.MAX_VALUE);
    }

    public synchronized void resetTerminalSessionNumberSinceBoot() {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT,
            TERMUX_APP.DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT, true);
    }


    public boolean isTerminalViewKeyLoggingEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, TERMUX_APP.DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED);
    }

    public void setTerminalViewKeyLoggingEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, value, false);
    }



    public boolean arePluginErrorNotificationsEnabled(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, TERMUX_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED);
        else
            return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED);
    }

    public void setPluginErrorNotificationsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, value, false);
    }



    public boolean areCrashReportNotificationsEnabled(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, TERMUX_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED);
       else
            return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, TERMUX_APP.DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED);
    }

    public void setCrashReportNotificationsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, value, false);
    }

    public boolean isTabBarEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_TAB_BAR, TERMUX_APP.DEFAULT_VALUE_SHOW_TAB_BAR);
    }

    public void setTabBarEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_TAB_BAR, value, false);
    }

    public int getTabCornerRadius() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_CORNER_RADIUS, TERMUX_APP.DEFAULT_VALUE_TAB_CORNER_RADIUS);
    }

    public void setTabCornerRadius(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_CORNER_RADIUS, value, false);
    }

    public int getTabBorderWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_WIDTH, TERMUX_APP.DEFAULT_VALUE_TAB_BORDER_WIDTH);
    }

    public void setTabBorderWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_WIDTH, value, false);
    }

    public boolean isTabBorderEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_ENABLED, TERMUX_APP.DEFAULT_VALUE_TAB_BORDER_ENABLED);
    }

    public void setTabBorderEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_ENABLED, value, false);
    }

    public boolean isTabCornerEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_CORNER_ENABLED, TERMUX_APP.DEFAULT_VALUE_TAB_CORNER_ENABLED);
    }

    public void setTabCornerEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_CORNER_ENABLED, value, false);
    }

    public boolean isTabBorderSync() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_SYNC, TERMUX_APP.DEFAULT_VALUE_TAB_BORDER_SYNC);
    }

    public void setTabBorderSync(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_SYNC, value, false);
    }

    public String getTabBorderColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_COLOR, TERMUX_APP.DEFAULT_VALUE_TAB_BORDER_COLOR, false);
    }

    public void setTabBorderColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_TAB_BORDER_COLOR, value, false);
    }

    public String getTabActiveColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_TAB_ACTIVE_COLOR, TERMUX_APP.DEFAULT_VALUE_TAB_ACTIVE_COLOR, false);
    }

    public void setTabActiveColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_TAB_ACTIVE_COLOR, value, true);
    }

    public int getTabButtonWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_BUTTON_WIDTH, TERMUX_APP.DEFAULT_VALUE_TAB_BUTTON_WIDTH);
    }

    public void setTabButtonWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_BUTTON_WIDTH, value, false);
    }

    public int getTabNewButtonWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_NEW_BUTTON_WIDTH, TERMUX_APP.DEFAULT_VALUE_TAB_NEW_BUTTON_WIDTH);
    }

    public void setTabNewButtonWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_NEW_BUTTON_WIDTH, value, false);
    }

    public int getTabCloseButtonWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_CLOSE_BUTTON_WIDTH, TERMUX_APP.DEFAULT_VALUE_TAB_CLOSE_BUTTON_WIDTH);
    }

    public void setTabCloseButtonWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_CLOSE_BUTTON_WIDTH, value, false);
    }

    public int getTabSettingsButtonWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_SETTINGS_BUTTON_WIDTH, TERMUX_APP.DEFAULT_VALUE_TAB_SETTINGS_BUTTON_WIDTH);
    }

    public void setTabSettingsButtonWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_SETTINGS_BUTTON_WIDTH, value, false);
    }

    public int getTabActiveTitleLength() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TAB_ACTIVE_TITLE_LENGTH, TERMUX_APP.DEFAULT_VALUE_TAB_ACTIVE_TITLE_LENGTH);
    }

    public void setTabActiveTitleLength(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TAB_ACTIVE_TITLE_LENGTH, value, false);
    }

    public boolean isTabBarGlassEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_BAR_GLASS_ENABLED, TERMUX_APP.DEFAULT_VALUE_TAB_BAR_GLASS_ENABLED);
    }

    public void setTabBarGlassEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_BAR_GLASS_ENABLED, value, false);
    }

    public boolean isTabAnimationEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_ANIMATION_ENABLED, TERMUX_APP.DEFAULT_VALUE_TAB_ANIMATION_ENABLED);
    }

    public void setTabAnimationEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TAB_ANIMATION_ENABLED, value, false);
    }

    public boolean isShowSessionNameWithoutTabBar() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_SESSION_NAME_WITHOUT_TAB_BAR, TERMUX_APP.DEFAULT_VALUE_SHOW_SESSION_NAME_WITHOUT_TAB_BAR);
    }

    public void setShowSessionNameWithoutTabBar(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_SHOW_SESSION_NAME_WITHOUT_TAB_BAR, value, false);
    }

    public String getSessionNameAlignment() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_SESSION_NAME_ALIGNMENT, TERMUX_APP.DEFAULT_VALUE_SESSION_NAME_ALIGNMENT, false);
    }

    public void setSessionNameAlignment(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_SESSION_NAME_ALIGNMENT, value, false);
    }

    public boolean isGesturePillEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_ENABLED, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_ENABLED);
    }

    public void setGesturePillEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_ENABLED, value, false);
    }

    public int getGesturePillWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_WIDTH, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_WIDTH);
    }

    public void setGesturePillWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_WIDTH, value, false);
    }

    public int getGesturePillHeight() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_HEIGHT, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_HEIGHT);
    }

    public void setGesturePillHeight(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_HEIGHT, value, false);
    }

    public int getGesturePillCornerRadius() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_CORNER_RADIUS, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_CORNER_RADIUS);
    }

    public void setGesturePillCornerRadius(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_CORNER_RADIUS, value, false);
    }

    public String getGesturePillColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_COLOR, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_COLOR, false);
    }

    public void setGesturePillColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_COLOR, value, false);
    }

    public float getGesturePillPosXRatio() {
        return SharedPreferenceUtils.getFloat(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_POS_X_RATIO, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_POS_X_RATIO);
    }

    public void setGesturePillPosXRatio(float value) {
        SharedPreferenceUtils.setFloat(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_POS_X_RATIO, value, false);
    }

    public float getGesturePillPosYRatio() {
        return SharedPreferenceUtils.getFloat(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_POS_Y_RATIO, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_POS_Y_RATIO);
    }

    public void setGesturePillPosYRatio(float value) {
        SharedPreferenceUtils.setFloat(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_POS_Y_RATIO, value, false);
    }

    public String getGesturePillSwipeLeft() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_LEFT, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_LEFT, false);
    }

    public void setGesturePillSwipeLeft(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_LEFT, value, false);
    }

    public String getGesturePillSwipeRight() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_RIGHT, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_RIGHT, false);
    }

    public void setGesturePillSwipeRight(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_RIGHT, value, false);
    }

    public String getGesturePillSwipeUp() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_UP, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_UP, false);
    }

    public void setGesturePillSwipeUp(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_UP, value, false);
    }

    public String getGesturePillSwipeDown() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_DOWN, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_DOWN, false);
    }

    public void setGesturePillSwipeDown(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_SWIPE_DOWN, value, false);
    }

    public String getGesturePillTap() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_TAP, TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_TAP, false);
    }

    public void setGesturePillTap(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_GESTURE_PILL_TAP, value, false);
    }

    public boolean isTerminalBorderEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_ENABLED, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_ENABLED);
    }

    public void setTerminalBorderEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_ENABLED, value, false);
    }

    public int getTerminalBorderCornerRadius() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_CORNER_RADIUS, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_CORNER_RADIUS);
    }

    public void setTerminalBorderCornerRadius(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_CORNER_RADIUS, value, false);
    }

    public int getTerminalBorderWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_WIDTH, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_WIDTH);
    }

    public void setTerminalBorderWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_WIDTH, value, false);
    }

    public String getTerminalBorderColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_COLOR, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_COLOR, false);
    }

    public void setTerminalBorderColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_COLOR, value, false);
    }

    public int getTerminalBorderGaps() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_GAPS, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_GAPS);
    }

    public void setTerminalBorderGaps(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_GAPS, value, false);
    }

    public int getTerminalHorizontalMargin() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_HORIZONTAL_MARGIN, TERMUX_APP.DEFAULT_VALUE_TERMINAL_HORIZONTAL_MARGIN);
    }

    public void setTerminalHorizontalMargin(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_HORIZONTAL_MARGIN, value, false);
    }

    public String getTerminalBorderStyle() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_STYLE, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_STYLE, false);
    }

    public void setTerminalBorderStyle(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_STYLE, value, false);
    }

    public String getTerminalBackgroundImagePath() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BACKGROUND_IMAGE_PATH, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BACKGROUND_IMAGE_PATH, false);
    }

    public void setTerminalBackgroundImagePath(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BACKGROUND_IMAGE_PATH, value, false);
    }

    public int getTerminalBackgroundOpacity() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BACKGROUND_OPACITY, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BACKGROUND_OPACITY);
    }

    public void setTerminalBackgroundOpacity(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BACKGROUND_OPACITY, value, false);
    }

    public boolean isTerminalCellBackgroundTransparencyEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_CELL_BACKGROUND_TRANSPARENCY, TERMUX_APP.DEFAULT_VALUE_TERMINAL_CELL_BACKGROUND_TRANSPARENCY);
    }

    public void setTerminalCellBackgroundTransparencyEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_CELL_BACKGROUND_TRANSPARENCY, value, false);
    }

    public int getTerminalCellBackgroundOpacity() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_CELL_BACKGROUND_OPACITY, TERMUX_APP.DEFAULT_VALUE_TERMINAL_CELL_BACKGROUND_OPACITY);
    }

    public void setTerminalCellBackgroundOpacity(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_CELL_BACKGROUND_OPACITY, value, false);
    }

    public boolean isTerminalSmoothScrollEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SMOOTH_SCROLL, TERMUX_APP.DEFAULT_VALUE_TERMINAL_SMOOTH_SCROLL);
    }

    public void setTerminalSmoothScrollEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SMOOTH_SCROLL, value, false);
    }

    public boolean isGlassEffectEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_GLASS_EFFECT_ENABLED, TERMUX_APP.DEFAULT_VALUE_GLASS_EFFECT_ENABLED);
    }

    public void setGlassEffectEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_GLASS_EFFECT_ENABLED, value, false);
    }

    public int getGlassTintOpacity() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_GLASS_TINT_OPACITY, TERMUX_APP.DEFAULT_VALUE_GLASS_TINT_OPACITY);
    }

    public void setGlassTintOpacity(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_GLASS_TINT_OPACITY, value, true);
    }

    public boolean isBackgroundBlurEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_BACKGROUND_BLUR_ENABLED, TERMUX_APP.DEFAULT_VALUE_BACKGROUND_BLUR_ENABLED);
    }

    public void setBackgroundBlurEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_BACKGROUND_BLUR_ENABLED, value, false);
    }

    public int getBackgroundBlurRadius() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_BACKGROUND_BLUR_RADIUS, TERMUX_APP.DEFAULT_VALUE_BACKGROUND_BLUR_RADIUS);
    }

    public void setBackgroundBlurRadius(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_BACKGROUND_BLUR_RADIUS, value, false);
    }

    public boolean isExtraKeysBarBackgroundSyncEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BACKGROUND_SYNC, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BACKGROUND_SYNC);
    }

    public void setExtraKeysBarBackgroundSyncEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BACKGROUND_SYNC, value, false);
    }

    public String getOuterBackgroundStyle() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_OUTER_BACKGROUND_STYLE, TERMUX_APP.DEFAULT_VALUE_OUTER_BACKGROUND_STYLE, false);
    }

    public void setOuterBackgroundStyle(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_OUTER_BACKGROUND_STYLE, value, false);
    }

    public boolean isTerminalShadowEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SHADOW_ENABLED, TERMUX_APP.DEFAULT_VALUE_TERMINAL_SHADOW_ENABLED);
    }

    public void setTerminalShadowEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_SHADOW_ENABLED, value, false);
    }

    public boolean isTerminalBorderCornerEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_CORNER_ENABLED, TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_CORNER_ENABLED);
    }

    public void setTerminalBorderCornerEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_TERMINAL_BORDER_CORNER_ENABLED, value, false);
    }

    public boolean isExtraKeysBarCornerEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_CORNER_ENABLED, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_CORNER_ENABLED);
    }

    public void setExtraKeysBarCornerEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_CORNER_ENABLED, value, false);
    }

    public boolean isKeepBorderLayoutEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_KEEP_BORDER_LAYOUT_ENABLED, TERMUX_APP.DEFAULT_VALUE_KEEP_BORDER_LAYOUT_ENABLED);
    }

    public void setKeepBorderLayoutEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_KEEP_BORDER_LAYOUT_ENABLED, value, false);
    }

    public int getExtraKeysCornerRadius() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_CORNER_RADIUS, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_CORNER_RADIUS);
    }

    public void setExtraKeysCornerRadius(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_CORNER_RADIUS, value, false);
    }

    public int getExtraKeysMargin() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_MARGIN, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_MARGIN);
    }

    public void setExtraKeysMargin(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_MARGIN, value, false);
    }

    public String getExtraKeysCustomJson() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_CUSTOM_JSON, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_CUSTOM_JSON, false);
    }

    public void setExtraKeysCustomJson(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_CUSTOM_JSON, value, false);
    }

    public boolean isExtraKeysUseCustom() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_USE_CUSTOM, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_USE_CUSTOM);
    }

    public void setExtraKeysUseCustom(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_USE_CUSTOM, value, false);
    }

    // Key Border
    public boolean isExtraKeysKeyBorderEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_ENABLED, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_KEY_BORDER_ENABLED);
    }

    public void setExtraKeysKeyBorderEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_ENABLED, value, false);
    }

    public int getExtraKeysKeyBorderWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_WIDTH, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_KEY_BORDER_WIDTH);
    }

    public void setExtraKeysKeyBorderWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_WIDTH, value, false);
    }

    public boolean isExtraKeysKeyBorderSync() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_SYNC, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_KEY_BORDER_SYNC);
    }

    public void setExtraKeysKeyBorderSync(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_SYNC, value, false);
    }

    public String getExtraKeysKeyBorderColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_COLOR, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_KEY_BORDER_COLOR, false);
    }

    public void setExtraKeysKeyBorderColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_KEY_BORDER_COLOR, value, false);
    }

    // Bar Border
    public boolean isExtraKeysBarBorderEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_ENABLED, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_ENABLED);
    }

    public void setExtraKeysBarBorderEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_ENABLED, value, false);
    }

    public int getExtraKeysBarBorderWidth() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_WIDTH, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_WIDTH);
    }

    public void setExtraKeysBarBorderWidth(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_WIDTH, value, false);
    }

    public boolean isExtraKeysBarBorderSync() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_SYNC, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_SYNC);
    }

    public void setExtraKeysBarBorderSync(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_SYNC, value, false);
    }

    public String getExtraKeysBarBorderColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_COLOR, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_COLOR, false);
    }

    public void setExtraKeysBarBorderColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_COLOR, value, false);
    }

    public int getExtraKeysBarBorderCornerRadius() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_CORNER_RADIUS, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_CORNER_RADIUS);
    }

    public void setExtraKeysBarBorderCornerRadius(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_CORNER_RADIUS, value, false);
    }

    public int getExtraKeysBarBorderGaps() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_GAPS, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_GAPS);
    }

    public void setExtraKeysBarBorderGaps(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_BAR_BORDER_GAPS, value, false);
    }

    public String getExtraKeysPreset() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_PRESET, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_PRESET, false);
    }

    public void setExtraKeysPreset(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_PRESET, value, false);
    }

    public int getExtraKeysTextSize() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_TEXT_SIZE, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_TEXT_SIZE);
    }

    public void setExtraKeysTextSize(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_TEXT_SIZE, value, false);
    }

    public int getExtraKeysHeightScale() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_HEIGHT_SCALE, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_HEIGHT_SCALE);
    }

    public void setExtraKeysHeightScale(int value) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_HEIGHT_SCALE, value, false);
    }

    public String getExtraKeysColorTheme() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_COLOR_THEME, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_COLOR_THEME, false);
    }

    public void setExtraKeysColorTheme(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_COLOR_THEME, value, false);
    }

    public boolean isExtraKeysFlatKeys() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_FLAT_KEYS, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_FLAT_KEYS);
    }

    public void setExtraKeysFlatKeys(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_FLAT_KEYS, value, false);
    }

    public String getExtraKeysPressedTextColor() {
        return SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_PRESSED_TEXT_COLOR, TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_PRESSED_TEXT_COLOR, false);
    }

    public void setExtraKeysPressedTextColor(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_APP.KEY_EXTRA_KEYS_PRESSED_TEXT_COLOR, value, true);
    }

    /**
     * Resets all custom UI settings (borders, colors, tab bar, extra keys, background) to their default values.
     */
    public void resetAllCustomUiSettingsToDefault() {
        setTabBarEnabled(TERMUX_APP.DEFAULT_VALUE_SHOW_TAB_BAR);
        setTabCornerRadius(TERMUX_APP.DEFAULT_VALUE_TAB_CORNER_RADIUS);
        setTabBorderWidth(TERMUX_APP.DEFAULT_VALUE_TAB_BORDER_WIDTH);
        setTabBorderColor(TERMUX_APP.DEFAULT_VALUE_TAB_BORDER_COLOR);
        setTabButtonWidth(TERMUX_APP.DEFAULT_VALUE_TAB_BUTTON_WIDTH);
        setTabNewButtonWidth(TERMUX_APP.DEFAULT_VALUE_TAB_NEW_BUTTON_WIDTH);
        setTabCloseButtonWidth(TERMUX_APP.DEFAULT_VALUE_TAB_CLOSE_BUTTON_WIDTH);
        setTabSettingsButtonWidth(TERMUX_APP.DEFAULT_VALUE_TAB_SETTINGS_BUTTON_WIDTH);
        setTabActiveTitleLength(TERMUX_APP.DEFAULT_VALUE_TAB_ACTIVE_TITLE_LENGTH);
        setTabAnimationEnabled(TERMUX_APP.DEFAULT_VALUE_TAB_ANIMATION_ENABLED);
        setShowSessionNameWithoutTabBar(TERMUX_APP.DEFAULT_VALUE_SHOW_SESSION_NAME_WITHOUT_TAB_BAR);
        setSessionNameAlignment(TERMUX_APP.DEFAULT_VALUE_SESSION_NAME_ALIGNMENT);

        setGesturePillEnabled(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_ENABLED);
        setGesturePillWidth(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_WIDTH);
        setGesturePillHeight(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_HEIGHT);
        setGesturePillCornerRadius(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_CORNER_RADIUS);
        setGesturePillColor(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_COLOR);
        setGesturePillPosXRatio(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_POS_X_RATIO);
        setGesturePillPosYRatio(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_POS_Y_RATIO);
        setGesturePillSwipeLeft(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_LEFT);
        setGesturePillSwipeRight(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_RIGHT);
        setGesturePillSwipeUp(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_UP);
        setGesturePillSwipeDown(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_SWIPE_DOWN);
        setGesturePillTap(TERMUX_APP.DEFAULT_VALUE_GESTURE_PILL_TAP);

        setTerminalBorderEnabled(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_ENABLED);
        setTerminalBorderCornerRadius(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_CORNER_RADIUS);
        setTerminalBorderWidth(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_WIDTH);
        setTerminalBorderColor(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_COLOR);
        setTerminalBorderGaps(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_GAPS);

        setTerminalBackgroundImagePath(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BACKGROUND_IMAGE_PATH);
        setTerminalBackgroundOpacity(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BACKGROUND_OPACITY);
        setTerminalCellBackgroundTransparencyEnabled(TERMUX_APP.DEFAULT_VALUE_TERMINAL_CELL_BACKGROUND_TRANSPARENCY);
        setTerminalCellBackgroundOpacity(TERMUX_APP.DEFAULT_VALUE_TERMINAL_CELL_BACKGROUND_OPACITY);
        setTerminalSmoothScrollEnabled(TERMUX_APP.DEFAULT_VALUE_TERMINAL_SMOOTH_SCROLL);
        setGlassEffectEnabled(TERMUX_APP.DEFAULT_VALUE_GLASS_EFFECT_ENABLED);
        setGlassTintOpacity(TERMUX_APP.DEFAULT_VALUE_GLASS_TINT_OPACITY);
        setBackgroundBlurEnabled(TERMUX_APP.DEFAULT_VALUE_BACKGROUND_BLUR_ENABLED);
        setBackgroundBlurRadius(TERMUX_APP.DEFAULT_VALUE_BACKGROUND_BLUR_RADIUS);
        setExtraKeysBarBackgroundSyncEnabled(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BACKGROUND_SYNC);
        setOuterBackgroundStyle(TERMUX_APP.DEFAULT_VALUE_OUTER_BACKGROUND_STYLE);
        setTerminalShadowEnabled(TERMUX_APP.DEFAULT_VALUE_TERMINAL_SHADOW_ENABLED);
        setTerminalBorderCornerEnabled(TERMUX_APP.DEFAULT_VALUE_TERMINAL_BORDER_CORNER_ENABLED);
        setExtraKeysBarCornerEnabled(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_CORNER_ENABLED);

        setExtraKeysCornerRadius(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_CORNER_RADIUS);
        setExtraKeysMargin(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_MARGIN);
        setExtraKeysTextSize(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_TEXT_SIZE);
        setExtraKeysHeightScale(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_HEIGHT_SCALE);
        setExtraKeysColorTheme(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_COLOR_THEME);
        setExtraKeysFlatKeys(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_FLAT_KEYS);
        setExtraKeysBarBorderEnabled(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_ENABLED);
        setExtraKeysBarBorderWidth(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_WIDTH);
        setExtraKeysBarBorderColor(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_COLOR);
        setExtraKeysBarBorderCornerRadius(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_CORNER_RADIUS);
        setExtraKeysBarBorderGaps(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_BAR_BORDER_GAPS);
        setExtraKeysCustomJson(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_CUSTOM_JSON);
        setExtraKeysPreset(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_PRESET);
        setExtraKeysUseCustom(TERMUX_APP.DEFAULT_VALUE_EXTRA_KEYS_USE_CUSTOM);
    }

}
