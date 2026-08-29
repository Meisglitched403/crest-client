package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/** Resolves block-based icon ids for waypoints placed on blocks. */
public final class WorldBlockIconService {
    private WorldBlockIconService() {}

    public static String iconIdForWorldBlock(Minecraft mc, BlockPos pos) {
        if (mc.level == null) return "block:grass";
        Block b = mc.level.getBlockState(pos).getBlock();
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).toString();
        return "block:" + name;
    }

    public static String iconIdForWorldBlockIgnoringAir(Minecraft mc, BlockPos pos) {
        return iconIdForWorldBlock(mc, pos);
    }

    public static boolean resolvePendingBlockIconImports(WaypointManager mgr) {
        boolean changed = false;
        for (Waypoint w : mgr.getWaypoints()) {
            if (w.hasPendingBlockIconResolution()) {
                w.clearPendingBlockIconResolution();
                changed = true;
            }
        }
        return changed;
    }

    public static boolean refreshRememberedDefaultBlockIcons(WaypointManager mgr, ModConfig cfg) {
        return false;
    }
}
