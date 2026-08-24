package com.crest.client.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CapeLibrary {
    private static final Path CAPES_DIR = FabricLoader.getInstance().getConfigDir().resolve("crest/capes");
    private static final Path METADATA_FILE = CAPES_DIR.resolve("library.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final Map<String, CapeEntry> entries = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> textures = new ConcurrentHashMap<>();
    private static final List<String> favorites = new ArrayList<>();
    private static String activeCapeId = null;
    
    static {
        try {
            Files.createDirectories(CAPES_DIR);
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static class CapeEntry {
        public String id;
        public String name;
        public String fileName;
        public long addedTime;
        public boolean isFavorite;
        public String source;
        
        public CapeEntry(String id, String name, String fileName, String source) {
            this.id = id;
            this.name = name;
            this.fileName = fileName;
            this.addedTime = System.currentTimeMillis();
            this.isFavorite = false;
            this.source = source;
        }
    }
    
    private static class LibraryData {
        Map<String, CapeEntry> capes = new HashMap<>();
        List<String> favorites = new ArrayList<>();
        String activeCape = null;
    }
    
    public static void load() {
        if (!Files.exists(METADATA_FILE)) return;
        
        try (Reader reader = Files.newBufferedReader(METADATA_FILE)) {
            LibraryData data = GSON.fromJson(reader, LibraryData.class);
            if (data != null) {
                entries.clear();
                entries.putAll(data.capes);
                favorites.clear();
                favorites.addAll(data.favorites);
                activeCapeId = data.activeCape;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void save() {
        try {
            Files.createDirectories(CAPES_DIR);
            LibraryData data = new LibraryData();
            data.capes.putAll(entries);
            data.favorites.addAll(favorites);
            data.activeCape = activeCapeId;
            
            try (Writer writer = Files.newBufferedWriter(METADATA_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String addCape(String name, File file, String source) throws IOException {
        String id = UUID.randomUUID().toString();
        String fileName = id + ".png";
        Path destPath = CAPES_DIR.resolve(fileName);
        Files.copy(file.toPath(), destPath);
        
        CapeEntry entry = new CapeEntry(id, name, fileName, source);
        entries.put(id, entry);
        save();
        return id;
    }
    
    public static String addCapeFromBytes(String name, byte[] data, String source) throws IOException {
        String id = UUID.randomUUID().toString();
        String fileName = id + ".png";
        Path destPath = CAPES_DIR.resolve(fileName);
        Files.write(destPath, data);
        
        CapeEntry entry = new CapeEntry(id, name, fileName, source);
        entries.put(id, entry);
        save();
        return id;
    }
    
    public static void removeCape(String id) {
        CapeEntry entry = entries.remove(id);
        if (entry != null) {
            try {
                Files.deleteIfExists(CAPES_DIR.resolve(entry.fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            favorites.remove(id);
            if (id.equals(activeCapeId)) {
                activeCapeId = null;
            }
            textures.remove(id);
            save();
        }
    }
    
    public static void toggleFavorite(String id) {
        CapeEntry entry = entries.get(id);
        if (entry != null) {
            entry.isFavorite = !entry.isFavorite;
            if (entry.isFavorite) {
                if (!favorites.contains(id)) favorites.add(id);
            } else {
                favorites.remove(id);
            }
            save();
        }
    }
    
    public static void setActiveCape(String id) {
        activeCapeId = id;
        save();
    }
    
    public static void clearActiveCape() {
        activeCapeId = null;
        save();
    }
    
    public static String getActiveCapeId() {
        return activeCapeId;
    }
    
    public static CapeEntry getActiveCape() {
        return activeCapeId != null ? entries.get(activeCapeId) : null;
    }
    
    public static List<CapeEntry> getAllCapes() {
        return new ArrayList<>(entries.values());
    }
    
    public static List<CapeEntry> getFavoriteCapes() {
        List<CapeEntry> result = new ArrayList<>();
        for (String id : favorites) {
            CapeEntry entry = entries.get(id);
            if (entry != null) result.add(entry);
        }
        return result;
    }
    
    public static CapeEntry getCape(String id) {
        return entries.get(id);
    }
    
    public static Identifier getTexture(String id) {
        return textures.computeIfAbsent(id, capeId -> {
            CapeEntry entry = entries.get(capeId);
            if (entry == null) return null;
            
            try {
                Path capePath = CAPES_DIR.resolve(entry.fileName);
                if (!Files.exists(capePath)) return null;
                
                NativeImage image = NativeImage.read(Files.newInputStream(capePath));
                DynamicTexture texture = new DynamicTexture(() -> "crest-cape-" + capeId, image);
                Identifier identifier = Identifier.fromNamespaceAndPath("crest", "capes/" + capeId);
                Minecraft.getInstance().getTextureManager().register(identifier, texture);
                return identifier;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    
    public static Identifier getActiveCapeTexture() {
        if (activeCapeId == null) return null;
        return getTexture(activeCapeId);
    }
    
    public static int getCount() {
        return entries.size();
    }
    
    public static int getFavoritesCount() {
        return favorites.size();
    }
    
    public static String addFromFile(File file) throws IOException {
        if (!file.exists() || !file.getName().toLowerCase().endsWith(".png")) {
            throw new IOException("Invalid file");
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            NativeImage image = NativeImage.read(fis);
            if (image == null) throw new IOException("Failed to read image");
            
            int w = image.getWidth();
            int h = image.getHeight();
            if (w != 64 || h != 32) {
                image.close();
                throw new IOException("Invalid cape dimensions (need 64x32)");
            }
            image.close();
            
            String name = file.getName().replace(".png", "").replace("_", " ");
            return addCape(name, file, "Upload");
        }
    }
}
