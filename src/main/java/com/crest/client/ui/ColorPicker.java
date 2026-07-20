package com.crest.client.ui;

import com.crest.client.core.setting.ColorSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Consumer;

/**
 * Reusable HSV color picker: saturation/value square, hue bar, alpha bar and hex readout.
 * Render and hit-test share one geometry source so they never drift apart.
 */
public final class ColorPicker {
    // Geometry (set per render call via layout())
    private int x, y, w, h;
    private int sq, sx, sy;          // saturation/value square
    private int hueX, hueY, hueW, hueH;
    private int alphaX, alphaY, alphaW, alphaH;
    private int readoutY;

    private int hue = 0;             // 0..359
    private int sat = 100;           // 0..100
    private int val = 100;           // 0..100
    private int alpha = 255;         // 0..255

    private Consumer<Integer> onPick;

    public ColorPicker() {}

    public ColorPicker setValue(int rgb) {
        float[] hsv = ColorUtil.toHSV(rgb);
        hue = (int) (hsv[0] * 360) % 360;
        sat = (int) (hsv[1] * 100);
        val = (int) (hsv[2] * 100);
        alpha = ColorUtil.getA(rgb);
        if (hue < 0) hue += 360;
        return this;
    }

    public ColorPicker setOnPick(Consumer<Integer> c) { this.onPick = c; return this; }

    public int getColor() {
        return ColorUtil.hsvToInt(hue / 360f, sat / 100f, val / 100f, alpha / 255f);
    }

    /** Compute picker geometry for a given top-left and total size. */
    public void layout(int px, int py, int pw, int ph) {
        this.x = px; this.y = py; this.w = pw; this.h = ph;
        int pad = Spacing.S3;
        this.sq = Math.min(pw - pad * 2 - 22, ph - pad * 2 - 40);
        this.sq = Math.max(80, sq);
        this.sx = px + pad;
        this.sy = py + pad + 18;
        this.hueX = sx + sq + Spacing.S2;
        this.hueY = sy;
        this.hueW = 14;
        this.hueH = sq;
        this.alphaX = sx;
        this.alphaY = sy + sq + Spacing.S2;
        this.alphaW = sq + hueW + Spacing.S2;
        this.alphaH = 10;
        this.readoutY = alphaY + alphaH + Spacing.S2;
    }

    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, int animAlpha) {
        int a = animAlpha;
        int accent = Theme.getAnimatedAccent();

        // saturation/value square
        for (int yy = 0; yy < sq; yy += 2) {
            for (int xx = 0; xx < sq; xx += 2) {
                float s = xx / (float) sq;
                float v = 1 - yy / (float) sq;
                int col = ColorUtil.hsvToInt(hue / 360f, s, v, 1f);
                g.fill(sx + xx, sy + yy, sx + xx + 2, sy + yy + 2, col);
            }
        }
        // hue bar
        for (int yy = 0; yy < hueH; yy += 2) {
            float hh = 1 - yy / (float) hueH;
            g.fill(hueX, sy + yy, hueX + hueW, sy + yy + 2, ColorUtil.hsvToInt(hh, 1, 1, 1f));
        }
        // alpha bar (gradient of current hue -> transparent)
        int cur = getColor();
        for (int xx = 0; xx < alphaW; xx += 2) {
            float t = xx / (float) alphaW;
            int c = ColorUtil.lerpARGB(ColorUtil.withAlpha(cur, 0), cur, t);
            g.fill(alphaX + xx, alphaY, alphaX + xx + 2, alphaY + alphaH, c);
        }
        Panel.drawHollowRect(g, alphaX, alphaY, alphaW, alphaH, Theme.BORDER_LIGHT);

        // selectors
        int selX = sx + (int) (sat / 100f * sq) - 3;
        int selY = sy + (int) ((1 - val / 100f) * sq) - 3;
        g.fill(selX, selY, selX + 6, selY + 6, 0xFFFFFFFF);
        Panel.drawHollowRect(g, selX, selY, 6, 6, 0xFF000000);
        int hy = sy + (int) ((1 - hue / 360f) * hueH) - 2;
        g.fill(hueX - 2, hy, hueX + hueW + 2, hy + 4, 0xFFFFFFFF);
        int ay = alphaY + (int) (alpha / 255f * alphaH) - 1;
        g.fill(alphaX - 2, ay, alphaX + alphaW + 2, ay + 2, 0xFFFFFFFF);

        // title + hex readout
        g.text(font, "Pick a color", sx, y + Spacing.S2, ColorUtil.withAlpha(Theme.FOREGROUND, a));
        String hex = String.format("#%06X", cur & 0xFFFFFF);
        g.text(font, hex, alphaX, readoutY, ColorUtil.withAlpha(Theme.FOREGROUND, a));
        g.text(font, "Done", x + w - Spacing.S4, readoutY, ColorUtil.withAlpha(accent, a));
    }

    /** Returns true if the click was consumed (inside the picker). */
    public boolean handleClick(double mx, double my) {
        if (mx >= x + w - Spacing.S4 && mx <= x + w - Spacing.S2 &&
            my >= readoutY - 2 && my <= readoutY + Spacing.S3) {
            if (onPick != null) onPick.accept(getColor());
            return true;
        }
        if (mx >= hueX && mx <= hueX + hueW && my >= hueY && my <= hueY + hueH) {
            hue = (int) ((1 - (my - hueY) / (float) hueH) * 360);
            apply();
            return true;
        }
        if (mx >= alphaX && mx <= alphaX + alphaW && my >= alphaY && my <= alphaY + alphaH) {
            alpha = (int) Anim.clamp((float) (mx - alphaX) / alphaW * 255, 0, 255);
            apply();
            return true;
        }
        if (mx >= sx && mx <= sx + sq && my >= sy && my <= sy + sq) {
            sat = (int) Anim.clamp((float) (mx - sx) / sq * 100, 0, 100);
            val = (int) Anim.clamp((float) (1 - (my - sy) / (double) sq) * 100, 0, 100);
            apply();
            return true;
        }
        return false;
    }

    private void apply() {
        if (onPick != null) onPick.accept(getColor());
    }
}
