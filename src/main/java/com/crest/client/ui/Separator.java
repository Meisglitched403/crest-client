package com.crest.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Horizontal rule. */
public final class Separator {
    private Separator() {}

    public static void draw(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, ColorUtil.withAlpha(Theme.BORDER, 80));
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }
}
