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

    public static final String[] SCENE_KEYS = {"random", "ants", "roach", "mouse", "fish", "snake", "butterfly", "firefly", "bubble"};
    public static final String[] SCENE_NAMES = {
            "🎲  Ngẫu nhiên mỗi lần",
            "🐜  Đàn kiến tha mồi",
            "🪳  Gián chạy",
            "🐭  Chuột rúc rích",
            "🐟  Hồ cá Koi",
            "🐍  Rắn bò",
            "🦋  Bướm bay",
            "✨  Đom đóm đêm",
            "🫧  Bong bóng xà phòng"};

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

    public static String scene(Context c) { return sp(c).getString("scene", "random"); }
    public static void setScene(Context c, String v) { sp(c).edit().putString("scene", v).apply(); }

    /** 0 = ít, 1 = vừa, 2 = nhiều */
    public static int amount(Context c) { return sp(c).getInt("amount", 1); }
    public static void setAmount(Context c, int v) { sp(c).edit().putInt("amount", v).apply(); }

    public static boolean skipMedia(Context c) { return sp(c).getBoolean("skipMedia", true); }
    public static void setSkipMedia(Context c, boolean v) { sp(c).edit().putBoolean("skipMedia", v).apply(); }

    public static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return 0;
    }

    public static String fmtSec(int s) {
        if (s == 0) return "Không tự tắt";
        if (s < 60) return s + " giây";
        if (s % 60 == 0) return (s / 60) + " phút";
        return (s / 60) + " phút " + (s % 60) + " giây";
    }
}
