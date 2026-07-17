package com.crest.client.ui;

import com.crest.client.core.setting.KeybindSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public class KeybindRow implements Widget {
    private final KeybindSetting setting;
    private BiConsumer<String, KeybindSetting> onCapture;
    private boolean capturing;

    public KeybindRow(KeybindSetting setting) {
        this.setting = setting;
    }

    public void setOnCapture(BiConsumer<String, KeybindSetting> onCapture) { this.onCapture = onCapture; }
    public void setCapturing(boolean v) { capturing = v; }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);

        String keyName = capturing ? "Press a key..." : setting.getKeyName();
        int keyW = font.width(keyName) + 16;
        int keyX = x + w - keyW - 4;

        boolean hover = mx >= keyX && mx <= keyX + keyW && my >= y && my <= y + H;
        int bg;
        if (capturing) {
            bg = ColorUtil.withAlpha(Theme.BG_SELECT, 240);
        } else if (hover) {
            bg = ColorUtil.withAlpha(Theme.BG_HOVER, 220);
        } else {
            bg = ColorUtil.withAlpha(Theme.SURFACE_VARIANT, 200);
        }
        g.fill(keyX, y + 2, keyX + keyW, y + H - 2, bg);
        if (capturing) {
            g.fill(keyX, y + 2, keyX + keyW, y + 3, Theme.getAnimatedAccent());
        }

        int textColor = capturing ? Theme.getAnimatedAccent() : (hover ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT);
        g.centeredText(font, Component.literal(keyName), keyX + keyW / 2, y + 4, textColor);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && onCapture != null) { onCapture.accept(null, setting); return true; }
        return false;
    }
}
