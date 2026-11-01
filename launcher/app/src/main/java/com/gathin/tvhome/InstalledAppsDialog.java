package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** A folder of installed utilities with standard TV focus, OK and Back navigation. */
final class InstalledAppsDialog {
    private final Activity activity;
    private final Dialog dialog;
    private View firstRow;
    private Dialog details;
    private int tileCount;
    private final List<String> packageOrder = new ArrayList<>();
    private final Map<String, View> tilesByPackage = new HashMap<>();
    private GridLayout tiles;
    private TextView hint;
    private AppTileMenu appMenu;
    private String movingPackage;
    private List<String> orderBeforeMove;

    InstalledAppsDialog(Activity activity, List<String> packages, Runnable onClose,
                        java.util.function.Consumer<String> moveToMain) {
        this.activity = activity;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(12));
        panel.setBackgroundResource(R.drawable.glass_panel);
        TextView title = text(activity.getString(R.string.our_apps_folder), 23, Color.WHITE);
        title.setPadding(dp(10), 0, dp(10), dp(12));
        panel.addView(title);
        ScrollView scroll = new ScrollView(activity);
        GridLayout rows = new GridLayout(activity);
        tiles = rows;
        rows.setColumnCount(3);
        rows.setClipChildren(false);
        rows.setPadding(dp(4), dp(4), dp(4), dp(4));
        rows.addOnLayoutChangeListener((v, left, top, right, bottom, ol, ot, or, ob) -> {
            int side = Math.max(1, (right - left - rows.getPaddingLeft() - rows.getPaddingRight()) / 3 - dp(12));
            for (int index = 0; index < rows.getChildCount(); index++) {
                View tile = rows.getChildAt(index);
                if (!tile.isFocusable()) continue;
                GridLayout.LayoutParams lp = (GridLayout.LayoutParams) tile.getLayoutParams();
                if (lp.width != side || lp.height != side) {
                    lp.width = side;
                    lp.height = side;
                    tile.setLayoutParams(lp);
                }
            }
        });
        scroll.addView(rows);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        PackageManager pm = activity.getPackageManager();
        for (String pkg : packages) {
            try {
                ApplicationInfo app = pm.getApplicationInfo(pkg, 0);
                String label = label(pkg, String.valueOf(pm.getApplicationLabel(app)));
                View row = activity.getLayoutInflater().inflate(R.layout.item_folder_app, rows, false);
                GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
                tileParams.width = dp(120);
                tileParams.height = dp(120);
                tileParams.setMargins(dp(6), dp(6), dp(6), dp(6));
                row.setLayoutParams(tileParams);
                row.setContentDescription(label);
                TextView rowTitle = row.findViewById(R.id.title);
                rowTitle.setText(label);
                rowTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
                ImageView icon = row.findViewById(R.id.icon);
                icon.setImageTintList(null);
                icon.setImageDrawable(pm.getApplicationIcon(app));
                row.setOnFocusChangeListener((tile, focused) -> tile.animate()
                        .scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f)
                        .setDuration(150).start());
                row.setOnClickListener(v -> {
                    if (movingPackage != null) finishMove(true);
                    else details = launch(activity, pkg, label);
                });
                row.setOnKeyListener((v, code, event) -> {
                    if (code != KeyEvent.KEYCODE_MENU) return false;
                    if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                        if (appMenu != null || movingPackage != null) return true;
                        appMenu = AppTileMenu.show(activity, panel, row, label,
                                R.string.folder_move_to_main, () -> details = launch(activity, pkg, label),
                                () -> beginMove(pkg), () -> moveToMain.accept(pkg), () -> {
                                    try {
                                        activity.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
                                    } catch (android.content.ActivityNotFoundException | SecurityException e) {
                                        Toast.makeText(activity, R.string.uninstall_unavailable, Toast.LENGTH_LONG).show();
                                    }
                                }, () -> appMenu = null, true);
                    }
                    return true;
                });
                rows.addView(row);
                packageOrder.add(pkg);
                tilesByPackage.put(pkg, row);
                tileCount++;
                if (firstRow == null) firstRow = row;
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        if (firstRow == null) {
            GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
            emptyParams.columnSpec = GridLayout.spec(0, 3);
            emptyParams.width = dp(360);
            rows.addView(text(activity.getString(R.string.folder_empty), 16, Color.WHITE), emptyParams);
        }
        hint = text(activity.getString(R.string.folder_hint), 12, 0xB3FFFFFF);
        hint.setPadding(dp(10), dp(12), dp(10), 0);
        panel.addView(hint);
        dialog.setContentView(panel);
        dialog.setOnKeyListener((d, code, event) -> {
            if (movingPackage == null) return false;
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == KeyEvent.ACTION_UP) finishMove(code != KeyEvent.KEYCODE_BACK);
                return true;
            }
            int dx = code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
            int dy = code == KeyEvent.KEYCODE_DPAD_UP ? -1 : code == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : 0;
            if (dx != 0 || dy != 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int from = packageOrder.indexOf(movingPackage);
                    int to = TileOrder.destination(from, packageOrder.size(), 3, dx, dy);
                    if (TileOrder.move(packageOrder, from, to)) reorderTiles(movingPackage);
                }
                return true;
            }
            return code == KeyEvent.KEYCODE_MENU;
        });
        dialog.setOnDismissListener(d -> {
            if (appMenu != null) appMenu.dismiss();
            if (movingPackage != null) finishMove(false);
            if (details != null) details.dismiss();
            onClose.run();
        });
    }

    void show() {
        dialog.show();
        resizeWindow();
        if (firstRow != null) firstRow.requestFocus();
    }

    private void resizeWindow() {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
            window.setGravity(Gravity.CENTER);
            int width = Math.min(dp(440), activity.getResources().getDisplayMetrics().widthPixels * 4 / 5);
            int cell = (width - dp(40)) / 3;
            int height = dp(112) + Math.max(1, (tileCount + 2) / 3) * cell
                    + (movingPackage == null ? 0 : dp(32));
            window.setLayout(width, Math.min(height, Math.min(dp(400),
                    activity.getResources().getDisplayMetrics().heightPixels * 4 / 5)));
        }
    }

    private void beginMove(String pkg) {
        movingPackage = pkg;
        orderBeforeMove = new ArrayList<>(packageOrder);
        TextView title = tilesByPackage.get(pkg).findViewById(R.id.title);
        hint.setText(activity.getString(R.string.app_move_hint, title.getText()));
        resizeWindow();
        tilesByPackage.get(pkg).requestFocus();
    }

    private void finishMove(boolean save) {
        String pkg = movingPackage;
        if (pkg == null) return;
        if (save && !activity.getSharedPreferences("app_folder", Activity.MODE_PRIVATE).edit()
                .putString("order", android.text.TextUtils.join("\n", packageOrder)).commit()) {
            save = false;
            Toast.makeText(activity, R.string.app_order_not_saved, Toast.LENGTH_LONG).show();
        }
        if (!save) {
            packageOrder.clear();
            packageOrder.addAll(orderBeforeMove);
        }
        movingPackage = null;
        orderBeforeMove = null;
        hint.setText(R.string.folder_hint);
        reorderTiles(pkg);
        resizeWindow();
    }

    private void reorderTiles(String focusedPackage) {
        tiles.removeAllViews();
        for (String pkg : packageOrder) {
            View tile = tilesByPackage.get(pkg);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams((GridLayout.LayoutParams) tile.getLayoutParams());
            lp.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED);
            tiles.addView(tile, lp);
        }
        tiles.post(() -> {
            View focused = tilesByPackage.get(focusedPackage);
            if (focused != null && focused.isAttachedToWindow()) focused.requestFocus();
        });
    }

    void dismiss() { dialog.dismiss(); }

    private static Intent intent(Activity activity, String pkg, PackageManager pm) {
        if ("ru.projector.digitalclock".equals(pkg)) {
            Intent settings = new Intent(Settings.ACTION_DREAM_SETTINGS);
            if (pm.resolveActivity(settings, 0) != null) return settings;
            Intent projectorSettings = new Intent().setComponent(new ComponentName(
                    "com.android.settings", "com.asu.settings.TvScreenSaverActivity"));
            if (pm.resolveActivity(projectorSettings, 0) != null) return projectorSettings;
        }
        Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
        if (launch != null) return launch;
        Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        return pm.resolveActivity(settings, 0) != null ? settings : null;
    }

    static Dialog launch(Activity activity, String pkg, String label) {
        Intent intent = intent(activity, pkg, activity.getPackageManager());
        if (intent != null) {
            try {
                if (VpnLaunchPolicy.requiresVpn(pkg)) YoutubeVpnLaunch.open(activity, intent);
                else activity.startActivity(intent);
            } catch (android.content.ActivityNotFoundException | SecurityException e) {
                Toast.makeText(activity, activity.getString(R.string.cant_open, label), Toast.LENGTH_SHORT).show();
            }
            return null;
        }
        String services = Settings.Secure.getString(activity.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean enabled = false;
        if (services != null) {
            for (String service : services.split(":")) {
                ComponentName component = ComponentName.unflattenFromString(service);
                if (component != null && pkg.equals(component.getPackageName())) enabled = true;
            }
        }
        Dialog details = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(label)
                .setMessage(activity.getString(enabled ? R.string.folder_helper_enabled : R.string.folder_helper_disabled))
                .setPositiveButton(android.R.string.ok, (d, which) -> { })
                .create();
        details.show();
        return details;
    }

    private String label(String pkg, String fallback) {
        if ("me.zhanghai.android.files".equals(pkg)) return activity.getString(R.string.folder_files);
        if ("org.liskovsoft.androidtv.rukeyboard".equals(pkg)) return activity.getString(R.string.folder_keyboard);
        if ("ru.projector.digitalclock".equals(pkg)) return activity.getString(R.string.folder_screensaver);
        if ("ru.projector.hdmiautoclose".equals(pkg)) return activity.getString(R.string.folder_hdmi);
        return fallback;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        return view;
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
