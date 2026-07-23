package com.crest.client.ui.layout;

public class ColumnLayout extends LayoutNode {
    private int gap = 0;

    public ColumnLayout gap(int g) { this.gap = g; return this; }

    @Override
    public void layout(int x, int y, int width, int height) {
        super.layout(x, y, width, height);
        LayoutEngine.layoutColumn(this, gap);
    }
}
