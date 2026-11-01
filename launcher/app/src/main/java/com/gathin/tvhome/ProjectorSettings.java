package com.gathin.tvhome;

import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Device access only. All reads and writes are performed off the UI thread. */
final class ProjectorSettings {
    interface Reader { int get() throws Exception; }
    interface Writer { void set(int value) throws Exception; }
    static final class Control {
        final String label;
        final int min, max;
        final String[] choices;
        final Reader reader;
        final Writer writer;
        final boolean confirmOff;
        Control(String label, int min, int max, String[] choices, Reader reader, Writer writer) {
            this(label, min, max, choices, reader, writer, false);
        }
        Control(String label, int min, int max, String[] choices, Reader reader, Writer writer, boolean confirmOff) {
            this.label=label; this.min=min; this.max=max; this.choices=choices;
            this.reader=reader; this.writer=writer; this.confirmOff=confirmOff;
        }
        int read() throws Exception {
            int v=reader.get();
            if (!SettingsValue.valid(v,min,max)) throw new IllegalStateException("Значение вне диапазона");
            return v;
        }
        int write(int target) throws Exception {
            if (!SettingsValue.valid(target,min,max)) throw new IllegalArgumentException("Недопустимое значение");
            writer.set(target);
            // The projector MCU and radio adapters acknowledge changes asynchronously.
            int actual=read();
            for(int attempt=0; actual!=target && attempt<8; attempt++) {
                Thread.sleep(150);
                actual=read();
            }
            return actual;
        }
        String display(int value) { return choices==null ? Integer.toString(value) : choices[value-min]; }
    }
    private final Context context;
    private Object projector;
    private Class<?> projectorClass;
    ProjectorSettings(Context context) { this.context=context.getApplicationContext(); }
    private int projectorCall(String name, int... args) throws Exception {
        if(projector==null) {
            projectorClass=Class.forName("android.os.ProjectorManager");
            projector=context.getSystemService("asuprojector");
            if(projector==null) throw new IllegalStateException("Нет службы проектора");
        }
        Class<?>[] signature=new Class<?>[args.length]; Object[] values=new Object[args.length];
        for(int i=0;i<args.length;i++){signature[i]=int.class;values[i]=args[i];}
        Method method=projectorClass.getMethod(name,signature);
        return ((Number)method.invoke(projector,values)).intValue();
    }
    private Control image(String label, int type) {
        return new Control(label,0,100,null,()->{
            // This scale is verified only for Cronus Android 9, not arbitrary vendor firmware.
            if(!"Cronus".equals(Build.MODEL) || !"t982_ar301".equals(Build.DEVICE)
                    || !"1.8.150".equals(Build.DISPLAY) || Build.VERSION.SDK_INT!=28)
                throw new IllegalStateException("Модель не проверена");
            if(projectorCall("getTypeMax",type)!=100) throw new IllegalStateException("Неизвестный диапазон");
            return SettingsValue.cronusImageValue(projectorCall("getTypeValue",type));
        }, value->{
            if(projectorCall("setTypeValue",type,value)<0) throw new IllegalStateException("Проектор отклонил изменение");
            // Same MCU save commands as the stock image preference fragment.
            if(projectorCall("setTypeValue",17,9)<0 || projectorCall("setTypeValue",17,13)<0)
                throw new IllegalStateException("Не удалось сохранить настройку");
        });
    }
    List<Control> imageControls() {
        List<Control> out=new ArrayList<>();
        out.add(image("Яркость",6)); out.add(image("Контрастность",26)); out.add(image("Насыщенность",27));
        // Stock getMode/updatePrefenceRadio switch: 0 ceiling/front, 1 ceiling/rear,
        // 2 desktop/rear, 3 desktop/front. These are not the visual XML row order.
        out.add(new Control("Проекция",0,3,new String[]{"Спереди · потолок","Сзади · потолок","Сзади · стол","Спереди · стол"},
                ()->projectorCall("getDirection"),v->{
                    if(projectorCall("setDirection",v)<0 || projectorCall("setTypeValue",17,1)<0)
                        throw new IllegalStateException("Не удалось сохранить проекцию");
                }));
        return out;
    }
    List<Control> soundControls() {
        AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        List<Control> out=new ArrayList<>();
        out.add(new Control("Громкость",0,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),null,
                ()->audio.getStreamVolume(AudioManager.STREAM_MUSIC),v->audio.setStreamVolume(AudioManager.STREAM_MUSIC,v,0)));
        out.add(new Control("Без звука",0,1,new String[]{"Выкл.","Вкл."},
                ()->audio.isStreamMute(AudioManager.STREAM_MUSIC)?1:0,
                v->audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,v==1?AudioManager.ADJUST_MUTE:AudioManager.ADJUST_UNMUTE,0)));
        return out;
    }
    private BluetoothAdapter bluetooth() {
        if(Build.VERSION.SDK_INT>=31) throw new IllegalStateException("Требуется разрешение Bluetooth");
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null)throw new IllegalStateException("Нет Bluetooth");
        return adapter;
    }
    List<Control> networkControls() {
        List<Control> out=new ArrayList<>();
        WifiManager wifi=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        out.add(new Control("Wi-Fi",0,1,new String[]{"Выкл.","Вкл."},
                ()->{if(wifi==null)throw new IllegalStateException("Нет Wi-Fi");return wifi.isWifiEnabled()?1:0;},
                v->{if(Build.VERSION.SDK_INT>=29 || wifi==null || !wifi.setWifiEnabled(v==1))throw new IllegalStateException("Android запретил изменение Wi-Fi");},true));
        out.add(new Control("Bluetooth",0,1,new String[]{"Выкл.","Вкл."},()->bluetooth().isEnabled()?1:0,
                v->{boolean ok=v==1?bluetooth().enable():bluetooth().disable();if(!ok)throw new IllegalStateException("Bluetooth отклонил изменение");},true));
        return out;
    }
    String pairedDevices() {
        try {
            if(!bluetooth().isEnabled())return "Bluetooth выключен";
            StringBuilder names=new StringBuilder();
            for(BluetoothDevice device:bluetooth().getBondedDevices()) {
                if(names.length()>0)names.append(", ");
                String name=device.getName();names.append(name==null?"Устройство Bluetooth":name);
            }
            return names.length()==0?"Нет сохранённых устройств":"Сохранённые устройства: "+names;
        } catch(Exception unavailable){return "Список Bluetooth недоступен";}
    }
    List<Control> launcherControls() {
        List<Control> out=new ArrayList<>();
        out.add(new Control("Голосовые команды",0,1,new String[]{"Выкл.","Вкл."},
                ()->context.getSharedPreferences("voice_control",0).getBoolean("running",false)
                        && context.getSharedPreferences("voice_control",0).getBoolean("armed",false)?1:0,
                v->{
                    if(v==1 && (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=0
                            || context.checkSelfPermission(android.Manifest.permission.READ_LOGS)!=0))
                        throw new IllegalStateException("Нет разрешений микрофона и голосового управления");
                    VoiceSettingsActivity.configure(context,v==1,v==1);
                }));
        return out;
    }
}
