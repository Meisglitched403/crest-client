package com.crest.client.ui;

import com.crest.client.core.CrestModules;
import com.crest.client.core.setting.ModeSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ModeRow implements Widget {
    private final ModeSetting setting;

    public ModeRow(ModeSetting setting) {
        this.setting = setting;
    }

    @Override
    public int getHeight() { return H; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int x, int y, int w, int mx, int my, float delta) {
        g.text(font, Component.literal(setting.getName()), x + 2, y + 4, Theme.ON_SURFACE_VARIANT);

        String mode = setting.getMode();
        int modeW = font.width(mode) + 22;
        int modeX = x + w - modeW - 4;

        boolean hover = mx >= modeX && mx <= modeX + modeW && my >= y && my <= y + H;
        int bg = ColorUtil.withAlpha(hover ? Theme.BG_HOVER : Theme.SURFACE_VARIANT, 220);
        g.fill(modeX, y + 2, modeX + modeW, y + H - 2, bg);

        String arrow = "\u25BC";
        int arrowW = font.width(arrow);
        g.text(font, Component.literal(mode), modeX + 6, y + 4, hover ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT);
        g.text(font, Component.literal(arrow), modeX + modeW - arrowW - 6, y + 4,
            ColorUtil.withAlpha(hover ? Theme.ON_SURFACE : Theme.ON_SURFACE_VARIANT, 150));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) { setting.cycle(); CrestModules.getConfigManager().markDirty(); return true; }
        if (button == 1) { setting.cycleBack(); CrestModules.getConfigManager().markDirty(); return true; }
        return false;
    }
}
