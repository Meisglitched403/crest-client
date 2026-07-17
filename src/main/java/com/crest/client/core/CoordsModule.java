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

public class CoordsModule extends HudModule {
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 2;

    private final HudBackground bg = new HudBackground();
    private final BooleanSetting showXyz = new BooleanSetting("Show XYZ", true);
    private final BooleanSetting showFacing = new BooleanSetting("Show Facing", true);
    private final BooleanSetting showBiome = new BooleanSetting("Show Biome", true);

    public CoordsModule() {
        super(4, 4);
    }

    @Override public String getId() { return "coords"; }
    @Override public String getName() { return "Coordinates"; }
    @Override public String getDescription() { return "Shows XYZ, direction, and biome (each toggleable)."; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        List<Setting<?>> s = new ArrayList<>(bg.settings());
        s.add(showXyz); s.add(showFacing); s.add(showBiome);
        return s;
    }

    @Override
    public int getWidth() {
        int w = 0;
        Minecraft mc = Minecraft.getInstance();
        if (showXyz.get()) w = Math.max(w, mc.font.width("XYZ: -0000 / -0000 / -0000"));
        if (showFacing.get()) w = Math.max(w, mc.font.width("Facing: South-West"));
        if (showBiome.get()) w = Math.max(w, mc.font.width("Biome: minecraft:unknown_biome"));
        return w + PADDING * 2;
    }

    @Override
    public int getHeight() {
        int lines = 0;
        if (showXyz.get()) lines++;
        if (showFacing.get()) lines++;
        if (showBiome.get()) lines++;
        return lines > 0 ? lines * LINE_HEIGHT + PADDING * 2 : LINE_HEIGHT + PADDING * 2;
    }

    private String biomeCache = "Unknown";
    private int biomeCacheTick;

    private int lastX = Integer.MAX_VALUE, lastY = Integer.MAX_VALUE, lastZ = Integer.MAX_VALUE;
    private String lastDir;
    private Component xyzComp, dirComp, biomeComp;

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

        if (pos.getX() != lastX || pos.getY() != lastY || pos.getZ() != lastZ || !dir.equals(lastDir)) {
            lastX = pos.getX(); lastY = pos.getY(); lastZ = pos.getZ(); lastDir = dir;
            xyzComp = Component.literal("XYZ: " + pos.getX() + " / " + pos.getY() + " / " + pos.getZ());
            dirComp = Component.literal("Facing: " + dir);
            biomeComp = Component.literal("Biome: " + biomeCache);
        }

        int boxW = getWidth();
        int boxH = getHeight();
        int rx = x < 0 ? mc.getWindow().getGuiScaledWidth() - boxW : x;
        int ry = y;
        bg.draw(g, rx, ry, boxW, boxH);

        int cy = ry + PADDING;
        if (showXyz.get()) { g.text(mc.font, xyzComp, rx + PADDING, cy, 0xFFFFFFFF); cy += LINE_HEIGHT; }
        if (showFacing.get()) { g.text(mc.font, dirComp, rx + PADDING, cy, 0xFFFFFFFF); cy += LINE_HEIGHT; }
        if (showBiome.get()) { g.text(mc.font, biomeComp, rx + PADDING, cy, 0xFFFFFFFF); cy += LINE_HEIGHT; }
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
