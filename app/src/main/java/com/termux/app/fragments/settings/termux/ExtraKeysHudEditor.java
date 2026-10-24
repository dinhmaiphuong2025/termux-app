package com.termux.app.fragments.settings.termux;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
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
            if (!TextUtils.isEmpty(key)) return key;
            if (!TextUtils.isEmpty(macro)) return macro;
            return "?";
        }

        public String getVisualPopupLabel() {
            if (!TextUtils.isEmpty(popupDisplay)) return popupDisplay;
            if (!TextUtils.isEmpty(popupKey)) return popupKey;
            if (!TextUtils.isEmpty(popupMacro)) return popupMacro;
            return "";
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
                boolean hasDisplay = !TextUtils.isEmpty(item.display) && !item.display.equals(item.key);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        root.setBackgroundColor(0xFF101319);

        // Header Title
        TextView tvTitle = new TextView(context);
        tvTitle.setText("BÀN PHÍM PHỤ EXTRA KEYS (HUD EDITOR)");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Chạm vào phím để sửa; bấm '+' để thêm phím/hàng mới.");
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSub.setTextColor(0xFF9EA3AF);
        tvSub.setPadding(0, dp(context, 2), 0, dp(context, 10));
        root.addView(tvSub);

        // Scrollable Grid of Rows
        ScrollView mainScroll = new ScrollView(context);
        mainScroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        root.addView(mainScroll, scrollLp);

        LinearLayout gridContainer = new LinearLayout(context);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        mainScroll.addView(gridContainer);

        AlertDialog dialog = builder.setView(root).create();

        Runnable refreshGrid = new Runnable() {
            @Override
            public void run() {
                gridContainer.removeAllViews();
                for (int r = 0; r < rows.size(); r++) {
                    final int rowIdx = r;
                    final List<KeyItem> row = rows.get(r);

                    // Row Card Container
                    LinearLayout rowCard = new LinearLayout(context);
                    rowCard.setOrientation(LinearLayout.VERTICAL);
                    rowCard.setBackground(makeShape(0xFF161A22, 0x33FFFFFF, dp(context, 1), dp(context, 10)));
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
                    tvRowName.setText("HÀNG " + (rowIdx + 1) + "  (" + row.size() + " phím)");
                    tvRowName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    tvRowName.setTypeface(Typeface.DEFAULT_BOLD);
                    tvRowName.setTextColor(0xFF4FC3F7);
                    headerRow.addView(tvRowName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    Button btnAddKey = new Button(context);
                    btnAddKey.setText("+ Thêm phím");
                    btnAddKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                    btnAddKey.setTextColor(0xFFFFFFFF);
                    btnAddKey.setBackground(makeRipple(0xFF263238, 0x44FFFFFF, 0x334FC3F7, dp(context, 1), dp(context, 6)));
                    btnAddKey.setMinHeight(dp(context, 32));
                    btnAddKey.setPadding(dp(context, 10), dp(context, 2), dp(context, 10), dp(context, 2));
                    btnAddKey.setOnClickListener(v -> {
                        promptPickKey(context, (key, display) -> {
                            KeyItem newItem = new KeyItem();
                            newItem.key = key;
                            newItem.display = display;
                            row.add(newItem);
                            run();
                        });
                    });
                    headerRow.addView(btnAddKey);

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
                        btnKey.setBackground(makeRipple(0xFF1E2430, 0x55FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 6)));
                        btnKey.setTextColor(Color.WHITE);
                        btnKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        btnKey.setTypeface(Typeface.DEFAULT_BOLD);
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
                        btnLp.setMargins(dp(context, 3), 0, dp(context, 3), 0);
                        btnKey.setLayoutParams(btnLp);

                        btnKey.setOnClickListener(v -> {
                            showKeyEditorDialog(context, row, colIdx, this);
                        });

                        keysRowLayout.addView(btnKey);
                    }

                    hScroll.addView(keysRowLayout);
                    rowCard.addView(hScroll);
                    gridContainer.addView(rowCard);
                }
            }
        };

        refreshGrid.run();

        // Control Buttons Row 1 (Add Row, Remove Row, Reset Default)
        LinearLayout opsRow = new LinearLayout(context);
        opsRow.setOrientation(LinearLayout.HORIZONTAL);
        opsRow.setPadding(0, dp(context, 8), 0, dp(context, 6));

        Button btnAddRow = new Button(context);
        btnAddRow.setText("+ Thêm hàng");
        btnAddRow.setTextColor(0xFFFFFFFF);
        btnAddRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAddRow.setBackground(makeRipple(0xFF1A2633, 0x44FFFFFF, 0x334FC3F7, dp(context, 1), dp(context, 8)));
        LinearLayout.LayoutParams arLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnAddRow.setLayoutParams(arLp);
        btnAddRow.setOnClickListener(v -> {
            List<KeyItem> newRow = new ArrayList<>();
            for (String k : new String[]{"F1", "F2", "F3", "F4"}) {
                KeyItem it = new KeyItem();
                it.key = k;
                it.display = k;
                newRow.add(it);
            }
            rows.add(newRow);
            refreshGrid.run();
        });
        opsRow.addView(btnAddRow);

        Button btnRemRow = new Button(context);
        btnRemRow.setText("- Xóa hàng");
        btnRemRow.setTextColor(0xFFFFFFFF);
        btnRemRow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnRemRow.setBackground(makeRipple(0xFF261D22, 0x44FFFFFF, 0x33EF5350, dp(context, 1), dp(context, 8)));
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rrLp.leftMargin = dp(context, 6);
        btnRemRow.setLayoutParams(rrLp);
        btnRemRow.setOnClickListener(v -> {
            if (rows.size() > 1) {
                rows.remove(rows.size() - 1);
                refreshGrid.run();
            } else {
                Toast.makeText(context, "Cần giữ ít nhất 1 hàng phím", Toast.LENGTH_SHORT).show();
            }
        });
        opsRow.addView(btnRemRow);

        Button btnReset = new Button(context);
        btnReset.setText("Mặc định");
        btnReset.setTextColor(0xFFE0E0E0);
        btnReset.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnReset.setBackground(makeRipple(0xFF21252D, 0x44FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 8)));
        LinearLayout.LayoutParams rstLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rstLp.leftMargin = dp(context, 6);
        btnReset.setLayoutParams(rstLp);
        btnReset.setOnClickListener(v -> {
            rows.clear();
            rows.addAll(parseJsonToRows(TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS));
            refreshGrid.run();
            Toast.makeText(context, "Đã hoàn về 2 hàng phím mặc định", Toast.LENGTH_SHORT).show();
        });
        opsRow.addView(btnReset);

        root.addView(opsRow);

        // Bottom Action (Save & Apply)
        Button btnSave = new Button(context);
        btnSave.setText("LƯU & ÁP DỤNG");
        btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setTextColor(0xFF0D1117);
        btnSave.setBackground(makeRipple(0xFF4FC3F7, 0x44000000, 0xFF81D4FA, dp(context, 1), dp(context, 8)));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        saveLp.topMargin = dp(context, 4);
        btnSave.setLayoutParams(saveLp);
        btnSave.setOnClickListener(v -> {
            String jsonOutput = serializeRowsToJson(rows);
            prefs.setExtraKeysCustomJson(jsonOutput);
            prefs.setExtraKeysUseCustom(true);
            dialog.dismiss();
            if (onSaveListener != null) {
                onSaveListener.onSaved();
            }
            Toast.makeText(context, "Đã lưu cấu hình phím thành công", Toast.LENGTH_SHORT).show();
        });
        root.addView(btnSave);

        dialog.show();
    }

    private static void showKeyEditorDialog(Context context, List<KeyItem> row, int colIdx, Runnable onUpdated) {
        if (colIdx < 0 || colIdx >= row.size()) return;
        final KeyItem item = row.get(colIdx);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        root.setBackgroundColor(0xFF141820);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("CẤU HÌNH PHÍM");
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(0xFFFFFFFF);
        root.addView(tvTitle);

        ScrollView scroll = new ScrollView(context);
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Preview Button
        final Button previewBtn = new Button(context);
        previewBtn.setBackground(makeShape(0xFF1E2430, 0x55FFFFFF, dp(context, 1), dp(context, 8)));
        previewBtn.setTextColor(Color.WHITE);
        previewBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        previewBtn.setTypeface(Typeface.DEFAULT_BOLD);
        previewBtn.setMinHeight(dp(context, 48));
        previewBtn.setMinWidth(dp(context, 60));
        previewBtn.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
        previewBtn.setAllCaps(false);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        prevLp.gravity = Gravity.CENTER_HORIZONTAL;
        prevLp.topMargin = dp(context, 8);
        prevLp.bottomMargin = dp(context, 12);
        previewBtn.setLayoutParams(prevLp);
        form.addView(previewBtn);

        // Display Label Input
        TextView tvLabelHeader = new TextView(context);
        tvLabelHeader.setText("Nhãn hiển thị trên phím");
        tvLabelHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelHeader.setTextColor(0xFF9EA3AF);
        form.addView(tvLabelHeader);

        final EditText etDisplay = new EditText(context);
        etDisplay.setText(item.getVisualLabel());
        etDisplay.setTextColor(Color.WHITE);
        etDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etDisplay.setBackground(makeShape(0xFF0F1218, 0x33FFFFFF, dp(context, 1), dp(context, 6)));
        etDisplay.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        etLp.topMargin = dp(context, 4);
        etLp.bottomMargin = dp(context, 12);
        etDisplay.setLayoutParams(etLp);
        form.addView(etDisplay);

        // Primary Action Card
        LinearLayout actionCard = new LinearLayout(context);
        actionCard.setOrientation(LinearLayout.VERTICAL);
        actionCard.setBackground(makeShape(0xFF191E27, 0x33FFFFFF, dp(context, 1), dp(context, 8)));
        actionCard.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        LinearLayout.LayoutParams actCardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        actCardLp.bottomMargin = dp(context, 10);
        actionCard.setLayoutParams(actCardLp);

        TextView actTitle = new TextView(context);
        actTitle.setText("HÀNH ĐỘNG CHÍNH KHI NHẤN");
        actTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        actTitle.setTypeface(Typeface.DEFAULT_BOLD);
        actTitle.setTextColor(0xFF4FC3F7);
        actionCard.addView(actTitle);

        final TextView tvActionDesc = new TextView(context);
        tvActionDesc.setText(describeAction(item.key, item.macro));
        tvActionDesc.setTextColor(Color.WHITE);
        tvActionDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvActionDesc.setPadding(0, dp(context, 4), 0, dp(context, 8));
        actionCard.addView(tvActionDesc);

        Button btnPickAction = new Button(context);
        btnPickAction.setText("Chọn phím / hành động");
        btnPickAction.setTextColor(Color.WHITE);
        btnPickAction.setBackground(makeRipple(0xFF263238, 0x44FFFFFF, 0x334FC3F7, dp(context, 1), dp(context, 6)));
        actionCard.addView(btnPickAction);
        form.addView(actionCard);

        // Swipe-up Popup Card
        LinearLayout popupCard = new LinearLayout(context);
        popupCard.setOrientation(LinearLayout.VERTICAL);
        popupCard.setBackground(makeShape(0xFF191E27, 0x33FFFFFF, dp(context, 1), dp(context, 8)));
        popupCard.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        LinearLayout.LayoutParams popCardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        popCardLp.bottomMargin = dp(context, 12);
        popupCard.setLayoutParams(popCardLp);

        TextView popTitle = new TextView(context);
        popTitle.setText("HÀNH ĐỘNG VUỐT LÊN (POPUP - TÙY CHỌN)");
        popTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        popTitle.setTypeface(Typeface.DEFAULT_BOLD);
        popTitle.setTextColor(0xFFFFB74D);
        popupCard.addView(popTitle);

        final TextView tvPopupDesc = new TextView(context);
        tvPopupDesc.setText(describeAction(item.popupKey, item.popupMacro));
        tvPopupDesc.setTextColor(Color.WHITE);
        tvPopupDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPopupDesc.setPadding(0, dp(context, 4), 0, dp(context, 8));
        popupCard.addView(tvPopupDesc);

        LinearLayout popupBtns = new LinearLayout(context);
        popupBtns.setOrientation(LinearLayout.HORIZONTAL);

        Button btnPickPopup = new Button(context);
        btnPickPopup.setText("Gán Popup");
        btnPickPopup.setTextColor(Color.WHITE);
        btnPickPopup.setBackground(makeRipple(0xFF33271F, 0x44FFFFFF, 0x33FFB74D, dp(context, 1), dp(context, 6)));
        LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnPickPopup.setLayoutParams(bpLp);
        popupBtns.addView(btnPickPopup);

        Button btnClearPopup = new Button(context);
        btnClearPopup.setText("Xóa Popup");
        btnClearPopup.setTextColor(0xFFE0E0E0);
        btnClearPopup.setBackground(makeRipple(0xFF22252B, 0x44FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 6)));
        LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bcLp.leftMargin = dp(context, 8);
        btnClearPopup.setLayoutParams(bcLp);
        popupBtns.addView(btnClearPopup);

        popupCard.addView(popupBtns);
        form.addView(popupCard);

        // Position & Delete Row
        LinearLayout reorderRow = new LinearLayout(context);
        reorderRow.setOrientation(LinearLayout.HORIZONTAL);
        reorderRow.setPadding(0, 0, 0, dp(context, 10));

        Button btnMoveLeft = new Button(context);
        btnMoveLeft.setText("← Trái");
        btnMoveLeft.setTextColor(Color.WHITE);
        btnMoveLeft.setEnabled(colIdx > 0);
        btnMoveLeft.setBackground(makeRipple(0xFF21252D, 0x44FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 6)));
        LinearLayout.LayoutParams bmlLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnMoveLeft.setLayoutParams(bmlLp);
        reorderRow.addView(btnMoveLeft);

        Button btnMoveRight = new Button(context);
        btnMoveRight.setText("Phải →");
        btnMoveRight.setTextColor(Color.WHITE);
        btnMoveRight.setEnabled(colIdx < row.size() - 1);
        btnMoveRight.setBackground(makeRipple(0xFF21252D, 0x44FFFFFF, 0x22FFFFFF, dp(context, 1), dp(context, 6)));
        LinearLayout.LayoutParams bmrLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bmrLp.leftMargin = dp(context, 6);
        btnMoveRight.setLayoutParams(bmrLp);
        reorderRow.addView(btnMoveRight);

        Button btnDelete = new Button(context);
        btnDelete.setText("Xóa phím");
        btnDelete.setTextColor(0xFFEF5350);
        btnDelete.setBackground(makeRipple(0xFF2D1619, 0x44FFFFFF, 0x33EF5350, dp(context, 1), dp(context, 6)));
        LinearLayout.LayoutParams bdelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bdelLp.leftMargin = dp(context, 6);
        btnDelete.setLayoutParams(bdelLp);
        reorderRow.addView(btnDelete);

        form.addView(reorderRow);

        AlertDialog editDialog = builder.setView(root).create();

        Runnable updatePreview = () -> {
            String lbl = etDisplay.getText().toString();
            if (TextUtils.isEmpty(lbl)) lbl = item.getVisualLabel();
            String pop = item.getVisualPopupLabel();
            if (!TextUtils.isEmpty(pop)) {
                previewBtn.setText(lbl + "\n↑" + pop);
            } else {
                previewBtn.setText(lbl);
            }
        };
        updatePreview.run();

        etDisplay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updatePreview.run(); }
        });

        btnPickAction.setOnClickListener(v -> {
            promptPickKey(context, (key, display) -> {
                item.key = key;
                item.macro = "";
                item.display = display;
                etDisplay.setText(display);
                tvActionDesc.setText(describeAction(item.key, item.macro));
                updatePreview.run();
            });
        });

        btnPickPopup.setOnClickListener(v -> {
            promptPickKey(context, (key, display) -> {
                item.popupKey = key;
                item.popupMacro = "";
                item.popupDisplay = display;
                tvPopupDesc.setText(describeAction(item.popupKey, item.popupMacro));
                updatePreview.run();
            });
        });

        btnClearPopup.setOnClickListener(v -> {
            item.popupKey = "";
            item.popupDisplay = "";
            item.popupMacro = "";
            tvPopupDesc.setText("Không có (None)");
            updatePreview.run();
        });

        btnMoveLeft.setOnClickListener(v -> {
            KeyItem prev = row.set(colIdx - 1, item);
            row.set(colIdx, prev);
            editDialog.dismiss();
            onUpdated.run();
        });

        btnMoveRight.setOnClickListener(v -> {
            KeyItem next = row.set(colIdx + 1, item);
            row.set(colIdx, next);
            editDialog.dismiss();
            onUpdated.run();
        });

        btnDelete.setOnClickListener(v -> {
            row.remove(colIdx);
            editDialog.dismiss();
            onUpdated.run();
        });

        Button btnDone = new Button(context);
        btnDone.setText("XONG");
        btnDone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnDone.setTypeface(Typeface.DEFAULT_BOLD);
        btnDone.setTextColor(0xFF0D1117);
        btnDone.setBackground(makeRipple(0xFF4FC3F7, 0x44000000, 0xFF81D4FA, dp(context, 1), dp(context, 8)));
        btnDone.setOnClickListener(v -> {
            String entered = etDisplay.getText().toString().trim();
            if (!entered.isEmpty()) {
                item.display = entered;
            }
            editDialog.dismiss();
            onUpdated.run();
        });
        root.addView(btnDone);

        editDialog.show();
    }

    private static String describeAction(String key, String macro) {
        if (!TextUtils.isEmpty(macro)) return "Macro: " + macro;
        if (TextUtils.isEmpty(key)) return "Không có (None)";
        switch (key) {
            case "KEYBOARD": return "Bật/tắt bàn phím ảo (KEYBOARD)";
            case "DRAWER": return "Mở menu ngăn kéo (DRAWER)";
            case "PASTE": return "Dán văn bản (PASTE)";
            case "SCROLL": return "Bật/tắt cuộn trang (SCROLL)";
            default: return "Phím: " + key;
        }
    }

    interface KeyPickCallback {
        void onSelected(String key, String display);
    }

    private static void promptPickKey(Context context, KeyPickCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_DARK);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        root.setBackgroundColor(0xFF141820);

        TextView title = new TextView(context);
        title.setText("CHỌN PHÍM / HÀNH ĐỘNG");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, dp(context, 8));
        root.addView(title);

        ScrollView scroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        AlertDialog dialog = builder.setView(root).create();

        // 1. Phím chức năng Terminal
        addCategoryHeader(context, content, "HÀNH ĐỘNG TERMINAL");
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"KEYBOARD", "DRAWER", "PASTE", "SCROLL"},
            new String[]{"⌨ Phím", "≡ Drawer", "📋 Paste", "↕ Scroll"}
        );

        // 2. Phím bổ trợ (Modifiers)
        addCategoryHeader(context, content, "PHÍM BỔ TRỢ (MODIFIERS)");
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"CTRL", "ALT", "SHIFT", "FN"},
            new String[]{"CTRL", "ALT", "SHIFT", "FN"}
        );

        // 3. Phím điều hướng & Soạn thảo
        addCategoryHeader(context, content, "ĐIỀU HƯỚNG & SOẠN THẢO");
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"ESC", "TAB", "ENTER", "BKSP", "DEL"},
            new String[]{"ESC", "TAB", "ENTER", "BKSP", "DEL"}
        );
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"UP", "DOWN", "LEFT", "RIGHT"},
            new String[]{"↑ UP", "↓ DOWN", "← LEFT", "→ RIGHT"}
        );
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"HOME", "END", "PGUP", "PGDN"},
            new String[]{"HOME", "END", "PGUP", "PGDN"}
        );

        // 4. Ký tự & Biểu tượng Code
        addCategoryHeader(context, content, "KÝ TỰ & BIỂU TƯỢNG");
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"/", "-", "_", "|", "~", "\\", "$", "@"},
            new String[]{"/", "-", "_", "|", "~", "\\", "$", "@"}
        );
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"=", "+", "*", "&", "%", "#", "!", "?"},
            new String[]{"=", "+", "*", "&", "%", "#", "!", "?"}
        );
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{":", ";", "\"", "'", "`", "^", "<", ">"},
            new String[]{":", ";", "\"", "'", "`", "^", "<", ">"}
        );
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"{", "}", "[", "]", "(", ")", ",", "."},
            new String[]{"{", "}", "[", "]", "(", ")", ",", "."}
        );

        // 5. Function Keys (F1 - F12)
        addCategoryHeader(context, content, "PHÍM CHỨC NĂNG (F1 - F12)");
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"F1", "F2", "F3", "F4", "F5", "F6"},
            new String[]{"F1", "F2", "F3", "F4", "F5", "F6"}
        );
        addKeyButtonsRow(context, content, dialog, callback,
            new String[]{"F7", "F8", "F9", "F10", "F11", "F12"},
            new String[]{"F7", "F8", "F9", "F10", "F11", "F12"}
        );

        // 6. Tùy chỉnh bất kỳ
        addCategoryHeader(context, content, "NHẬP TÙY CHỌN / LỆNH BẤT KỲ");
        LinearLayout customRow = new LinearLayout(context);
        customRow.setOrientation(LinearLayout.HORIZONTAL);
        customRow.setGravity(Gravity.CENTER_VERTICAL);
        customRow.setPadding(0, dp(context, 4), 0, dp(context, 10));

        final EditText etCustom = new EditText(context);
        etCustom.setHint("VD: clear, :wq, tmux, htop");
        etCustom.setHintTextColor(0xFF6B7280);
        etCustom.setTextColor(Color.WHITE);
        etCustom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etCustom.setBackground(makeShape(0xFF0F1218, 0x33FFFFFF, dp(context, 1), dp(context, 6)));
        etCustom.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        LinearLayout.LayoutParams customLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etCustom.setLayoutParams(customLp);
        customRow.addView(etCustom);

        Button btnAddCustom = new Button(context);
        btnAddCustom.setText("DÙNG");
        btnAddCustom.setTextColor(0xFF0D1117);
        btnAddCustom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAddCustom.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddCustom.setBackground(makeRipple(0xFF4FC3F7, 0x44000000, 0xFF81D4FA, dp(context, 1), dp(context, 6)));
        LinearLayout.LayoutParams btnCustomLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnCustomLp.leftMargin = dp(context, 6);
        btnAddCustom.setLayoutParams(btnCustomLp);
        btnAddCustom.setOnClickListener(v -> {
            String val = etCustom.getText().toString().trim();
            if (!val.isEmpty()) {
                callback.onSelected(val, val);
                dialog.dismiss();
            }
        });
        customRow.addView(btnAddCustom);
        content.addView(customRow);

        dialog.show();
    }

    private static void addCategoryHeader(Context context, LinearLayout parent, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(0xFF4FC3F7);
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
        row.setPadding(0, dp(context, 2), 0, dp(context, 6));

        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            final String d = (displays != null && i < displays.length) ? displays[i] : k;

            Button btn = new Button(context);
            btn.setText(d);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setTypeface(Typeface.DEFAULT_BOLD);
            btn.setBackground(makeRipple(0xFF1E2430, 0x55FFFFFF, 0x33FFFFFF, dp(context, 1), dp(context, 6)));
            btn.setMinHeight(dp(context, 40));
            btn.setMinWidth(dp(context, 42));
            btn.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
            btn.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dp(context, 2), 0, dp(context, 2), 0);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> {
                callback.onSelected(k, d);
                dialog.dismiss();
            });

            row.addView(btn);
        }

        hScroll.addView(row);
        parent.addView(hScroll);
    }
}
