package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Mèo đuổi chuột (nhìn từ trên xuống): chuột xám chạy, mèo mướp cam bám sát phía sau
 * (không bao giờ chồng lên nhau). Chuột khựng lại ngó thì mèo nép người rồi vồ vào chỗ đó,
 * chuột né ngang, mèo vồ hụt thì ngồi ngơ ngác hoặc chóng mặt quay sao.
 */
public class ChaseScene extends MouseScene {

    private static final int FOLLOW = 0, CROUCH = 1, POUNCE = 2, REST = 3, DIZZY = 4;
    private static final float[][] LEGS = {{0.17f, 1, 0}, {0.17f, -1, 3.1416f}, {-0.3f, 1, 3.1416f}, {-0.3f, -1, 0}};

    private static class Cat extends Walker {
        float s, gait, tail, pt, dizzy, ears, lx, ly;
        int phase, dodge;
        boolean fast;
        Mouse mouse;
    }

    private final ArrayList<Cat> cats = new ArrayList<>();
    private final Paint tailP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pawP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint catShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stripeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint earP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint earIn = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint muzzle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iris = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupilP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint noseP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiskP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cpath = new Path();
    private Paint bodyP, headP;

    public ChaseScene(float dp, float mult) {
        super(dp, mult);
        tailP.setStyle(Paint.Style.STROKE);
        tailP.setStrokeCap(Paint.Cap.ROUND);
        pawP.setColor(0xFFFFE0B2);
        catShadow.setColor(0x33000000);
        stripeP.setColor(0x998C3C0A);
        stripeP.setStyle(Paint.Style.STROKE);
        stripeP.setStrokeCap(Paint.Cap.ROUND);
        earP.setColor(0xFFE07A22);
        earIn.setColor(0xFFF4A3A3);
        muzzle.setColor(0xFFFFF3E0);
        iris.setColor(0xFF9CCC65);
        pupilP.setColor(0xFF111111);
        noseP.setColor(0xFFE88C95);
        whiskP.setColor(0xBFFFFFFF);
        whiskP.setStyle(Paint.Style.STROKE);
        starP.setColor(0xFFFFEB3B);
    }

