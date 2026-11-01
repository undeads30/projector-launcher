package ru.projector.cecpower;

import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

final class StandbyWatcher {
    static final String TAG="CecPowerBridge", LOCK="cec_power_bridge";
    static void run() throws Exception {
        if(!"false".equals(command("/system/bin/getprop","ro.vendor.platform.wifi.suspend")))
            throw new IllegalStateException("Wi-Fi suspend must be disabled before starting this bridge");
        final int systemPid=systemPid();
        String token=UUID.randomUUID().toString();
        StandbyTrigger trigger=new StandbyTrigger(systemPid,android.os.Process.myPid(),token);
        AtomicBoolean running=new AtomicBoolean(true);
        Process logs=null;
        Thread keeper=null;
        // Reserve CPU before subscribing; a timed lock expires even after a crash.
        write("/sys/power/wake_lock",LOCK+" 15000000000");
        try {
            logs=new ProcessBuilder("/system/bin/logcat","-b","system","-b","main","-v","epoch","-T","1",
                    "PowerManagerService:I","CecPowerBridge:I","*:S").start();
            final Process logProcess=logs;
            keeper=new Thread(()->{
                int attempts=0,ticks=0;
                try {
                    while(running.get()){
                        write("/sys/power/wake_lock",LOCK+" 15000000000");
                        if(!trigger.isArmed()){
                            if(++attempts>40)throw new IllegalStateException("No fresh-log boundary");
                            Log.i(TAG,"READY "+token);
                        }
                        if(ticks++%5==0 && systemPid()!=systemPid)
                            throw new IllegalStateException("system_server changed; restart watcher");
                        SystemClock.sleep(trigger.isArmed()?1000:250);
                    }
                } catch(Exception failure){
                    Log.e(TAG,"Watchdog failed",failure);logProcess.destroy();
                }
            },"cec-watchdog");
            keeper.setDaemon(true);keeper.start();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(logs.getInputStream()))){
                String line;
                while((line=reader.readLine())!=null){
                    if(!trigger.accept(line,System.currentTimeMillis(),SystemClock.uptimeMillis()))continue;
                    Log.i(TAG,"HDMI standby accepted; requesting native system shutdown");
                    new NativeShutdownAction(SystemShutdown::request).run();
                }
            }
        } finally {
            running.set(false);
            if(keeper!=null){keeper.interrupt();keeper.join(2000);}
            if(logs!=null)logs.destroy();
            write("/sys/power/wake_unlock",LOCK);
        }
        throw new IllegalStateException("logcat ended; init may restart watcher");
    }
    private static String command(String...args)throws Exception {
        Process process=new ProcessBuilder(args).redirectErrorStream(true).start();
        try {
            if(!process.waitFor(3,TimeUnit.SECONDS))throw new IllegalStateException("Command timed out: "+args[0]);
            StringBuilder output=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()))){
                String line;while((line=reader.readLine())!=null)output.append(line);
            }
            if(process.exitValue()!=0)throw new IllegalStateException("Command failed: "+args[0]+" "+output);
            return output.toString().trim();
        } finally {process.destroy();}
    }
    private static int systemPid() throws Exception {
        Process process=new ProcessBuilder("/system/bin/pidof","system_server").start();
        String pid;
        try(BufferedReader r=new BufferedReader(new InputStreamReader(process.getInputStream()))){pid=r.readLine();}
        if(process.waitFor()!=0 || pid==null || !pid.trim().matches("[0-9]+"))
            throw new IllegalStateException("No unambiguous system_server PID");
        return Integer.parseInt(pid.trim());
    }
    private static void write(String path,String value) throws Exception {
        try(FileOutputStream out=new FileOutputStream(path)){out.write((value+"\n").getBytes(StandardCharsets.US_ASCII));}
    }
}
