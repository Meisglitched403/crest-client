package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

/** Guards against connecting an unreasonable number of waypoints into one clump. */
public final class WaypointConnectionLimit {
    public static final long MAX_CONNECTED_WAYPOINTS = 4096;

    private WaypointConnectionLimit() {}

    public static boolean withinLimit(BlockPos a, BlockPos b) {
        long dx = Math.abs((long) a.getX() - b.getX()) + 1;
        long dz = Math.abs((long) a.getZ() - b.getZ()) + 1;
        long dy = Math.abs((long) a.getY() - b.getY()) + 1;
        return dx * dy * dz <= MAX_CONNECTED_WAYPOINTS;
    }

    public static long connectedWaypointCount(BlockPos a, BlockPos b) {
        long dx = Math.abs((long) a.getX() - b.getX()) + 1;
        long dz = Math.abs((long) a.getZ() - b.getZ()) + 1;
        long dy = Math.abs((long) a.getY() - b.getY()) + 1;
        return dx * dy * dz;
    }
}
