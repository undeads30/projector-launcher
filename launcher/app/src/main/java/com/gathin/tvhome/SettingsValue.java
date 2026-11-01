package com.gathin.tvhome;

final class SettingsValue {
    // Cronus 908 MCU: writing N reads back N-1 for types 6, 26 and 27.
    // Verified by write/read/restore on the device; do not use for other types.
    static int cronusImageValue(int raw) {
        if (!valid(raw, -1, 99)) throw new IllegalArgumentException("Invalid firmware reading");
        return raw + 1;
    }
    static boolean valid(int value, int min, int max) {
        return min <= max && value >= min && value <= max;
    }
    static int step(int value, int delta, int min, int max) {
        if (!valid(value, min, max)) throw new IllegalArgumentException("Unavailable value");
        return (int) Math.max(min, Math.min((long) max, (long) value + delta));
    }
    static boolean confirmed(int requested, int actual, int min, int max) {
        return valid(actual, min, max) && requested == actual;
    }
}
