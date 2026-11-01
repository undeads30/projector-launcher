package com.gathin.tvhome;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Private query chooser/dispatch screen. No audio or query persistence. */
public final class VoiceSearchActivity extends Activity {
    private boolean dispatched;
    private boolean launched;
    private VoiceSearchRequest request;
    private TextView hint;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String query=getIntent().getStringExtra("query");
        if(query==null||query.isBlank()||query.length()>200){finish();return;}
        float density=getResources().getDisplayMetrics().density;
        FrameLayout overlay=new FrameLayout(this);
        overlay.setOnClickListener(v->finish());
        overlay.setFocusable(false);
        View panel=getLayoutInflater().inflate(R.layout.popup_app_menu,overlay,false);
        panel.setClickable(true);
        panel.setFocusable(false);
        TextView title=panel.findViewById(R.id.appMenuTitle);
        title.setText("Где искать «"+query+"»?");
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTypeface(android.graphics.Typeface.create(getResources().getFont(R.font.outfit),android.graphics.Typeface.BOLD));
        title.setFontVariationSettings("'wght' 700");
        title.setSingleLine(false);title.setMaxLines(3);
        title.setPadding((int)(12*density),(int)(12*density),(int)(12*density),(int)(10*density));
        title.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
        LinearLayout rows=panel.findViewById(R.id.rows);
        String[] names={"Кино","YouTube","ВК Видео"};
        String[] packages={"mov.film","com.google.android.youtube.tv","com.vk.tv"};
        for(int i=0;i<names.length;i++){
            View row=getLayoutInflater().inflate(R.layout.item_input,rows,false);
            row.setFocusableInTouchMode(true);
            ((TextView)row.findViewById(R.id.title)).setText(names[i]);
            ImageView icon=row.findViewById(R.id.icon);
            try{icon.setImageDrawable(getPackageManager().getApplicationIcon(packages[i]));icon.setImageTintList(null);}
            catch(Exception ignored){icon.setImageResource(R.drawable.ic_app_open);}
            row.findViewById(R.id.dot).setVisibility(View.GONE);
            row.findViewById(R.id.status).setVisibility(View.GONE);
            String pkg=packages[i];row.setOnClickListener(v->dispatch(pkg,query));rows.addView(row);
        }
        hint=new TextView(this);hint.setText("Назад — отмена");
        hint.setTextSize(11);hint.setTextColor(android.graphics.Color.WHITE);
        hint.setTypeface(android.graphics.Typeface.create(getResources().getFont(R.font.outfit),android.graphics.Typeface.BOLD));
        hint.setFontVariationSettings("'wght' 700");
        hint.setPadding((int)(12*density),(int)(10*density),(int)(12*density),(int)(10*density));
        ((LinearLayout)panel).addView(hint);
        FrameLayout.LayoutParams placement=new FrameLayout.LayoutParams(Math.min((int)(320*density),getResources().getDisplayMetrics().widthPixels-(int)(48*density)),ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.CENTER);
        overlay.addView(panel,placement);setContentView(overlay);
        panel.setAlpha(0f);panel.setScaleX(.96f);panel.setScaleY(.96f);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        rows.getChildAt(0).post(()->rows.getChildAt(0).requestFocus());
        String destination=VoiceSearchQuery.destination(getIntent().getStringExtra("destination"));
        if(!destination.isEmpty())dispatch(destination,query);
    }
    private void dispatch(String pkg,String query) {
        if(dispatched)return;dispatched=true;
        hint.setText("Открываю поиск…   Назад — отмена");
        try{request=VoiceSearchRouter.open(this,pkg,query,()->{launched=true;finish();});}
        catch(Exception error){VoiceActions.message(this,"Не удалось открыть поиск приложения");finish();}
    }
    @Override protected void onPause(){super.onPause();if(dispatched&&!launched&&request!=null){request.cancel();finish();}}
    @Override protected void onDestroy(){if(!launched&&request!=null)request.cancel();super.onDestroy();}
}
