package com.phongtb.manchokienbo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** Khởi động lại máy thì tự bật lại màn chờ (nếu đang bật). */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (Prefs.enabled(c) && Settings.canDrawOverlays(c)) {
            try {
                c.startForegroundService(new Intent(c, ScreenGuardService.class));
            } catch (Exception ignored) {
            }
        }
    }
}
