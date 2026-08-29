package com.crest.client.waypoints;

import java.util.concurrent.atomic.AtomicBoolean;

/** Debounced save queue so rapid edits don't thrash disk. */
public final class WaypointSaveQueue {
    private static final AtomicBoolean pending = new AtomicBoolean(false);

    private WaypointSaveQueue() {}

    public static void markDirty() { pending.set(true); }

    public static boolean consume() { return pending.compareAndSet(true, false); }
}
