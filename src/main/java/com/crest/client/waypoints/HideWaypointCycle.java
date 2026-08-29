package com.crest.client.waypoints;

/** Distance-based hide thresholds that cycle through a set of values. */
public final class HideWaypointCycle {
    public static final int SHOW_ALL = 0;
    public static final int HIDE_ALL = Integer.MAX_VALUE;

    private HideWaypointCycle() {}

    public static int[] normalize(int[] thresholds, int maxBlocks) {
        if (thresholds == null || thresholds.length == 0) return new int[] { 0 };
        int[] out = new int[thresholds.length];
        for (int i = 0; i < thresholds.length; i++) out[i] = Math.max(0, Math.min(maxBlocks, thresholds[i]));
        return out;
    }

    public static int nextIndex(int[] thresholds, int current) {
        return (current + 1) % Math.max(1, thresholds.length);
    }

    public static int activeThreshold(int[] thresholds, int index) {
        if (thresholds == null || index < 0 || index >= thresholds.length) return SHOW_ALL;
        return thresholds[index];
    }

    public static String label(int threshold) {
        if (threshold <= 0) return "Show all";
        if (threshold == HIDE_ALL) return "Hide all";
        return "Hide < " + threshold + "m";
    }
}
