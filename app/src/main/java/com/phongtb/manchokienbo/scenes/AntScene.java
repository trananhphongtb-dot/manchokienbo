package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Đàn kiến khoảng 100 con: 5 hàng nối đuôi + 10 con trinh sát đi lẻ.
 * Con đầu hàng lúc đi, lúc chậm, lúc dừng ngó nghiêng; con theo sau tự giữ khoảng cách,
 * có con dừng chạm râu rồi chạy bắt kịp. To nhỏ và màu (đen, nâu, đỏ) khác nhau.
 */
public class AntScene extends Scene {

    private static final Mood[] LEAD = {
            new Mood(WALK, 55, 34, 46, 2, 6),
            new Mood(SLOW, 20, 12, 22, 1, 3),
            new Mood(FREEZE, 25, 0, 0, 0.6f, 2.2f)};
    private static final Mood[] SCOUT = {
            new Mood(WALK, 35, 35, 55, 1, 4),
            new Mood(DASH, 15, 80, 115, 0.4f, 1.2f),
            new Mood(SLOW, 15, 10, 20, 1, 2),
            new Mood(FREEZE, 35, 0, 0, 0.8f, 2.6f)};
    private static final int[] COLORS = {0xFF1C110B, 0xFF1C110B, 0xFF241812, 0xFF5A1E0E, 0xFF3B2414};

    private static class Column extends Walker {
        final Trail trail = new Trail();
        float base;
        int n;
        float[] fs, fv, fp, sc, ph;
        int[] cl;
        boolean[] crumb;
    }

    private static class Scout extends Walker {
        float dist, sc;
        int color;
    }

