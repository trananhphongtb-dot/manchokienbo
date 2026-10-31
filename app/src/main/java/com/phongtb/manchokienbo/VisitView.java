package com.phongtb.manchokienbo;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;

import com.phongtb.manchokienbo.scenes.Scene;

/**
 * Chế độ đồng hành: trong lúc đang dùng app khác, thỉnh thoảng vài con vật nhỏ
 * lướt ngang qua màn hình rồi biến mất. Cửa sổ này không nhận chạm (chạm xuyên qua được).
 */
public class VisitView extends View {

    // loại con, hệ số số lượng (cho ra vài con), tỉ lệ thu nhỏ (cỡ con gián bé)
    private static final String[] TYPES = {"ants", "roach", "mouse", "butterfly", "fish", "crack"};
    private static final float[] MULT = {0.15f, 0.2f, 0.4f, 0.12f, 0.22f, 1f};
    private static final float[] SCALE = {0.8f, 0.6f, 0.45f, 0.6f, 0.3f, 0.5f};

    private final Scene scene;
    private final float scale;
    private final float duration;
    private float age = 0f;
    private long lastFrame = 0;
    private boolean running = true;

    public VisitView(Context c, String type) {
        super(c);
        float dp = c.getResources().getDisplayMetrics().density;
        int i = 0;
        while (i < TYPES.length && !TYPES[i].equals(type)) i++;
        if (i == TYPES.length) i = 0;
        scale = SCALE[i];
        scene = SceneView.create(TYPES[i], dp, MULT[i]);
        scene.companion = true;
        duration = "crack".equals(TYPES[i]) ? 7f : 12f;
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public static boolean known(String type) {
        for (String t : TYPES) if (t.equals(type)) return true;
        return false;
    }

    /** Đã lướt qua xong chưa */
    public boolean finished() { return age >= duration; }

    public void stop() { running = false; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w > 0 && h > 0) scene.resize(Math.round(w / scale), Math.round(h / scale));
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!running || scene.w == 0) {
            if (running) postInvalidateOnAnimation();
            return;
        }
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        age += dt;
        scene.update(dt);
        // hiện dần lúc đầu, mờ dần lúc cuối
        float a = Math.min(1f, age / 0.6f) * Math.min(1f, Math.max(0f, (duration - age) / 1.5f));
        setAlpha(a);
        int sv = canvas.save();
        canvas.scale(scale, scale);
        scene.draw(canvas);
        canvas.restoreToCount(sv);
        if (age < duration) postInvalidateOnAnimation();
    }
}
