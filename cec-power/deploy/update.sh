#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || exit 1
[ "$(getprop ro.product.model)" = Cronus ] || exit 1
backup=/data/local/tmp/cec-power-before-update
[ ! -e "$backup" ] || { echo 'Update backup already exists; inspect before retry'; exit 1; }
mkdir "$backup"
cp /system/framework/cec-power.jar "$backup/cec-power.jar"
cp /system/etc/init/cec-power.rc "$backup/cec-power.rc"
cp /system/etc/cec-power/run.sh "$backup/run.sh"
mount -o remount,rw /
updated=0
cleanup() {
    result=$?
    trap - EXIT
    set +e
    if [ "$updated" != 1 ]; then
        cp "$backup/cec-power.jar" /system/framework/cec-power.jar || result=1
        cp "$backup/cec-power.rc" /system/etc/init/cec-power.rc || result=1
        cp "$backup/run.sh" /system/etc/cec-power/run.sh || result=1
    fi
    chmod 644 /system/framework/cec-power.jar /system/etc/init/cec-power.rc || result=1
    chmod 755 /system/etc/cec-power/run.sh || result=1
    restorecon /system/framework/cec-power.jar /system/etc/init/cec-power.rc /system/etc/cec-power/run.sh || result=1
    if ! mount -o remount,ro /; then echo 'ERROR: read-only restoration failed' >&2; result=1; fi
    exit "$result"
}
trap cleanup EXIT
cp /data/local/tmp/cec-power-install/cec-power.jar /system/framework/cec-power.jar
cp /data/local/tmp/cec-power-install/cec-power.rc /system/etc/init/cec-power.rc
cp /data/local/tmp/cec-power-install/run.sh /system/etc/cec-power/run.sh
cmp /data/local/tmp/cec-power-install/cec-power.jar /system/framework/cec-power.jar
cmp /data/local/tmp/cec-power-install/cec-power.rc /system/etc/init/cec-power.rc
cmp /data/local/tmp/cec-power-install/run.sh /system/etc/cec-power/run.sh
updated=1
echo 'Updated files; restoring read-only mount.'
