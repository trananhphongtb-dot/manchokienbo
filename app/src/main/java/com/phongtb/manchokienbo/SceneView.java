package com.phongtb.manchokienbo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import com.phongtb.manchokienbo.scenes.AntScene;
import com.phongtb.manchokienbo.scenes.BubbleScene;
import com.phongtb.manchokienbo.scenes.ButterflyScene;
import com.phongtb.manchokienbo.scenes.ChaseScene;
import com.phongtb.manchokienbo.scenes.CrackScene;
import com.phongtb.manchokienbo.scenes.FogScene;
import com.phongtb.manchokienbo.scenes.FireflyScene;
import com.phongtb.manchokienbo.scenes.FishScene;
import com.phongtb.manchokienbo.scenes.MouseScene;
import com.phongtb.manchokienbo.scenes.RoachScene;
import com.phongtb.manchokienbo.scenes.Scene;
import com.phongtb.manchokienbo.scenes.SnakeScene;

import java.util.Random;

/** Khung vẽ hoạt cảnh, chạy theo nhịp màn hình. */
public class SceneView extends View {

    private final Scene scene;
    private long lastFrame = 0;
    private boolean running = true;
    private boolean toBlack = false;
    private float black = 0f;
    private float age = 0f;
    private final Paint blackPaint = new Paint();
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final boolean doubleTap;
    private final String hint;

    public SceneView(Context c, String key, int amount, boolean doubleTap) {
        super(c);
        float dp = c.getResources().getDisplayMetrics().density;
        float mult = amount == 0 ? 0.5f : (amount == 2 ? 1.5f : 1f);
        if ("random".equals(key)) {
            String[] keys = {"ants", "roach", "mouse", "fish", "snake", "chase", "butterfly", "firefly", "bubble", "fog", "crack"};
            key = keys[new Random().nextInt(keys.length)];
        }
        this.doubleTap = doubleTap;
        hint = doubleTap ? Prefs.t(c, "Double-tap to unlock", "Chạm 2 lần liên tiếp để mở")
                : Prefs.t(c, "Tap anywhere to continue", "Chạm vào màn hình để tiếp tục");
        scene = create(key, dp, mult);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2 * dp);
        blackPaint.setColor(Color.BLACK);
        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextAlign(Paint.Align.CENTER);
        hintPaint.setTextSize(14 * dp);
        hintPaint.setShadowLayer(4 * dp, 0, 1 * dp, 0xAA000000);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public static Scene create(String key, float dp, float mult) {
        switch (key) {
            case "roach": return new RoachScene(dp, mult);
            case "mouse":
            case "hamster": return new MouseScene(dp, mult);
            case "chase": return new ChaseScene(dp, mult);
            case "fog": return new FogScene(dp, mult);
            case "crack": return new CrackScene(dp, mult);
            case "fish": return new FishScene(dp, mult);
            case "butterfly": return new ButterflyScene(dp, mult);
            case "firefly": return new FireflyScene(dp, mult);
            case "snake": return new SnakeScene(dp, mult);
            case "bubble": return new BubbleScene(dp, mult);
            case "ants":
            default: return new AntScene(dp, mult);
        }
    }

    private boolean showHint = true;

    public void setShowHint(boolean v) { showHint = v; }

    public void stop() { running = false; }

    public void fadeToBlack() { toBlack = true; }

    /** Đang tối/đen thì sáng lại, hoạt cảnh chạy tiếp. */
    public void unBlack() {
        toBlack = false;
        black = 0f;
        lastFrame = 0;
        postInvalidateOnAnimation();
    }

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float ringX, ringY, ringAge = 9f;

    /** Chạm 1 cái: vòng sáng nhỏ tại chỗ chạm, con vật gần đó giật mình. */
    public void touch(float x, float y) {
        ringX = x;
        ringY = y;
        ringAge = 0f;
        scene.touch(x, y);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w > 0 && h > 0) scene.resize(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!running || scene.w == 0) {
            if (running) postInvalidateOnAnimation();
            return;
        }
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0.016f : (now - lastFrame) / 1000f;
        lastFrame = now;
        if (dt > 0.05f) dt = 0.05f;
        age += dt;

        if (black < 1f) {
            scene.update(dt);
            scene.draw(canvas);
        }

        if (ringAge < 0.5f) {
            ringAge += dt;
            float k = Math.min(1f, ringAge / 0.5f);
            ringPaint.setColor(0xFFFFFFFF);
            ringPaint.setAlpha((int) (200 * (1 - k)));
            canvas.drawCircle(ringX, ringY, (10 + 40 * k) * scene.dp, ringPaint);
        }

        // Dòng nhắc mờ trong 4 giây đầu
        if (showHint && age < 4f && !toBlack) {
            float a = age < 0.8f ? age / 0.8f : (age > 3f ? (4f - age) : 1f);
            hintPaint.setAlpha((int) (150 * Math.max(0f, a)));
            canvas.drawText(hint, getWidth() / 2f,
                    getHeight() - 60 * scene.dp, hintPaint);
        }

        if (toBlack) {
            black = Math.min(1f, black + dt / 1.8f);
            blackPaint.setAlpha((int) (255 * black));
            canvas.drawRect(0, 0, getWidth(), getHeight(), blackPaint);
            if (black >= 1f) return; // đen hẳn: dừng vẽ cho đỡ tốn pin
        }
        postInvalidateOnAnimation();
    }
}
