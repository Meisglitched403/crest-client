package com.crest.client.waypoints;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.Locale;

/** Resolves the current world id and dimension id used as storage keys. */
public final class WorldContext {
    private static final String UNASSIGNED = "__unassigned__";

    private WorldContext() {}

    public static String currentWorldId(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            Path root = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
            return "sp:" + root.toAbsolutePath().normalize();
        }
        ServerData info = mc.getCurrentServer();
        if (info != null && info.ip != null && !info.ip.isBlank()) {
            return "mp:" + info.ip.trim().toLowerCase(Locale.ROOT);
        }
        if (mc.getConnection() != null && mc.getConnection().getConnection() != null) {
            var addr = mc.getConnection().getConnection().getRemoteAddress();
            if (addr != null) return "mp:" + addr;
        }
        return UNASSIGNED;
    }

    public static String currentDimensionId(Minecraft mc) {
        if (mc.level == null) return "unknown";
        return mc.level.dimension().toString();
    }
}
