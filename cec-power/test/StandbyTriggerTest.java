package ru.projector.cecpower;
public final class StandbyTriggerTest {
    static final String EVENT="0.106 580 580 I PowerManagerService: Going to sleep due to HDMI standby (uid 1000)...";
    public static void main(String[] args) {
        StandbyTrigger t=new StandbyTrigger(580,1234,"unique");
        check(!t.accept(EVENT,100,100),"reject history before marker");
        check(!t.accept("0.101 777 777 I CecPowerBridge: READY unique",101,101),"ignore spoofed marker pid");
        check(!t.accept(EVENT,102,102),"still unarmed");
        check(!t.accept("0.103 1234 1234 I CecPowerBridge: READY unique",103,103),"marker is not event");
        check(!t.accept(EVENT.replace("580","581"),104,104),"reject spoofed system tag");
        check(!t.accept(EVENT.replace("HDMI standby","power button"),105,105),"ignore native power");
        check(!t.accept(EVENT,5000,105),"reject stale buffered command after resume");
        check(!t.accept(EVENT.replace("0.106","99.000"),106,105),"reject future event after clock reset");
        check(t.accept(EVENT,106,106),"accept fresh system HDMI sleep");
        check(!t.accept(EVENT,200,200),"reject duplicate");
        check(!t.accept("0.300 1234 1234 I CecPowerBridge: READY unique",300,300),"marker cannot reset cooldown");
        check(!t.accept(EVENT,400,400),"still in cooldown");
        check(t.accept(EVENT.replace("0.106","15.106"),15106,15106),"new command after cooldown");
        System.out.println("PASS: thirteen standby trigger checks");
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
