package com.crest.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface Widget {
    int H = 20;

    int getHeight();
    void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta);
    boolean mouseClicked(double mx, double my, int button);
    default boolean mouseDragged(double mx, double my) { return false; }
    default boolean keyPressed(int key, int scan, int mods) { return false; }
    default boolean charTyped(int codepoint, int mods) { return false; }
    default void onLayout(int x, int y, int width, int height) {}
}
