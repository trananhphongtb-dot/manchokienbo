package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Chuột (nhìn từ trên xuống), khoảng 5 con: xám, nâu, trắng, đen; to nhỏ khác nhau.
 * Bốn chân bước chéo, đuôi dài uốn lượn, tai giật giật, râu rung, đứng lại quay đầu ngó.
 */
public class MouseScene extends Scene {

    private static final Mood[] MOODS = {
            new Mood(DASH, 25, 200, 260, 0.4f, 1.3f),
            new Mood(WALK, 25, 90, 140, 1f, 2.5f),
            new Mood(SLOW, 15, 22, 40, 1f, 2.5f),
            new Mood(FREEZE, 35, 0, 0, 1.2f, 3.5f)};
    // lông sáng, giữa, tối, tai
    private static final int[][] FUR = {
            {0xFFC2BDB8, 0xFF8C8782, 0xFF5E5955, 0xFF8F8984},
            {0xFFC8A98E, 0xFF8D6E58, 0xFF5D4335, 0xFF8D6E58},
            {0xFFFFFFFF, 0xFFEDE7E3, 0xFFC9BFB9, 0xFFE8DCD8},
            {0xFF8A8580, 0xFF5A5652, 0xFF34312E, 0xFF5A5652}};
    private static final float[][] LEGS = {{0.16f, 1, 0}, {0.16f, -1, 3.1416f}, {-0.28f, 1, 3.1416f}, {-0.28f, -1, 0}};

    private static class Mouse extends Walker {
        float gait, s, tail, sniff, head, ear, earT;
        int[] fur;
        Paint bodyP, headP, earP, legP, stripe;
    }

    private final ArrayList<Mouse> list = new ArrayList<>();
    private final Paint tailP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint earIn = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eye = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeHi = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nose = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whisk = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public MouseScene(float dp, float mult) {
        super(dp, mult);
        tailP.setColor(0xFFD3AAA4);
        tailP.setStyle(Paint.Style.STROKE);
        tailP.setStrokeCap(Paint.Cap.ROUND);
        pink.setColor(0xFFE8B1AE);
        earIn.setColor(0xFFE9A7A7);
        eye.setColor(0xFF111111);
        eyeHi.setColor(0xDDFFFFFF);
        nose.setColor(0xFFE88C95);
        whisk.setColor(0xA6FFFFFF);
        whisk.setStyle(Paint.Style.STROKE);
        shadow.setColor(0x30000000);
    }

