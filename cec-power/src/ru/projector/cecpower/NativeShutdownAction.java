package ru.projector.cecpower;
public final class NativeShutdownAction {
    public interface Device { void shutdown() throws Exception; }
    private final Device device;
    public NativeShutdownAction(Device device){this.device=device;}
    public void run()throws Exception {device.shutdown();}
}
