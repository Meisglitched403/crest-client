package com.crest.client.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads/saves the global ModConfig. */
public final class ConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getPath() {
        return ModPaths.configDir().resolve("mod-config.json");
    }

    public static ModConfig load() {
        Path p = getPath();
        if (!Files.exists(p)) return createDefaultConfig();
        try {
            String json = Files.readString(p, StandardCharsets.UTF_8);
            ModConfig cfg = GSON.fromJson(json, ModConfig.class);
            if (cfg == null) return createDefaultConfig();
            sanitize(cfg);
            return cfg;
        } catch (Exception e) {
            return createDefaultConfig();
        }
    }

    public static ModConfig createDefaultConfig() {
        return new ModConfig();
    }

    public static void save(ModConfig cfg) {
        try {
            Files.createDirectories(getPath().getParent());
            Files.writeString(getPath(), GSON.toJson(cfg), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    /** Fill any null nested objects that older saves may be missing. */
    public static void sanitize(ModConfig cfg) {
        if (cfg.singleWaypointDefaults == null) cfg.singleWaypointDefaults = new ModConfig.TypeDefaults();
        if (cfg.clumpDefaults == null) cfg.clumpDefaults = new ModConfig.TypeDefaults();
        if (cfg.groupDefaults == null) cfg.groupDefaults = new ModConfig.TypeDefaults();
        if (cfg.hopliteSupplyDropAppearance == null) cfg.hopliteSupplyDropAppearance = new ModConfig.HopliteAppearance();
        if (cfg.hopliteAutoPickAppearance == null) cfg.hopliteAutoPickAppearance = new ModConfig.HopliteAppearance();
        if (cfg.deathWaypointAppearance == null) cfg.deathWaypointAppearance = new ModConfig.HopliteAppearance();
        if (cfg.deathWaypointsAutoHiddenIds == null) cfg.deathWaypointsAutoHiddenIds = new java.util.ArrayList<>();
        if (cfg.hideWaypointCycleThresholds == null) cfg.hideWaypointCycleThresholds = new int[] { 0, 100, 500, 2000 };
    }
}
