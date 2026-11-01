#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || exit 1
[ "$(getprop ro.product.model)" = Cronus ] || exit 1
[ "$(getprop ro.vendor.platform.wifi.suspend)" = false ] || exit 1
backup=/data/local/tmp/cec-power-before-no-wake.jar
[ ! -e "$backup" ] || { echo 'Backup already exists; inspect before retry'; exit 1; }
[ -s /data/local/tmp/cec-power-no-wake.jar ] || exit 1
cp /system/framework/cec-power.jar "$backup"
setprop ctl.stop cec_power
attempt=0
while pidof cec_power >/dev/null; do
    attempt=$((attempt+1)); [ "$attempt" -le 25 ] || { echo 'Service did not stop'; exit 1; }
    sleep 0.2
done
mount -o remount,rw /
updated=0
cleanup() {
    result=$?
    trap - EXIT
    set +e
    if [ "$updated" != 1 ]; then cp "$backup" /system/framework/cec-power.jar || result=1; fi
    chown 0:0 /system/framework/cec-power.jar || result=1
    chmod 644 /system/framework/cec-power.jar || result=1
    restorecon /system/framework/cec-power.jar || result=1
    if ! mount -o remount,ro /; then echo 'ERROR: read-only restoration failed' >&2; result=1; fi
    setprop ctl.start cec_power || result=1
    exit "$result"
}
trap cleanup EXIT
cp /data/local/tmp/cec-power-no-wake.jar /system/framework/cec-power.jar
cmp /data/local/tmp/cec-power-no-wake.jar /system/framework/cec-power.jar
sha256sum /system/framework/cec-power.jar
updated=1
