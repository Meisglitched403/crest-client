package com.crest.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Waypoint> waypoints = new ArrayList<>();
    private static File configFile;

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        configFile = new File(mc.gameDirectory, "config/crest-waypoints.json");
        load();
    }

    public static void load() {
        waypoints.clear();
        if (configFile != null && configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Type type = new TypeToken<List<Waypoint>>(){}.getType();
                List<Waypoint> loaded = GSON.fromJson(reader, type);
                if (loaded != null) waypoints.addAll(loaded);
            } catch (Exception e) {
                System.err.println("[Crest] Failed to load waypoints: " + e);
            }
        }
    }

    public static void save() {
        if (configFile == null) return;
        try {
            configFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(waypoints, writer);
            }
        } catch (Exception e) {
            System.err.println("[Crest] Failed to save waypoints: " + e);
        }
    }

    public static List<Waypoint> getAll() { return waypoints; }

    public static void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        save();
    }

    public static void remove(String name) {
        waypoints.removeIf(w -> w.getName().equals(name));
        save();
    }

    public static Waypoint get(String name) {
        return waypoints.stream().filter(w -> w.getName().equals(name)).findFirst().orElse(null);
    }

    public static List<Waypoint> getVisible() {
        String currentDim = getCurrentDimension();
        return waypoints.stream()
            .filter(w -> w.isEnabled())
            .filter(w -> w.getDimension().equals(currentDim))
            .toList();
    }

    private static String getCurrentDimension() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "minecraft:overworld";
        return mc.level.dimension().identifier().toString();
    }
}
