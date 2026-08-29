package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

/** Projects waypoints between Overworld and Nether (1:8). */
public final class WaypointDimensionProjectionService {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";

    private WaypointDimensionProjectionService() {}

    public static boolean canProject(String from, String to) {
        return (from.equals(OVERWORLD) && to.equals(NETHER)) || (from.equals(NETHER) && to.equals(OVERWORLD));
    }

    public static BlockPos projectPosition(BlockPos pos, String from, String to) {
        if (from.equals(OVERWORLD) && to.equals(NETHER)) {
            return new BlockPos(Math.floorDiv(pos.getX(), 8), pos.getY(), Math.floorDiv(pos.getZ(), 8));
        } else {
            return new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
        }
    }

    public static WaypointManager projectManager(WaypointManager src, String from, String to) {
        WaypointManager out = new WaypointManager();
        for (Waypoint w : src.getWaypoints()) {
            if (w.getPosition() == null) continue;
            BlockPos p = projectPosition(w.getPosition(), from, to);
            Waypoint c = out.createWaypoint(p, w.getName(), w.getColor(), w.getIconId(), w.isDeathWaypoint(), w.getType());
        }
        return out;
    }
}
