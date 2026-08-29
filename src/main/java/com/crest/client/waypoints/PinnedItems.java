package com.crest.client.waypoints;

import java.util.HashSet;
import java.util.Set;

/** Tracks pinned waypoint ids (kept visible regardless of group/folder hide state). */
public final class PinnedItems {
    private static final Set<String> pinned = new HashSet<>();

    private PinnedItems() {}

    public static boolean isPinned(String id) { return pinned.contains(id); }
    public static void pin(String id) { pinned.add(id); }
    public static void unpin(String id) { pinned.remove(id); }
    public static void toggle(String id) { if (pinned.contains(id)) pinned.remove(id); else pinned.add(id); }
    public static void clear() { pinned.clear(); }
}
