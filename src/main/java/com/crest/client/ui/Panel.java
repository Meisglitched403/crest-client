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
        if (w <= 0 || h <= 0) return;
        g.fill(x, y, x + Math.min(w, 1), y + h, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + w, y + Math.min(h, 1), color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static final int SHADOW_TEX = 32;
    private static final Map<Integer, TextureEntry> shadowCache = new HashMap<>();

    private static Identifier shadowTexId(int elevation) {
        return Identifier.fromNamespaceAndPath("crest-client", "gui/shadow_e" + elevation);
    }

    private static void ensureShadowTexture(int elevation) {
        if (shadowCache.containsKey(elevation)) return;
        int R = Math.min(elevation, SHADOW_TEX / 2);
        double sigma = R * 0.4;
        NativeImage img = new NativeImage(SHADOW_TEX, SHADOW_TEX, false);
        for (int y = 0; y < SHADOW_TEX; y++) {
            for (int x = 0; x < SHADOW_TEX; x++) {
                int dx = Math.min(x, SHADOW_TEX - 1 - x);
                int dy = Math.min(y, SHADOW_TEX - 1 - y);
                double dist = Math.sqrt(dx * dx + dy * dy);
                double falloff = Math.exp(-(dist * dist) / (2 * sigma * sigma));
                int alpha = (int) (falloff * 255 * 0.35);
                img.setPixel(x, y, ColorUtil.rgba(0, 0, 0, Math.min(255, alpha)));
            }
        }
        DynamicTexture tex = new DynamicTexture(() -> "crest-shadow-e" + elevation, img);
        Identifier id = shadowTexId(elevation);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        shadowCache.put(elevation, new TextureEntry(id, tex));
    }

    private static void drawShadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int elevation) {
        if (elevation <= 0) return;
        int e = Math.min(elevation, 12);
        ensureShadowTexture(e);

        int ox = e;
        int oy = e;
        int sx = x + ox;
        int sy = y + oy;
        int sw = w;
        int sh = h;
        int r = Math.min(e, Math.min(sw, sh) / 2);

        Identifier id = shadowTexId(e);
        int tint = ColorUtil.rgba(0, 0, 0, 255);

        // corners
        blit(g, id, sx - e, sy - e, e, e, 0, 0, e, e, tint);
        blit(g, id, sx + sw - e, sy - e, e, e, SHADOW_TEX - e, 0, e, e, tint);
        blit(g, id, sx - e, sy + sh - e, e, e, 0, SHADOW_TEX - e, e, e, tint);
        blit(g, id, sx + sw - e, sy + sh - e, e, e, SHADOW_TEX - e, SHADOW_TEX - e, e, e, tint);
        // edges
        if (sw > 2 * e)
            blit(g, id, sx + e, sy - e, sw - 2 * e, e, e, 0, 1, e, tint);
        if (sw > 2 * e)
            blit(g, id, sx + e, sy + sh - e, sw - 2 * e, e, e, SHADOW_TEX - e, 1, e, tint);
        if (sh > 2 * e)
            blit(g, id, sx - e, sy + e, e, sh - 2 * e, 0, e, e, 1, tint);
        if (sh > 2 * e)
            blit(g, id, sx + sw - e, sy + e, e, sh - 2 * e, SHADOW_TEX - e, e, e, 1, tint);
    }

    public static void clearCache() {
        Minecraft mc = Minecraft.getInstance();
        for (var entry : cache.values()) {
            mc.getTextureManager().release(entry.id());
            entry.texture().close();
        }
        cache.clear();
        for (var entry : shadowCache.values()) {
            mc.getTextureManager().release(entry.id());
            entry.texture().close();
        }
        shadowCache.clear();
    }
}
