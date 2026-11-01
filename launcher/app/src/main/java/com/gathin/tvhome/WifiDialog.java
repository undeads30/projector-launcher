package com.gathin.tvhome;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** A bounded, remote-friendly Wi-Fi picker over the home screen. */
final class WifiDialog {
    private final Activity activity;
    private final WifiManager wifi;
    private final Dialog dialog;
    private final LinearLayout panel;
    private final LinearLayout rows;
    private final TextView message;
    private final TextView refresh;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Dialog passwordDialog;
    private Dialog networkMenu;
    private boolean registered;
    private String connecting;
    private String failedSsid;
    private String notice;
    private final Runnable timeout = () -> {
        failedSsid = connecting;
        connecting = null;
        notice = activityText(R.string.wifi_connect_failed);
        render();
    };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())
                    && intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING
                    && connecting != null) {
                failedSsid = connecting;
                connecting = null;
                handler.removeCallbacks(timeout);
                notice = activityText(R.string.wifi_auth_failed);
            }
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    && wifi.isWifiEnabled()) scan();
            render();
        }
    };

    WifiDialog(Activity activity) {
        this(activity, activity.findViewById(R.id.btnWifi));
    }

    WifiDialog(Activity activity, View returnFocus) {
        this.activity = activity;
        wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(12), dp(16), dp(12));
        panel.setBackgroundResource(R.drawable.glass_panel);
        TextView title = text(activityText(R.string.wifi), 22);
        panel.addView(title);
        message = text("", 13);
        message.setPadding(0, dp(6), 0, dp(8));
        panel.addView(message);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        // Weight constrains the list to the window, leaving the controls visible.
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout controls = new LinearLayout(activity);
        refresh = button(activityText(R.string.wifi_refresh), this::scan);
        controls.addView(refresh, new LinearLayout.LayoutParams(0, dp(42), 1));
        controls.addView(button(activityText(R.string.wifi_close), this::dismiss),
                new LinearLayout.LayoutParams(0, dp(42), 1));
        panel.addView(controls);
        dialog.setContentView(panel);
        dialog.setOnDismissListener(d -> {
            handler.removeCallbacksAndMessages(null);
            if (registered) {
                activity.unregisterReceiver(receiver);
                registered = false;
            }
            if (passwordDialog != null) passwordDialog.dismiss();
            if (networkMenu != null) networkMenu.dismiss();
            if (returnFocus != null) returnFocus.post(() -> {
                if (returnFocus.isAttachedToWindow()) returnFocus.requestFocus();
            });
        });
    }

    void show() {
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.35f);
            window.setLayout(Math.min(dp(480), activity.getResources().getDisplayMetrics().widthPixels * 4 / 5),
                    activity.getResources().getDisplayMetrics().heightPixels * 3 / 4);
            window.setGravity(Gravity.CENTER);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        else activity.registerReceiver(receiver, filter);
        registered = true;
        render();
        scan();
        if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        else refresh.requestFocus();
    }

    void dismiss() { dialog.dismiss(); }

    private boolean ready() {
        if (wifi == null) { message.setText(R.string.wifi_unavailable); return false; }
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            message.setText(R.string.wifi_permission); return false;
        }
        if (!wifi.isWifiEnabled()) {
            message.setText(R.string.wifi_disabled); return false;
        }
        return true;
    }

    private void scan() {
        notice = null;
        try {
            if (wifi != null && !wifi.isWifiEnabled()) {
                message.setText(wifi.setWifiEnabled(true) ? R.string.wifi_enabling : R.string.wifi_enable_failed);
                return;
            }
            if (!ready()) return;
            boolean started = wifi.startScan();
            render();
            if (connecting == null && (started || rows.getChildCount() > 0))
                message.setText(started ? R.string.wifi_scanning : R.string.wifi_scan_cached);
        } catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private String currentSsid() {
        WifiInfo info = wifi.getConnectionInfo();
        if (info == null || info.getNetworkId() < 0 || info.getSupplicantState() != SupplicantState.COMPLETED) return "";
        String ssid = info.getSSID();
        return ssid != null && ssid.length() >= 2 && ssid.startsWith("\"")
                ? ssid.substring(1, ssid.length() - 1) : "";
    }

    private void render() {
        if (!dialog.isShowing()) return;
        View focused = rows.findFocus();
        Object focusKey = focused == null ? null : focused.getTag();
        boolean hadFocus = focused != null;
        rows.removeAllViews();
        try {
            if (!ready()) return;
            String current = currentSsid();
            if (connecting != null && connecting.equals(current)) {
                failedSsid = null;
                connecting = null;
                handler.removeCallbacks(timeout);
                notice = null;
            }
            List<WifiNetworks.Entry> scans = new ArrayList<>();
            for (ScanResult scan : wifi.getScanResults())
                scans.add(new WifiNetworks.Entry(scan.SSID, scan.capabilities, scan.level));
            List<WifiNetworks.Entry> networks = WifiNetworks.visible(scans, current);
            View restore = null;
            for (WifiNetworks.Entry entry : networks) {
                View row = activity.getLayoutInflater().inflate(R.layout.item_input, rows, false);
                row.setTag(entry.key());
                ((ImageView) row.findViewById(R.id.icon)).setImageResource(R.drawable.ic_wifi);
                ((TextView) row.findViewById(R.id.title)).setText(entry.ssid);
                boolean connected = entry.ssid.equals(current);
                row.findViewById(R.id.dot).setVisibility(connected ? View.VISIBLE : View.GONE);
                String status = connected ? activityText(R.string.connected)
                        : entry.ssid.equals(connecting) ? activityText(R.string.wifi_connecting)
                        : (entry.security == WifiNetworks.Security.OPEN ? activityText(R.string.wifi_open) : "🔒")
                        + "  " + (WifiManager.calculateSignalLevel(entry.level, 4) + 1) + "/4";
                ((TextView) row.findViewById(R.id.status)).setText(status);
                row.setOnClickListener(v -> choose(entry));
                row.setOnKeyListener((v, code, event) -> {
                    if (code != KeyEvent.KEYCODE_MENU) return false;
                    if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0)
                        showNetworkMenu(entry);
                    return true;
                });
                rows.addView(row);
                if (entry.key().equals(focusKey)) restore = row;
            }
            message.setText(notice != null ? notice : connecting != null
                    ? activity.getString(R.string.wifi_connecting_to, connecting)
                    : networks.isEmpty() ? activityText(R.string.wifi_empty) : activityText(R.string.wifi_choose));
            if (restore != null) restore.requestFocus();
            else if (hadFocus) refresh.requestFocus();
        } catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private List<WifiConfiguration> savedNetworks(WifiNetworks.Entry entry) {
        List<WifiConfiguration> result = new ArrayList<>();
        List<WifiConfiguration> saved = wifi.getConfiguredNetworks();
        if (saved != null) for (WifiConfiguration config : saved) {
            if (!WifiNetworks.quote(entry.ssid).equals(config.SSID)) continue;
            boolean match = entry.security == WifiNetworks.Security.PSK
                    ? config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)
                    : entry.security == WifiNetworks.Security.WEP ? config.wepKeys[0] != null
                    : entry.security == WifiNetworks.Security.OPEN
                    ? config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && config.wepKeys[0] == null
                    : config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)
                        || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X);
            if (match) result.add(config);
        }
        return result;
    }

    private void showNetworkMenu(WifiNetworks.Entry entry) {
        try {
            boolean saved = !savedNetworks(entry).isEmpty();
            if (networkMenu != null) networkMenu.dismiss();
            Dialog menu = new Dialog(activity);
            menu.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View content = activity.getLayoutInflater().inflate(R.layout.popup_app_menu, panel, false);
            ((TextView) content.findViewById(R.id.appMenuTitle)).setText(entry.ssid);
            LinearLayout actions = content.findViewById(R.id.rows);
            boolean connected = entry.ssid.equals(currentSsid());
            int connectedId = connected ? wifi.getConnectionInfo().getNetworkId() : -1;
            addMenuAction(actions, menu, R.drawable.ic_wifi,
                    connected ? R.string.wifi_disconnect_action : R.string.wifi_connect_action,
                    connected ? () -> disconnect(entry, connectedId) : () -> choose(entry));
            if (saved) addMenuAction(actions, menu, R.drawable.ic_app_uninstall,
                    R.string.wifi_forget, () -> forget(entry));
            menu.setContentView(content);
            menu.setOnKeyListener((d, code, event) -> {
                if (code != KeyEvent.KEYCODE_MENU) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) menu.dismiss();
                return true;
            });
            menu.setOnDismissListener(d -> {
                networkMenu = null;
                if (dialog.isShowing()) for (int i = 0; i < rows.getChildCount(); i++) {
                    View row = rows.getChildAt(i);
                    if (entry.key().equals(row.getTag())) { row.requestFocus(); break; }
                }
            });
            networkMenu = menu;
            menu.show();
            if (menu.getWindow() != null) {
                menu.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                menu.getWindow().setDimAmount(0.15f);
                menu.getWindow().setLayout(Math.min(dp(280),
                        activity.getResources().getDisplayMetrics().widthPixels * 4 / 5), -2);
            }
            actions.getChildAt(0).requestFocus();
        } catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private void addMenuAction(LinearLayout actions, Dialog menu, int icon, int label, Runnable action) {
        View row = activity.getLayoutInflater().inflate(R.layout.item_input, actions, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(label);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        row.findViewById(R.id.status).setVisibility(View.GONE);
        row.setOnClickListener(v -> { menu.dismiss(); action.run(); });
        actions.addView(row);
    }

    private void disconnect(WifiNetworks.Entry entry, int networkId) {
        try {
            if (Build.VERSION.SDK_INT >= 29) { message.setText(R.string.wifi_android_restricted); return; }
            // A stale menu must never disconnect a different, newly connected network.
            WifiInfo current = wifi.getConnectionInfo();
            if (current == null || current.getNetworkId() != networkId
                    || !entry.ssid.equals(currentSsid())) { render(); return; }
            // Disable this saved profile until explicitly connected again; keep its credentials.
            boolean disabled = wifi.disableNetwork(networkId);
            if (disabled && entry.ssid.equals(connecting)) {
                connecting = null;
                handler.removeCallbacks(timeout);
            }
            notice = activityText(disabled ? R.string.wifi_disconnected : R.string.wifi_disconnect_failed);
            render();
        } catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private void forget(WifiNetworks.Entry entry) {
        try {
            if (Build.VERSION.SDK_INT >= 29) { message.setText(R.string.wifi_android_restricted); return; }
            boolean removed = true;
            for (WifiConfiguration config : savedNetworks(entry))
                removed = wifi.removeNetwork(config.networkId) && removed;
            if (entry.ssid.equals(connecting)) {
                connecting = null;
                handler.removeCallbacks(timeout);
            }
            if (entry.ssid.equals(failedSsid)) failedSsid = null;
            notice = activityText(removed ? R.string.wifi_forgotten : R.string.wifi_forget_failed);
            render();
        } catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private void choose(WifiNetworks.Entry entry) {
        try {
            if (entry.ssid.equals(currentSsid())) { message.setText(R.string.wifi_already_connected); return; }
            if (Build.VERSION.SDK_INT >= 29) { message.setText(R.string.wifi_android_restricted); return; }
            if (entry.security == WifiNetworks.Security.UNSUPPORTED) {
                message.setText(R.string.wifi_security_unsupported); return;
            }
            List<WifiConfiguration> saved = wifi.getConfiguredNetworks();
            if (saved != null) for (WifiConfiguration config : saved) {
                if (!WifiNetworks.quote(entry.ssid).equals(config.SSID)) continue;
                boolean match = entry.security == WifiNetworks.Security.PSK
                        ? config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)
                        : entry.security == WifiNetworks.Security.WEP ? config.wepKeys[0] != null
                        : config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && config.wepKeys[0] == null;
                if (match && !entry.ssid.equals(failedSsid)) { connect(config.networkId, entry.ssid); return; }
            }
            if (entry.security == WifiNetworks.Security.OPEN) addNetwork(entry, "");
            else askPassword(entry);
        } catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private void askPassword(WifiNetworks.Entry entry) {
        Dialog prompt = new Dialog(activity);
        prompt.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(16), dp(20), dp(16));
        content.setBackgroundResource(R.drawable.glass_panel);
        content.addView(text(entry.ssid, 20));
        EditText password = new EditText(activity);
        password.setSingleLine(true);
        password.setTextColor(Color.WHITE);
        password.setHintTextColor(0xFFBBBBBB);
        password.setHint(R.string.wifi_password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
                | android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        content.addView(password, new LinearLayout.LayoutParams(-1, dp(52)));
        Runnable submit = () -> {
            String value = password.getText().toString();
            if (!WifiNetworks.validPassword(entry.security, value)) {
                password.setError(activityText(R.string.wifi_password_invalid)); return;
            }
            addNetwork(entry, value);
            password.setText("");
            prompt.dismiss();
        };
        content.addView(button(activityText(R.string.wifi_connect), submit));
        content.addView(button(activityText(R.string.wifi_cancel), prompt::dismiss));
        password.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
            submit.run(); return true;
        });
        prompt.setContentView(content);
        prompt.setOnDismissListener(d -> { password.setText(""); passwordDialog = null; });
        passwordDialog = prompt;
        prompt.show();
        if (prompt.getWindow() != null) {
            prompt.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            prompt.getWindow().setLayout(Math.min(dp(440), activity.getResources().getDisplayMetrics().widthPixels * 4 / 5), -2);
        }
        password.requestFocus();
    }

    private void addNetwork(WifiNetworks.Entry entry, String password) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WifiNetworks.quote(entry.ssid);
        if (entry.security == WifiNetworks.Security.PSK) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.preSharedKey = password.length() == 64 && WifiNetworks.hex(password) ? password : WifiNetworks.quote(password);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            if (entry.security == WifiNetworks.Security.WEP) {
                config.wepKeys[0] = (password.length() == 10 || password.length() == 26) && WifiNetworks.hex(password)
                        ? password : WifiNetworks.quote(password);
                config.wepTxKeyIndex = 0;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            }
        }
        try { connect(wifi.addNetwork(config), entry.ssid); }
        catch (SecurityException e) { message.setText(R.string.wifi_permission); }
    }

    private void connect(int id, String ssid) {
        if (id < 0 || !wifi.enableNetwork(id, true)) { message.setText(R.string.wifi_connect_failed); return; }
        connecting = ssid;
        notice = null;
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, 30_000);
        render();
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private String activityText(int res) { return activity.getString(res); }
    private TextView text(String label, int size) {
        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextSize(size);
        text.setTextColor(Color.WHITE);
        return text;
    }
    private TextView button(String label, Runnable action) {
        TextView button = text(label, 15);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setTextColor(activity.getColorStateList(R.drawable.pill_text));
        button.setBackgroundResource(R.drawable.input_row_bg);
        button.setFocusable(true);
        button.setOnClickListener(v -> action.run());
        return button;
    }
}
