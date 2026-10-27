package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Scan normalization and credential validation, independent of Android services. */
final class WifiNetworks {
    enum Security { OPEN, PSK, WEP, UNSUPPORTED }

    static final class Entry {
        final String ssid;
        final Security security;
        final int level;
        Entry(String ssid, String capabilities, int level) {
            this.ssid = ssid;
            this.security = security(capabilities);
            this.level = level;
        }
        String key() { return ssid + "\n" + security; }
    }

    static Security security(String capabilities) {
        String c = capabilities == null ? "" : capabilities.toUpperCase(Locale.ROOT);
        if (c.contains("EAP")) return Security.UNSUPPORTED;
        if (c.contains("PSK")) return Security.PSK;
        if (c.contains("WEP")) return Security.WEP;
        if (c.contains("SAE") || c.contains("OWE") || c.contains("WAPI") || c.contains("RSN"))
            return Security.UNSUPPORTED;
        return Security.OPEN;
    }

    static List<Entry> visible(List<Entry> scans, String connected) {
        Map<String, Entry> unique = new HashMap<>();
        for (Entry e : scans) {
            if (e.ssid == null || e.ssid.isEmpty()) continue;
            Entry old = unique.get(e.key());
            if (old == null || e.level > old.level) unique.put(e.key(), e);
        }
        List<Entry> rows = new ArrayList<>(unique.values());
        rows.sort((a, b) -> {
            int current = Boolean.compare(b.ssid.equals(connected), a.ssid.equals(connected));
            if (current != 0) return current;
            int signal = Integer.compare(b.level, a.level);
            return signal != 0 ? signal : a.key().compareToIgnoreCase(b.key());
        });
        return rows;
    }

    static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static boolean validPassword(Security security, String password) {
        if (security == Security.OPEN) return true;
        int n = password.length();
        if (security == Security.PSK) return (n >= 8 && n <= 63) || (n == 64 && hex(password));
        if (security == Security.WEP) return n == 5 || n == 13 || ((n == 10 || n == 26) && hex(password));
        return false;
    }

    static boolean hex(String value) { return value.matches("[0-9a-fA-F]+"); }
}
