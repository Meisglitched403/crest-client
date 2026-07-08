package com.crest.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Rounded "glass" panel drawn via a runtime-generated white rounded texture,
 * 9-sliced and tinted with the theme color. No binary asset required.
 */
public final class Panel {
    private static final int TEX = 32;
    private static final int R = 8;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("crest-client", "gui/panel");
    private static boolean registered = false;

    private Panel() {}

    private static void ensureTexture() {
        if (registered) return;
        registered = true;
        DynamicTexture tex = new DynamicTexture("crest-panel", TEX, TEX, true);
        NativeImage img = tex.getPixels();
        // White rounded mask: alpha 255 inside, AA falloff at corners.
        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                int alpha = roundAlpha(x, y);
                img.setPixel(x, y, ColorUtil.rgba(255, 255, 255, alpha));
            }
        }
        tex.upload();
        Minecraft.getInstance().getTextureManager().register(ID, tex);
    }

    private static int roundAlpha(int x, int y) {
        int cx = Math.min(x, TEX - 1 - x);
        int cy = Math.min(y, TEX - 1 - y);
        if (cx >= R && cy >= R) return 255;
        // distance from the corner circle center
        int dx = R - cx;
        int dy = R - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist >= R) return 0;
        if (dist <= R - 1.5) return 255;
        double f = (R - dist) / 1.5;
        return (int) (255 * Anim.clamp((float) f, 0, 1));
    }

    /** Draw a rounded panel. tint is ARGB; alpha controls glass opacity. */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint) {
        ensureTexture();
        int r = Math.min(R, Math.min(w, h) / 2);
        if (w <= 0 || h <= 0) return;

        // clang-format off
        blit(g, x,            y,            r,        r,        0,  0,  r,  r,  tint);
        blit(g, x + r,        y,            w - 2*r,  r,        r,  0,  1,  r,  tint);
        blit(g, x + w - r,    y,            r,        r,        r,  0,  r,  r,  tint);
        blit(g, x,            y + r,        r,        h - 2*r,  0,  r,  r,  1,  tint);
        blit(g, x + r,        y + r,        w - 2*r,  h - 2*r,  r,  r,  1,  1,  tint);
        blit(g, x + w - r,    y + r,        r,        h - 2*r,  r,  r,  r,  1,  tint);
        blit(g, x,            y + h - r,    r,        r,        0,  r,  r,  r,  tint);
        blit(g, x + r,        y + h - r,    w - 2*r,  r,        r,  r,  1,  r,  tint);
        blit(g, x + w - r,    y + h - r,    r,        r,        r,  r,  r,  r,  tint);
        // clang-format on
    }

    private static void blit(GuiGraphicsExtractor g, int dx, int dy, int dw, int dh,
                             int sx, int sy, int sw, int sh, int tint) {
        if (dw <= 0 || dh <= 0) return;
        g.blit(RenderPipelines.GUI_TEXTURED, ID, dx, dy, 0f, 0f, sx, sy, sw, sh, TEX, TEX, tint);
    }

    /** Draw a panel with a subtle top accent line and a faint header gradient. */
    public static void drawGlass(GuiGraphicsExtractor g, int x, int y, int w, int h, int tint, int accent) {
        draw(g, x, y, w, h, tint);
        // soft top highlight
        g.fill(x + 3, y + 2, x + w - 3, y + 3, ColorUtil.withAlpha(0xFFFFFFFF, 18));
        // accent strip at top
        g.fill(x + 4, y + 1, x + w - 4, y + 2, ColorUtil.withAlpha(accent, 160));
    }
}
