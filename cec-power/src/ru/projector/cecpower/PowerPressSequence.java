package ru.projector.cecpower;

public final class PowerPressSequence {
    public interface Clock { long now(); void sleep(long ms); }
    public interface Keys { void power() throws Exception; }
    private final Clock clock;
    private final Keys keys;
    public PowerPressSequence(Clock clock, Keys keys) { this.clock=clock; this.keys=keys; }
    public void run() throws Exception {
        long start=clock.now();
        keys.power();
        long remaining=500-(clock.now()-start);
        if(remaining>0) clock.sleep(remaining);
        keys.power();
    }
}
