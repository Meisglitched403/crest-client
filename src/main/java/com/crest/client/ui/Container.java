package com.crest.client.ui;

import com.crest.client.ui.layout.LayoutEngine;
import com.crest.client.ui.layout.LayoutNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class Container implements Widget {
    protected LayoutNode root;
    protected final List<Item> items = new ArrayList<>();
    protected int x, y, width, height;
    protected int mx, my;

    protected static class Item {
        final LayoutNode node;
        final Widget widget;

        Item(LayoutNode node, Widget widget) {
            this.node = node;
            this.widget = widget;
        }
    }

    public Container() {
        this.root = new LayoutNode();
    }

    public Container(LayoutNode root) {
        this.root = root;
    }

    public Container add(Widget widget, LayoutNode node) {
        items.add(new Item(node, widget));
        root.addChild(node);
        return this;
    }

    public Container add(Widget widget) {
        LayoutNode node = new LayoutNode();
        return add(widget, node);
    }

    public Container setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public int getHeight() {
        if (height > 0) return height;
        return computeContentHeight();
    }

    protected int computeContentHeight() {
        if (root.children.isEmpty()) return 0;
        LayoutNode last = root.children.get(root.children.size() - 1);
        return last.y + last.height + last.marginBottom - root.y;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        this.x = x;
        this.y = y;
        this.width = w;

        int h = getHeight();
        this.height = h;

        this.mx = mx;
        this.my = my;

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow() != null ? mc.getWindow().getGuiScaledWidth() : 0;
        int sh = mc.getWindow() != null ? mc.getWindow().getGuiScaledHeight() : 0;
        String cacheKey = "c:" + Integer.toHexString(System.identityHashCode(this)) + "@" + w + "x" + h;

        if (!LayoutCache.isValid(cacheKey, root, sw, sh)) {
            root.layout(x, y, w, h);
            LayoutCache.store(cacheKey, root, sw, sh);
        }

        for (Item item : items) {
            LayoutNode node = item.node;
            item.widget.render(g, font, node.x, node.y, node.width, mx, my, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            LayoutNode node = item.node;
            if (mx >= node.x && mx <= node.x + node.width && my >= node.y && my <= node.y + node.height) {
                if (item.widget.mouseClicked(mx, my, button)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            LayoutNode node = item.node;
            if (mx >= node.x && mx <= node.x + node.width && my >= node.y && my <= node.y + node.height) {
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

    public LayoutNode getRoot() {
        return root;
    }
}
