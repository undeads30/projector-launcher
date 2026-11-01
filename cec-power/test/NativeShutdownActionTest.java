package ru.projector.cecpower;
import java.util.ArrayList;
import java.util.List;
public final class NativeShutdownActionTest {
    static class Fake implements NativeShutdownAction.Device {
        long now; boolean failRestore;
        List<String> events=new ArrayList<>();
        public long now(){return now;}
        public void sleep(long ms){now+=ms;}
        public void wake(){events.add("wake@"+now);}
        public void restoreWifi() throws Exception {events.add("wifi@"+now);}
        public void power(){events.add("power@"+now);}
        public void shutdown()throws Exception {events.add("shutdown@"+now);if(failRestore)throw new Exception("request rejected");}
    }
    public static void main(String[] args)throws Exception {
        Fake f=new Fake();new NativeShutdownAction(f).run();
        check(f.events.toString().equals("[shutdown@0]"),"direct shutdown without wake, wifi changes, keys or delay");
        f=new Fake();f.failRestore=true;
        try {new NativeShutdownAction(f).run();throw new AssertionError("must fail");}
        catch(Exception expected){check(f.events.toString().equals("[shutdown@0]"),"failure does not retry or fall back to waking/keys");}
        System.out.println("PASS: two direct shutdown checks");
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
