package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;

import java.util.ArrayList;

/**
 * Bong bóng xà phòng kiểu Windows: nhiều màu, nhiều cỡ, trôi lững lờ,
 * chạm mép thì nảy lại, va vào nhau cũng nảy ra. Viền cầu vồng xoay óng ánh.
 */
public class BubbleScene extends Scene {

    private static class Bubble {
        float x, y, r, vx, vy, rot, spin, ph, hue;
        Paint bodyP;
    }

    private final ArrayList<Bubble> list = new ArrayList<>();
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glint2 = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BubbleScene(float dp, float mult) {
        super(dp, mult);
        int[] rainbow = new int[7];
        for (int k = 0; k <= 6; k++) rainbow[k] = hsv((k * 60) % 360, 0.7f, 1f, 0.75f);
        rim.setShader(new SweepGradient(0, 0, rainbow, null));
        rim.setStyle(Paint.Style.STROKE);
        gloss.setColor(0x8CFFFFFF);
        glint.setColor(0xE6FFFFFF);
        glint2.setColor(0x73FFFFFF);
    }

    @Override protected void init() {
        for (int i = 0; i < count(24); i++) {
            Bubble b = new Bubble();
            b.r = rand(14, 64) * dp;
            float a = rand(0, 6.28f), v = rand(25, 75) * dp;
            int tries = 0;
            boolean overlap;
            do {
                b.x = rand(b.r, w - b.r);
                b.y = rand(b.r, h - b.r);
                overlap = false;
                for (Bubble o : list) {
                    if (Math.hypot(o.x - b.x, o.y - b.y) < o.r + b.r) { overlap = true; break; }
                }
                tries++;
            } while (overlap && tries < 30);
            b.vx = cos(a) * v;
            b.vy = sin(a) * v;
            b.hue = rand(0, 360);
            b.rot = rand(0, 6.28f);
            b.spin = rand(-0.6f, 0.6f);
            b.ph = rand(0, 6.28f);
            b.bodyP = new Paint(Paint.ANTI_ALIAS_FLAG);
            b.bodyP.setShader(new RadialGradient(0, 0, b.r,
                    new int[]{hsv(b.hue, 0.5f, 1f, 0.02f), hsv(b.hue, 0.6f, 1f, 0.08f), hsv((b.hue + 40) % 360, 0.8f, 1f, 0.38f)},
                    new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
            list.add(b);
        }
    }

    @Override protected void step(float dt) {
        for (Bubble b : list) {
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.rot += b.spin * dt;
            b.hue += dt * 12;
            if (b.x < b.r && b.vx < 0) b.vx = -b.vx;
            if (b.x > w - b.r && b.vx > 0) b.vx = -b.vx;
            if (b.y < b.r && b.vy < 0) b.vy = -b.vy;
            if (b.y > h - b.r && b.vy > 0) b.vy = -b.vy;
        }
        // va vào nhau thì nảy ra
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Bubble a = list.get(i), b = list.get(j);
                float dx = b.x - a.x, dy = b.y - a.y;
                float d = (float) Math.hypot(dx, dy), md = a.r + b.r;
                if (d < md && d > 0.01f) {
                    float nx = dx / d, ny = dy / d, ma = a.r * a.r, mb = b.r * b.r;
                    float rel = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny;
                    if (rel > 0) {
                        float imp = 2 * rel / (ma + mb);
                        a.vx -= imp * mb * nx; a.vy -= imp * mb * ny;
                        b.vx += imp * ma * nx; b.vy += imp * ma * ny;
                    }
                    float ov = (md - d) / 2;
                    a.x -= nx * ov; a.y -= ny * ov;
                    b.x += nx * ov; b.y += ny * ov;
                }
            }
        }
    }

    @Override public void draw(Canvas c) {
        for (Bubble b : list) {
            float k = 1 + 0.025f * sin(time * 2.5f + b.ph);
            float r = b.r * k;
            int save = c.save();
            c.translate(b.x, b.y);
            int s2 = c.save();
            c.scale(k, k);
            c.drawCircle(0, 0, b.r, b.bodyP);
            c.restoreToCount(s2);
            // viền cầu vồng xoay óng ánh
            s2 = c.save();
            c.rotate(deg(b.rot) + b.hue);
            rim.setStrokeWidth(Math.max(1.2f * dp, r * 0.06f));
            c.drawCircle(0, 0, r * 0.97f, rim);
            c.restoreToCount(s2);
            // ánh sáng phản chiếu
            s2 = c.save();
            c.rotate(-40f);
            c.drawOval(-r * 0.35f, -r * 0.78f, r * 0.35f, -r * 0.5f, gloss);
            c.restoreToCount(s2);
            c.drawCircle(-r * 0.42f, -r * 0.42f, r * 0.09f, glint);
            c.drawCircle(r * 0.5f, r * 0.5f, r * 0.06f, glint2);
            c.restoreToCount(save);
        }
    }
}
