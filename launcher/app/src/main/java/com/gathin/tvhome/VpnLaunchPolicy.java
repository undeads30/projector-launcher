package com.gathin.tvhome;
final class VpnLaunchPolicy {
 static boolean requiresVpn(String pkg) { return "com.google.android.youtube.tv".equals(pkg) || "com.vk.tv".equals(pkg); }
 static String profileKey(String pkg) { return "com.vk.tv".equals(pkg)?"vk_profile":"profile"; }
 static boolean shouldStart(boolean connected) { return !connected; }
 static boolean shouldStopForWindow(String pkg, boolean connected) {
  return connected && "mov.film".equals(pkg);
 }
}
