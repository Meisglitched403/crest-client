package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class ScrollContainer implements Widget {
    public float scrollOffset, scrollTarget;
    public int x, y, w, h;
    public int rowH;
    public int contentH;
    public List<? extends Widget> children;
    public int hoverColor;

    private final Animated scrollbarAlpha = new Animated(0f, 10f);
    private long lastScrollTime;

    public ScrollContainer rowHeight(int rh) { this.rowH = rh; return this; }

    public ScrollContainer children(List<? extends Widget> children) {
        this.children = children;
        if (children != null) {
            contentH = 8;
            for (Widget w : children) {
                contentH += w.getHeight();
            }
        } else {
            contentH = 0;
        }
        return this;
    }

    @Override
    public int getWidth() {
        return w;
    }

    public int getHeight() { return h > 0 ? h : contentH; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x; this.y = y; this.w = w;
        if (h <= 0) h = Math.min(contentH + 8, 500);
        render(g, font, mx, my, delta);
    }

    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float delta) {
        int maxH = Math.max(0, contentH - h);
        scrollTarget = Anim.clamp(scrollTarget, 0, maxH);
        scrollOffset += (scrollTarget - scrollOffset) * Anim.smooth(delta, 18f);

        boolean hovering = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean hasScroll = maxH > 0;
        if (hasScroll) {
            if (hovering) {
                scrollbarAlpha.set(1f);
                lastScrollTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastScrollTime > 1500) {
                scrollbarAlpha.set(0f);
            }
        } else {
            scrollbarAlpha.set(0f);
        }
        scrollbarAlpha.tick(delta);

        g.enableScissor(x, y, x + w, y + h);
        int sy = y - (int) scrollOffset;
        int currentY = sy;
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            int childH = child.getHeight();
            int cy = currentY;
            if (cy + childH < y) {
                currentY += childH;
                continue;
            }
            if (cy > y + h) break;
            boolean hover = hoverColor != 0 && mx >= x && mx <= x + w && my >= cy && my <= cy + childH - 2;
            if (hover) {
                g.fill(x, cy, x + w, cy + childH - 2, hoverColor);
            }
            child.render(g, font, x + 4, cy, w - 8, mx, my, delta);
            currentY += childH;
        }
        g.disableScissor();

        if (hasScroll) {
            float alpha = scrollbarAlpha.get();
            if (alpha > 0.01f) {
                int trackX = x + w - 4;
                int thumbH = Math.max(8, Math.round((float) h / contentH * h));
                int thumbY = Math.round(scrollOffset / contentH * h);
                g.fill(trackX, y, trackX + 2, y + h, ColorUtil.withAlpha(Theme.BG_BASE, (int) (200 * alpha)));
                g.fill(trackX, y + thumbY, trackX + 2, y + thumbY + thumbH,
                    ColorUtil.withAlpha(Theme.getAnimatedAccent(), (int) (255 * alpha)));
            }
        }
    }

    public Widget childAt(double my) {
        int currentY = y - (int) scrollOffset;
        for (Widget child : children) {
            int childH = child.getHeight();
            if (my >= currentY && my < currentY + childH) {
                return child;
            }
            currentY += childH;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!(mx >= x && mx <= x + w && my >= y && my <= y + h)) return false;
        Widget child = childAt(my);
        if (child != null) return child.mouseClicked(mx, my, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my) {
        if (!(mx >= x && mx <= x + w && my >= y && my <= y + h)) return false;
        Widget child = childAt(my);
        if (child != null) return child.mouseDragged(mx, my);
        return false;
    }

    public void mouseScrolled(double deltaY) {
        int maxH = Math.max(0, contentH - h);
        scrollTarget = Anim.clamp(scrollTarget - (float) deltaY * 3, 0, maxH);
        lastScrollTime = System.currentTimeMillis();
    }
}
