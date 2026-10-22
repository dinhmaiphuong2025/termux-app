package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.fragments.settings.ColorPickerDialogUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

@Keep
public class CustomUIPreferencesFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = "CustomUIPreferencesFragment";

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
                    TermuxActivity.updateTermuxActivityStyling(context, false);
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
        preferenceManager.setPreferenceDataStore(CustomUIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_custom_ui_preferences, rootKey);

        Preference pickPref = findPreference("terminal_background_pick");
        if (pickPref != null) {
            pickPref.setOnPreferenceClickListener(preference -> {
                if (mPickImageLauncher != null) {
                    mPickImageLauncher.launch("image/*");
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
                Toast.makeText(context, "Background image removed", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference colorPref = findPreference("terminal_border_color");
        if (colorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                colorPref.setSummary(prefs.getTerminalBorderColor());
            }
            colorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getTerminalBorderColor() : "#FFFFFF";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Window Border Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setTerminalBorderColor(hexColor);
                    }
                    colorPref.setSummary(hexColor);
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

        Preference barBorderColorPref = findPreference("extra_keys_bar_border_color");
        if (barBorderColorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                barBorderColorPref.setSummary(prefs.getExtraKeysBarBorderColor());
            }
            barBorderColorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getExtraKeysBarBorderColor() : "#FFFFFF";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Bar Border Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setExtraKeysBarBorderColor(hexColor);
                    }
                    barBorderColorPref.setSummary(hexColor);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                });
                return true;
            });
        }

        Preference pressedColorPref = findPreference("extra_keys_pressed_text_color");
        if (pressedColorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                String val = prefs.getExtraKeysPressedTextColor();
                pressedColorPref.setSummary(TextUtils.isEmpty(val) ? "Theme Default" : val);
            }
            pressedColorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getExtraKeysPressedTextColor() : "";
                if (TextUtils.isEmpty(current)) current = "#89B4FA";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Pressed Key Text Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setExtraKeysPressedTextColor(hexColor);
                    }
                    pressedColorPref.setSummary(hexColor);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                });
                return true;
            });
        }

        Preference presetPref = findPreference("extra_keys_preset");
        if (presetPref != null) {
            presetPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String presetVal = (String) newValue;
                if (!TextUtils.isEmpty(presetVal)) {
                    try {
                        new JSONArray(presetVal);
                        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                        if (prefs != null) {
                            prefs.setExtraKeysPreset(presetVal);
                            prefs.setExtraKeysCustomJson(presetVal);
                            prefs.setExtraKeysUseCustom(true);
                            SwitchPreferenceCompat customSwitch = findPreference("extra_keys_use_custom");
                            if (customSwitch != null) customSwitch.setChecked(true);
                            EditTextPreference customJsonPref = findPreference("extra_keys_custom_json");
                            if (customJsonPref != null) customJsonPref.setText(presetVal);
                            TermuxActivity.updateTermuxActivityStyling(context, false);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(context, "Invalid preset JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                return true;
            });
        }

        Preference customJsonPref = findPreference("extra_keys_custom_json");
        if (customJsonPref != null) {
            customJsonPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String jsonVal = (String) newValue;
                if (!TextUtils.isEmpty(jsonVal)) {
                    try {
                        new JSONArray(jsonVal);
                    } catch (JSONException e) {
                        Toast.makeText(context, "Invalid JSON format: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                if (prefs != null) {
                    prefs.setExtraKeysCustomJson(jsonVal != null ? jsonVal : "");
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                }
                return true;
            });
        }

        Preference useCustomPref = findPreference("extra_keys_use_custom");
        if (useCustomPref != null) {
            useCustomPref.setOnPreferenceChangeListener((preference, newValue) -> {
                TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                if (prefs != null) {
                    prefs.setExtraKeysUseCustom((Boolean) newValue);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                }
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
            "show_session_name_without_tab_bar",
            "session_name_alignment",
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
            "terminal_cell_background_opacity",
            "terminal_smooth_scroll",
            "glass_effect_enabled",
            "glass_tint_opacity",
            "background_blur_enabled",
            "background_blur_radius",
            "extra_keys_bar_background_sync",
            "outer_background_style",
            "terminal_shadow_enabled",
            "terminal_border_corner_enabled",
            "extra_keys_bar_corner_enabled",
            "keep_border_layout_enabled",
            "extra_keys_use_custom",
            "extra_keys_preset",
            "extra_keys_custom_json",
            "extra_keys_color_theme",
            "extra_keys_flat_keys",
            "extra_keys_margin",
            "extra_keys_text_size",
            "extra_keys_height_scale",
            "extra_keys_bar_border_enabled",
            "extra_keys_bar_border_width",
            "extra_keys_bar_border_corner_radius",
            "extra_keys_bar_border_gaps"
        };
        // Style reloads are dispatched post-save in CustomUIPreferencesDataStore
        // (putInt, putBoolean, putString) so the new values are always committed
        // to preferences before TermuxActivity receives the reload broadcast.
    }

}

class CustomUIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;

    private static CustomUIPreferencesDataStore mInstance;

    private CustomUIPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized CustomUIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CustomUIPreferencesDataStore(context);
        }
        return mInstance;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
            case "show_tab_bar":
                mPreferences.setTabBarEnabled(value);
                break;
            case "terminal_border_enabled":
                mPreferences.setTerminalBorderEnabled(value);
                break;
            case "tab_border_sync":
                mPreferences.setTabBorderSync(value);
                break;
            case "tab_border_enabled":
                mPreferences.setTabBorderEnabled(value);
                break;
            case "tab_corner_enabled":
                mPreferences.setTabCornerEnabled(value);
                break;
            case "tab_bar_glass_enabled":
                mPreferences.setTabBarGlassEnabled(value);
                break;
            case "terminal_cell_background_transparency":
                mPreferences.setTerminalCellBackgroundTransparencyEnabled(value);
                break;
            case "terminal_smooth_scroll":
                mPreferences.setTerminalSmoothScrollEnabled(value);
                break;
            case "glass_effect_enabled":
                mPreferences.setGlassEffectEnabled(value);
                break;
            case "background_blur_enabled":
                mPreferences.setBackgroundBlurEnabled(value);
                break;
            case "extra_keys_bar_background_sync":
                mPreferences.setExtraKeysBarBackgroundSyncEnabled(value);
                break;
            case "terminal_shadow_enabled":
                mPreferences.setTerminalShadowEnabled(value);
                break;
            case "terminal_border_corner_enabled":
                mPreferences.setTerminalBorderCornerEnabled(value);
                break;
            case "extra_keys_bar_corner_enabled":
                mPreferences.setExtraKeysBarCornerEnabled(value);
                break;
            case "keep_border_layout_enabled":
                mPreferences.setKeepBorderLayoutEnabled(value);
                break;
            case "extra_keys_use_custom":
                mPreferences.setExtraKeysUseCustom(value);
                break;
            case "tab_animation_enabled":
                mPreferences.setTabAnimationEnabled(value);
                break;
            case "show_session_name_without_tab_bar":
                mPreferences.setShowSessionNameWithoutTabBar(value);
                break;
            case "gesture_pill_enabled":
                mPreferences.setGesturePillEnabled(value);
                break;
            case "extra_keys_flat_keys":
                mPreferences.setExtraKeysFlatKeys(value);
                break;
            case "extra_keys_bar_border_enabled":
                mPreferences.setExtraKeysBarBorderEnabled(value);
                break;
            default:
                break;
        }
        TermuxActivity.updateTermuxActivityStyling(mContext, false);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null || key == null) return defValue;

        switch (key) {
            case "show_tab_bar":
                return mPreferences.isTabBarEnabled();
            case "terminal_border_enabled":
                return mPreferences.isTerminalBorderEnabled();
            case "tab_border_sync":
                return mPreferences.isTabBorderSync();
            case "tab_border_enabled":
                return mPreferences.isTabBorderEnabled();
            case "tab_corner_enabled":
                return mPreferences.isTabCornerEnabled();
            case "tab_bar_glass_enabled":
                return mPreferences.isTabBarGlassEnabled();
            case "terminal_cell_background_transparency":
                return mPreferences.isTerminalCellBackgroundTransparencyEnabled();
            case "terminal_smooth_scroll":
                return mPreferences.isTerminalSmoothScrollEnabled();
            case "glass_effect_enabled":
                return mPreferences.isGlassEffectEnabled();
            case "background_blur_enabled":
                return mPreferences.isBackgroundBlurEnabled();
            case "extra_keys_bar_background_sync":
                return mPreferences.isExtraKeysBarBackgroundSyncEnabled();
            case "terminal_shadow_enabled":
                return mPreferences.isTerminalShadowEnabled();
            case "terminal_border_corner_enabled":
                return mPreferences.isTerminalBorderCornerEnabled();
            case "extra_keys_bar_corner_enabled":
                return mPreferences.isExtraKeysBarCornerEnabled();
            case "keep_border_layout_enabled":
                return mPreferences.isKeepBorderLayoutEnabled();
            case "extra_keys_use_custom":
                return mPreferences.isExtraKeysUseCustom();
            case "tab_animation_enabled":
                return mPreferences.isTabAnimationEnabled();
            case "show_session_name_without_tab_bar":
                return mPreferences.isShowSessionNameWithoutTabBar();
            case "gesture_pill_enabled":
                return mPreferences.isGesturePillEnabled();
            case "extra_keys_flat_keys":
                return mPreferences.isExtraKeysFlatKeys();
            case "extra_keys_bar_border_enabled":
                return mPreferences.isExtraKeysBarBorderEnabled();
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
            case "glass_tint_opacity":
                mPreferences.setGlassTintOpacity(value);
                break;
            case "background_blur_radius":
                mPreferences.setBackgroundBlurRadius(value);
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
            case "extra_keys_margin":
                mPreferences.setExtraKeysMargin(value);
                break;
            case "extra_keys_text_size":
                mPreferences.setExtraKeysTextSize(value);
                break;
            case "extra_keys_height_scale":
                mPreferences.setExtraKeysHeightScale(value);
                break;
            case "extra_keys_bar_border_width":
                mPreferences.setExtraKeysBarBorderWidth(value);
                break;
            case "extra_keys_bar_border_corner_radius":
                mPreferences.setExtraKeysBarBorderCornerRadius(value);
                break;
            case "extra_keys_bar_border_gaps":
                mPreferences.setExtraKeysBarBorderGaps(value);
                break;
            default:
                break;
        }
        TermuxActivity.updateTermuxActivityStyling(mContext, false);
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
            case "glass_tint_opacity":
                return mPreferences.getGlassTintOpacity();
            case "background_blur_radius":
                return mPreferences.getBackgroundBlurRadius();
            case "gesture_pill_width":
                return mPreferences.getGesturePillWidth();
            case "gesture_pill_height":
                return mPreferences.getGesturePillHeight();
            case "gesture_pill_corner_radius":
                return mPreferences.getGesturePillCornerRadius();
            case "extra_keys_margin":
                return mPreferences.getExtraKeysMargin();
            case "extra_keys_text_size":
                return mPreferences.getExtraKeysTextSize();
            case "extra_keys_height_scale":
                return mPreferences.getExtraKeysHeightScale();
            case "extra_keys_bar_border_width":
                return mPreferences.getExtraKeysBarBorderWidth();
            case "extra_keys_bar_border_corner_radius":
                return mPreferences.getExtraKeysBarBorderCornerRadius();
            case "extra_keys_bar_border_gaps":
                return mPreferences.getExtraKeysBarBorderGaps();
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
            case "session_name_alignment":
                mPreferences.setSessionNameAlignment(value != null ? value : "center");
                break;
            case "outer_background_style":
                mPreferences.setOuterBackgroundStyle(value != null ? value : "off");
                break;
            case "extra_keys_preset":
                mPreferences.setExtraKeysPreset(value != null ? value : "");
                break;
            case "extra_keys_custom_json":
                mPreferences.setExtraKeysCustomJson(value != null ? value : "");
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
            case "extra_keys_color_theme":
                mPreferences.setExtraKeysColorTheme(value != null ? value : "default");
                break;
            case "extra_keys_bar_border_color":
                mPreferences.setExtraKeysBarBorderColor(value != null ? value : "#FFFFFF");
                break;
            case "extra_keys_pressed_text_color":
                mPreferences.setExtraKeysPressedTextColor(value != null ? value : "");
                break;
            default:
                break;
        }
        TermuxActivity.updateTermuxActivityStyling(mContext, false);
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
            case "session_name_alignment":
                return mPreferences.getSessionNameAlignment();
            case "outer_background_style":
                return mPreferences.getOuterBackgroundStyle();
            case "extra_keys_preset":
                return mPreferences.getExtraKeysPreset();
            case "extra_keys_custom_json":
                return mPreferences.getExtraKeysCustomJson();
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
            case "extra_keys_color_theme":
                return mPreferences.getExtraKeysColorTheme();
            case "extra_keys_bar_border_color":
                return mPreferences.getExtraKeysBarBorderColor();
            case "extra_keys_pressed_text_color":
                return mPreferences.getExtraKeysPressedTextColor();
            default:
                return defValue;
        }
    }

}
