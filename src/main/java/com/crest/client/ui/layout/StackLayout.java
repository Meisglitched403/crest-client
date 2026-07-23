package com.crest.client.ui.layout;

public class StackLayout extends LayoutNode {
    @Override
    public void layout(int x, int y, int width, int height) {
        super.layout(x, y, width, height);
        LayoutEngine.layoutStack(this);
    }
}
