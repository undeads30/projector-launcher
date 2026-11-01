package com.gathin.tvhome;
import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
public class VoiceSettingsActivity extends Activity {
    static void configure(Context c,boolean running,boolean armed){
        c.getSharedPreferences("voice_control",0).edit().putBoolean("running",running).putBoolean("armed",running&&armed).commit();
        if(running)ensureStarted(c);else c.stopService(new Intent(c,LocalVoiceService.class));
    }
    static void ensureStarted(Context c){
        if(!c.getSharedPreferences("voice_control",0).getBoolean("running",false))return;
        if(c.checkSelfPermission(Manifest.permission.READ_LOGS)!=PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)return;
        c.startForegroundService(new Intent(c,LocalVoiceService.class));
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        String mode=getIntent().getStringExtra("mode");
        // DUMP-protected existing maintenance entry, also used to verify app routing without audio.
        if("search-test".equals(mode)){
            String query=getIntent().getStringExtra("query");
            if(query!=null&&!query.isBlank()&&query.length()<=200)
                startActivity(new Intent(this,VoiceSearchActivity.class).putExtra("query",query)
                        .putExtra("destination",VoiceSearchQuery.destination(getIntent().getStringExtra("destination"))));
            finish();return;
        }
        if(mode!=null){configure(this,!"off".equals(mode),"on".equals(mode));finish();return;}
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(48,32,48,24);
        TextView text=new TextView(this);text.setTextSize(24);text.setText("Голосовые команды\n\nУдерживайте микрофон, говорите и отпускайте.\n«Открой ютуб», «Тише», «Домой», «Пауза», «Вход один».\n«Найди Интерстеллар» — поиск в текущем видеоприложении.\nС главного экрана выберите Кино, YouTube или ВК Видео.\nРаспознавание работает без интернета.");layout.addView(text);
        Button on=new Button(this);on.setText("Включить");on.setOnClickListener(v->{configure(this,true,true);finish();});layout.addView(on);
        Button off=new Button(this);off.setText("Выключить");off.setOnClickListener(v->{configure(this,false,false);finish();});layout.addView(off);setContentView(layout);
    }
}
