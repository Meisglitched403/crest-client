package com.crest.client.bongocat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class BongoCatConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bongocat.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BongoCatConfig instance;

    public float scale = 1.0f;

    public static BongoCatConfig getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    public static void reload() {
        instance = load();
    }

    private static BongoCatConfig load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
                return GSON.fromJson(reader, BongoCatConfig.class);
            } catch (IOException e) {
                return new BongoCatConfig();
            }
        }
        return new BongoCatConfig();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException ignored) {
        }
    }
}
