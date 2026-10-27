package com.gathin.tvhome;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
public class VpnProfileSetupActivity extends Activity {
 @Override public void onCreate(Bundle state) { super.onCreate(state);
  if (state == null) startActivityForResult(new Intent("com.twofortyfouram.locale.intent.action.EDIT_SETTING").setClassName("com.v2ray.ang", "com.v2ray.ang.ui.TaskerActivity"), 17);
 }
 @Override protected void onActivityResult(int req,int result,Intent data) {
  super.onActivityResult(req,result,data);
  if(req==17 && result==RESULT_OK && data!=null) {
   Bundle b=data.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE");
   String guid=b==null?null:b.getString("tasker_extra_bundle_guid");
   if(guid!=null && !guid.isEmpty() && !guid.equals("Default")) {
    String key="vk".equals(getIntent().getStringExtra("target"))?"vk_profile":"profile";
    getSharedPreferences("vpn_auto",MODE_PRIVATE).edit().putString(key,guid).apply();
    Toast.makeText(this,"VPN profile saved",Toast.LENGTH_LONG).show();
   }
  }
  finish();
 }
}
