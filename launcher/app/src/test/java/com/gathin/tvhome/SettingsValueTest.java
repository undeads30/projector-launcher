package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SettingsValueTest {
 @Test public void cronusImageReadbackUsesFirmwareScale() {
  assertEquals(50, SettingsValue.cronusImageValue(49));
  assertEquals(51, SettingsValue.cronusImageValue(50));
  assertEquals(0, SettingsValue.cronusImageValue(-1));
  assertEquals(100, SettingsValue.cronusImageValue(99));
 }
 @Test(expected=IllegalArgumentException.class) public void invalidFirmwareValueIsUnavailable() {
  SettingsValue.cronusImageValue(-2);
 }
 @Test public void stepsClampToActualHardwareRange() {
  assertEquals(100, SettingsValue.step(100, 1, 0, 100));
  assertEquals(0, SettingsValue.step(0, -1, 0, 100));
  assertEquals(-49, SettingsValue.step(-50, 1, -50, 50));
  assertEquals(51, SettingsValue.step(50, 1, 0, 100));
 }
 @Test public void invalidReadingsAreRejectedInsteadOfClamped() {
  assertFalse(SettingsValue.valid(-1, 0, 100));
  assertFalse(SettingsValue.valid(101, 0, 100));
  assertTrue(SettingsValue.valid(-50, -50, 50));
  assertFalse(SettingsValue.valid(0, 1, 0));
 }
 @Test(expected=IllegalArgumentException.class) public void cannotChangeUnavailableReading() {
  SettingsValue.step(-1, 1, 0, 100);
 }
 @Test public void confirmationUsesActualReadback() {
  assertTrue(SettingsValue.confirmed(51, 51, 0, 100));
  assertFalse(SettingsValue.confirmed(51, 50, 0, 100));
  assertFalse(SettingsValue.confirmed(-1, -1, 0, 100));
 }
}
