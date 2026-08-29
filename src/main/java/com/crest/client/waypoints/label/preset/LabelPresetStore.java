package com.crest.client.waypoints.label.preset;

import com.crest.client.waypoints.label.compose.LabelComposition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stores built-in and user-defined label presets. */
public class LabelPresetStore {
    private static final Gson GSON = new Gson();
    private static final List<LabelPreset> builtIns = new ArrayList<>();
    private static final List<LabelPreset> custom = new ArrayList<>();

    static {
        LabelPreset def = new LabelPreset("default", "Default", true);
        builtIns.add(def);
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("crest-waypoints").resolve("label-presets.json");
    }

    public static List<LabelPreset> getBuiltIns() { return builtIns; }
    public static List<LabelPreset> getCustom() { return custom; }

    public static List<LabelPreset> visibleLabelPresetChoices() {
        List<LabelPreset> all = new ArrayList<>();
        all.addAll(builtIns);
        all.addAll(custom);
        return all;
    }

    public static LabelPreset getById(String id) {
        for (LabelPreset p : visibleLabelPresetChoices()) if (p.id.equals(id)) return p;
        return null;
    }

    public static void initDefaults() {
        if (getById("default") == null) {
            LabelPreset def = new LabelPreset("default", "Default", true);
            def.composition = LabelComposition.defaultComposition();
            def.globals = new LabelGlobals();
            builtIns.add(def);
        }
    }

    public static void load() {
        custom.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            String json = Files.readString(f, StandardCharsets.UTF_8);
            List<LabelPreset> list = GSON.fromJson(json, new TypeToken<List<LabelPreset>>() {});
            if (list != null) custom.addAll(list);
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            String json = GSON.toJson(custom);
            Files.writeString(file(), json, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}
