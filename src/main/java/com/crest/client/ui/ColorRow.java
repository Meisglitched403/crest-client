package com.crest.client.ui;

import com.crest.client.core.setting.ColorSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ColorRow implements Widget {
    private final ColorSetting setting;
    private Consumer<ColorSetting> onPick;

    public ColorRow(ColorSetting setting) {
        this.setting = setting;
    }

    public void setOnPick(Consumer<ColorSetting> onPick) { this.onPick = onPick; }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        int labelW = font.width(setting.getName()) + 4;
        int swatchX = x + w - 22;
        int swatchS = 13;

        g.fill(swatchX - 1, y + 3, swatchX + swatchS + 1, y + 3 + swatchS + 1, 0xFF000000);
        g.fill(swatchX, y + 4, swatchX + swatchS, y + 4 + swatchS, 0xFF000000 | setting.getRGB());

        String hex = String.format("#%06X", setting.getRGB());
        int hexX = swatchX - font.width(hex) - 6;
        g.text(font, Component.literal(hex), hexX, y + 5, Theme.TEXT_DIM);

        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && onPick != null) { onPick.accept(setting); return true; }
        return false;
    }
}
