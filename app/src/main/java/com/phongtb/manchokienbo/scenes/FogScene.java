package com.phongtb.manchokienbo.scenes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Mây mù: ba lớp sương trôi theo gió với tốc độ khác nhau (xa chậm, gần nhanh),
 * dày mỏng thay đổi. Chạm vào thì sương bị thổi dạt ra, để lộ một khoảng trong.
 */
public class FogScene extends Scene {

    private static class Blob {
        float x, y, r, sx, v, a, ph, push, hole;
        int layer;
        Paint paint;
    }

    // màu, tốc độ (dp/s), bán kính nhỏ nhất, lớn nhất (dp), độ đậm
    private static final float[][] LAYERS = {{6, 90, 170, 0.45f}, {12, 130, 230, 0.5f}, {22, 170, 300, 0.45f}};
    private static final int[] LAYER_COLORS = {0xFFC9D3DF, 0xFFE3E9EF, 0xFFFFFFFF};

    private final ArrayList<Blob> blobs = new ArrayList<>();
    private final Paint haze = new Paint();
    private final RectF rect = new RectF();
    private float wind;

    public FogScene(float dp, float mult) {
        super(dp, mult);
        haze.setColor(0x1FBEC8D4);
    }

    private Bitmap sprite(int color) {
        int size = 128;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas g = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(64, 64, 64,
                new int[]{color, withAlpha(color, 0.6f), withAlpha(color, 0.2f), withAlpha(color, 0f)},
                new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        g.drawRect(0, 0, size, size, p);
        return b;
    }

    private Bitmap[] sprites;

    @Override protected void init() {
        sprites = new Bitmap[3];
        for (int L = 0; L < 3; L++) sprites[L] = sprite(LAYER_COLORS[L]);
        wind = rand(-0.25f, 0.25f);
        for (int L = 0; L < 3; L++) {
            for (int i = 0; i < count(7); i++) {
                Blob b = new Blob();
                float[] cfg = LAYERS[L];
                b.x = rand(-0.3f, 1.3f) * w;
                b.y = rand(-0.1f, 1.1f) * h;
                b.r = rand(cfg[1], cfg[2]) * dp;
                b.sx = rand(1.5f, 2.6f);
                b.v = cfg[0] * rand(0.7f, 1.3f) * dp;
                b.a = cfg[3];
                b.ph = rand(0, 6.28f);
                b.paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                b.layer = L;
                blobs.add(b);
            }
        }
    }

    @Override public void touch(float x, float y) {
        for (Blob b : blobs) {
            if (Math.hypot(b.x - x, b.y - y) < b.r * 1.2f) {
                b.push += (b.x > x ? 1 : -1) * 120 * dp;
                b.hole = 1;
            }
        }
    }

    @Override protected void step(float dt) {
        for (Blob b : blobs) {
            float v = b.v + b.push;
            b.push *= 1 - Math.min(1f, dt * 1.5f);
            b.hole = Math.max(0, b.hole - dt * 0.4f);
            b.x += v * dt;
            b.y += v * wind * dt + sin(time * 0.2f + b.ph) * 3 * dp * dt;
            float span = b.r * b.sx;
            if (b.x - span > w) b.x = -span;
            if (b.x + span < 0) b.x = w + span;
            if (b.y - b.r > h) b.y = -b.r;
            if (b.y + b.r < 0) b.y = h + b.r;
        }
    }

    @Override public void draw(Canvas c) {
        c.drawRect(0, 0, w, h, haze);
        for (Blob b : blobs) {
            float a = b.a * (0.7f + 0.3f * sin(time * 0.3f + b.ph)) * (1 - 0.8f * b.hole);
            b.paint.setAlpha((int) (255 * clamp(a, 0f, 1f)));
            int sv = c.save();
            c.translate(b.x, b.y);
            c.scale(b.sx, 1);
            rect.set(-b.r, -b.r, b.r, b.r);
            c.drawBitmap(sprites[b.layer], null, rect, b.paint);
            c.restoreToCount(sv);
        }
    }
}
