package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudModule implements CrestModule, RenderableModule {
    protected int x;
    protected int y;

    protected HudModule(int defaultX, int defaultY) {
        this.x = defaultX;
        this.y = defaultY;
    }

    public abstract void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d);
    public abstract int getWidth();
    public abstract int getHeight();

    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public void loadSettings() {}

    @Override
    public String getCategory() { return "HUD"; }
}
