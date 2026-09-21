package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

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
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

@Keep
public class TerminalIOPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalIOPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_terminal_io_preferences, rootKey);

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

        Preference barColorPref = findPreference("extra_keys_bar_border_color");
        if (barColorPref != null) {
            TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
            if (prefs != null) {
                barColorPref.setSummary(prefs.getExtraKeysBarBorderColor());
            }
            barColorPref.setOnPreferenceClickListener(preference -> {
                String current = prefs != null ? prefs.getExtraKeysBarBorderColor() : "#3D82F6";
                ColorPickerDialogUtils.showColorPickerDialog(context, "Bar Border Color", current, hexColor -> {
                    if (prefs != null) {
                        prefs.setExtraKeysBarBorderColor(hexColor);
                    }
                    barColorPref.setSummary(hexColor);
                    TermuxActivity.updateTermuxActivityStyling(context, false);
                });
                return true;
            });
        }

        String[] reloadStyleKeys = new String[]{
            "extra_keys_color_theme",
            "extra_keys_flat_keys",
            "extra_keys_margin",
            "extra_keys_text_size",
            "extra_keys_height_scale",
            "extra_keys_bar_border_enabled",
            "extra_keys_bar_border_width"
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

class TerminalIOPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;

    private static TerminalIOPreferencesDataStore mInstance;

    private TerminalIOPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalIOPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalIOPreferencesDataStore(context);
        }
        return mInstance;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
            case "soft_keyboard_enabled":
                mPreferences.setSoftKeyboardEnabled(value);
                break;
            case "soft_keyboard_enabled_only_if_no_hardware":
                mPreferences.setSoftKeyboardEnabledOnlyIfNoHardware(value);
                break;
            case "extra_keys_use_custom":
                mPreferences.setExtraKeysUseCustom(value);
                break;
            case "extra_keys_flat_keys":
                mPreferences.setExtraKeysFlatKeys(value);
                break;
            case "extra_keys_bar_border_enabled":
                mPreferences.setExtraKeysBarBorderEnabled(value);
                break;
            case "extra_keys_bar_border_sync":
                mPreferences.setExtraKeysBarBorderSync(value);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences == null || key == null) return defValue;

        switch (key) {
            case "soft_keyboard_enabled":
                return mPreferences.isSoftKeyboardEnabled();
            case "soft_keyboard_enabled_only_if_no_hardware":
                return mPreferences.isSoftKeyboardEnabledOnlyIfNoHardware();
            case "extra_keys_use_custom":
                return mPreferences.isExtraKeysUseCustom();
            case "extra_keys_flat_keys":
                return mPreferences.isExtraKeysFlatKeys();
            case "extra_keys_bar_border_enabled":
                return mPreferences.isExtraKeysBarBorderEnabled();
            case "extra_keys_bar_border_sync":
                return mPreferences.isExtraKeysBarBorderSync();
            default:
                return defValue;
        }
    }

    @Override
    public void putInt(String key, int value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
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
            default:
                break;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        if (mPreferences == null || key == null) return defValue;

        switch (key) {
            case "extra_keys_margin":
                return mPreferences.getExtraKeysMargin();
            case "extra_keys_text_size":
                return mPreferences.getExtraKeysTextSize();
            case "extra_keys_height_scale":
                return mPreferences.getExtraKeysHeightScale();
            case "extra_keys_bar_border_width":
                return mPreferences.getExtraKeysBarBorderWidth();
            default:
                return defValue;
        }
    }

    @Override
    public void putString(String key, @Nullable String value) {
        if (mPreferences == null || key == null) return;

        switch (key) {
            case "extra_keys_color_theme":
                mPreferences.setExtraKeysColorTheme(value != null ? value : "default");
                break;
            case "extra_keys_preset":
                mPreferences.setExtraKeysPreset(value != null ? value : "");
                break;
            case "extra_keys_custom_json":
                mPreferences.setExtraKeysCustomJson(value != null ? value : "");
                break;
            case "extra_keys_key_border_color":
                mPreferences.setExtraKeysKeyBorderColor(value != null ? value : "#3D82F6");
                break;
            case "extra_keys_bar_border_color":
                mPreferences.setExtraKeysBarBorderColor(value != null ? value : "#3D82F6");
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
            case "extra_keys_color_theme":
                String colorTheme = mPreferences.getExtraKeysColorTheme();
                return (colorTheme != null && !colorTheme.isEmpty()) ? colorTheme : defValue;
            case "extra_keys_preset":
                String preset = mPreferences.getExtraKeysPreset();
                return (preset != null && !preset.isEmpty()) ? preset : defValue;
            case "extra_keys_custom_json":
                return mPreferences.getExtraKeysCustomJson();
            case "extra_keys_key_border_color":
                return mPreferences.getExtraKeysKeyBorderColor();
            case "extra_keys_bar_border_color":
                return mPreferences.getExtraKeysBarBorderColor();
            default:
                return defValue;
        }
    }

}
