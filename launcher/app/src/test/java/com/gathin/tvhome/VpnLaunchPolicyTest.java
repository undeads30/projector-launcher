package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VpnLaunchPolicyTest {
 @Test public void youtubeAndVkRequireVpn() {
 assertTrue(VpnLaunchPolicy.requiresVpn("com.google.android.youtube.tv"));
 assertTrue(VpnLaunchPolicy.requiresVpn("com.vk.tv"));
 assertFalse(VpnLaunchPolicy.requiresVpn("com.v2ray.ang"));
 }
 @Test public void neverToggleAnAlreadyConnectedVpn() {
 assertFalse(VpnLaunchPolicy.shouldStart(true));
 assertTrue(VpnLaunchPolicy.shouldStart(false));
 }
 @Test public void filmMovStopsVpnButOtherAppsDoNot() {
 assertTrue(VpnLaunchPolicy.shouldStopForWindow("mov.film", true));
 assertFalse(VpnLaunchPolicy.shouldStopForWindow("mov.film", false));
 assertFalse(VpnLaunchPolicy.shouldStopForWindow("com.google.android.youtube.tv", true));
 assertFalse(VpnLaunchPolicy.shouldStopForWindow("com.vk.tv", true));
 assertFalse(VpnLaunchPolicy.shouldStopForWindow("com.v2ray.ang", true));
 assertFalse(VpnLaunchPolicy.shouldStopForWindow(null, true));
 assertFalse(VpnLaunchPolicy.requiresVpn("mov.film"));
 }
}
