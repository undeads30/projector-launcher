package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The same glass app menu on home tiles and folder tiles. */
final class AppTileMenu {
    private final Dialog dialog;

    private AppTileMenu(Dialog dialog) { this.dialog = dialog; }

    void dismiss() { dialog.dismiss(); }

    static AppTileMenu show(Activity activity, ViewGroup host, View anchor, String label,
                            int transferLabel, Runnable open, Runnable move, Runnable transfer,
                            Runnable uninstall, Runnable onDismiss) {
        return show(activity, host, anchor, label, transferLabel, open, move, transfer,
                uninstall, onDismiss, false);
    }

    static AppTileMenu show(Activity activity, ViewGroup host, View anchor, String label,
                            int transferLabel, Runnable open, Runnable move, Runnable transfer,
                            Runnable uninstall, Runnable onDismiss, boolean startAtCenter) {
        float density = activity.getResources().getDisplayMetrics().density;
        View panel = activity.getLayoutInflater().inflate(R.layout.popup_app_menu, host, false);
        ((TextView) panel.findViewById(R.id.appMenuTitle)).setText(label);
        LinearLayout rows = panel.findViewById(R.id.rows);
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        AppTileMenu menu = new AppTileMenu(dialog);
        FrameLayout overlay = new FrameLayout(activity);
        overlay.setOnClickListener(v -> menu.dismiss());
        overlay.setFocusable(false);
        panel.setClickable(true);
        panel.setFocusable(false);
        dialog.setContentView(overlay);
        dialog.setOnDismissListener(d -> {
            if (anchor.isAttachedToWindow()) anchor.requestFocus();
            onDismiss.run();
        });
        addRow(activity, rows, menu, R.drawable.ic_app_open, R.string.app_open, open);
        addRow(activity, rows, menu, R.drawable.ic_app_move, R.string.app_move, move);
        if (transfer != null) addRow(activity, rows, menu, R.drawable.ic_folder, transferLabel, transfer);
        if (uninstall != null) addRow(activity, rows, menu, R.drawable.ic_app_uninstall, R.string.app_uninstall, uninstall);
        long openedAt = SystemClock.uptimeMillis();
        for (int index = 0; index < rows.getChildCount(); index++) {
            rows.getChildAt(index).setOnKeyListener((v, code, event) -> {
                if (code != KeyEvent.KEYCODE_MENU) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0
                        && event.getDownTime() > openedAt) menu.dismiss();
                return true;
            });
        }
        Rect visible = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
        int gap = Math.round(10 * density);
        int width = Math.min(Math.round(240 * density),
                Math.round((visible.width() - 2 * gap) / AppMenuPlacement.SCALE));
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = panel.getMeasuredHeight();
        int visualWidth = AppMenuPlacement.scaled(width);
        int visualHeight = AppMenuPlacement.scaled(height);
        int[] tile = new int[2];
        anchor.getLocationOnScreen(tile);
        int x = Math.max(visible.left + gap, Math.min(tile[0], visible.right - visualWidth - gap));
        int y = AppMenuPlacement.top(tile[1], Math.round(anchor.getHeight() * anchor.getScaleY()),
                visualHeight, visible.top, visible.bottom, gap, startAtCenter);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        lp.leftMargin = x;
        lp.topMargin = y;
        overlay.addView(panel, lp);
        panel.setAlpha(0f);
        panel.setPivotX(0f);
        panel.setPivotY(0f);
        panel.setScaleX(AppMenuPlacement.SCALE);
        panel.setScaleY(0.92f * AppMenuPlacement.SCALE);
        panel.setTranslationY((y < tile[1] ? 8 : -8) * density);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0f);
            window.setGravity(Gravity.TOP | Gravity.LEFT);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setWindowAnimations(0);
        }
        panel.animate().alpha(1f).scaleY(AppMenuPlacement.SCALE).translationY(0f).setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
        rows.getChildAt(0).requestFocus();
        return menu;
    }

    private static void addRow(Activity activity, LinearLayout rows, AppTileMenu menu,
                               int icon, int title, Runnable action) {
        View row = activity.getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            menu.dismiss();
            action.run();
        });
        rows.addView(row);
    }
}
