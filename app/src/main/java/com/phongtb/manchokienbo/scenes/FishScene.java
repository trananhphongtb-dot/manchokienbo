package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Hồ cá Koi nhìn từ trên xuống: nhiều loại Koi (Kohaku, Sanke, Ogon, Showa, Asagi, Tancho),
 * cá vàng đuôi dài, đàn cá nhỏ. Thân uốn lượn, đuôi vẫy, vây ngực phẩy;
 * lá súng, hoa súng nổi trên mặt nước, gợn sóng lan tròn, vệt sáng lăn tăn.
 */
public class FishScene extends Scene {

    private static final Mood[] MOODS = {
            new Mood(WALK, 50, 35, 65, 3, 7),
            new Mood(SLOW, 25, 5, 14, 2, 4.5f),
            new Mood(DASH, 12, 160, 220, 0.35f, 0.7f),
            new Mood(TURN, 13, 25, 40, 1.5f, 3)};
    // màu nền, đốm 1, đốm 2 (0 = không có), vây
    private static final int[][] KOI = {
            {0xFFF7F3EE, 0xFFE53935, 0, 0xCCF2E6DE},
            {0xFFF7F3EE, 0xFFE53935, 0xFF1A1A1A, 0xCCF2E6DE},
            {0xFFFFB300, 0xFFFFE082, 0, 0xCCFFCA28},
            {0xFF1E1E1E, 0xFFE53935, 0xFFF7F3EE, 0xCC3A3A3A},
            {0xFFFF6D00, 0xFFFFAB40, 0, 0x99FF9E40},
            {0xFF6D8FA8, 0xFFFF7043, 0, 0xCC90A4AE},
            {0xFFFFFFFF, 0xFFFF8A65, 0xFF263238, 0xCCFFFFFF}};
    private static final int[] SMALL = {0xFF4DD0E1, 0xFFE0F7FA, 0, 0x8800E5FF};

    private static class Fish extends Walker {
        final Trail trail = new Trail();
        float L, W, seg, wag, fin, ox, oy;
        int type, N;
        int[] pal;
        float[] pt, poff, pr;
        int[] pc;
        float[][] sp;
        final Paint bodyP = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint finP = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    private static class Pad {
        float x, y, r, a, va, ph;
        boolean flower;
        Paint paint;
    }

    private final ArrayList<Fish> fish = new ArrayList<>();
    private final ArrayList<Fish> small = new ArrayList<>();
    private final ArrayList<Pad> pads = new ArrayList<>();
    private final ArrayList<float[]> ripples = new ArrayList<>(); // {x, y, age, max}
    private float[][] lights; // {x, y, r, ph}
    private Paint[] lightPaints;
    private Walker leader;
    private float rippleT = 0.5f;

    private final Paint water = new Paint();
    private final Paint shadowP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ray = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spineP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eye = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint patchP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripple = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint padShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vein = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint petal1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint petal2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pollen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path body = new Path();
    private final Path tail = new Path();
    private final float[] tmp = new float[3];
    private final float[][] left = new float[12][2], right = new float[12][2];

    public FishScene(float dp, float mult) {
        super(dp, mult);
        shadowP.setColor(0x38001420);
        ray.setColor(0x40FFFFFF);
        ray.setStyle(Paint.Style.STROKE);
        spineP.setColor(0x38FFFFFF);
        spineP.setStyle(Paint.Style.STROKE);
        spineP.setStrokeCap(Paint.Cap.ROUND);
        eye.setColor(0xFF111111);
        ripple.setStyle(Paint.Style.STROKE);
        padShadow.setColor(0x33000000);
        vein.setColor(0x59003C00);
        vein.setStyle(Paint.Style.STROKE);
        petal1.setColor(0xFFF8BBD0);
        petal2.setColor(0xFFF48FB1);
        pollen.setColor(0xFFFFEB3B);
    }

