package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.crest.client.core.setting.Setting;
import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Perf HUD — measure-first instrumentation so optimizations are
 * verifiable rather than guessed. Shows FPS, real frame time (from the ACFB
 * EMA), capture state, Block LOD active tier, and render distance. All values
 * are read-only on the render thread and the text is cached, rebuilt only when
 * a displayed value changes.
 */
public class PerfModule extends HudModule {
    public PerfModule() {
        super(-1, 4);
    }

    @Override public String getId() { return "perf"; }
    @Override public String getName() { return "Perf HUD"; }
    @Override public String getDescription() { return "Live FPS, frame time, capture state, Block LOD tier, and render distance."; }
    @Override public String getCategory() { return "HUD"; }
    @Override public boolean isEnabled() { return true; }

    private static final class Line {
        final String text;
        final int color;
        final Component comp;
        final int width;
        Line(Minecraft mc, String text, int color) {
            this.text = text;
            this.color = color;
            this.comp = Component.literal(text);
            this.width = mc.font.width(text);
        }
    }

    private List<Line> cachedLines;
    private String cacheKey = "";

    @Override
    public java.util.List<Setting<?>> getSettings() {
        return java.util.List.of();
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        List<Line> lines = buildLines(mc);
        int w = 0;
        for (Line l : lines) w = Math.max(w, l.width);
        return w + 4;
    }

    @Override
    public int getHeight() {
        Minecraft mc = Minecraft.getInstance();
        return mc.font.lineHeight * 5 + 4 + 8;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        List<Line> lines = buildLines(mc);
        int lh = mc.font.lineHeight;
        int pad = 2;
        int boxW = getRenderWidth();
        int boxH = lh * lines.size() + 4 + (lines.size() - 1) * 2 + pad;
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW - 4 : x;
        int ry = y;

        HudBackground.draw(g, rx, ry, boxW, boxH);
        int cy = ry + pad;
        for (Line l : lines) {
            g.text(mc.font, l.comp, rx + 2, cy, l.color);
            cy += lh + 2;
        }
    }

    private List<Line> buildLines(Minecraft mc) {
        int fps = mc.getFps();
        double frameMs = FrameBudget.avgFrameMs();
        boolean overBudget = frameMs > (1000.0 / Math.max(1, mc.getWindow().getRefreshRate()));
        int frameColor = overBudget ? 0xFFFF5555 : 0xFF55FF55;

        String cap = FrameBudget.lastCaptureDecision();
        int capColor = "RUN".equals(cap) ? 0xFF55FF55 : 0xFFFFFF55;

        String lod;
        int lodColor;
        if (BlockLodModule.active()) {
            int tier = currentLodTier(mc);
            lod = "ON (tier " + tier + ")";
            lodColor = tier == 0 ? 0xFF55FF55 : 0xFFFFFF55;
        } else {
            lod = "OFF";
            lodColor = 0xFF888888;
        }

        int rd = mc.options.renderDistance().get();

        String key = fps + "|" + (int) Math.round(frameMs) + "|" + cap + "|" + lod + "|" + rd;
        if (cachedLines != null && key.equals(cacheKey)) return cachedLines;

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(mc, fps + " FPS", fpsColor(fps)));
        lines.add(new Line(mc, String.format("%.1f ms", frameMs), frameColor));
        lines.add(new Line(mc, "Capture: " + cap, capColor));
        lines.add(new Line(mc, "LOD: " + lod, lodColor));
        lines.add(new Line(mc, "RD: " + rd + " sections", 0xFFAAAAAA));
        cachedLines = lines;
        cacheKey = key;
        return lines;
    }

    private static int currentLodTier(Minecraft mc) {
        if (mc.player == null) return 0;
        double cx = FrameBudget.cameraX();
        double cz = FrameBudget.cameraZ();
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        double dx = Math.abs(cx - px);
        double dz = Math.abs(cz - pz);
        double dist = Math.max(dx, dz) / 16.0;
        int t3 = BlockLodModule.tier3();
        int t2 = BlockLodModule.tier2();
        int t1 = BlockLodModule.tier1();
        if (dist >= t3) return 3;
        if (dist >= t2) return 2;
        if (dist >= t1) return 1;
        return 0;
    }

    private static int fpsColor(int fps) {
        if (fps >= 60) return 0xFF55FF55;
        if (fps >= 30) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    @Override
    public void loadSettings() {
        // clear caches so a reload after toggle/position change rebuilds cleanly
        cachedLines = null;
        cacheKey = "";
    }
}
