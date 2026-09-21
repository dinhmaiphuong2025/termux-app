package com.termux.shared.termux.extrakeys;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.google.android.material.color.MaterialColors;

public class ExtraKeysTheme {

    public final int barBackgroundColor;
    public final int buttonBackgroundColor;
    public final int buttonTextColor;
    public final int buttonActiveBackgroundColor;
    public final int buttonActiveTextColor;

    public ExtraKeysTheme(int barBg, int btnBg, int btnText, int btnActiveBg, int btnActiveText) {
        this.barBackgroundColor = barBg;
        this.buttonBackgroundColor = btnBg;
        this.buttonTextColor = btnText;
        this.buttonActiveBackgroundColor = btnActiveBg;
        this.buttonActiveTextColor = btnActiveText;
    }

    @NonNull
    public static ExtraKeysTheme getTheme(Context context, String themeName) {
        if (themeName == null) themeName = "default";

        switch (themeName) {
            case "system": {
                // Material You / System Dynamic Colors
                int barBg = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainer, Color.parseColor("#1C1B1F"));
                int btnBg = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.parseColor("#2B2930"));
                int btnText = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
                int btnActiveBg = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, Color.parseColor("#4F378B"));
                int btnActiveText = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.parseColor("#EADDFF"));
                return new ExtraKeysTheme(barBg, btnBg, btnText, btnActiveBg, btnActiveText);
            }

            case "catppuccin_mocha":
                // Catppuccin Mocha (Dark)
                return new ExtraKeysTheme(
                    Color.parseColor("#1E1E2E"), // Base
                    Color.parseColor("#313244"), // Surface0
                    Color.parseColor("#CDD6F4"), // Text
                    Color.parseColor("#45475A"), // Surface1
                    Color.parseColor("#89B4FA")  // Blue
                );

            case "catppuccin_macchiato":
                // Catppuccin Macchiato (Medium Dark)
                return new ExtraKeysTheme(
                    Color.parseColor("#24273A"), // Base
                    Color.parseColor("#363A4F"), // Surface0
                    Color.parseColor("#CAD3F5"), // Text
                    Color.parseColor("#494D64"), // Surface1
                    Color.parseColor("#8AADF4")  // Blue
                );

            case "catppuccin_latte":
                // Catppuccin Latte (Light)
                return new ExtraKeysTheme(
                    Color.parseColor("#EFF1F5"), // Base
                    Color.parseColor("#CCD0DA"), // Surface0
                    Color.parseColor("#4C4F69"), // Text
                    Color.parseColor("#BCC0CC"), // Surface1
                    Color.parseColor("#1E66F5")  // Blue
                );

            case "dracula":
                // Dracula Theme
                return new ExtraKeysTheme(
                    Color.parseColor("#282A36"), // Background
                    Color.parseColor("#44475A"), // Current Line / Button
                    Color.parseColor("#F8F8F2"), // Foreground
                    Color.parseColor("#6272A4"), // Comment / Active Button
                    Color.parseColor("#BD93F9")  // Purple
                );

            case "nord":
                // Nord Theme
                return new ExtraKeysTheme(
                    Color.parseColor("#2E3440"), // Polar Night nord0
                    Color.parseColor("#3B4252"), // Polar Night nord1
                    Color.parseColor("#ECEFF4"), // Snow Storm nord6
                    Color.parseColor("#4C566A"), // Polar Night nord3
                    Color.parseColor("#88C0D0")  // Frost nord8
                );

            case "one_dark":
                // One Dark Theme
                return new ExtraKeysTheme(
                    Color.parseColor("#21252B"), // Darker BG
                    Color.parseColor("#282C34"), // Editor BG
                    Color.parseColor("#ABB2BF"), // Foreground Text
                    Color.parseColor("#3E4451"), // Selection
                    Color.parseColor("#61AFEF")  // Blue
                );

            case "tokyo_night":
                // Tokyo Night
                return new ExtraKeysTheme(
                    Color.parseColor("#1A1B26"), // BG
                    Color.parseColor("#24283B"), // Darker Surface
                    Color.parseColor("#C0CAF5"), // Text
                    Color.parseColor("#414868"), // Selection
                    Color.parseColor("#7AA2F7")  // Blue
                );

            case "default":
            default:
                // Termux Classic Dark
                return new ExtraKeysTheme(
                    Color.BLACK,
                    Color.parseColor("#1C1C21"),
                    Color.WHITE,
                    Color.parseColor("#7F7F7F"),
                    Color.parseColor("#80DEEA")
                );
        }
    }
}
