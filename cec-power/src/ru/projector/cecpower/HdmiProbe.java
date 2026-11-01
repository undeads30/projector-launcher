package ru.projector.cecpower;
import android.os.IBinder;
import java.lang.reflect.Method;
public final class HdmiProbe {
    public static void main(String[] args)throws Exception {
        Class<?> sm=Class.forName("android.os.ServiceManager");
        IBinder binder=(IBinder)sm.getMethod("getService",String.class).invoke(null,"hdmi_control");
        Class<?> stub=Class.forName("android.hardware.hdmi.IHdmiControlService$Stub");
        Object service=stub.getMethod("asInterface",IBinder.class).invoke(null,binder);
        if(args.length==1 && ("guard-on".equals(args[0])||"guard-off".equals(args[0]))){
            service.getClass().getMethod("setProhibitMode",boolean.class).invoke(service,"guard-on".equals(args[0]));
            System.out.println("Temporary prohibit mode="+"guard-on".equals(args[0]));return;
        }
        if(args.length>0)throw new IllegalArgumentException("No args for read-only methods; guard-on/off for temporary mode");
        for(Method method:service.getClass().getMethods())System.out.println(method.toString());
    }
}
