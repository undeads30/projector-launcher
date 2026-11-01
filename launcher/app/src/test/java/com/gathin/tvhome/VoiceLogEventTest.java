package com.gathin.tvhome;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceLogEventTest {
    private byte[] frame(int uid, String tag, String message) {
        byte[] body = ("\u0003" + tag + "\0" + message + "\0").getBytes(StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(28+body.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short)body.length).putShort((short)28).putInt(428).putInt(428);
        b.putInt(1000).putInt(123000000).putInt(0).putInt(uid).put(body);
        return b.array();
    }
    @Test public void authenticatesUidAndRejectsForgedTags() {
        assertEquals(VoiceLogEvent.DOWN, VoiceLogEvent.parse(frame(0,"voice_hidraw","read_message(L663): voice key pressed"),1000100,1000200));
        assertEquals(VoiceLogEvent.UP, VoiceLogEvent.parse(frame(0,"voice_hidraw","read_message(L714): voice key relesed"),1000100,1000200));
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(frame(10062,"voice_hidraw","read_message(L663): voice key pressed"),1000100,1000200));
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(frame(0,"other","voice key pressed"),1000100,1000200));
    }
    @Test public void ignoresOldMalformedAndNonButtonData() {
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(frame(0,"voice_hidraw","voice key pressed"),1000200,1000300));
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(frame(0,"voice_hidraw","voice key pressed"),1000100,1003000));
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(frame(0,"voice_hidraw","voice data size: 240"),1000100,1000200));
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(new byte[5],0,1000200));
        byte[] noUid=frame(0,"voice_hidraw","voice key pressed"); noUid[2]=24;
        assertEquals(VoiceLogEvent.NONE, VoiceLogEvent.parse(noUid,1000100,1000200));
    }
}
