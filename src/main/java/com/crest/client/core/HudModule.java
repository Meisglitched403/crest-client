package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudModule implements CrestModule, RenderableModule {
    protected int x;
    protected int y;

    // Optional explicit size override (pixels). When set, the EditGUI box and
    // anchor math use these instead of the module's computed getWidth/getHeight.
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

    /** Width used for EditGUI box + anchor math: override when present, else computed. */
    public int getRenderWidth() {
        return overrideW != null ? overrideW : getWidth();
    }

    /** Height used for EditGUI box + anchor math: override when present, else computed. */
    public int getRenderHeight() {
        return overrideH != null ? overrideH : getHeight();
    }

    public void loadSettings() {}

    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 4.0f;

    /**
     * Renders the module, scaling its whole output (text + box) to fill the
     * override size when one is set. Modules draw at their natural size; this
     * wrapper applies the transform so resize affects content, not just the box.
     */
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

        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - natW : x;
        int ry = y;

        g.pose().pushMatrix();
        g.pose().translate(rx, ry);
        g.pose().scale(s);
        g.pose().translate(-rx, -ry);
        render(g, mc, d);
        g.pose().popMatrix();
    }

    @Override
    public String getCategory() { return "HUD"; }
}
