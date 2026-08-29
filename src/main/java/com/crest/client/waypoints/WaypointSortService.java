package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Comparator;
import java.util.List;

/** Orders waypoints for display (name / distance / date). */
public final class WaypointSortService {
    private WaypointSortService() {}

    public static void sort(List<Waypoint> list, ModConfig.WaypointMenuSortOrder order, Minecraft mc) {
        switch (order) {
            case DISTANCE -> {
                if (mc.player != null) {
                    final double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
                    list.sort(Comparator.comparingDouble(w -> {
                        BlockPos p = w.getPosition();
                        if (p == null) return Double.MAX_VALUE;
                        return p.distToCenterSqr(px, py, pz);
                    }));
                }
            }
            case NAME -> list.sort(Comparator.comparing(w -> w.getName() == null ? "" : w.getName().toLowerCase()));
            case DATE -> { /* creation order kept */ }
        }
    }
}
