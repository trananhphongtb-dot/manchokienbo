package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Vỡ màn hình: cứ vài giây lại có một chỗ nứt toác ra, vết nứt lan dần thành mạng nhện.
 * Chạm vào chỗ nào thì nứt thêm chỗ đó. (Chỉ là hình vẽ, máy không sao cả.)
 */
public class CrackScene extends Scene {

    private static class Ray {
        float[] pts; // x0,y0,x1,y1...
        float delay = -1;
    }

    private static class Hit {
        float x, y, age, grow;
        final ArrayList<Ray> rays = new ArrayList<>();
        final ArrayList<float[]> rings = new ArrayList<>();
        float[] chips;
        Paint glow;
    }

    private final ArrayList<Hit> hits = new ArrayList<>();
    private final Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float next = 1f;

    public CrackScene(float dp, float mult) {
        super(dp, mult);
        for (Paint p : new Paint[]{dark, light, chip}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        dark.setColor(0x66000000);
        light.setColor(0xCCFFFFFF);
        chip.setColor(0xB3FFFFFF);
    }

    @Override protected void init() {
        dark.setStrokeWidth(2.4f * dp);
        light.setStrokeWidth(1f * dp);
        chip.setStrokeWidth(0.8f * dp);
    }

    private Hit makeHit(float x, float y, boolean big) {
        Hit h = new Hit();
        h.x = x;
        h.y = y;
        h.grow = big ? 0.45f : 0.3f;
        int n = (int) rand(7, 12);
        float maxL = (big ? rand(220, 380) : rand(110, 220)) * dp;
        float a0 = rand(0, 6.28f);
        ArrayList<Ray> main = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            float a = a0 + i * 6.283f / n + rand(-0.25f, 0.25f);
            float L = maxL * rand(0.45f, 1f), px = x, py = y, len = 0;
            ArrayList<Float> pts = new ArrayList<>();
            pts.add(px); pts.add(py);
            while (len < L) {
                float st = rand(8, 18) * dp;
                a += rand(-0.35f, 0.35f);
                px += cos(a) * st;
                py += sin(a) * st;
                len += st;
                pts.add(px); pts.add(py);
            }
            Ray r = new Ray();
            r.pts = toArray(pts);
            h.rays.add(r);
            main.add(r);
            if (rnd.nextFloat() < 0.55f) { // nhánh phụ
                int cnt = r.pts.length / 2;
                int k = (int) rand(2, Math.max(3, cnt - 1));
                k = Math.min(k, cnt - 1);
                float ba = a + (rnd.nextBoolean() ? 1 : -1) * rand(0.5f, 1.1f);
                float bx = r.pts[k * 2], by = r.pts[k * 2 + 1];
                ArrayList<Float> bp = new ArrayList<>();
                bp.add(bx); bp.add(by);
                int steps = (int) rand(3, 7);
                for (int j = 0; j < steps; j++) {
                    ba += rand(-0.3f, 0.3f);
                    bx += cos(ba) * 11 * dp;
                    by += sin(ba) * 11 * dp;
                    bp.add(bx); bp.add(by);
                }
                Ray br = new Ray();
                br.pts = toArray(bp);
                br.delay = k / (float) cnt;
                h.rays.add(br);
            }
        }
        // vòng nứt quanh tâm
        for (float rr : new float[]{0.18f, 0.38f}) {
            float[] ring = new float[n * 2];
            for (int i = 0; i < n; i++) {
                float[] p = main.get(i).pts;
                int cnt = p.length / 2;
                int idx = Math.min(cnt - 1, Math.max(1, Math.round(cnt * rr)));
                ring[i * 2] = p[idx * 2];
                ring[i * 2 + 1] = p[idx * 2 + 1];
            }
            h.rings.add(ring);
        }
        h.chips = new float[26 * 4];
        for (int i = 0; i < 26; i++) {
            float a = rand(0, 6.28f), d = rand(2, 22) * dp;
            float cx = x + cos(a) * d, cy = y + sin(a) * d;
            h.chips[i * 4] = cx;
            h.chips[i * 4 + 1] = cy;
            h.chips[i * 4 + 2] = cx + cos(a + 1) * 5 * dp;
            h.chips[i * 4 + 3] = cy + sin(a + 1) * 5 * dp;
        }
        h.glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        h.glow.setShader(new RadialGradient(x, y, 26 * dp, new int[]{0x66FFFFFF, 0x22FFFFFF, 0x00FFFFFF},
                null, Shader.TileMode.CLAMP));
        return h;
    }

    private static float[] toArray(ArrayList<Float> l) {
        float[] a = new float[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    @Override public void touch(float x, float y) {
        hits.add(makeHit(x, y, false));
        if (hits.size() > 40) hits.remove(0);
    }

    @Override protected void step(float dt) {
        for (Hit h : hits) h.age += dt;
        next -= dt;
        if (next <= 0 && hits.size() < count(10)) {
            next = rand(3, 7);
            hits.add(makeHit(rand(0.1f, 0.9f) * w, rand(0.1f, 0.9f) * h, true));
        }
    }

    private void polyline(Canvas c, float[] p, float frac, Paint paint) {
        int cnt = p.length / 2;
        int n = Math.max(1, (int) ((cnt - 1) * frac));
        path.reset();
        path.moveTo(p[0], p[1]);
        for (int i = 1; i <= n && i < cnt; i++) path.lineTo(p[i * 2], p[i * 2 + 1]);
        c.drawPath(path, paint);
    }

    @Override public void draw(Canvas c) {
        for (Hit h : hits) {
            float g = Math.min(1f, h.age / h.grow);
            c.drawCircle(h.x, h.y, 26 * dp * g, h.glow);
            for (int pass = 0; pass < 2; pass++) {
                Paint p = pass == 0 ? dark : light;
                int sv = c.save();
                if (pass == 0) c.translate(0.8f * dp, 0.8f * dp); // bóng tối lệch nhẹ cho có chiều sâu
                for (Ray r : h.rays) {
                    float f = r.delay < 0 ? g : clamp((g - r.delay) / (1 - r.delay), 0f, 1f);
                    if (f > 0) polyline(c, r.pts, f, p);
                }
                if (g >= 1) {
                    for (float[] ring : h.rings) {
                        path.reset();
                        path.moveTo(ring[0], ring[1]);
                        for (int i = 2; i < ring.length; i += 2) path.lineTo(ring[i], ring[i + 1]);
                        path.close();
                        c.drawPath(path, p);
                    }
                }
                c.restoreToCount(sv);
            }
            if (g > 0.3f) c.drawLines(h.chips, chip);
        }
    }
}
