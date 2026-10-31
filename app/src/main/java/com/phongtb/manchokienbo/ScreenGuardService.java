package com.phongtb.manchokienbo;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Dịch vụ chạy nền:
 *  - Một cửa sổ 1 điểm ảnh nghe mọi lần chạm trên máy (ACTION_OUTSIDE) để biết máy có đang được dùng.
 *  - Giữ màn sáng trong lúc đếm, để yên đủ lâu thì phủ hoạt cảnh lên.
 *  - Chạm vào hoạt cảnh: tắt ngay, trở lại máy.
 *  - Hoạt cảnh chạy đủ thời gian: màn tối dần, nhả giữ sáng để hệ thống tắt màn như bình thường.
 */
public class ScreenGuardService extends Service {

    public static final String ACTION_STOP = "com.phongtb.manchokienbo.STOP";
    public static final String ACTION_PREVIEW = "com.phongtb.manchokienbo.PREVIEW";
    private static final String CHANNEL = "guard";
    private static final int NOTIF_ID = 7;

    private WindowManager wm;
    private PowerManager pm;
    private KeyguardManager km;
    private AudioManager am;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View watcher;
    private WindowManager.LayoutParams watcherLp;
    private boolean keepOn = true;

    private SceneView sceneView;
    private WindowManager.LayoutParams sceneLp;
    private boolean blackMode = false;
    private long sceneStart;
    private long lastActivity;
    private boolean previewPending = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            onTick();
            handler.postDelayed(this, 500);
        }
    };

    private final BroadcastReceiver screenRx = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                hideScene();
                blackMode = false;
                setKeepOn(false);
            } else {
                // Bật màn / mở khóa: tính lại từ đầu
                blackMode = false;
                lastActivity = SystemClock.uptimeMillis();
                onTick();
            }
        }
    };

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
        km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        lastActivity = SystemClock.uptimeMillis();

        startAsForeground();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        addWatcher();

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenRx, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(screenRx, f);
        }
        handler.post(tick);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String a = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(a)) {
            Prefs.setEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_PREVIEW.equals(a) && Settings.canDrawOverlays(this)) {
            previewPending = true;
            handler.postDelayed(() -> {
                previewPending = false;
                lastActivity = SystemClock.uptimeMillis();
                showScene();
            }, 700);
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screenRx); } catch (Exception ignored) {}
        hideScene();
        if (watcher != null) {
            try { wm.removeView(watcher); } catch (Exception ignored) {}
            watcher = null;
        }
        super.onDestroy();
    }

    // ---------------------------------------------------------------- thông báo

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, Prefs.t(this, "Screensaver running", "Màn chờ đang bật"),
                NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);

        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, ScreenGuardService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(Prefs.t(this, "Crawlscape is on", "Màn chờ đang bật"))
                .setContentText(Prefs.t(this, "Leave your phone idle and the critters come out", "Để yên máy là hoạt cảnh sẽ hiện ra"))
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, Prefs.t(this, "Turn off", "Tắt màn chờ"), stop).build())
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    // ---------------------------------------------------------------- cửa sổ canh chạm

    private void addWatcher() {
        watcher = new View(this);
        watcher.setOnTouchListener((v, e) -> {
            // đang có màn chờ thì để màn chờ tự xử lý cú chạm
            if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE && sceneView == null) onUserActivity();
            return false;
        });
        watcherLp = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        watcherLp.gravity = Gravity.TOP | Gravity.START;
        keepOn = true;
        try {
            wm.addView(watcher, watcherLp);
        } catch (Exception e) {
            watcher = null;
        }
    }

    private void setKeepOn(boolean on) {
        if (watcher == null || keepOn == on) return;
        keepOn = on;
        if (on) watcherLp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        else watcherLp.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        try { wm.updateViewLayout(watcher, watcherLp); } catch (Exception ignored) {}
    }

    private void onUserActivity() {
        lastActivity = SystemClock.uptimeMillis();
        if (sceneView != null) hideScene();
        if (blackMode) blackMode = false;
    }

    // ---------------------------------------------------------------- vòng lặp

    private void onTick() {
        if (!Prefs.enabled(this) && sceneView == null && !previewPending) {
            // Chỉ chạy để xem thử, xem xong thì thôi
            setKeepOn(false);
            stopSelf();
            return;
        }
        long now = SystemClock.uptimeMillis();

        boolean interactive = pm.isInteractive();
        boolean locked = km.isKeyguardLocked();

        // Đang ở màn khóa hoặc màn đã tắt: không giữ sáng, không hiện gì
        if (!interactive || locked) {
            if (sceneView != null) hideScene();
            setKeepOn(false);
            lastActivity = now;
            return;
        }

        setKeepOn(!blackMode);

        if (sceneView == null) {
            if (Prefs.skipVideo(this) && videoPlaying()) {
                lastActivity = now; // đang xem video: coi như đang dùng
                return;
            }
            if (Prefs.enabled(this) && now - lastActivity >= Prefs.idleSec(this) * 1000L) {
                showScene();
            }
        } else if (!blackMode) {
            int run = Prefs.runSec(this);
            if (run > 0 && now - sceneStart >= run * 1000L) goBlack();
        }
    }

    // ---------------------------------------------------------------- hoạt cảnh

    private void showScene() {
        if (sceneView != null || !Settings.canDrawOverlays(this)) return;
        final boolean dbl = Prefs.doubleTap(this);
        sceneView = new SceneView(this, Prefs.scene(this), Prefs.amount(this), dbl);
        lastTapTime = 0;
        sceneView.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                // cú chạm này không lọt xuống icon bên dưới
                onSceneTap(e.getX(), e.getY(), dbl);
            }
            return true;
        });

        sceneLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        sceneLp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 28) {
            sceneLp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            sceneLp.setFitInsetsTypes(0);
        }
        try {
            wm.addView(sceneView, sceneLp);
            sceneView.setAlpha(0f);
            sceneView.animate().alpha(1f).setDuration(700).start();
            sceneStart = SystemClock.uptimeMillis();
            blackMode = false;
        } catch (Exception e) {
            sceneView = null;
        }
    }

    private long lastTapTime;
    private float lastTapX, lastTapY;

    private void onSceneTap(float x, float y, boolean dbl) {
        if (!dbl) {
            sceneView.post(this::onUserActivity); // chạm 1 lần là mở
            return;
        }
        long now = SystemClock.uptimeMillis();
        float dp = getResources().getDisplayMetrics().density;
        if (now - lastTapTime < 400 && Math.hypot(x - lastTapX, y - lastTapY) < 90 * dp) {
            sceneView.post(this::onUserActivity); // chạm 2 lần liền: mở
            return;
        }
        lastTapTime = now;
        lastTapX = x;
        lastTapY = y;
        sceneStart = now; // đang chơi với con vật thì chưa tắt màn
        if (blackMode) wakeScene();
        else if (sceneView != null) sceneView.touch(x, y);
    }

    /** Màn đang tối mà chạm 1 cái: sáng lại, hoạt cảnh chạy tiếp. */
    private void wakeScene() {
        if (sceneView == null) return;
        blackMode = false;
        sceneView.unBlack();
        sceneLp.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        sceneLp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        try { wm.updateViewLayout(sceneView, sceneLp); } catch (Exception ignored) {}
        setKeepOn(true);
    }

    /**
     * Có đang xem video không: có âm thanh đang phát mà không phải loại nhạc / lời nói.
     * App nghe nhạc thường khai báo là nhạc nên màn chờ vẫn hiện.
     */
    private boolean videoPlaying() {
        if (!am.isMusicActive()) return false;
        try {
            for (AudioPlaybackConfiguration cfg : am.getActivePlaybackConfigurations()) {
                AudioAttributes a = cfg.getAudioAttributes();
                int usage = a.getUsage();
                if (usage != AudioAttributes.USAGE_MEDIA && usage != AudioAttributes.USAGE_GAME
                        && usage != AudioAttributes.USAGE_UNKNOWN) continue;
                int ct = a.getContentType();
                if (ct == AudioAttributes.CONTENT_TYPE_MUSIC || ct == AudioAttributes.CONTENT_TYPE_SPEECH) continue;
                return true; // phim, hoặc không rõ loại (thường là video)
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void hideScene() {
        if (sceneView == null) return;
        SceneView v = sceneView;
        sceneView = null;
        v.stop();
        try { wm.removeView(v); } catch (Exception ignored) {}
    }

    /** Hết giờ: tối dần rồi để hệ thống tắt màn. */
    private void goBlack() {
        if (sceneView == null) return;
        blackMode = true;
        sceneView.fadeToBlack();
        sceneLp.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        sceneLp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        try { wm.updateViewLayout(sceneView, sceneLp); } catch (Exception ignored) {}
        setKeepOn(false);
    }
}
