package com.crest.client.ui;

import com.crest.client.ui.layout.LayoutEngine;
import com.crest.client.ui.layout.LayoutNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ResponsiveGrid implements Widget {
    private final List<Item> items = new ArrayList<>();
    private int minColWidth = 200;
    private int gap = 12;
    private int maxCols = 0;
    private int fixedCols = 0;
    private int x, y, width;
    private int mx, my;
    private int computedHeight = 100;

    protected static class Item {
        final LayoutNode node;
        final Widget widget;

        Item(LayoutNode node, Widget widget) {
            this.node = node;
            this.widget = widget;
        }
    }

    public ResponsiveGrid minColWidth(int w) { this.minColWidth = w; return this; }
    public ResponsiveGrid gap(int g) { this.gap = g; return this; }
    public ResponsiveGrid maxCols(int m) { this.maxCols = m; return this; }
    public ResponsiveGrid cols(int c) { this.fixedCols = c; return this; }

    public ResponsiveGrid add(Widget widget, LayoutNode node) {
        items.add(new Item(node, widget));
        return this;
    }

    public ResponsiveGrid add(Widget widget) {
        LayoutNode node = new LayoutNode();
        items.add(new Item(node, widget));
        return this;
    }

    @Override
    public int getHeight() {
        return computedHeight;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.mx = mx;
        this.my = my;

        if (items.isEmpty()) {
            computedHeight = 0;
            return;
        }

        int cols = fixedCols > 0 ? fixedCols : LayoutEngine.computeGridCols(w, minColWidth, gap, maxCols);
        cols = Math.max(1, cols);
        int rows = LayoutEngine.computeGridRows(items.size(), cols);
        int cellH = 60;

        computedHeight = rows * (cellH + gap) - gap;

        int contentW = w - gap * (cols - 1);
        int cellW = contentW / cols;

        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (idx >= items.size()) break;
                Item item = items.get(idx);

                int cx = x + c * (cellW + gap) + item.node.marginStart;
                int cy = y + r * (cellH + gap) + item.node.marginTop;
                int cw = cellW - item.node.marginStart - item.node.marginEnd;
                int ch = cellH - item.node.marginTop - item.node.marginBottom;

                if (item.node.minWidth != null) cw = Math.max(item.node.minWidth, cw);
                if (item.node.maxWidth != null) cw = Math.min(item.node.maxWidth, cw);
                if (item.node.minHeight != null) ch = Math.max(item.node.minHeight, ch);
                if (item.node.maxHeight != null) ch = Math.min(item.node.maxHeight, ch);

                item.node.layout(cx, cy, cw, ch);
                item.widget.render(g, font, cx, cy, cw, mx, my, delta);
                idx++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Item item : items) {
            if (mx >= item.node.x && mx <= item.node.x + item.node.width &&
                my >= item.node.y && my <= item.node.y + item.node.height) {
                return item.widget.mouseClicked(mx, my, button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my) {
        for (Item item : items) {
            if (mx >= item.node.x && mx <= item.node.x + item.node.width &&
                my >= item.node.y && my <= item.node.y + item.node.height) {
                if (item.widget.mouseDragged(mx, my)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        for (Item item : items) {
            if (item.widget.keyPressed(key, scan, mods)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        for (Item item : items) {
            if (item.widget.charTyped(codepoint, mods)) return true;
        }
        return false;
    }
}
