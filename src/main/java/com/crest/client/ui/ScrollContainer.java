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
        contentH = children != null ? children.size() * rowH + 8 : 0;
        return this;
    }

    @Override
    public int getHeight() { return h > 0 ? h : contentH; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x; this.y = y; this.w = w;
        if (h <= 0) h = Math.min(contentH + 8, 400);
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
        for (int i = 0; i < children.size(); i++) {
            int cy = sy + i * rowH;
            if (cy + rowH < y) continue;
            if (cy > y + h) break;
            boolean hover = hoverColor != 0 && mx >= x && mx <= x + w && my >= cy && my <= cy + rowH - 2;
            if (hover) {
                g.fill(x, cy, x + w, cy + rowH - 2, hoverColor);
            }
            children.get(i).render(g, font, x + 4, cy, w - 8, mx, my, delta);
        }
        g.disableScissor();

        if (hasScroll) {
            float alpha = scrollbarAlpha.get();
            if (alpha > 0.01f) {
                int trackX = x + w - 4;
                float thumbH = (float) h / contentH * h;
                float thumbY = scrollOffset / contentH * h;
                g.fill(trackX, y, trackX + 2, y + h, ColorUtil.withAlpha(Theme.BG_BASE, (int) (200 * alpha)));
                g.fill(trackX, y + (int) thumbY, trackX + 2, y + (int) (thumbY + thumbH),
                    ColorUtil.withAlpha(Theme.getAnimatedAccent(), (int) (255 * alpha)));
            }
        }
    }

    public Widget childAt(double my) {
        int idx = (int) ((my - y + scrollOffset) / rowH);
        if (idx >= 0 && idx < children.size()) return children.get(idx);
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
