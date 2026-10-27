package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class VpnMenuPolicyTest {
    @Test public void hongKongUsesExistingYoutubeProfile() {
        assertEquals("profile", VpnMenuPolicy.profileKey("HK"));
    }
    @Test public void russiaUsesExistingVkProfile() {
        assertEquals("vk_profile", VpnMenuPolicy.profileKey("RU"));
    }
    @Test public void offAndUnknownChoicesNeverSelectAProfile() {
        assertNull(VpnMenuPolicy.profileKey("OFF"));
        assertNull(VpnMenuPolicy.profileKey("Other"));
        assertNull(VpnMenuPolicy.profileKey(null));
    }
}
