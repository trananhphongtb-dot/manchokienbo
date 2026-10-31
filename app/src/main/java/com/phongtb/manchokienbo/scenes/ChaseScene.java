package com.phongtb.manchokienbo.scenes;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.ArrayList;

/**
 * Mèo đuổi chuột (nhìn từ trên xuống): mèo mướp cam đi dạo, rình (tai cụp, đuôi giật),
 * rồi vồ đuổi; chuột xám lách né, mèo đuổi hụt thì ngồi thở hoặc chóng mặt quay sao.
 */
public class ChaseScene extends MouseScene {

    private static final int WANDER = 0, STALK = 1, CHASE = 2, REST = 3, DIZZY = 4;
    private static final float[][] LEGS = {{0.17f, 1, 0}, {0.17f, -1, 3.1416f}, {-0.3f, 1, 3.1416f}, {-0.3f, -1, 0}};

    private static class Cat extends Walker {
        float s, gait, tail, pt, dizzy, ears;
        int phase;
        Mouse mouse;
        float juked;
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
            m.mood = SLOW;
            m.timer = 1e9f;
            setupPaints(m);
            list.add(m);

            Cat cat = new Cat();
            float[] st = edgeStart(40 * dp);
            cat.x = st[0]; cat.y = st[1]; cat.ang = st[2];
            cat.s = s;
            cat.pt = rand(2, 4);
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
            if (Math.hypot(cat.x - x, cat.y - y) < 150 * dp && cat.phase != CHASE) {
                cat.phase = DIZZY; // bị trêu: mèo chóng mặt
                cat.pt = 1.5f;
                cat.dizzy = 0;
            }
        }
    }

    @Override protected void step(float dt) {
        for (Cat cat : cats) {
            Mouse m = cat.mouse;
            float dx = m.x - cat.x, dy = m.y - cat.y;
            float dist = (float) Math.hypot(dx, dy), toM = (float) Math.atan2(dy, dx);
            cat.pt -= dt;
            float cv = 0, mv = 0;
            switch (cat.phase) {
                case WANDER:
                    cv = 45 * dp;
                    if (!bounce(cat, 60 * dp, 2f, dt)) {
                        cat.turn += rand(-1f, 1f) * 2f * dt;
                        cat.turn *= 0.98f;
                        cat.ang += cat.turn * dt;
                    }
                    m.mood = SLOW;
                    mv = 30 * dp;
                    if (cat.pt <= 0) { cat.phase = STALK; cat.pt = rand(2.5f, 4f); }
                    break;
                case STALK: // rình, chuột vẫn hít ngửi chưa biết gì
                    cv = 32 * dp;
                    cat.ang += clamp(angDiff(toM, cat.ang), -1.5f * dt, 1.5f * dt);
                    m.mood = (time % 3f) < 1.5f ? FREEZE : SLOW;
                    mv = m.mood == FREEZE ? 0 : 25 * dp;
                    if (dist < 170 * dp || cat.pt <= 0) { cat.phase = CHASE; cat.pt = rand(4f, 6.5f); }
                    break;
                case CHASE: {
                    cv = 240 * dp;
                    cat.ang += clamp(angDiff(toM, cat.ang), -2.6f * dt, 2.6f * dt);
                    m.mood = DASH;
                    mv = 270 * dp;
                    float away = (float) Math.atan2(-dy, -dx) + sin(time * 6f) * 0.6f;
                    if (!bounce(m, 40 * dp, 9f, dt)) m.ang += clamp(angDiff(away, m.ang), -7f * dt, 7f * dt);
                    if (dist < 45 * dp && cat.juked <= 0) { // lách né
                        m.ang += (rnd.nextBoolean() ? 1 : -1) * 1.7f;
                        cat.juked = 0.5f;
                    }
                    if (cat.juked > 0) cat.juked -= dt;
                    if (cat.pt <= 0 || dist < 22 * dp) {
                        if (rnd.nextFloat() < 0.45f) { cat.phase = DIZZY; cat.pt = 2; cat.dizzy = 0; }
                        else { cat.phase = REST; cat.pt = rand(2.5f, 4f); }
                    }
                    break;
                }
                case REST: // mèo ngồi thở, chuột đứng xa ngó lại
                    m.mood = FREEZE;
                    m.ang += clamp(angDiff((float) Math.atan2(-dy, -dx) + 3.1416f, m.ang), -3f * dt, 3f * dt);
                    if (cat.pt <= 0) { cat.phase = WANDER; cat.pt = rand(2f, 4f); }
                    break;
                default: // chóng mặt
                    cat.dizzy += dt;
                    cat.ang += sin(time * 5f) * 1.5f * dt;
                    m.mood = WALK;
                    mv = 60 * dp;
                    if (cat.pt <= 0) { cat.phase = WANDER; cat.pt = rand(2f, 4f); }
                    break;
            }
            if (cat.phase != WANDER) bounce(cat, 30 * dp, 3f, dt);
            if (cat.phase != CHASE && !bounce(m, 40 * dp, 6f, dt) && mv > 0) {
                m.turn += rand(-3f, 3f) * dt;
                m.turn *= 0.97f;
                m.ang += m.turn * dt;
            }
            cat.v = lerp(cat.v, cv, Math.min(1f, dt * (cat.phase == CHASE ? 3f : 4f)));
            m.v = lerp(m.v, mv, Math.min(1f, dt * 6f));
            cat.x += cos(cat.ang) * cat.v * dt;
            cat.y += sin(cat.ang) * cat.v * dt;
            m.x += cos(m.ang) * m.v * dt;
            m.y += sin(m.ang) * m.v * dt;
            cat.gait += cat.v * dt / (cat.s * 0.12f);
            cat.tail += dt * (cat.phase == STALK ? 6f : 2f);
            cat.ears = lerp(cat.ears, cat.phase == STALK ? 1f : 0f, Math.min(1f, dt * 5f));
            m.gait += m.v * dt / (m.s * 0.11f);
            m.tail += dt * (1.2f + m.v / (45 * dp));
            m.sniff += dt * (m.mood == FREEZE ? 18f : 4f);
            float wantHead = m.mood == FREEZE
                    ? clamp(angDiff((float) Math.atan2(cat.y - m.y, cat.x - m.x), m.ang), -0.6f, 0.6f) : 0f;
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
            float ny = py + sin(ang) * 0.045f * s * 1.6f + (k.phase == STALK ? sin(k.tail * 2f) * t * 0.01f * s : 0f);
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
        float pw = (k.phase == STALK || k.phase == CHASE) ? 0.022f : 0.009f; // đồng tử nở khi rình
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
