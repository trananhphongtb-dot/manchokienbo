package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Gián (nhìn từ trên xuống), khoảng 10 con, to nhỏ và màu khác nhau.
 * Lúc chạy vụt, lúc đi, lúc bò rón rén, lúc đứng quét râu nghe ngóng rồi xoay người,
 * thỉnh thoảng xòe cánh vỗ phành phạch lao đi.
 */
public class RoachScene extends Scene {

    private static final Mood[] MOODS = {
            new Mood(DASH, 32, 220, 300, 0.3f, 1.2f),
            new Mood(WALK, 15, 90, 130, 0.8f, 2f),
            new Mood(SLOW, 15, 25, 45, 1f, 2.5f),
            new Mood(FREEZE, 30, 0, 0, 0.8f, 3.5f),
            new Mood(FLUTTER, 8, 120, 170, 0.5f, 0.9f)};
    // sáng, giữa, tối (cánh), mai sáng, mai tối / chân
    private static final int[][] PAL = {
            {0xFF8A4A22, 0xFF5A2A0F, 0xFF2A1206, 0xFF6A3415, 0xFF2E1408},
            {0xFF6E3A1A, 0xFF44200C, 0xFF1C0C04, 0xFF55290F, 0xFF221006},
            {0xFF7E4A24, 0xFF4F2A10, 0xFF241206, 0xFF62361A, 0xFF2A160A}};

    private static class Roach extends Walker {
        float gait, s, tw, open, flap;
        int[] pal;
        Paint wingL, wingR, shield, dark;
    }

