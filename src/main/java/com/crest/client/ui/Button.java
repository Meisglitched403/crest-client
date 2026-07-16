package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class Button implements Widget {
    public enum Variant { PRIMARY, SECONDARY, OUTLINE, GHOST, DESTRUCTIVE, LINK }
    public enum Size { SM, DEFAULT, LG, ICON }

    private final String text;
    private final Variant variant;
    private final Size size;
    private final int accent;
    private final Runnable onClick;
    private int lastX, lastY, lastW, lastH;

    public Button(String text, Runnable onClick) {
        this(text, Variant.PRIMARY, Size.DEFAULT, Theme.getAnimatedAccent(), onClick);
    }

    public Button(String text, int accent, Runnable onClick) {
        this(text, Variant.PRIMARY, Size.DEFAULT, accent, onClick);
    }

    public Button(String text, Variant variant, Runnable onClick) {
        this(text, variant, Size.DEFAULT, Theme.getAnimatedAccent(), onClick);
    }

    public Button(String text, Variant variant, Size size, int accent, Runnable onClick) {
        this.text = text;
        this.variant = variant;
        this.size = size;
        this.accent = accent;
        this.onClick = onClick;
    }

    @Override
    public int getHeight() {
        return switch (size) {
            case SM -> 24;
            case LG -> 44;
            case ICON -> 36;
            default -> 36;
        };
    }

    private int getWidth(String text, Font font) {
        int tw = font.width(text) + (size == Size.ICON ? 0 : Spacing.S6);
        return switch (size) {
            case SM -> Math.max(tw, 60);
            case LG -> Math.max(tw, 100);
            case ICON -> getHeight();
            default -> Math.max(tw, 80);
        };
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        lastX = x; lastY = y; lastW = w; lastH = getHeight();
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + lastH;

        int bg, fg;
        boolean outline = false;
        switch (variant) {
            case SECONDARY:
                bg = hover ? ColorUtil.withAlpha(Theme.MUTED, 200) : ColorUtil.withAlpha(Theme.MUTED, 150);
                fg = Theme.FOREGROUND;
                break;
            case OUTLINE:
                bg = hover ? ColorUtil.withAlpha(Theme.MUTED, 100) : 0;
                fg = hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND;
                outline = true;
                break;
            case GHOST:
                bg = hover ? ColorUtil.withAlpha(Theme.MUTED, 100) : 0;
                fg = hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND;
                break;
            case DESTRUCTIVE:
                bg = hover ? ColorUtil.withAlpha(Theme.DESTRUCTIVE, 180) : ColorUtil.withAlpha(Theme.DESTRUCTIVE, 220);
                fg = Theme.DESTRUCTIVE_FOREGROUND;
                break;
            case LINK:
                bg = 0;
                fg = hover ? Theme.getAnimatedAccent() : Theme.MUTED_FOREGROUND;
                break;
            default: // PRIMARY
                bg = hover ? ColorUtil.withAlpha(accent, 180) : ColorUtil.withAlpha(accent, 220);
                fg = Theme.PRIMARY_FOREGROUND;
                break;
        }

        if (variant == Variant.LINK) {
            g.text(font, Component.literal(text), x + (w - font.width(text)) / 2, y + (lastH - font.lineHeight) / 2, fg);
            if (hover) {
                int tw = font.width(text);
                g.fill(x + (w - tw) / 2, y + lastH - 1, x + (w + tw) / 2, y + lastH, fg);
            }
            return;
        }

        if (outline) {
            if (bg != 0) Panel.draw(g, x, y, w, lastH, bg);
            Panel.draw(g, x, y, w, lastH, ColorUtil.withAlpha(Theme.BORDER, hover ? 180 : 100));
        } else if (bg != 0) {
            Panel.draw(g, x, y, w, lastH, bg);
        }

        if (hover && variant != Variant.OUTLINE && variant != Variant.GHOST && bg != 0) {
            Panel.draw(g, x, y, w, lastH, ColorUtil.withAlpha(0xFFFFFFFF, 20));
        }

        g.centeredText(font, Component.literal(text), x + w / 2, y + (lastH - font.lineHeight) / 2, fg);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= lastX && mx <= lastX + lastW && my >= lastY && my <= lastY + lastH) {
            onClick.run();
            return true;
        }
        return false;
    }
}
