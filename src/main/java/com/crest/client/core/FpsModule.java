package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.crest.client.core.setting.Setting;

public class FpsModule extends HudModule {
    private int cachedFps = -1;
    private String cachedText;
    private int cachedWidth;
    private Component cachedComp;

    // ponytail: min/max/avg over a rolling window so the panel shows real FPS behavior.
    private int minFps = Integer.MAX_VALUE, maxFps = 0;
    private long sumFps;
    private int samples;
    private int avgFps;
    private int statTick;

    public FpsModule() {
        super(-1, 4);
    }

    @Override public String getId() { return "fps"; }
    @Override public String getName() { return "FPS Display"; }
    @Override public String getDescription() { return "Shows current FPS with min/avg/max over a rolling window."; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public java.util.List<Setting<?>> getSettings() {
        return java.util.List.of();
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        String probe = mc.getFps() + " FPS  min 0  avg 0  max 0";
        return mc.font.width(probe) + 4;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + 4;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        int fps = mc.getFps();

        // update rolling stats ~ once per second of ticks
        statTick++;
        if (statTick >= 20) {
            minFps = Math.min(minFps, fps);
            maxFps = Math.max(maxFps, fps);
            sumFps += fps; samples++;
            avgFps = (int) (sumFps / Math.max(1, samples));
            statTick = 0;
        }

        if (fps != cachedFps || cachedText == null) {
            cachedFps = fps;
            cachedText = fps + " FPS  min " + minFps + "  avg " + avgFps + "  max " + maxFps;
            cachedWidth = mc.font.width(cachedText);
            cachedComp = Component.literal(cachedText);
        }
        int color = getFpsColor(fps);
        int w = cachedWidth;
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - w - 4 : x;
        int ry = y;

        HudBackground.draw(g, rx, ry, w + 4, mc.font.lineHeight + 4);
        g.text(mc.font, cachedComp, rx + 2, ry + 2, color);
    }

    private static int getFpsColor(int fps) {
        if (fps >= 60) return 0xFF55FF55;
        if (fps >= 30) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
}
