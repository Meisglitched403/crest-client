package com.crest.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ToggleSwitch {
    public static final int W = 32;
    public static final int H = 14;

    public static void render(GuiGraphicsExtractor g, int x, int y, boolean on, float anim) {
        int bgColor = on
            ? ColorUtil.lerpARGB(0xFF333355, Theme.getAnimatedAccent(), anim)
            : ColorUtil.lerpARGB(0xFF333355, 0xFF555577, anim);
        int bw = (int) (W * 0.8f + W * 0.2f * anim);
        int bx = x + (W - bw) / 2;
        g.fill(bx, y, bx + bw, y + H, bgColor);

        int knobSize = H - 4;
        int knobX = on
            ? bx + bw - knobSize - 2
            : bx + 2;
        int knobColor = ColorUtil.lerpARGB(0xFF888899, 0xFFFFFFFF, anim);
        g.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, knobColor);
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
