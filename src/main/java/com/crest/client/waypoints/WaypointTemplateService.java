package com.crest.client.waypoints;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Stores reusable waypoint templates (name/icon/color presets). */
public final class WaypointTemplateService {
    private static final Map<String, Waypoint> templates = new HashMap<>();

    private WaypointTemplateService() {}

    public static void put(String id, Waypoint sample) { templates.put(id, sample); }
    public static Waypoint get(String id) { return templates.get(id); }
    public static boolean has(String id) { return templates.containsKey(id); }
    public static Map<String, Waypoint> all() { return new HashMap<>(templates); }

    public static Waypoint instantiate(String id, BlockPos pos) {
        Waypoint t = templates.get(id);
        if (t == null) return null;
        return t.copyTo(pos);
    }
}
