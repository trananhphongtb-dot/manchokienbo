package com.phongtb.manchokienbo.scenes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Sương mù theo từng mảng: mỗi mảng hiện ra đậm đặc rồi loang rộng, mỏng dần và tan,
 * mảng mới lại trôi vào. Giữa các mảng vẫn có chỗ trong. Chạm vào thì sương tan ra quanh ngón tay.
 */
public class FogScene extends Scene {

    private static class Puff {
        float ox, oy, r, a, sx, ph;
    }

    private static class Bank {
        float x, y, R, age, life, vx, vy, hole, hx, hy;
        final Puff[] puffs = new Puff[14];
    }

    private final ArrayList<Bank> banks = new ArrayList<>();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF rect = new RectF();
    private Bitmap sprite;
    private float wind;

    public FogScene(float dp, float mult) {
        super(dp, mult);
    }

    @Override protected void init() {
        int size = 128;
        sprite = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas g = new Canvas(sprite);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(64, 64, 64,
                new int[]{0xFFFFFFFF, 0x99FFFFFF, 0x33FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        g.drawRect(0, 0, size, size, p);
        wind = rand(0, 6.28f);
        for (int i = 0; i < count(3); i++) banks.add(makeBank(i == 0 ? 0.35f : rand(0, 0.6f), true));
    }

    private Bank makeBank(float age, boolean inside) {
        Bank b = new Bank();
        b.R = rand(110, 190) * dp;
        b.life = rand(18, 30);
        b.age = age * b.life;
        float sp = rand(8, 16) * dp;
        b.vx = cos(wind) * sp;
        b.vy = sin(wind) * sp;
        if (inside) {
            b.x = rand(0.15f, 0.85f) * w;
            b.y = rand(0.15f, 0.85f) * h;
        } else { // trôi vào từ phía đầu gió
            b.x = w / 2f - cos(wind) * (w * 0.5f + b.R);
            b.y = rand(0.1f, 0.9f) * h - sin(wind) * h * 0.3f;
        }
        for (int k = 0; k < b.puffs.length; k++) {
            Puff o = new Puff();
            float d = (float) Math.sqrt(rnd.nextFloat()), a = rand(0, 6.28f);
            o.ox = cos(a) * d;
            o.oy = sin(a) * d * 0.8f;
            o.r = rand(0.35f, 0.7f) * (1.2f - d * 0.5f);
            o.a = lerp(0.95f, 0.35f, d); // giữa đậm, rìa nhạt
            o.sx = rand(1.2f, 2.2f);
            o.ph = rand(0, 6.28f);
            b.puffs[k] = o;
        }
        return b;
    }

    @Override public void touch(float x, float y) {
        for (Bank b : banks) {
            b.hole = 1;
            b.hx = x;
            b.hy = y;
        }
    }

    @Override protected void step(float dt) {
        for (Bank b : banks) {
            b.age += dt;
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.hole = Math.max(0, b.hole - dt * 0.35f);
        }
        for (int i = banks.size() - 1; i >= 0; i--) if (banks.get(i).age >= banks.get(i).life) banks.remove(i);
        while (banks.size() < count(3)) banks.add(makeBank(0, rnd.nextBoolean()));
    }

    @Override public void draw(Canvas c) {
        float holeR = 160 * dp;
        for (Bank b : banks) {
            float t = b.age / b.life;
            float fade = t < 0.15f ? t / 0.15f : (t > 0.7f ? (1 - t) / 0.3f : 1f);
            float spread = 0.7f + 1.1f * t;   // càng lâu càng loang rộng
            float thin = 1 - 0.55f * t;       // loang ra thì mỏng dần
            for (Puff o : b.puffs) {
                float x = b.x + o.ox * b.R * spread + sin(time * 0.25f + o.ph) * 8 * dp;
                float y = b.y + o.oy * b.R * spread;
                float r = o.r * b.R * (0.8f + 0.6f * spread);
                float a = o.a * fade * thin * (0.85f + 0.15f * sin(time * 0.4f + o.ph));
                if (b.hole > 0) {
                    float d = (float) Math.hypot(x - b.hx, y - b.hy);
                    if (d < holeR) a *= 1 - b.hole * (1 - d / holeR);
                }
                paint.setAlpha((int) (255 * clamp(a, 0f, 1f)));
                int sv = c.save();
                c.translate(x, y);
                c.scale(o.sx, 1);
                rect.set(-r, -r, r, r);
                c.drawBitmap(sprite, null, rect, paint);
                c.restoreToCount(sv);
            }
        }
    }
}