    private final ArrayList<Roach> list = new ArrayList<>();
    private final Paint leg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint seam = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hi = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint membrane = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] lines = new float[48];
    private static final float[][] BASE = {{0.2f, 0.1f}, {0.05f, 0.12f}, {-0.1f, 0.12f}};
    private static final float[][] KNEE = {{0.32f, 0.32f}, {0.05f, 0.4f}, {-0.3f, 0.42f}};
    private static final float[][] FOOT = {{0.5f, 0.42f}, {-0.1f, 0.56f}, {-0.72f, 0.56f}};

    public RoachScene(float dp, float mult) {
        super(dp, mult);
        leg.setStyle(Paint.Style.STROKE);
        leg.setStrokeCap(Paint.Cap.ROUND);
        seam.setColor(0xAA2A1206);
        seam.setStyle(Paint.Style.STROKE);
        hi.setColor(0x2EFFFFFF);
        rim.setColor(0x66E0A870);
        rim.setStyle(Paint.Style.STROKE);
        head.setColor(0xFF24120A);
        shadow.setColor(0x33000000);
        membrane.setColor(0x59E6BE8C);
    }

    @Override protected void init() {
        for (int i = 0; i < count(10); i++) {
            Roach r = new Roach();
            float[] st = edgeStart(rand(30, 300) * dp);
            r.x = st[0]; r.y = st[1]; r.ang = st[2];
            r.s = rand(22, 42) * dp;
            r.tw = rand(0, 6);
            r.mood = DASH;
            r.timer = rand(0.6f, 1.6f);
            r.target = rand(200, 280) * dp;
            r.pal = PAL[rnd.nextInt(PAL.length)];
            float s = r.s;
            int[] P = r.pal;
            r.wingL = new Paint(Paint.ANTI_ALIAS_FLAG);
            r.wingL.setShader(new RadialGradient(-0.1f * s, -0.06f * s, 0.55f * s,
                    new int[]{P[0], P[1], P[2]}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            r.wingR = new Paint(Paint.ANTI_ALIAS_FLAG);
            r.wingR.setShader(new RadialGradient(-0.1f * s, 0.06f * s, 0.55f * s,
                    new int[]{P[0], P[1], P[2]}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            r.shield = new Paint(Paint.ANTI_ALIAS_FLAG);
            r.shield.setShader(new RadialGradient(0.25f * s, -0.04f * s, 0.22f * s, P[3], P[4], Shader.TileMode.CLAMP));
            r.dark = new Paint(Paint.ANTI_ALIAS_FLAG);
            r.dark.setColor(P[4]);
            list.add(r);
        }
    }

    @Override protected void step(float dt) {
        for (Roach r : list) {
            r.timer -= dt;
            if (r.timer <= 0) nextMood(r, MOODS);
            float acc = (r.mood == DASH || r.mood == FLUTTER) ? 1500 * dp : 700 * dp;
            if (r.v < r.target) r.v = Math.min(r.target, r.v + acc * dt);
            else r.v = Math.max(r.target, r.v - 2400 * dp * dt);

            if (!bounce(r, 30 * dp, 8f, dt)) {
                if (r.v > 1) {
                    r.turn += rand(-6f, 6f) * dt * (r.mood == DASH ? 1f : 0.4f);
                    r.turn *= 0.97f;
                    r.ang += r.turn * dt;
                } else {
                    r.ang += r.pivot * dt;
                    r.pivot *= 0.95f;
                }
            }
            r.x += cos(r.ang) * r.v * dt;
            r.y += sin(r.ang) * r.v * dt;
            r.gait += r.v * dt / (r.s * 0.12f);
            r.tw += dt * (r.mood == FREEZE ? 3.5f : 7f);
            r.open = lerp(r.open, r.mood == FLUTTER ? 1f : 0f, Math.min(1f, dt * 10f));
            r.flap += dt * 55f;
        }
    }

    @Override public void touch(float x, float y) {
        for (Roach r : list) scare(r, x, y, 160 * dp, 300 * dp);
    }

    @Override public void draw(Canvas c) {
        for (Roach r : list) drawRoach(c, r);
    }

    private void drawRoach(Canvas c, Roach r) {
        float s = r.s;
        if (offscreen(r.x, r.y, s * 2)) return;
        int save = c.save();
        c.translate(r.x, r.y);
        c.rotate(deg(r.ang));

        c.drawOval(-0.52f * s, -0.2f * s, 0.58f * s, 0.4f * s, shadow);

        // chân
        leg.setColor(r.pal[4]);
        leg.setStrokeWidth(0.035f * s);
        int k = 0;
        for (int side = -1; side <= 1; side += 2) {
            for (int j = 0; j < 3; j++) {
                boolean groupA = (j == 1) == (side == 1);
                float sw = r.v > 1 ? sin(r.gait + (groupA ? 0f : 3.1416f)) : 0f;
                lines[k++] = BASE[j][0] * s;
                lines[k++] = side * BASE[j][1] * s;
                lines[k++] = (KNEE[j][0] + sw * 0.06f) * s;
                lines[k++] = side * KNEE[j][1] * s;
                lines[k++] = (KNEE[j][0] + sw * 0.06f) * s;
                lines[k++] = side * KNEE[j][1] * s;
                lines[k++] = (FOOT[j][0] + sw * 0.12f) * s;
                lines[k++] = side * FOOT[j][1] * s;
            }
        }
        c.drawLines(lines, leg);

        // râu dài: đứng nghe ngóng thì quét chậm qua lại
        leg.setStrokeWidth(0.018f * s);
        boolean listen = r.mood == FREEZE;
        for (int side = -1; side <= 1; side += 2) {
            float t1 = listen ? sin(r.tw + side * 0.8f + r.look) * 0.3f : sin(r.tw + side) * 0.12f;
            float t2 = listen ? sin(r.tw * 0.7f + side * 2f + r.look) * 0.35f : sin(r.tw * 1.3f + side * 2f) * 0.2f;
            path.reset();
            path.moveTo(0.48f * s, side * 0.05f * s);
            path.quadTo(0.9f * s, side * (0.22f + t1) * s, 1.4f * s, side * (0.45f + t2) * s);
            c.drawPath(path, leg);
        }
        c.drawLine(-0.6f * s, -0.05f * s, -0.76f * s, -0.12f * s, leg);
        c.drawLine(-0.6f * s, 0.05f * s, -0.76f * s, 0.12f * s, leg);

        // bụng (lộ ra khi xòe cánh)
        c.drawOval(-0.6f * s, -0.2f * s, 0.15f * s, 0.2f * s, r.dark);

        // cánh màng vỗ nhanh
        if (r.open > 0.05f) {
            for (int side = -1; side <= 1; side += 2) {
                int sv = c.save();
                c.translate(0.12f * s, side * 0.04f * s);
                c.rotate(deg(side * (0.5f + 0.45f * sin(r.flap)) * r.open));
                if (side > 0) c.drawOval(-0.75f * s, 0, 0, 0.3f * s, membrane);
                else c.drawOval(-0.75f * s, -0.3f * s, 0, 0, membrane);
                c.restoreToCount(sv);
            }
        }

        // 2 cánh cứng, tách ra khi xòe
        for (int side = -1; side <= 1; side += 2) {
            int sv = c.save();
            c.translate(0.15f * s, side * 0.02f * s);
            c.rotate(deg(side * r.open * 0.45f));
            c.translate(-0.15f * s, -side * 0.02f * s);
            float cy = side * 0.12f * s;
            c.drawOval(-0.64f * s, cy - 0.155f * s, 0.2f * s, cy + 0.155f * s, side < 0 ? r.wingL : r.wingR);
            if (side > 0) c.drawOval(-0.48f * s, 0.1f * s, 0, 0.2f * s, hi);
            else c.drawOval(-0.48f * s, -0.2f * s, 0, -0.1f * s, hi);
            c.restoreToCount(sv);
        }
        seam.setStrokeWidth(0.015f * s);
        if (r.open < 0.2f) c.drawLine(0.12f * s, 0, -0.62f * s, 0, seam);

        // mai, đầu
        c.drawOval(0.08f * s, -0.22f * s, 0.42f * s, 0.22f * s, r.shield);
        rim.setStrokeWidth(0.02f * s);
        c.drawOval(0.08f * s, -0.22f * s, 0.42f * s, 0.22f * s, rim);
        c.drawOval(0.36f * s, -0.09f * s, 0.5f * s, 0.09f * s, head);

        c.restoreToCount(save);
    }
}
