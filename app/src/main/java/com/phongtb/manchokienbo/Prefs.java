package com.phongtb.manchokienbo;

import android.content.Context;
import android.content.SharedPreferences;

/** Lưu cài đặt của app. */
public final class Prefs {
    private static final String FILE = "settings";

    // Các mốc chọn trên thanh kéo
    public static final int[] IDLE_CHOICES = {10, 15, 20, 30, 45, 60, 90, 120, 180, 300};
    /** 0 = không tự tắt màn. */
    public static final int[] RUN_CHOICES = {15, 30, 45, 60, 90, 120, 180, 300, 600, 0};

    public static final String[] SCENE_KEYS = {"random", "ants", "roach", "mouse", "fish", "snake",
            "chase", "butterfly", "firefly", "bubble", "fog", "crack"};
    private static final String[] SCENE_VI = {
            "🎲  Ngẫu nhiên mỗi lần",
            "🐜  Đàn kiến tha mồi",
            "🪳  Gián chạy",
            "🐭  Chuột xám & Hamster",
            "🐟  Hồ cá Koi",
            "🐍  Rắn bò",
            "🐱  Mèo đuổi chuột",
            "🦋  Bướm bay",
            "✨  Đom đóm đêm",
            "🫧  Bong bóng xà phòng",
            "🌫️  Sương mù",
            "💥  Vỡ màn hình"};
    private static final String[] SCENE_EN = {
            "🎲  Surprise me",
            "🐜  Marching ants",
            "🪳  Scurrying cockroaches",
            "🐭  Mice & hamsters",
            "🐟  Koi pond",
            "🐍  Slithering snakes",
            "🐱  Cat chases mouse",
            "🦋  Butterflies",
            "✨  Fireflies at night",
            "🫧  Soap bubbles",
            "🌫️  Drifting fog",
            "💥  Cracked screen"};

    public static String sceneName(Context c, int i) { return english(c) ? SCENE_EN[i] : SCENE_VI[i]; }

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) { return sp(c).getBoolean("enabled", false); }
    public static void setEnabled(Context c, boolean v) { sp(c).edit().putBoolean("enabled", v).apply(); }

    public static int idleSec(Context c) { return sp(c).getInt("idle", 30); }
    public static void setIdleSec(Context c, int v) { sp(c).edit().putInt("idle", v).apply(); }

    public static int runSec(Context c) { return sp(c).getInt("run", 60); }
    public static void setRunSec(Context c, int v) { sp(c).edit().putInt("run", v).apply(); }

    public static String scene(Context c) {
        String k = sp(c).getString("scene", "random");
        return "hamster".equals(k) ? "mouse" : k; // bản cũ có màn hamster riêng
    }
    public static void setScene(Context c, String v) { sp(c).edit().putString("scene", v).apply(); }

    /** 0 = ít, 1 = vừa, 2 = nhiều */
    public static int amount(Context c) { return sp(c).getInt("amount", 1); }
    public static void setAmount(Context c, int v) { sp(c).edit().putInt("amount", v).apply(); }

    /** Không hiện khi đang xem video (nghe nhạc vẫn hiện) */
    public static boolean skipVideo(Context c) { return sp(c).getBoolean("skipVideo", true); }
    public static void setSkipVideo(Context c, boolean v) { sp(c).edit().putBoolean("skipVideo", v).apply(); }

    /** true = chạm 1 lần trêu con vật, chạm 2 lần liền mới mở; false = chạm 1 lần là mở */
    public static boolean doubleTap(Context c) { return sp(c).getBoolean("doubleTap", true); }
    public static void setDoubleTap(Context c, boolean v) { sp(c).edit().putBoolean("doubleTap", v).apply(); }

    public static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return 0;
    }

    public static boolean step1Done(Context c) { return sp(c).getBoolean("step1", false); }
    public static void setStep1Done(Context c, boolean v) { sp(c).edit().putBoolean("step1", v).apply(); }
    public static boolean notifAsked(Context c) { return sp(c).getBoolean("notifAsked", false); }
    public static void setNotifAsked(Context c, boolean v) { sp(c).edit().putBoolean("notifAsked", v).apply(); }
    public static boolean setupFinished(Context c) { return sp(c).getBoolean("setupDone", false); }
    public static void setSetupFinished(Context c, boolean v) { sp(c).edit().putBoolean("setupDone", v).apply(); }

    /** Ngôn ngữ: mặc định tiếng Anh */
    public static boolean english(Context c) { return sp(c).getBoolean("english", true); }
    public static void setEnglish(Context c, boolean v) { sp(c).edit().putBoolean("english", v).apply(); }

    /** Chọn chữ theo ngôn ngữ đang dùng */
    public static String t(Context c, String en, String vi) { return english(c) ? en : vi; }

    public static String fmtSec(Context c, int s) {
        boolean en = english(c);
        if (s == 0) return en ? "Never" : "Không tự tắt";
        if (s < 60) return s + (en ? " sec" : " giây");
        if (s % 60 == 0) return (s / 60) + (en ? " min" : " phút");
        return (s / 60) + (en ? " min " : " phút ") + (s % 60) + (en ? " sec" : " giây");
    }
}
