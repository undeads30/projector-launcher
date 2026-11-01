#!/system/bin/sh
export CLASSPATH=/system/framework/cec-power.jar
exec /system/xbin/su 0 /system/bin/app_process /system/bin --nice-name=cec_power ru.projector.cecpower.ShellMain --watch
