package com.gathin.tvhome;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
final class YoutubeVpnLaunch {
 private static boolean pending;
 static boolean connected(Context c) {
  ConnectivityManager m=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
  if(m==null)return false;
  for(Network n:m.getAllNetworks()) {
   NetworkCapabilities caps=m.getNetworkCapabilities(n);
   if(caps!=null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))return true;
  }
  return false;
 }
 private static void command(Context c,String guid,boolean on) {
  Bundle b=new Bundle();b.putBoolean("tasker_extra_bundle_switch",on);b.putString("tasker_extra_bundle_guid",guid);
  Intent i=new Intent("com.twofortyfouram.locale.intent.action.FIRE_SETTING").setClassName("com.v2ray.ang","com.v2ray.ang.receiver.TaskerReceiver");
  i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
  i.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE",b);c.sendBroadcast(i);
 }
 static void stop(Context c) {
  // TaskerReceiver requires a non-empty GUID even for OFF; OFF ignores the selected profile.
  command(c,"Default",false);
 }
 static void open(Context c,Intent target) {
  if(pending)return;
  String pkg=target.getComponent().getPackageName();
  String guid=c.getSharedPreferences("vpn_auto",Context.MODE_PRIVATE).getString(VpnLaunchPolicy.profileKey(pkg),"");
  if(guid.isEmpty()) {Toast.makeText(c,"Сначала настройте VPN для этого приложения",Toast.LENGTH_LONG).show();return;}
  pending=true;
  Handler h=new Handler(Looper.getMainLooper());
  Toast.makeText(c,"Подключение VPN для приложения…",Toast.LENGTH_SHORT).show();
  Runnable connect=()->{
   command(c,guid,true);
   h.postDelayed(new Runnable(){int remaining=40;public void run(){
    if(connected(c)) h.postDelayed(()->{pending=false;c.startActivity(target);},1500);
    else if(--remaining>0)h.postDelayed(this,500);
    else fail(c);
   }},500);
  };
  if(connected(c)) {
   command(c,guid,false);
   h.postDelayed(new Runnable(){int remaining=30;public void run(){
    if(!connected(c))h.postDelayed(connect,700);
    else if(--remaining>0)h.postDelayed(this,500);
    else fail(c);
   }},500);
  } else connect.run();
 }
 private static void fail(Context c) {
  pending=false;Toast.makeText(c,"VPN не подключился. Проверьте v2rayNG",Toast.LENGTH_LONG).show();
 }
}
