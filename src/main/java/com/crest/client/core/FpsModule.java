package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class FpsModule extends HudModule {
    public FpsModule() {
        super(-1, 4);
    }

    @Override
    public String getId() { return "fps"; }
    @Override
    public String getName() { return "FPS Display"; }
    @Override
    public String getDescription() { return "Shows current FPS with color-coded value"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public int getWidth() {
        String t = Minecraft.getInstance().getFps() + " FPS";
        return Minecraft.getInstance().font.width(t) + 4;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + 4;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        int fps = mc.getFps();
        String text = fps + " FPS";
        int color = getFpsColor(fps);
        int w = mc.font.width(text);
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - w - 4 - 2 : x;
        int ry = y;

        g.fill(rx, ry, rx + w + 4, ry + mc.font.lineHeight + 4, 0x66000000);
        g.text(mc.font, Component.literal(text), rx + 2, ry + 2, color);
    }

    private static int getFpsColor(int fps) {
        if (fps >= 60) return 0xFF55FF55;
        if (fps >= 30) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
}
