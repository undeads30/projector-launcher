package com.gathin.tvhome;

import android.app.Notification;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/** Reads only the active v2rayNG profile title; stores only its HK/RU label. */
public final class VpnStatusService extends NotificationListenerService {
    private static boolean isVpnNotification(StatusBarNotification notification) {
        return notification != null && "com.v2ray.ang".equals(notification.getPackageName())
                && notification.getId() == 1;
    }

    @Override public void onListenerConnected() {
        String code = "";
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            if (notifications != null) {
                for (StatusBarNotification notification : notifications) {
                    if (isVpnNotification(notification)) code = profileCode(notification);
                }
            }
        } catch (SecurityException ignored) { }
        saveCode(code);
        Log.i("VpnStatus", "Profile status listener ready");
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (isVpnNotification(notification)) saveCode(profileCode(notification));
    }

    @Override public void onNotificationRemoved(StatusBarNotification notification) {
        if (isVpnNotification(notification)) saveCode("");
    }

    @Override public void onListenerDisconnected() { saveCode(""); }

    private String profileCode(StatusBarNotification notification) {
        Notification content = notification.getNotification();
        if ((content.flags & Notification.FLAG_ONGOING_EVENT) == 0) return "";
        CharSequence title = content.extras.getCharSequence(Notification.EXTRA_TITLE);
        return VpnStatusLabel.profileCode(title == null ? null : title.toString());
    }

    private void saveCode(String code) {
        SharedPreferences preferences = getSharedPreferences("vpn_status", MODE_PRIVATE);
        if (!code.equals(preferences.getString("profile_code", ""))) {
            preferences.edit().putString("profile_code", code).apply();
            Log.i("VpnStatus", "Active profile label: " + (code.isEmpty() ? "unknown/off" : code));
        }
    }
}
