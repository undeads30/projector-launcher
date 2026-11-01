package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VpnHealthStateTest {
 @Test public void startsOrangeAndFollowsProbe() {
  VpnHealthState s = new VpnHealthState();
  assertEquals(VpnHealthState.Status.OFF, s.status());
  s.select("42/HK");
  assertEquals(VpnHealthState.Status.UNREACHABLE, s.status());
  s.complete(s.generation(), true);
  assertEquals(VpnHealthState.Status.ONLINE, s.status());
  s.complete(s.generation(), false);
  assertEquals(VpnHealthState.Status.UNREACHABLE, s.status());
 }
 @Test public void staleSuccessCannotGreenChangedProfileOrStoppedTunnel() {
  VpnHealthState s = new VpnHealthState();
  s.select("42/HK"); long old = s.generation();
  s.select("42/RU"); s.complete(old, true);
  assertEquals(VpnHealthState.Status.UNREACHABLE, s.status());
  old = s.generation(); s.select(""); s.complete(old, true);
  assertEquals(VpnHealthState.Status.OFF, s.status());
  s.select("43/RU"); s.complete(old, true);
  assertEquals(VpnHealthState.Status.UNREACHABLE, s.status());
 }
 @Test public void unchangedIdentityKeepsSuccessfulCheck() {
  VpnHealthState s = new VpnHealthState(); s.select("42/HK");
  s.complete(s.generation(), true); s.select("42/HK");
  assertEquals(VpnHealthState.Status.ONLINE, s.status());
 }
}
