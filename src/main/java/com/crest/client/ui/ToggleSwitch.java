package com.crest.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ToggleSwitch {
    public static final int W = 32;
    public static final int H = 14;

    public static void render(GuiGraphicsExtractor g, int x, int y, boolean on, float anim) {
        int bgColor = on
            ? ColorUtil.lerpARGB(0xFF333366, Theme.getAnimatedAccent(), anim)
            : ColorUtil.lerpARGB(0xFF333366, 0xFF555588, anim);
        int bw = (int) (W * 0.8f + W * 0.2f * anim);
        int bx = x + (W - bw) / 2;
        // Pill background
        int r = H / 2;
        g.fill(bx + r, y, bx + bw - r, y + H, bgColor);
        g.fill(bx, y + 1, bx + r, y + H - 1, bgColor);
        g.fill(bx + 1, y, bx + r - 1, y + 1, bgColor);
        g.fill(bx + 1, y + H - 1, bx + r - 1, y + H, bgColor);
        g.fill(bx + bw - r, y + 1, bx + bw, y + H - 1, bgColor);
        g.fill(bx + bw - r + 1, y, bx + bw - 1, y + 1, bgColor);
        g.fill(bx + bw - r + 1, y + H - 1, bx + bw - 1, y + H, bgColor);
        // Knob
        int knobSize = H - 4;
        int knobX = on ? bx + bw - knobSize - 2 : bx + 2;
        int knobY = y + 2;
        int knobR = knobSize / 2;
        int knobColor = ColorUtil.lerpARGB(0xFF888899, 0xFFFFFFFF, anim);
        // Rounded knob
        g.fill(knobX + knobR, knobY, knobX + knobSize - knobR, knobY + knobSize, knobColor);
        g.fill(knobX, knobY + 1, knobX + knobR, knobY + knobSize - 1, knobColor);
        g.fill(knobX + 1, knobY, knobX + knobR - 1, knobY + 1, knobColor);
        g.fill(knobX + 1, knobY + knobSize - 1, knobX + knobR - 1, knobY + knobSize, knobColor);
        g.fill(knobX + knobSize - knobR, knobY + 1, knobX + knobSize, knobY + knobSize - 1, knobColor);
        g.fill(knobX + knobSize - knobR + 1, knobY, knobX + knobSize - 1, knobY + 1, knobColor);
        g.fill(knobX + knobSize - knobR + 1, knobY + knobSize - 1, knobX + knobSize - 1, knobY + knobSize, knobColor);
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
