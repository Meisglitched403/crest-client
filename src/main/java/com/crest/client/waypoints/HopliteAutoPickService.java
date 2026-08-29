package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Hoplite auto-pick waypoint creation. */
public final class HopliteAutoPickService {
    public static final int WAYPOINT_COLOR = 0x55FF55;
    public static final String WAYPOINT_ICON_ID = "block:slime_block";

    private HopliteAutoPickService() {}

    public static void handleUse(Player player, BlockHitResult hit, WaypointManager mgr, ModConfig cfg, Runnable onChange) {
        if (!cfg.hopliteEnabled || !cfg.hopliteAutoPickCreatesWaypoint) return;
        BlockPos p = hit.getBlockPos();
        mgr.createWaypoint(p, "Pick", WAYPOINT_COLOR, WAYPOINT_ICON_ID, false, Waypoint.Type.AUTO_PICK);
        if (onChange != null) onChange.run();
    }

    public static boolean isWaypoint(Waypoint wp) { return wp.getType() == Waypoint.Type.AUTO_PICK; }
}
