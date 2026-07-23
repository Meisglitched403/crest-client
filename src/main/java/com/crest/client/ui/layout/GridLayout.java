package com.crest.client.ui.layout;

public class GridLayout extends LayoutNode {
    private int cols = 0;
    private int gap = 0;
    private int minColWidth = 0;
    private int maxCols = 0;

    public GridLayout cols(int c) { this.cols = c; return this; }
    public GridLayout gap(int g) { this.gap = g; return this; }
    public GridLayout minColWidth(int w) { this.minColWidth = w; return this; }
    public GridLayout maxCols(int m) { this.maxCols = m; return this; }

    @Override
    public void layout(int x, int y, int width, int height) {
        super.layout(x, y, width, height);

        int actualCols = cols;
        if (actualCols <= 0 && minColWidth > 0) {
            actualCols = LayoutEngine.computeGridCols(getInnerWidth(), minColWidth, gap, maxCols);
        }
        if (actualCols <= 0) actualCols = 1;

        LayoutEngine.layoutGrid(this, actualCols, gap);
    }
}
