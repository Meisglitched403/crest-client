package com.crest.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("crest.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();

    private final Map<String, Map<String, Object>> data = new HashMap<>();
    private boolean dirty;

    public boolean has(String moduleId, String key) {
        Map<String, Object> module = data.get(moduleId);
        return module != null && module.containsKey(key);
    }

    public boolean getBoolean(String moduleId, String key) {
        Object v = getValue(moduleId, key);
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return false;
    }

    public int getInt(String moduleId, String key) {
        Object v = getValue(moduleId, key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    public float getFloat(String moduleId, String key) {
        Object v = getValue(moduleId, key);
        if (v instanceof Number n) return n.floatValue();
        return 0f;
    }

    public String getString(String moduleId, String key) {
        Object v = getValue(moduleId, key);
        return v instanceof String s ? s : null;
    }

    public <T> void set(String moduleId, String key, T value) {
        data.computeIfAbsent(moduleId, k -> new HashMap<>()).put(key, value);
        dirty = true;
    }

    public void load() {
        data.clear();
        if (CONFIG_PATH.toFile().exists()) {
            try (FileReader r = new FileReader(CONFIG_PATH.toFile())) {
                Map<String, Map<String, Object>> loaded = GSON.fromJson(r, DATA_TYPE);
                if (loaded != null) data.putAll(loaded);
            } catch (IOException ignored) {}
        }
    }

    public void save() {
        if (!dirty) return;
        dirty = false;
        CONFIG_PATH.toFile().getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(data, w);
        } catch (IOException ignored) {}
    }

    public void markDirty() { dirty = true; }

    private Object getValue(String moduleId, String key) {
        Map<String, Object> module = data.get(moduleId);
        return module != null ? module.get(key) : null;
    }
}
