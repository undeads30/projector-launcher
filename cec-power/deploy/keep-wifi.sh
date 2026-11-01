#!/system/bin/sh
set -eu
[ "$(id -u)" = 0 ] || exit 1
[ "$(getprop ro.product.model)" = Cronus ] || exit 1
cmp /vendor/build.prop /data/local/tmp/vendor-build-before-wifi.prop
grep -qx 'ro.vendor.platform.wifi.suspend=false' /data/local/tmp/vendor-build-wifi-kept.prop
mount -o remount,rw /vendor
changed=0
cleanup() {
    result=$?
    trap - EXIT
    set +e
    if [ "$changed" != 1 ]; then
        cp /data/local/tmp/vendor-build-before-wifi.prop /vendor/build.prop || result=1
        chmod 600 /vendor/build.prop || result=1
        restorecon /vendor/build.prop || result=1
    fi
    if ! mount -o remount,ro /vendor; then echo 'ERROR: vendor read-only restoration failed' >&2; result=1; fi
    exit "$result"
}
trap cleanup EXIT
cp /data/local/tmp/vendor-build-wifi-kept.prop /vendor/build.prop
chown 0:0 /vendor/build.prop
chmod 600 /vendor/build.prop
restorecon /vendor/build.prop
cmp /vendor/build.prop /data/local/tmp/vendor-build-wifi-kept.prop
grep -n ro.vendor.platform.wifi.suspend /vendor/build.prop
ls -lZ /vendor/build.prop
changed=1
echo 'Wi-Fi sleep disable saved; applies after next boot.'
