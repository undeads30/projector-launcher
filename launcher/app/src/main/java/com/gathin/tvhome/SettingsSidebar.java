package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One navigable, bounded panel; vendor access never blocks remote navigation. */
final class SettingsSidebar {
    private final Activity activity;
    private final Runnable wifiAction;
    private final ProjectorSettings backend;
    private final Dialog dialog;
    private final LinearLayout rows;
    private final TextView title, subtitle, status;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"settings-device");t.setDaemon(true);return t;});
    private String page="";
    private volatile int generation;
    private volatile boolean disposed;
    private final List<ControlRow> activeControls=new ArrayList<>();
    private AlertDialog confirmation;
    private static final int WHITE=Color.WHITE, MUTED=0xffb9c0ce, ACCENT=0xffb9d1ef;

    SettingsSidebar(Activity activity,Runnable wifiAction) {
        this.activity=activity;this.wifiAction=wifiAction;backend=new ProjectorSettings(activity);
        dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22),dp(16),dp(22),dp(14));
        GradientDrawable background=new GradientDrawable();background.setColor(0xf21a1e28);background.setCornerRadii(new float[]{dp(20),dp(20),0,0,0,0,dp(20),dp(20)});
        panel.setBackground(background);
        title=text("Настройки",25,WHITE);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));panel.addView(title);
        subtitle=text("",12,MUTED);subtitle.setPadding(0,dp(5),0,dp(12));panel.addView(subtitle);
        ScrollView scroll=new ScrollView(activity);scroll.setClipToPadding(false);
        rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        status=text("",12,MUTED);status.setPadding(0,dp(8),0,0);panel.addView(status);
        TextView hint=text("↑↓ выбор   ←→ значение   ОК открыть   Назад",11,MUTED);hint.setPadding(0,dp(7),0,0);panel.addView(hint);
        dialog.setContentView(panel);
        dialog.setOnKeyListener((d,key,event)->{
            if(key!=KeyEvent.KEYCODE_BACK)return false;
            if(event.getAction()==KeyEvent.ACTION_UP){if(page.isEmpty())dismiss();else showPage("");}return true;
        });
        dialog.setOnDismissListener(d->{
            disposed=true;generation++;worker.shutdownNow();main.removeCallbacksAndMessages(null);
            if(confirmation!=null)confirmation.dismiss();
            View anchor=activity.findViewById(R.id.btnSettings);if(anchor!=null)anchor.requestFocus();
        });
    }
    void show() {
        dialog.show();Window w=dialog.getWindow();
        if(w!=null){
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.12f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            w.setLayout(activity.getResources().getDisplayMetrics().widthPixels*45/100,-1);
        }
        showPage("");
    }
    void dismiss(){dialog.dismiss();}
    boolean isShowing(){return dialog.isShowing();}
    View focusTarget(){View focused=dialog.getCurrentFocus();return focused!=null?focused:rows.getChildAt(0);}
    private void showPage(String next) {
        String previous=page;page=next;generation++;activeControls.clear();rows.removeAllViews();status.setText("");
        title.setText(next.isEmpty()?"Настройки":next);
        subtitle.setText(next.isEmpty()?"Управление проектором":"Настройки  /  "+next);
        if(next.isEmpty()) {
            for(String section:new String[]{"Изображение","Звук","Сеть и Bluetooth","Лаунчер","Об устройстве"})
                action(section,"›",()->showPage(section));
            action("Системные настройки","↗",this::openSystem);
            action("Закрыть","×",this::dismiss);
        } else {
            action("Назад к разделам","‹",()->showPage(""));
            switch(next) {
                case "Изображение": controls(backend.imageControls());note("Резкость, цветовая температура, масштаб и трапеция — в системных настройках.");break;
                case "Звук":controls(backend.soundControls());break;
                case "Сеть и Bluetooth":
                    controls(backend.networkControls());
                    action("Доступные сети Wi-Fi","›",wifiAction);
                    note("Подключение к новой сети — в списке Wi-Fi. Сопряжение новых Bluetooth-устройств — в системных настройках.");
                    int networkGeneration=generation;
                    worker.execute(()->{String devices=backend.pairedDevices();main.post(()->{if(!disposed&&generation==networkGeneration)note(devices);});});
                    break;
                case "Лаунчер":controls(backend.launcherControls());note("Удержание кнопки «Настройки» на главном экране открывает управление карточками.");break;
                case "Об устройстве":
                    note("Модель: "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+"\nСборка: "+Build.DISPLAY);
                    StatFs disk=new StatFs(activity.getFilesDir().getAbsolutePath());
                    note("Свободно: "+android.text.format.Formatter.formatFileSize(activity,disk.getAvailableBytes()));
                    try{note("Лаунчер: "+activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName);}catch(Exception ignored){}
                    break;
            }
            action("Системные настройки","↗",this::openSystem);
        }
        // Returning from a section restores its root row, rather than resetting to the top.
        int focus=next.isEmpty()?indexOfSection(previous):Math.min(1,rows.getChildCount()-1);
        if(rows.getChildCount()>0 && !rows.getChildAt(Math.max(0,focus)).requestFocus())
            rows.getChildAt(0).requestFocus();
    }
    private int indexOfSection(String name){
        String[] sections={"Изображение","Звук","Сеть и Bluetooth","Лаунчер","Об устройстве"};
        for(int i=0;i<sections.length;i++)if(sections[i].equals(name))return i;return 0;
    }
    private void controls(List<ProjectorSettings.Control> controls){for(ProjectorSettings.Control control:controls)activeControls.add(new ControlRow(control));}
    private final class ControlRow {
        final ProjectorSettings.Control control;
        final LinearLayout root;
        final TextView value;
        final SeekBar slider;
        final int ownGeneration=generation;
        Integer actual;
        boolean busy;
        ControlRow(ProjectorSettings.Control control){
            this.control=control;root=row();
            LinearLayout heading=new LinearLayout(activity);heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=text(control.label,16,WHITE);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            value=text("…",14,ACCENT);value.setGravity(Gravity.RIGHT);heading.addView(value);root.addView(heading);
            if(control.choices==null){
                slider=new SeekBar(activity);slider.setMax(control.max-control.min);slider.setFocusable(false);slider.setClickable(false);slider.setEnabled(false);
                // This is a remote-operated indicator; touch must not move it without a device write.
                slider.setOnTouchListener((v,event)->true);
                slider.setProgressTintList(ColorStateList.valueOf(ACCENT));slider.setThumbTintList(ColorStateList.valueOf(ACCENT));
                root.addView(slider,new LinearLayout.LayoutParams(-1,dp(25)));
            }else slider=null;
            root.setOnKeyListener((v,key,event)->{
                if(key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;
                if(event.getAction()==KeyEvent.ACTION_DOWN)change(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);
                return true;
            });
            root.setOnClickListener(v->{
                if(actual==null){status.setText("Этот параметр недоступен на текущей прошивке или не хватает разрешений.");return;}
                if(control.choices!=null && control.choices.length>2){
                    confirmation=new AlertDialog.Builder(activity).setTitle(control.label)
                            .setSingleChoiceItems(control.choices,actual-control.min,(d,which)->{d.dismiss();submit(control.min+which);})
                            .setNegativeButton("Отмена",null).create();confirmation.show();
                }else if(control.choices!=null)change(actual==control.max?-1:1);
                else status.setText("Меняйте значение стрелками ← и →");
            });
            load(null);
        }
        void change(int delta){
            if(busy||actual==null)return;
            int target=SettingsValue.step(actual,delta,control.min,control.max);
            if(target==actual)return;
            if(control.confirmOff&&target==0){
                if(confirmation!=null&&confirmation.isShowing())return;
                confirmation=new AlertDialog.Builder(activity).setTitle("Выключить "+control.label+"?")
                        .setMessage(control.label.equals("Bluetooth")?"Bluetooth-пульт и другие устройства потеряют связь.":"Текущее сетевое подключение будет разорвано.")
                        .setPositiveButton("Выключить",(d,w)->submit(target)).setNegativeButton("Отмена",null).create();confirmation.show();
            }else submit(target);
        }
        void submit(int target){if(!busy&&actual!=null)load(target);}
        void load(Integer target){
            busy=true;value.setText(target==null?"…":"Применение…");
            Runnable timeout=()->{
                if(disposed||generation!=ownGeneration||!busy)return;
                actual=null;value.setText("Нет ответа");root.setContentDescription(control.label+": нет ответа");
                status.setText("Служба настроек не отвечает. Можно закрыть панель и открыть её снова.");
                // Keep this row busy until its pending operation returns; never queue more writes.
            };
            main.postDelayed(timeout,3500);
            worker.execute(()->{
                if(disposed||generation!=ownGeneration)return;
                Integer result=null;String error=null;
                try {result=target==null?control.read():control.write(target);
                    if(target!=null&&!SettingsValue.confirmed(target,result,control.min,control.max))error="Прошивка не подтвердила изменение. Показано текущее значение.";
                }catch(Exception failure){
                    error="Не удалось "+(target==null?"прочитать":"изменить")+" «"+control.label+"».";
                    if(target!=null)try{result=control.read();}catch(Exception ignored){}
                }
                final Integer reading=result;final String message=error;
                main.post(()->{
                    main.removeCallbacks(timeout);
                    if(disposed||generation!=ownGeneration)return;
                    busy=false;actual=reading;
                    value.setText(reading==null?"Недоступно":control.display(reading));
                    root.setContentDescription(control.label+": "+value.getText());
                    if(slider!=null){slider.setEnabled(reading!=null);slider.setProgress(reading==null?0:reading-control.min);}
                    if(message!=null)status.setText(message);else if(target!=null)status.setText("Изменено: "+control.label);
                    if(target!=null&&page.equals("Звук"))
                        for(ControlRow related:activeControls)if(related!=this&&!related.busy)related.load(null);
                });
            });
        }
    }
    private void openSystem(){
        try{activity.startActivity(new Intent().setComponent(new ComponentName("com.android.settings","com.asu.settings.TvMenuActivity")));}
        catch(Exception absent){status.setText("Системные настройки недоступны");}
    }
    private LinearLayout row(){
        LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setFocusable(true);row.setFocusableInTouchMode(true);row.setClickable(true);
        StateListDrawable background=new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused},shape(0xff3d4b60));
        background.addState(new int[]{android.R.attr.state_pressed},shape(0xff3d4b60));
        background.addState(new int[]{},shape(0x222f394a));row.setBackground(background);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(6);rows.addView(row,params);return row;
    }
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(9));return d;}
    private void action(String label,String mark,Runnable action){
        LinearLayout row=row();TextView text=text(label+"   "+mark,16,WHITE);row.addView(text);
        row.setContentDescription(label);row.setOnClickListener(v->action.run());
    }
    private void note(String message){TextView note=text(message,12,MUTED);note.setPadding(dp(5),dp(8),dp(5),dp(12));rows.addView(note);}
    private TextView text(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextColor(color);t.setTextSize(size);return t;}
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
