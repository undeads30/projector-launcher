package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VpnStatusLabelTest {
    @Test public void identifiesHongKongNotification() {
        assertEquals("HK", VpnStatusLabel.profileCode("🇭🇰 HK| Sample server"));
        assertEquals("HK", VpnStatusLabel.profileCode("hk - Server"));
        assertEquals("HK", VpnStatusLabel.profileCode("🇭🇰 Server"));
    }
    @Test public void identifiesRussianNotification() {
        assertEquals("RU", VpnStatusLabel.profileCode("RU | Server"));
        assertEquals("RU", VpnStatusLabel.profileCode("russian-poector"));
        assertEquals("RU", VpnStatusLabel.profileCode("🇷🇺 Server"));
    }
    @Test public void doesNotInventLocationForUnknownOrAmbiguousNames() {
        assertEquals("", VpnStatusLabel.profileCode(null));
        assertEquals("", VpnStatusLabel.profileCode(""));
        assertEquals("", VpnStatusLabel.profileCode("TRUST Frankfurt"));
        assertEquals("", VpnStatusLabel.profileCode("HK / RU"));
    }
    @Test public void disconnectedStateNeverShowsPreviousProfile() {
        assertEquals("VPN", VpnStatusLabel.text(false, "HK"));
        assertEquals("VPN", VpnStatusLabel.text(false, "RU"));
    }
    @Test public void connectedStateShowsOnlySupportedProfileCodes() {
        assertEquals("VPN: HK", VpnStatusLabel.text(true, "HK"));
        assertEquals("VPN: RU", VpnStatusLabel.text(true, "RU"));
        assertEquals("VPN", VpnStatusLabel.text(true, null));
        assertEquals("VPN", VpnStatusLabel.text(true, "Other"));
    }
}
