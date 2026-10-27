package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/** Only Film.mov window notifications; no window contents, key filtering, or polling. */
public class FilmVpnService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence name = event.getPackageName();
        String pkg = name == null ? null : name.toString();
        if (!"mov.film".equals(pkg)) return;
        if (VpnLaunchPolicy.shouldStopForWindow(pkg, YoutubeVpnLaunch.connected(this))) {
            try {
                YoutubeVpnLaunch.stop(this);
                Log.i("FilmVpn", "Film.mov opened: requested VPN stop");
            } catch (SecurityException e) {
                Log.w("FilmVpn", "VPN stop was denied", e);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
