package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Random;

/** Khung chung cho mọi hoạt cảnh. Tọa độ tính bằng điểm ảnh, dp để đổi cỡ. */
public abstract class Scene {
    public final float dp;
    protected final float mult;
    public int w, h;
    protected final Random rnd = new Random();
    protected float time = 0f;
    /** Chế độ đồng hành: vài con nhỏ lướt qua rồi đi luôn, không quay lại khi chạm mép, không vẽ nền. */
    public boolean companion = false;

    protected Scene(float dp, float mult) {
        this.dp = dp;
        this.mult = mult;
    }

    public final void resize(int w, int h) {
        boolean first = this.w == 0;
        this.w = w;
        this.h = h;
        if (first) init(); else onResize();
    }

    public final void update(float dt) {
        time += dt;
        step(dt);
    }

    protected abstract void init();
    protected void onResize() {}
    protected abstract void step(float dt);
    public abstract void draw(Canvas c);

    /** Chạm 1 cái vào màn chờ: con vật gần đó giật mình chạy đi. */
    public void touch(float x, float y) {}

    /** Làm con vật đang ở gần (tx, ty) hoảng hốt chạy ra xa. Trả về true nếu bị dọa. */
    protected boolean scare(Walker c, float tx, float ty, float radius, float speed) {
        float d = (float) Math.hypot(c.x - tx, c.y - ty);
        if (d > radius) return false;
        c.mood = DASH;
        c.timer = rand(0.6f, 1.3f);
        c.target = speed;
        c.ang = (float) Math.atan2(c.y - ty, c.x - tx) + rand(-0.4f, 0.4f);
        c.turn = 0;
        c.esc = false;
        c.pivot = 0;
        return true;
    }

    // ------------------------------------------------ tiện ích

    protected float rand(float a, float b) { return a + rnd.nextFloat() * (b - a); }

    protected int count(int base) { return Math.max(1, Math.round(base * mult)); }

