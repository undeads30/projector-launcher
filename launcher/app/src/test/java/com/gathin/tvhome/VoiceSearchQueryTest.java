package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceSearchQueryTest {
 @Test public void extractsExplicitSearchAndKeepsTitleWords() {
  assertEquals("интерстеллар", VoiceSearchQuery.extract("найди фильм Интерстеллар"));
  assertEquals("пауза", VoiceSearchQuery.extract("найди Пауза"));
  assertEquals("домой", VoiceSearchQuery.extract("найди видео Домой"));
  assertEquals("громче", VoiceSearchQuery.extract("НАЙДИ   Громче!"));
 }
 @Test public void rejectsUnknownEmptyNegativeAndOversizedSpeech() {
  for(String phrase:new String[]{"", "найди", "найди фильм", "не найди кино", "не надо искать фильм", "интерстеллар", "открой ютуб"})
   assertEquals("", VoiceSearchQuery.extract(phrase));
  assertEquals("", VoiceSearchQuery.extract(null));
  assertEquals("", VoiceSearchQuery.extract("найди "+"а".repeat(201)));
 }
 @Test public void onlyAllowlistedForegroundAppsRouteDirectly() {
  for(String pkg:new String[]{"mov.film","com.google.android.youtube.tv","com.vk.tv"})assertEquals(pkg,VoiceSearchQuery.destination(pkg));
  for(String pkg:new String[]{"com.gathin.tvhome","com.android.settings","com.example",null})assertEquals("",VoiceSearchQuery.destination(pkg));
 }
 @Test public void searchOrUnknownSpeechCannotFallBackToDeviceCommand() {
  assertEquals(VoiceCommand.NONE,VoiceSearchQuery.command("найди пауза",VoiceCommand.PAUSE));
  assertEquals(VoiceCommand.NONE,VoiceSearchQuery.command("не делай тише",VoiceCommand.VOLUME_DOWN));
  assertEquals(VoiceCommand.NONE,VoiceSearchQuery.command("какой то фильм",VoiceCommand.HOME));
  assertEquals(VoiceCommand.VOLUME_DOWN,VoiceSearchQuery.command("тише",VoiceCommand.VOLUME_DOWN));
 }
}
