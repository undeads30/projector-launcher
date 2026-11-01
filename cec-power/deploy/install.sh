#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || { echo 'root required'; exit 1; }
[ "$(getprop ro.product.model)" = Cronus ] || { echo 'Cronus required'; exit 1; }
for target in /system/etc/init/cec-power.rc /system/framework/cec-power.jar /system/etc/cec-power; do
    [ ! -e "$target" ] || { echo "Refusing to replace $target"; exit 1; }
done
for source in cec-power.rc cec-power.jar run.sh; do
    [ -s "/data/local/tmp/cec-power-install/$source" ] || exit 1
done
mount -o remount,rw /
installed=0
cleanup() {
    result=$?
    trap - EXIT
    set +e
    if [ "$installed" != 1 ]; then
        rm -f /system/etc/init/cec-power.rc /system/framework/cec-power.jar /system/etc/cec-power/run.sh || result=1
        rmdir /system/etc/cec-power 2>/dev/null || true
    fi
    if ! mount -o remount,ro /; then
        echo 'ERROR: read-only mount restoration failed; installation incomplete' >&2
        result=1
    fi
    exit "$result"
}
trap cleanup EXIT
mkdir /system/etc/cec-power
cp /data/local/tmp/cec-power-install/cec-power.rc /system/etc/init/cec-power.rc
cp /data/local/tmp/cec-power-install/cec-power.jar /system/framework/cec-power.jar
cp /data/local/tmp/cec-power-install/run.sh /system/etc/cec-power/run.sh
chown 0:0 /system/etc/cec-power /system/etc/cec-power/run.sh /system/etc/init/cec-power.rc /system/framework/cec-power.jar
chmod 755 /system/etc/cec-power /system/etc/cec-power/run.sh
chmod 644 /system/etc/init/cec-power.rc /system/framework/cec-power.jar
restorecon /system/etc/cec-power /system/etc/cec-power/run.sh /system/etc/init/cec-power.rc /system/framework/cec-power.jar
cmp /data/local/tmp/cec-power-install/cec-power.jar /system/framework/cec-power.jar
cmp /data/local/tmp/cec-power-install/run.sh /system/etc/cec-power/run.sh
cmp /data/local/tmp/cec-power-install/cec-power.rc /system/etc/init/cec-power.rc
sha256sum /system/framework/cec-power.jar /system/etc/cec-power/run.sh /system/etc/init/cec-power.rc
ls -lZ /system/framework/cec-power.jar /system/etc/cec-power/run.sh /system/etc/init/cec-power.rc
installed=1
echo 'Files copied. Restoring read-only mount; init will load service on next boot.'
