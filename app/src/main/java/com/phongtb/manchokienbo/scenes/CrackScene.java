package com.phongtb.manchokienbo.scenes;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * Vỡ màn hình kiểu rễ cây: từ một điểm mọc ra vài rễ nứt, bò dần ra xa,
 * đi tới đâu tẻ nhánh tới đó, nhánh sau mảnh hơn nhánh trước, đầu rễ thon nhọn.
 * Cứ vài giây lại nứt thêm một chỗ; chạm vào đâu thì nứt thêm chỗ đó.
 * Phần đã nứt vẽ sẵn vào một ảnh đệm nên càng nhiều vết nứt máy vẫn nhẹ.
 */
public class CrackScene extends Scene {

    private static class Root {
        float x, y, lx, ly, ang, w, len, done, speed, nextBranch, drift;
        int gen, idx, expected;
    }

    private final ArrayList<Root> roots = new ArrayList<>();
    private final ArrayList<Root> born = new ArrayList<>();
    private final Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap cache;
    private Canvas cacheCanvas;
    private float next = 0.5f;
    private int hits = 0;

    public CrackScene(float dp, float mult) {
        super(dp, mult);
        for (Paint p : new Paint[]{dark, light}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        dark.setColor(0x73000000);
        light.setColor(0xD9FFFFFF);
    }

    @Override protected void init() {
        makeCache();
        if (companion) hit(rand(0.25f, 0.75f) * w, rand(0.25f, 0.75f) * h, 0.6f);
    }

    @Override protected void onResize() {
        makeCache();
        roots.clear();
    }

    private void makeCache() {
        try {
            cache = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas(cache);
        } catch (Throwable e) { // thiếu bộ nhớ: vẽ trực tiếp, không đệm
            cache = null;
            cacheCanvas = null;
        }
    }

    private Root root(float x, float y, float ang, float width, float len, int gen) {
        Root r = new Root();
        r.x = r.lx = x;
        r.y = r.ly = y;
        r.ang = ang;
        r.w = width;
        r.len = len;
        r.gen = gen;
        r.speed = rand(18, 32) * dp * (1 - gen * 0.15f);
        r.nextBranch = rand(0.15f, 0.35f) * len;
        r.drift = rand(-0.6f, 0.6f);
        r.expected = Math.max(2, Math.round(len / (5 * dp)));
        return r;
    }

    private void hit(float x, float y, float size) {
        int n = (int) rand(3, 6);
        float a0 = rand(0, 6.28f);
        for (int i = 0; i < n; i++) {
            roots.add(root(x, y, a0 + i * 6.283f / n + rand(-0.4f, 0.4f),
                    rand(2.4f, 3.4f) * dp * size, rand(120, 260) * dp * size, 0));
        }
        hits++;
    }

    @Override public void touch(float x, float y) {
        hit(x, y, 0.8f);
    }

    @Override protected void step(float dt) {
        next -= dt;
        if (!companion && next <= 0 && hits < count(8)) {
            next = rand(6, 10);
            hit(rand(0.15f, 0.85f) * w, rand(0.15f, 0.85f) * h, 1f);
        }
        born.clear();
        for (int k = roots.size() - 1; k >= 0; k--) {
            Root b = roots.get(k);
            float st = b.speed * dt;
            b.done += st;
            b.ang += (rand(-1f, 1f) * 2.2f + b.drift * 0.6f) * dt;
            b.x += cos(b.ang) * st;
            b.y += sin(b.ang) * st;
            if (Math.hypot(b.x - b.lx, b.y - b.ly) > 5 * dp) {
                b.idx++;
                segment(cacheCanvas, b, b.lx, b.ly, b.x, b.y);
                b.lx = b.x;
                b.ly = b.y;
            }
            if (b.gen < 4 && b.done >= b.nextBranch) { // tẻ nhánh
                b.nextBranch += rand(0.18f, 0.4f) * b.len;
                born.add(root(b.x, b.y, b.ang + (rnd.nextBoolean() ? 1 : -1) * rand(0.45f, 1f),
                        b.w * 0.62f, (b.len - b.done) * rand(0.5f, 0.9f), b.gen + 1));
            }
            if (b.done >= b.len && cacheCanvas != null) roots.remove(k); // xong: đã nằm trong ảnh đệm
        }
        roots.addAll(born);
    }

    /** Một đoạn nứt: bóng tối lệch nhẹ + đường sáng mảnh, càng về đầu rễ càng thon */
    private void segment(Canvas c, Root b, float x0, float y0, float x1, float y1) {
        if (c == null) return;
        float t = Math.min(1f, b.idx / (float) b.expected);
        float wd = Math.max(0.35f * dp, b.w * (1 - t * 0.85f));
        float o = 0.7f * dp;
        dark.setStrokeWidth(wd);
        c.drawLine(x0 + o, y0 + o, x1 + o, y1 + o, dark);
        light.setStrokeWidth(wd * 0.45f);
        c.drawLine(x0, y0, x1, y1, light);
    }

    @Override public void draw(Canvas c) {
        if (cache != null) c.drawBitmap(cache, 0, 0, null);
        for (Root b : roots) segment(c, b, b.lx, b.ly, b.x, b.y); // đầu rễ đang bò
    }
}
