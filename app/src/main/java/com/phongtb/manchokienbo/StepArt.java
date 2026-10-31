package com.phongtb.manchokienbo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * Ảnh minh họa nhỏ cho từng bước cài đặt, vẽ bằng nét vector nên luôn sắc nét.
 * Mô phỏng màn cài đặt của máy và chỉ chỗ cần bấm.
 */
public class StepArt extends View {

    private static final int ACCENT = 0xFF8BC34A;
    private final int step;
    private final boolean en;
    private final float d;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();

    public StepArt(Context c, int step, boolean en) {
        super(c);
        this.step = step;
        this.en = en;
        d = c.getResources().getDisplayMetrics().density;
        t.setTypeface(Typeface.DEFAULT);
    }

    private String L(String e, String v) { return en ? e : v; }

    @Override protected void onDraw(Canvas c) {
        float W = getWidth(), H = getHeight();
        // khung điện thoại
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF2B2F2D);
        r.set(0, 0, W, H);
        c.drawRoundRect(r, 12 * d, 12 * d, p);
        p.setColor(0xFFF4F6F5);
        r.set(3 * d, 3 * d, W - 3 * d, H - 3 * d);
        c.drawRoundRect(r, 10 * d, 10 * d, p);

        // thanh tiêu đề
        String title;
        switch (step) {
            case 1: title = L("App info", "Thông tin ứng dụng"); break;
            case 2: title = L("Display over other apps", "Hiển thị trên ứng dụng khác"); break;
            case 3: title = L("App battery usage", "Mức dùng pin"); break;
            default: title = L("Notifications", "Thông báo"); break;
        }
        t.setColor(0xFF1F2421);
        t.setTextSize(8.5f * d);
        t.setFakeBoldText(true);
        c.drawText(fit(title, W - 30 * d), 10 * d, 17 * d, t);
        t.setFakeBoldText(false);
        p.setColor(0xFFDADFDC);
        c.drawRect(3 * d, 23 * d, W - 3 * d, 23.6f * d, p);

