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
        int labelW = font.width(setting.getName()) + 4;
        String keyName = capturing ? "..." : setting.getKeyName();
        int keyW = font.width("[" + keyName + "]") + 14;
        int keyX = x + w - keyW - 4;

        boolean hover = mx >= keyX && mx <= keyX + keyW && my >= y && my <= y + H;
        int bg = ColorUtil.withAlpha(hover ? Theme.BG_HOVER : Theme.BG_SURFACE, 220);
        if (capturing) bg = ColorUtil.withAlpha(Theme.BG_SELECT, 240);
        g.fill(keyX, y + 2, keyX + keyW, y + H - 2, bg);
        g.centeredText(font, Component.literal("[" + keyName + "]"), keyX + keyW / 2, y + 4,
            capturing ? Theme.getAnimatedAccent() : hover ? Theme.TEXT : Theme.TEXT_DIM);

        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && onCapture != null) { onCapture.accept(null, setting); return true; }
        return false;
    }
}
