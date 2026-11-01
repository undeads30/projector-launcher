package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VpnHealthProbeTest {
 @Test public void acceptsOnlyExpected204ResponseIncludingWireCarriageReturn() {
  assertTrue(VpnHealthProbe.isSuccess("HTTP/1.1 204 No Content\r"));
  assertTrue(VpnHealthProbe.isSuccess("HTTP/1.0 204\r"));
  assertFalse(VpnHealthProbe.isSuccess("HTTP/1.1 200 OK\r"));
  assertFalse(VpnHealthProbe.isSuccess("HTTP/1.1 302 Found\r"));
  assertFalse(VpnHealthProbe.isSuccess("HTTP/1.1 503 Unavailable\r"));
  assertFalse(VpnHealthProbe.isSuccess(""));
 }
}
