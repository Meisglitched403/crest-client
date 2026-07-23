package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SplitPane implements Widget {
    public enum Orientation { HORIZONTAL, VERTICAL }

    private final Orientation orientation;
    private Widget left, right;
    private float dividerPos = 0.5f;
    private int dividerWidth = 6;
    private int minSize = 50;
    private boolean dragging = false;
    private int dragStart = 0;
    private float dragStartPos = 0f;
    private int x, y, width;
    private int mx, my;

    public SplitPane(Orientation orientation) {
        this.orientation = orientation;
    }

    public SplitPane left(Widget w) { this.left = w; return this; }
    public SplitPane right(Widget w) { this.right = w; return this; }
    public SplitPane dividerWidth(int w) { this.dividerWidth = w; return this; }
    public SplitPane minSize(int s) { this.minSize = s; return this; }
    public SplitPane dividerPos(float p) { this.dividerPos = Math.max(0.01f, Math.min(0.99f, p)); return this; }

    @Override
    public int getHeight() {
        return 200;
    }

    private int dividerActualW() {
        int maxW = orientation == Orientation.HORIZONTAL ? width : 0;
        int maxH = orientation == Orientation.VERTICAL ? height() : 0;
        return Math.min(dividerWidth, Math.max(2, Math.max(maxW, maxH) / 10));
    }

    private int height() {
        return left != null ? left.getHeight() : (right != null ? right.getHeight() : 200);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.mx = mx;
        this.my = my;
        int h = height();

        int dw = dividerActualW();
        int contentW = w - dw;
        int leftW = (int) (contentW * dividerPos);
        int rightW = contentW - leftW;

        if (orientation == Orientation.HORIZONTAL) {
            if (left != null) left.render(g, font, x, y, leftW, mx, my, delta);
            if (right != null) right.render(g, font, x + leftW + dw, y, rightW, mx, my, delta);
            g.fill(x + leftW, y, x + leftW + dw, y + h, 0x40808080);
        } else {
            int dh = Math.min(dividerWidth, Math.max(2, h / 10));
            int contentH = h - dh;
            int topH = (int) (contentH * dividerPos);
            int botH = contentH - topH;

            if (left != null) left.render(g, font, x, y, w, mx, my, delta);
            if (right != null) right.render(g, font, x, y + topH + dh, w, mx, my, delta);
            g.fill(x, y + topH, x + w, y + topH + dh, 0x40808080);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int dw = dividerActualW();
            if (orientation == Orientation.HORIZONTAL) {
                int leftW = (int) ((width - dw) * dividerPos);
                if (mx >= x + leftW && mx <= x + leftW + dw) {
                    dragging = true;
                    dragStart = (int) mx;
                    dragStartPos = dividerPos;
                    return true;
                }
            } else {
                int dh = Math.min(dividerWidth, Math.max(2, height() / 10));
                int topH = (int) ((height() - dh) * dividerPos);
                if (my >= y + topH && my <= y + topH + dh) {
                    dragging = true;
                    dragStart = (int) my;
                    dragStartPos = dividerPos;
                    return true;
                }
            }
        }

        int dw = dividerActualW();
        if (orientation == Orientation.HORIZONTAL) {
            int leftW = (int) ((width - dw) * dividerPos);
            if (mx < x + leftW && left != null) return left.mouseClicked(mx, my, button);
            if (mx > x + leftW + dw && right != null) return right.mouseClicked(mx, my, button);
        } else {
            int dh = Math.min(dividerWidth, Math.max(2, height() / 10));
            int topH = (int) ((height() - dh) * dividerPos);
            if (my < y + topH && left != null) return left.mouseClicked(mx, my, button);
            if (my > y + topH + dh && right != null) return right.mouseClicked(mx, my, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my) {
        if (dragging) {
            int dw = dividerActualW();
            if (orientation == Orientation.HORIZONTAL) {
                float delta = (float) (mx - dragStart) / (width - dw);
                dividerPos = Math.max(0.01f, Math.min(0.99f, dragStartPos + delta));
            } else {
                int dh = Math.min(dividerWidth, Math.max(2, height() / 10));
                float delta = (float) (my - dragStart) / (height() - dh);
                dividerPos = Math.max(0.01f, Math.min(0.99f, dragStartPos + delta));
            }
            return true;
        }

        int dw = dividerActualW();
        if (orientation == Orientation.HORIZONTAL) {
            int leftW = (int) ((width - dw) * dividerPos);
            if (mx < x + leftW && left != null) return left.mouseDragged(mx, my);
            if (mx > x + leftW + dw && right != null) return right.mouseDragged(mx, my);
        } else {
            int dh = Math.min(dividerWidth, Math.max(2, height() / 10));
            int topH = (int) ((height() - dh) * dividerPos);
            if (my < y + topH && left != null) return left.mouseDragged(mx, my);
            if (my > y + topH + dh && right != null) return right.mouseDragged(mx, my);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (left != null && left.keyPressed(key, scan, mods)) return true;
        if (right != null && right.keyPressed(key, scan, mods)) return true;
        return false;
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        if (left != null && left.charTyped(codepoint, mods)) return true;
        if (right != null && right.charTyped(codepoint, mods)) return true;
        return false;
    }
}
