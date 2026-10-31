package com.phongtb.manchokienbo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Màn cài đặt. Giao diện dựng bằng code cho gọn. */
public class MainActivity extends Activity {

    private static final int ACCENT = 0xFF8BC34A;
    private static final int BG = 0xFF0F1512;
    private static final int CARD = 0xFF1A2420;
    private static final int TEXT = 0xFFECEFEA;
    private static final int SUB = 0xFF9EAAA2;

    private float dp;
    private Switch toggle;
    private FrameLayout previewBox;
    private SceneView preview;
    private boolean pendingEnable = false;
    private boolean pendingPreview = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        final LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        final int pad = px(18);
        col.setPadding(pad, pad, pad, pad);
        scroll.addView(col);

        scroll.setOnApplyWindowInsetsListener((v, ins) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = ins.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = ins.getSystemWindowInsetTop();
                bottom = ins.getSystemWindowInsetBottom();
            }
            col.setPadding(pad, pad + top, pad, pad + bottom);
            return ins;
        });

        // ---- tiêu đề
        TextView title = text("Crawlscape", 28, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        TextView sub = text(L("Leave your phone idle for a moment and little critters crawl across your screen, just like a classic screensaver.",
                "Để yên máy một lúc, con vật sẽ bò ra phủ lên màn hình như màn chờ của Windows."), 14, SUB);
        sub.setPadding(0, px(6), 0, px(14));
        col.addView(sub);

        // ---- ngôn ngữ (trên cùng)
        LinearLayout lg = card(col);
        lg.addView(header(L("Language", "Ngôn ngữ")));
        RadioGroup langGroup = new RadioGroup(this);
        langGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton en = radio("English");
        en.setId(4000);
        en.setPadding(0, 0, px(24), 0);
        RadioButton vi = radio("Tiếng Việt");
        vi.setId(4001);
        langGroup.addView(en);
        langGroup.addView(vi);
        if (Prefs.english(this)) en.setChecked(true); else vi.setChecked(true);
        langGroup.setOnCheckedChangeListener((g, id) -> {
            boolean toEn = id == 4000;
            if (toEn != Prefs.english(this)) {
                Prefs.setEnglish(this, toEn);
                recreate();
            }
        });
        lg.addView(langGroup);

        // ---- khung xem trước
        previewBox = new FrameLayout(this);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(0xFF24433A);
        pbg.setCornerRadius(px(18));
        previewBox.setBackground(pbg);
        previewBox.setClipToOutline(true);
        previewBox.addView(new IconsBg(this), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, px(220));
        plp.setMargins(0, px(14), 0, 0);
        col.addView(previewBox, plp);
        rebuildPreview();

        // ---- công tắc
        LinearLayout tg = card(col);
        toggle = new Switch(this);
        toggle.setText(L("Screensaver on", "Bật màn chờ"));
        toggle.setTextSize(18);
        toggle.setTextColor(TEXT);
        toggle.setTypeface(Typeface.DEFAULT_BOLD);
        toggle.setThumbTintList(ColorStateList.valueOf(ACCENT));
        tg.addView(toggle);
        TextView tgSub = text(L("Runs in the background with a small notification to turn it off quickly.",
                "Khi bật, app chạy nền và có một thông báo nhỏ để tắt nhanh."), 13, SUB);
        tgSub.setPadding(0, px(4), 0, 0);
        tg.addView(tgSub);

        // ---- chọn hoạt cảnh
        LinearLayout sc = card(col);
        sc.addView(header(L("Scene", "Hoạt cảnh")));
        RadioGroup sceneGroup = new RadioGroup(this);
        String cur = Prefs.scene(this);
        for (int i = 0; i < Prefs.SCENE_KEYS.length; i++) {
            RadioButton rb = radio(Prefs.sceneName(this, i));
            rb.setId(1000 + i);
            sceneGroup.addView(rb);
            if (Prefs.SCENE_KEYS[i].equals(cur)) rb.setChecked(true);
        }
        sceneGroup.setOnCheckedChangeListener((g, id) -> {
            Prefs.setScene(this, Prefs.SCENE_KEYS[id - 1000]);
            rebuildPreview();
        });
        sc.addView(sceneGroup);

        sc.addView(header(L("Amount", "Số lượng")));
        RadioGroup amountGroup = new RadioGroup(this);
        amountGroup.setOrientation(RadioGroup.HORIZONTAL);
        String[] amounts = {L("Few", "Ít"), L("Normal", "Vừa"), L("Lots", "Nhiều")};
        for (int i = 0; i < 3; i++) {
            RadioButton rb = radio(amounts[i]);
            rb.setId(2000 + i);
            rb.setPadding(0, 0, px(16), 0);
            amountGroup.addView(rb);
            if (Prefs.amount(this) == i) rb.setChecked(true);
        }
        amountGroup.setOnCheckedChangeListener((g, id) -> {
            Prefs.setAmount(this, id - 2000);
            rebuildPreview();
        });
        sc.addView(amountGroup);

        // ---- thời gian
        final String idleTxt = L("Start after idle for:  ", "Để yên bao lâu thì hiện:  ");
        final String runTxt = L("Turn screen off after:  ", "Hoạt cảnh chạy bao lâu rồi tắt màn:  ");
        LinearLayout tm = card(col);
        final TextView idleLabel = header("");
        tm.addView(idleLabel);
        SeekBar idleBar = seek(Prefs.IDLE_CHOICES.length - 1,
                Prefs.indexOf(Prefs.IDLE_CHOICES, Prefs.idleSec(this)));
        tm.addView(idleBar);
        final TextView runLabel = header("");
        tm.addView(runLabel);
        SeekBar runBar = seek(Prefs.RUN_CHOICES.length - 1,
                Prefs.indexOf(Prefs.RUN_CHOICES, Prefs.runSec(this)));
        tm.addView(runBar);
        idleLabel.setText(idleTxt + Prefs.fmtSec(this, Prefs.idleSec(this)));
        runLabel.setText(runTxt + Prefs.fmtSec(this, Prefs.runSec(this)));
        idleBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                Prefs.setIdleSec(MainActivity.this, Prefs.IDLE_CHOICES[p]);
                idleLabel.setText(idleTxt + Prefs.fmtSec(MainActivity.this, Prefs.IDLE_CHOICES[p]));
            }
        });
        runBar.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                Prefs.setRunSec(MainActivity.this, Prefs.RUN_CHOICES[p]);
                runLabel.setText(runTxt + Prefs.fmtSec(MainActivity.this, Prefs.RUN_CHOICES[p]));
            }
        });

        CheckBox media = new CheckBox(this);
        media.setText(L("Don't show while watching videos (music is fine)",
                "Không hiện khi đang xem video (nghe nhạc vẫn hiện)"));
        media.setTextColor(TEXT);
        media.setButtonTintList(ColorStateList.valueOf(ACCENT));
        media.setChecked(Prefs.skipVideo(this));
        media.setOnCheckedChangeListener((v, c) -> Prefs.setSkipVideo(this, c));
        media.setPadding(0, px(8), 0, 0);
        tm.addView(media);

        // ---- kiểu chạm
        LinearLayout tp = card(col);
        tp.addView(header(L("When you touch the screensaver", "Khi chạm vào màn chờ")));
        RadioGroup tapGroup = new RadioGroup(this);
        RadioButton tap1 = radio(L("Single tap: unlock right away", "Chạm 1 lần là mở máy luôn"));
        tap1.setId(3000);
        RadioButton tap2 = radio(L("Single tap: tease the critters (they startle, bubbles pop...)\nDouble tap: unlock",
                "Chạm 1 lần: trêu con vật (chúng giật mình chạy, bóng vỡ...)\nChạm 2 lần liền: mở máy"));
        tap2.setId(3001);
        tapGroup.addView(tap1);
        tapGroup.addView(tap2);
        if (Prefs.doubleTap(this)) tap2.setChecked(true); else tap1.setChecked(true);
        tapGroup.setOnCheckedChangeListener((g, id) -> Prefs.setDoubleTap(this, id == 3001));
        tp.addView(tapGroup);
        TextView tpSub = text(L("You can also tap the preview above to try it.",
                "Ở khung xem trước phía trên cũng chạm thử được."), 13, SUB);
        tpSub.setPadding(0, px(4), 0, 0);
        tp.addView(tpSub);

        // ---- nút
        Button try1 = button(L("Preview full screen", "Xem thử toàn màn hình"), true);
        try1.setOnClickListener(v -> startPreview());
        col.addView(try1, buttonLp());

        // ---- lưu ý
        LinearLayout nt = card(col);
        nt.addView(header(L("Tips", "Lưu ý")));
        nt.addView(text(L(
                "• The first time, allow \"Display over other apps\".\n"
                        + "• On Xiaomi, Oppo, Vivo or Samsung phones, allow \"Autostart\" and set battery to \"Unrestricted\" so the app isn't killed in the background.\n"
                        + "• Set your phone's screen timeout short (15–30 sec) so the screen turns off right after the scene ends.\n"
                        + "• Nothing shows on the lock screen; it only runs after you unlock.\n"
                        + "• Home, Back and gesture navigation keep working as usual.",
                "• Lần đầu cần cho phép \"Hiển thị trên ứng dụng khác\".\n"
                        + "• Máy Xiaomi, Oppo, Vivo, Samsung: cho app \"Tự khởi chạy\" và chọn pin \"Không hạn chế\" để không bị tắt ngầm.\n"
                        + "• Nên đặt thời gian tắt màn của máy ngắn (15–30 giây) để hết hoạt cảnh là màn tắt ngay.\n"
                        + "• Ở màn khóa app không hiện gì, chỉ chạy sau khi đã mở khóa.\n"
                        + "• Nút Home, Quay lại và thanh vuốt của hệ thống vẫn hoạt động bình thường."),
                13, SUB));
        Button bat = button(L("Open battery settings for this app", "Mở cài đặt pin của app"), false);
        bat.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });
        LinearLayout.LayoutParams blp = buttonLp();
        blp.setMargins(0, px(10), 0, 0);
        nt.addView(bat, blp);

        // ---- đánh giá (dưới cùng)
        Button rate = button(L("⭐  Rate us on Google Play", "⭐  Đánh giá cho chúng tôi"), true);
        rate.setOnClickListener(v -> openStore());
        col.addView(rate, buttonLp());
        TextView thanks = text(L("Enjoying the critters? A 5-star review helps us a lot!",
                "Bạn thấy vui? Một đánh giá 5 sao giúp chúng tôi rất nhiều!"), 13, SUB);
        thanks.setGravity(Gravity.CENTER);
        thanks.setPadding(0, px(8), 0, px(8));
        col.addView(thanks);

        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        boolean can = Settings.canDrawOverlays(this);
        if (pendingEnable && can) {
            pendingEnable = false;
            Prefs.setEnabled(this, true);
        }
        if (pendingPreview && can) {
            pendingPreview = false;
            startPreview();
        }
        boolean on = Prefs.enabled(this) && can;
        if (on) startGuard();
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(on);
        toggle.setOnCheckedChangeListener((v, checked) -> onToggle(checked));
        if (preview == null) rebuildPreview();
    }

    @Override protected void onPause() {
        super.onPause();
        if (preview != null) {
            preview.stop();
            previewBox.removeView(preview);
            preview = null;
        }
    }

    // ------------------------------------------------------------------ hành động

    private void onToggle(boolean on) {
        if (on) {
            if (!Settings.canDrawOverlays(this)) {
                toggle.setChecked(false);
                pendingEnable = true;
                askOverlay();
                return;
            }
            Prefs.setEnabled(this, true);
            askNotification();
            startGuard();
        } else {
            Prefs.setEnabled(this, false);
            stopService(new Intent(this, ScreenGuardService.class));
        }
    }

    private void startGuard() {
        try {
            startForegroundService(new Intent(this, ScreenGuardService.class));
        } catch (Exception ignored) {
        }
    }

    private void startPreview() {
        if (!Settings.canDrawOverlays(this)) {
            pendingPreview = true;
            askOverlay();
            return;
        }
        try {
            startForegroundService(new Intent(this, ScreenGuardService.class)
                    .setAction(ScreenGuardService.ACTION_PREVIEW));
        } catch (Exception ignored) {
            return;
        }
        // về màn hình chính để thấy con vật bò trên icon
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }

    private void askOverlay() {
        new AlertDialog.Builder(this)
                .setTitle(L("Permission needed", "Cần cấp quyền"))
                .setMessage(L("To let critters appear over other apps, turn on \"Display over other apps\" for Crawlscape, then come back.",
                        "Để con vật hiện được lên trên các ứng dụng khác, hãy bật \"Hiển thị trên ứng dụng khác\" cho Crawlscape, rồi quay lại."))
                .setPositiveButton(L("Open settings", "Mở cài đặt"), (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    try {
                        startActivity(i);
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    }
                })
                .setNegativeButton(L("Later", "Để sau"), (d, w) -> {
                    pendingEnable = false;
                    pendingPreview = false;
                })
                .show();
    }

    private void askNotification() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 5);
        }
    }

    private void rebuildPreview() {
        if (previewBox == null) return;
        if (preview != null) {
            preview.stop();
            previewBox.removeView(preview);
        }
        preview = new SceneView(this, Prefs.scene(this), Prefs.amount(this), false);
        preview.setShowHint(false);
        final SceneView pv = preview;
        preview.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) pv.touch(e.getX(), e.getY());
            return true;
        });
        previewBox.addView(preview, new FrameLayout.LayoutParams(-1, -1));
    }

    // ------------------------------------------------------------------ dựng giao diện

    private int px(float v) { return Math.round(v * dp); }

    private String L(String en, String vi) { return Prefs.t(this, en, vi); }

    private void openStore() {
        String pkg = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            } catch (Exception ignored) {
            }
        }
    }

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private TextView header(String s) {
        TextView t = text(s, 15, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, px(10), 0, px(4));
        return t;
    }

    private RadioButton radio(String s) {
        RadioButton rb = new RadioButton(this);
        rb.setText(s);
        rb.setTextColor(TEXT);
        rb.setTextSize(15);
        rb.setButtonTintList(ColorStateList.valueOf(ACCENT));
        return rb;
    }

    private SeekBar seek(int max, int value) {
        SeekBar s = new SeekBar(this);
        s.setMax(max);
        s.setProgress(value);
        s.setProgressTintList(ColorStateList.valueOf(ACCENT));
        s.setThumbTintList(ColorStateList.valueOf(ACCENT));
        return s;
    }

    private LinearLayout card(ViewGroup parent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(px(16));
        c.setBackground(g);
        c.setPadding(px(16), px(12), px(16), px(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, px(14), 0, 0);
        parent.addView(c, lp);
        return c;
    }

    private Button button(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(primary ? 0xFF10200A : TEXT);
        GradientDrawable g = new GradientDrawable();
        g.setColor(primary ? ACCENT : 0xFF2C3A34);
        g.setCornerRadius(px(14));
        b.setBackground(g);
        return b;
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, px(52));
        lp.setMargins(0, px(16), 0, 0);
        return lp;
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}
    }

    /** Vẽ mấy ô icon giả để xem trước con vật bò trên màn hình chính. */
    private static class IconsBg extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] colors = {0xFFE57373, 0xFF64B5F6, 0xFFFFD54F, 0xFF81C784,
                0xFFBA68C8, 0xFF4DB6AC, 0xFFFF8A65, 0xFF90A4AE};

        IconsBg(Context c) { super(c); }

        @Override protected void onDraw(Canvas c) {
            float d = getResources().getDisplayMetrics().density;
            int cols = 4, rows = 3;
            float cw = getWidth() / (float) cols, ch = getHeight() / (float) rows;
            float size = Math.min(cw, ch) * 0.5f;
            int k = 0;
            for (int r = 0; r < rows; r++) {
                for (int i = 0; i < cols; i++) {
                    float cx = cw * (i + 0.5f), cy = ch * (r + 0.45f);
                    p.setColor(colors[k++ % colors.length]);
                    c.drawRoundRect(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2,
                            14 * d, 14 * d, p);
                    p.setColor(Color.argb(150, 255, 255, 255));
                    c.drawRoundRect(cx - size * 0.35f, cy + size * 0.62f, cx + size * 0.35f,
                            cy + size * 0.7f, 3 * d, 3 * d, p);
                }
            }
        }
    }
}