    @Override protected void init() {
        onResize();
        for (int i = 0; i < count(9); i++) fish.add(makeFish(i % KOI.length, rand(70, 130) * dp));

        leader = new Walker();
        leader.x = -60 * dp;
        leader.y = rand(0.3f, 0.6f) * h;
        leader.mood = WALK;
        leader.timer = 4;
        leader.v = leader.target = 70 * dp;
        for (int i = 0; i < count(12); i++) {
            Fish f = makeFish(-1, rand(22, 30) * dp);
            f.ox = -(i / 3) * 30 * dp - rand(0, 10) * dp;
            f.oy = ((i % 3) - 1) * 20 * dp + rand(-7, 7) * dp;
            f.x = leader.x + f.ox;
            f.y = leader.y + f.oy;
            f.ang = 0;
            f.trail.pts.clear();
            f.trail.push(f.x, f.y, 0, 0);
            small.add(f);
        }

        int np = Math.max(3, Math.round(w * h / (190 * 190 * dp * dp)));
        for (int i = 0; i < np; i++) {
            Pad p = new Pad();
            p.x = rand(0, w);
            p.y = rand(0, h);
            p.r = rand(22, 42) * dp;
            p.a = rand(0, 6.28f);
            p.va = rand(-0.05f, 0.05f);
            p.ph = rand(0, 6.28f);
            p.flower = rnd.nextFloat() < 0.3f;
            int base = rnd.nextBoolean() ? 0xFF4CAF50 : 0xFF66BB6A;
            p.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.paint.setShader(new RadialGradient(-p.r * 0.2f, -p.r * 0.2f, p.r * 1.1f,
                    new int[]{mix(base, 0xFFFFFFFF, 0.25f), base, mix(base, 0xFF000000, 0.35f)},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            pads.add(p);
        }
        lights = new float[9][];
        lightPaints = new Paint[9];
        for (int i = 0; i < 9; i++) {
            lights[i] = new float[]{rand(0, w), rand(0, h), rand(60, 140) * dp, rand(0, 6.28f)};
            lightPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            lightPaints[i].setShader(new RadialGradient(0, 0, lights[i][2], 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        }
    }

    @Override protected void onResize() {
        water.setShader(new LinearGradient(0, 0, w, h, new int[]{0x5520897E, 0x7A0B5560, 0x6A114A6B},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
    }

    private Fish makeFish(int type, float L) {
        Fish f = new Fish();
        float[] st = edgeStart(rand(20, 200) * dp);
        f.x = st[0];
        f.y = st[1];
        f.ang = st[2];
        f.L = L;
        f.type = type;
        f.wag = rand(0, 6.28f);
        f.N = type < 0 ? 6 : 10;
        f.seg = L / (f.N - 1) * 0.82f;
        f.W = L * (type == 4 ? 0.15f : 0.13f);
        f.mood = WALK;
        f.timer = rand(1, 5);
        f.v = f.target = rand(35, 60) * dp;
        f.trail.push(f.x, f.y, f.ang, 0);
        f.pal = type < 0 ? SMALL : KOI[type];
        f.sp = new float[f.N][3];
        f.finP.setColor(f.pal[3]);
        f.bodyP.setColor(f.pal[0]);
        int n = type < 0 ? 0 : (type == 6 ? 1 : 2 + rnd.nextInt(3));
        f.pt = new float[n];
        f.poff = new float[n];
        f.pr = new float[n];
        f.pc = new int[n];
        for (int k = 0; k < n; k++) {
            if (type == 6) { // chấm đỏ trên đầu
                f.pt[k] = 0.08f; f.poff[k] = 0; f.pr[k] = 0.45f; f.pc[k] = f.pal[1];
            } else {
                boolean second = f.pal[2] != 0 && rnd.nextFloat() < 0.4f;
                f.pt[k] = rand(0.1f, 0.75f);
                f.poff[k] = rand(-0.5f, 0.5f);
                f.pr[k] = rand(0.5f, 0.9f);
                f.pc[k] = second ? f.pal[2] : f.pal[1];
            }
        }
        return f;
    }

    /** Tính các điểm xương sống, có uốn mình */
    private boolean spine(Fish f) {
        int N = f.N;
        float amp = f.mood == SLOW ? 0.35f : 1f;
        for (int i = 0; i < N; i++) {
            float s = Math.max(f.trail.s - i * f.seg, f.trail.first());
            if (!f.trail.sample(s, tmp)) return false;
            float t = i / (float) (N - 1);
            float lat = sin(f.wag - t * 2.6f) * f.L * 0.07f * (float) Math.pow(t, 1.3) * amp;
            f.sp[i][0] = tmp[0] - sin(tmp[2]) * lat;
            f.sp[i][1] = tmp[1] + cos(tmp[2]) * lat;
        }
        for (int i = 0; i < N; i++) {
            float[] a = f.sp[Math.max(0, i - 1)], b = f.sp[Math.min(N - 1, i + 1)];
            f.sp[i][2] = (float) Math.atan2(a[1] - b[1], a[0] - b[0]);
        }
        return true;
    }

    private float width(Fish f, float t) {
        float p = t < 0.3f ? 0.6f + 0.4f * (float) Math.sqrt(t / 0.3f)
                : 1f - (float) Math.pow((t - 0.3f) / 0.7f, 1.4) * 0.85f;
        return f.W * clamp(p, 0.08f, 1f);
    }

    private void fishMood(Walker f, float dt) {
        f.timer -= dt;
        if (f.timer <= 0) {
            nextMood(f, MOODS);
            if (f.mood == TURN) f.turn = (rnd.nextBoolean() ? 1 : -1) * rand(1.2f, 2f);
        }
        float acc = f.mood == DASH ? 900 * dp : 120 * dp;
        if (f.v < f.target) f.v = Math.min(f.target, f.v + acc * dt);
        else f.v = Math.max(f.target, f.v - 160 * dp * dt);
    }

    private void moveFish(Fish f, float dt) {
        fishMood(f, dt);
        if (!bounce(f, 40 * dp, f.mood == DASH ? 4f : 2f, dt)) {
            if (f.mood == TURN) {
                f.ang += f.turn * dt;
            } else {
                f.turn += rand(-1f, 1f) * 1.5f * dt;
                f.turn *= 0.99f;
                f.turn = clamp(f.turn, -0.8f, 0.8f);
                f.ang += f.turn * dt * (f.mood == DASH ? 2.5f : 1f);
            }
        }
        float d = f.v * dt;
        f.x += cos(f.ang) * d;
        f.y += sin(f.ang) * d;
        f.trail.s += d;
        f.trail.push(f.x, f.y, f.ang, 2 * dp);
        f.trail.trim(f.trail.s - f.seg * f.N - 20 * dp);
        f.wag += dt * (2f + f.v / (9 * dp));
        f.fin += dt * (f.mood == SLOW ? 10f : 4f);
    }

    @Override protected void step(float dt) {
        for (Fish f : fish) moveFish(f, dt);

        Walker L = leader;
        fishMood(L, dt);
        if (!bounce(L, 60 * dp, 2f, dt)) {
            L.turn += rand(-1f, 1f) * 2f * dt;
            L.turn *= 0.99f;
            L.turn = clamp(L.turn, -0.9f, 0.9f);
            L.ang += L.turn * dt;
        }
        L.v = Math.max(L.v, 30 * dp);
        L.x += cos(L.ang) * L.v * dt;
        L.y += sin(L.ang) * L.v * dt;
        for (Fish f : small) {
            float ca = cos(L.ang), sa = sin(L.ang);
            float tx = L.x + f.ox * ca - f.oy * sa + sin(time * 1.3f + f.wag * 0.05f) * 6 * dp;
            float ty = L.y + f.ox * sa + f.oy * ca;
            float d = angDiff((float) Math.atan2(ty - f.y, tx - f.x), f.ang);
            f.ang += clamp(d, -3.5f * dt, 3.5f * dt);
            float dist = (float) Math.hypot(tx - f.x, ty - f.y);
            f.v = lerp(f.v, L.v * (0.75f + Math.min(1.2f, dist / (40 * dp))), Math.min(1f, dt * 3f));
            float s = f.v * dt;
            f.x += cos(f.ang) * s;
            f.y += sin(f.ang) * s;
            f.trail.s += s;
            f.trail.push(f.x, f.y, f.ang, 1.5f * dp);
            f.trail.trim(f.trail.s - f.seg * f.N - 10 * dp);
            f.wag += dt * (3f + f.v / (6 * dp));
        }

        for (Pad p : pads) {
            p.a += p.va * dt;
            p.x += sin(time * 0.2f + p.ph) * 2 * dp * dt;
        }
        rippleT -= dt;
        if (rippleT <= 0) {
            rippleT = rand(0.4f, 1.6f);
            ripples.add(new float[]{rand(0, w), rand(0, h), 0, rand(30, 70) * dp});
        }
        for (int i = ripples.size() - 1; i >= 0; i--) {
            float[] r = ripples.get(i);
            r[2] += dt;
            if (r[2] >= 2.5f) ripples.remove(i);
        }
    }

    @Override public void touch(float x, float y) {
        // gõ mặt nước: gợn sóng, cá giật mình quẫy đi
        ripples.add(new float[]{x, y, 0, 60 * dp});
        ripples.add(new float[]{x, y, -0.3f, 90 * dp});
        for (Fish f : fish) scare(f, x, y, 180 * dp, 220 * dp);
        if (Math.hypot(leader.x - x, leader.y - y) < 200 * dp) scare(leader, x, y, 200 * dp, 160 * dp);
    }

    @Override public void draw(Canvas c) {
        if (companion) { // chỉ vài con cá nhỏ bơi lướt qua, không có nước, lá súng
            for (Fish f : small) if (spine(f)) drawFish(c, f);
            for (Fish f : fish) if (spine(f)) drawFish(c, f);
            return;
        }
        c.drawRect(0, 0, w, h, water);

        // vệt sáng lăn tăn dưới nước
        for (int i = 0; i < lights.length; i++) {
            float[] l = lights[i];
            float a = 0.5f + 0.5f * sin(time * 0.6f + l[3]);
            float x = l[0] + sin(time * 0.25f + l[3]) * 30 * dp, y = l[1] + cos(time * 0.2f + l[3]) * 30 * dp;
            int sv = c.save();
            c.translate(x, y);
            lightPaints[i].setAlpha((int) (255 * 0.35f * a));
            c.drawCircle(0, 0, l[2], lightPaints[i]);
            c.restoreToCount(sv);
        }

        for (Fish f : small) if (spine(f)) drawFish(c, f);
        for (Fish f : fish) if (spine(f)) drawFish(c, f);

        ripple.setStrokeWidth(1.4f * dp);
        for (float[] r : ripples) {
            if (r[2] < 0) continue;
            float k = r[2] / 2.5f;
            ripple.setColor(withAlpha(0xFFFFFFFF, 0.45f * (1 - k)));
            float rr = r[3] * k + 2;
            c.drawOval(r[0] - rr, r[1] - rr * 0.9f, r[0] + rr, r[1] + rr * 0.9f, ripple);
            if (k > 0.25f) {
                float r2 = r[3] * (k - 0.25f);
                c.drawOval(r[0] - r2, r[1] - r2 * 0.9f, r[0] + r2, r[1] + r2 * 0.9f, ripple);
            }
        }

        for (Pad p : pads) drawPad(c, p);
    }

    private void drawPad(Canvas c, Pad p) {
        int sv = c.save();
        c.translate(p.x, p.y);
        c.rotate(deg(p.a));
        c.drawOval(-p.r + 4 * dp, -p.r + 6 * dp, p.r + 4 * dp, p.r + 6 * dp, padShadow);
        body.reset();
        body.moveTo(0, 0);
        body.arcTo(-p.r, -p.r, p.r, p.r, 14.3f, 340.8f, false);
        body.close();
        c.drawPath(body, p.paint);
        vein.setStrokeWidth(1 * dp);
        for (int k = 0; k < 7; k++) {
            float a = 0.4f + k * 0.85f;
            c.drawLine(0, 0, cos(a) * p.r * 0.85f, sin(a) * p.r * 0.85f, vein);
        }
        if (p.flower) {
            for (int k = 0; k < 8; k++) {
                int s2 = c.save();
                c.rotate(k * 45f + 11.5f);
                c.drawOval(0, -p.r * 0.1f, p.r * 0.42f, p.r * 0.1f, k % 2 == 1 ? petal1 : petal2);
                c.restoreToCount(s2);
            }
            c.drawCircle(0, 0, p.r * 0.1f, pollen);
        }
        c.restoreToCount(sv);
    }

    private void drawFish(Canvas c, Fish f) {
        int N = f.N;
        float[][] sp = f.sp;
        float[] head = sp[0];
        if (offscreen(head[0], head[1], f.L * 1.5f)) return;

        for (int i = 0; i < N; i++) {
            float t = i / (float) (N - 1), wd = width(f, t), a = sp[i][2];
            left[i][0] = sp[i][0] - sin(a) * wd;
            left[i][1] = sp[i][1] + cos(a) * wd;
            right[i][0] = sp[i][0] + sin(a) * wd;
            right[i][1] = sp[i][1] - cos(a) * wd;
        }
        float a0 = head[2], fw = f.W * 0.9f;
        body.reset();
        body.moveTo(left[0][0], left[0][1]);
        body.cubicTo(left[0][0] + cos(a0) * fw, left[0][1] + sin(a0) * fw,
                right[0][0] + cos(a0) * fw, right[0][1] + sin(a0) * fw, right[0][0], right[0][1]);
        for (int i = 1; i < N; i++) {
            body.quadTo(right[i - 1][0], right[i - 1][1],
                    (right[i - 1][0] + right[i][0]) / 2, (right[i - 1][1] + right[i][1]) / 2);
        }
        body.lineTo(right[N - 1][0], right[N - 1][1]);
        body.lineTo(left[N - 1][0], left[N - 1][1]);
        for (int i = N - 1; i > 0; i--) {
            body.quadTo(left[i][0], left[i][1],
                    (left[i - 1][0] + left[i][0]) / 2, (left[i - 1][1] + left[i][1]) / 2);
        }
        body.close();

        // bóng dưới đáy
        int sv = c.save();
        c.translate(6 * dp, 12 * dp);
        c.drawPath(body, shadowP);
        c.restoreToCount(sv);

        // vây ngực
        int pi = Math.min(2, N - 1);
        float pa = sp[pi][2], pw = width(f, pi / (float) (N - 1));
        for (int side = -1; side <= 1; side += 2) {
            sv = c.save();
            c.translate(sp[pi][0] - sin(pa) * pw * side * 0.8f, sp[pi][1] + cos(pa) * pw * side * 0.8f);
            c.rotate(deg(pa + side * (2.3f + sin(f.fin + side) * 0.35f)));
            c.drawOval(0, -f.W * 0.35f, f.W * 1.3f, f.W * 0.35f, f.finP);
            c.restoreToCount(sv);
        }

        // đuôi vẫy
        float[] tl = sp[N - 1];
        float ta = tl[2] + sin(f.wag - 2.9f) * 0.35f;
        float len = f.W * (f.type == 4 ? 3.2f : 2f), spread = f.type == 4 ? 0.75f : 0.55f;
        sv = c.save();
        c.translate(tl[0], tl[1]);
        c.rotate(deg(ta) + 180f);
        tail.reset();
        tail.moveTo(0, 0);
        tail.quadTo(len * 0.6f, -len * spread * 0.4f, len, -len * spread);
        tail.quadTo(len * 0.7f, 0, len, len * spread);
        tail.quadTo(len * 0.6f, len * spread * 0.4f, 0, 0);
        c.drawPath(tail, f.finP);
        ray.setStrokeWidth(0.8f * dp);
        for (int k = -2; k <= 2; k++) c.drawLine(0, 0, len * 0.9f, k * len * spread * 0.4f, ray);
        c.restoreToCount(sv);

        // thân + đốm
        if (f.type == 2) {
            f.bodyP.setShader(new LinearGradient(head[0], head[1], tl[0], tl[1],
                    new int[]{0xFFFFE082, 0xFFFFB300, 0xFFFF8F00}, null, Shader.TileMode.CLAMP));
        }
        c.drawPath(body, f.bodyP);
        sv = c.save();
        c.clipPath(body);
        for (int k = 0; k < f.pt.length; k++) {
            int idx = Math.min(N - 1, Math.round(f.pt[k] * (N - 1)));
            float[] q = sp[idx];
            float wd = width(f, f.pt[k]);
            int s2 = c.save();
            c.translate(q[0] - sin(q[2]) * wd * f.poff[k], q[1] + cos(q[2]) * wd * f.poff[k]);
            c.rotate(deg(q[2]));
            patchP.setColor(f.pc[k]);
            c.drawOval(-f.L * 0.16f * f.pr[k], -wd * f.pr[k], f.L * 0.16f * f.pr[k], wd * f.pr[k], patchP);
            c.restoreToCount(s2);
        }
        // sống lưng sáng
        spineP.setStrokeWidth(f.W * 0.35f);
        for (int i = 0; i < N - 2; i++) c.drawLine(sp[i][0], sp[i][1], sp[i + 1][0], sp[i + 1][1], spineP);
        c.restoreToCount(sv);

        // mắt
        for (int side = -1; side <= 1; side += 2) {
            c.drawCircle(head[0] + cos(a0) * f.W * 0.25f - sin(a0) * f.W * 0.42f * side,
                    head[1] + sin(a0) * f.W * 0.25f + cos(a0) * f.W * 0.42f * side, f.W * 0.11f, eye);
        }
    }
}
