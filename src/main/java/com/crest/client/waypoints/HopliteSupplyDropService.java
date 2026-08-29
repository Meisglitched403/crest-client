package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Hoplite supply-drop waypoint creation. */
public final class HopliteSupplyDropService {
    public static final int SUPPLY_COLOR = 0xFFAA00;
    public static final String WAYPOINT_ICON_ID = "block:ender_chest";

    private HopliteSupplyDropService() {}

    public static void maybeCreate(int x, int z, WaypointManager mgr, ModConfig cfg, String worldId, Runnable onChange) {
        if (!cfg.hopliteEnabled || !cfg.hopliteAutoSupplyDropWaypoints) return;
        int n = nextNumber(mgr, cfg);
        String name = "Supply " + n;
        mgr.createWaypoint(new BlockPos(x, 64, z), name, SUPPLY_COLOR, WAYPOINT_ICON_ID, false, Waypoint.Type.SUPPLY_DROP);
        if (onChange != null) onChange.run();
    }

    public static int nextNumber(WaypointManager mgr, ModConfig cfg) {
        int max = 0;
        for (Waypoint w : mgr.getWaypoints()) if (w.getType() == Waypoint.Type.SUPPLY_DROP) max++;
        return max + 1;
    }
}
