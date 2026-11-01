#!/system/bin/sh
set -eu
if pidof cec_power >/dev/null; then echo "cec_power already running"; exit 1; fi
export CLASSPATH=/data/local/tmp/cec-power-test.jar
nohup /system/bin/app_process /system/bin --nice-name=cec_power ru.projector.cecpower.ShellMain --watch > /data/local/tmp/cec-power-test.log 2>&1 < /dev/null &
echo "temporary watcher PID=$!"
