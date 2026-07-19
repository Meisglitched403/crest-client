package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ponytail: Scoreboard HUD. Renders the in-game sidebar objective itself so the
 * text scales with the EditGUI box (move + resize). The vanilla sidebar is
 * cancelled by ScoreboardMixin while this module is enabled to avoid double-draw.
 */
public class ScoreboardModule extends HudModule {
    private static final int PAD = 3;
    private static final int LINE_H = 11;

    public ScoreboardModule() {
        super(-1, 4);
    }

    @Override public String getId() { return "scoreboard"; }
    @Override public String getName() { return "Scoreboard"; }
    @Override public String getDescription() { return "The in-game scoreboard sidebar — position & size it from the EditGUI."; }
    @Override public String getCategory() { return "HUD"; }
    @Override public boolean isEnabled() { return true; }

    private Objective currentObjective() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Scoreboard sb = mc.level.getScoreboard();
        return sb.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    private static class Row {
        final Component name;
        final Component score;
        final int value;
        Row(Component name, Component score, int value) { this.name = name; this.score = score; this.value = value; }
    }

    private List<Row> buildRows(Objective obj) {
        List<Row> rows = new ArrayList<>();
        for (PlayerScoreEntry e : obj.getScoreboard().listPlayerScores(obj)) {
            if (e.isHidden()) continue;
            rows.add(new Row(e.display(), e.formatValue(obj.numberFormatOrDefault(
                net.minecraft.network.chat.numbers.StyledFormat.SIDEBAR_DEFAULT)), e.value()));
        }
        rows.sort(Comparator.<Row>comparingInt(r -> r.value).reversed());
        return rows;
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        Objective obj = currentObjective();
        if (obj == null) return mc.font.width("Scoreboard") + PAD * 2;
        return naturalWidth(mc.font, obj) + PAD * 2;
    }

    @Override
    public int getHeight() {
        Objective obj = currentObjective();
        if (obj == null) return LINE_H + PAD * 2;
        int lines = 1 + buildRows(obj).size();
        return lines * LINE_H + PAD * 2;
    }

    private int naturalWidth(Font font, Objective obj) {
        int w = font.width(obj.getDisplayName());
        for (Row r : buildRows(obj)) {
            w = Math.max(w, font.width(r.name) + font.width(" ") + font.width(r.score));
        }
        return w;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Objective obj = currentObjective();
        if (obj == null) return;

        int boxW = getWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;

        Font font = mc.font;
        List<Row> rows = buildRows(obj);

        HudBackground.draw(g, rx, ry, boxW, boxH);

        int tx = rx + PAD;
        int ty = ry + PAD;
        g.text(font, obj.getDisplayName(), tx, ty, 0xFFFFFFFF);
        ty += LINE_H;
        for (Row r : rows) {
            g.text(font, r.name, tx, ty, 0xFFFFFFFF);
            int scoreW = font.width(r.score);
            g.text(font, r.score, rx + boxW - PAD - scoreW, ty, 0xFFAAAAAA);
            ty += LINE_H;
        }
    }
}
