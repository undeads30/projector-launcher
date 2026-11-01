package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceSessionTest {
    @Test public void oneFinalResultAfterRelease() {
        VoiceSession state = new VoiceSession();
        long id = state.start(100);
        assertTrue(id > 0);
        assertEquals(0, state.start(200));
        assertFalse(state.complete(id));
        assertTrue(state.release(id, 300));
        assertFalse(state.isRecording(id, 300));
        assertTrue(state.complete(id));
        assertFalse(state.complete(id));
    }
    @Test public void cancelAndTimeoutNeverExecute() {
        VoiceSession state = new VoiceSession();
        long old = state.start(100);
        state.cancel();
        long current = state.start(200);
        assertFalse(state.release(old, 300));
        assertTrue(state.isRecording(current, 12199));
        assertFalse(state.isRecording(current, 12200));
        assertFalse(state.release(current, 12300));
        assertFalse(state.complete(current));
        assertTrue(state.start(12300) > current);
    }
    @Test public void cancelledProcessingDiscardsLateResult() {
        VoiceSession state = new VoiceSession();
        long old = state.start(0);
        state.release(old, 10);
        state.cancel();
        assertFalse(state.complete(old));
    }
    @Test public void lateReleaseCannotExecuteEvenIfDecoderWasBlocked() {
        VoiceSession state = new VoiceSession();
        long id = state.start(100);
        assertFalse(state.release(id, 12100));
        assertFalse(state.complete(id));
    }
    @Test public void cancelledResultCannotPublishOrPublishTwice() {
        VoiceSession state = new VoiceSession();
        StringBuilder published = new StringBuilder();
        long old = state.start(0); state.release(old, 100); state.cancel();
        assertFalse(state.publish(old, () -> published.append("stale")));
        long current = state.start(200); state.release(current, 300);
        assertTrue(state.publish(current, () -> published.append("command")));
        assertFalse(state.publish(current, () -> published.append("duplicate")));
        assertEquals("command", published.toString());
    }
}
