package ru.projector.cecpower;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import java.lang.reflect.Method;

public final class ShellMain {
    private static final String TAG="CecPowerSequence";
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || (!"--press-twice".equals(args[0]) && !"--watch".equals(args[0]) && !"--check-shutdown".equals(args[0]))) {
            System.out.println("Usage: ru.projector.cecpower.ShellMain --press-twice|--watch|--check-shutdown");
            return;
        }
        if("--check-shutdown".equals(args[0])){SystemShutdown.validate();return;}
        if("--watch".equals(args[0])){StandbyWatcher.run();return;}
        pressTwice(new Input());
    }
    static final class Input {
        final Object manager;
        final Method inject;
        Input() throws Exception {
        Class<?> type=Class.forName("android.hardware.input.InputManager");
        manager=type.getMethod("getInstance").invoke(null);
        inject=type.getMethod("injectInputEvent",InputEvent.class,int.class);
        }
        void key(int code) throws Exception {
            long down=SystemClock.uptimeMillis();
            Log.i(TAG,"KEY="+code+" down uptime="+down);
            send(manager,inject,down,KeyEvent.ACTION_DOWN,code);
            send(manager,inject,down,KeyEvent.ACTION_UP,code);
        }
    }
    static void pressTwice(Input input) throws Exception {
        PowerPressSequence.Clock clock=new PowerPressSequence.Clock() {
            public long now(){return SystemClock.uptimeMillis();}
            public void sleep(long ms){SystemClock.sleep(ms);}
        };
        PowerPressSequence.Keys keys=()->input.key(KeyEvent.KEYCODE_POWER);
        new PowerPressSequence(clock,keys).run();
        Log.i(TAG,"two POWER presses completed");
        System.out.println("two POWER presses completed");
    }
    private static void send(Object manager,Method inject,long down,int action,int code) throws Exception {
        KeyEvent event=new KeyEvent(down,SystemClock.uptimeMillis(),action,
                code,0,0,-1,0,KeyEvent.FLAG_FROM_SYSTEM,InputDevice.SOURCE_KEYBOARD);
        if(!Boolean.TRUE.equals(inject.invoke(manager,event,2)))
            throw new IllegalStateException("Key injection rejected, action="+action);
    }
}
