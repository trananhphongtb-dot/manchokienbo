package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;

/**
 * Rắn bò ngoằn ngoèo (nhìn từ trên xuống): cạp nong, lục xanh, trăn gấm, san hô, hổ mang, rắn ráo.
 * Lúc trườn, lúc chậm, lúc trườn nhanh, lúc dừng thè lưỡi nghe ngóng (hổ mang phùng mang).
 */
public class SnakeScene extends Scene {

    private static final Mood[] MOODS = {
            new Mood(WALK, 45, 40, 70, 2, 6),
            new Mood(SLOW, 20, 15, 25, 1.5f, 3.5f),
            new Mood(FREEZE, 25, 0, 0, 1.2f, 3.5f),
            new Mood(DASH, 10, 110, 150, 0.6f, 1.4f)};

    private static class Snake extends Walker {
        final Trail trail = new Trail();
        int type, N;
        float L, W, seg, wave, base, hood, tongue, tongueT;
        float[][] pts;
        int[] colors;
    }

    private final ArrayList<Snake> list = new ArrayList<>();
    private final Paint seg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hoodP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tongueP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headShine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] tmp = new float[3];

    public SnakeScene(float dp, float mult) {
        super(dp, mult);
        shadow.setColor(0x1F000000);
        shine.setColor(0x21FFFFFF);
        tongueP.setColor(0xFFE53935);
        tongueP.setStyle(Paint.Style.STROKE);
        tongueP.setStrokeCap(Paint.Cap.ROUND);
        pupil.setColor(0xFF111111);
        headShine.setColor(0x1FFFFFFF);
    }

    @Override protected void init() {
        for (int i = 0; i < count(5); i++) {
            Snake s = new Snake();
            float[] st = edgeStart(rand(20, 150) * dp);
            s.x = st[0]; s.y = st[1]; s.ang = st[2]; s.base = st[2];
            s.type = i % 6;
            s.L = rand(130, 240) * dp;
            s.W = s.L * rand(0.04f, 0.055f) * (s.type == 2 ? 1.35f : 1f);
            s.N = 46;
            s.seg = s.L / (s.N - 1);
            s.trail.push(s.x, s.y, s.ang, 0);
            s.wave = rand(0, 6.28f);
            s.tongueT = 1;
            s.mood = WALK;
            s.timer = rand(1, 4);
            s.v = s.target = 50 * dp;
            s.pts = new float[s.N][3];
            s.colors = new int[s.N];
            for (int k = 0; k < s.N; k++) s.colors[k] = segColor(s, k);
            list.add(s);
        }
    }

    private int segColor(Snake s, int i) {
        float t = i / (float) (s.N - 1);
        switch (s.type) {
            case 0: return (i / 3) % 2 == 1 ? 0xFF161616 : 0xFFF4C430;            // cạp nong: khoang đen vàng
            case 1: return mix(0xFF43A047, 0xFF9CCC65, 0.5f + 0.5f * sin(i * 0.5f)); // lục xanh
            case 2: {                                                              // trăn gấm
                boolean b = (sin(i * 1.3f) + sin(i * 0.47f + 1f)) > 0.6f;
                return b ? 0xFF6D4C2F : 0xFFC9A66B;
            }
            case 3: {                                                              // san hô
                int k = i % 8;
                return k < 3 ? 0xFFD32F2F : (k == 3 || k == 7 ? 0xFFFFEB3B : 0xFF111111);
            }
            case 4: return mix(0xFF5D5A3C, 0xFF3E3A28, t);                         // hổ mang
            default: return i % 4 == 0 ? 0xFF4E4A2E : 0xFF7B7448;                  // rắn ráo
        }
    }

    private float width(Snake s, int i) {
        float t = i / (float) (s.N - 1);
        float p;
        if (t < 0.06f) p = 0.85f;
        else if (t < 0.1f) p = 0.7f;
        else if (t < 0.45f) p = lerp(0.75f, 1f, (t - 0.1f) / 0.35f);
        else p = 1f - (float) Math.pow((t - 0.45f) / 0.55f, 1.6) * 0.9f;
        if (s.type == 4 && t > 0.05f && t < 0.2f) p *= 1 + s.hood * 1.3f * sin((t - 0.05f) / 0.15f * 3.1416f); // phùng mang
        return s.W * p;
    }

    @Override protected void step(float dt) {
        for (Snake s : list) {
            s.timer -= dt;
            if (s.timer <= 0) nextMood(s, MOODS);
            if (s.v < s.target) s.v = Math.min(s.target, s.v + 200 * dp * dt);
            else s.v = Math.max(s.target, s.v - 300 * dp * dt);
            boolean moving = s.v > 2 * dp;
            s.base = edge(s, s.base, 45 * dp, 2f, dt);
            if (!s.esc && moving) {
                s.turn += rand(-1f, 1f) * 1.2f * dt;
                s.turn *= 0.99f;
                s.turn = clamp(s.turn, -0.6f, 0.6f);
                s.base += s.turn * dt;
            }
            if (moving) s.wave += dt * s.v / (s.L * 0.11f);
            s.ang = s.base + sin(s.wave) * 0.75f;
            float d = s.v * dt;
            s.x += cos(s.ang) * d;
            s.y += sin(s.ang) * d;
            s.trail.s += d;
            s.trail.push(s.x, s.y, s.ang, 1.5f * dp);
            s.trail.trim(s.trail.s - s.L - 20 * dp);
            s.hood = lerp(s.hood, s.mood == FREEZE ? 1f : 0f, Math.min(1f, dt * 3f));
            s.tongueT -= dt;
            if (s.tongueT <= 0) {
                s.tongue = 0.35f;
                s.tongueT = s.mood == FREEZE ? rand(0.4f, 1f) : rand(1.5f, 3.5f);
            }
            s.tongue = Math.max(0, s.tongue - dt);
        }
    }

    @Override public void draw(Canvas c) {
        for (Snake s : list) {
            if (offscreen(s.x, s.y, s.L * 1.2f)) continue;
            int n = 0;
            for (int i = 0; i < s.N; i++) {
                if (!s.trail.sample(Math.max(s.trail.s - i * s.seg, s.trail.first()), tmp)) break;
                s.pts[i][0] = tmp[0];
                s.pts[i][1] = tmp[1];
                s.pts[i][2] = tmp[2];
                n++;
            }
            if (n == 0) continue;
            for (int i = n - 1; i >= 0; i -= 2) {
                c.drawCircle(s.pts[i][0] + 3 * dp, s.pts[i][1] + 5 * dp, width(s, i), shadow);
            }
            for (int i = n - 1; i >= 1; i--) {
                seg.setColor(s.colors[i]);
                c.drawCircle(s.pts[i][0], s.pts[i][1], width(s, i), seg);
            }
            // ánh bóng lưng
            for (int i = n - 1; i >= 1; i--) {
                float w = width(s, i);
                c.drawCircle(s.pts[i][0] - w * 0.3f, s.pts[i][1] - w * 0.3f, w * 0.38f, shine);
            }
            if (s.type == 4 && s.hood > 0.3f) {
                hoodP.setColor(withAlpha(0xFFF0E6BE, 0.35f * s.hood));
                for (int i = 3; i < 9 && i < n; i++) c.drawCircle(s.pts[i][0], s.pts[i][1], width(s, i) * 0.45f, hoodP);
            }
            drawHead(c, s, s.pts[0]);
        }
    }

    private void drawHead(Canvas c, Snake s, float[] h) {
        float W = s.W;
        int save = c.save();
        c.translate(h[0], h[1]);
        c.rotate(deg(h[2]));
        if (s.tongue > 0) {
            float ext = sin(s.tongue / 0.35f * 3.1416f) * W * 2.2f, fl = sin(time * 40f) * 0.25f;
            tongueP.setStrokeWidth(Math.max(1f, W * 0.12f));
            c.drawLine(W, 0, W + ext, 0, tongueP);
            c.drawLine(W + ext, 0, W + ext + W * 0.5f, -W * (0.25f + fl), tongueP);
            c.drawLine(W + ext, 0, W + ext + W * 0.5f, W * (0.25f - fl), tongueP);
        }
        int hc = (s.type == 0 || s.type == 3) ? 0xFF161616 : s.colors[1];
        seg.setColor(hc);
        if (s.type == 1 || s.type == 2) { // đầu tam giác
            path.reset();
            path.moveTo(W * 1.5f, 0);
            path.quadTo(W * 1.2f, -W * 1.05f, -W * 0.5f, -W * 0.95f);
            path.quadTo(-W * 0.9f, 0, -W * 0.5f, W * 0.95f);
            path.quadTo(W * 1.2f, W * 1.05f, W * 1.5f, 0);
            path.close();
            c.drawPath(path, seg);
        } else {
            c.drawOval(W * 0.35f - W * 1.15f, -W * 0.8f, W * 0.35f + W * 1.15f, W * 0.8f, seg);
        }
        c.drawCircle(W * 0.1f, -W * 0.3f, W * 0.35f, headShine);
        eyeP.setColor(s.type == 1 ? 0xFFFFEB3B : 0xFFE0C060);
        for (int side = -1; side <= 1; side += 2) {
            c.drawCircle(W * 0.75f, side * W * 0.55f, W * 0.24f, eyeP);
            c.drawOval(W * 0.71f, side * W * 0.55f - W * 0.16f, W * 0.79f, side * W * 0.55f + W * 0.16f, pupil);
        }
        c.restoreToCount(save);
    }
}
