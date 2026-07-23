package com.crest.client.ui.layout;

import java.util.ArrayList;
import java.util.List;

public class LayoutNode {
    public int x, y, width, height;
    public int marginStart, marginEnd, marginTop, marginBottom;
    public int paddingStart, paddingEnd, paddingTop, paddingBottom;
    public Integer minWidth, maxWidth, minHeight, maxHeight;
    public float weight = 0f;
    public Alignment alignment = Alignment.FILL;
    public LayoutNode parent;
    public final List<LayoutNode> children = new ArrayList<>();

    public enum Alignment {
        FILL, START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
    }

    public LayoutNode() {}

    public LayoutNode(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public LayoutNode margin(int all) {
        marginStart = marginEnd = marginTop = marginBottom = all;
        return this;
    }

    public LayoutNode margin(int horizontal, int vertical) {
        marginStart = marginEnd = horizontal;
        marginTop = marginBottom = vertical;
        return this;
    }

    public LayoutNode margin(int start, int top, int end, int bottom) {
        marginStart = start; marginTop = top; marginEnd = end; marginBottom = bottom;
        return this;
    }

    public LayoutNode padding(int all) {
        paddingStart = paddingEnd = paddingTop = paddingBottom = all;
        return this;
    }

    public LayoutNode padding(int horizontal, int vertical) {
        paddingStart = paddingEnd = horizontal;
        paddingTop = paddingBottom = vertical;
        return this;
    }

    public LayoutNode padding(int start, int top, int end, int bottom) {
        paddingStart = start; paddingTop = top; paddingEnd = end; paddingBottom = bottom;
        return this;
    }

    public LayoutNode minWidth(int w) { this.minWidth = w; return this; }
    public LayoutNode maxWidth(int w) { this.maxWidth = w; return this; }
    public LayoutNode minHeight(int h) { this.minHeight = h; return this; }
    public LayoutNode maxHeight(int h) { this.maxHeight = h; return this; }
    public LayoutNode weight(float w) { this.weight = w; return this; }
    public LayoutNode align(Alignment a) { this.alignment = a; return this; }

    public int getOuterWidth() { return width + marginStart + marginEnd; }
    public int getOuterHeight() { return height + marginTop + marginBottom; }
    public int getInnerWidth() { return width - paddingStart - paddingEnd; }
    public int getInnerHeight() { return height - paddingTop - paddingBottom; }

    public int getContentX() { return x + paddingStart; }
    public int getContentY() { return y + paddingTop; }

    public LayoutNode addChild(LayoutNode child) {
        children.add(child);
        child.parent = this;
        return child;
    }

    public void removeChild(LayoutNode child) {
        children.remove(child);
        child.parent = null;
    }

    public void clearChildren() {
        children.clear();
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public void layout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
