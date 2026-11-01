package com.gathin.tvhome;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
final class VoiceLogEvent {
    static final int NONE=0,DOWN=1,UP=2;
    static int parse(byte[] frame,long listenerStarted,long now) {
        if(frame==null||frame.length<28)return NONE;
        ByteBuffer b=ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN);
        int length=b.getShort(0)&65535, header=b.getShort(2)&65535;
        if(header!=28||header+length!=frame.length||length<4||b.getInt(24)!=0||b.getInt(4)<=0)return NONE;
        long timestamp=(b.getInt(12)&0xffffffffL)*1000L+(b.getInt(16)&0xffffffffL)/1000000L;
        if(timestamp<listenerStarted||now-timestamp>2000||timestamp-now>500)return NONE;
        int tagEnd=header+1;
        while(tagEnd<frame.length&&frame[tagEnd]!=0)tagEnd++;
        if(tagEnd>=frame.length)return NONE;
        String tag=new String(frame,header+1,tagEnd-header-1,StandardCharsets.UTF_8);
        if(!"voice_hidraw".equals(tag))return NONE;
        int end=tagEnd+1;while(end<frame.length&&frame[end]!=0)end++;
        String message=new String(frame,tagEnd+1,end-tagEnd-1,StandardCharsets.UTF_8).trim();
        if(message.matches("read_message\\(L[0-9]+\\): voice key pressed"))return DOWN;
        if(message.matches("read_message\\(L[0-9]+\\): voice key (relesed|released)"))return UP;
        return NONE;
    }
}
