package com.gathin.tvhome;

import android.content.*;
import android.net.Uri;
import android.os.*;

final class VoiceSearchRouter {
    private static VoiceSearchRequest current;
    private static boolean kinoSeen;
    private static boolean kinoPending;
    static void windowChanged(String pkg){
        if(!kinoPending||current==null||!current.active())return;
        if("mov.film".equals(pkg))kinoSeen=true;
        else if(kinoSeen && !pkg.contains("inputmethod") && !pkg.contains("rukeyboard"))current.cancel();
    }
    static VoiceSearchRequest open(Context context,String pkg,String query,Runnable launched) {
        if(VoiceSearchQuery.destination(pkg).isEmpty())return null;
        Context app=context.getApplicationContext();
        VoiceSearchRequest request=VoiceSearchRequest.replace(current);current=request;kinoSeen=false;kinoPending="mov.film".equals(pkg);
        Intent target;
        if("mov.film".equals(pkg)) {
            target=new Intent().setClassName(pkg,"mov.film.StartActivity")
                    .putExtra("section","@поиск").putExtra("caption","Поиск");
        } else if("com.vk.tv".equals(pkg)) {
            target=new Intent(Intent.ACTION_VIEW,Uri.parse("vkvideo://video/search").buildUpon()
                    .appendQueryParameter("query",query).build())
                    .setClassName(pkg,"com.vk.tv.presentation.base.links.TvLinkRedirectActivity");
        } else {
            target=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/results").buildUpon()
                    .appendQueryParameter("search_query",query).build())
                    .setClassName(pkg,"com.google.android.apps.youtube.tv.activity.ShellActivity");
        }
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(!"mov.film".equals(pkg)) {
            int token=YoutubeVpnLaunch.search(app,target,request::claim,launched);
            request.onCancel(()->YoutubeVpnLaunch.cancelSearch(token));
            return request;
        }
        YoutubeVpnLaunch.stop(app);
        launched.run();
        app.startActivity(target);
        Handler main=new Handler(Looper.getMainLooper());
        main.postDelayed(new Runnable(){int attempts=30;boolean seen;
            @Override public void run(){
                if(!request.active())return;
                String foreground=VoiceAccessService.activePackage();
                if("mov.film".equals(foreground)){
                    seen=true;
                    if(VoiceAccessService.setKinoQuery(query)){request.claim();return;}
                    // Never keep a query waiting after the search screen was dismissed within Kino.
                    request.cancel();VoiceActions.message(app,"Откройте поиск Кино и повторите команду");return;
                } else if(seen||!foreground.isEmpty()) {request.cancel();return;}
                if(--attempts>0)main.postDelayed(this,250);
                else {request.cancel();VoiceActions.message(app,"Откройте поиск Кино и повторите команду");}
            }
        },350);
        return request;
    }
}
