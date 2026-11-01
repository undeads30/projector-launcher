package com.gathin.tvhome;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
final class VoiceCommands {
    private static final Map<String,VoiceCommand> COMMANDS = new LinkedHashMap<>();
    static {
        add(VoiceCommand.YOUTUBE,"открой ютуб","включи ютуб","ютуб","открой youtube");
        add(VoiceCommand.VK,"открой вк видео","открой вк","включи вк видео");
        add(VoiceCommand.KINO,"открой кино","включи кино");
        add(VoiceCommand.BROWSER,"открой браузер","открой яндекс браузер");
        add(VoiceCommand.VOLUME_UP,"громче","сделай громче","прибавь громкость");
        add(VoiceCommand.VOLUME_DOWN,"тише","сделай тише","убавь громкость");
        add(VoiceCommand.MUTE,"выключи звук","отключи звук","без звука");
        add(VoiceCommand.UNMUTE,"включи звук","верни звук");
        add(VoiceCommand.HOME,"домой","главный экран","на главный экран");
        add(VoiceCommand.BACK,"назад","вернись назад");
        add(VoiceCommand.PAUSE,"пауза","поставь на паузу");
        add(VoiceCommand.PLAY,"продолжить","продолжи","продолжи воспроизведение");
        add(VoiceCommand.HDMI_1,"вход один","первый вход","переключи вход один","аш ди эм ай один","hdmi один");
        add(VoiceCommand.HDMI_2,"вход два","второй вход","переключи вход два","аш ди эм ай два","hdmi два");
        add(VoiceCommand.HDMI_3,"вход три","третий вход","переключи вход три","аш ди эм ай три","hdmi три");
        add(VoiceCommand.POWER_MENU,"выключи проектор","меню питания");
    }
    private static void add(VoiceCommand command,String... variants) {
        for(String phrase:variants) COMMANDS.put(phrase,command);
    }
    static VoiceCommand parse(String text) {
        if(text==null)return VoiceCommand.NONE;
        String normalized=text.toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("[.!?,]+$","").trim().replaceAll("\\s+"," ");
        VoiceCommand found=COMMANDS.get(normalized);
        return found==null?VoiceCommand.NONE:found;
    }
    static VoiceCommand accept(String text,double average,double minimum) {
        if(!Double.isFinite(average)||!Double.isFinite(minimum)||average<.8||minimum<.65)return VoiceCommand.NONE;
        return parse(text);
    }
    static List<String> phrases() {
        List<String> result=new ArrayList<>(COMMANDS.keySet());
        // Give negation its own acoustic paths; these phrases intentionally have no action.
        for(String phrase:COMMANDS.keySet())result.add("не "+phrase);
        java.util.Collections.addAll(result,"не делай тише","не делай громче","не открывай ютуб","не открывай кино",
                "не открывай браузер","не выключай звук","не включай звук","не переключай вход","не надо","отмена","нет");
        result.add("[unk]");return result;
    }
}
