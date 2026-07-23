package com.crest.client.ui;

import com.crest.client.ui.layout.LayoutNode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ResponsiveContainer extends Container {
    private Breakpoints.Size currentSize = null;
    private final List<Breakpoints.BreakpointObserver> observers = new ArrayList<>();

    public ResponsiveContainer() {
        super();
    }

    public ResponsiveContainer(LayoutNode root) {
        super(root);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        Breakpoints.Size newSize = Breakpoints.getCurrentSize(w);
        if (currentSize != null && newSize != currentSize) {
            onBreakpointChanged(currentSize, newSize, w);
        }
        currentSize = newSize;

        super.render(g, font, x, y, w, mx, my, delta);
    }

    protected void onBreakpointChanged(Breakpoints.Size oldSize, Breakpoints.Size newSize, int screenWidth) {
        for (Breakpoints.BreakpointObserver obs : observers) {
            obs.onBreakpointChanged(oldSize, newSize, screenWidth);
        }
    }

    public void addObserver(Breakpoints.BreakpointObserver obs) {
        observers.add(obs);
    }

    public Breakpoints.Size getCurrentSize() {
        return currentSize != null ? currentSize : Breakpoints.Size.MD;
    }

    public boolean isXs() { return getCurrentSize() == Breakpoints.Size.XS; }
    public boolean isSm() { return getCurrentSize() == Breakpoints.Size.SM; }
    public boolean isMd() { return getCurrentSize() == Breakpoints.Size.MD; }
    public boolean isLg() { return getCurrentSize() == Breakpoints.Size.LG; }
    public boolean isXl() { return getCurrentSize() == Breakpoints.Size.XL || getCurrentSize() == Breakpoints.Size.XXL; }
}
