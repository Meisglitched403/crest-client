package com.crest.client.music;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Plain (non-mixin) holder for the pause screen's mini player. Mixins cannot carry
 * non-private static members, so the widget instance and its card geometry live here.
 */
public final class PauseScreenMusicWidget {
    private static final int CARD_H = 80;

    private static final MusicMiniPlayer mini = new MusicMiniPlayer();
    private static int cardX;
    private static int cardY;
    private static int cardW;

    private PauseScreenMusicWidget() {}

    public static void render(GuiGraphicsExtractor g, Font font, int width, int height, int mx, int my, float delta) {
        cardW = Math.min(286, width - 24);
        cardX = width - 12 - cardW;
        cardY = height - 12 - CARD_H;
        mini.render(g, font, cardX, cardY, cardW, CARD_H, mx, my, delta);
    }

    public static boolean inside(double mx, double my) {
        return mx >= cardX && mx <= cardX + cardW
            && my >= cardY && my <= cardY + CARD_H;
    }

    public static boolean handleClick(double mx, double my) {
        return mini.mouseClicked(mx, my);
    }
}