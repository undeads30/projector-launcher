package ru.projector.cecpower;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.Method;
final class SystemShutdown {
    static void request()throws Exception {
        Class<?> manager=Class.forName("android.os.ServiceManager");
        IBinder binder=(IBinder)manager.getMethod("getService",String.class).invoke(null,"power");
        if(binder==null)throw new IllegalStateException("Power service unavailable");
        Class<?> stub=Class.forName("android.os.IPowerManager$Stub");
        Object power=stub.getMethod("asInterface",IBinder.class).invoke(null,binder);
        // Standard shutdown entry point: no confirmation UI, no screen wake, no reboot.
        shutdownMethod()
                .invoke(power,false,"userrequested",false);
        Log.i("CecPowerBridge","Native system shutdown requested without screen wake");
    }
    private static Method shutdownMethod()throws Exception {
        // Resolve on public AIDL interface, not its private generated Proxy class.
        return Class.forName("android.os.IPowerManager").getMethod("shutdown",boolean.class,String.class,boolean.class);
    }
    static void validate()throws Exception {
        Method method=shutdownMethod();
        if(!java.lang.reflect.Modifier.isPublic(method.getDeclaringClass().getModifiers()))
            throw new IllegalStateException("Shutdown method must be declared on a public interface");
        System.out.println("Shutdown API available: "+method);
    }
}
