package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class StreamerIndicator extends HudModule {
    public StreamerIndicator() {
        super(-1, 24);
    }

    @Override
    public String getId() { return "streamer_indicator"; }
    @Override
    public String getName() { return "Streamer Indicator"; }
    @Override
    public String getDescription() { return "Shows LIVE indicator when streaming"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public int getWidth() {
        return Minecraft.getInstance().font.width("LIVE 00:00") + 12;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + 4;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (!Streamer.isStreaming()) return;

        long elapsed = (System.currentTimeMillis() - Streamer.getStartTime()) / 1000;
        String text = String.format("LIVE %02d:%02d", elapsed / 60, elapsed % 60);
        int w = mc.font.width(text);
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - w - 12 - 2 : x;
        int ry = y;

        g.fill(rx, ry, rx + w + 12, ry + mc.font.lineHeight + 4, 0x66000000);
        g.text(mc.font, Component.literal(text), rx + 10, ry + 2, 0xFFFFFFFF);

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int dSize = 6;
            int dx = rx + 2;
            int dy = ry + (mc.font.lineHeight + 4 - dSize) / 2;
            g.fill(dx, dy, dx + dSize, dy + dSize, 0xFF33FF33);
        }
    }
}
