package com.gathin.tvhome;

final class VpnMenuPolicy {
    static String profileKey(String choice) {
        if ("HK".equals(choice)) return "profile";
        if ("RU".equals(choice)) return "vk_profile";
        return null;
    }
}