    private final ArrayList<Column> cols = new ArrayList<>();
    private final ArrayList<Scout> scouts = new ArrayList<>();
    private float size, spacing, minGap;

    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crumbP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] lines = new float[48];
    private final float[] tmp = new float[3];

    public AntScene(float dp, float mult) {
        super(dp, mult);
        leg.setStyle(Paint.Style.STROKE);
        leg.setStrokeCap(Paint.Cap.ROUND);
        gloss.setColor(0x40FFFFFF);
        shadow.setColor(0x29000000);
        crumbP.setColor(0xFFF3E2B3);
    }

    @Override protected void init() {
        size = 13 * dp;
        spacing = size * 1.3f;
        minGap = size * 0.9f;
        int nc = count(5), per = count(17);
        for (int i = 0; i < nc; i++) cols.add(makeColumn(per, i * 2.5f));
        for (int i = 0; i < count(10); i++) {
            Scout s = new Scout();
            float[] st = edgeStart(rand(20, 260) * dp);
            s.x = st[0]; s.y = st[1]; s.ang = st[2];
            s.timer = rand(1, 3);
            s.target = 45 * dp;
            s.sc = rand(0.75f, 1.2f);
            s.color = COLORS[rnd.nextInt(COLORS.length)];
            scouts.add(s);
        }
    }

    private Column makeColumn(int n, float delay) {
        Column t = new Column();
        float[] st = edgeStart(20 * dp + delay * 40 * dp);
        t.x = st[0]; t.y = st[1]; t.ang = st[2];
        t.n = n;
        t.base = rand(34, 46) * dp;
        t.timer = rand(2, 5);
        t.target = t.base;
        int colony = COLORS[rnd.nextInt(COLORS.length)];
        t.fs = new float[n + 1]; t.fv = new float[n + 1]; t.fp = new float[n + 1];
        t.sc = new float[n + 1]; t.ph = new float[n + 1]; t.cl = new int[n + 1]; t.crumb = new boolean[n + 1];
        for (int i = 0; i <= n; i++) {
            t.fs[i] = -spacing * i;
            t.fv[i] = t.base;
            t.sc[i] = rand(0.75f, 1.2f);
            t.crumb[i] = i > 0 && rnd.nextFloat() < 0.28f;
            t.ph[i] = rand(0, 6.28f);
            t.cl[i] = rnd.nextFloat() < 0.85f ? colony : COLORS[rnd.nextInt(COLORS.length)];
        }
        t.trail.push(t.x, t.y, t.ang, 0);
        return t;
    }

    @Override protected void step(float dt) {
        for (Column t : cols) {
            float d = moveWalker(t, dt, LEAD, 120 * dp, 4f, 30 * dp, 3.2f);
            t.trail.s += d;
            t.fs[0] = t.trail.s;
            t.trail.push(t.x, t.y, t.ang, 1.5f * dp);
            for (int i = 1; i <= t.n; i++) {
                float gap = t.fs[i - 1] - t.fs[i];
                if (t.fp[i] > 0) {
                    t.fp[i] -= dt;
                    if (gap > spacing * 2.6f) t.fp[i] = 0;
                } else if (rnd.nextFloat() < dt * 0.07f) {
                    t.fp[i] = rand(0.3f, 1.3f); // dừng chạm râu
                }
                float want = t.fp[i] > 0 ? 0f : t.base * clamp((gap - minGap) / (spacing - minGap), 0f, 1.7f);
                t.fv[i] += clamp(want - t.fv[i], -260 * dp * dt, 180 * dp * dt);
                t.fs[i] = Math.min(t.fs[i] + t.fv[i] * dt, t.fs[i - 1] - minGap * 0.8f);
            }
            t.trail.trim(t.fs[t.n] - 10 * dp);
        }
        for (Scout s : scouts) s.dist += moveWalker(s, dt, SCOUT, 300 * dp, 4f, 25 * dp, 4f);
    }

    @Override public void draw(Canvas c) {
        float T = time;
        for (Column t : cols) {
            for (int i = t.n; i >= 1; i--) {
                if (!t.trail.sample(t.fs[i], tmp)) continue;
                boolean moving = t.fv[i] > 3 * dp;
                float wig = moving ? sin(T * 3.2f + t.ph[i]) * 2.2f * dp : 0f;
                float head = moving ? sin(T * 3.2f + t.ph[i] + 1f) * 0.12f : sin(T * 2.3f + t.ph[i]) * 0.35f;
                drawAnt(c, tmp[0] - sin(tmp[2]) * wig, tmp[1] + cos(tmp[2]) * wig,
                        tmp[2] + (moving ? head : 0f), t.fs[i] / (size * 0.33f), size * t.sc[i],
                        t.cl[i], t.crumb[i], moving ? 0f : head, !moving);
            }
            boolean still = t.v < 3 * dp;
            drawAnt(c, t.x, t.y, t.ang, t.trail.s / (size * 0.33f), size * t.sc[0], t.cl[0], false,
                    still ? sin(T * 1.8f + t.look) * 0.4f : 0f, still);
        }
        for (Scout s : scouts) {
            boolean still = s.v < 3 * dp;
            drawAnt(c, s.x, s.y, s.ang, s.dist / (size * 0.33f), size * s.sc, s.color, false,
                    still ? sin(T * 1.8f + s.look) * 0.45f : 0f, still);
        }
    }

    /** headTurn: góc quay đầu ngó; alert: râu ngoe nguẩy nhanh khi đứng nghe ngóng */
    private void drawAnt(Canvas c, float x, float y, float ang, float phase, float s, int color,
                         boolean crumb, float headTurn, boolean alert) {
        if (offscreen(x, y, s * 2)) return;
        body.setColor(color);
        leg.setColor(color);
        int save = c.save();
        c.translate(x, y);
        c.rotate(deg(ang));

        c.drawOval(-0.6f * s, -0.12f * s, 0.54f * s, 0.36f * s, shadow);

        leg.setStrokeWidth(0.055f * s);
        int k = 0;
        for (int side = -1; side <= 1; side += 2) {
            for (int j = -1; j <= 1; j++) {
                float sw = sin(phase + (((j == 0) == (side == 1)) ? 0f : 3.1416f));
                float bx = j * 0.1f;
                float kx = (bx + j * 0.13f + sw * 0.09f) * s;
                float ky = side * 0.26f * s;
                lines[k++] = bx * s;
                lines[k++] = side * 0.05f * s;
                lines[k++] = kx;
                lines[k++] = ky;
                lines[k++] = kx;
                lines[k++] = ky;
                lines[k++] = (bx + j * 0.3f + sw * 0.15f) * s;
                lines[k++] = side * 0.4f * s;
            }
        }
        c.drawLines(lines, leg);

        c.drawOval(-0.72f * s, -0.21f * s, -0.12f * s, 0.21f * s, body);
        c.drawOval(-0.16f * s, -0.045f * s, -0.04f * s, 0.045f * s, body);
        c.drawOval(-0.08f * s, -0.1f * s, 0.18f * s, 0.1f * s, body);
        c.drawOval(-0.55f * s, -0.14f * s, -0.32f * s, -0.04f * s, gloss);

        c.translate(0.17f * s, 0);
        c.rotate(deg(headTurn));
        c.translate(-0.17f * s, 0);
        float aw = alert ? sin(time * 11f + x) * 0.07f * s : sin(phase * 0.5f) * 0.05f * s;
        float aw2 = alert ? sin(time * 9f + y) * 0.07f * s : -aw;
        leg.setStrokeWidth(0.035f * s);
        c.drawLine(0.36f * s, -0.05f * s, 0.5f * s, -0.2f * s, leg);
        c.drawLine(0.5f * s, -0.2f * s, 0.68f * s, -0.14f * s + aw, leg);
        c.drawLine(0.36f * s, 0.05f * s, 0.5f * s, 0.2f * s, leg);
        c.drawLine(0.5f * s, 0.2f * s, 0.68f * s, 0.14f * s + aw2, leg);
        c.drawCircle(0.29f * s, 0, 0.13f * s, body);
        c.drawCircle(0.31f * s, -0.05f * s, 0.04f * s, gloss);

        if (crumb) c.drawCircle(0.5f * s, 0, 0.13f * s, crumbP);
        c.restoreToCount(save);
    }
}
