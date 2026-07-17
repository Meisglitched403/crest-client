package com.crest.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ToggleSwitch {
    public static final int W = 32;
    public static final int H = 14;

    public static void render(GuiGraphicsExtractor g, int x, int y, boolean on, float anim) {
        // Track
        g.fill(x, y, x + W, y + H, ColorUtil.withAlpha(Theme.MUTED, 220));
        // Fill (accent) proportional to anim
        int bw = (int) (W * 0.8f + W * 0.2f * anim);
        int bx = x + (W - bw) / 2;
        int accent = Theme.getAnimatedAccent();
        g.fill(bx, y + 2, bx + bw, y + H - 2, on ? accent : ColorUtil.withAlpha(accent, 120));
        // Knob
        int kd = H - 4;
        int kx = on ? x + W - kd - 2 : x + 2;
        g.fill(kx, y + 2, kx + kd, y + kd + 2, Theme.FOREGROUND);
    }

    public static boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx <= x + W && my >= y && my <= y + H;
    }

    public static int knobCenterX(int x, boolean on, float anim) {
        int bw = (int) (W * 0.8f + W * 0.2f * anim);
        int bx = x + (W - bw) / 2;
        if (on) return bx + bw - (H - 2);
        return bx + 2 + (H - 4) / 2;
    }
}