    protected static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    protected static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }

    /** Góc chênh lệch thu về khoảng -PI..PI */
    protected static float angDiff(float target, float cur) {
        float d = target - cur;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    protected static float deg(float rad) { return (float) Math.toDegrees(rad); }

    protected static float sin(float a) { return (float) Math.sin(a); }

    protected static float cos(float a) { return (float) Math.cos(a); }

    protected boolean offscreen(float x, float y, float r) {
        return x < -r || y < -r || x > w + r || y > h + r;
    }

    /** Trộn 2 màu ARGB */
    protected static int mix(int c1, int c2, float t) {
        int a = Math.round(lerp(c1 >>> 24, c2 >>> 24, t));
        int r = Math.round(lerp((c1 >> 16) & 255, (c2 >> 16) & 255, t));
        int g = Math.round(lerp((c1 >> 8) & 255, (c2 >> 8) & 255, t));
        int b = Math.round(lerp(c1 & 255, c2 & 255, t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    protected static int withAlpha(int c, float a) {
        return (Math.round(a * 255) << 24) | (c & 0xFFFFFF);
    }

    /** Màu theo sắc độ: h 0..360, s, v, a 0..1 */
    protected static int hsv(float h, float s, float v, float a) {
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c, r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        return (Math.round(a * 255) << 24) | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    // ------------------------------------------------ tâm trạng: chạy vụt / đi / chậm / đứng nghe ngóng

    public static final int DASH = 0, WALK = 1, SLOW = 2, FREEZE = 3, TURN = 4, FLUTTER = 5;

    /** Một dòng trong bảng tâm trạng: kiểu, trọng số, tốc độ (dp/s), thời gian (giây). */
    protected static final class Mood {
        final int kind; final float wt, vmin, vmax, tmin, tmax;
        Mood(int kind, float wt, float vmin, float vmax, float tmin, float tmax) {
            this.kind = kind; this.wt = wt; this.vmin = vmin; this.vmax = vmax; this.tmin = tmin; this.tmax = tmax;
        }
    }

    /** Con vật biết tự đổi tâm trạng và quay lại khi chạm mép. */
    protected static class Walker {
        public float x, y, ang, v, target, timer, turn, look, pivot, escAng;
        public int mood = WALK;
        public boolean esc;
    }

    protected void nextMood(Walker c, Mood[] table) {
        float tot = 0;
        for (Mood m : table) tot += m.wt;
        float r = rnd.nextFloat() * tot;
        Mood pick = table[0];
        for (Mood m : table) {
            r -= m.wt;
            if (r <= 0) { pick = m; break; }
        }
        if (pick.kind == c.mood && pick.kind != FREEZE) {
            int i = 0;
            while (table[i] != pick) i++;
            pick = table[(i + 1) % table.length];
        }
        c.mood = pick.kind;
        c.timer = rand(pick.tmin, pick.tmax);
        c.target = rand(pick.vmin, pick.vmax) * dp;
        c.look = rand(0, 6.28f);
        c.pivot = (pick.kind == FREEZE && rnd.nextFloat() < 0.45f) ? rand(-1.6f, 1.6f) : 0f;
        if (pick.kind == DASH) c.turn += rand(-2.5f, 2.5f);
    }

    /**
     * Chạm mép thì quay lại theo một hướng mới ngẫu nhiên (không phải về giữa màn).
     * Trả về góc mới; c.esc = true trong lúc đang quay.
     */
    protected float edge(Walker c, float ang, float margin, float rate, float dt) {
        if (companion) { // lướt qua rồi đi luôn
            c.esc = false;
            return ang;
        }
        int nx = 0, ny = 0;
        if (c.x < margin) nx = 1; else if (c.x > w - margin) nx = -1;
        if (c.y < margin) ny = 1; else if (c.y > h - margin) ny = -1;
        if ((nx != 0 || ny != 0) && !c.esc && cos(ang) * nx + sin(ang) * ny < 0.25f) {
            c.escAng = (float) Math.atan2(ny, nx) + rand(-1.1f, 1.1f);
            c.esc = true;
        }
        if (c.esc) {
            float d = angDiff(c.escAng, ang), m = rate * dt;
            ang += clamp(d, -m, m);
            if (Math.abs(d) < 0.06f) c.esc = false;
        }
        return ang;
    }

    protected boolean bounce(Walker c, float margin, float rate, float dt) {
        c.ang = edge(c, c.ang, margin, rate, dt);
        if (c.esc) c.turn *= 0.9f;
        return c.esc;
    }

    /** Đi theo tâm trạng, lượn ngẫu nhiên, chạm mép quay lại. Trả về quãng đường đi được. */
    protected float moveWalker(Walker c, float dt, Mood[] table, float accel, float wander,
                               float margin, float edgeRate) {
        c.timer -= dt;
        if (c.timer <= 0) nextMood(c, table);
        if (c.v < c.target) c.v = Math.min(c.target, c.v + accel * dt);
        else c.v = Math.max(c.target, c.v - accel * 1.6f * dt);
        if (!bounce(c, margin, edgeRate, dt)) {
            if (c.mood == FREEZE) {
                c.ang += c.pivot * dt; // xoay người tại chỗ
                c.pivot *= 0.96f;
            } else {
                c.turn += rand(-1f, 1f) * wander * dt;
                c.turn *= 0.985f;
                c.turn = clamp(c.turn, -2.2f, 2.2f);
                c.ang += c.turn * dt;
            }
        }
        float d = c.v * dt;
        c.x += cos(c.ang) * d;
        c.y += sin(c.ang) * d;
        return d;
    }

    /** Điểm xuất phát ngoài mép màn, hướng vào trong. Trả về {x, y, ang} */
    protected float[] edgeStart(float extra) {
        return edgeStart(extra, rnd.nextInt(4));
    }

    protected float[] edgeStart(float extra, int e) {
        float x, y;
        switch (e) {
            case 0: x = rand(0, w); y = -extra; break;
            case 1: x = w + extra; y = rand(0, h); break;
            case 2: x = rand(0, w); y = h + extra; break;
            default: x = -extra; y = rand(0, h); break;
        }
        float ang = (float) Math.atan2(h / 2f + rand(-h / 3f, h / 3f) - y, w / 2f + rand(-w / 3f, w / 3f) - x);
        return new float[]{x, y, ang};
    }

    // ------------------------------------------------ vệt đường: thân/đuôi bám theo đường đi của đầu

    protected static final class Trail {
        final ArrayList<float[]> pts = new ArrayList<>(); // {x, y, ang, s}
        float s = 0, last = -1e9f;

        void push(float x, float y, float a, float step) {
            if (s - last >= step || pts.isEmpty()) {
                pts.add(new float[]{x, y, a, s});
                last = s;
            }
        }

        void trim(float minS) {
            while (pts.size() > 2 && pts.get(1)[3] < minS) pts.remove(0);
        }

        float first() { return pts.isEmpty() ? 0 : pts.get(0)[3]; }

        boolean sample(float s, float[] out) {
            if (pts.isEmpty() || s < pts.get(0)[3]) return false;
            int lo = 0, hi = pts.size() - 1;
            if (s >= pts.get(hi)[3]) {
                float[] q = pts.get(hi);
                out[0] = q[0]; out[1] = q[1]; out[2] = q[2];
                return true;
            }
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (pts.get(mid)[3] <= s) lo = mid; else hi = mid;
            }
            float[] a = pts.get(lo), b = pts.get(hi);
            float k = (s - a[3]) / Math.max(0.0001f, b[3] - a[3]);
            out[0] = lerp(a[0], b[0], k);
            out[1] = lerp(a[1], b[1], k);
            out[2] = a[2] + angDiff(b[2], a[2]) * k;
            return true;
        }
    }
}