    @Override protected void init() {
        for (int i = 0; i < count(5); i++) {
            Mouse m = new Mouse();
            float[] st = edgeStart(rand(40, 300) * dp);
            m.x = st[0]; m.y = st[1]; m.ang = st[2];
            m.s = rand(40, 68) * dp;
            m.tail = rand(0, 6);
            m.earT = rand(0.5f, 3);
            m.mood = WALK;
            m.timer = rand(1, 2.5f);
            m.target = 120 * dp;
            m.fur = FUR[i % FUR.length];
            float s = m.s;
            int[] F = m.fur;
            m.bodyP = new Paint(Paint.ANTI_ALIAS_FLAG);
            m.bodyP.setShader(new RadialGradient(-0.12f * s, -0.1f * s, 0.5f * s,
                    new int[]{F[0], F[1], F[2]}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            m.headP = new Paint(Paint.ANTI_ALIAS_FLAG);
            m.headP.setShader(new RadialGradient(0.3f * s, -0.07f * s, 0.35f * s,
                    new int[]{F[0], F[1], F[2]}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            m.earP = new Paint(Paint.ANTI_ALIAS_FLAG);
            m.earP.setColor(F[3]);
            m.legP = new Paint(Paint.ANTI_ALIAS_FLAG);
            m.legP.setColor(F[1]);
            m.legP.setStyle(Paint.Style.STROKE);
            m.legP.setStrokeCap(Paint.Cap.ROUND);
            m.stripe = new Paint(Paint.ANTI_ALIAS_FLAG);
            m.stripe.setColor(withAlpha(F[2], 0.25f));
            list.add(m);
        }
    }

    @Override protected void step(float dt) {
        for (Mouse m : list) {
            m.timer -= dt;
            if (m.timer <= 0) nextMood(m, MOODS);
            float acc = m.mood == DASH ? 1000 * dp : 500 * dp;
            if (m.v < m.target) m.v = Math.min(m.target, m.v + acc * dt);
            else m.v = Math.max(m.target, m.v - 1400 * dp * dt);

            if (!bounce(m, 35 * dp, 6f, dt)) {
                if (m.v > 1) {
                    m.turn += rand(-4f, 4f) * dt;
                    m.turn *= 0.97f;
                    float zig = m.mood == SLOW ? sin(time * 5f + m.s) * 1.2f : sin(time * 9f + m.s) * 0.5f;
                    m.ang += (m.turn + zig) * dt;
                } else {
                    m.ang += m.pivot * dt;
                    m.pivot *= 0.95f;
                }
            }
            m.x += cos(m.ang) * m.v * dt;
            m.y += sin(m.ang) * m.v * dt;
            m.gait += m.v * dt / (m.s * 0.11f);
            m.tail += dt * (1.2f + m.v / (45 * dp));
            boolean still = m.mood == FREEZE || m.mood == SLOW;
            m.sniff += dt * (still ? 18f : 4f);
            float wantHead = m.mood == FREEZE
                    ? sin(time * 1.4f + m.look) * 0.5f + sin(time * 3.7f + m.look) * 0.12f : 0f;
            m.head = lerp(m.head, wantHead, Math.min(1f, dt * 6f));
            // tai giật nhẹ
            m.earT -= dt;
            if (m.earT <= 0) {
                m.ear = 1;
                m.earT = still ? rand(0.4f, 1.6f) : rand(1.5f, 4f);
            }
            m.ear = Math.max(0, m.ear - dt * 4);
        }
    }

    @Override public void draw(Canvas c) {
        for (Mouse m : list) drawMouse(c, m);
    }

    private void drawMouse(Canvas c, Mouse m) {
        float s = m.s;
        if (offscreen(m.x, m.y, s * 2)) return;
        int save = c.save();
        c.translate(m.x, m.y);
        c.rotate(deg(m.ang));

        c.drawOval(-0.42f * s, -0.16f * s, 0.68f * s, 0.4f * s, shadow);

        // đuôi dài uốn lượn, càng về chóp càng vẫy mạnh
        boolean moving = m.v > 1;
        float amp = moving ? 1f : 0.6f;
        int segs = 14;
        float px = -0.45f * s, py = 0;
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs, nx = -0.45f * s - t * 1.05f * s;
            float ny = sin(m.tail * 2.2f - t * 4f) * t * 0.22f * s * amp + sin(m.tail * 0.7f) * t * t * 0.1f * s;
            tailP.setStrokeWidth((0.05f - 0.035f * t) * s);
            c.drawLine(px, py, nx, ny, tailP);
            px = nx;
            py = ny;
        }

        // 4 chân bước chéo (trước trái + sau phải)
        m.legP.setStrokeWidth(0.07f * s);
        for (float[] L : LEGS) {
            float lx = L[0], side = L[1], ph = L[2];
            float st = moving ? sin(m.gait + ph) : 0f;
            float lift = moving ? Math.max(0f, cos(m.gait + ph)) : 0f;
            float fx = (lx + st * 0.1f) * s, fy = side * (0.3f + 0.02f * lift) * s;
            c.drawLine(lx * s, side * 0.18f * s, fx, fy, m.legP);
            float pr = (0.055f - 0.012f * lift) * s;
            c.drawOval(fx - pr, fy - pr * 0.8f, fx + pr * 1.1f, fy + pr * 0.8f, pink);
            for (int k = -1; k <= 1; k++) c.drawCircle(fx + pr * 1.1f, fy + k * pr * 0.6f, pr * 0.32f, pink);
        }

        // thân
        c.drawOval(-0.52f * s, -0.27f * s, 0.26f * s, 0.27f * s, m.bodyP);
        c.drawOval(-0.36f * s, -0.04f * s, 0.06f * s, 0.04f * s, m.stripe);

        // đầu: quay quanh cổ, nhấp nhô khi hít ngửi
        c.translate(0.12f * s, 0);
        c.rotate(deg(m.head));
        c.translate(-0.12f * s, 0);
        boolean still = m.mood == FREEZE || m.mood == SLOW;
        c.translate(still ? sin(m.sniff) * 0.02f * s : 0f, 0);

        // tai giật ra sau rồi trở lại
        for (int side = -1; side <= 1; side += 2) {
            int sv = c.save();
            c.translate(0.2f * s, side * 0.2f * s);
            c.rotate(deg(side * (m.ear * 0.5f + (still ? sin(time * 2f + side) * 0.08f : 0f))));
            c.drawCircle(0, side * 0.03f * s, 0.13f * s, m.earP);
            c.drawCircle(0.01f * s, side * 0.03f * s, 0.085f * s, earIn);
            c.restoreToCount(sv);
        }

        path.reset();
        path.moveTo(0.1f * s, -0.2f * s);
        path.quadTo(0.42f * s, -0.22f * s, 0.63f * s, 0);
        path.quadTo(0.42f * s, 0.22f * s, 0.1f * s, 0.2f * s);
        path.close();
        c.drawPath(path, m.headP);

        c.drawCircle(0.42f * s, -0.095f * s, 0.037f * s, eye);
        c.drawCircle(0.42f * s, 0.095f * s, 0.037f * s, eye);
        c.drawCircle(0.43f * s, -0.105f * s, 0.013f * s, eyeHi);
        c.drawCircle(0.43f * s, 0.085f * s, 0.013f * s, eyeHi);
        c.drawCircle(0.63f * s, 0, 0.038f * s, nose);

        whisk.setStrokeWidth(0.009f * s);
        float tw = sin(m.sniff * 1.3f) * 0.035f * s;
        for (int side = -1; side <= 1; side += 2) {
            c.drawLine(0.58f * s, side * 0.03f * s, 0.8f * s, side * 0.09f * s + tw, whisk);
            c.drawLine(0.58f * s, side * 0.04f * s, 0.76f * s, side * 0.21f * s + tw, whisk);
            c.drawLine(0.57f * s, side * 0.05f * s, 0.67f * s, side * 0.28f * s - tw, whisk);
        }
        c.restoreToCount(save);
    }
}
