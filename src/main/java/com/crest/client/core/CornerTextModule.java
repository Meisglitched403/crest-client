package com.crest.client.core;

import com.crest.client.core.HudBackground;
import com.crest.client.core.setting.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CornerTextModule implements CrestModule {
    private final StringSetting text = new StringSetting("Text", "Crest Client");
    private final ModeSetting corner = new ModeSetting("Corner",
        new String[]{"Top Left", "Top Right", "Bottom Left", "Bottom Right"}, 3);
    private final ColorSetting color = new ColorSetting("Text Color", 0xFFFFFFFF);
    private final BooleanSetting shadow = new BooleanSetting("Text Shadow", true);
    private final BooleanSetting background = new BooleanSetting("Background", false);
    private final IntegerSetting offsetX = new IntegerSetting("Offset X", 0, 50, 4);
    private final IntegerSetting offsetY = new IntegerSetting("Offset Y", 0, 50, 4);
    private final FloatSetting scale = new FloatSetting("Scale", 0.5f, 3.0f, 1.0f);
    private final BooleanSetting imageEnabled = new BooleanSetting("Show Image", false);
    private final StringSetting imagePath = new StringSetting("Image Path", "");
    private final FloatSetting imageScale = new FloatSetting("Image Scale", 0.1f, 5.0f, 1.0f);
    private final IntegerSetting imageOffsetX = new IntegerSetting("Image Offset X", -500, 500, 0);
    private final IntegerSetting imageOffsetY = new IntegerSetting("Image Offset Y", -500, 500, 0);

    @Override public String getId() { return "corner_text"; }
    @Override public String getName() { return "Corner Text"; }
    @Override public String getDescription() { return "Shows customizable text and/or image in the corner of container screens"; }
    @Override public String getCategory() { return "Visual"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(text, corner, color, shadow, background, offsetX, offsetY, scale,
            imageEnabled, imagePath, imageScale, imageOffsetX, imageOffsetY);
    }

    @Override
    public Screen createConfigScreen(Screen parent) {
        return new CornerTextConfigScreen(this, parent);
    }

    public static String getText() {
        CornerTextModule m = getModule();
        return m != null ? m.text.get() : "Crest Client";
    }
    public static String getCorner() {
        CornerTextModule m = getModule();
        return m != null ? m.corner.getMode() : "Bottom Right";
    }
    public static int getColor() {
        CornerTextModule m = getModule();
        return m != null ? m.color.get() : 0xFFFFFFFF;
    }
    public static boolean hasShadow() {
        CornerTextModule m = getModule();
        return m != null && m.shadow.get();
    }
    public static boolean isBackgroundEnabled() {
        CornerTextModule m = getModule();
        return m != null && m.background.get();
    }
    public static int getOffsetX() {
        CornerTextModule m = getModule();
        return m != null ? m.offsetX.get() : 4;
    }
    public static int getOffsetY() {
        CornerTextModule m = getModule();
        return m != null ? m.offsetY.get() : 4;
    }
    public static float getScale() {
        CornerTextModule m = getModule();
        return m != null ? m.scale.get() : 1.0f;
    }
    public static boolean isImageEnabled() {
        CornerTextModule m = getModule();
        return m != null && m.imageEnabled.get();
    }
    public static String getImagePath() {
        CornerTextModule m = getModule();
        return m != null ? m.imagePath.get() : "";
    }
    public static float getImageScale() {
        CornerTextModule m = getModule();
        return m != null ? m.imageScale.get() : 1.0f;
    }
    public static int getImageOffsetX() {
        CornerTextModule m = getModule();
        return m != null ? m.imageOffsetX.get() : 0;
    }
    public static int getImageOffsetY() {
        CornerTextModule m = getModule();
        return m != null ? m.imageOffsetY.get() : 0;
    }
    public static void setImagePath(String path) {
        CornerTextModule m = getModule();
        if (m != null) { m.imagePath.set(path); clearCachedImage(); }
    }

    private static CornerTextModule getModule() {
        CrestModule m = CrestModules.get("corner_text");
        return m instanceof CornerTextModule ctm ? ctm : null;
    }

    // ── Image texture cache ───────────────────────────────────────────────

    private static final Map<String, ImageEntry> imageCache = new HashMap<>();
    private static String loadingPath = null;

    private record ImageEntry(Identifier id, DynamicTexture tex, int width, int height) {}

    static void clearCachedImage() {
        for (var entry : imageCache.values()) {
            Minecraft.getInstance().getTextureManager().release(entry.id());
            entry.tex().close();
        }
        imageCache.clear();
    }

    public static Identifier getImageTexture() {
        String path = getImagePath();
        if (path == null || path.isEmpty()) return null;

        ImageEntry cached = imageCache.get(path);
        if (cached != null) return cached.id();

        if (loadingPath != null && loadingPath.equals(path)) return null;
        loadingPath = path;

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            loadingPath = null;
            return null;
        }

        CompletableFuture.supplyAsync(() -> {
            try (FileInputStream in = new FileInputStream(file)) {
                NativeImage img = NativeImage.read(in);
                if (img == null) return (ImageEntry) null;
                Identifier id = Identifier.fromNamespaceAndPath("crest-client",
                    "corner_image/" + UUID.randomUUID().toString().replace("-", ""));
                DynamicTexture tex = new DynamicTexture(id::toDebugFileName, img);
                return new ImageEntry(id, tex, img.getWidth(), img.getHeight());
            } catch (Exception e) {
                return (ImageEntry) null;
            }
        }, Util.nonCriticalIoPool()).thenAccept(entry -> {
            loadingPath = null;
            if (entry != null) {
                Minecraft.getInstance().getTextureManager().register(entry.id(), entry.tex());
                imageCache.put(path, entry);
            }
        });

        return null;
    }

    public static int getImageWidth() {
        String path = getImagePath();
        if (path == null) return 0;
        ImageEntry e = imageCache.get(path);
        return e != null ? e.width() : 0;
    }

    public static int getImageHeight() {
        String path = getImagePath();
        if (path == null) return 0;
        ImageEntry e = imageCache.get(path);
        return e != null ? e.height() : 0;
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    public static void draw(GuiGraphicsExtractor g) {
        if (!CrestModules.isEnabled("corner_text")) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int guiW = g.guiWidth();
        int guiH = g.guiHeight();
        String corner = getCorner();

        // Draw image if enabled
        if (isImageEnabled()) {
            Identifier texId = getImageTexture();
            if (texId != null) {
                int iw = getImageWidth();
                int ih = getImageHeight();
                float is = getImageScale();
                int ioX = getImageOffsetX();
                int ioY = getImageOffsetY();
                int dw = (int) (iw * is);
                int dh = (int) (ih * is);

                int ix, iy;
                switch (corner) {
                    case "Top Left" -> { ix = ioX; iy = ioY; }
                    case "Top Right" -> { ix = guiW - dw - ioX; iy = ioY; }
                    case "Bottom Left" -> { ix = ioX; iy = guiH - dh - ioY; }
                    default -> { ix = guiW - dw - ioX; iy = guiH - dh - ioY; }
                }

                g.blit(RenderPipelines.GUI_TEXTURED, texId, ix, iy, 0f, 0f, dw, dh, iw, ih, 0xFFFFFFFF);
            }
        }

        // Draw text
        String t = getText();
        if (t == null || t.isEmpty()) return;

        int tw = font.width(t);
        int lh = font.lineHeight;
        int col = getColor();
        int offX = getOffsetX();
        int offY = getOffsetY();
        float s = getScale();

        int rx, ry;
        switch (corner) {
            case "Top Left" -> { rx = offX; ry = offY; }
            case "Top Right" -> { rx = guiW - tw - offX; ry = offY; }
            case "Bottom Left" -> { rx = offX; ry = guiH - lh - offY; }
            default -> { rx = guiW - tw - offX; ry = guiH - lh - offY; }
        }

        g.pose().pushMatrix();
        g.pose().translate(rx, ry);
        g.pose().scale(s);
        g.pose().translate(-rx, -ry);

        if (isBackgroundEnabled()) {
            HudBackground.draw(g, rx - 2, ry - 2, tw + 8, lh + 8);
        }
        if (hasShadow()) {
            g.text(font, Component.literal(t), rx + 1, ry + 1, (col & 0x00FFFFFF) | 0x80000000);
        }
        g.text(font, Component.literal(t), rx, ry, col);

        g.pose().popMatrix();
    }
}
