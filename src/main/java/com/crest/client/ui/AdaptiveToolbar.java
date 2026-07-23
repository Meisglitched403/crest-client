package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class AdaptiveToolbar implements Widget {
    private final List<ToolbarItem> items = new ArrayList<>();
    private int itemWidth = 72;
    private int itemHeight = 24;
    private int gap = Spacing.S2;
    private boolean iconsOnly = false;
    private int x, y, width, height;
    private int mx, my;
    private int hoveredIndex = -1;

    public static class ToolbarItem {
        public final String label;
        public final String icon;
        public final Runnable onClick;
        public boolean enabled = true;

        public ToolbarItem(String label, String icon, Runnable onClick) {
            this.label = label;
            this.icon = icon;
            this.onClick = onClick;
        }

        public ToolbarItem(String label, Runnable onClick) {
            this(label, null, onClick);
        }
    }

    public AdaptiveToolbar item(String label, Runnable onClick) {
        items.add(new ToolbarItem(label, onClick));
        return this;
    }

    public AdaptiveToolbar item(String label, String icon, Runnable onClick) {
        items.add(new ToolbarItem(label, icon, onClick));
        return this;
    }

    public AdaptiveToolbar itemWidth(int w) { this.itemWidth = w; return this; }
    public AdaptiveToolbar itemHeight(int h) { this.itemHeight = h; return this; }
    public AdaptiveToolbar gap(int g) { this.gap = g; return this; }
    public AdaptiveToolbar iconsOnly(boolean b) { this.iconsOnly = b; return this; }

    @Override
    public int getHeight() {
        return itemHeight;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = itemHeight;
        this.mx = mx;
        this.my = my;

        int visibleCount = (int) Math.floor((double) (w + gap) / (itemWidth + gap));
        visibleCount = Math.max(1, Math.min(visibleCount, items.size()));

        hoveredIndex = -1;
        int bx = x;
        for (int i = 0; i < visibleCount; i++) {
            ToolbarItem item = items.get(i);
            boolean hover = mx >= bx && mx <= bx + itemWidth && my >= y && my <= y + itemHeight;
            if (hover) hoveredIndex = i;

            int fill = hover ? ColorUtil.withAlpha(Theme.getAnimatedAccent(), 220) : ColorUtil.withAlpha(Theme.CARD, 230);
            Panel.draw(g, bx, y, itemWidth, itemHeight, fill);
            Panel.drawHollowRect(g, bx, y, itemWidth, itemHeight, Theme.BORDER_LIGHT);

            String label = item.label;
            g.text(font, label, bx + (itemWidth - font.width(label)) / 2, y + (itemHeight - font.lineHeight) / 2,
                hover ? Theme.FOREGROUND : Theme.MUTED_FOREGROUND);

            bx += itemWidth + gap;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (mx < x || mx > x + width || my < y || my > y + height) return false;

        int visibleCount = (int) Math.floor((double) (width + gap) / (itemWidth + gap));
        visibleCount = Math.max(1, Math.min(visibleCount, items.size()));

        int bx = x;
        for (int i = 0; i < visibleCount; i++) {
            if (mx >= bx && mx <= bx + itemWidth && my >= y && my <= y + itemHeight) {
                ToolbarItem item = items.get(i);
                if (item.enabled && item.onClick != null) {
                    item.onClick.run();
                }
                return true;
            }
            bx += itemWidth + gap;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        return false;
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        return false;
    }
}
