package com.crest.client.catstrokes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public class CatStrokesOverlay {
    public static void render(GuiGraphicsExtractor g, Minecraft mc, KeyStateTracker input, int baseX, int baseY, float scale) {
        if (mc.getWindow() == null) return;
        CatStrokesTextures.ensure();
        if (CatStrokesTextures.hasFailed()) return;

        int dw = (int) (CatStrokesTextures.TEX_W * scale);
        int dh = (int) (CatStrokesTextures.TEX_H * scale);

        int tw = CatStrokesTextures.TEX_W;
        int th = CatStrokesTextures.TEX_H;

        g.pose().pushMatrix();
        g.pose().translate(baseX, baseY);
        g.pose().scale(scale);
        g.pose().translate(-baseX, -baseY);

        g.blit(RenderPipelines.GUI_TEXTURED, CatStrokesTextures.CATBG,
            baseX, baseY, 0f, 0f, tw, th, tw, th, 0xFFFFFFFF);

        if (input.anyPressed()) {
            for (int i = 0; i < 15; i++) {
                if (input.isPressed(i)) {
                    g.blit(RenderPipelines.GUI_TEXTURED, CatStrokesTextures.KEYBOARD_OVERLAYS[i],
                        baseX, baseY, 0f, 0f, tw, th, tw, th, 0xFFFFFFFF);
                }
            }

            int handIdx = input.getLastPressedIndex();
            if (handIdx >= 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, CatStrokesTextures.LEFTHAND[handIdx],
                    baseX, baseY, 0f, 0f, tw, th, tw, th, 0xFFFFFFFF);
            }
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, CatStrokesTextures.LEFTUP,
                baseX, baseY, 0f, 0f, tw, th, tw, th, 0xFFFFFFFF);
        }

        g.pose().popMatrix();
    }
}
