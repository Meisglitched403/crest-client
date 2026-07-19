package com.crest.client.core;

import com.crest.client.core.setting.BooleanSetting;
import com.crest.client.core.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * ponytail: Standalone Biome HUD. Split out of Coordinates so the biome readout
 * is independently toggleable and themable. Shows the biome name, optionally the
 * raw registry id, and (per the user's "show only what I want" request) the
 * player's current coordinates as a secondary line.
 */
public class BiomeModule extends HudModule {
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 2;

    private final BooleanSetting showName = new BooleanSetting("Show Biome Name", true);
    private final BooleanSetting showId = new BooleanSetting("Show Registry ID", false);
    private final BooleanSetting showCoords = new BooleanSetting("Show Coordinates", false);

    public BiomeModule() {
        super(4, 36);
    }

    @Override public String getId() { return "biome"; }
    @Override public String getName() { return "Biome"; }
    @Override public String getDescription() { return "Shows the current biome (name/id/coords, each toggleable)."; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>();
        s.add(showName); s.add(showId); s.add(showCoords);
        return s;
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        int w = 0;
        if (showName.get()) w = Math.max(w, mc.font.width("Biome: minecraft:unknown_biome"));
        if (showId.get()) w = Math.max(w, mc.font.width("ID: minecraft:unknown_biome"));
        if (showCoords.get()) w = Math.max(w, mc.font.width("XYZ: -0000 / -0000 / -0000"));
        return w + PADDING * 2;
    }

    @Override
    public int getHeight() {
        int lines = 0;
        if (showName.get()) lines++;
        if (showId.get()) lines++;
        if (showCoords.get()) lines++;
        return lines > 0 ? lines * LINE_HEIGHT + PADDING * 2 : LINE_HEIGHT + PADDING * 2;
    }

    private String nameCache = "Unknown", idCache = "Unknown";
    private int cacheTick;
    private int lastX = Integer.MAX_VALUE, lastY = Integer.MAX_VALUE, lastZ = Integer.MAX_VALUE;
    private Component coordComp;

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Player player = mc.player;
        if (player == null) return;

        int tick = mc.gui.getGuiTicks();
        if (tick - cacheTick >= 10) {
            nameCache = getName(player);
            idCache = getId(player);
            cacheTick = tick;
        }

        BlockPos pos = player.blockPosition();
        if (pos.getX() != lastX || pos.getY() != lastY || pos.getZ() != lastZ) {
            lastX = pos.getX(); lastY = pos.getY(); lastZ = pos.getZ();
            coordComp = Component.literal("XYZ: " + pos.getX() + " / " + pos.getY() + " / " + pos.getZ());
        }

        int boxW = getRenderWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        HudBackground.draw(g, rx, ry, boxW, boxH);

        int cy = ry + PADDING;
        if (showName.get()) { g.text(mc.font, Component.literal("Biome: " + nameCache), rx + PADDING, cy, 0xFF88CCFF); cy += LINE_HEIGHT; }
        if (showId.get()) { g.text(mc.font, Component.literal("ID: " + idCache), rx + PADDING, cy, 0xFFAAAAAA); cy += LINE_HEIGHT; }
        if (showCoords.get() && coordComp != null) { g.text(mc.font, coordComp, rx + PADDING, cy, 0xFFFFFFFF); cy += LINE_HEIGHT; }
    }

    private static String getName(Player player) {
        var key = player.level().getBiome(player.blockPosition()).unwrapKey();
        if (key.isPresent()) {
            String s = key.get().toString();
            int idx = s.lastIndexOf('/');
            return idx >= 0 && idx + 1 < s.length() ? s.substring(idx + 1).replace("}", "") : s;
        }
        return "Unknown";
    }

    private static String getId(Player player) {
        var key = player.level().getBiome(player.blockPosition()).unwrapKey();
        return key.isPresent() ? key.get().toString().replace("}", "") : "Unknown";
    }
}
