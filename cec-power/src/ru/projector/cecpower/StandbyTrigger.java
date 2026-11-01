package ru.projector.cecpower;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
public final class StandbyTrigger {
    private final Pattern event,marker;
    private boolean armed,fired;
    private long last;
    public StandbyTrigger(int systemPid,int ownPid,String token) {
        event=Pattern.compile("^([0-9]+\\.[0-9]+)\\s+"+systemPid+"\\s+[0-9]+\\s+I\\s+PowerManagerService:\\s+Going to sleep due to HDMI standby \\(uid 1000\\)\\.\\.\\.$");
        marker=Pattern.compile("^[0-9]+\\.[0-9]+\\s+"+ownPid+"\\s+[0-9]+\\s+I\\s+CecPowerBridge:\\s+READY "+Pattern.quote(token)+"$");
    }
    public synchronized boolean isArmed(){return armed;}
    public synchronized boolean accept(String line,long wallNow,long now){
        if(marker.matcher(line.trim()).matches()){armed=true;return false;}
        Matcher match=event.matcher(line.trim());
        if(!armed || !match.matches())return false;
        double age=wallNow-Double.parseDouble(match.group(1))*1000.0;
        if(age>1500 || age< -500)return false;
        if(fired && now-last<15000)return false;
        fired=true; last=now; return true;
    }
}
