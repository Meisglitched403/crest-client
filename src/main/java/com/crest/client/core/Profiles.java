package com.crest.client.core;

import com.crest.client.core.setting.Setting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ponytail: Named config profiles (e.g. "PvP", "Survival"). A profile is a
 * snapshot of every module's enabled flag + all its settings, stored in
 * crest-profiles.json. Apply() restores the snapshot and saves.
 */
public final class Profiles {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("crest-profiles.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, Profile>>() {}.getType();

    private static Map<String, Profile> store = new HashMap<>();

    private record SettingVal(String key, Object value) {}
    private record ModuleSnap(boolean enabled, Map<String, Object> settings) {}
    private record Profile(Map<String, ModuleSnap> modules) {}

    static {
        load();
    }

    public static void load() {
        store.clear();
        if (PATH.toFile().exists()) {
            try (FileReader r = new FileReader(PATH.toFile())) {
                Map<String, Profile> loaded = GSON.fromJson(r, TYPE);
                if (loaded != null) store.putAll(loaded);
            } catch (Exception ignored) {}
        }
    }

    private static void saveFile() {
        try {
            PATH.toFile().getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(PATH.toFile())) {
                GSON.toJson(store, w);
            }
        } catch (Exception ignored) {}
    }

    public static java.util.Set<String> names() {
        return new java.util.LinkedHashSet<>(store.keySet());
    }

    public static boolean has(String name) {
        return store.containsKey(name);
    }

    /** Capture the current state of all modules into a profile. */
    public static void save(String name) {
        Map<String, ModuleSnap> snaps = new LinkedHashMap<>();
        ConfigManager cfg = CrestModules.getConfigManager();
        for (CrestModule mod : CrestModules.getAll().values()) {
            boolean enabled = CrestModules.isEnabled(mod.getId());
            Map<String, Object> settings = new LinkedHashMap<>();
            for (Setting<?> s : mod.getSettings()) {
                Object v = s.get();
                if (v instanceof Boolean || v instanceof Number || v instanceof String) {
                    settings.put(s.getName(), v);
                } else if (v instanceof int[] ia) {
                    settings.put(s.getName(), ia.clone());
                }
            }
            snaps.put(mod.getId(), new ModuleSnap(enabled, settings));
        }
        store.put(name, new Profile(snaps));
        saveFile();
    }

    /** Restore a profile's snapshot into the live modules + config. */
    public static void apply(String name) {
        Profile p = store.get(name);
        if (p == null) return;
        ConfigManager cfg = CrestModules.getConfigManager();
        for (var entry : p.modules().entrySet()) {
            String id = entry.getKey();
            ModuleSnap snap = entry.getValue();
            CrestModules.setEnabled(id, snap.enabled());
            for (var sv : snap.settings().entrySet()) {
                cfg.set(id, sv.getKey(), sv.getValue());
            }
        }
        cfg.save();
        CrestModules.reloadSettings();
    }

    public static void delete(String name) {
        if (store.remove(name) != null) saveFile();
    }
}
