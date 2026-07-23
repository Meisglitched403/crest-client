package com.crest.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class Panel {
    private static final int TEX = 32;
    private static final Map<Integer, TextureEntry> cache = new HashMap<>();
    private static int lastRadius = -1;

    private record TextureEntry(Identifier id, DynamicTexture texture) {}

    private Panel() {}

    private static Identifier texId(int radius) {
        return Identifier.fromNamespaceAndPath("crest-client", "gui/panel_r" + radius);
    }

    private static void ensureTexture(int r) {
        if (cache.containsKey(r)) return;
        NativeImage img = new NativeImage(TEX, TEX, false);
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                img.setPixel(x, y, ColorUtil.rgba(255, 255, 255, roundAlpha(x, y, r)));
            }
        }
        DynamicTexture tex = new DynamicTexture(() -> "crest-panel-r" + r, img);
        Identifier id = texId(r);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        cache.put(r, new TextureEntry(id, tex));
    }

    private static int roundAlpha(int x, int y, int R) {
        int cx = Math.min(x, TEX - 1 - x);
        int cy = Math.min(y, TEX - 1 - y);
        if (cx >= R && cy >= R) return 255;
        int dx = R - cx;
        int dy = R - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist >= R) return 0;
        if (dist <= R - 1.5) return 255;
        double f = (R - dist) / 1.5;
        return (int) (255 * Anim.clamp((float) f, 0, 1));
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint) {
        int r = Math.min(Theme.RADIUS, Math.min(w, h) / 2);
        if (r < 0) r = 0;
        if (w <= 0 || h <= 0) return;
        ensureTexture(r);

        Identifier id = texId(r);
        blit(g, id, x,            y,            r,        r,        0,  0,  r,  r,  tint);
        blit(g, id, x + r,        y,            w - 2*r,  r,        r,  0,  1,  r,  tint);
        blit(g, id, x + w - r,    y,            r,        r,        r,  0,  r,  r,  tint);
        blit(g, id, x,            y + r,        r,        h - 2*r,  0,  r,  r,  1,  tint);
        blit(g, id, x + r,        y + r,        w - 2*r,  h - 2*r,  r,  r,  1,  1,  tint);
        blit(g, id, x + w - r,    y + r,        r,        h - 2*r,  r,  r,  r,  1,  tint);
        blit(g, id, x,            y + h - r,    r,        r,        0,  r,  r,  r,  tint);
        blit(g, id, x + r,        y + h - r,    w - 2*r,  r,        r,  r,  1,  r,  tint);
        blit(g, id, x + w - r,    y + h - r,    r,        r,        r,  r,  r,  r,  tint);
    }

    private static void blit(GuiGraphicsExtractor g, Identifier id, int dx, int dy, int dw, int dh,
                             int sx, int sy, int sw, int sh, int tint) {
        if (dw <= 0 || dh <= 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, id, dx, dy, 0f, 0f, sx, sy, sw, sh, TEX, TEX, tint);
    }

    public static void drawGlass(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint, int accent) {
        draw(g, x, y, w, h, tint);
        g.fill(x + 3, y + 2, x + w - 3, y + 3, ColorUtil.withAlpha(0xFFFFFFFF, 18));
        g.fill(x + 4, y + 1, x + w - 4, y + 2, ColorUtil.withAlpha(accent, Theme.topStripAlpha));
    }

    public static void drawElevated(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint, int elevation) {
        if (elevation > 0) drawShadow(g, x, y, w, h, elevation);
        draw(g, x, y, w, h, tint);
    }

    public static void drawGlassElevated(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint, int accent, int elevation) {
        drawElevated(g, x, y, w, h, tint, elevation);
        g.fill(x + 3, y + 2, x + w - 3, y + 3, ColorUtil.withAlpha(0xFFFFFFFF, 18));
        g.fill(x + 4, y + 1, x + w - 4, y + 2, ColorUtil.withAlpha(accent, Theme.topStripAlpha));
    }

    public static void drawGlassCard(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover) {
        int topAlpha = hover ? 32 : 21;
        int botAlpha = hover ? 5 : 2;
        int top = ColorUtil.withAlpha(0xFFFFFFFF, topAlpha);
        int bot = ColorUtil.withAlpha(0xFFFFFFFF, botAlpha);
        g.fillGradient(x, y, x + w, y + h, top, bot);
        drawHollowRect(g, x, y, w, h, Theme.BORDER_LIGHT);
    }

    public static void drawHollowRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void drawShadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int elevation) {
        if (elevation <= 0) return;
        int baseAlpha = Math.min(40, 8 * elevation);
        for (int i = 1; i <= elevation; i++) {
            float t = 1f - (i - 1f) / elevation;
            int alpha = (int) (baseAlpha * t * t);
            if (alpha <= 0) continue;
            g.fill(x + i, y + h, x + w + i, y + h + 1, ColorUtil.withAlpha(0x000000, alpha));
            g.fill(x + w, y + i, x + w + 1, y + h + i, ColorUtil.withAlpha(0x000000, alpha));
        }
        int cornerAlpha = Math.min(20, 4 * elevation);
        for (int i = 1; i <= Math.min(elevation, 8); i++) {
            for (int j = 1; j <= Math.min(elevation, 8); j++) {
                float t = 1f - (Math.max(i, j) - 1f) / elevation;
                int alpha = (int) (cornerAlpha * t * t);
                if (alpha <= 0) continue;
                g.fill(x + w + j - 1, y + h + i - 1, x + w + j, y + h + i, ColorUtil.withAlpha(0x000000, alpha));
            }
        }
    }

    public static void clearCache() {
        Minecraft mc = Minecraft.getInstance();
        for (var entry : cache.values()) {
            mc.getTextureManager().release(entry.id());
            entry.texture().close();
        }
        cache.clear();
    }
}
