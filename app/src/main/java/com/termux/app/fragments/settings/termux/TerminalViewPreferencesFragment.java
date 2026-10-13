package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.fragments.settings.ColorPickerDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

@Keep
public class TerminalViewPreferencesFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = "TerminalViewPreferencesFragment";

    private ActivityResultLauncher<String> mPickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register visual image picker via Storage Access Framework / OpenDocument
        mPickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                Context context = getContext();
                if (context == null) return;

                try {
                    // Try to persist read permission if supported
                    try {
                        context.getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {
                        // Some content providers don't support persistable permissions
                    }

                    TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                    if (prefs != null) {
                        prefs.setTerminalBackgroundImagePath(uri.toString());
                    }

                    EditTextPreference pathPref = findPreference("terminal_background_image_path");
                    if (pathPref != null) {
                        pathPref.setText(uri.toString());
                    }
                } catch (Exception e) {
                    Logger.logStackTraceWithMessage(LOG_TAG, "Failed to persist picked image URI", e);
                }
            }
        );
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalViewPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_terminal_view_preferences, rootKey);

        Preference pickPref = findPreference("terminal_background_pick");
        if (pickPref != null) {
            pickPref.setOnPreferenceClickListener(preference -> {
                if (mPickImageLauncher != null) {
                    try {
                        mPickImageLauncher.launch("image/*");
                    } catch (Exception e) {
                        Logger.logStackTraceWithMessage(LOG_TAG, "Failed to launch image picker", e);
                    }
                }
                return true;
            });
        }

        Preference clearPref = findPreference("terminal_background_clear");
        if (clearPref != null) {
            clearPref.setOnPreferenceClickListener(preference -> {
                TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                if (prefs != null) {
                    prefs.setTerminalBackgroundImagePath("");
                }
                EditTextPreference pathPref = findPreference("terminal_background_image_path");
                if (pathPref != null) {
                    pathPref.setText("");
                }
                TermuxActivity.updateTermuxActivityStyling(context, false);
                return true;
            });
        }

        Preference termColorPref = findPreference("terminal_border_color");
        if (termColorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                termColorPref.setSummary(prefs.getTerminalBorderColor());
            }
            termColorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getTerminalBorderColor() : "#FFFFFF";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Terminal Border Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setTerminalBorderColor(hexColor);
                    }
                    termColorPref.setSummary(hexColor);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                });
                return true;
            });
        }

        Preference tabColorPref = findPreference("tab_border_color");
        if (tabColorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                tabColorPref.setSummary(prefs.getTabBorderColor());
            }
            tabColorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getTabBorderColor() : "#FFFFFF";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Tab Border Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setTabBorderColor(hexColor);
                    }
                    tabColorPref.setSummary(hexColor);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                });
                return true;
            });
        }

        Preference pillColorPref = findPreference("gesture_pill_color");
        if (pillColorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                pillColorPref.setSummary(prefs.getGesturePillColor());
            }
            pillColorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getGesturePillColor() : "#80FFFFFF";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Gesture Pill Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setGesturePillColor(hexColor);
                    }
                    pillColorPref.setSummary(hexColor);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                });
                return true;
            });
        }

        Preference resetPosPref = findPreference("gesture_pill_reset_position");
        if (resetPosPref != null) {
            resetPosPref.setOnPreferenceClickListener(preference -> {
                TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                if (prefs != null) {
                    prefs.setGesturePillPosXRatio(-1f);
                    prefs.setGesturePillPosYRatio(-1f);
                }
                TermuxActivity.updateTermuxActivityStyling(context, false);
                Toast.makeText(context, "Reset gesture pill position", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        String[] reloadStyleKeys = new String[]{
            "show_tab_bar",
            "tab_corner_radius",
            "tab_border_width",
            "tab_button_width",
            "tab_new_button_width",
            "tab_close_button_width",
            "tab_settings_button_width",
            "tab_active_title_length",
            "tab_animation_enabled",
            "gesture_pill_enabled",
            "gesture_pill_width",
            "gesture_pill_height",
            "gesture_pill_corner_radius",
            "gesture_pill_swipe_left",
            "gesture_pill_swipe_right",
            "gesture_pill_swipe_up",
            "gesture_pill_swipe_down",
            "gesture_pill_tap",
            "terminal_border_enabled",
            "terminal_border_corner_radius",
            "terminal_border_width",
            "terminal_border_gaps",
            "terminal_horizontal_margin",
            "terminal_border_style",
            "terminal_background_opacity",
            "terminal_cell_background_transparency",
            "terminal_cell_background_opacity"
        };
        for (String prefKey : reloadStyleKeys) {
            Preference pref = findPreference(prefKey);
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                    return true;
                });
            }
        }
    }

}

class TerminalViewPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;

    private static TerminalViewPreferencesDataStore mInstance;

    private TerminalViewPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalViewPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalViewPreferencesDataStore(context);
        }
        return mInstance;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
            case "terminal_margin_adjustment":
                mPreferences.setTerminalMarginAdjustment(value);
                break;
            case "show_tab_bar":
                mPreferences.setTabBarEnabled(value);
                break;
            case "terminal_border_enabled":
                mPreferences.setTerminalBorderEnabled(value);
                break;
            case "tab_border_sync":
                mPreferences.setTabBorderSync(value);
                break;
            case "terminal_cell_background_transparency":
                mPreferences.setTerminalCellBackgroundTransparencyEnabled(value);
                break;
            case "tab_animation_enabled":
                mPreferences.setTabAnimationEnabled(value);
                break;
            case "gesture_pill_enabled":
                mPreferences.setGesturePillEnabled(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null || key == null) return defValue;

        switch (key) {
            case "terminal_margin_adjustment":
                return mPreferences.isTerminalMarginAdjustmentEnabled();
            case "show_tab_bar":
                return mPreferences.isTabBarEnabled();
            case "terminal_border_enabled":
                return mPreferences.isTerminalBorderEnabled();
            case "tab_border_sync":
                return mPreferences.isTabBorderSync();
            case "terminal_cell_background_transparency":
                return mPreferences.isTerminalCellBackgroundTransparencyEnabled();
            case "tab_animation_enabled":
                return mPreferences.isTabAnimationEnabled();
            case "gesture_pill_enabled":
                return mPreferences.isGesturePillEnabled();
            default:
                return defValue;
        }
    }

    @Override
    public void putInt(String key, int value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
            case "tab_corner_radius":
                mPreferences.setTabCornerRadius(value);
                break;
            case "tab_border_width":
                mPreferences.setTabBorderWidth(value);
                break;
            case "tab_button_width":
                mPreferences.setTabButtonWidth(value);
                break;
            case "tab_new_button_width":
                mPreferences.setTabNewButtonWidth(value);
                break;
            case "tab_close_button_width":
                mPreferences.setTabCloseButtonWidth(value);
                break;
            case "tab_settings_button_width":
                mPreferences.setTabSettingsButtonWidth(value);
                break;
            case "tab_active_title_length":
                mPreferences.setTabActiveTitleLength(value);
                break;
            case "terminal_border_corner_radius":
                mPreferences.setTerminalBorderCornerRadius(value);
                break;
            case "terminal_border_width":
                mPreferences.setTerminalBorderWidth(value);
                break;
            case "terminal_border_gaps":
                mPreferences.setTerminalBorderGaps(value);
                break;
            case "terminal_horizontal_margin":
                mPreferences.setTerminalHorizontalMargin(value);
                break;
            case "terminal_background_opacity":
                mPreferences.setTerminalBackgroundOpacity(value);
                break;
            case "terminal_cell_background_opacity":
                mPreferences.setTerminalCellBackgroundOpacity(value);
                break;
            case "gesture_pill_width":
                mPreferences.setGesturePillWidth(value);
                break;
            case "gesture_pill_height":
                mPreferences.setGesturePillHeight(value);
                break;
            case "gesture_pill_corner_radius":
                mPreferences.setGesturePillCornerRadius(value);
                break;
            default:
                break;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        if (mPreferences == null || key == null) return defValue;

        switch (key) {
            case "tab_corner_radius":
                return mPreferences.getTabCornerRadius();
            case "tab_border_width":
                return mPreferences.getTabBorderWidth();
            case "tab_button_width":
                return mPreferences.getTabButtonWidth();
            case "tab_new_button_width":
                return mPreferences.getTabNewButtonWidth();
            case "tab_close_button_width":
                return mPreferences.getTabCloseButtonWidth();
            case "tab_settings_button_width":
                return mPreferences.getTabSettingsButtonWidth();
            case "tab_active_title_length":
                return mPreferences.getTabActiveTitleLength();
            case "terminal_border_corner_radius":
                return mPreferences.getTerminalBorderCornerRadius();
            case "terminal_border_width":
                return mPreferences.getTerminalBorderWidth();
            case "terminal_border_gaps":
                return mPreferences.getTerminalBorderGaps();
            case "terminal_horizontal_margin":
                return mPreferences.getTerminalHorizontalMargin();
            case "terminal_background_opacity":
                return mPreferences.getTerminalBackgroundOpacity();
            case "terminal_cell_background_opacity":
                return mPreferences.getTerminalCellBackgroundOpacity();
            case "gesture_pill_width":
                return mPreferences.getGesturePillWidth();
            case "gesture_pill_height":
                return mPreferences.getGesturePillHeight();
            case "gesture_pill_corner_radius":
                return mPreferences.getGesturePillCornerRadius();
            default:
                return defValue;
        }
    }

    @Override
    public void putString(String key, @Nullable String value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
            case "terminal_border_color":
                mPreferences.setTerminalBorderColor(value != null ? value : "#FFFFFF");
                break;
            case "tab_border_color":
                mPreferences.setTabBorderColor(value != null ? value : "#FFFFFF");
                break;
            case "terminal_background_image_path":
                mPreferences.setTerminalBackgroundImagePath(value != null ? value : "");
                break;
            case "terminal_border_style":
                mPreferences.setTerminalBorderStyle(value != null ? value : "full");
                break;
            case "gesture_pill_color":
                mPreferences.setGesturePillColor(value != null ? value : "#80FFFFFF");
                break;
            case "gesture_pill_swipe_left":
                mPreferences.setGesturePillSwipeLeft(value != null ? value : "prev_tab");
                break;
            case "gesture_pill_swipe_right":
                mPreferences.setGesturePillSwipeRight(value != null ? value : "next_tab");
                break;
            case "gesture_pill_swipe_up":
                mPreferences.setGesturePillSwipeUp(value != null ? value : "open_keyboard");
                break;
            case "gesture_pill_swipe_down":
                mPreferences.setGesturePillSwipeDown(value != null ? value : "close_keyboard");
                break;
            case "gesture_pill_tap":
                mPreferences.setGesturePillTap(value != null ? value : "toggle_keyboard");
                break;
            default:
                break;
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (mPreferences == null || key == null) return defValue;

        switch (key) {
            case "terminal_border_color":
                return mPreferences.getTerminalBorderColor();
            case "tab_border_color":
                return mPreferences.getTabBorderColor();
            case "terminal_background_image_path":
                return mPreferences.getTerminalBackgroundImagePath();
            case "terminal_border_style":
                return mPreferences.getTerminalBorderStyle();
            case "gesture_pill_color":
                return mPreferences.getGesturePillColor();
            case "gesture_pill_swipe_left":
                return mPreferences.getGesturePillSwipeLeft();
            case "gesture_pill_swipe_right":
                return mPreferences.getGesturePillSwipeRight();
            case "gesture_pill_swipe_up":
                return mPreferences.getGesturePillSwipeUp();
            case "gesture_pill_swipe_down":
                return mPreferences.getGesturePillSwipeDown();
            case "gesture_pill_tap":
                return mPreferences.getGesturePillTap();
            default:
                return defValue;
        }
    }

}
