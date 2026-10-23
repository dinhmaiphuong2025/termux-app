package com.termux.app.fragments.settings.termux;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
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
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Keep
public class CustomUIPreferencesFragment extends PreferenceFragmentCompat {

    private static final String LOG_TAG = "CustomUIPreferencesFragment";

    private ActivityResultLauncher<String> mPickImageLauncher;
    private ActivityResultLauncher<String> mExportConfigLauncher;
    private ActivityResultLauncher<String> mImportConfigLauncher;

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

        mExportConfigLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    exportConfigToUri(uri);
                }
            }
        );

        mImportConfigLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importConfigFromUri(uri);
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
        setupPreferences(context);
    }

    private void refreshPreferencesUi() {
        Context context = getContext();
        if (context == null || getPreferenceScreen() == null) return;
        getPreferenceScreen().removeAll();
        setPreferencesFromResource(R.xml.termux_custom_ui_preferences, null);
        setupPreferences(context);
    }

    private void setupPreferences(Context context) {
        if (context == null) return;

        ListPreference uiPresetPref = findPreference("ui_preset");
        if (uiPresetPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                uiPresetPref.setValue(prefs.getUiPreset());
            }
            uiPresetPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedPreset = (String) newValue;
                if (!"custom".equals(selectedPreset)) {
                    applyUiPreset(context, selectedPreset);
                } else {
                    TermuxAppSharedPreferences p = TermuxAppSharedPreferences.build(context, true);
                    if (p != null) p.setUiPreset("custom");
                }
                return true;
            });
        }

        Preference backupPref = findPreference("backup_ui_config");
        if (backupPref != null) {
            backupPref.setOnPreferenceClickListener(preference -> {
                if (mExportConfigLauncher != null) {
                    mExportConfigLauncher.launch("termux_ui_config_backup.json");
                }
                return true;
            });
        }

        Preference restorePref = findPreference("restore_ui_config");
        if (restorePref != null) {
            restorePref.setOnPreferenceClickListener(preference -> {
                if (mImportConfigLauncher != null) {
                    mImportConfigLauncher.launch("*/*");
                }
                return true;
            });
        }

        Preference resetPref = findPreference("reset_ui_settings");
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(context)
                    .setTitle(R.string.termux_reset_settings_title)
                    .setMessage(R.string.termux_reset_settings_confirm_message)
                    .setPositiveButton(R.string.termux_reset_settings_confirm_button, (dialog, which) -> {
                        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                        if (prefs != null) {
                            prefs.resetAllCustomUiSettingsToDefault();
                            refreshPreferencesUi();
                            TermuxActivity.updateTermuxActivityStyling(context, false);
                            Toast.makeText(context, R.string.termux_reset_settings_success, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
                return true;
            });
        }

        Preference pickPref = findPreference("terminal_background_pick");
        if (pickPref != null) {
            pickPref.setOnPreferenceClickListener(preference -> {
                if (mPickImageLauncher != null) {
                    mPickImageLauncher.launch("image/*");
                }
                return true;
            });
        }

        Preference systemWallPref = findPreference("terminal_background_system_wallpaper");
        if (systemWallPref != null) {
            systemWallPref.setOnPreferenceClickListener(preference -> {
                applySystemWallpaper(context);
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

                    Preference tabActiveColorPref = findPreference("tab_active_color");
                    if (tabActiveColorPref != null) {
                    TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                    if (prefs != null) {
                    tabActiveColorPref.setSummary(prefs.getTabActiveColor());
                    }
                    tabActiveColorPref.setOnPreferenceClickListener(preference -> {
                    String current = prefs != null ? prefs.getTabActiveColor() : "#FFFFFF";
                    ColorPickerDialogUtils.showColorPickerDialog(context, "Active Tab Accent Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setTabActiveColor(hexColor);
                    }
                    tabActiveColorPref.setSummary(hexColor);
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

        SwitchPreferenceCompat glassPref = findPreference("glass_effect_enabled");
        if (glassPref != null) {
            glassPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabling = (Boolean) newValue;
                if (enabling) {
                    TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                    String currentBg = prefs != null ? prefs.getTerminalBackgroundImagePath() : "";
                    if (TextUtils.isEmpty(currentBg)) {
                        showLiquidGlassOnboardingDialog(context);
                    }
                }
                return true;
            });
        }

        String[] reloadStyleKeys = new String[]{
            "show_tab_bar",
            "tab_corner_radius",
            "tab_bar_corner_radius",
            "tab_border_width",
            "tab_button_width",
            "tab_new_button_width",
            "tab_close_button_width",
            "tab_settings_button_width",
            "tab_active_title_length",
            "tab_inactive_title_length",
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

    private void applySystemWallpaper(Context context) {
        if (context == null) return;
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            Drawable drawable = wm.getDrawable();
            if (drawable != null) {
                Bitmap bitmap;
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();
                } else {
                    int w = Math.max(1, drawable.getIntrinsicWidth());
                    int h = Math.max(1, drawable.getIntrinsicHeight());
                    bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, w, h);
                    drawable.draw(canvas);
                }

                if (bitmap != null) {
                    int w = bitmap.getWidth();
                    int h = bitmap.getHeight();
                    int maxDim = Math.max(w, h);
                    int screenMax = 0;
                    if (context.getResources() != null && context.getResources().getDisplayMetrics() != null) {
                        screenMax = Math.max(context.getResources().getDisplayMetrics().widthPixels,
                            context.getResources().getDisplayMetrics().heightPixels);
                    }
                    if (screenMax <= 0) screenMax = 2560;

                    if (maxDim > screenMax) {
                        float ratio = (float) screenMax / maxDim;
                        int targetW = Math.max(1, Math.round(w * ratio));
                        int targetH = Math.max(1, Math.round(h * ratio));
                        bitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true);
                    }

                    File wallpaperFile = new File(context.getFilesDir(), "system_wallpaper.png");
                    try (FileOutputStream fos = new FileOutputStream(wallpaperFile)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos);
                    }

                    TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                    if (prefs != null) {
                        prefs.setTerminalBackgroundImagePath(wallpaperFile.getAbsolutePath());
                    }
                    EditTextPreference pathPref = findPreference("terminal_background_image_path");
                    if (pathPref != null) {
                        pathPref.setText(wallpaperFile.getAbsolutePath());
                    }
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                    Toast.makeText(context, R.string.termux_glass_onboarding_system_wallpaper_success, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (Throwable t) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to extract system wallpaper", t);
        }

        Toast.makeText(context, R.string.termux_glass_onboarding_system_wallpaper_failed, Toast.LENGTH_LONG).show();
        if (mPickImageLauncher != null) {
            mPickImageLauncher.launch("image/*");
        }
    }

    private void showLiquidGlassOnboardingDialog(Context context) {
        if (context == null) return;
        new AlertDialog.Builder(context)
            .setTitle(R.string.termux_glass_onboarding_title)
            .setMessage(R.string.termux_glass_onboarding_message)
            .setPositiveButton(R.string.termux_glass_onboarding_btn_pick, (dialog, which) -> {
                if (mPickImageLauncher != null) {
                    mPickImageLauncher.launch("image/*");
                }
            })
            .setNeutralButton(R.string.termux_glass_onboarding_btn_system, (dialog, which) -> {
                applySystemWallpaper(context);
            })
            .setNegativeButton(R.string.termux_glass_onboarding_btn_later, (dialog, which) -> {
                Toast.makeText(context, R.string.termux_glass_onboarding_later_tip, Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUiPresetPreference();
    }

    private void updateUiPresetPreference() {
        ListPreference uiPresetPref = findPreference("ui_preset");
        if (uiPresetPref != null) {
            Context context = getContext();
            if (context != null) {
                TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                if (prefs != null) {
                    uiPresetPref.setValue(prefs.getUiPreset());
                }
            }
        }
    }

    private void applyUiPreset(Context context, String presetKey) {
        if (context == null) return;
        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
        if (prefs == null) return;

        switch (presetKey) {
            case "niri_default":
                prefs.setTerminalBorderEnabled(true);
                prefs.setTerminalBorderWidth(1);
                prefs.setTerminalBorderCornerEnabled(true);
                prefs.setTerminalBorderCornerRadius(1);
                prefs.setTerminalBorderColor("#FFFFFF");
                prefs.setTerminalBorderGaps(4);
                prefs.setTerminalBorderStyle("full");
                prefs.setTerminalHorizontalMargin(3);
                prefs.setTerminalShadowEnabled(true);
                prefs.setKeepBorderLayoutEnabled(false);
                prefs.setTerminalBackgroundImagePath("");
                prefs.setOuterBackgroundStyle("off");

                prefs.setTabBarEnabled(true);
                prefs.setTabBarGlassEnabled(false);
                prefs.setTabBorderEnabled(true);
                prefs.setTabCornerEnabled(true);
                prefs.setTabCornerRadius(1);
                prefs.setTabBarCornerRadius(1);
                prefs.setTabBorderWidth(1);
                prefs.setTabBorderColor("#FFFFFF");
                prefs.setTabActiveColor("#FFFFFF");
                prefs.setTabInactiveTitleLength(4);

                prefs.setGlassEffectEnabled(false);
                prefs.setBackgroundBlurEnabled(false);
                prefs.setExtraKeysBarBackgroundSyncEnabled(false);
                prefs.setExtraKeysColorTheme("default");
                prefs.setExtraKeysFlatKeys(true);
                prefs.setExtraKeysBarBorderEnabled(true);
                prefs.setExtraKeysBarBorderWidth(1);
                prefs.setExtraKeysBarBorderColor("#FFFFFF");
                prefs.setExtraKeysBarBorderCornerRadius(1);
                prefs.setExtraKeysBarCornerEnabled(true);
                prefs.setExtraKeysPressedTextColor("#FFFFFF");
                break;

            case "liquid_glass":
                prefs.setTerminalBorderEnabled(false);
                prefs.setTerminalBorderWidth(1);
                prefs.setTerminalBorderCornerEnabled(true);
                prefs.setTerminalBorderCornerRadius(12);
                prefs.setTerminalBorderColor("#FFFFFF");
                prefs.setTerminalBorderGaps(4);
                prefs.setTerminalBorderStyle("full");
                prefs.setTerminalHorizontalMargin(3);
                prefs.setTerminalShadowEnabled(true);
                prefs.setKeepBorderLayoutEnabled(true);

                prefs.setTabBarEnabled(true);
                prefs.setTabBarGlassEnabled(true);
                prefs.setTabBorderEnabled(false);
                prefs.setTabCornerEnabled(true);
                prefs.setTabCornerRadius(8);
                prefs.setTabBarCornerRadius(18);
                prefs.setTabBorderWidth(1);
                prefs.setTabBorderColor("#FFFFFF");
                prefs.setTabActiveColor("#FFFFFF");
                prefs.setTabInactiveTitleLength(4);

                prefs.setGlassEffectEnabled(true);
                prefs.setGlassTintOpacity(100);
                prefs.setBackgroundBlurEnabled(false);
                prefs.setExtraKeysBarBackgroundSyncEnabled(true);
                prefs.setExtraKeysBarCornerEnabled(true);
                prefs.setExtraKeysBarBorderCornerRadius(12);
                prefs.setExtraKeysBarBorderEnabled(false);
                prefs.setExtraKeysFlatKeys(true);
                prefs.setExtraKeysPressedTextColor("#FFFFFF");
                prefs.setOuterBackgroundStyle("blur");

                if (TextUtils.isEmpty(prefs.getTerminalBackgroundImagePath())) {
                    showLiquidGlassOnboardingDialog(context);
                }
                break;

            case "classic_termux":
                prefs.setTerminalBorderEnabled(false);
                prefs.setTerminalBorderGaps(0);
                prefs.setTerminalBorderCornerEnabled(false);
                prefs.setTerminalBorderCornerRadius(0);
                prefs.setKeepBorderLayoutEnabled(false);
                prefs.setTerminalShadowEnabled(false);
                prefs.setTerminalBackgroundImagePath("");
                prefs.setOuterBackgroundStyle("off");

                prefs.setTabBarEnabled(false);
                prefs.setTabBarGlassEnabled(false);
                prefs.setTabCornerRadius(0);
                prefs.setTabBarCornerRadius(0);

                prefs.setGlassEffectEnabled(false);
                prefs.setBackgroundBlurEnabled(false);
                prefs.setExtraKeysBarBackgroundSyncEnabled(false);
                prefs.setExtraKeysColorTheme("default");
                prefs.setExtraKeysFlatKeys(true);
                prefs.setExtraKeysBarBorderEnabled(false);
                prefs.setExtraKeysBarCornerEnabled(false);
                prefs.setExtraKeysPressedTextColor("");
                break;

            case "custom":
            default:
                break;
        }

        prefs.setUiPreset(presetKey);
        refreshPreferencesUi();
        TermuxActivity.updateTermuxActivityStyling(context, false);
    }

    private void exportConfigToUri(Uri uri) {
        Context context = getContext();
        if (context == null || uri == null) return;
        try {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs == null) return;

            JSONObject json = new JSONObject();
            json.put("version", 1);
            json.put("ui_preset", prefs.getUiPreset());

            // Window & Border
            json.put("terminal_border_enabled", prefs.isTerminalBorderEnabled());
            json.put("terminal_border_width", prefs.getTerminalBorderWidth());
            json.put("terminal_border_color", prefs.getTerminalBorderColor());
            json.put("terminal_border_gaps", prefs.getTerminalBorderGaps());
            json.put("terminal_horizontal_margin", prefs.getTerminalHorizontalMargin());
            json.put("terminal_border_style", prefs.getTerminalBorderStyle());
            json.put("terminal_border_corner_enabled", prefs.isTerminalBorderCornerEnabled());
            json.put("terminal_border_corner_radius", prefs.getTerminalBorderCornerRadius());
            json.put("terminal_shadow_enabled", prefs.isTerminalShadowEnabled());
            json.put("keep_border_layout_enabled", prefs.isKeepBorderLayoutEnabled());

            // Background & Effects
            json.put("terminal_background_image_path", prefs.getTerminalBackgroundImagePath());
            json.put("terminal_background_opacity", prefs.getTerminalBackgroundOpacity());
            json.put("terminal_cell_background_transparency", prefs.isTerminalCellBackgroundTransparencyEnabled());
            json.put("terminal_cell_background_opacity", prefs.getTerminalCellBackgroundOpacity());
            json.put("outer_background_style", prefs.getOuterBackgroundStyle());
            json.put("glass_effect_enabled", prefs.isGlassEffectEnabled());
            json.put("glass_tint_opacity", prefs.getGlassTintOpacity());
            json.put("background_blur_enabled", prefs.isBackgroundBlurEnabled());
            json.put("background_blur_radius", prefs.getBackgroundBlurRadius());

            // Tab Bar
            json.put("show_tab_bar", prefs.isTabBarEnabled());
            json.put("tab_bar_glass_enabled", prefs.isTabBarGlassEnabled());
            json.put("tab_border_enabled", prefs.isTabBorderEnabled());
            json.put("tab_border_width", prefs.getTabBorderWidth());
            json.put("tab_border_color", prefs.getTabBorderColor());
            json.put("tab_active_color", prefs.getTabActiveColor());
            json.put("tab_corner_enabled", prefs.isTabCornerEnabled());
            json.put("tab_corner_radius", prefs.getTabCornerRadius());
            json.put("tab_bar_corner_radius", prefs.getTabBarCornerRadius());
            json.put("tab_button_width", prefs.getTabButtonWidth());
            json.put("tab_new_button_width", prefs.getTabNewButtonWidth());
            json.put("tab_close_button_width", prefs.getTabCloseButtonWidth());
            json.put("tab_settings_button_width", prefs.getTabSettingsButtonWidth());
            json.put("tab_active_title_length", prefs.getTabActiveTitleLength());
            json.put("tab_inactive_title_length", prefs.getTabInactiveTitleLength());
            json.put("show_session_name_without_tab_bar", prefs.isShowSessionNameWithoutTabBar());
            json.put("session_name_alignment", prefs.getSessionNameAlignment());
            json.put("tab_animation_enabled", prefs.isTabAnimationEnabled());

            // Extra Keys
            json.put("extra_keys_color_theme", prefs.getExtraKeysColorTheme());
            json.put("extra_keys_pressed_text_color", prefs.getExtraKeysPressedTextColor());
            json.put("extra_keys_flat_keys", prefs.isExtraKeysFlatKeys());
            json.put("extra_keys_margin", prefs.getExtraKeysMargin());
            json.put("extra_keys_text_size", prefs.getExtraKeysTextSize());
            json.put("extra_keys_height_scale", prefs.getExtraKeysHeightScale());
            json.put("extra_keys_bar_border_enabled", prefs.isExtraKeysBarBorderEnabled());
            json.put("extra_keys_bar_border_width", prefs.getExtraKeysBarBorderWidth());
            json.put("extra_keys_bar_border_color", prefs.getExtraKeysBarBorderColor());
            json.put("extra_keys_bar_border_corner_radius", prefs.getExtraKeysBarBorderCornerRadius());
            json.put("extra_keys_bar_border_gaps", prefs.getExtraKeysBarBorderGaps());
            json.put("extra_keys_bar_corner_enabled", prefs.isExtraKeysBarCornerEnabled());
            json.put("extra_keys_bar_background_sync", prefs.isExtraKeysBarBackgroundSyncEnabled());
            json.put("extra_keys_use_custom", prefs.isExtraKeysUseCustom());
            json.put("extra_keys_preset", prefs.getExtraKeysPreset());
            json.put("extra_keys_custom_json", prefs.getExtraKeysCustomJson());

            // Gesture Pill & Behavior
            json.put("gesture_pill_enabled", prefs.isGesturePillEnabled());
            json.put("gesture_pill_width", prefs.getGesturePillWidth());
            json.put("gesture_pill_height", prefs.getGesturePillHeight());
            json.put("gesture_pill_corner_radius", prefs.getGesturePillCornerRadius());
            json.put("gesture_pill_color", prefs.getGesturePillColor());
            json.put("gesture_pill_swipe_left", prefs.getGesturePillSwipeLeft());
            json.put("gesture_pill_swipe_right", prefs.getGesturePillSwipeRight());
            json.put("gesture_pill_swipe_up", prefs.getGesturePillSwipeUp());
            json.put("gesture_pill_swipe_down", prefs.getGesturePillSwipeDown());
            json.put("gesture_pill_tap", prefs.getGesturePillTap());
            json.put("terminal_smooth_scroll", prefs.isTerminalSmoothScrollEnabled());

            byte[] bytes = json.toString(2).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    os.write(bytes);
                    os.flush();
                }
            }

            // Also keep a local backup file in Download folder
            try {
                File dlFile = new File("/storage/emulated/0/Download", "termux_custom_ui_backup.json");
                try (FileOutputStream fos = new FileOutputStream(dlFile)) {
                    fos.write(bytes);
                    fos.flush();
                }
            } catch (Throwable ignored) {}

            Toast.makeText(context, R.string.termux_backup_config_success, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to export UI config", t);
            Toast.makeText(context, R.string.termux_backup_config_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importConfigFromUri(Uri uri) {
        Context context = getContext();
        if (context == null || uri == null) return;
        try {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs == null) return;

            String jsonStr;
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                jsonStr = baos.toString("UTF-8");
            }

            JSONObject json = new JSONObject(jsonStr);

            if (json.has("ui_preset")) prefs.setUiPreset(json.optString("ui_preset", "custom"));

            if (json.has("terminal_border_enabled")) prefs.setTerminalBorderEnabled(json.optBoolean("terminal_border_enabled"));
            if (json.has("terminal_border_width")) prefs.setTerminalBorderWidth(json.optInt("terminal_border_width"));
            if (json.has("terminal_border_color")) prefs.setTerminalBorderColor(json.optString("terminal_border_color"));
            if (json.has("terminal_border_gaps")) prefs.setTerminalBorderGaps(json.optInt("terminal_border_gaps"));
            if (json.has("terminal_horizontal_margin")) prefs.setTerminalHorizontalMargin(json.optInt("terminal_horizontal_margin"));
            if (json.has("terminal_border_style")) prefs.setTerminalBorderStyle(json.optString("terminal_border_style"));
            if (json.has("terminal_border_corner_enabled")) prefs.setTerminalBorderCornerEnabled(json.optBoolean("terminal_border_corner_enabled"));
            if (json.has("terminal_border_corner_radius")) prefs.setTerminalBorderCornerRadius(json.optInt("terminal_border_corner_radius"));
            if (json.has("terminal_shadow_enabled")) prefs.setTerminalShadowEnabled(json.optBoolean("terminal_shadow_enabled"));
            if (json.has("keep_border_layout_enabled")) prefs.setKeepBorderLayoutEnabled(json.optBoolean("keep_border_layout_enabled"));

            if (json.has("terminal_background_image_path")) prefs.setTerminalBackgroundImagePath(json.optString("terminal_background_image_path"));
            if (json.has("terminal_background_opacity")) prefs.setTerminalBackgroundOpacity(json.optInt("terminal_background_opacity"));
            if (json.has("terminal_cell_background_transparency")) prefs.setTerminalCellBackgroundTransparencyEnabled(json.optBoolean("terminal_cell_background_transparency"));
            if (json.has("terminal_cell_background_opacity")) prefs.setTerminalCellBackgroundOpacity(json.optInt("terminal_cell_background_opacity"));
            if (json.has("outer_background_style")) prefs.setOuterBackgroundStyle(json.optString("outer_background_style"));
            if (json.has("glass_effect_enabled")) prefs.setGlassEffectEnabled(json.optBoolean("glass_effect_enabled"));
            if (json.has("glass_tint_opacity")) prefs.setGlassTintOpacity(json.optInt("glass_tint_opacity"));
            if (json.has("background_blur_enabled")) prefs.setBackgroundBlurEnabled(json.optBoolean("background_blur_enabled"));
            if (json.has("background_blur_radius")) prefs.setBackgroundBlurRadius(json.optInt("background_blur_radius"));

            if (json.has("show_tab_bar")) prefs.setTabBarEnabled(json.optBoolean("show_tab_bar"));
            if (json.has("tab_bar_glass_enabled")) prefs.setTabBarGlassEnabled(json.optBoolean("tab_bar_glass_enabled"));
            if (json.has("tab_border_enabled")) prefs.setTabBorderEnabled(json.optBoolean("tab_border_enabled"));
            if (json.has("tab_border_width")) prefs.setTabBorderWidth(json.optInt("tab_border_width"));
            if (json.has("tab_border_color")) prefs.setTabBorderColor(json.optString("tab_border_color"));
            if (json.has("tab_active_color")) prefs.setTabActiveColor(json.optString("tab_active_color"));
            if (json.has("tab_corner_enabled")) prefs.setTabCornerEnabled(json.optBoolean("tab_corner_enabled"));
            if (json.has("tab_corner_radius")) prefs.setTabCornerRadius(json.optInt("tab_corner_radius"));
            if (json.has("tab_bar_corner_radius")) prefs.setTabBarCornerRadius(json.optInt("tab_bar_corner_radius"));
            if (json.has("tab_button_width")) prefs.setTabButtonWidth(json.optInt("tab_button_width"));
            if (json.has("tab_new_button_width")) prefs.setTabNewButtonWidth(json.optInt("tab_new_button_width"));
            if (json.has("tab_close_button_width")) prefs.setTabCloseButtonWidth(json.optInt("tab_close_button_width"));
            if (json.has("tab_settings_button_width")) prefs.setTabSettingsButtonWidth(json.optInt("tab_settings_button_width"));
            if (json.has("tab_active_title_length")) prefs.setTabActiveTitleLength(json.optInt("tab_active_title_length"));
            if (json.has("tab_inactive_title_length")) prefs.setTabInactiveTitleLength(json.optInt("tab_inactive_title_length"));
            if (json.has("show_session_name_without_tab_bar")) prefs.setShowSessionNameWithoutTabBar(json.optBoolean("show_session_name_without_tab_bar"));
            if (json.has("session_name_alignment")) prefs.setSessionNameAlignment(json.optString("session_name_alignment"));
            if (json.has("tab_animation_enabled")) prefs.setTabAnimationEnabled(json.optBoolean("tab_animation_enabled"));

            if (json.has("extra_keys_color_theme")) prefs.setExtraKeysColorTheme(json.optString("extra_keys_color_theme"));
            if (json.has("extra_keys_pressed_text_color")) prefs.setExtraKeysPressedTextColor(json.optString("extra_keys_pressed_text_color"));
            if (json.has("extra_keys_flat_keys")) prefs.setExtraKeysFlatKeys(json.optBoolean("extra_keys_flat_keys"));
            if (json.has("extra_keys_margin")) prefs.setExtraKeysMargin(json.optInt("extra_keys_margin"));
            if (json.has("extra_keys_text_size")) prefs.setExtraKeysTextSize(json.optInt("extra_keys_text_size"));
            if (json.has("extra_keys_height_scale")) prefs.setExtraKeysHeightScale(json.optInt("extra_keys_height_scale"));
            if (json.has("extra_keys_bar_border_enabled")) prefs.setExtraKeysBarBorderEnabled(json.optBoolean("extra_keys_bar_border_enabled"));
            if (json.has("extra_keys_bar_border_width")) prefs.setExtraKeysBarBorderWidth(json.optInt("extra_keys_bar_border_width"));
            if (json.has("extra_keys_bar_border_color")) prefs.setExtraKeysBarBorderColor(json.optString("extra_keys_bar_border_color"));
            if (json.has("extra_keys_bar_border_corner_radius")) prefs.setExtraKeysBarBorderCornerRadius(json.optInt("extra_keys_bar_border_corner_radius"));
            if (json.has("extra_keys_bar_border_gaps")) prefs.setExtraKeysBarBorderGaps(json.optInt("extra_keys_bar_border_gaps"));
            if (json.has("extra_keys_bar_corner_enabled")) prefs.setExtraKeysBarCornerEnabled(json.optBoolean("extra_keys_bar_corner_enabled"));
            if (json.has("extra_keys_bar_background_sync")) prefs.setExtraKeysBarBackgroundSyncEnabled(json.optBoolean("extra_keys_bar_background_sync"));
            if (json.has("extra_keys_use_custom")) prefs.setExtraKeysUseCustom(json.optBoolean("extra_keys_use_custom"));
            if (json.has("extra_keys_preset")) prefs.setExtraKeysPreset(json.optString("extra_keys_preset"));
            if (json.has("extra_keys_custom_json")) prefs.setExtraKeysCustomJson(json.optString("extra_keys_custom_json"));

            if (json.has("gesture_pill_enabled")) prefs.setGesturePillEnabled(json.optBoolean("gesture_pill_enabled"));
            if (json.has("gesture_pill_width")) prefs.setGesturePillWidth(json.optInt("gesture_pill_width"));
            if (json.has("gesture_pill_height")) prefs.setGesturePillHeight(json.optInt("gesture_pill_height"));
            if (json.has("gesture_pill_corner_radius")) prefs.setGesturePillCornerRadius(json.optInt("gesture_pill_corner_radius"));
            if (json.has("gesture_pill_color")) prefs.setGesturePillColor(json.optString("gesture_pill_color"));
            if (json.has("gesture_pill_swipe_left")) prefs.setGesturePillSwipeLeft(json.optString("gesture_pill_swipe_left"));
            if (json.has("gesture_pill_swipe_right")) prefs.setGesturePillSwipeRight(json.optString("gesture_pill_swipe_right"));
            if (json.has("gesture_pill_swipe_up")) prefs.setGesturePillSwipeUp(json.optString("gesture_pill_swipe_up"));
            if (json.has("gesture_pill_swipe_down")) prefs.setGesturePillSwipeDown(json.optString("gesture_pill_swipe_down"));
            if (json.has("gesture_pill_tap")) prefs.setGesturePillTap(json.optString("gesture_pill_tap"));
            if (json.has("terminal_smooth_scroll")) prefs.setTerminalSmoothScrollEnabled(json.optBoolean("terminal_smooth_scroll"));

            refreshPreferencesUi();
            TermuxActivity.updateTermuxActivityStyling(context, false);
            Toast.makeText(context, R.string.termux_restore_config_success, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to import UI config", t);
            Toast.makeText(context, R.string.termux_restore_config_failed, Toast.LENGTH_SHORT).show();
        }
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

    private void markCustomPreset() {
        if (mPreferences != null && !"custom".equals(mPreferences.getUiPreset())) {
            mPreferences.setUiPreset("custom");
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null || key == null) return;
        markCustomPreset();

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
        markCustomPreset();

        switch (key) {
            case "tab_corner_radius":
                mPreferences.setTabCornerRadius(value);
                break;
            case "tab_bar_corner_radius":
                mPreferences.setTabBarCornerRadius(value);
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
            case "tab_inactive_title_length":
                mPreferences.setTabInactiveTitleLength(value);
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
            case "tab_bar_corner_radius":
                return mPreferences.getTabBarCornerRadius();
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
            case "tab_inactive_title_length":
                return mPreferences.getTabInactiveTitleLength();
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
        if (!"ui_preset".equals(key)) {
            markCustomPreset();
        }

        switch (key) {
            case "ui_preset":
                mPreferences.setUiPreset(value != null ? value : "niri_default");
                break;
            case "terminal_border_color":
                mPreferences.setTerminalBorderColor(value != null ? value : "#FFFFFF");
                break;
            case "tab_border_color":
                mPreferences.setTabBorderColor(value != null ? value : "#FFFFFF");
                break;
            case "tab_active_color":
                mPreferences.setTabActiveColor(value != null ? value : "#FFFFFF");
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
            case "ui_preset":
                return mPreferences.getUiPreset();
            case "terminal_border_color":
                return mPreferences.getTerminalBorderColor();
            case "tab_border_color":
                return mPreferences.getTabBorderColor();
            case "tab_active_color":
                return mPreferences.getTabActiveColor();
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
