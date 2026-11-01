package com.gathin.tvhome;
import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.media.AudioManager;
import android.media.session.*;
import android.media.tv.*;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.util.List;
final class VoiceActions {
    static void message(Context c,String text){new Handler(Looper.getMainLooper()).post(()->Toast.makeText(c,text,Toast.LENGTH_SHORT).show());}
    static void execute(Context context,VoiceCommand command) {
        switch(command) {
            case YOUTUBE:open(context,"com.google.android.youtube.tv");return;
            case VK:open(context,"com.vk.tv");return;
            case KINO:open(context,"mov.film");return;
            case BROWSER:open(context,"com.yandex.browser.tv");return;
            case VOLUME_UP:volume(context,AudioManager.ADJUST_RAISE);return;
            case VOLUME_DOWN:volume(context,AudioManager.ADJUST_LOWER);return;
            case MUTE:volume(context,AudioManager.ADJUST_MUTE);return;
            case UNMUTE:volume(context,AudioManager.ADJUST_UNMUTE);return;
            case HOME:global(context,AccessibilityService.GLOBAL_ACTION_HOME);return;
            case BACK:global(context,AccessibilityService.GLOBAL_ACTION_BACK);return;
            case POWER_MENU:global(context,AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);return;
            case PAUSE:media(context,false);return;
            case PLAY:media(context,true);return;
            case HDMI_1:input(context,1);return;
            case HDMI_2:input(context,2);return;
            case HDMI_3:input(context,3);return;
            default:break;
        }
    }
    private static void open(Context c,String pkg) {
        Intent target=c.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if(target==null)target=c.getPackageManager().getLaunchIntentForPackage(pkg);
        if(target==null){message(c,"Приложение не найдено");return;}
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if(VpnLaunchPolicy.requiresVpn(pkg))YoutubeVpnLaunch.open(c,target);
        else {if("mov.film".equals(pkg))YoutubeVpnLaunch.stop(c);c.startActivity(target);}
    }
    private static void volume(Context c,int direction){((AudioManager)c.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC,direction,AudioManager.FLAG_SHOW_UI);}
    private static void global(Context c,int action){if(!VoiceAccessService.action(action))message(c,"Включите службу «Голосовые команды» в специальных возможностях");}
    private static void media(Context c,boolean play) {
        MediaSessionManager manager=(MediaSessionManager)c.getSystemService(Context.MEDIA_SESSION_SERVICE);
        List<MediaController> sessions=manager.getActiveSessions(new ComponentName(c,VpnStatusService.class));
        long required=play?PlaybackState.ACTION_PLAY:PlaybackState.ACTION_PAUSE;
        for(MediaController controller:sessions){PlaybackState state=controller.getPlaybackState();
            if(state!=null&&(state.getActions()&required)!=0){if(play)controller.getTransportControls().play();else controller.getTransportControls().pause();return;}}
        message(c,"Приложение не поддерживает эту команду воспроизведения");
    }
    private static void input(Context c,int physical) {
        int firmware=physical==1?3:physical==3?1:2;
        TvInputManager manager=(TvInputManager)c.getSystemService(Context.TV_INPUT_SERVICE);
        String needle="Hdmi"+firmware+"InputService";
        for(TvInputInfo info:manager.getTvInputList())if(info.isPassthroughInput()&&info.getId().contains(needle)) {
            Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.buildChannelUriForPassthroughInput(info.getId()),"vnd.android.cursor.item/channel").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(view);return;
        }
        message(c,"Вход HDMI не найден");
    }
}
