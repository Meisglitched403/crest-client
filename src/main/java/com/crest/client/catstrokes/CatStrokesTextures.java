package com.crest.client.catstrokes;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

public final class CatStrokesTextures {
    private static final Logger LOG = LoggerFactory.getLogger("Crest/CatStrokes");

    public static final Identifier CATBG = Identifier.fromNamespaceAndPath("crest-client", "catstrokes/catbg");

    public static final Identifier[] KEYBOARD_OVERLAYS = new Identifier[15];
    public static final Identifier[] LEFTHAND = new Identifier[15];
    public static final Identifier LEFTUP = Identifier.fromNamespaceAndPath("crest-client", "catstrokes/leftup");

    public static final Identifier RIGHTARM = Identifier.fromNamespaceAndPath("crest-client", "catstrokes/rightarm");
    public static final Identifier RIGHTHAND = Identifier.fromNamespaceAndPath("crest-client", "catstrokes/righthand");

    public static int TEX_W = 612;
    public static int TEX_H = 354;

    private static boolean registered = false;
    private static boolean loadFailed = false;

    private CatStrokesTextures() {}

    public static void ensure() {
        if (registered) return;
        registered = true;

        load(CATBG, "catstrokes/catbg.png");

        for (int i = 0; i < 15; i++) {
            KEYBOARD_OVERLAYS[i] = Identifier.fromNamespaceAndPath("crest-client", "catstrokes/kb_" + i);
            LEFTHAND[i] = Identifier.fromNamespaceAndPath("crest-client", "catstrokes/lh_" + i);
            load(KEYBOARD_OVERLAYS[i], "catstrokes/keyboard/" + i + ".png");
            load(LEFTHAND[i], "catstrokes/lefthand/" + i + ".png");
        }

        load(LEFTUP, "catstrokes/lefthand/leftup.png");

        load(RIGHTARM, "catstrokes/righthand/rightarm.png");
        load(RIGHTHAND, "catstrokes/righthand/righthand.png");
    }

    private static void load(Identifier id, String path) {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        try {
            Optional<net.minecraft.server.packs.resources.Resource> res =
                mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("crest-client", path));
            if (res.isEmpty()) {
                LOG.error("Missing catstrokes texture {}", path);
                loadFailed = true;
                return;
            }
            try (InputStream in = res.get().open()) {
                NativeImage img = NativeImage.read(in);
                DynamicTexture tex = new DynamicTexture(() -> "catstrokes", img);
                tm.register(id, tex);
            }
        } catch (Exception e) {
            LOG.error("Failed to load catstrokes texture {}", path, e);
            loadFailed = true;
        }
    }

    public static boolean hasFailed() {
        return loadFailed;
    }
}
