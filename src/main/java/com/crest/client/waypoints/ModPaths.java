package com.crest.client.waypoints;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Filesystem layout for wWaypoints data, rooted under the Fabric config dir. */
public final class ModPaths {
    private ModPaths() {}

    public static Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static Path baseDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("crest-waypoints");
    }

    public static Path configDir() {
        return baseDir();
    }

    public static Path worldsDir() {
        return baseDir().resolve("worlds");
    }

    public static Path worldDir(WorldKind kind, String id) {
        return worldsDir().resolve(kind.folder).resolve(sanitizeForFilename(id));
    }

    public static Path customIconsDir() {
        return baseDir().resolve("custom-icons");
    }

    public static Path ensureCustomIconsDir() {
        try { Files_create(customIconsDir()); } catch (Exception ignored) {}
        return customIconsDir();
    }

    public enum WorldKind {
        SINGLEPLAYER("sp"),
        MULTIPLAYER("mp");

        public final String folder;
        WorldKind(String f) { this.folder = f; }
    }

    public static String sanitizeForFilename(String s) {
        if (s == null) return "unknown";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') b.append(c);
            else b.append('_');
        }
        return b.length() == 0 ? "unknown" : b.toString();
    }

    public static String dimensionFileName(String dimId) {
        return sanitizeForFilename(dimId) + ".json";
    }

    private static void Files_create(Path p) throws Exception {
        java.nio.file.Files.createDirectories(p);
    }
}
