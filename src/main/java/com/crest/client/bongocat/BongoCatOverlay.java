package com.crest.client.bongocat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * ponytail: Faithful Bongo Cat overlay using the REAL bongo-cat-obs sprites
 * (cat-rest / cat-left / cat-right), baked rotated -13deg so they display like
 * the plugin. Behavior mirrors the plugin:
 *   - idle "breathing": whole cat subtly scales vertically (sine).
 *   - LEFT paw rises when WASD / Shift / Ctrl are held (cat-left image).
 *   - RIGHT paw "holds the mouse": the cat-right image follows the cursor
 *     smoothly (smoothed offset) and shows the raised right paw while moving.
 */
public class BongoCatOverlay {
    // smoothed mouse-follow offset (in display px, pre-scale)
    private static float followX, followY;
    private static long animT;

    public static void render(GuiGraphicsExtractor g, Minecraft mc, InputTracker input, BongoCatConfig config,
                              int baseX, int baseY) {
        if (mc.getWindow() == null) return;
        BongoCatTextures.ensure();

        float s = config.scale;
        int dw = (int) (BongoCatTextures.TEX_W * s);
        int dh = (int) (BongoCatTextures.TEX_H * s);

        // breathing (sine, plugin range ~0.98..1.08)
        animT += 16;
        double stretch = 1.03 + 0.05 * Math.sin(animT / 1000.0 * Math.PI * 2.0 * 0.25);
        int breathH = (int) (dh * stretch);

        boolean leftActive = input.anyWasdOrMod();
        boolean rightActive = input.mouseMovedRecently();

        // smooth the right-paw follow offset toward the cursor
        int tx = input.getCursorX();
        int ty = input.getCursorY();
        float targetX = clamp((tx - (baseX + dw / 2)) * 0.25f, -dw * 0.18f, dw * 0.18f);
        float targetY = clamp((ty - (baseY + dh / 2)) * 0.25f, -dh * 0.18f, dh * 0.18f);
        float lerp = rightActive ? 0.25f : 0.12f;
        followX += (targetX - followX) * lerp;
        followY += (targetY - followY) * lerp;

        int drawY = baseY + (dh - breathH); // anchor bottom
        int tw = BongoCatTextures.TEX_W;
        int th = BongoCatTextures.TEX_H;

        // base cat
        net.minecraft.resources.Identifier base = leftActive ? BongoCatTextures.LEFT : BongoCatTextures.REST;
        g.blit(RenderPipelines.GUI_TEXTURED, base, baseX, drawY, 0f, 0f, tw, th, tw, th, 0xFFFFFFFF);

        // right paw follows the mouse on top of the base
        if (rightActive) {
            int rx = (int) (baseX + followX);
            int ry = (int) (drawY + followY);
            g.blit(RenderPipelines.GUI_TEXTURED, BongoCatTextures.RIGHT, rx, ry, 0f, 0f, tw, th, tw, th, 0xFFFFFFFF);
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
