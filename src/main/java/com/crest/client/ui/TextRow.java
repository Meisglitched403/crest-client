package com.crest.client.ui;

import com.crest.client.core.CrestModules;
import com.crest.client.core.setting.StringSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class TextRow implements Widget {
    private final StringSetting setting;
    private final TextInput input;
    private int labelW;

    public TextRow(StringSetting setting) {
        this.setting = setting;
        input = new TextInput(setting.get(), v -> { setting.set(v); CrestModules.getConfigManager().markDirty(); });
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        labelW = font.width(setting.getName()) + 4;
        int fieldX = x + labelW + 4;
        int fieldW = Math.max(w - labelW - 8, 60);
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);
        input.render(g, font, fieldX, y, fieldW, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) { return input.mouseClicked(mx, my, button); }
    @Override
    public boolean keyPressed(int key, int scan, int mods) { return input.keyPressed(key, scan, mods); }
    @Override
    public boolean charTyped(int codepoint, int mods) { return input.charTyped(codepoint, mods); }
}
