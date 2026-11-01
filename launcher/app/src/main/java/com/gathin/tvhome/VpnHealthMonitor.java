package com.gathin.tvhome;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Home-only monitor: one bounded probe at a time, repeated 15 seconds after completion. */
final class VpnHealthMonitor {
    private final Context context;
    private final Runnable changed;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final VpnHealthState state = new VpnHealthState();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean active, busy;
    private long nextProbe;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            refresh();
            main.postDelayed(this, 1000);
        }
    };
    VpnHealthMonitor(Context context, Runnable changed) {
        this.context = context.getApplicationContext();
        this.changed = changed;
    }
    void start() { active = true; main.post(tick); }
    void stop() {
        active = false;
        state.select("");
        main.removeCallbacks(tick);
        worker.shutdownNow();
    }
    VpnHealthState.Status status() { return state.status(); }
    private String identity() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return network.toString() + "/" + context.getSharedPreferences("vpn_status", Context.MODE_PRIVATE)
                        .getString("profile_code", "");
            }
        }
        return "";
    }
    void refresh() {
        if (!active) return;
        long previous = state.generation();
        state.select(identity());
        if (previous != state.generation()) { nextProbe = 0; changed.run(); }
        if (state.status() == VpnHealthState.Status.OFF || busy || SystemClock.elapsedRealtime() < nextProbe) return;
        busy = true;
        final long version = state.generation();
        worker.execute(() -> {
            boolean success = VpnHealthProbe.reachable();
            main.post(() -> {
                busy = false;
                if (!active) return;
                state.select(identity());
                state.complete(version, success);
                nextProbe = version == state.generation() ? SystemClock.elapsedRealtime() + 15000 : 0;
                Log.i("VpnHealth", "VPN connectivity: " + state.status());
                changed.run();
            });
        });
    }
}
