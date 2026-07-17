package com.crest.client.core;

import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: CPS (clicks-per-second) HUD with a small rolling bar graph for left
 * and right mouse buttons. Click events arrive via MouseInputMixin -> CpsTracker.
 */
public class CpsModule extends HudModule {
    private static final int PAD = 2;
    private static final int LINE_H = 10;
    private static final int GRAPH_W = 60;
    private static final int GRAPH_H = 14;

    private final java.util.Deque<Integer> leftHist = new java.util.ArrayDeque<>();
    private final java.util.Deque<Integer> rightHist = new java.util.ArrayDeque<>();
    private int sampleTick;

    public CpsModule() {
        super(-1, 60);
    }

    @Override public String getId() { return "cps"; }
    @Override public String getName() { return "CPS Counter"; }
    @Override public String getDescription() { return "Shows left/right clicks-per-second with a rolling graph."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of();
    }

    @Override
    public int getWidth() {
        return Math.max(GRAPH_W + PAD * 2, Minecraft.getInstance().font.width("L 0  R 0") + PAD * 2);
    }

    @Override
    public int getHeight() {
        return LINE_H + GRAPH_H + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        CpsTracker.tick();

        sampleTick++;
        if (sampleTick >= 5) {
            push(leftHist, CpsTracker.leftCps());
            push(rightHist, CpsTracker.rightCps());
            sampleTick = 0;
        }

        int boxW = getWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        HudBackground.draw(g, rx, ry, boxW, boxH);

        int textY = ry + PAD;
        String txt = "L " + CpsTracker.leftCps() + "  R " + CpsTracker.rightCps();
        g.text(mc.font, Component.literal(txt), rx + PAD, textY, 0xFFFFFFFF);

        int gy = textY + LINE_H;
        drawGraph(g, rx + PAD, gy, leftHist, 0xFF55FF55);
        drawGraph(g, rx + PAD, gy, rightHist, 0xFF5555FF);
    }

    private void drawGraph(GuiGraphicsExtractor g, int x, int y, java.util.Deque<Integer> hist, int color) {
        int max = 20;
        for (int v : hist) max = Math.max(max, v);
        int i = 0;
        int n = hist.size();
        for (int v : hist) {
            int bh = v * GRAPH_H / max;
            int bx = x + (GRAPH_W * i / Math.max(1, n));
            int bw = Math.max(1, GRAPH_W / Math.max(1, n));
            g.fill(bx, y + GRAPH_H - bh, bx + bw - 1, y + GRAPH_H, color);
            i++;
        }
    }

    private static void push(java.util.Deque<Integer> q, int v) {
        q.addLast(v);
        while (q.size() > 24) q.removeFirst();
    }
}
