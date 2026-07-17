package com.crest.client.core;

import com.crest.client.core.setting.ColorSetting;
import com.crest.client.core.setting.Setting;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ponytail: GlintColorizer. Tints the enchantment glint by re-coloring the
 * vanilla glint textures and binding a tinted copy to a custom glint RenderType
 * (which reuses the vanilla GLINT pipeline). No custom shader required.
 */
public class GlintColorizerModule implements CrestModule {
    public static final GlintColorizerModule INSTANCE = new GlintColorizerModule();

    public static final Identifier TINTED_ITEM = Identifier.fromNamespaceAndPath("crest-client", "glint_item_tinted");
    public static final Identifier TINTED_ARMOR = Identifier.fromNamespaceAndPath("crest-client", "glint_armor_tinted");

    private final ColorSetting color = new ColorSetting("Glint Color", 0xFF55FFFF);

    private RenderType itemGlint;
    private RenderType armorGlint;
    private int lastColor = -1;
    private boolean registered;

    @Override public String getId() { return "glint_colorizer"; }
    @Override public String getName() { return "Glint Colorizer"; }
    @Override public String getDescription() { return "Recolors the enchantment glint on items and armor."; }
    @Override public String getCategory() { return "Render"; }
    @Override public boolean isEnabled() { return false; }

    @Override
    public List<Setting<?>> getSettings() {
        return new ArrayList<>(List.of(color));
    }

    @Override
    public void onEnable() { ensure(); }

    public static boolean shouldApply() {
        return CrestModules.isEnabled("glint_colorizer");
    }

    private void ensure() {
        if (!registered) {
            registered = true;
            rebuild();
        } else if (color.get() != lastColor) {
            rebuild();
        }
    }

    private void rebuild() {
        lastColor = color.get();
        int rgb = lastColor;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        float fr = r / 255.0F, fg = g / 255.0F, fb = b / 255.0F;

        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();

        itemGlint = makeGlint(tm, "textures/misc/enchanted_glint_item.png",
            TINTED_ITEM, fr, fg, fb);
        armorGlint = makeGlint(tm, "textures/misc/enchanted_glint_armor.png",
            TINTED_ARMOR, fr, fg, fb);
    }

    private RenderType makeGlint(TextureManager tm, String vanillaPath, Identifier tintedId,
                                 float r, float g, float b) {
        tintGlintTexture(tm, vanillaPath, tintedId, r, g, b);
        RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", tintedId)
            .createRenderSetup();
        return RenderType.create("crest_glint_" + tintedId.getPath(), setup);
    }

    private void tintGlintTexture(TextureManager tm, String vanillaPath, Identifier tintedId,
                                  float r, float g, float b) {
        try {
            Optional<net.minecraft.server.packs.resources.Resource> res =
                Minecraft.getInstance().getResourceManager()
                    .getResource(Identifier.withDefaultNamespace(vanillaPath));
            if (res.isEmpty()) return;
            try (InputStream in = res.get().open()) {
                NativeImage img = NativeImage.read(in);
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int pixel = img.getPixel(x, y); // ABGR
                        int pr = pixel & 0xFF;
                        int pg = (pixel >> 8) & 0xFF;
                        int pb = (pixel >> 16) & 0xFF;
                        int pa = (pixel >> 24) & 0xFF;
                        int nr = (int) (pr * r);
                        int ng = (int) (pg * g);
                        int nb = (int) (pb * b);
                        img.setPixel(x, y, (pa << 24) | (nb << 16) | (ng << 8) | nr);
                    }
                }
                NativeImage copy = img;
                DynamicTexture tex = new DynamicTexture(() -> tintedId.toString(), copy);
                tm.register(tintedId, tex);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("Crest/GlintColorizer")
                .warn("Failed to tint glint texture {}", vanillaPath, e);
        }
    }

    public static RenderType getItemGlint(RenderType original) {
        if (!shouldApply()) return original;
        GlintColorizerModule m = INSTANCE;
        m.ensure();
        return m.itemGlint != null ? m.itemGlint : original;
    }
}
