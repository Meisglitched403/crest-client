package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Detects what the player is currently targeting for quick waypoint creation. */
public final class WaypointTargetingService {
    private WaypointTargetingService() {}

    public static BlockPos targetedBlock(Minecraft mc) {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) mc.hitResult).getBlockPos();
        }
        return mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;
    }

    public static boolean hasTarget(Minecraft mc) {
        return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
    }
}
