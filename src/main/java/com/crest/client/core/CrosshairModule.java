package com.crest.client.core;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class CrosshairModule implements CrestModule {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath("crest-client", "crosshair");

    public static int style = 0;       // 0=dot, 1=cross, 2=circle
    public static int color = 0xFFFFFFFF;
    public static int size = 4;        // arm length or radius
    public static int gap = 2;         // center gap for cross style
    public static int thickness = 2;   // line width for cross style

    @Override
    public String getId() { return "crosshair"; }
    @Override
    public String getName() { return "Custom Crosshair"; }
    @Override
    public String getDescription() { return "Dot / Cross / Circle crosshair with color and size control"; }

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public void onInitialize() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CROSSHAIR,
            LAYER,
            this::render
        );
    }

    private void render(GuiGraphicsExtractor g, DeltaTracker d) {
        if (!CrestModules.isEnabled("crosshair")) return;
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        switch (style) {
            case 0 -> renderDot(g, cx, cy);
            case 1 -> renderCross(g, cx, cy);
            case 2 -> renderCircle(g, cx, cy);
        }
    }

    private void renderDot(GuiGraphicsExtractor g, int cx, int cy) {
        int r = Math.max(size / 2, 1);
        g.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, color);
    }

    private void renderCross(GuiGraphicsExtractor g, int cx, int cy) {
        int half = thickness / 2;
        // top
        g.fill(cx - half, cy - gap - size, cx - half + thickness, cy - gap, color);
        // bottom
        g.fill(cx - half, cy + gap, cx - half + thickness, cy + gap + size, color);
        // left
        g.fill(cx - gap - size, cy - half, cx - gap, cy - half + thickness, color);
        // right
        g.fill(cx + gap, cy - half, cx + gap + size, cy - half + thickness, color);
    }

    private void renderCircle(GuiGraphicsExtractor g, int cx, int cy) {
        int r = Math.max(size, 1);
        for (int row = -r; row <= r; row++) {
            int halfW = (int) Math.sqrt(r * r - row * row);
            g.fill(cx - halfW, cy + row, cx + halfW + 1, cy + row + 1, color);
        }
    }
}
