package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class RecorderIndicator extends HudModule {
    public RecorderIndicator() {
        super(-1, 4);
    }

    @Override
    public String getId() { return "recorder_indicator"; }
    @Override
    public String getName() { return "Recorder Indicator"; }
    @Override
    public String getDescription() { return "Shows red dot and timer when recording"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public int getWidth() {
        return Minecraft.getInstance().font.width("REC 00:00") + 12;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight + 4;
    }

    // ponytail: cache the timer text + Component; only rebuild once per second.
    private long cachedElapsed = -1;
    private String cachedText;
    private Component cachedComp;

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        if (!Recorder.isRecording()) return;

        long elapsed = (System.currentTimeMillis() - Recorder.getStartTime()) / 1000;
        if (elapsed != cachedElapsed || cachedComp == null) {
            cachedElapsed = elapsed;
            cachedText = String.format("REC %02d:%02d", elapsed / 60, elapsed % 60);
            cachedComp = Component.literal(cachedText);
        }
        int w = mc.font.width(cachedText);
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - w - 12 - 2 : x;
        int ry = y;

        HudBackground.draw(g, rx, ry, w + 12, mc.font.lineHeight + 4);
        g.text(mc.font, cachedComp, rx + 10, ry + 2, 0xFFFFFFFF);

        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int dSize = 6;
            int dx = rx + 2;
            int dy = ry + (mc.font.lineHeight + 4 - dSize) / 2;
            g.fill(dx, dy, dx + dSize, dy + dSize, 0xFFFF3333);
        }
    }
}
