package com.gathin.tvhome;
import android.content.*;
public class VoiceBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))VoiceSettingsActivity.ensureStarted(context);}
}
