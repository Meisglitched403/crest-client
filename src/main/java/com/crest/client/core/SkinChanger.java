package com.crest.client.core;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import com.mojang.blaze3d.platform.NativeImage;
import com.crest.client.core.CrestModules;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class SkinChanger {

    private static PlayerSkin current = null;
    private static PlayerSkin applied = null;
    private static String status = "";
    private static boolean loading = false;

    private SkinChanger() {}

    public static Supplier<PlayerSkin> supplier() {
        return SkinChanger::currentSkin;
    }

    public static PlayerSkin currentSkin() {
        return current != null ? current : defaultPlayerSkin();
    }

    public static String status() {
        return status;
    }

    public static boolean isLoading() {
        return loading;
    }

    public static void setStatus(String s) {
        status = s == null ? "" : s;
    }

    public static PlayerSkin getOverride() {
        return applied;
    }

    public static void reset() {
        current = null;
        applied = null;
        status = "";
        loading = false;
    }

    public static void applyToLocalPlayer() {
        applied = currentSkin();
        status = "Applied to your player";
    }

    private static void saveSource(String source) {
        CrestModules.getConfigManager().set("crest_client", "skin_source", source);
        CrestModules.getConfigManager().save();
    }

    public static void loadPersisted() {
        String source = CrestModules.getConfigManager().getString("crest_client", "skin_source");
        if (source == null || source.isEmpty()) return;
        if (source.startsWith("path:")) {
            loadFromFile(new File(source.substring(5)));
        } else if (source.startsWith("user:")) {
            loadFromUsername(source.substring(5));
        }
    }

    private static PlayerSkin defaultPlayerSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getGameProfile() != null) return DefaultPlayerSkin.get(mc.getGameProfile());
        return DefaultPlayerSkin.getDefaultSkin();
    }

    public static void loadFromFile(File file) {
        if (file == null || !file.exists()) {
            status = "File not found";
            return;
        }
        loading = true;
        status = "Loading image...";
        CompletableFuture.supplyAsync(() -> {
            try (FileInputStream in = new FileInputStream(file)) {
                NativeImage src = NativeImage.read(in);
                if (src == null) return (PlayerSkin) null;
                int w = src.getWidth();
                int h = src.getHeight();
                if (w != 64 || (h != 32 && h != 64)) {
                    src.close();
                    return (PlayerSkin) null;
                }
                PlayerModelType model = detectModel(src);
                Identifier id = Identifier.fromNamespaceAndPath("crest-client",
                        "skin/upload/" + UUID.randomUUID().toString().replace("-", ""));
                DynamicTexture tex = new DynamicTexture(id::toDebugFileName, src);
                Minecraft.getInstance().getTextureManager().register(id, tex);
                ClientAsset.Texture body = new ClientAsset.ResourceTexture(id);
                return (PlayerSkin) PlayerSkin.insecure(body, null, null, model);
            } catch (Exception e) {
                return (PlayerSkin) null;
            }
        }, Util.nonCriticalIoPool()).thenAccept(skin -> {
            loading = false;
            if (skin != null) {
                current = skin;
                status = "Loaded from file";
                applied = skin;
                saveSource("path:" + file.getAbsolutePath());
            } else {
                status = "Invalid skin (need 64x32 or 64x64 PNG)";
            }
        });
    }

    public static void loadFromUsername(String name) {
        if (name == null || name.trim().isEmpty()) {
            status = "Enter a username";
            return;
        }
        final String query = name.trim();
        loading = true;
        status = "Looking up " + query + "...";
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> {
            try {
                GameProfileRepository repo = mc.services().profileRepository();
                java.util.Optional<NameAndId> resolved = repo.findProfileByName(query);
                if (resolved.isEmpty()) return (PlayerSkin) null;
                UUID uuid = resolved.get().id();
                ProfileResult result = mc.services().sessionService().fetchProfile(uuid, true);
                GameProfile full = result.profile();
                if (full == null) return (PlayerSkin) null;
                return (PlayerSkin) mc.getSkinManager().get(full).join().orElse(null);
            } catch (Exception e) {
                return (PlayerSkin) null;
            }
        }, Util.nonCriticalIoPool()).thenAccept(skin -> {
            loading = false;
            if (skin != null) {
                current = skin;
                applied = skin;
                status = "Loaded " + query;
                saveSource("user:" + query);
            } else {
                status = "Player not found: " + query;
            }
        });
    }

    private static PlayerModelType detectModel(NativeImage ni) {
        if (ni.getHeight() < 64) return PlayerModelType.WIDE;
        int slim = 0, wide = 0;
        for (int y = 0; y < ni.getHeight(); y++) {
            if (isTransparent(ni, 42, y)) slim++;
            if (isTransparent(ni, 41, y)) wide++;
        }
        return slim > wide ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }

    private static boolean isTransparent(NativeImage ni, int x, int y) {
        if (x < 0 || x >= ni.getWidth() || y < 0 || y >= ni.getHeight()) return false;
        return (ni.getPixel(x, y) & 0xFF000000) == 0;
    }
}
