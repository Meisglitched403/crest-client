package com.crest.client.bongocat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

/**
 * ponytail: Loads the real bongo-cat-obs PNGs (bundled in the mod jar under
 * assets/crest-client/bongocat/) into registered Minecraft textures. Custom
 * textures are NOT auto-loaded by blit — they must be registered with the
 * TextureManager first (same pattern as ui/Panel).
 */
public final class BongoCatTextures {
    private static final Logger LOG = LoggerFactory.getLogger("Crest/BongoCat");

    public static final Identifier REST = Identifier.fromNamespaceAndPath("crest-client", "bongocat/rest");
    public static final Identifier LEFT = Identifier.fromNamespaceAndPath("crest-client", "bongocat/left");
    public static final Identifier RIGHT = Identifier.fromNamespaceAndPath("crest-client", "bongocat/right");

    private static boolean registered = false;
    static int TEX_W = 226;
    static int TEX_H = 170;

    private BongoCatTextures() {}

    public static void ensure() {
        if (registered) return;
        registered = true;
        load(REST, "bongocat/cat-rest.png");
        load(LEFT, "bongocat/cat-left.png");
        load(RIGHT, "bongocat/cat-right.png");
    }

    private static void load(Identifier id, String path) {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        try {
            Optional<net.minecraft.server.packs.resources.Resource> res =
                mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath("crest-client", path));
            if (res.isEmpty()) {
                LOG.error("Missing bongo cat texture {}", path);
                return;
            }
            try (InputStream in = res.get().open()) {
                NativeImage img = NativeImage.read(in);
                TEX_W = img.getWidth();
                TEX_H = img.getHeight();
                DynamicTexture tex = new DynamicTexture(() -> "bongocat", img);
                tm.register(id, tex);
            }
        } catch (Exception e) {
            LOG.error("Failed to load bongo cat texture {}", path, e);
        }
    }
}
