package com.crest.client.waypoints;

/** Tracks whether the mod has finished initial startup. */
public final class StartupStateService {
    private static volatile boolean started = false;

    private StartupStateService() {}

    public static boolean didStart() { return started; }
    public static void markStarted() { started = true; }
}
