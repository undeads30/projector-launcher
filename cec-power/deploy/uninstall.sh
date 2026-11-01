#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || exit 1
[ "$(getprop ro.product.model)" = Cronus ] || exit 1
setprop ctl.stop cec_power || true
for process in $(pidof cec_power || true); do
    [ -d "/proc/$process" ] || continue
    owner=$(awk '/^Uid:/{print $2}' "/proc/$process/status")
    executable=$(readlink "/proc/$process/exe")
    [ "$owner" = 0 ] || { echo 'Unexpected watcher owner'; exit 1; }
    case "$executable" in /system/bin/app_process|/system/bin/app_process32|/system/bin/app_process64) ;; *) echo 'Unexpected watcher executable'; exit 1;; esac
    tr '\000' '\n' < "/proc/$process/environ" | grep -Eq '^CLASSPATH=(/system/framework/cec-power.jar|/data/local/tmp/cec-power-test.jar)$' || { echo 'Unexpected watcher classpath'; exit 1; }
    kill "$process" || { [ ! -d "/proc/$process" ] || exit 1; }
done
remaining=0
while pidof cec_power >/dev/null; do
    remaining=$((remaining+1))
    [ "$remaining" -le 25 ] || { echo 'Watcher did not stop; files preserved'; exit 1; }
    sleep 0.2
done
echo cec_power_bridge > /sys/power/wake_unlock || true
mount -o remount,rw /
cleanup() {
    result=$?
    trap - EXIT
    set +e
    if ! mount -o remount,ro /; then echo 'ERROR: read-only mount restoration failed' >&2; result=1; fi
    exit "$result"
}
trap cleanup EXIT
rm -f /system/etc/init/cec-power.rc /system/framework/cec-power.jar /system/etc/cec-power/run.sh
rmdir /system/etc/cec-power 2>/dev/null || true
echo 'Removed. Original HDMI standby behavior restored.'
