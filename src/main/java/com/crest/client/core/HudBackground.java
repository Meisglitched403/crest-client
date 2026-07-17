package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.IntegerSetting;
import com.crest.client.core.setting.ModeSetting;
import com.crest.client.core.setting.Setting;
import com.crest.client.ui.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class HudBackground {
    public static final HudBackground INSTANCE = new HudBackground();

    public final BooleanSetting enabled = new BooleanSetting("BG Enabled", true);
    public final ColorSetting color = new ColorSetting("BG Color", 0x000000);
    public final IntegerSetting opacity = new IntegerSetting("BG Opacity", 0, 255, 0x66);
    public final ModeSetting style = new ModeSetting("BG Style",
        new String[]{"Filled", "Border", "Frosted"}, 0);
    public final IntegerSetting radius = new IntegerSetting("Corner Radius", 0, 12, 0);

    private HudBackground() {}

    public List<Setting<?>> settings() {
        return List.of(enabled, color, opacity, style, radius);
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        INSTANCE.doDraw(g, x, y, w, h);
    }

    private void doDraw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (!enabled.get() || w <= 0 || h <= 0) return;

        int a = opacity.get();
        int base = ColorUtil.argb(a, color.getR(), color.getG(), color.getB());
        int r = Math.min(radius.get(), Math.min(w, h) / 2);

        if (r > 0) {
            drawRounded(g, x, y, w, h, r, base, a, style.get());
        } else {
            drawFlat(g, x, y, w, h, base, a);
        }
    }

    private void drawFlat(GuiGraphicsExtractor g, int x, int y, int w, int h, int base, int a) {
        switch (style.get()) {
            case 1 -> { // Border
                g.fill(x, y, x + w, y + 1, base);
                g.fill(x, y + h - 1, x + w, y + h, base);
                g.fill(x, y, x + 1, y + h, base);
                g.fill(x + w - 1, y, x + w, y + h, base);
            }
            case 2 -> { // Frosted
                g.fill(x, y, x + w, y + h, base);
                int edge = ColorUtil.argb(Math.min(255, a + 40),
                    Math.min(255, color.getR() + 60),
                    Math.min(255, color.getG() + 60),
                    Math.min(255, color.getB() + 60));
                g.fill(x, y, x + w, y + 1, edge);
                g.fill(x, y, x + 1, y + h, edge);
            }
            default -> g.fill(x, y, x + w, y + h, base); // Filled
        }
    }

    private void drawRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int base, int a, int styleIdx) {
        if (styleIdx == 1) {
            drawRoundedBorderOnly(g, x, y, w, h, r, base);
            return;
        }

        // Filled rounded rect
        g.fill(x + r, y, x + w - r, y + h, base);
        g.fill(x, y + r, x + r, y + h - r, base);
        g.fill(x + w - r, y + r, x + w, y + h - r, base);

        for (int dy = 0; dy < r; dy++) {
            int stripLen = r - (int) Math.sqrt(r * r - (dy + 0.5) * (dy + 0.5));
            if (stripLen <= 0) continue;
            g.fill(x, y + dy, x + stripLen, y + dy + 1, base);
            g.fill(x + w - stripLen, y + dy, x + w, y + dy + 1, base);
            g.fill(x, y + h - 1 - dy, x + stripLen, y + h - dy, base);
            g.fill(x + w - stripLen, y + h - 1 - dy, x + w, y + h - dy, base);
        }

        if (styleIdx == 2) {
            int edge = ColorUtil.argb(Math.min(255, a + 40),
                Math.min(255, color.getR() + 60),
                Math.min(255, color.getG() + 60),
                Math.min(255, color.getB() + 60));
            g.fill(x + r, y, x + w - r, y + 1, edge);
            g.fill(x, y + r, x + 1, y + h - r, edge);
        }
    }

    private void drawRoundedBorderOnly(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int base) {
        g.fill(x + r, y, x + w - r, y + 1, base);
        g.fill(x + r, y + h - 1, x + w - r, y + h, base);
        g.fill(x, y + r, x + 1, y + h - r, base);
        g.fill(x + w - 1, y + r, x + w, y + h - r, base);

        for (int dy = 0; dy < r; dy++) {
            int stripLen = r - (int) Math.sqrt(r * r - (dy + 0.5) * (dy + 0.5));
            if (stripLen <= 0) continue;
            g.fill(x, y + dy, x + stripLen, y + dy + 1, base);
            g.fill(x + w - stripLen, y + dy, x + w, y + dy + 1, base);
            g.fill(x, y + h - 1 - dy, x + stripLen, y + h - dy, base);
            g.fill(x + w - stripLen, y + h - 1 - dy, x + w, y + h - dy, base);
        }
    }
}
