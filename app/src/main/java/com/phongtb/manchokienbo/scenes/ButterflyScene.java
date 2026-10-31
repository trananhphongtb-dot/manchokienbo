package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/** Khoảng 20 con bướm nhiều màu, to nhỏ khác nhau: lúc vỗ cánh bay, lúc dang cánh lượn, lúc đậu khép mở cánh. */
public class ButterflyScene extends Scene {

    private static class Fly extends Walker {
        float s, flap, bob, flapSp;
        int mode; // 0 bay, 1 đậu, 2 lượn
        Paint wing;
    }

    private static final int[][] PAL = {
            {0xFFFFCC80, 0xFFF57C00, 0xFF3E2723}, {0xFFB3E5FC, 0xFF0288D1, 0xFF0D1B2A},
            {0xFFFFF9C4, 0xFFFFD600, 0xFF5D4037}, {0xFFFCE4EC, 0xFFF06292, 0xFF4A148C},
            {0xFFE8F5E9, 0xFF66BB6A, 0xFF1B3A1E}, {0xFFFFFFFF, 0xFFE0E0E0, 0xFF424242},
            {0xFFE1BEE7, 0xFF8E24AA, 0xFF2A0A3A}, {0xFFFFE0B2, 0xFFFF5722, 0xFF3E1B10},
            {0xFFB2EBF2, 0xFF00ACC1, 0xFF06343A}};

    private final ArrayList<Fly> list = new ArrayList<>();
    private final Path fore = new Path(), hind = new Path();
    private final Paint edgeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint antP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ButterflyScene(float dp, float mult) {
        super(dp, mult);
        edgeP.setStyle(Paint.Style.STROKE);
        edgeP.setColor(0xCC2B1B10);
        edgeP.setStrokeWidth(3f);
        dot.setColor(0xDDFFFFFF);
        bodyP.setColor(0xFF2B1D14);
        antP.setColor(0xFF2B1D14);
        antP.setStyle(Paint.Style.STROKE);
        antP.setStrokeCap(Paint.Cap.ROUND);
        shadow.setColor(0x22000000);

        // cánh mẫu cỡ 100 đơn vị, phía trên thân (y âm)
        fore.moveTo(5, -3);
        fore.cubicTo(25, -40, 50, -85, 18, -97);
        fore.cubicTo(-8, -102, -18, -60, -5, -4);
        fore.close();
        hind.moveTo(-2, -3);
        hind.cubicTo(-8, -35, -55, -78, -62, -45);
        hind.cubicTo(-65, -20, -38, -5, -6, 0);
        hind.close();
    }

    @Override protected void init() {
        for (int i = 0; i < count(20); i++) {
            Fly f = new Fly();
            f.x = rand(0, w);
            f.y = rand(0.1f, 0.95f) * h;
            f.ang = rand(0, 6.28f);
            f.s = rand(20, 44) * dp;
            f.v = rand(55, 95) * dp;
            f.flap = rand(0, 6.28f);
            f.bob = rand(0, 6.28f);
            f.flapSp = rand(12, 18);
            f.mode = i % 3 == 0 ? 1 : 0;
            if (companion) { // bay vào từ mép, không đậu
                float[] st = edgeStart(rand(10, 80) * dp);
                f.x = st[0]; f.y = st[1]; f.ang = st[2];
                f.mode = 0;
            }
            f.timer = rand(1, 4);
            f.wing = new Paint(Paint.ANTI_ALIAS_FLAG);
            f.wing.setShader(new RadialGradient(0, 0, 105, PAL[i % PAL.length],
                    new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
            list.add(f);
        }
    }

    @Override protected void step(float dt) {
        for (Fly f : list) {
            f.timer -= dt;
            if (f.timer <= 0) {
                float r = rnd.nextFloat();
                if (f.mode == 1) { f.mode = 0; f.timer = rand(2, 5); }
                else if (!companion && r < 0.28f) { f.mode = 1; f.timer = rand(2, 5); }
                else if (r < 0.58f) { f.mode = 2; f.timer = rand(0.6f, 1.4f); }
                else { f.mode = 0; f.timer = rand(1.5f, 4); }
            }
            if (f.mode == 1) {
                f.flap += dt * 2.2f;
                continue;
            }
            if (!bounce(f, 30 * dp, 3f, dt)) {
                f.turn += rand(-1f, 1f) * 6f * dt;
                f.turn *= 0.97f;
                f.turn = clamp(f.turn, -2f, 2f);
                f.ang += (f.turn + sin(time * 1.7f + f.bob) * 0.8f) * dt * (f.mode == 2 ? 0.4f : 1f);
            }
            float sp = f.v * (f.mode == 2 ? 0.75f : 1f);
            f.x += cos(f.ang) * sp * dt;
            f.y += sin(f.ang) * sp * dt;
            f.flap += dt * (f.mode == 2 ? 3f : f.flapSp);
        }
    }

    @Override public void touch(float x, float y) {
        for (Fly f : list) {
            if (Math.hypot(f.x - x, f.y - y) > 140 * dp) continue;
            f.mode = 0; // giật mình bay vụt lên
            f.timer = rand(1.5f, 3f);
            f.ang = (float) Math.atan2(f.y - y, f.x - x) + rand(-0.5f, 0.5f);
            f.turn = 0;
            f.esc = false;
        }
    }

    @Override public void draw(Canvas c) {
        for (Fly f : list) drawFly(c, f);
    }

    private void drawFly(Canvas c, Fly f) {
        float s = f.s;
        float bob = f.mode == 1 ? 0f : sin(f.flap * 0.5f) * 3 * dp;
        float x = f.x, y = f.y + bob;
        if (offscreen(x, y, s * 2)) return;
        float open;
        if (f.mode == 1) open = 0.35f + 0.65f * (0.5f + 0.5f * sin(f.flap));
        else if (f.mode == 2) open = 0.8f + 0.2f * sin(f.flap);
        else open = 0.18f + 0.82f * Math.abs(sin(f.flap));

        if (f.mode != 1) {
            c.drawOval(x + 10 * dp - s * 0.5f, y + 22 * dp - s * 0.35f * open,
                    x + 10 * dp + s * 0.5f, y + 22 * dp + s * 0.35f * open, shadow);
        }

        int save = c.save();
        c.translate(x, y);
        c.rotate(deg(f.ang));
        float k = s / 100f;
        for (int side = -1; side <= 1; side += 2) {
            int sv = c.save();
            c.scale(k, k * open * side);
            c.drawPath(hind, f.wing);
            c.drawPath(hind, edgeP);
            c.drawPath(fore, f.wing);
            c.drawPath(fore, edgeP);
            c.drawCircle(18, -86, 4.5f, dot);
            c.drawCircle(5, -90, 3.5f, dot);
            c.drawCircle(-52, -48, 4, dot);
            c.restoreToCount(sv);
        }

        c.drawOval(-0.38f * s, -0.055f * s, 0.18f * s, 0.055f * s, bodyP);
        c.drawCircle(0.22f * s, 0, 0.065f * s, bodyP);
        antP.setStrokeWidth(0.025f * s);
        c.drawLine(0.25f * s, -0.03f * s, 0.55f * s, -0.2f * s, antP);
        c.drawLine(0.25f * s, 0.03f * s, 0.55f * s, 0.2f * s, antP);
        c.drawCircle(0.55f * s, -0.2f * s, 0.035f * s, bodyP);
        c.drawCircle(0.55f * s, 0.2f * s, 0.035f * s, bodyP);
        c.restoreToCount(save);
    }
}
