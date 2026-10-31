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

    public SceneView(Context c, String key, int amount) {
        super(c);
        float dp = c.getResources().getDisplayMetrics().density;
        float mult = amount == 0 ? 0.5f : (amount == 2 ? 1.5f : 1f);
        if ("random".equals(key)) {
            String[] keys = {"ants", "roach", "mouse", "fish", "snake", "butterfly", "firefly", "bubble"};
            key = keys[new Random().nextInt(keys.length)];
        }
        scene = create(key, dp, mult);
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
            case "mouse": return new MouseScene(dp, mult);
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

        // Dòng nhắc mờ trong 4 giây đầu
        if (showHint && age < 4f && !toBlack) {
            float a = age < 0.8f ? age / 0.8f : (age > 3f ? (4f - age) : 1f);
            hintPaint.setAlpha((int) (150 * Math.max(0f, a)));
            canvas.drawText("Chạm vào màn hình để tiếp tục", getWidth() / 2f,
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
