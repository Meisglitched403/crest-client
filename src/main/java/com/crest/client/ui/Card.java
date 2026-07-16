package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Static helper for drawing elevated glass cards with optional title. */
public final class Card {
    private Card() {}

    public static int draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h,
                           String title, int accent, int elevation) {
        Panel.drawGlassElevated(g, x, y, w, h, ColorUtil.withAlpha(Theme.CARD, 235), accent, elevation);
        if (title != null && !title.isEmpty()) {
            g.text(font, Component.literal(title), x + Spacing.S3, y + Spacing.S2, Theme.MUTED_FOREGROUND);
            return y + Spacing.S2 + font.lineHeight + Spacing.S1;
        }
        return y + Spacing.S2;
    }
}
