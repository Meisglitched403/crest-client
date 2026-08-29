package com.crest.client.waypoints;

/** Reports the current client runtime (Fabric) for feature gating. */
public final class ClientRuntimeCompat {
    private ClientRuntimeCompat() {}

    public static boolean isModLoaded(String id) {
        // Best-effort: Crest bundles everything; treat known ids as loaded.
        return true;
    }

    public static String loaderName() { return "Fabric"; }
}
