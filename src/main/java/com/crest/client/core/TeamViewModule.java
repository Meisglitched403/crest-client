package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: TeamView HUD. Lists scoreboard teams and their members, highlighting
 * the team(s) the local player belongs to. Read-only poll of the client Scoreboard.
 */
public class TeamViewModule extends HudModule {
    private static final int PAD = 2;
    private static final int LINE_H = 10;

    private final HudBackground bg = new HudBackground();
    private final BooleanSetting showMembers = new BooleanSetting("Show Members", true);
    private final BooleanSetting onlyMyTeam = new BooleanSetting("Only My Team", false);

    public TeamViewModule() {
        super(-1, 260);
    }

    @Override public String getId() { return "teamview"; }
    @Override public String getName() { return "Team View"; }
    @Override public String getDescription() { return "Lists scoreboard teams and members, highlighting your team."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>(bg.settings());
        s.add(showMembers);
        s.add(onlyMyTeam);
        return s;
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.font.width("Teams:");
        return w + PAD * 2;
    }

    @Override
    public int getHeight() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Scoreboard sb = mc.level != null ? mc.level.getScoreboard() : null;
        if (sb == null || player == null) return LINE_H + PAD * 2;

        String myTeam = currentTeamName(sb, player.getName().getString());
        int lines = 1;
        for (PlayerTeam team : sb.getPlayerTeams()) {
            if (onlyMyTeam.get() && !team.getName().equals(myTeam)) continue;
            lines++;
            if (showMembers.get()) lines += team.getPlayers().size();
        }
        if (lines == 1) lines = 2;
        return lines * LINE_H + PAD * 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        LocalPlayer player = mc.player;
        Scoreboard sb = mc.level != null ? mc.level.getScoreboard() : null;
        if (sb == null || player == null) return;

        String myName = player.getName().getString();
        String myTeam = currentTeamName(sb, myName);

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add("Teams:");
        colors.add(0xFFFFFFFF);

        for (PlayerTeam team : sb.getPlayerTeams()) {
            if (onlyMyTeam.get() && !team.getName().equals(myTeam)) continue;
            boolean mine = team.getName().equals(myTeam);
            int col = teamColor(team);
            lines.add((mine ? "* " : "  ") + team.getName());
            colors.add(mine ? 0xFF55FF55 : col);
            if (showMembers.get()) {
                for (String member : team.getPlayers()) {
                    lines.add("    " + member);
                    colors.add(0xFFCCCCCC);
                }
            }
        }

        int boxW = Math.max(getWidth(), longest(lines, mc));
        int boxH = lines.size() * LINE_H + PAD * 2;
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        bg.draw(g, rx, ry, boxW, boxH);

        int cy = ry + PAD;
        for (int i = 0; i < lines.size(); i++) {
            g.text(mc.font, Component.literal(lines.get(i)), rx + PAD, cy, colors.get(i));
            cy += LINE_H;
        }
    }

    private static String currentTeamName(Scoreboard sb, String playerName) {
        for (PlayerTeam team : sb.getPlayerTeams()) {
            if (team.getPlayers().contains(playerName)) return team.getName();
        }
        return null;
    }

    private static int teamColor(PlayerTeam team) {
        ChatFormatting fmt = team.getColor();
        Integer c = fmt != null ? fmt.getColor() : null;
        if (c == null) return 0xFFAAAAAA;
        return 0xFF000000 | c;
    }

    private static int longest(List<String> lines, Minecraft mc) {
        int w = 0;
        for (String l : lines) w = Math.max(w, mc.font.width(l));
        return w + PAD * 2;
    }
}
