package com.crest.client.waypoints;

/** Navigation helper for the (deferred) menu screens. */
public final class WaypointMenuNavigation {
    private static String currentFolder = "";

    private WaypointMenuNavigation() {}

    public static String currentFolderId() { return currentFolder; }
    public static void enterFolder(String id) { currentFolder = id; }
    public static void exitFolder() { currentFolder = ""; }
    public static boolean inFolder() { return !currentFolder.isEmpty(); }
}
