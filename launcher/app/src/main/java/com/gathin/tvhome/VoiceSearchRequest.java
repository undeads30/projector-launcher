package com.gathin.tvhome;
/** Main-thread ownership of deferred search actions. */
final class VoiceSearchRequest {
    private boolean active=true;
    private Runnable cancellation;
    boolean active(){return active;}
    boolean claim(){if(!active)return false;active=false;return true;}
    void cancel(){if(active){active=false;if(cancellation!=null)cancellation.run();}}
    void onCancel(Runnable action){cancellation=action;if(!active)action.run();}
    static VoiceSearchRequest replace(VoiceSearchRequest old){if(old!=null)old.cancel();return new VoiceSearchRequest();}
}
