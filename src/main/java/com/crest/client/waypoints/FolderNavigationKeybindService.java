package com.crest.client.waypoints;

import java.util.ArrayList;
import java.util.List;

/** Cycles the active folder scope so only one folder's waypoints are shown. */
public final class FolderNavigationKeybindService {
    private FolderNavigationKeybindService() {}

    public static boolean isKeyboardBindable(int key) {
        return key != org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
    }

    public static void cycle(WaypointManager mgr, ModConfig cfg) {
        List<String> ids = new ArrayList<>();
        ids.add("");
        for (Waypoint w : mgr.getWaypoints()) {
            String fid = mgr.getFolderIdForWaypoint(w.getId());
            if (fid != null && !fid.isEmpty() && !ids.contains(fid)) ids.add(fid);
        }
        int idx = ids.indexOf(cfg.activeFolderScope);
        if (idx < 0) idx = 0;
        cfg.activeFolderScope = ids.get((idx + 1) % ids.size());
    }

    public static String currentLabel(WaypointManager mgr, ModConfig cfg) {
        if (cfg.activeFolderScope == null || cfg.activeFolderScope.isEmpty()) return "All";
        Waypoint f = mgr.getById(cfg.activeFolderScope);
        return f != null ? f.getName() : "Folder";
    }
}
