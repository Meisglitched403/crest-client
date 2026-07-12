package com.crest.client.ui;

import com.crest.client.core.CrestModules;
import com.crest.client.core.setting.StringSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class TextRow implements Widget {
    private final StringSetting setting;
    private boolean focused;
    private boolean cursorOn;

    public TextRow(StringSetting setting) {
        this.setting = setting;
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        int labelW = font.width(setting.getName()) + 4;
        int fieldX = x + labelW + 4;
        int fieldW = Math.max(w - labelW - 8, 60);
        String text = setting.get();

        boolean hover = mx >= fieldX && mx <= fieldX + fieldW && my >= y && my <= y + H;
        int bg = ColorUtil.withAlpha(focused ? Theme.BG_SELECT : hover ? Theme.BG_HOVER : Theme.BG_SURFACE, 220);
        g.fill(fieldX, y + 2, fieldX + fieldW, y + H - 2, bg);
        if (focused) g.fill(fieldX, y + 2, fieldX + fieldW, y + 3, Theme.getAnimatedAccent());

        String display = font.width(text) > fieldW - 8
            ? font.plainSubstrByWidth(text, fieldW - 10) + "\u2026"
            : text;
        g.text(font, Component.literal(display), fieldX + 4, y + 4, Theme.TEXT);

        cursorOn = focused && (System.currentTimeMillis() / 500) % 2 == 0;
        if (cursorOn) {
            int cx = fieldX + 4 + font.width(text);
            g.fill(cx, y + 3, cx + 1, y + H - 3, Theme.getAnimatedAccent());
        }

        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { focused = true; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!focused) return false;
        if (key == 257 || key == 335) { focused = false; return true; }
        if (key == 259) {
            String val = setting.get();
            if (!val.isEmpty()) { setting.set(val.substring(0, val.length() - 1)); CrestModules.getConfigManager().markDirty(); }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(int codepoint, int mods) {
        if (!focused) return false;
        if (codepoint >= 32 && codepoint < 127) {
            setting.set(setting.get() + (char) codepoint);
            CrestModules.getConfigManager().markDirty();
            return true;
        }
        return false;
    }
}
