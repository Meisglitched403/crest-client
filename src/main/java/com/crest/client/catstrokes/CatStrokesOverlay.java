package com.crest.client.catstrokes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public class CatStrokesOverlay {
    private static final int ARM_X = 65;
    private static final int ARM_Y = 60;
    private static final int ARM_W = 106;
    private static final int ARM_H = 316;

    private static final int MPAD_X = 5;
    private static final int MPAD_Y = 225;
    private static final int MPAD_W = 170;
    private static final int MPAD_H = 115;

    private static final int HAND_W = 181;
    private static final int HAND_H = 128;

    public static void render(GuiGraphicsExtractor g, Minecraft mc, KeyStateTracker input, int baseX, int baseY, float scale) {
        if (mc.getWindow() == null) return;
        CatStrokesTextures.ensure();
        if (CatStrokesTextures.hasFailed()) return;

        int tw = CatStrokesTextures.TEX_W;
        int th = CatStrokesTextures.TEX_H;

        g.pose().pushMatrix();
        g.pose().translate(baseX, baseY);
        g.pose().scale(scale);
        g.pose().translate(-baseX, -baseY);

        g.blit(RenderPipelines.GUI_TEXTURED, CatStrokesTextures.RIGHTARM,
            baseX + ARM_X, baseY + ARM_Y, 0f, 0f, ARM_W, ARM_H, ARM_W, ARM_H, ARM_W, ARM_H, 0xFFFFFFFF);

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

        double rawX = input.getCursorX();
        double rawY = input.getCursorY();
        int winW = mc.getWindow().getWidth();
        int winH = mc.getWindow().getHeight();
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        double guiX = rawX * guiW / winW;
        double guiY = rawY * guiH / winH;
        double texX = (guiX - baseX) / scale;
        double texY = (guiY - baseY) / scale;
        texX = Math.max(MPAD_X, Math.min(MPAD_X + MPAD_W, texX));
        texY = Math.max(MPAD_Y, Math.min(MPAD_Y + MPAD_H, texY));

        g.blit(RenderPipelines.GUI_TEXTURED, CatStrokesTextures.RIGHTHAND,
            baseX + (int)texX - HAND_W / 2, baseY + (int)texY - HAND_H / 2,
            0f, 0f, HAND_W, HAND_H, HAND_W, HAND_H, HAND_W, HAND_H, 0xFFFFFFFF);

        g.pose().popMatrix();
    }
}
