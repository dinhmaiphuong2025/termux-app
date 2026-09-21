package com.termux.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.widget.Toast;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

@Keep
public class TermuxPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TermuxPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_preferences, rootKey);

        Preference resetPref = findPreference("reset_ui_settings_to_default");
        if (resetPref != null) {
            resetPref.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(context)
                    .setTitle(R.string.termux_reset_settings_title)
                    .setMessage(R.string.termux_reset_settings_confirm_message)
                    .setPositiveButton(R.string.termux_reset_settings_confirm_button, (dialog, which) -> {
                        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
                        if (prefs != null) {
                            prefs.resetAllCustomUiSettingsToDefault();
                            TermuxActivity.updateTermuxActivityStyling(context, false);
                            Toast.makeText(context, R.string.termux_reset_settings_success, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
                return true;
            });
        }
    }

}

class TermuxPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TermuxAppSharedPreferences mPreferences;

    private static TermuxPreferencesDataStore mInstance;

    private TermuxPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TermuxAppSharedPreferences.build(context, true);
    }

    public static synchronized TermuxPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TermuxPreferencesDataStore(context);
        }
        return mInstance;
    }

}
