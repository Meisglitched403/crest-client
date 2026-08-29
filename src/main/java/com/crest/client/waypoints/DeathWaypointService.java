package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Auto death-waypoint creation with configurable limits. */
public final class DeathWaypointService {
    private DeathWaypointService() {}

    public static void maybeCreateOnDeath(Minecraft mc, WaypointManager mgr, ModConfig cfg) {
        if (mc.player == null || mc.level == null) return;
        if (!cfg.deathWaypoints) return;
        BlockPos p = mc.player.blockPosition();
        Waypoint wp = mgr.createWaypoint(p, "LastDeath", 0xFF5555, "block:skull", true, Waypoint.Type.DEATH);
        wp.setDeathWaypoint(true);
        enforceLimit(mgr, cfg);
    }

    public static void enforceLimit(WaypointManager mgr, ModConfig cfg) {
        if (cfg.deathWaypointLimit <= 0) {
            List<Waypoint> deaths = new ArrayList<>();
            for (Waypoint w : mgr.getWaypoints()) if (w.isDeathWaypoint()) deaths.add(w);
            for (Waypoint w : deaths) mgr.removeWaypoint(w);
            return;
        }
        List<Waypoint> deaths = new ArrayList<>();
        for (Waypoint w : mgr.getWaypoints()) if (w.isDeathWaypoint()) deaths.add(w);
        // newest first
        deaths.sort((a, b) -> Long.compare(b.getPosition() != null ? 0 : 0, 0));
        int excess = deaths.size() - cfg.deathWaypointLimit;
        for (int i = 0; i < excess; i++) mgr.removeWaypoint(deaths.get(deaths.size() - 1 - i));
        // rename
        for (int i = 0; i < deaths.size() && i < cfg.deathWaypointLimit; i++) {
            deaths.get(i).setName(i == 0 ? "LastDeath" : "Death" + (i + 1));
        }
    }
}