        switch (step) {
            case 1: drawStep1(c, W, H); break;
            case 2: drawToggleRow(c, W, H, "Crawlscape", true); break;
            case 3: drawStep3(c, W, H); break;
            default: drawToggleRow(c, W, H, L("Allow notifications", "Bật thông báo"), false); break;
        }
    }

    private String fit(String s, float maxW) {
        if (t.measureText(s) <= maxW) return s;
        while (s.length() > 3 && t.measureText(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Bấm ⋮ ở góc → "Cho phép cài đặt bị hạn chế" */
    private void drawStep1(Canvas c, float W, float H) {
        // dấu ba chấm góc phải
        p.setColor(0xFF1F2421);
        for (int i = 0; i < 3; i++) c.drawCircle(W - 12 * d, (11 + i * 3.5f) * d, 1.2f * d, p);
        // vài dòng mờ
        p.setColor(0xFFDADFDC);
        for (int i = 0; i < 3; i++) {
            r.set(10 * d, (34 + i * 13) * d, W * 0.55f, (38 + i * 13) * d);
            c.drawRoundRect(r, 2 * d, 2 * d, p);
        }
        // menu thả xuống
        float mx = W * 0.32f, my = 22 * d;
        p.setColor(0x33000000);
        r.set(mx + 1.5f * d, my + 2 * d, W - 5 * d + 1.5f * d, my + 34 * d);
        c.drawRoundRect(r, 6 * d, 6 * d, p);
        p.setColor(0xFFFFFFFF);
        r.set(mx, my, W - 6 * d, my + 32 * d);
        c.drawRoundRect(r, 6 * d, 6 * d, p);
        p.setColor(withA(ACCENT, 0x55));
        r.set(mx + 2 * d, my + 2 * d, W - 8 * d, my + 16 * d);
        c.drawRoundRect(r, 4 * d, 4 * d, p);
        t.setColor(0xFF1F2421);
        t.setTextSize(6.8f * d);
        c.drawText(fit(L("Allow restricted settings", "Cho phép cài đặt hạn chế"), W - mx - 12 * d), mx + 5 * d, my + 11.5f * d, t);
        t.setColor(0xFF8A928E);
        c.drawText(fit(L("Uninstall updates", "Gỡ cài đặt bản cập nhật"), W - mx - 12 * d), mx + 5 * d, my + 26 * d, t);
        finger(c, W - 22 * d, my + 13 * d);
    }

    /** Một dòng có công tắc đang bật */
    private void drawToggleRow(Canvas c, float W, float H, String label, boolean appIcon) {
        float y = 42 * d;
        p.setColor(0xFFFFFFFF);
        r.set(8 * d, y - 13 * d, W - 8 * d, y + 13 * d);
        c.drawRoundRect(r, 6 * d, 6 * d, p);
        if (appIcon) {
            p.setColor(0xFF1E3A2B);
            r.set(13 * d, y - 8 * d, 29 * d, y + 8 * d);
            c.drawRoundRect(r, 4 * d, 4 * d, p);
            p.setColor(0xFFF4E3C3);
            c.drawCircle(21 * d, y - 3.5f * d, 2 * d, p);
            c.drawCircle(21 * d, y + 3 * d, 3 * d, p);
        } else { // cái chuông
            p.setColor(0xFFFFB300);
            path.reset();
            path.moveTo(15 * d, y + 4 * d);
            path.quadTo(16 * d, y - 8 * d, 21 * d, y - 8 * d);
            path.quadTo(26 * d, y - 8 * d, 27 * d, y + 4 * d);
            path.close();
            c.drawPath(path, p);
            c.drawCircle(21 * d, y + 6 * d, 1.8f * d, p);
        }
        t.setColor(0xFF1F2421);
        t.setTextSize(7.5f * d);
        c.drawText(fit(label, W - 75 * d), 34 * d, y + 2.5f * d, t);
        // công tắc bật
        float sx = W - 34 * d;
        p.setColor(ACCENT);
        r.set(sx, y - 6 * d, sx + 20 * d, y + 6 * d);
        c.drawRoundRect(r, 6 * d, 6 * d, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(sx + 14 * d, y, 4.5f * d, p);
        // dòng mờ bên dưới
        p.setColor(0xFFDADFDC);
        r.set(10 * d, y + 22 * d, W * 0.7f, y + 26 * d);
        c.drawRoundRect(r, 2 * d, 2 * d, p);
        finger(c, sx + 12 * d, y + 3 * d);
    }

    /** Chọn "Không hạn chế" */
    private void drawStep3(Canvas c, float W, float H) {
        String[] items = {L("Unrestricted", "Không hạn chế"), L("Optimized", "Tối ưu hóa"), L("Restricted", "Hạn chế")};
        t.setTextSize(7.5f * d);
        for (int i = 0; i < 3; i++) {
            float y = (36 + i * 17) * d;
            if (i == 0) {
                p.setColor(withA(ACCENT, 0x40));
                r.set(6 * d, y - 8 * d, W - 6 * d, y + 8 * d);
                c.drawRoundRect(r, 5 * d, 5 * d, p);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.2f * d);
            p.setColor(i == 0 ? ACCENT : 0xFF9AA39E);
            c.drawCircle(15 * d, y, 4 * d, p);
            p.setStyle(Paint.Style.FILL);
            if (i == 0) c.drawCircle(15 * d, y, 2.2f * d, p);
            t.setColor(i == 0 ? 0xFF1F2421 : 0xFF8A928E);
            c.drawText(fit(items[i], W - 30 * d), 24 * d, y + 2.5f * d, t);
        }
        finger(c, 18 * d, 39 * d);
    }

    /** Ngón tay chỉ chỗ bấm */
    private void finger(Canvas c, float x, float y) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x44FF7043);
        c.drawCircle(x, y, 9 * d, p);
        p.setColor(0xFFFF7043);
        c.drawCircle(x, y, 4 * d, p);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(x, y, 1.6f * d, p);
    }

    private static int withA(int c, int a) { return (a << 24) | (c & 0xFFFFFF); }
}
