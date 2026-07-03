package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class FpsModule implements CrestModule, RenderableModule {
    @Override
    public String getId() { return "fps"; }
    @Override
    public String getName() { return "FPS Display"; }
    @Override
    public String getDescription() { return "Shows current FPS in top-right corner"; }
    @Override
    public String getCategory() { return "HUD"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public void onInitialize() {}

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        int fps = mc.getFps();
        String text = fps + " FPS";
        int color = getFpsColor(fps);
        int width = mc.font.width(text);
        int x = mc.getWindow().getGuiScaledWidth() - width - 4;
        int y = 4;

        g.fill(x - 2, y - 2, x + width + 2, y + mc.font.lineHeight + 2, 0x66000000);
        g.text(mc.font, Component.literal(text), x, y, color);
    }

    private static int getFpsColor(int fps) {
        if (fps >= 60) return 0x55FF55;
        if (fps >= 30) return 0xFFFF55;
        return 0xFF5555;
    }
}
