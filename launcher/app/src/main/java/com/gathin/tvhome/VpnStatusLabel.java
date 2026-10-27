package com.gathin.tvhome;

import java.util.Locale;
import java.util.regex.Pattern;

final class VpnStatusLabel {
    private static final Pattern HK = Pattern.compile("(?<![a-z])hk(?![a-z])");
    private static final Pattern RU = Pattern.compile("(?<![a-z])ru(?![a-z])|russian|россия");

    static String profileCode(String title) {
        if (title == null) return "";
        String value = title.toLowerCase(Locale.ROOT);
        boolean hk = value.contains("🇭🇰") || HK.matcher(value).find();
        boolean ru = value.contains("🇷🇺") || RU.matcher(value).find();
        if (hk == ru) return "";
        return hk ? "HK" : "RU";
    }

    static String text(boolean connected, String code) {
        return connected && ("HK".equals(code) || "RU".equals(code)) ? "VPN: " + code : "VPN";
    }
}
