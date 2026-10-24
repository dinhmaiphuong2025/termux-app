package com.termux.app.fragments.settings.termux;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExtraKeysHudEditor {

    public interface OnSaveListener {
        void onSaved();
    }

    public static class KeyItem {
        public String key = "";
        public String display = "";
        public String macro = "";
        public String popupKey = "";
        public String popupDisplay = "";
        public String popupMacro = "";

        public KeyItem copy() {
            KeyItem k = new KeyItem();
            k.key = this.key;
            k.display = this.display;
            k.macro = this.macro;
            k.popupKey = this.popupKey;
            k.popupDisplay = this.popupDisplay;
            k.popupMacro = this.popupMacro;
            return k;
        }

        public String getVisualLabel() {
            if (!TextUtils.isEmpty(display)) return display;
            if (!TextUtils.isEmpty(key)) return getDefaultDisplayForKey(key);
            if (!TextUtils.isEmpty(macro)) return macro;
            return "?";
        }

        public String getVisualPopupLabel() {
            if (!TextUtils.isEmpty(popupDisplay)) return popupDisplay;
            if (!TextUtils.isEmpty(popupKey)) return getDefaultDisplayForKey(popupKey);
            if (!TextUtils.isEmpty(popupMacro)) return popupMacro;
            return "";
        }
    }

    public static String getDefaultDisplayForKey(String key) {
        if (key == null) return "";
        switch (key) {
            case "UP": return "↑";
            case "DOWN": return "↓";
            case "LEFT": return "←";
            case "RIGHT": return "→";
            case "KEYBOARD": return "⌨";
            case "DRAWER": return "☰";
            case "SCROLL": return "⇳";
            case "PASTE": return "PASTE";
            case "ENTER": return "↲";
            case "TAB": return "↹";
            case "BKSP": return "⌫";
            case "DEL": return "⌦";
            default: return key;
        }
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        ));
    }

    private static GradientDrawable makeShape(int bgColor, int strokeColor, int strokeWidthPx, int radiusPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(bgColor);
        gd.setCornerRadius(radiusPx);
        if (strokeWidthPx > 0) {
            gd.setStroke(strokeWidthPx, strokeColor);
        }
        return gd;
    }

    private static RippleDrawable makeRipple(int normalColor, int rippleColor, int strokeColor, int strokeWidthPx, int radiusPx) {
        GradientDrawable content = makeShape(normalColor, strokeColor, strokeWidthPx, radiusPx);
        GradientDrawable mask = makeShape(Color.WHITE, 0, 0, radiusPx);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public static void show(Context context, OnSaveListener onSaveListener) {
        if (context == null) return;

        TermuxAppSharedPreferences prefs = TermuxAppSharedPreferences.build(context, true);
        if (prefs == null) return;

        String currentJson = prefs.getExtraKeysCustomJson();
        if (TextUtils.isEmpty(currentJson)) {
            currentJson = TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS;
        }

        List<List<KeyItem>> rows = parseJsonToRows(currentJson);
        if (rows.isEmpty()) {
            rows = parseJsonToRows(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS);
        }

        showVisualEditorDialog(context, prefs, rows, onSaveListener);
    }

    private static List<List<KeyItem>> parseJsonToRows(String jsonStr) {
        List<List<KeyItem>> rows = new ArrayList<>();
        if (TextUtils.isEmpty(jsonStr)) return rows;

        try {
            JSONArray root = new JSONArray(jsonStr);
            for (int r = 0; r < root.length(); r++) {
                JSONArray rowArr = root.getJSONArray(r);
                List<KeyItem> row = new ArrayList<>();
                for (int c = 0; c < rowArr.length(); c++) {
                    KeyItem item = new KeyItem();
                    Object obj = rowArr.get(c);
                    if (obj instanceof String) {
                        item.key = (String) obj;
                        item.display = (String) obj;
                    } else if (obj instanceof JSONObject) {
                        JSONObject jsonKey = (JSONObject) obj;
                        item.key = jsonKey.optString("key", "");
                        item.display = jsonKey.optString("display", "");
                        item.macro = jsonKey.optString("macro", "");

                        Object popupObj = jsonKey.opt("popup");
                        if (popupObj instanceof String) {
                            item.popupKey = (String) popupObj;
                        } else if (popupObj instanceof JSONObject) {
                            JSONObject pop = (JSONObject) popupObj;
                            item.popupKey = pop.optString("key", "");
                            item.popupDisplay = pop.optString("display", "");
                            item.popupMacro = pop.optString("macro", "");
                        }
                    }
                    row.add(item);
                }
                rows.add(row);
            }
        } catch (JSONException ignored) {}
        return rows;
    }

    private static String serializeRowsToJson(List<List<KeyItem>> rows) {
        JSONArray root = new JSONArray();
        for (List<KeyItem> row : rows) {
            JSONArray rowArr = new JSONArray();
            for (KeyItem item : row) {
                boolean hasPopup = !TextUtils.isEmpty(item.popupKey)
                    || !TextUtils.isEmpty(item.popupDisplay)
                    || !TextUtils.isEmpty(item.popupMacro);
                boolean hasDisplay;
                if ("PASTE".equals(item.key)) {
                    hasDisplay = true;
                    if (TextUtils.isEmpty(item.display)) item.display = "PASTE";
                } else {
                    hasDisplay = !TextUtils.isEmpty(item.display)
                        && !item.display.equals(item.key)
                        && !item.display.equals(getDefaultDisplayForKey(item.key));
                }
                boolean hasMacro = !TextUtils.isEmpty(item.macro);

                if (!hasPopup && !hasDisplay && !hasMacro && !TextUtils.isEmpty(item.key)) {
                    rowArr.put(item.key);
                } else {
                    JSONObject keyObj = new JSONObject();
                    try {
                        if (hasMacro) {
                            keyObj.put("macro", item.macro);
                        } else if (!TextUtils.isEmpty(item.key)) {
                            keyObj.put("key", item.key);
                        }
                        if (!TextUtils.isEmpty(item.display)) {
                            keyObj.put("display", item.display);
                        }
                        if (hasPopup) {
                            if (TextUtils.isEmpty(item.popupDisplay) && TextUtils.isEmpty(item.popupMacro)) {
                                keyObj.put("popup", item.popupKey);
                            } else {
                                JSONObject popObj = new JSONObject();
                                if (!TextUtils.isEmpty(item.popupMacro)) {
                                    popObj.put("macro", item.popupMacro);
                                } else if (!TextUtils.isEmpty(item.popupKey)) {
                                    popObj.put("key", item.popupKey);
                                }
                                if (!TextUtils.isEmpty(item.popupDisplay)) {
                                    popObj.put("display", item.popupDisplay);
                                }
                                keyObj.put("popup", popObj);
                            }
                        }
                        rowArr.put(keyObj);
                    } catch (JSONException ignored) {}
                }
            }
            root.put(rowArr);
        }
        try {
            return root.toString(2);
        } catch (JSONException e) {
            return root.toString();
        }
    }

    private static void showVisualEditorDialog(Context context, TermuxAppSharedPreferences prefs,
                                              List<List<KeyItem>> rows, OnSaveListener onSaveListener) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 8));

        TextView tvHint = new TextView(context);
        tvHint.setText("Chạm vào từng phím để cấu hình hoặc gán thao tác vuốt lên (popup).");
        tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvHint.setTextColor(0xFF9E9E9E);
        tvHint.setPadding(0, 0, 0, dp(context, 12));
        root.addView(tvHint);

        ScrollView mainScroll = new ScrollView(context);
        mainScroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        root.addView(mainScroll, scrollLp);

        LinearLayout gridContainer = new LinearLayout(context);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        mainScroll.addView(gridContainer);

        // Bottom Operations Bar
        LinearLayout opsBar = new LinearLayout(context);
        opsBar.setOrientation(LinearLayout.HORIZONTAL);
        opsBar.setGravity(Gravity.CENTER_VERTICAL);
        opsBar.setPadding(0, dp(context, 10), 0, 0);

        Button btnAddRow = new Button(context);
        btnAddRow.setText("+ Thêm hàng");
        btnAddRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAddRow.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddRow.setTextColor(Color.WHITE);
        btnAddRow.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams arLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnAddRow.setLayoutParams(arLp);
        opsBar.addView(btnAddRow);

        Button btnReset = new Button(context);
        btnReset.setText("Đặt lại mặc định");
        btnReset.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnReset.setTextColor(0xFFB0B0B0);
        btnReset.setBackground(makeRipple(0xFF1E1E1E, 0x33FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams rstLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rstLp.leftMargin = dp(context, 8);
        btnReset.setLayoutParams(rstLp);
        opsBar.addView(btnReset);

        root.addView(opsBar);

        Runnable[] refreshHolder = new Runnable[1];

        refreshHolder[0] = () -> {
            gridContainer.removeAllViews();
            for (int r = 0; r < rows.size(); r++) {
                final int rowIdx = r;
                final List<KeyItem> row = rows.get(r);

                LinearLayout rowCard = new LinearLayout(context);
                rowCard.setOrientation(LinearLayout.VERTICAL);
                rowCard.setBackground(makeShape(0xFF181818, 0x22FFFFFF, dp(context, 1), dp(context, 6)));
                rowCard.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 10));
                LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardLp.bottomMargin = dp(context, 10);
                rowCard.setLayoutParams(cardLp);

                // Row Header
                LinearLayout headerRow = new LinearLayout(context);
                headerRow.setOrientation(LinearLayout.HORIZONTAL);
                headerRow.setGravity(Gravity.CENTER_VERTICAL);
                headerRow.setPadding(0, 0, 0, dp(context, 6));

                TextView tvRowName = new TextView(context);
                tvRowName.setText("HÀNG " + (rowIdx + 1) + " (" + row.size() + " phím)");
                tvRowName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                tvRowName.setTypeface(Typeface.DEFAULT_BOLD);
                tvRowName.setTextColor(0xFFB0B0B0);
                headerRow.addView(tvRowName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                Button btnAddKey = new Button(context);
                btnAddKey.setText("+ Phím");
                btnAddKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                btnAddKey.setTextColor(Color.WHITE);
                btnAddKey.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
                btnAddKey.setMinHeight(dp(context, 30));
                btnAddKey.setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2));
                btnAddKey.setOnClickListener(v -> {
                    promptPickKey(context, (key, display) -> {
                        KeyItem newItem = new KeyItem();
                        newItem.key = key;
                        newItem.display = display;
                        row.add(newItem);
                        refreshHolder[0].run();
                    });
                });
                headerRow.addView(btnAddKey);

                Button btnRemRow = new Button(context);
                btnRemRow.setText("- Hàng");
                btnRemRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                btnRemRow.setTextColor(0xFFE0E0E0);
                btnRemRow.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 4)));
                btnRemRow.setMinHeight(dp(context, 30));
                btnRemRow.setPadding(dp(context, 8), dp(context, 2), dp(context, 8), dp(context, 2));
                LinearLayout.LayoutParams remLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                remLp.leftMargin = dp(context, 6);
                btnRemRow.setLayoutParams(remLp);
                btnRemRow.setOnClickListener(v -> {
                    if (rows.size() > 1) {
                        rows.remove(rowIdx);
                        refreshHolder[0].run();
                    } else {
                        Toast.makeText(context, "Cần giữ ít nhất 1 hàng phím", Toast.LENGTH_SHORT).show();
                    }
                });
                headerRow.addView(btnRemRow);

                rowCard.addView(headerRow);

                // Horizontal Keys Scroll
                HorizontalScrollView hScroll = new HorizontalScrollView(context);
                hScroll.setHorizontalScrollBarEnabled(false);
                hScroll.setFillViewport(true);

                LinearLayout keysRowLayout = new LinearLayout(context);
                keysRowLayout.setOrientation(LinearLayout.HORIZONTAL);
                keysRowLayout.setGravity(Gravity.CENTER_VERTICAL);

                for (int c = 0; c < row.size(); c++) {
                    final int colIdx = c;
                    final KeyItem item = row.get(c);

                    Button btnKey = new Button(context);
                    btnKey.setBackground(makeRipple(0xFF222222, 0x44FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
                    btnKey.setTextColor(Color.WHITE);
                    btnKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    btnKey.setTypeface(Typeface.MONOSPACE);
                    btnKey.setMinHeight(dp(context, 44));
                    btnKey.setMinWidth(dp(context, 44));
                    btnKey.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
                    btnKey.setAllCaps(false);

                    String label = item.getVisualLabel();
                    String popupLabel = item.getVisualPopupLabel();
                    if (!TextUtils.isEmpty(popupLabel)) {
                        btnKey.setText(label + "\n↑" + popupLabel);
                    } else {
                        btnKey.setText(label);
                    }

                    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    btnLp.setMargins(dp(context, 2), 0, dp(context, 2), 0);
                    btnKey.setLayoutParams(btnLp);

                    btnKey.setOnClickListener(v -> {
                        showKeyEditorDialog(context, rows, rowIdx, colIdx, refreshHolder[0]);
                    });

                    keysRowLayout.addView(btnKey);
                }

                hScroll.addView(keysRowLayout);
                rowCard.addView(hScroll);
                gridContainer.addView(rowCard);
            }
        };

        btnAddRow.setOnClickListener(v -> {
            List<KeyItem> newRow = new ArrayList<>();
            for (String k : new String[]{"F1", "F2", "F3", "F4"}) {
                KeyItem it = new KeyItem();
                it.key = k;
                it.display = k;
                newRow.add(it);
            }
            rows.add(newRow);
            refreshHolder[0].run();
        });

        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Đặt lại mặc định")
                .setMessage("Khôi phục danh sách phím về cấu hình 2 hàng mặc định của Termux?")
                .setPositiveButton("Đặt lại", (d, which) -> {
                    rows.clear();
                    rows.addAll(parseJsonToRows(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS));
                    refreshHolder[0].run();
                })
                .setNegativeButton("Hủy", null)
                .show();
        });

        refreshHolder[0].run();

        new AlertDialog.Builder(context)
            .setTitle("Bàn phím phụ (Extra Keys)")
            .setView(root)
            .setPositiveButton("Lưu", (dialog, which) -> {
                String jsonOutput = serializeRowsToJson(rows);
                prefs.setExtraKeysCustomJson(jsonOutput);
                prefs.setExtraKeysUseCustom(true);
                if (onSaveListener != null) {
                    onSaveListener.onSaved();
                }
                Toast.makeText(context, "Đã lưu cấu hình phím", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private static void showKeyEditorDialog(Context context, List<List<KeyItem>> allRows,
                                           int rowIdx, int initialColIdx, Runnable onGlobalUpdated) {
        final List<KeyItem> row = allRows.get(rowIdx);
        final int[] currentPos = new int[]{initialColIdx};

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 8));

        // Key Preview
        final Button previewBtn = new Button(context);
        previewBtn.setBackground(makeShape(0xFF222222, 0x44FFFFFF, dp(context, 1), dp(context, 4)));
        previewBtn.setTextColor(Color.WHITE);
        previewBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        previewBtn.setTypeface(Typeface.MONOSPACE);
        previewBtn.setMinHeight(dp(context, 48));
        previewBtn.setMinWidth(dp(context, 64));
        previewBtn.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
        previewBtn.setAllCaps(false);
        previewBtn.setClickable(false);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        prevLp.gravity = Gravity.CENTER_HORIZONTAL;
        prevLp.bottomMargin = dp(context, 12);
        previewBtn.setLayoutParams(prevLp);
        root.addView(previewBtn);

        // Display Label Input
        TextView tvLabelHeader = new TextView(context);
        tvLabelHeader.setText("Nhãn hiển thị:");
        tvLabelHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelHeader.setTextColor(0xFF9E9E9E);
        root.addView(tvLabelHeader);

        final EditText etDisplay = new EditText(context);
        etDisplay.setText(row.get(currentPos[0]).getVisualLabel());
        etDisplay.setTextColor(Color.WHITE);
        etDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etDisplay.setTypeface(Typeface.MONOSPACE);
        etDisplay.setBackground(makeShape(0xFF1E1E1E, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
        etDisplay.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        etLp.topMargin = dp(context, 4);
        etLp.bottomMargin = dp(context, 10);
        etDisplay.setLayoutParams(etLp);
        root.addView(etDisplay);

        // Action selection row
        TextView tvActionHeader = new TextView(context);
        tvActionHeader.setText("Hành động khi nhấn:");
        tvActionHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvActionHeader.setTextColor(0xFF9E9E9E);
        root.addView(tvActionHeader);

        final Button btnPickAction = new Button(context);
        btnPickAction.setText(describeAction(row.get(currentPos[0]).key, row.get(currentPos[0]).macro));
        btnPickAction.setTextColor(Color.WHITE);
        btnPickAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnPickAction.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(context, 4);
        actLp.bottomMargin = dp(context, 10);
        btnPickAction.setLayoutParams(actLp);
        root.addView(btnPickAction);

        // Popup selection row
        TextView tvPopupHeader = new TextView(context);
        tvPopupHeader.setText("Vuốt lên (Popup):");
        tvPopupHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPopupHeader.setTextColor(0xFF9E9E9E);
        root.addView(tvPopupHeader);

        LinearLayout popupRow = new LinearLayout(context);
        popupRow.setOrientation(LinearLayout.HORIZONTAL);
        popupRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams popRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popRowLp.topMargin = dp(context, 4);
        popRowLp.bottomMargin = dp(context, 10);
        popupRow.setLayoutParams(popRowLp);

        final Button btnPickPopup = new Button(context);
        btnPickPopup.setText(describeAction(row.get(currentPos[0]).popupKey, row.get(currentPos[0]).popupMacro));
        btnPickPopup.setTextColor(Color.WHITE);
        btnPickPopup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnPickPopup.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnPickPopup.setLayoutParams(bpLp);
        popupRow.addView(btnPickPopup);

        final Button btnClearPopup = new Button(context);
        btnClearPopup.setText("Xóa");
        btnClearPopup.setTextColor(0xFFB0B0B0);
        btnClearPopup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnClearPopup.setBackground(makeRipple(0xFF1E1E1E, 0x33FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bcLp.leftMargin = dp(context, 6);
        btnClearPopup.setLayoutParams(bcLp);
        popupRow.addView(btnClearPopup);
        root.addView(popupRow);

        // Move Left / Move Right Buttons
        LinearLayout reorderRow = new LinearLayout(context);
        reorderRow.setOrientation(LinearLayout.HORIZONTAL);
        reorderRow.setGravity(Gravity.CENTER_VERTICAL);
        reorderRow.setPadding(0, dp(context, 2), 0, dp(context, 4));

        final Button btnMoveLeft = new Button(context);
        btnMoveLeft.setText("◀ Sang trái");
        btnMoveLeft.setTextColor(Color.WHITE);
        btnMoveLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnMoveLeft.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams mlLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnMoveLeft.setLayoutParams(mlLp);
        reorderRow.addView(btnMoveLeft);

        final Button btnMoveRight = new Button(context);
        btnMoveRight.setText("Sang phải ▶");
        btnMoveRight.setTextColor(Color.WHITE);
        btnMoveRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnMoveRight.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 4)));
        LinearLayout.LayoutParams mrLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mrLp.leftMargin = dp(context, 6);
        btnMoveRight.setLayoutParams(mrLp);
        reorderRow.addView(btnMoveRight);

        root.addView(reorderRow);

        Runnable updatePreview = () -> {
            if (currentPos[0] < 0 || currentPos[0] >= row.size()) return;
            KeyItem it = row.get(currentPos[0]);
            String lbl = etDisplay.getText().toString();
            if (TextUtils.isEmpty(lbl)) lbl = it.getVisualLabel();
            String pop = it.getVisualPopupLabel();
            if (!TextUtils.isEmpty(pop)) {
                previewBtn.setText(lbl + "\n↑" + pop);
            } else {
                previewBtn.setText(lbl);
            }
            btnMoveLeft.setEnabled(currentPos[0] > 0);
            btnMoveRight.setEnabled(currentPos[0] < row.size() - 1);
        };
        updatePreview.run();

        etDisplay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updatePreview.run(); }
        });

        btnPickAction.setOnClickListener(v -> {
            promptPickKey(context, (key, display) -> {
                KeyItem it = row.get(currentPos[0]);
                if (key != null && key.contains(" ")) {
                    it.macro = key;
                    it.key = "";
                } else {
                    it.key = key != null ? key : "";
                    it.macro = "";
                }
                it.display = display;
                etDisplay.setText(display);
                btnPickAction.setText(describeAction(it.key, it.macro));
                updatePreview.run();
                onGlobalUpdated.run();
            });
        });

        btnPickPopup.setOnClickListener(v -> {
            promptPickKey(context, (key, display) -> {
                KeyItem it = row.get(currentPos[0]);
                if (key != null && key.contains(" ")) {
                    it.popupMacro = key;
                    it.popupKey = "";
                } else {
                    it.popupKey = key != null ? key : "";
                    it.popupMacro = "";
                }
                it.popupDisplay = display;
                btnPickPopup.setText(describeAction(it.popupKey, it.popupMacro));
                updatePreview.run();
                onGlobalUpdated.run();
            });
        });

        btnClearPopup.setOnClickListener(v -> {
            KeyItem it = row.get(currentPos[0]);
            it.popupKey = "";
            it.popupDisplay = "";
            it.popupMacro = "";
            btnPickPopup.setText("Không có (None)");
            updatePreview.run();
            onGlobalUpdated.run();
        });

        // Reorder without dismissing dialog!
        btnMoveLeft.setOnClickListener(v -> {
            int p = currentPos[0];
            if (p > 0) {
                KeyItem it = row.remove(p);
                row.add(p - 1, it);
                currentPos[0] = p - 1;
                updatePreview.run();
                onGlobalUpdated.run();
            }
        });

        btnMoveRight.setOnClickListener(v -> {
            int p = currentPos[0];
            if (p < row.size() - 1) {
                KeyItem it = row.remove(p);
                row.add(p + 1, it);
                currentPos[0] = p + 1;
                updatePreview.run();
                onGlobalUpdated.run();
            }
        });

        new AlertDialog.Builder(context)
            .setTitle("Cấu hình phím")
            .setView(root)
            .setPositiveButton("Xong", (d, which) -> {
                KeyItem it = row.get(currentPos[0]);
                String entered = etDisplay.getText().toString().trim();
                if (!entered.isEmpty()) {
                    it.display = entered;
                }
                onGlobalUpdated.run();
            })
            .setNegativeButton("Hủy", null)
            .setNeutralButton("Xóa phím", (d, which) -> {
                row.remove(currentPos[0]);
                onGlobalUpdated.run();
            })
            .show();
    }

    private static String describeAction(String key, String macro) {
        if (!TextUtils.isEmpty(macro)) return "Macro: " + macro;
        if (TextUtils.isEmpty(key)) return "Không có (None)";
        switch (key) {
            case "KEYBOARD": return "Bàn phím ảo (KEYBOARD)";
            case "DRAWER": return "Ngăn kéo bên trái (DRAWER)";
            case "PASTE": return "Dán văn bản (PASTE)";
            case "SCROLL": return "Khóa/mở cuộn tự động (Lock / Pin Scroll)";
            default: return "Phím: " + key;
        }
    }

    interface KeyPickCallback {
        void onSelected(String key, String display);
    }

    private static void promptPickKey(Context context, KeyPickCallback callback) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 8));

        // Tabbed Partitioning bar to avoid excessive vertical touch scrolling
        HorizontalScrollView tabScroll = new HorizontalScrollView(context);
        tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setFillViewport(true);

        LinearLayout tabLayout = new LinearLayout(context);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setPadding(0, 0, 0, dp(context, 10));

        final String[] tabTitles = new String[]{
            "Hệ thống", "Điều hướng", "Ký tự", "Phím F", "Tùy chỉnh"
        };
        final Button[] tabButtons = new Button[tabTitles.length];

        ScrollView contentScroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        contentScroll.addView(content);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        Runnable[] showTab = new Runnable[tabTitles.length];

        showTab[0] = () -> {
            content.removeAllViews();
            addCategoryHeader(context, content, "HÀNH ĐỘNG TERMINAL");
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"KEYBOARD", "DRAWER", "PASTE", "SCROLL"},
                new String[]{"⌨", "☰", "PASTE", "⇳"}
            );
            addCategoryHeader(context, content, "PHÍM BỔ TRỢ (MODIFIERS)");
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"CTRL", "ALT", "SHIFT", "FN"},
                new String[]{"CTRL", "ALT", "SHIFT", "FN"}
            );
        };

        showTab[1] = () -> {
            content.removeAllViews();
            addCategoryHeader(context, content, "ĐIỀU HƯỚNG & SOẠN THẢO");
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"ESC", "TAB", "ENTER", "BKSP", "DEL", "INS", "SPACE"},
                new String[]{"ESC", "TAB", "ENTER", "BKSP", "DEL", "INS", "SPACE"}
            );
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"UP", "DOWN", "LEFT", "RIGHT"},
                new String[]{"↑", "↓", "←", "→"}
            );
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"HOME", "END", "PGUP", "PGDN"},
                new String[]{"HOME", "END", "PGUP", "PGDN"}
            );
        };

        showTab[2] = () -> {
            content.removeAllViews();
            addCategoryHeader(context, content, "KÝ TỰ & BIỂU TƯỢNG CODE");
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"/", "-", "_", "|", "~", "\\", "$", "@"},
                new String[]{"/", "-", "_", "|", "~", "\\", "$", "@"}
            );
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"=", "+", "*", "&", "%", "#", "!", "?"},
                new String[]{"=", "+", "*", "&", "%", "#", "!", "?"}
            );
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{":", ";", "\"", "'", "`", "^", "<", ">"},
                new String[]{":", ";", "\"", "'", "`", "^", "<", ">"}
            );
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"{", "}", "[", "]", "(", ")", ",", "."},
                new String[]{"{", "}", "[", "]", "(", ")", ",", "."}
            );
        };

        showTab[3] = () -> {
            content.removeAllViews();
            addCategoryHeader(context, content, "PHÍM CHỨC NĂNG (F1 - F12)");
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"F1", "F2", "F3", "F4", "F5", "F6"},
                new String[]{"F1", "F2", "F3", "F4", "F5", "F6"}
            );
            addKeyButtonsRow(context, content, dialogHolder[0], callback,
                new String[]{"F7", "F8", "F9", "F10", "F11", "F12"},
                new String[]{"F7", "F8", "F9", "F10", "F11", "F12"}
            );
        };

        showTab[4] = () -> {
            content.removeAllViews();
            addCategoryHeader(context, content, "NHẬP TÙY CHỌN / LỆNH BẤT KỲ");
            LinearLayout customRow = new LinearLayout(context);
            customRow.setOrientation(LinearLayout.HORIZONTAL);
            customRow.setGravity(Gravity.CENTER_VERTICAL);
            customRow.setPadding(0, dp(context, 4), 0, dp(context, 10));

            final EditText etCustom = new EditText(context);
            etCustom.setHint("VD: clear, :wq, tmux, CTRL c");
            etCustom.setHintTextColor(0xFF6B7280);
            etCustom.setTextColor(Color.WHITE);
            etCustom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            etCustom.setTypeface(Typeface.MONOSPACE);
            etCustom.setBackground(makeShape(0xFF1E1E1E, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
            etCustom.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
            LinearLayout.LayoutParams customLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            etCustom.setLayoutParams(customLp);
            customRow.addView(etCustom);

            Button btnAddCustom = new Button(context);
            btnAddCustom.setText("Dùng");
            btnAddCustom.setTextColor(Color.WHITE);
            btnAddCustom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btnAddCustom.setTypeface(Typeface.DEFAULT_BOLD);
            btnAddCustom.setBackground(makeRipple(0xFF262626, 0x33FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
            LinearLayout.LayoutParams btnCustomLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnCustomLp.leftMargin = dp(context, 6);
            btnAddCustom.setLayoutParams(btnCustomLp);
            btnAddCustom.setOnClickListener(v -> {
                String val = etCustom.getText().toString().trim();
                if (!val.isEmpty()) {
                    callback.onSelected(val, val);
                    if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                }
            });
            customRow.addView(btnAddCustom);
            content.addView(customRow);
        };

        for (int i = 0; i < tabTitles.length; i++) {
            final int tabIdx = i;
            Button tabBtn = new Button(context);
            tabBtn.setText(tabTitles[i]);
            tabBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tabBtn.setTypeface(Typeface.DEFAULT_BOLD);
            tabBtn.setMinHeight(dp(context, 32));
            tabBtn.setPadding(dp(context, 10), dp(context, 2), dp(context, 10), dp(context, 2));
            tabBtn.setAllCaps(false);

            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tabLp.setMargins(dp(context, 2), 0, dp(context, 2), 0);
            tabBtn.setLayoutParams(tabLp);

            tabBtn.setOnClickListener(v -> {
                for (int j = 0; j < tabButtons.length; j++) {
                    if (j == tabIdx) {
                        tabButtons[j].setBackground(makeShape(0xFF383838, 0x44FFFFFF, dp(context, 1), dp(context, 4)));
                        tabButtons[j].setTextColor(Color.WHITE);
                    } else {
                        tabButtons[j].setBackground(makeRipple(Color.TRANSPARENT, 0x22FFFFFF, 0, 0, dp(context, 4)));
                        tabButtons[j].setTextColor(0xFF888888);
                    }
                }
                showTab[tabIdx].run();
            });

            tabButtons[i] = tabBtn;
            tabLayout.addView(tabBtn);
        }

        tabScroll.addView(tabLayout);
        root.addView(tabScroll);
        root.addView(contentScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Select first tab by default
        tabButtons[0].performClick();

        dialogHolder[0] = new AlertDialog.Builder(context)
            .setTitle("Chọn phím")
            .setView(root)
            .setNegativeButton("Đóng", null)
            .show();
    }

    private static void addCategoryHeader(Context context, LinearLayout parent, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFF888888);
        tv.setPadding(0, dp(context, 8), 0, dp(context, 4));
        parent.addView(tv);
    }

    private static void addKeyButtonsRow(Context context, LinearLayout parent, AlertDialog dialog,
                                        KeyPickCallback callback, String[] keys, String[] displays) {
        HorizontalScrollView hScroll = new HorizontalScrollView(context);
        hScroll.setHorizontalScrollBarEnabled(false);
        hScroll.setFillViewport(true);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 2), 0, dp(context, 6));

        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            final String d = (displays != null && i < displays.length) ? displays[i] : k;

            Button btn = new Button(context);
            btn.setText(d);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setTypeface(Typeface.MONOSPACE);
            btn.setBackground(makeRipple(0xFF222222, 0x44FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 4)));
            btn.setMinHeight(dp(context, 40));
            btn.setMinWidth(dp(context, 40));
            btn.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
            btn.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dp(context, 2), 0, dp(context, 2), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> {
                callback.onSelected(k, d);
                if (dialog != null) dialog.dismiss();
            });

            row.addView(btn);
        }

        hScroll.addView(row);
        parent.addView(hScroll);
    }
}
