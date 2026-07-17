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
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);

        int swatchS = 12;
        int swatchX = x + w - swatchS - 4;
        int swatchY = y + 4;

        // Border
        g.fill(swatchX - 1, swatchY - 1, swatchX + swatchS + 1, swatchY + swatchS + 1, 0x44000000);
        // Color fill
        g.fill(swatchX, swatchY, swatchX + swatchS, swatchY + swatchS, 0xFF000000 | setting.getRGB());

        String hex = String.format("#%06X", setting.getRGB());
        int hexX = swatchX - font.width(hex) - 6;
        g.text(font, Component.literal(hex), hexX, y + 4, ColorUtil.withAlpha(Theme.ON_SURFACE_VARIANT, 180));

        boolean hover = mx >= swatchX && mx <= swatchX + swatchS && my >= swatchY && my <= swatchY + swatchS;
        if (hover) {
            g.fill(swatchX - 1, swatchY - 1, swatchX + swatchS + 1, swatchY + swatchS + 1, Theme.getAnimatedAccent());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && onPick != null) { onPick.accept(setting); return true; }
        return false;
    }
}
