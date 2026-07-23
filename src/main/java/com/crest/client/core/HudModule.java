package com.crest.client.core;

import com.crest.client.ui.Breakpoints;
import com.crest.client.ui.ResponsiveValue;
import com.crest.client.ui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudModule implements CrestModule, RenderableModule {
    protected int x;
    protected int y;

    private Integer overrideW;
    private Integer overrideH;

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

    public Integer getOverrideW() { return overrideW; }
    public Integer getOverrideH() { return overrideH; }

    public void setSize(Integer w, Integer h) {
        this.overrideW = w;
        this.overrideH = h;
    }

    public int getRenderWidth() {
        return overrideW != null ? overrideW : getWidth();
    }

    public int getRenderHeight() {
        return overrideH != null ? overrideH : getHeight();
    }

    public void loadSettings() {}

    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 4.0f;

    protected int getSafeLeft() {
        Minecraft mc = Minecraft.getInstance();
        int margin = 4;
        if (mc.getWindow() != null && Breakpoints.isXsOrSmaller(mc.getWindow().getGuiScaledWidth())) {
            margin = 8;
        }
        return margin;
    }

    protected int getSafeRight() {
        Minecraft mc = Minecraft.getInstance();
        int margin = 4;
        if (mc.getWindow() != null && Breakpoints.isXsOrSmaller(mc.getWindow().getGuiScaledWidth())) {
            margin = 8;
        }
        return mc.getWindow() != null ? mc.getWindow().getGuiScaledWidth() - margin : margin;
    }

    protected int getSafeTop() {
        return Breakpoints.isXsOrSmaller(width()) ? 8 : 4;
    }

    protected int getSafeBottom() {
        Minecraft mc = Minecraft.getInstance();
        int margin = Breakpoints.isXsOrSmaller(width()) ? 8 : 4;
        return mc.getWindow() != null ? mc.getWindow().getGuiScaledHeight() - margin : margin;
    }

    private static int width() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getWindow() != null ? mc.getWindow().getGuiScaledWidth() : 1920;
    }

    public void renderScaled(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (overrideW == null && overrideH == null) {
            render(g, mc, d);
            return;
        }
        int natW = getWidth();
        int natH = getHeight();
        if (natW <= 0 || natH <= 0) {
            render(g, mc, d);
            return;
        }
        float sx = (float) getRenderWidth() / natW;
        float sy = (float) getRenderHeight() / natH;
        float s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(sx, sy)));

        int screenW = mc.getWindow().getGuiScaledWidth();
        int maxScale = screenW < 800 ? 1 : screenW < 1200 ? 2 : 4;
        s = Math.min(s, maxScale);

        int rx = x < 0 ? screenW - natW : x;
        int ry = y;

        g.pose().pushMatrix();
        g.pose().translate(rx, ry);
        g.pose().scale(s);
        g.pose().translate(-rx, -ry);
        render(g, mc, d);
        g.pose().popMatrix();
    }

    public boolean isOnScreen(int screenW, int screenH) {
        int rx = x < 0 ? screenW - getRenderWidth() : x;
        int ry = y;
        return rx < screenW && ry < screenH && rx + getRenderWidth() > 0 && ry + getRenderHeight() > 0;
    }

    @Override
    public String getCategory() { return "HUD"; }
}
