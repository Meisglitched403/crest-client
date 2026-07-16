package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class ScrollContainer {
    public float scrollOffset, scrollTarget;
    public int x, y, w, h;
    public int rowH;
    public int contentH;
    public List<? extends Widget> children;
    public int hoverColor;

    public ScrollContainer set(int x, int y, int w, int h, int rowH, List<? extends Widget> children) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.rowH = rowH;
        setChildren(children);
        return this;
    }

    public void setChildren(List<? extends Widget> children) {
        this.children = children;
        contentH = children.size() * rowH + 8;
    }

    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float delta) {
        int maxH = Math.max(0, contentH - h);
        scrollTarget = Anim.clamp(scrollTarget, 0, maxH);
        scrollOffset += (scrollTarget - scrollOffset) * Anim.smooth(delta, 18f);

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

        if (maxH > 0) {
            int trackX = x + w - 4;
            float thumbH = (float) h / contentH * h;
            float thumbY = scrollOffset / contentH * h;
            g.fill(trackX, y, trackX + 2, y + h, ColorUtil.withAlpha(Theme.BG_BASE, 200));
            g.fill(trackX, y + (int) thumbY, trackX + 2, y + (int) (thumbY + thumbH), Theme.getAnimatedAccent());
        }
    }

    public Widget childAt(double my) {
        int idx = (int) ((my - y + scrollOffset) / rowH);
        if (idx >= 0 && idx < children.size()) return children.get(idx);
        return null;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!(mx >= x && mx <= x + w && my >= y && my <= y + h)) return false;
        Widget child = childAt(my);
        if (child != null) return child.mouseClicked(mx, my, button);
        return false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (!(mx >= x && mx <= x + w && my >= y && my <= y + h)) return false;
        Widget child = childAt(my);
        if (child != null) return child.mouseDragged(mx, my);
        return false;
    }

    public void mouseScrolled(double deltaY) {
        int maxH = Math.max(0, contentH - h);
        scrollTarget = Anim.clamp(scrollTarget - (float) deltaY * 3, 0, maxH);
    }
}
