package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceSearchRequestTest {
 @Test public void cancellationPreventsDelayedDispatch(){
  VoiceSearchRequest request=new VoiceSearchRequest();request.cancel();
  assertFalse(request.active());assertFalse(request.claim());
 }
 @Test public void completingDispatchIsSingleUse(){
  VoiceSearchRequest request=new VoiceSearchRequest();assertTrue(request.claim());assertFalse(request.claim());
 }
 @Test public void replacingRequestInvalidatesPrevious(){
  VoiceSearchRequest first=new VoiceSearchRequest();
  VoiceSearchRequest second=VoiceSearchRequest.replace(first);
  assertFalse(first.claim());assertTrue(second.claim());
 }
}