    @Override protected void init() {
        float s = 120 * dp;
        bodyP = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyP.setShader(new RadialGradient(-0.15f * s, -0.06f * s, 0.42f * s,
                new int[]{0xFFFFBE6E, 0xFFF08A2A, 0xFFB85A14}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        headP = new Paint(Paint.ANTI_ALIAS_FLAG);
        headP.setShader(new RadialGradient(0.29f * s, -0.04f * s, 0.18f * s,
                new int[]{0xFFFFC27A, 0xFFF08A2A, 0xFFC0621A}, new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        for (int i = 0; i < count(1); i++) {
            Mouse m = new Mouse();
            m.x = rand(0.2f, 0.8f) * w;
            m.y = rand(0.3f, 0.7f) * h;
            m.ang = rand(0, 6.28f);
            m.s = 48 * dp;
            m.earT = 1;
            m.fur = FUR[0];
            m.mood = WALK;
            m.timer = 1e9f;
            setupPaints(m);
            list.add(m);

            Cat cat = new Cat();
            cat.x = m.x - 150 * dp;
            cat.y = m.y;
            cat.ang = 0;
            cat.s = s;
            cat.pt = rand(3, 5);
            cat.mouse = m;
            cats.add(cat);
        }
    }

    @Override public void touch(float x, float y) {
        for (Cat cat : cats) {
            Mouse m = cat.mouse;
            if (Math.hypot(m.x - x, m.y - y) < 150 * dp) {
                m.ang = (float) Math.atan2(m.y - y, m.x - x);
                m.v = 200 * dp;
            }
            if (Math.hypot(cat.x - x, cat.y - y) < 150 * dp && cat.phase != POUNCE) {
                cat.phase = DIZZY; // bị trêu: mèo chóng mặt
                cat.pt = 1.5f;
                cat.dizzy = 0;
            }
        }
    }

    @Override protected void step(float dt) {
        for (Cat cat : cats) {
            Mouse m = cat.mouse;
            cat.pt -= dt;
            float cv = 0, mv = 0;
            float back = cat.s * 0.95f; // khoảng cách mèo bám sau lưng chuột
            switch (cat.phase) {
                case FOLLOW: { // chuột chạy, mèo bám theo phía sau
                    m.mood = cat.fast ? DASH : WALK;
                    mv = (cat.fast ? 190 : 110) * dp;
                    if (!bounce(m, 60 * dp, 4f, dt)) {
                        m.turn += rand(-3f, 3f) * dt;
                        m.turn *= 0.97f;
                        m.turn = clamp(m.turn, -1.6f, 1.6f);
                        m.ang += m.turn * dt;
                    }
                    float tx = m.x - cos(m.ang) * back, ty = m.y - sin(m.ang) * back;
                    float d = (float) Math.hypot(tx - cat.x, ty - cat.y);
                    cat.ang += clamp(angDiff((float) Math.atan2(ty - cat.y, tx - cat.x), cat.ang), -4f * dt, 4f * dt);
                    cv = clamp(d * 2.2f, 0, 260 * dp);
                    if (cat.pt <= 0) { cat.phase = CROUCH; cat.pt = rand(0.7f, 1.1f); }
                    if (rnd.nextFloat() < dt * 0.3f) cat.fast = !cat.fast;
                    break;
                }
                case CROUCH: // chuột khựng lại ngó, mèo nép người chuẩn bị vồ
                    m.mood = FREEZE;
                    cv = 20 * dp;
                    cat.ang += clamp(angDiff((float) Math.atan2(m.y - cat.y, m.x - cat.x), cat.ang), -3f * dt, 3f * dt);
                    if (cat.pt <= 0) {
                        cat.phase = POUNCE;
                        cat.pt = 0.5f;
                        cat.lx = m.x;
                        cat.ly = m.y;
                        cat.dodge = rnd.nextBoolean() ? 1 : -1;
                    }
                    break;
                case POUNCE: // vồ vào chỗ chuột vừa đứng, chuột né ngang vào phút chót
                    cv = 420 * dp;
                    cat.ang += clamp(angDiff((float) Math.atan2(cat.ly - cat.y, cat.lx - cat.x), cat.ang), -6f * dt, 6f * dt);
                    if (cat.pt < 0.38f) {
                        m.mood = DASH;
                        mv = 330 * dp;
                        float side = (float) Math.atan2(m.y - cat.y, m.x - cat.x) + cat.dodge * 1.5708f;
                        m.ang += clamp(angDiff(side, m.ang), -12f * dt, 12f * dt);
                    } else {
                        m.mood = FREEZE;
                    }
                    if (cat.pt <= 0 || Math.hypot(cat.lx - cat.x, cat.ly - cat.y) < 12 * dp) {
                        cat.phase = rnd.nextBoolean() ? REST : DIZZY;
                        cat.pt = rand(1.4f, 2.2f);
                        cat.dizzy = 0;
                    }
                    break;
                default: // vồ hụt: mèo ngồi ngơ ngác / chóng mặt, chuột chạy xa
                    if (cat.phase == DIZZY) {
                        cat.dizzy += dt;
                        cat.ang += sin(time * 5f) * 1.2f * dt;
                    }
                    m.mood = DASH;
                    mv = 200 * dp;
                    if (!bounce(m, 60 * dp, 6f, dt)) {
                        m.ang += clamp(angDiff((float) Math.atan2(m.y - cat.y, m.x - cat.x), m.ang), -3f * dt, 3f * dt);
                    }
                    if (cat.pt <= 0) { cat.phase = FOLLOW; cat.pt = rand(3f, 6f); cat.fast = false; }
                    break;
            }
            cat.v = lerp(cat.v, cv, Math.min(1f, dt * (cat.phase == POUNCE ? 8f : 4f)));
            m.v = lerp(m.v, mv, Math.min(1f, dt * (m.mood == DASH ? 10f : 6f)));
            cat.x += cos(cat.ang) * cat.v * dt;
            cat.y += sin(cat.ang) * cat.v * dt;
            m.x += cos(m.ang) * m.v * dt;
            m.y += sin(m.ang) * m.v * dt;

            // không bao giờ chồng lên nhau: đẩy mèo lùi ra
            float dx = cat.x - m.x, dy = cat.y - m.y, dd = (float) Math.hypot(dx, dy);
            float minD = cat.s * 0.62f + m.s * 0.5f;
            if (dd < minD && dd > 0.01f) {
                cat.x = m.x + dx / dd * minD;
                cat.y = m.y + dy / dd * minD;
            }
            // luôn ở trong màn hình
            float cm = cat.s * 0.3f, mm = m.s * 0.4f;
            cat.x = clamp(cat.x, cm, w - cm);
            cat.y = clamp(cat.y, cm, h - cm);
            m.x = clamp(m.x, mm, w - mm);
            m.y = clamp(m.y, mm, h - mm);

            cat.gait += cat.v * dt / (cat.s * 0.12f);
            cat.tail += dt * (cat.phase == CROUCH ? 7f : 2f);
            cat.ears = lerp(cat.ears, (cat.phase == CROUCH || cat.phase == POUNCE) ? 1f : 0f, Math.min(1f, dt * 6f));
            m.gait += m.v * dt / (m.s * 0.11f);
            m.tail += dt * (1.2f + m.v / (45 * dp));
            m.sniff += dt * (m.mood == FREEZE ? 18f : 4f);
            float wantHead = m.mood == FREEZE
                    ? clamp(angDiff((float) Math.atan2(cat.y - m.y, cat.x - m.x), m.ang), -0.7f, 0.7f) : 0f;
            m.head = lerp(m.head, wantHead, Math.min(1f, dt * 6f));
        }
    }

    @Override public void draw(Canvas c) {
        for (Mouse m : list) drawMouse(c, m);
        for (Cat cat : cats) drawCat(c, cat);
    }

    private void drawCat(Canvas c, Cat k) {
        float s = k.s;
        if (offscreen(k.x, k.y, s * 2)) return;
        int save = c.save();
        c.translate(k.x, k.y);
        c.rotate(deg(k.ang));
        float sit = k.phase == REST ? 1f : 0f;

        c.drawOval(-0.45f * s, -0.16f * s, 0.6f * s, 0.32f * s, catShadow);

        // đuôi vằn, lúc rình giật giật, lúc ngồi cuộn tròn
        int segs = 16;
        float px = -0.46f * s, py = 0;
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs;
            float ang = sin(k.tail - t * 3f) * 0.35f * t + sit * t * 2.2f;
            float nx = px - cos(ang) * 0.045f * s;
            float ny = py + sin(ang) * 0.045f * s * 1.6f + (k.phase == CROUCH ? sin(k.tail * 2f) * t * 0.01f * s : 0f);
            tailP.setColor(i > segs - 2 ? 0xFF7A3A0E : ((i / 2) % 2 == 1 ? 0xFFE07A22 : 0xFFF29A44));
            tailP.setStrokeWidth((0.075f - 0.025f * t) * s);
            c.drawLine(px, py, nx, ny, tailP);
            px = nx;
            py = ny;
        }

        // chân
        boolean moving = k.v > 2 * dp;
        for (float[] L : LEGS) {
            float st = moving ? sin(k.gait + L[2]) * 0.09f : 0f;
            float fx = (L[0] + st) * s, fy = L[1] * 0.21f * s, pr = 0.06f * s;
            c.drawOval(fx - pr, fy - pr * 0.8f, fx + pr * 1.2f, fy + pr * 0.8f, pawP);
        }

        // thân + vằn
        float bx = sit > 0 ? 0.82f : 1f;
        float l = -0.12f * s - 0.38f * s * bx, r = -0.12f * s + 0.38f * s * bx;
        c.drawOval(l, -0.2f * s, r, 0.2f * s, bodyP);
        int sv = c.save();
        cpath.reset();
        cpath.addOval(l, -0.2f * s, r, 0.2f * s, Path.Direction.CW);
        c.clipPath(cpath);
        stripeP.setStrokeWidth(0.035f * s);
        for (float x = -0.42f; x <= 0.12f; x += 0.09f) {
            for (int side = -1; side <= 1; side += 2) {
                cpath.reset();
                cpath.moveTo(x * s, side * 0.22f * s);
                cpath.quadTo((x + 0.04f) * s, side * 0.08f * s, (x + 0.01f) * s, side * 0.02f * s);
                c.drawPath(cpath, stripeP);
            }
        }
        c.restoreToCount(sv);

        // đầu: tai (cụp ra sau khi rình), mặt, mắt, ria
        float hx = 0.32f * s;
        for (int side = -1; side <= 1; side += 2) {
            int s2 = c.save();
            c.translate(hx - 0.02f * s, side * 0.1f * s);
            c.rotate(deg(side * (0.3f + 0.6f * k.ears)));
            cpath.reset();
            cpath.moveTo(-0.06f * s, 0);
            cpath.lineTo(0, side * 0.13f * s);
            cpath.lineTo(0.07f * s, 0);
            cpath.close();
            c.drawPath(cpath, earP);
            cpath.reset();
            cpath.moveTo(-0.03f * s, 0);
            cpath.lineTo(0, side * 0.08f * s);
            cpath.lineTo(0.04f * s, 0);
            cpath.close();
            c.drawPath(cpath, earIn);
            c.restoreToCount(s2);
        }
        c.drawCircle(hx, 0, 0.16f * s, headP);
        stripeP.setStrokeWidth(0.025f * s);
        c.drawLine(hx - 0.1f * s, 0, hx + 0.02f * s, 0, stripeP);
        c.drawLine(hx - 0.08f * s, -0.06f * s, hx, -0.03f * s, stripeP);
        c.drawLine(hx - 0.08f * s, 0.06f * s, hx, 0.03f * s, stripeP);
        c.drawOval(hx + 0.06f * s, -0.07f * s, hx + 0.19f * s, 0.07f * s, muzzle);
        float pw = (k.phase == CROUCH || k.phase == POUNCE) ? 0.022f : 0.009f; // đồng tử nở khi rình
        for (int side = -1; side <= 1; side += 2) {
            c.drawCircle(hx + 0.07f * s, side * 0.075f * s, 0.032f * s, iris);
            c.drawOval(hx + 0.07f * s - pw * s, side * 0.075f * s - 0.026f * s,
                    hx + 0.07f * s + pw * s, side * 0.075f * s + 0.026f * s, pupilP);
        }
        c.drawCircle(hx + 0.17f * s, 0, 0.022f * s, noseP);
        whiskP.setStrokeWidth(0.006f * s);
        for (int side = -1; side <= 1; side += 2) {
            for (int j = 0; j < 3; j++) {
                c.drawLine(hx + 0.15f * s, side * 0.04f * s, hx + 0.3f * s, side * (0.08f + j * 0.06f) * s, whiskP);
            }
        }
        if (k.phase == DIZZY) {
            for (int j = 0; j < 4; j++) {
                float a = k.dizzy * 5 + j * 1.5708f;
                star(c, hx + cos(a) * 0.2f * s, sin(a) * 0.2f * s, 0.035f * s);
            }
        }
        c.restoreToCount(save);
    }

    private void star(Canvas c, float x, float y, float r) {
        cpath.reset();
        for (int i = 0; i < 10; i++) {
            float a = i * 0.6283f, rr = i % 2 == 1 ? r * 0.45f : r;
            if (i == 0) cpath.moveTo(x + cos(a) * rr, y + sin(a) * rr);
            else cpath.lineTo(x + cos(a) * rr, y + sin(a) * rr);
        }
        cpath.close();
        c.drawPath(cpath, starP);
    }
}
