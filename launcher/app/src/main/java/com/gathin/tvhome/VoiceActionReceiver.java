package com.gathin.tvhome;
import android.content.*;
import android.os.SystemClock;
import android.Manifest;
import android.content.pm.PackageManager;
public class VoiceActionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if("voice.stop".equals(intent.getAction())){VoiceSettingsActivity.configure(context,false,false);return;}
        boolean search="voice.search".equals(intent.getAction());
        if(!search&&!"voice.result".equals(intent.getAction()))return;
        long age=SystemClock.elapsedRealtime()-intent.getLongExtra("issued",-100000);
        if(age<0||age>2000||!context.getSharedPreferences("voice_control",0).getBoolean("armed",false))return;
        if(context.checkSelfPermission(Manifest.permission.READ_LOGS)!=PackageManager.PERMISSION_GRANTED
                ||context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;
        try {
            if(search){
                String query=intent.getStringExtra("query");
                if(query==null||query.isBlank()||query.length()>200)return;
                context.startActivity(new Intent(context,VoiceSearchActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("query",query).putExtra("destination",VoiceSearchQuery.destination(intent.getStringExtra("destination"))));
            }else VoiceActions.execute(context,VoiceCommand.valueOf(intent.getStringExtra("command")));
        }
        catch(Exception error){VoiceActions.message(context,"Не удалось выполнить команду");}
    }
}
