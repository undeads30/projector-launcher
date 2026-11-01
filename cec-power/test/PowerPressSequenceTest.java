package ru.projector.cecpower;

public final class PowerPressSequenceTest {
    static class Fake implements PowerPressSequence.Clock, PowerPressSequence.Keys {
        long now, first=-1, second=-1, cost;
        int calls;
        boolean fail;
        public long now() { return now; }
        public void sleep(long ms) { if(ms<0) throw new AssertionError(); now+=ms; }
        public void power() throws Exception {
            if (fail) throw new Exception("injection denied");
            if(calls++==0) first=now; else second=now;
            now+=cost;
        }
    }
    public static void main(String[] args) throws Exception {
        Fake f=new Fake(); new PowerPressSequence(f,f).run();
        check(f.calls==2 && f.first==0 && f.second==500,"two presses at 0 and 500");
        f=new Fake(); f.cost=125; new PowerPressSequence(f,f).run();
        check(f.second==500,"injection time counts toward interval");
        f=new Fake(); f.cost=700; new PowerPressSequence(f,f).run();
        check(f.second==700,"never schedule in the past");
        f=new Fake(); f.fail=true;
        try { new PowerPressSequence(f,f).run(); throw new AssertionError("must fail"); }
        catch(Exception expected) { check(f.calls==0,"no retry after failure"); }
        System.out.println("PASS: four power sequence checks");
    }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
