package com.termux.app.fragments.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class ColorPickerDialogUtils {

    public interface OnColorSelectedListener {
        void onColorSelected(String hexColor);
    }

    // Popular vibrant and theme colors
    private static final String[][] PRESET_COLORS = new String[][] {
        // Row 1: Vibrant
        {"#3D82F6", "#06B6D4", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6"},
        // Row 2: Popular Themes & Neutrals
        {"#89B4FA", "#CBA6F7", "#7AA2F7", "#BD93F9", "#88C0D0", "#FFFFFF", "#6B7280"}
    };

    public static void showColorPickerDialog(@NonNull Context context, String title, String initialHex,
                                            @NonNull OnColorSelectedListener listener) {
        float density = context.getResources().getDisplayMetrics().density;
        final String currentHex = TextUtils.isEmpty(initialHex) ? "#3D82F6" : initialHex;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (18 * density);
        root.setPadding(padding, (int) (12 * density), padding, (int) (8 * density));

        // Top section: Live Preview Circle + Hex Input Field
        LinearLayout topSection = new LinearLayout(context);
        topSection.setOrientation(LinearLayout.HORIZONTAL);
        topSection.setGravity(Gravity.CENTER_VERTICAL);
        topSection.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Live preview circle
        final View previewCircle = new View(context);
        int circleSize = (int) (38 * density);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(circleSize, circleSize);
        circleParams.setMarginEnd((int) (12 * density));
        previewCircle.setLayoutParams(circleParams);

        updatePreviewCircle(previewCircle, currentHex, density);

        // Hex EditText
        final EditText hexInput = new EditText(context);
        hexInput.setText(currentHex);
        hexInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hexInput.setSingleLine(true);
        hexInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hexInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        topSection.addView(previewCircle);
        topSection.addView(hexInput);
        root.addView(topSection);

        // Palette Label
        TextView paletteLabel = new TextView(context);
        paletteLabel.setText("Presets Palette");
        paletteLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        paletteLabel.setAlpha(0.7f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, (int) (14 * density), 0, (int) (6 * density));
        paletteLabel.setLayoutParams(labelParams);
        root.addView(paletteLabel);

        // Palette Rows
        for (String[] rowColors : PRESET_COLORS) {
            HorizontalScrollView rowScroll = new HorizontalScrollView(context);
            rowScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout.LayoutParams rowScrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowScrollParams.setMargins(0, (int) (4 * density), 0, (int) (4 * density));
            rowScroll.setLayoutParams(rowScrollParams);

            LinearLayout rowContainer = new LinearLayout(context);
            rowContainer.setOrientation(LinearLayout.HORIZONTAL);

            for (String colorHex : rowColors) {
                FrameLayout swatchFrame = new FrameLayout(context);
                int swatchSize = (int) (32 * density);
                LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(swatchSize, swatchSize);
                swatchParams.setMarginEnd((int) (8 * density));
                swatchFrame.setLayoutParams(swatchParams);

                View swatchView = new View(context);
                FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                swatchView.setLayoutParams(vp);

                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                try {
                    gd.setColor(Color.parseColor(colorHex));
                } catch (Exception e) {
                    gd.setColor(Color.BLUE);
                }
                gd.setStroke((int) (1 * density), Color.parseColor("#44FFFFFF"));
                swatchView.setBackground(gd);

                final String selectedColor = colorHex;
                swatchFrame.setOnClickListener(v -> {
                    hexInput.setText(selectedColor);
                    hexInput.setSelection(selectedColor.length());
                    updatePreviewCircle(previewCircle, selectedColor, density);
                });

                swatchFrame.addView(swatchView);
                rowContainer.addView(swatchFrame);
            }

            rowScroll.addView(rowContainer);
            root.addView(rowScroll);
        }

        // TextWatcher to update preview when user types manually
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreviewCircle(previewCircle, s.toString(), density);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        new AlertDialog.Builder(context)
            .setTitle(title != null ? title : "Select Color")
            .setView(root)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String result = hexInput.getText().toString().trim();
                if (!result.startsWith("#")) {
                    result = "#" + result;
                }
                try {
                    Color.parseColor(result);
                    listener.onColorSelected(result);
                } catch (Exception e) {
                    // Invalid color, discard or keep previous
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private static void updatePreviewCircle(View view, String hex, float density) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        int parsedColor = Color.GRAY;
        try {
            if (!hex.startsWith("#")) hex = "#" + hex;
            parsedColor = Color.parseColor(hex);
        } catch (Exception ignored) {}
        gd.setColor(parsedColor);
        gd.setStroke((int) (2 * density), Color.parseColor("#66FFFFFF"));
        view.setBackground(gd);
    }
}
