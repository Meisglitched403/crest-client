package com.crest.client.ui.layout;

public class LayoutEngine {
    public static void layoutFill(LayoutNode node) {
        for (LayoutNode child : node.children) {
            int cx = node.getContentX() + child.marginStart;
            int cy = node.getContentY() + child.marginTop;
            int cw = node.getInnerWidth() - child.marginStart - child.marginEnd;
            int ch = node.getInnerHeight() - child.marginTop - child.marginBottom;

            if (child.minWidth != null) cw = Math.max(child.minWidth, cw);
            if (child.maxWidth != null) cw = Math.min(child.maxWidth, cw);
            if (child.minHeight != null) ch = Math.max(child.minHeight, ch);
            if (child.maxHeight != null) ch = Math.min(child.maxHeight, ch);

            child.layout(cx, cy, cw, ch);
        }
    }

    public static void layoutRow(LayoutNode node, int gap) {
        int totalWeight = 0;
        int totalFixed = 0;
        int visibleCount = node.children.size();

        for (LayoutNode child : node.children) {
            if (child.weight > 0) totalWeight += (int) child.weight;
            else {
                int cw = child.minWidth != null ? child.minWidth : 0;
                totalFixed += cw + child.marginStart + child.marginEnd;
            }
        }
        totalFixed += gap * Math.max(0, visibleCount - 1);

        int avail = node.getInnerWidth() - totalFixed;
        int cx = node.getContentX();
        int cy = node.getContentY();
        int rowH = node.getInnerHeight();

        int weightedCount = 0;
        for (LayoutNode child : node.children) {
            int cw, ch;
            if (child.weight > 0) {
                int share = totalWeight > 0 ? (int) (avail * (child.weight / totalWeight)) : avail / Math.max(1, totalWeight);
                cw = share;
                weightedCount++;
            } else {
                cw = child.minWidth != null ? child.minWidth : 0;
            }

            cw = Math.max(0, cw);
            ch = rowH - child.marginTop - child.marginBottom;
            if (child.minHeight != null) ch = Math.max(child.minHeight, ch);
            if (child.maxHeight != null) ch = Math.min(child.maxHeight, ch);

            int childX = cx + child.marginStart;
            int childY = cy + child.marginTop;
            child.layout(childX, childY, cw, ch);

            cx += child.getOuterWidth() + gap;
        }
    }

    public static void layoutColumn(LayoutNode node, int gap) {
        int totalWeight = 0;
        int totalFixed = 0;
        int visibleCount = node.children.size();

        for (LayoutNode child : node.children) {
            if (child.weight > 0) totalWeight += (int) child.weight;
            else {
                int ch = child.minHeight != null ? child.minHeight : 0;
                totalFixed += ch + child.marginTop + child.marginBottom;
            }
        }
        totalFixed += gap * Math.max(0, visibleCount - 1);

        int avail = node.getInnerHeight() - totalFixed;
        int cx = node.getContentX();
        int cy = node.getContentY();
        int colW = node.getInnerWidth();

        for (LayoutNode child : node.children) {
            int cw, ch;
            if (child.weight > 0) {
                int share = totalWeight > 0 ? (int) (avail * (child.weight / totalWeight)) : avail / Math.max(1, totalWeight);
                ch = share;
            } else {
                ch = child.minHeight != null ? child.minHeight : 0;
            }

            ch = Math.max(0, ch);
            cw = colW - child.marginStart - child.marginEnd;
            if (child.minWidth != null) cw = Math.max(child.minWidth, cw);
            if (child.maxWidth != null) cw = Math.min(child.maxWidth, cw);

            int childX = cx + child.marginStart;
            int childY = cy + child.marginTop;
            child.layout(childX, childY, cw, ch);

            cy += child.getOuterHeight() + gap;
        }
    }

    public static void layoutGrid(LayoutNode node, int colCount, int gap) {
        int childCount = node.children.size();
        if (childCount == 0) return;

        int cols = colCount > 0 ? colCount : (int) Math.ceil(Math.sqrt(childCount));
        int rows = (int) Math.ceil((double) childCount / cols);

        int contentW = node.getInnerWidth() - gap * (cols - 1);
        int contentH = node.getInnerHeight() - gap * (rows - 1);
        int cellW = contentW / cols;
        int cellH = contentH / rows;

        int idx = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (idx >= childCount) break;
                LayoutNode child = node.children.get(idx);

                int cx = node.getContentX() + c * (cellW + gap) + child.marginStart;
                int cy = node.getContentY() + r * (cellH + gap) + child.marginTop;
                int cw = cellW - child.marginStart - child.marginEnd;
                int ch = cellH - child.marginTop - child.marginBottom;

                if (child.minWidth != null) cw = Math.max(child.minWidth, cw);
                if (child.maxWidth != null) cw = Math.min(child.maxWidth, cw);
                if (child.minHeight != null) ch = Math.max(child.minHeight, ch);
                if (child.maxHeight != null) ch = Math.min(child.maxHeight, ch);

                child.layout(cx, cy, cw, ch);
                idx++;
            }
        }
    }

    public static void layoutStack(LayoutNode node) {
        for (LayoutNode child : node.children) {
            int cx = node.getContentX() + child.marginStart;
            int cy = node.getContentY() + child.marginTop;
            int cw = node.getInnerWidth() - child.marginStart - child.marginEnd;
            int ch = node.getInnerHeight() - child.marginTop - child.marginBottom;

            if (child.minWidth != null) cw = Math.max(child.minWidth, cw);
            if (child.maxWidth != null) cw = Math.min(child.maxWidth, cw);
            if (child.minHeight != null) ch = Math.max(child.minHeight, ch);
            if (child.maxHeight != null) ch = Math.min(child.maxHeight, ch);

            child.layout(cx, cy, cw, ch);
        }
    }

    public static int computeGridCols(int availableWidth, int minColWidth, int gap, int maxCols) {
        int cols = Math.max(1, (availableWidth + gap) / (minColWidth + gap));
        if (maxCols > 0) cols = Math.min(cols, maxCols);
        return cols;
    }

    public static int computeGridRows(int childCount, int cols) {
        return (int) Math.ceil((double) childCount / cols);
    }
}
