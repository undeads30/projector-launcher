package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class WifiNetworksTest {
    @Test public void mergesAccessPointsAndPutsConnectedNetworkFirst() {
        List<WifiNetworks.Entry> rows = WifiNetworks.visible(Arrays.asList(
                new WifiNetworks.Entry("Home", "[WPA2-PSK-CCMP][ESS]", -80),
                new WifiNetworks.Entry("Cafe", "[ESS]", -30),
                new WifiNetworks.Entry("Home", "[WPA2-PSK-CCMP][ESS]", -50),
                new WifiNetworks.Entry("", "[ESS]", -20)), "Home");
        assertEquals(2, rows.size());
        assertEquals("Home", rows.get(0).ssid);
        assertEquals(-50, rows.get(0).level);
        assertEquals("Cafe", rows.get(1).ssid);
    }

    @Test public void doesNotMergeOpenAndProtectedNetworksWithSameName() {
        assertEquals(2, WifiNetworks.visible(Arrays.asList(
                new WifiNetworks.Entry("Cafe", "[ESS]", -30),
                new WifiNetworks.Entry("Cafe", "[WPA2-PSK-CCMP][ESS]", -50)), "").size());
    }

    @Test public void recognizesSupportedSecurityWithoutTreatingEnterpriseAsOpen() {
        assertEquals(WifiNetworks.Security.OPEN, WifiNetworks.security("[ESS]"));
        assertEquals(WifiNetworks.Security.PSK, WifiNetworks.security("[WPA2-PSK-CCMP][SAE][ESS]"));
        assertEquals(WifiNetworks.Security.WEP, WifiNetworks.security("[WEP][ESS]"));
        assertEquals(WifiNetworks.Security.UNSUPPORTED, WifiNetworks.security("[WPA2-EAP-CCMP][ESS]"));
        assertEquals(WifiNetworks.Security.UNSUPPORTED, WifiNetworks.security("[SAE][ESS]"));
        assertEquals(WifiNetworks.Security.UNSUPPORTED, WifiNetworks.security("[OWE][ESS]"));
    }

    @Test public void validatesPasswordsAndEscapesQuotedSsid() {
        assertFalse(WifiNetworks.validPassword(WifiNetworks.Security.PSK, "short"));
        assertTrue(WifiNetworks.validPassword(WifiNetworks.Security.PSK, "password"));
        assertTrue(WifiNetworks.validPassword(WifiNetworks.Security.WEP, "12345"));
        assertFalse(WifiNetworks.validPassword(WifiNetworks.Security.WEP, "123456"));
        assertEquals("\"My \\\"WiFi\\\"\\\\room\"", WifiNetworks.quote("My \"WiFi\"\\room"));
    }
}
