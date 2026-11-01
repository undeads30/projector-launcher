package com.gathin.tvhome;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Private same-UID bridge from :voice; no persistent foreground-app history. */
public final class VoiceContextProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        Bundle result = new Bundle();
        if ("foreground".equals(method)) result.putString("package", VoiceAccessService.foreground());
        return result;
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){return null;}
    @Override public int delete(Uri u,String s,String[] a){return 0;}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
