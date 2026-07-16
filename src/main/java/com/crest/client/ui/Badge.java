package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Small pill label. */
public final class Badge {
    public enum Variant { DEFAULT, SECONDARY, OUTLINE, DESTRUCTIVE }

    private Badge() {}

    public static void render(GuiGraphicsExtractor g, Font font, String label, int x, int y, Variant variant) {
        render(g, font, label, x, y, variant, Theme.getAnimatedAccent());
    }

    public static void render(GuiGraphicsExtractor g, Font font, String label, int x, int y, Variant variant, int accent) {
        int tw = font.width(label);
        int pw = tw + Spacing.S2;
        int ph = 16;
        int bg, fg;
        switch (variant) {
            case SECONDARY:
                bg = ColorUtil.withAlpha(Theme.MUTED, 150);
                fg = Theme.MUTED_FOREGROUND;
                break;
            case OUTLINE:
                bg = 0;
                fg = Theme.MUTED_FOREGROUND;
                Panel.draw(g, x, y, pw, ph, ColorUtil.withAlpha(Theme.BORDER, 100));
                g.centeredText(font, Component.literal(label), x + pw / 2, y + (ph - font.lineHeight) / 2 + 1, fg);
                return;
            case DESTRUCTIVE:
                bg = ColorUtil.withAlpha(Theme.DESTRUCTIVE, 200);
                fg = Theme.DESTRUCTIVE_FOREGROUND;
                break;
            default:
                bg = ColorUtil.withAlpha(accent, 200);
                fg = Theme.PRIMARY_FOREGROUND;
                break;
        }
        Panel.draw(g, x, y, pw, ph, bg);
        g.centeredText(font, Component.literal(label), x + pw / 2, y + (ph - font.lineHeight) / 2 + 1, fg);
    }
}
