package com.crest.client.waypoints;

/** Stubs for backend renderer compatibility checks. */
public final class RenderBackendCompatibility {
    private RenderBackendCompatibility() {}

    public static boolean supportsFramebufferBlit() { return true; }
    public static boolean supportsGeometryShader() { return false; }
    public static String backendName() { return "OpenGL"; }
}
