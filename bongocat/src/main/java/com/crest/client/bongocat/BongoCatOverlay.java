package com.crest.client.bongocat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class BongoCatOverlay {
    private static final Identifier BODY = Identifier.fromNamespaceAndPath("bongocat", "textures/body.png");
    private static final Identifier LEFT_DOWN = Identifier.fromNamespaceAndPath("bongocat", "textures/paw_left_down.png");
    private static final Identifier LEFT_UP = Identifier.fromNamespaceAndPath("bongocat", "textures/paw_left_up.png");
    private static final Identifier RIGHT_DOWN = Identifier.fromNamespaceAndPath("bongocat", "textures/paw_right_down.png");
    private static final Identifier RIGHT_UP = Identifier.fromNamespaceAndPath("bongocat", "textures/paw_right_up.png");

    private static final float ASPECT = 397.0f / 201.0f;
    private static final int BASE_HEIGHT = 40;

    public static void render(GuiGraphicsExtractor g, Minecraft mc, InputTracker input, BongoCatConfig config) {
        if (mc.getWindow() == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float scale = config.scale;
        int catW = (int) (BASE_HEIGHT * ASPECT * scale);
        int catH = (int) (BASE_HEIGHT * scale);

        int cx = config.x;
        int cy = config.y;
        if (cx < 0 || cy < 0 || cx > screenW || cy > screenH) {
            cx = (screenW - catW) / 2;
            cy = screenH - catH - 4;
        }

        g.blit(RenderPipelines.GUI_TEXTURED, BODY, cx, cy, 0, 0, catW, catH, catW, catH);

        Identifier leftTex = input.getLeftPaw().isRaised() ? LEFT_UP : LEFT_DOWN;
        g.blit(RenderPipelines.GUI_TEXTURED, leftTex, cx, cy, 0, 0, catW, catH, catW, catH);

        Identifier rightTex = input.getRightPaw().isRaised() ? RIGHT_UP : RIGHT_DOWN;
        g.blit(RenderPipelines.GUI_TEXTURED, rightTex, cx, cy, 0, 0, catW, catH, catW, catH);

        if (config.keyboardVisible) {
            int kbX = cx + catW + 4;
            int kbY = cy + (catH - VirtualKeyboard.getHeight(scale)) / 2;
            VirtualKeyboard.render(g, kbX, kbY, scale, input.getKeyStates());
        }
    }
}
