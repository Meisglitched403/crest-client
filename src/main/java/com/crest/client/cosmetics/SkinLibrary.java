package com.crest.client.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinLibrary {
    private static final Path SKINS_DIR = FabricLoader.getInstance().getConfigDir().resolve("crest/skins");
    private static final Path METADATA_FILE = SKINS_DIR.resolve("library.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final Map<String, SkinEntry> entries = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> textures = new ConcurrentHashMap<>();
    private static final List<String> favorites = new ArrayList<>();
    private static final LinkedList<String> history = new LinkedList<>();
    private static final int MAX_HISTORY = 10;
    
    static {
        try {
            Files.createDirectories(SKINS_DIR);
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static class SkinEntry {
        public String id;
        public String name;
        public String fileName;
        public PlayerModelType modelType;
        public long addedTime;
        public boolean isFavorite;
        public String source;
        
        public SkinEntry(String id, String name, String fileName, PlayerModelType modelType, String source) {
            this.id = id;
            this.name = name;
            this.fileName = fileName;
            this.modelType = modelType;
            this.addedTime = System.currentTimeMillis();
            this.isFavorite = false;
            this.source = source;
        }
    }
    
    private static class LibraryData {
        Map<String, SkinEntry> skins = new HashMap<>();
        List<String> favorites = new ArrayList<>();
        List<String> history = new ArrayList<>();
    }
    
    public static void load() {
        if (!Files.exists(METADATA_FILE)) return;
        
        try (Reader reader = Files.newBufferedReader(METADATA_FILE)) {
            LibraryData data = GSON.fromJson(reader, LibraryData.class);
            if (data != null) {
                entries.clear();
                entries.putAll(data.skins);
                favorites.clear();
                favorites.addAll(data.favorites);
                history.clear();
                history.addAll(data.history);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void save() {
        try {
            Files.createDirectories(SKINS_DIR);
            LibraryData data = new LibraryData();
            data.skins.putAll(entries);
            data.favorites.addAll(favorites);
            data.history.addAll(history);
            
            try (Writer writer = Files.newBufferedWriter(METADATA_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String addSkin(String name, File file, PlayerModelType modelType, String source) throws IOException {
        String id = UUID.randomUUID().toString();
        String fileName = id + ".png";
        Path destPath = SKINS_DIR.resolve(fileName);
        Files.copy(file.toPath(), destPath);
        
        SkinEntry entry = new SkinEntry(id, name, fileName, modelType, source);
        entries.put(id, entry);
        addToHistory(id);
        save();
        return id;
    }
    
    public static String addSkinFromBytes(String name, byte[] data, PlayerModelType modelType, String source) throws IOException {
        String id = UUID.randomUUID().toString();
        String fileName = id + ".png";
        Path destPath = SKINS_DIR.resolve(fileName);
        Files.write(destPath, data);
        
        SkinEntry entry = new SkinEntry(id, name, fileName, modelType, source);
        entries.put(id, entry);
        addToHistory(id);
        save();
        return id;
    }
    
    public static void removeSkin(String id) {
        SkinEntry entry = entries.remove(id);
        if (entry != null) {
            try {
                Files.deleteIfExists(SKINS_DIR.resolve(entry.fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            favorites.remove(id);
            history.remove(id);
            textures.remove(id);
            save();
        }
    }
    
    public static void toggleFavorite(String id) {
        SkinEntry entry = entries.get(id);
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
    
    public static void addToHistory(String id) {
        history.remove(id);
        history.addFirst(id);
        while (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
        save();
    }
    
    public static List<SkinEntry> getAllSkins() {
        return new ArrayList<>(entries.values());
    }
    
    public static List<SkinEntry> getFavoriteSkins() {
        List<SkinEntry> result = new ArrayList<>();
        for (String id : favorites) {
            SkinEntry entry = entries.get(id);
            if (entry != null) result.add(entry);
        }
        return result;
    }
    
    public static List<SkinEntry> getRecentSkins() {
        List<SkinEntry> result = new ArrayList<>();
        for (String id : history) {
            SkinEntry entry = entries.get(id);
            if (entry != null) result.add(entry);
        }
        return result;
    }
    
    public static SkinEntry getSkin(String id) {
        return entries.get(id);
    }
    
    public static Identifier getTexture(String id) {
        return textures.computeIfAbsent(id, skinId -> {
            SkinEntry entry = entries.get(skinId);
            if (entry == null) return null;
            
            try {
                Path skinPath = SKINS_DIR.resolve(entry.fileName);
                if (!Files.exists(skinPath)) return null;
                
                NativeImage image = NativeImage.read(Files.newInputStream(skinPath));
                DynamicTexture texture = new DynamicTexture(() -> "crest-skin-" + skinId, image);
                Identifier identifier = Identifier.fromNamespaceAndPath("crest", "skins/" + skinId);
                Minecraft.getInstance().getTextureManager().register(identifier, texture);
                return identifier;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    
    public static PlayerSkin getPlayerSkin(String id) {
        SkinEntry entry = entries.get(id);
        if (entry == null) return null;
        
        Identifier texture = getTexture(id);
        if (texture == null) return null;
        
        return PlayerSkin.insecure(new ClientAsset.ResourceTexture(texture), null, null, entry.modelType);
    }
    
    public static PlayerModelType detectModelType(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            NativeImage image = NativeImage.read(fis);
            return detectModelType(image);
        } catch (IOException e) {
            return PlayerModelType.WIDE;
        }
    }
    
    public static PlayerModelType detectModelType(NativeImage image) {
        if (image.getWidth() != 64 || image.getHeight() != 64) {
            return PlayerModelType.WIDE;
        }
        
        boolean hasTransparency = false;
        for (int x = 50; x < 54; x++) {
            for (int y = 16; y < 20; y++) {
                int alpha = (image.getPixel(x, y) >> 24) & 0xFF;
                if (alpha == 0) {
                    hasTransparency = true;
                    break;
                }
            }
            if (hasTransparency) break;
        }
        
        return hasTransparency ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }
    
    public static int getCount() {
        return entries.size();
    }
    
    public static int getFavoritesCount() {
        return favorites.size();
    }
    
    public static void addFromUsername(String username, java.util.function.Consumer<String> onComplete, Runnable onError) {
        if (username == null || username.trim().isEmpty()) {
            if (onError != null) onError.run();
            return;
        }
        final String query = username.trim();
        Minecraft mc = Minecraft.getInstance();
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                // Use NameMC API to get skin - more reliable than Mojang
                // First get UUID from Mojang API
                String uuidUrl = "https://api.mojang.com/users/profiles/minecraft/" + query;
                java.net.URL url = new java.net.URL(uuidUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                if (conn.getResponseCode() != 200) {
                    return null;
                }
                
                java.io.InputStream stream = conn.getInputStream();
                String jsonResponse = new String(stream.readAllBytes());
                stream.close();
                
                // Parse JSON to get UUID
                com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(jsonResponse).getAsJsonObject();
                if (!json.has("id")) return null;
                String uuid = json.get("id").getAsString();
                
                // Format UUID with dashes
                String formattedUuid = uuid.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
                );
                
                // Get skin from NameMC (better quality, no auth needed)
                String skinUrl = "https://api.mineatar.io/skin/" + formattedUuid;
                
                // Download the skin
                java.net.URL imageUrl = new java.net.URL(skinUrl);
                java.net.HttpURLConnection skinConn = (java.net.HttpURLConnection) imageUrl.openConnection();
                skinConn.setRequestMethod("GET");
                skinConn.setConnectTimeout(5000);
                skinConn.setReadTimeout(5000);
                
                if (skinConn.getResponseCode() != 200) {
                    return null;
                }
                
                java.io.InputStream skinStream = skinConn.getInputStream();
                byte[] imageBytes = skinStream.readAllBytes();
                skinStream.close();
                
                // Save to library
                String id = UUID.randomUUID().toString();
                String fileName = id + ".png";
                Path destPath = SKINS_DIR.resolve(fileName);
                Files.write(destPath, imageBytes);
                
                // Detect model type from the saved image
                PlayerModelType modelType = detectModelType(new File(destPath.toString()));
                
                SkinEntry entry = new SkinEntry(id, query, fileName, modelType, "Player: " + query);
                entries.put(id, entry);
                addToHistory(id);
                save();
                return id;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, net.minecraft.util.Util.nonCriticalIoPool()).thenAccept(id -> Minecraft.getInstance().execute(() -> {
            if (id != null && onComplete != null) {
                onComplete.accept(id);
            } else if (id == null && onError != null) {
                onError.run();
            }
        }));
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
            if (w != 64 || (h != 32 && h != 64)) {
                image.close();
                throw new IOException("Invalid skin dimensions (need 64x32 or 64x64)");
            }
            
            PlayerModelType modelType = detectModelType(image);
            image.close();
            
            String name = file.getName().replace(".png", "").replace("_", " ");
            return addSkin(name, file, modelType, "Upload");
        }
    }
}
