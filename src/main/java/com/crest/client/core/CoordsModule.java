package com.crest.client.core;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;


public class CoordsModule extends HudModule {
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 2;
    private static final int BG_COLOR = 0x66000000;

    public CoordsModule() {
        super(4, 4);
    }

    @Override
    public String getId() { return "coords"; }
    @Override
    public String getName() { return "Coordinates"; }
    @Override
    public String getDescription() { return "Shows XYZ, direction, and biome"; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public int getWidth() {
        return 110;
    }

    @Override
    public int getHeight() {
        return LINE_HEIGHT * 3 + PADDING * 2;
    }

    private String biomeCache = "Unknown";
    private int biomeCacheTick;

    @Override
    public void render(GuiGraphicsExtractor g, Minecraft mc, DeltaTracker d) {
        Player player = mc.player;
        if (player == null) return;

        BlockPos pos = player.blockPosition();
        String dir = getFacing(player);

        int tick = mc.gui.getGuiTicks();
        if (tick - biomeCacheTick >= 10) {
            biomeCache = getBiomeName(player);
            biomeCacheTick = tick;
        }

        Component xyz = Component.literal(
            String.format("XYZ: %d / %d / %d", pos.getX(), pos.getY(), pos.getZ()));
        Component dirComp = Component.literal("Facing: " + dir);
        Component biomeComp = Component.literal("Biome: " + biomeCache);

        g.fill(x, y, x + getWidth(), y + getHeight(), BG_COLOR);
        g.text(mc.font, xyz, x + PADDING, y + PADDING, 0xFFFFFFFF);
        g.text(mc.font, dirComp, x + PADDING, y + PADDING + LINE_HEIGHT, 0xFFFFFFFF);
        g.text(mc.font, biomeComp, x + PADDING, y + PADDING + LINE_HEIGHT * 2, 0xFFFFFFFF);
    }

    private static String getFacing(Player player) {
        float yaw = player.getYRot();
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) return "South";
        if (yaw < 135) return "West";
        if (yaw < 225) return "North";
        return "East";
    }

    private static String getBiomeName(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        var holder = level.getBiome(pos);
        var key = holder.unwrapKey();
        if (key.isPresent()) {
            String s = key.get().toString();
            int idx = s.lastIndexOf('/');
            return idx >= 0 && idx + 1 < s.length() ? s.substring(idx + 1).replace("}", "") : s;
        }
        return "Unknown";
    }
}
