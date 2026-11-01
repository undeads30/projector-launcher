package com.gathin.tvhome;
final class VoiceSession {
    private long generation;
    private long deadline;
    private int state; // 0 idle, 1 recording, 2 finalizing
    synchronized long start(long now) {
        if(state!=0)return 0;
        generation++; deadline=now+12000; state=1; return generation;
    }
    synchronized boolean release(long id,long now) {
        if(id!=generation||state!=1)return false;
        if(now>=deadline){cancel();return false;}
        state=2; return true;
    }
    synchronized boolean isRecording(long id,long now) {
        if(id!=generation||state!=1)return false;
        if(now>=deadline) { cancel(); return false; }
        return true;
    }
    synchronized boolean isProcessing(long id) { return id==generation&&state==2; }
    synchronized boolean complete(long id) {
        if(!isProcessing(id))return false;
        state=0; return true;
    }
    // Publication is the commit point: cancellation cannot interleave with validation/send.
    synchronized boolean publish(long id,Runnable action) {
        if(!isProcessing(id))return false;
        state=0;action.run();return true;
    }
    synchronized void cancel() { generation++; state=0; }
}
