package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Static helper for drawing elevated glass cards with optional title. */
public final class Card {
    private Card() {}

    public static int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                           String title, int accent, int elevation) {
        int pad = Spacing.densityAdjusted(12);
        int titlePad = Spacing.densityAdjusted(8);
        int lineH = Spacing.densityAdjusted(9);
        Panel.drawGlassElevated(g, x, y, w, h, ColorUtil.withAlpha(Theme.CARD, 235), accent, elevation);
        if (title != null && !title.isEmpty()) {
            g.text(font, Component.literal(title), x + pad, y + titlePad, Theme.MUTED_FOREGROUND);
            return y + titlePad + lineH + Spacing.densityAdjusted(4);
        }
        return y + pad;
    }
}
