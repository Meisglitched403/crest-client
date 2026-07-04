package com.crest.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class HudSettings {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("crest-hud.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HudSettings instance;
    private static boolean dirty;

    public Map<String, HudPosition> positions = new HashMap<>();
    public Map<String, Map<String, Object>> moduleSettings = new HashMap<>();

    public static HudSettings getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    public static HudPosition getPosition(String moduleId, int defaultX, int defaultY) {
        return getInstance().positions.computeIfAbsent(moduleId, k -> new HudPosition(defaultX, defaultY));
    }

    public static void setPosition(String moduleId, int x, int y) {
        HudPosition pos = getInstance().positions.computeIfAbsent(moduleId, k -> new HudPosition(0, 0));
        pos.x = x;
        pos.y = y;
        dirty = true;
    }

    @SuppressWarnings("unchecked")
    public static int getInt(String moduleId, String key, int defaultValue) {
        Map<String, Object> s = getInstance().moduleSettings.computeIfAbsent(moduleId, k -> new HashMap<>());
        Object v = s.get(key);
        if (v instanceof Number n) return n.intValue();
        return defaultValue;
    }

    public static void setInt(String moduleId, String key, int value) {
        getInstance().moduleSettings.computeIfAbsent(moduleId, k -> new HashMap<>()).put(key, value);
        dirty = true;
    }

    public static void save() {
        if (!dirty) return;
        dirty = false;
        try (FileWriter w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(getInstance(), w);
        } catch (IOException ignored) {
        }
    }

    private static HudSettings load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (FileReader r = new FileReader(CONFIG_PATH.toFile())) {
                return GSON.fromJson(r, HudSettings.class);
            } catch (IOException e) {
                return new HudSettings();
            }
        }
        return new HudSettings();
    }

    public static class HudPosition {
        public int x;
        public int y;

        public HudPosition() {}

        public HudPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
