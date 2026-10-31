package com.phongtb.manchokienbo.scenes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.ArrayList;

/** Đêm trăng: sao lấp lánh, khoảng 20 con đom đóm to nhỏ, sắc vàng xanh khác nhau, chập chờn trên ngọn cỏ. */
public class FireflyScene extends Scene {

    private static class Fly extends Walker {
        float fq, ph, gs;
        Bitmap glow;
    }

    private final ArrayList<Fly> list = new ArrayList<>();
    private float[] sx, sy, sph;
    private float[] gx, gh, gph;
    private final RectF rect = new RectF();
    private final Paint night = new Paint();
    private final Paint glowP = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint star = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moonGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float moonX, moonY, moonR;

    public FireflyScene(float dp, float mult) {
        super(dp, mult);
        core.setColor(0xFFF4FFC8);
        star.setColor(0xFFFFFFFF);
        moon.setColor(0xF2FFF8E1);
        grass.setColor(0xFF03100A);
    }

    private Bitmap makeGlow(float hue) {
        int size = 64;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas gc = new Canvas(b);
        Paint gp = new Paint(Paint.ANTI_ALIAS_FLAG);
        gp.setShader(new RadialGradient(size / 2f, size / 2f, size / 2f,
                new int[]{hsv(hue, 0.4f, 1f, 1f), hsv(hue, 0.75f, 1f, 0.55f), hsv(hue, 1f, 1f, 0f)},
                new float[]{0f, 0.35f, 1f}, Shader.TileMode.CLAMP));
        gc.drawCircle(size / 2f, size / 2f, size / 2f, gp);
        return b;
    }

    @Override protected void init() {
        Bitmap[] glows = {makeGlow(65), makeGlow(78), makeGlow(90)};
        for (int i = 0; i < count(20); i++) {
            Fly f = new Fly();
            f.x = rand(0, w);
            f.y = rand(0.25f, 0.95f) * h;
            f.ang = rand(0, 6.28f);
            f.v = rand(12, 30) * dp;
            f.fq = rand(0.5f, 1.4f);
            f.ph = rand(0, 6.28f);
            f.gs = rand(0.6f, 1.5f);
            f.glow = glows[rnd.nextInt(glows.length)];
            list.add(f);
        }
        int ns = 70;
        sx = new float[ns]; sy = new float[ns]; sph = new float[ns];
        for (int i = 0; i < ns; i++) {
            sx[i] = rand(0, w);
            sy[i] = rand(0, 0.55f * h);
            sph[i] = rand(0, 6.28f);
        }
        onResize();
    }

    @Override protected void onResize() {
        night.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xD8020818, 0xC0061428, 0xB0081A14}, new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        moonR = Math.min(w, h) * 0.07f;
        moonX = w * 0.78f;
        moonY = h * 0.12f;
        moonGlow.setShader(new RadialGradient(moonX, moonY, moonR * 4f,
                0x40FFF8E1, 0x00FFF8E1, Shader.TileMode.CLAMP));
        int ng = Math.max(30, (int) (w / (7 * dp)));
        gx = new float[ng]; gh = new float[ng]; gph = new float[ng];
        for (int i = 0; i < ng; i++) {
            gx[i] = i * w / (float) (ng - 1) + rand(-3, 3) * dp;
            gh[i] = rand(0.05f, 0.14f) * h;
            gph[i] = rand(0, 6.28f);
        }
    }

    @Override protected void step(float dt) {
        for (Fly f : list) {
            if (!bounce(f, 30 * dp, 2f, dt)) {
                f.turn += rand(-1f, 1f) * 3f * dt;
                f.turn *= 0.98f;
                f.ang += f.turn * dt;
            }
            // không bay quá cao lên bầu trời
            if (f.y < 0.25f * h && !f.esc) {
                f.esc = true;
                f.escAng = (float) (Math.PI / 2) + rand(-1f, 1f);
            }
            f.x += cos(f.ang) * f.v * dt;
            f.y += sin(f.ang) * f.v * dt;
        }
    }

    @Override public void draw(Canvas c) {
        c.drawRect(0, 0, w, h, night);

        for (int i = 0; i < sx.length; i++) {
            float tw = 0.5f + 0.5f * sin(time * 2f + sph[i]);
            star.setAlpha((int) (60 + 160 * tw));
            c.drawCircle(sx[i], sy[i], (0.6f + 0.6f * tw) * dp, star);
        }

        c.drawCircle(moonX, moonY, moonR * 4f, moonGlow);
        c.drawCircle(moonX, moonY, moonR, moon);

        path.reset();
        path.moveTo(0, h);
        for (int i = 0; i < gx.length; i++) {
            float sway = sin(time * 1.1f + gph[i]) * 6 * dp;
            path.lineTo(gx[i] - 3 * dp, h);
            path.quadTo(gx[i] + sway * 0.3f, h - gh[i] * 0.6f, gx[i] + sway, h - gh[i]);
            path.quadTo(gx[i] + sway * 0.3f + 2 * dp, h - gh[i] * 0.6f, gx[i] + 3 * dp, h);
        }
        path.lineTo(w, h);
        path.close();
        c.drawPath(path, grass);

        for (Fly f : list) {
            float p = sin(time * f.fq * 3.1416f + f.ph);
            float b = p > 0 ? p * p * p : 0f;
            float a = 0.12f + 0.88f * b;
            glowP.setAlpha((int) (255 * a));
            float r = 22 * dp * f.gs * (0.6f + 0.6f * b);
            rect.set(f.x - r, f.y - r, f.x + r, f.y + r);
            c.drawBitmap(f.glow, null, rect, glowP);
            core.setAlpha((int) (120 + 135 * b));
            c.drawCircle(f.x, f.y, 1.6f * dp * f.gs, core);
        }
    }
}
